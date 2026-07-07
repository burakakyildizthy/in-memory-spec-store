package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for BaseSpecificationService collection operations.
 */
@DisplayName("BaseSpecificationService Tests")
class BaseSpecificationServiceTest {

    private static final String VALUE_A = "A";
    private static final String VALUE_B = "B";
    private static final String VALUE_C = "C";

    private static class TestEntity {
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private static class TestSpecificationService extends BaseSpecificationService<TestEntity> {
        @Override
        public boolean validateSpecification(TestEntity entity, MetaAttribute<TestEntity, ?> attribute, Operator operator, Object value) {
            return false;
        }

        @Override
        public boolean validateFilter(TestEntity entity, Object filter) {
            return false;
        }

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public TestEntity createInstance() {
            return new TestEntity(null, null);
        }

        @Override
        public Object getFieldValue(TestEntity entity, String fieldName) {
            return null;
        }

        @Override
        public Object getFieldValue(TestEntity entity, MetaAttribute<?, ?> metaAttribute) {
            return null;
        }

        @Override
        public void setFieldValue(TestEntity entity, MetaAttribute<?, ?> metaAttribute, Object value) {
            // Noncompliant - method is empty
        }  

        @Override
        public java.util.Comparator<TestEntity> createComparator(String fieldName, boolean ascending) {
            return null;
        }

        @Override
        public java.util.Comparator<TestEntity> createComparator(MetaAttribute<?, ?> metaAttribute, boolean ascending) {
            return null;
        }

        @Override
        public java.util.Comparator<TestEntity> createMultiFieldComparator(List<String> fieldNames, List<Boolean> ascendingFlags) {
            return null;
        }

        @Override
        public java.util.Comparator<TestEntity> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> metaAttributes, List<Boolean> ascendingFlags) {
            return null;
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

    @Test
    @DisplayName("extractFromCollection with ALL selector returns all elements")
    void testExtractFromCollectionAll() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        Object result = service.extractFromCollection(collection, CollectionSelector.ALL);
        
        assertThat(result).isInstanceOf(Collection.class);
        @SuppressWarnings("unchecked")
        Collection<String> resultCollection = (Collection<String>) result;
        assertThat(resultCollection).containsExactly(VALUE_A, VALUE_B, VALUE_C);
    }

    @Test
    @DisplayName("extractFromCollection with FIRST selector returns first element")
    void testExtractFromCollectionFirst() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        Object result = service.extractFromCollection(collection, CollectionSelector.FIRST);
        
        assertThat(result).isEqualTo(VALUE_A);
    }

    @Test
    @DisplayName("extractFromCollection with LAST selector returns last element")
    void testExtractFromCollectionLast() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        Object result = service.extractFromCollection(collection, CollectionSelector.LAST);
        
        assertThat(result).isEqualTo(VALUE_C);
    }

    @Test
    @DisplayName("extractFromCollection with ANY selector returns first element")
    void testExtractFromCollectionAny() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        Object result = service.extractFromCollection(collection, CollectionSelector.ANY);
        
        assertThat(result).isEqualTo(VALUE_A);
    }

    @Test
    @DisplayName("extractFromCollection with null collection returns empty list for ALL")
    void testExtractFromCollectionNullAll() {
        Object result = service.extractFromCollection(null, CollectionSelector.ALL);
        
        assertThat(result).isInstanceOf(Collection.class);
        assertThat((Collection<?>) result).isEmpty();
    }

    @Test
    @DisplayName("extractFromCollection with null collection returns null for FIRST")
    void testExtractFromCollectionNullFirst() {
        Object result = service.extractFromCollection(null, CollectionSelector.FIRST);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("extractFromCollection with empty collection returns empty list for ALL")
    void testExtractFromCollectionEmptyAll() {
        Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.ALL);
        
        assertThat(result).isInstanceOf(Collection.class);
        assertThat((Collection<?>) result).isEmpty();
    }

    @Test
    @DisplayName("extractFromCollection with empty collection returns null for FIRST")
    void testExtractFromCollectionEmptyFirst() {
        Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.FIRST);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getCollectionSize returns correct size")
    void testGetCollectionSize() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        int size = service.getCollectionSize(collection);
        
        assertThat(size).isEqualTo(3);
    }

    @Test
    @DisplayName("getCollectionSize with null collection returns 0")
    void testGetCollectionSizeNull() {
        int size = service.getCollectionSize(null);
        
        assertThat(size).isZero();
    }

    @Test
    @DisplayName("getCollectionSize with empty collection returns 0")
    void testGetCollectionSizeEmpty() {
        int size = service.getCollectionSize(new ArrayList<>());
        
        assertThat(size).isZero();
    }

    @Test
    @DisplayName("isCollectionEmpty returns true for null collection")
    void testIsCollectionEmptyNull() {
        boolean isEmpty = service.isCollectionEmpty(null);
        
        assertThat(isEmpty).isTrue();
    }

    @Test
    @DisplayName("isCollectionEmpty returns true for empty collection")
    void testIsCollectionEmptyEmpty() {
        boolean isEmpty = service.isCollectionEmpty(new ArrayList<>());
        
        assertThat(isEmpty).isTrue();
    }

    @Test
    @DisplayName("isCollectionEmpty returns false for non-empty collection")
    void testIsCollectionEmptyNonEmpty() {
        List<String> collection = Arrays.asList(VALUE_A, VALUE_B, VALUE_C);
        
        boolean isEmpty = service.isCollectionEmpty(collection);
        
        assertThat(isEmpty).isFalse();
    }
}
