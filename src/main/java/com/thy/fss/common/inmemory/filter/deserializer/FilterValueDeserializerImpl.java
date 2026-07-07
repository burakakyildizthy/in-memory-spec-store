package com.thy.fss.common.inmemory.filter.deserializer;

import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.filter.TemporalPreset;
import com.thy.fss.common.inmemory.filter.TemporalPresetParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * Implementation of FilterValueDeserializer that provides consistent value deserialization
 * logic for both JSON and Spring parameter binding.
 * Uses FilterConstants default patterns for temporal types when no custom formatter is provided.
 */
public class FilterValueDeserializerImpl implements FilterValueDeserializer {
    
    private static final String UNSUPPORTED_TEMP_TYPE = "Unsupported temporal type: ";

    @Override
    public <T> T deserializeValue(String value, Class<T> targetType, FieldDeserializationConfig config) {
        if (value == null) {
            return null;
        }

        // Handle temporal types
        if (isTemporalType(targetType)) {
            return deserializeTemporalType(value, targetType, config);
        }

        // Handle enum types
        if (targetType.isEnum()) {
            return deserializeEnumType(value, targetType, config);
        }

        // Handle numeric types
        if (isNumericType(targetType)) {
            return deserializeNumericType(value, targetType);
        }

        // Handle boolean type
        if (targetType == Boolean.class) {
            return targetType.cast(deserializeBoolean(value));
        }

        // Handle string type
        if (targetType == String.class) {
            return targetType.cast(deserializeString(value));
        }

        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }

    /**
     * Checks if the target type is a temporal type (LocalDateTime, LocalDate, or Instant).
     */
    private boolean isTemporalType(Class<?> targetType) {
        return targetType == LocalDateTime.class
                || targetType == LocalDate.class
                || targetType == Instant.class;
    }

    /**
     * Checks if the target type is a numeric type (Integer, Long, or Double).
     */
    private boolean isNumericType(Class<?> targetType) {
        return targetType == Integer.class
                || targetType == Long.class
                || targetType == Double.class;
    }

    /**
     * Deserializes temporal types with appropriate formatter.
     */
    private <T> T deserializeTemporalType(String value, Class<T> targetType, FieldDeserializationConfig config) {
        DateTimeFormatter formatter = getFormatterForTemporalType(targetType, config);

        if (targetType == LocalDateTime.class) {
            return targetType.cast(deserializeLocalDateTime(value, formatter));
        }

        if (targetType == LocalDate.class) {
            return targetType.cast(deserializeLocalDate(value, formatter));
        }

        if (targetType == Instant.class) {
            return targetType.cast(deserializeInstant(value, formatter));
        }

        throw new IllegalArgumentException(UNSUPPORTED_TEMP_TYPE + targetType.getName());
    }

    /**
     * Gets the appropriate DateTimeFormatter for the temporal type.
     */
    private DateTimeFormatter getFormatterForTemporalType(Class<?> targetType, FieldDeserializationConfig config) {
        // Use custom formatter if provided
        if (config != null && config.getDateTimeFormatter() != null) {
            return config.getDateTimeFormatter();
        }

        // Use default formatter based on type
        if (targetType == LocalDateTime.class) {
            return DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        }

        if (targetType == LocalDate.class) {
            return DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
        }

        if (targetType == Instant.class) {
            return DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_INSTANT_PATTERN);
        }

