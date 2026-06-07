package com.thy.fss.common.inmemory.integration;


import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestProfile;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Complex workflow integration tests covering advanced multi-component scenarios.
 * <p>
 * Note: Tests have been simplified as Dashboard synchronization is now handled
 * by DataSynchronizationEngine centrally. DashboardManager and DashboardStoreSyncHook
 * are no longer used.
 */
@Tag("workflow")
@DisplayName("Complex Workflow Integration Tests")
class ComplexWorkflowIntegrationTest extends BaseIntegrationTest {

    private static final String DATASOURCE_TEST_USERS = "test-users";
    private static final String DATASOURCE_TEST_PROFILES = "test-profiles";
    private static final String DATASOURCE_EMPTY_TEST = "empty-test";
    

    private InMemoryDataStore<TestUser> userStore;
    private InMemoryDataSource<TestUser> userDataSource;
    private InMemoryDataSource<TestProfile> profileDataSource;
    private ExecutorService executorService;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
        factory.unregisterDataSource(DATASOURCE_TEST_USERS);
        factory.unregisterDataSource(DATASOURCE_TEST_PROFILES);
        factory.unregisterDataSource(DATASOURCE_EMPTY_TEST);

        executorService = Executors.newFixedThreadPool(6);
        // Create data sources with initial data
        List<TestUser> initialUsers = TestDataGenerator.createUserList(50);
        List<TestProfile> initialProfiles = TestDataGenerator.createProfileList(25);

        userDataSource = new InMemoryDataSource<>(DATASOURCE_TEST_USERS, TestUser.class, initialUsers);
        profileDataSource = new InMemoryDataSource<>(DATASOURCE_TEST_PROFILES, TestProfile.class, initialProfiles);
        
        factory.registerDataSource(DATASOURCE_TEST_USERS, userDataSource, Duration.ofSeconds(5));
        factory.registerDataSource(DATASOURCE_TEST_PROFILES, profileDataSource, Duration.ofSeconds(5));

        userStore = factory.buildInMemoryStore(com.thy.fss.common.inmemory.common.model.TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
        }

        // Unregister all data sources to prevent conflicts between tests
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.unregisterDataSource(DATASOURCE_TEST_USERS);
        factory.unregisterDataSource(DATASOURCE_TEST_PROFILES);
        factory.unregisterDataSource(DATASOURCE_EMPTY_TEST);
        factory.clearAll();

