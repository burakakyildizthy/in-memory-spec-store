package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for CollectionFilter operations.
 * Tests all collection-specific filtering operations including contains, size, empty checks,
 * and complex filter combinations with large datasets.
 * Requirements: 4.3, 15.9
 */
class CollectionFilterTest {

    private static final String TEST_STRING = "test";
    private static final String ELEMENT_STRING = "element";
    private static final String SPECIAL_TAG = "SpecialTag";
    private static final String COMMON_TAG = "CommonTag";
    private static final String PREFIX_STRING = "prefix";
    private static final String SUFFIX_STRING = "suffix";
    private static final String COLLECTION_FILTER_STRING = "CollectionFilter";
    private static final String TAG = "Tag";
    

    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        CollectionFilter<String> filter = new CollectionFilter<>();

        assertThat(filter.getEquals()).isNull();
        assertThat(filter.getNotEquals()).isNull();
        assertThat(filter.getIsNull()).isNull();
        assertThat(filter.getIsNotNull()).isNull();
        assertThat(filter.getIn()).isNull();
        assertThat(filter.getNotIn()).isNull();
        assertThat(filter.getCollectionContains()).isNull();
        assertThat(filter.getCollectionAny()).isNull();
        assertThat(filter.getCollectionAll()).isNull();
        assertThat(filter.getCollectionNone()).isNull();
        assertThat(filter.getIsEmpty()).isNull();
        assertThat(filter.getIsNotEmpty()).isNull();
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        StringFilter anyFilter = new StringFilter().setContains(TEST_STRING);
        StringFilter allFilter = new StringFilter().setStartsWith(PREFIX_STRING);
        StringFilter noneFilter = new StringFilter().setEndsWith(SUFFIX_STRING);

        CollectionFilter<String> original = new CollectionFilter<>();
        original.setCollectionContains("element1");
        original.setCollectionAny(anyFilter);
        original.setCollectionAll(allFilter);
        original.setCollectionNone(noneFilter);
        original.setIsEmpty(true);
        original.setIsNotEmpty(false);
        original.setEquals(Arrays.asList("a", "b"));
        original.setNotEquals(Arrays.asList("c", "d"));
        original.setIsNull(false);
        original.setIsNotNull(true);

        CollectionFilter<String> copy = new CollectionFilter<>(original);

        assertThat(copy.getCollectionContains()).isEqualTo("element1");
        assertThat(copy.getCollectionAny()).isEqualTo(anyFilter);
        assertThat(copy.getCollectionAll()).isEqualTo(allFilter);
        assertThat(copy.getCollectionNone()).isEqualTo(noneFilter);
        assertThat(copy.getIsEmpty()).isTrue();
        assertThat(copy.getIsNotEmpty()).isFalse();
        assertThat(copy.getEquals()).containsExactly("a", "b");
        assertThat(copy.getNotEquals()).containsExactly("c", "d");
        assertThat(copy.getIsNull()).isFalse();
        assertThat(copy.getIsNotNull()).isTrue();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        CollectionFilter<String> original = new CollectionFilter<>();
        original.setCollectionContains("original");

        CollectionFilter<String> copy = new CollectionFilter<>(original);
        copy.setCollectionContains("modified");

