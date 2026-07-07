package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.testmodel.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for element type service delegation in collection filter validation.
 * 
 * Feature: collection-filter-model-type-support
 * Property 6: Element type service delegation
 * Validates: Requirements 4.1, 4.2, 4.3
 * 
 * Tests that collection filter validation correctly delegates to element type specification services
 * for any/all/none operators when collection elements are model types.
 */
@DisplayName("Property 6: Element type service delegation")
class ElementTypeServiceDelegationPropertyTest {

    /**
     * Provides arbitrary OrderItem instances.
     */
    @Provide
    Arbitrary<OrderItem> orderItems() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),
                Arbitraries.longs().between(1L, 100L),
                Arbitraries.integers().between(1, 10),
                Arbitraries.doubles().between(1.0, 1000.0)
        ).as((id, productId, quantity, price) -> {
            OrderItem item = new OrderItem();
            item.setId(id);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setUnitPrice(price);
            return item;
        });
    }

    /**
     * Provides arbitrary Order instances with OrderItem collections.
     */
    @Provide
    Arbitrary<Order> orders() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),
                Arbitraries.longs().between(1L, 100L),
                orderItems().list().ofMinSize(1).ofMaxSize(5)
        ).as((id, customerId, items) -> {
            Order order = new Order();
            order.setId(id);
            order.setCustomerId(customerId);
            order.setItems(items);
            return order;
        });
    }

    /**
     * Property: For any collection of model type elements and any filter,
     * the ANY operator should validate that at least one element matches using element type service.
     * 
     * Validates: Requirement 4.1
     */
    @Property(tries = 100)
    @DisplayName("ANY operator delegates to element type service and validates at least one match")
    void anyOperatorDelegatesToElementTypeService(
            @ForAll("orders") Order order,
            @ForAll @LongRange(min = 1, max = 100) Long matchingProductId) {
        
        // Ensure order has items
        if (order.getItems() == null || order.getItems().isEmpty()) {
            order.setItems(new ArrayList<>());
        }
        
        // Add an item with the matching product ID
        OrderItem matchingItem = new OrderItem();
        matchingItem.setId(999L);
        matchingItem.setProductId(matchingProductId);
        matchingItem.setQuantity(1);
        matchingItem.setUnitPrice(10.0);
        order.getItems().add(matchingItem);
        
        // Create a filter that matches the product ID
        OrderItemFilter itemFilter = new OrderItemFilter();
        itemFilter.setProductId(new com.thy.fss.common.inmemory.filter.LongFilter());
        itemFilter.getProductId().setEquals(matchingProductId);
        
        // Create order filter with collection ANY operator
        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setItems(new CollectionFilter<>());
        orderFilter.getItems().setCollectionAny(itemFilter);
        
        // Get the generated specification service
        OrderSpecificationService service = OrderSpecificationService.INSTANCE;
        
        // Validate - should return true because at least one element matches
        boolean result = service.validateFilter(order, orderFilter);
        
        // Assert that validation succeeded
        assert result : "ANY operator should validate when at least one element matches";
    }

    /**
     * Property: For any collection of model type elements and any filter,
     * the ALL operator should validate that all elements match using element type service.
     * 
     * Validates: Requirement 4.2
     */
    @Property(tries = 100)
    @DisplayName("ALL operator delegates to element type service and validates all elements match")
    void allOperatorDelegatesToElementTypeService(
            @ForAll @LongRange(min = 1, max = 100) Long productId,
            @ForAll @IntRange(min = 1, max = 5) int itemCount) {
        
        // Create order with multiple items having the same product ID
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            OrderItem item = new OrderItem();
            item.setId((long) i);
            item.setProductId(productId);
            item.setQuantity(1);
            item.setUnitPrice(10.0);
            items.add(item);
        }
        order.setItems(items);
        
        // Create a filter that matches all items
        OrderItemFilter itemFilter = new OrderItemFilter();
        itemFilter.setProductId(new com.thy.fss.common.inmemory.filter.LongFilter());
        itemFilter.getProductId().setEquals(productId);
        
        // Create order filter with collection ALL operator
        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setItems(new CollectionFilter<>());
        orderFilter.getItems().setCollectionAll(itemFilter);
        
        // Get the generated specification service
        OrderSpecificationService service = OrderSpecificationService.INSTANCE;
        
        // Validate - should return true because all elements match
        boolean result = service.validateFilter(order, orderFilter);
        
        // Assert that validation succeeded
        assert result : "ALL operator should validate when all elements match";
    }

    /**
     * Property: For any collection of model type elements and any filter,
     * the NONE operator should validate that no elements match using element type service.
     * 
     * Validates: Requirement 4.3
     */
    @Property(tries = 100)
    @DisplayName("NONE operator delegates to element type service and validates no elements match")
    void noneOperatorDelegatesToElementTypeService(
            @ForAll("orders") Order order,
            @ForAll @LongRange(min = 10000, max = 20000) Long nonMatchingProductId) {
        
        // Ensure order has items
        if (order.getItems() == null || order.getItems().isEmpty()) {
            OrderItem item = new OrderItem();
            item.setId(1L);
            item.setProductId(1L);
            item.setQuantity(1);
            item.setUnitPrice(10.0);
            order.setItems(List.of(item));
        }
        
        // Create a filter that matches no items (using a product ID that doesn't exist)
        OrderItemFilter itemFilter = new OrderItemFilter();
        itemFilter.setProductId(new com.thy.fss.common.inmemory.filter.LongFilter());
        itemFilter.getProductId().setEquals(nonMatchingProductId);
        
        // Create order filter with collection NONE operator
        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setItems(new CollectionFilter<>());
        orderFilter.getItems().setCollectionNone(itemFilter);
        
        // Get the generated specification service
        OrderSpecificationService service = OrderSpecificationService.INSTANCE;
        
        // Validate - should return true because no elements match
        boolean result = service.validateFilter(order, orderFilter);
        
        // Assert that validation succeeded
        assert result : "NONE operator should validate when no elements match";
    }

    /**
     * Property: For any collection with mixed matching and non-matching elements,
     * ANY operator should return true, ALL operator should return false.
     * 
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    @DisplayName("Mixed collection: ANY returns true, ALL returns false")
    void mixedCollectionValidation(
            @ForAll @LongRange(min = 1, max = 100) Long matchingProductId,
            @ForAll @LongRange(min = 101, max = 200) Long nonMatchingProductId) {
        
        // Create order with mixed items
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        
        OrderItem matchingItem = new OrderItem();
        matchingItem.setId(1L);
        matchingItem.setProductId(matchingProductId);
        matchingItem.setQuantity(1);
        matchingItem.setUnitPrice(10.0);
        
        OrderItem nonMatchingItem = new OrderItem();
        nonMatchingItem.setId(2L);
        nonMatchingItem.setProductId(nonMatchingProductId);
        nonMatchingItem.setQuantity(1);
        nonMatchingItem.setUnitPrice(20.0);
        
        order.setItems(List.of(matchingItem, nonMatchingItem));
        
        // Create a filter that matches only the first item
        OrderItemFilter itemFilter = new OrderItemFilter();
        itemFilter.setProductId(new com.thy.fss.common.inmemory.filter.LongFilter());
        itemFilter.getProductId().setEquals(matchingProductId);
        
        // Get the generated specification service
        OrderSpecificationService service = OrderSpecificationService.INSTANCE;
        
        // Test ANY operator - should return true
        OrderFilter anyFilter = new OrderFilter();
        anyFilter.setItems(new CollectionFilter<>());
        anyFilter.getItems().setCollectionAny(itemFilter);
        boolean anyResult = service.validateFilter(order, anyFilter);
        assert anyResult : "ANY operator should return true when at least one element matches";
        
        // Test ALL operator - should return false
        OrderFilter allFilter = new OrderFilter();
        allFilter.setItems(new CollectionFilter<>());
        allFilter.getItems().setCollectionAll(itemFilter);
        boolean allResult = service.validateFilter(order, allFilter);
        assert !allResult : "ALL operator should return false when not all elements match";
    }

    /**
     * Property: For null or empty collections, ANY and ALL should return false, NONE should return true.
     * 
     * Validates: Requirements 4.1, 4.2, 4.3
     */
    @Property(tries = 100)
    @DisplayName("Empty collection: ANY and ALL return false, NONE returns true")
    void emptyCollectionValidation(@ForAll boolean useNull) {
        // Create order with null or empty collection
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setItems(useNull ? null : new ArrayList<>());
        
        // Create a filter
        OrderItemFilter itemFilter = new OrderItemFilter();
        itemFilter.setProductId(new com.thy.fss.common.inmemory.filter.LongFilter());
        itemFilter.getProductId().setEquals(1L);
        
        // Get the generated specification service
        OrderSpecificationService service = OrderSpecificationService.INSTANCE;
        
        // Test ANY operator - should return false
        OrderFilter anyFilter = new OrderFilter();
        anyFilter.setItems(new CollectionFilter<>());
        anyFilter.getItems().setCollectionAny(itemFilter);
        boolean anyResult = service.validateFilter(order, anyFilter);
        assert !anyResult : "ANY operator should return false for empty/null collection";
        
        // Test ALL operator - should return false
        OrderFilter allFilter = new OrderFilter();
        allFilter.setItems(new CollectionFilter<>());
        allFilter.getItems().setCollectionAll(itemFilter);
        boolean allResult = service.validateFilter(order, allFilter);
        assert !allResult : "ALL operator should return false for empty/null collection";
        
        // Test NONE operator - should return true
        OrderFilter noneFilter = new OrderFilter();
        noneFilter.setItems(new CollectionFilter<>());
        noneFilter.getItems().setCollectionNone(itemFilter);
        boolean noneResult = service.validateFilter(order, noneFilter);
        assert noneResult : "NONE operator should return true for empty/null collection";
    }
}
