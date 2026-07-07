package com.thy.fss.common.inmemory.engine.exception;

/**
 * Exception thrown when a property mapping configuration is invalid.
 * This can occur due to:
 * <ul>
 *   <li>Type mismatches (e.g., aggregation on non-numeric field)</li>
 *   <li>Missing required fields (e.g., aggregation without source attribute)</li>
 *   <li>Invalid combinations (e.g., Dashboard with collection mapping)</li>
 *   <li>Null or invalid references</li>
 * </ul>
 */
public class InvalidMappingException extends DataSynchronizationException {

    private final String mappingDescription;

    /**
     * Creates a new InvalidMappingException.
     *
     * @param message the error message
     */
    public InvalidMappingException(String message) {
        super(message);
        this.mappingDescription = null;
    }

    /**
     * Creates a new InvalidMappingException with mapping description.
     *
     * @param message            the error message
     * @param mappingDescription description of the invalid mapping
     */
    public InvalidMappingException(String message, String mappingDescription) {
        super(String.format("%s - Mapping: %s", message, mappingDescription));
        this.mappingDescription = mappingDescription;
    }

    /**
     * Creates a new InvalidMappingException with a cause.
     *
     * @param message the error message
     * @param cause   the cause of the exception
     */
    public InvalidMappingException(String message, Throwable cause) {
        super(message, cause);
        this.mappingDescription = null;
    }

    /**
     * Gets the description of the invalid mapping.
     *
     * @return the mapping description, or null if not provided
     */
    public String getMappingDescription() {
        return mappingDescription;
    }
}
