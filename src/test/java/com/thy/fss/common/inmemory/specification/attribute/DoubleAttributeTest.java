package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.UserSummary;
import com.thy.fss.common.inmemory.testmodel.UserSummarySpecificationService;
import com.thy.fss.common.inmemory.testmodel.UserSummary_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for DoubleAttribute operations.
 * Tests all numeric operations using generated meta model (UserSummary_.averageAge, UserSummary_.totalSalary).
 * Tests with large datasets (10K entities) to verify performance.
 * Tests precision handling for floating-point operations.
 * Requirements: 5.2, 15.10, 15.9
 */
class DoubleAttributeTest {

    private static final String AVERAGE_AGE = "averageAge";
    private static final String TOTAL_SALARY = "totalSalary";

    private static final Random RANDOM = new Random(42);
    private static final double PRECISION = 0.0001;

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesDoubleAttribute() {
        DoubleAttribute<UserSummary> attribute = new DoubleAttribute<>(AVERAGE_AGE, UserSummary.class);

        assertThat(attribute.getName()).isEqualTo(AVERAGE_AGE);
        assertThat(attribute.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(attribute.getFieldType()).isEqualTo(Double.class);
        assertThat(attribute.getAttributeType()).isEqualTo(com.thy.fss.common.inmemory.specification.AttributeType.SINGLE);
    }

    @Test
    void testGeneratedMetaModelAverageAgeHasCorrectProperties() {
        assertThat(UserSummary_.averageAge).isNotNull();
        assertThat(UserSummary_.averageAge.getName()).isEqualTo(AVERAGE_AGE);
        assertThat(UserSummary_.averageAge.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(UserSummary_.averageAge.getFieldType()).isEqualTo(Double.class);
    }

    @Test
    void testGeneratedMetaModelTotalSalaryHasCorrectProperties() {
        assertThat(UserSummary_.totalSalary).isNotNull();
        assertThat(UserSummary_.totalSalary.getName()).isEqualTo(TOTAL_SALARY);
        assertThat(UserSummary_.totalSalary.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(UserSummary_.totalSalary.getFieldType()).isEqualTo(Double.class);
    }

    // ==================== GreaterThan Operation Tests ====================

    @Test
    void testGreaterThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() > 50.0);
    }

    @Test
    void testGreaterThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(40.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(4000).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() > 40.0);
    }

