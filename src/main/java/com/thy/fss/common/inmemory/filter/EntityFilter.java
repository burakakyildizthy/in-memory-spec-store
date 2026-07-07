package com.thy.fss.common.inmemory.filter;

/**
 * Marker interface for all entity filter classes.
 * This interface provides type safety for filter operations in InMemoryDataStore.
 * 
 * <p>This interface extends {@link FilterBase} to provide a unified type hierarchy
 * that enables type compatibility with {@link Filter} in generic contexts such as
 * {@link CollectionFilter}. This allows {@code CollectionFilter<E>} to work seamlessly
 * with both basic type filters and model type filters using a single {@code FilterBase<E>}
 * type parameter.</p>
 *
 * <p>All generated FilterMetaModel classes should implement this interface
 * to ensure type-safe usage with InMemoryDataStore filter methods.</p>
 * 
 * <h2>Unified Type Hierarchy</h2>
 * <p>By extending {@link FilterBase}, this interface enables model type filters to be
 * used interchangeably with basic type filters in collection filtering scenarios:</p>
 * <pre>
 * FilterBase&lt;T&gt; (marker interface)
 * ├── Filter&lt;T&gt; (abstract class) - for basic types
 * └── EntityFilter&lt;T&gt; (interface) - for model types
 * </pre>
 * 
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Type Compatibility:</b> Model filters can be used in {@code CollectionFilter<E>}
 *       alongside basic type filters</li>
 *   <li><b>Type Safety:</b> Maintains compile-time type safety through generic type parameter</li>
 *   <li><b>Backward Compatibility:</b> No breaking changes - existing code continues to work</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * Basic Usage
 * <pre>{@code
 * // Generated filter classes implement this interface
 * public class UserFilter implements EntityFilter<User> {
 *     private StringFilter name;
 *     private IntegerFilter age;
 *     // ... other filter fields
 *     
 *     @Override
 *     public Class<User> getEntityClass() {
 *         return User.class;
 *     }
 * }
 *
 * // Type-safe usage in InMemoryDataStore
 * InMemoryDataStore<User> store = ...;
 * UserFilter filter = new UserFilter();
 * Page<User> results = store.findAllByFilter(filter, pageable);
 * }</pre>
 * 
 * Collection Filtering with Model Types
 * <pre>{@code
 * // CollectionFilter can now work with model type filters
 * CollectionFilter<User> usersFilter = new CollectionFilter<>();
 * UserFilter userFilter = new UserFilter();
 * userFilter.getName().setEquals("John");
 * usersFilter.setCollectionAny(userFilter);  // ✓ Works - UserFilter implements FilterBase<User>
 * }</pre>
 *
 * @param <T> The entity type that this filter applies to
 * @since 1.0
 * @see FilterBase
 * @see Filter
 * @see CollectionFilter
 */
public interface EntityFilter<T> extends FilterBase<T> {

    /**
     * Returns the entity class that this filter applies to.
     * This method is used for type validation and should be implemented
     * by all concrete filter classes.
     *
     * @return the entity class
     */
    default Class<T> getEntityClass() {
        // This will be overridden by generated filter classes
        // to return the actual entity class
        throw new UnsupportedOperationException(
                "getEntityClass() must be implemented by concrete filter classes");
    }
}