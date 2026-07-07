package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test focusing on error handling and edge cases across components.
 * Tests system behavior under failure conditions and recovery scenarios.
 */
@Tag("integration")
@DisplayName("Error Handling Integration Tests")
class ErrorHandlingIntegrationTest extends BaseIntegrationTest {

    private InMemoryDataStore<TestUser> dataStore;
    private ExecutorService executorService;
    private InMemoryDataSource<TestUser> testDataSource;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create test data source with initial data
        List<TestUser> initialData = new ArrayList<>();
        initialData.add(TestDataGenerator.createUser("TestUser", 30));
        testDataSource = new InMemoryDataSource<>("test-source", TestUser.class, initialData);

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("test-source", testDataSource, Duration.ofSeconds(1));
        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually initialize store with data (since DataSyncEngine is not running in tests)
        dataStore.updateData(initialData, 1L);

        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Override
    protected void cleanup() {
        super.cleanup();

        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Close data source
        if (testDataSource != null) {
            testDataSource.close();
        }

        // Unregister datasources created by this test to prevent duplicate registration errors
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.unregisterDataSource("test-source");
        factory.unregisterDataSource("large-source");
        factory.unregisterDataSource("memory-pressure-source");
        factory.unregisterDataSource("interrupt-source");
        factory.unregisterDataSource("corrupted-source");
    }

