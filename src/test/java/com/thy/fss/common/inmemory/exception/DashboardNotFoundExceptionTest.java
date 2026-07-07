package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.dashboard.exception.DashboardNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DashboardNotFoundException class.
 * Tests exception construction, message handling, and dashboard ID tracking.
 */
@DisplayName("DashboardNotFoundException Tests")
class DashboardNotFoundExceptionTest {

    // Constants for duplicate string literals
    private static final String TEST_ID = "test";
    private static final String DASHBOARD_123 = "dashboard-123";
    private static final String DASHBOARD_456 = "dashboard-456";
    private static final String DASHBOARD_789 = "dashboard-789";
    private static final String DASHBOARD_NULL_MSG = "dashboard-null-msg";
    private static final String DASHBOARD_NULL_CAUSE = "dashboard-null-cause";
    private static final String DASH_1 = "dash-1";
    private static final String DASH_DEEP = "dash-deep";
    private static final String TEST_DASHBOARD = "test-dashboard";
    private static final String CUSTOM_DASHBOARD = "custom-dashboard";
    private static final String MSG_DASHBOARD_NOT_FOUND = "Dashboard not found";
    private static final String MSG_CUSTOM_ERROR = "Custom error message";
    private static final String MSG_DASHBOARD_LOOKUP_FAILED = "Dashboard lookup failed";
    private static final String MSG_DATABASE_CONNECTION_ERROR = "Database connection error";
    private static final String MSG_TEST_MESSAGE = "Test message";
    private static final String MSG_DATABASE_ERROR = "Database error";
    private static final String MSG_SERVICE_UNAVAILABLE = "Service unavailable";
    private static final String MSG_NETWORK_TIMEOUT = "Network timeout";
    private static final String MSG_CONNECTION_FAILED = "Connection failed";
    private static final String MSG_SERVICE_ERROR = "Service error";
    private static final String MSG_DEEP_CHAIN_ERROR = "Deep chain error";
    private static final String MSG_UNABLE_TO_LOCATE = "Unable to locate dashboard in registry";
    private static final String MSG_DASHBOARD_NOT_FOUND_WITH_ID = "Dashboard not found with ID: ";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Should create exception with dashboard ID")
        void testExceptionWithDashboardId() {
            String dashboardId = DASHBOARD_123;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId);

            assertEquals(dashboardId, exception.getDashboardId());
            assertTrue(exception.getMessage().contains(dashboardId));
            assertTrue(exception.getMessage().contains(MSG_DASHBOARD_NOT_FOUND));
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with dashboard ID and custom message")
        void testExceptionWithDashboardIdAndMessage() {
            String dashboardId = DASHBOARD_456;
            String message = MSG_CUSTOM_ERROR;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId, message);

            assertEquals(dashboardId, exception.getDashboardId());
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with dashboard ID, message, and cause")
        void testExceptionWithAllParameters() {
            String dashboardId = DASHBOARD_789;
            String message = MSG_DASHBOARD_LOOKUP_FAILED;
            Exception cause = new RuntimeException(MSG_DATABASE_CONNECTION_ERROR);
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId, message, cause);

            assertEquals(dashboardId, exception.getDashboardId());
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should handle null dashboard ID")
        void testExceptionWithNullDashboardId() {
            DashboardNotFoundException exception = new DashboardNotFoundException(null);

            assertNull(exception.getDashboardId());
            assertTrue(exception.getMessage().contains("null"));
        }

        @Test
        @DisplayName("Should handle empty dashboard ID")
        void testExceptionWithEmptyDashboardId() {
            String dashboardId = "";
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId);

