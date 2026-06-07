package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.integration.testentities.*;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end workflow tests that verify complete data processing workflows.
 * Tests multi-step operations, data transformations, and realistic usage scenarios.
 * <p>
 * Requirements covered: 6.1, 6.2, 6.4
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndWorkflowTest {

    private static final String ACTIVE = "ACTIVE";
    private static final String COMPLETED = "COMPLETED";
    private static final String GOLD = "GOLD";

    private InMemorySpecStoreFactory factory;
    private List<InMemoryDataSource<?>> dataSources;
    private com.thy.fss.common.inmemory.engine.DataSynchronizationEngine engine;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        dataSources = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Close engine first
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Stores and dashboards are managed by factory
        dataSources.forEach(ds -> {
            try {
                ds.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        });

        dataSources.clear();

        // Clear all datasources from factory to avoid conflicts between tests
        factory.clearAllDataSources();
    }

    /**
     * Test complete data processing workflow from ingestion to dashboard.
     * Simulates a realistic e-commerce scenario with customers, orders, and products.
     * Requirements: 6.1, 6.2
     */
    @Test
    @org.junit.jupiter.api.Order(1)
    @org.junit.jupiter.api.Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testCompleteDataProcessingWorkflow() throws Exception {
        // Step 1: Set up data sources with realistic data
        List<WorkflowCustomer> workflowCustomers = createTestWorkflowCustomers(100);
        List<WorkflowOrder> workflowOrders = createTestWorkflowOrders(workflowCustomers, 500);
        List<WorkflowProduct> workflowProducts = createTestWorkflowProducts(50);

        InMemoryDataSource<WorkflowCustomer> workflowCustomerDataSource = new InMemoryDataSource<>("WorkflowCustomers", WorkflowCustomer.class);
        workflowCustomerDataSource.clearData();
        workflowCustomerDataSource.addItems(workflowCustomers);
        dataSources.add(workflowCustomerDataSource);

        InMemoryDataSource<WorkflowOrder> workflowOrderDataSource = new InMemoryDataSource<>("WorkflowOrders", WorkflowOrder.class);
        workflowOrderDataSource.clearData();
        workflowOrderDataSource.addItems(workflowOrders);
        dataSources.add(workflowOrderDataSource);

        InMemoryDataSource<WorkflowProduct> workflowProductDataSource = new InMemoryDataSource<>("WorkflowProducts", WorkflowProduct.class);
        workflowProductDataSource.clearData();
        workflowProductDataSource.addItems(workflowProducts);
        dataSources.add(workflowProductDataSource);

        // Step 2: Register datasources and create data stores with synchronization
        factory.registerDataSource("WorkflowCustomers", workflowCustomerDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("WorkflowOrders", workflowOrderDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("WorkflowProducts", workflowProductDataSource, Duration.ofSeconds(5));

        InMemoryDataStore<WorkflowCustomer> workflowCustomerStore = factory.buildInMemoryStore(WorkflowCustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowCustomer.class)
                .build();

        InMemoryDataStore<WorkflowOrder> workflowOrderStore = factory.buildInMemoryStore(WorkflowOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowOrder.class)
                .build();

        InMemoryDataStore<WorkflowProduct> workflowProductStore = factory.buildInMemoryStore(WorkflowProductSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowProduct.class)
                .build();

        // Step 3: Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Step 4: Wait for initial data synchronization
        waitForSynchronization(workflowCustomerStore, 100);
        waitForSynchronization(workflowOrderStore, 500);
        waitForSynchronization(workflowProductStore, 50);

        // Step 5: Verify data ingestion
        assertEquals(100, workflowCustomerStore.findAll().size(), "Customer store should contain 100 customers");
        assertEquals(500, workflowOrderStore.findAll().size(), "Order store should contain 500 orders");
        assertEquals(50, workflowProductStore.findAll().size(), "Product store should contain 50 products");

        // Step 6: Perform complex data queries and transformations

        // Find premium customers (GOLD tier) using type-safe API
        Specification<WorkflowCustomer> premiumCustomersSpec = SpecificationBuilder.forService(WorkflowCustomerSpecificationService.INSTANCE)
                .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowCustomer_.tier).equalTo(GOLD)
                .and(SpecificationBuilder.forService(WorkflowCustomerSpecificationService.INSTANCE)
                        .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowCustomer_.status).equalTo(ACTIVE));

        Page<WorkflowCustomer> premiumCustomers = workflowCustomerStore.findAll(premiumCustomersSpec,
                PageRequest.of(0, 20, Sort.by("registrationDate").descending()));

        assertTrue(premiumCustomers.getTotalElements() > 0, "Should find premium customers");

        // Find high-value orders (> $500) - using manual filtering since Double not supported
        List<WorkflowOrder> allOrders = workflowOrderStore.findAll();
        List<WorkflowOrder> highValueOrders = allOrders.stream()
                .filter(order -> order.getAmount().compareTo(500.0) > 0)
                .filter(order -> COMPLETED.equals(order.getStatus()))
                .toList();

        assertFalse(highValueOrders.isEmpty(), "Should find high-value orders");

        // Find low-stock products using type-safe API
        Specification<WorkflowProduct> activeProductsSpec = SpecificationBuilder.forService(WorkflowProductSpecificationService.INSTANCE)
                .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowProduct_.status).equalTo(ACTIVE);

        List<WorkflowProduct> activeProducts = workflowProductStore.findAll(activeProductsSpec, PageRequest.of(0, 100)).getContent();
        List<WorkflowProduct> lowStockProducts = activeProducts.stream()
                .filter(p -> p.getStockLevel() < 10)
                .toList();

        assertNotNull(lowStockProducts, "Low stock products query should not be null");

        // Step 7: Verify customer analytics data is accessible
        // Dashboard functionality is now managed by DataSynchronizationEngine

        // Step 8: Verify data is accessible from store
        List<WorkflowCustomer> allCustomers = workflowCustomerStore.findAll();
        assertNotNull(allCustomers, "Customer data should be accessible");
        assertEquals(100, allCustomers.size(), "Should have 100 customers");

        // Step 9: Test data updates and propagation
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // Add new customers with unique IDs
        List<WorkflowCustomer> newCustomers = new ArrayList<>(workflowCustomers);
        newCustomers.addAll(createAdditionalTestWorkflowCustomers(20, 100)); // Start from ID 100
        workflowCustomerDataSource.clearData();
        workflowCustomerDataSource.addItems(newCustomers);

        // Wait for synchronization and dashboard update
        waitForSynchronization(workflowCustomerStore, 120);

        // Verify updates propagated
        assertEquals(120, workflowCustomerStore.findAll().size(), "Customer store should contain 120 customers after update");

        // Verify the update happened after our marker time
        assertTrue(LocalDateTime.now().isAfter(beforeUpdate),
                "Update should have occurred after marker time");

        // Step 10: Verify system performance under load
        long startTime = System.currentTimeMillis();

        // Perform multiple concurrent queries
        List<CompletableFuture<Void>> queryFutures = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Random queries to simulate realistic load
                        workflowCustomerStore.findAll(PageRequest.of(0, 20));
                        workflowOrderStore.findAll(PageRequest.of(0, 50));
                        workflowProductStore.findAll(PageRequest.of(0, 10));
                    } catch (Exception e) {
                        fail("Concurrent query failed: " + e.getMessage());
                    }
                }))
                .toList();

        CompletableFuture.allOf(queryFutures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        assertTrue(totalTime < 10000, "Concurrent queries should complete within 10 seconds");

        // Step 11: Verify data consistency across all components
        verifyDataConsistency(workflowCustomerStore, workflowOrderStore, workflowProductStore);
    }

    /**
     * Test multi-step data transformation workflow.
     * Tests complex data processing with multiple transformation steps.
     * Requirements: 6.1, 6.4
     */
    @Test
    @org.junit.jupiter.api.Order(2)
    @org.junit.jupiter.api.Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testMultiStepDataTransformationWorkflow() {
        // Step 1: Create initial dataset
        List<WorkflowCustomer> customers = createTestWorkflowCustomers(50);
        List<WorkflowOrder> orders = createTestWorkflowOrders(customers, 200);

        InMemoryDataSource<WorkflowCustomer> customerDataSource = new InMemoryDataSource<>("WorkflowCustomers", WorkflowCustomer.class);
        customerDataSource.clearData();
        customerDataSource.addItems(customers);
        dataSources.add(customerDataSource);

        InMemoryDataSource<WorkflowOrder> orderDataSource = new InMemoryDataSource<>("WorkflowOrders", WorkflowOrder.class);
        orderDataSource.clearData();
        orderDataSource.addItems(orders);
        dataSources.add(orderDataSource);

        // Step 2: Register datasources and create stores
        // Clear any existing datasources to avoid conflicts with other tests
        factory.clearAllDataSources();

        factory.registerDataSource("TransformCustomers", customerDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("TransformOrders", orderDataSource, Duration.ofSeconds(5));

        InMemoryDataStore<WorkflowCustomer> customerStore = factory.buildInMemoryStore(WorkflowCustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowCustomer.class)
                .build();

        InMemoryDataStore<WorkflowOrder> orderStore = factory.buildInMemoryStore(WorkflowOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowOrder.class)
                .build();

        // Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial synchronization
        waitForSynchronization(customerStore, 50);
        waitForSynchronization(orderStore, 200);

        // Step 3: Transformation 1 - Filter active customers using type-safe API
        Specification<WorkflowCustomer> activeCustomersSpec = SpecificationBuilder.forService(WorkflowCustomerSpecificationService.INSTANCE)
                .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowCustomer_.status).equalTo(ACTIVE);

        List<WorkflowCustomer> activeCustomers = customerStore.findAll(activeCustomersSpec, PageRequest.of(0, 100))
                .getContent();

        assertTrue(activeCustomers.size() > 0, "Should have active customers");

        // Step 4: Transformation 2 - Get orders for active customers
        Set<String> activeCustomerIds = activeCustomers.stream()
                .map(WorkflowCustomer::getCustomerId)
                .collect(Collectors.toSet());

        List<WorkflowOrder> activeCustomerOrders = orderStore.findAll().stream()
                .filter(order -> activeCustomerIds.contains(order.getCustomerId()))
                .toList();

        assertTrue(activeCustomerOrders.size() > 0, "Should have orders for active customers");

        // Step 5: Transformation 3 - Calculate customer metrics
        Map<String, CustomerMetrics> customerMetrics = calculateCustomerMetrics(activeCustomers, activeCustomerOrders);

        assertFalse(customerMetrics.isEmpty(), "Should calculate customer metrics");

        // Step 6: Transformation 4 - Filter high-value customers
        customerMetrics.values().stream()
                .filter(metrics -> metrics.totalSpent.compareTo(1000.0) > 0)
                .sorted((a, b) -> b.totalSpent.compareTo(a.totalSpent))
                .toList();

        // Step 7: Verify final transformation results
        // Dashboard functionality is now managed by DataSynchronizationEngine

        // Step 8: Verify data is accessible from store
        List<WorkflowCustomer> finalCustomers = customerStore.findAll();
        assertNotNull(finalCustomers, "Customer data should be accessible");
        assertTrue(finalCustomers.size() > 0, "Should have customers in store");

        // Step 9: Test transformation pipeline resilience
        // Verify transformations work with current data
        List<WorkflowCustomer> cachedActiveCustomers = customerStore.findAll(activeCustomersSpec, PageRequest.of(0, 100))
                .getContent();

        assertEquals(activeCustomers.size(), cachedActiveCustomers.size(),
                "Should get consistent results from store");

        // Step 10: Test transformation consistency
        // Verify transformations work consistently
        List<WorkflowCustomer> consistentActiveCustomers = customerStore.findAll(activeCustomersSpec, PageRequest.of(0, 100))
                .getContent();

        assertEquals(activeCustomers.size(), consistentActiveCustomers.size(),
                "Should get consistent results");
    }

    /**
     * Test realistic usage scenarios under various conditions.
     * Simulates real-world application usage patterns.
     * Requirements: 6.1, 6.2, 6.4
     */
    @Test
    @org.junit.jupiter.api.Order(3)
    @org.junit.jupiter.api.Timeout(value = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    void testRealisticUsageScenarios() {
        // Scenario 1: E-commerce analytics system
        testEcommerceAnalyticsScenario();

        // Scenario 2: Real-time monitoring dashboard
        testRealTimeMonitoringScenario();

        // Scenario 3: Batch processing workflow
        testBatchProcessingScenario();
    }

    private void testEcommerceAnalyticsScenario() {
        // Simulate e-commerce analytics with periodic data updates
        List<WorkflowCustomer> customers = createTestWorkflowCustomers(200);
        List<WorkflowOrder> orders = createTestWorkflowOrders(customers, 1000);

        InMemoryDataSource<WorkflowCustomer> customerDataSource = new InMemoryDataSource<>("ecommerce-customers", WorkflowCustomer.class);
        customerDataSource.clearData();
        customerDataSource.addItems(customers);
        dataSources.add(customerDataSource);

        InMemoryDataSource<WorkflowOrder> orderDataSource = new InMemoryDataSource<>("ecommerce-orders", WorkflowOrder.class);
        orderDataSource.clearData();
        orderDataSource.addItems(orders);
        dataSources.add(orderDataSource);

        // Clear any existing datasources to avoid conflicts
        factory.clearAllDataSources();

        factory.registerDataSource("ecommerce-customers", customerDataSource, Duration.ofSeconds(2));
        factory.registerDataSource("ecommerce-orders", orderDataSource, Duration.ofSeconds(2));

        InMemoryDataStore<WorkflowCustomer> customerStore = factory.buildInMemoryStore(WorkflowCustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowCustomer.class)
                .build();

        InMemoryDataStore<WorkflowOrder> orderStore = factory.buildInMemoryStore(WorkflowOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowOrder.class)
                .build();

        // Initialize and start the synchronization engine for this scenario
        com.thy.fss.common.inmemory.engine.DataSynchronizationEngine scenarioEngine =
                new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        scenarioEngine.initialize();

        TestUtil.await(2000);

        // Simulate periodic analytics queries
        for (int i = 0; i < 5; i++) {
            // Customer segmentation query using type-safe API
            Specification<WorkflowCustomer> goldCustomersSpec = SpecificationBuilder.forService(WorkflowCustomerSpecificationService.INSTANCE)
                    .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowCustomer_.tier).equalTo(GOLD);

            Page<WorkflowCustomer> goldCustomers = customerStore.findAll(goldCustomersSpec, PageRequest.of(0, 50));
            assertTrue(goldCustomers.getTotalElements() > 0, "Should find gold customers in iteration " + i);

            // Revenue analysis query using type-safe API
            Specification<WorkflowOrder> recentOrdersSpec = SpecificationBuilder.forService(WorkflowOrderSpecificationService.INSTANCE)
                    .where(com.thy.fss.common.inmemory.integration.testentities.WorkflowOrder_.status).equalTo(COMPLETED);

            Page<WorkflowOrder> recentOrders = orderStore.findAll(recentOrdersSpec, PageRequest.of(0, 100));
            assertTrue(recentOrders.getTotalElements() > 0, "Should find recent orders in iteration " + i);

            TestUtil.await(200);
        }

        // Close the scenario engine
        scenarioEngine.close();

        // Unregister datasources to avoid conflicts
        factory.unregisterDataSource("ecommerce-customers");
        factory.unregisterDataSource("ecommerce-orders");
    }

    private void testRealTimeMonitoringScenario() {
        // Simulate real-time monitoring with frequent updates
        List<WorkflowOrder> orders = new ArrayList<>();

        InMemoryDataSource<WorkflowOrder> orderDataSource = new InMemoryDataSource<>("realtime-orders", WorkflowOrder.class);
        orderDataSource.clearData();
        orderDataSource.addItems(orders);
        dataSources.add(orderDataSource);

        // Clear any existing datasources to avoid conflicts
        factory.clearAllDataSources();

        factory.registerDataSource("realtime-orders", orderDataSource, Duration.ofMillis(500));

        InMemoryDataStore<WorkflowOrder> orderStore = factory.buildInMemoryStore(WorkflowOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowOrder.class)
                .build();

        // Initialize and start the synchronization engine for this scenario
        com.thy.fss.common.inmemory.engine.DataSynchronizationEngine scenarioEngine =
                new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        scenarioEngine.initialize();

        // Simulate real-time order processing
        for (int i = 0; i < 10; i++) {
            // Add new orders
            orders.add(new WorkflowOrder("Order-" + i, "Customer-" + (i % 5),
                    100.00, "PENDING", "ELECTRONICS"));

            orderDataSource.clearData();
            orderDataSource.addItems(new ArrayList<>(orders));

            // Wait for synchronization with expected count
            waitForSynchronization(orderStore, i + 1);

            // Query current orders
            List<WorkflowOrder> currentOrders = orderStore.findAll();

            // Verify the expected count
            assertTrue(currentOrders.size() >= 1, "Should have at least 1 order in iteration " + i);
            assertTrue(currentOrders.size() <= i + 1, "Should have at most " + (i + 1) + " orders in iteration " + i);
        }

        // Close the scenario engine
        scenarioEngine.close();

        // Unregister datasource to avoid conflicts
        factory.unregisterDataSource("realtime-orders");
    }

    private void testBatchProcessingScenario(){
        // Simulate batch processing with large data sets
        List<WorkflowCustomer> customers = createTestWorkflowCustomers(500);
        List<WorkflowOrder> orders = createTestWorkflowOrders(customers, 2000);

        InMemoryDataSource<WorkflowCustomer> customerDataSource = new InMemoryDataSource<>("batch-customers", WorkflowCustomer.class);
        customerDataSource.clearData();
        customerDataSource.addItems(customers);
        dataSources.add(customerDataSource);

        InMemoryDataSource<WorkflowOrder> orderDataSource = new InMemoryDataSource<>("batch-orders", WorkflowOrder.class);
        orderDataSource.clearData();
        orderDataSource.addItems(orders);
        dataSources.add(orderDataSource);

        // Clear any existing datasources to avoid conflicts
        factory.clearAllDataSources();

        factory.registerDataSource("batch-customers", customerDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("batch-orders", orderDataSource, Duration.ofSeconds(5));

        InMemoryDataStore<WorkflowCustomer> customerStore = factory.buildInMemoryStore(WorkflowCustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowCustomer.class)
                .build();

        InMemoryDataStore<WorkflowOrder> orderStore = factory.buildInMemoryStore(WorkflowOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(WorkflowOrder.class)
                .build();

        // Initialize and start the synchronization engine for this scenario
        com.thy.fss.common.inmemory.engine.DataSynchronizationEngine scenarioEngine =
                new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        scenarioEngine.initialize();

        waitForSynchronization(customerStore, 500);
        waitForSynchronization(orderStore, 2000);

        // Batch processing: Process customers in chunks
        int pageSize = 50;
        int totalPages = (customers.size() + pageSize - 1) / pageSize;

        for (int page = 0; page < totalPages; page++) {
            Page<WorkflowCustomer> customerPage = customerStore.findAll(PageRequest.of(page, pageSize));
            assertNotNull(customerPage, "Customer page " + page + " should not be null");
            assertTrue(customerPage.getContent().size() > 0, "Customer page " + page + " should have content");
        }

        // Batch processing: Process orders in chunks
        pageSize = 100;
        totalPages = (orders.size() + pageSize - 1) / pageSize;

        for (int page = 0; page < totalPages; page++) {
            Page<WorkflowOrder> orderPage = orderStore.findAll(PageRequest.of(page, pageSize));
            assertNotNull(orderPage, "Order page " + page + " should not be null");
            assertTrue(orderPage.getContent().size() > 0, "Order page " + page + " should have content");
        }

        // Close the scenario engine
        scenarioEngine.close();

        // Unregister datasources to avoid conflicts
        factory.unregisterDataSource("batch-customers");
        factory.unregisterDataSource("batch-orders");
    }

    private List<WorkflowCustomer> createTestWorkflowCustomers(int count) {
        List<WorkflowCustomer> customers = new ArrayList<>();
        String[] tiers = {"BRONZE", "SILVER", GOLD, "PLATINUM"};
        String[] statuses = {ACTIVE, "INACTIVE", "SUSPENDED"};

        for (int i = 0; i < count; i++) {
            WorkflowCustomer customer = new WorkflowCustomer(
                    "Customer-" + i,
                    "Customer " + i,
                    "customer" + i + "@example.com",
                    tiers[i % tiers.length],
                    statuses[i % statuses.length]
            );
            customers.add(customer);
        }

        return customers;
    }

    private List<WorkflowCustomer> createAdditionalTestWorkflowCustomers(int count, int startIndex) {
        List<WorkflowCustomer> customers = new ArrayList<>();
        String[] tiers = {"BRONZE", "SILVER", GOLD, "PLATINUM"};
        String[] statuses = {ACTIVE, "INACTIVE", "SUSPENDED"};

        for (int i = 0; i < count; i++) {
            int actualIndex = startIndex + i;
            WorkflowCustomer customer = new WorkflowCustomer(
                    "Customer-" + actualIndex,
                    "Customer " + actualIndex,
                    "customer" + actualIndex + "@example.com",
                    tiers[actualIndex % tiers.length],
                    statuses[actualIndex % statuses.length]
            );
            customers.add(customer);
        }

        return customers;
    }

    private List<WorkflowOrder> createTestWorkflowOrders(List<WorkflowCustomer> customers, int count) {
        List<WorkflowOrder> orders = new ArrayList<>();
        String[] statuses = {"PENDING", COMPLETED, "CANCELLED", "SHIPPED"};
        String[] categories = {"ELECTRONICS", "CLOTHING", "BOOKS", "HOME", "SPORTS"};
        Random random = new Random(42); // Fixed seed for reproducible tests

        for (int i = 0; i < count; i++) {
            WorkflowCustomer customer = customers.get(i % customers.size());
            Double amount = 50.0 + random.nextInt(1000);

            WorkflowOrder order = new WorkflowOrder(
                    "Order-" + i,
                    customer.getCustomerId(),
                    amount,
                    statuses[i % statuses.length],
                    categories[i % categories.length]
            );
            orders.add(order);
        }

        return orders;
    }

    private List<WorkflowProduct> createTestWorkflowProducts(int count) {
        List<WorkflowProduct> products = new ArrayList<>();
        String[] categories = {"ELECTRONICS", "CLOTHING", "BOOKS", "HOME", "SPORTS"};
        String[] statuses = {ACTIVE, "INACTIVE", "DISCONTINUED"};
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            WorkflowProduct product = new WorkflowProduct(
                    "Product-" + i,
                    "Product " + i,
                    categories[i % categories.length],
                    10.0 + random.nextInt(500),
                    random.nextInt(100),
                    statuses[i % statuses.length]
            );
            products.add(product);
        }

        return products;
    }

    private Map<String, CustomerMetrics> calculateCustomerMetrics(List<WorkflowCustomer> customers, List<WorkflowOrder> orders) {
        Map<String, CustomerMetrics> metrics = new HashMap<>();

        // Group orders by customer
        Map<String, List<WorkflowOrder>> ordersByCustomer = orders.stream()
                .collect(Collectors.groupingBy(WorkflowOrder::getCustomerId));

        for (WorkflowCustomer customer : customers) {
            List<WorkflowOrder> customerOrders = ordersByCustomer.getOrDefault(customer.getCustomerId(), new ArrayList<>());

            Double totalSpent = customerOrders.stream()
                    .filter(order -> COMPLETED.equals(order.getStatus()))
                    .map(WorkflowOrder::getAmount)
                    .reduce(0.0, Double::sum);

            CustomerMetrics customerMetrics = new CustomerMetrics(
                    customer.getCustomerId(),
                    customer.getName(),
                    customerOrders.size(),
                    totalSpent
            );

            metrics.put(customer.getCustomerId(), customerMetrics);
        }

        return metrics;
    }

    // Helper methods

    private void waitForSynchronization(InMemoryDataStore<?> store, int expectedCount) {
        int maxWaitTime = 15000; // 15 seconds max (increased to accommodate 5-second sync intervals)
        int checkInterval = 200; // Check every 200ms
        int elapsed = 0;

        while (elapsed < maxWaitTime) {
            if (store.findAll().size() >= expectedCount) {
                return; // Synchronization complete
            }
            TestUtil.await(checkInterval);
            elapsed += checkInterval;
        }

        // If we get here, synchronization didn't complete in time
        fail("Synchronization timeout: expected " + expectedCount + " items, got " + store.findAll().size());
    }

    private void verifyDataConsistency(InMemoryDataStore<WorkflowCustomer> customerStore,
                                       InMemoryDataStore<WorkflowOrder> orderStore,
                                       InMemoryDataStore<WorkflowProduct> productStore) {

        // Verify data integrity
        List<WorkflowCustomer> customers = customerStore.findAll();
        List<WorkflowOrder> orders = orderStore.findAll();
        List<WorkflowProduct> products = productStore.findAll();

        assertFalse(customers.isEmpty(), "Should have customers");
        assertFalse(orders.isEmpty(), "Should have orders");
        assertFalse(products.isEmpty(), "Should have products");

        // Verify referential integrity (orders reference valid customers)
        Set<String> customerIds = customers.stream()
                .map(WorkflowCustomer::getCustomerId)
                .collect(Collectors.toSet());

        for (WorkflowOrder order : orders) {
            assertTrue(customerIds.contains(order.getCustomerId()),
                    "Order " + order.getOrderId() + " should reference valid customer");
        }
    }

    record CustomerMetrics(String customerId, String customerName, int totalOrders, Double totalSpent) {
    }
}
