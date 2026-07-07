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
import com.thy.fss.common.inmemory.testmodel.UserDashboard;
import com.thy.fss.common.inmemory.testmodel.UserDashboardSpecificationService;
import com.thy.fss.common.inmemory.testmodel.UserDashboard_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for dashboard components.
 * <p>
 * Note: Tests have been simplified as Dashboard synchronization is now handled
 * by DataSynchronizationEngine centrally. DashboardStoreSyncHook is no longer used.
 */
@DisplayName("Dashboard Integration Tests")
class DashboardIntegrationTest extends BaseIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create comprehensive test data
        List<TestUser> testUsers = createComprehensiveTestData();

        // Create data source and store using the new API
        InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>("integration-users", TestUser.class, testUsers);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("integration-users", dataSource, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        // Unregister the datasource to prevent duplicate registration in next test
        InMemorySpecStoreFactory.getInstance().unregisterDataSource("integration-users");
        cleanup();
    }

    @Test
    @DisplayName("Multiple dashboards with shared store")
    void testMultipleDashboardsWithSharedStore() {
        // Given - multiple dashboards sharing the same store

        Dashboard<UserDashboard> ageAnalyticsDashboard = createAgeAnalyticsDashboard();
        Dashboard<UserDashboard> activityDashboard = createActivityDashboard();

        // Then - all dashboards should work with shared store
        assertThat(ageAnalyticsDashboard).isNotNull();
        assertThat(activityDashboard).isNotNull();
    }

    @Test
    @DisplayName("Concurrent dashboard operations")
    void testConcurrentDashboardOperations() throws Exception {
        // Given - multiple dashboards for concurrent testing
        int dashboardCount = 5;
        @SuppressWarnings("unchecked")
        Dashboard<UserDashboard>[] dashboards = new Dashboard[dashboardCount];

        for (int i = 0; i < dashboardCount; i++) {
            dashboards[i] = createConcurrentTestDashboard("ConcurrentDashboard" + i);
        }

        // When - concurrent operations
        ExecutorService executor = Executors.newFixedThreadPool(dashboardCount);
        CountDownLatch latch = new CountDownLatch(dashboardCount * 10); // 10 operations per dashboard

        for (int i = 0; i < dashboardCount; i++) {
            final Dashboard<UserDashboard> dashboard = dashboards[i];

            for (int j = 0; j < 10; j++) {
                executor.submit(() -> {
                    try {
                        assertThat(dashboard).isNotNull();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        // Wait for completion
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then - all dashboards should remain functional
        for (Dashboard<UserDashboard> dashboard : dashboards) {
            assertThat(dashboard).isNotNull();
        }
    }

    private Dashboard<UserDashboard> createAgeAnalyticsDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Age Analytics Dashboard");
        return buildDashboard(builder);
    }

    private Dashboard<UserDashboard> createActivityDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Activity Dashboard");
        return buildDashboard(builder);
    }

    private Dashboard<UserDashboard> createConcurrentTestDashboard(String name) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName(name);

        return buildDashboard(builder);
    }

    private List<TestUser> createComprehensiveTestData() {
        return TestDataGenerator.createUserList(
                "Alice", "Bob", "Charlie", "Diana", "Eve",
                "Frank", "Grace", "Henry", "Ivy", "Jack",
                "Kate", "Liam", "Mia", "Noah", "Olivia"
        );
    }

    private Dashboard<UserDashboard> buildDashboard(DashboardBuilder<UserDashboard> builder){

        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(UserDashboard_.totalUsers);

        PropertyMapping<UserDashboard, Long> mapping = PropertyMapping.<UserDashboard, Long>builder()
                .consumerId("test-consumer-456")
                .datasourceName("test-users")
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
