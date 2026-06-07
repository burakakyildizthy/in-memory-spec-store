package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.factory.navigation.*;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestEntity;
import com.thy.fss.common.inmemory.testmodel.TestEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestEntity_;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for SpecificationBuilder.
 * Tests all methods to achieve 80%+ coverage including type-safe field builders.
 */
@DisplayName("SpecificationBuilder Tests")
class SpecificationBuilderTest {

    private SpecificationBuilder<TestEntity> builder;

    // Constants for duplicate string literals
    private static final String SPECIFICATION_SERVICE_CANNOT_BE_NULL = "Specification service cannot be null";
    private static final String JOHN_NAME = "John";
    private static final String TEST_VALUE = "test";
    private static final String TAG1_VALUE = "tag1";
    private static final String TEST_DOUBLE_FIELD = "testDouble";
    private static final String TEST_DATE_TIME_FIELD = "testDateTime";
    private static final String TEST_INSTANT_FIELD = "testInstant";
    private static final String STATUS_FIELD = "status";

    @BeforeEach
    void setUp() {
        builder = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE);
    }

    @Nested
    @DisplayName("Constructor and Factory Tests")
    class ConstructorAndFactoryTests {

        @Test
        @DisplayName("Should create builder using forService factory method")
        void shouldCreateBuilderUsingForServiceFactory() {
            SpecificationBuilder<TestEntity> newBuilder = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE);

            assertThat(newBuilder).isNotNull();
            assertThat(newBuilder.getTargetClass()).isEqualTo(TestEntity.class);
            assertThat(newBuilder.getSpecificationService()).isNotNull();
        }

        @Test
        @DisplayName("Should create builder using constructor")
        void shouldCreateBuilderUsingConstructor() {
            SpecificationBuilder<TestEntity> newBuilder = new SpecificationBuilder<>(TestEntitySpecificationService.INSTANCE);

            assertThat(newBuilder).isNotNull();
            assertThat(newBuilder.getTargetClass()).isEqualTo(TestEntity.class);
            assertThat(newBuilder.getSpecificationService()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception for null specification service in constructor")
        void shouldThrowExceptionForNullSpecificationServiceInConstructor() {
            assertThatThrownBy(() -> new SpecificationBuilder<TestEntity>(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SPECIFICATION_SERVICE_CANNOT_BE_NULL);
        }

        @Test
        @DisplayName("Should throw exception for null specification service in factory method")
        void shouldThrowExceptionForNullSpecificationServiceInFactory() {
            assertThatThrownBy(() -> SpecificationBuilder.forService(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SPECIFICATION_SERVICE_CANNOT_BE_NULL);
        }

        @Test
        @DisplayName("Should create collection element builder")
        void shouldCreateCollectionElementBuilder() {
            SpecificationBuilder<TestEntity> elementBuilder = SpecificationBuilder.forService(TestEntitySpecificationService.INSTANCE);

            assertThat(elementBuilder).isNotNull();
            assertThat(elementBuilder.getTargetClass()).isEqualTo(TestEntity.class);
        }
    }

    @Nested
    @DisplayName("Type-Safe Field Builder Tests")
    class TypeSafeFieldBuilderTests {

        @Test
        @DisplayName("Should create StringFieldBuilder for string attributes")
        void shouldCreateStringFieldBuilderForStringAttributes() {
            var stringBuilder = builder.where(TestEntity_.name);

            assertThat(stringBuilder).isNotNull();

            // Test string-specific operations
            Specification<TestEntity> spec = stringBuilder.equalTo(TEST_VALUE);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create NumericFieldBuilder for integer attributes")
        void shouldCreateNumericFieldBuilderForIntegerAttributes() {
            var integerBuilder = builder.where(TestEntity_.age);

            assertThat(integerBuilder).isNotNull();

            // Test numeric-specific operations
            Specification<TestEntity> spec = integerBuilder.equalTo(25);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create NumericFieldBuilder for long attributes")
        void shouldCreateNumericFieldBuilderForLongAttributes() {
            var longBuilder = builder.where(TestEntity_.id);

            assertThat(longBuilder).isNotNull();

            // Test numeric-specific operations
            Specification<TestEntity> spec = longBuilder.equalTo(1L);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create BooleanFieldBuilder for boolean attributes")
        void shouldCreateBooleanFieldBuilderForBooleanAttributes() {
            var booleanBuilder = builder.where(TestEntity_.available);

            assertThat(booleanBuilder).isNotNull();

            // Test boolean-specific operations
            Specification<TestEntity> spec = booleanBuilder.equalTo(true);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create CollectionFieldBuilder for collection attributes")
        void shouldCreateCollectionFieldBuilderForCollectionAttributes() {
            var collectionBuilder = builder.where(TestEntity_.tags);

            assertThat(collectionBuilder).isNotNull();

            // Test collection-specific operations
            Specification<TestEntity> spec = collectionBuilder.contains(TAG1_VALUE);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create TemporalFieldBuilder for LocalDate attributes")
        void shouldCreateTemporalFieldBuilderForLocalDateAttributes() {
            var dateBuilder = builder.where(TestEntity_.birthDate);

            assertThat(dateBuilder).isNotNull();

            // Test temporal-specific operations
            Specification<TestEntity> spec = dateBuilder.equalTo(LocalDate.now());
            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("Generic MetaAttribute Tests")
    class GenericMetaAttributeTests {

        @Test
        @DisplayName("Should create FieldSpecificationBuilder for generic meta attribute")
        void shouldCreateFieldSpecificationBuilderForGenericMetaAttribute() {
            var fieldBuilder = builder.where(TestEntity_.name);

            assertThat(fieldBuilder).isNotNull();

            // Test basic operations
            Specification<TestEntity> spec = fieldBuilder.equalTo(TEST_VALUE);
            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("Simple Specification Tests")
    class SimpleSpecificationTests {

        @Test
        @DisplayName("Should create simple string specification")
        void shouldCreateSimpleStringSpecification() {
            Specification<TestEntity> spec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName(JOHN_NAME);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create simple integer specification")
        void shouldCreateSimpleIntegerSpecification() {
            Specification<TestEntity> spec = builder.where(TestEntity_.age).equalTo(25);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setAge(25);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create simple boolean specification")
        void shouldCreateSimpleBooleanSpecification() {
            Specification<TestEntity> spec = builder.where(TestEntity_.available).equalTo(true);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setAvailable(true);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create simple collection specification")
        void shouldCreateSimpleCollectionSpecification() {
            Specification<TestEntity> spec = builder.where(TestEntity_.tags).contains(TAG1_VALUE);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setTags(Arrays.asList(TAG1_VALUE, "tag2"));

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create combined AND specification")
        void shouldCreateCombinedAndSpecification() {
            Specification<TestEntity> nameSpec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);
            Specification<TestEntity> ageSpec = builder.where(TestEntity_.age).equalTo(25);
            Specification<TestEntity> combinedSpec = nameSpec.and(ageSpec);

            assertThat(combinedSpec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName(JOHN_NAME);
            entity.setAge(25);

            Predicate<TestEntity> predicate = combinedSpec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create combined OR specification")
        void shouldCreateCombinedOrSpecification() {
            Specification<TestEntity> nameSpec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);
            Specification<TestEntity> ageSpec = builder.where(TestEntity_.age).equalTo(30);
            Specification<TestEntity> combinedSpec = nameSpec.or(ageSpec);

            assertThat(combinedSpec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName(JOHN_NAME);
            entity.setAge(25); // Age doesn't match, but name does

            Predicate<TestEntity> predicate = combinedSpec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }
    }

    @Nested
    @DisplayName("Getter Method Tests")
    class GetterMethodTests {

        @Test
        @DisplayName("Should return correct target class")
        void shouldReturnCorrectTargetClass() {
            assertThat(builder.getTargetClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("Should return specification service")
        void shouldReturnSpecificationService() {
            SpecificationService<TestEntity> service = builder.getSpecificationService();

            assertThat(service).isNotNull().isInstanceOf(SpecificationService.class);
        }
    }

    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalCoverageTests {

        @Test
        @DisplayName("Should create DoubleFieldBuilder for double attributes")
        void shouldCreateDoubleFieldBuilderForDoubleAttributes() {
            // Create a mock double attribute since TestEntity doesn't have one
            var doubleAttribute = new com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<>(TEST_DOUBLE_FIELD, TestEntity.class);
            var doubleBuilder = builder.where(doubleAttribute);

            assertThat(doubleBuilder).isNotNull();
        }

        @Test
        @DisplayName("Should create TemporalFieldBuilder for LocalDateTime attributes")
        void shouldCreateTemporalFieldBuilderForLocalDateTimeAttributes() {
            // Create a mock LocalDateTime attribute since TestEntity doesn't have one
            var localDateTimeAttribute = new com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute<>(TEST_DATE_TIME_FIELD, TestEntity.class);
            var dateTimeBuilder = builder.where(localDateTimeAttribute);

            assertThat(dateTimeBuilder).isNotNull();
        }

        @Test
        @DisplayName("Should create TemporalFieldBuilder for Instant attributes")
        void shouldCreateTemporalFieldBuilderForInstantAttributes() {
            // Create a mock Instant attribute since TestEntity doesn't have one
            var instantAttribute = new com.thy.fss.common.inmemory.specification.attribute.InstantAttribute<>(TEST_INSTANT_FIELD, TestEntity.class);
            var instantBuilder = builder.where(instantAttribute);

            assertThat(instantBuilder).isNotNull();
        }

        @Test
        @DisplayName("Should create type safe field builders for various attribute types")
        void shouldCreateTypeSafeFieldBuildersForVariousAttributeTypes() {
            // Create mock attributes for testing different types
            var doubleAttribute = new com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<>(TEST_DOUBLE_FIELD, TestEntity.class);
            var instantAttribute = new com.thy.fss.common.inmemory.specification.attribute.InstantAttribute<>(TEST_INSTANT_FIELD, TestEntity.class);
            var localDateTimeAttribute = new com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute<>(TEST_DATE_TIME_FIELD, TestEntity.class);

            enum TestStatus {ACTIVE, INACTIVE}
            var enumAttribute = new com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<TestEntity, TestStatus>(STATUS_FIELD, TestEntity.class, TestStatus.class);

            // Test that all builders can be created
            var doubleBuilder = builder.where(doubleAttribute);
            var instantBuilder = builder.where(instantAttribute);
            var dateTimeBuilder = builder.where(localDateTimeAttribute);
            var enumBuilder = builder.where(enumAttribute);

            assertThat(doubleBuilder).isNotNull();
            assertThat(instantBuilder).isNotNull();
            assertThat(dateTimeBuilder).isNotNull();
            assertThat(enumBuilder).isNotNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should create and execute simple specification")
        void shouldCreateAndExecuteSimpleSpecification() {
            TestEntity entity = new TestEntity();
            entity.setName(JOHN_NAME);
            entity.setAge(25);
            entity.setAvailable(true);

            Specification<TestEntity> spec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);

            assertThat(spec).isNotNull();

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should work with different field types")
        void shouldWorkWithDifferentFieldTypes() {
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setName(JOHN_NAME);
            entity.setAge(25);
            entity.setAvailable(true);
            entity.setBirthDate(LocalDate.of(1998, 1, 1));
            entity.setTags(Arrays.asList(TAG1_VALUE, "tag2"));

            // Test different field types
            Specification<TestEntity> longSpec = builder.where(TestEntity_.id).equalTo(1L);
            Specification<TestEntity> stringSpec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);
            Specification<TestEntity> intSpec = builder.where(TestEntity_.age).equalTo(25);
            Specification<TestEntity> boolSpec = builder.where(TestEntity_.available).equalTo(true);
            Specification<TestEntity> dateSpec = builder.where(TestEntity_.birthDate).equalTo(LocalDate.of(1998, 1, 1));
            Specification<TestEntity> collectionSpec = builder.where(TestEntity_.tags).contains(TAG1_VALUE);

            // Test each specification individually
            Predicate<TestEntity> longPredicate = longSpec.toPredicate();
            assertThat((Object) longPredicate).isNotNull();
            assertThat(longPredicate.test(entity)).isTrue();

            Predicate<TestEntity> stringPredicate = stringSpec.toPredicate();
            assertThat((Object) stringPredicate).isNotNull();
            assertThat(stringPredicate.test(entity)).isTrue();

            Predicate<TestEntity> intPredicate = intSpec.toPredicate();
            assertThat((Object) intPredicate).isNotNull();
            assertThat(intPredicate.test(entity)).isTrue();

            Predicate<TestEntity> boolPredicate = boolSpec.toPredicate();
            assertThat((Object) boolPredicate).isNotNull();
            assertThat(boolPredicate.test(entity)).isTrue();

            Predicate<TestEntity> datePredicate = dateSpec.toPredicate();
            assertThat((Object) datePredicate).isNotNull();
            assertThat(datePredicate.test(entity)).isTrue();

            Predicate<TestEntity> collectionPredicate = collectionSpec.toPredicate();
            assertThat((Object) collectionPredicate).isNotNull();
            assertThat(collectionPredicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create complex combined specifications")
        void shouldCreateComplexCombinedSpecifications() {
            TestEntity entity = new TestEntity();
            entity.setName(JOHN_NAME);
            entity.setAge(25);
            entity.setAvailable(true);

            // Create multiple specifications
            Specification<TestEntity> nameSpec = builder.where(TestEntity_.name).equalTo(JOHN_NAME);
            Specification<TestEntity> ageSpec = builder.where(TestEntity_.age).equalTo(25);
            Specification<TestEntity> availableSpec = builder.where(TestEntity_.available).equalTo(true);

            // Combine them in different ways
            Specification<TestEntity> andSpec = nameSpec.and(ageSpec);
            Specification<TestEntity> orSpec = nameSpec.or(availableSpec);
            Specification<TestEntity> complexSpec = andSpec.and(availableSpec);

            assertThat(andSpec).isNotNull();
            assertThat(orSpec).isNotNull();
            assertThat(complexSpec).isNotNull();

            // Test the combined specifications
            Predicate<TestEntity> andPredicate = andSpec.toPredicate();
            assertThat((Object) andPredicate).isNotNull();
            assertThat(andPredicate.test(entity)).isTrue();

            Predicate<TestEntity> orPredicate = orSpec.toPredicate();
            assertThat((Object) orPredicate).isNotNull();
            assertThat(orPredicate.test(entity)).isTrue();

            Predicate<TestEntity> complexPredicate = complexSpec.toPredicate();
            assertThat((Object) complexPredicate).isNotNull();
            assertThat(complexPredicate.test(entity)).isTrue();
        }
    }

    @Nested
    @DisplayName("Navigation Builder Tests")
    class NavigationBuilderTests {

        private static final String EXPECTED_NAME = "expectedName";
        private static final String EXPECTED_TAG = "expectedTag";
        private static final int EXPECTED_AGE = 30;

        @Test
        @DisplayName("Should create specification using StringNavigationBuilder")
        void shouldCreateSpecificationUsingStringNavigationBuilder() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            StringNavigationBuilder<TestEntity> stringNav = nav.field(TestEntity_.name);

            Specification<TestEntity> spec = builder.on(stringNav).equalTo(EXPECTED_NAME);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setName(EXPECTED_NAME);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create specification using NumericNavigationBuilder")
        void shouldCreateSpecificationUsingNumericNavigationBuilder() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            NumericNavigationBuilder<TestEntity, Integer> numericNav = nav.field(TestEntity_.age);

            Specification<TestEntity> spec = (Specification<TestEntity>) builder.on(numericNav).greaterThan(EXPECTED_AGE);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setAge(35);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create specification using BooleanNavigationBuilder")
        void shouldCreateSpecificationUsingBooleanNavigationBuilder() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            BooleanNavigationBuilder<TestEntity> booleanNav = nav.field(TestEntity_.available);

            Specification<TestEntity> spec = builder.on(booleanNav).isTrue();

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setAvailable(true);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create specification using CollectionNavigationBuilder")
        void shouldCreateSpecificationUsingCollectionNavigationBuilder() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            CollectionNavigationBuilder<TestEntity, String> collectionNav = nav.field(TestEntity_.tags);

            Specification<TestEntity> spec = builder.on(collectionNav).contains(EXPECTED_TAG);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setTags(Arrays.asList(EXPECTED_TAG, "tag2"));

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create specification using TemporalNavigationBuilder for LocalDate")
        void shouldCreateSpecificationUsingTemporalNavigationBuilderForLocalDate() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            TemporalNavigationBuilder<TestEntity, LocalDate> temporalNav = nav.field(TestEntity_.birthDate);

            LocalDate testDate = LocalDate.of(2000, 1, 1);
            Specification<TestEntity> spec = builder.on(temporalNav).isAfter(testDate);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setBirthDate(LocalDate.of(2001, 1, 1));

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        @DisplayName("Should create specification using EnumNavigationBuilder")
        void shouldCreateSpecificationUsingEnumNavigationBuilder() {
            enum TestStatus { ACTIVE, INACTIVE }

            var enumAttribute = new com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<TestEntity, TestStatus>(
                    STATUS_FIELD, TestEntity.class, TestStatus.class);

            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            EnumNavigationBuilder<TestEntity, TestStatus> enumNav = nav.field(enumAttribute);

            Specification<TestEntity> spec = builder.on(enumNav).equalTo(TestStatus.ACTIVE);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create specification using TemporalNavigationBuilder for LocalDateTime")
        void shouldCreateSpecificationUsingTemporalNavigationBuilderForLocalDateTime() {
            var localDateTimeAttribute = new com.thy.fss.common.inmemory.specification.attribute.LocalDateTimeAttribute<TestEntity>(
                    "createdAt", TestEntity.class);

            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            TemporalNavigationBuilder<TestEntity, java.time.LocalDateTime> temporalNav = nav.field(localDateTimeAttribute);

            java.time.LocalDateTime testDateTime = java.time.LocalDateTime.of(2024, 1, 1, 0, 0);
            Specification<TestEntity> spec = builder.on(temporalNav).isBefore(testDateTime);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create specification using TemporalNavigationBuilder for Instant")
        void shouldCreateSpecificationUsingTemporalNavigationBuilderForInstant() {
            var instantAttribute = new com.thy.fss.common.inmemory.specification.attribute.InstantAttribute<TestEntity>(
                    "timestamp", TestEntity.class);

            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            TemporalNavigationBuilder<TestEntity, java.time.Instant> temporalNav = nav.field(instantAttribute);

            java.time.Instant testInstant = java.time.Instant.now();
            Specification<TestEntity> spec = builder.on(temporalNav).isOnOrBefore(testInstant);

            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle DoubleAttribute navigation")
        void shouldHandleDoubleAttributeNavigation() {
            var doubleAttribute = new com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<TestEntity>(
                    "price", TestEntity.class);

            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            NumericNavigationBuilder<TestEntity, Double> numericNav = nav.field(doubleAttribute);

            Specification<TestEntity> spec = (Specification<TestEntity>) builder.on(numericNav).lessThan(100.0);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should handle LongAttribute navigation")
        void shouldHandleLongAttributeNavigation() {
            PropertyNavigationBuilder nav = new PropertyNavigationBuilder(TestEntity.class);
            NumericNavigationBuilder<TestEntity, Long> numericNav = nav.field(TestEntity_.id);

            Specification<TestEntity> spec = (Specification<TestEntity>) builder.on(numericNav).equalTo(1L);

            assertThat(spec).isNotNull();

            TestEntity entity = new TestEntity();
            entity.setId(1L);

            Predicate<TestEntity> predicate = spec.toPredicate();
            assertThat((Object) predicate).isNotNull();
            assertThat(predicate.test(entity)).isTrue();
        }
    }

}