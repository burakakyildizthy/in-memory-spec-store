package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for InstantFilter operations.
 * Tests all temporal filtering operations for Instant fields with timezone handling.
 * Requirements: 4.4, 15.9
 */
class InstantFilterTest {

    private static final String DATE_2020 = "2020-01-01T00:00:00Z";
    private static final String DATE_2024 = "2024-12-31T23:59:59Z";
    private static final String DATE_2023 = "2023-06-15T10:30:00Z";

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        InstantFilter filter = new InstantFilter();

        assertThat(filter.getEquals()).isNull();
        assertThat(filter.getNotEquals()).isNull();
        assertThat(filter.getIsBefore()).isNull();
        assertThat(filter.getIsAfter()).isNull();
        assertThat(filter.getIsOnOrBefore()).isNull();
        assertThat(filter.getIsOnOrAfter()).isNull();
        assertThat(filter.getIn()).isNull();
        assertThat(filter.getNotIn()).isNull();
        assertThat(filter.getIsNull()).isNull();
        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        Instant now = Instant.now();
        Instant past = now.minus(365, ChronoUnit.DAYS);
        Instant future = now.plus(365, ChronoUnit.DAYS);

        InstantFilter original = new InstantFilter()
            .setEquals(now)
            .setNotEquals(past)
            .setIsBefore(future)
            .setIsAfter(past)
            .setIsOnOrBefore(future)
            .setIsOnOrAfter(past)
            .setIn(Arrays.asList(now, past, future))
            .setIsNull(false)
            .setIsNotNull(true);

        InstantFilter copy = new InstantFilter(original);

