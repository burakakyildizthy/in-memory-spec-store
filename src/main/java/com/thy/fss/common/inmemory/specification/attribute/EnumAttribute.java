package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Enum fields.
 * Provides type-safe access to Enum fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 * @param <E> The enum type
 */
public class EnumAttribute<T, E extends Enum<E>> extends MetaAttribute<T, E> {

    /**
     * Constructor for enum attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     * @param enumType  The enum type
     */
    public EnumAttribute(String name, Class<T> ownerType, Class<E> enumType) {
        super(name, ownerType, enumType, AttributeType.SINGLE);
    }
}