        assertThat(original.getCollectionContains()).isEqualTo("original");
        assertThat(copy.getCollectionContains()).isEqualTo("modified");
    }

    // ==================== setCollectionContains Tests ====================

    @Test
    void testSetCollectionContainsSetsAndGetsValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains("testElement");

        assertThat(filter.getCollectionContains()).isEqualTo("testElement");
    }

    @Test
    void testSetCollectionContainsWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(null);

        assertThat(filter.getCollectionContains()).isNull();
    }

    @Test
    void testSetCollectionContainsReturnsFilterForChaining() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setCollectionContains(ELEMENT_STRING);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetCollectionContainsWithLargeDataset() {
        List<CollectionEntity> entities = generator.generateCollectionEntities(10_000);
        
        // Add specific tag to some entities
        for (int i = 0; i < 1000; i++) {
            if (entities.get(i).getTags() != null) {
                entities.get(i).getTags().add(SPECIAL_TAG);
            }
        }

        // Create filter with collectionContains
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(SPECIAL_TAG);

        // Verify filter is set correctly
        assertThat(filter.getCollectionContains()).isEqualTo(SPECIAL_TAG);
        
        // Manual filtering to verify behavior
        long matchCount = entities.stream()
            .filter(e -> e.getTags() != null && e.getTags().contains(SPECIAL_TAG))
            .count();
        
        assertThat(matchCount).isGreaterThan(0);
    }

    // ==================== setCollectionAny Tests ====================

    @Test
    void testSetCollectionAnySetsAndGetsValue() {
        StringFilter anyFilter = new StringFilter().setContains(TEST_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionAny(anyFilter);

        assertThat(filter.getCollectionAny()).isEqualTo(anyFilter);
    }

    @Test
    void testSetCollectionAnyWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionAny(null);

        assertThat(filter.getCollectionAny()).isNull();
    }

    @Test
    void testSetCollectionAnyReturnsFilterForChaining() {
        StringFilter anyFilter = new StringFilter().setContains(TEST_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setCollectionAny(anyFilter);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setCollectionAll Tests ====================

    @Test
    void testSetCollectionAllSetsAndGetsValue() {
        StringFilter allFilter = new StringFilter().setStartsWith(PREFIX_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionAll(allFilter);

        assertThat(filter.getCollectionAll()).isEqualTo(allFilter);
    }

    @Test
    void testSetCollectionAllWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionAll(null);

        assertThat(filter.getCollectionAll()).isNull();
    }

    @Test
    void testSetCollectionAllReturnsFilterForChaining() {
        StringFilter allFilter = new StringFilter().setStartsWith(PREFIX_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setCollectionAll(allFilter);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setCollectionNone Tests ====================

    @Test
    void testSetCollectionNoneSetsAndGetsValue() {
        StringFilter noneFilter = new StringFilter().setEndsWith(SUFFIX_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionNone(noneFilter);

        assertThat(filter.getCollectionNone()).isEqualTo(noneFilter);
    }

    @Test
    void testSetCollectionNoneWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionNone(null);

        assertThat(filter.getCollectionNone()).isNull();
    }

    @Test
    void testSetCollectionNoneReturnsFilterForChaining() {
        StringFilter noneFilter = new StringFilter().setEndsWith(SUFFIX_STRING);
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setCollectionNone(noneFilter);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsEmpty Tests ====================

    @Test
    void testSetIsEmptySetsAndGetsValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsEmpty(true);

        assertThat(filter.getIsEmpty()).isTrue();
    }

    @Test
    void testSetIsEmptyWithFalseValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsEmpty(false);

        assertThat(filter.getIsEmpty()).isFalse();
    }

    @Test
    void testSetIsEmptyWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsEmpty(null);

        assertThat(filter.getIsEmpty()).isNull();
    }

    @Test
    void testSetIsEmptyReturnsFilterForChaining() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setIsEmpty(true);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetIsEmptyWithLargeDataset() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with controlled collection states
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            if (i < 2000) {
                entity.setTags(new ArrayList<>()); // Empty
            } else {
                List<String> tags = new ArrayList<>();
                tags.add(TAG + i);
                entity.setTags(tags); // Non-empty
            }
            
            entities.add(entity);
        }

        // Create filter with isEmpty
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long emptyCount = entities.stream()
            .filter(e -> e.getTags() != null && e.getTags().isEmpty())
            .count();
        
        assertThat(emptyCount).isEqualTo(2000);
    }

    // ==================== setIsNotEmpty Tests ====================

    @Test
    void testSetIsNotEmptySetsAndGetsValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);

        assertThat(filter.getIsNotEmpty()).isTrue();
    }

    @Test
    void testSetIsNotEmptyWithFalseValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(false);

        assertThat(filter.getIsNotEmpty()).isFalse();
    }

    @Test
    void testSetIsNotEmptyWithNullValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(null);

        assertThat(filter.getIsNotEmpty()).isNull();
    }

    @Test
    void testSetIsNotEmptyReturnsFilterForChaining() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setIsNotEmpty(true);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetIsNotEmptyWithLargeDataset() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with controlled collection states
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            if (i < 2000) {
                entity.setTags(new ArrayList<>()); // Empty
            } else {
                List<String> tags = new ArrayList<>();
                tags.add(TAG + i);
                entity.setTags(tags); // Non-empty
            }
            
            entities.add(entity);
        }

        // Create filter with isNotEmpty
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNotEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long nonEmptyCount = entities.stream()
            .filter(e -> e.getTags() != null && !e.getTags().isEmpty())
            .count();
        
        assertThat(nonEmptyCount).isEqualTo(8000);
    }

    // ==================== Inherited Filter Methods Tests ====================

    @Test
    void testSetEqualsSetsAndGetsValue() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setEquals(collection);

        assertThat(filter.getEquals()).isEqualTo(collection);
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setEquals(collection);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotEqualsSetsAndGetsValue() {
        Collection<String> collection = Arrays.asList("x", "y", "z");
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setNotEquals(collection);

        assertThat(filter.getNotEquals()).isEqualTo(collection);
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        Collection<String> collection = Arrays.asList("x", "y", "z");
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setNotEquals(collection);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetIsNullSetsAndGetsValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testSetIsNullReturnsFilterForChaining() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setIsNull(true);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetIsNotNullSetsAndGetsValue() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    @Test
    void testSetIsNotNullReturnsFilterForChaining() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setIsNotNull(true);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetInSetsAndGetsValue() {
        Collection<Collection<String>> collections = Arrays.asList(
            Arrays.asList("a", "b"),
            Arrays.asList("c", "d")
        );
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIn(collections);

        assertThat(filter.getIn()).isEqualTo(collections);
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        Collection<Collection<String>> collections = Arrays.asList(
            Arrays.asList("a", "b"),
            Arrays.asList("c", "d")
        );
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setIn(collections);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotInSetsAndGetsValue() {
        Collection<Collection<String>> collections = Arrays.asList(
            Arrays.asList("x", "y"),
            Arrays.asList("z", "w")
        );
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setNotIn(collections);

        assertThat(filter.getNotIn()).isEqualTo(collections);
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        Collection<Collection<String>> collections = Arrays.asList(
            Arrays.asList("x", "y"),
            Arrays.asList("z", "w")
        );
        CollectionFilter<String> filter = new CollectionFilter<>();
        CollectionFilter<String> result = filter.setNotIn(collections);

        assertThat(result).isSameAs(filter);
    }

    // ==================== Null Collection Tests ====================

    @Test
    void testWithNullCollectionsIsNullFilter() {
        List<CollectionEntity> entities = generator.generateCollectionEntities(10_000);
        
        // Set some entities to have null tags
        for (int i = 0; i < 1500; i++) {
            entities.get(i).setTags(null);
        }

        // Create filter with isNull
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNull(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNull()).isTrue();
        
        // Manual filtering to verify behavior
        long nullCount = entities.stream()
            .filter(e -> e.getTags() == null)
            .count();
        
        assertThat(nullCount).isEqualTo(1500);
    }

    @Test
    void testWithNullCollectionsIsNotNullFilter() {
        List<CollectionEntity> entities = generator.generateCollectionEntities(10_000);
        
        // Set some entities to have null tags
        for (int i = 0; i < 1500; i++) {
            entities.get(i).setTags(null);
        }

        // Create filter with isNotNull
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotNull(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNotNull()).isTrue();
        
        // Manual filtering to verify behavior
        long notNullCount = entities.stream()
            .filter(e -> e.getTags() != null)
            .count();
        
        assertThat(notNullCount).isEqualTo(8500);
    }

    // ==================== Empty Collection Tests ====================

    @Test
    void testWithEmptyCollectionsEmptyFilter() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with various collection states
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            if (i < 2000) {
                entity.setTags(new ArrayList<>()); // Empty
            } else if (i < 4000) {
                entity.setTags(null); // Null
            } else {
                List<String> tags = new ArrayList<>();
                tags.add(TAG + i);
                entity.setTags(tags); // Non-empty
            }
            
            entities.add(entity);
        }

        // Create filter with isEmpty
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long emptyCount = entities.stream()
            .filter(e -> e.getTags() != null && e.getTags().isEmpty())
            .count();
        
        assertThat(emptyCount).isEqualTo(2000);
    }

    // ==================== Large Collection Tests ====================

    @Test
    void testWithLargeCollectionsSizeOperations() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with varying collection sizes
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            List<String> tags = new ArrayList<>();
            int tagCount = i % 100; // 0 to 99 tags
            for (int j = 0; j < tagCount; j++) {
                tags.add(TAG + j);
            }
            entity.setTags(tags);
            
            entities.add(entity);
        }

        // Create filter for testing
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNotEmpty()).isTrue();
        
        // Manual filtering to verify behavior - collections with size > 50
        long largeCollectionCount = entities.stream()
            .filter(e -> e.getTags() != null && e.getTags().size() > 50)
            .count();
        
        assertThat(largeCollectionCount).isGreaterThan(0);
    }

    @Test
    void testWithLargeCollectionsContainsOperations() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with large collections
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                tags.add(TAG + j);
            }
            
            // Add special tag to some entities
            if (i % 10 == 0) {
                tags.add(SPECIAL_TAG);
            }
            
            entity.setTags(tags);
            entities.add(entity);
        }

        // Create filter with collectionContains
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(SPECIAL_TAG);

        // Verify filter is set correctly
        assertThat(filter.getCollectionContains()).isEqualTo(SPECIAL_TAG);
        
        // Manual filtering to verify behavior
        long containsCount = entities.stream()
            .filter(e -> e.getTags() != null && e.getTags().contains(SPECIAL_TAG))
            .count();
        
        assertThat(containsCount).isEqualTo(1000);
    }

    // ==================== Complex Filter Combinations ====================

    @Test
    void testComplexFilterCombinationMultipleConditions() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create diverse dataset
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            if (i < 2000) {
                entity.setTags(new ArrayList<>()); // Empty
            } else if (i < 4000) {
                entity.setTags(null); // Null
            } else {
                List<String> tags = new ArrayList<>();
                tags.add(TAG + (i % 10));
                if (i % 5 == 0) {
                    tags.add(COMMON_TAG);
                }
                entity.setTags(tags);
            }
            
            entities.add(entity);
        }

        // Create filter with multiple conditions
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);
        filter.setCollectionContains(COMMON_TAG);

        // Verify filter is set correctly
        assertThat(filter.getIsNotEmpty()).isTrue();
        assertThat(filter.getCollectionContains()).isEqualTo(COMMON_TAG);
        
        // Manual filtering to verify behavior
        long matchCount = entities.stream()
            .filter(e -> e.getTags() != null && 
                        !e.getTags().isEmpty() && 
                        e.getTags().contains(COMMON_TAG))
            .count();
        
        assertThat(matchCount).isGreaterThan(0);
    }

    @Test
    void testComplexFilterCombinationWithNumbers() {
        List<CollectionEntity> entities = new ArrayList<>();
        
        // Create entities with number collections
        for (int i = 0; i < 10_000; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            Set<Integer> numbers = new HashSet<>();
            for (int j = 0; j < (i % 20); j++) {
                numbers.add(j * 10);
            }
            entity.setNumbers(numbers);
            
            entities.add(entity);
        }

        // Create filter for number collections
        CollectionFilter<Integer> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNotEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long nonEmptyCount = entities.stream()
            .filter(e -> e.getNumbers() != null && !e.getNumbers().isEmpty())
            .count();
        
        assertThat(nonEmptyCount).isGreaterThan(0);
    }

    // ==================== equals() and hashCode() Tests ====================

    @Test
    void testEqualsSameInstance() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(TEST_STRING);

        assertThat(filter).isEqualTo(filter);
    }

    @Test
    void testEqualsEqualFilters() {
        StringFilter anyFilter = new StringFilter().setContains(TEST_STRING);
        
        CollectionFilter<String> filter1 = new CollectionFilter<>();
        filter1.setCollectionContains(ELEMENT_STRING);
        filter1.setCollectionAny(anyFilter);
        filter1.setIsEmpty(true);

        CollectionFilter<String> filter2 = new CollectionFilter<>();
        filter2.setCollectionContains(ELEMENT_STRING);
        filter2.setCollectionAny(anyFilter);
        filter2.setIsEmpty(true);

        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2.hashCode());
    }

    @Test
    void testEqualsDifferentFilters() {
        CollectionFilter<String> filter1 = new CollectionFilter<>();
        filter1.setCollectionContains("element1");

        CollectionFilter<String> filter2 = new CollectionFilter<>();
        filter2.setCollectionContains("element2");

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithNull() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(TEST_STRING);

        assertThat(filter).isNotEqualTo(null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(TEST_STRING);

        assertThat(filter).isNotEqualTo("not a filter");
    }

    @Test
    void testHashCodeConsistentWithEquals() {
        CollectionFilter<String> filter1 = new CollectionFilter<>();
        filter1.setCollectionContains(ELEMENT_STRING);
        filter1.setIsEmpty(true);

        CollectionFilter<String> filter2 = new CollectionFilter<>();
        filter2.setCollectionContains(ELEMENT_STRING);
        filter2.setIsEmpty(true);

        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2.hashCode());
    }

    // ==================== toString() Tests ====================

    @Test
    void testToStringContainsAllFields() {
        StringFilter anyFilter = new StringFilter().setContains(TEST_STRING);
        
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains(ELEMENT_STRING);
        filter.setCollectionAny(anyFilter);
        filter.setIsEmpty(true);
        filter.setIsNotEmpty(false);

        String result = filter.toString();

        assertThat(result).contains(COLLECTION_FILTER_STRING)
                .contains("collectionContains=element")
                .contains("isEmpty=true")
                .contains("isNotEmpty=false");
    }

    @Test
    void testToStringWithNullFields() {
        CollectionFilter<String> filter = new CollectionFilter<>();

        String result = filter.toString();

        assertThat(result).contains(COLLECTION_FILTER_STRING).contains("collectionContains=null").contains("isEmpty=null");
    }

    // ==================== Integration Tests with Order Items ====================

    @Test
    void testWithOrderItemsLargeDataset() {
        List<Order> orders = new ArrayList<>();
        
        // Create orders with varying item counts
        for (int i = 0; i < 10_000; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCustomerId((long) (i % 1000));
            
            List<OrderItem> items = new ArrayList<>();
            int itemCount = i % 10; // 0 to 9 items
            for (int j = 0; j < itemCount; j++) {
                OrderItem item = new OrderItem();
                item.setId((long) (i * 10 + j));
                item.setProductId((long) j);
                item.setQuantity(j + 1);
                item.setUnitPrice(10.0 + j);
                items.add(item);
            }
            order.setItems(items);
            
            orders.add(order);
        }

        // Create filter for order items
        CollectionFilter<OrderItem> filter = new CollectionFilter<>();
        filter.setIsNotEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsNotEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long nonEmptyCount = orders.stream()
            .filter(o -> o.getItems() != null && !o.getItems().isEmpty())
            .count();
        
        assertThat(nonEmptyCount).isGreaterThan(0);
    }

    @Test
    void testWithOrderItemsEmptyItemsList() {
        List<Order> orders = new ArrayList<>();
        
        // Create orders with empty items
        for (int i = 0; i < 10_000; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCustomerId((long) (i % 1000));
            
            if (i < 3000) {
                order.setItems(new ArrayList<>()); // Empty
            } else {
                List<OrderItem> items = new ArrayList<>();
                OrderItem item = new OrderItem();
                item.setId((long) i);
                item.setQuantity(1);
                items.add(item);
                order.setItems(items);
            }
            
            orders.add(order);
        }

        // Create filter for empty order items
        CollectionFilter<OrderItem> filter = new CollectionFilter<>();
        filter.setIsEmpty(true);

        // Verify filter is set correctly
        assertThat(filter.getIsEmpty()).isTrue();
        
        // Manual filtering to verify behavior
        long emptyCount = orders.stream()
            .filter(o -> o.getItems() != null && o.getItems().isEmpty())
            .count();
        
        assertThat(emptyCount).isEqualTo(3000);
    }
}
