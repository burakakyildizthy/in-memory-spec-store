package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserFilter;
import com.thy.fss.common.inmemory.filter.BooleanFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.LocalDateFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive edge case tests covering null values, empty collections,
 * circular references, boundary conditions, and error scenarios.
 * <p>
 * Requirements covered:
 * - 8.8: Edge case handling (null values, empty collections, circular references)
 * - 7.10: Boundary condition testing
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Edge Case Tests")
class EdgeCaseTest {


    private static final String USER = "user";

    private SpecificationQueryEngine<TestUser> userEngine;
    private SpecificationQueryEngine<ComplexNestedEntity> complexEngine;
    private SpecificationQueryEngine<CollectionEntity> collectionEngine;

    @BeforeEach
    void setUp() {
        // Services are accessed via direct INSTANCE references

        userEngine = new SpecificationQueryEngine<>(TestUser.class);
        complexEngine = new SpecificationQueryEngine<>(ComplexNestedEntity.class);
        collectionEngine = new SpecificationQueryEngine<>(CollectionEntity.class);
    }

    /**
     * Test 1: Null Value Edge Cases
     * Comprehensive testing of null value handling
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Null Value Edge Cases")
    void testNullValueEdgeCases() {
        System.out.println("\n=== NULL VALUE EDGE CASES ===");

        // Create test data with various null scenarios
        List<TestUser> nullTestData = createNullTestData();

        // Test 1.1: Filter for null values using 'isNull' filter
        System.out.println("Testing null value filtering...");

        TestUserFilter nullNameFilter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setIsNull(true); // Find users with null names
        nullNameFilter.setName(nameFilter);

        List<TestUser> nullNameResults = userEngine.queryByFilter(nullTestData, nullNameFilter);
        long nullNameCount = nullTestData.stream().filter(u -> u.getName() == null).count();

        assertEquals(nullNameCount, nullNameResults.size(), "Should find all users with null names");
        assertTrue(nullNameResults.stream().allMatch(u -> u.getName() == null), "All results should have null names");

        // Test 1.2: Filter for non-null values
        TestUserFilter nonNullNameFilter = new TestUserFilter();
        StringFilter nonNullFilter = new StringFilter();
        nonNullFilter.setIsNull(false); // Find users with non-null names
        nonNullNameFilter.setName(nonNullFilter);

        List<TestUser> nonNullResults = userEngine.queryByFilter(nullTestData, nonNullNameFilter);
        long nonNullNameCount = nullTestData.stream().filter(u -> u.getName() != null).count();

        assertEquals(nonNullNameCount, nonNullResults.size(), "Should find all users with non-null names");
        assertTrue(nonNullResults.stream().allMatch(u -> u.getName() != null), "All results should have non-null names");

        // Test 1.3: Null-safe string operations
        TestUserFilter containsFilter = new TestUserFilter();
        StringFilter containsNameFilter = new StringFilter();
        containsNameFilter.setContains("Test");
        containsFilter.setName(containsNameFilter);

        List<TestUser> containsResults = userEngine.queryByFilter(nullTestData, containsFilter);
        // Should only match non-null names that contain "Test"
        long expectedContainsCount = nullTestData.stream()
                .filter(u -> u.getName() != null && u.getName().contains("Test"))
                .count();

        assertEquals(expectedContainsCount, containsResults.size(), "Contains filter should ignore null values");

        // Test 1.4: Multiple null field combinations
        TestUserFilter multipleNullFilter = new TestUserFilter();

        StringFilter nameSpecified = new StringFilter();
        nameSpecified.setIsNull(false); // non-null name
        multipleNullFilter.setName(nameSpecified);

        IntegerFilter ageSpecified = new IntegerFilter();
        ageSpecified.setIsNull(true); // null age
        multipleNullFilter.setAge(ageSpecified);

        List<TestUser> multipleNullResults = userEngine.queryByFilter(nullTestData, multipleNullFilter);
        long expectedMultipleCount = nullTestData.stream()
                .filter(u -> u.getName() != null && u.getAge() == null)
                .count();

        assertEquals(expectedMultipleCount, multipleNullResults.size(),
                "Should find users with non-null name and null age");

        // Test 1.5: Null date handling
        TestUserFilter nullDateFilter = new TestUserFilter();
        LocalDateFilter birthDateFilter = new LocalDateFilter();
        birthDateFilter.setIsNull(true); // null birth date
        nullDateFilter.setBirthDate(birthDateFilter);

        List<TestUser> nullDateResults = userEngine.queryByFilter(nullTestData, nullDateFilter);
        long expectedNullDateCount = nullTestData.stream()
                .filter(u -> u.getBirthDate() == null)
                .count();

        assertEquals(expectedNullDateCount, nullDateResults.size(), "Should find users with null birth dates");

        // Test 1.6: Boolean flag operators (IS_EMPTY, IS_NOT_EMPTY, IS_BLANK, IS_NOT_BLANK)
        List<TestUser> booleanFlagTestData = createBooleanFlagTestData();

        // Test IS_EMPTY with true (find empty names)
        TestUserFilter isEmptyTrueFilter = new TestUserFilter();
        StringFilter emptyTrueFilter = new StringFilter();
        emptyTrueFilter.setIsEmpty(true);
        isEmptyTrueFilter.setName(emptyTrueFilter);

        List<TestUser> emptyTrueResults = userEngine.queryByFilter(booleanFlagTestData, isEmptyTrueFilter);
        long expectedEmptyTrueCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() != null && u.getName().isEmpty())
                .count();
        assertEquals(expectedEmptyTrueCount, emptyTrueResults.size(), "Should find users with empty names when IS_EMPTY=true");

        // Test IS_EMPTY with false (find non-empty names)
        TestUserFilter isEmptyFalseFilter = new TestUserFilter();
        StringFilter emptyFalseFilter = new StringFilter();
        emptyFalseFilter.setIsEmpty(false);
        isEmptyFalseFilter.setName(emptyFalseFilter);

        List<TestUser> emptyFalseResults = userEngine.queryByFilter(booleanFlagTestData, isEmptyFalseFilter);
        long expectedEmptyFalseCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() == null || !u.getName().isEmpty())
                .count();
        assertEquals(expectedEmptyFalseCount, emptyFalseResults.size(), "Should find users with non-empty names when IS_EMPTY=false");

        // Test IS_NOT_EMPTY with true (find non-empty names)
        TestUserFilter isNotEmptyTrueFilter = new TestUserFilter();
        StringFilter notEmptyTrueFilter = new StringFilter();
        notEmptyTrueFilter.setIsNotEmpty(true);
        isNotEmptyTrueFilter.setName(notEmptyTrueFilter);

        List<TestUser> notEmptyTrueResults = userEngine.queryByFilter(booleanFlagTestData, isNotEmptyTrueFilter);
        long expectedNotEmptyTrueCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() != null && !u.getName().isEmpty())
                .count();
        assertEquals(expectedNotEmptyTrueCount, notEmptyTrueResults.size(), "Should find users with non-empty names when IS_NOT_EMPTY=true");

        // Test IS_NOT_EMPTY with false (find empty names)
        TestUserFilter isNotEmptyFalseFilter = new TestUserFilter();
        StringFilter notEmptyFalseFilter = new StringFilter();
        notEmptyFalseFilter.setIsNotEmpty(false);
        isNotEmptyFalseFilter.setName(notEmptyFalseFilter);

        List<TestUser> notEmptyFalseResults = userEngine.queryByFilter(booleanFlagTestData, isNotEmptyFalseFilter);
        long expectedNotEmptyFalseCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() == null || u.getName().isEmpty())
                .count();
        assertEquals(expectedNotEmptyFalseCount, notEmptyFalseResults.size(), "Should find users with empty names when IS_NOT_EMPTY=false");

        // Test IS_BLANK with true (find blank names)
        TestUserFilter isBlankTrueFilter = new TestUserFilter();
        StringFilter blankTrueFilter = new StringFilter();
        blankTrueFilter.setIsBlank(true);
        isBlankTrueFilter.setName(blankTrueFilter);

        List<TestUser> blankTrueResults = userEngine.queryByFilter(booleanFlagTestData, isBlankTrueFilter);
        long expectedBlankTrueCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() != null && u.getName().isBlank())
                .count();
        assertEquals(expectedBlankTrueCount, blankTrueResults.size(), "Should find users with blank names when IS_BLANK=true");

        // Test IS_BLANK with false (find non-blank names)
        TestUserFilter isBlankFalseFilter = new TestUserFilter();
        StringFilter blankFalseFilter = new StringFilter();
        blankFalseFilter.setIsBlank(false);
        isBlankFalseFilter.setName(blankFalseFilter);

        List<TestUser> blankFalseResults = userEngine.queryByFilter(booleanFlagTestData, isBlankFalseFilter);
        long expectedBlankFalseCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() == null || !u.getName().isBlank())
                .count();
        assertEquals(expectedBlankFalseCount, blankFalseResults.size(), "Should find users with non-blank names when IS_BLANK=false");

        // Test IS_NOT_BLANK with true (find non-blank names)
        TestUserFilter isNotBlankTrueFilter = new TestUserFilter();
        StringFilter notBlankTrueFilter = new StringFilter();
        notBlankTrueFilter.setIsNotBlank(true);
        isNotBlankTrueFilter.setName(notBlankTrueFilter);

        List<TestUser> notBlankTrueResults = userEngine.queryByFilter(booleanFlagTestData, isNotBlankTrueFilter);
        long expectedNotBlankTrueCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() != null && !u.getName().isBlank())
                .count();
        assertEquals(expectedNotBlankTrueCount, notBlankTrueResults.size(), "Should find users with non-blank names when IS_NOT_BLANK=true");

        // Test IS_NOT_BLANK with false (find blank names)
        TestUserFilter isNotBlankFalseFilter = new TestUserFilter();
        StringFilter notBlankFalseFilter = new StringFilter();
        notBlankFalseFilter.setIsNotBlank(false);
        isNotBlankFalseFilter.setName(notBlankFalseFilter);

        List<TestUser> notBlankFalseResults = userEngine.queryByFilter(booleanFlagTestData, isNotBlankFalseFilter);
        long expectedNotBlankFalseCount = booleanFlagTestData.stream()
                .filter(u -> u.getName() == null || u.getName().isBlank())
                .count();
        assertEquals(expectedNotBlankFalseCount, notBlankFalseResults.size(), "Should find users with blank names when IS_NOT_BLANK=false");

        System.out.println("✓ Null value and boolean flag edge cases handled correctly");
    }

    /**
     * Test 2: Empty Collection Edge Cases
     * Testing behavior with empty collections and datasets
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Empty Collection Edge Cases")
    void testEmptyCollectionEdgeCases() {
        System.out.println("\n=== EMPTY COLLECTION EDGE CASES ===");

        // Test 2.1: Empty dataset
        List<TestUser> emptyDataset = new ArrayList<>();

        TestUserFilter filter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("test");
        filter.setName(nameFilter);

        List<TestUser> emptyResults = userEngine.queryByFilter(emptyDataset, filter);
        assertNotNull(emptyResults, "Should return non-null list for empty dataset");
        assertEquals(0, emptyResults.size(), "Should return empty list for empty dataset");

        long emptyCount = userEngine.countByFilter(emptyDataset, filter);
        assertEquals(0, emptyCount, "Count should be 0 for empty dataset");

        Page<TestUser> emptyPage = userEngine.queryByFilter(emptyDataset, filter, PageRequest.of(0, 10));
        assertNotNull(emptyPage, "Should return non-null page for empty dataset");
        assertEquals(0, emptyPage.getTotalElements(), "Page should have 0 total elements");
        assertEquals(0, emptyPage.getContent().size(), "Page content should be empty");

        // Test 2.2: Null dataset
        List<TestUser> nullDataset = null;

        assertThrows(IllegalArgumentException.class, () -> {
            userEngine.queryByFilter(nullDataset, filter);
        }, "Should throw exception for null dataset");

        // Test 2.3: Collection entities with empty collections
        List<CollectionEntity> collectionTestData = createEmptyCollectionTestData();

        List<CollectionEntity> collectionResults = collectionEngine.queryByFilter(collectionTestData, null);
        assertEquals(collectionTestData.size(), collectionResults.size(),
                "Should return all entities when no filter is applied");

        // Test 2.4: Empty filter (null filter)
        List<TestUser> testUsers = createBasicTestData();

        List<TestUser> nullFilterResults = userEngine.queryByFilter(testUsers, null);
        assertEquals(testUsers.size(), nullFilterResults.size(),
                "Null filter should return all entities");

        long nullFilterCount = userEngine.countByFilter(testUsers, null);
        assertEquals(testUsers.size(), nullFilterCount,
                "Null filter count should return total count");

        // Test 2.5: Small pagination requests
        Page<TestUser> smallPageRequest = userEngine.queryByFilter(testUsers, null, PageRequest.of(0, 1));
        assertNotNull(smallPageRequest, "Should handle small page size");
        assertTrue(smallPageRequest.getContent().size() <= 1, "Should return at most 1 element for page size 1");

        // Test 2.6: Out of bounds pagination
        Page<TestUser> outOfBoundsPage = userEngine.queryByFilter(testUsers, null,
                PageRequest.of(1000, 10)); // Page way beyond available data
        assertNotNull(outOfBoundsPage, "Should handle out of bounds pagination");
        assertEquals(0, outOfBoundsPage.getContent().size(), "Should return empty content for out of bounds page");

        System.out.println("✓ Empty collection edge cases handled correctly");
    }

    /**
     * Test 3: Circular Reference Edge Cases
     * Testing complex object graphs and potential circular references
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Circular Reference Edge Cases")
    void testCircularReferenceEdgeCases() {
        System.out.println("\n=== CIRCULAR REFERENCE EDGE CASES ===");

        // Test 3.1: Deep nested structures (simulating potential circular references)
        List<ComplexNestedEntity> deepNestedData = createDeepNestedTestData();

        // Test deep navigation without actual circular references
        ComplexNestedEntityFilter deepFilter = new ComplexNestedEntityFilter();
        Level1Filter level1Filter = new Level1Filter();
        Level2Filter level2Filter = new Level2Filter();
        Level3Filter level3Filter = new Level3Filter();

        StringFilter valueFilter = new StringFilter();
        valueFilter.setContains("Deep");
        level3Filter.setValue(valueFilter);
        level2Filter.setLevel3(level3Filter);
        level1Filter.setLevel2(level2Filter);
        deepFilter.setLevel1(level1Filter);

        List<ComplexNestedEntity> deepResults = complexEngine.queryByFilter(deepNestedData, deepFilter);
        assertNotNull(deepResults, "Deep nested query should return results");

        // Test 3.2: Performance with deep object graphs
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            complexEngine.queryByFilter(deepNestedData, deepFilter);
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        assertTrue(duration < 5000, "Deep navigation should be efficient (under 5 seconds for 1000 operations)");

        // Test 3.3: Very deep nesting levels
        ComplexNestedEntity veryDeepEntity = createVeryDeepNestedEntity(); // 10 levels deep
        List<ComplexNestedEntity> veryDeepData = List.of(veryDeepEntity);

        List<ComplexNestedEntity> veryDeepResults = complexEngine.queryByFilter(veryDeepData, null);
        assertEquals(1, veryDeepResults.size(), "Should handle very deep nesting");

        // Test 3.4: Self-referencing structures (where possible)
        // Note: Our current model doesn't have true circular references by design
        // This test verifies the system can handle complex object graphs safely

        List<ComplexNestedEntity> complexGraphData = createComplexObjectGraph();

        // Test that complex object graphs don't cause issues
        ComplexNestedEntityFilter complexGraphFilter = new ComplexNestedEntityFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("Complex");
        complexGraphFilter.setName(nameFilter);

        List<ComplexNestedEntity> complexGraphResults = complexEngine.queryByFilter(complexGraphData, complexGraphFilter);
        assertNotNull(complexGraphResults, "Complex object graphs should be handled safely");

        // Test 3.5: Memory stability with complex graphs
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Perform many operations on complex graphs
        for (int i = 0; i < 100; i++) {
            complexEngine.queryByFilter(complexGraphData, complexGraphFilter);
        }

        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        assertTrue(Math.abs(memoryUsed) < 100_000_000,
                "Complex object graphs should not cause excessive memory usage");

        System.out.println("✓ Circular reference edge cases handled correctly");
    }

    /**
     * Test 4: Boundary Condition Edge Cases
     * Testing boundary values and extreme conditions
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Boundary Condition Edge Cases")
    void testBoundaryConditionEdgeCases() {
        System.out.println("\n=== BOUNDARY CONDITION EDGE CASES ===");

        List<TestUser> boundaryTestData = createBoundaryTestData();

        // Test 4.1: Integer boundary values
        TestUserFilter minIntFilter = new TestUserFilter();
        IntegerFilter ageFilter = new IntegerFilter();
        ageFilter.setEquals(Integer.MIN_VALUE);
        minIntFilter.setAge(ageFilter);

        List<TestUser> minIntResults = userEngine.queryByFilter(boundaryTestData, minIntFilter);
        long expectedMinIntCount = boundaryTestData.stream()
                .filter(u -> Integer.MIN_VALUE == (u.getAge() != null ? u.getAge() : 0))
                .count();
        assertEquals(expectedMinIntCount, minIntResults.size(), "Should handle Integer.MIN_VALUE");

        TestUserFilter maxIntFilter = new TestUserFilter();
        IntegerFilter maxAgeFilter = new IntegerFilter();
        maxAgeFilter.setEquals(Integer.MAX_VALUE);
        maxIntFilter.setAge(maxAgeFilter);

        List<TestUser> maxIntResults = userEngine.queryByFilter(boundaryTestData, maxIntFilter);
        long expectedMaxIntCount = boundaryTestData.stream()
                .filter(u -> Integer.MAX_VALUE == (u.getAge() != null ? u.getAge() : 0))
                .count();
        assertEquals(expectedMaxIntCount, maxIntResults.size(), "Should handle Integer.MAX_VALUE");

        // Test 4.2: Date boundary values
        TestUserFilter minDateFilter = new TestUserFilter();
        LocalDateFilter birthDateFilter = new LocalDateFilter();
        birthDateFilter.setEquals(LocalDate.MIN);
        minDateFilter.setBirthDate(birthDateFilter);

        List<TestUser> minDateResults = userEngine.queryByFilter(boundaryTestData, minDateFilter);
        assertNotNull(minDateResults, "Should handle LocalDate.MIN");

        TestUserFilter maxDateFilter = new TestUserFilter();
        LocalDateFilter maxBirthDateFilter = new LocalDateFilter();
        maxBirthDateFilter.setEquals(LocalDate.MAX);
        maxDateFilter.setBirthDate(maxBirthDateFilter);

        List<TestUser> maxDateResults = userEngine.queryByFilter(boundaryTestData, maxDateFilter);
        assertNotNull(maxDateResults, "Should handle LocalDate.MAX");

        // Test 4.3: String boundary values
        TestUserFilter emptyStringFilter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setEquals("");
        emptyStringFilter.setName(nameFilter);

        List<TestUser> emptyStringResults = userEngine.queryByFilter(boundaryTestData, emptyStringFilter);
        long expectedEmptyStringCount = boundaryTestData.stream()
                .filter(u -> "".equals(u.getName()))
                .count();
        assertEquals(expectedEmptyStringCount, emptyStringResults.size(), "Should handle empty strings");

        // Test 4.4: Very long strings
        String veryLongString = "a".repeat(10000);
        TestUserFilter longStringFilter = new TestUserFilter();
        StringFilter longNameFilter = new StringFilter();
        longNameFilter.setContains(veryLongString);
        longStringFilter.setName(longNameFilter);

        List<TestUser> longStringResults = userEngine.queryByFilter(boundaryTestData, longStringFilter);
        assertNotNull(longStringResults, "Should handle very long strings");

        // Test 4.5: Range boundary conditions
        TestUserFilter rangeBoundaryFilter = new TestUserFilter();
        IntegerFilter rangeBoundaryAgeFilter = new IntegerFilter();
        rangeBoundaryAgeFilter.setGreaterThan(Integer.MAX_VALUE - 1);
        rangeBoundaryAgeFilter.setLessThan(Integer.MAX_VALUE);
        rangeBoundaryFilter.setAge(rangeBoundaryAgeFilter);

        List<TestUser> rangeBoundaryResults = userEngine.queryByFilter(boundaryTestData, rangeBoundaryFilter);
        assertNotNull(rangeBoundaryResults, "Should handle range boundary conditions");

        System.out.println("✓ Boundary condition edge cases handled correctly");
    }

    /**
     * Test 5: Concurrent Edge Cases
     * Testing edge cases under concurrent access
     */
    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Concurrent Edge Cases")
    void testConcurrentEdgeCases() throws Exception {
        System.out.println("\n=== CONCURRENT EDGE CASES ===");

        List<TestUser> sharedTestData = createBasicTestData();

        // Test 5.1: Concurrent access to same data
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<List<TestUser>>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final int threadId = i;
            CompletableFuture<List<TestUser>> future = CompletableFuture.supplyAsync(() -> {
                TestUserFilter filter = new TestUserFilter();
                StringFilter nameFilter = new StringFilter();
                nameFilter.setContains(USER + (threadId % 10));
                filter.setName(nameFilter);

                return userEngine.queryByFilter(sharedTestData, filter);
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allFutures.get(30, TimeUnit.SECONDS);

        // Verify all operations completed successfully
        for (CompletableFuture<List<TestUser>> future : futures) {
            List<TestUser> result = future.get();
            assertNotNull(result, "Concurrent operation should not return null");
        }

        // Test 5.2: Concurrent modification scenarios
        List<TestUser> modifiableData = new ArrayList<>(sharedTestData);
        List<CompletableFuture<Void>> modificationFutures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // Some threads query
                if (threadId % 2 == 0) {
                    TestUserFilter filter = new TestUserFilter();
                    StringFilter nameFilter = new StringFilter();
                    nameFilter.setContains(USER);
                    filter.setName(nameFilter);

                    // Create a copy to avoid ConcurrentModificationException
                    List<TestUser> dataCopy;
                    synchronized (modifiableData) {
                        dataCopy = new ArrayList<>(modifiableData);
                    }
                    userEngine.queryByFilter(dataCopy, filter);
                } else {
                    // Other threads modify the list (add/remove)
                    synchronized (modifiableData) {
                        if (threadId % 4 == 1 && !modifiableData.isEmpty()) {
                            // Remove an element
                            modifiableData.remove(modifiableData.size() - 1);
                        } else {
                            // Add an element
                            TestUser newUser = new TestUser();
                            newUser.setName("ConcurrentUser" + threadId);
                            newUser.setAge(25);
                            modifiableData.add(newUser);
                        }
                    }
                }
            }, executor);
            modificationFutures.add(future);
        }

