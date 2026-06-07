package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Base class for all meta attributes.
 * Provides type-safe field references for use in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 * @param <F> The field type
 */
public abstract class MetaAttribute<T, F> {
    protected final String name;
    protected final Class<T> ownerType;
    protected final Class<F> fieldType;
    protected final AttributeType attributeType;

    /**
     * Constructor for meta attribute.
     *
     * @param name          The field name
     * @param ownerType     The class that owns this field
     * @param fieldType     The type of the field
     * @param attributeType The category of this attribute
     */
    public MetaAttribute(String name, Class<T> ownerType, Class<F> fieldType, AttributeType attributeType) {
        this.name = name;
        this.ownerType = ownerType;
        this.fieldType = fieldType;
        this.attributeType = attributeType;
    }

    /**
     * Gets the field name.
     *
     * @return The field name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the owner type.
     *
     * @return The class that owns this field
     */
    public Class<T> getOwnerType() {
        return ownerType;
    }

    /**
     * Gets the field type.
     *
     * @return The type of the field
     */
    public Class<F> getFieldType() {
        return fieldType;
    }

    /**
     * Gets the attribute type category.
     *
     * @return The attribute type
     */
    public AttributeType getAttributeType() {
        return attributeType;
    }

    @Override
    public String toString() {
        return String.format("%s.%s", ownerType.getSimpleName(), name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MetaAttribute<?, ?> that = (MetaAttribute<?, ?>) obj;
        return name.equals(that.name) &&
                ownerType.equals(that.ownerType) &&
                fieldType.equals(that.fieldType);
    }

    @Override
    public int hashCode() {
        return name.hashCode() + ownerType.hashCode() + fieldType.hashCode();
    }
}