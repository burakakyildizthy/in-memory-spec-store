package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Unit tests for {@link StreamingDataSourceLifecycleManager}.
 *
 * <p>Tests cover state transitions, connection loss handling, reconnection logic,
 * exponential backoff, and counter reset behavior.</p>
 */
class StreamingDataSourceLifecycleManagerTest {

    private static final String DS_NAME = "test-streaming-ds";
    private static final String NONEXISTENT = "nonexistent";

    private StreamingDataSourceLifecycleManager manager;

    @BeforeEach
    void setUp() {
        manager = new StreamingDataSourceLifecycleManager();
    }

    // === Registration ===

    @Test
    void registerSetsInitialState() {
        manager.register(DS_NAME);

        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);
        assertThat(manager.isHealthy(DS_NAME)).isTrue();
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isZero();
        assertThat(manager.isRegistered(DS_NAME)).isTrue();
    }

    @Test
    void unregisterRemovesDatasource() {
        manager.register(DS_NAME);
        manager.unregister(DS_NAME);

        assertThat(manager.isRegistered(DS_NAME)).isFalse();
        assertThat(manager.getState(DS_NAME)).isNull();
    }

    @Test
    void getStateReturnsNullForUnregistered() {
        assertThat(manager.getState(NONEXISTENT)).isNull();
    }

    @Test
    void isHealthyReturnsFalseForUnregistered() {
        assertThat(manager.isHealthy(NONEXISTENT)).isFalse();
    }

    // === Connection Loss ===

    @Test
    void handleConnectionLossSetsErrorState() {
        manager.register(DS_NAME);
        manager.handleInitialLoadComplete(DS_NAME); // Move to READY first

        manager.handleConnectionLoss(DS_NAME, "Connection reset");

        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(manager.isHealthy(DS_NAME)).isFalse();
    }

    @Test
    void handleConnectionLossDuringInitializing() {
        manager.register(DS_NAME);
        // Still in INITIALIZING state

        manager.handleConnectionLoss(DS_NAME, "Timeout during initial load");

        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(manager.isHealthy(DS_NAME)).isFalse();
    }

    // === Reconnection Success ===

    @Test
    void handleReconnectionSuccessSetsInitializingState() {
        manager.register(DS_NAME);
        manager.handleConnectionLoss(DS_NAME, "Connection lost");

        manager.handleReconnectionSuccess(DS_NAME);

        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);
        assertThat(manager.isHealthy(DS_NAME)).isTrue();
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isZero();
    }

    @Test
    void handleReconnectionSuccessResetsFailureCounter() {
        manager.register(DS_NAME);
        manager.handleConnectionLoss(DS_NAME, "error");

        // Accumulate some failures
        manager.recordReconnectionFailure(DS_NAME);
        manager.recordReconnectionFailure(DS_NAME);
        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isEqualTo(3);

        // Successful reconnection resets counter
        manager.handleReconnectionSuccess(DS_NAME);
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isZero();
    }

    // === Reconnection Failure ===

    @Test
    void recordReconnectionFailureIncrementsCounter() {
        manager.register(DS_NAME);

        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isEqualTo(1);

        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isEqualTo(2);

        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.getConsecutiveFailures(DS_NAME)).isEqualTo(3);
    }

    @Test
    void recordReconnectionFailureMarksUnhealthy() {
        manager.register(DS_NAME);

        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.isHealthy(DS_NAME)).isFalse();
    }

    // === Initial Load Complete ===

    @Test
    void handleInitialLoadCompleteTransitionsToReady() {
        manager.register(DS_NAME);

        manager.handleInitialLoadComplete(DS_NAME);

        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.READY);
        assertThat(manager.isHealthy(DS_NAME)).isTrue();
    }

    // === Full Lifecycle: INITIALIZING → READY → ERROR → INITIALIZING → READY ===

    @Test
    void fullLifecycleRoundTrip() {
        manager.register(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // Initial load completes
        manager.handleInitialLoadComplete(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.READY);

        // Connection drops
        manager.handleConnectionLoss(DS_NAME, "Network failure");
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.ERROR);
        assertThat(manager.isHealthy(DS_NAME)).isFalse();

        // Reconnection succeeds → back to INITIALIZING (not directly to READY)
        manager.handleReconnectionSuccess(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);
        assertThat(manager.isHealthy(DS_NAME)).isTrue();

        // Initial load completes again
        manager.handleInitialLoadComplete(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.READY);
    }

    @Test
    void initialLoadFailureThenReconnectRestartsInitialLoad() {
        manager.register(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // Connection error during initial load
        manager.handleConnectionLoss(DS_NAME, "Timeout during initial load");
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.ERROR);

        // Reconnection succeeds → initial load restarts from scratch
        manager.handleReconnectionSuccess(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.INITIALIZING);

        // Initial load completes
        manager.handleInitialLoadComplete(DS_NAME);
        assertThat(manager.getState(DS_NAME)).isEqualTo(StreamingDataSourceState.READY);
    }

    // === Exponential Backoff ===

    @Test
    void backoffDelayIsBaseForFirstThreeAttempts() {
        Duration base = StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY;

        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(0)).isEqualTo(base);
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(1)).isEqualTo(base);
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(2)).isEqualTo(base);
    }

    @Test
    void backoffDelayGrowsExponentiallyAfterThreshold() {
        Duration base = StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY;

        // attempt 3: base * 2^0 = base
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(3))
                .isEqualTo(base);

        // attempt 4: base * 2^1 = 2 * base
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(4))
                .isEqualTo(base.multipliedBy(2));

        // attempt 5: base * 2^2 = 4 * base
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(5))
                .isEqualTo(base.multipliedBy(4));

        // attempt 6: base * 2^3 = 8 * base
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(6))
                .isEqualTo(base.multipliedBy(8));
    }

    @Test
    void backoffDelayCapsAtMaximum() {
        Duration maxDelay = StreamingDataSourceLifecycleManager.MAX_BACKOFF_DELAY;

        // Very high failure count should cap at MAX_BACKOFF_DELAY
        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(50))
                .isEqualTo(maxDelay);

        assertThat(StreamingDataSourceLifecycleManager.calculateBackoffDelay(100))
                .isEqualTo(maxDelay);
    }

    @Test
    void calculateNextReconnectDelayUsesConsecutiveFailures() {
        manager.register(DS_NAME);
        Duration base = StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY;

        // 0 failures → base delay
        assertThat(manager.calculateNextReconnectDelay(DS_NAME)).isEqualTo(base);

        // 3 failures → still base (3 = threshold, exponent = 0)
        manager.recordReconnectionFailure(DS_NAME);
        manager.recordReconnectionFailure(DS_NAME);
        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.calculateNextReconnectDelay(DS_NAME)).isEqualTo(base);

        // 4 failures → 2 * base
        manager.recordReconnectionFailure(DS_NAME);
        assertThat(manager.calculateNextReconnectDelay(DS_NAME)).isEqualTo(base.multipliedBy(2));
    }

    @Test
    void calculateNextReconnectDelayReturnsBaseForUnregistered() {
        Duration base = StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY;
        assertThat(manager.calculateNextReconnectDelay(NONEXISTENT)).isEqualTo(base);
    }

    // === Clear ===

    @Test
    void clearRemovesAllDatasources() {
        manager.register("ds1");
        manager.register("ds2");

        manager.clear();

        assertThat(manager.isRegistered("ds1")).isFalse();
        assertThat(manager.isRegistered("ds2")).isFalse();
    }
}
