package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.AttributeType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.TestUser;
import com.thy.fss.common.inmemory.testmodel.TestUserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestUser_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for BooleanAttribute operations.
 * Tests all boolean-specific operations using generated meta model (TestUser_.active).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.5, 15.10, 15.9
 */
class BooleanAttributeTest {
    
    private static final String ACTIVE_ATTRIBUTE = "active";
    private static final String USER_PREFIX = "User";
    private static final String EMAIL_PREFIX = "user";
    private static final String EMAIL_SUFFIX = "@test.com";
    private static final String USER1_SUBSTRING = "User1";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesBooleanAttribute() {
        BooleanAttribute<TestUser> attribute = new BooleanAttribute<>(ACTIVE_ATTRIBUTE, TestUser.class);

        assertThat(attribute.getName()).isEqualTo(ACTIVE_ATTRIBUTE);
        assertThat(attribute.getOwnerType()).isEqualTo(TestUser.class);
        assertThat(attribute.getFieldType()).isEqualTo(Boolean.class);
        assertThat(attribute.getAttributeType()).isEqualTo(AttributeType.SINGLE);
    }

    @Test
    void testGeneratedMetaModelTestUserActiveHasCorrectProperties() {
        assertThat(TestUser_.active).isNotNull();
        assertThat(TestUser_.active.getName()).isEqualTo(ACTIVE_ATTRIBUTE);
        assertThat(TestUser_.active.getOwnerType()).isEqualTo(TestUser.class);
        assertThat(TestUser_.active.getFieldType()).isEqualTo(Boolean.class);
        assertThat(TestUser_.active.getAttributeType()).isEqualTo(AttributeType.SINGLE);
    }

    @Test
    void testToStringReturnsCorrectFormat() {
        BooleanAttribute<TestUser> attribute = new BooleanAttribute<>(ACTIVE_ATTRIBUTE, TestUser.class);
        assertThat(attribute).hasToString("TestUser.active");
    }

    @Test
    void testEqualsWithSameAttributes() {
        BooleanAttribute<TestUser> attr1 = new BooleanAttribute<>(ACTIVE_ATTRIBUTE, TestUser.class);
        BooleanAttribute<TestUser> attr2 = new BooleanAttribute<>(ACTIVE_ATTRIBUTE, TestUser.class);

        assertThat(attr1).isEqualTo(attr2).hasSameHashCodeAs(attr2.hashCode());
    }

    @Test
    void testEqualsWithDifferentAttributes() {
        BooleanAttribute<TestUser> attr1 = new BooleanAttribute<>(ACTIVE_ATTRIBUTE, TestUser.class);
        BooleanAttribute<TestUser> attr2 = new BooleanAttribute<>("enabled", TestUser.class);

        assertThat(attr1).isNotEqualTo(attr2);
    }

    // ==================== IsTrue Operation Tests ====================

    @Test
    void testIsTrueOperationWithLargeDataset() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> Boolean.TRUE.equals(user.getActive())).hasSizeGreaterThan(3000);
    }

    @Test
    void testIsTrueOperationWithNullValues() {
        List<TestUser> users = generateTestUsersWithBooleanValues(1000);
        users.get(0).setActive(null);
        users.get(1).setActive(null);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).allMatch(user -> Boolean.TRUE.equals(user.getActive())).noneMatch(user -> user.getActive() == null);
    }

    @Test
    void testIsTrueOperationWithAllFalseValues() {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(new TestUser((long) i, USER_PREFIX + i, EMAIL_PREFIX + i + EMAIL_SUFFIX, false));
        }

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isEmpty();
    }

    // ==================== IsFalse Operation Tests ====================

    @Test
    void testIsFalseOperationWithLargeDataset() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isFalse();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> Boolean.FALSE.equals(user.getActive())).hasSizeGreaterThan(3000);
    }

    @Test
    void testIsFalseOperationWithNullValues() {
        List<TestUser> users = generateTestUsersWithBooleanValues(1000);
        users.get(0).setActive(null);
        users.get(1).setActive(null);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isFalse();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).allMatch(user -> Boolean.FALSE.equals(user.getActive())).noneMatch(user -> user.getActive() == null);
    }

    @Test
    void testIsFalseOperationWithAllTrueValues() {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(new TestUser((long) i, USER_PREFIX + i, EMAIL_PREFIX + i + EMAIL_SUFFIX, true));
        }

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isFalse();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isEmpty();
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationWithTrueValueLargeDataset() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .equalTo(true);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> Boolean.TRUE.equals(user.getActive()));
    }

    @Test
    void testEqualsOperationWithFalseValueLargeDataset() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .equalTo(false);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> Boolean.FALSE.equals(user.getActive()));
    }

    @Test
    void testNotEqualsOperationWithTrueValue() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .notEqualTo(true);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> !Boolean.TRUE.equals(user.getActive()));
    }

    // ==================== Combined Operations Tests ====================

    @Test
    void testCombinedOperationsIsTrueAndOtherConditions() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> activeSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        Specification<TestUser> nameSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.name)
            .contains(USER1_SUBSTRING);

        Specification<TestUser> combinedSpec = activeSpec.and(nameSpec);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, combinedSpec);

        assertThat(results).isNotEmpty().allMatch(user ->
            Boolean.TRUE.equals(user.getActive()) && user.getName().contains(USER1_SUBSTRING)
        );
    }

    @Test
    void testCombinedOperationsIsFalseAndOtherConditions() {
        List<TestUser> users = generateTestUsersWithBooleanValues(10_000);

        Specification<TestUser> inactiveSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isFalse();

        Specification<TestUser> emailSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.email)
            .contains(EMAIL_SUFFIX);

        Specification<TestUser> combinedSpec = inactiveSpec.and(emailSpec);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, combinedSpec);

        assertThat(results).isNotEmpty().allMatch(user ->
            Boolean.FALSE.equals(user.getActive()) && user.getEmail().contains(EMAIL_SUFFIX)
        );
    }

    @Test
    void testOrOperationIsTrueOrIsFalse() {
        List<TestUser> users = generateTestUsersWithBooleanValues(1000);
        users.get(0).setActive(null);
        users.get(1).setActive(null);

        Specification<TestUser> trueSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        Specification<TestUser> falseSpec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isFalse();

        Specification<TestUser> combinedSpec = trueSpec.or(falseSpec);

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, combinedSpec);

        assertThat(results).hasSize(998).allMatch(user -> user.getActive() != null);
    }

    // ==================== Performance Tests ====================

    @Test
    void testPerformanceLargeDatasetQuery() {
        List<TestUser> users = generateTestUsersWithBooleanValues(100_000);

        long startTime = System.currentTimeMillis();

        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
            .where(TestUser_.active)
            .isTrue();

        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        List<TestUser> results = engine.query(users, spec);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(results).isNotEmpty().allMatch(user -> Boolean.TRUE.equals(user.getActive()));
        assertThat(duration).isLessThan(1000);
    }

    // ==================== Helper Methods ====================

    private List<TestUser> generateTestUsersWithBooleanValues(int count) {
        List<TestUser> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Boolean active;
            if (i % 3 == 0) {
                active = true;
            } else if (i % 3 == 1) {
                active = false;
            } else {
                active = i % 2 == 0;
            }
            users.add(new TestUser((long) i, USER_PREFIX + i, EMAIL_PREFIX + i + EMAIL_SUFFIX, active));
        }
        return users;
    }
}
