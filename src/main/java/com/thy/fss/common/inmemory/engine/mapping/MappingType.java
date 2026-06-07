package com.thy.fss.common.inmemory.engine.mapping;

/**
 * Enumeration of property mapping types.
 * Defines how data from a datasource should be mapped to a target property.
 */
public enum MappingType {

    /**
     * One-to-one mapping: Maps a single entity or value to the target property.
     * Used when the relationship is 1:1 based on primary-foreign key.
     */
    ONE_TO_ONE,

    /**
     * Many-to-one collection mapping: Collects multiple entities into a collection.
     * Used when the relationship is 1:N and target property is a Collection type.
     */
    MANY_TO_ONE_COLLECTION,

    /**
     * Many-to-one aggregation mapping: Aggregates multiple values into a single result.
     * Used when the relationship is 1:N and target property is a numeric type.
     * Requires an aggregation type (SUM, AVG, COUNT, MIN, MAX).
     */
    MANY_TO_ONE_AGGREGATION,

    /**
     * Many-to-one ANY mapping: Checks if any source entity matches a condition.
     * Returns true if at least one source entity satisfies the specification.
     * Target property must be Boolean type.
     */
    MANY_TO_ONE_ANY,

    /**
     * Many-to-one ALL mapping: Checks if all source entities match a condition.
     * Returns true if all source entities satisfy the specification.
     * Target property must be Boolean type.
     */
    MANY_TO_ONE_ALL,

    /**
     * Many-to-one FIRST mapping: Gets a field value from the first source entity.
     * Returns the field value from the first entity matching the join condition.
     * Can be combined with where clause for filtering.
     */
    MANY_TO_ONE_FIRST,

    /**
     * Many-to-one LAST mapping: Gets a field value from the last source entity.
     * Returns the field value from the last entity matching the join condition.
     * Can be combined with where clause for filtering.
     */
    MANY_TO_ONE_LAST
}
