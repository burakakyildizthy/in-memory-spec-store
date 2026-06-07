package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * Property-based test for Mutation Rate Threshold Control.
 *
 * <p>Verifies that for any entity set and mutation count, when the mutation rate
 * exceeds 50% of total entities, {@code updateIndexesForEntities()} returns {@code false}
 * (signalling full rebuild), and when at or below 50%, it returns {@code true}
 * (incremental update succeeds).</p>
 *
 * <p><b>Validates: Requirement 2.4</b></p>
 */
class MutationThresholdStrategyPropertyTest {

    private static final String DS_NAME = "test-ds";
    private static final List<String> NAME_POOL = List.of(
            "Alice", "Bob", "Charlie", "Diana", "Eve",
            "Frank", "Grace", "Hank", "Ivy", "Jack");

    // ==================== Test Data Record ====================

    /**
     * Holds a generated test scenario: total entity count and mutation count.
     */
    record ThresholdScenario(int totalCount, int mutationCount) {
        double mutationRate() {
            return (double) mutationCount / totalCount;
        }

        boolean expectsFullRebuild() {
            return mutationRate() > 0.50;
        }
    }

    // ==================== Arbitraries ====================

    /**
     * Generates scenarios where mutation rate is strictly above 50% (expects full rebuild / false).
     * Total count: 2–50, mutation count: strictly more than half.
     */
    @Provide
    Arbitrary<ThresholdScenario> aboveThresholdScenarios() {
        return Arbitraries.integers().between(2, 50).flatMap(total -> {
            int minMutations = (int) Math.floor(total * 0.50) + 1;
            if (minMutations > total) {
                minMutations = total;
            }
            return Arbitraries.integers().between(minMutations, total)
                    .map(mutations -> new ThresholdScenario(total, mutations));
        });
    }

    /**
     * Generates scenarios where mutation rate is at or below 50% (expects incremental / true).
     * Total count: 2–50, mutation count: at most half.
     */
    @Provide
    Arbitrary<ThresholdScenario> atOrBelowThresholdScenarios() {
        return Arbitraries.integers().between(2, 50).flatMap(total -> {
            int maxMutations = (int) Math.floor(total * 0.50);
            if (maxMutations < 1) {
                maxMutations = 1;
            }
            return Arbitraries.integers().between(1, maxMutations)
                    .map(mutations -> new ThresholdScenario(total, mutations));
        });
    }

    /**
     * Generates any valid scenario with random total (2–50) and mutation (1–total) counts.
     */
    @Provide
    Arbitrary<ThresholdScenario> anyThresholdScenarios() {
        return Arbitraries.integers().between(2, 50).flatMap(total ->
                Arbitraries.integers().between(1, total)
                        .map(mutations -> new ThresholdScenario(total, mutations)));
    }

    // ==================== Helpers ====================

