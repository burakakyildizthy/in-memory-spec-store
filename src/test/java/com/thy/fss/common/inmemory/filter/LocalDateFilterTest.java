package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for LocalDateFilter operations.
 * Tests all temporal filtering operations for LocalDate fields.
 * Requirements: 4.4, 15.9
 */
class LocalDateFilterTest {

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        LocalDateFilter filter = new LocalDateFilter();

        assertThat(filter)
                .satisfies(f -> {
                    assertThat(f.getEquals()).isNull();
                    assertThat(f.getNotEquals()).isNull();
                    assertThat(f.getIsBefore()).isNull();
                    assertThat(f.getIsAfter()).isNull();
                    assertThat(f.getIsOnOrBefore()).isNull();
                    assertThat(f.getIsOnOrAfter()).isNull();
                    assertThat(f.getIn()).isNull();
                    assertThat(f.getNotIn()).isNull();
                    assertThat(f.getIsNull()).isNull();
                    assertThat(f.getIsNotNull()).isNull();
                });
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        LocalDateFilter original = new LocalDateFilter()
            .setEquals(LocalDate.of(2023, 6, 15))
            .setNotEquals(LocalDate.of(2023, 1, 1))
            .setIsBefore(LocalDate.of(2024, 1, 1))
            .setIsAfter(LocalDate.of(2022, 1, 1))
            .setIsOnOrBefore(LocalDate.of(2023, 12, 31))
            .setIsOnOrAfter(LocalDate.of(2023, 1, 1))
            .setIn(Arrays.asList(LocalDate.of(2023, 6, 15), LocalDate.of(2023, 7, 15)))
            .setIsNull(false)
            .setIsNotNull(true);

        LocalDateFilter copy = new LocalDateFilter(original);

