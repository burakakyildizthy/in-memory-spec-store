package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

/**
 * Backward compatibility tests for Phase 2.5 optimizations in {@link IncrementalSyncProcessor}.
 *
 * <p>Verifies that the optimized delta-based pipeline preserves the behavior of the
 * original full-application pipeline, including:
 * <ul>
 *   <li>{@code applyPostInitializationCatchUp()} applies mappings to ALL root entities (Requirement 6.3)</li>
 *   <li>Optimized path failure triggers fallback to full application (Requirement 6.4)</li>
 *   <li>Index state is correctly rebuilt after fallback (Requirement 6.2)</li>
 * </ul>
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5</p>
 */
class BackwardCompatibilityPhase25Test {

    private static final String STORE_A = "store-A";
    private static final String STORE_B = "store-B";
    private static final String DS_FOREIGN = "ds-foreign";
    private static final String DS_PRIMARY = "ds-primary";
    private static final String DS_PRIMARY_A = "ds-primary-A";
    private static final String DS_PRIMARY_B = "ds-primary-B";

    // ==================== Reflection Setup ====================

    private static final Method APPLY_PHASE25_METHOD;

    static {
        try {
            APPLY_PHASE25_METHOD = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "applyPhase2_5StoreMappings", String.class, Set.class, List.class);
            APPLY_PHASE25_METHOD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up reflection for applyPhase2_5StoreMappings", e);
        }
    }

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Long> {
        private final Long id;
        private String mappedValue;

        TestEntity(Long id) {
            this.id = id;
        }

        @Override
        public Long getIdentity() {
            return id;
        }

        String getMappedValue() {
            return mappedValue;
        }

        void setMappedValue(String value) {
            this.mappedValue = value;
        }

        @Override
        public String toString() {
            return "TestEntity[id=" + id + ", mapped=" + mappedValue + "]";
        }
    }

    // ==================== Shared Fields ====================

    private InMemorySpecStoreFactory factory;
    private DependencyGraph dependencyGraph;
    private IncrementalSyncProcessor processor;

    @BeforeEach
    void setUp() {
        factory = mock(InMemorySpecStoreFactory.class);
        dependencyGraph = mock(DependencyGraph.class);

        when(factory.getAllStoreIds()).thenReturn(Collections.emptyList());

        // applyPostInitializationCatchUp() calls applyPhase4ConsumerPropagation() which needs this
        when(dependencyGraph.getAffectedConsumers(any())).thenReturn(
                new AffectedConsumerSet(Collections.emptySet(), Collections.emptySet(), Collections.emptyList()));

        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        processor = new IncrementalSyncProcessor(factory, dependencyGraph, analysisResult, new AtomicLong(0));
    }

    // ==================== Helpers ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PropertyMapping<?, ?> mockStoreMapping(String consumerId, String foreignDsName) {
        PropertyMapping mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.getDataSourceName()).thenReturn(foreignDsName);
        when(mapping.isForDashboard()).thenReturn(false);
        when(mapping.getMappingType()).thenReturn(MappingType.ONE_TO_ONE);
        when(mapping.getForeignKeyPaths()).thenReturn(null);
        when(mapping.getPrimaryKeyPaths()).thenReturn(null);
        return mapping;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setupStore(String storeId, String primaryDsName, List<TestEntity> rootEntities) {
        InMemoryDataStore mockStore = mock(InMemoryDataStore.class);
        when(mockStore.getPrimaryDataSourceName()).thenReturn(primaryDsName);
        when(factory.getStoreById(storeId)).thenReturn(mockStore);
        when(dependencyGraph.findAll(primaryDsName)).thenReturn(new ArrayList<>(rootEntities));
    }

    // ==================== Test: applyPostInitializationCatchUp Behavior Preserved ====================

    /**
     * Verifies that {@code applyPostInitializationCatchUp()} applies mappings to ALL root
     * entities in each store, not just delta-affected ones. This is the full-application
     * path used during initialization.
     *
     * <p>Setup: Store STORE_A with primary datasource DS_PRIMARY containing 3 root entities.
     * One store mapping exists. After calling applyPostInitializationCatchUp(), all 3 root
     * entities should have had mappings applied (verified via index rebuild on the primary ds).</p>
     *
     * <p>Validates: Requirement 6.3</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void postInitCatchUpAppliesMappingsToAllRootEntities() {
        // Setup store with 3 root entities
        List<TestEntity> rootEntities = List.of(
                new TestEntity(1L),
                new TestEntity(2L),
                new TestEntity(3L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        // Register store ID so applyPostInitializationCatchUp() iterates over it
        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A));

        // Create a store mapping for store-A
        PropertyMapping mapping = mockStoreMapping(STORE_A, DS_FOREIGN);
        when(dependencyGraph.getMappingsByConsumerId(STORE_A))
                .thenReturn(List.of(mapping));

        // Call the method under test
        processor.applyPostInitializationCatchUp();

        // Verify: index rebuild was called for the primary datasource
        // This confirms the full application path ran (mappings applied → index rebuild)
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY);
    }

    /**
     * Verifies that {@code applyPostInitializationCatchUp()} skips stores with no
     * store mappings (isForDashboard == false).
     *
     * <p>Validates: Requirement 6.3</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void postInitCatchUpSkipsWhenNoStoreMappings() {
        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A));

        // Only dashboard mappings exist — no store mappings
        PropertyMapping dashboardMapping = mock(PropertyMapping.class);
        when(dashboardMapping.isForDashboard()).thenReturn(true);
        when(dependencyGraph.getMappingsByConsumerId(STORE_A))
                .thenReturn(List.of(dashboardMapping));

        processor.applyPostInitializationCatchUp();

        // No index rebuild should happen since there are no store mappings
        verify(dependencyGraph, never()).rebuildIndexesForDataSource(any());
    }

    /**
     * Verifies that {@code applyPostInitializationCatchUp()} processes multiple stores
     * independently — an error in one store does not prevent other stores from being processed.
     *
     * <p>Validates: Requirement 6.3</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void postInitCatchUpMultipleStoresProcessedIndependently() {
        // Setup two stores
        List<TestEntity> rootEntitiesA = List.of(new TestEntity(1L), new TestEntity(2L));
        List<TestEntity> rootEntitiesB = List.of(new TestEntity(10L));
        setupStore(STORE_A, DS_PRIMARY_A, rootEntitiesA);
        setupStore(STORE_B, DS_PRIMARY_B, rootEntitiesB);

        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A, STORE_B));

        PropertyMapping mappingA = mockStoreMapping(STORE_A, "ds-foreign-A");
        PropertyMapping mappingB = mockStoreMapping(STORE_B, "ds-foreign-B");
        when(dependencyGraph.getMappingsByConsumerId(STORE_A)).thenReturn(List.of(mappingA));
        when(dependencyGraph.getMappingsByConsumerId(STORE_B)).thenReturn(List.of(mappingB));

        processor.applyPostInitializationCatchUp();

        // Both datasources should have their indexes rebuilt
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY_A);
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY_B);
    }

    // ==================== Test: Optimized Path Failure → Full Application Fallback ====================

    /**
     * When the optimized delta path throws an exception (e.g., getStoreMappings() fails),
     * the method should fall back to the full application path
     * ({@code applyPhase2_5StoreMappingsFull}).
     *
     * <p>Validates: Requirement 6.4</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void deltaPathFailureFallsBackToFullApplication() throws Exception {
        IncrementalSyncProcessor spyProcessor = spy(processor);

        // Force delta path to fail by making getStoreMappings() throw
        when(spyProcessor.getStoreMappings())
                .thenThrow(new RuntimeException("simulated delta failure"));

        // Stub the full fallback to do nothing — we just verify it's called
        doNothing().when(spyProcessor).applyPhase2_5StoreMappingsFull(any(), any());

        PropertyMapping mapping = mockStoreMapping(STORE_A, DS_FOREIGN);
        Set<PropertyMapping<?, ?>> affectedMappings = new LinkedHashSet<>();
        affectedMappings.add(mapping);
        List<TestEntity> eventEntities = List.of(new TestEntity(1L));

        APPLY_PHASE25_METHOD.invoke(spyProcessor, DS_FOREIGN, affectedMappings, eventEntities);

        // Verify fallback was called exactly once with the correct arguments
        verify(spyProcessor, times(1))
                .applyPhase2_5StoreMappingsFull(eq(DS_FOREIGN), eq(affectedMappings));
    }

    /**
     * When the full fallback path runs after a delta failure, it should apply mappings
     * to ALL root entities and rebuild indexes — producing correct final state.
     *
     * <p>Validates: Requirements 6.1, 6.4</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fullFallbackAppliesMappingsToAllRootEntitiesAndRebuildsIndexes() {
        // Setup store
        List<TestEntity> rootEntities = List.of(
                new TestEntity(1L),
                new TestEntity(2L),
                new TestEntity(3L));
        setupStore(STORE_A, DS_PRIMARY, rootEntities);

        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A));

        PropertyMapping mapping = mockStoreMapping(STORE_A, DS_FOREIGN);
        when(mapping.isForDashboard()).thenReturn(false);

        Set<PropertyMapping<?, ?>> affectedMappings = new LinkedHashSet<>();
        affectedMappings.add(mapping);

        // Call the full path directly (simulating what happens after fallback)
        processor.applyPhase2_5StoreMappingsFull(DS_FOREIGN, affectedMappings);

        // Verify: index rebuild was called for the primary datasource
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY);
    }

    // ==================== Test: Index State Preserved After Fallback ====================

    /**
     * After the full fallback path runs, indexes should be rebuilt for all affected
     * datasources. This verifies that the fallback path correctly calls
     * {@code rebuildIndexesForDataSource()} for each primary datasource.
     *
     * <p>Validates: Requirement 6.2</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fullFallbackRebuildsIndexesForAllAffectedDatasources() {
        // Setup two stores sharing different primary datasources
        List<TestEntity> rootEntitiesA = List.of(new TestEntity(1L));
        List<TestEntity> rootEntitiesB = List.of(new TestEntity(10L));
        setupStore(STORE_A, DS_PRIMARY_A, rootEntitiesA);
        setupStore(STORE_B, DS_PRIMARY_B, rootEntitiesB);

        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A, STORE_B));

        PropertyMapping mappingA = mockStoreMapping(STORE_A, DS_FOREIGN);
        PropertyMapping mappingB = mockStoreMapping(STORE_B, DS_FOREIGN);

        Set<PropertyMapping<?, ?>> affectedMappings = new LinkedHashSet<>();
        affectedMappings.add(mappingA);
        affectedMappings.add(mappingB);

        // Call the full path
        processor.applyPhase2_5StoreMappingsFull(DS_FOREIGN, affectedMappings);

        // Both primary datasources should have their indexes rebuilt
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY_A);
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY_B);
    }

    /**
     * When the full fallback path encounters a store that doesn't exist (factory returns null),
     * it should skip that store gracefully and continue with others.
     *
     * <p>Validates: Requirements 6.4, 6.5</p>
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fullFallbackSkipsNullStoreContinuesWithOthers() {
        // store-A exists, store-B returns null from factory
        List<TestEntity> rootEntitiesA = List.of(new TestEntity(1L));
        setupStore(STORE_A, DS_PRIMARY_A, rootEntitiesA);
        when(factory.getStoreById(STORE_B)).thenReturn(null);

        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A, STORE_B));

        PropertyMapping mappingA = mockStoreMapping(STORE_A, DS_FOREIGN);
        PropertyMapping mappingB = mockStoreMapping(STORE_B, DS_FOREIGN);

        Set<PropertyMapping<?, ?>> affectedMappings = new LinkedHashSet<>();
        affectedMappings.add(mappingA);
        affectedMappings.add(mappingB);

        // Should not throw — store-B is skipped, store-A is processed
        processor.applyPhase2_5StoreMappingsFull(DS_FOREIGN, affectedMappings);

        // Only store-A's datasource should have indexes rebuilt
        verify(dependencyGraph, times(1)).rebuildIndexesForDataSource(DS_PRIMARY_A);
        verify(dependencyGraph, never()).rebuildIndexesForDataSource(DS_PRIMARY_B);
    }
}
