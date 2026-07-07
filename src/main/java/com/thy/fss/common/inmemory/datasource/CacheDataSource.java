package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory cache-based DataSource implementation for fallback scenarios.
 * Provides cached data with configurable TTL (Time To Live) and refresh strategies.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class CacheDataSource<T> implements DataSource<T> {

    private static final Logger logger = LoggerFactory.getLogger(CacheDataSource.class);

    private final String name;
    private final Class<T> entityType;
    private final Function<T, Object> idExtractor;
    private final Duration ttl;
    private final Map<Object, T> cache;
    private final Map<Object, LocalDateTime> cacheTimestamps;
    private DataSource<T> fallbackDataSource;
    private volatile LocalDateTime lastFullRefresh;
    private volatile boolean healthy;

    /**
     * Creates a new CacheDataSource with default TTL of 1 hour.
     *
     * @param name        the name of this DataSource for identification
     * @param entityType  the type of entities this DataSource provides
     * @param idExtractor function to extract ID from entity
     */
    public CacheDataSource(String name, Class<T> entityType, Function<T, Object> idExtractor) {
        this(name, entityType, idExtractor, Duration.ofHours(1));
    }

    /**
     * Creates a new CacheDataSource with custom TTL.
     *
     * @param name        the name of this DataSource for identification
     * @param entityType  the type of entities this DataSource provides
     * @param idExtractor function to extract ID from entity
     * @param ttl         the time-to-live for cached entries
     */
    public CacheDataSource(String name, Class<T> entityType,
                           Function<T, Object> idExtractor, Duration ttl) {
        this.name = Objects.requireNonNull(name, "DataSource name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.idExtractor = Objects.requireNonNull(idExtractor, "ID extractor cannot be null");
        this.ttl = Objects.requireNonNull(ttl, "TTL cannot be null");

        this.cache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
        this.healthy = true;

        logger.info("Created CacheDataSource '{}' for entity type {} with TTL {}",
                name, entityType.getSimpleName(), ttl);
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
            try {
                cleanExpiredEntries();

                List<T> result = new ArrayList<>(cache.values());
                logger.debug("Returning {} cached records from CacheDataSource '{}'", result.size(), name);

                return result;

            } catch (Exception e) {
                throw new DataSourceConnectionException(
                        "Failed to fetch all data from cache '" + name + "'", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
        return CompletableFuture.supplyAsync(() -> {
            if (ids == null || ids.isEmpty()) {
                logger.debug("No IDs provided for fetchAllById, returning empty list");
                return new ArrayList<>();
            }

            try {
                cleanExpiredEntries();

                List<T> results = ids.stream()
                        .map(cache::get)
                        .filter(Objects::nonNull)
                        .toList();

                logger.debug("Returning {} cached records from CacheDataSource '{}' for {} IDs",
                        results.size(), name, ids.size());

                return results;

            } catch (Exception e) {
                throw new DataSourceConnectionException(
                        "Failed to fetch data by IDs from cache '" + name + "'", e);
            }
        });
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Sets the health status of this cache.
     *
     * @param healthy the health status
     */
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
        logger.debug("Set health status of CacheDataSource '{}' to {}", name, healthy);
    }

    @Override
    public void close() {
        logger.info("Closing CacheDataSource '{}'", name);
        cache.clear();
        cacheTimestamps.clear();
        healthy = false;
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
     * Adds or updates entities in the cache.
     *
     * @param entities the entities to cache
     */
    public void cacheEntities(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (T entity : entities) {
            try {
                Object id = idExtractor.apply(entity);
                if (id != null) {
                    cache.put(id, entity);
                    cacheTimestamps.put(id, now);
                }
            } catch (Exception e) {
                logger.warn("Failed to extract ID from entity for caching: {}", e.getMessage());
            }
        }

        lastFullRefresh = now;
        logger.info("Cached {} entities in CacheDataSource '{}'", entities.size(), name);
    }

    /**
     * Adds or updates a single entity in the cache.
     *
     * @param entity the entity to cache
     */
    public void cacheEntity(T entity) {
        if (entity == null) {
            return;
        }

        try {
            Object id = idExtractor.apply(entity);
            if (id != null) {
                LocalDateTime now = LocalDateTime.now();
                cache.put(id, entity);
                cacheTimestamps.put(id, now);
                logger.debug("Cached entity with ID {} in CacheDataSource '{}'", id, name);
            }
        } catch (Exception e) {
            logger.warn("Failed to cache entity: {}", e.getMessage());
        }
    }

    /**
     * Removes an entity from the cache by ID.
     *
     * @param id the ID of the entity to remove
     */
    public void evictEntity(Object id) {
        if (id != null) {
            cache.remove(id);
            cacheTimestamps.remove(id);
            logger.debug("Evicted entity with ID {} from CacheDataSource '{}'", id, name);
        }
    }

    /**
     * Clears all cached entities.
     */
    public void clearCache() {
        cache.clear();
        cacheTimestamps.clear();
        lastFullRefresh = null;
        logger.info("Cleared all cached entities from CacheDataSource '{}'", name);
    }

    /**
     * Gets the current cache size.
     *
     * @return the number of cached entities
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Gets the TTL configuration.
     *
     * @return the TTL duration
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Gets the last full refresh timestamp.
     *
     * @return the last full refresh timestamp, or null if never refreshed
     */
    public LocalDateTime getLastFullRefresh() {
        return lastFullRefresh;
    }

    /**
     * Checks if the cache needs a full refresh based on TTL.
     *
     * @return true if a full refresh is needed
     */
    public boolean needsFullRefresh() {
        if (lastFullRefresh == null) {
            return true;
        }

        return LocalDateTime.now().isAfter(lastFullRefresh.plus(ttl));
    }

    /**
     * Gets cache statistics.
     *
     * @return a map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", cache.size());
        stats.put("ttl", ttl.toString());
        stats.put("lastFullRefresh", lastFullRefresh);
        stats.put("needsFullRefresh", needsFullRefresh());
        stats.put("healthy", healthy);

        return stats;
    }

    private void cleanExpiredEntries() {
        if (ttl.isZero() || ttl.isNegative()) {
            return; // No expiration
        }

        LocalDateTime now = LocalDateTime.now();
        List<Object> expiredIds = new ArrayList<>();

        for (Map.Entry<Object, LocalDateTime> entry : cacheTimestamps.entrySet()) {
            if (now.isAfter(entry.getValue().plus(ttl))) {
                expiredIds.add(entry.getKey());
            }
        }

        if (!expiredIds.isEmpty()) {
            for (Object id : expiredIds) {
                cache.remove(id);
                cacheTimestamps.remove(id);
            }
            logger.debug("Cleaned {} expired entries from CacheDataSource '{}'", expiredIds.size(), name);
        }
    }
}