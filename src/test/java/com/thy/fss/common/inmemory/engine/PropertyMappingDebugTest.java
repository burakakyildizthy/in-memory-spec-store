package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Debug test to verify PropertyMapping registration and retrieval.
 * Tests WITHOUT initializing DataSynchronizationEngine to avoid infinite loops.
 */
@DisplayName("PropertyMapping Debug Test")
class PropertyMappingDebugTest {

    // Constants for duplicate string literals
    private static final String DS_CUSTOMERS = "customers";
    private static final String DS_ORDERS = "orders";
    private static final Duration CACHE_DURATION = Duration.ofSeconds(10);
    private static final String LOG_DATASOURCES_REGISTERED = "DataSources registered";
    private static final String LOG_BUILDING_STORE = "Building store with PropertyMapping...";
    private static final String LOG_STORE_BUILT = "Store built successfully";
    private static final String LOG_COUNT = "Count: ";
    private static final String LOG_STORE_HAS = "Store has ";
    private static final String LOG_PROPERTY_MAPPING_S = " PropertyMapping(s)";
    private static final String ASSERT_BUILDER_CREATE = "Builder MUST create PropertyMappings";
    private static final String ASSERT_FACTORY_RETRIEVE = "Factory MUST retrieve PropertyMappings from Store";

    private InMemorySpecStoreFactory factory;
    private InMemoryDataSource<Customer> customerDs;
    private InMemoryDataSource<Order> orderDs;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (customerDs != null) {
            customerDs.close();
        }
        if (orderDs != null) {
            orderDs.close();
        }
        
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Step 1: Test if Builder creates PropertyMappings")
    void testBuilderCreatesPropertyMappings() {
        System.out.println("\n=== STEP 1: Testing Builder PropertyMapping Creation ===");
        
        // Minimal setup - no data needed for this test
        Customer customer1 = new Customer();
        customer1.setId(100L);
        List<Customer> customers = Arrays.asList(customer1);

        Order order1 = new Order();
        order1.setId(1L);
        List<Order> orders = Arrays.asList(order1);

        customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
        orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);

        factory.registerDataSource(Customer.class, customerDs, CACHE_DURATION);
        factory.registerDataSource(Order.class, orderDs, CACHE_DURATION);

        System.out.println(LOG_DATASOURCES_REGISTERED);

        // Build store with PropertyMapping
        System.out.println(LOG_BUILDING_STORE);
        // Build store
        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget = builder.target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> store = ((InMemoryStoreBuilder<Customer>) afterTarget).build();

        System.out.println(LOG_STORE_BUILT);

        // CRITICAL TEST: Check if store has PropertyMappings
        List<PropertyMapping<Customer, ?>> storeMappings = store.getPropertyMappings();
        System.out.println("\n=== RESULT: Store PropertyMappings ===");
        System.out.println(LOG_COUNT + storeMappings.size());

        if (storeMappings.isEmpty()) {
            System.out.println("❌ PROBLEM FOUND: Builder did NOT create PropertyMappings!");
            System.out.println("This means the builder chain is broken.");
        } else {
            System.out.println("✓ Builder successfully created PropertyMappings");
            storeMappings.forEach(m -> System.out.println("  - " + m));
        }

        assertFalse(storeMappings.isEmpty(), ASSERT_BUILDER_CREATE);
    }

    @Test
    @DisplayName("Step 2: Test if Factory retrieves PropertyMappings from Store")
    void testFactoryRetrievesPropertyMappings() {
        System.out.println("\n=== STEP 2: Testing Factory PropertyMapping Retrieval ===");
        
        // Minimal setup
        Customer customer1 = new Customer();
        customer1.setId(100L);
        List<Customer> customers = Arrays.asList(customer1);

        Order order1 = new Order();
        order1.setId(1L);
        List<Order> orders = Arrays.asList(order1);

        customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
        orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);

        factory.registerDataSource(Customer.class, customerDs, CACHE_DURATION);
        factory.registerDataSource(Order.class, orderDs, CACHE_DURATION);

        // Build store
        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget = builder.target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> store = ((InMemoryStoreBuilder<Customer>) afterTarget).build();

        System.out.println(LOG_STORE_HAS + store.getPropertyMappings().size() + LOG_PROPERTY_MAPPING_S);

        // CRITICAL TEST: Check if factory can retrieve PropertyMappings
        List<PropertyMapping<?, ?>> allMappings = factory.getAllPropertyMappings();
        System.out.println("\n=== RESULT: Factory getAllPropertyMappings ===");
        System.out.println(LOG_COUNT + allMappings.size());

        if (allMappings.isEmpty()) {
            System.out.println("❌ PROBLEM FOUND: Factory.getAllPropertyMappings() returns empty!");
            System.out.println("This means Factory cannot retrieve PropertyMappings from Store.");
        } else {
            System.out.println("✓ Factory successfully retrieved PropertyMappings");
            allMappings.forEach(m -> System.out.println("  - " + m));
        }

        assertFalse(allMappings.isEmpty(), ASSERT_FACTORY_RETRIEVE);
    }
}
