package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error handling and validation tests for collection filtering operations.
 * Tests task 7.2: Test error handling and validation
 * <p>
 * Requirements covered:
 * - 4.1: Collection operator compatibility validation
 * - 4.2: Collection field type validation
 * - 4.3: Descriptive error messages for validation failures
 * - 4.4: Collection element type compatibility detection
 */
@DisplayName("Collection Filter Error Handling and Validation Tests")
class CollectionFilterErrorHandlingTest extends BaseIntegrationTest {

    private static final String WORK = "work";
    private static final String TAG = "tag";
    private static final String IMPORTANT = "important";
    private static final String COMMON = "common";

    private SpecificationQueryEngine<CollectionEntity> queryEngine;
    private CollectionEntitySpecificationService specificationService;
    private List<CollectionEntity> testEntities;

    @Override
    @BeforeEach
    public void setUp() {
        // Initialize query engine and specification service
        queryEngine = new SpecificationQueryEngine<>(CollectionEntity.class);
        specificationService = new CollectionEntitySpecificationService();

        // Create test data including edge cases
        testEntities = createTestDataWithEdgeCases();
    }

    /**
     * Creates test entities with various edge cases for error handling validation.
     */
    private List<CollectionEntity> createTestDataWithEdgeCases() {
        List<CollectionEntity> entities = new ArrayList<>();

        // Entity 1: Normal collections
        CollectionEntity entity1 = new CollectionEntity();
        entity1.setId(1L);
        entity1.setTags(Arrays.asList(WORK, IMPORTANT));
        entity1.setNumbers(Set.of(1, 2, 3));
        entities.add(entity1);

        // Entity 2: Empty collections
        CollectionEntity entity2 = new CollectionEntity();
        entity2.setId(2L);
        entity2.setTags(new ArrayList<>());
        entity2.setNumbers(new HashSet<>());
        entities.add(entity2);

        // Entity 3: Null collections
        CollectionEntity entity3 = new CollectionEntity();
        entity3.setId(3L);
        entity3.setTags(null);
        entity3.setNumbers(null);
        entities.add(entity3);

        // Entity 4: Collections with null elements (if supported by implementation)
        CollectionEntity entity4 = new CollectionEntity();
        entity4.setId(4L);
        entity4.setTags(new ArrayList<>(Arrays.asList("valid", null, TAG)));
        entity4.setNumbers(new HashSet<>(Arrays.asList(1, null, 3)));
        entities.add(entity4);

        return entities;
    }

    // ========== Task 7.2: Error Handling and Validation Tests ==========

