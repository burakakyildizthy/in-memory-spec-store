package com.thy.fss.common.inmemory.filter;

/**
 * Integer filter class for Integer field types.
 * Extends RangeFilter to provide range-based filtering operations for Integer values.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class IntegerFilter extends NumberFilter<Integer> {

    /**
     * Default constructor.
     */
    public IntegerFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The integer filter to copy from
     */
    public IntegerFilter(IntegerFilter filter) {
        super(filter);
    }

    @Override
    public IntegerFilter setEquals(Integer equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public IntegerFilter setNotEquals(Integer notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public IntegerFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public IntegerFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public IntegerFilter setIn(java.util.Collection<Integer> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public IntegerFilter setNotIn(java.util.Collection<Integer> notIn) {
        super.setNotIn(notIn);
        return this;
    }

    @Override
    public IntegerFilter setGreaterThan(Integer greaterThan) {
        super.setGreaterThan(greaterThan);
        return this;
    }

    @Override
    public IntegerFilter setLessThan(Integer lessThan) {
        super.setLessThan(lessThan);
        return this;
    }

    @Override
    public IntegerFilter setGreaterOrEqualThan(Integer greaterOrEqualThan) {
        super.setGreaterOrEqualThan(greaterOrEqualThan);
        return this;
    }

    @Override
    public IntegerFilter setLessOrEqualThan(Integer lessOrEqualThan) {
        super.setLessOrEqualThan(lessOrEqualThan);
        return this;
    }
}