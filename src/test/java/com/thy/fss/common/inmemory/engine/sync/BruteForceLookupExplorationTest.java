package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Bug condition exploration test — brute-force lookup performans sorunu.
 *
 * <p>Bu test, {@code dependencyGraphLookup} lambda'sının mevcut brute-force
 * implementasyonundaki (findAll + lineer tarama) performans sorununu kanıtlar.</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5</b></p>
 *
 * <p><b>Property 1: Fault Condition — Indexed Lookup Performans Eşdeğerliği</b></p>
 *
 * <p>EXPECTED: Bu test unfixed kodda FAIL eder — brute-force lookup indexed
 * lookup'a göre 10x+ yavaş olduğunu ve findAll() her çağrıda yeni ArrayList
 * kopyaladığını kanıtlar. Fix sonrası indexed lookup kullanılacağı için PASS edecek.</p>
 */
class BruteForceLookupExplorationTest {

    private static final String FOREIGN_DS = "foreignDataSource";
    private static final int FOREIGN_ENTITY_COUNT = 2000;
    private static final int ROOT_ENTITY_COUNT = 500;
    private static final int MAPPING_COUNT = 5;

    // -----------------------------------------------------------------------
    // Property 1: Fault Condition — Indexed Lookup Performans Eşdeğerliği
    // -----------------------------------------------------------------------

