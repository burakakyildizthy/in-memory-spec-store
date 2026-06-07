package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizationExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
        String message = "Synchronization failed";
        SynchronizationException exception = new SynchronizationException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(-1, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
        assertInstanceOf(InMemoryDataStoreException.class, exception);
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Data fetch failed during sync";
        Throwable cause = new RuntimeException("Network timeout");
        SynchronizationException exception = new SynchronizationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(-1, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should create exception with message and current version")
    void shouldCreateExceptionWithMessageAndCurrentVersion() {
        String message = "Version switch failed";
        long currentVersion = 42L;
        SynchronizationException exception = new SynchronizationException(message, currentVersion);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should create exception with message, cause, and current version")
    void shouldCreateExceptionWithMessageCauseAndCurrentVersion() {
        String message = "Data validation failed";
        Throwable cause = new IllegalStateException("Invalid data format");
        long currentVersion = 123L;
        SynchronizationException exception = new SynchronizationException(message, cause, currentVersion);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should create exception with message, current version, and sync phase")
    void shouldCreateExceptionWithMessageCurrentVersionAndSyncPhase() {
        String message = "Hook execution failed";
        long currentVersion = 456L;
        String syncPhase = "POST_SYNC";
        SynchronizationException exception = new SynchronizationException(message, currentVersion, syncPhase);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertEquals(syncPhase, exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should create exception with all parameters")
    void shouldCreateExceptionWithAllParameters() {
        String message = "Complete sync failure";
        Throwable cause = new RuntimeException("Database connection lost");
        long currentVersion = 789L;
        String syncPhase = "DATA_FETCH";
        SynchronizationException exception = new SynchronizationException(message, cause, currentVersion, syncPhase);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertEquals(syncPhase, exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        SynchronizationException exception = new SynchronizationException(null);
        assertNull(exception.getMessage());
        assertEquals(-1, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
        String message = "Sync error";
        SynchronizationException exception = new SynchronizationException(message, null);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle null sync phase")
    void shouldHandleNullSyncPhase() {
        String message = "Sync error";
        long currentVersion = 100L;
        SynchronizationException exception = new SynchronizationException(message, currentVersion, null);
        assertEquals(message, exception.getMessage());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertNull(exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should handle empty sync phase")
    void shouldHandleEmptySyncPhase() {
        String message = "Sync error";
        long currentVersion = 200L;
        String syncPhase = "";
        SynchronizationException exception = new SynchronizationException(message, currentVersion, syncPhase);
        assertEquals(message, exception.getMessage());
        assertEquals(currentVersion, exception.getCurrentVersion());
        assertEquals(syncPhase, exception.getSyncPhase());
    }

    @Test
    @DisplayName("Should handle zero current version")
    void shouldHandleZeroCurrentVersion() {
        String message = "Initial sync failed";
        long currentVersion = 0L;
        SynchronizationException exception = new SynchronizationException(message, currentVersion);
        assertEquals(message, exception.getMessage());
        assertEquals(currentVersion, exception.getCurrentVersion());
    }

    @Test
    @DisplayName("Should handle negative current version")
    void shouldHandleNegativeCurrentVersion() {
        String message = "Invalid version";
        long currentVersion = -100L;
        SynchronizationException exception = new SynchronizationException(message, currentVersion);
        assertEquals(message, exception.getMessage());
        assertEquals(currentVersion, exception.getCurrentVersion());
    }

    @Test
    @DisplayName("Should be instance of InMemoryDataStoreException")
    void shouldBeInstanceOfInMemoryDataStoreException() {
        SynchronizationException exception = new SynchronizationException("test");
        assertInstanceOf(InMemoryDataStoreException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should preserve version information")
    void shouldPreserveVersionInformation() {
        long version = Long.MAX_VALUE;
        SynchronizationException exception = new SynchronizationException("error", version);
        assertEquals(version, exception.getCurrentVersion());
    }

    @Test
    @DisplayName("Should preserve sync phase information")
    void shouldPreserveSyncPhaseInformation() {
        String syncPhase = "VALIDATION_PHASE";
        SynchronizationException exception = new SynchronizationException("error", 1L, syncPhase);
        assertEquals(syncPhase, exception.getSyncPhase());
    }
}