package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base implementation of SpecificationService providing default implementations
 * for collection operations. Generated SpecificationService classes should
 * extend this class instead of directly implementing the SpecificationService
 * interface to inherit common functionality.
 *
 * <h2>Version 2.0 Changes - Delegation Pattern</h2>
 * <p>
 * This class uses a delegation pattern for path navigation to eliminate runtime
 * type lookups. Generated services implement abstract delegation methods with
 * direct INSTANCE references to nested services.
 * </p>
 *
 * How It Works:
 * <ol>
 * <li>Base class provides template methods for path navigation</li>
 * <li>Generated services implement delegation methods for their specific fields</li>
 * <li>Delegation methods use direct INSTANCE references (no runtime lookups)</li>
 * </ol>
 *
 * Example Generated Code:
 * <pre>{@code
 * public class UserSpecificationService extends BaseSpecificationService<User> {
 *
 *     // Delegation method for nested model fields
 *     @Override
 *     protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr,
 *                                     List<MetaAttribute<?, ?>> path, int nextIndex) {
 *         if (attr == User_.profile) {
 *             // Direct INSTANCE reference - NO RUNTIME LOOKUP!
 *             return ProfileSpecificationService.INSTANCE
 *                 .getValueByPathImpl((Profile) fieldValue, path, nextIndex);
 *         }
 *         throw new IllegalArgumentException("Unknown nested field: " + attr.getName());
 *     }
 *
 *     // Similar delegation for setValueByPath and createInstance
 * }
 * }</pre>
 *
 * @param <T> The entity type this service validates
 * @since 2.0
 */
public abstract class BaseSpecificationService<T> implements SpecificationService<T> {

    private static final Logger logger = LoggerFactory.getLogger(BaseSpecificationService.class);

    /**
     * Default implementation of extractFromCollection. Extracts value(s) from a
     * collection based on the specified selector.
     *
     * @param collection The collection to extract from
     * @param selector The collection selector (ALL, FIRST, LAST, ANY)
     * @return The extracted value(s) - can be a single element or a collection
     * depending on selector
     */
    @Override
    public Object extractFromCollection(Collection<?> collection, CollectionSelector selector) {
        if (collection == null || collection.isEmpty()) {
            return selector == CollectionSelector.ALL ? new ArrayList<>() : null;
        }

        return switch (selector) {
            case ALL ->
                new ArrayList<>(collection);
            case FIRST ->
                collection.iterator().next();
            case LAST -> {
                Object last = null;
                for (Object element : collection) {
                    last = element;
                }
                yield last;
            }
            case ANY ->
                collection.iterator().next(); // Return first element for consistency
        };
    }

