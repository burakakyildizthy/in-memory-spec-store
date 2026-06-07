package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Range filter class for comparable field types.
 * Extends the base Filter class with range-based filtering operations.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 *
 * @param <F> The type of the field being filtered, must be Comparable
 */
public class NumberFilter<F extends Comparable<? super F>> extends Filter<F> {

    @JsonProperty("gt")
    private F greaterThan;
    @JsonProperty("lt")
    private F lessThan;
    @JsonProperty("gte")
    private F greaterOrEqualThan;
    @JsonProperty("lte")
    private F lessOrEqualThan;

    /**
     * Default constructor.
     */
    public NumberFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The range filter to copy from
     */
    public NumberFilter(NumberFilter<F> filter) {
        super(filter);
        this.greaterThan = filter.greaterThan;
        this.lessThan = filter.lessThan;
        this.greaterOrEqualThan = filter.greaterOrEqualThan;
        this.lessOrEqualThan = filter.lessOrEqualThan;
    }

    /**
     * Gets the greaterThan filter value.
     *
     * @return The value for greater than comparison
     */
    public F getGreaterThan() {
        return greaterThan;
    }

    /**
     * Sets the greaterThan filter value.
     *
     * @param greaterThan The value for greater than comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setGreaterThan(F greaterThan) {
        this.greaterThan = greaterThan;
        return this;
    }

    /**
     * Gets the lessThan filter value.
     *
     * @return The value for less than comparison
     */
    public F getLessThan() {
        return lessThan;
    }

    /**
     * Sets the lessThan filter value.
     *
     * @param lessThan The value for less than comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setLessThan(F lessThan) {
        this.lessThan = lessThan;
        return this;
    }

    /**
     * Gets the greaterOrEqualThan filter value.
     *
     * @return The value for greater than or equal comparison
     */
    public F getGreaterOrEqualThan() {
        return greaterOrEqualThan;
    }

    /**
     * Sets the greaterOrEqualThan filter value.
     *
     * @param greaterOrEqualThan The value for greater than or equal comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setGreaterOrEqualThan(F greaterOrEqualThan) {
        this.greaterOrEqualThan = greaterOrEqualThan;
        return this;
    }

    /**
     * Gets the lessOrEqualThan filter value.
     *
     * @return The value for less than or equal comparison
     */
    public F getLessOrEqualThan() {
        return lessOrEqualThan;
    }

    /**
     * Sets the lessOrEqualThan filter value.
     *
     * @param lessOrEqualThan The value for less than or equal comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setLessOrEqualThan(F lessOrEqualThan) {
        this.lessOrEqualThan = lessOrEqualThan;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NumberFilter<?> that = (NumberFilter<?>) o;
        return Objects.equals(greaterThan, that.greaterThan) &&
                Objects.equals(lessThan, that.lessThan) &&
                Objects.equals(greaterOrEqualThan, that.greaterOrEqualThan) &&
                Objects.equals(lessOrEqualThan, that.lessOrEqualThan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), greaterThan, lessThan, greaterOrEqualThan, lessOrEqualThan);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "equals=" + getEquals() +
                ", specified=" + getIsNull() +
                ", in=" + getIn() +
                ", notIn=" + getNotIn() +
                ", greaterThan=" + greaterThan +
                ", lessThan=" + lessThan +
                ", greaterOrEqualThan=" + greaterOrEqualThan +
                ", lessOrEqualThan=" + lessOrEqualThan +
                '}';
    }
}