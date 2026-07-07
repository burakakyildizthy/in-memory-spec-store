package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Test enum with Jackson annotations for enum deserializer testing.
 */
public enum UserStatus {
    ACTIVE("A"),
    INACTIVE("I"),
    PENDING("P"),
    SUSPENDED("S");

    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    @JsonCreator
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}