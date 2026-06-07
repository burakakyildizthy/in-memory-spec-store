package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.FieldDeserializationConfig;

import java.util.List;

/**
 * Interface for handling comma-separated parameter values and converting them to typed collections.
 * Used by Spring parameter binding to support collection operations (in, nin) in query parameters.
 *
 * <p>Example usage:
 * <pre>
 * // Query parameter: status.in=ACTIVE,PENDING,COMPLETED
 * List&lt;UserStatus&gt; statuses = handler.parseCommaSeparatedValues(
 *     "ACTIVE,PENDING,COMPLETED",
 *     UserStatus.class,
 *     config
 * );
 * </pre>
 */
public interface CollectionParameterHandler {

    /**
     * Parses a comma-separated string into a typed list.
     * Each element is deserialized using the shared FilterValueDeserializer.
     *
     * @param value       The comma-separated string value
     * @param elementType The type of elements in the collection
     * @param config      The deserialization configuration for the element type
     * @param <T>         The element type
     * @return A list of deserialized values
     * @throws IllegalArgumentException if any element cannot be deserialized
     */
    <T> List<T> parseCommaSeparatedValues(String value, Class<T> elementType, FieldDeserializationConfig config);

    /**
     * Checks if the given operator name represents a collection operation.
     * Collection operations include: in, nin (not in).
     *
     * @param operatorName The operator name from FilterConstants
     * @return True if the operator is a collection operation, false otherwise
     */
    boolean isCollectionOperation(String operatorName);

    /**
     * Creates a new instance of the specified element filter class.
     * Used for model type collection filtering where element filters need to be instantiated.
     *
     * @param elementFilterClass The filter class to instantiate
     * @param <F>                The filter type
     * @return A new instance of the element filter
     * @throws IllegalArgumentException if the filter cannot be instantiated
     */
    <F> F createElementFilter(Class<F> elementFilterClass);

    /**
     * Checks if the given element type is a model type (has a filter class).
     * Model types have their own filter classes (e.g., UserFilter for User entity).
     * Basic types use standard filter classes (StringFilter, IntegerFilter, etc.).
     *
     * @param elementType The element type class to check
     * @return True if the element type is a model type, false for basic types
     */
    boolean isModelElementType(Class<?> elementType);
}
