package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StorePropertyMappingBuilder with nested property support.
 */
@DisplayName("Store Property Mapping Builder Integration Tests")
class StorePropertyMappingBuilderIntegrationTest {

    private InMemorySpecStoreFactory factory;
    private InMemoryDataSource<Order> orderDataSource;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Address> addressDataSource;
    private InMemoryDataSource<City> cityDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        
        // Create test data
        City istanbul = new City(1L, "Istanbul");
        City ankara = new City(2L, "Ankara");
        
        Address addr1 = new Address(1L, "Street 1", 1L);
        Address addr2 = new Address(2L, "Street 2", 2L);
        
        Customer customer1 = new Customer(1L, "John", 1L);
        Customer customer2 = new Customer(2L, "Jane", 2L);
        
        Order order1 = new Order(1L, 1L, 100.0);
        Order order2 = new Order(2L, 1L, 200.0);
        Order order3 = new Order(3L, 2L, 150.0);
        
        // Register datasources
        cityDataSource = new InMemoryDataSource<>("cities", City.class, Arrays.asList(istanbul, ankara));
        addressDataSource = new InMemoryDataSource<>("addresses", Address.class, Arrays.asList(addr1, addr2));
        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, Arrays.asList(customer1, customer2));
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, Arrays.asList(order1, order2, order3));
        
        factory.registerDataSource("cities", cityDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("addresses", addressDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
        if (cityDataSource != null) cityDataSource.close();
        if (addressDataSource != null) addressDataSource.close();
        if (customerDataSource != null) customerDataSource.close();
        if (orderDataSource != null) orderDataSource.close();
    }

    @Test
    @DisplayName("Should create store with simple nested target property")
    void testSimpleNestedTargetProperty() {
        // This test verifies the new API structure
        InMemoryStoreBuilder<Order> builder = factory
                .buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class);
        builder.target(Order_.customerId)  // Single target() call with MetaAttribute
                    .from(
                        CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.customerId),
                        fkb -> fkb.field(Customer_.id)
                    )
                    .value(sfb -> sfb.field(Customer_.name));
        InMemoryDataStore<Order> store = builder.build();

        assertThat(store).isNotNull();
        assertThat(store.getPropertyMappings()).hasSize(1);
    }


    @Test
    @DisplayName("Should create store with count aggregation")
    void testCountAggregation() {
        InMemoryStoreBuilder<Customer> builder = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);
        builder
                .target(Customer_.id)  // Using id field as target for count
                    .from(
                        OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                    )
                    .count();
        InMemoryDataStore<Customer> store = builder.build();

        assertThat(store).isNotNull();
        assertThat(store.getPropertyMappings()).hasSize(1);
    }

    @Test
    @DisplayName("Should create store with sum aggregation")
    void testSumAggregation() {
        InMemoryStoreBuilder<Customer> builder2 = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);
        builder2
                .target(Customer_.id)  // Using id field as target
                    .from(
                        OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                    )
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        InMemoryDataStore<Customer> store = builder2.build();

        assertThat(store).isNotNull();
        assertThat(store.getPropertyMappings()).hasSize(1);
    }
}
