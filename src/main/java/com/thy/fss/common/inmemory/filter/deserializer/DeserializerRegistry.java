package com.thy.fss.common.inmemory.filter.deserializer;

import java.util.Set;

/**
 * Registry interface for caching and providing deserialization configurations.
 * Implementations should extract and cache configurations from existing JSON deserializers
 * at startup to ensure consistent deserialization logic across JSON and parameter binding.
 */
public interface DeserializerRegistry {

    /**
     * Gets the deserialization configuration for the specified type.
     *
     * @param type The type to get configuration for
     * @return The field deserialization configuration
     */
    FieldDeserializationConfig getConfigForType(Class<?> type);

    /**
     * Gets all known enum types that have been registered.
     *
     * @return Set of enum classes
     */
    Set<Class<? extends Enum<?>>> getKnownEnumTypes();

    /**
     * Registers a custom configuration for a specific type.
     * This allows applications to override default deserialization behavior.
     *
     * @param type   The type to register configuration for
     * @param config The configuration to use
     */
    void registerConfig(Class<?> type, FieldDeserializationConfig config);
}
