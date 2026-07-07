package com.thy.fss.common.inmemory.exception;

/**
 * Base exception class for all InMemoryDataStore related exceptions.
 * This is a runtime exception that serves as the parent for all specific
 * exception types in the library.
 */
public class InMemoryDataStoreException extends RuntimeException {

    /**
     * Constructs a new InMemoryDataStoreException with the specified detail message.
     *
     * @param message the detail message explaining the exception
     */
    public InMemoryDataStoreException(String message) {
        super(message);
    }

    /**
     * Constructs a new InMemoryDataStoreException with the specified detail message
     * and cause. This constructor is useful for exception chaining and preserving
     * the original stack trace.
     *
     * @param message the detail message explaining the exception
     * @param cause   the cause of this exception (which is saved for later retrieval
     *                by the {@link #getCause()} method)
     */
    public InMemoryDataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}