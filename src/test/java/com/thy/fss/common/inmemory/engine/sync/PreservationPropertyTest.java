package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.jqwik.api.Example;
import net.jqwik.api.Label;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AggregationTask;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan;
import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.LongAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;

/**
 * Preservation Property Tests — Mapping'siz ve INITIALIZING Datasource Davranışı Koruması
 *
 * <p>Bu testler, fix'lenmemiş kodda non-buggy input'lar için mevcut davranışı doğrular.
 * Hata koşulunun geçerli OLMADIĞI senaryolarda (mapping'siz datasource, INITIALIZING state,
 * dashboard-only mapping, boş affected mapping seti) pipeline davranışının korunduğunu kanıtlar.</p>
 *
 * <p><b>BEKLENEN SONUÇ</b>: Bu testler fix'lenmemiş kodda BAŞARILI olmalıdır.
 * Başarı, baseline davranışının doğrulandığını gösterir.</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7</b></p>
 */
class PreservationPropertyTest {

    private InMemorySpecStoreFactory factory;

    // ==================== Dashboard Data Target Class ====================

    public static class DashboardData {
        private long totalCount;

        public DashboardData() {
            this.totalCount = 0;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    }

    // ==================== MetaAttributes ====================

    static final LongAttribute<DashboardData> TOTAL_COUNT_ATTR =
            new LongAttribute<>("totalCount", DashboardData.class);

    // ==================== SpecificationService for DashboardData ====================

    static class DashboardDataSpecService implements SpecificationService<DashboardData> {
        @Override public Class<DashboardData> getEntityClass() { return DashboardData.class; }
        @Override public DashboardData createInstance() throws Exception { return new DashboardData(); }

