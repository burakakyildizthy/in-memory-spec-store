package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.target.*;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.*;

import java.util.*;

/**
 * Type-safe fluent builder for creating Dashboard instances.
 *
 * <p>Dashboard differs from Store in several ways:</p>
 * <ul>
 *   <li>NO primary datasource (dashboard doesn't have root entities)</li>
 *   <li>PropertyMapping is REQUIRED (dashboard fields must be populated from datasources)</li>
 *   <li>Only AGGREGATION mappings are allowed (no collection or single value)</li>
 *   <li>Dashboard contains a single model instance (not a list)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Dashboard<DashboardModel> dashboard = factory
 *     .buildDashboard(DashboardModelSpecificationService.INSTANCE)
 *     .target(DashboardModel_.totalOrders)
 *         .from(OrderSpecificationService.INSTANCE,
 *             pk -> pk.field(Order_.id),
 *             fk -> fk.field(Order_.id))
 *         .count()
 *     .target(DashboardModel_.totalRevenue)
 *         .from(OrderSpecificationService.INSTANCE,
 *             pk -> pk.field(Order_.id),
 *             fk -> fk.field(Order_.id))
 *         .where((nav, spec) -> spec.on(nav.field(Order_.status)).eq("COMPLETED"))
 *         .sum(nav -> nav.field(Order_.amount))
 *     .build();
 * }</pre>
 *
 * @param <T> the dashboard model type
 */
public class DashboardBuilder<T> extends AbstractRootBuilder<T> {
    private static final String TARGET_CANNOT_BE_NULL = "Target attribute cannot be null";

    private String dashboardName;

    /**
     * Creates a new DashboardBuilder.
     * Package-private constructor - use InMemorySpecStoreFactory.buildDashboard() instead.
     *
     * @param factory     the factory instance
     * @param targetService the specification service for the dashboard model
     */
    DashboardBuilder(InMemorySpecStoreFactory factory, SpecificationService<T> targetService) {
        super(factory, targetService, "dashboard-" + UUID.randomUUID(), true);
        this.dashboardName = targetService.getEntityClass().getSimpleName() + "-Dashboard";
    }

