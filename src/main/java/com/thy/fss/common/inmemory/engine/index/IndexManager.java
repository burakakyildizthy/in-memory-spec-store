package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Central manager for all indexes in the DataSync Engine.
 * Manages index lifecycle (creation, caching, invalidation) and provides
 * a simple HashMap-based cache with no concurrency overhead.
 * This class is designed for single-threaded use in DataSyncEngine.
 *
 * <p>Supports both single-field indexes (via NestedTreeMapIndex) and
 * composite key indexes (via CompositeKeyIndex).</p>
 */
public final class IndexManager {

    private static final String DATASOURCE_NAME_CANNOT_BE_NULL = "datasourceName cannot be null";
    private final Map<IndexKey, NestedTreeMapIndex<?>> indexCache;
    private final Map<IndexKey, CompositeKeyIndex<?>> compositeKeyIndexes;
    private final IndexDefinitionRegistry definitionRegistry;

    /**
     * Creates a new IndexManager with an empty cache.
     */
    public IndexManager() {
        this.indexCache = new HashMap<>();
        this.compositeKeyIndexes = new HashMap<>();
        this.definitionRegistry = new IndexDefinitionRegistry();
    }

    /**
     * Gets an existing index from cache or creates a new one.
     * This method performs cache lookup first, and only builds the index if not found.
     *
     * @param <T>            The entity type
     * @param datasourceName The name of the datasource
     * @param definition     The index definition
     * @param data           The data to index (only used if index needs to be created)
     * @return The cached or newly created index
     * @throws NullPointerException if any parameter is null
     */
    public <T> NestedTreeMapIndex<T> getOrCreateIndex(
            String datasourceName,
            IndexDefinition<T> definition,
            List<T> data) {

        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);
        Objects.requireNonNull(definition, "definition cannot be null");
        Objects.requireNonNull(data, "data cannot be null");

        // Check if custom definition exists in registry
        IndexDefinition<T> registeredDefinition = definitionRegistry.getOrCreateDefault(
                datasourceName,
                definition
        );

        // Create cache key
        IndexKey cacheKey = new IndexKey(datasourceName, registeredDefinition.getKeyFieldNames());

        // Check cache
        @SuppressWarnings("unchecked")
        NestedTreeMapIndex<T> cachedIndex = (NestedTreeMapIndex<T>) indexCache.get(cacheKey);

        if (cachedIndex != null) {
            return cachedIndex;
        }

        // Create new index
        NestedTreeMapIndex<T> newIndex = new NestedTreeMapIndex<>(registeredDefinition);
        newIndex.build(data);

        // Cache it
        indexCache.put(cacheKey, newIndex);

