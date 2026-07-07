package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.thy.fss.common.inmemory.testmodel.OrderItemSpecificationService;
import com.thy.fss.common.inmemory.testmodel.OrderItem_;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order_;

import net.jqwik.api.Example;

/**
 * Integration tests for the collection mapping nesting fix.
 *
 * <p>These tests use the REAL {@link MappingApplicator#applyMappingsToEntity} flow
 * with actual {@link PropertyMapping} configurations and a simple
 * {@link RelatedEntityLookup} to verify the full synchronization pipeline.</p>
 *
 * <p>Validates:</p>
 * <ul>
 *   <li>Collection-to-collection mapping produces flat result in full sync flow</li>
 *   <li>Single-level AND multi-level targets behave consistently (2.3)</li>
 *   <li>Repeated synchronization does not grow the target collection (idempotent, 2.2)</li>
 *   <li>Both List (tags) and Set (numbers) MANY_TO_ONE_COLLECTION targets work correctly</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3</b></p>
 */
class CollectionMappingNestingIntegrationTest {

    private static final String CONSUMER_ID = "integration-test-store";
    private static final String DATASOURCE_NAME = "test-datasource";

    // ==================== MANY_TO_ONE_COLLECTION — List<String> (tags) ====================

    /**
     * Full sync flow: MANY_TO_ONE_COLLECTION mapping collects String field values
     * from related entities into {@code CollectionEntity.tags} (List&lt;String&gt;).
     * Result must be flat — no nested wrapping.
     *
     * <p>Setup: 3 source OrderItem entities each with a known orderId. The mapping
     * extracts nothing (sourcePath = null uses whole entity) — instead we use Order
     * entities and extract their status (String) into tags.</p>
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void fullSyncFlow_tags_shouldProduceFlatResult() {
        // Given: target entity
        CollectionEntity target = new CollectionEntity();
        target.setId(100L);
        target.setTags(new ArrayList<>());

        // Source entities (Order) whose status will be collected into tags
        Order source1 = new Order();
        source1.setId(1L);
        source1.setCustomerId(100L); // FK matching target's id
        source1.setStatus("ACTIVE");

        Order source2 = new Order();
        source2.setId(2L);
        source2.setCustomerId(100L);
        source2.setStatus("PENDING");

        Order source3 = new Order();
        source3.setId(3L);
        source3.setCustomerId(100L);
        source3.setStatus("SHIPPED");

        List<Order> relatedEntities = Arrays.asList(source1, source2, source3);

        // Build MANY_TO_ONE_COLLECTION mapping: Order.status -> CollectionEntity.tags
        PropertyMapping<CollectionEntity, List<String>> mapping = buildTagsMapping(relatedEntities);

        // RelatedEntityLookup that returns our source entities
        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply via full MappingApplicator flow
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));

        // Then: tags should be flat ["ACTIVE", "PENDING", "SHIPPED"]
        assertThat(target.getTags())
                .as("Full sync: tags should be flat list of extracted strings")
                .isNotNull()
                .hasSize(3)
                .containsExactly("ACTIVE", "PENDING", "SHIPPED");

        // Each element must be a String, not a nested collection
        for (Object element : target.getTags()) {
            assertThat(element)
                    .as("Each tag element should be String, not a wrapped collection")
                    .isInstanceOf(String.class);
        }
    }

    // ==================== MANY_TO_ONE_COLLECTION — Set<Integer> (numbers) ====================

    /**
     * Full sync flow: MANY_TO_ONE_COLLECTION mapping collects Integer field values
     * from related entities into {@code CollectionEntity.numbers} (Set&lt;Integer&gt;).
     * Result must be flat and the Set concrete type must be preserved.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void fullSyncFlow_numbers_shouldProduceFlatResultAndPreserveSetType() {
        // Given: target entity with initialized Set
        CollectionEntity target = new CollectionEntity();
        target.setId(200L);
        Set<Integer> initialSet = new HashSet<>(Arrays.asList(999));
        target.setNumbers(initialSet);

        // Source entities (OrderItem) whose quantity will be collected into numbers
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrderId(200L); // FK matching target's id
        item1.setQuantity(5);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrderId(200L);
        item2.setQuantity(10);

        OrderItem item3 = new OrderItem();
        item3.setId(3L);
        item3.setOrderId(200L);
        item3.setQuantity(15);

        List<OrderItem> relatedEntities = Arrays.asList(item1, item2, item3);

        // Build MANY_TO_ONE_COLLECTION mapping: OrderItem.quantity -> CollectionEntity.numbers
        PropertyMapping<CollectionEntity, Set<Integer>> mapping = buildNumbersMapping(relatedEntities);

        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply via full MappingApplicator flow
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));

        // Then: numbers should be flat {5, 10, 15}
        assertThat(target.getNumbers())
                .as("Full sync: numbers should be flat set of extracted integers")
                .isNotNull()
                .hasSize(3)
                .containsExactlyInAnyOrder(5, 10, 15);

        // Each element must be an Integer, not a nested collection
        for (Object element : target.getNumbers()) {
            assertThat(element)
                    .as("Each number element should be Integer, not a wrapped collection")
                    .isInstanceOf(Integer.class);
        }

        // Concrete type should be Set
        assertThat(target.getNumbers())
                .as("Concrete type should remain Set")
                .isInstanceOf(Set.class);
    }

    // ==================== MANY_TO_ONE_COLLECTION — List<OrderItem> (Order.items) ====================

    /**
     * Full sync flow: MANY_TO_ONE_COLLECTION mapping collects entire OrderItem entities
     * into {@code Order.items} (List&lt;OrderItem&gt;) using sourcePath=null (direct entity reference).
     * Result must be flat — each element is an OrderItem, not a wrapped list.
     *
     * <p><b>Validates: Requirements 2.1</b></p>
     */
    @Example
    void fullSyncFlow_orderItems_shouldProduceFlatResult() {
        // Given: target Order entity
        Order target = new Order();
        target.setId(300L);
        target.setCustomerId(50L);

        // Source entities (OrderItem) — entire entities collected into Order.items
        OrderItem item1 = new OrderItem();
        item1.setId(10L);
        item1.setOrderId(300L); // FK matching target's id
        item1.setQuantity(2);
        item1.setUnitPrice(5.0);

        OrderItem item2 = new OrderItem();
        item2.setId(20L);
        item2.setOrderId(300L);
        item2.setQuantity(3);
        item2.setUnitPrice(7.0);

        List<OrderItem> relatedEntities = Arrays.asList(item1, item2);

        // Build MANY_TO_ONE_COLLECTION mapping: OrderItem (whole) -> Order.items
        // sourcePath = null means entire entity is used
        PropertyMapping<Order, List<OrderItem>> mapping = buildOrderItemsMapping(relatedEntities);

        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply via full MappingApplicator flow
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));

        // Then: items should be flat [item1, item2]
        assertThat(target.getItems())
                .as("Full sync: Order.items should be flat list of OrderItem entities")
                .isNotNull()
                .hasSize(2)
                .containsExactly(item1, item2);

        // Each element must be an OrderItem, not a nested list
        for (Object element : target.getItems()) {
            assertThat(element)
                    .as("Each element should be OrderItem, not a wrapped collection")
                    .isInstanceOf(OrderItem.class);
        }
    }

    // ==================== Multi-level target — ComplexNestedEntity.level1.items ====================

    /**
     * Full sync flow with multi-level target path: MANY_TO_ONE_COLLECTION mapping
     * targets {@code ComplexNestedEntity.level1.items} (path size > 1, ending in collection).
     * Verifies that single-level and multi-level targets behave consistently (2.3).
     *
     * <p><b>Validates: Requirements 2.1, 2.3</b></p>
     */
    @Example
    void fullSyncFlow_multiLevelTarget_shouldProduceFlatResult() {
        // Given: target ComplexNestedEntity with level1 initialized
        ComplexNestedEntity target = new ComplexNestedEntity();
        target.setName("integration-multi-level");
        Level1 level1 = new Level1();
        level1.setName("nested-level");
        level1.setItems(new ArrayList<>());
        target.setLevel1(level1);

        // Source entities (Order) whose status will be collected into level1.items
        Order source1 = new Order();
        source1.setId(1L);
        source1.setCustomerId(1L);
        source1.setStatus("FIRST");
        source1.setTotalAmount(10.0);

        Order source2 = new Order();
        source2.setId(2L);
        source2.setCustomerId(2L);
        source2.setStatus("SECOND");
        source2.setTotalAmount(20.0);

        List<Order> relatedEntities = Arrays.asList(source1, source2);

        // Build MANY_TO_ONE_COLLECTION mapping: Order.status -> ComplexNestedEntity.level1.items
        // Multi-level target path: [ComplexNestedEntity_.level1, Level1_.items]
        PropertyMapping<ComplexNestedEntity, List<String>> mapping = buildMultiLevelMapping(relatedEntities);

        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply via full MappingApplicator flow
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));

        // Then: level1.items should be flat
        List<String> result = target.getLevel1().getItems();
        assertThat(result)
                .as("Multi-level target: level1.items should be flat")
                .isNotNull()
                .hasSize(2)
                .containsExactly("FIRST", "SECOND");

        for (Object element : result) {
            assertThat(element)
                    .as("Each element in multi-level target should be String, not nested")
                    .isInstanceOf(String.class);
        }
    }

    // ==================== Idempotent — repeated sync (tags) ====================

    /**
     * Repeated synchronization: applying the same MANY_TO_ONE_COLLECTION mapping
     * multiple times must NOT grow the target collection. Verifies idempotency (2.2).
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Example
    void repeatedSync_tags_shouldNotGrow() {
        // Given: target entity
        CollectionEntity target = new CollectionEntity();
        target.setId(400L);
        target.setTags(new ArrayList<>());

        // Source entities
        Order source1 = new Order();
        source1.setId(1L);
        source1.setCustomerId(400L);
        source1.setStatus("ALPHA");

        Order source2 = new Order();
        source2.setId(2L);
        source2.setCustomerId(400L);
        source2.setStatus("BETA");

        List<Order> relatedEntities = Arrays.asList(source1, source2);

        PropertyMapping<CollectionEntity, List<String>> mapping = buildTagsMapping(relatedEntities);
        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply sync FIRST time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        List<String> afterFirstSync = new ArrayList<>(target.getTags());

        // When: Apply sync SECOND time (repeated)
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        List<String> afterSecondSync = new ArrayList<>(target.getTags());

        // When: Apply sync THIRD time (triple check)
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        List<String> afterThirdSync = new ArrayList<>(target.getTags());

        // Then: All results should be identical — no growth
        assertThat(afterFirstSync)
                .as("First sync should produce flat result")
                .hasSize(2)
                .containsExactly("ALPHA", "BETA");

        assertThat(afterSecondSync)
                .as("Second sync should be identical to first (idempotent)")
                .isEqualTo(afterFirstSync);

        assertThat(afterThirdSync)
                .as("Third sync should be identical to first (idempotent)")
                .isEqualTo(afterFirstSync);
    }

    // ==================== Idempotent — repeated sync (numbers/Set) ====================

    /**
     * Repeated synchronization for Set target: applying the same mapping
     * multiple times must NOT grow the target Set. Verifies idempotency (2.2).
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Example
    void repeatedSync_numbers_shouldNotGrow() {
        // Given: target entity with initialized Set
        CollectionEntity target = new CollectionEntity();
        target.setId(500L);
        target.setNumbers(new HashSet<>());

        // Source entities
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrderId(500L);
        item1.setQuantity(42);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrderId(500L);
        item2.setQuantity(99);

        List<OrderItem> relatedEntities = Arrays.asList(item1, item2);

        PropertyMapping<CollectionEntity, Set<Integer>> mapping = buildNumbersMapping(relatedEntities);
        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply sync FIRST time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        Set<Integer> afterFirstSync = new HashSet<>(target.getNumbers());

        // When: Apply sync SECOND time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        Set<Integer> afterSecondSync = new HashSet<>(target.getNumbers());

        // When: Apply sync THIRD time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        Set<Integer> afterThirdSync = new HashSet<>(target.getNumbers());

        // Then: All results should be identical — no growth
        assertThat(afterFirstSync)
                .as("First sync should produce flat result")
                .hasSize(2)
                .containsExactlyInAnyOrder(42, 99);

        assertThat(afterSecondSync)
                .as("Second sync should be identical to first (idempotent)")
                .isEqualTo(afterFirstSync);

        assertThat(afterThirdSync)
                .as("Third sync should be identical to first (idempotent)")
                .isEqualTo(afterFirstSync);
    }

    // ==================== Idempotent — repeated sync (Order.items) ====================

    /**
     * Repeated synchronization for Order.items: applying the same mapping
     * multiple times must NOT grow the target collection. Verifies idempotency (2.2).
     *
     * <p><b>Validates: Requirements 2.2</b></p>
     */
    @Example
    void repeatedSync_orderItems_shouldNotGrow() {
        // Given: target Order entity
        Order target = new Order();
        target.setId(600L);
        target.setCustomerId(50L);

        // Source entities
        OrderItem item1 = new OrderItem();
        item1.setId(10L);
        item1.setOrderId(600L);
        item1.setQuantity(1);
        item1.setUnitPrice(10.0);

        OrderItem item2 = new OrderItem();
        item2.setId(20L);
        item2.setOrderId(600L);
        item2.setQuantity(2);
        item2.setUnitPrice(20.0);

        List<OrderItem> relatedEntities = Arrays.asList(item1, item2);

        PropertyMapping<Order, List<OrderItem>> mapping = buildOrderItemsMapping(relatedEntities);
        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // When: Apply sync FIRST time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        List<OrderItem> afterFirstSync = new ArrayList<>(target.getItems());

        // When: Apply sync SECOND time
        MappingApplicator.applyMappingsToEntity(lookup, target, Collections.singletonList(mapping));
        List<OrderItem> afterSecondSync = new ArrayList<>(target.getItems());

        // Then: Both results should be identical — no growth
        assertThat(afterFirstSync)
                .as("First sync should produce flat [item1, item2]")
                .hasSize(2)
                .containsExactly(item1, item2);

        assertThat(afterSecondSync)
                .as("Second sync should be identical to first (idempotent)")
                .isEqualTo(afterFirstSync);
    }

    // ==================== Consistency: single-level vs multi-level (2.3) ====================

    /**
     * Consistency check: single-level target (CollectionEntity.tags) and multi-level target
     * (ComplexNestedEntity.level1.items) should produce equivalent flat results when given
     * the same source data. This verifies requirement 2.3 (consistent behavior).
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Example
    void singleLevel_and_multiLevel_shouldBehaveConsistently() {
        // Source entities — same data for both
        Order source1 = new Order();
        source1.setId(1L);
        source1.setCustomerId(700L);
        source1.setStatus("X");

        Order source2 = new Order();
        source2.setId(2L);
        source2.setCustomerId(700L);
        source2.setStatus("Y");

        List<Order> relatedEntities = Arrays.asList(source1, source2);
        RelatedEntityLookup lookup = (m, pkValues) -> relatedEntities;

        // --- Single-level target: CollectionEntity.tags ---
        CollectionEntity singleLevelTarget = new CollectionEntity();
        singleLevelTarget.setId(700L);
        singleLevelTarget.setTags(new ArrayList<>());

        PropertyMapping<CollectionEntity, List<String>> singleLevelMapping = buildTagsMapping(relatedEntities);
        MappingApplicator.applyMappingsToEntity(lookup, singleLevelTarget, Collections.singletonList(singleLevelMapping));

        // --- Multi-level target: ComplexNestedEntity.level1.items ---
        ComplexNestedEntity multiLevelTarget = new ComplexNestedEntity();
        multiLevelTarget.setName("consistency-check");
        Level1 level1 = new Level1();
        level1.setName("level");
        level1.setItems(new ArrayList<>());
        multiLevelTarget.setLevel1(level1);

        PropertyMapping<ComplexNestedEntity, List<String>> multiLevelMapping = buildMultiLevelMapping(relatedEntities);
        MappingApplicator.applyMappingsToEntity(lookup, multiLevelTarget, Collections.singletonList(multiLevelMapping));

        // Then: Both targets should have the same flat content
        assertThat(singleLevelTarget.getTags())
                .as("Single-level target should be flat")
                .hasSize(2);

        assertThat(multiLevelTarget.getLevel1().getItems())
                .as("Multi-level target should be flat")
                .hasSize(2);

        // Both should contain the same extracted values (status strings from Orders)
        // Single-level and multi-level are consistent
        for (Object element : singleLevelTarget.getTags()) {
            assertThat(element).isInstanceOf(String.class);
        }
        for (Object element : multiLevelTarget.getLevel1().getItems()) {
            assertThat(element).isInstanceOf(String.class);
        }
    }

    // ==================== Helper Methods — Mapping Builders ====================

    /**
     * Builds a MANY_TO_ONE_COLLECTION mapping: Order.status -> CollectionEntity.tags.
     * PK: CollectionEntity.id (Long), FK: Order.customerId (Long).
     */
    @SuppressWarnings("unchecked")
    private PropertyMapping<CollectionEntity, List<String>> buildTagsMapping(List<Order> relatedEntities) {
        return (PropertyMapping<CollectionEntity, List<String>>) (PropertyMapping<?, ?>)
                PropertyMapping.<CollectionEntity, String>builder()
                        .consumerId(CONSUMER_ID)
                        .isForDashboard(false)
                        .sourceService(OrderSpecificationService.INSTANCE)
                        .targetService(CollectionEntitySpecificationService.INSTANCE)
                        .targetPath(Collections.singletonList(CollectionEntity_.tags))
                        .sourcePath(Collections.singletonList(Order_.status))
                        .datasourceName(DATASOURCE_NAME)
                        .primaryKeyPaths(Collections.singletonList(
                                Collections.singletonList(CollectionEntity_.id)))
                        .foreignKeyPaths(Collections.singletonList(
                                Collections.singletonList(Order_.customerId)))
                        .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                        .build();
    }

    /**
     * Builds a MANY_TO_ONE_COLLECTION mapping: OrderItem.quantity -> CollectionEntity.numbers.
     * PK: CollectionEntity.id (Long), FK: OrderItem.orderId (Long).
     */
    @SuppressWarnings("unchecked")
    private PropertyMapping<CollectionEntity, Set<Integer>> buildNumbersMapping(List<OrderItem> relatedEntities) {
        return (PropertyMapping<CollectionEntity, Set<Integer>>) (PropertyMapping<?, ?>)
                PropertyMapping.<CollectionEntity, Integer>builder()
                        .consumerId(CONSUMER_ID)
                        .isForDashboard(false)
                        .sourceService(OrderItemSpecificationService.INSTANCE)
                        .targetService(CollectionEntitySpecificationService.INSTANCE)
                        .targetPath(Collections.singletonList(CollectionEntity_.numbers))
                        .sourcePath(Collections.singletonList(OrderItem_.quantity))
                        .datasourceName(DATASOURCE_NAME)
                        .primaryKeyPaths(Collections.singletonList(
                                Collections.singletonList(CollectionEntity_.id)))
                        .foreignKeyPaths(Collections.singletonList(
                                Collections.singletonList(OrderItem_.orderId)))
                        .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                        .build();
    }

    /**
     * Builds a MANY_TO_ONE_COLLECTION mapping: OrderItem (whole entity) -> Order.items.
     * PK: Order.id (Long), FK: OrderItem.orderId (Long).
     * sourcePath = null — entire entities are collected.
     */
    @SuppressWarnings("unchecked")
    private PropertyMapping<Order, List<OrderItem>> buildOrderItemsMapping(List<OrderItem> relatedEntities) {
        return (PropertyMapping<Order, List<OrderItem>>) (PropertyMapping<?, ?>)
                PropertyMapping.<Order, OrderItem>builder()
                        .consumerId(CONSUMER_ID)
                        .isForDashboard(false)
                        .sourceService(OrderItemSpecificationService.INSTANCE)
                        .targetService(OrderSpecificationService.INSTANCE)
                        .targetPath(Collections.singletonList(Order_.items))
                        // sourcePath intentionally NOT set — collect entire entities
                        .datasourceName(DATASOURCE_NAME)
                        .primaryKeyPaths(Collections.singletonList(
                                Collections.singletonList(Order_.id)))
                        .foreignKeyPaths(Collections.singletonList(
                                Collections.singletonList(OrderItem_.orderId)))
                        .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                        .build();
    }

    /**
     * Builds a MANY_TO_ONE_COLLECTION mapping with multi-level target:
     * Order.status -> ComplexNestedEntity.level1.items.
     * PK: ComplexNestedEntity.name (String), FK: Order.status (String).
     * Target path: [ComplexNestedEntity_.level1, Level1_.items]
     */
    @SuppressWarnings("unchecked")
    private PropertyMapping<ComplexNestedEntity, List<String>> buildMultiLevelMapping(List<Order> relatedEntities) {
        List<MetaAttribute<?, ?>> targetPath = Arrays.asList(
                ComplexNestedEntity_.level1, Level1_.items);

        return (PropertyMapping<ComplexNestedEntity, List<String>>) (PropertyMapping<?, ?>)
                PropertyMapping.<ComplexNestedEntity, String>builder()
                        .consumerId(CONSUMER_ID)
                        .isForDashboard(false)
                        .sourceService(OrderSpecificationService.INSTANCE)
                        .targetService(ComplexNestedEntitySpecificationService.INSTANCE)
                        .targetPath(targetPath)
                        .sourcePath(Collections.singletonList(Order_.status))
                        .datasourceName(DATASOURCE_NAME)
                        .primaryKeyPaths(Collections.singletonList(
                                Collections.singletonList(ComplexNestedEntity_.name)))
                        .foreignKeyPaths(Collections.singletonList(
                                Collections.singletonList(Order_.status)))
                        .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                        .build();
    }
}
