package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for Boolean fields.
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to a Boolean field, ensuring that
 * the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return BooleanFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.isActive)).isTrue())
 * // nav.field(Order_.isActive) returns BooleanNavigationBuilder
 * // spec.on(BooleanNavigationBuilder) returns BooleanFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the Boolean field
 */
public class BooleanNavigationBuilder<O> extends PropertyNavigationBuilder {
    
    private final BooleanAttribute<O> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the Boolean attribute
     */
    BooleanNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            BooleanAttribute<O> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the Boolean attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the Boolean attribute
     */
    public BooleanAttribute<O> getAttribute() {
        return attribute;
    }
}
