package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/**
 * Property-based test for precomputed store mapping grouping equivalence.
 *
 * <p>Verifies that after any sequence of mapping add/remove operations,
 * the precomputed store mapping groupings ({@code getStoreMappings()}) produce
 * the same result as runtime computation via
 * {@code stream().filter(!isForDashboard).collect(groupingBy(getConsumerId))}.</p>
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.4</b></p>
 */
class StoreMappingRegistryEquivalencePropertyTest {

    // ==================== Test Data Model ====================

    /**
     * Represents a single add or remove operation on a mapping.
     */
    record MappingOperation(boolean isAdd, int mappingIndex) {}

    /**
     * Represents a mapping's identity for generation purposes.
     */
    record MappingSpec(String consumerId, String dataSourceName, boolean isForDashboard) {}

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<String> consumerIds() {
        return Arbitraries.of("store-A", "store-B", "store-C", "store-D");
    }

    @Provide
    Arbitrary<String> dataSourceNames() {
        return Arbitraries.of("ds-orders", "ds-products", "ds-customers", "ds-inventory");
    }

    @Provide
    Arbitrary<MappingSpec> mappingSpecs() {
        // ~30% dashboard mappings via frequency weighting
        Arbitrary<Boolean> dashboardFlag = Arbitraries.frequency(
                Tuple.of(3, true),
                Tuple.of(7, false)
        );
        return Combinators.combine(
                consumerIds(),
                dataSourceNames(),
                dashboardFlag
        ).as(MappingSpec::new);
    }

    /**
     * Generates a list of mapping specs (the "pool" of mappings).
     */
    @Provide
    Arbitrary<List<MappingSpec>> mappingPools() {
        return mappingSpecs().list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<MappingOperation>> operationSequences() {
        // ~60% adds, ~40% removes
        Arbitrary<Boolean> isAddFlag = Arbitraries.frequency(
                Tuple.of(6, true),
                Tuple.of(4, false)
        );
        Arbitrary<MappingOperation> singleOp = Combinators.combine(
                isAddFlag,
                Arbitraries.integers().between(0, 9)
        ).as(MappingOperation::new);
        return singleOp.list().ofMinSize(1).ofMaxSize(30);
    }

    // ==================== Helpers ====================

    /**
     * Creates a mock PropertyMapping with the given spec.
     * Each call returns a distinct mock instance, but with consistent behavior.
     */
    private PropertyMapping<?, ?> createMockMapping(MappingSpec spec) {
        PropertyMapping<?, ?> mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(spec.consumerId());
        when(mapping.getDataSourceName()).thenReturn(spec.dataSourceName());
        when(mapping.isForDashboard()).thenReturn(spec.isForDashboard());
        when(mapping.toString()).thenReturn(
                "MockMapping[consumer=" + spec.consumerId()
                        + ", ds=" + spec.dataSourceName()
                        + ", dashboard=" + spec.isForDashboard() + "]");
        return mapping;
    }

    /**
     * Creates an IncrementalSyncProcessor with mocked dependencies.
     * The constructor calls registerMappingIndexes() which needs factory.getAllStoreIds()
     * and dependencyGraph.getMappingsByConsumerId() — we mock these to return empty results.
     */
    private IncrementalSyncProcessor createProcessor() {
        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);
        DependencyGraph dependencyGraph = mock(DependencyGraph.class);
        when(factory.getAllStoreIds()).thenReturn(Collections.emptyList());
        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
        return new IncrementalSyncProcessor(factory, dependencyGraph, analysisResult, new AtomicLong(0));
    }

    /**
     * Computes the expected runtime grouping from a truth list of active mappings.
     * This is the "oracle": filter out dashboard mappings, group by consumerId.
     */
    private Map<String, List<PropertyMapping<?, ?>>> computeRuntimeGrouping(
            List<PropertyMapping<?, ?>> activeMappings) {
        return activeMappings.stream()
                .filter(m -> !m.isForDashboard())
                .collect(Collectors.groupingBy(PropertyMapping::getConsumerId));
    }

    // ==================== Property Tests ====================

