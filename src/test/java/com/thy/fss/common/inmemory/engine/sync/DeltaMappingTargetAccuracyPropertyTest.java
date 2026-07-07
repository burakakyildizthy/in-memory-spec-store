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
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
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
 * Property-based test for delta mapping target accuracy.
 *
 * <p>Verifies that {@code resolveAffectedRootEntityIds()} returns exactly the union of
 * FK-related root entities and primary datasource event entities — no false positives,
 * no false negatives.</p>
 *
 * <p>Since {@code resolveAffectedRootEntityIds()} is a private method in
 * {@link IncrementalSyncProcessor}, reflection is used to invoke it.</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3</b></p>
 */
class DeltaMappingTargetAccuracyPropertyTest {

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

    // ==================== Test Scenario Model ====================

    /**
     * Encapsulates a complete test scenario for property generation.
     */
    static final class TestScenario {
        final String eventDataSourceName;
        final List<TestEntity> eventEntities;
        final List<StoreConfig> stores;
        TestScenario(String eventDataSourceName, List<TestEntity> eventEntities, List<StoreConfig> stores) {
            this.eventDataSourceName = eventDataSourceName;
            this.eventEntities = eventEntities;
            this.stores = stores;
        }
        String eventDataSourceName() { return eventDataSourceName; }
        List<TestEntity> eventEntities() { return eventEntities; }
        List<StoreConfig> stores() { return stores; }
    }

    static final class StoreConfig {
        final String storeId;
        final String primaryDataSourceName;
        final List<TestEntity> rootEntities;
        final List<MappingConfig> mappings;
        StoreConfig(String storeId, String primaryDataSourceName, List<TestEntity> rootEntities, List<MappingConfig> mappings) {
            this.storeId = storeId;
            this.primaryDataSourceName = primaryDataSourceName;
            this.rootEntities = rootEntities;
            this.mappings = mappings;
        }
        String storeId() { return storeId; }
        String primaryDataSourceName() { return primaryDataSourceName; }
        List<TestEntity> rootEntities() { return rootEntities; }
        List<MappingConfig> mappings() { return mappings; }
    }

    static final class MappingConfig {
        final String foreignDataSourceName;
        final boolean hasFkPaths;
        MappingConfig(String foreignDataSourceName, boolean hasFkPaths) {
            this.foreignDataSourceName = foreignDataSourceName;
            this.hasFkPaths = hasFkPaths;
        }
        String foreignDataSourceName() { return foreignDataSourceName; }
        boolean hasFkPaths() { return hasFkPaths; }
    }

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<TestScenario> testScenarios() {
        Arbitrary<String> dsNames = Arbitraries.of(
                "ds-primary-A", "ds-primary-B", "ds-foreign-X", "ds-foreign-Y");

        Arbitrary<TestEntity> eventEntityArb = Combinators.combine(
                Arbitraries.longs().between(1L, 100L),
                Arbitraries.longs().between(1L, 50L)
        ).as(TestEntity::new);

        Arbitrary<TestEntity> rootEntityArb = Combinators.combine(
                Arbitraries.longs().between(1L, 50L),
                Arbitraries.just(0L) // root entities don't need FK for this test
        ).as(TestEntity::new);

        Arbitrary<MappingConfig> mappingArb = Combinators.combine(
                dsNames,
                Arbitraries.of(true, false)
        ).as(MappingConfig::new);

        // Generate 1-3 stores with guaranteed unique storeIds
        String[] storeIds = {"store-A", "store-B", "store-C"};
        Arbitrary<Integer> storeCountArb = Arbitraries.integers().between(1, 3);

        Arbitrary<List<StoreConfig>> storesArb = storeCountArb.flatMap(count -> {
            List<Arbitrary<StoreConfig>> storeArbs = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String storeId = storeIds[i];
                storeArbs.add(Combinators.combine(
                        Arbitraries.just(storeId),
                        dsNames,
                        rootEntityArb.list().ofMinSize(1).ofMaxSize(5),
                        mappingArb.list().ofMinSize(1).ofMaxSize(3)
                ).as(StoreConfig::new));
            }
            return Combinators.combine(storeArbs).as(list -> list);
        });

