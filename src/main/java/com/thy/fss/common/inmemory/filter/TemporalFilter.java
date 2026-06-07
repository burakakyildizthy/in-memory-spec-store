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
public class TemporalFilter<F extends Comparable<? super F>> extends Filter<F> {

    @JsonProperty("be")
    private F isBefore;
    @JsonProperty("af")
    private F isAfter;
    @JsonProperty("obe")
    private F isOnOrBefore;
    @JsonProperty("oaf")
    private F isOnOrAfter;
    @JsonProperty("last")
    private TemporalPreset last;
    @JsonProperty("next")
    private TemporalPreset next;

    /**
     * Default constructor.
     */
    public TemporalFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The range filter to copy from
     */
    public TemporalFilter(TemporalFilter<F> filter) {
        super(filter);
        this.isBefore = filter.isBefore;
        this.isAfter = filter.isAfter;
        this.isOnOrBefore = filter.isOnOrBefore;
        this.isOnOrAfter = filter.isOnOrAfter;
        this.last = filter.last;
        this.next = filter.next;
    }

    /**
     * Gets the isBefore filter value.
     *
     * @return The value for is before comparison
     */
    public F getIsBefore() {
        return isBefore;
    }

    /**
     * Sets the isBefore filter value.
     *
     * @param isBefore The value for is before comparison
     */
    public TemporalFilter<F> setIsBefore(F isBefore) {
        this.isBefore = isBefore;
        return this;
    }

    /**
     * Gets the isAfter filter value.
     *
     * @return The value for is after comparison
     */
    public F getIsAfter() {
        return isAfter;
    }

    /**
     * Sets the isAfter filter value.
     *
     * @param isAfter The value for is after comparison
     */
    public TemporalFilter<F> setIsAfter(F isAfter) {
        this.isAfter = isAfter;
        return this;
    }

    /**
     * Gets the isOnOrBefore filter value.
     *
     * @return The value for is on or before comparison
     */
    public F getIsOnOrBefore() {
        return isOnOrBefore;
    }

    /**
     * Sets the isOnOrBefore filter value.
     *
     * @param isOnOrBefore The value for is on or before comparison
     */
    public TemporalFilter<F> setIsOnOrBefore(F isOnOrBefore) {
        this.isOnOrBefore = isOnOrBefore;
        return this;
    }

    /**
     * Gets the isOnOrAfter filter value.
     *
     * @return The value for is on or after comparison
     */
    public F getIsOnOrAfter() {
        return isOnOrAfter;
    }

    /**
     * Sets the isOnOrAfter filter value.
     *
     * @param isOnOrAfter The value for is on or after comparison
     */
    public TemporalFilter<F> setIsOnOrAfter(F isOnOrAfter) {
        this.isOnOrAfter = isOnOrAfter;
        return this;
    }

    public TemporalPreset getLast() {
        return last;
    }

    public TemporalFilter<F> setLast(TemporalPreset last) {
        this.last = last;
        return this;
    }

    public TemporalPreset getNext() {
        return next;
    }

    public TemporalFilter<F> setNext(TemporalPreset next) {
        this.next = next;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TemporalFilter<?> that = (TemporalFilter<?>) o;
        return Objects.equals(isBefore, that.isBefore)
            && Objects.equals(isAfter, that.isAfter)
            && Objects.equals(isOnOrBefore, that.isOnOrBefore)
            && Objects.equals(isOnOrAfter, that.isOnOrAfter)
            && Objects.equals(last, that.last)
            && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isBefore, isAfter, isOnOrBefore, isOnOrAfter, last, next);
    }

    @Override
    public String toString() {
        return "TemporalFilter{" +
                "isBefore=" + isBefore +
                ", isAfter=" + isAfter +
                ", isOnOrBefore=" + isOnOrBefore +
                ", isOnOrAfter=" + isOnOrAfter +
            ", last=" + last +
            ", next=" + next +
                '}';
    }
}