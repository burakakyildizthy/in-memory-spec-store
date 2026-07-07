package com.thy.fss.common.inmemory.specification.attribute;

import java.util.Collection;

import com.thy.fss.common.inmemory.specification.AttributeType;

/**
 * Meta attribute for Collection fields.
 * Provides type-safe access to Collection fields in specifications and filters.
 *
 * @param <T> The owner type (entity class)
 * @param <E> The element type of the collection
 */
public class CollectionAttribute<T, E> extends MetaAttribute<T, Collection<E>> {
    private final Class<E> elementType;
    private final Class<?> collectionType;

    /**
     * Constructor for collection attribute.
     * Backward-compatible: defaults collectionType to {@code Collection.class}.
     *
     * @param name        The field name
     * @param ownerType   The class that owns this field
     * @param elementType The type of elements in the collection
     */
    @SuppressWarnings("unchecked")
    public CollectionAttribute(String name, Class<T> ownerType, Class<E> elementType) {
        this(name, ownerType, elementType, Collection.class);
    }

    /**
     * Constructor for collection attribute with explicit collection type.
     *
     * @param name           The field name
     * @param ownerType      The class that owns this field
     * @param elementType    The type of elements in the collection
     * @param collectionType The declared collection interface (e.g. List.class, Set.class)
     */
    @SuppressWarnings("unchecked")
    public CollectionAttribute(String name, Class<T> ownerType, Class<E> elementType, Class<?> collectionType) {
        super(name, ownerType, (Class<Collection<E>>) (Class<?>) Collection.class, AttributeType.COLLECTION);
        this.elementType = elementType;
        this.collectionType = collectionType != null ? collectionType : Collection.class;
    }

    /**
     * Gets the element type of the collection.
     *
     * @return The element type
     */
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Gets the declared collection type (e.g. List, Set, Collection).
     *
     * @return The collection type
     */
    public Class<?> getCollectionType() {
        return collectionType;
    }

    @Override
    public String toString() {
        return "CollectionAttribute{" +
                "name='" + getName() + '\'' +
                ", ownerType=" + getOwnerType().getSimpleName() +
                ", elementType=" + elementType.getSimpleName() +
                ", collectionType=" + collectionType.getSimpleName() +
                '}';
    }

    @Override
    public int hashCode() {
        int result = 31 * super.hashCode() + elementType.hashCode();
        result = 31 * result + collectionType.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionAttribute<?, ?> that)) return false;
        if (!super.equals(o)) return false;
        return elementType.equals(that.elementType)
                && collectionType.equals(that.collectionType);
    }
}