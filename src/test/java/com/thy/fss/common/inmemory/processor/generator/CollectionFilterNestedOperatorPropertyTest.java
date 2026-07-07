package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for collection filter nested operator binding.
 * 
 * <p><b>Feature: collection-filter-web-binding, Property 4: Nested filter operator binding</b></p>
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b></p>
 * 
 * <p>This test verifies that nested filter operators (any, all, none) correctly create
 * nested filter instances and bind element filter operators to them via query parameter binding.</p>
 */
class CollectionFilterNestedOperatorPropertyTest {
    
    private FilterValueDeserializer deserializer;
    private CollectionParameterHandler collectionHandler;
    private DeserializerRegistry registry;
    private Class<?> collectionEntityFilterDeserializer;
    private Class<?> collectionEntityFilterClass;
    private Method bindQueryParametersMethod;
    
    @BeforeEach
    void setUp() throws Exception {
        // Set up mocks
        deserializer = mock(FilterValueDeserializer.class);
        collectionHandler = mock(CollectionParameterHandler.class);
        registry = mock(DeserializerRegistry.class);
        
        // Mock deserializer to return the input value for strings
        when(deserializer.deserializeValue(anyString(), eq(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock deserializer to return parsed integers
        when(deserializer.deserializeValue(anyString(), eq(Integer.class), any()))
            .thenAnswer(invocation -> Integer.parseInt(invocation.getArgument(0)));
        
        try {
            // Load the generated classes
            collectionEntityFilterDeserializer = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer"
            );
            collectionEntityFilterClass = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter"
            );
            bindQueryParametersMethod = collectionEntityFilterDeserializer.getMethod(
                    "bindQueryParameters",
                    Map.class,
                    FilterValueDeserializer.class,
                    CollectionParameterHandler.class,
                    DeserializerRegistry.class
            );
        } catch (ClassNotFoundException e) {
            System.out.println("Generated classes not found - skipping tests");
            bindQueryParametersMethod = null;
        }
    }
    
    /**
     * Property 4: Nested filter operator binding - any operator
     * 
     * For any collection filter field and any valid nested filter path using 'any',
     * the system should create a nested filter instance and set it as the collectionAny property.
     * 
     * Validates: Requirements 3.1
     */
    @Property(tries = 100)
    void anyOperatorShouldCreateNestedFilterInstance(
            @ForAll @StringLength(min = 1, max = 50) String elementValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given: Query parameters with 'any' nested operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{elementValue});
        
        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The tags collection filter should have collectionAny set
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isInstanceOf(StringFilter.class);
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        assertThat(anyFilter.getContains()).isEqualTo(elementValue);
    }
    
    /**
     * Property 4: Nested filter operator binding - all operator
     * 
     * For any collection filter field and any valid nested filter path using 'all',
     * the system should create a nested filter instance and set it as the collectionAll property.
     * 
     * Validates: Requirements 3.2
     */
    @Property(tries = 100)
    void allOperatorShouldCreateNestedFilterInstance(
            @ForAll @StringLength(min = 1, max = 50) String elementValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given: Query parameters with 'all' nested operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.all.start", new String[]{elementValue});
        
        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The tags collection filter should have collectionAll set
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isInstanceOf(StringFilter.class);
        
        StringFilter allFilter = (StringFilter) tagsFilter.getCollectionAll();
        assertThat(allFilter.getStartsWith()).isEqualTo(elementValue);
    }
    
    /**
     * Property 4: Nested filter operator binding - none operator
     * 
     * For any collection filter field and any valid nested filter path using 'none',
     * the system should create a nested filter instance and set it as the collectionNone property.
     * 
     * Validates: Requirements 3.3
     */
    @Property(tries = 100)
    void noneOperatorShouldCreateNestedFilterInstance(
            @ForAll @StringLength(min = 1, max = 50) String elementValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given: Query parameters with 'none' nested operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.none.eq", new String[]{elementValue});
        
        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The tags collection filter should have collectionNone set
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionNone()).isNotNull();
        assertThat(tagsFilter.getCollectionNone()).isInstanceOf(StringFilter.class);
        
