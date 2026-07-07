package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;
import com.thy.fss.common.inmemory.testmodel.User;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for Incremental Index Equivalence.
 *
 * <p>Verifies that for any entity set and mutated subset, incremental index update
 * (removeEntity + insertEntity) produces the same index state as a full rebuild
 * (rebuildIndexesForDataSource).</p>
 *
 * <p>Uses real {@link DependencyGraph} instances with {@link User} test entities
 * and {@link NestedTreeMapIndex} to validate the equivalence property.</p>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 6.2</b></p>
 */
class IncrementalIndexEquivalencePropertyTest {

    private static final String DS_NAME = "test-ds";
    private static final List<String> NAME_POOL = List.of(
            "Alice", "Bob", "Charlie", "Diana", "Eve",
            "Frank", "Grace", "Hank", "Ivy", "Jack");

    // ==================== Arbitraries ====================

    /**
     * Generates a list of unique User entities (3–20) with random names from the pool.
     */
    @Provide
    Arbitrary<List<User>> userSets() {
        return Arbitraries.integers().between(3, 20).flatMap(size -> {
            Arbitrary<String> names = Arbitraries.of(NAME_POOL);
            return names.list().ofSize(size).map(nameList -> {
                List<User> users = new ArrayList<>();
                for (int i = 0; i < nameList.size(); i++) {
                    User u = new User();
                    u.setId(String.valueOf(i + 1));
                    u.setName(nameList.get(i));
                    users.add(u);
                }
                return users;
            });
        });
    }

    /**
     * Generates a set of indices representing which entities to mutate.
     * The set size is constrained to at most 49% of the entity list size
     * to stay under the 50% threshold for incremental updates.
     */
    @Provide
    Arbitrary<Set<Integer>> mutationIndices() {
        // Will be filtered in the test method based on actual entity list size
        return Arbitraries.integers().between(0, 19)
                .set().ofMinSize(1).ofMaxSize(9);
    }

    /**
     * Generates new names for mutated entities.
     */
    @Provide
    Arbitrary<List<String>> newNames() {
        return Arbitraries.of(NAME_POOL).list().ofMinSize(1).ofMaxSize(10);
    }

    // ==================== Helpers ====================

    /**
     * Creates an IndexDefinition for User indexed by name, using addKeyFieldWithPath
     * to avoid SpecificationServices dependency.
     */
    private IndexDefinition<User> createNameIndexDefinition() {
        StringAttribute<User> nameAttr = new StringAttribute<>("name", User.class);
        return IndexDefinition.builder(User.class)
                .addKeyFieldWithPath(
                        Collections.singletonList(nameAttr),
                        User::getName)
                .build();
    }

    /**
     * Creates a DependencyGraph populated with the given entities and a name index.
     */
    private DependencyGraph createGraphWithEntities(List<User> entities, IndexDefinition<User> indexDef) {
        DependencyGraph graph = new DependencyGraph();
        graph.upsertAll(DS_NAME, entities);
        graph.registerIndex(DS_NAME, indexDef);
        return graph;
    }

    /**
     * Collects all indexed values from a DependencyGraph by looking up every
     * distinct name in the entity set. Returns a sorted list of (id, name) pairs
     * for deterministic comparison.
     */
    private List<String> collectIndexState(DependencyGraph graph, IndexDefinition<User> indexDef,
                                           List<User> allEntities) {
        Set<String> allNames = new HashSet<>();
        for (User u : allEntities) {
            if (u.getName() != null) {
                allNames.add(u.getName());
            }
        }

        List<String> entries = new ArrayList<>();
        for (String name : allNames) {
            List<User> found = graph.lookup(DS_NAME, indexDef, name);
            for (User u : found) {
                entries.add(u.getIdentity() + ":" + u.getName());
            }
        }
        Collections.sort(entries);
        return entries;
    }

    // ==================== Property Tests ====================

