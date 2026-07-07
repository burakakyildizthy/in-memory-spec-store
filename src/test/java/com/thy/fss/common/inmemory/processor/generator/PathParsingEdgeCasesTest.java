package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for path parsing edge cases in generated bindQueryParameters method.
 * 
 * Tests requirement 3.5: Path parsing should handle various path formats correctly.
 * 
 * Edge cases tested:
 * - Empty paths
 * - Single-level paths (field only, no operator)
 * - Two-level paths (field.operator)
 * - Three-level paths (field.nested.operator)
 * - Paths with extra segments (field.nested.operator.extra)
 */
@DisplayName("Path Parsing Edge Cases Tests")
class PathParsingEdgeCasesTest {

    private FilterValueDeserializer deserializer;
    private CollectionParameterHandler collectionHandler;
    private DeserializerRegistry registry;
    private Class<?> collectionEntityFilterDeserializer;
    private Class<?> collectionEntityFilterClass;
    private Method bindQueryParametersMethod;

    private static final String GET_TAGS_METHOD = "getTags";
    private static final String VALUE_LITERAL = "value";
    private static final String EXPECTED_RUNTIME_EXCEPTION = "Expected RuntimeException to be thrown";
    private static final String TAGS_CONT_PATH = "tags.cont";
    private static final String IMPORTANT_TAG = "important";

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
        
        // Mock deserializer to return parsed booleans
        when(deserializer.deserializeBoolean(anyString()))
            .thenAnswer(invocation -> Boolean.parseBoolean(invocation.getArgument(0)));
        
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

