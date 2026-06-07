package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.TestSynchronizationHelper;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.sync.DataVersion;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.integration.testentities.*;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestOrderSpecificationService;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestUserSummarySpecificationService;

/**
 * End-to-end integration test for the complete synchronization cycle.
 * <p>
 * Tests:
 * - Multiple datasources
 * - Multiple stores and dashboards
 * - Full sync cycle (Analysis, Collection, Production phases)
 * - DataVersion swap
 * - Push to consumers
 */
class EndToEndSynchronizationIntegrationTest {

    private static final String USERS = "users";
    private static final String ORDERS = "orders";
    private static final String ACTIVE = "active";
    private static final String COMPLETED = "completed";
    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;

    private TestableInMemoryDataSource<CrossTestUser> userDataSource;
    private TestableInMemoryDataSource<CrossTestOrder> orderDataSource;

    private InMemoryDataStore<CrossTestUser> userStore;
    private Dashboard<CrossTestUserSummary> userSummaryDashboard;

    @BeforeEach
    void setUp() {
        // Get factory instance
        factory = InMemorySpecStoreFactory.getInstance();

        // Create test data
        List<CrossTestUser> users = Arrays.asList(
                createUser(ALICE, "alice@test.com", 25, ACTIVE),
                createUser(BOB, "bob@test.com", 30, ACTIVE),
                createUser( "Charlie", "charlie@test.com", 35, "inactive")
        );

        List<CrossTestOrder> orders = Arrays.asList(
                createOrder("order1", ALICE, 100.00, COMPLETED),
                createOrder("order2", ALICE, 200.00, COMPLETED),
                createOrder("order3", BOB, 150.00, COMPLETED),
                createOrder("order4", BOB, 250.00, "pending"),
                createOrder("order5", "Charlie", 300.00, COMPLETED)
        );

        // Create datasources
        userDataSource = new TestableInMemoryDataSource<>(USERS, CrossTestUser.class, users);
        orderDataSource = new TestableInMemoryDataSource<>(ORDERS, CrossTestOrder.class, orders);

        // Register datasources
        factory.registerDataSource(USERS, userDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofMinutes(5));
    }

    @AfterEach
    void tearDown() {
        if (engine != null && engine.isRunning()) {
            engine.close();
        }
        // Clear all registrations to prevent duplicate datasource errors between tests
        factory.clearAll();
    }

    @Test
    @Disabled("Test disabled: Tries to access dataByDataSource which is now cleared after synchronization " +
              "to prevent memory leaks. This is the intended behavior from the memory leak fixes. " +
              "The test needs to be rewritten to verify functionality through stores/dashboards instead of " +
              "accessing intermediate data directly.")
    void testFullSynchronizationCycle() {
        // Create specification for active users
        Specification<CrossTestUser> activeSpec = SpecificationBuilder.forService(CrossTestUserSpecificationService.INSTANCE)
                .where(CrossTestUser_.status).equalTo(ACTIVE);

        // Build store with property mappings
        userStore = factory.buildInMemoryStore(CrossTestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestUser.class)
                .withSpecification(activeSpec)
                .build();

        // Build dashboard with aggregations
        DashboardBuilder<CrossTestUserSummary> builder = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        // Add totalOrders aggregation (COUNT)
        LinkedList<MetaAttribute<?, ?>> totalOrdersPath = new LinkedList<>();
        totalOrdersPath.add(CrossTestUserSummary_.totalOrders);

        LinkedList<MetaAttribute<?, ?>> orderIdSourcePath = new LinkedList<>();
        orderIdSourcePath.add(CrossTestOrder_.orderId);

        PropertyMapping<CrossTestUserSummary, Integer> totalOrdersMapping = PropertyMapping.<CrossTestUserSummary, Integer>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS)
                .isForDashboard(true)
                .targetPath(totalOrdersPath)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(orderIdSourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();
        builder.addPropertyMapping(totalOrdersMapping);

        // Add totalAmount aggregation (SUM)
        LinkedList<MetaAttribute<?, ?>> totalAmountPath = new LinkedList<>();
        totalAmountPath.add(CrossTestUserSummary_.totalAmount);

        LinkedList<MetaAttribute<?, ?>> amountSourcePath = new LinkedList<>();
        amountSourcePath.add(CrossTestOrder_.amount);

        PropertyMapping<CrossTestUserSummary, Double> totalAmountMapping = PropertyMapping.<CrossTestUserSummary, Double>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS)
                .isForDashboard(true)
                .targetPath(totalAmountPath)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(amountSourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();
        builder.addPropertyMapping(totalAmountMapping);

        userSummaryDashboard = builder.build();

        // Create engine and initialize
        engine = new DataSynchronizationEngine(factory);
        assertNotNull(engine, "Engine should be created");

        engine.initialize();

        // Wait for initial synchronization
        TestSynchronizationHelper.waitForCondition(
                () -> !userStore.findAll().isEmpty(),
                Duration.ofSeconds(5),
                "Store should have data after initialization"
        );

        // VERIFY PHASE 1: Analysis Phase
        // The engine should have completed analysis (internal state, not directly testable)

        // VERIFY PHASE 2: Collection Phase (MAP)
        // Check that stores received root data
        List<CrossTestUser> storeUsers = userStore.findAll();
        assertEquals(2, storeUsers.size(), "Store should have 2 active users");
        assertTrue(storeUsers.stream().allMatch(u -> ACTIVE.equals(u.getStatus())),
                "All users in store should be active");

        // VERIFY PHASE 3: Production Phase (REDUCE)
        // Check dashboard aggregations
        CrossTestUserSummary summary = userSummaryDashboard.getData();
        assertNotNull(summary, "Dashboard should have data");
        assertEquals(5, summary.getTotalOrders(), "Dashboard should show 5 total orders");
        assertEquals(1000.00, summary.getTotalAmount(),
                "Dashboard should show correct total amount");

        // VERIFY: DataVersion swap
        DataVersion currentVersion = engine.getCurrentDataVersion();
        assertNotNull(currentVersion, "Current data version should exist");
        assertTrue(currentVersion.getVersion() > 0, "Version number should be positive");

        // VERIFY: Data in DataVersion (check datasources are loaded)
        assertNotNull(currentVersion.getDataByDataSource(USERS),
                "DataVersion should contain users datasource");
        assertNotNull(currentVersion.getDataByDataSource(ORDERS),
                "DataVersion should contain orders datasource");

        // VERIFY: Populated entities exist
        assertFalse(currentVersion.getAllPopulatedEntities().isEmpty(),
                "DataVersion should contain populated entities for consumers");
    }

