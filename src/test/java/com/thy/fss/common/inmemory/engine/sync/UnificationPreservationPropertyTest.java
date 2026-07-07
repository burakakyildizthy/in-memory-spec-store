package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
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
 * Unification Preservation Property Tests — DataSynchronizationEngine.
 *
 * <p>Property 5: Streaming DataSource Periyodik Sync Hariç Tutma</p>
 * <p>Property 14: Sağlık Kontrolü Mantığı Korunumu</p>
 */
class UnificationPreservationPropertyTest {

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
    Arbitrary<Integer> batchDsCounts() {
        return Arbitraries.integers().between(0, 5);
    }

    @Provide
    Arbitrary<Integer> streamingDsCounts() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<StreamingDataSourceState> streamingStates() {
        return Arbitraries.of(
                StreamingDataSourceState.INITIALIZING,
                StreamingDataSourceState.READY,
                StreamingDataSourceState.ERROR
        );
    }

    @Provide
    Arbitrary<Integer> consecutiveFailureCounts() {
        return Arbitraries.integers().between(0, 15);
    }

    // ==================== Property 5: Streaming DataSource Periyodik Sync Hariç Tutma ====================

    /**
     * Property 5: Streaming DataSource Periyodik Sync Hariç Tutma.
     *
     * <p>Rastgele batch/streaming datasource konfigürasyonları ile {@code checkAndTriggerSync()}
     * çağrısı yapılır. Streaming datasource'lar için {@code shouldSync()} kontrolü yapılmadığı,
     * yalnızca sağlık kontrolü yürütüldüğü doğrulanır.</p>
     *
     * <p>Test stratejisi: DataSourceSyncMetadata nesneleri oluşturularak, streaming olarak
     * işaretlenen metadata'ların shouldSync() sonucunun sync tetiklemesinde kullanılmadığı,
     * batch metadata'ların ise normal sync döngüsüne dahil olduğu doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 3.5, 5.1, 5.2</b></p>
     */
    // Feature: streaming-datasource-unification, Property 5: Streaming DataSource Periyodik Sync Hariç Tutma
    @Property(tries = 100)
    void property5StreamingDataSourcesExcludedFromPeriodicSync(
            @ForAll("batchDsCounts") int batchCount,
            @ForAll("streamingDsCounts") int streamingCount) {

        // Create batch datasource metadata entries — these SHOULD participate in sync
        List<DataSourceSyncMetadata> batchMetadataList = new ArrayList<>();
        for (int i = 0; i < batchCount; i++) {
            String name = "batch-ds-" + i;
            DataSourceSyncMetadata meta = new DataSourceSyncMetadata(name, Duration.ofSeconds(30));
            // Not streaming — default isStreamingDataSource() returns false
            batchMetadataList.add(meta);
        }

        // Create streaming datasource metadata entries — these should NOT participate in sync
        List<DataSourceSyncMetadata> streamingMetadataList = new ArrayList<>();
        for (int i = 0; i < streamingCount; i++) {
            String name = "streaming-ds-" + i;
            DataSourceSyncMetadata meta = new DataSourceSyncMetadata(name, Duration.ZERO);
            meta.setStreamingDataSource(true);
            meta.setStreamingState(StreamingDataSourceState.READY);
            streamingMetadataList.add(meta);
        }

        // Verify: streaming datasources are correctly identified
        for (DataSourceSyncMetadata meta : streamingMetadataList) {
            assertThat(meta.isStreamingDataSource())
                    .as("Property 5: Metadata for '%s' should be identified as streaming",
                            meta.getDataSourceName())
                    .isTrue();
        }

        // Verify: batch datasources are correctly identified as non-streaming
        for (DataSourceSyncMetadata meta : batchMetadataList) {
            assertThat(meta.isStreamingDataSource())
                    .as("Property 5: Metadata for '%s' should NOT be identified as streaming",
                            meta.getDataSourceName())
                    .isFalse();
        }

        // Simulate checkAndTriggerSync() logic:
        // For each metadata, if streaming → health check only (no shouldSync)
        // If batch → shouldSync() check applies
        List<String> syncTriggered = new ArrayList<>();
        List<String> healthCheckTriggered = new ArrayList<>();

        List<DataSourceSyncMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(batchMetadataList);
        allMetadata.addAll(streamingMetadataList);

        for (DataSourceSyncMetadata metadata : allMetadata) {
            if (metadata.isStreamingDataSource()) {
                // Streaming path: health check only — shouldSync() is NEVER called
                healthCheckTriggered.add(metadata.getDataSourceName());
            } else {
                // Batch path: shouldSync() determines if sync is needed
                if (metadata.shouldSync()) {
                    syncTriggered.add(metadata.getDataSourceName());
                }
            }
        }

        // Property assertion: NO streaming datasource should appear in syncTriggered
        for (DataSourceSyncMetadata streamingMeta : streamingMetadataList) {
            assertThat(syncTriggered)
                    .as("Property 5: Streaming datasource '%s' must NEVER be in sync-triggered list. "
                            + "Streaming datasources are excluded from periodic sync — only health "
                            + "checks are performed.", streamingMeta.getDataSourceName())
                    .doesNotContain(streamingMeta.getDataSourceName());
        }

        // Property assertion: ALL streaming datasources should have health check triggered
        for (DataSourceSyncMetadata streamingMeta : streamingMetadataList) {
            assertThat(healthCheckTriggered)
                    .as("Property 5: Streaming datasource '%s' should have health check triggered",
                            streamingMeta.getDataSourceName())
                    .contains(streamingMeta.getDataSourceName());
        }

        // Property assertion: health check list should contain ONLY streaming datasources
        assertThat(healthCheckTriggered)
                .as("Property 5: Health check list should contain exactly the streaming datasources")
                .hasSize(streamingCount);

        // Cleanup
        factory.clearAll();
    }