        return Combinators.combine(
                dsNames,
                eventEntityArb.list().ofMinSize(1).ofMaxSize(5),
                storesArb
        ).as(TestScenario::new);
    }

    // ==================== Helpers ====================


    /**
     * Creates an IncrementalSyncProcessor with mocked dependencies configured
     * for the given test scenario.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private IncrementalSyncProcessor createProcessor(TestScenario scenario) {
        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);
        DependencyGraph dependencyGraph = mock(DependencyGraph.class);

        // factory.getAllStoreIds() — needed by registerMappingIndexes() in constructor
        when(factory.getAllStoreIds()).thenReturn(Collections.emptyList());

        // Accumulate root entities per datasource (multiple stores may share the same primaryDs)
        Map<String, List<TestEntity>> rootEntitiesByDs = new HashMap<>();
        for (StoreConfig store : scenario.stores()) {
            rootEntitiesByDs.computeIfAbsent(store.primaryDataSourceName(), k -> new ArrayList<>())
                    .addAll(store.rootEntities());
        }

        // Set up factory.getStoreById() for each store in the scenario
        for (StoreConfig store : scenario.stores()) {
            InMemoryDataStore mockStore = mock(InMemoryDataStore.class);
            when(mockStore.getPrimaryDataSourceName()).thenReturn(store.primaryDataSourceName());
            when(factory.getStoreById(store.storeId())).thenReturn(mockStore);
        }

        // Set up dependencyGraph.findAll() once per datasource with combined root entities
        for (Map.Entry<String, List<TestEntity>> dsEntry : rootEntitiesByDs.entrySet()) {
            when(dependencyGraph.findAll(dsEntry.getKey()))
                    .thenReturn(new ArrayList<>(dsEntry.getValue()));
        }

        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        return new IncrementalSyncProcessor(factory, dependencyGraph, analysisResult, new AtomicLong(0));
    }

    /**
     * Builds the mappingsByStore map with properly mocked PropertyMappings.
     * Each mapping has mocked sourceService, targetService, FK paths, and PK paths.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, List<PropertyMapping<?, ?>>> buildMappingsByStore(
            TestScenario scenario) {

        // Shared mock MetaAttribute instances for FK and PK paths
        MetaAttribute fkAttr = mock(MetaAttribute.class);
        MetaAttribute pkAttr = mock(MetaAttribute.class);
        List<MetaAttribute<?, ?>> fkPath = List.of(fkAttr);
        List<MetaAttribute<?, ?>> pkPath = List.of(pkAttr);

        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = new HashMap<>();

        for (StoreConfig store : scenario.stores()) {
            List<PropertyMapping<?, ?>> storeMappings = new ArrayList<>();

            for (MappingConfig mc : store.mappings()) {
                PropertyMapping mapping = mock(PropertyMapping.class);
                when(mapping.getConsumerId()).thenReturn(store.storeId());
                when(mapping.getDataSourceName()).thenReturn(mc.foreignDataSourceName());
                when(mapping.isForDashboard()).thenReturn(false);

                if (mc.hasFkPaths()) {
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
                    // No FK paths — mapping won't contribute to FK-based resolution
                    when(mapping.getForeignKeyPaths()).thenReturn(null);
                    when(mapping.getPrimaryKeyPaths()).thenReturn(null);
                    when(mapping.getSourceService()).thenReturn(null);
                    when(mapping.getTargetService()).thenReturn(null);
                }

                storeMappings.add(mapping);
            }

            mappingsByStore.put(store.storeId(), storeMappings);
        }

        return mappingsByStore;
    }

    /**
     * Computes the expected set of affected root entity IDs independently (the oracle).
     * This mirrors the specification logic without depending on the implementation.
     */
    private Set<Object> computeExpectedAffectedIds(TestScenario scenario) {
        Set<Object> expected = new LinkedHashSet<>();

        // Build combined root entities per datasource (mirrors real DependencyGraph behavior)
        Map<String, List<TestEntity>> rootEntitiesByDs = new HashMap<>();
        for (StoreConfig store : scenario.stores()) {
            rootEntitiesByDs.computeIfAbsent(store.primaryDataSourceName(), k -> new ArrayList<>())
                    .addAll(store.rootEntities());
        }

        for (StoreConfig store : scenario.stores()) {
            String primaryDs = store.primaryDataSourceName();

            // Scenario 2: Event datasource = primary datasource
            // Event entity IDs are directly affected
            if (scenario.eventDataSourceName().equals(primaryDs)) {
                for (TestEntity eventEntity : scenario.eventEntities()) {
                    expected.add(eventEntity.getIdentity());
                }
            }

            // Scenario 1: Event datasource = foreign datasource
            // Check each mapping to see if its dataSourceName matches event datasource
            for (MappingConfig mc : store.mappings()) {
                if (!scenario.eventDataSourceName().equals(mc.foreignDataSourceName()) || !mc.hasFkPaths()) {
                    continue;
                }

                // Collect FK values from event entities
                Set<Object> fkValues = new LinkedHashSet<>();
                for (TestEntity eventEntity : scenario.eventEntities()) {
                    Long fk = eventEntity.getFkValue();
                    if (fk != null) {
                        fkValues.add(fk);
                    }
                }

                if (!fkValues.isEmpty()) {
                    // Match against combined root entity PK values for this datasource
                    List<TestEntity> allRootEntities = rootEntitiesByDs.getOrDefault(primaryDs, Collections.emptyList());
                    for (TestEntity rootEntity : allRootEntities) {
                        Long pk = rootEntity.getIdentity();
                        if (pk != null && fkValues.contains(pk)) {
                            expected.add(pk);
                        }
                    }
                }
            }
        }

        return expected;
    }

    // ==================== Property Tests ====================

    /**
     * Property 1: Delta Mapping Target Accuracy
     *
     * <p>For any random entity/mapping/event combination, the set returned by
     * {@code resolveAffectedRootEntityIds()} must equal exactly the union of:
     * <ul>
     *   <li>Root entities whose PK values match FK values extracted from event entities
     *       (foreign datasource scenario)</li>
     *   <li>Event entity IDs when the event datasource is the primary datasource</li>
     * </ul>
     * No false positives, no false negatives.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 1.3</b></p>
     */
    @Property(tries = 100)
    @SuppressWarnings("unchecked")
    void resolveAffectedRootEntityIdsMatchesOracleComputation(
            @ForAll("testScenarios") TestScenario scenario) throws Exception {

        IncrementalSyncProcessor processor = createProcessor(scenario);
        Map<String, List<PropertyMapping<?, ?>>> mappingsByStore = buildMappingsByStore(scenario);

        // Invoke the private method via reflection
        Set<Object> actual = (Set<Object>) RESOLVE_METHOD.invoke(
                processor,
                scenario.eventDataSourceName(),
                scenario.eventEntities(),
                mappingsByStore);

        // Compute expected result independently
        Set<Object> expected = computeExpectedAffectedIds(scenario);

        assertThat(actual)
                .as("resolveAffectedRootEntityIds() must return exactly the expected affected IDs "
                        + "for eventDs='%s', eventEntities=%s, stores=%s",
                        scenario.eventDataSourceName(),
                        scenario.eventEntities(),
                        scenario.stores())
                .isEqualTo(expected);
    }
}
