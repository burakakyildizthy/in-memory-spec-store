package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for IntegerFilter operations.
 * Tests all integer-specific filtering operations including getters, setters, and utility methods.
 * Requirements: 4.2, 15.9
 */
class IntegerFilterTest {

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        IntegerFilter filter = new IntegerFilter();

        assertThat(filter.getEquals()).isNull();
        assertThat(filter.getNotEquals()).isNull();
        assertThat(filter.getGreaterThan()).isNull();
        assertThat(filter.getGreaterOrEqualThan()).isNull();
        assertThat(filter.getLessThan()).isNull();
        assertThat(filter.getLessOrEqualThan()).isNull();
        assertThat(filter.getIn()).isNull();
        assertThat(filter.getNotIn()).isNull();
        assertThat(filter.getIsNull()).isNull();
        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        IntegerFilter original = new IntegerFilter()
            .setEquals(100)
            .setNotEquals(200)
            .setGreaterThan(50)
            .setGreaterOrEqualThan(60)
            .setLessThan(150)
            .setLessOrEqualThan(140)
            .setIn(Arrays.asList(10, 20, 30))
            .setNotIn(Arrays.asList(40, 50, 60))
            .setIsNull(false)
            .setIsNotNull(true);

        IntegerFilter copy = new IntegerFilter(original);

        assertThat(copy.getEquals()).isEqualTo(100);
        assertThat(copy.getNotEquals()).isEqualTo(200);
        assertThat(copy.getGreaterThan()).isEqualTo(50);
        assertThat(copy.getGreaterOrEqualThan()).isEqualTo(60);
        assertThat(copy.getLessThan()).isEqualTo(150);
        assertThat(copy.getLessOrEqualThan()).isEqualTo(140);
        assertThat(copy.getIn()).containsExactly(10, 20, 30);
        assertThat(copy.getNotIn()).containsExactly(40, 50, 60);
        assertThat(copy.getIsNull()).isFalse();
        assertThat(copy.getIsNotNull()).isTrue();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        IntegerFilter original = new IntegerFilter().setEquals(100);
        IntegerFilter copy = new IntegerFilter(original);

        copy.setEquals(200);

        assertThat(original.getEquals()).isEqualTo(100);
        assertThat(copy.getEquals()).isEqualTo(200);
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(42);

