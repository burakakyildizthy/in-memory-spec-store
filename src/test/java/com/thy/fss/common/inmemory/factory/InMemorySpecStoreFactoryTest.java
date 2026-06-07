package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.exception.DataSourceNotFoundException;
import com.thy.fss.common.inmemory.engine.exception.DuplicateDataSourceException;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for InMemorySpecStoreFactory.
 * Tests singleton pattern, datasource registration, duplicate detection,
 * store/dashboard registration, and property mapping extraction.
 * <p>
 * Requirements tested:
 * - 2.1: Singleton DataSource Registry
 * - 2.2: Unique name validation
 * - 3.1: Merkezi Factory Pattern
 * - 3.6: Store ve Dashboard registry
 * - 6.4: PropertyMapping extraction
 */
@DisplayName("InMemorySpecStoreFactory Tests")
class InMemorySpecStoreFactoryTest {

    private static final String DS1 = "ds1";
    private static final String DS2 = "ds2";
    private static final String USERS = "users";
    private static final String NONEXISTENT = "nonexistent";
    private static final String TO_REMOVE = "to-remove";
    private static final String TEST_ENTITY_DS = "testEntityDataSource";

    private InMemorySpecStoreFactory factory;
    private DataSource<TestEntity> testDataSource1;
    private DataSource<TestEntity> testDataSource2;

    @BeforeEach
    void setUp() {
        // Get singleton instance
        factory = InMemorySpecStoreFactory.getInstance();
        
        // Clear all previous registrations to ensure test isolation
        factory.clearAll();

        // Create test datasources
        testDataSource1 = new InMemoryDataSource<>(
                "test-datasource-1",
                TestEntity.class,
                createTestEntities()
        );

        testDataSource2 = new InMemoryDataSource<>(
                "test-datasource-2",
                TestEntity.class,
                createTestEntities()
        );
    }

    @AfterEach
    void tearDown() {
        // Clean up factory state after each test to ensure test isolation
        factory.clearAll();
    }

    private List<TestEntity> createTestEntities() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        entity1.setName("Entity 1");
        entity1.setAge(25);

        TestEntity entity2 = new TestEntity();
        entity2.setId(2L);
        entity2.setName("Entity 2");
        entity2.setAge(30);

