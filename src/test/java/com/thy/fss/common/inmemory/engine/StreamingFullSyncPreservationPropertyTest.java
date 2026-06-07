package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.datasource.*;
import com.thy.fss.common.inmemory.engine.sync.DataSourceSyncMetadata;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation property tests for streaming full sync scheduling bugfix.
 *
 * These tests capture the CURRENT (unfixed) behavior that MUST be preserved after the fix.
 * All tests MUST PASS on unfixed code — they establish the baseline.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Label("Property 2: Preservation — Batch Sync, Streaming Event, Health Check, Zero-Interval, Initial Load")
class StreamingFullSyncPreservationPropertyTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;

    @BeforeTry
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
        factory.clearAllDataSources();
    }

    @AfterTry
    void tearDown() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
        factory.clearAll();
        factory.clearAllDataSources();
    }

    // ==================== Property 2a: Batch DS Sync Behavior Preserved ====================

    /**
     * Property 2a: For all batch datasource configurations, checkAndTriggerSync() behavior
     * (shouldSync + synchronizeDataSource flow) is preserved.
     *
     * When a batch DS has shouldSync() == true, checkAndTriggerSync() should call
     * synchronizeDataSource(), which re-reads the datasource via fetchAll().
     *
     * This tests the ELSE branch in checkAndTriggerSync() — must remain unchanged.
     *
     * **Validates: Requirements 3.1**
     */
    @Property(tries = 15)
    @Label("Property 2a: Batch DS checkAndTriggerSync shouldSync + synchronizeDataSource preserved")
    void batchDataSourceSyncBehaviorPreserved(
            @ForAll("batchSyncIntervals") Duration syncInterval) throws Exception {

        String dsName = "batch-ds-" + syncInterval.toSeconds();
        TrackingBatchDataSource ds = new TrackingBatchDataSource(dsName);

        factory.registerDataSource(dsName, ds, syncInterval);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        Map<String, DataSourceSyncMetadata> metadataMap = engine.getDataSourceMetadataInternal();
        DataSourceSyncMetadata metadata = metadataMap.get(dsName);

        assertThat(metadata)
                .as("Metadata should exist for batch DS '%s'", dsName)
                .isNotNull();
        assertThat(metadata.isStreamingDataSource())
                .as("Batch DS should NOT be marked as streaming")
                .isFalse();
        assertThat(metadata.getSyncInterval())
                .as("Batch DS metadata interval should match registered interval")
                .isEqualTo(syncInterval);

        // Record fetchAll count after initialization (init calls fetchAll at least once)
        int fetchCountAfterInit = ds.getFetchAllCallCount();
        assertThat(fetchCountAfterInit)
                .as("fetchAll should have been called during initialization")
                .isGreaterThanOrEqualTo(1);

        // Force shouldSync() to return true
        metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));
        assertThat(metadata.shouldSync()).isTrue();

        // Call checkAndTriggerSync via reflection — this should trigger synchronizeDataSource
        // which calls triggerGlobalSynchronization, re-reading the DS via fetchAll.
        // We retry a few times to handle scheduler lock contention.
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);

        // Try up to 5 times with small delays to handle lock contention with background scheduler
        boolean syncConfirmed = false;
        for (int attempt = 0; attempt < 5 && !syncConfirmed; attempt++) {
            // Ensure shouldSync is still true
            if (!metadata.shouldSync()) {
                metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));
            }
            checkAndTriggerSync.invoke(engine);
            Thread.sleep(300);

            // Check if fetchAll was called again (proves synchronizeDataSource was triggered)
            if (ds.getFetchAllCallCount() > fetchCountAfterInit) {
                syncConfirmed = true;
            }
        }

        assertThat(syncConfirmed)
                .as("Batch DS '%s' should have fetchAll() called again after checkAndTriggerSync " +
                        "when shouldSync()==true. fetchCountAfterInit=%d, fetchCountNow=%d",
                        dsName, fetchCountAfterInit, ds.getFetchAllCallCount())
                .isTrue();
    }

    // ==================== Property 2b: Streaming Event Incremental Sync Preserved ====================

    /**
     * Property 2b: For all streaming events (BatchSnapshotEvent), IncrementalSyncProcessor
     * incremental sync behavior is preserved.
     *
     * When a streaming DS receives a BatchSnapshotEvent, the event should be routed to
     * IncrementalSyncProcessor.processBatchSnapshot() and the streaming version should increment.
     *
     * **Validates: Requirements 3.2, 3.5**
     */
    @Property(tries = 10)
    @Label("Property 2b: Streaming event → IncrementalSyncProcessor incremental sync preserved")
    void streamingEventIncrementalSyncPreserved(
            @ForAll("entityCounts") int entityCount) throws Exception {

        String dsName = "streaming-event-ds";
        CapturingStreamingDataSource ds = new CapturingStreamingDataSource(dsName);

        factory.registerDataSource(dsName, ds);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Verify DS is in READY state after initialization
        DataSourceSyncMetadata metadata = engine.getDataSourceMetadataInternal().get(dsName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.isStreamingDataSource()).isTrue();
        assertThat(metadata.getStreamingState())
                .as("Streaming DS should be READY after successful initialization")
                .isEqualTo(StreamingDataSourceState.READY);

        // Capture streaming version before event
        long versionBefore = engine.getStreamingVersion();

        // Fire a BatchSnapshotEvent through the captured listener
        BatchSnapshotEventListener<PreservationTestEntity> listener = ds.getCapturedListener();
        assertThat(listener)
                .as("Listener should have been captured during subscribe()")
                .isNotNull();

        List<PreservationTestEntity> entities = new ArrayList<>();
        for (int i = 0; i < entityCount; i++) {
            entities.add(new PreservationTestEntity(i));
        }
        BatchSnapshotEvent<PreservationTestEntity> event =
                new BatchSnapshotEvent<>(entities, Instant.now());

        // Fire the event
        listener.onBatchSnapshot(event);

        // Verify streaming version incremented (proves event was processed, not queued)
        long versionAfter = engine.getStreamingVersion();
        assertThat(versionAfter)
                .as("Streaming version should increment after event processing. before=%d, after=%d",
                        versionBefore, versionAfter)
                .isGreaterThan(versionBefore);
    }

    // ==================== Property 2c: Health Check Behavior Preserved ====================

    /**
     * Property 2c: For all unhealthy streaming datasources, health check → ERROR state
     * + reconnection scheduling behavior is preserved.
     *
     * When checkAndTriggerSync() is called and a streaming DS reports isHealthy()=false,
     * checkStreamingDataSourceHealth() should transition the DS to ERROR state.
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 10)
    @Label("Property 2c: Unhealthy streaming DS → health check → ERROR state preserved")
    void unhealthyStreamingDataSourceHealthCheckPreserved(
            @ForAll("entityCounts") int entityCount) throws Exception {

        String dsName = "unhealthy-streaming-ds";
        ControllableStreamingDataSource ds = new ControllableStreamingDataSource(dsName, true);

        factory.registerDataSource(dsName, ds);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSourceSyncMetadata metadata = engine.getDataSourceMetadataInternal().get(dsName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.isStreamingDataSource()).isTrue();
        assertThat(metadata.getStreamingState())
                .as("DS should be READY after successful init")
                .isEqualTo(StreamingDataSourceState.READY);

        // Now make the DS unhealthy
        ds.setHealthy(false);

        // Call checkAndTriggerSync — this should trigger checkStreamingDataSourceHealth
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);
        checkAndTriggerSync.invoke(engine);

        // Give async operations time
        Thread.sleep(300);

        // Verify: metadata should transition to ERROR state
        assertThat(metadata.getStreamingState())
                .as("Unhealthy streaming DS should transition to ERROR state after health check")
                .isEqualTo(StreamingDataSourceState.ERROR);
    }

    // ==================== Property 2d: Zero-Interval Streaming DS No Periodic Sync ====================

    /**
     * Property 2d: For all streaming datasources with syncInterval=Duration.ZERO,
     * no periodic full sync should be triggered.
     *
     * This is the current behavior: streaming DS without explicit syncInterval only
     * operates via streaming events, no periodic full sync.
     *
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 10)
    @Label("Property 2d: Streaming DS with syncInterval=ZERO → no periodic full sync")
    void streamingDataSourceWithZeroIntervalNoPeriodicSync(
            @ForAll("entityCounts") int entityCount) throws Exception {

        String dsName = "zero-interval-streaming";
        CapturingStreamingDataSource ds = new CapturingStreamingDataSource(dsName);

        // Register WITHOUT syncInterval (defaults to Duration.ZERO)
        factory.registerDataSource(dsName, ds);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSourceSyncMetadata metadata = engine.getDataSourceMetadataInternal().get(dsName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.isStreamingDataSource()).isTrue();

        // Verify metadata has Duration.ZERO interval
        assertThat(metadata.getSyncInterval())
                .as("Zero-interval streaming DS should have Duration.ZERO in metadata")
                .isEqualTo(Duration.ZERO);

        // Force shouldSync() to return true by setting nextSyncTime to past
        metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));

        LocalDateTime lastSyncBefore = metadata.getLastSyncTime();

        // Call checkAndTriggerSync
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);
        checkAndTriggerSync.invoke(engine);

        Thread.sleep(300);

        // Verify: synchronizeDataSource should NOT have been called
        Set<String> pendingAfter = engine.getPendingDataSourcesInternal();
        LocalDateTime lastSyncAfter = metadata.getLastSyncTime();

        boolean syncWasTriggered = pendingAfter.contains(dsName)
                || (lastSyncAfter != null && !lastSyncAfter.equals(lastSyncBefore));

        assertThat(syncWasTriggered)
                .as("Zero-interval streaming DS should NOT get periodic full sync. " +
                        "pending=%s, lastSyncBefore=%s, lastSyncAfter=%s",
                        pendingAfter, lastSyncBefore, lastSyncAfter)
                .isFalse();
    }

    // ==================== Property 2e: Initial Load via fetchAll() Preserved ====================

    /**
     * Property 2e: For all streaming datasources, initializeStreamingInfrastructure()
     * fetchAll() initial load behavior is preserved.
     *
     * During engine initialization, streaming DS fetchAll() should be called to load
     * initial state, and the DS should transition to READY state upon success.
     *
     * **Validates: Requirements 3.5**
     */
    @Property(tries = 10)
    @Label("Property 2e: initializeStreamingInfrastructure fetchAll() initial load preserved")
    void initializeStreamingInfrastructureFetchAllPreserved(
            @ForAll("entityCounts") int entityCount) throws Exception {

        String dsName = "initial-load-ds";

        // Create entities for fetchAll to return
        List<PreservationTestEntity> initialEntities = new ArrayList<>();
        for (int i = 0; i < entityCount; i++) {
            initialEntities.add(new PreservationTestEntity(i));
        }

        TrackingStreamingDataSource ds = new TrackingStreamingDataSource(dsName, initialEntities);

        factory.registerDataSource(dsName, ds);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Verify fetchAll() was called during initialization
        assertThat(ds.getFetchAllCallCount())
                .as("fetchAll() should be called during initializeStreamingInfrastructure()")
                .isGreaterThanOrEqualTo(1);

        // Verify DS transitioned to READY state
        DataSourceSyncMetadata metadata = engine.getDataSourceMetadataInternal().get(dsName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.isStreamingDataSource()).isTrue();
        assertThat(metadata.getStreamingState())
                .as("Streaming DS should be READY after successful fetchAll() initial load")
                .isEqualTo(StreamingDataSourceState.READY);
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Duration> batchSyncIntervals() {
        return Arbitraries.longs()
                .between(1, 30)
                .map(Duration::ofMinutes);
    }

    @Provide
    Arbitrary<Integer> entityCounts() {
        return Arbitraries.integers().between(1, 10);
    }

    // ==================== Test Entity ====================

    static class PreservationTestEntity implements Identifiable<Integer> {
        private final int id;

        PreservationTestEntity(int id) {
            this.id = id;
        }

        @Override
        public Integer getIdentity() { return id; }
    }

    // ==================== Stub: Batch DataSource ====================

    /**
     * Simple batch (non-streaming) datasource stub that tracks fetchAll() calls for Property 2a.
     */
    static class TrackingBatchDataSource implements DataSource<PreservationTestEntity> {
        private final String name;
        private final AtomicInteger fetchAllCallCount = new AtomicInteger(0);

        TrackingBatchDataSource(String name) {
            this.name = name;
        }

        int getFetchAllCallCount() {
            return fetchAllCallCount.get();
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<PreservationTestEntity> getEntityType() { return PreservationTestEntity.class; }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAll() {
            fetchAllCallCount.incrementAndGet();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<PreservationTestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<PreservationTestEntity> fallbackDataSource) { }
    }

    // ==================== Stub: Capturing Streaming DataSource ====================

    /**
     * Streaming datasource that captures the subscribed listener for event firing (Property 2b, 2d).
     */
    static class CapturingStreamingDataSource implements StreamingDataSource<PreservationTestEntity> {
        private final String name;
        private volatile BatchSnapshotEventListener<PreservationTestEntity> capturedListener;

        CapturingStreamingDataSource(String name) {
            this.name = name;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<PreservationTestEntity> getEntityType() { return PreservationTestEntity.class; }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<PreservationTestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<PreservationTestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) {
            this.capturedListener = listener;
        }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) {
            if (this.capturedListener == listener) {
                this.capturedListener = null;
            }
        }

        BatchSnapshotEventListener<PreservationTestEntity> getCapturedListener() {
            return capturedListener;
        }
    }

    // ==================== Stub: Controllable Streaming DataSource ====================

    /**
     * Streaming datasource with controllable health status (Property 2c).
     */
    static class ControllableStreamingDataSource implements StreamingDataSource<PreservationTestEntity> {
        private final String name;
        private volatile boolean healthy;

        ControllableStreamingDataSource(String name, boolean initiallyHealthy) {
            this.name = name;
            this.healthy = initiallyHealthy;
        }

        void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<PreservationTestEntity> getEntityType() { return PreservationTestEntity.class; }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return healthy; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<PreservationTestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<PreservationTestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) { }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) { }
    }

    // ==================== Stub: Tracking Streaming DataSource ====================

    /**
     * Streaming datasource that tracks fetchAll() calls and returns configurable entities (Property 2e).
     */
    static class TrackingStreamingDataSource implements StreamingDataSource<PreservationTestEntity> {
        private final String name;
        private final List<PreservationTestEntity> entities;
        private final AtomicInteger fetchAllCallCount = new AtomicInteger(0);

        TrackingStreamingDataSource(String name, List<PreservationTestEntity> entities) {
            this.name = name;
            this.entities = entities;
        }

        int getFetchAllCallCount() {
            return fetchAllCallCount.get();
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<PreservationTestEntity> getEntityType() { return PreservationTestEntity.class; }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAll() {
            fetchAllCallCount.incrementAndGet();
            return CompletableFuture.completedFuture(new ArrayList<>(entities));
        }

        @Override
        public CompletableFuture<List<PreservationTestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<PreservationTestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<PreservationTestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) { }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<PreservationTestEntity> listener) { }
    }
}
