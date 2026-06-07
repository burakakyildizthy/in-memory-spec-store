package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementation of AnnotationValidator that provides comprehensive validation
 * of Jackson annotations for filter field generation.
 * <p>
 * Validates annotation parameters, field type compatibility, and annotation combinations
 * to ensure proper deserializer generation and prevent runtime errors.
 */
public class AnnotationValidatorImpl implements AnnotationValidator {
    
    private static final String JSON_FORMAT = "com.fasterxml.jackson.annotation.JsonFormat";
    private static final String JSON_VALUE = "com.fasterxml.jackson.annotation.JsonValue";
    private static final String JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo";
    private static final String JSON_DESERIALIZE = "com.fasterxml.jackson.databind.annotation.JsonDeserialize";
    private static final String JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    private static final String MAY_NOT_BE_VALID = "' may not be valid - ";
    private static final String BUT_FIELD_TYPE = "but field type is ";
    private static final String JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore";

    // Supported Jackson annotation types
    private static final List<String> SUPPORTED_ANNOTATIONS = List.of(
            JsonFormat.class.getName(),
            JsonProperty.class.getName(),
            JsonCreator.class.getName(),
            JsonValue.class.getName(),
            JsonDeserialize.class.getName(),
            JsonIgnore.class.getName(),
            JsonTypeInfo.class.getName()
    );

    // Temporal field types that support @JsonFormat
    private static final Set<String> TEMPORAL_TYPES = Set.of(
            LocalDateTime.class.getName(),
            LocalDate.class.getName(),
            Instant.class.getName(),
            "java.util.Date",
            "java.sql.Date",
            "java.sql.Timestamp"
    );

    // String field types
    private static final Set<String> STRING_TYPES = Set.of(
            String.class.getName(),
            "java.lang.CharSequence"
    );

    // Numeric field types
    private static final Set<String> NUMERIC_TYPES = Set.of(
            Integer.class.getName(),
            Long.class.getName(),
            Double.class.getName(),
            Float.class.getName(),
            "int", "long", "double", "float",
            "Double",
            "java.math.BigInteger"
    );

    // Pattern for valid Java identifiers (for property names)
    private static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

    @Override
    public ValidationResult validateAnnotation(VariableElement field, AnnotationInfo annotation) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check if annotation type is supported
        if (!SUPPORTED_ANNOTATIONS.contains(annotation.getAnnotationType())) {
            errors.add("Unsupported annotation type: " + annotation.getAnnotationType());
            return new ValidationResult(false, errors, warnings);
        }

        // Check if annotation is compatible with field type
        if (!isAnnotationSupportedForFieldType(field, annotation)) {
            warnings.add("Annotation " + getSimpleAnnotationType(annotation.getAnnotationType()) +
                    " may not be applicable to field type " + field.asType().toString());
        }

        // Validate annotation parameters
        ValidationResult paramResult = validateAnnotationParameters(annotation);
        errors.addAll(paramResult.getErrors());
        warnings.addAll(paramResult.getWarnings());

        // Perform annotation-specific validation
        ValidationResult specificResult = validateSpecificAnnotation(field, annotation);
        errors.addAll(specificResult.getErrors());
        warnings.addAll(specificResult.getWarnings());

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validateAnnotationCombination(VariableElement field, List<AnnotationInfo> annotations) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (annotations == null || annotations.isEmpty()) {
            return ValidationResult.valid();
        }

        // Check for conflicting annotations
        Set<String> annotationTypes = new HashSet<>();
        for (AnnotationInfo annotation : annotations) {
            String type = annotation.getAnnotationType();
            if (annotationTypes.contains(type)) {
                errors.add("Duplicate annotation: " + getSimpleAnnotationType(type));
            }
            annotationTypes.add(type);
        }

        // Check for specific conflicting combinations
        if (annotationTypes.contains(JsonIgnore.class.getName()) && annotationTypes.size() > 1) {
            warnings.add("@JsonIgnore annotation combined with other Jackson annotations - " +
                    "other annotations will be ignored during deserialization");
        }

        if (annotationTypes.contains(JsonFormat.class.getName()) &&
                annotationTypes.contains(JsonDeserialize.class.getName())) {
            warnings.add("@JsonFormat and @JsonDeserialize annotations combined - " +
                    "custom deserializer may override format settings");
        }

