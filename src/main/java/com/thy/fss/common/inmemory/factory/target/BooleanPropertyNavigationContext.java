package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.BooleanSourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specialized context for navigating Boolean properties.
 * Terminal type - NO field() method available.
 * 
 * <p>This context is returned when navigating to a Boolean field in the target entity.
 * Since Boolean is a terminal type (not a complex object), no further field navigation
 * is possible. Only source mapping operations via from() are available.</p>
 * 
 * <p>When BooleanSourceMappingContext is implemented, this class will override from() 
 * to return BooleanSourceMappingContext which provides Boolean-specific operations 
 * like any() and all().</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.isActive)  // Returns BooleanPropertyNavigationContext
 *     .from(UserStatus.class,
 *         pk -> pk.field(User_.id),
 *         fk -> fk.field(UserStatus_.userId))
 *     .value(nav -> nav.field(UserStatus_.active))
 * </pre>
 * 
 * @param <R> the root entity type
 */
public class BooleanPropertyNavigationContext<R>
        extends PropertyNavigationContext<R, Boolean> {
    
    /**
     * Package-private constructor for creating Boolean navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public BooleanPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, Boolean.class, collectionOperations);
    }
    
    // NO field() method - Boolean is a terminal type
    // Only from() method is available (inherited from PropertyNavigationContext)
    
    /**
     * Start source mapping definition using composite key builder.
     * Returns BooleanSourceMappingContext which provides Boolean-specific type validation.
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return BooleanSourceMappingContext for Boolean operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> BooleanSourceMappingContext<R, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            java.util.function.Function<com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder, com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder> keyPairBuilder) {
        return (BooleanSourceMappingContext<R, S>) super.from(sourceService, keyPairBuilder);
    }

    /**
     * Start source mapping definition with Boolean-specific type safety.
     * Returns BooleanSourceMappingContext which provides Boolean-specific type validation.
     * 
     * @param <S> the source entity type
     * <p><b>This method is maintained for backward compatibility.</b></p>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity
     * @param foreignKeyBuilder function to build foreign key path on source entity
     * @return BooleanSourceMappingContext for Boolean operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> BooleanSourceMappingContext<R, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {
        return (BooleanSourceMappingContext<R, S>) super.from(sourceService, primaryKeyBuilder, foreignKeyBuilder);
    }

    /**
     * Override to return BooleanSourceMappingContext which provides Boolean-specific
     * type validation and operations.
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths
     * @param foreignKeyPaths the list of foreign key paths
     * @return BooleanSourceMappingContext for Boolean operations
     */
    @Override
    protected <S> BooleanSourceMappingContext<R, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        return new BooleanSourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
}
