package com.thy.fss.common.inmemory.filter;

/**
 * Base marker interface for all filter types.
 * 
 * <p>This interface enables type compatibility between basic type filters ({@link Filter})
 * and model type filters ({@link EntityFilter}) in generic contexts such as 
 * {@link CollectionFilter}.</p>
 * 
 * <p>The primary purpose of this interface is to provide a unified type hierarchy that allows
 * {@code CollectionFilter<E>} to work seamlessly with both basic types (String, Integer, etc.)
 * and model types (User, Address, etc.) using a single {@code FilterBase<E>} type parameter.</p>
 * 
 * <h2>Type Hierarchy</h2>
 * <pre>
 * FilterBase&lt;T&gt; (marker interface)
 * ├── Filter&lt;T&gt; (abstract class) implements FilterBase&lt;T&gt;
 * │   ├── StringFilter extends Filter&lt;String&gt;
 * │   ├── IntegerFilter extends Filter&lt;Integer&gt;
 * │   └── CollectionFilter&lt;E&gt; extends Filter&lt;Collection&lt;E&gt;&gt;
 * │
 * └── EntityFilter&lt;T&gt; (interface) extends FilterBase&lt;T&gt;
 *     ├── UserFilter implements EntityFilter&lt;User&gt;
 *     └── AddressFilter implements EntityFilter&lt;Address&gt;
 * </pre>
 * 
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Type Compatibility:</b> Enables {@code CollectionFilter<E>} to accept both basic 
 *       and model type filters through a common {@code FilterBase<E>} type</li>
 *   <li><b>Type Safety:</b> Maintains compile-time type safety while providing flexibility</li>
 *   <li><b>Backward Compatibility:</b> No breaking changes to existing code - all existing 
 *       filters automatically implement this interface</li>
 *   <li><b>Minimal Impact:</b> Marker interface with no methods - purely for type compatibility</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * Basic Type Collections
 * <pre>{@code
 * // CollectionFilter with basic type (String)
 * CollectionFilter<String> tagsFilter = new CollectionFilter<>();
 * StringFilter stringFilter = new StringFilter();
 * stringFilter.setEquals("important");
 * tagsFilter.setCollectionAny(stringFilter);  // ✓ Works - StringFilter implements FilterBase<String>
 * }</pre>
 * 
 * Model Type Collections
 * <pre>{@code
 * // CollectionFilter with model type (User)
 * CollectionFilter<User> usersFilter = new CollectionFilter<>();
 * UserFilter userFilter = new UserFilter();
 * userFilter.getName().setEquals("John");
 * usersFilter.setCollectionAny(userFilter);  // ✓ Works - UserFilter implements FilterBase<User>
 * }</pre>
 * 
 * Type-Safe Casting
 * <pre>{@code
 * FilterBase<User> elementFilter = usersFilter.getCollectionAny();
 * if (elementFilter instanceof UserFilter) {
 *     UserFilter userFilter = (UserFilter) elementFilter;
 *     // Type-safe operations on UserFilter
 * } else if (elementFilter instanceof Filter) {
 *     Filter<User> filter = (Filter<User>) elementFilter;
 *     // Basic filter operations
 * }
 * }</pre>
 * 
 * <h2>Implementation Notes</h2>
 * <p>This is a marker interface with no methods. It exists solely to provide a common type
 * that both {@link Filter} and {@link EntityFilter} can implement/extend, enabling type
 * compatibility in generic contexts.</p>
 * 
 * <p>All filter classes automatically implement this interface through their parent types:</p>
 * <ul>
 *   <li>Basic type filters (StringFilter, IntegerFilter, etc.) inherit it from {@link Filter}</li>
 *   <li>Model type filters (UserFilter, AddressFilter, etc.) inherit it from {@link EntityFilter}</li>
 * </ul>
 * 
 * @param <T> The type being filtered
 * @since 1.0
 * @see Filter
 * @see EntityFilter
 * @see CollectionFilter
 */
public interface FilterBase<T> {
    // Marker interface - no methods
    // Provides type compatibility between Filter<T> and EntityFilter<T>
}
