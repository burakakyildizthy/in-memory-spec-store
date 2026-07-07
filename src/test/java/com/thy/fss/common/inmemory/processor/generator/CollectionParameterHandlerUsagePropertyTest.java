package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for CollectionParameterHandler usage in generated code.
 * 
 * Feature: collection-filter-web-binding, Property 7: CollectionParameterHandler usage
 * Validates: Requirements 5.2
 * 
 * Property: For any collection field operator that requires element deserialization, 
 * the generated code should use the CollectionParameterHandler to parse collection-specific parameters.
 */
class CollectionParameterHandlerUsagePropertyTest {

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
     * Property 7: CollectionParameterHandler usage
     * 
     * For any collection field operator that requires element deserialization,
     * the generated code should use the CollectionParameterHandler.
     */
    @Property(tries = 100)
    @Label("Generated code should use CollectionParameterHandler for collection operators")
    void generatedCodeShouldUseCollectionParameterHandler(
            @ForAll @StringLength(min = 1, max = 50) String elementValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: Query parameters with a collection contains operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{elementValue});
        
        // When: We call bindQueryParameters (which should use CollectionParameterHandler internally)
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The filter should be populated correctly
        assertNotNull(result, "Filter should be created");
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        Object tagsFilter = getTagsMethod.invoke(result);
        assertNotNull(tagsFilter, "Tags filter should be initialized");
        
        // And: The CollectionParameterHandler should have been used to parse the element
        // We verify this indirectly by checking that the filter was populated correctly
        Method getCollectionContainsMethod = tagsFilter.getClass().getMethod("getCollectionContains");
        Object containsValue = getCollectionContainsMethod.invoke(tagsFilter);
        assertEquals(elementValue, containsValue, 
                "CollectionParameterHandler should have deserialized the element value");
    }
    
    /**
     * Property: CollectionParameterHandler should handle integer collection elements
     */
    @Property(tries = 100)
    @Label("CollectionParameterHandler should handle integer elements")
    void collectionParameterHandlerShouldHandleIntegerElements(
            @ForAll @IntRange(min = 0, max = 1000) int elementValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: Query parameters with an integer collection contains operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("numbers.cont", new String[]{String.valueOf(elementValue)});
        
        // When: We call bindQueryParameters
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The filter should be populated with the integer value
        assertNotNull(result, "Filter should be created");
        Method getNumbersMethod = collectionEntityFilterClass.getMethod("getNumbers");
        Object numbersFilter = getNumbersMethod.invoke(result);
        assertNotNull(numbersFilter, "Numbers filter should be initialized");
        
        // And: The value should be correctly deserialized as an integer
        Method getCollectionContainsMethod = numbersFilter.getClass().getMethod("getCollectionContains");
        Object containsValue = getCollectionContainsMethod.invoke(numbersFilter);
        assertEquals(elementValue, containsValue,
                "CollectionParameterHandler should have deserialized the integer element");
    }
    
    /**
     * Property: CollectionParameterHandler should handle empty operator
     */
    @Property(tries = 50)
    @Label("CollectionParameterHandler should handle empty operator")
    void collectionParameterHandlerShouldHandleEmptyOperator(
            @ForAll boolean emptyValue) throws Exception {
        
        if (bindQueryParametersMethod == null) {
            Assume.that(false); // Skip if generated classes not available
            return;
        }
        
        // Given: Query parameters with an empty operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.empty", new String[]{String.valueOf(emptyValue)});
        
        // When: We call bindQueryParameters
        Object result = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );
        
        // Then: The filter should be populated with the empty flag
        assertNotNull(result, "Filter should be created");
        Method getTagsMethod = collectionEntityFilterClass.getMethod("getTags");
        Object tagsFilter = getTagsMethod.invoke(result);
        assertNotNull(tagsFilter, "Tags filter should be initialized");
        
        // And: The isEmpty property should be set correctly
        Method getIsEmptyMethod = tagsFilter.getClass().getMethod("getIsEmpty");
        Object isEmpty = getIsEmptyMethod.invoke(tagsFilter);
        assertEquals(emptyValue, isEmpty,
                "Empty operator should set the isEmpty property correctly");
    }
    
    /**
     * Property: Generated code should accept CollectionParameterHandler as parameter
     */
    @Test
    void generatedBindQueryParametersShouldAcceptCollectionParameterHandler() {
        // Then: The bindQueryParameters method should accept CollectionParameterHandler
        assertNotNull(bindQueryParametersMethod,
                "bindQueryParameters method should exist");
        
        Class<?>[] parameterTypes = bindQueryParametersMethod.getParameterTypes();
        assertEquals(4, parameterTypes.length,
                "bindQueryParameters should have 4 parameters");
        
        boolean hasCollectionParameterHandler = false;
        for (Class<?> paramType : parameterTypes) {
            if (CollectionParameterHandler.class.isAssignableFrom(paramType)) {
                hasCollectionParameterHandler = true;
                break;
            }
        }
        
        assertTrue(hasCollectionParameterHandler,
                "bindQueryParameters should accept CollectionParameterHandler as a parameter");
    }
}
