package com.thy.fss.common.inmemory.dashboard.result;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper class that contains dashboard data along with metadata information.
 * This class provides the calculated dashboard data along with health status,
 * version information, and error details.
 * This class is immutable and thread-safe.
 *
 * @param <T> The target class type for the dashboard data
 */
public record DashboardResult<T>(T data, LocalDateTime lastCalculationTime, HealthStatus healthStatus, long dataVersion,
                                 Map<String, String> errors) {

    /**
     * Creates a new DashboardResult with the specified parameters.
     *
     * @param data                the calculated dashboard data (can be null if calculation failed)
     * @param lastCalculationTime the timestamp of the last calculation
     * @param healthStatus        the health status of the dashboard
     * @param dataVersion         the version number of the data
     * @param errors              the map of field names to error messages (can be empty)
     * @throws IllegalArgumentException if required parameters are null
     */
    public DashboardResult(T data,
                           LocalDateTime lastCalculationTime,
                           HealthStatus healthStatus,
                           long dataVersion,
                           Map<String, String> errors) {

        // Data can be null if calculation failed
        this.data = data;

        // Validate required parameters
        this.lastCalculationTime = Objects.requireNonNull(lastCalculationTime,
                "Last calculation time cannot be null");
        this.healthStatus = Objects.requireNonNull(healthStatus,
                "Health status cannot be null");

        // Validate data version
        if (dataVersion < 0) {
            throw new IllegalArgumentException("Data version cannot be negative");
        }
        this.dataVersion = dataVersion;

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
     * Checks if the dashboard is healthy (no errors and data is available).
     *
     * @return true if the dashboard is healthy, false otherwise
     */
    public boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY && data != null && !hasErrors();
    }

    /**
     * Returns a specific error message for a field.
     *
     * @param fieldName the field name to get error for
     * @return the error message, or null if no error for this field
     */
    public String getFieldError(String fieldName) {
        return errors.get(fieldName);
    }

    /**
     * Checks if a specific field has an error.
     *
     * @param fieldName the field name to check
     * @return true if the field has an error, false otherwise
     */
    public boolean hasFieldError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardResult<?> that = (DashboardResult<?>) o;
        return dataVersion == that.dataVersion &&
                Objects.equals(data, that.data) &&
                Objects.equals(lastCalculationTime, that.lastCalculationTime) &&
                healthStatus == that.healthStatus &&
                Objects.equals(errors, that.errors);
    }

    @Override
    public String toString() {
        return "DashboardResult{" +
                "data=" + data +
                ", lastCalculationTime=" + lastCalculationTime +
                ", healthStatus=" + healthStatus +
                ", dataVersion=" + dataVersion +
                ", errors=" + errors +
                '}';
    }
}