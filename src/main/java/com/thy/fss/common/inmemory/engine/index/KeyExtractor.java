package com.thy.fss.common.inmemory.engine.index;

/**
 * Functional interface for extracting key values from entities.
 * Used by the index infrastructure to extract comparable key values
 * from entities without using reflection or equals() methods.
 *
 * @param <T> The entity type
 * @param <V> The key value type (must be Comparable)
 */
@FunctionalInterface
public interface KeyExtractor<T, V extends Comparable<V>> {
    
    /**
     * Extracts a key value from the given entity.
     *
     * @param entity The entity to extract the key from
     * @return The extracted key value, or null if the field is null
     */
    V extract(T entity);
}
