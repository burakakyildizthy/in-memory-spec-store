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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
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
 * Integration test for the complete streaming pipeline flow.
 *
 * <p>Verifies the end-to-end path: streaming datasource READY → batch snapshot event →
 * Phase 1 (entity upsert) → Phase 2 (mapping updates) → Phase 3 (aggregation with
 * on-demand dashboard data initialization) → Phase 4 (consumer/store propagation) →
 * dashboard aggregate values correct and store data updated.</p>
 */
class StreamingPipelineIntegrationTest {

    private static final String DS_NAME = "integration-streaming-ds";
    private static final String DASHBOARD_ID = "integration-dashboard";
    private static final String STORE_ID = "integration-store";
    private static final String UNKNOWN_FIELD_PREFIX = "Unknown field: ";
    private static final String NESTED_PATHS_NOT_SUPPORTED = "Nested paths not supported";
    private static final String COUNT_FROM_A = "countFromA";
    private static final String COUNT_FROM_B = "countFromB";
    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String CHARLIE = "Charlie";
    private static final String EVE = "Eve";
    private static final String DIANA = "Diana";
    private static final String TOTAL_COUNT = "totalCount";
    private static final String INTEGRATION_DASHBOARD = "Integration Dashboard";

    private InMemorySpecStoreFactory factory;

    // ==================== Dashboard Data Target Class ====================

    public static class DashboardData {
        private long totalCount;

        public DashboardData() {
            this.totalCount = 0;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

        // ==================== Multi-Datasource Dashboard Data ====================

        public static class MultiDsDashboardData {
            private long countFromA;
            private long countFromB;

            public MultiDsDashboardData() {
                this.countFromA = 0;
                this.countFromB = 0;
            }

            public long getCountFromA() { return countFromA; }
            public void setCountFromA(long countFromA) { this.countFromA = countFromA; }
            public long getCountFromB() { return countFromB; }
            public void setCountFromB(long countFromB) { this.countFromB = countFromB; }
        }

        static final LongAttribute<MultiDsDashboardData> COUNT_FROM_A_ATTR =
                new LongAttribute<>(COUNT_FROM_A, MultiDsDashboardData.class);
        static final LongAttribute<MultiDsDashboardData> COUNT_FROM_B_ATTR =
                new LongAttribute<>(COUNT_FROM_B, MultiDsDashboardData.class);

        static class MultiDsSpecService implements SpecificationService<MultiDsDashboardData> {
            @Override public Class<MultiDsDashboardData> getEntityClass() { return MultiDsDashboardData.class; }
            @Override public MultiDsDashboardData createInstance() throws Exception { return new MultiDsDashboardData(); }

            @Override
            public Object getFieldValue(MultiDsDashboardData entity, String fieldName) {
                return switch (fieldName) {
                    case COUNT_FROM_A -> entity.getCountFromA();
                    case COUNT_FROM_B -> entity.getCountFromB();
                    default -> throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + fieldName);
                };
            }

            @Override
            public Object getFieldValue(MultiDsDashboardData entity, MetaAttribute<?, ?> attribute) {
                return getFieldValue(entity, attribute.getName());
            }

            @Override
            public void setFieldValue(MultiDsDashboardData entity, MetaAttribute<?, ?> attribute, Object value) {
                switch (attribute.getName()) {
                    case COUNT_FROM_A -> entity.setCountFromA(((Number) value).longValue());
                    case COUNT_FROM_B -> entity.setCountFromB(((Number) value).longValue());
                    default -> throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + attribute.getName());
                }
            }

            @Override
            public Object getValueByPath(MultiDsDashboardData entity, List<MetaAttribute<?, ?>> path) {
                if (path.size() == 1) return getFieldValue(entity, path.get(0));
                throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
            }

            @Override
            public void setValueByPath(MultiDsDashboardData entity, List<MetaAttribute<?, ?>> path, Object value) {
                if (path.size() == 1) { setFieldValue(entity, path.get(0), value); return; }
                throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
            }

