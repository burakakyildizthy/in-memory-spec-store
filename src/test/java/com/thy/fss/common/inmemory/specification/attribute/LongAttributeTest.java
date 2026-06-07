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
 * Comprehensive tests for LongAttribute operations.
 * Tests all numeric operations using generated meta model (UserSummary_.totalUsers, UserSummary_.activeUsers).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.2, 15.10, 15.9
 */
class LongAttributeTest {

    private static final Random RANDOM = new Random(42);
    private static final String TOTAL_USERS_FIELD = "totalUsers";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesLongAttribute() {
        LongAttribute<UserSummary> attribute = new LongAttribute<>(TOTAL_USERS_FIELD, UserSummary.class);

        assertThat(attribute)
                .satisfies(a -> {
                    assertThat(a.getName()).isEqualTo(TOTAL_USERS_FIELD);
                    assertThat(a.getOwnerType()).isEqualTo(UserSummary.class);
                    assertThat(a.getFieldType()).isEqualTo(Long.class);
                    assertThat(a.getAttributeType()).isEqualTo(com.thy.fss.common.inmemory.specification.AttributeType.SINGLE);
                });
    }

    @Test
    void testGeneratedMetaModelTotalUsersHasCorrectProperties() {
        assertThat(UserSummary_.totalUsers)
                .isNotNull()
                .satisfies(attr -> {
                    assertThat(attr.getName()).isEqualTo(TOTAL_USERS_FIELD);
                    assertThat(attr.getOwnerType()).isEqualTo(UserSummary.class);
                    assertThat(attr.getFieldType()).isEqualTo(Long.class);
                });
    }

    @Test
    void testGeneratedMetaModelActiveUsersHasCorrectProperties() {
        assertThat(UserSummary_.activeUsers).isNotNull();
        assertThat(UserSummary_.activeUsers.getName()).isEqualTo("activeUsers");
        assertThat(UserSummary_.activeUsers.getOwnerType()).isEqualTo(UserSummary.class);
        assertThat(UserSummary_.activeUsers.getFieldType()).isEqualTo(Long.class);
    }

    // ==================== GreaterThan Operation Tests ====================

