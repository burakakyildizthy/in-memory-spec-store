package com.thy.fss.common.inmemory.filter;

import java.time.Instant;

/**
 * Instant filter class for Instant field types.
 * Extends RangeFilter to provide range-based filtering operations for Instant values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class InstantFilter extends TemporalFilter<Instant> {

    /**
     * Default constructor.
     */
    public InstantFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The instant filter to copy from
     */
    public InstantFilter(InstantFilter filter) {
        super(filter);
    }

    @Override
    public InstantFilter setEquals(Instant equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public InstantFilter setNotEquals(Instant notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public InstantFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public InstantFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public InstantFilter setIn(java.util.Collection<Instant> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public InstantFilter setNotIn(java.util.Collection<Instant> notIn) {
        super.setIn(notIn);
        return this;
    }

    @Override
    public InstantFilter setIsBefore(Instant isBefore) {
        super.setIsBefore(isBefore);
        return this;
    }

    @Override
    public InstantFilter setIsAfter(Instant isAfter) {
        super.setIsAfter(isAfter);
        return this;
    }

    @Override
    public InstantFilter setIsOnOrBefore(Instant isOnOrBefore) {
        super.setIsOnOrBefore(isOnOrBefore);
        return this;
    }

    @Override
    public InstantFilter setIsOnOrAfter(Instant isOnOrAfter) {
        super.setIsOnOrAfter(isOnOrAfter);
        return this;
    }

    @Override
    public InstantFilter setNotIsBefore(Instant notIsBefore) {
        super.setNotIsBefore(notIsBefore);
        return this;
    }

    @Override
    public InstantFilter setNotIsAfter(Instant notIsAfter) {
        super.setNotIsAfter(notIsAfter);
        return this;
    }

    @Override
    public InstantFilter setNotIsOnOrBefore(Instant notIsOnOrBefore) {
        super.setNotIsOnOrBefore(notIsOnOrBefore);
        return this;
    }

    @Override
    public InstantFilter setNotIsOnOrAfter(Instant notIsOnOrAfter) {
        super.setNotIsOnOrAfter(notIsOnOrAfter);
        return this;
    }
}