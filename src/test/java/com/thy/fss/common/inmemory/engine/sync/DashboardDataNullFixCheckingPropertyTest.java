package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.LongAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fix Checking Property Test — Dashboard Data Null On-Demand Initialization with Correct Aggregate Values.
 *
 * <p>This test verifies that after the fix, when dashboard data is null and a streaming batch event
 * arrives, the on-demand initialization creates the data AND the aggregate computation produces
 * CORRECT results (COUNT matches entity count).</p>
 *
 * <p>Difference from exploration test (4.1): Task 4.1 only checked that dashboard data becomes non-null.
 * This test also verifies that the aggregate VALUES are correct.</p>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.5</b></p>
 */
class DashboardDataNullFixCheckingPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String DASHBOARD_ID = "test-dashboard";

    // ==================== Dashboard Data Target Class ====================

    /**
     * Dashboard data POJO with totalCount field.
     * On-demand initialization uses {@code targetClass.getDeclaredConstructor().newInstance()}.
     */
    public static class TestDashboardData {
        private long totalCount;

        public TestDashboardData() {
            this.totalCount = 0;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    }

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final int value;

        TestEntity(int id, int value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Integer getIdentity() { return id; }
        public int getValue() { return value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id == that.id && value == that.value;
        }

        @Override
        public int hashCode() { return Objects.hash(id, value); }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", value=" + value + "}";
        }
    }

    // ==================== MetaAttribute definitions ====================

    static final LongAttribute<TestDashboardData> TOTAL_COUNT_ATTR =
            new LongAttribute<>("totalCount", TestDashboardData.class);

    // ==================== SpecificationService for TestDashboardData ====================

    /**
     * Minimal SpecificationService that supports getValueByPath and setValueByPath
     * for TestDashboardData. Only implements the methods needed by the aggregation pipeline.
     */
    static class TestDashboardDataSpecService implements SpecificationService<TestDashboardData> {

        @Override
        public Class<TestDashboardData> getEntityClass() { return TestDashboardData.class; }

        @Override
        public TestDashboardData createInstance() throws Exception {
            return new TestDashboardData();
        }

        @Override
        public Object getFieldValue(TestDashboardData entity, String fieldName) {
            if ("totalCount".equals(fieldName)) return entity.getTotalCount();
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        @Override
        public Object getFieldValue(TestDashboardData entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(TestDashboardData entity, MetaAttribute<?, ?> attribute, Object value) {
            if ("totalCount".equals(attribute.getName())) {
                entity.setTotalCount(((Number) value).longValue());
            } else {
                throw new IllegalArgumentException("Unknown field: " + attribute.getName());
            }
        }

        @Override
        public Object getValueByPath(TestDashboardData entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException("Nested paths not supported in test");
        }

        @Override
        public void setValueByPath(TestDashboardData entity, List<MetaAttribute<?, ?>> path, Object value) {
            if (path.size() == 1) { setFieldValue(entity, path.get(0), value); return; }
            throw new UnsupportedOperationException("Nested paths not supported in test");
        }

        // --- Unused methods ---
        @Override public boolean validateSpecification(TestDashboardData e, MetaAttribute<TestDashboardData, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
        @Override public boolean validateFilter(TestDashboardData e, Object f) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestDashboardData> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestDashboardData> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestDashboardData> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestDashboardData> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<TestDashboardData> c, CollectionSelector s, Specification<TestDashboardData> sp) { throw new UnsupportedOperationException(); }
        @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public Object getValueByPathWithCollections(TestDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
        @Override public void setValueByPathWithCollections(TestDashboardData e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
    }

    // ==================== SpecificationService for TestEntity ====================

    /**
     * Minimal SpecificationService for TestEntity. Only implements getValueByPath
     * which is needed by computeAggregationsInSinglePass for field value extraction.
     */
    static class TestEntitySpecService implements SpecificationService<TestEntity> {

        @Override
        public Class<TestEntity> getEntityClass() { return TestEntity.class; }

        @Override
        public TestEntity createInstance() throws Exception { return new TestEntity(0, 0); }

        @Override
        public Object getFieldValue(TestEntity entity, String fieldName) {
            if ("value".equals(fieldName)) return entity.getValue();
            if ("id".equals(fieldName)) return entity.getIdentity();
            throw new IllegalArgumentException("Unknown field: " + fieldName);
        }

        @Override
        public Object getFieldValue(TestEntity entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(TestEntity entity, MetaAttribute<?, ?> attribute, Object value) {
            throw new UnsupportedOperationException("TestEntity is immutable");
        }

        @Override
        public Object getValueByPath(TestEntity entity, List<MetaAttribute<?, ?>> path) {
            if (path.size() == 1) return getFieldValue(entity, path.get(0));
            throw new UnsupportedOperationException("Nested paths not supported in test");
        }

        @Override
        public void setValueByPath(TestEntity entity, List<MetaAttribute<?, ?>> path, Object value) {
            throw new UnsupportedOperationException("TestEntity is immutable");
        }

        // --- Unused methods ---
        @Override public boolean validateSpecification(TestEntity e, MetaAttribute<TestEntity, ?> a, Operator op, Object v) { throw new UnsupportedOperationException(); }
        @Override public boolean validateFilter(TestEntity e, Object f) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestEntity> createComparator(String f, boolean a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestEntity> createComparator(MetaAttribute<?, ?> a, boolean asc) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestEntity> createMultiFieldComparator(List<String> f, List<Boolean> a) { throw new UnsupportedOperationException(); }
        @Override public Comparator<TestEntity> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> a, List<Boolean> asc) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<?> c, CollectionSelector s) { throw new UnsupportedOperationException(); }
        @Override public Object extractFromCollection(Collection<TestEntity> c, CollectionSelector s, Specification<TestEntity> sp) { throw new UnsupportedOperationException(); }
        @Override public int getCollectionSize(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean isCollectionEmpty(Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public Object getValueByPathWithCollections(TestEntity e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c) { throw new UnsupportedOperationException(); }
        @Override public void setValueByPathWithCollections(TestEntity e, List<MetaAttribute<?, ?>> p, List<CollectionOperationMetadata<?, ?>> c, Object v) { throw new UnsupportedOperationException(); }
    }

    // ==================== Singleton instances ====================

    private static final TestDashboardDataSpecService DASHBOARD_SPEC_SERVICE = new TestDashboardDataSpecService();
    private static final TestEntitySpecService ENTITY_SPEC_SERVICE = new TestEntitySpecService();

    // ==================== Property 1: Fix Checking — Correct Aggregate Values ====================

    /**
     * Property 1 (Fix Checking): For any streaming batch event where dashboard data is null,
     * datasource is READY, and dashboard has a COUNT aggregation plan, the fixed code should:
     * 1. Create dashboard data on-demand (not null after processing)
     * 2. Compute COUNT aggregation correctly (totalCount == number of entities)
     *
     * <p>This goes beyond the exploration test (4.1) which only checked non-null.
     * Here we verify the aggregate VALUE is mathematically correct.</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.5</b></p>
     */
    @Property(tries = 50)
    void dashboardDataNullAfterFixAggregateValuesAreCorrect(
            @ForAll("randomEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Dashboard with null data ---
            Dashboard<TestDashboardData> dashboard = new Dashboard<>(
                    DASHBOARD_ID, "Test Dashboard", TestDashboardData.class);

            // Precondition: dashboard data is null (the bug condition)
            assertThat(dashboard.getData())
                    .as("Precondition: dashboard data must be null before processing")
                    .isNull();

            // Register dashboard with factory
            Method registerMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                    "registerDashboard", Dashboard.class, String.class, List.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(factory, dashboard, DASHBOARD_ID, Collections.emptyList());

            // --- Setup: Streaming datasource in READY state ---
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            // --- Setup: COUNT aggregation with real PropertyMapping ---
            // Build a real PropertyMapping that targets TestDashboardData.totalCount
            PropertyMapping<TestDashboardData, Long> countMapping = PropertyMapping
                    .<TestDashboardData, Long>builder()
                    .consumerId(DASHBOARD_ID)
                    .isForDashboard(true)
                    .datasourceName(DS_NAME)
                    .targetPath(List.of(TOTAL_COUNT_ATTR))
                    .sourceService(ENTITY_SPEC_SERVICE)
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

            // --- Setup: DependencyGraph and IncrementalSyncProcessor ---
            DependencyGraph graph = new DependencyGraph();
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // --- Act: Process batch snapshot with dashboard data = null ---
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // --- Assert 1: Dashboard data should NOT be null (on-demand initialization) ---
            assertThat(dashboard.getData())
                    .as("After fix: dashboard data should be initialized on-demand")
                    .isNotNull()
                    .isInstanceOf(TestDashboardData.class);

            // --- Assert 2: COUNT aggregate value must equal the number of entities ---
            // All entities have unique IDs, so DependencyGraph stores all of them.
            TestDashboardData data = dashboard.getData();
            long expectedCount = entities.size();

            assertThat(data.getTotalCount())
                    .as("COUNT aggregation should equal entity count. "
                            + "Entities: %d, got: %d",
                            expectedCount, data.getTotalCount())
                    .isEqualTo(expectedCount);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> randomEntities() {
        // Use unique IDs (1..size) so DependencyGraph doesn't deduplicate
        return Arbitraries.integers().between(1, 20).flatMap(size -> {
            List<Arbitrary<TestEntity>> entityArbs = new java.util.ArrayList<>();
            for (int i = 1; i <= size; i++) {
                final int id = i;
                entityArbs.add(Arbitraries.integers().between(1, 10000)
                        .map(value -> new TestEntity(id, value)));
            }
            return Combinators.combine(entityArbs).as(list -> list);
        });
    }
}
