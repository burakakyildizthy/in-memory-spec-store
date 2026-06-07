package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Objects;

/**
 * Collection filter class for Collection field types.
 * Extends the base Filter class with collection-specific filtering operations.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 * 
 * <p>This class uses {@link FilterBase} for collection operators (any, all, none) to
 * enable filtering collections of both basic types and model types. The unified type
 * hierarchy allows {@code CollectionFilter<E>} to work seamlessly with:</p>
 * <ul>
 *   <li><b>Basic Types:</b> String, Integer, Boolean, etc. using their respective
 *       Filter subclasses (StringFilter, IntegerFilter, etc.)</li>
 *   <li><b>Model Types:</b> User, Address, Order, etc. using their generated
 *       EntityFilter implementations (UserFilter, AddressFilter, etc.)</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * Basic Type Collections
 * <pre>{@code
 * // Filtering a collection of strings
 * CollectionFilter<String> tagsFilter = new CollectionFilter<>();
 * StringFilter stringFilter = new StringFilter();
 * stringFilter.setEquals("important");
 * tagsFilter.setCollectionAny(stringFilter);  // Any tag equals "important"
 * }</pre>
 * 
 * Model Type Collections
 * <pre>{@code
 * // Filtering a collection of User objects
 * CollectionFilter<User> usersFilter = new CollectionFilter<>();
 * UserFilter userFilter = new UserFilter();
 * userFilter.getName().setEquals("John");
 * userFilter.getAge().setGreaterThan(18);
 * usersFilter.setCollectionAny(userFilter);  // Any user named "John" and age > 18
 * }</pre>
 * 
 * Multiple Collection Operators
 * <pre>{@code
 * // Combining multiple collection operators
 * CollectionFilter<User> usersFilter = new CollectionFilter<>();
 * 
 * // Any user must be active
 * UserFilter anyFilter = new UserFilter();
 * anyFilter.getActive().setEquals(true);
 * usersFilter.setCollectionAny(anyFilter);
 * 
 * // All users must be verified
 * UserFilter allFilter = new UserFilter();
 * allFilter.getVerified().setEquals(true);
 * usersFilter.setCollectionAll(allFilter);
 * 
 * // No users should be banned
 * UserFilter noneFilter = new UserFilter();
 * noneFilter.getBanned().setEquals(true);
 * usersFilter.setCollectionNone(noneFilter);
 * }</pre>
 * 
 * Web Query Parameters
 * <pre>{@code
 * // Basic type collection filtering
 * // GET /api/entities?tags.any.eq=important
 * 
 * // Model type collection filtering (single level)
 * // GET /api/entities?users.any.name.eq=John
 * 
 * // Model type collection filtering (multi-level)
 * // GET /api/entities?users.any.address.city.eq=Istanbul
 * 
 * // Multiple operators
 * // GET /api/entities?users.any.active.eq=true&users.all.verified.eq=true
 * }</pre>
 *
 * @param <E> The type of elements in the collection being filtered
 * @see FilterBase
 * @see Filter
 * @see EntityFilter
 */
public class CollectionFilter<E> extends Filter<Collection<E>> {

    @JsonProperty("cont")
    private E collectionContains;
    @JsonProperty("any")
    private FilterBase<E> collectionAny;
    @JsonProperty("all")
    private FilterBase<E> collectionAll;
    @JsonProperty("none")
    private FilterBase<E> collectionNone;
    @JsonProperty("empty")
    private Boolean isEmpty;
    @JsonProperty("nempty")
    private Boolean isNotEmpty;

    /**
     * Default constructor.
     */
    public CollectionFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The collection filter to copy from
     */
    public CollectionFilter(CollectionFilter<E> filter) {
        super(filter);
        this.collectionContains = filter.collectionContains;
        this.collectionAny = filter.collectionAny;
        this.collectionAll = filter.collectionAll;
        this.collectionNone = filter.collectionNone;
        this.isEmpty = filter.isEmpty;
        this.isNotEmpty = filter.isNotEmpty;
    }

    /**
     * Gets the collectionContains filter value.
     *
     * @return The element that the collection must contain
     */
    public E getCollectionContains() {
        return collectionContains;
    }

    /**
     * Sets the collectionContains filter value.
     *
     * @param collectionContains The element that the collection must contain
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setCollectionContains(E collectionContains) {
        this.collectionContains = collectionContains;
        return this;
    }

    /**
     * Gets the collectionAny filter value.
     * 
     * <p>The returned filter can be either a basic type filter (e.g., StringFilter)
     * or a model type filter (e.g., UserFilter), both of which implement {@link FilterBase}.</p>
     *
     * @return The filter that any element in the collection must match
     */
    public FilterBase<E> getCollectionAny() {
        return collectionAny;
    }

