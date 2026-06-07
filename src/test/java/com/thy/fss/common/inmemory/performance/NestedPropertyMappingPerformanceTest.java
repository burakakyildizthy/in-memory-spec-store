package com.thy.fss.common.inmemory.performance;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.collection.*;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance test for nested property mapping with 1,000 entities.
 * Tests 3-level nesting, collection operations, and aggregation timing.
 * 
 * Requirements: 5.x (DataSyncEngine optimization), 6.x (Collection operations)
 */
@DisplayName("Nested Property Mapping Performance Tests")
@Tag("performance")
class NestedPropertyMappingPerformanceTest {

    private InMemoryDataSource<Order> orderDataSource;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Address> addressDataSource;
    private InMemoryDataSource<City> cityDataSource;
    private InMemoryDataSource<OrderItemDetail> itemDataSource;
    private InMemoryDataSource<OrderWithItems> orderWithItemsDataSource;
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
        closeDataSource(itemDataSource);
        closeDataSource(orderWithItemsDataSource);
        
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
    @DisplayName("Should handle 1,000 entities with 3-level nesting efficiently")
    void testThreeLevelNestingPerformance() {
        // Given: 1,000 orders with 3-level nesting (Order -> Customer -> Address -> City)
        int entityCount = 1000;
        PerformanceDataset dataset = generateThreeLevelNestedDataset(entityCount);
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, dataset.orders);
        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, dataset.customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, dataset.addresses);
        cityDataSource = new InMemoryDataSource<>("cities", City.class, dataset.cities);
        
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));
        
        // Create store with 3-level nested mapping
        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        builder.target(Order_.customerCityName)  // Single target() call with MetaAttribute
                .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .value(sfb -> sfb.field(Customer_.addressId));
        InMemoryDataStore<Order> store = builder.build();
        // When: Synchronize data and measure time
        long startTime = System.currentTimeMillis();
        
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == entityCount, Duration.ofSeconds(10));
        
        long syncTime = System.currentTimeMillis() - startTime;
        
        // Then: Verify performance and correctness
        assertEquals(entityCount, store.size(), "All orders should be synchronized");
        
        // Performance check: Should complete within reasonable time (< 5 seconds for 1K entities)
        assertTrue(syncTime < 5000, 
            String.format("Sync should complete within 5 seconds, took %d ms", syncTime));
        
        // Verify data correctness
        List<Order> syncedOrders = store.findAll();
        long mappedCount = syncedOrders.stream()
            .filter(o -> o.getCustomerCityName() != null)
            .count();
        
        assertTrue(mappedCount > 0, "Some orders should have mapped city names");
        
        System.out.printf("Performance: Synchronized %d entities with 3-level nesting in %d ms%n", 
            entityCount, syncTime);
    }

    @Test
    @DisplayName("Should handle collection operations on 1,000 entities efficiently")
    void testCollectionOperationPerformance() {
        // Given: 1,000 orders with collection items
        int orderCount = 1000;
        int avgItemsPerOrder = 5;
        CollectionDataset dataset = generateCollectionDataset(orderCount, avgItemsPerOrder);
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderWithItemsDataSource = new InMemoryDataSource<>("orders-with-items", OrderWithItems.class, dataset.orders);
        itemDataSource = new InMemoryDataSource<>("order-items", OrderItemDetail.class, dataset.items);
        
        factory.registerDataSource("orders-with-items", orderWithItemsDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("order-items", itemDataSource, Duration.ofSeconds(10));

        InMemoryStoreBuilder<OrderWithItems> builder = factory
                .buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
                .withPrimaryDataSource(OrderWithItems.class);
        builder.target(OrderWithItems_.totalItemPrice)  // Single target() call with MetaAttribute
                .from(
                        OrderItemDetailSpecificationService.INSTANCE,
                        pkb -> pkb.field(OrderWithItems_.id),
                        fkb -> fkb.field(OrderItemDetail_.orderId)
                )
                .sum(sfb -> sfb.field(OrderItemDetail_.price));

        builder.target(OrderWithItems_.firstProductName)
                .from(
                        OrderItemDetailSpecificationService.INSTANCE,
                        pkb -> pkb.field(OrderWithItems_.id),
                        fkb -> fkb.field(OrderItemDetail_.orderId)
                )
                .value(sfb -> sfb.field(OrderItemDetail_.productName));
        InMemoryDataStore<OrderWithItems> store = builder.build();
        
        // When: Synchronize data and measure time
        long startTime = System.currentTimeMillis();
        
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == orderCount, Duration.ofSeconds(10));
        
        long syncTime = System.currentTimeMillis() - startTime;
        
        // Then: Verify performance and correctness
        assertEquals(orderCount, store.size(), "All orders should be synchronized");
        
        // Performance check: Should complete within reasonable time (< 5 seconds)
        assertTrue(syncTime < 5000, 
            String.format("Collection sync should complete within 5 seconds, took %d ms", syncTime));
        
        // Verify aggregation correctness
        List<OrderWithItems> syncedOrders = store.findAll();
        long aggregatedCount = syncedOrders.stream()
            .filter(o -> o.getTotalItemPrice() != null && o.getTotalItemPrice() > 0)
            .count();
        
        assertTrue(aggregatedCount > 0, "Some orders should have aggregated prices");
        
        System.out.printf("Performance: Synchronized %d orders with %d items (collection aggregation) in %d ms%n", 
            orderCount, dataset.items.size(), syncTime);
    }

    @Test
    @DisplayName("Should handle aggregation timing efficiently")
    void testAggregationPerformance() {
        // Given: 1,000 orders with aggregation
        int entityCount = 1000;
        PerformanceDataset dataset = generateThreeLevelNestedDataset(entityCount);
        
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, dataset.orders);
        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, dataset.customers);
        
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        builder.target(Order_.totalAmount)  // Single target() call with MetaAttribute
                .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .count();
        // Create store with aggregation
        InMemoryDataStore<Order> store = builder.build();
        // When: Synchronize and measure aggregation time
        long startTime = System.currentTimeMillis();
        
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        DataSyncTestHelper.awaitSync(() -> store.size() == entityCount, Duration.ofSeconds(10));
        
        long aggregationTime = System.currentTimeMillis() - startTime;
        
        // Then: Verify aggregation performance
        assertEquals(entityCount, store.size(), "All orders should be synchronized");
        
        // Performance check: Aggregation should be fast (< 3 seconds)
        assertTrue(aggregationTime < 3000, 
            String.format("Aggregation should complete within 3 seconds, took %d ms", aggregationTime));
        
        System.out.printf("Performance: Aggregated %d entities in %d ms%n", 
            entityCount, aggregationTime);
    }

    private PerformanceDataset generateThreeLevelNestedDataset(int orderCount) {
        Random random = new Random(42);
        
        // Generate cities (10 cities)
        List<City> cities = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            cities.add(new City(i, "City" + i));
        }
        
        // Generate addresses (100 addresses)
        List<Address> addresses = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            long cityId = (i % 10) + 1;
            addresses.add(new Address(i, "Street" + i, cityId));
        }
        
        // Generate customers (500 customers)
        List<Customer> customers = new ArrayList<>();
        for (long i = 1; i <= 500; i++) {
            long addressId = (i % 100) + 1;
            customers.add(new Customer(i, "Customer" + i, addressId));
        }
        
        // Generate orders (1,000 orders)
        List<Order> orders = new ArrayList<>();
        for (long i = 1; i <= orderCount; i++) {
            long customerId = (i % 500) + 1;
            double amount = 100.0 + random.nextDouble() * 900.0;
            orders.add(new Order(i, customerId, amount));
        }
        
        return new PerformanceDataset(orders, customers, addresses, cities);
    }

    private CollectionDataset generateCollectionDataset(int orderCount, int avgItemsPerOrder) {
        Random random = new Random(42);
        
        List<OrderWithItems> orders = new ArrayList<>();
        List<OrderItemDetail> items = new ArrayList<>();
        
        long itemId = 1;
        for (long orderId = 1; orderId <= orderCount; orderId++) {
            long customerId = (orderId % 100) + 1;
            orders.add(new OrderWithItems(orderId, customerId, null));
            
            // Generate items for this order
            int itemCount = avgItemsPerOrder + random.nextInt(3) - 1; // avgItemsPerOrder ± 1
            for (int j = 0; j < itemCount; j++) {
                double price = 10.0 + random.nextDouble() * 90.0;
                items.add(new OrderItemDetail(itemId++, orderId, "Product" + itemId, price));
            }
        }
        
        return new CollectionDataset(orders, items);
    }

    private static class PerformanceDataset {
        final List<Order> orders;
        final List<Customer> customers;
        final List<Address> addresses;
        final List<City> cities;

        PerformanceDataset(List<Order> orders, List<Customer> customers, 
                          List<Address> addresses, List<City> cities) {
            this.orders = orders;
            this.customers = customers;
            this.addresses = addresses;
            this.cities = cities;
        }
    }

    private static class CollectionDataset {
        final List<OrderWithItems> orders;
        final List<OrderItemDetail> items;

        CollectionDataset(List<OrderWithItems> orders, List<OrderItemDetail> items) {
            this.orders = orders;
            this.items = items;
        }
    }
}