    @Test
    void testGreaterThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty()
                .allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() > 5000L);
    }

    @Test
    void testGreaterThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() > 3000L);
    }

    @Test
    void testGreaterThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setTotalUsers(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalUsers(Long.MAX_VALUE);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(Long.MAX_VALUE - 1);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() > Long.MAX_VALUE - 1);
    }

    // ==================== LessThan Operation Tests ====================

    @Test
    void testLessThanOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThan(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() < 3000L);
    }

    @Test
    void testLessThanOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThan(4000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() < 4000L);
    }

    @Test
    void testLessThanOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setActiveUsers(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThan(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOperationWithBoundaryValue() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setActiveUsers(Long.MIN_VALUE);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThan(Long.MIN_VALUE + 1);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() < Long.MIN_VALUE + 1);
    }

    // ==================== GreaterThanOrEqual Operation Tests ====================

    @Test
    void testGreaterThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThanOrEqual(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() >= 5000L);
    }

    @Test
    void testGreaterThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThanOrEqual(4000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() >= 4000L);
    }

    @Test
    void testGreaterThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setTotalUsers(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThanOrEqual(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testGreaterThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalUsers(5000L);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThanOrEqual(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers() >= 5000L);
    }

    // ==================== LessThanOrEqual Operation Tests ====================

    @Test
    void testLessThanOrEqualOperationWithMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThanOrEqual(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() <= 3000L);
    }

    @Test
    void testLessThanOrEqualOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThanOrEqual(4000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(5000).allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() <= 4000L);
    }

    @Test
    void testLessThanOrEqualOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setActiveUsers(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThanOrEqual(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    @Test
    void testLessThanOrEqualOperationWithExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setActiveUsers(3000L);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.activeUsers)
            .lessThanOrEqual(3000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).contains(summaries.get(0)).allMatch(summary -> summary.getActiveUsers() != null && summary.getActiveUsers() <= 3000L);
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationFindsExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalUsers(4242L);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .equalTo(4242L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers().equals(4242L));
    }

    @Test
    void testEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);

        // Aranan değeri içeren UserSummary ekle
        UserSummary matchingSummary = new UserSummary();
        matchingSummary.setTotalUsers(5000L);
        summaries.add(matchingSummary);

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .equalTo(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers().equals(5000L));
    }

    @Test
    void testEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);

        // Aranan değeri içeren UserSummary ekle
        UserSummary matchingSummary = new UserSummary();
        matchingSummary.setTotalUsers(4242L);
        summaries.add(matchingSummary);

        summaries.get(0).setTotalUsers(null);

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .equalTo(4242L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }

    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationExcludesExactMatch() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        summaries.get(0).setTotalUsers(4242L);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .notEqualTo(4242L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().noneMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers().equals(4242L));
    }

    @Test
    void testNotEqualsOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .notEqualTo(5000L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).hasSizeGreaterThan(9000).noneMatch(summary -> summary.getTotalUsers() != null && summary.getTotalUsers().equals(5000L));
    }

    @Test
    void testNotEqualsOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);
        summaries.get(0).setTotalUsers(null);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .notEqualTo(4242L);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotNull();
    }

    // ==================== In Operation Tests ====================

    @Test
    void testInOperationFindsMatchingValues() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .in(Arrays.asList(2500L, 3500L, 4500L, 5500L));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getTotalUsers() != null && 
            (summary.getTotalUsers() == 2500L || summary.getTotalUsers() == 3500L || 
             summary.getTotalUsers() == 4500L || summary.getTotalUsers() == 5500L));
    }

    @Test
    void testInOperationWithLargeDataset() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        List<Long> searchValues = new ArrayList<>();
        for (long i = 2000L; i <= 6000L; i += 500L) {
            searchValues.add(i);
        }
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .in(searchValues);

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getTotalUsers() != null && searchValues.contains(summary.getTotalUsers()));
    }

    @Test
    void testInOperationWithEmptyList() {
        List<UserSummary> summaries = generateUserSummaries(1000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .in(new ArrayList<>());

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isEmpty();
    }

    @Test
    void testInOperationWithNullValue() {
        List<UserSummary> summaries = generateUserSummaries(100);

        // Aranan değerlerden birini içeren UserSummary ekle
        UserSummary summary2500 = new UserSummary();
        summary2500.setTotalUsers(2500L);
        summaries.add(summary2500);

        UserSummary summary3500 = new UserSummary();
        summary3500.setTotalUsers(3500L);
        summaries.add(summary3500);

        UserSummary summary4500 = new UserSummary();
        summary4500.setTotalUsers(4500L);
        summaries.add(summary4500);

        summaries.get(0).setTotalUsers(null);

        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .in(Arrays.asList(2500L, 3500L, 4500L));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().doesNotContain(summaries.get(0));
    }
    // ==================== Complex Combination Tests ====================

    @Test
    void testCombinedOperationsGreaterThanAndLessThan() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(3000L)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .lessThan(7000L));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getTotalUsers() != null && summary.getTotalUsers() > 3000L && summary.getTotalUsers() < 7000L);
    }

    @Test
    void testCombinedOperationsRangeAndNotEquals() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThanOrEqual(3000L)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .lessThanOrEqual(7000L))
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.totalUsers)
                .notEqualTo(5000L));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getTotalUsers() != null && 
            summary.getTotalUsers() >= 3000L && 
            summary.getTotalUsers() <= 7000L && 
            !summary.getTotalUsers().equals(5000L));
    }

    @Test
    void testCombinedOperationsMultipleFields() {
        List<UserSummary> summaries = generateUserSummaries(10_000);
        
        Specification<UserSummary> spec = SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
            .where(UserSummary_.totalUsers)
            .greaterThan(5000L)
            .and(SpecificationBuilder.forService(UserSummarySpecificationService.INSTANCE)
                .where(UserSummary_.activeUsers)
                .lessThan(3000L));

        SpecificationQueryEngine<UserSummary> engine = new SpecificationQueryEngine<>(UserSummary.class);
        List<UserSummary> results = engine.query(summaries, spec);

        assertThat(results).isNotEmpty().allMatch(summary ->
            summary.getTotalUsers() != null && summary.getTotalUsers() > 5000L &&
            summary.getActiveUsers() != null && summary.getActiveUsers() < 3000L);
    }

    // ==================== Helper Methods ====================

    private List<UserSummary> generateUserSummaries(int count) {
        List<UserSummary> summaries = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            UserSummary summary = new UserSummary();
            summary.setTotalUsers((long) (1000 + RANDOM.nextInt(9000)));
            summary.setActiveUsers((long) (500 + RANDOM.nextInt(4500)));
            summary.setInactiveUsers((long) (100 + RANDOM.nextInt(900)));
            summary.setMaxAge(20 + RANDOM.nextInt(60));
            summary.setMinAge(18 + RANDOM.nextInt(30));
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
