package com.thy.fss.common.inmemory.engine.production;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.MappingApplicator;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Store Aggregation functionality in DataSynchronizationEngine.
 * <p>
 * Tests cover:
 * - Multiple aggregations in single loop
 * - Field-based grouping
 * - Specification-based filtering
 * - calculateMultipleNumericAggregations
 */
@DisplayName("Store Aggregation Tests")
class StoreAggregationTest {

    private static final String ACTIVE = "active";
    private static final String VALUE = "value";
    private static final String CALC_MULTIPLE_NUM_AGG  = "calculateMultipleNumericAggregations";
    private static final String NOT_IMPLEMENTED_FOR_TEST_MOCK = "Not implemented for test mock";
    
    // Mock SpecificationService for TestEntity
    private static final SpecificationService<TestEntity> TEST_ENTITY_SERVICE = new SpecificationService<TestEntity>() {
        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public TestEntity createInstance() {
            return new TestEntity();
        }

        @Override
        public Object getFieldValue(TestEntity entity, String fieldName) {
            if ("value".equals(fieldName)) {
                return entity.getValue();
            }
            return null;
        }

        @Override
        public Object getFieldValue(TestEntity entity, MetaAttribute<?, ?> attribute) {
            return getFieldValue(entity, attribute.getName());
        }

        @Override
        public void setFieldValue(TestEntity entity, MetaAttribute<?, ?> attribute, Object value) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public boolean validateSpecification(TestEntity entity, MetaAttribute<TestEntity, ?> attribute, Operator operator, Object value) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public boolean validateFilter(TestEntity entity, Object filter) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Comparator<TestEntity> createComparator(String fieldName, boolean ascending) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Comparator<TestEntity> createComparator(MetaAttribute<?, ?> attribute, boolean ascending) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Comparator<TestEntity> createMultiFieldComparator(List<String> fieldNames, List<Boolean> ascending) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Comparator<TestEntity> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> attributes, List<Boolean> ascending) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Object extractFromCollection(Collection<?> collection, com.thy.fss.common.inmemory.engine.mapping.CollectionSelector selector) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Object extractFromCollection(Collection<TestEntity> collection, com.thy.fss.common.inmemory.engine.mapping.CollectionSelector selector, Specification<TestEntity> specification) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public int getCollectionSize(Collection<?> collection) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public boolean isCollectionEmpty(Collection<?> collection) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Object getValueByPath(TestEntity entity, List<MetaAttribute<?, ?>> path) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public void setValueByPath(TestEntity entity, List<MetaAttribute<?, ?>> path, Object value) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public Object getValueByPathWithCollections(TestEntity entity, List<MetaAttribute<?, ?>> path, List<CollectionOperationMetadata<?, ?>> collectionOps) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }

        @Override
        public void setValueByPathWithCollections(TestEntity entity, List<MetaAttribute<?, ?>> path, List<CollectionOperationMetadata<?, ?>> collectionOps, Object value) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_FOR_TEST_MOCK);
        }
    };
    
    private DataSynchronizationEngine engine;

    @BeforeEach
    void setUp() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        engine = new DataSynchronizationEngine(factory);
    }

    @Test
    @DisplayName("Should calculate multiple numeric aggregations in single loop")
    void testCalculateMultipleNumericAggregations() throws Exception {
        // Given: List of entities with numeric values
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 100.0, ACTIVE),
                new TestEntity(2L, 200.0, ACTIVE),
                new TestEntity(3L, 150.0, ACTIVE),
                new TestEntity(4L, 300.0, ACTIVE)
        );

        // Create a simple MetaAttribute for the value field
        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        // Needed aggregation types
        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG,
                AggregationType.MIN,
                AggregationType.MAX
        ));

        // When: Call calculateMultipleNumericAggregations via reflection
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: All aggregations should be calculated correctly
        assertNotNull(results);
        assertEquals(750.0, (Double) results.get(AggregationType.SUM), 0.01, "SUM should be 750");
        assertEquals(187.5, (Double) results.get(AggregationType.AVG), 0.01, "AVG should be 187.5");
        assertEquals(100.0, (Double) results.get(AggregationType.MIN), 0.01, "MIN should be 100");
        assertEquals(300.0, (Double) results.get(AggregationType.MAX), 0.01, "MAX should be 300");
    }

    @Test
    @DisplayName("Should handle empty list in numeric aggregations")
    void testCalculateMultipleNumericAggregationsWithEmptyList() throws Exception {
        // Given: Empty list
        List<TestEntity> entities = new ArrayList<>();

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG,
                AggregationType.MIN,
                AggregationType.MAX
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: Should return appropriate zero/null values
        assertNotNull(results);
        assertEquals(0, results.get(AggregationType.SUM), "SUM of empty should be 0");
        assertNull(results.get(AggregationType.AVG), "AVG of empty should be null");
        assertNull(results.get(AggregationType.MIN), "MIN of empty should be null");
        assertNull(results.get(AggregationType.MAX), "MAX of empty should be null");
    }

    @Test
    @DisplayName("Should handle null values in numeric aggregations")
    void testCalculateMultipleNumericAggregationsWithNullValues() throws Exception {
        // Given: List with null values
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 100.0, ACTIVE),
                new TestEntity(2L, null, ACTIVE),
                new TestEntity(3L, 200.0, ACTIVE),
                new TestEntity(4L, null, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: Null values should be skipped
        assertNotNull(results);
        assertEquals(300.0, (Double) results.get(AggregationType.SUM), 0.01, "SUM should skip nulls");
        assertEquals(150.0, (Double) results.get(AggregationType.AVG), 0.01, "AVG should skip nulls");
    }

    @Test
    @DisplayName("Should calculate only requested aggregation types")
    void testCalculateOnlyRequestedAggregationTypes() throws Exception {
        // Given: List of entities
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 100.0, ACTIVE),
                new TestEntity(2L, 200.0, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        // Only request SUM and MIN
        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.MIN
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: Only requested types should be in results
        assertNotNull(results);
        assertTrue(results.containsKey(AggregationType.SUM), "Should contain SUM");
        assertTrue(results.containsKey(AggregationType.MIN), "Should contain MIN");
        assertFalse(results.containsKey(AggregationType.AVG), "Should not contain AVG");
        assertFalse(results.containsKey(AggregationType.MAX), "Should not contain MAX");

        assertEquals(300.0, (Double) results.get(AggregationType.SUM), 0.01);
        assertEquals(100.0, (Double) results.get(AggregationType.MIN), 0.01);
    }

    @Test
    @DisplayName("Should handle single value correctly")
    void testCalculateMultipleNumericAggregationsWithSingleValue() throws Exception {
        // Given: Single entity
        List<TestEntity> entities = List.of(
                new TestEntity(1L, 100.0, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG,
                AggregationType.MIN,
                AggregationType.MAX
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: All aggregations should equal the single value
        assertNotNull(results);
        assertEquals(100.0, (Double) results.get(AggregationType.SUM), 0.01);
        assertEquals(100.0, (Double) results.get(AggregationType.AVG), 0.01);
        assertEquals(100.0, (Double) results.get(AggregationType.MIN), 0.01);
        assertEquals(100.0, (Double) results.get(AggregationType.MAX), 0.01);
    }

    @Test
    @DisplayName("Should handle large numbers correctly")
    void testCalculateMultipleNumericAggregationsWithLargeNumbers() throws Exception {
        // Given: Entities with large values
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 1000000.0, ACTIVE),
                new TestEntity(2L, 2000000.0, ACTIVE),
                new TestEntity(3L, 3000000.0, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: Large numbers should be handled correctly
        assertNotNull(results);
        assertEquals(6000000.0, (Double) results.get(AggregationType.SUM), 0.01);
        assertEquals(2000000.0, (Double) results.get(AggregationType.AVG), 0.01);
    }

    @Test
    @DisplayName("Should calculate MIN and MAX correctly with mixed values")
    void testCalculateMinMaxWithMixedValues() throws Exception {
        // Given: Entities with mixed positive and negative values
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, -50.0, ACTIVE),
                new TestEntity(2L, 100.0, ACTIVE),
                new TestEntity(3L, -100.0, ACTIVE),
                new TestEntity(4L, 200.0, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.MIN,
                AggregationType.MAX
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: MIN and MAX should be correct
        assertNotNull(results);
        assertEquals(-100.0, (Double) results.get(AggregationType.MIN), 0.01, "MIN should be -100");
        assertEquals(200.0, (Double) results.get(AggregationType.MAX), 0.01, "MAX should be 200");
    }

    @Test
    @DisplayName("Should verify single loop optimization by testing all types together")
    void testSingleLoopOptimization() throws Exception {
        // Given: List of entities
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 10.0, ACTIVE),
                new TestEntity(2L, 20.0, ACTIVE),
                new TestEntity(3L, 30.0, ACTIVE),
                new TestEntity(4L, 40.0, ACTIVE),
                new TestEntity(5L, 50.0, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        // Request ALL aggregation types (should be calculated in single loop)
        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG,
                AggregationType.MIN,
                AggregationType.MAX
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        long startTime = System.nanoTime();
        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );
        long endTime = System.nanoTime();

        // Then: All aggregations should be correct
        assertNotNull(results);
        assertEquals(150.0, (Double) results.get(AggregationType.SUM), 0.01, "SUM: 10+20+30+40+50");
        assertEquals(30.0, (Double) results.get(AggregationType.AVG), 0.01, "AVG: 150/5");
        assertEquals(10.0, (Double) results.get(AggregationType.MIN), 0.01, "MIN: 10");
        assertEquals(50.0, (Double) results.get(AggregationType.MAX), 0.01, "MAX: 50");

        // Verify all 4 types were calculated
        assertEquals(4, results.size(), "Should have 4 aggregation results");

        // Performance should be reasonable (single loop)
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "Should complete quickly in single loop (took " + durationMs + "ms)");
    }

    @Test
    @DisplayName("Should handle decimal precision correctly")
    void testDecimalPrecision() throws Exception {
        // Given: Entities with decimal values
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, 10.5, ACTIVE),
                new TestEntity(2L, 20.7, ACTIVE),
                new TestEntity(3L, 30.3, ACTIVE)
        );

        MetaAttribute<TestEntity, Double> valueAttr = new MetaAttribute<TestEntity, Double>(VALUE, TestEntity.class, Double.class, null) {
        };

        Set<AggregationType> neededTypes = new HashSet<>(Arrays.asList(
                AggregationType.SUM,
                AggregationType.AVG
        ));

        // When: Call calculateMultipleNumericAggregations
        Method method = MappingApplicator.class.getDeclaredMethod(
                CALC_MULTIPLE_NUM_AGG,
                List.class,
                MetaAttribute.class,
                Set.class,
                com.thy.fss.common.inmemory.specification.SpecificationService.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<AggregationType, Object> results = (Map<AggregationType, Object>) method.invoke(
                engine,
                entities,
                valueAttr,
                neededTypes,
                TEST_ENTITY_SERVICE
        );

        // Then: Decimal precision should be maintained
        assertNotNull(results);
        assertEquals(61.5, (Double) results.get(AggregationType.SUM), 0.01);
        assertEquals(20.5, (Double) results.get(AggregationType.AVG), 0.01);
    }
}
