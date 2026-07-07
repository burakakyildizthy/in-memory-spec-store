package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.datasource.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for error recovery and fallback mechanisms.
 * Tests DataSourceExceptionHandler and other error recovery strategies.
 */
@DisplayName("Error Recovery and Fallback Tests")
class ErrorRecoveryTest {

    // Constants for duplicate string literals
    private static final String TEST_DATASOURCE_NAME = "test-datasource";
    private static final String MSG_SUCCESS = "success";
    private static final String MSG_SERVICE_UNAVAILABLE = "Service unavailable";
    private static final String MSG_CONNECTION_TIMEOUT_OCCURRED = "Connection timeout occurred";
    private static final String MSG_CIRCUIT_BREAKER_OPEN = "Circuit breaker is OPEN";
    private static final String MSG_INVALID_QUERY_PARAMETER = "Invalid query parameter";
    private static final String MSG_NON_RETRYABLE_ERROR = "Non-retryable error";
    private static final String MSG_UNEXPECTED_ERROR_RETRY = "Unexpected error in retry mechanism";
    private static final String MSG_RECOVERED = "recovered";
    private static final String MSG_CONNECTION_FAILED_FOR_DS = "Connection failed for DataSource '";
    private static final String MSG_OPERATION_FAILED_FOR_DS = "Operation failed for DataSource '";

    @Mock
    private DataSource<String> primaryDataSource;

    @Mock
    private DataSource<String> fallbackDataSource;

    @Mock
    private DataSource<String> secondaryFallbackDataSource;

