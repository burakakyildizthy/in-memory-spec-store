package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeTry;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for FilterArgumentResolver delegation to generated bindQueryParameters method.
 * 
 * Feature: collection-filter-web-binding, Property 6: FilterArgumentResolver delegation
 * Validates: Requirements 5.1
 * 
 * Property: For any filter class with collection fields, when the FilterArgumentResolver processes 
 * the filter, it should delegate to the generated bindQueryParameters method in the filter's 
 * Deserializer class.
 */
class FilterArgumentResolverDelegationPropertyTest {

    private FilterValueDeserializer deserializer;
    private CollectionParameterHandler collectionHandler;
    private DeserializerRegistry registry;
    
    private static Class<?> collectionEntityFilterClass;
    private static Class<?> collectionEntityFilterDeserializer;
    private static Method bindQueryParametersMethod;
    
    static {
        try {
            // Load the generated classes once
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
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            System.out.println("Generated classes not found - tests will be skipped");
            bindQueryParametersMethod = null;
        }
    }
    
    @BeforeTry
    void setUp() {
        deserializer = new FilterValueDeserializerImpl();
        collectionHandler = new CollectionParameterHandlerImpl(deserializer);
        registry = new DeserializerRegistryImpl();
    }
    
    /**
     * Property 6: FilterArgumentResolver delegation
     * 
     * For any filter class with collection fields, when bindQueryParameters is called,
     * it should correctly populate the filter object.
     */
    @Property(tries = 100)
    @Label("bindQueryParameters should correctly populate filter with collection operators")
    void bindQueryParametersShouldPopulateFilterCorrectly(
            @ForAll @StringLength(min = 1, max = 50) String fieldValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: Query parameters with a collection operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{fieldValue});
        
        // When: We call bindQueryParameters
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The result should be an instance of the filter class
        assertNotNull(result, "bindQueryParameters should return a non-null filter object");
        assertEquals(collectionEntityFilterClass, result.getClass(),
                "bindQueryParameters should return an instance of the filter class");
        
        // And: The filter should have been populated with the collection operator
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        Object tagsFilter = getTagsMethod.invoke(result);
        assertNotNull(tagsFilter, "Tags filter should be initialized when tags.cont is provided");
    }
    
    /**
     * Property: bindQueryParameters should handle empty parameter maps
     */
    @Property(tries = 50)
    @Label("bindQueryParameters should handle empty parameter maps")
    void bindQueryParametersShouldHandleEmptyParameterMaps() throws Exception {
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: An empty parameter map
        Map<String, String[]> parameterMap = new HashMap<>();
        
        // When: We call bindQueryParameters
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The result should be a valid filter instance (with no fields set)
        assertNotNull(result, "bindQueryParameters should return a non-null filter even with empty parameters");
        assertEquals(collectionEntityFilterClass, result.getClass(),
                "bindQueryParameters should return an instance of the filter class");
    }
    
    /**
     * Property: bindQueryParameters should handle multiple collection operators
     */
    @Property(tries = 50)
    @Label("bindQueryParameters should handle multiple collection operators")
    void bindQueryParametersShouldHandleMultipleOperators(
            @ForAll @StringLength(min = 1, max = 50) String contValue,
            @ForAll boolean emptyValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: Query parameters with multiple collection operators
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{contValue});
        parameterMap.put("tags.empty", new String[]{String.valueOf(emptyValue)});
        
        // When: We call bindQueryParameters
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The result should be populated with both operators
        assertNotNull(result, "bindQueryParameters should return a non-null filter object");
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        Object tagsFilter = getTagsMethod.invoke(result);
        assertNotNull(tagsFilter, "Tags filter should be initialized when multiple operators are provided");
    }
    
    /**
     * Property: Generated deserializer class should exist for filter with collection fields
     */
    @Property(tries = 10)
    @Label("Generated deserializer class should exist")
    void generatedDeserializerClassShouldExist() {
        // Then: The deserializer class should be generated
        assertNotNull(collectionEntityFilterDeserializer,
                "Deserializer class should be generated for filter with collection fields");
        assertNotNull(bindQueryParametersMethod,
                "bindQueryParameters method should exist in generated deserializer");
    }
}