    /**
     * Default implementation of extractFromCollection with specification filtering.
     * Filters the collection using the specification, then applies the selector.
     *
     * @param collection The collection to extract from
     * @param selector The collection selector (ALL, FIRST, LAST, ANY)
     * @param specification The specification to filter elements (can be null)
     * @return The extracted value(s) after filtering and selection
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object extractFromCollection(
            Collection<T> collection,
            CollectionSelector selector,
            Specification<T> specification) {
        
        if (collection == null || collection.isEmpty()) {
            return selector == CollectionSelector.ALL ? new ArrayList<>() : null;
        }

        // Filter by specification if provided
        java.util.stream.Stream<T> stream = collection.stream();
        if (specification != null) {
            stream = stream.filter(specification::test);
        }

        // Apply selector
        return switch (selector) {
            case ALL ->
                stream.toList();
            case FIRST ->
                stream.findFirst().orElse(null);
            case LAST -> {
                List<T> list = stream.toList();
                yield list.isEmpty() ? null : list.get(list.size() - 1);
            }
            case ANY ->
                stream.findAny().orElse(null);
        };
    }

    /**
     * Default implementation of getCollectionSize. Gets the size of a
     * collection.
     *
     * @param collection The collection to get size from
     * @return The size of the collection, or 0 if collection is null
     */
    @Override
    public int getCollectionSize(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * Default implementation of isCollectionEmpty. Checks if a collection is
     * empty.
     *
     * @param collection The collection to check
     * @return true if collection is null or empty, false otherwise
     */
    @Override
    public boolean isCollectionEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    // ==================== DELEGATION METHODS FOR PATH NAVIGATION ====================

    /**
     * Navigates to a nested field using direct service references.
     * Generated services implement this method with direct INSTANCE references
     * to avoid runtime type lookups.
     *
     * <p>Example generated implementation:</p>
     * <pre>{@code
     * protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr,
     *                                 List<MetaAttribute<?, ?>> path, int nextIndex) {
     *     if (attr == User_.profile) {
     *         return ProfileSpecificationService.INSTANCE
     *             .getValueByPathImpl((Profile) fieldValue, path, nextIndex);
     *     }
     *     if (attr == User_.address) {
     *         return AddressSpecificationService.INSTANCE
     *             .getValueByPathImpl((Address) fieldValue, path, nextIndex);
     *     }
     *     throw new IllegalArgumentException("Unknown nested field: " + attr.getName());
     * }
     * }</pre>
     *
     * @param fieldValue The current field value to navigate from
     * @param attr The meta attribute representing the current field
     * @param path The complete path being navigated
     * @param nextIndex The next index in the path to process
     * @return The value at the end of the path navigation
     */
    protected abstract Object navigateNested(Object fieldValue,
                                            MetaAttribute<?, ?> attr,
                                            List<MetaAttribute<?, ?>> path,
                                            int nextIndex);

    /**
     * Navigates to a nested field for setting a value using direct service references.
     * Generated services implement this method with direct INSTANCE references
     * to avoid runtime type lookups.
     *
     * <p>Example generated implementation:</p>
     * <pre>{@code
     * protected void navigateNestedForSet(Object fieldValue, MetaAttribute<?, ?> attr,
     *                                     List<MetaAttribute<?, ?>> path, int nextIndex, Object value) {
     *     if (attr == User_.profile) {
     *         ProfileSpecificationService.INSTANCE
     *             .setValueByPathImpl((Profile) fieldValue, path, nextIndex, value);
     *         return;
     *     }
     *     if (attr == User_.address) {
     *         AddressSpecificationService.INSTANCE
     *             .setValueByPathImpl((Address) fieldValue, path, nextIndex, value);
     *         return;
     *     }
     *     throw new IllegalArgumentException("Unknown nested field: " + attr.getName());
     * }
     * }</pre>
     *
     * @param fieldValue The current field value to navigate from
     * @param attr The meta attribute representing the current field
     * @param path The complete path being navigated
     * @param nextIndex The next index in the path to process
     * @param value The value to set at the end of the path
     */
    protected abstract void navigateNestedForSet(Object fieldValue,
                                                MetaAttribute<?, ?> attr,
                                                List<MetaAttribute<?, ?>> path,
                                                int nextIndex,
                                                Object value);

    /**
     * Creates an intermediate instance for a field using direct service references.
     * Generated services implement this method with direct INSTANCE references
     * to avoid runtime type lookups.
     *
     * <p>Example generated implementation:</p>
     * <pre>{@code
     * protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {
     *     if (attr == User_.profile) {
     *         return ProfileSpecificationService.INSTANCE.createInstance();
     *     }
     *     if (attr == User_.address) {
     *         return AddressSpecificationService.INSTANCE.createInstance();
     *     }
     *     throw new IllegalArgumentException("Unknown field: " + attr.getName());
     * }
     * }</pre>
     *
     * @param attr The meta attribute representing the field
     * @return A new instance of the field type
     */
    protected abstract Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr);

