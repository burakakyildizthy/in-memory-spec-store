package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.EnumAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for Enum fields.
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to an Enum field, ensuring that
 * the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return EnumFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.status)).equalTo(OrderStatus.COMPLETED))
 * // nav.field(Order_.status) returns EnumNavigationBuilder
 * // spec.on(EnumNavigationBuilder) returns EnumFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the Enum field
 * @param <E> the Enum type
 */
public class EnumNavigationBuilder<O, E extends Enum<E>> extends PropertyNavigationBuilder {
    
    private final EnumAttribute<O, E> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the Enum attribute
     */
    EnumNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            EnumAttribute<O, E> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the Enum attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the Enum attribute
     */
    public EnumAttribute<O, E> getAttribute() {
        return attribute;
    }
}
