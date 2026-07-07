package com.thy.fss.common.inmemory.specification;

import java.util.function.Predicate;

/**
 * Implementation of Specification that represents the logical NOT of a specification.
 * Updated to use the new test() method for better performance while maintaining
 * backward compatibility with toPredicate().
 *
 * @param <T> the type of objects this specification can evaluate
 */
public class NotSpecification<T> implements Specification<T> {

    private final Specification<T> specification;

    /**
     * Creates a new NOT specification.
     *
     * @param specification the specification to negate
     */
    public NotSpecification(Specification<T> specification) {
        this.specification = specification;
    }

    @Override
    public boolean test(T entity) {
        return !specification.test(entity);
    }

    @Override
    public Predicate<T> toPredicate() {
        return this::test;
    }
}