    /**
     * Sets the collectionAny filter value.
     * 
     * <p>Accepts both basic type filters (e.g., StringFilter) and model type filters
     * (e.g., UserFilter) through the unified {@link FilterBase} type hierarchy.</p>
     *
     * @param collectionAny The filter that any element in the collection must match
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setCollectionAny(FilterBase<E> collectionAny) {
        this.collectionAny = collectionAny;
        return this;
    }

    /**
     * Gets the collectionAll filter value.
     * 
     * <p>The returned filter can be either a basic type filter (e.g., StringFilter)
     * or a model type filter (e.g., UserFilter), both of which implement {@link FilterBase}.</p>
     *
     * @return The filter that all elements in the collection must match
     */
    public FilterBase<E> getCollectionAll() {
        return collectionAll;
    }

    /**
     * Sets the collectionAll filter value.
     * 
     * <p>Accepts both basic type filters (e.g., StringFilter) and model type filters
     * (e.g., UserFilter) through the unified {@link FilterBase} type hierarchy.</p>
     *
     * @param collectionAll The filter that all elements in the collection must match
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setCollectionAll(FilterBase<E> collectionAll) {
        this.collectionAll = collectionAll;
        return this;
    }

    /**
     * Gets the collectionNone filter value.
     * 
     * <p>The returned filter can be either a basic type filter (e.g., StringFilter)
     * or a model type filter (e.g., UserFilter), both of which implement {@link FilterBase}.</p>
     *
     * @return The filter that no elements in the collection must match
     */
    public FilterBase<E> getCollectionNone() {
        return collectionNone;
    }

    /**
     * Sets the collectionNone filter value.
     * 
     * <p>Accepts both basic type filters (e.g., StringFilter) and model type filters
     * (e.g., UserFilter) through the unified {@link FilterBase} type hierarchy.</p>
     *
     * @param collectionNone The filter that no elements in the collection must match
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setCollectionNone(FilterBase<E> collectionNone) {
        this.collectionNone = collectionNone;
        return this;
    }

    /**
     * Gets the isEmpty filter value.
     *
     * @return True to match empty collections, false to match non-empty collections, null to ignore
     */
    public Boolean getIsEmpty() {
        return isEmpty;
    }

    /**
     * Sets the isEmpty filter value.
     *
     * @param isEmpty True to match empty collections, false to match non-empty collections, null to ignore
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setIsEmpty(Boolean isEmpty) {
        this.isEmpty = isEmpty;
        return this;
    }

    /**
     * Gets the isNotEmpty filter value.
     *
     * @return True to match non-empty collections, false to match empty collections, null to ignore
     */
    public Boolean getIsNotEmpty() {
        return isNotEmpty;
    }

    /**
     * Sets the isNotEmpty filter value.
     *
     * @param isNotEmpty True to match non-empty collections, false to match empty collections, null to ignore
     * @return This filter instance for method chaining
     */
    public CollectionFilter<E> setIsNotEmpty(Boolean isNotEmpty) {
        this.isNotEmpty = isNotEmpty;
        return this;
    }

    @Override
    public CollectionFilter<E> setEquals(Collection<E> equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public CollectionFilter<E> setNotEquals(Collection<E> notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public CollectionFilter<E> setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public CollectionFilter<E> setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public CollectionFilter<E> setIn(Collection<Collection<E>> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public CollectionFilter<E> setNotIn(Collection<Collection<E>> notIn) {
        super.setNotIn(notIn);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CollectionFilter<?> that = (CollectionFilter<?>) o;
        return Objects.equals(collectionContains, that.collectionContains) &&
                Objects.equals(collectionAny, that.collectionAny) &&
                Objects.equals(collectionAll, that.collectionAll) &&
                Objects.equals(collectionNone, that.collectionNone) &&
                Objects.equals(isEmpty, that.isEmpty) &&
                Objects.equals(isNotEmpty, that.isNotEmpty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collectionContains, collectionAny, collectionAll, collectionNone, isEmpty, isNotEmpty);
    }

    @Override
    public String toString() {
        return "CollectionFilter{" +
                "equals=" + getEquals() +
                ", notEquals=" + getNotEquals() +
                ", isNull=" + getIsNull() +
                ", isNotNull=" + getIsNotNull() +
                ", in=" + getIn() +
                ", notIn=" + getNotIn() +
                ", collectionContains=" + collectionContains +
                ", collectionAny=" + collectionAny +
                ", collectionAll=" + collectionAll +
                ", collectionNone=" + collectionNone +
                ", isEmpty=" + isEmpty +
                ", isNotEmpty=" + isNotEmpty +
                '}';
    }
}