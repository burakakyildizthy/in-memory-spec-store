package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataSourceException.
 */
@DisplayName("DataSourceException Tests")
class DataSourceExceptionTest {

    // Constants for duplicate string literals
    private static final String MSG_ROOT_CAUSE = "Root cause";

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        String message = "Data source error occurred";
        DataSourceException exception = new DataSourceException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Data source error with cause";
        Throwable cause = new RuntimeException(MSG_ROOT_CAUSE);

        DataSourceException exception = new DataSourceException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should create exception with cause only")
    void shouldCreateExceptionWithCauseOnly() {
        Throwable cause = new RuntimeException(MSG_ROOT_CAUSE);

        DataSourceException exception = new DataSourceException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getMessage()).contains("RuntimeException");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        DataSourceException exception = new DataSourceException((String) null);

        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        String message = "";
        DataSourceException exception = new DataSourceException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void shouldBeInstanceOfRuntimeException() {
        DataSourceException exception = new DataSourceException("test message");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}