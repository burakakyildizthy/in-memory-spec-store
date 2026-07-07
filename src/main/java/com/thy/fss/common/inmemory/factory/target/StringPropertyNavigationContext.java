package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.StringSourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specialized context for navigating String properties.
 * Terminal type - NO field() method available.
 * 
 * <p>This context is returned when navigating to a String field in the target entity.
 * Since String is a terminal type (not a complex object), no further field navigation
 * is possible. Only source mapping operations via from() are available.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.name)  // Returns StringPropertyNavigationContext
 *     .from(UserProfile.class,
 *         pk -> pk.field(User_.id),
 *         fk -> fk.field(UserProfile_.userId))
 *     .value(nav -> nav.field(UserProfile_.displayName))
 * </pre>
 * 
 * @param <R> the root entity type
 */
public class StringPropertyNavigationContext<R>
        extends PropertyNavigationContext<R, String> {
    
    /**
     * Package-private constructor for creating String navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public StringPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, String.class, collectionOperations);
    }
    
    // NO field() method - String is a terminal type
    // Only from() method is available (inherited from PropertyNavigationContext)
    
    /**
     * Start source mapping definition using composite key builder.
     * Returns StringSourceMappingContext which provides String-specific type validation.
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return StringSourceMappingContext for String operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> StringSourceMappingContext<R, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            java.util.function.Function<com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder, com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder> keyPairBuilder) {
        return (StringSourceMappingContext<R, S>) super.from(sourceService, keyPairBuilder);
    }

    /**
     * Start source mapping definition with String-specific type safety.
     * Returns StringSourceMappingContext which provides String-specific type validation.
     * 
     * @param <S> the source entity type
     * <p><b>This method is maintained for backward compatibility.</b></p>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity
     * @param foreignKeyBuilder function to build foreign key path on source entity
     * @return StringSourceMappingContext for String operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> StringSourceMappingContext<R, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {
        return (StringSourceMappingContext<R, S>) super.from(sourceService, primaryKeyBuilder, foreignKeyBuilder);
    }
    
    /**
     * Override to return StringSourceMappingContext which provides String-specific
     * type validation for value mappings.
     * 
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths
     * @param foreignKeyPaths the list of foreign key paths
     * @return StringSourceMappingContext for String operations
     */
    @Override
    protected <S> StringSourceMappingContext<R, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        return new StringSourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
}
