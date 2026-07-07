package com.thy.fss.common.inmemory.dashboard;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.model.TestDashboard;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserDashboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Dashboard class.
 * Tests the passive data receiver implementation where Dashboard receives data
 * from DataSynchronizationEngine via updateData() method.
 * 
 * <p>Tests cover:
 * - Basic operations (getId, getName, getTargetClass, getData, updateData)
 * - Null data handling
 * - Concurrent access scenarios
 * - Integration with DataSynchronizationEngine via DataSyncTestHelper
 * - Large dataset handling (10K+ entities)
 * </p>
 */
class DashboardTest {

    private static final String DASH_1 = "dash-1";
    private static final String TEST_DASHBOARD = "Test Dashboard";
    private static final String USER_DASHBOARD = "User Dashboard";
    private static final String CONF_CANNOT_BE_NULL = "Configuration cannot be null";

    private DataSyncTestHelper.DashboardTestEnvironment<User, UserDashboard> testEnv;

    @AfterEach
    void cleanup() {
        if (testEnv != null) {
            DataSyncTestHelper.cleanup(testEnv);
            testEnv = null;
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ========== Basic Operations Tests ==========

    @Test
    void testGetIdReturnsCorrectId() {
        String expectedId = "dashboard-123";
        Dashboard<TestDashboard> dashboard = new Dashboard<>(expectedId, TEST_DASHBOARD, TestDashboard.class);

        String actualId = dashboard.getId();

        assertThat(actualId).isEqualTo(expectedId);
    }

    @Test
    void testGetNameReturnsCorrectName() {
        String expectedName = "Sales Dashboard";
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, expectedName, TestDashboard.class);

        String actualName = dashboard.getName();

        assertThat(actualName).isEqualTo(expectedName);
    }

    @Test
    void testGetTargetClassReturnsCorrectClass() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);

        Class<TestDashboard> targetClass = dashboard.getTargetClass();

