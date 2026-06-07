package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.specification.attribute.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Specialized context for navigating model (complex object) properties.
 * This is the ONLY context that provides field() methods for nested navigation.
 * 
 * <p>ModelPropertyNavigationContext allows navigation into nested fields of complex objects.
 * When navigating to a field, it returns a type-specific context based on the field type:</p>
 * <ul>
 *   <li>Number fields → NumericPropertyNavigationContext</li>
 *   <li>String fields → StringPropertyNavigationContext</li>
 *   <li>Comparable fields → ComparablePropertyNavigationContext</li>
 *   <li>Boolean fields → BooleanPropertyNavigationContext</li>
 *   <li>Collection fields → CollectionPropertyNavigationContext</li>
 *   <li>Other complex objects → ModelPropertyNavigationContext (recursive)</li>
 * </ul>
 * 
 * @param <R> the root entity type
 * @param <C> the current model type
 */
public class ModelPropertyNavigationContext<R, C> 
        extends PropertyNavigationContext<R, C> {

    private static final String ATTR_CANNOT_BE_NULL = "Attribute cannot be null";
    
    /**
     * Package-private constructor for creating model navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param currentType the current model type
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public ModelPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<C> currentType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, currentType, collectionOperations);
    }
    
    /**
     * Navigate to a numeric nested field within the model.
     * Returns NumericPropertyNavigationContext for type-safe numeric operations.
     * 
     * <p>This overload provides compile-time type safety for numeric fields,
     * ensuring that numeric-specific operations (sum, avg) are available.</p>
     * 
     * @param <N> the numeric field type (extends Number)
     * @param attribute the numeric field attribute
     * @return NumericPropertyNavigationContext for numeric operations
     * @throws NullPointerException if attribute is null
     */
    public <N extends Number> NumericPropertyNavigationContext<R, N> field(NumericMetaAttribute<C, N> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new NumericPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            getCollectionOperations()
        );
    }
    
    /**
     * Navigate to a String nested field within the model.
     * Returns StringPropertyNavigationContext for type-safe String operations.
     * 
     * @param attribute the String field attribute
     * @return StringPropertyNavigationContext for String operations
     * @throws NullPointerException if attribute is null
     */
    public StringPropertyNavigationContext<R> field(StringAttribute<C> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new StringPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            getCollectionOperations()
        );
    }
    
    /**
     * Navigate to a Boolean nested field within the model.
     * Returns BooleanPropertyNavigationContext for type-safe Boolean operations.
     * 
     * @param attribute the Boolean field attribute
     * @return BooleanPropertyNavigationContext for Boolean operations
     * @throws NullPointerException if attribute is null
     */
    public BooleanPropertyNavigationContext<R> field(BooleanAttribute<C> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new BooleanPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            getCollectionOperations()
        );
    }
    
    /**
     * Navigate to an Enum nested field within the model.
     * Returns EnumPropertyNavigationContext for type-safe Enum operations.
     * 
     * @param <E> the enum type
     * @param attribute the Enum field attribute
     * @return EnumPropertyNavigationContext for Enum operations
     * @throws NullPointerException if attribute is null
     */
    public <E extends Enum<E>> EnumPropertyNavigationContext<R, E> field(com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<C, E> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new EnumPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            getCollectionOperations()
        );
    }
    
    /**
     * Navigate to a nested model field within the model.
     * Returns ModelPropertyNavigationContext for continued navigation.
     * 
     * @param <M> the nested model type
     * @param attribute the model field attribute
     * @return ModelPropertyNavigationContext for continued navigation
     * @throws NullPointerException if attribute is null
     */
    public <M> ModelPropertyNavigationContext<R, M> field(ModelAttribute<C, M> attribute) {
        Objects.requireNonNull(attribute, ATTR_CANNOT_BE_NULL);
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new ModelPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getFieldType(),
            getCollectionOperations()
        );
    }
    

    
    /**
     * Navigate to a collection field within the model.
     * Returns CollectionPropertyNavigationContext for collection-specific operations.
     * 
     * <p>Collection fields require special handling to support operations like
     * first(), last(), and element field navigation.</p>
     * 
     * @param <E> the element type
     * @param attribute the collection attribute
     * @return CollectionPropertyNavigationContext for collection operations
     * @throws NullPointerException if attribute is null
     */
    public <E> CollectionPropertyNavigationContext<R, E> field(CollectionAttribute<C, E> attribute) {
        Objects.requireNonNull(attribute, "Collection attribute cannot be null");
        
        LinkedList<MetaAttribute<?, ?>> newPath = new LinkedList<>(getTargetPath());
        newPath.add(attribute);
        
        return new CollectionPropertyNavigationContext<>(
            getRootBuilder(),
            newPath,
            attribute.getElementType(),
            getCollectionOperations()
        );
    }
    

}
