package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.List;
import java.util.Objects;

/**
 * Represents a property mapping configuration that defines how data from a
 * datasource should be mapped to a target entity property.
 *
 * <p>
 * This class is used by both InMemoryDataStore and Dashboard, but with
 * different semantics:</p>
 * <ul>
 * <li><b>Store</b>: Primary datasource provides root entities, PropertyMapping
 * is optional, can be ONE_TO_ONE, MANY_TO_ONE_COLLECTION, or
 * MANY_TO_ONE_AGGREGATION</li>
 * <li><b>Dashboard</b>: No primary datasource, PropertyMapping is required, can
 * only be MANY_TO_ONE_AGGREGATION</li>
 * </ul>
 *
 * <h2>Version 2.0 Changes</h2>
 * <p>
 * PropertyMapping now stores {@link SpecificationService} instances instead of
 * {@code Class<?>} objects. This eliminates runtime type lookups and provides
 * better type safety.
 * </p>
 *
 * Migration Example:
 * <pre>{@code
 * // Before (v1.x) - NO LONGER WORKS
 * PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
 *     .sourceClass(Order.class)
 *     .targetPath(Arrays.asList(User_.name))
 *     .build();
 *
 * // After (v2.0) - Required
 * PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
 *     .sourceService(OrderSpecificationService.INSTANCE)
 *     .targetService(UserSpecificationService.INSTANCE)
 *     .targetPath(Arrays.asList(User_.name))
 *     .build();
 * }</pre>
 *
 * @param <T> the target entity type
 * @param <F> the target field type
 * @see SpecificationService
 * @since 2.0
 */
public class PropertyMapping<T, F> {

    // Consumer identification
    private final String consumerId; // storeId or dashboardId
    private final boolean isForDashboard;

    // Target property (path-based for nested field support)
    private final List<MetaAttribute<?, ?>> targetPath;

    // Source datasource
    private final String dataSourceName;
    private final SpecificationService<?> sourceService; // Source service for type safety
    private final SpecificationService<T> targetService; // Target service for type safety

    // Source field path (for aggregation or single value mapping)
    private final List<MetaAttribute<?, ?>> sourcePath;

    // Collection operation metadata stored separately (type-safe with generics)
    private final List<CollectionOperationMetadata<?, ?>> sourceCollectionOperations;
    private final List<CollectionOperationMetadata<?, ?>> targetCollectionOperations;

    // Relationship key paths (Store only - for primary-foreign key relationships)
    // Each list represents one field in the composite key
    private final List<List<MetaAttribute<?, ?>>> primaryKeyPaths;
    private final List<List<MetaAttribute<?, ?>>> foreignKeyPaths;

    // Optional filter
    private final Specification<?> specification;

    // Mapping configuration
    private final MappingType mappingType;
    private final AggregationType aggregationType; // Only for MANY_TO_ONE_AGGREGATION

    /**
     * Private constructor. Use Builder to create instances.
     */
    private PropertyMapping(Builder<T, F> builder) {
        this.consumerId = builder.consumerId;
        this.isForDashboard = builder.isForDashboard;
        this.targetPath = builder.targetPath;
        this.sourcePath = builder.sourcePath;
        this.sourceCollectionOperations = builder.sourceCollectionOperations;
        this.targetCollectionOperations = builder.targetCollectionOperations;
        this.primaryKeyPaths = builder.primaryKeyPaths;
        this.foreignKeyPaths = builder.foreignKeyPaths;
        this.sourceService = builder.sourceService;
        this.targetService = builder.targetService;
        this.dataSourceName = builder.datasourceName;
        this.specification = builder.specification;
        this.mappingType = builder.mappingType;
        this.aggregationType = builder.aggregationType;

        validate();
    }

    /**
     * Creates a new Builder for PropertyMapping.
     *
     * @param <T> the target entity type
     * @param <F> the target field type
     * @return a new Builder instance
     */
    public static <T, F> Builder<T, F> builder() {
        return new Builder<>();
    }

