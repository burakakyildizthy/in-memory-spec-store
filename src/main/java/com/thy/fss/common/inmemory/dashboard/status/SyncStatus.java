package com.thy.fss.common.inmemory.dashboard.status;

/**
 * Enumeration of possible synchronization statuses for a dashboard.
 * Indicates the current state of dashboard synchronization with the data store.
 */
public enum SyncStatus {

    /**
     * Dashboard synchronization is active and running normally.
     */
    ACTIVE,

    /**
     * Dashboard synchronization is paused due to inactivity timeout.
     * Will resume when dashboard is accessed again.
     */
    PAUSED,

    /**
     * Dashboard synchronization has encountered an error.
     * Manual intervention may be required.
     */
    ERROR
}