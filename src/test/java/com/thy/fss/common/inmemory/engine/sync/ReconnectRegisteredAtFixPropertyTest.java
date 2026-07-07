package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Fix verification property-based test for the reconnection registeredAt bug.
 *
 * <p>After the fix, {@code attemptReconnection()} calls {@code resetRegistrationTime()}
 * immediately after {@code handleReconnectionSuccess()}. This resets the {@code registeredAt}
 * timestamp so that {@code checkInitialLoadTimeout()} does not immediately fire on a stale
 * value.</p>
 *
 * <p>This test verifies the fixed behaviour: after {@code handleReconnectionSuccess()} followed
 * by {@code resetRegistrationTime()}, the {@code registeredAt} timestamp is fresh and
 * {@code checkInitialLoadTimeout()} returns {@code false}.</p>
 *
 * <p>This test should PASS on fixed code.</p>
 *
 * <p><b>Validates: Requirements 2.1</b></p>
 */
class ReconnectRegisteredAtFixPropertyTest {

    /**
     * Property: For any datasource that has been registered longer than the timeout,
     * after {@code handleReconnectionSuccess()} + {@code resetRegistrationTime()},
     * {@code checkInitialLoadTimeout()} must return {@code false} — because
     * {@code registeredAt} has been reset to a fresh timestamp.
     *
     * <p>On fixed code this PASSES: {@code resetRegistrationTime()} resets the timestamp,
     * so the timeout check sees a near-zero elapsed time.</p>
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     * @param timeoutMs    a short timeout (10-100 ms) so we can simulate "5+ minutes" quickly
     */
    @Property(tries = 50)
    void afterReconnectionWithFixRegisteredAtIsReset(
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

        // Sanity: timeout should fire on stale registeredAt
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("Sanity check: timeout should fire on stale registeredAt before reconnection")
                .isTrue();

        // State is now ERROR after timeout detection
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // 4. Simulate successful reconnection (as the fixed attemptReconnection does)
        manager.handleReconnectionSuccess(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 5. Apply the fix: resetRegistrationTime() — this is what the fixed code does
        manager.resetRegistrationTime(dsName);

        // 6. ASSERT: checkInitialLoadTimeout() must return false because registeredAt
        //    has been reset by resetRegistrationTime(). The fresh timestamp means
        //    elapsed time is near zero — well within the timeout window.
        assertThat(manager.checkInitialLoadTimeout(dsName))
                .as("After reconnection + resetRegistrationTime, timeout must NOT fire")
                .isFalse();

        // 7. ASSERT: state should still be INITIALIZING (not ERROR) — no timeout triggered
        assertThat(manager.getState(dsName))
                .as("State should remain INITIALIZING after reset — no spurious timeout")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);
    }
}
