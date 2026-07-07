package com.thy.fss.common.inmemory.factory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.exception.DataSourceNotFoundException;
import com.thy.fss.common.inmemory.engine.exception.DuplicateDataSourceException;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

/**
 * Singleton factory for managing datasources, stores, and dashboards. This is
 * the central point for all registration and build operations.
 *
 * <p>
 * All datasources must be registered through this factory before being used.
 * All stores and dashboards must be built through this factory to ensure proper
 * integration with the DataSynchronizationEngine.</p>
 *
 * <h2>Version 2.0 Changes</h2>
 * <p>
 * Factory methods now accept {@link SpecificationService} instances instead of
 * {@code Class<?>} objects. This eliminates runtime type lookups and provides
 * better type safety.
 * </p>
 *
 * Migration Example:
 * <pre>{@code
 * // Before (v1.x) - NO LONGER WORKS
 * InMemoryDataStore<User> store = factory.buildInMemoryStore(User.class)
 *     .withPrimaryDataSource("users")
 *     .build();
 *
 * // After (v2.0) - Required
 * InMemoryDataStore<User> store = factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
 *     .withPrimaryDataSource("users")
 *     .build();
 * }</pre>
 *
 * Usage Example:
 * <pre>{@code
 * InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
 *
 * // Register datasources
 * factory.registerDataSource("users", userDataSource, Duration.ofMinutes(5));
 * factory.registerDataSource("orders", orderDataSource, Duration.ofMinutes(10));
 *
 * // Build store
 * InMemoryDataStore<User> store = factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
 *     .withPrimaryDataSource("users")
 *     .build();
 *
 * // Build dashboard
 * Dashboard<DashboardModel> dashboard = factory.buildDashboard(DashboardModelSpecificationService.INSTANCE)
 *     .withProperty(DashboardModel_.totalOrders)
 *         .fromDataSource("orders")
 *         .onField(Order_.id)
 *         .count()
 *     .build();
 * }</pre>
 *
 * @since 2.0
 */
public class InMemorySpecStoreFactory {
    
    private static final String ENTITY_CANNOT_BE_NULL = "Entity class cannot be null";

    private static final Logger logger = LoggerFactory.getLogger(InMemorySpecStoreFactory.class);

    // Singleton instance
    private static final InMemorySpecStoreFactory INSTANCE = new InMemorySpecStoreFactory();

    // DataSource registry: name -> DataSource
    private final Map<String, DataSource<?>> dataSourceRegistry;

    // DataSource interval registry: name -> sync interval
    private final Map<String, Duration> dataSourceIntervalRegistry;

    // DataSource timeout registry: name -> read timeout
    private final Map<String, Duration> dataSourceTimeoutRegistry;

    // Store registry: storeId -> InMemoryDataStore
    private final Map<String, InMemoryDataStore<?>> storeRegistry;

    // Dashboard registry: list of all registered dashboards
    private final List<Dashboard<?>> dashboardRegistry;

    // Dashboard property mappings registry: dashboardId -> List<PropertyMapping>
    private final Map<String, List<PropertyMapping<?, ?>>> dashboardPropertyMappingsRegistry;

    // Streaming datasource TimeWindowRule registry: name -> TimeWindowRule (nullable values)
    private final Map<String, TimeWindowRule<?>> timeWindowRuleRegistry;

    // DataSynchronizationEngine reference (will be set later)

    /**
     * Private constructor for singleton pattern.
     */
    private InMemorySpecStoreFactory() {
        this.dataSourceRegistry = new ConcurrentHashMap<>();
        this.dataSourceIntervalRegistry = new ConcurrentHashMap<>();
        this.dataSourceTimeoutRegistry = new ConcurrentHashMap<>();
        this.storeRegistry = new ConcurrentHashMap<>();
        this.dashboardRegistry = Collections.synchronizedList(new ArrayList<>());
        this.dashboardPropertyMappingsRegistry = new ConcurrentHashMap<>();
        this.timeWindowRuleRegistry = new ConcurrentHashMap<>();

        logger.info("InMemorySpecStoreFactory initialized");
    }