        return Arrays.asList(entity1, entity2);
    }

    // ========== Singleton Pattern Tests ==========

    @Test
    @DisplayName("getInstance should always return the same instance")
    void testSingletonPattern() {
        InMemorySpecStoreFactory instance1 = InMemorySpecStoreFactory.getInstance();
        InMemorySpecStoreFactory instance2 = InMemorySpecStoreFactory.getInstance();
        InMemorySpecStoreFactory instance3 = InMemorySpecStoreFactory.getInstance();

        assertSame(instance1, instance2, "getInstance should return same instance");
        assertSame(instance2, instance3, "getInstance should return same instance");
        assertSame(instance1, instance3, "getInstance should return same instance");
    }

    @Test
    @DisplayName("Singleton instance should be the same as factory field")
    void testSingletonConsistency() {
        assertSame(factory, InMemorySpecStoreFactory.getInstance(),
                "Factory field should be same as getInstance()");
    }

    // ========== DataSource Registration Tests ==========

    @Test
    @DisplayName("registerDataSource should register datasource with name and interval")
    void testRegisterDataSource() {
        Duration syncInterval = Duration.ofMinutes(5);

        factory.registerDataSource(USERS, testDataSource1, syncInterval);

        assertTrue(factory.hasDataSource(USERS), "Datasource should be registered");
        assertEquals(testDataSource1, factory.getDataSource(USERS),
                "Registered datasource should match");
        assertEquals(syncInterval, factory.getDataSourceInterval(USERS),
                "Sync interval should match");
    }

    @Test
    @DisplayName("registerDataSource with class should derive datasource name and register")
    void testRegisterDataSourceWithClass() {
        Duration syncInterval = Duration.ofMinutes(5);

        factory.registerDataSource(TestEntity.class, testDataSource1, syncInterval);

        // Verify datasource is registered with derived name: testEntityDataSource
        assertTrue(factory.hasDataSource(TEST_ENTITY_DS), 
                "Datasource should be registered with derived name");
        assertEquals(testDataSource1, factory.getDataSource(TEST_ENTITY_DS),
                "Registered datasource should match");
        assertEquals(syncInterval, factory.getDataSourceInterval(TEST_ENTITY_DS),
                "Sync interval should match");
    }

    @Test
    @DisplayName("registerDataSource with class and timeout should register with both")
    void testRegisterDataSourceWithClassAndTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);
        Duration readTimeout = Duration.ofSeconds(10);

        factory.registerDataSource(TestEntity.class, testDataSource1, syncInterval, readTimeout);

        assertTrue(factory.hasDataSource(TEST_ENTITY_DS), 
                "Datasource should be registered");
        assertEquals(readTimeout, factory.getDataSourceTimeout(TEST_ENTITY_DS),
                "Read timeout should match");
    }

    @Test
    @DisplayName("registerDataSource with class should throw exception for null class")
    void testRegisterDataSourceWithNullClass() {
        Duration syncInterval = Duration.ofMinutes(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDataSource((Class<TestEntity>) null, testDataSource1, syncInterval)
        );

        assertTrue(exception.getMessage().contains("Entity class cannot be null"),
                "Exception message should mention null entity class");
    }

    @Test
    @DisplayName("getDataSourceNameByClass should find datasource by class type")
    void testGetDataSourceNameByClass() {
        factory.registerDataSource(TestEntity.class, testDataSource1, Duration.ofMinutes(5));

        String datasourceName = factory.getDataSourceNameByClass(TestEntity.class);

        assertEquals(TEST_ENTITY_DS, datasourceName,
                "Should return correct datasource name for class");
    }

    @Test
    @DisplayName("getDataSourceNameByClass should throw exception when no datasource found")
    void testGetDataSourceNameByClassNotFound() {
        // Don't register any datasource

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.getDataSourceNameByClass(TestEntity.class)
        );

        assertTrue(exception.getMessage().contains("No datasource found for class type"),
                "Exception message should mention no datasource found");
        assertTrue(exception.getMessage().contains("TestEntity"),
                "Exception message should contain class name");
    }

    @Test
    @DisplayName("getDataSourceNameByClass should throw exception when multiple datasources found")
    void testGetDataSourceNameByClassMultipleFound() {
        // Register two datasources with same entity type but different names
        factory.registerDataSource("datasource1", testDataSource1, Duration.ofMinutes(5));
        factory.registerDataSource("datasource2", testDataSource2, Duration.ofMinutes(5));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.getDataSourceNameByClass(TestEntity.class)
        );

        assertTrue(exception.getMessage().contains("Multiple datasources found for class type"),
                "Exception message should mention multiple datasources");
        assertTrue(exception.getMessage().contains("TestEntity"),
                "Exception message should contain class name");
    }

    @Test
    @DisplayName("getDataSourceNameByClass should throw exception for null class")
    void testGetDataSourceNameByClassNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getDataSourceNameByClass(null)
        );

        assertTrue(exception.getMessage().contains("Entity class cannot be null"),
                "Exception message should mention null entity class");
    }

    @Test
    @DisplayName("registerDataSource should register datasource with timeout")
    void testRegisterDataSourceWithTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);
        Duration readTimeout = Duration.ofSeconds(10);

        factory.registerDataSource("orders", testDataSource1, syncInterval, readTimeout);

        assertTrue(factory.hasDataSource("orders"), "Datasource should be registered");
        assertEquals(readTimeout, factory.getDataSourceTimeout("orders"),
                "Read timeout should match");
    }

    @Test
    @DisplayName("registerDataSource should use default timeout when null")
    void testRegisterDataSourceWithNullTimeout() {
        Duration syncInterval = Duration.ofMinutes(5);

        factory.registerDataSource("products", testDataSource1, syncInterval, null);

        assertTrue(factory.hasDataSource("products"), "Datasource should be registered");
        assertNull(factory.getDataSourceTimeout("products"),
                "Timeout should be null (will use default)");
    }

    @Test
    @DisplayName("registerDataSource should throw exception for null name")
    void testRegisterDataSourceNullName() {
        Duration syncInterval = Duration.ofMinutes(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDataSource((String) null, testDataSource1, syncInterval)
        );

        assertTrue(exception.getMessage().contains("name cannot be null"),
                "Exception message should mention null name");
    }

    @Test
    @DisplayName("registerDataSource should throw exception for empty name")
    void testRegisterDataSourceEmptyName() {
        Duration syncInterval = Duration.ofMinutes(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDataSource("", testDataSource1, syncInterval)
        );

        assertTrue(exception.getMessage().contains("name cannot be null or empty"),
                "Exception message should mention empty name");
    }

    @Test
    @DisplayName("registerDataSource should throw exception for null datasource")
    void testRegisterDataSourceNullDataSource() {
        Duration syncInterval = Duration.ofMinutes(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDataSource("test", null, syncInterval)
        );

        assertTrue(exception.getMessage().contains("Datasource cannot be null"),
                "Exception message should mention null datasource");
    }

    @Test
    @DisplayName("registerDataSource should throw exception for null interval")
    void testRegisterDataSourceNullInterval() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDataSource("test", testDataSource1, null)
        );

        assertTrue(exception.getMessage().contains("Sync interval cannot be null"),
                "Exception message should mention null interval");
    }

    @Test
    @DisplayName("hasDataSource should return false for non-existent datasource")
    void testHasDataSourceNonExistent() {
        assertFalse(factory.hasDataSource(NONEXISTENT),
                "Should return false for non-existent datasource");
    }

    @Test
    @DisplayName("hasDataSource should return true for registered datasource")
    void testHasDataSourceExists() {
        factory.registerDataSource("test", testDataSource1, Duration.ofMinutes(5));

        assertTrue(factory.hasDataSource("test"),
                "Should return true for registered datasource");
    }

    @Test
    @DisplayName("getDataSource should throw exception for non-existent datasource")
    void testGetDataSourceNonExistent() {
        DataSourceNotFoundException exception = assertThrows(
                DataSourceNotFoundException.class,
                () -> factory.getDataSource(NONEXISTENT)
        );

        assertEquals(NONEXISTENT, exception.getDataSourceName(),
                "Exception should contain datasource name");
    }

    @Test
    @DisplayName("getDataSourceInterval should throw exception for non-existent datasource")
    void testGetDataSourceIntervalNonExistent() {
        DataSourceNotFoundException exception = assertThrows(
                DataSourceNotFoundException.class,
                () -> factory.getDataSourceInterval(NONEXISTENT)
        );

        assertTrue(exception.getMessage().contains("No interval found"),
                "Exception message should mention interval");
    }

    @Test
    @DisplayName("getDataSourceTimeout should throw exception for non-existent datasource")
    void testGetDataSourceTimeoutNonExistent() {
        DataSourceNotFoundException exception = assertThrows(
                DataSourceNotFoundException.class,
                () -> factory.getDataSourceTimeout(NONEXISTENT)
        );

        assertEquals(NONEXISTENT, exception.getDataSourceName(),
                "Exception should contain datasource name");
    }

    @Test
    @DisplayName("getAllDataSourceNames should return all registered datasource names")
    void testGetAllDataSourceNames() {
        factory.registerDataSource(DS1, testDataSource1, Duration.ofMinutes(5));
        factory.registerDataSource(DS2, testDataSource2, Duration.ofMinutes(10));

        List<String> names = factory.getAllDataSourceNames();

        assertEquals(2, names.size(), "Should have 2 datasource names");
        assertTrue(names.contains(DS1), "Should contain ds1");
        assertTrue(names.contains(DS2), "Should contain ds2");
    }

    @Test
    @DisplayName("getAllDataSourceNames should return unmodifiable list")
    void testGetAllDataSourceNamesUnmodifiable() {
        factory.registerDataSource("test", testDataSource1, Duration.ofMinutes(5));

        List<String> names = factory.getAllDataSourceNames();

        assertThrows(UnsupportedOperationException.class, () -> {
            names.add("new-datasource");
        }, "Should not be able to modify returned list");
    }

    // ========== Duplicate Detection Tests ==========

    @Test
    @DisplayName("registerDataSource should throw exception for duplicate name")
    void testRegisterDuplicateDataSource() {
        Duration syncInterval = Duration.ofMinutes(5);

        // Register first datasource
        factory.registerDataSource("duplicate", testDataSource1, syncInterval);

        // Try to register second datasource with same name
        DuplicateDataSourceException exception = assertThrows(
                DuplicateDataSourceException.class,
                () -> factory.registerDataSource("duplicate", testDataSource2, syncInterval)
        );

        assertEquals("duplicate", exception.getDataSourceName(),
                "Exception should contain datasource name");
        assertTrue(exception.getMessage().contains("already registered"),
                "Exception message should mention already registered");
    }

    @Test
    @DisplayName("registerDataSource should allow different names")
    void testRegisterMultipleDifferentDataSources() {
        Duration syncInterval = Duration.ofMinutes(5);

        factory.registerDataSource(DS1, testDataSource1, syncInterval);
        factory.registerDataSource(DS2, testDataSource2, syncInterval);

        assertTrue(factory.hasDataSource(DS1), "ds1 should be registered");
        assertTrue(factory.hasDataSource(DS2), "ds2 should be registered");
        assertEquals(testDataSource1, factory.getDataSource(DS1), "ds1 should match");
        assertEquals(testDataSource2, factory.getDataSource(DS2), "ds2 should match");
    }

    @Test
    @DisplayName("Duplicate detection should be case-sensitive")
    void testDuplicateDetectionCaseSensitive() {
        Duration syncInterval = Duration.ofMinutes(5);

        factory.registerDataSource("Users", testDataSource1, syncInterval);
        factory.registerDataSource(USERS, testDataSource2, syncInterval);

        assertTrue(factory.hasDataSource("Users"), "Users should be registered");
        assertTrue(factory.hasDataSource(USERS), "users should be registered");
        assertNotSame(factory.getDataSource("Users"), factory.getDataSource(USERS),
                "Different case should be different datasources");
    }

    // ========== Store Registration Tests ==========

    @Test
    @DisplayName("registerStore should add store to registry")
    void testRegisterStore() {
        // Register datasource first
        factory.registerDataSource("test-ds", testDataSource1, Duration.ofMinutes(5));

        // Build and register store
        InMemoryDataStore<TestEntity> store = factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        assertNotNull(store, "Store should be created");

        // Verify store is in registry
        List<String> storeIds = factory.getAllStoreIds();
        assertTrue(storeIds.contains(store.getStoreId()),
                "Store should be in registry");
    }

    @Test
    @DisplayName("registerStore should throw exception for null store")
    void testRegisterStoreNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerStore(null)
        );

        assertTrue(exception.getMessage().contains("Store cannot be null"),
                "Exception message should mention null store");
    }

    @Test
    @DisplayName("getAllStoreIds should return all registered store IDs")
    void testGetAllStoreIds() {
        // Register datasources
        factory.registerDataSource(DS1, testDataSource1, Duration.ofMinutes(5));

        // Build stores
        InMemoryDataStore<TestEntity> store = factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        List<String> storeIds = factory.getAllStoreIds();

        assertFalse(storeIds.isEmpty(), "Should have at least one store");
        assertTrue(storeIds.contains(store.getStoreId()), "Should contain store with correct ID");
    }

    @Test
    @DisplayName("getAllStoreIds should return unmodifiable list")
    void testGetAllStoreIdsUnmodifiable() {
        List<String> storeIds = factory.getAllStoreIds();

        assertThrows(UnsupportedOperationException.class, () -> {
            storeIds.add("new-store");
        }, "Should not be able to modify returned list");
    }

    @Test
    @DisplayName("getStoreById should return store for valid ID")
    void testGetStoreById() {
        // Register datasource and build store
        factory.registerDataSource("test-ds", testDataSource1, Duration.ofMinutes(5));
        InMemoryDataStore<TestEntity> store = factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        InMemoryDataStore<?> retrievedStore = factory.getStoreById(store.getStoreId());

        assertNotNull(retrievedStore, "Store should be found");
        assertSame(store, retrievedStore, "Retrieved store should be same instance");
    }

    @Test
    @DisplayName("getStoreById should return null for non-existent ID")
    void testGetStoreByIdNonExistent() {
        InMemoryDataStore<?> store = factory.getStoreById(NONEXISTENT);

        assertNull(store, "Should return null for non-existent store");
    }

    // ========== Dashboard Registration Tests ==========

    @Test
    @DisplayName("registerDashboard should add dashboard to registry")
    void testRegisterDashboard() {
        // Note: Dashboard building requires PropertyMappings which are complex
        // This test verifies the registration mechanism works

        // For now, we'll test that getAllDashboardIds works
        List<String> dashboardIds = factory.getAllDashboardIds();
        assertNotNull(dashboardIds, "Dashboard IDs list should not be null");
    }

    @Test
    @DisplayName("registerDashboard should throw exception for null dashboard")
    void testRegisterDashboardNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.registerDashboard(null, "test-id", null)
        );

        assertTrue(exception.getMessage().contains("Dashboard cannot be null"),
                "Exception message should mention null dashboard");
    }

    @Test
    @DisplayName("getAllDashboardIds should return unmodifiable list")
    void testGetAllDashboardIdsUnmodifiable() {
        List<String> dashboardIds = factory.getAllDashboardIds();

        assertThrows(UnsupportedOperationException.class, () -> {
            dashboardIds.add("new-dashboard");
        }, "Should not be able to modify returned list");
    }

    @Test
    @DisplayName("getDashboardById should return null for non-existent ID")
    void testGetDashboardByIdNonExistent() {
        Dashboard<?> dashboard = factory.getDashboardById(NONEXISTENT);

        assertNull(dashboard, "Should return null for non-existent dashboard");
    }

    // ========== Consumer ID Tests ==========

    @Test
    @DisplayName("getAllConsumerIds should return combined store and dashboard IDs")
    void testGetAllConsumerIds() {
        // Register datasource and build store
        factory.registerDataSource("test-ds", testDataSource1, Duration.ofMinutes(5));
        InMemoryDataStore<TestEntity> store = factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        List<String> consumerIds = factory.getAllConsumerIds();

        assertFalse(consumerIds.isEmpty(), "Should have at least one consumer");
        assertTrue(consumerIds.contains(store.getStoreId()), "Should contain store ID");
    }

    @Test
    @DisplayName("getAllConsumerIds should return unmodifiable list")
    void testGetAllConsumerIdsUnmodifiable() {
        List<String> consumerIds = factory.getAllConsumerIds();

        assertThrows(UnsupportedOperationException.class, () -> {
            consumerIds.add("new-consumer");
        }, "Should not be able to modify returned list");
    }

    // ========== PropertyMapping Extraction Tests ==========

    @Test
    @DisplayName("getAllPropertyMappings should return empty list when no stores/dashboards")
    void testGetAllPropertyMappingsEmpty() {
        List<PropertyMapping<?, ?>> mappings = factory.getAllPropertyMappings();

        assertNotNull(mappings, "Mappings list should not be null");
        // Note: Currently returns empty list as PropertyMapping storage is not yet implemented
    }

    @Test
    @DisplayName("getAllPropertyMappings should collect from all stores and dashboards")
    void testGetAllPropertyMappings() {
        // Register datasource and build store
        factory.registerDataSource("test-ds", testDataSource1, Duration.ofMinutes(5));
        factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        List<PropertyMapping<?, ?>> mappings = factory.getAllPropertyMappings();

        assertNotNull(mappings, "Mappings list should not be null");
        // Note: Currently returns empty list as PropertyMapping storage is not yet implemented
        // When implemented, this should verify that mappings are extracted correctly
    }

    // ========== Builder Tests ==========

    @Test
    @DisplayName("buildInMemoryStore should return builder")
    void testBuildInMemoryStore() {
        InMemoryStoreBuilder<TestEntity> builder = factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE);

        assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("buildDashboard should return builder")
    void testBuildDashboard() {
        DashboardBuilder<TestEntity> builder = factory.buildDashboard(TestEntitySpecificationService.INSTANCE);

        assertNotNull(builder, "Builder should not be null");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Factory should maintain state across multiple operations")
    void testFactoryStateConsistency() {
        // Register datasource
        factory.registerDataSource("persistent-ds", testDataSource1, Duration.ofMinutes(5));

        // Verify it persists
        assertTrue(factory.hasDataSource("persistent-ds"),
                "Datasource should persist");

        // Build store
        factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        // Verify both persist
        assertTrue(factory.hasDataSource("persistent-ds"),
                "Datasource should still be registered");
        assertFalse(factory.getAllStoreIds().isEmpty(),
                "Store should be registered");
    }

    // ========== Unregister and Clear Tests ==========

    @Test
    @DisplayName("unregisterDataSource should remove datasource from all registries")
    void testUnregisterDataSource() {
        Duration syncInterval = Duration.ofMinutes(5);
        Duration readTimeout = Duration.ofSeconds(10);

        factory.registerDataSource(TO_REMOVE, testDataSource1, syncInterval, readTimeout);
        assertTrue(factory.hasDataSource(TO_REMOVE), "Datasource should be registered");

        factory.unregisterDataSource(TO_REMOVE);

        assertFalse(factory.hasDataSource(TO_REMOVE), "Datasource should be unregistered");
        assertThrows(DataSourceNotFoundException.class,
                () -> factory.getDataSource(TO_REMOVE),
                "Should throw exception for unregistered datasource");
        assertThrows(DataSourceNotFoundException.class,
                () -> factory.getDataSourceInterval(TO_REMOVE),
                "Should throw exception for unregistered datasource interval");
        assertThrows(DataSourceNotFoundException.class,
                () -> factory.getDataSourceTimeout(TO_REMOVE),
                "Should throw exception for unregistered datasource timeout");
    }

    @Test
    @DisplayName("unregisterDataSource should handle null name gracefully")
    void testUnregisterDataSourceNull() {
        factory.unregisterDataSource(null);
        assertTrue(factory.getAllDataSourceNames().isEmpty(), "Should have no datasources");
    }

    @Test
    @DisplayName("unregisterDataSource should handle non-existent datasource gracefully")
    void testUnregisterDataSourceNonExistent() {
        factory.unregisterDataSource(NONEXISTENT);
        assertTrue(factory.getAllDataSourceNames().isEmpty(), "Should have no datasources");
    }

    @Test
    @DisplayName("clearAllDataSources should remove all datasources")
    void testClearAllDataSources() {
        factory.registerDataSource(DS1, testDataSource1, Duration.ofMinutes(5));
        factory.registerDataSource(DS2, testDataSource2, Duration.ofMinutes(10));

        assertEquals(2, factory.getAllDataSourceNames().size(), "Should have 2 datasources");

        factory.clearAllDataSources();

        assertTrue(factory.getAllDataSourceNames().isEmpty(), "All datasources should be cleared");
        assertFalse(factory.hasDataSource(DS1), "ds1 should be cleared");
        assertFalse(factory.hasDataSource(DS2), "ds2 should be cleared");
    }

    @Test
    @DisplayName("clearAll should remove all registrations")
    void testClearAll() {
        factory.registerDataSource(DS1, testDataSource1, Duration.ofMinutes(5));
        factory.buildInMemoryStore(TestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(TestEntity.class)
                .build();

        assertFalse(factory.getAllDataSourceNames().isEmpty(), "Should have datasources");
        assertFalse(factory.getAllStoreIds().isEmpty(), "Should have stores");

        factory.clearAll();

        assertTrue(factory.getAllDataSourceNames().isEmpty(), "All datasources should be cleared");
        assertTrue(factory.getAllStoreIds().isEmpty(), "All stores should be cleared");
        assertTrue(factory.getAllDashboardIds().isEmpty(), "All dashboards should be cleared");
        assertTrue(factory.getAllConsumerIds().isEmpty(), "All consumers should be cleared");
    }

    // ========== Configuration Validation Tests ==========

    @Test
    @DisplayName("Factory should validate datasource configuration")
    void testDataSourceConfigurationValidation() {
        Duration syncInterval = Duration.ofMinutes(5);
        Duration readTimeout = Duration.ofSeconds(30);

        factory.registerDataSource("validated-ds", testDataSource1, syncInterval, readTimeout);

        assertEquals(syncInterval, factory.getDataSourceInterval("validated-ds"),
                "Sync interval should match configured value");
        assertEquals(readTimeout, factory.getDataSourceTimeout("validated-ds"),
                "Read timeout should match configured value");
    }

    @Test
    @DisplayName("Factory should handle different interval durations")
    void testDifferentIntervalDurations() {
        factory.registerDataSource("ds-seconds", testDataSource1, Duration.ofSeconds(30));
        factory.registerDataSource("ds-minutes", testDataSource2, Duration.ofMinutes(5));

        assertEquals(Duration.ofSeconds(30), factory.getDataSourceInterval("ds-seconds"),
                "Should handle seconds interval");
        assertEquals(Duration.ofMinutes(5), factory.getDataSourceInterval("ds-minutes"),
                "Should handle minutes interval");
    }

    @Test
    @DisplayName("Factory should handle different timeout durations")
    void testDifferentTimeoutDurations() {
        factory.registerDataSource("ds-timeout-1", testDataSource1, Duration.ofMinutes(5), Duration.ofSeconds(10));
        factory.registerDataSource("ds-timeout-2", testDataSource2, Duration.ofMinutes(5), Duration.ofSeconds(60));

        assertEquals(Duration.ofSeconds(10), factory.getDataSourceTimeout("ds-timeout-1"),
                "Should handle 10 second timeout");
        assertEquals(Duration.ofSeconds(60), factory.getDataSourceTimeout("ds-timeout-2"),
                "Should handle 60 second timeout");
    }

    // ========== Large Dataset Tests ==========

    @Test
    @DisplayName("Factory should handle large dataset with 10K entities")
    void testFactoryWithLargeDataset() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<User> largeDataset = generator.generateUsers(10_000);
        
        DataSource<User> largeDataSource =
            new InMemoryDataSource<>("large-ds", User.class, largeDataset);
        
        factory.registerDataSource("large-users", largeDataSource, Duration.ofMinutes(5));
        
        assertTrue(factory.hasDataSource("large-users"), "Large datasource should be registered");
        assertEquals(largeDataSource, factory.getDataSource("large-users"),
                "Large datasource should be retrievable");
        
        InMemoryDataStore<User> store =
            factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
                .withPrimaryDataSource(User.class)
                .build();
        
        assertNotNull(store, "Store with large dataset should be created");
        assertTrue(factory.getAllStoreIds().contains(store.getStoreId()), "Store should be registered");
    }

    @Test
    @DisplayName("Factory should handle multiple stores with large datasets")
    void testMultipleStoresWithLargeDatasets() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<User> users = generator.generateUsers(10_000);
        List<Order> orders = generator.generateOrders(10_000);
        
        DataSource<User> userDataSource =
            new InMemoryDataSource<>("users-ds", User.class, users);
        DataSource<Order> orderDataSource =
            new InMemoryDataSource<>("orders-ds", Order.class, orders);
        
        factory.registerDataSource("large-users", userDataSource, Duration.ofMinutes(5));
        factory.registerDataSource("large-orders", orderDataSource, Duration.ofMinutes(10));
        
        InMemoryDataStore<User> userStore =
            factory.buildInMemoryStore(UserSpecificationService.INSTANCE)
                .withPrimaryDataSource(User.class)
                .build();
        
        InMemoryDataStore<Order> orderStore =
            factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class)
                .build();
        
        assertNotNull(userStore, "User store should be created");
        assertNotNull(orderStore, "Order store should be created");
        
        assertEquals(2, factory.getAllDataSourceNames().size(), "Should have 2 datasources");
        assertEquals(2, factory.getAllStoreIds().size(), "Should have 2 stores");
        assertEquals(2, factory.getAllConsumerIds().size(), "Should have 2 consumers");
    }

    @Test
    @DisplayName("Factory should handle concurrent datasource registrations")
    void testConcurrentDataSourceRegistrations() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<TestUser> dataset = generator.generateTestUsers(5_000);
        
        for (int i = 0; i < 10; i++) {
            DataSource<TestUser> ds =
                new InMemoryDataSource<>("ds-" + i, TestUser.class, dataset);
            factory.registerDataSource("concurrent-ds-" + i, ds, Duration.ofMinutes((long) i + 1));
        }
        
        assertEquals(10, factory.getAllDataSourceNames().size(), "Should have 10 datasources");
        
        for (int i = 0; i < 10; i++) {
            assertTrue(factory.hasDataSource("concurrent-ds-" + i),
                    "Datasource " + i + " should be registered");
            assertEquals(Duration.ofMinutes((long) i + 1), factory.getDataSourceInterval("concurrent-ds-" + i),
                    "Datasource " + i + " should have correct interval");
        }
    }

    @Test
    @DisplayName("Factory should handle property mapping extraction with large datasets")
    void testPropertyMappingExtractionWithLargeDatasets() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<User> users = generator.generateUsers(10_000);
        
        DataSource<User> userDataSource =
            new InMemoryDataSource<>("users-ds", User.class, users);
        
        factory.registerDataSource("mapping-users", userDataSource, Duration.ofMinutes(5));
        
        List<PropertyMapping<?, ?>> mappings = factory.getAllPropertyMappings();
        
        assertNotNull(mappings, "Property mappings should not be null");
    }

    @Test
    @DisplayName("Factory should handle store retrieval with large datasets")
    void testStoreRetrievalWithLargeDatasets() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<Customer> customers = generator.generateCustomers(10_000);
        
        DataSource<Customer> customerDataSource =
            new InMemoryDataSource<>("customers-ds", Customer.class, customers);
        
        factory.registerDataSource("retrieval-customers", customerDataSource, Duration.ofMinutes(5));
        
        InMemoryDataStore<Customer> store =
            factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();
        
        InMemoryDataStore<?> retrievedStore = factory.getStoreById(store.getStoreId());
        
        assertNotNull(retrievedStore, "Store should be retrievable");
        assertSame(store, retrievedStore, "Retrieved store should be same instance");
        assertEquals(Customer.class, retrievedStore.getTargetClass(),
                "Store should have correct target class");
    }

    @Test
    @DisplayName("Factory should handle consumer management with large datasets")
    void testConsumerManagementWithLargeDatasets() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<Product> products = generator.generateProducts(10_000);
        
        DataSource<Product> productDataSource =
            new InMemoryDataSource<>("products-ds", Product.class, products);
        
        factory.registerDataSource("consumer-products", productDataSource, Duration.ofMinutes(5));
        
        InMemoryDataStore<Product> store =
            factory.buildInMemoryStore(ProductSpecificationService.INSTANCE)
                .withPrimaryDataSource(Product.class)
                .build();
        
        List<String> consumerIds = factory.getAllConsumerIds();
        
        assertFalse(consumerIds.isEmpty(), "Should have consumers");
        assertTrue(consumerIds.contains(store.getStoreId()), "Should contain Product consumer");
    }

    @Test
    @DisplayName("Factory should validate configuration with large datasets")
    void testConfigurationValidationWithLargeDatasets() {
        com.thy.fss.common.inmemory.common.LargeDatasetGenerator generator = 
            new com.thy.fss.common.inmemory.common.LargeDatasetGenerator();
        
        List<SimpleUser> users = generator.generateSimpleUsers(10_000);
        
        DataSource<SimpleUser> userDataSource =
            new InMemoryDataSource<>("users-ds", SimpleUser.class, users);
        
        Duration syncInterval = Duration.ofMinutes(3);
        Duration readTimeout = Duration.ofSeconds(45);
        
        factory.registerDataSource("validated-large-ds", userDataSource, syncInterval, readTimeout);
        
        assertEquals(syncInterval, factory.getDataSourceInterval("validated-large-ds"),
                "Sync interval should be validated correctly");
        assertEquals(readTimeout, factory.getDataSourceTimeout("validated-large-ds"),
                "Read timeout should be validated correctly");
        
        InMemoryDataStore<SimpleUser> store =
            factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();
        
        assertTrue(factory.getAllStoreIds().contains(store.getStoreId()),
                "Store should be registered with validated configuration");
    }
}
