package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.engine.index.NestedTreeMapIndex;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;

/**
 * Tek birlesik TreeMap tabanli veri yapisi.
 *
 * <p>Tum entity'leri, bagimliliklari ve index'leri tek bir yapida tutar.
 * Entity'ler datasource bazinda TreeMap icinde ID'ye gore sirali saklanir.</p>
 *
 * Thread-safety
 * <ul>
 *   <li>{@link #upsertAll} yeni bir TreeMap olusturur ve volatile swap ile
 *       referansi degistirir - okuma islemleri asla bloklanmaz.</li>
 *   <li>{@link #findAll} tutarli snapshot dondurur: ya eski ya da yeni durum,
 *       asla kismi guncelleme gorunmez.</li>
 *   <li>Tum yazma islemleri copy-on-write pattern ile calisir.</li>
 * </ul>
 */
public class DependencyGraph {

    private static final Logger logger = LoggerFactory.getLogger(DependencyGraph.class);

    // === Veri Saklama ===

    private final ConcurrentHashMap<String, TreeMap<Object, Object>> entityStore =
            new ConcurrentHashMap<>();

    // === Bagimlilik Izleme ===

    private volatile Map<String, List<PropertyMapping<?, ?>>> dependencyMap = new HashMap<>();

    private volatile Map<String, AffectedConsumerSet> consumerMap = new HashMap<>();

    // === Index Yonetimi (entegre) ===

    /**
     * datasource to (IndexDefinition to NestedTreeMapIndex) mapping.
     * Streaming datasource'lar icin entegre index yapisi.
     * Mevcut IndexManager batch full sync icin korunur - paralel calisir.
     * Index guncellemeleri copy-on-write pattern ile atomik olarak uygulanir.
     */
    private final ConcurrentHashMap<String, Map<IndexDefinition<?>, NestedTreeMapIndex<?>>> indexStore =
            new ConcurrentHashMap<>();

    // --- Veri Islemleri ---

    public <T extends Identifiable<?>> void upsert(String dataSourceName, T entity) {
        entityStore.compute(dataSourceName, (key, current) -> {
            TreeMap<Object, Object> newMap = current != null
                    ? new TreeMap<>(current)
                    : new TreeMap<>();
            newMap.put(entity.getIdentity(), entity);
            return newMap;
        });
    }

    public <T extends Identifiable<?>> void upsertAll(String dataSourceName, List<T> entities) {
        entityStore.compute(dataSourceName, (key, current) -> {
            TreeMap<Object, Object> newMap = current != null
                    ? new TreeMap<>(current)
                    : new TreeMap<>();
            for (T entity : entities) {
                newMap.put(entity.getIdentity(), entity);
            }
            return newMap;
        });
    }

    public <T extends Identifiable<?>> int upsertAllIndividually(
            String dataSourceName, List<T> entities) {
        int[] successCount = {0};
        entityStore.compute(dataSourceName, (key, current) -> {
            TreeMap<Object, Object> newMap = current != null
                    ? new TreeMap<>(current) : new TreeMap<>();
            for (T entity : entities) {
                try {
                    newMap.put(entity.getIdentity(), entity);
                    successCount[0]++;
                } catch (Exception e) {
                    logger.error("Error adding entity id={}: {}", entity.getIdentity(), e.getMessage());
                }
            }
            return newMap;
        });
        return successCount[0];
    }

