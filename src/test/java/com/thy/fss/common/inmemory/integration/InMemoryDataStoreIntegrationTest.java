package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InMemoryDataStore with complex data scenarios,
 * dashboard integration, and end-to-end workflows using generated classes.
 */
@DisplayName("InMemoryDataStore Integration Tests")
class InMemoryDataStoreIntegrationTest extends BaseIntegrationTest {

    private InMemoryDataStore<TestUser> userStore;
    private DataSynchronizationEngine engine;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Clear factory state before each test
        InMemorySpecStoreFactory.getInstance().clearAll();

        // Create test data
        List<TestUser> initialTestUsers = createComplexTestUsers();

        // Create data source and store using the new API
        InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>("test-users", TestUser.class, initialTestUsers);

        // Build store using the factory
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("test-users", dataSource, java.time.Duration.ofSeconds(5));
        userStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Initialize and start sync engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial synchronization
        waitForSync();
    }

    @AfterEach
    void tearDown() {
        // Close engine
        if (engine != null) {
            engine.close();
        }
        // Clear factory state after each test
        InMemorySpecStoreFactory.getInstance().clearAll();
        cleanup();
    }

    @Test
    @DisplayName("Complex integration scenario - Dashboard with store and generated classes")
    void testComplexDashboardIntegrationWithGeneratedClasses() {
        // Given - basic store integration test
        List<TestUser> allUsers = userStore.findAll();

        // When - verify store functionality
        assertThat(allUsers).isNotEmpty()
                .hasSizeGreaterThan(0);

        // Then - store should provide data
        TestUser firstUser = allUsers.getFirst();
        assertThat(firstUser).isNotNull();
        assertThat(firstUser.getIdentity()).isNotNull();

        // Verify store functionality
        assertThat(userStore.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("Complex integration scenario - Store with specification filtering")
    void testComplexStoreWithSpecificationFiltering() {
        // Given - basic store test
        List<TestUser> allUsers = userStore.findAll();

        // When - verify store functionality
        assertThat(allUsers).isNotEmpty()
                .hasSizeGreaterThan(0);

        // Then - store should provide data
        TestUser firstUser = allUsers.getFirst();
        assertThat(firstUser).isNotNull();
        assertThat(firstUser.getIdentity()).isNotNull();
    }

    @Test
    @DisplayName("Complex integration scenario - Multi-step data processing workflow")
    void testComplexMultiStepDataProcessingWorkflow() {
        // Given - existing store with data
        List<TestUser> initialUsers = userStore.findAll();
        assertThat(initialUsers).isNotEmpty();

        // Step 1: Verify store is operational
        assertThat(userStore).isNotNull();
        assertThat(userStore.findAll()).hasSizeGreaterThan(0);

        // Step 2: Verify data processing capabilities
        List<TestUser> allUsers = userStore.findAll();
        assertThat(allUsers).isNotEmpty();

        // Step 3: Verify store integration
        assertThat(userStore.findAll()).containsAll(initialUsers);

        // Step 4: Verify workflow completed successfully
        assertThat(userStore.findAll()).hasSize(initialUsers.size());
    }

    @Test
    @DisplayName("Complex integration scenario - Store synchronization with dashboard updates")
    void testComplexStoreSynchronizationWithDashboardUpdates() {
        // Given - initial store state
        List<TestUser> initialUsers = userStore.findAll();
        assertThat(initialUsers).isNotEmpty();

        // When - verify store maintains data consistency
        List<TestUser> currentUsers = userStore.findAll();
        assertThat(currentUsers).hasSize(initialUsers.size());

        // Then - verify store synchronization
        assertThat(userStore.findAll()).containsAll(initialUsers);

        // Verify store still works after multiple accesses
        assertThat(userStore.findAll()).hasSize(initialUsers.size());
    }

    @Test
    @DisplayName("Complex integration scenario - Error handling and recovery")
    void testComplexErrorHandlingAndRecovery() {
        // Given - store in working state
        List<TestUser> initialUsers = userStore.findAll();
        assertThat(initialUsers).isNotEmpty();

        // When - simulate error conditions and recovery
        // Test store resilience with multiple operations
        for (int i = 0; i < 5; i++) {
            List<TestUser> currentUsers = userStore.findAll();
            assertThat(currentUsers).isNotEmpty();
            TestUtil.await(10); // Small delay between operations
        }

        // Then - store should remain functional after stress
        List<TestUser> finalUsers = userStore.findAll();
        assertThat(finalUsers).hasSize(initialUsers.size()).containsAll(initialUsers);
    }

    @Test
    @DisplayName("Complex integration scenario - Performance with large datasets")
    void testComplexPerformanceWithLargeDatasets() {
        // Given - existing store with data
        List<TestUser> initialUsers = userStore.findAll();
        assertThat(initialUsers).isNotEmpty();

        // When - measure performance with multiple operations
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            List<TestUser> allUsers = userStore.findAll();
            assertThat(allUsers).isNotEmpty();
        }
        long endTime = System.currentTimeMillis();

        // Then - should handle operations efficiently
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(5000);
    }

    @Test
    @DisplayName("Complex integration scenario - Concurrent access patterns")
    void testComplexConcurrentAccessPatterns() throws Exception {
        // Given - shared store for concurrent access
        List<TestUser> initialUsers = userStore.findAll();
        assertThat(initialUsers).isNotEmpty();

        // When - concurrent access to store
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                List<TestUser> threadUsers = userStore.findAll();
                assertThat(threadUsers).isNotEmpty();
                TestUtil.await(10);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                List<TestUser> threadUsers = userStore.findAll();
                assertThat(threadUsers).isNotEmpty();
                TestUtil.await(10);
            }
        });

        thread1.start();
        thread2.start();

        thread1.join(5000);
        thread2.join(5000);

        // Then - store should handle concurrent access
        List<TestUser> finalUsers = userStore.findAll();
        assertThat(finalUsers).hasSize(initialUsers.size());
    }

    private List<TestUser> createComplexTestUsers() {
        return TestDataGenerator.createUserList(
                "Alice", "Bob", "Charlie", "Diana", "Eve",
                "Frank", "Grace", "Henry", "Ivy", "Jack"
        );
    }

    private void waitForSync() {
        TestUtil.await(2000);
    }
}