            @Override public boolean validateSpecification(MultiDsDashboardData e, MetaAttribute<MultiDsDashboardData, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
            @Override public boolean validateFilter(MultiDsDashboardData e, Object f) { throw new UnsupportedOperationException(); }
            @Override public Comparator<MultiDsDashboardData> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
            @Override public Comparator<MultiDsDashboardData> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
            @Override public Comparator<MultiDsDashboardData> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
            @Override public Comparator<MultiDsDashboardData> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
            @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
            @Override public Object extractFromCollection(Collection<MultiDsDashboardData> c, CollectionSelector s, Specification<MultiDsDashboardData> sp) { throw new UnsupportedOperationException(); }
            @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
            @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
            @Override public Object getValueByPathWithCollections(MultiDsDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
            @Override public void setValueByPathWithCollections(MultiDsDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
        }

        private static final MultiDsSpecService MULTI_DS_SPEC_SERVICE = new MultiDsSpecService();
    }

    // ==================== MetaAttribute ====================

    static final LongAttribute<DashboardData> TOTAL_COUNT_ATTR =
            new LongAttribute<>(TOTAL_COUNT, DashboardData.class);

    // ==================== SpecificationService for DashboardData ====================

    static class DashboardDataSpecService implements SpecificationService<DashboardData> {
        @Override public Class<DashboardData> getEntityClass() { return DashboardData.class; }
        @Override public DashboardData createInstance() throws Exception { return new DashboardData(); }

        @Override
        public Object getFieldValue(DashboardData entity, String fieldName) {
            if (TOTAL_COUNT.equals(fieldName)) return entity.getTotalCount();
            throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + fieldName);
        }

        @Override
        public Object getFieldValue(DashboardData entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(DashboardData entity, MetaAttribute<?, ?> attribute, Object value) {
            if (TOTAL_COUNT.equals(attribute.getName())) {
                entity.setTotalCount(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + attribute.getName());
            }
        }

        @Override
        public Object getValueByPath(DashboardData entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
        }

        @Override
        public void setValueByPath(DashboardData entity, List<MetaAttribute<?, ?>> path, Object value) {
            if (path.size() == 1) { setFieldValue(entity, path.get(0), value); return; }
            throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
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

    // ==================== SpecificationService for SimpleUser (source entity) ====================

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
                default -> throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + fieldName);
            };
        }

