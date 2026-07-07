package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestProfile;
import com.thy.fss.common.inmemory.common.model.TestTag;
import com.thy.fss.common.inmemory.common.model.TestUser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching test data to improve test performance
 * by avoiding repeated object creation.
 */
public class CachedTestDataFactory {

    private static final String STANDARD = "standard";
    private static final String TEST_COM = "@test.com";

    private static final Map<String, List<TestUser>> USER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<TestProfile>> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<TestTag>> TAG_CACHE = new ConcurrentHashMap<>();

    /**
     * Returns a standard set of test users for general testing.
     * This data is cached for performance.
     */
    public static List<TestUser> getStandardUserSet() {
        return USER_CACHE.computeIfAbsent(STANDARD, key ->
                Arrays.asList(
                        TestDataGenerator.createUser("Alice", 25, "alice@example.com", true),
                        TestDataGenerator.createUser("Bob", 30, "bob@example.com", false),
                        TestDataGenerator.createUser("Charlie", 35, "charlie@example.com", true),
                        TestDataGenerator.createUser("David", 40, "david@example.com", false),
                        TestDataGenerator.createUser("Eve", 28, "eve@example.com", true)
                )
        );
    }

    /**
     * Returns a set of test users specifically designed for filter testing.
     * Includes users with various ages, names, and active states.
     */
    public static List<TestUser> getFilterTestUserSet() {
        return USER_CACHE.computeIfAbsent("filter", key ->
                Arrays.asList(
                        TestDataGenerator.createUser("FilterUser1", 20, "filter1@test.com", true),
                        TestDataGenerator.createUser("FilterUser2", 25, "filter2@test.com", false),
                        TestDataGenerator.createUser("FilterUser3", 30, "filter3@test.com", true),
                        TestDataGenerator.createUser("FilterUser4", 35, "filter4@test.com", false),
                        TestDataGenerator.createUser("FilterUser5", 40, "filter5@test.com", true)
                )
        );
    }

    /**
     * Returns a large set of test users for performance testing.
     * Contains 100 users with varied data.
     */
    public static List<TestUser> getLargeUserSet() {
        return USER_CACHE.computeIfAbsent("large", key -> {
            List<TestUser> users = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                users.add(TestDataGenerator.createUser(
                        "User" + i,
                        20 + (i % 50), // Ages from 20 to 69
                        "user" + i + TEST_COM,
                        i % 2 == 0 // Alternating active/inactive
                ));
            }
            return users;
        });
    }

    /**
     * Returns a small set of test users for quick tests.
     */
    public static List<TestUser> getSmallUserSet() {
        return USER_CACHE.computeIfAbsent("small", key ->
                Arrays.asList(
                        TestDataGenerator.createUser("SmallUser1", 25, "small1@test.com", true),
                        TestDataGenerator.createUser("SmallUser2", 30, "small2@test.com", false)
                )
        );
    }

    /**
     * Returns test users with specific age ranges for age-based filtering tests.
     */
    public static List<TestUser> getAgeRangeUserSet() {
        return USER_CACHE.computeIfAbsent("ageRange", key ->
                Arrays.asList(
                        TestDataGenerator.createUser("Young1", 18, "young1@test.com", true),
                        TestDataGenerator.createUser("Young2", 22, "young2@test.com", true),
                        TestDataGenerator.createUser("Middle1", 35, "middle1@test.com", true),
                        TestDataGenerator.createUser("Middle2", 42, "middle2@test.com", false),
                        TestDataGenerator.createUser("Senior1", 55, "senior1@test.com", true),
                        TestDataGenerator.createUser("Senior2", 68, "senior2@test.com", false)
                )
        );
    }

    /**
     * Returns a set of test profiles for profile-related testing.
     */
    public static List<TestProfile> getStandardProfileSet() {
        return PROFILE_CACHE.computeIfAbsent(STANDARD, key ->
                Arrays.asList(
                        TestDataGenerator.createProfile("Profile1"),
                        TestDataGenerator.createProfile("Profile2"),
                        TestDataGenerator.createProfile("Profile3")
                )
        );
    }

    /**
     * Returns a set of test tags for tag-related testing.
     */
    public static List<TestTag> getStandardTagSet() {
        return TAG_CACHE.computeIfAbsent(STANDARD, key ->
                Arrays.asList(
                        TestDataGenerator.createTag("Java"),
                        TestDataGenerator.createTag("Spring"),
                        TestDataGenerator.createTag("Testing"),
                        TestDataGenerator.createTag("Performance"),
                        TestDataGenerator.createTag("Integration")
                )
        );
    }

    /**
     * Returns a copy of the standard user set to avoid test interference.
     * Use this when tests might modify the returned data.
     */
    public static List<TestUser> getStandardUserSetCopy() {
        List<TestUser> original = getStandardUserSet();
        List<TestUser> copy = new ArrayList<>();
        for (TestUser user : original) {
            copy.add(TestDataGenerator.createUser(
                    user.getName(),
                    user.getAge(),
                    user.getEmail(),
                    user.getActive()
            ));
        }
        return copy;
    }

    /**
     * Returns a copy of the filter test user set.
     */
    public static List<TestUser> getFilterTestUserSetCopy() {
        List<TestUser> original = getFilterTestUserSet();
        List<TestUser> copy = new ArrayList<>();
        for (TestUser user : original) {
            copy.add(TestDataGenerator.createUser(
                    user.getName(),
                    user.getAge(),
                    user.getEmail(),
                    user.getActive()
            ));
        }
        return copy;
    }

    /**
     * Creates a new user with a unique name to avoid conflicts in tests.
     */
    public static TestUser createUniqueUser() {
        return TestDataGenerator.createUser(
                "UniqueUser" + System.currentTimeMillis(),
                25 + (new Random().nextInt() * 40), // Random age 25-64
                "unique" + System.currentTimeMillis() + TEST_COM,
                Math.random() > 0.5 // Random active state
        );
    }

    /**
     * Creates a list of unique users for tests that need fresh data.
     */
    public static List<TestUser> createUniqueUserList(int count) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createUniqueUser());
        }
        return users;
    }

    /**
     * Clears all cached data. Use this in test cleanup if needed.
     */
    public static void clearCache() {
        USER_CACHE.clear();
        PROFILE_CACHE.clear();
        TAG_CACHE.clear();
    }

    /**
     * Returns cache statistics for monitoring cache effectiveness.
     */
    public static CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
                USER_CACHE.size(),
                PROFILE_CACHE.size(),
                TAG_CACHE.size()
        );
    }

    /**
     * Statistics about cache usage.
     */
    public record CacheStatistics(int userCacheSize, int profileCacheSize, int tagCacheSize) {

        public int getTotalCacheSize() {
            return userCacheSize + profileCacheSize + tagCacheSize;
        }

        @Override
        public String toString() {
            return String.format("CacheStatistics{users=%d, profiles=%d, tags=%d, total=%d}",
                    userCacheSize, profileCacheSize, tagCacheSize, getTotalCacheSize());
        }
    }
}