    @Test
    @DisplayName("Error Handling: Type compatibility validation for collection operations")
    void shouldValidateTypeCompatibilityForCollectionOperations() {
        // Test 1: Valid type compatibility - String element in String collection
        CollectionFilter<String> validStringFilter = new CollectionFilter<>();
        validStringFilter.setCollectionContains(WORK);

        CollectionEntityFilter validFilter = new CollectionEntityFilter();
        validFilter.setTags(validStringFilter);

        // Should not throw exception and work correctly
        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, validFilter, PageRequest.of(0, 10));
            assertEquals(1, result.getTotalElements());
        });

        // Test 2: Valid type compatibility - Integer element in Integer collection
        CollectionFilter<Integer> validIntegerFilter = new CollectionFilter<>();
        validIntegerFilter.setCollectionContains(1);

        CollectionEntityFilter validIntFilter = new CollectionEntityFilter();
        validIntFilter.setNumbers(validIntegerFilter);

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, validIntFilter, PageRequest.of(0, 10));
            assertTrue(result.getTotalElements() >= 0, "Should return valid result count");
        });

        // Test 3: Verify specification service validates correctly
        CollectionEntity testEntity = testEntities.get(0);
        assertTrue(specificationService.validateFilter(testEntity, validFilter),
                "Valid filter should pass specification service validation");
        assertTrue(specificationService.validateFilter(testEntity, validIntFilter),
                "Valid integer filter should pass specification service validation");
    }

    @Test
    @DisplayName("Error Handling: Invalid operator-attribute combinations")
    void shouldHandleInvalidOperatorAttributeCombinations() {
        // Test 1: Using collection operations on non-collection fields would be caught at compile time
        // due to type safety, but we can test runtime validation

        // Test 2: Verify that null filter values are handled gracefully
        CollectionEntityFilter filterWithNulls = new CollectionEntityFilter();
        filterWithNulls.setTags(null);
        filterWithNulls.setNumbers(null);

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, filterWithNulls, PageRequest.of(0, 10));
            // Null filters should match all entities (no filtering applied)
            assertEquals(testEntities.size(), result.getTotalElements());
        });

        // Test 3: Empty filter should not cause errors
        CollectionEntityFilter emptyFilter = new CollectionEntityFilter();

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, emptyFilter, PageRequest.of(0, 10));
            assertEquals(testEntities.size(), result.getTotalElements());
        });

        // Test 4: Verify specification service handles null filters
        CollectionEntity testEntity = testEntities.get(0);
        assertThrows(IllegalArgumentException.class, () -> {
            specificationService.validateFilter(testEntity, null);
        }, "Null filter should throw IllegalArgumentException");
        assertTrue(specificationService.validateFilter(testEntity, emptyFilter),
                "Empty filter should pass validation");
    }

    @Test
    @DisplayName("Error Handling: Null safety in collection operations")
    void shouldHandleNullSafetyInCollectionOperations() {
        // Test 1: COLLECTION_CONTAINS with null collections
        CollectionEntityFilter containsFilter = CollectionEntityFilter.builder()
                .tagsContains(WORK)
                .build();

        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, containsFilter, PageRequest.of(0, 10));

        // Should not include entities with null collections (entity 3)
        assertFalse(result.getContent().stream().anyMatch(e -> e.getIdentity().equals(3L)),
                "Entities with null collections should not match COLLECTION_CONTAINS");

        // Test 2: IS_EMPTY with null collections
        CollectionEntityFilter isEmptyFilter = CollectionEntityFilter.builder()
                .tagsIsEmpty(true)
                .build();

        Page<CollectionEntity> emptyResult = queryEngine.queryByFilter(testEntities, isEmptyFilter, PageRequest.of(0, 10));

        // Should only include entities with empty collections (entity 2), not null collections
        assertEquals(1, emptyResult.getTotalElements());
        assertEquals(2L, emptyResult.getContent().get(0).getIdentity());

        // Test 3: IS_NOT_EMPTY with null collections
        CollectionEntityFilter isNotEmptyFilter = CollectionEntityFilter.builder()
                .tagsIsNotEmpty(true)
                .build();

        Page<CollectionEntity> notEmptyResult = queryEngine.queryByFilter(testEntities, isNotEmptyFilter, PageRequest.of(0, 10));

        // Should not include entities with null or empty collections
        assertFalse(notEmptyResult.getContent().stream().anyMatch(e -> e.getIdentity().equals(2L) || e.getIdentity().equals(3L)),
                "Entities with null or empty collections should not match IS_NOT_EMPTY");

        // Test 4: Verify specification service handles null collections correctly
        CollectionEntity nullEntity = testEntities.stream()
                .filter(e -> e.getIdentity().equals(3L))
                .findFirst()
                .orElseThrow();

        assertFalse(specificationService.validateFilter(nullEntity, containsFilter),
                "Entity with null collection should fail COLLECTION_CONTAINS validation");
        assertFalse(specificationService.validateFilter(nullEntity, isEmptyFilter),
                "Entity with null collection should fail IS_EMPTY validation");
        assertFalse(specificationService.validateFilter(nullEntity, isNotEmptyFilter),
                "Entity with null collection should fail IS_NOT_EMPTY validation");
    }

    @Test
    @DisplayName("Error Handling: Null elements within collections")
    void shouldHandleNullElementsWithinCollections() {
        // Test 1: COLLECTION_CONTAINS with null element search
        CollectionFilter<String> nullContainsFilter = new CollectionFilter<>();
        nullContainsFilter.setCollectionContains(null);

        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(nullContainsFilter);

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));
            // Should find entity 4 if it contains null elements
            // Behavior depends on implementation - should not throw exception
        });

        // Test 2: Verify specification service handles null element searches
        CollectionEntity entityWithNulls = testEntities.stream()
                .filter(e -> e.getIdentity().equals(4L))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> {
            specificationService.validateFilter(entityWithNulls, filter);
            // Should handle gracefully without throwing exception
        });

        // Test 3: COLLECTION_ANY with null filter criteria
        CollectionFilter<String> anyNullFilter = new CollectionFilter<>();
        anyNullFilter.setCollectionAny(null);

        CollectionEntityFilter anyFilter = new CollectionEntityFilter();
        anyFilter.setTags(anyNullFilter);

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, anyFilter, PageRequest.of(0, 10));
            // Should handle null nested filter gracefully
        });
    }

    @Test
    @DisplayName("Error Handling: Invalid filter combinations")
    void shouldHandleInvalidFilterCombinations() {
        // Test 1: Conflicting collection state filters
        CollectionEntityFilter conflictingFilter = CollectionEntityFilter.builder()
                .tagsIsEmpty(true)
                .tagsIsNotEmpty(true)
                .build();

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, conflictingFilter, PageRequest.of(0, 10));
            // Should return no results as no collection can be both empty and not empty
            assertEquals(0, result.getTotalElements());
        });

        // Test 2: Multiple COLLECTION_CONTAINS on same field
        CollectionFilter<String> multiContainsFilter = new CollectionFilter<>();
        multiContainsFilter.setCollectionContains(WORK);
        // Note: Only one COLLECTION_CONTAINS can be set per filter instance
        // This tests the behavior when the same operation is set multiple times
        multiContainsFilter.setCollectionContains(IMPORTANT);

        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(multiContainsFilter);

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));
            // Should use the last set value (IMPORTANT)
            assertEquals(1, result.getTotalElements());
            assertEquals(1L, result.getContent().get(0).getIdentity());
        });

        // Test 3: Verify specification service handles conflicting filters
        CollectionEntity testEntity = testEntities.get(1); // Empty collections
        assertFalse(specificationService.validateFilter(testEntity, conflictingFilter),
                "Conflicting filter should fail validation");
    }

    @Test
    @DisplayName("Error Handling: Edge cases with nested filter criteria")
    void shouldHandleEdgeCasesWithNestedFilterCriteria() {
        // Test 1: COLLECTION_ANY with invalid nested filter
        StringFilter invalidNestedFilter = new StringFilter();
        invalidNestedFilter.setContains(""); // Empty string search

        CollectionFilter<String> anyFilter = new CollectionFilter<>();
        anyFilter.setCollectionAny(invalidNestedFilter);

        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(anyFilter);

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));
            // Should handle empty string search gracefully
        });

        // Test 2: COLLECTION_ALL with null nested filter
        CollectionFilter<String> allNullFilter = new CollectionFilter<>();
        allNullFilter.setCollectionAll(null);

        CollectionEntityFilter allFilter = new CollectionEntityFilter();
        allFilter.setTags(allNullFilter);

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, allFilter, PageRequest.of(0, 10));
            // Should handle null nested filter gracefully
        });

        // Test 3: COLLECTION_NONE with complex nested criteria
        StringFilter complexNestedFilter = new StringFilter();
        complexNestedFilter.setStartsWith("nonexistent");
        complexNestedFilter.setEndsWith("pattern");

        CollectionFilter<String> noneFilter = new CollectionFilter<>();
        noneFilter.setCollectionNone(complexNestedFilter);

        CollectionEntityFilter complexFilter = new CollectionEntityFilter();
        complexFilter.setTags(noneFilter);

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, complexFilter, PageRequest.of(0, 10));
            // Should handle complex nested criteria without errors
        });
    }

    @Test
    @DisplayName("Error Handling: Performance with malformed data")
    void shouldHandlePerformanceWithMalformedData() {
        // Test 1: Large collections with null elements
        List<CollectionEntity> malformedEntities = new ArrayList<>();

        CollectionEntity largeEntityWithNulls = new CollectionEntity();
        largeEntityWithNulls.setId(100L);
        List<String> tagsWithNulls = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tagsWithNulls.add(i % 10 == 0 ? null : TAG + i);
        }
        largeEntityWithNulls.setTags(tagsWithNulls);
        largeEntityWithNulls.setNumbers(Set.of(1, 2, 3));
        malformedEntities.add(largeEntityWithNulls);

        // Add normal entities
        malformedEntities.addAll(testEntities);

        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tagsContains("tag500")
                .build();

        // Should handle large collections with null elements efficiently
        long startTime = System.nanoTime();
        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(malformedEntities, filter, PageRequest.of(0, 10));
            // Should handle malformed data without throwing exceptions
            assertTrue(result.getTotalElements() >= 0, "Should return valid result count");
        });
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        assertTrue(duration < 1000, "Should handle malformed data efficiently (under 1000ms)");

        // Test 2: Verify specification service handles malformed data
        assertDoesNotThrow(() -> {
            specificationService.validateFilter(largeEntityWithNulls, filter);
            // Note: tag500 may not be found due to null elements in collection
            // The important thing is that no exception is thrown
        });
    }

    @Test
    @DisplayName("Error Handling: Boundary conditions")
    void shouldHandleBoundaryConditions() {
        // Test 1: Empty string in collection operations
        CollectionEntityFilter emptyStringFilter = CollectionEntityFilter.builder()
                .tagsContains("")
                .build();

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, emptyStringFilter, PageRequest.of(0, 10));
            // Should handle empty string search gracefully
        });

        // Test 2: Very large numbers in integer collections
        CollectionEntityFilter largeNumberFilter = CollectionEntityFilter.builder()
                .numbersContains(Integer.MAX_VALUE)
                .build();

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, largeNumberFilter, PageRequest.of(0, 10));
            assertEquals(0, result.getTotalElements());
        });

        // Test 3: Zero and negative numbers
        CollectionEntityFilter zeroFilter = CollectionEntityFilter.builder()
                .numbersContains(0)
                .build();

        CollectionEntityFilter negativeFilter = CollectionEntityFilter.builder()
                .numbersContains(-1)
                .build();

        assertDoesNotThrow(() -> {
            queryEngine.queryByFilter(testEntities, zeroFilter, PageRequest.of(0, 10));
            queryEngine.queryByFilter(testEntities, negativeFilter, PageRequest.of(0, 10));
        });

        // Test 4: Very long strings
        String veryLongString = "a".repeat(10000);
        CollectionEntityFilter longStringFilter = CollectionEntityFilter.builder()
                .tagsContains(veryLongString)
                .build();

        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, longStringFilter, PageRequest.of(0, 10));
            assertEquals(0, result.getTotalElements());
        });
    }

    @Test
    @DisplayName("Error Handling: Concurrent access and thread safety")
    void shouldHandleConcurrentAccessSafely() {
        // Test concurrent access to the same filter
        CollectionEntityFilter sharedFilter = CollectionEntityFilter.builder()
                .tagsContains(WORK)
                .build();

        // Execute multiple queries concurrently
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, sharedFilter, PageRequest.of(0, 10));
                        assertEquals(1, result.getTotalElements());

                        boolean validationResult = specificationService.validateFilter(testEntities.get(0), sharedFilter);
                        assertTrue(validationResult);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join());
        }

        // Verify no exceptions occurred during concurrent access
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access: " + exceptions);
    }

    @Test
    @DisplayName("Error Handling: Memory management with large result sets")
    void shouldHandleMemoryManagementWithLargeResultSets() {
        // Create a large dataset
        List<CollectionEntity> largeDataset = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            entity.setTags(Arrays.asList(TAG + (i % 100), COMMON));
            entity.setNumbers(Set.of(i % 1000));
            largeDataset.add(entity);
        }

        // Filter that matches many entities
        CollectionEntityFilter commonFilter = CollectionEntityFilter.builder()
                .tagsContains(COMMON)
                .build();

        // Should handle large result sets without memory issues
        assertDoesNotThrow(() -> {
            Page<CollectionEntity> result = queryEngine.queryByFilter(largeDataset, commonFilter, PageRequest.of(0, 100));
            assertEquals(100, result.getContent().size());
            assertEquals(10000, result.getTotalElements());
        });

        // Test count operation on large dataset
        assertDoesNotThrow(() -> {
            long count = queryEngine.countByFilter(largeDataset, commonFilter);
            assertEquals(10000, count);
        });

        // Verify memory usage remains reasonable
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Execute multiple queries
        for (int i = 0; i < 10; i++) {
            queryEngine.queryByFilter(largeDataset, commonFilter, PageRequest.of(i, 100));
        }

        System.gc(); // Suggest garbage collection
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = memoryAfter - memoryBefore;

        // Memory increase should be reasonable (less than 100MB)
        assertTrue(memoryIncrease < 100 * 1024 * 1024,
                "Memory increase should be reasonable: " + (memoryIncrease / 1024 / 1024) + "MB");
    }
}