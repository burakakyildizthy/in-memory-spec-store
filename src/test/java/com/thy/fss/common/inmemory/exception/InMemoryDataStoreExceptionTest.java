package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDataStoreExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
        String message = "Data store error";
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Data store initialization failed";
        Throwable cause = new IllegalStateException("Invalid configuration");
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        InMemoryDataStoreException exception = new InMemoryDataStoreException(null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
        String message = "Data store error";
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message, null);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        String message = "";
        InMemoryDataStoreException exception = new InMemoryDataStoreException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void shouldBeInstanceOfRuntimeException() {
        InMemoryDataStoreException exception = new InMemoryDataStoreException("test");
        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }

    @Test
    @DisplayName("Should preserve cause chain")
    void shouldPreserveCauseChain() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        IllegalStateException intermediateCause = new IllegalStateException("Intermediate cause", rootCause);
        InMemoryDataStoreException exception = new InMemoryDataStoreException("Final message", intermediateCause);

        assertEquals("Final message", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    @DisplayName("Should support exception chaining")
    void shouldSupportExceptionChaining() {
        Exception originalException = new Exception("Original error");
        InMemoryDataStoreException wrappedException = new InMemoryDataStoreException("Wrapped error", originalException);

        assertNotNull(wrappedException.getCause());
        assertEquals(originalException, wrappedException.getCause());
        assertEquals("Original error", wrappedException.getCause().getMessage());
    }
}