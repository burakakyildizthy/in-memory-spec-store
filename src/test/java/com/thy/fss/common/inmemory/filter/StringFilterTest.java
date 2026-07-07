package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for StringFilter operations.
 * Tests all string-specific filtering operations including getters, setters, and utility methods.
 * Requirements: 4.1, 15.9
 */
class StringFilterTest {

    private static final String TEST_VALUE = "test";
    private static final String PREFIX_VALUE = "prefix";
    private static final String CONTAINS_VALUE = "contains";
    private static final String SUFFIX_VALUE = "suffix";
    private static final String SUBSTRING_VALUE = "substring";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        StringFilter filter = new StringFilter();

        assertThat(filter)
            .returns(null, StringFilter::getEquals)
            .returns(null, StringFilter::getNotEquals)
            .returns(null, StringFilter::getContains)
            .returns(null, StringFilter::getStartsWith)
            .returns(null, StringFilter::getEndsWith)
            .returns(null, StringFilter::getMatches)
            .returns(null, StringFilter::getIsEmpty)
            .returns(null, StringFilter::getIsNotEmpty)
            .returns(null, StringFilter::getIsBlank)
            .returns(null, StringFilter::getIsNotBlank)
            .returns(null, StringFilter::getIn)
            .returns(null, StringFilter::getNotIn);
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        StringFilter original = new StringFilter()
            .setEquals("testEquals")
            .setNotEquals("testNotEquals")
            .setContains("testContains")
            .setStartsWith("testStarts")
            .setEndsWith("testEnds")
            .setMatches("test.*")
            .setIsEmpty(true)
            .setIsNotEmpty(false)
            .setIsBlank(true)
            .setIsNotBlank(false)
            .setIn(Arrays.asList("val1", "val2"))
            .setNotIn(Arrays.asList("val3", "val4"));

        StringFilter copy = new StringFilter(original);

        assertThat(copy.getEquals()).isEqualTo("testEquals");
        assertThat(copy.getNotEquals()).isEqualTo("testNotEquals");
        assertThat(copy.getContains()).isEqualTo("testContains");
        assertThat(copy.getStartsWith()).isEqualTo("testStarts");
        assertThat(copy.getEndsWith()).isEqualTo("testEnds");
        assertThat(copy.getMatches()).isEqualTo("test.*");
        assertThat(copy.getIsEmpty()).isTrue();
        assertThat(copy.getIsNotEmpty()).isFalse();
        assertThat(copy.getIsBlank()).isTrue();
        assertThat(copy.getIsNotBlank()).isFalse();
        assertThat(copy.getIn()).containsExactly("val1", "val2");
        assertThat(copy.getNotIn()).containsExactly("val3", "val4");
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        StringFilter original = new StringFilter().setEquals("original");
        StringFilter copy = new StringFilter(original);

        copy.setEquals("modified");

        assertThat(original.getEquals()).isEqualTo("original");
        assertThat(copy.getEquals()).isEqualTo("modified");
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setEquals("testValue");

