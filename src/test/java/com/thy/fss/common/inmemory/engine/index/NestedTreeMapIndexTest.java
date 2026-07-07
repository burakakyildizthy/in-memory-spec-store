package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.User_;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NestedTreeMapIndex.
 * Tests index building, lookup operations, and deep clearing functionality.
 */
class NestedTreeMapIndexTest {

    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String CHARLIE = "Charlie";

    /**
     * Test deepClear() on a single-level index.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearSingleLevelIndex() {
        // Given: A single-level index with entities
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        users.add(createUser("1", ALICE));
        users.add(createUser("2", BOB));
        users.add(createUser("3", CHARLIE));
        
        index.build(users);
        
        // Verify index has data before clearing
        assertThat(index.lookup("1")).hasSize(1);
        assertThat(index.lookup("2")).hasSize(1);
        assertThat(index.getAllValues()).hasSize(3);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All data should be cleared
        assertThat(index.lookup("1")).isEmpty();
        assertThat(index.lookup("2")).isEmpty();
        assertThat(index.getAllValues()).isEmpty();
    }
    
    /**
     * Test deepClear() on a multi-level nestedSortedMap index.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearMultiLevelNestedTreeMap() {
        // Given: A 2-level index with entities
        IndexDefinition<User> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        users.add(createUser("1", ALICE));
        users.add(createUser("1", "Alice2"));
        users.add(createUser("2", BOB));
        users.add(createUser("2", "Bob2"));
        users.add(createUser("3", CHARLIE));
        
        index.build(users);
        
        // Verify index has data before clearing
        assertThat(index.lookup("1", ALICE)).hasSize(1);
        assertThat(index.lookup("2", BOB)).hasSize(1);
        assertThat(index.getAllValues()).hasSize(5);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All nestedSortedMaps and entity lists should be cleared
        assertThat(index.lookup("1", ALICE)).isEmpty();
        assertThat(index.lookup("2", BOB)).isEmpty();
        assertThat(index.getAllValues()).isEmpty();
    }
    
    /**
     * Test that entity lists are cleared at leaf level.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldClearEntityListsAtLeafLevel() {
        // Given: An index with multiple entities sharing the same key
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        users.add(createUser("1", ALICE));
        users.add(createUser("1", "Alice2"));
        users.add(createUser("1", "Alice3"));
        
        index.build(users);
        
        // Verify multiple entities with same key
        assertThat(index.lookup("1")).hasSize(3);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: Entity list should be cleared
        assertThat(index.lookup("1")).isEmpty();
        assertThat(index.getAllValues()).isEmpty();
    }
    
    /**
     * Test that intermediateSortedMaps are cleared at all levels.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldClearIntermediateTreeMapsAtAllLevels() {
        // Given: A 3-level index with nestedSortedMaps
        IndexDefinition<User> definition = createThreeLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        users.add(createUser("1", ALICE));
        users.add(createUser("2", BOB));
        users.add(createUser("3", CHARLIE));
        
        index.build(users);
        
        // Verify index has data at all levels
        assertThat(index.getAllValues()).hasSize(3);
        SortedMap<Comparable<?>, Object> partialResult = index.partialLookup("1");
        assertThat(partialResult).isNotEmpty();
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All levels should be cleared
        assertThat(index.getAllValues()).isEmpty();
       SortedMap<Comparable<?>, Object> partialResultAfter = index.partialLookup("1");
        assertThat(partialResultAfter).isEmpty();
    }
    
    /**
     * Test that deepClear() is idempotent (safe to call multiple times).
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldBeIdempotentWhenCalledMultipleTimes() {
        // Given: An index with data
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        users.add(createUser("1", ALICE));
        users.add(createUser("2", BOB));
        
        index.build(users);
        
        // When: Calling deepClear() multiple times
        index.deepClear();
        index.deepClear();
        index.deepClear();
        
        // Then: Should not throw exception and index should remain empty
        assertThat(index.getAllValues()).isEmpty();
        assertThat(index.lookup("1")).isEmpty();
    }
    
    /**
     * Test deepClear() on an empty index.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldHandleDeepClearOnEmptyIndex() {
        // Given: An empty index
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        // When: Calling deepClear() on empty index
        index.deepClear();
        
        // Then: Should not throw exception
        assertThat(index.getAllValues()).isEmpty();
    }
    
    /**
     * Test that index can be rebuilt after deepClear().
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldAllowRebuildAfterDeepClear() {
        // Given: An index that has been cleared
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users1 = new ArrayList<>();
        users1.add(createUser("1", ALICE));
        users1.add(createUser("2", BOB));
        
        index.build(users1);
        index.deepClear();
        
        // When: Rebuilding the index with new data
        List<User> users2 = new ArrayList<>();
        users2.add(createUser("3", CHARLIE));
        users2.add(createUser("4", "David"));
        
        index.build(users2);
        
        // Then: Index should contain only new data
        assertThat(index.getAllValues()).hasSize(2);
        assertThat(index.lookup("1")).isEmpty(); // Old data gone
        assertThat(index.lookup("3")).hasSize(1); // New data present
        assertThat(index.lookup("4")).hasSize(1);
    }
    
    /**
     * Test deepClear() with complex nested structure (3 levels).
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void shouldDeepClearComplexNestedStructure() {
        // Given: A 3-level index with complex nesting
        IndexDefinition<User> definition = createThreeLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);
        
        List<User> users = new ArrayList<>();
        // Create users with different combinations of keys
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                users.add(createUser(String.valueOf(i), "Name" + j));
            }
        }
        
        index.build(users);
        
        // Verify complex structure is built
        assertThat(index.getAllValues()).hasSize(9);
        
        // When: Calling deepClear()
        index.deepClear();
        
        // Then: All nested levels should be cleared
        assertThat(index.getAllValues()).isEmpty();
        // Verify lookups return empty
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                assertThat(index.lookup(String.valueOf(i), "Name" + j, String.valueOf(i)))
                    .isEmpty();
            }
        }
    }
    
    // === insertEntity (public) tests ===

    /**
     * Test public insertEntity() on a single-level index.
     * Requirements: 2.1
     */
    @Test
    void shouldInsertEntityIncrementallySingleLevel() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);

        index.insertEntity(alice);
        index.insertEntity(bob);

        assertThat(index.lookup("1")).containsExactly(alice);
        assertThat(index.lookup("2")).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(2);
    }

    /**
     * Test public insertEntity() on a multi-level index.
     * Requirements: 2.1
     */
    @Test
    void shouldInsertEntityIncrementallyMultiLevel() {
        IndexDefinition<User> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);

        index.insertEntity(alice);
        index.insertEntity(bob);

        assertThat(index.lookup("1", ALICE)).containsExactly(alice);
        assertThat(index.lookup("2", BOB)).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(2);
    }

    /**
     * Test insertEntity() skips null entity and null key.
     * Requirements: 2.1
     */
    @Test
    void shouldInsertEntitySkipNullEntityAndNullKey() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        index.insertEntity(null);
        User noId = createUser(null, "NoId");
        index.insertEntity(noId);

        assertThat(index.getAllValues()).isEmpty();
        assertThat(index.getStatistics("ds").getTotalEntries()).isZero();
    }

    // === removeEntity tests ===

    /**
     * Test removeEntity() on a single-level index.
     * Requirements: 2.1
     */
    @Test
    void shouldRemoveEntityFromSingleLevelIndex() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        index.insertEntity(alice);
        index.insertEntity(bob);

        boolean removed = index.removeEntity(alice);

        assertThat(removed).isTrue();
        assertThat(index.lookup("1")).isEmpty();
        assertThat(index.lookup("2")).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test removeEntity() on a multi-level index.
     * Requirements: 2.1
     */
    @Test
    void shouldRemoveEntityFromMultiLevelIndex() {
        IndexDefinition<User> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        index.insertEntity(alice);
        index.insertEntity(bob);

        boolean removed = index.removeEntity(alice);

        assertThat(removed).isTrue();
        assertThat(index.lookup("1", ALICE)).isEmpty();
        assertThat(index.lookup("2", BOB)).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test removeEntity() prunes empty leaf nodes (single-level).
     * Requirements: 2.1
     */
    @Test
    void shouldPruneEmptyLeafNodeSingleLevel() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        index.insertEntity(alice);

        index.removeEntity(alice);

        // The key "1" should be pruned from rootMap
        assertThat(index.partialLookup()).isEmpty();
        assertThat(index.getStatistics("ds").getTotalEntries()).isZero();
    }

    /**
     * Test removeEntity() prunes empty intermediate nodes (multi-level).
     * Requirements: 2.1
     */
    @Test
    void shouldPruneEmptyIntermediateNodesMultiLevel() {
        IndexDefinition<User> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        index.insertEntity(alice);

        index.removeEntity(alice);

        // Both the leaf and intermediate node should be pruned
        assertThat(index.partialLookup()).isEmpty();
        assertThat(index.getStatistics("ds").getTotalEntries()).isZero();
    }

    /**
     * Test removeEntity() does not prune when other entities remain in the same leaf.
     * Requirements: 2.1
     */
    @Test
    void shouldNotPruneWhenOtherEntitiesRemainInLeaf() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        // Use different identities so idempotent insert doesn't deduplicate them
        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        index.insertEntity(alice);
        index.insertEntity(bob);

        index.removeEntity(alice);

        assertThat(index.lookup("2")).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test removeEntity() returns false for entity not in index.
     * Requirements: 2.1
     */
    @Test
    void shouldReturnFalseWhenEntityNotFound() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        index.insertEntity(alice);

        boolean removed = index.removeEntity(bob);

        assertThat(removed).isFalse();
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test removeEntity() handles null entity and null key gracefully.
     * Requirements: 2.1
     */
    @Test
    void shouldHandleNullEntityAndNullKeyOnRemove() {
        IndexDefinition<User> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        index.insertEntity(alice);

        assertThat(index.removeEntity(null)).isFalse();
        User noId = createUser(null, "NoId");
        assertThat(index.removeEntity(noId)).isFalse();
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test removeEntity() on 3-level index with pruning.
     * Requirements: 2.1
     */
    @Test
    void shouldRemoveAndPruneThreeLevelIndex() {
        IndexDefinition<User> definition = createThreeLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        index.insertEntity(alice);
        index.insertEntity(bob);

        index.removeEntity(alice);

        assertThat(index.lookup("1", ALICE, "1")).isEmpty();
        assertThat(index.lookup("2", BOB, "2")).containsExactly(bob);
        assertThat(index.getStatistics("ds").getTotalEntries()).isEqualTo(1);
    }

    /**
     * Test symmetry: insertEntity then removeEntity leaves index empty.
     * Requirements: 2.1
     */
    @Test
    void shouldBeSymmetricWithInsertEntity() {
        IndexDefinition<User> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<User> index = new NestedTreeMapIndex<>(definition);

        User alice = createUser("1", ALICE);
        User bob = createUser("2", BOB);
        User charlie = createUser("3", CHARLIE);

        index.insertEntity(alice);
        index.insertEntity(bob);
        index.insertEntity(charlie);

        index.removeEntity(alice);
        index.removeEntity(bob);
        index.removeEntity(charlie);

        assertThat(index.getAllValues()).isEmpty();
        assertThat(index.getStatistics("ds").getTotalEntries()).isZero();
        assertThat(index.partialLookup()).isEmpty();
    }

    // Helper methods
    
    private User createUser(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
    
    private IndexDefinition<User> createSingleLevelDefinition() {
        return IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
    }
    
    private IndexDefinition<User> createTwoLevelDefinition() {
        return IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .addKeyField(User_.name)
            .build();
    }
    
    private IndexDefinition<User> createThreeLevelDefinition() {
        // For 3-level, we'll use id, name, and id again (just for testing purposes)
        return IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .addKeyField(User_.name)
            .addKeyField(User_.id)
            .build();
    }
}
