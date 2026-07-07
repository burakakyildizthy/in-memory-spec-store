package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.SpecificationServices;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for Collection fields.
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to a Collection field, ensuring that
 * the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return CollectionFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.items)).contains(item))
 * // nav.field(Order_.items) returns CollectionNavigationBuilder
 * // spec.on(CollectionNavigationBuilder) returns CollectionFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the Collection field
 * @param <T> the element type
 */
public class CollectionNavigationBuilder<O, T> extends PropertyNavigationBuilder {
    
    private final CollectionAttribute<O, T> attribute;
    private static final String FILTER_FUNC_CANNOT_BE_NULL = "Filter function cannot be null";
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the Collection attribute
     */
    CollectionNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            CollectionAttribute<O, T> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the Collection attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the Collection attribute
     */
    public CollectionAttribute<O, T> getAttribute() {
        return attribute;
    }
    
    // ==================== COLLECTION ELEMENT SELECTION ====================
    
    /**
     * Get the FIRST element from the collection.
     * After calling this, you can navigate to element fields.
     * 
     * <p>Example: nav.field(User_.orders).first().field(Order_.amount)</p>
     * 
     * @return ModelNavigationBuilder for the first element
     */
    public ModelNavigationBuilder<O, T> first() {
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getPath());
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new java.util.ArrayList<>(getCollectionOperations());
        
        int pathIndex = newPath.size() - 1;
        newCollectionOps.add(new CollectionOperationMetadata<>(
            pathIndex,
            attribute,
            com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.FIRST,
            null
        ));
        
        com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<O, T> syntheticAttribute =
            new com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<>(
                attribute.getName() + "[first]",
                attribute.getOwnerType(),
                attribute.getElementType()
            );
        
        return new ModelNavigationBuilder<>(
            getRootClass(),
            newPath,
            newCollectionOps,
            syntheticAttribute
        );
    }
    
    /**
     * Get the FIRST element from the collection that matches a specification.
     * After calling this, you can navigate to element fields.
     * 
     * <p>Example: nav.field(User_.orders).first(spec -> spec.on(Order_.status).eq("ACTIVE")).field(Order_.amount)</p>
     * 
     * @param filterFunction function to build filter specification on element type
     * @return ModelNavigationBuilder for the first matching element
     * @throws IllegalArgumentException if filterFunction is null
     */
    @SuppressWarnings("java:S1854")
    public ModelNavigationBuilder<O, T> first(
            java.util.function.Function<com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<T>, 
                                        com.thy.fss.common.inmemory.specification.Specification<T>> filterFunction) {
        
        java.util.Objects.requireNonNull(filterFunction, FILTER_FUNC_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getPath());
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new java.util.ArrayList<>(getCollectionOperations());
        
        com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<T> specBuilder = 
            new com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<>(
                SpecificationServices.getService(attribute.getElementType())
            );
        com.thy.fss.common.inmemory.specification.Specification<T> filter = filterFunction.apply(specBuilder);
        
        int pathIndex = newPath.size() - 1;
        newCollectionOps.add(new CollectionOperationMetadata<>(
            pathIndex,
            attribute,
            com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.FIRST,
            filter
        ));
        
        com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<O, T> syntheticAttribute =
            new com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<>(
                attribute.getName() + "[first]",
                attribute.getOwnerType(),
                attribute.getElementType()
            );
        
        return new ModelNavigationBuilder<>(
            getRootClass(),
            newPath,
            newCollectionOps,
            syntheticAttribute
        );
    }
    
    /**
     * Get the LAST element from the collection.
     * After calling this, you can navigate to element fields.
     * 
     * <p>Example: nav.field(User_.orders).last().field(Order_.createdDate)</p>
     * 
     * @return ModelNavigationBuilder for the last element
     */
    public ModelNavigationBuilder<O, T> last() {
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getPath());
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new java.util.ArrayList<>(getCollectionOperations());
        
        int pathIndex = newPath.size() - 1;
        newCollectionOps.add(new CollectionOperationMetadata<>(
            pathIndex,
            attribute,
            com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.LAST,
            null
        ));
        
        com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<O, T> syntheticAttribute =
            new com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<>(
                attribute.getName() + "[last]",
                attribute.getOwnerType(),
                attribute.getElementType()
            );
        
        return new ModelNavigationBuilder<>(
            getRootClass(),
            newPath,
            newCollectionOps,
            syntheticAttribute
        );
    }
    
    /**
     * Get the LAST element from the collection that matches a specification.
     * After calling this, you can navigate to element fields.
     * 
     * <p>Example: nav.field(User_.orders).last(spec -> spec.on(Order_.status).eq("COMPLETED")).field(Order_.amount)</p>
     * 
     * @param filterFunction function to build filter specification on element type
     * @return ModelNavigationBuilder for the last matching element
     * @throws IllegalArgumentException if filterFunction is null
     */
    @SuppressWarnings("java:S1854")
    public ModelNavigationBuilder<O, T> last(
            java.util.function.Function<com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<T>, 
                                        com.thy.fss.common.inmemory.specification.Specification<T>> filterFunction) {
        
        java.util.Objects.requireNonNull(filterFunction, FILTER_FUNC_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getPath());
        List<CollectionOperationMetadata<?, ?>> newCollectionOps = new java.util.ArrayList<>(getCollectionOperations());
        
        com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<T> specBuilder = 
            new com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<>(
                SpecificationServices.getService(attribute.getElementType())
            );
        com.thy.fss.common.inmemory.specification.Specification<T> filter = filterFunction.apply(specBuilder);
        
        int pathIndex = newPath.size() - 1;
        newCollectionOps.add(new CollectionOperationMetadata<>(
            pathIndex,
            attribute,
            com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.LAST,
            filter
        ));
        
        com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<O, T> syntheticAttribute =
            new com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<>(
                attribute.getName() + "[last]",
                attribute.getOwnerType(),
                attribute.getElementType()
            );
        
        return new ModelNavigationBuilder<>(
            getRootClass(),
            newPath,
            newCollectionOps,
            syntheticAttribute
        );
    }
}
