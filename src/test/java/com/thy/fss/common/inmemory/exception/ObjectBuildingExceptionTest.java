package com.thy.fss.common.inmemory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectBuildingExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
        String message = "Object building failed";
        ObjectBuildingException exception = new ObjectBuildingException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getTargetClass());
        assertNull(exception.getPropertyName());
        assertInstanceOf(InMemoryDataStoreException.class, exception);
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Reflection error";
        Throwable cause = new IllegalAccessException("Field not accessible");
        ObjectBuildingException exception = new ObjectBuildingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getTargetClass());
        assertNull(exception.getPropertyName());
    }

    @Test
    @DisplayName("Should create exception with message and target class")
    void shouldCreateExceptionWithMessageAndTargetClass() {
        String message = "Cannot instantiate class";
        Class<?> targetClass = TestClass.class;
        ObjectBuildingException exception = new ObjectBuildingException(message, targetClass);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(targetClass, exception.getTargetClass());
        assertNull(exception.getPropertyName());
    }

    @Test
    @DisplayName("Should create exception with message, cause, and target class")
    void shouldCreateExceptionWithMessageCauseAndTargetClass() {
        String message = "Constructor invocation failed";
        Throwable cause = new InstantiationException("No default constructor");
        Class<?> targetClass = TestClass.class;
        ObjectBuildingException exception = new ObjectBuildingException(message, cause, targetClass);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(targetClass, exception.getTargetClass());
        assertNull(exception.getPropertyName());
    }

    @Test
    @DisplayName("Should create exception with message, target class, and property name")
    void shouldCreateExceptionWithMessageTargetClassAndPropertyName() {
        String message = "Property mapping failed";
        Class<?> targetClass = TestClass.class;
        String propertyName = "name";
        ObjectBuildingException exception = new ObjectBuildingException(message, targetClass, propertyName);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(targetClass, exception.getTargetClass());
        assertEquals(propertyName, exception.getPropertyName());
    }

    @Test
    @DisplayName("Should create exception with all parameters")
    void shouldCreateExceptionWithAllParameters() {
        String message = "Type conversion failed";
        Throwable cause = new NumberFormatException("Invalid number format");
        Class<?> targetClass = TestClass.class;
        String propertyName = "age";
        ObjectBuildingException exception = new ObjectBuildingException(message, cause, targetClass, propertyName);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(targetClass, exception.getTargetClass());
        assertEquals(propertyName, exception.getPropertyName());
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        ObjectBuildingException exception = new ObjectBuildingException(null);
        assertNull(exception.getMessage());
        assertNull(exception.getTargetClass());
        assertNull(exception.getPropertyName());
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
        String message = "Building error";
        ObjectBuildingException exception = new ObjectBuildingException(message, (Throwable) null);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should handle null target class")
    void shouldHandleNullTargetClass() {
        String message = "Building error";
        ObjectBuildingException exception = new ObjectBuildingException(message, (Class<?>) null);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getTargetClass());
    }

    @Test
    @DisplayName("Should handle null property name")
    void shouldHandleNullPropertyName() {
        String message = "Building error";
        Class<?> targetClass = TestClass.class;
        ObjectBuildingException exception = new ObjectBuildingException(message, targetClass, null);
        assertEquals(message, exception.getMessage());
        assertEquals(targetClass, exception.getTargetClass());
        assertNull(exception.getPropertyName());
    }

    @Test
    @DisplayName("Should handle empty property name")
    void shouldHandleEmptyPropertyName() {
        String message = "Building error";
        Class<?> targetClass = TestClass.class;
        String propertyName = "";
        ObjectBuildingException exception = new ObjectBuildingException(message, targetClass, propertyName);
        assertEquals(message, exception.getMessage());
        assertEquals(targetClass, exception.getTargetClass());
        assertEquals(propertyName, exception.getPropertyName());
    }

    @Test
    @DisplayName("Should be instance of InMemoryDataStoreException")
    void shouldBeInstanceOfInMemoryDataStoreException() {
        ObjectBuildingException exception = new ObjectBuildingException("test");
        assertInstanceOf(InMemoryDataStoreException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should preserve target class information")
    void shouldPreserveTargetClassInformation() {
        Class<?> targetClass = String.class;
        ObjectBuildingException exception = new ObjectBuildingException("error", targetClass);

        assertEquals(targetClass, exception.getTargetClass());
        assertEquals("String", exception.getTargetClass().getSimpleName());
    }

    @Test
    @DisplayName("Should preserve property name information")
    void shouldPreservePropertyNameInformation() {
        String propertyName = "testProperty";
        ObjectBuildingException exception = new ObjectBuildingException("error", TestClass.class, propertyName);

        assertEquals(propertyName, exception.getPropertyName());
    }

    static class TestClass {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}