package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataSourceConnectionException.
 */
@DisplayName("DataSourceConnectionException Tests")
class DataSourceConnectionExceptionTest {

    // Constants for duplicate string literals
    private static final String MSG_CONNECTION_FAILED = "Connection failed";
    private static final String TEST_DATA_SOURCE_NAME = "testDataSource";

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        String message = MSG_CONNECTION_FAILED;
        DataSourceConnectionException exception = new DataSourceConnectionException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Connection failed with cause";
        Throwable cause = new RuntimeException("Network error");

        DataSourceConnectionException exception = new DataSourceConnectionException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should create exception with message and data source name")
    void shouldCreateExceptionWithMessageAndDataSourceName() {
        String message = MSG_CONNECTION_FAILED;
        String dataSourceName = TEST_DATA_SOURCE_NAME;

        DataSourceConnectionException exception = new DataSourceConnectionException(message, dataSourceName);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getDataSourceName()).isEqualTo(dataSourceName);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message, cause and data source name")
    void shouldCreateExceptionWithMessageCauseAndDataSourceName() {
        String message = MSG_CONNECTION_FAILED;
        Throwable cause = new RuntimeException("Network timeout");
        String dataSourceName = TEST_DATA_SOURCE_NAME;

        DataSourceConnectionException exception = new DataSourceConnectionException(message, cause, dataSourceName);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getDataSourceName()).isEqualTo(dataSourceName);
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        DataSourceConnectionException exception = new DataSourceConnectionException(null);

        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        String message = "";
        DataSourceConnectionException exception = new DataSourceConnectionException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should be instance of InMemoryDataStoreException")
    void shouldBeInstanceOfInMemoryDataStoreException() {
        DataSourceConnectionException exception = new DataSourceConnectionException("test message");

        assertThat(exception).isInstanceOf(InMemoryDataStoreException.class)
                .isInstanceOf(RuntimeException.class);
    }
}