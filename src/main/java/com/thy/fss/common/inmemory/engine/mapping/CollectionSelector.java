package com.thy.fss.common.inmemory.engine.mapping;

/**
 * Enum representing collection selection operations.
 * Used to specify which element(s) to extract from a collection during path traversal.
 */
public enum CollectionSelector {
    /**
     * Select all elements from the collection.
     */
    ALL,

    /**
     * Select the first element from the collection.
     */
    FIRST,

    /**
     * Select the last element from the collection.
     */
    LAST,

    /**
     * Select any element from the collection (typically used for existence checks).
     */
    ANY
}
