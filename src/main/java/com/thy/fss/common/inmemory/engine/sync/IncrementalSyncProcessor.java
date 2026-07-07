package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.analysis.AggregationState;
import com.thy.fss.common.inmemory.engine.analysis.AggregationTask;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan;
import com.thy.fss.common.inmemory.engine.analysis.GroupingKey;
import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.engine.mapping.MappingApplicator;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.RelatedEntityLookup;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

/**
 * Processes batch snapshot events through a four-phase pipeline.
 *
 * <p>Integrates into {@code DataSynchronizationEngine} — this is NOT an
 * independent subsystem.</p>
 *
 * Pipeline Phases
 * <ol>
 *   <li><b>Entity Upsert</b> — apply filtered entities to DependencyGraph</li>
 *   <li><b>Mapping Update</b> — re-evaluate affected PropertyMappings</li>
 *   <li><b>Aggregation Update</b> — update affected dashboard aggregations</li>
 *   <li><b>Consumer Data Propagation</b> — bridge DependencyGraph → InMemoryDataStore</li>
 * </ol>
 *
 * Pre-processing
 * <p>If a {@link TimeWindowRule} is configured for the datasource, entities are
 * filtered through the rule's specification before entering the pipeline.
 * Entities where {@code specification.test(entity) == false} are discarded.</p>
 */
