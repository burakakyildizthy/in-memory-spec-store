package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CollectionFilter collection-specific operations.
 * Tests collection operation setters and getters including collectionContains,
 * collectionAny, collectionAll, collectionNone, isEmpty, and isNotEmpty.
 */
class CollectionFilterOperationsTest {


    @Test
    void testCollectionContainsProperty() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();

        // When - setting collectionContains
        filter.setCollectionContains("test-element");

        // Then
        assertEquals("test-element", filter.getCollectionContains());

        // When - setting to null
        filter.setCollectionContains(null);

        // Then
        assertNull(filter.getCollectionContains());

        // When - setting different value
        filter.setCollectionContains("another-element");

        // Then
        assertEquals("another-element", filter.getCollectionContains());

        System.out.println("✓ CollectionContains property test passed");
    }

    @Test
    void testCollectionAnyProperty() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        StringFilter stringFilter = new StringFilter().setContains("substring");

        // When - setting collectionAny
        filter.setCollectionAny(stringFilter);

        // Then
        assertEquals(stringFilter, filter.getCollectionAny());
        assertInstanceOf(StringFilter.class, filter.getCollectionAny());

        // When - setting to null
        filter.setCollectionAny(null);

        // Then
        assertNull(filter.getCollectionAny());

        // When - setting different filter
        StringFilter anotherFilter = new StringFilter().setStartsWith("prefix");
        filter.setCollectionAny(anotherFilter);

        // Then
        assertEquals(anotherFilter, filter.getCollectionAny());

        System.out.println("✓ CollectionAny property test passed");
    }

    @Test
    void testCollectionAllProperty() {
        // Given
        CollectionFilter<Integer> filter = new CollectionFilter<>();
        NumberFilter<Integer> numberFilter = new NumberFilter<Integer>().setGreaterThan(10);

        // When - setting collectionAll
        filter.setCollectionAll(numberFilter);

        // Then
        assertEquals(numberFilter, filter.getCollectionAll());
        assertInstanceOf(NumberFilter.class, filter.getCollectionAll());

        // When - setting to null
        filter.setCollectionAll(null);

        // Then
        assertNull(filter.getCollectionAll());

        // When - setting different filter
        NumberFilter<Integer> anotherFilter = new NumberFilter<Integer>().setLessThan(100);
        filter.setCollectionAll(anotherFilter);

        // Then
        assertEquals(anotherFilter, filter.getCollectionAll());

        System.out.println("✓ CollectionAll property test passed");
    }

    @Test
    void testCollectionNoneProperty() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        StringFilter stringFilter = new StringFilter().setEquals("forbidden");

        // When - setting collectionNone
        filter.setCollectionNone(stringFilter);

        // Then
        assertEquals(stringFilter, filter.getCollectionNone());
        assertInstanceOf(StringFilter.class, filter.getCollectionNone());

        // When - setting to null
        filter.setCollectionNone(null);

        // Then
        assertNull(filter.getCollectionNone());

        // When - setting different filter
        StringFilter anotherFilter = new StringFilter().setEndsWith("suffix");
        filter.setCollectionNone(anotherFilter);

        // Then
        assertEquals(anotherFilter, filter.getCollectionNone());

        System.out.println("✓ CollectionNone property test passed");
    }

    @Test
    void testIsEmptyProperty() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();

        // When - setting isEmpty to true
        filter.setIsEmpty(true);

        // Then
        assertTrue(filter.getIsEmpty());

        // When - setting isEmpty to false
        filter.setIsEmpty(false);

        // Then
        assertFalse(filter.getIsEmpty());

        // When - setting to null
        filter.setIsEmpty(null);

        // Then
        assertNull(filter.getIsEmpty());

        System.out.println("✓ IsEmpty property test passed");
    }

    @Test
    void testIsNotEmptyProperty() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();

        // When - setting isNotEmpty to true
        filter.setIsNotEmpty(true);

        // Then
        assertTrue(filter.getIsNotEmpty());

        // When - setting isNotEmpty to false
        filter.setIsNotEmpty(false);

        // Then
        assertFalse(filter.getIsNotEmpty());

        // When - setting to null
        filter.setIsNotEmpty(null);

        // Then
        assertNull(filter.getIsNotEmpty());

        System.out.println("✓ IsNotEmpty property test passed");
    }

    @Test
    void testCollectionOperationsCombination() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        StringFilter anyFilter = new StringFilter().setContains("any");
        StringFilter allFilter = new StringFilter().setStartsWith("all");
        StringFilter noneFilter = new StringFilter().setEndsWith("none");

        // When - setting multiple collection operations
        filter.setCollectionContains("element")
                .setCollectionAny(anyFilter)
                .setCollectionAll(allFilter)
                .setCollectionNone(noneFilter)
                .setIsEmpty(false)
                .setIsNotEmpty(true);

        // Then - all properties should be set correctly
        assertEquals("element", filter.getCollectionContains());
        assertEquals(anyFilter, filter.getCollectionAny());
        assertEquals(allFilter, filter.getCollectionAll());
        assertEquals(noneFilter, filter.getCollectionNone());
        assertFalse(filter.getIsEmpty());
        assertTrue(filter.getIsNotEmpty());

        System.out.println("✓ Collection operations combination test passed");
    }

    @Test
    void testCollectionOperationsWithNullValues() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();

        // When - setting all operations to null
        filter.setCollectionContains(null)
                .setCollectionAny(null)
                .setCollectionAll(null)
                .setCollectionNone(null)
                .setIsEmpty(null)
                .setIsNotEmpty(null);

        // Then - all properties should be null
        assertNull(filter.getCollectionContains());
        assertNull(filter.getCollectionAny());
        assertNull(filter.getCollectionAll());
        assertNull(filter.getCollectionNone());
        assertNull(filter.getIsEmpty());
        assertNull(filter.getIsNotEmpty());

        System.out.println("✓ Collection operations with null values test passed");
    }

    @Test
    void testCollectionOperationsMethodChaining() {
        // Given
        CollectionFilter<Integer> filter = new CollectionFilter<>();
        NumberFilter<Integer> numberFilter = new NumberFilter<Integer>().setGreaterThan(42);

        // When - using method chaining
        CollectionFilter<Integer> result = filter
                .setCollectionContains(100)
                .setCollectionAny(numberFilter)
                .setIsEmpty(false);

        // Then - should return the same instance and values should be set
        assertSame(result, filter);
        assertEquals(Integer.valueOf(100), filter.getCollectionContains());
        assertEquals(numberFilter, filter.getCollectionAny());
        assertFalse(filter.getIsEmpty());

        System.out.println("✓ Collection operations method chaining test passed");
    }

    @Test
    void testCollectionOperationsWithDifferentTypes() {
        // Test with String elements
        CollectionFilter<String> stringFilter = new CollectionFilter<>();
        stringFilter.setCollectionContains("test-string");
        assertEquals("test-string", stringFilter.getCollectionContains());

        // Test with Integer elements
        CollectionFilter<Integer> intFilter = new CollectionFilter<>();
        intFilter.setCollectionContains(42);
        assertEquals(Integer.valueOf(42), intFilter.getCollectionContains());

        // Test with Boolean elements
        CollectionFilter<Boolean> boolFilter = new CollectionFilter<>();
        boolFilter.setCollectionContains(true);
        assertEquals(Boolean.TRUE, boolFilter.getCollectionContains());

        // Test with nested filters of different types
        StringFilter stringNestedFilter = new StringFilter().setContains("nested");
        NumberFilter<Integer> numberNestedFilter = new NumberFilter<Integer>().setGreaterThan(0);

        stringFilter.setCollectionAny(stringNestedFilter);
        intFilter.setCollectionAll(numberNestedFilter);

        assertEquals(stringNestedFilter, stringFilter.getCollectionAny());
        assertEquals(numberNestedFilter, intFilter.getCollectionAll());

        System.out.println("✓ Collection operations with different types test passed");
    }
}