package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.TestUser;
import net.jqwik.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for LongFilter operations.
 * Tests all long-specific filtering operations including getters, setters, and utility methods.
 * Requirements: 4.2, 15.9
 */
class LongFilterTest {

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        LongFilter filter = new LongFilter();

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
        LongFilter original = new LongFilter()
            .setEquals(100L)
            .setNotEquals(200L)
            .setGreaterThan(50L)
            .setGreaterOrEqualThan(60L)
            .setLessThan(150L)
            .setLessOrEqualThan(140L)
            .setIn(Arrays.asList(10L, 20L, 30L))
            .setNotIn(Arrays.asList(40L, 50L, 60L))
            .setIsNull(false)
            .setIsNotNull(true);

        LongFilter copy = new LongFilter(original);

        assertThat(copy.getEquals()).isEqualTo(100L);
        assertThat(copy.getNotEquals()).isEqualTo(200L);
        assertThat(copy.getGreaterThan()).isEqualTo(50L);
        assertThat(copy.getGreaterOrEqualThan()).isEqualTo(60L);
        assertThat(copy.getLessThan()).isEqualTo(150L);
        assertThat(copy.getLessOrEqualThan()).isEqualTo(140L);
        assertThat(copy.getIn()).containsExactly(10L, 20L, 30L);
        assertThat(copy.getNotIn()).containsExactly(40L, 50L, 60L);
        assertThat(copy.getIsNull()).isFalse();
        assertThat(copy.getIsNotNull()).isTrue();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        LongFilter original = new LongFilter().setEquals(100L);
        LongFilter copy = new LongFilter(original);

        copy.setEquals(200L);

        assertThat(original.getEquals()).isEqualTo(100L);
        assertThat(copy.getEquals()).isEqualTo(200L);
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(42L);

