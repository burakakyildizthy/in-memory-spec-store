package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Context for navigating collection properties.
 * Provides collection-specific operations and element field access.
 * 
 * <p>CollectionPropertyNavigationContext allows navigation into collection elements
 * and provides operations like first() and last() to select specific elements.
 * When navigating to an element field, it returns a type-specific context based on the field type.</p>
 * 
 * @param <R> the root entity type
 * @param <T> the collection element type
 */
public class CollectionPropertyNavigationContext<R, T>
        extends PropertyNavigationContext<R, List<T>> {

    private static final String ATTR_CANNOT_BE_NULL = "Attribute cannot be null";
    private static final String FILTER_FUNC_CANNOT_BE_NULL = "Filter function cannot be null";
    
    private final Class<T> elementType;
    
    /**
     * Package-private constructor fo
     * r creating collection navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param elementType the collection element type
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    @SuppressWarnings("unchecked")
    public CollectionPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<T> elementType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, (Class<List<T>>) (Class<?>) List.class, collectionOperations);
        this.elementType = Objects.requireNonNull(elementType, "Element type cannot be null");
    }
    
    /**
     * Navigate to a numeric field of the collection element.
     * Returns NumericPropertyNavigationContext for type-safe numeric operations.
     * 
     * <p>Example: User_.orders.field(Order_.amount)</p>
     * 
     * @param <N> the numeric field type (extends Number)
     * @param attribute the numeric field attribute
     * @return NumericPropertyNavigationContext for numeric operations
     * @throws NullPointerException if attribute is null
     */
    public <N extends Number> NumericPropertyNavigationContext<R, N> field(NumericMetaAttribute<T, N> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = addCollectionAccessOperation();
        
        return new NumericPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            newCollectionOps
        );
    }
    
    /**
     * Navigate to a String field of the collection element.
     * Returns StringPropertyNavigationContext for type-safe String operations.
     * 
     * @param attribute the String field attribute
     * @return StringPropertyNavigationContext for String operations
     * @throws NullPointerException if attribute is null
     */
    public StringPropertyNavigationContext<R> field(StringAttribute<T> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = addCollectionAccessOperation();
        
        return new StringPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            newCollectionOps
        );
    }
    
    /**
     * Navigate to a Boolean field of the collection element.
     * Returns BooleanPropertyNavigationContext for type-safe Boolean operations.
     * 
     * @param attribute the Boolean field attribute
     * @return BooleanPropertyNavigationContext for Boolean operations
     * @throws NullPointerException if attribute is null
     */
    public BooleanPropertyNavigationContext<R> field(BooleanAttribute<T> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = addCollectionAccessOperation();
        
        return new BooleanPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            newCollectionOps
        );
    }
    
    /**
     * Navigate to an Enum field of the collection element.
     * Returns EnumPropertyNavigationContext for type-safe Enum operations.
     * 
     * @param <E> the enum type
     * @param attribute the Enum field attribute
     * @return EnumPropertyNavigationContext for Enum operations
     * @throws NullPointerException if attribute is null
     */
    public <E extends Enum<E>> EnumPropertyNavigationContext<R, E> field(com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<E, E> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = addCollectionAccessOperation();
        
        return new EnumPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            newCollectionOps
        );
    }
    
    /**
     * Navigate to a model field of the collection element.
     * Returns ModelPropertyNavigationContext for continued navigation.
     * 
     * @param <M> the model type
     * @param attribute the model field attribute
     * @return ModelPropertyNavigationContext for continued navigation
     * @throws NullPointerException if attribute is null
     */
    public <M> ModelPropertyNavigationContext<R, M> field(ModelAttribute<T, M> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = addCollectionAccessOperation();
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            newCollectionOps
        );
    }
    

    
    /**
     * Helper method to add collection access operation metadata.
     * Extracts common logic for adding ELEMENT_ACCESS operation.
     * 
     * @return new collection operations list with ELEMENT_ACCESS added
     */
    private List<CollectionOperationMetadata<?, ?>> addCollectionAccessOperation() {
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        // Find the collection attribute in the path (last element should be the collection)
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, T> collectionAttr = (CollectionAttribute<?, T>) lastPathElement;
            
            // Add ELEMENT_ACCESS operation at the current path index
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.ALL,
                null  // No filter for simple element access
            ));
        }
        
        return newCollectionOps;
    }
    
    /**
     * Navigate to a nested collection within the element.
     * 
     * <p>Example: User_.orders.field(Order_.items) where items is List&lt;OrderItem&gt;</p>
     * 
     * @param <E> the nested collection element type
     * @param attribute the nested collection attribute
     * @return CollectionPropertyNavigationContext for nested collection
     * @throws NullPointerException if attribute is null
     */
    public <E> CollectionPropertyNavigationContext<R, E> field(CollectionAttribute<E, E> attribute) {
        Objects.requireNonNull(attribute, "Collection attribute cannot be null");
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        // Add collection operation for nested collection access
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        // Find the parent collection attribute
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, E> collectionAttr = (CollectionAttribute<?, E>) lastPathElement;
            
            // Add ALL operation for parent collection
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.ALL,
                null
            ));
        }
        
        return new CollectionPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getElementType(),
            newCollectionOps
        );
    }
    
    /**
     * Get the FIRST element from the collection.
     * Returns ModelPropertyNavigationContext for the element, allowing further field navigation.
     * 
     * <p>Example: User_.orders.first().field(Order_.amount)</p>
     * 
     * @return ModelPropertyNavigationContext for the first element
     */
    public ModelPropertyNavigationContext<R, T> first() {
        // Add collection operation metadata for FIRST
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        // Find the collection attribute in the path
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, T> collectionAttr = (CollectionAttribute<?, T>) lastPathElement;
            
            // Add FIRST operation at the current path index
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.FIRST,
                null  // No filter
            ));
        }
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            getTargetPath(),  // Same path (orders)
            elementType,      // CURRENT type is now T (Order)
            newCollectionOps
        );
    }
    
    /**
     * Get the FIRST element from the collection that matches a condition.
     * 
     * <p>Example: User_.orders.first(spec -&gt; spec.on(Order_.status).eq("ACTIVE"))</p>
     * 
     * @param filterFunction function to build filter specification
     * @return ModelPropertyNavigationContext for the first matching element
     * @throws NullPointerException if filterFunction is null
     */
    public ModelPropertyNavigationContext<R, T> first(
            UnaryOperator<Specification<T>>  filterFunction) {
        Objects.requireNonNull(filterFunction, FILTER_FUNC_CANNOT_BE_NULL);
        
        // Build the filter specification
        // Note: The design document shows SpecificationBuilder, but we'll use Specification directly
        // as SpecificationBuilder doesn't exist in the codebase yet
        Specification<T> filter = filterFunction.apply(null);
        
        // Add collection operation metadata for FIRST with filter
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, T> collectionAttr = (CollectionAttribute<?, T>) lastPathElement;
            
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.FIRST,
                filter
            ));
        }
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            getTargetPath(),
            elementType,
            newCollectionOps
        );
    }
    
    /**
     * Get the LAST element from the collection.
     * Returns ModelPropertyNavigationContext for the element, allowing further field navigation.
     * 
     * <p>Example: User_.orders.last().field(Order_.createdDate)</p>
     * 
     * @return ModelPropertyNavigationContext for the last element
     */
    public ModelPropertyNavigationContext<R, T> last() {
        // Add collection operation metadata for LAST
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, T> collectionAttr = (CollectionAttribute<?, T>) lastPathElement;
            
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.LAST,
                null
            ));
        }
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            getTargetPath(),
            elementType,
            newCollectionOps
        );
    }
    
    /**
     * Get the LAST element from the collection that matches a condition.
     * 
     * <p>Example: User_.orders.last(spec -&gt; spec.on(Order_.amount).greaterThan(100))</p>
     * 
     * @param filterFunction function to build filter specification
     * @return ModelPropertyNavigationContext for the last matching element
     * @throws NullPointerException if filterFunction is null
     */
    public ModelPropertyNavigationContext<R, T> last(
            UnaryOperator<Specification<T>> filterFunction) {
        Objects.requireNonNull(filterFunction, FILTER_FUNC_CANNOT_BE_NULL);
        
        // Build the filter specification
        Specification<T> filter = filterFunction.apply(null);
        
        // Add collection operation metadata for LAST with filter
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new ArrayList<>(getCollectionOperations());
        
        MetaAttribute<?, ?> lastPathElement = getTargetPath().getLast();
        if (lastPathElement instanceof CollectionAttribute) {
            @SuppressWarnings("unchecked")
            CollectionAttribute<?, T> collectionAttr = (CollectionAttribute<?, T>) lastPathElement;
            
            int pathIndex = getTargetPath().size() - 1;
            newCollectionOps.add(new CollectionOperationMetadata<>(
                pathIndex,
                collectionAttr,
                CollectionSelector.LAST,
                filter
            ));
        }
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            getTargetPath(),
            elementType,
            newCollectionOps
        );
    }
    

}
