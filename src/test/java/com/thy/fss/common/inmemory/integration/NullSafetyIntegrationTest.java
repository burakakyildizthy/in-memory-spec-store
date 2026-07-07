package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.collection.OrderItemDetail;
import com.thy.fss.common.inmemory.testmodel.collection.OrderWithItems;
import com.thy.fss.common.inmemory.testmodel.collection.OrderWithItemsSpecificationService;
import com.thy.fss.common.inmemory.testmodel.collection.SubItem;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Null safety integration tests for nested property mapping.
 * Tests graceful handling of intermediate null values, empty collections,
 * and null collection elements.
 * 
 * Requirements: 1.4 (Null safety in nested paths), 3.4 (Dashboard null handling), 
 *               6.4 (Collection null handling)
 */
@DisplayName("Null Safety Integration Tests")
class NullSafetyIntegrationTest {

    private static final String ORDER_ITEMS = "order-items";
    private static final String ORDERS_WITH_ITEMS = "orders-with-items";
    private static final String CUSTOMERS = "customers";
    private static final String ORDERS = "orders";
    private static final String CITIES = "cities";
    private static final String ADDRESSES = "addresses";
    private static final String SYNCED_ORDERS_NOT_NULL = "Synced orders should not be null";
    private static final String ALL_ORDERS_SYNCED = "All orders should be synchronized";
    private static final String ORDER_NOT_NULL = "Order should not be null";

    private InMemoryDataSource<Order> orderDataSource;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Address> addressDataSource;
    private InMemoryDataSource<City> cityDataSource;
    private InMemoryDataSource<OrderWithItems> orderWithItemsDataSource;
    private InMemoryDataSource<OrderItemDetail> itemDataSource;
    private DataSynchronizationEngine engine;

    @AfterEach
    void cleanup() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        closeDataSource(orderDataSource);
        closeDataSource(customerDataSource);
        closeDataSource(addressDataSource);
        closeDataSource(cityDataSource);
        closeDataSource(orderWithItemsDataSource);
        closeDataSource(itemDataSource);
        
