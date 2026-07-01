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
    @JsonProperty("ngt")
    private F notGreaterThan;
    @JsonProperty("nlt")
    private F notLessThan;
    @JsonProperty("ngte")
    private F notGreaterOrEqualThan;
    @JsonProperty("nlte")
    private F notLessOrEqualThan;

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
        this.notGreaterThan = filter.notGreaterThan;
        this.notLessThan = filter.notLessThan;
        this.notGreaterOrEqualThan = filter.notGreaterOrEqualThan;
        this.notLessOrEqualThan = filter.notLessOrEqualThan;
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

    /**
     * Gets the notGreaterThan filter value.
     *
     * @return The value for not greater than comparison
     */
    public F getNotGreaterThan() {
        return notGreaterThan;
    }

    /**
     * Sets the notGreaterThan filter value.
     *
     * @param notGreaterThan The value for not greater than comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setNotGreaterThan(F notGreaterThan) {
        this.notGreaterThan = notGreaterThan;
        return this;
    }

    /**
     * Gets the notLessThan filter value.
     *
     * @return The value for not less than comparison
     */
    public F getNotLessThan() {
        return notLessThan;
    }

    /**
     * Sets the notLessThan filter value.
     *
     * @param notLessThan The value for not less than comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setNotLessThan(F notLessThan) {
        this.notLessThan = notLessThan;
        return this;
    }

    /**
     * Gets the notGreaterOrEqualThan filter value.
     *
     * @return The value for not greater than or equal comparison
     */
    public F getNotGreaterOrEqualThan() {
        return notGreaterOrEqualThan;
    }

    /**
     * Sets the notGreaterOrEqualThan filter value.
     *
     * @param notGreaterOrEqualThan The value for not greater than or equal comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setNotGreaterOrEqualThan(F notGreaterOrEqualThan) {
        this.notGreaterOrEqualThan = notGreaterOrEqualThan;
        return this;
    }

    /**
     * Gets the notLessOrEqualThan filter value.
     *
     * @return The value for not less than or equal comparison
     */
    public F getNotLessOrEqualThan() {
        return notLessOrEqualThan;
    }

    /**
     * Sets the notLessOrEqualThan filter value.
     *
     * @param notLessOrEqualThan The value for not less than or equal comparison
     * @return This filter instance for method chaining
     */
    public NumberFilter<F> setNotLessOrEqualThan(F notLessOrEqualThan) {
        this.notLessOrEqualThan = notLessOrEqualThan;
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
                Objects.equals(lessOrEqualThan, that.lessOrEqualThan) &&
                Objects.equals(notGreaterThan, that.notGreaterThan) &&
                Objects.equals(notLessThan, that.notLessThan) &&
                Objects.equals(notGreaterOrEqualThan, that.notGreaterOrEqualThan) &&
                Objects.equals(notLessOrEqualThan, that.notLessOrEqualThan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), greaterThan, lessThan, greaterOrEqualThan, lessOrEqualThan,
                notGreaterThan, notLessThan, notGreaterOrEqualThan, notLessOrEqualThan);
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
                ", notGreaterThan=" + notGreaterThan +
                ", notLessThan=" + notLessThan +
                ", notGreaterOrEqualThan=" + notGreaterOrEqualThan +
                ", notLessOrEqualThan=" + notLessOrEqualThan +
                '}';
    }
}