package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserSpecificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for element type service lookup in BaseSpecificationService.
 * Tests Requirements 9.1, 9.2, 9.3, 9.4.
 */
@DisplayName("Element Type Service Lookup Tests")
class ElementTypeServiceLookupTest {

    /**
     * Test specification service that extends BaseSpecificationService.
     * Used to test the protected getElementTypeService method.
     */
    private static class TestSpecificationService extends BaseSpecificationService<User> {
        @Override
        public boolean validateSpecification(User entity, MetaAttribute<User, ?> attribute, Operator operator, Object value) {
            return false;
        }

        @Override
        public boolean validateFilter(User entity, Object filter) {
            return false;
        }

        @Override
        public Class<User> getEntityClass() {
            return User.class;
        }

        @Override
        public User createInstance() {
            return new User();
        }

        @Override
        public Object getFieldValue(User entity, String fieldName) {
            return null;
        }

        @Override
        public Object getFieldValue(User entity, MetaAttribute<?, ?> metaAttribute) {
            return null;
        }

        @Override
        public void setFieldValue(User entity, MetaAttribute<?, ?> metaAttribute, Object value) {
            // Empty implementation for testing
        }

        @Override
        public java.util.Comparator<User> createComparator(String fieldName, boolean ascending) {
            return null;
        }

        @Override
        public java.util.Comparator<User> createComparator(MetaAttribute<?, ?> metaAttribute, boolean ascending) {
            return null;
        }

        @Override
        public java.util.Comparator<User> createMultiFieldComparator(List<String> fieldNames, List<Boolean> ascendingFlags) {
            return null;
        }

        @Override
        public java.util.Comparator<User> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> metaAttributes, List<Boolean> ascendingFlags) {
            return null;
        }

        // Expose protected method for testing
        public <E> SpecificationService<E> testGetElementTypeService(Class<E> elementTypeClass) {
            return getElementTypeService(elementTypeClass);
        }

        // Expose protected method for testing
        public <E> boolean testValidateCollectionElement(E element, Object elementFilter, Class<E> elementTypeClass) {
            return validateCollectionElement(element, elementFilter, elementTypeClass);
        }

        @Override
        protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex) {
            return null;
        }

        @Override
        protected void navigateNestedForSet(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex, Object value) {
            // No-op: not needed for testing purposes
        }

        @Override
        protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {
            return null;
        }
    }

    private final TestSpecificationService service = new TestSpecificationService();

    // ==================== Requirement 9.1: Get service for model types ====================

    @Test
    @DisplayName("getElementTypeService returns service for model type (User)")
    void testGetElementTypeServiceForModelType() {
        // User has @MetaModel annotation, so it should have a specification service
        SpecificationService<User> userService = service.testGetElementTypeService(User.class);

        assertThat(userService).isNotNull();
        assertThat(userService.getEntityClass()).isEqualTo(User.class);
    }

    // ==================== Requirement 9.2: Return null for basic types ====================

    @Test
    @DisplayName("getElementTypeService returns null for String (basic type)")
    void testGetElementTypeServiceForString() {
        SpecificationService<String> stringService = service.testGetElementTypeService(String.class);

        assertThat(stringService).isNull();
    }

    @Test
    @DisplayName("getElementTypeService returns null for Integer (basic type)")
    void testGetElementTypeServiceForInteger() {
        SpecificationService<Integer> integerService = service.testGetElementTypeService(Integer.class);

        assertThat(integerService).isNull();
    }

    @Test
    @DisplayName("getElementTypeService returns null for Long (basic type)")
    void testGetElementTypeServiceForLong() {
        SpecificationService<Long> longService = service.testGetElementTypeService(Long.class);

        assertThat(longService).isNull();
    }

    @Test
    @DisplayName("getElementTypeService returns null for Boolean (basic type)")
    void testGetElementTypeServiceForBoolean() {
        SpecificationService<Boolean> booleanService = service.testGetElementTypeService(Boolean.class);

        assertThat(booleanService).isNull();
    }

    @Test
    @DisplayName("getElementTypeService returns null for LocalDate (temporal type)")
    void testGetElementTypeServiceForLocalDate() {
        SpecificationService<LocalDate> localDateService = service.testGetElementTypeService(LocalDate.class);

        assertThat(localDateService).isNull();
    }

    @Test
    @DisplayName("getElementTypeService returns null for enum type")
    void testGetElementTypeServiceForEnum() {
        SpecificationService<TestEnum> enumService = service.testGetElementTypeService(TestEnum.class);

        assertThat(enumService).isNull();
    }

    private enum TestEnum {
        VALUE_A, VALUE_B
    }

    // ==================== Requirement 9.3: Return service for model types ====================

    @Test
    @DisplayName("getElementTypeService returns correct service instance for model type")
    void testGetElementTypeServiceReturnsCorrectInstance() {
        SpecificationService<User> userService = service.testGetElementTypeService(User.class);

        assertThat(userService).isNotNull();
        assertThat(userService).isInstanceOf(SpecificationService.class);
        
        // Verify it's the same instance as direct INSTANCE reference
        SpecificationService<User> directService = UserSpecificationService.INSTANCE;
        assertThat(userService).isSameAs(directService);
    }

    // ==================== Requirement 9.4: Throw exception for missing service ====================

    @Test
    @DisplayName("getElementTypeService throws IllegalStateException for model type without service")
    void testGetElementTypeServiceThrowsForMissingService() {
        // Create a class that looks like a model type but has no @MetaModel annotation
        class UnregisteredModel {
            private String name;
        }

        assertThatThrownBy(() -> service.testGetElementTypeService(UnregisteredModel.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No specification service found for element type")
                .hasMessageContaining("UnregisteredModel")
                .hasMessageContaining("@MetaModel annotation");
    }

    @Test
    @DisplayName("getElementTypeService handles null element type class")
    void testGetElementTypeServiceWithNull() {
        SpecificationService<?> nullService = service.testGetElementTypeService(null);

        assertThat(nullService).isNull();
    }

    // ==================== validateCollectionElement tests ====================

    @Test
    @DisplayName("validateCollectionElement returns false for null element")
    void testValidateCollectionElementWithNullElement() {
        User user = null;
        // With null element, validateCollectionElement delegates to the element service's
        // validateFilter which will throw if filter is not the correct type
        Object filter = new Object();

        assertThatThrownBy(() -> service.testValidateCollectionElement(user, filter, User.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateCollectionElement returns true for null filter")
    void testValidateCollectionElementWithNullFilter() {
        User user = new User();
        Object filter = null;

        boolean result = service.testValidateCollectionElement(user, filter, User.class);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateCollectionElement returns false for basic type")
    void testValidateCollectionElementWithBasicType() {
        String element = "test";
        Object filter = new Object();

        boolean result = service.testValidateCollectionElement(element, filter, String.class);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateCollectionElement throws IllegalStateException for model type without service")
    void testValidateCollectionElementThrowsForMissingService() {
        class UnregisteredModel {
            private String name;
        }

        UnregisteredModel element = new UnregisteredModel();
        Object filter = new Object();

        assertThatThrownBy(() -> service.testValidateCollectionElement(element, filter, UnregisteredModel.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No specification service found for element type")
                .hasMessageContaining("UnregisteredModel");
    }
}
