package com.thy.fss.common.inmemory.engine.exception;

import java.util.List;

/**
 * Exception thrown when a circular dependency is detected in property mappings.
 * Circular dependencies can cause infinite loops and data corruption,
 * so they must be detected and prevented.
 *
 * <p>Example: A → B → C → A</p>
 */
public class CircularMappingException extends DataSynchronizationException {

    private final List<String> cycle;

    /**
     * Creates a new CircularMappingException.
     *
     * @param cycle the list of consumer IDs forming the cycle
     */
    public CircularMappingException(List<String> cycle) {
        super(String.format("Circular mapping dependency detected: %s",
                cycle != null ? String.join(" → ", cycle) : "unknown"));
        this.cycle = cycle;
    }

    /**
     * Creates a new CircularMappingException with a custom message.
     *
     * @param cycle   the list of consumer IDs forming the cycle
     * @param message the custom error message
     */
    public CircularMappingException(List<String> cycle, String message) {
        super(message);
        this.cycle = cycle;
    }

    /**
     * Gets the list of consumer IDs forming the circular dependency.
     *
     * @return the cycle path
     */
    public List<String> getCycle() {
        return cycle;
    }
}
