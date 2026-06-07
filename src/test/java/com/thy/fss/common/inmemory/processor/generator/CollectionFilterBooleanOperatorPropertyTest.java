package com.thy.fss.common.inmemory.processor.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for collection filter boolean operator binding.
 * 
 * <p><b>Feature: collection-filter-web-binding, Property 3: Boolean collection operator binding</b></p>
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 2.4</b></p>
 * 
 * <p>This test verifies that the collection filter deserialization correctly binds
 * boolean operators (empty, nempty) to their corresponding properties.</p>
 */
class CollectionFilterBooleanOperatorPropertyTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Property 3: Boolean collection operator binding
     * 
     * For any collection filter field and any boolean value ("true" or "false"), when the
     * 'empty' or 'nempty' operator is used, the corresponding property (isEmpty or isNotEmpty)
     * should be set to the parsed boolean value.
     * 
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4
     */
    @Property(tries = 100)
    void emptyOperatorShouldBindToIsEmptyProperty(
            @ForAll boolean emptyValue) throws IOException {
        
        // Given: A JSON object with the 'empty' operator
        String json = String.format("{\"empty\": %b}", emptyValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The isEmpty property should be set to the boolean value
        assertThat(filter.getIsEmpty()).isEqualTo(emptyValue);
    }
    
    /**
     * Property 3 (variant): nempty operator binding
     * 
     * For any boolean value, the 'nempty' operator should correctly bind to the
     * isNotEmpty property.
     * 
     * Validates: Requirements 2.3, 2.4
     */
    @Property(tries = 100)
    void nemptyOperatorShouldBindToIsNotEmptyProperty(
            @ForAll boolean notEmptyValue) throws IOException {
        
        // Given: A JSON object with the 'nempty' operator
        String json = String.format("{\"nempty\": %b}", notEmptyValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The isNotEmpty property should be set to the boolean value
        assertThat(filter.getIsNotEmpty()).isEqualTo(notEmptyValue);
    }
    
    /**
     * Property 3 (variant): Both boolean operators coexistence
     * 
     * For any combination of boolean values, both 'empty' and 'nempty' operators
     * should be able to coexist without interference.
     * 
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4
     */
    @Property(tries = 100)
    void bothBooleanOperatorsShouldCoexist(
            @ForAll boolean emptyValue,
            @ForAll boolean notEmptyValue) throws IOException {
        
        // Given: A JSON object with both 'empty' and 'nempty' operators
        String json = String.format("{\"empty\": %b, \"nempty\": %b}", emptyValue, notEmptyValue);
        
        // When: We deserialize the JSON to a CollectionFilter
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: Both properties should be set correctly
        assertThat(filter.getIsEmpty()).isEqualTo(emptyValue);
        assertThat(filter.getIsNotEmpty()).isEqualTo(notEmptyValue);
    }
    
    /**
     * Property 3 (variant): Boolean operators with true value
     * 
     * When the boolean value is true, the properties should be set to true.
     * 
     * Validates: Requirements 2.1, 2.3
     */
    @Property(tries = 100)
    void booleanOperatorsWithTrueValueShouldSetPropertiesToTrue() throws IOException {
        
        // Given: JSON objects with true values
        String emptyJson = "{\"empty\": true}";
        String nemptyJson = "{\"nempty\": true}";
        
        // When: We deserialize the JSON to CollectionFilters
        CollectionFilter<String> emptyFilter = objectMapper.readValue(emptyJson, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        CollectionFilter<String> nemptyFilter = objectMapper.readValue(nemptyJson, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The properties should be set to true
        assertThat(emptyFilter.getIsEmpty()).isTrue();
        assertThat(nemptyFilter.getIsNotEmpty()).isTrue();
    }
    
    /**
     * Property 3 (variant): Boolean operators with false value
     * 
     * When the boolean value is false, the properties should be set to false.
     * 
     * Validates: Requirements 2.2, 2.4
     */
    @Property(tries = 100)
    void booleanOperatorsWithFalseValueShouldSetPropertiesToFalse() throws IOException {
        
        // Given: JSON objects with false values
        String emptyJson = "{\"empty\": false}";
        String nemptyJson = "{\"nempty\": false}";
        
        // When: We deserialize the JSON to CollectionFilters
        CollectionFilter<String> emptyFilter = objectMapper.readValue(emptyJson, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        CollectionFilter<String> nemptyFilter = objectMapper.readValue(nemptyJson, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then: The properties should be set to false
        assertThat(emptyFilter.getIsEmpty()).isFalse();
        assertThat(nemptyFilter.getIsNotEmpty()).isFalse();
    }
    
    /**
     * Basic unit test to verify empty operator with true
     */
    @Test
    void emptyOperatorWithTrueShouldSetIsEmptyToTrue() throws IOException {
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
     * Basic unit test to verify empty operator with false
     */
    @Test
    void emptyOperatorWithFalseShouldSetIsEmptyToFalse() throws IOException {
        // Given
        String json = "{\"empty\": false}";
        
        // When
        CollectionFilter<String> filter = objectMapper.readValue(json, 
            objectMapper.getTypeFactory().constructParametricType(CollectionFilter.class, String.class));
        
        // Then
        assertThat(filter).isNotNull();
        assertThat(filter.getIsEmpty()).isFalse();
    }
}