        assertThat(filter.getEquals()).isEqualTo("testValue");
    }

    @Test
    void testSetEqualsWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setEquals(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetEqualsWithEmptyString() {
        StringFilter filter = new StringFilter();
        filter.setEquals("");

        assertThat(filter.getEquals()).isEmpty();
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setNotEquals("excludeValue");

        assertThat(filter.getNotEquals()).isEqualTo("excludeValue");
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setNotEquals(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setContains Tests ====================

    @Test
    void testSetContainsSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setContains(SUBSTRING_VALUE);

        assertThat(filter.getContains()).isEqualTo(SUBSTRING_VALUE);
    }

    @Test
    void testSetContainsWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setContains(null);

        assertThat(filter.getContains()).isNull();
    }

    @Test
    void testSetContainsWithEmptyString() {
        StringFilter filter = new StringFilter();
        filter.setContains("");

        assertThat(filter.getContains()).isEmpty();
    }

    @Test
    void testSetContainsReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setContains(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setStartsWith Tests ====================

    @Test
    void testSetStartsWithSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setStartsWith(PREFIX_VALUE);

        assertThat(filter.getStartsWith()).isEqualTo(PREFIX_VALUE);
    }

    @Test
    void testSetStartsWithWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setStartsWith(null);

        assertThat(filter.getStartsWith()).isNull();
    }

    @Test
    void testSetStartsWithWithEmptyString() {
        StringFilter filter = new StringFilter();
        filter.setStartsWith("");

        assertThat(filter.getStartsWith()).isEmpty();
    }

    @Test
    void testSetStartsWithReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setStartsWith(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setEndsWith Tests ====================

    @Test
    void testSetEndsWithSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setEndsWith(SUFFIX_VALUE);

        assertThat(filter.getEndsWith()).isEqualTo(SUFFIX_VALUE);
    }

    @Test
    void testEndsWithWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setEndsWith(null);

        assertThat(filter.getEndsWith()).isNull();
    }

    @Test
    void testSetEndsWithWithEmptyString() {
        StringFilter filter = new StringFilter();
        filter.setEndsWith("");

        assertThat(filter.getEndsWith()).isEmpty();
    }

    @Test
    void testSetEndsWithReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setEndsWith(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setMatches Tests ====================

    @Test
    void testSetMatchesSetsAndGetsValue() {
        StringFilter filter = new StringFilter();
        filter.setMatches(".*pattern.*");

        assertThat(filter.getMatches()).isEqualTo(".*pattern.*");
    }

    @Test
    void testSetMatchesWithNullValue() {
        StringFilter filter = new StringFilter();
        filter.setMatches(null);

        assertThat(filter.getMatches()).isNull();
    }

    @Test
    void testSetMatchesWithComplexRegex() {
        StringFilter filter = new StringFilter();
        filter.setMatches("^[A-Z][a-z]+$");

        assertThat(filter.getMatches()).isEqualTo("^[A-Z][a-z]+$");
    }

    @Test
    void testSetMatchesReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setMatches(TEST_VALUE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsEmpty Tests ====================

    @Test
    void testSetIsEmptyWithTrue() {
        StringFilter filter = new StringFilter();
        filter.setIsEmpty(true);

        assertThat(filter.getIsEmpty()).isTrue();
    }

    @Test
    void testSetIsEmptyWithFalse() {
        StringFilter filter = new StringFilter();
        filter.setIsEmpty(false);

        assertThat(filter.getIsEmpty()).isFalse();
    }

    @Test
    void testSetIsEmptyWithNull() {
        StringFilter filter = new StringFilter();
        filter.setIsEmpty(null);

        assertThat(filter.getIsEmpty()).isNull();
    }

    @Test
    void testSetIsEmptyReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setIsEmpty(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNotEmpty Tests ====================

    @Test
    void testSetIsNotEmptyWithTrue() {
        StringFilter filter = new StringFilter();
        filter.setIsNotEmpty(true);

        assertThat(filter.getIsNotEmpty()).isTrue();
    }

    @Test
    void testSetIsNotEmptyWithFalse() {
        StringFilter filter = new StringFilter();
        filter.setIsNotEmpty(false);

        assertThat(filter.getIsNotEmpty()).isFalse();
    }

    @Test
    void testSetIsNotEmptyWithNull() {
        StringFilter filter = new StringFilter();
        filter.setIsNotEmpty(null);

        assertThat(filter.getIsNotEmpty()).isNull();
    }

    @Test
    void testSetIsNotEmptyReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setIsNotEmpty(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsBlank Tests ====================

    @Test
    void testSetIsBlankWithTrue() {
        StringFilter filter = new StringFilter();
        filter.setIsBlank(true);

        assertThat(filter.getIsBlank()).isTrue();
    }

    @Test
    void testSetIsBlankWithFalse() {
        StringFilter filter = new StringFilter();
        filter.setIsBlank(false);

        assertThat(filter.getIsBlank()).isFalse();
    }

    @Test
    void testSetIsBlankWithNull() {
        StringFilter filter = new StringFilter();
        filter.setIsBlank(null);

        assertThat(filter.getIsBlank()).isNull();
    }

    @Test
    void testSetIsBlankReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setIsBlank(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNotBlank Tests ====================

    @Test
    void testSetIsNotBlankWithTrue() {
        StringFilter filter = new StringFilter();
        filter.setIsNotBlank(true);

        assertThat(filter.getIsNotBlank()).isTrue();
    }

    @Test
    void testSetIsNotBlankWithFalse() {
        StringFilter filter = new StringFilter();
        filter.setIsNotBlank(false);

        assertThat(filter.getIsNotBlank()).isFalse();
    }

    @Test
    void testSetIsNotBlankWithNull() {
        StringFilter filter = new StringFilter();
        filter.setIsNotBlank(null);

        assertThat(filter.getIsNotBlank()).isNull();
    }

    @Test
    void testSetIsNotBlankReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setIsNotBlank(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithList() {
        StringFilter filter = new StringFilter();
        List<String> values = Arrays.asList("value1", "value2", "value3");
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly("value1", "value2", "value3");
    }

    @Test
    void testSetInWithEmptyList() {
        StringFilter filter = new StringFilter();
        filter.setIn(List.of());

        assertThat(filter.getIn()).isEmpty();
    }

    @Test
    void testSetInWithNull() {
        StringFilter filter = new StringFilter();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setIn(Arrays.asList(TEST_VALUE));

        assertThat(result).isSameAs(filter);
    }

    @Test
    void testSetInWithLargeList() {
        StringFilter filter = new StringFilter();
        List<String> largeList = Arrays.asList(
            "val1", "val2", "val3", "val4", "val5",
            "val6", "val7", "val8", "val9", "val10"
        );
        filter.setIn(largeList);

        assertThat(filter.getIn()).hasSize(10);
        assertThat(filter.getIn()).containsAll(largeList);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithList() {
        StringFilter filter = new StringFilter();
        List<String> values = Arrays.asList("exclude1", "exclude2");
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly("exclude1", "exclude2");
    }

    @Test
    void testSetNotInWithEmptyList() {
        StringFilter filter = new StringFilter();
        filter.setNotIn(List.of());

        assertThat(filter.getNotIn()).isEmpty();
    }

    @Test
    void testSetNotInWithNull() {
        StringFilter filter = new StringFilter();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        StringFilter filter = new StringFilter();
        StringFilter result = filter.setNotIn(Arrays.asList(TEST_VALUE));

        assertThat(result).isSameAs(filter);
    }

    // ==================== Inherited Methods Tests ====================

    @Test
    void testSetIsNullFromBaseFilter() {
        StringFilter filter = new StringFilter();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testSetIsNotNullFromBaseFilter() {
        StringFilter filter = new StringFilter();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        StringFilter filter = new StringFilter()
            .setEquals("equals")
            .setNotEquals("notEquals")
            .setContains(CONTAINS_VALUE)
            .setStartsWith("starts")
            .setEndsWith("ends")
            .setMatches(".*")
            .setIsEmpty(false)
            .setIsNotEmpty(true)
            .setIsBlank(false)
            .setIsNotBlank(true)
            .setIn(Arrays.asList("in1", "in2"))
            .setNotIn(Arrays.asList("out1", "out2"))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo("equals");
        assertThat(filter.getNotEquals()).isEqualTo("notEquals");
        assertThat(filter.getContains()).isEqualTo(CONTAINS_VALUE);
        assertThat(filter.getStartsWith()).isEqualTo("starts");
        assertThat(filter.getEndsWith()).isEqualTo("ends");
        assertThat(filter.getMatches()).isEqualTo(".*");
        assertThat(filter.getIsEmpty()).isFalse();
        assertThat(filter.getIsNotEmpty()).isTrue();
        assertThat(filter.getIsBlank()).isFalse();
        assertThat(filter.getIsNotBlank()).isTrue();
        assertThat(filter.getIn()).containsExactly("in1", "in2");
        assertThat(filter.getNotIn()).containsExactly("out1", "out2");
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Equals and HashCode Tests ====================

    @Test
    void testEqualsSameValues() {
        StringFilter filter1 = new StringFilter()
            .setEquals(TEST_VALUE)
            .setContains(CONTAINS_VALUE);
        StringFilter filter2 = new StringFilter()
            .setEquals(TEST_VALUE)
            .setContains(CONTAINS_VALUE);

        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2);
    }

    @Test
    void testEqualsDifferentValues() {
        StringFilter filter1 = new StringFilter().setEquals("test1");
        StringFilter filter2 = new StringFilter().setEquals("test2");

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void testEqualsSameInstance() {
        StringFilter filter = new StringFilter().setEquals(TEST_VALUE);

        assertThat(filter).isEqualTo(filter);
    }

    @Test
    void testEqualsWithNull() {
        StringFilter filter = new StringFilter().setEquals(TEST_VALUE);

        assertThat(filter).isNotEqualTo(null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        StringFilter filter = new StringFilter().setEquals(TEST_VALUE);
        String other = "test";

        assertThat(filter).isNotEqualTo(other);
    }

    // ==================== ToString Tests ====================

    @Test
    void testToStringWithAllFields() {
        StringFilter filter = new StringFilter()
            .setEquals("equals")
            .setContains(CONTAINS_VALUE)
            .setStartsWith("starts");

        String result = filter.toString();

        assertThat(result).contains("StringFilter").contains("equals").contains(CONTAINS_VALUE).contains("starts");
    }

    @Test
    void testToStringWithEmptyFilter() {
        StringFilter filter = new StringFilter();

        String result = filter.toString();

        assertThat(result).contains("StringFilter");
    }

    // ==================== Edge Cases and Null Safety Tests ====================

    @Test
    void testMultipleSettersOnSameField() {
        StringFilter filter = new StringFilter();
        filter.setEquals("first");
        filter.setEquals("second");
        filter.setEquals("third");

        assertThat(filter.getEquals()).isEqualTo("third");
    }

    @Test
    void testSettersWithNullAfterValue() {
        StringFilter filter = new StringFilter();
        filter.setEquals("value");
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testCombinedOperationsContainsAndStartsWith() {
        StringFilter filter = new StringFilter()
            .setContains(TEST_VALUE)
            .setStartsWith(PREFIX_VALUE);

        assertThat(filter.getContains()).isEqualTo(TEST_VALUE);
        assertThat(filter.getStartsWith()).isEqualTo(PREFIX_VALUE);
    }

    @Test
    void testCombinedOperationsAllStringOperations() {
        StringFilter filter = new StringFilter()
            .setEquals("exact")
            .setNotEquals("notThis")
            .setContains(SUBSTRING_VALUE)
            .setStartsWith(PREFIX_VALUE)
            .setEndsWith(SUFFIX_VALUE)
            .setMatches(".*pattern.*")
            .setIsEmpty(false)
            .setIsNotEmpty(true)
            .setIsBlank(false)
            .setIsNotBlank(true);

        assertThat(filter.getEquals()).isEqualTo("exact");
        assertThat(filter.getNotEquals()).isEqualTo("notThis");
        assertThat(filter.getContains()).isEqualTo(SUBSTRING_VALUE);
        assertThat(filter.getStartsWith()).isEqualTo(PREFIX_VALUE);
        assertThat(filter.getEndsWith()).isEqualTo(SUFFIX_VALUE);
        assertThat(filter.getMatches()).isEqualTo(".*pattern.*");
        assertThat(filter.getIsEmpty()).isFalse();
        assertThat(filter.getIsNotEmpty()).isTrue();
        assertThat(filter.getIsBlank()).isFalse();
        assertThat(filter.getIsNotBlank()).isTrue();
    }

    // ==================== Large Dataset Simulation Tests ====================

    @Test
    void testFilterCreationWithLargeInList() {
        List<String> largeList = Arrays.asList(
            "User0", "User1", "User2", "User3", "User4", "User5", "User6", "User7", "User8", "User9",
            "User10", "User11", "User12", "User13", "User14", "User15", "User16", "User17", "User18", "User19"
        );

        StringFilter filter = new StringFilter().setIn(largeList);

        assertThat(filter.getIn()).hasSize(20);
        assertThat(filter.getIn()).containsAll(largeList);
    }

    @Test
    void testFilterPerformanceMultipleChainedOperations() {
        long startTime = System.currentTimeMillis();

        StringFilter filter = null;
        for (int i = 0; i < 10000; i++) {
            filter = new StringFilter()
                .setEquals(TEST_VALUE + i)
                .setContains(SUBSTRING_VALUE)
                .setStartsWith(PREFIX_VALUE)
                .setEndsWith(SUFFIX_VALUE);
        }

        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(1000);
        Assertions.assertNotNull(filter);

    }

    @Test
    void testCopyConstructorWithLargeInList() {
        List<String> largeList = Arrays.asList(
            "val1", "val2", "val3", "val4", "val5", "val6", "val7", "val8", "val9", "val10"
        );
        StringFilter original = new StringFilter().setIn(largeList);

        StringFilter copy = new StringFilter(original);

        assertThat(copy.getIn()).hasSize(10);
        assertThat(copy.getIn()).containsAll(largeList);
    }
}
