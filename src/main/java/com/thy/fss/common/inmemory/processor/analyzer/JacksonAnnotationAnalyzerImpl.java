package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of JacksonAnnotationAnalyzer that extracts Jackson annotations
 * from entity fields using annotation processing APIs.
 * <p>
 * This analyzer supports the following Jackson annotations:
 * - @JsonFormat: Extracts pattern, timezone, locale, and shape parameters
 * - @JsonProperty: Extracts property name and access type
 * - @JsonCreator: Identifies creator methods and modes
 * - @JsonValue: Identifies value-based serialization
 * - @JsonDeserialize: Extracts custom deserializer information
 * - @JsonIgnore: Identifies ignored fields
 * - @JsonTypeInfo: Extracts polymorphic type information
 * <p>
 * Includes comprehensive validation of annotations and graceful error handling.
 */
public class JacksonAnnotationAnalyzerImpl implements JacksonAnnotationAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(JacksonAnnotationAnalyzerImpl.class);

    private static final String FIELD = "Field '";
    private static final String JACKSON_ANALYZER = "JacksonAnnotationAnalyzer: ";

    // Set of supported Jackson annotation types
    private static final Set<String> SUPPORTED_JACKSON_ANNOTATIONS = Set.of(
            JsonFormat.class.getName(),
            JsonProperty.class.getName(),
            JsonCreator.class.getName(),
            JsonValue.class.getName(),
            JsonDeserialize.class.getName(),
            JsonIgnore.class.getName(),
            JsonTypeInfo.class.getName()
    );

    private final AnnotationValidator annotationValidator;
    private final ProcessingEnvironment processingEnv;

    /**
     * Constructor with annotation validator for comprehensive validation.
     */
    public JacksonAnnotationAnalyzerImpl(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.annotationValidator = new AnnotationValidatorImpl();
    }

    /**
     * Constructor with custom annotation validator.
     */
    public JacksonAnnotationAnalyzerImpl(ProcessingEnvironment processingEnv, AnnotationValidator annotationValidator) {
        this.processingEnv = processingEnv;
        this.annotationValidator = annotationValidator;
    }

    @Override
    public List<AnnotationInfo> extractJacksonAnnotations(VariableElement field) {
        if (field == null) {
            return Collections.emptyList();
        }

        List<AnnotationInfo> annotations = new ArrayList<>();
        List<String> validationWarnings = new ArrayList<>();

        // Process all annotation mirrors on the field
        for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
            String annotationType = mirror.getAnnotationType().toString();

            if (SUPPORTED_JACKSON_ANNOTATIONS.contains(annotationType)) {
                try {
                    AnnotationInfo info = createAnnotationInfo(mirror, annotationType);
                    if (info != null) {
                        // Validate individual annotation
                        AnnotationValidator.ValidationResult result = annotationValidator.validateAnnotation(field, info);

                        if (result.isValid()) {
                            annotations.add(info);

                            // Log warnings if any
                            if (result.hasWarnings()) {
                                for (String warning : result.getWarnings()) {
                                    logWarning(FIELD + field.getSimpleName() + "': " + warning);
                                    validationWarnings.add(warning);
                                }
                            }
                        } else {
                            // Log validation errors but continue processing other annotations
                            for (String error : result.getErrors()) {
                                logError(FIELD + field.getSimpleName() + "' annotation validation failed: " + error);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log error and continue with other annotations (graceful degradation)
                    logError("Failed to process annotation " + annotationType + " on field '" +
                            field.getSimpleName() + "': " + e.getMessage());
                }
            }
        }

        // Validate annotation combinations
        if (!annotations.isEmpty()) {
            try {
                AnnotationValidator.ValidationResult combinationResult =
                        annotationValidator.validateAnnotationCombination(field, annotations);

                if (combinationResult.hasWarnings()) {
                    for (String warning : combinationResult.getWarnings()) {
                        logWarning(FIELD + field.getSimpleName() + "' annotation combination: " + warning);
                    }
                }

                if (combinationResult.hasErrors()) {
                    for (String error : combinationResult.getErrors()) {
                        logError(FIELD + field.getSimpleName() + "' annotation combination error: " + error);
                    }
                }
            } catch (Exception e) {
                logError("Failed to validate annotation combination for field '" +
                        field.getSimpleName() + "': " + e.getMessage());
            }
        }

        return annotations;
    }

    @Override
    public boolean hasJacksonAnnotations(VariableElement field) {
        if (field == null) {
            return false;
        }

        return field.getAnnotationMirrors().stream()
                .anyMatch(mirror -> SUPPORTED_JACKSON_ANNOTATIONS.contains(
                        mirror.getAnnotationType().toString()));
    }

    @Override
    public String generateAnnotationCode(AnnotationInfo annotation) {
        if (annotation == null || annotation.getAnnotationType() == null) {
            return "";
        }

        StringBuilder code = new StringBuilder();
        String simpleAnnotationType = getSimpleAnnotationType(annotation.getAnnotationType());
        code.append("@").append(simpleAnnotationType);

        Map<String, Object> parameters = annotation.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            code.append("(");

            // Special case: if there's only one parameter named "value", omit the parameter name
            if (parameters.size() == 1 && parameters.containsKey("value")) {
                Object value = parameters.get("value");
                if (value instanceof String) {
                    code.append("\"").append(escapeString((String) value)).append("\"");
                } else if (value instanceof Boolean) {
                    code.append(value);
                } else if (value instanceof Number) {
                    code.append(value);
                } else {
                    code.append(value.toString());
                }
            } else {
                // Multiple parameters or non-value parameter
                List<String> paramStrings = new ArrayList<>();
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    String paramCode = generateParameterCode(entry.getKey(), entry.getValue());
                    if (!paramCode.isEmpty()) {
                        paramStrings.add(paramCode);
                    }
                }
                code.append(String.join(", ", paramStrings));
            }

            code.append(")");
        }

        return code.toString();
    }

    @Override
    public List<String> extractAndGenerateAnnotationCode(VariableElement field) {
        List<AnnotationInfo> annotations = extractJacksonAnnotations(field);
        return annotations.stream()
                .map(this::generateAnnotationCode)
                .filter(code -> !code.isEmpty())
                .toList();
    }

    /**
     * Creates an AnnotationInfo object from an AnnotationMirror.
     */
    private AnnotationInfo createAnnotationInfo(AnnotationMirror mirror, String annotationType) {
        try {
            AnnotationInfo info = new AnnotationInfo();
            info.setAnnotationType(annotationType);

            // Extract annotation parameters with validation
            Map<String, Object> parameters = extractAnnotationParameters(mirror);
            info.setParameters(parameters);

            // Set application scope based on annotation type
            setAnnotationScope(info, annotationType);

            // Generate annotation code
            info.setAnnotationCode(generateAnnotationCode(info));

            return info;
        } catch (Exception e) {
            // Log detailed error information for debugging
            logError("Error processing annotation " + annotationType + ": " + e.getMessage() +
                    " (Mirror: " + mirror + ")");
            return null;
        }
    }

    /**
     * Extracts parameters from an AnnotationMirror.
     */
    private Map<String, Object> extractAnnotationParameters(AnnotationMirror mirror) {
        Map<String, Object> parameters = new HashMap<>();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                mirror.getElementValues().entrySet()) {

            String paramName = entry.getKey().getSimpleName().toString();
            Object paramValue = extractAnnotationValue(entry.getValue());

            if (paramValue != null) {
                parameters.put(paramName, paramValue);
            }
        }

        return parameters;
    }

    /**
     * Extracts the actual value from an AnnotationValue.
     */
    private Object extractAnnotationValue(AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }

        Object value = annotationValue.getValue();

        // Handle different types of annotation values
        if (value instanceof String) {
            return value;
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof Boolean) {
            return value;
        } else if (value instanceof List) {
            // Handle array values
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) value;
            return list.stream()
                    .map(this::extractAnnotationValue)
                    .collect(Collectors.toList());
        } else if (value != null) {
            // Handle enums and other complex types
            return value.toString();
        }

        return null;
    }

    /**
     * Sets the annotation scope (field, getter, setter) based on annotation type.
     */
    private void setAnnotationScope(AnnotationInfo info, String annotationType) {
        // Default: apply to field and getter
        info.setAppliesToField(true);
        info.setAppliesToGetter(true);
        info.setAppliesToSetter(false);

        // Specific rules for certain annotations
        switch (annotationType) {
            case "com.fasterxml.jackson.annotation.JsonFormat":
                // JsonFormat applies to field and getter for deserialization
                info.setAppliesToField(true);
                info.setAppliesToGetter(true);
                info.setAppliesToSetter(false);
                break;

            case "com.fasterxml.jackson.annotation.JsonProperty":
                // JsonProperty applies to getter and setter
                info.setAppliesToField(false);
                info.setAppliesToGetter(true);
                info.setAppliesToSetter(true);
                break;

            case "com.fasterxml.jackson.annotation.JsonIgnore":
                // JsonIgnore applies to getter and setter
                info.setAppliesToField(false);
                info.setAppliesToGetter(true);
                info.setAppliesToSetter(true);
                break;

            case "com.fasterxml.jackson.databind.annotation.JsonDeserialize":
                // JsonDeserialize applies to field and setter
                info.setAppliesToField(true);
                info.setAppliesToGetter(false);
                info.setAppliesToSetter(true);
                break;

            default:
                // Keep default settings
                break;
        }
    }

    /**
     * Generates parameter code for annotation parameters.
     */
    private String generateParameterCode(String paramName, Object paramValue) {
        if (paramValue == null) {
            return "";
        }

        StringBuilder code = new StringBuilder();

        // Named parameter
        code.append(paramName).append(" = ");

        if (paramValue instanceof String) {
            code.append("\"").append(escapeString((String) paramValue)).append("\"");
        } else if (paramValue instanceof Boolean) {
            code.append(paramValue);
        } else if (paramValue instanceof Number) {
            code.append(paramValue);
        } else if (paramValue instanceof List) {
            // Handle array parameters
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) paramValue;
            code.append("{");
            List<String> elements = list.stream()
                    .map(this::formatArrayElement)
                    .collect(Collectors.toList());
            code.append(String.join(", ", elements));
            code.append("}");
        } else {
            // Handle enums and other types
            code.append(paramValue);
        }

        return code.toString();
    }

    /**
     * Formats an array element for code generation.
     */
    private String formatArrayElement(Object element) {
        if (element instanceof String) {
            return "\"" + escapeString((String) element) + "\"";
        } else {
            return element.toString();
        }
    }

    /**
     * Escapes special characters in strings for code generation.
     */
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }

        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Gets the simple class name from a fully qualified annotation type.
     */
    private String getSimpleAnnotationType(String fullyQualifiedName) {
        if (fullyQualifiedName == null) {
            return "";
        }

        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Logs an error message using the processing environment.
     */
    private void logError(String message) {
        if (processingEnv != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    JACKSON_ANALYZER + message);
        } else {
            logger.info("JacksonAnnotationAnalyzer ERROR: " + message);
        }
    }

    /**
     * Logs a warning message using the processing environment.
     */
    private void logWarning(String message) {
        if (processingEnv != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    JACKSON_ANALYZER + message);
        } else {
            logger.info("JacksonAnnotationAnalyzer WARNING: {}" + message);
        }
    }

    /**
     * Logs an informational message using the processing environment.
     */
    private void logInfo(String message) {
        if (processingEnv != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    JACKSON_ANALYZER + message);
        } else {
            logger.info("JacksonAnnotationAnalyzer INFO: " + message);
        }
    }
}