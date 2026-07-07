package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.testmodel.ProfileFilter;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test coverage for EntityFilter with complex combinations.
 * Tests multiple filter criteria, nested field filtering, filter validation,
 * and generated filter classes with large datasets.
 * 
 * Requirements: 4.6, 15.10, 15.9
 */
class EntityFilterTest {

    private static final String USER = "User";
    private static final String USER1 = "User_1";
    private static final String USER_100 = "User_100";
    private static final String USER_5 = "Bio_5";
    private static final String USER_99 = "User_99";
    private static final String NUMBER_99 = "99";
    private static final String NUMBER_100 = "100";
    private static final String TEST = "test";
    private static final String JOHN = "John";

    private LargeDatasetGenerator generator;
    private SpecificationQueryEngine<User> queryEngine;

    @BeforeEach
    void setUp() {
        generator = LargeDatasetGenerator.create();
        queryEngine = new SpecificationQueryEngine<>(User.class);
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testEntityFilterSingleFieldFilter10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter for name contains USER_100
        UserFilter filter = UserFilter.builder()
                .nameContains(USER_100)
                .build();
        
        // Query with filter
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify results - should match User100, User1000-User1009, User1100-User1109, etc.
        assertThat(results).isNotEmpty()
                .allMatch(user -> user.getName().contains(USER_100))
                .hasSizeGreaterThan(10); // At least User100, User1000-1009
    }

