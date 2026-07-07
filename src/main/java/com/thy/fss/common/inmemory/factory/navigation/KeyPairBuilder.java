package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builder for defining multiple primary key to foreign key field pairs in a fluent way.
 * Each {@link #on(Function, Function)} call adds one PK-FK pair to the composite key.
 * 
 * <p>This class enables composite key mapping by allowing developers to specify
 * multiple field pairs that together form the relationship between target and source entities.
 * Composite keys are essential when entities are related through multiple fields rather than
 * a single identifier.</p>
 * 
 * <h2>Overview</h2>
 * <p>The KeyPairBuilder provides a fluent API for defining composite key relationships.
 * Each call to {@link #on(Function, Function)} adds one field pair to the composite key.
 * The order of calls matters - fields will be matched in the order they are defined.</p>
 * 
 * <h2>Key Concepts</h2>
 * <ul>
 * <li><b>Primary Key Path</b>: Navigation path to fields in the target entity used for matching</li>
 * <li><b>Foreign Key Path</b>: Navigation path to fields in the source entity used for matching</li>
 * <li><b>Composite Key</b>: Multiple field pairs that together uniquely identify a relationship</li>
 * <li><b>Single-Field Key</b>: A composite key with only one field pair (backward compatible)</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * Example 1: Single-Field Mapping
 * <p>A single-field mapping is treated as a composite key with one pair.
 * This maintains backward compatibility with existing code.</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.totalOrders)
 *     .from(Order.class, keys -&gt; keys
 *         .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *     )
 *     .count();
 * </pre>
 * 
 * Example 2: Two-Field Composite Key
 * <p>A two-field composite key where both userId and regionId must match
 * for entities to be related.</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.regionalOrderCount)
 *     .from(Order.class, keys -&gt; keys
 *         .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *         .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
 *     )
 *     .count();
 * </pre>
 * 
 * Example 3: Three-Field Composite Key with Nested Paths
 * <p>A complex composite key using nested field navigation. This example shows
 * how to match flight legs to flights using flight number, date, and leg sequence.</p>
 * <pre>
 * factory.buildInMemoryStore(Flight.class)
 *     .target(Flight_.totalPassengers)
 *     .from(FlightLeg.class, keys -&gt; keys
 *         .on(pk -&gt; pk.field(Flight_.flightNo), fk -&gt; fk.field(FlightLeg_.flightNo))
 *         .on(pk -&gt; pk.field(Flight_.date), fk -&gt; fk.field(FlightLeg_.date))
 *         .on(pk -&gt; pk.field(Flight_.legSequence), fk -&gt; fk.field(FlightLeg_.legSequence))
 *     )
 *     .sum(nav -&gt; nav.field(FlightLeg_.passengerCount));
 * </pre>
 * 
 * Example 4: Composite Key with Nested Entity Navigation
 * <p>This example shows how to use nested paths in composite keys, where key fields
 * are located in nested entities.</p>
 * <pre>
 * factory.buildInMemoryStore(Order.class)
 *     .target(Order_.customerCityName)
 *     .from(City.class, keys -&gt; keys
 *         .on(pk -&gt; pk.field(Order_.customer).field(Customer_.cityId), 
 *             fk -&gt; fk.field(City_.id))
 *         .on(pk -&gt; pk.field(Order_.customer).field(Customer_.countryCode), 
 *             fk -&gt; fk.field(City_.countryCode))
 *     )
 *     .value(nav -&gt; nav.field(City_.name));
 * </pre>
 * 
 * <h2>Migration from Old API</h2>
 * <p>The old API used two separate function parameters for primary and foreign keys.
 * The new API uses KeyPairBuilder for better clarity and support for multiple fields.</p>
 * 
 * Old API (still supported for backward compatibility):
 * <pre>
 * .from(Order.class,
 *     pk -&gt; pk.field(User_.id),
 *     fk -&gt; fk.field(Order_.userId))
 * </pre>
 * 
 * New API (recommended):
 * <pre>
 * .from(Order.class, keys -&gt; keys
 *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 * )
 * </pre>
 * 
 * Migrating to Composite Keys:
 * <p>To add additional key fields, simply chain more {@code on()} calls:</p>
 * <pre>
 * .from(Order.class, keys -&gt; keys
 *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *     .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))  // Add second field
 * )
 * </pre>
 * 
 * <h2>Validation</h2>
 * <p>The KeyPairBuilder performs validation when the mapping is built:</p>
 * <ul>
 * <li>At least one key pair must be defined</li>
 * <li>Primary and foreign key field counts must match</li>
 * <li>Corresponding field types must be compatible</li>
 * <li>All field paths must be valid and navigable</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>KeyPairBuilder is not thread-safe. Each builder instance should be used
 * within a single thread during configuration.</p>
 * 
 * @see PropertyNavigationBuilder
 * @see PropertyNavigation
 */
