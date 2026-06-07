package com.thy.fss.common.inmemory.store;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.filter.EntityFilter;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic in-memory data store
 * <p>
 * This class provides a thread-safe in-memory data store for entities of type
 * T. It supports full data replacement, querying with specifications and
 * filters, and pagination. The store maintains metadata such as store ID,
 * primary datasource name, root specification, property mappings, and
 * versioning.
 *
 * @param <T> The type of objects stored in this data store
 */
public class InMemoryDataStore<T> {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryDataStore.class);

    private final Class<T> targetClass;
    // Using AtomicReference for thread-safe lock-free reads
    // Write operations are infrequent (only during sync), reads are frequent
    private final AtomicReference<List<T>> currentData = new AtomicReference<>(Collections.emptyList());
    private final SpecificationQueryEngine<T> queryEngine;

    // Store metadata fields
    private final String storeId;
    private final String primaryDataSourceName;
    private final Specification<T> rootSpecification;
    private final List<PropertyMapping<T, ?>> propertyMappings;

    // Status tracking fields - version is managed externally by DataSynchronizationEngine
    private volatile long currentVersion;

    /**
     * Creates a new InMemoryDataStore with full metadata.
     *
     * @param targetClass the target class type
     * @param storeId the unique store ID
     * @param primaryDataSourceName the primary datasource name
     * @param rootSpecification optional root data filter
     * @param propertyMappings list of property mappings for this store
     * @throws IllegalArgumentException if targetClass is null
     */
    public InMemoryDataStore(Class<T> targetClass, String storeId, String primaryDataSourceName,
            Specification<T> rootSpecification, List<PropertyMapping<T, ?>> propertyMappings) {
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }

        logger.info("Initializing InMemoryDataStore for target class: {}", targetClass.getSimpleName());

        this.targetClass = targetClass;
        this.storeId = storeId;
        this.primaryDataSourceName = primaryDataSourceName;
        this.rootSpecification = rootSpecification;
        this.propertyMappings = propertyMappings != null ? propertyMappings : Collections.emptyList();
        this.currentData.set(Collections.emptyList());
        this.queryEngine = new SpecificationQueryEngine<>(targetClass);
        this.currentVersion = 0L;
    }

    /**
     * Updates the store data with version information. This method will be
     * called by DataSynchronizationEngine. Version is provided by the caller
     * and stored as-is without modification.
     *
     * @param newData the new data to set (should be immutable or not modified
     * after passing)
     * @param version the version number from DataSynchronizationEngine
     */
    public void updateData(List<T> newData, long version) {
        if (newData == null) {
            newData = Collections.emptyList();
        }

        // AtomicReference set ensures visibility to all threads
        this.currentData.set(newData);
        this.currentVersion = version;

        logger.debug("Updated data for {}: {} items, version: {}",
                targetClass.getSimpleName(), newData.size(), version);
    }

    /**
     * Updates the store data without version information (uses current
     * version). This is a convenience method for backward compatibility.
     *
     * @param newData the new data to set
     */
    public void updateData(List<T> newData) {
        updateData(newData, this.currentVersion);
    }

    /**
     * Returns all data in the store. Lock-free read with minimal memory barrier
     * overhead.
     *
     * @return list of all entities (immutable reference)
     */
    public List<T> findAll() {
        return currentData.get();
    }

    /**
     * Returns filtered data based on specification.
     *
     * @param specification the filter specification
     * @return list of matching entities
     */
    public List<T> findAll(Specification<T> specification) {
        if (specification == null) {
            return findAll();
        }
        List<T> data = currentData.get();
        return queryEngine.query(data, specification);
    }

    /**
     * Returns paginated data.
     *
     * @param pageable pagination parameters
     * @return page of entities
     */
    public Page<T> findAll(Pageable pageable) {
        List<T> data = currentData.get();
        return queryEngine.query(data, null, pageable);
    }

    /**
     * Returns paginated filtered data.
     *
     * @param specification the filter specification
     * @param pageable pagination parameters
     * @return page of matching entities
     */
    public Page<T> findAll(Specification<T> specification, Pageable pageable) {
        List<T> data = currentData.get();
        return queryEngine.query(data, specification, pageable);
    }

    /**
     * Returns filtered data based on entity filter.
     *
     * @param filter the entity filter
     * @return list of matching entities
     */
    public List<T> findAll(EntityFilter<T> filter) {
        if (filter == null) {
            return findAll();
        }
        List<T> data = currentData.get();
        return queryEngine.queryByFilter(data, filter);
    }

    /**
     * Returns paginated filtered data based on entity filter.
     *
     * @param filter the entity filter
     * @param pageable pagination parameters
     * @return page of matching entities
     */
    public Page<T> findAll(EntityFilter<T> filter, Pageable pageable) {
        if (filter == null) {
            return findAll(pageable);
        }
        List<T> data = currentData.get();
        return queryEngine.queryByFilter(data, filter, pageable);
    }

    /**
     * Returns the current data version. Version is managed by
     * DataSynchronizationEngine and passed via updateData().
     *
     * @return current version number
     */
    public long getVersion() {
        return currentVersion;
    }

    /**
     * Returns the number of entities in the store.
     *
     * @return entity count
     */
    public int size() {
        return currentData.get().size();
    }

    /**
     * Returns the target class.
     *
     * @return target class
     */
    public Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the store ID.
     *
     * @return store ID
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Returns the primary datasource name.
     *
     * @return primary datasource name
     */
    public String getPrimaryDataSourceName() {
        return primaryDataSourceName;
    }

    /**
     * Returns the root specification.
     *
     * @return root specification
     */
    public Specification<T> getRootSpecification() {
        return rootSpecification;
    }

    /**
     * Returns the property mappings.
     *
     * @return unmodifiable list of property mappings
     */
    public List<PropertyMapping<T, ?>> getPropertyMappings() {
        return Collections.unmodifiableList(propertyMappings);
    }

}
