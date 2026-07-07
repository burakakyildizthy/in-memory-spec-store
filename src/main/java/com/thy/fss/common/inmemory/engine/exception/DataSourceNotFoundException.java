package com.thy.fss.common.inmemory.engine.exception;

/**
 * Exception thrown when a referenced datasource is not found in the registry.
 * This typically occurs when a store or dashboard references a datasource name
 * that has not been registered with InMemorySpecStoreFactory.
 */
public class DataSourceNotFoundException extends DataSynchronizationException {

    private final String dataSourceName;

    /**
     * Creates a new DataSourceNotFoundException.
     *
     * @param dataSourceName the name of the datasource that was not found
     */
    public DataSourceNotFoundException(String dataSourceName) {
        super(String.format("Datasource not found: %s", dataSourceName));
        this.dataSourceName = dataSourceName;
    }

    /**
     * Creates a new DataSourceNotFoundException with a custom message.
     *
     * @param dataSourceName the name of the datasource that was not found
     * @param message        the custom error message
     */
    public DataSourceNotFoundException(String dataSourceName, String message) {
        super(message);
        this.dataSourceName = dataSourceName;
    }

    /**
     * Gets the name of the datasource that was not found.
     *
     * @return the datasource name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
}
