package com.thy.fss.common.inmemory.exception;

/**
 * Exception thrown when there are connectivity issues with a DataSource.
 * This includes network timeouts, connection failures, authentication issues,
 * and other connectivity-related problems.
 */
public class DataSourceConnectionException extends InMemoryDataStoreException {

    private final String dataSourceName;

    /**
     * Constructs a new DataSourceConnectionException with the specified detail message.
     *
     * @param message the detail message explaining the connection issue
     */
    public DataSourceConnectionException(String message) {
        super(message);
        this.dataSourceName = null;
    }

    /**
     * Constructs a new DataSourceConnectionException with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the connection issue
     * @param cause   the underlying cause of the connection failure
     */
    public DataSourceConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.dataSourceName = null;
    }

    /**
     * Constructs a new DataSourceConnectionException with the specified detail message
     * and DataSource name for better error context.
     *
     * @param message        the detail message explaining the connection issue
     * @param dataSourceName the name of the DataSource that failed to connect
     */
    public DataSourceConnectionException(String message, String dataSourceName) {
        super(message);
        this.dataSourceName = dataSourceName;
    }

    /**
     * Constructs a new DataSourceConnectionException with the specified detail message,
     * cause, and DataSource name for complete error context.
     *
     * @param message        the detail message explaining the connection issue
     * @param cause          the underlying cause of the connection failure
     * @param dataSourceName the name of the DataSource that failed to connect
     */
    public DataSourceConnectionException(String message, Throwable cause, String dataSourceName) {
        super(message, cause);
        this.dataSourceName = dataSourceName;
    }

    /**
     * Returns the name of the DataSource that experienced the connection issue.
     *
     * @return the DataSource name, or null if not specified
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
}