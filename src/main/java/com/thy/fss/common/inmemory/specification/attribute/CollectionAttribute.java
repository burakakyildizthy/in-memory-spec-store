package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.specification.AttributeType;

import java.util.Collection;

/**
 * Meta attribute for Collection fields.
 * Provides type-safe access to Collection fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 * @param <E> The element type of the collection
 */
public class CollectionAttribute<T, E> extends MetaAttribute<T, Collection<E>> {
    private final Class<E> elementType;

    /**
     * Constructor for collection attribute.
     *
     * @param name        The field name
     * @param ownerType   The class that owns this field
     * @param elementType The type of elements in the collection
     */
    @SuppressWarnings("unchecked")
    public CollectionAttribute(String name, Class<T> ownerType, Class<E> elementType) {
        super(name, ownerType, (Class<Collection<E>>) (Class<?>) Collection.class, AttributeType.COLLECTION);
        this.elementType = elementType;
    }

    /**
     * Gets the element type of the collection.
     *
     * @return The element type
     */
    public Class<E> getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "CollectionAttribute{" +
                "name='" + getName() + '\'' +
                ", ownerType=" + getOwnerType().getSimpleName() +
                ", elementType=" + elementType.getSimpleName() +
                '}';
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + elementType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionAttribute<?, ?> that)) return false;
        if (!super.equals(o)) return false;
        return elementType.equals(that.elementType);
    }
}