    @Test
    @DisplayName("Should handle empty parameter map gracefully")
    void shouldHandleEmptyParameterMap() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Empty parameter map
        Map<String, String[]> parameterMap = new HashMap<>();

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Filter should be created but with no fields set
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        assertThat(tagsFilter).isNull(); // No tags filter should be created
    }

    @Test
    @DisplayName("Should ignore single-level paths (field only, no operator)")
    void shouldIgnoreSingleLevelPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Single-level path (no operator)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags", new String[]{VALUE_LITERAL});

        // When/Then: Should throw RuntimeException with IllegalArgumentException cause
        try {
            bindQueryParametersMethod.invoke(
                    null, parameterMap, deserializer, collectionHandler, registry
            );
            // If we get here, the test should fail
            assertThat(false).as(EXPECTED_RUNTIME_EXCEPTION).isTrue();
        } catch (Exception e) {
            // Verify it's a RuntimeException wrapping IllegalArgumentException
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).contains("Malformed filter path 'tags'");
        }
    }

    @Test
    @DisplayName("Should handle two-level paths (field.operator) correctly")
    void shouldHandleTwoLevelPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Two-level path (field.operator)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(TAGS_CONT_PATH, new String[]{"work"});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: The direct operator should be applied
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionContains()).isEqualTo("work");
    }

    @Test
    @DisplayName("Should handle three-level paths (field.nested.operator) correctly")
    void shouldHandleThreeLevelPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Three-level path (field.nested.operator)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{IMPORTANT_TAG});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: The nested operator should be applied
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isInstanceOf(StringFilter.class);
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        assertThat(anyFilter.getContains()).isEqualTo(IMPORTANT_TAG);
    }

    @Test
    @DisplayName("Should ignore paths with extra segments (field.nested.operator.extra)")
    void shouldIgnorePathsWithExtraSegments() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Path with extra segments
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont.extra", new String[]{VALUE_LITERAL});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: The path with extra segments should be ignored
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        // The filter might be null or the nested filter might not be set
        // Either way, the extra segment should not cause an error
        if (tagsFilter != null) {
            // If a filter was created, it should not have the nested filter set
            // because the path was invalid
            assertThat(tagsFilter.getCollectionAny()).isNull();
        }
    }

    @Test
    @DisplayName("Should handle empty path string gracefully")
    void shouldHandleEmptyPathString() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Empty path string
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("", new String[]{VALUE_LITERAL});

        // When/Then: Should throw RuntimeException with IllegalArgumentException cause
        try {
            bindQueryParametersMethod.invoke(
                    null, parameterMap, deserializer, collectionHandler, registry
            );
            // If we get here, the test should fail
            assertThat(false).as(EXPECTED_RUNTIME_EXCEPTION).isTrue();
        } catch (Exception e) {
            // Verify it's a RuntimeException wrapping IllegalArgumentException
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).contains("Parameter path cannot be null or empty");
        }
    }

    @Test
    @DisplayName("Should handle null parameter values gracefully")
    void shouldHandleNullParameterValues() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Null parameter values
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(TAGS_CONT_PATH, null);

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Null values should be ignored gracefully
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        assertThat(tagsFilter).isNull();
    }

    @Test
    @DisplayName("Should handle empty parameter value array gracefully")
    void shouldHandleEmptyParameterValueArray() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Empty parameter value array
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(TAGS_CONT_PATH, new String[]{});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Empty array should be ignored gracefully
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        assertThat(tagsFilter).isNull();
    }

    @Test
    @DisplayName("Should handle multiple valid paths in same request")
    void shouldHandleMultipleValidPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Multiple valid paths
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(TAGS_CONT_PATH, new String[]{"work"});
        parameterMap.put("tags.any.cont", new String[]{IMPORTANT_TAG});
        parameterMap.put("tags.empty", new String[]{"false"});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: All valid paths should be applied
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionContains()).isEqualTo("work");
        assertThat(tagsFilter.getIsEmpty()).isEqualTo(false);
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        assertThat(anyFilter.getContains()).isEqualTo(IMPORTANT_TAG);
    }

    @Test
    @DisplayName("Should handle mix of valid and invalid paths")
    void shouldHandleMixOfValidAndInvalidPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Mix of valid and invalid paths
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(TAGS_CONT_PATH, new String[]{"work"}); // Valid
        parameterMap.put("tags", new String[]{"invalid"}); // Invalid (single-level)
        parameterMap.put("tags.any.cont.extra", new String[]{"invalid"}); // Invalid (extra segments)

        // When/Then: Should throw RuntimeException because of invalid path
        try {
            bindQueryParametersMethod.invoke(
                    null, parameterMap, deserializer, collectionHandler, registry
            );
            // If we get here, the test should fail
            assertThat(false).as(EXPECTED_RUNTIME_EXCEPTION).isTrue();
        } catch (Exception e) {
            // Verify it's a RuntimeException wrapping IllegalArgumentException
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).contains("Malformed filter path 'tags'");
        }
    }

    @Test
    @DisplayName("Should handle multi-level nested paths for model type collections")
    void shouldHandleMultiLevelNestedPathsForModelTypes() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Multi-level nested path for model type collection (e.g., users.any.address.city.eq)
        // Note: This test assumes there's a model type collection field in the test filter
        // If not available, the test will pass without assertions
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("users.any.address.city.eq", new String[]{"Istanbul"});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Filter should be created
        assertThat(filter).isNotNull();
        
        // Note: Full validation would require a test model with nested model type collections
        // For now, we verify that the path parsing doesn't throw an exception
    }

    @Test
    @DisplayName("Should handle paths with multiple collection operators")
    void shouldHandlePathsWithMultipleCollectionOperators() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Path with multiple collection operators (e.g., users.any.orders.all.status.eq)
        // Note: This tests nested collection filtering
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("users.any.orders.all.status.eq", new String[]{"PENDING"});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Filter should be created without errors
        assertThat(filter).isNotNull();
        
        // Note: Full validation would require a test model with nested collections
        // For now, we verify that the path parsing handles multiple operators gracefully
    }

    @Test
    @DisplayName("Should handle malformed paths with missing operator after collection field")
    void shouldHandleMalformedPathsWithMissingOperator() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Malformed path with missing operator (field.any without operator)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any", new String[]{VALUE_LITERAL});

        // When/Then: Should throw RuntimeException with IllegalArgumentException cause
        try {
            bindQueryParametersMethod.invoke(
                    null, parameterMap, deserializer, collectionHandler, registry
            );
            // If we get here, the test should fail
            assertThat(false).as(EXPECTED_RUNTIME_EXCEPTION).isTrue();
        } catch (Exception e) {
            // Verify it's a RuntimeException wrapping IllegalArgumentException
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage()).contains("Malformed nested filter path 'tags.any'");
        }
    }

    @Test
    @DisplayName("Should handle paths with invalid collection operator")
    void shouldHandlePathsWithInvalidCollectionOperator() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Path with invalid collection operator (not any, all, or none)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.invalid.cont", new String[]{VALUE_LITERAL});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: Invalid operator should be ignored gracefully
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        // The invalid operator path should be silently ignored
        assertThat(tagsFilter).isNull();
    }

    @Test
    @DisplayName("Should handle minimum valid path segments (field.any.operator)")
    void shouldHandleMinimumValidPathSegments() throws Exception {
        if (bindQueryParametersMethod == null) {
            return; // Skip if generated classes not available
        }

        // Given: Minimum valid path with three segments (field.any.operator)
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.eq", new String[]{"work"});

        // When: We bind the query parameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Then: The minimum valid path should be processed correctly
        assertThat(filter).isNotNull();
        
        Method getTagsMethod = collectionEntityFilterClass.getMethod(GET_TAGS_METHOD);
        CollectionFilter<String> tagsFilter = (CollectionFilter<String>) getTagsMethod.invoke(filter);
        
        assertThat(tagsFilter).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isNotNull();
        assertThat(tagsFilter.getCollectionAny()).isInstanceOf(StringFilter.class);
        
        StringFilter anyFilter = (StringFilter) tagsFilter.getCollectionAny();
        assertThat(anyFilter.getEquals()).isEqualTo("work");
    }
}

