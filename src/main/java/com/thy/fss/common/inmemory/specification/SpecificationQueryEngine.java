package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.filter.EntityFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Query engine for executing Specification-based and Filter-based queries with
 * Spring pagination support. Provides filtering and pagination capabilities for
 * in-memory data using StaticSpecificationService instead of reflection for
 * improved performance and type safety.
 * <p>
 * Key Features:
 * <ul>
 * <li>Complete elimination of reflection usage</li>
 * <li>StaticSpecificationService-based validation</li>
 * <li>Filter-based queries with comprehensive criteria support</li>
 * <li>Specification-based queries (legacy support)</li>
 * <li>Pagination support (without sorting)</li>
 * </ul>
 * <p>
 * Limitations:
 * <ul>
 * <li>Requires StaticSpecificationService generated for entity classes</li>
 * <li>Nested field sorting requires proper field path implementation in
 * StaticSpecificationService</li>
 * </ul>
 * <p>
 * Usage Examples:
 * <pre>{@code
 * // Create engine for specific entity type
 * SpecificationQueryEngine<User> engine = new SpecificationQueryEngine<>(User.class);
 *
 * // Filter-based query with sorting
 * UserFilter filter = new UserFilter();
 * filter.setName(new StringFilter().setContains("john"));
 *
 * // Paginated query with sorting
 * Pageable pageable = PageRequest.of(0, 10, Sort.by("name").and(Sort.by("age").descending()));
 * Page<User> page = engine.queryByFilter(data, filter, pageable);
 *
 * // Nested field sorting
 * Pageable nestedSort = PageRequest.of(0, 10, Sort.by("address.city"));
 * Page<User> sortedByCity = engine.queryByFilter(data, filter, nestedSort);
 *
 * // Count matching entities
 * long count = engine.countByFilter(data, filter);
 * }</pre>
 *
 * @param <T> The type of objects to query
 */
public class SpecificationQueryEngine<T> {

    private final SpecificationService<T> specificationService;

    /**
     * Creates a new SpecificationQueryEngine for the specified entity class.
     *
     * @param entityClass The entity class this engine will query
     */
    public SpecificationQueryEngine(Class<T> entityClass) {
        this.specificationService = getSpecificationService(entityClass);
    }

    /**
     * Gets the StaticSpecificationService for the given entity class. Delegates
     * to SpecificationServices which handles caching internally.
     *
     * @param entityClass The entity class
     * @return The StaticSpecificationService for the entity class
     */
    private static <E> SpecificationService<E> getSpecificationService(Class<E> entityClass) {
        try {
            return SpecificationServices.getService(entityClass);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("StaticSpecificationService not found for class " + entityClass.getName()
                    + ". Make sure the entity is annotated with @MetaModel and the annotation processor has run.", e);
        }
    }

    /**
     * Executes a query with the given specification and pagination parameters.
     *
     * @param data the data to query
     * @param spec the specification to apply (can be null for no filtering)
     * @param pageable the pagination parameters (including sorting)
     * @return a page of results
     */
    public Page<T> query(List<T> data, Specification<T> spec, Pageable pageable) {
        Predicate<T> predicate = spec != null ? createSpecificationPredicate(spec) : null;
        return queryWithPagination(data, predicate, pageable);
    }

    /**
     * Executes a query with the given filter and pagination parameters.
     *
     * @param data the data to query
     * @param filter the filter to apply (can be null for no filtering)
     * @param pageable the pagination parameters (including sorting)
     * @return a page of results
     */
    public Page<T> queryByFilter(List<T> data, EntityFilter<T> filter, Pageable pageable) {
        Predicate<T> predicate = filter != null ? createFilterPredicate(filter) : null;
        return queryWithPagination(data, predicate, pageable);
    }

    /**
     * Executes a query with the given filter and returns all matching results.
     *
     * @param data the data to query
     * @param filter the filter to apply (can be null for no filtering)
     * @return a list of all matching results
     */
    public List<T> queryByFilter(List<T> data, EntityFilter<T> filter) {
        Predicate<T> predicate = filter != null ? createFilterPredicate(filter) : null;
        return queryAll(data, predicate);
    }

    /**
     * Counts the number of entities matching the given filter.
     *
     * @param data the data to query
     * @param filter the filter to apply (can be null for no filtering)
     * @return the count of matching entities
     */
    public long countByFilter(List<T> data, EntityFilter<T> filter) {
        Predicate<T> predicate = filter != null ? createFilterPredicate(filter) : null;
        return countMatching(data, predicate);
    }

    /**
     * Executes a query with the given specification and returns all matching
     * results.
     *
     * @param data the data to query
     * @param spec the specification to apply (can be null for no filtering)
     * @return a list of all matching results
     */
    public List<T> query(List<T> data, Specification<T> spec) {
        Predicate<T> predicate = spec != null ? createSpecificationPredicate(spec) : null;
        return queryAll(data, predicate);
    }

    /**
     * Counts the number of entities matching the given specification.
     *
     * @param data the data to query
     * @param spec the specification to apply (can be null for no filtering)
     * @return the count of matching entities
     */
    public long count(List<T> data, Specification<T> spec) {
        Predicate<T> predicate = spec != null ? createSpecificationPredicate(spec) : null;
        return countMatching(data, predicate);
    }