    public void removeById(String dataSourceName, Object entityId) {
        entityStore.computeIfPresent(dataSourceName, (key, current) -> {
            TreeMap<Object, Object> newMap = new TreeMap<>(current);
            newMap.remove(entityId);
            return newMap;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T findById(String dataSourceName, Object entityId) {
        TreeMap<Object, Object> map = entityStore.get(dataSourceName);
        if (map == null) {
            return null;
        }
        return (T) map.get(entityId);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(String dataSourceName) {
        TreeMap<Object, Object> map = entityStore.get(dataSourceName);
        if (map == null) {
            return Collections.emptyList();
        }
        List<T> snapshot = new ArrayList<>();
        for (Object value : map.values()) {
            snapshot.add((T) value);
        }
        return Collections.unmodifiableList(snapshot);
    }

    // --- Bagimlilik Islemleri ---

    public void build(List<PropertyMapping<?, ?>> allMappings) {
        if (allMappings == null) {
            return;
        }
        Map<String, List<PropertyMapping<?, ?>>> newDependencyMap = new HashMap<>();
        Map<String, AffectedConsumerSet> newConsumerMap = new HashMap<>();
        for (PropertyMapping<?, ?> mapping : allMappings) {
            String dsName = mapping.getDataSourceName();
            if (dsName == null) {
                continue;
            }
            newDependencyMap.computeIfAbsent(dsName, k -> new ArrayList<>()).add(mapping);
        }
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : newDependencyMap.entrySet()) {
            newConsumerMap.put(entry.getKey(), buildConsumerSet(entry.getValue()));
        }
        // Atomic swap: readers see either old consistent state or new consistent state
        this.dependencyMap = newDependencyMap;
        this.consumerMap = newConsumerMap;
    }

    public List<PropertyMapping<?, ?>> getMappingsForDataSource(String dataSourceName) {
        List<PropertyMapping<?, ?>> mappings = dependencyMap.get(dataSourceName);
        if (mappings == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(mappings));
    }

    /**
     * Returns all mappings targeting a specific consumer (store or dashboard).
     * Scans all datasource entries in the dependency map.
     *
     * @param consumerId the consumer ID to match
     * @return list of mappings targeting the consumer, never null
     */
    public List<PropertyMapping<?, ?>> getMappingsByConsumerId(String consumerId) {
        List<PropertyMapping<?, ?>> result = new ArrayList<>();
        Map<String, List<PropertyMapping<?, ?>>> snapshot = this.dependencyMap;
        for (List<PropertyMapping<?, ?>> mappings : snapshot.values()) {
            for (PropertyMapping<?, ?> mapping : mappings) {
                if (consumerId.equals(mapping.getConsumerId())) {
                    result.add(mapping);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }


    public AffectedConsumerSet getAffectedConsumers(String dataSourceName) {
        AffectedConsumerSet consumers = consumerMap.get(dataSourceName);
        if (consumers == null) {
            return new AffectedConsumerSet(
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptyList());
        }
        return consumers;
    }

    public synchronized void addMapping(PropertyMapping<?, ?> mapping) {
        if (mapping == null || mapping.getDataSourceName() == null) {
            return;
        }
        String dsName = mapping.getDataSourceName();
        Map<String, List<PropertyMapping<?, ?>>> newDependencyMap = new HashMap<>(this.dependencyMap);
        List<PropertyMapping<?, ?>> current = newDependencyMap.get(dsName);
        List<PropertyMapping<?, ?>> newList = current != null
                ? new ArrayList<>(current)
                : new ArrayList<>();
        newList.add(mapping);
        newDependencyMap.put(dsName, newList);
        this.dependencyMap = newDependencyMap;
        rebuildConsumerMap(dsName);
    }

    public synchronized void removeMapping(PropertyMapping<?, ?> mapping) {
        if (mapping == null || mapping.getDataSourceName() == null) {
            return;
        }
        String dsName = mapping.getDataSourceName();
        Map<String, List<PropertyMapping<?, ?>>> newDependencyMap = new HashMap<>(this.dependencyMap);
        List<PropertyMapping<?, ?>> current = newDependencyMap.get(dsName);
        if (current == null) {
            return;
        }
        List<PropertyMapping<?, ?>> newList = new ArrayList<>(current);
        newList.remove(mapping);
        if (newList.isEmpty()) {
            newDependencyMap.remove(dsName);
        } else {
            newDependencyMap.put(dsName, newList);
        }
        this.dependencyMap = newDependencyMap;
        rebuildConsumerMap(dsName);
    }

    public void detectCycles() {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : dependencyMap.entrySet()) {
            Set<String> targets = new HashSet<>();
            for (PropertyMapping<?, ?> mapping : entry.getValue()) {
                targets.add(mapping.getConsumerId());
            }
            graph.put(entry.getKey(), targets);
        }
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                List<String> path = new ArrayList<>();
                if (hasCycle(node, graph, visited, recursionStack, path)) {
                    throw new IllegalStateException(
                            "Circular dependency detected: " + String.join(" -> ", path));
                }
            }
        }
    }

    // --- Index Islemleri (entegre) ---

    @SuppressWarnings("unchecked")
    public <T> List<T> lookup(String dataSourceName, IndexDefinition<T> definition, Object... keys) {
        Map<IndexDefinition<?>, NestedTreeMapIndex<?>> indexes = indexStore.get(dataSourceName);
        if (indexes == null) {
            return Collections.emptyList();
        }
        NestedTreeMapIndex<T> index = (NestedTreeMapIndex<T>) indexes.get(definition);
        if (index == null) {
            return Collections.emptyList();
        }
        return index.lookup(keys);
    }

    @SuppressWarnings("unchecked")
    public <T extends Identifiable<?>> void updateIndexes(
            String dataSourceName, List<T> oldEntities, List<T> newEntities) {
        if (newEntities == null || newEntities.isEmpty()) {
            return;
        }
        Map<IndexDefinition<?>, NestedTreeMapIndex<?>> currentIndexes = indexStore.get(dataSourceName);
        if (currentIndexes == null || currentIndexes.isEmpty()) {
            return;
        }
        for (Map.Entry<IndexDefinition<?>, NestedTreeMapIndex<?>> entry : currentIndexes.entrySet()) {
            NestedTreeMapIndex<T> index = (NestedTreeMapIndex<T>) entry.getValue();
            // Remove old entities from index
            if (oldEntities != null) {
                for (T oldEntity : oldEntities) {
                    index.removeEntity(oldEntity);
                }
            }
            // Insert new entities into index
            for (T newEntity : newEntities) {
                index.insertEntity(newEntity);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Identifiable<?>> void removeFromIndexes(
            String dataSourceName, List<T> removedEntities) {
        if (removedEntities == null || removedEntities.isEmpty()) {
            return;
        }
        Map<IndexDefinition<?>, NestedTreeMapIndex<?>> currentIndexes = indexStore.get(dataSourceName);
        if (currentIndexes == null || currentIndexes.isEmpty()) {
            return;
        }
        for (Map.Entry<IndexDefinition<?>, NestedTreeMapIndex<?>> entry : currentIndexes.entrySet()) {
            NestedTreeMapIndex<T> index = (NestedTreeMapIndex<T>) entry.getValue();
            for (T removedEntity : removedEntities) {
                index.removeEntity(removedEntity);
            }
        }
    }

    public <T> void registerIndex(String dataSourceName, IndexDefinition<T> definition) {
        indexStore.compute(dataSourceName, (key, current) -> {
            Map<IndexDefinition<?>, NestedTreeMapIndex<?>> newMap = current != null
                    ? new HashMap<>(current)
                    : new HashMap<>();
            if (!newMap.containsKey(definition)) {
                NestedTreeMapIndex<T> index = new NestedTreeMapIndex<>(definition);
                List<T> entities = findAll(dataSourceName);
                if (!entities.isEmpty()) {
                    index.build(entities);
                }
                newMap.put(definition, index);
            }
            return newMap;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> void rebuildIndexesForDataSource(String dataSourceName) {
        Map<IndexDefinition<?>, NestedTreeMapIndex<?>> currentIndexes = indexStore.get(dataSourceName);
        if (currentIndexes == null || currentIndexes.isEmpty()) {
            return;
        }
        List<T> allEntities = findAll(dataSourceName);
        Map<IndexDefinition<?>, NestedTreeMapIndex<?>> newIndexes = new HashMap<>();
        for (Map.Entry<IndexDefinition<?>, NestedTreeMapIndex<?>> entry : currentIndexes.entrySet()) {
            IndexDefinition<T> def = (IndexDefinition<T>) entry.getKey();
            NestedTreeMapIndex<T> newIndex = new NestedTreeMapIndex<>(def);
            newIndex.build(allEntities);
            newIndexes.put(def, newIndex);
        }
        indexStore.put(dataSourceName, newIndexes);
    }

    /**
     * Updates only the specified entities' index entries incrementally.
     * For each entity: removeEntity(old) + insertEntity(new).
     *
     * <p>If the mutation rate exceeds 50% of total entities, returns {@code false}
     * to signal that a full rebuild is more efficient.</p>
     *
     * <p>On any exception during incremental update, logs the error and returns
     * {@code false} so the caller can fall back to {@link #rebuildIndexesForDataSource}.</p>
     *
     * @param dataSourceName target datasource
     * @param mutatedEntities entities that were mutated
     * @param <T> entity type
     * @return true if incremental update succeeded, false if full rebuild is needed
     */
    @SuppressWarnings("unchecked")
    public <T extends Identifiable<?>> boolean updateIndexesForEntities(
            String dataSourceName,
            List<T> mutatedEntities) {
        try {
            if (mutatedEntities == null || mutatedEntities.isEmpty()) {
                return true; // Nothing to update
            }

            Map<IndexDefinition<?>, NestedTreeMapIndex<?>> currentIndexes = indexStore.get(dataSourceName);
            if (currentIndexes == null || currentIndexes.isEmpty()) {
                return true; // No indexes to update
            }

            // Mutation rate check: if > 50%, signal full rebuild
            TreeMap<Object, Object> allEntities = entityStore.get(dataSourceName);
            int totalSize = (allEntities != null) ? allEntities.size() : 0;
            if (totalSize > 0 && (double) mutatedEntities.size() / totalSize > 0.50) {
                logger.debug("Mutation rate {}/{} (>{}) exceeds 50% threshold for ds={}, signalling full rebuild",
                        mutatedEntities.size(), totalSize, "50%", dataSourceName);
                return false;
            }

            // Incremental update: remove old entry + insert new entry for each entity
            for (Map.Entry<IndexDefinition<?>, NestedTreeMapIndex<?>> entry : currentIndexes.entrySet()) {
                NestedTreeMapIndex<T> index = (NestedTreeMapIndex<T>) entry.getValue();
                for (T entity : mutatedEntities) {
                    index.removeEntityByIdentity(entity);
                    index.insertEntity(entity);
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Incremental index update failed for ds={}, falling back to full rebuild: {}",
                    dataSourceName, e.getMessage(), e);
            return false;
        }
    }


    // --- Private helpers ---

    private AffectedConsumerSet buildConsumerSet(List<PropertyMapping<?, ?>> mappings) {
        Set<String> storeIds = new LinkedHashSet<>();
        Set<String> dashboardIds = new LinkedHashSet<>();
        for (PropertyMapping<?, ?> mapping : mappings) {
            if (mapping.isForDashboard()) {
                dashboardIds.add(mapping.getConsumerId());
            } else {
                storeIds.add(mapping.getConsumerId());
            }
        }
        return new AffectedConsumerSet(storeIds, dashboardIds, mappings);
    }

    private void rebuildConsumerMap(String dataSourceName) {
        List<PropertyMapping<?, ?>> mappings = dependencyMap.get(dataSourceName);
        Map<String, AffectedConsumerSet> newConsumerMap = new HashMap<>(this.consumerMap);
        if (mappings == null || mappings.isEmpty()) {
            newConsumerMap.remove(dataSourceName);
        } else {
            newConsumerMap.put(dataSourceName, buildConsumerSet(mappings));
        }
        this.consumerMap = newConsumerMap;
    }

    private boolean hasCycle(String node,
                             Map<String, Set<String>> graph,
                             Set<String> visited,
                             Set<String> recursionStack,
                             List<String> path) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);
        Set<String> neighbors = graph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    if (graph.containsKey(neighbor)
                            && hasCycle(neighbor, graph, visited, recursionStack, path)) {
                        return true;
                    }
                } else if (recursionStack.contains(neighbor)) {
                    path.add(neighbor);
                    return true;
                }
            }
        }
        recursionStack.remove(node);
        path.remove(path.size() - 1);
        return false;
    }
}