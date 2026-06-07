package com.thy.fss.common.inmemory.filter;

import java.time.LocalDateTime;

/**
 * LocalDateTime filter class for LocalDateTime field types.
 * Extends RangeFilter to provide range-based filtering operations for LocalDateTime values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class LocalDateTimeFilter extends TemporalFilter<LocalDateTime> {

    /**
     * Default constructor.
     */
    public LocalDateTimeFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The local date time filter to copy from
     */
    public LocalDateTimeFilter(LocalDateTimeFilter filter) {
        super(filter);
    }

    @Override
    public LocalDateTimeFilter setEquals(LocalDateTime equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public LocalDateTimeFilter setNotEquals(LocalDateTime notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIn(java.util.Collection<LocalDateTime> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public LocalDateTimeFilter setNotIn(java.util.Collection<LocalDateTime> notIn) {
        super.setIn(notIn);
        return this;
    }


    @Override
    public LocalDateTimeFilter setIsBefore(LocalDateTime isBefore) {
        super.setIsBefore(isBefore);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIsAfter(LocalDateTime isAfter) {
        super.setIsAfter(isAfter);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIsOnOrBefore(LocalDateTime isOnOrBefore) {
        super.setIsOnOrBefore(isOnOrBefore);
        return this;
    }

    @Override
    public LocalDateTimeFilter setIsOnOrAfter(LocalDateTime isOnOrAfter) {
        super.setIsOnOrAfter(isOnOrAfter);
        return this;
    }
}