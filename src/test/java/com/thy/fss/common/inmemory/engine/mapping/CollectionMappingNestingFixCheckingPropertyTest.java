package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity_;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntity;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntity_;
import com.thy.fss.common.inmemory.testmodel.Level1;
import com.thy.fss.common.inmemory.testmodel.Level1_;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderItem;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order_;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fix-checking property test — düzeltme SONRASI.
 *
 * <p><b>Property 1: Expected Behavior</b> — Düz + İdempotent + Tip-Koruyucu Atama (Fix Checking)</p>
 *
 * <p>Bug koşulunu ({@code isBugCondition(input)}) sağlayan tüm girişler için düzeltilmiş
 * fonksiyonun beklenen davranışı ürettiğini doğrular:</p>
 * <ul>
 *   <li>Düz (flat, iç içe olmadan) atama — rastgele koleksiyon içerikleri ve eleman-tipi varyansı</li>
 *   <li>İdempotentlik — N kez uygulama boyut/içeriği değiştirmez</li>
 *   <li>Somut tip korunması — {@code List}→{@code List}, {@code Set}→{@code Set}</li>
 *   <li>Tek/çok seviyeli yol ve ALL seçici yolu kapsanır</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b></p>
 */
class CollectionMappingNestingFixCheckingPropertyTest {

    private static final SpecificationService<Order> ORDER_SVC = OrderSpecificationService.INSTANCE;
    private static final SpecificationService<CollectionEntity> COLLECTION_SVC = CollectionEntitySpecificationService.INSTANCE;
    private static final SpecificationService<ComplexNestedEntity> COMPLEX_SVC = ComplexNestedEntitySpecificationService.INSTANCE;

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    // ==================== Providers ====================

