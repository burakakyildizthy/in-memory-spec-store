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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fault Condition Exploration Property Tests — Full Sync Streaming Data Bug.
 *
 * <p>Bu test sınıfı, full sync sırasında streaming datasource verilerinin
 * {@code fetchAll()} ile dış kaynaktan okunması bug'ının varlığını kanıtlayan
 * property-based testler içerir.</p>
 *
 * <p>Bug: {@code readAllDataSources()} metodu streaming datasource verileri için
 * DependencyGraph (single source of truth) yerine {@code fetchAll()} dış kaynak
 * çağrısı yapıyor. Ayrıca sadece READY durumunu işliyor — INITIALIZING ve ERROR
 * durumlarındaki streaming datasource'lar tamamen atlanıyor.</p>
 *
 * <p><b>KRİTİK</b>: Bu testler düzeltilmemiş kodda BAŞARISIZ olmalıdır —
 * başarısızlık bug'ın varlığını kanıtlar.</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3</b></p>
 */
class FullSyncStreamingDataFaultConditionPropertyTest {

    private static final String STREAMING_DS_NAME = "streaming-orders";

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

    // ==================== Helper: Set up engine internals via reflection ====================

    /**
     * Sets up a DataSynchronizationEngine with the minimum internal state needed
     * to call readAllDataSources(). Uses reflection to set private fields and
     * invoke package-private methods.
     */
    @SuppressWarnings("unchecked")
    private EngineTestContext setupEngine(
            List<TestEntity> dgEntities,
            List<TestEntity> fetchAllEntities,
            StreamingDataSourceState streamingState) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        // 1. Register streaming datasource in factory with mock fetchAll()
        StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(STREAMING_DS_NAME);
        when(streamingDs.getState()).thenReturn(streamingState);
        when(streamingDs.isHealthy()).thenReturn(streamingState == StreamingDataSourceState.READY);
        // Mock both fetchAll() and fetchAllWithFallback() — Mockito doesn't delegate
        // default interface methods, so the parallel phase would NPE without this.
        when(streamingDs.fetchAll()).thenReturn(
                CompletableFuture.completedFuture(fetchAllEntities));
        when(streamingDs.fetchAllWithFallback()).thenReturn(
                CompletableFuture.completedFuture(fetchAllEntities));
        factory.registerDataSource(STREAMING_DS_NAME, streamingDs, Duration.ofMinutes(5));

        // 2. Create engine (constructor creates workerPool, scheduler, etc.)
        DataSynchronizationEngine engine = new DataSynchronizationEngine(factory);

        // 3. Create and populate DependencyGraph
        DependencyGraph dependencyGraph = new DependencyGraph();
        dependencyGraph.build(List.of()); // no mappings needed for this test
        if (!dgEntities.isEmpty()) {
            dependencyGraph.upsertAll(STREAMING_DS_NAME, dgEntities);
        }

