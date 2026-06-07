package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.factory.target.*;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.*;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;

/**
 * Type-safe fluent builder for creating InMemoryDataStore instances.
 *
 * <p>
 * This builder provides a fluent API for configuring stores with:</p>
 * <ul>
 * <li>Primary datasource (provides root entities)</li>
 * <li>Optional root data filter (Specification)</li>
 * <li>Optional property mappings (populate fields from secondary
 * datasources)</li>
 * </ul>
 *
 * <p>
 * Example usage:</p>
 * <pre>{@code
 * InMemoryDataStore<User> store = factory
 *     .buildInMemoryStore(UserSpecificationService.INSTANCE)
 *     .withPrimaryDataSource(User.class)
 *     .withSpecification(User_.active.isTrue())
 *     .target(User_.totalSpent)
 *         .from(OrderSpecificationService.INSTANCE,
 *             pk -> pk.field(User_.id),
 *             fk -> fk.field(Order_.userId))
 *         .where((nav, spec) -> spec.on(nav.field(Order_.status)).eq("COMPLETED"))
 *         .sum(nav -> nav.field(Order_.amount))
 *     .build();
 * }</pre>
 *
 * @param <T> the entity type for the data store
 */
public class InMemoryStoreBuilder<T> extends AbstractRootBuilder<T> {
    
    private static final String TARGET_CANNOT_BE_NULL = "Target attribute cannot be null";

    private Class<?> primaryDataSourceClass;
    private Specification<T> rootSpecification;

    /**
     * Creates a new InMemoryStoreBuilder. Package-private constructor - use
     * InMemorySpecStoreFactory.buildInMemoryStore() instead.
     *
     * @param factory the factory instance
     * @param targetService the specification service for the target entity
     */
    InMemoryStoreBuilder(InMemorySpecStoreFactory factory, SpecificationService<T> targetService) {
        super(factory, targetService, "store-" + UUID.randomUUID(), false);
    }

    /**
     * Sets the primary datasource that provides root entities for this store.
     * The datasource must be registered with InMemorySpecStoreFactory.
     *
     * @param dataSourceClass the entity class type of the datasource
     * @return this builder for method chaining
     * @throws IllegalArgumentException if dataSourceClass is null
     */
    public InMemoryStoreBuilder<T> withPrimaryDataSource(Class<?> dataSourceClass) {
        if (dataSourceClass == null) {
            throw new IllegalArgumentException("DataSource class cannot be null");
        }
        this.primaryDataSourceClass = dataSourceClass;
        return this;
    }

    /**
     * Sets an optional filter specification for root data. Only entities
     * matching this specification will be included in the store.
     *
     * @param specification the filter specification
     * @return this builder for method chaining
     */
    public InMemoryStoreBuilder<T> withSpecification(Specification<T> specification) {
        this.rootSpecification = specification;
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
            new java.util.ArrayList<>()
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
            new java.util.ArrayList<>()
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
            new java.util.ArrayList<>()
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
            new java.util.ArrayList<>()
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
            new java.util.ArrayList<>()
        );
    }
    


    /**
     * Builds and returns the configured InMemoryDataStore. Registers the store
     * with the factory and DataSynchronizationEngine.
     *
     * @return the configured InMemoryDataStore
     * @throws IllegalStateException if primary datasource is not set or datasource not found
     */
    public InMemoryDataStore<T> build() {
        // Validate required fields
        if (primaryDataSourceClass == null) {
            throw new IllegalStateException("Primary datasource must be set");
        }

        // Resolve datasource name from class
        String primaryDataSourceName;
        try {
            primaryDataSourceName = getFactory().getDataSourceNameByClass(primaryDataSourceClass);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                "Failed to resolve datasource for class: " + primaryDataSourceClass.getName() + ". " + e.getMessage(),
                e
            );
        }

        // Create store instance with metadata
        InMemoryDataStore<T> store = new InMemoryDataStore<>(
                getEntityClass(),
                getConsumerId(),
                primaryDataSourceName,
                rootSpecification,
                getPropertyMappings()
        );

        // Register with factory
        getFactory().registerStore(store);

        return store;
    }
}
