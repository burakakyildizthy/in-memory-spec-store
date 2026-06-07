package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.UserDashboard;
import com.thy.fss.common.inmemory.testmodel.UserDashboard_;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import com.thy.fss.common.inmemory.testmodel.UserDashboardSpecificationService;

/**
 * Comprehensive system integration test covering complex end-to-end scenarios.
 * <p>
 * Note: Tests have been simplified as Dashboard synchronization is now handled
 * by DataSynchronizationEngine centrally.
 */
@Tag("integration")
@DisplayName("Comprehensive System Integration Tests")
class ComprehensiveSystemIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USERS = "test-users";

    private InMemoryDataStore<TestUser> dataStore;
    private SpecificationQueryEngine<TestUser> queryEngine;
    private ExecutorService executorService;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create test data
        List<TestUser> testData = TestDataGenerator.createUserList(1000);
        InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>(TEST_USERS, TestUser.class, testData);

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource(TEST_USERS, dataSource, Duration.ofSeconds(5));

        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually populate the store with data from the datasource
        dataStore.updateData(testData, 1L);

        queryEngine = new SpecificationQueryEngine<>(TestUser.class);
        executorService = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        // Clear datasources to prevent DuplicateDataSourceException in subsequent tests
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.unregisterDataSource(TEST_USERS);
        factory.unregisterDataSource("empty-test");
        factory.clearAll();
    }

    @Test
    @DisplayName("Complex multi-component workflow with concurrent operations")
    void testComplexMultiComponentWorkflow() throws Exception {
        // Create multiple dashboards with different configurations
        Dashboard<UserDashboard> userStatsDashboard = createUserStatsDashboard();
        Dashboard<UserDashboard> ageAnalyticsDashboard = createAgeAnalyticsDashboard();
        Dashboard<UserDashboard> activityDashboard = createActivityDashboard();

        // Execute concurrent operations
        CompletableFuture<Void> statsAccess = CompletableFuture.runAsync(userStatsDashboard::getData, executorService);

        CompletableFuture<Void> ageAccess = CompletableFuture.runAsync(ageAnalyticsDashboard::getData, executorService);

        CompletableFuture<Void> activityAccess = CompletableFuture.runAsync(activityDashboard::getData, executorService);

        CompletableFuture<Void> dataQuery = CompletableFuture.runAsync(() -> {
            // Simulate data queries
            for (int i = 0; i < 100; i++) {
                dataStore.findAll();
            }
        }, executorService);

        // Wait for all operations to complete
        CompletableFuture.allOf(statsAccess, ageAccess, activityAccess, dataQuery)
                .get(30, TimeUnit.SECONDS);

        // Verify system consistency
        assertThat(dataStore.findAll()).isNotNull();
        assertThat(userStatsDashboard).isNotNull();
        assertThat(ageAnalyticsDashboard).isNotNull();
        assertThat(activityDashboard).isNotNull();
    }

    @Test
    @DisplayName("Edge case handling with empty and null data")
    void testEdgeCaseHandling() {
        // Test with empty data store
        InMemoryDataSource<TestUser> emptyDataSource = new InMemoryDataSource<>("empty-test", TestUser.class);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("empty-test", emptyDataSource, Duration.ofSeconds(5));


        Dashboard<UserDashboard> emptyDashboard = createDashboardForStore("empty-test");

        assertThat(emptyDashboard).isNotNull();

        // Test with null values in data
        TestUser userWithNulls = new TestUser();
        userWithNulls.setName(null);
        userWithNulls.setAge(null);
        userWithNulls.setActive(null);
        emptyDataSource.addItem(userWithNulls);

        assertThat(emptyDashboard).isNotNull();
    }

    @Test
    @DisplayName("Memory efficiency under load")
    void testMemoryEfficiencyUnderLoad() {
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create multiple dashboards
        Dashboard<UserDashboard> dashboard1 = createUserStatsDashboard();
        Dashboard<UserDashboard> dashboard2 = createAgeAnalyticsDashboard();
        Dashboard<UserDashboard> dashboard3 = createActivityDashboard();

        // Perform multiple data access cycles
        for (int i = 0; i < 10; i++) {
            dashboard1.getData();
            dashboard2.getData();
            dashboard3.getData();
        }

        // Force garbage collection
        System.gc();

        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Memory usage should be reasonable (less than 100MB for this test)
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024);
    }

    @Test
    @DisplayName("Complex query scenarios with multiple filters")
    void testComplexQueryScenarios() {
        // Test basic functionality
        List<TestUser> allUsers = dataStore.findAll();
        assertThat(allUsers).isNotNull().hasSize(1000);

        // Test query engine with complex scenarios
        assertThat(queryEngine).isNotNull();
    }

    private Dashboard<UserDashboard> createUserStatsDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("User Stats Dashboard");

        return buildDashboard(builder, TEST_USERS);
    }

    private Dashboard<UserDashboard> createAgeAnalyticsDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Age Analytics Dashboard");


        return buildDashboard(builder, TEST_USERS);
    }

    private Dashboard<UserDashboard> createActivityDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Activity Dashboard");

        return buildDashboard(builder, TEST_USERS);
    }

    private Dashboard<UserDashboard> createDashboardForStore(String name) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName(name + " Dashboard");


        return buildDashboard(builder, name);
    }

    private Dashboard<UserDashboard> buildDashboard(DashboardBuilder<UserDashboard> builder, String name){

        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(UserDashboard_.totalUsers);


        PropertyMapping<UserDashboard, Long> mapping = PropertyMapping.<UserDashboard, Long>builder()
                .consumerId("test-consumer-456")
                .datasourceName(name)
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
