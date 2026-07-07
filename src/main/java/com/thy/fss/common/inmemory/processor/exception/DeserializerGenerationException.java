package com.thy.fss.common.inmemory.processor.exception;

/**
 * Exception thrown when deserializer code generation fails.
 * Provides detailed error information for troubleshooting generation issues.
 */
public class DeserializerGenerationException extends ProcessingException {

    private final String filterClassName;
    private final String deserializerClassName;
    private final String generationPhase;

    /**
     * Constructs a new deserializer generation exception.
     *
     * @param filterClassName       the name of the filter class being processed
     * @param deserializerClassName the name of the deserializer being generated
     * @param generationPhase       the phase of generation where the error occurred
     * @param message               the error message
     */
    public DeserializerGenerationException(String filterClassName, String deserializerClassName,
                                           String generationPhase, String message) {
        super(buildMessage(filterClassName, deserializerClassName, generationPhase, message));
        this.filterClassName = filterClassName;
        this.deserializerClassName = deserializerClassName;
        this.generationPhase = generationPhase;
    }

    /**
     * Constructs a new deserializer generation exception with a cause.
     *
     * @param filterClassName       the name of the filter class being processed
     * @param deserializerClassName the name of the deserializer being generated
     * @param generationPhase       the phase of generation where the error occurred
     * @param message               the error message
     * @param cause                 the underlying cause
     */
    public DeserializerGenerationException(String filterClassName, String deserializerClassName,
                                           String generationPhase, String message, Throwable cause) {
        super(buildMessage(filterClassName, deserializerClassName, generationPhase, message), cause);
        this.filterClassName = filterClassName;
        this.deserializerClassName = deserializerClassName;
        this.generationPhase = generationPhase;
    }

    /**
     * Builds a comprehensive error message from the generation details.
     */
    private static String buildMessage(String filterClassName, String deserializerClassName,
                                       String generationPhase, String message) {
        return String.format("Deserializer generation failed for filter '%s' (deserializer: '%s') during phase '%s': %s",
                filterClassName, deserializerClassName, generationPhase, message);
    }

    /**
     * Gets the name of the filter class being processed.
     *
     * @return the filter class name
     */
    public String getFilterClassName() {
        return filterClassName;
    }

    /**
     * Gets the name of the deserializer being generated.
     *
     * @return the deserializer class name
     */
    public String getDeserializerClassName() {
        return deserializerClassName;
    }

    /**
     * Gets the phase of generation where the error occurred.
     *
     * @return the generation phase
     */
    public String getGenerationPhase() {
        return generationPhase;
    }
}