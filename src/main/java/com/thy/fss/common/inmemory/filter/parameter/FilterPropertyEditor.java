package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FieldDeserializationConfig;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.List;

/**
 * Generic PropertyEditor implementation for Spring parameter binding of filter field values.
 * This editor handles both single values and collections, delegating value conversion to
 * the shared FilterValueDeserializer to ensure consistency with JSON deserialization.
 *
 * <p>Supports all filter types:
 * <ul>
 *   <li>Single values: String, LocalDateTime, LocalDate, Instant, Enum, Integer, Long, Double, Boolean</li>
 *   <li>Collections: Comma-separated values for any of the above types</li>
 * </ul>
 *
 * <p>Example usage in Spring parameter binding:
 * <pre>
 * // Single value: ?name.eq=john
 * // Collection: ?status.in=ACTIVE,PENDING,COMPLETED
 * // Date: ?createdDate.gte=2024-01-01T00:00:00
 * </pre>
 */
public class FilterPropertyEditor extends PropertyEditorSupport {

    private final FilterValueDeserializer deserializer;
    private final CollectionParameterHandler collectionHandler;
    private final DeserializerRegistry registry;
    private final Class<?> targetType;
    private final Class<?> elementType; // For collections

    /**
     * Constructs a FilterPropertyEditor for a single value type.
     *
     * @param deserializer      The value deserializer to use
     * @param collectionHandler The collection parameter handler
     * @param registry          The deserializer registry for configuration lookup
     * @param targetType        The target type for this editor
     */
    public FilterPropertyEditor(FilterValueDeserializer deserializer,
                                CollectionParameterHandler collectionHandler,
                                DeserializerRegistry registry,
                                Class<?> targetType) {
        this(deserializer, collectionHandler, registry, targetType, null);
    }

    /**
     * Constructs a FilterPropertyEditor for a collection type.
     *
     * @param deserializer      The value deserializer to use
     * @param collectionHandler The collection parameter handler
     * @param registry          The deserializer registry for configuration lookup
     * @param targetType        The target collection type
     * @param elementType       The element type for collections (null for non-collection types)
     */
    public FilterPropertyEditor(FilterValueDeserializer deserializer,
                                CollectionParameterHandler collectionHandler,
                                DeserializerRegistry registry,
                                Class<?> targetType,
                                Class<?> elementType) {
        if (deserializer == null) {
            throw new IllegalArgumentException("FilterValueDeserializer cannot be null");
        }
        if (collectionHandler == null) {
            throw new IllegalArgumentException("CollectionParameterHandler cannot be null");
        }
        if (registry == null) {
            throw new IllegalArgumentException("DeserializerRegistry cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }

        this.deserializer = deserializer;
        this.collectionHandler = collectionHandler;
        this.registry = registry;
        this.targetType = targetType;
        this.elementType = elementType;
    }

    /**
     * Handles conversion of single (non-collection) values.
     */
    private void handleSingleValue(String text) {
        try {
            FieldDeserializationConfig config = registry.getConfigForType(targetType);
            Object value = deserializer.deserializeValue(text, targetType, config);
            setValue(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse %s value '%s': %s",
                            targetType.getSimpleName(),
                            text,
                            e.getMessage()),
                    e
            );
        }
    }

    /**
     * Handles conversion of collection values (comma-separated).
     */
    private void handleCollectionValue(String text) {
        if (elementType == null) {
            throw new IllegalArgumentException(
                    "Element type must be specified for collection conversion"
            );
        }

        try {
            FieldDeserializationConfig config = registry.getConfigForType(elementType);
            List<?> values = collectionHandler.parseCommaSeparatedValues(text, elementType, config);
            setValue(values);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse collection of %s from value '%s': %s",
                            elementType.getSimpleName(),
                            text,
                            e.getMessage()),
                    e
            );
        }
    }

    /**
     * Checks if the given type is a collection type.
     */
    private boolean isCollectionType(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || List.class.isAssignableFrom(type);
    }

    /**
     * Returns the string representation of the current value.
     * This is primarily used for debugging and logging.
     */
    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /**
     * Converts a string representation to the target type.
     * Handles both single values and comma-separated collections.
     *
     * @param text The string value to convert
     * @throws IllegalArgumentException if the value cannot be converted
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null) {
            setValue(null);
            return;
        }

        try {
            // Handle collections (comma-separated values)
            if (isCollectionType(targetType)) {
                handleCollectionValue(text);
            } else {
                // Handle single values
                handleSingleValue(text);
            }
        } catch (IllegalArgumentException e) {
            // Re-throw with context
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions with meaningful message
            throw new IllegalArgumentException(
                    String.format("Failed to convert value '%s' to type %s: %s",
                            text,
                            targetType.getSimpleName(),
                            e.getMessage()),
                    e
            );
        }
    }
}
