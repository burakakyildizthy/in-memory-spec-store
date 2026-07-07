package com.thy.fss.common.inmemory.processor.model;

import com.thy.fss.common.inmemory.filter.FilterConstants;

import java.util.Objects;

/**
 * Data model representing datetime format configuration extracted from build-time analysis.
 * Contains information about @JsonFormat patterns and default format patterns for temporal fields.
 */
public class DateTimeFormatInfo {

    private String pattern;
    private boolean hasCustomFormat;
    private String fieldType;
    private String timezone;
    private String locale;

    public DateTimeFormatInfo() {
    }

    public DateTimeFormatInfo(String fieldType) {
        this.fieldType = fieldType;
        this.hasCustomFormat = false;
        setDefaultPatternForType(fieldType);
    }

    public DateTimeFormatInfo(String pattern, String fieldType) {
        this.pattern = pattern;
        this.fieldType = fieldType;
        this.hasCustomFormat = true;
    }

    /**
     * Creates a DateTimeFormatInfo for a field type with default pattern.
     *
     * @param fieldType the Java type name
     * @return DateTimeFormatInfo with default pattern for the type
     */
    public static DateTimeFormatInfo forFieldType(String fieldType) {
        return new DateTimeFormatInfo(fieldType);
    }

    /**
     * Creates a DateTimeFormatInfo for a field with custom pattern.
     *
     * @param pattern   custom datetime pattern
     * @param fieldType the Java type name
     * @return DateTimeFormatInfo with custom pattern
     */
    public static DateTimeFormatInfo withCustomPattern(String pattern, String fieldType) {
        return new DateTimeFormatInfo(pattern, fieldType);
    }

    /**
     * Creates a DateTimeFormatInfo for a field with default pattern.
     *
     * @param fieldType the Java type name
     * @return DateTimeFormatInfo with default pattern for the type
     */
    public static DateTimeFormatInfo withDefaultPattern(String fieldType) {
        return new DateTimeFormatInfo(fieldType);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isHasCustomFormat() {
        return hasCustomFormat;
    }

    public void setHasCustomFormat(boolean hasCustomFormat) {
        this.hasCustomFormat = hasCustomFormat;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
        if (!hasCustomFormat) {
            setDefaultPatternForType(fieldType);
        }
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Sets the default pattern based on the field type using FilterConstants.
     *
     * @param fieldType the Java type name (e.g., "java.time.LocalDateTime")
     */
    private void setDefaultPatternForType(String fieldType) {
        if (fieldType == null) {
            return;
        }

        switch (fieldType) {
            case "java.time.LocalDateTime" -> this.pattern = FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN;
            case "java.time.LocalDate" -> this.pattern = FilterConstants.DEFAULT_LOCAL_DATE_PATTERN;
            case "java.time.Instant" -> this.pattern = FilterConstants.DEFAULT_INSTANT_PATTERN;
            default -> this.pattern = FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN;
        }
    }

    /**
     * Checks if this datetime field uses a custom format pattern.
     *
     * @return true if @JsonFormat annotation with custom pattern is present
     */
    public boolean usesCustomFormat() {
        return hasCustomFormat;
    }

    /**
     * Gets the effective pattern to use for parsing, either custom or default.
     *
     * @return the datetime pattern string
     */
    public String getEffectivePattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateTimeFormatInfo that = (DateTimeFormatInfo) o;
        return hasCustomFormat == that.hasCustomFormat &&
                Objects.equals(pattern, that.pattern) &&
                Objects.equals(fieldType, that.fieldType) &&
                Objects.equals(timezone, that.timezone) &&
                Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, hasCustomFormat, fieldType, timezone, locale);
    }

    @Override
    public String toString() {
        return "DateTimeFormatInfo{" +
                "pattern='" + pattern + '\'' +
                ", hasCustomFormat=" + hasCustomFormat +
                ", fieldType='" + fieldType + '\'' +
                ", timezone='" + timezone + '\'' +
                ", locale='" + locale + '\'' +
                '}';
    }
}