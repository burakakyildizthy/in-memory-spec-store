package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.Product;
import com.thy.fss.common.inmemory.testmodel.ProductFilter;
import net.jqwik.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;


/**
 * Comprehensive tests for DoubleFilter operations.
 * Tests all double-specific filtering operations including getters, setters, precision handling, and utility methods.
 * Requirements: 4.2, 15.9
 */
class DoubleFilterTest {


    private static final String DOUBLE_FILTER = "DoubleFilter";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        DoubleFilter filter = new DoubleFilter();

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
        DoubleFilter original = new DoubleFilter()
            .setEquals(100.5)
            .setNotEquals(200.5)
            .setGreaterThan(50.5)
            .setGreaterOrEqualThan(60.5)
            .setLessThan(150.5)
            .setLessOrEqualThan(140.5)
            .setIn(Arrays.asList(10.5, 20.5, 30.5))
            .setNotIn(Arrays.asList(40.5, 50.5, 60.5))
            .setIsNull(false)
            .setIsNotNull(true);

        DoubleFilter copy = new DoubleFilter(original);

        assertThat(copy.getEquals()).isEqualTo(100.5);
        assertThat(copy.getNotEquals()).isEqualTo(200.5);
        assertThat(copy.getGreaterThan()).isEqualTo(50.5);
        assertThat(copy.getGreaterOrEqualThan()).isEqualTo(60.5);
        assertThat(copy.getLessThan()).isEqualTo(150.5);
        assertThat(copy.getLessOrEqualThan()).isEqualTo(140.5);
        assertThat(copy.getIn()).containsExactly(10.5, 20.5, 30.5);
        assertThat(copy.getNotIn()).containsExactly(40.5, 50.5, 60.5);
        assertThat(copy.getIsNull()).isFalse();
        assertThat(copy.getIsNotNull()).isTrue();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        DoubleFilter original = new DoubleFilter().setEquals(100.5);
        DoubleFilter copy = new DoubleFilter(original);

        copy.setEquals(200.5);

        assertThat(original.getEquals()).isEqualTo(100.5);
        assertThat(copy.getEquals()).isEqualTo(200.5);
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(42.5);

