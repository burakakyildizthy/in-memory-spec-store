package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity;
import com.thy.fss.common.inmemory.testmodel.TemporalEntitySpecificationService;
import com.thy.fss.common.inmemory.testmodel.TemporalEntity_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for LocalDateAttribute operations.
 * Tests all temporal operations using generated meta model (TemporalEntity_.birthDate).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.5, 15.10, 15.9
 */
class LocalDateAttributeTest {

    private static final String BIRTH_DATE_FIELD = "birthDate";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesLocalDateAttribute() {
        LocalDateAttribute<TemporalEntity> attribute = new LocalDateAttribute<>(BIRTH_DATE_FIELD, TemporalEntity.class);

        assertThat(attribute)
                .satisfies(a -> {
                    assertThat(a.getName()).isEqualTo(BIRTH_DATE_FIELD);
                    assertThat(a.getOwnerType()).isEqualTo(TemporalEntity.class);
                    assertThat(a.getFieldType()).isEqualTo(LocalDate.class);
                    assertThat(a.getAttributeType()).isEqualTo(com.thy.fss.common.inmemory.specification.AttributeType.SINGLE);
                });
    }

    @Test
    void testGeneratedMetaModelBirthDateHasCorrectProperties() {
        assertThat(TemporalEntity_.birthDate)
                .isNotNull()
                .satisfies(attr -> {
                    assertThat(attr.getName()).isEqualTo(BIRTH_DATE_FIELD);
                    assertThat(attr.getOwnerType()).isEqualTo(TemporalEntity.class);
                    assertThat(attr.getFieldType()).isEqualTo(LocalDate.class);
                });
    }

    // ==================== IsBefore Operation Tests ====================

    @Test
    void testIsBeforeOperationWithMatchingDates() {
        LocalDate beforeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isBefore(beforeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty()
                .allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isBefore(beforeDate));
    }

