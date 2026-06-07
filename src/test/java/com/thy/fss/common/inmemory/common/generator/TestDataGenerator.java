package com.thy.fss.common.inmemory.common.generator;

import com.thy.fss.common.inmemory.common.model.TestProfile;
import com.thy.fss.common.inmemory.common.model.TestTag;
import com.thy.fss.common.inmemory.common.model.TestUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Temporary test data generator for DashboardMetaModelTest
 */
public class TestDataGenerator {
    
    private static final String PROFILE = "Profile";
    private static final AtomicLong userIdCounter = new AtomicLong(1);
    private static final AtomicLong profileIdCounter = new AtomicLong(1);
    private static final AtomicLong tagIdCounter = new AtomicLong(1);

    public static TestUser createUser(String name, Integer age) {
        return new TestUser(
                userIdCounter.getAndIncrement(),
                name,
                age,
                name.toLowerCase().replace(" ", ".") + "@example.com",
                true
        );
    }

    public static TestUser createUser(String name, Integer age, String email, Boolean active) {
        return new TestUser(
                userIdCounter.getAndIncrement(),
                name,
                age,
                email,
                active
        );
    }

    public static List<TestUser> createUserList(String... names) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            users.add(createUser(names[i], 25 + i));
        }
        return users;
    }

    public static TestProfile createProfile(String description) {
        return new TestProfile(profileIdCounter.getAndIncrement(), description);
    }

    public static TestTag createTag(String name) {
        TestTag tag = new TestTag();
        tag.setId(tagIdCounter.getAndIncrement());
        tag.setName(name);
        return tag;
    }

    public static TestDataSet createCompleteDataSet(int size) {
        List<TestUser> users = new ArrayList<>();
        List<TestProfile> profiles = new ArrayList<>();
        List<TestTag> tags = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            users.add(createUser("User" + i, 20 + i));
            profiles.add(createProfile(PROFILE + i));
            tags.add(createTag("Tag" + i));
        }

        return new TestDataSet(users, profiles, tags);
    }

    public static void resetCounters() {
        userIdCounter.set(1);
        profileIdCounter.set(1);
        tagIdCounter.set(1);
    }

    public static List<TestUser> createUserList(int count) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createUser("User" + i, 20 + (i % 50)));
        }
        return users;
    }

    public static List<TestProfile> createProfileList(int count) {
        List<TestProfile> profiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            profiles.add(createProfile(PROFILE + i));
        }
        return profiles;
    }

    public static List<TestTag> createTagList(int count) {
        List<TestTag> tags = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tags.add(createTag("Tag" + i));
        }
        return tags;
    }

    public static TestTag createTag(String name, String category) {
        return new TestTag(name, category);
    }

    public static List<TestTag> createTags(String[] names) {
        List<TestTag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(createTag(name));
        }
        return tags;
    }

    /**
     * Container for complete test dataset
     */
    public record TestDataSet(List<TestUser> users, List<TestProfile> profiles, List<TestTag> tags) {
    }
}