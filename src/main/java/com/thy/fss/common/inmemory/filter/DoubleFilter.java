package com.thy.fss.common.inmemory.filter;

/**
 * Double filter class for Double field types.
 * Extends RangeFilter to provide range-based filtering operations for Double values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class DoubleFilter extends NumberFilter<Double> {


    /**
     * Default constructor.
     */
    public DoubleFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The double filter to copy from
     */
    public DoubleFilter(DoubleFilter filter) {
        super(filter);
    }

    @Override
    public DoubleFilter setEquals(Double equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public DoubleFilter setNotEquals(Double notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public DoubleFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public DoubleFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public DoubleFilter setIn(java.util.Collection<Double> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public DoubleFilter setNotIn(java.util.Collection<Double> notIn) {
        super.setNotIn(notIn);
        return this;
    }

    @Override
    public DoubleFilter setGreaterThan(Double greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    @Override
    public DoubleFilter setLessThan(Double lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    @Override
    public DoubleFilter setGreaterOrEqualThan(Double greaterOrEqualThan) {
        super.setGreaterOrEqualThan(greaterOrEqualThan);
        return this;
    }

    @Override
    public DoubleFilter setLessOrEqualThan(Double lessOrEqualThan) {
        super.setLessOrEqualThan(lessOrEqualThan);
        return this;
    }
}