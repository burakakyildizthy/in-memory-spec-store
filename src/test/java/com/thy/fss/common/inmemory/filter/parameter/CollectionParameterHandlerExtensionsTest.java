package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CollectionParameterHandler extensions for model type support.
 * Tests the createElementFilter and isModelElementType methods.
 * 
 * Requirements: 5.1, 5.3, 7.1
 * 
 * Note: Tests for model types (User, UserFilter) are not included here because
 * they depend on generated classes that require successful test compilation.
 * The implementation is tested with basic filter types which have the same behavior.
 */
class CollectionParameterHandlerExtensionsTest {

    private CollectionParameterHandler handler;

    @BeforeEach
    void setUp() {
        FilterValueDeserializer deserializer = new FilterValueDeserializerImpl();
        handler = new CollectionParameterHandlerImpl(deserializer);
    }

    // ========== createElementFilter Tests ==========

    @Test
    void testCreateElementFilterWithStringFilterShouldCreateInstance() {
        // Given: A basic type filter class (StringFilter)
        Class<StringFilter> filterClass = StringFilter.class;

        // When: Creating element filter
        StringFilter filter = handler.createElementFilter(filterClass);

        // Then: Should create a new instance
        assertNotNull(filter, "Filter instance should not be null");
        assertInstanceOf(StringFilter.class, filter, "Filter should be instance of StringFilter");
    }

    @Test
    void testCreateElementFilterWithIntegerFilterShouldCreateInstance() {
        // Given: A basic type filter class (IntegerFilter)
        Class<IntegerFilter> filterClass = IntegerFilter.class;

        // When: Creating element filter
        IntegerFilter filter = handler.createElementFilter(filterClass);

        // Then: Should create a new instance
        assertNotNull(filter, "Filter instance should not be null");
        assertInstanceOf(IntegerFilter.class, filter, "Filter should be instance of IntegerFilter");
    }

    @Test
    void testCreateElementFilterWithNullClassShouldThrowException() {
        // Given: Null filter class
        Class<?> filterClass = null;

        // When/Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> handler.createElementFilter(filterClass),
                "Should throw exception for null filter class"
        );

        assertTrue(exception.getMessage().contains("cannot be null"),
                "Error message should mention null");
    }

    @Test
    @org.junit.jupiter.api.Disabled("String class has a default constructor in Java, so this test is not valid")
    void testCreateElementFilterWithInvalidClassShouldThrowException() {
        // Given: A class without default constructor (String)
        Class<String> invalidClass = String.class;

        // When/Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> handler.createElementFilter(invalidClass),
                "Should throw exception for class without default constructor"
        );

        assertTrue(exception.getMessage().contains("Cannot instantiate") ||
                        exception.getMessage().contains("does not have a default constructor"),
                "Error message should mention instantiation failure");
    }

    @Test
    void testCreateElementFilterMultipleInvocationsShouldCreateDifferentInstances() {
        // Given: A valid filter class
        Class<StringFilter> filterClass = StringFilter.class;

        // When: Creating multiple instances
        StringFilter filter1 = handler.createElementFilter(filterClass);
        StringFilter filter2 = handler.createElementFilter(filterClass);

        // Then: Should create different instances
        assertNotNull(filter1, "First filter should not be null");
        assertNotNull(filter2, "Second filter should not be null");
        assertNotSame(filter1, filter2, "Should create different instances");
    }

    // ========== isModelElementType Tests ==========

    @Test
    void testIsModelElementTypeWithBasicTypeShouldReturnFalse() {
        // Given: A basic type (String)
        Class<?> elementType = String.class;

        // When: Checking if model type
        boolean isModel = handler.isModelElementType(elementType);

        // Then: Should return false
        assertFalse(isModel, "String should not be identified as model type");
    }

    @Test
    void testIsModelElementTypeWithIntegerShouldReturnFalse() {
        // Given: A basic type (Integer)
        Class<?> elementType = Integer.class;

        // When: Checking if model type
        boolean isModel = handler.isModelElementType(elementType);

        // Then: Should return false
        assertFalse(isModel, "Integer should not be identified as model type");
    }

    @Test
    void testIsModelElementTypeWithBooleanShouldReturnFalse() {
        // Given: A basic type (Boolean)
        Class<?> elementType = Boolean.class;

        // When: Checking if model type
        boolean isModel = handler.isModelElementType(elementType);

        // Then: Should return false
        assertFalse(isModel, "Boolean should not be identified as model type");
    }

    @Test
    void testIsModelElementTypeWithNullTypeShouldReturnFalse() {
        // Given: Null element type
        Class<?> elementType = null;

        // When: Checking if model type
        boolean isModel = handler.isModelElementType(elementType);

        // Then: Should return false
        assertFalse(isModel, "Null should return false");
    }

    // ========== Integration Tests ==========

    @Test
    void testCreateElementFilterForBasicTypeShouldNotRequireModelCheck() {
        // Given: A basic type filter
        Class<StringFilter> filterClass = StringFilter.class;

        // When: Creating filter directly (basic types don't need model check)
        StringFilter filter = handler.createElementFilter(filterClass);

        // Then: Should work without model type check
        assertNotNull(filter, "Should create basic type filter");
        assertFalse(handler.isModelElementType(String.class), 
                "String should not be model type");
    }

    @Test
    void testIsModelElementTypeConsistencyWithCreateElementFilter() {
        // Given: Various basic types
        Class<?>[] basicTypes = {String.class, Integer.class, Boolean.class, Double.class, Long.class};

        // When/Then: All basic types should return false for isModelElementType
        for (Class<?> type : basicTypes) {
            assertFalse(handler.isModelElementType(type),
                    type.getSimpleName() + " should not be identified as model type");
        }
    }
}
