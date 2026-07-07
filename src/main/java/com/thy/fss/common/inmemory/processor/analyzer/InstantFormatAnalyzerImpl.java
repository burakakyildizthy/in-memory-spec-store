package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.InstantFormatInfo;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

/**
 * Implementation of InstantFormatAnalyzer that extracts Instant format configuration
 * from entity fields using annotation processing APIs.
 * <p>
 * This analyzer specifically handles java.time.Instant fields which have unique requirements:
 * 1. Identifies Instant fields (java.time.Instant)
 * 2. Extracts @JsonFormat patterns, shape, and timezone when present
 * 3. Provides default Instant pattern from FilterConstants when no custom format specified
 * 4. Handles timestamp (numeric) format based on shape configuration
 * 5. Manages timezone configuration for proper UTC handling
 * <p>
 * The analysis results are used to generate matching deserializers for InstantFilter fields
 * that replicate the exact datetime parsing behavior of entity Instant fields.
 */
public class InstantFormatAnalyzerImpl implements InstantFormatAnalyzer {

    // Instant type name for field type checking
    private static final String INSTANT_TYPE = "java.time.Instant";

    @Override
    public InstantFormatInfo analyzeInstantField(VariableElement field) {
        if (field == null || !isInstantField(field)) {
            return null;
        }

        InstantFormatInfo info = new InstantFormatInfo();

        // Check for @JsonFormat annotation
        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null) {
            // Extract pattern if present
            if (!jsonFormat.pattern().isEmpty()) {
                info.setPattern(jsonFormat.pattern());
                info.setHasCustomFormat(true);
            }

            // Extract shape configuration
            if (jsonFormat.shape() != JsonFormat.Shape.ANY) {
                String shapeValue = jsonFormat.shape().name();
                info.setShape(shapeValue);

                // Check if timestamp format should be used
                if (JsonFormat.Shape.NUMBER.equals(jsonFormat.shape())) {
                    info.setUseTimestamp(true);
                }
            }

            // Extract timezone if present
            if (!jsonFormat.timezone().isEmpty()) {
                info.setTimezone(jsonFormat.timezone());
            }

            // Extract locale if present
            if (!jsonFormat.locale().isEmpty()) {
                info.setLocale(jsonFormat.locale());
            }
        }

        // If no custom format was found, ensure we have the default
        if (!info.isHasCustomFormat()) {
            info.setPattern(FilterConstants.DEFAULT_INSTANT_PATTERN);
        }

