package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Specialized source mapping context for Enum target fields.
 * Overrides value() to validate that the source field is also an Enum type.
 * 
 * <p>This context is returned when mapping from a source to an Enum target field.
 * It ensures type safety by validating that source fields used in value mappings
 * are Enum types.</p>
 * 
 * <p>All terminal operations return to the root builder, allowing method chaining
 * for defining additional mappings.</p>
 * 
 * @param <R> the root entity type
 * @param <E> the enum type
 * @param <S> the source entity type
 */
public class EnumSourceMappingContext<R, E extends Enum<E>, S>
        extends SourceMappingContext<R, E, S> {
    
    /**
     * Public constructor for creating enum source mapping context.
     * 
     * @param targetContext the target navigation context
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths for joining
     * @param foreignKeyPaths the list of foreign key paths for joining
     * @throws NullPointerException if targetContext or sourceService is null
     */
    public EnumSourceMappingContext(
            PropertyNavigationContext<R, E> targetContext,
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        super(targetContext, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
    
    /**
     * Complete mapping as single value mapping with Enum type validation.
     * Maps a single source field value to the target field.
     * The source field must be an Enum type.
     * Returns to root builder.
     * 
     * @param sourceFieldBuilder function to select source field (must return Enum field)
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     * @throws IllegalArgumentException if source field is not an Enum type
     */
    @Override
    public AbstractRootBuilder<R> value(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        
        Objects.requireNonNull(sourceFieldBuilder, "Source field builder cannot be null");
        
        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        validateSourceFieldType(sourcePath, Enum.class, "value");
        
        return super.value(sourceFieldBuilder);
    }
}