        assertThat(filter.getEquals()).isEqualTo(42);
    }

    @Test
    void testSetEqualsWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsWithZero() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(0);

        assertThat(filter.getEquals()).isZero();
    }

    @Test
    void testSetEqualsWithNegativeValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(-100);

        assertThat(filter.getEquals()).isEqualTo(-100);
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setEquals(42);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setNotEquals(99);

        assertThat(filter.getNotEquals()).isEqualTo(99);
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setNotEquals(99);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterThan Tests ====================

    @Test
    void testSetGreaterThanSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterThan(50);

        assertThat(filter.getGreaterThan()).isEqualTo(50);
    }

    @Test
    void testSetGreaterThanWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterThan(null);

        assertThat(filter.getGreaterThan()).isNull();
    }

    @Test
    void testSetGreaterThanWithZero() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterThan(0);

        assertThat(filter.getGreaterThan()).isZero();
    }

    @Test
    void testSetGreaterThanWithNegativeValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterThan(-50);

        assertThat(filter.getGreaterThan()).isEqualTo(-50);
    }

    @Test
    void testSetGreaterThanReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setGreaterThan(50);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterOrEqualThan Tests ====================

    @Test
    void testSetGreaterOrEqualThanSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterOrEqualThan(75);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(75);
    }

    @Test
    void testSetGreaterOrEqualThanWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterOrEqualThan(null);

        assertThat(filter.getGreaterOrEqualThan()).isNull();
    }

    @Test
    void testSetGreaterOrEqualThanWithZero() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterOrEqualThan(0);

        assertThat(filter.getGreaterOrEqualThan()).isZero();
    }

    @Test
    void testSetGreaterOrEqualThanReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setGreaterOrEqualThan(75);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessThan Tests ====================

    @Test
    void testSetLessThanSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessThan(100);

        assertThat(filter.getLessThan()).isEqualTo(100);
    }

    @Test
    void testSetLessThanWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessThan(null);

        assertThat(filter.getLessThan()).isNull();
    }

    @Test
    void testSetLessThanWithZero() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessThan(0);

        assertThat(filter.getLessThan()).isZero();
    }

    @Test
    void testSetLessThanReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setLessThan(100);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessOrEqualThan Tests ====================

    @Test
    void testSetLessOrEqualThanSetsAndGetsValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessOrEqualThan(90);

        assertThat(filter.getLessOrEqualThan()).isEqualTo(90);
    }

    @Test
    void testSetLessOrEqualThanWithNullValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessOrEqualThan(null);

        assertThat(filter.getLessOrEqualThan()).isNull();
    }

    @Test
    void testSetLessOrEqualThanWithZero() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessOrEqualThan(0);

        assertThat(filter.getLessOrEqualThan()).isZero();
    }

    @Test
    void testSetLessOrEqualThanReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setLessOrEqualThan(90);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithList() {
        IntegerFilter filter = new IntegerFilter();
        List<Integer> values = Arrays.asList(10, 20, 30, 40, 50);
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly(10, 20, 30, 40, 50);
    }

    @Test
    void testSetInWithEmptyList() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIn(List.of());

        assertThat(filter.getIn()).isEmpty();
    }

    @Test
    void testSetInWithNull() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInWithSingleValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIn(Arrays.asList(42));

        assertThat(filter.getIn()).containsExactly(42);
    }

    @Test
    void testSetInWithNegativeValues() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIn(Arrays.asList(-10, -20, -30));

        assertThat(filter.getIn()).containsExactly(-10, -20, -30);
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setIn(Arrays.asList(1, 2, 3));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithList() {
        IntegerFilter filter = new IntegerFilter();
        List<Integer> values = Arrays.asList(100, 200, 300);
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly(100, 200, 300);
    }

    @Test
    void testSetNotInWithEmptyList() {
        IntegerFilter filter = new IntegerFilter();
        filter.setNotIn(List.of());

        assertThat(filter.getNotIn()).isNotNull().isEmpty();
    }

    @Test
    void testSetNotInWithNull() {
        IntegerFilter filter = new IntegerFilter();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        IntegerFilter filter = new IntegerFilter();
        IntegerFilter result = filter.setNotIn(Arrays.asList(1, 2, 3));

        assertThat(result).isSameAs(filter);
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testBoundaryValuesIntegerMinValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(Integer.MIN_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesIntegerMaxValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(Integer.MAX_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesGreaterThanMinValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setGreaterThan(Integer.MIN_VALUE);

        assertThat(filter.getGreaterThan()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesLessThanMaxValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setLessThan(Integer.MAX_VALUE);

        assertThat(filter.getLessThan()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesInListWithBoundaries() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIn(Arrays.asList(Integer.MIN_VALUE, 0, Integer.MAX_VALUE));

        assertThat(filter.getIn()).containsExactly(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesRangeWithBoundaries() {
        IntegerFilter filter = new IntegerFilter()
            .setGreaterOrEqualThan(Integer.MIN_VALUE)
            .setLessOrEqualThan(Integer.MAX_VALUE);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(Integer.MIN_VALUE);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(Integer.MAX_VALUE);
    }

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        IntegerFilter filter = new IntegerFilter()
            .setEquals(100)
            .setNotEquals(200)
            .setGreaterThan(50)
            .setGreaterOrEqualThan(60)
            .setLessThan(150)
            .setLessOrEqualThan(140)
            .setIn(Arrays.asList(10, 20, 30))
            .setNotIn(Arrays.asList(40, 50, 60))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo(100);
        assertThat(filter.getNotEquals()).isEqualTo(200);
        assertThat(filter.getGreaterThan()).isEqualTo(50);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(60);
        assertThat(filter.getLessThan()).isEqualTo(150);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(140);
        assertThat(filter.getIn()).containsExactly(10, 20, 30);
        assertThat(filter.getNotIn()).containsExactly(40, 50, 60);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Equals and HashCode Tests ====================

    @Test
    void testEqualsSameValues() {
        IntegerFilter filter1 = new IntegerFilter()
            .setEquals(100)
            .setGreaterThan(50);
        IntegerFilter filter2 = new IntegerFilter()
            .setEquals(100)
            .setGreaterThan(50);

        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2);
    }

    @Test
    void testEqualsDifferentValues() {
        IntegerFilter filter1 = new IntegerFilter().setEquals(100);
        IntegerFilter filter2 = new IntegerFilter().setEquals(200);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsSameInstance() {
        IntegerFilter filter = new IntegerFilter().setEquals(100);

        assertThat(filter).isEqualTo(filter);
    }

    @Test
    void testEqualsWithNull() {
        IntegerFilter filter = new IntegerFilter().setEquals(100);

        assertThat(filter).isNotEqualTo(null);
    }

    // ==================== ToString Tests ====================

    @Test
    void testToStringWithAllFields() {
        IntegerFilter filter = new IntegerFilter()
            .setEquals(100)
            .setGreaterThan(50)
            .setLessThan(150);

        String result = filter.toString();

        assertThat(result).contains("IntegerFilter")
                .contains("100")
                .contains("50")
                .contains("150");
    }

    @Test
    void testToStringWithEmptyFilter() {
        IntegerFilter filter = new IntegerFilter();

        String result = filter.toString();

        assertThat(result).contains("IntegerFilter");
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetFilterWithEquals() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);

        IntegerFilter ageFilter = new IntegerFilter().setEquals(25);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && u.getAge().equals(25))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getEquals()).isEqualTo(25);
    }

    @Test
    void testLargeDatasetFilterWithGreaterThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);

        IntegerFilter ageFilter = new IntegerFilter().setGreaterThan(50);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && u.getAge() > 50)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getGreaterThan()).isEqualTo(50);
    }

    @Test
    void testLargeDatasetFilterWithLessThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);

        IntegerFilter ageFilter = new IntegerFilter().setLessThan(30);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && u.getAge() < 30)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getLessThan()).isEqualTo(30);
    }

    @Test
    void testLargeDatasetFilterWithRange() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);

        IntegerFilter ageFilter = new IntegerFilter()
            .setGreaterOrEqualThan(25)
            .setLessOrEqualThan(35);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && u.getAge() >= 25 && u.getAge() <= 35)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getGreaterOrEqualThan()).isEqualTo(25);
        assertThat(ageFilter.getLessOrEqualThan()).isEqualTo(35);
    }

    @Test
    void testLargeDatasetFilterWithInList() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);
        List<Integer> targetAges = Arrays.asList(25, 30, 35, 40, 45);

        IntegerFilter ageFilter = new IntegerFilter().setIn(targetAges);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && targetAges.contains(u.getAge()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getIn()).containsExactlyElementsOf(targetAges);
    }

    @Test
    void testLargeDatasetFilterPerformance() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            
            users.stream()
                .filter(u -> u.getAge() != null && u.getAge() > 20 && u.getAge() < 60)
                .count();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testLargeDatasetComplexFilterCombination() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<SimpleUser> users = generator.generateSimpleUsers(10000);

        IntegerFilter ageFilter = new IntegerFilter()
            .setGreaterOrEqualThan(18)
            .setLessOrEqualThan(65)
            .setNotEquals(50);
        
        long count = users.stream()
            .filter(u -> u.getAge() != null && 
                        u.getAge() >= 18 && 
                        u.getAge() <= 65 && 
                        !u.getAge().equals(50))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(ageFilter.getGreaterOrEqualThan()).isEqualTo(18);
        assertThat(ageFilter.getLessOrEqualThan()).isEqualTo(65);
        assertThat(ageFilter.getNotEquals()).isEqualTo(50);
    }

    // ==================== Edge Cases and Null Safety Tests ====================

    @Test
    void testMultipleSettersOnSameField() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(100);
        filter.setEquals(200);
        filter.setEquals(300);

        assertThat(filter.getEquals()).isEqualTo(300);
    }

    @Test
    void testSettersWithNullAfterValue() {
        IntegerFilter filter = new IntegerFilter();
        filter.setEquals(100);
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testCombinedRangeOperations() {
        IntegerFilter filter = new IntegerFilter()
            .setGreaterThan(10)
            .setLessThan(100)
            .setGreaterOrEqualThan(20)
            .setLessOrEqualThan(90);

        assertThat(filter.getGreaterThan()).isEqualTo(10);
        assertThat(filter.getLessThan()).isEqualTo(100);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(20);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(90);
    }

    @Test
    void testInheritedMethodsIsNull() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testInheritedMethodsIsNotNull() {
        IntegerFilter filter = new IntegerFilter();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }
}
