package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
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
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fault Condition Exploration Property Test — removedEntities Ignored When changedEntities Empty.
 *
 * <p>This test proves that when {@code changedEntities} is empty but {@code removedEntities}
 * is non-empty, the fixed code does NOT skip Phase 3 aggregation updates.</p>
 *
 * <p>In the unfixed code, {@code applyPhase3AggregationUpdates()} had an early return:
 * {@code if (changedEntities.isEmpty()) { return; }} which ignored removedEntities entirely.
 * The fix changed this to: {@code if (changedEntities.isEmpty() && removedEntities.isEmpty()) { return; }}</p>
 *
 * <p><b>Validates: Requirements 2.3, 2.7</b></p>
 */
class RemovedEntitiesFaultConditionPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String DASHBOARD_ID = "test-dashboard";

    // ==================== Simple Dashboard Data Target Class ====================

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
        private final String name;

        TestEntity(int id, int value, String name) {
            this.id = id;
            this.value = value;
            this.name = name;
        }

        @Override
        public Integer getIdentity() { return id; }
        public int getValue() { return value; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id == that.id && value == that.value && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() { return Objects.hash(id, value, name); }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", value=" + value + ", name='" + name + "'}";
        }
    }

    // ==================== Reject-All Specification ====================

    /**
     * A Specification that rejects ALL entities. When used as a TimeWindowRule,
     * this causes all incoming entities to be filtered out (filteredEntities = empty),
     * making changedEntities empty. If those entities already exist in DependencyGraph,
     * they become removedEntities.
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

    // ==================== Property 2: changedEntities Empty + removedEntities Non-Empty → Phase 3 Runs ====================

    /**
     * Property 2: For any set of entities that already exist in DependencyGraph,
     * when a batch event arrives and a TimeWindowRule filters ALL of them out
     * (changedEntities = empty, removedEntities = non-empty), the fixed code
     * should still execute Phase 3 aggregation updates.
     *
     * <p>Bug condition: {@code changedEntities.isEmpty()} caused Phase 3 to skip
     * entirely, ignoring that removedEntities was non-empty.</p>
     *
     * <p>Fix: early return changed to {@code changedEntities.isEmpty() && removedEntities.isEmpty()}</p>
     *
     * <p><b>Validates: Requirements 2.3, 2.7</b></p>
     */
    @Property(tries = 50)
    void removedEntitiesNonEmptyChangedEntitiesEmptyPhase3StillExecutes(
            @ForAll("randomEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Dashboard with null data (so we can detect Phase 3 ran by checking data != null) ---
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
            // This ensures filteredEntities is empty → changedEntities is empty
            TimeWindowRule<TestEntity> rejectAllRule = new TimeWindowRule<>(
                    DS_NAME, RejectAllSpecification::new);
            factory.registerTimeWindowRule(DS_NAME, rejectAllRule);

            // --- Setup: AnalysisResult with DashboardAggregationPlan ---
            AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
            @SuppressWarnings("unchecked")
            PropertyMapping<TestDashboardData, ?> mockMapping = mock(PropertyMapping.class);
            task.addMapping(AggregationType.COUNT, mockMapping);

            DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_ID);
            plan.addTask(task);

            Map<String, DashboardAggregationPlan> dashboardPlans = new HashMap<>();
            dashboardPlans.put(DASHBOARD_ID, plan);

            AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

            // --- Setup: DependencyGraph pre-populated with entities ---
            // These entities must exist in DependencyGraph BEFORE the batch event arrives.
            // When TimeWindowRule rejects them, detectRemovedEntities() will find them
            // in DependencyGraph → removedEntities becomes non-empty.
            DependencyGraph graph = new DependencyGraph();
            for (TestEntity entity : entities) {
                graph.upsert(DS_NAME, entity);
            }

            // --- Setup: IncrementalSyncProcessor ---
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // --- Act: Process batch snapshot ---
            // All entities will be rejected by TimeWindowRule → filteredEntities = empty
            // But entities exist in DependencyGraph → removedEntities = non-empty
            // In unfixed code: changedEntities.isEmpty() → return (Phase 3 skipped)
            // In fixed code: changedEntities.isEmpty() && removedEntities.isEmpty() → false → Phase 3 runs
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // --- Assert: Phase 3 ran (dashboard data was initialized on-demand) ---
            // If Phase 3 was skipped (unfixed code), dashboard data remains null.
            // If Phase 3 ran (fixed code), on-demand initialization creates a TestDashboardData instance.
            assertThat(dashboard.getData())
                    .as("After fix: Phase 3 should run even when changedEntities is empty, "
                            + "because removedEntities is non-empty (%d entities). "
                            + "The unfixed code would skip Phase 3 due to changedEntities.isEmpty() check. "
                            + "Dashboard data should be initialized on-demand.",
                            entities.size())
                    .isNotNull();

            assertThat(dashboard.getData())
                    .as("On-demand initialized data should be an instance of TestDashboardData")
                    .isInstanceOf(TestDashboardData.class);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> randomEntities() {
        Arbitrary<TestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 1000),
                Arbitraries.integers().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        ).as(TestEntity::new);

        return entityArb.list().ofMinSize(1).ofMaxSize(20).uniqueElements(TestEntity::getIdentity);
    }
}
