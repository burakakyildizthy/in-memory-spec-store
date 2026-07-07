package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.NumericSourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specialized context for navigating numeric properties.
 * Terminal type - NO field() method available.
 * 
 * <p>This context is returned when navigating to a numeric field (Integer, Long, Double, 
 * BigDecimal, etc.) in the target entity. Since numeric types are terminal types 
 * (not complex objects), no further field navigation is possible. Only source mapping 
 * operations via from() are available.</p>
 * 
 * <p>When NumericSourceMappingContext is implemented, this class will override from() 
 * to return NumericSourceMappingContext which provides numeric-specific aggregation 
 * operations like sum() and avg().</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.totalSpent)  // Returns NumericPropertyNavigationContext&lt;User, BigDecimal&gt;
 *     .from(Order.class,
 *         pk -> pk.field(User_.id),
 *         fk -> fk.field(Order_.userId))
 *     .sum(nav -> nav.field(Order_.amount))  // sum() available via NumericSourceMappingContext
 * </pre>
 * 
 * @param <R> the root entity type
 * @param <C> the current numeric type (extends Number)
 */
public class NumericPropertyNavigationContext<R, C extends Number>
        extends PropertyNavigationContext<R, C> {
    
    /**
     * Package-private constructor for creating numeric navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param currentType the numeric type class
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public NumericPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<C> currentType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, currentType, collectionOperations);
    }
    
    // NO field() method - numeric types are terminal types
    // Only from() method is available (inherited from PropertyNavigationContext)
    
    /**
     * Start source mapping definition using composite key builder.
     * Returns NumericSourceMappingContext which provides numeric-specific aggregation operations.
     *
     * <p><b>Dashboard vs Store behavior:</b></p>
     * <ul>
     * <li><b>Store:</b> Primary and foreign key paths are used to establish relationships</li>
     * <li><b>Dashboard:</b> Primary and foreign key paths are IGNORED (Dashboard has no primary datasource)</li>
     * </ul>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return NumericSourceMappingContext for numeric operations (sum, avg, etc.)
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> NumericSourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            java.util.function.Function<com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder, com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder> keyPairBuilder) {
        return (NumericSourceMappingContext<R, C, S>) super.from(sourceService, keyPairBuilder);
    }

    /**
     * Start source mapping definition with numeric-specific type safety.
     * Returns NumericSourceMappingContext which provides numeric-specific aggregation operations.
     * 
     * <p><b>This method is maintained for backward compatibility.</b></p>
     *
     * <p><b>Dashboard vs Store behavior:</b></p>
     * <ul>
     * <li><b>Store:</b> Primary and foreign key paths are used to establish relationships</li>
     * <li><b>Dashboard:</b> Primary and foreign key paths are IGNORED (Dashboard has no primary datasource)</li>
     * </ul>
     * 
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity (ignored for Dashboard)
     * @param foreignKeyBuilder function to build foreign key path on source entity (ignored for Dashboard)
     * @return NumericSourceMappingContext for numeric operations (sum, avg, etc.)
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> NumericSourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {
        return (NumericSourceMappingContext<R, C, S>) super.from(sourceService, primaryKeyBuilder, foreignKeyBuilder);
    }
    
    /**
     * Override to return NumericSourceMappingContext which provides numeric-specific
     * aggregation operations like sum() and avg().
     * 
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths
     * @param foreignKeyPaths the list of foreign key paths
     * @return NumericSourceMappingContext for numeric operations
     */
    @Override
    protected <S> NumericSourceMappingContext<R, C, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        return new NumericSourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
}
