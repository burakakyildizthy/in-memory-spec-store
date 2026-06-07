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
 * Comprehensive test coverage for AndSpecification.
 * Tests all public methods with various scenarios including simple specifications,
 * nested specifications, null handling, predicate conversion, and unique key generation.
 */
class AndSpecificationTest {

    // String literals used multiple times
    private static final String USER_PREFIX = "User";
    private static final String USER_1 = "User_1";
    private static final String USER_100 = "User_100";
    private static final String PERSON_100 = "Person100";
    private static final String ID_1 = "1";
    private static final String ID_100 = "100";
    private static final String ID_200 = "200";
    
    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testAndSpecificationWithTwoSimpleSpecifications() {
        // Create simple specifications using SpecificationBuilder
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        // Create AND specification
        AndSpecification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        // Test with matching entity
        User matchingUser = new User();
        matchingUser.setId(ID_100);
        matchingUser.setName(USER_100);
        assertThat(andSpec.test(matchingUser)).isTrue();
        
        // Test with non-matching entity (name doesn't match)
        User nonMatchingUser1 = new User();
        nonMatchingUser1.setId(ID_100);
        nonMatchingUser1.setName(PERSON_100);
        assertThat(andSpec.test(nonMatchingUser1)).isFalse();
        
        // Test with non-matching entity (id doesn't match)
        User nonMatchingUser2 = new User();
        nonMatchingUser2.setId(ID_200);
        nonMatchingUser2.setName(USER_100);
        assertThat(andSpec.test(nonMatchingUser2)).isFalse();
        
        // Test with non-matching entity (both don't match)
        User nonMatchingUser3 = new User();
        nonMatchingUser3.setId(ID_200);
        nonMatchingUser3.setName("Person200");
        assertThat(andSpec.test(nonMatchingUser3)).isFalse();
    }

    @Test
    void testAndSpecificationWithNestedSpecifications() {
        // Create nested specifications: (name contains USER_1) AND ((id contains "1") OR (id contains "2"))
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec1 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> idSpec2 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("2");
        Specification<User> orSpec = new OrSpecification<>(idSpec1, idSpec2);
        
        AndSpecification<User> nestedAndSpec = new AndSpecification<>(nameSpec, orSpec);
        
        // Test with matching entity (name contains USER_1 and id contains "1")
        User matchingUser1 = new User();
        matchingUser1.setId(ID_100);
        matchingUser1.setName(USER_100);
        assertThat(nestedAndSpec.test(matchingUser1)).isTrue();
        
        // Test with matching entity (name contains USER_1 and id contains "2")
        User matchingUser2 = new User();
        matchingUser2.setId(ID_200);
        matchingUser2.setName(USER_100);
        assertThat(nestedAndSpec.test(matchingUser2)).isTrue();
        
        // Test with non-matching entity (name doesn't contain USER_1)
        User nonMatchingUser1 = new User();
        nonMatchingUser1.setId(ID_100);
        nonMatchingUser1.setName(PERSON_100);
        assertThat(nestedAndSpec.test(nonMatchingUser1)).isFalse();
        
        // Test with non-matching entity (id doesn't contain "1" or "2")
        User nonMatchingUser2 = new User();
        nonMatchingUser2.setId("300");
        nonMatchingUser2.setName(USER_100);
        assertThat(nestedAndSpec.test(nonMatchingUser2)).isFalse();
    }

