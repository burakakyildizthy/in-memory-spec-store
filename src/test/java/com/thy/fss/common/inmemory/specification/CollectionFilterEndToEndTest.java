package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity_;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end validation tests for collection filtering workflow.
 * Tests task 7.1: Test complete collection filtering workflow
 * <p>
 * Requirements covered:
 * - 5.1: Consistent operator behavior between filters and specifications
 * - 5.2: Switching between filter and specification APIs
 * - 5.3: Collection operations use same underlying logic
 * - 5.4: Operator support consistency
 * - 5.5: Basic collection support functionality
 */
@DisplayName("Collection Filter End-to-End Validation Tests")
class CollectionFilterEndToEndTest extends BaseIntegrationTest {

    private static final String WORK = "work";
    private static final String PROJECT = "project";
    private static final String PERSONAL = "personal";
    private static final String TEST = "test";
    private static final String TAG_PREFIX = "tags";
    
    private SpecificationQueryEngine<CollectionEntity> queryEngine;
    private CollectionEntitySpecificationService specificationService;
    private List<CollectionEntity> testEntities;

    @Override
    @BeforeEach
    public void setUp() {
        // Initialize query engine and specification service
        queryEngine = new SpecificationQueryEngine<>(CollectionEntity.class);
        specificationService = new CollectionEntitySpecificationService();

        // Create comprehensive test data
        testEntities = createComprehensiveTestData();
    }

    /**
     * Creates comprehensive test entities with various collection types and scenarios.
     */
    private List<CollectionEntity> createComprehensiveTestData() {
        List<CollectionEntity> entities = new ArrayList<>();

        // Entity 1: List with multiple string elements
        CollectionEntity entity1 = new CollectionEntity();
        entity1.setId(1L);
        entity1.setTags(Arrays.asList(WORK, "important", "urgent", PROJECT));
        entity1.setNumbers(Set.of(1, 5, 10, 15));
        entities.add(entity1);

        // Entity 2: Set with different elements
        CollectionEntity entity2 = new CollectionEntity();
        entity2.setId(2L);
        entity2.setTags(Arrays.asList(PERSONAL, "hobby", "fun", "weekend"));
        entity2.setNumbers(Set.of(2, 4, 6, 8));
        entities.add(entity2);

        // Entity 3: Mixed collections with overlapping values
        CollectionEntity entity3 = new CollectionEntity();
        entity3.setId(3L);
        entity3.setTags(Arrays.asList(WORK, PROJECT, "deadline", "critical"));
        entity3.setNumbers(Set.of(1, 3, 7, 9));
        entities.add(entity3);

        // Entity 4: Empty collections
        CollectionEntity entity4 = new CollectionEntity();
        entity4.setId(4L);
        entity4.setTags(new ArrayList<>());
        entity4.setNumbers(new HashSet<>());
        entities.add(entity4);

        // Entity 5: Null collections
        CollectionEntity entity5 = new CollectionEntity();
        entity5.setId(5L);
        entity5.setTags(null);
        entity5.setNumbers(null);
        entities.add(entity5);

        // Entity 6: Single element collections
        CollectionEntity entity6 = new CollectionEntity();
        entity6.setId(6L);
        entity6.setTags(List.of("single"));
        entity6.setNumbers(Set.of(42));
        entities.add(entity6);

        // Entity 7: Large collections for performance testing
        CollectionEntity entity7 = new CollectionEntity();
        entity7.setId(7L);
        List<String> largeTags = new ArrayList<>();
        Set<Integer> largeNumbers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            largeTags.add("tag" + i);
            largeNumbers.add(i);
        }
        entity7.setTags(largeTags);
        entity7.setNumbers(largeNumbers);
        entities.add(entity7);

