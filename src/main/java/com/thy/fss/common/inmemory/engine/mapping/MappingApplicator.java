package com.thy.fss.common.inmemory.engine.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.AttributeType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

/**
 * Stateless utility class for applying property mappings to entities.
 *
 * <p>Extracted from {@code DataSynchronizationEngine} to enable shared usage
 * between full sync and streaming pipelines. FK-based entity lookup is
 * abstracted via {@link RelatedEntityLookup}, allowing different data access
 * strategies (DataVersion-backed indexed lookup vs DependencyGraph-backed
 * in-memory matching).</p>
 *
 * <p>All methods are {@code public static} — no instance state is held.</p>
 *
 * @see RelatedEntityLookup
 * @see PropertyMapping
 */
public final class MappingApplicator {

    private static final Logger logger = LoggerFactory.getLogger(MappingApplicator.class);
    private static final String NO_RELATED_ENTITIES_MSG = "No related entities found for key in mapping";

    private MappingApplicator() {
        // Utility class — no instantiation
    }

    // ========================================================================
    // Public API — Mapping Application
    // ========================================================================

    /**
     * Applies property mappings to a single entity.
     * <p>
     * Groups mappings by type (ONE_TO_ONE, COLLECTION, AGGREGATION) and applies
     * each group. Each individual mapping application is wrapped in try-catch
     * to prevent one failure from blocking others.
     *
     * @param lookup   FK-based entity lookup strategy
     * @param rootEntity the target entity to apply mappings to
     * @param mappings list of property mappings to apply
     */
    @SuppressWarnings("unchecked")
    public static void applyMappingsToEntity(
            RelatedEntityLookup lookup,
            Object rootEntity,
            List<PropertyMapping<?, ?>> mappings) {

        logger.trace("Applying {} mapping(s) to entity", mappings.size());

        // Group mappings by type
        List<PropertyMapping<?, ?>> oneToOneMappings = new ArrayList<>();
        List<PropertyMapping<?, ?>> collectionMappings = new ArrayList<>();
        List<PropertyMapping<?, ?>> aggregationMappings = new ArrayList<>();

        for (PropertyMapping<?, ?> mapping : mappings) {
            switch (mapping.getMappingType()) {
                case ONE_TO_ONE:
                    oneToOneMappings.add(mapping);
                    break;
                case MANY_TO_ONE_COLLECTION:
                    collectionMappings.add(mapping);
                    break;
                case MANY_TO_ONE_AGGREGATION:
                    aggregationMappings.add(mapping);
                    break;
            }
        }

        // Apply ONE_TO_ONE mappings
        for (PropertyMapping<?, ?> mapping : oneToOneMappings) {
            try {
                applyOneToOneMapping(lookup, rootEntity, mapping);
            } catch (Exception e) {
                logger.warn("Error applying ONE_TO_ONE mapping {}: {}", mapping, e.getMessage());
            }
        }

        // Apply COLLECTION mappings
        for (PropertyMapping<?, ?> mapping : collectionMappings) {
            try {
                applyCollectionMapping(lookup, rootEntity, mapping);
            } catch (Exception e) {
                logger.warn("Error applying COLLECTION mapping {}: {}", mapping, e.getMessage());
            }
        }

        // Apply AGGREGATION mappings (optimized)
        if (!aggregationMappings.isEmpty()) {
            try {
                applyAggregationMappingsOptimized(lookup, rootEntity, aggregationMappings);
            } catch (Exception e) {
                logger.warn("Error applying AGGREGATION mappings: {}", e.getMessage());
            }
        }
    }

    // ========================================================================
    // ONE_TO_ONE Mapping
    // ========================================================================

