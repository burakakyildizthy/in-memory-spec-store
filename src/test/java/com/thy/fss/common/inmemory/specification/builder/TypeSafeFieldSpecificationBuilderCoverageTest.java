package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.*;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for TypeSafeFieldSpecificationBuilder inner classes.
 * Tests ModelFieldBuilder, BooleanFieldBuilder, EnumFieldBuilder,
 * CollectionFieldBuilder and BaseOperations methods not covered elsewhere.
 */
@DisplayName("TypeSafeFieldSpecificationBuilder Coverage Tests")
class TypeSafeFieldSpecificationBuilderCoverageTest {

    @AfterEach
    void cleanup() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== BaseOperations ====================

    @Nested
    @DisplayName("BaseOperations Tests")
    class BaseOperationsTests {

        @Test
        @DisplayName("notEqualTo creates specification")
        void notEqualTo() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).notEqualTo("Bob");
            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName("Alice");
            assertThat(spec.toPredicate().test(entity)).isTrue();
        }

        @Test
        @DisplayName("in with collection creates specification")
        void inCollection() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).in(Arrays.asList("Alice", "Bob"));
            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName("Alice");
            assertThat(spec.toPredicate().test(entity)).isTrue();
        }

        @Test
        @DisplayName("in with varargs creates specification")
        void inVarargs() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).in("Alice", "Bob");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("notIn with collection creates specification")
        void notInCollection() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).notIn(Arrays.asList("Alice", "Bob"));
            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName("Charlie");
            assertThat(spec.toPredicate().test(entity)).isTrue();
        }

        @Test
        @DisplayName("notIn with varargs creates specification")
        void notInVarargs() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).notIn("Alice", "Bob");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNull creates specification")
        void isNullSpec() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isNull();
            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName(null);
            assertThat(spec.toPredicate().test(entity)).isTrue();
        }

        @Test
        @DisplayName("isNotNull creates specification")
        void isNotNullSpec() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isNotNull();
            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName("hello");
            assertThat(spec.toPredicate().test(entity)).isTrue();
        }
    }

    // ==================== StringFieldBuilder ====================

    @Nested
    @DisplayName("StringFieldBuilder Tests")
    class StringFieldBuilderTests {

        @Test
        @DisplayName("contains creates specification")
        void contains() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).contains("ali");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("startsWith creates specification")
        void startsWith() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).startsWith("Al");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("endsWith creates specification")
        void endsWith() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).endsWith("ce");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("matches with string regex")
        void matchesString() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).matches("A.*");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("matches with Pattern")
        void matchesPattern() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).matches(Pattern.compile("A.*"));
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isEmpty creates specification")
        void isEmpty() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isEmpty();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isBlank creates specification")
        void isBlank() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isBlank();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNotBlank creates specification")
        void isNotBlank() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isNotBlank();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNotEmpty creates specification")
        void isNotEmpty() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.name).isNotEmpty();
            assertThat(spec).isNotNull();
        }
    }

    // ==================== NumericFieldBuilder ====================

    @Nested
    @DisplayName("NumericFieldBuilder Tests")
    class NumericFieldBuilderTests {

        @Test
        @DisplayName("greaterThan")
        void greaterThan() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.age).greaterThan(20);
            assertThat(spec).isNotNull();
            TestEntity e = new TestEntity();
            e.setAge(25);
            assertThat(spec.toPredicate().test(e)).isTrue();
        }

        @Test
        @DisplayName("lessThan")
        void lessThan() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.age).lessThan(30);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("greaterThanOrEqual")
        void gte() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.age).greaterThanOrEqual(20);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("lessThanOrEqual")
        void lte() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.age).lessThanOrEqual(30);
            assertThat(spec).isNotNull();
        }
    }

    // ==================== BooleanFieldBuilder ====================

    @Nested
    @DisplayName("BooleanFieldBuilder Tests")
    class BooleanFieldBuilderTests {

        @Test
        @DisplayName("isTrue creates specification")
        void isTrue() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.available).isTrue();
            assertThat(spec).isNotNull();
            TestEntity e = new TestEntity();
            e.setAvailable(true);
            assertThat(spec.toPredicate().test(e)).isTrue();
        }

        @Test
        @DisplayName("isFalse creates specification")
        void isFalse() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.available).isFalse();
            assertThat(spec).isNotNull();
            TestEntity e = new TestEntity();
            e.setAvailable(false);
            assertThat(spec.toPredicate().test(e)).isTrue();
        }

        @Test
        @DisplayName("equalTo for boolean")
        void equalTo() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.available).equalTo(true);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNull for boolean")
        void isNull() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.available).isNull();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNotNull for boolean")
        void isNotNull() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.available).isNotNull();
            assertThat(spec).isNotNull();
        }
    }

    // ==================== CollectionFieldBuilder ====================

    @Nested
    @DisplayName("CollectionFieldBuilder Tests")
    class CollectionFieldBuilderTests {

        @Test
        @DisplayName("contains element")
        void contains() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags).contains("tag1");
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("getElementType returns correct type")
        void getElementType() {
            var builder = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags);
            assertThat(builder.getElementType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("collectionAny with element spec")
        void collectionAny() {
            Specification<String> elementSpec = () -> s -> s.startsWith("a");
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags).collectionAny(elementSpec);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("collectionAll with element spec")
        void collectionAll() {
            Specification<String> elementSpec = () -> s -> s.length() > 0;
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags).collectionAll(elementSpec);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("collectionNone with element spec")
        void collectionNone() {
            Specification<String> elementSpec = () -> s -> s.equals("bad");
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags).collectionNone(elementSpec);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isNull for collection")
        void isNull() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.tags).isNull();
            assertThat(spec).isNotNull();
        }
    }

    // ==================== TemporalFieldBuilder ====================

    @Nested
    @DisplayName("TemporalFieldBuilder Tests")
    class TemporalFieldBuilderTests {

        @Test
        @DisplayName("isBefore")
        void isBefore() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).isBefore(LocalDate.now());
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isAfter")
        void isAfter() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).isAfter(LocalDate.of(2000, 1, 1));
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isOnOrBefore")
        void isOnOrBefore() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).isOnOrBefore(LocalDate.now());
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("isOnOrAfter")
        void isOnOrAfter() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).isOnOrAfter(LocalDate.of(1990, 1, 1));
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("greaterThan for temporal")
        void greaterThan() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).greaterThan(LocalDate.of(2000, 1, 1));
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("lessThan for temporal")
        void lessThan() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).lessThan(LocalDate.now());
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("greaterThanOrEqual for temporal")
        void gte() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).greaterThanOrEqual(LocalDate.of(2000, 1, 1));
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("lessThanOrEqual for temporal")
        void lte() {
            Specification<TestEntity> spec = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE)
                    .where(TestEntity_.birthDate).lessThanOrEqual(LocalDate.now());
            assertThat(spec).isNotNull();
        }
    }

    // ==================== ModelFieldBuilder ====================

    @Nested
    @DisplayName("ModelFieldBuilder Tests")
    class ModelFieldBuilderTests {

        @Test
        @DisplayName("Model field isNull")
        void modelIsNull() {
            // Create a model attribute for testing
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            var builder = new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
                    modelAttr, TestEntitySpecificationService.INSTANCE, spec -> spec);
            Specification<TestEntity> spec = builder.isNull();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Model field isNotNull")
        void modelIsNotNull() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            var builder = new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
                    modelAttr, TestEntitySpecificationService.INSTANCE, spec -> spec);
            Specification<TestEntity> spec = builder.isNotNull();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Model field with field path constructor")
        void modelWithFieldPath() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            var builder = new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
                    modelAttr, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(modelAttr));
            Specification<TestEntity> spec = builder.isNull();
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Model field with collection operations constructor")
        void modelWithCollectionOps() {
            var modelAttr = new ModelAttribute<TestEntity, Object>("nested", TestEntity.class, Object.class);
            var builder = new TypeSafeFieldSpecificationBuilder.ModelFieldBuilder<>(
                    modelAttr, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(modelAttr), null);
            Specification<TestEntity> spec = builder.equalTo(null);
            assertThat(spec).isNotNull();
        }
    }

    // ==================== EnumFieldBuilder ====================

    @Nested
    @DisplayName("EnumFieldBuilder Tests")
    class EnumFieldBuilderTests {

        enum TestStatus { ACTIVE, INACTIVE }

        @Test
        @DisplayName("Enum equalTo")
        void enumEqualTo() {
            var enumAttr = new EnumAttribute<TestEntity, TestStatus>("status", TestEntity.class, TestStatus.class);
            var builder = new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
                    enumAttr, TestEntitySpecificationService.INSTANCE, spec -> spec);
            Specification<TestEntity> spec = builder.equalTo(TestStatus.ACTIVE);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Enum in with varargs")
        void enumIn() {
            var enumAttr = new EnumAttribute<TestEntity, TestStatus>("status", TestEntity.class, TestStatus.class);
            var builder = new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
                    enumAttr, TestEntitySpecificationService.INSTANCE, spec -> spec);
            Specification<TestEntity> spec = builder.in(TestStatus.ACTIVE, TestStatus.INACTIVE);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Enum with field path constructor")
        void enumWithFieldPath() {
            var enumAttr = new EnumAttribute<TestEntity, TestStatus>("status", TestEntity.class, TestStatus.class);
            var builder = new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
                    enumAttr, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(enumAttr));
            Specification<TestEntity> spec = builder.notEqualTo(TestStatus.INACTIVE);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Enum with collection ops constructor")
        void enumWithCollectionOps() {
            var enumAttr = new EnumAttribute<TestEntity, TestStatus>("status", TestEntity.class, TestStatus.class);
            var builder = new TypeSafeFieldSpecificationBuilder.EnumFieldBuilder<>(
                    enumAttr, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(enumAttr), null);
            Specification<TestEntity> spec = builder.isNull();
            assertThat(spec).isNotNull();
        }
    }

    // ==================== Constructor variants ====================

    @Nested
    @DisplayName("Constructor Variant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("StringFieldBuilder with field path")
        void stringWithPath() {
            var builder = new TypeSafeFieldSpecificationBuilder.StringFieldBuilder<>(
                    TestEntity_.name, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.name));
            assertThat(builder.equalTo("test")).isNotNull();
        }

        @Test
        @DisplayName("StringFieldBuilder with collection ops")
        void stringWithCollOps() {
            var builder = new TypeSafeFieldSpecificationBuilder.StringFieldBuilder<>(
                    TestEntity_.name, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.name), null);
            assertThat(builder.contains("test")).isNotNull();
        }

        @Test
        @DisplayName("NumericFieldBuilder with field path")
        void numericWithPath() {
            var builder = new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(
                    TestEntity_.age, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.age));
            assertThat(builder.greaterThan(10)).isNotNull();
        }

        @Test
        @DisplayName("NumericFieldBuilder with collection ops")
        void numericWithCollOps() {
            var builder = new TypeSafeFieldSpecificationBuilder.NumericFieldBuilder<>(
                    TestEntity_.age, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.age), null);
            assertThat(builder.lessThan(50)).isNotNull();
        }

        @Test
        @DisplayName("TemporalFieldBuilder with field path")
        void temporalWithPath() {
            var builder = new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
                    TestEntity_.birthDate, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.birthDate));
            assertThat(builder.isBefore(LocalDate.now())).isNotNull();
        }

        @Test
        @DisplayName("TemporalFieldBuilder with collection ops")
        void temporalWithCollOps() {
            var builder = new TypeSafeFieldSpecificationBuilder.TemporalFieldBuilder<>(
                    TestEntity_.birthDate, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.birthDate), null);
            assertThat(builder.isAfter(LocalDate.of(2000, 1, 1))).isNotNull();
        }

        @Test
        @DisplayName("BooleanFieldBuilder with field path")
        void booleanWithPath() {
            var builder = new TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<>(
                    TestEntity_.available, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.available));
            assertThat(builder.isTrue()).isNotNull();
        }

        @Test
        @DisplayName("BooleanFieldBuilder with collection ops")
        void booleanWithCollOps() {
            var builder = new TypeSafeFieldSpecificationBuilder.BooleanFieldBuilder<>(
                    TestEntity_.available, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.available), null);
            assertThat(builder.isFalse()).isNotNull();
        }

        @Test
        @DisplayName("CollectionFieldBuilder with field path")
        void collectionWithPath() {
            var builder = new TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<>(
                    TestEntity_.tags, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.tags));
            assertThat(builder.contains("x")).isNotNull();
        }

        @Test
        @DisplayName("CollectionFieldBuilder with collection ops")
        void collectionWithCollOps() {
            var builder = new TypeSafeFieldSpecificationBuilder.CollectionFieldBuilder<>(
                    TestEntity_.tags, TestEntitySpecificationService.INSTANCE, spec -> spec, List.of(TestEntity_.tags), null);
            assertThat(builder.isNull()).isNotNull();
        }
    }
}
