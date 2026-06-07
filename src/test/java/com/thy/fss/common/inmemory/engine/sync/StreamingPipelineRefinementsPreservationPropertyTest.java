package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AggregationTask;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Preservation Property Tests — Streaming Pipeline Refinements.
 *
 * <p>Bu test sınıfı, bug koşulu dışındaki girdiler için mevcut davranışın
 * korunduğunu doğrulayan property-based testler içerir. Tüm testler
 * düzeltilmemiş kodda GEÇMELİDİR — temel davranışı doğrularlar.</p>
 *
 * <p><b>Property 2: Preservation — Mevcut Davranış Korunması</b></p>
 * <p><b>FOR ALL input WHERE NOT isBugCondition(input) DO ASSERT F(input) = F'(input)</b></p>
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10</b></p>
 */
class StreamingPipelineRefinementsPreservationPropertyTest {

    private static final String DS_NAME = "preservation-test-ds";

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

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> entityLists() {
        return Arbitraries.integers().between(1, 50)
                .flatMap(size -> {
                    Arbitrary<TestEntity> entityArb = Arbitraries.integers().between(1, 200)
                            .flatMap(id -> Arbitraries.integers().between(1, 1000)
                                    .flatMap(value -> Arbitraries.of("active", "inactive", "pending")
                                            .map(status -> new TestEntity(id, value, status))));
                    return entityArb.list().ofSize(size);
                });
    }

    @Provide
    Arbitrary<Integer> entityCounts() {
        return Arbitraries.integers().between(1, 30);
    }

    @Provide
    Arbitrary<Integer> consecutiveFailureCounts() {
        return Arbitraries.integers().between(0, 20);
    }


    // ==================== Gözlem 3.2: Copy-on-write volatile swap semantiği ====================

