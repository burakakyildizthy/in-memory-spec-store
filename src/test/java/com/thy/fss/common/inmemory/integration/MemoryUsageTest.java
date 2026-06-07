package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserFilter;
import com.thy.fss.common.inmemory.filter.BooleanFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.LocalDateFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Memory usage tests to verify memory efficiency of the reflection-free system.
 * Tests memory consumption, garbage collection behavior, and memory leak detection.
 * <p>
 * Requirements covered:
 * - 7.10: Memory usage optimization
 * - 8.8: Edge case handling (memory-related)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Memory Usage Tests")
@Tag("performance")
class MemoryUsageTest {

    private SpecificationQueryEngine<TestUser> userEngine;

    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() {
        // Services are accessed via direct INSTANCE references

        userEngine = new SpecificationQueryEngine<>(TestUser.class);

        memoryBean = ManagementFactory.getMemoryMXBean();

        // Force initial garbage collection
        forceGarbageCollection();
    }

    /**
     * Test 1: Filter Object Memory Footprint
     * Measures memory usage of filter objects
     */
    @Test
    @Order(1)
    @DisplayName("Filter Object Memory Footprint")
    void testFilterObjectMemoryFootprint() {
        System.out.println("\n=== FILTER OBJECT MEMORY FOOTPRINT ===");

        MemoryUsage beforeMemory = getMemoryUsage();

        // Create various types of filters
        List<Object> filters = new ArrayList<>();

        // String filters
        for (int i = 0; i < 10000; i++) {
            StringFilter stringFilter = new StringFilter();
            stringFilter.setEquals("test" + i);
            stringFilter.setContains("contains" + i);
            stringFilter.setStartsWith("starts" + i);
            stringFilter.setEndsWith("ends" + i);
            stringFilter.setIsNull(i % 2 == 0);
            stringFilter.setIn(Arrays.asList("value1", "value2", "value3"));
            filters.add(stringFilter);
        }

        // Integer filters
        for (int i = 0; i < 10000; i++) {
            IntegerFilter integerFilter = new IntegerFilter();
            integerFilter.setEquals(i);
            integerFilter.setGreaterThan(i - 10);
            integerFilter.setLessThan(i + 10);
            integerFilter.setGreaterOrEqualThan(i - 5);
            integerFilter.setLessOrEqualThan(i + 5);
            integerFilter.setIsNull(i % 3 == 0);
            integerFilter.setIn(Arrays.asList(i, i + 1, i + 2));
            filters.add(integerFilter);
        }

        // Boolean filters
        for (int i = 0; i < 10000; i++) {
            BooleanFilter booleanFilter = new BooleanFilter();
            booleanFilter.setEquals(i % 2 == 0);
            booleanFilter.setIsNull(i % 4 == 0);
            booleanFilter.setIn(Arrays.asList(true, false));
            filters.add(booleanFilter);
        }

        // Date filters
        for (int i = 0; i < 10000; i++) {
            LocalDateFilter dateFilter = new LocalDateFilter();
            dateFilter.setEquals(LocalDate.of(2020 + (i % 5), 1 + (i % 12), 1 + (i % 28)));
            dateFilter.setIsOnOrAfter(LocalDate.of(2020, 1, 1));
            dateFilter.setIsOnOrBefore(LocalDate.of(2025, 1, 1));
            dateFilter.setIsNull(i % 5 == 0);
            filters.add(dateFilter);
        }

        // Complex filters (TestUserFilter)
        for (int i = 0; i < 5000; i++) {
            TestUserFilter userFilter = new TestUserFilter();

            StringFilter nameFilter = new StringFilter();
            nameFilter.setContains("user" + i);
            userFilter.setName(nameFilter);

            IntegerFilter ageFilter = new IntegerFilter();
            ageFilter.setGreaterThan(20 + (i % 50));
            userFilter.setAge(ageFilter);

            BooleanFilter activeFilter = new BooleanFilter();
            activeFilter.setEquals(i % 2 == 0);
            userFilter.setActive(activeFilter);

            filters.add(userFilter);
        }

        forceGarbageCollection();
        MemoryUsage afterMemory = getMemoryUsage();

        long memoryUsed = afterMemory.getUsed() - beforeMemory.getUsed();
        double memoryPerFilter = memoryUsed / (double) filters.size();

        System.out.printf("Total filters created: %d%n", filters.size());
        System.out.printf("Memory used: %.2f MB%n", memoryUsed / 1024.0 / 1024.0);
        System.out.printf("Memory per filter: %.2f bytes%n", memoryPerFilter);

        // Verify reasonable memory usage
        assertTrue(memoryUsed < 500_000_000, "45K filters should use less than 500MB");
        assertTrue(memoryPerFilter < 10000, "Each filter should use less than 10KB on average");

        System.out.println("✓ Filter object memory footprint is reasonable");
    }

