package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.exception.AnnotationValidationException;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;

import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * Interface for validating Jackson annotations during analysis.
 * Provides comprehensive validation of annotation parameters and combinations
 * to ensure proper deserializer generation.
 */
public interface AnnotationValidator {

    /**
     * Validates a single Jackson annotation for the given field.
     *
     * @param field      the entity field being analyzed
     * @param annotation the annotation to validate
     * @return validation result containing any errors found
     */
    ValidationResult validateAnnotation(VariableElement field, AnnotationInfo annotation);

    /**
     * Validates a combination of Jackson annotations for the given field.
     * Checks for conflicting or unsupported annotation combinations.
     *
     * @param field       the entity field being analyzed
     * @param annotations the list of annotations to validate together
     * @return validation result containing any errors found
     */
    ValidationResult validateAnnotationCombination(VariableElement field, List<AnnotationInfo> annotations);

    /**
     * Validates that an annotation is supported for the given field type.
     *
     * @param field      the entity field being analyzed
     * @param annotation the annotation to check for support
     * @return true if the annotation is supported for this field type
     */
    boolean isAnnotationSupportedForFieldType(VariableElement field, AnnotationInfo annotation);

    /**
     * Gets a list of all supported Jackson annotation types.
     *
     * @return set of fully qualified annotation class names that are supported
     */
    List<String> getSupportedAnnotationTypes();

    /**
     * Validates annotation parameters for correctness and completeness.
     *
     * @param annotation the annotation to validate parameters for
     * @return validation result containing any parameter errors found
     */
    ValidationResult validateAnnotationParameters(AnnotationInfo annotation);

    /**
     * Result of annotation validation containing errors and warnings.
     */
    class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? errors : List.of();
            this.warnings = warnings != null ? warnings : List.of();
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, List.of(), List.of());
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, List.of(error), List.of());
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors, List.of());
        }

        public static ValidationResult withWarnings(List<String> warnings) {
            return new ValidationResult(true, List.of(), warnings);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * Throws an AnnotationValidationException if this result contains errors.
         *
         * @param fieldName      the name of the field being validated
         * @param annotationType the type of annotation being validated
         * @throws AnnotationValidationException if validation failed
         */
        public void throwIfInvalid(String fieldName, String annotationType) throws AnnotationValidationException {
            if (!valid && hasErrors()) {
                throw new AnnotationValidationException(fieldName, annotationType, errors);
            }
        }
    }
}