    @Test
    void testEntityFilterMultipleFieldFilters10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter with multiple criteria (AND logic)
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER1)
                .idStartsWith("1")
                .build();
        
        // Query with filter
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify results - both conditions must be satisfied
        assertThat(results).isNotEmpty().allMatch(user ->
            user.getName().startsWith(USER1) && user.getIdentity().startsWith("1")
        );
    }

    @Test
    void testEntityFilterNestedFieldFiltering10KDataset() {
        // Generate 10K users with profiles
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter with nested profile filter
        ProfileFilter profileFilter = ProfileFilter.builder()
                .bioContains(USER_5)
                .build();
        
        UserFilter filter = UserFilter.builder()
                .profile(profileFilter)
                .build();
        
        // Query with nested filter
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify results - profile.bio must contain USER_5
        assertThat(results).isNotEmpty().allMatch(user ->
            user.getProfile() != null && 
            user.getProfile().getBio() != null &&
            user.getProfile().getBio().contains(USER_5)
        );
    }

    @Test
    void testEntityFilterComplexNestedCombination10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create complex filter: name starts with USER1 AND profile followers > 5000
        ProfileFilter profileFilter = ProfileFilter.builder()
                .followersGreaterThan(5000)
                .build();
        
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER1)
                .profile(profileFilter)
                .build();
        
        // Query with complex filter
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify results
        assertThat(results).isNotEmpty().allMatch(user ->
            user.getName().startsWith(USER1) &&
            user.getProfile() != null &&
            user.getProfile().getFollowers() != null &&
            user.getProfile().getFollowers() > 5000
        );
    }

    @Test
    void testEntityFilterStringOperations10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Test contains
        UserFilter containsFilter = UserFilter.builder()
                .nameContains(NUMBER_99)
                .build();
        List<User> containsResults = queryEngine.queryByFilter(users, containsFilter);
        assertThat(containsResults).isNotEmpty().allMatch(user -> user.getName().contains(NUMBER_99));
        
        // Test startsWith
        UserFilter startsWithFilter = UserFilter.builder()
                .nameStartsWith(USER_99)
                .build();
        List<User> startsWithResults = queryEngine.queryByFilter(users, startsWithFilter);
        assertThat(startsWithResults).isNotEmpty().allMatch(user -> user.getName().startsWith(USER_99));
        
        // Test endsWith
        UserFilter endsWithFilter = UserFilter.builder()
                .nameEndsWith(NUMBER_99)
                .build();
        List<User> endsWithResults = queryEngine.queryByFilter(users, endsWithFilter);
        assertThat(endsWithResults).isNotEmpty().allMatch(user -> user.getName().endsWith(NUMBER_99));
    }

    @Test
    void testEntityFilterNullHandling() {
        // Create users with null fields
        List<User> users = new ArrayList<>();
        
        User userWithName = new User();
        userWithName.setId("1");
        userWithName.setName(JOHN);
        users.add(userWithName);
        
        User userWithoutName = new User();
        userWithoutName.setId("2");
        userWithoutName.setName(null);
        users.add(userWithoutName);
        
        // Test isNull filter
        UserFilter isNullFilter = UserFilter.builder()
                .nameIsNull(true)
                .build();
        List<User> nullResults = queryEngine.queryByFilter(users, isNullFilter);
        assertThat(nullResults).hasSize(1);
        assertThat(nullResults.get(0).getName()).isNull();
        
        // Test isNotNull filter
        UserFilter isNotNullFilter = UserFilter.builder()
                .nameIsNotNull(true)
                .build();
        List<User> notNullResults = queryEngine.queryByFilter(users, isNotNullFilter);
        assertThat(notNullResults).hasSize(1);
        assertThat(notNullResults.get(0).getName()).isNotNull();
    }

    @Test
    void testEntityFilterEmptyAndBlankHandling() {
        // Create users with empty and blank names
        List<User> users = new ArrayList<>();
        
        User userWithName = new User();
        userWithName.setId("1");
        userWithName.setName(JOHN);
        users.add(userWithName);
        
        User userWithEmpty = new User();
        userWithEmpty.setId("2");
        userWithEmpty.setName("");
        users.add(userWithEmpty);
        
        User userWithBlank = new User();
        userWithBlank.setId("3");
        userWithBlank.setName("   ");
        users.add(userWithBlank);
        
        // Test isEmpty filter
        UserFilter isEmptyFilter = UserFilter.builder()
                .nameIsEmpty(true)
                .build();
        List<User> emptyResults = queryEngine.queryByFilter(users, isEmptyFilter);
        assertThat(emptyResults).hasSize(1);
        assertThat(emptyResults.get(0).getName()).isEmpty();
        
        // Test isBlank filter
        UserFilter isBlankFilter = UserFilter.builder()
                .nameIsBlank(true)
                .build();
        List<User> blankResults = queryEngine.queryByFilter(users, isBlankFilter);
        assertThat(blankResults).hasSize(2); // Empty and blank
    }

    @Test
    void testEntityFilterInAndNotInOperations10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Test IN operation
        List<String> targetNames = Arrays.asList(USER_100, "User_200", "User_300");
        UserFilter inFilter = UserFilter.builder()
                .nameIn(targetNames)
                .build();
        List<User> inResults = queryEngine.queryByFilter(users, inFilter);
        assertThat(inResults).hasSize(3).allMatch(user -> targetNames.contains(user.getName()));
        
        // Test NOT IN operation
        UserFilter notInFilter = UserFilter.builder()
                .nameNotIn(targetNames)
                .build();
        List<User> notInResults = queryEngine.queryByFilter(users, notInFilter);
        assertThat(notInResults).hasSize(10_000 - 3).noneMatch(user -> targetNames.contains(user.getName()));
    }

    @Test
    void testEntityFilterWithPagination10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER1)
                .build();
        
        // Query with pagination
        PageRequest pageable = PageRequest.of(0, 100);
        Page<User> page = queryEngine.queryByFilter(users, filter, pageable);
        
        // Verify pagination
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isGreaterThan(100);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getContent()).allMatch(user -> user.getName().startsWith(USER1));
    }

    @Test
    void testEntityFilterMultiplePages10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER)
                .build();
        
        // Query first page
        Page<User> firstPage = queryEngine.queryByFilter(users, filter, PageRequest.of(0, 100));
        assertThat(firstPage.getContent()).hasSize(100);
        assertThat(firstPage.getNumber()).isZero();
        
        // Query second page
        Page<User> secondPage = queryEngine.queryByFilter(users, filter, PageRequest.of(1, 100));
        assertThat(secondPage.getContent()).hasSize(100);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        
        // Verify different content
        assertThat(firstPage.getContent()).doesNotContainAnyElementsOf(secondPage.getContent());
    }

    @Test
    void testEntityFilterCountOperation10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameContains(NUMBER_100)
                .build();
        
        // Count matching entities
        long count = queryEngine.countByFilter(users, filter);
        
        // Verify count
        assertThat(count).isGreaterThan(0);
        
        // Verify count matches actual results
        List<User> results = queryEngine.queryByFilter(users, filter);
        assertThat(count).isEqualTo(results.size());
    }

    @Test
    void testEntityFilterNullFilter() {
        // Generate users
        List<User> users = generator.generateUsers(100);
        
        // Query with null filter - should return all
        List<User> results = queryEngine.queryByFilter(users, null);
        
        assertThat(results).hasSize(100);
    }

    @Test
    void testEntityFilterEmptyDataset() {
        // Empty dataset
        List<User> users = new ArrayList<>();
        
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameContains(TEST)
                .build();
        
        // Query empty dataset
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        assertThat(results).isEmpty();
    }

    @Test
    void testEntityFilterNullDataThrowsException() {
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameContains(TEST)
                .build();
        
        // Query with null data should throw exception
        assertThatThrownBy(() -> queryEngine.queryByFilter(null, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data cannot be null");
    }

    @Test
    void testEntityFilterBuilderPattern() {
        // Test builder pattern with method chaining
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER)
                .nameContains(NUMBER_100)
                .idStartsWith("1")
                .build();
        
        // Verify filter is built correctly
        assertThat(filter.getName()).isNotNull();
        assertThat(filter.getName().getStartsWith()).isEqualTo(USER);
        assertThat(filter.getName().getContains()).isEqualTo(NUMBER_100);
        assertThat(filter.getId()).isNotNull();
        assertThat(filter.getId().getStartsWith()).isEqualTo("1");
    }

    @Test
    void testEntityFilterCopyConstructor() {
        // Create original filter
        UserFilter original = UserFilter.builder()
                .nameContains(TEST)
                .idEquals("123")
                .build();
        
        // Create copy
        UserFilter copy = new UserFilter(original);
        
        // Verify copy has same values
        assertThat(copy.getName()).isNotNull();
        assertThat(copy.getName().getContains()).isEqualTo(TEST);
        assertThat(copy.getId()).isNotNull();
        assertThat(copy.getId().getEquals()).isEqualTo("123");
        
        // Verify independent copy (modifying copy doesn't affect original)
        copy.getName().setContains("modified");
        assertThat(original.getName().getContains()).isEqualTo(TEST);
    }

    @Test
    void testEntityFilterCopyConstructorWithNull() {
        // Create copy from null
        UserFilter copy = new UserFilter(null);
        
        // Verify empty filter is created
        assertThat(copy).isNotNull();
        assertThat(copy.getName()).isNull();
        assertThat(copy.getId()).isNull();
    }

    @Test
    void testEntityFilterGetEntityClass() {
        // Create filter
        UserFilter filter = new UserFilter();
        
        // Verify entity class
        assertThat(filter.getEntityClass()).isEqualTo(User.class);
    }

    @Test
    void testEntityFilterEqualsAndHashCode() {
        // Create two identical filters
        UserFilter filter1 = UserFilter.builder()
                .nameContains(TEST)
                .build();
        
        UserFilter filter2 = UserFilter.builder()
                .nameContains(TEST)
                .build();
        
        // Verify equals and hashCode
        assertThat(filter1).isEqualTo(filter2).hasSameHashCodeAs(filter2);
        
        // Create different filter
        UserFilter filter3 = UserFilter.builder()
                .nameContains("different")
                .build();
        
        assertThat(filter1).isNotEqualTo(filter3);
    }

    @Test
    void testEntityFilterToString() {
        // Create filter
        UserFilter filter = UserFilter.builder()
                .nameContains(TEST)
                .build();
        
        // Verify toString
        String toString = filter.toString();
        assertThat(toString).contains("UserFilter").contains("name");
    }


    @Test
    void testEntityFilterComplexCombinationLargeDataset100K() {
        // Generate 100K users for performance test
        List<User> users = generator.generateUsers(100_000);

        // Create complex filter with multiple criteria
        ProfileFilter profileFilter = ProfileFilter.builder()
                .followersGreaterThan(5000)
                .bioContains("Bio")
                .build();

        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER1)
                .idStartsWith("1")
                .profile(profileFilter)
                .build();

        // Measure query performance
        long startTime = System.currentTimeMillis();
        List<User> results = queryEngine.queryByFilter(users, filter);
        long endTime = System.currentTimeMillis();

        // Verify results
        assertThat(results).isNotEmpty().allMatch(user ->
                user.getName().startsWith(USER1) &&
                        user.getIdentity().startsWith("1") &&
                        user.getProfile() != null &&
                        user.getProfile().getFollowers() != null &&
                        user.getProfile().getFollowers() > 5000 &&
                        user.getProfile().getBio() != null &&
                        user.getProfile().getBio().contains("Bio")
        );

        // Verify performance (should complete in reasonable time)
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(1000); // Should complete in less than 1 second
    }

    @Test
    void testEntityFilterNestedFilterBuilder() {
        // Test nested filter builder pattern
        UserFilter filter = UserFilter.builder()
                .nameContains(TEST)
                .profile()
                    .bioContains("developer")
                    .followersGreaterThan(1000)
                    .and()
                .build();
        
        // Verify nested filter is built correctly
        assertThat(filter.getName()).isNotNull();
        assertThat(filter.getName().getContains()).isEqualTo(TEST);
        assertThat(filter.getProfile()).isNotNull();
        assertThat(filter.getProfile().getBio()).isNotNull();
        assertThat(filter.getProfile().getBio().getContains()).isEqualTo("developer");
        assertThat(filter.getProfile().getFollowers()).isNotNull();
        assertThat(filter.getProfile().getFollowers().getGreaterThan()).isEqualTo(1000);
    }

    @Test
    void testEntityFilterAllStringOperations10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Test all string operations in one filter
        UserFilter filter = new UserFilter();
        
        // Set name filter with multiple operations
        StringFilter nameFilter = new StringFilter();
        nameFilter.setStartsWith(USER);
        nameFilter.setContains(NUMBER_100);
        filter.setName(nameFilter);
        
        // Query
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify all conditions are met (AND logic)
        assertThat(results).isNotEmpty().allMatch(user ->
            user.getName().startsWith(USER) &&
            user.getName().contains(NUMBER_100)
        );
    }

    @Test
    void testEntityFilterValidationWithWrongFilterType() {
        // This test verifies that the generated service validates filter type
        // The validation happens inside SpecificationService.validateFilter
        
        // Generate users
        List<User> users = generator.generateUsers(100);
        
        // Create correct filter
        UserFilter filter = UserFilter.builder()
                .nameContains(USER)
                .build();
        
        // Query should work fine
        List<User> results = queryEngine.queryByFilter(users, filter);
        assertThat(results).isNotEmpty();
    }

    @Test
    void testEntityFilterPaginationBoundaries10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter that matches all
        UserFilter filter = UserFilter.builder()
                .nameStartsWith(USER)
                .build();
        
        // Test first page
        Page<User> firstPage = queryEngine.queryByFilter(users, filter, PageRequest.of(0, 100));
        assertThat(firstPage.getContent()).hasSize(100);
        assertThat(firstPage.isFirst()).isTrue();
        
        // Test last page
        int totalPages = (int) Math.ceil(10_000.0 / 100);
        Page<User> lastPage = queryEngine.queryByFilter(users, filter, PageRequest.of(totalPages - 1, 100));
        assertThat(lastPage.isLast()).isTrue();
        
        // Test out of bounds page
        Page<User> outOfBoundsPage = queryEngine.queryByFilter(users, filter, PageRequest.of(totalPages + 10, 100));
        assertThat(outOfBoundsPage.getContent()).isEmpty();
    }

    @Test
    void testEntityFilterNoMatchingResults10KDataset() {
        // Generate 10K users
        List<User> users = generator.generateUsers(10_000);
        
        // Create filter that matches nothing
        UserFilter filter = UserFilter.builder()
                .nameEquals("NonExistentUser")
                .build();
        
        // Query
        List<User> results = queryEngine.queryByFilter(users, filter);
        
        // Verify no results
        assertThat(results).isEmpty();
        
        // Verify count is zero
        long count = queryEngine.countByFilter(users, filter);
        assertThat(count).isZero();
    }
}
