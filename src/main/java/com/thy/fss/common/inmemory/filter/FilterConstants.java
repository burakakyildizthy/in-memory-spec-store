package com.thy.fss.common.inmemory.filter;

/**
 * Shared constants for filter deserialization and processing.
 * Contains field names, default format patterns, and performance optimization constants
 * used across all filter types and deserializers.
 */
public final class FilterConstants {

    // Common filter field names (used across all filter types)
    public static final String FIELD_EQ = "eq";
    public static final String FIELD_NEQ = "neq";
    public static final String FIELD_IN = "in";
    public static final String FIELD_NIN = "nin";
    public static final String FIELD_ISN = "isn";
    public static final String FIELD_ISNN = "isnn";

    // Range filter field names (used in numeric and temporal filters)
    public static final String FIELD_GT = "gt";
    public static final String FIELD_GTE = "gte";
    public static final String FIELD_LT = "lt";
    public static final String FIELD_LTE = "lte";

    // String filter specific field names
    public static final String FIELD_CONT = "cont";
    public static final String FIELD_START = "start";
    public static final String FIELD_END = "end";
    public static final String FIELD_MATCH = "match";
    public static final String FIELD_EMPTY = "empty";
    public static final String FIELD_NEMPTY = "nempty";
    public static final String FIELD_BLANK = "blank";
    public static final String FIELD_NBLANK = "nblank";

    // Collection filter specific field names (nested operators)
    public static final String FIELD_ANY = "any";
    public static final String FIELD_ALL = "all";
    public static final String FIELD_NONE = "none";

    // Temporal filter specific field names
    public static final String FIELD_BEFORE = "be";
    public static final String FIELD_AFTER = "af";
    public static final String FIELD_ON_OR_BEFORE = "obe";
    public static final String FIELD_ON_OR_AFTER = "oaf";
    public static final String FIELD_LAST = "last";
    public static final String FIELD_NEXT = "next";

    // Performance: Pre-sized collections based on common usage patterns
    public static final int TYPICAL_IN_SIZE = 4;
    public static final int TYPICAL_STRING_SIZE = 16;

    // Default datetime format patterns (used when no @JsonFormat annotation present)
    public static final String DEFAULT_LOCAL_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_LOCAL_DATE_PATTERN = "yyyy-MM-dd";
    public static final String DEFAULT_INSTANT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";

    private FilterConstants() {
        // Utility class - prevent instantiation
    }
}