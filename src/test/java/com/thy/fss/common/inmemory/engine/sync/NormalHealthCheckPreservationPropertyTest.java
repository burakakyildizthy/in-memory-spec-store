package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Preservation Property Test — Normal Health Check Flow.
 *
 * <p>This test verifies that the normal health check flow is completely unaffected
 * by the reconnection bugfix. When a streaming datasource is in READY state and
 * healthy, {@code checkStreamingDataSourceHealth()} should take no action: no
 * reconnection triggered, no state change, no failure counter increment.</p>
 *
 * <p>At the lifecycle manager level, this means:
 * <ul>
 *   <li>{@code checkInitialLoadTimeout()} returns false (state is READY, not INITIALIZING)</li>
 *   <li>State remains READY</li>
 *   <li>Datasource remains healthy</li>
 *   <li>Consecutive failures remain at zero</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 3.2</b></p>
 */
class NormalHealthCheckPreservationPropertyTest {

    /**
     * Property: For any datasource in READY state with healthy=true, a health check
     * cycle (checkInitialLoadTimeout) must take no action — state stays READY,
     * datasource stays healthy, consecutive failures stay at zero.
     *
     * <p>This simulates what {@code checkStreamingDataSourceHealth()} does when
     * state is READY and the datasource is healthy: it checks for INITIALIZING
     * timeout (returns false since state != INITIALIZING), then checks health
     * (healthy=true so no branch is taken). Net effect: nothing changes.</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void healthCheckOnReadyHealthyDatasourceTakesNoAction(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: bring datasource to READY state via normal startup flow
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);

        // Pre-conditions: state is READY and healthy
        assertThat(manager.getState(dsName))
                .as("Pre-condition: state must be READY")
                .isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(dsName))
                .as("Pre-condition: datasource must be healthy")
                .isTrue();

        // Simulate health check: checkInitialLoadTimeout must return false
        // (state is READY, not INITIALIZING — timeout check is skipped)
        boolean timeoutFired = manager.checkInitialLoadTimeout(dsName);

        assertThat(timeoutFired)
                .as("checkInitialLoadTimeout must return false when state is READY")
                .isFalse();

        // Post-conditions: nothing changed
        assertThat(manager.getState(dsName))
                .as("State must remain READY after health check")
                .isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(dsName))
                .as("Datasource must remain healthy after health check")
                .isTrue();
        assertThat(manager.getConsecutiveFailures(dsName))
                .as("Consecutive failures must remain zero after health check")
                .isEqualTo(0);
    }

    /**
     * Property: For any datasource in READY state, repeated health check cycles
     * must all be no-ops. This verifies that consecutive health checks on a healthy
     * READY datasource never accumulate side effects.
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void repeatedHealthChecksOnReadyDatasourceAreAllNoOps(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 2, max = 20) int healthCheckCount) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: bring datasource to READY state
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);

        // Run multiple health check cycles — all must be no-ops
        for (int i = 0; i < healthCheckCount; i++) {
            boolean timeoutFired = manager.checkInitialLoadTimeout(dsName);

            assertThat(timeoutFired)
                    .as("Health check #%d: checkInitialLoadTimeout must return false", i + 1)
                    .isFalse();
        }

        // After all health checks, state must still be READY and healthy
        assertThat(manager.getState(dsName))
                .as("State must remain READY after %d health checks", healthCheckCount)
                .isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(dsName))
                .as("Datasource must remain healthy after %d health checks", healthCheckCount)
                .isTrue();
        assertThat(manager.getConsecutiveFailures(dsName))
                .as("Consecutive failures must remain zero after %d health checks", healthCheckCount)
                .isEqualTo(0);
    }

    /**
     * Property: For any datasource that went through a full reconnection cycle
     * and returned to READY state, subsequent health checks must still be no-ops.
     * This verifies that the reconnection bugfix does not leave any residual state
     * that would cause health checks to misbehave after recovery.
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void healthCheckAfterReconnectionRecoveryIsStillNoOp(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Normal startup → READY
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);

        // 2. Simulate connection loss → ERROR
        manager.handleConnectionLoss(dsName, "Simulated connection loss");
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // 3. Simulate successful reconnection (fixed flow)
        manager.handleReconnectionSuccess(dsName);
        manager.resetRegistrationTime(dsName);
        manager.handleInitialLoadComplete(dsName);

        // 4. Back to READY — health check must be a no-op
        assertThat(manager.getState(dsName))
                .as("Pre-condition: state must be READY after reconnection recovery")
                .isEqualTo(StreamingDataSourceState.READY);

        boolean timeoutFired = manager.checkInitialLoadTimeout(dsName);

        assertThat(timeoutFired)
                .as("checkInitialLoadTimeout must return false after reconnection recovery")
                .isFalse();

        assertThat(manager.getState(dsName))
                .as("State must remain READY after health check post-reconnection")
                .isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(dsName))
                .as("Datasource must remain healthy after health check post-reconnection")
                .isTrue();
        assertThat(manager.getConsecutiveFailures(dsName))
                .as("Consecutive failures must remain zero after health check post-reconnection")
                .isEqualTo(0);
    }
}
