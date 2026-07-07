package com.thy.fss.common.inmemory.specification;

/**
 * Enum defining all supported operators for specifications and filters.
 * These operators are used in conjunction with meta attributes to create type-safe queries.
 */
public enum Operator {
    // Basic comparison operators
    EQUALS,
    NOT_EQUALS,

    // Numeric comparison operators
    GREATER_THAN,
    LESS_THAN,
    GREATER_OR_EQUAL_THAN,
    LESS_OR_EQUAL_THAN,

    // Numeric negated comparison operators
    NOT_GREATER_THAN,
    NOT_LESS_THAN,
    NOT_GREATER_OR_EQUAL_THAN,
    NOT_LESS_OR_EQUAL_THAN,

    // String operators
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES,
    IS_EMPTY,
    IS_NOT_EMPTY,
    IS_BLANK,
    IS_NOT_BLANK,

    // Collection operators
    COLLECTION_CONTAINS,
    COLLECTION_ANY,
    COLLECTION_ALL,
    COLLECTION_NONE,

    // Temporal operators
    IS_BEFORE,
    IS_AFTER,
    IS_ON_OR_BEFORE,
    IS_ON_OR_AFTER,
    LAST,
    NEXT,

    // Temporal negated operators
    NOT_IS_BEFORE,
    NOT_IS_AFTER,
    NOT_IS_ON_OR_BEFORE,
    NOT_IS_ON_OR_AFTER,

    // Null checking operators
    IS_NULL,
    IS_NOT_NULL,

    // List operators
    IN,
    NOT_IN
}