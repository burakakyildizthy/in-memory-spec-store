package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.testmodel.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test for the new test utility classes.
 * Ensures all utilities are working correctly.
 */
class TestUtilitiesVerificationTest {
    
    private static final String NULL = "null";
    

    @AfterEach
    void cleanup() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    void testLargeDatasetGeneratorGeneratesUsers() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        
        List<User> users = generator.generateUsers(1000);
        
        assertThat(users).hasSize(1000);
        User firstUser = users.get(0);
        assertThat(firstUser)
                .extracting(User::getName)
                .isEqualTo( "User_0" );
    }

    @Test
    void testLargeDatasetGeneratorGeneratesCompleteDataset() {
        LargeDatasetGenerator generator = LargeDatasetGenerator.create();
        
        LargeDatasetGenerator.CompleteDataset dataset = generator.generateCompleteDataset(100);
        
        assertThat(dataset.getUsers()).hasSize(100);
        assertThat(dataset.getOrders()).isNotEmpty();
    }

    @Test
    void testMetaModelTestHelperVerifiesGeneratedClasses() {
        assertThat(MetaModelTestHelper.hasMetaModelClass(User.class)).isTrue();
        assertThat(MetaModelTestHelper.hasFilterClass(User.class)).isTrue();
        assertThat(MetaModelTestHelper.hasSpecificationServiceClass(User.class)).isTrue();
    }

    @Test
    void testMetaModelTestHelperGetsMetaModelClass() {
        Class<?> metaModelClass = MetaModelTestHelper.getMetaModelClass(User.class);
        
        assertThat(metaModelClass).isNotNull();
        assertThat(metaModelClass.getSimpleName()).isEqualTo("User_");
    }

    @Test
    void testCoverageTestHelperTestsNullSafety() {
        CoverageTestHelper.testNullSafety(
            input -> input == null ? NULL : "not null",
            NULL
        );
    }

    @Test
    void testCoverageTestHelperTestsEmptyCollection() {
        CoverageTestHelper.testEmptyCollection(
                List::isEmpty,
            true
        );
    }
}
