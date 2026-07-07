package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

// Feature: streaming-datasource-support, Property 15: Datasource Bazında Event Sıralama Korunumu ve PK/FK Mapping Tespiti

/**
 * Property-based tests for per-datasource event ordering preservation and
 * PK/FK mapping detection via {@link DependencyGraph}.
 *
 * <p>Two properties are tested:</p>
 * <ol>
 *   <li><b>Event ordering preservation per datasource</b>: When multiple
 *       BatchSnapshotEvents from different datasources are interleaved and
 *       processed, the final state in DependencyGraph must reflect the last
 *       event's data for each datasource (FIFO / last-write-wins). Events
 *       from different datasources must not interfere with each other.</li>
 *   <li><b>PK/FK mapping detection</b>: When DependencyGraph is built with
 *       PropertyMappings, {@code getMappingsForDataSource()} must return all
 *       mappings referencing that datasource and must NOT return mappings
 *       referencing other datasources.</li>
 * </ol>
 *
 * <p><b>Validates: Requirements 5.6, 5.8, 11.3</b></p>
 */
class DatasourceOrderingAndMappingPropertyTest {

    private static final String DS_A = "ds-A";
    private static final String DS_B = "ds-B";
    private static final String DS_C = "ds-C";

    // ==================== Property 15a: Event Ordering Preservation Per Datasource ====================

    /**
     * Property 15a: For any interleaved sequence of BatchSnapshotEvents from
     * multiple datasources (ds-A, ds-B), when all events are queued and then
     * processed, the final state in DependencyGraph for each datasource must
     * reflect the last event's data (FIFO order / last-write-wins within each
     * datasource). Events from ds-A must not affect ds-B's data and vice versa.
     *
     * <p>Test approach:</p>
     * <ol>
     *   <li>Generate events for ds-A and ds-B with overlapping entity IDs</li>
     *   <li>Interleave them in a single queue</li>
     *   <li>Process all queued events via processQueuedEvents()</li>
     *   <li>Verify each datasource's final state reflects its own last event's data</li>
     *   <li>Verify cross-datasource isolation</li>
     * </ol>
     *
     * <p><b>Validates: Requirements 5.6, 5.8, 11.3</b></p>
     */
    @Property(tries = 100)
    void eventOrderingPreservedPerDatasourceWithNoInterference(
            @ForAll("interleavedEventSequences") List<TaggedBatch> taggedBatches) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register both streaming datasources without TimeWindowRule
            registerStreamingDs(factory, DS_A);
            registerStreamingDs(factory, DS_B);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Model: track expected final state per datasource (last-write-wins)
            Map<String, Map<Integer, OrderTestEntity>> model = new HashMap<>();
            model.put(DS_A, new HashMap<>());
            model.put(DS_B, new HashMap<>());

            // Queue all events in the interleaved order
            for (TaggedBatch tb : taggedBatches) {
                BatchSnapshotEvent<OrderTestEntity> event = new BatchSnapshotEvent<>(
                        tb.entities, Instant.now());
                processor.queueEvent(tb.dsName, event);

                // Apply last-write-wins to model for this datasource
                Map<Integer, OrderTestEntity> dsModel = model.get(tb.dsName);
                for (OrderTestEntity entity : tb.entities) {
                    dsModel.put(entity.getIdentity(), entity);
                }
            }

            // Process all queued events
            processor.processQueuedEvents();

            // --- PROPERTY: Per-datasource final state reflects last event's data ---
            for (String dsName : List.of(DS_A, DS_B)) {
                Map<Integer, OrderTestEntity> expectedState = model.get(dsName);
                List<OrderTestEntity> storedEntities = graph.findAll(dsName);

                assertThat(storedEntities)
                        .as("Datasource '%s' must have exactly %d unique entities",
                                dsName, expectedState.size())
                        .hasSize(expectedState.size());

                for (Map.Entry<Integer, OrderTestEntity> entry : expectedState.entrySet()) {
                    OrderTestEntity found = graph.findById(dsName, entry.getKey());
                    assertThat(found)
                            .as("Entity id=%d in '%s' must exist", entry.getKey(), dsName)
                            .isNotNull();
                    assertThat(found.getValue())
                            .as("Entity id=%d in '%s' must have last-write-wins value",
                                    entry.getKey(), dsName)
                            .isEqualTo(entry.getValue().getValue());
                }
            }

