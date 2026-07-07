package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

// Feature: streaming-datasource-support, Property 16: Artımlı Güncelleme Sonrası Versiyon Artışı

/**
 * Property-based test verifying that {@code localStreamingVersion} in
 * {@link IncrementalSyncProcessor} monotonically increases after each
 * successful batch snapshot processing (Phase 4 consumer propagation).
 *
 * <p><b>Validates: Requirements 10.6</b></p>
 */
class StreamingVersionIncrementPropertyTest {

    private static final String DS_NAME = "version-test-ds";

    /**
     * Property 16: For any sequence of N batch snapshot events processed
     * successfully, the streaming version must:
     * <ul>
     *   <li>Start at 0 before any processing</li>
     *   <li>Strictly increase after each successful processing (Engine increments after each call)</li>
     *   <li>Never decrease</li>
     *   <li>Be &ge; N after processing N events</li>
     * </ul>
     *
     * <p>Bug 3 fix: Version increment now happens in Engine listener, not inside Processor.
     * This test simulates the Engine's behavior by incrementing the shared AtomicLong
     * after each processBatchSnapshot() call.</p>
     *
     * <p><b>Validates: Requirements 10.6</b></p>
     */
    @Property(tries = 100)
    void streamingVersionMonotonicallyIncreasesAfterEachProcessing(
            @ForAll("batchSequences") List<List<VersionTestEntity>> batches) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            DependencyGraph graph = new DependencyGraph();
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            @SuppressWarnings("unchecked")
            StreamingDataSource<VersionTestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(DS_NAME);
            factory.registerDataSource(DS_NAME, streamingDs);

            // Shared version counter — simulates Engine's streamingVersion (single source of truth)
            AtomicLong sharedVersion = new AtomicLong(0);
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, sharedVersion);

            // --- PROPERTY: Version starts at 0 before any processing ---
            assertThat(processor.getLocalStreamingVersion())
                    .as("Version must start at 0 before any processing")
                    .isEqualTo(0L);

            long previousVersion = processor.getLocalStreamingVersion();
            List<Long> versionHistory = new ArrayList<>();
            versionHistory.add(previousVersion);

            for (List<VersionTestEntity> batch : batches) {
                BatchSnapshotEvent<VersionTestEntity> event = new BatchSnapshotEvent<>(
                        batch, Instant.now());

                processor.processBatchSnapshot(DS_NAME, event);
                // Simulate Engine listener: increment shared version AFTER processing
                sharedVersion.incrementAndGet();

                long currentVersion = processor.getLocalStreamingVersion();

                // --- PROPERTY: Each successive processing results in a strictly higher version ---
                assertThat(currentVersion)
                        .as("Version must strictly increase after processing (was %d)", previousVersion)
                        .isGreaterThan(previousVersion);

                // --- PROPERTY: Version never decreases ---
                assertThat(currentVersion)
                        .as("Version must never decrease")
                        .isGreaterThan(previousVersion);

                versionHistory.add(currentVersion);
                previousVersion = currentVersion;
            }

            // --- PROPERTY: After processing N events, version >= N ---
            assertThat(processor.getLocalStreamingVersion())
                    .as("After processing %d events, version must be >= %d", batches.size(), batches.size())
                    .isGreaterThanOrEqualTo(batches.size());

            // --- Verify entire history is strictly monotonically increasing ---
            for (int i = 1; i < versionHistory.size(); i++) {
                assertThat(versionHistory.get(i))
                        .as("Version history must be strictly increasing at index %d", i)
                        .isGreaterThan(versionHistory.get(i - 1));
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<List<List<VersionTestEntity>>> batchSequences() {
        Arbitrary<VersionTestEntity> entityArb = Combinators.combine(
                Arbitraries.integers().between(1, 50),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        ).as(VersionTestEntity::new);

        Arbitrary<List<VersionTestEntity>> singleBatch = entityArb.list()
                .ofMinSize(1).ofMaxSize(20);

        return singleBatch.list().ofMinSize(1).ofMaxSize(15);
    }

    // ==================== Test Entity ====================

    static class VersionTestEntity implements Identifiable<Integer> {
        private final int id;
        private final String value;

        VersionTestEntity(int id, String value) {
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
            VersionTestEntity that = (VersionTestEntity) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "VersionTestEntity{id=" + id + ", value='" + value + "'}";
        }
    }
}