        throw new IllegalArgumentException(UNSUPPORTED_TEMP_TYPE + targetType.getName());
    }

    /**
     * Deserializes enum types with appropriate configuration.
     */
    private <T> T deserializeEnumType(String value, Class<T> targetType, FieldDeserializationConfig config) {
        @SuppressWarnings("unchecked")
        Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;

        EnumDeserializationInfo enumInfo = getEnumInfo(config);

        @SuppressWarnings("unchecked")
        T result = (T) deserializeEnum(value, enumType, enumInfo);
        return result;
    }

    /**
     * Gets enum deserialization info from config or creates default.
     */
    private EnumDeserializationInfo getEnumInfo(FieldDeserializationConfig config) {
        if (config != null && config.getEnumInfo() != null) {
            return config.getEnumInfo();
        }

        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo();
        enumInfo.setType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        return enumInfo;
    }

    /**
     * Deserializes numeric types (Integer, Long, Double).
     */
    private <T> T deserializeNumericType(String value, Class<T> targetType) {
        if (targetType == Integer.class) {
            return targetType.cast(deserializeInteger(value));
        }

        if (targetType == Long.class) {
            return targetType.cast(deserializeLong(value));
        }

        if (targetType == Double.class) {
            return targetType.cast(deserializeDouble(value));
        }

        throw new IllegalArgumentException("Unsupported numeric type: " + targetType.getName());
    }


    @Override
    public LocalDateTime deserializeLocalDateTime(String value, DateTimeFormatter formatter) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to LocalDateTime");
        }

        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse LocalDateTime from '%s'. Expected format: %s", value, formatter),
                    e
            );
        }
    }

    @Override
    public LocalDate deserializeLocalDate(String value, DateTimeFormatter formatter) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to LocalDate");
        }

        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse LocalDate from '%s'. Expected format: %s", value, formatter),
                    e
            );
        }
    }

    @Override
    public Instant deserializeInstant(String value, DateTimeFormatter formatter) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to Instant");
        }

        try {
            // Parse as ZonedDateTime first, then convert to Instant
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(value, formatter);
            return zonedDateTime.toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse Instant from '%s'. Expected format: %s", value, formatter),
                    e
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E deserializeEnum(String value, Class<E> enumType, EnumDeserializationInfo info) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to enum " + enumType.getSimpleName());
        }

        if (info == null) {
            info = new EnumDeserializationInfo();
            info.setType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        }

        try {
            switch (info.getType()) {
                case CREATOR_METHOD:
                    return invokeCreatorMethod(enumType, info.getJsonCreatorMethod(), value);

                case VALUE_FIELD:
                    return findByValueField(enumType, info.getJsonValueField(), value);

                case VALUE_METHOD:
                    return findByValueMethod(enumType, info.getJsonValueMethod(), value);

                case DEFAULT_MATCHING:
                default:
                    return parseEnumDefault(enumType, value);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse enum %s from value '%s'", enumType.getSimpleName(), value),
                    e
            );
        }
    }

    @Override
    public Integer deserializeInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to Integer");
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse Integer from '%s'", value),
                    e
            );
        }
    }

    @Override
    public Long deserializeLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to Long");
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse Long from '%s'", value),
                    e
            );
        }
    }

    @Override
    public Double deserializeDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to Double");
        }

        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse Double from '%s'", value),
                    e
            );
        }
    }

    @Override
    public String deserializeString(String value) {
        return value;
    }

    @Override
    public Boolean deserializeBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse null or empty string to Boolean");
        }

        String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed) || "1".equals(trimmed)) {
            return Boolean.TRUE;
        } else if ("false".equals(trimmed) || "0".equals(trimmed)) {
            return Boolean.FALSE;
        } else {
            throw new IllegalArgumentException(
                    String.format("Cannot parse Boolean from '%s'. Expected: true, false, 1, or 0", value)
            );
        }
    }

    @Override
    public TemporalPreset deserializeTemporalPreset(String value) {
        return TemporalPresetParser.parse(value);
    }

    /**
     * Invokes a static factory method annotated with @JsonCreator.
     */
    private <E extends Enum<E>> E invokeCreatorMethod(Class<E> enumType, String methodName, String value) {
        try {
            Method method = enumType.getDeclaredMethod(methodName, String.class);
            @SuppressWarnings("unchecked")
            E result = (E) method.invoke(null, value);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot invoke creator method %s on enum %s", methodName, enumType.getSimpleName()),
                    e
            );
        }
    }

    /**
     * Finds enum constant by matching a field value annotated with @JsonValue.
     */
    private <E extends Enum<E>> E findByValueField(Class<E> enumType, String fieldName, String value) {
        try {
            E[] constants = enumType.getEnumConstants();
            for (E constant : constants) {
                var field = enumType.getDeclaredField(fieldName);
                Object fieldValue = field.get(constant);
                if (fieldValue != null && fieldValue.toString().equals(value)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException(
                    String.format("No enum constant %s with field %s = '%s'", enumType.getSimpleName(), fieldName, value)
            );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot access field %s on enum %s", fieldName, enumType.getSimpleName()),
                    e
            );
        }
    }

    /**
     * Finds enum constant by matching a method return value annotated with @JsonValue.
     */
    private <E extends Enum<E>> E findByValueMethod(Class<E> enumType, String methodName, String value) {
        try {
            E[] constants = enumType.getEnumConstants();
            for (E constant : constants) {
                Method method = enumType.getDeclaredMethod(methodName);
                Object methodValue = method.invoke(constant);
                if (methodValue != null && methodValue.toString().equals(value)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException(
                    String.format("No enum constant %s with method %s() = '%s'", enumType.getSimpleName(), methodName, value)
            );
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot invoke method %s on enum %s", methodName, enumType.getSimpleName()),
                    e
            );
        }
    }

    /**
     * Parses enum using default name matching (case-insensitive).
     */
    private <E extends Enum<E>> E parseEnumDefault(Class<E> enumType, String value) {
        E[] constants = enumType.getEnumConstants();

        // Try exact match first
        for (E constant : constants) {
            if (constant.name().equals(value)) {
                return constant;
            }
        }

        // Try case-insensitive match
        for (E constant : constants) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }

        throw new IllegalArgumentException(
                String.format("No enum constant %s.%s. Available values: %s",
                        enumType.getSimpleName(),
                        value,
                        Arrays.toString(constants))
        );
    }
}
