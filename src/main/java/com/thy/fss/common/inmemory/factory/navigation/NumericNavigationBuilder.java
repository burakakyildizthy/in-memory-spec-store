package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.NumericMetaAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for numeric fields.
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to a numeric field (Integer, Long, Double),
 * ensuring that the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return NumericFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.amount)).greaterThan(100))
 * // nav.field(Order_.amount) returns NumericNavigationBuilder
 * // spec.on(NumericNavigationBuilder) returns NumericFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the numeric field
 * @param <N> the numeric type (Integer, Long, Double, etc.)
 */
public class NumericNavigationBuilder<O, N extends Number> extends PropertyNavigationBuilder {
    
    private final NumericMetaAttribute<O, N> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the numeric attribute
     */
    NumericNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            NumericMetaAttribute<O, N> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the numeric attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the numeric attribute
     */
    public NumericMetaAttribute<O, N> getAttribute() {
        return attribute;
    }
}
