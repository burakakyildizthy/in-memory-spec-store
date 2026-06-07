package com.thy.fss.common.inmemory.specification;

import java.util.function.Predicate;

/**
 * Implementation of Specification that represents the logical AND of two specifications.
 * Updated to use the new test() method for better performance while maintaining
 * backward compatibility with toPredicate().
 *
 * @param <T> the type of objects this specification can evaluate
 */
public class AndSpecification<T> implements Specification<T> {

    private final Specification<T> left;
    private final Specification<T> right;

    /**
     * Creates a new AND specification.
     *
     * @param left  the first specification
     * @param right the second specification
     */
    public AndSpecification(Specification<T> left, Specification<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean test(T entity) {
        return left.test(entity) && right.test(entity);
    }

    @Override
    public Predicate<T> toPredicate() {
        return this::test;
    }
}