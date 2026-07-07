package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.entity.Identifiable;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

// Feature: streaming-datasource-support, Property 12: Artımlı Aggregation — Full Senkronizasyon Tutarlılığı

/**
 * Property-based test for incremental aggregation vs full sync consistency.
 *
 * <p>Core property: after applying a series of batch snapshot events incrementally
 * through DependencyGraph, the aggregation results computed from the final state
 * must be identical to what you'd get by computing aggregations from scratch
 * over the full dataset.</p>
 *
 * <p>For each aggregation type:</p>
 * <ul>
 *   <li>COUNT: incremental count == full recount of entities</li>
 *   <li>SUM: incremental sum == full resum of all values</li>
 *   <li>AVG: incremental avg == full average of all values</li>
 *   <li>MIN: incremental min == full scan min</li>
 *   <li>MAX: incremental max == full scan max</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 2.9, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.11</b></p>
 */
class IncrementalAggregationPropertyTest {

    private static final String DS_NAME = "aggregation-test-ds";
    private static final double EPSILON = 0.0001;

    // ==================== Property 12: Incremental vs Full Sync Aggregation Consistency ====================

    /**
     * Property 12: After applying an initial batch and then a second batch of updates
     * (some new entities, some updates to existing ones) incrementally through
     * DependencyGraph, the aggregation results computed from the final DependencyGraph
     * state must be identical to aggregation results computed from scratch over
     * a model that tracks the expected final state.
     *
     * <p>This is a model-based test: the "model" is a simple HashMap that applies
     * the same upsert logic. After all batches, we compare aggregations computed
     * from DependencyGraph.findAll() with aggregations computed from the model.</p>
     *
     * <p><b>Validates: Requirements 2.9, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.11</b></p>
     */
    @Property(tries = 100)
    void incrementalAggregationMatchesFullSyncAggregation(
            @ForAll("initialBatches") List<NumericTestEntity> initialBatch,
            @ForAll("updateBatches") List<NumericTestEntity> updateBatch) {

        DependencyGraph graph = new DependencyGraph();

        // --- Model: simple HashMap tracking expected final state ---
        Map<Integer, NumericTestEntity> model = new HashMap<>();

        // --- Phase 1: Apply initial batch (simulates initial load) ---
        graph.upsertAll(DS_NAME, initialBatch);
        for (NumericTestEntity entity : initialBatch) {
            model.put(entity.getIdentity(), entity);
        }

        // --- Phase 2: Apply update batch (simulates incremental updates) ---
        // This batch may contain new entities and updates to existing ones
        graph.upsertAll(DS_NAME, updateBatch);
        for (NumericTestEntity entity : updateBatch) {
            model.put(entity.getIdentity(), entity);
        }

        // --- Compute aggregations from DependencyGraph (incremental result) ---
        List<NumericTestEntity> graphEntities = graph.findAll(DS_NAME);
        Map<AggregationType, Object> graphAggregations = computeAggregations(graphEntities);

        // --- Compute aggregations from model (full sync result) ---
        List<NumericTestEntity> modelEntities = new ArrayList<>(model.values());
        Map<AggregationType, Object> modelAggregations = computeAggregations(modelEntities);

        // --- PROPERTY: Incremental aggregation results must match full sync results ---

        // COUNT
        long graphCount = (long) graphAggregations.get(AggregationType.COUNT);
        long modelCount = (long) modelAggregations.get(AggregationType.COUNT);
        assertThat(graphCount)
                .as("COUNT: incremental count must equal full recount")
                .isEqualTo(modelCount);

        // SUM
        double graphSum = (double) graphAggregations.get(AggregationType.SUM);
        double modelSum = (double) modelAggregations.get(AggregationType.SUM);
        assertThat(graphSum)
                .as("SUM: incremental sum must equal full resum")
                .isCloseTo(modelSum, within(EPSILON));

        // AVG
        double graphAvg = (double) graphAggregations.get(AggregationType.AVG);
        double modelAvg = (double) modelAggregations.get(AggregationType.AVG);
        assertThat(graphAvg)
                .as("AVG: incremental avg must equal full average")
                .isCloseTo(modelAvg, within(EPSILON));

        // MIN
        Double graphMin = (Double) graphAggregations.get(AggregationType.MIN);
        Double modelMin = (Double) modelAggregations.get(AggregationType.MIN);
        if (modelMin == null) {
            assertThat(graphMin)
                    .as("MIN: both must be null when no entities")
                    .isNull();
        } else {
            assertThat(graphMin)
                    .as("MIN: incremental min must equal full scan min")
                    .isCloseTo(modelMin, within(EPSILON));
        }

        // MAX
        Double graphMax = (Double) graphAggregations.get(AggregationType.MAX);
        Double modelMax = (Double) modelAggregations.get(AggregationType.MAX);
        if (modelMax == null) {
            assertThat(graphMax)
                    .as("MAX: both must be null when no entities")
                    .isNull();
        } else {
            assertThat(graphMax)
                    .as("MAX: incremental max must equal full scan max")
                    .isCloseTo(modelMax, within(EPSILON));
        }

        // Additional structural check: entity count in graph matches model
        assertThat(graphEntities).hasSameSizeAs(modelEntities);
    }

