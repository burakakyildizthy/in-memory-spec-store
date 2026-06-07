package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.LongAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;

import net.jqwik.api.Property;

/**
 * Preservation Property Test — PropertyMapping Tabanlı Yayılım ve Pipeline Davranışı Korunması.
 *
 * <p>Bu test sınıfı, bug koşulu geçerli OLMADIĞINDA mevcut pipeline davranışının
 * korunduğunu doğrular. Gözlem-öncelikli (observation-first) metodolojisi ile
 * düzeltilmemiş kodda davranış gözlemlenir ve property olarak kodlanır.</p>
 *
 * <p><b>Property 2: Preservation — Mevcut Davranış Korunması</b></p>
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b></p>
 */
class PrimaryDatasourceConsumerPreservationPropertyTest {

    private static final String SOURCE_DS = "sourceDS";
    private static final String SPEC_DS = "specDS";
    private static final String MIX_DS = "mixDS";
    private static final String INIT_DS = "initDS";
    private static final String EVENT_DS = "eventDS";
    private static final String STREAM_DS = "streamDS";
    private static final String SPEC_STORE = "specStore";
    private static final String MAPPED_STORE = "mappedStore";

    // ==================== Helper: Register store via reflection ====================

    private static <T> void registerStoreViaReflection(InMemorySpecStoreFactory factory,
                                                       InMemoryDataStore<T> store) throws Exception {
        Method registerStoreMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStoreMethod.setAccessible(true);
        registerStoreMethod.invoke(factory, store);
    }

    // ==================== Helper: Create SimpleUser ====================

