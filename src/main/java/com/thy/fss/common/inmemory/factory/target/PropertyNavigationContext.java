package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.SourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Base context for navigating target properties. Provides common operations:
 * from() Does NOT provide field() - only ModelPropertyNavigationContext can
 * navigate to nested fields. NO parent() method - all terminal operations
 * return to root builder.
 *
 * <p>
 * This class maintains the navigation state and provides the bridge between
 * target field navigation and source mapping configuration. It is the base
 * class for all type-specific navigation contexts.</p>
 *
 * <h2>Design Principles</h2>
 * <ul>
 * <li><b>No field() in base class</b>: Only ModelPropertyNavigationContext can
 * navigate to nested fields</li>
 * <li><b>Terminal types</b>: String, Number, Boolean, Comparable contexts are
 * terminal (no field())</li>
 * <li><b>Always return to root</b>: All terminal operations return
 * AbstractRootBuilder</li>
 * <li><b>Type-specific contexts</b>: Subclasses provide type-specific
 * operations</li>
 * </ul>
 *
 * <h2>Context Hierarchy</h2>
 * <pre>
 * PropertyNavigationContext (base - provides from())
 *   ├─ ModelPropertyNavigationContext (navigable - HAS field())
 *   ├─ CollectionPropertyNavigationContext (navigable - HAS field(), first(), last())
 *   ├─ StringPropertyNavigationContext (terminal - NO field())
 *   ├─ NumericPropertyNavigationContext (terminal - NO field())
 *   ├─ ComparablePropertyNavigationContext (terminal - NO field())
 *   └─ BooleanPropertyNavigationContext (terminal - NO field())
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // Example 1: Terminal type (String) - NO field() method
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.name)  // Returns StringPropertyNavigationContext
 *     // .field(???)  // COMPILE ERROR: field() not available on terminal types
 *     .from(UserProfile.class,
 *         pk -&gt; pk.field(User_.id),
 *         fk -&gt; fk.field(UserProfile_.userId))
 *     .value(nav -&gt; nav.field(UserProfile_.displayName));
 *
 * // Example 2: Model type - HAS field() method
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.profile)  // Returns ModelPropertyNavigationContext
 *     .field(Profile_.settings)  // ✓ field() available - returns ModelPropertyNavigationContext
 *     .field(Settings_.theme)  // ✓ field() available - returns StringPropertyNavigationContext
 *     .from(Theme.class, pk -&gt; ..., fk -&gt; ...)
 *     .value(nav -&gt; nav.field(Theme_.name));
 * </pre>
 *
 * @param <R> the root entity type
 * @param <C> the current field type
 * @see ModelPropertyNavigationContext
 * @see CollectionPropertyNavigationContext
 * @see StringPropertyNavigationContext
 * @see NumericPropertyNavigationContext
 * @see ComparablePropertyNavigationContext
 * @see BooleanPropertyNavigationContext
 */
public class PropertyNavigationContext<R, C> {

    private final AbstractRootBuilder<R> rootBuilder;
    private final LinkedList<MetaAttribute<?, ?>> targetPath;
    private final Class<C> currentType;
    private final List<CollectionOperationMetadata<?, ?>> collectionOperations;