    @Test
    void testIsBeforeOperationWithLargeDataset() {
        LocalDate beforeDate = LocalDate.of(2022, 1, 1);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isBefore(beforeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000)
                .allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isBefore(beforeDate));
    }

    @Test
    void testIsBeforeOperationWithNullValues() {
        LocalDate beforeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(100,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));
        entities.get(0).setBirthDate(null);
        entities.get(10).setBirthDate(null);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isBefore(beforeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null);
    }

    // ==================== IsAfter Operation Tests ====================

    @Test
    void testIsAfterOperationWithMatchingDates() {
        LocalDate afterDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isAfter(afterDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty()
                .allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isAfter(afterDate));
    }

    @Test
    void testIsAfterOperationWithLargeDataset() {
        LocalDate afterDate = LocalDate.of(2023, 1, 1);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isAfter(afterDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getBirthDate() != null && e.getBirthDate().isAfter(afterDate));
    }

    // ==================== IsOnOrBefore Operation Tests ====================

    @Test
    void testIsOnOrBeforeOperationWithMatchingDates() {
        LocalDate onOrBeforeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrBefore(onOrBeforeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            (e.getBirthDate().isBefore(onOrBeforeDate) || e.getBirthDate().isEqual(onOrBeforeDate)));
    }

    @Test
    void testIsOnOrBeforeOperationWithLargeDataset() {
        LocalDate onOrBeforeDate = LocalDate.of(2022, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrBefore(onOrBeforeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getBirthDate() != null &&
            !e.getBirthDate().isAfter(onOrBeforeDate));
    }

    // ==================== IsOnOrAfter Operation Tests ====================

    @Test
    void testIsOnOrAfterOperationWithMatchingDates() {
        LocalDate onOrAfterDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrAfter(onOrAfterDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            (e.getBirthDate().isAfter(onOrAfterDate) || e.getBirthDate().isEqual(onOrAfterDate)));
    }

    @Test
    void testIsOnOrAfterOperationWithLargeDataset() {
        LocalDate onOrAfterDate = LocalDate.of(2023, 1, 1);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrAfter(onOrAfterDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getBirthDate() != null &&
            !e.getBirthDate().isBefore(onOrAfterDate));
    }

    // ==================== Between Operation Tests ====================

    @Test
    void testBetweenOperationWithDateRange() {
        LocalDate startDate = LocalDate.of(2022, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrAfter(startDate)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.birthDate)
                .isOnOrBefore(endDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            !e.getBirthDate().isBefore(startDate) &&
            !e.getBirthDate().isAfter(endDate));
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationWithMatchingDate() {
        LocalDate targetDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(targetDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(targetDate));
    }

    @Test
    void testEqualsOperationWithLargeDataset() {
        LocalDate targetDate = LocalDate.of(2022, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(targetDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(targetDate));
    }

    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationWithExcludedDate() {
        LocalDate excludeDate = LocalDate.of(2023, 6, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .notEqualTo(excludeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() == null || !e.getBirthDate().equals(excludeDate));
    }

    @Test
    void testNotEqualsOperationWithLargeDataset() {
        LocalDate excludeDate = LocalDate.of(2022, 1, 1);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .notEqualTo(excludeDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(9000).allMatch(e -> e.getBirthDate() == null || !e.getBirthDate().equals(excludeDate));
    }

    // ==================== In Operation Tests ====================

    @Test
    void testInOperationWithMultipleDates() {
        LocalDate date1 = LocalDate.of(2023, 1, 15);
        LocalDate date2 = LocalDate.of(2023, 6, 15);
        LocalDate date3 = LocalDate.of(2023, 12, 15);
        List<LocalDate> dates = Arrays.asList(date1, date2, date3);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .in(dates);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && dates.contains(e.getBirthDate()));
    }

    @Test
    void testInOperationWithLargeDataset() {
        LocalDate date1 = LocalDate.of(2022, 3, 15);
        LocalDate date2 = LocalDate.of(2022, 6, 15);
        LocalDate date3 = LocalDate.of(2022, 9, 15);
        LocalDate date4 = LocalDate.of(2022, 12, 15);
        List<LocalDate> dates = Arrays.asList(date1, date2, date3, date4);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .in(dates);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null && dates.contains(e.getBirthDate()));
    }

    // ==================== Complex Combination Tests ====================

    @Test
    void testComplexCombinationWithMultipleConditions() {
        LocalDate startDate = LocalDate.of(2022, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        LocalDate excludeDate = LocalDate.of(2023, 6, 15);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isOnOrAfter(startDate)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.birthDate)
                .isOnOrBefore(endDate))
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.birthDate)
                .notEqualTo(excludeDate));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null &&
            !e.getBirthDate().isBefore(startDate) &&
            !e.getBirthDate().isAfter(endDate) &&
            !e.getBirthDate().equals(excludeDate));
    }

    @Test
    void testComplexCombinationWithLargeDataset() {
        LocalDate date1 = LocalDate.of(2022, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 1, 1);
        LocalDate date3 = LocalDate.of(2024, 1, 1);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .isAfter(date1)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.birthDate)
                .isBefore(date3))
            .or(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.birthDate)
                .equalTo(date2));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getBirthDate() != null);
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testBoundaryValuesMinAndMaxDates() {
        LocalDate minDate = LocalDate.of(2020, 1, 1);
        LocalDate maxDate = LocalDate.of(2024, 12, 31);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(10_000, minDate, maxDate);

        Specification<TemporalEntity> specMin = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(minDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.query(entities, specMin);

        assertThat(minResults).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(minDate));

        Specification<TemporalEntity> specMax = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(maxDate);

        List<TemporalEntity> maxResults = engine.query(entities, specMax);

        assertThat(maxResults).isNotEmpty().allMatch(e -> e.getBirthDate() != null && e.getBirthDate().equals(maxDate));
    }

    @Test
    void testBoundaryValuesEdgeCases() {
        LocalDate minDate = LocalDate.MIN;
        LocalDate maxDate = LocalDate.MAX;

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntities(100,
            LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31));

        entities.get(0).setBirthDate(minDate);
        entities.get(1).setBirthDate(maxDate);

        Specification<TemporalEntity> specMin = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(minDate);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.query(entities, specMin);

        assertThat(minResults).hasSize(1);
        assertThat(minResults.get(0).getBirthDate()).isEqualTo(minDate);

        Specification<TemporalEntity> specMax = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.birthDate)
            .equalTo(maxDate);

        List<TemporalEntity> maxResults = engine.query(entities, specMax);

        assertThat(maxResults).hasSize(1);
        assertThat(maxResults.get(0).getBirthDate()).isEqualTo(maxDate);
    }
}