public class KeyPairBuilder {
    
    private final Class<?> targetClass;
    private final Class<?> sourceClass;
    private final List<PropertyNavigation> primaryKeyPaths;
    private final List<PropertyNavigation> foreignKeyPaths;
    
    /**
     * Creates a new KeyPairBuilder for the specified target and source classes.
     * 
     * @param targetClass the target entity class (cannot be null)
     * @param sourceClass the source entity class (cannot be null)
     * @throws NullPointerException if targetClass or sourceClass is null
     */
    public KeyPairBuilder(Class<?> targetClass, Class<?> sourceClass) {
        this.targetClass = Objects.requireNonNull(targetClass, "Target class cannot be null");
        this.sourceClass = Objects.requireNonNull(sourceClass, "Source class cannot be null");
        this.primaryKeyPaths = new ArrayList<>();
        this.foreignKeyPaths = new ArrayList<>();
    }
    
    /**
     * Adds a primary key to foreign key field pair to the composite key definition.
     * 
     * <p>Each call to this method adds one field pair to the composite key. The order
     * of calls matters - fields will be matched in the order they are defined.</p>
     * 
     * <p>The primary key builder navigates from the target entity class, while the
     * foreign key builder navigates from the source entity class.</p>
     * 
     * @param primaryKeyBuilder function to build the primary key field path from target entity
     * @param foreignKeyBuilder function to build the foreign key field path from source entity
     * @return this builder for method chaining
     * @throws NullPointerException if primaryKeyBuilder or foreignKeyBuilder is null
     * @throws IllegalStateException if the builder functions fail to build valid PropertyNavigation
     * 
     * @see PropertyNavigationBuilder#field
     * @see PropertyNavigationBuilder#build
     */
    public KeyPairBuilder on(
            Function<PropertyNavigationBuilder, PropertyNavigationBuilder> primaryKeyBuilder,
            Function<PropertyNavigationBuilder, PropertyNavigationBuilder> foreignKeyBuilder) {
        
        Objects.requireNonNull(primaryKeyBuilder, "Primary key builder cannot be null");
        Objects.requireNonNull(foreignKeyBuilder, "Foreign key builder cannot be null");
        
        // Build primary key path
        PropertyNavigationBuilder pkNav = new PropertyNavigationBuilder(targetClass);
        PropertyNavigationBuilder pkResult = primaryKeyBuilder.apply(pkNav);
        PropertyNavigation pkPath = pkResult.build();
        primaryKeyPaths.add(pkPath);
        
        // Build foreign key path
        PropertyNavigationBuilder fkNav = new PropertyNavigationBuilder(sourceClass);
        PropertyNavigationBuilder fkResult = foreignKeyBuilder.apply(fkNav);
        PropertyNavigation fkPath = fkResult.build();
        foreignKeyPaths.add(fkPath);
        
        return this;
    }
    
    /**
     * Gets the list of primary key paths that have been defined.
     * 
     * @return unmodifiable list of primary key PropertyNavigation instances
     */
    public List<PropertyNavigation> getPrimaryKeyPaths() {
        return List.copyOf(primaryKeyPaths);
    }
    
    /**
     * Gets the list of foreign key paths that have been defined.
     * 
     * @return unmodifiable list of foreign key PropertyNavigation instances
     */
    public List<PropertyNavigation> getForeignKeyPaths() {
        return List.copyOf(foreignKeyPaths);
    }
    
    /**
     * Gets the target entity class.
     * 
     * @return the target class
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }
    
    /**
     * Gets the source entity class.
     * 
     * @return the source class
     */
    public Class<?> getSourceClass() {
        return sourceClass;
    }
    
    /**
     * Gets the number of key pairs that have been defined.
     * 
     * @return the number of key pairs
     */
    public int getKeyPairCount() {
        return primaryKeyPaths.size();
    }
}
