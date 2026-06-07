package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.exception.InMemoryDataStoreException;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.*;

/**
 * Index structure for efficient composite key lookups using nested maps.
 * 
 * <p>This index supports composite keys with multiple fields, where all fields
 * must match for a successful lookup. The index uses a nested HashMap structure
 * where each level represents one field in the composite key.</p>
 * 
 * <h2>Algorithm Overview</h2>
 * 
 * <p>The CompositeKeyIndex uses a nested map structure for O(1) average-case lookup
 * per field. For a composite key with N fields, the structure has N levels of nesting,
 * with entity lists stored at the deepest level.</p>
 * 
 * Data Structure
 * <p>For a 2-field composite key (userId, regionId), the structure is:</p>
 * <pre>
 * Map&lt;userId, Map&lt;regionId, List&lt;Entity&gt;&gt;&gt;
 * 
 * Example with data:
 * {
 *   1L: {
 *     "US": [entity1, entity2],
 *     "EU": [entity3]
 *   },
 *   2L: {
 *     "US": [entity4]
 *   }
 * }
 * </pre>
 * 
 * <p>For a 3-field composite key (flightNo, date, legSequence), the structure is:</p>
 * <pre>
 * Map&lt;flightNo, Map&lt;date, Map&lt;legSequence, List&lt;Entity&gt;&gt;&gt;&gt;
 * 
 * Example with data:
 * {
 *   "TK123": {
 *     "2024-01-15": {
 *       1: [leg1],
 *       2: [leg2]
 *     }
 *   }
 * }
 * </pre>
 * 
 * Index Building Algorithm
 * <p>The {@link #buildIndex(Collection)} method constructs the nested map structure:</p>
 * <ol>
 * <li>For each entity, extract all key field values by navigating the key paths</li>
 * <li>Recursively add the entity to the nested map structure:
 *   <ul>
 *   <li>At intermediate levels: create or retrieve nested maps</li>
 *   <li>At the final level: add entity to the list</li>
 *   </ul>
 * </li>
 * </ol>
 * 
 * <p><b>Time Complexity:</b> O(N * K) where N is the number of entities and K is the number of key fields</p>
 * <p><b>Space Complexity:</b> O(N * K) for storing the nested map structure</p>
 * 
 * Lookup Algorithm
 * <p>The {@link #lookup(List)} method retrieves entities matching a composite key:</p>
 * <ol>
 * <li>Start at the root map with the first key value</li>
 * <li>Recursively navigate through nested maps using each key value</li>
 * <li>At the final level, return the entity list</li>
 * <li>If any key value is not found, return empty list</li>
 * </ol>
 * 
 * <p><b>Time Complexity:</b> O(K) where K is the number of key fields (constant time per field)</p>
 * <p><b>Space Complexity:</b> O(1) for the lookup operation itself</p>
 * 
 * Null Key Values
 * <p>Null key values are treated as valid keys and can be used for matching.
 * This allows entities with null key fields to be indexed and retrieved.</p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * Example 1: Two-Field Composite Key
 * <pre>
 * // Define key paths
 * List&lt;List&lt;MetaAttribute&lt;?, ?&gt;&gt;&gt; keyPaths = List.of(
 *     List.of(Order_.userId),
 *     List.of(Order_.regionId)
 * );
 * 
 * // Create and build index
 * CompositeKeyIndex&lt;Order&gt; index = new CompositeKeyIndex&lt;&gt;(keyPaths);
 * index.buildIndex(orders);
 * 
 * // Lookup orders for user 1 in region "US"
 * List&lt;Order&gt; results = index.lookup(List.of(1L, "US"));
 * </pre>
 * 
 * Example 2: Three-Field Composite Key with Nested Paths
 * <pre>
 * // Define key paths with nested navigation
 * List&lt;List&lt;MetaAttribute&lt;?, ?&gt;&gt;&gt; keyPaths = List.of(
 *     List.of(FlightLeg_.flight, Flight_.flightNo),
 *     List.of(FlightLeg_.flight, Flight_.date),
 *     List.of(FlightLeg_.legSequence)
 * );
 * 
 * // Create and build index
 * CompositeKeyIndex&lt;FlightLeg&gt; index = new CompositeKeyIndex&lt;&gt;(keyPaths);
 * index.buildIndex(flightLegs);
 * 
 * // Lookup flight leg for TK123 on 2024-01-15, leg 1
 * List&lt;FlightLeg&gt; results = index.lookup(
 *     List.of("TK123", LocalDate.parse("2024-01-15"), 1)
 * );
 * </pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li><b>Index Build:</b> O(N * K) time, O(N * K) space</li>
 * <li><b>Lookup:</b> O(K) time, O(1) space</li>
 * <li><b>Memory:</b> More efficient than concatenated string keys (no string allocation)</li>
 * <li><b>Type Safety:</b> Preserves original key types (no conversion needed)</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>CompositeKeyIndex is not thread-safe. External synchronization is required
 * if the index is accessed from multiple threads.</p>
 *
 * @param <T> The entity type
 * @see IndexManager
 * @see com.thy.fss.common.inmemory.engine.mapping.PropertyMapping
 */