    @Test
    void testMultipleStoresAndDashboards() {
        // Create specifications
        Specification<CrossTestUser> activeSpec = SpecificationBuilder.forService(CrossTestUserSpecificationService.INSTANCE)
                .where(CrossTestUser_.status).equalTo(ACTIVE);
        Specification<CrossTestOrder> completedSpec = SpecificationBuilder.forService(CrossTestOrderSpecificationService.INSTANCE)
                .where(CrossTestOrder_.status).equalTo(COMPLETED);

        // Build multiple stores
        InMemoryDataStore<CrossTestUser> activeUserStore = factory.buildInMemoryStore(CrossTestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestUser.class)
                .withSpecification(activeSpec)
                .build();

        InMemoryDataStore<CrossTestUser> allUserStore = factory.buildInMemoryStore(CrossTestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestUser.class)
                .build();

        InMemoryDataStore<CrossTestOrder> completedOrderStore = factory.buildInMemoryStore(CrossTestOrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestOrder.class)
                .withSpecification(completedSpec)
                .build();

        // Build multiple dashboards
        DashboardBuilder<CrossTestUserSummary> builder1 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        // Dashboard 1: totalOrders (COUNT)
        LinkedList<MetaAttribute<?, ?>> totalOrdersPath1 = new LinkedList<>();
        totalOrdersPath1.add(CrossTestUserSummary_.totalOrders);

        LinkedList<MetaAttribute<?, ?>> orderIdSourcePath1 = new LinkedList<>();
        orderIdSourcePath1.add(CrossTestOrder_.orderId);

        PropertyMapping<CrossTestUserSummary, Integer> totalOrdersMapping1 = PropertyMapping.<CrossTestUserSummary, Integer>builder()
                .consumerId(builder1.getConsumerId())
                .datasourceName(ORDERS)
                .isForDashboard(true)
                .targetPath(totalOrdersPath1)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(orderIdSourcePath1)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();
        builder1.addPropertyMapping(totalOrdersMapping1);

        Dashboard<CrossTestUserSummary> dashboard1 = builder1.build();

        DashboardBuilder<CrossTestUserSummary> builder2 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        // Dashboard 2: totalAmount (SUM)
        LinkedList<MetaAttribute<?, ?>> totalAmountPath2 = new LinkedList<>();
        totalAmountPath2.add(CrossTestUserSummary_.totalAmount);

        LinkedList<MetaAttribute<?, ?>> amountSourcePath2 = new LinkedList<>();
        amountSourcePath2.add(CrossTestOrder_.amount);

        PropertyMapping<CrossTestUserSummary, Double> totalAmountMapping2 = PropertyMapping.<CrossTestUserSummary, Double>builder()
                .consumerId(builder2.getConsumerId())
                .datasourceName(ORDERS)
                .isForDashboard(true)
                .targetPath(totalAmountPath2)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(amountSourcePath2)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();
        builder2.addPropertyMapping(totalAmountMapping2);

        Dashboard<CrossTestUserSummary> dashboard2 = builder2.build();

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for synchronization
        TestSynchronizationHelper.waitForCondition(
                () -> !activeUserStore.findAll().isEmpty() && !allUserStore.findAll().isEmpty(),
                Duration.ofSeconds(5)
        );

        // Verify all stores received data
        assertEquals(2, activeUserStore.findAll().size(), "Active user store should have 2 users");
        assertEquals(3, allUserStore.findAll().size(), "All user store should have 3 users");
        assertEquals(4, completedOrderStore.findAll().size(), "Completed order store should have 4 orders");

        // Verify all dashboards received data
        assertNotNull(dashboard1.getData(), "Dashboard 1 should have data");
        assertNotNull(dashboard2.getData(), "Dashboard 2 should have data");
        assertEquals(5, dashboard1.getData().getTotalOrders(), "Dashboard 1 should show 5 orders");
        assertEquals(1000.00, dashboard2.getData().getTotalAmount(),
                "Dashboard 2 should show all orders total");
    }


