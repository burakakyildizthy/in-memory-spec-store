package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Test — Non-Bug-Condition Davranış Korunması.
 *
 * <p>These tests verify that behaviors which are NOT affected by the identity corruption bug
 * work correctly on the CURRENT (unfixed) code. They serve as a baseline to ensure the fix
 * does not introduce regressions.</p>
 *
 * <p>Non-bug-condition scenarios include:</p>
 * <ul>
 *   <li>First-time inserts (no old entity exists — no removeEntity call)</li>
 *   <li>Remove with SAME reference (reference equality holds — removeEntity succeeds)</li>
 *   <li>Lookup results in non-duplicate scenarios</li>
 *   <li>Multi-level index operations with composite keys</li>
 * </ul>
 *
 * <p><b>Property 2: Preservation</b> — Non-Bug-Condition Davranış Korunması</p>
 * <p><b>Validates: Requirements 3.1, 3.2, 3.5, 3.6, 3.7, 3.9, 3.10</b></p>
 */
class IdentityCorruptionPreservationPropertyTest {

    // ==================== Test Entity ====================

    /**
     * Minimal entity implementing Identifiable but NOT overriding equals()/hashCode().
     * Mirrors real entity classes — same pattern as the exploration test.
     */
    static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final String fkValue;

        TestEntity(int id, String fkValue) {
            this.id = id;
            this.fkValue = fkValue;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getFkValue() {
            return fkValue;
        }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", fk=" + fkValue
                    + ", ref=@" + Integer.toHexString(System.identityHashCode(this)) + "}";
        }
        // equals()/hashCode() intentionally NOT overridden — mirrors real entities
    }

    // ==================== Index Definition Helpers ====================

    @SuppressWarnings("unchecked")
    private static final MetaAttribute<TestEntity, ?> FK_ATTR =
            new StringAttribute<>("fkValue", (Class<TestEntity>) (Class<?>) TestEntity.class);

    @SuppressWarnings("unchecked")
    private static final MetaAttribute<TestEntity, ?> ID_ATTR =
            new StringAttribute<>("id", (Class<TestEntity>) (Class<?>) TestEntity.class);

    private IndexDefinition<TestEntity> createSingleLevelDefinition() {
        return IndexDefinition.builder(TestEntity.class)
                .addKeyFieldWithPath(
                        List.of(FK_ATTR),
                        e -> ((TestEntity) e).getFkValue())
                .build();
    }

    private IndexDefinition<TestEntity> createTwoLevelDefinition() {
        return IndexDefinition.builder(TestEntity.class)
                .addKeyFieldWithPath(
                        List.of(FK_ATTR),
                        e -> ((TestEntity) e).getFkValue())
                .addKeyFieldWithPath(
                        List.of(ID_ATTR),
                        e -> String.valueOf(((TestEntity) e).getIdentity()))
                .build();
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<Integer> entityIds() {
        return Arbitraries.integers().between(1, 1000);
    }

    @Provide
    Arbitrary<String> fkValues() {
        return Arbitraries.of("FK_A", "FK_B", "FK_C", "FK_D", "FK_E");
    }

    // ==================== Property Tests ====================

    /**
     * First Insert Preservation: When an entity is inserted for the first time
     * (no prior entry exists), insertEntity() should correctly add it to the
     * right bucket and lookup() should return it.
     *
     * <p>This is a non-bug-condition scenario because no removeEntity() is called —
     * the reference inequality issue never triggers.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2, 3.5</b></p>
     */
    @Property(tries = 50)
    void firstInsertShouldAddEntityToCorrectBucketAndBeRetrievableViaLookup(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String fkValue) {

        // Given: empty single-level index
        IndexDefinition<TestEntity> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        // When: first-time insert (no old entity, no removeEntity call)
        TestEntity entity = new TestEntity(entityId, fkValue);
        index.insertEntity(entity);

        // Then: entity should be in the correct bucket via lookup
        List<TestEntity> lookupResult = index.lookup(fkValue);
        assertThat(lookupResult)
                .as("lookup('%s') should return the inserted entity (id=%d)", fkValue, entityId)
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getIdentity()).isEqualTo(entityId);
                    assertThat(e.getFkValue()).isEqualTo(fkValue);
                });

        // And: getAllValues should contain exactly 1 entity
        List<TestEntity> allValues = index.getAllValues();
        assertThat(allValues)
                .as("getAllValues() should contain exactly 1 entity after first insert")
                .hasSize(1);

        // And: the entity in the index should be the SAME reference we inserted
        assertThat(lookupResult.get(0))
                .as("lookup should return the same reference that was inserted")
                .isSameAs(entity);
    }

    /**
     * Same Reference Remove Preservation: When removeEntity() is called with the
     * SAME object reference that exists in the index, removal succeeds. A subsequent
     * insertEntity() should result in exactly one entry.
     *
     * <p>This is a non-bug-condition scenario because reference equality holds —
     * List.remove(Object) succeeds via == comparison.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.5, 3.9</b></p>
     */
    @Property(tries = 50)
    void sameReferenceRemoveThenInsertShouldResultInSingleEntry(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String originalFk,
            @ForAll("fkValues") String updatedFk) {

        // Given: single-level index with entity ref@A
        IndexDefinition<TestEntity> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        TestEntity refA = new TestEntity(entityId, originalFk);
        index.insertEntity(refA);

        // When: remove with the SAME reference (not a different instance)
        boolean removed = index.removeEntity(refA);

        // Then: removal should succeed (same reference → == is true)
        assertThat(removed)
                .as("removeEntity() with same reference should succeed")
                .isTrue();

        // And: index should be empty after removal
        List<TestEntity> afterRemove = index.getAllValues();
        assertThat(afterRemove)
                .as("index should be empty after removing the only entity")
                .isEmpty();

        // When: insert new entity (potentially different FK)
        TestEntity refB = new TestEntity(entityId, updatedFk);
        index.insertEntity(refB);

        // Then: exactly 1 entry in the index
        List<TestEntity> allValues = index.getAllValues();
        long countForId = allValues.stream()
                .filter(e -> e.getIdentity().equals(entityId))
                .count();

        assertThat(countForId)
                .as("After same-ref remove + insert, entity id=%d should have exactly 1 entry", entityId)
                .isEqualTo(1);

        // And: lookup with the new FK should return the new entity
        List<TestEntity> lookupResult = index.lookup(updatedFk);
        assertThat(lookupResult)
                .as("lookup('%s') should return the newly inserted entity", updatedFk)
                .anyMatch(e -> e.getIdentity().equals(entityId));
    }

    /**
     * Lookup Result Preservation: In non-duplicate scenarios (multiple distinct entities
     * with different IDs in the same FK bucket), lookup() should return the correct
     * entity list.
     *
     * <p>This verifies that the index correctly handles multiple entities sharing the
     * same FK key — each with a unique identity.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.6, 3.7, 3.10</b></p>
     */
    @Property(tries = 50)
    void lookupShouldReturnCorrectEntitiesInNonDuplicateScenario(
            @ForAll("fkValues") String sharedFk) {

        // Given: single-level index
        IndexDefinition<TestEntity> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        // When: insert 3 distinct entities (different IDs) with the same FK
        TestEntity entity1 = new TestEntity(1, sharedFk);
        TestEntity entity2 = new TestEntity(2, sharedFk);
        TestEntity entity3 = new TestEntity(3, sharedFk);

        index.insertEntity(entity1);
        index.insertEntity(entity2);
        index.insertEntity(entity3);

        // Then: lookup should return all 3 entities
        List<TestEntity> lookupResult = index.lookup(sharedFk);
        assertThat(lookupResult)
                .as("lookup('%s') should return all 3 distinct entities", sharedFk)
                .hasSize(3);

        // And: each entity should have a unique identity
        assertThat(lookupResult)
                .extracting(TestEntity::getIdentity)
                .containsExactlyInAnyOrder(1, 2, 3);

        // And: getAllValues should also return exactly 3
        assertThat(index.getAllValues())
                .as("getAllValues() should return exactly 3 entities")
                .hasSize(3);
    }

    /**
     * Multi-Level Index Preservation: Composite key indexes should correctly handle
     * normal insert/remove/lookup cycles when using same-reference removal.
     *
     * <p>This verifies that multi-level (depth > 1) indexes work correctly in
     * non-bug-condition scenarios — same reference removal + first-time inserts.</p>
     *
     * <p><b>Validates: Requirements 3.5, 3.6, 3.9</b></p>
     */
    @Property(tries = 50)
    void multiLevelIndexShouldPreserveNormalInsertRemoveLookupCycle(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String fkValue) {

        // Given: two-level (composite key) index
        IndexDefinition<TestEntity> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        // When: first-time insert
        TestEntity entity = new TestEntity(entityId, fkValue);
        index.insertEntity(entity);

        // Then: lookup with full composite key should return the entity
        List<TestEntity> lookupResult = index.lookup(fkValue, String.valueOf(entityId));
        assertThat(lookupResult)
                .as("Multi-level lookup(%s, %d) should return the inserted entity", fkValue, entityId)
                .hasSize(1)
                .first()
                .satisfies(e -> assertThat(e.getIdentity()).isEqualTo(entityId));

        // When: remove with SAME reference (non-bug-condition)
        boolean removed = index.removeEntity(entity);
        assertThat(removed)
                .as("Multi-level removeEntity() with same reference should succeed")
                .isTrue();

        // Then: lookup should return empty
        List<TestEntity> afterRemove = index.lookup(fkValue, String.valueOf(entityId));
        assertThat(afterRemove)
                .as("Multi-level lookup after same-ref removal should be empty")
                .isEmpty();

        // When: re-insert (simulating a new update with same identity)
        TestEntity newEntity = new TestEntity(entityId, fkValue);
        index.insertEntity(newEntity);

        // Then: exactly 1 entry via lookup
        List<TestEntity> afterReinsert = index.lookup(fkValue, String.valueOf(entityId));
        assertThat(afterReinsert)
                .as("Multi-level lookup after re-insert should return exactly 1 entity")
                .hasSize(1);

        // And: getAllValues should have exactly 1 entity
        assertThat(index.getAllValues())
                .as("Multi-level getAllValues() should have exactly 1 entity after cycle")
                .hasSize(1);
    }
}
