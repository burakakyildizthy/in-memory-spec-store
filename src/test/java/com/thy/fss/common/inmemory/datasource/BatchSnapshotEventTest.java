package com.thy.fss.common.inmemory.datasource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thy.fss.common.inmemory.entity.Identifiable;

import net.jqwik.api.Property;

/**
 * Property-based tests for {@link BatchSnapshotEvent}.
 */
class BatchSnapshotEventTest {

    // Feature: streaming-datasource-support, Property 1: BatchSnapshotEvent Boş Liste Reddi

    /**
     * Property 1: For any null List<T> used to create a BatchSnapshotEvent,
     * the constructor must throw IllegalArgumentException and the object must not be created.
     *
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void nullEntityListShouldBeRejected() {
        assertThatThrownBy(() ->
                new BatchSnapshotEvent<>(null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Property 1: For any empty List<T> used to create a BatchSnapshotEvent,
     * the constructor must throw IllegalArgumentException and the object must not be created.
     *
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void emptyEntityListShouldBeRejected() {
        List<TestEntity> emptyList = Collections.emptyList();

        assertThatThrownBy(() ->
                new BatchSnapshotEvent<>(emptyList, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Simple test entity implementing Identifiable for property tests.
     */
    private static class TestEntity implements Identifiable<String> {
        private final String id;

        TestEntity(String id) {
            this.id = id;
        }

        @Override
        public String getIdentity() {
            return id;
        }
    }
}
