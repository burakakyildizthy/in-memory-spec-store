package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Key for grouping Dashboard aggregations by datasource, specification, and field path.
 * Does NOT include aggregationType - used to group multiple aggregation types together.
 *
 * <p>This key is used during analysis phase to identify which aggregations can be
 * computed together in a single loop.</p>
 * <p>Uses path reference equality for comparison (List of MetaAttribute).</p>
 * <p>Mappings with different specifications are grouped into separate tasks so that
 * specification filters can be applied correctly during aggregation computation.</p>
 */
public record DataSourceSpecFieldKey(String dataSourceName, List<MetaAttribute<?, ?>> fieldPath,
                                     Specification<?> specification) {

    /**
     * Creates a new DataSourceSpecFieldKey.
     *
     * @param dataSourceName the datasource name
     * @param fieldPath      the field path (List of MetaAttribute)
     * @param specification  the specification filter (null if no filtering needed)
     */
    public DataSourceSpecFieldKey {
        Objects.requireNonNull(dataSourceName, "Datasource name cannot be null");
        Objects.requireNonNull(fieldPath, "Field path cannot be null");
    }

    /**
     * Backward-compatible constructor without specification.
     *
     * @param dataSourceName the datasource name
     * @param fieldPath      the field path (List of MetaAttribute)
     */
    public DataSourceSpecFieldKey(String dataSourceName, List<MetaAttribute<?, ?>> fieldPath) {
        this(dataSourceName, fieldPath, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceSpecFieldKey that = (DataSourceSpecFieldKey) o;
        
        // Use reference equality for path comparison
        if (!Objects.equals(dataSourceName, that.dataSourceName)) return false;
        if (fieldPath.size() != that.fieldPath.size()) return false;
        
        for (int i = 0; i < fieldPath.size(); i++) {
            if (fieldPath.get(i) != that.fieldPath.get(i)) return false;
        }

        // Specification comparison: both null or same reference
        if (specification != that.specification) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dataSourceName);
        for (Object obj : fieldPath) {
            result = 31 * result + System.identityHashCode(obj);
        }
        result = 31 * result + (specification != null ? System.identityHashCode(specification) : 0);
        return result;
    }

    @Override
    public String toString() {
        String pathStr = fieldPath.stream()
                .map(obj -> String.valueOf(System.identityHashCode(obj)))
                .collect(Collectors.joining("."));
        String specStr = specification != null ? String.valueOf(System.identityHashCode(specification)) : "null";
        return String.format("DataSourceSpecFieldKey[%s:%s:spec=%s]", dataSourceName, pathStr, specStr);
    }
}
