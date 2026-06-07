package com.thy.fss.common.inmemory.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST API-backed DataSource implementation using Spring's RestTemplate.
 * Supports both fetchAll() and fetchAllById() operations with JSON response parsing.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class RestApiDataSource<T> implements DataSource<T> {

    private static final Logger logger = LoggerFactory.getLogger(RestApiDataSource.class);
    private static final String HTTP_REQUEST_FAILED_WITH_STATUS = "HTTP request failed with status: ";

    private final String name;
    private final Class<T> entityType;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String healthEndpoint;
    private final ObjectMapper objectMapper;
    private DataSource<T> fallbackDataSource;

    /**
     * Creates a new RestApiDataSource with default configuration.
     *
     * @param name       the name of this DataSource for identification
     * @param entityType the type of entities this DataSource provides
     * @param baseUrl    the base URL of the REST API
     */
    public RestApiDataSource(String name, Class<T> entityType, String baseUrl) {
        this(name, entityType, baseUrl, null, new RestTemplate(), new ObjectMapper());
    }

    /**
     * Creates a new RestApiDataSource with custom configuration.
     *
     * @param name           the name of this DataSource for identification
     * @param entityType     the type of entities this DataSource provides
     * @param baseUrl        the base URL of the REST API
     * @param healthEndpoint the health check endpoint (optional, can be null)
     * @param restTemplate   the RestTemplate to use for HTTP operations
     * @param objectMapper   the ObjectMapper to use for JSON parsing
     */
    public RestApiDataSource(String name, Class<T> entityType, String baseUrl, String healthEndpoint,
                             RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.name = Objects.requireNonNull(name, "DataSource name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.healthEndpoint = healthEndpoint;
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");

        logger.info("Created RestApiDataSource '{}' for entity type {} with base URL '{}'",
                name, entityType.getSimpleName(), baseUrl);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getEntityType() {
        return entityType;
    }

    @Override
    public CompletableFuture<List<T>> fetchAll() {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl;
            logger.debug("Executing fetchAll HTTP GET request to: {}", url);
            return executeHttpGet(url, "fetch all data from");
        });
    }

    @Override
    public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
        return CompletableFuture.supplyAsync(() -> {
            if (ids == null || ids.isEmpty()) {
                logger.debug("No IDs provided for fetchAllById, returning empty list");
                return new ArrayList<>();
            }

            String idsParam = ids.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));

            String url = baseUrl + "?ids=" + idsParam;
            logger.debug("Executing fetchAllById HTTP GET request to: {} with {} IDs", url, ids.size());
            return executeHttpGet(url, "fetch data by IDs from");
        });
    }

    private List<T> executeHttpGet(String url, String operationDescription) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    logger.debug("Received empty response body from {}", url);
                    return new ArrayList<>();
                }

                List<T> results = parseJsonResponse(responseBody);
                logger.debug("Successfully fetched {} records from REST API '{}'", results.size(), url);
                return results;
            } else {
                throw new DataSourceConnectionException(
                        HTTP_REQUEST_FAILED_WITH_STATUS + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorMessage = HTTP_REQUEST_FAILED_WITH_STATUS + e.getStatusCode().value();
            logger.error("Failed to {} REST API '{}': {}", operationDescription, baseUrl, errorMessage, e);
            throw new DataSourceConnectionException(errorMessage, e);
        } catch (RestClientException e) {
            logger.error("Failed to {} REST API '{}': {}", operationDescription, baseUrl, e.getMessage(), e);
            throw new DataSourceConnectionException(
                    "Failed to " + operationDescription + " REST API '" + baseUrl + "'", e);
        } catch (Exception e) {
            logger.error("Unexpected error to {} REST API '{}': {}", operationDescription, baseUrl, e.getMessage(), e);
            throw new DataSourceConnectionException(
                    "Unexpected error to " + operationDescription + " REST API '" + baseUrl + "'", e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            String healthUrl = healthEndpoint != null ? healthEndpoint : baseUrl;
            logger.debug("Performing health check for RestApiDataSource '{}' at: {}", name, healthUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Use HEAD request if health endpoint is specified, otherwise GET
            HttpMethod method = healthEndpoint != null ? HttpMethod.HEAD : HttpMethod.GET;

            ResponseEntity<String> response = restTemplate.exchange(
                    healthUrl, method, entity, String.class);

            boolean healthy = response.getStatusCode().is2xxSuccessful();
            logger.debug("Health check {} for RestApiDataSource '{}'",
                    healthy ? "passed" : "failed", name);

            return healthy;

        } catch (RestClientException e) {
            logger.warn("Health check failed for RestApiDataSource '{}': {}", name, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("Unexpected error during health check for RestApiDataSource '{}': {}",
                    name, e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        logger.info("Closing RestApiDataSource '{}'", name);
        // RestTemplate doesn't need explicit closing
        // Any connection pooling cleanup would be handled by the HTTP client configuration
    }

    @Override
    public Optional<DataSource<T>> getFallbackDataSource() {
        return Optional.ofNullable(fallbackDataSource);
    }

    @Override
    public void setFallbackDataSource(DataSource<T> fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;
        logger.info("Set fallback DataSource for '{}': {}",
                name, fallbackDataSource != null ? fallbackDataSource.getName() : "null");
    }

    /**
     * Parses JSON response body into a list of entities.
     *
     * @param responseBody the JSON response body
     * @return the parsed list of entities
     * @throws DataSourceConnectionException if JSON parsing fails
     */
    private List<T> parseJsonResponse(String responseBody) {
        try {
            // Try to parse as array first
            if (responseBody.trim().startsWith("[")) {
                return objectMapper.readValue(responseBody,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, entityType));
            } else {
                // Try to parse as single object and wrap in list
                T singleObject = objectMapper.readValue(responseBody, entityType);
                return Collections.singletonList(singleObject);
            }
        } catch (Exception e) {
            logger.error("Failed to parse JSON response: {}", e.getMessage(), e);
            throw new DataSourceConnectionException("Failed to parse JSON response", e);
        }
    }

    /**
     * Gets the base URL of the REST API.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the health endpoint URL.
     *
     * @return the health endpoint URL, or null if not configured
     */
    public String getHealthEndpoint() {
        return healthEndpoint;
    }

    /**
     * Gets the underlying RestTemplate for advanced operations.
     *
     * @return the RestTemplate instance
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * Gets the ObjectMapper used for JSON parsing.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}