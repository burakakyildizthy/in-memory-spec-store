package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Integer fields.
 * Provides type-safe access to Integer fields in specifications and filters.
 * Extends {@link NumericMetaAttribute} to enable compile-time type safety for numeric operations.
 *
 * @param <T> The owner type (entity class)
 */
public class IntegerAttribute<T> extends NumericMetaAttribute<T, Integer> {

    /**
     * Constructor for Integer attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public IntegerAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, Integer.class, AttributeType.SINGLE);
    }
}