    @Test
    @DisplayName("Concurrent modification exception handling")
    void testConcurrentModificationHandling() throws Exception {
        // Get initial data size
        int initialSize = dataStore.findAll().size();

        // Simulate concurrent modifications on the data store
        CompletableFuture<Void> modifier1 = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 50; i++) {
                TestUser newUser = TestDataGenerator.createUser("User" + i, 20 + i);
                // Add data to the data source instead of store directly
                testDataSource.addItem(newUser);
                // Small delay between additions to avoid overwhelming the system
                if (i % 10 == 0) {
                    TestUtil.await(10);
                }
            }
        }, executorService);

        CompletableFuture<Void> modifier2 = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 50; i++) {
                // Access store data concurrently
                List<TestUser> currentData = dataStore.findAll();
                assertThat(currentData).isNotNull();
                // Small delay between reads to avoid overwhelming the system
                if (i % 10 == 0) {
                    TestUtil.await(15);
                }
            }
        }, executorService);

        // Wait for completion
        CompletableFuture.allOf(modifier1, modifier2).get(30, TimeUnit.SECONDS);

        // Manually sync the store with datasource (since DataSyncEngine is not running in tests)
        dataStore.updateData(testDataSource.fetchAll().get());

        // System should remain consistent and have more data
        List<TestUser> finalData = dataStore.findAll();
        assertThat(finalData).hasSizeGreaterThan(initialSize);
    }

    @Test
    @DisplayName("Resource exhaustion handling")
    void testResourceExhaustionHandling() {
        // Clear existing datasources to avoid conflicts
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();

        // Create a data source with large amount of data
        List<TestUser> largeDataSet = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestUser user = TestDataGenerator.createUser("User" + i, 20 + i);
            largeDataSet.add(user);
        }

        InMemoryDataSource<TestUser> largeDataSource = new InMemoryDataSource<>("large-source", TestUser.class, largeDataSet);

        factory.registerDataSource("large-source", largeDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> smallStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually initialize store with data
        smallStore.updateData(largeDataSet, 1L);

        // Store should handle the load gracefully
        assertThat(smallStore.findAll()).hasSizeGreaterThan(10);
    }

    @Test
    @DisplayName("Invalid configuration recovery")
    void testInvalidConfigurationRecovery() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        // Clear all datasources to test invalid configuration scenario
        factory.clearAllDataSources();

        // Setup store builder
        var builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);

        // Only call the method expected to throw
        assertThatThrownBy(() -> builder.withPrimaryDataSource(null))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    @DisplayName("Memory pressure handling")
    void testMemoryPressureHandling() {
        // Clear existing datasources to avoid conflicts
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();

        // Create large dataset to simulate memory pressure
        List<TestUser> largeDataSet = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            TestUser user = TestDataGenerator.createUser("User" + i, 20 + (i % 60));
            largeDataSet.add(user);
        }

        InMemoryDataSource<TestUser> largeDataSource = new InMemoryDataSource<>("memory-pressure-source", TestUser.class, largeDataSet);

        factory.registerDataSource("memory-pressure-source", largeDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> largeStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually initialize store with data
        largeStore.updateData(largeDataSet, 1L);

        // Perform multiple concurrent operations to simulate memory pressure
        List<CompletableFuture<Void>> operations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            operations.add(CompletableFuture.runAsync(() -> {
                // Perform multiple read operations
                for (int j = 0; j < 100; j++) {
                    List<TestUser> data = largeStore.findAll();
                    assertThat(data).isNotNull();
                }
            }, executorService));
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(operations.toArray(new CompletableFuture[0])).join();

        // System should remain stable under memory pressure
        assertThat(largeStore.findAll()).hasSize(1000);
    }

    @Test
    @DisplayName("Interrupted operation handling")
    void testInterruptedOperationHandling() {
        // Clear existing datasources to avoid conflicts
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();

        // Add data to source
        List<TestUser> interruptTestData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestUser user = TestDataGenerator.createUser("User" + i, 25);
            interruptTestData.add(user);
        }

        InMemoryDataSource<TestUser> interruptDataSource = new InMemoryDataSource<>("interrupt-source", TestUser.class, interruptTestData);

        factory.registerDataSource("interrupt-source", interruptDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> interruptStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually initialize store with data
        interruptStore.updateData(interruptTestData, 1L);

        // Start long-running operation and interrupt it
        CompletableFuture<Void> operation = CompletableFuture.runAsync(() -> {

            for (int i = 0; i < 1000; i++) {
                // Perform store operations that could be interrupted
                List<TestUser> data = interruptStore.findAll();
                assertThat(data).isNotNull();
                // Simulate work
                if (i % 100 == 0) {
                    TestUtil.await(1);
                }
            }

        }, executorService);

        // Cancel the operation
        TestUtil.await(100);
        operation.cancel(true);

        // System should remain stable after interruption
        assertThat(interruptStore.findAll()).hasSize(100);
    }

    @Test
    @DisplayName("Data corruption recovery")
    void testDataCorruptionRecovery() {
        // Clear existing datasources to avoid conflicts
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();

        // Create data with valid and corrupted entries
        List<TestUser> corruptedDataSet = new ArrayList<>();

        // Add valid data
        TestUser validUser = TestDataGenerator.createUser("ValidUser", 30);
        corruptedDataSet.add(validUser);

        // Add user with corrupted/invalid data
        TestUser corruptedUser = new TestUser();
        corruptedUser.setName(""); // Empty name
        corruptedUser.setAge(-1);  // Invalid age
        corruptedDataSet.add(corruptedUser);

        InMemoryDataSource<TestUser> corruptedDataSource = new InMemoryDataSource<>("corrupted-source", TestUser.class, corruptedDataSet);

        factory.registerDataSource("corrupted-source", corruptedDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> corruptedStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Manually initialize store with data
        corruptedStore.updateData(corruptedDataSet, 1L);

        // Verify store handles corrupted data gracefully
        List<TestUser> allData = corruptedStore.findAll();
        final String validUserName = "ValidUser";
        final String emptyName = "";
        assertThat(allData)
                .satisfies(data -> {
                    assertThat(data).hasSize(2);
                    // Verify we can still query the data despite corruption
                    assertThat(data.stream().anyMatch(u -> validUserName.equals(u.getName()))).isTrue();
                    assertThat(data.stream().anyMatch(u -> emptyName.equals(u.getName()))).isTrue();
                });
    }

    @Test
    @DisplayName("System recovery after component failure")
    void testSystemRecoveryAfterComponentFailure() {
        // Verify initial state
        int initialSize = dataStore.findAll().size();

        // Simulate recovery scenario by adding more data dynamically
        // This tests the system's ability to handle data changes and continue working
        for (int i = 0; i < 50; i++) {
            TestUser newUser = TestDataGenerator.createUser("RecoveryUser" + i, 30 + i);
            testDataSource.addItem(newUser);
        }

        // Manually sync the store with datasource (since DataSyncEngine is not running in tests)
        try {
            dataStore.updateData(testDataSource.fetchAll().get());
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync data", e);
        }

        // Verify system recovered and is working with new data
        List<TestUser> finalData = dataStore.findAll();
        final String recoveryPrefix = "RecoveryUser";
        assertThat(finalData)
                .satisfies(data -> {
                    assertThat(data).hasSizeGreaterThan(initialSize);
                    // Verify we can still perform operations after the "recovery"
                    assertThat(data.stream().anyMatch(u -> u.getName().startsWith(recoveryPrefix))).isTrue();
                });
    }
}
