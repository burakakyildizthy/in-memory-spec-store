package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Test helper utility for DataSynchronizationEngine integration tests.
 * Provides common setup, cleanup, and synchronization patterns for tests.
 * 
 * <p>This helper addresses the requirement that DataSynchronizationEngine must be used
 * to populate stores and dashboards, and handles static registry cleanup between tests.</p>
 */
public class DataSyncTestHelper {
    private static final String TEST_STORE = "test-store-";
    private static final String FAILED_TO_SYNC_DATASOURCE = "Failed to synchronize datasource";

    private static final Duration DEFAULT_SYNC_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(50);

    private DataSyncTestHelper() {
        // Utility class
    }

    /**
     * Creates a test environment with store, datasource, engine, and factory.
     * 
     * @param <T> entity type
     * @param entityClass entity class
     * @param initialData initial data for the datasource
     * @return configured test environment
     */
    public static <T> TestEnvironment<T> createTestEnvironment(
            Class<T> entityClass,
            List<T> initialData) {
        
        String dataSourceName = "test-ds-" + entityClass.getSimpleName();
        String storeId = TEST_STORE + entityClass.getSimpleName();
        
        InMemoryDataSource<T> dataSource = new InMemoryDataSource<>(dataSourceName, entityClass, initialData);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        factory.registerDataSource(dataSourceName, dataSource, Duration.ofSeconds(10));
        
        InMemoryDataStore<T> store = new InMemoryDataStore<>(entityClass, storeId, dataSourceName, null, null);
        
        DataSynchronizationEngine engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        awaitSync(() -> store.size() == initialData.size(), DEFAULT_SYNC_TIMEOUT);
        
        return new TestEnvironment<>(store, dataSource, engine, factory);
    }

    /**
     * Creates a dashboard test environment with store, dashboard, datasource, engine, and factory.
     * 
     * @param <T> entity type
     * @param <D> dashboard type
     * @param entityClass entity class
     * @param dashboardClass dashboard class
     * @param initialData initial data for the datasource
     * @return configured dashboard test environment
     */
    public static <T, D> DashboardTestEnvironment<T, D> createDashboardTestEnvironment(
            Class<T> entityClass,
            Class<D> dashboardClass,
            List<T> initialData) {
        
        String dataSourceName = "test-ds-" + entityClass.getSimpleName();
        String storeId = TEST_STORE + entityClass.getSimpleName();
        String dashboardId = "test-dashboard-" + dashboardClass.getSimpleName();
        
        InMemoryDataSource<T> dataSource = new InMemoryDataSource<>(dataSourceName, entityClass, initialData);
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        
        factory.registerDataSource(dataSourceName, dataSource, Duration.ofSeconds(10));
        
        InMemoryDataStore<T> store = new InMemoryDataStore<>(entityClass, storeId, dataSourceName, null, null);
        Dashboard<D> dashboard = new Dashboard<>(dashboardId, dashboardId, dashboardClass);
        
        DataSynchronizationEngine engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        awaitSync(() -> store.size() == initialData.size(), DEFAULT_SYNC_TIMEOUT);
        
        return new DashboardTestEnvironment<>(store, dashboard, dataSource, engine, factory);
    }

