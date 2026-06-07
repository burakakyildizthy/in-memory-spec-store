package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Specialized source mapping context for comparable target fields.
 * Provides comparable-specific aggregation operations (min, max) in addition to base operations.
 * 
 * <p>This context is returned when mapping from a source to a comparable target field.
 * It validates that source fields used in comparable operations are also comparable types.
 * 
 * <p>All terminal operations return to the root builder, allowing method chaining
 * for defining additional mappings.
 * 
 * @param <R> the root entity type
 * @param <C> the current target field type (must extend Comparable)
 * @param <S> the source entity type
 */
public class ComparableSourceMappingContext<R, C extends Comparable<C>, S>
        extends SourceMappingContext<R, C, S> {

    private static final String SOURCE_FIELD_CANNOT_BE_NULL = "Source field builder cannot be null";
    
    /**
     * Public constructor for creating comparable source mapping context.
     * 
     * @param targetContext the target navigation context
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths for joining
     * @param foreignKeyPaths the list of foreign key paths for joining
     * @throws NullPointerException if targetContext or sourceService is null
     */
    public ComparableSourceMappingContext(
            PropertyNavigationContext<R, C> targetContext,
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        super(targetContext, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
    
    /**
     * Complete mapping as MIN aggregation.
     * Finds the minimum value of the specified source field across all matching source entities.
     * The source field must be a comparable type.
     * Returns to root builder.
     * 
     * @param sourceFieldBuilder function to select source field (must return Comparable field)
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     * @throws IllegalArgumentException if source field is not a Comparable type
     */
    public AbstractRootBuilder<R> min(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        
        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);
        
        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        validateSourceFieldType(sourcePath, Comparable.class, "min");
        
        createMapping(sourcePath, MappingType.MANY_TO_ONE_AGGREGATION, AggregationType.MIN);
        return getRootBuilder();
    }
    
    /**
     * Complete mapping as MAX aggregation.
     * Finds the maximum value of the specified source field across all matching source entities.
     * The source field must be a comparable type.
     * Returns to root builder.
     * 
     * @param sourceFieldBuilder function to select source field (must return Comparable field)
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     * @throws IllegalArgumentException if source field is not a Comparable type
     */
    public AbstractRootBuilder<R> max(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        
        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);
        
        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        validateSourceFieldType(sourcePath, Comparable.class, "max");
        
        createMapping(sourcePath, MappingType.MANY_TO_ONE_AGGREGATION, AggregationType.MAX);
        return getRootBuilder();
    }
}
