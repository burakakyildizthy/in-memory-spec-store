package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.AttributeType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.AbbreviatedUser;
import com.thy.fss.common.inmemory.testmodel.AbbreviatedUserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.AbbreviatedUser_;
import com.thy.fss.common.inmemory.testmodel.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for EnumAttribute operations.
 * Tests all enum-specific operations using generated meta model (AbbreviatedUser_.status).
 * Tests with large datasets (10K entities) to verify performance.
 * Requirements: 5.5, 15.10, 15.9
 */
class EnumAttributeTest {

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    private static final String STATUS_FIELD = "status";

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesEnumAttribute() {
        EnumAttribute<AbbreviatedUser, UserStatus> attribute =
            new EnumAttribute<>(STATUS_FIELD, AbbreviatedUser.class, UserStatus.class);

        assertThat(attribute.getName()).isEqualTo(STATUS_FIELD);
        assertThat(attribute.getOwnerType()).isEqualTo(AbbreviatedUser.class);
        assertThat(attribute.getFieldType()).isEqualTo(UserStatus.class);
        assertThat(attribute.getAttributeType()).isEqualTo(AttributeType.SINGLE);
    }

    @Test
    void testGeneratedMetaModelAbbreviatedUserStatusHasCorrectProperties() {
        assertThat(AbbreviatedUser_.status).isNotNull();
        assertThat(AbbreviatedUser_.status.getName()).isEqualTo(STATUS_FIELD);
        assertThat(AbbreviatedUser_.status.getOwnerType()).isEqualTo(AbbreviatedUser.class);
        assertThat(AbbreviatedUser_.status.getFieldType()).isEqualTo(UserStatus.class);
        assertThat(AbbreviatedUser_.status.getAttributeType()).isEqualTo(AttributeType.SINGLE);
    }

    @Test
    void testToStringReturnsCorrectFormat() {
        EnumAttribute<AbbreviatedUser, UserStatus> attribute = 
            new EnumAttribute<>(STATUS_FIELD, AbbreviatedUser.class, UserStatus.class);
        assertThat(attribute.toString()).hasToString("AbbreviatedUser.status");
    }

    @Test
    void testEqualsWithSameAttributes() {
        EnumAttribute<AbbreviatedUser, UserStatus> attr1 = 
            new EnumAttribute<>(STATUS_FIELD, AbbreviatedUser.class, UserStatus.class);
        EnumAttribute<AbbreviatedUser, UserStatus> attr2 = 
            new EnumAttribute<>(STATUS_FIELD, AbbreviatedUser.class, UserStatus.class);

        assertThat(attr1).isEqualTo(attr2);
        assertThat(attr1.hashCode()).hasSameHashCodeAs(attr2.hashCode());
    }

    @Test
    void testEqualsWithDifferentAttributes() {
        EnumAttribute<AbbreviatedUser, UserStatus> attr1 = 
            new EnumAttribute<>(STATUS_FIELD, AbbreviatedUser.class, UserStatus.class);
        EnumAttribute<AbbreviatedUser, UserStatus> attr2 = 
            new EnumAttribute<>("type", AbbreviatedUser.class, UserStatus.class);

        assertThat(attr1).isNotEqualTo(attr2);
    }

    // ==================== Equals Operation Tests ====================