public class IncrementalSyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalSyncProcessor.class);

    private final InMemorySpecStoreFactory factory;
    private final DependencyGraph dependencyGraph;
    private final Queue<QueuedEvent> eventQueue;
    private volatile AnalysisResult analysisResult;
    private final AtomicLong streamingVersion;
    private volatile boolean fullSyncInProgress;

    /**
     * Wraps a BatchSnapshotEvent with its datasource name for queuing.
     * Datasource identity comes from subscription context, not from the event itself.
     */
    static class QueuedEvent {
        final String dataSourceName;
        final BatchSnapshotEvent<?> event;

        QueuedEvent(String dataSourceName, BatchSnapshotEvent<?> event) {
            this.dataSourceName = dataSourceName;
            this.event = event;
        }
    }

    /**
     * Cache of aggregation state per dashboard+task combination.
     * Enables incremental aggregation computation instead of full scans.
     * Key format: "dashboardId:dataSourceName:fieldPathHash"
     * Cleared when a full sync completes to ensure consistency.
     */
    private final Map<String, AggregationState> aggregationStateCache = new ConcurrentHashMap<>();

    /**
     * Cache of IndexDefinitions keyed by dedup key (dataSourceName:fkPathNames).
     * Ensures one IndexDefinition per unique (datasource, FK paths) combination.
     */
    private final Map<String, IndexDefinition<?>> indexDefinitionCache = new ConcurrentHashMap<>();

    /**
     * Maps each PropertyMapping to its corresponding IndexDefinition.
     * Used by the optimized dependencyGraphLookup lambda for indexed FK lookup.
     */
    private final Map<PropertyMapping<?, ?>, IndexDefinition<?>> mappingIndexCache = new ConcurrentHashMap<>();

    /**
     * Pre-computed store mapping groupings.
     * Updated on DependencyGraph.addMapping() / removeMapping() calls.
     *
     * Structure: Map&lt;storeId, List&lt;PropertyMapping&lt;?, ?&gt;&gt;&gt;
     * Only includes mappings where isForDashboard() == false.
     *
     * Thread safety: volatile reference + copy-on-write semantics.
     * Write operations (mapping add/remove) are infrequent and already synchronized.
     * Each write creates a new immutable map snapshot.
     */
    private volatile Map<String, List<PropertyMapping<?, ?>>> precomputedStoreMappings = Collections.emptyMap();

    /**
     * Creates a new IncrementalSyncProcessor.
     *
     * @param factory           the factory for accessing datasource registrations and TimeWindowRules
     * @param dependencyGraph   the unified data structure for entity storage and dependency tracking
     * @param analysisResult    the initial analysis result from DataSynchronizationEngine
     * @param streamingVersion  shared streaming version counter from Engine (single source of truth)
     */
    public IncrementalSyncProcessor(
            InMemorySpecStoreFactory factory,
            DependencyGraph dependencyGraph,
            AnalysisResult analysisResult,
            AtomicLong streamingVersion) {
        this.factory = factory;
        this.dependencyGraph = dependencyGraph;
        this.analysisResult = analysisResult;
        this.streamingVersion = streamingVersion;
        this.eventQueue = new ConcurrentLinkedQueue<>();
        registerMappingIndexes();
    }

    /**
     * Updates the analysis result after mapping add/remove operations.
     * Called by DataSynchronizationEngine when mappings change.
     *
     * @param newAnalysisResult the updated analysis result
     */
    public void updateAnalysisResult(AnalysisResult newAnalysisResult) {
        this.analysisResult = newAnalysisResult;
        registerMappingIndexes();
    }

    /**
     * Returns the current analysis result.
     *
     * @return the current analysis result
     */
    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    /**
     * Returns the current streaming version counter value.
     * Package-private for testing.
     *
     * @return the current streaming version
     */
    long getLocalStreamingVersion() {
        return streamingVersion.get();
    }

    /**
     * Sets whether a full sync is currently in progress.
     * When true, incoming batch snapshot events should be queued instead of processed immediately.
     * Called by DataSynchronizationEngine before and after full sync cycles.
     *
     * @param inProgress true if full sync is in progress, false otherwise
     */
    public void setFullSyncInProgress(boolean inProgress) {
        this.fullSyncInProgress = inProgress;
        logger.debug("Full sync in progress flag set to {}", inProgress);

        // Clear aggregation state cache when full sync completes.
        // The next batch will do a full scan to repopulate the cache,
        // ensuring consistency with the post-full-sync DependencyGraph state.
        if (!inProgress) {
            aggregationStateCache.clear();
            logger.debug("Aggregation state cache cleared after full sync completion");
        }
    }

    /**
     * Returns whether a full sync is currently in progress.
     * When true, incoming events should be queued via {@link #queueEvent} instead of
     * being processed immediately via {@link #processBatchSnapshot}.
     *
     * @return true if full sync is in progress
     */
    public boolean isFullSyncInProgress() {
        return fullSyncInProgress;
    }

    /**
     * Main pipeline entry point. Processes a batch snapshot event through
     * pre-processing (TimeWindowRule filtering) and the four-phase pipeline.
     *
     * <p>Every incoming entity is considered changed — no field-by-field comparison.
     * A single entity processing error is logged; remaining entities continue.</p>
     *
     * <p>Datasource identity is provided via the {@code dataSourceName} parameter
     * (from subscription context), not from the event itself.</p>
     *
     * <p>When the datasource is in INITIALIZING state, Phases 2, 3, 4 are skipped —
     * consumers should not see data until the datasource is READY.</p>
     *
     * @param dataSourceName the datasource name (from subscription context)
     * @param event          the batch snapshot event to process
     * @param <T>            entity type implementing Identifiable
     */
    public <T extends Identifiable<?>> void processBatchSnapshot(
                String dataSourceName, BatchSnapshotEvent<T> event) {
            List<T> entities = event.getEntities();
            long totalStart = System.currentTimeMillis();

            logger.debug("Processing batch snapshot for datasource '{}': {} entities",
                    dataSourceName, entities.size());

            // === PRE-PROCESSING: TimeWindowRule filtering ===
            List<T> filteredEntities = applyTimeWindowFilter(dataSourceName, entities);

            logger.debug("After TimeWindowRule filtering for '{}': {} of {} entities passed",
                    dataSourceName, filteredEntities.size(), entities.size());

            // === PRE-PROCESSING: Detect entities filtered out by TimeWindowRule that exist in DependencyGraph ===
            // These entities no longer pass the specification and must be removed from DependencyGraph and indexes.
            List<T> removedEntities = detectRemovedEntities(dataSourceName, entities, filteredEntities);

            // === PRE-PROCESSING: Capture old entity states BEFORE Phase 1 overwrites them ===
            // Needed by Phase 2 (Bug 6: FIRST/LAST, Bug 7: ANY) and Phase 3 (Bug 5: incremental aggregation)
            Map<Object, Object> oldEntityStates = captureOldEntityStates(dataSourceName, filteredEntities);

            // === PRE-PROCESSING: Capture old FIRST/LAST entity IDs BEFORE Phase 1 ===
            // Needed by Phase 2 (Bug 6): after Phase 1 updates entities, the sorted order may change.
            // We must know the old first/last to detect when the first/last element shifts to a different entity.
            Map<String, Object> oldFirstLastIds = captureOldFirstLastIds(dataSourceName);

            // === PHASE 1: Entity Upsert — apply to DependencyGraph atomically ===
            // Phase 1 always runs — data accumulation in DependencyGraph continues
            // even during INITIALIZING state.
            long phase1Start = System.currentTimeMillis();
            applyPhase1EntityUpsert(dataSourceName, filteredEntities, oldEntityStates);

            // === PHASE 1 (cont.): Remove entities that no longer pass TimeWindowRule ===
            removeExpiredEntities(dataSourceName, removedEntities);
            long phase1Ms = System.currentTimeMillis() - phase1Start;

            // When INITIALIZING: skip Phases 2, 3, 4.
            // Data should not be included in mapping/aggregation calculations
            // and should not be reflected to consumers until READY.
            if (isDataSourceInitializing(dataSourceName)) {
                logger.debug("Phases 2-4 skipped for datasource '{}': datasource is still INITIALIZING",
                        dataSourceName);
                logger.info("StreamSync ds={}: Phase1 {} entities {}ms | total:{}ms",
                        dataSourceName, filteredEntities.size(), phase1Ms,
                        System.currentTimeMillis() - totalStart);
                return;
            }

            // === PHASE 2: Mapping Update — detect and re-evaluate affected mappings ===
            long phase2Start = System.currentTimeMillis();
            Set<PropertyMapping<?, ?>> affectedMappings = applyPhase2MappingUpdates(
                    dataSourceName, filteredEntities, oldFirstLastIds, oldEntityStates);
            long phase2Ms = System.currentTimeMillis() - phase2Start;

            // === PHASE 2.5: Store Mapping Application (via MappingApplicator) ===
            long phase2_5Start = System.currentTimeMillis();
            applyPhase2_5StoreMappings(dataSourceName, affectedMappings, filteredEntities);
            long phase2_5Ms = System.currentTimeMillis() - phase2_5Start;
            int storeMappingCount = (int) affectedMappings.stream()
                    .filter(m -> !m.isForDashboard()).count();

            // === PHASE 3: Aggregation Update — update affected dashboard aggregations ===
            long phase3Start = System.currentTimeMillis();
            applyPhase3AggregationUpdates(dataSourceName, filteredEntities, affectedMappings,
                    oldEntityStates, removedEntities);
            long phase3Ms = System.currentTimeMillis() - phase3Start;
            int aggregationCount = (int) affectedMappings.stream()
                    .filter(PropertyMapping::isForDashboard).count();

            // === PHASE 4: Consumer Data Propagation — DependencyGraph → InMemoryDataStore bridge ===
            // Bug 3 fix: Use shared streamingVersion from Engine (single source of truth).
            // Version increment happens ONLY in Engine listener, not here.
            long phase4Start = System.currentTimeMillis();
            long version = streamingVersion.get();
            applyPhase4ConsumerPropagation(dataSourceName, version);
            long phase4Ms = System.currentTimeMillis() - phase4Start;
            AffectedConsumerSet consumers = dependencyGraph.getAffectedConsumers(dataSourceName);
            int storeCount = consumers.getStoreIds().size();

            long totalMs = System.currentTimeMillis() - totalStart;
            logger.info("StreamSync ds={}: Phase1 {} entities {}ms | Phase2 {} mappings {}ms | Phase2.5 {} storeMappings {}ms | Phase3 {} aggregations {}ms | Phase4 {} stores {}ms | total:{}ms",
                    dataSourceName, filteredEntities.size(), phase1Ms,
                    affectedMappings.size(), phase2Ms,
                    storeMappingCount, phase2_5Ms,
                    aggregationCount, phase3Ms,
                    storeCount, phase4Ms,
                    totalMs);
        }


    /**
     * Processes batch datasource full sync results through the same pipeline.
     * Stub for now — implemented in task 6.7.
     *
     * @param dataSourceName the datasource name
     * @param entities       the full sync entity list
     * @param <T>            entity type implementing Identifiable
     */
    public <T extends Identifiable<?>> void processBatchDataSourceResult(
            String dataSourceName, List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            logger.debug("Skipping batch datasource result for '{}': empty or null entity list",
                    dataSourceName);
            return;
        }

        logger.debug("Processing batch datasource full sync result for '{}': {} entities",
                dataSourceName, entities.size());

        // Create a BatchSnapshotEvent and delegate to the same pipeline
        BatchSnapshotEvent<T> event = new BatchSnapshotEvent<>(
                entities, Instant.now());
        processBatchSnapshot(dataSourceName, event);
    }

    /**
     * Queues a batch snapshot event for later processing (during full sync or INITIALIZING).
     * Events are queued in FIFO order and processed after full sync completes.
     * Datasource name is provided from subscription context.
     *
     * @param dataSourceName the datasource name (from subscription context)
     * @param event          the event to queue
     */
    public void queueEvent(String dataSourceName, BatchSnapshotEvent<?> event) {
        eventQueue.offer(new QueuedEvent(dataSourceName, event));
        logger.debug("Queued event for datasource '{}': {} entities",
                dataSourceName, event.getEntities().size());
    }

    /**
     * Returns the number of events currently in the queue.
     *
     * @return the queued event count
     */
    public int getQueuedEventCount() {
        return eventQueue.size();
    }

    /**
     * Processes all queued events in order after full sync completes.
     * Events are processed sequentially in the order they were queued (FIFO).
     * No events are lost — each queued event is polled and processed exactly once.
     */
    @SuppressWarnings("unchecked")
        public void processQueuedEvents() {
            int processedCount = 0;
            QueuedEvent queued;
            while ((queued = eventQueue.poll()) != null) {
                try {
                    streamingVersion.incrementAndGet();  // Bug 2 fix: increment version per event before processing
                    processBatchSnapshot(queued.dataSourceName, (BatchSnapshotEvent) queued.event);
                    processedCount++;
                } catch (Exception e) {
                    logger.error("Error processing queued event for datasource '{}': {}",
                            queued.dataSourceName, e.getMessage(), e);
                }
            }
            if (processedCount > 0) {
                logger.debug("Processed {} queued event(s)", processedCount);
            }
        }

    /**
     * Processes queued events for a specific datasource.
     * Events for other datasources remain in the queue.
     * Events are processed in FIFO order.
     *
     * @param dsName the datasource name to process events for
     */
    @SuppressWarnings("unchecked")
    public void processQueuedEventsForDataSource(String dsName) {
        int processedCount = 0;
        Queue<QueuedEvent> remaining = new ConcurrentLinkedQueue<>();
        QueuedEvent queued;
        while ((queued = eventQueue.poll()) != null) {
            if (queued.dataSourceName.equals(dsName)) {
                try {
                    streamingVersion.incrementAndGet();
                    processBatchSnapshot(queued.dataSourceName, (BatchSnapshotEvent) queued.event);
                    processedCount++;
                } catch (Exception e) {
                    logger.error("Error processing queued event for datasource '{}': {}",
                            queued.dataSourceName, e.getMessage(), e);
                }
            } else {
                remaining.offer(queued);
            }
        }
        // Put back events for other datasources
        eventQueue.addAll(remaining);
        if (processedCount > 0) {
            logger.debug("Processed {} queued event(s) for datasource '{}'", processedCount, dsName);
        }
    }

    /**
     * Post-initialization catch-up: applies Phase 2.5 (store mappings) and Phase 4
     * (consumer propagation) for ALL stores after all streaming datasources have
     * transitioned to READY.
     *
     * <p>During INITIALIZING state, {@link #processBatchSnapshot} only runs Phase 1
     * (entity upsert to DependencyGraph) and skips Phase 2-4. This means cross-datasource
     * mappings are never applied to the initial data loaded via fetchAll(). This method
     * provides the catch-up mechanism: once all datasources are READY and their data is
     * in DependencyGraph, it applies all store mappings and propagates results to consumers.</p>
     *
     * <p>Called by {@code DataSynchronizationEngine.initializeStreamingInfrastructure()}
     * after all streaming datasources have completed their initial load.</p>
     */
    @SuppressWarnings("unchecked")
    public void applyPostInitializationCatchUp() {
        logger.debug("Post-initialization catch-up: applying store mappings for all stores...");

        // Collect ALL non-dashboard store mappings across all stores
        Set<PropertyMapping<?, ?>> allStoreMappings = new LinkedHashSet<>();
        Set<String> affectedPrimaryDatasources = new HashSet<>();

        for (String storeId : factory.getAllStoreIds()) {
            List<PropertyMapping<?, ?>> storeMappings = dependencyGraph.getMappingsByConsumerId(storeId);
            for (PropertyMapping<?, ?> m : storeMappings) {
                if (!m.isForDashboard()) {
                    allStoreMappings.add(m);
                }
            }
        }

        if (allStoreMappings.isEmpty()) {
            logger.debug("Post-initialization catch-up: no store mappings found, skipping");
            return;
        }

        // Group store mappings by consumerId (storeId)
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = allStoreMappings.stream()
                .collect(Collectors.groupingBy(PropertyMapping::getConsumerId));

        // Indexed lookup via shared helper — O(R × M × log F) via DependencyGraph.lookup() + NestedTreeMapIndex
        // Falls back to brute-force for mappings without a registered index (DRY: Task 3.3)
        RelatedEntityLookup dependencyGraphLookup = this::lookupRelatedEntities;

        // Apply mappings per store (Phase 2.5 catch-up)
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : mappingsByStore.entrySet()) {
            String storeId = entry.getKey();
            List<PropertyMapping<?, ?>> storeMappingList = entry.getValue();

            try {
                InMemoryDataStore<Object> store =
                        (InMemoryDataStore<Object>) factory.getStoreById(storeId);
                if (store == null) {
                    continue;
                }

                String primaryDs = store.getPrimaryDataSourceName();
                List<Object> rootEntities = dependencyGraph.findAll(primaryDs);

                if (rootEntities == null || rootEntities.isEmpty()) {
                    continue;
                }

                logger.debug("Post-init catch-up: applying {} mapping(s) to {} root entities for store '{}'",
                        storeMappingList.size(), rootEntities.size(), storeId);

                for (Object rootEntity : rootEntities) {
                    if (rootEntity == null) continue;
                    MappingApplicator.applyMappingsToEntity(dependencyGraphLookup, rootEntity, storeMappingList);
                }

                affectedPrimaryDatasources.add(primaryDs);
            } catch (Exception e) {
                logger.error("Post-init catch-up: error applying mappings for store '{}': {}",
                        storeId, e.getMessage(), e);
            }
        }

        // Post-init catch-up mutasyon sonrası: root entity'lerin datasource'larının index'lerini rebuild et.
        // Root entity'ler in-place mutasyona uğradı — bu entity'ler başka store'ların index'lerinde
        // foreign entity olarak bulunabilir. Index bucket key'lerin güncel FK değerleriyle eşleşmesi
        // için etkilenen datasource'ların index'leri yeniden oluşturulmalı.
        Set<String> rebuiltDatasources = new HashSet<>();
        for (String storeId : mappingsByStore.keySet()) {
            InMemoryDataStore<?> store = factory.getStoreById(storeId);
            if (store != null) {
                String primaryDs = store.getPrimaryDataSourceName();
                if (primaryDs != null && rebuiltDatasources.add(primaryDs)) {
                    dependencyGraph.rebuildIndexesForDataSource(primaryDs);
                    logger.debug("Post-init catch-up: Rebuilt indexes for datasource '{}' (store '{}')",
                            primaryDs, storeId);
                }
            }
        }

        // Phase 4 catch-up: propagate mapped data to consumers
        long version = streamingVersion.get();
        for (String primaryDs : affectedPrimaryDatasources) {
            applyPhase4ConsumerPropagation(primaryDs, version);
        }

        logger.debug("Post-initialization catch-up complete: {} store(s) processed, {} datasource(s) propagated, {} datasource index(es) rebuilt",
                mappingsByStore.size(), affectedPrimaryDatasources.size(), rebuiltDatasources.size());
    }


    // === Private helpers ===

    /**
     * Phase 2.5 (Delta): Apply store mapping transformations only to affected root entities.
     *
     * <p>Uses precomputed store mapping groupings ({@link #getStoreMappings()}) instead of
     * runtime mapping collection. Determines affected root entities via
     * {@link #resolveAffectedRootEntityIds} and applies mappings only to those entities,
     * reducing complexity from O(S × R × M × log F) to O(S × A × M × log F) where A << R.</p>
     *
     * <p>Uses {@link FKLookupCache} to eliminate redundant FK lookups within the same event.</p>
     *
     * @param dataSourceName    the datasource whose data changed
     * @param affectedMappings  the set of affected mappings from Phase 2 (used by fallback)
     * @param eventEntities     the entities from the BatchSnapshotEvent that triggered this processing
     */
    @SuppressWarnings("unchecked")
    private void applyPhase2_5StoreMappings(String dataSourceName,
                                            Set<PropertyMapping<?, ?>> affectedMappings,
                                            List<?> eventEntities) {
        long totalStart = System.nanoTime();
        try {
            // --- Sub-step 1: Mapping collection ---
            long mappingCollectStart = System.nanoTime();
            Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = getStoreMappings();
            long mappingCollectNanos = System.nanoTime() - mappingCollectStart;

            if (mappingsByStore == null || mappingsByStore.isEmpty()) {
                return; // Early exit — no store mappings
            }

            // --- Sub-step 2: Entity filtering (delta resolution) ---
            long entityFilterStart = System.nanoTime();
            @SuppressWarnings("rawtypes")
            List rawEventEntities = (List) eventEntities;
            @SuppressWarnings("unchecked")
            Set<Object> affectedRootEntityIds = resolveAffectedRootEntityIds(
                    dataSourceName, rawEventEntities, mappingsByStore);
            long entityFilterNanos = System.nanoTime() - entityFilterStart;

            // Early exit if no root entities are affected (Requirement 1.4)
            if (affectedRootEntityIds.isEmpty()) {
                logger.debug("Phase 2.5 (delta): No affected root entities for datasource '{}', skipping",
                        dataSourceName);
                return;
            }

            logger.debug("Phase 2.5 (delta): {} affected root entity ID(s) for datasource '{}'",
                    affectedRootEntityIds.size(), dataSourceName);

            // Create event-scoped FK lookup cache (Task 1.1)
            FKLookupCache fkCache = new FKLookupCache();
            RelatedEntityLookup cachedLookup = (mapping, pkValues) ->
                    fkCache.getOrLookup(mapping, pkValues, this::lookupRelatedEntities);

            // Collect mutated entities per datasource for incremental index update (Task 5.2)
            Map<String, List<Object>> mutatedEntitiesByDs = new HashMap<>();

            // Counters for metrics
            int totalRootEntities = 0;
            int processedRootEntities = 0;
            int appliedMappingCount = 0;

            // --- Sub-step 3: Mapping application ---
            long mappingApplyStart = System.nanoTime();
            for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : mappingsByStore.entrySet()) {
                String storeId = entry.getKey();
                List<PropertyMapping<?, ?>> storeMappingList = entry.getValue();

                try {
                    InMemoryDataStore<Object> store =
                            (InMemoryDataStore<Object>) factory.getStoreById(storeId);
                    if (store == null) {
                        logger.warn("Phase 2.5 (delta): Store '{}' not found, skipping", storeId);
                        continue;
                    }

                    String primaryDs = store.getPrimaryDataSourceName();
                    List<Object> rootEntities = dependencyGraph.findAll(primaryDs);

                    if (rootEntities == null || rootEntities.isEmpty()) {
                        continue;
                    }

                    // Filter to only affected root entities (A << R)
                    int totalRoots = rootEntities.size();
                    totalRootEntities += totalRoots;
                    int processedCount = 0;
                    for (Object rootEntity : rootEntities) {
                        if (rootEntity == null) continue;
                        Object entityId = ((Identifiable<?>) rootEntity).getIdentity();
                        if (affectedRootEntityIds.contains(entityId)) {
                            MappingApplicator.applyMappingsToEntity(cachedLookup, rootEntity, storeMappingList);
                            processedCount++;
                            // Track mutated entity per datasource for incremental index update
                            mutatedEntitiesByDs
                                    .computeIfAbsent(primaryDs, k -> new ArrayList<>())
                                    .add(rootEntity);
                        }
                    }
                    processedRootEntities += processedCount;
                    appliedMappingCount += processedCount * storeMappingList.size();

                    logger.debug("Phase 2.5 (delta): Store '{}' — processed {}/{} root entities, {} mapping(s)",
                            storeId, processedCount, totalRoots, storeMappingList.size());

                } catch (Exception e) {
                    logger.error("Phase 2.5 (delta): Error applying mappings for store '{}': {}",
                            storeId, e.getMessage(), e);
                    // Continue with other stores — one store failure doesn't block others
                }
            }
            long mappingApplyNanos = System.nanoTime() - mappingApplyStart;

            // --- Sub-step 4: Index update ---
            long indexUpdateStart = System.nanoTime();
            int incrementalCount = 0;
            int fullRebuildCount = 0;
            for (Map.Entry<String, List<Object>> dsEntry : mutatedEntitiesByDs.entrySet()) {
                String primaryDs = dsEntry.getKey();
                List<Object> mutatedEntities = dsEntry.getValue();

                @SuppressWarnings("rawtypes")
                List rawMutated = (List) mutatedEntities;
                @SuppressWarnings("unchecked")
                boolean incrementalSuccess = dependencyGraph.updateIndexesForEntities(primaryDs, rawMutated);
                if (incrementalSuccess) {
                    incrementalCount++;
                } else {
                    logger.warn("Phase 2.5 (delta): Incremental index update failed or threshold exceeded for ds='{}' ({} mutated entities), falling back to full rebuild",
                            primaryDs, mutatedEntities.size());
                    dependencyGraph.rebuildIndexesForDataSource(primaryDs);
                    fullRebuildCount++;
                }
            }
            long indexUpdateNanos = System.nanoTime() - indexUpdateStart;

            // Collect cache metrics before clearing (Requirement 3.3)
            double cacheHitRate = fkCache.getHitRate();
            fkCache.clear();

            // Calculate skipped root entities
            int skippedRootEntities = totalRootEntities - processedRootEntities;

            // Convert nanos to millis for the summary log
            long mappingCollectMs = TimeUnit.NANOSECONDS.toMillis(mappingCollectNanos);
            long entityFilterMs = TimeUnit.NANOSECONDS.toMillis(entityFilterNanos);
            long mappingApplyMs = TimeUnit.NANOSECONDS.toMillis(mappingApplyNanos);
            long indexUpdateMs = TimeUnit.NANOSECONDS.toMillis(indexUpdateNanos);
            long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);

            // Single-line summary log (Requirements 5.1, 5.2, 5.3, 5.4)
            logger.debug("Phase2.5 ds={}: mappingCollect={}ms entityFilter={}ms mappingApply={}ms indexUpdate={}ms | processed={}/{} roots (skipped={}) | mappings={} | cacheHitRate={}% | total={}ms",
                    dataSourceName,
                    mappingCollectMs, entityFilterMs, mappingApplyMs, indexUpdateMs,
                    processedRootEntities, totalRootEntities, skippedRootEntities,
                    appliedMappingCount,
                    String.format("%.1f", cacheHitRate),
                    totalMs);
        } catch (Exception e) {
            // Fallback: delta-based processing failed, fall back to full application (Requirements 6.3, 6.4)
            logger.warn("Phase 2.5: Delta-based processing failed for datasource '{}', falling back to full application: {}",
                    dataSourceName, e.getMessage(), e);
            applyPhase2_5StoreMappingsFull(dataSourceName, affectedMappings);
        }
    }

    /**
     * Phase 2.5 (Full): Original full-application store mapping logic.
     *
     * <p>Applies mappings to ALL root entities in each store, regardless of which entities
     * were affected by the event. Preserved as a fallback for delta-based processing
     * (used by Task 4.3 fallback mechanism).</p>
     *
     * <p>Uses a DependencyGraph-backed {@link RelatedEntityLookup} for FK-based entity lookup
     * (in-memory FK matching with composite key support).</p>
     *
     * @param dataSourceName    the datasource whose data changed
     * @param affectedMappings  the set of affected mappings from Phase 2
     */
    @SuppressWarnings("unchecked")
    void applyPhase2_5StoreMappingsFull(String dataSourceName, Set<PropertyMapping<?, ?>> affectedMappings) {
        // Source 1: Store mappings from affectedMappings (event datasource = foreign datasource)
        Set<PropertyMapping<?, ?>> allStoreMappings = new LinkedHashSet<>();
        for (PropertyMapping<?, ?> m : affectedMappings) {
            if (!m.isForDashboard()) {
                allStoreMappings.add(m);
            }
        }

        // Source 2: Store mappings where event datasource = primary datasource
        // When a primary entity arrives, we need to apply mappings FROM foreign datasources TO it.
        // These mappings are keyed by the foreign datasource in DependencyGraph, so they won't
        // appear in affectedMappings (which is keyed by the event datasource).
        for (String storeId : factory.getAllStoreIds()) {
            InMemoryDataStore<?> store = factory.getStoreById(storeId);
            if (store != null && dataSourceName.equals(store.getPrimaryDataSourceName())) {
                List<PropertyMapping<?, ?>> storeMappingsForConsumer =
                        dependencyGraph.getMappingsByConsumerId(storeId);
                for (PropertyMapping<?, ?> m : storeMappingsForConsumer) {
                    if (!m.isForDashboard()) {
                        allStoreMappings.add(m);
                    }
                }
            }
        }

        if (allStoreMappings.isEmpty()) {
            return;
        }

        logger.debug("Phase 2.5 (full): Applying {} store mapping(s) for datasource '{}'",
                allStoreMappings.size(), dataSourceName);

        // Group store mappings by consumerId (storeId)
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = allStoreMappings.stream()
                .collect(Collectors.groupingBy(PropertyMapping::getConsumerId));

        // Indexed lookup via shared helper — O(R × M × log F) via DependencyGraph.lookup() + NestedTreeMapIndex
        // Falls back to brute-force for mappings without a registered index (DRY: Task 3.3)
        RelatedEntityLookup dependencyGraphLookup = this::lookupRelatedEntities;

        // Apply mappings per store — ALL root entities
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : mappingsByStore.entrySet()) {
            String storeId = entry.getKey();
            List<PropertyMapping<?, ?>> storeMappingList = entry.getValue();

            try {
                InMemoryDataStore<Object> store =
                        (InMemoryDataStore<Object>) factory.getStoreById(storeId);
                if (store == null) {
                    logger.warn("Phase 2.5 (full): Store '{}' not found, skipping", storeId);
                    continue;
                }

                String primaryDs = store.getPrimaryDataSourceName();
                List<Object> rootEntities = dependencyGraph.findAll(primaryDs);

                if (rootEntities == null || rootEntities.isEmpty()) {
                    logger.debug("Phase 2.5 (full): No root entities for store '{}' (primaryDs='{}'), skipping",
                            storeId, primaryDs);
                    continue;
                }

                logger.debug("Phase 2.5 (full): Applying {} mapping(s) to {} root entities for store '{}'",
                        storeMappingList.size(), rootEntities.size(), storeId);

                for (Object rootEntity : rootEntities) {
                    if (rootEntity == null) continue;
                    MappingApplicator.applyMappingsToEntity(dependencyGraphLookup, rootEntity, storeMappingList);
                }

                logger.debug("Phase 2.5 (full): Successfully applied mappings for store '{}'", storeId);

            } catch (Exception e) {
                logger.error("Phase 2.5 (full): Error applying mappings for store '{}': {}",
                        storeId, e.getMessage(), e);
                // Continue with other stores — one store failure doesn't block others
            }
        }

        // Phase 2.5 mutasyon sonrası: root entity'lerin datasource'larının index'lerini rebuild et.
        Set<String> rebuiltDatasources = new HashSet<>();
        for (String storeId : mappingsByStore.keySet()) {
            InMemoryDataStore<?> store = factory.getStoreById(storeId);
            if (store != null) {
                String primaryDs = store.getPrimaryDataSourceName();
                if (primaryDs != null && rebuiltDatasources.add(primaryDs)) {
                    dependencyGraph.rebuildIndexesForDataSource(primaryDs);
                    logger.debug("Phase 2.5 (full): Rebuilt indexes for datasource '{}' (store '{}')",
                            primaryDs, storeId);
                }
            }
        }

        logger.debug("Phase 2.5 (full) complete for datasource '{}': {} store(s) processed, {} datasource index(es) rebuilt",
                dataSourceName, mappingsByStore.size(), rebuiltDatasources.size());
    }

    /**
     * Phase 4: Propagate data from DependencyGraph to affected consumers.
     *
     * <p>For each affected {@link InMemoryDataStore}: calls
     * {@code dependencyGraph.findAll(dataSourceName)} to get a consistent snapshot,
     * then {@code store.updateData(entities, streamingVersion)} to perform an
     * atomic volatile reference swap.</p>
     *
     * <p>For each affected {@link Dashboard}: Phase 3 already applied aggregation
     * results directly to the dashboard data object, so no additional action is
     * needed here. Dashboard consumers read via {@code getData()}.</p>
     *
     * <p>Phase 4 runs ONLY after Phase 3 is fully complete.</p>
     *
     * @param dataSourceName   the datasource whose data changed
     * @param streamingVersion the version number for this update
     */
    @SuppressWarnings("unchecked")
    private void applyPhase4ConsumerPropagation(String dataSourceName, long streamingVersion) {
        AffectedConsumerSet consumers = dependencyGraph.getAffectedConsumers(dataSourceName);

        Set<String> storeIds = consumers.getStoreIds();
        Set<String> dashboardIds = consumers.getDashboardIds();

        // PropertyMapping-based propagation
        int updatedStores = 0;
        if (!storeIds.isEmpty() || !dashboardIds.isEmpty()) {

            // Propagate to affected stores
            for (String storeId : storeIds) {
                try {
                    InMemoryDataStore<Object> store =
                            (InMemoryDataStore<Object>) factory.getStoreById(storeId);
                    if (store == null) {
                        logger.warn("Phase 4: Store '{}' not found, skipping", storeId);
                        continue;
                    }

                    // FIX 1: Get entities from store's PRIMARY datasource (correct type)
                    // Phase 2.5 applied store mapping transformations via MappingApplicator.
                    // Entities in DependencyGraph now contain mapped values.
                    String primaryDs = store.getPrimaryDataSourceName();
                    List<Object> primaryEntities = dependencyGraph.findAll(primaryDs);

                    // FIX 2: Apply rootSpecification filter (consistent with full sync behavior)
                    List<Object> filteredEntities;
                    @SuppressWarnings("unchecked")
                    Specification<Object> rootSpec = (Specification<Object>) store.getRootSpecification();
                    if (rootSpec != null) {
                        filteredEntities = new ArrayList<>(primaryEntities.size());
                        for (Object entity : primaryEntities) {
                            if (rootSpec.test(entity)) {
                                filteredEntities.add(entity);
                            }
                        }
                        logger.debug("Phase 4: Applied rootSpecification filter for store '{}': {} -> {} entities",
                                storeId, primaryEntities.size(), filteredEntities.size());
                    } else {
                        filteredEntities = primaryEntities;
                    }

                    store.updateData(filteredEntities, streamingVersion);
                    updatedStores++;
                } catch (Exception e) {
                    logger.error("Phase 4: Error propagating data to store '{}': {}",
                            storeId, e.getMessage(), e);
                    // Continue with other stores
                }
            }

            // Dashboards: Phase 3 already applied aggregation results directly
            // to dashboard data objects. No additional propagation needed.
            // Log for traceability.
            if (!dashboardIds.isEmpty()) {
                logger.debug("Phase 4: {} dashboard(s) were updated in Phase 3 for datasource '{}'",
                        dashboardIds.size(), dataSourceName);
            }
        }

        // Primary datasource consumer discovery
        // Iterates all stores to find those whose primaryDataSourceName matches this datasource.
        // This ensures stores are updated even when no PropertyMapping exists for the datasource,
        // consistent with full sync's collectRootDataForStores() behavior.
        // Stores already updated via PropertyMapping are skipped to preserve existing behavior.
        int primaryUpdatedStores = 0;
        List<Object> cachedPrimaryEntities = null; // FIX 3: lazy cache for primary path
        List<String> allStoreIds = factory.getAllStoreIds();
        for (String storeId : allStoreIds) {
            // Skip stores already updated via PropertyMapping path
            if (storeIds.contains(storeId)) {
                continue;
            }

            try {
                @SuppressWarnings("unchecked")
                InMemoryDataStore<Object> store =
                        (InMemoryDataStore<Object>) factory.getStoreById(storeId);
                if (store == null) {
                    continue;
                }

                String primaryDsName = store.getPrimaryDataSourceName();
                if (primaryDsName == null || !primaryDsName.equals(dataSourceName)) {
                    continue;
                }

                logger.debug("Phase 4: Found primary datasource consumer — store '{}' has primaryDataSourceName='{}'",
                        storeId, dataSourceName);

                // FIX 3: Lazy init — call findAll once, reuse for subsequent stores
                if (cachedPrimaryEntities == null) {
                    cachedPrimaryEntities = dependencyGraph.findAll(dataSourceName);
                }

                // Apply rootSpecification filter for full sync consistency
                List<Object> filteredEntities;
                @SuppressWarnings("unchecked")
                Specification<Object> rootSpec = (Specification<Object>) store.getRootSpecification();
                if (rootSpec != null) {
                    filteredEntities = new ArrayList<>(cachedPrimaryEntities.size());
                    for (Object entity : cachedPrimaryEntities) {
                        if (rootSpec.test(entity)) {
                            filteredEntities.add(entity);
                        }
                    }
                    logger.debug("Phase 4: Applied rootSpecification filter for store '{}': {} -> {} entities",
                            storeId, cachedPrimaryEntities.size(), filteredEntities.size());
                } else {
                    filteredEntities = cachedPrimaryEntities;
                }

                store.updateData(filteredEntities, streamingVersion);
                primaryUpdatedStores++;

                logger.debug("Phase 4: Updated store '{}' via primary datasource '{}', {} entities, version={}",
                        storeId, dataSourceName, filteredEntities.size(), streamingVersion);
            } catch (Exception e) {
                logger.error("Phase 4: Error propagating data to primary consumer store '{}': {}",
                        storeId, e.getMessage(), e);
                // Continue with other stores
            }
        }

        if (primaryUpdatedStores > 0) {
            logger.debug("Phase 4: Primary datasource discovery updated {} store(s) for datasource '{}'",
                    primaryUpdatedStores, dataSourceName);
        }

        int totalUpdated = updatedStores + primaryUpdatedStores;
        if (totalUpdated == 0 && dashboardIds.isEmpty()) {
            logger.debug("Phase 4: No affected consumers for datasource '{}'", dataSourceName);
        } else {
            logger.debug("Phase 4 complete for datasource '{}': {} store(s) updated (PropertyMapping: {}, primary: {}), version={}",
                    dataSourceName, totalUpdated, updatedStores, primaryUpdatedStores, streamingVersion);
        }
    }

    /**
     * Applies TimeWindowRule filtering if a rule exists for the datasource.
     * Entities where specification.test(entity) == false are filtered OUT.
     * If no TimeWindowRule exists, all entities pass through.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> List<T> applyTimeWindowFilter(
            String dataSourceName, List<T> entities) {
        TimeWindowRule<T> timeWindowRule =
                (TimeWindowRule<T>) factory.getTimeWindowRule(dataSourceName);

        if (timeWindowRule == null) {
            return entities;
        }

        Specification<T> specification = timeWindowRule.getSpecificationFactory().get();
        List<T> filtered = new ArrayList<>(entities.size());

        for (T entity : entities) {
            try {
                if (specification.test(entity)) {
                    filtered.add(entity);
                } else {
                    logger.debug("Entity filtered out by TimeWindowRule for datasource '{}': id={}",
                            dataSourceName, entity.getIdentity());
                }
            } catch (Exception e) {
                logger.warn("Error evaluating TimeWindowRule for entity in datasource '{}', id={}: {}",
                        dataSourceName, entity.getIdentity(), e.getMessage());
                // Entity is filtered out on error (specification returned false/error)
            }
        }

        return filtered;
    }

    /**
     * Captures the old entity states from DependencyGraph BEFORE Phase 1 overwrites them.
     * This is needed by Phase 2 (Bug 6: FIRST/LAST comparison, Bug 7: ANY comparison)
     * and Phase 3 (Bug 5: incremental aggregation) to compare old vs new state.
     *
     * @param dataSourceName    the datasource name to look up entities in
     * @param incomingEntities  the incoming entities whose old states should be captured
     * @return map of entityId → old entity (the reference from DependencyGraph before upsert)
     */
    private <T extends Identifiable<?>> Map<Object, Object> captureOldEntityStates(
            String dataSourceName, List<T> incomingEntities) {
        Map<Object, Object> oldStates = new HashMap<>(incomingEntities.size());
        for (T incoming : incomingEntities) {
            Object entityId = incoming.getIdentity();
            Object existing = dependencyGraph.findById(dataSourceName, entityId);
            if (existing != null) {
                oldStates.put(entityId, existing);
            }
        }
        return oldStates;
    }

    /**
     * Captures old FIRST/LAST entity IDs for all affected mappings BEFORE Phase 1.
     * This allows Phase 2 to detect when the first/last element shifts to a different entity
     * after Phase 1 updates the DependencyGraph.
     *
     * @param dataSourceName the datasource being processed
     * @return map of composite key (consumerId:collOpIndex:FIRST/LAST) → old first/last entity ID
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> captureOldFirstLastIds(String dataSourceName) {
        Set<PropertyMapping<?, ?>> allMappings = collectAffectedMappings(dataSourceName);
        if (allMappings.isEmpty()) {
            return Collections.emptyMap();
        }

        // Single findAll call — reused for all mappings (Bug 4 fix: task 6.1)
        List<Object> allEntities = dependencyGraph.findAll(dataSourceName);
        if (allEntities.isEmpty()) {
            return Collections.emptyMap();
        }

        // Cache: (specHashCode + ":" + comparatorHashCode) → sorted filtered list (Bug 4 fix: task 6.2)
        Map<String, List<Object>> sortedCache = new HashMap<>();
        Map<String, Object> oldIds = new HashMap<>();

        for (PropertyMapping<?, ?> mapping : allMappings) {
            List<CollectionOperationMetadata<?, ?>> collOps = mapping.getSourceCollectionOperations();
            if (collOps == null || collOps.isEmpty()) {
                continue;
            }

            for (int i = 0; i < collOps.size(); i++) {
                CollectionOperationMetadata<?, ?> collOp = collOps.get(i);
                CollectionSelector selector = collOp.getSelector();

                if (selector != CollectionSelector.FIRST && selector != CollectionSelector.LAST) {
                    continue;
                }

                boolean isFirst = (selector == CollectionSelector.FIRST);
                Comparator comparator = collOp.getComparator();
                if (comparator == null) {
                    continue; // No comparator → will conservatively re-evaluate anyway
                }

                // Build cache key from specification + comparator hash codes
                Specification specification = collOp.getSpecification();
                String cacheKey = (specification != null ? specification.hashCode() : "null")
                        + ":" + comparator.hashCode();

                List<Object> sortedFiltered = sortedCache.get(cacheKey);
                if (sortedFiltered == null) {
                    // Cache miss — filter and sort, then cache the result
                    List<Object> filteredEntities;
                    if (specification != null) {
                        filteredEntities = new ArrayList<>();
                        for (Object entity : allEntities) {
                            try {
                                if (specification.test(entity)) {
                                    filteredEntities.add(entity);
                                }
                            } catch (Exception e) {
                                filteredEntities.add(entity);
                            }
                        }
                    } else {
                        filteredEntities = new ArrayList<>(allEntities);
                    }

                    if (filteredEntities.isEmpty()) {
                        continue;
                    }

                    // Sort and cache
                    try {
                        filteredEntities.sort(comparator);
                    } catch (Exception e) {
                        continue; // Sort failed → will conservatively re-evaluate anyway
                    }

                    sortedFiltered = filteredEntities;
                    sortedCache.put(cacheKey, sortedFiltered);
                }

                if (sortedFiltered.isEmpty()) {
                    continue;
                }

                Object targetEntity = isFirst
                        ? sortedFiltered.get(0)
                        : sortedFiltered.get(sortedFiltered.size() - 1);

                if (targetEntity instanceof Identifiable) {
                    String key = buildFirstLastKey(mapping.getConsumerId(), i, isFirst);
                    oldIds.put(key, ((Identifiable<?>) targetEntity).getIdentity());
                }
            }
        }

        return oldIds;
    }

    /**
     * Builds a composite key for the old first/last ID map.
     * Format: "consumerId:collOpIndex:FIRST" or "consumerId:collOpIndex:LAST"
     */
    private String buildFirstLastKey(String consumerId, int collOpIndex, boolean isFirst) {
        return consumerId + ":" + collOpIndex + ":" + (isFirst ? "FIRST" : "LAST");
    }

    /**
     * Detects entities that were filtered out by TimeWindowRule but already exist in DependencyGraph.
     * These entities no longer pass the specification and should be removed from DependencyGraph and indexes.
     *
     * @param dataSourceName    the datasource name
     * @param allEntities       the original incoming entities (before filtering)
     * @param filteredEntities  the entities that passed TimeWindowRule filtering
     * @return list of entities that exist in DependencyGraph but no longer pass the TimeWindowRule
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> List<T> detectRemovedEntities(
            String dataSourceName, List<T> allEntities, List<T> filteredEntities) {
        // If no entities were filtered out, nothing to remove
        if (allEntities.size() == filteredEntities.size()) {
            return Collections.emptyList();
        }

        // No TimeWindowRule means no filtering happened — filteredEntities == allEntities
        TimeWindowRule<T> timeWindowRule =
                (TimeWindowRule<T>) factory.getTimeWindowRule(dataSourceName);
        if (timeWindowRule == null) {
            return Collections.emptyList();
        }

        // Build set of IDs that passed the filter
        Set<Object> passedIds = new HashSet<>(filteredEntities.size());
        for (T entity : filteredEntities) {
            passedIds.add(entity.getIdentity());
        }

        // Find entities that were filtered out AND exist in DependencyGraph
        List<T> removed = new ArrayList<>();
        for (T entity : allEntities) {
            if (!passedIds.contains(entity.getIdentity())) {
                Object existing = dependencyGraph.findById(dataSourceName, entity.getIdentity());
                if (existing != null) {
                    removed.add((T) existing);
                }
            }
        }

        if (!removed.isEmpty()) {
            logger.debug("Detected {} entities in DependencyGraph that no longer pass TimeWindowRule for '{}'",
                    removed.size(), dataSourceName);
        }

        return removed;
    }

    /**
     * Removes expired entities from DependencyGraph and indexes.
     * Called after Phase 1 upsert to clean up entities that no longer pass TimeWindowRule.
     *
     * @param dataSourceName  the datasource name
     * @param removedEntities entities to remove from DependencyGraph and indexes
     */
    private <T extends Identifiable<?>> void removeExpiredEntities(
            String dataSourceName, List<T> removedEntities) {
        if (removedEntities.isEmpty()) {
            return;
        }

        // Remove from DependencyGraph entity store
        for (T entity : removedEntities) {
            try {
                dependencyGraph.removeById(dataSourceName, entity.getIdentity());
            } catch (Exception e) {
                logger.error("Error removing expired entity id={} from DependencyGraph for '{}': {}",
                        entity.getIdentity(), dataSourceName, e.getMessage(), e);
            }
        }

        // Remove from indexes
        try {
            dependencyGraph.removeFromIndexes(dataSourceName, removedEntities);
            logger.debug("Removed {} expired entities from DependencyGraph and indexes for '{}'",
                    removedEntities.size(), dataSourceName);
        } catch (Exception e) {
            logger.error("Error removing expired entities from indexes for '{}': {}",
                    dataSourceName, e.getMessage(), e);
        }
    }

    /**
     * Phase 1: Apply filtered entities to DependencyGraph atomically.
     * Every incoming entity is considered changed — no field-by-field comparison.
     * Single entity processing error is logged, remaining entities continue.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void applyPhase1EntityUpsert(
            String dataSourceName, List<T> filteredEntities, Map<Object, Object> oldEntityStates) {
        if (filteredEntities.isEmpty()) {
            logger.debug("No entities to upsert for datasource '{}' after filtering", dataSourceName);
            return;
        }

        // Extract old entities from oldEntityStates for incremental index update
        List<T> oldEntities = new ArrayList<>();
        for (T entity : filteredEntities) {
            Object oldEntity = oldEntityStates.get(entity.getIdentity());
            if (oldEntity != null) {
                oldEntities.add((T) oldEntity);
            }
        }

        try {
            dependencyGraph.upsertAll(dataSourceName, filteredEntities);
            logger.debug("Phase 1 complete for datasource '{}': {} entities upserted",
                    dataSourceName, filteredEntities.size());
        } catch (Exception e) {
            logger.error("Error during Phase 1 (entity upsert) for datasource '{}': {}",
                    dataSourceName, e.getMessage(), e);
            // Fall back to individual entity processing so remaining entities are not lost
            upsertEntitiesIndividually(dataSourceName, filteredEntities);
        }

        // Bug 1 fix: Update indexes incrementally — remove old, insert new
        try {
            dependencyGraph.updateIndexes(dataSourceName, oldEntities, filteredEntities);
            logger.debug("Phase 1: Indexes updated for datasource '{}' with {} entities",
                    dataSourceName, filteredEntities.size());
        } catch (Exception e) {
            logger.error("Phase 1: Error updating indexes for datasource '{}': {}",
                    dataSourceName, e.getMessage(), e);
        }
    }


    /**
     * Phase 2: Detect and re-evaluate affected PropertyMappings.
     *
     * <p>Uses {@link AnalysisResult}'s common grouping definitions and
     * {@link DependencyGraph}'s dependency map to find all mappings affected
     * by the changed datasource. For each affected mapping, applies the
     * {@link CollectionOperationMetadata} re-evaluation strategy:</p>
     * <ul>
     *   <li><b>ALL</b> — mapping is fully re-evaluated</li>
     *   <li><b>FIRST</b> — re-sort collection, check if first element changed</li>
     *   <li><b>LAST</b> — re-sort collection, check if last element changed</li>
     *   <li><b>ANY</b> — re-evaluate specification on changed entities</li>
     * </ul>
     *
     * <p>Conservative approach: in case of doubt, the mapping is marked for
     * re-evaluation. False positives are acceptable; false negatives are NOT.</p>
     *
     * @param dataSourceName  the datasource whose entities changed
     * @param changedEntities the entities that were upserted in Phase 1
     * @return the set of mappings that need re-evaluation (for Phase 3 and 4)
     */
    private <T extends Identifiable<?>> Set<PropertyMapping<?, ?>> applyPhase2MappingUpdates(
            String dataSourceName, List<T> changedEntities, Map<String, Object> oldFirstLastIds,
            Map<Object, Object> oldEntityStates) {

        if (changedEntities.isEmpty()) {
            logger.debug("Phase 2: No changed entities for datasource '{}', skipping", dataSourceName);
            return Collections.emptySet();
        }

        // Collect all affected mappings from both DependencyGraph and AnalysisResult
        Set<PropertyMapping<?, ?>> allAffectedMappings = collectAffectedMappings(dataSourceName);

        if (allAffectedMappings.isEmpty()) {
            logger.debug("Phase 2: No affected mappings found for datasource '{}'", dataSourceName);
            return Collections.emptySet();
        }

        logger.debug("Phase 2: Found {} affected mapping(s) for datasource '{}'",
                allAffectedMappings.size(), dataSourceName);

        // Determine which mappings need re-evaluation based on CollectionOperationMetadata
        Set<PropertyMapping<?, ?>> mappingsToReevaluate = new LinkedHashSet<>();
        Set<Object> changedEntityIds = collectEntityIds(changedEntities);

        for (PropertyMapping<?, ?> mapping : allAffectedMappings) {
            try {
                if (shouldReevaluateMapping(mapping, dataSourceName, changedEntityIds, oldFirstLastIds, oldEntityStates)) {
                    mappingsToReevaluate.add(mapping);
                }
            } catch (Exception e) {
                // Conservative: on error, mark for re-evaluation
                logger.warn("Phase 2: Error evaluating mapping for consumer '{}', "
                        + "conservatively marking for re-evaluation: {}",
                        mapping.getConsumerId(), e.getMessage());
                mappingsToReevaluate.add(mapping);
            }
        }

        logger.debug("Phase 2 complete for datasource '{}': {}/{} mappings marked for re-evaluation",
                dataSourceName, mappingsToReevaluate.size(), allAffectedMappings.size());

        return Collections.unmodifiableSet(mappingsToReevaluate);
    }

    /**
     * Phase 3: Update affected dashboard aggregations.
     *
     * <p>Uses {@link AnalysisResult}'s {@link DashboardAggregationPlan} and
     * {@link AggregationTask} lists to detect affected dashboards. For each
     * affected task, recomputes aggregation results from the full dataset in
     * DependencyGraph and applies them to the dashboard data object.</p>
     *
     * <p>Phase 3 starts ONLY after Phase 2 is fully complete.</p>
     *
     * @param dataSourceName   the datasource whose entities changed
     * @param changedEntities  the entities that were processed in Phase 1
     * @param affectedMappings the set of affected mappings from Phase 2
     * @param oldEntityStates  old entity states captured before Phase 1 (entityId → old entity)
     * @param removedEntities  entities removed by TimeWindowRule filtering
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void applyPhase3AggregationUpdates(
            String dataSourceName,
            List<T> changedEntities,
            Set<PropertyMapping<?, ?>> affectedMappings,
            Map<Object, Object> oldEntityStates,
            List<T> removedEntities) {

        AnalysisResult currentAnalysis = this.analysisResult;
        if (currentAnalysis == null || !currentAnalysis.hasDashboardPlans()) {
            logger.debug("Phase 3: No dashboard aggregation plans, skipping");
            return;
        }

        if (changedEntities.isEmpty() && removedEntities.isEmpty()) {
            logger.debug("Phase 3: No changed or removed entities for datasource '{}', skipping", dataSourceName);
            return;
        }

        int updatedDashboards = 0;

        for (Map.Entry<String, DashboardAggregationPlan> entry
                : currentAnalysis.dashboardAggregationPlans().entrySet()) {
            String dashboardId = entry.getKey();
            DashboardAggregationPlan plan = entry.getValue();

            if (!plan.hasTasks()) {
                continue;
            }

            // Check if any task in this plan references the changed datasource
            boolean isAffected = false;
            for (AggregationTask task : plan.getTasks()) {
                if (dataSourceName.equals(task.getDataSourceName())) {
                    isAffected = true;
                    break;
                }
            }

            if (!isAffected) {
                continue;
            }

            try {
                // Get the dashboard instance to update its data object
                Dashboard<?> dashboard = factory.getDashboardById(dashboardId);
                if (dashboard == null) {
                    logger.warn("Phase 3: Dashboard '{}' not found, skipping", dashboardId);
                    continue;
                }

                Object dashboardData = dashboard.getData();
                if (dashboardData == null) {
                    logger.info("Phase 3: Dashboard '{}' has no data yet, initializing on-demand via targetClass", dashboardId);
                    try {
                        Class<?> targetClass = dashboard.getTargetClass();
                        Object newInstance = targetClass.getDeclaredConstructor().newInstance();
                        ((Dashboard<Object>) dashboard).updateData(newInstance);
                        dashboardData = newInstance;
                    } catch (ReflectiveOperationException e) {
                        logger.warn("Phase 3: Failed to initialize dashboard data on-demand for '{}', skipping: {}",
                                dashboardId, e.getMessage(), e);
                        continue;
                    }
                }

                // Process each task that references the changed datasource
                for (AggregationTask task : plan.getTasks()) {
                    if (!dataSourceName.equals(task.getDataSourceName())) {
                        continue;
                    }

                    try {
                        applyAggregationTask(dashboardId, dashboardData, task,
                                changedEntities, oldEntityStates, removedEntities);
                    } catch (Exception e) {
                        logger.error("Phase 3: Error processing aggregation task for dashboard '{}': {}",
                                dashboardId, e.getMessage(), e);
                        // Continue with other tasks
                    }
                }

                updatedDashboards++;
            } catch (Exception e) {
                logger.error("Phase 3: Error updating dashboard '{}': {}",
                        dashboardId, e.getMessage(), e);
                // Continue with other dashboards
            }
        }

        logger.debug("Phase 3 complete for datasource '{}': {} dashboard(s) updated, changedEntities={}, removedEntities={}",
                dataSourceName, updatedDashboards, changedEntities.size(), removedEntities.size());
    }

    /**
     * Applies a single aggregation task: computes aggregation results from
     * DependencyGraph data and sets them on the dashboard data object.
     *
     * <p>Bug 5 fix: Uses incremental computation when a cached {@link AggregationState}
     * exists for this dashboard+task. On the first call (no cached state), a full scan
     * is performed and the state is cached. Subsequent calls compute deltas from
     * changed and removed entities only.</p>
     *
     * <p>For COUNT/SUM/AVG, incremental computation avoids {@code findAll()}.
     * For MIN/MAX, a full scan is needed only if the current min/max entity
     * was among the changed or removed entities.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Identifiable<?>> void applyAggregationTask(
            String dashboardId,
            Object dashboardData,
            AggregationTask task,
            List<T> changedEntities,
            Map<Object, Object> oldEntityStates,
            List<T> removedEntities) {

        String taskDataSourceName = task.getDataSourceName();
        List<MetaAttribute<?, ?>> fieldPath = task.getFieldPath();
        Set<AggregationType> aggregationTypes = task.getAggregationTypes();

        if (aggregationTypes.isEmpty()) {
            return;
        }

        // Build a unique key for this dashboard+task combination
        String stateKey = buildAggregationStateKey(dashboardId, taskDataSourceName, fieldPath);

        // Look up cached state
        AggregationState cachedState = aggregationStateCache.get(stateKey);

        Map<AggregationType, Object> results;

        if (cachedState == null) {
            // First computation: full scan, then cache the state
            results = computeAggregationsFullScan(taskDataSourceName, fieldPath, aggregationTypes, task, stateKey);
        } else {
            // Incremental computation using cached state + deltas
            results = computeAggregationsIncremental(
                    cachedState, taskDataSourceName, fieldPath, aggregationTypes, task, stateKey,
                    changedEntities, oldEntityStates, removedEntities);
        }

        // Apply results to dashboard data object via each mapping's target path
        applyAggregationResults(dashboardId, dashboardData, aggregationTypes, results, task);
    }

    /**
     * Builds a unique key for the aggregation state cache.
     * Format: "dashboardId:dataSourceName:fieldPathIdentityHash"
     */
    private String buildAggregationStateKey(
            String dashboardId, String dataSourceName, List<MetaAttribute<?, ?>> fieldPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(dashboardId).append(':').append(dataSourceName).append(':');
        if (fieldPath != null) {
            for (MetaAttribute<?, ?> attr : fieldPath) {
                sb.append(System.identityHashCode(attr)).append('.');
            }
        }
        return sb.toString();
    }

    /**
     * Full scan computation: iterates all entities from DependencyGraph, computes
     * aggregation values, caches the state, and returns results.
     */
    private Map<AggregationType, Object> computeAggregationsFullScan(
            String dataSourceName,
            List<MetaAttribute<?, ?>> fieldPath,
            Set<AggregationType> aggregationTypes,
            AggregationTask task,
            String stateKey) {

        List<?> allEntities = dependencyGraph.findAll(dataSourceName);

        // Apply specification filter if the task has one (Bug 3 fix)
        Specification<?> spec = task.getSpecification();
        if (spec != null) {
            allEntities = filterBySpecification(allEntities, spec);
        }

        Map<AggregationType, Object> results = computeAggregationsInSinglePass(
                allEntities, fieldPath, aggregationTypes, task);

        // Cache the state for future incremental updates
        AggregationState state = new AggregationState();
        if (results.containsKey(AggregationType.COUNT)) {
            state.setCount(((Number) results.get(AggregationType.COUNT)).longValue());
        } else {
            state.setCount(allEntities.size());
        }
        if (results.containsKey(AggregationType.SUM)) {
            state.setSum(((Number) results.get(AggregationType.SUM)).doubleValue());
        }
        if (results.containsKey(AggregationType.AVG)) {
            // Recompute sum from all entities for accurate caching
            double sum = 0.0;
            SpecificationService sourceService = resolveSourceService(task);
            for (Object entity : allEntities) {
                if (entity == null) continue;
                Double val = extractNumericValue(entity, fieldPath, sourceService);
                if (val != null) sum += val;
            }
            state.setSum(sum);
        }
        if (results.containsKey(AggregationType.MIN) && results.get(AggregationType.MIN) != null) {
            state.setMin(((Number) results.get(AggregationType.MIN)).doubleValue());
            state.setMinEntityId(findEntityIdWithValue(allEntities, fieldPath, task,
                    state.getMin()));
        }
        if (results.containsKey(AggregationType.MAX) && results.get(AggregationType.MAX) != null) {
            state.setMax(((Number) results.get(AggregationType.MAX)).doubleValue());
            state.setMaxEntityId(findEntityIdWithValue(allEntities, fieldPath, task,
                    state.getMax()));
        }

        aggregationStateCache.put(stateKey, state);
        logger.trace("Phase 3: Cached aggregation state for key '{}': {}", stateKey, state);

        return results;
    }

    /**
     * Incremental computation: uses cached state and applies deltas from changed
     * and removed entities. Falls back to full scan for MIN/MAX when the current
     * min/max entity is affected.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> Map<AggregationType, Object> computeAggregationsIncremental(
            AggregationState cachedState,
            String dataSourceName,
            List<MetaAttribute<?, ?>> fieldPath,
            Set<AggregationType> aggregationTypes,
            AggregationTask task,
            String stateKey,
            List<T> changedEntities,
            Map<Object, Object> oldEntityStates,
            List<T> removedEntities) {

        boolean needsMinMax = aggregationTypes.contains(AggregationType.MIN)
                || aggregationTypes.contains(AggregationType.MAX);

        // Check if MIN/MAX entity is among changed or removed — if so, need full scan for MIN/MAX
        boolean minMaxAffected = false;
        if (needsMinMax) {
            Set<Object> affectedIds = new HashSet<>();
            for (T entity : changedEntities) {
                affectedIds.add(entity.getIdentity());
            }
            for (T entity : removedEntities) {
                affectedIds.add(entity.getIdentity());
            }
            if ((cachedState.getMinEntityId() != null && affectedIds.contains(cachedState.getMinEntityId()))
                    || (cachedState.getMaxEntityId() != null && affectedIds.contains(cachedState.getMaxEntityId()))) {
                minMaxAffected = true;
            }
        }

        SpecificationService sourceService = resolveSourceService(task);
        Specification<?> spec = task.getSpecification();

        long count = cachedState.getCount();
        double sum = cachedState.getSum();

        // Process changed entities (upserted in Phase 1)
        for (T newEntity : changedEntities) {
            Object entityId = newEntity.getIdentity();
            Object oldEntity = oldEntityStates.get(entityId);

            boolean newPassesSpec = entityPassesSpecification(newEntity, spec);

            if (oldEntity == null) {
                // New insert — only count if new entity passes spec
                if (newPassesSpec) {
                    count++;
                    Double newValue = extractNumericValue(newEntity, fieldPath, sourceService);
                    if (newValue != null) {
                        sum += newValue;
                    }
                }
            } else {
                // Update — handle spec transition combinations
                boolean oldPassesSpec = entityPassesSpecification(oldEntity, spec);
                Double newValue = newPassesSpec ? extractNumericValue(newEntity, fieldPath, sourceService) : null;
                Double oldValue = oldPassesSpec ? extractNumericValue(oldEntity, fieldPath, sourceService) : null;

                if (oldPassesSpec && newPassesSpec) {
                    // Both pass spec → normal delta (subtract old, add new), count unchanged
                    if (oldValue != null) {
                        sum -= oldValue;
                    }
                    if (newValue != null) {
                        sum += newValue;
                    }
                } else if (oldPassesSpec) {
                    // Old passes, new fails → treat as removal
                    count--;
                    if (oldValue != null) {
                        sum -= oldValue;
                    }
                } else if (newPassesSpec) {
                    // Old fails, new passes → treat as addition
                    count++;
                    if (newValue != null) {
                        sum += newValue;
                    }
                }
                // else: both fail spec → skip (no impact on aggregation)
            }
        }

        // Process removed entities (expired by TimeWindowRule)
        for (T removedEntity : removedEntities) {
            // Only subtract if the removed entity passed the spec
            if (entityPassesSpecification(removedEntity, spec)) {
                Double oldValue = extractNumericValue(removedEntity, fieldPath, sourceService);
                count--;
                if (oldValue != null) {
                    sum -= oldValue;
                }
            }
        }

        // Build results
        Map<AggregationType, Object> results = new EnumMap<>(AggregationType.class);

        if (aggregationTypes.contains(AggregationType.COUNT)) {
            results.put(AggregationType.COUNT, count);
        }
        if (aggregationTypes.contains(AggregationType.SUM)) {
            results.put(AggregationType.SUM, sum);
        }
        if (aggregationTypes.contains(AggregationType.AVG)) {
            results.put(AggregationType.AVG, count > 0 ? sum / count : 0.0);
        }

        // For MIN/MAX: if affected, do full scan; otherwise check new values against cached bounds
        if (needsMinMax) {
            if (minMaxAffected) {
                // Full scan needed for MIN/MAX only — apply spec filter
                List<?> allEntities = dependencyGraph.findAll(dataSourceName);
                if (spec != null) {
                    allEntities = filterBySpecification(allEntities, spec);
                }
                computeMinMaxFromEntities(allEntities, fieldPath, sourceService, aggregationTypes, results,
                        cachedState);
            } else {
                // Check if any new value exceeds current bounds
                computeMinMaxIncremental(changedEntities, oldEntityStates, fieldPath, sourceService,
                        aggregationTypes, results, cachedState);
            }
        }

        // Update cached state
        cachedState.setCount(count);
        cachedState.setSum(sum);
        aggregationStateCache.put(stateKey, cachedState);

        logger.trace("Phase 3: Incremental aggregation for key '{}': {}", stateKey, cachedState);

        return results;
    }

    /**
     * Extracts a numeric value from an entity using the given field path.
     * Returns null if the value is null or non-numeric.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Double extractNumericValue(Object entity, List<MetaAttribute<?, ?>> fieldPath,
                                       SpecificationService sourceService) {
        if (entity == null) return null;

        Object value;
        if (fieldPath != null && !fieldPath.isEmpty() && sourceService != null) {
            try {
                value = sourceService.getValueByPath(entity, fieldPath);
            } catch (Exception e) {
                return null;
            }
        } else {
            value = entity;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Finds the entity ID that holds the given min or max value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object findEntityIdWithValue(List<?> entities, List<MetaAttribute<?, ?>> fieldPath,
                                         AggregationTask task, double targetValue) {
        SpecificationService sourceService = resolveSourceService(task);
        for (Object entity : entities) {
            if (entity == null) continue;
            Double val = extractNumericValue(entity, fieldPath, sourceService);
            if (val != null && Double.compare(val, targetValue) == 0) {
                if (entity instanceof Identifiable<?>) {
                    return ((Identifiable<?>) entity).getIdentity();
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Computes MIN/MAX from a full entity scan (used when current min/max entity is affected).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void computeMinMaxFromEntities(
            List<?> allEntities,
            List<MetaAttribute<?, ?>> fieldPath,
            SpecificationService sourceService,
            Set<AggregationType> aggregationTypes,
            Map<AggregationType, Object> results,
            AggregationState cachedState) {

        Double min = null;
        Double max = null;
        Object minId = null;
        Object maxId = null;

        for (Object entity : allEntities) {
            if (entity == null) continue;
            Double val = extractNumericValue(entity, fieldPath, sourceService);
            if (val == null) continue;

            Object entityId = (entity instanceof Identifiable<?>) ? ((Identifiable<?>) entity).getIdentity() : null;

            if (min == null || val < min) {
                min = val;
                minId = entityId;
            }
            if (max == null || val > max) {
                max = val;
                maxId = entityId;
            }
        }

        if (aggregationTypes.contains(AggregationType.MIN)) {
            results.put(AggregationType.MIN, min);
        }
        if (aggregationTypes.contains(AggregationType.MAX)) {
            results.put(AggregationType.MAX, max);
        }

        cachedState.setMin(min);
        cachedState.setMinEntityId(minId);
        cachedState.setMax(max);
        cachedState.setMaxEntityId(maxId);
    }

    /**
     * Incremental MIN/MAX: checks if any new value from changed entities exceeds
     * the current cached bounds. If not, the cached values are reused.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void computeMinMaxIncremental(
            List<T> changedEntities,
            Map<Object, Object> oldEntityStates,
            List<MetaAttribute<?, ?>> fieldPath,
            SpecificationService sourceService,
            Set<AggregationType> aggregationTypes,
            Map<AggregationType, Object> results,
            AggregationState cachedState) {

        Double currentMin = cachedState.getMin();
        Double currentMax = cachedState.getMax();
        Object minId = cachedState.getMinEntityId();
        Object maxId = cachedState.getMaxEntityId();

        for (T entity : changedEntities) {
            Double val = extractNumericValue(entity, fieldPath, sourceService);
            if (val == null) continue;

            Object entityId = entity.getIdentity();

            if (currentMin == null || val < currentMin) {
                currentMin = val;
                minId = entityId;
            }
            if (currentMax == null || val > currentMax) {
                currentMax = val;
                maxId = entityId;
            }
        }

        if (aggregationTypes.contains(AggregationType.MIN)) {
            results.put(AggregationType.MIN, currentMin);
        }
        if (aggregationTypes.contains(AggregationType.MAX)) {
            results.put(AggregationType.MAX, currentMax);
        }

        cachedState.setMin(currentMin);
        cachedState.setMinEntityId(minId);
        cachedState.setMax(currentMax);
        cachedState.setMaxEntityId(maxId);
    }

    /**
     * Applies computed aggregation results to the dashboard data object via
     * each mapping's target path.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyAggregationResults(
            String dashboardId,
            Object dashboardData,
            Set<AggregationType> aggregationTypes,
            Map<AggregationType, Object> results,
            AggregationTask task) {

        for (AggregationType aggType : aggregationTypes) {
            Object result = results.get(aggType);
            List<PropertyMapping<?, ?>> mappings = task.getMappings(aggType);

            for (PropertyMapping<?, ?> mapping : mappings) {
                try {
                    List<MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
                    if (targetPath == null || targetPath.isEmpty()) {
                        logger.warn("Phase 3: Empty target path for mapping on dashboard '{}'", dashboardId);
                        continue;
                    }

                    MetaAttribute<?, ?> targetAttr = targetPath.get(targetPath.size() - 1);
                    Object convertedResult = convertAggregationResult(
                            result, targetAttr.getFieldType(), aggType);

                    SpecificationService targetService = mapping.getTargetService();
                    List targetCollectionOps = mapping.getTargetCollectionOperations();

                    if (targetCollectionOps != null && !targetCollectionOps.isEmpty()) {
                        targetService.setValueByPathWithCollections(
                                dashboardData, targetPath, targetCollectionOps, convertedResult);
                    } else {
                        targetService.setValueByPath(dashboardData, targetPath, convertedResult);
                    }

                    logger.trace("Phase 3: Set dashboard '{}' field '{}' = {} ({})",
                            dashboardId, targetAttr.getName(), convertedResult, aggType);

                } catch (Exception e) {
                    logger.warn("Phase 3: Error setting aggregation result on dashboard '{}': {}",
                            dashboardId, e.getMessage());
                }
            }
        }
    }

    /**
     * Computes multiple aggregation types in a single pass over the entity list.
     * Uses the same accumulator pattern as the full sync in DataSynchronizationEngine.
     *
     * @param entities         all entities from DependencyGraph for the datasource
     * @param fieldPath        the field path to extract values from
     * @param aggregationTypes the set of aggregation types to compute
     * @param task             the aggregation task (used to get source service from mappings)
     * @return map of aggregation type to computed result
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<AggregationType, Object> computeAggregationsInSinglePass(
            List<?> entities,
            List<MetaAttribute<?, ?>> fieldPath,
            Set<AggregationType> aggregationTypes,
            AggregationTask task) {

        Map<AggregationType, Object> results = new EnumMap<>(AggregationType.class);

        if (entities == null || entities.isEmpty()) {
            for (AggregationType type : aggregationTypes) {
                results.put(type, getDefaultAggregationValue(type));
            }
            return results;
        }

        // Get a SpecificationService from any mapping in the task for field value extraction
        SpecificationService sourceService = resolveSourceService(task);

        // Initialize accumulators
        long count = 0;
        double sum = 0.0;
        Double min = null;
        Double max = null;

        boolean needsSum = aggregationTypes.contains(AggregationType.SUM)
                || aggregationTypes.contains(AggregationType.AVG);
        boolean needsMin = aggregationTypes.contains(AggregationType.MIN);
        boolean needsMax = aggregationTypes.contains(AggregationType.MAX);

        // Single pass over all entities
        for (Object entity : entities) {
            if (entity == null) {
                continue;
            }

            try {
                count++;

                // For COUNT-only tasks, no need to extract field values
                if (!needsSum && !needsMin && !needsMax) {
                    continue;
                }

                // Extract field value
                Object value;
                if (fieldPath != null && !fieldPath.isEmpty() && sourceService != null) {
                    value = sourceService.getValueByPath(entity, fieldPath);
                } else {
                    value = entity;
                }

                if (value == null) {
                    continue;
                }

                // Convert to double for numeric aggregations
                double numValue;
                if (value instanceof Number) {
                    numValue = ((Number) value).doubleValue();
                } else {
                    continue; // Skip non-numeric values
                }

                if (needsSum) {
                    sum += numValue;
                }
                if (needsMin) {
                    if (min == null || numValue < min) {
                        min = numValue;
                    }
                }
                if (needsMax) {
                    if (max == null || numValue > max) {
                        max = numValue;
                    }
                }

            } catch (Exception e) {
                logger.warn("Phase 3: Error processing entity for aggregation: {}", e.getMessage());
                // Continue with other entities
            }
        }

        // Store results
        if (aggregationTypes.contains(AggregationType.COUNT)) {
            results.put(AggregationType.COUNT, count);
        }
        if (aggregationTypes.contains(AggregationType.SUM)) {
            results.put(AggregationType.SUM, sum);
        }
        if (aggregationTypes.contains(AggregationType.AVG)) {
            results.put(AggregationType.AVG, count > 0 ? sum / count : 0.0);
        }
        if (needsMin) {
            results.put(AggregationType.MIN, min);
        }
        if (needsMax) {
            results.put(AggregationType.MAX, max);
        }

        return results;
    }

    /**
     * Resolves a SpecificationService from the task's mappings for field value extraction.
     * Returns null if no service can be resolved (e.g., COUNT-only tasks with no field path).
     */
    @SuppressWarnings("rawtypes")
    private SpecificationService resolveSourceService(AggregationTask task) {
        for (List<PropertyMapping<?, ?>> mappings : task.getMappingsByAggregationType().values()) {
            for (PropertyMapping<?, ?> mapping : mappings) {
                SpecificationService<?> service = mapping.getSourceService();
                if (service != null) {
                    return service;
                }
            }
        }
        return null;
    }

    /**
     * Returns the default value for an aggregation type when there are no entities.
     */
    private Object getDefaultAggregationValue(AggregationType type) {
        return switch (type) {
            case COUNT -> 0L;
            case SUM -> 0.0;
            case AVG -> 0.0;
            case MIN, MAX -> null;
            default -> null;
        };
    }

    /**
     * Converts an aggregation result to the target field type.
     * Handles common numeric type conversions.
     */
    private Object convertAggregationResult(Object result, Class<?> targetType, AggregationType aggType) {
        if (result == null) {
            return null;
        }

        if (targetType == null || targetType.isAssignableFrom(result.getClass())) {
            return result;
        }

        if (result instanceof Number numResult) {
            if (targetType == Integer.class || targetType == int.class) {
                return numResult.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return numResult.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return numResult.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return numResult.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return numResult.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return numResult.byteValue();
            }
        }

        logger.warn("Phase 3: Could not convert aggregation result of type {} to {} for {}",
                result.getClass().getSimpleName(), targetType.getSimpleName(), aggType);
        return result;
    }

    /**
     * Collects all PropertyMappings affected by a datasource change.
     * Combines mappings from DependencyGraph's dependency map and
     * AnalysisResult's common groupings.
     */
    private Set<PropertyMapping<?, ?>> collectAffectedMappings(String dataSourceName) {
        Set<PropertyMapping<?, ?>> result = new LinkedHashSet<>();

        // Source 1: DependencyGraph dependency map
        List<PropertyMapping<?, ?>> dgMappings =
                dependencyGraph.getMappingsForDataSource(dataSourceName);
        result.addAll(dgMappings);

        // Source 2: AnalysisResult common groupings
        AnalysisResult currentAnalysis = this.analysisResult;
        if (currentAnalysis != null && currentAnalysis.hasCommonGroupings()) {
            for (var entry : currentAnalysis.commonGroupings().entrySet()) {
                GroupingKey key = entry.getKey();
                if (dataSourceName.equals(key.dataSourceName())) {
                    result.addAll(entry.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Collects entity IDs from a list of changed entities into a Set for fast lookup.
     */
    private <T extends Identifiable<?>> Set<Object> collectEntityIds(List<T> entities) {
        Set<Object> ids = new LinkedHashSet<>(entities.size());
        for (T entity : entities) {
            ids.add(entity.getIdentity());
        }
        return ids;
    }

    /**
     * Resolves the set of root entity IDs affected by a batch snapshot event.
     *
     * <p>Two scenarios are handled:</p>
     * <ul>
     *   <li><b>Foreign datasource:</b> When the event datasource matches a mapping's
     *       {@code dataSourceName}, FK values are extracted from event entities and matched
     *       against root entity PK values in the store's primary datasource.</li>
     *   <li><b>Primary datasource:</b> When the event datasource matches a store's
     *       {@code primaryDataSourceName}, event entity IDs are directly the affected
     *       root entity IDs.</li>
     * </ul>
     *
     * <p>Complexity: O(A × K) for FK extraction + O(R × K) for PK matching per mapping,
     * where A = event entity count, K = key field count, R = root entity count.</p>
     *
     * @param dataSourceName  the datasource that produced the event
     * @param eventEntities   entities from the batch snapshot event
     * @param mappingsByStore store mappings grouped by storeId (consumerId)
     * @return combined set of affected root entity IDs across all stores
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Identifiable<?>> Set<Object> resolveAffectedRootEntityIds(
            String dataSourceName,
            List<T> eventEntities,
            Map<String, List<PropertyMapping<?, ?>>> mappingsByStore) {

        if (eventEntities == null || eventEntities.isEmpty()
                || mappingsByStore == null || mappingsByStore.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Object> affectedRootEntityIds = new LinkedHashSet<>();

        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : mappingsByStore.entrySet()) {
            String storeId = entry.getKey();
            List<PropertyMapping<?, ?>> storeMappings = entry.getValue();

            InMemoryDataStore<?> store = factory.getStoreById(storeId);
            if (store == null) {
                continue;
            }

            String primaryDs = store.getPrimaryDataSourceName();

            // Scenario 2: Event datasource = primary datasource
            // Event entities ARE root entities — their IDs are directly affected
            if (dataSourceName.equals(primaryDs)) {
                for (T entity : eventEntities) {
                    affectedRootEntityIds.add(entity.getIdentity());
                }
            }

            // Scenario 1: Event datasource = foreign datasource
            // Check each mapping to see if its dataSourceName matches the event datasource
            for (PropertyMapping<?, ?> mapping : storeMappings) {
                if (!dataSourceName.equals(mapping.getDataSourceName())) {
                    continue;
                }

                // Extract FK values from event entities (foreign entities).
                // These FK values correspond to root entity PK values.
                SpecificationService sourceService = mapping.getSourceService();
                List<List<MetaAttribute<?, ?>>> fkPaths = mapping.getForeignKeyPaths();

                if (sourceService == null || fkPaths == null || fkPaths.isEmpty()) {
                    continue;
                }

                Set<List<Object>> fkValueTuples = new LinkedHashSet<>();
                for (T eventEntity : eventEntities) {
                    List<Object> fkValues = new ArrayList<>(fkPaths.size());
                    boolean hasNull = false;
                    for (List<MetaAttribute<?, ?>> fkPath : fkPaths) {
                        Object fkValue = sourceService.getValueByPath(eventEntity, fkPath);
                        if (fkValue == null) {
                            hasNull = true;
                            break;
                        }
                        fkValues.add(fkValue);
                    }
                    if (!hasNull) {
                        fkValueTuples.add(fkValues);
                    }
                }

                if (fkValueTuples.isEmpty()) {
                    continue;
                }

                // Match FK value tuples against root entity PK value tuples.
                // Scan all root entities and compare PK paths — this handles both
                // simple and composite keys, and same-datasource self-referencing
                // mappings where FK values are NOT entity IDs.
                List<?> rootEntities = dependencyGraph.findAll(primaryDs);
                if (rootEntities == null || rootEntities.isEmpty()) {
                    continue;
                }

                SpecificationService targetService = mapping.getTargetService();
                List<List<MetaAttribute<?, ?>>> pkPaths = mapping.getPrimaryKeyPaths();

                if (targetService == null || pkPaths == null || pkPaths.isEmpty()) {
                    continue;
                }

                for (Object rootEntity : rootEntities) {
                    if (rootEntity == null) {
                        continue;
                    }
                    List<Object> pkValues = new ArrayList<>(pkPaths.size());
                    boolean hasNull = false;
                    for (List<MetaAttribute<?, ?>> pkPath : pkPaths) {
                        Object pkValue = targetService.getValueByPath(rootEntity, pkPath);
                        if (pkValue == null) {
                            hasNull = true;
                            break;
                        }
                        pkValues.add(pkValue);
                    }
                    if (!hasNull && fkValueTuples.contains(pkValues)) {
                        affectedRootEntityIds.add(((Identifiable<?>) rootEntity).getIdentity());
                    }
                }
            }
        }

        return affectedRootEntityIds;
    }

    /**
     * Determines whether a mapping needs re-evaluation based on its
     * CollectionOperationMetadata and the changed entities.
     *
     * <p>Conservative approach: returns true when in doubt.</p>
     */
    private boolean shouldReevaluateMapping(
            PropertyMapping<?, ?> mapping,
            String dataSourceName,
            Set<Object> changedEntityIds,
            Map<String, Object> oldFirstLastIds,
            Map<Object, Object> oldEntityStates) {

        List<CollectionOperationMetadata<?, ?>> collectionOps =
                mapping.getSourceCollectionOperations();

        // No collection operations → simple mapping, always re-evaluate
        if (collectionOps == null || collectionOps.isEmpty()) {
            return true;
        }

        // Check each collection operation — if ANY requires re-evaluation, the mapping
        // needs re-evaluation. Conservative: one "needs re-eval" is enough.
        for (int i = 0; i < collectionOps.size(); i++) {
            CollectionOperationMetadata<?, ?> collOp = collectionOps.get(i);
            if (shouldReevaluateForCollectionOp(collOp, dataSourceName, changedEntityIds,
                    mapping.getConsumerId(), i, oldFirstLastIds, oldEntityStates)) {
                return true;
            }
        }

        // All collection operations determined no re-evaluation needed
        return false;
    }

    /**
     * Evaluates a single CollectionOperationMetadata to determine if the mapping
     * needs re-evaluation based on the selector type.
     */
    private boolean shouldReevaluateForCollectionOp(
            CollectionOperationMetadata<?, ?> collOp,
            String dataSourceName,
            Set<Object> changedEntityIds,
            String consumerId,
            int collOpIndex,
            Map<String, Object> oldFirstLastIds,
            Map<Object, Object> oldEntityStates) {

        CollectionSelector selector = collOp.getSelector();

        return switch (selector) {
            case ALL ->
                // ALL: any change to the collection affects the result
                true;
            case FIRST ->
                shouldReevaluateForFirstOrLast(collOp, dataSourceName, changedEntityIds, true,
                        consumerId, collOpIndex, oldFirstLastIds);
            case LAST ->
                shouldReevaluateForFirstOrLast(collOp, dataSourceName, changedEntityIds, false,
                        consumerId, collOpIndex, oldFirstLastIds);
            case ANY ->
                shouldReevaluateForAny(collOp, dataSourceName, changedEntityIds, oldEntityStates);
        };
    }

    /**
     * Determines if a FIRST or LAST collection operation needs re-evaluation.
     *
     * <p>Bug 6 fix: Compares old first/last entity ID (captured before Phase 1)
     * with the new first/last entity ID (after Phase 1 updated the graph).
     * If the first/last element shifted to a different entity, re-evaluation is needed
     * even if the new first/last entity is not in changedEntityIds.</p>
     *
     * <p>Decision logic:
     * <ul>
     *   <li>Old != New → true (first/last shifted to different entity)</li>
     *   <li>Old == New AND new is in changedEntityIds → true (same entity but data changed)</li>
     *   <li>Old == New AND new is NOT in changedEntityIds → false (nothing relevant changed)</li>
     * </ul></p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean shouldReevaluateForFirstOrLast(
            CollectionOperationMetadata<?, ?> collOp,
            String dataSourceName,
            Set<Object> changedEntityIds,
            boolean isFirst,
            String consumerId,
            int collOpIndex,
            Map<String, Object> oldFirstLastIds) {

        Comparator comparator = collOp.getComparator();

        // No comparator → can't determine order, conservative: re-evaluate
        if (comparator == null) {
            logger.trace("Phase 2: No comparator for {} selector, conservatively re-evaluating",
                    isFirst ? "FIRST" : "LAST");
            return true;
        }

        // Get all entities for this datasource from DependencyGraph (post-Phase 1)
        List<Object> allEntities = dependencyGraph.findAll(dataSourceName);
        if (allEntities.isEmpty()) {
            return false;
        }

        // Apply specification filter if present
        Specification specification = collOp.getSpecification();
        List<Object> filteredEntities;
        if (specification != null) {
            filteredEntities = new ArrayList<>();
            for (Object entity : allEntities) {
                try {
                    if (specification.test(entity)) {
                        filteredEntities.add(entity);
                    }
                } catch (Exception e) {
                    // Conservative: include entity on error
                    filteredEntities.add(entity);
                }
            }
        } else {
            filteredEntities = allEntities;
        }

        if (filteredEntities.isEmpty()) {
            return false;
        }

        // Sort using comparator
        try {
            filteredEntities = new ArrayList<>(filteredEntities);
            filteredEntities.sort(comparator);
        } catch (Exception e) {
            // Sort failed — conservative: re-evaluate
            logger.trace("Phase 2: Sort failed for {} selector, conservatively re-evaluating",
                    isFirst ? "FIRST" : "LAST");
            return true;
        }

        // Determine the new first/last entity ID
        Object newTargetEntity = isFirst
                ? filteredEntities.get(0)
                : filteredEntities.get(filteredEntities.size() - 1);

        if (!(newTargetEntity instanceof Identifiable)) {
            // Can't determine entity ID — conservative: re-evaluate
            return true;
        }

        Object newTargetId = ((Identifiable<?>) newTargetEntity).getIdentity();

        // Look up the old first/last entity ID captured before Phase 1
        String key = buildFirstLastKey(consumerId, collOpIndex, isFirst);
        Object oldTargetId = oldFirstLastIds.get(key);

        if (oldTargetId == null) {
            // No old state captured (e.g., new datasource or first batch) — conservative: re-evaluate
            return true;
        }

        // Bug 6 fix: Compare old vs new first/last entity ID
        if (!oldTargetId.equals(newTargetId)) {
            // First/last shifted to a different entity → must re-evaluate
            logger.trace("Phase 2: {} entity changed from id={} to id={} for consumer '{}', re-evaluating",
                    isFirst ? "FIRST" : "LAST", oldTargetId, newTargetId, consumerId);
            return true;
        }

        // Same entity is still first/last — only re-evaluate if its data changed
        return changedEntityIds.contains(newTargetId);
    }

    /**
     * Determines if an ANY collection operation needs re-evaluation.
     *
     * <p>Bug 7 fix: Uses old entity states captured before Phase 1 to compare
     * specification results. If the specification result changed (old matched but
     * new doesn't, or vice versa), the mapping needs re-evaluation. If the result
     * is the same for all changed entities, re-evaluation is skipped.</p>
     *
     * <p>Conservative: if old entity state is not found (new insert), returns true.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean shouldReevaluateForAny(
            CollectionOperationMetadata<?, ?> collOp,
            String dataSourceName,
            Set<Object> changedEntityIds,
            Map<Object, Object> oldEntityStates) {

        Specification specification = collOp.getSpecification();

        // No specification → can't compare results, conservative: re-evaluate
        if (specification == null) {
            return true;
        }

        // No old entity states available → conservative: re-evaluate
        if (oldEntityStates == null || oldEntityStates.isEmpty()) {
            return true;
        }

        // Check each changed entity: did the specification result change?
        for (Object entityId : changedEntityIds) {
            Object oldEntity = oldEntityStates.get(entityId);

            if (oldEntity == null) {
                // New insert — no old state, conservative: re-evaluate
                return true;
            }

            // Get the new (post-Phase 1) entity from DependencyGraph
            Object newEntity = dependencyGraph.findById(dataSourceName, entityId);
            if (newEntity == null) {
                // Entity was removed — specification result changed (existed → gone)
                return true;
            }

            try {
                boolean oldResult = specification.test(oldEntity);
                boolean newResult = specification.test(newEntity);

                if (oldResult != newResult) {
                    // Specification result changed → must re-evaluate
                    logger.trace("Phase 2: ANY specification result changed for entity id={} "
                            + "(old={}, new={}), re-evaluating", entityId, oldResult, newResult);
                    return true;
                }
            } catch (Exception e) {
                // Error during specification evaluation — conservative: re-evaluate
                logger.warn("Phase 2: Error evaluating ANY specification for entity id={}: {}",
                        entityId, e.getMessage());
                return true;
            }
        }

        // All changed entities have the same specification result before and after
        logger.trace("Phase 2: ANY specification results unchanged for all {} changed entities "
                + "in datasource '{}', skipping re-evaluation", changedEntityIds.size(), dataSourceName);
        return false;
    }

    /**
     * Filters a list of entities by the given specification.
     *
     * <p>Conservative approach: if an entity causes an exception during
     * specification evaluation (e.g., ClassCastException), the entity is
     * included in the result to avoid data loss.</p>
     *
     * @param entities the entities to filter
     * @param spec     the specification to apply
     * @return a new list containing only entities that pass the specification test
     */
    @SuppressWarnings("unchecked")
    private List<?> filterBySpecification(List<?> entities, Specification<?> spec) {
        List<Object> filtered = new ArrayList<>();
        for (Object entity : entities) {
            try {
                if (((Specification) spec).test(entity)) {
                    filtered.add(entity);
                }
            } catch (Exception e) {
                filtered.add(entity);  // Hata durumunda dahil et (konservatif)
            }
        }
        return filtered;
    }

    /**
     * Tests whether a single entity passes the given specification.
     * Returns true if spec is null (no filtering) or if the entity passes the spec.
     * On error, returns true (conservative — include the entity).
     */
    @SuppressWarnings("unchecked")
    private boolean entityPassesSpecification(Object entity, Specification<?> spec) {
        if (spec == null) {
            return true;
        }
        try {
            return ((Specification) spec).test(entity);
        } catch (Exception e) {
            return true; // Hata durumunda dahil et (konservatif)
        }
    }

    /**
     * Fallback: upserts entities one by one, logging errors for individual failures
     * while continuing to process the rest of the batch.
     */
    private <T extends Identifiable<?>> void upsertEntitiesIndividually(
            String dataSourceName, List<T> entities) {
        int successCount = dependencyGraph.upsertAllIndividually(dataSourceName, entities);
        logger.debug("Individual upsert fallback for datasource '{}': {}/{} entities succeeded",
                dataSourceName, successCount, entities.size());
    }

    /**
     * Checks whether the given datasource is a streaming datasource currently
     * in INITIALIZING state. Returns false for batch datasources or if the
     * streaming datasource is not found.
     *
     * @param dataSourceName the datasource name to check
     * @return true if the datasource is a streaming datasource in INITIALIZING state
     */
    private boolean isDataSourceInitializing(String dataSourceName) {
        if (!factory.isStreamingDataSource(dataSourceName)) {
            return false;
        }
        StreamingDataSource<?> streamingDs = factory.getStreamingDataSource(dataSourceName);
        return streamingDs != null
                && streamingDs.getState() == StreamingDataSourceState.INITIALIZING;
    }

    // === Index Registration for Cross-Datasource Mappings ===

    /**
     * Registers IndexDefinitions for all cross-datasource store mappings' FK paths.
     * This enables O(log N) indexed lookup via {@link DependencyGraph#lookup} instead of
     * O(F) brute-force via {@link DependencyGraph#findAll} + linear scan.
     *
     * <p>Called from constructor and {@link #updateAnalysisResult} to ensure indexes
     * are registered whenever mappings are built or changed. Index lifecycle (updates
     * on entity upsert/delete) is already managed by {@link #applyPhase1EntityUpsert}
     * and {@link #removeExpiredEntities}.</p>
     */
    private void registerMappingIndexes() {
        Map<String, List<PropertyMapping<?, ?>>> storeMappingsBuilder = new HashMap<>();
        for (String storeId : factory.getAllStoreIds()) {
            List<PropertyMapping<?, ?>> mappings = dependencyGraph.getMappingsByConsumerId(storeId);
            for (PropertyMapping<?, ?> mapping : mappings) {
                if (mapping.isForDashboard()) {
                    continue;
                }
                // Collect non-dashboard mappings for precomputed store mappings
                storeMappingsBuilder
                        .computeIfAbsent(storeId, k -> new ArrayList<>())
                        .add(mapping);
                if (mapping.getDataSourceName() == null) {
                    continue;
                }
                if (mapping.getForeignKeyPaths() == null || mapping.getForeignKeyPaths().isEmpty()) {
                    continue;
                }
                registerIndexForMapping(mapping);
            }
        }
        // Build immutable precomputed store mappings
        Map<String, List<PropertyMapping<?, ?>>> immutable = new HashMap<>();
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : storeMappingsBuilder.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        this.precomputedStoreMappings = Collections.unmodifiableMap(immutable);
    }

    /**
     * Called after a mapping is added to DependencyGraph.
     * Updates the pre-computed store mapping groupings using copy-on-write semantics.
     * Dashboard mappings (isForDashboard() == true) are excluded.
     *
     * @param mapping the newly added mapping
     */
    public void onMappingAdded(PropertyMapping<?, ?> mapping) {
        if (mapping.isForDashboard()) {
            return;
        }
        String storeId = mapping.getConsumerId();
        Map<String, List<PropertyMapping<?, ?>>> current = this.precomputedStoreMappings;
        Map<String, List<PropertyMapping<?, ?>>> mutable = new HashMap<>(current);
        List<PropertyMapping<?, ?>> existing = mutable.getOrDefault(storeId, Collections.emptyList());
        List<PropertyMapping<?, ?>> updated = new ArrayList<>(existing);
        updated.add(mapping);
        mutable.put(storeId, Collections.unmodifiableList(updated));
        this.precomputedStoreMappings = Collections.unmodifiableMap(mutable);
    }

    /**
     * Called after a mapping is removed from DependencyGraph.
     * Updates the pre-computed store mapping groupings using copy-on-write semantics.
     * Dashboard mappings (isForDashboard() == true) are excluded.
     *
     * @param mapping the removed mapping
     */
    public void onMappingRemoved(PropertyMapping<?, ?> mapping) {
        if (mapping.isForDashboard()) {
            return;
        }
        String storeId = mapping.getConsumerId();
        Map<String, List<PropertyMapping<?, ?>>> current = this.precomputedStoreMappings;
        if (!current.containsKey(storeId)) {
            return;
        }
        Map<String, List<PropertyMapping<?, ?>>> mutable = new HashMap<>(current);
        List<PropertyMapping<?, ?>> existing = mutable.get(storeId);
        List<PropertyMapping<?, ?>> updated = new ArrayList<>(existing);
        updated.remove(mapping);
        if (updated.isEmpty()) {
            mutable.remove(storeId);
        } else {
            mutable.put(storeId, Collections.unmodifiableList(updated));
        }
        this.precomputedStoreMappings = Collections.unmodifiableMap(mutable);
    }

    /**
     * Returns the pre-computed store mapping groupings.
     * Used by Phase 2.5 instead of runtime grouping.
     *
     * @return immutable map of storeId to list of non-dashboard PropertyMappings
     */
    Map<String, List<PropertyMapping<?, ?>>> getStoreMappings() {
        return precomputedStoreMappings;
    }

    /**
     * Registers an IndexDefinition for a single mapping's FK paths, with deduplication.
     * Mappings that share the same (dataSourceName, FK paths) reuse the same IndexDefinition.
     */
    private void registerIndexForMapping(PropertyMapping<?, ?> mapping) {
        String dsName = mapping.getDataSourceName();
        List<List<MetaAttribute<?, ?>>> fkPaths = mapping.getForeignKeyPaths();

        // Build dedup key: dataSourceName + FK path attribute names
        StringBuilder keyBuilder = new StringBuilder(dsName);
        for (List<MetaAttribute<?, ?>> path : fkPaths) {
            keyBuilder.append(':');
            for (MetaAttribute<?, ?> attr : path) {
                keyBuilder.append(attr.getName()).append('.');
            }
        }
        String dedupKey = keyBuilder.toString();

        IndexDefinition<?> indexDef = indexDefinitionCache.get(dedupKey);
        if (indexDef == null) {
            indexDef = buildIndexDefinition(mapping);
            indexDefinitionCache.put(dedupKey, indexDef);
            dependencyGraph.registerIndex(dsName, indexDef);
            logger.debug("Registered index for datasource '{}' with FK paths: {}",
                    dsName, dedupKey);
        }

        mappingIndexCache.put(mapping, indexDef);
    }

    /**
     * Builds an IndexDefinition for a mapping's FK paths using the mapping's
     * SpecificationService for value extraction — same extraction logic as the
     * brute-force lambda's {@code sourceService.getValueByPath(entity, fkPath)}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> IndexDefinition<T> buildIndexDefinition(PropertyMapping<?, ?> mapping) {
        Class<T> entityClass = (Class<T>) mapping.getSourceClass();
        SpecificationService sourceService = mapping.getSourceService();

        IndexDefinition.Builder<T> builder = IndexDefinition.builder(entityClass);
        for (List<MetaAttribute<?, ?>> fkPath : mapping.getForeignKeyPaths()) {
            builder.addKeyFieldWithPath(
                    fkPath,
                    entity -> sourceService.getValueByPath(entity, fkPath));
        }
        return builder.build();
    }

    /**
     * Returns the cached IndexDefinition for a given mapping.
     * Package-private for use by optimized lookup lambda (Task 3.2/3.3).
     *
     * @param mapping the property mapping
     * @return the IndexDefinition, or null if not registered
     */
    IndexDefinition<?> getIndexDefinition(PropertyMapping<?, ?> mapping) {
        return mappingIndexCache.get(mapping);
    }

    /**
     * Indexed RelatedEntityLookup implementation shared by applyPhase2_5StoreMappings()
     * and applyPostInitializationCatchUp() — DRY extraction (Task 3.3).
     *
     * <p>Tries indexed lookup via DependencyGraph.lookup() + NestedTreeMapIndex first (O(log F)).
     * Falls back to brute-force findAll() + linear FK scan for mappings without a registered index.</p>
     *
     * @param mapping        the property mapping with FK paths and datasource info
     * @param primaryKeyValues the PK values to match against FK values
     * @return matching foreign entities; empty list if no match or null datasource
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<?> lookupRelatedEntities(PropertyMapping<?, ?> mapping, List<Object> primaryKeyValues) {
        String dsName = mapping.getDataSourceName();
        if (dsName == null) {
            return Collections.emptyList();
        }

        // Try indexed lookup first (O(log F))
        IndexDefinition<?> indexDef = getIndexDefinition(mapping);
        if (indexDef != null) {
            return dependencyGraph.lookup(dsName, indexDef, primaryKeyValues.toArray());
        }

        // Fallback: brute-force for mappings without a registered index
        List<?> foreignEntities = dependencyGraph.findAll(dsName);
        if (foreignEntities == null || foreignEntities.isEmpty()) {
            return Collections.emptyList();
        }

        SpecificationService sourceService = mapping.getSourceService();
        List<Object> matched = new ArrayList<>();
        for (Object entity : foreignEntities) {
            boolean match = true;
            for (int i = 0; i < mapping.getForeignKeyPaths().size(); i++) {
                Object fkValue = sourceService.getValueByPath(entity, mapping.getForeignKeyPaths().get(i));
                if (!Objects.equals(fkValue, primaryKeyValues.get(i))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                matched.add(entity);
            }
        }
        return matched;
    }

    /**
     * Event-scoped FK lookup cache.
     *
     * <p>Caches FK lookup results during a single {@code applyPhase2_5StoreMappings()} call
     * to eliminate redundant index accesses when multiple root entities reference the same
     * foreign entity.</p>
     *
     * <p>Cache key: {@code mapping.getConsumerId() + "|" + mapping.getDataSourceName() + "|" + primaryKeyValues.toString()}</p>
     *
     * <p>Lifecycle: created at the start of Phase 2.5, cleared at the end via {@link #clear()}.</p>
     *
     * @see RelatedEntityLookup
     */
    private static class FKLookupCache {
        private final Map<String, List<?>> cache = new HashMap<>();
        private int hitCount = 0;
        private int missCount = 0;

        /**
         * Returns cached lookup result or delegates to the fallback lookup and caches the result.
         *
         * @param mapping          the property mapping with FK paths and datasource info
         * @param primaryKeyValues the PK values to match against FK values
         * @param fallbackLookup   the actual lookup to invoke on cache miss
         * @return matching foreign entities from cache or fallback
         */
        List<?> getOrLookup(PropertyMapping<?, ?> mapping,
                            List<Object> primaryKeyValues,
                            RelatedEntityLookup fallbackLookup) {
            String cacheKey = buildCacheKey(mapping, primaryKeyValues);
            List<?> cached = cache.get(cacheKey);
            if (cached != null) {
                hitCount++;
                return cached;
            }
            missCount++;
            List<?> result = fallbackLookup.lookupRelatedEntities(mapping, primaryKeyValues);
            cache.put(cacheKey, result);
            return result;
        }

        /**
         * Clears all cached entries and resets hit/miss counters.
         */
        void clear() {
            cache.clear();
            hitCount = 0;
            missCount = 0;
        }

        /**
         * Returns the cache hit rate as a percentage (0.0–100.0).
         *
         * @return hit rate percentage, or 0.0 if no lookups have been performed
         */
        double getHitRate() {
            int total = hitCount + missCount;
            if (total == 0) {
                return 0.0;
            }
            return (hitCount * 100.0) / total;
        }

        private static String buildCacheKey(PropertyMapping<?, ?> mapping, List<Object> primaryKeyValues) {
            return mapping.getConsumerId() + "|" + mapping.getDataSourceName() + "|" + primaryKeyValues.toString();
        }
    }

}
