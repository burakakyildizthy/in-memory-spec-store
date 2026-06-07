package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the unified type hierarchy using FilterBase.
 * 
 * These tests verify that:
 * 1. Filter implements FilterBase
 * 2. EntityFilter extends FilterBase
 * 3. CollectionFilter can work with both basic and model type filters
 * 4. Type compatibility works correctly with FilterBase
 * 5. instanceof checks work as expected
 */
@DisplayName("FilterBase Unified Hierarchy Tests")
class FilterBaseUnifiedHierarchyTest {

    /**
     * Test that Filter implements FilterBase.
     */
    @Test
    @DisplayName("Filter should implement FilterBase")
    void testFilterImplementsFilterBase() {
        // Arrange & Act
        StringFilter stringFilter = new StringFilter();
        
        // Assert
        assertTrue(stringFilter instanceof FilterBase, 
            "StringFilter should be an instance of FilterBase");
        assertTrue(stringFilter instanceof Filter, 
            "StringFilter should be an instance of Filter");
        
        // Verify type compatibility
        FilterBase<String> base = stringFilter;
        assertNotNull(base, "FilterBase reference should not be null");
    }

    /**
     * Test that EntityFilter extends FilterBase.
     */
    @Test
    @DisplayName("EntityFilter should extend FilterBase")
    void testEntityFilterExtendsFilterBase() {
        // Arrange & Act
        TestEntityFilter entityFilter = new TestEntityFilter();
        
        // Assert
        assertTrue(entityFilter instanceof FilterBase, 
            "EntityFilter implementation should be an instance of FilterBase");
        assertTrue(entityFilter instanceof EntityFilter, 
            "TestEntityFilter should be an instance of EntityFilter");
        
        // Verify type compatibility
        FilterBase<TestEntity> base = entityFilter;
        assertNotNull(base, "FilterBase reference should not be null");
    }

    /**
     * Test type compatibility with CollectionFilter for basic types.
     */
    @Test
    @DisplayName("CollectionFilter should accept basic type filters via FilterBase")
    void testCollectionFilterWithBasicTypes() {
        // Arrange
        CollectionFilter<String> collectionFilter = new CollectionFilter<>();
        StringFilter stringFilter = new StringFilter();
        stringFilter.setEquals("test");
        
        // Act
        collectionFilter.setCollectionAny(stringFilter);
        
        // Assert
        assertNotNull(collectionFilter.getCollectionAny(), 
            "CollectionAny should not be null");
        assertTrue(collectionFilter.getCollectionAny() instanceof StringFilter, 
            "CollectionAny should be a StringFilter");
        assertTrue(collectionFilter.getCollectionAny() instanceof FilterBase, 
            "CollectionAny should be a FilterBase");
    }

    /**
     * Test type compatibility with CollectionFilter for model types.
     */
    @Test
    @DisplayName("CollectionFilter should accept model type filters via FilterBase")
    void testCollectionFilterWithModelTypes() {
        // Arrange
        CollectionFilter<TestEntity> collectionFilter = new CollectionFilter<>();
        TestEntityFilter entityFilter = new TestEntityFilter();
        
        // Act
        collectionFilter.setCollectionAny(entityFilter);
        
        // Assert
        assertNotNull(collectionFilter.getCollectionAny(), 
            "CollectionAny should not be null");
        assertTrue(collectionFilter.getCollectionAny() instanceof TestEntityFilter, 
            "CollectionAny should be a TestEntityFilter");
        assertTrue(collectionFilter.getCollectionAny() instanceof FilterBase, 
            "CollectionAny should be a FilterBase");
        assertTrue(collectionFilter.getCollectionAny() instanceof EntityFilter, 
            "CollectionAny should be an EntityFilter");
    }

    /**
     * Test that FilterBase can hold both basic and model type filters.
     */
    @Test
    @DisplayName("FilterBase should be able to hold both basic and model type filters")
    void testFilterBaseHoldsBothTypes() {
        // Arrange & Act - Basic type
        FilterBase<String> basicFilter = new StringFilter();
        
        // Assert - Basic type
        assertNotNull(basicFilter, "Basic type FilterBase should not be null");
        assertTrue(basicFilter instanceof Filter, 
            "Basic type FilterBase should be a Filter");
        
        // Arrange & Act - Model type
        FilterBase<TestEntity> modelFilter = new TestEntityFilter();
        
        // Assert - Model type
        assertNotNull(modelFilter, "Model type FilterBase should not be null");
        assertTrue(modelFilter instanceof EntityFilter, 
            "Model type FilterBase should be an EntityFilter");
    }

