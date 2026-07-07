package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for Pipeline Output Equivalence.
 *
 * <p>Verifies that for any valid BatchSnapshotEvent sequence and DependencyGraph
 * initial state, the optimized pipeline (delta + cache + incremental index) produces
 * the same final entity states as the original full application pipeline.</p>
 *
 * <p>Uses the original {@code applyPhase2_5StoreMappingsFull()} logic as reference
 * implementation and compares against the optimized {@code applyPhase2_5StoreMappings()}.</p>
 *
 * <p>Both paths assume Phase 1 (entity upsert) has already run — event entities are
 * upserted into the DependencyGraph before either path executes, matching the real
 * pipeline flow in {@code processBatchSnapshot()}.</p>
 *
 * <p><b>Validates: Requirements 1.5, 6.1, 6.5</b></p>
 */
class PipelineOutputEquivalencePropertyTest {

    // ==================== Reflection Setup ====================

    private static final Method OPTIMIZED_METHOD;

    static {
        try {
            OPTIMIZED_METHOD = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "applyPhase2_5StoreMappings",
                    String.class, Set.class, List.class);
            OPTIMIZED_METHOD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up applyPhase2_5StoreMappings reflection", e);
        }
    }

    // ==================== Constants ====================

    private static final String PRIMARY_DS = "ds-primary";
    private static final String FOREIGN_DS = "ds-foreign";
    private static final String STORE_ID = "test-store";

    // Shared mock MetaAttribute instances with names (needed by registerIndexForMapping dedup key)
    private static final MetaAttribute<?, ?> FK_ATTR;
    private static final MetaAttribute<?, ?> PK_ATTR;
    private static final MetaAttribute<?, ?> SOURCE_ATTR;
    private static final MetaAttribute<?, ?> TARGET_ATTR;

    static {
        FK_ATTR = mock(MetaAttribute.class);
        when(FK_ATTR.getName()).thenReturn("fkValue");
        PK_ATTR = mock(MetaAttribute.class);
        when(PK_ATTR.getName()).thenReturn("id");
        SOURCE_ATTR = mock(MetaAttribute.class);
        when(SOURCE_ATTR.getName()).thenReturn("mappedValue");
        TARGET_ATTR = mock(MetaAttribute.class);
        when(TARGET_ATTR.getName()).thenReturn("mappedField");
    }

    private static final List<MetaAttribute<?, ?>> FK_PATH = List.of(FK_ATTR);
    private static final List<MetaAttribute<?, ?>> PK_PATH = List.of(PK_ATTR);
    private static final List<MetaAttribute<?, ?>> SOURCE_PATH = List.of(SOURCE_ATTR);
    private static final List<MetaAttribute<?, ?>> TARGET_PATH = List.of(TARGET_ATTR);

    // ==================== Test Entities ====================

    /**
     * Foreign entity with an FK value pointing to a root entity's PK
     * and a mapped value that will be copied to the root entity.
     */
    static class ForeignEntity implements Identifiable<Long> {
        private final Long id;
        private final Long fkValue;
        private final String mappedValue;

        ForeignEntity(Long id, Long fkValue, String mappedValue) {
            this.id = id;
            this.fkValue = fkValue;
            this.mappedValue = mappedValue;
        }

        @Override
        public Long getIdentity() { return id; }
        Long getFkValue() { return fkValue; }
        String getMappedValue() { return mappedValue; }

        @Override
        public String toString() {
            return "Foreign[id=" + id + ", fk=" + fkValue + ", val=" + mappedValue + "]";
        }
    }

    /**
     * Mutable root entity. MappingApplicator mutates {@code mappedField}
     * via the mocked SpecificationService.
     */
    static class RootEntity implements Identifiable<Long> {
        private final Long id;
        private String mappedField;

        RootEntity(Long id, String mappedField) {
            this.id = id;
            this.mappedField = mappedField;
        }

        @Override
        public Long getIdentity() { return id; }
        String getMappedField() { return mappedField; }
        void setMappedField(String value) { this.mappedField = value; }

        RootEntity copy() { return new RootEntity(id, mappedField); }

        @Override
        public String toString() {
            return "Root[id=" + id + ", mapped=" + mappedField + "]";
        }
    }

    // ==================== Test Scenario ====================

    static final class ForeignDsScenario {
        final List<RootEntity> rootEntities;
        final List<ForeignEntity> existingForeignEntities;
        final List<ForeignEntity> eventEntities;
        ForeignDsScenario(List<RootEntity> rootEntities, List<ForeignEntity> existingForeignEntities, List<ForeignEntity> eventEntities) {
            this.rootEntities = rootEntities;
            this.existingForeignEntities = existingForeignEntities;
            this.eventEntities = eventEntities;
        }
        List<RootEntity> rootEntities() { return rootEntities; }
        List<ForeignEntity> existingForeignEntities() { return existingForeignEntities; }
        List<ForeignEntity> eventEntities() { return eventEntities; }
    }

    static final class PrimaryDsScenario {
        final List<RootEntity> rootEntities;
        final List<ForeignEntity> existingForeignEntities;
        final List<RootEntity> eventRootEntities;
        PrimaryDsScenario(List<RootEntity> rootEntities, List<ForeignEntity> existingForeignEntities, List<RootEntity> eventRootEntities) {
            this.rootEntities = rootEntities;
            this.existingForeignEntities = existingForeignEntities;
            this.eventRootEntities = eventRootEntities;
        }
        List<RootEntity> rootEntities() { return rootEntities; }
        List<ForeignEntity> existingForeignEntities() { return existingForeignEntities; }
        List<RootEntity> eventRootEntities() { return eventRootEntities; }
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<ForeignDsScenario> foreignDsScenarios() {
        return Arbitraries.integers().between(2, 8).flatMap(rootCount -> {
            // Root entities with unique sequential IDs
            List<RootEntity> roots = new ArrayList<>();
            for (int i = 1; i <= rootCount; i++) {
                roots.add(new RootEntity((long) i, "initial-" + i));
            }

            // Existing foreign entities in the graph (pre-event state)
            Arbitrary<List<ForeignEntity>> existingArb = Combinators.combine(
                    Arbitraries.longs().between(100L, 199L),
                    Arbitraries.longs().between(1L, rootCount),
                    Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
            ).as(ForeignEntity::new).list().ofMinSize(1).ofMaxSize(5);

            // Event entities: new foreign entities arriving via BatchSnapshotEvent
            // These get upserted into the graph (simulating Phase 1) before Phase 2.5
            Arbitrary<List<ForeignEntity>> eventArb = Combinators.combine(
                    Arbitraries.longs().between(200L, 299L),
                    Arbitraries.longs().between(1L, rootCount),
                    Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
            ).as(ForeignEntity::new).list().ofMinSize(1).ofMaxSize(4);

            return Combinators.combine(
                    Arbitraries.just(roots), existingArb, eventArb
            ).as(ForeignDsScenario::new);
        });
    }

    @Provide
    Arbitrary<PrimaryDsScenario> primaryDsScenarios() {
        return Arbitraries.integers().between(2, 8).flatMap(rootCount -> {
            List<RootEntity> roots = new ArrayList<>();
            for (int i = 1; i <= rootCount; i++) {
                roots.add(new RootEntity((long) i, "initial-" + i));
            }

            Arbitrary<List<ForeignEntity>> existingArb = Combinators.combine(
                    Arbitraries.longs().between(100L, 199L),
                    Arbitraries.longs().between(1L, rootCount),
                    Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(8)
            ).as(ForeignEntity::new).list().ofMinSize(1).ofMaxSize(5);

            // Event entities are root entities (primary datasource event)
            // Pick a subset of root IDs as event entities
            Arbitrary<List<RootEntity>> eventArb = Arbitraries.integers()
                    .between(1, Math.max(1, rootCount / 2))
                    .flatMap(eventCount -> {
                        List<RootEntity> eventRoots = new ArrayList<>();
                        for (int i = 1; i <= Math.min(eventCount, rootCount); i++) {
                            eventRoots.add(new RootEntity((long) i, "updated-" + i));
                        }
                        return Arbitraries.just(eventRoots);
                    });

            return Combinators.combine(
                    Arbitraries.just(roots), existingArb, eventArb
            ).as(PrimaryDsScenario::new);
        });
    }

    // ==================== Helpers ====================

    /**
     * Creates a mocked ONE_TO_ONE PropertyMapping:
     * foreign.fkValue → root.id (FK/PK join), foreign.mappedValue → root.mappedField
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PropertyMapping<?, ?> createMockMapping() {
        PropertyMapping mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(STORE_ID);
        when(mapping.getDataSourceName()).thenReturn(FOREIGN_DS);
        when(mapping.isForDashboard()).thenReturn(false);
        when(mapping.getMappingType()).thenReturn(MappingType.ONE_TO_ONE);
        when(mapping.getForeignKeyPaths()).thenReturn(List.of(FK_PATH));
        when(mapping.getPrimaryKeyPaths()).thenReturn(List.of(PK_PATH));
        when(mapping.getSourcePath()).thenReturn(SOURCE_PATH);
        when(mapping.getTargetPath()).thenReturn(TARGET_PATH);
        when(mapping.getSourceCollectionOperations()).thenReturn(null);
        when(mapping.getTargetCollectionOperations()).thenReturn(null);
        when(mapping.getSpecification()).thenReturn(null);
        when(mapping.requiresGrouping()).thenReturn(false);

        // Source service: operates on ForeignEntity
        SpecificationService sourceService = mock(SpecificationService.class);
        when(sourceService.getValueByPath(any(), eq(FK_PATH))).thenAnswer(inv -> {
            Object entity = inv.getArgument(0);
            return (entity instanceof ForeignEntity fe) ? fe.getFkValue() : null;
        });
        when(sourceService.getValueByPath(any(), eq(SOURCE_PATH))).thenAnswer(inv -> {
            Object entity = inv.getArgument(0);
            return (entity instanceof ForeignEntity fe) ? fe.getMappedValue() : null;
        });
        when(mapping.getSourceService()).thenReturn(sourceService);

        // Target service: operates on RootEntity
        SpecificationService targetService = mock(SpecificationService.class);
        when(targetService.getValueByPath(any(), eq(PK_PATH))).thenAnswer(inv -> {
            Object entity = inv.getArgument(0);
            return (entity instanceof RootEntity re) ? re.getIdentity() : null;
        });
        doAnswer(inv -> {
            Object entity = inv.getArgument(0);
            Object value = inv.getArgument(2);
            if (entity instanceof RootEntity re) {
                re.setMappedField(value != null ? value.toString() : null);
            }
            return null;
        }).when(targetService).setValueByPath(any(), eq(TARGET_PATH), any());
        when(mapping.getTargetService()).thenReturn(targetService);

        return mapping;
    }

    /**
     * Creates an IncrementalSyncProcessor with the given DependencyGraph.
     * The factory is mocked; the mapping is registered in both the graph
     * and the processor's precomputed store mappings.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private IncrementalSyncProcessor createProcessor(
            DependencyGraph graph, PropertyMapping<?, ?> mapping) {

        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);

        // Empty during construction so registerMappingIndexes() is a no-op
        when(factory.getAllStoreIds()).thenReturn(Collections.emptyList());

        InMemoryDataStore mockStore = mock(InMemoryDataStore.class);
        when(mockStore.getPrimaryDataSourceName()).thenReturn(PRIMARY_DS);
        when(factory.getStoreById(STORE_ID)).thenReturn(mockStore);

        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, new AtomicLong(0));

        // Post-construction: enable store ID lookup for the full path
        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_ID));

        // Register mapping in graph (for full path's getMappingsByConsumerId)
        graph.addMapping(mapping);

        // Register in precomputed store mappings (for optimized path's getStoreMappings)
        processor.onMappingAdded(mapping);

        return processor;
    }

    /**
     * Snapshots root entity states as sorted "id:mappedField" strings
     * for deterministic comparison.
     */
    private List<String> snapshotEntityStates(DependencyGraph graph) {
        List<?> entities = graph.findAll(PRIMARY_DS);
        if (entities == null) return Collections.emptyList();

        return entities.stream()
                .filter(RootEntity.class::isInstance)
                .map(e -> {
                    RootEntity re = (RootEntity) e;
                    return re.getIdentity() + ":" + re.getMappedField();
                })
                .sorted()
                .collect(Collectors.toList());
    }

    // ==================== Property Tests ====================

    /**
     * Property 2: Pipeline Output Equivalence (foreign datasource events)
     *
     * <p>For any set of root entities, existing foreign entities, and new foreign
     * event entities, the optimized pipeline produces the same final root entity
     * states as the full pipeline.</p>
     *
     * <p>Simulates the real pipeline flow:
     * <ol>
     *   <li>Initialization: Apply full mapping to establish baseline state</li>
     *   <li>Phase 1: Upsert event entities into the graph</li>
     *   <li>Phase 2.5: Apply store mappings (optimized vs. full)</li>
     *   <li>Compare: Root entity states must be identical</li>
     * </ol></p>
     *
     * <p><b>Validates: Requirements 1.5, 6.1, 6.5</b></p>
     */
    @Property(tries = 100)
    void optimizedPipelineProducesSameEntityStatesAsFullPipeline(
            @ForAll("foreignDsScenarios") ForeignDsScenario scenario) throws Exception {

        // Separate mappings for each processor (mocks are stateful)
        PropertyMapping<?, ?> mappingA = createMockMapping();
        PropertyMapping<?, ?> mappingB = createMockMapping();

        // Clone root entities — each path gets its own mutable copies
        List<RootEntity> rootsA = scenario.rootEntities().stream()
                .map(RootEntity::copy).collect(Collectors.toList());
        List<RootEntity> rootsB = scenario.rootEntities().stream()
                .map(RootEntity::copy).collect(Collectors.toList());

        // Set up two identical DependencyGraphs
        DependencyGraph graphA = new DependencyGraph();
        DependencyGraph graphB = new DependencyGraph();

        // Populate with root entities and existing foreign entities
        graphA.upsertAll(PRIMARY_DS, rootsA);
        graphA.upsertAll(FOREIGN_DS, scenario.existingForeignEntities());
        graphB.upsertAll(PRIMARY_DS, rootsB);
        graphB.upsertAll(FOREIGN_DS, scenario.existingForeignEntities());

        // Create processors (registers mapping in graph + precomputed mappings)
        IncrementalSyncProcessor processorA = createProcessor(graphA, mappingA);
        IncrementalSyncProcessor processorB = createProcessor(graphB, mappingB);

        // === Initialization: apply full mapping to establish baseline ===
        // In the real pipeline, applyPostInitializationCatchUp() does this.
        // Both graphs must start with the same mapped state before the event.
        Set<PropertyMapping<?, ?>> initAffectedA = new LinkedHashSet<>(List.of(mappingA));
        Set<PropertyMapping<?, ?>> initAffectedB = new LinkedHashSet<>(List.of(mappingB));
        processorA.applyPhase2_5StoreMappingsFull(FOREIGN_DS, initAffectedA);
        processorB.applyPhase2_5StoreMappingsFull(FOREIGN_DS, initAffectedB);

        // Verify baseline is identical before the event
        List<String> baselineA = snapshotEntityStates(graphA);
        List<String> baselineB = snapshotEntityStates(graphB);
        assertThat(baselineA).as("Baseline states must be identical").isEqualTo(baselineB);

        // === Simulate Phase 1: upsert event entities into both graphs ===
        for (ForeignEntity fe : scenario.eventEntities()) {
            graphA.upsert(FOREIGN_DS, fe);
            graphB.upsert(FOREIGN_DS, fe);
        }

        // Build affectedMappings sets for the event
        Set<PropertyMapping<?, ?>> affectedA = new LinkedHashSet<>(List.of(mappingA));
        Set<PropertyMapping<?, ?>> affectedB = new LinkedHashSet<>(List.of(mappingB));

        // Path A: Optimized pipeline (private method via reflection)
        OPTIMIZED_METHOD.invoke(processorA, FOREIGN_DS, affectedA,
                (List<?>) scenario.eventEntities());

        // Path B: Full pipeline (package-private, directly accessible)
        processorB.applyPhase2_5StoreMappingsFull(FOREIGN_DS, affectedB);

        // Compare final root entity states
        List<String> statesA = snapshotEntityStates(graphA);
        List<String> statesB = snapshotEntityStates(graphB);

        assertThat(statesA)
                .as("Optimized pipeline must produce same entity states as full pipeline "
                        + "(roots=%d, existing=%d, event=%d)",
                        scenario.rootEntities().size(),
                        scenario.existingForeignEntities().size(),
                        scenario.eventEntities().size())
                .isEqualTo(statesB);
    }

    /**
     * Property 2 (primary datasource variant): Pipeline Output Equivalence
     * when the event arrives on the primary datasource.
     *
     * <p>When event entities are root entities themselves, the optimized path
     * treats them as directly affected while the full path applies mappings
     * to all root entities. Both must produce the same result.</p>
     *
     * <p><b>Validates: Requirements 1.5, 6.1, 6.5</b></p>
     */
    @Property(tries = 100)
    void optimizedPipelineEquivalentForPrimaryDatasourceEvents(
            @ForAll("primaryDsScenarios") PrimaryDsScenario scenario) throws Exception {

        PropertyMapping<?, ?> mappingA = createMockMapping();
        PropertyMapping<?, ?> mappingB = createMockMapping();

        List<RootEntity> rootsA = scenario.rootEntities().stream()
                .map(RootEntity::copy).collect(Collectors.toList());
        List<RootEntity> rootsB = scenario.rootEntities().stream()
                .map(RootEntity::copy).collect(Collectors.toList());

        DependencyGraph graphA = new DependencyGraph();
        DependencyGraph graphB = new DependencyGraph();

        graphA.upsertAll(PRIMARY_DS, rootsA);
        graphA.upsertAll(FOREIGN_DS, scenario.existingForeignEntities());
        graphB.upsertAll(PRIMARY_DS, rootsB);
        graphB.upsertAll(FOREIGN_DS, scenario.existingForeignEntities());

        IncrementalSyncProcessor processorA = createProcessor(graphA, mappingA);
        IncrementalSyncProcessor processorB = createProcessor(graphB, mappingB);

        // === Initialization: apply full mapping to establish baseline ===
        Set<PropertyMapping<?, ?>> initAffectedA = new LinkedHashSet<>(List.of(mappingA));
        Set<PropertyMapping<?, ?>> initAffectedB = new LinkedHashSet<>(List.of(mappingB));
        processorA.applyPhase2_5StoreMappingsFull(FOREIGN_DS, initAffectedA);
        processorB.applyPhase2_5StoreMappingsFull(FOREIGN_DS, initAffectedB);

        // === Simulate Phase 1: upsert event root entities ===
        for (RootEntity re : scenario.eventRootEntities()) {
            graphA.upsert(PRIMARY_DS, re.copy());
            graphB.upsert(PRIMARY_DS, re.copy());
        }

        Set<PropertyMapping<?, ?>> affectedA = new LinkedHashSet<>(List.of(mappingA));
        Set<PropertyMapping<?, ?>> affectedB = new LinkedHashSet<>(List.of(mappingB));

        // Path A: Optimized with primary datasource event
        OPTIMIZED_METHOD.invoke(processorA, PRIMARY_DS, affectedA,
                (List<?>) scenario.eventRootEntities());

        // Path B: Full pipeline
        processorB.applyPhase2_5StoreMappingsFull(PRIMARY_DS, affectedB);

        List<String> statesA = snapshotEntityStates(graphA);
        List<String> statesB = snapshotEntityStates(graphB);

        assertThat(statesA)
                .as("Optimized pipeline must match full pipeline for primary ds events "
                        + "(roots=%d, eventRoots=%d)",
                        scenario.rootEntities().size(),
                        scenario.eventRootEntities().size())
                .isEqualTo(statesB);
    }
}
