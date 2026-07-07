package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
 * Preservation Property Test — Phase 3 Exception Handling Fault Tolerance.
 *
 * <p>This test verifies that Phase 3's exception handling behavior is preserved after
 * the streaming pipeline fixes. When an aggregation task throws an exception for one
 * dashboard, the system should catch it, log it, and continue processing other dashboards.</p>
 *
 * <p>Test approach:
 * <ol>
 *   <li>Register two dashboards — one with a targetClass that has NO no-arg constructor
 *       (will cause ReflectiveOperationException during on-demand init), one with a valid class</li>
 *   <li>Both dashboards have null data (streaming scenario) and aggregation plans</li>
 *   <li>Send a batch event with entities</li>
 *   <li>Assert: processBatchSnapshot does NOT throw an exception</li>
 *   <li>Assert: the valid dashboard's data is initialized and aggregation ran</li>
 *   <li>Assert: the failing dashboard's data remains null (init failed, was caught)</li>
 * </ol>
 *
 * <p><b>Validates: Requirements 3.4</b></p>
 */
class Phase3ExceptionHandlingPreservationPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String GOOD_DASHBOARD_ID = "good-dashboard";
    private static final String FAILING_DASHBOARD_ID = "failing-dashboard";

    // ==================== Dashboard Data Target Classes ====================

    /**
     * A valid POJO with a no-arg constructor — on-demand initialization will succeed.
     */
    public static class GoodDashboardData {
        private long totalCount;

        public GoodDashboardData() {
            this.totalCount = 0;
        }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    }

    /**
     * A POJO WITHOUT a no-arg constructor — on-demand initialization via
     * {@code targetClass.getDeclaredConstructor().newInstance()} will throw
     * {@link NoSuchMethodException} (a {@link ReflectiveOperationException}).
     * This simulates a dashboard whose aggregation setup causes an exception.
     * The Phase 3 try-catch should catch this and continue with other dashboards.
     */
    public static class FailingDashboardData {
        private final String requiredParam;

        // Only constructor requires a parameter — no no-arg constructor exists
        public FailingDashboardData(String requiredParam) {
            this.requiredParam = requiredParam;
        }

        public String getRequiredParam() { return requiredParam; }
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

    // ==================== Property 4: Preservation — Exception Handling Fault Tolerance ====================

    /**
     * Property 4 (Preservation): When Phase 3 processes multiple dashboards and one
     * dashboard's on-demand initialization throws a ReflectiveOperationException:
     * <ul>
     *   <li>The exception is caught and logged (not propagated)</li>
     *   <li>Other dashboards are still processed successfully</li>
     *   <li>processBatchSnapshot does NOT throw an exception</li>
     * </ul>
     *
     * <p>This ensures the streaming fixes preserve the existing fault tolerance behavior
     * in Phase 3 — one failing dashboard must not break the entire pipeline.</p>
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 50)
    void phase3ExceptionHandlingOneDashboardFailsOtherDashboardsStillProcessed(
            @ForAll("randomEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Two dashboards, both with null data ---

            // Dashboard 1: FAILING — targetClass has no no-arg constructor
            // On-demand init will throw NoSuchMethodException → caught by Phase 3 try-catch
            Dashboard<FailingDashboardData> failingDashboard = new Dashboard<>(
                    FAILING_DASHBOARD_ID, "Failing Dashboard", FailingDashboardData.class);

            // Dashboard 2: GOOD — targetClass has a no-arg constructor
            // On-demand init will succeed and aggregation will run
            Dashboard<GoodDashboardData> goodDashboard = new Dashboard<>(
                    GOOD_DASHBOARD_ID, "Good Dashboard", GoodDashboardData.class);

            // Precondition: both dashboards have null data
            assertThat(failingDashboard.getData())
                    .as("Precondition: failing dashboard data must be null")
                    .isNull();
            assertThat(goodDashboard.getData())
                    .as("Precondition: good dashboard data must be null")
                    .isNull();

            // Register both dashboards with factory
            Method registerMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                    "registerDashboard", Dashboard.class, String.class, List.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(factory, failingDashboard, FAILING_DASHBOARD_ID, Collections.emptyList());
            registerMethod.invoke(factory, goodDashboard, GOOD_DASHBOARD_ID, Collections.emptyList());

            // --- Setup: Streaming datasource in READY state ---
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            // --- Setup: Aggregation plans for BOTH dashboards referencing the same datasource ---
            // Failing dashboard plan
            AggregationTask failingTask = new AggregationTask(DS_NAME, Collections.emptyList());
            @SuppressWarnings("unchecked")
            PropertyMapping<FailingDashboardData, ?> failingMapping = mock(PropertyMapping.class);
            failingTask.addMapping(AggregationType.COUNT, failingMapping);

            DashboardAggregationPlan failingPlan = new DashboardAggregationPlan(FAILING_DASHBOARD_ID);
            failingPlan.addTask(failingTask);

            // Good dashboard plan
            AggregationTask goodTask = new AggregationTask(DS_NAME, Collections.emptyList());
            @SuppressWarnings("unchecked")
            PropertyMapping<GoodDashboardData, ?> goodMapping = mock(PropertyMapping.class);
            goodTask.addMapping(AggregationType.COUNT, goodMapping);

            DashboardAggregationPlan goodPlan = new DashboardAggregationPlan(GOOD_DASHBOARD_ID);
            goodPlan.addTask(goodTask);

            // Register both plans — failing dashboard FIRST to ensure it's processed before good one
            Map<String, DashboardAggregationPlan> dashboardPlans = new java.util.LinkedHashMap<>();
            dashboardPlans.put(FAILING_DASHBOARD_ID, failingPlan);
            dashboardPlans.put(GOOD_DASHBOARD_ID, goodPlan);

            AnalysisResult analysisResult = new AnalysisResult(null, dashboardPlans, null);

            // --- Setup: DependencyGraph and IncrementalSyncProcessor ---
            DependencyGraph graph = new DependencyGraph();
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // --- Act: Process batch snapshot — should NOT throw despite failing dashboard ---
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());

            assertThatCode(() -> processor.processBatchSnapshot(DS_NAME, event))
                    .as("processBatchSnapshot must NOT throw even when one dashboard's "
                            + "aggregation fails. Fault tolerance must be preserved. "
                            + "Entities: %d", entities.size())
                    .doesNotThrowAnyException();

            // --- Assert 1: Failing dashboard's data remains null (init failed, was caught) ---
            assertThat(failingDashboard.getData())
                    .as("Failing dashboard data should remain null — on-demand init threw "
                            + "NoSuchMethodException which was caught by Phase 3 try-catch")
                    .isNull();

            // --- Assert 2: Good dashboard's data was initialized on-demand and is NOT null ---
            assertThat(goodDashboard.getData())
                    .as("Good dashboard data should be initialized on-demand despite the "
                            + "other dashboard failing. Fault tolerance means one failure "
                            + "doesn't block others. Entities: %d", entities.size())
                    .isNotNull();

            // --- Assert 3: Good dashboard data is the correct type ---
            assertThat(goodDashboard.getData())
                    .as("Good dashboard data should be an instance of GoodDashboardData")
                    .isInstanceOf(GoodDashboardData.class);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> randomEntities() {
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
