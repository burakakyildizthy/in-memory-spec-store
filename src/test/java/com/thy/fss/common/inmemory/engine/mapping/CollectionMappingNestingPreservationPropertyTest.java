package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity_;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntity;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.ComplexNestedEntity_;
import com.thy.fss.common.inmemory.testmodel.Level1;
import com.thy.fss.common.inmemory.testmodel.Level1_;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntity;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntity_;
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
 * Preservation (koruma) property testleri — düzeltme ÖNCESİ.
 *
 * <p><b>Property 2: Preservation</b> — Doğrudan Atama Kapsamı Dışındaki Davranışların Korunması</p>
 *
 * <p><b>ÖNEMLİ:</b> Bu testler DÜZELTİLMEMİŞ kodda GEÇMELİDİR — korunacak baseline davranışı
 * doğrular.</p>
 *
 * <p>Gözlemlenen ve testlerle sabitlenen davranışlar (nihai gereksinim numaralandırması 3.1–3.7):</p>
 * <ul>
 *   <li>3.1 Uyumlu düz atama (tek seviyeli): eleman tipleri hedefle uyumlu grup düz atanır</li>
 *   <li>3.2 Uyumlu düz atama (çok seviyeli yol): iç içe nesne üzerinden erişilen koleksiyon hedefe uyumlu grup düz atanır</li>
 *   <li>3.3 Tek eleman ekleme (append): tek bir skaler değer koleksiyona eklenir</li>
 *   <li>3.4 Skaler hedef: koleksiyon olmayan alana değer doğrudan atanır</li>
 *   <li>3.5 Agregasyon: sayısal sonuç skaler hedefe değiştirilmeden yazılır</li>
 *   <li>3.6 Meşru iç içe: List&lt;List&lt;String&gt;&gt; hedefine tek iç liste eklenir → [[...]]</li>
 *   <li>3.7 Null-List başlatma: null alan önce initialize edilir sonra doldurulur</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7</b></p>
 */
class CollectionMappingNestingPreservationPropertyTest {

    private static final SpecificationService<Order> ORDER_SVC = OrderSpecificationService.INSTANCE;
    private static final SpecificationService<CollectionEntity> COLLECTION_SVC = CollectionEntitySpecificationService.INSTANCE;
    private static final SpecificationService<NestedCollectionEntity> NESTED_SVC = NestedCollectionEntitySpecificationService.INSTANCE;
    private static final SpecificationService<ComplexNestedEntity> COMPLEX_SVC = ComplexNestedEntitySpecificationService.INSTANCE;

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    // ==================== Providers ====================

