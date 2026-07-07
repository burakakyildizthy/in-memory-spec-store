package com.thy.fss.common.inmemory.exception;

/**
 * Base exception for all DataSource-related errors in the in-memory data library.
 * This is a runtime exception that serves as the parent for all DataSource-specific exceptions.
 */
public class DataSourceException extends RuntimeException {

    /**
     * Constructs a new DataSourceException with the specified detail message.
     *
     * @param message the detail message explaining the exception
     */
    public DataSourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataSourceException with the specified detail message and cause.
     *
     * @param message the detail message explaining the exception
     * @param cause   the cause of this exception (which is saved for later retrieval)
     */
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DataSourceException with the specified cause.
     *
     * @param cause the cause of this exception (which is saved for later retrieval)
     */
    public DataSourceException(Throwable cause) {
        super(cause);
    }
}