package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and scalability integration tests.
 * Tests system behavior under various load conditions and performance requirements.
 * <p>
 * Note: Dashboard-specific tests have been removed as Dashboard synchronization
 * is now handled by DataSynchronizationEngine centrally.
 */
@Tag("integration")
@Tag("performance")
@DisplayName("Performance and Scalability Integration Tests")
class PerformanceScalabilityIntegrationTest extends BaseIntegrationTest {

    private InMemoryDataStore<TestUser> dataStore;
    private ExecutorService executorService;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create dataStore with empty initial data - will be populated in individual tests
        List<TestUser> emptyData = new ArrayList<>();
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryDataSource<TestUser> emptyDataSource = new InMemoryDataSource<>("EmptyDataSource", TestUser.class, emptyData);
        factory.registerDataSource("EmptyDataSource", emptyDataSource, java.time.Duration.ofSeconds(5));
        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();
        executorService = Executors.newFixedThreadPool(8);
    }

    @Test
    @DisplayName("Large dataset performance test")
    void testLargeDatasetPerformance() {
        Instant start = Instant.now();

        // Create large dataset
        List<TestUser> users = TestDataGenerator.createUserList(10000);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryDataSource<TestUser> largeDataSource = new InMemoryDataSource<>("TestUserDataSource", TestUser.class, users);
        factory.registerDataSource("TestUserDataSource", largeDataSource, java.time.Duration.ofSeconds(5));
        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE).withPrimaryDataSource(TestUser.class).build();

        Duration insertTime = Duration.between(start, Instant.now());

        // Insert should be reasonably fast (less than 5 seconds for 10k records)
        assertThat(insertTime.toMillis()).isLessThan(5000);

        // Test query performance
        start = Instant.now();
        List<TestUser> allUsers = dataStore.findAll();
        Duration queryTime = Duration.between(start, Instant.now());

        assertThat(allUsers).hasSize(10000);
        assertThat(queryTime.toMillis()).isLessThan(1000); // Query should be fast
    }

    @Test
    @DisplayName("Concurrent access scalability test")
    void testConcurrentAccessScalability() throws Exception {
        // Pre-populate with data
        List<TestUser> users = TestDataGenerator.createUserList(1000);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryDataSource<TestUser> concurrentDataSource = new InMemoryDataSource<>("TestUserDataSource", TestUser.class, users);
        factory.registerDataSource("TestUserDataSource-concurrent", concurrentDataSource, java.time.Duration.ofSeconds(5));
        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        Instant start = Instant.now();

        // Create multiple concurrent operations
        CompletableFuture<?>[] futures = IntStream.range(0, 20).mapToObj(i -> CompletableFuture.runAsync(() -> {
            // Each thread performs multiple operations
            for (int j = 0; j < 50; j++) {
                // Concurrent read operations
                dataStore.findAll();
            }
        }, executorService)).toArray(CompletableFuture[]::new);

        // Wait for all operations to complete
        CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);

        Duration totalTime = Duration.between(start, Instant.now());

        // All operations should complete within reasonable time
        assertThat(totalTime.toSeconds()).isLessThan(30);

        // Verify data consistency
        assertThat(dataStore.findAll()).hasSize(1000);
    }

    @Test
    @DisplayName("Memory usage scalability test")
    void testMemoryUsageScalability() {
        Runtime runtime = Runtime.getRuntime();

        // Measure baseline memory
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

        // Add increasingly large datasets
        for (int batchSize = 1000; batchSize <= 10000; batchSize += 1000) {
            List<TestUser> batch = TestDataGenerator.createUserList(1000);
            // Create new datastore with updated data since InMemoryDataStore doesn't have save method
            InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
            InMemoryDataSource<TestUser> batchDataSource = new InMemoryDataSource<>("TestUserDataSource-batch-" + batchSize, TestUser.class,
                    !dataStore.findAll().isEmpty() ?
                            java.util.stream.Stream.concat(dataStore.findAll().stream(), batch.stream()).toList() :
                            batch);
            factory.registerDataSource("TestUserDataSource-batch-" + batchSize, batchDataSource, java.time.Duration.ofSeconds(5));
            dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            // Check memory usage
            System.gc();
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = currentMemory - baselineMemory;

            // Memory increase should be reasonable (less than 200MB for 10k records)
            assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024);
        }

        assertThat(dataStore.findAll()).hasSize(10000);
    }

    @Test
    @DisplayName("Store query performance under load")
    void testStoreQueryPerformanceUnderLoad() {
        // Create large dataset
        List<TestUser> users = TestDataGenerator.createUserList(5000);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryDataSource<TestUser> dataSource = new InMemoryDataSource<>("TestUserDataSource", TestUser.class, users);
        factory.registerDataSource("TestUserDataSource-query", dataSource, java.time.Duration.ofSeconds(5));
        dataStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Measure query performance
        Instant start = Instant.now();

        for (int i = 0; i < 100; i++) {
            List<TestUser> result = dataStore.findAll();
            assertThat(result).hasSize(5000);
        }

        Duration queryTime = Duration.between(start, Instant.now());

        // 100 queries should complete within reasonable time
        assertThat(queryTime.toSeconds()).isLessThan(10);
    }

    @Test
    @DisplayName("Throughput test with continuous operations")
    void testThroughputWithContinuousOperations() throws Exception {
        Instant start = Instant.now();
        final int operationCount = 10000;

        // Perform continuous read operations
        CompletableFuture<Void> operations = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < operationCount; i++) {
                dataStore.findAll();
            }
        }, executorService);

        operations.get(60, TimeUnit.SECONDS);

        Duration totalTime = Duration.between(start, Instant.now());
        double totalSeconds = totalTime.toSeconds();
        double operationsPerSecond = totalSeconds > 0 ? operationCount / totalSeconds : operationCount;

        // Should achieve reasonable throughput (at least 1000 ops/sec)
        assertThat(operationsPerSecond).isGreaterThan(1000);
    }

    @Test
    @DisplayName("Stress test with resource constraints")
    void testStressWithResourceConstraints() throws Exception {
        // Create multiple data stores to simulate resource pressure
        @SuppressWarnings("unchecked")
        InMemoryDataStore<TestUser>[] stores = new InMemoryDataStore[10];

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        for (int i = 0; i < 10; i++) {
            // Create initial data for each store
            List<TestUser> initialData = TestDataGenerator.createUserList(500);
            InMemoryDataSource<TestUser> stressDataSource = new InMemoryDataSource<>("StressTestDataSource" + i, TestUser.class, initialData);
            factory.registerDataSource("StressTestDataSource" + i, stressDataSource, java.time.Duration.ofSeconds(5));
            stores[i] = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();
        }

        // Perform operations on all stores concurrently
        CompletableFuture<?>[] futures = IntStream.range(0, 10).mapToObj(storeIndex -> CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                // Simulate stress by performing read operations
                stores[storeIndex].findAll();
            }
        }, executorService)).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);

        // Verify all systems remain healthy under stress
        for (int i = 0; i < 10; i++) {
            assertThat(stores[i].findAll()).hasSize(500);
        }
    }
}