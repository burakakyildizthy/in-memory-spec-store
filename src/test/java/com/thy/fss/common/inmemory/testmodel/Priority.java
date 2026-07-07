package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Test enum with @JsonValue field for enum deserializer testing.
 */
public enum Priority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    @JsonValue
    public final String value;

    Priority(String value) {
        this.value = value;
    }
}