        // Validate that @JsonCreator and @JsonValue are not used on regular fields
        if (annotationTypes.contains(JsonCreator.class.getName())) {
            errors.add("@JsonCreator annotation should not be used on regular fields - " +
                    "it is only valid on constructors and static factory methods");
        }

        if (annotationTypes.contains(JsonValue.class.getName()) && !isEnumType(field)) {
            warnings.add("@JsonValue annotation is typically used on enum fields or methods - " +
                    "usage on regular fields may not work as expected");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public boolean isAnnotationSupportedForFieldType(VariableElement field, AnnotationInfo annotation) {
        String annotationType = annotation.getAnnotationType();
        String fieldType = field.asType().toString();

        return switch (annotationType) {
            case JSON_FORMAT -> isTemporalType(fieldType) || isNumericType(fieldType);

            case JSON_PROPERTY -> true; // JsonProperty can be used on any field

            case JSON_IGNORE -> true; // JsonIgnore can be used on any field

            case JSON_DESERIALIZE ->
                    true; // JsonDeserialize can be used on any field

            case JSON_VALUE ->
                    isEnumType(field) || isStringType(fieldType) || isNumericType(fieldType);

            case JSON_TYPE_INFO -> isObjectType(fieldType);

            default -> false;
        };
    }

    @Override
    public List<String> getSupportedAnnotationTypes() {
        return new ArrayList<>(SUPPORTED_ANNOTATIONS);
    }

    @Override
    public ValidationResult validateAnnotationParameters(AnnotationInfo annotation) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (annotation.getParameters() == null) {
            return ValidationResult.valid();
        }

        String annotationType = annotation.getAnnotationType();
        Map<String, Object> parameters = annotation.getParameters();

        switch (annotationType) {
            case JSON_FORMAT ->
                    validateJsonFormatParameters(parameters, errors, warnings);

            case JSON_PROPERTY ->
                    validateJsonPropertyParameters(parameters, errors, warnings);

            case JSON_DESERIALIZE ->
                    validateJsonDeserializeParameters(parameters, errors);

            case JSON_TYPE_INFO ->
                    validateJsonTypeInfoParameters(parameters, errors);
            default -> throw new IllegalStateException("Unexpected value: " + annotationType);
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Validates @JsonFormat annotation parameters.
     */
    private void validateJsonFormatParameters(Map<String, Object> parameters,
                                              List<String> errors, List<String> warnings) {
        Object pattern = parameters.get("pattern");
        if (pattern != null) {
            String patternStr = pattern.toString();
            if (patternStr.trim().isEmpty()) {
                errors.add("@JsonFormat pattern cannot be empty");
            } else {
                // Validate date/time pattern
                try {
                    DateTimeFormatter.ofPattern(patternStr);
                } catch (IllegalArgumentException e) {
                    errors.add("Invalid @JsonFormat pattern '" + patternStr + "': " + e.getMessage());
                }
            }
        }

        Object timezone = parameters.get("timezone");
        if (timezone != null) {
            String timezoneStr = timezone.toString();
            if (!isValidTimezone(timezoneStr)) {
                warnings.add("Timezone '" + timezoneStr + MAY_NOT_BE_VALID +
                        "ensure it follows standard timezone format");
            }
        }

        Object locale = parameters.get("locale");
        if (locale != null) {
            String localeStr = locale.toString();
            if (!isValidLocale(localeStr)) {
                warnings.add("Locale '" + localeStr + MAY_NOT_BE_VALID +
                        "ensure it follows standard locale format (e.g., 'en_US')");
            }
        }
    }

    /**
     * Validates @JsonProperty annotation parameters.
     */
    private void validateJsonPropertyParameters(Map<String, Object> parameters,
                                                List<String> errors, List<String> warnings) {
        Object value = parameters.get("value");
        if (value != null) {
            String propertyName = value.toString();
            if (propertyName.trim().isEmpty()) {
                errors.add("@JsonProperty value cannot be empty");
            } else if (!isValidPropertyName(propertyName)) {
                warnings.add("Property name '" + propertyName + "' contains special characters - " +
                        "ensure it's valid for JSON serialization");
            }
        }

        Object access = parameters.get("access");
        if (access != null) {
            String accessStr = access.toString();
            if (!isValidAccessType(accessStr)) {
                errors.add("Invalid @JsonProperty access type: " + accessStr);
            }
        }
    }

    /**
     * Validates @JsonDeserialize annotation parameters.
     */
    private void validateJsonDeserializeParameters(Map<String, Object> parameters,
                                                   List<String> errors) {
        Object using = parameters.get("using");
        if (using != null) {
            String deserializerClass = using.toString();
            if (!isValidClassName(deserializerClass)) {
                errors.add("Invalid deserializer class name: " + deserializerClass);
            }
        }

        Object as = parameters.get("as");
        if (as != null) {
            String targetClass = as.toString();
            if (!isValidClassName(targetClass)) {
                errors.add("Invalid target class name: " + targetClass);
            }
        }
    }

    /**
     * Validates @JsonTypeInfo annotation parameters.
     */
    private void validateJsonTypeInfoParameters(Map<String, Object> parameters,
                                                List<String> errors) {
        Object use = parameters.get("use");
        if (use == null) {
            errors.add("@JsonTypeInfo requires 'use' parameter");
        }

        Object include = parameters.get("include");
        if (include != null) {
            String includeStr = include.toString();
            if (!isValidIncludeType(includeStr)) {
                errors.add("Invalid @JsonTypeInfo include type: " + includeStr);
            }
        }
    }

    /**
     * Performs annotation-specific validation beyond parameter validation.
     */
    private ValidationResult validateSpecificAnnotation(VariableElement field, AnnotationInfo annotation) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String annotationType = annotation.getAnnotationType();
        String fieldType = field.asType().toString();

        if (annotationType.equals(JSON_FORMAT)) {
            if (!isTemporalType(fieldType) && !isNumericType(fieldType)) {
                warnings.add("@JsonFormat is typically used with temporal or numeric types, " +
                        BUT_FIELD_TYPE + fieldType);
            }
        } else if (annotationType.equals(JSON_VALUE) && !isEnumType(field) && !isStringType(fieldType) && !isNumericType(fieldType)) {
                warnings.add("@JsonValue is typically used with enum, string, or numeric types, " +
                        BUT_FIELD_TYPE + fieldType);
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    // Helper methods for type checking

    private boolean isTemporalType(String fieldType) {
        return TEMPORAL_TYPES.stream().anyMatch(fieldType::contains);
    }

    private boolean isStringType(String fieldType) {
        return STRING_TYPES.stream().anyMatch(fieldType::contains);
    }

    private boolean isNumericType(String fieldType) {
        return NUMERIC_TYPES.stream().anyMatch(fieldType::contains);
    }

    private boolean isEnumType(VariableElement field) {
        TypeMirror type = field.asType();
        return type.toString().contains("enum") ||
                field.getEnclosingElement().getKind().name().equals("ENUM");
    }

    private boolean isObjectType(String fieldType) {
        return !isTemporalType(fieldType) && !isStringType(fieldType) &&
                !isNumericType(fieldType) && !fieldType.equals("boolean");
    }

    // Helper methods for parameter validation

    private boolean isValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidLocale(String locale) {
        try {
            String[] parts = locale.split("_");
            return parts.length >= 1 && parts.length <= 3 &&
                    parts[0].matches("[a-z]{2,3}");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidPropertyName(String propertyName) {
        // Allow most characters for JSON property names, but warn about special cases
        return propertyName != null && !propertyName.trim().isEmpty() &&
                !propertyName.contains("\"") && !propertyName.contains("\\");
    }

    private boolean isValidAccessType(String access) {
        return access.equals("AUTO") || access.equals("READ_ONLY") ||
                access.equals("WRITE_ONLY") || access.equals("READ_WRITE");
    }

    private boolean isValidClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }

        // Basic validation for class name format
        String[] parts = className.split("\\.");
        for (String part : parts) {
            if (!JAVA_IDENTIFIER_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIncludeType(String include) {
        return include.equals("PROPERTY") || include.equals("WRAPPER_OBJECT") ||
                include.equals("WRAPPER_ARRAY") || include.equals("EXTERNAL_PROPERTY");
    }

    private String getSimpleAnnotationType(String fullyQualifiedName) {
        if (fullyQualifiedName == null) {
            return "";
        }
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
}