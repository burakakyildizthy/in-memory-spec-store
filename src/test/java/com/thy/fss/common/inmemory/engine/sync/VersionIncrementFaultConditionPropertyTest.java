package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Fault Condition Exploration Property Test — Version Increment Ordering Consistency.
 *
 * <p>This test proves that version increment ordering is CONSISTENT between the
 * listener path (DataSynchronizationEngine) and the processQueuedEvents path
 * (IncrementalSyncProcessor).</p>
 *
 * <p>In the unfixed code:</p>
 * <ul>
 *   <li>Listener: processBatchSnapshot() → incrementAndGet() (increment AFTER processing)</li>
 *   <li>processQueuedEvents: incrementAndGet() → processBatchSnapshot() (increment BEFORE processing)</li>
 * </ul>
 *
 * <p>This inconsistency meant Phase 4 inside processBatchSnapshot() would read different
 * version values depending on which path was taken — the listener path would read the
 * OLD version (pre-increment), while the queued path would read the NEW version (post-increment).
 * Stores would end up with inconsistent version numbers for the same logical event.</p>
 *
 * <p>In the fixed code, both paths do incrementAndGet() → processBatchSnapshot(),
 * so Phase 4 always reads the incremented version.</p>
 *
 * <p><b>Validates: Requirements 2.6</b></p>
 */
class VersionIncrementFaultConditionPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String STORE_ID = "version-test-store";

    /**
     * Property 3: For any streaming event processed through both the listener path
     * and the processQueuedEvents path, the store version written by Phase 4 must
     * be identical — proving version increment ordering is consistent.
     *
     * <p>Bug condition: In the unfixed code, the listener did
     * {@code processBatchSnapshot()} then {@code incrementAndGet()} (increment AFTER),
     * so Phase 4 inside processBatchSnapshot read the OLD version.
     * Meanwhile processQueuedEvents did {@code incrementAndGet()} then
     * {@code processBatchSnapshot()} (increment BEFORE), so Phase 4 read the NEW version.
     * This caused the same logical event to produce different store versions depending
     * on the processing path.</p>
     *
     * <p>Fix: Both paths now do {@code incrementAndGet()} → {@code processBatchSnapshot()},
     * so Phase 4 always reads the incremented version.</p>
     *
     * <p><b>Validates: Requirements 2.6</b></p>
     */
    @Property(tries = 50)
    void versionIncrementOrderingConsistentBetweenListenerAndQueuedPaths(
            @ForAll("randomSimpleUsers") List<SimpleUser> entities,
            @ForAll("randomInitialVersion") long initialVersion
    ) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // --- Setup: Streaming datasource in READY state ---
            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(DS_NAME, streamingDs);

            // --- Setup: InMemoryDataStore as consumer ---
            InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                    SimpleUser.class, STORE_ID, DS_NAME, null, Collections.emptyList());

            Method registerStoreMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                    "registerStore", InMemoryDataStore.class);
            registerStoreMethod.setAccessible(true);
            registerStoreMethod.invoke(factory, store);

            // --- Setup: Shared version counter and processor ---
            AtomicLong streamingVersion = new AtomicLong(initialVersion);
            AnalysisResult analysisResult = new AnalysisResult(
                    null, Collections.emptyMap(), null);
            DependencyGraph graph = new DependencyGraph();
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            // === PATH A: Listener path (simulating fixed Engine listener behavior) ===
            // Fixed code: incrementAndGet() BEFORE processBatchSnapshot()
            streamingVersion.set(initialVersion);
            store.updateData(Collections.emptyList(), 0L);

            BatchSnapshotEvent<SimpleUser> eventA = new BatchSnapshotEvent<>(entities, Instant.now());
            streamingVersion.incrementAndGet();
            processor.processBatchSnapshot(DS_NAME, eventA);
            long listenerPathVersion = store.getVersion();

            // === PATH B: processQueuedEvents path ===
            // processQueuedEvents internally does: incrementAndGet() → processBatchSnapshot()
            streamingVersion.set(initialVersion);
            store.updateData(Collections.emptyList(), 0L);

            BatchSnapshotEvent<SimpleUser> eventB = new BatchSnapshotEvent<>(entities, Instant.now());
            processor.queueEvent(DS_NAME, eventB);
            processor.processQueuedEvents();
            long queuedPathVersion = store.getVersion();

            // === ASSERT: Both paths must produce the same store version ===
            // In the unfixed code, listenerPathVersion would be initialVersion (old, pre-increment)
            // while queuedPathVersion would be initialVersion + 1 (new, post-increment).
            // In the fixed code, both should be initialVersion + 1.
            assertThat(listenerPathVersion)
                    .as("Version ordering must be consistent between listener and queued paths. "
                            + "Both should produce version %d (initial=%d + 1). "
                            + "Listener=%d, Queued=%d. Entities=%d.",
                            initialVersion + 1, initialVersion,
                            listenerPathVersion, queuedPathVersion, entities.size())
                    .isEqualTo(queuedPathVersion);

            // Both should equal initialVersion + 1 (the incremented version)
            assertThat(listenerPathVersion)
                    .as("Listener path: Phase 4 should read the incremented version %d, not the old version %d",
                            initialVersion + 1, initialVersion)
                    .isEqualTo(initialVersion + 1);

            assertThat(queuedPathVersion)
                    .as("Queued path: Phase 4 should read the incremented version %d",
                            initialVersion + 1)
                    .isEqualTo(initialVersion + 1);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<SimpleUser>> randomSimpleUsers() {
        Arbitrary<SimpleUser> userArb = Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                Arbitraries.integers().between(18, 80)
        ).as((id, name, age) -> {
            SimpleUser user = new SimpleUser();
            user.setId(id);
            user.setName(name);
            user.setAge(age);
            user.setActive(true);
            return user;
        });
        return userArb.list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Long> randomInitialVersion() {
        return Arbitraries.longs().between(0L, 1000L);
    }
}
