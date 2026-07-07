package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilterDeserializer;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for multiple collection operators coexistence.
 * 
 * Feature: collection-filter-web-binding, Property 2: Multiple collection operators coexistence
 * Validates: Requirements 1.3
 * 
 * Property: For any collection filter field and any valid combination of collection operators,
 * when multiple operators are used on the same field, all operators should be correctly applied
 * to the CollectionFilter instance.
 */
class CollectionFilterMultipleOperatorsPropertyTest {
    
    private final FilterValueDeserializer deserializer = new FilterValueDeserializerImpl();
    private final CollectionParameterHandler collectionHandler = new CollectionParameterHandlerImpl(deserializer);
    private final DeserializerRegistry registry = new DeserializerRegistryImpl();
    
    /**
     * Property 2: Multiple collection operators coexistence
     * 
     * For any collection filter field and any valid combination of collection operators,
     * when multiple operators are used on the same field, all operators should be correctly
     * applied to the CollectionFilter instance without interference.
     */
    @Property(tries = 100)
    void multipleOperatorsOnSameFieldShouldCoexist(
            @ForAll @NotEmpty String containsValue,
            @ForAll boolean isEmptyValue,
            @ForAll boolean isNotEmptyValue
    ) {
        // Given: Multiple operators on the same collection field
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{containsValue});
        parameterMap.put("tags.empty", new String[]{String.valueOf(isEmptyValue)});
        parameterMap.put("tags.nempty", new String[]{String.valueOf(isNotEmptyValue)});
        
        // When: Binding filter from query parameters
        CollectionTestEntityFilter filter = bindFilterFromQueryParams(parameterMap);
        
        // Then: All operators should be applied to the same CollectionFilter instance
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionContains()).isEqualTo(containsValue);
        assertThat(filter.getTags().getIsEmpty()).isEqualTo(isEmptyValue);
        assertThat(filter.getTags().getIsNotEmpty()).isEqualTo(isNotEmptyValue);
    }
    
    /**
     * Property: Multiple collection fields with different operators should not interfere
     */
    @Property(tries = 100)
    void multipleCollectionFieldsShouldNotInterfere(
            @ForAll @NotEmpty String tagsValue,
            @ForAll @IntRange(min = 0, max = 1000) int scoresValue,
            @ForAll boolean prioritiesIsEmpty
    ) {
        // Given: Operators on multiple different collection fields
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{tagsValue});
        parameterMap.put("scores.cont", new String[]{String.valueOf(scoresValue)});
        parameterMap.put("priorities.empty", new String[]{String.valueOf(prioritiesIsEmpty)});
        
        // When: Binding filter from query parameters
        CollectionTestEntityFilter filter = bindFilterFromQueryParams(parameterMap);
        
        // Then: Each collection field should have its own filter with correct values
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionContains()).isEqualTo(tagsValue);
        
        assertThat(filter.getScores()).isNotNull();
        assertThat(filter.getScores().getCollectionContains()).isEqualTo(scoresValue);
        
        assertThat(filter.getPriorities()).isNotNull();
        assertThat(filter.getPriorities().getIsEmpty()).isEqualTo(prioritiesIsEmpty);
    }
    
    /**
     * Property: Combining direct and nested operators should work correctly
     */
    @Property(tries = 100)
    void directAndNestedOperatorsShouldCoexist(
            @ForAll @NotEmpty String containsValue,
            @ForAll @NotEmpty String nestedContainsValue,
            @ForAll boolean isNotEmpty
    ) {
        // Given: Both direct and nested operators on the same field
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{containsValue});
        parameterMap.put("tags.any.cont", new String[]{nestedContainsValue});
        parameterMap.put("tags.nempty", new String[]{String.valueOf(isNotEmpty)});
        
        // When: Binding filter from query parameters
        CollectionTestEntityFilter filter = bindFilterFromQueryParams(parameterMap);
        
        // Then: Both direct and nested operators should be set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionContains()).isEqualTo(containsValue);
        assertThat(filter.getTags().getCollectionAny()).isNotNull();
        assertThat(filter.getTags().getIsNotEmpty()).isEqualTo(isNotEmpty);
    }
    
    /**
     * Property: Order of operators in parameter map should not affect result
     */
    @Property(tries = 100)
    void operatorOrderShouldNotMatter(
            @ForAll @NotEmpty String value1,
            @ForAll @NotEmpty String value2,
            @ForAll boolean boolValue
    ) {
        // Given: Same operators in different orders
        Map<String, String[]> parameterMap1 = new HashMap<>();
        parameterMap1.put("tags.cont", new String[]{value1});
        parameterMap1.put("tags.any.cont", new String[]{value2});
        parameterMap1.put("tags.nempty", new String[]{String.valueOf(boolValue)});
        
        Map<String, String[]> parameterMap2 = new HashMap<>();
        parameterMap2.put("tags.nempty", new String[]{String.valueOf(boolValue)});
        parameterMap2.put("tags.any.cont", new String[]{value2});
        parameterMap2.put("tags.cont", new String[]{value1});
        
        // When: Binding filters from both parameter maps
        CollectionTestEntityFilter filter1 = bindFilterFromQueryParams(parameterMap1);
        CollectionTestEntityFilter filter2 = bindFilterFromQueryParams(parameterMap2);
        
        // Then: Both filters should have identical values
        assertThat(filter1.getTags().getCollectionContains())
            .isEqualTo(filter2.getTags().getCollectionContains());
        assertThat(filter1.getTags().getIsNotEmpty())
            .isEqualTo(filter2.getTags().getIsNotEmpty());
        // Note: We can't directly compare nested filters, but we can verify they're both set
        assertThat(filter1.getTags().getCollectionAny()).isNotNull();
        assertThat(filter2.getTags().getCollectionAny()).isNotNull();
    }
    
    /**
     * Property: Empty parameter map should result in null collection filters
     */
    @Property(tries = 50)
    void emptyParameterMapShouldResultInNullFilters() {
        // Given: Empty parameter map
        Map<String, String[]> parameterMap = new HashMap<>();
        
        // When: Binding filter from empty parameters
        CollectionTestEntityFilter filter = bindFilterFromQueryParams(parameterMap);
        
        // Then: All collection filters should be null
        assertThat(filter.getTags()).isNull();
        assertThat(filter.getScores()).isNull();
        assertThat(filter.getPriorities()).isNull();
    }
    
    /**
     * Property: Operators on non-collection fields should not affect collection fields
     */
    @Property(tries = 100)
    void nonCollectionFieldsShouldNotAffectCollectionFields(
            @ForAll @NotEmpty String nameValue,
            @ForAll @NotEmpty String tagsValue
    ) {
        // Given: Operators on both collection and non-collection fields
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("name.cont", new String[]{nameValue});
        parameterMap.put("tags.cont", new String[]{tagsValue});
        
        // When: Binding filter from query parameters
        CollectionTestEntityFilter filter = bindFilterFromQueryParams(parameterMap);
        
        // Then: Both fields should be set independently
        assertThat(filter.getName()).isNotNull();
        assertThat(filter.getName().getContains()).isEqualTo(nameValue);
        
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionContains()).isEqualTo(tagsValue);
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private CollectionTestEntityFilter bindFilterFromQueryParams(Map<String, String[]> parameterMap) {
        return CollectionTestEntityFilterDeserializer.bindQueryParameters(
            parameterMap, deserializer, collectionHandler, registry
        );
    }
}