public class CompositeKeyIndex<T> {
    
    private final List<List<MetaAttribute<?, ?>>> keyPaths;
    private final Map<Object, Object> rootMap;
    
    /**
     * Creates a new CompositeKeyIndex with the given key paths.
     * 
     * <p>Each key path is a list of MetaAttributes representing the navigation
     * from the entity to a key field. For simple fields, the path contains one element.
     * For nested fields, the path contains multiple elements.</p>
     * 
     * Example:
     * <pre>
     * // Simple fields
     * List&lt;List&lt;MetaAttribute&lt;?, ?&gt;&gt;&gt; keyPaths = List.of(
     *     List.of(Order_.userId),      // Simple field
     *     List.of(Order_.regionId)     // Simple field
     * );
     * 
     * // Nested fields
     * List&lt;List&lt;MetaAttribute&lt;?, ?&gt;&gt;&gt; keyPaths = List.of(
     *     List.of(Order_.customer, Customer_.id),        // Nested field
     *     List.of(Order_.customer, Customer_.regionId)   // Nested field
     * );
     * </pre>
     * 
     * @param keyPaths The list of key field paths, where each path is a list of MetaAttributes
     *                 representing the navigation to a key field
     * @throws IllegalArgumentException if keyPaths is null or empty
     */
    public CompositeKeyIndex(List<List<MetaAttribute<?, ?>>> keyPaths) {
        if (keyPaths == null || keyPaths.isEmpty()) {
            throw new IllegalArgumentException("Key paths cannot be null or empty");
        }
        this.keyPaths = List.copyOf(keyPaths);
        this.rootMap = new HashMap<>();
    }
    
