package com.thy.fss.common.inmemory.engine.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DataVersion class.
 * Tests version increment, data storage/retrieval, immutability,
 * commonAggregationResults storage, and groupedData storage.
 * <p>
 * Requirements tested:
 * - 1.4: DataVersion structure
 * - 6.1: Version management
 * - 13.1: Versioned data container
 */
@DisplayName("DataVersion Tests")
class DataVersionTest {
    
    private static final String STORE1 = "store1";
    private static final String STORE2 = "store2";
    private static final String DASHBOARD1 = "dashboard1";
    private static final String USERS = "users";
    private static final String ORDERS = "orders";
    private static final String NONEXISTENT = "nonexistent";
    private static final String GROUPING1 = "grouping1";
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private static final String VALUE3 = "value3";
    private static final String VALUE4 = "value4";
    private static final String COUNT_USERS = "count:users";
    private static final String SUM_TOTAL = "sum:total";
    private static final String COUNT_ITEMS = "count:items";
    private static final String ENTITY1 = "entity1";
    private static final String ENTITY2 = "entity2";
    private static final String USER1 = "user1";
    private static final String ORDER1 = "order1";
    private static final String DATA1 = "data1";
    private static final String DATA2 = "data2";
    private static final String GROUPING2 = "grouping2";
    private static final String A = "a";
    private static final String B = "b";
    private static final String C = "c";
    private static final String D = "d";
    private static final String SUM_ORDERS_AMOUNT = "sum:orders:amount";
    private static final String AVG_PRODUCTS_PRICE = "avg:products:price";
    private static final String MAX_ORDERS_TOTAL = "max:orders:total";
    private static final String PRODUCTS = "products";
    private static final String ANY = "any";
    private static final String OLD1 = "old1";
    private static final String NEW1 = "new1";
    
    
    private DataVersion dataVersion;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        timestamp = LocalDateTime.now();
        dataVersion = new DataVersion(1L, timestamp);
    }

    // ========== Version Increment Tests ==========

    @Test
    @DisplayName("DataVersion should be created with correct version number")
    void testVersionNumber() {
        assertEquals(1L, dataVersion.getVersion(), "Version should be 1");

        DataVersion version2 = new DataVersion(2L, timestamp);
        assertEquals(2L, version2.getVersion(), "Version should be 2");

        DataVersion version100 = new DataVersion(100L, timestamp);
        assertEquals(100L, version100.getVersion(), "Version should be 100");
    }

    @Test
    @DisplayName("DataVersion should store timestamp correctly")
    void testTimestamp() {
        assertEquals(timestamp, dataVersion.getTimestamp(), "Timestamp should match");

        LocalDateTime newTimestamp = LocalDateTime.now().plusHours(1);
        DataVersion newVersion = new DataVersion(2L, newTimestamp);
        assertEquals(newTimestamp, newVersion.getTimestamp(), "New timestamp should match");
    }

    @Test
    @DisplayName("DataVersion toString should include version and timestamp")
    void testToString() {
        String str = dataVersion.toString();
        assertTrue(str.contains("version=1"), "toString should contain version");
        assertTrue(str.contains("timestamp="), "toString should contain timestamp");
        assertTrue(str.contains("consumers=0"), "toString should contain consumer count");
        assertTrue(str.contains("datasources=0"), "toString should contain datasource count");
    }

    // ========== Data Storage and Retrieval Tests ==========

    @Test
    @DisplayName("Populated entities should be stored and retrieved correctly")
    void testPopulatedEntitiesStorage() {
        // Create test data
        List<String> storeData = Arrays.asList(ENTITY1, ENTITY2, "entity3");
        List<Integer> dashboardData = Arrays.asList(1, 2, 3);

        // Store data
        dataVersion.setPopulatedEntities(STORE1, storeData);
        dataVersion.setPopulatedEntities(DASHBOARD1, dashboardData);

        // Retrieve and verify
        List<String> retrievedStoreData = dataVersion.getPopulatedEntities(STORE1);
        assertEquals(storeData, retrievedStoreData, "Store data should match");

        List<Integer> retrievedDashboardData = dataVersion.getPopulatedEntities(DASHBOARD1);
        assertEquals(dashboardData, retrievedDashboardData, "Dashboard data should match");
    }

    @Test
    @DisplayName("Get populated entities should return null for non-existent consumer")
    void testGetPopulatedEntitiesNonExistent() {
        List<String> data = dataVersion.getPopulatedEntities(NONEXISTENT);
        assertNull(data, "Should return null for non-existent consumer");
    }

    @Test
    @DisplayName("DataByDataSource should be stored and retrieved correctly")
    void testDataByDataSourceStorage() {
        // Create test data
        List<String> userData = Arrays.asList(USER1, "user2");
        List<String> orderData = Arrays.asList(ORDER1, "order2", "order3");

        // Store data
        dataVersion.setDataByDataSource(USERS, userData);
        dataVersion.setDataByDataSource(ORDERS, orderData);

        // Retrieve and verify
        List<String> retrievedUserData = dataVersion.getDataByDataSource(USERS);
        assertEquals(userData, retrievedUserData, "User data should match");

        List<String> retrievedOrderData = dataVersion.getDataByDataSource(ORDERS);
        assertEquals(orderData, retrievedOrderData, "Order data should match");
    }

    @Test
    @DisplayName("Get data by datasource should return null for non-existent datasource")
    void testGetDataByDataSourceNonExistent() {
        List<String> data = dataVersion.getDataByDataSource(NONEXISTENT);
        assertNull(data, "Should return null for non-existent datasource");
    }

    @Test
    @DisplayName("Grouped data should be stored and retrieved correctly")
    void testGroupedDataStorage() {
        // Create grouped data
        Map<Object, List<?>> groupedData1 = new HashMap<>();
        groupedData1.put(KEY1, Arrays.asList(VALUE1, VALUE2));
        groupedData1.put(KEY2, Arrays.asList(VALUE3, VALUE4));

        Map<Object, List<?>> groupedData2 = new HashMap<>();
        groupedData2.put(1, Arrays.asList(A, B));
        groupedData2.put(2, Arrays.asList(C, D));

        // Store data
        dataVersion.setGroupedData(GROUPING1, groupedData1);
        dataVersion.setGroupedData(GROUPING2, groupedData2);

        // Retrieve and verify
        Map<Object, List<String>> retrieved1 = dataVersion.getGroupedData(GROUPING1);
        assertNotNull(retrieved1, "Grouped data should not be null");
        assertEquals(2, retrieved1.size(), "Should have 2 groups");
        assertEquals(Arrays.asList(VALUE1, VALUE2), retrieved1.get(KEY1));
        assertEquals(Arrays.asList(VALUE3, VALUE4), retrieved1.get(KEY2));

        Map<Object, List<String>> retrieved2 = dataVersion.getGroupedData(GROUPING2);
        assertNotNull(retrieved2, "Grouped data should not be null");
        assertEquals(2, retrieved2.size(), "Should have 2 groups");
        assertEquals(Arrays.asList(A, B), retrieved2.get(1));
        assertEquals(Arrays.asList(C, D), retrieved2.get(2));
    }

    @Test
    @DisplayName("Get grouped data should return null for non-existent grouping key")
    void testGetGroupedDataNonExistent() {
        Map<Object, List<String>> data = dataVersion.getGroupedData(NONEXISTENT);
        assertNull(data, "Should return null for non-existent grouping key");
    }

    // ========== Common Aggregation Results Tests ==========

    @Test
    @DisplayName("Common aggregation results should be stored and retrieved correctly")
    void testCommonAggregationResultsStorage() {
        // Store various types of aggregation results
        dataVersion.setCommonAggregationResult(SUM_ORDERS_AMOUNT, 1000.0);
        dataVersion.setCommonAggregationResult(COUNT_USERS, 50);
        dataVersion.setCommonAggregationResult(AVG_PRODUCTS_PRICE, 25.5);
        dataVersion.setCommonAggregationResult(MAX_ORDERS_TOTAL, 500.0);

        // Retrieve and verify
        Double sumResult = dataVersion.getCommonAggregationResult(SUM_ORDERS_AMOUNT);
        assertEquals(1000.0, sumResult, "Sum result should match");

        Integer countResult = dataVersion.getCommonAggregationResult(COUNT_USERS);
        assertEquals(50, countResult, "Count result should match");

        Double avgResult = dataVersion.getCommonAggregationResult(AVG_PRODUCTS_PRICE);
        assertEquals(25.5, avgResult, "Average result should match");

        Double maxResult = dataVersion.getCommonAggregationResult(MAX_ORDERS_TOTAL);
        assertEquals(500.0, maxResult, "Max result should match");
    }

    @Test
    @DisplayName("Get common aggregation result should return null for non-existent key")
    void testGetCommonAggregationResultNonExistent() {
        Object result = dataVersion.getCommonAggregationResult(NONEXISTENT);
        assertNull(result, "Should return null for non-existent aggregation key");
    }

    @Test
    @DisplayName("Common aggregation results should support complex keys")
    void testCommonAggregationResultsComplexKeys() {
        // Test with complex storage keys (as would be generated by CommonAggregationKey.toStorageKey())
        String complexKey1 = "datasource:orders|spec:status=completed|field:amount|agg:SUM";
        String complexKey2 = "datasource:users|spec:active=true|field:id|agg:COUNT";

        dataVersion.setCommonAggregationResult(complexKey1, 5000.0);
        dataVersion.setCommonAggregationResult(complexKey2, 100);

        Double result1 = dataVersion.getCommonAggregationResult(complexKey1);
        assertEquals(5000.0, result1, "Complex key result 1 should match");

        Integer result2 = dataVersion.getCommonAggregationResult(complexKey2);
        assertEquals(100, result2, "Complex key result 2 should match");
    }

    // ========== Immutability Tests ==========

    @Test
    @DisplayName("Make immutable should return same instance")
    void testMakeImmutable() {
        DataVersion immutable = dataVersion.makeImmutable();
        assertSame(dataVersion, immutable, "makeImmutable should return same instance");
    }

    @Test
    @DisplayName("Get all populated entities should return unmodifiable map")
    void testGetAllPopulatedEntitiesUnmodifiable() {
        dataVersion.setPopulatedEntities(STORE1, List.of(DATA1));

        Map<String, List<?>> allEntities = dataVersion.getAllPopulatedEntities();
        assertNotNull(allEntities, "Map should not be null");
        assertEquals(1, allEntities.size(), "Map should have 1 entry");

        // Given
        List<String> newData = List.of(DATA2);

        // When & Then - Verify it's unmodifiable
        assertThrows(UnsupportedOperationException.class, () ->
                        allEntities.put(STORE2, newData),
                "Should not be able to modify returned map");
    }

    @Test
    @DisplayName("Get populated entity consumer IDs should return unmodifiable set")
    void testGetPopulatedEntityConsumerIdsUnmodifiable() {
        dataVersion.setPopulatedEntities(STORE1, List.of(DATA1));
        dataVersion.setPopulatedEntities(STORE2, List.of(DATA2));

        Set<String> consumerIds = dataVersion.getPopulatedEntityConsumerIds();
        assertNotNull(consumerIds, "Set should not be null");
        assertEquals(2, consumerIds.size(), "Set should have 2 entries");
        assertTrue(consumerIds.contains(STORE1), "Should contain store1");
        assertTrue(consumerIds.contains(STORE2), "Should contain store2");

        // Verify it's unmodifiable
        assertThrows(UnsupportedOperationException.class,
            () -> consumerIds.add("store3"),
            "Should not be able to modify returned set");
    }

    @Test
    @DisplayName("Get datasource names should return unmodifiable set")
    void testGetDataSourceNamesUnmodifiable() {
        dataVersion.setDataByDataSource(USERS, List.of(USER1));
        dataVersion.setDataByDataSource(ORDERS, List.of(ORDER1));

        Set<String> datasourceNames = dataVersion.getDataSourceNames();
        assertNotNull(datasourceNames, "Set should not be null");
        assertEquals(2, datasourceNames.size(), "Set should have 2 entries");
        assertTrue(datasourceNames.contains(USERS), "Should contain users");
        assertTrue(datasourceNames.contains(ORDERS), "Should contain orders");

        // Verify it's unmodifiable
        assertThrows(UnsupportedOperationException.class,
            () -> datasourceNames.add(PRODUCTS),
            "Should not be able to modify returned set");
    }

    // ========== Convenience Methods Tests ==========

    @Test
    @DisplayName("Store data convenience methods should work correctly")
    void testStoreDataConvenienceMethods() {
        List<String> storeData = Arrays.asList(ENTITY1, ENTITY2);

        dataVersion.setStoreData(STORE1, storeData);

        List<String> retrieved = dataVersion.getStoreData(STORE1);
        assertEquals(storeData, retrieved, "Store data should match");
    }

    @Test
    @DisplayName("Dashboard data convenience methods should work correctly")
    void testDashboardDataConvenienceMethods() {
        List<Integer> dashboardData = Arrays.asList(1, 2, 3);

        dataVersion.setDashboardData(DASHBOARD1, dashboardData);

        List<Integer> retrieved = dataVersion.getDashboardData(DASHBOARD1);
        assertEquals(dashboardData, retrieved, "Dashboard data should match");
    }

    @Test
    @DisplayName("Has populated entities should return correct boolean")
    void testHasPopulatedEntities() {
        assertFalse(dataVersion.hasPopulatedEntities(STORE1),
                "Should return false for non-existent consumer");

        dataVersion.setPopulatedEntities(STORE1, List.of("data"));

        assertTrue(dataVersion.hasPopulatedEntities(STORE1),
                "Should return true for existing consumer");
        assertFalse(dataVersion.hasPopulatedEntities(STORE2),
                "Should return false for non-existent consumer");
    }

    @Test
    @DisplayName("Has datasource should return correct boolean")
    void testHasDataSource() {
        assertFalse(dataVersion.hasDataSource(USERS),
                "Should return false for non-existent datasource");

        dataVersion.setDataByDataSource(USERS, List.of(USER1));

        assertTrue(dataVersion.hasDataSource(USERS),
                "Should return true for existing datasource");
        assertFalse(dataVersion.hasDataSource(ORDERS),
                "Should return false for non-existent datasource");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("DataVersion should handle multiple data types simultaneously")
    void testMultipleDataTypes() {
        // Populate all data types
        dataVersion.setPopulatedEntities(STORE1, Arrays.asList("s1", "s2"));
        dataVersion.setPopulatedEntities(DASHBOARD1, Arrays.asList(1, 2));

        dataVersion.setDataByDataSource(USERS, Arrays.asList("u1", "u2"));
        dataVersion.setDataByDataSource(ORDERS, Arrays.asList("o1", "o2"));

        Map<Object, List<?>> grouped = new HashMap<>();
        grouped.put(KEY1, Arrays.asList("v1", "v2"));
        dataVersion.setGroupedData(GROUPING1, grouped);

        dataVersion.setCommonAggregationResult(SUM_TOTAL, 1000.0);
        dataVersion.setCommonAggregationResult(COUNT_ITEMS, 50);

        // Verify all data is accessible
        assertEquals(2, dataVersion.getPopulatedEntities(STORE1).size());
        assertEquals(2, dataVersion.getPopulatedEntities(DASHBOARD1).size());
        assertEquals(2, dataVersion.getDataByDataSource(USERS).size());
        assertEquals(2, dataVersion.getDataByDataSource(ORDERS).size());
        assertEquals(1, dataVersion.getGroupedData(GROUPING1).size());
        Double sumResult = dataVersion.getCommonAggregationResult(SUM_TOTAL);
        assertEquals(1000.0, sumResult, 0.001);
        Integer countResult = dataVersion.getCommonAggregationResult(COUNT_ITEMS);
        assertEquals(50, countResult.intValue());

        // Verify counts
        assertEquals(2, dataVersion.getPopulatedEntityConsumerIds().size());
        assertEquals(2, dataVersion.getDataSourceNames().size());
    }

    @Test
    @DisplayName("DataVersion should handle empty state correctly")
    void testEmptyState() {
        // Verify empty state
        assertTrue(dataVersion.getPopulatedEntityConsumerIds().isEmpty(),
                "Consumer IDs should be empty");
        assertTrue(dataVersion.getDataSourceNames().isEmpty(),
                "Datasource names should be empty");
        assertTrue(dataVersion.getAllPopulatedEntities().isEmpty(),
                "All populated entities should be empty");

        assertNull(dataVersion.getPopulatedEntities(ANY));
        assertNull(dataVersion.getDataByDataSource(ANY));
        assertNull(dataVersion.getGroupedData(ANY));
        assertNull(dataVersion.getCommonAggregationResult(ANY));
    }

    @Test
    @DisplayName("DataVersion should handle overwriting data correctly")
    void testOverwriteData() {
        // Set initial data
        dataVersion.setPopulatedEntities(STORE1, Arrays.asList(OLD1, "old2"));
        assertEquals(2, dataVersion.getPopulatedEntities(STORE1).size());

        // Overwrite with new data
        dataVersion.setPopulatedEntities(STORE1, Arrays.asList(NEW1, "new2", "new3"));
        List<String> retrieved = dataVersion.getPopulatedEntities(STORE1);
        assertEquals(3, retrieved.size(), "Should have new data size");
        assertTrue(retrieved.contains(NEW1), "Should contain new data");
        assertFalse(retrieved.contains(OLD1), "Should not contain old data");
    }

    // ========== Clear Intermediate Data Tests ==========

    @Test
    @DisplayName("clearIntermediateData should clear all three intermediate maps")
    void testClearIntermediateDataClearsAllMaps() {
        // Populate all intermediate data structures
        dataVersion.setDataByDataSource(USERS, Arrays.asList(USER1, "user2"));
        dataVersion.setDataByDataSource(ORDERS, Arrays.asList(ORDER1, "order2"));

        Map<Object, List<?>> grouped = new HashMap<>();
        grouped.put(KEY1, Arrays.asList(VALUE1, VALUE2));
        dataVersion.setGroupedData(GROUPING1, grouped);

        dataVersion.setCommonAggregationResult(SUM_TOTAL, 1000.0);
        dataVersion.setCommonAggregationResult(COUNT_ITEMS, 50);

        // Verify data is present
        assertNotNull(dataVersion.getDataByDataSource(USERS), "Users data should exist");
        assertNotNull(dataVersion.getDataByDataSource(ORDERS), "Orders data should exist");
        assertNotNull(dataVersion.getGroupedData(GROUPING1), "Grouped data should exist");
        assertNotNull(dataVersion.getCommonAggregationResult(SUM_TOTAL), "Sum result should exist");
        assertNotNull(dataVersion.getCommonAggregationResult(COUNT_ITEMS), "Count result should exist");

        // Clear intermediate data
        dataVersion.clearIntermediateData();

        // Verify all intermediate data is cleared
        assertNull(dataVersion.getDataByDataSource(USERS), "Users data should be cleared");
        assertNull(dataVersion.getDataByDataSource(ORDERS), "Orders data should be cleared");
        assertNull(dataVersion.getGroupedData(GROUPING1), "Grouped data should be cleared");
        assertNull(dataVersion.getCommonAggregationResult(SUM_TOTAL), "Sum result should be cleared");
        assertNull(dataVersion.getCommonAggregationResult(COUNT_ITEMS), "Count result should be cleared");

        // Verify datasource names set is empty
        assertTrue(dataVersion.getDataSourceNames().isEmpty(), "Datasource names should be empty");
        assertTrue(dataVersion.getAllGroupedDataKeys().isEmpty(), "Grouped data keys should be empty");
    }

    @Test
    @DisplayName("clearIntermediateData should NOT clear populatedEntities")
    void testClearIntermediateDataPreservesPopulatedEntities() {
        // Populate both intermediate and final data
        dataVersion.setPopulatedEntities(STORE1, Arrays.asList(ENTITY1, ENTITY2));
        dataVersion.setPopulatedEntities(DASHBOARD1, Arrays.asList(1, 2, 3));

        dataVersion.setDataByDataSource(USERS, Arrays.asList(USER1, "user2"));

        Map<Object, List<?>> grouped = new HashMap<>();
        grouped.put(KEY1, Arrays.asList(VALUE1, VALUE2));
        dataVersion.setGroupedData(GROUPING1, grouped);

        dataVersion.setCommonAggregationResult(SUM_TOTAL, 1000.0);

        // Verify all data is present
        assertEquals(2, dataVersion.getPopulatedEntities(STORE1).size(), "Store data should exist");
        assertEquals(3, dataVersion.getPopulatedEntities(DASHBOARD1).size(), "Dashboard data should exist");
        assertNotNull(dataVersion.getDataByDataSource(USERS), "Users data should exist");

        // Clear intermediate data
        dataVersion.clearIntermediateData();

        // Verify populatedEntities is NOT cleared
        List<String> storeData = dataVersion.getPopulatedEntities(STORE1);
        assertNotNull(storeData, "Store data should still exist");
        assertEquals(2, storeData.size(), "Store data size should be unchanged");
        assertEquals(ENTITY1, storeData.get(0), "Store data content should be unchanged");

        List<Integer> dashboardData = dataVersion.getPopulatedEntities(DASHBOARD1);
        assertNotNull(dashboardData, "Dashboard data should still exist");
        assertEquals(3, dashboardData.size(), "Dashboard data size should be unchanged");

        // Verify intermediate data is cleared
        assertNull(dataVersion.getDataByDataSource(USERS), "Users data should be cleared");
        assertNull(dataVersion.getGroupedData(GROUPING1), "Grouped data should be cleared");
        assertNull(dataVersion.getCommonAggregationResult(SUM_TOTAL), "Sum result should be cleared");
    }

    @Test
    @DisplayName("clearIntermediateData should be idempotent - safe to call multiple times")
    void testClearIntermediateDataIdempotency() {
        // Populate intermediate data
        dataVersion.setDataByDataSource(USERS, Arrays.asList(USER1, "user2"));

        Map<Object, List<?>> grouped = new HashMap<>();
        grouped.put(KEY1, Arrays.asList(VALUE1, VALUE2));
        dataVersion.setGroupedData(GROUPING1, grouped);

        dataVersion.setCommonAggregationResult(SUM_TOTAL, 1000.0);

        // Populate final data
        dataVersion.setPopulatedEntities(STORE1, Arrays.asList(ENTITY1, ENTITY2));

        // Call clearIntermediateData multiple times
        dataVersion.clearIntermediateData();
        dataVersion.clearIntermediateData();
        dataVersion.clearIntermediateData();

        // Verify intermediate data is still cleared (no exceptions thrown)
        assertNull(dataVersion.getDataByDataSource(USERS), "Users data should be cleared");
        assertNull(dataVersion.getGroupedData(GROUPING1), "Grouped data should be cleared");
        assertNull(dataVersion.getCommonAggregationResult(SUM_TOTAL), "Sum result should be cleared");

        // Verify populatedEntities is still intact
        List<String> storeData = dataVersion.getPopulatedEntities(STORE1);
        assertNotNull(storeData, "Store data should still exist");
        assertEquals(2, storeData.size(), "Store data should be unchanged");
    }

    @Test
    @DisplayName("clearIntermediateData should work correctly on empty DataVersion")
    void testClearIntermediateDataOnEmptyVersion() {
        // Create a new empty DataVersion
        DataVersion emptyVersion = new DataVersion(2L, LocalDateTime.now());

        // Call clearIntermediateData on empty version (should not throw exception)
        assertDoesNotThrow(() -> emptyVersion.clearIntermediateData(),
                "Clearing empty version should not throw exception");

        // Verify it's still empty
        assertTrue(emptyVersion.getDataSourceNames().isEmpty(), "Datasource names should be empty");
        assertTrue(emptyVersion.getAllGroupedDataKeys().isEmpty(), "Grouped data keys should be empty");
    }
}