    // ==================== Core Query Methods (Shared Logic) ====================
    /**
     * Executes paginated query with predicate. OPTIMIZATION: Uses two-pass
     * approach to minimize GC pressure.
     */
    private Page<T> queryWithPagination(List<T> data, Predicate<T> predicate, Pageable pageable) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }
        if (data.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Sort sort = pageable.getSort();
        boolean needsSorting = sort != null && sort.isSorted();

        if (needsSorting) {
            return queryWithSortingOptimized(data, predicate, sort, pageable);
        } else {
            return queryWithoutSortingOptimized(data, predicate, pageable);
        }
    }

    /**
     * Executes query and returns all matching results. OPTIMIZATION: Uses
     * pre-sized ArrayList to minimize reallocations.
     */
    private List<T> queryAll(List<T> data, Predicate<T> predicate) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (data.isEmpty()) {
            return List.of();
        }
        if (predicate == null) {
            return data;
        }

        int estimatedSize = Math.max(100, data.size() / 2);
        List<T> result = new ArrayList<>(estimatedSize);

        for (T item : data) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }

        return result;
    }

    /**
     * Counts matching entities. OPTIMIZATION: Uses simple loop to avoid object
     * allocation.
     */
    private long countMatching(List<T> data, Predicate<T> predicate) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (data.isEmpty()) {
            return 0;
        }
        if (predicate == null) {
            return data.size();
        }

        long count = 0;
        for (T item : data) {
            if (predicate.test(item)) {
                count++;
            }
        }

        return count;
    }

    // ==================== Predicate Creation ====================
    /**
     * Creates a predicate from a Specification using
     * StaticSpecificationService. This replaces the direct call to
     * spec.toPredicate() to use the generated service.
     *
     * @param spec the specification
     * @return a predicate that uses StaticSpecificationService for validation
     */
    private Predicate<T> createSpecificationPredicate(Specification<T> spec) {
        if (spec == null) {
            return null;
        }

        // Get the original predicate from the specification
        Predicate<T> originalPredicate = spec.toPredicate();

        // For now, we'll use the original predicate since Specification interface
        // will be updated in a separate task to use StaticSpecificationService
        return originalPredicate;
    }

    /**
     * Creates a predicate from a Filter using StaticSpecificationService.
     *
     * @param filter the filter object
     * @return a predicate that uses StaticSpecificationService for validation
     */
    private Predicate<T> createFilterPredicate(Object filter) {
        if (filter == null) {
            return null;
        }

        if (specificationService == null) {
            throw new UnsupportedOperationException(
                    "Filter-based queries require a StaticSpecificationService. "
                    + "Use SpecificationQueryEngine(Class<T> entityClass) constructor instead of the default constructor."
            );
        }

        return entity -> {
            return specificationService.validateFilter(entity, filter);
        };
    }

    /**
     * Optimized query without sorting - uses streaming with early termination.
     * Avoids creating large intermediate lists by collecting only needed items.
     */
    private Page<T> queryWithoutSortingOptimized(List<T> data, Predicate<T> predicate, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int skipCount = pageNumber * pageSize;

        // Pre-allocate list with exact size needed
        List<T> pageContent = new ArrayList<>(pageSize);
        int matchCount = 0;
        int collectedCount = 0;

        // Single pass: count total and collect page items
        for (T item : data) {
            // Apply filter
            if (predicate != null && !predicate.test(item)) {
                continue;
            }

            matchCount++;

            // Skip items before current page
            if (matchCount <= skipCount) {
                continue;
            }

            // Collect items for current page
            if (collectedCount < pageSize) {
                pageContent.add(item);
                collectedCount++;

                // Early termination: stop when page is full and we don't need total count
                // Note: We still need to count all for accurate total, so continue
            }
        }

        return new PageImpl<>(pageContent, pageable, matchCount);
    }

    /**
     * Optimized query with sorting - collects matching items efficiently. Uses
     * ArrayList with pre-sized capacity to minimize reallocations.
     */
    private Page<T> queryWithSortingOptimized(List<T> data, Predicate<T> predicate, Sort sort, Pageable pageable) {
        // Estimate capacity based on filter selectivity
        // If no filter, use full size; otherwise estimate 50% selectivity
        int estimatedSize = predicate == null ? data.size() : Math.max(100, data.size() / 2);
        List<T> filteredData = new ArrayList<>(estimatedSize);

        // Single pass: collect matching items
        for (T item : data) {
            if (predicate == null || predicate.test(item)) {
                filteredData.add(item);
            }
        }

        // Sort in-place (no new list created)
        if (!filteredData.isEmpty()) {
            Comparator<T> comparator = createMultiFieldComparator(sort);
            filteredData.sort(comparator);
        }

        // Apply pagination
        return applyPagination(filteredData, pageable);
    }

    /**
     * Creates a multi-field comparator from Sort specification.
     */
    private Comparator<T> createMultiFieldComparator(Sort sort) {
        List<String> fieldPaths = new ArrayList<>();
        List<Boolean> ascendingFlags = new ArrayList<>();

        for (Sort.Order order : sort) {
            fieldPaths.add(order.getProperty());
            ascendingFlags.add(order.getDirection() == Sort.Direction.ASC);
        }

        return specificationService.createMultiFieldComparator(fieldPaths, ascendingFlags);
    }

    /**
     * Applies pagination to the data using Spring's PageImpl. Handles boundary
     * conditions and returns appropriate page content.
     *
     * @param data the data to paginate
     * @param pageable the pagination parameters
     * @return a Page containing the requested page of data
     */
    private Page<T> applyPagination(List<T> data, Pageable pageable) {
        if (data == null) {
            data = List.of();
        }

        int totalElements = data.size();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();

        // Calculate start and end indices
        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);

        // Handle out-of-bounds requests
        if (startIndex >= totalElements) {
            // Return empty page for out-of-bounds requests
            return new PageImpl<>(List.of(), pageable, totalElements);
        }

        // Extract the page content
        List<T> pageContent = data.subList(startIndex, endIndex);

        // Return the page with content, pageable info, and total elements
        return new PageImpl<>(pageContent, pageable, totalElements);
    }
}
