package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.ShrinkingMode;

/**
 * Preservation Property Tests — Full Sync Streaming Data Fix.
 *
 * <p>Bu test sınıfı, full sync streaming data fix'inin mevcut non-buggy davranışları
 * bozmadığını doğrulayan property-based testler içerir.</p>
 *
 * <p>Bu testler fix uygulanmadan ÖNCE yazılır ve çalıştırılır — tümü BAŞARILI olmalıdır
 * (baseline davranışı doğrular). Fix sonrası da BAŞARILI kalmalıdır (regresyon yok).</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b></p>
 */
class FullSyncStreamingDataPreservationPropertyTest {

    private static final String BATCH_DS_NAME = "batch-orders";
    private static final String STREAMING_DS_NAME = "streaming-prices";

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

    // ==================== Helper: Reflection utilities ====================

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    // ==================== Helper: Engine setup ====================

    static final class EngineTestContext {
        final DataSynchronizationEngine engine;
        final InMemorySpecStoreFactory factory;
        final DependencyGraph dependencyGraph;
        EngineTestContext(DataSynchronizationEngine engine, InMemorySpecStoreFactory factory, DependencyGraph dependencyGraph) {
            this.engine = engine;
            this.factory = factory;
            this.dependencyGraph = dependencyGraph;
        }
        DataSynchronizationEngine engine() { return engine; }
        InMemorySpecStoreFactory factory() { return factory; }
        DependencyGraph dependencyGraph() { return dependencyGraph; }
    }

    /**
     * Sets up a DataSynchronizationEngine with a batch datasource that returns
     * the given entities via fetchAllWithFallback(). No streaming datasources.
     */
    @SuppressWarnings("unchecked")
    private EngineTestContext setupEngineWithBatchDs(
            List<TestEntity> batchEntities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        // Register batch datasource with mock fetchAllWithFallback()
        DataSource<TestEntity> batchDs = mock(DataSource.class);
        when(batchDs.getName()).thenReturn(BATCH_DS_NAME);
        when(batchDs.isHealthy()).thenReturn(true);
        when(batchDs.fetchAll()).thenReturn(
                CompletableFuture.completedFuture(batchEntities));
        when(batchDs.fetchAllWithFallback()).thenReturn(
                CompletableFuture.completedFuture(batchEntities));
        factory.registerDataSource(BATCH_DS_NAME, batchDs, Duration.ofMinutes(5));

        // Create engine
        DataSynchronizationEngine engine = new DataSynchronizationEngine(factory);

        // Create DependencyGraph (empty — no streaming data)
        DependencyGraph dependencyGraph = new DependencyGraph();
        dependencyGraph.build(List.of());

        // Create IncrementalSyncProcessor
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, dependencyGraph, analysisResult, streamingVersion);

        // Set up DataSourceSyncMetadata for batch datasource
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(
                BATCH_DS_NAME, Duration.ofMinutes(5));
        metadata.setStreamingDataSource(false);

        // Inject internal state via reflection
        setField(engine, "dependencyGraph", dependencyGraph);
        setField(engine, "incrementalSyncProcessor", processor);
        setField(engine, "cachedAnalysisResult", analysisResult);

        Map<String, DataSourceSyncMetadata> metadataMap = getField(engine, "dataSourceMetadata");
        metadataMap.put(BATCH_DS_NAME, metadata);

        Set<String> pendingDs = getField(engine, "pendingDataSources");
        pendingDs.add(BATCH_DS_NAME);

