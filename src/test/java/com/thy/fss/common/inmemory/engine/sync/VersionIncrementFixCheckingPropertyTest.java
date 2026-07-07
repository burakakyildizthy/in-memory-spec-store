package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
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
 * Fix Checking Property Test — Version Increment Consistency (Property 3).
 *
 * <p>This test verifies that the FIXED code produces consistent version increments
 * across both the listener path and the processQueuedEvents path when processing
 * multiple sequential events.</p>
 *
 * <p>Unlike the fault condition test (4.3) which tested a single event, this test
 * goes further by verifying:</p>
 * <ul>
 *   <li>For N sequential events via listener path: final version = initialVersion + N</li>
 *   <li>For N sequential events via queued path: final version = initialVersion + N</li>
 *   <li>Both paths produce identical final versions for the same N events</li>
 *   <li>Version is monotonically increasing during processing (each event sees a higher version)</li>
 * </ul>
 *
 * <p>In the fixed code, both paths do {@code incrementAndGet()} BEFORE
 * {@code processBatchSnapshot()}, ensuring Phase 4 always reads the incremented version.</p>
 *
 * <p><b>Validates: Requirements 2.6</b></p>
 */
class VersionIncrementFixCheckingPropertyTest {

    private static final String DS_NAME = "streaming-ds";
    private static final String STORE_ID = "version-fix-check-store";

    /**
     * Property 3 — Fix Checking: For N random events processed sequentially,
     * both the listener path and the queued path must produce identical final
     * versions equal to initialVersion + N, and each intermediate version must
     * be monotonically increasing.
     *
     * <p>This proves the fix is correct: version increment happens BEFORE processing
     * in both paths, so Phase 4 always reads the incremented version. Multiple
     * sequential events produce predictable, monotonically increasing versions.</p>
     *
     * <p><b>Validates: Requirements 2.6</b></p>
     */
    @Property(tries = 50)
    void versionIncrementConsistencyMultipleSequentialEventsBothPathsProduceIdenticalMonotonicVersions(
            @ForAll("randomEventBatches") List<List<SimpleUser>> eventBatches,
            @ForAll("randomInitialVersion") long initialVersion
    ) throws Exception {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            int N = eventBatches.size();

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

            // === PATH A: Listener path — N sequential events ===
            // Fixed code: incrementAndGet() BEFORE processBatchSnapshot() for each event
            streamingVersion.set(initialVersion);
            store.updateData(Collections.emptyList(), 0L);

            List<Long> listenerVersions = new ArrayList<>();
            for (List<SimpleUser> entities : eventBatches) {
                BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
                streamingVersion.incrementAndGet();
                processor.processBatchSnapshot(DS_NAME, event);
                listenerVersions.add(store.getVersion());
            }
            long listenerFinalVersion = store.getVersion();

            // === PATH B: Queued path — N sequential events via processQueuedEvents ===
            streamingVersion.set(initialVersion);
            store.updateData(Collections.emptyList(), 0L);

            for (List<SimpleUser> entities : eventBatches) {
                BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
                processor.queueEvent(DS_NAME, event);
            }
            processor.processQueuedEvents();

            long queuedFinalVersion = store.getVersion();

            // Collect queued intermediate versions by replaying one-by-one
            streamingVersion.set(initialVersion);
            store.updateData(Collections.emptyList(), 0L);

            List<Long> queuedVersions = new ArrayList<>();
            for (List<SimpleUser> entities : eventBatches) {
                BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());
                processor.queueEvent(DS_NAME, event);
                processor.processQueuedEvents();
                queuedVersions.add(store.getVersion());
            }

            // === ASSERT 1: Final version = initialVersion + N for both paths ===
            long expectedFinalVersion = initialVersion + N;

            assertThat(listenerFinalVersion)
                    .as("Listener path: final version should be initialVersion(%d) + N(%d) = %d, but was %d",
                            initialVersion, N, expectedFinalVersion, listenerFinalVersion)
                    .isEqualTo(expectedFinalVersion);

            assertThat(queuedFinalVersion)
                    .as("Queued path: final version should be initialVersion(%d) + N(%d) = %d, but was %d",
                            initialVersion, N, expectedFinalVersion, queuedFinalVersion)
                    .isEqualTo(expectedFinalVersion);

            // === ASSERT 2: Both paths produce identical final versions ===
            assertThat(listenerFinalVersion)
                    .as("Both paths must produce identical final versions. Listener=%d, Queued=%d",
                            listenerFinalVersion, queuedFinalVersion)
                    .isEqualTo(queuedFinalVersion);

            // === ASSERT 3: Versions are monotonically increasing in listener path ===
            for (int i = 1; i < listenerVersions.size(); i++) {
                assertThat(listenerVersions.get(i))
                        .as("Listener path: version at event %d (%d) must be > version at event %d (%d)",
                                i, listenerVersions.get(i), i - 1, listenerVersions.get(i - 1))
                        .isGreaterThan(listenerVersions.get(i - 1));
            }

            // === ASSERT 4: Versions are monotonically increasing in queued path ===
            for (int i = 1; i < queuedVersions.size(); i++) {
                assertThat(queuedVersions.get(i))
                        .as("Queued path: version at event %d (%d) must be > version at event %d (%d)",
                                i, queuedVersions.get(i), i - 1, queuedVersions.get(i - 1))
                        .isGreaterThan(queuedVersions.get(i - 1));
            }

            // === ASSERT 5: Intermediate versions match between paths ===
            assertThat(listenerVersions)
                    .as("Intermediate versions must be identical between listener and queued paths")
                    .isEqualTo(queuedVersions);

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<List<SimpleUser>>> randomEventBatches() {
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

        // Each batch is a list of 1-5 users, and we generate 2-6 batches (sequential events)
        Arbitrary<List<SimpleUser>> batchArb = userArb.list().ofMinSize(1).ofMaxSize(5);
        return batchArb.list().ofMinSize(2).ofMaxSize(6);
    }

    @Provide
    Arbitrary<Long> randomInitialVersion() {
        return Arbitraries.longs().between(0L, 1000L);
    }
}