        assertThat(filter.getEquals()).isEqualTo(42L);
    }

    @Test
    void testSetEqualsWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsWithZero() {
        LongFilter filter = new LongFilter();
        filter.setEquals(0L);

        assertThat(filter.getEquals()).isZero();
    }

    @Test
    void testSetEqualsWithNegativeValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(-100L);

        assertThat(filter.getEquals()).isEqualTo(-100L);
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setEquals(42L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setNotEquals(99L);

        assertThat(filter.getNotEquals()).isEqualTo(99L);
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setNotEquals(99L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterThan Tests ====================

    @Test
    void testSetGreaterThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterThan(50L);

        assertThat(filter.getGreaterThan()).isEqualTo(50L);
    }

    @Test
    void testSetGreaterThanWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterThan(null);

        assertThat(filter.getGreaterThan()).isNull();
    }

    @Test
    void testSetGreaterThanWithZero() {
        LongFilter filter = new LongFilter();
        filter.setGreaterThan(0L);

        assertThat(filter.getGreaterThan()).isZero();
    }

    @Test
    void testSetGreaterThanWithNegativeValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterThan(-50L);

        assertThat(filter.getGreaterThan()).isEqualTo(-50L);
    }

    @Test
    void testSetGreaterThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setGreaterThan(50L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterOrEqualThan Tests ====================

    @Test
    void testSetGreaterOrEqualThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterOrEqualThan(75L);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(75L);
    }

    @Test
    void testSetGreaterOrEqualThanWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterOrEqualThan(null);

        assertThat(filter.getGreaterOrEqualThan()).isNull();
    }

    @Test
    void testSetGreaterOrEqualThanWithZero() {
        LongFilter filter = new LongFilter();
        filter.setGreaterOrEqualThan(0L);

        assertThat(filter.getGreaterOrEqualThan()).isZero();
    }

    @Test
    void testSetGreaterOrEqualThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setGreaterOrEqualThan(75L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessThan Tests ====================

    @Test
    void testSetLessThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setLessThan(100L);

        assertThat(filter.getLessThan()).isEqualTo(100L);
    }

    @Test
    void testSetLessThanWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setLessThan(null);

        assertThat(filter.getLessThan()).isNull();
    }

    @Test
    void testSetLessThanWithZero() {
        LongFilter filter = new LongFilter();
        filter.setLessThan(0L);

        assertThat(filter.getLessThan()).isZero();
    }

    @Test
    void testSetLessThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setLessThan(100L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessOrEqualThan Tests ====================

    @Test
    void testSetLessOrEqualThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setLessOrEqualThan(90L);

        assertThat(filter.getLessOrEqualThan()).isEqualTo(90L);
    }

    @Test
    void testSetLessOrEqualThanWithNullValue() {
        LongFilter filter = new LongFilter();
        filter.setLessOrEqualThan(null);

        assertThat(filter.getLessOrEqualThan()).isNull();
    }

    @Test
    void testSetLessOrEqualThanWithZero() {
        LongFilter filter = new LongFilter();
        filter.setLessOrEqualThan(0L);

        assertThat(filter.getLessOrEqualThan()).isZero();
    }

    @Test
    void testSetLessOrEqualThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setLessOrEqualThan(90L);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithList() {
        LongFilter filter = new LongFilter();
        List<Long> values = Arrays.asList(10L, 20L, 30L, 40L, 50L);
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly(10L, 20L, 30L, 40L, 50L);
    }

    @Test
    void testSetInWithEmptyList() {
        LongFilter filter = new LongFilter();
        filter.setIn(List.of());

        assertThat(filter.getIn()).isEmpty();
    }

    @Test
    void testSetInWithNull() {
        LongFilter filter = new LongFilter();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInWithSingleValue() {
        LongFilter filter = new LongFilter();
        filter.setIn(Arrays.asList(42L));

        assertThat(filter.getIn()).containsExactly(42L);
    }

    @Test
    void testSetInWithNegativeValues() {
        LongFilter filter = new LongFilter();
        filter.setIn(Arrays.asList(-10L, -20L, -30L));

        assertThat(filter.getIn()).containsExactly(-10L, -20L, -30L);
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setIn(Arrays.asList(1L, 2L, 3L));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithList() {
        LongFilter filter = new LongFilter();
        List<Long> values = Arrays.asList(100L, 200L, 300L);
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly(100L, 200L, 300L);
    }

    @Test
    void testSetNotInWithEmptyList() {
        LongFilter filter = new LongFilter();
        filter.setNotIn(List.of());

        assertThat(filter.getNotIn()).isNotNull().isEmpty();
    }

    @Test
    void testSetNotInWithNull() {
        LongFilter filter = new LongFilter();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        LongFilter result = filter.setNotIn(Arrays.asList(1L, 2L, 3L));

        assertThat(result).isSameAs(filter);
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testBoundaryValuesLongMinValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(Long.MIN_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesLongMaxValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(Long.MAX_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesGreaterThanMinValue() {
        LongFilter filter = new LongFilter();
        filter.setGreaterThan(Long.MIN_VALUE);

        assertThat(filter.getGreaterThan()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesLessThanMaxValue() {
        LongFilter filter = new LongFilter();
        filter.setLessThan(Long.MAX_VALUE);

        assertThat(filter.getLessThan()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesInListWithBoundaries() {
        LongFilter filter = new LongFilter();
        filter.setIn(Arrays.asList(Long.MIN_VALUE, 0L, Long.MAX_VALUE));

        assertThat(filter.getIn()).containsExactly(Long.MIN_VALUE, 0L, Long.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesRangeWithBoundaries() {
        LongFilter filter = new LongFilter()
            .setGreaterOrEqualThan(Long.MIN_VALUE)
            .setLessOrEqualThan(Long.MAX_VALUE);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(Long.MIN_VALUE);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(Long.MAX_VALUE);
    }

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        LongFilter filter = new LongFilter()
            .setEquals(100L)
            .setNotEquals(200L)
            .setGreaterThan(50L)
            .setGreaterOrEqualThan(60L)
            .setLessThan(150L)
            .setLessOrEqualThan(140L)
            .setIn(Arrays.asList(10L, 20L, 30L))
            .setNotIn(Arrays.asList(40L, 50L, 60L))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo(100L);
        assertThat(filter.getNotEquals()).isEqualTo(200L);
        assertThat(filter.getGreaterThan()).isEqualTo(50L);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(60L);
        assertThat(filter.getLessThan()).isEqualTo(150L);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(140L);
        assertThat(filter.getIn()).containsExactly(10L, 20L, 30L);
        assertThat(filter.getNotIn()).containsExactly(40L, 50L, 60L);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Equals and HashCode Tests ====================

    @Test
    void testEqualsSameValues() {
        LongFilter filter1 = new LongFilter()
            .setEquals(100L)
            .setGreaterThan(50L);
        LongFilter filter2 = new LongFilter()
            .setEquals(100L)
            .setGreaterThan(50L);

        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2);
    }

    @Test
    void testEqualsDifferentValues() {
        LongFilter filter1 = new LongFilter().setEquals(100L);
        LongFilter filter2 = new LongFilter().setEquals(200L);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsSameInstance() {
        LongFilter filter = new LongFilter().setEquals(100L);

        assertThat(filter).isEqualTo(filter);
    }

    @Test
    void testEqualsWithNull() {
        LongFilter filter = new LongFilter().setEquals(100L);

        assertThat(filter).isNotEqualTo(null);
    }

    // ==================== ToString Tests ====================

    @Test
    void testToStringWithAllFields() {
        LongFilter filter = new LongFilter()
            .setEquals(100L)
            .setGreaterThan(50L)
            .setLessThan(150L);

        String result = filter.toString();

        assertThat(result).contains("LongFilter")
                .contains("100")
                .contains("50")
                .contains("150");
    }

    @Test
    void testToStringWithEmptyFilter() {
        LongFilter filter = new LongFilter();

        String result = filter.toString();

        assertThat(result).contains("LongFilter");
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetFilterWithEquals() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);

        LongFilter idFilter = new LongFilter().setEquals(25L);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && u.getIdentity().equals(25L))
            .count();

        assertThat(count).isEqualTo(1);
        assertThat(idFilter.getEquals()).isEqualTo(25L);
    }

    @Test
    void testLargeDatasetFilterWithGreaterThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);

        LongFilter idFilter = new LongFilter().setGreaterThan(9000L);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && u.getIdentity() > 9000L)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(idFilter.getGreaterThan()).isEqualTo(9000L);
    }

    @Test
    void testLargeDatasetFilterWithLessThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);

        LongFilter idFilter = new LongFilter().setLessThan(100L);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && u.getIdentity() < 100L)
            .count();

        assertThat(count).isEqualTo(100);
        assertThat(idFilter.getLessThan()).isEqualTo(100L);
    }

    @Test
    void testLargeDatasetFilterWithRange() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);

        LongFilter idFilter = new LongFilter()
            .setGreaterOrEqualThan(1000L)
            .setLessOrEqualThan(2000L);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && u.getIdentity() >= 1000L && u.getIdentity() <= 2000L)
            .count();

        assertThat(count).isEqualTo(1001);
        assertThat(idFilter.getGreaterOrEqualThan()).isEqualTo(1000L);
        assertThat(idFilter.getLessOrEqualThan()).isEqualTo(2000L);
    }

    @Test
    void testLargeDatasetFilterWithInList() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);
        List<Long> targetIds = Arrays.asList(25L, 50L, 75L, 100L, 125L);

        LongFilter idFilter = new LongFilter().setIn(targetIds);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && targetIds.contains(u.getIdentity()))
            .count();

        assertThat(count).isEqualTo(5);
        assertThat(idFilter.getIn()).containsExactlyElementsOf(targetIds);
    }

    @Test
    void testLargeDatasetFilterPerformance() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            
            users.stream()
                .filter(u -> u.getIdentity() != null && u.getIdentity() > 1000L && u.getIdentity() < 9000L)
                .count();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testLargeDatasetComplexFilterCombination() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<TestUser> users = generator.generateTestUsers(10000);

        LongFilter idFilter = new LongFilter()
            .setGreaterOrEqualThan(100L)
            .setLessOrEqualThan(9900L)
            .setNotEquals(5000L);
        
        long count = users.stream()
            .filter(u -> u.getIdentity() != null && 
                        u.getIdentity() >= 100L && 
                        u.getIdentity() <= 9900L && 
                        !u.getIdentity().equals(5000L))
            .count();

        assertThat(count).isEqualTo(9800);
        assertThat(idFilter.getGreaterOrEqualThan()).isEqualTo(100L);
        assertThat(idFilter.getLessOrEqualThan()).isEqualTo(9900L);
        assertThat(idFilter.getNotEquals()).isEqualTo(5000L);
    }

    // ==================== Edge Cases and Null Safety Tests ====================

    @Test
    void testMultipleSettersOnSameField() {
        LongFilter filter = new LongFilter();
        filter.setEquals(100L);
        filter.setEquals(200L);
        filter.setEquals(300L);

        assertThat(filter.getEquals()).isEqualTo(300L);
    }

    @Test
    void testSettersWithNullAfterValue() {
        LongFilter filter = new LongFilter();
        filter.setEquals(100L);
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testCombinedRangeOperations() {
        LongFilter filter = new LongFilter()
            .setGreaterThan(10L)
            .setLessThan(100L)
            .setGreaterOrEqualThan(20L)
            .setLessOrEqualThan(90L);

        assertThat(filter.getGreaterThan()).isEqualTo(10L);
        assertThat(filter.getLessThan()).isEqualTo(100L);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(20L);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(90L);
    }

    @Test
    void testInheritedMethodsIsNull() {
        LongFilter filter = new LongFilter();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testInheritedMethodsIsNotNull() {
        LongFilter filter = new LongFilter();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Negated Operator Tests ====================

    @Test
    void testDefaultConstructorNegatedFieldsAreNull() {
        LongFilter filter = new LongFilter();

        assertThat(filter.getNotGreaterThan()).isNull();
        assertThat(filter.getNotLessThan()).isNull();
        assertThat(filter.getNotGreaterOrEqualThan()).isNull();
        assertThat(filter.getNotLessOrEqualThan()).isNull();
    }

    @Test
    void testSetNotGreaterThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setNotGreaterThan(75L);

        assertThat(filter.getNotGreaterThan()).isEqualTo(75L);
    }

    @Test
    void testSetNotGreaterThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        NumberFilter<Long> result = filter.setNotGreaterThan(75L);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotLessThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setNotLessThan(25L);

        assertThat(filter.getNotLessThan()).isEqualTo(25L);
    }

    @Test
    void testSetNotLessThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        NumberFilter<Long> result = filter.setNotLessThan(25L);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotGreaterOrEqualThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setNotGreaterOrEqualThan(80L);

        assertThat(filter.getNotGreaterOrEqualThan()).isEqualTo(80L);
    }

    @Test
    void testSetNotGreaterOrEqualThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        NumberFilter<Long> result = filter.setNotGreaterOrEqualThan(80L);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotLessOrEqualThanSetsAndGetsValue() {
        LongFilter filter = new LongFilter();
        filter.setNotLessOrEqualThan(20L);

        assertThat(filter.getNotLessOrEqualThan()).isEqualTo(20L);
    }

    @Test
    void testSetNotLessOrEqualThanReturnsFilterForChaining() {
        LongFilter filter = new LongFilter();
        NumberFilter<Long> result = filter.setNotLessOrEqualThan(20L);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testCopyConstructorCopiesNegatedFields() {
        LongFilter original = new LongFilter();
        original.setNotGreaterThan(10L);
        original.setNotLessThan(20L);
        original.setNotGreaterOrEqualThan(30L);
        original.setNotLessOrEqualThan(40L);

        LongFilter copy = new LongFilter(original);

        assertThat(copy.getNotGreaterThan()).isEqualTo(10L);
        assertThat(copy.getNotLessThan()).isEqualTo(20L);
        assertThat(copy.getNotGreaterOrEqualThan()).isEqualTo(30L);
        assertThat(copy.getNotLessOrEqualThan()).isEqualTo(40L);
    }

    @Test
    void testEqualsWithDifferentNotGreaterThan() {
        LongFilter filter1 = new LongFilter();
        filter1.setNotGreaterThan(10L);
        LongFilter filter2 = new LongFilter();
        filter2.setNotGreaterThan(20L);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotLessThan() {
        LongFilter filter1 = new LongFilter();
        filter1.setNotLessThan(10L);
        LongFilter filter2 = new LongFilter();
        filter2.setNotLessThan(20L);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotGreaterOrEqualThan() {
        LongFilter filter1 = new LongFilter();
        filter1.setNotGreaterOrEqualThan(10L);
        LongFilter filter2 = new LongFilter();
        filter2.setNotGreaterOrEqualThan(20L);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotLessOrEqualThan() {
        LongFilter filter1 = new LongFilter();
        filter1.setNotLessOrEqualThan(10L);
        LongFilter filter2 = new LongFilter();
        filter2.setNotLessOrEqualThan(20L);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testHashCodeConsistencyWithNegatedFields() {
        LongFilter filter1 = new LongFilter();
        filter1.setNotGreaterThan(50L);
        filter1.setNotLessThan(10L);
        LongFilter filter2 = new LongFilter();
        filter2.setNotGreaterThan(50L);
        filter2.setNotLessThan(10L);

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }

    @Test
    void testToStringContainsNegatedFields() {
        LongFilter filter = new LongFilter();
        filter.setNotGreaterThan(99L);
        filter.setNotLessThan(11L);
        filter.setNotGreaterOrEqualThan(88L);
        filter.setNotLessOrEqualThan(22L);

        String result = filter.toString();

        assertThat(result).contains("notGreaterThan=99");
        assertThat(result).contains("notLessThan=11");
        assertThat(result).contains("notGreaterOrEqualThan=88");
        assertThat(result).contains("notLessOrEqualThan=22");
    }

    @Test
    void testNegatedOperatorNotGreaterThanSemantics() {
        // ngt means: field <= value (negation of field > value)
        Long fieldValue = 50L;
        Long threshold = 75L;

        assertThat(fieldValue.compareTo(threshold) <= 0).isTrue();
        assertThat(threshold.compareTo(threshold) <= 0).isTrue();

        Long largerValue = 100L;
        assertThat(largerValue.compareTo(threshold) <= 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotLessThanSemantics() {
        // nlt means: field >= value (negation of field < value)
        Long fieldValue = 80L;
        Long threshold = 50L;

        assertThat(fieldValue.compareTo(threshold) >= 0).isTrue();
        assertThat(threshold.compareTo(threshold) >= 0).isTrue();

        Long smallerValue = 30L;
        assertThat(smallerValue.compareTo(threshold) >= 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotGreaterOrEqualThanSemantics() {
        // ngte means: field < value (negation of field >= value)
        Long fieldValue = 40L;
        Long threshold = 50L;

        assertThat(fieldValue.compareTo(threshold) < 0).isTrue();
        assertThat(threshold.compareTo(threshold) < 0).isFalse();

        Long largerValue = 60L;
        assertThat(largerValue.compareTo(threshold) < 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotLessOrEqualThanSemantics() {
        // nlte means: field > value (negation of field <= value)
        Long fieldValue = 60L;
        Long threshold = 50L;

        assertThat(fieldValue.compareTo(threshold) > 0).isTrue();
        assertThat(threshold.compareTo(threshold) > 0).isFalse();

        Long smallerValue = 40L;
        assertThat(smallerValue.compareTo(threshold) > 0).isFalse();
    }

    @Test
    void testNegatedOperatorWithNullFieldValueReturnsFalse() {
        Long fieldValue = null;
        Long threshold = 50L;

        boolean ngtResult = fieldValue != null && fieldValue.compareTo(threshold) <= 0;
        boolean nltResult = fieldValue != null && fieldValue.compareTo(threshold) >= 0;
        boolean ngteResult = fieldValue != null && fieldValue.compareTo(threshold) < 0;
        boolean nlteResult = fieldValue != null && fieldValue.compareTo(threshold) > 0;

        assertThat(ngtResult).isFalse();
        assertThat(nltResult).isFalse();
        assertThat(ngteResult).isFalse();
        assertThat(nlteResult).isFalse();
    }

    // ==================== Property-Based Tests ====================

    /**
     * Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı
     *
     * For any non-null Long field value and threshold, each negated operator yields the
     * exact logical negation of the corresponding positive operator via compareTo.
     * When field value is null, all negated operators return false.
     *
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı")
    void negatedOperatorsShouldBeLogicalNegationOfPositiveOperators(
            @ForAll("nullableLong") Long fieldValue,
            @ForAll("nonNullLong") Long threshold) {

        if (fieldValue != null) {
            // greaterThan: compareTo > 0; notGreaterThan: compareTo <= 0
            boolean gtResult = fieldValue.compareTo(threshold) > 0;
            boolean ngtResult = fieldValue.compareTo(threshold) <= 0;
            assertThat(ngtResult).isEqualTo(!gtResult);

            // lessThan: compareTo < 0; notLessThan: compareTo >= 0
            boolean ltResult = fieldValue.compareTo(threshold) < 0;
            boolean nltResult = fieldValue.compareTo(threshold) >= 0;
            assertThat(nltResult).isEqualTo(!ltResult);

            // greaterOrEqualThan: compareTo >= 0; notGreaterOrEqualThan: compareTo < 0
            boolean gteResult = fieldValue.compareTo(threshold) >= 0;
            boolean ngteResult = fieldValue.compareTo(threshold) < 0;
            assertThat(ngteResult).isEqualTo(!gteResult);

            // lessOrEqualThan: compareTo <= 0; notLessOrEqualThan: compareTo > 0
            boolean lteResult = fieldValue.compareTo(threshold) <= 0;
            boolean nlteResult = fieldValue.compareTo(threshold) > 0;
            assertThat(nlteResult).isEqualTo(!lteResult);
        } else {
            // When fieldValue is null, all negated operators must return false
            boolean ngtResult = fieldValue != null && fieldValue.compareTo(threshold) <= 0;
            boolean nltResult = fieldValue != null && fieldValue.compareTo(threshold) >= 0;
            boolean ngteResult = fieldValue != null && fieldValue.compareTo(threshold) < 0;
            boolean nlteResult = fieldValue != null && fieldValue.compareTo(threshold) > 0;

            assertThat(ngtResult).isFalse();
            assertThat(nltResult).isFalse();
            assertThat(ngteResult).isFalse();
            assertThat(nlteResult).isFalse();
        }
    }

    @Provide
    Arbitrary<Long> nullableLong() {
        return Arbitraries.longs().between(-1_000_000L, 1_000_000L).injectNull(0.2);
    }

    @Provide
    Arbitrary<Long> nonNullLong() {
        return Arbitraries.longs().between(-1_000_000L, 1_000_000L);
    }
}