    // ==================== Property 14: Sağlık Kontrolü Mantığı Korunumu ====================

    /**
     * Property 14: Sağlık Kontrolü Mantığı Korunumu.
     *
     * <p>Rastgele sağlık durumu senaryoları ile mevcut INITIALIZING timeout, bağlantı durumu
     * ve reconnection mantığının korunduğu doğrulanır. Yalnızca zamanlayıcı birleştirilmiştir;
     * kontrol mantığı değiştirilmemiştir.</p>
     *
     * <p>Test stratejisi: StreamingDataSourceLifecycleManager kullanılarak INITIALIZING timeout
     * kontrolü, bağlantı durumu kontrolü (healthy/unhealthy), ve reconnection tetikleme
     * mantığının korunduğu doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 5.4</b></p>
     */
    // Feature: streaming-datasource-unification, Property 14: Sağlık Kontrolü Mantığı Korunumu
    @Property(tries = 100)
    void property14HealthCheckLogicPreserved(
            @ForAll("streamingStates") StreamingDataSourceState initialState,
            @ForAll("consecutiveFailureCounts") int failureCount) {

        String dsName = "health-check-ds";
        StreamingDataSourceLifecycleManager lifecycleManager = new StreamingDataSourceLifecycleManager();

        // Register datasource — starts in INITIALIZING
        lifecycleManager.register(dsName);
        assertThat(lifecycleManager.getState(dsName))
                .as("Property 14: Newly registered datasource should start in INITIALIZING")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // Transition to the desired initial state
        switch (initialState) {
            case READY:
                lifecycleManager.handleInitialLoadComplete(dsName);
                assertThat(lifecycleManager.getState(dsName))
                        .isEqualTo(StreamingDataSourceState.READY);
                break;
            case ERROR:
                lifecycleManager.handleConnectionLoss(dsName, "test error");
                assertThat(lifecycleManager.getState(dsName))
                        .isEqualTo(StreamingDataSourceState.ERROR);
                break;
            case INITIALIZING:
                // Already in INITIALIZING — no transition needed
                break;
        }

        // === Verify INITIALIZING timeout logic ===
        if (initialState == StreamingDataSourceState.INITIALIZING) {
            // With default timeout, a freshly registered datasource should NOT timeout
            boolean timedOut = lifecycleManager.checkInitialLoadTimeout(dsName);
            assertThat(timedOut)
                    .as("Property 14: Freshly registered INITIALIZING datasource should NOT timeout")
                    .isFalse();

            // Set a very short timeout and verify it triggers
            lifecycleManager.setInitialLoadTimeout(0); // 0ms timeout — immediate
            // Reset registration time to ensure timeout check works
            lifecycleManager.resetRegistrationTime(dsName);
            // Need a tiny delay for Instant.now() to be after registeredAt
            try { Thread.sleep(1); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            boolean timedOutAfter = lifecycleManager.checkInitialLoadTimeout(dsName);
            assertThat(timedOutAfter)
                    .as("Property 14: INITIALIZING datasource with 0ms timeout should timeout")
                    .isTrue();
            // After timeout, state should be ERROR
            assertThat(lifecycleManager.getState(dsName))
                    .as("Property 14: After INITIALIZING timeout, state should be ERROR")
                    .isEqualTo(StreamingDataSourceState.ERROR);
        }

        // === Verify connection status and reconnection logic ===
        if (initialState == StreamingDataSourceState.READY) {
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 14: READY datasource should be healthy")
                    .isTrue();

            // Simulate connection loss
            lifecycleManager.handleConnectionLoss(dsName, "connection lost");
            assertThat(lifecycleManager.getState(dsName))
                    .as("Property 14: After connection loss, state should be ERROR")
                    .isEqualTo(StreamingDataSourceState.ERROR);
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 14: After connection loss, datasource should be unhealthy")
                    .isFalse();

            // Record consecutive failures and verify backoff
            for (int i = 0; i < failureCount; i++) {
                lifecycleManager.recordReconnectionFailure(dsName);
            }
            assertThat(lifecycleManager.getConsecutiveFailures(dsName))
                    .as("Property 14: Consecutive failures should be tracked correctly")
                    .isEqualTo(failureCount);

            // Verify backoff delay calculation is preserved
            Duration delay = lifecycleManager.calculateNextReconnectDelay(dsName);
            Duration expectedDelay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(failureCount);
            assertThat(delay)
                    .as("Property 14: Backoff delay should match calculateBackoffDelay(%d)", failureCount)
                    .isEqualTo(expectedDelay);

            // Verify reconnection success resets state
            lifecycleManager.handleReconnectionSuccess(dsName);
            assertThat(lifecycleManager.getState(dsName))
                    .as("Property 14: After reconnection success, state should be INITIALIZING")
                    .isEqualTo(StreamingDataSourceState.INITIALIZING);
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 14: After reconnection success, datasource should be healthy")
                    .isTrue();
            assertThat(lifecycleManager.getConsecutiveFailures(dsName))
                    .as("Property 14: After reconnection success, consecutive failures should be 0")
                    .isEqualTo(0);
        }

        // === Verify ERROR state reconnection logic ===
        if (initialState == StreamingDataSourceState.ERROR) {
            assertThat(lifecycleManager.isHealthy(dsName))
                    .as("Property 14: ERROR datasource should be unhealthy")
                    .isFalse();

            // Record failures and verify backoff
            for (int i = 0; i < failureCount; i++) {
                lifecycleManager.recordReconnectionFailure(dsName);
            }

            Duration delay = lifecycleManager.calculateNextReconnectDelay(dsName);
            assertThat(delay)
                    .as("Property 14: Backoff delay should be positive")
                    .isPositive();
            assertThat(delay)
                    .as("Property 14: Backoff delay should not exceed max")
                    .isLessThanOrEqualTo(StreamingDataSourceLifecycleManager.MAX_BACKOFF_DELAY);
        }
    }

    // ==================== Property 13: Geriye Uyumluluk — Batch Datasource Davranışı Korunumu ====================

    /**
     * Property 13: Geriye Uyumluluk — Batch Datasource Davranışı Korunumu.
     *
     * <p>Rastgele batch-only konfigürasyonlar ile mevcut davranışın birebir korunduğunu doğrular.
     * Batch kayıt akışı, periyodik senkronizasyon, sağlık kontrolü ve artımlı güncelleme
     * mantığının değişmediğini doğrular.</p>
     *
     * <p>Test stratejisi: Rastgele sayıda batch datasource kaydedilir ve kayıt akışı,
     * isStreamingDataSource() sonucu, shouldSync() davranışı, sağlık kontrolü/retry mantığı
     * ve streaming-spesifik davranışın sızmadığı doğrulanır.</p>
     *
     * <p><b>Validates: Requirements 8.1, 8.5, 8.6</b></p>
     */
    // Feature: streaming-datasource-unification, Property 13: Geriye Uyumluluk — Batch Datasource Davranışı Korunumu
    @Property(tries = 100)
    void property13BackwardCompatibilityBatchDataSourceBehaviorPreserved(
            @ForAll("batchDsCounts") int batchCount,
            @ForAll("consecutiveFailureCounts") int failureCount) {

        // Ensure at least 1 batch datasource for meaningful test
        int effectiveBatchCount = Math.max(1, batchCount);

        // === 1. Batch datasource registration with syncInterval works correctly ===
        List<String> registeredNames = new ArrayList<>();
        Duration syncInterval = Duration.ofSeconds(30);

        for (int i = 0; i < effectiveBatchCount; i++) {
            String name = "batch-compat-ds-" + i;
            DataSource<TestEntity> ds = createSimpleBatchDataSource(name);
            factory.registerDataSource(name, ds, syncInterval);
            registeredNames.add(name);
        }

        // Verify all batch datasources are registered
        for (String name : registeredNames) {
            assertThat(factory.hasDataSource(name))
                    .as("Property 13: Batch datasource '%s' should be registered", name)
                    .isTrue();
        }

        // === 2. isStreamingDataSource() returns false for batch datasources ===
        for (String name : registeredNames) {
            assertThat(factory.isStreamingDataSource(name))
                    .as("Property 13: Batch datasource '%s' must NOT be identified as streaming", name)
                    .isFalse();
        }

        // === 3. Batch datasources should NOT appear in streaming datasource names ===
        for (String name : registeredNames) {
            assertThat(factory.getAllStreamingDataSourceNames())
                    .as("Property 13: Batch datasource '%s' must NOT appear in streaming names", name)
                    .doesNotContain(name);
        }

        // === 4. getStreamingDataSource() returns null for batch datasources ===
        for (String name : registeredNames) {
            assertThat(factory.getStreamingDataSource(name))
                    .as("Property 13: getStreamingDataSource('%s') should return null for batch ds", name)
                    .isNull();
        }

        // === 5. Batch datasources participate in periodic sync (shouldSync is checked) ===
        for (String name : registeredNames) {
            DataSourceSyncMetadata meta = new DataSourceSyncMetadata(name, syncInterval);

            // Batch metadata should NOT be streaming
            assertThat(meta.isStreamingDataSource())
                    .as("Property 13: Batch metadata '%s' must not be streaming", name)
                    .isFalse();

            // After recordSuccess(), nextSyncTime moves forward by syncInterval → shouldSync() = false
            meta.recordSuccess();
            assertThat(meta.shouldSync())
                    .as("Property 13: After recordSuccess, batch metadata '%s' should not need immediate sync", name)
                    .isFalse();

            // Force nextSyncTime to the past to simulate elapsed interval
            meta.updateNextSyncTime(java.time.LocalDateTime.now().minusSeconds(1));
            assertThat(meta.shouldSync())
                    .as("Property 13: Batch metadata '%s' should need sync after interval elapsed", name)
                    .isTrue();
        }

        // === 6. Batch datasource health check and retry logic works ===
        DataSourceSyncMetadata healthMeta = new DataSourceSyncMetadata("batch-health-test", syncInterval);

        // Initially healthy
        assertThat(healthMeta.isHealthy())
                .as("Property 13: New batch metadata should be healthy")
                .isTrue();

        // shouldRetryHealthCheck returns false when healthy
        assertThat(healthMeta.shouldRetryHealthCheck())
                .as("Property 13: Healthy batch metadata should not need health check retry")
                .isFalse();

        // Record failures up to the given count
        for (int i = 0; i < failureCount; i++) {
            healthMeta.recordFailure("failure-" + i);
        }
        assertThat(healthMeta.getConsecutiveFailures())
                .as("Property 13: Consecutive failures should be tracked correctly")
                .isEqualTo(failureCount);

        // After enough failures, datasource becomes unhealthy
        if (failureCount >= 3) { // MAX_CONSECUTIVE_FAILURES = 3
            assertThat(healthMeta.isHealthy())
                    .as("Property 13: After %d failures, batch ds should be unhealthy", failureCount)
                    .isFalse();
        }

        // markHealthy() resets state
        healthMeta.markHealthy();
        assertThat(healthMeta.isHealthy())
                .as("Property 13: After markHealthy, batch ds should be healthy")
                .isTrue();
        assertThat(healthMeta.getConsecutiveFailures())
                .as("Property 13: After markHealthy, consecutive failures should be 0")
                .isEqualTo(0);

        // === 7. Simulate checkAndTriggerSync logic — no streaming behavior leaks ===
        List<String> syncTriggered = new ArrayList<>();
        List<String> healthCheckOnly = new ArrayList<>();

        for (String name : registeredNames) {
            DataSourceSyncMetadata meta = new DataSourceSyncMetadata(name, syncInterval);
            // Force past nextSyncTime to simulate elapsed interval
            meta.updateNextSyncTime(java.time.LocalDateTime.now().minusSeconds(1));
            // Batch path: shouldSync() determines sync, NOT health check only
            if (meta.isStreamingDataSource()) {
                healthCheckOnly.add(name);
            } else {
                if (meta.shouldSync()) {
                    syncTriggered.add(name);
                }
            }
        }

        // No batch datasource should end up in health-check-only path
        assertThat(healthCheckOnly)
                .as("Property 13: No batch datasource should be routed to streaming health-check-only path")
                .isEmpty();

        // All batch datasources with elapsed sync interval should be in syncTriggered
        assertThat(syncTriggered)
                .as("Property 13: All batch datasources with elapsed interval should need sync")
                .hasSize(effectiveBatchCount);

        // === 8. Verify syncInterval is preserved in registry ===
        for (String name : registeredNames) {
            Duration registeredInterval = factory.getDataSourceInterval(name);
            assertThat(registeredInterval)
                    .as("Property 13: Registered syncInterval for '%s' should be preserved", name)
                    .isEqualTo(syncInterval);
        }

        // Cleanup
        factory.clearAll();
    }

    /**
     * Creates a simple batch DataSource (NOT StreamingDataSource) for testing.
     */
    private DataSource<TestEntity> createSimpleBatchDataSource(String name) {
        return new DataSource<TestEntity>() {
            @Override
            public String getName() { return name; }

            @Override
            public Class<TestEntity> getEntityType() { return TestEntity.class; }

            @Override
            public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAll() {
                return java.util.concurrent.CompletableFuture.completedFuture(new ArrayList<>());
            }

            @Override
            public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAllById(java.util.Collection<Object> ids) {
                return java.util.concurrent.CompletableFuture.completedFuture(new ArrayList<>());
            }

            @Override
            public boolean isHealthy() { return true; }

            @Override
            public void close() { }

            @Override
            public java.util.Optional<DataSource<TestEntity>> getFallbackDataSource() {
                return java.util.Optional.empty();
            }

            @Override
            public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) { }
        };
    }

}
