package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base filter class for all field types.
 * Provides common filtering operations that can be applied to any field type.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 * 
 * <p>This class implements {@link FilterBase} to provide type compatibility with
 * {@link EntityFilter} in generic contexts such as {@link CollectionFilter}. This
 * unified type hierarchy enables {@code CollectionFilter<E>} to work with both
 * basic type filters (StringFilter, IntegerFilter, etc.) and model type filters
 * (UserFilter, AddressFilter, etc.) using a single {@code FilterBase<E>} type parameter.</p>
 *
 * @param <F> The type of the field being filtered
 * @see FilterBase
 * @see EntityFilter
 * @see CollectionFilter
 */
public abstract class Filter<F> implements FilterBase<F> {

    @JsonProperty("eq")
    private F equals;
    @JsonProperty("neq")
    private F notEquals;
    @JsonProperty("isn")
    private Boolean isNull;
    @JsonProperty("isnn")
    private Boolean isNotNull;
    @JsonProperty("in")
    private Collection<F> in;
    @JsonProperty("nin")
    private Collection<F> notIn;

    /**
     * Default constructor.
     */
    protected Filter() {
    }

    /**
     * Copy constructor.
     *
     * @param filter The filter to copy from
     */
    protected Filter(Filter<F> filter) {
        this.equals = filter.equals;
        this.notEquals = filter.notEquals;
        this.isNull = filter.isNull;
        this.isNotNull = filter.isNotNull;
        this.in = filter.in != null ? List.copyOf(filter.in) : null;
        this.notIn = filter.notIn != null ? List.copyOf(filter.notIn) : null;
    }

    /**
     * Gets the equals filter value.
     *
     * @return The value for exact matching
     */
    public F getEquals() {
        return equals;
    }

    /**
     * Sets the equals filter value.
     *
     * @param equals The value for exact matching
     * @return This filter instance for method chaining
     */
    public Filter<F> setEquals(F equals) {
        this.equals = equals;
        return this;
    }

    /**
     * Gets the notEquals filter value.
     *
     * @return The value for not equals matching
     */
    public F getNotEquals() {
        return notEquals;
    }

    /**
     * Sets the notEquals filter value.
     *
     * @param notEquals The value for not equals matching
     * @return This filter instance for method chaining
     */
    public Filter<F> setNotEquals(F notEquals) {
        this.notEquals = notEquals;
        return this;
    }

    /**
     * Gets the specified filter value.
     *
     * @return True to match non-null values, false to match null values, null to ignore
     */
    public Boolean getIsNull() {
        return isNull;
    }

    /**
     * Sets the specified filter value.
     *
     * @param isNull True to match non-null values, false to match null values, null to ignore
     * @return This filter instance for method chaining
     */
    public Filter<F> setIsNull(Boolean isNull) {
        this.isNull = isNull;
        return this;
    }

    /**
     * Gets the isNotNull filter value.
     *
     * @return True to match non-null values, false to match null values, null to ignore
     */
    public Boolean getIsNotNull() {
        return isNotNull;
    }

    /**
     * Sets the isNotNull filter value.
     *
     * @param isNotNull True to match non-null values, false to match null values, null to ignore
     * @return This filter instance for method chaining
     */
    public Filter<F> setIsNotNull(Boolean isNotNull) {
        this.isNotNull = isNotNull;
        return this;
    }

    /**
     * Gets the in filter values.
     *
     * @return List of values for inclusion matching
     */
    public Collection<F> getIn() {
        return in;
    }

    /**
     * Sets the in filter values.
     *
     * @param in List of values for inclusion matching
     * @return This filter instance for method chaining
     */
    public Filter<F> setIn(Collection<F> in) {
        this.in = in;
        return this;
    }

    /**
     * Gets the notIn filter values.
     *
     * @return List of values for exclusion matching
     */
    public Collection<F> getNotIn() {
        return notIn;
    }

    /**
     * Sets the notIn filter values.
     *
     * @param notIn List of values for exclusion matching
     * @return This filter instance for method chaining
     */
    public Filter<F> setNotIn(Collection<F> notIn) {
        this.notIn = notIn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Filter<?> filter = (Filter<?>) o;
        return Objects.equals(equals, filter.equals) &&
                Objects.equals(notEquals, filter.notEquals) &&
                Objects.equals(isNull, filter.isNull) &&
                Objects.equals(isNotNull, filter.isNotNull) &&
                Objects.equals(in, filter.in) &&
                Objects.equals(notIn, filter.notIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(equals, notEquals, isNull, isNotNull, in, notIn);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "equals=" + equals +
                ", notEquals=" + notEquals +
                ", isNull=" + isNull +
                ", isNotNull=" + isNotNull +
                ", in=" + in +
                ", notIn=" + notIn +
                '}';
    }
}