    /**
     * Property 3: Incremental Index Equivalence
     *
     * <p>For any entity set and mutated subset, incremental index update via
     * {@code updateIndexesForEntities()} produces the same index state as
     * full rebuild via {@code rebuildIndexesForDataSource()}.</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 6.2</b></p>
     */
    @Property(tries = 100)
    void incrementalUpdateProducesSameIndexStateAsFullRebuild(
            @ForAll("userSets") List<User> initialUsers,
            @ForAll("mutationIndices") Set<Integer> rawMutationIndices,
            @ForAll("newNames") List<String> rawNewNames) {

        // Constrain mutation indices to valid range and <= 49% of total
        int maxMutations = Math.max(1, (int) (initialUsers.size() * 0.49));
        List<Integer> validIndices = rawMutationIndices.stream()
                .filter(i -> i >= 0 && i < initialUsers.size())
                .limit(maxMutations)
                .collect(Collectors.toList());

        if (validIndices.isEmpty()) {
            return; // Skip if no valid mutations
        }

        // Create the index definition (same instance for both graphs)
        IndexDefinition<User> indexDef = createNameIndexDefinition();

        // Set up two identical graphs with initial entities
        DependencyGraph incrementalGraph = createGraphWithEntities(initialUsers, indexDef);
        DependencyGraph rebuildGraph = createGraphWithEntities(initialUsers, indexDef);

        // Create mutated entities: change the name of selected entities
        List<User> mutatedEntities = new ArrayList<>();
        for (int i = 0; i < validIndices.size(); i++) {
            int entityIdx = validIndices.get(i);
            User original = initialUsers.get(entityIdx);
            User mutated = new User();
            mutated.setId(original.getId());
            // Pick a new name (cycling through rawNewNames)
            String newName = rawNewNames.get(i % rawNewNames.size());
            mutated.setName(newName);
            mutatedEntities.add(mutated);
        }

        // Upsert mutated entities into both graphs (update entity store)
        for (User mutated : mutatedEntities) {
            incrementalGraph.upsert(DS_NAME, mutated);
            rebuildGraph.upsert(DS_NAME, mutated);
        }

        // Path A: Incremental update
        boolean incrementalSuccess = incrementalGraph.updateIndexesForEntities(DS_NAME, mutatedEntities);
        if (!incrementalSuccess) {
            // Threshold exceeded — fall back to rebuild (both paths become rebuild)
            incrementalGraph.rebuildIndexesForDataSource(DS_NAME);
        }

        // Path B: Full rebuild
        rebuildGraph.rebuildIndexesForDataSource(DS_NAME);

        // Collect final entity list (same for both graphs after upserts)
        List<User> finalEntities = incrementalGraph.findAll(DS_NAME);

        // Compare index states
        List<String> incrementalState = collectIndexState(incrementalGraph, indexDef, finalEntities);
        List<String> rebuildState = collectIndexState(rebuildGraph, indexDef, finalEntities);

        assertThat(incrementalState)
                .as("Incremental index update must produce the same index state as full rebuild "
                        + "(entities=%d, mutations=%d)", initialUsers.size(), mutatedEntities.size())
                .isEqualTo(rebuildState);
    }

    /**
     * Property 3 (multi-level variant): Incremental Index Equivalence with 2-level index.
     *
     * <p>Verifies the same equivalence property holds for multi-level indexes
     * (indexed by both id and name).</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 6.2</b></p>
     */
    @Property(tries = 100)
    void incrementalUpdateEquivalentToRebuildForMultiLevelIndex(
            @ForAll("userSets") List<User> initialUsers,
            @ForAll("mutationIndices") Set<Integer> rawMutationIndices,
            @ForAll("newNames") List<String> rawNewNames) {

        int maxMutations = Math.max(1, (int) (initialUsers.size() * 0.49));
        List<Integer> validIndices = rawMutationIndices.stream()
                .filter(i -> i >= 0 && i < initialUsers.size())
                .limit(maxMutations)
                .collect(Collectors.toList());

        if (validIndices.isEmpty()) {
            return;
        }

        // 2-level index: id + name
        StringAttribute<User> idAttr = new StringAttribute<>("id", User.class);
        StringAttribute<User> nameAttr = new StringAttribute<>("name", User.class);
        IndexDefinition<User> indexDef = IndexDefinition.builder(User.class)
                .addKeyFieldWithPath(
                        Collections.singletonList(idAttr),
                        User::getId)
                .addKeyFieldWithPath(
                        Collections.singletonList(nameAttr),
                        User::getName)
                .build();

        DependencyGraph incrementalGraph = createGraphWithEntities(initialUsers, indexDef);
        DependencyGraph rebuildGraph = createGraphWithEntities(initialUsers, indexDef);

        // Create mutated entities
        List<User> mutatedEntities = new ArrayList<>();
        for (int i = 0; i < validIndices.size(); i++) {
            int entityIdx = validIndices.get(i);
            User original = initialUsers.get(entityIdx);
            User mutated = new User();
            mutated.setId(original.getId());
            mutated.setName(rawNewNames.get(i % rawNewNames.size()));
            mutatedEntities.add(mutated);
        }

        for (User mutated : mutatedEntities) {
            incrementalGraph.upsert(DS_NAME, mutated);
            rebuildGraph.upsert(DS_NAME, mutated);
        }

        boolean incrementalSuccess = incrementalGraph.updateIndexesForEntities(DS_NAME, mutatedEntities);
        if (!incrementalSuccess) {
            incrementalGraph.rebuildIndexesForDataSource(DS_NAME);
        }

        rebuildGraph.rebuildIndexesForDataSource(DS_NAME);

        // Compare using getAllValues via lookup of all entities
        List<User> finalEntities = incrementalGraph.findAll(DS_NAME);

        // For 2-level index, look up each entity by (id, name)
        List<String> incrementalState = new ArrayList<>();
        List<String> rebuildState = new ArrayList<>();

        for (User u : finalEntities) {
            if (u.getId() != null && u.getName() != null) {
                List<User> incResult = incrementalGraph.lookup(DS_NAME, indexDef, u.getId(), u.getName());
                List<User> rebResult = rebuildGraph.lookup(DS_NAME, indexDef, u.getId(), u.getName());

                for (User r : incResult) {
                    incrementalState.add(r.getIdentity() + ":" + r.getName());
                }
                for (User r : rebResult) {
                    rebuildState.add(r.getIdentity() + ":" + r.getName());
                }
            }
        }

        Collections.sort(incrementalState);
        Collections.sort(rebuildState);

        assertThat(incrementalState)
                .as("Multi-level incremental index update must produce same state as full rebuild "
                        + "(entities=%d, mutations=%d)", initialUsers.size(), mutatedEntities.size())
                .isEqualTo(rebuildState);
    }
}