    /**
     * Applies a ONE_TO_ONE mapping: looks up a single related entity via FK
     * and copies a source field value to the target entity.
     *
     * @param lookup     FK-based entity lookup strategy
     * @param rootEntity the target entity
     * @param mapping    the ONE_TO_ONE property mapping
     */
    @SuppressWarnings("unchecked")
    public static void applyOneToOneMapping(
            RelatedEntityLookup lookup,
            Object rootEntity,
            PropertyMapping<?, ?> mapping) {

        // Skip if root entity is null
        if (rootEntity == null) {
            logger.trace("Skipping ONE_TO_ONE mapping - root entity is null for targetPath={}",
                    mapping.getTargetPath());
            return;
        }

        try {
            // Get SpecificationService from the mapping (no runtime lookup needed)
            SpecificationService service = mapping.getTargetService();

            // Extract primary key values from target entity
            List<Object> primaryKeyValues = new ArrayList<>();
            for (List<MetaAttribute<?, ?>> pkPath : mapping.getPrimaryKeyPaths()) {
                Object pkValue = service.getValueByPath(rootEntity, pkPath);
                primaryKeyValues.add(pkValue);
            }

            // Skip if any primary key value is null
            if (primaryKeyValues.stream().anyMatch(v -> v == null)) {
                logger.trace("Skipping ONE_TO_ONE mapping - at least one primary key value is null for targetPath={}",
                        mapping.getTargetPath());
                return;
            }

            logger.trace("ONE_TO_ONE mapping: primaryKeyValues={}, targetPath={}",
                    primaryKeyValues, mapping.getTargetPath());

            // Delegate FK lookup to RelatedEntityLookup
            List<?> relatedEntities = lookup.lookupRelatedEntities(mapping, primaryKeyValues);

            if (relatedEntities == null || relatedEntities.isEmpty()) {
                logger.trace(NO_RELATED_ENTITIES_MSG);
                return;
            }

            // Apply specification filter if defined
            if (mapping.getSpecification() != null) {
                relatedEntities = applySpecificationImmutable(
                        (List<Object>) relatedEntities,
                        (Specification<Object>) mapping.getSpecification()
                );
            }

            // Get first entity (ONE_TO_ONE)
            if (!relatedEntities.isEmpty()) {
                Object relatedEntity = relatedEntities.get(0);

                logger.trace("Setting ONE_TO_ONE value: relatedEntity={}, targetPath={}, sourcePath={}",
                        relatedEntity != null ? relatedEntity.getClass().getSimpleName() : "null",
                        mapping.getTargetPath(),
                        mapping.getSourcePath());

                // Extract value from source entity
                Object valueToSet = extractSourceValue(relatedEntity, mapping);

                logger.trace("Extracted value from source: {}", valueToSet);

                // Assign value to target entity
                assignTargetValue(rootEntity, mapping, valueToSet);

                logger.trace("ONE_TO_ONE mapping applied successfully");
            }
        } catch (Exception e) {
            logger.error("Error applying ONE_TO_ONE mapping {}: {}", mapping, e.getMessage(), e);
            throw e;
        }
    }

    // ========================================================================
    // COLLECTION Mapping
    // ========================================================================

    /**
     * Applies a MANY_TO_ONE_COLLECTION mapping: looks up related entities via FK
     * and builds a collection of source field values on the target entity.
     *
     * @param lookup     FK-based entity lookup strategy
     * @param rootEntity the target entity
     * @param mapping    the COLLECTION property mapping
     */
    @SuppressWarnings("unchecked")
    public static void applyCollectionMapping(
            RelatedEntityLookup lookup,
            Object rootEntity,
            PropertyMapping<?, ?> mapping) {

        // Skip if root entity is null
        if (rootEntity == null) {
            logger.trace("Skipping COLLECTION mapping - root entity is null for targetPath={}",
                    mapping.getTargetPath());
            return;
        }

        // Get SpecificationService from the mapping (no runtime lookup needed)
        SpecificationService service = mapping.getTargetService();

        // Extract primary key values from target entity
        List<Object> primaryKeyValues = new ArrayList<>();
        for (List<MetaAttribute<?, ?>> pkPath : mapping.getPrimaryKeyPaths()) {
            Object pkValue = service.getValueByPath(rootEntity, pkPath);
            primaryKeyValues.add(pkValue);
        }

        // Skip if any primary key value is null
        if (primaryKeyValues.stream().anyMatch(v -> v == null)) {
            logger.trace("Skipping COLLECTION mapping - at least one primary key value is null for targetPath={}",
                    mapping.getTargetPath());
            return;
        }

        logger.trace("COLLECTION mapping: primaryKeyValues={}, targetPath={}",
                primaryKeyValues, mapping.getTargetPath());

        // Delegate FK lookup to RelatedEntityLookup
        List<?> relatedEntities = lookup.lookupRelatedEntities(mapping, primaryKeyValues);

        if (relatedEntities == null || relatedEntities.isEmpty()) {
            logger.trace(NO_RELATED_ENTITIES_MSG);
            return;
        }

        // Apply specification filter if defined
        if (mapping.getSpecification() != null) {
            relatedEntities = applySpecificationImmutable(
                    (List<Object>) relatedEntities,
                    (Specification<Object>) mapping.getSpecification()
            );
        }

        // Extract values or copy entities based on sourcePath
        List<Object> valuesToSet;
        if (mapping.getSourcePath() != null && !mapping.getSourcePath().isEmpty()) {
            // Extract specific field values from related entities
            valuesToSet = new ArrayList<>();
            for (Object relatedEntity : relatedEntities) {
                // Use extractSourceValue to handle collection operations
                Object value = extractSourceValue(relatedEntity, mapping);
                valuesToSet.add(value);
            }

            logger.trace("Extracted {} values from source path for collection", valuesToSet.size());
        } else {
            // No source path specified, use original entity references
            valuesToSet = new ArrayList<>(relatedEntities);
        }

        // For COLLECTION mappings, set the collection directly using setFieldValue
        // instead of going through setValueByPath which would trigger handleCollectionField
        // and wrap the list inside another list (collection.add(entireList) → [[items]])
        List<MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
        if (targetPath.size() == 1 && targetPath.get(0).getAttributeType() == AttributeType.COLLECTION) {
            // Direct field set — bypasses handleCollectionField wrapping
            service.setFieldValue(rootEntity, targetPath.get(0), new ArrayList<>(valuesToSet));
        } else {
            // Multi-level path or non-collection — use standard assignment
            assignTargetValue(rootEntity, mapping, valuesToSet);
        }
    }

