package com.thy.fss.common.inmemory.processor.generator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for collection filter operator binding.
 * 
 * <p><b>Feature: collection-filter-web-binding, Property 1: Collection contains operator binding</b></p>
 * <p><b>Validates: Requirements 1.1, 1.2</b></p>
 * 
 * <p>This test verifies that the collection filter deserialization correctly binds
 * the 'cont' (contains) operator to the collectionContains property.</p>
 */
class CollectionFilterOperatorBindingPropertyTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Property 1: Collection contains operator binding
     * 
     * For any collection filter field and any valid element value, when a JSON object
     * uses the 'cont' operator, the collectionContains property should be set to the
     * deserialized element value.
     * 
     * Validates: Requirements 1.1, 1.2
     */
    @Property(tries = 100)
    void collectionContainsOperatorShouldBindToCollectionContainsProperty(
            @ForAll @StringLength(min = 1, max = 50) String elementValue) throws IOException {
        
        // Given: A JSON object with the 'cont' operator
        // Use ObjectMapper to properly escape the string value
        String escapedValue = objectMapper.writeValueAsString(elementValue);
        String json = String.format("{\"cont\": %s}", escapedValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The collectionContains property should be set to the element value
        assertThat(filter.getCollectionContains()).isEqualTo(elementValue);
    }
    
    /**
     * Property 1 (variant): Multiple operators coexistence
     * 
     * For any collection filter with multiple operators, all operators should be
     * correctly applied to the CollectionFilter instance without interference.
     * 
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void multipleCollectionOperatorsShouldCoexist(
            @ForAll @StringLength(min = 1, max = 50) String containsValue,
            @ForAll boolean isEmpty) throws IOException {
        
        // Given: A JSON object with multiple collection operators
        // Use ObjectMapper to properly escape the string value
        String escapedValue = objectMapper.writeValueAsString(containsValue);
        String json = String.format("{\"cont\": %s, \"empty\": %b}", escapedValue, isEmpty);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: Both operators should be set correctly
        assertThat(filter.getCollectionContains()).isEqualTo(containsValue);
        assertThat(filter.getIsEmpty()).isEqualTo(isEmpty);
    }
    
    /**
     * Property 1 (variant): Element type deserialization
     * 
     * For any valid element value, the deserialization should use the appropriate
     * type deserializer based on the element type.
     * 
     * Validates: Requirements 1.2
     */
    @Property(tries = 100)
    void collectionContainsShouldDeserializeElementCorrectly(
            @ForAll @IntRange(min = -1000, max = 1000) int elementValue) throws IOException {
        
        // Given: A JSON object with an integer element
        String json = String.format("{\"cont\": %d}", elementValue);
        
        // When: We deserialize the JSON to a CollectionFilter<Integer>
        CollectionFilter<Integer> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, Integer.class));
        
        // Then: The collectionContains property should be set to the integer value
        assertThat(filter.getCollectionContains()).isEqualTo(elementValue);
    }
    
    /**
     * Basic unit test to verify CollectionFilter can be deserialized
     */
    @Test
    void collectionFilterShouldBeDeserializable() throws IOException {
        // Given
        String json = "{\"cont\": \"test\"}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getCollectionContains()).isEqualTo("test");
    }
    
    /**
     * Basic unit test to verify empty operator binding
     */
    @Test
    void emptyOperatorShouldBindToIsEmptyProperty() throws IOException {
        // Given
        String json = "{\"empty\": true}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getIsEmpty()).isTrue();
    }
    
    /**
     * Basic unit test to verify nempty operator binding
     */
    @Test
    void nemptyOperatorShouldBindToIsNotEmptyProperty() throws IOException {
        // Given
        String json = "{\"nempty\": true}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getIsNotEmpty()).isTrue();
    }
}
