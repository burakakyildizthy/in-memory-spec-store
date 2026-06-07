package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
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
}