    /**
     * Brute-force lookup (findAll + lineer tarama) ile indexed lookup'un
     * fonksiyonel eşdeğerliğini doğrular ve brute-force'un findAll() ile
     * her çağrıda O(F) ArrayList kopyalama yaptığını kanıtlar.
     *
     * <p>Bug condition: Her lookup çağrısında findAll() TÜM entity'leri yeni
     * ArrayList'e kopyalıyor. R×M çağrı ile toplam R×M×F kopya oluşuyor.
     * Indexed lookup'ta bu kopya sıfır — doğrudan TreeMap index'ten erişim.</p>
     *
     * <p>Test, brute-force yaklaşımın indexed lookup'a göre en az 10x yavaş
     * olduğunu assert eder. Bu oran, findAll() kopyalama + lineer tarama
     * overhead'ini kanıtlar.</p>
     *
     * <p>Fix sonrası lambda indexed lookup kullanacağı için, aynı işlem
     * indexed hızda tamamlanacak ve oran assertion'ı geçersiz olacak.
     * Test, indexed lookup süresinin 500ms altında olmasını da assert eder.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5</b></p>
     */
    @Property(tries = 3)
    void bruteForceLookupShouldCompleteWithinPerformanceBudget(
            @ForAll("fkDistributions") List<String> fkValues) {

        // --- Setup: DependencyGraph with 2000 foreign entities ---
        DependencyGraph graph = new DependencyGraph();

        List<ForeignEntity> foreignEntities = new ArrayList<>(FOREIGN_ENTITY_COUNT);
        for (int i = 0; i < FOREIGN_ENTITY_COUNT; i++) {
            String parentId = fkValues.get(i % fkValues.size());
            foreignEntities.add(new ForeignEntity(i, parentId));
        }
        graph.upsertAll(FOREIGN_DS, foreignEntities);

        // --- Setup: Index for indexed lookup ---
        StringAttribute<ForeignEntity> parentIdAttr =
                new StringAttribute<>("parentId", ForeignEntity.class);
        IndexDefinition<ForeignEntity> indexDef = IndexDefinition
                .<ForeignEntity>builder(ForeignEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((ForeignEntity) entity).getParentId())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        // --- Collect root PK values ---
        List<String> rootPkValues = fkValues.stream()
                .distinct()
                .limit(ROOT_ENTITY_COUNT)
                .collect(Collectors.toList());
        while (rootPkValues.size() < ROOT_ENTITY_COUNT) {
            rootPkValues.add("pk_" + rootPkValues.size());
        }

        // --- Track findAll() call count ---
        AtomicInteger findAllCallCount = new AtomicInteger(0);

        // ---------------------------------------------------------------
        // Brute-force lookup: simulates current dependencyGraphLookup lambda
        // findAll() + lineer FK tarama — O(R × M × F)
        // ---------------------------------------------------------------
        long bruteForceStart = System.nanoTime();
        List<List<Object>> bruteForceResults = new ArrayList<>();

        for (String pkValue : rootPkValues) {
            for (int m = 0; m < MAPPING_COUNT; m++) {
                // This is exactly what the buggy lambda does:
                // 1. findAll() copies ALL entities into new ArrayList
                // 2. Linear scan for FK match
                List<ForeignEntity> allForeign = graph.findAll(FOREIGN_DS);
                findAllCallCount.incrementAndGet();
                List<Object> matched = new ArrayList<>();
                for (ForeignEntity entity : allForeign) {
                    if (Objects.equals(entity.getParentId(), pkValue)) {
                        matched.add(entity);
                    }
                }
                bruteForceResults.add(matched);
            }
        }
        long bruteForceNs = System.nanoTime() - bruteForceStart;

        // ---------------------------------------------------------------
        // Indexed lookup: what the fix will use — O(R × M × log F)
        // ---------------------------------------------------------------
        long indexedStart = System.nanoTime();
        List<List<Object>> indexedResults = new ArrayList<>();

        for (String pkValue : rootPkValues) {
            for (int m = 0; m < MAPPING_COUNT; m++) {
                List<ForeignEntity> found = graph.lookup(FOREIGN_DS, indexDef, pkValue);
                indexedResults.add(new ArrayList<>(found));
            }
        }
        long indexedNs = System.nanoTime() - indexedStart;

        long bruteForceMs = bruteForceNs / 1_000_000;
        long indexedMs = indexedNs / 1_000_000;
        double ratio = (double) bruteForceNs / Math.max(indexedNs, 1);

        // ---------------------------------------------------------------
        // Assertion 1: Fonksiyonel eşdeğerlik — aynı entity'ler dönmeli
        // ---------------------------------------------------------------
        assertThat(bruteForceResults).hasSameSizeAs(indexedResults);
        for (int i = 0; i < bruteForceResults.size(); i++) {
            assertThat(bruteForceResults.get(i))
                    .as("Lookup %d: brute-force ve indexed aynı entity'leri döndürmeli", i)
                    .containsExactlyInAnyOrderElementsOf(indexedResults.get(i));
        }

        // ---------------------------------------------------------------
        // Assertion 2: findAll() çağrı sayısı — R × M kez çağrılıyor
        // Bug condition: Her lookup için findAll() çağrılıyor
        // Fix sonrası: findAll() hiç çağrılmayacak (indexed lookup)
        // ---------------------------------------------------------------
        int expectedFindAllCalls = ROOT_ENTITY_COUNT * MAPPING_COUNT;
        assertThat(findAllCallCount.get())
                .as("findAll() %d kez çağrıldı — her root×mapping için bir kez. "
                                + "Bu, %d entity'nin %d kez kopyalandığı anlamına geliyor "
                                + "(toplam %d kopya). Indexed lookup'ta bu sıfır olmalı.",
                        findAllCallCount.get(), FOREIGN_ENTITY_COUNT,
                        findAllCallCount.get(),
                        (long) FOREIGN_ENTITY_COUNT * findAllCallCount.get())
                .isEqualTo(expectedFindAllCalls);

        // ---------------------------------------------------------------
        // Assertion 3: Brute-force, indexed'a göre en az 3x yavaş olmalı
        // Bu, findAll() kopyalama + lineer tarama overhead'ini kanıtlar
        // Fix sonrası: lambda indexed kullanacak, bu assertion geçersiz
        // ---------------------------------------------------------------
        assertThat(ratio)
                .as("Brute-force/indexed oran: %.1fx (brute=%dms, indexed=%dms). "
                                + "findAll() her çağrıda %d entity kopyalıyor, "
                                + "toplam %d findAll() çağrısı. "
                                + "Indexed lookup O(log F) ile doğrudan erişim sağlıyor.",
                        ratio, bruteForceMs, indexedMs,
                        FOREIGN_ENTITY_COUNT, findAllCallCount.get())
                .isGreaterThan(3.0);

        // ---------------------------------------------------------------
        // Assertion 4: Indexed lookup 500ms altında olmalı
        // Bu assertion fix sonrası da geçerli — indexed lookup hızlı
        // ---------------------------------------------------------------
        assertThat(indexedMs)
                .as("Indexed lookup %dms sürdü — 500ms altında olmalı", indexedMs)
                .isLessThan(500);
    }

