package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEventListener;
import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.exception.DataSynchronizationException;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.engine.index.CompositeKeyIndex;
import com.thy.fss.common.inmemory.engine.mapping.MappingApplicator;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.RelatedEntityLookup;
import com.thy.fss.common.inmemory.engine.sync.CompositeVersion;
import com.thy.fss.common.inmemory.engine.sync.DataSourceSyncMetadata;
import com.thy.fss.common.inmemory.engine.sync.DataVersion;
import com.thy.fss.common.inmemory.engine.sync.DependencyGraph;
import com.thy.fss.common.inmemory.engine.sync.IncrementalSyncProcessor;
import com.thy.fss.common.inmemory.engine.sync.StreamingDataSourceLifecycleManager;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.SpecificationServices;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central synchronization engine that manages data synchronization across all
 * datasources, stores, and dashboards.
 *
 * <p>
 * This engine is responsible for:</p>
 * <ul>
 * <li>Managing datasource synchronization intervals</li>
 * <li>Triggering synchronization based on datasource intervals</li>
 * <li>Creating and managing global DataVersion instances</li>
 * <li>Parallel bulk datasource reading</li>
 * <li>Primary-foreign key grouping and aggregation</li>
 * <li>Pushing data to all stores and dashboards</li>
 * <li>Health checking and fallback management</li>
 * </ul>
 *
 * <p>
 * The engine uses a global DataVersion to ensure consistency across all
 * consumers. When a datasource's interval expires, the engine triggers a
 * synchronization that re-reads only the changed datasources and updates all
 * affected consumers.</p>
 *
 * <p>
 * Thread Safety: This class is thread-safe. All synchronization operations are
 * protected by a ReentrantLock to prevent concurrent modifications.</p>
 */
public class DataSynchronizationEngine {

    private static final Logger logger = LoggerFactory.getLogger(DataSynchronizationEngine.class);

    /**
     * Default timeout for datasource read operations (in seconds). This value
     * is used when a datasource doesn't have a specific timeout configured.
     */
    private static final long DEFAULT_READ_TIMEOUT_SECONDS = 30;

    /**
     * Maximum wait time for datasources to become ready during startup (in seconds).
     * The engine will wait up to this duration for datasources to become healthy
     * before proceeding with initialization.
     */
    private static final long DATASOURCE_STARTUP_WAIT_SECONDS = 30;

    /**
     * Retry interval for checking datasource health during startup (in milliseconds).
     */
    private static final long DATASOURCE_STARTUP_RETRY_INTERVAL_MS = 2000;

    // Current global data version (atomic for thread-safe reads)
    private final AtomicReference<DataVersion> currentDataVersion;

    // Lock for synchronization operations (prevents concurrent sync)
    private final ReentrantLock syncLock;

    // Scheduler for interval-based datasource synchronization
    private final ScheduledExecutorService scheduler;

    // Worker thread pool for parallel processing (datasource reads, aggregations)
    private final ExecutorService workerPool;

    // Set of datasources that need to be synchronized (thread-safe)
    private final Set<String> pendingDataSources;

    // Metadata for each datasource (sync times, health status, etc.)
    private final Map<String, DataSourceSyncMetadata> dataSourceMetadata;

    // Reference to the factory for accessing datasources and consumers
    private final InMemorySpecStoreFactory factory;

    // Cached analysis result (computed once during initialization)
    // Since property mappings and specifications don't change at runtime,
    // we can cache the analysis result and reuse it for all synchronization cycles
    private com.thy.fss.common.inmemory.engine.analysis.AnalysisResult cachedAnalysisResult;

    // Index manager for optimized data access
    private final com.thy.fss.common.inmemory.engine.index.IndexManager indexManager;

    // Engine running state
    private volatile boolean running;

    // --- Streaming datasource support ---

    // Unified data structure for streaming entity storage and dependency tracking
    private DependencyGraph dependencyGraph;

    // Processes batch snapshot events through the four-phase pipeline
    private IncrementalSyncProcessor incrementalSyncProcessor;

    // Manages streaming datasource lifecycle states (INITIALIZING, READY, ERROR)
    private StreamingDataSourceLifecycleManager lifecycleManager;

    // Stores listener references per streaming datasource for cleanup during close()
    private final Map<String, BatchSnapshotEventListener<?>> streamingListeners = new ConcurrentHashMap<>();

    // Tracks datasources with a pending reconnection attempt to prevent duplicate scheduling
    private final Set<String> pendingReconnections = ConcurrentHashMap.newKeySet();

    // Lock for sequential processing of streaming events from multiple datasources.
    // Ensures that events from different streaming datasources are processed one at a time,
    // and coordinates with the full sync queuing mechanism.
    private final Object streamingEventLock = new Object();

    // Lightweight streaming version counter Ã¢â‚¬â€ incremented after each successful BatchSnapshotEvent processing
    private final AtomicLong streamingVersion = new AtomicLong(0);

    // Timestamp of the last successful streaming update
    private volatile Instant lastStreamingUpdateTimestamp;

    /**
     * Creates a new DataSynchronizationEngine with the specified factory.
     *
     * @param factory the InMemorySpecStoreFactory instance
     */
    public DataSynchronizationEngine(InMemorySpecStoreFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }

        this.factory = factory;
        this.currentDataVersion = new AtomicReference<>(createInitialDataVersion());
        this.syncLock = new ReentrantLock();
        this.indexManager = new com.thy.fss.common.inmemory.engine.index.IndexManager();
        this.scheduler = Executors.newScheduledThreadPool(
                2, // Small pool for scheduling tasks
                r -> {
                    Thread t = new Thread(r, "DataSync-Scheduler");
                    t.setDaemon(true);
                    return t;
                }
        );
        this.workerPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "DataSync-Worker");
                    t.setDaemon(true);
                    return t;
                }
        );
        this.pendingDataSources = ConcurrentHashMap.newKeySet();
        this.dataSourceMetadata = new ConcurrentHashMap<>();
        this.running = false;

        logger.debug("DataSynchronizationEngine created");
    }

    /**
     * Creates the initial empty DataVersion.
     *
     * @return a new DataVersion with version 0
     */
    private DataVersion createInitialDataVersion() {
        return new DataVersion(0, LocalDateTime.now());
    }

    /**
     * Gets the current DataVersion. This method is thread-safe and returns the
     * current global version.
     *
     * @return the current DataVersion
     */
    public DataVersion getCurrentDataVersion() {
        return currentDataVersion.get();
    }

    /**
     * Checks if the engine is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the sync lock for testing purposes.
     *
     * @return the sync lock
     */
    ReentrantLock getSyncLock() {
        return syncLock;
    }

    /**
     * Gets the pending datasources set for testing purposes.
     *
     * @return the pending datasources set
     */
    Set<String> getPendingDataSourcesInternal() {
        return pendingDataSources;
    }

    /**
     * Gets the datasource metadata map for testing purposes.
     *
     * @return the datasource metadata map
     */
    Map<String, DataSourceSyncMetadata> getDataSourceMetadataInternal() {
        return dataSourceMetadata;
    }

    /**
     * Returns the DependencyGraph used for streaming datasource entity storage.
     * Package-private for use by related engine components and tests.
     *
     * @return the dependency graph, or null if not yet initialized
     */
    DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Returns the IncrementalSyncProcessor used for streaming event processing.
     * Package-private for use by related engine components and tests.
     *
     * @return the incremental sync processor, or null if not yet initialized
     */
    IncrementalSyncProcessor getIncrementalSyncProcessor() {
        return incrementalSyncProcessor;
    }

    /**
     * Returns the StreamingDataSourceLifecycleManager.
     * Package-private for use by related engine components and tests.
     *
     * @return the lifecycle manager, or null if not yet initialized
     */
    StreamingDataSourceLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    /**
     * Returns the current streaming version number.
     * Incremented after each successful BatchSnapshotEvent processing.
     *
     * @return the current streaming version
     */
    public long getStreamingVersion() {
        return streamingVersion.get();
    }

    /**
     * Returns the timestamp of the last successful streaming update.
     *
     * @return the last streaming update timestamp, or null if no streaming update has occurred
     */
    public Instant getLastStreamingUpdateTimestamp() {
        return lastStreamingUpdateTimestamp;
    }

    /**
     * Returns a composite version combining batch and streaming version information.
     * Useful for monitoring and reporting purposes.
     *
     * @return a CompositeVersion with batch version, streaming version, and last streaming update timestamp
     */
    public CompositeVersion getCompositeVersion() {
        return new CompositeVersion(
                currentDataVersion.get().getVersion(),
                streamingVersion.get(),
                lastStreamingUpdateTimestamp
        );
    }

    /**
     * Returns the streaming lifecycle state of each registered streaming datasource.
     *
     * <p>This method queries the metadata for all streaming datasources and returns
     * a map from datasource name to its current {@link StreamingDataSourceState}.
     * Useful for bulk readiness checks Ã¢â‚¬â€ callers can see which datasources are
     * INITIALIZING, READY, or in ERROR state.</p>
     *
     * @return an unmodifiable map of streaming datasource names to their states;
     *         empty map if no streaming datasources are registered
     */
    public Map<String, StreamingDataSourceState> getStreamingDataSourceStates() {
        Map<String, StreamingDataSourceState> result = new java.util.HashMap<>();
        for (Map.Entry<String, DataSourceSyncMetadata> entry : dataSourceMetadata.entrySet()) {
            DataSourceSyncMetadata metadata = entry.getValue();
            if (metadata.isStreamingDataSource()) {
                result.put(entry.getKey(), metadata.getStreamingState());
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    // --- Dynamic mapping management ---

    /**
     * Adds a property mapping at runtime and re-analyzes the AnalysisResult.
     *
     * <p>This method updates the DependencyGraph, re-runs the analysis phase,
     * and propagates the new AnalysisResult to the IncrementalSyncProcessor so that
     * pipeline Phase 2 (mapping) and Phase 3 (aggregation) use the updated information.</p>
     *
     * <p>Thread-safety: This method acquires the syncLock to prevent concurrent
     * modification during re-analysis.</p>
     *
     * @param mapping the property mapping to add
     * @throws IllegalArgumentException if mapping is null
     * @throws IllegalStateException    if the engine is not running
     */
    public void addPropertyMapping(PropertyMapping<?, ?> mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("Property mapping cannot be null");
        }
        if (!running) {
            throw new IllegalStateException("Engine is not running");
        }

        syncLock.lock();
        try {
            logger.debug("Adding property mapping at runtime: dataSource='{}', consumer='{}'",
                    mapping.getDataSourceName(), mapping.getConsumerId());

            // 1. Update DependencyGraph
            dependencyGraph.addMapping(mapping);

            // 1b. Update pre-computed store mapping groupings
            incrementalSyncProcessor.onMappingAdded(mapping);

            // 2. Re-analyze with all current mappings
            java.util.List<PropertyMapping<?, ?>> allMappings = factory.getAllPropertyMappings();
            // Include the new mapping if not already in factory's list
            if (!allMappings.contains(mapping)) {
                allMappings = new java.util.ArrayList<>(allMappings);
                allMappings.add(mapping);
            }
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult newAnalysisResult =
                    analyzeAggregationsAndMappings(allMappings);

            // 3. Update cached result and propagate to processor
            this.cachedAnalysisResult = newAnalysisResult;
            incrementalSyncProcessor.updateAnalysisResult(newAnalysisResult);

            logger.debug("Property mapping added and AnalysisResult re-analyzed: {}", newAnalysisResult);
        } finally {
            syncLock.unlock();
        }
    }

    /**
     * Removes a property mapping at runtime and re-analyzes the AnalysisResult.
     *
     * <p>This method updates the DependencyGraph, re-runs the analysis phase,
     * and propagates the new AnalysisResult to the IncrementalSyncProcessor so that
     * pipeline Phase 2 (mapping) and Phase 3 (aggregation) use the updated information.</p>
     *
     * <p>Thread-safety: This method acquires the syncLock to prevent concurrent
     * modification during re-analysis.</p>
     *
     * @param mapping the property mapping to remove
     * @throws IllegalArgumentException if mapping is null
     * @throws IllegalStateException    if the engine is not running
     */
    public void removePropertyMapping(PropertyMapping<?, ?> mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("Property mapping cannot be null");
        }
        if (!running) {
            throw new IllegalStateException("Engine is not running");
        }

        syncLock.lock();
        try {
            logger.debug("Removing property mapping at runtime: dataSource='{}', consumer='{}'",
                    mapping.getDataSourceName(), mapping.getConsumerId());

            // 1. Update DependencyGraph
            dependencyGraph.removeMapping(mapping);

            // 1b. Update pre-computed store mapping groupings
            incrementalSyncProcessor.onMappingRemoved(mapping);

            // 2. Re-analyze with remaining mappings
            java.util.List<PropertyMapping<?, ?>> allMappings = factory.getAllPropertyMappings();
            // Exclude the removed mapping if still in factory's list
            allMappings = new java.util.ArrayList<>(allMappings);
            allMappings.remove(mapping);
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult newAnalysisResult =
                    analyzeAggregationsAndMappings(allMappings);

            // 3. Update cached result and propagate to processor
            this.cachedAnalysisResult = newAnalysisResult;
            incrementalSyncProcessor.updateAnalysisResult(newAnalysisResult);

            logger.debug("Property mapping removed and AnalysisResult re-analyzed: {}", newAnalysisResult);
        } finally {
            syncLock.unlock();
        }
    }

    /**
     * Initializes the engine and starts synchronization.
     *
     * <p>
     * This method:</p>
     * <ul>
     * <li>Bootstraps all registered datasources</li>
     * <li>Performs health checks on all datasources</li>
     * <li>Reads initial data from all datasources</li>
     * <li>Starts interval-based scheduling</li>
     * </ul>
     *
     * <p>
     * This method should be called once after all datasources are
     * registered.</p>
     *
     * @throws IllegalStateException if the engine is already running
     */
    public void initialize() {
        if (running) {
            throw new IllegalStateException("Engine is already running");
        }

        logger.debug("Initializing DataSynchronizationEngine...");

        try {
            // Step 1: Initialize metadata for all registered datasources
            initializeDataSourceMetadata();

            // Step 2: Perform health checks on all datasources
            performInitialHealthChecks();

            // Step 3: Validate property mappings (circular dependency check)
            // This is done once at initialization since mappings don't change at runtime
            validatePropertyMappings();

            // Step 4: Perform analysis phase once and cache the result
            // Since property mappings and specifications don't change at runtime,
            // we analyze once and reuse the result for all synchronization cycles
            performInitialAnalysis();

            // Step 4.5: Initialize streaming datasource infrastructure
            // Creates DependencyGraph, IncrementalSyncProcessor, and LifecycleManager.
            // Subscribes to all registered streaming datasources.
            initializeStreamingInfrastructure();

            // Step 5: Bootstrap - read all datasources immediately
            bootstrapAllDataSources();

            // Step 6: Perform initial synchronization synchronously
            // This ensures that data is available immediately after initialization
            performInitialSynchronization();

            // Step 7: Start interval-based scheduling
            startScheduling();

            // Mark as running
            running = true;

            logger.debug("DataSynchronizationEngine initialized successfully");

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Failed to initialize DataSynchronizationEngine", e);
            // Cleanup on failure
            close();
            throw new DataSynchronizationException("Failed to initialize DataSynchronizationEngine", e);
        }
    }

    /**
     * Performs the initial synchronization synchronously.
     * Ensures that data is available immediately after initialization.
     */
    private void performInitialSynchronization() {
        logger.debug("Performing initial synchronization...");
        try {
            triggerGlobalSynchronization();
            logger.debug("Initial synchronization completed");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Initial synchronization failed", e);
            throw new DataSynchronizationException("Initial synchronization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes sync metadata for all registered datasources.
     */
    private void initializeDataSourceMetadata() {
        for (String dataSourceName : factory.getAllDataSourceNames()) {
            java.time.Duration interval = factory.getDataSourceInterval(dataSourceName);
            java.time.Duration timeout = factory.getDataSourceTimeout(dataSourceName);
            DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dataSourceName, interval, timeout);
            dataSourceMetadata.put(dataSourceName, metadata);
            logger.debug("Initialized metadata for datasource '{}' with interval {} and timeout {}",
                    dataSourceName, interval, timeout != null ? timeout : "default");
        }
    }

    /**
     * Performs initial health checks on all datasources with retry mechanism.
     * Waits for datasources to become ready during application startup without
     * logging errors until the timeout is reached.
     *
     * <p>This method implements a graceful startup mechanism that:</p>
     * <ul>
     * <li>Waits for datasources to become ready during initial startup</li>
     * <li>Retries health checks silently without logging errors</li>
     * <li>Only logs warnings if datasources remain unhealthy after timeout</li>
     * <li>Allows the application to start even if some datasources are unavailable</li>
     * </ul>
     */
    private void performInitialHealthChecks() {
        logger.info("Waiting for datasources to become ready (timeout: {}s)...",
                DATASOURCE_STARTUP_WAIT_SECONDS);

        long startTime = System.currentTimeMillis();
        long timeoutMs = DATASOURCE_STARTUP_WAIT_SECONDS * 1000;

        // Track which datasources are still unhealthy
        Set<String> unhealthyDataSources = ConcurrentHashMap.newKeySet();
        unhealthyDataSources.addAll(dataSourceMetadata.keySet());

        // Retry loop - wait for datasources to become ready
        while (!unhealthyDataSources.isEmpty() &&
                (System.currentTimeMillis() - startTime) < timeoutMs) {

            Set<String> currentlyUnhealthy = new java.util.HashSet<>(unhealthyDataSources);

            for (String dataSourceName : currentlyUnhealthy) {
                DataSourceSyncMetadata metadata = dataSourceMetadata.get(dataSourceName);

                try {
                    // Try to get the datasource (basic health check)
                    factory.getDataSource(dataSourceName);
                    metadata.markHealthy();
                    unhealthyDataSources.remove(dataSourceName);
                    logger.info("Datasource '{}' is now ready", dataSourceName);

                } catch (Exception e) {
                    // During startup, don't log errors - just silently retry
                    // This prevents flooding logs with connection errors during startup
                }
            }

            // If all datasources are healthy, break early
            if (unhealthyDataSources.isEmpty()) {
                break;
            }

            // Wait before next retry
            try {
                Thread.sleep(DATASOURCE_STARTUP_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for datasources to become ready");
                break;
            }
        }

        // After timeout, log warnings for datasources that are still unhealthy
        if (!unhealthyDataSources.isEmpty()) {
            logger.warn("The following datasources are not ready after {}s: {}",
                    DATASOURCE_STARTUP_WAIT_SECONDS, unhealthyDataSources);
            logger.warn("Application will continue with available datasources. " +
                    "Unhealthy datasources will be retried automatically.");

            // Mark remaining datasources as unhealthy
            for (String dataSourceName : unhealthyDataSources) {
                DataSourceSyncMetadata metadata = dataSourceMetadata.get(dataSourceName);
                metadata.markUnhealthy("Not ready after startup timeout");
            }
        } else {
            logger.info("All {} datasources are ready", dataSourceMetadata.size());
        }
    }

    /**
     * Validates all property mappings by detecting circular dependencies. This
     * is performed once during initialization since property mappings don't
     * change at runtime.
     *
     * @throws
     * com.thy.fss.common.inmemory.engine.exception.CircularMappingException if
     * circular dependency detected
     */
    private void validatePropertyMappings() {
        logger.debug("Validating property mappings for circular dependencies...");

        java.util.List<PropertyMapping<?, ?>> allPropertyMappings = factory.getAllPropertyMappings();
        logger.debug("Found {} property mapping(s) to validate", allPropertyMappings.size());

        // Detect circular mappings
        detectCircularMappings(allPropertyMappings);

        logger.debug("Property mapping validation completed - no circular dependencies detected");
    }

    /**
     * Performs the analysis phase once during initialization and caches the
     * result. Since property mappings and specifications don't change at
     * runtime, we can analyze once and reuse the result for all synchronization
     * cycles.
     * <p>
     * This is a heavy operation that analyzes:
     * <ul>
     * <li>Common groupings for Store primary-foreign key relationships</li>
     * <li>Dashboard aggregation plans with optimized task grouping</li>
     * <li>All source datasources needed</li>
     * </ul>
     * <p>
     * The cached result is used by all subsequent synchronization cycles,
     * eliminating the need to re-analyze on every sync.
     */
    private void performInitialAnalysis() {
        logger.debug("Performing initial analysis phase (one-time operation)...");

        java.util.List<PropertyMapping<?, ?>> allPropertyMappings = factory.getAllPropertyMappings();
        logger.debug("Analyzing {} property mapping(s)", allPropertyMappings.size());

        // Perform comprehensive analysis
        cachedAnalysisResult = analyzeAggregationsAndMappings(allPropertyMappings);

        logger.debug("Initial analysis completed and cached - {}", cachedAnalysisResult);
        logger.debug("Analysis will be reused for all synchronization cycles");
    }

    /**
     * Initializes streaming datasource infrastructure.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Creates a {@link DependencyGraph} and builds it from all PropertyMapping definitions</li>
     *   <li>Creates an {@link IncrementalSyncProcessor} with the factory, DependencyGraph, and cached AnalysisResult</li>
     *   <li>Creates a {@link StreamingDataSourceLifecycleManager} for lifecycle management</li>
     *   <li>Sets the initial load completion callback on the processor</li>
     *   <li>Subscribes to all registered streaming datasources</li>
     * </ol>
     *
     * <p>If no streaming datasources are registered, the infrastructure is still created
     * (for potential future dynamic registration) but no subscriptions are made.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initializeStreamingInfrastructure() {
        logger.debug("Initializing streaming datasource infrastructure...");

        // 1. Create DependencyGraph and build from all PropertyMapping definitions
        this.dependencyGraph = new DependencyGraph();
        List<PropertyMapping<?, ?>> allMappings = factory.getAllPropertyMappings();
        dependencyGraph.build(allMappings);
        logger.debug("DependencyGraph built with {} property mapping(s)", allMappings.size());

        // 2. Create IncrementalSyncProcessor with factory, DependencyGraph, AnalysisResult,
        //    and shared streamingVersion (Bug 3 fix: single source of truth for version counter)
        this.incrementalSyncProcessor = new IncrementalSyncProcessor(
                factory, dependencyGraph, cachedAnalysisResult, streamingVersion);

        // 3. Create StreamingDataSourceLifecycleManager
        this.lifecycleManager = new StreamingDataSourceLifecycleManager();

        // 4. Subscribe to all registered streaming datasources
        Set<String> streamingNames = factory.getAllStreamingDataSourceNames();
        if (streamingNames.isEmpty()) {
            logger.debug("No streaming datasources registered, skipping subscription");
            return;
        }

        logger.debug("Subscribing to {} streaming datasource(s)...", streamingNames.size());

        for (String dsName : streamingNames) {
            subscribeToStreamingDataSource(dsName);
        }

        // Post-initialization catch-up: apply cross-datasource store mappings that were
        // skipped during INITIALIZING state. Now that all datasources have their initial
        // data in DependencyGraph, Phase 2.5 + Phase 4 can run for all stores.
        incrementalSyncProcessor.applyPostInitializationCatchUp();

        logger.debug("Streaming datasource infrastructure initialized Ã¢â‚¬â€ {} datasource(s) subscribed",
                streamingNames.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void subscribeToStreamingDataSource(String dsName) {
        try {
            StreamingDataSource streamingDs = factory.getStreamingDataSource(dsName);
            if (streamingDs == null) {
                logger.warn("Streaming datasource '{}' not found in factory, skipping", dsName);
                return;
            }

            lifecycleManager.register(dsName);

            Duration interval = factory.getDataSourceInterval(dsName);
            DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, interval);
            metadata.setStreamingDataSource(true);
            metadata.setStreamingState(StreamingDataSourceState.INITIALIZING);
            dataSourceMetadata.put(dsName, metadata);

            final String capturedDsName = dsName;
            BatchSnapshotEventListener listener = event -> {
                synchronized (streamingEventLock) {
                    if (incrementalSyncProcessor.isFullSyncInProgress()) {
                        incrementalSyncProcessor.queueEvent(capturedDsName, event);
                    } else if (isDataSourceInitializing(capturedDsName)) {
                        incrementalSyncProcessor.queueEvent(capturedDsName, event);
                    } else {
                        streamingVersion.incrementAndGet();
                        incrementalSyncProcessor.processBatchSnapshot(capturedDsName, event);
                        lastStreamingUpdateTimestamp = Instant.now();
                    }
                }
            };
            streamingDs.subscribe(listener);
            streamingListeners.put(dsName, listener);

            performStreamingInitialLoad(dsName, streamingDs, metadata);

            logger.debug("Subscribed to streaming datasource '{}' (state={})", dsName, metadata.getStreamingState());

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Failed to subscribe to streaming datasource '{}': {}",
                    dsName, e.getMessage(), e);
            handleStreamingSubscriptionFailure(dsName, e);
        }
    }

    private void handleStreamingSubscriptionFailure(String dsName, Exception e) {
        if (lifecycleManager.isRegistered(dsName)) {
            lifecycleManager.handleConnectionLoss(dsName,
                    "Subscription failed: " + e.getMessage());
        }
        DataSourceSyncMetadata meta = dataSourceMetadata.get(dsName);
        if (meta != null && meta.isStreamingDataSource()) {
            meta.updateStreamingState(StreamingDataSourceState.ERROR);
            meta.incrementReconnectAttempts();
        }
    }

    /**
     * Checks if a datasource is currently in INITIALIZING state.
     * Used by the streaming event listener to queue events during initial load.
     *
     * @param dataSourceName the datasource name to check
     * @return true if the datasource is a streaming datasource in INITIALIZING state
     */
    private boolean isDataSourceInitializing(String dataSourceName) {
        DataSourceSyncMetadata meta = dataSourceMetadata.get(dataSourceName);
        return meta != null && meta.isStreamingDataSource()
                && meta.getStreamingState() == StreamingDataSourceState.INITIALIZING;
    }

    /**
     * Performs the initial fetchAll() load for a streaming datasource.
     * Transitions the datasource state to READY on success or ERROR on failure.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void performStreamingInitialLoad(String dsName, StreamingDataSource streamingDs,
                                              DataSourceSyncMetadata metadata) {
        try {
            Duration timeout = factory.getDataSourceTimeout(dsName);
            long readTimeoutMs = (timeout != null) ? timeout.toMillis() : DEFAULT_READ_TIMEOUT_SECONDS * 1000;

            CompletableFuture<List> future = streamingDs.fetchAll();
            List entities = future.get(readTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (entities != null && !entities.isEmpty()) {
                incrementalSyncProcessor.processBatchDataSourceResult(dsName, entities);
            }

            lifecycleManager.handleInitialLoadComplete(dsName);
            metadata.updateStreamingState(StreamingDataSourceState.READY);

            incrementalSyncProcessor.processQueuedEventsForDataSource(dsName);

            logger.info("Streaming datasource '{}' initial load complete via fetchAll() state transitioned to READY", dsName);

        } catch (Exception fetchEx) {
            logger.error("Initial fetchAll() failed for streaming datasource '{}': {}",
                    dsName, fetchEx.getMessage(), fetchEx);
            if (fetchEx instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lifecycleManager.handleConnectionLoss(dsName,
                    "Initial fetchAll() failed: " + fetchEx.getMessage());
            metadata.updateStreamingState(StreamingDataSourceState.ERROR);
            scheduleReconnection(dsName);
        }
    }

    /**
     * Bootstraps all datasources by marking them for synchronization. The
     * actual synchronization will happen when synchronizeDataSource is called
     * or when the scheduler triggers it based on intervals.
     */
    private void bootstrapAllDataSources() {
        logger.debug("Bootstrapping all datasources...");

        // Mark all datasources as pending for initial sync
        // They will be synchronized when explicitly requested or by the scheduler
        pendingDataSources.addAll(dataSourceMetadata.keySet());

        logger.debug("Marked {} datasources for initial synchronization",
                pendingDataSources.size());
    }

    /**
     * Starts interval-based scheduling for all datasources. Each datasource
     * will be synchronized according to its configured interval.
     */
    private void startScheduling() {
        logger.debug("Starting interval-based scheduling...");

        // Schedule periodic check for datasources that need sync.
        // Use a 1-second initial delay to avoid racing with streaming events
        // that arrive immediately after initialization. The initial sync was
        // already performed in initialize(), so no datasource needs immediate re-sync.
        scheduler.scheduleAtFixedRate(
                this::checkAndTriggerSync,
                1, // Initial delay Ã¢â‚¬â€ avoid racing with post-init streaming events
                1, // Check every second
                java.util.concurrent.TimeUnit.SECONDS
        );

        logger.debug("Scheduling started");
    }

    /**
     * Checks all datasources and triggers sync for those whose interval has
     * expired. This method is called periodically by the scheduler.
     */
    private void checkAndTriggerSync() {
        if (!running) {
            return;
        }

        try {
            for (Map.Entry<String, DataSourceSyncMetadata> entry : dataSourceMetadata.entrySet()) {
                String dataSourceName = entry.getKey();
                DataSourceSyncMetadata metadata = entry.getValue();

                if (metadata.isStreamingDataSource()) {
                    checkStreamingDataSourceSync(dataSourceName, metadata);
                } else {
                    checkBatchDataSourceSync(dataSourceName, metadata);
                }
            }
        } catch (Exception e) {
            logger.error("Error in scheduled sync check", e);
        }
    }

    private void checkStreamingDataSourceSync(String dataSourceName, DataSourceSyncMetadata metadata) {
        checkStreamingDataSourceHealth(dataSourceName);
        if (!metadata.getSyncInterval().isZero() && metadata.shouldSync()) {
            logger.debug("Streaming datasource '{}' interval expired, triggering full sync", dataSourceName);
            synchronizeDataSource(dataSourceName);
        }
    }

    private void checkBatchDataSourceSync(String dataSourceName, DataSourceSyncMetadata metadata) {
        if (metadata.shouldSync()) {
            logger.debug("Datasource '{}' interval expired, triggering sync", dataSourceName);
            synchronizeDataSource(dataSourceName);
        }
        if (!metadata.isHealthy() && metadata.shouldRetryHealthCheck()) {
            logger.debug("Retrying health check for datasource '{}'", dataSourceName);
            retryHealthCheck(dataSourceName, metadata);
        }
    }

    /**
     * Retries health check for an unhealthy datasource.
     *
     * @param dataSourceName the datasource name
     * @param metadata the datasource metadata
     */
    private void retryHealthCheck(String dataSourceName, DataSourceSyncMetadata metadata) {
        try {
            factory.getDataSource(dataSourceName);
            metadata.markHealthy();
            logger.debug("Datasource '{}' health check succeeded, marked as healthy", dataSourceName);

            // Trigger sync now that it's healthy
            synchronizeDataSource(dataSourceName);

        } catch (Exception e) {
            metadata.recordFailure("Health check retry failed: " + e.getMessage());
            logger.warn("Datasource '{}' health check retry failed: {}",
                    dataSourceName, e.getMessage());
        }
    }

    /**
     * Checks the health of a single streaming datasource and handles state transitions.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void checkStreamingDataSourceHealth(String dsName) {
        StreamingDataSource streamingDs = factory.getStreamingDataSource(dsName);
        if (streamingDs == null) {
            return;
        }

        StreamingDataSourceState currentState = lifecycleManager.getState(dsName);
        boolean healthy = streamingDs.isHealthy();

        // INITIALIZING timeout check Ã¢â‚¬â€ detect datasources stuck without initial load
        if (currentState == StreamingDataSourceState.INITIALIZING
                && lifecycleManager.checkInitialLoadTimeout(dsName)) {
            DataSourceSyncMetadata meta = dataSourceMetadata.get(dsName);
            if (meta != null) {
                meta.updateStreamingState(StreamingDataSourceState.ERROR);
            }
            scheduleReconnection(dsName);
            return;
        }

        if (!healthy && currentState != StreamingDataSourceState.ERROR) {
            // Connection lost Ã¢â‚¬â€ transition to ERROR
            lifecycleManager.handleConnectionLoss(dsName, "Health check detected unhealthy datasource");

            DataSourceSyncMetadata meta = dataSourceMetadata.get(dsName);
            if (meta != null) {
                meta.updateStreamingState(StreamingDataSourceState.ERROR);
            }

            // Schedule reconnection attempt
            scheduleReconnection(dsName);
        } else if (!healthy && currentState == StreamingDataSourceState.ERROR) {
            // Already in ERROR Ã¢â‚¬â€ attempt reconnection if not already scheduled
            scheduleReconnection(dsName);
        }
    }

    /**
     * Schedules a reconnection attempt for a streaming datasource with backoff delay.
     * Uses pendingReconnections guard to prevent duplicate scheduling.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void scheduleReconnection(String dsName) {
        if (!pendingReconnections.add(dsName)) {
            // Already has a pending reconnection scheduled Ã¢â‚¬â€ skip
            return;
        }

        java.time.Duration delay = lifecycleManager.calculateNextReconnectDelay(dsName);
        logger.debug("Scheduling reconnection for '{}' with delay {}", dsName, delay);

        scheduler.schedule(() -> {
            try {
                attemptReconnection(dsName);
            } catch (Exception e) {
                logger.error("Reconnection attempt failed for streaming datasource '{}'", dsName, e);
                pendingReconnections.remove(dsName);
            }
        }, delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to reconnect a streaming datasource. On success, removes old listener,
     * subscribes new listener, and restarts initial data load. On failure, records
     * the failure for backoff calculation and schedules the next attempt with backoff.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void attemptReconnection(String dsName) {
        StreamingDataSource streamingDs = factory.getStreamingDataSource(dsName);
        if (streamingDs == null) {
            pendingReconnections.remove(dsName);
            return;
        }

        try {
            // Check if datasource has recovered
            if (!streamingDs.isHealthy()) {
                lifecycleManager.recordReconnectionFailure(dsName);
                logger.warn("Reconnection failed for '{}' - datasource still unhealthy, scheduling next attempt with backoff", dsName);
                pendingReconnections.remove(dsName);
                scheduleReconnection(dsName);
                return;
            }

            // Remove old listener to prevent duplicate subscriptions
            BatchSnapshotEventListener<?> oldListener = streamingListeners.remove(dsName);
            if (oldListener != null) {
                streamingDs.unsubscribe(oldListener);
            }

            // Create and subscribe new listener
            final String capturedDsName2 = dsName;
            BatchSnapshotEventListener listener = event -> {
                synchronized (streamingEventLock) {
                    if (incrementalSyncProcessor.isFullSyncInProgress()) {
                        incrementalSyncProcessor.queueEvent(capturedDsName2, event);
                    } else {
                        incrementalSyncProcessor.processBatchSnapshot(capturedDsName2, event);
                        streamingVersion.incrementAndGet();
                        lastStreamingUpdateTimestamp = Instant.now();
                    }
                }
            };
            streamingDs.subscribe(listener);
            streamingListeners.put(dsName, listener);

            // Handle successful reconnection - transitions to INITIALIZING for initial load restart
            lifecycleManager.handleReconnectionSuccess(dsName);
            lifecycleManager.resetRegistrationTime(dsName);

            DataSourceSyncMetadata meta = dataSourceMetadata.get(dsName);
            if (meta != null) {
                meta.updateStreamingState(StreamingDataSourceState.INITIALIZING);
            }

            // Clear guard - reconnection complete
            pendingReconnections.remove(dsName);

            performStreamingInitialLoad(dsName, streamingDs, meta);

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lifecycleManager.recordReconnectionFailure(dsName);
            logger.error("Reconnection attempt failed for '{}': {}", dsName, e.getMessage(), e);
            // Clear guard and schedule next attempt with backoff
            pendingReconnections.remove(dsName);
            scheduleReconnection(dsName);
        }
    }

    /**
     * Closes the engine and performs graceful shutdown.
     *
     * <p>
     * This method:</p>
     * <ul>
     * <li>Stops accepting new synchronization requests</li>
     * <li>Waits for ongoing synchronization to complete</li>
     * <li>Shuts down thread pools gracefully</li>
     * </ul>
     *
     * <p>
     * This method blocks until all resources are released.</p>
     */
    public void close() {
        boolean wasRunning = running;

        if (!wasRunning) {
            logger.debug("Engine is not running, shutting down thread pools only");
        } else {
            logger.debug("Closing DataSynchronizationEngine...");
        }

        // Mark as not running to stop new operations
        running = false;

        try {
            if (wasRunning) {
                // Wait for ongoing sync to complete (with timeout)
                if (syncLock.isLocked()) {
                    logger.debug("Waiting for ongoing synchronization to complete...");
                    boolean acquired = syncLock.tryLock(30, java.util.concurrent.TimeUnit.SECONDS);
                    if (acquired) {
                        syncLock.unlock();
                        logger.debug("Ongoing synchronization completed");
                    } else {
                        logger.warn("Timeout waiting for synchronization to complete");
                    }
                }

                // Cleanup streaming datasource subscriptions and resources
                cleanupStreamingDataSources();
            }

            // Always shutdown thread pools Ã¢â‚¬â€ they are created in the constructor
            // and must be cleaned up even if initialize() was never called or failed
            logger.debug("Shutting down scheduler...");
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("Scheduler did not terminate gracefully, forced shutdown");
            }

            logger.debug("Shutting down worker pool...");
            workerPool.shutdown();
            if (!workerPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
                logger.warn("Worker pool did not terminate gracefully, forced shutdown");
            }

            logger.debug("DataSynchronizationEngine closed successfully");

        } catch (InterruptedException e) {
            logger.error("Interrupted while closing engine", e);
            Thread.currentThread().interrupt();

            // Force shutdown
            scheduler.shutdownNow();
            workerPool.shutdownNow();
        }
    }

    /**
     * Cleans up all streaming datasource subscriptions and resources.
     * Unsubscribes listeners, closes streaming datasources, clears lifecycle manager,
     * and removes the initial load callback from IncrementalSyncProcessor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cleanupStreamingDataSources() {
        Set<String> streamingNames = factory.getAllStreamingDataSourceNames();
        if (streamingNames.isEmpty()) {
            logger.debug("No streaming datasources to clean up");
            return;
        }

        logger.debug("Cleaning up {} streaming datasource(s)...", streamingNames.size());

        for (String dsName : streamingNames) {
            try {
                StreamingDataSource streamingDs = factory.getStreamingDataSource(dsName);
                if (streamingDs == null) {
                    logger.debug("Streaming datasource '{}' not found, skipping cleanup", dsName);
                    continue;
                }

                // 1. Unsubscribe the listener
                unsubscribeStreamingListener(dsName, streamingDs);

                // 2. Close the streaming datasource
                closeStreamingDataSource(dsName, streamingDs);

            } catch (Exception e) {
                logger.warn("Error during cleanup of streaming datasource '{}': {}",
                        dsName, e.getMessage(), e);
            }
        }

        // 3. Clear lifecycle manager
        if (lifecycleManager != null) {
            try {
                lifecycleManager.clear();
                logger.debug("Lifecycle manager cleared");
            } catch (Exception e) {
                logger.warn("Error clearing lifecycle manager: {}", e.getMessage(), e);
            }
        }

        logger.debug("Streaming datasource cleanup completed");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void unsubscribeStreamingListener(String dsName, StreamingDataSource streamingDs) {
        BatchSnapshotEventListener listener = streamingListeners.remove(dsName);
        if (listener != null) {
            try {
                streamingDs.unsubscribe(listener);
                logger.debug("Unsubscribed listener from streaming datasource '{}'", dsName);
            } catch (Exception e) {
                logger.warn("Error unsubscribing from streaming datasource '{}': {}",
                        dsName, e.getMessage(), e);
            }
        }
    }

    private void closeStreamingDataSource(String dsName, StreamingDataSource<?> streamingDs) {
        try {
            streamingDs.close();
            logger.debug("Closed streaming datasource '{}'", dsName);
        } catch (Exception e) {
            logger.warn("Error closing streaming datasource '{}': {}",
                    dsName, e.getMessage(), e);
        }
    }

    /**
     * Marks a datasource for synchronization and triggers global sync.
     *
     * <p>
     * This method is called when:</p>
     * <ul>
     * <li>A datasource's interval expires</li>
     * <li>Manual synchronization is requested</li>
     * <li>A datasource recovers from unhealthy state</li>
     * </ul>
     *
     * <p>
     * The actual synchronization is performed asynchronously by the global
     * synchronization process, which will read all pending datasources and
     * update all affected consumers.</p>
     *
     * @param dataSourceName the datasource name to synchronize
     * @throws IllegalArgumentException if datasource name is null or not
     * registered
     */
    public void synchronizeDataSource(String dataSourceName) {
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Datasource name cannot be null or empty");
        }

        if (!dataSourceMetadata.containsKey(dataSourceName)) {
            throw new IllegalArgumentException(
                    "Datasource not registered: " + dataSourceName);
        }

        logger.debug("Marking datasource '{}' for synchronization", dataSourceName);

        // Mark datasource as pending
        markDataSourceForSync(dataSourceName);

        // Trigger global synchronization
        triggerGlobalSynchronization();
    }

    /**
     * Triggers global synchronization for all pending datasources.
     *
     * <p>
     * This method orchestrates the entire synchronization process:</p>
     * <ol>
     * <li>Checks for reentrant calls and returns if already syncing</li>
     * <li>Acquires sync lock to prevent concurrent synchronization</li>
     * <li>Gets all pending datasources that need to be re-read</li>
     * <li>Creates a new DataVersion to hold synchronized data</li>
     * <li>MAP PHASE: Reads all datasources in parallel</li>
     * <li>Extracts all PropertyMappings from consumers</li>
     * <li>REDUCE PHASE: Populates all entities in the new DataVersion</li>
     * <li>Atomically swaps to the new DataVersion</li>
     * <li>Pushes populated data to all consumers</li>
     * <li>Clears pending datasources</li>
     * </ol>
     *
     * <p>
     * Thread Safety: This method uses a ReentrantLock to ensure only one
     * synchronization runs at a time. Reentrant calls are detected and
     * ignored.</p>
     */
    void triggerGlobalSynchronization() {
        // Reentrant check: if current thread already holds the lock, return
        if (syncLock.isHeldByCurrentThread()) {
            logger.warn("Sync already in progress in current thread, skipping reentrant call");
            return;
        }

        // Try to acquire lock - if another sync is in progress, return immediately
        if (!syncLock.tryLock()) {
            logger.debug("Another synchronization is already in progress, skipping this request");
            return;
        }

        long startTime = System.currentTimeMillis();
        long phaseStartTime;

        // Timing breakdown
        long readDataSourcesTime = 0;
        long extractMappingsTime = 0;
        long populateEntitiesTime = 0;
        long pushToConsumersTime = 0;
        
        // Declare newVersion outside try block so it's accessible in catch block
        DataVersion newVersion = null;

        try {
            // Signal IncrementalSyncProcessor to queue incoming streaming events during full sync
            if (incrementalSyncProcessor != null) {
                incrementalSyncProcessor.setFullSyncInProgress(true);
            }

            logger.debug("Starting global synchronization...");

            // Step 1: Get all datasources marked for sync
            Set<String> dataSourcesNeedingSync = getPendingDataSources();

            // If no datasources need sync, return early
            if (dataSourcesNeedingSync.isEmpty()) {
                logger.debug("No pending datasources, skipping synchronization");
                return;
            }

            logger.debug("Synchronizing {} datasource(s): {}",
                    dataSourcesNeedingSync.size(), dataSourcesNeedingSync);

            // Step 2: Create new DataVersion (empty, will be populated)
            newVersion = createNewDataVersion();
            logger.debug("Created new DataVersion: {}", newVersion);

            // Step 3: Get ALL consumer IDs (stores + dashboards)
            java.util.List<String> allConsumerIds = getAllConsumerIds();
            logger.debug("Found {} consumer(s) to update", allConsumerIds.size());

            // MAP PHASE: Collect all data from all datasources
            // Step 4: Read ALL datasources in PARALLEL
            // - Datasources in dataSourcesNeedingSync: re-read
            // - Other datasources: copy from current DataVersion
            phaseStartTime = System.currentTimeMillis();
            readAllDataSources(newVersion, dataSourcesNeedingSync);
            readDataSourcesTime = System.currentTimeMillis() - phaseStartTime;

            // Step 5: Extract ALL property mappings from all consumers
            phaseStartTime = System.currentTimeMillis();
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings
                    = factory.getAllPropertyMappings();
            logger.debug("Extracted {} property mapping(s)", allPropertyMappings.size());
            extractMappingsTime = System.currentTimeMillis() - phaseStartTime;

            // REDUCE PHASE: Process all collected data
            // Step 6: Populate ALL entities in newVersion
            phaseStartTime = System.currentTimeMillis();
            populateAllEntitiesInDataVersion(newVersion, allPropertyMappings);
            populateEntitiesTime = System.currentTimeMillis() - phaseStartTime;

            // Step 7: Atomic swap to new DataVersion
            DataVersion oldVersion = currentDataVersion.get();
            swapDataVersion(newVersion);

            // Step 8: Push populated data to ALL stores/dashboards
            phaseStartTime = System.currentTimeMillis();
            pushDataToAllConsumers(newVersion, allConsumerIds);
            pushToConsumersTime = System.currentTimeMillis() - phaseStartTime;

            // Step 9: Clear intermediate data from newVersion after push
            // This frees memory by removing temporary data structures that are no longer needed
            newVersion.clearIntermediateData();
            logger.debug("Cleared intermediate data from new DataVersion {}", newVersion.getVersion());

            // Step 10: Deep clear index cache
            // This ensures nested map structures are properly cleared for garbage collection
            indexManager.clearAllIndexesDeep();
            logger.debug("Deep cleared all index caches");

            // Step 11: Clear intermediate data from old DataVersion
            // This helps garbage collection by breaking reference chains faster
            if (oldVersion != null) {
                oldVersion.clearIntermediateData();
                logger.debug("Cleared intermediate data from old DataVersion {}", oldVersion.getVersion());
            }

            // Step 12: Clear pending datasources
            clearPendingDataSources();

            drainQueuedStreamingEvents();

            // Log concise summary with duration breakdown
            long totalDuration = System.currentTimeMillis() - startTime;
            logSynchronizationSummary(newVersion, dataSourcesNeedingSync, allConsumerIds,
                    totalDuration, readDataSourcesTime, extractMappingsTime,
                    populateEntitiesTime, pushToConsumersTime);

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Error during global synchronization", e);
            // Cleanup partial data structures on failure
            cleanupOnFailure(newVersion);
            throw new DataSynchronizationException("Global synchronization failed", e);

        } finally {
            // Always reset the full sync flag and process queued events, even on failure.
            // This prevents streaming events from being stuck in the queue indefinitely.
            drainQueuedStreamingEventsSafely();

            // Always release the lock
            syncLock.unlock();
            logger.debug("Released sync lock");
        }
    }

    private void drainQueuedStreamingEvents() {
        if (incrementalSyncProcessor == null) {
            return;
        }
        synchronized (streamingEventLock) {
            int queuedBefore = incrementalSyncProcessor.getQueuedEventCount();
            incrementalSyncProcessor.setFullSyncInProgress(false);
            incrementalSyncProcessor.processQueuedEvents();
            if (queuedBefore > 0 && incrementalSyncProcessor.getQueuedEventCount() == 0) {
                lastStreamingUpdateTimestamp = Instant.now();
            }
        }
    }

    private void drainQueuedStreamingEventsSafely() {
        if (incrementalSyncProcessor == null || !incrementalSyncProcessor.isFullSyncInProgress()) {
            return;
        }
        try {
            drainQueuedStreamingEvents();
        } catch (Exception ex) {
            logger.warn("Error processing queued streaming events after sync: {}",
                    ex.getMessage(), ex);
        }
    }

    /**
     * Cleans up partial data structures when synchronization fails.
     * 
     * <p>This method ensures that when synchronization fails for any reason,
     * all partial data structures are properly cleaned up to prevent memory leaks.
     * It is safe to call this method even if the DataVersion is null or partially populated.</p>
     * 
     * <p>Cleanup operations performed:</p>
     * <ul>
     * <li>Clears intermediate data from partial DataVersion (if not null)</li>
     * <li>Deep clears all index caches to free nested map structures</li>
     * </ul>
     * 
     * <p>This method is idempotent and safe to call multiple times.</p>
     * 
     * @param partialVersion the partially populated DataVersion, may be null
     */
    private void cleanupOnFailure(DataVersion partialVersion) {
        logger.debug("Cleaning up partial data structures after synchronization failure");
        
        try {
            // Clear intermediate data from partial DataVersion if it exists
            if (partialVersion != null) {
                partialVersion.clearIntermediateData();
                logger.debug("Cleared intermediate data from partial DataVersion {}", 
                    partialVersion.getVersion());
            }
            
            // Deep clear all index caches
            indexManager.clearAllIndexesDeep();
            logger.debug("Deep cleared all index caches");
            
        } catch (Exception e) {
            // Log but don't throw - we're already in an error state
            logger.warn("Error during cleanup on failure: {}", e.getMessage());
        }
    }

    /**
     * Creates a new DataVersion with incremented version number. Initializes
     * all data structures (populatedEntities, dataByDataSource, groupedData,
     * commonAggregationResults) as empty maps.
     *
     * @return a new DataVersion ready to be populated
     */
    private DataVersion createNewDataVersion() {
        DataVersion currentVersion = currentDataVersion.get();
        long newVersionNumber = currentVersion.getVersion() + 1;
        LocalDateTime timestamp = LocalDateTime.now();

        DataVersion newVersion = new DataVersion(newVersionNumber, timestamp);

        logger.debug("Created new DataVersion: version={}, timestamp={}",
                newVersionNumber, timestamp);

        return newVersion;
    }

    /**
     * Gets all consumer IDs (stores + dashboards) from the factory.
     *
     * @return list of all consumer IDs
     */
    private java.util.List<String> getAllConsumerIds() {
        return factory.getAllConsumerIds();
    }

    /**
     * Marks a datasource as needing synchronization. Adds the datasource to the
     * pending set in a thread-safe manner.
     *
     * @param dataSourceName the datasource name
     */
    private void markDataSourceForSync(String dataSourceName) {
        boolean added = pendingDataSources.add(dataSourceName);
        if (added) {
            logger.debug("Datasource '{}' added to pending sync set", dataSourceName);
        } else {
            logger.debug("Datasource '{}' already in pending sync set", dataSourceName);
        }
    }

    /**
     * Gets a copy of the pending datasources set. This method is thread-safe
     * and returns a snapshot of pending datasources.
     *
     * @return a new set containing all pending datasource names
     */
    Set<String> getPendingDataSources() {
        return new java.util.HashSet<>(pendingDataSources);
    }

    /**
     * Clears all pending datasources. This method is called after a successful
     * global synchronization.
     */
    void clearPendingDataSources() {
        int count = pendingDataSources.size();
        pendingDataSources.clear();
        logger.debug("Cleared {} pending datasources", count);
    }

    // ==================== MAP PHASE: Parallel Datasource Reading ====================
    /**
     * Reads all datasources in parallel and stores them in the new DataVersion.
     *
     * <p>
     * This method implements the MAP PHASE of the synchronization process:</p>
     * <ol>
     * <li>Collects all unique datasources (primary + secondary from
     * PropertyMappings)</li>
     * <li>For each datasource:
     * <ul>
     * <li>IF in pendingDataSources: re-read with
     * {@link #ensureDataSourceInDataVersion}</li>
     * <li>ELSE: copy reference from currentDataVersion.dataByDataSource</li>
     * </ul>
     * </li>
     * <li>Parallel reading using workerPool (ExecutorService)</li>
     * <li>Store in newVersion.dataByDataSource</li>
     * </ol>
     *
     * <p>
     * Thread Safety: This method uses parallel processing via workerPool. All
     * datasource reads are independent and can be executed concurrently.</p>
     *
     * @param newVersion the new DataVersion to populate
     * @param dataSourcesNeedingSync set of datasources that need to be re-read
     */
    private void readAllDataSources(
            DataVersion newVersion,
            java.util.Set<String> dataSourcesNeedingSync) {

        logger.debug("MAP PHASE: Starting parallel datasource reading...");

        // PHASE 1: Collect all unique datasources
        java.util.Set<String> allDatasources = new java.util.HashSet<>();

        // Add all registered datasources (includes primary datasources)
        allDatasources.addAll(factory.getAllDataSourceNames());

        // Add secondary datasources from PropertyMappings
        java.util.List<PropertyMapping<?, ?>> allPropertyMappings = factory.getAllPropertyMappings();
        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            allDatasources.add(mapping.getDataSourceName());
        }

        logger.debug("Found {} unique datasource(s) to process", allDatasources.size());

        // PHASE 2: Read all datasources in PARALLEL
        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (String dataSourceName : allDatasources) {
            java.util.concurrent.CompletableFuture<Void> future
                    = java.util.concurrent.CompletableFuture.runAsync(
                        () -> readSingleDataSource(newVersion, dataSourcesNeedingSync, dataSourceName),
                        workerPool);

            futures.add(future);
        }

        // Wait for all datasource reads to complete
        try {
            java.util.concurrent.CompletableFuture.allOf(
                    futures.toArray(new java.util.concurrent.CompletableFuture[0])
            ).join();

            logger.debug("MAP PHASE: Successfully read {} datasource(s) in parallel", allDatasources.size());

            // Read streaming datasource data from DependencyGraph during full sync.
            loadStreamingDataFromDependencyGraph(newVersion);

        } catch (java.util.concurrent.CompletionException e) {
            // Unwrap the CompletionException to get the actual cause
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("MAP PHASE: Error during parallel datasource reading. Root cause: {}",
                    cause != null ? cause.getMessage() : e.getMessage(), cause != null ? cause : e);
            throw new DataSynchronizationException("Parallel datasource reading failed", cause != null ? cause : e);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("MAP PHASE: Error during parallel datasource reading", e);
            throw new DataSynchronizationException("Parallel datasource reading failed", e);
        }
    }

    private void readSingleDataSource(DataVersion newVersion, java.util.Set<String> dataSourcesNeedingSync, String dataSourceName) {
        try {
            if (dataSourcesNeedingSync.contains(dataSourceName)) {
                logger.debug("Re-reading datasource '{}' (marked for sync)", dataSourceName);
                ensureDataSourceInDataVersion(newVersion, dataSourceName);
            } else {
                copyOrReadDataSource(newVersion, dataSourceName);
            }
            applyTimeWindowRuleToDataVersion(newVersion, dataSourceName);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Error reading datasource '{}': {}", dataSourceName, e.getMessage(), e);
            throw new DataSynchronizationException("Failed to read datasource: " + dataSourceName, e);
        }
    }

    private void copyOrReadDataSource(DataVersion newVersion, String dataSourceName) {
        DataVersion currentVersion = currentDataVersion.get();
        java.util.List<?> existingData = currentVersion.getDataByDataSource(dataSourceName);
        if (existingData != null) {
            newVersion.setDataByDataSource(dataSourceName, existingData);
            logger.debug("Copied datasource '{}' from current version ({} items)",
                    dataSourceName, existingData.size());
        } else {
            logger.debug("First time reading datasource '{}', fetching data", dataSourceName);
            ensureDataSourceInDataVersion(newVersion, dataSourceName);
        }
    }

    private void loadStreamingDataFromDependencyGraph(DataVersion newVersion) {
        for (Map.Entry<String, DataSourceSyncMetadata> entry : dataSourceMetadata.entrySet()) {
            DataSourceSyncMetadata metadata = entry.getValue();
            if (!metadata.isStreamingDataSource()) {
                continue;
            }
            String dsName = entry.getKey();
            if (dependencyGraph == null) {
                continue;
            }
            java.util.List<?> entities = dependencyGraph.findAll(dsName);
            if (!entities.isEmpty()) {
                newVersion.setDataByDataSource(dsName, entities);
                logger.debug("Full sync: loaded {} streaming entities from DependencyGraph for '{}' (state={})",
                        entities.size(), dsName, metadata.getStreamingState());
            } else {
                logger.debug("Full sync: DependencyGraph has no data for streaming datasource '{}' (state={})",
                        dsName, metadata.getStreamingState());
            }
        }
    }

    /**
     * Ensures that data from a datasource is present in the DataVersion. If the
     * data is not already present, reads it from the datasource and stores the
     * original reference.
     *
     * <p>
     * This method:</p>
     * <ul>
     * <li>Checks if datasource data already exists in
     * newVersion.dataByDataSource</li>
     * <li>If not present: reads from datasource using
     * {@link #readFromDataSourceRaw}</li>
     * <li>Stores original reference in dataByDataSource</li>
     * </ul>
     *
     * <p>
     * The original reference is used throughout the synchronization process,
     * eliminating garbage collection overhead.</p>
     *
     * @param newVersion the DataVersion to populate
     * @param dataSourceName the datasource name
     * @return the data (original reference)
     */
    private java.util.List<?> ensureDataSourceInDataVersion(
            DataVersion newVersion,
            String dataSourceName) {

        // Check if already present
        java.util.List<?> existingData = newVersion.getDataByDataSource(dataSourceName);
        if (existingData != null) {
            logger.debug("Datasource '{}' already in DataVersion, returning existing data", dataSourceName);
            return existingData;
        }

        // Read from datasource (returns REFERENCE)
        logger.debug("Reading datasource '{}' from source...", dataSourceName);
        java.util.List<?> rawData = readFromDataSourceRaw(dataSourceName);

        // Store original reference in dataByDataSource
        newVersion.setDataByDataSource(dataSourceName, rawData);

        logger.debug("Stored {} items from datasource '{}' in DataVersion",
                rawData.size(), dataSourceName);

        return rawData;
    }

    /**
     * Applies TimeWindowRule filtering to data already stored in DataVersion.
     * If a TimeWindowRule is defined for the datasource, expired entities are
     * filtered out and the DataVersion is updated with only valid entities.
     * If no TimeWindowRule is defined, data passes through unchanged.
     *
     * @param newVersion     the DataVersion containing the data
     * @param dataSourceName the datasource name to check for TimeWindowRule
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyTimeWindowRuleToDataVersion(DataVersion newVersion, String dataSourceName) {
        TimeWindowRule timeWindowRule = (TimeWindowRule) factory.getTimeWindowRule(dataSourceName);
        if (timeWindowRule == null) {
            return; // No TimeWindowRule Ã¢â‚¬â€ pass through unchanged
        }

        java.util.List<?> data = newVersion.getDataByDataSource(dataSourceName);
        if (data == null || data.isEmpty()) {
            return;
        }

        Specification spec = (Specification) timeWindowRule.getSpecificationFactory().get();
        java.util.List<Object> filtered = new java.util.ArrayList<>(data.size());

        for (Object entity : data) {
            try {
                if (spec.test(entity)) {
                    filtered.add(entity);
                } else {
                    logger.debug("Full sync: entity filtered out by TimeWindowRule for datasource '{}'",
                            dataSourceName);
                }
            } catch (Exception e) {
                logger.warn("Full sync: error evaluating TimeWindowRule for entity in datasource '{}': {}",
                        dataSourceName, e.getMessage());
            }
        }

        int removedCount = data.size() - filtered.size();
        if (removedCount > 0) {
            newVersion.setDataByDataSource(dataSourceName, filtered);
            logger.debug("Full sync: TimeWindowRule filtered {} of {} entities for datasource '{}'",
                    removedCount, data.size(), dataSourceName);
        }
    }

    /**
     * Reads raw data from a datasource. Returns a REFERENCE to the data.
     *
     * <p>
     * This method determines whether to read from synced data or fetch new
     * data:</p>
     * <ul>
     * <li>IF datasource is synced: uses getCurrentData() (returns
     * reference)</li>
     * <li>IF datasource is out-of-sync: uses readData() (fetches new data)</li>
     * </ul>
     *
     * <p>
     * <b>IMPORTANT:</b> This method returns a REFERENCE. The original
     * reference is used throughout the synchronization process.</p>
     *
     * @param dataSourceName the datasource name
     * @return raw data from the datasource (original reference)
     * @throws RuntimeException if datasource read fails
     */
    /**
     * Reads data from a datasource with health check and fallback support.
     *
     * <p>
     * This method implements the following fallback chain:</p>
     * <ol>
     * <li>Primary DataSource: Attempts to read from the primary datasource</li>
     * <li>Fallback DataSource(s): If primary fails, tries fallback chain</li>
     * <li>Empty List: If all datasources fail, returns empty list for graceful
     * degradation</li>
     * </ol>
     *
     * <p>
     * Health Check Logic:</p>
     * <ul>
     * <li>Checks metadata health status before reading</li>
     * <li>Checks datasource health status (isHealthy())</li>
     * <li>Records success/failure in metadata</li>
     * <li>Automatic recovery when datasource becomes healthy</li>
     * </ul>
     *
     * @param dataSourceName the datasource name
     * @return the data list (never null, may be empty on total failure)
     * @throws RuntimeException if datasource read fails and no fallback
     * succeeds
     */
    private java.util.List<?> readFromDataSourceRaw(String dataSourceName) {
        try {
            // Get the datasource
            DataSource<?> dataSource = factory.getDataSource(dataSourceName);

            // Get metadata to check sync status and get timeout
            DataSourceSyncMetadata metadata = dataSourceMetadata.get(dataSourceName);

            // Get timeout from metadata
            // Note: metadata.getReadTimeout() is never null because DataSourceSyncMetadata
            // constructor ensures it's set to DEFAULT_READ_TIMEOUT if null is passed
            long timeoutSeconds = metadata != null
                    ? metadata.getReadTimeout().getSeconds()
                    : DEFAULT_READ_TIMEOUT_SECONDS;

            // HEALTH CHECK: Check datasource health before attempting to read
            if (metadata != null && !metadata.isHealthy()) {
                logger.warn("Datasource '{}' is unhealthy (consecutive failures: {}), attempting read with fallback",
                        dataSourceName, metadata.getConsecutiveFailures());
            }

            // Check datasource health status
            if (!dataSource.isHealthy()) {
                logger.warn("Datasource '{}' reports unhealthy status, will use fallback chain",
                        dataSourceName);

                // Update metadata to reflect unhealthy status
                if (metadata != null) {
                    metadata.markUnhealthy("DataSource reports unhealthy status");
                }
            }

            // Determine if we should use cached data or fetch new data
            // For now, always fetch new data (synced vs out-of-sync logic can be enhanced later)
            logger.debug("Fetching data from datasource '{}' with timeout {} seconds...",
                    dataSourceName, timeoutSeconds);

            // FALLBACK MECHANISM: Fetch data using fetchAllWithFallback
            // This implements the fallback chain: Primary Ã¢â€ â€™ Fallback Ã¢â€ â€™ Empty
            // The DataSource.fetchAllWithFallback() method automatically:
            // 1. Checks health of primary datasource
            // 2. Tries fallback datasources if primary fails
            // 3. Returns empty list if all datasources fail (graceful degradation)
            java.util.List<?> data = dataSource.fetchAllWithFallback()
                    .get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

            // Check if data is null (should not happen, but defensive programming)
            if (data == null) {
                logger.warn("Datasource '{}' returned null, using empty list", dataSourceName);
                data = new java.util.ArrayList<>();
            }

            // Update metadata on successful read
            if (metadata != null) {
                metadata.recordSuccess();
                logger.debug("Datasource '{}' read successful, updated metadata", dataSourceName);
            }

            // Log success with data size
            if (data.isEmpty()) {
                logger.warn("Datasource '{}' returned empty list (all fallbacks may have failed)",
                        dataSourceName);
            } else {
                logger.debug("Fetched {} items from datasource '{}'", data.size(), dataSourceName);
            }

            return data;

        } catch (java.util.concurrent.TimeoutException e) {
            return handleDataSourceReadFailure(dataSourceName, "Timeout: " + e.getMessage(),
                    "timeout (graceful degradation)");

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Error reading from datasource '{}' (all fallbacks failed): {}",
                    dataSourceName, e.getMessage(), e);
            return handleDataSourceReadFailure(dataSourceName, e.getMessage(),
                    "error (graceful degradation)");
        }
    }

    private java.util.List<?> handleDataSourceReadFailure(String dataSourceName, String failureMessage, String reason) {
        DataSourceSyncMetadata metadata = dataSourceMetadata.get(dataSourceName);
        if (metadata != null) {
            metadata.recordFailure(failureMessage);
        }
        logger.warn("Returning empty list for datasource '{}' due to {} ", dataSourceName, reason);
        return new java.util.ArrayList<>();
    }

    // ==================== REDUCE PHASE: Main Orchestration ====================
    /**
     * Populates all entities in the DataVersion by orchestrating the two main
     * phases: Data Collection and Data Production/Mapping.
     *
     * <p>
     * This method implements the REDUCE PHASE of the synchronization
     * process:</p>
     * <ol>
     * <li>PHASE 1: Uses cached analysis result (computed once during
     * initialization)</li>
     * <li>PHASE 2: {@link #collectAllRequiredData} - Collects all required
     * data</li>
     * <li>PHASE 3: {@link #produceAndMapData} - Produces and maps data to
     * entities</li>
     * </ol>
     *
     * <p>
     * This orchestration ensures that:</p>
     * <ul>
     * <li>Analysis is performed once during initialization and reused
     * (cached)</li>
     * <li>All required data is collected before mapping</li>
     * <li>Data production and mapping happens in an optimized manner</li>
     * </ul>
     *
     * <p>
     * Note: The analysis phase is NOT performed here. It's done once during
     * engine initialization in {@link #performInitialAnalysis()} and the result
     * is cached in {@link #cachedAnalysisResult}.</p>
     *
     * @param newVersion the new DataVersion to populate
     * @param allPropertyMappings list of all property mappings from all
     * consumers
     */
    private void populateAllEntitiesInDataVersion(
            DataVersion newVersion,
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("REDUCE PHASE: Starting populateAllEntitiesInDataVersion...");

        // PHASE 1: Use cached analysis result (computed once during initialization)
        logger.debug("REDUCE PHASE 1: Using cached analysis result...");
        com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analysisResult = cachedAnalysisResult;
        logger.debug("REDUCE PHASE 1: Using cached analysis - {}", analysisResult);

        // PHASE 2: DATA COLLECTION - Collect all required data
        logger.debug("REDUCE PHASE 2: Collecting all required data...");
        collectAllRequiredData(newVersion, analysisResult);
        logger.debug("REDUCE PHASE 2: Data collection complete");

        // PHASE 3: DATA PRODUCTION - Produce and map data
        logger.debug("REDUCE PHASE 3: Producing and mapping data...");
        produceAndMapData(newVersion, allPropertyMappings, analysisResult);
        logger.debug("REDUCE PHASE 3: Data production and mapping complete");

        logger.debug("REDUCE PHASE: populateAllEntitiesInDataVersion completed successfully");
    }

    /**
     * PHASE 1: Analyzes aggregations and mappings to identify common groupings,
     * dashboard aggregation plans, and source datasources.
     *
     * <p>
     * This method will be implemented in task 8.1.</p>
     *
     * @param newVersion the new DataVersion
     * @param allPropertyMappings list of all property mappings
     * @return the analysis result containing common groupings, dashboard plans,
     * and source datasources
     */
    /**
     * ANALYSIS PHASE: Analyzes all property mappings to identify common
     * groupings, dashboard aggregation plans, and source datasources.
     *
     * <p>
     * This method orchestrates the analysis phase by:</p>
     * <ol>
     * <li>Analyzing common groupings for Store primary-foreign key
     * relationships</li>
     * <li>Analyzing dashboard aggregations and creating aggregation plans</li>
     * <li>Identifying all source datasources needed</li>
     * </ol>
     *
     * <p>
     * Note: Circular dependency detection is performed once during engine
     * initialization in the {@link #validatePropertyMappings()} method, not
     * during each synchronization cycle, since property mappings don't change
     * at runtime.</p>
     *
     * @param allPropertyMappings list of all property mappings from all
     * consumers
     * @return AnalysisResult containing common groupings, dashboard plans, and
     * source datasources
     */
    private com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analyzeAggregationsAndMappings(
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("ANALYSIS PHASE: Starting aggregation and mapping analysis for {} mappings",
                allPropertyMappings.size());

        // Step 1: Analyze common groupings for Stores (primary-foreign key based)
        java.util.Map<com.thy.fss.common.inmemory.engine.analysis.GroupingKey, java.util.List<PropertyMapping<?, ?>>> commonGroupings
                = analyzeCommonGroupings(allPropertyMappings);
        logger.debug("Identified {} common grouping(s) for Stores", commonGroupings.size());

        // Step 2: Analyze dashboard aggregations and create plans
        java.util.Map<String, com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan> dashboardPlans
                = analyzeDashboardAggregations(allPropertyMappings);
        logger.debug("Created {} dashboard aggregation plan(s)", dashboardPlans.size());

        // Step 3: Identify all source datasources
        java.util.Set<String> sourceDatasources = identifySourceDatasources(allPropertyMappings);
        logger.debug("Identified {} source datasource(s)", sourceDatasources.size());

        // Note: Circular dependency detection is performed once during initialization
        // in validatePropertyMappings() method, not during each synchronization cycle
        // Create and return analysis result
        com.thy.fss.common.inmemory.engine.analysis.AnalysisResult result
                = new com.thy.fss.common.inmemory.engine.analysis.AnalysisResult(
                commonGroupings,
                dashboardPlans,
                sourceDatasources
        );

        logger.debug("ANALYSIS PHASE: Completed - {}", result);
        return result;
    }

    /**
     * Analyzes common groupings for Store property mappings. Groups mappings by
     * their primary-foreign key relationships to enable efficient data grouping
     * (single grouping operation per unique relationship).
     *
     * <p>
     * Only Store PropertyMappings are analyzed. Dashboard mappings are skipped
     * as they don't use primary-foreign key relationships.</p>
     *
     * @param allPropertyMappings list of all property mappings
     * @return map of grouping key to list of property mappings sharing that
     * grouping
     */
    private java.util.Map<com.thy.fss.common.inmemory.engine.analysis.GroupingKey, java.util.List<PropertyMapping<?, ?>>> analyzeCommonGroupings(
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("Analyzing common groupings for Store mappings...");

        java.util.Map<com.thy.fss.common.inmemory.engine.analysis.GroupingKey, java.util.List<PropertyMapping<?, ?>>> groupings
                = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            // Skip Dashboard mappings (they don't use primary-foreign key grouping)
            if (mapping.isForDashboard()) {
                continue;
            }

            // Skip Store mappings that don't require grouping
            if (!mapping.requiresGrouping()) {
                continue;
            }

            // Composite key mappings use CompositeKeyIndex which is created on-demand
            // during aggregation, so we don't need to create traditional groupings for them.
            // However, we still need to ensure the source data is available.
            if (mapping.isCompositeKey()) {
                logger.trace("Composite key mapping will use CompositeKeyIndex (created on-demand): {}", mapping);
                continue;
            }

            // Create grouping key from datasource + primary key paths + foreign key paths
            // For single-field keys only
            com.thy.fss.common.inmemory.engine.analysis.GroupingKey key
                    = new com.thy.fss.common.inmemory.engine.analysis.GroupingKey(
                    mapping.getDataSourceName(),
                    mapping.getPrimaryKeyPaths().get(0),
                    mapping.getForeignKeyPaths().get(0)
            );

            // Add mapping to this grouping
            groupings.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(mapping);

            logger.trace("Added mapping {} to grouping {}", mapping, key);
        }

        logger.debug("Found {} unique grouping(s) for Store mappings (composite keys handled separately)", groupings.size());
        return groupings;
    }

    /**
     * Analyzes dashboard aggregations and creates aggregation plans. Groups
     * mappings by dashboard ID and creates an aggregation plan for each
     * dashboard.
     *
     * <p>
     * Only Dashboard PropertyMappings are analyzed. Store mappings are
     * skipped.</p>
     *
     * @param allPropertyMappings list of all property mappings
     * @return map of dashboard ID to aggregation plan
     */
    private java.util.Map<String, com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan> analyzeDashboardAggregations(
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("Analyzing dashboard aggregations...");

        // Group mappings by dashboard ID
        java.util.Map<String, java.util.List<PropertyMapping<?, ?>>> mappingsByDashboard = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            // Only process Dashboard mappings
            if (!mapping.isForDashboard()) {
                continue;
            }

            String dashboardId = mapping.getConsumerId();
            mappingsByDashboard.computeIfAbsent(dashboardId, k -> new java.util.ArrayList<>()).add(mapping);
        }

        logger.debug("Found {} dashboard(s) with aggregations", mappingsByDashboard.size());

        // Create aggregation plan for each dashboard
        java.util.Map<String, com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan> plans
                = new java.util.HashMap<>();

        for (java.util.Map.Entry<String, java.util.List<PropertyMapping<?, ?>>> entry : mappingsByDashboard.entrySet()) {
            String dashboardId = entry.getKey();
            java.util.List<PropertyMapping<?, ?>> dashboardMappings = entry.getValue();

            com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan plan
                    = createDashboardAggregationPlan(dashboardId, dashboardMappings);

            plans.put(dashboardId, plan);

            logger.debug("Created aggregation plan for dashboard '{}' with {} task(s)",
                    dashboardId, plan.getTaskCount());
        }

        return plans;
    }

    /**
     * Creates a dashboard aggregation plan by grouping mappings by datasource +
     * specification + field path.
     *
     * <p>
     * This enables optimization where multiple aggregation types over the same
     * field path and specification can be computed in a single loop.</p>
     *
     * <p>
     * Example: If a dashboard needs SUM, AVG, and MAX of Order.amount with the
     * same specification, they will be grouped into a single AggregationTask.</p>
     *
     * <p>
     * Mappings with different specifications are grouped into separate tasks so
     * that specification filters can be applied correctly during aggregation.</p>
     *
     * @param dashboardId the dashboard ID
     * @param dashboardMappings list of property mappings for this dashboard
     * @return the aggregation plan
     */
    private com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan createDashboardAggregationPlan(
            String dashboardId,
            java.util.List<PropertyMapping<?, ?>> dashboardMappings) {

        logger.trace("Creating aggregation plan for dashboard '{}'", dashboardId);

        com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan plan
                = new com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan(dashboardId);

        // Group mappings by (datasource + specification + field path)
        // Use DataSourceSpecFieldKey for grouping Ã¢â‚¬â€ specification is included so that
        // mappings with different specifications get separate tasks
        java.util.Map<com.thy.fss.common.inmemory.engine.analysis.DataSourceSpecFieldKey, com.thy.fss.common.inmemory.engine.analysis.AggregationTask> tasksByKey
                = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : dashboardMappings) {
            String dataSourceName = mapping.getDataSourceName();
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> fieldPath = mapping.getSourcePath();
            com.thy.fss.common.inmemory.config.AggregationType aggregationType = mapping.getAggregationType();
            com.thy.fss.common.inmemory.specification.Specification<?> specification = mapping.getSpecification();

            // Create unique key including specification Ã¢â‚¬â€ different specs get separate tasks
            com.thy.fss.common.inmemory.engine.analysis.DataSourceSpecFieldKey taskKey
                    = new com.thy.fss.common.inmemory.engine.analysis.DataSourceSpecFieldKey(
                    dataSourceName, fieldPath != null ? fieldPath : java.util.Collections.emptyList(),
                    specification
            );

            // Get or create aggregation task with specification
            com.thy.fss.common.inmemory.engine.analysis.AggregationTask task
                    = tasksByKey.computeIfAbsent(taskKey, k
                            -> new com.thy.fss.common.inmemory.engine.analysis.AggregationTask(
                            dataSourceName, fieldPath, specification
                    )
            );

            // Add this mapping to the task
            task.addMapping(aggregationType, mapping);

            logger.trace("Added mapping {} to task {}", mapping, taskKey);
        }

        // Add all tasks to the plan
        for (com.thy.fss.common.inmemory.engine.analysis.AggregationTask task : tasksByKey.values()) {
            plan.addTask(task);
        }

        logger.trace("Created plan with {} task(s) for dashboard '{}'",
                plan.getTaskCount(), dashboardId);

        return plan;
    }

    /**
     * Identifies all source datasources needed for property mappings.
     *
     * @param allPropertyMappings list of all property mappings
     * @return set of datasource names
     */
    private java.util.Set<String> identifySourceDatasources(java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("Identifying source datasources...");

        java.util.Set<String> datasources = new java.util.HashSet<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            datasources.add(mapping.getDataSourceName());
        }

        logger.debug("Identified {} source datasource(s): {}", datasources.size(), datasources);
        return datasources;
    }

    /**
     * Detects circular mapping dependencies.
     *
     * <p>
     * This method builds a dependency graph from property mappings and uses
     * depth-first search (DFS) to detect cycles. If a cycle is found, a
     * CircularMappingException is thrown.</p>
     *
     * <p>
     * Note: Current implementation is a placeholder. Full circular dependency
     * detection would require tracking entity-to-entity relationships through
     * property mappings, which is complex. For now, we perform basic
     * validation.</p>
     *
     * @param allPropertyMappings list of all property mappings
     * @throws
     * com.thy.fss.common.inmemory.engine.exception.CircularMappingException if
     * circular dependency detected
     */
    private void detectCircularMappings(java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("Detecting circular mappings...");

        // Build dependency graph: consumerId Ã¯Â¿Â½ set of datasources it depends on
        java.util.Map<String, java.util.Set<String>> localDependencyGraph = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            String consumerId = mapping.getConsumerId();
            String dataSourceName = mapping.getDataSourceName();

            localDependencyGraph.computeIfAbsent(consumerId, k -> new java.util.HashSet<>())
                    .add(dataSourceName);
        }

        // For each consumer, check if it creates a cycle
        // A cycle would occur if: Consumer A depends on DataSource B,
        // and DataSource B is populated by Consumer A
        // This is a simplified check - full cycle detection would be more complex
        for (java.util.Map.Entry<String, java.util.Set<String>> entry : localDependencyGraph.entrySet()) {
            String consumerId = entry.getKey();
            java.util.Set<String> dependencies = entry.getValue();

            // Check if any dependency creates a direct cycle
            // (consumer depends on a datasource with the same name as the consumer)
            if (dependencies.contains(consumerId)) {
                // Create cycle list for exception
                java.util.List<String> cycle = new java.util.ArrayList<>();
                cycle.add(consumerId);
                cycle.add(consumerId); // Self-reference

                throw new com.thy.fss.common.inmemory.engine.exception.CircularMappingException(
                        cycle,
                        String.format("Circular mapping detected: Consumer '%s' depends on itself", consumerId)
                );
            }
        }

        logger.debug("No circular mappings detected");
    }

    /**
     * PHASE 2: Collects all required data including root data for stores and
     * source data for mappings.
     *
     * <p>
     * This method orchestrates the data collection phase:</p>
     * <ol>
     * <li>Collects root data for all stores (from primary datasources)</li>
     * <li>Collects source data for all property mappings</li>
     * </ol>
     *
     * @param newVersion the new DataVersion to populate
     * @param analysisResult the analysis result from phase 1
     */
    private void collectAllRequiredData(
            DataVersion newVersion,
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analysisResult) {

        logger.debug("DATA COLLECTION PHASE: Starting data collection...");

        // Step 1: Collect root data for stores (and create empty dashboard instances)
        collectRootDataForStores(newVersion);

        // Step 2: Collect source data for mappings (ensure all source datasources are loaded)
        collectSourceDataForMappings(newVersion, analysisResult.sourceDatasources());

        logger.debug("DATA COLLECTION PHASE: Completed data collection");
    }

    /**
     * Collects root data for all stores and creates empty dashboard instances.
     *
     * <p>
     * For each store:</p>
     * <ul>
     * <li>Gets data from primary datasource (via
     * ensureDataSourceInDataVersion)</li>
     * <li>Applies specification filter if defined (immutable)</li>
     * <li>Stores original references in newVersion.populatedEntities[storeId]</li>
     * </ul>
     *
     * <p>
     * For each dashboard:</p>
     * <ul>
     * <li>Creates empty dashboard instance</li>
     * <li>Stores in newVersion.populatedEntities[dashboardId]</li>
     * </ul>
     *
     * <p>
     * <b>IMPORTANT:</b> Original entity references are used throughout,
     * eliminating garbage collection overhead.</p>
     *
     * @param newVersion the new DataVersion to populate
     */
    @SuppressWarnings("unchecked")
    private void collectRootDataForStores(DataVersion newVersion) {
        logger.debug("Collecting root data for stores...");

        // Process all stores
        java.util.List<String> storeIds = factory.getAllStoreIds();
        logger.debug("Found {} store(s) to process", storeIds.size());

        for (String storeId : storeIds) {
            try {
                // Get store
                InMemoryDataStore<?> store = factory.getStoreById(storeId);
                if (store == null) {
                    logger.warn("Store not found for ID: {}", storeId);
                    continue;
                }

                // Get primary datasource name
                String primaryDataSourceName = store.getPrimaryDataSourceName();
                if (primaryDataSourceName == null || primaryDataSourceName.trim().isEmpty()) {
                    logger.warn("Store '{}' has no primary datasource, skipping", storeId);
                    continue;
                }

                // 1. Get data from datasource
                ensureDataSourceInDataVersion(newVersion, primaryDataSourceName);
                java.util.List<?> sourceData = newVersion.getDataByDataSource(primaryDataSourceName);

                if (sourceData == null || sourceData.isEmpty()) {
                    logger.debug("No data found in datasource '{}' for store '{}'", primaryDataSourceName, storeId);
                    newVersion.setPopulatedEntities(storeId, java.util.Collections.emptyList());
                    continue;
                }

                // 2. Apply specification filter if defined
                java.util.List<?> filteredData = sourceData;
                if (store.getRootSpecification() != null) {
                    filteredData = applySpecificationImmutable(
                            (java.util.List) sourceData,
                            (com.thy.fss.common.inmemory.specification.Specification) store.getRootSpecification()
                    );
                    logger.debug("Applied root specification filter for store '{}': {} -> {} items",
                            storeId, sourceData.size(), filteredData.size());
                }

                // 3. Store the filtered data (original references)
                newVersion.setPopulatedEntities(storeId, filteredData);

                logger.debug("Collected {} root entities for store '{}' from datasource '{}'",
                        filteredData.size(), storeId, primaryDataSourceName);

            } catch (Exception e) {
                logger.error("Error collecting root data for store '{}': {}", storeId, e.getMessage(), e);
                // Continue with other stores
            }
        }

        // Process all dashboards - create empty instances
        createEmptyDashboardInstances(newVersion);

        logger.debug("Root data collection completed");
    }

    private void createEmptyDashboardInstances(DataVersion newVersion) {
        java.util.List<String> dashboardIds = factory.getAllDashboardIds();
        logger.debug("Found {} dashboard(s) to process", dashboardIds.size());

        for (String dashboardId : dashboardIds) {
            try {
                Dashboard<?> dashboard = factory.getDashboardById(dashboardId);
                if (dashboard == null) {
                    logger.warn("Dashboard not found for ID: {}", dashboardId);
                    continue;
                }

                Class<?> targetClass = dashboard.getTargetClass();
                com.thy.fss.common.inmemory.specification.SpecificationService service
                        = com.thy.fss.common.inmemory.specification.SpecificationServices.getService(targetClass);
                Object emptyInstance = createEmptyInstance(service);

                newVersion.setPopulatedEntities(dashboardId, java.util.Collections.singletonList(emptyInstance));

                logger.debug("Created empty instance for dashboard '{}'", dashboardId);

            } catch (Exception e) {
                logger.error("Error creating empty instance for dashboard '{}': {}", dashboardId, e.getMessage(), e);
            }
        }
    }

    /**
     * Creates an empty instance using the provided SpecificationService.
     *
     * @param service the specification service for the entity type
     * @return a new empty instance
     * @throws Exception if instance creation fails
     */
    @SuppressWarnings("unchecked")
    private <T> T createEmptyInstance(SpecificationService<T> service) throws Exception {
        return service.createInstance();
    }

    /**
     * Collects source data for all property mappings.
     *
     * <p>
     * For each source datasource:</p>
     * <ul>
     * <li>Calls ensureDataSourceInDataVersion() to load data if not already
     * present</li>
     * <li>If datasource already loaded, skips re-reading</li>
     * </ul>
     *
     * <p>
     * Source data remains SAF (safe) and shared across all consumers.</p>
     *
     * @param newVersion the new DataVersion to populate
     * @param sourceDatasources set of source datasource names
     */
    private void collectSourceDataForMappings(
            DataVersion newVersion,
            java.util.Set<String> sourceDatasources) {

        logger.debug("Collecting source data for {} datasource(s)...", sourceDatasources.size());

        for (String dataSourceName : sourceDatasources) {
            try {
                // Ensure datasource data is in DataVersion
                // If already present, this will return existing data
                // If not present, this will read from datasource
                ensureDataSourceInDataVersion(newVersion, dataSourceName);

                logger.debug("Source data collected for datasource '{}'", dataSourceName);

            } catch (Exception e) {
                logger.error("Error collecting source data for datasource '{}': {}",
                        dataSourceName, e.getMessage(), e);
                // Continue with other datasources
            }
        }

        logger.debug("Source data collection completed");
    }

    /**
     * Applies specification filter to a list immutably.
     *
     * <p>
     * <b>IMPORTANT:</b> This method does NOT modify the original list. It
     * returns a new filtered list.</p>
     *
     * <p>
     * Implementation:</p>
     * <ul>
     * <li>Uses SpecificationService for filtering (no reflection)</li>
     * <li>Returns new list with filtered entities</li>
     * <li>Entity references are copied (shallow copy of list, not
     * entities)</li>
     * <li>If specification is null, returns original list</li>
     * </ul>
     *
     * @param data the data to filter
     * @param specification the specification to apply (can be null)
     * @param <T> the entity type
     * @return a new filtered list (or original list if specification is null)
     */
    @SuppressWarnings("unchecked")
    private <T> java.util.List<T> applySpecificationImmutable(
            java.util.List<T> data,
            com.thy.fss.common.inmemory.specification.Specification<T> specification) {

        // If no specification, return original list
        if (specification == null) {
            logger.debug("No specification provided, returning original list");
            return data;
        }

        // If data is empty, return empty list
        if (data == null || data.isEmpty()) {
            logger.debug("Data is empty, returning empty list");
            return new java.util.ArrayList<>();
        }

        logger.debug("Applying specification filter to {} items...", data.size());

        // Get entity class from first non-null item
        T firstItem = null;
        for (T item : data) {
            if (item != null) {
                firstItem = item;
                break;
            }
        }

        if (firstItem == null) {
            logger.debug("All items are null, returning empty list");
            return new java.util.ArrayList<>();
        }

        // Create new filtered list
        java.util.List<T> filteredData = new java.util.ArrayList<>();

        // Filter using Specification.test() (no reflection)
        for (T entity : data) {
            if (specification.test(entity)) {
                filteredData.add(entity); // Add reference (shallow copy of list)
            }
        }

        logger.debug("Filtered {} items to {} items", data.size(), filteredData.size());

        return filteredData;
    }

    /**
     * PHASE 3: Produces and maps data by orchestrating optimization, grouping,
     * store mapping, and dashboard mapping.
     *
     * <p>
     * This method orchestrates four main steps:</p>
     * <ol>
     * <li>Optimize data access by building indexes on key mappings</li>
     * <li>Group source data for aggregations (Store only)</li>
     * <li>Apply property mappings for Stores</li>
     * <li>Apply property mappings for Dashboards</li>
     * </ol>
     *
     * @param newVersion the new DataVersion to populate
     * @param allPropertyMappings list of all property mappings
     * @param analysisResult the analysis result
     */
    private void produceAndMapData(
            DataVersion newVersion,
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings,
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analysisResult) {

        logger.debug("DATA PRODUCTION PHASE: Starting produceAndMapData...");

        // IF allPropertyMappings.isEmpty(): return (PropertyMapping yok)
        if (allPropertyMappings == null || allPropertyMappings.isEmpty()) {
            logger.debug("No property mappings found, skipping data production phase");
            return;
        }

        logger.debug("Processing {} property mapping(s)", allPropertyMappings.size());

        // Step 0: Optimize data access by building indexes on key mappings
        // This creates indexes for fast lookup based on primary/foreign key paths
        // Specifications are NOT analyzed - they are used only as filters after index lookup
        logger.debug("Step 0: Optimizing data access with index-based lookups...");
        optimizeDataAccess(newVersion, allPropertyMappings);

        // Step 1: Group source data for aggregations (Store iÃ¯Â¿Â½in gruplama)
        logger.debug("Step 1: Grouping source data for Store aggregations...");
        groupSourceDataForAggregations(newVersion, analysisResult.commonGroupings());

        // Step 2: Apply property mappings for Stores
        logger.debug("Step 2: Applying property mappings for Stores...");
        applyPropertyMappingsForStores(newVersion, allPropertyMappings);

        // Step 3: Apply property mappings for Dashboards
        logger.debug("Step 3: Applying property mappings for Dashboards...");
        applyPropertyMappingsForDashboards(newVersion, allPropertyMappings, analysisResult);

        logger.debug("DATA PRODUCTION PHASE: produceAndMapData completed successfully");
    }

    /**
     * Optimizes data access by building indexes based on key mappings using
     * IndexManager.
     * <p>
     * This method creates indexes for efficient data lookup based on:
     * <ul>
     * <li>Primary key paths from property mappings</li>
     * <li>Foreign key paths from property mappings</li>
     * </ul>
     * <p>
     * Indexes are created using the new NestedTreeMapIndex structure and cached
     * in IndexManager. Specifications are used only as filters after
     * index-based lookup, not for optimization decisions.
     * <p>
     * <b>Key Principle:</b> This method does NOT analyze or compare
     * specifications. It only builds indexes based on key paths defined in
     * property mappings.
     *
     * @param newVersion the new DataVersion to populate with indexes
     * @param allPropertyMappings list of all property mappings containing key
     * paths
     */
    @SuppressWarnings("unchecked")
    private void optimizeDataAccess(
            DataVersion newVersion,
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("OPTIMIZATION: Building indexes using IndexManager...");

        if (allPropertyMappings == null || allPropertyMappings.isEmpty()) {
            logger.debug("No property mappings found, skipping index optimization");
            return;
        }

        // Clear all indexes at the start of new synchronization cycle
        indexManager.clearAllIndexes();

        // Collect unique index keys (datasource + key path combinations)
        java.util.Map<String, IndexKey> indexKeys = new java.util.HashMap<>();

        // Collect unique composite key indexes (datasource + composite key paths)
        java.util.Map<String, CompositeKeyIndexKey> compositeKeyIndexKeys = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            // Skip dashboard mappings (they don't use primary-foreign key relationships)
            if (mapping.isForDashboard()) {
                continue;
            }

            // Skip mappings without key paths
            if (mapping.getPrimaryKeyPaths() == null || mapping.getPrimaryKeyPaths().isEmpty()
                    || mapping.getForeignKeyPaths() == null || mapping.getForeignKeyPaths().isEmpty()) {
                continue;
            }

            // Handle composite key mappings separately
            if (mapping.isCompositeKey()) {
                String dataSourceName = mapping.getDataSourceName();
                java.util.List<java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> foreignKeyPaths
                        = mapping.getForeignKeyPaths();

                // Create unique key for this composite index
                String compositeKeyStr = createCompositeKeyString(dataSourceName, foreignKeyPaths);

                // Store composite key index info
                if (!compositeKeyIndexKeys.containsKey(compositeKeyStr)) {
                    compositeKeyIndexKeys.put(compositeKeyStr, new CompositeKeyIndexKey(dataSourceName, foreignKeyPaths));
                    logger.trace("Identified composite key index: datasource='{}', keyFieldCount={}",
                            dataSourceName, foreignKeyPaths.size());
                }
                continue;
            }

            String dataSourceName = mapping.getDataSourceName();
            // For single-field keys only
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> foreignKeyPath
                    = mapping.getForeignKeyPaths().get(0);

            // Create unique key for this index (datasource + foreign key path)
            String indexKeyStr = createIndexKeyString(dataSourceName, foreignKeyPath);

            // Store index key info
            if (!indexKeys.containsKey(indexKeyStr)) {
                indexKeys.put(indexKeyStr, new IndexKey(dataSourceName, foreignKeyPath, mapping.getSourceService()));
                logger.trace("Identified index key: datasource='{}', path='{}'",
                        dataSourceName, pathToString(foreignKeyPath));
            }
        }

        logger.debug("Found {} unique index key(s) and {} composite key index(es) to build",
                indexKeys.size(), compositeKeyIndexKeys.size());

        // Build indexes in parallel
        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        // Build composite key indexes first
        for (java.util.Map.Entry<String, CompositeKeyIndexKey> entry : compositeKeyIndexKeys.entrySet()) {
            String compositeKeyStr = entry.getKey();
            CompositeKeyIndexKey compositeKey = entry.getValue();

            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // Get source data from dataByDataSource
                    java.util.List<?> sourceData = newVersion.getDataByDataSource(compositeKey.dataSourceName);

                    if (sourceData == null || sourceData.isEmpty()) {
                        logger.debug("No source data found for datasource '{}', skipping composite key index",
                                compositeKey.dataSourceName);
                        return;
                    }

                    // Build composite key index using IndexManager
                    // The index is cached internally by IndexManager, so we don't need to store the return value
                    // It will be retrieved from cache when needed in applyAggregationMappingsOptimized()
                    indexManager.getOrCreateCompositeIndex(
                            compositeKey.dataSourceName,
                            compositeKey.keyPaths,
                            sourceData
                    );

                    logger.debug("Built composite key index for datasource '{}' with {} key fields: {} total items",
                            compositeKey.dataSourceName,
                            compositeKey.keyPaths.size(),
                            sourceData.size());

                } catch (Exception e) {
                    logger.error("Error building composite key index for key '{}': {}", compositeKeyStr, e.getMessage(), e);
                    throw new DataSynchronizationException("Failed to build composite key index for key: " + compositeKeyStr, e);
                }
            }, workerPool);

            futures.add(future);
        }

        // Build single-field indexes
        for (java.util.Map.Entry<String, IndexKey> entry : indexKeys.entrySet()) {
            String indexKeyStr = entry.getKey();
            IndexKey indexKey = entry.getValue();

            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // Get source data from dataByDataSource
                    java.util.List<?> sourceData = newVersion.getDataByDataSource(indexKey.dataSourceName);

                    if (sourceData == null || sourceData.isEmpty()) {
                        logger.debug("No source data found for datasource '{}', skipping index",
                                indexKey.dataSourceName);
                        return;
                    }

                    // Find first non-null entity to determine class type
                    Object firstNonNullEntity = null;
                    for (Object entity : sourceData) {
                        if (entity != null) {
                            firstNonNullEntity = entity;
                            break;
                        }
                    }

                    if (firstNonNullEntity == null) {
                        logger.debug("All entities are null in datasource '{}', skipping index",
                                indexKey.dataSourceName);
                        return;
                    }

                    // Build index using IndexManager with NestedTreeMapIndex
                    @SuppressWarnings("rawtypes")
                    com.thy.fss.common.inmemory.engine.index.IndexDefinition indexDefinition
                            = createIndexDefinition(indexKey.service, indexKey.keyPath);

                    @SuppressWarnings({"rawtypes", "unchecked"})
                    com.thy.fss.common.inmemory.engine.index.NestedTreeMapIndex index
                            = indexManager.getOrCreateIndex(indexKey.dataSourceName, indexDefinition, sourceData);

                    // Convert NestedTreeMapIndex to Map format for backward compatibility
                    @SuppressWarnings("unchecked")
                    java.util.Map<Object, java.util.List<?>> indexMap = convertIndexToMap(index, indexKey.keyPath, (SpecificationService) indexKey.service);

                    // Store index in DataVersion's groupedData
                    newVersion.setGroupedData(indexKeyStr, indexMap);

                    logger.debug("Built index for datasource '{}' on path '{}': {} unique keys, {} total items",
                            indexKey.dataSourceName,
                            pathToString(indexKey.keyPath),
                            indexMap.size(),
                            sourceData.size());

                } catch (Exception e) {
                    logger.error("Error building index for key '{}': {}", indexKeyStr, e.getMessage(), e);
                    throw new DataSynchronizationException("Failed to build index for key: " + indexKeyStr, e);
                }
            }, workerPool);

            futures.add(future);
        }

        // Wait for all index building operations to complete
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(60, java.util.concurrent.TimeUnit.SECONDS);
            logger.debug("OPTIMIZATION: All indexes built successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for index building operations to complete", e);
            throw new DataSynchronizationException("Index building interrupted", e);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Error waiting for index building operations to complete", e);
            throw new DataSynchronizationException("Failed to complete index building operations", e);
        }
    }

    /**
     * Creates a unique string key for an index based on datasource name and key
     * path.
     *
     * @param dataSourceName the datasource name
     * @param keyPath the key path (list of MetaAttribute)
     * @return unique string key for the index
     */
    private String createIndexKeyString(
            String dataSourceName,
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath) {

        StringBuilder sb = new StringBuilder();
        sb.append(dataSourceName).append(":");

        for (com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> attr : keyPath) {
            sb.append(attr.getName()).append(".");
        }

        // Remove trailing dot
        if (sb.charAt(sb.length() - 1) == '.') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Creates a unique string key for a composite key index based on datasource name and key paths.
     *
     * @param dataSourceName the datasource name
     * @param keyPaths the list of key paths (each path is a list of MetaAttribute)
     * @return unique string key for the composite index
     */
    private String createCompositeKeyString(
            String dataSourceName,
            java.util.List<java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths) {

        StringBuilder sb = new StringBuilder();
        sb.append(dataSourceName).append(":composite:");

        for (java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath : keyPaths) {
            for (com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> attr : keyPath) {
                sb.append(attr.getName()).append(".");
            }
            // Remove trailing dot and add separator
            if (sb.charAt(sb.length() - 1) == '.') {
                sb.setLength(sb.length() - 1);
            }
            sb.append("+");
        }

        // Remove trailing plus
        if (sb.charAt(sb.length() - 1) == '+') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Converts a key path to a readable string for logging.
     *
     * @param keyPath the key path (list of MetaAttribute)
     * @return readable string representation
     */
    private String pathToString(
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath) {

        if (keyPath == null || keyPath.isEmpty()) {
            return "<empty>";
        }

        return keyPath.stream()
                .map(com.thy.fss.common.inmemory.specification.attribute.MetaAttribute::getName)
                .collect(java.util.stream.Collectors.joining("."));
    }

    /**
     * Creates an IndexDefinition from service and key path.
     *
     * @param service the specification service for the entity type
     * @param keyPath the key path (list of MetaAttribute)
     * @return IndexDefinition for the given path
     */
    @SuppressWarnings("unchecked")
    private <T> com.thy.fss.common.inmemory.engine.index.IndexDefinition<T> createIndexDefinition(
            SpecificationService<T> service,
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath) {

        com.thy.fss.common.inmemory.engine.index.IndexDefinition.Builder<T> builder
                = com.thy.fss.common.inmemory.engine.index.IndexDefinition.builder((Class<T>) service.getEntityClass());

        // For nested paths, we need to use getValueByPath instead of getFieldValue
        // Only the final value in the path should be used as the index key
        if (keyPath.size() == 1) {
            // Simple case: single field
            builder.addKeyField((com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<T, ? extends Comparable>) keyPath.get(0));
        } else {
            // Nested path: create a custom key extractor that navigates the path
            // Get the last attribute to determine the field type
            com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> lastAttr = keyPath.get(keyPath.size() - 1);

            // Create a custom key extractor using getValueByPath with the provided service
            builder.addKeyFieldWithPath(keyPath, entity -> service.getValueByPath((T) entity, keyPath));
        }

        return builder.build();
    }

    /**
     * Converts NestedTreeMapIndex to Map format for backward compatibility.
     * Extracts all values from the index and groups them by the first key
     * level.
     *
     * @param index the NestedTreeMapIndex
     * @param keyPath the key path used for indexing
     * @param service the specification service for the entity type
     * @return Map of key to list of entities
     */
   
    private <T> java.util.Map<Object, java.util.List<?>> convertIndexToMap(
            com.thy.fss.common.inmemory.engine.index.NestedTreeMapIndex<T> index,
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath,
            SpecificationService<T> service) {

        java.util.Map<Object, java.util.List<Object>> result = new java.util.HashMap<>();

        if (keyPath.size() == 1) {
            // Single-level index: direct conversion
            java.util.List<T> allValues = index.getAllValues();

            for (T entity : allValues) {
                Object keyValue = service.getValueByPath(entity, keyPath);
                if (keyValue != null) {
                    result.computeIfAbsent(keyValue, k -> new java.util.ArrayList<>()).add(entity);
                }
            }
        } else {
            // Multi-level index: use partial lookup to get all first-level keys
            java.util.SortedMap<Comparable<?>, Object> rootMap = index.partialLookup();

            for (java.util.Map.Entry<Comparable<?>, Object> entry : rootMap.entrySet()) {
                Object key = entry.getKey();
                java.util.List<T> values = index.prefixSearch(key);

                // Group by the full key path
                for (T entity : values) {
                    Object fullKeyValue = service.getValueByPath(entity, keyPath);
                    if (fullKeyValue != null) {
                        result.computeIfAbsent(fullKeyValue, k -> new java.util.ArrayList<>()).add(entity);
                    }
                }
            }
        }

        return (java.util.Map<Object, java.util.List<?>>) (java.util.Map<?, ?>) result;
    }

    /**
     * Helper class to store index key information.
     */
    private static class IndexKey {

        final String dataSourceName;
        final java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath;
        final SpecificationService<?> service;

        IndexKey(String dataSourceName,
                 java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> keyPath,
                 SpecificationService<?> service) {
            this.dataSourceName = dataSourceName;
            this.keyPath = keyPath;
            this.service = service;
        }
    }

    /**
     * Helper class to store composite key index information.
     */
    private static class CompositeKeyIndexKey {

        final String dataSourceName;
        final java.util.List<java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths;

        CompositeKeyIndexKey(String dataSourceName,
                             java.util.List<java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths) {
            this.dataSourceName = dataSourceName;
            this.keyPaths = keyPaths;
        }
    }

    /**
     * Groups source data for aggregations (Store only).
     * <p>
     * For each common grouping key:
     * <ul>
     * <li>Gets source data from dataByDataSource</li>
     * <li>Stores in newVersion.groupedData[groupingKey]</li>
     * </ul>
     * <p>
     * Parallel grouping is performed using workerPool for better performance.
     *
     * @param newVersion the new DataVersion to populate
     * @param commonGroupings map of grouping key to property mappings
     */
    /**
     * Groups source data for aggregations using cached indexes from
     * IndexManager.
     * <p>
     * This method checks if indexes were already created in the optimization
     * phase. If an index exists, it reuses it. Otherwise, it creates a new
     * grouping. This ensures single-pass indexing as per requirement 3.2, 3.3,
     * 3.4.
     *
     * @param newVersion the new DataVersion
     * @param commonGroupings map of grouping key to property mappings
     */
    private void groupSourceDataForAggregations(
            DataVersion newVersion,
            java.util.Map<com.thy.fss.common.inmemory.engine.analysis.GroupingKey, java.util.List<PropertyMapping<?, ?>>> commonGroupings) {

        logger.debug("Grouping source data for {} common grouping(s) using cached indexes...", commonGroupings.size());

        if (commonGroupings.isEmpty()) {
            logger.debug("No common groupings found, skipping grouping phase");
            return;
        }

        // Process each grouping
        // Note: Composite key mappings are not included in commonGroupings
        // They use CompositeKeyIndex which is created on-demand during aggregation
        for (java.util.Map.Entry<com.thy.fss.common.inmemory.engine.analysis.GroupingKey, java.util.List<PropertyMapping<?, ?>>> entry : commonGroupings.entrySet()) {
            com.thy.fss.common.inmemory.engine.analysis.GroupingKey groupingKey = entry.getKey();

            try {
                String dataSourceName = groupingKey.dataSourceName();
                java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> foreignKeyPath = groupingKey.foreignKeyPath();
                String indexKey = createIndexKeyString(dataSourceName, foreignKeyPath);

                // Check if index already exists in groupedData (created during optimization phase)
                @SuppressWarnings("unchecked")
                java.util.Map<Object, java.util.List<?>> existingIndex
                        = (java.util.Map<Object, java.util.List<?>>) (java.util.Map<?, ?>) newVersion.getGroupedData(indexKey);

                if (existingIndex != null) {
                    // Index already exists from optimization phase - reuse it
                    logger.debug("GROUPING: Reusing cached index for key '{}' (single-pass indexing)", indexKey);
                    logger.debug("GROUPING: Cached index has {} groups", existingIndex.size());
                    continue;
                }

                // Index doesn't exist - create it now
                // This can happen if the grouping wasn't identified during optimization
                logger.debug("GROUPING: Creating new index for key '{}' (not found in cache)", indexKey);

                java.util.List<?> sourceData = newVersion.getDataByDataSource(dataSourceName);

                if (sourceData == null || sourceData.isEmpty()) {
                    logger.debug("No source data found for datasource '{}', skipping grouping", dataSourceName);
                    continue;
                }

                // Group by foreign key path
                // Get service from the first mapping in this grouping
                PropertyMapping<?, ?> firstMapping = entry.getValue().get(0);
                java.util.Map<Object, java.util.List<?>> grouped = groupByForeignKeyPath(
                        sourceData, 
                        foreignKeyPath,
                        firstMapping.getSourceService()
                );

                // Store in newVersion.groupedData
                newVersion.setGroupedData(indexKey, grouped);

                logger.debug("GROUPING: Grouped {} items into {} groups for key '{}'",
                        sourceData.size(), grouped.size(), indexKey);
                logger.debug("GROUPING: Index key details - datasource: {}, foreignKeyPath size: {}",
                        groupingKey.dataSourceName(), groupingKey.foreignKeyPath().size());

            } catch (Exception e) {
                logger.error("Error grouping data for key '{}': {}", groupingKey, e.getMessage(), e);
                throw new DataSynchronizationException("Failed to group data for key: " + groupingKey, e);
            }
        }

        logger.debug("All grouping operations completed successfully (using cached indexes where available)");
    }

    /**
     * Applies property mappings for all Stores.
     * <p>
     * For each store:
     * <ul>
     * <li>Gets root entities from populatedEntities</li>
     * <li>Filters Store PropertyMappings</li>
     * <li>Applies mappings to each root entity using
     * {@link #applyMappingsToEntity}</li>
     * </ul>
     *
     * @param newVersion the new DataVersion to populate
     * @param allPropertyMappings list of all property mappings
     */
    @SuppressWarnings("unchecked")
    private void applyPropertyMappingsForStores(
            DataVersion newVersion,
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings) {

        logger.debug("Applying property mappings for Stores...");

        // Group mappings by consumer ID (storeId)
        java.util.Map<String, java.util.List<PropertyMapping<?, ?>>> mappingsByStore = new java.util.HashMap<>();

        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            // Skip Dashboard mappings
            if (mapping.isForDashboard()) {
                continue;
            }

            String storeId = mapping.getConsumerId();
            mappingsByStore.computeIfAbsent(storeId, k -> new java.util.ArrayList<>()).add(mapping);
        }

        logger.debug("Found {} store(s) with property mappings", mappingsByStore.size());

        // DataVersion-backed RelatedEntityLookup Ã¢â‚¬â€ uses indexed grouped data for FK lookup
        RelatedEntityLookup dataVersionLookup = (mapping, pkValues) -> {
            if (mapping.isCompositeKey()) {
                java.util.List<?> sourceData = newVersion.getDataByDataSource(mapping.getDataSourceName());
                if (sourceData == null || sourceData.isEmpty()) return java.util.Collections.emptyList();
                CompositeKeyIndex<?> compositeIndex = indexManager.getOrCreateCompositeIndex(
                        mapping.getDataSourceName(), mapping.getForeignKeyPaths(), (java.util.Collection) sourceData);
                java.util.List<?> result = compositeIndex.lookup(pkValues);
                return result != null ? result : java.util.Collections.emptyList();
            } else {
                String indexKey = createIndexKeyString(mapping.getDataSourceName(), mapping.getForeignKeyPaths().get(0));
                java.util.Map<Object, java.util.List<?>> grouped = (java.util.Map<Object, java.util.List<?>>) (java.util.Map<?, ?>) newVersion.getGroupedData(indexKey);
                if (grouped == null) return java.util.Collections.emptyList();
                java.util.List<?> result = grouped.get(pkValues.get(0));
                return result != null ? result : java.util.Collections.emptyList();
            }
        };

        // Process each store
        for (java.util.Map.Entry<String, java.util.List<PropertyMapping<?, ?>>> entry : mappingsByStore.entrySet()) {
            String storeId = entry.getKey();
            java.util.List<PropertyMapping<?, ?>> storeMappings = entry.getValue();

            try {
                // Get root entities from populatedEntities
                java.util.List<?> rootEntities = newVersion.getPopulatedEntities(storeId);

                if (rootEntities == null || rootEntities.isEmpty()) {
                    logger.debug("No root entities found for store '{}', skipping mappings", storeId);
                    continue;
                }

                logger.debug("Applying {} mapping(s) to {} root entities for store '{}'",
                        storeMappings.size(), rootEntities.size(), storeId);

                // Apply mappings to each root entity via MappingApplicator
                for (Object rootEntity : rootEntities) {
                    // Skip null entities
                    if (rootEntity == null) {
                        logger.trace("Skipping null root entity in store '{}'", storeId);
                        continue;
                    }
                    MappingApplicator.applyMappingsToEntity(dataVersionLookup, rootEntity, storeMappings);
                }

                logger.debug("Successfully applied mappings for store '{}'", storeId);

            } catch (Exception e) {
                logger.error("Error applying mappings for store '{}': {}", storeId, e.getMessage(), e);
                // Continue with other stores
            }
        }

        logger.debug("Store property mappings application completed");
    }

    /**
     * Applies property mappings for all Dashboards.
     * <p>
     * This method will be implemented in task 11.
     *
     * @param newVersion the new DataVersion
     * @param allPropertyMappings list of all property mappings
     * @param analysisResult the analysis result
     */
    private void applyPropertyMappingsForDashboards(
            DataVersion newVersion,
            java.util.List<PropertyMapping<?, ?>> allPropertyMappings,
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analysisResult) {

        logger.debug("Applying property mappings for Dashboards...");

        // Filter dashboard mappings
        java.util.List<PropertyMapping<?, ?>> dashboardMappings = new java.util.ArrayList<>();
        for (PropertyMapping<?, ?> mapping : allPropertyMappings) {
            if (mapping.isForDashboard()) {
                dashboardMappings.add(mapping);
            }
        }

        if (dashboardMappings.isEmpty()) {
            logger.debug("No dashboard mappings found, skipping dashboard aggregation phase");
            return;
        }

        logger.debug("Found {} dashboard mapping(s)", dashboardMappings.size());

        // PHASE 1: Calculate common aggregations
        logger.debug("PHASE 1: Calculating common aggregations...");
        calculateCommonAggregations(newVersion, analysisResult);

        // PHASE 2: Apply dashboard aggregation plans
        logger.debug("PHASE 2: Applying dashboard aggregation plans...");
        for (java.util.Map.Entry<String, com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan> entry
                : analysisResult.dashboardAggregationPlans().entrySet()) {
            String dashboardId = entry.getKey();
            com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan plan = entry.getValue();

            try {
                applyDashboardAggregationPlan(newVersion, dashboardId, plan);
            } catch (Exception e) {
                logger.error("Error applying aggregation plan for dashboard '{}': {}",
                        dashboardId, e.getMessage(), e);
                // Continue with other dashboards
            }
        }

        logger.debug("Dashboard property mappings application completed");
    }

    /**
     * Calculates common aggregations across all dashboards.
     * <p>
     * This method:
     * <ol>
     * <li>Collects all unique aggregation tasks from all dashboard plans</li>
     * <li>Merges tasks with the same (datasource + specification + field)</li>
     * <li>Calculates all aggregations in a single loop for each unique
     * task</li>
     * <li>Stores results in newVersion.commonAggregationResults</li>
     * </ol>
     * <p>
     * TEK LOOP guarantee: Same (datasource + spec + field) with multiple
     * aggregation types are calculated in one pass.
     *
     * @param newVersion the new DataVersion
     * @param analysisResult the analysis result containing dashboard
     * aggregation plans
     */
    private void calculateCommonAggregations(
            DataVersion newVersion,
            com.thy.fss.common.inmemory.engine.analysis.AnalysisResult analysisResult) {

        logger.debug("Calculating common aggregations...");

        // Step 1: Collect all unique tasks (DataSourceSpecFieldKey based)
        java.util.Map<String, com.thy.fss.common.inmemory.engine.analysis.AggregationTask> uniqueTasks = new java.util.HashMap<>();

        for (com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan plan
                : analysisResult.dashboardAggregationPlans().values()) {
            for (com.thy.fss.common.inmemory.engine.analysis.AggregationTask task : plan.getTasks()) {
                // Create unique key for this task (datasource + specification + field path)
                String dataSourceName = task.getDataSourceName();
                java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> fieldPath = task.getFieldPath();
                com.thy.fss.common.inmemory.specification.Specification<?> specification = task.getSpecification();

                String fieldPart = fieldPath != null
                        ? fieldPath.stream()
                        .map(obj -> String.valueOf(System.identityHashCode(obj)))
                        .collect(java.util.stream.Collectors.joining("."))
                        : "null";
                String specPart = specification != null ? String.valueOf(System.identityHashCode(specification)) : "null";
                String taskKey = String.format("%s:%s:spec=%s", dataSourceName, fieldPart, specPart);

                // Merge tasks with same key
                if (uniqueTasks.containsKey(taskKey)) {
                    com.thy.fss.common.inmemory.engine.analysis.AggregationTask existingTask = uniqueTasks.get(taskKey);
                    com.thy.fss.common.inmemory.engine.analysis.AggregationTask mergedTask
                            = mergeAggregationTasks(existingTask, task);
                    uniqueTasks.put(taskKey, mergedTask);
                } else {
                    uniqueTasks.put(taskKey, task);
                }
            }
        }

        logger.debug("Found {} unique aggregation task(s) after merging", uniqueTasks.size());

        // Step 2: Calculate aggregations for each unique task
        for (java.util.Map.Entry<String, com.thy.fss.common.inmemory.engine.analysis.AggregationTask> entry
                : uniqueTasks.entrySet()) {
            String taskKey = entry.getKey();
            com.thy.fss.common.inmemory.engine.analysis.AggregationTask task = entry.getValue();

            try {
                logger.trace("Calculating aggregations for task: {}", taskKey);

                // Get source data from dataByDataSource
                String dataSourceName = task.getDataSourceName();
                java.util.List<?> sourceData = newVersion.getDataByDataSource(dataSourceName);

                if (sourceData == null || sourceData.isEmpty()) {
                    logger.debug("No source data found for datasource '{}', skipping task", dataSourceName);
                    continue;
                }

                // Specification filter is carried by the task Ã¢â‚¬â€ applied during aggregation computation
                // Calculate multiple aggregations in single loop
                java.util.Map<AggregationType, Object> results = calculateMultipleAggregationsInSingleLoop(
                        sourceData,
                        task.getFieldPath(),
                        task.getAggregationTypes()
                );

                // Store results in commonAggregationResults (each aggregationType gets its own key)
                for (java.util.Map.Entry<AggregationType, Object> resultEntry : results.entrySet()) {
                    AggregationType aggregationType = resultEntry.getKey();
                    Object result = resultEntry.getValue();

                    // Create CommonAggregationKey and store result (no specification)
                    com.thy.fss.common.inmemory.engine.analysis.CommonAggregationKey aggKey
                            = new com.thy.fss.common.inmemory.engine.analysis.CommonAggregationKey(
                            dataSourceName,
                            null,
                            task.getFieldPath(),
                            aggregationType
                    );

                    String storageKey = aggKey.toStorageKey();
                    newVersion.setCommonAggregationResult(storageKey, result);

                    logger.trace("Stored aggregation result: {} = {}", storageKey, result);
                }

                logger.trace("Calculated {} aggregation(s) for task '{}'",
                        results.size(), taskKey);

            } catch (Exception e) {
                logger.error("Error calculating aggregations for task '{}': {}",
                        taskKey, e.getMessage(), e);
                // Continue with other tasks
            }
        }

        logger.debug("Common aggregations calculation completed");
    }

    /**
     * Merges two aggregation tasks with the same (datasource + specification + field path).
     * Combines all aggregation types from both tasks into a single task.
     *
     * @param task1 the first task
     * @param task2 the second task
     * @return merged task containing all aggregation types from both tasks
     */
    private com.thy.fss.common.inmemory.engine.analysis.AggregationTask mergeAggregationTasks(
            com.thy.fss.common.inmemory.engine.analysis.AggregationTask task1,
            com.thy.fss.common.inmemory.engine.analysis.AggregationTask task2) {

        logger.trace("Merging aggregation tasks");

        // Create new task with same datasource, field path, and specification
        com.thy.fss.common.inmemory.engine.analysis.AggregationTask mergedTask
                = new com.thy.fss.common.inmemory.engine.analysis.AggregationTask(
                task1.getDataSourceName(),
                task1.getFieldPath(),
                task1.getSpecification()
        );

        // Add all mappings from task1
        for (java.util.Map.Entry<AggregationType, java.util.List<PropertyMapping<?, ?>>> entry
                : task1.getMappingsByAggregationType().entrySet()) {
            AggregationType aggType = entry.getKey();
            for (PropertyMapping<?, ?> mapping : entry.getValue()) {
                mergedTask.addMapping(aggType, mapping);
            }
        }

        // Add all mappings from task2
        for (java.util.Map.Entry<AggregationType, java.util.List<PropertyMapping<?, ?>>> entry
                : task2.getMappingsByAggregationType().entrySet()) {
            AggregationType aggType = entry.getKey();
            for (PropertyMapping<?, ?> mapping : entry.getValue()) {
                mergedTask.addMapping(aggType, mapping);
            }
        }

        logger.trace("Merged task now has {} aggregation type(s)",
                mergedTask.getAggregationTypeCount());

        return mergedTask;
    }

    /**
     * Calculates multiple aggregations in a single loop over the data. Supports
     * path-based field access including nested paths and collection segments.
     *
     * <p>
     * This method maintains the original optimization: all aggregation types
     * are computed in a SINGLE loop over the data, avoiding multiple
     * iterations.</p>
     *
     * @param sourceData the source data to aggregate
     * @param fieldPath the field path to aggregate (List of MetaAttribute)
     * @param aggregationTypes set of aggregation types to calculate
     * @return map of aggregation type to result
     */
  
    private java.util.Map<AggregationType, Object> calculateMultipleAggregationsInSingleLoop(
            java.util.List<?> sourceData,
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> fieldPath,
            java.util.Set<AggregationType> aggregationTypes) {

        logger.trace("Calculating {} aggregation type(s) for {} entities",
                aggregationTypes.size(), sourceData != null ? sourceData.size() : 0);

        java.util.Map<AggregationType, Object> results = new java.util.HashMap<>();

        if (sourceData == null || sourceData.isEmpty()) {
            // Return default values for empty data
            for (AggregationType type : aggregationTypes) {
                results.put(type, getDefaultAggregationValue(type));
            }
            return results;
        }

        // Initialize accumulators for all aggregation types
        java.util.Map<AggregationType, AggregationAccumulator> accumulators = new java.util.HashMap<>();
        for (AggregationType type : aggregationTypes) {
            accumulators.put(type, new AggregationAccumulator(type));
        }

        // SINGLE LOOP: Process all entities once and update all accumulators
        for (Object entity : sourceData) {
            // Skip null entities
            if (entity == null) {
                continue;
            }

            try {
                // Extract value using SpecificationService (supports nested paths and collections)
                Object value;
                if (fieldPath != null && !fieldPath.isEmpty()) {
                    // This is used for Dashboard aggregations where we don't have PropertyMapping context
                    SpecificationService service = SpecificationServices.getService(entity.getClass());
                    value = service.getValueByPath(entity, fieldPath);
                } else {
                    value = entity;
                }

                // Update ALL accumulators in this single iteration
                for (AggregationAccumulator accumulator : accumulators.values()) {
                    accumulator.accumulate(value);
                }
            } catch (Exception e) {
                logger.warn("Error processing entity for aggregation: {}", e.getMessage());
                // Continue with other entities
            }
        }

        // Get final results from all accumulators
        for (java.util.Map.Entry<AggregationType, AggregationAccumulator> entry : accumulators.entrySet()) {
            results.put(entry.getKey(), entry.getValue().getResult());
        }

        logger.trace("Calculated aggregations: {}", results);
        return results;
    }

    /**
     * Gets the default value for an aggregation type when there's no data.
     *
     * @param type the aggregation type
     * @return the default value
     */
    private Object getDefaultAggregationValue(AggregationType type) {
        switch (type) {
            case COUNT:
                return 0L;
            case SUM:
                return 0.0;
            case AVG:
                return 0.0;
            case MIN:
                return null;
            case MAX:
                return null;
            default:
                return null;
        }
    }

    /**
     * Applies a dashboard aggregation plan to a specific dashboard.
     * <p>
     * For each task in the plan:
     * <ol>
     * <li>Retrieves pre-computed aggregation results from
     * commonAggregationResults</li>
     * <li>Writes results to the corresponding dashboard field via property
     * mappings</li>
     * </ol>
     *
     * @param newVersion the new DataVersion
     * @param dashboardId the dashboard ID
     * @param plan the dashboard aggregation plan
     */
    private void applyDashboardAggregationPlan(
            DataVersion newVersion,
            String dashboardId,
            com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan plan) {

        logger.debug("Applying aggregation plan for dashboard '{}'", dashboardId);

        // Get dashboard instance from populatedEntities
        // Dashboard contains a single aggregation result (like a single row from SELECT COUNT(*), SUM(...))
        // Unlike Stores which contain multiple rows, Dashboard has only one instance with aggregated values
        java.util.List<?> dashboardList = newVersion.getPopulatedEntities(dashboardId);

        if (dashboardList == null || dashboardList.isEmpty()) {
            logger.warn("No dashboard instance found for dashboard '{}', skipping", dashboardId);
            return;
        }

        // Dashboard should contain exactly one instance (single aggregation result row)
        if (dashboardList.size() != 1) {
            logger.warn("Dashboard '{}' has {} instances, expected exactly 1 (aggregation result should be single row)",
                    dashboardId, dashboardList.size());
            // Continue with first instance even if there are multiple (defensive programming)
        }

        Object dashboardInstance = dashboardList.getFirst();

        // Process each task in the plan
        for (com.thy.fss.common.inmemory.engine.analysis.AggregationTask task : plan.getTasks()) {
            try {
                // For each aggregation type in this task
                for (AggregationType aggregationType : task.getAggregationTypes()) {
                    // Create CommonAggregationKey to retrieve result (no specification)
                    com.thy.fss.common.inmemory.engine.analysis.CommonAggregationKey aggKey
                            = new com.thy.fss.common.inmemory.engine.analysis.CommonAggregationKey(
                            task.getDataSourceName(),
                            null,
                            task.getFieldPath(),
                            aggregationType
                    );

                    String storageKey = aggKey.toStorageKey();
                    Object result = newVersion.getCommonAggregationResult(storageKey);

                    if (result == null) {
                        logger.trace("No result found for aggregation key '{}'", storageKey);
                        continue;
                    }

                    // Get all mappings for this aggregation type
                    java.util.List<PropertyMapping<?, ?>> mappings = task.getMappings(aggregationType);

                    // Write result to all target fields
                    for (PropertyMapping<?, ?> mapping : mappings) {
                        applyAggregationResultToField(dashboardInstance, dashboardId, mapping, result, storageKey, aggregationType);
                    }
                }

            } catch (Exception e) {
                logger.error("Error processing task for dashboard '{}': {}",
                        dashboardId, e.getMessage(), e);
                // Continue with other tasks
            }
        }

        logger.debug("Successfully applied aggregation plan for dashboard '{}'", dashboardId);
    }

    private void applyAggregationResultToField(
            Object dashboardInstance, String dashboardId,
            PropertyMapping<?, ?> mapping, Object result,
            String storageKey, AggregationType aggregationType) {
        try {
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
            com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> targetAttr
                    = targetPath != null && !targetPath.isEmpty() ? targetPath.get(targetPath.size() - 1) : null;

            Object convertedResult = convertAggregationResultToTargetType(
                    result,
                    targetAttr.getFieldType(),
                    aggregationType
            );

            MappingApplicator.assignTargetValue(dashboardInstance, mapping, convertedResult);

            logger.trace("Set dashboard field '{}' = {} for aggregation '{}'",
                    targetAttr.getName(), convertedResult, storageKey);

        } catch (Exception e) {
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
            com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> targetAttr
                    = targetPath != null && !targetPath.isEmpty() ? targetPath.get(targetPath.size() - 1) : null;
            logger.warn("Error setting field '{}' on dashboard '{}': {}",
                    targetAttr != null ? targetAttr.getName() : "unknown", dashboardId, e.getMessage());
        }
    }

    /**
     * Converts aggregation result to match the target field type.
     * <p>
     * This method handles type conversions for aggregation results,
     * particularly:
     * <ul>
     * <li>COUNT returns long, but target field might be Integer</li>
     * <li>SUM/AVG might return Double, but target field might be Float or
     * Integer</li>
     * </ul>
     *
     * @param result the aggregation result
     * @param targetType the target field type
     * @param aggregationType the aggregation type
     * @return the converted result
     */
    private Object convertAggregationResultToTargetType(
            Object result,
            Class<?> targetType,
            AggregationType aggregationType) {

        if (result == null) {
            return null;
        }

        // If types already match, no conversion needed
        if (targetType.isAssignableFrom(result.getClass())) {
            return result;
        }

        // Handle numeric conversions
        if (result instanceof Number numResult) {

            // Convert to Integer
            if (targetType == Integer.class || targetType == int.class) {
                return numResult.intValue();
            } // Convert to Long
            else if (targetType == Long.class || targetType == long.class) {
                return numResult.longValue();
            } // Convert to Double
            else if (targetType == Double.class || targetType == double.class) {
                return numResult.doubleValue();
            } // Convert to Float
            else if (targetType == Float.class || targetType == float.class) {
                return numResult.floatValue();
            } // Convert to Short
            else if (targetType == Short.class || targetType == short.class) {
                return numResult.shortValue();
            } // Convert to Byte
            else if (targetType == Byte.class || targetType == byte.class) {
                return numResult.byteValue();
            }
        }

        // If no conversion possible, return original result
        logger.warn("Could not convert aggregation result of type {} to target type {} for aggregation {}",
                result.getClass().getSimpleName(), targetType.getSimpleName(), aggregationType);
        return result;
    }

    /**
     * Atomically swaps the current DataVersion with the new version.
     * <p>
     * This method performs an atomic swap using AtomicReference.set(), making
     * the new DataVersion immediately visible to all consumers. The old version
     * becomes eligible for garbage collection.
     * <p>
     * Thread Safety: This operation is atomic and thread-safe. All subsequent
     * reads will see the new version.
     *
     * @param newVersion the new DataVersion to activate
     */
    private void swapDataVersion(DataVersion newVersion) {
        if (newVersion == null) {
            throw new IllegalArgumentException("New DataVersion cannot be null");
        }

        DataVersion oldVersion = currentDataVersion.get();
        currentDataVersion.set(newVersion);

        logger.debug("Swapped DataVersion: {} -> {} (atomic operation completed)",
                oldVersion.getVersion(), newVersion.getVersion());
        logger.debug("Old version {} is now eligible for garbage collection", oldVersion.getVersion());
    }

    // ==================== TASK 12: DataVersion Swap and Push ====================
    /**
     * Pushes populated data to all consumers (stores and dashboards).
     * <p>
     * For each consumer:
     * <ol>
     * <li>Retrieves populated entities from newVersion.populatedEntities</li>
     * <li>Calls consumer.updateData() to push the data</li>
     * </ol>
     * <p>
     * This method updates all stores and dashboards with their respective data
     * from the new DataVersion.
     *
     * @param newVersion the new DataVersion containing populated data
     * @param allConsumerIds list of all consumer IDs (stores + dashboards)
     */
    private void pushDataToAllConsumers(DataVersion newVersion, java.util.List<String> allConsumerIds) {
        if (newVersion == null) {
            throw new IllegalArgumentException("DataVersion cannot be null");
        }
        if (allConsumerIds == null || allConsumerIds.isEmpty()) {
            logger.debug("No consumers to push data to");
            return;
        }

        logger.debug("Pushing data to {} consumer(s)...", allConsumerIds.size());

        int successCount = 0;
        int failureCount = 0;

        for (String consumerId : allConsumerIds) {
            try {
                if (pushDataToConsumer(consumerId, newVersion)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                logger.error("Error pushing data to consumer '{}': {}", consumerId, e.getMessage(), e);
                failureCount++;
            }
        }

        logger.debug("Data push completed: {} successful, {} failed", successCount, failureCount);
    }

    private boolean pushDataToConsumer(String consumerId, DataVersion newVersion) {
        java.util.List<?> populatedEntities = newVersion.getPopulatedEntities(consumerId);

        if (populatedEntities == null) {
            logger.warn("No populated entities found for consumer '{}', skipping", consumerId);
            return false;
        }

        InMemoryDataStore<?> store = factory.getStoreById(consumerId);
        if (store != null) {
            pushDataToStore(store, populatedEntities, newVersion.getVersion());
            return true;
        }

        Dashboard<?> dashboard = factory.getDashboardById(consumerId);
        if (dashboard != null) {
            pushDataToDashboard(dashboard, populatedEntities);
            return true;
        }

        logger.warn("Consumer '{}' not found in registry, skipping", consumerId);
        return false;
    }

    /**
     * Pushes data to a specific Store.
     * <p>
     * Calls store.updateData() with the populated entities and version number.
     * This method is thread-safe as InMemoryDataStore.updateData() uses
     * volatile write for visibility.
     *
     * @param store the store to update
     * @param populatedEntities the populated entities for this store
     * @param version the DataVersion number
     */
    @SuppressWarnings("unchecked")
    private void pushDataToStore(InMemoryDataStore<?> store, java.util.List<?> populatedEntities, long version) {
        try {
            // Cast is safe because we know the store's type matches the populated entities
            ((InMemoryDataStore<Object>) store).updateData((java.util.List<Object>) populatedEntities, version);

            logger.debug("Pushed {} entities to Store '{}' (version: {})",
                    populatedEntities.size(),
                    store.getTargetClass().getSimpleName(),
                    version);

        } catch (Exception e) {
            logger.error("Error pushing data to Store '{}': {}",
                    store.getTargetClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Pushes data to a specific Dashboard.
     * <p>
     * Calls dashboard.updateData() with the populated dashboard instance.
     * Dashboard should contain exactly one instance (single aggregation
     * result). This method is thread-safe as Dashboard.updateData() uses
     * volatile write for visibility.
     *
     * @param dashboard the dashboard to update
     * @param populatedEntities the populated entities (should be single
     * instance for dashboard)
     */
    @SuppressWarnings("unchecked")
    private void pushDataToDashboard(Dashboard<?> dashboard, java.util.List<?> populatedEntities) {
        try {
            if (populatedEntities.isEmpty()) {
                logger.warn("Dashboard '{}' has no populated data, skipping", dashboard.getId());
                return;
            }

            if (populatedEntities.size() != 1) {
                logger.warn("Dashboard '{}' has {} instances, expected exactly 1 (using first instance)",
                        dashboard.getId(), populatedEntities.size());
            }

            // Dashboard contains single aggregation result instance
            Object dashboardInstance = populatedEntities.get(0);

            if (dashboardInstance == null) {
                logger.warn("Dashboard '{}' instance is null, skipping", dashboard.getId());
                return;
            }

            // Cast is safe because we know the dashboard's type matches the populated instance
            ((Dashboard<Object>) dashboard).updateData(dashboardInstance);

            logger.debug("Pushed data to Dashboard '{}' (ID: {})",
                    dashboard.getName(),
                    dashboard.getId());

        } catch (Exception e) {
            logger.error("Error pushing data to Dashboard '{}': {}",
                    dashboard.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Groups entities by foreign key path value using SpecificationService.
     * Handles nested paths and collection segments.
     *
     * @param entities the entities to group
     * @param foreignKeyPath the foreign key path (List of MetaAttribute)
     * @param service the specification service for the entity type
     * @return map of foreign key value to list of entities
     */
    @SuppressWarnings({"unchecked", "java:S1854"})
    private java.util.Map<Object, java.util.List<?>> groupByForeignKeyPath(
            java.util.List<?> entities,
            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> foreignKeyPath,
            SpecificationService<?> service) {
        if (entities == null || entities.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        java.util.Map<Object, java.util.List<Object>> result = new java.util.HashMap<>();

        for (Object entity : entities) {
            // Skip null entities
            if (entity == null) {
                continue;
            }

            try {
                // Cast to suppress wildcard type issues
                @SuppressWarnings("unchecked")
                SpecificationService<Object> typedService = (SpecificationService<Object>) service;
                Object keyValue = typedService.getValueByPath(entity, foreignKeyPath);

                if (keyValue != null) {
                    result.computeIfAbsent(keyValue, k -> new java.util.ArrayList<>()).add(entity);
                }
            } catch (Exception e) {
                logger.warn("Error extracting foreign key value from entity {}: {}", entity, e.getMessage());
            }
        }

        return (java.util.Map<Object, java.util.List<?>>) (java.util.Map<?, ?>) result;
    }

    // ==================== Path-Based Helper Methods ====================
    /**
     * Logs a concise summary of the synchronization cycle with timing
     * breakdown.
     *
     * @param newVersion the new DataVersion that was created
     * @param syncedDataSources the datasources that were synchronized
     * @param allConsumerIds all consumer IDs (stores + dashboards)
     * @param totalDurationMs the total duration of the synchronization in
     * milliseconds
     * @param readDataSourcesMs time spent reading datasources
     * @param extractMappingsMs time spent extracting mappings
     * @param populateEntitiesMs time spent populating entities
     * @param pushToConsumersMs time spent pushing to consumers
     */
    private void logSynchronizationSummary(DataVersion newVersion, Set<String> syncedDataSources,
                                           java.util.List<String> allConsumerIds, long totalDurationMs, long readDataSourcesMs,
                                           long extractMappingsMs, long populateEntitiesMs, long pushToConsumersMs) {
        // Count total rows across all datasources
        long totalRows = 0;
        for (String dsName : factory.getAllDataSourceNames()) {
            java.util.List<?> data = newVersion.getDataByDataSource(dsName);
            if (data != null) {
                totalRows += data.size();
            }
        }

        // Count populated entities in stores and dashboards
        long storeRows = 0;
        long dashboardRows = 0;
        java.util.List<String> storeIds = factory.getAllStoreIds();

        for (String consumerId : allConsumerIds) {
            java.util.List<?> entities = newVersion.getPopulatedEntities(consumerId);
            if (entities != null) {
                if (storeIds.contains(consumerId)) {
                    storeRows += entities.size();
                } else {
                    dashboardRows += entities.size();
                }
            }
        }

        // Calculate overhead (version creation, swap, cleanup, etc.)
        long overheadMs = totalDurationMs - (readDataSourcesMs + extractMappingsMs + populateEntitiesMs + pushToConsumersMs);

        logger.info("Sync v{}: {} DS ({} synced) | {} rows at {} stores ({} rows), {} dashboards ({} rows) | {}ms [read:{}ms map:{}ms populate:{}ms push:{}ms other:{}ms]",
                newVersion.getVersion(),
                factory.getAllDataSourceNames().size(),
                syncedDataSources.size(),
                totalRows,
                storeIds.size(),
                storeRows,
                factory.getAllDashboardIds().size(),
                dashboardRows,
                totalDurationMs,
                readDataSourcesMs,
                extractMappingsMs,
                populateEntitiesMs,
                pushToConsumersMs,
                overheadMs);
    }

    /**
     * Helper class to accumulate aggregation values in a single loop. Maintains
     * state for one aggregation type and updates it as entities are processed.
     */
    private static class AggregationAccumulator {

        private final AggregationType type;
        private long count = 0;
        private double sum = 0.0;
        private Double min = null;
        private Double max = null;

        public AggregationAccumulator(AggregationType type) {
            this.type = type;
        }

        /**
         * Accumulates a value for this aggregation type. Called once per entity
         * in the single loop.
         */
        public void accumulate(Object value) {
            if (value == null) {
                return;
            }

            count++;

            if (type == AggregationType.COUNT) {
                // COUNT doesn't need value processing
                return;
            }

            // Convert to number for numeric aggregations
            double numValue;
            if (value instanceof Number) {
                numValue = ((Number) value).doubleValue();
            } else {
                // Skip non-numeric values for numeric aggregations
                return;
            }

            // Update aggregation state based on type
            switch (type) {
                case SUM:
                case AVG:
                    sum += numValue;
                    break;
                case MIN:
                    if (min == null || numValue < min) {
                        min = numValue;
                    }
                    break;
                case MAX:
                    if (max == null || numValue > max) {
                        max = numValue;
                    }
                    break;
                default:
                    break;
            }
        }

        /**
         * Gets the final result for this aggregation type.
         */
        public Object getResult() {
            switch (type) {
                case COUNT:
                    return count;
                case SUM:
                    return sum;
                case AVG:
                    return count > 0 ? sum / count : 0.0;
                case MIN:
                    return min;
                case MAX:
                    return max;
                default:
                    return null;
            }
        }
    }

}