        @Override
        public Object getFieldValue(SimpleUser entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(SimpleUser entity, MetaAttribute<?, ?> attribute, Object value) {
            throw new UnsupportedOperationException("Not needed for source entity in aggregation");
        }

        @Override
        public Object getValueByPath(SimpleUser entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
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

    // ==================== Multi-Datasource Dashboard Data ====================

    public static class MultiDsDashboardData {
        private long countFromA;
        private long countFromB;

        public MultiDsDashboardData() {
            this.countFromA = 0;
            this.countFromB = 0;
        }

        public long getCountFromA() { return countFromA; }
        public void setCountFromA(long countFromA) { this.countFromA = countFromA; }
        public long getCountFromB() { return countFromB; }
        public void setCountFromB(long countFromB) { this.countFromB = countFromB; }
    }

    static final LongAttribute<MultiDsDashboardData> COUNT_FROM_A_ATTR =
            new LongAttribute<>(COUNT_FROM_A, MultiDsDashboardData.class);
    static final LongAttribute<MultiDsDashboardData> COUNT_FROM_B_ATTR =
            new LongAttribute<>(COUNT_FROM_B, MultiDsDashboardData.class);

    static class MultiDsSpecService implements SpecificationService<MultiDsDashboardData> {
        @Override public Class<MultiDsDashboardData> getEntityClass() { return MultiDsDashboardData.class; }
        @Override public MultiDsDashboardData createInstance() throws Exception { return new MultiDsDashboardData(); }

        @Override
        public Object getFieldValue(MultiDsDashboardData entity, String fieldName) {
            return switch (fieldName) {
                case COUNT_FROM_A -> entity.getCountFromA();
                case COUNT_FROM_B -> entity.getCountFromB();
                default -> throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + fieldName);
            };
        }

        @Override
        public Object getFieldValue(MultiDsDashboardData entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(MultiDsDashboardData entity, MetaAttribute<?, ?> attribute, Object value) {
            switch (attribute.getName()) {
                case COUNT_FROM_A -> entity.setCountFromA(((Number) value).longValue());
                case COUNT_FROM_B -> entity.setCountFromB(((Number) value).longValue());
                default -> throw new IllegalArgumentException(UNKNOWN_FIELD_PREFIX + attribute.getName());
            }
        }

        @Override
        public Object getValueByPath(MultiDsDashboardData entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
        }

        @Override
        public void setValueByPath(MultiDsDashboardData entity, List<MetaAttribute<?, ?>> path, Object value) {
            if (path.size() == 1) { setFieldValue(entity, path.get(0), value); return; }
            throw new UnsupportedOperationException(NESTED_PATHS_NOT_SUPPORTED);
        }

        @Override public boolean validateSpecification(MultiDsDashboardData e, MetaAttribute<MultiDsDashboardData, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
        @Override public boolean validateFilter(MultiDsDashboardData e, Object f) { throw new UnsupportedOperationException(); }
        @Override public Comparator<MultiDsDashboardData> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<MultiDsDashboardData> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
        @Override public Comparator<MultiDsDashboardData> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<MultiDsDashboardData> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<MultiDsDashboardData> c, CollectionSelector s, Specification<MultiDsDashboardData> sp) { throw new UnsupportedOperationException(); }
        @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public Object getValueByPathWithCollections(MultiDsDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
        @Override public void setValueByPathWithCollections(MultiDsDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
    }

    private static final MultiDsSpecService MULTI_DS_SPEC_SERVICE = new MultiDsSpecService();

    // ==================== Helper: Create SimpleUser ====================

    private static SimpleUser createUser(long id, String name, int age) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setAge(age);
        user.setActive(true);
        return user;
    }

    // ==================== Setup / Teardown ====================

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
    }

    @AfterEach
    void tearDown() {
        factory.clearAll();
    }

    // ==================== Test: Full Streaming Pipeline ====================

    @Test
    @DisplayName("Full streaming pipeline: READY datasource → batch event → Phase 1-2-3-4 → dashboard aggregate updated and store propagated")
    void fullStreamingPipelineReadyDatasourceAllPhasesExecuteCorrectly() throws Exception {
        // --- Arrange: Streaming datasource in READY state ---
        @SuppressWarnings("unchecked")
        StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(DS_NAME);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(DS_NAME, streamingDs);

        // --- Arrange: Dashboard with null data (simulates streaming-only scenario) ---
        Dashboard<DashboardData> dashboard = new Dashboard<>(
                DASHBOARD_ID, INTEGRATION_DASHBOARD, DashboardData.class);
        assertThat(dashboard.getData()).as("Precondition: dashboard data is null").isNull();

        Method registerDashboard = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerDashboard", Dashboard.class, String.class, List.class);
        registerDashboard.setAccessible(true);
        registerDashboard.invoke(factory, dashboard, DASHBOARD_ID, Collections.emptyList());

        // --- Arrange: InMemoryDataStore as consumer (for Phase 4 verification) ---
        // Uses SimpleUser which has @MetaModel annotation and generated SpecificationService
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, STORE_ID, DS_NAME, null, Collections.emptyList());

        Method registerStore = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStore.setAccessible(true);
        registerStore.invoke(factory, store);

        // --- Arrange: COUNT aggregation mapping (dashboard ← datasource) ---
        PropertyMapping<DashboardData, Long> countMapping = PropertyMapping
                .<DashboardData, Long>builder()
                .consumerId(DASHBOARD_ID)
                .isForDashboard(true)
                .datasourceName(DS_NAME)
                .targetPath(List.of(TOTAL_COUNT_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(DASHBOARD_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
        task.addMapping(AggregationType.COUNT, countMapping);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_ID);
        plan.addTask(task);

        Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
        dashboardPlans.put(DASHBOARD_ID, plan);

        AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

        // --- Arrange: DependencyGraph with mapping registered (for consumer discovery) ---
        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(countMapping);

        // --- Arrange: Shared version counter and processor ---
        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // --- Act: Simulate listener path — incrementAndGet() then processBatchSnapshot() ---
        List<SimpleUser> entities = List.of(
                createUser(1L, ALICE, 30),
                createUser(2L, BOB, 25),
                createUser(3L, CHARLIE, 35)
        );
        BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

        streamingVersion.incrementAndGet(); // Listener increments first (fixed behavior)
        processor.processBatchSnapshot(DS_NAME, event);

        // === PHASE 1 VERIFICATION: Entities stored in DependencyGraph ===
        List<Object> storedEntities = graph.findAll(DS_NAME);
        assertThat(storedEntities)
                .as("Phase 1: All 3 entities should be stored in DependencyGraph")
                .hasSize(3);

        assertThat((Object) graph.findById(DS_NAME, 1L)).as("Phase 1: Entity id=1 exists").isNotNull();
        assertThat((Object) graph.findById(DS_NAME, 2L)).as("Phase 1: Entity id=2 exists").isNotNull();
        assertThat((Object) graph.findById(DS_NAME, 3L)).as("Phase 1: Entity id=3 exists").isNotNull();

        // === PHASE 3 VERIFICATION: Dashboard data initialized on-demand, aggregate correct ===
        assertThat(dashboard.getData())
                .as("Phase 3: Dashboard data should be initialized on-demand (was null before)")
                .isNotNull()
                .isInstanceOf(DashboardData.class);

        DashboardData data = dashboard.getData();
        assertThat(data.getTotalCount())
                .as("Phase 3: COUNT aggregation should equal 3 (number of entities)")
                .isEqualTo(3L);

        // === PHASE 4 VERIFICATION: Store updated with correct version and data ===
        assertThat(store.getVersion())
                .as("Phase 4: Store version should be 1 (the incremented streaming version)")
                .isEqualTo(1L);

        assertThat(store.findAll())
                .as("Phase 4: Store should contain all 3 entities from DependencyGraph")
                .hasSize(3);
    }

    @Test
    @DisplayName("Streaming pipeline with second batch: aggregate values accumulate correctly")
    void streamingPipelineSecondBatchAggregateUpdatesCorrectly() throws Exception {
        // --- Arrange ---
        @SuppressWarnings("unchecked")
        StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(DS_NAME);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(DS_NAME, streamingDs);

        Dashboard<DashboardData> dashboard = new Dashboard<>(
                DASHBOARD_ID, INTEGRATION_DASHBOARD, DashboardData.class);

        Method registerDashboard = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerDashboard", Dashboard.class, String.class, List.class);
        registerDashboard.setAccessible(true);
        registerDashboard.invoke(factory, dashboard, DASHBOARD_ID, Collections.emptyList());

        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, STORE_ID, DS_NAME, null, Collections.emptyList());

        Method registerStore = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStore.setAccessible(true);
        registerStore.invoke(factory, store);

        PropertyMapping<DashboardData, Long> countMapping = PropertyMapping
                .<DashboardData, Long>builder()
                .consumerId(DASHBOARD_ID)
                .isForDashboard(true)
                .datasourceName(DS_NAME)
                .targetPath(List.of(TOTAL_COUNT_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(DASHBOARD_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
        task.addMapping(AggregationType.COUNT, countMapping);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_ID);
        plan.addTask(task);

        Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
        dashboardPlans.put(DASHBOARD_ID, plan);

        AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(countMapping);

        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // --- Act: First batch — 2 entities ---
        List<SimpleUser> batch1 = List.of(createUser(1L, ALICE, 30), createUser(2L, BOB, 25));
        BatchSnapshotEvent<SimpleUser> event1 = new BatchSnapshotEvent<>(batch1, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(DS_NAME, event1);

        assertThat(dashboard.getData()).isNotNull();
        assertThat(dashboard.getData().getTotalCount())
                .as("After batch 1: COUNT should be 2")
                .isEqualTo(2L);
        assertThat(store.getVersion()).isEqualTo(1L);
        assertThat(store.findAll()).hasSize(2);

        // --- Act: Second batch — 3 new entities (IDs 3,4,5) ---
        List<SimpleUser> batch2 = List.of(
                createUser(3L, CHARLIE, 35),
                createUser(4L, DIANA, 28),
                createUser(5L, EVE, 40));
        BatchSnapshotEvent<SimpleUser> event2 = new BatchSnapshotEvent<>(batch2, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(DS_NAME, event2);

        // DependencyGraph now has 5 entities total (2 from batch1 + 3 from batch2)
        assertThat(graph.findAll(DS_NAME)).hasSize(5);

        assertThat(dashboard.getData().getTotalCount())
                .as("After batch 2: COUNT should be 5 (all entities in DependencyGraph)")
                .isEqualTo(5L);

        assertThat(store.getVersion())
                .as("Store version should be 2 after second batch")
                .isEqualTo(2L);

        assertThat(store.findAll())
                .as("Store should contain all 5 entities")
                .hasSize(5);
    }

    // ==================== Test: Multiple Streaming Datasources ====================

    @Test
    @DisplayName("Multiple streaming datasources: DS-A and DS-B become READY sequentially → each independently contributes to dashboard aggregation")
    void multipleStreamingDatasourcesSequentialReadyBothContributeToDashboardAggregate() throws Exception {
        String dsNameA = "streaming-ds-A";
        String dsNameB = "streaming-ds-B";
        String multiDashboardId = "multi-ds-dashboard";

        // --- Arrange: Two streaming datasources in READY state ---
        @SuppressWarnings("unchecked")
        StreamingDataSource<SimpleUser> streamingDsA = mock(StreamingDataSource.class);
        when(streamingDsA.getName()).thenReturn(dsNameA);
        when(streamingDsA.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(dsNameA, streamingDsA);

        @SuppressWarnings("unchecked")
        StreamingDataSource<SimpleUser> streamingDsB = mock(StreamingDataSource.class);
        when(streamingDsB.getName()).thenReturn(dsNameB);
        when(streamingDsB.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(dsNameB, streamingDsB);

        // --- Arrange: Dashboard with two count fields (one per datasource) ---
        Dashboard<MultiDsDashboardData> dashboard = new Dashboard<>(
                multiDashboardId, "Multi-DS Dashboard", MultiDsDashboardData.class);
        assertThat(dashboard.getData()).as("Precondition: dashboard data is null").isNull();

        Method registerDashboard = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerDashboard", Dashboard.class, String.class, List.class);
        registerDashboard.setAccessible(true);
        registerDashboard.invoke(factory, dashboard, multiDashboardId, Collections.emptyList());

        // --- Arrange: COUNT mapping for DS-A → countFromA ---
        PropertyMapping<MultiDsDashboardData, Long> countMappingA = PropertyMapping
                .<MultiDsDashboardData, Long>builder()
                .consumerId(multiDashboardId)
                .isForDashboard(true)
                .datasourceName(dsNameA)
                .targetPath(List.of(COUNT_FROM_A_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(MULTI_DS_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        // --- Arrange: COUNT mapping for DS-B → countFromB ---
        PropertyMapping<MultiDsDashboardData, Long> countMappingB = PropertyMapping
                .<MultiDsDashboardData, Long>builder()
                .consumerId(multiDashboardId)
                .isForDashboard(true)
                .datasourceName(dsNameB)
                .targetPath(List.of(COUNT_FROM_B_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(MULTI_DS_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        // --- Arrange: Two aggregation tasks in one plan (one per datasource) ---
        AggregationTask taskA = new AggregationTask(dsNameA, Collections.emptyList());
        taskA.addMapping(AggregationType.COUNT, countMappingA);

        AggregationTask taskB = new AggregationTask(dsNameB, Collections.emptyList());
        taskB.addMapping(AggregationType.COUNT, countMappingB);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(multiDashboardId);
        plan.addTask(taskA);
        plan.addTask(taskB);

        Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
        dashboardPlans.put(multiDashboardId, plan);

        AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

        // --- Arrange: DependencyGraph with both mappings ---
        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(countMappingA);
        graph.addMapping(countMappingB);

        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // === ACT 1: DS-A becomes READY and sends 3 entities ===
        List<SimpleUser> entitiesA = List.of(
                createUser(1L, ALICE, 30),
                createUser(2L, BOB, 25),
                createUser(3L, CHARLIE, 35));
        BatchSnapshotEvent<SimpleUser> eventA = new BatchSnapshotEvent<>(entitiesA, Instant.now());

        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsNameA, eventA);

        // --- Verify after DS-A: dashboard data initialized, countFromA = 3, countFromB = 0 ---
        assertThat(dashboard.getData())
                .as("After DS-A: Dashboard data should be initialized on-demand")
                .isNotNull()
                .isInstanceOf(MultiDsDashboardData.class);

        MultiDsDashboardData dataAfterA = dashboard.getData();
        assertThat(dataAfterA.getCountFromA())
                .as("After DS-A: countFromA should be 3 (DS-A's 3 entities)")
                .isEqualTo(3L);
        assertThat(dataAfterA.getCountFromB())
                .as("After DS-A: countFromB should still be 0 (DS-B hasn't sent data yet)")
                .isEqualTo(0L);

        assertThat(graph.findAll(dsNameA))
                .as("After DS-A: DependencyGraph should have 3 entities for DS-A")
                .hasSize(3);

        // === ACT 2: DS-B becomes READY and sends 2 entities ===
        List<SimpleUser> entitiesB = List.of(
                createUser(10L, DIANA, 28),
                createUser(11L, EVE, 40));
        BatchSnapshotEvent<SimpleUser> eventB = new BatchSnapshotEvent<>(entitiesB, Instant.now());

        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(dsNameB, eventB);

        // --- Verify after DS-B: countFromA preserved, countFromB = 2, total = 5 ---
        MultiDsDashboardData dataAfterB = dashboard.getData();
        assertThat(dataAfterB.getCountFromA())
                .as("After DS-B: countFromA should still be 3 (DS-A's contribution preserved)")
                .isEqualTo(3L);
        assertThat(dataAfterB.getCountFromB())
                .as("After DS-B: countFromB should be 2 (DS-B's 2 entities)")
                .isEqualTo(2L);

        long totalCount = dataAfterB.getCountFromA() + dataAfterB.getCountFromB();
        assertThat(totalCount)
                .as("After both datasources: total COUNT should be 5 (3 from DS-A + 2 from DS-B)")
                .isEqualTo(5L);

        assertThat(graph.findAll(dsNameB))
                .as("After DS-B: DependencyGraph should have 2 entities for DS-B")
                .hasSize(2);

        // --- Verify datasource independence: DS-A entities untouched by DS-B's batch ---
        assertThat(graph.findAll(dsNameA))
                .as("DS-A entities should remain unchanged after DS-B's batch")
                .hasSize(3);
    }

    // ==================== Test: TimeWindowRule Entity Filtering → removedEntities → Dashboard Aggregate Update ====================

    @Test
    @DisplayName("TimeWindowRule filtering: entities rejected by rule become removedEntities → dashboard COUNT decreases")
    void timeWindowRuleFilteringRemovedEntitiesDashboardAggregateUpdated() throws Exception {
        // --- Arrange: Streaming datasource in READY state ---
        @SuppressWarnings("unchecked")
        StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(DS_NAME);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(DS_NAME, streamingDs);

        // --- Arrange: Dashboard with null data ---
        Dashboard<DashboardData> dashboard = new Dashboard<>(
                DASHBOARD_ID, INTEGRATION_DASHBOARD, DashboardData.class);
        assertThat(dashboard.getData()).as("Precondition: dashboard data is null").isNull();

        Method registerDashboard = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerDashboard", Dashboard.class, String.class, List.class);
        registerDashboard.setAccessible(true);
        registerDashboard.invoke(factory, dashboard, DASHBOARD_ID, Collections.emptyList());

        // --- Arrange: InMemoryDataStore as consumer (for Phase 4) ---
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, STORE_ID, DS_NAME, null, Collections.emptyList());

        Method registerStore = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStore.setAccessible(true);
        registerStore.invoke(factory, store);

        // --- Arrange: COUNT aggregation mapping ---
        PropertyMapping<DashboardData, Long> countMapping = PropertyMapping
                .<DashboardData, Long>builder()
                .consumerId(DASHBOARD_ID)
                .isForDashboard(true)
                .datasourceName(DS_NAME)
                .targetPath(List.of(TOTAL_COUNT_ATTR))
                .sourceService(SOURCE_SPEC_SERVICE)
                .targetService(DASHBOARD_SPEC_SERVICE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
        task.addMapping(AggregationType.COUNT, countMapping);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_ID);
        plan.addTask(task);

        Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
        dashboardPlans.put(DASHBOARD_ID, plan);

        AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(countMapping);

        AtomicLong streamingVersion = new AtomicLong(0);
        IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                factory, graph, analysisResult, streamingVersion);

        // === ACT 1: First batch — 5 entities, no TimeWindowRule yet → all accepted, COUNT = 5 ===
        List<SimpleUser> batch1 = List.of(
                createUser(1L, ALICE, 30),
                createUser(2L, BOB, 25),
                createUser(3L, CHARLIE, 35),
                createUser(4L, DIANA, 28),
                createUser(5L, EVE, 40));
        BatchSnapshotEvent<SimpleUser> event1 = new BatchSnapshotEvent<>(batch1, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(DS_NAME, event1);

        assertThat(dashboard.getData()).as("After batch 1: dashboard data initialized").isNotNull();
        assertThat(dashboard.getData().getTotalCount())
                .as("After batch 1: COUNT should be 5 (all entities accepted)")
                .isEqualTo(5L);
        assertThat(graph.findAll(DS_NAME)).as("After batch 1: DependencyGraph has 5 entities").hasSize(5);

        // === ARRANGE 2: Register TimeWindowRule that rejects entities with id > 3 ===
        // Specification: entity passes only if id <= 3
        Specification<SimpleUser> rejectHighIdSpec = new Specification<>() {
            @Override
            public java.util.function.Predicate<SimpleUser> toPredicate() {
                return entity -> entity.getIdentity() <= 3L;
            }

            @Override
            public boolean test(SimpleUser entity) {
                return entity.getIdentity() <= 3L;
            }
        };
        TimeWindowRule<SimpleUser> timeWindowRule = new TimeWindowRule<>(DS_NAME, () -> rejectHighIdSpec);
        factory.registerTimeWindowRule(DS_NAME, timeWindowRule);

        // === ACT 2: Second batch — same 5 entities, but TimeWindowRule rejects id=4 and id=5 ===
        // filteredEntities = [1,2,3] (passed), entities 4,5 rejected
        // detectRemovedEntities finds id=4 and id=5 in DependencyGraph → removedEntities = [4,5]
        // Phase 1: upserts [1,2,3], removes [4,5] from DependencyGraph
        // Phase 3: re-aggregates → COUNT = 3
        List<SimpleUser> batch2 = List.of(
                createUser(1L, ALICE, 30),
                createUser(2L, BOB, 25),
                createUser(3L, CHARLIE, 35),
                createUser(4L, DIANA, 28),
                createUser(5L, EVE, 40));
        BatchSnapshotEvent<SimpleUser> event2 = new BatchSnapshotEvent<>(batch2, Instant.now());
        streamingVersion.incrementAndGet();
        processor.processBatchSnapshot(DS_NAME, event2);

        // === VERIFY: DependencyGraph only has 3 entities (id=4,5 removed) ===
        assertThat(graph.findAll(DS_NAME))
                .as("After batch 2: DependencyGraph should have 3 entities (id=4,5 removed by TimeWindowRule)")
                .hasSize(3);
        assertThat((Object) graph.findById(DS_NAME, 1L)).as("Entity id=1 still exists").isNotNull();
        assertThat((Object) graph.findById(DS_NAME, 2L)).as("Entity id=2 still exists").isNotNull();
        assertThat((Object) graph.findById(DS_NAME, 3L)).as("Entity id=3 still exists").isNotNull();
        assertThat((Object) graph.findById(DS_NAME, 4L)).as("Entity id=4 removed").isNull();
        assertThat((Object) graph.findById(DS_NAME, 5L)).as("Entity id=5 removed").isNull();

        // === VERIFY: Dashboard aggregate updated — COUNT decreased from 5 to 3 ===
        assertThat(dashboard.getData().getTotalCount())
                .as("After batch 2: COUNT should be 3 (entities 4,5 removed by TimeWindowRule)")
                .isEqualTo(3L);

        // === VERIFY: Store updated with correct version ===
        assertThat(store.getVersion())
                .as("Store version should be 2 after second batch")
                .isEqualTo(2L);

        assertThat(store.findAll())
                .as("Store should contain only 3 entities after TimeWindowRule filtering")
                .hasSize(3);
    }
}