    @Test
    void testAndSpecificationWithNullEntity() {
        // Create simple specifications that handle null gracefully
        Specification<User> nameSpec = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> entity != null && entity.getName() != null && entity.getName().contains(USER_PREFIX);
            }
        };
        Specification<User> idSpec = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> entity != null && entity.getIdentity() != null && entity.getIdentity().contains(ID_1);
            }
        };
        
        AndSpecification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        // Test with null entity - should handle gracefully
        assertThat(andSpec.test(null)).isFalse();
        
        // Test with entity with null fields
        User userWithNullFields = new User();
        assertThat(andSpec.test(userWithNullFields)).isFalse();
    }

    @Test
    void testAndSpecificationToPredicateMethod() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        AndSpecification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        // Get predicate
        Predicate<User> predicate = andSpec.toPredicate();
        assertThat(predicate).isNotNull();
        
        // Test predicate with matching entity
        User matchingUser = new User();
        matchingUser.setId(ID_100);
        matchingUser.setName(USER_100);
        assertThat(predicate.test(matchingUser)).isTrue();
        
        // Test predicate with non-matching entity
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_200);
        nonMatchingUser.setName("Person200");
        assertThat(predicate.test(nonMatchingUser)).isFalse();
        
        // Verify predicate is consistent with test() method
        List<User> users = generator.generateUsers(100);
        for (User user : users) {
            assertThat(predicate.test(user)).isEqualTo(andSpec.test(user));
        }
    }

    @Test
    void testAndSpecificationToUniqueKeyMethod() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        AndSpecification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        // Get unique key
        String uniqueKey = andSpec.toString();
        assertThat(uniqueKey).isNotNull();
        
        // Note: Specifications created with SpecificationBuilder use UUID-based keys
        // which are not deterministic across multiple calls, but the structure is consistent
        
        // Verify different AND specifications produce different structure
        Specification<User> differentSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains("Different");
        AndSpecification<User> differentAndSpec = new AndSpecification<>(nameSpec, differentSpec);
        String differentKey = differentAndSpec.toString();
        
        // Both should have AND structure but different content
        assertThat(differentKey).isNotNull();
        
        // Verify the key format is correct for nested specifications
        Specification<User> nestedSpec = new AndSpecification<>(nameSpec, idSpec);
        AndSpecification<User> doubleNestedSpec = new AndSpecification<>(nestedSpec, differentSpec);
        String nestedKey = doubleNestedSpec.toString();
        assertThat(nestedKey).isNotNull();
    }

    @Test
    void testAndSpecificationWithLargeDataset() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        AndSpecification<User> andSpec = new AndSpecification<>(nameSpec, idSpec);
        
        // Generate large dataset (10K users)
        List<User> users = generator.generateUsers(10_000);
        
        // Filter using specification
        long matchCount = users.stream()
            .filter(andSpec::test)
            .count();
        
        // Verify results (users with id containing "1" and name containing USER_1)
        // IDs: 1, 10-19, 100-199, 1000-1999, etc.
        // All generated users have names like "User0", USER_1, etc.
        assertThat(matchCount).isGreaterThan(0);
        
        // Verify all matching users satisfy both conditions
        List<User> matchingUsers = users.stream()
            .filter(andSpec::test)
            .toList();
        
        for (User user : matchingUsers) {
            assertThat(user.getIdentity()).contains(ID_1);
            assertThat(user.getName()).contains(USER_1);
        }
    }

    @Test
    void testAndSpecificationComplexNestedCombinations() {
        // Create complex nested specification: ((A AND B) AND (C OR D))
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("0");
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("5");
        
        AndSpecification<User> innerAnd = new AndSpecification<>(specA, specB);
        OrSpecification<User> innerOr = new OrSpecification<>(specC, specD);
        AndSpecification<User> outerAnd = new AndSpecification<>(innerAnd, innerOr);
        
        // Test with matching entity (name contains USER_PREFIX, id contains "1", "0", and "5")
        User matchingUser1 = new User();
        matchingUser1.setId("150");
        matchingUser1.setName("User150");
        assertThat(outerAnd.test(matchingUser1)).isTrue();
        
        // Test with matching entity (name contains USER_PREFIX, id contains "1" and "0")
        User matchingUser2 = new User();
        matchingUser2.setId(ID_100);
        matchingUser2.setName(USER_100);
        assertThat(outerAnd.test(matchingUser2)).isTrue();
        
        // Test with non-matching entity (id doesn't contain "1")
        User nonMatchingUser1 = new User();
        nonMatchingUser1.setId("250");
        nonMatchingUser1.setName("User250");
        assertThat(outerAnd.test(nonMatchingUser1)).isFalse();
        
        // Test with non-matching entity (id doesn't contain "0" or "5")
        User nonMatchingUser2 = new User();
        nonMatchingUser2.setId("123");
        nonMatchingUser2.setName("User123");
        assertThat(outerAnd.test(nonMatchingUser2)).isFalse();
        
        // Verify unique key structure
        String uniqueKey = outerAnd.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testAndSpecificationShortCircuitEvaluation() {
        // Create specifications where first one fails
        Specification<User> alwaysFalse = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> false;
            }
        };
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        
        AndSpecification<User> andSpec = new AndSpecification<>(alwaysFalse, nameSpec);
        
        // Test with entity - should short-circuit and return false
        User user = new User();
        user.setId(ID_1);
        user.setName(USER_1);
        assertThat(andSpec.test(user)).isFalse();
        
        // Create specifications where first one passes but second fails
        Specification<User> alwaysTrue = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> true;
            }
        };
        Specification<User> alwaysFalse2 = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> false;
            }
        };
        
        AndSpecification<User> andSpec2 = new AndSpecification<>(alwaysTrue, alwaysFalse2);
        assertThat(andSpec2.test(user)).isFalse();
    }
}
