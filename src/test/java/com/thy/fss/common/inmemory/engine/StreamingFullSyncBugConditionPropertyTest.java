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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug condition exploration property test for streaming full sync scheduling.
 *
 * This test encodes the EXPECTED (correct) behavior. On UNFIXED code, these tests
 * MUST FAIL — failure confirms the bug exists across all three root causes.
 *
 * Validates: Requirements 1.1, 1.2, 1.3
 *
 * Bug Condition: StreamingDataSource with syncInterval > Duration.ZERO should get
 * periodic full sync, but currently doesn't due to three root causes:
 *   1. registerDataSource() doesn't save interval for streaming DS
 *   2. initializeStreamingInfrastructure() creates metadata with Duration.ZERO
 *   3. checkAndTriggerSync() skips shouldSync()/synchronizeDataSource() for streaming DS
 */
@Label("Property 1: Fault Condition — Streaming DataSource Periodic Full Sync Not Triggered")
class StreamingFullSyncBugConditionPropertyTest {

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

    // ==================== Fault Condition 1: Interval Registration ====================

    /**
     * Fault Condition 1: After registerDataSource("test", streamingDs, syncInterval),
     * getDataSourceInterval("test") should return the user-provided interval, NOT Duration.ZERO.
     *
     * On UNFIXED code: FAILS because streaming DS interval is not saved to dataSourceIntervalRegistry.
     *
     * Validates: Requirements 1.2
     */
    @Property(tries = 20)
    @Label("Fault Condition 1: Streaming DS interval should be saved in registry")
    void streamingDataSourceIntervalShouldBeSavedInRegistry(
            @ForAll("positiveSyncIntervals") Duration syncInterval) {

        String dsName = "test-streaming-" + syncInterval.toSeconds();
        StubStreamingDataSource ds = new StubStreamingDataSource(dsName);

        factory.registerDataSource(dsName, ds, syncInterval);

        // Bug: getDataSourceInterval returns Duration.ZERO for streaming DS
        // because registerDataSource doesn't save interval to dataSourceIntervalRegistry
        Duration registeredInterval = factory.getDataSourceInterval(dsName);

        assertThat(registeredInterval)
                .as("Streaming DS '%s' registered with interval %s should have that interval in registry, not ZERO",
                        dsName, syncInterval)
                .isNotEqualTo(Duration.ZERO)
                .isEqualTo(syncInterval);
    }

    // ==================== Fault Condition 2: Metadata Interval ====================

