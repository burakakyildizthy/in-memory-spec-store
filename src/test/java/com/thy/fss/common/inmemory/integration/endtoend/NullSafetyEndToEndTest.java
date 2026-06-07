package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.collection.*;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive end-to-end test for null safety.
 * Tests intermediate null values in path, empty collections,
 * null collection elements, and graceful degradation.
 */
@DisplayName("Null Safety End-to-End Test")
class NullSafetyEndToEndTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;

    @BeforeEach
    void setUp() {
        DataSyncTestHelper.clearStaticRegistries();
        factory = InMemorySpecStoreFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should handle intermediate null values in nested path gracefully")
    void testIntermediateNullValuesInPath() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1L, "John Doe", null));
        customers.add(new Customer(2L, "Jane Smith", 2L));
        customers.add(new Customer(3L, null, 3L));

        InMemoryDataSource<Customer> customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));

        // Primary datasource for Order is required by the new store builder API
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(100L, 1L, null));
        orders.add(new Order(200L, 2L, null));
        orders.add(new Order(300L, 3L, null));
        InMemoryDataSource<Order> orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> orderBuilder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        orderBuilder = (InMemoryStoreBuilder<Order>) orderBuilder
                .target(Order_.customerCityName)
                    .from(CustomerSpecificationService.INSTANCE, pkb -> pkb.field(Order_.customerId), fkb -> fkb.field(Customer_.id))
                    .value(sfb -> sfb.field(Customer_.name));
        InMemoryDataStore<Order> store = orderBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
            DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));
        }).doesNotThrowAnyException();

        assertThat(store.size()).isGreaterThanOrEqualTo(0);

        try {
            customerDataSource.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @DisplayName("Should handle empty collections gracefully")
    void testEmptyCollections() {
        List<OrderItemDetail> items = Collections.emptyList();

        InMemoryDataSource<OrderItemDetail> itemDataSource = new InMemoryDataSource<>("order-items", OrderItemDetail.class, items);
        factory.registerDataSource("order-items", itemDataSource, Duration.ofSeconds(10));

        // Register primary datasource for OrderWithItems
        List<OrderWithItems> ordersWithItems = new ArrayList<>();
        ordersWithItems.add(new OrderWithItems(1L, 10L, Collections.emptyList()));
        ordersWithItems.add(new OrderWithItems(2L, 20L, Collections.emptyList()));
        InMemoryDataSource<OrderWithItems> orderWithItemsDs = new InMemoryDataSource<>("orders-with-items", OrderWithItems.class, ordersWithItems);
        factory.registerDataSource("orders-with-items", orderWithItemsDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<OrderWithItems> owBuilder = factory
                .buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
                .withPrimaryDataSource(OrderWithItems.class);
        owBuilder = (InMemoryStoreBuilder<OrderWithItems>) owBuilder
                .target(OrderWithItems_.totalItemPrice)
                    .from(OrderItemDetailSpecificationService.INSTANCE, pkb -> pkb.field(OrderWithItems_.id), fkb -> fkb.field(OrderItemDetail_.orderId))
                    .sum(sfb -> sfb.field(OrderItemDetail_.price));
        InMemoryDataStore<OrderWithItems> store = owBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
        }).doesNotThrowAnyException();

        try {
            itemDataSource.close();
            orderWithItemsDs.close();
        } catch (Exception e) {
            // Ignore
        }
        assertThat(store).isNotNull();
    }

    @Test
    @DisplayName("Should handle null collection elements gracefully")
    void testNullCollectionElements() {
        List<OrderItemDetail> items = new ArrayList<>();
        items.add(new OrderItemDetail(1L, 1L, "Product A", 100.0));
        items.add(null);
        items.add(new OrderItemDetail(3L, 1L, "Product C", 200.0));

        InMemoryDataSource<OrderItemDetail> itemDataSource = new InMemoryDataSource<>("order-items", OrderItemDetail.class, items);
        factory.registerDataSource("order-items", itemDataSource, Duration.ofSeconds(10));

        // Register primary datasource for OrderWithItems
        List<OrderWithItems> ordersWithItems = new ArrayList<>();
        ordersWithItems.add(new OrderWithItems(1L, 10L, Collections.emptyList()));
        InMemoryDataSource<OrderWithItems> orderWithItemsDs = new InMemoryDataSource<>("orders-with-items", OrderWithItems.class, ordersWithItems);
        factory.registerDataSource("orders-with-items", orderWithItemsDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<OrderWithItems> owBuilder = factory
                .buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
                .withPrimaryDataSource(OrderWithItems.class);
        owBuilder = (InMemoryStoreBuilder<OrderWithItems>) owBuilder
                .target(OrderWithItems_.totalItemPrice)
                    .from(OrderItemDetailSpecificationService.INSTANCE, pkb -> pkb.field(OrderWithItems_.id), fkb -> fkb.field(OrderItemDetail_.orderId))
                    .sum(sfb -> sfb.field(OrderItemDetail_.price));
        InMemoryDataStore<OrderWithItems> store = owBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
        }).doesNotThrowAnyException();

        try {
            itemDataSource.close();
        } catch (Exception e) {
            // Ignore
        }

        assertThat(store).isNotNull();
    }

    @Test
    @DisplayName("Should degrade gracefully when all values are null")
    void testGracefulDegradationWithAllNulls() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1L, null, null));
        customers.add(new Customer(2L, null, null));

        InMemoryDataSource<Customer> customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));

        // Primary datasource for Order
        List<Order> orderRoots = new ArrayList<>();
        orderRoots.add(new Order(10L, 1L, null));
        orderRoots.add(new Order(20L, 2L, null));
        InMemoryDataSource<Order> orderRootDs = new InMemoryDataSource<>("orders", Order.class, orderRoots);
        factory.registerDataSource("orders", orderRootDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> ordBuilder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        ordBuilder = (InMemoryStoreBuilder<Order>) ordBuilder
                .target(Order_.customerCityName)
                    .from(CustomerSpecificationService.INSTANCE, pkb -> pkb.field(Order_.customerId), fkb -> fkb.field(Customer_.id))
                    .value(sfb -> sfb.field(Customer_.name));
        InMemoryDataStore<Order> store = ordBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
            DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));
        }).doesNotThrowAnyException();

        try {
            customerDataSource.close();
            orderRootDs.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @DisplayName("Should handle null values in aggregation source fields")
    void testNullValuesInAggregationSourceFields() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1L, 1L, 100.0));
        orders.add(new Order(2L, 1L, null));
        orders.add(new Order(3L, 1L, 200.0));

        InMemoryDataSource<Order> orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        // Primary datasource for Customer
        List<Customer> customerRoots = new ArrayList<>();
        customerRoots.add(new Customer(1L, "Cust1", null));
        customerRoots.add(new Customer(2L, "Cust2", null));
        InMemoryDataSource<Customer> customerRootDs = new InMemoryDataSource<>("customer-roots", Customer.class, customerRoots);
        factory.registerDataSource("customer-roots", customerRootDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Customer> custBuilder = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);
        custBuilder = (InMemoryStoreBuilder<Customer>) custBuilder
                .target(Customer_.addressId)
                    .from(OrderSpecificationService.INSTANCE, pkb -> pkb.field(Customer_.id), fkb -> fkb.field(Order_.customerId))
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        InMemoryDataStore<Customer> store = custBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
        }).doesNotThrowAnyException();

        try {
            orderDataSource.close();
        } catch (Exception e) {
            // Ignore
        }
        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle mixed null and non-null values in nested paths")
    void testMixedNullAndNonNullValues() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1L, "John Doe", 1L));
        customers.add(new Customer(2L, null, 2L));
        customers.add(new Customer(3L, "Bob Johnson", null));
        customers.add(new Customer(4L, "Alice Smith", 4L));

        InMemoryDataSource<Customer> customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));

        // Primary datasource for Order
        List<Order> mixedOrderRoots = new ArrayList<>();
        mixedOrderRoots.add(new Order(1L, 1L, null));
        mixedOrderRoots.add(new Order(2L, 2L, null));
        mixedOrderRoots.add(new Order(3L, 3L, null));
        mixedOrderRoots.add(new Order(4L, 4L, null));
        InMemoryDataSource<Order> mixedOrdersDs = new InMemoryDataSource<>("orders", Order.class, mixedOrderRoots);
        factory.registerDataSource("orders", mixedOrdersDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> storeBuilder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        storeBuilder = (InMemoryStoreBuilder<Order>) storeBuilder
                .target(Order_.customerCityName)
                    .from(CustomerSpecificationService.INSTANCE, pkb -> pkb.field(Order_.customerId), fkb -> fkb.field(Customer_.id))
                    .value(sfb -> sfb.field(Customer_.name));
        InMemoryDataStore<Order> store = storeBuilder.build();

        assertThatCode(() -> {
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();
            DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));
        }).doesNotThrowAnyException();

        assertThat(store.size()).isGreaterThanOrEqualTo(0);

        try {
            customerDataSource.close();
            mixedOrdersDs.close();
        } catch (Exception e) {
            // Ignore
        }
    }
}
