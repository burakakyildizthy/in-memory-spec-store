package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fault Condition Exploration Property Test — Identity-Based Remove Sonrası Tekil Entry.
 *
 * <p>This test proves that {@code NestedTreeMapIndex.removeEntity()} fails silently when
 * the entity reference passed differs from the reference stored in the index, even though
 * both have the same identity ({@code getIdentity()}). This is because entity classes
 * do not override {@code equals()}/{@code hashCode()}, so {@code List.remove(Object)}
 * falls back to reference equality ({@code ==}).</p>
 *
 * <p>After the failed removal, {@code insertEntity()} blindly appends the new entity,
 * causing duplicate accumulation in the index.</p>
 *
 * <p><b>Property 1: Fault Condition</b> — Identity-Bazlı Remove Sonrası Tekil Entry</p>
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.5, 2.1, 2.2, 2.3, 2.5</b></p>
 */
class IdentityCorruptionFaultConditionPropertyTest {

    // ==================== Test Entity ====================

    /**
     * Minimal entity implementing Identifiable but NOT overriding equals()/hashCode().
     * This mirrors real entity classes — the root cause of the bug.
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
        // equals()/hashCode() intentionally NOT overridden — this IS the bug condition
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
     * Property 1: Single remove→insert cycle with different object references.
     *
     * <p>Scenario:
     * <ol>
     *   <li>Create index, insert entity ref@A (id=X, fk=F1)</li>
     *   <li>Create different instance ref@B with same identity (id=X, fk=F1)</li>
     *   <li>Call removeEntity(ref@B) — should remove by identity</li>
     *   <li>Create ref@C (id=X, fk=F2), call insertEntity(ref@C)</li>
     *   <li>Assert: exactly 1 entry for id=X in the index</li>
     * </ol>
     * </p>
     *
     * <p>On unfixed code: removeEntity(ref@B) fails silently (ref@B != ref@A),
     * then insertEntity(ref@C) adds a duplicate → 2 entries instead of 1.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 2.1, 2.2, 2.3</b></p>
     */
    @Property(tries = 50)
    void singleUpdateCycleShouldResultInExactlyOneEntry(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String originalFk,
            @ForAll("fkValues") String updatedFk) {

        // Given: single-level index with entity ref@A
        IndexDefinition<TestEntity> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        TestEntity refA = new TestEntity(entityId, originalFk);
        index.insertEntity(refA);

        // When: remove with DIFFERENT instance ref@B (same identity, same FK)
        TestEntity refB = new TestEntity(entityId, originalFk);
        assertThat(refA).isNotSameAs(refB); // confirm different references
        index.removeEntity(refB);

        // And: insert ref@C with potentially updated FK
        TestEntity refC = new TestEntity(entityId, updatedFk);
        index.insertEntity(refC);

        // Then: index should contain exactly 1 entry for this entity
        List<TestEntity> allEntities = index.getAllValues();
        long countForId = allEntities.stream()
                .filter(e -> e.getIdentity().equals(entityId))
                .count();

        assertThat(countForId)
                .as("Entity id=%d should have exactly 1 entry in index after remove→insert cycle "
                        + "(refA=@%s, refB=@%s, refC=@%s)",
                        entityId,
                        Integer.toHexString(System.identityHashCode(refA)),
                        Integer.toHexString(System.identityHashCode(refB)),
                        Integer.toHexString(System.identityHashCode(refC)))
                .isEqualTo(1);
    }

    /**
     * Property 2: Cumulative duplicate accumulation over 3 consecutive update cycles.
     *
     * <p>Simulates 3 streaming batches updating the same entity. Each batch:
     * removeEntity(differentRef) → insertEntity(newRef). On unfixed code,
     * each failed removal adds another duplicate.</p>
     *
     * <p><b>Validates: Requirements 1.5, 2.5</b></p>
     */
    @Property(tries = 50)
    void threeConsecutiveUpdatesShouldStillHaveOneEntry(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String fk) {

        IndexDefinition<TestEntity> definition = createSingleLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        // Initial insert
        TestEntity current = new TestEntity(entityId, fk);
        index.insertEntity(current);

        // Simulate 3 consecutive streaming updates
        for (int cycle = 1; cycle <= 3; cycle++) {
            // "old" reference captured before upsert (different instance, same identity)
            TestEntity oldRef = new TestEntity(entityId, fk);
            index.removeEntity(oldRef);

            // "new" reference after upsert
            TestEntity newRef = new TestEntity(entityId, fk);
            index.insertEntity(newRef);
            current = newRef;
        }

        // Assert: still exactly 1 entry
        List<TestEntity> allEntities = index.getAllValues();
        long countForId = allEntities.stream()
                .filter(e -> e.getIdentity().equals(entityId))
                .count();

        assertThat(countForId)
                .as("After 3 update cycles, entity id=%d should have exactly 1 entry (not %d)",
                        entityId, countForId)
                .isEqualTo(1);
    }

    /**
     * Property 3: Multi-level (depth > 1) index — same identity-based removal bug.
     *
     * <p>Verifies the bug also manifests in multi-level indexes where
     * removeEntityRecursive() has the same List.remove(Object) issue at leaf level.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 2.1, 2.2</b></p>
     */
    @Property(tries = 50)
    void multiLevelIndexShouldAlsoHaveOneEntryAfterCycle(
            @ForAll("entityIds") int entityId,
            @ForAll("fkValues") String fk) {

        IndexDefinition<TestEntity> definition = createTwoLevelDefinition();
        NestedTreeMapIndex<TestEntity> index = new NestedTreeMapIndex<>(definition);

        // Insert ref@A
        TestEntity refA = new TestEntity(entityId, fk);
        index.insertEntity(refA);

        // Remove with different ref@B (same identity)
        TestEntity refB = new TestEntity(entityId, fk);
        index.removeEntity(refB);

        // Insert ref@C
        TestEntity refC = new TestEntity(entityId, fk);
        index.insertEntity(refC);

        // Assert: exactly 1 entry
        List<TestEntity> allEntities = index.getAllValues();
        long countForId = allEntities.stream()
                .filter(e -> e.getIdentity().equals(entityId))
                .count();

        assertThat(countForId)
                .as("Multi-level index: entity id=%d should have exactly 1 entry after cycle", entityId)
                .isEqualTo(1);
    }
}
