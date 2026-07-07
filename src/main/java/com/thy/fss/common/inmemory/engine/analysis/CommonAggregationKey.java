package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Key for storing common aggregation results in DataVersion.
 * Used by Dashboard to identify and retrieve pre-computed aggregation results.
 *
 * <p>Format: datasource + specification + fieldPath + aggregationType</p>
 * <p>Multiple dashboards can share the same aggregation result if they have
 * the same datasource, specification, field path, and aggregation type.</p>
 * <p>Uses path reference equality for comparison (List of MetaAttribute).</p>
 */
public record CommonAggregationKey(String dataSourceName, Specification<?> specification, List<MetaAttribute<?, ?>> fieldPath,
                                   AggregationType aggregationType) {

    /**
     * Creates a new CommonAggregationKey.
     *
     * @param dataSourceName  the datasource name
     * @param specification   the specification (can be null)
     * @param fieldPath       the field path to aggregate (can be empty for COUNT aggregations)
     * @param aggregationType the aggregation type
     */
    public CommonAggregationKey {
        Objects.requireNonNull(dataSourceName, "Datasource name cannot be null");
        Objects.requireNonNull(aggregationType, "Aggregation type cannot be null");
        if (fieldPath == null) {
            fieldPath = java.util.Collections.emptyList();
        }
    }

    /**
     * Converts this key to a storage key string for use in DataVersion.commonAggregationResults.
     * Format: "datasource:specHash:fieldPath:aggType"
     * Path representation uses System.identityHashCode for reference equality.
     *
     * @return the storage key string
     */
    public String toStorageKey() {
        String specPart = specification != null ? String.valueOf(specification.hashCode()) : "null";
        String fieldPart = fieldPath != null
                ? fieldPath.stream()
                .map(obj -> String.valueOf(System.identityHashCode(obj)))
                .collect(Collectors.joining("."))
                : "null";
        return String.format("%s:%s:%s:%s", dataSourceName, specPart, fieldPart, aggregationType.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommonAggregationKey that = (CommonAggregationKey) o;

        // Use reference equality for path comparison
        if (!Objects.equals(dataSourceName, that.dataSourceName)) return false;
        if (!Objects.equals(specification, that.specification)) return false;
        if (aggregationType != that.aggregationType) return false;

        if (fieldPath == null && that.fieldPath == null) return true;
        if (fieldPath == null || that.fieldPath == null) return false;
        if (fieldPath.size() != that.fieldPath.size()) return false;

        for (int i = 0; i < fieldPath.size(); i++) {
            if (fieldPath.get(i) != that.fieldPath.get(i)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dataSourceName, specification, aggregationType);
        if (fieldPath != null) {
            for (Object obj : fieldPath) {
                result = 31 * result + System.identityHashCode(obj);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("CommonAggregationKey[%s]", toStorageKey());
    }
}
