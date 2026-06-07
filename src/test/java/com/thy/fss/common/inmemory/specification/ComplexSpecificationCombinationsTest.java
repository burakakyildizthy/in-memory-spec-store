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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test coverage for complex specification combinations.
 * Tests deeply nested combinations of AND, OR, and NOT specifications with large datasets.
 */
class ComplexSpecificationCombinationsTest {

    // String literals used multiple times
    private static final String USER_PREFIX = "User";
    private static final String ID_0 = "0";
    private static final String ID_1 = "1";
    private static final String ID_2 = "2";
    private static final String ID_5 = "5";
    private static final String ID_100 = "100";
    private static final String ID_200 = "200";
    private static final String USER_100 = "User100";
    private static final String USER_200 = "User200";
    private static final String PERSON_100 = "Person100";
    private static final String PERSON_200 = "Person200";
    private static final String PERSON_333 = "Person333";
    
    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testComplexCombinationAndOfOrs() {
        // Create: (A OR B) AND (C OR D)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_5);
        
        OrSpecification<User> leftOr = new OrSpecification<>(specA, specB);
        OrSpecification<User> rightOr = new OrSpecification<>(specC, specD);
        AndSpecification<User> andOfOrs = new AndSpecification<>(leftOr, rightOr);
        
        // Test with entity matching (A OR B) AND (C OR D)
        User matchingUser1 = new User();
        matchingUser1.setId("150");  // Contains "1", ID_5, and "0"
        matchingUser1.setName("User150");  // Contains "User"
        assertThat(andOfOrs.test(matchingUser1)).isTrue();
        
        // Test with entity matching only left OR
        User matchingLeft = new User();
        matchingLeft.setId("111");  // Contains "1" but not "0" or ID_5
        matchingLeft.setName("User111");
        assertThat(andOfOrs.test(matchingLeft)).isFalse();
        
        // Test with entity matching only right OR
        User matchingRight = new User();
        matchingRight.setId("250");  // Contains ID_5 and "0" but not "1"
        matchingRight.setName("Person250");  // Doesn't contain "User"
        assertThat(andOfOrs.test(matchingRight)).isFalse();
        
        // Test with entity matching neither
        User matchingNeither = new User();
        matchingNeither.setId("333");
        matchingNeither.setName(PERSON_333);
        assertThat(andOfOrs.test(matchingNeither)).isFalse();
        
        // Verify unique key structure
        String uniqueKey = andOfOrs.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationOrOfAnds() {
        // Create: (A AND B) OR (C AND D)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        
        AndSpecification<User> leftAnd = new AndSpecification<>(specA, specB);
        AndSpecification<User> rightAnd = new AndSpecification<>(specC, specD);
        OrSpecification<User> orOfAnds = new OrSpecification<>(leftAnd, rightAnd);
        
        // Test with entity matching first AND
        User matchingFirst = new User();
        matchingFirst.setId(ID_100);  // Contains "1" and "0"
        matchingFirst.setName(USER_100);  // Contains "User"
        assertThat(orOfAnds.test(matchingFirst)).isTrue();
        
        // Test with entity matching second AND
        User matchingSecond = new User();
        matchingSecond.setId(ID_200);  // Contains "2" and "0"
        matchingSecond.setName(PERSON_200);  // Doesn't contain "User"
        assertThat(orOfAnds.test(matchingSecond)).isTrue();
        
        // Test with entity matching both ANDs
        User matchingBoth = new User();
        matchingBoth.setId("120");  // Contains "1", "2", and "0"
        matchingBoth.setName("User120");  // Contains "User"
        assertThat(orOfAnds.test(matchingBoth)).isTrue();
        
        // Test with entity matching neither AND
        User matchingNeither = new User();
        matchingNeither.setId("333");
        matchingNeither.setName(PERSON_333);
        assertThat(orOfAnds.test(matchingNeither)).isFalse();
        
        // Verify unique key structure
        String uniqueKey = orOfAnds.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationNotOfAnd() {
        // Create: NOT(A AND B)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        AndSpecification<User> andSpec = new AndSpecification<>(specA, specB);
        NotSpecification<User> notOfAnd = new NotSpecification<>(andSpec);
        
        // Test with entity matching both A and B (should return false)
        User matchingBoth = new User();
        matchingBoth.setId(ID_100);
        matchingBoth.setName(USER_100);
        assertThat(notOfAnd.test(matchingBoth)).isFalse();
        
        // Test with entity matching only A (should return true)
        User matchingA = new User();
        matchingA.setId(ID_200);
        matchingA.setName(USER_200);
        assertThat(notOfAnd.test(matchingA)).isTrue();
        
        // Test with entity matching only B (should return true)
        User matchingB = new User();
        matchingB.setId(ID_100);
        matchingB.setName(PERSON_100);
        assertThat(notOfAnd.test(matchingB)).isTrue();
        
        // Test with entity matching neither (should return true)
        User matchingNeither = new User();
        matchingNeither.setId(ID_200);
        matchingNeither.setName(PERSON_200);
        assertThat(notOfAnd.test(matchingNeither)).isTrue();
        
        // Verify unique key structure
        String uniqueKey = notOfAnd.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationNotOfOr() {
        // Create: NOT(A OR B)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        OrSpecification<User> orSpec = new OrSpecification<>(specA, specB);
        NotSpecification<User> notOfOr = new NotSpecification<>(orSpec);
        
        // Test with entity matching both A and B (should return false)
        User matchingBoth = new User();
        matchingBoth.setId(ID_100);
        matchingBoth.setName(USER_100);
        assertThat(notOfOr.test(matchingBoth)).isFalse();
        
        // Test with entity matching only A (should return false)
        User matchingA = new User();
        matchingA.setId(ID_200);
        matchingA.setName(USER_200);
        assertThat(notOfOr.test(matchingA)).isFalse();
        
        // Test with entity matching only B (should return false)
        User matchingB = new User();
        matchingB.setId(ID_100);
        matchingB.setName(PERSON_100);
        assertThat(notOfOr.test(matchingB)).isFalse();
        
        // Test with entity matching neither (should return true)
        User matchingNeither = new User();
        matchingNeither.setId(ID_200);
        matchingNeither.setName(PERSON_200);
        assertThat(notOfOr.test(matchingNeither)).isTrue();
        
        // Verify unique key structure
        String uniqueKey = notOfOr.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationDeeplyNestedCombinations() {
        // Create: ((A AND B) OR (C AND D)) AND NOT(E OR F)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        Specification<User> specE = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("9");
        Specification<User> specF = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("8");
        
        AndSpecification<User> leftAnd = new AndSpecification<>(specA, specB);
        AndSpecification<User> rightAnd = new AndSpecification<>(specC, specD);
        OrSpecification<User> innerOr = new OrSpecification<>(leftAnd, rightAnd);
        
        OrSpecification<User> rightOr = new OrSpecification<>(specE, specF);
        NotSpecification<User> notSpec = new NotSpecification<>(rightOr);
        
        AndSpecification<User> outerAnd = new AndSpecification<>(innerOr, notSpec);
        
        // Test with entity matching (A AND B) and NOT(E OR F)
        User matchingUser1 = new User();
        matchingUser1.setId(ID_100);  // Contains "1" and "0", doesn't contain "9" or "8"
        matchingUser1.setName(USER_100);
        assertThat(outerAnd.test(matchingUser1)).isTrue();
        
        // Test with entity matching (C AND D) and NOT(E OR F)
        User matchingUser2 = new User();
        matchingUser2.setId(ID_200);  // Contains ID_2 and "0", doesn't contain "9" or "8"
        matchingUser2.setName(PERSON_200);
        assertThat(outerAnd.test(matchingUser2)).isTrue();
        
        // Test with entity matching (A AND B) but also matching (E OR F)
        User matchingButFails = new User();
        matchingButFails.setId("190");  // Contains "1", "9", and "0"
        matchingButFails.setName("User190");
        assertThat(outerAnd.test(matchingButFails)).isFalse();
        
        // Test with entity not matching any condition
        User nonMatching = new User();
        nonMatching.setId("333");
        nonMatching.setName(PERSON_333);
        assertThat(outerAnd.test(nonMatching)).isFalse();
        
        // Verify unique key structure
        String uniqueKey = outerAnd.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationWithLargeDataset() {
        // Create complex specification: (A AND B) OR (NOT(C) AND D)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains("User_1");
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains("9");
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_5);
        
        AndSpecification<User> leftAnd = new AndSpecification<>(specA, specB);
        NotSpecification<User> notC = new NotSpecification<>(specC);
        AndSpecification<User> rightAnd = new AndSpecification<>(notC, specD);
        OrSpecification<User> complexSpec = new OrSpecification<>(leftAnd, rightAnd);
        
        // Generate large dataset (10K users)
        List<User> users = generator.generateUsers(10_000);
        
        // Filter using complex specification
        List<User> matchingUsers = users.stream()
            .filter(complexSpec::test)
            .toList();
        
        // Verify results
        assertThat(matchingUsers).isNotEmpty();
        
        // Verify all matching users satisfy the complex condition
        for (User user : matchingUsers) {
            boolean matchesLeftAnd = user.getName().contains("User_1") && user.getIdentity().contains(ID_0 );
            boolean matchesRightAnd = !user.getIdentity().contains("9") && user.getIdentity().contains(ID_5);
            assertThat(matchesLeftAnd || matchesRightAnd).isTrue();
        }
        
        // Verify count is reasonable for 10K dataset
        assertThat(matchingUsers.size()).isGreaterThan(0).isLessThan(10_000);
    }

    @Test
    void testComplexCombinationMultipleNegations() {
        // Create: NOT(NOT(A) OR NOT(B)) which equals (A AND B)
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        
        NotSpecification<User> notA = new NotSpecification<>(specA);
        NotSpecification<User> notB = new NotSpecification<>(specB);
        OrSpecification<User> orSpec = new OrSpecification<>(notA, notB);
        NotSpecification<User> complexNot = new NotSpecification<>(orSpec);
        
        // This should be equivalent to (A AND B)
        AndSpecification<User> simpleAnd = new AndSpecification<>(specA, specB);
        
        // Test with large dataset
        List<User> users = generator.generateUsers(1000);
        
        // Verify both specifications produce same results
        for (User user : users) {
            boolean complexResult = complexNot.test(user);
            boolean simpleResult = simpleAnd.test(user);
            assertThat(complexResult).isEqualTo(simpleResult);
        }
        
        // Verify unique key structure
        String uniqueKey = complexNot.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationFiveLayerNesting() {
        // Create deeply nested: (((A OR B) AND C) OR D) AND E
        Specification<User> specA = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> specB = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> specC = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        Specification<User> specD = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_5);
        Specification<User> specE = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_2);
        
        OrSpecification<User> layer1 = new OrSpecification<>(specA, specB);
        AndSpecification<User> layer2 = new AndSpecification<>(layer1, specC);
        OrSpecification<User> layer3 = new OrSpecification<>(layer2, specD);
        AndSpecification<User> layer4 = new AndSpecification<>(layer3, specE);
        
        // Test with entity matching all conditions
        User matchingAll = new User();
        matchingAll.setId("1025");  // Contains "1", "0", "2", ID_5
        matchingAll.setName("User1025");
        assertThat(layer4.test(matchingAll)).isTrue();
        
        // Test with entity matching partial conditions
        User matchingPartial = new User();
        matchingPartial.setId("520");  // Contains ID_5, "2", "0"
        matchingPartial.setName("Person520");
        assertThat(layer4.test(matchingPartial)).isTrue();
        
        // Test with entity not matching E
        User notMatchingE = new User();
        notMatchingE.setId("105");  // Contains "1", "0", ID_5 but not "2"
        notMatchingE.setName("User105");
        assertThat(layer4.test(notMatchingE)).isFalse();
        
        // Verify unique key has deep nesting
        String uniqueKey = layer4.toString();
        assertThat(uniqueKey).isNotNull();
    }

    @Test
    void testComplexCombinationPerformanceWithLargeDataset() {
        // Create complex specification with multiple levels
        Specification<User> spec1 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains(USER_PREFIX);
        Specification<User> spec2 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_1);
        Specification<User> spec3 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_0 );
        Specification<User> spec4 = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.id).contains(ID_5);
        
        AndSpecification<User> and1 = new AndSpecification<>(spec1, spec2);
        AndSpecification<User> and2 = new AndSpecification<>(spec3, spec4);
        OrSpecification<User> or1 = new OrSpecification<>(and1, and2);
        NotSpecification<User> not1 = new NotSpecification<>(or1);
        
        // Generate large dataset (10K users)
        List<User> users = generator.generateUsers(10_000);
        
        // Measure performance
        long startTime = System.currentTimeMillis();
        List<User> results = users.stream()
            .filter(not1::test)
            .toList();
        long endTime = System.currentTimeMillis();
        
        // Verify performance (should complete quickly even with 10K entities)
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(1000); // Should complete in less than 1 second
        
        // Verify results are correct
        assertThat(results).isNotEmpty();
        for (User user : results) {
            // Verify the NOT condition is satisfied
            boolean matchesAnd1 = user.getName().contains(USER_PREFIX) && user.getIdentity().contains(ID_1);
            boolean matchesAnd2 = user.getIdentity().contains(ID_0 ) && user.getIdentity().contains(ID_5);
            boolean matchesOr = matchesAnd1 || matchesAnd2;
            assertThat(matchesOr).isFalse(); // NOT(or) should be true, so or should be false
        }
    }
}