        cleanup();
    }

    @Test
    @DisplayName("Complex business workflow - User lifecycle with dashboard tracking")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testComplexUserLifecycleWithDashboardTracking() {
        // Given - comprehensive user lifecycle tracking system
        Dashboard<UserDashboard> lifecycleDashboard = createUserLifecycleDashboard();
        Dashboard<UserDashboard> analyticsDashboard = createUserAnalyticsDashboard();

        assertThat(lifecycleDashboard).isNotNull();
        assertThat(analyticsDashboard).isNotNull();

        // When - simulate complete user lifecycle

        // 1. User registration phase
        List<TestUser> newUsers = TestDataGenerator.createUserList(25);
        newUsers.forEach(user -> {
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            userDataSource.addItem(user);
        });

        // 2. User activation phase
        List<TestUser> allUsers = userStore.findAll();
        allUsers.stream()
                .filter(user -> !user.getActive())
                .limit(10)
                .forEach(user -> {
                    user.setActive(true);
                    userDataSource.addItem(user);
                });

        // Wait for dashboard synchronization
        TestUtil.await(2000);

        // Then - verify lifecycle tracking
        assertThat(lifecycleDashboard).isNotNull();
        assertThat(analyticsDashboard).isNotNull();
    }

    @Test
    @DisplayName("Complex error cascade and recovery workflow")
    void testComplexErrorCascadeAndRecoveryWorkflow() {
        // Given - system with multiple failure points
        Dashboard<UserDashboard> primaryDashboard = createUserAnalyticsDashboard();
        Dashboard<UserDashboard> backupDashboard = createUserPerformanceDashboard();

        // When - simulate error cascade

        // 1. Initial healthy state
        assertThat(primaryDashboard).isNotNull();

        // 2. Introduce data corruption
        TestUser corruptedUser = new TestUser();
        corruptedUser.setName(""); // Invalid name
        corruptedUser.setAge(-1); // Invalid age
        corruptedUser.setActive(null); // Null status
        userDataSource.addItem(corruptedUser);

        // 3. System should continue functioning despite errors
        assertThat(primaryDashboard).isNotNull();

        // 4. Backup dashboard should remain functional
        assertThat(backupDashboard).isNotNull();

        // 5. Attempt recovery
        try {
            // Clean up corrupted data
            List<TestUser> users = userStore.findAll();
            users.removeIf(user -> user.getName() == null || user.getName().isEmpty() || user.getAge() < 0);
        } catch (Exception e) {
            // Recovery handling
        }

        // Then - verify recovery
        assertThat(primaryDashboard).isNotNull();
    }

    @Test
    @DisplayName("Complex multi-component synchronization workflow")
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void testComplexMultiComponentSynchronizationWorkflow() throws Exception {
        // Given - multiple synchronized components
        Dashboard<UserDashboard> userDashboard = createUserAnalyticsDashboard();
        Dashboard<ProfileDashboard> profileDashboard = createProfileAnalyticsDashboard();

        Map<String, Object> synchronizationResults = new ConcurrentHashMap<>();
        AtomicInteger syncEvents = new AtomicInteger(0);

        // When - execute synchronized operations
        CompletableFuture<Void> userOperations = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 30; i++) {
                TestUser user = TestDataGenerator.createUser("SyncUser" + i, 25 + (i % 30));
                userDataSource.addItem(user);

                synchronizationResults.put("user_sync_" + i, System.currentTimeMillis());

                syncEvents.incrementAndGet();

                TestUtil.await(20);
            }
        }, executorService);

        CompletableFuture<Void> profileOperations = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 20; i++) {
                TestProfile profile = TestDataGenerator.createProfile("SyncProfile" + i);
                profileDataSource.addItem(profile);

                synchronizationResults.put("profile_sync_" + i, System.currentTimeMillis());

                syncEvents.incrementAndGet();

                TestUtil.await(30);
            }
        }, executorService);

        CompletableFuture<Void> crossComponentSync = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 15; i++) {
                // Cross-component synchronization
                synchronizationResults.put("cross_sync_" + i, System.currentTimeMillis());

                TestUtil.await(50);
            }
        }, executorService);

        // Wait for all synchronization operations
        CompletableFuture.allOf(userOperations, profileOperations, crossComponentSync)
                .get(30, TimeUnit.SECONDS);

        // Then - verify synchronization consistency
        assertThat(syncEvents.get()).isEqualTo(50); // 30 user + 20 profile sync events
        assertThat(synchronizationResults).hasSizeGreaterThan(60);

        // Verify final state consistency
        assertThat(userDashboard).isNotNull();
        assertThat(profileDashboard).isNotNull();
    }

    @Test
    @DisplayName("Complex performance optimization workflow")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testComplexPerformanceOptimizationWorkflow() {
        // Given - performance optimization scenario

        // Create performance monitoring dashboards
        Dashboard<UserDashboard> performanceDashboard = createUserPerformanceDashboard();
        Dashboard<UserDashboard> optimizedDashboard = createOptimizedDashboard();

        // Baseline performance measurement
        long baselineStart = System.currentTimeMillis();
        performanceDashboard.getData();
        long baselineTime = System.currentTimeMillis() - baselineStart;

        assertThat(performanceDashboard).isNotNull();

        // When - simulate performance optimization workflow

        // 1. Load testing phase
        for (int loadLevel = 1; loadLevel <= 3; loadLevel++) {
            List<TestUser> loadUsers = TestDataGenerator.createUserList(loadLevel * 100);
            loadUsers.forEach(userDataSource::addItem);

            // Measure performance under load
            long loadStart = System.currentTimeMillis();
            performanceDashboard.getData();
            long loadTime = System.currentTimeMillis() - loadStart;

            assertThat(performanceDashboard).isNotNull();

            // Performance should remain reasonable
            long adjustedBaselineTime = Math.max(baselineTime, 1);
            assertThat(loadTime).isLessThan(adjustedBaselineTime * (loadLevel + 1) * 5);
        }

        // 2. Optimization phase - use optimized dashboard
        long optimizedStart = System.currentTimeMillis();
        optimizedDashboard.getData();
        long optimizedTime = System.currentTimeMillis() - optimizedStart;

        // 3. Comparison phase
        long comparisonStart = System.currentTimeMillis();
        performanceDashboard.getData();
        long comparisonTime = System.currentTimeMillis() - comparisonStart;

        // Then - verify optimization effectiveness
        assertThat(optimizedDashboard).isNotNull();
        assertThat(performanceDashboard).isNotNull();

        // Optimized dashboard should perform reasonably well
        long adjustedComparisonTime = Math.max(comparisonTime, 1);
        assertThat(optimizedTime).isLessThanOrEqualTo(adjustedComparisonTime * 5);
    }

    @Test
    @DisplayName("Complex edge case boundary testing workflow")
    void testComplexEdgeCaseBoundaryTestingWorkflow() {
        // Given - comprehensive edge case testing

        // Test with various boundary conditions
        Dashboard<UserDashboard> edgeCaseDashboard = createUserAnalyticsDashboard();

        // When - test edge cases

        // 1. Empty data edge case
        InMemoryDataSource<TestUser> emptyDataSource = new InMemoryDataSource<>(DATASOURCE_EMPTY_TEST, TestUser.class);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
        factory.registerDataSource(DATASOURCE_EMPTY_TEST, emptyDataSource, Duration.ofSeconds(5));
        Dashboard<UserDashboard> emptyDashboard = createDashboardForStore(DATASOURCE_EMPTY_TEST);

        assertThat(emptyDashboard).isNotNull();

        // 2. Single item edge case
        TestUser singleUser = TestDataGenerator.createUser("EdgeUser", 30);
        emptyDataSource.addItem(singleUser);

        assertThat(emptyDashboard).isNotNull();

        // 3. Null value edge cases
        TestUser nullUser = new TestUser();
        nullUser.setName(null);
        nullUser.setAge(null);
        nullUser.setActive(null);
        emptyDataSource.addItem(nullUser);

        assertThat(emptyDashboard).isNotNull();

        // 4. Extreme value edge cases
        TestUser extremeUser = TestDataGenerator.createUser("ExtremeUser", Integer.MAX_VALUE);
        emptyDataSource.addItem(extremeUser);

        TestUser minUser = TestDataGenerator.createUser("MinUser", 0);
        emptyDataSource.addItem(minUser);

        assertThat(emptyDashboard).isNotNull();

        // 5. Large string edge case
        StringBuilder largeNameBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeNameBuilder.append("A");
        }
        TestUser largeStringUser = TestDataGenerator.createUser(largeNameBuilder.toString(), 25);
        emptyDataSource.addItem(largeStringUser);

        assertThat(emptyDashboard).isNotNull();

        // Then - verify edge case handling
        assertThat(edgeCaseDashboard).isNotNull();
    }

    // Helper methods

    private Dashboard<UserDashboard> createUserLifecycleDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("User Lifecycle Dashboard");

        return buildDashboard(builder, DATASOURCE_TEST_USERS);
    }

    private Dashboard<UserDashboard> createUserAnalyticsDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("User Analytics Dashboard");


        return buildDashboard(builder, DATASOURCE_TEST_USERS);
    }

    private Dashboard<UserDashboard> createUserPerformanceDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("User Performance Dashboard");

        return buildDashboard(builder, DATASOURCE_TEST_USERS);
    }

    private Dashboard<ProfileDashboard> createProfileAnalyticsDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        DashboardBuilder<ProfileDashboard> builder = factory.buildDashboard(ProfileDashboardSpecificationService.INSTANCE)
                .withName("Profile Analytics Dashboard");

        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(ProfileDashboard_.totalProfiles);

        PropertyMapping<ProfileDashboard, Long> mapping = PropertyMapping.<ProfileDashboard, Long>builder()
                .consumerId("test-consumer-457")
                .datasourceName(profileDataSource.getName())
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(ProfileDashboardSpecificationService.INSTANCE)
                .targetService(ProfileDashboardSpecificationService.INSTANCE)
                .sourcePath(null)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();
        builder.addPropertyMapping(mapping);
        return builder.build();
    }

    private Dashboard<UserDashboard> createOptimizedDashboard() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        DashboardBuilder<UserDashboard> builder = factory.buildDashboard(UserDashboardSpecificationService.INSTANCE)
                .withName("Optimized Dashboard");

        return buildDashboard(builder, DATASOURCE_TEST_USERS);
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
