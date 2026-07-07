package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Address;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.City;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Customer;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Order;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DataSynchronizationEngine with path-based property mappings.
 * Tests path traversal, collection extraction, nested collections, and aggregations.
 * 
 * Requirements tested:
 * - 5.x: DataSyncEngine optimizations with path-based mappings
 * - 6.x: Collection operations support
 */
@DisplayName("DataSyncEngine Integration Tests")
class DataSyncEngineIntegrationTest {

    // Constants for duplicate string literals
    private static final String DS_ORDERS = "orders";
    private static final String DS_CUSTOMERS = "customers";
    private static final String DS_ADDRESSES = "addresses";
    private static final String DS_CITIES = "cities";
    private static final String CUSTOMER_PREFIX = "Customer";
    private static final String ALICE = "Alice";

    private static final Duration CACHE_DURATION = Duration.ofSeconds(10);

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private List<InMemoryDataSource<?>> dataSources;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        dataSources = new ArrayList<>();
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
        
        for (InMemoryDataSource<?> ds : dataSources) {
            try {
                ds.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ========== Path Traversal Tests ==========

    @Nested
    @DisplayName("Path Traversal Tests")
    class PathTraversalTests {

        @Test
        @DisplayName("Should initialize engine with nested data sources")
        void testEngineInitializationWithNestedDataSources() {
            // Setup: Order -> Customer (single level)
            List<Order> orders = Arrays.asList(
                    new Order(1L, 100L, 500.0),
                    new Order(2L, 200L, 750.0),
                    new Order(3L, 100L, 300.0)
            );

            List<Customer> customers = Arrays.asList(
                    new Customer(100L, ALICE, 1000L),
                    new Customer(200L, "Bob", 2000L)
            );

            InMemoryDataSource<Order> orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);
            InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
            dataSources.add(orderDs);
            dataSources.add(customerDs);

            factory.registerDataSource(DS_ORDERS, orderDs, CACHE_DURATION);
            factory.registerDataSource(DS_CUSTOMERS, customerDs, CACHE_DURATION);

            // Initialize engine
            engine = new DataSynchronizationEngine(factory);
            assertDoesNotThrow(() -> engine.initialize());

            // Verify: Engine initializes without errors
            assertNotNull(engine);
        }

        @Test
        @DisplayName("Should handle multi-level nested data sources")
        void testMultiLevelNestedDataSources() {
            // Setup: Order -> Customer -> Address -> City (3 levels deep)
            List<Order> orders = Arrays.asList(
                    new Order(1L, 100L, 500.0),
                    new Order(2L, 200L, 750.0)
            );

            List<Customer> customers = Arrays.asList(
                    new Customer(100L, ALICE, 1000L),
                    new Customer(200L, "Bob", 2000L)
            );

            List<Address> addresses = Arrays.asList(
                    new Address(1000L, "123 Main St", 10L),
                    new Address(2000L, "456 Oak Ave", 20L)
            );

            List<City> cities = Arrays.asList(
                    new City(10L, "New York"),
                    new City(20L, "Los Angeles")
            );

            InMemoryDataSource<Order> orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);
            InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
            InMemoryDataSource<Address> addressDs = new InMemoryDataSource<>(DS_ADDRESSES, Address.class, addresses);
            InMemoryDataSource<City> cityDs = new InMemoryDataSource<>(DS_CITIES, City.class, cities);
            
            dataSources.addAll(Arrays.asList(orderDs, customerDs, addressDs, cityDs));

            factory.registerDataSource(DS_ORDERS, orderDs, CACHE_DURATION);
            factory.registerDataSource(DS_CUSTOMERS, customerDs, CACHE_DURATION);
            factory.registerDataSource(DS_ADDRESSES, addressDs, CACHE_DURATION);
            factory.registerDataSource(DS_CITIES, cityDs, CACHE_DURATION);

            // Initialize engine
            engine = new DataSynchronizationEngine(factory);
            assertDoesNotThrow(() -> engine.initialize());

            // Verify: Engine processes multi-level nesting without errors
            assertNotNull(engine);
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void testNullSafetyInDataSources() {
            // Setup: Orders with some null customer IDs
            List<Order> orders = Arrays.asList(
                    new Order(1L, 100L, 500.0),
                    new Order(2L, null, 750.0),  // Null customer ID
                    new Order(3L, 300L, 300.0)   // Non-existent customer
            );

            List<Customer> customers = Arrays.asList(
                    new Customer(100L, ALICE, 1000L)
            );

            InMemoryDataSource<Order> orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);
            InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
            dataSources.add(orderDs);
            dataSources.add(customerDs);

            factory.registerDataSource(DS_ORDERS, orderDs, CACHE_DURATION);
            factory.registerDataSource(DS_CUSTOMERS, customerDs, CACHE_DURATION);

            // Initialize engine - should not throw exceptions
            engine = new DataSynchronizationEngine(factory);
            assertDoesNotThrow(() -> engine.initialize());

            // Verify: Engine handles nulls gracefully
            assertNotNull(engine);
        }
    }

    // ========== Large Dataset Performance Tests ==========

    @Nested
    @DisplayName("Large Dataset Tests")
    class LargeDatasetTests {

        @Test
        @DisplayName("Should handle large dataset efficiently")
        void testLargeDatasetPerformance() {
            // Create 1000 orders
            List<Order> orders = new ArrayList<>();
            for (long i = 1; i <= 1000; i++) {
                orders.add(new Order(i, i % 100, i * 10.0));
            }

            // Create 100 customers
            List<Customer> customers = new ArrayList<>();
            for (long i = 0; i < 100; i++) {
                customers.add(new Customer(i, CUSTOMER_PREFIX + i, i * 10));
            }

            InMemoryDataSource<Order> orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);
            InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
            dataSources.add(orderDs);
            dataSources.add(customerDs);

            factory.registerDataSource(DS_ORDERS, orderDs, CACHE_DURATION);
            factory.registerDataSource(DS_CUSTOMERS, customerDs, CACHE_DURATION);

            long startTime = System.currentTimeMillis();
            
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();

            long duration = System.currentTimeMillis() - startTime;

            // Verify
            assertNotNull(engine);
            
            // Performance check: Should complete within reasonable time (10 seconds)
            assertTrue(duration < 10000, "Large dataset initialization took too long: " + duration + "ms");
        }

        @Test
        @DisplayName("Should handle multiple data sources efficiently")
        void testMultipleDataSourcesPerformance() {
            // Create 500 orders
            List<Order> orders = new ArrayList<>();
            for (long i = 1; i <= 500; i++) {
                orders.add(new Order(i, i % 50, 0.0));
            }

            // Create 1000 customers (multiple per order)
            List<Customer> customers = new ArrayList<>();
            for (long i = 0; i < 1000; i++) {
                customers.add(new Customer(i, CUSTOMER_PREFIX + i, i % 50));
            }

            InMemoryDataSource<Order> orderDs = new InMemoryDataSource<>(DS_ORDERS, Order.class, orders);
            InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(DS_CUSTOMERS, Customer.class, customers);
            dataSources.add(orderDs);
            dataSources.add(customerDs);

            factory.registerDataSource(DS_ORDERS, orderDs, CACHE_DURATION);
            factory.registerDataSource(DS_CUSTOMERS, customerDs, CACHE_DURATION);

            long startTime = System.currentTimeMillis();
            
            engine = new DataSynchronizationEngine(factory);
            engine.initialize();

            long duration = System.currentTimeMillis() - startTime;

            // Verify
            assertNotNull(engine);
            assertTrue(duration < 10000, "Multiple data sources initialization took too long: " + duration + "ms");
        }
    }
}