    /**
     * Sets an optional name for the dashboard.
     * If not set, defaults to "{ClassName}-Dashboard".
     *
     * @param name the dashboard name
     * @return this builder for method chaining
     */
    public DashboardBuilder<T> withName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.dashboardName = name;
        }
        return this;
    }

    /**
     * Starts defining a target property mapping for numeric field.
     * Returns NumericPropertyNavigationContext for type-safe numeric operations.
     *
     * @param <N> the numeric field type (extends Number)
     * @param targetAttribute the numeric target attribute
     * @return NumericPropertyNavigationContext for numeric operations
     * @throws IllegalArgumentException if targetAttribute is null
     */
    public <N extends Number> NumericPropertyNavigationContext<T, N> target(NumericMetaAttribute<T, N> targetAttribute) {
        Objects.requireNonNull(targetAttribute, TARGET_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> path = new LinkedList<>();
        path.add(targetAttribute);
        
        return new NumericPropertyNavigationContext<>(
            this,
            path,
            targetAttribute.getFieldType(),
            new ArrayList<>()
        );
    }
    
    /**
     * Starts defining a target property mapping for String field.
     * Returns StringPropertyNavigationContext for type-safe String operations.
     *
     * @param targetAttribute the String target attribute
     * @return StringPropertyNavigationContext for String operations
     * @throws IllegalArgumentException if targetAttribute is null
     */
    public StringPropertyNavigationContext<T> target(StringAttribute<T> targetAttribute) {
        Objects.requireNonNull(targetAttribute, TARGET_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> path = new LinkedList<>();
        path.add(targetAttribute);
        
        return new StringPropertyNavigationContext<>(
            this,
            path,
            new ArrayList<>()
        );
    }
    
    /**
     * Starts defining a target property mapping for Boolean field.
     * Returns BooleanPropertyNavigationContext for type-safe Boolean operations.
     *
     * @param targetAttribute the Boolean target attribute
     * @return BooleanPropertyNavigationContext for Boolean operations
     * @throws IllegalArgumentException if targetAttribute is null
     */
    public BooleanPropertyNavigationContext<T> target(BooleanAttribute<T> targetAttribute) {
        Objects.requireNonNull(targetAttribute, TARGET_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> path = new LinkedList<>();
        path.add(targetAttribute);
        
        return new BooleanPropertyNavigationContext<>(
            this,
            path,
            new ArrayList<>()
        );
    }
    
    /**
     * Starts defining a target property mapping for model field.
     * Returns ModelPropertyNavigationContext for continued navigation.
     *
     * @param <M> the model type
     * @param targetAttribute the model target attribute
     * @return ModelPropertyNavigationContext for continued navigation
     * @throws IllegalArgumentException if targetAttribute is null
     */
    public <M> ModelPropertyNavigationContext<T, M> target(ModelAttribute<T, M> targetAttribute) {
        Objects.requireNonNull(targetAttribute, TARGET_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> path = new LinkedList<>();
        path.add(targetAttribute);
        
        return new ModelPropertyNavigationContext<>(
            this,
            path,
            targetAttribute.getFieldType(),
            new ArrayList<>()
        );
    }
    
    /**
     * Starts defining a target property mapping for collection field.
     *
     * @param <E> the element type
     * @param targetAttribute the collection attribute
     * @return CollectionPropertyNavigationContext
     * @throws IllegalArgumentException if targetAttribute is null
     */
    public <E> CollectionPropertyNavigationContext<T, E> target(CollectionAttribute<T, E> targetAttribute) {
        Objects.requireNonNull(targetAttribute, TARGET_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> path = new LinkedList<>();
        path.add(targetAttribute);
        
        return new CollectionPropertyNavigationContext<>(
            this,
            path,
            targetAttribute.getElementType(),
            new ArrayList<>()
        );
    }
    


    /**
     * Builds and returns the configured Dashboard.
     * Registers the dashboard with the factory and DataSynchronizationEngine.
     *
     * @return the configured Dashboard
     * @throws IllegalStateException if no property mappings are defined
     */
    public Dashboard<T> build() {
        // Validate that at least one property mapping is defined
        if (getPropertyMappings().isEmpty()) {
            throw new IllegalStateException(
                    "Dashboard must have at least one property mapping. Use target() to add mappings."
            );
        }

        // Create dashboard instance
        Dashboard<T> dashboard = new Dashboard<>(
                getConsumerId(),
                this.dashboardName,
                getEntityClass()
        );

        // Update all property mappings to use the dashboard's actual ID
        // The Dashboard generates its own ID in the constructor, which is different from the builder's consumerId
        List<PropertyMapping<T, ?>> updatedMappings = new ArrayList<>();
        for (PropertyMapping<T, ?> mapping : getPropertyMappings()) {
            PropertyMapping<T, ?> updatedMapping = PropertyMapping.<T, Object>builder()
                    .consumerId(dashboard.getId())  // Use the dashboard's actual ID
                    .isForDashboard(true)  // Dashboard mappings are always for dashboard
                    .sourceService(mapping.getSourceService())
                    .targetService(mapping.getTargetService())
                    .targetPath(mapping.getTargetPath())
                    .datasourceName(mapping.getDataSourceName())
                    .sourcePath(mapping.getSourcePath())
                    .specification(mapping.getSpecification())
                    .mappingType(mapping.getMappingType())
                    .aggregationType(mapping.getAggregationType())
                    // Note: primaryKeyPath and foreignKeyPath are intentionally omitted (null for dashboard)
                    .build();
            updatedMappings.add(updatedMapping);
        }

        // Register with factory using the dashboard's actual ID and updated mappings
        getFactory().registerDashboard(dashboard, dashboard.getId(), updatedMappings);

        return dashboard;
    }
}