        return new EngineTestContext(engine, factory, dependencyGraph);
    }

    /**
     * Sets up engine with both a batch datasource and a streaming datasource.
     * The streaming datasource is in READY state with data in DependencyGraph.
     */
    @SuppressWarnings("unchecked")
    private EngineTestContext setupEngineWithBatchAndStreaming(
            List<TestEntity> batchEntities,
            List<TestEntity> streamingDgEntities,
            List<TestEntity> streamingFetchAllEntities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        // Register batch datasource
        DataSource<TestEntity> batchDs = mock(DataSource.class);
        when(batchDs.getName()).thenReturn(BATCH_DS_NAME);
        when(batchDs.isHealthy()).thenReturn(true);
        when(batchDs.fetchAll()).thenReturn(
                CompletableFuture.completedFuture(batchEntities));
        when(batchDs.fetchAllWithFallback()).thenReturn(
                CompletableFuture.completedFuture(batchEntities));
        factory.registerDataSource(BATCH_DS_NAME, batchDs, Duration.ofMinutes(5));

        // Register streaming datasource
        StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(STREAMING_DS_NAME);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
        when(streamingDs.isHealthy()).thenReturn(true);
        when(streamingDs.fetchAll()).thenReturn(
                CompletableFuture.completedFuture(streamingFetchAllEntities));
        when(streamingDs.fetchAllWithFallback()).thenReturn(
                CompletableFuture.completedFuture(streamingFetchAllEntities));
        factory.registerDataSource(STREAMING_DS_NAME, streamingDs, Duration.ofMinutes(5));

        // Create engine
        DataSynchronizationEngine engine = new DataSynchronizationEngine(factory);

        // Create and populate DependencyGraph
        DependencyGraph dependencyGraph = new DependencyGraph();
        dependencyGraph.build(List.of());
        if (!streamingDgEntities.isEmpty()) {
            dependencyGraph.upsertAll(STREAMING_DS_NAME, streamingDgEntities);
        }

        // Create IncrementalSyncProcessor
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, dependencyGraph, analysisResult, streamingVersion);

        // Set up metadata for both datasources
        DataSourceSyncMetadata batchMeta = new DataSourceSyncMetadata(
                BATCH_DS_NAME, Duration.ofMinutes(5));
        batchMeta.setStreamingDataSource(false);

        DataSourceSyncMetadata streamingMeta = new DataSourceSyncMetadata(
                STREAMING_DS_NAME, Duration.ofMinutes(5));
        streamingMeta.setStreamingDataSource(true);
        streamingMeta.setStreamingState(StreamingDataSourceState.READY);

        // Inject internal state
        setField(engine, "dependencyGraph", dependencyGraph);
        setField(engine, "incrementalSyncProcessor", processor);
        setField(engine, "cachedAnalysisResult", analysisResult);

        Map<String, DataSourceSyncMetadata> metadataMap = getField(engine, "dataSourceMetadata");
        metadataMap.put(BATCH_DS_NAME, batchMeta);
        metadataMap.put(STREAMING_DS_NAME, streamingMeta);

        Set<String> pendingDs = getField(engine, "pendingDataSources");
        pendingDs.add(BATCH_DS_NAME);
        pendingDs.add(STREAMING_DS_NAME);

        return new EngineTestContext(engine, factory, dependencyGraph);
    }

    // ==================== Helper: Invoke readAllDataSources via reflection ====================

    private DataVersion callReadAllDataSources(DataSynchronizationEngine engine) throws Exception {
        DataVersion newVersion = new DataVersion(1, LocalDateTime.now());

        Set<String> pendingDs = getField(engine, "pendingDataSources");
        Set<String> dataSourcesNeedingSync = new java.util.HashSet<>(pendingDs);

        Method method = DataSynchronizationEngine.class.getDeclaredMethod(
                "readAllDataSources",
                DataVersion.class, java.util.Set.class);
        method.setAccessible(true);

        try {
            method.invoke(engine, newVersion, dataSourcesNeedingSync);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                // Expected: parallel phase failure — continue to check DataVersion
            } else {
                throw e;
            }
        }

        return newVersion;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> batchEntities() {
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 200),
                Arbitraries.integers().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
        ).as((id, val, name) -> new TestEntity(id, val, name));

        return entityArb.list().ofMinSize(10).ofMaxSize(50)
                .map(list -> {
                    java.util.LinkedHashMap<Integer, TestEntity> byId = new java.util.LinkedHashMap<>();
                    for (TestEntity e : list) {
                        byId.put(e.getIdentity(), e);
                    }
                    return (List<TestEntity>) new ArrayList<>(byId.values());
                })
                .filter(list -> list.size() >= 5);
    }

    @Provide
    Arbitrary<List<TestEntity>> streamingEntities() {
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(201, 400),
                Arbitraries.integers().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
        ).as((id, val, name) -> new TestEntity(id, val, name));

        return entityArb.list().ofMinSize(10).ofMaxSize(50)
                .map(list -> {
                    java.util.LinkedHashMap<Integer, TestEntity> byId = new java.util.LinkedHashMap<>();
                    for (TestEntity e : list) {
                        byId.put(e.getIdentity(), e);
                    }
                    return (List<TestEntity>) new ArrayList<>(byId.values());
                })
                .filter(list -> list.size() >= 5);
    }

    // ==================== Cleanup ====================

    private void cleanup(EngineTestContext ctx) {
        if (ctx != null) {
            try {
                ctx.engine().close();
            } catch (Exception e) {
                // ignore cleanup errors
            }
            try {
                ctx.factory().clearAll();
                ctx.factory().clearAllDataSources();
            } catch (Exception e) {
                // ignore cleanup errors
            }
        }
    }

    // ==================== Property 1: Batch Datasource fetchAllWithFallback() Preservation ====================

    /**
     * Preservation: Batch datasources are read via fetchAllWithFallback() and the data
     * is correctly written to DataVersion. This behavior must NOT change after the fix.
     *
     * <p>Observation (unfixed code): When a batch (non-streaming) datasource is marked
     * for sync, readAllDataSources() calls ensureDataSourceInDataVersion() which calls
     * readFromDataSourceRaw() which calls fetchAllWithFallback(). The returned data is
     * stored in DataVersion via setDataByDataSource().</p>
     *
     * <p>This test verifies that batch datasource data in DataVersion matches exactly
     * what fetchAllWithFallback() returns.</p>
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 10)
    void batchDatasourceDataVersionContainsFetchAllWithFallbackData(
            @ForAll("batchEntities") List<TestEntity> entities) throws Exception {

        EngineTestContext ctx = null;
        try {
            ctx = setupEngineWithBatchDs(entities);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            // PROPERTY: DataVersion must contain exactly the data from fetchAllWithFallback()
            List<?> dvData = newVersion.getDataByDataSource(BATCH_DS_NAME);

            assertThat(dvData)
                    .as("Batch datasource '%s' data in DataVersion must match fetchAllWithFallback() result",
                            BATCH_DS_NAME)
                    .isNotNull()
                    .hasSize(entities.size());

            // Compare element-by-element
            List<String> dvStrings = dvData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = entities.stream().map(Object::toString).sorted().toList();
            assertThat(dvStrings)
                    .as("Batch datasource data must match fetchAllWithFallback() data exactly")
                    .isEqualTo(expectedStrings);

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 2: Batch Datasource Unaffected by Streaming Presence ====================

    /**
     * Preservation: When both batch and streaming datasources exist, the batch datasource
     * reading via fetchAllWithFallback() is unaffected by the streaming datasource's presence.
     *
     * <p>Observation (unfixed code): The parallel phase reads batch datasources via
     * ensureDataSourceInDataVersion() → readFromDataSourceRaw() → fetchAllWithFallback().
     * The streaming block runs AFTER the parallel phase. Batch data should be independent.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.3</b></p>
     */
    @Property(tries = 10)
    void batchDatasourceUnaffectedByStreamingDatasourcePresence(
            @ForAll("batchEntities") List<TestEntity> batchData,
            @ForAll("streamingEntities") List<TestEntity> streamingDgData,
            @ForAll("streamingEntities") List<TestEntity> streamingFetchAllData) throws Exception {

        EngineTestContext ctx = null;
        try {
            ctx = setupEngineWithBatchAndStreaming(batchData, streamingDgData, streamingFetchAllData);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            // PROPERTY: Batch datasource data must be exactly what fetchAllWithFallback() returned
            List<?> dvBatchData = newVersion.getDataByDataSource(BATCH_DS_NAME);

            assertThat(dvBatchData)
                    .as("Batch datasource data must be present regardless of streaming datasource")
                    .isNotNull()
                    .hasSize(batchData.size());

            List<String> dvStrings = dvBatchData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = batchData.stream().map(Object::toString).sorted().toList();
            assertThat(dvStrings)
                    .as("Batch datasource data must match fetchAllWithFallback() exactly, "
                            + "unaffected by streaming datasource presence")
                    .isEqualTo(expectedStrings);

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 3: TimeWindowRule Filtering Preservation ====================

    /**
     * Preservation: TimeWindowRule filtering is applied to datasources during full sync.
     * Entities that don't pass the TimeWindowRule specification are filtered out.
     *
     * <p>Observation (unfixed code): After data is loaded into DataVersion via the parallel
     * phase, applyTimeWindowRuleToDataVersion() is called for each datasource. If a
     * TimeWindowRule is registered, entities are filtered using the specification.</p>
     *
     * <p>This test registers a TimeWindowRule that filters entities with value > threshold,
     * and verifies that only matching entities remain in DataVersion.</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 10)
    @SuppressWarnings("unchecked")
    void timeWindowRuleFilteringAppliedDuringFullSync(
            @ForAll("batchEntities") List<TestEntity> entities) throws Exception {

        // Use value > 5000 as the filter threshold — entities with value <= 5000 are "expired"
        final int THRESHOLD = 5000;

        EngineTestContext ctx = null;
        try {
            ctx = setupEngineWithBatchDs(entities);

            // Register a TimeWindowRule that keeps only entities with value > THRESHOLD
            Specification<TestEntity> spec = new Specification<>() {
                @Override
                public Predicate<TestEntity> toPredicate() {
                    return e -> e.getValue() > THRESHOLD;
                }

                @Override
                public boolean test(TestEntity entity) {
                    return entity.getValue() > THRESHOLD;
                }
            };

            TimeWindowRule<TestEntity> rule = new TimeWindowRule<>(BATCH_DS_NAME, () -> spec);
            ctx.factory().registerTimeWindowRule(BATCH_DS_NAME, rule);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            // Calculate expected filtered entities
            List<TestEntity> expectedFiltered = entities.stream()
                    .filter(e -> e.getValue() > THRESHOLD)
                    .toList();

            List<?> dvData = newVersion.getDataByDataSource(BATCH_DS_NAME);

            // PROPERTY: DataVersion must contain only entities that pass the TimeWindowRule
            assertThat(dvData)
                    .as("TimeWindowRule filtering must be applied — only entities with value > %d should remain",
                            THRESHOLD)
                    .isNotNull()
                    .hasSize(expectedFiltered.size());

            if (!expectedFiltered.isEmpty()) {
                List<String> dvStrings = dvData.stream().map(Object::toString).sorted().toList();
                List<String> expectedStrings = expectedFiltered.stream()
                        .map(Object::toString).sorted().toList();
                assertThat(dvStrings)
                        .as("Filtered data must match expected TimeWindowRule result")
                        .isEqualTo(expectedStrings);
            }

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 4: initializeStreamingInfrastructure fetchAll() Preservation ====================

    /**
     * Preservation: initializeStreamingInfrastructure() uses fetchAll() to load initial
     * state into DependencyGraph. This behavior must NOT change after the fix.
     *
     * <p>Observation (unfixed code): When a streaming datasource is registered,
     * initializeStreamingInfrastructure() calls fetchAll() on it, and the returned
     * entities are processed via processBatchDataSourceResult() which writes them
     * to DependencyGraph. The datasource transitions to READY state.</p>
     *
     * <p>This test verifies that after initializeStreamingInfrastructure(), the
     * DependencyGraph contains the entities returned by fetchAll().</p>
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 10, shrinking = ShrinkingMode.OFF)
    @SuppressWarnings("unchecked")
    void initializeStreamingInfrastructureFetchAllLoadsIntoDependencyGraph(
            @ForAll("streamingEntities") List<TestEntity> initialEntities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        DataSynchronizationEngine engine = null;
        try {
            // Register streaming datasource with mock fetchAll()
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(STREAMING_DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
            when(streamingDs.isHealthy()).thenReturn(true);
            when(streamingDs.fetchAll()).thenReturn(
                    CompletableFuture.completedFuture(initialEntities));
            when(streamingDs.fetchAllWithFallback()).thenReturn(
                    CompletableFuture.completedFuture(initialEntities));
            factory.registerDataSource(STREAMING_DS_NAME, streamingDs, Duration.ofMinutes(5));

            // Create engine — constructor does NOT call initializeStreamingInfrastructure
            engine = new DataSynchronizationEngine(factory);

            // Call initializeStreamingInfrastructure via reflection
            Method initMethod = DataSynchronizationEngine.class.getDeclaredMethod(
                    "initializeStreamingInfrastructure");
            initMethod.setAccessible(true);
            initMethod.invoke(engine);

            // Get DependencyGraph from engine
            DependencyGraph dg = getField(engine, "dependencyGraph");

            // PROPERTY: DependencyGraph must contain the entities from fetchAll()
            List<?> dgData = dg.findAll(STREAMING_DS_NAME);

            assertThat(dgData)
                    .as("initializeStreamingInfrastructure() must load fetchAll() data into DependencyGraph")
                    .isNotNull()
                    .hasSize(initialEntities.size());

            // Verify data matches
            List<String> dgStrings = dgData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = initialEntities.stream()
                    .map(Object::toString).sorted().toList();
            assertThat(dgStrings)
                    .as("DependencyGraph data must match fetchAll() result after initialization")
                    .isEqualTo(expectedStrings);

            // Verify streaming state transitioned to READY
            Map<String, DataSourceSyncMetadata> metadataMap = getField(engine, "dataSourceMetadata");
            DataSourceSyncMetadata meta = metadataMap.get(STREAMING_DS_NAME);
            assertThat(meta)
                    .as("Streaming datasource metadata must exist after initialization")
                    .isNotNull();
            assertThat(meta.getStreamingState())
                    .as("Streaming datasource must transition to READY after successful fetchAll()")
                    .isEqualTo(StreamingDataSourceState.READY);

        } finally {
            if (engine != null) {
                try { engine.close(); } catch (Exception e) { /* ignore */ }
            }
            factory.clearAll();
            factory.clearAllDataSources();
        }
    }

    // ==================== Property 5: clearIntermediateData() Preservation ====================

    /**
     * Preservation: clearIntermediateData() clears dataByDataSource, groupedData,
     * and commonAggregationResults from DataVersion. This behavior must NOT change.
     *
     * <p>Observation (unfixed code): After full sync completes and data is pushed to
     * consumers, clearIntermediateData() is called to free memory. It clears all
     * intermediate data structures.</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 10)
    void clearIntermediateDataClearsAllIntermediateStructures(
            @ForAll("batchEntities") List<TestEntity> entities) throws Exception {

        EngineTestContext ctx = null;
        try {
            ctx = setupEngineWithBatchDs(entities);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            // Precondition: DataVersion has data
            assertThat(newVersion.getDataByDataSource(BATCH_DS_NAME))
                    .as("Precondition: DataVersion must have batch data before clearIntermediateData()")
                    .isNotNull()
                    .isNotEmpty();

            // Act: call clearIntermediateData()
            newVersion.clearIntermediateData();

            // PROPERTY: After clearIntermediateData(), dataByDataSource must be cleared
            List<?> dataAfterClear = newVersion.getDataByDataSource(BATCH_DS_NAME);
            assertThat(dataAfterClear)
                    .as("clearIntermediateData() must clear dataByDataSource — data should be null after clear")
                    .isNull();

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 6: Incremental Sync Processing Preservation ====================

    /**
     * Preservation: Incremental sync (streaming event) processing via IncrementalSyncProcessor
     * writes data to DependencyGraph. This behavior must NOT change after the fix.
     *
     * <p>Observation (unfixed code): When a streaming event arrives, IncrementalSyncProcessor
     * processes it via processBatchDataSourceResult() which upserts entities into DependencyGraph.</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 10)
    void incrementalSyncWritesToDependencyGraph(
            @ForAll("streamingEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Register streaming datasource (needed for factory lookup)
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(STREAMING_DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            when(streamingDs.isHealthy()).thenReturn(true);
            factory.registerDataSource(STREAMING_DS_NAME, streamingDs, Duration.ofMinutes(5));

            // Create DependencyGraph and IncrementalSyncProcessor
            DependencyGraph dg = new DependencyGraph();
            dg.build(List.of());
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);
            AtomicLong streamingVersion = new AtomicLong(0);
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, dg, analysisResult, streamingVersion);

            // Act: process batch datasource result (simulates initial load or batch event)
            processor.processBatchDataSourceResult(STREAMING_DS_NAME, entities);

            // PROPERTY: DependencyGraph must contain the processed entities
            List<?> dgData = dg.findAll(STREAMING_DS_NAME);

            assertThat(dgData)
                    .as("IncrementalSyncProcessor must write entities to DependencyGraph")
                    .isNotNull()
                    .hasSize(entities.size());

            List<String> dgStrings = dgData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = entities.stream()
                    .map(Object::toString).sorted().toList();
            assertThat(dgStrings)
                    .as("DependencyGraph data must match processed entities")
                    .isEqualTo(expectedStrings);

        } finally {
            factory.clearAll();
            factory.clearAllDataSources();
        }
    }
}
