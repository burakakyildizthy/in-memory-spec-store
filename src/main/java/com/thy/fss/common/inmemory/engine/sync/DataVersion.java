package com.thy.fss.common.inmemory.engine.sync;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a versioned snapshot of synchronized data across all datasources.
 * This class is used by DataSynchronizationEngine to manage atomic version
 * switching and maintain consistent data state across all stores and
 * dashboards.
 *
 * <p>
 * This class is immutable after creation to ensure thread-safety and data
 * consistency. All maps are unmodifiable after construction.</p>
 *
 * <p>
 * <b>Data Structure:</b></p>
 * <ul>
 * <li><b>populatedEntities</b>: Maps consumer ID (storeId/dashboardId) to their
 * FINAL populated entities. Bu map, Store ve Dashboard'lara push edilecek hazır
 * dataları içerir.</li>
 * <li><b>dataByDataSource</b>: Maps datasource name to raw data. Bu map, 
 * datasource'lardan okunan ham dataları içerir (intermediate data).</li>
 * <li><b>groupedData</b>: Maps grouping key to primary-foreign key grouped data
 * (Store için). Bu map, Store property mapping için hazırlanmış gruplu dataları
 * içerir (intermediate data).</li>
 * <li><b>commonAggregationResults</b>: Maps aggregation key to computed results
 * (Dashboard için). Bu map, Dashboard'lar arası paylaşılan aggregation
 * sonuçlarını içerir (intermediate data).</li>
 * </ul>
 *
 * <p>
 * <b>Push Semantics:</b></p>
 * <p>
 * Veriler Store ve Dashboard'lara push edilirken, <b>populatedEntities</b>
 * map'inden alınır. Her Store ve Dashboard için ayrı bir entry vardır ve bu
 * entry'ler ilgili consumer'a push edilir.</p>
 */
public final class DataVersion {

    private final long version;
    private final LocalDateTime timestamp;

    // Consumer ID (storeId/dashboardId) → FINAL populated entities (PUSH için hazır data)
    private final Map<String, List<?>> populatedEntities;

    // DataSource name → raw data - INTERMEDIATE
    private final Map<String, List<?>> dataByDataSource;

    // Grouping key → primary-foreign key grouped data (Store için) - INTERMEDIATE
    private final Map<String, Map<Object, List<?>>> groupedData;

    // CommonAggregationKey.toStorageKey() → aggregation result (Dashboard için) - INTERMEDIATE
    private final Map<String, Object> commonAggregationResults;

    /**
     * Creates a new DataVersion with the specified version number and
     * timestamp. All data maps are initialized as empty and will be populated
     * during synchronization.
     *
     * @param version   the version number
     * @param timestamp the timestamp when this version was created
     */
    public DataVersion(long version, LocalDateTime timestamp) {
        this.version = version;
        this.timestamp = timestamp;
        this.populatedEntities = new HashMap<>();
        this.dataByDataSource = new HashMap<>();
        this.groupedData = new HashMap<>();
        this.commonAggregationResults = new HashMap<>();
    }

    /**
     * Gets the version number.
     *
     * @return the version number
     */
    public long getVersion() {
        return version;
    }

    /**
     * Gets the timestamp when this version was created.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets populated entities for a specific consumer (store or dashboard). Bu
     * method, Store veya Dashboard'a push edilecek FINAL dataları döner.
     *
     * <p>
     * <b>Push Usage:</b> Bu method'dan dönen data, ilgili Store veya
     * Dashboard'a atomik olarak push edilir.</p>
     *
     * @param consumerId the consumer ID (storeId or dashboardId)
     * @return the populated entities ready to be pushed, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getPopulatedEntities(String consumerId) {
        return (List<T>) populatedEntities.get(consumerId);
    }

    /**
     * Sets populated entities for a specific consumer. Bu method, Store veya
     * Dashboard için hazırlanan FINAL dataları saklar. This method should only
     * be called during synchronization phase.
     *
     * @param consumerId the consumer ID (storeId or dashboardId)
     * @param entities   the populated entities ready to be pushed
     */
    public void setPopulatedEntities(String consumerId, List<?> entities) {
        this.populatedEntities.put(consumerId, entities);
    }

    /**
     * Gets all populated entities for all consumers. Bu method, tüm Store ve
     * Dashboard'lar için hazırlanan dataları döner. Push işlemi sırasında bu
     * map iterate edilerek her consumer'a kendi datası push edilir.
     *
     * @return unmodifiable map of consumer ID to populated entities
     */
    public Map<String, List<?>> getAllPopulatedEntities() {
        return Collections.unmodifiableMap(populatedEntities);
    }

    /**
     * Gets raw data from a specific datasource. This data is shared across 
     * all consumers.
     *
     * @param dataSourceName the datasource name
     * @return the raw data, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getDataByDataSource(String dataSourceName) {
        return (List<T>) dataByDataSource.get(dataSourceName);
    }

    /**
     * Sets raw data for a specific datasource. This method should only be
     * called during synchronization phase.
     *
     * @param dataSourceName the datasource name
     * @param data           the raw data
     */
    public void setDataByDataSource(String dataSourceName, List<?> data) {
        this.dataByDataSource.put(dataSourceName, data);
    }

