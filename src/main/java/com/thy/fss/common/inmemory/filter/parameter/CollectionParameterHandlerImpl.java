package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.filter.deserializer.FieldDeserializationConfig;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CollectionParameterHandler that parses comma-separated values
 * into typed collections using the shared FilterValueDeserializer.
 *
 * <p>Supports all filter types:
 * <ul>
 *   <li>String - e.g., "john,jane,bob"</li>
 *   <li>LocalDateTime - e.g., "2024-01-01T00:00:00,2024-12-31T23:59:59"</li>
 *   <li>LocalDate - e.g., "2024-01-01,2024-12-31"</li>
 *   <li>Instant - e.g., "2024-01-01T00:00:00.000Z,2024-12-31T23:59:59.999Z"</li>
 *   <li>Enum - e.g., "ACTIVE,PENDING,COMPLETED"</li>
 *   <li>Integer - e.g., "1,2,3,4,5"</li>
 *   <li>Long - e.g., "100,200,300"</li>
 *   <li>Double - e.g., "10.5,20.75,30.25"</li>
 *   <li>Boolean - e.g., "true,false,true"</li>
 * </ul>
 */
public class CollectionParameterHandlerImpl implements CollectionParameterHandler {

    private final FilterValueDeserializer valueDeserializer;

    /**
     * Constructs a CollectionParameterHandlerImpl with the specified value deserializer.
     *
     * @param valueDeserializer The FilterValueDeserializer to use for element conversion
     */
    public CollectionParameterHandlerImpl(FilterValueDeserializer valueDeserializer) {
        if (valueDeserializer == null) {
            throw new IllegalArgumentException("FilterValueDeserializer cannot be null");
        }
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public <T> List<T> parseCommaSeparatedValues(String value, Class<T> elementType, FieldDeserializationConfig config) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Split by comma
        String[] elements = value.split(",");

        // Pre-size the list for performance (use typical size or actual size)
        List<T> result = new ArrayList<>(Math.max(elements.length, FilterConstants.TYPICAL_IN_SIZE));

        // Deserialize each element using the shared deserializer
        for (String element : elements) {
            String trimmedElement = element.trim();
            if (!trimmedElement.isEmpty()) {
                try {
                    T deserializedValue = valueDeserializer.deserializeValue(trimmedElement, elementType, config);
                    result.add(deserializedValue);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            String.format("Cannot parse collection element '%s' as %s: %s",
                                    trimmedElement,
                                    elementType.getSimpleName(),
                                    e.getMessage()),
                            e
                    );
                }
            }
        }

        return result;
    }

    @Override
    public boolean isCollectionOperation(String operatorName) {
        if (operatorName == null) {
            return false;
        }

        // Check against FilterConstants collection operators
        return FilterConstants.FIELD_IN.equals(operatorName)
                || FilterConstants.FIELD_NIN.equals(operatorName);
    }

    @Override
    public <F> F createElementFilter(Class<F> elementFilterClass) {
        if (elementFilterClass == null) {
            throw new IllegalArgumentException("Element filter class cannot be null");
        }

        try {
            // Create new instance using default constructor
            return elementFilterClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    String.format("Element filter class %s does not have a default constructor",
                            elementFilterClass.getSimpleName()),
                    e
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Cannot instantiate element filter class %s: %s",
                            elementFilterClass.getSimpleName(),
                            e.getMessage()),
                    e
            );
        }
    }

    @Override
    public boolean isModelElementType(Class<?> elementType) {
        if (elementType == null) {
            return false;
        }

        // Check if element type has a filter class
        // Model types have filter classes in the same package with naming convention: {TypeName}Filter
        String filterClassName = elementType.getName() + "Filter";
        
        try {
            // Try to load the filter class
            Class.forName(filterClassName);
            return true;
        } catch (ClassNotFoundException e) {
            // Filter class not found - this is a basic type
            return false;
        }
    }
}