        @Override
        public Object getFieldValue(DashboardData entity, String fieldName) {
            if ("totalCount".equals(fieldName)) return entity.getTotalCount();
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        @Override
        public Object getFieldValue(DashboardData entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(DashboardData entity, MetaAttribute<?, ?> attribute, Object value) {
            if ("totalCount".equals(attribute.getName())) {
                entity.setTotalCount(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException("Unknown field: " + attribute.getName());
            }
        }

        @Override
        public Object getValueByPath(DashboardData entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException("Nested paths not supported");
        }

        @Override
        public void setValueByPath(DashboardData entity, List<MetaAttribute<?, ?>> path, Object value) {
            if (path.size() == 1) { setFieldValue(entity, path.get(0), value); return; }
            throw new UnsupportedOperationException("Nested paths not supported");
        }

        @Override public boolean validateSpecification(DashboardData e, MetaAttribute<DashboardData, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
        @Override public boolean validateFilter(DashboardData e, Object f) { throw new UnsupportedOperationException(); }
        @Override public Comparator<DashboardData> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<DashboardData> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
        @Override public Comparator<DashboardData> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<DashboardData> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<DashboardData> c, CollectionSelector s, Specification<DashboardData> sp) { throw new UnsupportedOperationException(); }
        @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public Object getValueByPathWithCollections(DashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
        @Override public void setValueByPathWithCollections(DashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
    }


    // ==================== SpecificationService for SimpleUser ====================

    static class SimpleUserSpecService implements SpecificationService<SimpleUser> {
        @Override public Class<SimpleUser> getEntityClass() { return SimpleUser.class; }
        @Override public SimpleUser createInstance() throws Exception { return new SimpleUser(); }

        @Override
        public Object getFieldValue(SimpleUser entity, String fieldName) {
            return switch (fieldName) {
                case "id" -> entity.getIdentity();
                case "name" -> entity.getName();
                case "age" -> entity.getAge();
                case "active" -> entity.getActive();
                default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
            };
        }

        @Override
        public Object getFieldValue(SimpleUser entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(SimpleUser entity, MetaAttribute<?, ?> attribute, Object value) {
            throw new UnsupportedOperationException("Not needed for source entity");
        }

        @Override
        public Object getValueByPath(SimpleUser entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException("Nested paths not supported");
        }

        @Override
        public void setValueByPath(SimpleUser entity, List<MetaAttribute<?, ?>> path, Object value) {
            throw new UnsupportedOperationException("Not needed for source entity");
        }

        @Override public boolean validateSpecification(SimpleUser e, MetaAttribute<SimpleUser, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
        @Override public boolean validateFilter(SimpleUser e, Object f) { throw new UnsupportedOperationException(); }
        @Override public Comparator<SimpleUser> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<SimpleUser> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
        @Override public Comparator<SimpleUser> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<SimpleUser> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<SimpleUser> c, CollectionSelector s, Specification<SimpleUser> sp) { throw new UnsupportedOperationException(); }
        @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public Object getValueByPathWithCollections(SimpleUser e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
        @Override public void setValueByPathWithCollections(SimpleUser e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
    }

    private static final DashboardDataSpecService DASHBOARD_SPEC_SERVICE = new DashboardDataSpecService();
    private static final SimpleUserSpecService SOURCE_SPEC_SERVICE = new SimpleUserSpecService();

    // ==================== Setup / Teardown ====================

    @BeforeProperty
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
    }

    @AfterProperty
    void tearDown() {
        factory.clearAll();
    }

    // ==================== Helpers ====================

    @SuppressWarnings("unchecked")
    private <T extends com.thy.fss.common.inmemory.entity.Identifiable<?>> void registerReadyDatasource(String dsName) {
        StreamingDataSource<T> ds = mock(StreamingDataSource.class);
        when(ds.getName()).thenReturn(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(dsName, ds);
    }

    @SuppressWarnings("unchecked")
    private <T extends com.thy.fss.common.inmemory.entity.Identifiable<?>> void registerInitializingDatasource(String dsName) {
        StreamingDataSource<T> ds = mock(StreamingDataSource.class);
        when(ds.getName()).thenReturn(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
        factory.registerDataSource(dsName, ds);
    }

    private void registerStore(InMemoryDataStore<?> store) throws Exception {
        Method registerStore = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStore.setAccessible(true);
        registerStore.invoke(factory, store);
    }

    private void registerDashboard(Dashboard<?> dashboard, String dashboardId) throws Exception {
        Method registerDashboard = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerDashboard", Dashboard.class, String.class, List.class);
        registerDashboard.setAccessible(true);
        registerDashboard.invoke(factory, dashboard, dashboardId, Collections.emptyList());
    }

    private static SimpleUser createUser(long id, String name, int age) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setAge(age);
        user.setActive(true);
        return user;
    }

    // ==================== Test 1: Mapping-less Datasource Preservation ====================

    /**
     * Mapping'i olmayan datasource'dan event gönderildiğinde store davranışının korunması.
     *
     * <p>Scenario: Datasource has NO store mappings registered in DependencyGraph.
     * When entities arrive via streaming, Phase 1 upserts them to DependencyGraph,
     * Phase 2 finds no affected mappings, and Phase 4 propagates entities to store
     * via primary datasource discovery.</p>
     *
     * <p>This verifies that mapping-less datasources work correctly without any
     * Phase 2.5 interference.</p>
     *
     * <p><b>Validates: Requirements 3.5, 3.7</b></p>
     */
    @Example
    @Label("Mapping-less datasource: entities propagated to store via primary datasource discovery — SHOULD PASS")
    void mappinglessDatasourceEntitiesPropagatedToStore() throws Exception {
        String dsName = "no-mapping-ds";
        String storeId = "no-mapping-store";

        // Register READY datasource with NO mappings
        registerReadyDatasource(dsName);

        // Register store whose primaryDataSourceName matches the datasource
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, storeId, dsName, null, Collections.emptyList());
        registerStore(store);

        // DependencyGraph with NO mappings
        DependencyGraph graph = new DependencyGraph();

        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // Send entities
        List<SimpleUser> entities = List.of(
                createUser(1L, "Alice", 30),
                createUser(2L, "Bob", 25));
        BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event);

        // Phase 1: entities should be in DependencyGraph
        List<Object> graphEntities = graph.findAll(dsName);
        assertThat(graphEntities)
                .as("Phase 1: Entities should be upserted to DependencyGraph")
                .hasSize(2);

        // Phase 4: store should contain entities via primary datasource discovery
        List<SimpleUser> storeData = store.findAll();
        assertThat(storeData)
                .as("Phase 4: Store should contain entities via primary datasource discovery "
                        + "(no mappings, no Phase 2.5 needed)")
                .hasSize(2);

        // Verify entity data integrity
        assertThat(storeData.stream().map(SimpleUser::getName).toList())
                .as("Store entities should have correct data")
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    // ==================== Test 2: INITIALIZING Datasource Preservation ====================

    /**
     * INITIALIZING durumundaki datasource'dan event gönderildiğinde Phase 2-4 atlanması.
     *
     * <p>Scenario: Datasource is in INITIALIZING state. When entities arrive,
     * only Phase 1 (entity upsert) runs. Phases 2, 3, and 4 are skipped entirely.
     * Store should NOT be updated.</p>
     *
     * <p><b>Validates: Requirements 3.2, 3.6</b></p>
     */
    @Example
    @Label("INITIALIZING datasource: Phase 1 runs, Phase 2-4 skipped, store NOT updated — SHOULD PASS")
    void initializingDatasourceOnlyPhase1Runs() throws Exception {
        String dsName = "initializing-ds";
        String storeId = "initializing-store";

        // Register INITIALIZING datasource
        registerInitializingDatasource(dsName);

        // Register store
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, storeId, dsName, null, Collections.emptyList());
        registerStore(store);

        DependencyGraph graph = new DependencyGraph();
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // Send entities
        List<SimpleUser> entities = List.of(
                createUser(1L, "Alice", 30),
                createUser(2L, "Bob", 25),
                createUser(3L, "Charlie", 35));
        BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event);

        // Phase 1: entities SHOULD be in DependencyGraph (Phase 1 always runs)
        List<Object> graphEntities = graph.findAll(dsName);
        assertThat(graphEntities)
                .as("Phase 1: Entities should be upserted to DependencyGraph even during INITIALIZING")
                .hasSize(3);

        // Phase 4 SKIPPED: store should be EMPTY
        List<SimpleUser> storeData = store.findAll();
        assertThat(storeData)
                .as("Phase 4 skipped: Store should be empty because datasource is INITIALIZING — "
                        + "Phases 2-4 are skipped for INITIALIZING datasources")
                .isEmpty();
    }

    // ==================== Test 3: Dashboard Aggregation Preservation ====================

    /**
     * Dashboard-only mapping'i olan datasource'dan event gönderildiğinde
     * Phase 3 dashboard aggregation'ın mevcut şekilde çalışması.
     *
     * <p>Scenario: Datasource has only dashboard mappings (isForDashboard=true).
     * Phase 3 should compute dashboard aggregation correctly. No store mapping
     * application (Phase 2.5) should interfere.</p>
     *
     * <p><b>Validates: Requirements 3.3, 3.5</b></p>
     */
    @Example
    @Label("Dashboard-only mapping: Phase 3 aggregation works correctly, no store mapping interference — SHOULD PASS")
    void dashboardOnlyMappingAggregationWorksCorrectly() throws Exception {
        String dsName = "dashboard-ds";
        String dashboardId = "test-dashboard";
        String storeId = "dashboard-store";

        // Register READY datasource
        registerReadyDatasource(dsName);

        // Register dashboard
        Dashboard<DashboardData> dashboard = new Dashboard<>(
                dashboardId, "Test Dashboard", DashboardData.class);
        registerDashboard(dashboard, dashboardId);

        // Register store (for Phase 4 primary datasource discovery)
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, storeId, dsName, null, Collections.emptyList());
        registerStore(store);

        // Dashboard COUNT aggregation mapping (isForDashboard=true)
        PropertyMapping<DashboardData, Long> countMapping = PropertyMapping
                .<DashboardData, Long>builder()
                .consumerId(dashboardId)
                .isForDashboard(true)
                .datasourceName(dsName)
                .targetPath(List.of(TOTAL_COUNT_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(DASHBOARD_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        // Setup aggregation plan
        AggregationTask task = new AggregationTask(dsName, Collections.emptyList());
        task.addMapping(AggregationType.COUNT, countMapping);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(dashboardId);
        plan.addTask(task);

        Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
        dashboardPlans.put(dashboardId, plan);

        AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

        // DependencyGraph with dashboard mapping only
        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(countMapping);

        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // Send 3 entities
        List<SimpleUser> entities = List.of(
                createUser(1L, "Alice", 30),
                createUser(2L, "Bob", 25),
                createUser(3L, "Charlie", 35));
        BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event);

        // Phase 3: Dashboard should have correct COUNT
        assertThat(dashboard.getData())
                .as("Phase 3: Dashboard data should be initialized on-demand")
                .isNotNull();

        DashboardData data = dashboard.getData();
        assertThat(data.getTotalCount())
                .as("Phase 3: Dashboard COUNT aggregation should equal 3 — "
                        + "dashboard aggregation flow is unaffected by store mapping changes")
                .isEqualTo(3L);

        // Phase 4: Store should also have entities via primary datasource discovery
        List<SimpleUser> storeData = store.findAll();
        assertThat(storeData)
                .as("Phase 4: Store should contain entities")
                .hasSize(3);
    }

    // ==================== Test 4: Phase 1 Entity Upsert Preservation ====================

    /**
     * Phase 1 entity upsert ve index güncelleme işlemlerinin normal çalışması.
     *
     * <p>Scenario: Send entities, then send updated entities. Phase 1 should
     * correctly upsert entities (insert new, update existing) in DependencyGraph.</p>
     *
     * <p><b>Validates: Requirements 3.6</b></p>
     */
    @Example
    @Label("Phase 1: Entity upsert correctly inserts and updates entities in DependencyGraph — SHOULD PASS")
    void phase1EntityUpsertCorrectlyInsertsAndUpdates() throws Exception {
        String dsName = "upsert-ds";
        String storeId = "upsert-store";

        registerReadyDatasource(dsName);

        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, storeId, dsName, null, Collections.emptyList());
        registerStore(store);

        DependencyGraph graph = new DependencyGraph();
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // Batch 1: Insert 2 entities
        List<SimpleUser> batch1 = List.of(
                createUser(1L, "Alice", 30),
                createUser(2L, "Bob", 25));
        BatchSnapshotEvent<SimpleUser> event1 = new BatchSnapshotEvent<>(batch1, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event1);

        // Verify batch 1
        List<Object> graphEntities = graph.findAll(dsName);
        assertThat(graphEntities)
                .as("Phase 1 batch 1: 2 entities should be in DependencyGraph")
                .hasSize(2);

        SimpleUser alice = (SimpleUser) graph.findById(dsName, 1L);
        assertThat(alice).as("Phase 1: Alice should exist").isNotNull();
        assertThat(alice.getName()).as("Phase 1: Alice name").isEqualTo("Alice");
        assertThat(alice.getAge()).as("Phase 1: Alice age").isEqualTo(30);

        // Batch 2: Update Alice (age 30→31), add Charlie
        List<SimpleUser> batch2 = List.of(
                createUser(1L, "Alice", 31),  // update
                createUser(3L, "Charlie", 35)); // new
        BatchSnapshotEvent<SimpleUser> event2 = new BatchSnapshotEvent<>(batch2, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event2);

        // Verify batch 2
        graphEntities = graph.findAll(dsName);
        assertThat(graphEntities)
                .as("Phase 1 batch 2: 3 entities should be in DependencyGraph (2 original + 1 new)")
                .hasSize(3);

        SimpleUser updatedAlice = (SimpleUser) graph.findById(dsName, 1L);
        assertThat(updatedAlice.getAge())
                .as("Phase 1: Alice's age should be updated from 30 to 31")
                .isEqualTo(31);

        SimpleUser bob = (SimpleUser) graph.findById(dsName, 2L);
        assertThat(bob)
                .as("Phase 1: Bob should still exist after batch 2")
                .isNotNull();
        assertThat(bob.getName()).isEqualTo("Bob");

        SimpleUser charlie = (SimpleUser) graph.findById(dsName, 3L);
        assertThat(charlie)
                .as("Phase 1: Charlie should be added in batch 2")
                .isNotNull();

        // Phase 4: Store should reflect all 3 entities
        List<SimpleUser> storeData = store.findAll();
        assertThat(storeData)
                .as("Phase 4: Store should contain all 3 entities after batch 2")
                .hasSize(3);
    }

    // ==================== Test 5: Empty Affected Mapping Set Preservation ====================

    /**
     * Etkilenen mapping seti boş olduğunda ek işlem yapılmaması.
     *
     * <p>Scenario: DependencyGraph has NO mappings for the datasource.
     * Phase 2 should return empty affected mapping set. No Phase 2.5 processing.
     * Phase 4 should still propagate entities to store via primary datasource discovery.</p>
     *
     * <p>This is the "empty affected mapping set → no additional processing" preservation.</p>
     *
     * <p><b>Validates: Requirements 3.4, 3.5, 3.7</b></p>
     */
    @Example
    @Label("Empty affected mapping set: no additional processing, store updated via primary DS discovery — SHOULD PASS")
    void emptyAffectedMappingSetNoAdditionalProcessing() throws Exception {
        String dsName = "empty-mapping-ds";
        String otherDsName = "other-ds";
        String storeId = "empty-mapping-store";

        // Register READY datasources
        registerReadyDatasource(dsName);
        registerReadyDatasource(otherDsName);

        // Register store with primaryDataSourceName = dsName
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, storeId, dsName, null, Collections.emptyList());
        registerStore(store);

        // DependencyGraph has a mapping for OTHER datasource, not for dsName
        // This means collectAffectedMappings(dsName) returns empty set
        PropertyMapping<DashboardData, Long> unrelatedMapping = PropertyMapping
                .<DashboardData, Long>builder()
                .consumerId("unrelated-dashboard")
                .isForDashboard(true)
                .datasourceName(otherDsName)
                .targetPath(List.of(TOTAL_COUNT_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(DASHBOARD_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(unrelatedMapping);

        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // Send entities to dsName (which has no mappings)
        List<SimpleUser> entities = List.of(
                createUser(1L, "Alice", 30),
                createUser(2L, "Bob", 25));
        BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsName, event);

        // Phase 1: entities in DependencyGraph
        List<Object> graphEntities = graph.findAll(dsName);
        assertThat(graphEntities)
                .as("Phase 1: Entities should be in DependencyGraph")
                .hasSize(2);

        // Phase 4: store should have entities via primary datasource discovery
        // (no mapping-based propagation, only primary DS discovery)
        List<SimpleUser> storeData = store.findAll();
        assertThat(storeData)
                .as("Phase 4: Store should contain entities via primary datasource discovery — "
                        + "empty affected mapping set means no additional processing")
                .hasSize(2);

        // Verify store version was updated
        assertThat(store.getVersion())
                .as("Phase 4: Store version should be updated")
                .isEqualTo(1L);
    }
}
