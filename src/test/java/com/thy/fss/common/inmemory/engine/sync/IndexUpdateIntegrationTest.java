package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying that DependencyGraph index updates are properly
 * triggered from the IncrementalSyncProcessor pipeline (Bug 1 fix).
 *
 * <p>After {@code processBatchSnapshot()}, {@code lookup()} must return
 * up-to-date results reflecting the upserted entities.</p>
 *
 * <p><b>Validates: Requirements 2.1</b></p>
 */
class IndexUpdateIntegrationTest {

    private static final String DS_NAME = "index-integration-ds";
    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";
    private static final String GAMMA = "gamma";

    private InMemorySpecStoreFactory factory;
    private DependencyGraph graph;
    private IncrementalSyncProcessor processor;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        graph = new DependencyGraph();
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);

        // Register a streaming datasource in READY state
        @SuppressWarnings("unchecked")
        StreamingDataSource<IndexEntity> streamingDs = mock(StreamingDataSource.class);
        when(streamingDs.getName()).thenReturn(DS_NAME);
        when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(DS_NAME, streamingDs);

        processor = new IncrementalSyncProcessor(factory, graph, analysisResult, new AtomicLong(0));
    }

    @AfterEach
    void tearDown() {
        factory.clearAll();
    }

    @Test
    void lookupReturnsCurrentResultsAfterPipelineUpsert() {
        // Register an index using custom extractor (avoids SpecificationService dependency)
        StringAttribute<IndexEntity> categoryAttr =
                new StringAttribute<>("category", IndexEntity.class);
        IndexDefinition<IndexEntity> indexDef = IndexDefinition
                .<IndexEntity>builder(IndexEntity.class)
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((IndexEntity) entity).getCategory())
                .build();
        graph.registerIndex(DS_NAME, indexDef);

        // Process entities through the pipeline
        List<IndexEntity> entities = List.of(
                new IndexEntity(1, ALPHA),
                new IndexEntity(2, BETA),
                new IndexEntity(3, ALPHA)
        );
        BatchSnapshotEvent<IndexEntity> event = new BatchSnapshotEvent<>(
                entities, Instant.now());
        processor.processBatchSnapshot(DS_NAME, event);

        // lookup() must return up-to-date results
        List<IndexEntity> alphaResults = graph.lookup(DS_NAME, indexDef, ALPHA);
        assertThat(alphaResults)
                .as("lookup('alpha') should return 2 entities after pipeline upsert")
                .hasSize(2)
                .extracting(IndexEntity::getIdentity)
                .containsExactlyInAnyOrder(1, 3);

        List<IndexEntity> betaResults = graph.lookup(DS_NAME, indexDef, BETA);
        assertThat(betaResults)
                .as("lookup('beta') should return 1 entity after pipeline upsert")
                .hasSize(1)
                .extracting(IndexEntity::getIdentity)
                .containsExactly(2);
    }

    @Test
    void lookupReturnsUpdatedResultsAfterEntityUpdate() {
        // Register index
        StringAttribute<IndexEntity> categoryAttr =
                new StringAttribute<>("category", IndexEntity.class);
        IndexDefinition<IndexEntity> indexDef = IndexDefinition
                .<IndexEntity>builder(IndexEntity.class)
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((IndexEntity) entity).getCategory())
                .build();
        graph.registerIndex(DS_NAME, indexDef);

        // Initial upsert
        List<IndexEntity> initial = List.of(
                new IndexEntity(1, ALPHA),
                new IndexEntity(2, ALPHA)
        );
        processor.processBatchSnapshot(DS_NAME, new BatchSnapshotEvent<>(
                initial, Instant.now()));

        assertThat(graph.lookup(DS_NAME, indexDef, ALPHA)).hasSize(2);
        assertThat(graph.lookup(DS_NAME, indexDef, BETA)).isEmpty();

        // Update entity 2: move from ALPHA to BETA
        List<IndexEntity> update = List.of(new IndexEntity(2, BETA));
        processor.processBatchSnapshot(DS_NAME, new BatchSnapshotEvent<>(
                update, Instant.now()));

        // Index must reflect the update
        List<IndexEntity> alphaAfter = graph.lookup(DS_NAME, indexDef, ALPHA);
        assertThat(alphaAfter)
                .as("After moving entity 2 to beta, alpha should have 1 entity")
                .hasSize(1)
                .extracting(IndexEntity::getIdentity)
                .containsExactly(1);

        List<IndexEntity> betaAfter = graph.lookup(DS_NAME, indexDef, BETA);
        assertThat(betaAfter)
                .as("After moving entity 2 to beta, beta should have 1 entity")
                .hasSize(1)
                .extracting(IndexEntity::getIdentity)
                .containsExactly(2);
    }

    @Test
    void fallbackUpsertPathAlsoUpdatesIndexes() {
        // Register index
        StringAttribute<IndexEntity> categoryAttr =
                new StringAttribute<>("category", IndexEntity.class);
        IndexDefinition<IndexEntity> indexDef = IndexDefinition
                .<IndexEntity>builder(IndexEntity.class)
                .addKeyFieldWithPath(
                        List.of(categoryAttr),
                        entity -> ((IndexEntity) entity).getCategory())
                .build();
        graph.registerIndex(DS_NAME, indexDef);

        // Process entities — even if upsertAll fails and falls back to individual upsert,
        // indexes should still be updated (the updateIndexes call is after both paths)
        List<IndexEntity> entities = List.of(
                new IndexEntity(10, GAMMA),
                new IndexEntity(20, GAMMA),
                new IndexEntity(30, "delta")
        );
        processor.processBatchSnapshot(DS_NAME, new BatchSnapshotEvent<>(
                entities, Instant.now()));

        assertThat(graph.lookup(DS_NAME, indexDef, GAMMA))
                .as("lookup('gamma') should return 2 entities")
                .hasSize(2);
        assertThat(graph.lookup(DS_NAME, indexDef, "delta"))
                .as("lookup('delta') should return 1 entity")
                .hasSize(1);
    }

    // === Test Entity ===

    static class IndexEntity implements Identifiable<Integer> {
        private final int id;
        private final String category;

        IndexEntity(int id, String category) {
            this.id = id;
            this.category = category;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IndexEntity that = (IndexEntity) o;
            return id == that.id && Objects.equals(category, that.category);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, category);
        }

        @Override
        public String toString() {
            return "IndexEntity{id=" + id + ", category='" + category + "'}";
        }
    }
}