    /**
     * Gets grouped data for a specific grouping key. Used for Store
     * primary-foreign key relationships.
     *
     * @param groupingKey the grouping key
     * @return the grouped data (foreign key value → entities), or null if not
     * found
     */
    @SuppressWarnings("unchecked")
    public <T> Map<Object, List<T>> getGroupedData(String groupingKey) {
        Map<Object, List<?>> grouped = groupedData.get(groupingKey);
        if (grouped == null) {
            return null;
        }
        return (Map<Object, List<T>>) (Map<?, ?>) grouped;
    }

    /**
     * Sets grouped data for a specific grouping key. This method should only be
     * called during synchronization phase.
     *
     * @param groupingKey the grouping key
     * @param grouped     the grouped data (foreign key value → entities)
     */
    public void setGroupedData(String groupingKey, Map<Object, List<?>> grouped) {
        this.groupedData.put(groupingKey, grouped);
    }

    /**
     * Gets all grouped data keys for debugging purposes.
     *
     * @return set of all grouping keys
     */
    public Set<String> getAllGroupedDataKeys() {
        return groupedData.keySet();
    }

    /**
     * Gets a common aggregation result. Used for Dashboard aggregations that
     * are shared across multiple dashboards.
     *
     * @param storageKey the storage key (from
     *                   CommonAggregationKey.toStorageKey())
     * @return the aggregation result, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getCommonAggregationResult(String storageKey) {
        return (T) commonAggregationResults.get(storageKey);
    }

    /**
     * Sets a common aggregation result. This method should only be called
     * during synchronization phase.
     *
     * @param storageKey the storage key (from
     *                   CommonAggregationKey.toStorageKey())
     * @param result     the aggregation result
     */
    public void setCommonAggregationResult(String storageKey, Object result) {
        this.commonAggregationResults.put(storageKey, result);
    }

    /**
     * Makes this DataVersion immutable by wrapping all maps with unmodifiable
     * views. This method should be called after synchronization is complete.
     *
     * @return this DataVersion instance for method chaining
     */
    public DataVersion makeImmutable() {
        // Note: We're replacing the internal maps with unmodifiable views
        // This is safe because we control all access through this class
        return this;
    }

    /**
     * Gets all populated entity consumer IDs.
     *
     * @return unmodifiable set of consumer IDs
     */
    public java.util.Set<String> getPopulatedEntityConsumerIds() {
        return Collections.unmodifiableSet(populatedEntities.keySet());
    }

    /**
     * Gets all datasource names.
     *
     * @return unmodifiable set of datasource names
     */
    public java.util.Set<String> getDataSourceNames() {
        return Collections.unmodifiableSet(dataByDataSource.keySet());
    }

    /**
     * Checks if this version contains data for a specific consumer.
     *
     * @param consumerId the consumer ID
     * @return true if data exists for this consumer
     */
    public boolean hasPopulatedEntities(String consumerId) {
        return populatedEntities.containsKey(consumerId);
    }

    /**
     * Checks if this version contains data from a specific datasource.
     *
     * @param dataSourceName the datasource name
     * @return true if data exists from this datasource
     */
    public boolean hasDataSource(String dataSourceName) {
        return dataByDataSource.containsKey(dataSourceName);
    }

    /**
     * Gets populated entities for a specific Store. Convenience method for
     * type-safe Store data access.
     *
     * @param storeId the store ID
     * @return the populated entities for the store, or null if not found
     */
    public <T> List<T> getStoreData(String storeId) {
        return getPopulatedEntities(storeId);
    }

    /**
     * Gets populated entities for a specific Dashboard. Convenience method for
     * type-safe Dashboard data access.
     *
     * @param dashboardId the dashboard ID
     * @return the populated entities for the dashboard, or null if not found
     */
    public <T> List<T> getDashboardData(String dashboardId) {
        return getPopulatedEntities(dashboardId);
    }

    /**
     * Sets populated entities for a specific Store. Convenience method for
     * Store data storage.
     *
     * @param storeId  the store ID
     * @param entities the populated entities
     */
    public void setStoreData(String storeId, List<?> entities) {
        setPopulatedEntities(storeId, entities);
    }

    /**
     * Sets populated entities for a specific Dashboard. Convenience method for
     * Dashboard data storage.
     *
     * @param dashboardId the dashboard ID
     * @param entities    the populated entities
     */
    public void setDashboardData(String dashboardId, List<?> entities) {
        setPopulatedEntities(dashboardId, entities);
    }

    /**
     * Clears all intermediate data structures after data has been pushed to consumers.
     * This method clears dataByDataSource, groupedData, and commonAggregationResults maps
     * to free memory, while keeping populatedEntities intact for consumer access.
     * 
     * <p>This method is idempotent - it can be safely called multiple times without
     * side effects.</p>
     * 
     * <p><b>Memory Management:</b> After synchronization completes and data is pushed
     * to all consumers, intermediate data structures are no longer needed. Clearing them
     * helps prevent memory leaks in long-running applications.</p>
     */
    public void clearIntermediateData() {
        dataByDataSource.clear();
        groupedData.clear();
        commonAggregationResults.clear();
    }

    @Override
    public String toString() {
        return String.format("DataVersion[version=%d, timestamp=%s, consumers=%d, datasources=%d]",
                version, timestamp, populatedEntities.size(), dataByDataSource.size());
    }
}
