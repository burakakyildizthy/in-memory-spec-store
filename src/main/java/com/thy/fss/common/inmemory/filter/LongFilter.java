package com.thy.fss.common.inmemory.filter;

/**
 * Long filter class for Long field types.
 * Extends RangeFilter to provide range-based filtering operations for Long values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class LongFilter extends NumberFilter<Long> {

    /**
     * Default constructor.
     */
    public LongFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The long filter to copy from
     */
    public LongFilter(LongFilter filter) {
        super(filter);
    }

    @Override
    public LongFilter setEquals(Long equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public LongFilter setNotEquals(Long notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public LongFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public LongFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public LongFilter setIn(java.util.Collection<Long> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public LongFilter setNotIn(java.util.Collection<Long> notIn) {
        super.setNotIn(notIn);
        return this;
    }

    @Override
    public LongFilter setGreaterThan(Long greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    @Override
    public LongFilter setLessThan(Long lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    @Override
    public LongFilter setGreaterOrEqualThan(Long greaterOrEqualThan) {
        super.setGreaterOrEqualThan(greaterOrEqualThan);
        return this;
    }

    @Override
    public LongFilter setLessOrEqualThan(Long lessOrEqualThan) {
        super.setLessOrEqualThan(lessOrEqualThan);
        return this;
    }
}