    /**
     * Updates datasource data and triggers synchronization.
     * 
     * @param <T> entity type
     * @param env test environment
     * @param newData new data to set
     */
    public static <T> void updateDataAndSync(TestEnvironment<T> env, List<T> newData) {
        env.getDataSource().clearData();
        env.getDataSource().addItems(newData);
        
        try {
            env.getEngine().synchronizeDataSource(env.getDataSource().getName());
        } catch (Exception e) {
            throw new RuntimeException(FAILED_TO_SYNC_DATASOURCE, e);
        }
        
        awaitSync(() -> env.getStore().size() == newData.size(), DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Updates datasource data and triggers synchronization for dashboard environment.
     * 
     * @param <T> entity type
     * @param <D> dashboard type
     * @param env dashboard test environment
     * @param newData new data to set
     */
    public static <T, D> void updateDataAndSync(DashboardTestEnvironment<T, D> env, List<T> newData) {
        env.getDataSource().clearData();
        env.getDataSource().addItems(newData);
        
        try {
            env.getEngine().synchronizeDataSource(env.getDataSource().getName());
        } catch (Exception e) {
            throw new RuntimeException(FAILED_TO_SYNC_DATASOURCE, e);
        }
        
        awaitSync(() -> env.getStore().size() == newData.size(), DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Waits for a synchronization condition to be met.
     * 
     * @param condition condition to wait for
     * @param timeout maximum wait time
     */
    public static void awaitSync(Supplier<Boolean> condition, Duration timeout) {
        TestSynchronizationHelper.waitForCondition(condition, timeout, DEFAULT_POLL_INTERVAL);
    }

    /**
     * Cleans up test environment and clears static registries.
     * 
     * @param env test environment to cleanup
     */
    public static <T> void cleanup(TestEnvironment<T> env) {
        if (env != null) {
            try {
                if (env.getEngine() != null) {
                    env.getEngine().close();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            
            try {
                if (env.getDataSource() != null) {
                    env.getDataSource().close();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        clearStaticRegistries();
    }

    /**
     * Cleans up dashboard test environment and clears static registries.
     * 
     * @param env dashboard test environment to cleanup
     */
    public static <T, D> void cleanup(DashboardTestEnvironment<T, D> env) {
        if (env != null) {
            try {
                if (env.getEngine() != null) {
                    env.getEngine().close();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            
            try {
                if (env.getDataSource() != null) {
                    env.getDataSource().close();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        clearStaticRegistries();
    }

    /**
     * Clears all static registries to prevent test interference.
     * Note: Registry pattern has been eliminated. This now only clears factory registrations.
     */
    public static void clearStaticRegistries() {
        clearFactoryRegistrations();
    }
    
    /**
     * Clears InMemorySpecStoreFactory registrations.
     */
    private static void clearFactoryRegistrations() {
        try {
            InMemorySpecStoreFactory.getInstance().clearAll();
        } catch (Exception e) {
            // Ignore if factory can't be cleared
        }
    }

    /**
     * Test environment holder for store-based tests.
     * 
     * @param <T> entity type
     */
    public static final class TestEnvironment<T> {
        private final InMemoryDataStore<T> store;
        private final InMemoryDataSource<T> dataSource;
        private final DataSynchronizationEngine engine;
        private final InMemorySpecStoreFactory factory;

        public TestEnvironment(
                InMemoryDataStore<T> store,
                InMemoryDataSource<T> dataSource,
                DataSynchronizationEngine engine,
                InMemorySpecStoreFactory factory) {
            this.store = store;
            this.dataSource = dataSource;
            this.engine = engine;
            this.factory = factory;
        }

        public InMemoryDataStore<T> getStore() {
            return store;
        }

        public InMemoryDataSource<T> getDataSource() {
            return dataSource;
        }

        public DataSynchronizationEngine getEngine() {
            return engine;
        }

        public InMemorySpecStoreFactory getFactory() {
            return factory;
        }
    }

    /**
     * Dashboard test environment holder for dashboard-based tests.
     * 
     * @param <T> entity type
     * @param <D> dashboard type
     */
    public static final class DashboardTestEnvironment<T, D> {
        private final InMemoryDataStore<T> store;
        private final Dashboard<D> dashboard;
        private final InMemoryDataSource<T> dataSource;
        private final DataSynchronizationEngine engine;
        private final InMemorySpecStoreFactory factory;

        public DashboardTestEnvironment(
                InMemoryDataStore<T> store,
                Dashboard<D> dashboard,
                InMemoryDataSource<T> dataSource,
                DataSynchronizationEngine engine,
                InMemorySpecStoreFactory factory) {
            this.store = store;
            this.dashboard = dashboard;
            this.dataSource = dataSource;
            this.engine = engine;
            this.factory = factory;
        }

        public InMemoryDataStore<T> getStore() {
            return store;
        }

        public Dashboard<D> getDashboard() {
            return dashboard;
        }

        public InMemoryDataSource<T> getDataSource() {
            return dataSource;
        }

        public DataSynchronizationEngine getEngine() {
            return engine;
        }

        public InMemorySpecStoreFactory getFactory() {
            return factory;
        }
    }
}