    @Test
    void testEqualsOperationWithActiveStatusLargeDataset() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.ACTIVE);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() == UserStatus.ACTIVE)
                .hasSizeGreaterThan(2000);
    }

    @Test
    void testEqualsOperationWithInactiveStatus() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.INACTIVE);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() == UserStatus.INACTIVE);
    }

    @Test
    void testEqualsOperationWithPendingStatus() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.PENDING);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() == UserStatus.PENDING);
    }

    @Test
    void testEqualsOperationWithSuspendedStatus() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.SUSPENDED);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() == UserStatus.SUSPENDED);
    }

    @Test
    void testEqualsOperationWithNullValues() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(1000);
        users.get(0).setStatus(null);
        users.get(1).setStatus(null);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.ACTIVE);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).allMatch(user -> user.getStatus() == UserStatus.ACTIVE).noneMatch(user -> user.getStatus() == null);
    }

    // ==================== NotEquals Operation Tests ====================

    @Test
    void testNotEqualsOperationWithActiveStatusLargeDataset() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .notEqualTo(UserStatus.ACTIVE);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() != UserStatus.ACTIVE)
                .hasSizeGreaterThan(7000);
    }

    @Test
    void testNotEqualsOperationWithNullValues() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(1000);
        users.get(0).setStatus(null);
        users.get(1).setStatus(null);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .notEqualTo(UserStatus.ACTIVE);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() != UserStatus.ACTIVE);
    }

    // ==================== In Operation Tests ====================

    @Test
    void testInOperationWithMultipleStatusesLargeDataset() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .in(Arrays.asList(UserStatus.ACTIVE, UserStatus.PENDING));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user ->
            user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.PENDING
        ).hasSizeGreaterThan(4000);
    }

    @Test
    void testInOperationWithAllStatuses() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .in(Arrays.asList(UserStatus.ACTIVE, UserStatus.INACTIVE, UserStatus.PENDING, UserStatus.SUSPENDED));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).hasSize(10_000);
    }

    @Test
    void testInOperationWithSingleStatus() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .in(Arrays.asList(UserStatus.SUSPENDED));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() == UserStatus.SUSPENDED);
    }

    @Test
    void testInOperationWithEmptyList() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(1000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .in(Arrays.asList());

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isEmpty();
    }

    // ==================== NotIn Operation Tests ====================

    @Test
    void testNotInOperationWithMultipleStatusesLargeDataset() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .notIn(Arrays.asList(UserStatus.ACTIVE, UserStatus.PENDING));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isNotEmpty().allMatch(user ->
            user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING
        ).hasSizeGreaterThan(4000);
    }

    @Test
    void testNotInOperationWithAllStatuses() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .notIn(Arrays.asList(UserStatus.ACTIVE, UserStatus.INACTIVE, UserStatus.PENDING, UserStatus.SUSPENDED));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).isEmpty();
    }

    @Test
    void testNotInOperationWithEmptyList() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(1000);

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .notIn(Arrays.asList());

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        assertThat(results).hasSize(1000);
    }

    // ==================== Combined Operations Tests ====================

    @Test
    void testCombinedOperationsStatusAndName() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> statusSpec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.ACTIVE);

        Specification<AbbreviatedUser> nameSpec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.name)
            .contains("User1");

        Specification<AbbreviatedUser> combinedSpec = statusSpec.and(nameSpec);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, combinedSpec);

        assertThat(results).isNotEmpty().allMatch(user ->
            user.getStatus() == UserStatus.ACTIVE && user.getName().contains("User1")
        );
    }

    @Test
    void testOrOperationActiveOrInactive() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> activeSpec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.ACTIVE);

        Specification<AbbreviatedUser> inactiveSpec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.INACTIVE);

        Specification<AbbreviatedUser> combinedSpec = activeSpec.or(inactiveSpec);

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, combinedSpec);

        assertThat(results).isNotEmpty().allMatch(user ->
            user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.INACTIVE
        ).hasSizeGreaterThan(4000);
    }

    @Test
    void testNotOperationNotActive() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(10_000);

        Specification<AbbreviatedUser> activeSpec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .equalTo(UserStatus.ACTIVE);

        Specification<AbbreviatedUser> notActiveSpec = activeSpec.not();

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, notActiveSpec);

        assertThat(results).isNotEmpty().allMatch(user -> user.getStatus() != UserStatus.ACTIVE);
    }

    // ==================== Performance Tests ====================

    @Test
    void testPerformanceLargeDatasetQuery() {
        List<AbbreviatedUser> users = generateUsersWithEnumValues(100_000);

        long startTime = System.currentTimeMillis();

        Specification<AbbreviatedUser> spec = SpecificationBuilder.forService(AbbreviatedUserSpecificationService.INSTANCE)
            .where(AbbreviatedUser_.status)
            .in(Arrays.asList(UserStatus.ACTIVE, UserStatus.PENDING));

        SpecificationQueryEngine<AbbreviatedUser> engine = new SpecificationQueryEngine<>(AbbreviatedUser.class);
        List<AbbreviatedUser> results = engine.query(users, spec);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(results).isNotEmpty().allMatch(user ->
            user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.PENDING
        );
        assertThat(duration).isLessThan(1000);
    }

    // ==================== Helper Methods ====================

    private List<AbbreviatedUser> generateUsersWithEnumValues(int count) {
        List<AbbreviatedUser> users = new ArrayList<>(count);
        UserStatus[] statuses = UserStatus.values();
        
        for (int i = 0; i < count; i++) {
            AbbreviatedUser user = new AbbreviatedUser();
            user.setId((long) i);
            user.setName("User" + i);
            user.setEmail("user" + i + "@test.com");
            user.setAge(20 + (i % 60));
            user.setStatus(statuses[i % statuses.length]);
            users.add(user);
        }
        
        return users;
    }
}
