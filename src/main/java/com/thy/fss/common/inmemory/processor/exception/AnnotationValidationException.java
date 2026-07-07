package com.thy.fss.common.inmemory.processor.exception;

import java.util.List;

/**
 * Exception thrown when Jackson annotation validation fails during analysis.
 * Contains detailed information about validation errors for debugging purposes.
 */
public class AnnotationValidationException extends ProcessingException {

    private final String fieldName;
    private final String annotationType;
    private final List<String> validationErrors;

    /**
     * Constructs a new annotation validation exception with detailed error information.
     *
     * @param fieldName        the name of the field being validated
     * @param annotationType   the type of annotation that failed validation
     * @param validationErrors list of specific validation error messages
     */
    public AnnotationValidationException(String fieldName, String annotationType, List<String> validationErrors) {
        super(buildMessage(fieldName, annotationType, validationErrors));
        this.fieldName = fieldName;
        this.annotationType = annotationType;
        this.validationErrors = validationErrors;
    }

    /**
     * Constructs a new annotation validation exception with a single error message.
     *
     * @param fieldName      the name of the field being validated
     * @param annotationType the type of annotation that failed validation
     * @param errorMessage   the validation error message
     */
    public AnnotationValidationException(String fieldName, String annotationType, String errorMessage) {
        this(fieldName, annotationType, List.of(errorMessage));
    }

    /**
     * Constructs a new annotation validation exception with a cause.
     *
     * @param fieldName      the name of the field being validated
     * @param annotationType the type of annotation that failed validation
     * @param errorMessage   the validation error message
     * @param cause          the underlying cause
     */
    public AnnotationValidationException(String fieldName, String annotationType, String errorMessage, Throwable cause) {
        super(buildMessage(fieldName, annotationType, List.of(errorMessage)), cause);
        this.fieldName = fieldName;
        this.annotationType = annotationType;
        this.validationErrors = List.of(errorMessage);
    }

    /**
     * Builds a comprehensive error message from the validation details.
     */
    private static String buildMessage(String fieldName, String annotationType, List<String> validationErrors) {
        StringBuilder message = new StringBuilder();
        message.append("Annotation validation failed for field '").append(fieldName)
                .append("' with annotation '").append(annotationType).append("'");

        if (validationErrors != null && !validationErrors.isEmpty()) {
            message.append(": ");
            if (validationErrors.size() == 1) {
                message.append(validationErrors.get(0));
            } else {
                message.append("\n");
                for (int i = 0; i < validationErrors.size(); i++) {
                    message.append("  ").append(i + 1).append(". ").append(validationErrors.get(i));
                    if (i < validationErrors.size() - 1) {
                        message.append("\n");
                    }
                }
            }
        }

        return message.toString();
    }

    /**
     * Gets the name of the field that failed validation.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the type of annotation that failed validation.
     *
     * @return the annotation type
     */
    public String getAnnotationType() {
        return annotationType;
    }

    /**
     * Gets the list of validation error messages.
     *
     * @return the validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}