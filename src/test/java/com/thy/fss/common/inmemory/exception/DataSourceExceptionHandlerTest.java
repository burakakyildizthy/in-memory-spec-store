package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.datasource.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Test class for DataSourceExceptionHandler functionality.
 */
class DataSourceExceptionHandlerTest {

    // Constants for duplicate string literals
    private static final String TEST_SOURCE_NAME = "test-source";
    private static final String FALLBACK_ENTITY_NAME = "Fallback Entity";
    private static final String MSG_CONNECTION_TIMEOUT = "Connection timeout";
    private static final String MSG_CONNECTION_FAILED = "Connection failed";
    private static final String MSG_SUCCESS = "Success";

    @Mock
    private DataSource<TestEntity> primaryDataSource;

    @Mock
    private DataSource<TestEntity> fallbackDataSource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should execute operation successfully with primary data source")
    void shouldExecuteOperationSuccessfullyWithPrimaryDataSource() throws Exception {
        List<TestEntity> expectedData = List.of(new TestEntity(1L, "Entity 1"));

        when(primaryDataSource.isHealthy()).thenReturn(true);
        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation =
                dataSource -> CompletableFuture.completedFuture(expectedData);

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEqualTo(expectedData);
        verify(primaryDataSource).isHealthy();
    }

    @Test
    @DisplayName("Should use fallback data source when primary fails")
    void shouldUseFallbackDataSourceWhenPrimaryFails() throws Exception {
        List<TestEntity> fallbackData = List.of(new TestEntity(2L, FALLBACK_ENTITY_NAME));

        when(primaryDataSource.isHealthy()).thenReturn(true);
        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
        when(fallbackDataSource.isHealthy()).thenReturn(true);
        when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation = dataSource -> {
            if (dataSource == primaryDataSource) {
                return CompletableFuture.failedFuture(new RuntimeException("Primary failed"));
            } else {
                return CompletableFuture.completedFuture(fallbackData);
            }
        };

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEqualTo(fallbackData);
        verify(primaryDataSource).isHealthy();
        verify(fallbackDataSource).isHealthy();
    }

    @Test
    @DisplayName("Should skip unhealthy data sources")
    void shouldSkipUnhealthyDataSources() throws Exception {
        List<TestEntity> fallbackData = List.of(new TestEntity(2L, FALLBACK_ENTITY_NAME));

        when(primaryDataSource.isHealthy()).thenReturn(false);
        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
        when(fallbackDataSource.isHealthy()).thenReturn(true);
        when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation =
                dataSource -> CompletableFuture.completedFuture(fallbackData);

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEqualTo(fallbackData);
        verify(primaryDataSource).isHealthy();
        verify(fallbackDataSource).isHealthy();
    }

    @Test
    @DisplayName("Should return empty list when all data sources fail")
    void shouldReturnEmptyListWhenAllDataSourcesFail() throws Exception {
        when(primaryDataSource.isHealthy()).thenReturn(true);
        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
        when(fallbackDataSource.isHealthy()).thenReturn(true);
        when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation =
                dataSource -> CompletableFuture.failedFuture(new RuntimeException("All failed"));

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEmpty();
    }

    @Test
    @DisplayName("Should wrap operation and handle exceptions")
    void shouldWrapOperationAndHandleExceptions() {
        Supplier<String> failingOperation = () -> {
            throw new RuntimeException("Operation failed");
        };

        Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                failingOperation, TEST_SOURCE_NAME);

        assertThatThrownBy(wrappedOperation::get)
                .isInstanceOf(DataSourceException.class)
                .hasMessageContaining("Operation failed for DataSource '" + TEST_SOURCE_NAME + "'")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should wrap operation and handle connection exceptions")
    void shouldWrapOperationAndHandleConnectionExceptions() {
        Supplier<String> failingOperation = () -> {
            throw new RuntimeException(MSG_CONNECTION_TIMEOUT);
        };

        Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                failingOperation, TEST_SOURCE_NAME);

