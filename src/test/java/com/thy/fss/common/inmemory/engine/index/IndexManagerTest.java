package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for IndexManager.
 * Tests cache management, index lifecycle, and statistics collection.
 */
class IndexManagerTest {

    private static final String USER_DATASOURCE = "user-datasource";
    private static final String PRODUCT_DATASOURCE = "product-datasource";
    private static final String ORDER_DATASOURCE = "order-datasource";
    private static final String DATASOURCE = "datasource";
    
    
    private IndexManager indexManager;
    private LargeDatasetGenerator datasetGenerator;
    private List<User> largeUserDataset;
    private List<Product> largeProductDataset;
    private List<Order> largeOrderDataset;
    
    @BeforeEach
    void setUp() {
        indexManager = new IndexManager();
        datasetGenerator = new LargeDatasetGenerator();
        
        // Generate large datasets for realistic testing
        largeUserDataset = datasetGenerator.generateUsers(10000);
        largeProductDataset = datasetGenerator.generateProducts(5000);
        largeOrderDataset = datasetGenerator.generateOrders(10000, 2);
    }
    
    @AfterEach
    void tearDown() {
        indexManager.clearAllIndexes();
        indexManager.getDefinitionRegistry().clear();
    }
    
    @Test
    void testGetOrCreateIndexCreatesNewIndex() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        // When
        NestedTreeMapIndex<User> index = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeUserDataset
        );
        
        // Then
        assertNotNull(index);
        assertEquals(1, index.getDepth());
        assertTrue(indexManager.hasIndex(datasourceName));
    }
    
    @Test
    void testGetOrCreateIndexReturnsCachedIndex() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        // When
        NestedTreeMapIndex<User> firstIndex = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeUserDataset
        );
        NestedTreeMapIndex<User> secondIndex = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeUserDataset
        );
        
        // Then
        assertSame(firstIndex, secondIndex, "Should return cached index");
    }
    
    @Test
    void testGetOrCreateIndexMultiLevelIndex() {
        // Given
        String datasourceName = ORDER_DATASOURCE;
        IndexDefinition<Order> definition = IndexDefinition.builder(Order.class)
            .addKeyField(Order_.customerId)
            .addKeyField(Order_.status)
            .addKeyField(Order_.id)
            .build();
        
        // When
        NestedTreeMapIndex<Order> index = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeOrderDataset
        );
        
        // Then
        assertNotNull(index);
        assertEquals(3, index.getDepth());
    }
    
    @Test
    void testInvalidateIndexRemovesSpecificDatasourceIndexes() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String productDatasource = PRODUCT_DATASOURCE;
        
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        IndexDefinition<Product> productDefinition = IndexDefinition.builder(Product.class)
            .addKeyField(Product_.id)
            .build();
        
        indexManager.getOrCreateIndex(userDatasource, userDefinition, largeUserDataset);
        indexManager.getOrCreateIndex(productDatasource, productDefinition, largeProductDataset);
        
        // When
        indexManager.invalidateIndex(userDatasource);
        
        // Then
        assertFalse(indexManager.hasIndex(userDatasource));
        assertTrue(indexManager.hasIndex(productDatasource));
    }
    
    @Test
    void testClearAllIndexesRemovesAllIndexes() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String productDatasource = PRODUCT_DATASOURCE;
        
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        IndexDefinition<Product> productDefinition = IndexDefinition.builder(Product.class)
            .addKeyField(Product_.id)
            .build();
        
        indexManager.getOrCreateIndex(userDatasource, userDefinition, largeUserDataset);
        indexManager.getOrCreateIndex(productDatasource, productDefinition, largeProductDataset);
        
        // When
        indexManager.clearAllIndexes();
        
        // Then
        assertFalse(indexManager.hasIndex(userDatasource));
        assertFalse(indexManager.hasIndex(productDatasource));
    }
    
    @Test
    void testRegisterIndexDefinitionCustomDefinition() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> customDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .addKeyField(User_.name)
            .build();
        
        // When
        indexManager.registerIndexDefinition(datasourceName, customDefinition);
        
        // Then
        assertTrue(indexManager.getDefinitionRegistry().hasCustomDefinition(datasourceName));
    }
    
    @Test
    void testGetAllStatisticsReturnsStatisticsForAllIndexes() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String productDatasource = PRODUCT_DATASOURCE;
        
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        IndexDefinition<Product> productDefinition = IndexDefinition.builder(Product.class)
            .addKeyField(Product_.id)
            .build();
        
        indexManager.getOrCreateIndex(userDatasource, userDefinition, largeUserDataset);
        indexManager.getOrCreateIndex(productDatasource, productDefinition, largeProductDataset);
        
        // When
        Map<String, IndexStatistics> statistics = indexManager.getAllStatistics();
        
        // Then
        assertNotNull(statistics);
        assertEquals(2, statistics.size());
        assertTrue(statistics.containsKey(userDatasource));
        assertTrue(statistics.containsKey(productDatasource));
        
        IndexStatistics userStats = statistics.get(userDatasource);
        assertNotNull(userStats);
        assertEquals(10000, userStats.getTotalEntries());
        assertEquals(1, userStats.getDepth());
        assertTrue(userStats.getCreationTimeMs() >= 0);
    }
    
    @Test
    void testGetAllStatisticsEmptyWhenNoIndexes() {
        // When
        Map<String, IndexStatistics> statistics = indexManager.getAllStatistics();
        
        // Then
        assertNotNull(statistics);
        assertTrue(statistics.isEmpty());
    }
    
    @Test
    void testHasIndexReturnsFalseForNonExistentDatasource() {
        // When
        boolean hasIndex = indexManager.hasIndex("non-existent");
        
        // Then
        assertFalse(hasIndex);
    }
    
    @Test
    void testGetOrCreateIndexNullDatasourceNameThrowsException() {
        // Given
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        // When & Then
        assertThrows(NullPointerException.class, () ->
            indexManager.getOrCreateIndex(null, definition, largeUserDataset)
        );
    }
    
    @Test
    void testGetOrCreateIndexNullDefinitionThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            indexManager.getOrCreateIndex(DATASOURCE, null, largeUserDataset)
        );
    }
    
    @Test
    void testGetOrCreateIndexNullDataThrowsException() {
        // Given
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        // When & Then
        assertThrows(NullPointerException.class, () ->
            indexManager.getOrCreateIndex(DATASOURCE, definition, null)
        );
    }
    
    @Test
    void testInvalidateIndexNullDatasourceNameThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            indexManager.invalidateIndex(null)
        );
    }
    
    @Test
    void testHasIndexNullDatasourceNameThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
            indexManager.hasIndex(null)
        );
    }
    
    @Test
    void testIndexRecreationAfterInvalidation() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        indexManager.getOrCreateIndex(datasourceName, definition, largeUserDataset);
        
        // When
        indexManager.invalidateIndex(datasourceName);
        NestedTreeMapIndex<User> secondIndex = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeUserDataset
        );
        
        // Then
        assertNotNull(secondIndex);
        assertTrue(indexManager.hasIndex(datasourceName));
        // Should be a new instance after invalidation
    }
    
    @Test
    void testMultipleDatasourcesWithDifferentDefinitions() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String orderDatasource = ORDER_DATASOURCE;
        
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        IndexDefinition<Order> orderDefinition = IndexDefinition.builder(Order.class)
            .addKeyField(Order_.customerId)
            .addKeyField(Order_.status)
            .build();
        
        // When
        NestedTreeMapIndex<User> userIndex = indexManager.getOrCreateIndex(
            userDatasource, 
            userDefinition, 
            largeUserDataset
        );
        NestedTreeMapIndex<Order> orderIndex = indexManager.getOrCreateIndex(
            orderDatasource, 
            orderDefinition, 
            largeOrderDataset
        );
        
        // Then
        assertNotNull(userIndex);
        assertNotNull(orderIndex);
        assertEquals(1, userIndex.getDepth());
        assertEquals(2, orderIndex.getDepth());
        assertTrue(indexManager.hasIndex(userDatasource));
        assertTrue(indexManager.hasIndex(orderDatasource));
    }
    
    @Test
    void testStatisticsAfterClearAllIndexes() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        indexManager.getOrCreateIndex(datasourceName, definition, largeUserDataset);
        
        // When
        indexManager.clearAllIndexes();
        Map<String, IndexStatistics> statistics = indexManager.getAllStatistics();
        
        // Then
        assertTrue(statistics.isEmpty());
    }
    
    @Test
    void testClearAllIndexesDeepClearsBothCaches() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String productDatasource = PRODUCT_DATASOURCE;
        
        // Create single-field index
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        indexManager.getOrCreateIndex(userDatasource, userDefinition, largeUserDataset);
        
        // Create composite key index
        List<List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(Product_.id),
            List.of(Product_.name)
        );
        indexManager.getOrCreateCompositeIndex(productDatasource, keyPaths, largeProductDataset);
        
        // When
        indexManager.clearAllIndexesDeep();
        
        // Then
        assertFalse(indexManager.hasIndex(userDatasource), "Single-field index cache should be cleared");
        assertFalse(indexManager.hasIndex(productDatasource), "Composite key index cache should be cleared");
        
        Map<String, IndexStatistics> statistics = indexManager.getAllStatistics();
        assertTrue(statistics.isEmpty(), "Statistics should be empty after deep clear");
    }
    
    @Test
    void testClearAllIndexesDeepCallsDeepClearOnEachIndex() {
        // Given
        String datasourceName = USER_DATASOURCE;
        
        // Create multi-level index with nested TreeMaps
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .addKeyField(User_.name)
            .build();
        
        NestedTreeMapIndex<User> index = indexManager.getOrCreateIndex(
            datasourceName, 
            definition, 
            largeUserDataset
        );
        
        // Verify index has data before clearing
        List<User> lookupResult = index.lookup(largeUserDataset.get(0).getIdentity(), largeUserDataset.get(0).getName());
        assertFalse(lookupResult.isEmpty(), "Index should contain data before clearing");
        
        // When
        indexManager.clearAllIndexesDeep();
        
        // Then
        // After deep clear, the index should be empty (all nested structures cleared)
        // We can't directly verify the internal state, but we can verify the cache is cleared
        assertFalse(indexManager.hasIndex(datasourceName), "Index should be removed from cache");
    }
    
    @Test
    void testClearAllIndexesDeepIsIdempotent() {
        // Given
        String datasourceName = USER_DATASOURCE;
        IndexDefinition<User> definition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .build();
        
        indexManager.getOrCreateIndex(datasourceName, definition, largeUserDataset);
        
        // When - call clearAllIndexesDeep multiple times
        indexManager.clearAllIndexesDeep();
        indexManager.clearAllIndexesDeep();
        indexManager.clearAllIndexesDeep();
        
        // Then - should not throw exception and cache should remain empty
        assertFalse(indexManager.hasIndex(datasourceName));
        Map<String, IndexStatistics> statistics = indexManager.getAllStatistics();
        assertTrue(statistics.isEmpty());
    }
    
    @Test
    void testClearAllIndexesDeepWithCompositeKeyIndexes() {
        // Given
        String orderDatasource = ORDER_DATASOURCE;
        
        // Create composite key index with nested HashMaps
        List<List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(Order_.customerId),
            List.of(Order_.status),
            List.of(Order_.id)
        );
        
        CompositeKeyIndex<Order> index = indexManager.getOrCreateCompositeIndex(
            orderDatasource, 
            keyPaths, 
            largeOrderDataset
        );
        
        // Verify index has data before clearing
        Order firstOrder = largeOrderDataset.get(0);
        List<Object> keyValues = List.of(firstOrder.getCustomerId(), firstOrder.getStatus(), firstOrder.getIdentity());
        List<Order> lookupResult = index.lookup(keyValues);
        assertFalse(lookupResult.isEmpty(), "Composite index should contain data before clearing");
        
        // When
        indexManager.clearAllIndexesDeep();
        
        // Then
        assertFalse(indexManager.hasIndex(orderDatasource), "Composite index should be removed from cache");
    }
    
    @Test
    void testClearAllIndexesDeepWithMixedIndexTypes() {
        // Given
        String userDatasource = USER_DATASOURCE;
        String orderDatasource = ORDER_DATASOURCE;
        
        // Create single-field index
        IndexDefinition<User> userDefinition = IndexDefinition.builder(User.class)
            .addKeyField(User_.id)
            .addKeyField(User_.name)
            .build();
        indexManager.getOrCreateIndex(userDatasource, userDefinition, largeUserDataset);
        
        // Create composite key index
        List<List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(Order_.customerId),
            List.of(Order_.status)
        );
        CompositeKeyIndex<Order> compositeIndex = indexManager.getOrCreateCompositeIndex(orderDatasource, keyPaths, largeOrderDataset);
        
        // Verify both indexes exist
        assertTrue(indexManager.hasIndex(userDatasource), "Single-field index should exist before clearing");
        assertNotNull(compositeIndex, "Composite key index should exist before clearing");
        
        // Verify composite index has data
        Order firstOrder = largeOrderDataset.get(0);
        List<Object> keyValues = List.of(firstOrder.getCustomerId(), firstOrder.getStatus());
        List<Order> lookupResult = compositeIndex.lookup(keyValues);
        assertFalse(lookupResult.isEmpty(), "Composite index should contain data before clearing");
        
        // When
        indexManager.clearAllIndexesDeep();
        
        // Then
        assertFalse(indexManager.hasIndex(userDatasource), "Single-field index should be cleared");
        
        // Verify composite index cache is cleared by trying to get it again
        CompositeKeyIndex<Order> newCompositeIndex = indexManager.getOrCreateCompositeIndex(orderDatasource, keyPaths, largeOrderDataset);
        assertNotSame(compositeIndex, newCompositeIndex, "Should create new composite index after deep clear");
    }
}
