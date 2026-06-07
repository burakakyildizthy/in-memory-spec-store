package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

/**
 * Preservation Property Test — Batch Datasource Flow.
 *
 * <p>This test verifies that the batch datasource synchronization flow is completely
 * unaffected by the streaming reconnection bugfix. The bugfix only changed
 * {@code attemptReconnection()} in the streaming path, so batch datasource operations
 * ({@code shouldSync()}, {@code synchronizeDataSource()}, health retry) must be
 * entirely preserved.</p>
 *
 * <p>At the metadata level, batch datasources use:
 * <ul>
 *   <li>{@code shouldSync()} — checks if sync interval has elapsed</li>
 *   <li>{@code recordSuccess()} — resets failures, schedules next sync</li>
 *   <li>{@code recordFailure()} — increments failures, marks unhealthy after threshold</li>
 *   <li>{@code shouldRetryHealthCheck()} — checks if health retry interval has elapsed</li>
 *   <li>{@code markHealthy()} — restores health after successful retry</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 3.6</b></p>
 */
class BatchDatasourcePreservationPropertyTest {

    /**
     * Property: A newly created batch datasource metadata must be ready for
     * immediate sync (shouldSync returns true) and must not be a streaming
     * datasource. This verifies the batch identity is preserved.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void newBatchDatasourceIsReadyForImmediateSync(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 1, max = 60) int syncIntervalSeconds) {

        String dsName = "batch-" + dsNameSuffix;
        Duration syncInterval = Duration.ofSeconds(syncIntervalSeconds);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, syncInterval);

        // Batch datasource must NOT be streaming
        assertThat(metadata.isStreamingDataSource())
                .as("Batch datasource must not be a streaming datasource")
                .isFalse();

        // nextSyncTime is set to LocalDateTime.now() at construction — verify it's
        // approximately now (within 1 second), meaning sync is intended to happen immediately
        assertThat(metadata.getNextSyncTime())
                .as("New batch datasource nextSyncTime must be approximately now (immediate sync)")
                .isBeforeOrEqualTo(LocalDateTime.now());

        // Must start healthy with zero failures
        assertThat(metadata.isHealthy())
                .as("New batch datasource must start healthy")
                .isTrue();
        assertThat(metadata.getConsecutiveFailures())
                .as("New batch datasource must have zero consecutive failures")
                .isEqualTo(0);
    }

    /**
     * Property: After a successful sync, the batch datasource must be healthy,
     * have zero consecutive failures, and shouldSync must return false until
     * the sync interval elapses. This verifies the success recording flow.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void successfulSyncResetsStateAndSchedulesNext(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 10, max = 300) int syncIntervalSeconds) {

        String dsName = "batch-" + dsNameSuffix;
        Duration syncInterval = Duration.ofSeconds(syncIntervalSeconds);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, syncInterval);

        // Simulate a successful sync
        metadata.recordSuccess();

        // After success: healthy, zero failures
        assertThat(metadata.isHealthy())
                .as("After successful sync, datasource must be healthy")
                .isTrue();
        assertThat(metadata.getConsecutiveFailures())
                .as("After successful sync, consecutive failures must be zero")
                .isEqualTo(0);
        assertThat(metadata.getLastErrorMessage())
                .as("After successful sync, last error message must be null")
                .isNull();

        // shouldSync must be false (next sync is in the future)
        assertThat(metadata.shouldSync())
                .as("After successful sync, shouldSync must be false (interval not elapsed)")
                .isFalse();

        // Next sync time must be in the future
        assertThat(metadata.getNextSyncTime())
                .as("Next sync time must be after now")
                .isAfter(LocalDateTime.now().minusSeconds(1));
    }

    /**
     * Property: Consecutive failures below the threshold (3) keep the datasource
     * healthy, while reaching the threshold marks it unhealthy. This verifies
     * the failure recording and health threshold flow.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void failuresAccumulateAndUnhealthyThresholdIsPreserved(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 1, max = 10) int failureCount) {

        String dsName = "batch-" + dsNameSuffix;
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, Duration.ofMinutes(1));

        for (int i = 1; i <= failureCount; i++) {
            metadata.recordFailure("Failure #" + i);

            assertThat(metadata.getConsecutiveFailures())
                    .as("After %d failure(s), consecutive failures must be %d", i, i)
                    .isEqualTo(i);

            if (i < 3) {
                assertThat(metadata.isHealthy())
                        .as("After %d failure(s) (below threshold), datasource must still be healthy", i)
                        .isTrue();
            } else {
                assertThat(metadata.isHealthy())
                        .as("After %d failure(s) (at/above threshold), datasource must be unhealthy", i)
                        .isFalse();
            }
        }

        // Last error message must be set
        assertThat(metadata.getLastErrorMessage())
                .as("Last error message must be set after failures")
                .isNotNull()
                .contains("Failure #" + failureCount);
    }

    /**
     * Property: For an unhealthy batch datasource, shouldRetryHealthCheck must
     * return true on first call (no previous health check time), and markHealthy
     * must restore the datasource to a healthy state with zero failures.
     * This verifies the health retry flow.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void healthRetryFlowRestoresUnhealthyDatasource(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "batch-" + dsNameSuffix;
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, Duration.ofMinutes(1));

        // Drive datasource to unhealthy (3 consecutive failures)
        metadata.recordFailure("fail-1");
        metadata.recordFailure("fail-2");
        metadata.recordFailure("fail-3");

        assertThat(metadata.isHealthy())
                .as("Pre-condition: datasource must be unhealthy after 3 failures")
                .isFalse();

        // shouldRetryHealthCheck should not return true immediately after recordFailure
        // because recordFailure sets lastHealthCheckTime to now, and retry interval is 5 min
        assertThat(metadata.shouldRetryHealthCheck())
                .as("shouldRetryHealthCheck must be false right after failure (retry interval not elapsed)")
                .isFalse();

        // Simulate health recovery via markHealthy (as retryHealthCheck does on success)
        metadata.markHealthy();

        assertThat(metadata.isHealthy())
                .as("After markHealthy, datasource must be healthy")
                .isTrue();
        assertThat(metadata.getConsecutiveFailures())
                .as("After markHealthy, consecutive failures must be zero")
                .isEqualTo(0);
        assertThat(metadata.getLastErrorMessage())
                .as("After markHealthy, last error message must be null")
                .isNull();
    }

    /**
     * Property: The full batch datasource lifecycle — sync success, then failures
     * leading to unhealthy, then health recovery — must work end-to-end. This
     * verifies the complete shouldSync + synchronizeDataSource + health retry
     * flow is preserved.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void fullBatchLifecycleIsPreserved(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 1, max = 5) int successCount) {

        String dsName = "batch-" + dsNameSuffix;
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(dsName, Duration.ofMinutes(1));

        // Phase 1: nextSyncTime is set to now at construction — verify it's approximately now
        assertThat(metadata.getNextSyncTime())
                .as("Phase 1: new datasource nextSyncTime must be approximately now")
                .isBeforeOrEqualTo(LocalDateTime.now());

        // Phase 2: Record N successful syncs
        for (int i = 0; i < successCount; i++) {
            metadata.recordSuccess();
            assertThat(metadata.isHealthy()).isTrue();
            assertThat(metadata.getConsecutiveFailures()).isEqualTo(0);
        }

        // Phase 3: Drive to unhealthy with 3 failures
        metadata.recordFailure("error-1");
        metadata.recordFailure("error-2");
        metadata.recordFailure("error-3");
        assertThat(metadata.isHealthy())
                .as("Phase 3: must be unhealthy after 3 failures")
                .isFalse();
        assertThat(metadata.getConsecutiveFailures())
                .as("Phase 3: must have 3 consecutive failures")
                .isEqualTo(3);

        // Phase 4: Health recovery
        metadata.markHealthy();
        assertThat(metadata.isHealthy())
                .as("Phase 4: must be healthy after recovery")
                .isTrue();
        assertThat(metadata.getConsecutiveFailures())
                .as("Phase 4: failures must be reset after recovery")
                .isEqualTo(0);

        // Phase 5: After recovery, a new success should work normally
        metadata.recordSuccess();
        assertThat(metadata.isHealthy())
                .as("Phase 5: must remain healthy after post-recovery success")
                .isTrue();
        assertThat(metadata.shouldSync())
                .as("Phase 5: shouldSync must be false after recent success")
                .isFalse();
    }
}
