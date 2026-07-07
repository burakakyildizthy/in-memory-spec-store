package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntityFilter;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
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

    // ====================================================================
    // Negated Temporal Operator Tests (Requirements 16.3–16.10)
    // ====================================================================

    @Test
    void testDefaultConstructorNegatedFieldsAreNull() {
        InstantFilter filter = new InstantFilter();

        assertThat(filter.getNotIsBefore()).isNull();
        assertThat(filter.getNotIsAfter()).isNull();
        assertThat(filter.getNotIsOnOrBefore()).isNull();
        assertThat(filter.getNotIsOnOrAfter()).isNull();
    }

    @Test
    void testNegatedSettersReturnSameInstance() {
        InstantFilter filter = new InstantFilter();
        Instant now = Instant.now();

        InstantFilter result1 = filter.setNotIsBefore(now);
        assertThat(result1).isSameAs(filter);
        assertThat(filter.getNotIsBefore()).isEqualTo(now);

        InstantFilter result2 = filter.setNotIsAfter(now);
        assertThat(result2).isSameAs(filter);
        assertThat(filter.getNotIsAfter()).isEqualTo(now);

        InstantFilter result3 = filter.setNotIsOnOrBefore(now);
        assertThat(result3).isSameAs(filter);
        assertThat(filter.getNotIsOnOrBefore()).isEqualTo(now);

        InstantFilter result4 = filter.setNotIsOnOrAfter(now);
        assertThat(result4).isSameAs(filter);
        assertThat(filter.getNotIsOnOrAfter()).isEqualTo(now);
    }

    @Test
    void testCopyConstructorCopiesNegatedFields() {
        Instant t1 = Instant.parse("2023-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2023-06-15T12:00:00Z");
        Instant t3 = Instant.parse("2023-09-01T08:00:00Z");
        Instant t4 = Instant.parse("2023-12-31T23:59:59Z");

        InstantFilter original = new InstantFilter()
            .setNotIsBefore(t1)
            .setNotIsAfter(t2)
            .setNotIsOnOrBefore(t3)
            .setNotIsOnOrAfter(t4);

        InstantFilter copy = new InstantFilter(original);

        assertThat(copy.getNotIsBefore()).isEqualTo(t1);
        assertThat(copy.getNotIsAfter()).isEqualTo(t2);
        assertThat(copy.getNotIsOnOrBefore()).isEqualTo(t3);
        assertThat(copy.getNotIsOnOrAfter()).isEqualTo(t4);
    }

    @Test
    void testNegatedFieldsAffectEquals() {
        Instant now = Instant.now();

        InstantFilter filter1 = new InstantFilter().setNotIsBefore(now);
        InstantFilter filter2 = new InstantFilter().setNotIsBefore(now);
        InstantFilter filter3 = new InstantFilter().setNotIsBefore(now.plus(1, ChronoUnit.DAYS));

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1).isNotEqualTo(filter3);

        InstantFilter filter4 = new InstantFilter().setNotIsAfter(now);
        InstantFilter filter5 = new InstantFilter();
        assertThat(filter4).isNotEqualTo(filter5);

        InstantFilter filter6 = new InstantFilter().setNotIsOnOrBefore(now);
        InstantFilter filter7 = new InstantFilter();
        assertThat(filter6).isNotEqualTo(filter7);

        InstantFilter filter8 = new InstantFilter().setNotIsOnOrAfter(now);
        InstantFilter filter9 = new InstantFilter();
        assertThat(filter8).isNotEqualTo(filter9);
    }

    @Test
    void testNegatedFieldsAffectHashCode() {
        Instant now = Instant.now();

        InstantFilter filter1 = new InstantFilter().setNotIsBefore(now);
        InstantFilter filter2 = new InstantFilter().setNotIsBefore(now);

        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }

    @Test
    void testNegatedFieldsAppearInToString() {
        Instant t1 = Instant.parse("2023-03-15T10:00:00Z");
        Instant t2 = Instant.parse("2023-06-20T14:30:00Z");
        Instant t3 = Instant.parse("2023-09-25T18:45:00Z");
        Instant t4 = Instant.parse("2023-12-30T22:00:00Z");

        InstantFilter filter = new InstantFilter()
            .setNotIsBefore(t1)
            .setNotIsAfter(t2)
            .setNotIsOnOrBefore(t3)
            .setNotIsOnOrAfter(t4);

        String str = filter.toString();
        assertThat(str).contains("notIsBefore=" + t1);
        assertThat(str).contains("notIsAfter=" + t2);
        assertThat(str).contains("notIsOnOrBefore=" + t3);
        assertThat(str).contains("notIsOnOrAfter=" + t4);
    }

    @Test
    void testNotIsBeforeMatchesSemantics() {
        // notIsBefore(X) means !isBefore(X), i.e. field >= X
        Instant threshold = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getLastModified() != null && !e.getLastModified().isBefore(threshold));
    }

    @Test
    void testNotIsAfterMatchesSemantics() {
        // notIsAfter(X) means !isAfter(X), i.e. field <= X
        Instant threshold = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotIsAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getLastModified() != null && !e.getLastModified().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrBeforeMatchesSemantics() {
        // notIsOnOrBefore(X) means isAfter(X), i.e. field > X
        Instant threshold = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotIsOnOrBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getLastModified() != null && e.getLastModified().isAfter(threshold));
    }

    @Test
    void testNotIsOnOrAfterMatchesSemantics() {
        // notIsOnOrAfter(X) means isBefore(X), i.e. field < X
        Instant threshold = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            startInstant, endInstant);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotIsOnOrAfter(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        assertThat(results).isNotEmpty().allMatch(e ->
            e.getLastModified() != null && e.getLastModified().isBefore(threshold));
    }

    @Test
    void testNegatedOperatorsReturnFalseWhenFieldIsNull() {
        Instant threshold = Instant.parse(DATE_2023);
        Instant startInstant = Instant.parse(DATE_2020);
        Instant endInstant = Instant.parse(DATE_2024);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(100,
            startInstant, endInstant);

        // Set some entities to have null lastModified
        entities.get(0).setLastModified(null);
        entities.get(1).setLastModified(null);
        entities.get(2).setLastModified(null);

        TemporalEntityFilter filter = new TemporalEntityFilter();
        filter.setLastModified(new InstantFilter().setNotIsBefore(threshold));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(entities, filter);

        // Null field values should NOT match (return false)
        assertThat(results).allMatch(e -> e.getLastModified() != null);
    }

    // ==================== Property-Based Tests ====================

    /**
     * Property 5: Kopyalama kurucusu eşitliği
     *
     * Validates: Requirements 7.1, 7.2
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 5: Kopyalama kurucusu eşitliği")
    void copyConstructorShouldProduceEqualInstance(@ForAll("arbitraryInstantFilter") InstantFilter original) {
        // When: create a copy using the copy constructor
        InstantFilter copy = new InstantFilter(original);

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
    void equalsHashCodeInclusionAndConsistency(@ForAll("arbitraryInstantFilter") InstantFilter original,
                                               @ForAll("negatedFieldIndex") int fieldIndex,
                                               @ForAll("distinctInstant") Instant differentValue) {
        // Part 1: hashCode consistency — equal instances produce same hashCode
        InstantFilter copy = new InstantFilter(original);
        assertThat(copy).isEqualTo(original);
        assertThat(copy.hashCode()).isEqualTo(original.hashCode());

        // Part 2: Inclusion — changing one negated field makes equals return false
        InstantFilter mutated = new InstantFilter(original);
        Instant currentValue;
        switch (fieldIndex) {
            case 0:
                currentValue = mutated.getNotIsBefore();
                Instant newNbe = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue.plus(1, java.time.temporal.ChronoUnit.SECONDS) : differentValue;
                mutated.setNotIsBefore(newNbe);
                if (!java.util.Objects.equals(newNbe, original.getNotIsBefore())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 1:
                currentValue = mutated.getNotIsAfter();
                Instant newNaf = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue.plus(1, java.time.temporal.ChronoUnit.SECONDS) : differentValue;
                mutated.setNotIsAfter(newNaf);
                if (!java.util.Objects.equals(newNaf, original.getNotIsAfter())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 2:
                currentValue = mutated.getNotIsOnOrBefore();
                Instant newNobe = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue.plus(1, java.time.temporal.ChronoUnit.SECONDS) : differentValue;
                mutated.setNotIsOnOrBefore(newNobe);
                if (!java.util.Objects.equals(newNobe, original.getNotIsOnOrBefore())) {
                    assertThat(mutated).isNotEqualTo(original);
                }
                break;
            case 3:
                currentValue = mutated.getNotIsOnOrAfter();
                Instant newNoaf = (currentValue != null && currentValue.equals(differentValue))
                    ? differentValue.plus(1, java.time.temporal.ChronoUnit.SECONDS) : differentValue;
                mutated.setNotIsOnOrAfter(newNoaf);
                if (!java.util.Objects.equals(newNoaf, original.getNotIsOnOrAfter())) {
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
    void setterShouldReturnSameInstanceAndGetterShouldReturnSetValue(@ForAll("nullableInstant") Instant value) {
        InstantFilter filter = new InstantFilter();

        // setNotIsBefore: returns same instance (typed as InstantFilter) and getter returns the value
        InstantFilter result1 = filter.setNotIsBefore(value);
        assertThat(result1).isSameAs(filter);
        assertThat(result1).isInstanceOf(InstantFilter.class);
        assertThat(filter.getNotIsBefore()).isEqualTo(value);

        // setNotIsAfter: returns same instance (typed as InstantFilter) and getter returns the value
        InstantFilter result2 = filter.setNotIsAfter(value);
        assertThat(result2).isSameAs(filter);
        assertThat(result2).isInstanceOf(InstantFilter.class);
        assertThat(filter.getNotIsAfter()).isEqualTo(value);

        // setNotIsOnOrBefore: returns same instance (typed as InstantFilter) and getter returns the value
        InstantFilter result3 = filter.setNotIsOnOrBefore(value);
        assertThat(result3).isSameAs(filter);
        assertThat(result3).isInstanceOf(InstantFilter.class);
        assertThat(filter.getNotIsOnOrBefore()).isEqualTo(value);

        // setNotIsOnOrAfter: returns same instance (typed as InstantFilter) and getter returns the value
        InstantFilter result4 = filter.setNotIsOnOrAfter(value);
        assertThat(result4).isSameAs(filter);
        assertThat(result4).isInstanceOf(InstantFilter.class);
        assertThat(filter.getNotIsOnOrAfter()).isEqualTo(value);
    }

    /**
     * Property 2: Zamansal olumsuzlama ikiliği (negation duality)
     *
     * For any non-null Instant fieldValue and threshold:
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
    void temporalNegationDuality(@ForAll("arbitraryInstant") Instant fieldValue,
                                  @ForAll("arbitraryInstant") Instant threshold) {
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
    void temporalNegationDualityNullFieldReturnsFalse(@ForAll("arbitraryInstant") Instant threshold) {
        // When fieldValue is null, the generated validation code evaluates "field != null && ..."
        // which always returns false. We model this expectation directly.
        Instant fieldValue = null;

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

    /**
     * Property 3: validateFilter mantıksal VE kombinasyonu
     *
     * For any InstantFilter with a random combination of positive and negated temporal operators
     * (some null, some set) and any field value, validateFilter returns true if and only if
     * ALL non-null operators are simultaneously satisfied. Null operators do not affect the result.
     *
     * Uses SpecificationQueryEngine end-to-end with a single-element TemporalEntity list.
     *
     * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 15.3
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 3: validateFilter mantıksal VE kombinasyonu")
    void validateFilterLogicalAndCombination(
            @ForAll("rangeOnlyInstantFilter") InstantFilter lastModifiedFilter,
            @ForAll("arbitraryInstant") Instant fieldValue) {

        // Build a TemporalEntity with the given field value
        TemporalEntity entity = new TemporalEntity();
        entity.setId(1L);
        entity.setLastModified(fieldValue);

        // Compute the expected result using the model (logical AND of all non-null operators)
        boolean expected = computeExpectedTemporalResult(fieldValue, lastModifiedFilter);

        // Use SpecificationQueryEngine to apply the filter via the generated validateFilter
        TemporalEntityFilter temporalEntityFilter = new TemporalEntityFilter();
        temporalEntityFilter.setLastModified(lastModifiedFilter);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(
            Collections.singletonList(entity), temporalEntityFilter);

        if (expected) {
            assertThat(results).hasSize(1);
        } else {
            assertThat(results).isEmpty();
        }
    }

    /**
     * Computes the expected validateFilter result for a temporal (Instant) field using the model:
     * Each non-null operator must be satisfied (logical AND). Null operators are skipped.
     */
    private boolean computeExpectedTemporalResult(Instant fieldValue, InstantFilter filter) {
        // isBefore: field.isBefore(value)
        if (filter.getIsBefore() != null) {
            if (!fieldValue.isBefore(filter.getIsBefore())) return false;
        }
        // isAfter: field.isAfter(value)
        if (filter.getIsAfter() != null) {
            if (!fieldValue.isAfter(filter.getIsAfter())) return false;
        }
        // isOnOrBefore: !field.isAfter(value)
        if (filter.getIsOnOrBefore() != null) {
            if (fieldValue.isAfter(filter.getIsOnOrBefore())) return false;
        }
        // isOnOrAfter: !field.isBefore(value)
        if (filter.getIsOnOrAfter() != null) {
            if (fieldValue.isBefore(filter.getIsOnOrAfter())) return false;
        }
        // notIsBefore: !field.isBefore(value)
        if (filter.getNotIsBefore() != null) {
            if (fieldValue.isBefore(filter.getNotIsBefore())) return false;
        }
        // notIsAfter: !field.isAfter(value)
        if (filter.getNotIsAfter() != null) {
            if (fieldValue.isAfter(filter.getNotIsAfter())) return false;
        }
        // notIsOnOrBefore: field.isAfter(value)
        if (filter.getNotIsOnOrBefore() != null) {
            if (!fieldValue.isAfter(filter.getNotIsOnOrBefore())) return false;
        }
        // notIsOnOrAfter: field.isBefore(value)
        if (filter.getNotIsOnOrAfter() != null) {
            if (!fieldValue.isBefore(filter.getNotIsOnOrAfter())) return false;
        }
        return true;
    }

    /**
     * Property 4: JSON çevrimi (round-trip) korunumu
     *
     * An InstantFilter with negated fields set, when serialized to JSON and deserialized back,
     * produces an equal filter instance.
     *
     * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 4: JSON çevrimi (round-trip) korunumu")
    void jsonRoundTripPreservesNegatedFields(@ForAll("arbitraryInstantFilter") InstantFilter original) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writeValueAsString(original);
        InstantFilter deserialized = mapper.readValue(json, InstantFilter.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Provide
    Arbitrary<Instant> arbitraryInstant() {
        return Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond);
    }

    @Provide
    Arbitrary<Instant> nullableInstant() {
        return Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond)
            .injectNull(0.2);
    }

    @Provide
    Arbitrary<Integer> negatedFieldIndex() {
        return Arbitraries.integers().between(0, 3);
    }

    @Provide
    Arbitrary<Instant> distinctInstant() {
        return Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond);
    }

    @Provide
    Arbitrary<InstantFilter> arbitraryInstantFilter() {
        Arbitrary<Instant> nullableInstant = Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond)
            .injectNull(0.2);

        return Combinators.combine(
            nullableInstant, // equals
            nullableInstant, // isBefore
            nullableInstant, // isAfter
            nullableInstant, // isOnOrBefore
            nullableInstant, // isOnOrAfter
            nullableInstant, // notIsBefore
            nullableInstant, // notIsAfter
            nullableInstant  // notIsOnOrBefore
        ).as((eq, be, af, obe, oaf, nbe, naf, nobe) -> {
            InstantFilter filter = new InstantFilter();
            filter.setEquals(eq);
            filter.setIsBefore(be);
            filter.setIsAfter(af);
            filter.setIsOnOrBefore(obe);
            filter.setIsOnOrAfter(oaf);
            filter.setNotIsBefore(nbe);
            filter.setNotIsAfter(naf);
            filter.setNotIsOnOrBefore(nobe);
            return filter;
        }).flatMap(filter -> nullableInstant.map(noaf -> {
            filter.setNotIsOnOrAfter(noaf);
            return filter;
        }));
    }

    /**
     * Property 8: Geriye dönük uyumluluk (olumsuz alanlar null)
     *
     * When all negated fields are null, validateFilter result is identical to computing
     * only positive temporal operators (isBefore, isAfter, isOnOrBefore, isOnOrAfter).
     * This proves backward compatibility: when no negated operators are used, behavior is unchanged.
     *
     * Validates: Requirements 15.1, 15.2, 15.3, 6.2
     */
    @Property(tries = 100)
    @Label("Feature: filter-negated-operators, Property 8: Geriye dönük uyumluluk")
    void backwardCompatibilityWhenNegatedFieldsAreNull(
            @ForAll("positiveOnlyInstantFilter") InstantFilter lastModifiedFilter,
            @ForAll("arbitraryInstant") Instant fieldValue) {

        // Precondition: all negated fields must be null (enforced by generator)
        assertThat(lastModifiedFilter.getNotIsBefore()).isNull();
        assertThat(lastModifiedFilter.getNotIsAfter()).isNull();
        assertThat(lastModifiedFilter.getNotIsOnOrBefore()).isNull();
        assertThat(lastModifiedFilter.getNotIsOnOrAfter()).isNull();

        // Build a TemporalEntity with the given field value
        TemporalEntity entity = new TemporalEntity();
        entity.setId(1L);
        entity.setLastModified(fieldValue);

        // Use SpecificationQueryEngine to apply the filter
        TemporalEntityFilter temporalEntityFilter = new TemporalEntityFilter();
        temporalEntityFilter.setLastModified(lastModifiedFilter);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.queryByFilter(
            Collections.singletonList(entity), temporalEntityFilter);

        // Compute expected from model: only positive temporal operators (logical AND)
        boolean expected = computeExpectedPositiveOnlyTemporalResult(fieldValue, lastModifiedFilter);

        if (expected) {
            assertThat(results).hasSize(1);
        } else {
            assertThat(results).isEmpty();
        }
    }

    /**
     * Computes the expected result using ONLY positive temporal operators.
     * Negated fields are assumed null and skipped.
     */
    private boolean computeExpectedPositiveOnlyTemporalResult(Instant fieldValue, InstantFilter filter) {
        if (filter.getIsBefore() != null) {
            if (!fieldValue.isBefore(filter.getIsBefore())) return false;
        }
        if (filter.getIsAfter() != null) {
            if (!fieldValue.isAfter(filter.getIsAfter())) return false;
        }
        if (filter.getIsOnOrBefore() != null) {
            if (fieldValue.isAfter(filter.getIsOnOrBefore())) return false;
        }
        if (filter.getIsOnOrAfter() != null) {
            if (fieldValue.isBefore(filter.getIsOnOrAfter())) return false;
        }
        return true;
    }

    @Provide
    Arbitrary<InstantFilter> positiveOnlyInstantFilter() {
        Arbitrary<Instant> nullableInstant = Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond)
            .injectNull(0.3);

        return Combinators.combine(
            nullableInstant, // isBefore
            nullableInstant, // isAfter
            nullableInstant, // isOnOrBefore
            nullableInstant  // isOnOrAfter
        ).as((be, af, obe, oaf) -> {
            InstantFilter filter = new InstantFilter();
            filter.setIsBefore(be);
            filter.setIsAfter(af);
            filter.setIsOnOrBefore(obe);
            filter.setIsOnOrAfter(oaf);
            // All negated fields remain null (backward compatibility)
            return filter;
        });
    }

    @Provide
    Arbitrary<InstantFilter> rangeOnlyInstantFilter() {
        Arbitrary<Instant> nullableInstant = Arbitraries.longs()
            .between(
                Instant.parse("2000-01-01T00:00:00Z").getEpochSecond(),
                Instant.parse("2030-12-31T23:59:59Z").getEpochSecond()
            )
            .map(Instant::ofEpochSecond)
            .injectNull(0.3);

        return Combinators.combine(
            nullableInstant, // isBefore
            nullableInstant, // isAfter
            nullableInstant, // isOnOrBefore
            nullableInstant, // isOnOrAfter
            nullableInstant, // notIsBefore
            nullableInstant, // notIsAfter
            nullableInstant, // notIsOnOrBefore
            nullableInstant  // notIsOnOrAfter
        ).as((be, af, obe, oaf, nbe, naf, nobe, noaf) -> {
            InstantFilter filter = new InstantFilter();
            filter.setIsBefore(be);
            filter.setIsAfter(af);
            filter.setIsOnOrBefore(obe);
            filter.setIsOnOrAfter(oaf);
            filter.setNotIsBefore(nbe);
            filter.setNotIsAfter(naf);
            filter.setNotIsOnOrBefore(nobe);
            filter.setNotIsOnOrAfter(noaf);
            return filter;
        });
    }
}
