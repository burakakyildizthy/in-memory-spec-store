package com.thy.fss.common.inmemory.datasource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thy.fss.common.inmemory.entity.Identifiable;

/**
 * Represents a batch snapshot event from a streaming datasource.
 * Contains one or more entity snapshots that are treated as a single atomic update unit.
 * Entity IDs are obtained via {@link Identifiable#getIdentity()}.
 * <p>
 * Datasource identity is known from the subscription context (closure-captured dsName),
 * not carried within the event itself.
 *
 * @param <T> entity type implementing Identifiable
 */
public class BatchSnapshotEvent<T extends Identifiable<?>> {

    private final List<T> entities;
    private final Instant timestamp;

    /**
     * Creates a new BatchSnapshotEvent.
     *
     * @param entities  the entity list — must contain at least 1 element
     * @param timestamp the batch timestamp
     * @throws IllegalArgumentException if entities is null or empty
     */
    public BatchSnapshotEvent(List<T> entities, Instant timestamp) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entity list must contain at least one element");
        }
        this.entities = Collections.unmodifiableList(new ArrayList<>(entities));
        this.timestamp = timestamp;
    }

    public List<T> getEntities() {
        return entities;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
