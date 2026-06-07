package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Abstract base class for numeric meta attributes.
 * This class extends {@link MetaAttribute} and provides compile-time type safety
 * for numeric operations like aggregations (SUM, AVG, MIN, MAX).
 *
 * <p>Subclasses:</p>
 * <ul>
 *   <li>{@link IntegerAttribute} - for Integer and int fields</li>
 *   <li>{@link LongAttribute} - for Long and long fields</li>
 *   <li>{@link DoubleAttribute} - for Double and double fields</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // This will compile - Order_.amount is a DoubleAttribute which extends NumericMetaAttribute
 * .sum(Order_.amount)
 *
 * // This will NOT compile - Order_.status is a StringAttribute which doesn't extend NumericMetaAttribute
 * .sum(Order_.status) // Compile error!
 * }</pre>
 *
 * @param <T> The owner type (entity class)
 * @param <N> The numeric field type (must extend Number)
 */
public abstract class NumericMetaAttribute<T, N extends Number> extends MetaAttribute<T, N> {

    /**
     * Constructor for numeric meta attribute.
     *
     * @param name          The field name
     * @param ownerType     The class that owns this field
     * @param fieldType     The type of the field (must be a Number type)
     * @param attributeType The category of this attribute
     */
    protected NumericMetaAttribute(String name, Class<T> ownerType, Class<N> fieldType, AttributeType attributeType) {
        super(name, ownerType, fieldType, attributeType);
    }
}
