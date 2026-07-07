package com.thy.fss.common.inmemory.config;

/**
 * Enumeration of supported aggregation types for property data source configurations.
 * Used to specify how related data should be aggregated when populating object properties.
 */
public enum AggregationType {
    /**
     * Count the number of related items.
     */
    COUNT,

    /**
     * Sum numeric values from a specific field.
     */
    SUM,

    /**
     * Calculate average of numeric values from a specific field.
     */
    AVG,

    /**
     * Find minimum value from a specific field.
     */
    MIN,

    /**
     * Find maximum value from a specific field.
     */
    MAX,

    /**
     * Use a custom aggregation function.
     */
    CUSTOM
}