package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;

import javax.lang.model.element.VariableElement;

/**
 * Interface for analyzing datetime format configuration from entity fields.
 * Extracts @JsonFormat patterns from LocalDateTime/LocalDate fields and provides
 * default patterns from FilterConstants when no custom format is specified.
 * <p>
 * This analyzer supports the following temporal types:
 * - java.time.LocalDateTime
 * - java.time.LocalDate
 * - java.time.Instant
 * <p>
 * For each field, it determines:
 * - Custom @JsonFormat pattern if present
 * - Default pattern from FilterConstants if no custom format
 * - Additional format configuration (timezone, locale)
 */
public interface DateTimeFormatAnalyzer {

    /**
     * Analyzes a datetime field to extract format configuration.
     *
     * @param field the entity field to analyze (must be a temporal type)
     * @return DateTimeFormatInfo containing format configuration, or null if field is not a temporal type
     */
    DateTimeFormatInfo analyzeDateTimeField(VariableElement field);

    /**
     * Checks if the given field is a supported temporal type.
     *
     * @param field the entity field to check
     * @return true if the field is LocalDateTime, LocalDate, or Instant
     */
    boolean isTemporalField(VariableElement field);

    /**
     * Checks if the given field has a custom @JsonFormat annotation.
     *
     * @param field the entity field to check
     * @return true if the field has @JsonFormat with a custom pattern
     */
    boolean hasCustomDateTimeFormat(VariableElement field);

    /**
     * Extracts the @JsonFormat pattern from a field if present.
     *
     * @param field the entity field to analyze
     * @return the custom pattern string, or null if no @JsonFormat annotation found
     */
    String extractCustomPattern(VariableElement field);

    /**
     * Gets the default pattern for a temporal field type using FilterConstants.
     *
     * @param fieldType the Java type name (e.g., "java.time.LocalDateTime")
     * @return the default pattern string for the type
     */
    String getDefaultPatternForType(String fieldType);

    /**
     * Determines the field type from a VariableElement.
     *
     * @param field the entity field to analyze
     * @return the fully qualified type name (e.g., "java.time.LocalDateTime")
     */
    String getFieldType(VariableElement field);
}