    // ==================== PATH-BASED FIELD ACCESS ====================
    /**
     * Default implementation of getValueByPath. Navigates through nested
     * properties using the provided path of MetaAttributes.
     *
     * @param entity The entity to get the value from
     * @param path The path to the field (list of MetaAttribute objects)
     * @return The field value, or null if any intermediate value is null
     */
    @Override
    public Object getValueByPath(T entity, List<MetaAttribute<?, ?>> path) {
        if (entity == null || path == null || path.isEmpty()) {
            return null;
        }

        return getValueByPathImpl(entity, path, 0);
    }

    /**
     * Implementation method for path navigation. This is a template method that
     * delegates to generated services for nested field navigation.
     *
     * @param currentValue The current value being navigated
     * @param path The complete path
     * @param index The current index in the path
     * @return The value at the end of the path
     */
    public Object getValueByPathImpl(Object currentValue, List<MetaAttribute<?, ?>> path, int index) {
        if (currentValue == null || index >= path.size()) {
            return currentValue;
        }

        MetaAttribute<?, ?> metaAttribute = path.get(index);
        Object fieldValue = getFieldValue((T) currentValue, metaAttribute);

        if (fieldValue == null || index == path.size() - 1) {
            return fieldValue;
        }

        // Delegate to generated service for nested navigation - NO RUNTIME LOOKUP!
        return navigateNested(fieldValue, metaAttribute, path, index + 1);
    }

    /**
     * Default implementation of setValueByPath. Navigates through nested
     * properties to set the final value. Automatically creates intermediate
     * objects if they are null. Supports collection fields by initializing
     * collections and adding elements.
     *
     * @param entity The entity to set the value on
     * @param path The path to the field (list of MetaAttribute objects)
     * @param value The value to set
     */
    @Override
    public void setValueByPath(T entity, List<MetaAttribute<?, ?>> path, Object value) {
        if (entity == null || path == null || path.isEmpty()) {
            return;
        }

        setValueByPathImpl(entity, path, 0, value);
    }

    /**
     * Implementation method for path-based value setting. This is a template method that
     * delegates to generated services for nested field navigation.
     *
     * @param currentValue The current value being navigated
     * @param path The complete path
     * @param index The current index in the path
     * @param value The value to set at the end of the path
     */
    @SuppressWarnings("unchecked")
    public void setValueByPathImpl(Object currentValue, List<MetaAttribute<?, ?>> path, int index, Object value) {
        if (currentValue == null || index >= path.size()) {
            return;
        }

        MetaAttribute<?, ?> metaAttribute = path.get(index);

        // If this is the last attribute in the path, set the value
        if (index == path.size() - 1) {
            // Check if final field is a collection
            if (metaAttribute.getAttributeType() == AttributeType.COLLECTION) {
                handleCollectionField(currentValue, metaAttribute, value, (SpecificationService<Object>) this);
            } else {
                // Regular field - validate type compatibility before setting
                Class<?> fieldType = metaAttribute.getFieldType();
                if (value != null && !isAssignableFrom(fieldType, value.getClass())) {
                    // Log detailed information for debugging
                    if (logger.isWarnEnabled()) {
                        logger.warn("Type mismatch detected: value type={}, field type={}, path={}, " +
                                "lastAttribute={}, currentValue type={}",
                                value.getClass().getName(),
                                fieldType.getName(),
                                formatPath(path),
                                metaAttribute.getName(),
                                currentValue.getClass().getName());
                    }

                    throw new IllegalArgumentException(
                            String.format("Type mismatch: Cannot assign value of type %s to field of type %s. " +
                                    "Field path: %s. This usually means the mapping configuration is incorrect. " +
                                    "If the target field is a DTO object, you need to navigate to a primitive field inside it.",
                                    value.getClass().getName(),
                                    fieldType.getName(),
                                    formatPath(path))
                    );
                }
                setFieldValue((T) currentValue, metaAttribute, value);
            }
            return;
        }

        // Navigate to the next level
        Object fieldValue = getFieldValue((T) currentValue, metaAttribute);

        // If intermediate value is null, create a new instance
        if (fieldValue == null) {
            fieldValue = createIntermediateInstanceForNavigation(metaAttribute, currentValue, index);
        }

        // Delegate to generated service for nested navigation - NO RUNTIME LOOKUP!
        navigateNestedForSet(fieldValue, metaAttribute, path, index + 1, value);
    }

