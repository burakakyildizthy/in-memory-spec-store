package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Pure navigation builder for constructing field paths. Used in from() lambdas
 * and where() lambdas to build PropertyNavigation instances.
 *
 * <p>
 * This class has NO generic parameters at class level - instead it uses generic
 * methods for type-safe field access. This allows flexible navigation without
 * complex generic chains.</p>
 *
 * Type Safety Design
 * <p>
 * This class uses method-level generics rather than class-level generics for
 * simplicity. Type safety is provided by the metamodel:</p>
 * <ul>
 * <li>Each MetaAttribute is strongly typed (e.g., User_.id is
 * MetaAttribute&lt;User, Long&gt;)</li>
 * <li>The metamodel generator ensures field-to-owner relationships are
 * correct</li>
 * <li>Runtime validation catches any path construction errors</li>
 * </ul>
 *
 * <p>
 * <b>Trade-off:</b> While the compiler cannot validate field chains at compile
 * time (e.g., it won't prevent
 * {@code nav.field(User_.id).field(Order_.userId)}), the metamodel provides
 * sufficient type safety in practice. Developers are guided by IDE autocomplete
 * to write correct paths, and incorrect paths fail fast at runtime with clear
 * error messages.</p>
 *
 * Example Usage
 * <pre>
 * // In from() lambda - building PK/FK paths
 * .from(Order.class,
 *     pk -> pk.field(User_.id),           // MetaAttribute&lt;User, Long&gt;
 *     fk -> fk.field(Order_.userId)       // MetaAttribute&lt;Order, Long&gt;
 * )
 *
 * // In where() lambda - building specification paths
 * .where((nav, spec) -> spec.on(nav.field(Order_.status)).eq("COMPLETED"))
 *
 * // Collection navigation with first/last
 * nav.field(City_.districts)              // Navigate to collection
 *    .first()                             // Get first element
 *    .field(District_.name)               // Navigate to element field
 *
 * // Nested object navigation
 * nav.field(User_.address)                // Navigate to Address
 *    .field(Address_.cityId)              // Navigate to Long
 * </pre>
 *
 * Correct vs Incorrect Usage
 * <pre>
 * //  CORRECT: Metamodel ensures type safety
 * nav.field(User_.address).field(Address_.cityId)
 * // User_.address is MetaAttribute&lt;User, Address&gt;
 * // Address_.cityId is MetaAttribute&lt;Address, Long&gt;
 *
 * //  INCORRECT: Would fail at runtime (not caught at compile time)
 * nav.field(User_.id).field(Address_.cityId)
 * // User_.id is MetaAttribute&lt;User, Long&gt;
 * // Can't navigate from Long to Address - runtime error
 * </pre>
 *
 * @see PropertyNavigation
 * @see MetaAttribute
 */
public class PropertyNavigationBuilder {
    
    private static final String ATTR_CANNOT_BE_NULL = "Attribute cannot be null";

    private final Class<?> rootClass;
    private final LinkedList<MetaAttribute<?, ?>> path;
    private final List<CollectionOperationMetadata<?, ?>> collectionOperations;

    /**
     * Public constructor for creating a new navigation builder.
     *
     * @param rootClass the root entity class to start navigation from
     * @throws IllegalArgumentException if rootClass is null
     */
    public PropertyNavigationBuilder(Class<?> rootClass) {
        this(rootClass, new LinkedList<>(), new ArrayList<>());
    }

