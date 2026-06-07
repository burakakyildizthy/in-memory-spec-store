package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.entity.Identifiable;
import net.jqwik.api.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for StreamingDataSource / DataSource type hierarchy.
 *
 * Feature: streaming-datasource-unification, Property 1: StreamingDataSource, DataSource Tip Hiyerarşisi
 *
 * Validates: Requirements 1.1, 1.3
 */
class StreamingDataSourceHierarchyTest {

    // Feature: streaming-datasource-unification, Property 1: StreamingDataSource, DataSource Tip Hiyerarşisi

    /**
     * Property 1: StreamingDataSource, DataSource Tip Hiyerarşisi
     *
     * For any StreamingDataSource implementation, the object must also be an instance of DataSource
     * (instanceof DataSource returns true) and fetchAll(), getName(), getEntityType(), isHealthy(),
     * close() methods must be accessible.
     *
     * Validates: Requirements 1.1, 1.3
     */
    @Property(tries = 100)
    @Label("Feature: streaming-datasource-unification, Property 1: StreamingDataSource, DataSource Tip Hiyerarşisi")
    void streamingDataSourceShouldBeInstanceOfDataSourceWithAccessibleMethods(
            @ForAll("streamingDataSources") StreamingDataSource<TestEntity> streamingDs) {

        // 1. instanceof DataSource must be true
        assertTrue(streamingDs instanceof DataSource,
                "StreamingDataSource must be instanceof DataSource");

        // 2. DataSource reference should work via upcast
        DataSource<TestEntity> dataSource = streamingDs;
        assertNotNull(dataSource);

        // 3. Verify fetchAll() is accessible and returns a non-null CompletableFuture
        CompletableFuture<List<TestEntity>> future = dataSource.fetchAll();
        assertNotNull(future, "fetchAll() must return a non-null CompletableFuture");

        // 4. Verify getName() is accessible and returns a non-null value
        String name = dataSource.getName();
        assertNotNull(name, "getName() must return a non-null value");

        // 5. Verify getEntityType() is accessible and returns the correct type
        Class<TestEntity> entityType = dataSource.getEntityType();
        assertNotNull(entityType, "getEntityType() must return a non-null value");
        assertEquals(TestEntity.class, entityType);

        // 6. Verify isHealthy() is accessible (returns boolean, no exception)
        boolean healthy = dataSource.isHealthy();
        // Just verify it doesn't throw — value can be true or false

        // 7. Verify close() is accessible (no exception)
        assertDoesNotThrow(() -> dataSource.close(),
                "close() must be callable without exception");

        // 8. Verify streaming-specific methods are also accessible
        assertNotNull(streamingDs.getState(),
                "getState() must return a non-null StreamingDataSourceState");
    }

    // Feature: streaming-datasource-unification, Property 15: Streaming DataSource Subscribe/Event Delivery