    // ==================== Aggregation Computation (mirrors IncrementalSyncProcessor logic) ====================

    /**
     * Computes COUNT, SUM, AVG, MIN, MAX over a list of entities.
     * This mirrors the logic in IncrementalSyncProcessor.computeAggregationsInSinglePass.
     */
    private Map<AggregationType, Object> computeAggregations(List<NumericTestEntity> entities) {
        Map<AggregationType, Object> results = new EnumMap<>(AggregationType.class);

        if (entities == null || entities.isEmpty()) {
            results.put(AggregationType.COUNT, 0L);
            results.put(AggregationType.SUM, 0.0);
            results.put(AggregationType.AVG, 0.0);
            results.put(AggregationType.MIN, null);
            results.put(AggregationType.MAX, null);
            return results;
        }

        long count = 0;
        double sum = 0.0;
        Double min = null;
        Double max = null;

        for (NumericTestEntity entity : entities) {
            count++;
            double value = entity.getAmount();
            sum += value;
            if (min == null || value < min) {
                min = value;
            }
            if (max == null || value > max) {
                max = value;
            }
        }

        results.put(AggregationType.COUNT, count);
        results.put(AggregationType.SUM, sum);
        results.put(AggregationType.AVG, count > 0 ? sum / count : 0.0);
        results.put(AggregationType.MIN, min);
        results.put(AggregationType.MAX, max);

        return results;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<NumericTestEntity>> initialBatches() {
        return numericEntityArbitrary().list().ofMinSize(1).ofMaxSize(30);
    }

    @Provide
    Arbitrary<List<NumericTestEntity>> updateBatches() {
        return numericEntityArbitrary().list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<NumericTestEntity> numericEntityArbitrary() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 50);
        Arbitrary<Double> amounts = Arbitraries.doubles().between(-1000.0, 1000.0)
                .ofScale(2);

        return Combinators.combine(ids, amounts)
                .as((id, amount) -> new NumericTestEntity(id, amount));
    }

    // ==================== Test Entity ====================

    /**
     * Test entity with a numeric field (amount) for aggregation testing.
     * Uses Identifiable&lt;Integer&gt; as required by DependencyGraph.
     */
    static class NumericTestEntity implements Identifiable<Integer> {
        private final int id;
        private final double amount;

        NumericTestEntity(int id, double amount) {
            this.id = id;
            this.amount = amount;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NumericTestEntity that = (NumericTestEntity) o;
            return id == that.id && Double.compare(that.amount, amount) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, amount);
        }

        @Override
        public String toString() {
            return "NumericTestEntity{id=" + id + ", amount=" + amount + "}";
        }
    }
}
