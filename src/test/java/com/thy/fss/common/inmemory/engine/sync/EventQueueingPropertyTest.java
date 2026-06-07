package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

// Feature: streaming-datasource-support, Property 14: Full Sync Sırasında Event Kuyruklama

/**
 * Property-based test for event queuing during full sync.
 *
 * <p>Core property: when multiple BatchSnapshotEvents are queued via {@code queueEvent()}
 * during a full sync, calling {@code processQueuedEvents()} must:</p>
 * <ul>
 *   <li>Process ALL queued events (no events lost)</li>
 *   <li>Process events in FIFO order (order preserved)</li>
 *   <li>Leave the queue empty after processing</li>
 *   <li>Result in all entities from queued events ending up in DependencyGraph
 *       (last-write-wins for duplicate IDs across batches)</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 10.5, 11.1, 11.2</b></p>
 */
class EventQueueingPropertyTest {

    private static final String DS_NAME = "queue-test-ds";

    // ==================== Property 14: Full Sync Sırasında Event Kuyruklama ====================

    /**
     * Property 14: For any sequence of BatchSnapshotEvents queued via queueEvent()
     * during a full sync, processQueuedEvents() must process all events in FIFO order,
     * leave the queue empty, and result in DependencyGraph containing all expected
     * entities (last-write-wins for duplicate IDs across batches).
     *
     * <p>Test approach:</p>
     * <ol>
     *   <li>Create an IncrementalSyncProcessor with a DependencyGraph</li>
     *   <li>Generate random batches of entities</li>
     *   <li>Queue them as BatchSnapshotEvents via queueEvent()</li>
     *   <li>Call processQueuedEvents()</li>
     *   <li>Verify DependencyGraph contains all expected entities (last-write-wins)</li>
     *   <li>Verify the queue is empty (a second processQueuedEvents() is a no-op)</li>
     * </ol>
     *
     * <p><b>Validates: Requirements 10.5, 11.1, 11.2</b></p>
     */
    @Property(tries = 100)
    void queuedEventsAreProcessedInFifoOrderWithNoLoss(
            @ForAll("batchSequences") List<List<QueueTestEntity>> batches) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource without TimeWindowRule (no filtering)
            @SuppressWarnings("unchecked")
            StreamingDataSource<QueueTestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            factory.registerDataSource(DS_NAME, streamingDs);

            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, new AtomicLong(0));

            // --- Model: track expected final state with last-write-wins ---
            // Process batches in order to build the expected model
            Map<Integer, QueueTestEntity> model = new HashMap<>();

            for (List<QueueTestEntity> batch : batches) {
                // Queue the event
                BatchSnapshotEvent<QueueTestEntity> event = new BatchSnapshotEvent<>(
                        batch, Instant.now());
                processor.queueEvent(DS_NAME, event);

                // Apply last-write-wins to model (FIFO order)
                for (QueueTestEntity entity : batch) {
                    model.put(entity.getIdentity(), entity);
                }
            }

            // --- Process all queued events ---
            processor.processQueuedEvents();

            // --- PROPERTY 1: All queued events are processed (no events lost) ---
            // DependencyGraph must contain exactly the unique IDs from the model
            List<QueueTestEntity> storedEntities = graph.findAll(DS_NAME);
            assertThat(storedEntities)
                    .as("All unique entity IDs must be present in DependencyGraph")
                    .hasSize(model.size());

            // --- PROPERTY 2: Events processed in FIFO order (last-write-wins) ---
            // For each entity ID, the stored value must match the model (last batch wins)
            for (Map.Entry<Integer, QueueTestEntity> entry : model.entrySet()) {
                QueueTestEntity found = graph.findById(DS_NAME, entry.getKey());
                assertThat(found)
                        .as("Entity id=%d must be in DependencyGraph", entry.getKey())
                        .isNotNull();
                assertThat(found.getValue())
                        .as("Entity id=%d must have last-write-wins value", entry.getKey())
                        .isEqualTo(entry.getValue().getValue());
            }

            // --- PROPERTY 3: Queue is empty after processing ---
            // A second processQueuedEvents() should be a no-op; graph state unchanged
            int sizeBeforeSecondCall = graph.<QueueTestEntity>findAll(DS_NAME).size();
            processor.processQueuedEvents();
            int sizeAfterSecondCall = graph.<QueueTestEntity>findAll(DS_NAME).size();
            assertThat(sizeAfterSecondCall)
                    .as("Queue must be empty — second processQueuedEvents() must be a no-op")
                    .isEqualTo(sizeBeforeSecondCall);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<List<QueueTestEntity>>> batchSequences() {
        Arbitrary<QueueTestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 30),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        ).as(QueueTestEntity::new);

        Arbitrary<List<QueueTestEntity>> singleBatch = entityArb.list()
                .ofMinSize(1).ofMaxSize(15);

        return singleBatch.list().ofMinSize(1).ofMaxSize(10);
    }

    // ==================== Test Entity ====================

    /**
     * Simple test entity implementing Identifiable&lt;Integer&gt;.
     */
    static class QueueTestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;

        QueueTestEntity(int id, String value) {
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
            QueueTestEntity that = (QueueTestEntity) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "QueueTestEntity{id=" + id + ", value='" + value + "'}";
        }
    }
}
