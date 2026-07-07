package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.navigation.*;
import com.thy.fss.common.inmemory.specification.attribute.*;
import com.thy.fss.common.inmemory.testmodel.Order_;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.User_;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for PropertyNavigationBuilder.
 * Tests pure navigation functionality without generic type parameters at class level.
 */
class PropertyNavigationBuilderTest {


    private static final String NULL_ATTRIBUTE_MSG = "Attribute cannot be null";
    private static final String NULL_COLLECTION_ATTRIBUTE_MSG = "Collection attribute cannot be null";
    private static final String FIELD_NAME = "testField";
    private static final String STATUS_FIELD = "status";
    private static final String AGE_FIELD = "age";

    private PropertyNavigationBuilder builder;
    private LinkedList<MetaAttribute<?, ?>> existingPath;
    private List<CollectionOperationMetadata<?, ?>> existingOperations;

    @BeforeEach
    void setUp() {
        builder = new PropertyNavigationBuilder(User.class);
        existingPath = new LinkedList<>();
        existingOperations = new ArrayList<>();
    }

    @Test
    @DisplayName("Should navigate to String field and return StringNavigationBuilder")
    void shouldNavigateToStringField() {
        StringAttribute<User> stringAttr = new StringAttribute<>(FIELD_NAME, User.class);

        StringNavigationBuilder<User> result = builder.field(stringAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(stringAttr);
        assertThat(result.getPath()).hasSize(1);
        assertThat(result.getPath().get(0)).isEqualTo(stringAttr);
    }

    @Test
    @DisplayName("Should reject null String attribute")
    void shouldRejectNullStringAttribute() {
        assertThatThrownBy(() -> builder.field((StringAttribute<User>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should reject null numeric attribute")
    void shouldRejectNullNumericAttribute() {
        assertThatThrownBy(() -> builder.field((NumericMetaAttribute<User, Long>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to Boolean field and return BooleanNavigationBuilder")
    void shouldNavigateToBooleanField() {
        BooleanAttribute<User> booleanAttr = new BooleanAttribute<>(FIELD_NAME, User.class);

        BooleanNavigationBuilder<User> result = builder.field(booleanAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(booleanAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null Boolean attribute")
    void shouldRejectNullBooleanAttribute() {
        assertThatThrownBy(() -> builder.field((BooleanAttribute<User>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to Enum field and return EnumNavigationBuilder")
    void shouldNavigateToEnumField() {
        EnumAttribute<User, TestStatus> enumAttr = new EnumAttribute<>(STATUS_FIELD, User.class, TestStatus.class);

        EnumNavigationBuilder<User, TestStatus> result = builder.field(enumAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(enumAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null Enum attribute")
    void shouldRejectNullEnumAttribute() {
        assertThatThrownBy(() -> builder.field((EnumAttribute<User, TestStatus>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to LocalDateTime field and return TemporalNavigationBuilder")
    void shouldNavigateToLocalDateTimeField() {
        LocalDateTimeAttribute<User> dateTimeAttr = new LocalDateTimeAttribute<>(FIELD_NAME, User.class);

        TemporalNavigationBuilder<User, LocalDateTime> result = builder.field(dateTimeAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(dateTimeAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null LocalDateTime attribute")
    void shouldRejectNullLocalDateTimeAttribute() {
        assertThatThrownBy(() -> builder.field((LocalDateTimeAttribute<User>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to LocalDate field and return TemporalNavigationBuilder")
    void shouldNavigateToLocalDateField() {
        LocalDateAttribute<User> dateAttr = new LocalDateAttribute<>(FIELD_NAME, User.class);

        TemporalNavigationBuilder<User, LocalDate> result = builder.field(dateAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(dateAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null LocalDate attribute")
    void shouldRejectNullLocalDateAttribute() {
        assertThatThrownBy(() -> builder.field((LocalDateAttribute<User>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to Instant field and return TemporalNavigationBuilder")
    void shouldNavigateToInstantField() {
        InstantAttribute<User> instantAttr = new InstantAttribute<>(FIELD_NAME, User.class);

        TemporalNavigationBuilder<User, Instant> result = builder.field(instantAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(instantAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null Instant attribute")
    void shouldRejectNullInstantAttribute() {
        assertThatThrownBy(() -> builder.field((InstantAttribute<User>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should navigate to Model field and return ModelNavigationBuilder")
    void shouldNavigateToModelField() {
        ModelAttribute<User, TestProfile> modelAttr = new ModelAttribute<>(FIELD_NAME, User.class, TestProfile.class);

        ModelNavigationBuilder<User, TestProfile> result = builder.field(modelAttr);

        assertThat(result).isNotNull();
        assertThat(result.getAttribute()).isEqualTo(modelAttr);
        assertThat(result.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null Model attribute")
    void shouldRejectNullModelAttribute() {
        assertThatThrownBy(() -> builder.field((ModelAttribute<User, TestProfile>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should reject null Collection attribute")
    void shouldRejectNullCollectionAttribute() {
        assertThatThrownBy(() -> builder.field((CollectionAttribute<User, TestProfile>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_COLLECTION_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should create builder with existing path and operations")
    void shouldCreateBuilderWithExistingPathAndOperations() {
        MetaAttribute<?, ?> mockAttribute = mock(MetaAttribute.class);
        existingPath.add(mockAttribute);

        CollectionOperationMetadata<?, ?> mockOperation = mock(CollectionOperationMetadata.class);
        existingOperations.add(mockOperation);

        PropertyNavigationBuilder result = new PropertyNavigationBuilder(
                User.class,
                existingPath,
                existingOperations
        );

        assertThat(result).isNotNull();
        assertThat(result.getRootClass()).isEqualTo(User.class);
        assertThat(result.getPath()).hasSize(1);
        assertThat(result.getCollectionOperations()).hasSize(1);
    }

    @Test
    @DisplayName("Should return immutable collection operations list")
    void shouldReturnCollectionOperations() {
        CollectionOperationMetadata<?, ?> mockOperation = mock(CollectionOperationMetadata.class);
        existingOperations.add(mockOperation);

        PropertyNavigationBuilder result = new PropertyNavigationBuilder(
                User.class,
                existingPath,
                existingOperations
        );

        List<CollectionOperationMetadata<?, ?>> operations = result.getCollectionOperations();

        assertThat(operations).hasSize(1).containsExactly(mockOperation);
    }

    @Test
    @DisplayName("Should preserve existing path when navigating to new field")
    void shouldPreserveExistingPathWhenNavigating() {
        MetaAttribute<?, ?> mockAttribute = mock(MetaAttribute.class);
        existingPath.add(mockAttribute);

        PropertyNavigationBuilder builderWithPath = new PropertyNavigationBuilder(
                User.class,
                existingPath,
                existingOperations
        );

        PropertyNavigationBuilder result = builderWithPath.field(User_.id);

        assertThat(result.getPath()).hasSize(2);
        assertThat(result.getPath().get(0)).isEqualTo(mockAttribute);
        assertThat(result.getPath().get(1)).isEqualTo(User_.id);
    }

    @Test
    @DisplayName("Should preserve collection operations when navigating")
    void shouldPreserveCollectionOperationsWhenNavigating() {
        CollectionOperationMetadata<?, ?> mockOperation = mock(CollectionOperationMetadata.class);
        existingOperations.add(mockOperation);

        PropertyNavigationBuilder builderWithOps = new PropertyNavigationBuilder(
                User.class,
                existingPath,
                existingOperations
        );

        PropertyNavigationBuilder result = builderWithOps.field(User_.id);

        assertThat(result.getCollectionOperations()).hasSize(1);
        assertThat(result.getCollectionOperations().get(0)).isEqualTo(mockOperation);
    }

    @Test
    @DisplayName("Should build PropertyNavigation with collection operations")
    void shouldBuildPropertyNavigationWithCollectionOperations() {
        CollectionOperationMetadata<?, ?> mockOperation = mock(CollectionOperationMetadata.class);
        existingOperations.add(mockOperation);

        PropertyNavigationBuilder builderWithOps = new PropertyNavigationBuilder(
                User.class,
                existingPath,
                existingOperations
        );

        PropertyNavigation navigation = builderWithOps.field(User_.id).build();

        assertThat(navigation).isNotNull();
        assertThat(navigation.getPath()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject null attribute in generic field method")
    void shouldRejectNullAttributeInGenericFieldMethod() {
        assertThatThrownBy(() -> builder.field((MetaAttribute<User, String>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }


    @Test
    @DisplayName("Should chain multiple typed field navigations")
    void shouldChainMultipleTypedFieldNavigations() {
        StringAttribute<User> stringAttr = new StringAttribute<>(FIELD_NAME, User.class);
        LongAttribute<User> longAttr = new LongAttribute<>(AGE_FIELD, User.class);

        PropertyNavigationBuilder result = builder
                .field(stringAttr)
                .field(longAttr);

        assertThat(result.getPath()).hasSize(2);
    }

    private enum TestStatus {
        ACTIVE, INACTIVE
    }

    private static class TestProfile {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    @DisplayName("Should create builder with root class")
    void shouldCreateBuilderWithRootClass() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        assertThat(propertyNavigationBuilder).isNotNull();
        assertThat(propertyNavigationBuilder.getRootClass()).isEqualTo(User.class);
        assertThat(propertyNavigationBuilder.getPath()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null root class")
    void shouldRejectNullRootClass() {
        assertThatThrownBy(() -> new PropertyNavigationBuilder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Root class cannot be null");
    }

    @Test
    @DisplayName("Should navigate to single field")
    void shouldNavigateToSingleField() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigationBuilder result = propertyNavigationBuilder.field(User_.id);
        
        assertThat(result).isNotNull();
        assertThat(result.getPath()).hasSize(1);
        assertThat(result.getPath().get(0)).isEqualTo(User_.id);
    }

    @Test
    @DisplayName("Should navigate to nested fields")
    void shouldNavigateToNestedFields() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigationBuilder result = propertyNavigationBuilder
                .field(User_.profile)
                .field(User_.name);
        
        assertThat(result.getPath()).hasSize(2);
    }

    @Test
    @DisplayName("Should build PropertyNavigation from path")
    void shouldBuildPropertyNavigation() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigation navigation = propertyNavigationBuilder.field(User_.id).build();
        
        assertThat(navigation).isNotNull();
        assertThat(navigation.getPath()).hasSize(1);
        assertThat(navigation.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject build() on empty path")
    void shouldRejectBuildOnEmptyPath() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        assertThatThrownBy(propertyNavigationBuilder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot build PropertyNavigation with empty path");
    }

    @Test
    @DisplayName("Should navigate to collection field")
    void shouldNavigateToCollectionField() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigationBuilder result = propertyNavigationBuilder.field(User_.name);
        
        assertThat(result).isNotNull();
        assertThat(result.getPath()).hasSize(1);
    }


    @Test
    @DisplayName("Should chain collection navigation with field access")
    void shouldChainCollectionNavigationWithFieldAccess() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigationBuilder result = propertyNavigationBuilder
                .field(User_.id)
                .field(Order_.totalAmount);
        
        assertThat(result).isNotNull();
        assertThat(result.getPath()).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("Should maintain immutability - original builder unchanged")
    void shouldMaintainImmutability() {
        PropertyNavigationBuilder propertyNavigationBuilder = new PropertyNavigationBuilder(User.class);
        
        PropertyNavigationBuilder extended = propertyNavigationBuilder.field(User_.id);
        
        assertThat(propertyNavigationBuilder.getPath()).isEmpty();
        assertThat(extended.getPath()).hasSize(1);
    }
}
