package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for String fields.
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to a String field, ensuring that
 * the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return StringFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.status)).contains("ACTIVE"))
 * // nav.field(Order_.status) returns StringNavigationBuilder
 * // spec.on(StringNavigationBuilder) returns StringFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the String field
 */
public class StringNavigationBuilder<O> extends PropertyNavigationBuilder {
    
    private final StringAttribute<O> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the String attribute
     */
    StringNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            StringAttribute<O> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the String attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the String attribute
     */
    public StringAttribute<O> getAttribute() {
        return attribute;
    }
}
