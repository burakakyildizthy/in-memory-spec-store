package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.exception.DataSourceException;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import com.thy.fss.common.inmemory.testmodel.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryDataSourceTest {

    private static final String TEST_SOURCE = "test-source";
    private static final String TEST = "test";
    private static final String ITEM1 = "item1";
    private static final String ITEM2 = "item2";
    private static final String ITEM3 = "item3";
    private static final String INT_SOURCE = "int-source";
    private static final String TEST1 = "test1";
    private static final String SINGLE1 = "single1";
    private static final String SINGLE2 = "single2";
    private static final String BATCH1 = "batch1";
    private static final String BATCH2 = "batch2";
    private static final String BATCH3 = "batch3";
    private static final String INIT1 = "init1";
    private static final String INIT2 = "init2";
    private static final String INIT3 = "init3";
    private static final String USERS = "users";
    private static final String FALLBACK = "fallback";
    private static final String NOT_HEALTHY = "not healthy";
    private static final String CONCURRENT = "concurrent";
    private static final String DS1_ITEM1 = "ds1-item1";
    private static final String DS1_ITEM2 = "ds1-item2";
    private static final String DS2_ITEM1 = "ds2-item1";
    private static final String DS2_ITEM2 = "ds2-item2";
    private static final String DS2_ITEM3 = "ds2-item3";
    private static final String CUSTOMERS = "customers";

    private InMemoryDataSource<String> dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new InMemoryDataSource<>(TEST_SOURCE, String.class);
    }

    @Test
    @DisplayName("Should create data source with name and type")
    void shouldCreateDataSourceWithNameAndType() {
        assertEquals(TEST_SOURCE, dataSource.getName());
        assertEquals(String.class, dataSource.getEntityType());
    }

    @Test
    @DisplayName("Should throw exception for null name")
    void shouldThrowExceptionForNullName() {
        assertThrows(NullPointerException.class, () -> new InMemoryDataSource<>(null, String.class));
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // Empty name might be allowed, just test it doesn't crash
        InMemoryDataSource<String> ds = new InMemoryDataSource<>("", String.class);
        assertThat(ds.getName()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for null entity type")
    void shouldThrowExceptionForNullEntityType() {
        assertThrows(NullPointerException.class, () -> new InMemoryDataSource<>(TEST, null));
    }

    @Test
    @DisplayName("Should start with empty data")
    void shouldStartWithEmptyData() {
        assertEquals(0, dataSource.size());
    }

    @Test
    @DisplayName("Should add single item")
    void shouldAddSingleItem() {
        dataSource.addItem("test-item");

        assertEquals(1, dataSource.size());

        List<String> data = dataSource.fetchAll().join();
        assertEquals(1, data.size());
        assertEquals("test-item", data.get(0));
    }

    @Test
    @DisplayName("Should add multiple items")
    void shouldAddMultipleItems() {
        List<String> items = Arrays.asList(ITEM1, ITEM2, ITEM3);
        dataSource.addItems(items);

        assertEquals(3, dataSource.size());

        List<String> data = dataSource.fetchAll().join();
        assertEquals(3, data.size());
        assertEquals(ITEM1, data.get(0));
        assertEquals(ITEM2, data.get(1));
        assertEquals(ITEM3, data.get(2));
    }

    @Test
    @DisplayName("Should handle null item")
    void shouldHandleNullItem() {
        dataSource.addItem(null);

        // Null items are not added according to the implementation
        assertEquals(0, dataSource.size());
    }

    @Test
    @DisplayName("Should handle null items collection")
    void shouldHandleNullItemsCollection() {
        // Null collections are handled gracefully
        dataSource.addItems(null);
        assertEquals(0, dataSource.size());
    }

    @Test
    @DisplayName("Should handle empty items collection")
    void shouldHandleEmptyItemsCollection() {
        dataSource.addItems(List.of());

        assertEquals(0, dataSource.size());
    }

    @Test
    @DisplayName("Should clear all data")
    void shouldClearAllData() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));
        assertEquals(3, dataSource.size());

        dataSource.clearData();

        assertEquals(0, dataSource.size());
    }

    @Test
    @DisplayName("Should be healthy by default")
    void shouldBeHealthyByDefault() {
        assertTrue(dataSource.isHealthy());
    }

    @Test
    @DisplayName("Should return immutable data list")
    void shouldReturnImmutableDataList() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2));

        List<String> data = dataSource.fetchAll().join();

        // Should be able to modify the returned list (it's a copy)
        assertDoesNotThrow(() -> {
            data.add(ITEM3);
        });

        // But original data should remain unchanged
        assertEquals(2, dataSource.size());
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void shouldHandleConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int itemsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++) {
                    dataSource.addItem("thread-" + threadIndex + "-item-" + j);
                }
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

        // Verify total count
        assertEquals(threadCount * itemsPerThread, dataSource.size());
    }

    @Test
    @DisplayName("Should handle large datasets")
    void shouldHandleLargeDatasets() {
        final int itemCount = 10000;

        for (int i = 0; i < itemCount; i++) {
            dataSource.addItem("item-" + i);
        }

        assertEquals(itemCount, dataSource.size());

        List<String> data = dataSource.fetchAll().join();
        assertEquals(itemCount, data.size());
        assertEquals("item-0", data.get(0));
        assertEquals("item-" + (itemCount - 1), data.get(itemCount - 1));
    }

    @Test
    @DisplayName("Should work with different entity types")
    void shouldWorkWithDifferentEntityTypes() {
        InMemoryDataSource<Integer> intDataSource = new InMemoryDataSource<>(INT_SOURCE, Integer.class);
        intDataSource.addItems(Arrays.asList(1, 2, 3, 4, 5));

        assertEquals(INT_SOURCE, intDataSource.getName());
        assertEquals(Integer.class, intDataSource.getEntityType());
        assertEquals(5, intDataSource.size());

        List<Integer> data = intDataSource.fetchAll().join();
        assertEquals(Integer.valueOf(1), data.get(0));
    }

    @Test
    @DisplayName("Should work with custom objects")
    void shouldWorkWithCustomObjects() {
        record TestEntity(String name, int value) {
        }

        InMemoryDataSource<TestEntity> entityDataSource = new InMemoryDataSource<>("entity-source", TestEntity.class);
        TestEntity entity1 = new TestEntity(TEST1, 100);
        TestEntity entity2 = new TestEntity("test2", 200);

        entityDataSource.addItems(Arrays.asList(entity1, entity2));

        assertEquals(2, entityDataSource.size());

        List<TestEntity> data = entityDataSource.fetchAll().join();
        assertEquals(TEST1, data.get(0).name());
        assertEquals(200, data.get(1).value());
    }

    @Test
    @DisplayName("Should maintain insertion order")
    void shouldMaintainInsertionOrder() {
        List<String> items = Arrays.asList("first", "second", "third", "fourth", "fifth");
        dataSource.addItems(items);

        List<String> data = dataSource.fetchAll().join();
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i), data.get(i));
        }
    }

    @Test
    @DisplayName("Should handle mixed add operations")
    void shouldHandleMixedAddOperations() {
        dataSource.addItem(SINGLE1);
        dataSource.addItems(Arrays.asList(BATCH1, BATCH2));
        dataSource.addItem(SINGLE2);
        dataSource.addItems(List.of(BATCH3));

        assertEquals(5, dataSource.size());

        List<String> data = dataSource.fetchAll().join();
        assertEquals(5, data.size());
        assertEquals(SINGLE1, data.get(0));
        assertEquals(BATCH1, data.get(1));
        assertEquals(BATCH2, data.get(2));
        assertEquals(SINGLE2, data.get(3));
        assertEquals(BATCH3, data.get(4));
    }

    @Test
    @DisplayName("Should handle clear and re-add operations")
    void shouldHandleClearAndReAddOperations() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));
        assertEquals(3, dataSource.size());

        dataSource.clearData();
        assertEquals(0, dataSource.size());

        dataSource.addItems(Arrays.asList("new1", "new2"));
        assertEquals(2, dataSource.size());

        List<String> data = dataSource.fetchAll().join();
        assertEquals("new1", data.get(0));
        assertEquals("new2", data.get(1));
    }

    // ==================== Comprehensive Tests for All Operations ====================

    @Test
    @DisplayName("Should create with initial data")
    void shouldCreateWithInitialData() {
        List<String> initialData = Arrays.asList(INIT1, INIT2, INIT3);
        InMemoryDataSource<String> ds = new InMemoryDataSource<>(TEST, String.class, initialData);

        assertThat(ds.getName()).isEqualTo(TEST);
        assertThat(ds.getEntityType()).isEqualTo(String.class);
        assertThat(ds.size()).isEqualTo(3);
        assertThat(ds.isHealthy()).isTrue();

        List<String> data = ds.fetchAll().join();
        assertThat(data).containsExactly(INIT1, INIT2, INIT3);
    }

    @Test
    @DisplayName("Should handle null initial data")
    void shouldHandleNullInitialData() {
        InMemoryDataSource<String> ds = new InMemoryDataSource<>(TEST, String.class, null);

        assertThat(ds.size()).isZero();
        assertThat(ds.fetchAll().join()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty initial data")
    void shouldHandleEmptyInitialData() {
        InMemoryDataSource<String> ds = new InMemoryDataSource<>(TEST, String.class, Collections.emptyList());

        assertThat(ds.size()).isZero();
        assertThat(ds.fetchAll().join()).isEmpty();
    }

    @Test
    @DisplayName("Should fetch all data asynchronously")
    void shouldFetchAllDataAsynchronously() throws ExecutionException, InterruptedException {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));

        CompletableFuture<List<String>> future = dataSource.fetchAll();
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();

        List<String> data = future.get();
        assertThat(data).hasSize(3).containsExactly(ITEM1, ITEM2, ITEM3);
    }

    @Test
    @DisplayName("Should throw exception when fetching from unhealthy datasource")
    void shouldThrowExceptionWhenFetchingFromUnhealthyDatasource() {
        TestableInMemoryDataSource<String> testDs = new TestableInMemoryDataSource<>(TEST, String.class);
        testDs.addItems(Arrays.asList(ITEM1, ITEM2));
        testDs.setHealthy(false);

        assertThatThrownBy(() -> testDs.fetchAll().join())
                .hasCauseInstanceOf(DataSourceException.class)
                .hasMessageContaining(NOT_HEALTHY);
    }

    @Test
    @DisplayName("Should fetch by IDs with Identifiable entities")
    void shouldFetchByIdsWithIdentifiableEntities() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<User> users = generator.generateUsers(100);

        InMemoryDataSource<User> userDs = new InMemoryDataSource<>(USERS, User.class, users);

        List<Object> idsToFetch = Arrays.asList("0", "10", "50", "99");
        List<User> fetchedUsers = userDs.fetchAllById(idsToFetch).join();

        assertThat(fetchedUsers).hasSize(4);
        assertThat(fetchedUsers).extracting(User::getIdentity)
                .containsExactlyInAnyOrder("0", "10", "50", "99");
    }

    @Test
    @DisplayName("Should return empty list when fetching by null IDs")
    void shouldReturnEmptyListWhenFetchingByNullIds() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2));

        List<String> result = dataSource.fetchAllById(null).join();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when fetching by empty IDs")
    void shouldReturnEmptyListWhenFetchingByEmptyIds() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2));

        List<String> result = dataSource.fetchAllById(Collections.emptyList()).join();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle non-existent IDs when fetching by ID")
    void shouldHandleNonExistentIdsWhenFetchingById() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<User> users = generator.generateUsers(10);

        InMemoryDataSource<User> userDs = new InMemoryDataSource<>(USERS, User.class, users);

        List<Object> idsToFetch = Arrays.asList("999", "1000", "2000");
        List<User> fetchedUsers = userDs.fetchAllById(idsToFetch).join();

        assertThat(fetchedUsers).isEmpty();
    }

    @Test
    @DisplayName("Should handle mixed existing and non-existing IDs")
    void shouldHandleMixedExistingAndNonExistingIds() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<User> users = generator.generateUsers(10);

        InMemoryDataSource<User> userDs = new InMemoryDataSource<>(USERS, User.class, users);

        List<Object> idsToFetch = Arrays.asList("0", "999", "5", "1000");
        List<User> fetchedUsers = userDs.fetchAllById(idsToFetch).join();

        assertThat(fetchedUsers).hasSize(2);
        assertThat(fetchedUsers).extracting(User::getIdentity)
                .containsExactlyInAnyOrder("0", "5");
    }

    @Test
    @DisplayName("Should throw exception when fetching by ID from unhealthy datasource")
    void shouldThrowExceptionWhenFetchingByIdFromUnhealthyDatasource() {
        TestableInMemoryDataSource<String> testDs = new TestableInMemoryDataSource<>(TEST, String.class);
        testDs.addItems(Arrays.asList(ITEM1, ITEM2));
        testDs.setHealthy(false);

        assertThatThrownBy(() -> testDs.fetchAllById(Arrays.asList(ITEM1)).join())
                .hasCauseInstanceOf(DataSourceException.class)
                .hasMessageContaining(NOT_HEALTHY);
    }

    @Test
    @DisplayName("Should handle fallback datasource")
    void shouldHandleFallbackDatasource() {
        InMemoryDataSource<String> primary = new InMemoryDataSource<>("primary", String.class);
        InMemoryDataSource<String> fallback = new InMemoryDataSource<>(FALLBACK, String.class);

        primary.setFallbackDataSource(fallback);

        Optional<DataSource<String>> fallbackOpt = primary.getFallbackDataSource();
        assertThat(fallbackOpt).isPresent().contains(fallback);
    }

    @Test
    @DisplayName("Should return empty optional when no fallback datasource")
    void shouldReturnEmptyOptionalWhenNoFallbackDatasource() {
        Optional<DataSource<String>> fallbackOpt = dataSource.getFallbackDataSource();
        assertThat(fallbackOpt).isEmpty();
    }

    @Test
    @DisplayName("Should set fallback datasource to null")
    void shouldSetFallbackDatasourceToNull() {
        InMemoryDataSource<String> fallback = new InMemoryDataSource<>(FALLBACK, String.class);
        dataSource.setFallbackDataSource(fallback);

        assertThat(dataSource.getFallbackDataSource()).isPresent();

        dataSource.setFallbackDataSource(null);
        assertThat(dataSource.getFallbackDataSource()).isEmpty();
    }

    @Test
    @DisplayName("Should close datasource and clear data")
    void shouldCloseDatasourceAndClearData() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));
        assertThat(dataSource.size()).isEqualTo(3);
        assertThat(dataSource.isHealthy()).isTrue();

        dataSource.close();

        assertThat(dataSource.size()).isZero();
        assertThat(dataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should not fetch data after close")
    void shouldNotFetchDataAfterClose() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2));
        dataSource.close();

        assertThatThrownBy(() -> dataSource.fetchAll().join())
                .hasCauseInstanceOf(DataSourceException.class)
                .hasMessageContaining(NOT_HEALTHY);
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2));

        dataSource.close();
        assertThat(dataSource.size()).isZero();
        assertThat(dataSource.isHealthy()).isFalse();

        // Second close should not throw exception
        assertDoesNotThrow(() -> dataSource.close());
        assertThat(dataSource.size()).isZero();
        assertThat(dataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should handle concurrent reads")
    void shouldHandleConcurrentReads() throws InterruptedException {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<User> users = generator.generateUsers(1000);
        InMemoryDataSource<User> userDs = new InMemoryDataSource<>(USERS, User.class, users);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<User> data = userDs.fetchAll().join();
                    if (data.size() == 1000) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await();

        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Should handle concurrent writes and reads")
    void shouldHandleConcurrentWritesAndReads() throws InterruptedException {
        InMemoryDataSource<String> ds = new InMemoryDataSource<>(CONCURRENT, String.class);

        int writerThreads = 5;
        int readerThreads = 5;
        int itemsPerWriter = 100;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(writerThreads + readerThreads);

        // Writer threads
        for (int i = 0; i < writerThreads; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < itemsPerWriter; j++) {
                        ds.addItem("writer-" + threadIndex + "-item-" + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        // Reader threads
        for (int i = 0; i < readerThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        ds.fetchAll().join();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await();

        assertThat(ds.size()).isEqualTo(writerThreads * itemsPerWriter);
    }

    @Test
    @DisplayName("Should handle large dataset - 10K entities")
    void shouldHandleLargeDataset10K() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Customer> customers = generator.generateCustomers(10_000);

        InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(CUSTOMERS, Customer.class);
        customerDs.addItems(customers);

        assertThat(customerDs.size()).isEqualTo(10_000);

        List<Customer> fetchedCustomers = customerDs.fetchAll().join();
        assertThat(fetchedCustomers).hasSize(10_000);
        assertThat(fetchedCustomers.get(0).getIdentity()).isZero();
        assertThat(fetchedCustomers.get(9999).getIdentity()).isEqualTo(9999L);
    }

    @Test
    @DisplayName("Should handle large dataset with fetchById - 10K entities")
    void shouldHandleLargeDatasetWithFetchById10K() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Customer> customers = generator.generateCustomers(10_000);

        InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(CUSTOMERS, Customer.class, customers);

        List<Object> idsToFetch = Arrays.asList(0L, 1000L, 5000L, 9999L);
        List<Customer> fetchedCustomers = customerDs.fetchAllById(idsToFetch).join();

        assertThat(fetchedCustomers).hasSize(4);
        assertThat(fetchedCustomers).extracting(Customer::getIdentity)
                .containsExactlyInAnyOrder(0L, 1000L, 5000L, 9999L);
    }

    @Test
    @DisplayName("Should maintain data isolation between datasources")
    void shouldMaintainDataIsolationBetweenDatasources() {
        InMemoryDataSource<String> ds1 = new InMemoryDataSource<>("ds1", String.class);
        InMemoryDataSource<String> ds2 = new InMemoryDataSource<>("ds2", String.class);

        ds1.addItems(Arrays.asList(DS1_ITEM1, DS1_ITEM2));
        ds2.addItems(Arrays.asList(DS2_ITEM1, DS2_ITEM2, DS2_ITEM3));

        assertThat(ds1.size()).isEqualTo(2);
        assertThat(ds2.size()).isEqualTo(3);

        List<String> data1 = ds1.fetchAll().join();
        List<String> data2 = ds2.fetchAll().join();

        assertThat(data1).containsExactly(DS1_ITEM1, DS1_ITEM2);
        assertThat(data2).containsExactly(DS2_ITEM1, DS2_ITEM2, DS2_ITEM3);
    }

    @Test
    @DisplayName("Should return defensive copy of data")
    void shouldReturnDefensiveCopyOfData() {
        dataSource.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));

        List<String> data1 = dataSource.fetchAll().join();
        List<String> data2 = dataSource.fetchAll().join();

        // Modify first copy
        data1.add("modified");

        // Second copy should be unaffected
        assertThat(data2).hasSize(3).containsExactly(ITEM1, ITEM2, ITEM3);

        // Original datasource should be unaffected
        assertThat(dataSource.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        InMemoryDataSource<String> ds1 = new InMemoryDataSource<>(TEST, String.class);
        InMemoryDataSource<String> ds2 = new InMemoryDataSource<>(TEST, String.class);
        InMemoryDataSource<String> ds3 = new InMemoryDataSource<>("different", String.class);

        ds1.addItems(Arrays.asList(ITEM1, ITEM2));
        ds2.addItems(Arrays.asList(ITEM1, ITEM2));
        ds3.addItems(Arrays.asList(ITEM1, ITEM2));

        assertAll(
                () -> assertThat(ds1).isEqualTo(ds2).hasSameHashCodeAs(ds2),
                () -> assertThat(ds1).isNotEqualTo(ds3).isEqualTo(ds1).isNotEqualTo(null).isNotEqualTo("string")

        );
    }

    @Test
    @DisplayName("Should handle different entity types correctly")
    void shouldHandleDifferentEntityTypesCorrectly() {
        InMemoryDataSource<String> stringDs = new InMemoryDataSource<>("strings", String.class);
        InMemoryDataSource<Integer> intDs = new InMemoryDataSource<>("integers", Integer.class);

        stringDs.addItems(Arrays.asList("a", "b", "c"));
        intDs.addItems(Arrays.asList(1, 2, 3));

        assertThat(stringDs.getEntityType()).isEqualTo(String.class);
        assertThat(intDs.getEntityType()).isEqualTo(Integer.class);

        assertThat(stringDs.fetchAll().join()).containsExactly("a", "b", "c");
        assertThat(intDs.fetchAll().join()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Should handle complex entity types")
    void shouldHandleComplexEntityTypes() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<User> users = generator.generateUsers(100);

        InMemoryDataSource<User> userDs = new InMemoryDataSource<>(USERS, User.class, users);

        assertThat(userDs.getEntityType()).isEqualTo(User.class);
        assertThat(userDs.size()).isEqualTo(100);

        List<User> fetchedUsers = userDs.fetchAll().join();
        assertThat(fetchedUsers).hasSize(100);
        assertThat(fetchedUsers.get(0).getName()).isEqualTo("User_0");
        assertThat(fetchedUsers.get(0).getProfile()).isNotNull();
    }

    @Test
    @DisplayName("Should handle batch operations efficiently")
    void shouldHandleBatchOperationsEfficiently() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();

        InMemoryDataSource<Customer> customerDs = new InMemoryDataSource<>(CUSTOMERS, Customer.class);

        // Add in batches
        for (int i = 0; i < 10; i++) {
            List<Customer> batch = generator.generateCustomers(1000);
            // Adjust IDs to be unique across batches
            for (int j = 0; j < batch.size(); j++) {
                batch.get(j).setId((long) (i * 1000 + j));
            }
            customerDs.addItems(batch);
        }

        assertThat(customerDs.size()).isEqualTo(10_000);

        List<Customer> allCustomers = customerDs.fetchAll().join();
        assertThat(allCustomers).hasSize(10_000);
    }

    @Test
    @DisplayName("Should handle clear during concurrent operations")
    void shouldHandleClearDuringConcurrentOperations() throws InterruptedException {
        InMemoryDataSource<String> ds = new InMemoryDataSource<>(CONCURRENT, String.class);
        ds.addItems(Arrays.asList(ITEM1, ITEM2, ITEM3));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(3);

        // Reader thread
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    ds.fetchAll().join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        }).start();

        // Writer thread
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    ds.addItem("new-item-" + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        }).start();

        // Clear thread
        new Thread(() -> {
            try {
                startLatch.await();
                TestUtil.await(10);
                ds.clearData();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        endLatch.await();

        // After all operations, datasource should be in consistent state
        assertThat(ds.isHealthy()).isTrue();
        assertDoesNotThrow(() -> ds.fetchAll().join());
    }
}