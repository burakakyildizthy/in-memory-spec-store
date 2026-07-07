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

// Feature: streaming-datasource-support, Property 6: Alım Zamanı Filtreleme — Expire Entity'ler Store'a Alınmaz

/**
 * Property-based tests for intake-time filtering in {@link IncrementalSyncProcessor}.
 *
 * <p>When a {@link TimeWindowRule} is defined for a datasource, expired entities
 * (where {@code specification.test(entity) == false}) must NOT be added to
 * {@link DependencyGraph}. When NO TimeWindowRule is defined, ALL entities must
 * pass through.</p>
 *
 * <p><b>Validates: Requirements 4.6</b></p>
 */
class IntakeTimeFilteringPropertyTest {

    private static final String DS_NAME = "filtering-test-ds";

    // ==================== Property 6a: With TimeWindowRule — only valid entities stored ====================

    /**
     * Property 6a: For any mix of valid and expired entities processed through
     * IncrementalSyncProcessor with a TimeWindowRule, ONLY valid entities
     * (specification returns true) must be present in DependencyGraph.
     * Expired entities (specification returns false) must NOT be present.
     *
     * <p><b>Validates: Requirements 4.6</b></p>
     */
    @Property(tries = 100)
    void withTimeWindowRuleOnlyValidEntitiesAreStored(
            @ForAll("entityBatches") List<TimedTestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // TimeWindowRule: entities with valid=true pass, valid=false are expired
            TimeWindowRule<TimedTestEntity> rule = new TimeWindowRule<>(
                    DS_NAME,
                    Duration.ofHours(2),
                    () -> new Specification<TimedTestEntity>() {
                        @Override
                        public Predicate<TimedTestEntity> toPredicate() {
                            return TimedTestEntity::isValid;
                        }
                    }
            );

            @SuppressWarnings("unchecked")
            StreamingDataSource<TimedTestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);

            factory.registerDataSource(DS_NAME, streamingDs);
            factory.registerTimeWindowRule(DS_NAME, rule);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Model the processor behavior: filter FIRST, then upsert.
            // Only entities where specification.test() == true survive filtering.
            // After filtering, upsertAll applies them — last-write-wins for duplicate IDs.
            Set<Integer> idsPassingFilter = new HashSet<>();
            Set<Integer> allIds = new HashSet<>();
            for (TimedTestEntity entity : entities) {
                allIds.add(entity.getIdentity());
                if (entity.isValid()) {
                    idsPassingFilter.add(entity.getIdentity());
                }
            }
            // IDs that ONLY appear as expired (never valid) should NOT be in graph
            Set<Integer> expiredOnlyIds = new HashSet<>(allIds);
            expiredOnlyIds.removeAll(idsPassingFilter);

            // Process through the pipeline
            BatchSnapshotEvent<TimedTestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // PROPERTY: All entities that passed the filter must be in DependencyGraph
            for (Integer validId : idsPassingFilter) {
                TimedTestEntity found = graph.findById(DS_NAME, validId);
                assertThat(found)
                        .as("Valid entity with id=%d must be in DependencyGraph", validId)
                        .isNotNull();
            }

            // PROPERTY: Entities that ONLY appeared as expired must NOT be in DependencyGraph
            for (Integer expiredId : expiredOnlyIds) {
                TimedTestEntity found = graph.findById(DS_NAME, expiredId);
                assertThat(found)
                        .as("Expired-only entity with id=%d must NOT be in DependencyGraph", expiredId)
                        .isNull();
            }

            // PROPERTY: Total stored count must equal count of IDs that passed filter
            List<TimedTestEntity> allStored = graph.findAll(DS_NAME);
            assertThat(allStored).hasSize(idsPassingFilter.size());

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Property 6b: Without TimeWindowRule — all entities stored ====================

    /**
     * Property 6b: For any set of entities processed through IncrementalSyncProcessor
     * WITHOUT a TimeWindowRule, ALL entities must be present in DependencyGraph
     * regardless of their valid/expired status.
     *
     * <p><b>Validates: Requirements 4.6</b></p>
     */
    @Property(tries = 100)
    void withoutTimeWindowRuleAllEntitiesAreStored(
            @ForAll("entityBatches") List<TimedTestEntity> entities) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register WITHOUT TimeWindowRule (null)
            @SuppressWarnings("unchecked")
            StreamingDataSource<TimedTestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);

            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // Compute expected unique IDs (last-write-wins for duplicates)
            Set<Integer> expectedIds = new HashSet<>();
            for (TimedTestEntity entity : entities) {
                expectedIds.add(entity.getIdentity());
            }

            // Process through the pipeline
            BatchSnapshotEvent<TimedTestEntity> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            processor.processBatchSnapshot(DS_NAME, event);

            // PROPERTY: ALL entities must be in DependencyGraph (no filtering)
            for (Integer id : expectedIds) {
                TimedTestEntity found = graph.findById(DS_NAME, id);
                assertThat(found)
                        .as("Entity with id=%d must be in DependencyGraph (no TimeWindowRule)", id)
                        .isNotNull();
            }

            // PROPERTY: Total stored count must equal unique entity count
            List<TimedTestEntity> allStored = graph.findAll(DS_NAME);
            assertThat(allStored).hasSize(expectedIds.size());

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<TimedTestEntity>> entityBatches() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 50);
        Arbitrary<Boolean> validFlags = Arbitraries.of(true, false);

        Arbitrary<TimedTestEntity> entityArb = Combinators.combine(ids, validFlags)
                .as((id, valid) -> new TimedTestEntity(id, "value-" + id, valid));

        return entityArb.list().ofMinSize(1).ofMaxSize(30);
    }

    // ==================== Test Entity ====================

    /**
     * Test entity with a validity flag simulating time-window-based expiration.
     * When {@code valid == true}, the entity is within the time window.
     * When {@code valid == false}, the entity is expired (outside the time window).
     */
    static class TimedTestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;
        private final boolean valid;

        TimedTestEntity(int id, String value, boolean valid) {
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
            TimedTestEntity that = (TimedTestEntity) o;
            return id == that.id && valid == that.valid
                    && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value, valid);
        }

        @Override
        public String toString() {
            return "TimedTestEntity{id=" + id + ", valid=" + valid + "}";
        }
    }
}
