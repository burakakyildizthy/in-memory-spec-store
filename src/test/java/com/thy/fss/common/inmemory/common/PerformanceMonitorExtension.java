package com.thy.fss.common.inmemory.common;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Annotation to specify maximum execution time for a test method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MaxExecutionTime {
    long milliseconds() default 1000;
}

/**
 * JUnit 5 extension for monitoring test performance and detecting regressions.
 */
public class PerformanceMonitorExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

    private static final ConcurrentMap<String, Long> testStartTimes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TestExecutionStats> testStats = new ConcurrentHashMap<>();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceMonitorExtension.class);
    private static long suiteStartTime;

    /**
     * Gets performance statistics for a specific test.
     */
    public static TestExecutionStats getTestStats(String testKey) {
        return testStats.get(testKey);
    }

    /**
     * Clears all performance statistics.
     */
    public static void clearStats() {
        testStats.clear();
        testStartTimes.clear();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        suiteStartTime = System.currentTimeMillis();
        System.out.println("Starting performance monitoring for test class: " + context.getDisplayName());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        long suiteExecutionTime = System.currentTimeMillis() - suiteStartTime;
        System.out.println("Test class execution completed: " + context.getDisplayName());
        System.out.println("Total execution time: " + suiteExecutionTime + " ms");

        // Print performance summary
        printPerformanceSummary();

        // Check for performance violations
        checkPerformanceViolations();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        String testKey = getTestKey(context);
        testStartTimes.put(testKey, System.currentTimeMillis());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        String testKey = getTestKey(context);
        Long startTime = testStartTimes.remove(testKey);

        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordTestExecution(context, executionTime);

            // Check for timeout violations
            checkTimeoutViolation(context, executionTime);
        }
    }

    private String getTestKey(ExtensionContext context) {
        return context.getTestClass().map(Class::getSimpleName).orElse("Unknown") +
                "." + context.getDisplayName();
    }

    private void recordTestExecution(ExtensionContext context, long executionTime) {
        String testKey = getTestKey(context);
        TestExecutionStats stats = testStats.computeIfAbsent(testKey, k -> new TestExecutionStats());
        stats.recordExecution(executionTime);

        // Log execution time
        System.out.printf("Test %s executed in %d ms%n", testKey, executionTime);
    }

    private void checkTimeoutViolation(ExtensionContext context, long executionTime) {
        MaxExecutionTime annotation = context.getRequiredTestMethod().getAnnotation(MaxExecutionTime.class);
        if (annotation != null) {
            long maxTime = annotation.milliseconds();
            if (executionTime > maxTime) {
                System.err.printf("WARNING: Test %s exceeded maximum execution time. " +
                                "Expected: <%d ms, Actual: %d ms%n",
                        getTestKey(context), maxTime, executionTime);
            }
        }
    }

    private void printPerformanceSummary() {
        System.out.println("\n=== Performance Summary ===");
        testStats.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().getAverageTime(), e1.getValue().getAverageTime()))
                .forEach(entry -> {
                    String testName = entry.getKey();
                    TestExecutionStats stats = entry.getValue();
                    System.out.printf("%-50s | Avg: %4d ms | Min: %4d ms | Max: %4d ms | Executions: %d%n",
                            testName, stats.getAverageTime(), stats.getMinTime(), stats.getMaxTime(), stats.getExecutionCount());
                });
        System.out.println("===========================\n");
    }

    private void checkPerformanceViolations() {
        boolean hasViolations = false;

        for (var entry : testStats.entrySet()) {
            TestExecutionStats stats = entry.getValue();
            if (stats.getAverageTime() > 5000) { // 5 second threshold
                System.err.printf("PERFORMANCE VIOLATION: Test %s has average execution time of %d ms%n",
                        entry.getKey(), stats.getAverageTime());
                hasViolations = true;
            }
        }

        if (hasViolations) {
            log.info("Performance violations detected. Consider optimizing slow tests.");
        }
    }

    /**
     * Statistics for test execution performance.
     */
    public static class TestExecutionStats {
        private long totalTime = 0;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = 0;
        private int executionCount = 0;

        public synchronized void recordExecution(long executionTime) {
            totalTime += executionTime;
            minTime = Math.min(minTime, executionTime);
            maxTime = Math.max(maxTime, executionTime);
            executionCount++;
        }

        public long getAverageTime() {
            return executionCount > 0 ? totalTime / executionCount : 0;
        }

        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }

        public long getMaxTime() {
            return maxTime;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public int getExecutionCount() {
            return executionCount;
        }
    }
}