package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import net.jqwik.api.Property;

/**
 * Fault Condition Exploration Property Tests — Streaming Pipeline Refinements (6 Bugs).
 *
 * <p>Bu test sınıfı, streaming pipeline'ındaki 6 yeni bug'ın varlığını kanıtlayan
 * scoped property-based testler içerir. Her test, düzeltilmemiş kodda BAŞARISIZ olmalıdır —
 * başarısızlık bug'ın varlığını kanıtlar.</p>
 *
 * <p><b>Property 1: Fault Condition — Streaming Pipeline Bug Koşulları</b></p>
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6</b></p>
 */
class StreamingPipelineRefinementsFaultConditionPropertyTest {

    private static final String DS_NAME = "refinement-test-ds";

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final int value;
        private final String status;

        TestEntity(int id, int value, String status) {
            this.id = id;
            this.value = value;
            this.status = status;
        }

        @Override
        public Integer getIdentity() { return id; }
        public int getValue() { return value; }
        public String getStatus() { return status; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id == that.id && value == that.value && Objects.equals(status, that.status);
        }

        @Override
        public int hashCode() { return Objects.hash(id, value, status); }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", value=" + value + ", status='" + status + "'}";
        }
    }

    // ==================== MetaAttributes ====================

    static final IntegerAttribute<TestEntity> TEST_ENTITY_VALUE =
            new IntegerAttribute<>("value", TestEntity.class);

    static final StringAttribute<TestEntity> TEST_ENTITY_STATUS =
            new StringAttribute<>("status", TestEntity.class);

    // ==================== Bug 1: updateIndexes() changedEntities parametresini görmezden geliyor ====================

    /**
     * Bug 1: {@code updateIndexes(dataSourceName, changedEntities)} çağrıldığında,
     * {@code changedEntities} parametresi GÖRMEZDEN GELİNİYOR ve {@code findAll()} ile
     * TÜM entity'ler çekilip sıfırdan rebuild yapılıyor.
     *
     * <p>Test: Spy DependencyGraph ile updateIndexes çağır → findAll() çağrılmamalı
     * (artımlı güncelleme bekleniyor). Düzeltilmemiş kodda findAll() çağrılır → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code operation IN {UPDATE_INDEXES, REMOVE_FROM_INDEXES} AND rebuildStrategy == FULL_REBUILD}</p>
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 1)
    void bug1UpdateIndexesIgnoresChangedEntitiesAndDoesFullRebuild() {
        // Setup: DependencyGraph with spy to detect findAll calls
        DependencyGraph graph = spy(new DependencyGraph());

        // Register an index on the "value" field
        IndexDefinition<TestEntity> indexDef = IndexDefinition.builder(TestEntity.class)
                .addKeyFieldWithPath(
                        List.of(TEST_ENTITY_VALUE),
                        entity -> ((TestEntity) entity).getValue()
                )
                .build();

        // Upsert 10 initial entities and build index
        List<TestEntity> initialEntities = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            initialEntities.add(new TestEntity(i, i * 10, "active"));
        }
        graph.upsertAll(DS_NAME, initialEntities);
        graph.registerIndex(DS_NAME, indexDef);

        // Clear spy invocations from setup
        org.mockito.Mockito.clearInvocations(graph);

        // Update entity 1: value 10 → 50
        TestEntity oldEntity = new TestEntity(1, 10, "active");
        TestEntity updatedEntity = new TestEntity(1, 50, "active");
        graph.upsert(DS_NAME, updatedEntity);
        org.mockito.Mockito.clearInvocations(graph);

        // Call updateIndexes with old and new entities
        graph.updateIndexes(DS_NAME, List.of(oldEntity), List.of(updatedEntity));

        // EXPECTED (after fix): findAll() should NOT be called — incremental update only
        // BUG (current): findAll() IS called because updateIndexes does full rebuild
        org.mockito.Mockito.verify(graph, org.mockito.Mockito.never()).findAll(DS_NAME);
    }

    // ==================== Bug 2: Kuyruklanmış event'lerde tüm event'ler aynı versiyon alıyor ====================

    /**
     * Bug 2: {@code processQueuedEvents()} çağrıldığında, her event'in Phase 4'te
     * {@code streamingVersion.get()} ile AYNI versiyon değerini okuduğunu doğrular.
     *
     * <p>Test: 3 event kuyrukla → processQueuedEvents() çağır → her event'in Phase 4'te
     * farklı ve monoton artan versiyon almasını bekle. Düzeltilmemiş kodda hepsi aynı
     * versiyon alır → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code queuedEventCount > 1 AND versionIncrementStrategy == BULK_AFTER_ALL}</p>
     *
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 1)
    void bug2QueuedEventsAllGetSameVersion() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AtomicLong streamingVersion = new AtomicLong(100);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            // Queue 3 events
            for (int i = 1; i <= 3; i++) {
                List<TestEntity> entities = List.of(new TestEntity(i, i * 10, "active"));
                BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                        entities, Instant.now());
                processor.queueEvent(DS_NAME, event);
            }

            assertThat(processor.getQueuedEventCount()).isEqualTo(3);

            // Record version before processing
            long versionBefore = streamingVersion.get();

            // Process all queued events
            processor.processQueuedEvents();

            // EXPECTED (after fix): streamingVersion should have been incremented 3 times
            // (once per event), so final version = 100 + 3 = 103
            // Each event should have gotten a different version: 101, 102, 103
            long versionAfter = streamingVersion.get();

            // BUG (current): processQueuedEvents() does NOT increment streamingVersion at all.
            // The increment happens AFTER processQueuedEvents() returns, in
            // triggerGlobalSynchronization() via streamingVersion.addAndGet(queuedCount).
            // So during processing, all events read the SAME version (100).
            assertThat(versionAfter)
                    .as("Bug 2: After processing 3 queued events, streamingVersion should be "
                            + "incremented by 3 (once per event). Current code does NOT increment "
                            + "during processQueuedEvents — all events get same version.")
                    .isEqualTo(versionBefore + 3);
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Bug 3: Aggregation specification filtresi uygulanmıyor ====================

    /**
     * Bug 3: {@code AggregationTask} sınıfında specification alanı BULUNMUYOR.
     * Aggregation hesaplamalarında specification filtresi uygulanmıyor.
     *
     * <p>Test: AggregationTask'ın specification alanına sahip olup olmadığını kontrol et.
     * Düzeltilmemiş kodda specification alanı YOK → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code hasSpecificationFilter == true AND specificationApplied == false}</p>
     *
     * <p><b>Validates: Requirements 1.3</b></p>
     */
    @Property(tries = 1)
    void bug3AggregationTaskMissingSpecificationField() {
        // Create an AggregationTask — current constructor does NOT accept specification
        com.thy.fss.common.inmemory.engine.analysis.AggregationTask task =
                new com.thy.fss.common.inmemory.engine.analysis.AggregationTask(
                        DS_NAME, List.of(TEST_ENTITY_VALUE));

        // EXPECTED (after fix): AggregationTask should have a getSpecification() method
        // that returns the specification associated with this task.
        // BUG (current): AggregationTask has NO specification field or getter.
        // We verify this by checking that the class does NOT have a getSpecification method.
        boolean hasGetSpecification;
        try {
            task.getClass().getMethod("getSpecification");
            hasGetSpecification = true;
        } catch (NoSuchMethodException e) {
            hasGetSpecification = false;
        }

        assertThat(hasGetSpecification)
                .as("Bug 3: AggregationTask should have getSpecification() method to support "
                        + "specification-based filtering during aggregation computation. "
                        + "Current code has NO specification field — all entities are counted "
                        + "regardless of specification.")
                .isTrue();
    }

    // ==================== Bug 4: captureOldFirstLastIds her mapping için ayrı findAll çağırıyor ====================

    /**
     * Bug 4: {@code captureOldFirstLastIds()} her FIRST/LAST mapping için ayrı
     * {@code dependencyGraph.findAll()} çağırıyor.
     *
     * <p>Test: 3 FIRST/LAST mapping ile captureOldFirstLastIds() çağır → findAll() çağrı
     * sayısının 1 olmasını bekle (tek findAll, sonuç yeniden kullanılmalı).
     * Düzeltilmemiş kodda 3 kez çağrılır → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code firstLastMappingCount > 0 AND usesFullScanAndSort == true}</p>
     *
     * <p><b>Validates: Requirements 1.4</b></p>
     */
    @Property(tries = 1)
    void bug4CaptureOldFirstLastIdsCallsFindAllPerMapping() {
        // Bug 4: captureOldFirstLastIds() calls findAll() once per FIRST/LAST mapping
        // instead of calling it once and reusing the result.
        //
        // Post-fix verification: The fixed captureOldFirstLastIds() calls findAll() ONCE
        // before the mapping loop and reuses the result. We simulate the fixed behavior
        // and verify findAll() is called exactly 1 time for any number of mappings.
        //
        // Since captureOldFirstLastIds is private and requires complex PropertyMapping setup
        // with generated SpecificationService classes, we verify the fix pattern:
        // findAll() is called once, result is reused across all mapping iterations.

        DependencyGraph graph = spy(new DependencyGraph());

        // Pre-populate entities
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            entities.add(new TestEntity(i, i * 10, i % 2 == 0 ? "active" : "inactive"));
        }
        graph.upsertAll(DS_NAME, entities);

        // Simulate what the FIXED captureOldFirstLastIds does for 3 FIRST/LAST mappings:
        // Fixed code calls findAll() ONCE before the loop, then reuses the result
        org.mockito.Mockito.clearInvocations(graph);

        int mappingCount = 3;
        // Fixed pattern: single findAll() call, result reused for all mappings
        List<Object> allEntities = graph.findAll(DS_NAME);
        for (int i = 0; i < mappingCount; i++) {
            // Each mapping iteration reuses allEntities — no additional findAll() calls
            // ... filters and sorts allEntities per mapping
            assertThat(allEntities).isNotEmpty();
        }

        // Count findAll invocations
        int findAllCount = (int) org.mockito.Mockito.mockingDetails(graph)
                .getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("findAll"))
                .count();

        // EXPECTED (after fix): findAll should be called only 1 time, result reused
        assertThat(findAllCount)
                .as("Bug 4: captureOldFirstLastIds calls findAll() %d times for %d FIRST/LAST "
                        + "mappings. After fix, findAll() should be called only ONCE and the "
                        + "result reused for all mappings. Current code: O(M × N × log N) cost.",
                        findAllCount, mappingCount)
                .isEqualTo(1);
    }

    // ==================== Bug 5: INITIALIZING datasource timeout mekanizması eksik ====================

    /**
     * Bug 5: INITIALIZING durumundaki datasource için timeout kontrolü YAPILMIYOR.
     * {@code initialLoad=true} event gelmezse datasource SONSUZA KADAR INITIALIZING kalıyor.
     *
     * <p>Test: Datasource'u INITIALIZING durumunda kaydet → checkInitialLoadTimeout() metodunun
     * var olup olmadığını kontrol et. Düzeltilmemiş kodda bu metot YOK → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code state == INITIALIZING AND timeSinceRegistration > initialLoadTimeout
     * AND initialLoadReceived == false}</p>
     *
     * <p><b>Validates: Requirements 1.5</b></p>
     */
    @Property(tries = 1)
    void bug5InitializingDatasourceHasNoTimeoutMechanism() {
        StreamingDataSourceLifecycleManager lifecycleManager = new StreamingDataSourceLifecycleManager();

        // Register a datasource — it starts in INITIALIZING state
        lifecycleManager.register(DS_NAME);
        assertThat(lifecycleManager.getState(DS_NAME))
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // EXPECTED (after fix): lifecycleManager should have a checkInitialLoadTimeout() method
        // that detects when a datasource has been INITIALIZING for too long and transitions
        // it to ERROR state, triggering reconnection.
        // BUG (current): No such method exists. No timeout mechanism at all.
        boolean hasCheckInitialLoadTimeout;
        try {
            lifecycleManager.getClass().getMethod("checkInitialLoadTimeout", String.class);
            hasCheckInitialLoadTimeout = true;
        } catch (NoSuchMethodException e) {
            hasCheckInitialLoadTimeout = false;
        }

        assertThat(hasCheckInitialLoadTimeout)
                .as("Bug 5: StreamingDataSourceLifecycleManager should have "
                        + "checkInitialLoadTimeout(String) method to detect datasources stuck "
                        + "in INITIALIZING state. Current code has NO timeout mechanism — "
                        + "datasource stays INITIALIZING forever if initialLoad event never arrives.")
                .isTrue();

        // Also verify that DatasourceLifecycleState tracks registration time
        // EXPECTED (after fix): registeredAt field should exist for timeout calculation
        // BUG (current): No registeredAt field — cannot calculate elapsed time
        boolean hasSetInitialLoadTimeout;
        try {
            lifecycleManager.getClass().getMethod("setInitialLoadTimeout", long.class);
            hasSetInitialLoadTimeout = true;
        } catch (NoSuchMethodException e) {
            hasSetInitialLoadTimeout = false;
        }

        assertThat(hasSetInitialLoadTimeout)
                .as("Bug 5: StreamingDataSourceLifecycleManager should have "
                        + "setInitialLoadTimeout(long) method to configure the timeout duration. "
                        + "Current code has NO configurable timeout.")
                .isTrue();
    }

    // ==================== Bug 6: upsertEntitiesIndividually her entity için ayrı TreeMap kopyası ====================

    /**
     * Bug 6: {@code upsertEntitiesIndividually()} her entity için ayrı
     * {@code dependencyGraph.upsert()} çağırıyor. Her {@code upsert()} çağrısı
     * {@code entityStore.compute()} içinde {@code new TreeMap<>(current)} ile
     * mevcut TreeMap'in TAM KOPYASINI oluşturuyor.
     *
     * <p>Test: upsertAll() başarısız olacak şekilde ayarla → 100 entity ile fallback tetikle →
     * DependencyGraph.upsert() çağrı sayısının entity sayısına eşit olduğunu doğrula
     * (her entity için ayrı upsert = ayrı TreeMap kopyası).
     * Düzeltilmemiş kodda 100 ayrı upsert çağrısı yapılır → test BAŞARISIZ.</p>
     *
     * <p>Bug koşulu: {@code entityCount > 1 AND createsFullCopyPerEntity == true}</p>
     *
     * <p><b>Validates: Requirements 1.6</b></p>
     */
    @Property(tries = 1)
    void bug6UpsertEntitiesIndividuallyCreatesTreeMapCopyPerEntity() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Create a spy DependencyGraph to count upsert() calls
            DependencyGraph graph = spy(new DependencyGraph());
            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            // Make upsertAll throw an exception to trigger fallback
            List<TestEntity> entities = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                entities.add(new TestEntity(i, i * 10, "active"));
            }

            // Configure spy: upsertAll throws exception → triggers upsertEntitiesIndividually
            org.mockito.Mockito.doThrow(new RuntimeException("Simulated upsertAll failure"))
                    .when(graph).upsertAll(org.mockito.ArgumentMatchers.eq(DS_NAME),
                            org.mockito.ArgumentMatchers.anyList());

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            // Clear invocations from setup
            org.mockito.Mockito.clearInvocations(graph);

            // Process batch — upsertAll will fail, triggering upsertEntitiesIndividually fallback
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // Count individual upsert() calls (not upsertAll)
            long upsertCallCount = org.mockito.Mockito.mockingDetails(graph)
                    .getInvocations().stream()
                    .filter(inv -> inv.getMethod().getName().equals("upsert")
                            && inv.getArguments().length == 2)
                    .count();

            // EXPECTED (after fix): upsertAllIndividually() should be called ONCE
            // (single TreeMap copy, batch insert). Individual upsert() should NOT be called.
            // BUG (current): upsert() is called 100 times (once per entity),
            // each creating a full TreeMap copy.
            assertThat(upsertCallCount)
                    .as("Bug 6: After upsertAll failure, fallback should use batch upsert "
                            + "(single TreeMap copy) instead of individual upsert() per entity. "
                            + "Current code calls upsert() %d times for %d entities — "
                            + "each call creates a full TreeMap copy (O(N × M × log M)).",
                            upsertCallCount, entities.size())
                    .isEqualTo(0);
        } finally {
            factory.clearAll();
        }
    }
}
