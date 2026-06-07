package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Preservation Property Test â€” Real INITIALIZING Timeout Mechanism.
 *
 * <p>This test verifies that the genuine INITIALIZING timeout mechanism is
 * completely unaffected by the reconnection bugfix. When a datasource is
 * truly stuck in INITIALIZING state (not due to a reconnection), the timeout
 * must still fire after the configured period, transitioning state to ERROR.</p>
 *
 * <p>Uses short timeouts (10-100ms) to simulate the real 5-minute timeout quickly.</p>
 *
 * <p><b>Validates: Requirements 3.5</b></p>
 */
class InitializingTimeoutPreservationPropertyTest {

    /**
     * Property: For any datasource that stays in INITIALIZING longer than the
     * configured timeout, {@code checkInitialLoadTimeout()} must return true
     * and transition state to ERROR. This is the preserved timeout mechanism
     * for genuinely stuck datasources.
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     * @param timeoutMs    a short timeout (10-100 ms) to simulate the 5-minute timeout
     */
    @Property(tries = 50)
    void genuinelyStuckDatasourceTriggersTimeout(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 10, max = 100) int timeoutMs) throws InterruptedException {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register datasource â€” state = INITIALIZING, registeredAt = now
        manager.register(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 2. Set a short timeout to simulate the 5-minute window
        manager.setInitialLoadTimeout(timeoutMs);

        // 3. Before timeout elapses, check should return false (not timed out yet)
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must NOT fire before the configured period elapses")
                .isFalse();
        assertThat(manager.getState(dsName))
                .as("State must remain INITIALIZING before timeout")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 4. Wait for the timeout to elapse
        Thread.sleep((long) timeoutMs + 50);

        // 5. Now checkInitialLoadTimeout must detect the timeout
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must fire when datasource is genuinely stuck in INITIALIZING")
                .isTrue();

        // 6. State must have transitioned to ERROR
        assertThat(manager.getState(dsName))
                .as("State must be ERROR after timeout fires")
                .isEqualTo(StreamingDataSourceState.ERROR);
    }

    /**
     * Property: After a genuine timeout fires and state transitions to ERROR,
     * subsequent calls to {@code checkInitialLoadTimeout()} must return false
     * because the state is no longer INITIALIZING.
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     * @param timeoutMs    a short timeout (10-100 ms) to simulate the 5-minute timeout
     */
    @Property(tries = 50)
    void timeoutDoesNotFireAgainAfterStateTransitionsToError(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 10, max = 100) int timeoutMs) throws InterruptedException {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // Setup: register and wait for timeout
        manager.register(dsName);
        manager.setInitialLoadTimeout(timeoutMs);
        Thread.sleep((long) timeoutMs + 50);

        // First call fires the timeout
        assertThat(manager.checkInitialLoadTimeout(dsName)).isTrue();
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // Subsequent calls must return false â€” state is ERROR, not INITIALIZING
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must not fire again once state is ERROR")
                .isFalse();
        assertThat(manager.getState(dsName))
                .as("State must remain ERROR")
                .isEqualTo(StreamingDataSourceState.ERROR);
    }

    /**
     * Property: After a successful reconnection with resetRegistrationTime(),
     * a genuinely stuck datasource (that never completes initial load) must
     * still trigger the timeout based on the NEW registeredAt timestamp.
     * This verifies that the bugfix (resetRegistrationTime) does not disable
     * the timeout â€” it only resets the clock.
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     * @param timeoutMs    a short timeout (10-100 ms) to simulate the 5-minute timeout
     */
    @Property(tries = 50)
    void timeoutStillFiresAfterReconnectionIfDatasourceRemainsStuck(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 10, max = 100) int timeoutMs) throws InterruptedException {

        String dsName = "ds-" + dsNameSuffix;
        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register and let the initial timeout fire
        manager.register(dsName);
        manager.setInitialLoadTimeout(timeoutMs);
        Thread.sleep((long) timeoutMs + 50);
        assertThat(manager.checkInitialLoadTimeout(dsName)).isTrue();
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // 2. Simulate successful reconnection with the bugfix
        manager.handleReconnectionSuccess(dsName);
        manager.resetRegistrationTime(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 3. Immediately after reset, timeout must NOT fire (fresh registeredAt)
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must NOT fire immediately after resetRegistrationTime â€” clock was reset")
                .isFalse();

        // 4. Wait for the timeout to elapse again (datasource is genuinely stuck)
        Thread.sleep((long) timeoutMs + 50);

        // 5. Timeout must fire again on the new registeredAt
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Timeout must fire again when datasource remains stuck after reconnection")
                .isTrue();
        assertThat(manager.getState(dsName))
                .as("State must be ERROR after second timeout")
                .isEqualTo(StreamingDataSourceState.ERROR);
    }
}
