package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.factory.navigation.*;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.*;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for SpecificationBuilder - especially the deprecated on(PropertyNavigationBuilder)
 * method and the model navigation on() method.
 */
@DisplayName("SpecificationBuilder Coverage Tests")
class SpecificationBuilderCoverageTest {

    @AfterEach
    void cleanup() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    private final SpecificationBuilder<TestEntity> builder =
            SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE);

    // ==================== Deprecated on(PropertyNavigationBuilder) ====================

    @Nested
    @DisplayName("Deprecated on(PropertyNavigationBuilder) Tests")
    @SuppressWarnings("deprecation")
    class DeprecatedOnTests {

        @Test
        @DisplayName("String attribute via generic on()")
        void stringAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.name);
            TypeSafeFieldSpecificationBuilder.StringFieldBuilder<TestEntity, Specification<TestEntity>> result =
                    builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Integer attribute via generic on()")
        void integerAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.age);
            TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<TestEntity, Integer, Specification<TestEntity>> result =
                    builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Long attribute via generic on()")
        void longAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.id);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Double attribute via generic on()")
        void doubleAttribute() {
            var doubleAttr = new DoubleAttribute<TestEntity>("price", TestEntity.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(doubleAttr);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Boolean attribute via generic on()")
        void booleanAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.available);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Enum attribute via generic on()")
        void enumAttribute() {
            var enumAttr = new EnumAttribute<TestEntity, TestStatus>("status", TestEntity.class, TestStatus.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(enumAttr);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("LocalDateTime attribute via generic on()")
        void localDateTimeAttribute() {
            var attr = new LocalDateTimeAttribute<TestEntity>("createdAt", TestEntity.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(attr);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("LocalDate attribute via generic on()")
        void localDateAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.birthDate);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Instant attribute via generic on()")
        void instantAttribute() {
            var attr = new InstantAttribute<TestEntity>("timestamp", TestEntity.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(attr);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Collection attribute via generic on()")
        void collectionAttribute() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(TestEntity_.tags);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Model attribute via generic on()")
        void modelAttribute() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            nav = nav.field(modelAttr);
            var result = builder.on(nav);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Empty path throws IllegalArgumentException")
        void emptyPath() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            // Don't add any field
            assertThatThrownBy(() -> builder.on(nav))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Field path cannot be empty");
        }
    }

    // ==================== Type-safe on() with ModelNavigationBuilder ====================

    @Nested
    @DisplayName("on(ModelNavigationBuilder) Tests")
    class ModelNavigationTests {

        @Test
        @DisplayName("Model navigation isNull")
        void modelNavigationIsNull() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            var modelNav = nav.field(modelAttr);
            Specification<TestEntity> spec = builder.on(modelNav).isNull();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Model navigation isNotNull")
        void modelNavigationIsNotNull() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            var modelNav = nav.field(modelAttr);
            Specification<TestEntity> spec = builder.on(modelNav).isNotNull();
            assertThat(spec).isNotNull();
        }
    }

    // ==================== Null checks on type-safe on() methods ====================

    @Nested
    @DisplayName("Null Navigation Builder Tests")
    class NullNavigationTests {

        @Test
        @DisplayName("on(StringNavigationBuilder) null throws NPE")
        void stringNull() {
            assertThatThrownBy(() -> builder.on((StringNavigationBuilder<?>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(NumericNavigationBuilder) null throws NPE")
        void numericNull() {
            assertThatThrownBy(() -> builder.on((NumericNavigationBuilder) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(BooleanNavigationBuilder) null throws NPE")
        void booleanNull() {
            assertThatThrownBy(() -> builder.on((BooleanNavigationBuilder<?>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(EnumNavigationBuilder) null throws NPE")
        void enumNull() {
            assertThatThrownBy(() -> builder.on((EnumNavigationBuilder<?, ?>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(TemporalNavigationBuilder) null throws NPE")
        void temporalNull() {
            assertThatThrownBy(() -> builder.on((TemporalNavigationBuilder<?, ?>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(CollectionNavigationBuilder) null throws NPE")
        void collectionNull() {
            assertThatThrownBy(() -> builder.on((CollectionNavigationBuilder<?, ?>) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("on(ModelNavigationBuilder) null throws NPE")
        void modelNull() {
            assertThatThrownBy(() -> builder.on((com.thy.fss.common.inmemory.factory.navigation.ModelNavigationBuilder<?, ?>) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
