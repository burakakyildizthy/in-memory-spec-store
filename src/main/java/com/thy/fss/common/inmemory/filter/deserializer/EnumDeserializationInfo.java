package com.thy.fss.common.inmemory.filter.deserializer;

/**
 * Information about how to deserialize enum values.
 * Supports different deserialization strategies based on Jackson annotations.
 */
public class EnumDeserializationInfo {

    private DeserializationType type;
    private String jsonCreatorMethod;
    private String jsonValueField;
    private String jsonValueMethod;

    /**
     * Default constructor.
     */
    public EnumDeserializationInfo() {
        this.type = DeserializationType.DEFAULT_MATCHING;
    }

    /**
     * Gets the deserialization type.
     *
     * @return The deserialization type
     */
    public DeserializationType getType() {
        return type;
    }

    /**
     * Sets the deserialization type.
     *
     * @param type The deserialization type
     * @return This info instance for method chaining
     */
    public EnumDeserializationInfo setType(DeserializationType type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the name of the @JsonCreator annotated method.
     *
     * @return The method name, or null if not applicable
     */
    public String getJsonCreatorMethod() {
        return jsonCreatorMethod;
    }

    /**
     * Sets the name of the @JsonCreator annotated method.
     *
     * @param jsonCreatorMethod The method name
     * @return This info instance for method chaining
     */
    public EnumDeserializationInfo setJsonCreatorMethod(String jsonCreatorMethod) {
        this.jsonCreatorMethod = jsonCreatorMethod;
        return this;
    }

    /**
     * Gets the name of the @JsonValue annotated field.
     *
     * @return The field name, or null if not applicable
     */
    public String getJsonValueField() {
        return jsonValueField;
    }

    /**
     * Sets the name of the @JsonValue annotated field.
     *
     * @param jsonValueField The field name
     * @return This info instance for method chaining
     */
    public EnumDeserializationInfo setJsonValueField(String jsonValueField) {
        this.jsonValueField = jsonValueField;
        return this;
    }

    /**
     * Gets the name of the @JsonValue annotated method.
     *
     * @return The method name, or null if not applicable
     */
    public String getJsonValueMethod() {
        return jsonValueMethod;
    }

    /**
     * Sets the name of the @JsonValue annotated method.
     *
     * @param jsonValueMethod The method name
     * @return This info instance for method chaining
     */
    public EnumDeserializationInfo setJsonValueMethod(String jsonValueMethod) {
        this.jsonValueMethod = jsonValueMethod;
        return this;
    }

    /**
     * Enum deserialization type strategies.
     */
    public enum DeserializationType {
        /**
         * Use a static factory method annotated with @JsonCreator.
         */
        CREATOR_METHOD,

        /**
         * Use a field annotated with @JsonValue.
         */
        VALUE_FIELD,

        /**
         * Use a method annotated with @JsonValue.
         */
        VALUE_METHOD,

        /**
         * Use default enum name matching.
         */
        DEFAULT_MATCHING
    }
}
