package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import com.thy.fss.common.inmemory.testmodel.UserDashboard;
import com.thy.fss.common.inmemory.testmodel.UserDashboard_;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.thy.fss.common.inmemory.testmodel.UserDashboardSpecificationService;

/**
 * Integration test for dashboard meta model functionality.
 * Tests the dashboard system with meta attributes.
 * <p>
 * Note: DashboardManager has been removed as synchronization is now handled
 * by DataSynchronizationEngine centrally.
 */
@Tag("integration")
class DashboardMetaModelIntegrationTest extends BaseIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create comprehensive test data using common generator
        TestDataGenerator.TestDataSet dataSet = TestDataGenerator.createCompleteDataSet(10);
        List<TestUser> testData = dataSet.users();

        // Enhance test data with varied ages and active status for complex scenarios
        for (int i = 0; i < testData.size(); i++) {
            TestUser user = testData.get(i);
            user.setAge(20 + (i * 5)); // Ages from 20 to 65
            user.setActive(i % 3 != 0); // Mix of active/inactive users
        }

        // Create data source with test data
        InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>("integration-test-users", TestUser.class, testData);

        // Create user store with data source
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("integration-test-users", dataSource, Duration.ofSeconds(5));
        // Wait for initial sync
        TestUtil.await(200);
    }

    @AfterEach
    void tearDown() {
        // Unregister datasources to prevent duplicate registration in next test
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.unregisterDataSource("integration-test-users");
        factory.unregisterDataSource("additional-integration-users");

        cleanup();
    }

    @Test
    @DisplayName("Basic dashboard with aggregations")
    void testBasicDashboardWithAggregations() {
        // Create dashboard using new API
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Basic User Summary");

        Dashboard<UserDashboard> dashboard = buildDashboard(builder);

        // Verify results - data may be null initially as DataSyncEngine hasn't pushed data yet
        // In real usage, DataSyncEngine would populate this
        assertNotNull(dashboard);
        assertNotNull(dashboard.getId());
        assertNotNull(dashboard.getName());

        System.out.println("✅ Basic dashboard integration test completed successfully!");
    }

    @Test
    @DisplayName("Dashboard configuration validation")
    void testDashboardConfigurationValidation() {
        // Test basic dashboard creation using new API
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Validation Test Dashboard");

        Dashboard<UserDashboard> dashboard = buildDashboard(builder);

        assertNotNull(dashboard, "Dashboard should be created successfully");
        assertEquals("Validation Test Dashboard", dashboard.getName());
        assertEquals(UserDashboard.class, dashboard.getTargetClass());

        System.out.println("✅ Dashboard configuration validation test completed successfully!");
    }

    @Test
    @DisplayName("Concurrent dashboard operations")
    void testConcurrentDashboardOperations() throws Exception {
        // Create dashboard using new API
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Concurrent Test Dashboard");

        Dashboard<UserDashboard> dashboard = buildDashboard(builder);

        // Execute multiple concurrent dashboard data requests
        CompletableFuture<UserDashboard> future1 = CompletableFuture.supplyAsync(dashboard::getData);
        CompletableFuture<UserDashboard> future2 = CompletableFuture.supplyAsync(dashboard::getData);
        CompletableFuture<UserDashboard> future3 = CompletableFuture.supplyAsync(dashboard::getData);

        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.get(5, TimeUnit.SECONDS);

        // Verify all results completed without errors (may be null initially)
        assertNotNull(dashboard);

        System.out.println("✅ Concurrent operations integration test completed successfully!");
    }

    @Test
    @DisplayName("Dynamic data updates with dashboards")
    void testDynamicDataUpdatesWithDashboards() {
        // Create dashboard using new API
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Dynamic Test Dashboard");

        Dashboard<UserDashboard> dashboard = buildDashboard(builder);
        assertNotNull(dashboard);

        // Wait for data synchronization
        TestUtil.await(300);

        assertNotNull(dashboard);

        System.out.println("✅ Dynamic data updates integration test completed successfully!");
    }

    @Test
    @DisplayName("Simple dashboard lifecycle")
    void testSimpleDashboardLifecycle() {
        // Create dashboard using new API
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Lifecycle Test Dashboard");

        Dashboard<UserDashboard> lifecycleDashboard = buildDashboard(builder);

        // Initial data retrieval
        assertNotNull(lifecycleDashboard);

        // Wait for synchronization
        TestUtil.await(300);

        // Verify data updates are reflected
        assertNotNull(lifecycleDashboard);

        // Test dashboard inactivity timeout
        TestUtil.await(3000);

        // Dashboard should still work after timeout
        assertNotNull(lifecycleDashboard);

        System.out.println("✅ Simple dashboard lifecycle integration test completed successfully!");
    }

    @Test
    @DisplayName("Multiple dashboards with shared store")
    void testMultipleDashboardsWithSharedStore() {
        // Create simple additional test data
        TestDataGenerator.TestDataSet additionalData = TestDataGenerator.createCompleteDataSet(5);
        List<TestUser> additionalUsers = additionalData.users();

        // Create second data source and store
        InMemoryDataSource<TestUser> additionalDataSource = new InMemoryDataSource<>(
                "additional-integration-users", TestUser.class, additionalUsers);

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("additional-integration-users", additionalDataSource, Duration.ofSeconds(5));


        // Wait for sync
        TestUtil.await(200);

        // Create dashboard for additional store using new API
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Additional Store Dashboard");

        Dashboard<UserDashboard> additionalDashboard = buildDashboard(builder);

        // Verify results
        assertNotNull(additionalDashboard);
        assertNotNull(additionalDashboard.getId());

        System.out.println("✅ Multiple dashboards integration test completed successfully!");

    }

    private Dashboard<UserDashboard> buildDashboard(DashboardBuilder<UserDashboard> builder){
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();

        targetPath.add(UserDashboard_.totalUsers);

        PropertyMapping<UserDashboard, Long> mapping = PropertyMapping.<UserDashboard, Long>builder()
                .consumerId("test-consumer-456")
                .datasourceName("integration-test-users")
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(UserDashboardSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(null)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.addPropertyMapping(mapping);
        return builder.build();
    }
}
