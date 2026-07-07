package com.thy.fss.common.inmemory.engine.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import com.thy.fss.common.inmemory.entity.Identifiable;

/**
 * Multi-level nested TreeMap-based index structure.
 * Provides O(log n) lookup performance at each level.
 * Supports partial key lookup, range queries, and prefix search.
 *
 * @param <T> The entity type
 */
public class NestedTreeMapIndex<T> {

    private final TreeMap<Comparable<?>, Object> rootMap;
    private final IndexDefinition<T> definition;
    private final int depth;
    private long buildTimeMs;
    private int totalEntries;

    /**
     * Creates a new NestedTreeMapIndex with the given definition.
     *
     * @param definition The index definition
     */
    public NestedTreeMapIndex(IndexDefinition<T> definition) {
        this.definition = definition;
        this.depth = definition.getDepth();
        this.rootMap = new TreeMap<>(definition.getComparatorForLevel(0));
    }

    /**
     * Builds the index from the given data.
     * This method should be called once after creating the index.
     *
     * @param data The list of entities to index
     */
    @SuppressWarnings("unchecked")
    public void build(List<T> data) {
        long startTime = System.currentTimeMillis();

        if (data == null || data.isEmpty()) {
            this.buildTimeMs = System.currentTimeMillis() - startTime;
            this.totalEntries = 0;
            return;
        }

        int entryCount = 0;
        for (T entity : data) {
            Comparable<?> key = entity != null ? definition.extractKeyValue(entity, 0) : null;
            if (key == null) {
                continue;
            }

            entryCount++;

            if (depth == 1) {
                // Single-level index: store entities directly
                Object existing = rootMap.get(key);
                if (existing == null) {
                    // First entity with this key
                    List<T> entities = new ArrayList<>();
                    entities.add(entity);
                    rootMap.put(key, entities);
                } else {
                    // Add to existing list
                    ((List<T>) existing).add(entity);
                }
            } else {
                // Multi-level index: recursively build nested TreeMaps
                insertEntity(rootMap, entity, 0);
            }
        }

        this.buildTimeMs = System.currentTimeMillis() - startTime;
        this.totalEntries = entryCount;
    }

    /**
     * Looks up entities by full key.
     * All key values must be provided (one for each level).
     *
     * @param keys The key values (must match the depth)
     * @return The list of entities matching the key, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<T> lookup(Object... keys) {
        if (keys == null || keys.length != depth) {
            throw new IllegalArgumentException(
                    String.format("Expected %d keys, got %d", depth, keys == null ? 0 : keys.length)
            );
        }

        if (depth == 1) {
            // Single-level lookup
            Object result = rootMap.get(keys[0]);
            if (result == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>((List<T>) result);
        } else {
            // Multi-level lookup: traverse nested TreeMaps
            return lookupRecursive(rootMap, keys, 0);
        }
    }

    /**
     * Recursively inserts an entity into the nested TreeMap structure.
     * Creates intermediate TreeMaps lazily as needed.
     *
     * @param currentMap The current TreeMap at this level
     * @param entity     The entity to insert
     * @param level      The current level (0-based)
     */
    @SuppressWarnings("unchecked")
    private void insertEntity(TreeMap<Comparable<?>, Object> currentMap, T entity, int level) {
        Comparable<?> key = definition.extractKeyValue(entity, level);
        if (key == null) {
            return; // Skip entities with null keys
        }

        if (level == depth - 1) {
            // Last level: store entities in a list
            Object existing = currentMap.get(key);
            if (existing == null) {
                List<T> entities = new ArrayList<>();
                entities.add(entity);
                currentMap.put(key, entities);
            } else {
                List<T> entities = (List<T>) existing;
                // Duplicate kontrolü: aynı identity varsa önce sil
                if (entity instanceof Identifiable) {
                    Object newId = ((Identifiable<?>) entity).getIdentity();
                    int sizeBefore = entities.size();
                    entities.removeIf(e -> e instanceof Identifiable
                            && Objects.equals(((Identifiable<?>) e).getIdentity(), newId));
                    totalEntries -= (sizeBefore - entities.size());
                }
                entities.add(entity);
            }
        } else {
            // Intermediate level: get or create nested TreeMap
            Object existing = currentMap.get(key);
            TreeMap<Comparable<?>, Object> nextMap;

            if (existing == null) {
                // Lazy initialization: create new TreeMap for next level
                nextMap = new TreeMap<>(definition.getComparatorForLevel(level + 1));
                currentMap.put(key, nextMap);
            } else {
                nextMap = (TreeMap<Comparable<?>, Object>) existing;
            }

            // Recurse to next level
            insertEntity(nextMap, entity, level + 1);
        }
    }

