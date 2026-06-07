package com.thy.fss.common.inmemory.processor.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for collection filter base operator binding.
 * 
 * <p><b>Feature: collection-filter-web-binding, Property 5: Base operator binding on collections</b></p>
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6</b></p>
 * 
 * <p>This test verifies that the collection filter deserialization correctly binds
 * base operators (eq, neq, in, nin, isn, isnn) to their corresponding properties.</p>
 */
class CollectionFilterBaseOperatorPropertyTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Property 5: Base operator binding on collections
     * 
     * For any collection filter field and any base filter operator (eq, neq, in, nin, isn, isnn),
     * the system should correctly parse the parameter value and set the corresponding property
     * on the CollectionFilter.
     * 
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
     */
    @Property(tries = 100)
    void equalsOperatorShouldBindToEqualsProperty(
            @ForAll @Size(min = 1, max = 5) List<@StringLength(min = 1, max = 20) String> collectionValue) throws IOException {
        
        // Given: A JSON object with the 'eq' operator
        String json = String.format("{\"eq\": %s}", objectMapper.writeValueAsString(collectionValue));
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The equals property should be set to the collection value
        assertThat(filter.getEquals()).isEqualTo(collectionValue);
    }
    
    /**
     * Property 5 (variant): neq operator binding
     * 
     * For any collection value, the 'neq' operator should correctly bind to the
     * notEquals property.
     * 
     * Validates: Requirements 4.2
     */
    @Property(tries = 100)
    void notEqualsOperatorShouldBindToNotEqualsProperty(
            @ForAll @Size(min = 1, max = 5) List<@StringLength(min = 1, max = 20) String> collectionValue) throws IOException {
        
        // Given: A JSON object with the 'neq' operator
        String json = String.format("{\"neq\": %s}", objectMapper.writeValueAsString(collectionValue));
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The notEquals property should be set to the collection value
        assertThat(filter.getNotEquals()).isEqualTo(collectionValue);
    }
    
    /**
     * Property 5 (variant): in operator binding
     * 
     * For any collection of collections, the 'in' operator should correctly bind to the
     * in property.
     * 
     * Validates: Requirements 4.3
     */
    @Property(tries = 100)
    void inOperatorShouldBindToInProperty(
            @ForAll @Size(min = 1, max = 3) List<@Size(min = 1, max = 3) List<@StringLength(min = 1, max = 10) String>> collectionsValue) throws IOException {
        
        // Given: A JSON object with the 'in' operator
        String json = String.format("{\"in\": %s}", objectMapper.writeValueAsString(collectionsValue));
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The in property should be set to the collection of collections
        assertThat(filter.getIn()).isEqualTo(collectionsValue);
    }
    
    /**
     * Property 5 (variant): nin operator binding
     * 
     * For any collection of collections, the 'nin' operator should correctly bind to the
     * notIn property.
     * 
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    void notInOperatorShouldBindToNotInProperty(
            @ForAll @Size(min = 1, max = 3) List<@Size(min = 1, max = 3) List<@StringLength(min = 1, max = 10) String>> collectionsValue) throws IOException {
        
        // Given: A JSON object with the 'nin' operator
        String json = String.format("{\"nin\": %s}", objectMapper.writeValueAsString(collectionsValue));
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The notIn property should be set to the collection of collections
        assertThat(filter.getNotIn()).isEqualTo(collectionsValue);
    }
    
    /**
     * Property 5 (variant): isn operator binding
     * 
     * For any boolean value, the 'isn' operator should correctly bind to the
     * isNull property.
     * 
     * Validates: Requirements 4.5
     */
    @Property(tries = 100)
    void isNullOperatorShouldBindToIsNullProperty(
            @ForAll boolean isNullValue) throws IOException {
        
        // Given: A JSON object with the 'isn' operator
        String json = String.format("{\"isn\": %b}", isNullValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The isNull property should be set to the boolean value
        assertThat(filter.getIsNull()).isEqualTo(isNullValue);
    }
    
    /**
     * Property 5 (variant): isnn operator binding
     * 
     * For any boolean value, the 'isnn' operator should correctly bind to the
     * isNotNull property.
     * 
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void isNotNullOperatorShouldBindToIsNotNullProperty(
            @ForAll boolean isNotNullValue) throws IOException {
        
        // Given: A JSON object with the 'isnn' operator
        String json = String.format("{\"isnn\": %b}", isNotNullValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The isNotNull property should be set to the boolean value
        assertThat(filter.getIsNotNull()).isEqualTo(isNotNullValue);
    }
    
    /**
     * Property 5 (variant): Multiple base operators coexistence
     * 
     * For any combination of base operators, all operators should be able to
     * coexist without interference.
     * 
     * Validates: Requirements 4.1, 4.2, 4.5, 4.6
     */
    @Property(tries = 100)
    void multipleBaseOperatorsShouldCoexist(
            @ForAll @Size(min = 1, max = 3) List<@StringLength(min = 1, max = 10) String> equalsValue,
            @ForAll boolean isNullValue) throws IOException {
        
        // Given: A JSON object with multiple base operators
        String json = String.format("{\"eq\": %s, \"isn\": %b}", 
            objectMapper.writeValueAsString(equalsValue), isNullValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: Both properties should be set correctly
        assertThat(filter.getEquals()).isEqualTo(equalsValue);
        assertThat(filter.getIsNull()).isEqualTo(isNullValue);
    }
    
    /**
     * Basic unit test to verify eq operator binding
     */
    @Test
    void equalsOperatorShouldBindCorrectly() throws IOException {
        // Given
        String json = "{\"eq\": [\"a\", \"b\", \"c\"]}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getEquals()).containsExactly("a", "b", "c");
    }
    
    /**
     * Basic unit test to verify isn operator binding
     */
    @Test
    void isNullOperatorShouldBindCorrectly() throws IOException {
        // Given
        String json = "{\"isn\": true}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getIsNull()).isTrue();
    }
}