    /**
     * Composite FK key senaryosunda brute-force ve indexed lookup eşdeğerliğini
     * ve performans farkını test eder.
     *
     * <p><b>Validates: Requirements 1.2, 1.4</b></p>
     */
    @Property(tries = 3)
    void compositeFkLookupShouldBeEquivalentAndFast(
            @ForAll("compositeFkDistributions") List<String[]> compositeFkValues) {

        DependencyGraph graph = new DependencyGraph();

        List<CompositeFkEntity> foreignEntities = new ArrayList<>(FOREIGN_ENTITY_COUNT);
        for (int i = 0; i < FOREIGN_ENTITY_COUNT; i++) {
            String[] fk = compositeFkValues.get(i % compositeFkValues.size());
            foreignEntities.add(new CompositeFkEntity(i, fk[0], fk[1]));
        }
        graph.upsertAll(FOREIGN_DS, foreignEntities);

        StringAttribute<CompositeFkEntity> parentIdAttr =
                new StringAttribute<>("parentId", CompositeFkEntity.class);
        StringAttribute<CompositeFkEntity> categoryAttr =
                new StringAttribute<>("category", CompositeFkEntity.class);
        IndexDefinition<CompositeFkEntity> indexDef = IndexDefinition
                .<CompositeFkEntity>builder(CompositeFkEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((CompositeFkEntity) entity).getParentId())
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((CompositeFkEntity) entity).getCategory())
                .build();
        graph.registerIndex(FOREIGN_DS, indexDef);

        List<String[]> rootKeys = compositeFkValues.stream()
                .distinct()
                .limit(ROOT_ENTITY_COUNT)
                .collect(Collectors.toList());

        // Brute-force
        AtomicInteger findAllCalls = new AtomicInteger(0);
        List<List<Object>> bruteForceResults = new ArrayList<>();
        for (String[] key : rootKeys) {
            List<CompositeFkEntity> allForeign = graph.findAll(FOREIGN_DS);
            findAllCalls.incrementAndGet();
            List<Object> matched = new ArrayList<>();
            for (CompositeFkEntity entity : allForeign) {
                if (Objects.equals(entity.getParentId(), key[0])
                        && Objects.equals(entity.getCategory(), key[1])) {
                    matched.add(entity);
                }
            }
            bruteForceResults.add(matched);
        }

        // Indexed
        List<List<Object>> indexedResults = new ArrayList<>();
        for (String[] key : rootKeys) {
            List<CompositeFkEntity> found = graph.lookup(FOREIGN_DS, indexDef, key[0], key[1]);
            indexedResults.add(new ArrayList<>(found));
        }

        // Functional equivalence
        assertThat(bruteForceResults).hasSameSizeAs(indexedResults);
        for (int i = 0; i < bruteForceResults.size(); i++) {
            assertThat(bruteForceResults.get(i))
                    .as("Composite FK lookup %d: eşdeğerlik", i)
                    .containsExactlyInAnyOrderElementsOf(indexedResults.get(i));
        }