            // --- PROPERTY: Cross-datasource isolation ---
            // Entities from ds-A must not appear in ds-B's store and vice versa
            Map<Integer, OrderTestEntity> dsAModel = model.get(DS_A);
            Map<Integer, OrderTestEntity> dsBModel = model.get(DS_B);

            for (Integer idInA : dsAModel.keySet()) {
                // If this ID is NOT in ds-B's model, it must not be found in ds-B's store
                if (!dsBModel.containsKey(idInA)) {
                    OrderTestEntity foundInB = graph.findById(DS_B, idInA);
                    assertThat(foundInB)
                            .as("Entity id=%d from ds-A must NOT appear in ds-B", idInA)
                            .isNull();
                }
            }

            for (Integer idInB : dsBModel.keySet()) {
                if (!dsAModel.containsKey(idInB)) {
                    OrderTestEntity foundInA = graph.findById(DS_A, idInB);
                    assertThat(foundInA)
                            .as("Entity id=%d from ds-B must NOT appear in ds-A", idInB)
                            .isNull();
                }
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Property 15b: PK/FK Mapping Detection ====================

    /**
     * Property 15b: For any set of PropertyMappings built into DependencyGraph,
     * {@code getMappingsForDataSource(dsName)} must return ALL mappings that
     * reference that datasource and must NOT return mappings that reference
     * other datasources.
     *
     * <p>Test approach:</p>
     * <ol>
     *   <li>Generate random PropertyMappings across multiple datasources</li>
     *   <li>Build DependencyGraph with these mappings</li>
     *   <li>For each datasource, verify getMappingsForDataSource() returns
     *       exactly the correct mappings</li>
     *   <li>Verify that a non-existent datasource returns empty</li>
     * </ol>
     *
     * <p><b>Validates: Requirements 5.6, 5.8, 11.3</b></p>
     */
    @Property(tries = 100)
    void getMappingsForDataSourceReturnsExactlyRelevantMappings(
            @ForAll("mappingDescriptors") List<MappingDescriptor> descriptors) {

        DependencyGraph graph = new DependencyGraph();
        List<PropertyMapping<?, ?>> allMappings = new ArrayList<>();

        for (MappingDescriptor desc : descriptors) {
            PropertyMapping<?, ?> mockMapping = createMockMapping(
                    desc.dataSourceName, desc.consumerId, desc.forDashboard);
            allMappings.add(mockMapping);
        }

        graph.build(allMappings);

        // For each datasource used in descriptors, verify correct mappings returned
        Map<String, List<MappingDescriptor>> expectedByDs = new HashMap<>();
        for (MappingDescriptor desc : descriptors) {
            expectedByDs.computeIfAbsent(desc.dataSourceName, k -> new ArrayList<>()).add(desc);
        }

        for (Map.Entry<String, List<MappingDescriptor>> entry : expectedByDs.entrySet()) {
            String dsName = entry.getKey();
            List<MappingDescriptor> expectedDescs = entry.getValue();

            List<PropertyMapping<?, ?>> returned = graph.getMappingsForDataSource(dsName);

            // PROPERTY: Count must match
            assertThat(returned)
                    .as("getMappingsForDataSource('%s') must return exactly %d mappings",
                            dsName, expectedDescs.size())
                    .hasSize(expectedDescs.size());

            // PROPERTY: Every returned mapping must reference this datasource
            for (PropertyMapping<?, ?> mapping : returned) {
                assertThat(mapping.getDataSourceName())
                        .as("Returned mapping must reference datasource '%s'", dsName)
                        .isEqualTo(dsName);
            }
        }

        // PROPERTY: Non-existent datasource returns empty
        List<PropertyMapping<?, ?>> nonExistent = graph.getMappingsForDataSource("non-existent-ds");
        assertThat(nonExistent)
                .as("Non-existent datasource must return empty mapping list")
                .isEmpty();

        // PROPERTY: Mappings NOT referencing a datasource must NOT be returned for it
        for (String dsName : expectedByDs.keySet()) {
            List<PropertyMapping<?, ?>> returned = graph.getMappingsForDataSource(dsName);
            for (PropertyMapping<?, ?> mapping : returned) {
                assertThat(mapping.getDataSourceName())
                        .as("Mapping returned for '%s' must not reference another datasource", dsName)
                        .isEqualTo(dsName);
            }
        }
    }

    // ==================== Helpers ====================

    @SuppressWarnings("unchecked")
    private void registerStreamingDs(InMemorySpecStoreFactory factory, String dsName) {
        StreamingDataSource<OrderTestEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(dsName);
        factory.registerDataSource(dsName, streamingDs);
    }

    private PropertyMapping<?, ?> createMockMapping(
            String dataSourceName, String consumerId, boolean forDashboard) {
        PropertyMapping<?, ?> mapping = mock(PropertyMapping.class);
        when(mapping.getDataSourceName()).thenReturn(dataSourceName);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.isForDashboard()).thenReturn(forDashboard);
        return mapping;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TaggedBatch>> interleavedEventSequences() {
        Arbitrary<OrderTestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 20),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
        ).as(OrderTestEntity::new);

