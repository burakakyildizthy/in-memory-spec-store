package com.thy.fss.common.inmemory.engine.exception;

/**
 * Base exception for all data synchronization related errors.
 * This is the parent exception for all synchronization engine exceptions.
 */
public class DataSynchronizationException extends RuntimeException {

    /**
     * Creates a new DataSynchronizationException with the specified message.
     *
     * @param message the error message
     */
    public DataSynchronizationException(String message) {
        super(message);
    }

    /**
     * Creates a new DataSynchronizationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the cause of the exception
     */
    public DataSynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new DataSynchronizationException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public DataSynchronizationException(Throwable cause) {
        super(cause);
    }
}