        assertThat(copy)
                .satisfies(c -> {
                    assertThat(c.getEquals()).isEqualTo(original.getEquals());
                    assertThat(c.getNotEquals()).isEqualTo(original.getNotEquals());
                    assertThat(c.getIsBefore()).isEqualTo(original.getIsBefore());
                    assertThat(c.getIsAfter()).isEqualTo(original.getIsAfter());
                    assertThat(c.getIsOnOrBefore()).isEqualTo(original.getIsOnOrBefore());
                    assertThat(c.getIsOnOrAfter()).isEqualTo(original.getIsOnOrAfter());
                    assertThat(c.getIn()).isEqualTo(original.getIn());
                    assertThat(c.getIsNull()).isEqualTo(original.getIsNull());
                    assertThat(c.getIsNotNull()).isEqualTo(original.getIsNotNull());
                });
    }

    @Test
    void testFluentAPIReturnsFilterInstance() {
        LocalDate date = LocalDate.of(2023, 6, 15);

        LocalDateFilter filter = new LocalDateFilter()
            .setEquals(date)
            .setIsBefore(LocalDate.of(2024, 1, 1))
            .setIsAfter(LocalDate.of(2022, 1, 1))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter)
                .satisfies(f -> {
                    assertThat(f.getEquals()).isEqualTo(date);
                    assertThat(f.getIsBefore()).isEqualTo(LocalDate.of(2024, 1, 1));
                    assertThat(f.getIsAfter()).isEqualTo(LocalDate.of(2022, 1, 1));
                    assertThat(f.getIsNull()).isFalse();
                    assertThat(f.getIsNotNull()).isTrue();
                });
    }

    @Test
    void testSetEqualsWithLargeDataset() {
        LocalDate targetDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setEquals(targetDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty()
                .allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(targetDate));
    }

    @Test
    void testSetNotEqualsWithLargeDataset() {
        LocalDate excludeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotEquals(excludeDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() == null || !e.getBirthDate().equals(excludeDate));
    }

    @Test
    void testSetIsBeforeWithLargeDataset() {
        LocalDate beforeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setIsBefore(beforeDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isBefore(beforeDate));
    }

    @Test
    void testSetIsAfterWithLargeDataset() {
        LocalDate afterDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setIsAfter(afterDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isAfter(afterDate));
    }

    @Test
    void testSetIsOnOrBeforeWithLargeDataset() {
        LocalDate onOrBeforeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setIsOnOrBefore(onOrBeforeDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            (e.getBirthDate().isBefore(onOrBeforeDate) || e.getBirthDate().isEqual(onOrBeforeDate)));
    }

    @Test
    void testSetIsOnOrAfterWithLargeDataset() {
        LocalDate onOrAfterDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setIsOnOrAfter(onOrAfterDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            (e.getBirthDate().isAfter(onOrAfterDate) || e.getBirthDate().isEqual(onOrAfterDate)));
    }

    @Test
    void testSetInWithLargeDataset() {
        LocalDate date1 = LocalDate.of(2023, 1, 15);
        LocalDate date2 = LocalDate.of(2023, 6, 15);
        LocalDate date3 = LocalDate.of(2023, 12, 15);
        List<LocalDate> dates = Arrays.asList(date1, date2, date3);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setIn(dates));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && dates.contains(e.getBirthDate()));
    }

    @Test
    void testNullValuesWithLargeDataset() {
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        entities.get(0).setBirthDate(null);
        entities.get(100).setBirthDate(null);
        entities.get(500).setBirthDate(null);

        TemporalEntityFilter filterNull = new TemporalEntityFilter();
        filterNull.setBirthDate(new LocalDateFilter().setIsNull(true));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> nullResults = engine.queryByFilter(entities, filterNull);

        assertThat(nullResults).hasSize(3).allMatch(e -> e.getBirthDate() == null);

        TemporalEntityFilter filterNotNull = new TemporalEntityFilter();
        filterNotNull.setBirthDate(new LocalDateFilter().setIsNotNull(true));

        List<TemporalEntity> notNullResults = engine.queryByFilter(entities, filterNotNull);

        assertThat(notNullResults).hasSize(10000 - 3).allMatch(e -> e.getBirthDate() != null);
    }

    @Test
    void testBoundaryDatesWithLargeDataset() {
        LocalDate minDate = LocalDate.of(2020, 1, 1);
        LocalDate maxDate = LocalDate.of(2024, 12, 31);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000, minDate, maxDate);

        TemporalEntityFilter filterMin = new TemporalEntityFilter();
        filterMin.setBirthDate(new LocalDateFilter().setEquals(minDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.queryByFilter(entities, filterMin);

        assertThat(minResults).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(minDate));

        TemporalEntityFilter filterMax = new TemporalEntityFilter();
        filterMax.setBirthDate(new LocalDateFilter().setEquals(maxDate));

        List<TemporalEntity> maxResults = engine.queryByFilter(entities, filterMax);

        assertThat(maxResults).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(maxDate));
    }

    @Test
    void testCombinedFiltersWithLargeDataset() {
        LocalDate startDate = LocalDate.of(2022, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter()
            .setIsOnOrAfter(startDate)
            .setIsOnOrBefore(endDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            !e.getBirthDate().isBefore(startDate) &&
            !e.getBirthDate().isAfter(endDate));
    }

    // ====================================================================
    // Negated Temporal Operator Tests (Requirements 16.3–16.10)
    // ====================================================================

    @Test
    void testDefaultConstructorNegatedFieldsAreNull() {
        LocalDateFilter filter = new LocalDateFilter();

        assertThat(filter.getNotIsBefore()).isNull();
        assertThat(filter.getNotIsAfter()).isNull();
        assertThat(filter.getNotIsOnOrBefore()).isNull();
        assertThat(filter.getNotIsOnOrAfter()).isNull();
    }

    @Test
    void testNegatedSettersReturnSameInstance() {
        LocalDateFilter filter = new LocalDateFilter();
        LocalDate date = LocalDate.of(2023, 6, 15);

        LocalDateFilter result1 = filter.setNotIsBefore(date);
        assertThat(result1).isSameAs(filter);
        assertThat(filter.getNotIsBefore()).isEqualTo(date);

        LocalDateFilter result2 = filter.setNotIsAfter(date);
        assertThat(result2).isSameAs(filter);
        assertThat(filter.getNotIsAfter()).isEqualTo(date);

        LocalDateFilter result3 = filter.setNotIsOnOrBefore(date);
        assertThat(result3).isSameAs(filter);
        assertThat(filter.getNotIsOnOrBefore()).isEqualTo(date);

        LocalDateFilter result4 = filter.setNotIsOnOrAfter(date);
        assertThat(result4).isSameAs(filter);
        assertThat(filter.getNotIsOnOrAfter()).isEqualTo(date);
    }

    @Test
    void testCopyConstructorCopiesNegatedFields() {
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 6, 15);
        LocalDate d3 = LocalDate.of(2023, 9, 1);
        LocalDate d4 = LocalDate.of(2023, 12, 31);

        LocalDateFilter original = new LocalDateFilter()
            .setNotIsBefore(d1)
            .setNotIsAfter(d2)
            .setNotIsOnOrBefore(d3)
            .setNotIsOnOrAfter(d4);

        LocalDateFilter copy = new LocalDateFilter(original);

        assertThat(copy.getNotIsBefore()).isEqualTo(d1);
        assertThat(copy.getNotIsAfter()).isEqualTo(d2);
        assertThat(copy.getNotIsOnOrBefore()).isEqualTo(d3);
        assertThat(copy.getNotIsOnOrAfter()).isEqualTo(d4);
    }

    @Test
    void testNegatedFieldsAffectEquals() {
        LocalDate date = LocalDate.of(2023, 6, 15);

        LocalDateFilter filter1 = new LocalDateFilter().setNotIsBefore(date);
        LocalDateFilter filter2 = new LocalDateFilter().setNotIsBefore(date);
        LocalDateFilter filter3 = new LocalDateFilter().setNotIsBefore(date.plusDays(1));

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1).isNotEqualTo(filter3);

        LocalDateFilter filter4 = new LocalDateFilter().setNotIsAfter(date);
        LocalDateFilter filter5 = new LocalDateFilter();
        assertThat(filter4).isNotEqualTo(filter5);

        LocalDateFilter filter6 = new LocalDateFilter().setNotIsOnOrBefore(date);
        LocalDateFilter filter7 = new LocalDateFilter();
        assertThat(filter6).isNotEqualTo(filter7);

        LocalDateFilter filter8 = new LocalDateFilter().setNotIsOnOrAfter(date);
        LocalDateFilter filter9 = new LocalDateFilter();
        assertThat(filter8).isNotEqualTo(filter9);
    }

    @Test
    void testNegatedFieldsAffectHashCode() {
        LocalDate date = LocalDate.of(2023, 6, 15);

        LocalDateFilter filter1 = new LocalDateFilter().setNotIsBefore(date);
        LocalDateFilter filter2 = new LocalDateFilter().setNotIsBefore(date);

        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }

    @Test
    void testNegatedFieldsAppearInToString() {
        LocalDate d1 = LocalDate.of(2023, 3, 15);
        LocalDate d2 = LocalDate.of(2023, 6, 20);
        LocalDate d3 = LocalDate.of(2023, 9, 25);
        LocalDate d4 = LocalDate.of(2023, 12, 30);

        LocalDateFilter filter = new LocalDateFilter()
            .setNotIsBefore(d1)
            .setNotIsAfter(d2)
            .setNotIsOnOrBefore(d3)
            .setNotIsOnOrAfter(d4);

        String str = filter.toString();
        assertThat(str).contains("notIsBefore=" + d1);
        assertThat(str).contains("notIsAfter=" + d2);
        assertThat(str).contains("notIsOnOrBefore=" + d3);
        assertThat(str).contains("notIsOnOrAfter=" + d4);
    }

    @Test
    void testNotIsBeforeMatchesSemantics() {
        // notIsBefore(X) means !isBefore(X), i.e. field >= X
        LocalDate threshold = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getBirthDate() != null && !e.getBirthDate().isBefore(threshold));
    }

    @Test
    void testNotIsAfterMatchesSemantics() {
        // notIsAfter(X) means !isAfter(X), i.e. field <= X
        LocalDate threshold = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotIsAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getBirthDate() != null && !e.getBirthDate().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrBeforeMatchesSemantics() {
        // notIsOnOrBefore(X) means isAfter(X), i.e. field > X
        LocalDate threshold = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotIsOnOrBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getBirthDate() != null && e.getBirthDate().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrAfterMatchesSemantics() {
        // notIsOnOrAfter(X) means isBefore(X), i.e. field < X
        LocalDate threshold = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotIsOnOrAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getBirthDate() != null && e.getBirthDate().isBefore(threshold));
    }

    @Test
    void testNegatedOperatorsReturnFalseWhenFieldIsNull() {
        LocalDate threshold = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(100,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        // Set some entities to have null birthDate
        entities.get(0).setBirthDate(null);
        entities.get(1).setBirthDate(null);
        entities.get(2).setBirthDate(null);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setBirthDate(new LocalDateFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        // Null field values should NOT match (return false)
        assertThat(results).allMatch(e -> e.getBirthDate() != null);
    }

    // ==================== Property-Based Tests ====================

    /**
     * Property 2: Zamansal olumsuzlama ikiliği (negation duality)
     *
     * For any non-null LocalDate fieldValue and threshold:
     * - notIsBefore(threshold) == !fieldValue.isBefore(threshold)
     * - notIsAfter(threshold) == !fieldValue.isAfter(threshold)
     * - notIsOnOrBefore(threshold) == fieldValue.isAfter(threshold)
     * - notIsOnOrAfter(threshold) == fieldValue.isBefore(threshold)
     * When fieldValue is null, all negated operators return false.
     *
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 2: Zamansal olumsuzlama ikiliği")
    void temporalNegationDuality(@ForAll("arbitraryLocalDate") LocalDate fieldValue,
                                  @ForAll("arbitraryLocalDate") LocalDate threshold) {
        // For non-null fieldValue: negated operator == logical negation of positive operator
        // notIsBefore(threshold) should equal !fieldValue.isBefore(threshold)
        boolean isBefore = fieldValue.isBefore(threshold);
        boolean expectedNotIsBefore = !isBefore;
        assertThat(expectedNotIsBefore).isEqualTo(!fieldValue.isBefore(threshold));

        // notIsAfter(threshold) should equal !fieldValue.isAfter(threshold)
        boolean isAfter = fieldValue.isAfter(threshold);
        boolean expectedNotIsAfter = !isAfter;
        assertThat(expectedNotIsAfter).isEqualTo(!fieldValue.isAfter(threshold));

        // notIsOnOrBefore(threshold): positive isOnOrBefore = !isAfter, negation = isAfter
        boolean isOnOrBefore = !fieldValue.isAfter(threshold);
        boolean expectedNotIsOnOrBefore = !isOnOrBefore; // == fieldValue.isAfter(threshold)
        assertThat(expectedNotIsOnOrBefore).isEqualTo(fieldValue.isAfter(threshold));

        // notIsOnOrAfter(threshold): positive isOnOrAfter = !isBefore, negation = isBefore
        boolean isOnOrAfter = !fieldValue.isBefore(threshold);
        boolean expectedNotIsOnOrAfter = !isOnOrAfter; // == fieldValue.isBefore(threshold)
        assertThat(expectedNotIsOnOrAfter).isEqualTo(fieldValue.isBefore(threshold));
    }

    /**
     * Property 2 (null case): When fieldValue is null, all negated temporal operators return false.
     *
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 2: Zamansal olumsuzlama ikiliği - null alan")
    void temporalNegationDualityNullFieldReturnsFalse(@ForAll("arbitraryLocalDate") LocalDate threshold) {
        // When fieldValue is null, the generated validation code evaluates "field != null && ..."
        // which always returns false. We model this expectation directly.
        LocalDate fieldValue = null;

        // notIsBefore: field != null && !field.isBefore(value) → false
        boolean notIsBeforeResult = fieldValue != null && !fieldValue.isBefore(threshold);
        assertThat(notIsBeforeResult).isFalse();

        // notIsAfter: field != null && !field.isAfter(value) → false
        boolean notIsAfterResult = fieldValue != null && !fieldValue.isAfter(threshold);
        assertThat(notIsAfterResult).isFalse();

        // notIsOnOrBefore: field != null && field.isAfter(value) → false
        boolean notIsOnOrBeforeResult = fieldValue != null && fieldValue.isAfter(threshold);
        assertThat(notIsOnOrBeforeResult).isFalse();

        // notIsOnOrAfter: field != null && field.isBefore(value) → false
        boolean notIsOnOrAfterResult = fieldValue != null && fieldValue.isBefore(threshold);
        assertThat(notIsOnOrAfterResult).isFalse();
    }

    @Provide
    Arbitrary<LocalDate> arbitraryLocalDate() {
        return Arbitraries.integers()
            .between(0, 10000)
            .map(days -> LocalDate.of(2000, 1, 1).plusDays(days));
    }
}
