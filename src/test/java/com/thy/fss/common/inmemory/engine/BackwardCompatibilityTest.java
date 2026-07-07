package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.engine.sync.DataSourceSyncMetadata;
import com.thy.fss.common.inmemory.engine.sync.DataVersion;
import com.thy.fss.common.inmemory.engine.sync.DependencyGraph;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;
import com.thy.fss.common.inmemory.testmodel.SimpleUserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.SimpleUser_;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backward compatibility tests verifying that batch-only configurations
 * work identically after streaming datasource infrastructure was added.
 *
 * Validates Requirements: 13.1, 13.2, 13.3, 13.4, 13.5
 */
class BackwardCompatibilityTest {

    private static final String USERS = "users";
    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String BATCH_DS = "batchDs";

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private List<TestableInMemoryDataSource<?>> testDataSources;
    private SpecificationBuilder<SimpleUser> builder;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        builder = SpecificationBuilder.forService(SimpleUserSpecificationService.INSTANCE);
        factory.clearAll();
        factory.clearAllDataSources();
        testDataSources = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
        for (TestableInMemoryDataSource<?> ds : testDataSources) {
            ds.close();
        }
        testDataSources.clear();
        factory.clearAll();
        factory.clearAllDataSources();
    }

    private <T> TestableInMemoryDataSource<T> createTestDataSource(String name, Class<T> entityType) {
        TestableInMemoryDataSource<T> ds = new TestableInMemoryDataSource<>(name, entityType);
        testDataSources.add(ds);
        return ds;
    }

    private SimpleUser createUser(Long id, String name) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setActive(true);
        return user;
    }

    // ==================== Requirement 13.2: DataSource interface unchanged ====================

    @Test
    void testDataSourceInterfaceHasAllOriginalMethods() {
        // Verify DataSource interface still declares all original methods via compilation check.
        // If any method were removed, this test would not compile.
        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource("verify", SimpleUser.class);
        DataSource<SimpleUser> dataSource = ds;

        assertNotNull(dataSource.fetchAll());
        assertNotNull(dataSource.fetchAllById(List.of()));
        assertDoesNotThrow(dataSource::isHealthy);
        assertDoesNotThrow(dataSource::close);
        assertNotNull(dataSource.getFallbackDataSource());
        assertNotNull(dataSource.getName());
        assertNotNull(dataSource.getEntityType());
    }

    // ==================== Requirement 13.3: InMemoryDataStore.updateData unchanged ====================

    @Test
    void testInMemoryDataStoreUpdateDataSignaturePreserved() {
        InMemoryDataStore<SimpleUser> store = new InMemoryDataStore<>(
                SimpleUser.class, "testStore", "testDs", null, null);

        SimpleUser user = createUser(1L, ALICE);

        // updateData(List<T>, long) must still work
        store.updateData(List.of(user), 1L);
        assertEquals(1, store.findAll().size());
        assertEquals(1L, store.getVersion());

        // updateData(List<T>) convenience overload must still work
        store.updateData(List.of(user, createUser(2L, BOB)));
        assertEquals(2, store.findAll().size());
    }

    // ==================== Requirement 13.4: PropertyMapping, DataVersion, DataSourceSyncMetadata unchanged ====================

    @Test
    void testDataVersionPublicApiPreserved() {
        DataVersion dv = new DataVersion(1L, java.time.LocalDateTime.now());

        assertEquals(1L, dv.getVersion());
        assertNotNull(dv.getTimestamp());

        // Core methods must still exist
        dv.setDataByDataSource("ds1", List.of());
        dv.setPopulatedEntities("consumer1", List.of());
        assertNotNull(dv.getAllPopulatedEntities());
        assertNotNull(dv.makeImmutable());
    }

    @Test
    void testDataSourceSyncMetadataPublicApiPreserved() {
        DataSourceSyncMetadata metadata = new DataSourceSyncMetadata("ds1", Duration.ofMinutes(5));

        assertEquals("ds1", metadata.getDataSourceName());
        assertEquals(Duration.ofMinutes(5), metadata.getSyncInterval());
        assertDoesNotThrow(metadata::isHealthy);
        assertDoesNotThrow(metadata::shouldSync);
        assertDoesNotThrow(metadata::getLastSyncTime);
        assertDoesNotThrow(metadata::getNextSyncTime);
        assertDoesNotThrow(metadata::getConsecutiveFailures);
        assertDoesNotThrow(metadata::getLastErrorMessage);

        // Mutation methods
        assertDoesNotThrow(() -> metadata.recordSuccess());
        assertDoesNotThrow(() -> metadata.recordFailure("test error"));
        assertDoesNotThrow(() -> metadata.markHealthy());
        assertDoesNotThrow(() -> metadata.markUnhealthy("test"));
    }

    // ==================== Requirement 13.5: Batch registration unchanged ====================

    @Test
    void testBatchDataSourceRegistrationWithSyncIntervalPreserved() {
        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource(BATCH_DS, SimpleUser.class);

        // registerDataSource(String, DataSource, Duration) must still work
        assertDoesNotThrow(() ->
                factory.registerDataSource(BATCH_DS, ds, Duration.ofMinutes(5)));

        assertTrue(factory.hasDataSource(BATCH_DS));
        assertEquals(Duration.ofMinutes(5), factory.getDataSourceInterval(BATCH_DS));
        assertFalse(factory.isStreamingDataSource(BATCH_DS));
    }

    // ==================== Requirement 13.1: Batch-only config preserves existing behavior ====================

    @Test
    void testBatchOnlyConfigInitializesAndSyncsCorrectly() {
        // Given: Only batch datasources, no streaming
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource(USERS, SimpleUser.class);
        ds.addItems(List.of(createUser(1L, ALICE), createUser(2L, BOB)));

        factory.registerDataSource(USERS, ds, Duration.ofMinutes(5));

        InMemoryDataStore<SimpleUser> store = factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        // When
        engine.initialize();

        // Then: Engine runs, sync works, data is available
        assertTrue(engine.isRunning());
        assertNotNull(engine.getCurrentDataVersion());

        engine.synchronizeDataSource(USERS);
        TestUtil.await(2000);

        List<SimpleUser> data = store.findAll();
        assertEquals(2, data.size(), "Batch sync should populate store with 2 users");
    }

    @Test
    void testBatchOnlyConfigStreamingInfrastructureDoesNotInterfere() {
        // Given: Only batch datasources
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource(USERS, SimpleUser.class);
        ds.addItems(List.of(createUser(1L, ALICE)));

        factory.registerDataSource(USERS, ds, Duration.ofMinutes(5));

        factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        // When
        engine.initialize();

        // Then: Streaming infrastructure is created but has no streaming datasources
        assertNotNull(engine.getDependencyGraph(), "DependencyGraph should be created even without streaming datasources");
        assertNotNull(engine.getIncrementalSyncProcessor(), "IncrementalSyncProcessor should be created");
        assertNotNull(engine.getLifecycleManager(), "LifecycleManager should be created");

        // No streaming datasources registered
        assertTrue(factory.getAllStreamingDataSourceNames().isEmpty(),
                "No streaming datasources should be registered");

        // Streaming version should be 0 (no streaming events processed)
        assertEquals(0L, engine.getStreamingVersion(),
                "Streaming version should be 0 with no streaming datasources");
        assertNull(engine.getLastStreamingUpdateTimestamp(),
                "Last streaming update timestamp should be null");
    }

    @Test
    void testBatchOnlyConfigDataSyncUpdatesStoreCorrectly() {
        // Given: Batch datasource with initial data, then updated data
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource(USERS, SimpleUser.class);
        ds.addItems(List.of(createUser(1L, ALICE), createUser(2L, BOB)));

        factory.registerDataSource(USERS, ds, Duration.ofMinutes(5));

        InMemoryDataStore<SimpleUser> store = factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        engine.initialize();
        engine.synchronizeDataSource(USERS);
        TestUtil.await(2000);

        assertEquals(2, store.findAll().size(), "Store should have 2 users after first sync");

        // When: Update datasource data and re-sync
        ds.clearData();
        ds.addItems(List.of(createUser(1L, ALICE), createUser(2L, BOB), createUser(3L, "Charlie")));
        engine.synchronizeDataSource(USERS);
        TestUtil.await(2000);

        // Then: Store reflects updated data
        assertEquals(3, store.findAll().size(), "Store should have 3 users after re-sync");
    }

    @Test
    void testBatchOnlyConfigWithSpecificationFiltering() {
        // Given: Batch datasource with specification filter
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource("filtered", SimpleUser.class);
        SimpleUser active = createUser(1L, ALICE);
        active.setActive(true);
        SimpleUser inactive = createUser(2L, BOB);
        inactive.setActive(false);
        ds.addItems(List.of(active, inactive));

        factory.registerDataSource("filtered", ds, Duration.ofMinutes(5));

        Specification<SimpleUser> spec = builder.where(SimpleUser_.active).equalTo(true);

        InMemoryDataStore<SimpleUser> store = factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .withSpecification(spec)
                .build();

        // When
        engine.initialize();
        engine.synchronizeDataSource("filtered");
        TestUtil.await(2000);

        // Then: Only active users in store
        assertEquals(1, store.findAll().size(), "Only active users should be in store");
        assertTrue(store.findAll().stream().allMatch(SimpleUser::getActive));
    }

    @Test
    void testBatchOnlyConfigDataVersionSwapWorks() {
        // Given
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource("versioned", SimpleUser.class);
        ds.addItems(List.of(createUser(1L, ALICE)));

        factory.registerDataSource("versioned", ds, Duration.ofMinutes(5));

        factory.buildInMemoryStore(SimpleUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(SimpleUser.class)
                .build();

        engine.initialize();
        DataVersion v0 = engine.getCurrentDataVersion();

        // When: Trigger sync
        engine.synchronizeDataSource("versioned");
        TestUtil.await(2000);

        // Then: DataVersion should have been swapped
        DataVersion v1 = engine.getCurrentDataVersion();
        assertTrue(v1.getVersion() >= v0.getVersion(),
                "DataVersion should be incremented or equal after sync");
    }

    @Test
    void testBatchOnlyConfigEngineCloseReleasesResources() {
        // Given
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds = createTestDataSource("closeable", SimpleUser.class);
        factory.registerDataSource("closeable", ds, Duration.ofMinutes(5));

        engine.initialize();
        assertTrue(engine.isRunning());

        // When
        engine.close();

        // Then
        assertFalse(engine.isRunning(), "Engine should be stopped after close");
    }

    @Test
    void testBatchOnlyConfigMetadataCreatedForAllDatasources() {
        // Given
        engine = new DataSynchronizationEngine(factory);

        TestableInMemoryDataSource<SimpleUser> ds1 = createTestDataSource("meta1", SimpleUser.class);
        TestableInMemoryDataSource<SimpleUser> ds2 = createTestDataSource("meta2", SimpleUser.class);

        factory.registerDataSource("meta1", ds1, Duration.ofMinutes(5));
        factory.registerDataSource("meta2", ds2, Duration.ofMinutes(10));

        // When
        engine.initialize();

        // Then
        Map<String, DataSourceSyncMetadata> metadata = engine.getDataSourceMetadataInternal();
        assertTrue(metadata.containsKey("meta1"));
        assertTrue(metadata.containsKey("meta2"));

        // Batch datasources should NOT be marked as streaming
        assertFalse(metadata.get("meta1").isStreamingDataSource());
        assertFalse(metadata.get("meta2").isStreamingDataSource());
    }
}
