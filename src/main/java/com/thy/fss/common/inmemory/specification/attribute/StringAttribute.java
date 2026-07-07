package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for String fields.
 * Provides type-safe access to String fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 */
public class StringAttribute<T> extends MetaAttribute<T, String> {

    /**
     * Constructor for String attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public StringAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, String.class, AttributeType.SINGLE);
    }
}