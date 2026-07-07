package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.ModelAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * Type-safe navigation builder for Model (complex object) fields.
 * Provides nested field navigation with compile-time type safety.
 * 
 * <p>This class is returned when navigating to a Model field, allowing continued
 * navigation into nested fields with type-safe builders.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * nav.field(User_.address)              // Returns ModelNavigationBuilder
 *    .field(Address_.city)              // Returns StringNavigationBuilder
 *    
 * nav.field(Order_.customer)            // Returns ModelNavigationBuilder
 *    .field(Customer_.contactInfo)      // Returns ModelNavigationBuilder
 *    .field(ContactInfo_.email)         // Returns StringNavigationBuilder
 * </pre>
 * 
 * @param <O> the owner type of the Model field
 * @param <M> the model type
 */
public class ModelNavigationBuilder<O, M> extends PropertyNavigationBuilder {
    
    private final ModelAttribute<O, M> attribute;
    
    /**
     * Package-private constructor.
     * 
     * @param rootClass the root entity class
     * @param path the current field path (will be copied)
     * @param collectionOperations the collection operations metadata (will be copied)
     * @param attribute the Model attribute
     */
    ModelNavigationBuilder(
            Class<?> rootClass,
            LinkedList<MetaAttribute<?, ?>> path,
            List<CollectionOperationMetadata<?, ?>> collectionOperations,
            ModelAttribute<O, M> attribute) {
        super(rootClass, new LinkedList<>(path), new java.util.ArrayList<>(collectionOperations));
        this.attribute = attribute;
    }
    
    /**
     * Gets the Model attribute.
     * 
     * @return the Model attribute
     */
    public ModelAttribute<O, M> getAttribute() {
        return attribute;
    }
    
    // ==================== TYPE-SAFE NESTED FIELD NAVIGATION ====================
    // Override parent class methods to ensure type-specific builders are returned
    // for nested field navigation
    
    /**
     * Navigate to a String field with compile-time type safety.
     * Returns StringNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param attribute the String attribute
     * @return StringNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER> StringNavigationBuilder<FIELD_OWNER> field(
            com.thy.fss.common.inmemory.specification.attribute.StringAttribute<FIELD_OWNER> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a numeric field with compile-time type safety.
     * Returns NumericNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param <N> the numeric type
     * @param attribute the numeric attribute
     * @return NumericNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER, N extends Number> NumericNavigationBuilder<FIELD_OWNER, N> field(
            com.thy.fss.common.inmemory.specification.attribute.NumericMetaAttribute<FIELD_OWNER, N> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a Boolean field with compile-time type safety.
     * Returns BooleanNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param attribute the Boolean attribute
     * @return BooleanNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER> BooleanNavigationBuilder<FIELD_OWNER> field(
            com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<FIELD_OWNER> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to an Enum field with compile-time type safety.
     * Returns EnumNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param <E> the enum type
     * @param attribute the Enum attribute
     * @return EnumNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER, E extends Enum<E>> EnumNavigationBuilder<FIELD_OWNER, E> field(
            com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<FIELD_OWNER, E> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a LocalDateTime field with compile-time type safety.
     * Returns TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param attribute the LocalDateTime attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER> TemporalNavigationBuilder<FIELD_OWNER, java.time.LocalDateTime> field(
            com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute<FIELD_OWNER> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a LocalDate field with compile-time type safety.
     * Returns TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param attribute the LocalDate attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER> TemporalNavigationBuilder<FIELD_OWNER, java.time.LocalDate> field(
            com.thy.fss.common.inmemory.specification.attribute.LocalDateAttribute<FIELD_OWNER> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to an Instant field with compile-time type safety.
     * Returns TemporalNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param attribute the Instant attribute
     * @return TemporalNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER> TemporalNavigationBuilder<FIELD_OWNER, java.time.Instant> field(
            com.thy.fss.common.inmemory.specification.attribute.InstantAttribute<FIELD_OWNER> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a nested Model field with compile-time type safety.
     * Returns ModelNavigationBuilder for continued nested field navigation.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param <NESTED_MODEL> the nested model type
     * @param attribute the Model attribute
     * @return ModelNavigationBuilder for nested field navigation
     */
    @Override
    public <FIELD_OWNER, NESTED_MODEL> ModelNavigationBuilder<FIELD_OWNER, NESTED_MODEL> field(
            com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<FIELD_OWNER, NESTED_MODEL> attribute) {
        return super.field(attribute);
    }
    
    /**
     * Navigate to a Collection field with compile-time type safety.
     * Returns CollectionNavigationBuilder for type-safe SpecificationBuilder.on() usage.
     *
     * @param <FIELD_OWNER> the owner type of the attribute
     * @param <E> the element type
     * @param attribute the Collection attribute
     * @return CollectionNavigationBuilder for type-safe specification building
     */
    @Override
    public <FIELD_OWNER, E> CollectionNavigationBuilder<FIELD_OWNER, E> field(
            com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<FIELD_OWNER, E> attribute) {
        return super.field(attribute);
    }
}