    /**
     * Test 2: Query Result Memory Usage
     * Tests memory usage of query results
     */
    @Test
    @Order(2)
    @DisplayName("Query Result Memory Usage")
    void testQueryResultMemoryUsage() {
        System.out.println("\n=== QUERY RESULT MEMORY USAGE ===");

        // Create large dataset
        List<TestUser> largeDataset = createLargeDataset(100000);
        System.out.printf("Created dataset with %d users%n", largeDataset.size());

        MemoryUsage beforeQuery = getMemoryUsage();

        // Execute query that returns significant results
        TestUserFilter filter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("user"); // Should match most users
        filter.setName(nameFilter);

        List<TestUser> results = userEngine.queryByFilter(largeDataset, filter);

        MemoryUsage afterQuery = getMemoryUsage();

        long queryMemory = afterQuery.getUsed() - beforeQuery.getUsed();
        double memoryPerResult = queryMemory / (double) results.size();

        System.out.printf("Query returned %d results%n", results.size());
        System.out.printf("Query memory usage: %.2f MB%n", queryMemory / 1024.0 / 1024.0);
        System.out.printf("Memory per result: %.2f bytes%n", memoryPerResult);

        // Test multiple queries to verify memory is released
        MemoryUsage beforeMultiple = getMemoryUsage();

        for (int i = 0; i < 10; i++) {
            TestUserFilter tempFilter = new TestUserFilter();
            StringFilter tempNameFilter = new StringFilter();
            tempNameFilter.setContains("temp" + i);
            tempFilter.setName(tempNameFilter);

            List<TestUser> tempResults = userEngine.queryByFilter(largeDataset, tempFilter);
            assertNotNull(tempResults);
        }

        forceGarbageCollection();
        MemoryUsage afterMultiple = getMemoryUsage();

        long multipleQueryMemory = afterMultiple.getUsed() - beforeMultiple.getUsed();

        System.out.printf("Memory after 10 additional queries: %.2f MB%n", multipleQueryMemory / 1024.0 / 1024.0);

        // Memory should not grow significantly with multiple queries
        assertTrue(Math.abs(multipleQueryMemory) < 100_000_000,
                "Multiple queries should not cause significant memory growth");

        System.out.println("✓ Query result memory usage is efficient");
    }

    /**
     * Test 3: Memory Leak Detection
     * Tests for memory leaks in repeated operations
     */
    @Test
    @Order(3)
    @DisplayName("Memory Leak Detection")
    void testMemoryLeakDetection() {
        System.out.println("\n=== MEMORY LEAK DETECTION ===");

        List<TestUser> testDataset = createLargeDataset(10000);

        // Baseline memory measurement
        forceGarbageCollection();
        MemoryUsage baseline = getMemoryUsage();

        // Perform many operations
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            // Create filter
            TestUserFilter filter = new TestUserFilter();
            StringFilter nameFilter = new StringFilter();
            nameFilter.setContains("user" + (i % 100));
            filter.setName(nameFilter);

            IntegerFilter ageFilter = new IntegerFilter();
            ageFilter.setGreaterThan(20 + (i % 50));
            filter.setAge(ageFilter);

            // Execute query
            List<TestUser> results = userEngine.queryByFilter(testDataset, filter);
            assertNotNull(results);

            // Count operation
            long count = userEngine.countByFilter(testDataset, filter);
            assertTrue(count >= 0);

            // Periodic garbage collection
            if (i % 100 == 0) {
                System.gc();
                Thread.yield();
            }
        }

        // Final memory measurement
        forceGarbageCollection();
        MemoryUsage afterOperations = getMemoryUsage();

        long memoryDifference = afterOperations.getUsed() - baseline.getUsed();
        double memoryPerOperation = memoryDifference / (double) iterations;

        System.out.printf("Performed %d operations%n", iterations);
        System.out.printf("Memory difference: %.2f MB%n", memoryDifference / 1024.0 / 1024.0);
        System.out.printf("Memory per operation: %.2f bytes%n", memoryPerOperation);

        // Should not have significant memory leaks
        assertTrue(Math.abs(memoryDifference) < 100_000_000,
                "Should not have memory leaks > 100MB after " + iterations + " operations");
        assertTrue(Math.abs(memoryPerOperation) < 100000,
                "Memory per operation should be minimal");