    @Provide
    Arbitrary<String> singleTag() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
    }

    @Provide
    Arbitrary<Integer> singleNumber() {
        return Arbitraries.integers().between(1, 10000);
    }

    @Provide
    Arbitrary<String> scalarStringValue() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    @Provide
    Arbitrary<Double> scalarDoubleValue() {
        return Arbitraries.doubles().between(0.01, 999999.99);
    }

    @Provide
    Arbitrary<List<String>> innerList() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
                .list().ofMinSize(1).ofMaxSize(4);
    }

    @Provide
    Arbitrary<OrderItem> singleOrderItem() {
        return Arbitraries.longs().between(1, 10000).map(id -> {
            OrderItem item = new OrderItem();
            item.setId(id);
            item.setQuantity(1);
            item.setUnitPrice(10.0);
            return item;
        });
    }

    @Provide
    Arbitrary<List<String>> compatibleStringList() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ==================== 3.1 Uyumlu düz atama — tek seviyeli (compatible flat assignment) ====================

    /**
     * 3.1 — Uyumlu düz atama (tek seviyeli): Eleman tipleri hedefin bildirilen eleman tipiyle
     * zaten uyumlu olan bir grup (örn. {@code List<String>} → {@code List<String>} alanı) düz
     * atanır; sonuç düz kalır.
     *
     * <p>Bu, düzeltilmemiş kodun doğru çalışan durumudur: {@code allElementsAssignable} {@code true}
     * döner → C dalı → {@code clear()} + {@code addAll()} → düz + idempotent atama.</p>
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 50)
    void property3_1_compatibleFlatAssignment_singleLevel_tags(
            @ForAll("compatibleStringList") List<String> sourceTags) {

        // Given: An entity with pre-initialized tags (so we test the flat assign path, not null init)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("old1", "old2")));

        // When: Assign a compatible List<String> to tags (List<String>) field
        // Source element types (String) ARE assignable to declared elementType (String)
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        // Then: Result should be FLAT — contains exactly source elements (clear+addAll behavior)
        assertThat(entity.getTags())
                .as("Compatible flat assignment: tags should be flat with source elements. "
                        + "Source: %s, Result: %s", sourceTags, entity.getTags())
                .isNotNull()
                .hasSize(sourceTags.size())
                .containsExactlyElementsOf(sourceTags);

        // Each element should be a String (not a nested collection)
        for (Object element : entity.getTags()) {
            assertThat(element)
                    .as("Each element should be String, not a nested structure")
                    .isInstanceOf(String.class);
        }
    }

    /**
     * 3.1 — Uyumlu düz atama (tek seviyeli, idempotent): Aynı uyumlu koleksiyon iki kez
     * atanır; boyut sabit kalır (idempotent).
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 50)
    void property3_1_compatibleFlatAssignment_idempotent(
            @ForAll("compatibleStringList") List<String> sourceTags) {

        // Given: An entity with pre-initialized tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("old")));

        // When: Apply the same compatible assignment TWICE
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        int sizeAfterFirst = entity.getTags().size();

        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);
        int sizeAfterSecond = entity.getTags().size();

        // Then: Size should remain constant (idempotent — clear+addAll)
        assertThat(sizeAfterSecond)
                .as("Compatible flat assignment should be idempotent. Size after first: %d, second: %d",
                        sizeAfterFirst, sizeAfterSecond)
                .isEqualTo(sizeAfterFirst)
                .isEqualTo(sourceTags.size());
    }

    // ==================== 3.2 Uyumlu düz atama — çok seviyeli yol (multi-level path) ====================

    /**
     * 3.2 — Uyumlu düz atama (çok seviyeli yol): İç içe nesne üzerinden erişilen koleksiyon
     * hedefe ({@code [ComplexNestedEntity_.level1, Level1_.items]}) uyumlu grup atanır;
     * tek seviyeli durumla tutarlı biçimde düz kalır.
     *
     * <p>Çok seviyeli yol, son elemanı koleksiyon tipinde olan bir yolda {@code assignToCollectionField}
     * aynı discriminator mantığını uygular. Uyumlu elemanlar için düz atama korunur.</p>
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void property3_2_compatibleFlatAssignment_multiLevelPath(
            @ForAll("compatibleStringList") List<String> sourceItems) {

        // Given: A ComplexNestedEntity with level1 and pre-initialized items
        ComplexNestedEntity root = new ComplexNestedEntity();
        root.setName("test-" + ID_SEQ.getAndIncrement());
        Level1 level1 = new Level1();
        level1.setName("nested");
        level1.setItems(new ArrayList<>(Arrays.asList("existing1", "existing2")));
        root.setLevel1(level1);

        // When: Assign a compatible List<String> via multi-level path
        List<MetaAttribute<?, ?>> multiLevelPath = List.of(ComplexNestedEntity_.level1, Level1_.items);
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);

        // Then: Result should be FLAT — consistent with single-level behavior
        List<?> resultItems = root.getLevel1().getItems();
        assertThat(resultItems)
                .as("Multi-level compatible flat assignment: items should be flat. "
                        + "Source: %s, Result: %s", sourceItems, resultItems)
                .isNotNull()
                .hasSize(sourceItems.size());

        // Each element should be a String, not nested
        for (Object element : resultItems) {
            assertThat(element)
                    .as("Each element should be String, not a nested collection")
                    .isInstanceOf(String.class);
        }

        assertThat((List<String>) resultItems)
                .as("Multi-level path: content should match source exactly")
                .containsExactlyElementsOf(sourceItems);
    }

    /**
     * 3.2 — Uyumlu düz atama (çok seviyeli yol, idempotent): Aynı uyumlu koleksiyon çok
     * seviyeli yol üzerinden iki kez atanır; boyut sabit kalır.
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Property(tries = 50)
    void property3_2_compatibleFlatAssignment_multiLevelPath_idempotent(
            @ForAll("compatibleStringList") List<String> sourceItems) {

        // Given: A ComplexNestedEntity with level1 and pre-initialized items
        ComplexNestedEntity root = new ComplexNestedEntity();
        root.setName("test-" + ID_SEQ.getAndIncrement());
        Level1 level1 = new Level1();
        level1.setName("nested");
        level1.setItems(new ArrayList<>(Arrays.asList("old")));
        root.setLevel1(level1);

        List<MetaAttribute<?, ?>> multiLevelPath = List.of(ComplexNestedEntity_.level1, Level1_.items);

        // When: Apply the same compatible assignment TWICE via multi-level path
        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);
        int sizeAfterFirst = root.getLevel1().getItems().size();

        COMPLEX_SVC.setValueByPath(root, multiLevelPath, sourceItems);
        int sizeAfterSecond = root.getLevel1().getItems().size();

        // Then: Size should remain constant (idempotent)
        assertThat(sizeAfterSecond)
                .as("Multi-level compatible flat assignment should be idempotent. "
                        + "Size after first: %d, second: %d", sizeAfterFirst, sizeAfterSecond)
                .isEqualTo(sizeAfterFirst)
                .isEqualTo(sourceItems.size());
    }

    // ==================== 3.3 Tek eleman ekleme (single element append) ====================

    /**
     * 3.3 — Tek eleman ekleme (List&lt;String&gt; tags): Rastgele tek bir String değer
     * {@code setValueByPath} ile koleksiyon hedefine atanır; eleman koleksiyona EKLENİR.
     *
     * <p>Bu, düzeltilmemiş kodun doğru davranışıdır: {@code handleCollectionField} her zaman
     * {@code collection.add(elementToAdd)} yapar. Tek bir skaler değer (koleksiyon olmayan)
     * için bu doğrudur ve korunmalıdır.</p>
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 50)
    void property3_3_singleElementAppend_tagsField(
            @ForAll("singleTag") String newTag) {

        // Given: An entity with existing tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        entity.setTags(new ArrayList<>(Arrays.asList("existing1", "existing2")));
        int initialSize = entity.getTags().size();

        // When: Assign a SINGLE scalar value (not a collection) to the collection field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), newTag);

        // Then: The single element should be ADDED (append) to the existing collection
        assertThat(entity.getTags())
                .as("Single scalar value should be appended to collection, size should grow by 1")
                .hasSize(initialSize + 1);

        // The new element should be the last one
        assertThat(entity.getTags().get(entity.getTags().size() - 1))
                .as("Last element should be the newly added tag")
                .isEqualTo(newTag);

        // Previous elements should still be there
        assertThat(entity.getTags())
                .as("Original elements should be preserved")
                .contains("existing1", "existing2");
    }

    /**
     * 3.3 — Tek eleman ekleme (Order.items — model koleksiyon): Tek bir OrderItem
     * koleksiyona eklenir. FIRST/LAST/ANY ile seçilmiş tek eleman senaryosunu simüle eder.
     *
     * <p><b>Validates: Requirements 3.3</b></p>
     */
    @Property(tries = 50)
    void property3_3_singleElementAppend_orderItems(
            @ForAll("singleOrderItem") OrderItem newItem) {

        // Given: An order with existing items
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());
        OrderItem existing = new OrderItem();
        existing.setId(999L);
        existing.setQuantity(2);
        existing.setUnitPrice(20.0);
        order.setItems(new ArrayList<>(List.of(existing)));
        int initialSize = order.getItems().size();

        // When: Assign a SINGLE OrderItem (not a list) to the collection field
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), newItem);

        // Then: The single item should be ADDED (append) to the existing collection
        assertThat(order.getItems())
                .as("Single OrderItem should be appended, size grows by 1")
                .hasSize(initialSize + 1);

        // The new item should be the last one
        assertThat(order.getItems().get(order.getItems().size() - 1))
                .as("Last item should be the newly added OrderItem")
                .isInstanceOf(OrderItem.class)
                .isSameAs(newItem);

        // Original item should still be there
        assertThat(order.getItems().get(0))
                .as("First item should be the existing one")
                .isSameAs(existing);
    }

    // ==================== 3.4 Skaler hedef (scalar target assignment) ====================

    /**
     * 3.4 — Skaler hedef (String): Koleksiyon olmayan {@code Order.status} alanına
     * çözümlenen değer doğrudan atanır.
     *
     * <p>Bu path koleksiyon değil, {@code AttributeType.SINGLE} olduğundan
     * {@code handleCollectionField} çağrılmaz; {@code setFieldValue} doğrudan kullanılır.</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 50)
    void property3_4_scalarTarget_stringField(
            @ForAll("scalarStringValue") String statusValue) {

        // Given: An order with some status
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());
        order.setStatus("INITIAL");

        // When: Assign a scalar value to a non-collection field
        ORDER_SVC.setValueByPath(order, List.of(Order_.status), statusValue);

        // Then: The value should be directly set (not added to a collection)
        assertThat(order.getStatus())
                .as("Scalar field should be directly assigned the value")
                .isEqualTo(statusValue);
    }

    // ==================== 3.5 Agregasyon (aggregation result to scalar) ====================

    /**
     * 3.5 — Agregasyon: SUM/AVG/COUNT/MIN/MAX gibi agregasyon sonuçları skaler hedefe
     * değiştirilmeden yazılır. Bu, 3.4 ile aynı mekanizmayı kullanır ancak sayısal
     * değerler için geçerlidir.
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 50)
    void property3_5_aggregationResult_doubleField(
            @ForAll("scalarDoubleValue") Double aggregationResult) {

        // Given: An order with some totalAmount
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());
        order.setTotalAmount(0.0);

        // When: Assign an aggregation result (scalar numeric) to a scalar field
        ORDER_SVC.setValueByPath(order, List.of(Order_.totalAmount), aggregationResult);

        // Then: The aggregation result should be directly set without modification
        assertThat(order.getTotalAmount())
                .as("Aggregation result should be written unchanged to scalar target")
                .isEqualTo(aggregationResult);
    }

    /**
     * 3.5 — Agregasyon (Long): COUNT gibi bir agregasyon sonucu Long skaler hedefe yazılır.
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 50)
    void property3_5_aggregationResult_longField(
            @ForAll("singleNumber") Integer countResult) {

        // Given: An order
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());

        // When: Assign a count result to customerId (Long scalar field)
        Long countValue = countResult.longValue();
        ORDER_SVC.setValueByPath(order, List.of(Order_.customerId), countValue);

        // Then: The value should be directly set
        assertThat(order.getCustomerId())
                .as("Count aggregation result should be written unchanged to Long scalar target")
                .isEqualTo(countValue);
    }

    // ==================== 3.6 Meşru iç içe (legitimate nesting) ====================

    /**
     * 3.6 — Meşru iç içe: Hedef eleman tipi bir koleksiyon ({@code List<List<String>>})
     * iken tek bir iç liste eklenir → sonuç {@code [[...]]} (add davranışı korunur).
     *
     * <p>Bu durumda kaynak değer ({@code List<String>}) hedefin eleman tipine (List)
     * doğrudan atanabilir, yani "meşru" bir koleksiyon-içinde-koleksiyon ekleme yapılır.
     * Fix'in discriminator'ü bu durumu hariç tutar:
     * {@code isAssignable(elementType, value.getClass())} → true → ¬C → add korunur.</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Property(tries = 50)
    void property3_6_legitimateNesting_listOfLists(
            @ForAll("innerList") List<String> innerList) {

        // Given: A NestedCollectionEntity with an initialized nestedTags field
        NestedCollectionEntity entity = new NestedCollectionEntity();
        entity.setId("test-" + ID_SEQ.getAndIncrement());
        entity.setNestedTags(new ArrayList<>());

        // When: Assign a single inner list to the List<List<String>> field
        // This is legitimate nesting: the value (a List) IS assignable to elementType (List)
        NESTED_SVC.setValueByPath(entity, List.of(NestedCollectionEntity_.nestedTags), innerList);

        // Then: The inner list should be ADDED as a single element (not flattened)
        // Result should be [[...]] — legitimate nesting
        assertThat(entity.getNestedTags())
                .as("Inner list should be added as single element, producing [[...]]")
                .hasSize(1);

        assertThat(entity.getNestedTags().get(0))
                .as("The single element should be the inner list itself")
                .isInstanceOf(List.class)
                .isEqualTo(innerList);
    }

    /**
     * 3.6 — Meşru iç içe: Birden fazla iç liste eklendiğinde hepsi ayrı elemanlar olur.
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Example
    void example3_6_legitimateNesting_multipleInnerLists() {
        // Given: A NestedCollectionEntity with an initialized nestedTags field
        NestedCollectionEntity entity = new NestedCollectionEntity();
        entity.setId("test-nested");
        entity.setNestedTags(new ArrayList<>());

        // When: Add two inner lists separately
        List<String> inner1 = List.of("a", "b");
        List<String> inner2 = List.of("c", "d");
        NESTED_SVC.setValueByPath(entity, List.of(NestedCollectionEntity_.nestedTags), inner1);
        NESTED_SVC.setValueByPath(entity, List.of(NestedCollectionEntity_.nestedTags), inner2);

        // Then: nestedTags == [["a","b"], ["c","d"]] — each inner list is a separate element
        assertThat(entity.getNestedTags())
                .as("Each inner list should be a separate element in the outer list")
                .hasSize(2);

        assertThat(entity.getNestedTags().get(0)).isEqualTo(inner1);
        assertThat(entity.getNestedTags().get(1)).isEqualTo(inner2);
    }

    // ==================== 3.7 Null koleksiyon başlatma (null collection init) ====================

    /**
     * 3.7 — Null koleksiyon başlatma (List&lt;String&gt; tags): Null koleksiyon alanı
     * önce initialize edilip sonra tek eleman eklenir.
     *
     * <p>{@code handleCollectionField} içindeki {@code collection == null} kontrolü
     * koleksiyonu {@code new ArrayList<>()} ile başlatır ve ardından elemanı ekler.</p>
     *
     * <p><b>Validates: Requirements 3.7</b></p>
     */
    @Property(tries = 50)
    void property3_7_nullCollectionInit_tagsField(
            @ForAll("singleTag") String newTag) {

        // Given: An entity with NULL tags collection
        CollectionEntity entity = new CollectionEntity();
        entity.setId(ID_SEQ.getAndIncrement());
        assertThat(entity.getTags())
                .as("Precondition: tags should be null initially")
                .isNull();

        // When: Assign a single scalar value to the null collection field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), newTag);

        // Then: Collection should be initialized and element added
        assertThat(entity.getTags())
                .as("Null collection should be initialized")
                .isNotNull()
                .as("Initialized collection should contain the added element")
                .hasSize(1)
                .contains(newTag);
    }

    /**
     * 3.7 — Null koleksiyon başlatma (Order.items — model koleksiyon): Null items alanı
     * başlatılıp model öğesi eklenir.
     *
     * <p><b>Validates: Requirements 3.7</b></p>
     */
    @Property(tries = 50)
    void property3_7_nullCollectionInit_orderItems(
            @ForAll("singleOrderItem") OrderItem newItem) {

        // Given: An order with NULL items
        Order order = new Order();
        order.setId(ID_SEQ.getAndIncrement());
        assertThat(order.getItems())
                .as("Precondition: items should be null initially")
                .isNull();

        // When: Assign a single OrderItem to the null collection field
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), newItem);

        // Then: Collection should be initialized and item added
        assertThat(order.getItems())
                .as("Null collection should be initialized")
                .isNotNull()
                .as("Initialized collection should contain the added item")
                .hasSize(1);

        assertThat(order.getItems().get(0))
                .as("The item should be the one we added")
                .isSameAs(newItem);
    }

    /**
     * 3.7 — Null koleksiyon başlatma + model null: Model tipli koleksiyonda null değer
     * geçildiğinde yeni örnek üretilir ve eklenir.
     *
     * <p>{@code handleCollectionField}: elementType model (non-boxed) ve value null ise
     * {@code SpecificationServices.getService(elementType).createInstance()} çağrılır.</p>
     *
     * <p><b>Validates: Requirements 3.7</b></p>
     */
    @Example
    void example3_7_nullCollectionInit_nullModelValue() {
        // Given: An order with NULL items
        Order order = new Order();
        order.setId(99L);
        assertThat(order.getItems()).isNull();

        // When: Assign null to a model-type collection field
        // handleCollectionField should create a new OrderItem instance
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), null);

        // Then: Collection initialized, with a new (empty) OrderItem instance
        assertThat(order.getItems())
                .as("Null collection should be initialized")
                .isNotNull()
                .hasSize(1);

        assertThat(order.getItems().get(0))
                .as("A new OrderItem instance should have been created for null value")
                .isNotNull()
                .isInstanceOf(OrderItem.class);
    }

    // ==================== Combined / Edge case examples ====================

    /**
     * 3.3+3.7 combined: Null koleksiyona tek eleman eklenmesi hem init hem append'i test eder.
     */
    @Example
    void example_combined_nullInitThenAppend() {
        // Given: Null tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(100L);

        // When: Add two elements sequentially to initially null collection
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), "first");
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), "second");

        // Then: Both appended in order
        assertThat(entity.getTags())
                .as("Two elements appended sequentially to initially-null collection")
                .hasSize(2)
                .containsExactly("first", "second");
    }

    /**
     * 3.4 — Skaler hedef üstüne yazma: İkinci atama ilkini override eder.
     */
    @Example
    void example3_4_scalarOverwrite() {
        Order order = new Order();
        order.setId(101L);

        ORDER_SVC.setValueByPath(order, List.of(Order_.status), "FIRST");
        assertThat(order.getStatus()).isEqualTo("FIRST");

        ORDER_SVC.setValueByPath(order, List.of(Order_.status), "SECOND");
        assertThat(order.getStatus())
                .as("Second scalar assignment should overwrite the first")
                .isEqualTo("SECOND");
    }
}
