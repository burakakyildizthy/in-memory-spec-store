package com.thy.fss.common.inmemory.specification.builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.thy.fss.common.inmemory.factory.navigation.BooleanNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.CollectionNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.EnumNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.NumericNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.StringNavigationBuilder;
import com.thy.fss.common.inmemory.factory.navigation.TemporalNavigationBuilder;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute;
import com.thy.fss.common.inmemory.specification.attribute.EnumAttribute;
import com.thy.fss.common.inmemory.specification.attribute.InstantAttribute;
import com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute;
import com.thy.fss.common.inmemory.specification.attribute.LocalDateAttribute;
import com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute;
import com.thy.fss.common.inmemory.specification.attribute.LongAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;

/**
 * Builder for creating type-safe Specification instances using meta models.
 * Provides a fluent API for building complex queries with compile-time type safety.
 * <p>
 * This updated version uses StaticMetaModel attributes instead of string field paths,
 * eliminating reflection usage and providing compile-time validation.
 * <p>
 * This class combines both the builder functionality and field specification operations
 * to provide a unified API for creating specifications.
 *
 * @param <T> the type of objects this builder creates specifications for
 */
public class SpecificationBuilder<T> {

    private final SpecificationService<T> specificationService;

    /**
     * Creates a new specification builder for the given specification service.
     *
     * @param specificationService the specification service to use for building specifications
     * @throws IllegalArgumentException if specificationService is null
     */
    public SpecificationBuilder(SpecificationService<T> specificationService) {
        if (specificationService == null) {
            throw new IllegalArgumentException("Specification service cannot be null");
        }
        this.specificationService = specificationService;
    }

    /**
     * Creates a new specification builder for the given specification service.
     *
     * @param <T>                  the type of objects to build specifications for
     * @param specificationService the specification service to use
     * @return a new SpecificationBuilder instance
     * @throws IllegalArgumentException if specificationService is null
     */
    public static <T> SpecificationBuilder<T> forService(SpecificationService<T> specificationService) {
        return new SpecificationBuilder<>(specificationService);
    }

    protected SpecificationService<T> getSpecificationService() {
        return specificationService;
    }

    /**
     * Gets the target class for this builder.
     *
     * @return the target class
     */
    public Class<T> getTargetClass() {
        return specificationService.getEntityClass();
    }

    // ==================== TYPE-SAFE FIELD METHODS ====================

