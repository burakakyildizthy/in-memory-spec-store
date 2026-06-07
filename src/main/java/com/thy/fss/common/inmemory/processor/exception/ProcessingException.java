package com.thy.fss.common.inmemory.processor.exception;

/**
 * Exception thrown during annotation processing when errors occur.
 * This exception is used to signal processing failures that should
 * stop the compilation process.
 */
public class ProcessingException extends Exception {

    /**
     * Constructs a new processing exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new processing exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new processing exception with the specified cause.
     *
     * @param cause the cause
     */
    public ProcessingException(Throwable cause) {
        super(cause);
    }
}