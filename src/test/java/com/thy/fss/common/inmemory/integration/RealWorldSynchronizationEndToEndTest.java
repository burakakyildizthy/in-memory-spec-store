package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.*;
import com.thy.fss.common.inmemory.testmodel.Order;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-world end-to-end synchronization test without any mocks.
 *
 * <p>
 * This test simulates a real production scenario where:</p>
 * <ul>
 * <li>Multiple data sources continuously update their data</li>
 * <li>Store and Dashboard run in parallel</li>
 * <li>Complex nested property mappings (3 levels deep)</li>
 * <li>Aggregations are computed in real-time</li>
 * <li>Test runs for 3 minutes continuously</li>
 * <li>1000+ rows of data with relationships</li>
 * <li>NO MOCKS - all real implementations</li>
 * </ul>
 *
 * <p>
 * Run this test separately using: ./gradlew test --tests
 * "*RealWorldSynchronizationEndToEndTest"</p>
 */
@Tag("integration")
@DisplayName("Real-World Synchronization End-to-End Test")
class RealWorldSynchronizationEndToEndTest {

    private static final Logger logger = LoggerFactory.getLogger(RealWorldSynchronizationEndToEndTest.class);

    private static final int TEST_DURATION_MINUTES = 3;
    private static final int INITIAL_CUSTOMER_COUNT = 1000;
    private static final int INITIAL_ORDER_COUNT = 5000;
    private static final int INITIAL_PRODUCT_COUNT = 500;
    private static final int UPDATE_INTERVAL_MS = 100;

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private LiveCustomerDataSource customerDataSource;
    private LiveOrderDataSource orderDataSource;
    private LiveProductDataSource productDataSource;
    private LiveOrderItemDataSource orderItemDataSource;

    private InMemoryDataStore<Customer> customerStore;
    private Dashboard<SalesDashboard> salesDashboard;

    private ExecutorService executorService;
    private AtomicBoolean testRunning;

    @BeforeEach
    void setUp() {
        logger.info("=".repeat(80));
        logger.info("Setting up Real-World Synchronization End-to-End Test");
        logger.info("=".repeat(80));

        factory = InMemorySpecStoreFactory.getInstance();
        executorService = Executors.newFixedThreadPool(10);
        testRunning = new AtomicBoolean(true);

        // Initialize live data sources
        customerDataSource = new LiveCustomerDataSource();
        orderDataSource = new LiveOrderDataSource();
        productDataSource = new LiveProductDataSource();
        orderItemDataSource = new LiveOrderItemDataSource();

        logger.info("Initialized {} customers", INITIAL_CUSTOMER_COUNT);
        logger.info("Initialized {} orders", INITIAL_ORDER_COUNT);
        logger.info("Initialized {} products", INITIAL_PRODUCT_COUNT);
    }