    /**
     * Fault Condition 2: After initializeStreamingInfrastructure(), the streaming DS
     * metadata's getSyncInterval() should be the user-provided interval, NOT Duration.ZERO.
     *
     * On UNFIXED code: FAILS because initializeStreamingInfrastructure() hardcodes Duration.ZERO.
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 20)
    @Label("Fault Condition 2: Streaming DS metadata should have user-provided interval")
    void streamingDataSourceMetadataShouldHaveUserProvidedInterval(
            @ForAll("positiveSyncIntervals") Duration syncInterval) {

        String dsName = "test-streaming-meta-" + syncInterval.toSeconds();
        StubStreamingDataSource ds = new StubStreamingDataSource(dsName);

        factory.registerDataSource(dsName, ds, syncInterval);

        // Initialize engine — this calls initializeStreamingInfrastructure()
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Get metadata via internal API
        Map<String, DataSourceSyncMetadata> metadataMap = engine.getDataSourceMetadataInternal();
        DataSourceSyncMetadata metadata = metadataMap.get(dsName);

        assertThat(metadata)
                .as("Metadata should exist for streaming DS '%s'", dsName)
                .isNotNull();

        assertThat(metadata.isStreamingDataSource())
                .as("Metadata should be marked as streaming")
                .isTrue();

        // Bug: metadata.getSyncInterval() returns Duration.ZERO because
        // initializeStreamingInfrastructure() creates metadata with Duration.ZERO
        assertThat(metadata.getSyncInterval())
                .as("Streaming DS '%s' metadata interval should be %s, not ZERO", dsName, syncInterval)
                .isNotEqualTo(Duration.ZERO)
                .isEqualTo(syncInterval);
    }

    // ==================== Fault Condition 3: Sync Trigger ====================

    /**
     * Fault Condition 3: When checkAndTriggerSync() is called and streaming DS has
     * shouldSync() == true, synchronizeDataSource() should be called.
     *
     * On UNFIXED code: FAILS because checkAndTriggerSync() only does health check
     * for streaming DS and skips shouldSync()/synchronizeDataSource().
     *
     * Validates: Requirements 1.1
     */
    @Property(tries = 20)
    @Label("Fault Condition 3: checkAndTriggerSync should trigger full sync for streaming DS")
    void checkAndTriggerSyncShouldTriggerFullSyncForStreamingDS(
            @ForAll("positiveSyncIntervals") Duration syncInterval) throws Exception {

        String dsName = "test-streaming-sync-" + syncInterval.toSeconds();
        StubStreamingDataSource ds = new StubStreamingDataSource(dsName);

        factory.registerDataSource(dsName, ds, syncInterval);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        Map<String, DataSourceSyncMetadata> metadataMap = engine.getDataSourceMetadataInternal();
        DataSourceSyncMetadata metadata = metadataMap.get(dsName);

        assertThat(metadata).as("Metadata should exist for '%s'", dsName).isNotNull();

        // Force shouldSync() to return true by setting nextSyncTime to the past
        metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));

        assertThat(metadata.shouldSync())
                .as("shouldSync() should return true after setting nextSyncTime to past")
                .isTrue();

        // Record fetchAll count before calling checkAndTriggerSync
        // (fetchAll is called once during initialize for initial load)
        int fetchAllCountBefore = ds.getFetchAllCallCount();

        // Call checkAndTriggerSync via reflection (it's private)
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);
        checkAndTriggerSync.invoke(engine);

        // Give a small window for async operations to complete
        Thread.sleep(500);

        // Bug: checkAndTriggerSync() only calls checkStreamingDataSourceHealth()
        // for streaming DS and never calls synchronizeDataSource().
        // After fix, synchronizeDataSource() should be called, which triggers
        // triggerGlobalSynchronization() and eventually fetchAll() on the DS.
        //
        // We verify by checking if fetchAll was called again (count increased).
        int fetchAllCountAfter = ds.getFetchAllCallCount();

        assertThat(fetchAllCountAfter)
                .as("synchronizeDataSource should have been called for streaming DS '%s' " +
                        "when shouldSync() == true and syncInterval > ZERO. " +
                        "fetchAllCount before=%d, after=%d",
                        dsName, fetchAllCountBefore, fetchAllCountAfter)
                .isGreaterThan(fetchAllCountBefore);
    }

    // ==================== Combined Property ====================

    /**
     * Combined Property: For all streaming datasources with syncInterval > Duration.ZERO,
     * after register → initialize → advanceTime → checkAndTriggerSync,
     * synchronizeDataSource() should have been called.
     *
     * On UNFIXED code: FAILS due to all three root causes.
     *
     * Validates: Requirements 1.1, 1.2, 1.3
     */
    @Property(tries = 20)
    @Label("Combined: Streaming DS with syncInterval > ZERO should get periodic full sync")
    void streamingDataSourceWithPositiveIntervalShouldGetPeriodicFullSync(
            @ForAll("positiveSyncIntervals") Duration syncInterval) throws Exception {

        String dsName = "test-combined-" + syncInterval.toSeconds();
        StubStreamingDataSource ds = new StubStreamingDataSource(dsName);

        // Step 1: Register
        factory.registerDataSource(dsName, ds, syncInterval);

        // Verify interval is saved (Fault Condition 1)
        Duration registeredInterval = factory.getDataSourceInterval(dsName);
        assertThat(registeredInterval)
                .as("Interval should be saved for streaming DS")
                .isEqualTo(syncInterval);

        // Step 2: Initialize
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Verify metadata interval (Fault Condition 2)
        DataSourceSyncMetadata metadata = engine.getDataSourceMetadataInternal().get(dsName);
        assertThat(metadata).isNotNull();
        assertThat(metadata.getSyncInterval())
                .as("Metadata interval should match registered interval")
                .isEqualTo(syncInterval);

        // Step 3: Advance time past interval
        metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));

        // Record fetchAll count before triggering sync
        // (fetchAll is called once during initialize for initial load)
        int fetchAllCountBefore = ds.getFetchAllCallCount();

        // Step 4: Trigger checkAndTriggerSync
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);
        checkAndTriggerSync.invoke(engine);

        Thread.sleep(500);

        // Step 5: Verify sync was triggered (Fault Condition 3)
        // fetchAll should have been called again by synchronizeDataSource → triggerGlobalSynchronization
        int fetchAllCountAfter = ds.getFetchAllCallCount();

        assertThat(fetchAllCountAfter)
                .as("Full sync should have been triggered for streaming DS '%s' with interval %s. " +
                        "fetchAllCount before=%d, after=%d",
                        dsName, syncInterval, fetchAllCountBefore, fetchAllCountAfter)
                .isGreaterThan(fetchAllCountBefore);
    }

    // ==================== Edge Case: Duration.ZERO ====================

    /**
     * Edge Case: Streaming DS with syncInterval = Duration.ZERO should NOT get
     * periodic full sync. This should pass even on UNFIXED code.
     *
     * Validates: Requirements 3.4
     */
    @Property(tries = 10)
    @Label("Edge Case: Streaming DS with syncInterval=ZERO should NOT get periodic full sync")
    void streamingDataSourceWithZeroIntervalShouldNotGetPeriodicSync(
            @ForAll("zeroOrNullIntervals") Duration syncInterval) throws Exception {

        String dsName = "test-zero-interval";
        StubStreamingDataSource ds = new StubStreamingDataSource(dsName);

        factory.registerDataSource(dsName, ds, syncInterval);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        Map<String, DataSourceSyncMetadata> metadataMap = engine.getDataSourceMetadataInternal();
        DataSourceSyncMetadata metadata = metadataMap.get(dsName);

        assertThat(metadata).isNotNull();
        assertThat(metadata.isStreamingDataSource()).isTrue();

        // For ZERO interval, metadata should have Duration.ZERO
        assertThat(metadata.getSyncInterval()).isEqualTo(Duration.ZERO);

        // Force shouldSync to be true
        metadata.updateNextSyncTime(LocalDateTime.now().minusMinutes(10));

        // Call checkAndTriggerSync — record fetchAll count immediately before and after.
        // Since checkAndTriggerSync is synchronous, any fetchAll it triggers happens within
        // the invoke itself. No Thread.sleep needed — avoids flakiness from background scheduler.
        Method checkAndTriggerSync = DataSynchronizationEngine.class
                .getDeclaredMethod("checkAndTriggerSync");
        checkAndTriggerSync.setAccessible(true);

        int fetchAllCountBefore = ds.getFetchAllCallCount();
        checkAndTriggerSync.invoke(engine);
        int fetchAllCountAfter = ds.getFetchAllCallCount();

        // synchronizeDataSource should NOT have been called — fetchAll count should not increase
        assertThat(fetchAllCountAfter)
                .as("synchronizeDataSource should NOT be called for streaming DS with ZERO interval. " +
                        "fetchAllCount before=%d, after=%d", fetchAllCountBefore, fetchAllCountAfter)
                .isEqualTo(fetchAllCountBefore);
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Duration> positiveSyncIntervals() {
        return Arbitraries.longs()
                .between(1, 30)
                .map(minutes -> Duration.ofMinutes(minutes));
    }

    @Provide
    Arbitrary<Duration> zeroOrNullIntervals() {
        return Arbitraries.just(Duration.ZERO);
    }

    // ==================== Stub Streaming DataSource ====================

    static class StubStreamingDataSource implements StreamingDataSource<TestEntity> {
        private final String name;
        private final AtomicInteger fetchAllCallCount = new AtomicInteger(0);

        StubStreamingDataSource(String name) {
            this.name = name;
        }

        int getFetchAllCallCount() {
            return fetchAllCallCount.get();
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<TestEntity> getEntityType() { return TestEntity.class; }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAll() {
            fetchAllCallCount.incrementAndGet();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<TestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<TestEntity> listener) { }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<TestEntity> listener) { }
    }

    static class TestEntity implements Identifiable<Integer> {
        private final int id;

        TestEntity(int id) {
            this.id = id;
        }

        @Override
        public Integer getIdentity() { return id; }
    }
}