        assertThat(copy.getEquals()).isEqualTo(original.getEquals());
        assertThat(copy.getNotEquals()).isEqualTo(original.getNotEquals());
        assertThat(copy.getIsBefore()).isEqualTo(original.getIsBefore());
        assertThat(copy.getIsAfter()).isEqualTo(original.getIsAfter());
        assertThat(copy.getIsOnOrBefore()).isEqualTo(original.getIsOnOrBefore());
        assertThat(copy.getIsOnOrAfter()).isEqualTo(original.getIsOnOrAfter());
        assertThat(copy.getIn()).isEqualTo(original.getIn());
        assertThat(copy.getIsNull()).isEqualTo(original.getIsNull());
        assertThat(copy.getIsNotNull()).isEqualTo(original.getIsNotNull());
    }

    @Test
    void testFluentAPIReturnsFilterInstance() {
        Instant now = Instant.now();
        Instant past = now.minus(365, ChronoUnit.DAYS);
        Instant future = now.plus(365, ChronoUnit.DAYS);

        InstantFilter filter = new InstantFilter()
            .setEquals(now)
            .setIsBefore(future)
            .setIsAfter(past)
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo(now);
        assertThat(filter.getIsBefore()).isEqualTo(future);
        assertThat(filter.getIsAfter()).isEqualTo(past);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    @Test
    void testSetEqualsWithLargeDataset() {
        Instant targetInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
                startInstant, endInstant);

        // Manually set some entities to have the target instant to guarantee matches
        entities.get(0).setLastModified(targetInstant);
        entities.get(100).setLastModified(targetInstant);
        entities.get(500).setLastModified(targetInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setEquals(targetInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).hasSize(3).allMatch(e -> e.getLastModified() != null && e.getLastModified().equals(targetInstant));
    }

    @Test
    void testSetNotEqualsWithLargeDataset() {
        Instant excludeInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotEquals(excludeInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() == null || !e.getLastModified().equals(excludeInstant));
    }

    @Test
    void testSetIsBeforeWithLargeDataset() {
        Instant beforeInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setIsBefore(beforeInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() != null && e.getLastModified().isBefore(beforeInstant));
    }

    @Test
    void testSetIsAfterWithLargeDataset() {
        Instant afterInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setIsAfter(afterInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() != null && e.getLastModified().isAfter(afterInstant));
    }

    @Test
    void testSetIsOnOrBeforeWithLargeDataset() {
        Instant onOrBeforeInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setIsOnOrBefore(onOrBeforeInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() != null &&
            (e.getLastModified().isBefore(onOrBeforeInstant) || e.getLastModified().equals(onOrBeforeInstant)));
    }

    @Test
    void testSetIsOnOrAfterWithLargeDataset() {
        Instant onOrAfterInstant = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setIsOnOrAfter(onOrAfterInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() != null &&
            (e.getLastModified().isAfter(onOrAfterInstant) || e.getLastModified().equals(onOrAfterInstant)));
    }

    @Test
    void testSetInWithLargeDataset() {
        Instant instant1 = Instant.parse("2023-01-15T10:00:00Z");
        Instant instant2 = Instant.parse("2023-06-15T14:30:00Z");
        Instant instant3 = Instant.parse("2023-12-15T18:45:00Z");
        List<Instant> instants = Arrays.asList(instant1, instant2, instant3);

        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
                startInstant, endInstant);

        // Manually set some entities to have the target instants to guarantee matches
        entities.get(0).setLastModified(instant1);
        entities.get(100).setLastModified(instant2);
        entities.get(200).setLastModified(instant3);
        entities.get(300).setLastModified(instant1); // Add duplicate for variety

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setIn(instants));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).hasSize(4).allMatch(e -> e.getLastModified() != null && instants.contains(e.getLastModified()));
    }


    @Test
    void testNullValuesWithLargeDataset() {
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        entities.get(0).setLastModified(null);
        entities.get(100).setLastModified(null);
        entities.get(500).setLastModified(null);

        TemporalEntityFilter filterNull = new TemporalEntityFilter();
        filterNull.setLastModified(new InstantFilter().setIsNull(true));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> nullResults = engine.queryByFilter(entities, filterNull);

        assertThat(nullResults).hasSize(3).allMatch(e -> e.getLastModified() == null);

        TemporalEntityFilter filterNotNull = new TemporalEntityFilter();
        filterNotNull.setLastModified(new InstantFilter().setIsNotNull(true));

        List<TemporalEntity> notNullResults = engine.queryByFilter(entities, filterNotNull);

        assertThat(notNullResults).hasSize(10_000 - 3).allMatch(e -> e.getLastModified() != null);
    }

    @Test
    void testTimezoneHandlingWithLargeDataset() {
        Instant utcInstant = Instant.parse(DATE_2023);
        Instant sameInstantDifferentFormat = Instant.parse("2023-06-15T10:30:00.000Z");

        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        entities.get(0).setLastModified(utcInstant);
        entities.get(1).setLastModified(sameInstantDifferentFormat);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setEquals(utcInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2).allMatch(e -> e.getLastModified() != null && e.getLastModified().equals(utcInstant));
    }

    @Test
    void testBoundaryInstantsWithLargeDataset() {
        Instant minInstant = Instant.parse(DATE_2020);
        Instant maxInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000, minInstant, maxInstant);

        // Manually set some entities to have the boundary instants to guarantee matches
        entities.get(0).setLastModified(minInstant);
        entities.get(1).setLastModified(minInstant);
        entities.get(100).setLastModified(maxInstant);
        entities.get(101).setLastModified(maxInstant);

        TemporalEntityFilter filterMin = new TemporalEntityFilter();
        filterMin.setLastModified(new InstantFilter().setEquals(minInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.queryByFilter(entities, filterMin);

        assertThat(minResults).hasSize(2).allMatch(e -> e.getLastModified() != null && e.getLastModified().equals(minInstant));

        TemporalEntityFilter filterMax = new TemporalEntityFilter();
        filterMax.setLastModified(new InstantFilter().setEquals(maxInstant));

        List<TemporalEntity> maxResults = engine.queryByFilter(entities, filterMax);

        assertThat(maxResults).hasSize(2).allMatch(e -> e.getLastModified() != null && e.getLastModified().equals(maxInstant));
    }

    @Test
    void testCombinedFiltersWithLargeDataset() {
        Instant startInstant = Instant.parse("2022-01-01T00:00:00Z");
        Instant endInstant = Instant.parse("2023-12-31T23:59:59Z");

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            Instant.parse(DATE_2020),
            Instant.parse(DATE_2024));

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter()
            .setIsOnOrAfter(startInstant)
            .setIsOnOrBefore(endInstant));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e -> e.getLastModified() != null &&
            !e.getLastModified().isBefore(startInstant) &&
            !e.getLastModified().isAfter(endInstant));
    }
}
