package com.thy.fss.common.inmemory.processor.model;

import com.thy.fss.common.inmemory.filter.FilterConstants;

import java.util.Objects;

/**
 * Data model representing Instant format configuration extracted from build-time analysis.
 * Contains information about @JsonFormat patterns and default format patterns for Instant fields.
 * <p>
 * This class is specifically designed for java.time.Instant fields which have unique
 * serialization/deserialization requirements compared to LocalDateTime and LocalDate.
 */
public class InstantFormatInfo {

    private String pattern;
    private boolean hasCustomFormat;
    private String timezone;
    private String locale;
    private boolean useTimestamp;
    private String shape;

    public InstantFormatInfo() {
        this.hasCustomFormat = false;
        this.pattern = FilterConstants.DEFAULT_INSTANT_PATTERN;
        this.useTimestamp = false;
    }

    public InstantFormatInfo(String customPattern) {
        this.pattern = customPattern;
        this.hasCustomFormat = true;
        this.useTimestamp = false;
    }

    /**
     * Creates an InstantFormatInfo with default pattern.
     *
     * @return InstantFormatInfo with default Instant pattern
     */
    public static InstantFormatInfo withDefaultPattern() {
        return new InstantFormatInfo();
    }

    /**
     * Creates an InstantFormatInfo with custom pattern.
     *
     * @param pattern custom datetime pattern
     * @return InstantFormatInfo with custom pattern
     */
    public static InstantFormatInfo withCustomPattern(String pattern) {
        return new InstantFormatInfo(pattern);
    }

    /**
     * Creates an InstantFormatInfo configured for timestamp serialization.
     *
     * @return InstantFormatInfo configured for timestamp format
     */
    public static InstantFormatInfo withTimestampFormat() {
        InstantFormatInfo info = new InstantFormatInfo();
        info.setUseTimestamp(true);
        info.setShape("NUMBER");
        return info;
    }

    /**
     * Creates an InstantFormatInfo with timezone configuration.
     *
     * @param timezone the timezone to use
     * @return InstantFormatInfo with timezone configuration
     */
    public static InstantFormatInfo withTimezone(String timezone) {
        InstantFormatInfo info = new InstantFormatInfo();
        info.setTimezone(timezone);
        return info;
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

    public boolean isUseTimestamp() {
        return useTimestamp;
    }

    public void setUseTimestamp(boolean useTimestamp) {
        this.useTimestamp = useTimestamp;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    /**
     * Checks if this Instant field uses a custom format pattern.
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
        return pattern != null && !pattern.trim().isEmpty() ? pattern : FilterConstants.DEFAULT_INSTANT_PATTERN;
    }

    /**
     * Checks if this Instant field should be serialized as timestamp.
     *
     * @return true if timestamp format should be used
     */
    public boolean shouldUseTimestamp() {
        return useTimestamp || "NUMBER".equals(shape);
    }

    /**
     * Gets the effective timezone to use, defaulting to UTC if not specified.
     *
     * @return the timezone string, defaults to "UTC"
     */
    public String getEffectiveTimezone() {
        return timezone != null && !timezone.isEmpty() ? timezone : "UTC";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstantFormatInfo that = (InstantFormatInfo) o;
        return hasCustomFormat == that.hasCustomFormat &&
                useTimestamp == that.useTimestamp &&
                Objects.equals(pattern, that.pattern) &&
                Objects.equals(timezone, that.timezone) &&
                Objects.equals(locale, that.locale) &&
                Objects.equals(shape, that.shape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, hasCustomFormat, timezone, locale, useTimestamp, shape);
    }

    @Override
    public String toString() {
        return "InstantFormatInfo{" +
                "pattern='" + pattern + '\'' +
                ", hasCustomFormat=" + hasCustomFormat +
                ", timezone='" + timezone + '\'' +
                ", locale='" + locale + '\'' +
                ", useTimestamp=" + useTimestamp +
                ", shape='" + shape + '\'' +
                '}';
    }
}