        // 4. Create IncrementalSyncProcessor (needed by triggerGlobalSynchronization)
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, dependencyGraph, analysisResult, streamingVersion);

        // 5. Set up DataSourceSyncMetadata for the streaming datasource
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(
                STREAMING_DS_NAME, Duration.ofMinutes(5));
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(streamingState);

        // 6. Inject internal state via reflection (fields are private, test is in different package)
        setField(engine, "dependencyGraph", dependencyGraph);
        setField(engine, "incrementalSyncProcessor", processor);
        setField(engine, "cachedAnalysisResult", analysisResult);

        // Access dataSourceMetadata map via reflection
        Map<String, DataSourceSyncMetadata> metadataMap = getField(engine, "dataSourceMetadata");
        metadataMap.put(STREAMING_DS_NAME, metadata);

        // Mark datasource as pending for sync via reflection
        Set<String> pendingDs = getField(engine, "pendingDataSources");
        pendingDs.add(STREAMING_DS_NAME);

        return new EngineTestContext(engine, factory, dependencyGraph);
    }

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

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> dgEntities() {
        // Generate 50-100 entities for DependencyGraph (the "truth")
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 200),
                Arbitraries.integers().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
        ).as((id, val, name) -> new TestEntity(id, val, name));

        return entityArb.list().ofMinSize(50).ofMaxSize(100)
                .map(list -> {
                    // Deduplicate by ID — keep last occurrence
                    java.util.LinkedHashMap<Integer, TestEntity> byId = new java.util.LinkedHashMap<>();
                    for (TestEntity e : list) {
                        byId.put(e.getIdentity(), e);
                    }
                    return (List<TestEntity>) new ArrayList<>(byId.values());
                })
                .filter(list -> list.size() >= 10); // ensure meaningful size
    }

    @Provide
    Arbitrary<List<TestEntity>> fetchAllSubset() {
        // Generate 20-50 entities for fetchAll() — different/fewer than DG
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 200),
                Arbitraries.integers().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
        ).as((id, val, name) -> new TestEntity(id, val, name));

        return entityArb.list().ofMinSize(20).ofMaxSize(50)
                .map(list -> {
                    java.util.LinkedHashMap<Integer, TestEntity> byId = new java.util.LinkedHashMap<>();
                    for (TestEntity e : list) {
                        byId.put(e.getIdentity(), e);
                    }
                    return (List<TestEntity>) new ArrayList<>(byId.values());
                })
                .filter(list -> list.size() >= 5);
    }

    // ==================== Helper: Invoke triggerGlobalSynchronization via reflection ====================

    /**
     * Calls readAllDataSources() directly via reflection to test the streaming data
     * loading behavior without the full triggerGlobalSynchronization flow.
     * This avoids clearIntermediateData() which clears dataByDataSource after sync.
     */
    private DataVersion callReadAllDataSources(DataSynchronizationEngine engine) throws Exception {
        // Create a new DataVersion to populate
        DataVersion newVersion = new DataVersion(1, LocalDateTime.now());

        // Get pending datasources
        Set<String> pendingDs = getField(engine, "pendingDataSources");
        Set<String> dataSourcesNeedingSync = new java.util.HashSet<>(pendingDs);

        // Call readAllDataSources via reflection
        Method method = DataSynchronizationEngine.class.getDeclaredMethod(
                "readAllDataSources",
                DataVersion.class, java.util.Set.class);
        method.setAccessible(true);

        try {
            method.invoke(engine, newVersion, dataSourcesNeedingSync);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // readAllDataSources may throw on parallel read failure.
            // We still check the DataVersion state after the failure.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                // Expected: parallel phase failure — continue to check DataVersion
            } else {
                throw e;
            }
        }

        return newVersion;
    }

    // ==================== Property 1: READY + Different Data ====================

    /**
     * Bug Scenario 1: Streaming datasource READY, DependencyGraph has N entities,
     * fetchAll() returns M different entities (M != N).
     *
     * <p>Expected (correct) behavior: DataVersion should contain DependencyGraph data.</p>
     * <p>Actual (buggy) behavior: DataVersion contains fetchAll() data instead.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2</b></p>
     */
    @Property(tries = 10)
    void readyStateDataVersionShouldContainDependencyGraphDataNotFetchAllData(
            @ForAll("dgEntities") List<TestEntity> dgEntities,
            @ForAll("fetchAllSubset") List<TestEntity> fetchAllEntities) throws Exception {

        // Ensure DG and fetchAll return genuinely different data
        if (dgEntities.equals(fetchAllEntities)) return;

        EngineTestContext ctx = null;
        try {
            ctx = setupEngine(dgEntities, fetchAllEntities, StreamingDataSourceState.READY);

            // Call readAllDataSources directly — avoids clearIntermediateData() which clears dataByDataSource
            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            // Get the data written to DataVersion for the streaming datasource
            List<?> dvData = newVersion.getDataByDataSource(STREAMING_DS_NAME);

            // The DependencyGraph data (single source of truth)
            List<?> expectedData = ctx.dependencyGraph().findAll(STREAMING_DS_NAME);

            // PROPERTY: DataVersion streaming data MUST equal DependencyGraph data
            // BUG: This will FAIL because readAllDataSources uses fetchAll() instead of DG
            assertThat(dvData)
                    .as("Full sync READY: DataVersion should contain DependencyGraph data (%d entities) "
                                    + "but contains fetchAll() data instead. "
                                    + "Bug: readAllDataSources() uses fetchAll() instead of dependencyGraph.findAll()",
                            expectedData.size())
                    .isNotNull()
                    .hasSize(expectedData.size());

            // Compare element-by-element using toString to avoid wildcard type issues
            List<String> dvStrings = dvData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = expectedData.stream().map(Object::toString).sorted().toList();
            assertThat(dvStrings)
                    .as("DataVersion data should match DependencyGraph data exactly")
                    .isEqualTo(expectedStrings);

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 2: READY + Stale Data ====================

    /**
     * Bug Scenario 2: Streaming datasource READY, DependencyGraph has current entities,
     * fetchAll() returns stale/old versions of the same entities.
     *
     * <p>Expected (correct) behavior: DataVersion should contain DependencyGraph's current data.</p>
     * <p>Actual (buggy) behavior: DataVersion contains fetchAll()'s stale data.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2</b></p>
     */
    @Property(tries = 10)
    void readyStateDataVersionShouldContainCurrentDataNotStaleData(
            @ForAll("dgEntities") List<TestEntity> dgCurrentEntities) throws Exception {

        // Create stale versions: same IDs but different values (simulating old snapshot)
        List<TestEntity> staleEntities = new ArrayList<>();
        for (TestEntity current : dgCurrentEntities) {
            staleEntities.add(new TestEntity(current.getIdentity(), current.getValue() - 1, "stale-" + current.getName()));
        }

        EngineTestContext ctx = null;
        try {
            ctx = setupEngine(dgCurrentEntities, staleEntities, StreamingDataSourceState.READY);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            List<?> dvData = newVersion.getDataByDataSource(STREAMING_DS_NAME);
            List<?> expectedData = ctx.dependencyGraph().findAll(STREAMING_DS_NAME);

            // PROPERTY: DataVersion must have current (DG) data, not stale (fetchAll) data
            // BUG: This will FAIL because fetchAll() returns stale data and that's what gets written
            assertThat(dvData)
                    .as("Full sync READY+Stale: DataVersion should contain current DependencyGraph data, "
                            + "not stale fetchAll() data")
                    .isNotNull()
                    .hasSize(expectedData.size());

            List<String> dvStrings = dvData.stream().map(Object::toString).sorted().toList();
            List<String> expectedStrings = expectedData.stream().map(Object::toString).sorted().toList();
            assertThat(dvStrings)
                    .as("DataVersion data should match current DependencyGraph data, not stale fetchAll data")
                    .isEqualTo(expectedStrings);

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 3: INITIALIZING State ====================

    /**
     * Bug Scenario 3: Streaming datasource in INITIALIZING state,
     * DependencyGraph has partial accumulated data.
     *
     * <p>Expected (correct) behavior: DataVersion should contain DependencyGraph's partial data.</p>
     * <p>Actual (buggy) behavior: Streaming data is completely absent from DataVersion
     * because the code only handles READY state.</p>
     *
     * <p><b>Validates: Requirements 1.3</b></p>
     */
    @Property(tries = 10)
    void initializingStateDataVersionShouldContainPartialDependencyGraphData(
            @ForAll("dgEntities") List<TestEntity> dgPartialEntities) throws Exception {

        // fetchAll returns dummy — doesn't matter, code won't call it for non-READY
        List<TestEntity> dummyFetchAll = List.of(new TestEntity(9999, 0, "dummy"));

        EngineTestContext ctx = null;
        try {
            ctx = setupEngine(dgPartialEntities, dummyFetchAll, StreamingDataSourceState.INITIALIZING);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            List<?> dvData = newVersion.getDataByDataSource(STREAMING_DS_NAME);
            List<?> expectedData = ctx.dependencyGraph().findAll(STREAMING_DS_NAME);

            // PROPERTY: DataVersion should have DG's partial data even in INITIALIZING state
            // BUG: This will FAIL because readAllDataSources() only handles READY state,
            // completely skipping INITIALIZING streaming datasources
            assertThat(dvData)
                    .as("Full sync INITIALIZING: DataVersion should contain DependencyGraph's partial data "
                            + "(%d entities) but streaming data is completely absent. "
                            + "Bug: readAllDataSources() only processes READY streaming datasources",
                            expectedData.size())
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(expectedData.size());

        } finally {
            cleanup(ctx);
        }
    }

    // ==================== Property 4: ERROR State ====================

    /**
     * Bug Scenario 4: Streaming datasource in ERROR state,
     * DependencyGraph has last known good data.
     *
     * <p>Expected (correct) behavior: DataVersion should contain DependencyGraph's last known data.</p>
     * <p>Actual (buggy) behavior: Streaming data is completely absent from DataVersion
     * because the code only handles READY state.</p>
     *
     * <p><b>Validates: Requirements 1.3</b></p>
     */
    @Property(tries = 10)
    void errorStateDataVersionShouldContainLastKnownDependencyGraphData(
            @ForAll("dgEntities") List<TestEntity> dgLastKnownEntities) throws Exception {

        List<TestEntity> dummyFetchAll = List.of(new TestEntity(9999, 0, "dummy"));

        EngineTestContext ctx = null;
        try {
            ctx = setupEngine(dgLastKnownEntities, dummyFetchAll, StreamingDataSourceState.ERROR);

            DataVersion newVersion = callReadAllDataSources(ctx.engine());

            List<?> dvData = newVersion.getDataByDataSource(STREAMING_DS_NAME);
            List<?> expectedData = ctx.dependencyGraph().findAll(STREAMING_DS_NAME);

            // PROPERTY: DataVersion should have DG's last known data even in ERROR state
            // BUG: This will FAIL because readAllDataSources() only processes READY state,
            // completely skipping ERROR streaming datasources
            assertThat(dvData)
                    .as("Full sync ERROR: DataVersion should contain DependencyGraph's last known data "
                            + "(%d entities) but streaming data is completely absent. "
                            + "Bug: readAllDataSources() only processes READY streaming datasources",
                            expectedData.size())
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(expectedData.size());

        } finally {
            cleanup(ctx);
        }
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
}
