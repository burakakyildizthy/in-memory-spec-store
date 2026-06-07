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
 * Comprehensive tests for IntegerAttribute operations.
 * Tests all numeric operations using generated meta model (UserSummary_.maxAge, UserSummary_.minAge).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.2, 15.10, 15.9
 */
class IntegerAttributeTest {

    private static final Random RANDOM = new Random(42);
    private static final String MAX_AGE_FIELD = "maxAge";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesIntegerAttribute() {
        IntegerAttribute<UserSummary> attribute = new IntegerAttribute<>(MAX_AGE_FIELD, UserSummary.class);

        assertThat(attribute.getName()).isEqualTo(MAX_AGE_FIELD);
        assertThat(attribute.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(attribute.getFieldType()).isEqualTo(Integer.class);
        assertThat(attribute.getAttributeType()).isEqualTo(com.thy.fss.common.inmemory.specification.AttributeType.SINGLE);
    }

    @Test
    void testGeneratedMetaModelMaxAgeHasCorrectProperties() {
        assertThat(UserSummary_.maxAge).isNotNull();
        assertThat(UserSummary_.maxAge.getName()).isEqualTo(MAX_AGE_FIELD);
        assertThat(UserSummary_.maxAge.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(UserSummary_.maxAge.getFieldType()).isEqualTo(Integer.class);
    }

    @Test
    void testGeneratedMetaModelMinAgeHasCorrectProperties() {
        assertThat(UserSummary_.minAge).isNotNull();
        assertThat(UserSummary_.minAge.getName()).isEqualTo("minAge");
        assertThat(UserSummary_.minAge.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(UserSummary_.minAge.getFieldType()).isEqualTo(Integer.class);
    }

    // ==================== GreaterThan Operation Tests ====================

    @Test
    void testGreaterThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() > 50);
    }

    @Test
    void testGreaterThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() > 30);
    }

    @Test
    void testGreaterThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMaxAge(Integer.MAX_VALUE);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(Integer.MAX_VALUE - 1);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() > Integer.MAX_VALUE - 1);
    }

    // ==================== LessThan Operation Tests ====================

    @Test
    void testLessThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThan(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() < 30);
    }

    @Test
    void testLessThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThan(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() < 50);
    }

    @Test
    void testLessThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMinAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThan(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMinAge(Integer.MIN_VALUE);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThan(Integer.MIN_VALUE + 1);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() < Integer.MIN_VALUE + 1);
    }

    // ==================== GreaterThanOrEqual Operation Tests ====================

    @Test
    void testGreaterThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() >= 50);
    }

    @Test
    void testGreaterThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(40);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() >= 40);
    }

    @Test
    void testGreaterThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMaxAge(50);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge() >= 50);
    }

    // ==================== LessThanOrEqual Operation Tests ====================

    @Test
    void testLessThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThanOrEqual(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() <= 30);
    }

    @Test
    void testLessThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThanOrEqual(40);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() <= 40);
    }

    @Test
    void testLessThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMinAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThanOrEqual(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMinAge(30);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.minAge)
            .lessThanOrEqual(30);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getMinAge() != null && summary.getMinAge() <= 30);
    }

    // ==================== Between Operation Tests (using greaterThanOrEqual + lessThanOrEqual) ====================

    @Test
    void testBetweenOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(30)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThanOrEqual(50));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && summary.getMaxAge() >= 30 && summary.getMaxAge() <= 50);
    }

    @Test
    void testBetweenOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(20)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThanOrEqual(60));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary ->
            summary.getMaxAge() != null && summary.getMaxAge() >= 20 && summary.getMaxAge() <= 60);
    }

    @Test
    void testBetweenOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(30)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThanOrEqual(50));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testBetweenOperationWithBoundaryValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMaxAge(30);
        summaries.get(1).setMaxAge(50);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(30)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThanOrEqual(50));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0), summaries.get(1)).allMatch(summary ->
            summary.getMaxAge() != null && summary.getMaxAge() >= 30 && summary.getMaxAge() <= 50);
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationFindsExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMaxAge(42);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .equalTo(42);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge().equals(42));
    }

    @Test
    void testEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .equalTo(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge().equals(50));
    }

    @Test
    void testEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        summaries.get(1).setMaxAge(42);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .equalTo(42);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationExcludesExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setMaxAge(42);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .notEqualTo(42);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().noneMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge().equals(42));
    }

    @Test
    void testNotEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .notEqualTo(50);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(9000).noneMatch(summary -> summary.getMaxAge() != null && summary.getMaxAge().equals(50));
    }

    @Test
    void testNotEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .notEqualTo(42);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotNull();
    }

    // ==================== In Operation Tests ====================

    @Test
    void testInOperationFindsMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .in(Arrays.asList(25, 35, 45, 55));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && 
            (summary.getMaxAge() == 25 || summary.getMaxAge() == 35 || 
             summary.getMaxAge() == 45 || summary.getMaxAge() == 55));
    }

    @Test
    void testInOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        List<Integer> searchValues = new ArrayList<>();
        for (int i = 20; i <= 60; i += 5) {
            searchValues.add(i);
        }
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .in(searchValues);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && searchValues.contains(summary.getMaxAge()));
    }

    @Test
    void testInOperationWithEmptyList() {
        List<UserSummary> summaries = generateUserSummaries(1000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .in(new ArrayList<>());

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isEmpty();
    }

    @Test
    void testInOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setMaxAge(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .in(Arrays.asList(25, 35, 45));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    // ==================== Complex Combination Tests ====================

    @Test
    void testCombinedOperationsGreaterThanAndLessThan() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(30)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThan(60));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && summary.getMaxAge() > 30 && summary.getMaxAge() < 60);
    }

    @Test
    void testCombinedOperationsRangeAndNotEquals() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThanOrEqual(30)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .lessThanOrEqual(60))
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.maxAge)
                .notEqualTo(45));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && 
            summary.getMaxAge() >= 30 && 
            summary.getMaxAge() <= 60 && 
            !summary.getMaxAge().equals(45));
    }

    @Test
    void testCombinedOperationsMultipleFields() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.maxAge)
            .greaterThan(50)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.minAge)
                .lessThan(30));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getMaxAge() != null && summary.getMaxAge() > 50 &&
            summary.getMinAge() != null && summary.getMinAge() < 30);
    }

    // ==================== Helper Methods ====================

    private List<UserSummary> generateUserSummaries(int count) {
        List<UserSummary> summaries = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            UserSummary summary = new UserSummary();
            summary.setMaxAge(20 + RANDOM.nextInt(60)); // Ages 20-79
            summary.setMinAge(18 + RANDOM.nextInt(30)); // Ages 18-47
            summary.setTotalUsers((long) (1000 + RANDOM.nextInt(9000)));
            summary.setActiveUsers((long) (500 + RANDOM.nextInt(4500)));
            summary.setInactiveUsers((long) (100 + RANDOM.nextInt(900)));
            summary.setAverageAge(25.0 + RANDOM.nextDouble() * 40.0);
            summary.setTotalSalary(50000.0 + RANDOM.nextDouble() * 150000.0);
            summary.setActiveCount((long) (100 + RANDOM.nextInt(900)));
            summary.setYoungUsers((long) (50 + RANDOM.nextInt(450)));
            summary.setSeniorUsers((long) (50 + RANDOM.nextInt(450)));
            summaries.add(summary);
        }
        
        return summaries;
    }
}
