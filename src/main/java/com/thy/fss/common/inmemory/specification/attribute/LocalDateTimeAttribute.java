package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

import java.time.LocalDateTime;

/**
 * Meta attribute for LocalDateTime fields.
 * Provides type-safe access to LocalDateTime fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 */
public class LocalDateTimeAttribute<T> extends MetaAttribute<T, LocalDateTime> {

    /**
     * Constructor for LocalDateTime attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public LocalDateTimeAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, LocalDateTime.class, AttributeType.SINGLE);
    }
}