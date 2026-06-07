package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FilterConstants to achieve 80%+ line coverage.
 * Verifies all constant values and the private constructor.
 */
@DisplayName("FilterConstants Tests")
class FilterConstantsTest {

    @Test
    @DisplayName("Should have correct common filter field names")
    void shouldHaveCorrectCommonFilterFieldNames() {
        assertThat(FilterConstants.FIELD_EQ).isEqualTo("eq");
        assertThat(FilterConstants.FIELD_NEQ).isEqualTo("neq");
        assertThat(FilterConstants.FIELD_IN).isEqualTo("in");
        assertThat(FilterConstants.FIELD_NIN).isEqualTo("nin");
        assertThat(FilterConstants.FIELD_ISN).isEqualTo("isn");
        assertThat(FilterConstants.FIELD_ISNN).isEqualTo("isnn");
    }

    @Test
    @DisplayName("Should have correct range filter field names")
    void shouldHaveCorrectRangeFilterFieldNames() {
        assertThat(FilterConstants.FIELD_GT).isEqualTo("gt");
        assertThat(FilterConstants.FIELD_GTE).isEqualTo("gte");
        assertThat(FilterConstants.FIELD_LT).isEqualTo("lt");
        assertThat(FilterConstants.FIELD_LTE).isEqualTo("lte");
    }

    @Test
    @DisplayName("Should have correct string filter field names")
    void shouldHaveCorrectStringFilterFieldNames() {
        assertThat(FilterConstants.FIELD_CONT).isEqualTo("cont");
        assertThat(FilterConstants.FIELD_START).isEqualTo("start");
        assertThat(FilterConstants.FIELD_END).isEqualTo("end");
        assertThat(FilterConstants.FIELD_MATCH).isEqualTo("match");
        assertThat(FilterConstants.FIELD_EMPTY).isEqualTo("empty");
        assertThat(FilterConstants.FIELD_NEMPTY).isEqualTo("nempty");
        assertThat(FilterConstants.FIELD_BLANK).isEqualTo("blank");
        assertThat(FilterConstants.FIELD_NBLANK).isEqualTo("nblank");
    }

    @Test
    @DisplayName("Should have correct collection filter field names")
    void shouldHaveCorrectCollectionFilterFieldNames() {
        assertThat(FilterConstants.FIELD_ANY).isEqualTo("any");
        assertThat(FilterConstants.FIELD_ALL).isEqualTo("all");
        assertThat(FilterConstants.FIELD_NONE).isEqualTo("none");
    }

    @Test
    @DisplayName("Should have correct temporal filter field names")
    void shouldHaveCorrectTemporalFilterFieldNames() {
        assertThat(FilterConstants.FIELD_BEFORE).isEqualTo("be");
        assertThat(FilterConstants.FIELD_AFTER).isEqualTo("af");
        assertThat(FilterConstants.FIELD_ON_OR_BEFORE).isEqualTo("obe");
        assertThat(FilterConstants.FIELD_ON_OR_AFTER).isEqualTo("oaf");
    }

    @Test
    @DisplayName("Should have correct performance constants")
    void shouldHaveCorrectPerformanceConstants() {
        assertThat(FilterConstants.TYPICAL_IN_SIZE).isEqualTo(4);
        assertThat(FilterConstants.TYPICAL_STRING_SIZE).isEqualTo(16);
    }

    @Test
    @DisplayName("Should have correct default datetime format patterns")
    void shouldHaveCorrectDefaultDatetimeFormatPatterns() {
        assertThat(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss");
        assertThat(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN).isEqualTo("yyyy-MM-dd");
        assertThat(FilterConstants.DEFAULT_INSTANT_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss.SSSX");
    }

    @Test
    @DisplayName("Should not be instantiable - private constructor")
    void shouldNotBeInstantiable() throws Exception {
        var constructor = FilterConstants.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();
        constructor.setAccessible(true);
        // Invoke to cover the private constructor
        FilterConstants instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
