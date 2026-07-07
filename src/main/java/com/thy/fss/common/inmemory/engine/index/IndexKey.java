package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Composite key used for index cache lookup.
 * Combines datasource name and key field names to uniquely identify an index.
 * This class is immutable and thread-safe.
 * 
 * <p>Supports both single-field indexes (list of field names) and composite key
 * indexes (list of field path strings).</p>
 */
public final class IndexKey {
    
    private final String datasourceName;
    private final List<String> keyFieldNames;
    private final int hashCode;
    
    /**
     * Creates a new IndexKey.
     *
     * @param datasourceName The name of the datasource
     * @param keyFieldNames  The list of key field names (in order)
     */
    public IndexKey(String datasourceName, List<String> keyFieldNames) {
        this.datasourceName = Objects.requireNonNull(datasourceName, "datasourceName cannot be null");
        this.keyFieldNames = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(keyFieldNames, "keyFieldNames cannot be null")
        ));
        this.hashCode = computeHashCode();
    }
    
    /**
     * Gets the datasource name.
     *
     * @return The datasource name
     */
    public String getDatasourceName() {
        return datasourceName;
    }
    
    /**
     * Gets the key field names.
     *
     * @return An unmodifiable list of key field names
     */
    public List<String> getKeyFieldNames() {
        return keyFieldNames;
    }
    
    private int computeHashCode() {
        return Objects.hash(datasourceName, keyFieldNames);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        IndexKey other = (IndexKey) obj;
        return datasourceName.equals(other.datasourceName) &&
               keyFieldNames.equals(other.keyFieldNames);
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public String toString() {
        return String.format("IndexKey{datasource='%s', fields=%s}", 
            datasourceName, keyFieldNames);
    }
    
    /**
     * Creates an IndexKey from composite key paths.
     * Each path is converted to a dot-separated string representation.
     * 
     * <p>For example, a path [User_.id, User_.name] becomes "id.name"</p>
     *
     * @param dataSourceName The name of the data source
     * @param keyPaths       The list of key field paths
     * @return A new IndexKey
     * @throws NullPointerException if any parameter is null
     */
    public static IndexKey fromCompositeKeyPaths(
            String dataSourceName,
            List<List<MetaAttribute<?, ?>>> keyPaths) {
        
        Objects.requireNonNull(dataSourceName, "dataSourceName cannot be null");
        Objects.requireNonNull(keyPaths, "keyPaths cannot be null");
        
        // Convert each path to a dot-separated string
        List<String> fieldNames = keyPaths.stream()
            .map(IndexKey::pathToString)
            .collect(Collectors.toList());
        
        return new IndexKey(dataSourceName, fieldNames);
    }
    
    /**
     * Converts a path of MetaAttributes to a dot-separated string.
     * 
     * @param path The path of MetaAttributes
     * @return A dot-separated string representation
     */
    private static String pathToString(List<MetaAttribute<?, ?>> path) {
        return path.stream()
            .map(MetaAttribute::getName)
            .collect(Collectors.joining("."));
    }
}
