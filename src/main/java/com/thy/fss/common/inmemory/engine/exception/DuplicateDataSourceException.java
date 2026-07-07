package com.thy.fss.common.inmemory.engine.exception;

/**
 * Exception thrown when attempting to register a datasource with a name
 * that is already registered.
 * Datasource names must be unique within the InMemorySpecStoreFactory registry.
 */
public class DuplicateDataSourceException extends DataSynchronizationException {

    private final String dataSourceName;

    /**
     * Creates a new DuplicateDataSourceException.
     *
     * @param dataSourceName the name of the duplicate datasource
     */
    public DuplicateDataSourceException(String dataSourceName) {
        super(String.format("Datasource already registered: %s", dataSourceName));
        this.dataSourceName = dataSourceName;
    }

    /**
     * Creates a new DuplicateDataSourceException with a custom message.
     *
     * @param dataSourceName the name of the duplicate datasource
     * @param message        the custom error message
     */
    public DuplicateDataSourceException(String dataSourceName, String message) {
        super(message);
        this.dataSourceName = dataSourceName;
    }

    /**
     * Gets the name of the duplicate datasource.
     *
     * @return the datasource name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
}
