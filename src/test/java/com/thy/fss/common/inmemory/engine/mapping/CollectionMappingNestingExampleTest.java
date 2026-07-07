package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity_;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntity;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.NestedCollectionEntity_;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderItem;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order_;

import net.jqwik.api.Example;

/**
 * Somut birim testleri (Example) — düzeltme sonrası beklenen davranışı belgeleyen
 * deterministik, tekrarlanabilir örnekler.
 *
 * <p>Bu testler koleksiyondan koleksiyona atamanın düz (flat) ve idempotent çalıştığını,
 * kenar durumları (boş koleksiyon, null hedef) doğru işlediğini ve meşru iç içe (3.4)
 * davranışın korunduğunu somut örneklerle belgeler.</p>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 3.4, 3.5</b></p>
 */
class CollectionMappingNestingExampleTest {

    private static final SpecificationService<Order> ORDER_SVC = OrderSpecificationService.INSTANCE;
    private static final SpecificationService<CollectionEntity> COLLECTION_SVC = CollectionEntitySpecificationService.INSTANCE;
    private static final SpecificationService<NestedCollectionEntity> NESTED_SVC = NestedCollectionEntitySpecificationService.INSTANCE;

    // ==================== Order.items: düz atama (setValueByPath) ====================

    /**
     * Order.items: kaynak {@code [i1, i2]} -> hedef düz {@code [i1, i2]}.
     * setValueByPath yolu üzerinden koleksiyondan koleksiyona atama düz sonuç üretir.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void orderItems_shouldBeFlatAfterAssignment() {
        // Given
        Order order = new Order();
        order.setId(1L);

        OrderItem i1 = new OrderItem();
        i1.setId(10L);
        i1.setQuantity(2);
        i1.setUnitPrice(5.0);

        OrderItem i2 = new OrderItem();
        i2.setId(20L);
        i2.setQuantity(3);
        i2.setUnitPrice(7.0);

        List<OrderItem> sourceItems = List.of(i1, i2);

        // When
        ORDER_SVC.setValueByPath(order, List.of(Order_.items), sourceItems);

        // Then: flat [i1, i2], not nested [[i1, i2]]
        assertThat(order.getItems())
                .as("Order.items should be flat [i1, i2]")
                .hasSize(2)
                .containsExactly(i1, i2);

        // Each element is OrderItem, not a wrapped collection
        for (Object element : order.getItems()) {
            assertThat(element).isInstanceOf(OrderItem.class);
        }
    }

    // ==================== CollectionEntity.tags (List<String>): düz atama ====================

    /**
     * CollectionEntity.tags: kaynak {@code ["a","b"]} -> hedef düz {@code ["a","b"]}.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void collectionEntityTags_shouldBeFlatAfterAssignment() {
        // Given
        CollectionEntity entity = new CollectionEntity();
        entity.setId(1L);
        entity.setTags(new ArrayList<>(Arrays.asList("old1", "old2")));

        List<String> sourceTags = List.of("a", "b");

        // When
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        // Then: flat ["a", "b"]
        assertThat(entity.getTags())
                .as("tags should be [a, b], not [[a, b]]")
                .hasSize(2)
                .containsExactly("a", "b");

        // Each element is String, not List
        for (Object element : entity.getTags()) {
            assertThat(element).isInstanceOf(String.class);
        }
    }

    // ==================== CollectionEntity.numbers (Set<Integer>): tip korunur ====================

    /**
     * CollectionEntity.numbers: kaynak {@code {1, 2}} -> hedef {@code {1, 2}};
     * somut tip {@code Set} olarak korunur (clear()+addAll mevcut örneği yerinde günceller).
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void collectionEntityNumbers_shouldPreserveSetType() {
        // Given
        CollectionEntity entity = new CollectionEntity();
        entity.setId(2L);
        Set<Integer> initialSet = new HashSet<>(Arrays.asList(99, 100));
        entity.setNumbers(initialSet);

        Set<Integer> sourceNumbers = new HashSet<>(Arrays.asList(1, 2));

        // When
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.numbers), sourceNumbers);

        // Then: content is {1, 2}
        assertThat(entity.getNumbers())
                .as("numbers should contain exactly {1, 2}")
                .hasSize(2)
                .containsExactlyInAnyOrder(1, 2);

        // Concrete type remains Set (clear()+addAll preserves the existing instance)
        assertThat(entity.getNumbers())
                .as("Concrete type should remain Set, not converted to List")
                .isInstanceOf(Set.class);

        // Same instance should be reused (not replaced with a new collection)
        assertThat(entity.getNumbers())
                .as("The same Set instance should be reused via clear()+addAll")
                .isSameAs(initialSet);
    }

    // ==================== Boş kaynak koleksiyon (edge case) ====================

    /**
     * Boş kaynak koleksiyon: hedef boşalır (idempotent).
     * {@code List.of()} atandığında hedef koleksiyonun içeriği temizlenir.
     *
     * <p><b>Validates: Requirements 2.1, 2.2</b></p>
     */
    @Example
    void emptySourceCollection_shouldEmptyTarget() {
        // Given: entity with existing tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(3L);
        entity.setTags(new ArrayList<>(Arrays.asList("x", "y", "z")));

        // When: Assign empty collection
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), List.of());

        // Then: target is now empty
        assertThat(entity.getTags())
                .as("Empty source should clear target (idempotent)")
                .isNotNull()
                .isEmpty();

        // Re-apply: still empty (idempotent)
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), List.of());
        assertThat(entity.getTags())
                .as("Repeated empty assignment should remain empty (idempotent)")
                .isEmpty();
    }

    // ==================== Null hedef alan: initialize + populate (3.5) ====================

    /**
     * Null hedef alan: koleksiyon önce initialize edilir, sonra kaynak elemanlarıyla doldurulur.
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Example
    void nullTargetField_shouldInitializeAndPopulate() {
        // Given: entity with null tags (default)
        CollectionEntity entity = new CollectionEntity();
        entity.setId(4L);
        assertThat(entity.getTags())
                .as("Precondition: tags should be null")
                .isNull();

        List<String> sourceTags = List.of("hello", "world");

        // When: Assign a collection to the null field
        COLLECTION_SVC.setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags);

        // Then: Collection is initialized and populated with source elements (flat)
        assertThat(entity.getTags())
                .as("Null field should be initialized and populated")
                .isNotNull()
                .hasSize(2)
                .containsExactly("hello", "world");
    }

    // ==================== Meşru iç içe: List<List<String>> (3.4 preserved) ====================

    /**
     * Meşru iç içe: hedef {@code List<List<String>>} — tek iç liste eklenir -> {@code [[...]]}.
     * Bu durumda eleman tipi kendisi bir koleksiyon olduğundan add davranışı korunur.
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Example
    void legitimateNesting_singleInnerListAdded_shouldProduceNestedStructure() {
        // Given: NestedCollectionEntity with initialized nestedTags
        NestedCollectionEntity entity = new NestedCollectionEntity();
        entity.setId("nested-1");
        entity.setNestedTags(new ArrayList<>());

        List<String> innerList = List.of("a", "b", "c");

        // When: Add a single inner list to List<List<String>>
        NESTED_SVC.setValueByPath(entity, List.of(NestedCollectionEntity_.nestedTags), innerList);

        // Then: Result is [[a, b, c]] — the inner list is added as ONE element (legitimate nesting)
        assertThat(entity.getNestedTags())
                .as("Legitimate nesting: inner list added as single element -> [[...]]")
                .hasSize(1);

        assertThat(entity.getNestedTags().get(0))
                .as("First element should be the inner list itself")
                .isInstanceOf(List.class)
                .isEqualTo(innerList);
    }

    // ==================== ALL seçici yolu (setValueByPathWithCollections): düz sonuç ====================

    /**
     * ALL seçici yolu: {@code setValueByPathWithCollections} ile bütün bir koleksiyon atanır;
     * sonuç düz (flat) olmalı.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    @SuppressWarnings("unchecked")
    void allSelectorPath_shouldProduceFlatResult() {
        // Given: entity with existing tags
        CollectionEntity entity = new CollectionEntity();
        entity.setId(5L);
        entity.setTags(new ArrayList<>(Arrays.asList("existing")));

        List<String> sourceTags = List.of("x", "y", "z");

        // Build CollectionOperationMetadata for ALL selector
        CollectionOperationMetadata<CollectionEntity, String> allOp =
                new CollectionOperationMetadata<>(
                        0,  // pathIndex: 0 (the only element in path)
                        (CollectionAttribute<CollectionEntity, String>) (CollectionAttribute<?, ?>) CollectionEntity_.tags,
                        CollectionSelector.ALL,
                        null  // no specification filter
                );

        // When: Use setValueByPathWithCollections with ALL selector
        COLLECTION_SVC.setValueByPathWithCollections(
                entity,
                List.of(CollectionEntity_.tags),
                List.of(allOp),
                sourceTags
        );

        // Then: Result should be flat [x, y, z]
        assertThat(entity.getTags())
                .as("ALL selector path should produce flat result")
                .isNotNull();

        for (Object element : entity.getTags()) {
            assertThat(element)
                    .as("Each element should be String, not a nested collection. Actual tags: %s",
                            entity.getTags())
                    .isInstanceOf(String.class);
        }

        assertThat(entity.getTags())
                .as("Tags should contain source elements flat")
                .containsExactly("x", "y", "z");
    }
}
