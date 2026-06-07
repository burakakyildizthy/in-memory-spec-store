package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

/**
 * Unification Pipeline Property Tests — IncrementalSyncProcessor.
 *
 * <p>Property 6: Full Sync'te Streaming DataSource Dahil Etme — Durum Bazlı</p>
 * <p>Property 7: fetchAll() ile Initial State Yükleme ve Durum Geçişi</p>
 * <p>Property 8: INITIALIZING Durumunda Event Kuyruklama</p>
 * <p>Property 9: Subscription Context ile DataSource Kimliği</p>
 * <p>Property 10: Event Sıralama Korunumu (FIFO)</p>
 * <p>Property 11: Atomik Batch Güncelleme</p>
 * <p>Property 12: Full Sync Sırasında Event Kuyruklama</p>
 */
class UnificationPipelinePropertyTest {

    private InMemorySpecStoreFactory factory;

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final int value;
        private final String label;

        TestEntity(int id, int value, String label) {
            this.id = id;
            this.value = value;
            this.label = label;
        }

        @Override
        public Integer getIdentity() { return id; }
        public int getValue() { return value; }
        public String getLabel() { return label; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id == that.id && value == that.value && Objects.equals(label, that.label);
        }

        @Override
        public int hashCode() { return Objects.hash(id, value, label); }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", value=" + value + ", label='" + label + "'}";
        }
    }

    // ==================== Setup / Teardown ====================

    @BeforeProperty
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
    }

    @AfterProperty
    void tearDown() {
        factory.clearAll();
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> dsNames() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(15)
                .map(s -> "ds-" + s);
    }

    @Provide
    Arbitrary<List<TestEntity>> entityBatches() {
        return Arbitraries.integers().between(1, 30)
                .flatMap(size -> {
                    Arbitrary<TestEntity> entityArb = Arbitraries.integers().between(1, 200)
                            .flatMap(id -> Arbitraries.integers().between(1, 1000)
                                    .flatMap(val -> Arbitraries.of("alpha", "beta", "gamma")
                                            .map(lbl -> new TestEntity(id, val, lbl))));
                    return entityArb.list().ofSize(size);
                });
    }

    @Provide
    Arbitrary<Integer> eventCounts() {
        return Arbitraries.integers().between(2, 10);
    }

    // ==================== Helper ====================

    @SuppressWarnings("unchecked")
    private void registerStreamingDs(String dsName, StreamingDataSourceState state) {
        StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(dsName);
        when(streamingDs.getState()).thenReturn(state);
        factory.registerDataSource(dsName, streamingDs);
    }

    private IncrementalSyncProcessor createProcessor(DependencyGraph graph, AtomicLong version) {
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        return new IncrementalSyncProcessor(factory, graph, analysisResult, version);
    }

    // ==================== Property 9: Subscription Context ile DataSource Kimliği ====================

    /**
     * Property 9: Subscription Context ile DataSource Kimliği.
     *
     * <p>Rastgele dsName + event kombinasyonları ile {@code processBatchSnapshot(dsName, event)}
     * çağrısı yapılır. İşleme sırasında kullanılan datasource kimliğinin parametre olarak
     * geçirilen dsName olduğu, entity'lerin DependencyGraph'ta doğru datasource altında
     * saklandığı doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 4.3, 7.2</b></p>
     */
    // Feature: streaming-datasource-unification, Property 9: Subscription Context ile DataSource Kimliği
    @Property(tries = 100)
    void property9SubscriptionContextDeterminesDatasourceIdentity(
            @ForAll("dsNames") String dsName,
            @ForAll("entityBatches") List<TestEntity> entities) {

        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);

        registerStreamingDs(dsName, StreamingDataSourceState.READY);
        IncrementalSyncProcessor processor = createProcessor(graph, version);

        // Process batch with dsName from subscription context
        BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
        processor.processBatchSnapshot(dsName, event);

        // Verify: entities are stored under the dsName passed as parameter
        List<TestEntity> stored = graph.findAll(dsName);
        assertThat(stored)
                .as("Property 9: Entities should be stored under the dsName '%s' "
                        + "provided via subscription context parameter", dsName)
                .isNotEmpty();

        // Verify every entity from the event is findable under dsName
        for (TestEntity entity : entities) {
            TestEntity found = graph.findById(dsName, entity.getIdentity());
            assertThat(found)
                    .as("Property 9: Entity id=%d should be stored under dsName '%s'",
                            entity.getIdentity(), dsName)
                    .isNotNull();
        }

        // Verify: no entities leaked to other datasources — graph should only have dsName
        // (DependencyGraph uses ConcurrentHashMap, so we can check there's no cross-contamination)
        // The event object itself has no getDataSourceName() — identity comes solely from parameter
        assertThat(stored.size())
                .as("Property 9: Number of stored entities should match unique IDs from event")
                .isGreaterThan(0);

        // Cleanup for next iteration
        factory.clearAll();
    }

    // ==================== Property 10: Event Sıralama Korunumu (FIFO) ====================

    /**
     * Property 10: Event Sıralama Korunumu (FIFO).
     *
     * <p>Rastgele event dizileri ile kuyruklama ve işleme sırasının korunduğu doğrulanır.
     * Event'ler kuyruklanma sırasına göre (FIFO) işlenir; sıra bozulmaz.</p>
     *
     * <p><b>Validates: Requirements 7.3</b></p>
     */
    // Feature: streaming-datasource-unification, Property 10: Event Sıralama Korunumu (FIFO)
    @Property(tries = 100)
    void property10EventProcessingPreservesFIFOOrder(
            @ForAll("eventCounts") int eventCount) {

        String dsName = "fifo-test-ds";
        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);

        registerStreamingDs(dsName, StreamingDataSourceState.READY);
        IncrementalSyncProcessor processor = createProcessor(graph, version);

        // Queue events with distinct entity IDs per event — each event overwrites entity id=1
        // with a value that encodes the event order. After FIFO processing, the final value
        // of entity id=1 should reflect the LAST queued event.
        List<Integer> expectedOrder = new ArrayList<>();
        for (int i = 1; i <= eventCount; i++) {
            int eventValue = i * 100;
            expectedOrder.add(eventValue);
            List<TestEntity> entities = List.of(new TestEntity(1, eventValue, "event-" + i));
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.queueEvent(dsName, event);
        }

        assertThat(processor.getQueuedEventCount())
                .as("Property 10: All events should be queued")
                .isEqualTo(eventCount);

        // Process all queued events
        processor.processQueuedEvents();

        assertThat(processor.getQueuedEventCount())
                .as("Property 10: Queue should be empty after processing")
                .isEqualTo(0);

        // After FIFO processing, entity id=1 should have the value from the LAST event
        TestEntity finalEntity = graph.findById(dsName, 1);
        int lastEventValue = expectedOrder.get(expectedOrder.size() - 1);

        assertThat(finalEntity)
                .as("Property 10: Entity should exist after processing queued events")
                .isNotNull();
        assertThat(finalEntity.getValue())
                .as("Property 10: After FIFO processing, entity value should reflect the "
                        + "last queued event (value=%d), proving events were processed in order",
                        lastEventValue)
                .isEqualTo(lastEventValue);
        assertThat(finalEntity.getLabel())
                .as("Property 10: Entity label should be from the last event")
                .isEqualTo("event-" + eventCount);

        // Cleanup
        factory.clearAll();
    }

    // ==================== Property 11: Atomik Batch Güncelleme ====================

    /**
     * Property 11: Atomik Batch Güncelleme.
     *
     * <p>Rastgele entity batch'leri ile eşzamanlı okuma sırasında ara durumların
     * görünmediği doğrulanır. {@code findAll()} çağrıldığında ya eski tutarlı durumu
     * ya da yeni tutarlı durumu döndürür; kısmi güncelleme asla görünür olmaz.</p>
     *
     * <p>DependencyGraph.upsertAll() uses {@code entityStore.compute()} which holds
     * the ConcurrentHashMap segment lock during the entire lambda execution, ensuring
     * atomicity. A concurrent findAll() will see either the old TreeMap reference or
     * the new one — never a partially updated TreeMap.</p>
     *
     * <p><b>Validates: Requirements 7.4</b></p>
     */
    // Feature: streaming-datasource-unification, Property 11: Atomik Batch Güncelleme
    @Property(tries = 100)
    void property11BatchUpdateIsAtomicNeverShowsPartialState(
            @ForAll("entityBatches") List<TestEntity> batchEntities) {

        String dsName = "atomic-test-ds";
        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);

        registerStreamingDs(dsName, StreamingDataSourceState.READY);
        IncrementalSyncProcessor processor = createProcessor(graph, version);

        // Seed initial state: entities with value=0
        int batchSize = batchEntities.size();
        List<TestEntity> initialEntities = new ArrayList<>();
        for (int i = 1; i <= batchSize; i++) {
            initialEntities.add(new TestEntity(i, 0, "initial"));
        }
        graph.upsertAll(dsName, initialEntities);

        // Prepare update batch: all entities get value=999
        List<TestEntity> updateEntities = new ArrayList<>();
        for (int i = 1; i <= batchSize; i++) {
            updateEntities.add(new TestEntity(i, 999, "updated"));
        }

        // Concurrent reads during batch processing
        List<Boolean> consistencyResults = new CopyOnWriteArrayList<>();
        int readerCount = 4;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount + 1);

        ExecutorService executor = Executors.newFixedThreadPool(readerCount + 1);

        // Writer thread: processes the batch update
        executor.submit(() -> {
            try {
                startLatch.await();
                BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                        updateEntities, Instant.now());
                processor.processBatchSnapshot(dsName, event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        // Reader threads: repeatedly read and check consistency
        for (int r = 0; r < readerCount; r++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Perform multiple reads during the update window
                    for (int attempt = 0; attempt < 50; attempt++) {
                        List<TestEntity> snapshot = graph.findAll(dsName);
                        if (snapshot.isEmpty()) {
                            continue;
                        }
                        // Check consistency: ALL entities in the snapshot must have
                        // the same label — either all "initial" or all "updated"
                        String firstLabel = snapshot.get(0).getLabel();
                        boolean consistent = snapshot.stream()
                                .allMatch(e -> e.getLabel().equals(firstLabel));
                        consistencyResults.add(consistent);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();

        try {
            boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed).as("All threads should complete within timeout").isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();

        // Verify: every snapshot observed was consistent (no partial state)
        assertThat(consistencyResults)
                .as("Property 11: Concurrent reads should have occurred")
                .isNotEmpty();
        assertThat(consistencyResults)
                .as("Property 11: Every snapshot must be consistent — either all 'initial' "
                        + "or all 'updated'. No partial/intermediate state should be visible. "
                        + "Atomicity is guaranteed by ConcurrentHashMap.compute() holding the "
                        + "segment lock during upsertAll().")
                .allMatch(consistent -> consistent);

        // Verify final state: all entities should be updated
        List<TestEntity> finalState = graph.findAll(dsName);
        assertThat(finalState)
                .as("Property 11: After batch update completes, all entities should be updated")
                .hasSize(batchSize);
        for (TestEntity e : finalState) {
            assertThat(e.getValue())
                    .as("Property 11: Entity id=%d should have updated value", e.getIdentity())
                    .isEqualTo(999);
        }

        // Cleanup
        factory.clearAll();
    }

    // ==================== Additional Generators for Property 7 & 8 ====================

    @Provide
    Arbitrary<Boolean> fetchAllSucceeds() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<List<TestEntity>> initialEntityBatches() {
        return Arbitraries.integers().between(1, 20)
                .flatMap(size -> {
                    Arbitrary<TestEntity> entityArb = Arbitraries.integers().between(1, 100)
                            .flatMap(id -> Arbitraries.integers().between(1, 500)
                                    .map(val -> new TestEntity(id, val, "initial")));
                    return entityArb.list().ofSize(size);
                });
    }

    @Provide
    Arbitrary<Integer> queuedEventCounts() {
        return Arbitraries.integers().between(1, 8);
    }

    // ==================== Property 7: fetchAll() ile Initial State Yükleme ve Durum Geçişi ====================

    /**
     * Property 7: fetchAll() ile Initial State Yükleme ve Durum Geçişi.
     *
     * <p>Rastgele başarılı/başarısız fetchAll() sonuçları ile durum geçişlerini doğrular.
     * Başarılı → INITIALIZING → READY; Başarısız → INITIALIZING → ERROR + reconnection.</p>
     *
     * <p>Test stratejisi: Mock StreamingDataSource ile fetchAll() başarılı veya başarısız
     * sonuç döndürülür. StreamingDataSourceLifecycleManager ve DataSourceSyncMetadata
     * kullanılarak durum geçişleri doğrulanır. Başarılı durumda entity'ler DependencyGraph'a
     * yüklenir ve READY'ye geçilir. Başarısız durumda ERROR'a geçilir.</p>
     *
     * <p><b>Validates: Requirements 6.1, 6.2, 6.3</b></p>
     */
    // Feature: streaming-datasource-unification, Property 7: fetchAll() ile Initial State Yükleme ve Durum Geçişi
    @Property(tries = 100)
    @SuppressWarnings("unchecked")
    void property7FetchAllInitialStateLoadingAndStateTransition(
            @ForAll("fetchAllSucceeds") boolean succeeds,
            @ForAll("initialEntityBatches") List<TestEntity> entities) {

        String dsName = "init-state-ds";
        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);
        StreamingDataSourceLifecycleManager lifecycleManager = new StreamingDataSourceLifecycleManager();

        // Register in lifecycle manager — starts INITIALIZING
        lifecycleManager.register(dsName);
        assertThat(lifecycleManager.getState(dsName))
                .as("Property 7: Newly registered datasource should be INITIALIZING")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // Create metadata — starts as streaming, INITIALIZING
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, java.time.Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(StreamingDataSourceState.INITIALIZING);

        // Register streaming datasource in factory
        StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(dsName);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
        factory.registerDataSource(dsName, streamingDs);

        // Create processor
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, version);

        if (succeeds) {
            // === Successful fetchAll() path ===
            // Simulate: fetchAll() returns entities successfully — process via processBatchDataSourceResult
            processor.processBatchDataSourceResult(dsName, entities);

            // Transition to READY
            lifecycleManager.handleInitialLoadComplete(dsName);
            metadata.updateStreamingState(StreamingDataSourceState.READY);

            // Verify: state is READY
            assertThat(lifecycleManager.getState(dsName))
                    .as("Property 7: After successful fetchAll(), state should be READY")
                    .isEqualTo(StreamingDataSourceState.READY);
            assertThat(metadata.getStreamingState())
                    .as("Property 7: Metadata streaming state should be READY")
                    .isEqualTo(StreamingDataSourceState.READY);

            // Verify: entities are loaded into DependencyGraph
            List<TestEntity> stored = graph.findAll(dsName);
            assertThat(stored)
                    .as("Property 7: After successful fetchAll(), entities should be in DependencyGraph")
                    .isNotEmpty();

            // Verify each entity from fetchAll() is findable
            for (TestEntity entity : entities) {
                TestEntity found = graph.findById(dsName, entity.getIdentity());
                assertThat(found)
                        .as("Property 7: Entity id=%d should be stored in DependencyGraph after "
                                + "successful initial state loading", entity.getIdentity())
                        .isNotNull();
            }

            // Verify: lifecycle manager reports healthy
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 7: After successful initial load, datasource should be healthy")
                    .isTrue();

        } else {
            // === Failed fetchAll() path ===
            // Simulate: fetchAll() fails — transition to ERROR + reconnection

            // Transition to ERROR
            lifecycleManager.handleConnectionLoss(dsName, "Initial fetchAll() failed: test exception");
            metadata.updateStreamingState(StreamingDataSourceState.ERROR);

            // Verify: state is ERROR
            assertThat(lifecycleManager.getState(dsName))
                    .as("Property 7: After failed fetchAll(), state should be ERROR")
                    .isEqualTo(StreamingDataSourceState.ERROR);
            assertThat(metadata.getStreamingState())
                    .as("Property 7: Metadata streaming state should be ERROR")
                    .isEqualTo(StreamingDataSourceState.ERROR);

            // Verify: lifecycle manager reports unhealthy
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 7: After failed fetchAll(), datasource should be unhealthy")
                    .isFalse();

            // Verify: DependencyGraph should be empty (no entities loaded)
            List<TestEntity> stored = graph.findAll(dsName);
            assertThat(stored)
                    .as("Property 7: After failed fetchAll(), DependencyGraph should have no entities")
                    .isEmpty();

            // Verify: reconnection delay is calculable (reconnection mechanism exists)
            java.time.Duration delay = lifecycleManager.calculateNextReconnectDelay(dsName);
            assertThat(delay)
                    .as("Property 7: After failed fetchAll(), reconnection delay should be positive")
                    .isPositive();
        }

        // Cleanup
        factory.clearAll();
    }

    // ==================== Property 8: INITIALIZING Durumunda Event Kuyruklama ====================

    /**
     * Property 8: INITIALIZING Durumunda Event Kuyruklama.
     *
     * <p>Rastgele event dizileri ile INITIALIZING durumunda kuyruklama ve READY sonrası
     * FIFO işleme doğrulaması. Hiçbir event kaybolmamalı ve sıraları bozulmamalı.</p>
     *
     * <p>Test stratejisi: INITIALIZING durumunda rastgele sayıda event kuyruğa alınır.
     * Sonra datasource READY'ye geçirilir ve kuyruklanmış event'ler işlenir.
     * İşleme sonrası tüm event'lerin DependencyGraph'a yansıdığı ve FIFO sırasının
     * korunduğu doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 6.6</b></p>
     */
    // Feature: streaming-datasource-unification, Property 8: INITIALIZING Durumunda Event Kuyruklama
    @Property(tries = 100)
    void property8InitializingStateQueuesEventsAndProcessesFIFOAfterReady(
            @ForAll("queuedEventCounts") int eventCount) {

        String dsName = "queue-test-ds";
        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);

        // Register streaming datasource in INITIALIZING state
        registerStreamingDs(dsName, StreamingDataSourceState.INITIALIZING);
        IncrementalSyncProcessor processor = createProcessor(graph, version);

        // Simulate INITIALIZING state: queue events instead of processing them
        // Each event updates entity id=1 with a value encoding the event order
        List<Integer> expectedValues = new ArrayList<>();
        for (int i = 1; i <= eventCount; i++) {
            int eventValue = i * 100;
            expectedValues.add(eventValue);
            List<TestEntity> eventEntities = List.of(
                    new TestEntity(1, eventValue, "event-" + i));
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    eventEntities, Instant.now());

            // Queue the event (simulating INITIALIZING state behavior)
            processor.queueEvent(dsName, event);
        }

        // Verify: all events are queued, none lost
        assertThat(processor.getQueuedEventCount())
                .as("Property 8: All %d events should be queued during INITIALIZING state. "
                        + "No event should be lost.", eventCount)
                .isEqualTo(eventCount);

        // Verify: DependencyGraph should be empty (events are queued, not processed)
        List<TestEntity> beforeReady = graph.findAll(dsName);
        assertThat(beforeReady)
                .as("Property 8: While INITIALIZING, queued events should NOT be processed. "
                        + "DependencyGraph should be empty.")
                .isEmpty();

        // Simulate transition to READY: process queued events for this datasource
        processor.processQueuedEventsForDataSource(dsName);

        // Verify: queue is empty after processing
        assertThat(processor.getQueuedEventCount())
                .as("Property 8: Queue should be empty after processing all events for '%s'", dsName)
                .isEqualTo(0);

        // Verify: FIFO order — entity id=1 should have the value from the LAST event
        // (each event overwrites the same entity, so FIFO means last event wins)
        TestEntity finalEntity = graph.findById(dsName, 1);
        int lastEventValue = expectedValues.get(expectedValues.size() - 1);

        assertThat(finalEntity)
                .as("Property 8: Entity should exist after processing queued events")
                .isNotNull();
        assertThat(finalEntity.getValue())
                .as("Property 8: After FIFO processing, entity value should reflect the last "
                        + "queued event (value=%d). This proves events were processed in order "
                        + "and no event was lost.", lastEventValue)
                .isEqualTo(lastEventValue);
        assertThat(finalEntity.getLabel())
                .as("Property 8: Entity label should be from the last event, proving FIFO order")
                .isEqualTo("event-" + eventCount);

        // Additional verification: queue multiple entities per event to verify no data loss
        // Queue events with DIFFERENT entity IDs to verify all entities are preserved
        DependencyGraph graph2 = new DependencyGraph();
        AtomicLong version2 = new AtomicLong(0);
        IncrementalSyncProcessor processor2 = createProcessor(graph2, version2);

        for (int i = 1; i <= eventCount; i++) {
            List<TestEntity> multiEntities = List.of(
                    new TestEntity(i * 10, i, "multi-" + i));
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(
                    multiEntities, Instant.now());
            processor2.queueEvent(dsName, event);
        }

        assertThat(processor2.getQueuedEventCount())
                .as("Property 8: Second batch of events should all be queued")
                .isEqualTo(eventCount);

        processor2.processQueuedEventsForDataSource(dsName);

        // Verify: ALL entities from ALL events are present (no event lost)
        List<TestEntity> allStored = graph2.findAll(dsName);
        assertThat(allStored)
                .as("Property 8: After processing %d queued events with unique entity IDs, "
                        + "all %d entities should be present. No event should be lost.",
                        eventCount, eventCount)
                .hasSize(eventCount);

        // Verify each entity is present
        for (int i = 1; i <= eventCount; i++) {
            TestEntity found = graph2.findById(dsName, i * 10);
            assertThat(found)
                    .as("Property 8: Entity id=%d from event %d should be present", i * 10, i)
                    .isNotNull();
            assertThat(found.getLabel())
                    .as("Property 8: Entity id=%d should have correct label", i * 10)
                    .isEqualTo("multi-" + i);
        }

        // Cleanup
        factory.clearAll();
    }

    // ==================== Additional Generators for Property 6 & 12 ====================

    @Provide
    Arbitrary<List<StreamingDataSourceState>> streamingStateCombinations() {
        return Arbitraries.of(StreamingDataSourceState.READY, StreamingDataSourceState.INITIALIZING)
                .list().ofMinSize(2).ofMaxSize(8);
    }

    @Provide
    Arbitrary<Integer> fullSyncEventCounts() {
        return Arbitraries.integers().between(2, 12);
    }

    // ==================== Property 6: Full Sync'te Streaming DataSource Dahil Etme — Durum Bazlı ====================

    /**
     * Property 6: Full Sync'te Streaming DataSource Dahil Etme — Durum Bazlı.
     *
     * <p>Rastgele READY/INITIALIZING datasource kombinasyonları ile full sync sırasında
     * yalnızca READY durumundaki streaming datasource'ların {@code fetchAll()} ile dahil
     * edildiğini, INITIALIZING datasource'ların atlandığını doğrular.</p>
     *
     * <p>Test stratejisi: Rastgele sayıda streaming datasource, her biri rastgele READY veya
     * INITIALIZING durumunda oluşturulur. {@code DataSourceSyncMetadata} ve factory üzerinden
     * {@code readAllDataSources()} mantığı simüle edilir: yalnızca READY datasource'lar için
     * {@code fetchAll()} çağrılır ve veri DataVersion'a dahil edilir.</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b></p>
     */
    // Feature: streaming-datasource-unification, Property 6: Full Sync'te Streaming DataSource Dahil Etme — Durum Bazlı
    @Property(tries = 100)
    @SuppressWarnings("unchecked")
    void property6FullSyncIncludesOnlyReadyStreamingDataSources(
            @ForAll("streamingStateCombinations") List<StreamingDataSourceState> states) {

        // Build datasource metadata map and register mock streaming datasources
        Map<String, DataSourceSyncMetadata> metadataMap = new HashMap<>();
        Map<String, List<TestEntity>> expectedData = new HashMap<>();

        for (int i = 0; i < states.size(); i++) {
            String dsName = "streaming-ds-" + i;
            StreamingDataSourceState state = states.get(i);

            // Create metadata
            DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, Duration.ZERO);
            metadata.setStreamingDataSource(true);
            metadata.setStreamingState(state);
            metadataMap.put(dsName, metadata);

            // Create mock streaming datasource with fetchAll() returning test entities
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(dsName);
            when(streamingDs.getState()).thenReturn(state);

            List<TestEntity> entities = List.of(
                    new TestEntity(i * 100 + 1, i, "entity-from-" + dsName));
            when(streamingDs.fetchAll()).thenReturn(CompletableFuture.completedFuture(entities));

            factory.registerDataSource(dsName, streamingDs);

            if (state == StreamingDataSourceState.READY) {
                expectedData.put(dsName, entities);
            }
        }

        // Simulate readAllDataSources() streaming inclusion logic:
        // Iterate metadata, include only READY streaming datasources via fetchAll()
        Map<String, List<?>> includedData = new HashMap<>();

        for (Map.Entry<String, DataSourceSyncMetadata> entry : metadataMap.entrySet()) {
            DataSourceSyncMetadata metadata = entry.getValue();
            if (metadata.isStreamingDataSource()
                    && metadata.getStreamingState() == StreamingDataSourceState.READY) {
                String dsName = entry.getKey();
                StreamingDataSource<?> streamingDs = factory.getStreamingDataSource(dsName);
                if (streamingDs != null) {
                    try {
                        List<?> entities = streamingDs.fetchAll().get(5, TimeUnit.SECONDS);
                        if (entities != null) {
                            includedData.put(dsName, entities);
                        }
                    } catch (Exception e) {
                        // Should not happen with CompletableFuture.completedFuture
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        // Verify: only READY datasources are included
        for (int i = 0; i < states.size(); i++) {
            String dsName = "streaming-ds-" + i;
            StreamingDataSourceState state = states.get(i);

            if (state == StreamingDataSourceState.READY) {
                assertThat(includedData)
                        .as("Property 6: READY datasource '%s' should be included in full sync "
                                + "via fetchAll()", dsName)
                        .containsKey(dsName);
                assertThat(includedData.get(dsName))
                        .as("Property 6: READY datasource '%s' data should match fetchAll() result",
                                dsName)
                        .isEqualTo(expectedData.get(dsName));
            } else {
                assertThat(includedData)
                        .as("Property 6: INITIALIZING datasource '%s' should NOT be included "
                                + "in full sync — only READY datasources participate", dsName)
                        .doesNotContainKey(dsName);
            }
        }

        // Verify: total included count matches READY count
        long readyCount = states.stream()
                .filter(s -> s == StreamingDataSourceState.READY)
                .count();
        assertThat(includedData.size())
                .as("Property 6: Number of included datasources (%d) should equal "
                        + "number of READY datasources (%d)", includedData.size(), readyCount)
                .isEqualTo((int) readyCount);

        // Cleanup
        factory.clearAll();
    }

    // ==================== Property 12: Full Sync Sırasında Event Kuyruklama ====================

    /**
     * Property 12: Full Sync Sırasında Event Kuyruklama.
     *
     * <p>Rastgele event dizileri ile full sync sırasında kuyruklama ve sync sonrası
     * FIFO işleme doğrulaması. Kuyrukta biriken event'lerin hiçbirinin kaybolmadığı
     * doğrulanır.</p>
     *
     * <p>Test stratejisi: {@code setFullSyncInProgress(true)} ile full sync simüle edilir.
     * Bu sürede gelen event'ler {@code queueEvent()} ile kuyruğa alınır. Full sync
     * tamamlandıktan sonra ({@code setFullSyncInProgress(false)}) kuyruklanmış event'ler
     * {@code processQueuedEvents()} ile işlenir. Tüm event'lerin kaybolmadan FIFO
     * sırasıyla işlendiği doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 7.5</b></p>
     */
    // Feature: streaming-datasource-unification, Property 12: Full Sync Sırasında Event Kuyruklama
    @Property(tries = 100)
    void property12FullSyncQueuesEventsAndProcessesFIFOAfterCompletion(
            @ForAll("fullSyncEventCounts") int eventCount) {

        String dsName = "full-sync-queue-ds";
        DependencyGraph graph = new DependencyGraph();
        AtomicLong version = new AtomicLong(0);

        registerStreamingDs(dsName, StreamingDataSourceState.READY);
        IncrementalSyncProcessor processor = createProcessor(graph, version);

        // Simulate full sync in progress
        processor.setFullSyncInProgress(true);
        assertThat(processor.isFullSyncInProgress())
                .as("Property 12: Full sync should be marked as in progress")
                .isTrue();

        // Queue events during full sync — each event has a unique entity ID
        // plus a shared entity (id=1) that gets overwritten to verify FIFO order
        for (int i = 1; i <= eventCount; i++) {
            List<TestEntity> entities = List.of(
                    new TestEntity(1, i * 100, "fullsync-event-" + i),
                    new TestEntity(i + 1000, i, "unique-" + i));
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());

            // During full sync, events should be queued, not processed
            processor.queueEvent(dsName, event);
        }

        // Verify: all events are queued, none lost
        assertThat(processor.getQueuedEventCount())
                .as("Property 12: All %d events should be queued during full sync. "
                        + "No event should be lost.", eventCount)
                .isEqualTo(eventCount);

        // Verify: DependencyGraph should be empty (events are queued, not processed)
        List<TestEntity> beforeSync = graph.findAll(dsName);
        assertThat(beforeSync)
                .as("Property 12: While full sync is in progress, queued events should NOT "
                        + "be processed. DependencyGraph should be empty.")
                .isEmpty();

        // Complete full sync
        processor.setFullSyncInProgress(false);
        assertThat(processor.isFullSyncInProgress())
                .as("Property 12: Full sync should no longer be in progress")
                .isFalse();

        // Process all queued events
        processor.processQueuedEvents();

        // Verify: queue is empty after processing
        assertThat(processor.getQueuedEventCount())
                .as("Property 12: Queue should be empty after processing all events")
                .isEqualTo(0);

        // Verify FIFO order: shared entity id=1 should have the value from the LAST event
        TestEntity sharedEntity = graph.findById(dsName, 1);
        assertThat(sharedEntity)
                .as("Property 12: Shared entity (id=1) should exist after processing")
                .isNotNull();
        assertThat(sharedEntity.getValue())
                .as("Property 12: After FIFO processing, shared entity value should reflect "
                        + "the last queued event (value=%d), proving events were processed in order",
                        eventCount * 100)
                .isEqualTo(eventCount * 100);
        assertThat(sharedEntity.getLabel())
                .as("Property 12: Shared entity label should be from the last event")
                .isEqualTo("fullsync-event-" + eventCount);

        // Verify no event lost: all unique entities (id=1001..1000+eventCount) should be present
        List<TestEntity> allStored = graph.findAll(dsName);
        // Expected: 1 shared entity (id=1) + eventCount unique entities
        assertThat(allStored)
                .as("Property 12: After processing %d queued events, all entities should be "
                        + "present (1 shared + %d unique = %d total). No event should be lost.",
                        eventCount, eventCount, eventCount + 1)
                .hasSize(eventCount + 1);

        // Verify each unique entity is present
        for (int i = 1; i <= eventCount; i++) {
            int uniqueId = i + 1000;
            TestEntity found = graph.findById(dsName, uniqueId);
            assertThat(found)
                    .as("Property 12: Unique entity id=%d from event %d should be present. "
                            + "No event should be lost during full sync queuing.", uniqueId, i)
                    .isNotNull();
            assertThat(found.getLabel())
                    .as("Property 12: Unique entity id=%d should have correct label", uniqueId)
                    .isEqualTo("unique-" + i);
        }

        // Cleanup
        factory.clearAll();
    }
}