        return entities;
    }

    // ========== Task 7.1: Complete Collection Filtering Workflow Tests ==========

    @Test
    @DisplayName("End-to-End: List collection filtering with annotation processor generated code")
    void shouldTestCompleteListCollectionWorkflow() {
        // Given: Verify annotation processor generated the required classes
        assertNotNull(CollectionEntity_.tags, "Meta model should be generated for tags field");
        assertNotNull(CollectionEntity_.numbers, "Meta model should be generated for numbers field");

        // Verify the meta attributes have correct types
        assertEquals(TAG_PREFIX, CollectionEntity_.tags.getName());
        assertEquals(CollectionEntity.class, CollectionEntity_.tags.getOwnerType());
        assertEquals(String.class, CollectionEntity_.tags.getElementType());

        // Create filter using generated filter class
        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tagsContains(WORK)
                .numbersContains(1)
                .build();

        // When: Execute query using SpecificationQueryEngine
        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));

        // Then: Validate results match expected collection filtering behavior
        assertEquals(2, result.getTotalElements(), "Should find entities 1 and 3 with 'work' tag and number 1");
        List<Long> resultIds = result.getContent().stream()
                .map(CollectionEntity::getIdentity)
                .sorted()
                .toList();
        assertEquals(Arrays.asList(1L, 3L), resultIds);

        // Verify individual entity validation using specification service
        CollectionEntity entity1 = testEntities.get(0); // ID 1
        assertTrue(specificationService.validateFilter(entity1, filter),
                "Entity 1 should pass filter validation");

        CollectionEntity entity2 = testEntities.get(1); // ID 2
        assertFalse(specificationService.validateFilter(entity2, filter),
                "Entity 2 should fail filter validation");
    }

    @Test
    @DisplayName("End-to-End: Set collection filtering with various element types")
    void shouldTestCompleteSetCollectionWorkflow() {
        // Given: Create filter for Set<Integer> numbers field
        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .numbersContains(42)
                .build();

        // When: Execute query
        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));

        // Then: Should find entity 6 (single element collection with 42)
        assertTrue(result.getTotalElements() >= 1, "Should find at least one entity containing 42");
        assertTrue(result.getContent().stream().anyMatch(e -> e.getIdentity().equals(6L)),
                "Should find entity 6 which contains 42");

        // Verify using specification service
        CollectionEntity entity6 = testEntities.stream()
                .filter(e -> e.getIdentity().equals(6L))
                .findFirst()
                .orElseThrow();
        assertTrue(specificationService.validateFilter(entity6, filter));
    }

    @Test
    @DisplayName("End-to-End: Collection interface filtering with generic Collection type")
    void shouldTestGenericCollectionInterfaceWorkflow() {
        // Given: Test that both List and Set implementations work with Collection interface
        // Create entities with different Collection implementations
        List<CollectionEntity> mixedCollectionEntities = new ArrayList<>();

        // Entity with ArrayList
        CollectionEntity listEntity = new CollectionEntity();
        listEntity.setId(100L);
        listEntity.setTags(new ArrayList<>(Arrays.asList(TEST, "list")));
        listEntity.setNumbers(new HashSet<>(Set.of(100, 200)));
        mixedCollectionEntities.add(listEntity);

        // Entity with LinkedList
        CollectionEntity linkedListEntity = new CollectionEntity();
        linkedListEntity.setId(101L);
        linkedListEntity.setTags(new LinkedList<>(Arrays.asList(TEST, "linked")));
        linkedListEntity.setNumbers(new TreeSet<>(Set.of(101, 201)));
        mixedCollectionEntities.add(linkedListEntity);

        // Create filter that should work with any Collection implementation
        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tagsContains(TEST)
                .build();

        // When: Execute query on mixed collection types
        Page<CollectionEntity> result = queryEngine.queryByFilter(mixedCollectionEntities, filter, PageRequest.of(0, 10));

        // Then: Should find both entities regardless of Collection implementation
        assertEquals(2, result.getTotalElements());
        List<Long> resultIds = result.getContent().stream()
                .map(CollectionEntity::getIdentity)
                .sorted()
                .toList();
        assertEquals(Arrays.asList(100L, 101L), resultIds);
    }

    @Test
    @DisplayName("End-to-End: Complex collection operations with nested filter criteria")
    void shouldTestComplexCollectionOperationsWorkflow() {
        // Given: Create entities for complex nested operations
        List<CollectionEntity> complexEntities = new ArrayList<>();

        // Entity with tags that start with WORK
        CollectionEntity entity1 = new CollectionEntity();
        entity1.setId(200L);
        entity1.setTags(Arrays.asList("work-project", "work-task", PERSONAL));
        entity1.setNumbers(Set.of(1, 2, 3));
        complexEntities.add(entity1);

        // Entity with tags that don't start with WORK
        CollectionEntity entity2 = new CollectionEntity();
        entity2.setId(201L);
        entity2.setTags(Arrays.asList(PERSONAL, "hobby", "fun"));
        entity2.setNumbers(Set.of(4, 5, 6));
        complexEntities.add(entity2);

        // Create nested filter: any tag starts with WORK
        StringFilter workFilter = new StringFilter().setStartsWith(WORK);
        CollectionFilter<String> tagsFilter = new CollectionFilter<String>()
                .setCollectionAny(workFilter);

        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tags(tagsFilter)
                .build();

        // When: Execute query with nested filter criteria
        Page<CollectionEntity> result = queryEngine.queryByFilter(complexEntities, filter, PageRequest.of(0, 10));

        // Then: Should find only entity 200 (has tags starting with WORK)
        assertEquals(1, result.getTotalElements());
        assertEquals(200L, result.getContent().get(0).getIdentity());

        // Verify using specification service
        assertTrue(specificationService.validateFilter(entity1, filter));
        assertFalse(specificationService.validateFilter(entity2, filter));
    }

    @Test
    @DisplayName("End-to-End: Performance validation with large collections")
    void shouldTestPerformanceWithLargeCollections() {
        // Given: Entity with large collection (entity 7 from test data)
        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tagsContains("tag50")
                .numbersContains(75)
                .build();

        // When: Execute query and measure performance
        long startTime = System.nanoTime();
        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to ms

        // Then: Should efficiently handle large collections
        assertEquals(1, result.getTotalElements(), "Should find entity 7 with large collections");
        assertEquals(7L, result.getContent().get(0).getIdentity());
        assertTrue(duration < 100, "Query should complete in under 100ms for large collections");

        // Verify multiple queries maintain performance
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 10));
        }
        endTime = System.nanoTime();
        long batchDuration = (endTime - startTime) / 1_000_000;
        assertTrue(batchDuration < 1000, "100 queries should complete in under 1 second");
    }

    @Test
    @DisplayName("End-to-End: Collection state operations (isEmpty/isNotEmpty)")
    void shouldTestCollectionStateOperationsWorkflow() {
        // Given: Filter for empty collections
        CollectionEntityFilter emptyFilter = CollectionEntityFilter.builder()
                .tagsIsEmpty(true)
                .numbersIsEmpty(true)
                .build();

        // When: Execute query for empty collections
        Page<CollectionEntity> emptyResult = queryEngine.queryByFilter(testEntities, emptyFilter, PageRequest.of(0, 10));

        // Then: Should find only entity 4 (empty collections)
        assertEquals(1, emptyResult.getTotalElements());
        assertEquals(4L, emptyResult.getContent().get(0).getIdentity());

        // Given: Filter for non-empty collections
        CollectionEntityFilter nonEmptyFilter = CollectionEntityFilter.builder()
                .tagsIsNotEmpty(true)
                .numbersIsNotEmpty(true)
                .build();

        // When: Execute query for non-empty collections
        Page<CollectionEntity> nonEmptyResult = queryEngine.queryByFilter(testEntities, nonEmptyFilter, PageRequest.of(0, 10));

        // Then: Should find entities 1, 2, 3, 6, 7 (all with non-empty collections)
        assertEquals(5, nonEmptyResult.getTotalElements());
        List<Long> resultIds = nonEmptyResult.getContent().stream()
                .map(CollectionEntity::getIdentity)
                .sorted()
                .toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 6L, 7L), resultIds);
    }

    @Test
    @DisplayName("End-to-End: Multiple collection operations on same entity")
    void shouldTestMultipleCollectionOperationsWorkflow() {
        // Given: Complex filter with multiple collection operations
        CollectionEntityFilter complexFilter = CollectionEntityFilter.builder()
                .tagsIsNotEmpty(true)
                .tagsContains(WORK)
                .numbersIsNotEmpty(true)
                .numbersContains(1)
                .build();

        // When: Execute query with multiple operations
        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, complexFilter, PageRequest.of(0, 10));

        // Then: Should find entities that satisfy ALL conditions
        assertEquals(2, result.getTotalElements());
        List<Long> resultIds = result.getContent().stream()
                .map(CollectionEntity::getIdentity)
                .sorted()
                .toList();
        assertEquals(Arrays.asList(1L, 3L), resultIds);

        // Verify each result entity individually
        for (CollectionEntity entity : result.getContent()) {
            assertTrue(specificationService.validateFilter(entity, complexFilter));
            assertNotNull(entity.getTags());
            assertFalse(entity.getTags().isEmpty());
            assertTrue(entity.getTags().contains(WORK));
            assertNotNull(entity.getNumbers());
            assertFalse(entity.getNumbers().isEmpty());
            assertTrue(entity.getNumbers().contains(1));
        }
    }

    @Test
    @DisplayName("End-to-End: Pagination with collection filters")
    void shouldTestPaginationWithCollectionFilters() {
        // Given: Filter that matches multiple entities
        CollectionEntityFilter filter = CollectionEntityFilter.builder()
                .tagsIsNotEmpty(true)
                .build();

        // When: Execute paginated queries
        Page<CollectionEntity> page1 = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(0, 2));
        Page<CollectionEntity> page2 = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(1, 2));
        Page<CollectionEntity> page3 = queryEngine.queryByFilter(testEntities, filter, PageRequest.of(2, 2));

        // Then: Verify pagination works correctly
        assertEquals(5, page1.getTotalElements(), "Total elements should be 5");
        assertEquals(3, page1.getTotalPages(), "Should have 3 pages with size 2");
        assertEquals(2, page1.getContent().size(), "Page 1 should have 2 elements");
        assertEquals(2, page2.getContent().size(), "Page 2 should have 2 elements");
        assertEquals(1, page3.getContent().size(), "Page 3 should have 1 element");

        // Verify no duplicates across pages
        Set<Long> allIds = new HashSet<>();
        page1.getContent().forEach(e -> allIds.add(e.getIdentity()));
        page2.getContent().forEach(e -> allIds.add(e.getIdentity()));
        page3.getContent().forEach(e -> allIds.add(e.getIdentity()));
        assertEquals(5, allIds.size(), "Should have 5 unique entities across all pages");
    }

    @Test
    @DisplayName("End-to-End: Count operations with collection filters")
    void shouldTestCountOperationsWithCollectionFilters() {
        // Given: Various filters for counting
        CollectionEntityFilter allFilter = new CollectionEntityFilter(); // No filters = all entities
        CollectionEntityFilter emptyFilter = CollectionEntityFilter.builder()
                .tagsIsEmpty(true)
                .build();
        CollectionEntityFilter workFilter = CollectionEntityFilter.builder()
                .tagsContains(WORK)
                .build();

        // When: Execute count operations
        long totalCount = queryEngine.countByFilter(testEntities, allFilter);
        long emptyCount = queryEngine.countByFilter(testEntities, emptyFilter);
        long workCount = queryEngine.countByFilter(testEntities, workFilter);

        // Then: Verify counts are correct
        assertEquals(7, totalCount, "Should count all 7 entities");
        assertEquals(1, emptyCount, "Should count 1 entity with empty collections");
        assertEquals(2, workCount, "Should count 2 entities with 'work' tag");

        // Verify counts match query results
        Page<CollectionEntity> workResults = queryEngine.queryByFilter(testEntities, workFilter, PageRequest.of(0, 10));
        assertEquals(workCount, workResults.getTotalElements(), "Count should match query results");
    }

    @Test
    @DisplayName("End-to-End: Integration with meta model attributes")
    void shouldTestMetaModelAttributeIntegration() {
        // Given: Verify meta model attributes are properly generated and accessible
        assertNotNull(CollectionEntity_.id, "ID attribute should be generated");
        assertNotNull(CollectionEntity_.tags, "Tags collection attribute should be generated");
        assertNotNull(CollectionEntity_.numbers, "Numbers collection attribute should be generated");

        // Verify attribute properties
        assertEquals("id", CollectionEntity_.id.getName());
        assertEquals(TAG_PREFIX, CollectionEntity_.tags.getName());
        assertEquals("numbers", CollectionEntity_.numbers.getName());
        assertEquals(CollectionEntity.class, CollectionEntity_.tags.getOwnerType());
        assertEquals(String.class, CollectionEntity_.tags.getElementType());
        assertEquals(Integer.class, CollectionEntity_.numbers.getElementType());

        // Test that meta model can be used for programmatic filter creation
        CollectionEntityFilter programmaticFilter = new CollectionEntityFilter();

        // Use meta model information to set up filters
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionContains("important");
        programmaticFilter.setTags(tagsFilter);

        // When: Execute query using programmatically created filter
        Page<CollectionEntity> result = queryEngine.queryByFilter(testEntities, programmaticFilter, PageRequest.of(0, 10));

        // Then: Should work correctly with meta model integration
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getIdentity());
    }
}