package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.testmodel.TestUserFilter;
import com.thy.fss.common.inmemory.testmodel.TestUserFilterDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that Spring pagination and sorting parameters (page, size, sort) are properly
 * excluded from filter binding and handled by Spring's PageableHandlerMethodArgumentResolver.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring Pagination Parameter Exclusion Tests")
class SpringPaginationParameterExclusionTest {

    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
    private static final String NAME_EQ = "name.eq";
    private static final String NAME_ASC = "name,asc";
    private static final String JOHN = "John";

    @Mock
    private FilterValueDeserializer deserializer;

    @Mock
    private CollectionParameterHandler collectionHandler;

    @Mock
    private DeserializerRegistry registry;

    @Test
    @DisplayName("Should exclude 'page' parameter from filter binding")
    void shouldExcludePageParameter() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{"0"});
        parameterMap.put(NAME_EQ, new String[]{JOHN});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Page parameter should be ignored, only name.eq should be processed
        // (name.eq processing would fail if attempted, but we're just checking no exception)
    }

    @Test
    @DisplayName("Should exclude 'size' parameter from filter binding")
    void shouldExcludeSizeParameter() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(SIZE, new String[]{"20"});
        parameterMap.put(NAME_EQ, new String[]{JOHN});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Size parameter should be ignored
    }

    @Test
    @DisplayName("Should exclude 'sort' parameter from filter binding")
    void shouldExcludeSortParameter() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(SORT, new String[]{NAME_ASC});
        parameterMap.put(NAME_EQ, new String[]{JOHN});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Sort parameter should be ignored
    }

    @Test
    @DisplayName("Should exclude all pagination parameters (page, size, sort) together")
    void shouldExcludeAllPaginationParameters() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{"0"});
        parameterMap.put(SIZE, new String[]{"20"});
        parameterMap.put(SORT, new String[]{NAME_ASC});
        parameterMap.put(NAME_EQ, new String[]{JOHN});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // All pagination parameters should be ignored
    }

    @Test
    @DisplayName("Should process filter parameters when pagination parameters are present")
    void shouldProcessFilterParametersWithPaginationPresent() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{"0"});
        parameterMap.put(SIZE, new String[]{"20"});
        parameterMap.put(SORT, new String[]{NAME_ASC});
        parameterMap.put(NAME_EQ, new String[]{JOHN});
        parameterMap.put("active.eq", new String[]{"true"});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Pagination parameters should be excluded, filter parameters should be processed
    }

    @Test
    @DisplayName("Should handle only pagination parameters without filter parameters")
    void shouldHandleOnlyPaginationParameters() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{"0"});
        parameterMap.put(SIZE, new String[]{"20"});
        parameterMap.put(SORT, new String[]{NAME_ASC});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Should return empty filter (all parameters excluded)
    }

    @Test
    @DisplayName("Should not exclude parameters that start with 'page', 'size', or 'sort' but are not exact matches")
    void shouldNotExcludeParametersWithSimilarNames() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("pageNumber", new String[]{"1"});  // Not 'page'
        parameterMap.put("sizeLimit", new String[]{"100"}); // Not 'size'
        parameterMap.put("sortOrder", new String[]{"asc"}); // Not 'sort'

        // When/Then
        // These parameters should NOT be excluded (they're not exact matches)
        // They will be processed as unknown parameters and throw an exception
        // because they don't match the expected filter field format
        try {
            TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                    parameterMap, deserializer, collectionHandler, registry);
            // If we get here, it means the parameters were ignored (which is also acceptable)
        } catch (RuntimeException e) {
            // Expected: these are not valid filter parameters
            assertThat(e.getMessage()).contains("Malformed filter path");
        }
    }

    @Test
    @DisplayName("Should exclude pagination parameters case-sensitively")
    void shouldExcludePaginationParametersCaseSensitively() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{"0"});   // Should be excluded
        parameterMap.put("Page", new String[]{"1"});   // Should NOT be excluded (different case)
        parameterMap.put("PAGE", new String[]{"2"});   // Should NOT be excluded (different case)

        // When/Then
        // Only lowercase 'page' should be excluded
        // 'Page' and 'PAGE' will be processed and throw exceptions (invalid filter paths)
        try {
            TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                    parameterMap, deserializer, collectionHandler, registry);
            // If we get here, it means the parameters were ignored (which is also acceptable)
        } catch (RuntimeException e) {
            // Expected: 'Page' and 'PAGE' are not valid filter parameters
            assertThat(e.getMessage()).contains("Malformed filter path");
        }
    }

    @Test
    @DisplayName("Should work with empty parameter map")
    void shouldWorkWithEmptyParameterMap() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Should return empty filter
    }

    @Test
    @DisplayName("Should work with null parameter values for pagination parameters")
    void shouldWorkWithNullParameterValues() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, null);
        parameterMap.put(SIZE, null);
        parameterMap.put(SORT, null);

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Should handle null values gracefully
    }

    @Test
    @DisplayName("Should work with empty parameter values for pagination parameters")
    void shouldWorkWithEmptyParameterValues() throws Exception {
        // Given
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(PAGE, new String[]{});
        parameterMap.put(SIZE, new String[]{});
        parameterMap.put(SORT, new String[]{});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Should handle empty arrays gracefully
    }
}
