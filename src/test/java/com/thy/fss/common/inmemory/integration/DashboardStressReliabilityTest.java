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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import com.thy.fss.common.inmemory.testmodel.UserDashboardSpecificationService;

/**
 * Stress and reliability integration tests for dashboard components.
 * <p>
 * Note: Tests have been simplified as Dashboard synchronization is now handled
 * by DataSynchronizationEngine centrally. DashboardStoreSyncHook and DashboardManager
 * are no longer used.
 */
@DisplayName("Dashboard Stress and Reliability Integration Tests")
class DashboardStressReliabilityTest extends BaseIntegrationTest {

    private static final String TEST_USERS = "test-users";
    private static final String MEMORY_TEST_PREFIX = "memory-test-";
    
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

        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    @DisplayName("Stress test - High volume dashboard creation")
    @Timeout(30)
    void testHighVolumeDashboardCreation() throws Exception {
        // Given - prepare for high volume operations
        int dashboardCount = 50;
        List<Dashboard<UserDashboard>> dashboards = new ArrayList<>();

        // When - create many dashboards concurrently
        List<Future<Dashboard<UserDashboard>>> futures = new ArrayList<>();

        for (int i = 0; i < dashboardCount; i++) {
            final int index = i;
            Future<Dashboard<UserDashboard>> future = executorService.submit(() ->
                    createStressDashboard("StressDashboard" + index)
            );
            futures.add(future);
        }

        // Collect results
        for (Future<Dashboard<UserDashboard>> future : futures) {
            Dashboard<UserDashboard> dashboard = future.get(5, TimeUnit.SECONDS);
            dashboards.add(dashboard);
        }

        // Then - all dashboards should be created successfully
        assertThat(dashboards).hasSize(dashboardCount);

        // Verify all dashboards are functional
        for (Dashboard<UserDashboard> dashboard : dashboards) {
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getId()).isNotNull();
        }
    }

    @Test
    @DisplayName("Stress test - Concurrent dashboard data access")
    @Timeout(30)
    void testConcurrentDashboardDataAccess() throws Exception {
        // Given - dashboard with store
        Dashboard<UserDashboard> dashboard = createStressDashboard("ConcurrentTestDashboard");

        // When - trigger many concurrent data access operations
        int accessCount = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(accessCount);

        for (int i = 0; i < accessCount; i++) {
            executorService.submit(() -> {
                try {
                    UserDashboard data = dashboard.getData();
                    if (data != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Then - most accesses should succeed
        assertThat(successCount.get()).isGreaterThan((int) (accessCount * 0.5)); // At least 50% success
    }

    @Test
    @DisplayName("Reliability test - Dashboard under memory pressure")
    @Timeout(30)
    void testDashboardUnderMemoryPressure() throws Exception {
        // Given - create memory pressure with large datasets
        List<Dashboard<UserDashboard>> dashboards = new ArrayList<>();

        try {
            // Create dashboards with large datasets
            for (int i = 0; i < 10; i++) {
                List<TestUser> largeData = TestDataGenerator.createUserList(1000 + i * 100);
                InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>(MEMORY_TEST_PREFIX + i, TestUser.class, largeData);

                InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
                factory.registerDataSource(MEMORY_TEST_PREFIX + i, dataSource, Duration.ofSeconds(5));

                Dashboard<UserDashboard> dashboard = createStressDashboardForDataSource("MemoryDashboard" + i, MEMORY_TEST_PREFIX + i);
                dashboards.add(dashboard);
            }

            // When - perform operations under memory pressure
            int operationCount = 100;
            CountDownLatch memoryLatch = new CountDownLatch(operationCount);
            AtomicInteger successfulOperations = new AtomicInteger(0);

            for (int i = 0; i < operationCount; i++) {
                final int operationIndex = i;

                executorService.submit(() -> {
                    try {
                        // Perform memory-intensive operations
                        Dashboard<UserDashboard> dashboard = dashboards.get(operationIndex % dashboards.size());
                        UserDashboard data = dashboard.getData();

                        if (data != null) {
                            successfulOperations.incrementAndGet();
                        }

                    } catch (OutOfMemoryError | Exception e) {
                        // Expected under memory pressure
                    } finally {
                        memoryLatch.countDown();
                    }
                });
            }

            // Wait for completion
            boolean completed = memoryLatch.await(20, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Then - system should handle memory pressure gracefully
            // At least some operations should succeed
            assertThat(successfulOperations.get()).isGreaterThan(0);

        } finally {
            // Force garbage collection
            System.gc();
        }
    }

    @Test
    @DisplayName("Reliability test - Long-running stability")
    @Timeout(60)
    void testLongRunningStability() throws Exception {
        // Given - dashboard for long-running test
        Dashboard<UserDashboard> dashboard = createStressDashboard("LongRunningDashboard");

        // When - run operations for extended period
        long testDurationMs = 10000; // 10 seconds
        long startTime = System.currentTimeMillis();
        AtomicInteger operationCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Submit continuous operations
        Future<?> operationFuture = executorService.submit(() -> {
            while (System.currentTimeMillis() - startTime < testDurationMs) {
                try {
                    // Continuous operations
                    UserDashboard data = dashboard.getData();
                    if (data != null) {
                        operationCount.incrementAndGet();
                    }

                    // Small delay to prevent overwhelming
                    TestUtil.await(10);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }
        });

        // Wait for completion
        operationFuture.get(testDurationMs + 5000, TimeUnit.MILLISECONDS);

        // Then - system should remain stable
        assertThat(operationCount.get()).isGreaterThan(10); // Should have performed some operations
        assertThat(errorCount.get()).isLessThan((int) (operationCount.get() * 0.2)); // Less than 20% errors

        assertThat(dashboard).isNotNull();
    }

    private Dashboard<UserDashboard> createStressDashboard(String name) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName(name);

        Dashboard<UserDashboard> dashboard = buildDashboard(builder, TEST_USERS);
        // Initialize dashboard with test data for stress testing
        // Since these are stress tests and not testing synchronization,
        // we manually populate the dashboard with sample data
        UserDashboard sampleData = new UserDashboard();
        sampleData.setTotalUsers(1000L);
        dashboard.updateData(sampleData);

        return dashboard;
    }

    private Dashboard<UserDashboard> createStressDashboardForDataSource(String name, String dataSourceName) {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName(name);
        Dashboard<UserDashboard> dashboard = buildDashboard(builder, dataSourceName);

        // Initialize dashboard with test data for stress testing
        UserDashboard sampleData = new UserDashboard();
        sampleData.setTotalUsers(1000L);
        dashboard.updateData(sampleData);

        return dashboard;
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }

        // Clear all datasources to prevent duplicate registration in next test
        InMemorySpecStoreFactory.getInstance().clearAll();

        cleanup();
    }


    private Dashboard<UserDashboard> buildDashboard(DashboardBuilder<UserDashboard> builder, String dataSourceName) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();

        targetPath.add((UserDashboard_.totalUsers));

        PropertyMapping<UserDashboard, Long> mapping = PropertyMapping.<UserDashboard, Long>builder()
                .consumerId("test-consumer-456")
                .datasourceName(dataSourceName)
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
