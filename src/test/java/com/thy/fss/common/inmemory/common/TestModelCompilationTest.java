package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestProfile;
import com.thy.fss.common.inmemory.common.model.TestTag;
import com.thy.fss.common.inmemory.common.model.TestUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the common test models compile correctly,
 * the annotation processor generates the required classes,
 * and the test infrastructure works properly.
 */
class TestModelCompilationTest extends BaseIntegrationTest {

    private static final String JOHN_DOE = "John Doe";
    private static final String JANE_SMITH = "Jane Smith";
    private static final String USER_PROFILE = "User profile";
    private static final String IMPORTANT = "important";
    private static final String PRIORITY = "priority";
    private static final String JANE_SMITH_PROFILE = "Jane Smith Profile";
    private static final String TEST = "test";
    
    @Test
    void testModelCreation() {
        // Test basic model creation using constructors
        TestUser user = new TestUser(JOHN_DOE, 30);
        TestTag tag = new TestTag(IMPORTANT, PRIORITY);
        TestProfile profile = new TestProfile(USER_PROFILE);

        // Assertions to ensure objects are created correctly
        assertEquals(JOHN_DOE, user.getName());
        assertEquals(30, user.getAge());
        assertEquals(IMPORTANT, tag.getName());
        assertEquals(PRIORITY, tag.getCategory());
        assertEquals(USER_PROFILE, profile.getDescription());
        assertEquals(USER_PROFILE, profile.getDescription());
    }

    @Test
    void testDataGeneratorIntegration() {
        // Test using the test data generator
        TestUser user = TestDataGenerator.createUser(JANE_SMITH, 25);
        TestProfile profile = TestDataGenerator.createProfile(JANE_SMITH_PROFILE);
        TestTag tag = TestDataGenerator.createTag(TEST);

        assertNotNull(user.getIdentity());
        assertEquals(JANE_SMITH, user.getName());
        assertEquals(25, user.getAge());
        assertTrue(user.getActive());

        assertNotNull(profile.getId());
        assertEquals(JANE_SMITH_PROFILE, profile.getDescription());

        assertNotNull(tag.getId());
        assertEquals(TEST, tag.getName());
    }

    @Test
    void testBaseTestClassUtilities() {
        // Test using base class utilities
        TestUser defaultUser = super.createDefaultUser();
        TestProfile defaultProfile = super.createDefaultProfile();
        TestTag defaultTag = super.createDefaultTag();

        assertNotNull(defaultUser);
        assertEquals("Integration Test User", defaultUser.getName());
        assertEquals(35, defaultUser.getAge());

        assertNotNull(defaultProfile);
        assertNotNull(defaultProfile.getDescription());

        assertNotNull(defaultTag);
        assertEquals("Integration Test Tag", defaultTag.getName());
    }

    @Test
    void testCompleteDataSet() {
        // Test creating a complete dataset
        TestDataGenerator.TestDataSet dataSet = super.createTestDataSet(3);

        assertEquals(3, dataSet.users().size());
        assertEquals(3, dataSet.profiles().size());
        assertEquals(3, dataSet.tags().size());

        // Verify basic properties
        for (int i = 0; i < dataSet.profiles().size(); i++) {
            TestProfile profile = dataSet.profiles().get(i);
            assertNotNull(profile.getDescription());
        }
    }
}