    /**
     * Recursively looks up entities in the nested TreeMap structure.
     *
     * @param currentMap The current TreeMap at this level
     * @param keys       The array of key values
     * @param level      The current level (0-based)
     * @return The list of entities, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    private List<T> lookupRecursive(TreeMap<Comparable<?>, Object> currentMap, Object[] keys, int level) {
        Object result = currentMap.get(keys[level]);
        if (result == null) {
            return Collections.emptyList();
        }

        if (level == depth - 1) {
            // Last level: return the entity list (defensive copy for snapshot isolation)
            return new ArrayList<>((List<T>) result);
        } else {
            // Intermediate level: recurse to next level
            TreeMap<Comparable<?>, Object> nextMap = (TreeMap<Comparable<?>, Object>) result;
            return lookupRecursive(nextMap, keys, level + 1);
        }
    }

    /**
     * Performs a partial key lookup.
     * Returns a map at the specified level containing all matching sub-trees.
     *
     * @param keys The partial key values (can be less than depth)
     * @return A map at the last specified level, or empty map if not found
     */
    @SuppressWarnings("unchecked")
    public SortedMap<Comparable<?>, Object> partialLookup(Object... keys) {
        if (keys == null || keys.length == 0) {
            return rootMap;
        }

        if (keys.length > depth) {
            throw new IllegalArgumentException(
                    String.format("Too many keys: expected at most %d, got %d", depth, keys.length)
            );
        }

        return partialLookupRecursive(rootMap, keys, 0);
    }

    /**
     * Recursively performs partial lookup in the nested TreeMap structure.
     *
     * @param currentMap The current TreeMap at this level
     * @param keys       The array of key values
     * @param level      The current level (0-based)
     * @return The TreeMap at the target level, or empty map if not found
     */
    @SuppressWarnings("unchecked")
    private TreeMap<Comparable<?>, Object> partialLookupRecursive(
            TreeMap<Comparable<?>, Object> currentMap, Object[] keys, int level) {

        if (level >= keys.length) {
            // Reached the target level
            return currentMap;
        }

        Object result = currentMap.get(keys[level]);
        if (result == null) {
            return new TreeMap<>();
        }

        if (level == depth - 1) {
            // At last level, can't go deeper
            TreeMap<Comparable<?>, Object> resultMap = new TreeMap<>();
            resultMap.put((Comparable<?>) keys[level], result);
            return resultMap;
        }

        // Continue to next level
        TreeMap<Comparable<?>, Object> nextMap = (TreeMap<Comparable<?>, Object>) result;
        return partialLookupRecursive(nextMap, keys, level + 1);
    }

    /**
     * Performs a range query at a specific level.
     * Returns all entries between fromKey and toKey (inclusive) at the specified level.
     *
     * @param fromKey The starting key (inclusive)
     * @param toKey   The ending key (inclusive)
     * @param level   The level at which to perform the range query (0-based)
     * @return A sub-map containing entries in the specified range
     */
    @SuppressWarnings("unchecked")
    public SortedMap<Comparable<?>, Object> rangeQuery(Object fromKey, Object toKey, int level) {
        if (level < 0 || level >= depth) {
            throw new IllegalArgumentException(
                    String.format("Invalid level: %d (depth is %d)", level, depth)
            );
        }

        if (level == 0) {
            // Range query at root level
            return new TreeMap<>(rootMap.subMap(
                    (Comparable<?>) fromKey, true,
                    (Comparable<?>) toKey, true
            ));
        }

        // For deeper levels, we need to traverse to that level first
        // This is a simplified implementation that returns empty map
        // A full implementation would need prefix keys to navigate to the target level
        return new TreeMap<>();
    }

    /**
     * Performs a prefix search starting from the given partial keys.
     * Returns all entities that match the prefix.
     *
     * @param prefixKeys The prefix key values
     * @return A list of all entities matching the prefix
     */
    @SuppressWarnings("unchecked")
    public List<T> prefixSearch(Object... prefixKeys) {
        if (prefixKeys == null || prefixKeys.length == 0) {
            // No prefix: return all values
            return getAllValues();
        }

        if (prefixKeys.length >= depth) {
            // Full key: use regular lookup
            return lookup(prefixKeys);
        }

        // Partial prefix: get the sub-tree and collect all values
        SortedMap<Comparable<?>, Object> subTree = partialLookup(prefixKeys);
        return collectAllValues(subTree, prefixKeys.length);
    }

