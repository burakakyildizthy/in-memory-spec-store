package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
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
 * Fix Checking Property Test — removedEntities Aggregate Values After Removal.
 *
 * <p>This test verifies that after the fix, when {@code changedEntities} is empty but
 * {@code removedEntities} is non-empty, Phase 3 runs AND the aggregate VALUES are
 * correctly updated to reflect the entity removals.</p>
 *
 * <p>Difference from exploration test (4.2): Task 4.2 only checked that Phase 3 ran
 * (dashboard data became non-null). This test verifies that the COUNT aggregate
 * decreases to 0 after all entities are removed via TimeWindowRule rejection.</p>
 *
 * <p><b>Validates: Requirements 2.3, 2.7</b></p>
 */
class RemovedEntitiesFixCheckingPropertyTest {

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

    // ==================== Reject-All Specification ====================

    /**
     * A Specification that rejects ALL entities. When used as a TimeWindowRule,
     * this causes all incoming entities to be filtered out (filteredEntities = empty),
     * making changedEntities empty while removedEntities becomes non-empty.
     */
    static class RejectAllSpecification implements Specification<TestEntity> {
        @Override
        public java.util.function.Predicate<TestEntity> toPredicate() {
            return entity -> false;
        }

        @Override
        public boolean test(TestEntity entity) {
            return false;
        }
    }

    // ==================== SpecificationService for TestDashboardData ====================

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

    // ==================== Property 2: Fix Checking — Correct Aggregate Values After Removal ====================

    /**
     * Property 2 (Fix Checking): For any set of entities pre-populated in DependencyGraph,
     * when a batch event arrives and a TimeWindowRule rejects ALL entities
     * (changedEntities = empty, removedEntities = non-empty), the fixed code should:
     * 1. Execute Phase 3 (not skip due to changedEntities.isEmpty())
     * 2. Initialize dashboard data on-demand (since it starts null)
     * 3. Compute COUNT aggregation correctly: totalCount == 0 (all entities removed)
     *
     * <p>This goes beyond the exploration test (4.2) which only checked that Phase 3 ran.
     * Here we verify the aggregate VALUE reflects the removal — COUNT must be 0 because
     * all entities were removed from DependencyGraph by removeExpiredEntities().</p>
     *
     * <p><b>Validates: Requirements 2.3, 2.7</b></p>
     */
    @Property(tries = 50)
    void removedEntitiesNonEmptyAfterFixAggregateValuesReflectRemoval(
            @ForAll("randomEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Dashboard with null data ---
            Dashboard<TestDashboardData> dashboard = new Dashboard<>(
                    DASHBOARD_ID, "Test Dashboard", TestDashboardData.class);

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

            // --- Setup: TimeWindowRule that rejects ALL entities ---
            TimeWindowRule<TestEntity> rejectAllRule = new TimeWindowRule<>(
                    DS_NAME, RejectAllSpecification::new);
            factory.registerTimeWindowRule(DS_NAME, rejectAllRule);

            // --- Setup: COUNT aggregation with real PropertyMapping ---
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

            // --- Setup: DependencyGraph pre-populated with entities ---
            // These entities exist BEFORE the batch event arrives.
            // When TimeWindowRule rejects them all, detectRemovedEntities() finds them
            // in DependencyGraph → removedEntities becomes non-empty.
            DependencyGraph graph = new DependencyGraph();
            for (TestEntity entity : entities) {
                graph.upsert(DS_NAME, entity);
            }

            // Verify precondition: entities are in DependencyGraph
            int initialCount = graph.findAll(DS_NAME).size();
            assertThat(initialCount)
                    .as("Precondition: DependencyGraph should contain %d entities", entities.size())
                    .isEqualTo(entities.size());

            // --- Setup: IncrementalSyncProcessor ---
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // --- Act: Process batch snapshot ---
            // All entities rejected by TimeWindowRule → filteredEntities = empty
            // Entities exist in DependencyGraph → removedEntities = non-empty
            // removeExpiredEntities() removes them from DependencyGraph
            // Fixed code: Phase 3 runs because removedEntities is non-empty
            // Aggregation full-scan on DependencyGraph finds 0 entities → COUNT = 0
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // --- Assert 1: Dashboard data should be initialized on-demand ---
            assertThat(dashboard.getData())
                    .as("Phase 3 should run and initialize dashboard data on-demand")
                    .isNotNull()
                    .isInstanceOf(TestDashboardData.class);

            // --- Assert 2: All entities removed from DependencyGraph ---
            int remainingEntities = graph.findAll(DS_NAME).size();
            assertThat(remainingEntities)
                    .as("All entities should be removed from DependencyGraph after TimeWindowRule rejection")
                    .isEqualTo(0);

            // --- Assert 3: COUNT aggregate must be 0 (all entities removed) ---
            // This is the KEY assertion that differentiates this test from 4.2.
            // The aggregate value must reflect that zero entities remain after removal.
            TestDashboardData data = dashboard.getData();
            assertThat(data.getTotalCount())
                    .as("COUNT aggregation should be 0 after all %d entities were removed. "
                            + "DependencyGraph has %d entities remaining.",
                            entities.size(), remainingEntities)
                    .isEqualTo(0L);

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
