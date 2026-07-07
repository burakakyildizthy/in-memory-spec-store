package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fault-condition (exploration) property test — düzeltme ÖNCESİ.
 *
 * <p><b>Property 1: Bug Condition</b> — Koleksiyondan Koleksiyona Düz ve İdempotent Atama</p>
 *
 * <p><b>KRİTİK:</b> Bu test DÜZELTİLMEMİŞ kodda BAŞARISIZ olmalıdır — başarısızlık hatanın
 * var olduğunu kanıtlar.</p>
 *
 * <p><b>AMAÇ:</b> Hatayı gösteren karşı örnekleri ({@code [[...]]} iç içe sarmalama) yüzeye
 * çıkarmak.</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3</b></p>
 */
class CollectionMappingNestingFaultConditionPropertyTest {

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
                    item.setQuantity(1);
                    item.setUnitPrice(10.0);
                    return item;
                });
        return itemArb.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<String>> stringTags() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ==================== Senaryo A: Order.items (model koleksiyonu) ====================

    /**
     * Senaryo A — Order.items (1.1): Rastgele {@code List<OrderItem>} üret;
     * {@code setValueByPath(order, [Order_.items], sourceItems)} çağır;
     * {@code order.getItems()} düz {@code [i1, i2]} assert et.
     *
     * <p>Düzeltilmemişte beklenen başarısızlık: {@code items == [[i1, i2]]} (iç içe)</p>
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 50)
    void scenarioA_orderItems_shouldBeFlatAfterAssignment(
            @ForAll("orderItems") List<OrderItem> sourceItems) {

        // Given: A fresh order with no items
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());

        // When: Assign a collection to the collection field via setValueByPath
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), sourceItems);

        // Then: The target collection should contain the source elements FLAT
        List<?> resultItems = order.getItems();
        assertThat(resultItems)
                .as("Order.items should be a flat list of OrderItem, not nested [[...]]")
                .hasSize(sourceItems.size());

        // Each element should be an OrderItem, not a List
        for (Object element : resultItems) {
            assertThat(element)
                    .as("Each element should be OrderItem, not a wrapped collection")
                    .isInstanceOf(OrderItem.class);
        }

        // Content should match source exactly
        assertThat((List<OrderItem>) resultItems).containsExactlyElementsOf(sourceItems);
    }

    // ==================== Senaryo B: CollectionEntity.tags (List<String>) ====================

    /**
     * Senaryo B — CollectionEntity.tags (1.1): Rastgele {@code List<String>} üret;
     * {@code setValueByPath(entity, [CollectionEntity_.tags], sourceTags)} çağır;
     * {@code tags} düz assert et.
     *
     * <p>Düzeltilmemişte beklenen başarısızlık: {@code tags == [["a","b"]]} (iç içe)</p>
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 50)
    void scenarioB_collectionEntityTags_shouldBeFlatAfterAssignment(
            @ForAll("stringTags") List<String> sourceTags) {

        // Given: A fresh entity with no tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Assign a collection to the collection field via setValueByPath
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        // Then: The target collection should contain the source elements FLAT
        List<?> resultTags = entity.getTags();
        assertThat(resultTags)
                .as("CollectionEntity.tags should be flat, not nested [[...]]")
                .hasSize(sourceTags.size());

        // Each element should be a String, not a List
        for (Object element : resultTags) {
            assertThat(element)
                    .as("Each element should be String, not a wrapped collection")
                    .isInstanceOf(String.class);
        }

        // Content should match source exactly
        assertThat((List<String>) resultTags).containsExactlyElementsOf(sourceTags);
    }

    // ==================== Senaryo C: Çok seviyeli hedef (multi-level path) ====================

    /**
     * Senaryo C — Çok seviyeli hedef (1.3): Son elemanı koleksiyon olan çok seviyeli
     * bir yola koleksiyon atanır. Path: {@code [ComplexNestedEntity_.level1, Level1_.items]}
     *
     * <p>Düzeltilmemişte beklenen başarısızlık: Tek seviyeli workaround bu yolu
     * kapsamadığı için iç içe sarmalama olur.</p>
     *
     * <p><b>Validates: Requirements 1.3</b></p>
     */
    @Property(tries = 50)
    void scenarioC_multiLevelPath_shouldBeFlatAfterAssignment(
            @ForAll("stringTags") List<String> sourceItems) {

        // Given: A ComplexNestedEntity with level1 already set (so navigation works)
        ComplexNestedEntity root = new ComplexNestedEntity();
        root.setName("test-root");
        Level1 level1 = new Level1();
        level1.setName("nested");
        root.setLevel1(level1);

        // When: Assign a collection to a multi-level path ending in a collection
        // Path: root -> level1 -> items (List<String>)
        List<MetaAttribute<?, ?>> multiLevelPath = List.of(ComplexNestedEntity_.level1, Level1_.items);
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);

        // Then: The nested collection should contain the source elements FLAT
        List<?> resultItems = root.getLevel1().getItems();
        assertThat(resultItems)
                .as("Multi-level target items should be flat, not nested [[...]]")
                .hasSize(sourceItems.size());

        for (Object element : resultItems) {
            assertThat(element)
                    .as("Each element should be String, not a wrapped collection")
                    .isInstanceOf(String.class);
        }

        assertThat((List<String>) resultItems).containsExactlyElementsOf(sourceItems);
    }

    // ==================== Senaryo D: İdempotentlik ====================

    /**
     * Senaryo D — İdempotentlik (1.2): Aynı koleksiyondan koleksiyona atama aynı hedefe
     * iki kez uygulanır; hedef koleksiyon boyutunun sabit kaldığı assert et.
     *
     * <p>Düzeltilmemişte beklenen başarısızlık: Her uygulamada koleksiyon büyür
     * ({@code [["a","b"], ["a","b"]]}).</p>
     *
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 50)
    void scenarioD_idempotency_collectionSizeShouldRemainConstant(
            @ForAll("stringTags") List<String> sourceTags) {

        // Given: A fresh entity
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Apply the same collection-to-collection assignment TWICE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        int sizeAfterFirst = entity.getTags().size();

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        int sizeAfterSecond = entity.getTags().size();

        // Then: Size should remain constant (idempotent)
        assertThat(sizeAfterSecond)
                .as("Collection size should remain constant after re-application (idempotent). "
                        + "First: %d, Second: %d. Tags after second: %s",
                        sizeAfterFirst, sizeAfterSecond, entity.getTags())
                .isEqualTo(sizeAfterFirst);

        // Additionally, the size should equal the source size (not grow)
        assertThat(sizeAfterSecond)
                .as("Collection should have same size as source after any number of applications")
                .isEqualTo(sourceTags.size());
    }

    // ==================== Senaryo E: ALL seçici yolu (addValueToCollection) ====================

    /**
     * Senaryo E — ALL seçici yolu: {@code setValueByPathWithCollections} ile son indekste
     * ALL seçicili {@code CollectionOperationMetadata} kullanılarak bütün bir koleksiyon
     * atanır; düz sonuç assert et.
     *
     * <p>Düzeltilmemişte beklenen başarısızlık: {@code addValueToCollection} her zaman
     * {@code add(value)} yapıyor, iç içe sarmalama olur.</p>
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 50)
    @SuppressWarnings("unchecked")
    void scenarioE_allSelectorPath_shouldBeFlatAfterAssignment(
            @ForAll("stringTags") List<String> sourceTags) {

        // Given: An entity with an existing (possibly empty) tags collection
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("existing")));

        // When: Use setValueByPathWithCollections with ALL selector on the last path element
        CollectionOperationMetadata<CollectionEntity, String> allOp =
                new CollectionOperationMetadata<>(
                        0,  // pathIndex: 0 (the only element in path)
                        (CollectionAttribute<CollectionEntity, String>) (CollectionAttribute<?, ?>) CollectionEntity_.tags,
                        CollectionSelector.ALL,
                        null  // no specification filter
                );

        COLLECTION_SVC.setValueByPathWithCollections(
                entity,
                List.of(CollectionEntity_.tags),
                List.of(allOp),
                sourceTags
        );

        // Then: The target collection should contain the source elements FLAT
        List<?> resultTags = entity.getTags();

        // With ALL selector, after assigning a full collection, elements should be flat
        // Check that no element is itself a List (which would indicate nesting)
        for (Object element : resultTags) {
            assertThat(element)
                    .as("Each element in tags should be String, not a nested collection. "
                            + "Actual tags: %s", resultTags)
                    .isInstanceOf(String.class);
        }
    }

    // ==================== Concrete Examples ====================

    /**
     * Somut örnek: {@code tags = ["a", "b"]} atandığında sonuç düz olmalı.
     * Düzeltilmemişte: {@code [["a", "b"]]} — FAIL.
     */
    @Example
    void concreteExample_tagsAssignment_shouldBeFlat() {
        CollectionEntity entity = new CollectionEntity();
        entity.setId(1L);

        List<String> source = List.of("a", "b");
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);

        assertThat(entity.getTags())
                .as("tags should be [a, b], not [[a, b]]")
                .hasSize(2)
                .containsExactly("a", "b");

        // Verify no nesting: first element should be String, not List
        assertThat(entity.getTags().get(0))
                .isInstanceOf(String.class);
    }

    /**
     * Somut örnek: Tekrarlanan atamada koleksiyon büyümemeli.
     * Düzeltilmemişte: İlk = {@code [["a","b"]]}, İkinci = {@code [["a","b"],["a","b"]]} — FAIL.
     */
    @Example
    void concreteExample_repeatedAssignment_shouldNotGrow() {
        CollectionEntity entity = new CollectionEntity();
        entity.setId(2L);

        List<String> source = List.of("a", "b");

        // First application
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);
        int sizeAfterFirst = entity.getTags().size();

        // Second application
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);
        int sizeAfterSecond = entity.getTags().size();

        assertThat(sizeAfterSecond)
                .as("After second application, size should equal source size (2), not grow. "
                        + "Actual: %s", entity.getTags())
                .isEqualTo(2)
                .isEqualTo(sizeAfterFirst);
    }

    // ==================================================================================
    // BUG CONDITION EXPLORATION — Element-Type Variance + Null Target Scenarios
    // ==================================================================================
    //
    // Aşağıdaki senaryolar, mevcut 42 testin maskelediği regresyon tetikleyicilerini
    // açıkça hedefler:
    //   - NULL hedef koleksiyon (önceden başlatılmamış)
    //   - Eleman tipi varyansı (bildirilen elementType ile uyumsuz çalışma-zamanı tipleri)
    //
    // Bu senaryolar DÜZELTİLMEMİŞ kodda BAŞARISIZ olmalıdır.
    // ==================================================================================

    // ==================== Providers (Element-Type Variance) ====================

    @Provide
    Arbitrary<List<Integer>> integerValues() {
        return Arbitraries.integers().between(1, 1000)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ==================== Senaryo (a) — tags null + eleman-tipi varyansı → iç içe (1.1) ====================

    /**
     * Senaryo (a) — {@code tags} ({@code List<String>}) null hedef + eleman-tipi varyansı → iç içe (1.1).
     *
     * <p>Bildirilen eleman tipiyle ({@code String}) birebir atanamayan değerlerden oluşan bir grup
     * üretilir (örn. {@code List.of(7, 8)} — {@code Integer} değerler) ve önceden başlatılmamış
     * (null) {@code tags} alanına atanır. Düz {@code [7, 8]} beklenir.</p>
     *
     * <p><b>DÜZELTİLMEMİŞ karşı örnek:</b> {@code allElementsAssignable} {@code false} döner →
     * {@code else} dalı → {@code collection.add(source)} → iç içe {@code [[7, 8]]}
     * (tek elemanlı, ilk eleman bir {@code List}).</p>
     *
     * <p><b>Validates: Requirements 1.1</b></p>
     */
    @Property(tries = 50)
    void scenarioA_variant_tagsNullTarget_elementTypeVariance_shouldBeFlatNotNested(
            @ForAll("integerValues") List<Integer> sourceIntegers) {

        // Given: A fresh entity with NULL tags (not pre-initialized)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        // tags is null — NOT pre-initialized

        // When: Assign a collection of Integer values to tags (List<String>) field
        // This triggers element-type variance: Integer is NOT assignable to String
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);

        // Then: The target collection should contain the source elements FLAT (not nested)
        List<?> resultTags = entity.getTags();
        assertThat(resultTags)
                .as("tags should be flat with %d elements, not nested [[...]]. Actual: %s",
                        sourceIntegers.size(), resultTags)
                .isNotNull()
                .hasSize(sourceIntegers.size());

        // Each element should be a value from source, NOT a wrapped collection
        for (Object element : resultTags) {
            assertThat(element)
                    .as("Each element should be a flat value (Integer), not a nested List. "
                            + "Element type: %s, Actual tags: %s",
                            element != null ? element.getClass().getName() : "null", resultTags)
                    .isNotInstanceOf(Collection.class);
        }
    }

    /**
     * Somut deterministik örnek — Senaryo (a): {@code List.of(7, 8)} → null tags.
     * Düzeltilmemişte: {@code tags == [[7, 8]]} (FAIL).
     */
    @Example
    void concreteExample_scenarioA_integerListToNullTags_shouldBeFlat() {
        CollectionEntity entity = new CollectionEntity();
        entity.setId(100L);
        // tags is null

        List<Integer> source = List.of(7, 8);
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);

        List<?> result = entity.getTags();
        assertThat(result)
                .as("tags should be flat [7, 8], not nested [[7, 8]]. Actual: %s", result)
                .isNotNull()
                .hasSize(2);

        // First element should be Integer(7), not a List
        assertThat(result.get(0))
                .as("First element should be 7 (Integer), not [7, 8] (List). Actual type: %s",
                        result.get(0) != null ? result.get(0).getClass().getName() : "null")
                .isNotInstanceOf(Collection.class);
    }

    // ==================== Senaryo (b) — Tekrarlı uygulama → sınırsız büyüme (1.2) ====================

    /**
     * Senaryo (b) — Tekrarlı uygulama → sınırsız büyüme (1.2).
     *
     * <p>(a)'daki atama (eleman-tipi varyansı ile) aynı hedefe 2-3 kez uygulanır;
     * hedef boyutunun kaynak boyutuyla sabit kalması beklenir (idempotent).</p>
     *
     * <p><b>DÜZELTİLMEMİŞ karşı örnek:</b> her uygulamada {@code add} yeni bir iç içe eleman ekler →
     * {@code [[7,8]]} → {@code [[7,8],[7,8]]} → … (büyüme).</p>
     *
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 50)
    void scenarioB_variant_repeatedApplication_elementTypeVariance_shouldBeIdempotent(
            @ForAll("integerValues") List<Integer> sourceIntegers) {

        // Given: A fresh entity with NULL tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());

        // When: Apply same variant assignment 3 times
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);
        int sizeAfterFirst = entity.getTags().size();

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);
        int sizeAfterSecond = entity.getTags().size();

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceIntegers);
        int sizeAfterThird = entity.getTags().size();

        // Then: Size should remain constant (idempotent) — equal to source size
        assertThat(sizeAfterFirst)
                .as("After first application, size should equal source size (%d). Actual tags: %s",
                        sourceIntegers.size(), entity.getTags())
                .isEqualTo(sourceIntegers.size());

        assertThat(sizeAfterSecond)
                .as("After second application, size should remain %d (idempotent). "
                        + "Actual size: %d, tags: %s",
                        sourceIntegers.size(), sizeAfterSecond, entity.getTags())
                .isEqualTo(sourceIntegers.size());

        assertThat(sizeAfterThird)
                .as("After third application, size should remain %d (idempotent, no unbounded growth). "
                        + "Actual size: %d, tags: %s",
                        sourceIntegers.size(), sizeAfterThird, entity.getTags())
                .isEqualTo(sourceIntegers.size());
    }

    /**
     * Somut deterministik örnek — Senaryo (b): Tekrarlı atama büyümemeli.
     * Düzeltilmemişte: {@code [[7,8]]} → {@code [[7,8],[7,8]]} → … (FAIL).
     */
    @Example
    void concreteExample_scenarioB_repeatedVariantAssignment_shouldNotGrow() {
        CollectionEntity entity = new CollectionEntity();
        entity.setId(101L);

        List<Integer> source = List.of(7, 8);

        // First
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);
        int firstSize = entity.getTags().size();

        // Second
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), source);
        int secondSize = entity.getTags().size();

        assertThat(secondSize)
                .as("Repeated application should be idempotent. First size: %d, Second size: %d. "
                        + "Tags after second: %s", firstSize, secondSize, entity.getTags())
                .isEqualTo(firstSize)
                .isEqualTo(source.size());
    }

    // ==================== Senaryo (c) — numbers (Set<Integer>) null → hiç doldurulmama (1.3, 2.3) ====================

    /**
     * Senaryo (c) — {@code numbers} ({@code Set<Integer>}) null hedef → hiç doldurulmama (1.3, 2.3).
     *
     * <p>Bir grup {@code Integer} değer, önceden başlatılmamış (null) {@code numbers}
     * ({@code Set<Integer>}) alanına atanır. Alanın düz doldurulmuş ve {@code Set} tipinde
     * olması beklenir.</p>
     *
     * <p><b>DÜZELTİLMEMİŞ karşı örnek:</b> null init koşulsuz {@code new ArrayList<>()} üretir;
     * üretilen setter {@code (java.util.Set) value} cast'i ile {@code ClassCastException} fırlatır
     * → alan atanmamış/{@code null} kalır (ya da CCE yayılır).</p>
     *
     * <p><b>Validates: Requirements 1.3, 2.3</b></p>
     */
    @Property(tries = 50)
    void scenarioC_variant_numbersNullTarget_shouldBePopulatedAsSet(
            @ForAll("integerValues") List<Integer> sourceIntegers) {

        // Given: A fresh entity with NULL numbers (Set<Integer> field, not pre-initialized)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        // numbers is null — NOT pre-initialized

        // When: Assign a collection of Integer values to numbers (Set<Integer>) field
        // On UNFIXED code: new ArrayList<>() is cast to (java.util.Set) → ClassCastException
        // We catch the exception to verify the bug: field remains null
        try {
            COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceIntegers);
        } catch (ClassCastException e) {
            // UNFIXED behavior: CCE is thrown (or swallowed upstream).
            // In either case, numbers should be populated after the fix.
        }

        // Then: The numbers field should be populated (not null) and be a Set
        Set<?> resultNumbers = entity.getNumbers();
        assertThat(resultNumbers)
                .as("numbers (Set<Integer>) should be populated after assignment, not null. "
                        + "On UNFIXED code: new ArrayList<>() cast to Set throws ClassCastException "
                        + "→ field stays null.")
                .isNotNull();

        // Should be a Set (concrete type preserved)
        assertThat(resultNumbers)
                .as("numbers should be an instance of Set (concrete type preserved)")
                .isInstanceOf(Set.class);

        // Should contain the source elements (flat)
        assertThat(resultNumbers)
                .as("numbers should contain all source integers (flat). "
                        + "Source: %s, Result: %s", sourceIntegers, resultNumbers)
                .containsAll((Iterable) sourceIntegers);
    }

    /**
     * Somut deterministik örnek — Senaryo (c): {@code List.of(1, 2, 3)} → null numbers (Set).
     * Düzeltilmemişte: {@code numbers == null} (veya CCE fırlatır) (FAIL).
     */
    @Example
    void concreteExample_scenarioC_integerListToNullNumbers_shouldPopulateAsSet() {
        CollectionEntity entity = new CollectionEntity();
        entity.setId(102L);
        // numbers (Set<Integer>) is null

        List<Integer> source = List.of(1, 2, 3);

        // On UNFIXED code: ClassCastException from casting ArrayList to Set
        try {
            COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), source);
        } catch (ClassCastException e) {
            // Expected on UNFIXED code — swallowed to verify field state
        }

        Set<?> result = entity.getNumbers();
        assertThat(result)
                .as("numbers should be populated as a Set with [1, 2, 3], not null. "
                        + "UNFIXED: ClassCastException from new ArrayList<>() cast to java.util.Set")
                .isNotNull()
                .hasSize(3);

        assertThat(result)
                .as("numbers should be a Set instance")
                .isInstanceOf(Set.class);
    }
}