    /**
     * Gets all values from the index.
     * Traverses the entire tree and collects all entities.
     *
     * @return A list of all entities in the index
     */
    public List<T> getAllValues() {
        return collectAllValues(rootMap, 0);
    }

    /**
     * Collects all entity values from a sub-tree.
     *
     * @param map   The TreeMap to collect from
     * @param level The current level in the tree
     * @return A list of all entities in the sub-tree
     */
    @SuppressWarnings("unchecked")
    private List<T> collectAllValues(Map<Comparable<?>, Object> map, int level) {
        List<T> result = new ArrayList<>();

        for (Object value : map.values()) {
            if (level == depth - 1) {
                // At leaf level: value is List<T>
                result.addAll((List<T>) value);
            } else {
                // At intermediate level: value is TreeMap
                TreeMap<Comparable<?>, Object> nextMap = (TreeMap<Comparable<?>, Object>) value;
                result.addAll(collectAllValues(nextMap, level + 1));
            }
        }

        return result;
    }

    /**
     * Gets the index definition.
     *
     * @return The index definition
     */
    public IndexDefinition<T> getDefinition() {
        return definition;
    }

    /**
     * Gets the depth of the index.
     *
     * @return The number of levels
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Gets statistics for this index.
     * This is a lightweight operation with no runtime overhead.
     *
     * @param datasourceName The name of the datasource
     * @return IndexStatistics containing creation-time metrics
     */
    public IndexStatistics getStatistics(String datasourceName) {
        return new IndexStatistics(datasourceName, buildTimeMs, totalEntries, depth);
    }

    /**
     * Inserts a single entity into the index.
     * This is the public API for incremental index updates.
     * Handles both single-level and multi-level indexes.
     *
     * @param entity The entity to insert
     */
    @SuppressWarnings("unchecked")
    public void insertEntity(T entity) {
        if (entity == null) {
            return;
        }
        Comparable<?> key = definition.extractKeyValue(entity, 0);
        if (key == null) {
            return;
        }
        if (depth == 1) {
            Object existing = rootMap.get(key);
            if (existing == null) {
                List<T> entities = new ArrayList<>();
                entities.add(entity);
                rootMap.put(key, entities);
            } else {
                List<T> entities = (List<T>) existing;
                // Duplicate kontrolü: aynı identity varsa önce sil
                if (entity instanceof Identifiable) {
                    Object newId = ((Identifiable<?>) entity).getIdentity();
                    int sizeBefore = entities.size();
                    entities.removeIf(e -> e instanceof Identifiable
                            && Objects.equals(((Identifiable<?>) e).getIdentity(), newId));
                    totalEntries -= (sizeBefore - entities.size());
                }
                entities.add(entity);
            }
        } else {
            insertEntity(rootMap, entity, 0);
        }
        totalEntries++;
    }

    /**
     * Removes a single entity from the index.
     * Symmetric with {@link #insertEntity(Object)}.
     * Extracts key values using the index definition and removes the entity
     * from the leaf node list. Prunes empty nodes up the tree.
     *
     * @param entity The entity to remove
     * @return true if the entity was found and removed, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean removeEntity(T entity) {
        if (entity == null) {
            return false;
        }
        Comparable<?> key = definition.extractKeyValue(entity, 0);
        if (key == null) {
            return false;
        }
        boolean removed;
        if (depth == 1) {
            Object existing = rootMap.get(key);
            if (existing == null) {
                return false;
            }
            List<T> entities = (List<T>) existing;
            removed = removeByIdentity(entities, entity);
            if (removed && entities.isEmpty()) {
                rootMap.remove(key);
            }
        } else {
            removed = removeEntityRecursive(rootMap, entity, 0);
        }
        if (removed) {
            totalEntries--;
        }
        return removed;
    }

    /**
     * Recursively removes an entity from the nested TreeMap structure.
     * Prunes empty nodes after removal.
     *
     * @param currentMap The current TreeMap at this level
     * @param entity     The entity to remove
     * @param level      The current level (0-based)
     * @return true if the entity was found and removed
     */
    @SuppressWarnings("unchecked")
    private boolean removeEntityRecursive(TreeMap<Comparable<?>, Object> currentMap, T entity, int level) {
        Comparable<?> key = definition.extractKeyValue(entity, level);
        if (key == null) {
            return false;
        }
        Object existing = currentMap.get(key);
        if (existing == null) {
            return false;
        }
        if (level == depth - 1) {
            // Leaf level: remove entity from list
            List<T> entities = (List<T>) existing;
            boolean removed = removeByIdentity(entities, entity);
            if (removed && entities.isEmpty()) {
                currentMap.remove(key);
            }
            return removed;
        } else {
            // Intermediate level: recurse into nested TreeMap
            TreeMap<Comparable<?>, Object> nextMap = (TreeMap<Comparable<?>, Object>) existing;
            boolean removed = removeEntityRecursive(nextMap, entity, level + 1);
            if (removed && nextMap.isEmpty()) {
                currentMap.remove(key);
            }
            return removed;
        }
    }

