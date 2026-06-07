package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

// Feature: streaming-datasource-support, Property 7: Upsert Semantiği (DependencyGraph)

/**
 * Property-based tests for {@link DependencyGraph} upsert semantics.
 *
 * <p><b>Validates: Requirements 5.2, 5.3, 8.4, 8.5</b></p>
 */
class DependencyGraphTest {

    private static final String DS_NAME = "testDataSource";

    // --- Property 7a: After upsert, findById returns that entity ---

    /**
     * Property 7a: For any entity, after upsert, findById returns that entity.
     * If entity's ID does NOT exist in DependencyGraph, upsert should add the new entity.
     *
     * Validates: Requirements 5.2, 8.5
     */
    @Property(tries = 100)
    void afterUpsertFindByIdReturnsThatEntity(
            @ForAll @IntRange(min = 1, max = 10000) int entityId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String value) {

        DependencyGraph graph = new DependencyGraph();
        TestEntity entity = new TestEntity(entityId, value);

        graph.upsert(DS_NAME, entity);

        TestEntity found = graph.findById(DS_NAME, entityId);
        assertThat(found).isNotNull();
        assertThat(found.getIdentity()).isEqualTo(entityId);
        assertThat(found.getValue()).isEqualTo(value);
    }

    // --- Property 7b: Upsert with same ID replaces existing entity ---

    /**
     * Property 7b: For any entity with same ID but different value, after upsert,
     * findById returns the updated entity (not the old one).
     *
     * Validates: Requirements 5.3, 8.4
     */
    @Property(tries = 100)
    void upsertWithSameIdReplacesExistingEntity(
            @ForAll @IntRange(min = 1, max = 10000) int entityId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String oldValue,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String newValue) {

        DependencyGraph graph = new DependencyGraph();
        TestEntity oldEntity = new TestEntity(entityId, oldValue);
        TestEntity newEntity = new TestEntity(entityId, newValue);

        graph.upsert(DS_NAME, oldEntity);
        graph.upsert(DS_NAME, newEntity);

        TestEntity found = graph.findById(DS_NAME, entityId);
        assertThat(found).isNotNull();
        assertThat(found.getIdentity()).isEqualTo(entityId);
        assertThat(found.getValue()).isEqualTo(newValue);
    }

    // --- Property 7c: After upsertAll, all entities are findable by their IDs ---

    /**
     * Property 7c: For any list of entities, after upsertAll, all entities are
     * findable by their IDs via findById.
     *
     * Validates: Requirements 5.2, 5.3, 8.4, 8.5
     */
    @Property(tries = 100)
    void afterUpsertAllAllEntitiesAreFindableByIds(
            @ForAll @Size(min = 1, max = 50) List<@IntRange(min = 1, max = 500) Integer> ids,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String valuePrefix) {

        DependencyGraph graph = new DependencyGraph();

        // Create entities with unique IDs (deduplicate)
        List<TestEntity> entities = new ArrayList<>();
        List<Integer> uniqueIds = ids.stream().distinct().toList();
        for (int i = 0; i < uniqueIds.size(); i++) {
            entities.add(new TestEntity(uniqueIds.get(i), valuePrefix + i));
        }

        graph.upsertAll(DS_NAME, entities);

        for (TestEntity entity : entities) {
            TestEntity found = graph.findById(DS_NAME, entity.getIdentity());
            assertThat(found).isNotNull();
            assertThat(found.getIdentity()).isEqualTo(entity.getIdentity());
            assertThat(found.getValue()).isEqualTo(entity.getValue());
        }

        // Verify total count matches
        List<TestEntity> all = graph.findAll(DS_NAME);
        assertThat(all).hasSize(uniqueIds.size());
    }

    // --- Property 11: Atomik Batch Güncelleme ---
    // Feature: streaming-datasource-support, Property 11: Atomik Batch Güncelleme

