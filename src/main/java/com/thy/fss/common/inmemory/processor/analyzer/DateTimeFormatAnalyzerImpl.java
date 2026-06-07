package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of DateTimeFormatAnalyzer that extracts datetime format configuration
 * from entity fields using annotation processing APIs.
 * <p>
 * This analyzer:
 * 1. Identifies temporal fields (LocalDateTime, LocalDate, Instant)
 * 2. Extracts @JsonFormat patterns when present
 * 3. Provides default patterns from FilterConstants when no custom format specified
 * 4. Handles additional format configuration (timezone, locale)
 * <p>
 * The analysis results are used to generate matching deserializers for filter fields
 * that replicate the exact datetime parsing behavior of entity fields.
 */
public class DateTimeFormatAnalyzerImpl implements DateTimeFormatAnalyzer {
    
    private static final String LOCAL_DATE_TIME_TYPE = "java.time.LocalDateTime";
    private static final String LOCAL_DATE_TYPE = "java.time.LocalDate";
    private static final String INSTANT_TYPE = "java.time.Instant";

    // Set of supported temporal types for datetime format analysis
    private static final Set<String> SUPPORTED_TEMPORAL_TYPES = Set.of(
            LOCAL_DATE_TIME_TYPE,
            LOCAL_DATE_TYPE,
            INSTANT_TYPE
    );

    @Override
    public DateTimeFormatInfo analyzeDateTimeField(VariableElement field) {
        if (field == null || !isTemporalField(field)) {
            return null;
        }

        String fieldType = getFieldType(field);
        DateTimeFormatInfo info = new DateTimeFormatInfo(fieldType);

        // Check for @JsonFormat annotation
        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && !jsonFormat.pattern().isEmpty() && !jsonFormat.pattern().isBlank()) {
            // Custom format found
            info.setPattern(jsonFormat.pattern());
            info.setHasCustomFormat(true);

            // Extract additional format configuration
            if (!jsonFormat.timezone().isEmpty()) {
                info.setTimezone(jsonFormat.timezone());
            }

            if (!jsonFormat.locale().isEmpty()) {
                info.setLocale(jsonFormat.locale());
            }
        } else {
            // No custom format - use default from FilterConstants
            String defaultPattern = getDefaultPatternForType(fieldType);
            info.setPattern(defaultPattern);
            info.setHasCustomFormat(false);
        }

