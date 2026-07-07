package com.thy.fss.common.inmemory.specification;

/**
 * Enum defining the types of attributes in the meta model.
 * Used to categorize fields for appropriate handling in specifications and filters.
 */
public enum AttributeType {
    /**
     * Single value attributes (String, Integer, LocalDate, etc.)
     */
    SINGLE,

    /**
     * Collection attributes (List, Set, etc.)
     */
    COLLECTION,

    /**
     * Model attributes (nested objects)
     */
    MODEL,

    /**
     * Enum attributes
     */
    ENUM
}