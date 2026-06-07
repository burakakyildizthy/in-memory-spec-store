package com.thy.fss.common.inmemory.exception;

/**
 * Exception thrown when there are issues during the synchronization process.
 * This includes problems with periodic data updates, version switching failures,
 * and other synchronization-related errors in the InMemoryDataStore.
 */
public class SynchronizationException extends InMemoryDataStoreException {

    private final long currentVersion;
    private final String syncPhase;

    /**
     * Constructs a new SynchronizationException with the specified detail message.
     *
     * @param message the detail message explaining the synchronization issue
     */
    public SynchronizationException(String message) {
        super(message);
        this.currentVersion = -1;
        this.syncPhase = null;
    }

    /**
     * Constructs a new SynchronizationException with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the synchronization issue
     * @param cause   the underlying cause of the synchronization failure
     */
    public SynchronizationException(String message, Throwable cause) {
        super(message, cause);
        this.currentVersion = -1;
        this.syncPhase = null;
    }

    /**
     * Constructs a new SynchronizationException with the specified detail message
     * and current version for better error context.
     *
     * @param message        the detail message explaining the synchronization issue
     * @param currentVersion the version number when the synchronization failed
     */
    public SynchronizationException(String message, long currentVersion) {
        super(message);
        this.currentVersion = currentVersion;
        this.syncPhase = null;
    }

    /**
     * Constructs a new SynchronizationException with the specified detail message,
     * cause, and current version for complete error context.
     *
     * @param message        the detail message explaining the synchronization issue
     * @param cause          the underlying cause of the synchronization failure
     * @param currentVersion the version number when the synchronization failed
     */
    public SynchronizationException(String message, Throwable cause, long currentVersion) {
        super(message, cause);
        this.currentVersion = currentVersion;
        this.syncPhase = null;
    }

    /**
     * Constructs a new SynchronizationException with the specified detail message,
     * current version, and sync phase for detailed error context.
     *
     * @param message        the detail message explaining the synchronization issue
     * @param currentVersion the version number when the synchronization failed
     * @param syncPhase      the phase of synchronization where the error occurred
     */
    public SynchronizationException(String message, long currentVersion, String syncPhase) {
        super(message);
        this.currentVersion = currentVersion;
        this.syncPhase = syncPhase;
    }

    /**
     * Constructs a new SynchronizationException with complete error context including
     * message, cause, current version, and sync phase.
     *
     * @param message        the detail message explaining the synchronization issue
     * @param cause          the underlying cause of the synchronization failure
     * @param currentVersion the version number when the synchronization failed
     * @param syncPhase      the phase of synchronization where the error occurred
     */
    public SynchronizationException(String message, Throwable cause, long currentVersion, String syncPhase) {
        super(message, cause);
        this.currentVersion = currentVersion;
        this.syncPhase = syncPhase;
    }

    /**
     * Returns the version number when the synchronization failure occurred.
     *
     * @return the current version, or -1 if not specified
     */
    public long getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns the synchronization phase where the error occurred.
     *
     * @return the sync phase, or null if not specified
     */
    public String getSyncPhase() {
        return syncPhase;
    }
}