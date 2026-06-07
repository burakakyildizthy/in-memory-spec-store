package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for nested model fields.
 * Provides type-safe access to nested object fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 * @param <M> The model type (nested object type)
 */
public class ModelAttribute<T, M> extends MetaAttribute<T, M> {

    /**
     * Constructor for model attribute.
     *
     * @param name      The field name
     * @param ownerType The class that owns this field
     * @param modelType The type of the nested model
     */
    public ModelAttribute(String name, Class<T> ownerType, Class<M> modelType) {
        super(name, ownerType, modelType, AttributeType.MODEL);
    }
}