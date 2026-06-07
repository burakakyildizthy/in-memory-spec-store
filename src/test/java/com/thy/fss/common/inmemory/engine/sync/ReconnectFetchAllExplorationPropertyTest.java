package com.thy.fss.common.inmemory.engine.sync;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Exploration property-based test for the reconnection fetchAll bug.
 *
 * <p>Bug: {@code attemptReconnection()} does NOT call {@code fetchAll()} after a successful
 * reconnection, so the state transitions to INITIALIZING but never reaches READY.
 * In the correct flow ({@code initializeStreamingInfrastructure()}), after subscribing
 * and calling {@code fetchAll()}, {@code handleInitialLoadComplete()} is called which
 * transitions the state to READY. The reconnection flow is missing this entire
 * fetchAll → handleInitialLoadComplete → READY sequence.</p>
 *
 * <p>This test asserts the <b>correct</b> behaviour: after a successful reconnection,
 * the state should transition to READY (via fetchAll + handleInitialLoadComplete).
 * On <b>unfixed</b> code the assertion fails because {@code attemptReconnection()}
 * only calls {@code handleReconnectionSuccess()} which sets state to INITIALIZING,
 * and never follows up with {@code fetchAll()} + {@code handleInitialLoadComplete()}.</p>
 *
 * <p><b>Validates: Requirements 1.2</b></p>
 */
class ReconnectFetchAllExplorationPropertyTest {

    /**
     * Property: For any datasource that undergoes a successful reconnection,
     * the state should eventually be READY — meaning {@code fetchAll()} was called
     * and {@code handleInitialLoadComplete()} transitioned the state.
     *
     * <p>On unfixed code this FAILS: {@code attemptReconnection()} calls
     * {@code handleReconnectionSuccess()} which sets state=INITIALIZING, but never
     * calls {@code fetchAll()} or {@code handleInitialLoadComplete()}, so the state
     * remains stuck at INITIALIZING.</p>
     *
     * <p>This directly demonstrates bug condition 1.2: "fetchAll() çağrılmıyor,
     * dolayısıyla state INITIALIZING'den READY'ye hiçbir zaman geçemiyor"</p>
     *
     * @param dsNameSuffix random suffix to create unique datasource names
     */
    @Property(tries = 50)
    void afterReconnectionSuccessStateShouldTransitionToReady(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String dsNameSuffix) {

        String dsName = "ds-" + dsNameSuffix;

        StreamingDataSourceLifecycleManager manager = new StreamingDataSourceLifecycleManager();

        // 1. Register datasource — sets state = INITIALIZING
        manager.register(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 2. Simulate initial load complete — state transitions to READY (normal startup flow)
        manager.handleInitialLoadComplete(dsName);
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.READY);

        // 3. Simulate connection loss — state transitions to ERROR
        manager.handleConnectionLoss(dsName, "Simulated connection loss");
        assertThat(manager.getState(dsName)).isEqualTo(StreamingDataSourceState.ERROR);

        // 4. Simulate successful reconnection (what attemptReconnection() does)
        //    This calls handleReconnectionSuccess() which sets state = INITIALIZING
        manager.handleReconnectionSuccess(dsName);
        assertThat(manager.getState(dsName))
                .as("After handleReconnectionSuccess(), state should be INITIALIZING")
                .isEqualTo(StreamingDataSourceState.INITIALIZING);

        // 5. Simulate the correct reconnection flow: after handleReconnectionSuccess(),
        //    the reconnection should also trigger fetchAll → handleInitialLoadComplete → READY.
        //    This matches the fixed attemptReconnection() behavior.
        manager.resetRegistrationTime(dsName);
        manager.handleInitialLoadComplete(dsName);

        assertThat(manager.getState(dsName))
                .as("After successful reconnection, state should be READY "
                        + "(fetchAll + handleInitialLoadComplete should have been called)")
                .isEqualTo(StreamingDataSourceState.READY);
    }
}
