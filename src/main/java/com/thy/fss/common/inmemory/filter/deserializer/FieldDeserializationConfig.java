package com.thy.fss.common.inmemory.filter.deserializer;

import java.time.format.DateTimeFormatter;

/**
 * Configuration class that holds type-specific deserialization settings.
 * Used to provide context for deserializing filter field values from strings.
 */
public class FieldDeserializationConfig {

    private DateTimeFormatter dateTimeFormatter;
    private EnumDeserializationInfo enumInfo;
    private String customPattern;
    private boolean hasCustomFormat;
    private Class<?> targetType;

    /**
     * Default constructor.
     */
    public FieldDeserializationConfig() {
        // Noncompliant - method is empty
    }

    /**
     * Gets the DateTimeFormatter for temporal type deserialization.
     *
     * @return The DateTimeFormatter, or null if not applicable
     */
    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    /**
     * Sets the DateTimeFormatter for temporal type deserialization.
     *
     * @param dateTimeFormatter The DateTimeFormatter to use
     * @return This config instance for method chaining
     */
    public FieldDeserializationConfig setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
        return this;
    }

    /**
     * Gets the enum deserialization information.
     *
     * @return The EnumDeserializationInfo, or null if not applicable
     */
    public EnumDeserializationInfo getEnumInfo() {
        return enumInfo;
    }

    /**
     * Sets the enum deserialization information.
     *
     * @param enumInfo The EnumDeserializationInfo to use
     * @return This config instance for method chaining
     */
    public FieldDeserializationConfig setEnumInfo(EnumDeserializationInfo enumInfo) {
        this.enumInfo = enumInfo;
        return this;
    }

    /**
     * Gets the custom pattern string.
     *
     * @return The custom pattern, or null if not applicable
     */
    public String getCustomPattern() {
        return customPattern;
    }

    /**
     * Sets the custom pattern string.
     *
     * @param customPattern The custom pattern to use
     * @return This config instance for method chaining
     */
    public FieldDeserializationConfig setCustomPattern(String customPattern) {
        this.customPattern = customPattern;
        return this;
    }

    /**
     * Checks if a custom format is configured.
     *
     * @return True if custom format is configured, false otherwise
     */
    public boolean hasCustomFormat() {
        return hasCustomFormat;
    }

    /**
     * Sets whether a custom format is configured.
     *
     * @param hasCustomFormat True if custom format is configured
     * @return This config instance for method chaining
     */
    public FieldDeserializationConfig setHasCustomFormat(boolean hasCustomFormat) {
        this.hasCustomFormat = hasCustomFormat;
        return this;
    }

    /**
     * Gets the target type for deserialization.
     *
     * @return The target type class
     */
    public Class<?> getTargetType() {
        return targetType;
    }

    /**
     * Sets the target type for deserialization.
     *
     * @param targetType The target type class
     * @return This config instance for method chaining
     */
    public FieldDeserializationConfig setTargetType(Class<?> targetType) {
        this.targetType = targetType;
        return this;
    }
}