    /**
     * Public constructor for internal use when extending the path. Made public
     * to allow type-safe navigation builder subclasses.
     *
     * @param rootClass the root entity class
     * @param path the current field path
     * @param collectionOperations the collection operations metadata
     */
    public PropertyNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        this.rootClass = Objects.requireNonNull(rootClass, "Root class cannot be null");
        this.path = path;
        this.collectionOperations = collectionOperations;
    }

    /**
     * Navigate to a field.
     *
     * <p>
     * This method uses method-level generics to ensure the attribute is
     * strongly typed. The metamodel guarantees that
     * {@code MetaAttribute<O, F>} relationships are correct.</p>
     *
     * <p>
     * <b>Note:</b> The compiler cannot validate that O matches the current
     * navigation type. For example, after {@code nav.field(User_.address)}, you
     * should call {@code field(Address_.cityId)}, not {@code field(User_.id)}.
     * The metamodel and IDE autocomplete guide you to write correct paths.</p>
     *
     * @param <O> the owner type of the attribute (not validated against
     * current type)
     * @param <F> the field type
     * @param attribute the meta attribute
     * @return new PropertyNavigationBuilder with extended path
     * @throws IllegalArgumentException if attribute is null
     */
    public <O, F> PropertyNavigationBuilder field(MetaAttribute<O, F> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new PropertyNavigationBuilder(rootClass, newPath, collectionOperations);
    }

    // ==================== TYPE-SAFE F NAVIGATION METHODS ====================
    /**
     * Navigate to a String field with compile-time type safety. Returns
     * StringNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param attribute the String attribute
     * @return StringNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O> StringNavigationBuilder<O> field(com.thy.fss.common.inmemory.specification.attribute.StringAttribute<O> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new StringNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a numeric field with compile-time type safety. Returns
     * NumericNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param <N> the numeric type
     * @param attribute the numeric attribute
     * @return NumericNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O, N extends Number> NumericNavigationBuilder<O, N> field(com.thy.fss.common.inmemory.specification.attribute.NumericMetaAttribute<O, N> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new NumericNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a Boolean field with compile-time type safety. Returns
     * BooleanNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param attribute the Boolean attribute
     * @return BooleanNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O> BooleanNavigationBuilder<O> field(com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<O> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new BooleanNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to an Enum field with compile-time type safety. Returns
     * EnumNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param <E> the enum type
     * @param attribute the Enum attribute
     * @return EnumNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O, E extends Enum<E>> EnumNavigationBuilder<O, E> field(com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<O, E> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new EnumNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a LocalDateTime field with compile-time type safety. Returns
     * TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param attribute the LocalDateTime attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O> TemporalNavigationBuilder<O, java.time.LocalDateTime> field(com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute<O> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new TemporalNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a LocalDate field with compile-time type safety. Returns
     * TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param attribute the LocalDate attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O> TemporalNavigationBuilder<O, java.time.LocalDate> field(com.thy.fss.common.inmemory.specification.attribute.LocalDateAttribute<O> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new TemporalNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to an Instant field with compile-time type safety. Returns
     * TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <O> the owner type of the attribute
     * @param attribute the Instant attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O> TemporalNavigationBuilder<O, java.time.Instant> field(com.thy.fss.common.inmemory.specification.attribute.InstantAttribute<O> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new TemporalNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a Model field with compile-time type safety. Returns
     * ModelNavigationBuilder for nested field navigation.
     *
     * @param <O> the owner type of the attribute
     * @param <M> the model type
     * @param attribute the Model attribute
     * @return ModelNavigationBuilder for nested field navigation
     * @throws IllegalArgumentException if attribute is null
     */
    public <O, M> ModelNavigationBuilder<O, M> field(com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<O, M> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new ModelNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Navigate to a Collection field with compile-time type safety. Returns
     * CollectionNavigationBuilder for type-safe SpecificationBuilder.on()
     * usage.
     *
     * <p>
     * This method is used to navigate to collection-typed fields. After calling
     * this method, you can use {@link CollectionNavigationBuilder#first()} or
     * {@link CollectionNavigationBuilder#last()} to select elements from the
     * collection, then continue navigating to element fields.</p>
     *
     * @param <O> the owner type of the attribute
     * @param <E> the element type
     * @param attribute the Collection attribute
     * @return CollectionNavigationBuilder for type-safe specification building
     * @throws IllegalArgumentException if attribute is null
     */
    public <O, E> CollectionNavigationBuilder<O, E> field(CollectionAttribute<O, E> attribute) {
        Objects.requireNonNull(attribute, "Collection attribute cannot be null");
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(path);
        newPath.add(attribute);
        return new CollectionNavigationBuilder<>(rootClass, newPath, collectionOperations, attribute);
    }

    /**
     * Build the PropertyNavigation.
     *
     * @return immutable PropertyNavigation
     * @throws IllegalStateException if path is empty
     */
    public PropertyNavigation build() {
        if (path.isEmpty()) {
            throw new IllegalStateException("Cannot build PropertyNavigation with empty path");
        }
        return new PropertyNavigation(path, collectionOperations);
    }

    // Getters for internal use
    /**
     * Gets the root class.
     *
     * @return the root entity class
     */
    public Class<?> getRootClass() {
        return rootClass;
    }

    /**
     * Gets a copy of the current path.
     *
     * @return copy of the field path
     */
    public List<MetaAttribute<?, ?>> getPath() {
        return new ArrayList<>(path);
    }

    /**
     * Gets the collection operations metadata. Public method for specification builders
     * to access collection operations.
     *
     * @return the collection operations metadata
     */
    public List<CollectionOperationMetadata<?, ?>> getCollectionOperations() {
        return collectionOperations;
    }
}