    /**
     * Returns the singleton instance of InMemorySpecStoreFactory.
     *
     * @return the singleton instance
     */
    public static InMemorySpecStoreFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a datasource using the entity class type to derive the datasource name.
     * Uses default read timeout (30 seconds).
     * The datasource name is automatically generated from the class name.
     *
     * @param entityClass  the entity class type
     * @param dataSource   the datasource to register
     * @param syncInterval the synchronization interval for this datasource
     * @param <T>          the entity type of the datasource
     * @throws IllegalArgumentException     if entityClass or dataSource is null
     * @throws DuplicateDataSourceException if a datasource with the derived name already exists
     */
    public <T> void registerDataSource(Class<T> entityClass, DataSource<T> dataSource, Duration syncInterval) {
        registerDataSource(entityClass, dataSource, syncInterval, null);
    }

    /**
     * Registers a datasource using the entity class type to derive the datasource name.
     * Allows specifying a custom read timeout.
     * The datasource name is automatically generated from the class name.
     *
     * @param entityClass  the entity class type
     * @param dataSource   the datasource to register
     * @param syncInterval the synchronization interval for this datasource
     * @param readTimeout  the timeout for datasource read operations (null for default 30 seconds)
     * @param <T>          the entity type of the datasource
     * @throws IllegalArgumentException     if entityClass or dataSource is null
     * @throws DuplicateDataSourceException if a datasource with the derived name already exists
     */
    public <T> void registerDataSource(Class<T> entityClass, DataSource<T> dataSource, Duration syncInterval, Duration readTimeout) {
        if (entityClass == null) {
            throw new IllegalArgumentException(ENTITY_CANNOT_BE_NULL);
        }
        
        String datasourceName = resolveDataSourceName(entityClass);
        registerDataSource(datasourceName, dataSource, syncInterval, readTimeout);
    }

    /**
     * Registers a datasource with the specified name and sync interval. Uses
     * default read timeout (30 seconds). Datasource names must be unique.
     *
     * @param name         the unique name for the datasource
     * @param dataSource   the datasource to register
     * @param syncInterval the synchronization interval for this datasource
     * @param <T>          the entity type of the datasource
     * @throws IllegalArgumentException     if name or dataSource is null
     * @throws DuplicateDataSourceException if a datasource with this name
     *                                      already exists
     */
    public <T> void registerDataSource(String name, DataSource<T> dataSource, Duration syncInterval) {
        registerDataSource(name, dataSource, syncInterval, null);
    }

