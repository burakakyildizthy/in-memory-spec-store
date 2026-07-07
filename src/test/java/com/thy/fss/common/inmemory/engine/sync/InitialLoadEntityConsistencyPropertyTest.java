package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

// Feature: streaming-datasource-support, Property 2: Initial Load Entity Yükleme Tutarlılığı

/**
 * Property-based tests for initial data load entity consistency in
 * {@link IncrementalSyncProcessor}.
 *
 * <p>When a {@link BatchSnapshotEvent} with {@code initialLoad=true} is processed,
 * ALL entities (that pass TimeWindowRule filtering) must be loaded into
 * {@link DependencyGraph}. After processing, the graph must contain exactly
 * the entities from the initial load event, each findable by its ID.
 * The initial load callback must be invoked (state transition to READY).</p>
 *
 * <p><b>Validates: Requirements 2.3</b></p>
 */
class InitialLoadEntityConsistencyPropertyTest {

    private static final String DS_NAME = "initial-load-test-ds";

    // ==================== Property 2a: Initial load without TimeWindowRule ====================

    /**
     * Property 2a: For any BatchSnapshotEvent processed through
     * IncrementalSyncProcessor WITHOUT a TimeWindowRule, ALL entities must be
     * loaded into DependencyGraph. Entity count must match unique ID count,
     * and every entity must be findable by its ID.
     *
     * <p>In the unified architecture, initial state is loaded via fetchAll() by the engine,
     * which transitions the datasource to READY before events are processed through the pipeline.</p>
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void initialLoadWithoutTimeWindowRuleLoadsAllEntities(
            @ForAll("entityBatches") List<InitialLoadEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource WITHOUT TimeWindowRule
            // State is READY — in the unified architecture, initial load via fetchAll()
            // transitions the datasource to READY before events are processed.
            @SuppressWarnings("unchecked")
            StreamingDataSource<InitialLoadEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);

            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Compute expected unique IDs (last-write-wins for duplicates)
            Set<Integer> expectedIds = new HashSet<>();
            for (InitialLoadEntity entity : entities) {
                expectedIds.add(entity.getIdentity());
            }

            // Process event
            BatchSnapshotEvent<InitialLoadEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // PROPERTY: All entities must be in DependencyGraph
            List<InitialLoadEntity> stored = graph.findAll(DS_NAME);
            assertThat(stored)
                    .as("DependencyGraph must contain exactly the unique entities from initial load")
                    .hasSize(expectedIds.size());

            // PROPERTY: Every entity must be findable by its ID
            for (Integer id : expectedIds) {
                InitialLoadEntity found = graph.findById(DS_NAME, id);
                assertThat(found)
                        .as("Entity with id=%d must be findable in DependencyGraph", id)
                        .isNotNull();
                assertThat(found.getIdentity()).isEqualTo(id);
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Property 2b: Initial load with TimeWindowRule ====================

    /**
     * Property 2b: For any BatchSnapshotEvent processed through
     * IncrementalSyncProcessor WITH a TimeWindowRule, only entities passing the
     * specification filter must be loaded into DependencyGraph. The entity count
     * must match the count of entities that passed the filter.
     *
     * <p>In the unified architecture, initial state is loaded via fetchAll() by the engine,
     * which transitions the datasource to READY before events are processed through the pipeline.</p>
     *
     * <p><b>Validates: Requirements 2.3</b></p>
     */
    @Property(tries = 100)
    void initialLoadWithTimeWindowRuleLoadsOnlyValidEntities(
            @ForAll("timedEntityBatches") List<TimedInitialLoadEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // TimeWindowRule: only entities with valid=true pass
            TimeWindowRule<TimedInitialLoadEntity> rule = new TimeWindowRule<>(
                    DS_NAME,
                    Duration.ofHours(2),
                    () -> new Specification<TimedInitialLoadEntity>() {
                        @Override
                        public Predicate<TimedInitialLoadEntity> toPredicate() {
                            return TimedInitialLoadEntity::isValid;
                        }
                    }
            );

            // State is READY — in the unified architecture, initial load via fetchAll()
            // transitions the datasource to READY before events are processed.
            @SuppressWarnings("unchecked")
            StreamingDataSource<TimedInitialLoadEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);

            factory.registerDataSource(DS_NAME, streamingDs);
            factory.registerTimeWindowRule(DS_NAME, rule);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Model: compute expected IDs that pass the filter
            // Filter first, then last-write-wins for duplicate IDs among passing entities
            Set<Integer> idsPassingFilter = new HashSet<>();
            Set<Integer> allIds = new HashSet<>();
            for (TimedInitialLoadEntity entity : entities) {
                allIds.add(entity.getIdentity());
                if (entity.isValid()) {
                    idsPassingFilter.add(entity.getIdentity());
                }
            }
            Set<Integer> expiredOnlyIds = new HashSet<>(allIds);
            expiredOnlyIds.removeAll(idsPassingFilter);

            // Process event
            BatchSnapshotEvent<TimedInitialLoadEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // PROPERTY: Only valid entities must be in DependencyGraph
            List<TimedInitialLoadEntity> stored = graph.findAll(DS_NAME);
            assertThat(stored)
                    .as("DependencyGraph must contain only entities passing TimeWindowRule filter")
                    .hasSize(idsPassingFilter.size());

            // PROPERTY: Every valid entity must be findable by its ID
            for (Integer validId : idsPassingFilter) {
                TimedInitialLoadEntity found = graph.findById(DS_NAME, validId);
                assertThat(found)
                        .as("Valid entity with id=%d must be findable", validId)
                        .isNotNull();
            }

            // PROPERTY: Expired-only entities must NOT be in DependencyGraph
            for (Integer expiredId : expiredOnlyIds) {
                TimedInitialLoadEntity found = graph.findById(DS_NAME, expiredId);
                assertThat(found)
                        .as("Expired-only entity with id=%d must NOT be in DependencyGraph", expiredId)
                        .isNull();
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<InitialLoadEntity>> entityBatches() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 50);
        Arbitrary<String> values = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);

        Arbitrary<InitialLoadEntity> entityArb = Combinators.combine(ids, values)
                .as(InitialLoadEntity::new);

        return entityArb.list().ofMinSize(1).ofMaxSize(30);
    }

    @Provide
    Arbitrary<List<TimedInitialLoadEntity>> timedEntityBatches() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 50);
        Arbitrary<String> values = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<Boolean> validFlags = Arbitraries.of(true, false);

        Arbitrary<TimedInitialLoadEntity> entityArb = Combinators.combine(ids, values, validFlags)
                .as(TimedInitialLoadEntity::new);

        return entityArb.list().ofMinSize(1).ofMaxSize(30);
    }

    // ==================== Test Entities ====================

    /**
     * Simple test entity for initial load without TimeWindowRule.
     */
    static class InitialLoadEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;

        InitialLoadEntity(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InitialLoadEntity that = (InitialLoadEntity) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "InitialLoadEntity{id=" + id + ", value='" + value + "'}";
        }
    }

    /**
     * Test entity with a validity flag simulating time-window-based expiration
     * for initial load with TimeWindowRule.
     */
    static class TimedInitialLoadEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;
        private final boolean valid;

        TimedInitialLoadEntity(int id, String value, boolean valid) {
            this.id = id;
            this.value = value;
            this.valid = valid;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimedInitialLoadEntity that = (TimedInitialLoadEntity) o;
            return id == that.id && valid == that.valid
                    && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value, valid);
        }

        @Override
        public String toString() {
            return "TimedInitialLoadEntity{id=" + id + ", valid=" + valid + "}";
        }
    }
}
