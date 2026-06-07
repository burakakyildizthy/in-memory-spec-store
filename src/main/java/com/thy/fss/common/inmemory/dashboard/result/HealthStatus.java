package com.thy.fss.common.inmemory.dashboard.result;

/**
 * Enumeration of possible health statuses for a dashboard.
 * Indicates the current state of dashboard calculation and data availability.
 */
public enum HealthStatus {

    /**
     * Dashboard is healthy with successful calculation and no errors.
     */
    HEALTHY,

    /**
     * Dashboard calculation failed or has errors.
     * Some or all fields may be null due to calculation failures.
     */
    ERROR,

    /**
     * Dashboard is currently being calculated.
     * Data may be stale or unavailable during calculation.
     */
    CALCULATING
}