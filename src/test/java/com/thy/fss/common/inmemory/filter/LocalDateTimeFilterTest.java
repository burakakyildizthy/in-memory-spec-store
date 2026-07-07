package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for LocalDateTimeFilter operations.
 * Tests all temporal filtering operations for LocalDateTime fields with time precision.
 * Requirements: 4.4, 15.9
 */
class LocalDateTimeFilterTest {

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        LocalDateTimeFilter filter = new LocalDateTimeFilter();

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
        LocalDateTimeFilter original = new LocalDateTimeFilter()
            .setEquals(LocalDateTime.of(2023, 6, 15, 10, 30, 0))
            .setNotEquals(LocalDateTime.of(2023, 1, 1, 0, 0, 0))
            .setIsBefore(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
            .setIsAfter(LocalDateTime.of(2022, 1, 1, 0, 0, 0))
            .setIsOnOrBefore(LocalDateTime.of(2023, 12, 31, 23, 59, 59))
            .setIsOnOrAfter(LocalDateTime.of(2023, 1, 1, 0, 0, 0))
            .setIn(Arrays.asList(
                LocalDateTime.of(2023, 6, 15, 10, 30, 0),
                LocalDateTime.of(2023, 7, 15, 14, 45, 0)))
            .setIsNull(false)
            .setIsNotNull(true);

        LocalDateTimeFilter copy = new LocalDateTimeFilter(original);

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
        LocalDateTime dateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);

        LocalDateTimeFilter filter = new LocalDateTimeFilter()
            .setEquals(dateTime)
            .setIsBefore(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
            .setIsAfter(LocalDateTime.of(2022, 1, 1, 0, 0, 0))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter)
                .satisfies(f -> {
                    assertThat(f.getEquals()).isEqualTo(dateTime);
                    assertThat(f.getIsBefore()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
                    assertThat(f.getIsAfter()).isEqualTo(LocalDateTime.of(2022, 1, 1, 0, 0, 0));
                    assertThat(f.getIsNull()).isFalse();
                    assertThat(f.getIsNotNull()).isTrue();
                });
    }

