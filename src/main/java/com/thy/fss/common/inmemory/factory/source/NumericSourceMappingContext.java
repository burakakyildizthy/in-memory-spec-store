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
 * Specialized source mapping context for numeric target fields.
 * Provides numeric-specific aggregation operations (sum, avg) in addition to base operations.
 * 
 * <p>This context is returned when mapping from a source to a numeric target field.
 * It validates that source fields used in numeric operations are also numeric types.
 * 
 * <p>All terminal operations return to the root builder, allowing method chaining
 * for defining additional mappings.
 * 
 * @param <R> the root entity type
 * @param <C> the current target field type (must extend Number)
 * @param <S> the source entity type
 */
public class NumericSourceMappingContext<R, C extends Number, S>
        extends SourceMappingContext<R, C, S> {

    private static final String SOURCE_FIELD_CANNOT_BE_NULL = "Source field builder cannot be null";
    
    /**
     * Public constructor for creating numeric source mapping context.
     * 
     * @param targetContext the target navigation context
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths for joining
     * @param foreignKeyPaths the list of foreign key paths for joining
     * @throws NullPointerException if targetContext or sourceService is null
     */
    public NumericSourceMappingContext(
            PropertyNavigationContext<R, C> targetContext,
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        super(targetContext, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
    
    /**
     * Complete mapping as SUM aggregation.
     * Sums the values of the specified source field across all matching source entities.
     * The source field must be a numeric type.
     * Returns to root builder.
     * 
     * @param sourceFieldBuilder function to select source field (must return Number field)
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     * @throws IllegalArgumentException if source field is not a Number type
     */
    public AbstractRootBuilder<R> sum(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        
        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);
        
        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        validateSourceFieldType(sourcePath, Number.class, "sum");
        
        createMapping(sourcePath, MappingType.MANY_TO_ONE_AGGREGATION, AggregationType.SUM);
        return getRootBuilder();
    }
    
    /**
     * Complete mapping as AVG (average) aggregation.
     * Calculates the average of the specified source field across all matching source entities.
     * The source field must be a numeric type.
     * Returns to root builder.
     * 
     * @param sourceFieldBuilder function to select source field (must return Number field)
     * @return root builder for defining additional mappings
     * @throws NullPointerException if sourceFieldBuilder is null
     * @throws IllegalArgumentException if source field is not a Number type
     */
    public AbstractRootBuilder<R> avg(
            UnaryOperator<PropertyNavigationBuilder> sourceFieldBuilder) {
        
        Objects.requireNonNull(sourceFieldBuilder, SOURCE_FIELD_CANNOT_BE_NULL);
        
        PropertyNavigation sourcePath = buildSourcePath(sourceFieldBuilder);
        validateSourceFieldType(sourcePath, Number.class, "avg");
        
        createMapping(sourcePath, MappingType.MANY_TO_ONE_AGGREGATION, AggregationType.AVG);
        return getRootBuilder();
    }

    /**
     * Add filter specification for source data.
     * Overridden to return NumericSourceMappingContext for method chaining.
     *
     * @param whereFunction function that receives navigation builder and spec builder
     * @return this context for method chaining
     * @throws NullPointerException if whereFunction is null
     */
    @Override
    public NumericSourceMappingContext<R, C, S> where(
            java.util.function.BiFunction<PropertyNavigationBuilder,
                com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder<S>,
                com.thy.fss.common.inmemory.specification.Specification<S>> whereFunction) {
        super.where(whereFunction);
        return this;
    }
}
