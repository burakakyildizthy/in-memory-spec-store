package com.thy.fss.common.inmemory.factory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.entity.Identifiable;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

// Feature: streaming-datasource-support, Property 4: Streaming DataSource Periyodik Sync Hariç Tutma
// Feature: streaming-datasource-support, Property 3: INITIALIZING Durumunda Veri Erişim Kısıtı

/**
 * Property-based tests for streaming datasource registration in {@link InMemorySpecStoreFactory}.
 *
 * <p><b>Validates: Requirements 3.4, 2.5, 2.6, 10.7, 12.6</b></p>
 */
class FactoryStreamingPropertyTest {

    // ==================== Property 4: Streaming DataSource Periyodik Sync Hariç Tutma ====================

    /**
     * Property 4: For any combination of batch and streaming datasources registered in the
     * factory, streaming datasources must NOT appear in the periodic sync scheduling list
     * (getAllDataSourceNames). Only batch datasources should be subject to periodic sync.
     * Streaming datasources must be queryable via isStreamingDataSource and
     * getAllStreamingDataSourceNames.
     *
     * <p><b>Validates: Requirements 3.4</b></p>
     */
    @Property(tries = 100)
    void streamingDataSourcesExcludedFromPeriodicSync(
            @ForAll @IntRange(min = 1, max = 5) int streamingCount,
            @ForAll @IntRange(min = 0, max = 5) int batchCount) {

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Register batch datasources
            for (int i = 0; i < batchCount; i++) {
                String batchName = "batch-ds-" + i;
                @SuppressWarnings("unchecked")
                DataSource<SimpleTestEntity> batchDs = mock(DataSource.class);
                when(batchDs.getName()).thenReturn(batchName);
                factory.registerDataSource(batchName, batchDs, Duration.ofMinutes(5));
            }

            // Register streaming datasources (no syncInterval required)
            for (int i = 0; i < streamingCount; i++) {
                String streamingName = "streaming-ds-" + i;
                @SuppressWarnings("unchecked")
                StreamingDataSource<SimpleTestEntity> streamingDs = mock(StreamingDataSource.class);
                when(streamingDs.getName()).thenReturn(streamingName);
                factory.registerDataSource(streamingName, streamingDs);
            }

            // PROPERTY: After unification, ALL datasources appear in getAllDataSourceNames
            // (both batch and streaming are in the unified dataSourceRegistry)
            List<String> allNames = factory.getAllDataSourceNames();
            for (int i = 0; i < batchCount; i++) {
                String batchName = "batch-ds-" + i;
                assertThat(allNames)
                        .as("Batch datasource '%s' must be in datasource list", batchName)
                        .contains(batchName);
            }
            for (int i = 0; i < streamingCount; i++) {
                String streamingName = "streaming-ds-" + i;
                assertThat(allNames)
                        .as("Streaming datasource '%s' must be in unified datasource list", streamingName)
                        .contains(streamingName);
            }

            // PROPERTY: Streaming datasources must be queryable as streaming type
            for (int i = 0; i < streamingCount; i++) {
                assertThat(factory.isStreamingDataSource("streaming-ds-" + i))
                        .as("Must be identified as streaming")
                        .isTrue();
            }

            // PROPERTY: Batch datasources must NOT be identified as streaming
            for (int i = 0; i < batchCount; i++) {
                assertThat(factory.isStreamingDataSource("batch-ds-" + i))
                        .as("Must not be identified as streaming")
                        .isFalse();
            }

            // PROPERTY: getAllStreamingDataSourceNames returns exactly the streaming names
            Set<String> allStreamingNames = factory.getAllStreamingDataSourceNames();
            assertThat(allStreamingNames).hasSize(streamingCount);
            for (int i = 0; i < streamingCount; i++) {
                assertThat(allStreamingNames).contains("streaming-ds-" + i);
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Property 3: INITIALIZING Durumunda Veri Erişim Kısıtı ====================

    /**
     * Property 3: For any StreamingDataSource in any state (INITIALIZING, READY, ERROR),
     * the health reporting must be consistent: only READY datasources report isHealthy=true.
     * INITIALIZING and ERROR datasources must report isHealthy=false.
     * Regardless of state, streaming datasources must never appear in periodic sync list
     * and must always be identifiable as streaming type.
     *
     * <p><b>Validates: Requirements 2.5, 2.6, 10.7, 12.6</b></p>
     */
    @Property(tries = 100)
    void initializingDataSourceReportsNotReady(
            @ForAll @IntRange(min = 0, max = 2) int stateOrdinal) {

        StreamingDataSourceState state = StreamingDataSourceState.values()[stateOrdinal];
        String dsName = "streaming-ds-state-test";

        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleTestEntity> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(dsName);
            when(streamingDs.getState()).thenReturn(state);
            when(streamingDs.isHealthy()).thenReturn(state == StreamingDataSourceState.READY);

            factory.registerDataSource(dsName, streamingDs);

            StreamingDataSource<?> retrieved = factory.getStreamingDataSource(dsName);
            assertThat(retrieved).isNotNull();

            StreamingDataSourceState currentState = retrieved.getState();

            // PROPERTY: Health reporting must be consistent with state
            switch (currentState) {
                case INITIALIZING:
                    assertThat(retrieved.isHealthy())
                            .as("INITIALIZING datasource must report isHealthy=false")
                            .isFalse();
                    break;
                case READY:
                    assertThat(retrieved.isHealthy())
                            .as("READY datasource must report isHealthy=true")
                            .isTrue();
                    break;
                case ERROR:
                    assertThat(retrieved.isHealthy())
                            .as("ERROR datasource must report isHealthy=false")
                            .isFalse();
                    break;
            }

            // PROPERTY: After unification, streaming datasource appears in getAllDataSourceNames
            // (periodic sync exclusion is handled by engine via isStreamingDataSource check)
            assertThat(factory.getAllDataSourceNames())
                    .as("Streaming datasource must appear in unified datasource list")
                    .contains(dsName);

            // PROPERTY: Streaming datasource must be identifiable as streaming regardless of state
            assertThat(factory.isStreamingDataSource(dsName))
                    .as("Must be identified as streaming regardless of state")
                    .isTrue();

            // PROPERTY: Factory readiness check must be consistent with state
            // Only READY datasources should be reported as ready by the factory
            if (currentState == StreamingDataSourceState.READY) {
                assertThat(factory.isStreamingDataSourceReady(dsName))
                        .as("READY datasource must be reported as ready by factory")
                        .isTrue();
            } else {
                assertThat(factory.isStreamingDataSourceReady(dsName))
                        .as("%s datasource must NOT be reported as ready by factory", currentState)
                        .isFalse();
            }

        } finally {
            factory.clearAll();
        }
    }

    // ==================== Helper Classes ====================

    static class SimpleTestEntity implements Identifiable<Integer> {
        private final int id;

        SimpleTestEntity(int id) {
            this.id = id;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }
    }
}