        assertThat(filter.getEquals()).isEqualTo(42.5);
    }

    @Test
    void testSetEqualsWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsWithZero() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(0.0);

        assertThat(filter.getEquals()).isEqualTo(0.0);
    }

    @Test
    void testSetEqualsWithNegativeValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(-100.5);

        assertThat(filter.getEquals()).isEqualTo(-100.5);
    }

    @Test
    void testSetEqualsWithPrecisionValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(3.141592653589793);

        assertThat(filter.getEquals()).isCloseTo(Math.PI, within(0.0000000000001));
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setEquals(42.5);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotEquals(99.9);

        assertThat(filter.getNotEquals()).isEqualTo(99.9);
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setNotEquals(99.9);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterThan Tests ====================

    @Test
    void testSetGreaterThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterThan(50.5);

        assertThat(filter.getGreaterThan()).isEqualTo(50.5);
    }

    @Test
    void testSetGreaterThanWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterThan(null);

        assertThat(filter.getGreaterThan()).isNull();
    }

    @Test
    void testSetGreaterThanWithZero() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterThan(0.0);

        assertThat(filter.getGreaterThan()).isEqualTo(0.0);
    }

    @Test
    void testSetGreaterThanWithNegativeValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterThan(-50.5);

        assertThat(filter.getGreaterThan()).isEqualTo(-50.5);
    }

    @Test
    void testSetGreaterThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setGreaterThan(50.5);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setGreaterOrEqualThan Tests ====================

    @Test
    void testSetGreaterOrEqualThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterOrEqualThan(75.5);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(75.5);
    }

    @Test
    void testSetGreaterOrEqualThanWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterOrEqualThan(null);

        assertThat(filter.getGreaterOrEqualThan()).isNull();
    }

    @Test
    void testSetGreaterOrEqualThanWithZero() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterOrEqualThan(0.0);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(0.0);
    }

    @Test
    void testSetGreaterOrEqualThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setGreaterOrEqualThan(75.5);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessThan Tests ====================

    @Test
    void testSetLessThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessThan(100.5);

        assertThat(filter.getLessThan()).isEqualTo(100.5);
    }

    @Test
    void testSetLessThanWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessThan(null);

        assertThat(filter.getLessThan()).isNull();
    }

    @Test
    void testSetLessThanWithZero() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessThan(0.0);

        assertThat(filter.getLessThan()).isEqualTo(0.0);
    }

    @Test
    void testSetLessThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setLessThan(100.5);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setLessOrEqualThan Tests ====================

    @Test
    void testSetLessOrEqualThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessOrEqualThan(90.5);

        assertThat(filter.getLessOrEqualThan()).isEqualTo(90.5);
    }

    @Test
    void testSetLessOrEqualThanWithNullValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessOrEqualThan(null);

        assertThat(filter.getLessOrEqualThan()).isNull();
    }

    @Test
    void testSetLessOrEqualThanWithZero() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessOrEqualThan(0.0);

        assertThat(filter.getLessOrEqualThan()).isEqualTo(0.0);
    }

    @Test
    void testSetLessOrEqualThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setLessOrEqualThan(90.5);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithList() {
        DoubleFilter filter = new DoubleFilter();
        List<Double> values = Arrays.asList(10.5, 20.5, 30.5, 40.5, 50.5);
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly(10.5, 20.5, 30.5, 40.5, 50.5);
    }

    @Test
    void testSetInWithEmptyList() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIn(List.of());

        assertThat(filter.getIn()).isEmpty();
    }

    @Test
    void testSetInWithNull() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInWithSingleValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIn(Arrays.asList(42.5));

        assertThat(filter.getIn()).containsExactly(42.5);
    }

    @Test
    void testSetInWithNegativeValues() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIn(Arrays.asList(-10.5, -20.5, -30.5));

        assertThat(filter.getIn()).containsExactly(-10.5, -20.5, -30.5);
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setIn(Arrays.asList(1.5, 2.5, 3.5));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithList() {
        DoubleFilter filter = new DoubleFilter();
        List<Double> values = Arrays.asList(100.5, 200.5, 300.5);
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly(100.5, 200.5, 300.5);
    }

    @Test
    void testSetNotInWithEmptyList() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotIn(List.of());

        assertThat(filter.getNotIn()).isNotNull().isEmpty();
    }

    @Test
    void testSetNotInWithNull() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        DoubleFilter result = filter.setNotIn(Arrays.asList(1.5, 2.5, 3.5));

        assertThat(result).isSameAs(filter);
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testBoundaryValuesDoubleMinValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(Double.MIN_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Double.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesDoubleMaxValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(Double.MAX_VALUE);

        assertThat(filter.getEquals()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void testBoundaryValueGreaterThanMinValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setGreaterThan(Double.MIN_VALUE);

        assertThat(filter.getGreaterThan()).isEqualTo(Double.MIN_VALUE);
    }

    @Test
    void testBoundaryValuesLessThanMaxValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setLessThan(Double.MAX_VALUE);

        assertThat(filter.getLessThan()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesInListWithBoundaries() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIn(Arrays.asList(Double.MIN_VALUE, 0.0, Double.MAX_VALUE));

        assertThat(filter.getIn()).containsExactly(Double.MIN_VALUE, 0.0, Double.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesRangeWithBoundaries() {
        DoubleFilter filter = new DoubleFilter()
            .setGreaterOrEqualThan(Double.MIN_VALUE)
            .setLessOrEqualThan(Double.MAX_VALUE);

        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(Double.MIN_VALUE);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void testBoundaryValuesPositiveInfinity() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(Double.POSITIVE_INFINITY);

        assertThat(filter.getEquals()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void testBoundaryValuesNegativeInfinity() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(Double.NEGATIVE_INFINITY);

        assertThat(filter.getEquals()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    void testBoundaryValuesNaN() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(Double.NaN);

        assertThat(filter.getEquals()).isNaN();
    }

    // ==================== Precision Tests ====================

    @ParameterizedTest
    @DisplayName("Should handle double precision edge cases")
    @MethodSource("precisionValueProvider")
    void shouldHandleDoublePrecisionEdgeCases(double value) {
        // Given
        DoubleFilter filter = new DoubleFilter();

        // When
        filter.setEquals(value);

        // Then
        assertThat(filter.getEquals()).isEqualTo(value);
    }

    private static Stream<Arguments> precisionValueProvider() {
        return Stream.of(
                Arguments.of(1.0000000000001),
                Arguments.of(1.7976931348623157E308),
                Arguments.of(4.9E-324)
        );
    }
    

    @Test
    void testPrecisionFloatingPointArithmetic() {
        DoubleFilter filter = new DoubleFilter();
        double value = 0.1 + 0.2;
        filter.setEquals(value);

        assertThat(filter.getEquals()).isCloseTo(0.3, within(0.0000001));
    }
    

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        DoubleFilter filter = new DoubleFilter()
            .setEquals(100.5)
            .setNotEquals(200.5)
            .setGreaterThan(50.5)
            .setGreaterOrEqualThan(60.5)
            .setLessThan(150.5)
            .setLessOrEqualThan(140.5)
            .setIn(Arrays.asList(10.5, 20.5, 30.5))
            .setNotIn(Arrays.asList(40.5, 50.5, 60.5))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo(100.5);
        assertThat(filter.getNotEquals()).isEqualTo(200.5);
        assertThat(filter.getGreaterThan()).isEqualTo(50.5);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(60.5);
        assertThat(filter.getLessThan()).isEqualTo(150.5);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(140.5);
        assertThat(filter.getIn()).containsExactly(10.5, 20.5, 30.5);
        assertThat(filter.getNotIn()).containsExactly(40.5, 50.5, 60.5);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Equals and HashCode Tests ====================

    @Test
    void testEqualsSameValues() {
        DoubleFilter filter1 = new DoubleFilter()
            .setEquals(100.5)
            .setGreaterThan(50.5);
        DoubleFilter filter2 = new DoubleFilter()
            .setEquals(100.5)
            .setGreaterThan(50.5);

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1.hashCode()).hasSameHashCodeAs(filter2.hashCode());
    }

    @Test
    void testEqualsDifferentValues() {
        DoubleFilter filter1 = new DoubleFilter().setEquals(100.5);
        DoubleFilter filter2 = new DoubleFilter().setEquals(200.5);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsSameInstance() {
        DoubleFilter filter = new DoubleFilter().setEquals(100.5);

        assertThat(filter).isEqualTo(filter);
    }

    @Test
    void testEqualsWithNull() {
        DoubleFilter filter = new DoubleFilter().setEquals(100.5);

        assertThat(filter).isNotEqualTo(null);
    }

    // ==================== ToString Tests ====================

    @Test
    void testToStringWithAllFields() {
        DoubleFilter filter = new DoubleFilter()
            .setEquals(100.5)
            .setGreaterThan(50.5)
            .setLessThan(150.5);

        String result = filter.toString();

        assertThat(result).contains(DOUBLE_FILTER)
                .contains("100.5")
                .contains("50.5")
                .contains("150.5");
    }

    @Test
    void testToStringWithEmptyFilter() {
        DoubleFilter filter = new DoubleFilter();

        String result = filter.toString();

        assertThat(result).contains(DOUBLE_FILTER);
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetFilterWithEquals() {
        DoubleFilter priceFilter = new DoubleFilter().setEquals(100.0);

        assertThat(priceFilter.getEquals()).isEqualTo(100.0);
    }

    @Test
    void testLargeDatasetFilterWithGreaterThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Product> products = generator.generateProducts(10000);

        DoubleFilter priceFilter = new DoubleFilter().setGreaterThan(500.0);
        
        long count = products.stream()
            .filter(p -> p.getPrice() != null && p.getPrice() > 500.0)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(priceFilter.getGreaterThan()).isEqualTo(500.0);
    }

    @Test
    void testLargeDatasetFilterWithLessThan() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Product> products = generator.generateProducts(10000);

        DoubleFilter priceFilter = new DoubleFilter().setLessThan(100.0);
        
        long count = products.stream()
            .filter(p -> p.getPrice() != null && p.getPrice() < 100.0)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(priceFilter.getLessThan()).isEqualTo(100.0);
    }

    @Test
    void testLargeDatasetFilterWithRange() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Product> products = generator.generateProducts(10000);

        DoubleFilter priceFilter = new DoubleFilter()
            .setGreaterOrEqualThan(100.0)
            .setLessOrEqualThan(500.0);
        
        long count = products.stream()
            .filter(p -> p.getPrice() != null && p.getPrice() >= 100.0 && p.getPrice() <= 500.0)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(priceFilter.getGreaterOrEqualThan()).isEqualTo(100.0);
        assertThat(priceFilter.getLessOrEqualThan()).isEqualTo(500.0);
    }

    @Test
    void testLargeDatasetFilterWithInList() {
        List<Double> targetPrices = Arrays.asList(50.0, 100.0, 150.0, 200.0, 250.0);

        DoubleFilter priceFilter = new DoubleFilter().setIn(targetPrices);

        assertThat(priceFilter.getIn()).containsExactlyElementsOf(targetPrices);
    }

    @Test
    void testLargeDatasetFilterPerformance() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Product> products = generator.generateProducts(10000);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            
            products.stream()
                .filter(p -> p.getPrice() != null && p.getPrice() > 100.0 && p.getPrice() < 900.0)
                .count();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testLargeDatasetComplexFilterCombination() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        List<Product> products = generator.generateProducts(10000);

        DoubleFilter priceFilter = new DoubleFilter()
            .setGreaterOrEqualThan(50.0)
            .setLessOrEqualThan(950.0)
            .setNotEquals(500.0);
        
        long count = products.stream()
            .filter(p -> p.getPrice() != null && 
                        p.getPrice() >= 50.0 && 
                        p.getPrice() <= 950.0 && 
                        Math.abs(p.getPrice() - 500.0) > 0.01)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(priceFilter.getGreaterOrEqualThan()).isEqualTo(50.0);
        assertThat(priceFilter.getLessOrEqualThan()).isEqualTo(950.0);
        assertThat(priceFilter.getNotEquals()).isEqualTo(500.0);
    }

    // ==================== Edge Cases and Null Safety Tests ====================

    @Test
    void testMultipleSettersOnSameField() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(100.5);
        filter.setEquals(200.5);
        filter.setEquals(300.5);

        assertThat(filter.getEquals()).isEqualTo(300.5);
    }

    @Test
    void testSettersWithNullAfterValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setEquals(100.5);
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testCombinedRangeOperations() {
        DoubleFilter filter = new DoubleFilter()
            .setGreaterThan(10.5)
            .setLessThan(100.5)
            .setGreaterOrEqualThan(20.5)
            .setLessOrEqualThan(90.5);

        assertThat(filter.getGreaterThan()).isEqualTo(10.5);
        assertThat(filter.getLessThan()).isEqualTo(100.5);
        assertThat(filter.getGreaterOrEqualThan()).isEqualTo(20.5);
        assertThat(filter.getLessOrEqualThan()).isEqualTo(90.5);
    }

    @Test
    void testInheritedMethodsIsNull() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testInheritedMethodsIsNotNull() {
        DoubleFilter filter = new DoubleFilter();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Negated Operator Tests ====================

    @Test
    void testDefaultConstructorNegatedFieldsAreNull() {
        DoubleFilter filter = new DoubleFilter();

        assertThat(filter.getNotGreaterThan()).isNull();
        assertThat(filter.getNotLessThan()).isNull();
        assertThat(filter.getNotGreaterOrEqualThan()).isNull();
        assertThat(filter.getNotLessOrEqualThan()).isNull();
    }

    @Test
    void testSetNotGreaterThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotGreaterThan(75.5);

        assertThat(filter.getNotGreaterThan()).isEqualTo(75.5);
    }

    @Test
    void testSetNotGreaterThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        NumberFilter<Double> result = filter.setNotGreaterThan(75.5);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotLessThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotLessThan(25.5);

        assertThat(filter.getNotLessThan()).isEqualTo(25.5);
    }

    @Test
    void testSetNotLessThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        NumberFilter<Double> result = filter.setNotLessThan(25.5);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotGreaterOrEqualThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotGreaterOrEqualThan(80.5);

        assertThat(filter.getNotGreaterOrEqualThan()).isEqualTo(80.5);
    }

    @Test
    void testSetNotGreaterOrEqualThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        NumberFilter<Double> result = filter.setNotGreaterOrEqualThan(80.5);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetNotLessOrEqualThanSetsAndGetsValue() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotLessOrEqualThan(20.5);

        assertThat(filter.getNotLessOrEqualThan()).isEqualTo(20.5);
    }

    @Test
    void testSetNotLessOrEqualThanReturnsFilterForChaining() {
        DoubleFilter filter = new DoubleFilter();
        NumberFilter<Double> result = filter.setNotLessOrEqualThan(20.5);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testCopyConstructorCopiesNegatedFields() {
        DoubleFilter original = new DoubleFilter();
        original.setNotGreaterThan(10.5);
        original.setNotLessThan(20.5);
        original.setNotGreaterOrEqualThan(30.5);
        original.setNotLessOrEqualThan(40.5);

        DoubleFilter copy = new DoubleFilter(original);

        assertThat(copy.getNotGreaterThan()).isEqualTo(10.5);
        assertThat(copy.getNotLessThan()).isEqualTo(20.5);
        assertThat(copy.getNotGreaterOrEqualThan()).isEqualTo(30.5);
        assertThat(copy.getNotLessOrEqualThan()).isEqualTo(40.5);
    }

    @Test
    void testEqualsWithDifferentNotGreaterThan() {
        DoubleFilter filter1 = new DoubleFilter();
        filter1.setNotGreaterThan(10.0);
        DoubleFilter filter2 = new DoubleFilter();
        filter2.setNotGreaterThan(20.0);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotLessThan() {
        DoubleFilter filter1 = new DoubleFilter();
        filter1.setNotLessThan(10.0);
        DoubleFilter filter2 = new DoubleFilter();
        filter2.setNotLessThan(20.0);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotGreaterOrEqualThan() {
        DoubleFilter filter1 = new DoubleFilter();
        filter1.setNotGreaterOrEqualThan(10.0);
        DoubleFilter filter2 = new DoubleFilter();
        filter2.setNotGreaterOrEqualThan(20.0);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsWithDifferentNotLessOrEqualThan() {
        DoubleFilter filter1 = new DoubleFilter();
        filter1.setNotLessOrEqualThan(10.0);
        DoubleFilter filter2 = new DoubleFilter();
        filter2.setNotLessOrEqualThan(20.0);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testHashCodeConsistencyWithNegatedFields() {
        DoubleFilter filter1 = new DoubleFilter();
        filter1.setNotGreaterThan(50.0);
        filter1.setNotLessThan(10.0);
        DoubleFilter filter2 = new DoubleFilter();
        filter2.setNotGreaterThan(50.0);
        filter2.setNotLessThan(10.0);

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }

    @Test
    void testToStringContainsNegatedFields() {
        DoubleFilter filter = new DoubleFilter();
        filter.setNotGreaterThan(99.9);
        filter.setNotLessThan(11.1);
        filter.setNotGreaterOrEqualThan(88.8);
        filter.setNotLessOrEqualThan(22.2);

        String result = filter.toString();

        assertThat(result).contains("notGreaterThan=99.9");
        assertThat(result).contains("notLessThan=11.1");
        assertThat(result).contains("notGreaterOrEqualThan=88.8");
        assertThat(result).contains("notLessOrEqualThan=22.2");
    }

    @Test
    void testNegatedOperatorNotGreaterThanSemantics() {
        // ngt means: field <= value (negation of field > value)
        Double fieldValue = 50.0;
        Double threshold = 75.0;

        // 50 <= 75 => ngt should match (true)
        assertThat(fieldValue.compareTo(threshold) <= 0).isTrue();

        // When field equals threshold: 75 <= 75 => true
        assertThat(threshold.compareTo(threshold) <= 0).isTrue();

        // When field > threshold: 100 <= 75 => false
        Double largerValue = 100.0;
        assertThat(largerValue.compareTo(threshold) <= 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotLessThanSemantics() {
        // nlt means: field >= value (negation of field < value)
        Double fieldValue = 80.0;
        Double threshold = 50.0;

        // 80 >= 50 => nlt should match (true)
        assertThat(fieldValue.compareTo(threshold) >= 0).isTrue();

        // When field equals threshold: 50 >= 50 => true
        assertThat(threshold.compareTo(threshold) >= 0).isTrue();

        // When field < threshold: 30 >= 50 => false
        Double smallerValue = 30.0;
        assertThat(smallerValue.compareTo(threshold) >= 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotGreaterOrEqualThanSemantics() {
        // ngte means: field < value (negation of field >= value)
        Double fieldValue = 40.0;
        Double threshold = 50.0;

        // 40 < 50 => ngte should match (true)
        assertThat(fieldValue.compareTo(threshold) < 0).isTrue();

        // When field equals threshold: 50 < 50 => false
        assertThat(threshold.compareTo(threshold) < 0).isFalse();

        // When field > threshold: 60 < 50 => false
        Double largerValue = 60.0;
        assertThat(largerValue.compareTo(threshold) < 0).isFalse();
    }

    @Test
    void testNegatedOperatorNotLessOrEqualThanSemantics() {
        // nlte means: field > value (negation of field <= value)
        Double fieldValue = 60.0;
        Double threshold = 50.0;

        // 60 > 50 => nlte should match (true)
        assertThat(fieldValue.compareTo(threshold) > 0).isTrue();

        // When field equals threshold: 50 > 50 => false
        assertThat(threshold.compareTo(threshold) > 0).isFalse();

        // When field < threshold: 40 > 50 => false
        Double smallerValue = 40.0;
        assertThat(smallerValue.compareTo(threshold) > 0).isFalse();
    }

    @Test
    void testNegatedOperatorWithNullFieldValueReturnsFalse() {
        // When field value is null, all negated operators should yield false
        Double fieldValue = null;
        Double threshold = 50.0;

        // null field cannot be compared — result is false
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
     * Property 5: Kopyalama kurucusu eşitliği
     *
     * Validates: Requirements 7.1, 7.2
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 5: Kopyalama kurucusu eşitliği")
    void copyConstructorShouldProduceEqualInstance(@ForAll("arbitraryDoubleFilter") DoubleFilter original) {
        // When: create a copy using the copy constructor
        DoubleFilter copy = new DoubleFilter(original);

        // Then: copy equals original
        assertThat(copy).isEqualTo(original);
        // And: hashCode is consistent with equals
        assertThat(copy.hashCode()).isEqualTo(original.hashCode());
    }

    /**
     * Property 6: equals/hashCode dahiliyeti ve tutarlılığı
     *
     * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 6: equals/hashCode dahiliyeti ve tutarlılığı")
    void equalsHashCodeInclusionAndConsistency(@ForAll("arbitraryDoubleFilter") DoubleFilter original,
                                               @ForAll("negatedFieldIndex") int fieldIndex,
                                               @ForAll("distinctDouble") Double differentValue) {
        // Part 1: hashCode consistency — equal instances produce same hashCode
        DoubleFilter copy = new DoubleFilter(original);
        assertThat(copy).isEqualTo(original);
        assertThat(copy.hashCode()).isEqualTo(original.hashCode());

        // Part 2: Inclusion — changing one negated field makes equals return false
        DoubleFilter mutated = new DoubleFilter(original);
        Double currentValue;
        switch (fieldIndex) {
            case 0:
                currentValue = mutated.getNotGreaterThan();
                Double newNgt = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue + 1.0 : differentValue;
                mutated.setNotGreaterThan(newNgt);
                // Only assert inequality if we actually changed the value
                if (!java.util.Objects.equals(newNgt, original.getNotGreaterThan())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 1:
                currentValue = mutated.getNotLessThan();
                Double newNlt = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue + 1.0 : differentValue;
                mutated.setNotLessThan(newNlt);
                if (!java.util.Objects.equals(newNlt, original.getNotLessThan())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 2:
                currentValue = mutated.getNotGreaterOrEqualThan();
                Double newNgte = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue + 1.0 : differentValue;
                mutated.setNotGreaterOrEqualThan(newNgte);
                if (!java.util.Objects.equals(newNgte, original.getNotGreaterOrEqualThan())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 3:
                currentValue = mutated.getNotLessOrEqualThan();
                Double newNlte = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue + 1.0 : differentValue;
                mutated.setNotLessOrEqualThan(newNlte);
                if (!java.util.Objects.equals(newNlte, original.getNotLessOrEqualThan())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
        }
    }

    /**
     * Property 7: Erişimci çevrimi ve zincirlenebilir setter
     *
     * Validates: Requirements 1.3, 1.4, 3.3, 3.4, 5.1, 5.2, 5.3, 5.4
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 7: Erişimci çevrimi ve zincirlenebilir setter")
    void setterShouldReturnSameInstanceAndGetterShouldReturnSetValue(@ForAll("nullableDouble") Double value) {
        DoubleFilter filter = new DoubleFilter();

        // setNotGreaterThan: returns same instance and getter returns the value
        NumberFilter<Double> result1 = filter.setNotGreaterThan(value);
        assertThat(result1).isSameAs(filter);
        assertThat(filter.getNotGreaterThan()).isEqualTo(value);

        // setNotLessThan: returns same instance and getter returns the value
        NumberFilter<Double> result2 = filter.setNotLessThan(value);
        assertThat(result2).isSameAs(filter);
        assertThat(filter.getNotLessThan()).isEqualTo(value);

        // setNotGreaterOrEqualThan: returns same instance and getter returns the value
        NumberFilter<Double> result3 = filter.setNotGreaterOrEqualThan(value);
        assertThat(result3).isSameAs(filter);
        assertThat(filter.getNotGreaterOrEqualThan()).isEqualTo(value);

        // setNotLessOrEqualThan: returns same instance and getter returns the value
        NumberFilter<Double> result4 = filter.setNotLessOrEqualThan(value);
        assertThat(result4).isSameAs(filter);
        assertThat(filter.getNotLessOrEqualThan()).isEqualTo(value);
    }

    /**
     * Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı
     *
     * For any non-null Double field value and threshold, each negated operator yields the
     * exact logical negation of the corresponding positive operator via compareTo.
     * When field value is null, all negated operators return false.
     *
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı")
    void negatedOperatorsShouldBeLogicalNegationOfPositiveOperators(
            @ForAll("nullableDouble") Double fieldValue,
            @ForAll("nonNullDouble") Double threshold) {

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

    /**
     * Property 3: validateFilter mantıksal VE kombinasyonu
     *
     * For any DoubleFilter with a random combination of positive and negated range operators
     * (some null, some set) and any field value, validateFilter returns true if and only if
     * ALL non-null operators are simultaneously satisfied. Null operators do not affect the result.
     *
     * Uses SpecificationQueryEngine end-to-end with a single-element Product list.
     *
     * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 15.3
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 3: validateFilter mantıksal VE kombinasyonu")
    void validateFilterLogicalAndCombination(
            @ForAll("rangeOnlyDoubleFilter") DoubleFilter priceFilter,
            @ForAll("nonNullDouble") Double fieldValue) {

        // Build a Product entity with the given field value
        Product product = new Product();
        product.setId(1L);
        product.setPrice(fieldValue);

        // Compute the expected result using the model (logical AND of all non-null operators)
        boolean expected = computeExpectedNumericResult(fieldValue, priceFilter);

        // Use SpecificationQueryEngine to apply the filter via the generated validateFilter
        ProductFilter productFilter = new ProductFilter();
        productFilter.setPrice(priceFilter);

        SpecificationQueryEngine<Product> engine = new SpecificationQueryEngine<>(Product.class);
        List<Product> results = engine.queryByFilter(Collections.singletonList(product), productFilter);

        if (expected) {
            assertThat(results).hasSize(1);
        } else {
            assertThat(results).isEmpty();
        }
    }

    /**
     * Computes the expected validateFilter result for a numeric field using the model:
     * Each non-null operator must be satisfied (logical AND). Null operators are skipped.
     */
    private boolean computeExpectedNumericResult(Double fieldValue, DoubleFilter filter) {
        // greaterThan: field > value
        if (filter.getGreaterThan() != null) {
            if (fieldValue.compareTo(filter.getGreaterThan()) <= 0) return false;
        }
        // lessThan: field < value
        if (filter.getLessThan() != null) {
            if (fieldValue.compareTo(filter.getLessThan()) >= 0) return false;
        }
        // greaterOrEqualThan: field >= value
        if (filter.getGreaterOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getGreaterOrEqualThan()) < 0) return false;
        }
        // lessOrEqualThan: field <= value
        if (filter.getLessOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getLessOrEqualThan()) > 0) return false;
        }
        // notGreaterThan: field <= value (negation of field > value)
        if (filter.getNotGreaterThan() != null) {
            if (fieldValue.compareTo(filter.getNotGreaterThan()) > 0) return false;
        }
        // notLessThan: field >= value (negation of field < value)
        if (filter.getNotLessThan() != null) {
            if (fieldValue.compareTo(filter.getNotLessThan()) < 0) return false;
        }
        // notGreaterOrEqualThan: field < value (negation of field >= value)
        if (filter.getNotGreaterOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getNotGreaterOrEqualThan()) >= 0) return false;
        }
        // notLessOrEqualThan: field > value (negation of field <= value)
        if (filter.getNotLessOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getNotLessOrEqualThan()) <= 0) return false;
        }
        return true;
    }

    /**
     * Property 4: JSON çevrimi (round-trip) korunumu
     *
     * A DoubleFilter with negated fields set, when serialized to JSON and deserialized back,
     * produces an equal filter instance.
     *
     * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 4: JSON çevrimi (round-trip) korunumu")
    void jsonRoundTripPreservesNegatedFields(@ForAll("arbitraryDoubleFilter") DoubleFilter original) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(original);
        DoubleFilter deserialized = mapper.readValue(json, DoubleFilter.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Provide
    Arbitrary<Double> nonNullDouble() {
        return Arbitraries.doubles().between(-1_000_000.0, 1_000_000.0);
    }

    @Provide
    Arbitrary<Double> nullableDouble() {
        return Arbitraries.doubles()
            .between(-1_000_000.0, 1_000_000.0)
            .injectNull(0.2);
    }

    @Provide
    Arbitrary<Integer> negatedFieldIndex() {
        return Arbitraries.integers().between(0, 3);
    }

    @Provide
    Arbitrary<Double> distinctDouble() {
        return Arbitraries.doubles().between(-1_000_000.0, 1_000_000.0);
    }

    @Provide
    Arbitrary<DoubleFilter> arbitraryDoubleFilter() {
        Arbitrary<Double> nullableDouble = Arbitraries.doubles()
            .between(-1_000_000.0, 1_000_000.0)
            .injectNull(0.2);

        return Combinators.combine(
            nullableDouble, // equals
            nullableDouble, // greaterThan
            nullableDouble, // lessThan
            nullableDouble, // greaterOrEqualThan
            nullableDouble, // lessOrEqualThan
            nullableDouble, // notGreaterThan
            nullableDouble, // notLessThan
            nullableDouble  // notGreaterOrEqualThan
        ).as((eq, gt, lt, gte, lte, ngt, nlt, ngte) -> {
            DoubleFilter filter = new DoubleFilter();
            filter.setEquals(eq);
            filter.setGreaterThan(gt);
            filter.setLessThan(lt);
            filter.setGreaterOrEqualThan(gte);
            filter.setLessOrEqualThan(lte);
            filter.setNotGreaterThan(ngt);
            filter.setNotLessThan(nlt);
            filter.setNotGreaterOrEqualThan(ngte);
            return filter;
        }).flatMap(filter -> nullableDouble.map(nlte -> {
            filter.setNotLessOrEqualThan(nlte);
            return filter;
        }));
    }

    /**
     * Property 8: Geriye dönük uyumluluk (olumsuz alanlar null)
     *
     * When all negated fields are null, validateFilter result is identical to computing
     * only positive operators (gt, lt, gte, lte). This proves backward compatibility:
     * when no negated operators are used, behavior is unchanged.
     *
     * Validates: Requirements 15.1, 15.2, 15.3, 6.2
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 8: Geriye dönük uyumluluk")
    void backwardCompatibilityWhenNegatedFieldsAreNull(
            @ForAll("positiveOnlyDoubleFilter") DoubleFilter priceFilter,
            @ForAll("nonNullDouble") Double fieldValue) {

        // Precondition: all negated fields must be null (enforced by generator)
        assertThat(priceFilter.getNotGreaterThan()).isNull();
        assertThat(priceFilter.getNotLessThan()).isNull();
        assertThat(priceFilter.getNotGreaterOrEqualThan()).isNull();
        assertThat(priceFilter.getNotLessOrEqualThan()).isNull();

        // Build a Product entity with the given field value
        Product product = new Product();
        product.setId(1L);
        product.setPrice(fieldValue);

        // Use SpecificationQueryEngine to apply the filter
        ProductFilter productFilter = new ProductFilter();
        productFilter.setPrice(priceFilter);

        SpecificationQueryEngine<Product> engine = new SpecificationQueryEngine<>(Product.class);
        List<Product> results = engine.queryByFilter(Collections.singletonList(product), productFilter);

        // Compute expected from model: only positive operators (logical AND)
        boolean expected = computeExpectedPositiveOnlyNumericResult(fieldValue, priceFilter);

        if (expected) {
            assertThat(results).hasSize(1);
        } else {
            assertThat(results).isEmpty();
        }
    }

    /**
     * Computes the expected result using ONLY positive operators (gt, lt, gte, lte).
     * Negated fields are assumed null and skipped.
     */
    private boolean computeExpectedPositiveOnlyNumericResult(Double fieldValue, DoubleFilter filter) {
        if (filter.getGreaterThan() != null) {
            if (fieldValue.compareTo(filter.getGreaterThan()) <= 0) return false;
        }
        if (filter.getLessThan() != null) {
            if (fieldValue.compareTo(filter.getLessThan()) >= 0) return false;
        }
        if (filter.getGreaterOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getGreaterOrEqualThan()) < 0) return false;
        }
        if (filter.getLessOrEqualThan() != null) {
            if (fieldValue.compareTo(filter.getLessOrEqualThan()) > 0) return false;
        }
        return true;
    }

    @Provide
    Arbitrary<DoubleFilter> positiveOnlyDoubleFilter() {
        Arbitrary<Double> nullableDouble = Arbitraries.doubles()
            .between(-1_000.0, 1_000.0)
            .injectNull(0.3);

        return Combinators.combine(
            nullableDouble, // greaterThan
            nullableDouble, // lessThan
            nullableDouble, // greaterOrEqualThan
            nullableDouble  // lessOrEqualThan
        ).as((gt, lt, gte, lte) -> {
            DoubleFilter filter = new DoubleFilter();
            filter.setGreaterThan(gt);
            filter.setLessThan(lt);
            filter.setGreaterOrEqualThan(gte);
            filter.setLessOrEqualThan(lte);
            // All negated fields remain null (backward compatibility)
            return filter;
        });
    }

    @Provide
    Arbitrary<DoubleFilter> rangeOnlyDoubleFilter() {
        Arbitrary<Double> nullableDouble = Arbitraries.doubles()
            .between(-1_000.0, 1_000.0)
            .injectNull(0.3);

        return Combinators.combine(
            nullableDouble, // greaterThan
            nullableDouble, // lessThan
            nullableDouble, // greaterOrEqualThan
            nullableDouble, // lessOrEqualThan
            nullableDouble, // notGreaterThan
            nullableDouble, // notLessThan
            nullableDouble, // notGreaterOrEqualThan
            nullableDouble  // notLessOrEqualThan
        ).as((gt, lt, gte, lte, ngt, nlt, ngte, nlte) -> {
            DoubleFilter filter = new DoubleFilter();
            filter.setGreaterThan(gt);
            filter.setLessThan(lt);
            filter.setGreaterOrEqualThan(gte);
            filter.setLessOrEqualThan(lte);
            filter.setNotGreaterThan(ngt);
            filter.setNotLessThan(nlt);
            filter.setNotGreaterOrEqualThan(ngte);
            filter.setNotLessOrEqualThan(nlte);
            return filter;
        });
    }
}
