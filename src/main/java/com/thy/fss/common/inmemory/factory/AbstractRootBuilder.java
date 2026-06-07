package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.specification.SpecificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for root builders (InMemoryStoreBuilder and DashboardBuilder).
 * Maintains central state and provides common functionality for the builder chain.
 * 
 * <p>This class serves as the single source of truth for builder chain state,
 * eliminating the need for temporary anonymous builder instances. It is created once
 * at the start of the builder chain and referenced throughout all nested builders.</p>
 * 
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Maintain central state (consumerId, entityClass, factory reference)</li>
 *   <li>Store all property mappings defined in the builder chain</li>
 *   <li>Provide common functionality for InMemoryStoreBuilder and DashboardBuilder</li>
 *   <li>Serve as the return point for all terminal operations</li>
 * </ul>
 * 
 * <h2>Architecture Benefits</h2>
 * <ul>
 *   <li><b>Single Instance</b>: One root builder instance throughout the entire chain</li>
 *   <li><b>No Temporary Builders</b>: Eliminates anonymous BasePropertyMappingBuilder instances</li>
 *   <li><b>Unified State</b>: All nested builders access the same central state</li>
 *   <li><b>Type Safety</b>: Generic parameter T ensures type consistency</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // InMemoryStoreBuilder extends AbstractRootBuilder&lt;User&gt;
 * InMemoryDataStore&lt;User&gt; store = factory
 *     .buildInMemoryStore(UserSpecificationService.INSTANCE)  // Creates AbstractRootBuilder&lt;User&gt; instance
 *     .withPrimaryDataSource(User.class)
 *     .target(User_.totalSpent)
 *     .from(OrderSpecificationService.INSTANCE, pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *     .sum(nav -&gt; nav.field(Order_.amount))  // Returns to root builder
 *     .target(User_.orderCount)  // Still using same root builder instance
 *     .from(OrderSpecificationService.INSTANCE, pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *     .count()  // Returns to root builder
 *     .build();  // Builds store with all accumulated mappings
 * </pre>
 * 
 * @param <T> the entity type
 * @see InMemoryStoreBuilder
 * @see DashboardBuilder
 */
public abstract class AbstractRootBuilder<T> {
    
    protected final InMemorySpecStoreFactory factory;
    protected final SpecificationService<T> targetService;
    protected final String consumerId;
    protected final List<PropertyMapping<T, ?>> propertyMappings;
    protected final boolean isForDashboard;
    
    /**
     * Creates a new AbstractRootBuilder with the specified configuration.
     * 
     * @param factory the factory instance
     * @param targetService the specification service for the target entity
     * @param consumerId the unique consumer identifier (storeId or dashboardId)
     * @param isForDashboard whether this builder is for a dashboard (true) or store (false)
     * @throws NullPointerException if any parameter is null
     */
    protected AbstractRootBuilder(
            InMemorySpecStoreFactory factory,
            SpecificationService<T> targetService,
            String consumerId,
            boolean isForDashboard) {
        this.factory = Objects.requireNonNull(factory, "Factory cannot be null");
        this.targetService = Objects.requireNonNull(targetService, "Target service cannot be null");
        this.consumerId = Objects.requireNonNull(consumerId, "Consumer ID cannot be null");
        this.propertyMappings = new ArrayList<>();
        this.isForDashboard = isForDashboard;
    }
    
    /**
     * Adds a property mapping to this root builder.
     * Package-private - called by PropertyNavigationContext when completing a mapping.
     * 
     * @param <P> the property type
     * @param mapping the property mapping to add
     * @throws NullPointerException if mapping is null
     */
    public <P> void addPropertyMapping(PropertyMapping<T, P> mapping) {
        Objects.requireNonNull(mapping, "Property mapping cannot be null");
        propertyMappings.add(mapping);
    }
    
    /**
     * Returns the consumer ID (storeId or dashboardId).
     * 
     * @return the consumer ID
     */
    public String getConsumerId() {
        return consumerId;
    }
    
    /**
     * Returns the target specification service.
     * 
     * @return the target service
     */
    public SpecificationService<T> getTargetService() {
        return targetService;
    }
    
    /**
     * Returns the entity class type from the target service.
     * 
     * @return the entity class
     */
    public Class<T> getEntityClass() {
        return targetService.getEntityClass();
    }
    
    /**
     * Returns the factory instance.
     * 
     * @return the factory
     */
    public InMemorySpecStoreFactory getFactory() {
        return factory;
    }
    
    /**
     * Returns the list of property mappings.
     * Protected - accessible to subclasses for building stores/dashboards.
     * 
     * @return the property mappings list
     */
    protected List<PropertyMapping<T, ?>> getPropertyMappings() {
        return propertyMappings;
    }
    
    /**
     * Returns whether this builder is for a dashboard.
     * 
     * @return true if this is a dashboard builder, false if it's a store builder
     */
    public boolean isForDashboard() {
        return isForDashboard;
    }
}