    /**
     * Creates a type-safe field builder for String attributes.
     * Only string-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.StringFieldBuilder<T, Specification<T>> where(StringAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.StringFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Integer attributes.
     * Only numeric-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<T, Integer, Specification<T>> where(IntegerAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Long attributes.
     * Only numeric-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<T, Long, Specification<T>> where(LongAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Double attributes.
     * Only numeric-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<T, Double, Specification<T>> where(DoubleAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Boolean attributes.
     * Only boolean-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<T, Specification<T>> where(BooleanAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Enum attributes.
     * Only enum-appropriate operations will be available.
     */
    public <E extends Enum<E>> TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<T, E, Specification<T>> where(EnumAttribute<T, E> attribute) {
        return new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Collection attributes.
     * Only collection-appropriate operations will be available.
     */
    public <E> TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<T, E, Specification<T>> where(CollectionAttribute<T, E> attribute) {
        return new TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for LocalDateTime attributes.
     * Only temporal-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<T, LocalDateTime, Specification<T>> where(LocalDateTimeAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for LocalDate attributes.
     * Only temporal-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<T, LocalDate, Specification<T>> where(LocalDateAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    /**
     * Creates a type-safe field builder for Instant attributes.
     * Only temporal-appropriate operations will be available.
     */
    public TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<T, Instant, Specification<T>> where(InstantAttribute<T> attribute) {
        return new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(attribute, specificationService, spec -> spec);
    }

    // ==================== TYPE-SAFE ON() METHODS FOR PROPERTY NAVIGATION BUILDER INTEGRATION ====================
    
    /**
     * Creates a type-safe String field specification builder from StringNavigationBuilder.
     * This method provides compile-time type safety for String field specifications.
     * 
     * @param navigationBuilder the String navigation builder
     * @return StringFieldBuilder for String-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public TypeSafeFieldSpecificationBuilder.StringFieldBuilder<T, Specification<T>> 
    on(StringNavigationBuilder<?> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.StringFieldBuilder<>(
            (com.thy.fss.common.inmemory.specification.attribute.StringAttribute<T>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe numeric field specification builder from NumericNavigationBuilder.
     * This method provides compile-time type safety for numeric field specifications.
     * 
     * @param navigationBuilder the numeric navigation builder
     * @return NumericFieldBuilder for numeric-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public TypeSafeFieldSpecificationBuilder.NumericFieldBuilder 
    on(NumericNavigationBuilder navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        // Safe cast: All Number types used in practice (Integer, Long, Double) implement Comparable
        return new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder(
            navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe Boolean field specification builder from BooleanNavigationBuilder.
     * This method provides compile-time type safety for Boolean field specifications.
     * 
     * @param navigationBuilder the Boolean navigation builder
     * @return BooleanFieldBuilder for Boolean-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<T, Specification<T>> 
    on(BooleanNavigationBuilder<?> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<>(
            (com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<T>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe Enum field specification builder from EnumNavigationBuilder.
     * This method provides compile-time type safety for Enum field specifications.
     * 
     * @param <E> the enum type
     * @param navigationBuilder the Enum navigation builder
     * @return EnumFieldBuilder for Enum-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<T, E, Specification<T>> 
    on(EnumNavigationBuilder<?, E> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
            (com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<T, E>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe temporal field specification builder from TemporalNavigationBuilder.
     * This method provides compile-time type safety for temporal field specifications.
     * 
     * @param <D> the temporal type (LocalDateTime, LocalDate, Instant)
     * @param navigationBuilder the temporal navigation builder
     * @return TemporalFieldBuilder for temporal-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public <D> TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<T, D, Specification<T>> 
    on(TemporalNavigationBuilder<?, D> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
            (MetaAttribute<T, D>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe collection field specification builder from CollectionNavigationBuilder.
     * This method provides compile-time type safety for collection field specifications.
     * 
     * @param <E> the element type
     * @param navigationBuilder the collection navigation builder
     * @return CollectionFieldBuilder for collection-specific operations
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public <E> TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<T, E, Specification<T>> 
    on(CollectionNavigationBuilder<?, E> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<>(
            (CollectionAttribute<T, E>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }
    
    /**
     * Creates a type-safe model field specification builder from ModelNavigationBuilder.
     * This method provides compile-time type safety for model field specifications.
     * Allows null checks on model-typed fields.
     * 
     * @param <M> the model type
     * @param navigationBuilder the model navigation builder
     * @return ModelFieldBuilder for model-specific operations (isNull, isNotNull, equalTo, etc.)
     * @throws IllegalArgumentException if navigationBuilder is null
     */
    @SuppressWarnings("unchecked")
    public <M> TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<T, M, Specification<T>> 
    on(com.thy.fss.common.inmemory.factory.navigation.ModelNavigationBuilder<?, M> navigationBuilder) {
        Objects.requireNonNull(navigationBuilder, "Navigation builder cannot be null");
        return new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
            (com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, M>) navigationBuilder.getAttribute(),
            specificationService,
            spec -> spec,
            navigationBuilder.getPath(),
            navigationBuilder.getCollectionOperations()
        );
    }

    // ==================== GENERIC ON() METHOD (DEPRECATED - FOR BACKWARD COMPATIBILITY) ====================

    /**
     * Creates a type-safe specification builder for a field from a PropertyNavigationBuilder.
     * This method integrates field path navigation with specification creation.
     * The appropriate builder type is determined at runtime based on the field's attribute type.
     * 
     * @deprecated Use type-safe overloaded on() methods instead. This method will be removed in future versions.
     * 
     * Usage example:
     * <pre>
     * // In where() lambda
     * .where((nav, spec) -> spec.on(nav.field(Order_.status)).eq("COMPLETED"))
     * 
     * // For String field
     * TypeSafeFieldSpecificationBuilder.StringFieldBuilder&lt;User, Specification&lt;User&gt;&gt; builder = 
     *     specBuilder.on(navBuilder.field(User_.name));
     * 
     * // For Integer field
     * TypeSafeFieldSpecificationBuilder.NumericFieldBuilder&lt;User, Integer, Specification&lt;User&gt;&gt; builder = 
     *     specBuilder.on(navBuilder.field(User_.age));
     * </pre>
     *
     * @param <BUILDER> the type-safe field specification builder type
     * @param navigationBuilder the property navigation builder pointing to a field
     * @return a type-safe field specification builder for the field
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <BUILDER extends TypeSafeFieldSpecificationBuilder.BaseOperations<T, ?, Specification<T>>>
    BUILDER on(PropertyNavigationBuilder navigationBuilder) {
        
        java.util.List<MetaAttribute<?, ?>> fieldPath = navigationBuilder.getPath();
        if (fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Field path cannot be empty");
        }
        
        MetaAttribute<?, ?> lastAttribute = fieldPath.get(fieldPath.size() - 1);
        
        // String field
        if (lastAttribute instanceof StringAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.StringFieldBuilder<>(
                (StringAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Integer field
        if (lastAttribute instanceof IntegerAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(
                (IntegerAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Long field
        if (lastAttribute instanceof LongAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(
                (LongAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Double field
        if (lastAttribute instanceof DoubleAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(
                (DoubleAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Boolean field
        if (lastAttribute instanceof BooleanAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<>(
                (BooleanAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Enum field
        if (lastAttribute instanceof EnumAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
                (EnumAttribute<T, ?>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // LocalDateTime field
        if (lastAttribute instanceof LocalDateTimeAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
                (LocalDateTimeAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // LocalDate field
        if (lastAttribute instanceof LocalDateAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
                (LocalDateAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Instant field
        if (lastAttribute instanceof InstantAttribute) {
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
                (InstantAttribute<T>) lastAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Collection field
        if (lastAttribute instanceof CollectionAttribute) {
            CollectionAttribute<T, ?> collectionAttribute = (CollectionAttribute<T, ?>) lastAttribute;
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<>(
                collectionAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        // Model field (complex object)
        if (lastAttribute instanceof com.thy.fss.common.inmemory.specification.attribute.ModelAttribute) {
            com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, ?> modelAttribute = 
                (com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, ?>) lastAttribute;
            return (BUILDER) new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
                modelAttribute,
                specificationService,
                spec -> spec,
                fieldPath
            );
        }
        
        throw new IllegalArgumentException(
            "Unsupported attribute type: " + (lastAttribute != null ? lastAttribute.getClass().getSimpleName() : "null") + 
            " for field: " + (lastAttribute != null ? lastAttribute.getName() : "unknown")
        );
    }
}
