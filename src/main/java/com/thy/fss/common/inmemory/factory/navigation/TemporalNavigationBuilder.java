package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for temporal fields (LocalDateTime, LocalDate, Instant).
 * Wraps PropertyNavigationBuilder to provide compile-time type safety for SpecificationBuilder.on() method.
 * 
 * <p>This class is returned when navigating to a temporal field, ensuring that
 * the SpecificationBuilder.on() method can determine the correct field type
 * at compile time and return TemporalFieldBuilder.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * .where((nav, spec) -> spec.on(nav.field(Order_.createdDate)).isAfter(LocalDateTime.now()))
 * // nav.field(Order_.createdDate) returns TemporalNavigationBuilder
 * // spec.on(TemporalNavigationBuilder) returns TemporalFieldBuilder
 * </pre>
 * 
 * @param <O> the owner type of the temporal field
 * @param <L> the temporal type (LocalDateTime, LocalDate, Instant)
 */
public class TemporalNavigationBuilder<O, L> extends PropertyNavigationBuilder {
    
    private final MetaAttribute<O, L> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the temporal attribute
     */
    TemporalNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            MetaAttribute<O, L> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the temporal attribute.
     * Used by SpecificationBuilder.on() to create type-safe field builder.
     * 
     * @return the temporal attribute
     */
    public MetaAttribute<O, L> getAttribute() {
        return attribute;
    }
}
