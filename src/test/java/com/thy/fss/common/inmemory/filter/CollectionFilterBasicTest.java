package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Comprehensive unit tests for CollectionFilter class.
 * Tests basic functionality including constructors, method chaining, equals/hashCode, and toString.
 */
class CollectionFilterBasicTest {

    @Test
    void testDefaultConstructor() {
        // When
        CollectionFilter<String> filter = new CollectionFilter<>();

        // Then - all collection-specific properties should be null
        assertNull(filter.getCollectionContains());
        assertNull(filter.getCollectionAny());
        assertNull(filter.getCollectionAll());
        assertNull(filter.getCollectionNone());
        assertNull(filter.getIsEmpty());
        assertNull(filter.getIsNotEmpty());

        // Then - inherited properties should also be null
        assertNull(filter.getEquals());
        assertNull(filter.getNotEquals());
        assertNull(filter.getIsNull());
        assertNull(filter.getIsNotNull());
        assertNull(filter.getIn());
        assertNull(filter.getNotIn());

        System.out.println("✓ Default constructor test passed");
    }

    @Test
    void testCopyConstructor() {
        // Given
        CollectionFilter<String> original = new CollectionFilter<>();
        original.setCollectionContains("test")
                .setIsEmpty(true)
                .setIsNotEmpty(false)
                .setEquals(Arrays.asList("a", "b"))
                .setIsNull(true);

        StringFilter stringFilter = new StringFilter().setContains("filter");
        original.setCollectionAny(stringFilter);

        // When
        CollectionFilter<String> copy = new CollectionFilter<>(original);

        // Then - collection-specific properties should be copied
        assertEquals(original.getCollectionContains(), copy.getCollectionContains());
        assertEquals(original.getCollectionAny(), copy.getCollectionAny());
        assertSame(original.getCollectionAll(), copy.getCollectionAll());
        assertSame(original.getCollectionNone(), copy.getCollectionNone());
        assertEquals(original.getIsEmpty(), copy.getIsEmpty());
        assertEquals(original.getIsNotEmpty(), copy.getIsNotEmpty());

        // Then - inherited properties should be copied
        assertEquals(original.getEquals(), copy.getEquals());
        assertEquals(original.getIsNull(), copy.getIsNull());

        // Then - objects should be equal but not the same instance
        assertEquals(original, copy);
        assertNotSame(original, copy);

        System.out.println("✓ Copy constructor test passed");
    }

    @Test
    void testMethodChaining() {
        // Given
        StringFilter anyFilter = new StringFilter().setContains("any");
        StringFilter allFilter = new StringFilter().setContains("all");
        StringFilter noneFilter = new StringFilter().setContains("none");
        List<String> equalsList = Arrays.asList("a", "b");
        Collection<Collection<String>> inList = Arrays.asList(List.of("x"), List.of("y"));
        Collection<Collection<String>> notInList = List.of(List.of("z"));

        // When
        CollectionFilter<String> filter = new CollectionFilter<String>()
                .setCollectionContains("test")
                .setCollectionAny(anyFilter)
                .setCollectionAll(allFilter)
                .setCollectionNone(noneFilter)
                .setIsEmpty(true)
                .setIsNotEmpty(false)
                .setEquals(equalsList)
                .setNotEquals(Arrays.asList("c", "d"))
                .setIsNull(false)
                .setIsNotNull(true)
                .setIn(inList)
                .setNotIn(notInList);

        // Then - all values should be set correctly
        assertEquals("test", filter.getCollectionContains());
        assertEquals(anyFilter, filter.getCollectionAny());
        assertEquals(allFilter, filter.getCollectionAll());
        assertEquals(noneFilter, filter.getCollectionNone());
        assertTrue(filter.getIsEmpty());
        assertFalse(filter.getIsNotEmpty());
        assertEquals(equalsList, filter.getEquals());
        assertEquals(Arrays.asList("c", "d"), filter.getNotEquals());
        assertFalse(filter.getIsNull());
        assertTrue(filter.getIsNotNull());
        assertEquals(inList, filter.getIn());
        assertEquals(notInList, filter.getNotIn());

        System.out.println("✓ Method chaining test passed");
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        CollectionFilter<String> filter1 = new CollectionFilter<>();
        CollectionFilter<String> filter2 = new CollectionFilter<>();

        // When both are empty - Then they should be equal
        assertEquals(filter1, filter2);
        assertEquals(filter1.hashCode(), filter2.hashCode());

        // When both have same values
        StringFilter stringFilter = new StringFilter().setContains("test");
        filter1.setCollectionContains("element")
                .setCollectionAny(stringFilter)
                .setIsEmpty(true)
                .setEquals(Arrays.asList("a", "b"));
        filter2.setCollectionContains("element")
                .setCollectionAny(stringFilter)
                .setIsEmpty(true)
                .setEquals(Arrays.asList("a", "b"));

        // Then they should be equal
        assertEquals(filter1, filter2);
        assertEquals(filter1.hashCode(), filter2.hashCode());

        // When values differ - Then they should not be equal
        filter2.setCollectionContains("different");
        assertNotEquals(filter1, filter2);
        assertNotEquals(filter1.hashCode(), filter2.hashCode());

        System.out.println("✓ Equals and hashCode test passed");
    }

    @Test
    void testEqualsWithNullAndDifferentTypes() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains("test");

        // Then
        assertNotEquals(null, filter);
        assertNotEquals("not a filter", filter);
        assertNotEquals(filter, new StringFilter());
        assertEquals(filter, filter);

        System.out.println("✓ Equals with null and different types test passed");
    }

    @Test
    void testToString() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        filter.setCollectionContains("test")
                .setIsEmpty(true)
                .setEquals(Arrays.asList("a", "b"))
                .setIsNull(false);

        // When
        String toString = filter.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("CollectionFilter"));
        assertTrue(toString.contains("collectionContains=test"));
        assertTrue(toString.contains("isEmpty=true"));
        assertTrue(toString.contains("equals=[a, b]"));
        assertTrue(toString.contains("isNull=false"));

        System.out.println("✓ ToString test passed");
    }

    @Test
    void testToStringWithNullValues() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();

        // When
        String toString = filter.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("CollectionFilter"));
        assertTrue(toString.contains("collectionContains=null"));
        assertTrue(toString.contains("isEmpty=null"));
        assertTrue(toString.contains("equals=null"));

        System.out.println("✓ ToString with null values test passed");
    }

    @Test
    void testDifferentGenericTypes() {
        // Given & When
        CollectionFilter<Integer> intFilter = new CollectionFilter<>();
        CollectionFilter<String> stringFilter = new CollectionFilter<>();
        CollectionFilter<Boolean> booleanFilter = new CollectionFilter<>();

        // Then - should be able to set type-specific values
        intFilter.setCollectionContains(42);
        stringFilter.setCollectionContains("test");
        booleanFilter.setCollectionContains(true);

        assertEquals(Integer.valueOf(42), intFilter.getCollectionContains());
        assertEquals("test", stringFilter.getCollectionContains());
        assertEquals(Boolean.TRUE, booleanFilter.getCollectionContains());

        System.out.println("✓ Different generic types test passed");
    }

    @Test
    void testTypeSafetyWithNestedFilters() {
        // Given
        CollectionFilter<String> filter = new CollectionFilter<>();
        StringFilter stringFilter = new StringFilter().setContains("substring");

        // When
        filter.setCollectionAny(stringFilter);

        // Then
        assertEquals(stringFilter, filter.getCollectionAny());
        assertInstanceOf(StringFilter.class, filter.getCollectionAny());

        System.out.println("✓ Type safety with nested filters test passed");
    }
}