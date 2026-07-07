package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Double fields.
 * Provides type-safe access to Double fields in specifications and filters.
 * Extends {@link NumericMetaAttribute} to enable compile-time type safety for numeric operations.
 *
 * @param <T> The owner type (entity class)
 */
public class DoubleAttribute<T> extends NumericMetaAttribute<T, Double> {

    /**
     * Constructor for Double attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public DoubleAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, Double.class, AttributeType.SINGLE);
    }
}