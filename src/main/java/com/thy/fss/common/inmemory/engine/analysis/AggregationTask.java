package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a single aggregation task that computes multiple aggregation types
 * over the same datasource and field path.
 *
 * <p>This class enables the optimization where multiple aggregation types
 * (SUM, AVG, MIN, MAX, COUNT) can be computed in a single loop over the data.</p>
 *
 * <p>Example: If multiple dashboards need SUM, AVG, and MAX of Order.amount,
 * this task will compute all three in one pass.</p>
 * 
 * <p>Specifications are NOT used for grouping tasks. They are applied as filters
 * after index-based lookup during task execution.</p>
 */
public class AggregationTask {

    private final String dataSourceName;
    private final List<MetaAttribute<?, ?>> fieldPath;
    private final Specification<?> specification;

    // Map: AggregationType → List of PropertyMappings that need this aggregation
    private final Map<AggregationType, List<PropertyMapping<?, ?>>> mappingsByAggregationType;

    /**
     * Creates a new AggregationTask without a specification filter.
     * Backward-compatible constructor — equivalent to passing null for specification.
     *
     * @param dataSourceName the datasource name
     * @param fieldPath      the field path to aggregate (can be empty for COUNT aggregations)
     */
    public AggregationTask(
            String dataSourceName,
            List<MetaAttribute<?, ?>> fieldPath) {
        this(dataSourceName, fieldPath, null);
    }

    /**
     * Creates a new AggregationTask with an optional specification filter.
     *
     * @param dataSourceName the datasource name
     * @param fieldPath      the field path to aggregate (can be empty for COUNT aggregations)
     * @param specification  the specification filter (null if no filtering needed)
     */
    public AggregationTask(
            String dataSourceName,
            List<MetaAttribute<?, ?>> fieldPath,
            Specification<?> specification) {
        this.dataSourceName = Objects.requireNonNull(dataSourceName, "Datasource name cannot be null");
        this.fieldPath = fieldPath != null ? fieldPath : Collections.emptyList();
        this.specification = specification;
        this.mappingsByAggregationType = new EnumMap<>(AggregationType.class);
    }

    /**
     * Adds a property mapping to this task.
     *
     * @param aggregationType the aggregation type
     * @param mapping         the property mapping
     */
    public void addMapping(AggregationType aggregationType, PropertyMapping<?, ?> mapping) {
        mappingsByAggregationType
                .computeIfAbsent(aggregationType, k -> new ArrayList<>())
                .add(mapping);
    }

    /**
     * Gets all aggregation types that need to be computed.
     *
     * @return set of aggregation types
     */
    public Set<AggregationType> getAggregationTypes() {
        return mappingsByAggregationType.keySet();
    }

    /**
     * Gets all property mappings for a specific aggregation type.
     *
     * @param aggregationType the aggregation type
     * @return list of property mappings, or empty list if none
     */
    public List<PropertyMapping<?, ?>> getMappings(AggregationType aggregationType) {
        return mappingsByAggregationType.getOrDefault(aggregationType, Collections.emptyList());
    }

    /**
     * Gets all property mappings across all aggregation types.
     *
     * @return list of all property mappings
     */
    public List<PropertyMapping<?, ?>> getAllMappings() {
        List<PropertyMapping<?, ?>> allMappings = new ArrayList<>();
        for (List<PropertyMapping<?, ?>> mappings : mappingsByAggregationType.values()) {
            allMappings.addAll(mappings);
        }
        return allMappings;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public List<MetaAttribute<?, ?>> getFieldPath() {
        return fieldPath;
    }

    /**
     * Gets the specification filter for this task.
     *
     * @return the specification, or null if no filtering is needed
     */
    public Specification<?> getSpecification() {
        return specification;
    }

    public Map<AggregationType, List<PropertyMapping<?, ?>>> getMappingsByAggregationType() {
        return Collections.unmodifiableMap(mappingsByAggregationType);
    }

    /**
     * Checks if this task has any mappings.
     *
     * @return true if there are mappings
     */
    public boolean hasMappings() {
        return !mappingsByAggregationType.isEmpty();
    }

    /**
     * Gets the number of aggregation types in this task.
     *
     * @return the count of aggregation types
     */
    public int getAggregationTypeCount() {
        return mappingsByAggregationType.size();
    }

    @Override
    public String toString() {
        String pathStr = fieldPath != null
                ? fieldPath.stream()
                .map(obj -> String.valueOf(System.identityHashCode(obj)))
                .collect(Collectors.joining("."))
                : "null";
        return String.format("AggregationTask[%s:%s, types=%s]",
                dataSourceName, pathStr, getAggregationTypes());
    }
}
