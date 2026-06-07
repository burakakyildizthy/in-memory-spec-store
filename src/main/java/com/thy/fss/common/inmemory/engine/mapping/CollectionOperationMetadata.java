package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;

import java.util.Comparator;
import java.util.Objects;

/**
 * Metadata for a collection operation at a specific position in a field path.
 * Stored separately from the path to maintain path performance
 * (List&lt;MetaAttribute&gt;).
 *
 * <p>
 * The pathIndex indicates which position in the path this collection operation
 * applies to. For example:</p>
 * <pre>
 *   Path: [User_.orders, Order_.items, Item_.price]
 *   CollectionOps: [
 *     {pathIndex=0, attribute=orders, selector=FIRST},
 *     {pathIndex=1, attribute=items, selector=ALL}
 *   ]
 * </pre>
 * <p>
 * This means: navigate to User.orders, take FIRST order, then navigate to
 * Order.items, take ALL items, then get Item.price</p>
 *
 * @param <O> The type that owns the collection field
 * @param <E> The element type of the collection
 */
public class CollectionOperationMetadata<O, E> {

    private final int pathIndex;
    private final CollectionAttribute<O, E> collectionAttribute;
    private final CollectionSelector selector;
    private final Specification<E> specification;
    private final Comparator<E> comparator;

    /**
     * Constructor for collection operation metadata. NOTE: pathIndex is
     * calculated automatically by the builder, not provided by the user.
     *
     * @param pathIndex Index in the path where this operation applies
     * (calculated by builder)
     * @param collectionAttribute The collection attribute (type-safe)
     * @param selector The collection selector
     * @param specification The specification for filtering (can be null)
     * @throws IllegalArgumentException if pathIndex is negative,
     * collectionAttribute is null, or selector is null
     */
    public CollectionOperationMetadata(
            int pathIndex,
            CollectionAttribute<O, E> collectionAttribute,
            CollectionSelector selector,
            Specification<E> specification
    ) {
        this(pathIndex, collectionAttribute, selector, specification, null);
    }

    /**
     * Constructor for collection operation metadata with comparator support.
     * NOTE: pathIndex is calculated automatically by the builder, not provided by the user.
     *
     * @param pathIndex Index in the path where this operation applies
     * (calculated by builder)
     * @param collectionAttribute The collection attribute (type-safe)
     * @param selector The collection selector
     * @param specification The specification for filtering (can be null)
     * @param comparator The comparator for sorting elements (can be null)
     * @throws IllegalArgumentException if pathIndex is negative,
     * collectionAttribute is null, or selector is null
     */
    public CollectionOperationMetadata(
            int pathIndex,
            CollectionAttribute<O, E> collectionAttribute,
            CollectionSelector selector,
            Specification<E> specification,
            Comparator<E> comparator
    ) {
        if (pathIndex < 0) {
            throw new IllegalArgumentException("pathIndex must be non-negative");
        }
        Objects.requireNonNull(collectionAttribute, "collectionAttribute cannot be null");
        Objects.requireNonNull(selector, "selector cannot be null");

        this.pathIndex = pathIndex;
        this.collectionAttribute = collectionAttribute;
        this.selector = selector;
        this.specification = specification;
        this.comparator = comparator;
    }

    /**
     * Gets the index in the path where this operation applies (0-based).
     *
     * @return the path index
     */
    public int getPathIndex() {
        return pathIndex;
    }

    /**
     * Gets the collection attribute this operation applies to.
     *
     * @return the collection attribute
     */
    public CollectionAttribute<O, E> getCollectionAttribute() {
        return collectionAttribute;
    }

    /**
     * Gets the collection selector (ALL, FIRST, LAST, ANY).
     *
     * @return the collection selector
     */
    public CollectionSelector getSelector() {
        return selector;
    }

    /**
     * Gets the specification for filtering collection elements.
     *
     * @return the specification, or null if no filtering is applied
     */
    public Specification<E> getSpecification() {
        return specification;
    }

    /**
     * Gets the comparator for sorting collection elements.
     *
     * @return the comparator, or null if no sorting is applied
     */
    public Comparator<E> getComparator() {
        return comparator;
    }

    /**
     * Gets the element type of the collection.
     *
     * @return the element type class
     */
    public Class<E> getElementType() {
        return collectionAttribute.getElementType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CollectionOperationMetadata<?, ?> that = (CollectionOperationMetadata<?, ?>) o;
        return pathIndex == that.pathIndex
                && Objects.equals(collectionAttribute, that.collectionAttribute)
                && selector == that.selector
                && Objects.equals(specification, that.specification)
                && Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathIndex, collectionAttribute, selector, specification, comparator);
    }

    @Override
    public String toString() {
        return "CollectionOperationMetadata{"
                + "pathIndex=" + pathIndex
                + ", collectionAttribute=" + collectionAttribute
                + ", selector=" + selector
                + ", specification=" + specification
                + ", comparator=" + comparator
                + '}';
    }
}
