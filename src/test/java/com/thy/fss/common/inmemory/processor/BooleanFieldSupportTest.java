package com.thy.fss.common.inmemory.processor;

import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestEntity;
import com.thy.fss.common.inmemory.testmodel.TestEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestEntity_;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify boolean field support in the library.
 */
class BooleanFieldSupportTest {

    @Test
    @DisplayName("Should support primitive boolean field filtering")
    void shouldSupportPrimitiveBooleanFieldFiltering() {
        // Given
        TestEntity entity1 = new TestEntity();
        entity1.setAvailable(true);

        TestEntity entity2 = new TestEntity();
        entity2.setAvailable(false);

        SpecificationService<TestEntity> service = TestEntitySpecificationService.INSTANCE;
        assertNotNull(service, "SpecificationService should be available for TestEntity");

        // When & Then - Test EQUALS operator
        assertTrue(service.validateSpecification(entity1, TestEntity_.available, Operator.EQUALS, true));
        assertFalse(service.validateSpecification(entity1, TestEntity_.available, Operator.EQUALS, false));

        assertTrue(service.validateSpecification(entity2, TestEntity_.available, Operator.EQUALS, false));
        assertFalse(service.validateSpecification(entity2, TestEntity_.available, Operator.EQUALS, true));

        // Test NOT_EQUALS operator
        assertFalse(service.validateSpecification(entity1, TestEntity_.available, Operator.NOT_EQUALS, true));
        assertTrue(service.validateSpecification(entity1, TestEntity_.available, Operator.NOT_EQUALS, false));

        // Test IS_NULL operator (should always return false for primitive boolean)
        assertFalse(service.validateSpecification(entity1, TestEntity_.available, Operator.IS_NULL, true));
        assertFalse(service.validateSpecification(entity2, TestEntity_.available, Operator.IS_NULL, true));

        // Test IS_NOT_NULL operator (should always return true for primitive boolean)
        assertTrue(service.validateSpecification(entity1, TestEntity_.available, Operator.IS_NOT_NULL, true));
        assertTrue(service.validateSpecification(entity2, TestEntity_.available, Operator.IS_NOT_NULL, true));
    }

    @Test
    @DisplayName("Should support boolean field value extraction")
    void shouldSupportBooleanFieldValueExtraction() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setAvailable(true);

        SpecificationService<TestEntity> service = TestEntitySpecificationService.INSTANCE;

        // When & Then
        Object value = service.getFieldValue(entity, "available");
        assertNotNull(value);
        assertInstanceOf(Boolean.class, value);
        assertEquals(true, value);

        // Test with meta attribute
        Object metaValue = service.getFieldValue(entity, TestEntity_.available);
        assertNotNull(metaValue);
        assertInstanceOf(Boolean.class, metaValue);
        assertEquals(true, metaValue);
    }

    @Test
    @DisplayName("Should support primitive int field filtering")
    void shouldSupportPrimitiveIntFieldFiltering() {
        // Given
        TestEntity entity1 = new TestEntity();
        entity1.setScore(100);

        TestEntity entity2 = new TestEntity();
        entity2.setScore(50);

        SpecificationService<TestEntity> service = TestEntitySpecificationService.INSTANCE;

        // When & Then - Test EQUALS operator
        assertTrue(service.validateSpecification(entity1, TestEntity_.score, Operator.EQUALS, 100));
        assertFalse(service.validateSpecification(entity1, TestEntity_.score, Operator.EQUALS, 50));

        // Test GREATER_THAN operator
        assertTrue(service.validateSpecification(entity1, TestEntity_.score, Operator.GREATER_THAN, 50));
        assertFalse(service.validateSpecification(entity2, TestEntity_.score, Operator.GREATER_THAN, 100));

        // Test IS_NULL operator (should always return false for primitive int)
        assertFalse(service.validateSpecification(entity1, TestEntity_.score, Operator.IS_NULL, true));
        assertFalse(service.validateSpecification(entity2, TestEntity_.score, Operator.IS_NULL, true));

        // Test IS_NOT_NULL operator (should always return true for primitive int)
        assertTrue(service.validateSpecification(entity1, TestEntity_.score, Operator.IS_NOT_NULL, true));
        assertTrue(service.validateSpecification(entity2, TestEntity_.score, Operator.IS_NOT_NULL, true));
    }
}