            assertEquals(dashboardId, exception.getDashboardId());
            assertTrue(exception.getMessage().contains(MSG_DASHBOARD_NOT_FOUND));
        }

        @Test
        @DisplayName("Should handle null message")
        void testExceptionWithNullMessage() {
            String dashboardId = DASHBOARD_NULL_MSG;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId, null);

            assertEquals(dashboardId, exception.getDashboardId());
            assertNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should handle null cause")
        void testExceptionWithNullCause() {
            String dashboardId = DASHBOARD_NULL_CAUSE;
            String message = MSG_TEST_MESSAGE;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId, message, null);

            assertEquals(dashboardId, exception.getDashboardId());
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }
    }

    @Nested
    @DisplayName("Dashboard ID Tests")
    class DashboardIdTest {


        @DisplayName("Should preserve dashboard ID with special characters")
        @ParameterizedTest
        @ValueSource(strings = {"dashboard-123_test@domain.com", "dashboard with spaces", "dashboard-测试-🚀"})
        void testNotNull(String dashboardId) {
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId);

            assertEquals(dashboardId, exception.getDashboardId());
            assertTrue(exception.getMessage().contains(dashboardId));
        }

        @Test
        @DisplayName("Should preserve very long dashboard ID")
        void testVeryLongDashboardId() {
            String dashboardId = "a".repeat(1000);
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId);

            assertEquals(dashboardId, exception.getDashboardId());
            assertTrue(exception.getMessage().contains(dashboardId));
        }

    }

    @Nested
    @DisplayName("Exception Type Tests")
    class ExceptionTypeTest {

        @Test
        @DisplayName("Should be instance of RuntimeException")
        void testExceptionType() {
            DashboardNotFoundException exception = new DashboardNotFoundException(TEST_ID);

            assertInstanceOf(RuntimeException.class, exception);
            assertInstanceOf(Exception.class, exception);
            assertInstanceOf(Throwable.class, exception);
        }

        @Test
        @DisplayName("Should not be instance of checked exceptions")
        void testNotCheckedException() {
            DashboardNotFoundException exception = new DashboardNotFoundException(TEST_ID);

            // Should be a runtime exception, not a checked exception
            assertInstanceOf(RuntimeException.class, exception);
        }
    }

    @Nested
    @DisplayName("Exception Chaining Tests")
    class ExceptionChainingTest {

        @Test
        @DisplayName("Should preserve exception chain")
        void testExceptionChaining() {
            RuntimeException rootCause = new RuntimeException(MSG_DATABASE_ERROR);
            IllegalStateException intermediateCause = new IllegalStateException(MSG_SERVICE_UNAVAILABLE, rootCause);
            DashboardNotFoundException exception = new DashboardNotFoundException(DASH_1, MSG_DASHBOARD_LOOKUP_FAILED, intermediateCause);

            assertEquals(DASH_1, exception.getDashboardId());
            assertEquals(MSG_DASHBOARD_LOOKUP_FAILED, exception.getMessage());
            assertEquals(intermediateCause, exception.getCause());
            assertEquals(rootCause, exception.getCause().getCause());
        }

        @Test
        @DisplayName("Should handle deep exception chains")
        void testDeepExceptionChain() {
            Exception level1 = new Exception(MSG_NETWORK_TIMEOUT);
            Exception level2 = new Exception(MSG_CONNECTION_FAILED, level1);
            Exception level3 = new Exception(MSG_SERVICE_ERROR, level2);
            DashboardNotFoundException exception = new DashboardNotFoundException(DASH_DEEP, MSG_DEEP_CHAIN_ERROR, level3);

            assertEquals(DASH_DEEP, exception.getDashboardId());
            assertEquals(level3, exception.getCause());
            assertEquals(level2, exception.getCause().getCause());
            assertEquals(level1, exception.getCause().getCause().getCause());
        }
    }

    @Nested
    @DisplayName("Message Format Tests")
    class MessageFormatTest {

        @Test
        @DisplayName("Should generate default message format correctly")
        void testDefaultMessageFormat() {
            String dashboardId = TEST_DASHBOARD;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId);

            String expectedMessage = MSG_DASHBOARD_NOT_FOUND_WITH_ID + dashboardId;
            assertEquals(expectedMessage, exception.getMessage());
        }

        @Test
        @DisplayName("Should handle custom message format")
        void testCustomMessageFormat() {
            String dashboardId = CUSTOM_DASHBOARD;
            String customMessage = MSG_UNABLE_TO_LOCATE;
            DashboardNotFoundException exception = new DashboardNotFoundException(dashboardId, customMessage);

            assertEquals(customMessage, exception.getMessage());
            // Dashboard ID should still be accessible
            assertEquals(dashboardId, exception.getDashboardId());
        }
    }
}