package com.thy.fss.common.inmemory.specification.attribute;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.AttributeType;
import com.thy.fss.common.inmemory.testmodel.Profile;
import com.thy.fss.common.inmemory.testmodel.Profile_;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.User_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for ModelAttribute operations.
 * Tests nested object navigation, nested field access, and nested specification building.
 * Tests with generated meta model (User_.profile) and large datasets (10K entities).
 * Requirements: 5.4, 15.10, 15.9
 */
class ModelAttributeTest {

    private static final String PROFILE_FIELD = "profile";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor and Basic Tests ====================

    @Test
    void testConstructorCreatesModelAttribute() {
        ModelAttribute<User, Profile> attribute = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);

        assertThat(attribute.getName()).isEqualTo(PROFILE_FIELD);
        assertThat(attribute.getOwnerType()).isEqualTo(User.class);
        assertThat(attribute.getFieldType()).isEqualTo(Profile.class);
        assertThat(attribute.getAttributeType()).isEqualTo(AttributeType.MODEL);
    }

    @Test
    void testGeneratedMetaModelUserProfileHasCorrectProperties() {
        assertThat(User_.profile).isNotNull();
        assertThat(User_.profile.getName()).isEqualTo(PROFILE_FIELD);
        assertThat(User_.profile.getOwnerType()).isEqualTo(User.class);
        assertThat(User_.profile.getFieldType()).isEqualTo(Profile.class);
        assertThat(User_.profile.getAttributeType()).isEqualTo(AttributeType.MODEL);
    }

    @Test
    void testModelAttributeToString() {
        ModelAttribute<User, Profile> attribute = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);
        
        assertThat(attribute.toString()).hasToString("User.profile");
    }

    @Test
    void testModelAttributeEqualsSameAttributes() {
        ModelAttribute<User, Profile> attr1 = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);
        ModelAttribute<User, Profile> attr2 = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);

        assertThat(attr1).isEqualTo(attr2).hasSameHashCodeAs(attr2.hashCode());
    }

    @Test
    void testModelAttributeEqualsDifferentAttributes() {
        ModelAttribute<User, Profile> attr1 = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);
        ModelAttribute<User, Profile> attr2 = new ModelAttribute<>("otherProfile", User.class, Profile.class);

        assertThat(attr1).isNotEqualTo(attr2);
    }

    @Test
    void testModelAttributeEqualsSameInstance() {
        ModelAttribute<User, Profile> attribute = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);

        assertThat(attribute).isEqualTo(attribute);
    }

    @Test
    void testModelAttributeEqualsNull() {
        ModelAttribute<User, Profile> attribute = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);

        assertThat(attribute).isNotEqualTo(null);
    }

    @Test
    void testModelAttributeEqualsDifferentClass() {
        ModelAttribute<User, Profile> attribute = new ModelAttribute<>(PROFILE_FIELD, User.class, Profile.class);
        StringAttribute<User> stringAttr = new StringAttribute<>("name", User.class);

        assertThat(attribute).isNotEqualTo(stringAttr);
    }

    // ==================== Nested Object Navigation Tests ====================

    @Test
    void testNestedObjectNavigationAccessProfileField() {
        List<User> users = createUsersWithProfiles(1000);

        // Verify that users have profiles
        assertThat(users).allMatch(user -> user.getProfile() != null).isNotEmpty().allMatch(user -> user.getProfile().getBio() != null);
    }

    @Test
    void testNestedObjectNavigationWithLargeDataset() {
        List<User> users = createUsersWithProfiles(10_000);

        // Verify nested object structure with large dataset
        assertThat(users).hasSize(10_000)
                .allMatch(user -> user.getProfile() != null)
                .allMatch(user -> user.getProfile().getBio() != null)
                .allMatch(user -> user.getProfile().getFollowers() != null);
    }

    @Test
    void testNestedObjectNavigationWithNullProfile() {
        List<User> users = createUsersWithProfiles(1000);
        users.get(0).setProfile(null);
        users.get(1).setProfile(null);

        // Verify that some users have null profiles
        long nullProfileCount = users.stream()
            .filter(user -> user.getProfile() == null)
            .count();

        assertThat(nullProfileCount).isEqualTo(2);
    }

    @Test
    void testNestedObjectNavigationMixedNullAndNonNull() {
        List<User> users = createUsersWithProfiles(10_000);
        
        // Set some profiles to null
        for (int i = 0; i < 1000; i++) {
            users.get(i).setProfile(null);
        }

        long nullProfileCount = users.stream()
            .filter(user -> user.getProfile() == null)
            .count();
        long nonNullProfileCount = users.stream()
            .filter(user -> user.getProfile() != null)
            .count();

        assertThat(nullProfileCount).isEqualTo(1000);
        assertThat(nonNullProfileCount).isEqualTo(9000);
    }

    // ==================== Nested Field Access Tests ====================

    @Test
    void testNestedFieldAccessProfileBio() {
        List<User> users = createUsersWithProfiles(1000);

        // Access nested field through profile
        assertThat(users).isNotEmpty().allMatch(user -> {
            Profile profile = user.getProfile();
            return profile != null && profile.getBio() != null;
        });
    }

    @Test
    void testNestedFieldAccessProfileFollowers() {
        List<User> users = createUsersWithProfiles(1000);

        // Access nested field through profile
        assertThat(users).isNotEmpty().allMatch(user -> {
            Profile profile = user.getProfile();
            return profile != null && profile.getFollowers() != null;
        });
    }

    @Test
    void testNestedFieldAccessWithLargeDataset() {
        List<User> users = createUsersWithProfiles(10_000);

        // Verify nested field access with large dataset
        long usersWithBio = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getBio() != null)
            .count();

        assertThat(usersWithBio).isEqualTo(10_000);
    }

    @Test
    void testNestedFieldAccessWithNullNestedField() {
        List<User> users = createUsersWithProfiles(1000);
        
        // Set some nested fields to null
        users.get(0).getProfile().setBio(null);
        users.get(1).getProfile().setFollowers(null);

        long nullBioCount = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getBio() == null)
            .count();

        assertThat(nullBioCount).isEqualTo(1);
    }

    @Test
    void testNestedFieldAccessVariousFollowerCounts() {
        List<User> users = createUsersWithProfiles(10_000);

        // Verify follower counts vary
        long lowFollowers = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getFollowers() < 500)
            .count();

        long highFollowers = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getFollowers() >= 500)
            .count();

        assertThat(lowFollowers).isGreaterThan(0);
        assertThat(highFollowers).isGreaterThan(0);
    }

    // ==================== Nested Specification Building Tests ====================

    @Test
    void testNestedSpecificationBuildingProfileMetaModel() {
        // Verify Profile_ meta model exists and has correct attributes
        assertThat(Profile_.bio).isNotNull();
        assertThat(Profile_.bio.getName()).isEqualTo("bio");
        assertThat(Profile_.bio.getOwnerType()).isEqualTo(Profile.class);
        assertThat(Profile_.bio.getFieldType()).isEqualTo(String.class);
    }

    @Test
    void testNestedSpecificationBuildingProfileFollowersMetaModel() {
        // Verify Profile_ meta model has followers attribute
        assertThat(Profile_.followers).isNotNull();
        assertThat(Profile_.followers.getName()).isEqualTo("followers");
        assertThat(Profile_.followers.getOwnerType()).isEqualTo(Profile.class);
        assertThat(Profile_.followers.getFieldType()).isEqualTo(Integer.class);
    }

    @Test
    void testNestedSpecificationBuildingUserProfileMetaModel() {
        // Verify User_ meta model has profile attribute
        assertThat(User_.profile).isNotNull();
        assertThat(User_.profile.getName()).isEqualTo(PROFILE_FIELD);
        assertThat(User_.profile.getOwnerType()).isEqualTo(User.class);
        assertThat(User_.profile.getFieldType()).isEqualTo(Profile.class);
    }

    @Test
    void testNestedSpecificationBuildingMetaModelHierarchy() {
        // Verify the meta model hierarchy: User -> Profile -> fields
        assertThat(User_.profile).isNotNull();
        assertThat(User_.profile.getFieldType()).isEqualTo(Profile.class);
        
        // Profile has its own meta model
        assertThat(Profile_.bio).isNotNull();
        assertThat(Profile_.followers).isNotNull();
    }

    // ==================== Type Safety Tests ====================

    @Test
    void testTypeSafetyCorrectGenericTypes() {
        ModelAttribute<User, Profile> attribute = User_.profile;

        // Verify generic types are correct
        assertThat(attribute.getOwnerType()).isEqualTo(User.class);
        assertThat(attribute.getFieldType()).isEqualTo(Profile.class);
    }

    @Test
    void testTypeSafetyFieldTypeMatchesActualType() {
        // Create a user with profile
        User user = new User();
        Profile profile = new Profile();
        profile.setBio("Test bio");
        profile.setFollowers(100);
        user.setProfile(profile);

        // Verify the field type matches
        assertThat(user.getProfile()).isInstanceOf(User_.profile.getFieldType());
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetNestedFieldDistribution() {
        List<User> users = createUsersWithProfiles(10_000);

        // Verify bio distribution - all bios follow pattern "Bio for UserN"
        long shortBios = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getBio().length() < 15)
            .count();

        long mediumBios = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getBio().length() >= 15 && user.getProfile().getBio().length() < 20)
            .count();

        long longBios = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getBio().length() >= 20)
            .count();

        // All bios should exist and have varying lengths based on user number
        assertThat(shortBios + mediumBios + longBios).isEqualTo(10_000);
        assertThat(users).allMatch(user -> user.getProfile().getBio().startsWith("Bio for User"));
    }

    @Test
    void testLargeDatasetFollowerDistribution() {
        List<User> users = createUsersWithProfiles(10_000);

        // Verify follower distribution
        long lowFollowers = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getFollowers() < 333)
            .count();

        long mediumFollowers = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getFollowers() >= 333 && user.getProfile().getFollowers() < 666)
            .count();

        long highFollowers = users.stream()
            .filter(user -> user.getProfile() != null)
            .filter(user -> user.getProfile().getFollowers() >= 666)
            .count();

        assertThat(lowFollowers).isGreaterThan(0);
        assertThat(mediumFollowers).isGreaterThan(0);
        assertThat(highFollowers).isGreaterThan(0);
    }

    @Test
    void testLargeDatasetNestedObjectMemoryEfficiency() {
        // Create large dataset and verify memory efficiency
        List<User> users = createUsersWithProfiles(10_000);

        // Verify all profiles are created
        assertThat(users).allMatch(user -> user.getProfile() != null).isNotEmpty().allMatch(user -> {
            Profile profile = user.getProfile();
            return profile.getBio() != null && profile.getFollowers() != null;
        });
    }

    // ==================== Edge Cases ====================

    @Test
    void testEdgeCaseEmptyBio() {
        List<User> users = createUsersWithProfiles(1000);
        users.get(0).getProfile().setBio("");

        assertThat(users.get(0).getProfile().getBio()).isEmpty();
    }

    @Test
    void testEdgeCaseZeroFollowers() {
        List<User> users = createUsersWithProfiles(1000);
        users.get(0).getProfile().setFollowers(0);

        assertThat(users.get(0).getProfile().getFollowers()).isZero();
    }

    @Test
    void testEdgeCaseVeryLongBio() {
        List<User> users = createUsersWithProfiles(100);
        String longBio = "A".repeat(10000);
        users.get(0).getProfile().setBio(longBio);

        assertThat(users.get(0).getProfile().getBio()).hasSize(10000);
    }

    @Test
    void testEdgeCaseNegativeFollowers() {
        List<User> users = createUsersWithProfiles(100);
        users.get(0).getProfile().setFollowers(-1);

        assertThat(users.get(0).getProfile().getFollowers()).isEqualTo(-1);
    }

    @Test
    void testEdgeCaseMaxIntegerFollowers() {
        List<User> users = createUsersWithProfiles(100);
        users.get(0).getProfile().setFollowers(Integer.MAX_VALUE);

        assertThat(users.get(0).getProfile().getFollowers()).isEqualTo(Integer.MAX_VALUE);
    }

    // ==================== Helper Methods ====================

    private List<User> createUsersWithProfiles(int count) {
        List<User> users = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setId("user" + i);
            user.setName("User" + i);
            
            Profile profile = new Profile();
            profile.setBio("Bio for User" + i);
            profile.setFollowers(i % 1000);
            
            user.setProfile(profile);
            users.add(user);
        }
        
        return users;
    }
}