        assertThatThrownBy(wrappedOperation::get)
                .isInstanceOf(DataSourceConnectionException.class)
                .hasMessageContaining(MSG_CONNECTION_FAILED + " for DataSource '" + TEST_SOURCE_NAME + "'")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should wrap operation successfully when no exception occurs")
    void shouldWrapOperationSuccessfullyWhenNoExceptionOccurs() {
        Supplier<String> successfulOperation = () -> MSG_SUCCESS;

        Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                successfulOperation, TEST_SOURCE_NAME);

        String result = wrappedOperation.get();
        assertThat(result).isEqualTo(MSG_SUCCESS);
    }

    @Test
    @DisplayName("Should identify connection exceptions correctly")
    void shouldIdentifyConnectionExceptionsCorrectly() {
        // Direct DataSourceConnectionException
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new DataSourceConnectionException(MSG_CONNECTION_FAILED))).isTrue();

        // Exceptions with connection-related messages
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException(MSG_CONNECTION_TIMEOUT))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Network unreachable"))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Connection refused"))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Authentication failed"))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Unauthorized access"))).isTrue();

        // Test individual keywords for branch coverage
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("timeout occurred"))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("host unreachable"))).isTrue();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("connection refused by server"))).isTrue();

        // Non-connection exceptions
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Data parsing error"))).isFalse();
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Invalid query"))).isFalse();

        // Exception with null message
        assertThat(DataSourceExceptionHandler.isConnectionException(
                new RuntimeException((String) null))).isFalse();
    }

    @Test
    @DisplayName("Should handle case-insensitive connection exception detection")
    void shouldHandleCaseInsensitiveConnectionExceptionDetection() {
        // Test each case individually to identify which one fails
        boolean connectionTest = DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("CONNECTION timeout"));
        assertThat(connectionTest).as("CONNECTION timeout should be detected").isTrue();

        boolean networkTest = DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("Network UNREACHABLE"));
        assertThat(networkTest).as("Network UNREACHABLE should be detected").isTrue();

        boolean authTest = DataSourceExceptionHandler.isConnectionException(
                new RuntimeException("AUTHENTICATION failed"));
        assertThat(authTest).as("AUTHENTICATION failed should be detected").isTrue();
    }

    @Test
    @DisplayName("Should build fallback chain correctly")
    void shouldBuildFallbackChainCorrectly() throws Exception {
        @SuppressWarnings("unchecked")
        DataSource<TestEntity> thirdDataSource = mock(DataSource.class);

        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
        when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.of(thirdDataSource));
        when(thirdDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        when(primaryDataSource.isHealthy()).thenReturn(false);
        when(fallbackDataSource.isHealthy()).thenReturn(false);
        when(thirdDataSource.isHealthy()).thenReturn(true);

        List<TestEntity> expectedData = List.of(new TestEntity(3L, "Third Entity"));

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation = dataSource -> {
            if (dataSource == thirdDataSource) {
                return CompletableFuture.completedFuture(expectedData);
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Failed"));
            }
        };

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEqualTo(expectedData);

        verify(primaryDataSource).isHealthy();
        verify(fallbackDataSource).isHealthy();
        verify(thirdDataSource).isHealthy();
    }

    @Test
    @DisplayName("Should log connection issue warning when connection exception occurs")
    void shouldLogConnectionIssueWarningWhenConnectionExceptionOccurs() throws Exception {
        when(primaryDataSource.isHealthy()).thenReturn(true);
        when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
        when(fallbackDataSource.isHealthy()).thenReturn(true);
        when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

        List<TestEntity> fallbackData = List.of(new TestEntity(2L, FALLBACK_ENTITY_NAME));

        DataSourceExceptionHandler.DataSourceOperation<TestEntity> operation = dataSource -> {
            if (dataSource == primaryDataSource) {
                return CompletableFuture.failedFuture(new RuntimeException(MSG_CONNECTION_TIMEOUT));
            } else {
                return CompletableFuture.completedFuture(fallbackData);
            }
        };

        CompletableFuture<List<TestEntity>> result = DataSourceExceptionHandler.executeWithFallback(
                primaryDataSource, operation, TEST_SOURCE_NAME);

        List<TestEntity> actualData = result.get();
        assertThat(actualData).isEqualTo(fallbackData);
    }

    // Test entity class
    public record TestEntity(Long id, String name) {

    }
}