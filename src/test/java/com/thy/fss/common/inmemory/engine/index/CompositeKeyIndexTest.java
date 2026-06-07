package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CompositeKeyIndex.
 * Tests index building, lookup operations, and edge cases.
 */
class CompositeKeyIndexTest {
    
    /**
     * Test index building with 2-field composite key
     */
    @Test
    void shouldBuildIndexWithTwoFieldCompositeKey() {
        // Given: A 2-field composite key (id, code)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Create test entities
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "B", 2, "EU", 200L));
        entities.add(createTestTarget(2L, "A", 3, "US", 300L));
        entities.add(createTestTarget(2L, "B", 4, "EU", 400L));
        
        // When: Building the index
        index.buildIndex(entities);
        
        // Then: Index should be built successfully
        assertThat(index.getKeyFieldCount()).isEqualTo(2);
        assertThat(index.getKeyPaths()).hasSize(2);
    }
    
    /**
     * Test index building with 3-field composite key
     */
    @Test
    void shouldBuildIndexWithThreeFieldCompositeKey() {
        // Given: A 3-field composite key (id, code, version)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code),
            List.of(TestTarget_.version)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Create test entities
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "A", 2, "EU", 200L));
        entities.add(createTestTarget(1L, "B", 1, "US", 300L));
        
        // When: Building the index
        index.buildIndex(entities);
        
        // Then: Index should be built successfully
        assertThat(index.getKeyFieldCount()).isEqualTo(3);
    }
    
    /**
     * Test lookup with exact matches
     */
    @Test
    void shouldLookupWithExactMatches() {
        // Given: An index with 2-field composite key
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        TestTarget entity1 = createTestTarget(1L, "A", 1, "US", 100L);
        TestTarget entity2 = createTestTarget(1L, "B", 2, "EU", 200L);
        TestTarget entity3 = createTestTarget(2L, "A", 3, "US", 300L);
        
        index.buildIndex(List.of(entity1, entity2, entity3));
        
        // When: Looking up with exact key match
        List<TestTarget> result1 = index.lookup(List.of(1L, "A"));
        List<TestTarget> result2 = index.lookup(List.of(1L, "B"));
        List<TestTarget> result3 = index.lookup(List.of(2L, "A"));
        
        // Then: Should return matching entities
        assertThat(result1).hasSize(1).contains(entity1);
        assertThat(result2).hasSize(1).contains(entity2);
        assertThat(result3).hasSize(1).contains(entity3);
    }
    
    /**
     * Test lookup with no matches
     */
    @Test
    void shouldReturnEmptyListWhenNoMatches() {
        // Given: An index with entities
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        TestTarget entity1 = createTestTarget(1L, "A", 1, "US", 100L);
        TestTarget entity2 = createTestTarget(2L, "B", 2, "EU", 200L);
        
        index.buildIndex(List.of(entity1, entity2));
        
        // When: Looking up with non-existent key
        List<TestTarget> result = index.lookup(List.of(3L, "C"));
        
        // Then: Should return empty list
        assertThat(result).isEmpty();
    }
    
    /**
     * Test lookup with partial matches (should return empty)
     */
    @Test
    void shouldReturnEmptyListForPartialMatches() {
        // Given: An index with 2-field composite key
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        TestTarget entity1 = createTestTarget(1L, "A", 1, "US", 100L);
        TestTarget entity2 = createTestTarget(1L, "B", 2, "EU", 200L);
        
        index.buildIndex(List.of(entity1, entity2));
        
        // When: Looking up with key that matches first field but not second
        List<TestTarget> result = index.lookup(List.of(1L, "C"));
        
        // Then: Should return empty list (partial match is not enough)
        assertThat(result).isEmpty();
    }
    
    /**
     * Test lookup with null key values
     */
    @Test
    void shouldHandleNullKeyValues() {
        // Given: An index with entities that have null key values
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        TestTarget entity1 = createTestTarget(1L, null, 1, "US", 100L);
        TestTarget entity2 = createTestTarget(null, "A", 2, "EU", 200L);
        TestTarget entity3 = createTestTarget(1L, "A", 3, "US", 300L);
        
        index.buildIndex(List.of(entity1, entity2, entity3));
        
        // When: Looking up with null key values (using ArrayList to allow nulls)
        List<Object> key1 = new ArrayList<>();
        key1.add(1L);
        key1.add(null);
        
        List<Object> key2 = new ArrayList<>();
        key2.add(null);
        key2.add("A");
        
        List<TestTarget> result1 = index.lookup(key1);
        List<TestTarget> result2 = index.lookup(key2);
        List<TestTarget> result3 = index.lookup(List.of(1L, "A"));
        
        // Then: Should match entities with null values
        assertThat(result1).hasSize(1).contains(entity1);
        assertThat(result2).hasSize(1).contains(entity2);
        assertThat(result3).hasSize(1).contains(entity3);
    }
    
    /**
     * Test index building with empty entity collection
     */
    @Test
    void shouldHandleEmptyEntityCollection() {
        // Given: An index with empty entity collection
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // When: Building index with empty collection
        index.buildIndex(List.of());
        
        // Then: Should not throw exception
        List<TestTarget> result = index.lookup(List.of(1L, "A"));
        assertThat(result).isEmpty();
    }
    
    /**
     * Test index building with null entity collection
     */
    @Test
    void shouldHandleNullEntityCollection() {
        // Given: An index
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // When: Building index with null collection
        index.buildIndex(null);
        
        // Then: Should not throw exception
        List<TestTarget> result = index.lookup(List.of(1L, "A"));
        assertThat(result).isEmpty();
    }
    
    /**
     * Test multiple entities with same composite key
     */
    @Test
    void shouldHandleMultipleEntitiesWithSameKey() {
        // Given: An index with multiple entities having the same composite key
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        TestTarget entity1 = createTestTarget(1L, "A", 1, "US", 100L);
        TestTarget entity2 = createTestTarget(1L, "A", 2, "EU", 200L);
        TestTarget entity3 = createTestTarget(1L, "A", 3, "ASIA", 300L);
        
        index.buildIndex(List.of(entity1, entity2, entity3));
        
        // When: Looking up with the shared key
        List<TestTarget> result = index.lookup(List.of(1L, "A"));
        
        // Then: Should return all matching entities
        assertThat(result).hasSize(3).containsExactlyInAnyOrder(entity1, entity2, entity3);
    }
    
    /**
     * Test constructor with null key paths
     */
    @Test
    void shouldThrowExceptionForNullKeyPaths() {
        // When/Then: Creating index with null key paths should throw exception
        assertThatThrownBy(() -> new CompositeKeyIndex<TestTarget>(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Key paths cannot be null or empty");
    }
    
    /**
     * Test constructor with empty key paths
     */
    @Test
    void shouldThrowExceptionForEmptyKeyPaths() {
        // Given: Null key paths
        List<List<MetaAttribute<?, ?>>> nullKeyPaths = null;

        // When/Then: Creating index with null key paths should throw exception
        assertThatThrownBy(() -> new CompositeKeyIndex<>(nullKeyPaths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key paths cannot be null or empty");
    }
    
    /**
     * Test lookup with wrong number of key values
     */
    @Test
    void shouldThrowExceptionForWrongNumberOfKeyValues() {
        // Given: An index with 2-field composite key
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(List.of(createTestTarget(1L, "A", 1, "US", 100L)));

        // When/Then: Looking up with wrong number of key values should throw exception
        List<Object> singleKeyValue = List.of(1L);
        assertThatThrownBy(() -> index.lookup(singleKeyValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected 2 key values, got 1");

        List<Object> threeKeyValues = List.of(1L, "A", "extra");
        assertThatThrownBy(() -> index.lookup(threeKeyValues))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected 2 key values, got 3");

    }
    
    /**
     * Test lookup with null key values list
     */
    @Test
    void shouldThrowExceptionForNullKeyValuesList() {
        // Given: An index
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // When/Then: Looking up with null key values list should throw exception
        assertThatThrownBy(() -> index.lookup(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Key values cannot be null");
    }
    
    /**
     * Helper method to create a TestTarget entity
     */
    private TestTarget createTestTarget(Long id, String code, Integer version, String region, Long timestamp) {
        TestTarget target = new TestTarget();
        target.setId(id);
        target.setCode(code);
        target.setVersion(version);
        target.setRegion(region);
        target.setTimestamp(timestamp);
        return target;
    }
    
    /**
     * Test deepClear() with single-level HashMap
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearSingleLevelHashMap() {
        // Given: An index with 1-field composite key (single level)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Build index with test entities
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(2L, "B", 2, "EU", 200L));
        entities.add(createTestTarget(3L, "C", 3, "ASIA", 300L));
        
        index.buildIndex(entities);
        
        // Verify index has data before clearing
        assertThat(index.lookup(List.of(1L))).hasSize(1);
        assertThat(index.lookup(List.of(2L))).hasSize(1);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All lookups should return empty lists
        assertThat(index.lookup(List.of(1L))).isEmpty();
        assertThat(index.lookup(List.of(2L))).isEmpty();
        assertThat(index.lookup(List.of(3L))).isEmpty();
    }
    
    /**
     * Test deepClear() with multi-level nested HashMap (2 levels)
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearTwoLevelNestedHashMap() {
        // Given: An index with 2-field composite key (2 levels)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Build index with test entities
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "B", 2, "EU", 200L));
        entities.add(createTestTarget(2L, "A", 3, "US", 300L));
        entities.add(createTestTarget(2L, "B", 4, "EU", 400L));
        
        index.buildIndex(entities);
        
        // Verify index has data before clearing
        assertThat(index.lookup(List.of(1L, "A"))).hasSize(1);
        assertThat(index.lookup(List.of(1L, "B"))).hasSize(1);
        assertThat(index.lookup(List.of(2L, "A"))).hasSize(1);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All lookups should return empty lists
        assertThat(index.lookup(List.of(1L, "A"))).isEmpty();
        assertThat(index.lookup(List.of(1L, "B"))).isEmpty();
        assertThat(index.lookup(List.of(2L, "A"))).isEmpty();
        assertThat(index.lookup(List.of(2L, "B"))).isEmpty();
    }
    
    /**
     * Test deepClear() with multi-level nested HashMap (3 levels)
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearThreeLevelNestedHashMap() {
        // Given: An index with 3-field composite key (3 levels)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code),
            List.of(TestTarget_.version)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Build index with test entities
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "A", 2, "EU", 200L));
        entities.add(createTestTarget(1L, "B", 1, "US", 300L));
        entities.add(createTestTarget(2L, "A", 1, "ASIA", 400L));
        
        index.buildIndex(entities);
        
        // Verify index has data before clearing
        assertThat(index.lookup(List.of(1L, "A", 1))).hasSize(1);
        assertThat(index.lookup(List.of(1L, "A", 2))).hasSize(1);
        assertThat(index.lookup(List.of(1L, "B", 1))).hasSize(1);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All lookups should return empty lists
        assertThat(index.lookup(List.of(1L, "A", 1))).isEmpty();
        assertThat(index.lookup(List.of(1L, "A", 2))).isEmpty();
        assertThat(index.lookup(List.of(1L, "B", 1))).isEmpty();
        assertThat(index.lookup(List.of(2L, "A", 1))).isEmpty();
    }
    
    /**
     * Test that entity lists are cleared at leaf level
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldClearEntityListsAtLeafLevel() {
        // Given: An index with multiple entities sharing the same key
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Build index with multiple entities having the same composite key
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "A", 2, "EU", 200L));
        entities.add(createTestTarget(1L, "A", 3, "ASIA", 300L));
        
        index.buildIndex(entities);
        
        // Verify index has multiple entities for the same key
        assertThat(index.lookup(List.of(1L, "A"))).hasSize(3);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: Entity list should be cleared
        assertThat(index.lookup(List.of(1L, "A"))).isEmpty();
    }
    
    /**
     * Test that intermediate HashMaps are cleared
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldClearIntermediateHashMaps() {
        // Given: An index with nested structure
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code),
            List.of(TestTarget_.version)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // Build index with entities creating multiple intermediate maps
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(1L, "A", 2, "EU", 200L));
        entities.add(createTestTarget(1L, "B", 1, "US", 300L));
        entities.add(createTestTarget(2L, "A", 1, "ASIA", 400L));
        entities.add(createTestTarget(2L, "B", 2, "EU", 500L));
        
        index.buildIndex(entities);
        
        // Verify index has data at all levels
        assertThat(index.lookup(List.of(1L, "A", 1))).hasSize(1);
        assertThat(index.lookup(List.of(2L, "B", 2))).hasSize(1);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All intermediate maps should be cleared
        // Verify by checking that lookups at all levels return empty
        assertThat(index.lookup(List.of(1L, "A", 1))).isEmpty();
        assertThat(index.lookup(List.of(1L, "A", 2))).isEmpty();
        assertThat(index.lookup(List.of(1L, "B", 1))).isEmpty();
        assertThat(index.lookup(List.of(2L, "A", 1))).isEmpty();
        assertThat(index.lookup(List.of(2L, "B", 2))).isEmpty();
    }
    
    /**
     * Test that deepClear() is idempotent (safe to call multiple times)
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldBeIdempotent() {
        // Given: An index with data
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        List<TestTarget> entities = new ArrayList<>();
        entities.add(createTestTarget(1L, "A", 1, "US", 100L));
        entities.add(createTestTarget(2L, "B", 2, "EU", 200L));
        
        index.buildIndex(entities);
        
        // When: Calling deepClear() multiple times
        index.deepClear();
        index.deepClear();
        index.deepClear();
        
        // Then: Should not throw exception and index should remain empty
        assertThat(index.lookup(List.of(1L, "A"))).isEmpty();
        assertThat(index.lookup(List.of(2L, "B"))).isEmpty();
    }
    
    /**
     * Test deepClear() on empty index
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldHandleDeepClearOnEmptyIndex() {
        // Given: An empty index
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        
        CompositeKeyIndex<TestTarget> index = new CompositeKeyIndex<>(keyPaths);
        
        // When: Calling deepClear() on empty index
        index.deepClear();
        
        // Then: Should not throw exception
        assertThat(index.lookup(List.of(1L, "A"))).isEmpty();
    }
}