    /**
     * Test instanceof checks for FilterBase, Filter, and EntityFilter.
     */
    @Test
    @DisplayName("instanceof checks should work correctly for all filter types")
    void testInstanceofChecks() {
        // Arrange
        StringFilter stringFilter = new StringFilter();
        TestEntityFilter entityFilter = new TestEntityFilter();
        
        // Act & Assert - StringFilter
        assertTrue(stringFilter instanceof FilterBase, 
            "StringFilter should be instanceof FilterBase");
        assertTrue(stringFilter instanceof Filter, 
            "StringFilter should be instanceof Filter");
        // EntityFilter is an interface, so StringFilter (which extends Filter) cannot be instanceof EntityFilter
        
        // Act & Assert - EntityFilter
        assertTrue(entityFilter instanceof FilterBase, 
            "EntityFilter should be instanceof FilterBase");
        // EntityFilter implementations don't extend Filter, they implement EntityFilter
        assertTrue(entityFilter instanceof EntityFilter, 
            "EntityFilter should be instanceof EntityFilter");
    }

    /**
     * Test type-safe casting from FilterBase to concrete types.
     */
    @Test
    @DisplayName("Type-safe casting from FilterBase should work correctly")
    void testTypeSafeCasting() {
        // Arrange
        CollectionFilter<String> stringCollection = new CollectionFilter<>();
        StringFilter stringFilter = new StringFilter();
        stringFilter.setEquals("test");
        stringCollection.setCollectionAny(stringFilter);
        
        CollectionFilter<TestEntity> entityCollection = new CollectionFilter<>();
        TestEntityFilter entityFilter = new TestEntityFilter();
        entityCollection.setCollectionAny(entityFilter);
        
        // Act & Assert - Basic type casting
        FilterBase<String> stringBase = stringCollection.getCollectionAny();
        assertNotNull(stringBase, "String FilterBase should not be null");
        if (stringBase instanceof StringFilter) {
            StringFilter cast = (StringFilter) stringBase;
            assertEquals("test", cast.getEquals(), 
                "Cast StringFilter should have correct value");
        } else {
            fail("FilterBase should be instanceof StringFilter");
        }
        
        // Act & Assert - Model type casting
        FilterBase<TestEntity> entityBase = entityCollection.getCollectionAny();
        assertNotNull(entityBase, "Entity FilterBase should not be null");
        if (entityBase instanceof TestEntityFilter) {
            TestEntityFilter cast = (TestEntityFilter) entityBase;
            assertNotNull(cast, "Cast TestEntityFilter should not be null");
        } else {
            fail("FilterBase should be instanceof TestEntityFilter");
        }
    }

    /**
     * Test that all collection operators (any, all, none) work with FilterBase.
     */
    @Test
    @DisplayName("All collection operators should work with FilterBase")
    void testAllCollectionOperators() {
        // Arrange
        CollectionFilter<String> collectionFilter = new CollectionFilter<>();
        StringFilter anyFilter = new StringFilter();
        anyFilter.setEquals("any");
        StringFilter allFilter = new StringFilter();
        allFilter.setEquals("all");
        StringFilter noneFilter = new StringFilter();
        noneFilter.setEquals("none");
        
        // Act
        collectionFilter.setCollectionAny(anyFilter);
        collectionFilter.setCollectionAll(allFilter);
        collectionFilter.setCollectionNone(noneFilter);
        
        // Assert
        assertNotNull(collectionFilter.getCollectionAny(), 
            "CollectionAny should not be null");
        assertNotNull(collectionFilter.getCollectionAll(), 
            "CollectionAll should not be null");
        assertNotNull(collectionFilter.getCollectionNone(), 
            "CollectionNone should not be null");
        
        assertTrue(collectionFilter.getCollectionAny() instanceof FilterBase, 
            "CollectionAny should be FilterBase");
        assertTrue(collectionFilter.getCollectionAll() instanceof FilterBase, 
            "CollectionAll should be FilterBase");
        assertTrue(collectionFilter.getCollectionNone() instanceof FilterBase, 
            "CollectionNone should be FilterBase");
    }

    // Test helper classes
    
    /**
     * Test entity class for EntityFilter testing.
     */
    private static class TestEntity {
        private String id;
        private String name;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * Test EntityFilter implementation.
     */
    private static class TestEntityFilter implements EntityFilter<TestEntity> {
        private StringFilter id;
        private StringFilter name;
        
        public StringFilter getId() { return id; }
        public void setId(StringFilter id) { this.id = id; }
        public StringFilter getName() { return name; }
        public void setName(StringFilter name) { this.name = name; }
        
        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }
    }
}
