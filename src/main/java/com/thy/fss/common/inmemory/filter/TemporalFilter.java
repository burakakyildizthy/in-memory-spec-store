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
    @JsonProperty("nbe")
    private F notIsBefore;
    @JsonProperty("naf")
    private F notIsAfter;
    @JsonProperty("nobe")
    private F notIsOnOrBefore;
    @JsonProperty("noaf")
    private F notIsOnOrAfter;
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
        this.notIsBefore = filter.notIsBefore;
        this.notIsAfter = filter.notIsAfter;
        this.notIsOnOrBefore = filter.notIsOnOrBefore;
        this.notIsOnOrAfter = filter.notIsOnOrAfter;
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

    /**
     * Gets the notIsBefore filter value.
     *
     * @return The value for not is before comparison
     */
    public F getNotIsBefore() {
        return notIsBefore;
    }

    /**
     * Sets the notIsBefore filter value.
     *
     * @param notIsBefore The value for not is before comparison
     */
    public TemporalFilter<F> setNotIsBefore(F notIsBefore) {
        this.notIsBefore = notIsBefore;
        return this;
    }

    /**
     * Gets the notIsAfter filter value.
     *
     * @return The value for not is after comparison
     */
    public F getNotIsAfter() {
        return notIsAfter;
    }

    /**
     * Sets the notIsAfter filter value.
     *
     * @param notIsAfter The value for not is after comparison
     */
    public TemporalFilter<F> setNotIsAfter(F notIsAfter) {
        this.notIsAfter = notIsAfter;
        return this;
    }

    /**
     * Gets the notIsOnOrBefore filter value.
     *
     * @return The value for not is on or before comparison
     */
    public F getNotIsOnOrBefore() {
        return notIsOnOrBefore;
    }

    /**
     * Sets the notIsOnOrBefore filter value.
     *
     * @param notIsOnOrBefore The value for not is on or before comparison
     */
    public TemporalFilter<F> setNotIsOnOrBefore(F notIsOnOrBefore) {
        this.notIsOnOrBefore = notIsOnOrBefore;
        return this;
    }

    /**
     * Gets the notIsOnOrAfter filter value.
     *
     * @return The value for not is on or after comparison
     */
    public F getNotIsOnOrAfter() {
        return notIsOnOrAfter;
    }

    /**
     * Sets the notIsOnOrAfter filter value.
     *
     * @param notIsOnOrAfter The value for not is on or after comparison
     */
    public TemporalFilter<F> setNotIsOnOrAfter(F notIsOnOrAfter) {
        this.notIsOnOrAfter = notIsOnOrAfter;
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
            && Objects.equals(notIsBefore, that.notIsBefore)
            && Objects.equals(notIsAfter, that.notIsAfter)
            && Objects.equals(notIsOnOrBefore, that.notIsOnOrBefore)
            && Objects.equals(notIsOnOrAfter, that.notIsOnOrAfter)
            && Objects.equals(last, that.last)
            && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isBefore, isAfter, isOnOrBefore, isOnOrAfter,
            notIsBefore, notIsAfter, notIsOnOrBefore, notIsOnOrAfter, last, next);
    }

    @Override
    public String toString() {
        return "TemporalFilter{" +
                "isBefore=" + isBefore +
                ", isAfter=" + isAfter +
                ", isOnOrBefore=" + isOnOrBefore +
                ", isOnOrAfter=" + isOnOrAfter +
                ", notIsBefore=" + notIsBefore +
                ", notIsAfter=" + notIsAfter +
                ", notIsOnOrBefore=" + notIsOnOrBefore +
                ", notIsOnOrAfter=" + notIsOnOrAfter +
            ", last=" + last +
            ", next=" + next +
                '}';
    }
}