        return info;
    }

    @Override
    public boolean isTemporalField(VariableElement field) {
        if (field == null) {
            return false;
        }

        String fieldType = getFieldType(field);
        return SUPPORTED_TEMPORAL_TYPES.contains(fieldType);
    }

    @Override
    public boolean hasCustomDateTimeFormat(VariableElement field) {
        if (field == null) {
            return false;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        return jsonFormat != null && !jsonFormat.pattern().isEmpty() && !jsonFormat.pattern().isBlank();
    }

    @Override
    public String extractCustomPattern(VariableElement field) {
        if (field == null) {
            return null;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && !jsonFormat.pattern().isEmpty() && !jsonFormat.pattern().isBlank()) {
            return jsonFormat.pattern();
        }

        return null;
    }

    @Override
    public String getDefaultPatternForType(String fieldType) {
        if (fieldType == null) {
            return FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN;
        }

        return switch (fieldType) {
            case LOCAL_DATE_TIME_TYPE -> FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN;
            case LOCAL_DATE_TYPE -> FilterConstants.DEFAULT_LOCAL_DATE_PATTERN;
            case INSTANT_TYPE -> FilterConstants.DEFAULT_INSTANT_PATTERN;
            default -> FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN;
        };
    }

    @Override
    public String getFieldType(VariableElement field) {
        if (field == null) {
            return null;
        }

        TypeMirror typeMirror = field.asType();
        return typeMirror.toString();
    }

    /**
     * Extracts @JsonFormat annotation information using annotation processing APIs.
     * This method provides more detailed extraction than the simple annotation access,
     * useful for complex annotation parameter handling.
     *
     * @param field the entity field to analyze
     * @return DateTimeFormatInfo with detailed annotation information, or null if no @JsonFormat found
     */
    public DateTimeFormatInfo extractDetailedJsonFormat(VariableElement field) {
        if (field == null || !isTemporalField(field)) {
            return null;
        }

        String fieldType = getFieldType(field);

        // Look for @JsonFormat annotation mirror for detailed extraction
        for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
            if (JsonFormat.class.getName().equals(mirror.getAnnotationType().toString())) {
                return extractFromAnnotationMirror(mirror, fieldType);
            }
        }

        // No @JsonFormat found - return default configuration
        return new DateTimeFormatInfo(fieldType);
    }

    /**
     * Extracts DateTimeFormatInfo from a @JsonFormat AnnotationMirror.
     * This provides access to all annotation parameters including defaults.
     */
    private DateTimeFormatInfo extractFromAnnotationMirror(AnnotationMirror mirror, String fieldType) {
        DateTimeFormatInfo info = new DateTimeFormatInfo(fieldType);

        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            String paramName = entry.getKey().getSimpleName().toString();
            Object paramValue = entry.getValue().getValue();

            switch (paramName) {
                case "pattern" -> handlePatternParameter(paramValue, info);
                case "timezone" -> handleTimezoneParameter(paramValue, info);
                case "locale" -> handleLocaleParameter(paramValue, info);
                default -> {
                    //do nothing
                }
                // Additional parameters can be handled here if needed
                // "shape"
                //"lenient"
            }
        }

        // If no custom pattern was found, ensure we have the default
        if (!info.isHasCustomFormat()) {
            String defaultPattern = getDefaultPatternForType(fieldType);
            info.setPattern(defaultPattern);
        }

        return info;
    }

    /**
     * Handles the 'pattern' parameter from @JsonFormat annotation.
     */
    private void handlePatternParameter(Object paramValue, DateTimeFormatInfo info) {
        if (paramValue instanceof String pattern && !pattern.isEmpty()) {
            info.setPattern(pattern);
            info.setHasCustomFormat(true);
        }
    }

    /**
     * Handles the 'timezone' parameter from @JsonFormat annotation.
     */
    private void handleTimezoneParameter(Object paramValue, DateTimeFormatInfo info) {
        if (paramValue instanceof String timezone && !timezone.isEmpty()) {
            info.setTimezone(timezone);
        }
    }

    /**
     * Handles the 'locale' parameter from @JsonFormat annotation.
     */
    private void handleLocaleParameter(Object paramValue, DateTimeFormatInfo info) {
        if (paramValue instanceof String locale && !locale.isEmpty()) {
            info.setLocale(locale);
        }
    }


    /**
     * Validates that a DateTimeFormatInfo has a valid pattern.
     *
     * @param info the DateTimeFormatInfo to validate
     * @return true if the info has a non-null, non-empty pattern
     */
    public boolean isValidDateTimeFormatInfo(DateTimeFormatInfo info) {
        return info != null &&
                info.getPattern() != null &&
                !info.getPattern().trim().isEmpty();
    }

    /**
     * Creates a DateTimeFormatInfo for testing purposes with a custom pattern.
     *
     * @param fieldType     the Java type name
     * @param customPattern the custom datetime pattern
     * @return DateTimeFormatInfo configured with the custom pattern
     */
    public DateTimeFormatInfo createCustomFormatInfo(String fieldType, String customPattern) {
        return DateTimeFormatInfo.withCustomPattern(customPattern, fieldType);
    }

    /**
     * Creates a DateTimeFormatInfo for testing purposes with default pattern.
     *
     * @param fieldType the Java type name
     * @return DateTimeFormatInfo configured with the default pattern for the type
     */
    public DateTimeFormatInfo createDefaultFormatInfo(String fieldType) {
        return DateTimeFormatInfo.forFieldType(fieldType);
    }
}