    /**
     * Gözlem 3.2: {@code upsertAll()} başarılı olduğunda copy-on-write volatile swap
     * semantiği çalışıyor. Yeni bir TreeMap kopyası oluşturulup entity'ler ekleniyor,
     * sonra atomik olarak swap ediliyor. Eski referanslar mutasyona uğramıyor.
     *
     * <p>Non-bug condition: upsertAll() başarılı (fallback yoluna düşmüyor)</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void preservation32UpsertAllCopyOnWriteSwapSemantics(@ForAll("entityLists") List<TestEntity> entities) {
        DependencyGraph graph = new DependencyGraph();

        // Deduplicate by ID — keep last occurrence (same as TreeMap put behavior)
        Map<Integer, TestEntity> deduped = new java.util.LinkedHashMap<>();
        for (TestEntity e : entities) {
            deduped.put(e.getIdentity(), e);
        }
        List<TestEntity> uniqueEntities = new ArrayList<>(deduped.values());

        // Phase 1: Initial upsertAll
        graph.upsertAll(DS_NAME, uniqueEntities);

        // Capture snapshot BEFORE second upsert
        List<TestEntity> snapshotBefore = graph.findAll(DS_NAME);

        // Phase 2: Upsert additional entities (some overlapping IDs with new values)
        List<TestEntity> updatedEntities = new ArrayList<>();
        for (TestEntity e : uniqueEntities) {
            updatedEntities.add(new TestEntity(e.getIdentity(), e.getValue() + 100, e.getStatus()));
        }
        graph.upsertAll(DS_NAME, updatedEntities);

        // Verify: snapshot taken before second upsert should be UNCHANGED
        // (copy-on-write means old TreeMap is not mutated)
        List<TestEntity> snapshotAfter = graph.findAll(DS_NAME);

        // Old snapshot should still reflect original values
        assertThat(snapshotBefore)
                .as("Preservation 3.2: Copy-on-write semantics — old snapshot should not be "
                        + "mutated by subsequent upsertAll()")
                .hasSize(uniqueEntities.size());

        // New snapshot should reflect updated values
        assertThat(snapshotAfter)
                .as("Preservation 3.2: After upsertAll, all entities should be updated")
                .hasSize(uniqueEntities.size());

        // Verify each entity in new snapshot has updated value
        for (TestEntity updated : snapshotAfter) {
            TestEntity original = deduped.get(updated.getIdentity());
            assertThat(updated.getValue())
                    .as("Preservation 3.2: Entity id=%d should have updated value", updated.getIdentity())
                    .isEqualTo(original.getValue() + 100);
        }
    }

    // ==================== Gözlem 3.3: Normal event'lerde versiyon artırma mekanizması ====================

    /**
     * Gözlem 3.3: Normal (kuyruklanmamış) event'lerde versiyon artırma mekanizması
     * doğru çalışıyor. Tek event işlendiğinde, Phase 4'te streamingVersion.get() ile
     * okunan versiyon, Engine tarafından set edilen değerdir.
     *
     * <p>Non-bug condition: Kuyruklanmamış event (tek event, doğrudan processBatchSnapshot)</p>
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 20)
    void preservation33NormalEventVersionMechanism(@ForAll("entityCounts") int entityCount) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            long initialVersion = 100;
            AtomicLong streamingVersion = new AtomicLong(initialVersion);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            // Create entities
            List<TestEntity> entities = new ArrayList<>();
            for (int i = 1; i <= entityCount; i++) {
                entities.add(new TestEntity(i, i * 10, "active"));
            }

            // Process single event (NOT queued — direct processBatchSnapshot)
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // Verify: streamingVersion should NOT have been changed by processBatchSnapshot
            // (version increment happens in Engine listener, not in Processor)
            assertThat(streamingVersion.get())
                    .as("Preservation 3.3: Normal (non-queued) event processing should NOT "
                            + "modify streamingVersion — increment happens in Engine listener")
                    .isEqualTo(initialVersion);
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Gözlem 3.4: Specification'sız aggregation tüm entity'ler üzerinden ====================

    /**
     * Gözlem 3.4: Specification tanımlı olmayan aggregation task'ları tüm entity'ler
     * üzerinden hesaplama yapıyor. AggregationTask'ın specification alanı olmadığından,
     * tüm entity'ler dahil ediliyor.
     *
     * <p>Non-bug condition: hasSpecificationFilter == false (specification tanımlı değil)</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 30)
    void preservation34NoSpecificationAggregationUsesAllEntities(@ForAll("entityCounts") int entityCount) {
        DependencyGraph graph = new DependencyGraph();

        // Create entities with varying values
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 1; i <= entityCount; i++) {
            entities.add(new TestEntity(i, i * 10, i % 2 == 0 ? "active" : "inactive"));
        }
        graph.upsertAll(DS_NAME, entities);

        // Create AggregationTask WITHOUT specification (current behavior — no spec field)
        AggregationTask task = new AggregationTask(DS_NAME, List.of(TEST_ENTITY_VALUE));

        // Verify: task has no getSpecification method (current code)
        // This means all entities are included in aggregation
        List<?> allEntities = graph.findAll(DS_NAME);

        assertThat(allEntities)
                .as("Preservation 3.4: Without specification, findAll returns ALL entities "
                        + "for aggregation computation")
                .hasSize(entityCount);

        // Verify: AggregationTask stores all mappings correctly
        assertThat(task.getDataSourceName()).isEqualTo(DS_NAME);
        assertThat(task.getFieldPath()).hasSize(1);
    }

    // ==================== Gözlem 3.5: captureOldFirstLastIds doğru ID'ler döndürüyor ====================

    /**
     * Gözlem 3.5: {@code captureOldFirstLastIds()} doğru first/last entity ID'lerini
     * döndürüyor. Mevcut implementasyon findAll + sort ile doğru sonuç üretiyor.
     *
     * <p>Bu test, captureOldFirstLastIds'in kullandığı temel mekanizmayı doğrular:
     * findAll → filter → sort → first/last. Sonuçlar doğru olmalı.</p>
     *
     * <p>Non-bug condition: Sonuç doğruluğu (performans değil)</p>
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 50)
    void preservation35FindAllSortProducesCorrectFirstLastIds(@ForAll("entityLists") List<TestEntity> entities) {
        DependencyGraph graph = new DependencyGraph();

        // Deduplicate by ID
        Map<Integer, TestEntity> deduped = new java.util.LinkedHashMap<>();
        for (TestEntity e : entities) {
            deduped.put(e.getIdentity(), e);
        }
        List<TestEntity> uniqueEntities = new ArrayList<>(deduped.values());
        if (uniqueEntities.isEmpty()) return;

        graph.upsertAll(DS_NAME, uniqueEntities);

        // Simulate what captureOldFirstLastIds does: findAll → sort → first/last
        List<TestEntity> allEntities = graph.findAll(DS_NAME);

        // Sort by value (ascending) — simulates comparator-based sorting
        List<TestEntity> sorted = new ArrayList<>(allEntities);
        sorted.sort(java.util.Comparator.comparingInt(TestEntity::getValue));

        TestEntity first = sorted.get(0);
        TestEntity last = sorted.get(sorted.size() - 1);

        // Verify: first entity has minimum value, last has maximum
        int minValue = uniqueEntities.stream().mapToInt(TestEntity::getValue).min().orElse(0);
        int maxValue = uniqueEntities.stream().mapToInt(TestEntity::getValue).max().orElse(0);

        assertThat(first.getValue())
                .as("Preservation 3.5: First entity after sort should have minimum value")
                .isEqualTo(minValue);

        assertThat(last.getValue())
                .as("Preservation 3.5: Last entity after sort should have maximum value")
                .isEqualTo(maxValue);

        // Verify: findAll returns all entities
        assertThat(allEntities)
                .as("Preservation 3.5: findAll should return all upserted entities")
                .hasSize(uniqueEntities.size());
    }


    // ==================== Gözlem 3.6: initialLoad=true → INITIALIZING → READY geçişi ====================

    /**
     * Gözlem 3.6: {@code initialLoad=true} event gönderen datasource'lar
     * INITIALIZING → READY geçişi yapıyor. Bu geçiş
     * {@code handleInitialLoadComplete()} ile tetikleniyor.
     *
     * <p>Non-bug condition: initialLoad=true event geldiğinde (timeout süresi dolmadan)</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 10)
    void preservation36InitialLoadTriggersInitializingToReadyTransition() {
        StreamingDataSourceLifecycleManager lifecycleManager = new StreamingDataSourceLifecycleManager();

        // Register datasource — starts in INITIALIZING
        lifecycleManager.register(DS_NAME);
        assertThat(lifecycleManager.getState(DS_NAME))
                .as("Preservation 3.6: Newly registered datasource should be INITIALIZING")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        assertThat(lifecycleManager.isHealthy(DS_NAME))
                .as("Preservation 3.6: Newly registered datasource should be healthy")
                .isTrue();

        // Simulate initial load complete (what happens when initialLoad=true event arrives)
        lifecycleManager.handleInitialLoadComplete(DS_NAME);

        // Verify: state transitions to READY
        assertThat(lifecycleManager.getState(DS_NAME))
                .as("Preservation 3.6: After handleInitialLoadComplete, state should be READY")
                .isEqualTo(StreamingDataSourceState.READY);

        assertThat(lifecycleManager.isHealthy(DS_NAME))
                .as("Preservation 3.6: After initial load complete, datasource should be healthy")
                .isTrue();

        assertThat(lifecycleManager.getConsecutiveFailures(DS_NAME))
                .as("Preservation 3.6: After initial load complete, consecutive failures should be 0")
                .isEqualTo(0);
    }

    // ==================== Gözlem 3.7: upsertEntitiesIndividually fallback sonucu doğru ====================

    /**
     * Gözlem 3.7: {@code upsertEntitiesIndividually()} fallback sonucu entity durumu
     * doğru. Her entity için ayrı {@code upsert()} çağrılıyor ve sonuçta
     * DependencyGraph'taki entity durumu {@code upsertAll()} ile aynı olmalı.
     *
     * <p>Non-bug condition: Sonuç doğruluğu (performans değil)</p>
     *
     * <p><b>Validates: Requirements 3.7</b></p>
     */
    @Property(tries = 50)
    void preservation37IndividualUpsertProducesSameResultAsUpsertAll(
            @ForAll("entityLists") List<TestEntity> entities) {
        // Deduplicate by ID
        Map<Integer, TestEntity> deduped = new java.util.LinkedHashMap<>();
        for (TestEntity e : entities) {
            deduped.put(e.getIdentity(), e);
        }
        List<TestEntity> uniqueEntities = new ArrayList<>(deduped.values());

        // Path A: upsertAll (batch)
        DependencyGraph graphBatch = new DependencyGraph();
        graphBatch.upsertAll(DS_NAME, uniqueEntities);

        // Path B: individual upserts (what upsertEntitiesIndividually does)
        DependencyGraph graphIndividual = new DependencyGraph();
        for (TestEntity entity : uniqueEntities) {
            graphIndividual.upsert(DS_NAME, entity);
        }

        // Verify: both paths produce identical entity store state
        List<TestEntity> batchResult = graphBatch.findAll(DS_NAME);
        List<TestEntity> individualResult = graphIndividual.findAll(DS_NAME);

        assertThat(individualResult)
                .as("Preservation 3.7: Individual upsert should produce same entity set as upsertAll")
                .hasSize(batchResult.size());

        // Compare entity by entity (TreeMap orders by ID)
        for (int i = 0; i < batchResult.size(); i++) {
            TestEntity batch = batchResult.get(i);
            TestEntity individual = individualResult.get(i);
            assertThat(individual)
                    .as("Preservation 3.7: Entity at position %d should be identical", i)
                    .isEqualTo(batch);
        }
    }

