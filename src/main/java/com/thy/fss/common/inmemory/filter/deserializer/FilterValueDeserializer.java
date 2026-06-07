package com.thy.fss.common.inmemory.filter.deserializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.thy.fss.common.inmemory.filter.TemporalPreset;

/**
 * Interface for deserializing filter field values from string representations.
 * Provides type-safe deserialization methods for all supported filter types.
 * This interface is used by both JSON deserializers and Spring parameter binding
 * to ensure consistent value conversion logic.
 */
public interface FilterValueDeserializer {

    /**
     * Deserializes a string value to the target type using the provided configuration.
     *
     * @param value      The string value to deserialize
     * @param targetType The target type class
     * @param config     The deserialization configuration
     * @param <T>        The target type
     * @return The deserialized value
     * @throws IllegalArgumentException if the value cannot be deserialized
     */
    <T> T deserializeValue(String value, Class<T> targetType, FieldDeserializationConfig config);

    /**
     * Deserializes a string to LocalDateTime using the specified formatter.
     *
     * @param value     The string value to deserialize
     * @param formatter The DateTimeFormatter to use
     * @return The deserialized LocalDateTime
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    LocalDateTime deserializeLocalDateTime(String value, DateTimeFormatter formatter);

    /**
     * Deserializes a string to LocalDate using the specified formatter.
     *
     * @param value     The string value to deserialize
     * @param formatter The DateTimeFormatter to use
     * @return The deserialized LocalDate
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    LocalDate deserializeLocalDate(String value, DateTimeFormatter formatter);

    /**
     * Deserializes a string to Instant using the specified formatter.
     *
     * @param value     The string value to deserialize
     * @param formatter The DateTimeFormatter to use
     * @return The deserialized Instant
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    Instant deserializeInstant(String value, DateTimeFormatter formatter);

    /**
     * Deserializes a string to an enum value using the provided deserialization info.
     *
     * @param value    The string value to deserialize
     * @param enumType The enum class
     * @param info     The enum deserialization information
     * @param <E>      The enum type
     * @return The deserialized enum value
     * @throws IllegalArgumentException if the value cannot be deserialized
     */
    <E extends Enum<E>> E deserializeEnum(String value, Class<E> enumType, EnumDeserializationInfo info);

    /**
     * Deserializes a string to Integer.
     *
     * @param value The string value to deserialize
     * @return The deserialized Integer
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    Integer deserializeInteger(String value);

    /**
     * Deserializes a string to Long.
     *
     * @param value The string value to deserialize
     * @return The deserialized Long
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    Long deserializeLong(String value);

    /**
     * Deserializes a string to Double.
     *
     * @param value The string value to deserialize
     * @return The deserialized Double
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    Double deserializeDouble(String value);

    /**
     * Deserializes a string value (returns as-is or with processing).
     *
     * @param value The string value to deserialize
     * @return The deserialized String
     */
    String deserializeString(String value);

    /**
     * Deserializes a string to Boolean.
     *
     * @param value The string value to deserialize
     * @return The deserialized Boolean
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    Boolean deserializeBoolean(String value);

    /**
     * Deserializes a string to TemporalPreset expression (e.g. 24h, 40m, 2M).
     *
     * @param value The string value to deserialize
     * @return The deserialized TemporalPreset
     */
    TemporalPreset deserializeTemporalPreset(String value);
}
