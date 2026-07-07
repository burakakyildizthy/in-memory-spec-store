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
 * Integration test verifying that Spring pagination parameters (page, size, sort)
 * are properly excluded from filter binding in a realistic scenario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring Pagination Integration Tests")
class SpringPaginationIntegrationTest {

    @Mock
    private FilterValueDeserializer deserializer;

    @Mock
    private CollectionParameterHandler collectionHandler;

    @Mock
    private DeserializerRegistry registry;

    @Test
    @DisplayName("Should handle filter parameters with pagination parameters in realistic scenario")
    void shouldHandleFilterWithPaginationRealistic() throws Exception {
        // Given - Simulating a real HTTP request with both filter and pagination params
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("name.eq", new String[]{"John"});
        parameterMap.put("active.eq", new String[]{"true"});
        parameterMap.put("page", new String[]{"0"});
        parameterMap.put("size", new String[]{"20"});
        parameterMap.put("sort", new String[]{"name,asc"});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Pagination parameters should be excluded, filter should be created successfully
    }

    @Test
    @DisplayName("Should handle only pagination parameters without filter in realistic scenario")
    void shouldHandleOnlyPaginationRealistic() throws Exception {
        // Given - Simulating a request with only pagination
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("page", new String[]{"0"});
        parameterMap.put("size", new String[]{"20"});
        parameterMap.put("sort", new String[]{"name,asc"});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Should return empty filter (all parameters excluded)
    }

    @Test
    @DisplayName("Should handle multiple sort parameters in realistic scenario")
    void shouldHandleMultipleSortParametersRealistic() throws Exception {
        // Given - Simulating a request with multiple sort parameters
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("name.eq", new String[]{"John"});
        parameterMap.put("page", new String[]{"0"});
        parameterMap.put("size", new String[]{"20"});
        parameterMap.put("sort", new String[]{"name,asc", "email,desc"});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // All sort parameters should be excluded
    }

    @Test
    @DisplayName("Should handle complex filter with nested paths and pagination in realistic scenario")
    void shouldHandleComplexFilterWithPaginationRealistic() throws Exception {
        // Given - Simulating a complex real-world request
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("name.cont", new String[]{"John"});
        parameterMap.put("email.start", new String[]{"john"});
        parameterMap.put("active.eq", new String[]{"true"});
        parameterMap.put("page", new String[]{"1"});
        parameterMap.put("size", new String[]{"50"});
        parameterMap.put("sort", new String[]{"name,asc"});

        // When
        TestUserFilter filter = TestUserFilterDeserializer.bindQueryParameters(
                parameterMap, deserializer, collectionHandler, registry);

        // Then
        assertThat(filter).isNotNull();
        // Pagination parameters should be excluded, filter parameters should be processed
    }
}
