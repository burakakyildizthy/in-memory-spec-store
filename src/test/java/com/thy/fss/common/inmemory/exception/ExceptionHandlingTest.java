package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for all custom exception classes.
 * Tests exception construction, message handling, cause chaining, and custom properties.
 */
@DisplayName("Exception Handling Tests")
class ExceptionHandlingTest {

    // Common test display names
    private static final String DISPLAY_CREATE_WITH_MESSAGE = "Should create exception with message";
    private static final String DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE = "Should create exception with message and cause";
    private static final String DISPLAY_CREATE_WITH_ALL_PARAMETERS = "Should create exception with all parameters";

    // Common test messages
    private static final String TEST_ERROR_MESSAGE = "Test error message";
    private static final String DATASOURCE_ERROR = "DataSource error";
    private static final String NETWORK_FAILURE = "Network failure";

    @Nested
    @DisplayName("InMemoryDataStoreException Tests")
    class InMemoryDataStoreExceptionTest {

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE)
        void testExceptionWithMessage() {
            String message = TEST_ERROR_MESSAGE;
            InMemoryDataStoreException exception = new InMemoryDataStoreException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE)
        void testExceptionWithMessageAndCause() {
            String message = TEST_ERROR_MESSAGE;
            RuntimeException cause = new RuntimeException("Root cause");
            InMemoryDataStoreException exception = new InMemoryDataStoreException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void testExceptionWithNullMessage() {
            InMemoryDataStoreException exception = new InMemoryDataStoreException(null);

            assertNull(exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should handle null cause gracefully")
        void testExceptionWithNullCause() {
            String message = "Test message";
            InMemoryDataStoreException exception = new InMemoryDataStoreException(message, null);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }
    }

    @Nested
    @DisplayName("DataSourceException Tests")
    class DataSourceExceptionTest {

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE)
        void testExceptionWithMessage() {
            String message = DATASOURCE_ERROR;
            DataSourceException exception = new DataSourceException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE)
        void testExceptionWithMessageAndCause() {
            String message = DATASOURCE_ERROR;
            Exception cause = new Exception(NETWORK_FAILURE);
            DataSourceException exception = new DataSourceException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with cause only")
        void testExceptionWithCauseOnly() {
            Exception cause = new Exception(NETWORK_FAILURE);
            DataSourceException exception = new DataSourceException(cause);

            assertEquals(cause, exception.getCause());
            // Message should be derived from cause
            assertTrue(exception.getMessage().contains(NETWORK_FAILURE));
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void testExceptionWithNullValues() {
            DataSourceException exception1 = new DataSourceException((String) null);
            DataSourceException exception2 = new DataSourceException((Throwable) null);
            DataSourceException exception3 = new DataSourceException(null, null);

            assertNull(exception1.getMessage());
            assertNull(exception2.getCause());
            assertNull(exception3.getMessage());
            assertNull(exception3.getCause());
        }
    }

    @Nested
    @DisplayName("DataSourceConnectionException Tests")
    class DataSourceConnectionExceptionTest {

        @Test
        @DisplayName("Should create exception with message only")
        void testExceptionWithMessage() {
            String message = "Connection failed";
            DataSourceConnectionException exception = new DataSourceConnectionException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
            assertNull(exception.getDataSourceName());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE)
        void testExceptionWithMessageAndCause() {
            String message = "Connection timeout";
            Exception cause = new Exception("Socket timeout");
            DataSourceConnectionException exception = new DataSourceConnectionException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertNull(exception.getDataSourceName());
        }

        @Test
        @DisplayName("Should create exception with message and data source name")
        void testExceptionWithMessageAndDataSourceName() {
            String message = "Connection refused";
            String dataSourceName = "primary-db";
            DataSourceConnectionException exception = new DataSourceConnectionException(message, dataSourceName);

            assertEquals(message, exception.getMessage());
            assertEquals(dataSourceName, exception.getDataSourceName());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_ALL_PARAMETERS)
        void testExceptionWithAllParameters() {
            String message = "Authentication failed";
            Exception cause = new Exception("Invalid credentials");
            String dataSourceName = "auth-service";
            DataSourceConnectionException exception = new DataSourceConnectionException(message, cause, dataSourceName);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(dataSourceName, exception.getDataSourceName());
        }

        @Test
        @DisplayName("Should handle null data source name")
        void testExceptionWithNullDataSourceName() {
            String message = "Connection error";
            DataSourceConnectionException exception = new DataSourceConnectionException(message, (String) null);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getDataSourceName());
        }
    }

    @Nested
    @DisplayName("ObjectBuildingException Tests")
    class ObjectBuildingExceptionTest {

        @Test
        @DisplayName("Should create exception with message only")
        void testExceptionWithMessage() {
            String message = "Object building failed";
            ObjectBuildingException exception = new ObjectBuildingException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
            assertNull(exception.getTargetClass());
            assertNull(exception.getPropertyName());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE)
        void testExceptionWithMessageAndCause() {
            String message = "Reflection error";
            Exception cause = new ReflectiveOperationException("Field not accessible");
            ObjectBuildingException exception = new ObjectBuildingException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertNull(exception.getTargetClass());
            assertNull(exception.getPropertyName());
        }

        @Test
        @DisplayName("Should create exception with message and target class")
        void testExceptionWithMessageAndTargetClass() {
            String message = "Type conversion failed";
            Class<?> targetClass = String.class;
            ObjectBuildingException exception = new ObjectBuildingException(message, targetClass);

            assertEquals(message, exception.getMessage());
            assertEquals(targetClass, exception.getTargetClass());
            assertNull(exception.getCause());
            assertNull(exception.getPropertyName());
        }

        @Test
        @DisplayName("Should create exception with message, cause, and target class")
        void testExceptionWithMessageCauseAndTargetClass() {
            String message = "Instantiation failed";
            Exception cause = new InstantiationException("No default constructor");
            Class<?> targetClass = Object.class;
            ObjectBuildingException exception = new ObjectBuildingException(message, cause, targetClass);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(targetClass, exception.getTargetClass());
            assertNull(exception.getPropertyName());
        }

        @Test
        @DisplayName("Should create exception with message, target class, and property name")
        void testExceptionWithMessageTargetClassAndPropertyName() {
            String message = "Property mapping failed";
            Class<?> targetClass = String.class;
            String propertyName = "length";
            ObjectBuildingException exception = new ObjectBuildingException(message, targetClass, propertyName);

            assertEquals(message, exception.getMessage());
            assertEquals(targetClass, exception.getTargetClass());
            assertEquals(propertyName, exception.getPropertyName());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_ALL_PARAMETERS)
        void testExceptionWithAllParameters() {
            String message = "Property validation failed";
            Exception cause = new IllegalArgumentException("Invalid value");
            Class<?> targetClass = Integer.class;
            String propertyName = "value";
            ObjectBuildingException exception = new ObjectBuildingException(message, cause, targetClass, propertyName);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(targetClass, exception.getTargetClass());
            assertEquals(propertyName, exception.getPropertyName());
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void testExceptionWithNullValues() {
            ObjectBuildingException exception = new ObjectBuildingException("test", null, (String) null);

            assertEquals("test", exception.getMessage());
            assertNull(exception.getTargetClass());
            assertNull(exception.getPropertyName());
        }
    }

    @Nested
    @DisplayName("SynchronizationException Tests")
    class SynchronizationExceptionTest {

        @Test
        @DisplayName("Should create exception with message only")
        void testExceptionWithMessage() {
            String message = "Synchronization failed";
            SynchronizationException exception = new SynchronizationException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
            assertEquals(-1, exception.getCurrentVersion());
            assertNull(exception.getSyncPhase());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_MESSAGE_AND_CAUSE)
        void testExceptionWithMessageAndCause() {
            String message = "Version conflict";
            Exception cause = new Exception("Concurrent modification");
            SynchronizationException exception = new SynchronizationException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(-1, exception.getCurrentVersion());
            assertNull(exception.getSyncPhase());
        }

        @Test
        @DisplayName("Should create exception with message and current version")
        void testExceptionWithMessageAndCurrentVersion() {
            String message = "Version mismatch";
            long currentVersion = 42L;
            SynchronizationException exception = new SynchronizationException(message, currentVersion);

            assertEquals(message, exception.getMessage());
            assertEquals(currentVersion, exception.getCurrentVersion());
            assertNull(exception.getCause());
            assertNull(exception.getSyncPhase());
        }

        @Test
        @DisplayName("Should create exception with message, cause, and current version")
        void testExceptionWithMessageCauseAndCurrentVersion() {
            String message = "Sync interrupted";
            Exception cause = new InterruptedException("Thread interrupted");
            long currentVersion = 123L;
            SynchronizationException exception = new SynchronizationException(message, cause, currentVersion);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(currentVersion, exception.getCurrentVersion());
            assertNull(exception.getSyncPhase());
        }

        @Test
        @DisplayName("Should create exception with message, current version, and sync phase")
        void testExceptionWithMessageCurrentVersionAndSyncPhase() {
            String message = "Phase validation failed";
            long currentVersion = 456L;
            String syncPhase = "VALIDATION";
            SynchronizationException exception = new SynchronizationException(message, currentVersion, syncPhase);

            assertEquals(message, exception.getMessage());
            assertEquals(currentVersion, exception.getCurrentVersion());
            assertEquals(syncPhase, exception.getSyncPhase());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName(DISPLAY_CREATE_WITH_ALL_PARAMETERS)
        void testExceptionWithAllParameters() {
            String message = "Complete sync failure";
            Exception cause = new RuntimeException("System error");
            long currentVersion = 789L;
            String syncPhase = "COMMIT";
            SynchronizationException exception = new SynchronizationException(message, cause, currentVersion, syncPhase);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
            assertEquals(currentVersion, exception.getCurrentVersion());
            assertEquals(syncPhase, exception.getSyncPhase());
        }

        @Test
        @DisplayName("Should handle negative version numbers")
        void testExceptionWithNegativeVersion() {
            String message = "Invalid version";
            long negativeVersion = -999L;
            SynchronizationException exception = new SynchronizationException(message, negativeVersion);

            assertEquals(message, exception.getMessage());
            assertEquals(negativeVersion, exception.getCurrentVersion());
        }

        @Test
        @DisplayName("Should handle null sync phase")
        void testExceptionWithNullSyncPhase() {
            String message = "Unknown phase error";
            long currentVersion = 100L;
            SynchronizationException exception = new SynchronizationException(message, currentVersion, null);

            assertEquals(message, exception.getMessage());
            assertEquals(currentVersion, exception.getCurrentVersion());
            assertNull(exception.getSyncPhase());
        }
    }
}