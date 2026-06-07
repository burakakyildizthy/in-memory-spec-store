package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.model.InstantFormatInfo;

import javax.lang.model.element.VariableElement;

/**
 * Interface for analyzing Instant format configuration from entity fields.
 * Extracts @JsonFormat patterns from java.time.Instant fields and provides
 * default patterns from FilterConstants when no custom format is specified.
 * <p>
 * This analyzer is specifically designed for java.time.Instant fields which have
 * unique serialization/deserialization requirements:
 * - Support for timestamp (numeric) format
 * - Timezone handling for UTC conversion
 * - ISO-8601 format with milliseconds and timezone offset
 * - Shape configuration (STRING vs NUMBER)
 * <p>
 * The analyzer determines:
 * - Custom @JsonFormat pattern if present
 * - Default Instant pattern from FilterConstants if no custom format
 * - Timestamp format preference based on shape configuration
 * - Timezone configuration for proper UTC handling
 */
public interface InstantFormatAnalyzer {

    /**
     * Analyzes an Instant field to extract format configuration.
     *
     * @param field the entity field to analyze (must be java.time.Instant)
     * @return InstantFormatInfo containing format configuration, or null if field is not Instant
     */
    InstantFormatInfo analyzeInstantField(VariableElement field);

    /**
     * Checks if the given field is a java.time.Instant type.
     *
     * @param field the entity field to check
     * @return true if the field is java.time.Instant
     */
    boolean isInstantField(VariableElement field);

    /**
     * Checks if the given field has a custom @JsonFormat annotation.
     *
     * @param field the entity field to check
     * @return true if the field has @JsonFormat with a custom pattern or shape
     */
    boolean hasCustomInstantFormat(VariableElement field);

    /**
     * Extracts the @JsonFormat pattern from an Instant field if present.
     *
     * @param field the entity field to analyze
     * @return the custom pattern string, or null if no @JsonFormat annotation found
     */
    String extractCustomInstantPattern(VariableElement field);

    /**
     * Extracts the @JsonFormat shape from an Instant field if present.
     *
     * @param field the entity field to analyze
     * @return the shape string (STRING or NUMBER), or null if not specified
     */
    String extractInstantShape(VariableElement field);

    /**
     * Extracts the @JsonFormat timezone from an Instant field if present.
     *
     * @param field the entity field to analyze
     * @return the timezone string, or null if not specified
     */
    String extractInstantTimezone(VariableElement field);

    /**
     * Checks if the Instant field should use timestamp (numeric) format.
     * This is determined by @JsonFormat shape=NUMBER or specific patterns.
     *
     * @param field the entity field to check
     * @return true if timestamp format should be used
     */
    boolean shouldUseTimestampFormat(VariableElement field);

    /**
     * Gets the default pattern for Instant fields using FilterConstants.
     *
     * @return the default Instant pattern string
     */
    String getDefaultInstantPattern();

    /**
     * Creates an InstantFormatInfo with default configuration for Instant fields.
     *
     * @return InstantFormatInfo with default Instant pattern and configuration
     */
    InstantFormatInfo createDefaultInstantFormatInfo();

    /**
     * Creates an InstantFormatInfo with custom pattern configuration.
     *
     * @param customPattern the custom datetime pattern
     * @return InstantFormatInfo with custom pattern configuration
     */
    InstantFormatInfo createCustomInstantFormatInfo(String customPattern);

    /**
     * Creates an InstantFormatInfo configured for timestamp serialization.
     *
     * @return InstantFormatInfo configured for numeric timestamp format
     */
    InstantFormatInfo createTimestampFormatInfo();
}