        return newIndex;
    }

    /**
     * Invalidates all indexes for a specific datasource.
     * The indexes will be removed from cache and recreated on next access.
     *
     * @param datasourceName The name of the datasource
     * @throws NullPointerException if datasourceName is null
     */
    public void invalidateIndex(String datasourceName) {
        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);

        // Remove all indexes for this datasource
        indexCache.entrySet().removeIf(entry ->
                entry.getKey().getDatasourceName().equals(datasourceName)
        );
    }

    /**
     * Clears all indexes from the cache.
     * This is typically called at the start of a new synchronization cycle.
     */
    public void clearAllIndexes() {
        indexCache.clear();
        compositeKeyIndexes.clear();
    }

    /**
     * Clears all indexes with deep cleanup of nested structures.
     * Ensures nested maps are recursively cleared for proper garbage collection.
     * This method is idempotent and safe to call multiple times.
     * 
     * <p>This method performs the following operations:</p>
     * <ul>
     * <li>Calls deepClear() on all NestedTreeMapIndex instances</li>
     * <li>Calls deepClear() on all CompositeKeyIndex instances</li>
     * <li>Clears both indexCache and compositeKeyIndexes maps</li>
     * </ul>
     */
    public void clearAllIndexesDeep() {
        // Deep clear all single-field indexes
        for (NestedTreeMapIndex<?> index : indexCache.values()) {
            index.deepClear();
        }
        indexCache.clear();
        
        // Deep clear all composite key indexes
        for (CompositeKeyIndex<?> index : compositeKeyIndexes.values()) {
            index.deepClear();
        }
        compositeKeyIndexes.clear();
    }

    /**
     * Registers a custom index definition for a datasource.
     * This should be called before the first index creation for that datasource.
     *
     * @param <T>            The entity type
     * @param datasourceName The name of the datasource
     * @param definition     The index definition
     * @throws NullPointerException     if datasourceName or definition is null
     * @throws IllegalArgumentException if a definition is already registered
     */
    public <T> void registerIndexDefinition(String datasourceName, IndexDefinition<T> definition) {
        definitionRegistry.register(datasourceName, definition);
    }

    /**
     * Gets statistics for all cached indexes.
     * This is a lazy calculation performed on-demand with no runtime overhead.
     *
     * @return A map of datasource names to their index statistics
     */
    public Map<String, IndexStatistics> getAllStatistics() {
        Map<String, IndexStatistics> statistics = new HashMap<>();

        for (Map.Entry<IndexKey, NestedTreeMapIndex<?>> entry : indexCache.entrySet()) {
            String datasourceName = entry.getKey().getDatasourceName();
            NestedTreeMapIndex<?> index = entry.getValue();

            IndexStatistics stats = index.getStatistics(datasourceName);
            statistics.put(datasourceName, stats);
        }

        return statistics;
    }

    /**
     * Checks if an index exists in the cache for a specific datasource.
     *
     * @param datasourceName The name of the datasource
     * @return true if at least one index exists for this datasource, false otherwise
     * @throws NullPointerException if datasourceName is null
     */
    public boolean hasIndex(String datasourceName) {
        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);

        return indexCache.keySet().stream()
                .anyMatch(key -> key.getDatasourceName().equals(datasourceName));
    }

    /**
     * Gets or creates a composite key index for the given data source and key paths.
     * This method performs cache lookup first, and only builds the index if not found.
     *
     * <p>Composite key indexes support multi-field keys where all fields must match
     * for a successful lookup. The index uses a nested map structure for efficient
     * lookups.</p>
     *
     * @param <T>            The entity type
     * @param dataSourceName The name of the data source
     * @param keyPaths       The list of key field paths (each path is a list of MetaAttributes)
     * @param data           The data to index (only used if index needs to be created)
     * @return The cached or newly created composite key index
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if keyPaths is empty
     */
    public <T> CompositeKeyIndex<T> getOrCreateCompositeIndex(
            String dataSourceName,
            List<List<MetaAttribute<?, ?>>> keyPaths,
            Collection<T> data) {

        Objects.requireNonNull(dataSourceName, DATASOURCE_NAME_CANNOT_BE_NULL);
        Objects.requireNonNull(keyPaths, "keyPaths cannot be null");
        Objects.requireNonNull(data, "data cannot be null");

        if (keyPaths.isEmpty()) {
            throw new IllegalArgumentException("keyPaths cannot be empty");
        }

        // Create cache key from data source name and key paths
        IndexKey cacheKey = IndexKey.fromCompositeKeyPaths(dataSourceName, keyPaths);

        // Check cache
        @SuppressWarnings("unchecked")
        CompositeKeyIndex<T> cachedIndex = (CompositeKeyIndex<T>) compositeKeyIndexes.get(cacheKey);

        if (cachedIndex != null) {
            return cachedIndex;
        }

        // Create new composite key index
        CompositeKeyIndex<T> newIndex = new CompositeKeyIndex<>(keyPaths);
        newIndex.buildIndex(data);

        // Cache it
        compositeKeyIndexes.put(cacheKey, newIndex);

        return newIndex;
    }

    /**
     * Gets the definition registry.
     * This is primarily for testing purposes.
     *
     * @return The index definition registry
     */
    IndexDefinitionRegistry getDefinitionRegistry() {
        return definitionRegistry;
    }
}
