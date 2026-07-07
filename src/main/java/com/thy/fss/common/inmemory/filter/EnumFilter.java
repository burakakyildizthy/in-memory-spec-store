package com.thy.fss.common.inmemory.filter;

public class EnumFilter<F extends Enum<? super F>> extends Filter<F> {

    public EnumFilter() {
        super();
    }

    public EnumFilter(EnumFilter<F> filter) {
        super(filter);
    }

    @Override
    public EnumFilter<F> setEquals(F equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public EnumFilter<F> setNotEquals(F notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public EnumFilter<F> setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public EnumFilter<F> setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public EnumFilter<F> setIn(java.util.Collection<F> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public EnumFilter<F> setNotIn(java.util.Collection<F> notIn) {
        super.setNotIn(notIn);
        return this;
    }

}