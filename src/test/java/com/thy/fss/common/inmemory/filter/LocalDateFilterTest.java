package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
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
}