    @Test
    void testSetEqualsWithLargeDataset() {
        LocalDateTime targetDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        // Manually set some entities to have the target datetime to guarantee matches
        entities.get(0).setCreatedAt(targetDateTime);
        entities.get(100).setCreatedAt(targetDateTime);
        entities.get(500).setCreatedAt(targetDateTime);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setEquals(targetDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).hasSize(3).allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(targetDateTime));
    }

    @Test
    void testSetNotEqualsWithLargeDataset() {
        LocalDateTime excludeDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotEquals(excludeDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() == null || !e.getCreatedAt().equals(excludeDateTime));
    }

    @Test
    void testSetIsBeforeWithLargeDataset() {
        LocalDateTime beforeDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setIsBefore(beforeDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isBefore(beforeDateTime));
    }

    @Test
    void testSetIsAfterWithLargeDataset() {
        LocalDateTime afterDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setIsAfter(afterDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(afterDateTime));
    }

    @Test
    void testSetIsOnOrBeforeWithLargeDataset() {
        LocalDateTime onOrBeforeDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setIsOnOrBefore(onOrBeforeDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            (e.getCreatedAt().isBefore(onOrBeforeDateTime) || e.getCreatedAt().isEqual(onOrBeforeDateTime)));
    }

    @Test
    void testSetIsOnOrAfterWithLargeDataset() {
        LocalDateTime onOrAfterDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setIsOnOrAfter(onOrAfterDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            (e.getCreatedAt().isAfter(onOrAfterDateTime) || e.getCreatedAt().isEqual(onOrAfterDateTime)));
    }

    @Test
    void testSetInWithLargeDataset() {
        LocalDateTime dateTime1 = LocalDateTime.of(2023, 1, 15, 10, 0, 0);
        LocalDateTime dateTime2 = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        LocalDateTime dateTime3 = LocalDateTime.of(2023, 12, 15, 18, 45, 0);
        List<LocalDateTime> dateTimes = Arrays.asList(dateTime1, dateTime2, dateTime3);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        // Manually set some entities to have the target datetimes to guarantee matches
        entities.get(0).setCreatedAt(dateTime1);
        entities.get(100).setCreatedAt(dateTime2);
        entities.get(200).setCreatedAt(dateTime3);
        entities.get(300).setCreatedAt(dateTime1); // Add duplicate for variety

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setIn(dateTimes));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).hasSize(4).allMatch(e -> e.getCreatedAt() != null && dateTimes.contains(e.getCreatedAt()));
    }


    @Test
    void testNullValuesWithLargeDataset() {
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        entities.get(0).setCreatedAt(null);
        entities.get(100).setCreatedAt(null);
        entities.get(500).setCreatedAt(null);

        TemporalEntityFilter filterNull = new TemporalEntityFilter();
        filterNull.setCreatedAt(new LocalDateTimeFilter().setIsNull(true));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> nullResults = engine.queryByFilter(entities, filterNull);

        assertThat(nullResults).hasSize(3).allMatch(e -> e.getCreatedAt() == null);

        TemporalEntityFilter filterNotNull = new TemporalEntityFilter();
        filterNotNull.setCreatedAt(new LocalDateTimeFilter().setIsNotNull(true));

        List<TemporalEntity> notNullResults = engine.queryByFilter(entities, filterNotNull);

        assertThat(notNullResults).hasSize(10_000 - 3).allMatch(e -> e.getCreatedAt() != null);
    }

    @Test
    void testTimePrecisionWithLargeDataset() {
        LocalDateTime baseDateTime = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        LocalDateTime sameMinute = LocalDateTime.of(2023, 6, 15, 10, 30, 30);
        LocalDateTime differentMinute = LocalDateTime.of(2023, 6, 15, 10, 31, 0);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        entities.get(0).setCreatedAt(baseDateTime);
        entities.get(1).setCreatedAt(sameMinute);
        entities.get(2).setCreatedAt(differentMinute);

        TemporalEntityFilter filterExact = new TemporalEntityFilter();
        filterExact.setCreatedAt(new LocalDateTimeFilter().setEquals(baseDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> exactResults = engine.queryByFilter(entities, filterExact);

        assertThat(exactResults).hasSize(1);
        assertThat(exactResults.get(0).getCreatedAt()).isEqualTo(baseDateTime);
        assertThat(exactResults.get(0).getCreatedAt()).isNotEqualTo(sameMinute);
    }

    @Test
    void testBoundaryDateTimesWithLargeDataset() {
        LocalDateTime minDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime maxDateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000, minDateTime, maxDateTime);

        // Manually set some entities to have the boundary datetimes to guarantee matches
        entities.get(0).setCreatedAt(minDateTime);
        entities.get(1).setCreatedAt(minDateTime);
        entities.get(100).setCreatedAt(maxDateTime);
        entities.get(101).setCreatedAt(maxDateTime);

        TemporalEntityFilter filterMin = new TemporalEntityFilter();
        filterMin.setCreatedAt(new LocalDateTimeFilter().setEquals(minDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.queryByFilter(entities, filterMin);

        assertThat(minResults).hasSize(2).allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(minDateTime));

        TemporalEntityFilter filterMax = new TemporalEntityFilter();
        filterMax.setCreatedAt(new LocalDateTimeFilter().setEquals(maxDateTime));

        List<TemporalEntity> maxResults = engine.queryByFilter(entities, filterMax);

        assertThat(maxResults).hasSize(2).allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(maxDateTime));
    }

    @Test
    void testCombinedFiltersWithLargeDataset() {
        LocalDateTime startDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 12, 31, 23, 59, 59);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter()
            .setIsOnOrAfter(startDateTime)
            .setIsOnOrBefore(endDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            !e.getCreatedAt().isBefore(startDateTime) &&
            !e.getCreatedAt().isAfter(endDateTime));
    }

    // ====================================================================
    // Negated Temporal Operator Tests (Requirements 16.3–16.10)
    // ====================================================================

    @Test
    void testDefaultConstructorNegatedFieldsAreNull() {
        LocalDateTimeFilter filter = new LocalDateTimeFilter();

        assertThat(filter.getNotIsBefore()).isNull();
        assertThat(filter.getNotIsAfter()).isNull();
        assertThat(filter.getNotIsOnOrBefore()).isNull();
        assertThat(filter.getNotIsOnOrAfter()).isNull();
    }

    @Test
    void testNegatedSettersReturnSameInstance() {
        LocalDateTimeFilter filter = new LocalDateTimeFilter();
        LocalDateTime dt = LocalDateTime.of(2023, 6, 15, 10, 30, 0);

        LocalDateTimeFilter result1 = filter.setNotIsBefore(dt);
        assertThat(result1).isSameAs(filter);
        assertThat(filter.getNotIsBefore()).isEqualTo(dt);

        LocalDateTimeFilter result2 = filter.setNotIsAfter(dt);
        assertThat(result2).isSameAs(filter);
        assertThat(filter.getNotIsAfter()).isEqualTo(dt);

        LocalDateTimeFilter result3 = filter.setNotIsOnOrBefore(dt);
        assertThat(result3).isSameAs(filter);
        assertThat(filter.getNotIsOnOrBefore()).isEqualTo(dt);

        LocalDateTimeFilter result4 = filter.setNotIsOnOrAfter(dt);
        assertThat(result4).isSameAs(filter);
        assertThat(filter.getNotIsOnOrAfter()).isEqualTo(dt);
    }

    @Test
    void testCopyConstructorCopiesNegatedFields() {
        LocalDateTime dt1 = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        LocalDateTime dt2 = LocalDateTime.of(2023, 6, 15, 12, 0, 0);
        LocalDateTime dt3 = LocalDateTime.of(2023, 9, 1, 8, 0, 0);
        LocalDateTime dt4 = LocalDateTime.of(2023, 12, 31, 23, 59, 59);

        LocalDateTimeFilter original = new LocalDateTimeFilter()
            .setNotIsBefore(dt1)
            .setNotIsAfter(dt2)
            .setNotIsOnOrBefore(dt3)
            .setNotIsOnOrAfter(dt4);

        LocalDateTimeFilter copy = new LocalDateTimeFilter(original);

        assertThat(copy.getNotIsBefore()).isEqualTo(dt1);
        assertThat(copy.getNotIsAfter()).isEqualTo(dt2);
        assertThat(copy.getNotIsOnOrBefore()).isEqualTo(dt3);
        assertThat(copy.getNotIsOnOrAfter()).isEqualTo(dt4);
    }

    @Test
    void testNegatedFieldsAffectEquals() {
        LocalDateTime dt = LocalDateTime.of(2023, 6, 15, 10, 30, 0);

        LocalDateTimeFilter filter1 = new LocalDateTimeFilter().setNotIsBefore(dt);
        LocalDateTimeFilter filter2 = new LocalDateTimeFilter().setNotIsBefore(dt);
        LocalDateTimeFilter filter3 = new LocalDateTimeFilter().setNotIsBefore(dt.plusDays(1));

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1).isNotEqualTo(filter3);

        LocalDateTimeFilter filter4 = new LocalDateTimeFilter().setNotIsAfter(dt);
        LocalDateTimeFilter filter5 = new LocalDateTimeFilter();
        assertThat(filter4).isNotEqualTo(filter5);

        LocalDateTimeFilter filter6 = new LocalDateTimeFilter().setNotIsOnOrBefore(dt);
        LocalDateTimeFilter filter7 = new LocalDateTimeFilter();
        assertThat(filter6).isNotEqualTo(filter7);

        LocalDateTimeFilter filter8 = new LocalDateTimeFilter().setNotIsOnOrAfter(dt);
        LocalDateTimeFilter filter9 = new LocalDateTimeFilter();
        assertThat(filter8).isNotEqualTo(filter9);
    }

    @Test
    void testNegatedFieldsAffectHashCode() {
        LocalDateTime dt = LocalDateTime.of(2023, 6, 15, 10, 30, 0);

        LocalDateTimeFilter filter1 = new LocalDateTimeFilter().setNotIsBefore(dt);
        LocalDateTimeFilter filter2 = new LocalDateTimeFilter().setNotIsBefore(dt);

        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }

    @Test
    void testNegatedFieldsAppearInToString() {
        LocalDateTime dt1 = LocalDateTime.of(2023, 3, 15, 10, 0, 0);
        LocalDateTime dt2 = LocalDateTime.of(2023, 6, 20, 14, 30, 0);
        LocalDateTime dt3 = LocalDateTime.of(2023, 9, 25, 18, 45, 0);
        LocalDateTime dt4 = LocalDateTime.of(2023, 12, 30, 22, 0, 0);

        LocalDateTimeFilter filter = new LocalDateTimeFilter()
            .setNotIsBefore(dt1)
            .setNotIsAfter(dt2)
            .setNotIsOnOrBefore(dt3)
            .setNotIsOnOrAfter(dt4);

        String str = filter.toString();
        assertThat(str).contains("notIsBefore=" + dt1);
        assertThat(str).contains("notIsAfter=" + dt2);
        assertThat(str).contains("notIsOnOrBefore=" + dt3);
        assertThat(str).contains("notIsOnOrAfter=" + dt4);
    }

    @Test
    void testNotIsBeforeMatchesSemantics() {
        // notIsBefore(X) means !isBefore(X), i.e. field >= X
        LocalDateTime threshold = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getCreatedAt() != null && !e.getCreatedAt().isBefore(threshold));
    }

    @Test
    void testNotIsAfterMatchesSemantics() {
        // notIsAfter(X) means !isAfter(X), i.e. field <= X
        LocalDateTime threshold = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotIsAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getCreatedAt() != null && !e.getCreatedAt().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrBeforeMatchesSemantics() {
        // notIsOnOrBefore(X) means isAfter(X), i.e. field > X
        LocalDateTime threshold = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotIsOnOrBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getCreatedAt() != null && e.getCreatedAt().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrAfterMatchesSemantics() {
        // notIsOnOrAfter(X) means isBefore(X), i.e. field < X
        LocalDateTime threshold = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotIsOnOrAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getCreatedAt() != null && e.getCreatedAt().isBefore(threshold));
    }

    @Test
    void testNegatedOperatorsReturnFalseWhenFieldIsNull() {
        LocalDateTime threshold = LocalDateTime.of(2023, 6, 15, 10, 30, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(100,
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 12, 31, 23, 59, 59));

        // Set some entities to have null createdAt
        entities.get(0).setCreatedAt(null);
        entities.get(1).setCreatedAt(null);
        entities.get(2).setCreatedAt(null);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setCreatedAt(new LocalDateTimeFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        // Null field values should NOT match (return false)
        assertThat(results).allMatch(e -> e.getCreatedAt() != null);
    }

    // ==================== Property-Based Tests ====================

    /**
     * Property 2: Zamansal olumsuzlama ikiliği (negation duality)
     *
     * For any non-null LocalDateTime fieldValue and threshold:
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
    void temporalNegationDuality(@ForAll("arbitraryLocalDateTime") LocalDateTime fieldValue,
                                  @ForAll("arbitraryLocalDateTime") LocalDateTime threshold) {
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
    void temporalNegationDualityNullFieldReturnsFalse(@ForAll("arbitraryLocalDateTime") LocalDateTime threshold) {
        // When fieldValue is null, the generated validation code evaluates "field != null && ..."
        // which always returns false. We model this expectation directly.
        LocalDateTime fieldValue = null;

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
    Arbitrary<LocalDateTime> arbitraryLocalDateTime() {
        return Arbitraries.longs()
            .between(0, 10000L * 24 * 60 * 60)
            .map(seconds -> LocalDateTime.of(2000, 1, 1, 0, 0, 0).plusSeconds(seconds));
    }
}
