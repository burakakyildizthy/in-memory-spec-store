package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a navigation path through entity fields.
 * Pure data structure with no builder dependencies.
 * Used to represent target paths, source paths, primary key paths, and foreign key paths.
 * 
 * <p>This class is immutable and thread-safe.</p>
 * 
 * <p>Example paths:</p>
 * <pre>
 * // Simple path: User.id
 * PropertyNavigation(path=[User_.id], collectionOps=[])
 * 
 * // Nested path: User.address.cityId
 * PropertyNavigation(path=[User_.address, Address_.cityId], collectionOps=[])
 * 
 * // Collection path: User.orders.first().amount
 * PropertyNavigation(
 *   path=[User_.orders, Order_.amount],
 *   collectionOps=[CollectionOperationMetadata(pathIndex=0, selector=FIRST)]
 * )
 * </pre>
 */
public class PropertyNavigation {
    
    private final List<MetaAttribute<?, ?>> path;
    private final List<CollectionOperationMetadata<?, ?>> collectionOperations;
    
    /**
     * Creates a new PropertyNavigation.
     * 
     * @param path the field path (cannot be null or empty)
     * @param collectionOperations the collection operations metadata (can be null or empty)
     * @throws IllegalArgumentException if path is null or empty
     */
    public PropertyNavigation(
            List<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        
        // Validate: path cannot be empty
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
        this.collectionOperations = collectionOperations != null 
            ? Collections.unmodifiableList(new ArrayList<>(collectionOperations))
            : Collections.emptyList();
    }
    
    /**
     * Gets the field path.
     * 
     * @return immutable list of meta attributes representing the field path
     */
    public List<MetaAttribute<?, ?>> getPath() {
        return path;
    }
    
    /**
     * Gets the collection operations metadata.
     * 
     * @return immutable list of collection operation metadata
     */
    public List<CollectionOperationMetadata<?, ?>> getCollectionOperations() {
        return collectionOperations;
    }
    
    /**
     * Returns the root entity class (first attribute's owner type).
     * Never returns null because path cannot be empty.
     * 
     * @return the root entity class
     */
    public Class<?> getRootClass() {
        return path.get(0).getOwnerType();
    }
    
    /**
     * Returns the leaf field class (last attribute's field type).
     * Never returns null because path cannot be empty.
     * 
     * @return the leaf field class
     */
    public Class<?> getLeafClass() {
        return path.get(path.size() - 1).getFieldType();
    }
    
    /**
     * Returns the path depth (number of fields in the path).
     * 
     * @return the path depth (always >= 1)
     */
    public int getDepth() {
        return path.size();
    }
    
    /**
     * Checks if this navigation involves any collection operations.
     * 
     * @return true if there are collection operations, false otherwise
     */
    public boolean hasCollectionOperations() {
        return !collectionOperations.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyNavigation that = (PropertyNavigation) o;
        return Objects.equals(path, that.path) 
            && Objects.equals(collectionOperations, that.collectionOperations);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path, collectionOperations);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PropertyNavigation{path=");
        
        // Build path string
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(".");
            }
            MetaAttribute<?, ?> attr = path.get(i);
            sb.append(attr.getOwnerType().getSimpleName())
              .append(".")
              .append(attr.getName());
        }
        
        // Add collection operations if any
        if (!collectionOperations.isEmpty()) {
            sb.append(", collectionOps=").append(collectionOperations);
        }
        
        sb.append("}");
        return sb.toString();
    }
}
