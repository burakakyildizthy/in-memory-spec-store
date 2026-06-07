package com.thy.fss.common.inmemory.processor.model;

import java.util.Objects;

/**
 * Data model representing enum deserialization configuration extracted from build-time analysis.
 * Contains information about Jackson annotations (@JsonCreator, @JsonValue) and deserialization strategy.
 */
public class EnumDeserializationInfo {

    private DeserializationType deserializationType;
    private String jsonCreatorMethod;
    private String jsonValueField;
    private String jsonValueMethod;
    private String enumClassName;

    public EnumDeserializationInfo() {
        this.deserializationType = DeserializationType.DEFAULT_MATCHING;
    }

    public EnumDeserializationInfo(String enumClassName) {
        this.enumClassName = enumClassName;
        this.deserializationType = DeserializationType.DEFAULT_MATCHING;
    }

    public DeserializationType getDeserializationType() {
        return deserializationType;
    }

    public void setDeserializationType(DeserializationType deserializationType) {
        this.deserializationType = deserializationType;
    }

    public String getJsonCreatorMethod() {
        return jsonCreatorMethod;
    }

    public void setJsonCreatorMethod(String jsonCreatorMethod) {
        this.jsonCreatorMethod = jsonCreatorMethod;
    }

    public String getJsonValueField() {
        return jsonValueField;
    }

    public void setJsonValueField(String jsonValueField) {
        this.jsonValueField = jsonValueField;
    }

    public String getJsonValueMethod() {
        return jsonValueMethod;
    }

    public void setJsonValueMethod(String jsonValueMethod) {
        this.jsonValueMethod = jsonValueMethod;
    }

    public String getEnumClassName() {
        return enumClassName;
    }

    public void setEnumClassName(String enumClassName) {
        this.enumClassName = enumClassName;
    }

    /**
     * Checks if this enum has custom Jackson deserialization configuration.
     *
     * @return true if @JsonCreator or @JsonValue annotations are present
     */
    public boolean hasCustomDeserialization() {
        return deserializationType != DeserializationType.DEFAULT_MATCHING;
    }

    /**
     * Gets the method or field name used for deserialization based on the strategy.
     *
     * @return method name, field name, or null for default matching
     */
    public String getDeserializationTarget() {
        return switch (deserializationType) {
            case CREATOR_METHOD -> jsonCreatorMethod;
            case VALUE_FIELD -> jsonValueField;
            case VALUE_METHOD -> jsonValueMethod;
            case DEFAULT_MATCHING -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumDeserializationInfo that = (EnumDeserializationInfo) o;
        return deserializationType == that.deserializationType &&
                Objects.equals(jsonCreatorMethod, that.jsonCreatorMethod) &&
                Objects.equals(jsonValueField, that.jsonValueField) &&
                Objects.equals(jsonValueMethod, that.jsonValueMethod) &&
                Objects.equals(enumClassName, that.enumClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deserializationType, jsonCreatorMethod, jsonValueField,
                jsonValueMethod, enumClassName);
    }

    @Override
    public String toString() {
        return "EnumDeserializationInfo{" +
                "deserializationType=" + deserializationType +
                ", jsonCreatorMethod='" + jsonCreatorMethod + '\'' +
                ", jsonValueField='" + jsonValueField + '\'' +
                ", jsonValueMethod='" + jsonValueMethod + '\'' +
                ", enumClassName='" + enumClassName + '\'' +
                '}';
    }

    /**
     * Enum deserialization strategy types based on Jackson annotations found during build-time analysis.
     */
    public enum DeserializationType {
        CREATOR_METHOD,    // Uses @JsonCreator method
        VALUE_FIELD,       // Uses @JsonValue field
        VALUE_METHOD,      // Uses @JsonValue method
        DEFAULT_MATCHING   // No Jackson annotations - uses valueOf() with fallback
    }
}