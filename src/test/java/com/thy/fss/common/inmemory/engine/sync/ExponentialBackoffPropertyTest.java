package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

// Feature: streaming-datasource-support, Property 17: Exponential Backoff

/**
 * Property-based test for exponential backoff delay calculation.
 *
 * <p>Verifies that {@link StreamingDataSourceLifecycleManager#calculateBackoffDelay(int)}
 * correctly implements exponential backoff:</p>
 * <ul>
 *   <li>Below threshold (first 3 failures): always BASE_BACKOFF_DELAY (1s)</li>
 *   <li>At/above threshold: delay grows exponentially, capped at MAX_BACKOFF_DELAY (5min)</li>
 *   <li>Delay is always positive and non-zero</li>
 *   <li>Delay is monotonically non-decreasing as consecutive failures increase</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 12.4</b></p>
 */
class ExponentialBackoffPropertyTest {

    private static final Duration BASE = StreamingDataSourceLifecycleManager.BASE_BACKOFF_DELAY;
    private static final Duration MAX = StreamingDataSourceLifecycleManager.MAX_BACKOFF_DELAY;
    private static final int THRESHOLD = StreamingDataSourceLifecycleManager.BACKOFF_THRESHOLD;

    /**
     * For consecutive failures below the backoff threshold, the delay must always
     * equal BASE_BACKOFF_DELAY (1 second).
     */
    @Property(tries = 100)
    void belowThresholdAlwaysReturnsBaseDelay(
            @ForAll @IntRange(min = 0, max = 2) int consecutiveFailures) {

        Duration delay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures);

        assertThat(delay)
                .as("Below threshold (%d failures), delay must be BASE_BACKOFF_DELAY", consecutiveFailures)
                .isEqualTo(BASE);
    }

    /**
     * After reaching the backoff threshold, delay must grow exponentially:
     * baseDelay * 2^(consecutiveFailures - BACKOFF_THRESHOLD), capped at MAX_BACKOFF_DELAY.
     */
    @Property(tries = 100)
    void atOrAboveThresholdDelayGrowsExponentially(
            @ForAll @IntRange(min = 3, max = 50) int consecutiveFailures) {

        Duration delay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures);

        int exponent = consecutiveFailures - THRESHOLD;
        if (exponent > 17) {
            // Overflow guard: should be capped at MAX
            assertThat(delay)
                    .as("Exponent overflow guard: delay must be MAX_BACKOFF_DELAY")
                    .isEqualTo(MAX);
        } else {
            long expectedMillis = BASE.toMillis() * (1L << exponent);
            Duration expectedDelay = Duration.ofMillis(expectedMillis);
            Duration expected = expectedDelay.compareTo(MAX) > 0 ? MAX : expectedDelay;

            assertThat(delay)
                    .as("At %d failures (exponent=%d), delay must match exponential formula or cap",
                            consecutiveFailures, exponent)
                    .isEqualTo(expected);
        }
    }

    /**
     * Delay must always be positive and non-zero for any non-negative consecutive failure count.
     */
    @Property(tries = 100)
    void delayIsAlwaysPositive(
            @ForAll @IntRange(min = 0, max = 100) int consecutiveFailures) {

        Duration delay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures);

        assertThat(delay)
                .as("Delay must be positive for %d consecutive failures", consecutiveFailures)
                .isPositive();
    }

    /**
     * Delay must never exceed MAX_BACKOFF_DELAY (5 minutes).
     */
    @Property(tries = 100)
    void delayNeverExceedsMax(
            @ForAll @IntRange(min = 0, max = 100) int consecutiveFailures) {

        Duration delay = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures);

        assertThat(delay)
                .as("Delay must not exceed MAX_BACKOFF_DELAY for %d failures", consecutiveFailures)
                .isLessThanOrEqualTo(MAX);
    }

    /**
     * Delay must be monotonically non-decreasing: for any n, delay(n) <= delay(n+1).
     */
    @Property(tries = 100)
    void delayIsMonotonicallyNonDecreasing(
            @ForAll @IntRange(min = 0, max = 99) int consecutiveFailures) {

        Duration delayCurrent = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures);
        Duration delayNext = StreamingDataSourceLifecycleManager.calculateBackoffDelay(consecutiveFailures + 1);

        assertThat(delayNext)
                .as("delay(%d) must be >= delay(%d)", consecutiveFailures + 1, consecutiveFailures)
                .isGreaterThanOrEqualTo(delayCurrent);
    }

    /**
     * After a successful reconnection (counter reset to 0), delay must return to BASE_BACKOFF_DELAY.
     * This verifies the reset semantics: calculateBackoffDelay(0) == BASE_BACKOFF_DELAY.
     */
    @Property(tries = 100)
    void afterResetDelayReturnsToBase(
            @ForAll @IntRange(min = 1, max = 50) int previousFailures) {

        // Simulate: had some failures, then reset to 0
        Duration delayBeforeReset = StreamingDataSourceLifecycleManager.calculateBackoffDelay(previousFailures);
        Duration delayAfterReset = StreamingDataSourceLifecycleManager.calculateBackoffDelay(0);

        assertThat(delayBeforeReset)
                .as("Before reset, delay should be >= BASE")
                .isGreaterThanOrEqualTo(BASE);

        assertThat(delayAfterReset)
                .as("After reset (0 failures), delay must return to BASE_BACKOFF_DELAY")
                .isEqualTo(BASE);
    }
}
