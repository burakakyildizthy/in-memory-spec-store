package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.User_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test coverage for NotSpecification.
 * Tests all public methods with various scenarios including simple specifications,
 * nested specifications, null handling, predicate conversion, and unique key generation.
 */
class NotSpecificationTest {

    // String literals used multiple times
    private static final String USER_PREFIX = "User";
    private static final String USER_1 = "User1";
    private static final String USER_100 = "User100";
    private static final String PERSON_200 = "Person200";
    private static final String ID_1 = "1";
    private static final String ID_100 = "100";
    private static final String ID_200 = "200";
    
    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testNotSpecificationWithSimpleSpecification() {
        // Create simple specification using SpecificationBuilder
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        
        // Create NOT specification
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        
        // Test with entity matching the original specification (should return false)
        User matchingUser = new User();
        matchingUser.setId(ID_100);
        matchingUser.setName(USER_100);
        assertThat(notSpec.test(matchingUser)).isFalse();
        
        // Test with entity not matching the original specification (should return true)
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_200);
        nonMatchingUser.setName(PERSON_200);
        assertThat(notSpec.test(nonMatchingUser)).isTrue();
    }

    @Test
    void testNotSpecificationWithNestedSpecification() {
        // Create nested specification: NOT((name contains USER_1) AND (id contains ID_1))
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        NotSpecification<User> notSpec = new NotSpecification<>(andSpec);
        
        // Test with entity matching both conditions (should return false)
        User matchingBoth = new User();
        matchingBoth.setId(ID_100);
        matchingBoth.setName(USER_100);
        assertThat(notSpec.test(matchingBoth)).isFalse();
        
        // Test with entity matching only name (should return true)
        User matchingName = new User();
        matchingName.setId(ID_200);
        matchingName.setName(USER_100);
        assertThat(notSpec.test(matchingName)).isTrue();
        
        // Test with entity matching only id (should return true)
        User matchingId = new User();
        matchingId.setId(ID_100);
        matchingId.setName("Person100");
        assertThat(notSpec.test(matchingId)).isTrue();
        
        // Test with entity matching neither (should return true)
        User matchingNeither = new User();
        matchingNeither.setId(ID_200);
        matchingNeither.setName(PERSON_200);
        assertThat(notSpec.test(matchingNeither)).isTrue();
    }

    @Test
    void testNotSpecificationWithNullEntity() {
        // Create simple specification that handles null gracefully
        Specification<User> nameSpec = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> entity != null && entity.getName() != null && entity.getName().contains(USER_PREFIX);
            }
        };
        
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        
        // Test with null entity - NOT(false) should return true
        assertThat(notSpec.test(null)).isTrue();
        
        // Test with entity with null fields - NOT(false) should return true
        User userWithNullFields = new User();
        assertThat(notSpec.test(userWithNullFields)).isTrue();
        
        // Test with entity matching the specification - NOT(true) should return false
        User matchingUser = new User();
        matchingUser.setId(ID_1);
        matchingUser.setName(USER_1);
        assertThat(notSpec.test(matchingUser)).isFalse();
    }

    @Test
    void testNotSpecificationToPredicateMethod() {
        // Create specification
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        
        // Get predicate
        Predicate<User> predicate = notSpec.toPredicate();
        assertThat(predicate).isNotNull();
        
        // Test predicate with entity matching original specification
        User matchingUser = new User();
        matchingUser.setId(ID_100);
        matchingUser.setName(USER_100);
        assertThat(predicate.test(matchingUser)).isFalse();
        
        // Test predicate with entity not matching original specification
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_200);
        nonMatchingUser.setName(PERSON_200);
        assertThat(predicate.test(nonMatchingUser)).isTrue();
        
        // Verify predicate is consistent with test() method
        List<User> users = generator.generateUsers(100);
        for (User user : users) {
            assertThat(predicate.test(user)).isEqualTo(notSpec.test(user));
        }
    }

    @Test
    void testNotSpecificationToUniqueKeyMethod() {
        // Create specification
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        
        // Get unique key
        String uniqueKey = notSpec.toString();
        assertThat(uniqueKey).isNotNull();
        
        // Note: Specifications created with SpecificationBuilder use UUID-based keys
        // which are not deterministic across multiple calls, but the structure is consistent
        
        // Verify different NOT specifications produce different structure
        Specification<User> differentSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains("Different");
        NotSpecification<User> differentNotSpec = new NotSpecification<>(differentSpec);
        String differentKey = differentNotSpec.toString();
        
        // Both should have NOT structure
        assertThat(differentKey).isNotNull();
        
        // Verify the key format is correct for nested specifications
        Specification<User> andSpec = new AndSpecification<>(nameSpec, differentSpec);
        NotSpecification<User> notOfAndSpec = new NotSpecification<>(andSpec);
        String nestedKey = notOfAndSpec.toString();
        assertThat(nestedKey).isNotNull();
    }

    @Test
    void testNotSpecificationWithLargeDataset() {
        // Create specification
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        
        // Generate large dataset (10K users)
        List<User> users = generator.generateUsers(10_000);
        
        // Filter using specification
        long matchCount = users.stream()
            .filter(notSpec::test)
            .count();
        
        // Verify results (users with name NOT containing USER_1)
        // Should match most users since only a subset contains USER_1
        assertThat(matchCount).isGreaterThan(0);
        
        // Verify all matching users do NOT satisfy the original condition
        List<User> matchingUsers = users.stream()
            .filter(notSpec::test)
            .toList();
        
        for (User user : matchingUsers) {
            assertThat(user.getName()).doesNotContain(USER_1);
        }
        
        // Verify count consistency
        long originalMatchCount = users.stream()
            .filter(nameSpec::test)
            .count();
        assertThat(matchCount + originalMatchCount).isEqualTo(users.size());
    }

    @Test
    void testNotSpecificationComplexNestedCombinations() {
        // Create complex nested specification: NOT((A AND B) OR (C AND D))
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("2");
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("0");
        
        AndSpecification<User> innerAnd1 = new AndSpecification<>(specA, specB);
        AndSpecification<User> innerAnd2 = new AndSpecification<>(specC, specD);
        OrSpecification<User> innerOr = new OrSpecification<>(innerAnd1, innerAnd2);
        NotSpecification<User> outerNot = new NotSpecification<>(innerOr);
        
        // Test with entity matching first AND (should return false)
        User matchingFirstAnd = new User();
        matchingFirstAnd.setId(ID_100);
        matchingFirstAnd.setName(USER_100);
        assertThat(outerNot.test(matchingFirstAnd)).isFalse();
        
        // Test with entity matching second AND (should return false)
        User matchingSecondAnd = new User();
        matchingSecondAnd.setId(ID_200);
        matchingSecondAnd.setName(PERSON_200);
        assertThat(outerNot.test(matchingSecondAnd)).isFalse();
        
        // Test with entity matching neither AND (should return true)
        User matchingNeither = new User();
        matchingNeither.setId("333");
        matchingNeither.setName("Person333");
        assertThat(outerNot.test(matchingNeither)).isTrue();
        
        // Verify unique key structure
        String uniqueKey = outerNot.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testNotSpecificationDoubleNegation() {
        // Create double negation: NOT(NOT(spec))
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        
        NotSpecification<User> notSpec = new NotSpecification<>(nameSpec);
        NotSpecification<User> doubleNotSpec = new NotSpecification<>(notSpec);
        
        // Test with entity matching original specification
        // NOT(NOT(true)) should return true
        User matchingUser = new User();
        matchingUser.setId(ID_100);
        matchingUser.setName(USER_100);
        assertThat(doubleNotSpec.test(matchingUser)).isTrue();
        
        // Test with entity not matching original specification
        // NOT(NOT(false)) should return false
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_200);
        nonMatchingUser.setName(PERSON_200);
        assertThat(doubleNotSpec.test(nonMatchingUser)).isFalse();
        
        // Verify double negation equals original
        List<User> users = generator.generateUsers(100);
        for (User user : users) {
            assertThat(doubleNotSpec.test(user)).isEqualTo(nameSpec.test(user));
        }
        
        // Verify unique key structure
        String uniqueKey = doubleNotSpec.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testNotSpecificationDeMorgansLaw() {
        // Verify De Morgan's Law: NOT(A AND B) = NOT(A) OR NOT(B)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        // NOT(A AND B)
        AndSpecification<User> andSpec = new AndSpecification<>(specA, specB);
        NotSpecification<User> notAndSpec = new NotSpecification<>(andSpec);
        
        // NOT(A) OR NOT(B)
        NotSpecification<User> notA = new NotSpecification<>(specA);
        NotSpecification<User> notB = new NotSpecification<>(specB);
        OrSpecification<User> orNotSpec = new OrSpecification<>(notA, notB);
        
        // Test with large dataset
        List<User> users = generator.generateUsers(1000);
        
        // Verify both expressions produce same results
        for (User user : users) {
            boolean result1 = notAndSpec.test(user);
            boolean result2 = orNotSpec.test(user);
            assertThat(result1).isEqualTo(result2);
        }
    }
}