    @Provide
    Arbitrary<List<OrderItem>> orderItems() {
        Arbitrary<OrderItem> itemArb = Arbitraries.longs().between(1, 10000)
                .map(id -> {
                    OrderItem item = new OrderItem();
                    item.setId(id);
                    item.setQuantity((int) (id % 10) + 1);
                    item.setUnitPrice(id * 1.5);
                    return item;
                });
        return itemArb.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<String>> stringTags() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<Set<Integer>> integerNumbers() {
        return Arbitraries.integers().between(1, 10000)
                .set().ofMinSize(1).ofMaxSize(5);
    }

    /** Element-type variance provider: Integer values for String-declared tags field */
    @Provide
    Arbitrary<List<Integer>> variantIntegersForTags() {
        return Arbitraries.integers().between(1, 1000)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    /** Element-type variance provider: unique Long values for Integer-declared numbers (Set) field */
    @Provide
    Arbitrary<Set<Long>> variantLongsForNumbers() {
        return Arbitraries.longs().between(1L, 10000L)
                .set().ofMinSize(1).ofMaxSize(5);
    }

    /** Element-type variance provider: Double values for String-declared tags field */
    @Provide
    Arbitrary<List<Double>> variantDoublesForTags() {
        return Arbitraries.doubles().between(0.1, 999.9)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    /** Arbitrary repetition count for idempotency testing */
    @Provide
    Arbitrary<Integer> repetitionCount() {
        return Arbitraries.integers().between(2, 5);
    }

    // ====================================================================================
    // 2.1 DÜZ (FLAT) ATAMA — Uyumlu eleman tipleri (compatible element types)
    // ====================================================================================

    /**
     * 2.1 — CollectionEntity.tags düz atama: Rastgele {@code List<String>} üretilir;
     * {@code setValueByPath} ile koleksiyon hedefe atanır; sonuç düz (flat) olmalıdır.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_shouldBeFlatAfterAssignment(
            @ForAll("stringTags") List<String> sourceTags) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        List<?> result = entity.getTags();
        assertThat(result)
                .as("tags should have same size as source (flat, not wrapped)")
                .hasSize(sourceTags.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be String, not a wrapped collection")
                    .isInstanceOf(String.class);
        }

        assertThat((List<String>) result).containsExactlyElementsOf(sourceTags);
    }

    /**
     * 2.1 — CollectionEntity.numbers düz atama: Rastgele {@code Set<Integer>} üretilir;
     * pre-initialized Set hedefe atanır; sonuç düz (flat) olmalıdır.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Property(tries = 100)
    void fixCheck_numbers_shouldBeFlatAfterAssignment(
            @ForAll("integerNumbers") Set<Integer> sourceNumbers) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setNumbers(new HashSet<>(Arrays.asList(999, 888)));

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);

        Set<?> result = entity.getNumbers();
        assertThat(result)
                .as("numbers should have same size as source (flat, not wrapped)")
                .hasSize(sourceNumbers.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be Integer, not a wrapped collection")
                    .isInstanceOf(Integer.class);
        }

        assertThat((Set<Integer>) result).containsExactlyInAnyOrderElementsOf(sourceNumbers);
    }

    /**
     * 2.1 — Order.items düz atama: Rastgele {@code List<OrderItem>} üretilir;
     * model koleksiyon hedefe atanır; sonuç düz olmalıdır.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Property(tries = 100)
    void fixCheck_orderItems_shouldBeFlatAfterAssignment(
            @ForAll("orderItems") List<OrderItem> sourceItems) {

        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());

        ORDER_SVC.setValueByPath(order, List.of(Order_.items), sourceItems);

        List<?> result = order.getItems();
        assertThat(result)
                .as("Order.items should be flat with same size as source")
                .hasSize(sourceItems.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be OrderItem, not a wrapped collection")
                    .isInstanceOf(OrderItem.class);
        }

        assertThat((List<OrderItem>) result).containsExactlyElementsOf(sourceItems);
    }

    // ====================================================================================
    // 2.1 DÜZ (FLAT) ATAMA — Eleman-tipi varyansı (Element-type variance)
    // ====================================================================================

    /**
     * 2.1 — Eleman-tipi varyansı (Integer → tags/String): Bildirilen eleman tipiyle
     * ({@code String}) uyumsuz olan {@code Integer} değerlerden oluşan bir grup üretilir
     * ve null hedefe atanır. Sonuç düz olmalı (iç içe değil).
     *
     * <p>Bu, bug condition'ın triggersNesting tetikleyicisini hedefler.</p>
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_elementTypeVariance_integer_shouldBeFlatNotNested(
            @ForAll("variantIntegersForTags") List<Integer> sourceIntegers) {

        // Given: NULL target (not pre-initialized) — triggers both nesting AND null-init paths
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Assign Integer values to String-declared tags field (element-type variance)
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);

        // Then: Flat assignment, no nesting
        List<?> result = entity.getTags();
        assertThat(result)
                .as("tags should be flat with %d elements despite element-type variance", sourceIntegers.size())
                .isNotNull()
                .hasSize(sourceIntegers.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be a flat value, not a nested Collection")
                    .isNotInstanceOf(Collection.class);
        }
    }

    /**
     * 2.1 — Eleman-tipi varyansı (Double → tags/String): {@code Double} değerler
     * String-declared tags alanına atanır.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_elementTypeVariance_double_shouldBeFlatNotNested(
            @ForAll("variantDoublesForTags") List<Double> sourceDoubles) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceDoubles);

        List<?> result = entity.getTags();
        assertThat(result)
                .as("tags should be flat with %d elements despite Double→String variance", sourceDoubles.size())
                .isNotNull()
                .hasSize(sourceDoubles.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be a flat value (Double), not a nested Collection")
                    .isNotInstanceOf(Collection.class);
        }
    }

    /**
     * 2.1 — Eleman-tipi varyansı (Long → numbers/Integer): {@code Long} değerler
     * Integer-declared numbers (Set) alanına null hedefle atanır.
     *
     * <p>Bu hem eleman-tipi varyansını hem de null-Set başlatma yolunu kapsar.</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_numbers_elementTypeVariance_long_shouldBeFlatNotNested(
            @ForAll("variantLongsForNumbers") Set<Long> sourceLongs) {

        // Given: NULL numbers target (Set<Integer> field, not pre-initialized)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Assign Long values to Integer-declared numbers (Set) field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceLongs);

        // Then: Flat assignment to a Set
        Set<?> result = entity.getNumbers();
        assertThat(result)
                .as("numbers should be populated (not null) after variant assignment to null Set target")
                .isNotNull()
                .hasSize(sourceLongs.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be a flat value (Long), not a nested Collection")
                    .isNotInstanceOf(Collection.class);
        }
    }

    // ====================================================================================
    // 2.2 İDEMPOTENTLİK — N kez uygulama boyut/içeriği değiştirmez
    // ====================================================================================

    /**
     * 2.2 — İdempotentlik (tags): Aynı atama N kez uygulanır;
     * boyut ve içerik sabit kalır (sınırsız büyüme yok).
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_shouldBeIdempotent(
            @ForAll("stringTags") List<String> sourceTags) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // Apply ONCE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        List<String> applyOnce = new ArrayList<>(entity.getTags());

        // Apply TWICE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        List<String> applyTwice = new ArrayList<>(entity.getTags());

        assertThat(applyTwice)
                .as("Idempotent: second application should produce same result as first")
                .isEqualTo(applyOnce);

        assertThat(applyTwice)
                .as("Size should remain equal to source size after any number of applications")
                .hasSize(sourceTags.size());
    }

    /**
     * 2.2 — İdempotentlik (numbers/Set): Set hedefinde N kez uygulamada idempotentlik.
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Property(tries = 100)
    void fixCheck_numbers_shouldBeIdempotent(
            @ForAll("integerNumbers") Set<Integer> sourceNumbers) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setNumbers(new HashSet<>());

        // Apply ONCE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);
        Set<Integer> applyOnce = new HashSet<>(entity.getNumbers());

        // Apply TWICE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);
        Set<Integer> applyTwice = new HashSet<>(entity.getNumbers());

        assertThat(applyTwice)
                .as("Idempotent: second application should produce same result as first")
                .isEqualTo(applyOnce);

        assertThat(applyTwice)
                .as("Size should remain equal to source size")
                .hasSize(sourceNumbers.size());
    }

    /**
     * 2.2 — İdempotentlik (Order.items): Model koleksiyonunda idempotentlik.
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Property(tries = 100)
    void fixCheck_orderItems_shouldBeIdempotent(
            @ForAll("orderItems") List<OrderItem> sourceItems) {

        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());

        // Apply ONCE
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), sourceItems);
        List<OrderItem> applyOnce = new ArrayList<>(order.getItems());

        // Apply TWICE
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), sourceItems);
        List<OrderItem> applyTwice = new ArrayList<>(order.getItems());

        assertThat(applyTwice)
                .as("Idempotent: Order.items after second application should equal first")
                .isEqualTo(applyOnce);

        assertThat(applyTwice)
                .as("Size should remain equal to source size")
                .hasSize(sourceItems.size());
    }

    /**
     * 2.2 — İdempotentlik + eleman-tipi varyansı: Varyant atama N kez uygulandığında
     * sonuç sabit kalmalı (büyümemeli).
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_elementTypeVariance_shouldBeIdempotent(
            @ForAll("variantIntegersForTags") List<Integer> sourceIntegers,
            @ForAll("repetitionCount") int n) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // Apply N times
        for (int i = 0; i < n; i++) {
            COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);
        }

        List<?> result = entity.getTags();
        assertThat(result)
                .as("After %d applications with element-type variance, size should equal source size (%d). "
                        + "Actual size: %d", n, sourceIntegers.size(), result.size())
                .hasSize(sourceIntegers.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be flat after %d applications", n)
                    .isNotInstanceOf(Collection.class);
        }
    }

    // ====================================================================================
    // 2.3 SOMUT TİP KORUNMASI — List→List, Set→Set (Concrete Type Preservation)
    // ====================================================================================

    /**
     * 2.3 — Somut tip korunması (tags/List): Atama sonrası {@code tags} alanı
     * hâlâ {@code List} tipinde olmalıdır.
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_concreteTypePreserved_shouldBeList(
            @ForAll("stringTags") List<String> sourceTags) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        assertThat(entity.getTags())
                .as("tags should be a List instance (concrete type preserved)")
                .isNotNull()
                .isInstanceOf(List.class);
    }

    /**
     * 2.3 — Somut tip korunması (numbers/Set): Atama sonrası {@code numbers} alanı
     * hâlâ {@code Set} tipinde olmalıdır. Pre-initialized Set hedefinde test eder.
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_numbers_concreteTypePreserved_shouldBeSet(
            @ForAll("integerNumbers") Set<Integer> sourceNumbers) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setNumbers(new HashSet<>());

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);

        assertThat(entity.getNumbers())
                .as("numbers should be a Set instance (concrete type preserved)")
                .isNotNull()
                .isInstanceOf(Set.class);
    }

    /**
     * 2.3 — Somut tip korunması (numbers/Set) + NULL hedef: Null {@code numbers}
     * (Set) alanına atama yapıldığında, alan {@code Set} tipinde başlatılmalı ve
     * doldurulmalıdır.
     *
     * <p>Bu, null-init'in tip-duyarlı yapıldığını doğrular (ArrayList değil, Set).</p>
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_numbers_nullTarget_concreteTypePreserved_shouldBeSet(
            @ForAll("integerNumbers") Set<Integer> sourceNumbers) {

        // Given: NULL numbers (not pre-initialized)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Assign to null Set field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);

        // Then: numbers should be populated as a Set (not null, not ArrayList)
        Set<?> result = entity.getNumbers();
        assertThat(result)
                .as("numbers should be populated after null-target assignment")
                .isNotNull();
        assertThat(result)
                .as("numbers should be a Set instance (type-aware null initialization)")
                .isInstanceOf(Set.class);
        assertThat(result)
                .as("numbers should contain all source elements (flat)")
                .hasSize(sourceNumbers.size())
                .containsAll((Iterable) sourceNumbers);
    }

    /**
     * 2.3 — Somut tip korunması (tags/List) + NULL hedef: Null {@code tags}
     * (List) alanına atama yapıldığında, alan {@code List} tipinde başlatılmalı.
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_tags_nullTarget_concreteTypePreserved_shouldBeList(
            @ForAll("stringTags") List<String> sourceTags) {

        // Given: NULL tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Assign to null List field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        // Then: tags should be populated as a List
        List<?> result = entity.getTags();
        assertThat(result)
                .as("tags should be populated after null-target assignment")
                .isNotNull();
        assertThat(result)
                .as("tags should be a List instance (type-aware null initialization)")
                .isInstanceOf(List.class);
        assertThat(result)
                .as("tags should contain all source elements")
                .hasSize(sourceTags.size());
    }

    // ====================================================================================
    // ÇOK SEVİYELİ YOL (Multi-level path)
    // ====================================================================================

    /**
     * 2.1+2.2 — Çok seviyeli hedef düz + idempotent: Son elemanı koleksiyon olan çok
     * seviyeli bir yola koleksiyon atanır; düz ve idempotent sonuç doğrulanır.
     * Path: {@code [ComplexNestedEntity_.level1, Level1_.items]}
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_multiLevelPath_shouldBeFlatAndIdempotent(
            @ForAll("stringTags") List<String> sourceItems) {

        ComplexNestedEntity root = new ComplexNestedEntity();
        root.setName("fix-check-" + ID_SEQ.getAndIncrement());
        Level1 level1 = new Level1();
        level1.setName("nested");
        root.setLevel1(level1);

        List<MetaAttribute<?, ?>> multiLevelPath = List.of(ComplexNestedEntity_.level1, Level1_.items);

        // Apply ONCE — flat
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);

        List<?> resultOnce = root.getLevel1().getItems();
        assertThat(resultOnce)
                .as("Multi-level target should be flat with same size as source")
                .hasSize(sourceItems.size());

        for (Object element : resultOnce) {
            assertThat(element)
                    .as("Each element should be String, not a wrapped collection")
                    .isInstanceOf(String.class);
        }

        assertThat((List<String>) resultOnce).containsExactlyElementsOf(sourceItems);

        List<String> applyOnce = new ArrayList<>((List<String>) resultOnce);

        // Apply TWICE — idempotent
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);
        List<?> resultTwice = root.getLevel1().getItems();
        List<String> applyTwice = new ArrayList<>((List<String>) resultTwice);

        assertThat(applyTwice)
                .as("Idempotent: multi-level target after second application should equal first")
                .isEqualTo(applyOnce);

        assertThat(applyTwice).hasSize(sourceItems.size());
    }

    /**
     * 2.1 — Çok seviyeli yol + eleman-tipi varyansı: Varyant değerlerle çok seviyeli
     * hedefte düz atama.
     *
     * <p><b>Validates: Requirements 2.1, 2.3</b></p>
     */
    @Property(tries = 100)
    void fixCheck_multiLevelPath_elementTypeVariance_shouldBeFlat(
            @ForAll("variantIntegersForTags") List<Integer> sourceIntegers) {

        ComplexNestedEntity root = new ComplexNestedEntity();
        root.setName("variant-multi-" + ID_SEQ.getAndIncrement());
        Level1 level1 = new Level1();
        level1.setName("nested");
        root.setLevel1(level1);

        List<MetaAttribute<?, ?>> multiLevelPath = List.of(ComplexNestedEntity_.level1, Level1_.items);

        // When: Assign Integer values to String-declared items (multi-level)
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceIntegers);

        // Then: Flat
        List<?> result = root.getLevel1().getItems();
        assertThat(result)
                .as("Multi-level target should be flat despite element-type variance")
                .isNotNull()
                .hasSize(sourceIntegers.size());

        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be flat value, not nested collection")
                    .isNotInstanceOf(Collection.class);
        }
    }

    // ====================================================================================
    // ALL SEÇİCİ YOLU (setValueByPathWithCollections)
    // ====================================================================================

    /**
     * 2.1+2.2 — ALL seçici yolu: {@code setValueByPathWithCollections} ile bütün bir
     * koleksiyon atanır; düz + idempotent sonuç doğrulanır.
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b></p>
     */
    @Property(tries = 100)
    @SuppressWarnings("unchecked")
    void fixCheck_allSelectorPath_shouldBeFlatAndIdempotent(
            @ForAll("stringTags") List<String> sourceTags) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("pre-existing")));

        CollectionOperationMetadata<CollectionEntity, String> allOp =
                new CollectionOperationMetadata<>(
                        0,
                        (CollectionAttribute<CollectionEntity, String>) (CollectionAttribute<?, ?>) CollectionEntity_.tags,
                        CollectionSelector.ALL,
                        null
                );

        // Apply ONCE
        COLLECTION_SVC.setValueByPathWithCollections(
                entity,
                List.of(CollectionEntity_.tags),
                List.of(allOp),
                sourceTags
        );

        List<?> resultOnce = entity.getTags();
        for (Object element : resultOnce) {
            assertThat(element)
                    .as("Each element should be String after ALL selector assignment")
                    .isInstanceOf(String.class);
        }

        List<String> applyOnce = new ArrayList<>((List<String>) resultOnce);

        // Apply TWICE — idempotent
        COLLECTION_SVC.setValueByPathWithCollections(
                entity,
                List.of(CollectionEntity_.tags),
                List.of(allOp),
                sourceTags
        );

        List<?> resultTwice = entity.getTags();
        List<String> applyTwice = new ArrayList<>((List<String>) resultTwice);

        assertThat(applyTwice)
                .as("Idempotent: ALL selector path after re-application should not grow")
                .isEqualTo(applyOnce);

        for (Object element : resultTwice) {
            assertThat(element)
                    .as("Each element should remain String after second application")
                    .isInstanceOf(String.class);
        }
    }

    /**
     * 2.1 — ALL seçici yolu + eleman-tipi varyansı: Varyant değerlerle ALL selector
     * kullanarak düz atama.
     *
     * <p><b>Validates: Requirements 2.1, 2.3</b></p>
     */
    @Property(tries = 100)
    @SuppressWarnings("unchecked")
    void fixCheck_allSelectorPath_elementTypeVariance_shouldBeFlat(
            @ForAll("variantIntegersForTags") List<Integer> sourceIntegers) {

        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("initial")));

        CollectionOperationMetadata<CollectionEntity, String> allOp =
                new CollectionOperationMetadata<>(
                        0,
                        (CollectionAttribute<CollectionEntity, String>) (CollectionAttribute<?, ?>) CollectionEntity_.tags,
                        CollectionSelector.ALL,
                        null
                );

        // When: Assign variant Integer values via ALL selector path
        COLLECTION_SVC.setValueByPathWithCollections(
                entity,
                List.of(CollectionEntity_.tags),
                List.of(allOp),
                sourceIntegers
        );

        // Then: Flat, no nesting
        List<?> result = entity.getTags();
        for (Object element : result) {
            assertThat(element)
                    .as("Each element should be flat (Integer), not nested collection. Actual: %s", result)
                    .isNotInstanceOf(Collection.class);
        }
    }
}
