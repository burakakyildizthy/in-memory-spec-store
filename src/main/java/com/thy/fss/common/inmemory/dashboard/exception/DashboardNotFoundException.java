package com.thy.fss.common.inmemory.dashboard.exception;

/**
 * Exception thrown when a requested dashboard cannot be found.
 * This exception is used to indicate that a dashboard with the specified
 * identifier does not exist in the dashboard registry.
 */
public class DashboardNotFoundException extends RuntimeException {

    private final String dashboardId;

    /**
     * Creates a new DashboardNotFoundException with the specified dashboard ID.
     *
     * @param dashboardId the ID of the dashboard that was not found
     */
    public DashboardNotFoundException(String dashboardId) {
        super("Dashboard not found with ID: " + dashboardId);
        this.dashboardId = dashboardId;
    }

    /**
     * Creates a new DashboardNotFoundException with the specified dashboard ID and message.
     *
     * @param dashboardId the ID of the dashboard that was not found
     * @param message     the detail message
     */
    public DashboardNotFoundException(String dashboardId, String message) {
        super(message);
        this.dashboardId = dashboardId;
    }

    /**
     * Creates a new DashboardNotFoundException with the specified dashboard ID, message, and cause.
     *
     * @param dashboardId the ID of the dashboard that was not found
     * @param message     the detail message
     * @param cause       the cause of the exception
     */
    public DashboardNotFoundException(String dashboardId, String message, Throwable cause) {
        super(message, cause);
        this.dashboardId = dashboardId;
    }

    /**
     * Returns the ID of the dashboard that was not found.
     *
     * @return the dashboard ID
     */
    public String getDashboardId() {
        return dashboardId;
    }
}