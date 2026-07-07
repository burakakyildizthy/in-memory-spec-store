package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.integration.testentities.SimpleTestEntity;
import com.thy.fss.common.inmemory.integration.testentities.SimpleTestEntitySpecificationService;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test to verify basic cross-component functionality.
 * This test avoids dependencies on problematic existing test classes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleIntegrationTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine syncEngine;
    private TestableInMemoryDataSource<SimpleTestEntity> dataSource;
    private InMemoryDataStore<SimpleTestEntity> store;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll(); // Clear any previous registrations

        // Create test data
        List<SimpleTestEntity> entities = Arrays.asList(
                new SimpleTestEntity("1", "Entity One", 100),
                new SimpleTestEntity("2", "Entity Two", 200),
                new SimpleTestEntity("3", "Entity Three", 300)
        );

        // Set up data source with initial data
        dataSource = new TestableInMemoryDataSource<>("test-entities", SimpleTestEntity.class, entities);
    }

    @AfterEach
    void tearDown() {
        // Close sync engine first
        if (syncEngine != null) {
            syncEngine.close();
        }
        
        // Close data source
        if (dataSource != null) {
            dataSource.close();
        }
        
        // Clear factory
        factory.clearAll();
    }

    /**
     * Test basic datasource-store integration.
     */
    @Test
    @Order(1)
    void testBasicDataSourceStoreIntegration() {
        // Register datasource and create store with short sync interval for testing
        factory.registerDataSource("test-entities", dataSource, Duration.ofMillis(500));

        store = factory.buildInMemoryStore(SimpleTestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleTestEntity.class)
                .build();

        // Initialize and start sync engine
        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();

        // Wait for initial synchronization
        TestUtil.await(2000);

        // Verify data was loaded
        List<SimpleTestEntity> entities = store.findAll();
        assertNotNull(entities, "Store should return entities");
        assertEquals(3, entities.size(), "Store should contain 3 entities");
    }

    /**
     * Test data source health monitoring integration.
     */
    @Test
    @Order(2)
    void testDataSourceHealthIntegration() {
        // Register datasource and create store with short sync interval for testing
        factory.registerDataSource("test-entities-health", dataSource, Duration.ofMillis(500));

        store = factory.buildInMemoryStore(SimpleTestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleTestEntity.class)
                .build();

        // Initialize and start sync engine
        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();

        TestUtil.await(2000);

        // Simulate datasource failure
        dataSource.setHealthy(false);

        // Wait for next sync cycle to detect failure
        TestUtil.await(1500);

        // Verify store handles failure gracefully
        List<SimpleTestEntity> entities = store.findAll();
        // Note: Store behavior during datasource failure may vary - it might preserve data or clear it
        // The important thing is that the store remains operational
        assertNotNull(entities, "Store should return a list even after datasource failure");

        // Test recovery
        dataSource.setHealthy(true);

        // Wait for recovery
        TestUtil.await(1500);

        // Verify store recovers and data is accessible
        entities = store.findAll();
        assertNotNull(entities, "Store should return entities after recovery");
        assertEquals(3, entities.size(), "Store should contain 3 entities after recovery");
    }

    /**
     * Test concurrent access to integrated components.
     */
    @Test
    @Order(3)
    void testConcurrentIntegration() throws Exception {
        // Register datasource and create store with short sync interval for testing
        factory.registerDataSource("test-entities-concurrent", dataSource, Duration.ofMillis(500));

        store = factory.buildInMemoryStore(SimpleTestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleTestEntity.class)
                .build();

        // Initialize and start sync engine
        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();

        TestUtil.await(2000);

        // Test concurrent access
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    // Concurrent store operations
                    List<SimpleTestEntity> entities = store.findAll();
                    results[threadId] = entities != null && entities.size() == 3;

                } catch (Exception e) {
                    results[threadId] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }

        // Verify all threads succeeded
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i], "Thread " + i + " should succeed");
        }
    }

    /**
     * Test end-to-end workflow with data updates.
     */
    @Test
    @Order(4)
    void testEndToEndWorkflow() {
        // Give time for previous test cleanup
        TestUtil.await(500);
        
        // Step 1: Register datasource and create store with short sync interval
        factory.registerDataSource("test-entities-workflow", dataSource, Duration.ofMillis(500));

        store = factory.buildInMemoryStore(SimpleTestEntitySpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleTestEntity.class)
                .build();

        // Initialize and start sync engine
        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();

        TestUtil.await(2000);

        List<SimpleTestEntity> initialEntities = store.findAll();
        assertEquals(3, initialEntities.size(), "Should have initial data");

        // Step 2: Update the existing datasource with new data
        dataSource.clearData();
        dataSource.addItem(new SimpleTestEntity("1", "Updated Entity One", 150));
        dataSource.addItem(new SimpleTestEntity("2", "Updated Entity Two", 250));
        dataSource.addItem(new SimpleTestEntity("3", "Updated Entity Three", 350));
        dataSource.addItem(new SimpleTestEntity("4", "New Entity Four", 400));

        // Trigger synchronization to pick up the changes
        syncEngine.synchronizeDataSource("test-entities-workflow");

        // Step 3: Wait for synchronization to complete with polling
        int maxAttempts = 20;
        int attempt = 0;
        List<SimpleTestEntity> finalEntities = null;
        while (attempt < maxAttempts) {
            TestUtil.await(500);
            finalEntities = store.findAll();
            if (finalEntities.size() == 4) {
                break;
            }
            attempt++;
        }

        // Step 4: Verify updates propagated
        assertNotNull(finalEntities, "Store should return entities");
        assertEquals(4, finalEntities.size(), "Should have updated data");

        // Step 5: Verify data integrity
        boolean foundNewEntity = finalEntities.stream()
                .anyMatch(entity -> "New Entity Four".equals(entity.getName()));
        assertTrue(foundNewEntity, "Should find the new entity");
    }

}