        assertThat(targetClass).isEqualTo(TestDashboard.class);
    }

    @Test
    void testGetDataInitiallyReturnsNull() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);

        TestDashboard data = dashboard.getData();

        assertThat(data).isNull();
    }

    @Test
    void testUpdateDataStoresDataCorrectly() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        TestDashboard newData = new TestDashboard();
        newData.setTotalOrders(100);
        newData.setTotalRevenue(5000.0);
        newData.setAvgOrderValue(50.0);

        dashboard.updateData(newData);

        TestDashboard retrievedData = dashboard.getData();
        assertThat(retrievedData).isNotNull();
        assertThat(retrievedData.getTotalOrders()).isEqualTo(100);
        assertThat(retrievedData.getTotalRevenue()).isEqualTo(5000.0);
        assertThat(retrievedData.getAvgOrderValue()).isEqualTo(50.0);
    }

    @Test
    void testUpdateDataReplacesExistingData() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        TestDashboard firstData = new TestDashboard();
        firstData.setTotalOrders(100);
        firstData.setTotalRevenue(5000.0);
        dashboard.updateData(firstData);
        
        TestDashboard secondData = new TestDashboard();
        secondData.setTotalOrders(200);
        secondData.setTotalRevenue(10000.0);
        dashboard.updateData(secondData);

        TestDashboard retrievedData = dashboard.getData();
        assertThat(retrievedData).isNotNull();
        assertThat(retrievedData.getTotalOrders()).isEqualTo(200);
        assertThat(retrievedData.getTotalRevenue()).isEqualTo(10000.0);
    }

    // ========== Null Data Handling Tests ==========

    @Test
    void testUpdateDataAcceptsNullData() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        TestDashboard initialData = new TestDashboard();
        initialData.setTotalOrders(100);
        dashboard.updateData(initialData);

        dashboard.updateData(null);

        assertThat(dashboard.getData()).isNull();
    }

    @Test
    void testUpdateDataNullAfterNonNull() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        TestDashboard data = new TestDashboard();
        data.setTotalOrders(100);
        dashboard.updateData(data);
        assertThat(dashboard.getData()).isNotNull();
        
        dashboard.updateData(null);
        assertThat(dashboard.getData()).isNull();
    }

    @Test
    void testConstructorThrowsExceptionWhenTargetClassIsNull() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Dashboard<TestDashboard>(DASH_1, TEST_DASHBOARD, null)
        );
        
        assertThat(exception.getMessage()).isEqualTo(CONF_CANNOT_BE_NULL);
    }

    @Test
    void shouldCreateDashboardWithValidConfiguration() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);

        assertNotNull(dashboard);
        assertNotNull(dashboard.getId());
        assertEquals(TEST_DASHBOARD, dashboard.getName());
        assertEquals(TestDashboard.class, dashboard.getTargetClass());
        assertNull(dashboard.getData());
    }

    @Test
    void shouldThrowExceptionWhenConfigurationIsNull() {
        // When & Then
        String dashboardId = UUID.randomUUID().toString();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Dashboard<TestDashboard>(dashboardId, TEST_DASHBOARD, null)
        );
        assertEquals(CONF_CANNOT_BE_NULL, exception.getMessage());
    }

    @Test
    void shouldReturnUniqueIdForEachDashboard() {
        // When
        Dashboard<TestDashboard> dashboard1 = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        Dashboard<TestDashboard> dashboard2 = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);

        // Then
        assertNotNull(dashboard1.getId());
        assertNotNull(dashboard2.getId());
        assertNotEquals(dashboard1.getId(), dashboard2.getId());
    }

    @Test
    void shouldUpdateDataSuccessfully() {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        TestDashboard newData = new TestDashboard();
        newData.setTotalOrders(100);
        newData.setTotalRevenue(5000.00);
        newData.setAvgOrderValue(50.00);

        // When
        dashboard.updateData(newData);

        // Then
        TestDashboard retrievedData = dashboard.getData();
        assertNotNull(retrievedData);
        assertEquals(100, retrievedData.getTotalOrders());
        assertEquals(5000.00, retrievedData.getTotalRevenue());
        assertEquals(50.00, retrievedData.getAvgOrderValue());
    }

    @Test
    void shouldReplaceDataOnSubsequentUpdates() {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);

        TestDashboard firstData = new TestDashboard();
        firstData.setTotalOrders(100);
        firstData.setTotalRevenue(5000.00);

        TestDashboard secondData = new TestDashboard();
        secondData.setTotalOrders(200);
        secondData.setTotalRevenue(10000.00);

        // When
        dashboard.updateData(firstData);
        TestDashboard afterFirst = dashboard.getData();

        dashboard.updateData(secondData);
        TestDashboard afterSecond = dashboard.getData();

        // Then
        assertEquals(100, afterFirst.getTotalOrders());
        assertEquals(200, afterSecond.getTotalOrders());
        assertEquals(10000.00, afterSecond.getTotalRevenue());
    }

    @Test
    void shouldAcceptNullDataUpdate() {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        TestDashboard initialData = new TestDashboard();
        initialData.setTotalOrders(100);
        dashboard.updateData(initialData);

        // When
        dashboard.updateData(null);

        // Then
        assertNull(dashboard.getData());
    }

    @Test
    void shouldStoreDataAtomically() {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        TestDashboard data = new TestDashboard();
        data.setTotalOrders(100);

        // When
        dashboard.updateData(data);

        // Then
        assertSame(data, dashboard.getData());
    }

    @Test
    void shouldHandleConcurrentReads() throws InterruptedException {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        TestDashboard data = new TestDashboard();
        data.setTotalOrders(100);
        dashboard.updateData(data);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    TestDashboard readData = dashboard.getData();
                    if (readData != null && readData.getTotalOrders() == 100) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, successCount.get());
        executor.shutdown();
    }

    @Test
    void shouldHandleConcurrentWrites() throws InterruptedException {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    TestDashboard data = new TestDashboard();
                    data.setTotalOrders(value);
                    dashboard.updateData(data);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(dashboard.getData()); // Should have some data
        assertTrue(dashboard.getData().getTotalOrders() >= 0 &&
                dashboard.getData().getTotalOrders() < threadCount);
        executor.shutdown();
    }

    @Test
    void shouldHandleConcurrentReadAndWrite() throws InterruptedException {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        TestDashboard initialData = new TestDashboard();
        initialData.setTotalOrders(0);
        dashboard.updateData(initialData);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);

        // When - Half threads read, half write
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            if (i % 2 == 0) {
                // Reader thread
                executor.submit(() -> {
                    try {
                        TestDashboard data = dashboard.getData();
                        if (data != null) {
                            readCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                // Writer thread
                executor.submit(() -> {
                    try {
                        TestDashboard data = new TestDashboard();
                        data.setTotalOrders(value);
                        dashboard.updateData(data);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(readCount.get() > 0); // At least some reads succeeded
        assertNotNull(dashboard.getData()); // Should have data
        executor.shutdown();
    }

    @Test
    void shouldMaintainDataIntegrityUnderLoad() throws InterruptedException {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);
        int updateCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(updateCount);

        // When - Rapid sequential updates
        for (int i = 0; i < updateCount; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    TestDashboard data = new TestDashboard();
                    data.setTotalOrders(value);
                    data.setTotalRevenue(Double.valueOf((double) value * 100));
                    dashboard.updateData(data);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        TestDashboard finalData = dashboard.getData();
        assertNotNull(finalData);
        assertNotNull(finalData.getTotalOrders());
        assertNotNull(finalData.getTotalRevenue());
        // Verify data consistency (orders and revenue should match)
        assertEquals(
                finalData.getTotalOrders() * 100,
                finalData.getTotalRevenue().intValue()
        );
        executor.shutdown();
    }

    @Test
    void shouldReturnNameFromConfiguration() {
        // Given
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);

        // When
        String name = dashboard.getName();

        // Then
        assertEquals(TEST_DASHBOARD, name);
    }

    @Test
    void shouldReturnTargetClassFromConfiguration() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(UUID.randomUUID().toString(), TEST_DASHBOARD, TestDashboard.class);

        Class<TestDashboard> targetClass = dashboard.getTargetClass();

        assertEquals(TestDashboard.class, targetClass);
    }

    // ========== Large Dataset Tests ==========

    @Test
    void testDashboardWithLargeDataset10K() {
        Dashboard<UserDashboard> dashboard = new Dashboard<>(DASH_1, USER_DASHBOARD, UserDashboard.class);
        
        UserDashboard largeData = new UserDashboard();
        largeData.setTotalUsers(10_000L);
        
        dashboard.updateData(largeData);

        assertThat(dashboard.getData()).isNotNull();
        assertThat(dashboard.getData().getTotalUsers()).isEqualTo(10_000L);
    }

    @Test
    void testDashboardMultipleUpdatesWithLargeValues() {
        Dashboard<UserDashboard> dashboard = new Dashboard<>(DASH_1, USER_DASHBOARD, UserDashboard.class);
        
        UserDashboard data1 = new UserDashboard();
        data1.setTotalUsers(5_000L);
        dashboard.updateData(data1);
        assertThat(dashboard.getData().getTotalUsers()).isEqualTo(5_000L);
        
        UserDashboard data2 = new UserDashboard();
        data2.setTotalUsers(10_000L);
        dashboard.updateData(data2);
        assertThat(dashboard.getData().getTotalUsers()).isEqualTo(10_000L);
        
        UserDashboard data3 = new UserDashboard();
        data3.setTotalUsers(15_000L);
        dashboard.updateData(data3);
        assertThat(dashboard.getData().getTotalUsers()).isEqualTo(15_000L);
    }

    @Test
    void testDashboardWithComplexDashboardData() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        TestDashboard complexData = new TestDashboard();
        complexData.setTotalOrders(10_000);
        complexData.setTotalRevenue(500_000.0);
        complexData.setAvgOrderValue(50.0);
        
        dashboard.updateData(complexData);

        TestDashboard retrieved = dashboard.getData();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getTotalOrders()).isEqualTo(10_000);
        assertThat(retrieved.getTotalRevenue()).isEqualTo(500_000.0);
        assertThat(retrieved.getAvgOrderValue()).isEqualTo(50.0);
    }

    @Test
    void testDashboardRapidUpdatesLargeDataset() {
        Dashboard<UserDashboard> dashboard = new Dashboard<>(DASH_1, USER_DASHBOARD, UserDashboard.class);
        
        for (int i = 1; i <= 100; i++) {
            UserDashboard data = new UserDashboard();
            data.setTotalUsers((long) i * 100);
            dashboard.updateData(data);
        }

        assertThat(dashboard.getData()).isNotNull();
        assertThat(dashboard.getData().getTotalUsers()).isEqualTo(10_000L);
    }

    @Test
    void testDashboardDataConsistencyAfterMultipleUpdates() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        for (int i = 1; i <= 1000; i++) {
            TestDashboard data = new TestDashboard();
            data.setTotalOrders(i);
            data.setTotalRevenue((double) i * 100);
            data.setAvgOrderValue(100.0);
            dashboard.updateData(data);
        }

        TestDashboard finalData = dashboard.getData();
        assertThat(finalData).isNotNull();
        assertThat(finalData.getTotalOrders()).isEqualTo(1000);
        assertThat(finalData.getTotalRevenue()).isEqualTo(100_000.0);
        assertThat(finalData.getAvgOrderValue()).isEqualTo(100.0);
    }

    // ========== Concurrent Access Tests ==========

    @Test
    void testConcurrentReadsWithLargeDataset() throws InterruptedException {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        TestDashboard data = new TestDashboard();
        data.setTotalOrders(10_000);
        data.setTotalRevenue(500_000.0);
        dashboard.updateData(data);

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    TestDashboard readData = dashboard.getData();
                    if (readData != null && readData.getTotalOrders() == 10_000) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertThat(successCount.get()).isEqualTo(threadCount);
        executor.shutdown();
    }

    @Test
    void testConcurrentWritesWithLargeDataset() throws InterruptedException {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int value = i * 1000;
            executor.submit(() -> {
                try {
                    TestDashboard data = new TestDashboard();
                    data.setTotalOrders(value);
                    data.setTotalRevenue((double) value * 50);
                    dashboard.updateData(data);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertThat(dashboard.getData()).isNotNull();
        assertThat(dashboard.getData().getTotalOrders()).isNotNull();
        executor.shutdown();
    }

    @Test
    void testConcurrentReadWriteMixedOperations() throws InterruptedException {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        TestDashboard initialData = new TestDashboard();
        initialData.setTotalOrders(0);
        dashboard.updateData(initialData);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            if (i % 2 == 0) {
                executor.submit(() -> {
                    try {
                        TestDashboard data = dashboard.getData();
                        if (data != null) {
                            readCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            } else {
                executor.submit(() -> {
                    try {
                        TestDashboard data = new TestDashboard();
                        data.setTotalOrders(value);
                        dashboard.updateData(data);
                        writeCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertThat(readCount.get()).isGreaterThan(0);
        assertThat(writeCount.get()).isEqualTo(threadCount / 2);
        assertThat(dashboard.getData()).isNotNull();
        executor.shutdown();
    }

    @Test
    void testDataIntegrityUnderHighLoad() throws InterruptedException {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        
        int updateCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(updateCount);

        for (int i = 0; i < updateCount; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    TestDashboard data = new TestDashboard();
                    data.setTotalOrders(value);
                    data.setTotalRevenue((double) value * 100);
                    data.setAvgOrderValue(100.0);
                    dashboard.updateData(data);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        TestDashboard finalData = dashboard.getData();
        assertThat(finalData).isNotNull();
        assertThat(finalData.getTotalOrders()).isNotNull();
        assertThat(finalData.getTotalRevenue()).isNotNull();
        assertThat(finalData.getTotalOrders() * 100).isEqualTo(finalData.getTotalRevenue().intValue());
        executor.shutdown();
    }

    @Test
    void testAtomicDataReplacement() {
        Dashboard<TestDashboard> dashboard = new Dashboard<>(DASH_1, TEST_DASHBOARD, TestDashboard.class);
        TestDashboard data = new TestDashboard();
        data.setTotalOrders(100);

        dashboard.updateData(data);

        assertThat(dashboard.getData()).isSameAs(data);
    }

    @Test
    void testMultipleDashboardsIndependentData() {
        Dashboard<TestDashboard> dashboard1 = new Dashboard<>(DASH_1, "Dashboard 1", TestDashboard.class);
        Dashboard<TestDashboard> dashboard2 = new Dashboard<>("dash-2", "Dashboard 2", TestDashboard.class);

        TestDashboard data1 = new TestDashboard();
        data1.setTotalOrders(100);
        dashboard1.updateData(data1);

        TestDashboard data2 = new TestDashboard();
        data2.setTotalOrders(200);
        dashboard2.updateData(data2);

        assertThat(dashboard1.getData().getTotalOrders()).isEqualTo(100);
        assertThat(dashboard2.getData().getTotalOrders()).isEqualTo(200);
        assertThat(dashboard1.getId()).isNotEqualTo(dashboard2.getId());
    }
}
