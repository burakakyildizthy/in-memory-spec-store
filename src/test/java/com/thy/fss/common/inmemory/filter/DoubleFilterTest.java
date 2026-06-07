package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
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
}
