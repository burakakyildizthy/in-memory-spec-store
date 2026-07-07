package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Boolean fields.
 * Provides type-safe access to Boolean fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 */
public class BooleanAttribute<T> extends MetaAttribute<T, Boolean> {

    /**
     * Constructor for Boolean attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public BooleanAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, Boolean.class, AttributeType.SINGLE);
    }
}