package com.thy.fss.common.inmemory.filter;

/**
 * Boolean filter class for Boolean field types.
 * Extends the base Filter class for Boolean-specific filtering operations.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class BooleanFilter extends Filter<Boolean> {

    /**
     * Default constructor.
     */
    public BooleanFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The boolean filter to copy from
     */
    public BooleanFilter(BooleanFilter filter) {
        super(filter);
    }

    @Override
    public BooleanFilter setEquals(Boolean equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public BooleanFilter setNotEquals(Boolean notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public BooleanFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public BooleanFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public BooleanFilter setIn(java.util.Collection<Boolean> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public BooleanFilter setNotIn(java.util.Collection<Boolean> notIn) {
        super.setNotIn(notIn);
        return this;
    }

}