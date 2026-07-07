package com.thy.fss.common.inmemory.processor.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for processor exception classes:
 * ProcessingException, AnnotationValidationException, DeserializerGenerationException.
 */
@DisplayName("Processor Exception Tests")
class ProcessorExceptionTest {

    // ==================== ProcessingException ====================

    @Nested
    @DisplayName("ProcessingException")
    class ProcessingExceptionTests {

        @Test
        @DisplayName("Message-only constructor")
        void messageOnly() {
            ProcessingException ex = new ProcessingException("test error");
            assertThat(ex.getMessage()).isEqualTo("test error");
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("Message and cause constructor")
        void messageAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            ProcessingException ex = new ProcessingException("wrapped", cause);
            assertThat(ex.getMessage()).isEqualTo("wrapped");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Cause-only constructor")
        void causeOnly() {
            RuntimeException cause = new RuntimeException("original");
            ProcessingException ex = new ProcessingException(cause);
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Is a checked Exception")
        void isCheckedException() {
            ProcessingException ex = new ProcessingException("test");
            assertThat(ex).isInstanceOf(Exception.class);
        }
    }

    // ==================== AnnotationValidationException ====================

    @Nested
    @DisplayName("AnnotationValidationException")
    class AnnotationValidationExceptionTests {

        @Test
        @DisplayName("Multi-error list constructor")
        void multiErrorList() {
            List<String> errors = List.of("Error 1", "Error 2", "Error 3");
            AnnotationValidationException ex = new AnnotationValidationException(
                    "myField", "JsonFormat", errors);

            assertThat(ex.getFieldName()).isEqualTo("myField");
            assertThat(ex.getAnnotationType()).isEqualTo("JsonFormat");
            assertThat(ex.getValidationErrors()).containsExactly("Error 1", "Error 2", "Error 3");
            assertThat(ex.getMessage()).contains("myField");
            assertThat(ex.getMessage()).contains("JsonFormat");
            assertThat(ex.getMessage()).contains("Error 1");
            assertThat(ex.getMessage()).contains("Error 2");
        }

        @Test
        @DisplayName("Single error string constructor")
        void singleErrorString() {
            AnnotationValidationException ex = new AnnotationValidationException(
                    "field1", "JsonProperty", "Invalid value");

            assertThat(ex.getFieldName()).isEqualTo("field1");
            assertThat(ex.getAnnotationType()).isEqualTo("JsonProperty");
            assertThat(ex.getValidationErrors()).containsExactly("Invalid value");
            assertThat(ex.getMessage()).contains("field1");
            assertThat(ex.getMessage()).contains("Invalid value");
        }

        @Test
        @DisplayName("Constructor with cause")
        void withCause() {
            RuntimeException cause = new RuntimeException("root");
            AnnotationValidationException ex = new AnnotationValidationException(
                    "field2", "JsonCreator", "Parse error", cause);

            assertThat(ex.getFieldName()).isEqualTo("field2");
            assertThat(ex.getAnnotationType()).isEqualTo("JsonCreator");
            assertThat(ex.getValidationErrors()).containsExactly("Parse error");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Message format for single error")
        void messageFormatSingle() {
            AnnotationValidationException ex = new AnnotationValidationException(
                    "name", "JsonFormat", "bad pattern");
            assertThat(ex.getMessage()).contains("Annotation validation failed");
            assertThat(ex.getMessage()).contains("'name'");
            assertThat(ex.getMessage()).contains("'JsonFormat'");
            assertThat(ex.getMessage()).contains("bad pattern");
        }

        @Test
        @DisplayName("Message format for multiple errors with numbering")
        void messageFormatMultiple() {
            AnnotationValidationException ex = new AnnotationValidationException(
                    "price", "JsonFormat", List.of("Error A", "Error B"));
            String msg = ex.getMessage();
            assertThat(msg).contains("1.");
            assertThat(msg).contains("2.");
            assertThat(msg).contains("Error A");
            assertThat(msg).contains("Error B");
        }

        @Test
        @DisplayName("Empty error list")
        void emptyErrorList() {
            AnnotationValidationException ex = new AnnotationValidationException(
                    "field", "Annotation", List.of());
            assertThat(ex.getFieldName()).isEqualTo("field");
            assertThat(ex.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("Null error list handled")
        void nullErrorList() {
            AnnotationValidationException ex = new AnnotationValidationException(
                    "field", "Ann", (List<String>) null);
            assertThat(ex.getFieldName()).isEqualTo("field");
        }

        @Test
        @DisplayName("Is a ProcessingException")
        void isProcessingException() {
            AnnotationValidationException ex = new AnnotationValidationException("f", "a", "e");
            assertThat(ex).isInstanceOf(ProcessingException.class);
        }
    }

    // ==================== DeserializerGenerationException ====================

    @Nested
    @DisplayName("DeserializerGenerationException")
    class DeserializerGenerationExceptionTests {

        @Test
        @DisplayName("Four-arg constructor")
        void fourArgConstructor() {
            DeserializerGenerationException ex = new DeserializerGenerationException(
                    "UserFilter", "UserFilterDeserializer", "field-generation", "Cannot resolve type");

            assertThat(ex.getFilterClassName()).isEqualTo("UserFilter");
            assertThat(ex.getDeserializerClassName()).isEqualTo("UserFilterDeserializer");
            assertThat(ex.getGenerationPhase()).isEqualTo("field-generation");
            assertThat(ex.getMessage()).contains("UserFilter");
            assertThat(ex.getMessage()).contains("UserFilterDeserializer");
            assertThat(ex.getMessage()).contains("field-generation");
            assertThat(ex.getMessage()).contains("Cannot resolve type");
        }

        @Test
        @DisplayName("Five-arg constructor with cause")
        void fiveArgConstructorWithCause() {
            RuntimeException cause = new RuntimeException("IO error");
            DeserializerGenerationException ex = new DeserializerGenerationException(
                    "OrderFilter", "OrderFilterDeserializer", "write-phase", "Write failed", cause);

            assertThat(ex.getFilterClassName()).isEqualTo("OrderFilter");
            assertThat(ex.getDeserializerClassName()).isEqualTo("OrderFilterDeserializer");
            assertThat(ex.getGenerationPhase()).isEqualTo("write-phase");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).contains("OrderFilter");
        }

        @Test
        @DisplayName("Message format includes all details")
        void messageFormat() {
            DeserializerGenerationException ex = new DeserializerGenerationException(
                    "Filter1", "Deser1", "phase1", "detail");
            String msg = ex.getMessage();
            assertThat(msg).contains("Deserializer generation failed");
            assertThat(msg).contains("'Filter1'");
            assertThat(msg).contains("'Deser1'");
            assertThat(msg).contains("'phase1'");
            assertThat(msg).contains("detail");
        }

        @Test
        @DisplayName("Is a ProcessingException")
        void isProcessingException() {
            DeserializerGenerationException ex = new DeserializerGenerationException(
                    "F", "D", "P", "M");
            assertThat(ex).isInstanceOf(ProcessingException.class);
        }
    }
}