    /**
     * Builds the index from the given entities.
     * This method should be called once after creating the index.
     * 
     * <p>The build process extracts key values from each entity and constructs
     * the nested map structure. Entities with null key values are included in the index.</p>
     * 
     * Algorithm:
     * <ol>
     * <li>For each entity:
     *   <ol>
     *   <li>Extract all key field values by navigating the key paths</li>
     *   <li>Recursively add entity to nested map structure</li>
     *   </ol>
     * </li>
     * </ol>
     * 
     * Example:
     * <pre>
     * CompositeKeyIndex&lt;Order&gt; index = new CompositeKeyIndex&lt;&gt;(keyPaths);
     * 
     * List&lt;Order&gt; orders = List.of(
     *     new Order(1L, "US", ...),
     *     new Order(1L, "EU", ...),
     *     new Order(2L, "US", ...)
     * );
     * 
     * index.buildIndex(orders);
     * // Creates structure:
     * // {
     * //   1L: { "US": [order1], "EU": [order2] },
     * //   2L: { "US": [order3] }
     * // }
     * </pre>
     * 
     * <p><b>Time Complexity:</b> O(N * K) where N is the number of entities and K is the number of key fields</p>
     * <p><b>Space Complexity:</b> O(N * K) for the nested map structure</p>
     * 
     * @param entities The collection of entities to index
     */
    public void buildIndex(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            
            List<Object> keyValues = extractKeyValues(entity);
            addToNestedMap(rootMap, keyValues, entity);
        }
    }
    
    /**
     * Looks up entities by composite key values.
     * All key values must be provided (one for each key field).
     * 
     * <p>The lookup navigates through the nested map structure using each key value
     * in order. All key values must match for entities to be returned.</p>
     * 
     * Algorithm:
     * <ol>
     * <li>Start at root map with first key value</li>
     * <li>For each subsequent key value:
     *   <ol>
     *   <li>Navigate to nested map using current key value</li>
     *   <li>If key not found, return empty list</li>
     *   </ol>
     * </li>
     * <li>At final level, return entity list</li>
     * </ol>
     * 
     * Example:
     * <pre>
     * // Index structure:
     * // {
     * //   1L: { "US": [order1, order2], "EU": [order3] },
     * //   2L: { "US": [order4] }
     * // }
     * 
     * // Lookup orders for user 1 in region "US"
     * List&lt;Order&gt; results = index.lookup(List.of(1L, "US"));
     * // Returns: [order1, order2]
     * 
     * // Lookup orders for user 1 in region "EU"
     * List&lt;Order&gt; results = index.lookup(List.of(1L, "EU"));
     * // Returns: [order3]
     * 
     * // Lookup orders for user 3 (not in index)
     * List&lt;Order&gt; results = index.lookup(List.of(3L, "US"));
     * // Returns: [] (empty list)
     * </pre>
     * 
     * <p><b>Time Complexity:</b> O(K) where K is the number of key fields</p>
     * <p><b>Space Complexity:</b> O(1) for the lookup operation</p>
     * 
     * @param keyValues The list of key values to match (must match keyPaths size)
     * @return The list of entities matching the composite key, or empty list if not found
     * @throws IllegalArgumentException if keyValues is null or size doesn't match keyPaths size
     */
    public List<T> lookup(List<Object> keyValues) {
        if (keyValues == null) {
            throw new IllegalArgumentException("Key values cannot be null");
        }
        
        if (keyValues.size() != keyPaths.size()) {
            throw new IllegalArgumentException(
                String.format("Expected %d key values, got %d", keyPaths.size(), keyValues.size())
            );
        }
        
        List<T> result = findInNestedMap(rootMap, keyValues);
        return result != null ? result : Collections.emptyList();
    }
    
    /**
     * Extracts key values from an entity by navigating the key paths.
     * 
     * @param entity The entity to extract key values from
     * @return The list of key values
     */
    private List<Object> extractKeyValues(T entity) {
        List<Object> values = new ArrayList<>();
        
        for (List<MetaAttribute<?, ?>> path : keyPaths) {
            Object value = navigatePath(entity, path);
            values.add(value);  // Null values are allowed
        }
        
        return values;
    }
    
    /**
     * Navigates a path of MetaAttributes to extract a value from an entity.
     * 
     * @param entity The entity to navigate
     * @param path The path of MetaAttributes
     * @return The value at the end of the path, or null if any intermediate value is null
     */
    private Object navigatePath(Object entity, List<MetaAttribute<?, ?>> path) {
        Object current = entity;
        
        for (MetaAttribute<?, ?> attribute : path) {
            if (current == null) {
                return null;
            }
            
            // Use reflection to get the field value
            try {
                String fieldName = attribute.getName();
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                
                java.lang.reflect.Method getter = current.getClass().getMethod(getterName);
                current = getter.invoke(current);
            } catch (Exception e) {
                throw new InMemoryDataStoreException(
                    "Failed to navigate path for field: " + attribute.getName(), e
                );
            }
        }
        
        return current;
    }
    
    /**
     * Recursively adds an entity to the nested map structure.
     * Creates intermediate maps lazily as needed.
     * 
     * @param map The current map at this level
     * @param keys The list of key values
     * @param entity The entity to add
     */
    @SuppressWarnings("unchecked")
    private void addToNestedMap(Map<Object, Object> map, List<Object> keys, T entity) {
        if (keys.size() == 1) {
            // Last level: store entity list
            Object key = keys.get(0);
            Object existing = map.get(key);
            
            if (existing == null) {
                // First entity with this key
                List<T> entities = new ArrayList<>();
                entities.add(entity);
                map.put(key, entities);
            } else {
                // Add to existing list
                ((List<T>) existing).add(entity);
            }
        } else {
            // Intermediate level: get or create nested map
            Object firstKey = keys.get(0);
            Object existing = map.get(firstKey);
            Map<Object, Object> nestedMap;
            
            if (existing == null) {
                // Lazy initialization: create new map for next level
                nestedMap = new HashMap<>();
                map.put(firstKey, nestedMap);
            } else {
                nestedMap = (Map<Object, Object>) existing;
            }
            
            // Recurse to next level
            addToNestedMap(nestedMap, keys.subList(1, keys.size()), entity);
        }
    }
    
    /**
     * Recursively finds entities in the nested map structure.
     * 
     * @param map The current map at this level
     * @param keys The list of key values
     * @return The list of entities, or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<T> findInNestedMap(Map<Object, Object> map, List<Object> keys) {
        if (keys.size() == 1) {
            // Last level: return the entity list
            Object result = map.get(keys.get(0));
            return result != null ? (List<T>) result : Collections.emptyList();
        } else {
            // Intermediate level: navigate deeper
            Object firstKey = keys.get(0);
            Object result = map.get(firstKey);
            
            if (result == null) {
                return Collections.emptyList();
            }
            
            Map<Object, Object> nestedMap = (Map<Object, Object>) result;
            return findInNestedMap(nestedMap, keys.subList(1, keys.size()));
        }
    }
    
    /**
     * Gets the number of key fields in this composite key.
     * 
     * @return The number of key fields
     */
    public int getKeyFieldCount() {
        return keyPaths.size();
    }
    
    /**
     * Gets the key paths for this index.
     * 
     * @return An unmodifiable list of key paths
     */
    public List<List<MetaAttribute<?, ?>>> getKeyPaths() {
        return keyPaths;
    }
    
    /**
     * Recursively clears all nested HashMap structures.
     * Ensures all levels are cleared for garbage collection.
     * This method is idempotent and safe to call multiple times.
     */
    public void deepClear() {
        clearNestedMap(rootMap, 0);
        rootMap.clear();
    }
    
    /**
     * Recursively clears nested HashMap structures at all levels.
     *
     * @param map   The Map to clear at this level
     * @param level The current level (0-based)
     */
    @SuppressWarnings("unchecked")
    private void clearNestedMap(Map<Object, Object> map, int level) {
        if (level == keyPaths.size() - 1) {
            // Leaf level: clear entity lists
            for (Object value : map.values()) {
                ((List<T>) value).clear();
            }
        } else {
            // Intermediate level: recurse and clear nested maps
            for (Object value : map.values()) {
                Map<Object, Object> nestedMap = (Map<Object, Object>) value;
                clearNestedMap(nestedMap, level + 1);
                nestedMap.clear();
            }
        }
    }
}