        Arbitrary<List<OrderTestEntity>> batchArb = entityArb.list()
                .ofMinSize(1).ofMaxSize(10);

        Arbitrary<String> dsNameArb = Arbitraries.of(DS_A, DS_B);

        Arbitrary<TaggedBatch> taggedBatchArb = Combinators.combine(dsNameArb, batchArb)
                .as(TaggedBatch::new);

        // Generate 2-12 interleaved batches from both datasources
        return taggedBatchArb.list().ofMinSize(2).ofMaxSize(12);
    }

    @Provide
    Arbitrary<List<MappingDescriptor>> mappingDescriptors() {
        Arbitrary<String> dsNames = Arbitraries.of(DS_A, DS_B, DS_C);
        Arbitrary<String> consumerIds = Arbitraries.of(
                "store-1", "store-2", "store-3", "dash-1", "dash-2");
        Arbitrary<Boolean> forDashboard = Arbitraries.of(true, false);

        Arbitrary<MappingDescriptor> descriptor = Combinators.combine(dsNames, consumerIds, forDashboard)
                .as(MappingDescriptor::new);

        return descriptor.list().ofMinSize(1).ofMaxSize(20);
    }

    // ==================== Records and Test Entities ====================

    /**
     * A batch tagged with its datasource name, used to interleave events
     * from multiple datasources.
     */
    static final class TaggedBatch {
        final String dsName;
        final List<OrderTestEntity> entities;
        TaggedBatch(String dsName, List<OrderTestEntity> entities) {
            this.dsName = dsName;
            this.entities = entities;
        }
    }

    /**
     * Descriptor for generating random mapping configurations.
     */
    static final class MappingDescriptor {
        final String dataSourceName;
        final String consumerId;
        final boolean forDashboard;
        MappingDescriptor(String dataSourceName, String consumerId, boolean forDashboard) {
            this.dataSourceName = dataSourceName;
            this.consumerId = consumerId;
            this.forDashboard = forDashboard;
        }
    }

    /**
     * Simple test entity implementing Identifiable&lt;Integer&gt;.
     */
    static class OrderTestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;

        OrderTestEntity(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderTestEntity that = (OrderTestEntity) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "OrderTestEntity{id=" + id + ", value='" + value + "'}";
        }
    }
}
