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
import com.thy.fss.common.inmemory.engine.analysis.AggregationTask;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.analysis.DashboardAggregationPlan;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fault Condition Exploration Property Test — Dashboard Data Null On-Demand Initialization.
 *
 * <p>This test proves that when dashboard data is null and a streaming batch event arrives
 * with the datasource in READY state and the dashboard has aggregation plans, the fixed code
 * creates dashboard data on-demand via {@code targetClass.getDeclaredConstructor().newInstance()}
 * and executes aggregation (does NOT skip it).</p>
 *
 * <p>In the unfixed code, {@code applyPhase3AggregationUpdates()} would log
 * "Dashboard '{}' has no data yet, skipping" and skip aggregation entirely.
 * The fix initializes dashboard data on-demand so aggregation proceeds.</p>
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.5</b></p>
 */
class DashboardDataNullFaultConditionPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String DASHBOARD_ID = "test-dashboard";

    // ==================== Simple Dashboard Data Target Class ====================

    /**
     * A simple POJO with a no-arg constructor that serves as the dashboard data target.
     * The on-demand initialization uses {@code targetClass.getDeclaredConstructor().newInstance()},
     * so this class must have a public no-arg constructor.
     */
    public static class TestDashboardData {
        private long totalCount;
        private double totalSum;

        public TestDashboardData() {
            this.totalCount = 0;
            this.totalSum = 0.0;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public double getTotalSum() { return totalSum; }
        public void setTotalSum(double totalSum) { this.totalSum = totalSum; }
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

    // ==================== Property 1: Dashboard Data Null → On-Demand Initialization ====================

    /**
     * Property 1: For any streaming batch event where dashboard data is null,
     * datasource is READY, and dashboard has aggregation plans, the fixed code
     * should create dashboard data on-demand and execute aggregation (not skip it).
     *
     * <p>Bug condition: {@code dashboard.getData() == null} caused Phase 3 to skip
     * aggregation entirely with "has no data yet, skipping" log.</p>
     *
     * <p>Fix: on-demand initialization via {@code targetClass.getDeclaredConstructor().newInstance()}</p>
     *
     * <p><b>Validates: Requirements 2.1, 2.2, 2.5</b></p>
     */
    @Property(tries = 50)
    void dashboardDataNullOnDemandInitializationAggregationExecuted(
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

            // Register dashboard with factory via reflection (registerDashboard is package-private)
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

            // --- Setup: AnalysisResult with DashboardAggregationPlan ---
            AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
            // Add a COUNT aggregation type (simplest — doesn't need field path extraction)
            // We use a mock PropertyMapping since we only need the task to reference our datasource
            @SuppressWarnings("unchecked")
            PropertyMapping<TestDashboardData, ?> mockMapping = mock(PropertyMapping.class);
            task.addMapping(AggregationType.COUNT, mockMapping);

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

            // --- Assert: Dashboard data should NOT be null after processing ---
            // In the unfixed code, this would remain null because Phase 3 skipped aggregation.
            // In the fixed code, on-demand initialization creates a new TestDashboardData instance.
            assertThat(dashboard.getData())
                    .as("After fix: dashboard data should be initialized on-demand when it was null. "
                            + "The unfixed code would skip aggregation with 'has no data yet, skipping' log. "
                            + "Entities processed: %d", entities.size())
                    .isNotNull();

            // Verify the data is of the correct type (created via targetClass reflection)
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

        return entityArb.list().ofMinSize(1).ofMaxSize(20);
    }
}
