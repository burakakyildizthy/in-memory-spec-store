package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Preservation Property Test — Unhealthy Transition and Reconnection Failure Flow.
 *
 * <p>This test verifies that the unhealthy transition and reconnection failure flows
 * are completely unaffected by the reconnection bugfix. Specifically:</p>
 * <ul>
 *   <li>When a datasource becomes unhealthy, {@code handleConnectionLoss()} transitions
 *       state to ERROR and marks the datasource as not healthy (Requirement 3.3)</li>
 *   <li>When reconnection fails, {@code recordReconnectionFailure()} increments
 *       consecutive failures and {@code calculateNextReconnectDelay()} returns
 *       increasing delays via exponential backoff (Requirement 3.4)</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 3.3, 3.4</b></p>
 */
class UnhealthyTransitionPreservationPropertyTest {

    /**
     * Property: For any healthy READY datasource, handleConnectionLoss() must
     * transition state to ERROR and mark the datasource as not healthy.
     * This is the preserved unhealthy transition flow (Requirement 3.3).
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 50)
    void connectionLossTransitionsReadyDatasourceToError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: bring datasource to READY state via normal startup
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);

        // Pre-conditions
        assertThat(manager.getState(dsName))
                .as("Pre-condition: state must be READY")
                .isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(dsName))
                .as("Pre-condition: datasource must be healthy")
                .isTrue();

        // Act: simulate connection loss (unhealthy transition)
        manager.handleConnectionLoss(dsName, "Simulated network failure");

        // Assert: state must be ERROR and datasource must not be healthy
        assertThat(manager.getState(dsName))
                .as("After connection loss, state must be ERROR")
                .isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(manager.isHealthy(dsName))
                .as("After connection loss, datasource must not be healthy")
                .isFalse();
    }

    /**
     * Property: For any datasource in ERROR state, recordReconnectionFailure()
     * must increment consecutive failures. Each call increases the counter by 1.
     * This is the preserved reconnection failure tracking (Requirement 3.4).
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 50)
    void reconnectionFailureIncrementsConsecutiveFailures(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 1, max = 10) int failureCount) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: bring datasource to ERROR state
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);
        manager.handleConnectionLoss(dsName, "Simulated failure");

        assertThat(manager.getConsecutiveFailures(dsName))
                .as("Pre-condition: consecutive failures must be zero before recording")
                .isEqualTo(0);

        // Act: record N reconnection failures
        for (int i = 1; i <= failureCount; i++) {
            manager.recordReconnectionFailure(dsName);

            assertThat(manager.getConsecutiveFailures(dsName))
                    .as("After %d reconnection failure(s), counter must be %d", i, i)
                    .isEqualTo(i);
        }

        // Assert: datasource remains unhealthy throughout
        assertThat(manager.isHealthy(dsName))
                .as("After reconnection failures, datasource must remain unhealthy")
                .isFalse();
    }

    /**
     * Property: For any number of consecutive reconnection failures,
     * calculateNextReconnectDelay() must return monotonically non-decreasing
     * delays (exponential backoff). This verifies the preserved backoff
     * mechanism through the lifecycle manager's instance method (Requirement 3.4).
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 50)
    void reconnectionFailuresProduceIncreasingDelaysViaBackoff(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 2, max = 15) int totalFailures) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: bring datasource to ERROR state
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);
        manager.handleConnectionLoss(dsName, "Simulated failure");

        Duration previousDelay = Duration.ZERO;

        // Act: record failures one by one and verify delays are non-decreasing
        for (int i = 0; i < totalFailures; i++) {
            manager.recordReconnectionFailure(dsName);
            Duration currentDelay = manager.calculateNextReconnectDelay(dsName);

            assertThat(currentDelay)
                    .as("Delay after failure #%d must be positive", i + 1)
                    .isPositive();
            assertThat(currentDelay)
                    .as("Delay after failure #%d must be >= previous delay (monotonic)", i + 1)
                    .isGreaterThanOrEqualTo(previousDelay);

            previousDelay = currentDelay;
        }
    }

    /**
     * Property: For any datasource that goes through connection loss followed by
     * multiple reconnection failures, the full unhealthy → ERROR → backoff flow
     * must be preserved end-to-end. State stays ERROR, datasource stays unhealthy,
     * and delays increase with each failure.
     *
     * <p><b>Validates: Requirements 3.3, 3.4</b></p>
     */
    @Property(tries = 50)
    void fullUnhealthyToReconnectionFailureFlowIsPreserved(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 1, max = 8) int failureCount) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Normal startup → READY
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.READY);

        // 2. Connection loss → ERROR (Requirement 3.3)
        manager.handleConnectionLoss(dsName, "Network timeout");
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(manager.isHealthy(dsName)).isFalse();

        // 3. Multiple reconnection failures with exponential backoff (Requirement 3.4)
        Duration previousDelay = Duration.ZERO;
        for (int i = 0; i < failureCount; i++) {
            manager.recordReconnectionFailure(dsName);

            // State must remain ERROR throughout
            assertThat(manager.getState(dsName))
                    .as("State must remain ERROR after failure #%d", i + 1)
                    .isEqualTo(StreamingDataSourceState.ERROR);

            // Datasource must remain unhealthy
            assertThat(manager.isHealthy(dsName))
                    .as("Datasource must remain unhealthy after failure #%d", i + 1)
                    .isFalse();

            // Consecutive failures must match
            assertThat(manager.getConsecutiveFailures(dsName))
                    .as("Consecutive failures must be %d after failure #%d", i + 1, i + 1)
                    .isEqualTo(i + 1);

            // Delay must be non-decreasing (exponential backoff preserved)
            Duration delay = manager.calculateNextReconnectDelay(dsName);
            assertThat(delay)
                    .as("Delay must be >= previous delay after failure #%d", i + 1)
                    .isGreaterThanOrEqualTo(previousDelay);

            previousDelay = delay;
        }
    }
}
