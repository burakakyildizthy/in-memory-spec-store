package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

/**
 * Unit tests for delta-based mapping resolution in {@link IncrementalSyncProcessor}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Early exit when no root entity is affected (Requirement 1.4)</li>
 *   <li>Foreign datasource event → FK reverse lookup finds root entity (Requirements 1.1, 1.2)</li>
 *   <li>Primary datasource event → direct root entity identification (Requirement 1.3)</li>
 *   <li>Fallback mechanism when delta path throws an exception (Requirement 6.4)</li>
 * </ul>
 *
 * <p>Uses reflection to access the private {@code resolveAffectedRootEntityIds()} method,
 * following the same pattern as {@link DeltaMappingTargetAccuracyPropertyTest}.</p>
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 6.4</p>
 */
class DeltaResolverTest {

    private static final String STORE_A = "store-A";
    private static final String DS_FOREIGN = "ds-foreign";
    private static final String DS_PRIMARY = "ds-primary";

    // ==================== Reflection Setup ====================

    private static final Method RESOLVE_METHOD;

    static {
        try {
            RESOLVE_METHOD = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "resolveAffectedRootEntityIds",
                    String.class, List.class, Map.class);
            RESOLVE_METHOD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up resolveAffectedRootEntityIds reflection", e);
        }
    }

    // ==================== Test Entity ====================

    /**
     * Simple test entity with a Long ID and a Long FK value.
     * Reuses the same pattern as {@link DeltaMappingTargetAccuracyPropertyTest.TestEntity}.
     */
    static class TestEntity implements Identifiable<Long> {
        private final Long id;
        private final Long fkValue;

        TestEntity(Long id, Long fkValue) {
            this.id = id;
            this.fkValue = fkValue;
        }

        @Override
        public Long getIdentity() {
            return id;
        }

        Long getFkValue() {
            return fkValue;
        }

        @Override
        public String toString() {
            return "TestEntity[id=" + id + ", fk=" + fkValue + "]";
        }
    }

    // ==================== Shared Fields ====================

    private InMemorySpecStoreFactory factory;
    private DependencyGraph dependencyGraph;
    private IncrementalSyncProcessor processor;

    private List<MetaAttribute<?, ?>> fkPath;
    private List<MetaAttribute<?, ?>> pkPath;

    @BeforeEach
    void setUp() {
        factory = mock(InMemorySpecStoreFactory.class);
        dependencyGraph = mock(DependencyGraph.class);

        when(factory.getAllStoreIds()).thenReturn(Collections.emptyList());

        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        processor = new IncrementalSyncProcessor(factory, dependencyGraph, analysisResult, new AtomicLong(0));

        MetaAttribute<?, ?> fkAttr = mock(MetaAttribute.class);
        MetaAttribute<?, ?> pkAttr = mock(MetaAttribute.class);
        fkPath = List.of(fkAttr);
        pkPath = List.of(pkAttr);
    }

    // ==================== Helpers ====================

    @SuppressWarnings("unchecked")
    private Set<Object> invokeResolve(String dataSourceName,
                                       List<? extends Identifiable<?>> eventEntities,
                                       Map<String, List<PropertyMapping<?, ?>>> mappingsByStore) throws Exception {
        return (Set<Object>) RESOLVE_METHOD.invoke(processor, dataSourceName, eventEntities, mappingsByStore);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PropertyMapping<?, ?> mockMapping(String consumerId, String foreignDsName,
                                               boolean hasFkPaths) {
        PropertyMapping mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.getDataSourceName()).thenReturn(foreignDsName);
        when(mapping.isForDashboard()).thenReturn(false);

        if (hasFkPaths) {
            when(mapping.getForeignKeyPaths()).thenReturn(List.of(fkPath));
            when(mapping.getPrimaryKeyPaths()).thenReturn(List.of(pkPath));

            // Source service: extracts FK value from event entities
            SpecificationService sourceService = mock(SpecificationService.class);
            when(sourceService.getValueByPath(any(), eq(fkPath))).thenAnswer(invocation -> {
                Object entity = invocation.getArgument(0);
                if (entity instanceof TestEntity te) {
                    return te.getFkValue();
                }
                return null;
            });
            when(mapping.getSourceService()).thenReturn(sourceService);

            // Target service: extracts PK value from root entities (PK = entity ID)
            SpecificationService targetService = mock(SpecificationService.class);
            when(targetService.getValueByPath(any(), eq(pkPath))).thenAnswer(invocation -> {
                Object entity = invocation.getArgument(0);
                if (entity instanceof TestEntity te) {
                    return te.getIdentity();
                }
                return null;
            });
            when(mapping.getTargetService()).thenReturn(targetService);
        } else {
            when(mapping.getForeignKeyPaths()).thenReturn(null);
            when(mapping.getPrimaryKeyPaths()).thenReturn(null);
            when(mapping.getSourceService()).thenReturn(null);
            when(mapping.getTargetService()).thenReturn(null);
        }

        return mapping;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setupStore(String storeId, String primaryDsName, List<TestEntity> rootEntities) {
        InMemoryDataStore mockStore = mock(InMemoryDataStore.class);
        when(mockStore.getPrimaryDataSourceName()).thenReturn(primaryDsName);
        when(factory.getStoreById(storeId)).thenReturn(mockStore);
        when(dependencyGraph.findAll(primaryDsName)).thenReturn(new ArrayList<>(rootEntities));
    }

    // ==================== Test: Early Exit — No Affected Entities ====================

    /**
     * When event entities have no FK relationship with any root entity AND the event
     * datasource is not a primary datasource, resolveAffectedRootEntityIds must return
     * an empty set.
     *
     * <p>Validates: Requirement 1.4</p>
     */
    @Test
    void noRootEntityAffectedReturnsEmptySet() throws Exception {
        // Store STORE_A has primary datasource DS_PRIMARY
        // Root entities have IDs 10, 20, 30
        List<TestEntity> rootEntities = List.of(
                new TestEntity(10L, 0L),
                new TestEntity(20L, 0L),
                new TestEntity(30L, 0L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        // Mapping: foreign datasource = DS_FOREIGN, with FK paths
        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);

        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        // Event from "ds-unrelated" — neither primary nor foreign datasource
        // Event entities have FK values 10, 20 but datasource doesn't match any mapping
        List<TestEntity> eventEntities = List.of(
                new TestEntity(1L, 10L),
                new TestEntity(2L, 20L));

        Set<Object> result = invokeResolve("ds-unrelated", eventEntities, mappingsByStore);

        assertThat(result)
                .as("No root entity should be affected when event datasource matches neither primary nor foreign")
                .isEmpty();
    }

    /**
     * When event entities list is empty, resolveAffectedRootEntityIds must return empty set.
     *
     * <p>Validates: Requirement 1.4</p>
     */
    @Test
    void emptyEventEntitiesReturnsEmptySet() throws Exception {
        setupStore(STORE_A, DS_PRIMARY, List.of(new TestEntity(10L, 0L)));

        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        Set<Object> result = invokeResolve(DS_FOREIGN, Collections.emptyList(), mappingsByStore);

        assertThat(result)
                .as("Empty event entities should produce empty affected set")
                .isEmpty();
    }

    /**
     * When mappingsByStore is empty, resolveAffectedRootEntityIds must return empty set.
     *
     * <p>Validates: Requirement 1.4</p>
     */
    @Test
    void emptyMappingsByStoreReturnsEmptySet() throws Exception {
        List<TestEntity> eventEntities = List.of(new TestEntity(1L, 10L));

        Set<Object> result = invokeResolve(DS_FOREIGN, eventEntities, Collections.emptyMap());

        assertThat(result)
                .as("Empty mappingsByStore should produce empty affected set")
                .isEmpty();
    }

    // ==================== Test: Foreign Datasource Event → FK Reverse Lookup ====================

    /**
     * When an event comes from a foreign datasource, the FK values of event entities
     * are matched against root entity PK values. Root entities whose PK matches an
     * event entity's FK should be in the result.
     *
     * <p>Validates: Requirements 1.1, 1.2</p>
     */
    @Test
    void foreignDatasourceEventFkReverseLookupFindsRootEntity() throws Exception {
        // Root entities with IDs 10, 20, 30
        List<TestEntity> rootEntities = List.of(
                new TestEntity(10L, 0L),
                new TestEntity(20L, 0L),
                new TestEntity(30L, 0L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        // Mapping: foreign datasource = DS_FOREIGN, with FK paths
        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);

        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        // Event from DS_FOREIGN — event entities have FK values pointing to root entity IDs 10 and 30
        List<TestEntity> eventEntities = List.of(
                new TestEntity(100L, 10L),  // FK=10 → matches root entity ID 10
                new TestEntity(101L, 30L)); // FK=30 → matches root entity ID 30

        Set<Object> result = invokeResolve(DS_FOREIGN, eventEntities, mappingsByStore);

        assertThat(result)
                .as("FK reverse lookup should find root entities whose PK matches event entity FK values")
                .containsExactlyInAnyOrder(10L, 30L);
    }

    /**
     * When a foreign datasource event has FK values that don't match any root entity PK,
     * the result should be empty.
     *
     * <p>Validates: Requirements 1.1, 1.4</p>
     */
    @Test
    void foreignDatasourceEventNoMatchingRootEntityReturnsEmpty() throws Exception {
        List<TestEntity> rootEntities = List.of(
                new TestEntity(10L, 0L),
                new TestEntity(20L, 0L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        // Event entities have FK values 999, 888 — no matching root entity PK
        List<TestEntity> eventEntities = List.of(
                new TestEntity(100L, 999L),
                new TestEntity(101L, 888L));

        Set<Object> result = invokeResolve(DS_FOREIGN, eventEntities, mappingsByStore);

        assertThat(result)
                .as("No root entity should be affected when FK values don't match any root PK")
                .isEmpty();
    }

    // ==================== Test: Primary Datasource Event → Direct Identification ====================

    /**
     * When an event comes from the primary datasource, the event entity IDs are
     * directly the affected root entity IDs.
     *
     * <p>Validates: Requirement 1.3</p>
     */
    @Test
    void primaryDatasourceEventDirectRootEntityIdentification() throws Exception {
        List<TestEntity> rootEntities = List.of(
                new TestEntity(10L, 0L),
                new TestEntity(20L, 0L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        // Mapping exists but its foreign datasource is different
        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        // Event from DS_PRIMARY — event entity IDs are directly affected root entity IDs
        List<TestEntity> eventEntities = List.of(
                new TestEntity(10L, 0L),
                new TestEntity(20L, 0L));

        Set<Object> result = invokeResolve(DS_PRIMARY, eventEntities, mappingsByStore);

        assertThat(result)
                .as("Primary datasource event entities should be directly identified as affected root entities")
                .containsExactlyInAnyOrder(10L, 20L);
    }

    /**
     * When an event comes from the primary datasource, even new entity IDs (not yet
     * in root entities) should be in the affected set.
     *
     * <p>Validates: Requirement 1.3</p>
     */
    @Test
    void primaryDatasourceEventNewEntityIdsAreIncluded() throws Exception {
        List<TestEntity> rootEntities = List.of(new TestEntity(10L, 0L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        PropertyMapping<?, ?> mapping = mockMapping(STORE_A, DS_FOREIGN, true);
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();
        mappingsByStore.put(STORE_A, List.of(mapping));

        // Event from DS_PRIMARY with a new entity ID 99 (not in existing root entities)
        List<TestEntity> eventEntities = List.of(new TestEntity(99L, 0L));

        Set<Object> result = invokeResolve(DS_PRIMARY, eventEntities, mappingsByStore);

        assertThat(result)
                .as("New entity IDs from primary datasource should be included in affected set")
                .containsExactly(99L);
    }

    // ==================== Test: Fallback Mechanism ====================

    /**
     * When the delta-based path (inside the try block of applyPhase2_5StoreMappings)
     * throws an exception, the method should fall back to the full application path
     * (applyPhase2_5StoreMappingsFull).
     *
     * <p>We force an exception by making {@code getStoreMappings()} throw on the spy,
     * then verify that {@code applyPhase2_5StoreMappingsFull} is called as fallback.</p>
     *
     * <p>Validates: Requirement 6.4</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fallbackMechanismDeltaFailureTriggersFullApplication() throws Exception {
        // Create a spy processor so we can intercept method calls
        IncrementalSyncProcessor spyProcessor = spy(processor);

        // Make getStoreMappings() throw — this is called early in the try block
        // of applyPhase2_5StoreMappings, triggering the catch → fallback path
        when(spyProcessor.getStoreMappings())
                .thenThrow(new RuntimeException("simulated delta failure"));

        // Stub the fallback method to do nothing (we only want to verify it's called)
        org.mockito.Mockito.doNothing().when(spyProcessor)
                .applyPhase2_5StoreMappingsFull(any(), any());

        // Prepare arguments
        PropertyMapping mapping = mock(PropertyMapping.class);
        when(mapping.isForDashboard()).thenReturn(false);
        Set<PropertyMapping<?, ?>> affectedMappings = new LinkedHashSet<>();
        affectedMappings.add(mapping);
        List<TestEntity> eventEntities = List.of(new TestEntity(1L, 10L));

        // Use reflection to call the private applyPhase2_5StoreMappings method
        Method deltaMethod = IncrementalSyncProcessor.class.getDeclaredMethod(
                "applyPhase2_5StoreMappings", String.class, Set.class, List.class);
        deltaMethod.setAccessible(true);

        deltaMethod.invoke(spyProcessor, DS_FOREIGN, affectedMappings, eventEntities);

        // Verify that the full fallback method was called exactly once
        verify(spyProcessor, times(1))
                .applyPhase2_5StoreMappingsFull(eq(DS_FOREIGN), eq(affectedMappings));
    }
}