    // Getters
    /**
     * Validates the property mapping configuration.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    private void validate() {
        Objects.requireNonNull(consumerId, "Consumer ID cannot be null");
        Objects.requireNonNull(mappingType, "Mapping type cannot be null");

        // Validate target path
        if (targetPath == null || targetPath.isEmpty()) {
            throw new IllegalStateException("Target path cannot be null or empty");
        }

        // Validate target path elements (must be MetaAttribute or CollectionPathSegment)
        validatePath(targetPath, "Target");

        // Validate datasource
        if (dataSourceName == null || dataSourceName.isEmpty()) {
            throw new IllegalStateException("Datasource name cannot be null or empty");
        }

        // Validate services
        if (sourceService == null) {
            throw new IllegalStateException("Source service cannot be null");
        }
        if (targetService == null) {
            throw new IllegalStateException("Target service cannot be null");
        }

        // Dashboard-specific validation
        if (isForDashboard) {
            if (mappingType != MappingType.MANY_TO_ONE_AGGREGATION) {
                throw new IllegalStateException(
                        "Dashboard mappings must be MANY_TO_ONE_AGGREGATION type"
                );
            }
            if (aggregationType == null) {
                throw new IllegalStateException(
                        "Dashboard mappings must have an aggregation type"
                );
            }
            // For COUNT aggregation, sourcePath can be null
            // For other aggregations (SUM, AVG, MIN, MAX), sourcePath is required
            if ((sourcePath == null || sourcePath.isEmpty()) && aggregationType != AggregationType.COUNT) {
                throw new IllegalStateException(
                        "Dashboard mappings must have a source attribute for " + aggregationType + " aggregation"
                );
            }
            if ((primaryKeyPaths != null && !primaryKeyPaths.isEmpty())
                    || (foreignKeyPaths != null && !foreignKeyPaths.isEmpty())) {
                throw new IllegalStateException(
                        "Dashboard mappings cannot have primary/foreign key fields"
                );
            }
        }

        // Store-specific validation
        if (!isForDashboard) {
            // Store mappings with relationships need primary and foreign keys
            // This applies to all mapping types including ONE_TO_ONE with null sourcePath
            // (direct model reference), which still requires PK/FK to locate the related entity
            if (primaryKeyPaths == null || primaryKeyPaths.isEmpty()
                    || foreignKeyPaths == null || foreignKeyPaths.isEmpty()) {
                throw new IllegalStateException(
                        "Store mappings with relationships must have primary and foreign key fields"
                );
            }

            // Validate composite key configuration
            validateCompositeKeys();
        }

        // Aggregation-specific validation
        if (mappingType == MappingType.MANY_TO_ONE_AGGREGATION) {
            if (aggregationType == null) {
                throw new IllegalStateException(
                        "MANY_TO_ONE_AGGREGATION mappings must have an aggregation type"
                );
            }
            if ((sourcePath == null || sourcePath.isEmpty()) && aggregationType != AggregationType.COUNT) {
                throw new IllegalStateException(
                        "MANY_TO_ONE_AGGREGATION mappings (except COUNT) must have a source attribute"
                );
            }
        }

        // Validate source path if present
        if (sourcePath != null && !sourcePath.isEmpty()) {
            validatePath(sourcePath, "Source");
        }

        // Validate primary key paths if present
        if (primaryKeyPaths != null && !primaryKeyPaths.isEmpty()) {
            for (int i = 0; i < primaryKeyPaths.size(); i++) {
                List<MetaAttribute<?, ?>> path = primaryKeyPaths.get(i);
                if (path == null || path.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("Primary key path at index %d cannot be null or empty", i)
                    );
                }
                validatePath(path, "Primary key[" + i + "]");
            }
        }

        // Validate foreign key paths if present
        if (foreignKeyPaths != null && !foreignKeyPaths.isEmpty()) {
            for (int i = 0; i < foreignKeyPaths.size(); i++) {
                List<MetaAttribute<?, ?>> path = foreignKeyPaths.get(i);
                if (path == null || path.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("Foreign key path at index %d cannot be null or empty", i)
                    );
                }
                validatePath(path, "Foreign key[" + i + "]");
            }
        }

        // Validate collection operations
        if (sourceCollectionOperations != null && !sourceCollectionOperations.isEmpty()) {
            validateCollectionOperations(sourceCollectionOperations, sourcePath, "Source");
        }
        if (targetCollectionOperations != null && !targetCollectionOperations.isEmpty()) {
            validateCollectionOperations(targetCollectionOperations, targetPath, "Target");
        }
    }

    /**
     * Validates composite key configuration.
     * Ensures field counts match and types are compatible.
     *
     * <p>This method performs the following validations:</p>
     * <ul>
     * <li>Primary and foreign key field counts must be equal</li>
     * <li>Corresponding field types at each position must match</li>
     * <li>All paths must be non-null and non-empty</li>
     * </ul>
     *
     * Validation Examples
     *
     * <p><b>Valid Configuration:</b></p>
     * <pre>
     * Primary Keys: [User_.id (Long), User_.regionId (String)]
     * Foreign Keys: [Order_.userId (Long), Order_.regionId (String)]
     * // ✓ Same count, matching types
     * </pre>
     *
     * <p><b>Invalid - Field Count Mismatch:</b></p>
     * <pre>
     * Primary Keys: [User_.id (Long), User_.regionId (String)]
     * Foreign Keys: [Order_.userId (Long)]
     * // ✗ Throws: "Composite key field count mismatch: 2 primary key fields vs 1 foreign key fields"
     * </pre>
     *
     * <p><b>Invalid - Type Mismatch:</b></p>
     * <pre>
     * Primary Keys: [User_.id (Long), User_.regionId (String)]
     * Foreign Keys: [Order_.userId (Long), Order_.regionCode (Integer)]
     * // ✗ Throws: "Composite key field type mismatch at position 1: String vs Integer"
     * </pre>
     *
     * @throws IllegalStateException if composite key configuration is invalid
     */
    private void validateCompositeKeys() {
        if (primaryKeyPaths == null || foreignKeyPaths == null) {
            return;
        }

        // Validate same number of fields
        if (primaryKeyPaths.size() != foreignKeyPaths.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Composite key field count mismatch: %d primary key fields vs %d foreign key fields",
                            primaryKeyPaths.size(),
                            foreignKeyPaths.size()
                    )
            );
        }

        // Validate field types match at each position
        for (int i = 0; i < primaryKeyPaths.size(); i++) {
            List<MetaAttribute<?, ?>> pkPath = primaryKeyPaths.get(i);
            List<MetaAttribute<?, ?>> fkPath = foreignKeyPaths.get(i);

            if (pkPath == null || pkPath.isEmpty()) {
                throw new IllegalStateException(
                        String.format("Primary key path at position %d cannot be null or empty", i)
                );
            }
            if (fkPath == null || fkPath.isEmpty()) {
                throw new IllegalStateException(
                        String.format("Foreign key path at position %d cannot be null or empty", i)
                );
            }

            Class<?> pkType = getLeafType(pkPath);
            Class<?> fkType = getLeafType(fkPath);

            if (!pkType.equals(fkType)) {
                throw new IllegalStateException(
                        String.format(
                                "Composite key field type mismatch at position %d: %s vs %s",
                                i,
                                pkType.getSimpleName(),
                                fkType.getSimpleName()
                        )
                );
            }
        }
    }

    /**
     * Gets the leaf type of a path (the type of the last attribute in the path).
     *
     * @param path the path to get the leaf type from
     * @return the leaf type
     */
    private Class<?> getLeafType(List<MetaAttribute<?, ?>> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        MetaAttribute<?, ?> leafAttribute = path.get(path.size() - 1);
        return leafAttribute.getFieldType();
    }

    /**
     * Validates that a path contains only MetaAttribute objects.
     *
     * @param path the path to validate
     * @param pathName the name of the path (for error messages)
     * @throws IllegalStateException if path contains invalid elements
     */
    private void validatePath(List<MetaAttribute<?, ?>> path, String pathName) {
        for (int i = 0; i < path.size(); i++) {
            MetaAttribute<?, ?> element = path.get(i);
            if (element == null) {
                throw new IllegalStateException(
                        String.format("%s path element at index %d cannot be null", pathName, i)
                );
            }
        }
    }

    /**
     * Validates collection operations against their corresponding path.
     *
     * @param operations the collection operations to validate
     * @param path the path that the operations apply to
     * @param pathName the name of the path (for error messages)
     * @throws IllegalStateException if collection operations are invalid
     */
    private void validateCollectionOperations(
            List<CollectionOperationMetadata<?, ?>> operations,
            List<MetaAttribute<?, ?>> path,
            String pathName
    ) {
        if (operations == null || path == null) {
            return;
        }

        for (CollectionOperationMetadata<?, ?> op : operations) {
            // Validate pathIndex is within path bounds
            if (op.getPathIndex() < 0 || op.getPathIndex() >= path.size()) {
                throw new IllegalStateException(
                        String.format(
                                "%s collection operation pathIndex %d is out of bounds (path size: %d)",
                                pathName, op.getPathIndex(), path.size()
                        )
                );
            }

            MetaAttribute<?, ?> pathAttribute = path.get(op.getPathIndex());

            // Validate attribute at pathIndex matches metadata attribute
            if (!pathAttribute.equals(op.getCollectionAttribute())) {
                throw new IllegalStateException(
                        String.format(
                                "%s collection operation attribute mismatch at index %d: expected %s, found %s",
                                pathName, op.getPathIndex(), op.getCollectionAttribute(), pathAttribute
                        )
                );
            }

            // Validate attribute at pathIndex is a CollectionAttribute
            if (!(pathAttribute instanceof CollectionAttribute)) {
                throw new IllegalStateException(
                        String.format(
                                "%s collection operation at index %d must reference a CollectionAttribute, but found %s",
                                pathName, op.getPathIndex(), pathAttribute.getClass().getSimpleName()
                        )
                );
            }
        }
    }

    public String getConsumerId() {
        return consumerId;
    }

    public boolean isForDashboard() {
        return isForDashboard;
    }

    public List<MetaAttribute<?, ?>> getTargetPath() {
        return targetPath;
    }

    public List<MetaAttribute<?, ?>> getSourcePath() {
        return sourcePath;
    }

    public List<List<MetaAttribute<?, ?>>> getPrimaryKeyPaths() {
        return primaryKeyPaths;
    }

    public List<List<MetaAttribute<?, ?>>> getForeignKeyPaths() {
        return foreignKeyPaths;
    }



    /**
     * Checks if this mapping uses composite keys (more than one field).
     *
     * <p>A composite key mapping requires multiple fields to match for entities
     * to be related. Single-field mappings return false.</p>
     *
     * Example:
     * <pre>
     * // Single-field mapping
     * PropertyMapping with 1 key field
     * isCompositeKey() returns false
     *
     * // Two-field composite key
     * PropertyMapping with 2 key fields
     * isCompositeKey() returns true
     * </pre>
     *
     * @return true if composite keys are used (size > 1), false otherwise
     */
    public boolean isCompositeKey() {
        return primaryKeyPaths != null && primaryKeyPaths.size() > 1;
    }

    /**
     * Gets the number of fields in the composite key.
     *
     * <p>Returns the count of field pairs that must match for entities to be related.
     * For single-field mappings, returns 1. For Dashboard mappings (which don't use keys),
     * returns 0.</p>
     *
     * Example:
     * <pre>
     * // Single-field mapping
     * PropertyMapping with [User_.id] -&gt; [Order_.userId]
     * getKeyFieldCount() returns 1
     *
     * // Three-field composite key
     * PropertyMapping with:
     *   [Flight_.flightNo, Flight_.date, Flight_.legSequence]
     *   -&gt; [FlightLeg_.flightNo, FlightLeg_.date, FlightLeg_.legSequence]
     * getKeyFieldCount() returns 3
     * </pre>
     *
     * @return the number of key fields, or 0 if no keys are defined
     */
    public int getKeyFieldCount() {
        return primaryKeyPaths != null ? primaryKeyPaths.size() : 0;
    }

    /**
     * Gets the source service for this mapping.
     *
     * <p>
     * The source service provides type-safe access to source entity fields
     * without requiring runtime type lookups.
     * </p>
     *
     * @return the source specification service
     * @since 2.0
     */
    public SpecificationService<?> getSourceService() {
        return sourceService;
    }

    /**
     * Gets the target service for this mapping.
     *
     * <p>
     * The target service provides type-safe access to target entity fields
     * without requiring runtime type lookups.
     * </p>
     *
     * @return the target specification service
     * @since 2.0
     */
    public SpecificationService<T> getTargetService() {
        return targetService;
    }

    /**
     * Gets the source class for this mapping.
     *
     * @return the source entity class
     * @deprecated Use {@link #getSourceService()} instead and call
     *             {@link SpecificationService#getEntityClass()} if needed.
     *             This method will be removed in version 3.0.
     *             <p>
     *             Migration example:
     *             <pre>{@code
     *             // Old (deprecated)
     *             Class<?> sourceClass = mapping.getSourceClass();
     *
     *             // New (recommended)
     *             SpecificationService<?> service = mapping.getSourceService();
     *             Class<?> sourceClass = service.getEntityClass();
     *             }</pre>
     * @since 1.0
     */
    @Deprecated
    public Class<?> getSourceClass() {
        return sourceService != null ? sourceService.getEntityClass() : null;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Specification<?> getSpecification() {
        return specification;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public List<CollectionOperationMetadata<?, ?>> getSourceCollectionOperations() {
        return sourceCollectionOperations;
    }

    public List<CollectionOperationMetadata<?, ?>> getTargetCollectionOperations() {
        return targetCollectionOperations;
    }

    /**
     * Checks if this mapping requires grouping (primary-foreign key based).
     * Dashboard mappings never require grouping.
     *
     * @return true if grouping is required
     */
    public boolean requiresGrouping() {
        if (isForDashboard) {
            return false;
        }
        return primaryKeyPaths != null && !primaryKeyPaths.isEmpty()
                && foreignKeyPaths != null && !foreignKeyPaths.isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
                "PropertyMapping[consumer=%s, targetPath=%s, source=%s, type=%s, aggType=%s]",
                consumerId,
                targetPath,
                dataSourceName,
                mappingType,
                aggregationType
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyMapping<?, ?> that = (PropertyMapping<?, ?>) o;
        return isForDashboard == that.isForDashboard
                && Objects.equals(consumerId, that.consumerId)
                && Objects.equals(targetPath, that.targetPath)
                && Objects.equals(dataSourceName, that.dataSourceName)
                && Objects.equals(sourcePath, that.sourcePath)
                && Objects.equals(sourceCollectionOperations, that.sourceCollectionOperations)
                && Objects.equals(targetCollectionOperations, that.targetCollectionOperations)
                && Objects.equals(primaryKeyPaths, that.primaryKeyPaths)
                && Objects.equals(foreignKeyPaths, that.foreignKeyPaths)
                && mappingType == that.mappingType
                && aggregationType == that.aggregationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerId, isForDashboard, targetPath, dataSourceName,
                sourcePath, sourceCollectionOperations, targetCollectionOperations,
                primaryKeyPaths, foreignKeyPaths, mappingType, aggregationType);
    }

    /**
     * Builder for PropertyMapping.
     *
     * <h2>Version 2.0 Changes:</h2>
     * <p>
     * The builder now requires {@link SpecificationService} instances instead of
     * {@code Class<?>} objects. Use {@link #sourceService(SpecificationService)} and
     * {@link #targetService(SpecificationService)} instead of the deprecated
     * {@code sourceClass()} method.
     * </p>
     *
     * <h2>Example Usage:</h2>
     * <pre>{@code
     * PropertyMapping<User, Integer> mapping = PropertyMapping.<User, Integer>builder()
     *     .consumerId("userStore")
     *     .sourceService(OrderSpecificationService.INSTANCE)
     *     .targetService(UserSpecificationService.INSTANCE)
     *     .dataSourceName("orders")
     *     .sourcePath(Arrays.asList(Order_.amount))
     *     .targetPath(Arrays.asList(User_.totalAmount))
     *     .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
     *     .aggregationType(AggregationType.SUM)
     *     .build();
     * }</pre>
     *
     * @param <T> the target entity type
     * @param <F> the target field type
     * @since 2.0
     */
    public static class Builder<T, F> {

        private String consumerId;
        private boolean isForDashboard;
        private List<MetaAttribute<?, ?>> targetPath;
        private List<MetaAttribute<?, ?>> sourcePath;
        private List<CollectionOperationMetadata<?, ?>> sourceCollectionOperations;
        private List<CollectionOperationMetadata<?, ?>> targetCollectionOperations;
        private List<List<MetaAttribute<?, ?>>> primaryKeyPaths;
        private List<List<MetaAttribute<?, ?>>> foreignKeyPaths;
        private SpecificationService<?> sourceService;
        private SpecificationService<T> targetService;
        private String datasourceName;
        private Specification<?> specification;
        private MappingType mappingType;
        private AggregationType aggregationType;

        public Builder<T, F> consumerId(String consumerId) {
            this.consumerId = consumerId;
            return this;
        }

        public Builder<T, F> isForDashboard(boolean isForDashboard) {
            this.isForDashboard = isForDashboard;
            return this;
        }

        public Builder<T, F> targetPath(List<MetaAttribute<?, ?>> targetPath) {
            this.targetPath = targetPath;
            return this;
        }

        public Builder<T, F> sourcePath(List<MetaAttribute<?, ?>> sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder<T, F> sourceCollectionOperations(List<CollectionOperationMetadata<?, ?>> sourceCollectionOperations) {
            this.sourceCollectionOperations = sourceCollectionOperations;
            return this;
        }

        public Builder<T, F> targetCollectionOperations(List<CollectionOperationMetadata<?, ?>> targetCollectionOperations) {
            this.targetCollectionOperations = targetCollectionOperations;
            return this;
        }

        public Builder<T, F> primaryKeyPaths(List<List<MetaAttribute<?, ?>>> primaryKeyPaths) {
            this.primaryKeyPaths = primaryKeyPaths;
            return this;
        }

        public Builder<T, F> foreignKeyPaths(List<List<MetaAttribute<?, ?>>> foreignKeyPaths) {
            this.foreignKeyPaths = foreignKeyPaths;
            return this;
        }

        /**
         * Sets the source service for this mapping.
         *
         * <p>
         * The source service provides type-safe access to source entity fields
         * and eliminates the need for runtime type lookups.
         * </p>
         *
         * @param sourceService the source entity's specification service
         * @return this builder
         * @since 2.0
         */
        public Builder<T, F> sourceService(SpecificationService<?> sourceService) {
            this.sourceService = sourceService;
            return this;
        }

        /**
         * Sets the target service for this mapping.
         *
         * <p>
         * The target service provides type-safe access to target entity fields
         * and eliminates the need for runtime type lookups.
         * </p>
         *
         * @param targetService the target entity's specification service
         * @return this builder
         * @since 2.0
         */
        public Builder<T, F> targetService(SpecificationService<T> targetService) {
            this.targetService = targetService;
            return this;
        }

        /**
         * Sets the source class for this mapping.
         *
         * @param sourceClass the source entity class
         * @return this builder
         * @deprecated Use {@link #sourceService(SpecificationService)} instead.
         *             This method is deprecated and will be removed in version 3.0.
         *             <p>
         *             Migration example:
         *             <pre>{@code
         *             // Old (deprecated)
         *             .sourceClass(Order.class)
         *
         *             // New (required)
         *             .sourceService(OrderSpecificationService.INSTANCE)
         *             }</pre>
         * @since 1.0
         */
        @Deprecated
        @SuppressWarnings("java:S1133")
        public Builder<T, F> sourceClass(Class<?> sourceClass) {
            // For backward compatibility during migration
            // This will be removed in a future version
            // Note: This doesn't set sourceService, so validation will fail if sourceService is not set
            return this;
        }

        public Builder<T, F> datasourceName(String datasourceName) {
            this.datasourceName = datasourceName;
            return this;
        }

        public Builder<T, F> specification(Specification<?> specification) {
            this.specification = specification;
            return this;
        }

        public Builder<T, F> mappingType(MappingType mappingType) {
            this.mappingType = mappingType;
            return this;
        }

        public Builder<T, F> aggregationType(AggregationType aggregationType) {
            this.aggregationType = aggregationType;
            return this;
        }



        public PropertyMapping<T, F> build() {
            return new PropertyMapping<>(this);
        }
    }
}
