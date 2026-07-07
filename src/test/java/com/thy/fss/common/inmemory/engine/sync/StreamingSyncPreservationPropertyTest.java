package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Preservation Property Tests — Mevcut Davranış Korunması.
 *
 * <p>Bu test sınıfı, bug koşulu geçerli OLMAYAN girdiler için mevcut davranışı
 * yakalayan property-based testler içerir. Testler düzeltilmemiş kodda GEÇMELİDİR
 * — mevcut davranışı doğrular.</p>
 *
 * <p>Observation-first methodology: Önce düzeltilmemiş kodda davranış gözlemlendi,
 * sonra bu davranışı yakalayan property'ler yazıldı.</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11</b></p>
 */
class StreamingSyncPreservationPropertyTest {

    private static final String DS_NAME = "preservation-test-ds";
    private static final String BATCH_DS = "batch-ds";
    private static final String STREAMING_DS = "streaming-ds";
    private static final String OTHER_DS = "other-ds";
    private static final String TEST_ENTITIES = "testEntities";

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final int value;
        private final String name;

        TestEntity(int id, int value, String name) {
            this.id = id;
            this.value = value;
            this.name = name;
        }

        @Override
        public Integer getIdentity() { return id; }

    public Integer getId() {
        return id;
    }

        public int getValue() { return value; }

        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id == that.id && value == that.value && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() { return Objects.hash(id, value, name); }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", value=" + value + ", name='" + name + "'}";
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> testEntities() {
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 50),
                Arbitraries.integers().between(1, 1000),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
        ).as(TestEntity::new);
        return entityArb.list().ofMinSize(1).ofMaxSize(15);
    }


    // ==================== 3.1, 3.7: upsertAll copy-on-write volatile swap ====================

    /**
     * Property: upsertAll() creates a new TreeMap via copy-on-write and performs
     * a volatile swap. After upsert, findAll() returns a consistent snapshot
     * containing all upserted entities. Existing entities for other datasources
     * are not affected.
     *
     * <p>Observed behavior: upsertAll uses entityStore.compute() which creates a
     * new TreeMap, copies existing entries, adds new ones, and atomically swaps
     * the reference. findAll() returns an unmodifiable list snapshot.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.7</b></p>
     */
    @Property(tries = 50)
    void preservationUpsertAllCopyOnWriteVolatileSwap(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        DependencyGraph graph = new DependencyGraph();

        // Pre-populate with a different datasource to verify isolation
        TestEntity otherDsEntity = new TestEntity(999, 999, "other");
        graph.upsertAll(OTHER_DS, List.of(otherDsEntity));

        // Capture state before upsert
        List<TestEntity> beforeOtherDs = graph.findAll(OTHER_DS);

        // Perform upsertAll — copy-on-write volatile swap
        graph.upsertAll(DS_NAME, entities);

        // Verify: findAll returns consistent snapshot with all entities
        List<TestEntity> result = graph.findAll(DS_NAME);
        assertThat(result).isNotNull();

        // All upserted entities should be findable by ID (last-write-wins for duplicates)
        for (TestEntity entity : entities) {
            TestEntity found = graph.findById(DS_NAME, entity.getIdentity());
            assertThat(found)
                    .as("Entity with id=%d should be in graph after upsertAll", entity.getIdentity())
                    .isNotNull();
        }

        // Other datasource should be unaffected (isolation)
        List<TestEntity> afterOtherDs = graph.findAll(OTHER_DS);
        assertThat(afterOtherDs).isEqualTo(beforeOtherDs);

        // findAll returns unmodifiable list
        assertThat(result).isUnmodifiable();
    }

    /**
     * Property: upsertAll with duplicate IDs — last entity in the list wins.
     * This is the existing upsert semantics (insert or update).
     *
     * <p><b>Validates: Requirements 3.1, 3.7</b></p>
     */
    @Property(tries = 30)
    void preservationUpsertAllLastWriteWins(
            @ForAll @IntRange(min = 1, max = 10) int entityId,
            @ForAll @IntRange(min = 1, max = 500) int value1,
            @ForAll @IntRange(min = 501, max = 1000) int value2) {

        DependencyGraph graph = new DependencyGraph();

        // Upsert two entities with the same ID — second should win
        List<TestEntity> entities = List.of(
                new TestEntity(entityId, value1, "first"),
                new TestEntity(entityId, value2, "second")
        );
        graph.upsertAll(DS_NAME, entities);

        TestEntity result = graph.findById(DS_NAME, entityId);
        assertThat(result)
                .as("Last entity with same ID should win in upsertAll")
                .isNotNull();
        assertThat(result.getValue()).isEqualTo(value2);
        assertThat(result.getName()).isEqualTo("second");
    }

    // ==================== 3.2: No TimeWindowRule → entities pass through ====================

    /**
     * Property: When no TimeWindowRule is configured for a datasource,
     * all entities pass through the pipeline without filtering.
     *
     * <p>Observed behavior: applyTimeWindowFilter returns entities unchanged
     * when factory.getTimeWindowRule() returns null.</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 30)
    void preservationNoTimeWindowRuleAllEntitiesPassThrough(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs); // null = no TimeWindowRule

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Process entities — no TimeWindowRule configured
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // All entities should be in the graph (none filtered)
            List<TestEntity> result = graph.findAll(DS_NAME);

            // Count unique IDs from input (last-write-wins for duplicates)
            java.util.Map<Integer, TestEntity> uniqueById = new java.util.LinkedHashMap<>();
            for (TestEntity e : entities) {
                uniqueById.put(e.getIdentity(), e);
            }

            assertThat(result)
                    .as("Without TimeWindowRule, all unique entities should pass through")
                    .hasSize(uniqueById.size());

        } finally {
            factory.clearAll();
        }
    }

    // ==================== 3.3: PropertyMapping evaluation and PK/FK detection ====================

    /**
     * Property: shouldReevaluateMapping returns true for mappings with no
     * collection operations (simple mappings). This is the conservative approach.
     *
     * <p>Observed behavior: When collectionOps is null or empty, the method
     * returns true (always re-evaluate simple mappings).</p>
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 20)
    void preservationSimpleMappingAlwaysReevaluated(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Create a simple mapping (no collection operations) and add to graph
            @SuppressWarnings("unchecked")
            PropertyMapping<TestEntity, ?> simpleMapping = mock(PropertyMapping.class);
            when(simpleMapping.getDataSourceName()).thenReturn(DS_NAME);
            when(simpleMapping.getConsumerId()).thenReturn("test-consumer");
            when(simpleMapping.isForDashboard()).thenReturn(false);
            when(simpleMapping.getSourceCollectionOperations()).thenReturn(null);

            graph.addMapping(simpleMapping);

            // Use reflection to call shouldReevaluateMapping
            java.lang.reflect.Method method = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "shouldReevaluateMapping",
                    PropertyMapping.class, String.class, Set.class, Map.class, Map.class);
            method.setAccessible(true);

            Set<Object> changedIds = Set.of(1, 2, 3);
            Map<String, Object> emptyFirstLastIds = Map.of();
            Map<Object, Object> emptyOldEntityStates = Map.of();
            boolean result = (boolean) method.invoke(processor, simpleMapping, DS_NAME, changedIds, emptyFirstLastIds, emptyOldEntityStates);

            assertThat(result)
                    .as("Simple mappings (no collection ops) should always be re-evaluated")
                    .isTrue();

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            factory.clearAll();
        }
    }

    /**
     * Property: ALL collection selector always triggers re-evaluation.
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 20)
    void preservationAllSelectorAlwaysReevaluated(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Create a mapping with ALL collection selector
            CollectionAttribute<TestEntity, TestEntity> collAttr =
                    new CollectionAttribute<>("entities", TestEntity.class, TestEntity.class);
            CollectionOperationMetadata<TestEntity, TestEntity> allOp =
                    new CollectionOperationMetadata<>(0, collAttr, CollectionSelector.ALL, null);

            @SuppressWarnings("unchecked")
            PropertyMapping<TestEntity, ?> mapping = mock(PropertyMapping.class);
            when(mapping.getDataSourceName()).thenReturn(DS_NAME);
            when(mapping.getConsumerId()).thenReturn("test-consumer");
            when(mapping.isForDashboard()).thenReturn(false);
            when(mapping.getSourceCollectionOperations()).thenReturn(List.of(allOp));

            java.lang.reflect.Method method = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "shouldReevaluateMapping",
                    PropertyMapping.class, String.class, Set.class, Map.class, Map.class);
            method.setAccessible(true);

            Set<Object> changedIds = Set.of(1);
            Map<String, Object> emptyFirstLastIds = Map.of();
            Map<Object, Object> emptyOldEntityStates = Map.of();
            boolean result = (boolean) method.invoke(processor, mapping, DS_NAME, changedIds, emptyFirstLastIds, emptyOldEntityStates);

            assertThat(result)
                    .as("ALL selector should always trigger re-evaluation")
                    .isTrue();

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            factory.clearAll();
        }
    }


    // ==================== 3.4: InMemoryDataStore.updateData volatile swap ====================

    /**
     * Property: InMemoryDataStore.updateData(List, long) performs a volatile reference
     * swap. After update, findAll() returns the new data, and
     * getVersion() returns the provided version.
     *
     * <p>Observed behavior: updateData sets currentData via volatile write.
     * findAll() returns the volatile reference. Version is stored as-is.</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 50)
    void preservationInMemoryDataStoreUpdateDataVolatileSwap(
            @ForAll @IntRange(min = 1, max = 10) int entityCount,
            @ForAll @IntRange(min = 1, max = 10000) int version) {

        // Use SimpleUser which has @MetaModel annotation and generated SpecificationService
        InMemoryDataStore<com.thy.fss.common.inmemory.testmodel.SimpleUser> store =
                new InMemoryDataStore<>(
                        com.thy.fss.common.inmemory.testmodel.SimpleUser.class,
                        "test-store", DS_NAME, null, null);

        // Initial state
        assertThat(store.findAll()).isEmpty();
        assertThat(store.getVersion()).isEqualTo(0L);

        // Create test data
        List<com.thy.fss.common.inmemory.testmodel.SimpleUser> data = new java.util.ArrayList<>();
        for (int i = 0; i < entityCount; i++) {
            com.thy.fss.common.inmemory.testmodel.SimpleUser user =
                    new com.thy.fss.common.inmemory.testmodel.SimpleUser();
            user.setId((long) i);
            user.setName("user-" + i);
            data.add(user);
        }

        // Update with data and version — volatile swap
        store.updateData(data, version);

        // Verify volatile swap: findAll returns the data, version is stored
        assertThat(store.findAll()).hasSize(entityCount);
        assertThat(store.getVersion()).isEqualTo(version);
        assertThat(store.size()).isEqualTo(entityCount);

        // Second update overwrites atomically
        com.thy.fss.common.inmemory.testmodel.SimpleUser singleUser =
                new com.thy.fss.common.inmemory.testmodel.SimpleUser();
        singleUser.setId(99L);
        singleUser.setName("single");
        store.updateData(List.of(singleUser), version + 1);
        assertThat(store.findAll()).hasSize(1);
        assertThat(store.getVersion()).isEqualTo(version + 1);

        // Null handling: null data becomes empty list
        store.updateData(null, version + 2);
        assertThat(store.findAll()).isEmpty();
        assertThat(store.getVersion()).isEqualTo(version + 2);
    }

    // ==================== 3.5: Event queuing during full sync ====================

    /**
     * Property: During full sync (fullSyncInProgress=true), events queued via
     * queueEvent() are preserved in FIFO order and processed sequentially
     * by processQueuedEvents() after sync completes.
     *
     * <p>Observed behavior: ConcurrentLinkedQueue maintains FIFO order.
     * processQueuedEvents() polls and processes each event exactly once.</p>
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 30)
    void preservationEventQueuingFifoOrder(
            @ForAll(TEST_ENTITIES) List<TestEntity> batch1,
            @ForAll(TEST_ENTITIES) List<TestEntity> batch2) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Set full sync in progress
            processor.setFullSyncInProgress(true);
            assertThat(processor.isFullSyncInProgress()).isTrue();

            // Queue events
            BatchSnapshotEvent<TestEntity> event1 = new BatchSnapshotEvent<>(
                    batch1, Instant.now());
            BatchSnapshotEvent<TestEntity> event2 = new BatchSnapshotEvent<>(
                    batch2, Instant.now());

            processor.queueEvent(DS_NAME, event1);
            processor.queueEvent(DS_NAME, event2);

            // Verify queue count
            assertThat(processor.getQueuedEventCount()).isEqualTo(2);

            // End full sync and process queued events
            processor.setFullSyncInProgress(false);
            assertThat(processor.isFullSyncInProgress()).isFalse();

            processor.processQueuedEvents();

            // Queue should be empty after processing
            assertThat(processor.getQueuedEventCount()).isEqualTo(0);

            // All entities from both batches should be in the graph
            List<TestEntity> result = graph.findAll(DS_NAME);
            assertThat(result).isNotEmpty();

            // Verify FIFO: batch2 processed after batch1, so for entities with same ID,
            // the graph should contain the batch2 version (last-write-wins).
            // We collect the expected final state: batch1 first, then batch2 overwrites.
            java.util.Map<Integer, TestEntity> expectedState = new java.util.LinkedHashMap<>();
            for (TestEntity e : batch1) {
                expectedState.put(e.getIdentity(), e);
            }
            for (TestEntity e : batch2) {
                expectedState.put(e.getIdentity(), e);
            }

            assertThat(result).hasSize(expectedState.size());

            for (TestEntity expected : expectedState.values()) {
                TestEntity found = graph.findById(DS_NAME, expected.getIdentity());
                assertThat(found)
                        .as("Entity id=%d should match the last-written version", expected.getIdentity())
                        .isEqualTo(expected);
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== 3.6: INITIALIZING datasource access restrictions ====================

    /**
     * Property: When a streaming datasource is in INITIALIZING state and the event
     * is NOT an initial load, Phases 2-4 are skipped. Only Phase 1 (entity upsert)
     * runs — data accumulates in DependencyGraph but is not propagated to consumers.
     *
     * <p>Observed behavior: processBatchSnapshot checks isDataSourceInitializing()
     * and skips remaining phases when true and event is not initial load.</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 30)
    void preservationInitializingDatasourceSkipPhases2to4(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource in INITIALIZING state
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
            factory.registerDataSource(DS_NAME, streamingDs);

            // Add a mapping so Phase 2 would have work to do
            @SuppressWarnings("unchecked")
            PropertyMapping<TestEntity, ?> mapping = mock(PropertyMapping.class);
            when(mapping.getDataSourceName()).thenReturn(DS_NAME);
            when(mapping.getConsumerId()).thenReturn("test-store");
            when(mapping.isForDashboard()).thenReturn(false);
            graph.addMapping(mapping);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Process non-initial-load event while INITIALIZING
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // Phase 1 should have run — entities are in DependencyGraph
            List<TestEntity> graphEntities = graph.findAll(DS_NAME);
            assertThat(graphEntities)
                    .as("Phase 1 should run even during INITIALIZING")
                    .isNotEmpty();

            // Phase 4 should NOT have run — version should still be 0
            // (Bug 3 fix: version is now shared from Engine; no increment happens here)
            assertThat(processor.getLocalStreamingVersion())
                    .as("Version should not increment when phases 2-4 are skipped (INITIALIZING)")
                    .isEqualTo(0L);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== 3.8: TimeWindowRule filtering behavior ====================

    /**
     * Property: When a TimeWindowRule is configured, entities failing the
     * specification test are filtered out before entering the pipeline.
     * Only entities passing the specification are upserted to DependencyGraph.
     *
     * <p>Observed behavior: applyTimeWindowFilter iterates entities and keeps
     * only those where specification.test(entity) returns true.</p>
     *
     * <p><b>Validates: Requirements 3.8</b></p>
     */
    @Property(tries = 30)
    void preservationTimeWindowRuleFiltersExpiredEntities(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities,
            @ForAll @IntRange(min = 100, max = 900) int threshold) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Create a TimeWindowRule that filters entities with value > threshold
            Specification<TestEntity> spec = (Specification<TestEntity>) () -> e -> e.getValue() <= threshold;
            TimeWindowRule<TestEntity> rule = new TimeWindowRule<>(
                    DS_NAME, Duration.ofHours(1), () -> spec);

            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);
            factory.registerTimeWindowRule(DS_NAME, rule);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // Only entities with value <= threshold should be in the graph
            List<TestEntity> result = graph.findAll(DS_NAME);
            for (TestEntity e : result) {
                assertThat(e.getValue())
                        .as("Entity id=%d with value=%d should pass TimeWindowRule (threshold=%d)",
                                e.getIdentity(), e.getValue(), threshold)
                        .isLessThanOrEqualTo(threshold);
            }

            // Count expected entities (unique by ID, last-write-wins, only passing ones)
            java.util.Map<Integer, TestEntity> expectedById = new java.util.LinkedHashMap<>();
            for (TestEntity e : entities) {
                if (e.getValue() <= threshold) {
                    expectedById.put(e.getIdentity(), e);
                }
            }
            assertThat(result).hasSize(expectedById.size());

        } finally {
            factory.clearAll();
        }
    }

    // ==================== 3.9: DataSource interface preserved ====================

    /**
     * Property: The DataSource interface contract is preserved — StreamingDataSource
     * is a separate interface that does NOT extend DataSource. Both coexist.
     *
     * <p>This is a structural verification that the interface hierarchy is maintained.</p>
     *
     * <p><b>Validates: Requirements 3.9</b></p>
     */
    @Property(tries = 1)
        void preservationDataSourceInterfacePreserved() {
            // Verify StreamingDataSource EXTENDS DataSource (unified hierarchy)
            assertThat(com.thy.fss.common.inmemory.datasource.DataSource.class
                    .isAssignableFrom(StreamingDataSource.class))
                    .as("StreamingDataSource should extend DataSource — unified interface hierarchy")
                    .isTrue();

            // Verify DataSource interface has the expected methods
            try {
                com.thy.fss.common.inmemory.datasource.DataSource.class.getMethod("fetchAll");
                com.thy.fss.common.inmemory.datasource.DataSource.class.getMethod("isHealthy");
                com.thy.fss.common.inmemory.datasource.DataSource.class.getMethod("close");
                com.thy.fss.common.inmemory.datasource.DataSource.class.getMethod("getName");
                com.thy.fss.common.inmemory.datasource.DataSource.class.getMethod("getFallbackDataSource");
            } catch (NoSuchMethodException e) {
                throw new AssertionError("DataSource interface is missing expected method: " + e.getMessage());
            }

            // Verify StreamingDataSource inherits DataSource methods and has its own streaming-specific methods
            try {
                StreamingDataSource.class.getMethod("getName");
                StreamingDataSource.class.getMethod("isHealthy");
                StreamingDataSource.class.getMethod("close");
                StreamingDataSource.class.getMethod("fetchAll");
                StreamingDataSource.class.getMethod("getFallbackDataSource");
                StreamingDataSource.class.getMethod("getState");
                StreamingDataSource.class.getMethod("subscribe", com.thy.fss.common.inmemory.datasource.BatchSnapshotEventListener.class);
                StreamingDataSource.class.getMethod("unsubscribe", com.thy.fss.common.inmemory.datasource.BatchSnapshotEventListener.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError("StreamingDataSource interface is missing expected method: " + e.getMessage());
            }
        }


    // ==================== 3.10: Batch datasource registration flow ====================

    /**
     * Property: Batch datasource registration via registerDataSource preserves
     * the datasource, its sync interval, and makes it available via getAllDataSourceNames().
     * Streaming datasources are registered separately and do NOT appear in batch list.
     *
     * <p><b>Validates: Requirements 3.10</b></p>
     */
    @Property(tries = 1)
    void preservationBatchDatasourceRegistrationFlow() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Register a batch datasource
            @SuppressWarnings("unchecked")
            com.thy.fss.common.inmemory.datasource.DataSource<TestEntity> batchDs =
                    mock(com.thy.fss.common.inmemory.datasource.DataSource.class);
            when(batchDs.getName()).thenReturn(BATCH_DS);
            when(batchDs.fetchAll()).thenReturn(CompletableFuture.completedFuture(List.of()));

            factory.registerDataSource(BATCH_DS, batchDs, Duration.ofMinutes(5));

            // Register a streaming datasource
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(STREAMING_DS);
            factory.registerDataSource(STREAMING_DS, streamingDs);

            // After unification, both batch and streaming datasources appear in getAllDataSourceNames
            assertThat(factory.getAllDataSourceNames()).contains(BATCH_DS);
            assertThat(factory.getAllDataSourceNames()).contains(STREAMING_DS);
            assertThat(factory.hasDataSource(BATCH_DS)).isTrue();
            assertThat(factory.getDataSourceInterval(BATCH_DS)).isEqualTo(Duration.ofMinutes(5));

            // Streaming datasource should be in streaming list
            assertThat(factory.getAllStreamingDataSourceNames()).contains(STREAMING_DS);
            assertThat(factory.isStreamingDataSource(STREAMING_DS)).isTrue();

            // Batch datasource should NOT be identified as streaming
            assertThat(factory.isStreamingDataSource(BATCH_DS)).isFalse();

        } finally {
            factory.clearAll();
        }
    }

    // ==================== 3.11: DependencyGraph streaming entity store ====================

    /**
     * Property: DependencyGraph streaming entity store uses TreeMap-based structure.
     * upsert/removeById semantics are preserved: upsert adds or updates by ID,
     * removeById removes by ID, findAll returns consistent snapshot.
     *
     * <p>Observed behavior: entityStore is ConcurrentHashMap&lt;String, TreeMap&gt;.
     * upsert creates new TreeMap via compute (copy-on-write). removeById uses
     * computeIfPresent. findAll iterates values into unmodifiable list.</p>
     *
     * <p><b>Validates: Requirements 3.11</b></p>
     */
    @Property(tries = 50)
    void preservationDependencyGraphUpsertRemoveByIdSemantics(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities,
            @ForAll @IntRange(min = 1, max = 50) int removeId) {

        DependencyGraph graph = new DependencyGraph();

        // Upsert all entities
        graph.upsertAll(DS_NAME, entities);

        // Count unique IDs
        java.util.Set<Integer> uniqueIds = new java.util.HashSet<>();
        for (TestEntity e : entities) {
            uniqueIds.add(e.getIdentity());
        }

        List<TestEntity> afterUpsert = graph.findAll(DS_NAME);
        assertThat(afterUpsert).hasSize(uniqueIds.size());

        // Single upsert (update existing or insert new)
        TestEntity newEntity = new TestEntity(removeId, 9999, "updated");
        graph.upsert(DS_NAME, newEntity);

        TestEntity found = graph.findById(DS_NAME, removeId);
        assertThat(found)
                .as("After upsert, entity should be findable by ID")
                .isNotNull();
        assertThat(found.getValue()).isEqualTo(9999);

        // Remove by ID
        graph.removeById(DS_NAME, removeId);

        TestEntity afterRemove = graph.findById(DS_NAME, removeId);
        assertThat(afterRemove)
                .as("After removeById, entity should not be findable")
                .isNull();

        // findAll should not contain the removed entity
        List<TestEntity> afterRemoveAll = graph.findAll(DS_NAME);
        for (TestEntity e : afterRemoveAll) {
            assertThat(e.getIdentity())
                    .as("Removed entity should not appear in findAll")
                    .isNotEqualTo(removeId);
        }
    }

    /**
     * Property: DependencyGraph findAll returns empty list for unknown datasource,
     * and findById returns null for unknown datasource or unknown ID.
     *
     * <p><b>Validates: Requirements 3.11</b></p>
     */
    @Property(tries = 10)
    void preservationDependencyGraphEmptyForUnknownDatasource() {
        DependencyGraph graph = new DependencyGraph();

        List<TestEntity> result = graph.findAll("nonexistent-ds");
        assertThat(result)
                .as("findAll for unknown datasource should return empty list")
                .isEmpty();

        TestEntity found = graph.findById("nonexistent-ds", 1);
        assertThat(found)
                .as("findById for unknown datasource should return null")
                .isNull();
    }

    /**
     * Property: DependencyGraph entity store maintains TreeMap ordering (by entity ID).
     * findAll returns entities sorted by their natural ID order.
     *
     * <p><b>Validates: Requirements 3.11</b></p>
     */
    @Property(tries = 30)
    void preservationDependencyGraphTreeMapOrdering(
            @ForAll(TEST_ENTITIES) List<TestEntity> entities) {

        DependencyGraph graph = new DependencyGraph();
        graph.upsertAll(DS_NAME, entities);

        List<TestEntity> result = graph.findAll(DS_NAME);

        // Verify entities are in TreeMap natural order (by Integer ID)
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).getIdentity())
                    .as("Entities should be ordered by ID (TreeMap natural order)")
                    .isGreaterThan(result.get(i - 1).getIdentity());
        }
    }
}
