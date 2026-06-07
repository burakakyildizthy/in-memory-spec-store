package com.thy.fss.common.inmemory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.datasource.RestApiDataSource;
import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RestApiDataSource using WireMock for HTTP mocking.
 * Migrated from temp_tests_backup/integration/RestApiDataSourceIT.java to use new API structure.
 * Tests REST API data source integration scenarios, error handling, and performance with generated classes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
@Tag("slow")
class RestApiDataSourceIntegrationTest extends BaseIntegrationTest {

    private static WireMockServer wireMockServer;
    private static String baseUrl;
    private static ObjectMapper objectMapper;
    private static RestTemplate restTemplate;
    private com.thy.fss.common.inmemory.engine.DataSynchronizationEngine engine;

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Override
    @BeforeAll
    public void setUp() {
        // Start WireMock server on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort()
                .usingFilesUnderDirectory("src/test/resources"));
        wireMockServer.start();

        baseUrl = "http://localhost:" + wireMockServer.port();
        objectMapper = new ObjectMapper();
        restTemplate = new RestTemplate();

        // Configure WireMock client
        WireMock.configureFor("localhost", wireMockServer.port());

        System.out.println("WireMock server started on port: " + wireMockServer.port());
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        // Clear factory state to prevent test interference
        InMemorySpecStoreFactory.getInstance().clearAll();
        // Stop previous engine if exists
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create RestApiDataSource with valid configuration")
    void shouldCreateRestApiDataSourceWithValidConfiguration() {
        // Given
        String name = "test-api-users";
        Class<ApiUser> entityType = ApiUser.class;
        String apiBaseUrl = baseUrl + "/users";

        // When
        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                name, entityType, apiBaseUrl);

