package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.entity.Identifiable;

/**
 * Functional interface for listening to batch snapshot events from streaming datasources.
 *
 * @param <T> entity type implementing Identifiable
 */
@FunctionalInterface
public interface BatchSnapshotEventListener<T extends Identifiable<?>> {

    /**
     * Called when a batch snapshot event is received from a streaming datasource.
     *
     * @param event the batch snapshot event containing entity snapshots
     */
    void onBatchSnapshot(BatchSnapshotEvent<T> event);
}