    // ========================================================================
    // AGGREGATION Mapping (Optimized)
    // ========================================================================

    /**
     * Applies MANY_TO_ONE_AGGREGATION mappings with optimization: groups mappings
     * by (datasource + specification + field) and calculates multiple aggregations
     * in a single loop over related entities.
     *
     * @param lookup              FK-based entity lookup strategy
     * @param rootEntity          the target entity
     * @param aggregationMappings list of aggregation property mappings
     */
    @SuppressWarnings("unchecked")
    public static void applyAggregationMappingsOptimized(
            RelatedEntityLookup lookup,
            Object rootEntity,
            List<PropertyMapping<?, ?>> aggregationMappings) {

        // Skip if root entity is null
        if (rootEntity == null) {
            logger.trace("Skipping aggregation mappings - root entity is null");
            return;
        }

        logger.trace("Applying {} aggregation mapping(s) with optimization", aggregationMappings.size());

        // Group mappings by (datasource + specification + field)
        Map<String, List<PropertyMapping<?, ?>>> mappingsByField = new HashMap<>();

        for (PropertyMapping<?, ?> mapping : aggregationMappings) {
            String dataSourceName = mapping.getDataSourceName();
            Specification<?> specification = mapping.getSpecification();
            List<MetaAttribute<?, ?>> sourcePath = mapping.getSourcePath();
            MetaAttribute<?, ?> field
                    = sourcePath != null && !sourcePath.isEmpty() ? sourcePath.get(sourcePath.size() - 1) : null;

            // Create unique key for this combination (using identity hash instead of specification key)
            String specPart = specification != null ? String.valueOf(System.identityHashCode(specification)) : "null";
            String fieldKey = String.format("%s:%s:%s", dataSourceName, specPart, field != null ? field.getName() : "null");

            mappingsByField.computeIfAbsent(fieldKey, k -> new ArrayList<>()).add(mapping);
        }

        logger.trace("Grouped into {} unique field group(s)", mappingsByField.size());

        // Process each field group
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : mappingsByField.entrySet()) {
            String fieldKey = entry.getKey();
            List<PropertyMapping<?, ?>> fieldMappings = entry.getValue();

            try {
                // Get the first mapping to extract common properties
                PropertyMapping<?, ?> firstMapping = fieldMappings.get(0);

                // Get SpecificationService from the mapping (no runtime lookup needed)
                SpecificationService service = firstMapping.getTargetService();

                // Extract primary key values from target entity
                List<Object> primaryKeyValues = new ArrayList<>();
                for (List<MetaAttribute<?, ?>> pkPath : firstMapping.getPrimaryKeyPaths()) {
                    Object pkValue = service.getValueByPath(rootEntity, pkPath);
                    primaryKeyValues.add(pkValue);
                }

                // Skip if any primary key value is null
                if (primaryKeyValues.stream().anyMatch(v -> v == null)) {
                    logger.trace("Skipping AGGREGATION mappings for field group '{}' - at least one primary key value is null",
                            fieldKey);
                    continue;
                }

                logger.trace("AGGREGATION mapping: primaryKeyValues={}, fieldKey={}",
                        primaryKeyValues, fieldKey);

                // Delegate FK lookup to RelatedEntityLookup
                List<?> relatedEntities = lookup.lookupRelatedEntities(firstMapping, primaryKeyValues);

                if (relatedEntities == null || relatedEntities.isEmpty()) {
                    logger.trace(NO_RELATED_ENTITIES_MSG);
                    continue;
                }

                // Apply specification filter if defined
                if (firstMapping.getSpecification() != null) {
                    relatedEntities = applySpecificationImmutable(
                            (List<Object>) relatedEntities,
                            (Specification<Object>) firstMapping.getSpecification()
                    );
                }

                // Calculate multiple aggregations in single loop
                List<MetaAttribute<?, ?>> sourcePath = firstMapping.getSourcePath();
                MetaAttribute<?, ?> sourceField
                        = sourcePath != null && !sourcePath.isEmpty() ? sourcePath.get(sourcePath.size() - 1) : null;

                Map<AggregationType, Object> results = calculateMultipleAggregationsForSingleEntity(
                        relatedEntities,
                        sourceField,
                        fieldMappings,
                        firstMapping.getSourceService()
                );

                // Write results to target fields
                for (PropertyMapping<?, ?> mapping : fieldMappings) {
                    AggregationType aggType = mapping.getAggregationType();
                    Object result = results.get(aggType);

                    logger.trace("Setting aggregation result: type={}, value={}, targetPath={}",
                            aggType, result, mapping.getTargetPath());

                    if (result != null) {
                        // Use assignTargetValue to handle collection operations and type conversion
                        assignTargetValue(rootEntity, mapping, result);
                    } else {
                        logger.warn("Aggregation result is null for type={}, targetPath={}",
                                aggType, mapping.getTargetPath());
                    }
                }

                logger.trace("Applied {} aggregation(s) for field group '{}'", fieldMappings.size(), fieldKey);

            } catch (Exception e) {
                logger.error("Error applying aggregations for field group '{}': {}", fieldKey, e.getMessage(), e);
            }
        }
    }

    // ========================================================================
    // Value Extraction & Assignment
    // ========================================================================

    /**
     * Extracts a value from a source entity using the mapping's source path.
     *
     * @param sourceEntity the source entity to extract from
     * @param mapping      the property mapping defining the source path
     * @return the extracted value, or null if source entity is null
     */
    @SuppressWarnings("unchecked")
    public static Object extractSourceValue(
            Object sourceEntity,
            PropertyMapping<?, ?> mapping) {

        if (sourceEntity == null) {
            return null;
        }

        List<MetaAttribute<?, ?>> sourcePath = mapping.getSourcePath();
        if (sourcePath == null || sourcePath.isEmpty()) {
            // No source path specified, return the entire entity
            return sourceEntity;
        }

        List<CollectionOperationMetadata<?, ?>> sourceCollectionOps
                = mapping.getSourceCollectionOperations();

        // Get SpecificationService from the mapping (no runtime lookup needed)
        SpecificationService service = mapping.getSourceService();

        // Delegate to SpecificationService - it handles:
        // - Regular field navigation via getFieldValue()
        // - Collection operations via extractFromCollection()
        // - Specification filtering via validateSpecification()
        // - Null-safe navigation
        if (sourceCollectionOps != null && !sourceCollectionOps.isEmpty()) {
            return service.getValueByPathWithCollections(sourceEntity, sourcePath, sourceCollectionOps);
        } else {
            return service.getValueByPath(sourceEntity, sourcePath);
        }
    }

    /**
     * Assigns a value to a target entity using the mapping's target path.
     *
     * @param targetEntity the target entity to assign to
     * @param mapping      the property mapping defining the target path
     * @param value        the value to assign
     */
    @SuppressWarnings("unchecked")
    public static void assignTargetValue(
            Object targetEntity,
            PropertyMapping<?, ?> mapping,
            Object value) {

        if (targetEntity == null) {
            throw new IllegalArgumentException("Target entity cannot be null");
        }

        List<MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
        if (targetPath == null || targetPath.isEmpty()) {
            throw new IllegalArgumentException("Target path cannot be null or empty");
        }

        List<CollectionOperationMetadata<?, ?>> targetCollectionOps
                = mapping.getTargetCollectionOperations();

        // Get SpecificationService from the mapping (no runtime lookup needed)
        SpecificationService service = mapping.getTargetService();

        // Convert value to target field type if needed (for aggregation results)
        Object convertedValue = convertValueToTargetType(value, targetPath, service, targetEntity);

        // Delegate to SpecificationService - it handles:
        // - Regular field assignment via setFieldValue()
        // - Collection operations for target assignment
        // - Null-safe navigation with descriptive errors
        if (targetCollectionOps != null && !targetCollectionOps.isEmpty()) {
            service.setValueByPathWithCollections(targetEntity, targetPath, targetCollectionOps, convertedValue);
        } else {
            service.setValueByPath(targetEntity, targetPath, convertedValue);
        }
    }

    /**
     * Converts a value to the target field type if needed (common for aggregation results).
     *
     * @param value        the value to convert
     * @param targetPath   the target path (last element determines target type)
     * @param service      the specification service
     * @param targetEntity the target entity (for context)
     * @return the converted value, or original if no conversion needed
     */
    public static Object convertValueToTargetType(
            Object value,
            List<MetaAttribute<?, ?>> targetPath,
            SpecificationService service,
            Object targetEntity) {

        if (value == null) {
            return null;
        }

        try {
            // Get target field type
            MetaAttribute<?, ?> targetField = targetPath.get(targetPath.size() - 1);
            Class<?> targetType = targetField.getFieldType();

            // If value is already correct type, return as-is
            if (targetType.isAssignableFrom(value.getClass())) {
                return value;
            }

            // Handle numeric conversions (common for aggregation results)
            if (value instanceof Number) {
                Number numValue = (Number) value;

                if (targetType == Integer.class || targetType == int.class) {
                    return numValue.intValue();
                } else if (targetType == Long.class || targetType == long.class) {
                    return numValue.longValue();
                } else if (targetType == Double.class || targetType == double.class) {
                    return numValue.doubleValue();
                } else if (targetType == Float.class || targetType == float.class) {
                    return numValue.floatValue();
                } else if (targetType == Short.class || targetType == short.class) {
                    return numValue.shortValue();
                } else if (targetType == Byte.class || targetType == byte.class) {
                    return numValue.byteValue();
                }
            }

            // If no conversion available, return original value
            // SpecificationService will handle the error if types are incompatible
            return value;

        } catch (Exception e) {
            // If conversion fails, return original value
            // SpecificationService will handle the error
            logger.debug("Could not convert value type, returning original: {}", e.getMessage());
            return value;
        }
    }

    // ========================================================================
    // Aggregation Calculation
    // ========================================================================

    /**
     * Calculates multiple aggregations for a single entity's related entities.
     * Handles COUNT separately (doesn't need field values), then delegates
     * numeric aggregations to {@link #calculateMultipleNumericAggregations}.
     *
     * @param relatedEntities the related entities to aggregate over
     * @param sourceField     the source field for numeric aggregations (may be null for COUNT-only)
     * @param mappings        the aggregation mappings defining which types are needed
     * @param sourceService   the specification service for the source entity type
     * @return map of aggregation type to calculated result
     */
    public static Map<AggregationType, Object> calculateMultipleAggregationsForSingleEntity(
            List<?> relatedEntities,
            MetaAttribute<?, ?> sourceField,
            List<PropertyMapping<?, ?>> mappings,
            SpecificationService<?> sourceService) {

        logger.trace("Calculating multiple aggregations for {} entities", relatedEntities.size());

        Map<AggregationType, Object> results = new HashMap<>();

        // Determine which aggregation types are needed
        Set<AggregationType> neededTypes = new HashSet<>();
        for (PropertyMapping<?, ?> mapping : mappings) {
            AggregationType aggType = mapping.getAggregationType();
            logger.trace("Mapping aggregationType: {}", aggType);
            neededTypes.add(aggType);
        }

        logger.trace("Needed aggregation types: {}", neededTypes);

        // Handle COUNT separately (doesn't need field values)
        if (neededTypes.contains(AggregationType.COUNT)) {
            int countValue = relatedEntities.size();
            results.put(AggregationType.COUNT, countValue);
            logger.trace("COUNT aggregation calculated: {}", countValue);
            neededTypes.remove(AggregationType.COUNT);
        }

        // Handle numeric aggregations (SUM, AVG, MIN, MAX)
        if (!neededTypes.isEmpty()) {
            Map<AggregationType, Object> numericResults = calculateMultipleNumericAggregations(
                    relatedEntities,
                    sourceField,
                    neededTypes,
                    sourceService
            );
            results.putAll(numericResults);
        }

        return results;
    }

    /**
     * Calculates multiple numeric aggregations (SUM, AVG, MIN, MAX) in a single
     * loop over the related entities for efficiency.
     *
     * @param relatedEntities the related entities to aggregate over
     * @param sourceField     the source field to extract numeric values from
     * @param neededTypes     the set of aggregation types to calculate
     * @param sourceService   the specification service for the source entity type
     * @return map of aggregation type to calculated result
     */
    @SuppressWarnings("unchecked")
    public static Map<AggregationType, Object> calculateMultipleNumericAggregations(
            List<?> relatedEntities,
            MetaAttribute<?, ?> sourceField,
            Set<AggregationType> neededTypes,
            SpecificationService<?> sourceService) {

        logger.trace("Calculating numeric aggregations for {} entities: {}", relatedEntities.size(), neededTypes);

        Map<AggregationType, Object> results = new HashMap<>();

        if (relatedEntities.isEmpty()) {
            // Return appropriate zero/null values for empty collections
            for (AggregationType type : neededTypes) {
                switch (type) {
                    case COUNT:
                    case SUM:
                        results.put(type, 0);
                        break;
                    case AVG:
                    case MIN:
                    case MAX:
                        results.put(type, null);
                        break;
                }
            }
            return results;
        }

        // Initialize accumulators
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int count = 0;

        // Single loop to calculate all aggregations
        for (Object entity : relatedEntities) {
            // Skip null entities
            if (entity == null) {
                continue;
            }

            try {
                // Get field value using SpecificationService from parameter
                @SuppressWarnings("unchecked")
                SpecificationService<Object> typedService = (SpecificationService<Object>) sourceService;
                Object fieldValue = typedService.getFieldValue(entity, sourceField.getName());

                if (fieldValue == null) {
                    continue; // Skip null values
                }

                // Convert to double for calculation
                double value;
                if (fieldValue instanceof Number) {
                    value = ((Number) fieldValue).doubleValue();
                } else {
                    throw new IllegalArgumentException("Field value is not numeric: " + fieldValue + " fieldName: " + sourceField.getName());
                }

                // Update accumulators
                if (neededTypes.contains(AggregationType.SUM) || neededTypes.contains(AggregationType.AVG)) {
                    sum += value;
                }

                if (neededTypes.contains(AggregationType.MIN)) {
                    min = Math.min(min, value);
                }

                if (neededTypes.contains(AggregationType.MAX)) {
                    max = Math.max(max, value);
                }

                count++;

            } catch (Exception e) {
                logger.warn("Error getting field value from entity: {}", e.getMessage());
            }
        }

        // Store results
        if (neededTypes.contains(AggregationType.SUM)) {
            results.put(AggregationType.SUM, sum);
        }

        if (neededTypes.contains(AggregationType.AVG)) {
            results.put(AggregationType.AVG, count > 0 ? sum / count : null);
        }

        if (neededTypes.contains(AggregationType.MIN)) {
            results.put(AggregationType.MIN, count > 0 ? min : null);
        }

        if (neededTypes.contains(AggregationType.MAX)) {
            results.put(AggregationType.MAX, count > 0 ? max : null);
        }

        logger.trace("Calculated aggregations: {}", results);
        return results;
    }

    // ========================================================================
    // Specification Filter (Private Helper)
    // ========================================================================

    /**
     * Applies a specification filter to a list of entities, returning a new
     * filtered list (immutable operation — original list is not modified).
     *
     * @param data          the list of entities to filter
     * @param specification the specification to apply
     * @param <T>           the entity type
     * @return a new list containing only entities matching the specification
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> applySpecificationImmutable(
            List<T> data,
            Specification<T> specification) {

        // If no specification, return original list
        if (specification == null) {
            logger.debug("No specification provided, returning original list");
            return data;
        }

        // If data is empty, return empty list
        if (data == null || data.isEmpty()) {
            logger.debug("Data is empty, returning empty list");
            return new ArrayList<>();
        }

        logger.debug("Applying specification filter to {} items...", data.size());

        // Get entity class from first non-null item
        T firstItem = null;
        for (T item : data) {
            if (item != null) {
                firstItem = item;
                break;
            }
        }

        if (firstItem == null) {
            logger.debug("All items are null, returning empty list");
            return new ArrayList<>();
        }

        // Create new filtered list
        List<T> filteredData = new ArrayList<>();

        // Filter using Specification.test() (no reflection)
        for (T entity : data) {
            if (specification.test(entity)) {
                filteredData.add(entity); // Add reference (shallow copy of list)
            }
        }

        logger.debug("Filtered {} items to {} items", data.size(), filteredData.size());

        return filteredData;
    }
}