    /**
     * Property 11: For any batch upsertAll and concurrent findAll, the reader
     * must always see a consistent snapshot — either ALL old values or ALL new
     * values, never a mix of old and new.
     *
     * <p><b>Validates: Requirements 5.9, 8.6, 8.8, 11.5</b></p>
     */
    @Property(tries = 100)
    void upsertAllIsAtomicWithConcurrentReads(
            @ForAll @IntRange(min = 2, max = 20) int entityCount) throws Exception {

        DependencyGraph graph = new DependencyGraph();
        String oldValue = "old";
        String newValue = "new";

        // Step 1: Pre-populate with "old" entities (IDs 1..entityCount)
        List<TestEntity> oldEntities = new ArrayList<>();
        for (int i = 1; i <= entityCount; i++) {
            oldEntities.add(new TestEntity(i, oldValue));
        }
        graph.upsertAll(DS_NAME, oldEntities);

        // Step 2: Concurrent reader — repeatedly calls findAll() and checks consistency
        AtomicBoolean writerDone = new AtomicBoolean(false);
        CopyOnWriteArrayList<String> violations = new CopyOnWriteArrayList<>();
        CountDownLatch readerStarted = new CountDownLatch(1);

        CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
            readerStarted.countDown();
            while (!writerDone.get()) {
                List<TestEntity> snapshot = graph.findAll(DS_NAME);
                if (snapshot.isEmpty()) {
                    continue;
                }
                // Every entity in the snapshot must have the same value
                String firstValue = snapshot.get(0).getValue();
                for (TestEntity entity : snapshot) {
                    if (!entity.getValue().equals(firstValue)) {
                        violations.add(
                                "Partial state detected: entity " + entity.getIdentity()
                                        + " has value '" + entity.getValue()
                                        + "' but first entity has value '" + firstValue + "'");
                    }
                }
            }
            // One final read after writer is done
            List<TestEntity> finalSnapshot = graph.findAll(DS_NAME);
            if (!finalSnapshot.isEmpty()) {
                String firstValue = finalSnapshot.get(0).getValue();
                for (TestEntity entity : finalSnapshot) {
                    if (!entity.getValue().equals(firstValue)) {
                        violations.add(
                                "Partial state in final read: entity " + entity.getIdentity()
                                        + " has value '" + entity.getValue()
                                        + "' but first entity has value '" + firstValue + "'");
                    }
                }
            }
        });

        // Wait for reader to start
        readerStarted.await();

        // Step 3: Writer — upsertAll with "new" values
        List<TestEntity> newEntities = new ArrayList<>();
        for (int i = 1; i <= entityCount; i++) {
            newEntities.add(new TestEntity(i, newValue));
        }
        graph.upsertAll(DS_NAME, newEntities);
        writerDone.set(true);

        // Step 4: Wait for reader to finish and verify no violations
        reader.join();
        assertThat(violations)
                .as("findAll() must return a consistent snapshot (all old or all new, never mixed)")
                .isEmpty();

        // Step 5: Final state must be all "new"
        List<TestEntity> finalState = graph.findAll(DS_NAME);
        assertThat(finalState).hasSize(entityCount);
        for (TestEntity entity : finalState) {
            assertThat(entity.getValue()).isEqualTo(newValue);
        }
    }

    // --- Property 8: DependencyGraph Etkilenen Bileşen Tespiti ---
    // Feature: streaming-datasource-support, Property 8: DependencyGraph Etkilenen Bileşen Tespiti

    /**
     * Property 8a: For any set of PropertyMappings and a datasource name,
     * getMappingsForDataSource() returns exactly the mappings that use this
     * datasource as source.
     *
     * <p><b>Validates: Requirements 7.2</b></p>
     */
    @Property(tries = 100)
    void getMappingsForDataSourceReturnsExactMatchingMappings(
            @ForAll("mappingDescriptors") List<MappingDescriptor> descriptors) {

        DependencyGraph graph = new DependencyGraph();
        List<PropertyMapping<?, ?>> allMappings = new ArrayList<>();

        for (MappingDescriptor desc : descriptors) {
            PropertyMapping<?, ?> mockMapping = createMockMapping(
                    desc.dataSourceName, desc.consumerId, desc.forDashboard);
            allMappings.add(mockMapping);
        }

        graph.build(allMappings);

        // Collect all unique datasource names
        Set<String> allDsNames = descriptors.stream()
                .map(d -> d.dataSourceName)
                .collect(Collectors.toSet());

        for (String dsName : allDsNames) {
            List<PropertyMapping<?, ?>> returned = graph.getMappingsForDataSource(dsName);

            // Count expected mappings for this datasource
            long expectedCount = descriptors.stream()
                    .filter(d -> d.dataSourceName.equals(dsName))
                    .count();

            assertThat(returned).hasSize((int) expectedCount);

            // Every returned mapping must have the correct datasource name
            for (PropertyMapping<?, ?> mapping : returned) {
                assertThat(mapping.getDataSourceName()).isEqualTo(dsName);
            }
        }

        // A datasource name not in the set should return empty
        List<PropertyMapping<?, ?>> nonExistent = graph.getMappingsForDataSource("nonExistentDs");
        assertThat(nonExistent).isEmpty();
    }

    /**
     * Property 8b: For any set of PropertyMappings and a datasource name,
     * getAffectedConsumers() returns the complete set of affected store and
     * dashboard consumer IDs.
     *
     * <p><b>Validates: Requirements 7.3</b></p>
     */
    @Property(tries = 100)
    void getAffectedConsumersReturnsCompleteConsumerSets(
            @ForAll("mappingDescriptors") List<MappingDescriptor> descriptors) {

        DependencyGraph graph = new DependencyGraph();
        List<PropertyMapping<?, ?>> allMappings = new ArrayList<>();

        for (MappingDescriptor desc : descriptors) {
            PropertyMapping<?, ?> mockMapping = createMockMapping(
                    desc.dataSourceName, desc.consumerId, desc.forDashboard);
            allMappings.add(mockMapping);
        }

        graph.build(allMappings);

        // Collect all unique datasource names
        Set<String> allDsNames = descriptors.stream()
                .map(d -> d.dataSourceName)
                .collect(Collectors.toSet());

        for (String dsName : allDsNames) {
            AffectedConsumerSet consumers = graph.getAffectedConsumers(dsName);

            // Compute expected store and dashboard IDs
            Set<String> expectedStoreIds = new HashSet<>();
            Set<String> expectedDashboardIds = new HashSet<>();
            for (MappingDescriptor desc : descriptors) {
                if (desc.dataSourceName.equals(dsName)) {
                    if (desc.forDashboard) {
                        expectedDashboardIds.add(desc.consumerId);
                    } else {
                        expectedStoreIds.add(desc.consumerId);
                    }
                }
            }

            assertThat(consumers.getStoreIds())
                    .as("Store IDs for datasource '%s'", dsName)
                    .containsExactlyInAnyOrderElementsOf(expectedStoreIds);

            assertThat(consumers.getDashboardIds())
                    .as("Dashboard IDs for datasource '%s'", dsName)
                    .containsExactlyInAnyOrderElementsOf(expectedDashboardIds);
        }

        // A datasource name not in the set should return empty consumer sets
        AffectedConsumerSet nonExistent = graph.getAffectedConsumers("nonExistentDs");
        assertThat(nonExistent.getStoreIds()).isEmpty();
        assertThat(nonExistent.getDashboardIds()).isEmpty();
    }

    // --- Property 9: DependencyGraph Mapping Ekleme/Kaldırma Round-Trip ---
    // Feature: streaming-datasource-support, Property 9: DependencyGraph Mapping Ekleme/Kaldırma Round-Trip

    /**
     * Property 9: For any PropertyMapping, after adding it via addMapping(),
     * getMappingsForDataSource() includes this mapping. After removing it via
     * removeMapping(), getMappingsForDataSource() does NOT include this mapping.
     *
     * <p><b>Validates: Requirements 7.4</b></p>
     */
    @Property(tries = 100)
    void addMappingThenRemoveMappingRoundTrip(
            @ForAll("mappingDescriptors") List<MappingDescriptor> descriptors) {

        DependencyGraph graph = new DependencyGraph();

        // Create mock mappings from descriptors
        List<PropertyMapping<?, ?>> mappings = new ArrayList<>();
        for (MappingDescriptor desc : descriptors) {
            mappings.add(createMockMapping(desc.dataSourceName(), desc.consumerId(), desc.forDashboard()));
        }

        // Step 1: Add all mappings one by one
        for (PropertyMapping<?, ?> mapping : mappings) {
            graph.addMapping(mapping);
        }

        // Step 2: Verify each mapping is present in getMappingsForDataSource
        for (int i = 0; i < mappings.size(); i++) {
            PropertyMapping<?, ?> mapping = mappings.get(i);
            String dsName = descriptors.get(i).dataSourceName();
            List<PropertyMapping<?, ?>> returned = graph.getMappingsForDataSource(dsName);
            assertThat(returned)
                    .as("After addMapping, getMappingsForDataSource('%s') should contain the mapping", dsName)
                    .contains(mapping);
        }

        // Step 3: Remove all mappings one by one and verify removal
        for (int i = 0; i < mappings.size(); i++) {
            PropertyMapping<?, ?> mapping = mappings.get(i);
            String dsName = descriptors.get(i).dataSourceName();

            graph.removeMapping(mapping);

            List<PropertyMapping<?, ?>> returned = graph.getMappingsForDataSource(dsName);
            assertThat(returned)
                    .as("After removeMapping, getMappingsForDataSource('%s') should NOT contain the mapping", dsName)
                    .doesNotContain(mapping);
        }
    }

    // --- Property 10: DependencyGraph Döngüsel Bağımlılık Tespiti ---
    // Feature: streaming-datasource-support, Property 10: DependencyGraph Döngüsel Bağımlılık Tespiti

    /**
     * Property 10a: For any set of PropertyMappings that contain a circular dependency,
     * detectCycles() should throw IllegalStateException.
     *
     * A cycle is formed when consumer IDs match datasource names, creating a directed
     * loop: dsA → dsB → ... → dsA.
     *
     * <p><b>Validates: Requirements 7.5</b></p>
     */
    @Property(tries = 100)
    void detectCyclesThrowsForCircularDependencies(
            @ForAll @IntRange(min = 2, max = 6) int cycleLength) {

        DependencyGraph graph = new DependencyGraph();
        List<PropertyMapping<?, ?>> mappings = new ArrayList<>();

        // Create a cycle: ds0 → ds1 → ds2 → ... → ds(N-1) → ds0
        // Each mapping has dataSourceName="dsI" and consumerId="ds(I+1 mod N)"
        for (int i = 0; i < cycleLength; i++) {
            String dsName = "ds" + i;
            String consumerId = "ds" + ((i + 1) % cycleLength);
            mappings.add(createMockMapping(dsName, consumerId, false));
        }

        graph.build(mappings);

        assertThatThrownBy(() -> graph.detectCycles())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular dependency detected");
    }

    /**
     * Property 10b: For any set of PropertyMappings that do NOT contain a circular
     * dependency, detectCycles() should NOT throw any exception.
     *
     * Acyclic mappings are ensured by using consumer IDs that never match any
     * datasource name (prefixed with "store_").
     *
     * <p><b>Validates: Requirements 7.5</b></p>
     */
    @Property(tries = 100)
    void detectCyclesDoesNotThrowForAcyclicDependencies(
            @ForAll("mappingDescriptors") List<MappingDescriptor> descriptors) {

        DependencyGraph graph = new DependencyGraph();
        List<PropertyMapping<?, ?>> mappings = new ArrayList<>();

        // Ensure acyclic: consumer IDs from mappingDescriptors provider use
        // "store1", "store2", etc. which never match datasource names "dsA", "dsB", etc.
        // This guarantees no cycle can form.
        for (MappingDescriptor desc : descriptors) {
            mappings.add(createMockMapping(desc.dataSourceName(), desc.consumerId(), desc.forDashboard()));
        }

        graph.build(mappings);

        assertThatCode(() -> graph.detectCycles())
                .doesNotThrowAnyException();
    }

    // --- Property 8 helpers ---

    private PropertyMapping<?, ?> createMockMapping(
            String dataSourceName, String consumerId, boolean forDashboard) {
        PropertyMapping<?, ?> mapping = mock(PropertyMapping.class);
        when(mapping.getDataSourceName()).thenReturn(dataSourceName);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.isForDashboard()).thenReturn(forDashboard);
        return mapping;
    }

    /**
     * Descriptor record for generating random mapping configurations.
     */
    private static final class MappingDescriptor {
        final String dataSourceName;
        final String consumerId;
        final boolean forDashboard;
        MappingDescriptor(String dataSourceName, String consumerId, boolean forDashboard) {
            this.dataSourceName = dataSourceName;
            this.consumerId = consumerId;
            this.forDashboard = forDashboard;
        }
        String dataSourceName() { return dataSourceName; }
        String consumerId() { return consumerId; }
        boolean forDashboard() { return forDashboard; }
    }

    @Provide
    Arbitrary<List<MappingDescriptor>> mappingDescriptors() {
        Arbitrary<String> dsNames = Arbitraries.of("dsA", "dsB", "dsC", "dsD");
        Arbitrary<String> consumerIds = Arbitraries.of(
                "store1", "store2", "store3", "dash1", "dash2", "dash3");
        Arbitrary<Boolean> forDashboard = Arbitraries.of(true, false);

        Arbitrary<MappingDescriptor> descriptor = Combinators.combine(dsNames, consumerIds, forDashboard)
                .as(MappingDescriptor::new);

        return descriptor.list().ofMinSize(1).ofMaxSize(20);
    }

    // --- Property 13: Index Artımlı Güncelleme Tutarlılığı ---
    // Feature: streaming-datasource-support, Property 13: Index Artımlı Güncelleme Tutarlılığı

    /**
     * Property 13: For any sequence of entity changes (insert, update, remove),
     * the incremental index update result in DependencyGraph should produce the
     * same lookup results as a full index rebuild.
     *
     * <p>Model-based: apply operations incrementally with updateIndexes/removeFromIndexes
     * after each step, then compare with rebuildIndexesForDataSource result.</p>
     *
     * <p><b>Validates: Requirements 9.1, 9.2, 9.3, 9.4</b></p>
     */
    @Property(tries = 100)
    void incrementalIndexUpdateProducesSameResultAsFullRebuild(
            @ForAll("indexOperationSequences") List<IndexOperation> operations) {

        DependencyGraph graph = new DependencyGraph();

        // Create an IndexDefinition using addKeyFieldWithPath with a custom extractor
        StringAttribute<IndexableTestEntity> categoryAttr =
                new StringAttribute<>("category", IndexableTestEntity.class);
        IndexDefinition<IndexableTestEntity> indexDef = IndexDefinition
                .<IndexableTestEntity>builder(IndexableTestEntity.class)
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((IndexableTestEntity) entity).getCategory())
                .build();

        graph.registerIndex(DS_NAME, indexDef);

        // Track all categories seen for later lookup comparison
        Set<String> allCategories = new HashSet<>();

        // Apply operations incrementally
        for (IndexOperation op : operations) {
            allCategories.add(op.category());
            switch (op.type()) {
                case INSERT, UPDATE -> {
                    IndexableTestEntity entity = new IndexableTestEntity(
                            op.entityId(), "val" + op.entityId(), op.category());
                    IndexableTestEntity oldEntity = graph.findById(DS_NAME, op.entityId());
                    graph.upsert(DS_NAME, entity);
                    List<IndexableTestEntity> oldEntities = oldEntity != null
                            ? List.of(oldEntity) : List.of();
                    graph.updateIndexes(DS_NAME, oldEntities, List.of(entity));
                }
                case REMOVE -> {
                    IndexableTestEntity existing = graph.findById(DS_NAME, op.entityId());
                    if (existing != null) {
                        graph.removeById(DS_NAME, op.entityId());
                        graph.removeFromIndexes(DS_NAME, List.of(existing));
                    }
                }
            }
        }

        // Capture lookup results after incremental updates
        Map<String, List<Integer>> incrementalResults = new HashMap<>();
        for (String category : allCategories) {
            List<IndexableTestEntity> found = graph.lookup(DS_NAME, indexDef, category);
            List<Integer> ids = found.stream()
                    .map(IndexableTestEntity::getIdentity)
                    .sorted()
                    .collect(Collectors.toList());
            incrementalResults.put(category, ids);
        }

        // Full rebuild
        graph.rebuildIndexesForDataSource(DS_NAME);

        // Capture lookup results after full rebuild
        Map<String, List<Integer>> rebuildResults = new HashMap<>();
        for (String category : allCategories) {
            List<IndexableTestEntity> found = graph.lookup(DS_NAME, indexDef, category);
            List<Integer> ids = found.stream()
                    .map(IndexableTestEntity::getIdentity)
                    .sorted()
                    .collect(Collectors.toList());
            rebuildResults.put(category, ids);
        }

        // Incremental and full rebuild results must be identical
        assertThat(incrementalResults)
                .as("Incremental index update results must match full rebuild results")
                .isEqualTo(rebuildResults);
    }

    private enum OpType { INSERT, UPDATE, REMOVE }

    private record IndexOperation(OpType type, int entityId, String category) {}

    @Provide
    Arbitrary<List<IndexOperation>> indexOperationSequences() {
        Arbitrary<OpType> opTypes = Arbitraries.of(OpType.INSERT, OpType.UPDATE, OpType.REMOVE);
        Arbitrary<Integer> entityIds = Arbitraries.integers().between(1, 20);
        Arbitrary<String> categories = Arbitraries.of("catA", "catB", "catC", "catD");

        Arbitrary<IndexOperation> operation = Combinators.combine(opTypes, entityIds, categories)
                .as(IndexOperation::new);

        return operation.list().ofMinSize(3).ofMaxSize(30);
    }

    // --- Test entities ---

    /**
     * Test entity with an indexable "category" field for Property 13.
     */
    private static class IndexableTestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;
        private final String category;

        IndexableTestEntity(int id, String value, String category) {
            this.id = id;
            this.value = value;
            this.category = category;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IndexableTestEntity that = (IndexableTestEntity) o;
            return id == that.id && Objects.equals(value, that.value)
                    && Objects.equals(category, that.category);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value, category);
        }
    }

    // --- Test entity ---

    /**
     * Simple test entity implementing Identifiable for property tests.
     */
    private static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;

        TestEntity(int id, String value) {
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
            TestEntity that = (TestEntity) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }
    }
}
