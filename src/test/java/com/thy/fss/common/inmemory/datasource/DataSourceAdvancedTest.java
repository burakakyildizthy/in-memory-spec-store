package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.entity.Identifiable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceAdvancedTest {

    private static final String TEST_SOURCE = "test-source";

    private InMemoryDataSource<TestEntity> dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new InMemoryDataSource<>(TEST_SOURCE, TestEntity.class);
        dataSource.addItems(Arrays.asList(
                new TestEntity(1L, "Alice", 25, true),
                new TestEntity(2L, "Bob", 30, false),
                new TestEntity(3L, "Charlie", 35, true),
                new TestEntity(4L, "David", 40, false),
                new TestEntity(5L, "Eve", 28, true)
        ));
    }

    @Test
    @DisplayName("Should fetch all data asynchronously")
    void shouldFetchAllDataAsynchronously() throws ExecutionException, InterruptedException {
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAll();

        assertNotNull(future);
        // Note: InMemoryDataSource might complete immediately

        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(5, results.size());
        assertTrue(future.isDone());
    }

    @Test
    @DisplayName("Should fetch all data with fallback")
    void shouldFetchAllDataWithFallback() throws ExecutionException, InterruptedException {
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllWithFallback();

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    @DisplayName("Should fetch data by IDs")
    void shouldFetchDataByIds() throws ExecutionException, InterruptedException {
        List<Object> ids = Arrays.asList(1L, 3L, 5L);
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(ids);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify correct entities were fetched
        assertTrue(results.stream().anyMatch(e -> e.getIdentity().equals(1L)));
        assertTrue(results.stream().anyMatch(e -> e.getIdentity().equals(3L)));
        assertTrue(results.stream().anyMatch(e -> e.getIdentity().equals(5L)));
    }

    @Test
    @DisplayName("Should fetch data by IDs with fallback")
    void shouldFetchDataByIdsWithFallback() throws ExecutionException, InterruptedException {
        List<Object> ids = Arrays.asList(1L, 3L, 5L);
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllByIdWithFallback(ids);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    @DisplayName("Should handle empty ID list")
    void shouldHandleEmptyIdList() throws ExecutionException, InterruptedException {
        List<Object> emptyIds = List.of();
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(emptyIds);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle null ID list")
    void shouldHandleNullIdList() throws ExecutionException, InterruptedException {
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(null);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle non-existent IDs")
    void shouldHandleNonExistentIds() throws ExecutionException, InterruptedException {
        List<Object> nonExistentIds = Arrays.asList(99L, 100L);
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(nonExistentIds);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle mixed existing and non-existent IDs")
    void shouldHandleMixedExistingAndNonExistentIds() throws ExecutionException, InterruptedException {
        List<Object> mixedIds = Arrays.asList(1L, 99L, 3L, 100L);
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(mixedIds);

        assertNotNull(future);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(2, results.size()); // Only existing IDs (1L, 3L)

        assertTrue(results.stream().anyMatch(e -> e.getIdentity().equals(1L)));
        assertTrue(results.stream().anyMatch(e -> e.getIdentity().equals(3L)));
    }

    @Test
    @DisplayName("Should get data source name")
    void shouldGetDataSourceName() {
        assertEquals(TEST_SOURCE, dataSource.getName());
    }

    @Test
    @DisplayName("Should get entity type")
    void shouldGetEntityType() {
        assertEquals(TestEntity.class, dataSource.getEntityType());
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void shouldHandleConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final CompletableFuture<List<TestEntity>>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                futures[index] = dataSource.fetchAll();
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all futures completed successfully
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            assertNotNull(futures[index]);
            assertDoesNotThrow(() -> {
                List<TestEntity> results = futures[index].get();
                assertNotNull(results);
                assertEquals(5, results.size());
            });
        }
    }

    @Test
    @DisplayName("Should handle data source with no items")
    void shouldHandleDataSourceWithNoItems() throws ExecutionException, InterruptedException {
        InMemoryDataSource<TestEntity> emptyDataSource = new InMemoryDataSource<>("empty-source", TestEntity.class);

        CompletableFuture<List<TestEntity>> future = emptyDataSource.fetchAll();
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle adding items after creation")
    void shouldHandleAddingItemsAfterCreation() throws ExecutionException, InterruptedException {
        InMemoryDataSource<TestEntity> dynamicDataSource = new InMemoryDataSource<>("dynamic-source", TestEntity.class);

        // Initially empty
        List<TestEntity> initialResults = dynamicDataSource.fetchAll().get();
        assertTrue(initialResults.isEmpty());

        // Add items
        dynamicDataSource.addItems(Arrays.asList(
                new TestEntity(1L, "Test1", 25, true),
                new TestEntity(2L, "Test2", 30, false)
        ));

        // Should now have items
        List<TestEntity> updatedResults = dynamicDataSource.fetchAll().get();
        assertEquals(2, updatedResults.size());
    }

    @Test
    @DisplayName("Should handle null items in add operation")
    void shouldHandleNullItemsInAddOperation() {
        assertDoesNotThrow(() -> {
            dataSource.addItems(null);
        });

        // Original data should still be there
        assertDoesNotThrow(() -> {
            List<TestEntity> results = dataSource.fetchAll().get();
            assertEquals(5, results.size());
        });
    }

    @Test
    @DisplayName("Should handle empty items list in add operation")
    void shouldHandleEmptyItemsListInAddOperation() {
        assertDoesNotThrow(() -> {
            dataSource.addItems(List.of());
        });

        // Original data should still be there
        assertDoesNotThrow(() -> {
            List<TestEntity> results = dataSource.fetchAll().get();
            assertEquals(5, results.size());
        });
    }

    @Test
    @DisplayName("Should handle different ID types")
    void shouldHandleDifferentIdTypes() throws ExecutionException, InterruptedException {
        // Test with String IDs
        List<Object> stringIds = Arrays.asList("1", "3", "5");
        CompletableFuture<List<TestEntity>> future1 = dataSource.fetchAllById(stringIds);
        List<TestEntity> results1 = future1.get();
        assertTrue(results1.isEmpty()); // String IDs won't match Long IDs

        // Test with Integer IDs
        List<Object> intIds = Arrays.asList(1, 3, 5);
        CompletableFuture<List<TestEntity>> future2 = dataSource.fetchAllById(intIds);
        List<TestEntity> results2 = future2.get();
        assertTrue(results2.isEmpty()); // Integer IDs won't match Long IDs

        // Test with correct Long IDs
        List<Object> longIds = Arrays.asList(1L, 3L, 5L);
        CompletableFuture<List<TestEntity>> future3 = dataSource.fetchAllById(longIds);
        List<TestEntity> results3 = future3.get();
        assertEquals(3, results3.size()); // Long IDs should match
    }

    @Test
    @DisplayName("Should maintain data integrity across multiple operations")
    void shouldMaintainDataIntegrityAcrossMultipleOperations() throws ExecutionException, InterruptedException {
        // Fetch all data multiple times
        List<TestEntity> results1 = dataSource.fetchAll().get();
        List<TestEntity> results2 = dataSource.fetchAll().get();
        List<TestEntity> results3 = dataSource.fetchAllWithFallback().get();

        // All results should be consistent
        assertEquals(results1.size(), results2.size());
        assertEquals(results2.size(), results3.size());

        // Verify data consistency
        for (int i = 0; i < results1.size(); i++) {
            TestEntity entity1 = results1.get(i);
            TestEntity entity2 = results2.get(i);
            TestEntity entity3 = results3.get(i);

            assertEquals(entity1.getIdentity(), entity2.getIdentity());
            assertEquals(entity2.getIdentity(), entity3.getIdentity());
            assertEquals(entity1.name(), entity2.name());
            assertEquals(entity2.name(), entity3.name());
        }
    }

    @Test
    @DisplayName("Should handle large ID lists efficiently")
    void shouldHandleLargeIdListsEfficiently() throws ExecutionException, InterruptedException {
        // Create a large list of IDs (some existing, some not)
        List<Object> largeIdList = new java.util.ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            largeIdList.add(i);
        }

        CompletableFuture<List<TestEntity>> future = dataSource.fetchAllById(largeIdList);
        List<TestEntity> results = future.get();

        assertNotNull(results);
        assertEquals(5, results.size()); // Only 5 entities exist (IDs 1-5)
    }

    record TestEntity(Long getIdentity, String name, int age, boolean active) implements Identifiable<Long> {
    }
}