    /**
     * Property 6: Precomputed Mapping Grouping Equivalence
     *
     * <p>For any sequence of mapping add/remove operations, the precomputed
     * store mapping groupings must equal the runtime-computed grouping
     * (filter non-dashboard, group by consumerId) of the truth list.</p>
     *
     * <p><b>Validates: Requirements 4.1, 4.2, 4.4</b></p>
     */
    @Property(tries = 100)
    void precomputedGroupingEqualsRuntimeGroupingAfterOperations(
            @ForAll("mappingPools") List<MappingSpec> pool,
            @ForAll("operationSequences") List<MappingOperation> operations) {

        IncrementalSyncProcessor processor = createProcessor();

        // Truth list: tracks all currently active mappings
        List<PropertyMapping<?, ?>> activeMappings = new ArrayList<>();

        for (MappingOperation op : operations) {
            int idx = op.mappingIndex() % pool.size();

            if (op.isAdd()) {
                PropertyMapping<?, ?> mapping = createMockMapping(pool.get(idx));
                activeMappings.add(mapping);
                processor.onMappingAdded(mapping);
            } else {
                // Remove: find an active mapping with the same spec and remove it
                MappingSpec targetSpec = pool.get(idx);
                PropertyMapping<?, ?> toRemove = null;
                for (PropertyMapping<?, ?> m : activeMappings) {
                    if (m.getConsumerId().equals(targetSpec.consumerId())
                            && m.getDataSourceName().equals(targetSpec.dataSourceName())
                            && m.isForDashboard() == targetSpec.isForDashboard()) {
                        toRemove = m;
                        break;
                    }
                }
                if (toRemove != null) {
                    activeMappings.remove(toRemove);
                    processor.onMappingRemoved(toRemove);
                }
            }
        }

        // Compare precomputed vs. runtime grouping
        Map<String, List<PropertyMapping<?, ?>>> precomputed = processor.getStoreMappings();
        Map<String, List<PropertyMapping<?, ?>>> runtimeGrouping = computeRuntimeGrouping(activeMappings);

        // Both should have the same keys
        assertThat(precomputed.keySet())
                .as("Precomputed and runtime groupings must have the same store IDs")
                .isEqualTo(runtimeGrouping.keySet());

        // For each key, the lists should have the same size and same elements (order may differ)
        for (String storeId : runtimeGrouping.keySet()) {
            List<PropertyMapping<?, ?>> precomputedList = precomputed.get(storeId);
            List<PropertyMapping<?, ?>> runtimeList = runtimeGrouping.get(storeId);

            assertThat(precomputedList)
                    .as("Precomputed list for store '%s' must have same size as runtime list", storeId)
                    .hasSameSizeAs(runtimeList);

            // Verify same elements by identity (same mock instances)
            assertThat(new HashSet<>(precomputedList))
                    .as("Precomputed mappings for store '%s' must contain same elements as runtime", storeId)
                    .isEqualTo(new HashSet<>(runtimeList));
        }
    }

    /**
     * Verifies that dashboard mappings are always excluded from precomputed groupings,
     * regardless of the add/remove sequence.
     *
     * <p><b>Validates: Requirements 4.1, 4.2, 4.4</b></p>
     */
    @Property(tries = 100)
    void dashboardMappingsAreAlwaysExcluded(
            @ForAll("mappingPools") List<MappingSpec> pool,
            @ForAll("operationSequences") List<MappingOperation> operations) {

        IncrementalSyncProcessor processor = createProcessor();
        List<PropertyMapping<?, ?>> activeMappings = new ArrayList<>();

        for (MappingOperation op : operations) {
            int idx = op.mappingIndex() % pool.size();

            if (op.isAdd()) {
                PropertyMapping<?, ?> mapping = createMockMapping(pool.get(idx));
                activeMappings.add(mapping);
                processor.onMappingAdded(mapping);
            } else {
                MappingSpec targetSpec = pool.get(idx);
                PropertyMapping<?, ?> toRemove = null;
                for (PropertyMapping<?, ?> m : activeMappings) {
                    if (m.getConsumerId().equals(targetSpec.consumerId())
                            && m.getDataSourceName().equals(targetSpec.dataSourceName())
                            && m.isForDashboard() == targetSpec.isForDashboard()) {
                        toRemove = m;
                        break;
                    }
                }
                if (toRemove != null) {
                    activeMappings.remove(toRemove);
                    processor.onMappingRemoved(toRemove);
                }
            }
        }

        Map<String, List<PropertyMapping<?, ?>>> precomputed = processor.getStoreMappings();

        // No mapping in the precomputed result should be a dashboard mapping
        for (Map.Entry<String, List<PropertyMapping<?, ?>>> entry : precomputed.entrySet()) {
            for (PropertyMapping<?, ?> m : entry.getValue()) {
                assertThat(m.isForDashboard())
                        .as("Dashboard mapping should never appear in precomputed store mappings "
                                + "(store='%s', mapping=%s)", entry.getKey(), m)
                        .isFalse();
            }
        }
    }
}
