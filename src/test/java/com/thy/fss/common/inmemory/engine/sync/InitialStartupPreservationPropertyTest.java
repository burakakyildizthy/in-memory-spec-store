package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Preservation Property Test — Initial Startup Flow.
 *
 * <p>This test verifies that the initial startup flow
 * ({@code initializeStreamingInfrastructure()}) is completely unaffected by the
 * reconnection bugfix. The fix only changed {@code attemptReconnection()}, so the
 * register → fetchAll → READY flow must remain identical.</p>
 *
 * <p>Test approach:
 * <ol>
 *   <li>Register a datasource — state must be INITIALIZING, healthy=true</li>
 *   <li>Complete initial load — state must transition to READY</li>
 *   <li>Datasource must be healthy with zero consecutive failures</li>
 *   <li>No timeout should fire during normal initial load</li>
 * </ol>
 *
 * <p><b>Validates: Requirements 3.1</b></p>
 */
class InitialStartupPreservationPropertyTest {

    /**
     * Property: For any datasource name, the initial startup flow
     * (register → handleInitialLoadComplete) must transition the state from
     * INITIALIZING to READY, with the datasource marked as healthy and zero
     * consecutive failures.
     *
     * <p>This is the core preservation property: the bugfix must not alter the
     * initial startup lifecycle in any way.</p>
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 50)
    void initialStartupFlowTransitionsFromInitializingToReady(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register datasource — state must be INITIALIZING
        manager.register(dsName);

        assertThat(manager.getState(dsName))
                .as("After register(), state must be INITIALIZING")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        assertThat(manager.isHealthy(dsName))
                .as("After register(), datasource must be healthy (initial state)")
                .isTrue();

        assertThat(manager.isRegistered(dsName))
                .as("After register(), datasource must be registered")
                .isTrue();

        // 2. Complete initial load (simulates successful fetchAll) — state must be READY
        manager.handleInitialLoadComplete(dsName);

        assertThat(manager.getState(dsName))
                .as("After handleInitialLoadComplete(), state must be READY")
                .isEqualTo(StreamingDataSourceState.READY);

        // 3. Datasource must be healthy with zero failures
        assertThat(manager.isHealthy(dsName))
                .as("After initial load complete, datasource must be healthy")
                .isTrue();

        assertThat(manager.getConsecutiveFailures(dsName))
                .as("After initial load complete, consecutive failures must be zero")
                .isEqualTo(0);
    }

    /**
     * Property: For any datasource undergoing initial startup, no timeout should
     * fire during the normal INITIALIZING → READY transition. The timeout mechanism
     * must only trigger when the datasource is genuinely stuck.
     *
     * <p>This verifies that {@code checkInitialLoadTimeout()} returns false when
     * the datasource is freshly registered (registeredAt is recent) and also returns
     * false once the datasource reaches READY state.</p>
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 50)
    void noTimeoutDuringNormalInitialStartup(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register datasource
        manager.register(dsName);

        // 2. Immediately after registration, timeout must NOT fire (registeredAt is fresh)
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must not fire immediately after registration — registeredAt is fresh")
                .isFalse();

        // State must still be INITIALIZING (not ERROR)
        assertThat(manager.getState(dsName))
                .as("State must remain INITIALIZING after timeout check returns false")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 3. Complete initial load — state transitions to READY
        manager.handleInitialLoadComplete(dsName);

        // 4. After READY, timeout check must return false (only applies to INITIALIZING state)
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must not fire when state is READY — only applies to INITIALIZING")
                .isFalse();

        // State must still be READY
        assertThat(manager.getState(dsName))
                .as("State must remain READY after timeout check")
                .isEqualTo(StreamingDataSourceState.READY);
    }

    /**
     * Property: For any datasource, the initial startup flow must produce a valid
     * reconnection delay calculation (exponential backoff infrastructure is intact)
     * even though no reconnection is needed. This verifies the backoff mechanism
     * is available and functional after initial startup.
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 50)
    void backoffInfrastructureAvailableAfterInitialStartup(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Normal initial startup flow
        manager.register(dsName);
        manager.handleInitialLoadComplete(dsName);

        // 2. Backoff delay calculation must work (returns base delay with 0 failures)
        Duration delay = manager.calculateNextReconnectDelay(dsName);
        assertThat(delay)
                .as("Backoff delay calculation must be available after initial startup")
                .isNotNull();

        // 3. State must still be READY — calculating delay must not change state
        assertThat(manager.getState(dsName))
                .as("Calculating reconnect delay must not change state")
                .isEqualTo(StreamingDataSourceState.READY);
    }
}
