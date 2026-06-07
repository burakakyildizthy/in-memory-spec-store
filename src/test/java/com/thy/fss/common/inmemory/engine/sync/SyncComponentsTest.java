package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import com.thy.fss.common.inmemory.testmodel.User;

/**
 * Comprehensive test coverage for engine sync components.
 * Tests DataVersion and DataSourceSyncMetadata with all public methods and scenarios.
 */
class SyncComponentsTest {

    private static final String STORE1 = "store1";
    private static final String STORE2 = "store2";
    private static final String DASHBOARD1 = "dashboard1";
    private static final String USER_DATASOURCE = "userDataSource";
    private static final String ORDER_DATASOURCE = "orderDataSource";
    private static final String CONNECTION_TIMEOUT = "Connection timeout";
    private static final String ERROR_1 = "Error 1";
    private static final String ERROR_2 = "Error 2";
    private static final String ERROR_3 = "Error 3";
    private static final String GROUP_KEY1 = "groupingKey1";
    private static final String AGGREGATION_KEY1 = "aggKey1";
    private static final String AGGREGATION_KEY2 = "aggKey2";
    private static final String NONEXISTENT = "nonExistent";
    private static final String MARK_UNHEALTHY = "Manual unhealthy mark";
    private static final String STREAM_DS = "streamDs";

    // ========== DataVersion Tests ==========

    @Test
    void testDataVersionCreation() {
        LocalDateTime timestamp = LocalDateTime.now();
        DataVersion version = new DataVersion(1L, timestamp);

        assertThat(version.getVersion()).isEqualTo(1L);
        assertThat(version.getTimestamp()).isEqualTo(timestamp);
        assertThat(version.getAllPopulatedEntities()).isEmpty();
        assertThat(version.getDataSourceNames()).isEmpty();
        assertThat(version.getPopulatedEntityConsumerIds()).isEmpty();
    }

    @Test
    void testDataVersionSetAndGetPopulatedEntities() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> users = createTestUsers(100);

        version.setPopulatedEntities(STORE1, users);