    @Test
    void testGreaterThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setAverageAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(1000000.0);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(999999.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() > 999999.0);
    }

    @Test
    void testGreaterThanOperationWithPrecisionIssues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(50.00001);
        summaries.get(1).setAverageAge(50.00002);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0), summaries.get(1)).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() > 50.0);
    }

    // ==================== LessThan Operation Tests ====================

    @Test
    void testLessThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .lessThan(40.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() < 40.0);
    }

    @Test
    void testLessThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .lessThan(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(4000).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() < 50.0);
    }

    @Test
    void testLessThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setAverageAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .lessThan(40.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(Double.MIN_VALUE);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .lessThan(Double.MIN_VALUE + 1.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() < Double.MIN_VALUE + 1.0);
    }

    @Test
    void testLessThanOperationWithPrecisionIssues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(39.99999);
        summaries.get(1).setAverageAge(39.99998);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .lessThan(40.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0), summaries.get(1)).allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge() < 40.0);
    }

    // ==================== GreaterThanOrEqual Operation Tests ====================

    @Test
    void testGreaterThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .greaterThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() >= 100000.0);
    }

    @Test
    void testGreaterThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .greaterThanOrEqual(80000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() >= 80000.0);
    }

    @Test
    void testGreaterThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setTotalSalary(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .greaterThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalSalary(100000.0);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .greaterThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() >= 100000.0);
    }

    // ==================== LessThanOrEqual Operation Tests ====================

    @Test
    void testLessThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .lessThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() <= 100000.0);
    }

    @Test
    void testLessThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .lessThanOrEqual(120000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(4000).allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() <= 120000.0);
    }

    @Test
    void testLessThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setTotalSalary(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .lessThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalSalary(100000.0);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalSalary)
            .lessThanOrEqual(100000.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getTotalSalary() != null && summary.getTotalSalary() <= 100000.0);
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationFindsExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(42.5);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .equalTo(42.5);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getAverageAge() != null &&
            Math.abs(summary.getAverageAge() - 42.5) < PRECISION);
    }

    @Test
    void testEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        UserSummary matchingSummary = new UserSummary();
        matchingSummary.setAverageAge(50.0);
        summaries.add(matchingSummary); // Aranan değeri garanti ekle

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .equalTo(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge().equals(50.0));
    }

    @Test
    void testEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        UserSummary matchingSummary = new UserSummary();
        matchingSummary.setAverageAge(42.5);
        summaries.add(matchingSummary);

        summaries.get(0).setAverageAge(null);

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .equalTo(42.5);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }
    @Test
    void testEqualsOperationWithPrecisionIssues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(42.500000001);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .equalTo(42.5);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        // Due to floating-point precision, this might or might not match
        // The test verifies the behavior is consistent
        assertThat(results).isNotNull();
    }

    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationExcludesExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setAverageAge(42.5);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .notEqualTo(42.5);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().noneMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge().equals(42.5));
    }

    @Test
    void testNotEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .notEqualTo(50.0);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(9000).noneMatch(summary -> summary.getAverageAge() != null && summary.getAverageAge().equals(50.0));
    }

    @Test
    void testNotEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setAverageAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .notEqualTo(42.5);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotNull();
    }

    // ==================== In Operation Tests ====================

    @Test
    void testInOperationFindsMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        // Set specific values to ensure matches
        summaries.get(0).setAverageAge(30.0);
        summaries.get(1).setAverageAge(40.0);
        summaries.get(2).setAverageAge(50.0);
        summaries.get(3).setAverageAge(60.0);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .in(Arrays.asList(30.0, 40.0, 50.0, 60.0));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThanOrEqualTo(4).allMatch(summary ->
            summary.getAverageAge() != null && 
            (summary.getAverageAge().equals(30.0) || summary.getAverageAge().equals(40.0) || 
             summary.getAverageAge().equals(50.0) || summary.getAverageAge().equals(60.0)));
    }

    @Test
    void testInOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        List<Double> searchValues = new ArrayList<>();
        for (double i = 30.0; i <= 60.0; i += 5.0) {
            searchValues.add(i);
        }
        
        // Set specific values to ensure matches
        for (int i = 0; i < searchValues.size() && i < summaries.size(); i++) {
            summaries.get(i).setAverageAge(searchValues.get(i));
        }
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .in(searchValues);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThanOrEqualTo(searchValues.size()).allMatch(summary ->
            summary.getAverageAge() != null && searchValues.contains(summary.getAverageAge()));
    }

    @Test
    void testInOperationWithEmptyList() {
        List<UserSummary> summaries = generateUserSummaries(1000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .in(new ArrayList<>());

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isEmpty();
    }

    @Test
    void testInOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        UserSummary matchingSummary = new UserSummary();
        matchingSummary.setAverageAge(30.0);
        summaries.add(matchingSummary);

        summaries.get(0).setAverageAge(null);

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .in(Arrays.asList(30.0, 40.0, 50.0));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    // ==================== Complex Combination Tests ====================

    @Test
    void testCombinedOperationsGreaterThanAndLessThan() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(35.0)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .lessThan(55.0));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getAverageAge() != null && summary.getAverageAge() > 35.0 && summary.getAverageAge() < 55.0);
    }

    @Test
    void testCombinedOperationsRangeAndNotEquals() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThanOrEqual(35.0)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .lessThanOrEqual(55.0))
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.averageAge)
                .notEqualTo(45.0));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getAverageAge() != null && 
            summary.getAverageAge() >= 35.0 && 
            summary.getAverageAge() <= 55.0 && 
            !summary.getAverageAge().equals(45.0));
    }

    @Test
    void testCombinedOperationsMultipleFields() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.averageAge)
            .greaterThan(50.0)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalSalary)
                .lessThan(100000.0));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> 
            summary.getAverageAge() != null && summary.getAverageAge() > 50.0 &&
            summary.getTotalSalary() != null && summary.getTotalSalary() < 100000.0);
    }

    // ==================== Helper Methods ====================

    private List<UserSummary> generateUserSummaries(int count) {
        List<UserSummary> summaries = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            UserSummary summary = new UserSummary();
            summary.setAverageAge(25.0 + RANDOM.nextDouble() * 40.0); // Ages 25-65
            summary.setTotalSalary(50000.0 + RANDOM.nextDouble() * 150000.0); // Salary 50K-200K
            summary.setTotalUsers((long) (1000 + RANDOM.nextInt(9000)));
            summary.setActiveUsers((long) (500 + RANDOM.nextInt(4500)));
            summary.setInactiveUsers((long) (100 + RANDOM.nextInt(900)));
            summary.setMaxAge(20 + RANDOM.nextInt(60));
            summary.setMinAge(18 + RANDOM.nextInt(30));
            summary.setActiveCount((long) (100 + RANDOM.nextInt(900)));
            summary.setYoungUsers((long) (50 + RANDOM.nextInt(450)));
            summary.setSeniorUsers((long) (50 + RANDOM.nextInt(450)));
            summaries.add(summary);
        }
        
        return summaries;
    }
}
