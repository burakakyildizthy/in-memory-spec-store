package com.thy.fss.common.inmemory.filter.deserializer;

import com.thy.fss.common.inmemory.filter.FilterConstants;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of DeserializerRegistry that caches and provides deserialization configurations.
 * Configurations are initialized with default values from FilterConstants and can be customized
 * by applications as needed.
 */
public class DeserializerRegistryImpl implements DeserializerRegistry {

    private final Map<Class<?>, FieldDeserializationConfig> configCache = new ConcurrentHashMap<>();
    private final Map<Class<? extends Enum<?>>, EnumDeserializationInfo> enumInfoCache = new ConcurrentHashMap<>();

    /**
     * Default constructor that initializes default configurations.
     */
    public DeserializerRegistryImpl() {
        initializeDefaultConfigurations();
    }

    @Override
    public FieldDeserializationConfig getConfigForType(Class<?> type) {
        return configCache.computeIfAbsent(type, this::createDefaultConfig);
    }

    @Override
    public Set<Class<? extends Enum<?>>> getKnownEnumTypes() {
        return enumInfoCache.keySet();
    }

    @Override
    public void registerConfig(Class<?> type, FieldDeserializationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        configCache.put(type, config);
    }

    /**
     * Registers enum deserialization information for a specific enum type.
     *
     * @param enumType The enum class
     * @param info     The deserialization information
     */
    public void registerEnumInfo(Class<? extends Enum<?>> enumType, EnumDeserializationInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("EnumDeserializationInfo cannot be null");
        }
        enumInfoCache.put(enumType, info);

        // Also create and cache a config for this enum type
        FieldDeserializationConfig config = new FieldDeserializationConfig();
        config.setTargetType(enumType);
        config.setEnumInfo(info);
        configCache.put(enumType, config);
    }

    /**
     * Initializes default configurations for common types.
     */
    private void initializeDefaultConfigurations() {
        // LocalDateTime configuration
        FieldDeserializationConfig localDateTimeConfig = new FieldDeserializationConfig();
        localDateTimeConfig.setTargetType(LocalDateTime.class);
        localDateTimeConfig.setDateTimeFormatter(
                DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN)
        );
        configCache.put(LocalDateTime.class, localDateTimeConfig);

        // LocalDate configuration
        FieldDeserializationConfig localDateConfig = new FieldDeserializationConfig();
        localDateConfig.setTargetType(LocalDate.class);
        localDateConfig.setDateTimeFormatter(
                DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN)
        );
        configCache.put(LocalDate.class, localDateConfig);

        // Instant configuration
        FieldDeserializationConfig instantConfig = new FieldDeserializationConfig();
        instantConfig.setTargetType(Instant.class);
        instantConfig.setDateTimeFormatter(
                DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_INSTANT_PATTERN)
        );
        configCache.put(Instant.class, instantConfig);

        // Numeric types - no special configuration needed, but cache for consistency
        configCache.put(Integer.class, createSimpleConfig(Integer.class));
        configCache.put(Long.class, createSimpleConfig(Long.class));
        configCache.put(Double.class, createSimpleConfig(Double.class));

        // String type
        configCache.put(String.class, createSimpleConfig(String.class));

        // Boolean type
        configCache.put(Boolean.class, createSimpleConfig(Boolean.class));
    }

    /**
     * Creates a default configuration for the given type.
     */
    private FieldDeserializationConfig createDefaultConfig(Class<?> type) {
        FieldDeserializationConfig config = new FieldDeserializationConfig();
        config.setTargetType(type);

        // Handle temporal types with default formatters
        if (type == LocalDateTime.class) {
            config.setDateTimeFormatter(
                    DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN)
            );
        } else if (type == LocalDate.class) {
            config.setDateTimeFormatter(
                    DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN)
            );
        } else if (type == Instant.class) {
            config.setDateTimeFormatter(
                    DateTimeFormatter.ofPattern(FilterConstants.DEFAULT_INSTANT_PATTERN)
            );
        } else if (type.isEnum()) {
            // For enum types, check if we have cached info, otherwise use default
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
            EnumDeserializationInfo enumInfo = enumInfoCache.get(enumType);
            if (enumInfo == null) {
                enumInfo = new EnumDeserializationInfo();
                enumInfo.setType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
            }
            config.setEnumInfo(enumInfo);
        }

        return config;
    }

    /**
     * Creates a simple configuration for types that don't need special handling.
     */
    private FieldDeserializationConfig createSimpleConfig(Class<?> type) {
        FieldDeserializationConfig config = new FieldDeserializationConfig();
        config.setTargetType(type);
        return config;
    }
}