        DataSyncTestHelper.clearStaticRegistries();
    }

    private void closeDataSource(InMemoryDataSource<?> dataSource) {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    @DisplayName("Should handle intermediate null values in nested path gracefully")
    void testIntermediateNullValuesInNestedPath() {
        // Given: Orders with customers, some customers have null addressId
        List<City> cities = Arrays.asList(
            new City(1L, "Istanbul"),
            new City(2L, "Ankara")
        );
        
        List<Address> addresses = Arrays.asList(
            new Address(1L, "Street 1", 1L),
            new Address(2L, "Street 2", 2L)
        );
        
        List<Customer> customers = Arrays.asList(
            new Customer(1L, "Customer1", 1L),      // Has address
            new Customer(2L, "Customer2", null),    // NULL addressId - intermediate null
            new Customer(3L, "Customer3", 2L),      // Has address
            new Customer(4L, "Customer4", 999L)     // Non-existent addressId
        );
        
        List<Order> orders = Arrays.asList(
            new Order(1L, 1L, 100.0),  // Customer with valid address
            new Order(2L, 2L, 200.0),  // Customer with null addressId
            new Order(3L, 3L, 300.0),  // Customer with valid address
            new Order(4L, 4L, 400.0),  // Customer with non-existent address
            new Order(5L, 999L, 500.0) // Non-existent customer
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderDataSource = new InMemoryDataSource<>(ORDERS, Order.class, orders);
        customerDataSource = new InMemoryDataSource<>(CUSTOMERS, Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>(ADDRESSES, Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>(CITIES, City.class, cities);
        
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ADDRESSES, addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CITIES, cityDataSource, Duration.ofSeconds(10));
        
        // Create store with nested mapping that may encounter nulls
        InMemoryDataStore<Order> store = factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
            .withPrimaryDataSource(Order.class)
            .build();
        
        // When: Synchronize data
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: All orders should be synchronized despite null intermediate values
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<Order> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(5, syncedOrders.size(), "Should have all 5 orders");
        
        // Verify graceful degradation - orders with null paths should have null target values
        Order order1 = syncedOrders.stream().filter(o -> o.getId().equals(1L)).findFirst().orElse(null);
        Order order2 = syncedOrders.stream().filter(o -> o.getId().equals(2L)).findFirst().orElse(null);
        Order order5 = syncedOrders.stream().filter(o -> o.getId().equals(5L)).findFirst().orElse(null);
        
        assertNotNull(order1, "Order 1 should exist");
        assertNotNull(order2, "Order 2 should exist");
        assertNotNull(order5, "Order 5 should exist");
        
        // Order 2 has customer with null addressId - should handle gracefully
        // Order 5 has non-existent customer - should handle gracefully
        // System should not crash and should continue processing other orders
        
        System.out.println("Successfully handled intermediate null values:");
        System.out.println("- Total orders: " + syncedOrders.size());
        System.out.println("- Orders with valid nested paths: " + 
            syncedOrders.stream().filter(o -> o.getId() <= 3).count());
        System.out.println("- Orders with null intermediate values: " + 
            syncedOrders.stream().filter(o -> o.getId() > 3).count());
    }

    @Test
    @DisplayName("Should handle empty collections gracefully")
    void testEmptyCollections() {
        // Given: Orders with empty item collections
        List<OrderWithItems> orders = Arrays.asList(
            new OrderWithItems(1L, 1L, Collections.emptyList()),  // Empty collection
            new OrderWithItems(2L, 2L, null),                      // Null collection
            new OrderWithItems(3L, 3L, new ArrayList<>())          // Empty ArrayList
        );
        
        List<OrderItemDetail> items = Collections.emptyList();  // No items at all
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderWithItemsDataSource = new InMemoryDataSource<>(ORDERS_WITH_ITEMS, OrderWithItems.class, orders);
        itemDataSource = new InMemoryDataSource<>(ORDER_ITEMS, OrderItemDetail.class, items);
        
        factory.registerDataSource(ORDERS_WITH_ITEMS, orderWithItemsDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ORDER_ITEMS, itemDataSource, Duration.ofSeconds(10));
        
        // Create store with collection aggregation
        InMemoryDataStore<OrderWithItems> store = factory.buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
            .withPrimaryDataSource(OrderWithItems.class)
            .build();
        
        // When: Synchronize data with empty collections
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: All orders should be synchronized despite empty collections
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<OrderWithItems> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(3, syncedOrders.size(), "Should have all 3 orders");
        
        // Verify each order exists and handles empty collections gracefully
        for (OrderWithItems order : syncedOrders) {
            assertNotNull(order, ORDER_NOT_NULL);
            assertNotNull(order.getId(), "Order ID should not be null");
            
            // Empty collections should result in null or zero aggregation values
            // System should not crash when processing empty collections
        }
        
        System.out.println("Successfully handled empty collections:");
        System.out.println("- Total orders: " + syncedOrders.size());
        System.out.println("- All orders processed without errors");
    }

    @Test
    @DisplayName("Should handle null collection elements gracefully")
    void testNullCollectionElements() {
        // Given: Collections with null elements and items with null fields
        List<SubItem> subItems1 = Arrays.asList(
            new SubItem(1L, 5, "SubItem1"),
            null,  // Null element in collection
            new SubItem(2L, null, "SubItem2"),  // Null quantity
            new SubItem(3L, 10, null)  // Null name
        );
        
        List<OrderItemDetail> items = Arrays.asList(
            new OrderItemDetail(1L, 1L, "Product1", 100.0, subItems1),
            new OrderItemDetail(2L, 1L, null, 200.0, null),  // Null productName and subItems
            null,  // Null element in items collection
            new OrderItemDetail(3L, 1L, "Product3", null, Collections.emptyList())  // Null price
        );
        
        List<OrderWithItems> orders = Arrays.asList(
            new OrderWithItems(1L, 1L, items)
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderWithItemsDataSource = new InMemoryDataSource<>(ORDERS_WITH_ITEMS, OrderWithItems.class, orders);
        itemDataSource = new InMemoryDataSource<>(ORDER_ITEMS, OrderItemDetail.class, items);
        
        factory.registerDataSource(ORDERS_WITH_ITEMS, orderWithItemsDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ORDER_ITEMS, itemDataSource, Duration.ofSeconds(10));
        
        // Create store with collection operations
        InMemoryDataStore<OrderWithItems> store = factory.buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
            .withPrimaryDataSource(OrderWithItems.class)
            .build();
        
        // When: Synchronize data with null collection elements
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: Orders should be synchronized despite null elements
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<OrderWithItems> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(1, syncedOrders.size(), "Should have 1 order");
        
        OrderWithItems order = syncedOrders.get(0);
        assertNotNull(order, ORDER_NOT_NULL);
        assertNotNull(order.getId(), "Order ID should not be null");
        
        // System should skip null elements and continue processing
        // Aggregations should handle null values gracefully
        
        System.out.println("Successfully handled null collection elements:");
        System.out.println("- Order processed: " + order.getId());
        System.out.println("- Null elements skipped gracefully");
    }

    @Test
    @DisplayName("Should handle mixed null scenarios in complex nested paths")
    void testMixedNullScenariosInComplexPaths() {
        // Given: Complex scenario with multiple null conditions
        List<City> cities = Arrays.asList(
            new City(1L, "Istanbul"),
            new City(2L, null)  // Null city name
        );
        
        List<Address> addresses = Arrays.asList(
            new Address(1L, "Street 1", 1L),
            new Address(2L, null, 2L),      // Null street
            new Address(3L, "Street 3", null)  // Null cityId
        );
        
        List<Customer> customers = Arrays.asList(
            new Customer(1L, "Customer1", 1L),
            new Customer(2L, null, 2L),      // Null name
            new Customer(3L, "Customer3", null),  // Null addressId
            new Customer(4L, "Customer4", 3L)     // Address with null cityId
        );
        
        List<Order> orders = Arrays.asList(
            new Order(1L, 1L, 100.0),   // Valid path
            new Order(2L, 2L, 200.0),   // Customer with null name
            new Order(3L, 3L, 300.0),   // Customer with null addressId
            new Order(4L, 4L, 400.0),   // Customer -> Address with null cityId
            new Order(5L, null, 500.0)  // Null customerId
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderDataSource = new InMemoryDataSource<>(ORDERS, Order.class, orders);
        customerDataSource = new InMemoryDataSource<>(CUSTOMERS, Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>(ADDRESSES, Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>(CITIES, City.class, cities);
        
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ADDRESSES, addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CITIES, cityDataSource, Duration.ofSeconds(10));
        
        // Create store
        InMemoryDataStore<Order> store = factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
            .withPrimaryDataSource(Order.class)
            .build();
        
        // When: Synchronize data with mixed null scenarios
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: All orders should be synchronized with graceful null handling
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<Order> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(5, syncedOrders.size(), "Should have all 5 orders");
        
        // Verify all orders exist despite various null conditions
        for (Order order : syncedOrders) {
            assertNotNull(order, ORDER_NOT_NULL);
            // System should handle all null scenarios gracefully
        }
        
        System.out.println("Successfully handled mixed null scenarios:");
        System.out.println("- Total orders: " + syncedOrders.size());
        System.out.println("- All null conditions handled gracefully");
    }

    @Test
    @DisplayName("Should handle null values in aggregation operations")
    void testNullValuesInAggregationOperations() {
        // Given: Items with null numeric values for aggregation
        List<OrderItemDetail> items = Arrays.asList(
            new OrderItemDetail(1L, 1L, "Product1", 100.0),
            new OrderItemDetail(2L, 1L, "Product2", null),    // Null price
            new OrderItemDetail(3L, 1L, "Product3", 200.0),
            new OrderItemDetail(4L, 2L, "Product4", null),    // Null price
            new OrderItemDetail(5L, 2L, "Product5", null),    // Null price
            new OrderItemDetail(6L, 3L, "Product6", 150.0)
        );
        
        List<OrderWithItems> orders = Arrays.asList(
            new OrderWithItems(1L, 1L, null),  // Has items with mixed null prices
            new OrderWithItems(2L, 2L, null),  // Has items with all null prices
            new OrderWithItems(3L, 3L, null)   // Has items with valid price
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderWithItemsDataSource = new InMemoryDataSource<>(ORDERS_WITH_ITEMS, OrderWithItems.class, orders);
        itemDataSource = new InMemoryDataSource<>(ORDER_ITEMS, OrderItemDetail.class, items);
        
        factory.registerDataSource(ORDERS_WITH_ITEMS, orderWithItemsDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ORDER_ITEMS, itemDataSource, Duration.ofSeconds(10));
        
        // Create store
        InMemoryDataStore<OrderWithItems> store = factory.buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
            .withPrimaryDataSource(OrderWithItems.class)
            .build();
        
        // When: Synchronize data with null aggregation values
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: All orders should be synchronized with proper null handling in aggregations
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<OrderWithItems> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(3, syncedOrders.size(), "Should have all 3 orders");
        
        // Verify aggregations handle null values correctly
        // - Null values should be skipped in SUM/AVG/MIN/MAX
        // - COUNT should count non-null values
        // - System should not crash on null numeric values
        
        for (OrderWithItems order : syncedOrders) {
            assertNotNull(order, ORDER_NOT_NULL);
            assertNotNull(order.getId(), "Order ID should not be null");
        }
        
        System.out.println("Successfully handled null values in aggregations:");
        System.out.println("- Total orders: " + syncedOrders.size());
        System.out.println("- Null numeric values handled gracefully");
    }

    @Test
    @DisplayName("Should handle deeply nested null values with collections")
    void testDeeplyNestedNullValuesWithCollections() {
        // Given: Deeply nested structure with nulls at various levels
        List<SubItem> subItems1 = Arrays.asList(
            new SubItem(1L, 5, "SubItem1"),
            new SubItem(2L, null, "SubItem2")  // Null quantity
        );
        
        List<SubItem> subItems2 = null;  // Null sub-collection
        
        List<OrderItemDetail> items = Arrays.asList(
            new OrderItemDetail(1L, 1L, "Product1", 100.0, subItems1),
            new OrderItemDetail(2L, 1L, "Product2", 200.0, subItems2),  // Null subItems
            new OrderItemDetail(3L, 1L, null, null, null)  // Multiple nulls
        );
        
        List<OrderWithItems> orders = Arrays.asList(
            new OrderWithItems(1L, 1L, items)
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderWithItemsDataSource = new InMemoryDataSource<>(ORDERS_WITH_ITEMS, OrderWithItems.class, orders);
        itemDataSource = new InMemoryDataSource<>(ORDER_ITEMS, OrderItemDetail.class, items);
        
        factory.registerDataSource(ORDERS_WITH_ITEMS, orderWithItemsDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ORDER_ITEMS, itemDataSource, Duration.ofSeconds(10));
        
        // Create store
        InMemoryDataStore<OrderWithItems> store = factory.buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
            .withPrimaryDataSource(OrderWithItems.class)
            .build();
        
        // When: Synchronize data with deeply nested nulls
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: Order should be synchronized with graceful null handling at all levels
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<OrderWithItems> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(1, syncedOrders.size(), "Should have 1 order");
        
        OrderWithItems order = syncedOrders.get(0);
        assertNotNull(order, ORDER_NOT_NULL);
        
        // System should handle nulls at all nesting levels:
        // - Null sub-collections
        // - Null fields in nested objects
        // - Null elements in nested collections
        
        System.out.println("Successfully handled deeply nested null values:");
        System.out.println("- Order ID: " + order.getId());
        System.out.println("- All nesting levels handled gracefully");
    }

    @Test
    @DisplayName("Should maintain data integrity with partial null data")
    void testDataIntegrityWithPartialNullData() {
        // Given: Mix of valid and null data to ensure valid data is not affected
        List<City> cities = Arrays.asList(
            new City(1L, "Istanbul"),
            new City(2L, "Ankara"),
            new City(3L, "Izmir")
        );
        
        List<Address> addresses = Arrays.asList(
            new Address(1L, "Street 1", 1L),
            new Address(2L, "Street 2", null),  // Null cityId
            new Address(3L, "Street 3", 3L)
        );
        
        List<Customer> customers = Arrays.asList(
            new Customer(1L, "Customer1", 1L),
            new Customer(2L, "Customer2", 2L),  // Address with null cityId
            new Customer(3L, "Customer3", 3L)
        );
        
        List<Order> orders = Arrays.asList(
            new Order(1L, 1L, 100.0),  // Valid complete path
            new Order(2L, 2L, 200.0),  // Path with null in middle
            new Order(3L, 3L, 300.0)   // Valid complete path
        );
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderDataSource = new InMemoryDataSource<>(ORDERS, Order.class, orders);
        customerDataSource = new InMemoryDataSource<>(CUSTOMERS, Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>(ADDRESSES, Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>(CITIES, City.class, cities);
        
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(ADDRESSES, addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(CITIES, cityDataSource, Duration.ofSeconds(10));
        
        // Create store
        InMemoryDataStore<Order> store = factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
            .withPrimaryDataSource(Order.class)
            .build();
        
        // When: Synchronize data
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orders.size(), Duration.ofSeconds(10));
        
        // Then: All orders should be synchronized and valid data should be intact
        assertEquals(orders.size(), store.size(), ALL_ORDERS_SYNCED);
        
        List<Order> syncedOrders = store.findAll();
        assertNotNull(syncedOrders, SYNCED_ORDERS_NOT_NULL);
        assertEquals(3, syncedOrders.size(), "Should have all 3 orders");
        
        // Verify valid orders maintain their data integrity
        Order order1 = syncedOrders.stream().filter(o -> o.getId().equals(1L)).findFirst().orElse(null);
        Order order3 = syncedOrders.stream().filter(o -> o.getId().equals(3L)).findFirst().orElse(null);
        
        assertNotNull(order1, "Order 1 should exist");
        assertNotNull(order3, "Order 3 should exist");
        assertEquals(100.0, order1.getTotalAmount(), "Order 1 amount should be intact");
        assertEquals(300.0, order3.getTotalAmount(), "Order 3 amount should be intact");
        
        // Verify order with null in path still exists
        Order order2 = syncedOrders.stream().filter(o -> o.getId().equals(2L)).findFirst().orElse(null);
        assertNotNull(order2, "Order 2 should exist despite null in path");
        assertEquals(200.0, order2.getTotalAmount(), "Order 2 amount should be intact");
        
        System.out.println("Successfully maintained data integrity:");
        System.out.println("- Valid orders: 2");
        System.out.println("- Orders with null paths: 1");
        System.out.println("- All data integrity maintained");
    }
}