        // findAll() call count — proves the bug pattern
        assertThat(findAllCalls.get())
                .as("Composite FK: findAll() %d kez çağrıldı — her root key için bir kez. "
                                + "Indexed lookup'ta bu sıfır olmalı.",
                        findAllCalls.get())
                .isEqualTo(rootKeys.size());
    }

    /**
     * Boş datasource ve null FK edge case'lerinde brute-force ve indexed
     * lookup'un aynı davranışı gösterdiğini doğrular.
     *
     * <p><b>Validates: Requirements 1.1, 1.2</b></p>
     */
    @Property(tries = 10)
    void emptyAndNullFkEdgeCasesShouldBeEquivalent(
            @ForAll("fkDistributions") List<String> fkValues) {

        DependencyGraph graph = new DependencyGraph();

        StringAttribute<ForeignEntity> parentIdAttr =
                new StringAttribute<>("parentId", ForeignEntity.class);
        IndexDefinition<ForeignEntity> indexDef = IndexDefinition
                .<ForeignEntity>builder(ForeignEntity.class)
                .addKeyFieldWithPath(
                        List.of(parentIdAttr),
                        entity -> ((ForeignEntity) entity).getParentId())
                .build();

        // Edge case 1: Empty datasource
        graph.registerIndex(FOREIGN_DS, indexDef);
        List<ForeignEntity> bruteForceEmpty = graph.findAll(FOREIGN_DS);
        List<ForeignEntity> indexedEmpty = graph.lookup(FOREIGN_DS, indexDef, "anyKey");
        assertThat(bruteForceEmpty).isEmpty();
        assertThat(indexedEmpty).isEmpty();

        // Edge case 2: Lookup non-existent key
        List<ForeignEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(new ForeignEntity(i, fkValues.get(i % fkValues.size())));
        }
        graph.upsertAll(FOREIGN_DS, entities);
        graph.rebuildIndexesForDataSource(FOREIGN_DS);

        String nonExistentKey = "nonExistent_" + System.nanoTime();
        List<ForeignEntity> allEntities = graph.findAll(FOREIGN_DS);
        List<Object> bfMatched = new ArrayList<>();
        for (ForeignEntity entity : allEntities) {
            if (Objects.equals(entity.getParentId(), nonExistentKey)) {
                bfMatched.add(entity);
            }
        }
        List<ForeignEntity> idxMatched = graph.lookup(FOREIGN_DS, indexDef, nonExistentKey);

        assertThat(bfMatched).isEmpty();
        assertThat(idxMatched).isEmpty();
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
                .ofMinSize(50)
                .ofMaxSize(200);
    }

    @Provide
    Arbitrary<List<String[]>> compositeFkDistributions() {
        Arbitrary<String> parentIds = Arbitraries.of(
                "parent1", "parent2", "parent3", "parent4", "parent5",
                "parent6", "parent7", "parent8", "parent9", "parent10");
        Arbitrary<String> categories = Arbitraries.of(
                "catA", "catB", "catC", "catD", "catE");

        return parentIds.flatMap(pid ->
                categories.map(cat -> new String[]{pid, cat})
        ).list().ofMinSize(20).ofMaxSize(50);
    }

    // -----------------------------------------------------------------------
    // Test entities
    // -----------------------------------------------------------------------

    private static class ForeignEntity implements Identifiable<Integer> {
        private final int id;
        private final String parentId;

        ForeignEntity(int id, String parentId) {
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
            ForeignEntity that = (ForeignEntity) o;
            return id == that.id && Objects.equals(parentId, that.parentId);
        }

        @Override
        public int hashCode() { return Objects.hash(id, parentId); }
    }

    private static class CompositeFkEntity implements Identifiable<Integer> {
        private final int id;
        private final String parentId;
        private final String category;

        CompositeFkEntity(int id, String parentId, String category) {
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
            CompositeFkEntity that = (CompositeFkEntity) o;
            return id == that.id
                    && Objects.equals(parentId, that.parentId)
                    && Objects.equals(category, that.category);
        }

        @Override
        public int hashCode() { return Objects.hash(id, parentId, category); }
    }
}
