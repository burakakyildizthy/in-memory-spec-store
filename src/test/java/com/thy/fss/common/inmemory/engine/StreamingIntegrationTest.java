package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEventListener;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.datasource.TimeWindowRule;
import com.thy.fss.common.inmemory.engine.sync.CompositeVersion;
import com.thy.fss.common.inmemory.engine.sync.DataSourceSyncMetadata;
import com.thy.fss.common.inmemory.engine.sync.DependencyGraph;
import com.thy.fss.common.inmemory.engine.sync.IncrementalSyncProcessor;
import com.thy.fss.common.inmemory.engine.sync.StreamingDataSourceLifecycleManager;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;
import com.thy.fss.common.inmemory.testmodel.SimpleUserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the streaming datasource support feature.
 *
 * <p>These tests verify the full end-to-end flow of batch and streaming
 * datasources working together through the DataSynchronizationEngine.</p>
 *
 * <p>Validates Requirements: 10.1, 10.3, 10.5, 10.6, 13.3</p>
 */
class StreamingIntegrationTest {

    private static final String FILTERED_DS = "filteredDs";
    private static final String NO_RULE_DS = "noRuleDs";
    private static final String QUEUE_DS = "queueDs";
    private static final String STREAM_DS = "streamDs";
    private static final String STREAM_USERS = "streamUsers";
    private static final String BATCH_USERS = "batchUsers";

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private List<TestableInMemoryDataSource<?>> testDataSources;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
        factory.clearAllDataSources();
        testDataSources = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
        for (TestableInMemoryDataSource<?> ds : testDataSources) {
            ds.close();
        }
        testDataSources.clear();
        factory.clearAll();
        factory.clearAllDataSources();
    }

    // ==================== Helpers ====================

    private <T> TestableInMemoryDataSource<T> createBatchDataSource(String name, Class<T> entityType) {
        TestableInMemoryDataSource<T> ds = new TestableInMemoryDataSource<>(name, entityType);
        testDataSources.add(ds);
        return ds;
    }

    private SimpleUser createUser(Long id, String name) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setActive(true);
        return user;
    }

    private SimpleUser createUser(Long id, String name, boolean active) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setActive(active);
        return user;
    }

    // ==================== Test 1: Batch + Streaming Coexistence ====================

    /**
     * Verifies that batch and streaming datasources can coexist in the same engine.
     * Batch sync works normally, and streaming events are processed independently.
     *
     * <p>Validates Requirements: 10.1, 10.3, 13.3</p>
     */
    @Test
    void batchAndStreamingDatasourcesCoexist() {
        // Given: a batch datasource with initial data
        TestableInMemoryDataSource<SimpleUser> batchDs =
                createBatchDataSource(BATCH_USERS, SimpleUser.class);
        batchDs.addItems(List.of(createUser(1L, "Alice"), createUser(2L, "Bob")));
        factory.registerDataSource(BATCH_USERS, batchDs, Duration.ofMinutes(5));

        // And: a streaming datasource (controllable mock)
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>(STREAM_USERS, SimpleUser.class);
        factory.registerDataSource(STREAM_USERS, streamingDs);

        // And: a store backed by the batch datasource
        InMemoryDataStore<SimpleUser> store = factory
                .buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        // When: engine initializes
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Then: engine is running with both datasource types
        assertThat(engine.isRunning()).isTrue();
        assertThat(factory.isStreamingDataSource(STREAM_USERS)).isTrue();
        assertThat(factory.isStreamingDataSource(BATCH_USERS)).isFalse();

        // And: batch sync populates the store
        engine.synchronizeDataSource(BATCH_USERS);
        TestUtil.await(2000);
        assertThat(store.findAll()).hasSize(2);

        // And: streaming events are processed independently
        streamingDs.emitInitialLoad(List.of(
                createUser(10L, "StreamAlice"),
                createUser(11L, "StreamBob")));

        // Verify streaming data is in DependencyGraph
        DependencyGraph graph = engine.getDependencyGraph();
        assertThat((List<?>) graph.findAll(STREAM_USERS)).hasSize(2);

        // And: streaming version incremented
        assertThat(engine.getStreamingVersion()).isGreaterThan(0);

        // And: batch store still has its own data
        assertThat(store.findAll()).hasSize(2);
    }

    // ==================== Test 2: Full Streaming Lifecycle ====================

    /**
     * Verifies the complete streaming lifecycle:
     * INITIALIZING → initial load → READY → incremental updates → consumer propagation.
     *
     * <p>Validates Requirements: 10.1, 13.3</p>
     */
    @Test
    void fullStreamingLifecycle() {
        // Given: a batch datasource (required for store builder resolution)
        TestableInMemoryDataSource<SimpleUser> batchDs =
                createBatchDataSource("users", SimpleUser.class);
        factory.registerDataSource("users", batchDs, Duration.ofMinutes(5));

        // And: a streaming datasource that returns initial data via fetchAll()
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>(STREAM_DS, SimpleUser.class);
        // Pre-load data that fetchAll() will return during initialization
        streamingDs.setFetchAllData(List.of(
                createUser(1L, "Alice"),
                createUser(2L, "Bob"),
                createUser(3L, "Charlie")));
        factory.registerDataSource(STREAM_DS, streamingDs);

        // And: a store
        factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        // When: engine initializes
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Then: engine calls fetchAll() during initialization and transitions to READY
        StreamingDataSourceLifecycleManager lifecycle = engine.getLifecycleManager();
        assertThat(lifecycle.getState(STREAM_DS))
                .isEqualTo(StreamingDataSourceState.READY);

        // And: data is loaded into DependencyGraph via fetchAll()
        DependencyGraph graph = engine.getDependencyGraph();
        assertThat((List<?>) graph.findAll(STREAM_DS)).hasSize(3);

        // When: incremental update arrives
        streamingDs.emitIncrementalUpdate(List.of(
                createUser(4L, "Dave"),
                createUser(1L, "AliceUpdated")));

        // Then: DependencyGraph is updated
        assertThat((List<?>) graph.findAll(STREAM_DS)).hasSize(4);
        SimpleUser alice = graph.findById(STREAM_DS, 1L);
        assertThat(alice.getName()).isEqualTo("AliceUpdated");
    }

    // ==================== Test 3: TimeWindowRule Filtering in Full Flow ====================

    /**
     * Verifies that TimeWindowRule filtering works end-to-end:
     * only valid entities end up in DependencyGraph, expired ones are filtered out.
     *
     * <p>Validates Requirements: 10.1</p>
     */
    @Test
    void timeWindowRuleFiltersExpiredEntities() {
        // Given: a streaming datasource WITH TimeWindowRule
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>(FILTERED_DS, SimpleUser.class);

        // TimeWindowRule: only active users pass (active=true → valid, active=false → expired)
        TimeWindowRule<SimpleUser> rule = new TimeWindowRule<>(
                FILTERED_DS,
                Duration.ofHours(2),
                () -> new Specification<SimpleUser>() {
                    @Override
                    public Predicate<SimpleUser> toPredicate() {
                        return user -> Boolean.TRUE.equals(user.getActive());
                    }
                });

        factory.registerDataSource(FILTERED_DS, streamingDs);
        factory.registerTimeWindowRule(FILTERED_DS, rule);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // When: initial load with mix of valid and expired entities
        streamingDs.emitInitialLoad(List.of(
                createUser(1L, "Active1", true),
                createUser(2L, "Expired1", false),
                createUser(3L, "Active2", true),
                createUser(4L, "Expired2", false),
                createUser(5L, "Active3", true)));

        // Then: only valid (active) entities are in DependencyGraph
        DependencyGraph graph = engine.getDependencyGraph();
        List<SimpleUser> stored = graph.findAll(FILTERED_DS);
        assertThat(stored).hasSize(3);
        assertThat(stored).allMatch(u -> Boolean.TRUE.equals(u.getActive()));

        // And: expired entities are NOT stored
        assertThat((SimpleUser) graph.findById(FILTERED_DS, 2L)).isNull();
        assertThat((SimpleUser) graph.findById(FILTERED_DS, 4L)).isNull();

        // When: incremental update with more mixed entities
        streamingDs.emitIncrementalUpdate(List.of(
                createUser(6L, "Active4", true),
                createUser(7L, "Expired3", false)));

        // Then: only the valid one is added
        assertThat((List<?>) graph.findAll(FILTERED_DS)).hasSize(4);
        assertThat((SimpleUser) graph.findById(FILTERED_DS, 6L)).isNotNull();
        assertThat((SimpleUser) graph.findById(FILTERED_DS, 7L)).isNull();
    }

    // ==================== Test 4: TimeWindowRule Optional (No Rule) ====================

    /**
     * Verifies that a streaming datasource WITHOUT TimeWindowRule (null)
     * passes ALL entities through without filtering.
     *
     * <p>Validates Requirements: 10.1</p>
     */
    @Test
    void noTimeWindowRuleAllEntitiesPassThrough() {
        // Given: a streaming datasource WITHOUT TimeWindowRule
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>(NO_RULE_DS, SimpleUser.class);
        factory.registerDataSource(NO_RULE_DS, streamingDs);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // When: events with mix of active/inactive entities
        streamingDs.emitInitialLoad(List.of(
                createUser(1L, "Active", true),
                createUser(2L, "Inactive", false),
                createUser(3L, "NullActive")));

        // Then: ALL entities are stored (no filtering)
        DependencyGraph graph = engine.getDependencyGraph();
        List<SimpleUser> stored = graph.findAll(NO_RULE_DS);
        assertThat(stored).hasSize(3);
        assertThat((SimpleUser) graph.findById(NO_RULE_DS, 1L)).isNotNull();
        assertThat((SimpleUser) graph.findById(NO_RULE_DS, 2L)).isNotNull();
        assertThat((SimpleUser) graph.findById(NO_RULE_DS, 3L)).isNotNull();
    }

    // ==================== Test 5: Event Queuing During Full Sync ====================

    /**
     * Verifies that streaming events arriving during a full sync are queued
     * and processed after the sync completes.
     *
     * <p>Validates Requirements: 10.5</p>
     */
    @Test
    void eventQueuedDuringFullSyncAndProcessedAfter() {
        // Given: a batch datasource
        TestableInMemoryDataSource<SimpleUser> batchDs =
                createBatchDataSource("batchDs", SimpleUser.class);
        batchDs.addItems(List.of(createUser(1L, "BatchUser")));
        factory.registerDataSource("batchDs", batchDs, Duration.ofMinutes(5));

        // And: a streaming datasource
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>(QUEUE_DS, SimpleUser.class);
        factory.registerDataSource(QUEUE_DS, streamingDs);

        factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // First, complete initial load so the streaming ds is READY
        streamingDs.emitInitialLoad(List.of(createUser(100L, "InitialStreamUser")));

        IncrementalSyncProcessor processor = engine.getIncrementalSyncProcessor();

        // When: simulate full sync in progress
        processor.setFullSyncInProgress(true);

        // And: streaming events arrive during full sync — they should be queued
        processor.queueEvent(QUEUE_DS, new BatchSnapshotEvent<>(
                List.of(createUser(200L, "QueuedUser1")),
                Instant.now()));
        processor.queueEvent(QUEUE_DS, new BatchSnapshotEvent<>(
                List.of(createUser(201L, "QueuedUser2")),
                Instant.now()));

        // Then: events are queued, not yet processed
        assertThat(processor.getQueuedEventCount()).isEqualTo(2);

        // When: full sync completes and queued events are processed
        processor.setFullSyncInProgress(false);
        processor.processQueuedEvents();

        // Then: queued events have been processed
        assertThat(processor.getQueuedEventCount()).isZero();

        // And: queued entities are now in DependencyGraph
        DependencyGraph graph = engine.getDependencyGraph();
        assertThat((SimpleUser) graph.findById(QUEUE_DS, 200L)).isNotNull();
        assertThat((SimpleUser) graph.findById(QUEUE_DS, 201L)).isNotNull();
    }

    // ==================== Test 6: CompositeVersion Correctness ====================

    /**
     * Verifies that CompositeVersion correctly combines batch and streaming versions.
     * Streaming version increments with each processed event.
     *
     * <p>Validates Requirements: 10.6</p>
     */
    @Test
    void compositeVersionCombinesBatchAndStreamingVersions() {
        // Given: a streaming datasource only (no batch datasource to avoid scheduler race)
        ControllableStreamingDataSource<SimpleUser> streamingDs =
                new ControllableStreamingDataSource<>("streamVersionDs", SimpleUser.class);
        factory.registerDataSource("streamVersionDs", streamingDs);

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Then: initial streaming version is 0
        assertThat(engine.getStreamingVersion()).isZero();
        assertThat(engine.getLastStreamingUpdateTimestamp()).isNull();

        // When: initial load event
        Instant beforeFirstEvent = Instant.now();
        streamingDs.emitInitialLoad(List.of(createUser(10L, "StreamUser1")));

        // Then: streaming version incremented
        long versionAfterInitialLoad = engine.getStreamingVersion();
        assertThat(versionAfterInitialLoad).isGreaterThan(0);
        assertThat(engine.getLastStreamingUpdateTimestamp()).isNotNull();

        // When: incremental updates
        streamingDs.emitIncrementalUpdate(List.of(createUser(11L, "StreamUser2")));
        long versionAfterUpdate1 = engine.getStreamingVersion();
        assertThat(versionAfterUpdate1).isGreaterThan(versionAfterInitialLoad);

        streamingDs.emitIncrementalUpdate(List.of(createUser(12L, "StreamUser3")));
        long versionAfterUpdate2 = engine.getStreamingVersion();
        assertThat(versionAfterUpdate2).isGreaterThan(versionAfterUpdate1);

        // Then: CompositeVersion reflects both batch and streaming versions
        CompositeVersion composite = engine.getCompositeVersion();
        assertThat(composite).isNotNull();
        assertThat(composite.streamingVersion()).isEqualTo(versionAfterUpdate2);
        assertThat(composite.lastStreamingUpdate()).isNotNull();
        // Batch version comes from the engine's DataVersion
        assertThat(composite.batchVersion()).isGreaterThanOrEqualTo(0);
    }

    // ==================== Controllable StreamingDataSource for Testing ====================

    /**
     * A test-only StreamingDataSource implementation that allows controlled
     * emission of events. Listeners are stored and events are dispatched
     * synchronously for deterministic testing.
     */
    private static class ControllableStreamingDataSource<T extends Identifiable<?>>
            implements StreamingDataSource<T> {

        private final String name;
        private final Class<T> entityType;
        private final List<BatchSnapshotEventListener<T>> listeners = new CopyOnWriteArrayList<>();
        private volatile StreamingDataSourceState state = StreamingDataSourceState.INITIALIZING;
        private volatile boolean healthy = true;
        private volatile com.thy.fss.common.inmemory.datasource.DataSource<T> fallbackDataSource;
        private volatile List<T> fetchAllData = Collections.emptyList();

        ControllableStreamingDataSource(String name, Class<T> entityType) {
            this.name = name;
            this.entityType = entityType;
        }

        void setFetchAllData(List<T> data) {
            this.fetchAllData = data != null ? new ArrayList<>(data) : Collections.emptyList();
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<T> getEntityType() { return entityType; }

        @Override
        public CompletableFuture<List<T>> fetchAll() {
            return CompletableFuture.completedFuture(new ArrayList<>(fetchAllData));
        }

        @Override
        public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return healthy; }

        @Override
        public StreamingDataSourceState getState() { return state; }

        @Override
        public void subscribe(BatchSnapshotEventListener<T> listener) {
            listeners.add(listener);
        }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<T> listener) {
            listeners.remove(listener);
        }

        @Override
        public void close() {
            listeners.clear();
        }

        @Override
        public Optional<com.thy.fss.common.inmemory.datasource.DataSource<T>> getFallbackDataSource() {
            return Optional.ofNullable(fallbackDataSource);
        }

        @Override
        public void setFallbackDataSource(com.thy.fss.common.inmemory.datasource.DataSource<T> fallbackDataSource) {
            this.fallbackDataSource = fallbackDataSource;
        }

        /**
         * Emits an initial load event (initialLoad=true) to all subscribers.
         */
        void emitInitialLoad(List<T> entities) {
            BatchSnapshotEvent<T> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            for (BatchSnapshotEventListener<T> listener : listeners) {
                listener.onBatchSnapshot(event);
            }
            state = StreamingDataSourceState.READY;
        }

        /**
         * Emits an incremental update event (initialLoad=false) to all subscribers.
         */
        void emitIncrementalUpdate(List<T> entities) {
            BatchSnapshotEvent<T> event = new BatchSnapshotEvent<>(
                    entities, Instant.now());
            for (BatchSnapshotEventListener<T> listener : listeners) {
                listener.onBatchSnapshot(event);
            }
        }
    }
}
