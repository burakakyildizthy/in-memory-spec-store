package com.thy.fss.common.inmemory.filter;

import java.time.LocalDate;

/**
 * LocalDate filter class for LocalDate field types.
 * Extends RangeFilter to provide range-based filtering operations for LocalDate values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class LocalDateFilter extends TemporalFilter<LocalDate> {

    /**
     * Default constructor.
     */
    public LocalDateFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The local date filter to copy from
     */
    public LocalDateFilter(LocalDateFilter filter) {
        super(filter);
    }

    @Override
    public LocalDateFilter setEquals(LocalDate equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public LocalDateFilter setNotEquals(LocalDate notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public LocalDateFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public LocalDateFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public LocalDateFilter setIn(java.util.Collection<LocalDate> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public LocalDateFilter setNotIn(java.util.Collection<LocalDate> notIn) {
        super.setIn(notIn);
        return this;
    }


    @Override
    public LocalDateFilter setIsBefore(LocalDate isBefore) {
        super.setIsBefore(isBefore);
        return this;
    }

    @Override
    public LocalDateFilter setIsAfter(LocalDate isAfter) {
        super.setIsAfter(isAfter);
        return this;
    }

    @Override
    public LocalDateFilter setIsOnOrBefore(LocalDate isOnOrBefore) {
        super.setIsOnOrBefore(isOnOrBefore);
        return this;
    }

    @Override
    public LocalDateFilter setIsOnOrAfter(LocalDate isOnOrAfter) {
        super.setIsOnOrAfter(isOnOrAfter);
        return this;
    }
}