    /**
     * Property 15: Streaming DataSource Subscribe/Event Delivery
     *
     * For any sequence of subscribe/unsubscribe operations, events emitted after subscribe()
     * must be delivered to the listener, and events emitted after unsubscribe() must NOT be
     * delivered. Multiple subscribe/unsubscribe cycles and multiple simultaneous listeners
     * must work correctly.
     *
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    @Label("Feature: streaming-datasource-unification, Property 15: Streaming DataSource Subscribe/Event Delivery")
    void subscribeUnsubscribeEventDeliveryShouldBeCorrect(
            @ForAll("entityListsForEvents") List<TestEntity> entities) {

        EventTrackingStreamingDataSource ds = new EventTrackingStreamingDataSource("test-ds");
        BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(entities, Instant.now());

        // 1. After subscribe(), listener receives events
        List<BatchSnapshotEvent<TestEntity>> received1 = new ArrayList<>();
        BatchSnapshotEventListener<TestEntity> listener1 = received1::add;

        ds.subscribe(listener1);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "Subscribed listener must receive the event");
        assertSame(event, received1.get(0), "Listener must receive the exact event emitted");

        // 2. After unsubscribe(), listener no longer receives events
        ds.unsubscribe(listener1);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "Unsubscribed listener must NOT receive further events");

        // 3. Multiple subscribe/unsubscribe cycles work correctly
        received1.clear();
        ds.subscribe(listener1);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "Re-subscribed listener must receive events again");

        ds.unsubscribe(listener1);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "Re-unsubscribed listener must NOT receive further events");

        // 4. Multiple listeners can be subscribed simultaneously
        received1.clear();
        List<BatchSnapshotEvent<TestEntity>> received2 = new ArrayList<>();
        BatchSnapshotEventListener<TestEntity> listener2 = received2::add;

        ds.subscribe(listener1);
        ds.subscribe(listener2);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "First listener must receive the event");
        assertEquals(1, received2.size(), "Second listener must receive the event");

        // Unsubscribe only one — the other should still receive
        ds.unsubscribe(listener1);
        ds.emitEvent(event);
        assertEquals(1, received1.size(), "Unsubscribed first listener must NOT receive further events");
        assertEquals(2, received2.size(), "Still-subscribed second listener must receive the event");

        ds.unsubscribe(listener2);
    }

    @Provide
    Arbitrary<List<TestEntity>> entityListsForEvents() {
        return Arbitraries.integers().between(1, 10)
                .flatMap(size -> Arbitraries.integers().between(1, 10000)
                        .list().ofSize(size)
                        .map(ids -> {
                            List<TestEntity> entities = new ArrayList<>();
                            for (Integer id : ids) {
                                entities.add(new TestEntity(id));
                            }
                            return entities;
                        }));
    }

    @Provide
    Arbitrary<StreamingDataSource<TestEntity>> streamingDataSources() {
        Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(20);
        Arbitrary<Boolean> healthyFlags = Arbitraries.of(true, false);
        Arbitrary<StreamingDataSourceState> states = Arbitraries.of(StreamingDataSourceState.values());
        Arbitrary<List<TestEntity>> entityLists = Arbitraries.integers()
                .between(0, 10)
                .flatMap(size -> Arbitraries.integers().between(1, 10000)
                        .list().ofSize(size)
                        .map(ids -> {
                            List<TestEntity> entities = new ArrayList<>();
                            for (Integer id : ids) {
                                entities.add(new TestEntity(id));
                            }
                            return entities;
                        }));

        return Combinators.combine(names, healthyFlags, states, entityLists)
                .as(StubStreamingDataSource::new);
    }

    // --- Test doubles ---

    static class TestEntity implements Identifiable<Integer> {
        private final int id;

        TestEntity(int id) {
            this.id = id;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }
    }

    static class StubStreamingDataSource implements StreamingDataSource<TestEntity> {
        private final String name;
        private final boolean healthy;
        private final StreamingDataSourceState state;
        private final List<TestEntity> entities;
        private DataSource<TestEntity> fallback;

        StubStreamingDataSource(String name, boolean healthy,
                                StreamingDataSourceState state, List<TestEntity> entities) {
            this.name = name;
            this.healthy = healthy;
            this.state = state;
            this.entities = entities;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<TestEntity> getEntityType() { return TestEntity.class; }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.unmodifiableList(entities));
        }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return healthy; }

        @Override
        public void close() { /* no-op for test stub */ }

        @Override
        public Optional<DataSource<TestEntity>> getFallbackDataSource() {
            return Optional.ofNullable(fallback);
        }

        @Override
        public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) {
            this.fallback = fallbackDataSource;
        }

        @Override
        public StreamingDataSourceState getState() { return state; }

        @Override
        public void subscribe(BatchSnapshotEventListener<TestEntity> listener) { /* no-op */ }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<TestEntity> listener) { /* no-op */ }
    }

    /**
     * A streaming datasource stub that tracks listeners and can emit events to them.
     * Used by Property 15 to test subscribe/unsubscribe event delivery.
     */
    static class EventTrackingStreamingDataSource implements StreamingDataSource<TestEntity> {
        private final String name;
        private final CopyOnWriteArrayList<BatchSnapshotEventListener<TestEntity>> listeners = new CopyOnWriteArrayList<>();

        EventTrackingStreamingDataSource(String name) {
            this.name = name;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<TestEntity> getEntityType() { return TestEntity.class; }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<TestEntity>> getFallbackDataSource() { return Optional.empty(); }

        @Override
        public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<TestEntity> listener) {
            listeners.add(listener);
        }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<TestEntity> listener) {
            listeners.remove(listener);
        }

        void emitEvent(BatchSnapshotEvent<TestEntity> event) {
            for (BatchSnapshotEventListener<TestEntity> listener : listeners) {
                listener.onBatchSnapshot(event);
            }
        }
    }
}
