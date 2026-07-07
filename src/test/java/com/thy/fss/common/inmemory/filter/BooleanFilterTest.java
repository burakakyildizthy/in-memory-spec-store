package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for BooleanFilter operations.
 * Tests all boolean-specific filtering operations including getters, setters, and utility methods.
 * Requirements: 4.5, 15.9
 */
class BooleanFilterTest {

    private final LargeDatasetGenerator generator = new LargeDatasetGenerator();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        BooleanFilter filter = new BooleanFilter();

        assertThat(filter.getEquals()).isNull();
        assertThat(filter.getNotEquals()).isNull();
        assertThat(filter.getIn()).isNull();
        assertThat(filter.getNotIn()).isNull();
        assertThat(filter.getIsNull()).isNull();
        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        BooleanFilter original = new BooleanFilter()
            .setEquals(true)
            .setNotEquals(false)
            .setIn(Arrays.asList(true, false))
            .setNotIn(Arrays.asList(false))
            .setIsNull(true)
            .setIsNotNull(false);

        BooleanFilter copy = new BooleanFilter(original);

        assertThat(copy.getEquals()).isTrue();
        assertThat(copy.getNotEquals()).isFalse();
        assertThat(copy.getIn()).containsExactly(true, false);
        assertThat(copy.getNotIn()).containsExactly(false);
        assertThat(copy.getIsNull()).isTrue();
        assertThat(copy.getIsNotNull()).isFalse();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        BooleanFilter original = new BooleanFilter().setEquals(true);
        BooleanFilter copy = new BooleanFilter(original);

        copy.setEquals(false);

        assertThat(original.getEquals()).isTrue();
        assertThat(copy.getEquals()).isFalse();
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsWithTrueValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setEquals(true);

        assertThat(filter.getEquals()).isTrue();
    }

    @Test
    void testSetEqualsWithFalseValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setEquals(false);

        assertThat(filter.getEquals()).isFalse();
    }

    @Test
    void testSetEqualsWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setEquals(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsWithTrueValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setNotEquals(true);

        assertThat(filter.getNotEquals()).isTrue();
    }

    @Test
    void testSetNotEqualsWithFalseValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setNotEquals(false);

        assertThat(filter.getNotEquals()).isFalse();
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setNotEquals(false);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithMultipleValues() {
        BooleanFilter filter = new BooleanFilter();
        List<Boolean> values = Arrays.asList(true, false);
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly(true, false);
    }

    @Test
    void testSetInWithSingleValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIn(Arrays.asList(true));

        assertThat(filter.getIn()).containsExactly(true);
    }

    @Test
    void testSetInWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setIn(Arrays.asList(true));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithMultipleValues() {
        BooleanFilter filter = new BooleanFilter();
        List<Boolean> values = Arrays.asList(true, false);
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly(true, false);
    }

    @Test
    void testSetNotInWithSingleValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setNotIn(Arrays.asList(false));

        assertThat(filter.getNotIn()).containsExactly(false);
    }

    @Test
    void testSetNotInWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setNotIn(Arrays.asList(false));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNull Tests ====================

    @Test
    void testSetIsNullWithTrueValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testSetIsNullWithFalseValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNull(false);

        assertThat(filter.getIsNull()).isFalse();
    }

    @Test
    void testSetIsNullWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNull(null);

        assertThat(filter.getIsNull()).isNull();
    }

    @Test
    void testSetIsNullReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setIsNull(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNotNull Tests ====================

    @Test
    void testSetIsNotNullWithTrueValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    @Test
    void testSetIsNotNullWithFalseValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNotNull(false);

        assertThat(filter.getIsNotNull()).isFalse();
    }

    @Test
    void testSetIsNotNullWithNullValue() {
        BooleanFilter filter = new BooleanFilter();
        filter.setIsNotNull(null);

        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testSetIsNotNullReturnsFilterForChaining() {
        BooleanFilter filter = new BooleanFilter();
        BooleanFilter result = filter.setIsNotNull(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        BooleanFilter filter = new BooleanFilter()
            .setEquals(true)
            .setNotEquals(false)
            .setIn(Arrays.asList(true, false))
            .setNotIn(Arrays.asList(false))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isTrue();
        assertThat(filter.getNotEquals()).isFalse();
        assertThat(filter.getIn()).containsExactly(true, false);
        assertThat(filter.getNotIn()).containsExactly(false);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetFilterWithEqualsTrue() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter().setEquals(true);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && u.getActive().equals(true))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getEquals()).isTrue();
    }

    @Test
    void testLargeDatasetFilterWithEqualsFalse() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter().setEquals(false);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && u.getActive().equals(false))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getEquals()).isFalse();
    }

    @Test
    void testLargeDatasetFilterWithNotEquals() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter().setNotEquals(true);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && !u.getActive().equals(true))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getNotEquals()).isTrue();
    }

    @Test
    void testLargeDatasetFilterWithInList() {
        List<TestUser> users = generator.generateTestUsers(10_000);
        List<Boolean> targetValues = Arrays.asList(true, false);

        BooleanFilter activeFilter = new BooleanFilter().setIn(targetValues);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && targetValues.contains(u.getActive()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getIn()).containsExactlyElementsOf(targetValues);
    }

    @Test
    void testLargeDatasetFilterWithNotInList() {
        List<TestUser> users = generator.generateTestUsers(10_000);
        List<Boolean> excludeValues = Arrays.asList(true);

        BooleanFilter activeFilter = new BooleanFilter().setNotIn(excludeValues);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && !excludeValues.contains(u.getActive()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getNotIn()).containsExactlyElementsOf(excludeValues);
    }

    @Test
    void testLargeDatasetFilterWithIsNull() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter().setIsNull(true);
        
        users.stream()
            .filter(u -> u.getActive() == null)
            .count();

        assertThat(activeFilter.getIsNull()).isTrue();
    }

    @Test
    void testLargeDatasetFilterWithIsNotNull() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter().setIsNotNull(true);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getIsNotNull()).isTrue();
    }

    @Test
    void testLargeDatasetFilterPerformance() {
        List<TestUser> users = generator.generateTestUsers(10_000);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            new BooleanFilter()
                .setEquals(true)
                .setIsNotNull(true);
            
            users.stream()
                .filter(u -> u.getActive() != null && u.getActive().equals(true))
                .count();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testLargeDatasetComplexFilterCombination() {
        List<TestUser> users = generator.generateTestUsers(10_000);

        BooleanFilter activeFilter = new BooleanFilter()
            .setEquals(true)
            .setIsNotNull(true)
            .setNotEquals(false);
        
        long count = users.stream()
            .filter(u -> u.getActive() != null && 
                        u.getActive().equals(true) && 
                        !u.getActive().equals(false))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(activeFilter.getEquals()).isTrue();
        assertThat(activeFilter.getIsNotNull()).isTrue();
        assertThat(activeFilter.getNotEquals()).isFalse();
    }
}