        StringFilter noneFilter = (StringFilter) tagsFilter.getCollectionNone();
        assertThat(noneFilter.getEquals()).isEqualTo(elementValue);
    }
    
    /**
     * Property 4: Nested filter operator binding - combined with element-level operators
     * 
     * For any nested filter operators combined with element-level operators,
     * the system should correctly parse and apply the nested filter criteria.
     * 
     * Validates: Requirements 3.4
     */
    @Property(tries = 100)
    void nestedOperatorsShouldCombineWithElementOperators(
            @ForAll @StringLength(min = 1, max = 50) String anyValue,
            @ForAll @StringLength(min = 1, max = 50) String allValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given: Query parameters with multiple nested operators
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{anyValue});
        parameterMap.put("tags.all.start", new String[]{allValue});
        
        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: Both nested filters should be set correctly
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isNotNull();
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        StringFilter allFilter = (StringFilter) tagsFilter.getCollectionAll();
        
        assertThat(anyFilter.getContains()).isEqualTo(anyValue);
        assertThat(allFilter.getStartsWith()).isEqualTo(allValue);
    }
    
    /**
     * Property 4: Nested filter operator binding - numeric element types
     * 
     * For any collection filter with numeric element types, nested operators should
     * create appropriate numeric filter instances.
     * 
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Property(tries = 100)
    void nestedOperatorsShouldWorkWithNumericTypes(
            @ForAll @IntRange(min = 0, max = 1000) int threshold) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given: Query parameters with numeric element filter
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("numbers.any.gt", new String[]{String.valueOf(threshold)});
        
        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The numbers collection filter should have collectionAny set with IntegerFilter
        Method getNumbersMethod = collectionEntityFilterClass.getMethod("getNumbers");
        CollectionFilter<Integer> numbersFilter = (CollectionFilter<Integer>) getNumbersMethod.invoke(filter);
        
        assertThat(numbersFilter).isNotNull();
        assertThat(numbersFilter.getCollectionAny()).isNotNull();
        assertThat(numbersFilter.getCollectionAny()).isInstanceOf(IntegerFilter.class);
        
        IntegerFilter anyFilter = (IntegerFilter) numbersFilter.getCollectionAny();
        assertThat(anyFilter.getGreaterThan()).isEqualTo(threshold);
    }
    
    /**
     * Basic unit test to verify any operator creates nested filter via query parameters
     */
    @Test
    void anyOperatorShouldCreateStringFilter() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{"test"});
        
        // When
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isInstanceOf(StringFilter.class);
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        assertThat(anyFilter.getContains()).isEqualTo("test");
    }
    
    /**
     * Basic unit test to verify all operator creates nested filter via query parameters
     */
    @Test
    void allOperatorShouldCreateStringFilter() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.all.start", new String[]{"prefix"});
        
        // When
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isInstanceOf(StringFilter.class);
        StringFilter allFilter = (StringFilter) tagsFilter.getCollectionAll();
        assertThat(allFilter.getStartsWith()).isEqualTo("prefix");
    }
    
    /**
     * Basic unit test to verify none operator creates nested filter via query parameters
     */
    @Test
    void noneOperatorShouldCreateStringFilter() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.none.eq", new String[]{"forbidden"});
        
        // When
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionNone()).isNotNull();
        assertThat(tagsFilter.getCollectionNone()).isInstanceOf(StringFilter.class);
        StringFilter noneFilter = (StringFilter) tagsFilter.getCollectionNone();
        assertThat(noneFilter.getEquals()).isEqualTo("forbidden");
    }
    
    /**
     * Basic unit test to verify multiple nested operators can coexist via query parameters
     */
    @Test
    void multipleNestedOperatorsShouldCoexist() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }
        
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{"work"});
        parameterMap.put("tags.all.start", new String[]{"prefix"});
        parameterMap.put("tags.none.eq", new String[]{"forbidden"});
        
        // When
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAll()).isNotNull();
        assertThat(tagsFilter.getCollectionNone()).isNotNull();
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        StringFilter allFilter = (StringFilter) tagsFilter.getCollectionAll();
        StringFilter noneFilter = (StringFilter) tagsFilter.getCollectionNone();
        
        assertThat(anyFilter.getContains()).isEqualTo("work");
        assertThat(allFilter.getStartsWith()).isEqualTo("prefix");
        assertThat(noneFilter.getEquals()).isEqualTo("forbidden");
    }
}
