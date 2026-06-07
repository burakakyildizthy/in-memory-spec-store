package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Preservation property tests — Mapping Sonuç Doğruluğunun Korunması.
 *
 * <p><b>Property 2: Preservation</b> — Brute-force lookup'un fonksiyonel doğruluğunu
 * baseline olarak kaydeder. Bu testler fix uygulanmadan ÖNCE yazılır ve PASS eder.
 * Fix sonrası indexed lookup'un aynı sonuçları ürettiğini doğrulamak için
 * tekrar çalıştırılır.</p>
 *
 * <p>Observation-first methodology: Unfixed kodda brute-force lookup'un doğru
 * sonuçlar ürettiğini gözlemle, sonra bu davranışı property olarak encode et.</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10</b></p>
 *
 * <p><b>EXPECTED OUTCOME: Tests PASS on unfixed code (baseline preservation)</b></p>
 */
class BruteForceLookupPreservationTest {

    private static final String FOREIGN_DS = "foreignDataSource";

    // -----------------------------------------------------------------------
    // Property 2.1: Brute-force ve indexed lookup fonksiyonel eşdeğerliği
    // For all mapping inputs, brute-force lookup sonuçları ile indexed lookup
    // sonuçları birebir aynı olmalı.
    // -----------------------------------------------------------------------

    /**
     * For all FK distributions, brute-force lookup (findAll + linear scan)
     * and indexed lookup (registerIndex + lookup) return identical entity sets.
     *
     * <p>This property captures the functional correctness of the brute-force
     * approach that must be preserved after the indexed lookup optimization.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2, 3.6</b></p>
     */
    @Property(tries = 50)
    void bruteForceLookupResultsMatchIndexedLookup(
            @ForAll("fkDistributions") List<String> fkValues,
            @ForAll("entityCounts") int entityCount) {

        DependencyGraph graph = new DependencyGraph();

        // Populate foreign entities with generated FK values
        List<SimpleEntity> entities = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            String parentId = fkValues.get(i % fkValues.size());
            entities.add(new SimpleEntity(i, parentId));
        }
        graph.upsertAll(FOREIGN_DS, entities);

        // Register index for indexed lookup
        StringAttribute<SimpleEntity> parentIdAttr =
                new StringAttribute<>("parentId", SimpleEntity.class);
        IndexDefinition<SimpleEntity> indexDef = IndexDefinition
                .<SimpleEntity>builder(SimpleEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((SimpleEntity) entity).getParentId())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        // Test each distinct FK value as a lookup key
        List<String> lookupKeys = fkValues.stream().distinct().collect(Collectors.toList());