    @AfterEach
    void tearDown() {
        logger.info("=".repeat(80));
        logger.info("Tearing down test");
        logger.info("=".repeat(80));

        testRunning.set(false);

        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (engine != null && engine.isRunning()) {
            engine.close();
        }

        if (customerDataSource != null) {
            customerDataSource.close();
        }
        if (orderDataSource != null) {
            orderDataSource.close();
        }
        if (productDataSource != null) {
            productDataSource.close();
        }
        if (orderItemDataSource != null) {
            orderItemDataSource.close();
        }

        // Clear all registrations to prevent duplicate datasource errors
        factory.clearAll();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    @DisplayName("Should maintain data synchronization for 3 minutes with continuous updates")
    @Disabled("Disabled by default - enable to run the full 3-minute real-world synchronization test")
    void shouldMaintainDataSynchronizationWithContinuousUpdates() throws Exception {
        logger.info("Starting 3-minute real-world synchronization test");

        // Register data sources
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("products", productDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("orderItems", orderItemDataSource, Duration.ofSeconds(1));

        // Build Store with complex nested mappings
        customerStore = buildCustomerStore();

        // Build Dashboard with aggregations
        salesDashboard = buildSalesDashboard();

        // Create and initialize the synchronization engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial data synchronization
        waitForInitialDataLoad();

        // Start continuous data updates
        startContinuousDataUpdates();

        // Monitor and verify for 3 minutes
        monitorSynchronizationFor3Minutes();

        logger.info("Test completed successfully!");
    }

    private InMemoryDataStore<Customer> buildCustomerStore() {
        logger.info("Building Customer Store with nested property mappings...");

        InMemoryStoreBuilder<Customer> builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);
        builder.target(Customer_.totalOrders)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .count();
        builder.target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .sum(sfb -> sfb.field(Order_.totalAmount));
        builder.target(Customer_.averageOrderValue)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .avg(afb -> afb.field(Order_.totalAmount));
        builder.target(Customer_.orders)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .collection();
        InMemoryDataStore<Customer> store = builder.build();

        logger.info("Customer Store built successfully");
        return store;
    }

    private Dashboard<SalesDashboard> buildSalesDashboard() {
        logger.info("Building Sales Dashboard with aggregations...");

        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE)
                        .withName("Real-Time Sales Dashboard");

        builder.target(SalesDashboard_.totalCustomers)
                .from(CustomerSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .count();
        builder.target(SalesDashboard_.totalOrders)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Order_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .count();
        builder.target(SalesDashboard_.totalRevenue)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .sum(sfb -> sfb.field(Order_.totalAmount));
        builder.target(SalesDashboard_.averageOrderValue)
                .from(OrderSpecificationService.INSTANCE,
                        pkb -> pkb.field(Customer_.id),
                        fkb -> fkb.field(Order_.customerId)
                )
                .avg(afb -> afb.field(Order_.totalAmount));

        builder.target(SalesDashboard_.totalProducts)
                .from(ProductSpecificationService.INSTANCE,
                        pkb -> pkb.field(Product_.id),
                        fkb -> fkb.field(Order_.id)
                )
                .count();
        Dashboard<SalesDashboard> dashboard = builder.build();

        logger.info("Sales Dashboard built successfully");
        return dashboard;
    }

    private void waitForInitialDataLoad() {
        logger.info("Waiting for initial data synchronization...");

        int maxAttempts = 30;
        int attempt = 0;

        while (attempt < maxAttempts) {
            TestUtil.await(1000);
            attempt++;

            List<Customer> customers = customerStore.findAll();
            SalesDashboard dashboardData = salesDashboard.getData();

            if (!customers.isEmpty() && dashboardData.getTotalCustomers() != null && dashboardData.getTotalCustomers() > 0) {
                // Also verify that aggregations are computed
                long customersWithOrders = customers.stream()
                        .filter(c -> c.getOrders() != null && !c.getOrders().isEmpty())
                        .count();
                
                long customersWithTotalSpent = customers.stream()
                        .filter(c -> c.getTotalSpent() != null && c.getTotalSpent() > 0)
                        .count();

                if (customersWithOrders > 0 && customersWithTotalSpent > 0) {
                    logger.info("Initial data loaded successfully after {} seconds", attempt);
                    logger.info("  Store has {} customers", customers.size());
                    logger.info("  Dashboard shows {} customers", dashboardData.getTotalCustomers());
                    logger.info("  Customers with orders: {}", customersWithOrders);
                    logger.info("  Customers with totalSpent: {}", customersWithTotalSpent);
                    return;
                }
            }

            logger.info("Waiting for data... attempt {}/{}", attempt, maxAttempts);
        }

        logger.warn("Initial data load timeout after {} seconds", maxAttempts);
    }

    private void startContinuousDataUpdates() {
        logger.info("Starting continuous data updates...");


        // Update customers
        executorService.submit(() -> {


                    while (testRunning.get()) {
                        customerDataSource.updateRandomCustomers(10);
                        sleep(UPDATE_INTERVAL_MS);
                    }
                }
        );

        // Add new orders
        executorService.submit(() -> {
            while (testRunning.get()) {
                orderDataSource.addNewOrders(5);
                sleep(UPDATE_INTERVAL_MS);
            }
        });

        // Update order statuses
        executorService.submit(() -> {
            while (testRunning.get()) {
                orderDataSource.updateRandomOrderStatuses(10);
                sleep(UPDATE_INTERVAL_MS);
            }
        });

        // Update products
        executorService.submit(() -> {
            while (testRunning.get()) {
                productDataSource.updateRandomProducts(5);
                sleep(UPDATE_INTERVAL_MS);
            }
        });

        logger.info("Continuous data updates started");
    }

    private void monitorSynchronizationFor3Minutes() throws Exception {
        logger.info("Monitoring synchronization for {} minutes...", TEST_DURATION_MINUTES);

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(TEST_DURATION_MINUTES);

        AtomicInteger checkCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        while (LocalDateTime.now().isBefore(endTime)) {
            checkCount.incrementAndGet();

            try {
                // Verify Store synchronization
                verifyStoreSync();

                // Verify Dashboard synchronization
                verifyDashboardSync();

                successCount.incrementAndGet();

                if (checkCount.get() % 10 == 0) {
                    logProgress(startTime, checkCount.get(), successCount.get());
                }

            } catch (Exception e) {
                logger.error("Synchronization check failed at iteration {}: {}",
                        checkCount.get(), e.getMessage(), e);
                throw e;
            }

            TestUtil.await(1000); // Check every second
        }

        logger.info("=".repeat(80));
        logger.info("Monitoring completed!");
        logger.info("Total checks: {}", checkCount.get());
        logger.info("Successful checks: {}", successCount.get());
        logger.info("Success rate: {}%", (successCount.get() * 100.0 / checkCount.get()));
        logger.info("=".repeat(80));

        assertThat(successCount.get()).isEqualTo(checkCount.get());
    }

    private void verifyStoreSync() {
        List<Customer> customers = customerStore.findAll();

        assertThat(customers)
                .isNotNull()
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(INITIAL_CUSTOMER_COUNT);

        // Verify nested mappings are populated - retry if needed
        long customersWithOrders = 0;
        long customersWithTotalSpent = 0;
        
        for (int retry = 0; retry < 3; retry++) {
            customersWithOrders = customers.stream()
                    .filter(c -> c.getOrders() != null && !c.getOrders().isEmpty())
                    .count();

            customersWithTotalSpent = customers.stream()
                    .filter(c -> c.getTotalSpent() != null && c.getTotalSpent() > 0)
                    .count();
            
            if (customersWithOrders > 0 && customersWithTotalSpent > 0) {
                break;
            }
            
            // Wait a bit and retry
            if (retry < 2) {
                sleep(500);
                customers = customerStore.findAll();
            }
        }

        assertThat(customersWithOrders).isGreaterThan(0);
        assertThat(customersWithTotalSpent).isGreaterThan(0);
    }

    private void verifyDashboardSync() {
        SalesDashboard data = salesDashboard.getData();

        assertThat(data).isNotNull();
        assertThat(data.getTotalCustomers()).isGreaterThanOrEqualTo(INITIAL_CUSTOMER_COUNT);
        assertThat(data.getTotalOrders()).isGreaterThanOrEqualTo(INITIAL_ORDER_COUNT);
        assertThat(data.getTotalRevenue()).isGreaterThan(0.0);
        assertThat(data.getAverageOrderValue()).isGreaterThan(0.0);
        assertThat(data.getTotalProducts()).isGreaterThanOrEqualTo(INITIAL_PRODUCT_COUNT);
    }

    private void logProgress(LocalDateTime startTime, int checkCount, int successCount) {
        long elapsedSeconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        long remainingSeconds = (TEST_DURATION_MINUTES * 60) - elapsedSeconds;

        logger.info("-".repeat(80));
        logger.info("Progress Update:");
        logger.info("  Elapsed: {}s / {}s", elapsedSeconds, TEST_DURATION_MINUTES * 60);
        logger.info("  Remaining: {}s", remainingSeconds);
        logger.info("  Checks: {} (Success: {})", checkCount, successCount);
        logger.info("  Customers: {}", customerDataSource.getCount());
        logger.info("  Orders: {}", orderDataSource.getCount());
        logger.info("  Products: {}", productDataSource.getCount());
        logger.info("-".repeat(80));
    }

    private void sleep(long millis) {
        TestUtil.await(millis);
    }

    // ==================== Live Data Source Implementations ====================

    /**
     * Live Customer DataSource that continuously updates data
     */
    private class LiveCustomerDataSource implements DataSource<Customer> {
        private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);
        private final Random random = new Random();

        public LiveCustomerDataSource() {
            // Initialize with 1000 customers
            for (int i = 0; i < INITIAL_CUSTOMER_COUNT; i++) {
                Customer customer = createRandomCustomer();
                customers.put(customer.getIdentity(), customer);
            }
        }

        @Override
        public String getName() {
            return "customers";
        }

        @Override
        public Class<Customer> getEntityType() {
            return Customer.class;
        }


        @Override
        public CompletableFuture<List<Customer>> fetchAll() {
            return CompletableFuture.completedFuture(new ArrayList<>(customers.values()));
        }

        @Override
        public CompletableFuture<List<Customer>> fetchAllById(Collection<Object> ids) {
            List<Customer> result = ids.stream()
                    .map(id -> customers.get(((Number) id).longValue()))
                    .filter(Objects::nonNull)
                    .toList();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public void close() {
            customers.clear();
        }

        @Override
        public Optional<DataSource<Customer>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<Customer> fallbackDataSource) {
            // Not used in this test
        }

        public void updateRandomCustomers(int count) {
            List<Customer> customerList = new ArrayList<>(customers.values());
            for (int i = 0; i < count && !customerList.isEmpty(); i++) {
                Customer customer = customerList.get(random.nextInt(customerList.size()));
                customer.setEmail("updated_" + System.currentTimeMillis() + "@example.com");
                customer.setLastUpdated(LocalDateTime.now());
            }
        }

        public int getCount() {
            return customers.size();
        }

        private Customer createRandomCustomer() {
            Customer customer = new Customer();
            long id = idGenerator.getAndIncrement();
            customer.setId(id);
            customer.setName("Customer " + id);
            customer.setEmail("customer" + id + "@example.com");
            customer.setRegistrationDate(LocalDateTime.now().minusDays(random.nextInt(365)));
            customer.setActive(random.nextBoolean());
            customer.setLastUpdated(LocalDateTime.now());
            return customer;
        }
    }

