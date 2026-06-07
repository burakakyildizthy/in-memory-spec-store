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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for LocalDateTimeAttribute operations.
 * Tests all temporal operations using generated meta model (TemporalEntity_.createdAt).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.5, 15.10, 15.9
 */
class LocalDateTimeAttributeTest {

    private static final String CREATED_AT_FIELD = "createdAt";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesLocalDateTimeAttribute() {
        LocalDateTimeAttribute<TemporalEntity> attribute = new LocalDateTimeAttribute<>(CREATED_AT_FIELD, TemporalEntity.class);

        assertThat(attribute)
                .satisfies(a -> {
                    assertThat(a.getName()).isEqualTo(CREATED_AT_FIELD);
                    assertThat(a.getOwnerType()).isEqualTo(TemporalEntity.class);
                    assertThat(a.getFieldType()).isEqualTo(LocalDateTime.class);
                    assertThat(a.getAttributeType()).isEqualTo(com.thy.fss.common.inmemory.specification.AttributeType.SINGLE);
                });
    }

    @Test
    void testGeneratedMetaModelCreatedAtHasCorrectProperties() {
        assertThat(TemporalEntity_.createdAt)
                .isNotNull()
                .satisfies(attr -> {
                    assertThat(attr.getName()).isEqualTo(CREATED_AT_FIELD);
                    assertThat(attr.getOwnerType()).isEqualTo(TemporalEntity.class);
                    assertThat(attr.getFieldType()).isEqualTo(LocalDateTime.class);
                });
    }

    // ==================== IsBefore Operation Tests ====================

    @Test
    void testIsBeforeOperationWithMatchingDateTimes() {
        LocalDateTime beforeDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isBefore(beforeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isBefore(beforeDateTime));
    }

    @Test
    void testIsBeforeOperationWithLargeDataset() {
        LocalDateTime beforeDateTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isBefore(beforeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isBefore(beforeDateTime));
    }

