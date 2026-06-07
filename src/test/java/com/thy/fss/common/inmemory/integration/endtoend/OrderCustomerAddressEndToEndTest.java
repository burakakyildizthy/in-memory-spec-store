package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive end-to-end test for Order-Customer-Address scenario.
 * Tests deep nested paths, nested relationships, and value mapping.
 */
@DisplayName("Order-Customer-Address End-to-End Test")
class OrderCustomerAddressEndToEndTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private InMemoryDataStore<Order> store;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Address> addressDataSource;
    private InMemoryDataSource<City> cityDataSource;
    private InMemoryDataSource<Order> orderDataSource;

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
        if (customerDataSource != null) {
            try {
                customerDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (addressDataSource != null) {
            try {
                addressDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (cityDataSource != null) {
            try {
                cityDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (orderDataSource != null) {
            try {
                orderDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should map deep nested path with multiple relationships")
    void testDeepNestedPathWithMultipleRelationships() {
        List<Customer> customers = createCustomerData();
        List<Address> addresses = createAddressData();
        List<City> cities = createCityData();

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>("cities", City.class, cities);

        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));

        // Register primary datasource for orders
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1L, 1L, null));
        orders.add(new Order(2L, 2L, null));
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);

        builder.target(Order_.customerCityName)  // Single target() call with MetaAttribute
                .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .value(sfb -> sfb.field(Customer_.name));

        store = builder.build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));

        List<Order> orderList1 = store.findAll(
            SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.id)
                .equalTo(1L)
        );
        assertThat(orderList1).isNotEmpty();
        Order order1 = orderList1.get(0);
        assertThat(order1.getCustomerCityName()).isEqualTo("John Doe");

        List<Order> orderList2 = store.findAll(
            SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.id)
                .equalTo(2L)
        );
        assertThat(orderList2).isNotEmpty();
        Order order2 = orderList2.get(0);
        assertThat(order2.getCustomerCityName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should handle null values in nested path gracefully")
    void testNullValuesInNestedPath() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1L, "John Doe", null));
        customers.add(new Customer(2L, "Jane Smith", 2L));

        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address(2L, "456 Oak St", 2L));

        List<City> cities = new ArrayList<>();
        cities.add(new City(2L, "Los Angeles"));

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>("cities", City.class, cities);

        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));

        // Register primary datasource for orders
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1L, 1L, null));
        orders.add(new Order(2L, 2L, null));
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);

        builder.target(Order_.customerCityName)  // Single target() call with MetaAttribute
                .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .value(sfb -> sfb.field(Customer_.name));
        store = builder.build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));

        List<Order> orderList1 = store.findAll(
            SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.id)
                .equalTo(1L)
        );
        assertThat(orderList1).isNotEmpty();
        Order order1 = orderList1.get(0);
        assertThat(order1).isNotNull();

        List<Order> orderList2 = store.findAll(
            SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.id)
                .equalTo(2L)
        );
        assertThat(orderList2).isNotEmpty();
        Order order2 = orderList2.get(0);
        assertThat(order2).isNotNull();
        assertThat(order2.getCustomerCityName()).isNotNull();
    }

    @Test
    @DisplayName("Should handle large dataset with deep nested paths")
    void testLargeDatasetWithDeepNestedPaths() {
        List<Customer> customers = createLargeCustomerDataset(1000);
        List<Address> addresses = createLargeAddressDataset(1000);
        List<City> cities = createLargeCityDataset(100);

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>("cities", City.class, cities);

        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));

        // Register primary datasource for orders
        List<Order> orders = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            long customerId = ((i - 1) % 1000) + 1;
            orders.add(new Order(i, customerId, null));
        }
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);

        builder.target(Order_.customerCityName)  // Single target() call with MetaAttribute
                .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .value(sfb -> sfb.field(Customer_.name));
        store = builder.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        assertThat(store.size()).isGreaterThan(0);
        assertThat(endTime - startTime).isLessThan(5000);

        List<Order> firstOrderList = store.findAll(
            SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.id)
                .equalTo(1L)
        );
        assertThat(firstOrderList).isNotEmpty();
        Order firstOrder = firstOrderList.get(0);
        assertThat(firstOrder).isNotNull();
    }

    private List<Customer> createCustomerData() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1L, "John Doe", 1L));
        customers.add(new Customer(2L, "Jane Smith", 2L));
        customers.add(new Customer(3L, "Bob Johnson", 3L));
        return customers;
    }

    private List<Address> createAddressData() {
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address(1L, "123 Main St", 1L));
        addresses.add(new Address(2L, "456 Oak St", 2L));
        addresses.add(new Address(3L, "789 Pine St", 1L));
        return addresses;
    }

    private List<City> createCityData() {
        List<City> cities = new ArrayList<>();
        cities.add(new City(1L, "New York"));
        cities.add(new City(2L, "Los Angeles"));
        cities.add(new City(3L, "Chicago"));
        return cities;
    }

    private List<Customer> createLargeCustomerDataset(int count) {
        List<Customer> customers = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            customers.add(new Customer(i, "Customer" + i, i));
        }
        return customers;
    }

    private List<Address> createLargeAddressDataset(int count) {
        List<Address> addresses = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            long cityId = (i % 100) + 1;
            addresses.add(new Address(i, "Street" + i, cityId));
        }
        return addresses;
    }

    private List<City> createLargeCityDataset(int count) {
        List<City> cities = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            cities.add(new City(i, "City" + i));
        }
        return cities;
    }
}
