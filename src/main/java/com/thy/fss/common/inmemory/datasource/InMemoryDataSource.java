package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory implementation of DataSource interface.
 * This implementation stores data in an internal thread-safe list and provides
 * all required DataSource operations with proper generic type handling.
 * Suitable for both production use cases and testing scenarios.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class InMemoryDataSource<T> implements DataSource<T> {

    private final String name;
    private final Class<T> entityType;
    private final List<T> data;
    private final AtomicBoolean healthy;
    private volatile DataSource<T> fallbackDataSource;

    /**
     * Creates a new InMemoryDataSource with the specified name and entity type.
     *
     * @param name       the name of this DataSource
     * @param entityType the Class object representing the entity type
     */
    public InMemoryDataSource(String name, Class<T> entityType) {
        this.name = Objects.requireNonNull(name, "DataSource name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.data = new CopyOnWriteArrayList<>();
        this.healthy = new AtomicBoolean(true);
    }

    /**
     * Creates a new InMemoryDataSource with initial data.
     *
     * @param name        the name of this DataSource
     * @param entityType  the Class object representing the entity type
     * @param initialData the initial data to store
     */
    public InMemoryDataSource(String name, Class<T> entityType, Collection<T> initialData) {
        this(name, entityType);
        if (initialData != null) {
            this.data.addAll(initialData);
        }
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
            if (!isHealthy()) {
                throw new DataSourceException("DataSource '" + name + "' is not healthy");
            }
            // Return a copy to prevent external modification
            return new ArrayList<>(data);
        });
    }

    @Override
    public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isHealthy()) {
                throw new DataSourceException("DataSource '" + name + "' is not healthy");
            }

            if (ids == null || ids.isEmpty()) {
                return new ArrayList<>();
            }

            // Convert IDs to Set for faster lookup
            Set<Object> idSet = new HashSet<>(ids);

            return data.stream()
                    .filter(item -> {
                        // Try to find an ID field in the entity
                        Object itemId = extractIdFromEntity(item);
                        return itemId != null && idSet.contains(itemId);
                    })
                    .toList();
        });
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    /**
     * Sets the health status of this DataSource.
     * This method is protected to allow subclasses to control health status for testing.
     *
     * @param healthy true to mark as healthy, false to mark as unhealthy
     */
    protected void setHealthy(boolean healthy) {
        this.healthy.set(healthy);
    }

    @Override
    public void close() {
        data.clear();
        healthy.set(false);
    }

    @Override
    public Optional<DataSource<T>> getFallbackDataSource() {
        return Optional.ofNullable(fallbackDataSource);
    }

    // Additional utility methods

    @Override
    public void setFallbackDataSource(DataSource<T> fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;
    }

    /**
     * Adds a single item to this DataSource.
     *
     * @param item the item to add
     */
    public void addItem(T item) {
        if (item != null) {
            data.add(item);
        }
    }

    /**
     * Adds multiple items to this DataSource.
     *
     * @param items the items to add
     */
    public void addItems(Collection<T> items) {
        if (items != null) {
            data.addAll(items);
        }
    }

    /**
     * Clears all data from this DataSource.
     */
    public void clearData() {
        data.clear();
    }

    /**
     * Returns the current number of items in this DataSource.
     *
     * @return the number of items
     */
    public int size() {
        return data.size();
    }

    /**
     * Extracts the ID from an entity using the Identifiable interface.
     * This replaces reflection-based ID extraction for type safety and performance.
     *
     * @param entity the entity to extract ID from
     * @return the entity's ID, or the entity itself if not Identifiable
     */
    @SuppressWarnings("unchecked")
    private Object extractIdFromEntity(T entity) {
        if (entity == null) {
            return null;
        }

        // Use Identifiable interface if available (preferred approach)
        if (entity instanceof com.thy.fss.common.inmemory.entity.Identifiable) {
            return ((com.thy.fss.common.inmemory.entity.Identifiable<?>) entity).getIdentity();
        }

        throw new IllegalStateException("Entity of type " + entity.getClass().getName() +
                " does not implement Identifiable interface. Cannot extract ID. Implement Identifiable interface !");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InMemoryDataSource<?> that = (InMemoryDataSource<?>) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(entityType, that.entityType) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, entityType, data);
    }
}