    @BeforeEach
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Mocks are automatically closed
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize mocks", e);
        }
    }

    /**
     * Simple circuit breaker implementation for testing purposes.
     * Uses controllable time for better testability.
     */
    private static class SimpleCircuitBreaker {
        private final int failureThreshold;
        private final long timeout;
        private State state = State.CLOSED;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private long currentTime = System.currentTimeMillis();

        public SimpleCircuitBreaker(int failureThreshold, long timeout) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }

        // For testing purposes - allows controlling time
        public void advanceTime(long milliseconds) {
            this.currentTime += milliseconds;
        }

        public <T> T execute(Supplier<T> operation) {
            if (state == State.OPEN) {
                if (getCurrentTime() - lastFailureTime > timeout) {
                    state = State.HALF_OPEN;
                } else {
                    throw new RuntimeException(MSG_CIRCUIT_BREAKER_OPEN);
                }
            }

            try {
                T result = operation.get();
                onSuccess();
                return result;
            } catch (RuntimeException e) {
                onFailure();
                throw e;
            }
        }

        private long getCurrentTime() {
            return currentTime;
        }

        private void onSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }

        private void onFailure() {
            failureCount++;
            lastFailureTime = getCurrentTime();

            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }

        private enum State {CLOSED, OPEN, HALF_OPEN}
    }

    @Nested
    @DisplayName("DataSourceExceptionHandler Tests")
    class DataSourceExceptionHandlerTest {

        @Test
        @DisplayName("Should execute operation successfully on primary DataSource")
        void testSuccessfulPrimaryExecution() {
            // Setup
            List<String> expectedData = Arrays.asList("data1", "data2");
            when(primaryDataSource.isHealthy()).thenReturn(true);
            when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

            DataSourceExceptionHandler.DataSourceOperation<String> operation =
                    dataSource -> CompletableFuture.completedFuture(expectedData);

            // Execute
            CompletableFuture<List<String>> result = DataSourceExceptionHandler.executeWithFallback(
                    primaryDataSource, operation, TEST_DATASOURCE_NAME);

            // Verify
            assertDoesNotThrow(() -> {
                List<String> data = result.get();
                assertEquals(expectedData, data);
            });

            verify(primaryDataSource).isHealthy();
            verify(primaryDataSource).getFallbackDataSource();
        }

        @Test
        @DisplayName("Should fallback to secondary DataSource when primary fails")
        void testFallbackExecution() {
            // Setup primary to fail
            when(primaryDataSource.isHealthy()).thenReturn(true);
            when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
            when(fallbackDataSource.isHealthy()).thenReturn(true);
            when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

            List<String> fallbackData = Arrays.asList("fallback1", "fallback2");

            DataSourceExceptionHandler.DataSourceOperation<String> operation = dataSource -> {
                if (dataSource == primaryDataSource) {
                    throw new RuntimeException("Primary failed");
                }
                return CompletableFuture.completedFuture(fallbackData);
            };

            // Execute
            CompletableFuture<List<String>> result = DataSourceExceptionHandler.executeWithFallback(
                    primaryDataSource, operation, TEST_DATASOURCE_NAME);

            // Verify
            assertDoesNotThrow(() -> {
                List<String> data = result.get();
                assertEquals(fallbackData, data);
            });
        }

        @Test
        @DisplayName("Should skip unhealthy DataSources in fallback chain")
        void testSkipUnhealthyDataSources() {
            // Setup
            when(primaryDataSource.isHealthy()).thenReturn(false); // Unhealthy
            when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
            when(fallbackDataSource.isHealthy()).thenReturn(true); // Healthy
            when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

            List<String> fallbackData = List.of("healthy-data");

            DataSourceExceptionHandler.DataSourceOperation<String> operation =
                    dataSource -> CompletableFuture.completedFuture(fallbackData);

            // Execute
            CompletableFuture<List<String>> result = DataSourceExceptionHandler.executeWithFallback(
                    primaryDataSource, operation, TEST_DATASOURCE_NAME);

            // Verify
            assertDoesNotThrow(() -> {
                List<String> data = result.get();
                assertEquals(fallbackData, data);
            });

            verify(primaryDataSource).isHealthy();
            verify(fallbackDataSource).isHealthy();
        }

        @Test
        @DisplayName("Should return empty list when all DataSources fail")
        void testAllDataSourcesFail() {
            // Setup all to fail
            when(primaryDataSource.isHealthy()).thenReturn(true);
            when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
            when(fallbackDataSource.isHealthy()).thenReturn(true);
            when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

            DataSourceExceptionHandler.DataSourceOperation<String> operation =
                    dataSource -> {
                        throw new RuntimeException("All fail");
                    };

            // Execute
            CompletableFuture<List<String>> result = DataSourceExceptionHandler.executeWithFallback(
                    primaryDataSource, operation, TEST_DATASOURCE_NAME);

            // Verify
            assertDoesNotThrow(() -> {
                List<String> data = result.get();
                assertTrue(data.isEmpty());
            });
        }

        @Test
        @DisplayName("Should handle multiple levels of fallback")
        void testMultipleFallbackLevels() {
            // Setup three-level fallback chain
            when(primaryDataSource.isHealthy()).thenReturn(true);
            when(primaryDataSource.getFallbackDataSource()).thenReturn(Optional.of(fallbackDataSource));
            when(fallbackDataSource.isHealthy()).thenReturn(true);
            when(fallbackDataSource.getFallbackDataSource()).thenReturn(Optional.of(secondaryFallbackDataSource));
            when(secondaryFallbackDataSource.isHealthy()).thenReturn(true);
            when(secondaryFallbackDataSource.getFallbackDataSource()).thenReturn(Optional.empty());

            List<String> secondaryData = List.of("secondary-data");

            DataSourceExceptionHandler.DataSourceOperation<String> operation = dataSource -> {
                if (dataSource == primaryDataSource || dataSource == fallbackDataSource) {
                    throw new RuntimeException("First two fail");
                }
                return CompletableFuture.completedFuture(secondaryData);
            };

            // Execute
            CompletableFuture<List<String>> result = DataSourceExceptionHandler.executeWithFallback(
                    primaryDataSource, operation, TEST_DATASOURCE_NAME);

            // Verify
            assertDoesNotThrow(() -> {
                List<String> data = result.get();
                assertEquals(secondaryData, data);
            });
        }
    }

    @Nested
    @DisplayName("Operation Wrapping Tests")
    class OperationWrappingTest {

        @Test
        @DisplayName("Should execute wrapped operation successfully")
        void testSuccessfulWrappedOperation() {
            String expectedResult = MSG_SUCCESS;
            Supplier<String> operation = () -> expectedResult;

            Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                    operation, TEST_DATASOURCE_NAME);

            String result = wrappedOperation.get();
            assertEquals(expectedResult, result);
        }

        @Test
        @DisplayName("Should wrap connection exceptions as DataSourceConnectionException")
        void testConnectionExceptionWrapping() {
            Supplier<String> operation = () -> {
                throw new RuntimeException(MSG_CONNECTION_TIMEOUT_OCCURRED);
            };

            Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                    operation, TEST_DATASOURCE_NAME);

            DataSourceConnectionException exception = assertThrows(
                    DataSourceConnectionException.class, wrappedOperation::get);

            assertTrue(exception.getMessage().contains(MSG_CONNECTION_FAILED_FOR_DS + TEST_DATASOURCE_NAME + "'"));
            assertTrue(exception.getMessage().contains(MSG_CONNECTION_TIMEOUT_OCCURRED));
            assertInstanceOf(RuntimeException.class, exception.getCause());
        }

        @Test
        @DisplayName("Should wrap non-connection exceptions as DataSourceException")
        void testNonConnectionExceptionWrapping() {
            Supplier<String> operation = () -> {
                throw new IllegalArgumentException(MSG_INVALID_QUERY_PARAMETER);
            };

            Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                    operation, TEST_DATASOURCE_NAME);

            DataSourceException exception = assertThrows(
                    DataSourceException.class, wrappedOperation::get);

            assertTrue(exception.getMessage().contains(MSG_OPERATION_FAILED_FOR_DS + TEST_DATASOURCE_NAME + "'"));
            assertTrue(exception.getMessage().contains(MSG_INVALID_QUERY_PARAMETER));
            assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        }

        @Test
        @DisplayName("Should preserve DataSourceConnectionException without double wrapping")
        void testPreserveDataSourceConnectionException() {
            DataSourceConnectionException originalException = new DataSourceConnectionException(
                    "Already wrapped", "original-source");

            Supplier<String> operation = () -> {
                throw originalException;
            };

            Supplier<String> wrappedOperation = DataSourceExceptionHandler.wrapOperation(
                    operation, TEST_DATASOURCE_NAME);

            DataSourceConnectionException exception = assertThrows(
                    DataSourceConnectionException.class, wrappedOperation::get);

            // Should wrap even DataSourceConnectionException to add context
            assertTrue(exception.getMessage().contains(MSG_CONNECTION_FAILED_FOR_DS + TEST_DATASOURCE_NAME + "'"));
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Connection Exception Detection Tests")
    class ConnectionExceptionDetectionTest {

        @Test
        @DisplayName("Should detect DataSourceConnectionException")
        void testDetectDataSourceConnectionException() {
            DataSourceConnectionException exception = new DataSourceConnectionException("Connection failed");

            assertTrue(DataSourceExceptionHandler.isConnectionException(exception));
        }

        @Test
        @DisplayName("Should detect connection-related messages")
        void testDetectConnectionMessages() {
            String[] connectionMessages = {
                    "Connection timeout",
                    "Network unreachable",
                    "Connection refused",
                    "Authentication failed",
                    "Unauthorized access",
                    "CONNECTION ERROR"
            };

            for (String message : connectionMessages) {
                RuntimeException exception = new RuntimeException(message);
                assertTrue(DataSourceExceptionHandler.isConnectionException(exception),
                        "Should detect connection issue in message: " + message);
            }
        }

        @Test
        @DisplayName("Should not detect non-connection exceptions")
        void testNotDetectNonConnectionExceptions() {
            String[] nonConnectionMessages = {
                    "Invalid query syntax",
                    "Data not found",
                    "Parsing error",
                    "Validation failed"
            };

            for (String message : nonConnectionMessages) {
                RuntimeException exception = new RuntimeException(message);
                assertFalse(DataSourceExceptionHandler.isConnectionException(exception),
                        "Should not detect connection issue in message: " + message);
            }
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void testHandleNullMessage() {
            RuntimeException exception = new RuntimeException((String) null);

            assertFalse(DataSourceExceptionHandler.isConnectionException(exception));
        }

        @Test
        @DisplayName("Should handle empty message gracefully")
        void testHandleEmptyMessage() {
            RuntimeException exception = new RuntimeException("");

            assertFalse(DataSourceExceptionHandler.isConnectionException(exception));
        }
    }

    @Nested
    @DisplayName("Retry Mechanism Tests")
    class RetryMechanismTest {

        @Test
        @DisplayName("Should retry operation on transient failures")
        void testRetryOnTransientFailures() {
            final int[] attemptCount = {0};
            final int maxAttempts = 3;

            Supplier<String> operation = () -> {
                attemptCount[0]++;
                if (attemptCount[0] < maxAttempts) {
                    throw new RuntimeException("Transient failure " + attemptCount[0]);
                }
                return MSG_SUCCESS;
            };

            // Simulate retry mechanism
            String result = executeWithRetry(operation, maxAttempts);

            assertEquals(MSG_SUCCESS, result);
            assertEquals(maxAttempts, attemptCount[0]);
        }

        @Test
        @DisplayName("Should fail after max retry attempts")
        void testFailAfterMaxRetries() {
            final int[] attemptCount = {0};
            final int maxAttempts = 3;

            Supplier<String> operation = () -> {
                attemptCount[0]++;
                throw new RuntimeException("Persistent failure " + attemptCount[0]);
            };

            // Simulate retry mechanism
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> executeWithRetry(operation, maxAttempts));

            assertTrue(exception.getMessage().contains("Persistent failure 3"));
            assertEquals(maxAttempts, attemptCount[0]);
        }

        @Test
        @DisplayName("Should not retry on non-retryable exceptions")
        void testNoRetryOnNonRetryableExceptions() {
            final int[] attemptCount = {0};

            Supplier<String> operation = () -> {
                attemptCount[0]++;
                throw new IllegalArgumentException(MSG_NON_RETRYABLE_ERROR);
            };

            // Simulate retry mechanism that doesn't retry IllegalArgumentException
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> executeWithRetrySelective(operation, 3));

            assertEquals(MSG_NON_RETRYABLE_ERROR, exception.getMessage());
            assertEquals(1, attemptCount[0]); // Should only attempt once
        }

        private String executeWithRetry(Supplier<String> operation, int maxAttempts) {
            RuntimeException lastException = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return operation.get();
                } catch (RuntimeException e) {
                    lastException = e;
                    if (attempt == maxAttempts) {
                        throw e;
                    }
                    // In real implementation, would add delay between retries
                }
            }

            // This line should never be reached, but added for safety
            if (lastException != null) {
                throw lastException;
            }
            throw new RuntimeException(MSG_UNEXPECTED_ERROR_RETRY);
        }

        private String executeWithRetrySelective(Supplier<String> operation, int maxAttempts) {
            RuntimeException lastException = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return operation.get();
                } catch (IllegalArgumentException e) {
                    // Don't retry IllegalArgumentException
                    throw e;
                } catch (RuntimeException e) {
                    lastException = e;
                    if (attempt == maxAttempts) {
                        throw e;
                    }
                }
            }

            // This line should never be reached, but added for safety
            if (lastException != null) {
                throw lastException;
            }
            throw new RuntimeException(MSG_UNEXPECTED_ERROR_RETRY);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Pattern Tests")
    class CircuitBreakerTest {

        @Test
        @DisplayName("Should open circuit after failure threshold")
        void testCircuitBreakerOpens() {
            SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker(3, 1000);

            Supplier<String> failingOperation = () -> {
                throw new RuntimeException(MSG_SERVICE_UNAVAILABLE);
            };

            // First 3 attempts should fail and open the circuit
            for (int i = 0; i < 3; i++) {
                assertThrows(RuntimeException.class,
                        () -> circuitBreaker.execute(failingOperation));
            }

            // 4th attempt should fail fast due to open circuit
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> circuitBreaker.execute(failingOperation));

            assertTrue(exception.getMessage().contains(MSG_CIRCUIT_BREAKER_OPEN));
        }

        @Test
        @DisplayName("Should allow requests when circuit is closed")
        void testCircuitBreakerClosed() {
            SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker(3, 1000);

            Supplier<String> successOperation = () -> MSG_SUCCESS;

            String result = circuitBreaker.execute(successOperation);
            assertEquals(MSG_SUCCESS, result);
        }

        @Test
        @DisplayName("Should reset circuit after timeout")
        void testCircuitBreakerReset() {
            SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker(2, 100); // 100ms timeout

            Supplier<String> failingOperation = () -> {
                throw new RuntimeException(MSG_SERVICE_UNAVAILABLE);
            };

            // Fail twice to open circuit
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(failingOperation));
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(failingOperation));

            // Should be open now
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(failingOperation));

            // Advance time beyond timeout
            circuitBreaker.advanceTime(150);

            // Should allow one request (half-open state)
            Supplier<String> successOperation = () -> MSG_RECOVERED;
            String result = circuitBreaker.execute(successOperation);
            assertEquals(MSG_RECOVERED, result);
        }
    }
}