package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

import java.time.LocalDate;

/**
 * Meta attribute for LocalDate fields.
 * Provides type-safe access to LocalDate fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 */
public class LocalDateAttribute<T> extends MetaAttribute<T, LocalDate> {

    /**
     * Constructor for LocalDate attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public LocalDateAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, LocalDate.class, AttributeType.SINGLE);
    }
}