    private IndexDefinition<User> createNameIndexDefinition() {
        StringAttribute<User> nameAttr = new StringAttribute<>("name", User.class);
        return IndexDefinition.builder(User.class)
                .addKeyFieldWithPath(
                        Collections.singletonList(nameAttr),
                        User::getName)
                .build();
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User u = new User();
            u.setId(String.valueOf(i + 1));
            u.setName(NAME_POOL.get(i % NAME_POOL.size()));
            users.add(u);
        }
        return users;
    }

    private DependencyGraph createGraphWithEntities(List<User> entities, IndexDefinition<User> indexDef) {
        DependencyGraph graph = new DependencyGraph();
        graph.upsertAll(DS_NAME, entities);
        graph.registerIndex(DS_NAME, indexDef);
        return graph;
    }

    private List<User> createMutatedSubset(List<User> allEntities, int mutationCount) {
        List<User> mutated = new ArrayList<>();
        for (int i = 0; i < mutationCount; i++) {
            User original = allEntities.get(i);
            User m = new User();
            m.setId(original.getId());
            m.setName(original.getName() + "_mutated");
            mutated.add(m);
        }
        return mutated;
    }

    // ==================== Property Tests ====================

    /**
     * Property 4: Mutation Rate Threshold Control
     *
     * <p>For any entity set and mutation count, when the mutation rate is strictly
     * above 50%, {@code updateIndexesForEntities()} returns {@code false} (full rebuild).
     * When at or below 50%, it returns {@code true} (incremental update).</p>
     *
     * <p><b>Validates: Requirement 2.4</b></p>
     */
    @Property(tries = 100)
    void aboveThresholdTriggersFullRebuild(
            @ForAll("aboveThresholdScenarios") ThresholdScenario scenario) {

        IndexDefinition<User> indexDef = createNameIndexDefinition();
        List<User> allEntities = createUsers(scenario.totalCount());
        DependencyGraph graph = createGraphWithEntities(allEntities, indexDef);

        List<User> mutated = createMutatedSubset(allEntities, scenario.mutationCount());

        // Upsert mutated entities into the graph before calling updateIndexesForEntities
        for (User m : mutated) {
            graph.upsert(DS_NAME, m);
        }

        boolean result = graph.updateIndexesForEntities(DS_NAME, mutated);

        assertThat(result)
                .as("Mutation rate %.2f (%d/%d) > 0.50 should trigger full rebuild (return false)",
                        scenario.mutationRate(), scenario.mutationCount(), scenario.totalCount())
                .isFalse();
    }

    /**
     * Property 4: Mutation Rate Threshold Control (at or below threshold)
     *
     * <p>When mutation rate is at or below 50%, incremental update should succeed
     * and return {@code true}.</p>
     *
     * <p><b>Validates: Requirement 2.4</b></p>
     */
    @Property(tries = 100)
    void atOrBelowThresholdTriggersIncrementalUpdate(
            @ForAll("atOrBelowThresholdScenarios") ThresholdScenario scenario) {

        IndexDefinition<User> indexDef = createNameIndexDefinition();
        List<User> allEntities = createUsers(scenario.totalCount());
        DependencyGraph graph = createGraphWithEntities(allEntities, indexDef);

        List<User> mutated = createMutatedSubset(allEntities, scenario.mutationCount());

        for (User m : mutated) {
            graph.upsert(DS_NAME, m);
        }

        boolean result = graph.updateIndexesForEntities(DS_NAME, mutated);

        assertThat(result)
                .as("Mutation rate %.2f (%d/%d) <= 0.50 should trigger incremental update (return true)",
                        scenario.mutationRate(), scenario.mutationCount(), scenario.totalCount())
                .isTrue();
    }

    /**
     * Property 4: Mutation Rate Threshold Control (general property)
     *
     * <p>For any random mutation rate, the return value of {@code updateIndexesForEntities()}
     * must match the threshold rule: {@code rate > 0.50 → false, rate <= 0.50 → true}.</p>
     *
     * <p><b>Validates: Requirement 2.4</b></p>
     */
    @Property(tries = 100)
    void thresholdDecisionMatchesMutationRate(
            @ForAll("anyThresholdScenarios") ThresholdScenario scenario) {

        IndexDefinition<User> indexDef = createNameIndexDefinition();
        List<User> allEntities = createUsers(scenario.totalCount());
        DependencyGraph graph = createGraphWithEntities(allEntities, indexDef);

        List<User> mutated = createMutatedSubset(allEntities, scenario.mutationCount());

        for (User m : mutated) {
            graph.upsert(DS_NAME, m);
        }

        boolean result = graph.updateIndexesForEntities(DS_NAME, mutated);
        boolean expectedIncremental = !scenario.expectsFullRebuild();

        assertThat(result)
                .as("Mutation rate %.2f (%d/%d): expected %s but got %s",
                        scenario.mutationRate(), scenario.mutationCount(), scenario.totalCount(),
                        expectedIncremental ? "incremental (true)" : "full rebuild (false)",
                        result ? "incremental (true)" : "full rebuild (false)")
                .isEqualTo(expectedIncremental);
    }
}