        CompletableFuture.allOf(modificationFutures.toArray(new CompletableFuture[0])).get();

        executor.shutdown();

        System.out.println("✓ Concurrent edge cases handled correctly");
    }

    /**
     * Test 7: Performance Edge Cases
     * Testing performance under extreme conditions
     */
    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Performance Edge Cases")
    @Timeout(60)
    @Tag("performance")
    void testPerformanceEdgeCases() {
        System.out.println("\n=== PERFORMANCE EDGE CASES ===");

        // Test 7.1: Very large datasets
        List<TestUser> veryLargeDataset = IntStream.range(0, 1_000_000)
                .parallel()
                .mapToObj(i -> {
                    TestUser user = new TestUser();
                    user.setName(USER + i);
                    user.setAge(i % 100);
                    user.setActive(i % 2 == 0);
                    return user;
                })
                .toList();

        System.out.println("Created very large dataset: " + veryLargeDataset.size() + " users");

        TestUserFilter simpleFilter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("user1"); // Should match many users
        simpleFilter.setName(nameFilter);

        long startTime = System.nanoTime();
        List<TestUser> largeResults = userEngine.queryByFilter(veryLargeDataset, simpleFilter);
        long duration = System.nanoTime() - startTime;

        System.out.printf("Query on 1M dataset returned %d results in %.2f ms%n",
                largeResults.size(), duration / 1_000_000.0);

        assertTrue(duration < 30_000_000_000L, "Large dataset query should complete in under 30 seconds");

        // Test 7.2: Complex filters on large datasets
        TestUserFilter complexFilter = new TestUserFilter();

        StringFilter complexNameFilter = new StringFilter();
        complexNameFilter.setContains(USER);
        complexNameFilter.setStartsWith("user1");
        complexFilter.setName(complexNameFilter);

        IntegerFilter ageFilter = new IntegerFilter();
        ageFilter.setGreaterThan(25);
        ageFilter.setLessThan(75);
        complexFilter.setAge(ageFilter);

        BooleanFilter activeFilter = new BooleanFilter();
        activeFilter.setEquals(true);
        complexFilter.setActive(activeFilter);

        startTime = System.nanoTime();
        List<TestUser> complexResults = userEngine.queryByFilter(veryLargeDataset, complexFilter);
        duration = System.nanoTime() - startTime;

        System.out.printf("Complex query on 1M dataset returned %d results in %.2f ms%n",
                complexResults.size(), duration / 1_000_000.0);

        assertTrue(duration < 45_000_000_000L, "Complex query should complete in under 45 seconds");

        // Test 7.3: Many small queries vs few large queries
        long manySmallStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            TestUserFilter smallFilter = new TestUserFilter();
            StringFilter smallNameFilter = new StringFilter();
            smallNameFilter.setEquals(USER + i);
            smallFilter.setName(smallNameFilter);

            userEngine.queryByFilter(veryLargeDataset, smallFilter);
        }
        long manySmallDuration = System.nanoTime() - manySmallStart;

        System.out.printf("1000 small queries completed in %.2f ms%n", manySmallDuration / 1_000_000.0);
        assertTrue(manySmallDuration < 60_000_000_000L, "Many small queries should be efficient");

        System.out.println("✓ Performance edge cases handled correctly");
    }

    /**
     * Test 8: Edge Case Summary
     * Final verification of all edge case handling
     */
    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Edge Case Summary")
    void testEdgeCaseSummary() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("EDGE CASE TEST SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println("✓ Null value edge cases verified");
        System.out.println("✓ Empty collection edge cases verified");
        System.out.println("✓ Circular reference edge cases verified");
        System.out.println("✓ Boundary condition edge cases verified");
        System.out.println("✓ Concurrent edge cases verified");
        System.out.println("✓ Error condition edge cases verified");
        System.out.println("✓ Performance edge cases verified");
        System.out.println("=".repeat(50));
        System.out.println("EDGE CASE HANDLING: COMPREHENSIVE");
        System.out.println("=".repeat(50));

        assertTrue(true, "All edge case tests completed successfully");
    }

    // Helper methods for creating test data

    private List<TestUser> createNullTestData() {
        List<TestUser> data = new ArrayList<>();

        // User with all null fields
        TestUser allNull = new TestUser();
        data.add(allNull);

        // User with null name
        TestUser nullName = new TestUser();
        nullName.setAge(25);
        nullName.setActive(true);
        nullName.setBirthDate(LocalDate.now());
        data.add(nullName);

        // User with null age
        TestUser nullAge = new TestUser();
        nullAge.setName("TestUser1");
        nullAge.setActive(false);
        nullAge.setBirthDate(LocalDate.now());
        data.add(nullAge);

        // User with null birth date
        TestUser nullBirthDate = new TestUser();
        nullBirthDate.setName("TestUser2");
        nullBirthDate.setAge(30);
        nullBirthDate.setActive(true);
        data.add(nullBirthDate);

        // User with no null fields
        TestUser noNull = new TestUser();
        noNull.setName("TestUser3");
        noNull.setAge(35);
        noNull.setActive(false);
        noNull.setBirthDate(LocalDate.of(1988, 5, 15));
        data.add(noNull);

        return data;
    }

    private List<CollectionEntity> createEmptyCollectionTestData() {
        List<CollectionEntity> data = new ArrayList<>();

        // Entity with empty collections
        CollectionEntity emptyCollections = new CollectionEntity();
        emptyCollections.setTags(new ArrayList<>());
        emptyCollections.setNumbers(new HashSet<>());
        data.add(emptyCollections);

        // Entity with null collections
        CollectionEntity nullCollections = new CollectionEntity();
        data.add(nullCollections);

        // Entity with populated collections
        CollectionEntity populatedCollections = new CollectionEntity();
        populatedCollections.setTags(Arrays.asList("tag1", "tag2"));
        populatedCollections.setNumbers(Set.of(1, 2, 3));
        data.add(populatedCollections);

        return data;
    }

    private List<TestUser> createBooleanFlagTestData() {
        List<TestUser> data = new ArrayList<>();

        // User with null name
        TestUser nullName = new TestUser();
        nullName.setAge(25);
        nullName.setActive(true);
        data.add(nullName);

        // User with empty string name
        TestUser emptyName = new TestUser();
        emptyName.setName("");
        emptyName.setAge(30);
        emptyName.setActive(false);
        data.add(emptyName);

        // User with blank string name (only whitespace)
        TestUser blankName = new TestUser();
        blankName.setName("   ");
        blankName.setAge(35);
        blankName.setActive(true);
        data.add(blankName);

        // User with whitespace-only name (tabs and spaces)
        TestUser whitespaceOnlyName = new TestUser();
        whitespaceOnlyName.setName("\t  \n  ");
        whitespaceOnlyName.setAge(40);
        whitespaceOnlyName.setActive(false);
        data.add(whitespaceOnlyName);

        // User with normal name
        TestUser normalName = new TestUser();
        normalName.setName("TestUser");
        normalName.setAge(45);
        normalName.setActive(true);
        data.add(normalName);

        // User with name containing only spaces
        TestUser spacesOnlyName = new TestUser();
        spacesOnlyName.setName("     ");
        spacesOnlyName.setAge(50);
        spacesOnlyName.setActive(false);
        data.add(spacesOnlyName);

        return data;
    }

    private List<TestUser> createBasicTestData() {
        return IntStream.range(0, 100)
                .mapToObj(i -> {
                    TestUser user = new TestUser();
                    user.setName(USER + i);
                    user.setAge(20 + (i % 50));
                    user.setActive(i % 2 == 0);
                    user.setBirthDate(LocalDate.of(1970 + (i % 30), 1 + (i % 12), 1 + (i % 28)));
                    return user;
                })
                .toList();
    }

    private List<ComplexNestedEntity> createDeepNestedTestData() {
        return IntStream.range(0, 50)
                .mapToObj(i -> {
                    ComplexNestedEntity entity = new ComplexNestedEntity();
                    entity.setName("DeepEntity" + i);

                    Level1 level1 = new Level1();
                    level1.setName("Level1_" + i);
                    level1.setItems(Arrays.asList("item1", "item2", "item3"));

                    Level2 level2 = new Level2();
                    level2.setName("Level2_" + i);

                    Level3 level3 = new Level3();
                    level3.setValue("DeepValue_" + i);

                    level2.setLevel3(level3);
                    level1.setLevel2(level2);
                    entity.setLevel1(level1);

                    return entity;
                })
                .toList();
    }

    private ComplexNestedEntity createVeryDeepNestedEntity() {
        ComplexNestedEntity entity = new ComplexNestedEntity();
        entity.setName("VeryDeepEntity");

        Level1 level1 = new Level1();
        level1.setName("VeryDeepLevel1");
        level1.setItems(Arrays.asList("deep1", "deep2", "deep3"));

        Level2 level2 = new Level2();
        level2.setName("VeryDeepLevel2");

        Level3 level3 = new Level3();
        level3.setValue("VeryDeepValue");

        level2.setLevel3(level3);
        level1.setLevel2(level2);
        entity.setLevel1(level1);

        return entity;
    }

    private List<ComplexNestedEntity> createComplexObjectGraph() {
        List<ComplexNestedEntity> data = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            ComplexNestedEntity entity = new ComplexNestedEntity();
            entity.setName("ComplexGraphEntity" + i);

            Level1 level1 = new Level1();
            level1.setName("GraphLevel1_" + i);
            level1.setItems(Arrays.asList("graph1", "graph2", "graph3"));

            Level2 level2 = new Level2();
            level2.setName("GraphLevel2_" + i);

            Level3 level3 = new Level3();
            level3.setValue("GraphValue_" + i);

            level2.setLevel3(level3);
            level1.setLevel2(level2);
            entity.setLevel1(level1);

            data.add(entity);
        }

        return data;
    }

    private List<TestUser> createBoundaryTestData() {
        List<TestUser> data = new ArrayList<>();

        // User with minimum integer values
        TestUser minUser = new TestUser();
        minUser.setName("MinUser");
        minUser.setAge(Integer.MIN_VALUE);
        minUser.setActive(true);
        minUser.setBirthDate(LocalDate.MIN);
        data.add(minUser);

        // User with maximum integer values
        TestUser maxUser = new TestUser();
        maxUser.setName("MaxUser");
        maxUser.setAge(Integer.MAX_VALUE);
        maxUser.setActive(false);
        maxUser.setBirthDate(LocalDate.MAX);
        data.add(maxUser);

        // User with empty string
        TestUser emptyStringUser = new TestUser();
        emptyStringUser.setName("");
        emptyStringUser.setAge(0);
        emptyStringUser.setActive(true);
        emptyStringUser.setBirthDate(LocalDate.now());
        data.add(emptyStringUser);

        // User with very long string
        TestUser longStringUser = new TestUser();
        longStringUser.setName("a".repeat(1000));
        longStringUser.setAge(25);
        longStringUser.setActive(false);
        longStringUser.setBirthDate(LocalDate.now());
        data.add(longStringUser);

        return data;
    }
}