    /**
     * Removes an entity from the list using identity-based matching for Identifiable entities.
     * Falls back to List.remove(Object) for non-Identifiable entities.
     * Uses Iterator.remove() to avoid ConcurrentModificationException.
     *
     * @param entities The list to remove from
     * @param entity   The entity to remove
     * @return true if an entity was found and removed
     */
    private boolean removeByIdentity(List<T> entities, T entity) {
        if (entity instanceof Identifiable) {
            Object targetId = ((Identifiable<?>) entity).getIdentity();
            Iterator<T> it = entities.iterator();
            while (it.hasNext()) {
                T existing = it.next();
                if (existing instanceof Identifiable
                        && Objects.equals(((Identifiable<?>) existing).getIdentity(), targetId)) {
                    it.remove();
                    return true;
                }
            }
            return false;
        } else {
            return entities.remove(entity);
        }
    }

    /**
     * Removes an entity by identity across ALL buckets in the index, regardless of
     * the entity's current key values. This is necessary when an entity's indexed
     * field values have changed (mutated) — the old index entry is stored under the
     * OLD key, but the entity now holds the NEW key values.
     *
     * <p>Traverses all leaf-level lists and removes the first entry matching the
     * entity's identity. Complexity is O(N) where N = total indexed entries, but
     * this is only called for mutated entities during incremental index updates.</p>
     *
     * @param entity The entity to remove (matched by identity, not by key value)
     * @return true if an entity was found and removed
     */
    @SuppressWarnings("unchecked")
    public boolean removeEntityByIdentity(T entity) {
        if (entity == null) {
            return false;
        }
        boolean removed;
        if (depth == 1) {
            removed = removeByIdentityFromLeafMap(rootMap, entity);
        } else {
            removed = removeByIdentityRecursive(rootMap, entity, 0);
        }
        if (removed) {
            totalEntries--;
        }
        return removed;
    }

    /**
     * Recursively traverses intermediate-level maps to reach leaf-level lists,
     * then removes the entity by identity from any leaf list that contains it.
     */
    @SuppressWarnings("unchecked")
    private boolean removeByIdentityRecursive(TreeMap<Comparable<?>, Object> currentMap, T entity, int level) {
        if (level == depth - 1) {
            return removeByIdentityFromLeafMap(currentMap, entity);
        }
        Iterator<Map.Entry<Comparable<?>, Object>> it = currentMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Comparable<?>, Object> entry = it.next();
            TreeMap<Comparable<?>, Object> nextMap = (TreeMap<Comparable<?>, Object>) entry.getValue();
            boolean removed = removeByIdentityRecursive(nextMap, entity, level + 1);
            if (removed) {
                if (nextMap.isEmpty()) {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Scans all leaf-level lists in the given map and removes the entity by identity.
     */
    @SuppressWarnings("unchecked")
    private boolean removeByIdentityFromLeafMap(TreeMap<Comparable<?>, Object> leafMap, T entity) {
        Iterator<Map.Entry<Comparable<?>, Object>> it = leafMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Comparable<?>, Object> entry = it.next();
            List<T> entities = (List<T>) entry.getValue();
            boolean removed = removeByIdentity(entities, entity);
            if (removed) {
                if (entities.isEmpty()) {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively clears all nested TreeMap structures.
     * Ensures all levels are cleared for garbage collection.
     * This method is idempotent and safe to call multiple times.
     */
    public void deepClear() {
        clearRecursive(rootMap, 0);
        rootMap.clear();
        totalEntries = 0;
    }


    /**
     * Recursively clears nested TreeMap structures at all levels.
     *
     * @param map   The TreeMap to clear at this level
     * @param level The current level (0-based)
     */
    @SuppressWarnings("unchecked")
    private void clearRecursive(TreeMap<Comparable<?>, Object> map, int level) {
        if (level == depth - 1) {
            // Leaf level: clear entity lists
            for (Object value : map.values()) {
                ((List<T>) value).clear();
            }
        } else {
            // Intermediate level: recurse and clear nested maps
            for (Object value : map.values()) {
                TreeMap<Comparable<?>, Object> nestedMap =
                        (TreeMap<Comparable<?>, Object>) value;
                clearRecursive(nestedMap, level + 1);
                nestedMap.clear();
            }
        }
    }
}
