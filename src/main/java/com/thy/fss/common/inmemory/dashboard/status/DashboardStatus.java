package com.thy.fss.common.inmemory.dashboard.status;

import com.thy.fss.common.inmemory.dashboard.result.HealthStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Status information for a dashboard including monitoring and health data.
 * This class provides comprehensive information about dashboard state,
 * synchronization status, and error details for monitoring purposes.
 * This class is immutable and thread-safe.
 */
public record DashboardStatus(String id, String name, LocalDateTime lastCalculationTime, LocalDateTime lastReadTime,
                              SyncStatus syncStatus, HealthStatus healthStatus, Map<String, String> errors) {

    /**
     * Creates a new DashboardStatus with the specified parameters.
     *
     * @param id                  the dashboard ID
     * @param name                the dashboard name
     * @param lastCalculationTime the timestamp of the last calculation (can be null if never calculated)
     * @param lastReadTime        the timestamp of the last read (can be null if never read)
     * @param syncStatus          the synchronization status
     * @param healthStatus        the health status
     * @param errors              the map of error messages (can be empty)
     * @throws IllegalArgumentException if required parameters are null
     */
    public DashboardStatus(String id,
                           String name,
                           LocalDateTime lastCalculationTime,
                           LocalDateTime lastReadTime,
                           SyncStatus syncStatus,
                           HealthStatus healthStatus,
                           Map<String, String> errors) {

        // Validate required parameters
        this.id = Objects.requireNonNull(id, "Dashboard ID cannot be null");
        this.name = Objects.requireNonNull(name, "Dashboard name cannot be null");
        this.syncStatus = Objects.requireNonNull(syncStatus, "Sync status cannot be null");
        this.healthStatus = Objects.requireNonNull(healthStatus, "Health status cannot be null");

        // These can be null if never calculated/read
        this.lastCalculationTime = lastCalculationTime;
        this.lastReadTime = lastReadTime;

        // Create defensive copy of errors map
        if (errors == null) {
            this.errors = Collections.emptyMap();
        } else {
            this.errors = Collections.unmodifiableMap(new HashMap<>(errors));
        }
    }

    /**
     * Checks if the dashboard has any errors.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Checks if the dashboard is currently active (sync not paused and healthy).
     *
     * @return true if the dashboard is active, false otherwise
     */
    public boolean isActive() {
        return syncStatus == SyncStatus.ACTIVE && healthStatus == HealthStatus.HEALTHY;
    }

    /**
     * Checks if the dashboard synchronization is paused.
     *
     * @return true if sync is paused, false otherwise
     */
    public boolean isSyncPaused() {
        return syncStatus == SyncStatus.PAUSED;
    }

    /**
     * Checks if the dashboard has been calculated at least once.
     *
     * @return true if calculated, false otherwise
     */
    public boolean hasBeenCalculated() {
        return lastCalculationTime != null;
    }

    /**
     * Checks if the dashboard has been read at least once.
     *
     * @return true if read, false otherwise
     */
    public boolean hasBeenRead() {
        return lastReadTime != null;
    }

    /**
     * Returns a specific error message.
     *
     * @param errorKey the error key to get message for
     * @return the error message, or null if no error for this key
     */
    public String getError(String errorKey) {
        return errors.get(errorKey);
    }

    /**
     * Checks if a specific error exists.
     *
     * @param errorKey the error key to check
     * @return true if the error exists, false otherwise
     */
    public boolean hasError(String errorKey) {
        return errors.containsKey(errorKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardStatus that = (DashboardStatus) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(lastCalculationTime, that.lastCalculationTime) &&
                Objects.equals(lastReadTime, that.lastReadTime) &&
                syncStatus == that.syncStatus &&
                healthStatus == that.healthStatus &&
                Objects.equals(errors, that.errors);
    }

    @Override
    public String toString() {
        return "DashboardStatus{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lastCalculationTime=" + lastCalculationTime +
                ", lastReadTime=" + lastReadTime +
                ", syncStatus=" + syncStatus +
                ", healthStatus=" + healthStatus +
                ", errors=" + errors +
                '}';
    }
}