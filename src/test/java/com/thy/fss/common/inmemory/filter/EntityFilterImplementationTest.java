package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test to verify that generated filter classes correctly implement EntityFilter interface.
 */
@DisplayName("EntityFilter Implementation Tests")
class EntityFilterImplementationTest {

    @Test
    @DisplayName("TestUserFilter should implement EntityFilter<TestUser>")
    void testUserFilterShouldImplementEntityFilter() {
        // Given
        TestUserFilter filter = new TestUserFilter();

        // Then
        assertInstanceOf(EntityFilter.class, filter);
        assertEquals(TestUser.class, filter.getEntityClass());
    }

    @Test
    @DisplayName("CollectionEntityFilter should implement EntityFilter<CollectionEntity>")
    void collectionEntityFilterShouldImplementEntityFilter() {
        // Given
        CollectionEntityFilter filter = new CollectionEntityFilter();

        // Then
        assertInstanceOf(EntityFilter.class, filter);
        assertEquals(CollectionEntity.class, filter.getEntityClass());
    }

    @Test
    @DisplayName("EntityFilter should provide type safety")
    void entityFilterShouldProvideTypeSafety() {
        // Given
        TestUserFilter userFilter = new TestUserFilter();
        CollectionEntityFilter collectionFilter = new CollectionEntityFilter();

        // When & Then - Type safety at compile time
        EntityFilter<TestUser> typedUserFilter = userFilter;
        EntityFilter<CollectionEntity> typedCollectionFilter = collectionFilter;

        assertEquals(TestUser.class, typedUserFilter.getEntityClass());
        assertEquals(CollectionEntity.class, typedCollectionFilter.getEntityClass());
    }

}