        assertThat(version.getPopulatedEntities(STORE1)).isEqualTo(users);
        assertThat(version.hasPopulatedEntities(STORE1)).isTrue();
        assertThat(version.hasPopulatedEntities(STORE2)).isFalse();
    }

    @Test
    void testDataVersionSetAndGetStoreData() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> users = createTestUsers(100);

        version.setStoreData(STORE1, users);

        assertThat(version.getStoreData(STORE1)).isEqualTo(users);
        assertThat(version.getPopulatedEntities(STORE1)).isEqualTo(users);
    }

    @Test
    void testDataVersionSetAndGetDashboardData() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> users = createTestUsers(100);

        version.setDashboardData(DASHBOARD1, users);

        assertThat(version.getDashboardData(DASHBOARD1)).isEqualTo(users);
        assertThat(version.getPopulatedEntities(DASHBOARD1)).isEqualTo(users);
    }

    @Test
    void testDataVersionGetAllPopulatedEntities() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> users = createTestUsers(100);
        List<Order> orders = createTestOrders(200);

        version.setPopulatedEntities(STORE1, users);
        version.setPopulatedEntities(DASHBOARD1, orders);

        Map<String, List<?>> allEntities = version.getAllPopulatedEntities();

        assertThat(allEntities).hasSize(2).containsKeys(STORE1, DASHBOARD1)
                        .containsEntry(STORE1, users).containsEntry(DASHBOARD1, orders);
    }

    @Test
    void testDataVersionGetAllPopulatedEntitiesUnmodifiable() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setPopulatedEntities(STORE1, createTestUsers(100));

        Map<String, List<?>> allEntities = version.getAllPopulatedEntities();

        // Prepare parameters outside the assertion
        String key = STORE2;
        List<User> value = createTestUsers(50);

        // Only call the method expected to throw
        assertThatThrownBy(() -> allEntities.put(key, value))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testDataVersionGetPopulatedEntityConsumerIds() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setPopulatedEntities(STORE1, createTestUsers(100));
        version.setPopulatedEntities(STORE2, createTestUsers(50));
        version.setPopulatedEntities(DASHBOARD1, createTestOrders(200));

        Set<String> consumerIds = version.getPopulatedEntityConsumerIds();

        assertThat(consumerIds).containsExactlyInAnyOrder(STORE1, STORE2, DASHBOARD1);
    }

    @Test
    void testDataVersionGetPopulatedEntityConsumerIdsUnmodifiable() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setPopulatedEntities(STORE1, createTestUsers(100));

        Set<String> consumerIds = version.getPopulatedEntityConsumerIds();

        assertThatThrownBy(() -> consumerIds.add(STORE2))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testDataVersionSetAndGetDataByDataSource() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> users = createTestUsers(100);

        version.setDataByDataSource(USER_DATASOURCE, users);

        assertThat(version.getDataByDataSource(USER_DATASOURCE)).isEqualTo(users);
        assertThat(version.hasDataSource(USER_DATASOURCE)).isTrue();
        assertThat(version.hasDataSource(ORDER_DATASOURCE)).isFalse();
    }

    @Test
    void testDataVersionGetDataSourceNames() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setDataByDataSource(USER_DATASOURCE, createTestUsers(100));
        version.setDataByDataSource(ORDER_DATASOURCE, createTestOrders(200));

        Set<String> datasourceNames = version.getDataSourceNames();

        assertThat(datasourceNames).containsExactlyInAnyOrder(USER_DATASOURCE, ORDER_DATASOURCE);
    }

    @Test
    void testDataVersionGetDataSourceNamesUnmodifiable() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setDataByDataSource(USER_DATASOURCE, createTestUsers(100));

        Set<String> datasourceNames = version.getDataSourceNames();

        assertThatThrownBy(() -> datasourceNames.add(ORDER_DATASOURCE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testDataVersionSetAndGetGroupedData() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        Map<Object, List<?>> groupedData = new HashMap<>();
        groupedData.put("key1", createTestOrders(50));
        groupedData.put("key2", createTestOrders(30));

        version.setGroupedData(GROUP_KEY1, groupedData);

        Map<Object, List<Order>> retrieved = version.getGroupedData(GROUP_KEY1);

        assertThat(retrieved).hasSize(2).containsKeys("key1", "key2");
    }

    @Test
    void testDataVersionGetGroupedDataNonExistent() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());

        Map<Object, List<Order>> retrieved = version.getGroupedData(NONEXISTENT);

        assertThat(retrieved).isNull();
    }

    @Test
    void testDataVersionSetAndGetCommonAggregationResult() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());

        version.setCommonAggregationResult(AGGREGATION_KEY1, 12345.67);
        version.setCommonAggregationResult(AGGREGATION_KEY2, 100L);

        assertThat(version.<Double>getCommonAggregationResult(AGGREGATION_KEY1)).isEqualTo(12345.67);
        assertThat(version.<Long>getCommonAggregationResult(AGGREGATION_KEY2)).isEqualTo(100L);
    }

    @Test
    void testDataVersionGetCommonAggregationResultNonExistent() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());

        Object result = version.getCommonAggregationResult(NONEXISTENT);

        assertThat(result).isNull();
    }

    @Test
    void testDataVersionMakeImmutable() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        version.setPopulatedEntities(STORE1, createTestUsers(100));

        DataVersion immutable = version.makeImmutable();

        assertThat(immutable).isSameAs(version);
    }

    @Test
    void testDataVersionToString() {
        LocalDateTime timestamp = LocalDateTime.now();
        DataVersion version = new DataVersion(1L, timestamp);
        version.setPopulatedEntities(STORE1, createTestUsers(100));
        version.setDataByDataSource(USER_DATASOURCE, createTestUsers(100));

        String toString = version.toString();

        assertThat(toString).contains("DataVersion").contains("version=1").contains("consumers=1").contains("datasources=1");
    }

    @Test
    void testDataVersionWithLargeDataset() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());
        List<User> largeUserList = createTestUsers(10000);
        List<Order> largeOrderList = createTestOrders(50000);

        version.setPopulatedEntities(STORE1, largeUserList);
        version.setPopulatedEntities(STORE2, largeOrderList);
        version.setDataByDataSource(USER_DATASOURCE, largeUserList);
        version.setDataByDataSource(ORDER_DATASOURCE, largeOrderList);

        assertThat(version.getPopulatedEntities(STORE1)).hasSize(10000);
        assertThat(version.getPopulatedEntities(STORE2)).hasSize(50000);
        assertThat(version.getDataByDataSource(USER_DATASOURCE)).hasSize(10000);
        assertThat(version.getDataByDataSource(ORDER_DATASOURCE)).hasSize(50000);
    }

    @Test
    void testDataVersionMultipleConsumers() {
        DataVersion version = new DataVersion(1L, LocalDateTime.now());

        for (int i = 0; i < 100; i++) {
            version.setPopulatedEntities("store" + i, createTestUsers(100));
        }

        assertThat(version.getPopulatedEntityConsumerIds()).hasSize(100);
        assertThat(version.getAllPopulatedEntities()).hasSize(100);
    }

    // ========== DataSourceSyncMetadata Tests ==========

    @Test
    void testDataSourceSyncMetadataCreationWithDefaultTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, syncInterval);

        assertThat(metadata.getDataSourceName()).isEqualTo(USER_DATASOURCE);
        assertThat(metadata.getSyncInterval()).isEqualTo(syncInterval);
        assertThat(metadata.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getConsecutiveFailures()).isZero();
        assertThat(metadata.getLastSyncTime()).isNull();
        assertThat(metadata.getNextSyncTime()).isNotNull();
    }

    @Test
    void testDataSourceSyncMetadataCreationWithCustomTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);
        Duration readTimeout = Duration.ofSeconds(60);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, syncInterval, readTimeout);

        assertThat(metadata.getReadTimeout()).isEqualTo(readTimeout);
    }

    @Test
    void testDataSourceSyncMetadataCreationWithNullTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, syncInterval, null);

        assertThat(metadata.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void testDataSourceSyncMetadataShouldSyncInitially() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMillis(10));

        TestUtil.await(20);
        assertThat(metadata.shouldSync()).isTrue();
    }

    @Test
    void testDataSourceSyncMetadataRecordSuccess()  {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMillis(100));

        metadata.recordSuccess();

        assertThat(metadata.getLastSyncTime()).isNotNull();
        assertThat(metadata.getNextSyncTime()).isAfter(LocalDateTime.now());
        assertThat(metadata.getConsecutiveFailures()).isZero();
        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getLastErrorMessage()).isNull();

        TestUtil.await(150);
        assertThat(metadata.shouldSync()).isTrue();
    }

    @Test
    void testDataSourceSyncMetadataRecordFailureSingle() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.recordFailure(CONNECTION_TIMEOUT);

        assertThat(metadata.getConsecutiveFailures()).isEqualTo(1);
        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getLastErrorMessage()).isEqualTo(CONNECTION_TIMEOUT);
    }

    @Test
    void testDataSourceSyncMetadataRecordFailureMultipleBecomesUnhealthy() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.recordFailure(ERROR_1);
        assertThat(metadata.isHealthy()).isTrue();

        metadata.recordFailure(ERROR_2);
        assertThat(metadata.isHealthy()).isTrue();

        metadata.recordFailure(ERROR_3);
        assertThat(metadata.isHealthy()).isFalse();
        assertThat(metadata.getConsecutiveFailures()).isEqualTo(3);
        assertThat(metadata.getLastErrorMessage()).isEqualTo(ERROR_3);
    }

    @Test
    void testDataSourceSyncMetadataRecordSuccessResetsFailures() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.recordFailure(ERROR_1);
        metadata.recordFailure(ERROR_2);
        assertThat(metadata.getConsecutiveFailures()).isEqualTo(2);

        metadata.recordSuccess();

        assertThat(metadata.getConsecutiveFailures()).isZero();
        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getLastErrorMessage()).isNull();
    }

    @Test
    void testDataSourceSyncMetadataMarkHealthy() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.recordFailure(ERROR_1);
        metadata.recordFailure(ERROR_2);
        metadata.recordFailure(ERROR_3);
        assertThat(metadata.isHealthy()).isFalse();

        metadata.markHealthy();

        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getConsecutiveFailures()).isZero();
        assertThat(metadata.getLastErrorMessage()).isNull();
    }

    @Test
    void testDataSourceSyncMetadataMarkUnhealthy() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.markUnhealthy(MARK_UNHEALTHY);

        assertThat(metadata.isHealthy()).isFalse();
        assertThat(metadata.getLastErrorMessage()).isEqualTo(MARK_UNHEALTHY);
    }

    @Test
    void testDataSourceSyncMetadataShouldRetryHealthCheckWhenHealthy() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        assertThat(metadata.shouldRetryHealthCheck()).isFalse();
    }

    @Test
    void testDataSourceSyncMetadataShouldRetryHealthCheckWhenUnhealthyAfterFailures() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));
        
        metadata.recordFailure(ERROR_1);
        metadata.recordFailure(ERROR_2);
        metadata.recordFailure(ERROR_3);
        
        assertThat(metadata.isHealthy()).isFalse();
        assertThat(metadata.shouldRetryHealthCheck()).isFalse();
    }

    @Test
    void testDataSourceSyncMetadataUpdateNextSyncTime() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));
        LocalDateTime newNextSync = LocalDateTime.now().plusHours(1);

        metadata.updateNextSyncTime(newNextSync);

        assertThat(metadata.getNextSyncTime()).isEqualTo(newNextSync);
    }

    @Test
    void testDataSourceSyncMetadataToString() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));
        metadata.recordSuccess();

        String toString = metadata.toString();

        assertThat(toString).contains("DataSourceSyncMetadata").contains(USER_DATASOURCE).contains("healthy=true").contains("failures=0");
    }

    @Test
    void testDataSourceSyncMetadataFailureBackoff() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMillis(100));

        metadata.recordFailure(ERROR_1);
        metadata.recordFailure(ERROR_2);
        metadata.recordFailure(ERROR_3);

        assertThat(metadata.isHealthy()).isFalse();
        assertThat(metadata.getNextSyncTime()).isNotNull();

        TestUtil.await(50);
        assertThat(metadata.shouldSync()).isFalse();
    }

    @Test
    void testDataSourceSyncMetadataMultipleSuccessfulSyncs() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMillis(50));

        for (int i = 0; i < 10; i++) {
            metadata.recordSuccess();
            TestUtil.await(60);
            assertThat(metadata.shouldSync()).isTrue();
        }

        assertThat(metadata.getConsecutiveFailures()).isZero();
        assertThat(metadata.isHealthy()).isTrue();
    }

    @Test
    void testDataSourceSyncMetadataAlternatingSuccessAndFailure() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        metadata.recordSuccess();
        assertThat(metadata.getConsecutiveFailures()).isZero();

        metadata.recordFailure(ERROR_1);
        assertThat(metadata.getConsecutiveFailures()).isEqualTo(1);

        metadata.recordSuccess();
        assertThat(metadata.getConsecutiveFailures()).isZero();

        metadata.recordFailure(ERROR_2);
        assertThat(metadata.getConsecutiveFailures()).isEqualTo(1);
    }

    @Test
    void testDataSourceSyncMetadataWithLargeInterval() {
        Duration largeInterval = Duration.ofHours(24);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, largeInterval);

        metadata.recordSuccess();

        assertThat(metadata.getNextSyncTime()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    void testDataSourceSyncMetadataWithSmallInterval() {
        Duration smallInterval = Duration.ofMillis(10);
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, smallInterval);

        metadata.recordSuccess();
        TestUtil.await(20);

        assertThat(metadata.shouldSync()).isTrue();
    }

    // ========== Streaming DataSourceSyncMetadata Tests ==========

    @Test
    void testStreamingMetadataDefaultsForBatchDatasource() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));

        assertThat(metadata.isStreamingDataSource()).isFalse();
        assertThat(metadata.getStreamingState()).isNull();
        assertThat(metadata.getReconnectAttempts()).isZero();
    }

    @Test
    void testStreamingMetadataSetAndGetStreamingState() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.INITIALIZING);

        assertThat(metadata.isStreamingDataSource()).isTrue();
        assertThat(metadata.getStreamingState())
                .isEqualTo(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.INITIALIZING);
    }

    @Test
    void testStreamingMetadataUpdateStreamingStateToReady() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.INITIALIZING);

        metadata.updateStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.READY);

        assertThat(metadata.getStreamingState())
                .isEqualTo(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.READY);
        assertThat(metadata.isHealthy()).isTrue();
        assertThat(metadata.getLastErrorMessage()).isNull();
    }

    @Test
    void testStreamingMetadataUpdateStreamingStateToError() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.READY);

        metadata.updateStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.ERROR);

        assertThat(metadata.getStreamingState())
                .isEqualTo(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.ERROR);
        assertThat(metadata.isHealthy()).isFalse();
    }

    @Test
    void testStreamingMetadataUpdateStreamingStateToInitializing() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.ERROR);

        metadata.updateStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.INITIALIZING);

        assertThat(metadata.getStreamingState())
                .isEqualTo(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.INITIALIZING);
    }

    @Test
    void testStreamingMetadataReconnectAttempts() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);

        assertThat(metadata.getReconnectAttempts()).isZero();

        metadata.incrementReconnectAttempts();
        assertThat(metadata.getReconnectAttempts()).isEqualTo(1);

        metadata.incrementReconnectAttempts();
        metadata.incrementReconnectAttempts();
        assertThat(metadata.getReconnectAttempts()).isEqualTo(3);

        metadata.resetReconnectAttempts();
        assertThat(metadata.getReconnectAttempts()).isZero();
    }

    @Test
    void testStreamingMetadataSetReconnectAttempts() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setReconnectAttempts(5);

        assertThat(metadata.getReconnectAttempts()).isEqualTo(5);
    }

    @Test
    void testStreamingMetadataToStringForStreamingDatasource() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(STREAM_DS, Duration.ZERO);
        metadata.setStreamingDataSource(true);
        metadata.setStreamingState(com.thy.fss.common.inmemory.datasource.StreamingDataSourceState.READY);

        String toString = metadata.toString();

        assertThat(toString).contains("streaming=true")
                .contains("streamingState=READY")
                .contains(STREAM_DS);
    }

    @Test
    void testStreamingMetadataToStringForBatchDatasource() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata(USER_DATASOURCE, Duration.ofMinutes(5));
        metadata.recordSuccess();

        String toString = metadata.toString();

        // Batch datasource should NOT contain streaming fields
        assertThat(toString).doesNotContain("streaming=true").contains("healthy=true");
    }

    // ========== Helper Methods ==========

    private List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setId("user" + i);
            user.setName("User " + i);
            users.add(user);
        }
        return users;
    }

    private List<Order> createTestOrders(int count) {
        List<Order> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCustomerId((long) (i % 100));
            order.setTotalAmount(100.0 + i);
            order.setStatus("ACTIVE");
            orders.add(order);
        }
        return orders;
    }
}
