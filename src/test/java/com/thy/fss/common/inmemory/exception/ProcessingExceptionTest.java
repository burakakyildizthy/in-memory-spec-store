package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProcessingException class.
 * Tests exception construction, message handling, and cause chaining.
 */
@DisplayName("ProcessingException Tests")
class ProcessingExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Should create exception with message")
        void testExceptionWithMessage() {
            String message = "Processing failed";
            ProcessingException exception = new ProcessingException(message);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void testExceptionWithMessageAndCause() {
            String message = "Annotation processing error";
            Exception cause = new RuntimeException("Compilation error");
            ProcessingException exception = new ProcessingException(message, cause);

            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with cause only")
        void testExceptionWithCauseOnly() {
            Exception cause = new IllegalStateException("Invalid processor state");
            ProcessingException exception = new ProcessingException(cause);

            assertEquals(cause, exception.getCause());
            // Message should be derived from cause
            assertTrue(exception.getMessage().contains("Invalid processor state"));
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void testExceptionWithNullMessage() {
            ProcessingException exception = new ProcessingException((String) null);

            assertNull(exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should handle null cause gracefully")
        void testExceptionWithNullCause() {
            String message = "Test message";
            ProcessingException exception = new ProcessingException(message, null);

            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should handle null cause in cause-only constructor")
        void testExceptionWithNullCauseOnly() {
            ProcessingException exception = new ProcessingException((Throwable) null);

            assertNull(exception.getCause());
        }
    }

    @Nested
    @DisplayName("Exception Chaining Tests")
    class ExceptionChainingTest {

        @Test
        @DisplayName("Should preserve exception chain")
        void testExceptionChaining() {
            RuntimeException rootCause = new RuntimeException("Root cause");
            IllegalArgumentException intermediateCause = new IllegalArgumentException("Intermediate", rootCause);
            ProcessingException exception = new ProcessingException("Processing failed", intermediateCause);

            assertEquals("Processing failed", exception.getMessage());
            assertEquals(intermediateCause, exception.getCause());
            assertEquals(rootCause, exception.getCause().getCause());
        }

        @Test
        @DisplayName("Should handle deep exception chains")
        void testDeepExceptionChain() {
            Exception level1 = new Exception("Level 1");
            Exception level2 = new Exception("Level 2", level1);
            Exception level3 = new Exception("Level 3", level2);
            ProcessingException exception = new ProcessingException("Processing error", level3);

            assertEquals("Processing error", exception.getMessage());
            assertEquals(level3, exception.getCause());
            assertEquals(level2, exception.getCause().getCause());
            assertEquals(level1, exception.getCause().getCause().getCause());
        }
    }

    @Nested
    @DisplayName("Exception Type Tests")
    class ExceptionTypeTest {

        @Test
        @DisplayName("Should be instance of Exception")
        void testExceptionType() {
            ProcessingException exception = new ProcessingException("test");

            assertInstanceOf(Exception.class, exception);
            assertInstanceOf(Throwable.class, exception);
        }

        @Test
        @DisplayName("Should not be instance of RuntimeException")
        void testNotRuntimeException() {
            // ProcessingException extends Exception, not RuntimeException
            assertFalse(RuntimeException.class.isAssignableFrom(ProcessingException.class));
        }
    }

    @Nested
    @DisplayName("Stack Trace Tests")
    class StackTraceTest {

        @Test
        @DisplayName("Should have valid stack trace")
        void testStackTrace() {
            ProcessingException exception = new ProcessingException("test");

            StackTraceElement[] stackTrace = exception.getStackTrace();
            assertNotNull(stackTrace);
            assertTrue(stackTrace.length > 0);

            // First element should be this test method
            assertEquals("testStackTrace", stackTrace[0].getMethodName());
        }

        @Test
        @DisplayName("Should preserve cause stack trace")
        void testCauseStackTrace() {
            RuntimeException cause = new RuntimeException("cause");
            ProcessingException exception = new ProcessingException("wrapper", cause);

            StackTraceElement[] causeStackTrace = exception.getCause().getStackTrace();
            assertNotNull(causeStackTrace);
            assertTrue(causeStackTrace.length > 0);
        }
    }
}