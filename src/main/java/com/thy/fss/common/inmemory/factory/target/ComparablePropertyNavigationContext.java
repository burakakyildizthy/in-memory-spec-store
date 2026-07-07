package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.ComparableSourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specialized context for navigating Comparable properties.
 * Terminal type - NO field() method available.
 * 
 * <p>This context is returned when navigating to a Comparable field (LocalDateTime, 
 * LocalDate, Instant, etc.) in the target entity. Since Comparable types are terminal 
 * types (not complex objects), no further field navigation is possible. Only source 
 * mapping operations via from() are available.</p>
 * 
 * <p>When ComparableSourceMappingContext is implemented, this class will override from() 
 * to return ComparableSourceMappingContext which provides Comparable-specific aggregation 
 * operations like min() and max().</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.lastLoginDate)  // Returns ComparablePropertyNavigationContext&lt;User, LocalDateTime&gt;
 *     .from(LoginEvent.class,
 *         pk -> pk.field(User_.id),
 *         fk -> fk.field(LoginEvent_.userId))
 *     .max(nav -> nav.field(LoginEvent_.timestamp))  // max() available via ComparableSourceMappingContext
 * </pre>
 * 
 * @param <R> the root entity type
 * @param <C> the current comparable type (extends Comparable)
 */
public class ComparablePropertyNavigationContext<R, C extends Comparable<C>>
        extends PropertyNavigationContext<R, C> {
    
    /**
     * Package-private constructor for creating comparable navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param currentType the comparable type class
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public ComparablePropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<C> currentType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, currentType, collectionOperations);
    }
    
    // NO field() method - Comparable types are terminal types
    // Only from() method is available (inherited from PropertyNavigationContext)
    
    /**
     * Start source mapping definition using composite key builder.
     * Returns ComparableSourceMappingContext which provides Comparable-specific aggregation operations.
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return ComparableSourceMappingContext for Comparable operations (min, max, etc.)
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> ComparableSourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            java.util.function.Function<com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder, com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder> keyPairBuilder) {
        return (ComparableSourceMappingContext<R, C, S>) super.from(sourceService, keyPairBuilder);
    }

    /**
     * Start source mapping definition with Comparable-specific type safety.
     * Returns ComparableSourceMappingContext which provides Comparable-specific aggregation operations.
     * 
     * @param <S> the source entity type
     * <p><b>This method is maintained for backward compatibility.</b></p>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity
     * @param foreignKeyBuilder function to build foreign key path on source entity
     * @return ComparableSourceMappingContext for Comparable operations (min, max, etc.)
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> ComparableSourceMappingContext<R, C, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {
        return (ComparableSourceMappingContext<R, C, S>) super.from(sourceService, primaryKeyBuilder, foreignKeyBuilder);
    }
    
    /**
     * Override to return ComparableSourceMappingContext which provides Comparable-specific
     * aggregation operations like min() and max().
     * 
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths
     * @param foreignKeyPaths the list of foreign key paths
     * @return ComparableSourceMappingContext for Comparable operations
     */
    @Override
    protected <S> ComparableSourceMappingContext<R, C, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        return new ComparableSourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
}
