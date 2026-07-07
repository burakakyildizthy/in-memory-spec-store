package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Base context for configuring source mapping with composite key support.
 * Provides operations available for all target types.
 * Subclasses add type-specific operations (sum, avg, min, max).
 * All terminal operations return AbstractRootBuilder&lt;R&gt;.
 *
 * <p>This class handles the source side of property mappings, including:
 * <ul>
 * <li>Storing composite key paths for matching target and source entities</li>
 * <li>Filtering source data with where() clauses</li>
 * <li>Defining aggregations (count, sum, avg, min, max)</li>
 * <li>Defining collection operations (any, all, first, last)</li>
 * <li>Defining value and collection mappings</li>
 * </ul>
 *
 * @param <R> the root entity type
 * @param <C> the current target field type
 * @param <S> the source entity type
 * <h2>Composite Key Support</h2>
 * <p>This class stores lists of primary and foreign key paths that define the relationship
 * between target and source entities. For single-field mappings, these lists contain one element.
 * For composite keys, they contain multiple elements.</p>
 *
 * Key Methods for Composite Keys
 * <ul>
 * <li>{@link #isCompositeKey()} - Check if this mapping uses multiple key fields</li>
 * <li>{@link #getKeyFieldCount()} - Get the number of key fields</li>
 * <li>{@link #getPrimaryKeyPaths()} - Get all primary key paths</li>
 * <li>{@link #getForeignKeyPaths()} - Get all foreign key paths</li>
 * </ul>
 *
 * Example: Single-Field Mapping
 * <pre>
 * .from(OrderSpecificationService.INSTANCE, keys -&gt; keys
 *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 * )
 * .count();
 *
 * // isCompositeKey() returns false
 * // getKeyFieldCount() returns 1
 * </pre>
 *
 * Example: Two-Field Composite Key
 * <pre>
 * .from(OrderSpecificationService.INSTANCE, keys -&gt; keys
 *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
 *     .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
 * )
 * .count();
 *
 * // isCompositeKey() returns true
 * // getKeyFieldCount() returns 2
 * </pre>
 *
 * @param <R> the root entity type
 * @param <C> the current target field type
 * @param <S> the source entity type
 * @see KeyPairBuilder
 * @see PropertyNavigationContext#from(com.thy.fss.common.inmemory.specification.SpecificationService, Function)
 */
public class SourceMappingContext<R, C, S> {

    private static final String SOURCE_FIELD_CANNOT_BE_NULL = "Source field builder cannot be null";
    private static final String FUNC_CANNOT_BE_NULL = "Condition function cannot be null";

    private final PropertyNavigationContext<R, C> targetContext;
    private final com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService;
    private final List<PropertyNavigation> primaryKeyPaths;
    private final List<PropertyNavigation> foreignKeyPaths;
    private Specification<S> specification;

    /**
     * Package-private constructor for creating source mapping context with composite keys.
     *
     * @param targetContext the target navigation context
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths for joining (null for Dashboard)
     * @param foreignKeyPaths the list of foreign key paths for joining (null for Dashboard)
     * @throws NullPointerException if targetContext or sourceService is null
     */
    public SourceMappingContext(
            PropertyNavigationContext<R, C> targetContext,
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        this.targetContext = Objects.requireNonNull(targetContext, "Target context cannot be null");
        this.sourceService = Objects.requireNonNull(sourceService, "Source service cannot be null");
        // primaryKeyPaths and foreignKeyPaths can be null for Dashboard
        this.primaryKeyPaths = primaryKeyPaths != null ? List.copyOf(primaryKeyPaths) : null;
        this.foreignKeyPaths = foreignKeyPaths != null ? List.copyOf(foreignKeyPaths) : null;
    }

    /**
     * Gets the source entity specification service.
     *
     * @return the source specification service
     */
    public com.thy.fss.common.inmemory.specification.SpecificationService<S> getSourceService() {
        return sourceService;
    }

    /**
     * Check if this mapping uses composite keys (more than one key field).
     *
     * <p>A composite key mapping has multiple field pairs that must all match
     * for entities to be related. Single-field mappings return false.</p>
     *
     * Example:
     * <pre>
     * // Single-field mapping
     * .from(Order.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     * )
     * // isCompositeKey() returns false
     *
     * // Two-field composite key
     * .from(Order.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     *     .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
     * )
     * // isCompositeKey() returns true
     * </pre>
     *
     * @return true if the mapping has more than one key field, false otherwise
     */
    public boolean isCompositeKey() {
        return primaryKeyPaths != null && primaryKeyPaths.size() > 1;
    }

    /**
     * Get the number of key fields in this mapping.
     *
     * <p>Returns the count of field pairs in the composite key. For single-field
     * mappings, returns 1. For Dashboard mappings (which don't use keys), returns 0.</p>
     *
     * Example:
     * <pre>
     * // Single-field mapping
     * .from(Order.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     * )
     * // getKeyFieldCount() returns 1
     *
     * // Three-field composite key
     * .from(FlightLeg.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(Flight_.flightNo), fk -&gt; fk.field(FlightLeg_.flightNo))
     *     .on(pk -&gt; pk.field(Flight_.date), fk -&gt; fk.field(FlightLeg_.date))
     *     .on(pk -&gt; pk.field(Flight_.legSequence), fk -&gt; fk.field(FlightLeg_.legSequence))
     * )
     * // getKeyFieldCount() returns 3
     * </pre>
     *
     * @return the number of key fields, or 0 if no keys are defined
     */
    public int getKeyFieldCount() {
        return primaryKeyPaths != null ? primaryKeyPaths.size() : 0;
    }

    /**
     * Get the list of primary key paths for this mapping.
     *
     * <p>Each PropertyNavigation in the list represents one field in the composite key
     * from the target entity. The order matches the order of {@link #getForeignKeyPaths()}.</p>
     *
     * Example:
     * <pre>
     * .from(Order.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     *     .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
     * )
     *
     * // getPrimaryKeyPaths() returns:
     * // [PropertyNavigation(User_.id), PropertyNavigation(User_.regionId)]
     * </pre>
     *
     * @return unmodifiable list of primary key paths, or null if not defined (Dashboard)
     */
    public List<PropertyNavigation> getPrimaryKeyPaths() {
        return primaryKeyPaths;
    }

    /**
     * Get the list of foreign key paths for this mapping.
     *
     * <p>Each PropertyNavigation in the list represents one field in the composite key
     * from the source entity. The order matches the order of {@link #getPrimaryKeyPaths()}.</p>
     *
     * Example:
     * <pre>
     * .from(Order.class, keys -&gt; keys
     *     .on(pk -&gt; pk.field(User_.id), fk -&gt; fk.field(Order_.userId))
     *     .on(pk -&gt; pk.field(User_.regionId), fk -&gt; fk.field(Order_.regionId))
     * )
     *
     * // getForeignKeyPaths() returns:
     * // [PropertyNavigation(Order_.userId), PropertyNavigation(Order_.regionId)]
     * </pre>
     *
     * @return unmodifiable list of foreign key paths, or null if not defined (Dashboard)
     */
    public List<PropertyNavigation> getForeignKeyPaths() {
        return foreignKeyPaths;
    }

    /**
     * Add filter specification for source data.
     * The specification is used to filter source entities before mapping.
     *
     * @param whereFunction function that receives navigation builder and spec builder
     * @return this context for method chaining
     * @throws NullPointerException if whereFunction is null
     */
    public SourceMappingContext<R, C, S> where(
            BiFunction<PropertyNavigationBuilder, SpecificationBuilder<S>, Specification<S>> whereFunction) {

        Objects.requireNonNull(whereFunction, "Where function cannot be null");

        Class<S> sourceClass = sourceService.getEntityClass();
        PropertyNavigationBuilder navBuilder = new PropertyNavigationBuilder(sourceClass);
        SpecificationBuilder<S> specBuilder = new SpecificationBuilder<>(sourceService);
        this.specification = whereFunction.apply(navBuilder, specBuilder);

        return this;
    }

    /**
     * Complete mapping as COUNT aggregation.
     * Counts the number of source entities matching the join condition and where clause.
     * Returns to root builder.
     *
     * @return root builder for defining additional mappings
     */
    public AbstractRootBuilder<R> count() {
        createMapping(null, MappingType.MANY_TO_ONE_AGGREGATION, AggregationType.COUNT);
        return targetContext.getRootBuilder();
    }

    /**
     * Complete mapping as direct entity reference.
     * Maps the source entity itself (not a specific field) to the target field.
     * This is useful when the target field holds a reference to the entire source entity,
     * such as mapping a {@code Department} object directly to {@code User_.department}.
     *
     * <p>Requires that primary key and foreign key paths are defined via {@code from()}
     * so the system can resolve the related entity.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * .target(User_.department)
     *  .from(DepartmentService.INSTANCE, keys -> keys.on(Department_.id, User_.departmentId))
     *  .value()
     * }</pre>
     *
     * <p>Unlike {@link #value(UnaryOperator)}, which maps a specific field of the source entity,
     * this parameterless overload maps the source entity object itself.</p>
     *
     * @return root builder for defining additional mappings
     */
    public AbstractRootBuilder<R> value() {
        createMapping(null, MappingType.ONE_TO_ONE, null);
        return targetContext.getRootBuilder();
    }

    /**
     * Complete mapping as single value mapping.
     * Maps a single source field value to the target field.
     * Returns to root builder.
     *
     * @param sourceFieldBuilder function to select source field
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     */
    public AbstractRootBuilder<R> value(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {

        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);

        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        createMapping(sourcePath, MappingType.ONE_TO_ONE, null);
        return targetContext.getRootBuilder();
    }

    /**
     * Complete mapping as collection mapping.
     * Maps a collection of source entities to the target field.
     * Returns to root builder.
     *
     * @return root builder for defining additional mappings
     */
    public AbstractRootBuilder<R> collection() {
        createMapping(null, MappingType.MANY_TO_ONE_COLLECTION, null);
        return targetContext.getRootBuilder();
    }

    /**
     * Check if ANY source element matches condition.
     * Returns true if at least one source entity matches the condition.
     * Returns to root builder.
     *
     * @param conditionFunction function to build condition specification
     * @return root builder for defining additional mappings
     * @throws NullPointerException if conditionFunction is null
     */
    public AbstractRootBuilder<R> any(
            BiFunction<PropertyNavigationBuilder, SpecificationBuilder<S>, Specification<S>> conditionFunction) {

        Objects.requireNonNull(conditionFunction, FUNC_CANNOT_BE_NULL);

        Class<S> sourceClass = sourceService.getEntityClass();
        PropertyNavigationBuilder navBuilder = new PropertyNavigationBuilder(sourceClass);
        SpecificationBuilder<S> specBuilder = new SpecificationBuilder<>(sourceService);
        Specification<S> condition = conditionFunction.apply(navBuilder, specBuilder);

        // Combine with existing where clause if present
        if (specification != null) {
            condition = specification.and(condition);
        }

        createMapping(null, MappingType.MANY_TO_ONE_ANY, null, condition);
        return targetContext.getRootBuilder();
    }

    /**
     * Check if ALL source elements match condition.
     * Returns true if all source entities match the condition.
     * Returns to root builder.
     *
     * @param conditionFunction function to build condition specification
     * @return root builder for defining additional mappings
     * @throws NullPointerException if conditionFunction is null
     */
    public AbstractRootBuilder<R> all(
            BiFunction<PropertyNavigationBuilder, SpecificationBuilder<S>, Specification<S>> conditionFunction) {

        Objects.requireNonNull(conditionFunction, FUNC_CANNOT_BE_NULL);

        Class<S> sourceClass = sourceService.getEntityClass();
        PropertyNavigationBuilder navBuilder = new PropertyNavigationBuilder(sourceClass);
        SpecificationBuilder<S> specBuilder = new SpecificationBuilder<>(sourceService);
        Specification<S> condition = conditionFunction.apply(navBuilder, specBuilder);

        // Combine with existing where clause if present
        if (specification != null) {
            condition = specification.and(condition);
        }

        createMapping(null, MappingType.MANY_TO_ONE_ALL, null, condition);
        return targetContext.getRootBuilder();
    }

    /**
     * Get field from FIRST source element.
     * Returns the field value from the first source entity (optionally filtered by where clause).
     * Returns to root builder.
     *
     * @param sourceFieldBuilder function to select source field
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     */
    public AbstractRootBuilder<R> first(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {

        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);

        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        createMapping(sourcePath, MappingType.MANY_TO_ONE_FIRST, null);
        return targetContext.getRootBuilder();
    }

    /**
     * Get field from LAST source element.
     * Returns the field value from the last source entity (optionally filtered by where clause).
     * Returns to root builder.
     *
     * @param sourceFieldBuilder function to select source field
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     */
    public AbstractRootBuilder<R> last(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {

        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);

        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        createMapping(sourcePath, MappingType.MANY_TO_ONE_LAST, null);
        return targetContext.getRootBuilder();
    }

    /**
     * Helper method to build source path from builder function.
     *
     * @param sourceFieldBuilder function to build source field path
     * @return the built PropertyNavigation
     */
    protected PropertyNavigation buildSourcePath(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        Class<S> sourceClass = sourceService.getEntityClass();
        PropertyNavigationBuilder navBuilder = new PropertyNavigationBuilder(sourceClass);
        PropertyNavigationBuilder result = sourceFieldBuilder.apply(navBuilder);
        return result.build();
    }

    /**
     * Get the root builder instance.
     * Protected method for subclasses to access root builder.
     *
     * @return the root builder
     */
    protected AbstractRootBuilder<R> getRootBuilder() {
        return targetContext.getRootBuilder();
    }

    /**
     * Helper method to validate source field type.
     *
     * @param sourcePath the source field path to validate
     * @param expectedType the expected field type
     * @param operation the operation name (for error messages)
     * @throws IllegalArgumentException if source field type doesn't match expected type
     */
    protected void validateSourceFieldType(PropertyNavigation sourcePath,
                                           Class<?> expectedType,
                                           String operation) {
        Class<?> sourceFieldType = sourcePath.getLeafClass();
        if (!expectedType.isAssignableFrom(sourceFieldType)) {
            throw new IllegalArgumentException(
                    String.format("Source field type must be %s for %s operation. Found: %s",
                            expectedType.getSimpleName(),
                            operation,
                            sourceFieldType.getName())
            );
        }
    }

    /**
     * Helper method to create and register property mapping.
     * Uses the current specification stored in this context.
     *
     * @param sourcePath the source field path (null for count/collection operations)
     * @param mappingType the type of mapping
     * @param aggregationType the aggregation type (null if not aggregation)
     */
    protected void createMapping(PropertyNavigation sourcePath, MappingType mappingType,
                                 AggregationType aggregationType) {
        createMapping(sourcePath, mappingType, aggregationType, specification);
    }

    /**
     * Helper method to create and register property mapping with custom specification.
     * This overload is used by any() and all() operations that provide their own specification.
     *
     * @param sourcePath the source field path (null for count/collection operations)
     * @param mappingType the type of mapping
     * @param aggregationType the aggregation type (null if not aggregation)
     * @param spec the specification to use for filtering
     */
    protected void createMapping(PropertyNavigation sourcePath, MappingType mappingType,
                                 AggregationType aggregationType, Specification<S> spec) {

        // Resolve datasource name from source service using factory
        String datasourceName = targetContext.getRootBuilder()
                .getFactory()
                .getDataSourceNameByClass(sourceService.getEntityClass());

        // Get isForDashboard flag from root builder
        boolean isForDashboard = targetContext.getRootBuilder().isForDashboard();

        // Convert List<PropertyNavigation> to List<List<MetaAttribute<?, ?>>>
        List<List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> pkPaths = null;
        List<List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> fkPaths = null;

        if (primaryKeyPaths != null && !primaryKeyPaths.isEmpty()) {
            pkPaths = new ArrayList<>();
            for (PropertyNavigation nav : primaryKeyPaths) {
                pkPaths.add(nav.getPath());
            }
        }

        if (foreignKeyPaths != null && !foreignKeyPaths.isEmpty()) {
            fkPaths = new ArrayList<>();
            for (PropertyNavigation nav : foreignKeyPaths) {
                fkPaths.add(nav.getPath());
            }
        }

        // Build PropertyMapping using builder pattern with service instances
        PropertyMapping<R, C> mapping = PropertyMapping.<R, C>builder()
                .consumerId(targetContext.getRootBuilder().getConsumerId())
                .isForDashboard(isForDashboard)
                .targetPath(targetContext.getTargetPath())
                .targetCollectionOperations(targetContext.getCollectionOperations())
                .sourceService(sourceService)  // Use service instance
                .targetService(getTargetService())  // Get target service from root builder
                .datasourceName(datasourceName)
                .primaryKeyPaths(pkPaths)
                .foreignKeyPaths(fkPaths)
                .sourcePath(sourcePath != null ? sourcePath.getPath() : null)
                .sourceCollectionOperations(sourcePath != null ? sourcePath.getCollectionOperations() : null)
                .specification(spec)
                .mappingType(mappingType)
                .aggregationType(aggregationType)
                .build();

        // Register mapping with root builder
        targetContext.getRootBuilder().addPropertyMapping(mapping);
    }

    /**
     * Helper method to get the target service from the root builder.
     * This method will be implemented once AbstractRootBuilder is updated to store the service.
     *
     * @return the target specification service
     */
    @SuppressWarnings("unchecked")
    private com.thy.fss.common.inmemory.specification.SpecificationService<R> getTargetService() {
        // TODO: This should be retrieved from AbstractRootBuilder once it's updated to store the service
        // For now, we need to use SpecificationServices.getService() as a temporary solution
        // This will be replaced in task 3 when we update the factory API
        return (com.thy.fss.common.inmemory.specification.SpecificationService<R>)
                com.thy.fss.common.inmemory.specification.SpecificationServices.getService(
                        targetContext.getRootBuilder().getEntityClass()
                );
    }
}