    /**
     * Registers a datasource with the specified name, sync interval, and read
     * timeout. Datasource names must be unique.
     *
     * @param name         the unique name for the datasource
     * @param dataSource   the datasource to register
     * @param syncInterval the synchronization interval for this datasource
     * @param readTimeout  the timeout for datasource read operations (null for
     *                     default 30 seconds)
     * @param <T>          the entity type of the datasource
     * @throws IllegalArgumentException     if name or dataSource is null
     * @throws DuplicateDataSourceException if a datasource with this name
     *                                      already exists
     */
    public <T> void registerDataSource(String name, DataSource<T> dataSource, Duration syncInterval, Duration readTimeout) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Datasource name cannot be null or empty");
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("Datasource cannot be null");
        }

        // Check for duplicate
        if (dataSourceRegistry.containsKey(name)) {
            throw new DuplicateDataSourceException(name);
        }

        if (dataSource instanceof StreamingDataSource) {
            // Streaming datasource: syncInterval is NOT required (null/ZERO accepted)
            dataSourceRegistry.put(name, dataSource);
            // Streaming datasource'lar için de syncInterval kaydedilmeli (eğer belirtilmişse)
            if (syncInterval != null && !syncInterval.isZero()) {
                dataSourceIntervalRegistry.put(name, syncInterval);
            }
            if (readTimeout != null) {
                dataSourceTimeoutRegistry.put(name, readTimeout);
            }
            logger.info("Registered streaming datasource '{}' with interval {}", name, syncInterval);
        } else {
            // Batch datasource: syncInterval is required
            if (syncInterval == null) {
                throw new IllegalArgumentException("Sync interval cannot be null");
            }
            dataSourceRegistry.put(name, dataSource);
            dataSourceIntervalRegistry.put(name, syncInterval);
            if (readTimeout != null) {
                dataSourceTimeoutRegistry.put(name, readTimeout);
            }
            logger.info("Registered datasource '{}' with interval {} and timeout {}",
                    name, syncInterval, readTimeout != null ? readTimeout : "default");
        }
    }

    /**
     * Registers a streaming datasource without a syncInterval.
     * This overload is only valid for StreamingDataSource instances — batch datasources
     * require a syncInterval and must use the other registerDataSource overloads.
     *
     * @param name       the unique name for the datasource
     * @param dataSource the streaming datasource to register
     * @param <T>        the entity type of the datasource
     * @throws IllegalArgumentException     if the datasource is not a StreamingDataSource
     * @throws DuplicateDataSourceException if a datasource with this name already exists
     */
    public <T> void registerDataSource(String name, DataSource<T> dataSource) {
        if (!(dataSource instanceof StreamingDataSource)) {
            throw new IllegalArgumentException(
                    "syncInterval is required for non-streaming datasources");
        }
        registerDataSource(name, dataSource, Duration.ZERO, null);
    }

    /**
     * Gets a registered datasource by name.
     *
     * @param name the datasource name
     * @return the datasource
     * @throws DataSourceNotFoundException if no datasource with this name
     *                                     exists
     */
    public DataSource<?> getDataSource(String name) {
        DataSource<?> dataSource = dataSourceRegistry.get(name);
        if (dataSource == null) {
            throw new DataSourceNotFoundException(name);
        }
        return dataSource;
    }

    /**
     * Gets the sync interval for a registered datasource.
     *
     * @param name the datasource name
     * @return the sync interval
     * @throws DataSourceNotFoundException if no datasource with this name
     *                                     exists
     */
    public Duration getDataSourceInterval(String name) {
        Duration interval = dataSourceIntervalRegistry.get(name);
        if (interval == null) {
            // Streaming datasources don't have a sync interval — return ZERO
            if (isStreamingDataSource(name)) {
                return Duration.ZERO;
            }
            throw new DataSourceNotFoundException(name,
                    "No interval found for datasource: " + name);
        }
        return interval;
    }

    /**
     * Gets the read timeout for a registered datasource.
     *
     * @param name the datasource name
     * @return the read timeout, or null if not set (will use default)
     * @throws DataSourceNotFoundException if no datasource with this name
     *                                     exists
     */
    public Duration getDataSourceTimeout(String name) {
        if (!dataSourceRegistry.containsKey(name)) {
            throw new DataSourceNotFoundException(name);
        }
        return dataSourceTimeoutRegistry.get(name); // May be null, which means use default
    }

    /**
     * Checks if a datasource with the given name is registered.
     *
     * @param name the datasource name
     * @return true if registered, false otherwise
     */
    public boolean hasDataSource(String name) {
        return dataSourceRegistry.containsKey(name);
    }

    /**
     * Returns all registered datasource names.
     *
     * @return unmodifiable list of datasource names
     */
    public List<String> getAllDataSourceNames() {
        return Collections.unmodifiableList(new ArrayList<>(dataSourceRegistry.keySet()));
    }

    /**
     * Gets the datasource name for a given entity class type.
     * Searches through all registered datasources and finds the one matching the entity type.
     *
     * @param entityClass the entity class type
     * @return the datasource name
     * @throws IllegalStateException if no datasource found or multiple datasources match
     * @throws IllegalArgumentException if entityClass is null
     */
    public String getDataSourceNameByClass(Class<?> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException(ENTITY_CANNOT_BE_NULL);
        }
        
        logger.debug("Looking for datasource with entity type: {}", entityClass.getSimpleName());
        logger.debug("Registered datasources: {}", getAllDataSourceNames());
        
        // Find all datasources that match the entity class type
        List<String> matchingDataSources = new ArrayList<>();
        List<String> matchingBatchDataSources = new ArrayList<>();
        for (String dataSourceName : getAllDataSourceNames()) {
            DataSource<?> dataSource = getDataSource(dataSourceName);
            logger.debug("Checking datasource '{}' with entity type: {}", 
                    dataSourceName, dataSource.getEntityType().getSimpleName());
            if (dataSource.getEntityType().equals(entityClass)) {
                matchingDataSources.add(dataSourceName);
                if (!(dataSource instanceof StreamingDataSource)) {
                    matchingBatchDataSources.add(dataSourceName);
                }
                logger.debug("MATCH FOUND: datasource '{}' matches entity type {}", 
                        dataSourceName, entityClass.getSimpleName());
            }
        }
        
        // Validate: exactly one datasource must match
        if (matchingDataSources.isEmpty()) {
            logger.error("NO DATASOURCE FOUND for entity type: {}", entityClass.getSimpleName());
            logger.error("Available datasources and their types:");
            for (String dsName : getAllDataSourceNames()) {
                DataSource<?> ds = getDataSource(dsName);
                logger.error("  - '{}' -> {}", dsName, ds.getEntityType().getSimpleName());
            }
            throw new IllegalStateException(
                String.format("No datasource found for class type: %s. Please register a datasource with this entity type.",
                    entityClass.getName())
            );
        }
        
        // If multiple matches but exactly one batch datasource, prefer the batch one
        // (streaming datasources are event-driven and not used as store primary datasources)
        if (matchingDataSources.size() > 1 && matchingBatchDataSources.size() == 1) {
            String foundName = matchingBatchDataSources.get(0);
            logger.debug("Multiple datasources match entity type {}, preferring batch datasource '{}'",
                    entityClass.getSimpleName(), foundName);
            return foundName;
        }
        
        if (matchingDataSources.size() > 1) {
            throw new IllegalStateException(
                String.format("Multiple datasources found for class type: %s. Matching datasources: %s. "
                    + "Each class type must have exactly one datasource.",
                    entityClass.getName(), matchingDataSources)
            );
        }
        
        String foundName = matchingDataSources.get(0);
        logger.debug("Resolved datasource name '{}' for entity type {}", foundName, entityClass.getSimpleName());
        return foundName;
    }

    /**
     * Resolves a datasource name from an entity class type.
     * Generates a consistent naming convention: ClassName -> classNameDataSource
     * For example: User -> userDataSource, Order -> orderDataSource
     *
     * @param entityClass the entity class type
     * @return the resolved datasource name
     */
    private String resolveDataSourceName(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        // Convert first character to lowercase and append "DataSource"
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1) + "DataSource";
    }

    /**
     * Unregisters a datasource by name. This removes the datasource from all
     * registries.
     *
     * @param name the datasource name to unregister
     */
    public void unregisterDataSource(String name) {
        if (name == null) {
            return;
        }

        if (dataSourceRegistry.containsKey(name)) {
            boolean isStreaming = dataSourceRegistry.get(name) instanceof StreamingDataSource;
            dataSourceRegistry.remove(name);
            dataSourceIntervalRegistry.remove(name);
            dataSourceTimeoutRegistry.remove(name);
            timeWindowRuleRegistry.remove(name);
            logger.info("Unregistered {} datasource '{}'", isStreaming ? "streaming" : "batch", name);
        }
    }

    /**
     * Clears all registered datasources. This is primarily useful for testing
     * scenarios where you need to reset the factory state between tests.
     */
    public void clearAllDataSources() {
        dataSourceRegistry.clear();
        dataSourceIntervalRegistry.clear();
        dataSourceTimeoutRegistry.clear();
        timeWindowRuleRegistry.clear();

        logger.info("Cleared all datasources");
    }

    // ==================== Streaming DataSource Query Methods ====================

    /**
     * Checks if the datasource with the given name is a streaming datasource.
     * Uses instanceof check on the unified dataSourceRegistry.
     *
     * @param name the datasource name
     * @return true if the datasource is a streaming datasource, false otherwise
     */
    public boolean isStreamingDataSource(String name) {
        DataSource<?> ds = dataSourceRegistry.get(name);
        return ds instanceof StreamingDataSource;
    }

    /**
     * Gets a registered streaming datasource by name.
     *
     * @param name the streaming datasource name
     * @return the streaming datasource, or null if not found or not a streaming datasource
     */
    public StreamingDataSource<?> getStreamingDataSource(String name) {
        DataSource<?> ds = dataSourceRegistry.get(name);
        return ds instanceof StreamingDataSource ? (StreamingDataSource<?>) ds : null;
    }

    /**
     * Gets the TimeWindowRule associated with a datasource.
     *
     * @param name the datasource name
     * @return the TimeWindowRule, or null if no rule is associated
     */
    public TimeWindowRule<?> getTimeWindowRule(String name) {
        return timeWindowRuleRegistry.get(name);
    }

    /**
     * Returns all registered streaming datasource names.
     *
     * @return unmodifiable set of streaming datasource names
     */
    public Set<String> getAllStreamingDataSourceNames() {
        return dataSourceRegistry.entrySet().stream()
                .filter(e -> e.getValue() instanceof StreamingDataSource)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks if a streaming datasource is in READY state (healthy and ready to serve data).
     * Only READY datasources are considered healthy; INITIALIZING and ERROR datasources
     * are reported as "not ready".
     *
     * <p>Returns false for batch datasources or unknown datasource names.</p>
     *
     * @param name the datasource name
     * @return true if the datasource is a streaming datasource in READY state
     */
    public boolean isStreamingDataSourceReady(String name) {
        DataSource<?> ds = dataSourceRegistry.get(name);
        if (ds instanceof StreamingDataSource) {
            return ((StreamingDataSource<?>) ds).getState() == StreamingDataSourceState.READY;
        }
        return false;
    }

    /**
     * Registers a TimeWindowRule for a datasource.
     * The datasource must already be registered.
     *
     * @param dataSourceName the name of the datasource to associate the rule with
     * @param timeWindowRule the time window rule for entity expiration filtering
     * @param <T>            the entity type implementing Identifiable
     */
    public <T extends Identifiable<?>> void registerTimeWindowRule(
            String dataSourceName, TimeWindowRule<T> timeWindowRule) {
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Datasource name cannot be null or empty");
        }
        if (timeWindowRule == null) {
            throw new IllegalArgumentException("TimeWindowRule cannot be null");
        }
        timeWindowRuleRegistry.put(dataSourceName, timeWindowRule);
        logger.info("Registered TimeWindowRule for datasource '{}'", dataSourceName);
    }

    /**
     * Registers a store with the factory. This method is called internally by
     * the builder after a store is built.
     *
     * @param store the store to register
     * @param <T>   the entity type of the store
     */
    <T> void registerStore(InMemoryDataStore<T> store) {
        if (store == null) {
            throw new IllegalArgumentException("Store cannot be null");
        }

        String storeId = store.getStoreId();
        if (storeId == null) {
            throw new IllegalArgumentException("Store ID cannot be null");
        }

        storeRegistry.put(storeId, store);

        logger.info("Registered store '{}' for entity type: {} with primary datasource: {}",
                storeId, store.getTargetClass().getSimpleName(), store.getPrimaryDataSourceName());
    }

    /**
     * Registers a dashboard with the factory. This method is called internally
     * by the builder after a dashboard is built.
     *
     * @param dashboard        the dashboard to register
     * @param dashboardId      the unique dashboard ID
     * @param propertyMappings list of property mappings for this dashboard
     * @param <T>              the target type of the dashboard
     */
    <T> void registerDashboard(
            Dashboard<T> dashboard,
            String dashboardId,
            List<PropertyMapping<T, ?>> propertyMappings) {
        if (dashboard == null) {
            throw new IllegalArgumentException("Dashboard cannot be null");
        }

        dashboardRegistry.add(dashboard);

        // Store property mappings for this dashboard (cast to wildcard type for storage)
        @SuppressWarnings("unchecked")
        List<PropertyMapping<?, ?>> wildcardMappings = (List<PropertyMapping<?, ?>>) (List<?>) propertyMappings;
        dashboardPropertyMappingsRegistry.put(dashboardId, new ArrayList<>(wildcardMappings));

        logger.info("Registered dashboard: {} (ID: {}) with {} property mapping(s)",
                dashboard.getName(), dashboardId, propertyMappings.size());

    }

    /**
     * Returns all registered store IDs.
     *
     * @return list of store IDs
     */
    public List<String> getAllStoreIds() {
        return Collections.unmodifiableList(new ArrayList<>(storeRegistry.keySet()));
    }

    /**
     * Returns all registered dashboard IDs.
     *
     * @return list of dashboard IDs
     */
    public List<String> getAllDashboardIds() {
        List<String> dashboardIds = new ArrayList<>();
        for (Dashboard<?> dashboard : dashboardRegistry) {
            dashboardIds.add(dashboard.getId());
        }
        return Collections.unmodifiableList(dashboardIds);
    }

    /**
     * Returns all consumer IDs (stores + dashboards).
     *
     * @return list of all consumer IDs
     */
    public List<String> getAllConsumerIds() {
        List<String> consumerIds = new ArrayList<>();
        consumerIds.addAll(getAllStoreIds());
        consumerIds.addAll(getAllDashboardIds());
        return Collections.unmodifiableList(consumerIds);
    }

    /**
     * Gets a store by its ID.
     *
     * @param storeId the store ID
     * @return the store, or null if not found
     */
    public InMemoryDataStore<?> getStoreById(String storeId) {
        return storeRegistry.get(storeId);
    }

    /**
     * Gets a dashboard by its ID.
     *
     * @param dashboardId the dashboard ID
     * @return the dashboard, or null if not found
     */
    public Dashboard<?> getDashboardById(String dashboardId) {
        for (Dashboard<?> dashboard : dashboardRegistry) {
            if (dashboard.getId().equals(dashboardId)) {
                return dashboard;
            }
        }
        return null;
    }

    /**
     * Extracts all property mappings from all registered stores and dashboards.
     *
     * @return list of all property mappings
     */
    public List<PropertyMapping<?, ?>> getAllPropertyMappings() {
        List<PropertyMapping<?, ?>> allMappings = new ArrayList<>();

        // DEBUG: Log registry sizes
        logger.debug("getAllPropertyMappings called - storeRegistry size: {}, dashboardRegistry size: {}", 
                storeRegistry.size(), dashboardRegistry.size());

        // Extract from stores
        for (InMemoryDataStore<?> store : storeRegistry.values()) {
            List<PropertyMapping<?, ?>> storeMappings = extractPropertyMappingsFromStore(store);
            logger.debug("Store {} has {} PropertyMapping(s)", store.getStoreId(), storeMappings.size());
            allMappings.addAll(storeMappings);
        }

        // Extract from dashboards
        for (Dashboard<?> dashboard : dashboardRegistry) {
            List<PropertyMapping<?, ?>> dashboardMappings = extractPropertyMappingsFromDashboard(dashboard);
            logger.debug("Dashboard {} has {} PropertyMapping(s)", dashboard.getId(), dashboardMappings.size());
            allMappings.addAll(dashboardMappings);
        }

        logger.debug("getAllPropertyMappings returning {} total PropertyMapping(s)", allMappings.size());
        return allMappings;
    }

    /**
     * Extracts property mappings from a store.
     *
     * @param store the store
     * @return list of property mappings from this store
     */
    private List<PropertyMapping<?, ?>> extractPropertyMappingsFromStore(InMemoryDataStore<?> store) {
        List<PropertyMapping<?, ?>> mappings = (List<PropertyMapping<?, ?>>) (List<?>) store.getPropertyMappings();
        return mappings != null ? new ArrayList<>(mappings) : Collections.emptyList();
    }

    /**
     * Extracts property mappings from a dashboard.
     *
     * @param dashboard the dashboard
     * @return list of property mappings from this dashboard
     */
    private List<PropertyMapping<?, ?>> extractPropertyMappingsFromDashboard(Dashboard<?> dashboard) {
        String dashboardId = dashboard.getId();
        List<PropertyMapping<?, ?>> mappings = dashboardPropertyMappingsRegistry.get(dashboardId);
        return mappings != null ? new ArrayList<>(mappings) : Collections.emptyList();
    }

    /**
     * Builds a new InMemoryDataStore with the specified specification service.
     *
     * <p>
     * This method creates a builder for configuring an in-memory data store.
     * The specification service provides type-safe access to entity fields
     * and eliminates the need for runtime type lookups.
     * </p>
     *
     * Example Usage:
     * <pre>{@code
     * InMemoryDataStore<User> store = factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
     *     .withPrimaryDataSource("users")
     *     .target(User_.totalOrders)
     *     .from(OrderSpecificationService.INSTANCE, keys -> keys
     *         .on(pk -> pk.field(User_.id), fk -> fk.field(Order_.userId))
     *     )
     *     .count()
     *     .build();
     * }</pre>
     *
     * @param service the specification service for the store
     * @param <T>     the entity type
     * @return a builder for configuring the store
     * @since 2.0
     */
    public <T> InMemoryStoreBuilder<T> buildInMemoryStore(SpecificationService<T> service) {
        return new InMemoryStoreBuilder<>(this, service);
    }

    /**
     * Builds a new Dashboard with the specified specification service.
     *
     * <p>
     * This method creates a builder for configuring a dashboard.
     * The specification service provides type-safe access to entity fields
     * and eliminates the need for runtime type lookups.
     * </p>
     *
     * Example Usage:
     * <pre>{@code
     * Dashboard<UserSummary> dashboard = factory.buildDashboard(UserSummarySpecificationService.INSTANCE)
     *     .withName("User Statistics")
     *     .target(UserSummary_.totalUsers)
     *     .from(UserSpecificationService.INSTANCE, keys -> keys
     *         .on(pk -> pk.field(UserSummary_.id), fk -> fk.field(User_.id))
     *     )
     *     .count()
     *     .build();
     * }</pre>
     *
     * @param service the specification service for the dashboard
     * @param <T>     the target type
     * @return a builder for configuring the dashboard
     * @since 2.0
     */
    public <T> DashboardBuilder<T> buildDashboard(SpecificationService<T> service) {
        return new DashboardBuilder<>(this, service);
    }

    /**
     * Clears all registered datasources, stores, and dashboards. This method is
     * primarily intended for testing purposes.
     *
     * <p>
     * <strong>WARNING:</strong> This will clear all registrations and should
     * only be used in test cleanup or when reinitializing the factory.</p>
     */
    public void clearAll() {
        dataSourceRegistry.clear();
        dataSourceIntervalRegistry.clear();
        dataSourceTimeoutRegistry.clear();
        storeRegistry.clear();
        dashboardRegistry.clear();
        dashboardPropertyMappingsRegistry.clear();
        timeWindowRuleRegistry.clear();

        logger.info("Cleared all factory registrations");
    }
}