    /**
     * Live Order DataSource that continuously adds and updates orders
     */
    private class LiveOrderDataSource implements DataSource<Order> {
        private final Map<Long, Order> orders = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);
        private final Random random = new Random();

        public LiveOrderDataSource() {
            // Initialize with 5000 orders
            for (int i = 0; i < INITIAL_ORDER_COUNT; i++) {
                Order order = createRandomOrder();
                orders.put(order.getIdentity(), order);
            }
        }

        @Override
        public String getName() {
            return "orders";
        }

        @Override
        public Class<Order> getEntityType() {
            return Order.class;
        }

        @Override

        public CompletableFuture<List<Order>> fetchAll() {
            return CompletableFuture.completedFuture(new ArrayList<>(orders.values()));
        }

        @Override
        public CompletableFuture<List<Order>> fetchAllById(Collection<Object> ids) {
            List<Order> result = ids.stream()
                    .map(id -> orders.get(((Number) id).longValue()))
                    .filter(Objects::nonNull)
                    .toList();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public void close() {
            orders.clear();
        }

        @Override
        public Optional<DataSource<Order>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<Order> fallbackDataSource) {
            // Not used in this test
        }

        public void addNewOrders(int count) {
            for (int i = 0; i < count; i++) {
                Order order = createRandomOrder();
                orders.put(order.getIdentity(), order);
            }
        }

