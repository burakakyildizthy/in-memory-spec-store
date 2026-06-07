package com.thy.fss.common.inmemory.common.base;

import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestProfile;
import com.thy.fss.common.inmemory.common.model.TestTag;
import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

/**
 * Base class for integration tests with common setup and utilities.
 * Provides integration test infrastructure including query engines and test data.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public abstract class BaseIntegrationTest {

    // TestDataGenerator has static methods, no instance needed

    /**
     * Specification query engine for testing query operations.
     */
    protected SpecificationQueryEngine<TestUser> userQueryEngine;

    /**
     * Test data list for integration testing.
     */
    protected List<TestUser> testUsers;

    /**
     * Setup method called before each test.
     * Initializes test infrastructure including query engines and test data.
     */
    @BeforeEach
    public void setUp() {
        TestDataGenerator.resetCounters();

        // Initialize query engine
        this.userQueryEngine = new SpecificationQueryEngine<>(TestUser.class);

        // Initialize test data
        TestDataGenerator.TestDataSet dataSet = TestDataGenerator.createCompleteDataSet(5);
        this.testUsers = dataSet.users();
    }

    /**
     * Creates test data with specified user count.
     *
     * @param userCount The number of users to create
     * @return List of test users
     */
    protected List<TestUser> createTestUsers(int userCount) {
        TestDataGenerator.TestDataSet dataSet = TestDataGenerator.createCompleteDataSet(userCount);
        return dataSet.users();
    }

    /**
     * Creates a test user with default values for quick testing.
     *
     * @return A test user with name "Integration Test User" and age 35
     */
    protected TestUser createDefaultUser() {
        return TestDataGenerator.createUser("Integration Test User", 35);
    }

    /**
     * Creates a test profile with default values for quick testing.
     *
     * @return A test profile with a default user
     */
    protected TestProfile createDefaultProfile() {
        return TestDataGenerator.createProfile("Default Profile");
    }

    /**
     * Creates a test tag with default values for quick testing.
     *
     * @return A test tag with name "Integration Test Tag"
     */
    protected TestTag createDefaultTag() {
        return TestDataGenerator.createTag("Integration Test Tag");
    }

    /**
     * Creates a complete test dataset for comprehensive integration testing.
     *
     * @param size The number of entities to create
     * @return A complete test dataset
     */
    protected TestDataGenerator.TestDataSet createTestDataSet(int size) {
        return TestDataGenerator.createCompleteDataSet(size);
    }

    /**
     * Performs cleanup after integration tests.
     * Override this method in subclasses to add specific cleanup logic.
     */
    protected void cleanup() {
        TestDataGenerator.resetCounters();
        if (testUsers != null) {
            testUsers.clear();
        }
    }
}