    /**
     * Creates an intermediate instance for a null field in the path.
     * Uses delegation to generated services for type-specific instance creation.
     *
     * @param metaAttribute The meta attribute for the field
     * @param currentValue The current object
     * @param pathIndex The index in the path (for error messages)
     * @return The newly created instance
     */
    @SuppressWarnings("unchecked")
    private Object createIntermediateInstanceForNavigation(MetaAttribute<?, ?> metaAttribute,
            Object currentValue,
            int pathIndex) {
        Class<?> fieldType = metaAttribute.getFieldType();

        try {
            Object nextValue;

            // If it's a collection, create an ArrayList
            if (metaAttribute.getAttributeType() == AttributeType.COLLECTION) {
                nextValue = new ArrayList<>();
            } else {
                // Delegate to generated service - NO RUNTIME LOOKUP!
                nextValue = createIntermediateInstanceForField(metaAttribute);
            }

            // Set the newly created instance on the current object
            setFieldValue((T) currentValue, metaAttribute, nextValue);

            return nextValue;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot create instance of type " + fieldType.getName()
                    + " for intermediate path element at index " + pathIndex
                    + ". Ensure the entity has a registered SpecificationService with createInstance() method.", e
            );
        }
    }

    /**
     * Checks if a value of sourceType can be assigned to a field of targetType.
     * Handles primitive types and their wrapper classes.
     *
     * @param targetType The target field type
     * @param sourceType The source value type
     * @return true if assignment is valid
     */
    private boolean isAssignableFrom(Class<?> targetType, Class<?> sourceType) {
        // Direct assignment
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        // Handle primitive types and their wrappers
        if (targetType.isPrimitive() || sourceType.isPrimitive()) {
            return isPrimitiveCompatible(targetType, sourceType);
        }

        return false;
    }

    /**
     * Checks if primitive types are compatible (including wrapper classes).
     *
     * @param targetType The target type
     * @param sourceType The source type
     * @return true if compatible
     */
    private boolean isPrimitiveCompatible(Class<?> targetType, Class<?> sourceType) {
        // Map primitives to their wrapper classes
        Class<?> targetWrapper = getWrapperClass(targetType);
        Class<?> sourceWrapper = getWrapperClass(sourceType);

        return targetWrapper.equals(sourceWrapper);
    }

    /**
     * Gets the wrapper class for a primitive type, or returns the class itself if not primitive.
     *
     * @param clazz The class to check
     * @return The wrapper class or the original class
     */
    private Class<?> getWrapperClass(Class<?> clazz) {
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == short.class) return Short.class;
        if (clazz == char.class) return Character.class;
        return clazz;
    }

    /**
     * Formats a path for error messages.
     *
     * @param path The path to format
     * @return A string representation of the path
     */
    private String formatPath(List<MetaAttribute<?, ?>> path) {
        return path.stream()
                .map(MetaAttribute::getName)
                .reduce((a, b) -> a + "." + b)
                .orElse("<empty>");
    }

    /**
     * Handles setting a value on a collection field. Initializes the collection
     * if null, and adds the value to it.
     *
     * @param currentValue The object containing the collection field
     * @param collectionAttr The collection attribute
     * @param value The value to add to the collection
     * @param service The service for the current object
     */
    @SuppressWarnings("unchecked")
    private void handleCollectionField(Object currentValue,
            MetaAttribute<?, ?> collectionAttr,
            Object value,
            SpecificationService<Object> service) {
        // Get the current collection
        Collection<Object> collection = (Collection<Object>) service.getFieldValue(currentValue, collectionAttr);

        // If collection is null, create a new ArrayList
        if (collection == null) {
            collection = new ArrayList<>();
            service.setFieldValue(currentValue, collectionAttr, collection);
        }

        // Determine what to add to the collection
        Object elementToAdd = value;

        // If the attribute is a CollectionAttribute, check element type
        if (collectionAttr instanceof com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute) {
            com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<?, ?> colAttr
                    = (com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<?, ?>) collectionAttr;

            Class<?> elementType = colAttr.getElementType();

            // Check if element type is a boxed type (primitive wrapper) or String
            boolean isBoxedType = isBoxedType(elementType);

            if (!isBoxedType && value == null) {
                // Element type is a model and value is null - create a new instance
                try {
                    SpecificationService<Object> elementService = (SpecificationService<Object>) SpecificationServices.getService(elementType);
                    elementToAdd = elementService.createInstance();
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Cannot create instance of element type " + elementType.getName()
                            + " for collection field " + collectionAttr.getName(), e
                    );
                }
            }
        }

        // Add the element to the collection
        collection.add(elementToAdd);
    }

    /**
     * Checks if a type is a boxed type (primitive wrapper) or String.
     *
     * @param type The type to check
     * @return true if the type is a boxed type or String
     */
    private boolean isBoxedType(Class<?> type) {
        return type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Character.class
                || type == Byte.class
                || type == Short.class;
    }

    // ==================== COLLECTION OPERATIONS WITH PATH ====================

    /**
     * Default implementation of getValueByPathWithCollections.
     * Navigates through a path that may contain collection operations.
     *
     * @param entity The entity to start navigation from
     * @param path The path containing only MetaAttribute objects
     * @param collectionOperations Collection operation metadata for positions in the path
     * @return The value at the end of the path, or null if any intermediate value is null
     */
    @Override
    public Object getValueByPathWithCollections(
            T entity,
            List<MetaAttribute<?, ?>> path,
            List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
        
        if (entity == null || path == null || path.isEmpty()) {
            return null;
        }

        // Create a map of pathIndex -> CollectionOperationMetadata for quick lookup
        java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap = new java.util.HashMap<>();
        if (collectionOperations != null) {
            for (com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> op : collectionOperations) {
                operationMap.put(op.getPathIndex(), op);
            }
        }

        return getValueByPathWithCollectionsImpl(entity, path, operationMap, 0);
    }

    /**
     * Implementation method for path navigation with collection operations.
     * Uses delegation pattern to avoid runtime type lookups.
     *
     * @param currentValue The current value being navigated
     * @param path The complete path
     * @param operationMap Map of path index to collection operations
     * @param index The current index in the path
     * @return The value at the end of the path
     */
    @SuppressWarnings("unchecked")
    protected Object getValueByPathWithCollectionsImpl(
            Object currentValue,
            List<MetaAttribute<?, ?>> path,
            java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap,
            int index) {
        
        if (currentValue == null || index >= path.size()) {
            return currentValue;
        }

        MetaAttribute<?, ?> metaAttribute = path.get(index);
        
        // Get the field value
        Object fieldValue = getFieldValue((T) currentValue, metaAttribute);
        
        // Check if there's a collection operation at this index
        com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> operation = operationMap.get(index);
        
        Object nextValue;
        if (operation != null && fieldValue instanceof Collection) {
            // Apply collection operation
            nextValue = processCollectionOperation((Collection<?>) fieldValue, operation);
        } else {
            nextValue = fieldValue;
        }

        if (nextValue == null || index == path.size() - 1) {
            return nextValue;
        }

        // Delegate to generated service for nested navigation - NO RUNTIME LOOKUP!
        return navigateNestedWithCollections(nextValue, metaAttribute, path, operationMap, index + 1);
    }

    /**
     * Navigates to nested fields with collection operations using direct service references.
     * Generated services should override this to provide direct INSTANCE references.
     * Default implementation uses the delegation pattern.
     *
     * @param fieldValue The current field value
     * @param attr The meta attribute
     * @param path The complete path
     * @param operationMap Map of collection operations
     * @param nextIndex The next index to process
     * @return The value at the end of navigation
     */
    protected Object navigateNestedWithCollections(
            Object fieldValue,
            MetaAttribute<?, ?> attr,
            List<MetaAttribute<?, ?>> path,
            java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap,
            int nextIndex) {
        // Default implementation delegates to navigateNested for simple path navigation
        // Generated services can override this for more specific behavior
        return navigateNested(fieldValue, attr, path, nextIndex);
    }

    /**
     * Default implementation of setValueByPathWithCollections.
     * Sets a value using a path that may contain collection operations.
     *
     * @param entity The entity to start navigation from
     * @param path The path containing only MetaAttribute objects
     * @param collectionOperations Collection operation metadata for positions in the path
     * @param value The value to set
     */
    @Override
    public void setValueByPathWithCollections(
            T entity,
            List<MetaAttribute<?, ?>> path,
            List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations,
            Object value) {

        if (entity == null || path == null || path.isEmpty()) {
            return;
        }

        java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap =
                buildOperationMap(collectionOperations);

        setValueByPathWithCollectionsImpl(entity, path, operationMap, 0, value);
    }

    /**
     * Implementation method for path-based value setting with collection operations.
     * Uses delegation pattern to avoid runtime type lookups.
     *
     * @param currentValue The current value being navigated
     * @param path The complete path
     * @param operationMap Map of path index to collection operations
     * @param index The current index in the path
     * @param value The value to set at the end of the path
     */
    @SuppressWarnings("unchecked")
    protected void setValueByPathWithCollectionsImpl(
            Object currentValue,
            List<MetaAttribute<?, ?>> path,
            java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap,
            int index,
            Object value) {

        if (currentValue == null || index >= path.size()) {
            return;
        }

        MetaAttribute<?, ?> metaAttribute = path.get(index);
        com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> operation = operationMap.get(index);

        // If this is the last attribute in the path, set the value
        if (index == path.size() - 1) {
            if (operation != null) {
                handleCollectionOperation(currentValue, metaAttribute, operation, value, (SpecificationService<Object>) this);
            } else {
                handleRegularFieldAssignment(currentValue, metaAttribute, value, (SpecificationService<Object>) this);
            }
            return;
        }

        // Navigate to the next level
        Object fieldValue = getFieldValue((T) currentValue, metaAttribute);

        // Resolve field value with collection operation if present
        Object nextValue = resolveFieldValueForSet(fieldValue, operation, metaAttribute, currentValue, index);

        // Delegate to generated service for nested navigation - NO RUNTIME LOOKUP!
        navigateNestedForSetWithCollections(nextValue, metaAttribute, path, operationMap, index + 1, value);
    }

    /**
     * Navigates to nested fields for setting with collection operations using direct service references.
     * Generated services should override this to provide direct INSTANCE references.
     * Default implementation uses the delegation pattern.
     *
     * @param fieldValue The current field value
     * @param attr The meta attribute
     * @param path The complete path
     * @param operationMap Map of collection operations
     * @param nextIndex The next index to process
     * @param value The value to set
     */
    protected void navigateNestedForSetWithCollections(
            Object fieldValue,
            MetaAttribute<?, ?> attr,
            List<MetaAttribute<?, ?>> path,
            java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap,
            int nextIndex,
            Object value) {
        // Default implementation delegates to navigateNestedForSet for simple path navigation
        // Generated services can override this for more specific behavior
        navigateNestedForSet(fieldValue, attr, path, nextIndex, value);
    }

    /**
     * Resolves field value for setting, handling collection operations and null values.
     *
     * @param fieldValue The current field value
     * @param operation The collection operation (if any)
     * @param metaAttribute The meta attribute
     * @param currentValue The current object
     * @param pathIndex The current path index
     * @return The resolved field value
     */
    @SuppressWarnings("unchecked")
    private Object resolveFieldValueForSet(
            Object fieldValue,
            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> operation,
            MetaAttribute<?, ?> metaAttribute,
            Object currentValue,
            int pathIndex) {

        if (operation != null && fieldValue instanceof Collection) {
            return processCollectionOperation((Collection<?>) fieldValue, operation);
        }

        if (fieldValue == null) {
            return createIntermediateInstanceForNavigation(metaAttribute, currentValue, pathIndex);
        }

        return fieldValue;
    }


    /**
     * Builds a map of path index to collection operation metadata.
     */
    private java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> buildOperationMap(
            List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {

        java.util.Map<Integer, com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> operationMap = new java.util.HashMap<>();

        if (collectionOperations != null) {
            for (com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> op : collectionOperations) {
                operationMap.put(op.getPathIndex(), op);
            }
        }

        return operationMap;
    }


    /**
     * Handles collection field with a specific operation selector.
     */
    private void handleCollectionFieldWithOperation(
            Collection<Object> collection,
            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> finalOperation,
            Object currentValue,
            MetaAttribute<?, ?> lastAttribute,
            Object value,
            SpecificationService<Object> service) {

        if (finalOperation.getSelector() == com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.ALL) {
            addValueToCollection(collection, currentValue, lastAttribute, value, service);
        } else {
            handleNonAllSelector(collection, finalOperation);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCollectionOperation(
            Object currentValue,
            MetaAttribute<?, ?> lastAttribute,
            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> finalOperation,
            Object value,
            SpecificationService<Object> service) {

        Object fieldValue = service.getFieldValue(currentValue, lastAttribute);

        if (fieldValue instanceof Collection) {
            handleCollectionFieldWithOperation((Collection<Object>) fieldValue, finalOperation,
                    currentValue, lastAttribute, value, service);
        }
    }


    /**
     * Adds a value to a collection, initializing it if necessary.
     */
    private void addValueToCollection(
            Collection<Object> collection,
            Object currentValue,
            MetaAttribute<?, ?> lastAttribute,
            Object value,
            SpecificationService<Object> service) {

        Collection<Object> targetCollection = collection;

        if (targetCollection == null) {
            targetCollection = new ArrayList<>();
            service.setFieldValue(currentValue, lastAttribute, targetCollection);
        }

        targetCollection.add(value);
    }


    /**
     * Handles FIRST/LAST/ANY selectors which are not yet fully supported.
     */
    private void handleNonAllSelector(
            Collection<Object> collection,
            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> finalOperation) {

        Object targetElement = processCollectionOperation(collection, finalOperation);

        if (targetElement != null) {
            throw new UnsupportedOperationException(
                    "Setting values on collection elements with FIRST/LAST/ANY selector is not yet fully supported");
        }
    }

    /**
     * Handles regular field assignment without collection operations.
     */
    @SuppressWarnings("unchecked")
    private void handleRegularFieldAssignment(
            Object currentValue,
            MetaAttribute<?, ?> lastAttribute,
            Object value,
            SpecificationService<Object> service) {

        if (lastAttribute.getAttributeType() == AttributeType.COLLECTION) {
            handleCollectionField(currentValue, lastAttribute, value, service);
        } else {
            service.setFieldValue(currentValue, lastAttribute, value);
        }
    }

    /**
     * Processes a collection operation with comparator and specification support.
     * Applies specification filter first, then comparator for ordering, then selector.
     *
     * @param collection The collection to process
     * @param operation The collection operation metadata
     * @return The result of the collection operation
     */
    @SuppressWarnings("unchecked")
    private Object processCollectionOperation(
            Collection<?> collection,
            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> operation) {
        
        if (collection == null || collection.isEmpty()) {
            return operation.getSelector() == com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.ALL 
                ? new ArrayList<>() 
                : null;
        }

        java.util.stream.Stream<?> stream = collection.stream();
        
        // Apply specification filter if provided
        Specification<?> specification = operation.getSpecification();
        if (specification != null) {
            final Specification<Object> spec = (Specification<Object>) specification;
            stream = stream.filter(spec::test);
        }
        
        // Apply comparator if provided
        java.util.Comparator<?> comparator = operation.getComparator();
        
        // Apply selector
        com.thy.fss.common.inmemory.engine.mapping.CollectionSelector selector = operation.getSelector();
        
        return switch (selector) {
            case FIRST -> {
                if (comparator != null) {
                    yield stream.min((java.util.Comparator<Object>) comparator).orElse(null);
                } else {
                    yield stream.findFirst().orElse(null);
                }
            }
            case LAST -> {
                if (comparator != null) {
                    yield stream.max((java.util.Comparator<Object>) comparator).orElse(null);
                } else {
                    List<?> list = stream.toList();
                    yield list.isEmpty() ? null : list.get(list.size() - 1);
                }
            }
            case ANY ->
                stream.findAny().orElse(null);
            case ALL ->
                stream.toList();
        };
    }

    // ==================== ELEMENT TYPE SERVICE LOOKUP ====================

    /**
     * Gets the specification service for an element type in a collection.
     * This method is used to delegate validation of collection elements to their
     * respective specification services.
     *
     * <p>For model types (complex objects with their own filters), this method
     * returns the specification service that can validate instances of that type.
     * For basic types (String, Integer, etc.), this method returns null since
     * basic types don't have specification services.</p>
     *
     * @param elementTypeClass The class of the collection element type
     * @param <E> The element type
     * @return The specification service for model types, null for basic types
     * @throws IllegalStateException if the element type is a model type but no service is found
     */
    protected <E> SpecificationService<E> getElementTypeService(Class<E> elementTypeClass) {
        if (elementTypeClass == null) {
            return null;
        }

        // Check if this is a basic type (no specification service expected)
        if (isBasicType(elementTypeClass)) {
            return null;
        }

        // Try to get the service for model types
        if (!SpecificationServices.hasService(elementTypeClass)) {
            throw new IllegalStateException(
                    "No specification service found for element type: " + elementTypeClass.getName() +
                    ". Model types used in collections must have a @MetaModel annotation and " +
                    "the annotation processor must have generated a specification service."
            );
        }

        return SpecificationServices.getService(elementTypeClass);
    }

    /**
     * Checks if a type is a basic type (primitive wrapper, String, or enum).
     * Basic types don't have specification services.
     *
     * @param type The type to check
     * @return true if the type is a basic type
     */
    private boolean isBasicType(Class<?> type) {
        return type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Character.class
                || type == Byte.class
                || type == Short.class
                || type.isEnum()
                || java.time.temporal.Temporal.class.isAssignableFrom(type); // LocalDate, LocalDateTime, Instant, etc.
    }

    /**
     * Validates a collection element against an element filter.
     * This method delegates validation to the element type's specification service.
     *
     * <p>This method is used when validating collection filters with any/all/none operators
     * on model type collections. It gets the appropriate specification service for the
     * element type and delegates the validation to that service.</p>
     *
     * @param element The collection element to validate
     * @param elementFilter The filter to validate against (can be FilterBase, Filter, or EntityFilter)
     * @param elementTypeClass The class of the element type
     * @param <E> The element type
     * @return true if the element matches the filter criteria, false otherwise
     * @throws IllegalStateException if the element type is a model type but no service is found
     */
    protected <E> boolean validateCollectionElement(E element, Object elementFilter, Class<E> elementTypeClass) {
        if (elementFilter == null) {
            return true;
        }

        // Get the element type service
        SpecificationService<E> elementService = getElementTypeService(elementTypeClass);
        
        if (elementService == null) {
            // Basic type - cannot validate with specification service
            // This should not happen in normal usage, but handle gracefully
            logger.warn("Cannot validate basic type element: {}. Basic types should not use element filters.",
                    elementTypeClass.getName());
            return false;
        }

        // Delegate validation to the element type's specification service (handles null element correctly)
        return elementService.validateFilter(element, elementFilter);
    }
}