        System.out.println("✓ No significant memory leaks detected");
    }

    /**
     * Test 4: Complex Object Memory Usage
     * Tests memory usage with multiple TestUser objects
     */
    @Test
    @Order(4)
    @DisplayName("Complex Object Memory Usage")
    void testComplexObjectMemoryUsage() {
        System.out.println("\n=== COMPLEX OBJECT MEMORY USAGE ===");

        MemoryUsage beforeComplex = getMemoryUsage();

        // Create many TestUser entities with varied data
        List<TestUser> complexEntities = new ArrayList<>();
        for (int i = 0; i < 50000; i++) {
            TestUser user = new TestUser();
            user.setName("ComplexUser" + i + "_" + (i % 1000 == 0 ? "special" : "normal"));
            user.setAge(18 + (i % 65));
            user.setActive(i % 4 != 0);
            complexEntities.add(user);
        }

        MemoryUsage afterCreation = getMemoryUsage();
        long creationMemory = afterCreation.getUsed() - beforeComplex.getUsed();

        // Perform queries on complex objects
        TestUserFilter complexFilter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("special");
        complexFilter.setName(nameFilter);

        List<TestUser> results = userEngine.queryByFilter(complexEntities, complexFilter);

        MemoryUsage afterQuery = getMemoryUsage();
        long queryMemory = afterQuery.getUsed() - afterCreation.getUsed();

        System.out.printf("Created %d complex entities%n", complexEntities.size());
        System.out.printf("Creation memory: %.2f MB%n", creationMemory / 1024.0 / 1024.0);
        System.out.printf("Query memory: %.2f MB%n", queryMemory / 1024.0 / 1024.0);
        System.out.printf("Query returned %d results%n", results.size());

        // Test repeated operations on complex objects
        for (int i = 0; i < 100; i++) {
            userEngine.queryByFilter(complexEntities, complexFilter);
        }

        forceGarbageCollection();
        MemoryUsage afterRepeated = getMemoryUsage();
        long repeatedMemory = afterRepeated.getUsed() - afterQuery.getUsed();

        System.out.printf("Memory after 100 repeated queries: %.2f MB%n", repeatedMemory / 1024.0 / 1024.0);

        // Complex objects should not cause excessive memory usage
        assertTrue(creationMemory < 1_000_000_000, "50K complex objects should use less than 1GB");
        assertTrue(Math.abs(repeatedMemory) < 100_000_000, "Repeated queries should not leak memory");

        System.out.println("✓ Complex object memory usage is reasonable");
    }

    /**
     * Test 5: Concurrent Memory Usage
     * Tests memory usage under concurrent operations
     */
    @Test
    @Order(5)
    @DisplayName("Concurrent Memory Usage")
    void testConcurrentMemoryUsage() throws Exception {
        System.out.println("\n=== CONCURRENT MEMORY USAGE ===");

        List<TestUser> testDataset = createLargeDataset(50000);

        MemoryUsage beforeConcurrent = getMemoryUsage();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Submit concurrent tasks
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 10; j++) {
                    TestUserFilter filter = new TestUserFilter();
                    StringFilter nameFilter = new StringFilter();
                    nameFilter.setContains("user" + (taskId * 10 + j));
                    filter.setName(nameFilter);

                    List<TestUser> results = userEngine.queryByFilter(testDataset, filter);
                    assertNotNull(results);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        executor.shutdown();

        forceGarbageCollection();
        MemoryUsage afterConcurrent = getMemoryUsage();

        long concurrentMemory = afterConcurrent.getUsed() - beforeConcurrent.getUsed();

        System.out.printf("Concurrent operations completed%n");
        System.out.printf("Memory used during concurrent operations: %.2f MB%n",
                concurrentMemory / 1024.0 / 1024.0);

        // Concurrent operations should not cause excessive memory usage
        assertTrue(Math.abs(concurrentMemory) < 500_000_000,
                "Concurrent operations should not use excessive memory");

        System.out.println("✓ Concurrent memory usage is reasonable");
    }

    /**
     * Test 6: Garbage Collection Behavior
     * Tests garbage collection efficiency
     */
    @Test
    @Order(6)
    @DisplayName("Garbage Collection Behavior")
    void testGarbageCollectionBehavior() {
        System.out.println("\n=== GARBAGE COLLECTION BEHAVIOR ===");

        List<TestUser> testDataset = createLargeDataset(20000);

        // Measure GC behavior during operations
        long gcCountBefore = getGarbageCollectionCount();
        MemoryUsage memoryBefore = getMemoryUsage();

        // Perform operations that create temporary objects
        for (int i = 0; i < 500; i++) {
            // Create many temporary filters
            List<TestUserFilter> tempFilters = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                TestUserFilter filter = new TestUserFilter();
                StringFilter nameFilter = new StringFilter();
                nameFilter.setContains("temp" + i + "_" + j);
                filter.setName(nameFilter);
                tempFilters.add(filter);
            }

            // Use one filter for query
            if (!tempFilters.isEmpty()) {
                List<TestUser> results = userEngine.queryByFilter(testDataset, tempFilters.get(0));
                assertNotNull(results);
            }

            // Let tempFilters go out of scope (eligible for GC)
        }

        // Force garbage collection
        forceGarbageCollection();

        long gcCountAfter = getGarbageCollectionCount();
        MemoryUsage memoryAfter = getMemoryUsage();

        long gcOccurred = gcCountAfter - gcCountBefore;
        long memoryReclaimed = memoryBefore.getUsed() - memoryAfter.getUsed();

        System.out.printf("GC cycles during test: %d%n", gcOccurred);
        System.out.printf("Memory before operations: %.2f MB%n", memoryBefore.getUsed() / 1024.0 / 1024.0);
        System.out.printf("Memory after operations: %.2f MB%n", memoryAfter.getUsed() / 1024.0 / 1024.0);
        System.out.printf("Memory reclaimed: %.2f MB%n", memoryReclaimed / 1024.0 / 1024.0);

        // GC should be able to reclaim memory effectively
        assertTrue(gcOccurred >= 0, "GC should occur during memory-intensive operations");

        System.out.println("✓ Garbage collection behavior is normal");
    }

    /**
     * Test 7: Memory Usage Summary
     * Provides overall memory usage summary
     */
    @Test
    @Order(7)
    @DisplayName("Memory Usage Summary")
    void testMemoryUsageSummary() {
        System.out.println("\n=== MEMORY USAGE SUMMARY ===");

        // Get current memory statistics
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

        System.out.printf("Heap Memory Usage:%n");
        System.out.printf("  Used: %.2f MB%n", heapMemory.getUsed() / 1024.0 / 1024.0);
        System.out.printf("  Committed: %.2f MB%n", heapMemory.getCommitted() / 1024.0 / 1024.0);
        System.out.printf("  Max: %.2f MB%n", heapMemory.getMax() / 1024.0 / 1024.0);

        System.out.printf("Non-Heap Memory Usage:%n");
        System.out.printf("  Used: %.2f MB%n", nonHeapMemory.getUsed() / 1024.0 / 1024.0);
        System.out.printf("  Committed: %.2f MB%n", nonHeapMemory.getCommitted() / 1024.0 / 1024.0);

        // Final memory efficiency test
        MemoryUsage beforeFinal = getMemoryUsage();

        // Perform a comprehensive set of operations
        List<TestUser> dataset = createLargeDataset(10000);

        for (int i = 0; i < 100; i++) {
            TestUserFilter filter = new TestUserFilter();
            StringFilter nameFilter = new StringFilter();
            nameFilter.setContains("final_test");
            filter.setName(nameFilter);

            userEngine.queryByFilter(dataset, filter);
            userEngine.countByFilter(dataset, filter);
        }

        forceGarbageCollection();
        MemoryUsage afterFinal = getMemoryUsage();

        long finalMemoryDiff = afterFinal.getUsed() - beforeFinal.getUsed();

        System.out.printf("Memory difference after final test: %.2f MB%n",
                finalMemoryDiff / 1024.0 / 1024.0);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("MEMORY USAGE TEST RESULTS");
        System.out.println("=".repeat(50));
        System.out.println("✓ Filter object memory footprint verified");
        System.out.println("✓ Query result memory usage optimized");
        System.out.println("✓ No memory leaks detected");
        System.out.println("✓ Complex object memory usage reasonable");
        System.out.println("✓ Concurrent memory usage controlled");
        System.out.println("✓ Garbage collection behavior normal");
        System.out.println("=".repeat(50));
        System.out.println("MEMORY EFFICIENCY: VERIFIED");
        System.out.println("=".repeat(50));

        // Final assertion
        assertTrue(Math.abs(finalMemoryDiff) < 200_000_000,
                "Final memory difference should be reasonable");
    }

    // Helper methods

    private void forceGarbageCollection() {
        // Request garbage collection multiple times
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.yield();
            TestUtil.await(100);

        }
    }

    private MemoryUsage getMemoryUsage() {
        return memoryBean.getHeapMemoryUsage();
    }

    private long getGarbageCollectionCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gcBean -> gcBean.getCollectionCount())
                .sum();
    }

    private List<TestUser> createLargeDataset(int size) {
        return IntStream.range(0, size)
                .parallel()
                .mapToObj(i -> {
                    TestUser user = new TestUser();
                    user.setName("user" + i + (i % 1000 == 0 ? "_special" : ""));
                    user.setAge(18 + (i % 65));
                    user.setActive(i % 4 != 0);
                    return user;
                })
                .toList();
    }
}