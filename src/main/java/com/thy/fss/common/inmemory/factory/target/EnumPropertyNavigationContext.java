package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.source.EnumSourceMappingContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Specialized context for navigating Enum properties.
 * Terminal type - NO field() method available.
 * 
 * <p>This context is returned when navigating to an Enum field in the target entity.
 * Since Enum is a terminal type (not a complex object), no further field navigation
 * is possible. Only source mapping operations via from() are available.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * factory.buildInMemoryStore(User.class)
 *     .target(User_.status)  // Returns EnumPropertyNavigationContext
 *     .from(UserProfile.class,
 *         pk -> pk.field(User_.id),
 *         fk -> fk.field(UserProfile_.userId))
 *     .value(nav -> nav.field(UserProfile_.accountStatus))
 * </pre>
 * 
 * @param <R> the root entity type
 * @param <E> the enum type
 */
public class EnumPropertyNavigationContext<R, E extends Enum<E>>
        extends PropertyNavigationContext<R, E> {
    
    /**
     * Package-private constructor for creating Enum navigation context.
     * 
     * @param rootBuilder the root builder instance
     * @param targetPath the current target field path
     * @param enumType the enum type class
     * @param collectionOperations the collection operations metadata
     * @throws NullPointerException if any required parameter is null
     */
    public EnumPropertyNavigationContext(
            AbstractRootBuilder<R> rootBuilder,
            LinkedList<MetaAttribute<?, ?>> targetPath,
            Class<E> enumType,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        super(rootBuilder, targetPath, enumType, collectionOperations);
    }
    
    // NO field() method - Enum is a terminal type
    // Only from() method is available (inherited from PropertyNavigationContext)
    
    /**
     * Start source mapping definition using composite key builder.
     * Returns EnumSourceMappingContext which provides Enum-specific type validation.
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param keyPairBuilder function that builds multiple PK-FK pairs using KeyPairBuilder
     * @return EnumSourceMappingContext for Enum operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> EnumSourceMappingContext<R, E, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            java.util.function.Function<com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder, com.thy.fss.common.inmemory.factory.navigation.KeyPairBuilder> keyPairBuilder) {
        return (EnumSourceMappingContext<R, E, S>) super.from(sourceService, keyPairBuilder);
    }

    /**
     * Start source mapping definition with Enum-specific type safety.
     * Returns EnumSourceMappingContext which provides Enum-specific type validation.
     * 
     * @param <S> the source entity type
     * <p><b>This method is maintained for backward compatibility.</b></p>
     *
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyBuilder function to build primary key path on target entity
     * @param foreignKeyBuilder function to build foreign key path on source entity
     * @return EnumSourceMappingContext for Enum operations
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public <S> EnumSourceMappingContext<R, E, S> from(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            UnaryOperator<PropertyNavigationBuilder> primaryKeyBuilder,
            UnaryOperator<PropertyNavigationBuilder> foreignKeyBuilder) {
        return (EnumSourceMappingContext<R, E, S>) super.from(sourceService, primaryKeyBuilder, foreignKeyBuilder);
    }
    
    /**
     * Override to return EnumSourceMappingContext which provides Enum-specific
     * type validation for value mappings.
     * 
     * @param <S> the source entity type
     * @param sourceService the source entity specification service
     * @param primaryKeyPaths the list of primary key paths
     * @param foreignKeyPaths the list of foreign key paths
     * @return EnumSourceMappingContext for Enum operations
     */
    @Override
    protected <S> EnumSourceMappingContext<R, E, S> createSourceMappingContext(
            com.thy.fss.common.inmemory.specification.SpecificationService<S> sourceService,
            List<PropertyNavigation> primaryKeyPaths,
            List<PropertyNavigation> foreignKeyPaths) {
        return new EnumSourceMappingContext<>(this, sourceService, primaryKeyPaths, foreignKeyPaths);
    }
}