        return info;
    }

    @Override
    public boolean isInstantField(VariableElement field) {
        if (field == null) {
            return false;
        }

        TypeMirror typeMirror = field.asType();
        return INSTANT_TYPE.equals(typeMirror.toString());
    }

    @Override
    public boolean hasCustomInstantFormat(VariableElement field) {
        if (field == null) {
            return false;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        return jsonFormat != null &&
                (!jsonFormat.pattern().isEmpty() || jsonFormat.shape() != JsonFormat.Shape.ANY);
    }

    @Override
    public String extractCustomInstantPattern(VariableElement field) {
        if (field == null) {
            return null;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && !jsonFormat.pattern().isEmpty()) {
            return jsonFormat.pattern();
        }

        return null;
    }

    @Override
    public String extractInstantShape(VariableElement field) {
        if (field == null) {
            return null;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && jsonFormat.shape() != JsonFormat.Shape.ANY) {
            return jsonFormat.shape().name();
        }

        return null;
    }

    @Override
    public String extractInstantTimezone(VariableElement field) {
        if (field == null) {
            return null;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null && !jsonFormat.timezone().isEmpty()) {
            return jsonFormat.timezone();
        }

        return null;
    }

    @Override
    public boolean shouldUseTimestampFormat(VariableElement field) {
        if (field == null) {
            return false;
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null) {
            // Check if shape is explicitly set to NUMBER
            if (JsonFormat.Shape.NUMBER.equals(jsonFormat.shape())) {
                return true;
            }

            // Check if pattern suggests timestamp format
            String pattern = jsonFormat.pattern();
            return pattern != null && (pattern.contains("timestamp") || pattern.contains("epoch"));
        }

        return false;
    }

    @Override
    public String getDefaultInstantPattern() {
        return FilterConstants.DEFAULT_INSTANT_PATTERN;
    }

    @Override
    public InstantFormatInfo createDefaultInstantFormatInfo() {
        return InstantFormatInfo.withDefaultPattern();
    }

    @Override
    public InstantFormatInfo createCustomInstantFormatInfo(String customPattern) {
        return InstantFormatInfo.withCustomPattern(customPattern);
    }

    @Override
    public InstantFormatInfo createTimestampFormatInfo() {
        return InstantFormatInfo.withTimestampFormat();
    }

    /**
     * Extracts @JsonFormat annotation information using annotation processing APIs.
     * This method provides more detailed extraction than the simple annotation access,
     * useful for complex annotation parameter handling.
     *
     * @param field the entity field to analyze
     * @return InstantFormatInfo with detailed annotation information, or null if no @JsonFormat found
     */
    public InstantFormatInfo extractDetailedJsonFormat(VariableElement field) {
        if (field == null || !isInstantField(field)) {
            return null;
        }

        // Look for @JsonFormat annotation mirror for detailed extraction
        for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
            if (JsonFormat.class.getName().equals(mirror.getAnnotationType().toString())) {
                return extractFromAnnotationMirror(mirror);
            }
        }

        // No @JsonFormat found - return default configuration
        return createDefaultInstantFormatInfo();
    }

    /**
     * Extracts InstantFormatInfo from a @JsonFormat AnnotationMirror.
     * This provides access to all annotation parameters including defaults.
     */
    private InstantFormatInfo extractFromAnnotationMirror(AnnotationMirror mirror) {
        InstantFormatInfo info = new InstantFormatInfo();

        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            String paramName = entry.getKey().getSimpleName().toString();
            Object paramValue = entry.getValue().getValue();

            switch (paramName) {
                case "pattern" -> handlePatternParameter(paramValue, info);
                case "shape" -> handleShapeParameter(paramValue, info);
                case "timezone" ->  handleTimezoneParameter(paramValue, info);
                case "locale" -> handleLocaleParameter(paramValue, info);
                default -> {
                    // do nothing
                }
                // Additional parameters can be handled here if needed
                //"lenient"
            }
        }

        // If no custom pattern was found, ensure we have the default
        if (!info.isHasCustomFormat()) {
            info.setPattern(FilterConstants.DEFAULT_INSTANT_PATTERN);
        }

        return info;
    }

    /**
     * Handles the 'pattern' parameter from @JsonFormat annotation.
     */
    private void handlePatternParameter(Object paramValue, InstantFormatInfo info) {
        if (paramValue instanceof String pattern && !pattern.isEmpty()) {
            info.setPattern(pattern);
            info.setHasCustomFormat(true);
        }
    }

    /**
     * Handles the 'timezone' parameter from @JsonFormat annotation.
     */
    private void handleTimezoneParameter(Object paramValue, InstantFormatInfo info) {
        if (paramValue instanceof String timezone && !timezone.isEmpty()) {
            info.setTimezone(timezone);
        }
    }

    /**
     * Handles the 'locale' parameter from @JsonFormat annotation.
     */
    private void handleLocaleParameter(Object paramValue, InstantFormatInfo info) {
        if (paramValue instanceof String locale && !locale.isEmpty()) {
            info.setLocale(locale);
        }
    }

    /**
     * Handles the 'shape' parameter from @JsonFormat annotation.
     */

    private void handleShapeParameter(Object paramValue, InstantFormatInfo info) {
        if (paramValue != null) {
            String shapeValue = paramValue.toString();
            info.setShape(shapeValue);

            // Check if timestamp format should be used
            if (shapeValue.contains("NUMBER")) {
                info.setUseTimestamp(true);
            }
        }
    }

    /**
     * Validates that an InstantFormatInfo has valid configuration.
     *
     * @param info the InstantFormatInfo to validate
     * @return true if the info has valid configuration
     */
    public boolean isValidInstantFormatInfo(InstantFormatInfo info) {
        return info != null &&
                info.getPattern() != null &&
                !info.getPattern().trim().isEmpty();
    }

    /**
     * Creates an InstantFormatInfo with timezone configuration.
     *
     * @param timezone the timezone to use
     * @return InstantFormatInfo with timezone configuration
     */
    public InstantFormatInfo createInstantFormatInfoWithTimezone(String timezone) {
        return InstantFormatInfo.withTimezone(timezone);
    }
}