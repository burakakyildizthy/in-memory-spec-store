package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.entity.Identifiable;
import net.jqwik.api.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for BatchSnapshotEvent empty list rejection and simplified structure.
 *
 * Feature: streaming-datasource-unification, Property 2: BatchSnapshotEvent Boş Liste Reddi ve Sadeleştirilmiş Yapı
 *
 * Validates: Requirements 4.1, 4.2
 */
class BatchSnapshotEventUnificationTest {

    // Feature: streaming-datasource-unification, Property 2: BatchSnapshotEvent Boş Liste Reddi ve Sadeleştirilmiş Yapı

    /**
     * Property 2: BatchSnapshotEvent Boş Liste Reddi ve Sadeleştirilmiş Yapı
     *
     * For any empty or null entity list, the constructor must throw IllegalArgumentException.
     * For any valid (non-empty) entity list, the event must contain only entities and timestamp fields.
     * getDataSourceName() and isInitialLoad() methods must NOT exist.
     *
     * Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    @Label("Feature: streaming-datasource-unification, Property 2: BatchSnapshotEvent Boş Liste Reddi ve Sadeleştirilmiş Yapı")
    void batchSnapshotEventShouldRejectEmptyListAndHaveSimplifiedStructure(
            @ForAll("nonEmptyEntityLists") List<TestEntity> validEntities) {

        Instant timestamp = Instant.now();

        // 1. Null list must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> new BatchSnapshotEvent<>(null, timestamp),
                "Null entity list must throw IllegalArgumentException");

        // 2. Empty list must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> new BatchSnapshotEvent<>(Collections.emptyList(), timestamp),
                "Empty entity list must throw IllegalArgumentException");

        // 3. Valid (non-empty) list must create event successfully
        BatchSnapshotEvent<TestEntity> event = new BatchSnapshotEvent<>(validEntities, timestamp);
        assertNotNull(event, "Event must be created successfully with non-empty list");
        assertEquals(validEntities.size(), event.getEntities().size(),
                "Event entities size must match input size");
        assertEquals(timestamp, event.getTimestamp(),
                "Event timestamp must match input timestamp");

        // 4. Verify only 'entities' and 'timestamp' public getters exist (simplified structure)
        Set<String> publicMethods = Arrays.stream(BatchSnapshotEvent.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(publicMethods.contains("getEntities"),
                "getEntities() must exist");
        assertTrue(publicMethods.contains("getTimestamp"),
                "getTimestamp() must exist");

        // 5. getDataSourceName() must NOT exist — datasource identity comes from subscription context
        assertFalse(publicMethods.contains("getDataSourceName"),
                "getDataSourceName() must NOT exist — datasource identity is known from subscription context");

        // 6. isInitialLoad() must NOT exist — initial state is managed via fetchAll()
        assertFalse(publicMethods.contains("isInitialLoad"),
                "isInitialLoad() must NOT exist — initial state is managed via fetchAll()");
    }

    @Provide
    Arbitrary<List<TestEntity>> nonEmptyEntityLists() {
        return Arbitraries.integers().between(1, 1000)
                .list().ofMinSize(1).ofMaxSize(20)
                .map(ids -> {
                    List<TestEntity> entities = new ArrayList<>();
                    for (Integer id : ids) {
                        entities.add(new TestEntity(id));
                    }
                    return entities;
                });
    }

    // --- Test double ---

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
}