        for (String key : lookupKeys) {
            // Brute-force: findAll + linear scan (current lambda behavior)
            List<SimpleEntity> allForeign = graph.findAll(FOREIGN_DS);
            List<Object> bruteForceMatched = new ArrayList<>();
            for (SimpleEntity entity : allForeign) {
                if (Objects.equals(entity.getParentId(), key)) {
                    bruteForceMatched.add(entity);
                }
            }

            // Indexed: lookup via NestedTreeMapIndex
            List<SimpleEntity> indexedMatched = graph.lookup(FOREIGN_DS, indexDef, key);

            // Functional equivalence: same entities returned
            assertThat(bruteForceMatched)
                    .as("FK key '%s': brute-force and indexed lookup must return same entities", key)
                    .containsExactlyInAnyOrderElementsOf(indexedMatched);
        }
    }

    // -----------------------------------------------------------------------
    // Property 2.2: Composite FK key eşleşme doğruluğu
    // For all composite FK key inputs, eşleşme sonuçları aynı olmalı.
    // -----------------------------------------------------------------------

    /**
     * For all composite FK key combinations, brute-force multi-field matching
     * and indexed composite key lookup return identical entity sets.
     *
     * <p><b>Validates: Requirements 3.8</b></p>
     */
    @Property(tries = 50)
    void compositeFkLookupResultsMatchIndexedLookup(
            @ForAll("compositeFkDistributions") List<String[]> compositeFkValues,
            @ForAll("entityCounts") int entityCount) {

        DependencyGraph graph = new DependencyGraph();

        // Populate entities with composite FK values
        List<CompositeEntity> entities = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            String[] fk = compositeFkValues.get(i % compositeFkValues.size());
            entities.add(new CompositeEntity(i, fk[0], fk[1]));
        }
        graph.upsertAll(FOREIGN_DS, entities);

        // Register composite index
        StringAttribute<CompositeEntity> parentIdAttr =
                new StringAttribute<>("parentId", CompositeEntity.class);
        StringAttribute<CompositeEntity> categoryAttr =
                new StringAttribute<>("category", CompositeEntity.class);
        IndexDefinition<CompositeEntity> indexDef = IndexDefinition
                .<CompositeEntity>builder(CompositeEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((CompositeEntity) entity).getParentId())
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((CompositeEntity) entity).getCategory())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        // Test each distinct composite key
        List<String[]> lookupKeys = compositeFkValues.stream()
                .collect(Collectors.toMap(
                        k -> k[0] + "|" + k[1], k -> k, (a, b) -> a))
                .values().stream().collect(Collectors.toList());

        for (String[] key : lookupKeys) {
            // Brute-force: findAll + multi-field linear scan
            List<CompositeEntity> allForeign = graph.findAll(FOREIGN_DS);
            List<Object> bruteForceMatched = new ArrayList<>();
            for (CompositeEntity entity : allForeign) {
                if (Objects.equals(entity.getParentId(), key[0])
                        && Objects.equals(entity.getCategory(), key[1])) {
                    bruteForceMatched.add(entity);
                }
            }

            // Indexed: composite key lookup
            List<CompositeEntity> indexedMatched = graph.lookup(FOREIGN_DS, indexDef, key[0], key[1]);

            assertThat(bruteForceMatched)
                    .as("Composite key [%s, %s]: brute-force and indexed must match", key[0], key[1])
                    .containsExactlyInAnyOrderElementsOf(indexedMatched);
        }
    }

    // -----------------------------------------------------------------------
    // Property 2.3: Boş datasource — her iki yaklaşım da boş liste döndürmeli
    // For all empty datasource inputs, both approaches return empty list.
    // -----------------------------------------------------------------------

    /**
     * For any lookup key on an empty datasource, both brute-force and indexed
     * lookup return empty lists.
     *
     * <p><b>Validates: Requirements 3.3, 3.7</b></p>
     */
    @Property(tries = 30)
    void emptyDatasourceReturnEmptyForBothApproaches(
            @ForAll("lookupKeys") String lookupKey) {

        DependencyGraph graph = new DependencyGraph();

        // Register index on empty datasource
        StringAttribute<SimpleEntity> parentIdAttr =
                new StringAttribute<>("parentId", SimpleEntity.class);
        IndexDefinition<SimpleEntity> indexDef = IndexDefinition
                .<SimpleEntity>builder(SimpleEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((SimpleEntity) entity).getParentId())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        // Brute-force on empty datasource
        List<SimpleEntity> bruteForceAll = graph.findAll(FOREIGN_DS);
        List<Object> bruteForceMatched = new ArrayList<>();
        for (SimpleEntity entity : bruteForceAll) {
            if (Objects.equals(entity.getParentId(), lookupKey)) {
                bruteForceMatched.add(entity);
            }
        }

        // Indexed on empty datasource
        List<SimpleEntity> indexedMatched = graph.lookup(FOREIGN_DS, indexDef, lookupKey);

        assertThat(bruteForceMatched)
                .as("Empty datasource: brute-force must return empty for key '%s'", lookupKey)
                .isEmpty();
        assertThat(indexedMatched)
                .as("Empty datasource: indexed must return empty for key '%s'", lookupKey)
                .isEmpty();
    }

    // -----------------------------------------------------------------------
    // Property 2.4: Null FK değerleri — her iki yaklaşım da aynı davranış
    // For all null FK inputs, both approaches show same behavior.
    // -----------------------------------------------------------------------

    /**
     * For entities with null FK values mixed with non-null FK values,
     * brute-force and indexed approaches produce identical results for
     * non-null lookup keys. Null-FK entities are skipped by the index
     * (NestedTreeMapIndex skips null keys during build), so the preservation
     * property focuses on non-null key lookups remaining correct when
     * null-FK entities are present in the datasource.
     *
     * <p>Observed behavior on unfixed code:</p>
     * <ul>
     *   <li>Brute-force: Objects.equals(null, null) matches null-FK entities</li>
     *   <li>Indexed: NestedTreeMapIndex skips null keys — lookup(null) returns empty</li>
     *   <li>For non-null keys: both approaches return identical results</li>
     * </ul>
     *
     * <p><b>Validates: Requirements 3.4, 3.5</b></p>
     */
    @Property(tries = 30)
    void nullFkValuesProduceSameResultsForNonNullKeys(
            @ForAll("fkDistributionsWithNulls") List<String> fkValues,
            @ForAll("entityCounts") int entityCount) {

        DependencyGraph graph = new DependencyGraph();

        // Populate entities — some will have null parentId
        List<SimpleEntity> entities = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            String parentId = fkValues.get(i % fkValues.size()); // may be null
            entities.add(new SimpleEntity(i, parentId));
        }
        graph.upsertAll(FOREIGN_DS, entities);

        // Register index
        StringAttribute<SimpleEntity> parentIdAttr =
                new StringAttribute<>("parentId", SimpleEntity.class);
        IndexDefinition<SimpleEntity> indexDef = IndexDefinition
                .<SimpleEntity>builder(SimpleEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((SimpleEntity) entity).getParentId())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        List<SimpleEntity> allForeign = graph.findAll(FOREIGN_DS);

        // Verify: null-FK entities exist in the datasource (brute-force finds them)
        long nullFkCount = entities.stream().filter(e -> e.getParentId() == null).count();
        long bruteForceNullCount = allForeign.stream()
                .filter(e -> e.getParentId() == null).count();
        assertThat(bruteForceNullCount)
                .as("Null-FK entities should be present in datasource via findAll()")
                .isEqualTo(nullFkCount);

        // Core preservation property: for all NON-NULL keys, brute-force and
        // indexed lookup return identical results even when null-FK entities exist
        List<String> nonNullKeys = fkValues.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        for (String key : nonNullKeys) {
            List<Object> bfMatched = new ArrayList<>();
            for (SimpleEntity entity : allForeign) {
                if (Objects.equals(entity.getParentId(), key)) {
                    bfMatched.add(entity);
                }
            }
            List<SimpleEntity> idxMatched = graph.lookup(FOREIGN_DS, indexDef, key);

            assertThat(bfMatched)
                    .as("Non-null key '%s' with null-FK entities present: must match", key)
                    .containsExactlyInAnyOrderElementsOf(idxMatched);
        }
    }

    // -----------------------------------------------------------------------
    // Providers
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<List<String>> fkDistributions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .list()
                .ofMinSize(5)
                .ofMaxSize(50);
    }

    @Provide
    Arbitrary<List<String>> fkDistributionsWithNulls() {
        // Mix of non-null strings and nulls
        Arbitrary<String> nullableStrings = Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.just(null)
        );
        return nullableStrings.list().ofMinSize(5).ofMaxSize(30);
    }

    @Provide
    Arbitrary<List<String[]>> compositeFkDistributions() {
        Arbitrary<String> parentIds = Arbitraries.of(
                "parent1", "parent2", "parent3", "parent4", "parent5");
        Arbitrary<String> categories = Arbitraries.of(
                "catA", "catB", "catC", "catD");

        return parentIds.flatMap(pid ->
                categories.map(cat -> new String[]{pid, cat})
        ).list().ofMinSize(5).ofMaxSize(20);
    }

    @Provide
    Arbitrary<Integer> entityCounts() {
        return Arbitraries.integers().between(10, 200);
    }

    @Provide
    Arbitrary<String> lookupKeys() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15);
    }

    // -----------------------------------------------------------------------
    // Test entities
    // -----------------------------------------------------------------------

    private static class SimpleEntity implements Identifiable<Integer> {
        private final int id;
        private final String parentId;

        SimpleEntity(int id, String parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        @Override
        public Integer getIdentity() { return id; }

        public String getParentId() { return parentId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleEntity that = (SimpleEntity) o;
            return id == that.id && Objects.equals(parentId, that.parentId);
        }

        @Override
        public int hashCode() { return Objects.hash(id, parentId); }
    }

    private static class CompositeEntity implements Identifiable<Integer> {
        private final int id;
        private final String parentId;
        private final String category;

        CompositeEntity(int id, String parentId, String category) {
            this.id = id;
            this.parentId = parentId;
            this.category = category;
        }

        @Override
        public Integer getIdentity() { return id; }

        public String getParentId() { return parentId; }

        public String getCategory() { return category; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompositeEntity that = (CompositeEntity) o;
            return id == that.id
                    && Objects.equals(parentId, that.parentId)
                    && Objects.equals(category, that.category);
        }

        @Override
        public int hashCode() { return Objects.hash(id, parentId, category); }
    }
}