    private static SimpleUser createUser(Long id, String name, boolean active) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setActive(active);
        return user;
    }

    // ==================== Helper: Create a store PropertyMapping (mock-based) ====================

    /**
     * Creates a minimal valid PropertyMapping for a store (non-dashboard).
     * Uses LongAttribute + mocked SpecificationService to avoid annotation processor dependencies.
     */
    @SuppressWarnings("unchecked")
    private static <T, F> PropertyMapping<T, F> createStorePropertyMapping(
            String consumerId, String dataSourceName, Class<T> targetClass) {
        LongAttribute<T> idAttr = new LongAttribute<>("id", targetClass);
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(idAttr);

        // Use mock SpecificationServices — configure getEntityClass() to return
        // non-null values required by index registration in IncrementalSyncProcessor
        SpecificationService<?> sourceService = mock(SpecificationService.class);
        when(sourceService.getEntityClass()).thenReturn((Class) targetClass);
        SpecificationService<T> targetService = (SpecificationService<T>) mock(SpecificationService.class);
        when(targetService.getEntityClass()).thenReturn(targetClass);

        // PK/FK paths required for store mappings validation
        LongAttribute<T> pkAttr = new LongAttribute<>("id", targetClass);
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = List.of(List.of(pkAttr));
        LongAttribute<T> fkAttr = new LongAttribute<>("id", targetClass);
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = List.of(List.of(fkAttr));

        return PropertyMapping.<T, F>builder()
                .consumerId(consumerId)
                .isForDashboard(false)
                .targetPath(targetPath)
                .datasourceName(dataSourceName)
                .sourceService(sourceService)
                .targetService(targetService)
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .mappingType(MappingType.ONE_TO_ONE)
                .build();
    }

    // ==================== Scenario 1: PropertyMapping-based store propagation preserved ====================

    /**
     * Observation 1: When a store has PropertyMapping AND its primaryDataSourceName matches
     * the source dataSourceName, entities are written correctly (no type mismatch).
     * This should be preserved after the fix.
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     */
    @Property(tries = 1)
    void preservationPropertyMappingBasedStorePropagationShouldContinueWorking() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping(MAPPED_STORE, SOURCE_DS, SimpleUser.class);

            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mapping));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(SOURCE_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(SOURCE_DS, streamingDs);

            // Store with primaryDataSourceName matching sourceDS — no type mismatch
            InMemoryDataStore<SimpleUser> mappedStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    MAPPED_STORE,
                    SOURCE_DS,     // matches the streaming DS name
                    null,
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, mappedStore);

            assertThat(mappedStore.findAll()).isEmpty();

            AffectedConsumerSet consumers = graph.getAffectedConsumers(SOURCE_DS);
            assertThat(consumers.getStoreIds()).contains(MAPPED_STORE);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "User1", true),
                    createUser(2L, "User2", false),
                    createUser(3L, "User3", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(SOURCE_DS, event);

            // Preservation: entities in DependencyGraph
            assertThat(graph.findAll(SOURCE_DS))
                    .as("Phase 1 preservation: entities should be in DependencyGraph")
                    .hasSize(3);

            // Preservation: Phase 4 propagates entities to store via PropertyMapping
            assertThat(mappedStore.findAll())
                    .as("Preservation (3.1, 3.2): PropertyMapping-based store propagation should work — "
                            + "store should contain all 3 entities after Phase 4")
                    .hasSize(3);
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 2: INITIALIZING datasource skips Phase 2-4 ====================

    /**
     * Observation: When a streaming datasource is in INITIALIZING state, Phase 1 upserts
     * entities to DependencyGraph but Phase 2-4 are skipped entirely.
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Property(tries = 1)
    void preservationInitializingDatasourceShouldSkipPhase2Through4() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            graph.build(Collections.emptyList());

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(INIT_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
            factory.registerDataSource(INIT_DS, streamingDs);

            InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "initStore",
                    INIT_DS,
                    null,
                    Collections.emptyList()
            );
            registerStoreViaReflection(factory, store);

            assertThat(store.findAll()).isEmpty();

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "InitUser1", true),
                    createUser(2L, "InitUser2", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(INIT_DS, event);

            assertThat(graph.findAll(INIT_DS))
                    .as("Phase 1 preservation: entities should be in DependencyGraph even during INITIALIZING")
                    .hasSize(2);

            assertThat(store.findAll())
                    .as("Preservation (3.5): INITIALIZING datasource should skip Phase 2-4 — "
                            + "store should remain empty")
                    .isEmpty();
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 3: Non-matching primaryDataSourceName store not affected ====================

    /**
     * Observation 7: Stores with different primaryDataSourceName and no PropertyMapping
     * are correctly not updated.
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     */
    @Property(tries = 1)
    void preservationNonMatchingPrimaryDataSourceNameStoreShouldNotBeAffected() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            graph.build(Collections.emptyList());

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(EVENT_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(EVENT_DS, streamingDs);

            // Store with DIFFERENT primaryDataSourceName, no PropertyMapping
            InMemoryDataStore<SimpleUser> otherStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "otherStore",
                    "otherDS",              // does NOT match EVENT_DS
                    null,
                    Collections.emptyList()
            );
            registerStoreViaReflection(factory, otherStore);

            assertThat(otherStore.findAll()).isEmpty();

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "EventUser1", true),
                    createUser(2L, "EventUser2", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(EVENT_DS, event);

            assertThat(graph.findAll(EVENT_DS))
                    .as("Phase 1: entities should be in DependencyGraph")
                    .hasSize(2);

            assertThat(otherStore.findAll())
                    .as("Preservation (3.1, 3.2): Store with primaryDataSourceName='otherDS' should not "
                            + "be affected by event for 'eventDS'")
                    .isEmpty();
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 4: Null primaryDataSourceName store skipped ====================

    /**
     * Observation: Stores with null primaryDataSourceName are not affected
     * by any streaming event unless they have a PropertyMapping.
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     */
    @Property(tries = 1)
    void preservationNullPrimaryDataSourceNameStoreShouldBeSkipped() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            graph.build(Collections.emptyList());

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(STREAM_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(STREAM_DS, streamingDs);

            InMemoryDataStore<SimpleUser> nullPrimaryStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "nullPrimaryStore",
                    null,                   // null primaryDataSourceName
                    null,
                    Collections.emptyList()
            );
            registerStoreViaReflection(factory, nullPrimaryStore);

            assertThat(nullPrimaryStore.findAll()).isEmpty();

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(createUser(1L, "StreamUser1", true));
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(STREAM_DS, event);

            assertThat(graph.findAll(STREAM_DS))
                    .as("Phase 1: entities should be in DependencyGraph")
                    .hasSize(1);

            assertThat(nullPrimaryStore.findAll())
                    .as("Preservation (3.1, 3.2): Store with null primaryDataSourceName should not "
                            + "be affected by any streaming event")
                    .isEmpty();
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 5: PropertyMapping + rootSpecification — filtered entities propagated ====================

    /**
     * After fix: when a store has PropertyMapping AND rootSpecification,
     * Phase 4 correctly applies rootSpecification filtering — only matching entities
     * are written to the store. This validates the fix is working correctly.
     *
     * <p>Note: primaryDataSourceName matches sourceDS (no type mismatch), so this tests
     * the rootSpecification filter behavior in isolation.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     */
    @Property(tries = 1)
    void preservationPropertyMappingWithRootSpecificationFilteredEntitiesPropagated() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping(SPEC_STORE, SPEC_DS, SimpleUser.class);

            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mapping));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(SPEC_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(SPEC_DS, streamingDs);

            // rootSpecification: only active users
            Specification<SimpleUser> activeOnlySpec = () -> user ->
                    user.getActive() != null && user.getActive();

            // Store with PropertyMapping + rootSpecification, primaryDS matches sourceDS
            InMemoryDataStore<SimpleUser> specStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    SPEC_STORE,
                    SPEC_DS,           // matches source DS — no type mismatch
                    activeOnlySpec,
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, specStore);

            AffectedConsumerSet consumers = graph.getAffectedConsumers(SPEC_DS);
            assertThat(consumers.getStoreIds()).contains(SPEC_STORE);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "Active1", true),
                    createUser(2L, "Inactive1", false),
                    createUser(3L, "Active2", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(SPEC_DS, event);

            // After fix: PropertyMapping path now correctly applies rootSpecification filter
            // Only 2 active users pass the filter (Active1 and Active2)
            assertThat(specStore.findAll())
                    .as("Preservation (3.1, 3.2): After fix, Phase 4 PropertyMapping path applies "
                            + "rootSpecification filtering — only 2 active entities expected")
                    .hasSize(2);
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 6: Mixed store configurations ====================

    /**
     * Observation 7: Multiple stores with different configs — only PropertyMapping-mapped
     * store updated, others not. Verifies the storeIds.contains(storeId) skip logic.
     *
     * <p><b>Validates: Requirements 3.1, 3.2</b></p>
     */
    @Property(tries = 1)
    void preservationMixedStoreConfigurationsOnlyPropertyMappedStoreUpdated() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping("mappedMixStore", MIX_DS, SimpleUser.class);

            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mapping));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(MIX_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(MIX_DS, streamingDs);

            // Store 1: HAS PropertyMapping from MIX_DS → will be updated
            InMemoryDataStore<SimpleUser> mappedMixStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "mappedMixStore",
                    MIX_DS,
                    null,
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, mappedMixStore);

            // Store 2: Different primaryDataSourceName, no PropertyMapping → not affected
            InMemoryDataStore<SimpleUser> unrelatedStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "unrelatedStore",
                    "differentDS",
                    null,
                    Collections.emptyList()
            );
            registerStoreViaReflection(factory, unrelatedStore);

            // Store 3: Null primaryDataSourceName, no PropertyMapping → not affected
            InMemoryDataStore<SimpleUser> nullStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "nullStore",
                    null,
                    null,
                    Collections.emptyList()
            );
            registerStoreViaReflection(factory, nullStore);

            assertThat(mappedMixStore.findAll()).isEmpty();
            assertThat(unrelatedStore.findAll()).isEmpty();
            assertThat(nullStore.findAll()).isEmpty();

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "MixUser1", true),
                    createUser(2L, "MixUser2", false)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(MIX_DS, event);

            // Preservation: PropertyMapping-based store IS updated
            assertThat(mappedMixStore.findAll())
                    .as("Preservation (3.1): Store with PropertyMapping should be updated")
                    .hasSize(2);

            // Preservation: Unrelated store is NOT updated
            assertThat(unrelatedStore.findAll())
                    .as("Preservation (3.2): Store with different primaryDataSourceName should not be affected")
                    .isEmpty();

            // Preservation: Null primary store is NOT updated
            assertThat(nullStore.findAll())
                    .as("Preservation (3.2): Store with null primaryDataSourceName should not be affected")
                    .isEmpty();
        } finally {
            factory.clearAll();
        }
    }
}
