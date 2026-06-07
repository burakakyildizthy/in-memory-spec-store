package com.thy.fss.common.inmemory.specification;

import java.util.function.Predicate;

/**
 * Specification interface for type-safe querying of in-memory data. Provides a
 * fluent API for building complex queries with logical operations.
 *
 * @param <T> the type of objects this specification can evaluate
 */
public interface Specification<T> {

    /**
     * Converts this specification to a Predicate that can be used for
     * filtering.
     * <p>
     * For new implementations using StaticSpecificationService, this method
     * should delegate to the test() method. For legacy implementations, this
     * method provides the core functionality.
     *
     * @return a Predicate representing this specification's criteria
     */
    Predicate<T> toPredicate();

    /**
     * Tests whether the given entity matches this specification's criteria.
     * <p>
     * This method provides a direct way to test entities without creating
     * predicates. New implementations using StaticSpecificationService should
     * implement this method directly for better performance. Legacy
     * implementations can use the default implementation that delegates to
     * toPredicate().
     *
     * @param entity the entity to test
     * @return true if the entity matches the specification, false otherwise
     */
    default boolean test(T entity) {
        return toPredicate().test(entity);
    }

    /**
     * Creates a new specification that is the logical AND of this specification
     * and another.
     *
     * @param other the other specification to combine with
     * @return a new specification representing the logical AND
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /**
     * Creates a new specification that is the logical OR of this specification
     * and another.
     *
     * @param other the other specification to combine with
     * @return a new specification representing the logical OR
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * Creates a new specification that is the logical NOT of this
     * specification.
     *
     * @return a new specification representing the logical NOT
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
