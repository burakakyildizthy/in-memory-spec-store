package com.thy.fss.common.inmemory.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class RestApiDataSourceTest {
    private static final String TEST_API = "TestAPI";
    private static final String HTTP_TEST_API = "http://test.api";
    private static final String HTTP_TEST_API_HEALTH = "http://test.api/health";
    private static final String HTTP_DEFAULT_API = "http://default.api";
    private static final String DEFAULT_API = "DefaultAPI";
    private static final String ENTITY1 = "Entity1";
    private static final String ENTITY2 = "Entity2";
    private static final String ERROR = "Error";
    private static final String CONNECTION_FAILED = "Connection failed";
    private static final String UNEXPECTED_ERROR = "Unexpected error";
    private static final String OK = "OK";
    private static final String TEST_API_ID = "http://test.api?ids=1";
    private static final String LARGE_DATASET_PARSING = "Large dataset parsing should complete within 10 seconds";

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();
    @Mock
    private RestTemplate mockRestTemplate;
    @Mock
    private ObjectMapper mockObjectMapper;
    private RestApiDataSource<TestEntity> dataSource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataSource = new RestApiDataSource<>(TEST_API, TestEntity.class, HTTP_TEST_API,
                HTTP_TEST_API_HEALTH, mockRestTemplate, mockObjectMapper);
    }

    @AfterEach
    void tearDown() {
        // Tüm aktif CompletableFuture'ları iptal et
        activeFutures.forEach(future -> {
            if (!future.isDone()) {
                future.cancel(true);
            }
        });
        activeFutures.clear();

        // DataSource'u kapat (executor service'i kapatır)
        if (dataSource != null) {
            dataSource.close();
        }

        // ForkJoinPool'un temizlenmesini bekle
        try {
            ForkJoinPool.commonPool().awaitQuiescence(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        // Mock'ları reset et
        reset(mockRestTemplate, mockObjectMapper);
    }

    @Test
    @DisplayName("Should create RestApiDataSource with default configuration")
    void shouldCreateRestApiDataSourceWithDefaultConfiguration() {
        // Execute
        RestApiDataSource<TestEntity> defaultDataSource = new RestApiDataSource<>(
                DEFAULT_API, TestEntity.class, HTTP_DEFAULT_API);

        // Verify
        assertNotNull(defaultDataSource);
        assertEquals(DEFAULT_API, defaultDataSource.getName());
        assertEquals(TestEntity.class, defaultDataSource.getEntityType());
        assertEquals(HTTP_DEFAULT_API, defaultDataSource.getBaseUrl());
        assertNull(defaultDataSource.getHealthEndpoint());
        assertNotNull(defaultDataSource.getRestTemplate());
        assertNotNull(defaultDataSource.getObjectMapper());
    }

    @Test
    @DisplayName("Should handle fetchAllById with null ids")
    void shouldHandleFetchAllByIdWithNullIds() throws Exception {
        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(null);

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());

        // Verify no HTTP calls were made
        verifyNoInteractions(mockRestTemplate);
        verifyNoInteractions(mockObjectMapper);
    }

    @Test
    @DisplayName("Should handle fetchAllById with empty ids")
    void shouldHandleFetchAllByIdWithEmptyIds() throws Exception {
        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(Collections.emptyList());

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());

        // Verify no HTTP calls were made
        verifyNoInteractions(mockRestTemplate);
        verifyNoInteractions(mockObjectMapper);
    }

    @Test
    @DisplayName("Should handle fetchAllById with successful response")
    void shouldHandleFetchAllByIdWithSuccessfulResponse() throws Exception {
        // Use real ObjectMapper for this test to avoid complex mocking
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Setup
        Collection<Object> ids = Arrays.asList(1L, 2L);
        String expectedUrl = "http://test.api?ids=1,2";
        String responseBody = "[{\"id\":1,\"name\":\"Entity1\"},{\"id\":2,\"name\":\"Entity2\"}]";

        ResponseEntity<String> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAllById(ids);

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertEquals(ENTITY1, entities.get(0).getName());
        assertEquals(ENTITY2, entities.get(1).getName());
    }

    @Test
    @DisplayName("Should handle fetchAllById with empty response body")
    void shouldHandleFetchAllByIdWithEmptyResponseBody() throws Exception {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        ResponseEntity<String> mockResponse = new ResponseEntity<>("", HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }

    @Test
    @DisplayName("Should handle fetchAllById with null response body")
    void shouldHandleFetchAllByIdWithNullResponseBody() throws Exception {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        ResponseEntity<String> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }

    @Test
    @DisplayName("Should handle fetchAllById with HTTP error status")
    void shouldHandleFetchAllByIdWithHttpErrorStatus() {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        ResponseEntity<String> mockResponse = new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAllById with HttpStatusCodeException")
    void shouldHandleFetchAllByIdWithHttpStatusCodeException() {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAllById with RestClientException")
    void shouldHandleFetchAllByIdWithRestClientException() {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        RestClientException exception = new RestClientException(CONNECTION_FAILED);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAllById with unexpected exception")
    void shouldHandleFetchAllByIdWithUnexpectedException() {
        // Setup
        Collection<Object> ids = List.of(1L);
        String expectedUrl = TEST_API_ID;

        RuntimeException exception = new RuntimeException(UNEXPECTED_ERROR);
        when(mockRestTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAll with empty response body")
    void shouldHandleFetchAllWithEmptyResponseBody() throws Exception {
        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>("", HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }

    @Test
    @DisplayName("Should handle fetchAll with null response body")
    void shouldHandleFetchAllWithNullResponseBody() throws Exception {
        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideFetchAllSuccessfulResponses")
    @DisplayName("Should handle fetchAll with successful responses")
    void shouldHandleFetchAllWithSuccessfulResponses(String testCase, String responseBody, int timeoutMillis, String expectedName) throws Exception {
        // Use real ObjectMapper for this test to avoid complex mocking
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAll();

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(timeoutMillis, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertEquals(1, entities.size());
        assertEquals(expectedName, entities.get(0).getName());
    }

    private static Stream<Arguments> provideFetchAllSuccessfulResponses() {
        return Stream.of(
                Arguments.of("array response", "[{\"id\":1,\"name\":\"Entity1\"}]", 5000, ENTITY1),
                Arguments.of("single object response", "{\"id\":1,\"name\":\"Entity1\"}", 500, ENTITY1),
                Arguments.of("single object with different name", "{\"id\":1,\"name\":\"SingleEntity\"}", 500, "SingleEntity")
        );
    }


    @Test
    @DisplayName("Should handle fetchAll with JSON parsing exception")
    void shouldHandleFetchAllWithJsonParsingException() {
        // Use real ObjectMapper for this test to avoid complex mocking
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Setup
        String responseBody = "invalid json";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAll with HTTP error status")
    void shouldHandleFetchAllWithHttpErrorStatus() {
        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAll with HttpStatusCodeException")
    void shouldHandleFetchAllWithHttpStatusCodeException() {
        // Setup
        HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify - Kısa timeout ile test
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        // Exception'ı kontrol et
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS); // Çok kısa timeout
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAll with RestClientException")
    void shouldHandleFetchAllWithRestClientException() {
        // Setup
        RestClientException exception = new RestClientException(CONNECTION_FAILED);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle fetchAll with unexpected exception")
    void shouldHandleFetchAllWithUnexpectedException() {
        // Setup
        RuntimeException exception = new RuntimeException(UNEXPECTED_ERROR);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute & Verify
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });

        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());
    }

    @Test
    @DisplayName("Should handle isHealthy with successful health check using health endpoint")
    void shouldHandleIsHealthyWithSuccessfulHealthCheckUsingHealthEndpoint() {
        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>(OK, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API_HEALTH), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        boolean result = dataSource.isHealthy();

        // Verify
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle isHealthy with failed health check")
    void shouldHandleIsHealthyWithFailedHealthCheck() {
        // Setup
        ResponseEntity<String> mockResponse = new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API_HEALTH), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        boolean result = dataSource.isHealthy();

        // Verify
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle isHealthy with RestClientException")
    void shouldHandleIsHealthyWithRestClientException() {
        // Setup
        RestClientException exception = new RestClientException(CONNECTION_FAILED);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API_HEALTH), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute
        boolean result = dataSource.isHealthy();

        // Verify
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle isHealthy with unexpected exception")
    void shouldHandleIsHealthyWithUnexpectedException() {
        // Setup
        RuntimeException exception = new RuntimeException(UNEXPECTED_ERROR);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API_HEALTH), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Execute
        boolean result = dataSource.isHealthy();

        // Verify
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle isHealthy without health endpoint")
    void shouldHandleIsHealthyWithoutHealthEndpoint() {
        // Setup - create data source without health endpoint
        RestApiDataSource<TestEntity> dataSourceWithoutHealth = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, null, mockRestTemplate, mockObjectMapper);

        ResponseEntity<String> mockResponse = new ResponseEntity<>(OK, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        boolean result = dataSourceWithoutHealth.isHealthy();

        // Verify
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle close method")
    void shouldHandleCloseMethod() {
        // Execute - should not throw any exception
        assertDoesNotThrow(() -> dataSource.close());
    }

    @Test
    @DisplayName("Should handle fallback data source operations")
    void shouldHandleFallbackDataSourceOperations() {
        // Setup
        RestApiDataSource<TestEntity> fallbackDataSource = new RestApiDataSource<>(
                "FallbackAPI", TestEntity.class, "http://fallback.api");

        // Test initial state
        assertFalse(dataSource.getFallbackDataSource().isPresent());

        // Set fallback
        dataSource.setFallbackDataSource(fallbackDataSource);

        // Verify fallback is set
        assertTrue(dataSource.getFallbackDataSource().isPresent());
        assertEquals(fallbackDataSource, dataSource.getFallbackDataSource().get());

        // Set null fallback
        dataSource.setFallbackDataSource(null);

        // Verify fallback is cleared
        assertFalse(dataSource.getFallbackDataSource().isPresent());
    }


    @Test
    @DisplayName("Should handle parseJsonResponse with array")
    void shouldHandleParseJsonResponseWithArray() throws Exception {
        // Use real ObjectMapper for this test
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Setup - array response
        String responseBody = "[{\"id\":1,\"name\":\"Entity1\"},{\"id\":2,\"name\":\"Entity2\"}]";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAll();

        // Verify
        assertNotNull(result);
        List<TestEntity> entities = result.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertEquals(ENTITY1, entities.get(0).getName());
        assertEquals(ENTITY2, entities.get(1).getName());
    }

    @Test
    @DisplayName("Should handle getter methods")
    void shouldHandleGetterMethods() {
        // Verify all getter methods
        assertEquals(TEST_API, dataSource.getName());
        assertEquals(TestEntity.class, dataSource.getEntityType());
        assertEquals(HTTP_TEST_API, dataSource.getBaseUrl());
        assertEquals(HTTP_TEST_API_HEALTH, dataSource.getHealthEndpoint());
        assertEquals(mockRestTemplate, dataSource.getRestTemplate());
        assertEquals(mockObjectMapper, dataSource.getObjectMapper());
    }

    @Test
    @DisplayName("Should handle constructor parameter validation")
    void shouldHandleConstructorParameterValidation() {
        // Test null name
        assertThrows(NullPointerException.class, () -> {
            new RestApiDataSource<>(null, TestEntity.class, HTTP_TEST_API);
        });

        // Test null entity type
        assertThrows(NullPointerException.class, () -> {
            new RestApiDataSource<>(TEST_API, null, HTTP_TEST_API);
        });

        // Test null base URL
        assertThrows(NullPointerException.class, () -> {
            new RestApiDataSource<>(TEST_API, TestEntity.class, null);
        });

        // Test null RestTemplate
        String name1 = TEST_API;
        Class<TestEntity> entityType1 = TestEntity.class;
        String baseUrl1 = HTTP_TEST_API;
        String healthEndpoint1 = null;
        RestTemplate nullRestTemplate = null;
        ObjectMapper objectMapper1 = new ObjectMapper();

        assertThrows(NullPointerException.class, () ->
                new RestApiDataSource<>(name1, entityType1, baseUrl1, healthEndpoint1, nullRestTemplate, objectMapper1)
        );

        // Test null ObjectMapper
        String name2 = TEST_API;
        Class<TestEntity> entityType2 = TestEntity.class;
        String baseUrl2 = HTTP_TEST_API;
        String healthEndpoint2 = null;
        RestTemplate restTemplate2 = new RestTemplate();
        ObjectMapper nullObjectMapper = null;

        assertThrows(NullPointerException.class, () ->
                new RestApiDataSource<>(name2, entityType2, baseUrl2, healthEndpoint2, restTemplate2, nullObjectMapper)
        );
    }

    @Test
    @DisplayName("Should handle large response with 10K+ entities")
    void shouldHandleLargeResponseWith10KPlusEntities() throws Exception {
        // Use real ObjectMapper for this test
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Generate large JSON response with 10,000 entities
        StringBuilder largeJsonBuilder = new StringBuilder("[");
        for (int i = 0; i < 10_000; i++) {
            if (i > 0) largeJsonBuilder.append(",");
            largeJsonBuilder.append(String.format("{\"id\":%d,\"name\":\"Entity%d\"}", i, i));
        }
        largeJsonBuilder.append("]");

        ResponseEntity<String> mockResponse = new ResponseEntity<>(largeJsonBuilder.toString(), HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAll();
        List<TestEntity> entities = result.get(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Verify
        assertNotNull(entities);
        assertEquals(10_000, entities.size());
        
        // Verify first and last entities
        assertEquals(0L, entities.get(0).getId());
        assertEquals("Entity0", entities.get(0).getName());
        assertEquals(9999L, entities.get(9999).getId());
        assertEquals("Entity9999", entities.get(9999).getName());
        
        // Verify performance - should complete within reasonable time
        assertTrue(endTime - startTime < 10_000, LARGE_DATASET_PARSING);
    }

    @Test
    @DisplayName("Should handle fetchAllById with large response (10K+ entities)")
    void shouldHandleFetchAllByIdWithLargeResponse() throws Exception {
        // Use real ObjectMapper for this test
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Generate large collection of IDs
        List<Object> ids = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add((long) i);
        }

        // Generate large JSON response
        StringBuilder largeJsonBuilder = new StringBuilder("[");
        for (int i = 0; i < 10_000; i++) {
            if (i > 0) largeJsonBuilder.append(",");
            largeJsonBuilder.append(String.format("{\"id\":%d,\"name\":\"Entity%d\"}", i, i));
        }
        largeJsonBuilder.append("]");
        
        ResponseEntity<String> mockResponse = new ResponseEntity<>(largeJsonBuilder.toString(), HttpStatus.OK);
        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAllById(ids);
        List<TestEntity> entities = result.get(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Verify
        assertNotNull(entities);
        assertEquals(10_000, entities.size());
        
        // Verify performance
        assertTrue(endTime - startTime < 10_000, LARGE_DATASET_PARSING);
    }

    @Test
    @DisplayName("Should handle very large response with 100K+ entities")
    void shouldHandleVeryLargeResponseWith100KPlusEntities() throws Exception {
        // Use real ObjectMapper for this test
        RestApiDataSource<TestEntity> realDataSource = new RestApiDataSource<>(
                TEST_API, TestEntity.class, HTTP_TEST_API, HTTP_TEST_API_HEALTH,
                mockRestTemplate, new ObjectMapper());

        // Generate very large JSON response with 100,000 entities
        StringBuilder largeJsonBuilder = new StringBuilder("[");
        for (int i = 0; i < 100_000; i++) {
            if (i > 0) largeJsonBuilder.append(",");
            largeJsonBuilder.append(String.format("{\"id\":%d,\"name\":\"Entity%d\"}", i, i));
        }
        largeJsonBuilder.append("]");

        ResponseEntity<String> mockResponse = new ResponseEntity<>(largeJsonBuilder.toString(), HttpStatus.OK);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // Execute
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = realDataSource.fetchAll();
        List<TestEntity> entities = result.get(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Verify
        assertNotNull(entities);
        assertEquals(100_000, entities.size());
        
        // Verify random samples
        assertEquals(0L, entities.get(0).getId());
        assertEquals(50_000L, entities.get(50_000).getId());
        assertEquals(99_999L, entities.get(99_999).getId());
        
        // Verify performance - should complete within reasonable time
        assertTrue(endTime - startTime < 30_000, "Very large dataset parsing should complete within 30 seconds");
    }

    @Test
    @DisplayName("Should handle HTTP 4xx error responses")
    void shouldHandleHttp4xxErrorResponses() {
        // Test 400 Bad Request
        ResponseEntity<String> badRequestResponse = new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(badRequestResponse);

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());

        // Test 401 Unauthorized
        ResponseEntity<String> unauthorizedResponse = new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(unauthorizedResponse);

        CompletableFuture<List<TestEntity>> result2 = dataSource.fetchAll();
        activeFutures.add(result2);

        ExecutionException executionException2 = assertThrows(ExecutionException.class, () -> {
            result2.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException2.getCause());

        // Test 403 Forbidden
        ResponseEntity<String> forbiddenResponse = new ResponseEntity<>("Forbidden", HttpStatus.FORBIDDEN);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(forbiddenResponse);

        CompletableFuture<List<TestEntity>> result3 = dataSource.fetchAll();
        activeFutures.add(result3);

        ExecutionException executionException3 = assertThrows(ExecutionException.class, () -> {
            result3.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException3.getCause());
    }

    @Test
    @DisplayName("Should handle HTTP 5xx error responses")
    void shouldHandleHttp5xxErrorResponses() {
        // Test 500 Internal Server Error
        ResponseEntity<String> serverErrorResponse = new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(serverErrorResponse);

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        activeFutures.add(result);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException.getCause());

        // Test 502 Bad Gateway
        ResponseEntity<String> badGatewayResponse = new ResponseEntity<>("Bad Gateway", HttpStatus.BAD_GATEWAY);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(badGatewayResponse);

        CompletableFuture<List<TestEntity>> result2 = dataSource.fetchAll();
        activeFutures.add(result2);

        ExecutionException executionException2 = assertThrows(ExecutionException.class, () -> {
            result2.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException2.getCause());

        // Test 503 Service Unavailable
        ResponseEntity<String> serviceUnavailableResponse = new ResponseEntity<>("Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        when(mockRestTemplate.exchange(eq(HTTP_TEST_API), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(serviceUnavailableResponse);

        CompletableFuture<List<TestEntity>> result3 = dataSource.fetchAll();
        activeFutures.add(result3);

        ExecutionException executionException3 = assertThrows(ExecutionException.class, () -> {
            result3.get(5, TimeUnit.SECONDS);
        });
        assertInstanceOf(DataSourceConnectionException.class, executionException3.getCause());
    }

    static class TestEntity {
        private Long id;
        private String name;

        public TestEntity() {
        }

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}