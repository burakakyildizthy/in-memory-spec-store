package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceException;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic DataSource interface for providing data to the in-memory data library.
 * This interface supports both primary DataSource usage (fetching all data) and
 * secondary DataSource usage (fetching data by specific IDs).
 *
 * <p>DataSources provide the foundation for the multi-source data aggregation
 * capability of the library. They can be chained together using fallback
 * mechanisms to provide resilience and high availability.</p>
 * <p>
 * DataSource Types:
 * <ul>
 *   <li><strong>Primary DataSource:</strong> Provides the main entity list using {@link #fetchAll()}</li>
 *   <li><strong>Secondary DataSource:</strong> Provides related data using {@link #fetchAllById(Collection)}</li>
 *   <li><strong>Fallback DataSource:</strong> Provides alternative data when primary sources fail</li>
 * </ul>
 * <p>
 * Built-in Implementations:
 * <ul>
 *   <li>{@code InMemoryDataSource} - For testing and caching</li>
 *   <li>{@code DatabaseDataSource} - JDBC-based database access</li>
 *   <li>{@code RestApiDataSource} - HTTP REST API access</li>
 *   <li>{@code FileDataSource} - File-based data access</li>
 * </ul>
 * <p>
 * Fallback Chain Example:
 * <pre>{@code
 * // Create fallback chain: Database -> Cache -> File
 * DataSource<User> primary = new DatabaseDataSource<>("users", User.class);
 * DataSource<User> cache = new InMemoryDataSource<>("cache", User.class);
 * DataSource<User> file = new FileDataSource<>("backup.json", User.class);
 *
 * primary.setFallbackDataSource(cache);
 * cache.setFallbackDataSource(file);
 *
 * // Automatic fallback handling
 * List<User> users = primary.fetchAllWithFallback().get();
 * }</pre>
 * <p>
 * Implementation Guidelines:
 * <ul>
 *   <li>Implement proper error handling and logging</li>
 *   <li>Make operations asynchronous using CompletableFuture</li>
 *   <li>Provide meaningful health checks</li>
 *   <li>Handle resource cleanup in {@link #close()}</li>
 *   <li>Use appropriate timeouts for external calls</li>
 * </ul>
 *
 * @param <T> The type of entities this DataSource provides
 * @see InMemoryDataStore
 * @since 1.0
 */
public interface DataSource<T> {

    /**
     * Returns the name of this DataSource for identification and logging purposes.
     *
     * @return the name of this DataSource
     */
    String getName();

    /**
     * Returns the entity type that this DataSource provides.
     * This method enables generic type information access at runtime.
     *
     * @return the Class object representing the entity type
     */
    Class<T> getEntityType();



    /**
     * Fetches all available data from this DataSource.
     * This method is primarily used by primary DataSources to provide the main data list.
     *
     * <p>Implementations should return all available entities without any filtering.
     * The returned CompletableFuture should complete with a list of entities,
     * or complete exceptionally if the operation fails.</p>
     *
     * <h4>Implementation Notes:</h4>
     * <ul>
     *   <li>Should be non-blocking and return immediately</li>
     *   <li>Actual data fetching should happen asynchronously</li>
     *   <li>Should handle timeouts and connection issues gracefully</li>
     *   <li>May return empty list if no data is available</li>
     *   <li>Should not return null - use empty list instead</li>
     * </ul>
     *
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * DataSource<User> userDataSource = new DatabaseDataSource<>("users", User.class);
     *
     * userDataSource.fetchAll()
     *     .thenAccept(users -> {
     *         System.out.println("Fetched " + users.size() + " users");
     *     })
     *     .exceptionally(throwable -> {
     *         logger.error("Failed to fetch users", throwable);
     *         return null;
     *     });
     * }</pre>
     *
     * @return a CompletableFuture containing the list of all entities, never null
     * @see #fetchAllWithFallback()
     */
    CompletableFuture<List<T>> fetchAll();

    /**
     * Fetches data from this DataSource filtered by the specified IDs.
     * This method is primarily used by secondary DataSources to fetch related data
     * based on foreign key relationships.
     *
     * <p>Implementations should return only entities whose foreign key field
     * matches one of the provided IDs. This enables efficient loading of
     * related data for nested object hierarchies.</p>
     *
     * <h4>Implementation Notes:</h4>
     * <ul>
     *   <li>Should filter data based on the configured foreign key field</li>
     *   <li>May return empty list if no matching entities are found</li>
     *   <li>Should handle null or empty ID collections gracefully</li>
     *   <li>Should be optimized for batch operations when possible</li>
     *   <li>Should not return null - use empty list instead</li>
     * </ul>
     *
     * <h4>Usage Example:</h4>
     * <pre>{@code
     * // Fetch orders for specific user IDs
     * Collection<Object> userIds = Arrays.asList(1L, 2L, 3L);
     * DataSource<Order> orderDataSource = new DatabaseDataSource<>("orders", Order.class);
     *
     * orderDataSource.fetchAllById(userIds)
     *     .thenAccept(orders -> {
     *         System.out.println("Found " + orders.size() + " orders for users");
     *     });
     * }</pre>
     *
     * @param ids the collection of IDs to filter by, may be null or empty
     * @return a CompletableFuture containing the list of entities matching the specified IDs, never null
     * @see #fetchAllByIdWithFallback(Collection)
     */
    CompletableFuture<List<T>> fetchAllById(Collection<Object> ids);

    // Lifecycle and monitoring methods

    /**
     * Checks if this DataSource is currently healthy and able to provide data.
     *
     * @return true if the DataSource is healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Closes this DataSource and releases any resources it holds.
     * After calling this method, the DataSource should not be used anymore.
     */
    void close();

    // Fallback DataSource support

    /**
     * Returns the fallback DataSource that should be used when this DataSource fails.
     * The fallback DataSource provides an alternative source of data for resilience.
     *
     * @return an Optional containing the fallback DataSource, or empty if no fallback is configured
     */
    Optional<DataSource<T>> getFallbackDataSource();

    /**
     * Sets the fallback DataSource that should be used when this DataSource fails.
     *
     * @param fallbackDataSource the fallback DataSource to set
     */
    void setFallbackDataSource(DataSource<T> fallbackDataSource);

    // Default methods for fallback support

    /**
     * Safely gets the fallback DataSource, handling any exceptions that might occur.
     *
     * @param currentDataSource the current DataSource to get fallback from
     * @param logger            the logger to use for error messages
     * @return the fallback DataSource or null if not available or exception occurred
     */
    default DataSource<T> safeGetFallbackDataSource(DataSource<T> currentDataSource, Logger logger) {
        try {
            return currentDataSource.getFallbackDataSource().orElse(null);
        } catch (Exception e) {
            logger.error("Exception while getting fallback DataSource from {}: {}",
                    currentDataSource.getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches all data with automatic fallback chain traversal.
     * This default implementation handles the fallback logic automatically,
     * trying each DataSource in the fallback chain until one succeeds or all fail.
     *
     * @return a CompletableFuture containing the list of all entities with fallback support
     */
    default CompletableFuture<List<T>> fetchAllWithFallback() {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        return CompletableFuture.supplyAsync(() ->
                traverseFallbackChain(logger, ds -> ds.fetchAll().get(), "fetch all data from")
        );
    }

    /**
     * Fetches data by IDs with automatic fallback chain traversal.
     * This default implementation handles the fallback logic automatically,
     * trying each DataSource in the fallback chain until one succeeds or all fail.
     *
     * @param ids the collection of IDs to filter by
     * @return a CompletableFuture containing the list of entities matching the specified IDs with fallback support
     */
    default CompletableFuture<List<T>> fetchAllByIdWithFallback(Collection<Object> ids) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        return CompletableFuture.supplyAsync(() ->
                traverseFallbackChain(logger, ds -> ds.fetchAllById(ids).get(), "fetch data by IDs from")
        );
    }

    /**
     * Traverses the fallback chain, attempting the given fetch operation on each DataSource.
     *
     * @param logger the logger to use
     * @param fetchOperation the fetch operation to attempt on each DataSource
     * @param operationDescription description of the operation for logging
     * @return the fetched data, or an empty list if all DataSources failed
     */
    default List<T> traverseFallbackChain(
            Logger logger,
            FallbackFetchOperation<T> fetchOperation,
            String operationDescription) {

        DataSource<T> currentDataSource = this;
        int fallbackLevel = 0;

        while (currentDataSource != null) {
            String currentDataSourceName = currentDataSource.getName();

            try {
                logger.debug("Attempting to {} DataSource: {} (fallback level: {})",
                        operationDescription, currentDataSourceName, fallbackLevel);

                if (!currentDataSource.isHealthy()) {
                    logger.warn("DataSource {} is not healthy, trying fallback", currentDataSourceName);
                    currentDataSource = safeGetFallbackDataSource(currentDataSource, logger);
                    fallbackLevel++;
                    continue;
                }

                List<T> data = fetchOperation.fetch(currentDataSource);

                if (data == null) {
                    logger.warn("DataSource {} returned null data, trying fallback", currentDataSourceName);
                    currentDataSource = safeGetFallbackDataSource(currentDataSource, logger);
                    fallbackLevel++;
                    continue;
                }

                logger.debug("Successfully fetched {} items from DataSource: {} (fallback level: {})",
                        data.size(), currentDataSourceName, fallbackLevel);
                return data;

            } catch (DataSourceException e) {
                logger.error("DataSource exception from {}: {}", currentDataSourceName, e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while fetching from DataSource {}: {}",
                        currentDataSourceName, e.getMessage(), e);
                return new ArrayList<>();
            } catch (Exception e) {
                logger.error("Unexpected exception from DataSource {}: {}",
                        currentDataSourceName, e.getMessage(), e);
            }

            currentDataSource = safeGetFallbackDataSource(currentDataSource, logger);
            fallbackLevel++;

            if (currentDataSource != null) {
                logger.info("Trying fallback DataSource: {} (level: {})",
                        currentDataSource.getName(), fallbackLevel);
            }
        }

        logger.error("All DataSources in fallback chain failed for {} (tried {} levels), returning empty list",
                this.getName(), fallbackLevel);
        return new ArrayList<>();
    }

    /**
     * Functional interface for fetch operations used in fallback chain traversal.
     */
    @FunctionalInterface
    interface FallbackFetchOperation<T> {
        List<T> fetch(DataSource<T> dataSource) throws Exception;
    }
}