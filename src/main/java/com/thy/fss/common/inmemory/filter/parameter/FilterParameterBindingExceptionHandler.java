package com.thy.fss.common.inmemory.filter.parameter;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for filter parameter binding errors.
 * Provides meaningful error messages and field-level error details for parameter binding failures.
 *
 * <p>This handler catches exceptions thrown during Spring parameter binding of filter objects
 * and converts them into structured error responses with appropriate HTTP status codes.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link BindException} - General parameter binding errors with field-level details</li>
 *   <li>{@link TypeMismatchException} - Type conversion errors with clear error messages</li>
 * </ul>
 */
@ControllerAdvice
public class FilterParameterBindingExceptionHandler {

    private static final String FIELD = "field";
    private static final String REJECETED_VALUE = "rejectedValue";
    private static final String REQUIRED_TYPE = "requiredType";
    private static final String REASON = "reason";
    /**
     * Handles BindException thrown during parameter binding.
     * Extracts field-level error details and returns a structured error response.
     *
     * @param e The BindException containing binding errors
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage("Invalid filter parameters");
        error.setDetails(extractBindingErrors(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles TypeMismatchException thrown during type conversion.
     * Provides clear error message indicating which parameter failed and why.
     *
     * @param e The TypeMismatchException containing type mismatch details
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(TypeMismatchException e) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage("Invalid parameter value: " + e.getValue());
        error.setField(e.getPropertyName());

        // Add detailed error information
        Map<String, Object> details = new HashMap<>();
        details.put(FIELD, e.getPropertyName());
        details.put(REJECETED_VALUE, e.getValue());
        details.put(REQUIRED_TYPE, e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");

        if (e.getCause() != null) {
            details.put(REASON, e.getCause().getMessage());
        }

        error.setDetails(details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Extracts field-level binding errors from a BindException.
     * Creates a map of field names to error messages for all binding errors.
     *
     * @param e The BindException to extract errors from
     * @return Map of field names to error details
     */
    private Map<String, Object> extractBindingErrors(BindException e){
        Map<String, Object> errors = new HashMap<>();
        List<Map<String, String>> fieldErrors = new ArrayList<>();

        for (FieldError fieldError : e.getFieldErrors()) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put(FIELD, fieldError.getField());
            errorDetail.put(REJECETED_VALUE, fieldError.getRejectedValue() != null ? 
                    fieldError.getRejectedValue().toString() : "null");
            errorDetail.put("message", fieldError.getDefaultMessage());
            fieldErrors.add(errorDetail);
        }

        errors.put("fieldErrors", fieldErrors);
        errors.put("errorCount", e.getErrorCount());

        return errors;
    }

    /**
     * Error response structure for filter parameter binding errors.
     * Contains a general error message, optional field name, and detailed error information.
     */
    public static class ErrorResponse {
        private String message;
        private String field;
        private Map<String, Object> details;

        public ErrorResponse() {
            this.details = new HashMap<>();
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}