    @Test
    void testDataVersionIncrement() {
        // Build simple store
        userStore = factory.buildInMemoryStore(CrossTestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestUser.class)
                .build();

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial sync
        TestSynchronizationHelper.waitForCondition(
                () -> !userStore.findAll().isEmpty(),
                Duration.ofSeconds(5)
        );

        // Get initial version
        DataVersion version1 = engine.getCurrentDataVersion();
        long versionNumber1 = version1.getVersion();

        // Trigger another sync
        engine.synchronizeDataSource(USERS);

        // Wait a bit for sync to complete
        TestSynchronizationHelper.waitForCondition(
                () -> engine.getCurrentDataVersion().getVersion() > versionNumber1,
                Duration.ofSeconds(5),
                "Version number should increment"
        );

        // Verify version incremented
        DataVersion version2 = engine.getCurrentDataVersion();
        assertTrue(version2.getVersion() > versionNumber1,
                "Version number should have incremented");
    }

    @Test
    void testPushToConsumers() {
        // Build store and dashboard
        userStore = factory.buildInMemoryStore(CrossTestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(CrossTestUser.class)
                .build();

        DashboardBuilder<CrossTestUserSummary> builder1 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        // Add totalOrders aggregation (COUNT)
        LinkedList<MetaAttribute<?, ?>> totalOrdersPath = new LinkedList<>();
        totalOrdersPath.add(CrossTestUserSummary_.totalOrders);

        LinkedList<MetaAttribute<?, ?>> orderIdSourcePath = new LinkedList<>();
        orderIdSourcePath.add(CrossTestOrder_.orderId);

        PropertyMapping<CrossTestUserSummary, Integer> totalOrdersMapping = PropertyMapping.<CrossTestUserSummary, Integer>builder()
                .consumerId(builder1.getConsumerId())
                .datasourceName(ORDERS)
                .isForDashboard(true)
                .targetPath(totalOrdersPath)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(orderIdSourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();
        builder1.addPropertyMapping(totalOrdersMapping);

        userSummaryDashboard = builder1.build();

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for data
        TestSynchronizationHelper.waitForCondition(
                () -> !userStore.findAll().isEmpty() && userSummaryDashboard.getData() != null,
                Duration.ofSeconds(5)
        );

        // Verify store received data via updateData()
        assertFalse(userStore.findAll().isEmpty(), "Store should have received data");

        // Verify dashboard received data via updateData()
        assertNotNull(userSummaryDashboard.getData(), "Dashboard should have received data");

        // Modify datasource
        List<CrossTestUser> newUsers = List.of(
                createUser( "David", "david@test.com", 40, ACTIVE)
        );
        userDataSource.clearData();
        userDataSource.addItems(newUsers);

        // Trigger sync
        engine.synchronizeDataSource(USERS);

        // Wait for update
        TestSynchronizationHelper.waitForCondition(
                () -> userStore.findAll().size() == 1,
                Duration.ofSeconds(5),
                "Store should be updated with new data"
        );

        // Verify consumers received updated data
        assertEquals(1, userStore.findAll().size(), "Store should have new data");
        assertEquals("David", userStore.findAll().get(0).getName(), "Store should have correct user");
    }

    // Helper methods
    private CrossTestUser createUser(String name, String email, Integer age, String status) {
        // Note: CrossTestUser doesn't have an id field, using name as identifier
        return new CrossTestUser(name, email, age, status);
    }

    private CrossTestOrder createOrder(String orderId, String userId, Double amount, String status) {
        return new CrossTestOrder(orderId, userId, amount, status);
    }
}