        public void updateRandomOrderStatuses(int count) {
            List<Order> orderList = new ArrayList<>(orders.values());
            String[] statuses = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"};

            for (int i = 0; i < count && !orderList.isEmpty(); i++) {
                Order order = orderList.get(random.nextInt(orderList.size()));
                order.setStatus(statuses[random.nextInt(statuses.length)]);
                order.setLastUpdated(LocalDateTime.now());
            }
        }

        public int getCount() {
            return orders.size();
        }

        private Order createRandomOrder() {
            Order order = new Order();
            long id = idGenerator.getAndIncrement();
            order.setId(id);
            order.setCustomerId((long) (random.nextInt(INITIAL_CUSTOMER_COUNT) + 1));
            order.setOrderDate(LocalDateTime.now().minusDays(random.nextInt(90)));
            order.setTotalAmount(random.nextDouble() * 1000 + 10);
            order.setStatus("PENDING");
            order.setLastUpdated(LocalDateTime.now());
            return order;
        }
    }

    /**
     * Live Product DataSource
     */
    private class LiveProductDataSource implements DataSource<Product> {
        private final Map<Long, Product> products = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);
        private final Random random = new Random();

        public LiveProductDataSource() {
            // Initialize with 500 products
            for (int i = 0; i < INITIAL_PRODUCT_COUNT; i++) {
                Product product = createRandomProduct();
                products.put(product.getIdentity(), product);
            }
        }

        @Override
        public String getName() {
            return "products";
        }

        @Override
        public Class<Product> getEntityType() {
            return Product.class;
        }

        @Override

        public CompletableFuture<List<Product>> fetchAll() {
            return CompletableFuture.completedFuture(new ArrayList<>(products.values()));
        }

        @Override
        public CompletableFuture<List<Product>> fetchAllById(Collection<Object> ids) {
            List<Product> result = ids.stream()
                    .map(id -> products.get(((Number) id).longValue()))
                    .filter(Objects::nonNull)
                    .toList();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public void close() {
            products.clear();
        }

        @Override
        public Optional<DataSource<Product>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<Product> fallbackDataSource) {
            // Not used in this test
        }

        public void updateRandomProducts(int count) {
            List<Product> productList = new ArrayList<>(products.values());
            for (int i = 0; i < count && !productList.isEmpty(); i++) {
                Product product = productList.get(random.nextInt(productList.size()));
                product.setPrice(random.nextDouble() * 500 + 10);
                product.setStock(random.nextInt(1000));
            }
        }

        public int getCount() {
            return products.size();
        }

        private Product createRandomProduct() {
            Product product = new Product();
            long id = idGenerator.getAndIncrement();
            product.setId(id);
            product.setName("Product " + id);
            product.setPrice(random.nextDouble() * 500 + 10);
            product.setStock(random.nextInt(1000));
            product.setCategory("Category " + (random.nextInt(10) + 1));
            return product;
        }
    }

    /**
     * Live OrderItem DataSource
     */
    private class LiveOrderItemDataSource implements DataSource<OrderItem> {
        private final Map<Long, OrderItem> orderItems = new ConcurrentHashMap<>();
        @Override
        public String getName() {
            return "orderItems";
        }

        @Override
        public Class<OrderItem> getEntityType() {
            return OrderItem.class;
        }

        @Override
        public CompletableFuture<List<OrderItem>> fetchAll() {
            return CompletableFuture.completedFuture(new ArrayList<>(orderItems.values()));
        }

        @Override
        public CompletableFuture<List<OrderItem>> fetchAllById(Collection<Object> ids) {
            List<OrderItem> result = ids.stream()
                    .map(id -> orderItems.get(((Number) id).longValue()))
                    .filter(Objects::nonNull)

                    .toList();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public void close() {
            orderItems.clear();
        }

        @Override
        public Optional<DataSource<OrderItem>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<OrderItem> fallbackDataSource) {
            // Not used in this test
        }
    }
}
