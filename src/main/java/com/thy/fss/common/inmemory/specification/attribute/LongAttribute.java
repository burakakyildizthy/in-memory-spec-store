package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Long fields.
 * Provides type-safe access to Long fields in specifications and filters.
 * Extends {@link NumericMetaAttribute} to enable compile-time type safety for numeric operations.
 *
 * @param <T> The owner type (entity class)
 */
public class LongAttribute<T> extends NumericMetaAttribute<T, Long> {

    /**
     * Constructor for Long attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public LongAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, Long.class, AttributeType.SINGLE);
    }
}