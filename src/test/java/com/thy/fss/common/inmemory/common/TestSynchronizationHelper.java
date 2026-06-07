package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.testmodel.TestUtil;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Utility class for test synchronization to replace "Thread.sleep() calls
 * with condition-based waiting for better test performance and reliability.
 */
public class TestSynchronizationHelper {

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(10);

    /**
     * Waits for a condition to become true within the specified timeout.
     *
     * @param condition The condition to wait for
     * @param timeout   Maximum time to wait
     * @throws AssertionError if condition is not met within timeout
     */
    public static void waitForCondition(Supplier<Boolean> condition, Duration timeout) {
        waitForCondition(condition, timeout, DEFAULT_POLL_INTERVAL);
    }

    /**
     * Waits for a condition to become true within the specified timeout with custom polling interval.
     *
     * @param condition    The condition to wait for
     * @param timeout      Maximum time to wait
     * @param pollInterval How often to check the condition
     * @throws AssertionError if condition is not met within timeout
     */
    public static void waitForCondition(Supplier<Boolean> condition, Duration timeout, Duration pollInterval) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        long pollIntervalMs = pollInterval.toMillis();

        while (!condition.get()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new AssertionError("Condition not met within timeout: " + timeout);
            }
            TestUtil.await(pollIntervalMs);
        }
    }

    /**
     * Waits for a condition with a custom timeout and provides a descriptive error message.
     *
     * @param condition   The condition to wait for
     * @param timeout     Maximum time to wait
     * @param description Description of what we're waiting for (used in error message)
     */
    public static void waitForCondition(Supplier<Boolean> condition, Duration timeout, String description) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();

        while (!condition.get()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new AssertionError(String.format(
                        "Condition not met within timeout: %s. Waiting for: %s",
                        timeout, description
                ));
            }
            TestUtil.await(DEFAULT_POLL_INTERVAL);
        }
    }
}