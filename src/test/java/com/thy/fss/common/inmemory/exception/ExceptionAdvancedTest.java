package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionAdvancedTest {

    // Constants for duplicate string literals
    private static final String MSG_TEST_DATA_SOURCE_ERROR = "Test data source error";
    private static final String MSG_ROOT_CAUSE = "Root cause";
    private static final String MSG_CONNECTION_FAILED = "Connection failed";
    private static final String MSG_OBJECT_BUILDING_FAILED = "Object building failed";
    private static final String MSG_STORE_OPERATION_FAILED = "Store operation failed";
    private static final String MSG_SYNCHRONIZATION_FAILED = "Synchronization failed";
    private static final String MSG_TEST_MESSAGE = "Test message";
    private static final String MSG_TEST_EXCEPTION = "Test exception";
    private static final String MSG_MESSAGE = "message";
    private static final String MSG_OBJECT_BUILDING_ERROR = "Object building error";
    private static final String MSG_TEST_SYNC_EXCEPTION = "Test sync exception";

    @Test
    @DisplayName("Should create DataSourceException with message")
    void shouldCreateDataSourceExceptionWithMessage() {
        String message = MSG_TEST_DATA_SOURCE_ERROR;
        DataSourceException exception = new DataSourceException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create DataSourceException with message and cause")
    void shouldCreateDataSourceExceptionWithMessageAndCause() {
        String message = MSG_TEST_DATA_SOURCE_ERROR;
        Throwable cause = new RuntimeException(MSG_ROOT_CAUSE);
        DataSourceException exception = new DataSourceException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create DataSourceException with cause only")
    void shouldCreateDataSourceExceptionWithCauseOnly() {
        Throwable cause = new RuntimeException(MSG_ROOT_CAUSE);
        DataSourceException exception = new DataSourceException(cause);

        assertNotNull(exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create DataSourceConnectionException with message")
    void shouldCreateDataSourceConnectionExceptionWithMessage() {
        String message = MSG_CONNECTION_FAILED;
        DataSourceConnectionException exception = new DataSourceConnectionException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertInstanceOf(InMemoryDataStoreException.class, exception);
    }

    @Test
    @DisplayName("Should create DataSourceConnectionException with message and cause")
    void shouldCreateDataSourceConnectionExceptionWithMessageAndCause() {
        String message = MSG_CONNECTION_FAILED;
        Throwable cause = new java.net.ConnectException("Network error");
        DataSourceConnectionException exception = new DataSourceConnectionException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create ObjectBuildingException with message")
    void shouldCreateObjectBuildingExceptionWithMessage() {
        String message = MSG_OBJECT_BUILDING_FAILED;
        ObjectBuildingException exception = new ObjectBuildingException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create ObjectBuildingException with message and cause")
    void shouldCreateObjectBuildingExceptionWithMessageAndCause() {
        String message = MSG_OBJECT_BUILDING_FAILED;
        Throwable cause = new IllegalAccessException("Field access denied");
        ObjectBuildingException exception = new ObjectBuildingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create InMemoryDataStoreException with message")
    void shouldCreateInMemoryDataStoreExceptionWithMessage() {
        String message = MSG_STORE_OPERATION_FAILED;
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create InMemoryDataStoreException with message and cause")
    void shouldCreateInMemoryDataStoreExceptionWithMessageAndCause() {
        String message = MSG_STORE_OPERATION_FAILED;
        Throwable cause = new IllegalStateException("Invalid state");
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should create SynchronizationException with message")
    void shouldCreateSynchronizationExceptionWithMessage() {
        String message = MSG_SYNCHRONIZATION_FAILED;
        SynchronizationException exception = new SynchronizationException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create SynchronizationException with message and cause")
    void shouldCreateSynchronizationExceptionWithMessageAndCause() {
        String message = MSG_SYNCHRONIZATION_FAILED;
        Throwable cause = new InterruptedException("Thread interrupted");
        SynchronizationException exception = new SynchronizationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null messages gracefully")
    void shouldHandleNullMessagesGracefully() {
        DataSourceException exception1 = new DataSourceException((String) null);
        assertNull(exception1.getMessage());

        ObjectBuildingException exception2 = new ObjectBuildingException(null);
        assertNull(exception2.getMessage());

        InMemoryDataStoreException exception3 = new InMemoryDataStoreException(null);
        assertNull(exception3.getMessage());

        SynchronizationException exception4 = new SynchronizationException(null);
        assertNull(exception4.getMessage());
    }

    @Test
    @DisplayName("Should handle null causes gracefully")
    void shouldHandleNullCausesGracefully() {
        DataSourceException exception1 = new DataSourceException((Throwable) null);
        assertNull(exception1.getCause());

        ObjectBuildingException exception2 = new ObjectBuildingException(MSG_MESSAGE, (Throwable) null);
        assertEquals(MSG_MESSAGE, exception2.getMessage());
        assertNull(exception2.getCause());
    }

    @Test
    @DisplayName("Should maintain exception hierarchy")
    void shouldMaintainExceptionHierarchy() {
        // All custom exceptions should extend RuntimeException
        assertTrue(RuntimeException.class.isAssignableFrom(DataSourceException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(ObjectBuildingException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(InMemoryDataStoreException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(SynchronizationException.class));

        // DataSourceConnectionException should extend InMemoryDataStoreException
        assertTrue(InMemoryDataStoreException.class.isAssignableFrom(DataSourceConnectionException.class));
    }

    @Test
    @DisplayName("Should support exception chaining")
    void shouldSupportExceptionChaining() {
        RuntimeException rootCause = new RuntimeException(MSG_ROOT_CAUSE);
        DataSourceException dataSourceException = new DataSourceException("Data source error", rootCause);
        ObjectBuildingException objectBuildingException = new ObjectBuildingException(MSG_OBJECT_BUILDING_ERROR, dataSourceException);

        assertEquals(MSG_OBJECT_BUILDING_ERROR, objectBuildingException.getMessage());
        assertEquals(dataSourceException, objectBuildingException.getCause());
        assertEquals(rootCause, objectBuildingException.getCause().getCause());
    }

    @Test
    @DisplayName("Should provide stack trace information")
    void shouldProvideStackTraceInformation() {
        DataSourceException exception = new DataSourceException(MSG_TEST_EXCEPTION);

        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);

        // Should contain this test method in the stack trace
        boolean foundTestMethod = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().equals("shouldProvideStackTraceInformation")) {
                foundTestMethod = true;
                break;
            }
        }
        assertTrue(foundTestMethod);
    }

    @Test
    @DisplayName("Should support toString operations")
    void shouldSupportToStringOperations() {
        DataSourceException exception1 = new DataSourceException(MSG_TEST_MESSAGE);
        String toString1 = exception1.toString();
        assertNotNull(toString1);
        assertTrue(toString1.contains("DataSourceException"));
        assertTrue(toString1.contains(MSG_TEST_MESSAGE));

        RuntimeException cause = new RuntimeException("Cause message");
        ObjectBuildingException exception2 = new ObjectBuildingException(MSG_TEST_MESSAGE, cause);
        String toString2 = exception2.toString();
        assertNotNull(toString2);
        assertTrue(toString2.contains("ObjectBuildingException"));
        assertTrue(toString2.contains(MSG_TEST_MESSAGE));
    }

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
        // Test throwing and catching DataSourceException
        assertThrows(DataSourceException.class, () -> {
            throw new DataSourceException(MSG_TEST_EXCEPTION);
        });

        // Test catching as RuntimeException
        assertThrows(RuntimeException.class, () -> {
            throw new ObjectBuildingException(MSG_TEST_EXCEPTION);
        });

        // Test catching specific exception type
        try {
            throw new SynchronizationException(MSG_TEST_SYNC_EXCEPTION);
        } catch (SynchronizationException e) {
            assertEquals(MSG_TEST_SYNC_EXCEPTION, e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle empty string messages")
    void shouldHandleEmptyStringMessages() {
        DataSourceException exception1 = new DataSourceException("");
        assertEquals("", exception1.getMessage());

        ObjectBuildingException exception2 = new ObjectBuildingException("", new RuntimeException());
        assertEquals("", exception2.getMessage());
    }

    @Test
    @DisplayName("Should support suppressed exceptions")
    void shouldSupportSuppressedExceptions() {
        DataSourceException mainException = new DataSourceException("Main exception");
        RuntimeException suppressedException = new RuntimeException("Suppressed exception");

        mainException.addSuppressed(suppressedException);

        Throwable[] suppressed = mainException.getSuppressed();
        assertEquals(1, suppressed.length);
        assertEquals(suppressedException, suppressed[0]);
    }
}