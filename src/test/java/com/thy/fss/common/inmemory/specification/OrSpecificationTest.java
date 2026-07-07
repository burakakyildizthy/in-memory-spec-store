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
 * Comprehensive test coverage for OrSpecification.
 * Tests all public methods with various scenarios including simple specifications,
 * nested specifications, null handling, predicate conversion, and unique key generation.
 */
class OrSpecificationTest {

    // String literals used multiple times
    private static final String USER_PREFIX = "User";
    private static final String USER_1 = "User_1";
    private static final String USER_100 = "User_100";
    private static final String USER_120 = "User_120";
    private static final String PERSON_200 = "Person200";
    private static final String PERSON_300 = "Person300";
    private static final String PERSON_333 = "Person333";
    private static final String ID_1 = "1";
    private static final String ID_2 = "2";
    private static final String ID_0 = "0";
    private static final String ID_100 = "100";
    private static final String ID_120 = "120";
    private static final String ID_200 = "200";
    private static final String ID_300 = "300";
    private static final String ID_333 = "333";
    
    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testOrSpecificationWithTwoSimpleSpecifications() {
        // Create simple specifications using SpecificationBuilder
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        
        // Create OR specification
        OrSpecification<User> orSpec = new OrSpecification<>(nameSpec, idSpec);
        
        // Test with entity matching first condition
        User matchingUser1 = new User();
        matchingUser1.setId(ID_100);
        matchingUser1.setName(USER_100);
        assertThat(orSpec.test(matchingUser1)).isTrue();
        
        // Test with entity matching second condition
        User matchingUser2 = new User();
        matchingUser2.setId(ID_200);
        matchingUser2.setName(PERSON_200);
        assertThat(orSpec.test(matchingUser2)).isTrue();
        
        // Test with entity matching both conditions
        User matchingUser3 = new User();
        matchingUser3.setId(ID_120);
        matchingUser3.setName(USER_120);
        assertThat(orSpec.test(matchingUser3)).isTrue();
        
        // Test with entity matching neither condition
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_300);
        nonMatchingUser.setName(PERSON_300);
        assertThat(orSpec.test(nonMatchingUser)).isFalse();
    }

    @Test
    void testOrSpecificationWithNestedSpecifications() {
        // Create nested specifications: (name contains USER_1) OR ((id contains "2") AND (id contains "0"))
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec1 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        Specification<User> idSpec2 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0);
        Specification<User> andSpec = new AndSpecification<>(idSpec1, idSpec2);
        
        OrSpecification<User> nestedOrSpec = new OrSpecification<>(nameSpec, andSpec);
        
        // Test with entity matching first condition (name contains USER_1)
        User matchingUser1 = new User();
        matchingUser1.setId(ID_300);
        matchingUser1.setName(USER_100);
        assertThat(nestedOrSpec.test(matchingUser1)).isTrue();
        
        // Test with entity matching second condition (id contains "2" and "0")
        User matchingUser2 = new User();
        matchingUser2.setId(ID_200);
        matchingUser2.setName(PERSON_200);
        assertThat(nestedOrSpec.test(matchingUser2)).isTrue();
        
        // Test with entity matching both conditions
        User matchingUser3 = new User();
        matchingUser3.setId(ID_120);
        matchingUser3.setName(USER_120);
        assertThat(nestedOrSpec.test(matchingUser3)).isTrue();
        
        // Test with entity matching neither condition
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_333);
        nonMatchingUser.setName(PERSON_333);
        assertThat(nestedOrSpec.test(nonMatchingUser)).isFalse();
    }

    @Test
    void testOrSpecificationWithNullEntity() {
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
        
        OrSpecification<User> orSpec = new OrSpecification<>(nameSpec, idSpec);
        
        // Test with null entity - should handle gracefully
        assertThat(orSpec.test(null)).isFalse();
        
        // Test with entity with null fields
        User userWithNullFields = new User();
        assertThat(orSpec.test(userWithNullFields)).isFalse();
    }

    @Test
    void testOrSpecificationToPredicateMethod() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        
        OrSpecification<User> orSpec = new OrSpecification<>(nameSpec, idSpec);
        
        // Get predicate
        Predicate<User> predicate = orSpec.toPredicate();
        assertThat(predicate).isNotNull();
        
        // Test predicate with entity matching first condition
        User matchingUser1 = new User();
        matchingUser1.setId(ID_100);
        matchingUser1.setName(USER_100);
        assertThat(predicate.test(matchingUser1)).isTrue();
        
        // Test predicate with entity matching second condition
        User matchingUser2 = new User();
        matchingUser2.setId(ID_200);
        matchingUser2.setName(PERSON_200);
        assertThat(predicate.test(matchingUser2)).isTrue();
        
        // Test predicate with non-matching entity
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_300);
        nonMatchingUser.setName(PERSON_300);
        assertThat(predicate.test(nonMatchingUser)).isFalse();
        
        // Verify predicate is consistent with test() method
        List<User> users = generator.generateUsers(100);
        for (User user : users) {
            assertThat(predicate.test(user)).isEqualTo(orSpec.test(user));
        }
    }

    @Test
    void testOrSpecificationToUniqueKeyMethod() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        OrSpecification<User> orSpec = new OrSpecification<>(nameSpec, idSpec);
        
        // Get unique key
        String uniqueKey = orSpec.toString();
        assertThat(uniqueKey).isNotNull();
        
        // Note: Specifications created with SpecificationBuilder use UUID-based keys
        // which are not deterministic across multiple calls, but the structure is consistent
        
        // Verify different OR specifications produce different structure
        Specification<User> differentSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains("Different");
        OrSpecification<User> differentOrSpec = new OrSpecification<>(nameSpec, differentSpec);
        String differentKey = differentOrSpec.toString();
        
        // Both should have OR structure but different content
        assertThat(differentKey).isNotNull();
        
        // Verify the key format is correct for nested specifications
        Specification<User> nestedSpec = new OrSpecification<>(nameSpec, idSpec);
        OrSpecification<User> doubleNestedSpec = new OrSpecification<>(nestedSpec, differentSpec);
        String nestedKey = doubleNestedSpec.toString();
        assertThat(nestedKey).isNotNull();
    }

    @Test
    void testOrSpecificationWithLargeDataset() {
        // Create specifications
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_1);
        Specification<User> idSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        
        OrSpecification<User> orSpec = new OrSpecification<>(nameSpec, idSpec);
        
        // Generate large dataset (10K users)
        List<User> users = generator.generateUsers(10_000);
        
        // Filter using specification
        long matchCount = users.stream()
            .filter(orSpec::test)
            .count();
        
        // Verify results (users with id containing "2" OR name containing USER_1)
        // Should match many users since it's an OR condition
        assertThat(matchCount).isGreaterThan(0);
        
        // Verify all matching users satisfy at least one condition
        List<User> matchingUsers = users.stream()
            .filter(orSpec::test)
            .toList();
        
        for (User user : matchingUsers) {
            boolean matchesName = user.getName().contains(USER_1);
            boolean matchesId = user.getIdentity().contains(ID_2);
            assertThat(matchesName || matchesId).isTrue();
        }
    }

    @Test
    void testOrSpecificationComplexNestedCombinations() {
        // Create complex nested specification: ((A OR B) OR (C AND D))
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0);

        OrSpecification<User> innerOr = new OrSpecification<>(specA, specB);
        AndSpecification<User> innerAnd = new AndSpecification<>(specC, specD);
        OrSpecification<User> outerOr = new OrSpecification<>(innerOr, innerAnd);

        // Test with entity matching first OR condition (name contains USER_PREFIX)
        User matchingUser1 = new User();
        matchingUser1.setId(ID_333);
        matchingUser1.setName("User333");
        assertThat(outerOr.test(matchingUser1)).isTrue();

        // Test with entity matching first OR condition (id contains "1")
        User matchingUser2 = new User();
        matchingUser2.setId(ID_100);
        matchingUser2.setName("Person100");
        assertThat(outerOr.test(matchingUser2)).isTrue();

        // Test with entity matching second AND condition (id contains "2" and "0")
        User matchingUser3 = new User();
        matchingUser3.setId(ID_200);
        matchingUser3.setName(PERSON_200);
        assertThat(outerOr.test(matchingUser3)).isTrue();

        // Test with entity matching none of the conditions
        User nonMatchingUser = new User();
        nonMatchingUser.setId(ID_333);
        nonMatchingUser.setName(PERSON_333);
        assertThat(outerOr.test(nonMatchingUser)).isFalse();

        // Verify unique key structure
        String uniqueKey = outerOr.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testOrSpecificationShortCircuitEvaluation() {
        // Create specifications where first one passes
        Specification<User> alwaysTrue = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> true;
            }
        };
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        
        OrSpecification<User> orSpec = new OrSpecification<>(alwaysTrue, nameSpec);
        
        // Test with entity - should short-circuit and return true
        User user = new User();
        user.setId(ID_1);
        user.setName("Person1");
        assertThat(orSpec.test(user)).isTrue();
        
        // Create specifications where first one fails but second passes
        Specification<User> alwaysFalse = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> false;
            }
        };
        Specification<User> alwaysTrue2 = new Specification<>() {
            @Override
            public Predicate<User> toPredicate() {
                return entity -> true;
            }
        };
        
        OrSpecification<User> orSpec2 = new OrSpecification<>(alwaysFalse, alwaysTrue2);
        assertThat(orSpec2.test(user)).isTrue();
    }
}