    @Test
    void testIsBeforeOperationWithNullValues() {
        LocalDateTime beforeDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(100,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));
        entities.get(0).setCreatedAt(null);
        entities.get(10).setCreatedAt(null);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isBefore(beforeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null);
    }

    // ==================== IsAfter Operation Tests ====================

    @Test
    void testIsAfterOperationWithMatchingDateTimes() {
        LocalDateTime afterDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isAfter(afterDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty()
                .allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(afterDateTime));
    }

    @Test
    void testIsAfterOperationWithLargeDataset() {
        LocalDateTime afterDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isAfter(afterDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(afterDateTime));
    }

    // ==================== IsOnOrBefore Operation Tests ====================

    @Test
    void testIsOnOrBeforeOperationWithMatchingDateTimes() {
        LocalDateTime onOrBeforeDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrBefore(onOrBeforeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            (e.getCreatedAt().isBefore(onOrBeforeDateTime) || e.getCreatedAt().isEqual(onOrBeforeDateTime)));
    }

    @Test
    void testIsOnOrBeforeOperationWithLargeDataset() {
        LocalDateTime onOrBeforeDateTime = LocalDateTime.of(2022, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrBefore(onOrBeforeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getCreatedAt() != null &&
            !e.getCreatedAt().isAfter(onOrBeforeDateTime));
    }

    // ==================== IsOnOrAfter Operation Tests ====================

    @Test
    void testIsOnOrAfterOperationWithMatchingDateTimes() {
        LocalDateTime onOrAfterDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrAfter(onOrAfterDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            (e.getCreatedAt().isAfter(onOrAfterDateTime) || e.getCreatedAt().isEqual(onOrAfterDateTime)));
    }

    @Test
    void testIsOnOrAfterOperationWithLargeDataset() {
        LocalDateTime onOrAfterDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrAfter(onOrAfterDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(1000).allMatch(e -> e.getCreatedAt() != null &&
            !e.getCreatedAt().isBefore(onOrAfterDateTime));
    }

    // ==================== Between Operation Tests ====================

    @Test
    void testBetweenOperationWithDateTimeRange() {
        LocalDateTime startDateTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 12, 31, 23, 59);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrAfter(startDateTime)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .isOnOrBefore(endDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            !e.getCreatedAt().isBefore(startDateTime) &&
            !e.getCreatedAt().isAfter(endDateTime));
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationWithMatchingDateTime() {
        LocalDateTime targetDateTime = LocalDateTime.of(2023, 6, 15, 12, 30);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        // Garanti olarak hedef zamanı içeren bir entity ekle
        TemporalEntity matching = new TemporalEntity();
        matching.setCreatedAt(targetDateTime);
        entities.add(matching);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .equalTo(targetDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(targetDateTime));
    }

    @Test
    void testEqualsOperationWithLargeDataset() {
        LocalDateTime targetDateTime = LocalDateTime.of(2022, 6, 15, 10, 15);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        TemporalEntity matching = new TemporalEntity();
        matching.setCreatedAt(targetDateTime);
        entities.add(matching);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .equalTo(targetDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(targetDateTime));
    }
    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationWithExcludedDateTime() {
        LocalDateTime excludeDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .notEqualTo(excludeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() == null || !e.getCreatedAt().equals(excludeDateTime));
    }

    @Test
    void testNotEqualsOperationWithLargeDataset() {
        LocalDateTime excludeDateTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .notEqualTo(excludeDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSizeGreaterThan(9000).allMatch(e -> e.getCreatedAt() == null || !e.getCreatedAt().equals(excludeDateTime));
    }

    // ==================== In Operation Tests ====================
    @Test
    void testInOperationWithMultipleDateTimes() {
        LocalDateTime dateTime1 = LocalDateTime.of(2023, 1, 15, 10, 0);
        LocalDateTime dateTime2 = LocalDateTime.of(2023, 6, 15, 14, 30);
        LocalDateTime dateTime3 = LocalDateTime.of(2023, 12, 15, 18, 45);
        List<LocalDateTime> dateTimes = Arrays.asList(dateTime1, dateTime2, dateTime3);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        // Her tarihe garanti entity ekle
        for (LocalDateTime d : dateTimes) {
            TemporalEntity matching = new TemporalEntity();
            matching.setCreatedAt(d);
            entities.add(matching);
        }

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .in(dateTimes);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && dateTimes.contains(e.getCreatedAt()));
    }

    @Test
    void testInOperationWithLargeDataset() {
        LocalDateTime dateTime1 = LocalDateTime.of(2022, 3, 15, 9, 0);
        LocalDateTime dateTime2 = LocalDateTime.of(2022, 6, 15, 12, 0);
        LocalDateTime dateTime3 = LocalDateTime.of(2022, 9, 15, 15, 0);
        LocalDateTime dateTime4 = LocalDateTime.of(2022, 12, 15, 18, 0);
        List<LocalDateTime> dateTimes = Arrays.asList(dateTime1, dateTime2, dateTime3, dateTime4);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        // Her tarihe garanti entity ekle
        for (LocalDateTime d : dateTimes) {
            TemporalEntity matching = new TemporalEntity();
            matching.setCreatedAt(d);
            entities.add(matching);
        }

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .in(dateTimes);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && dateTimes.contains(e.getCreatedAt()));
    }
    // ==================== Time Precision Tests ====================

    @Test
    void testTimePrecisionWithSecondsAndNanos() {
        LocalDateTime dateTime1 = LocalDateTime.of(2023, 6, 15, 12, 30, 45);
        LocalDateTime dateTime2 = LocalDateTime.of(2023, 6, 15, 12, 30, 46);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(100,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        entities.get(0).setCreatedAt(dateTime1);
        entities.get(1).setCreatedAt(dateTime2);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .equalTo(dateTime1);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCreatedAt()).isEqualTo(dateTime1);
    }

    @Test
    void testTimePrecisionWithLargeDataset() {
        LocalDateTime startDateTime = LocalDateTime.of(2023, 6, 15, 12, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 6, 15, 13, 0, 0);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
                LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        // Test aralığında bir entity ekle (garanti aralıkta)
        TemporalEntity guaranteed = new TemporalEntity();
        guaranteed.setCreatedAt(LocalDateTime.of(2023, 6, 15, 12, 30, 0));
        entities.add(guaranteed);

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .isOnOrAfter(startDateTime)
                .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                        .where(TemporalEntity_.createdAt)
                        .isBefore(endDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
                !e.getCreatedAt().isBefore(startDateTime) &&
                e.getCreatedAt().isBefore(endDateTime));
    }

    // ==================== Complex Combination Tests ====================

    @Test
    void testComplexCombinationWithMultipleConditions() {
        LocalDateTime startDateTime = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 12, 31, 23, 59);
        LocalDateTime excludeDateTime = LocalDateTime.of(2023, 6, 15, 12, 0);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isOnOrAfter(startDateTime)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .isOnOrBefore(endDateTime))
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .notEqualTo(excludeDateTime));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty().allMatch(e -> e.getCreatedAt() != null &&
            !e.getCreatedAt().isBefore(startDateTime) &&
            !e.getCreatedAt().isAfter(endDateTime) &&
            !e.getCreatedAt().equals(excludeDateTime));
    }

    @Test
    void testComplexCombinationWithLargeDataset() {
        LocalDateTime dateTime1 = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime dateTime2 = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime dateTime3 = LocalDateTime.of(2024, 1, 1, 0, 0);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        Specification<TemporalEntity> spec = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .isAfter(dateTime1)
            .and(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .isBefore(dateTime3))
            .or(SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
                .where(TemporalEntity_.createdAt)
                .equalTo(dateTime2));

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> results = engine.query(entities, spec);

        assertThat(results).isNotEmpty()
                .allMatch(e -> e.getCreatedAt() != null);
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testBoundaryValuesMinAndMaxDateTimes() {
        LocalDateTime minDateTime = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime maxDateTime = LocalDateTime.of(2024, 12, 31, 23, 59);

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(10_000, minDateTime, maxDateTime);
        
        entities.get(0).setCreatedAt(minDateTime);
        entities.get(1).setCreatedAt(minDateTime);
        entities.get(2).setCreatedAt(maxDateTime);
        entities.get(3).setCreatedAt(maxDateTime);

        Specification<TemporalEntity> specMin = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .equalTo(minDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.query(entities, specMin);

        assertThat(minResults).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(minDateTime));

        Specification<TemporalEntity> specMax = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .equalTo(maxDateTime);

        List<TemporalEntity> maxResults = engine.query(entities, specMax);

        assertThat(maxResults).isNotEmpty().allMatch(e -> e.getCreatedAt() != null && e.getCreatedAt().equals(maxDateTime));
    }

    @Test
    void testBoundaryValuesEdgeCases() {
        LocalDateTime minDateTime = LocalDateTime.MIN;
        LocalDateTime maxDateTime = LocalDateTime.MAX;

        List<TemporalEntity> entities = LargeDatasetGenerator.generateTemporalEntitiesWithDateTime(100,
            LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59));

        entities.get(0).setCreatedAt(minDateTime);
        entities.get(1).setCreatedAt(maxDateTime);

        Specification<TemporalEntity> specMin = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .equalTo(minDateTime);

        SpecificationQueryEngine<TemporalEntity> engine = new SpecificationQueryEngine<>(TemporalEntity.class);
        List<TemporalEntity> minResults = engine.query(entities, specMin);

        assertThat(minResults).hasSize(1);
        assertThat(minResults.get(0).getCreatedAt()).isEqualTo(minDateTime);

        Specification<TemporalEntity> specMax = SpecificationBuilder.forService(TemporalEntitySpecificationService.INSTANCE)
            .where(TemporalEntity_.createdAt)
            .equalTo(maxDateTime);

        List<TemporalEntity> maxResults = engine.query(entities, specMax);

        assertThat(maxResults).hasSize(1);
        assertThat(maxResults.get(0).getCreatedAt()).isEqualTo(maxDateTime);
    }
}
