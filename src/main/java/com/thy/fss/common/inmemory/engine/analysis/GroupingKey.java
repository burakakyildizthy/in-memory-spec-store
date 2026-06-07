package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.List;
import java.util.Objects;

/**
 * Key for Store primary-foreign key grouping.
 * Used to identify and cache grouped data based on primary-foreign key relationships.
 *
 * <p>Format: datasource + primaryKeyPath + foreignKeyPath</p>
 * <p>Multiple Store mappings can share the same grouped data if they have
 * the same datasource and primary-foreign key relationship.</p>
 * <p>Uses path reference equality for comparison (List of MetaAttribute).</p>
 */
public record GroupingKey(String dataSourceName, List<MetaAttribute<?, ?>> primaryKeyPath, List<MetaAttribute<?, ?>> foreignKeyPath) {


    /**
     * Creates a new GroupingKey with single MetaAttribute primary and foreign keys.
     *
     * @param dataSourceName the datasource name
     * @param primaryKey     the primary key MetaAttribute
     * @param foreignKey     the foreign key MetaAttribute
     */
    public GroupingKey(
            String dataSourceName,
            MetaAttribute<?, ?> primaryKey,
            MetaAttribute<?, ?> foreignKey) {
        this(dataSourceName, List.of(primaryKey), List.of(foreignKey));
    }

    /**
     * Creates a new GroupingKey.
     *
     * @param dataSourceName the datasource name
     * @param primaryKeyPath the primary key path (List of MetaAttribute)
     * @param foreignKeyPath the foreign key path (List of MetaAttribute)
     */
    public GroupingKey {
        Objects.requireNonNull(dataSourceName, "Datasource name cannot be null");
        Objects.requireNonNull(primaryKeyPath, "Primary key path cannot be null");
        Objects.requireNonNull(foreignKeyPath, "Foreign key path cannot be null");
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupingKey that = (GroupingKey) o;

        // Use reference equality for path comparison
        if (!Objects.equals(dataSourceName, that.dataSourceName)) return false;
        if (primaryKeyPath.size() != that.primaryKeyPath.size()) return false;
        if (foreignKeyPath.size() != that.foreignKeyPath.size()) return false;

        for (int i = 0; i < primaryKeyPath.size(); i++) {
            if (primaryKeyPath.get(i) != that.primaryKeyPath.get(i)) return false;
        }

        for (int i = 0; i < foreignKeyPath.size(); i++) {
            if (foreignKeyPath.get(i) != that.foreignKeyPath.get(i)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(dataSourceName);
        for (Object obj : primaryKeyPath) {
            result = 31 * result + System.identityHashCode(obj);
        }
        for (Object obj : foreignKeyPath) {
            result = 31 * result + System.identityHashCode(obj);
        }
        return result;
    }

    @Override
    public String toString() {
        String primaryPathStr = primaryKeyPath.stream()
                .map(MetaAttribute::getName)
                .collect(java.util.stream.Collectors.joining("."));
        String foreignPathStr = foreignKeyPath.stream()
                .map(MetaAttribute::getName)
                .collect(java.util.stream.Collectors.joining("."));
        return String.format("GroupingKey[%s:%s:%s]", dataSourceName, primaryPathStr, foreignPathStr);
    }
}