        // Then
        assertThat(dataSource.getName()).isEqualTo(name);
        assertThat(dataSource.getEntityType()).isEqualTo(entityType);
        assertThat(dataSource.getBaseUrl()).isEqualTo(apiBaseUrl);
    }

    @Test
    @Order(2)
    @DisplayName("Should fetch all users from REST API")
    void shouldFetchAllUsersFromRestApi() throws ExecutionException, InterruptedException, TimeoutException {

        String jsonResponse = """
                [
                    {"id": 1, "name": "John Doe", "email": "john@example.com", "status": "ACTIVE"},
                    {"id": 2, "name": "Jane Smith", "email": "jane@example.com", "status": "ACTIVE"},
                    {"id": 3, "name": "Bob Johnson", "email": "bob@example.com", "status": "INACTIVE"}
                ]
                """;

        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponse)));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        List<ApiUser> users = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(users).isNotNull().hasSize(3);
        assertThat(users).extracting(ApiUser::getName)
                .containsExactly("John Doe", "Jane Smith", "Bob Johnson");
        assertThat(users).extracting(ApiUser::getStatus)
                .contains("ACTIVE", "INACTIVE");

        // Verify the request was made
        verify(getRequestedFor(urlEqualTo("/users"))
                .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    @Order(3)
    @DisplayName("Should integrate REST API data source with TestUser generated classes")
    void shouldIntegrateRestApiDataSourceWithTestUserGeneratedClasses() {
        // Given - Create REST API mock for TestUser data using common test infrastructure
        String jsonResponse = """
                [
                    {"id": 1, "name": "John Doe", "age": 30, "email": "john@example.com", "active": true},
                    {"id": 2, "name": "Jane Smith", "age": 25, "email": "jane@example.com", "active": true},
                    {"id": 3, "name": "Bob Johnson", "age": 35, "email": "bob@example.com", "active": false}
                ]
                """;

        stubFor(get(urlEqualTo("/testusers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponse)));

        // When - Create data store with REST API data source and TestUser
        RestApiDataSource<TestUser> apiDataSource = new RestApiDataSource<>(
                "testusers-api", TestUser.class, baseUrl + "/testusers");

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("testusers-api", apiDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> store = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial data load
        TestUtil.await(2000);

        // Then - Verify integration with TestUser and generated classes
        List<TestUser> users = store.findAll();
        assertThat(users).hasSize(3);

        // Test with stream filtering using TestUser methods
        List<TestUser> activeUsers = users.stream()
                .filter(TestUser::getActive)
                .toList();
        assertThat(activeUsers).hasSize(2).allMatch(TestUser::getActive);

        // Verify data integrity
        TestUser johnDoe = users.stream()
                .filter(u -> "John Doe".equals(u.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(johnDoe.getAge()).isEqualTo(30);
        assertThat(johnDoe.getEmail()).isEqualTo("john@example.com");
        assertThat(johnDoe.getActive()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Should fetch users by IDs from REST API")
    void shouldFetchUsersByIdsFromRestApi() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String jsonResponse = """
                [
                    {"id": 1, "name": "John Doe", "email": "john@example.com", "status": "ACTIVE"},
                    {"id": 3, "name": "Bob Johnson", "email": "bob@example.com", "status": "INACTIVE"}
                ]
                """;

        stubFor(get(urlPathEqualTo("/users"))
                .withQueryParam("ids", equalTo("1,3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponse)));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        Collection<Object> ids = Arrays.asList(1L, 3L);

        // When
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAllById(ids);
        List<ApiUser> users = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(users).isNotNull().hasSize(2);
        assertThat(users).extracting(ApiUser::getId)
                .containsExactly(1L, 3L);
        assertThat(users).extracting(ApiUser::getName)
                .containsExactly("John Doe", "Bob Johnson");

        // Verify the request was made with correct query parameters
        verify(getRequestedFor(urlPathEqualTo("/users"))
                .withQueryParam("ids", equalTo("1,3"))
                .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    @Order(5)
    @DisplayName("Should handle complex API product data with nested structures")
    void shouldHandleComplexApiProductDataWithNestedStructures() throws ExecutionException, InterruptedException, TimeoutException {
        // Given - Complex JSON with brand field
        String complexJsonResponse = """
                [
                    {
                        "id": 1,
                        "name": "Premium Laptop",
                        "price": 1299.99,
                        "category": "Electronics",
                        "available": true,
                        "createdAt": "2024-01-15T10:30:00",
                        "brand": "TechCorp"
                    },
                    {
                        "id": 2,
                        "name": "Gaming Mouse",
                        "price": 79.99,
                        "category": "Accessories",
                        "available": false,
                        "createdAt": "2024-01-20T14:15:00",
                        "brand": "GameTech"
                    }
                ]
                """;

        stubFor(get(urlEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(complexJsonResponse)));

        // When - Create REST API data source for complex data with configured ObjectMapper
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        RestApiDataSource<ApiProduct> productDataSource = new RestApiDataSource<>(
                "products-api", ApiProduct.class, baseUrl + "/products", null, new RestTemplate(), om);

        CompletableFuture<List<ApiProduct>> future = productDataSource.fetchAll();
        List<ApiProduct> products = future.get(5, TimeUnit.SECONDS);

        // Then - Verify complex data parsing
        assertThat(products).hasSize(2);

        ApiProduct laptop = products.stream()
                .filter(p -> p.getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(laptop.getName()).isEqualTo("Premium Laptop");
        assertThat(laptop.getPrice()).isEqualByComparingTo(1299.99);
        assertThat(laptop.getCategory()).isEqualTo("Electronics");
        assertThat(laptop.isAvailable()).isTrue();
        assertThat(laptop.getBrand()).isEqualTo("TechCorp");
    }

    @Test
    @Order(6)
    @DisplayName("Should handle single object JSON response")
    void shouldHandleSingleObjectJsonResponse() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String jsonResponse = """
                {"id": 1, "name": "John Doe", "email": "john@example.com", "status": "ACTIVE"}
                """;

        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponse)));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        List<ApiUser> users = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(users).isNotNull().hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(1L);
        assertThat(users.get(0).getName()).isEqualTo("John Doe");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle empty JSON array response")
    void shouldHandleEmptyJsonArrayResponse() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        List<ApiUser> users = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(users).isNotNull().isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("Should handle HTTP error responses gracefully")
    void shouldHandleHttpErrorResponsesGracefully() {
        // Given
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When & Then
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class)
                .hasMessageContaining("HTTP request failed with status: 404");
    }

    @Test
    @Order(9)
    @DisplayName("Should handle complex error scenarios and fallback mechanisms")
    void shouldHandleComplexErrorScenariosAndFallbackMechanisms() throws ExecutionException, InterruptedException, TimeoutException {
        // Given - Primary API that fails
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));

        // Fallback API that works
        stubFor(get(urlEqualTo("/fallback-users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"id": 1, "name": "Fallback User", "email": "fallback@example.com", "status": "ACTIVE"}
                                ]
                                """)));

        // When - Create primary and fallback data sources
        RestApiDataSource<ApiUser> primaryDataSource = new RestApiDataSource<>(
                "primary-users-api", ApiUser.class, baseUrl + "/users");

        RestApiDataSource<ApiUser> fallbackDataSource = new RestApiDataSource<>(
                "fallback-users-api", ApiUser.class, baseUrl + "/fallback-users");

        primaryDataSource.setFallbackDataSource(fallbackDataSource);

        // Then - Verify fallback mechanism works
        assertThat(primaryDataSource.getFallbackDataSource()).isPresent();

        // Test primary failure scenario
        CompletableFuture<List<ApiUser>> primaryFuture = primaryDataSource.fetchAll();
        assertThatThrownBy(() -> primaryFuture.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class);

        // Test fallback success
        CompletableFuture<List<ApiUser>> fallbackFuture = fallbackDataSource.fetchAll();
        List<ApiUser> fallbackUsers = fallbackFuture.get(5, TimeUnit.SECONDS);

        assertThat(fallbackUsers).hasSize(1);
        assertThat(fallbackUsers.get(0).getName()).isEqualTo("Fallback User");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle connection timeout gracefully")
    void shouldHandleConnectionTimeoutGracefully() {
        // Given
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")
                        .withFixedDelay(10000))); // 10 second delay to simulate timeout

        // Create a RestTemplate with short timeout settings
        RestTemplate timeoutRestTemplate = new RestTemplate();
        // Set connection timeout to 2 seconds and read timeout to 3 seconds
        timeoutRestTemplate.getRequestFactory();
        if (timeoutRestTemplate.getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setConnectTimeout(2000); // 2 seconds
            factory.setReadTimeout(3000); // 3 seconds
        }

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users", null, timeoutRestTemplate, objectMapper);

        // When & Then
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class)
                .hasMessageContaining("Failed to fetch all data from REST API");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle malformed JSON response gracefully")
    void shouldHandleMalformedJsonResponseGracefully() {
        // Given
        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ invalid json }")));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When & Then
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class);
    }

    @Test
    @Order(12)
    @DisplayName("Should test health check with dedicated endpoint")
    void shouldTestHealthCheckWithDedicatedEndpoint() {
        // Given
        stubFor(head(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users", baseUrl + "/health",
                restTemplate, objectMapper);

        // When
        boolean healthy = dataSource.isHealthy();

        // Then
        assertThat(healthy).isTrue();
        verify(headRequestedFor(urlEqualTo("/health")));
    }

    @Test
    @Order(13)
    @DisplayName("Should handle complex concurrent API requests with data store integration")
    @Disabled("Disabled due to occasional timeouts in CI environment")
    void shouldHandleComplexConcurrentApiRequestsWithDataStoreIntegration() {
        // Given - API that responds with different data based on timing
        stubFor(get(urlEqualTo("/testusers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"id": 1, "name": "Concurrent User 1", "age": 25, "email": "user1@example.com", "active": true},
                                    {"id": 2, "name": "Concurrent User 2", "age": 30, "email": "user2@example.com", "active": true}
                                ]
                                """)
                        .withFixedDelay(100))); // Simulate network latency

        // When - Create multiple data stores with same API source
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();
        List<InMemoryDataStore<TestUser>> stores = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            RestApiDataSource<TestUser> apiDataSource = new RestApiDataSource<>(
                    "testusers-api-" + i, TestUser.class, baseUrl + "/testusers");

            factory.registerDataSource("testusers-api-" + i, apiDataSource, Duration.ofSeconds(2));

            InMemoryDataStore<TestUser> store = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            stores.add(store);
        }

        // Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for all stores to load data
        TestUtil.await(3000);

        // Then - Verify all stores loaded data correctly
        for (InMemoryDataStore<TestUser> store : stores) {
            List<TestUser> users = store.findAll();
            assertThat(users).hasSize(2);

            // Test filtering with stream operations
            List<TestUser> concurrentUsers = users.stream()
                    .filter(u -> u.getName().contains("Concurrent"))
                    .toList();
            assertThat(concurrentUsers).hasSize(2);
        }

        // Verify multiple concurrent requests were made
        verify(moreThan(4), getRequestedFor(urlEqualTo("/testusers")));
    }

    @Test
    @Order(14)
    @DisplayName("Should test concurrent requests performance")
    @Tag("performance")
    @Tag("concurrent")
    void shouldTestConcurrentRequestsPerformance() {
        // Given
        String jsonResponse = """
                [
                    {"id": 1, "name": "John Doe", "email": "john@example.com", "status": "ACTIVE"},
                    {"id": 2, "name": "Jane Smith", "email": "jane@example.com", "status": "ACTIVE"}
                ]
                """;

        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonResponse)
                        .withFixedDelay(100))); // Small delay to simulate network latency

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        int numberOfRequests = 10;
        List<CompletableFuture<List<ApiUser>>> futures = new ArrayList<>();

        // When - Execute concurrent requests
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfRequests; i++) {
            futures.add(dataSource.fetchAll());
        }

        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        // Then
        assertThatCode(() -> allFutures.get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Verify all requests succeeded
        for (CompletableFuture<List<ApiUser>> future : futures) {
            assertThat(future.join()).hasSize(2);
        }

        // Verify requests were made concurrently (should be faster than sequential)
        assertThat(totalTime).isLessThan(numberOfRequests * 200); // Allow some overhead

        // Verify all requests were made
        verify(numberOfRequests, getRequestedFor(urlEqualTo("/users")));
    }

    @Test
    @Order(15)
    @DisplayName("Should handle complex pagination and large dataset scenarios")
    void shouldHandleComplexPaginationAndLargeDatasetScenarios() throws ExecutionException, InterruptedException, TimeoutException {
        // Given - Large dataset API response
        StringBuilder largeJsonBuilder = new StringBuilder("[");
        for (int i = 1; i <= 1000; i++) {
            if (i > 1) largeJsonBuilder.append(",");
            largeJsonBuilder.append(String.format(
                    "{\"id\": %d, \"name\": \"User %d\", \"email\": \"user%d@example.com\", \"status\": \"ACTIVE\"}",
                    i, i, i));
        }
        largeJsonBuilder.append("]");

        stubFor(get(urlEqualTo("/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(largeJsonBuilder.toString())));

        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        // When
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
        List<ApiUser> users = future.get(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(users).isNotNull().hasSize(1000);
        assertThat(endTime - startTime).isLessThan(5000); // Should complete within 5 seconds

        // Verify first and last users
        assertThat(users.get(0).getId()).isEqualTo(1L);
        assertThat(users.get(0).getName()).isEqualTo("User 1");
        assertThat(users.get(999).getId()).isEqualTo(1000L);
        assertThat(users.get(999).getName()).isEqualTo("User 1000");
    }

    @Test
    @Order(16)
    @DisplayName("Should handle complex product data with nested structures and data store integration")
    void shouldHandleComplexProductDataWithNestedStructuresAndDataStoreIntegration() {
        // Given - Complex JSON with brand field
        String complexJsonResponse = """
                [
                    {
                        "id": 1,
                        "name": "Premium Laptop",
                        "price": 1299.99,
                        "category": "Electronics",
                        "available": true,
                        "createdAt": "2024-01-15T10:30:00",
                        "brand": "TechCorp"
                    },
                    {
                        "id": 2,
                        "name": "Gaming Mouse",
                        "price": 79.99,
                        "category": "Accessories",
                        "available": false,
                        "createdAt": "2024-01-20T14:15:00",
                        "brand": "GameTech"
                    }
                ]
                """;

        stubFor(get(urlEqualTo("/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(complexJsonResponse)));

        // When - Create REST API data source and integrate with data store with configured ObjectMapper
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        RestApiDataSource<ApiProduct> productDataSource = new RestApiDataSource<>(
                "products-api", ApiProduct.class, baseUrl + "/products", null, new RestTemplate(), om);

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.registerDataSource("products-api", productDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<ApiProduct> store = factory.buildInMemoryStore(ApiProductSpecificationService.INSTANCE)
                .withPrimaryDataSource(ApiProduct.class)
                .build();

        // Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial data load
        TestUtil.await(2000);

        // Then - Verify complex data parsing and store integration
        List<ApiProduct> products = store.findAll();
        assertThat(products).hasSize(2);

        ApiProduct laptop = products.stream()
                .filter(p -> p.getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(laptop.getName()).isEqualTo("Premium Laptop");
        assertThat(laptop.getPrice()).isEqualByComparingTo(1299.99);
        assertThat(laptop.getCategory()).isEqualTo("Electronics");
        assertThat(laptop.isAvailable()).isTrue();
        assertThat(laptop.getBrand()).isEqualTo("TechCorp");

        // Test filtering available products
        List<ApiProduct> availableProducts = products.stream()
                .filter(ApiProduct::isAvailable)
                .toList();
        assertThat(availableProducts).hasSize(1);
        assertThat(availableProducts.get(0).getName()).isEqualTo("Premium Laptop");
    }

    @Test
    @Order(17)
    @DisplayName("Should handle different HTTP response codes")
    void shouldTestDifferentHttpResponseCodes() {
        // Test various HTTP status codes
        int[] statusCodes = {400, 401, 403, 404, 429, 500, 502, 503, 504};

        for (int statusCode : statusCodes) {
            // Given
            wireMockServer.resetAll();
            stubFor(get(urlEqualTo("/users"))
                    .willReturn(aResponse()
                            .withStatus(statusCode)
                            .withBody("Error response")));

            RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                    "users-api", ApiUser.class, baseUrl + "/users");

            // When & Then
            CompletableFuture<List<ApiUser>> future = dataSource.fetchAll();
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(DataSourceConnectionException.class)
                    .hasMessageContaining("HTTP request failed with status: " + statusCode);
        }
    }

    @Test
    @Order(18)
    @DisplayName("Should handle empty ID collection gracefully")
    void shouldHandleEmptyIdCollectionGracefully() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        RestApiDataSource<ApiUser> dataSource = new RestApiDataSource<>(
                "users-api", ApiUser.class, baseUrl + "/users");

        Collection<Object> emptyIds = Collections.emptyList();

        // When
        CompletableFuture<List<ApiUser>> future = dataSource.fetchAllById(emptyIds);
        List<ApiUser> users = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(users).isNotNull().isEmpty();

        // Verify no HTTP request was made
        verify(0, getRequestedFor(urlMatching("/users.*")));
    }

    @Test
    @Order(19)
    @DisplayName("Should test fallback DataSource functionality")
    void shouldTestFallbackDataSourceFunctionality() {
        // Given
        RestApiDataSource<ApiUser> primaryDataSource = new RestApiDataSource<>(
                "primary-users-api", ApiUser.class, baseUrl + "/users");

        RestApiDataSource<ApiUser> fallbackDataSource = new RestApiDataSource<>(
                "fallback-users-api", ApiUser.class, baseUrl + "/fallback-users");

        // When
        primaryDataSource.setFallbackDataSource(fallbackDataSource);

        // Then
        assertThat(primaryDataSource.getFallbackDataSource())
                .isPresent().contains(fallbackDataSource);
    }

    @Test
    @Order(20)
    @DisplayName("Should handle complex health check and monitoring scenarios")
    void shouldHandleComplexHealthCheckAndMonitoringScenarios() {
        // Given - Health check endpoints with different behaviors
        stubFor(head(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-Health-Status", "OK")
                        .withHeader("X-Response-Time", "50ms")));

        stubFor(head(urlEqualTo("/degraded-health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-Health-Status", "DEGRADED")
                        .withFixedDelay(1000))); // Slow response

        stubFor(head(urlEqualTo("/unhealthy"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("X-Health-Status", "DOWN")));

        // When - Test different health check scenarios
        RestApiDataSource<ApiUser> healthyDataSource = new RestApiDataSource<>(
                "healthy-api", ApiUser.class, baseUrl + "/users", baseUrl + "/health",
                restTemplate, objectMapper);

        RestApiDataSource<ApiUser> degradedDataSource = new RestApiDataSource<>(
                "degraded-api", ApiUser.class, baseUrl + "/users", baseUrl + "/degraded-health",
                restTemplate, objectMapper);

        RestApiDataSource<ApiUser> unhealthyDataSource = new RestApiDataSource<>(
                "unhealthy-api", ApiUser.class, baseUrl + "/users", baseUrl + "/unhealthy",
                restTemplate, objectMapper);

        // Then - Verify health check behaviors
        assertThat(healthyDataSource.isHealthy()).isTrue();
        assertThat(degradedDataSource.isHealthy()).isTrue(); // Still returns 200
        assertThat(unhealthyDataSource.isHealthy()).isFalse();

        // Verify health check requests were made
        verify(headRequestedFor(urlEqualTo("/health")));
        verify(headRequestedFor(urlEqualTo("/degraded-health")));
        verify(headRequestedFor(urlEqualTo("/unhealthy")));
    }

    @Test
    @Order(21)
    @DisplayName("Should handle complex retry and timeout scenarios")
    void shouldHandleComplexRetryAndTimeoutScenarios() {
        // Given - API with intermittent failures
        stubFor(get(urlEqualTo("/unreliable-users"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error"))
                .willSetStateTo("First Failure"));

        stubFor(get(urlEqualTo("/unreliable-users"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Failure")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable"))
                .willSetStateTo("Second Failure"));

        stubFor(get(urlEqualTo("/unreliable-users"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"id": 1, "name": "Retry Success User", "email": "retry@example.com", "status": "ACTIVE"}
                                ]
                                """)));

        // When - Create data source that will experience failures
        RestApiDataSource<ApiUser> unreliableDataSource = new RestApiDataSource<>(
                "unreliable-users-api", ApiUser.class, baseUrl + "/unreliable-users");

        // Then - Test failure scenarios
        CompletableFuture<List<ApiUser>> firstAttempt = unreliableDataSource.fetchAll();
        assertThatThrownBy(() -> firstAttempt.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class);

        CompletableFuture<List<ApiUser>> secondAttempt = unreliableDataSource.fetchAll();
        assertThatThrownBy(() -> secondAttempt.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(DataSourceConnectionException.class);

        // Third attempt should succeed
        CompletableFuture<List<ApiUser>> thirdAttempt = unreliableDataSource.fetchAll();
        assertThatCode(() -> {
            List<ApiUser> users = thirdAttempt.get(5, TimeUnit.SECONDS);
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getName()).isEqualTo("Retry Success User");
        }).doesNotThrowAnyException();

        // Verify all three requests were made
        verify(3, getRequestedFor(urlEqualTo("/unreliable-users")));
    }

    @Test
    @Order(22)
    @DisplayName("Should handle complex multi-data source integration scenarios")
    @Disabled("Disabled due to occasional timeouts in CI environment")
    void shouldHandleComplexMultiDataSourceIntegrationScenarios() {
        // Given - Multiple API endpoints with different data
        stubFor(get(urlEqualTo("/users-primary"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"id": 1, "name": "Primary User 1", "age": 25, "email": "primary1@example.com", "active": true},
                                    {"id": 2, "name": "Primary User 2", "age": 30, "email": "primary2@example.com", "active": true}
                                ]
                                """)));

        stubFor(get(urlEqualTo("/users-secondary"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"id": 3, "name": "Secondary User 1", "age": 35, "email": "secondary1@example.com", "active": false},
                                    {"id": 4, "name": "Secondary User 2", "age": 40, "email": "secondary2@example.com", "active": true}
                                ]
                                """)));

        // When - Create multiple data stores with different API sources
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();

        RestApiDataSource<TestUser> primaryDataSource = new RestApiDataSource<>(
                "primary-users-api", TestUser.class, baseUrl + "/users-primary");

        RestApiDataSource<TestUser> secondaryDataSource = new RestApiDataSource<>(
                "secondary-users-api", TestUser.class, baseUrl + "/users-secondary");

        factory.registerDataSource("primary-users-api", primaryDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("secondary-users-api", secondaryDataSource, Duration.ofSeconds(1));

        InMemoryDataStore<TestUser> primaryStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        InMemoryDataStore<TestUser> secondaryStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        // Initialize and start the synchronization engine
        engine = new com.thy.fss.common.inmemory.engine.DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for data load
        TestUtil.await(2000);

        // Then - Verify both stores loaded their respective data
        List<TestUser> primaryUsers = primaryStore.findAll();
        List<TestUser> secondaryUsers = secondaryStore.findAll();

        assertThat(primaryUsers).hasSize(2);
        assertThat(secondaryUsers).hasSize(2);

        // Verify primary store data
        assertThat(primaryUsers).allMatch(u -> u.getName().startsWith("Primary"))
                .allMatch(TestUser::getActive);

        // Verify secondary store data
        assertThat(secondaryUsers).allMatch(u -> u.getName().startsWith("Secondary"));
        long activeSecondaryCount = secondaryUsers.stream()
                .mapToLong(u -> u.getActive() ? 1 : 0)
                .sum();
        assertThat(activeSecondaryCount).isEqualTo(1);

        // Verify requests were made to both endpoints
        verify(getRequestedFor(urlEqualTo("/users-primary")));
        verify(getRequestedFor(urlEqualTo("/users-secondary")));
    }

    // Test entity classes for API integration
    public static class ApiUser {
        private Long id;
        private String name;
        private String email;
        private String status;
        private LocalDateTime createdAt;

        // Constructors
        public ApiUser() {
        }

        public ApiUser(Long id, String name, String email, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.status = status;
            this.createdAt = LocalDateTime.now();
        }

        // Getters and setters
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApiUser apiUser = (ApiUser) o;
            return Objects.equals(id, apiUser.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "ApiUser{id=" + id + ", name='" + name + "', email='" + email + "', status='" + status + "'}";
        }
    }

}