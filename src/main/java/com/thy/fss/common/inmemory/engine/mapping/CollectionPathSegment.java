package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;

import java.util.Objects;

/**
 * Represents a segment in a property path that involves a collection field.
 * Contains both the collection attribute and the selector operation to apply.
 */
public class CollectionPathSegment {

    private final CollectionAttribute<?, ?> attribute;
    private final CollectionSelector selector;

    /**
     * Constructor for CollectionPathSegment.
     *
     * @param attribute the collection attribute
     * @param selector the collection selector operation
     */
    public CollectionPathSegment(CollectionAttribute<?, ?> attribute, CollectionSelector selector) {
        this.attribute = Objects.requireNonNull(attribute, "Attribute cannot be null");
        this.selector = Objects.requireNonNull(selector, "Selector cannot be null");
    }

    /**
     * Gets the collection attribute.
     *
     * @return the collection attribute
     */
    public CollectionAttribute<?, ?> getAttribute() {
        return attribute;
    }

    /**
     * Gets the collection selector.
     *
     * @return the collection selector
     */
    public CollectionSelector getSelector() {
        return selector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CollectionPathSegment that)) {
            return false;
        }
        return attribute.equals(that.attribute) && selector == that.selector;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, selector);
    }

    @Override
    public String toString() {
        return String.format("CollectionPathSegment{attribute=%s, selector=%s}", attribute, selector);
    }
}