    /**
     * Package-private constructor for creating navigation context.
     *
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param currentType the current field type
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public PropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<C> currentType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        this.rootBuilder = Objects.requireNonNull(rootBuilder, "Root builder cannot be null");
        this.targetPath = new LinkedList<>(targetPath);
        this.currentType = Objects.requireNonNull(currentType, "Current type cannot be null");
        this.collectionOperations = new ArrayList<>(collectionOperations);
    }

    // NO field() method in base class!
    // Only ModelPropertyNavigationContext can navigate to nested fields.
    
    /**
     * Start source mapping definition using composite key builder.
     * This method supports both single-field and multi-field composite key mappings.
     *
     * <p>This is the primary method for defining relationships between target and source entities.
     * It uses {@link KeyPairBuilder} to define one or more primary key to foreign key field pairs
     * that establish the relationship.</p>
     *
     * Dashboard vs Store Behavior
     * <ul>
     * <li><b>Store:</b> Primary and foreign key paths are used to establish relationships between entities</li>
     * <li><b>Dashboard:</b> Primary and foreign key paths are IGNORED (Dashboard has no primary datasource)</li>
     * </ul>
     *
     * Single-Field Mapping Example
     * <p>A single-field mapping uses one {@code on()} call to define the relationship:</p>
     * <pre>
     * factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
     *     .target(User_.totalOrders)
     *     .from(OrderSpecificationService.INSTANCE, keys -&gt; keys
     *         .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     *     )
     *     .count();
     * </pre>
     *
     * Two-Field Composite Key Example
     * <p>A two-field composite key uses two {@code on()} calls. Both fields must match
     * for entities to be related:</p>
     * <pre>
     * factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
     *     .target(User_.regionalOrderCount)
     *     .from(OrderSpecificationService.INSTANCE, keys -&gt; keys
     *         .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     *         .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
     *     )
     *     .count();
     * </pre>
     *
     * Three-Field Composite Key with Nested Paths Example
     * <p>Complex composite keys can use nested field navigation:</p>
     * <pre>
     * factory.buildInMemoryStore(FlightSpecificationService.INSTANCE)
     *     .target(Flight_.totalPassengers)
     *     .from(FlightLegSpecificationService.INSTANCE, keys -&gt; keys
     *         .on(pk -&gt; pk.field(Flight_.flightNo), fk -&gt; fk.field(FlightLeg_.flightNo))
     *         .on(pk -&gt; pk.field(Flight_.date), fk -&gt; fk.field(FlightLeg_.date))
     *         .on(pk -&gt; pk.field(Flight_.legSequence), fk -&gt; fk.field(FlightLeg_.legSequence))
     *     )
     *     .sum(nav -&gt; nav.field(FlightLeg_.passengerCount));
     * </pre>
     *
     * Migration from Old API
     * <p>The old {@code from()} method with two separate function parameters is still supported
     * for backward compatibility. It internally delegates to this method with a single key pair.</p>
     *
     * <p><b>Old API:</b></p>
     * <pre>
     * .from(Order.class,
     *     pk -&gt; pk.field(User_.id),
     *     fk -&gt; fk.field(Order_.userId))
     * </pre>
     *
     * <p><b>New API (equivalent):</b></p>
     * <pre>
     * .from(OrderSpecificationService.INSTANCE, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     * )
     * </pre>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return SourceMappingContext for configuring the mapping
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if no key pairs are defined (for Store mappings)
     * @see KeyPairBuilder
     * @see SourceMappingContext
     */
    public <S> SourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            Function<KeyPairBuilder, KeyPairBuilder> keyPairBuilder) {

        Objects.requireNonNull(sourceService, "Source service cannot be null");
        Objects.requireNonNull(keyPairBuilder, "Key pair builder cannot be null");

        Class<S> sourceClass = sourceService.getEntityClass();

        // For Dashboard, primary/foreign keys are not used
        List<PropertyNavigation> primaryKeyPaths = null;
        List<PropertyNavigation> foreignKeyPaths = null;

        if (!rootBuilder.isForDashboard()) {
            // Create KeyPairBuilder instance with target and source classes
            KeyPairBuilder builder = new KeyPairBuilder(rootBuilder.getEntityClass(), sourceClass);

            // Apply the keyPairBuilder function to get configured builder
            KeyPairBuilder result = keyPairBuilder.apply(builder);

            // Extract primaryKeyPaths and foreignKeyPaths from builder
            primaryKeyPaths = result.getPrimaryKeyPaths();
            foreignKeyPaths = result.getForeignKeyPaths();

            // Validate that at least one key pair is defined
            if (primaryKeyPaths.isEmpty() || foreignKeyPaths.isEmpty()) {
                throw new IllegalArgumentException(
                    "At least one key pair must be defined using on() method");
            }
        }
        // else: Dashboard - primaryKeyPaths and foreignKeyPaths remain null

        // Call createSourceMappingContext() with the service
        return createSourceMappingContext(sourceService, primaryKeyPaths, foreignKeyPaths);
    }

    /**
     * Start source mapping definition. Creates PropertyNavigationBuilder
     * instances for primary key and foreign key path building.
     *
     * <p><b>This method is maintained for backward compatibility.</b>
     * It delegates to the new composite key implementation with a single key pair.</p>
     *
     * <p><b>Dashboard vs Store behavior:</b></p>
     * <ul>
     * <li><b>Store:</b> Primary and foreign key paths are used to establish relationships</li>
     * <li><b>Dashboard:</b> Primary and foreign key paths are IGNORED (Dashboard has no primary datasource)</li>
     * </ul>
     *
     * <p>
     * <b>Type Safety:</b> This method uses the current type information (stored
     * in currentType field) to create a type-safe SourceMappingContext. The
     * C generic parameter and currentType field work together to maintain
     * type safety throughout the mapping chain.</p>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity (ignored for Dashboard)
     * @param foreignKeyBuilder function to build foreign key path on source entity (ignored for Dashboard)
     * @return SourceMappingContext for configuring the mapping
     * @throws NullPointerException if any parameter is null
     */
    public <S> SourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {

        Objects.requireNonNull(sourceService, "Source service cannot be null");
        Objects.requireNonNull(primaryKeyBuilder, "Primary key builder cannot be null");
        Objects.requireNonNull(foreignKeyBuilder, "Foreign key builder cannot be null");

        // Delegate to new implementation with single pair
        return from(sourceService, keys -> keys.on(primaryKeyBuilder, foreignKeyBuilder));
    }

    /**
     * Factory method to create appropriate SourceMappingContext based on
     * current type. This method can be overridden by subclasses to return
     * type-specific source mapping contexts.
     *
     * <p>
     * The currentType field is maintained throughout the navigation chain to
     * enable:
     * <ul>
     * <li>Type-specific context creation in subclasses (e.g.,
     * NumericSourceMappingContext)</li>
     * <li>Proper generic type propagation through the builder chain</li>
     * <li>Future extensibility for type-based operations</li>
     * </ul>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths (null for Dashboard)
     * @param foreignKeyPaths the list of foreign key paths (null for Dashboard)
     * @return SourceMappingContext (or type-specific subclass)
     */
    protected <S> SourceMappingContext<R, C, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        // Base implementation returns generic SourceMappingContext
        // The currentType field is available for subclasses to use when creating
        // type-specific source mapping contexts
        return new SourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }

    // Package-private getters for SourceMappingContext
    /**
     * Gets the root builder instance.
     *
     * @return the root builder
     */
    public AbstractRootBuilder<R> getRootBuilder() {
        return rootBuilder;
    }

    /**
     * Gets a copy of the target path.
     *
     * @return copy of the target field path
     */
    public LinkedList<MetaAttribute<?, ?>> getTargetPath() {
        return new LinkedList<>(targetPath);
    }

    /**
     * Gets the current field type. This type information is used to maintain
     * type safety throughout the navigation chain. Each context stores the type
     * of the field it represents, enabling:
     * <ul>
     * <li>Runtime type validation when needed</li>
     * <li>Type-specific context creation in ModelPropertyNavigationContext</li>
     * <li>Proper generic type propagation through the builder chain</li>
     * </ul>
     *
     * @return the current field type class
     */
    public Class<C> getCurrentType() {
        return currentType;
    }

    /**
     * Gets a copy of the collection operations.
     *
     * @return copy of the collection operations metadata
     */
    public List<CollectionOperationMetadata<?, ?>> getCollectionOperations() {
        return new ArrayList<>(collectionOperations);
    }
}
