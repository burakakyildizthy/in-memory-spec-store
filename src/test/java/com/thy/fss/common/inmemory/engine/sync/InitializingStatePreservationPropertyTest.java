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
 * Preservation Property Test — INITIALIZING State Phase 2-3-4 Skip Behavior.
 *
 * <p>This test verifies that when a streaming datasource is in INITIALIZING state,
 * Phase 2-3-4 are skipped and only Phase 1 (entity upsert) runs. The streaming
 * pipeline fixes (on-demand dashboard data initialization, removedEntities check
 * expansion) must NOT change this behavior.</p>
 *
 * <p>Test approach:
 * <ol>
 *   <li>Register a streaming datasource in INITIALIZING state</li>
 *   <li>Set up dashboard with null data and aggregation plans</li>
 *   <li>Send batch event with entities</li>
 *   <li>Assert: entities ARE in DependencyGraph (Phase 1 ran)</li>
 *   <li>Assert: dashboard data is STILL null (Phase 3 was skipped, no on-demand init)</li>
 * </ol>
 *
 * <p><b>Validates: Requirements 3.2, 3.5</b></p>
 */
class InitializingStatePreservationPropertyTest {

    private static final String DS_NAME = "streaming-ds-initializing";
    private static final String DASHBOARD_ID = "test-dashboard";

    // ==================== Dashboard Data Target Class ====================

    /**
     * Simple POJO with no-arg constructor for dashboard data target.
     * In this test, dashboard data should remain null because Phase 3 is skipped
     * for INITIALIZING datasources — on-demand initialization must NOT apply.
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

    // ==================== Property 4: Preservation — INITIALIZING State Skip ====================

    /**
     * Property 4 (Preservation): For any streaming datasource in INITIALIZING state,
     * when a batch event arrives with entities:
     * <ul>
     *   <li>Phase 1 (entity upsert) MUST execute — entities stored in DependencyGraph</li>
     *   <li>Phase 2-3-4 MUST be skipped — no mapping, no aggregation, no consumer propagation</li>
     *   <li>Dashboard data MUST remain null — on-demand initialization must NOT apply</li>
     * </ul>
     *
     * <p>This ensures the streaming fixes do not accidentally trigger on-demand dashboard
     * data initialization for INITIALIZING datasources, which should only accumulate data
     * in DependencyGraph without any mapping/aggregation processing.</p>
     *
     * <p><b>Validates: Requirements 3.2, 3.5</b></p>
     */
    @Property(tries = 50)
    void initializingDatasourcePhase234SkippedEntitiesStoredButNoDashboardUpdate(
            @ForAll("randomEntities") List<TestEntity> entities) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Dashboard with null data and aggregation plans ---
            Dashboard<TestDashboardData> dashboard = new Dashboard<>(
                    DASHBOARD_ID, "Test Dashboard", TestDashboardData.class);

            // Precondition: dashboard data is null
            assertThat(dashboard.getData())
                    .as("Precondition: dashboard data must be null before processing")
                    .isNull();

            // Register dashboard with factory
            Method registerMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                    "registerDashboard", Dashboard.class, String.class, List.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(factory, dashboard, DASHBOARD_ID, Collections.emptyList());

            // --- Setup: Streaming datasource in INITIALIZING state ---
            @SuppressWarnings("unchecked")
            StreamingDataSource<TestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
            factory.registerDataSource(DS_NAME, streamingDs);

            // Verify: factory identifies this as a streaming datasource
            assertThat(factory.isStreamingDataSource(DS_NAME))
                    .as("Datasource must be identified as streaming")
                    .isTrue();

            // Verify: datasource is in INITIALIZING state
            assertThat(factory.getStreamingDataSource(DS_NAME).getState())
                    .as("Datasource must be in INITIALIZING state")
                    .isEqualTo(StreamingDataSourceState.INITIALIZING);

            // --- Setup: AnalysisResult with DashboardAggregationPlan ---
            // Even though aggregation plans exist, Phase 3 should NOT run for INITIALIZING datasources
            AggregationTask task = new AggregationTask(DS_NAME, Collections.emptyList());
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

            // --- Act: Process batch snapshot with datasource in INITIALIZING state ---
            BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // --- Assert 1: Entities ARE in DependencyGraph (Phase 1 ran) ---
            // Phase 1 always runs regardless of datasource state — data accumulation continues.
            List<TestEntity> storedEntities = graph.findAll(DS_NAME);
            assertThat(storedEntities)
                    .as("Phase 1 must execute: entities should be stored in DependencyGraph "
                            + "even when datasource is INITIALIZING. Sent %d entities.", entities.size())
                    .hasSize(entities.size());

            // --- Assert 2: Dashboard data is STILL null (Phase 3 was skipped) ---
            // The on-demand initialization fix must NOT apply to INITIALIZING datasources.
            // Phase 2-3-4 are skipped entirely when isDataSourceInitializing returns true.
            assertThat(dashboard.getData())
                    .as("Phase 3 must be skipped for INITIALIZING datasource: dashboard data "
                            + "should remain null. On-demand initialization must NOT trigger "
                            + "for INITIALIZING datasources. Entities: %d", entities.size())
                    .isNull();

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TestEntity>> randomEntities() {
        // Use unique IDs to avoid DependencyGraph deduplication
        return Arbitraries.integers().between(1, 20).flatMap(size -> {
            List<Arbitrary<TestEntity>> entityArbs = new java.util.ArrayList<>();
            for (int i = 1; i <= size; i++) {
                final int id = i;
                entityArbs.add(
                    Combinators.combine(
                        Arbitraries.integers().between(1, 10000),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                    ).as((value, name) -> new TestEntity(id, value, name))
                );
            }
            return Combinators.combine(entityArbs).as(list -> list);
        });
    }
}
