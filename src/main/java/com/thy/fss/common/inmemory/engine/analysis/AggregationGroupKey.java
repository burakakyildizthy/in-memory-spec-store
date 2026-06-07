package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Key for grouping Store aggregations by datasource, specification, and field path.
 * Used during Store property mapping to identify which aggregations can be
 * computed together.
 *
 * <p>Similar to DataSourceSpecFieldKey but specifically for Store context.</p>
 * <p>Uses path reference equality for comparison (List of MetaAttribute).</p>
 */
public record AggregationGroupKey(String dataSourceName, Specification<?> specification, List<MetaAttribute<?, ?>> fieldPath) {

    /**
     * Creates a new AggregationGroupKey.
     *
     * @param dataSourceName the datasource name
     * @param specification  the specification (can be null)
     * @param fieldPath      the field path (List of MetaAttribute)
     */
    public AggregationGroupKey {
        Objects.requireNonNull(dataSourceName, "Datasource name cannot be null");
        Objects.requireNonNull(fieldPath, "Field path cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregationGroupKey that = (AggregationGroupKey) o;
        
        // Use reference equality for path comparison
        if (!Objects.equals(dataSourceName, that.dataSourceName)) return false;
        if (!Objects.equals(specification, that.specification)) return false;
        if (fieldPath.size() != that.fieldPath.size()) return false;
        
        for (int i = 0; i < fieldPath.size(); i++) {
            if (fieldPath.get(i) != that.fieldPath.get(i)) return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dataSourceName, specification);
        for (Object obj : fieldPath) {
            result = 31 * result + System.identityHashCode(obj);
        }
        return result;
    }

    @Override
    public String toString() {
        String specPart = specification != null ? String.valueOf(specification.hashCode()) : "null";
        String pathStr = fieldPath.stream()
                .map(obj -> String.valueOf(System.identityHashCode(obj)))
                .collect(Collectors.joining("."));
        return String.format("AggregationGroupKey[%s:%s:%s]", dataSourceName, specPart, pathStr);
    }
}
