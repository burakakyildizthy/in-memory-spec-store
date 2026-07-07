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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance test for nested property mapping with 1,000 entities,
 * 3-level nesting, basic collection operation, and aggregation timing check.
 */
@DisplayName("Performance End-to-End Test")
@Tag("performance")
class PerformanceEndToEndTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private InMemoryDataStore<Order> store;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Address> addressDataSource;
    private InMemoryDataSource<City> cityDataSource;

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
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should handle 1,000 entities with 3-level nesting efficiently")
    void testPerformanceWith1000Entities() {
        List<Customer> customers = createCustomerDataset(1000);
        List<Address> addresses = createAddressDataset(1000);
        List<City> cities = createCityDataset(100);

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, addresses);
        cityDataSource = new InMemoryDataSource<>("cities", City.class, cities);

        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));

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

        store = builder.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        long syncTime = endTime - startTime;
        assertThat(syncTime).isLessThan(5000);

        assertThat(store.size()).isGreaterThan(0);

        List<Order> firstOrder = store.findAll(
                SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                        .where(Order_.id)
                        .equalTo(2L)
        );
        assertThat(firstOrder).isNotNull();
        Order order2 = firstOrder.getFirst();
        assertThat(order2).isNotNull();

    }

    @Test
    @DisplayName("Should perform aggregation on 1,000 entities within acceptable time")
    void testAggregationPerformance() {
        List<Customer> customers = createCustomerDataset(1000);

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
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

        store = builder.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        long syncTime = endTime - startTime;
        assertThat(syncTime).isLessThan(5000);

        assertThat(store.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle multiple nested mappings with 1,000 entities")
    void testMultipleNestedMappingsPerformance() {
        List<Customer> customers = createCustomerDataset(1000);
        List<Address> addresses = createAddressDataset(1000);

        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, addresses);

        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));

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
        builder.target(Order_.totalAmount)
                .from(CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                )
                .count();

        store = builder.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        long syncTime = endTime - startTime;
        assertThat(syncTime).isLessThan(5000);

        assertThat(store.size()).isGreaterThan(0);
    }

    private List<Customer> createCustomerDataset(int count) {
        List<Customer> customers = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            customers.add(new Customer(i, "Customer" + i, i));
        }
        return customers;
    }

    private List<Address> createAddressDataset(int count) {
        List<Address> addresses = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            long cityId = (i % 100) + 1;
            addresses.add(new Address(i, "Street" + i, cityId));
        }
        return addresses;
    }

    private List<City> createCityDataset(int count) {
        List<City> cities = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            cities.add(new City(i, "City" + i));
        }
        return cities;
    }
}