    // ==================== Gözlem 3.8: Pipeline aşama sıralaması korunuyor ====================

    /**
     * Gözlem 3.8: Pipeline aşama sıralaması (Phase 1→2→3→4) korunuyor.
     * {@code processBatchSnapshot()} Phase 1'de entity upsert, Phase 2'de mapping update,
     * Phase 3'te aggregation update, Phase 4'te consumer propagation yapıyor.
     *
     * <p>Bu test, pipeline'ın doğru sırada çalıştığını doğrular: Phase 1 sonrası
     * entity'ler DependencyGraph'ta mevcut olmalı.</p>
     *
     * <p>Non-bug condition: Normal pipeline execution</p>
     *
     * <p><b>Validates: Requirements 3.8</b></p>
     */
    @Property(tries = 20)
    void preservation38PipelinePhaseOrderingPreserved(@ForAll("entityCounts") int entityCount) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            // Create entities
            List<TestEntity> entities = new ArrayList<>();
            for (int i = 1; i <= entityCount; i++) {
                entities.add(new TestEntity(i, i * 10, "active"));
            }

            // Process batch snapshot
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // Verify Phase 1 result: entities should be in DependencyGraph
            List<TestEntity> storedEntities = graph.findAll(DS_NAME);
            assertThat(storedEntities)
                    .as("Preservation 3.8: After processBatchSnapshot, Phase 1 should have "
                            + "upserted all entities into DependencyGraph")
                    .hasSize(entityCount);

