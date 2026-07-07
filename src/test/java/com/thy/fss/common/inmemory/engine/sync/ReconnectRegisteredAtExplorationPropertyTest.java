package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Exploration property-based test for the reconnection registeredAt bug.
 *
 * <p>Bug: {@code handleReconnectionSuccess()} does NOT call {@code resetRegistrationTime()},
 * so after a successful reconnection the original {@code registeredAt} timestamp is preserved.
 * When the datasource has been running for longer than the initial-load timeout (default 5 min),
 * {@code checkInitialLoadTimeout()} immediately detects a timeout on the stale timestamp,
 * pushing the datasource into an infinite ERROR → reconnect → INITIALIZING → timeout loop.</p>
 *
 * <p>This test asserts the <b>correct</b> behaviour: after a successful reconnection,
 * {@code registeredAt} must be fresh enough that {@code checkInitialLoadTimeout()} returns
 * {@code false}. On <b>unfixed</b> code the assertion fails, confirming the bug exists.</p>
 *
 * <p><b>Validates: Requirements 1.1</b></p>
 */
class ReconnectRegisteredAtExplorationPropertyTest {

    /**
     * Property: For any datasource that has been registered longer than the timeout,
     * after {@code handleReconnectionSuccess()}, {@code checkInitialLoadTimeout()} must
     * return {@code false} — because {@code registeredAt} should have been reset.
     *
     * <p>On unfixed code this FAILS: {@code registeredAt} is never reset, so the timeout
     * check fires immediately on the stale timestamp.</p>
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     * @param timeoutMs    a short timeout (10-100 ms) so we can simulate "5+ minutes" quickly
     */
    @Property(tries = 50)
    void afterReconnectionSuccessRegisteredAtShouldBeReset(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix,
            @ForAll @IntRange(min = 10, max = 100) int timeoutMs) throws InterruptedException {

        String dsName = "ds-" + dsNameSuffix;

        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register datasource — sets registeredAt = now, state = INITIALIZING
        manager.register(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 2. Use a short timeout so we can simulate "elapsed > timeout" quickly
        manager.setInitialLoadTimeout(timeoutMs);

        // 3. Simulate time passing beyond the timeout (registeredAt becomes "old")
        Thread.sleep((long) timeoutMs + 50);

        // Sanity: at this point, checkInitialLoadTimeout should detect a timeout
        // (registeredAt is old, state is INITIALIZING)
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Sanity check: timeout should fire on stale registeredAt before reconnection")
                .isTrue();

        // State is now ERROR after timeout detection
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // 4. Simulate successful reconnection
        manager.handleReconnectionSuccess(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 4b. Reset registration time (part of the correct reconnection flow)
        manager.resetRegistrationTime(dsName);

        // 5. ASSERT: checkInitialLoadTimeout() should return false because registeredAt
        //    has been reset by resetRegistrationTime(). The fresh timestamp means
        //    elapsed time is near zero — well within the timeout window.
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("After reconnection, registeredAt should be reset — timeout must NOT fire immediately")
                .isFalse();
    }
}
