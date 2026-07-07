package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

import java.time.Instant;

/**
 * Meta attribute for Instant fields.
 * Provides type-safe access to Instant fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 */
public class InstantAttribute<T> extends MetaAttribute<T, Instant> {

    /**
     * Constructor for Instant attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     */
    public InstantAttribute(String name, Class<T> ownerType) {
        super(name, ownerType, Instant.class, AttributeType.SINGLE);
    }
}