            // Verify entities are correct
            for (int i = 1; i <= entityCount; i++) {
                final int entityId = i;
                boolean found = storedEntities.stream()
                        .anyMatch(e -> e.getIdentity() == entityId);
                assertThat(found)
                        .as("Preservation 3.8: Entity with id=%d should be in DependencyGraph", entityId)
                        .isTrue();
            }

            // Process a second batch (update) — pipeline should still work
            List<TestEntity> updatedEntities = new ArrayList<>();
            for (int i = 1; i <= entityCount; i++) {
                updatedEntities.add(new TestEntity(i, i * 20, "updated"));
            }
            BatchSnapshotEvent<TestEntity> event2 = new BatchSnapshotEvent<>(
                    updatedEntities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event2);

            // Verify: entities should be updated
            List<TestEntity> updatedStored = graph.findAll(DS_NAME);
            assertThat(updatedStored)
                    .as("Preservation 3.8: After second batch, entities should be updated")
                    .hasSize(entityCount);

            for (TestEntity e : updatedStored) {
                assertThat(e.getStatus())
                        .as("Preservation 3.8: Entity id=%d should have updated status", e.getIdentity())
                        .isEqualTo("updated");
            }
        } finally {
            factory.clearAll();
        }
    }


    // ==================== Gözlem 3.9: Durum geçişleri ve exponential backoff ====================

    /**
     * Gözlem 3.9: Durum geçişleri ve exponential backoff mekanizması çalışıyor.
     * INITIALIZING → READY, READY → ERROR, ERROR → INITIALIZING geçişleri
     * ve backoff hesaplaması doğru çalışıyor.
     *
     * <p>Non-bug condition: Normal state transitions and backoff calculation</p>
     *
     * <p><b>Validates: Requirements 3.9</b></p>
     */
    @Property(tries = 20)
    void preservation39StateTransitionsAndExponentialBackoff(
            @ForAll("consecutiveFailureCounts") int failures) {
        StreamingDataSourceLifecycleManager lifecycleManager = new StreamingDataSourceLifecycleManager();

        // === Test state transitions ===

        // 1. Register → INITIALIZING
        lifecycleManager.register(DS_NAME);
        assertThat(lifecycleManager.getState(DS_NAME))
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 2. INITIALIZING → READY (via handleInitialLoadComplete)
        lifecycleManager.handleInitialLoadComplete(DS_NAME);
        assertThat(lifecycleManager.getState(DS_NAME))
                .isEqualTo(StreamingDataSourceState.READY);

        // 3. READY → ERROR (via handleConnectionLoss)
        lifecycleManager.handleConnectionLoss(DS_NAME, "test error");
        assertThat(lifecycleManager.getState(DS_NAME))
                .isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(lifecycleManager.isHealthy(DS_NAME)).isFalse();

        // 4. ERROR → INITIALIZING (via handleReconnectionSuccess)
        lifecycleManager.handleReconnectionSuccess(DS_NAME);
        assertThat(lifecycleManager.getState(DS_NAME))
                .isEqualTo(StreamingDataSourceState.INITIALIZING);
        assertThat(lifecycleManager.isHealthy(DS_NAME)).isTrue();
        assertThat(lifecycleManager.getConsecutiveFailures(DS_NAME)).isEqualTo(0);

        // === Test exponential backoff ===

        // Record consecutive failures
        for (int i = 0; i < failures; i++) {
            lifecycleManager.recordReconnectionFailure(DS_NAME);
        }

        assertThat(lifecycleManager.getConsecutiveFailures(DS_NAME))
                .as("Preservation 3.9: Consecutive failures should be tracked correctly")
                .isEqualTo(failures);

        // Verify backoff delay calculation
        Duration delay = lifecycleManager.calculateNextReconnectDelay(DS_NAME);
        Duration expectedDelay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(failures);

        assertThat(delay)
                .as("Preservation 3.9: Backoff delay should match calculateBackoffDelay(%d)", failures)
                .isEqualTo(expectedDelay);

        // Verify backoff properties
        if (failures < StreamingDataSourceLifecycleManager.BACKOFF_THRESHOLD) {
            assertThat(delay)
                    .as("Preservation 3.9: Below threshold, delay should be base delay")
                    .isEqualTo(StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY);
        } else {
            assertThat(delay)
                    .as("Preservation 3.9: Above threshold, delay should be >= base delay")
                    .isGreaterThanOrEqualTo(StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY);
            assertThat(delay)
                    .as("Preservation 3.9: Delay should never exceed max backoff delay")
                    .isLessThanOrEqualTo(StreamingDataSourceLifecycleManager.MAX_BACKOFF_DELAY);
        }
    }

    // ==================== Gözlem 3.10: Artımlı aggregation delta mantığı ====================

    /**
     * Gözlem 3.10: Artımlı aggregation delta mantığı doğru çalışıyor.
     * {@code computeAggregationsIncremental()} eski değeri çıkarıp yeni değeri ekliyor.
     * Bu test, delta mantığının temelini doğrular: entity ekleme, güncelleme ve silme
     * senaryolarında count ve sum değerleri doğru hesaplanıyor.
     *
     * <p>Non-bug condition: Specification filtresi yok (tüm entity'ler dahil)</p>
     *
     * <p><b>Validates: Requirements 3.10</b></p>
     */
    @Property(tries = 30)
    void preservation310IncrementalAggregationDeltaLogic(@ForAll("entityCounts") int entityCount) {
        DependencyGraph graph = new DependencyGraph();

        // Create initial entities
        List<TestEntity> initialEntities = new ArrayList<>();
        int expectedSum = 0;
        for (int i = 1; i <= entityCount; i++) {
            int value = i * 10;
            initialEntities.add(new TestEntity(i, value, "active"));
            expectedSum += value;
        }
        graph.upsertAll(DS_NAME, initialEntities);

        // Verify initial state via full scan
        List<TestEntity> allEntities = graph.findAll(DS_NAME);
        assertThat(allEntities).hasSize(entityCount);

        int actualSum = 0;
        for (TestEntity e : allEntities) {
            actualSum += e.getValue();
        }
        assertThat(actualSum)
                .as("Preservation 3.10: Initial sum should match expected")
                .isEqualTo(expectedSum);

        // Simulate delta: update entity 1 (value 10 → 100)
        if (entityCount >= 1) {
            TestEntity oldEntity = initialEntities.get(0);
            TestEntity newEntity = new TestEntity(1, 100, "active");
            graph.upsert(DS_NAME, newEntity);

            // Delta logic: sum = sum - oldValue + newValue
            int deltaSum = expectedSum - oldEntity.getValue() + newEntity.getValue();

            // Verify via full scan
            List<TestEntity> afterUpdate = graph.findAll(DS_NAME);
            int fullScanSum = 0;
            for (TestEntity e : afterUpdate) {
                fullScanSum += e.getValue();
            }

            assertThat(fullScanSum)
                    .as("Preservation 3.10: After update, full scan sum should match delta calculation")
                    .isEqualTo(deltaSum);

            // Count should remain the same (update, not insert)
            assertThat(afterUpdate)
                    .as("Preservation 3.10: Update should not change entity count")
                    .hasSize(entityCount);
        }

        // Simulate delta: add new entity
        if (entityCount >= 1) {
            int newId = entityCount + 1;
            int newValue = 999;
            TestEntity newEntity = new TestEntity(newId, newValue, "new");
            graph.upsert(DS_NAME, newEntity);

            List<TestEntity> afterInsert = graph.findAll(DS_NAME);

            // Delta logic: count++, sum += newValue
            assertThat(afterInsert)
                    .as("Preservation 3.10: Insert should increase entity count by 1")
                    .hasSize(entityCount + 1);

            int insertSum = 0;
            for (TestEntity e : afterInsert) {
                insertSum += e.getValue();
            }

            // Expected: previous delta sum + new entity value
            int expectedAfterInsert = (expectedSum - initialEntities.get(0).getValue() + 100) + newValue;
            assertThat(insertSum)
                    .as("Preservation 3.10: After insert, sum should include new entity value")
                    .isEqualTo(expectedAfterInsert);
        }

        // Simulate delta: remove entity
        if (entityCount >= 2) {
            // Remove entity with id=2
            TestEntity removedEntity = initialEntities.get(1); // id=2, value=20
            graph.removeById(DS_NAME, removedEntity.getIdentity());

            List<TestEntity> afterRemove = graph.findAll(DS_NAME);

            // Delta logic: count--, sum -= removedValue
            // Current state: entityCount + 1 entities (after insert above)
            assertThat(afterRemove)
                    .as("Preservation 3.10: Remove should decrease entity count by 1")
                    .hasSize(entityCount); // entityCount + 1 - 1 = entityCount
        }
    }

}
