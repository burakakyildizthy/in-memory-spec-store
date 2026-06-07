package com.thy.fss.common.inmemory.datasource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing DataSource implementations with various failure scenarios.
 * This class provides helper methods to create DataSources with specific failure behaviors
 * and to simulate different types of failures for testing fallback mechanisms.
 */
public class DataSourceTestUtils {

    /**
     * Creates a TestableInMemoryDataSource that always fails with the specified exception.
     *
     * @param <T>        the entity type
     * @param name       the name of the DataSource
     * @param entityType the entity type class
     * @param exception  the exception to throw on every operation
     * @return a DataSource that always fails
     */
    public static <T> TestableInMemoryDataSource<T> createAlwaysFailingDataSource(
            String name, Class<T> entityType, RuntimeException exception) {
        TestableInMemoryDataSource<T> dataSource = new TestableInMemoryDataSource<>(name, entityType);
        dataSource.enableFailureSimulation(1.0); // Always fail
        dataSource.setFailureExceptionSupplier(() -> exception);
        return dataSource;
    }

    /**
     * Creates a TestableInMemoryDataSource that fails with the specified probability.
     *
     * @param <T>         the entity type
     * @param name        the name of the DataSource
     * @param entityType  the entity type class
     * @param data        the initial data for the DataSource
     * @param failureRate the probability of failure (0.0 to 1.0)
     * @return a DataSource that fails randomly
     */
    public static <T> TestableInMemoryDataSource<T> createRandomlyFailingDataSource(
            String name, Class<T> entityType, Collection<T> data, double failureRate) {
        TestableInMemoryDataSource<T> dataSource = new TestableInMemoryDataSource<>(name, entityType, data);
        dataSource.enableFailureSimulation(failureRate);
        return dataSource;
    }

    /**
     * Creates a TestableInMemoryDataSource that fails after a specific number of requests.
     *
     * @param <T>               the entity type
     * @param name              the name of the DataSource
     * @param entityType        the entity type class
     * @param data              the initial data for the DataSource
     * @param failAfterRequests the number of successful requests before starting to fail
     * @return a DataSource that fails after the specified number of requests
     */
    public static <T> TestableInMemoryDataSource<T> createFailAfterRequestsDataSource(
            String name, Class<T> entityType, Collection<T> data, int failAfterRequests) {
        TestableInMemoryDataSource<T> dataSource = new TestableInMemoryDataSource<>(name, entityType, data);
        dataSource.failAfterRequests(failAfterRequests);
        return dataSource;
    }

    /**
     * Creates a TestableInMemoryDataSource that becomes unhealthy but doesn't throw exceptions.
     *
     * @param <T>        the entity type
     * @param name       the name of the DataSource
     * @param entityType the entity type class
     * @param data       the initial data for the DataSource
     * @return an unhealthy DataSource
     */
    public static <T> TestableInMemoryDataSource<T> createUnhealthyDataSource(
            String name, Class<T> entityType, Collection<T> data) {
        TestableInMemoryDataSource<T> dataSource = new TestableInMemoryDataSource<>(name, entityType, data);
        dataSource.setHealthy(false);
        return dataSource;
    }

    /**
     * Creates a DataSource that simulates slow responses by adding delays.
     *
     * @param <T>         the entity type
     * @param name        the name of the DataSource
     * @param entityType  the entity type class
     * @param data        the initial data for the DataSource
     * @param delayMillis the delay in milliseconds for each operation
     * @return a DataSource with simulated delays
     */
    public static <T> DataSource<T> createSlowDataSource(
            String name, Class<T> entityType, Collection<T> data, long delayMillis) {
        InMemoryDataSource<T> baseDataSource = new InMemoryDataSource<>(name, entityType, data);

        return new DataSource<T>() {
            @Override
            public String getName() {
                return baseDataSource.getName();
            }

            @Override
            public Class<T> getEntityType() {
                return baseDataSource.getEntityType();
            }

            @Override
            public CompletableFuture<List<T>> fetchAll() {
                return CompletableFuture
                        .supplyAsync(() -> baseDataSource.fetchAll().join(),
                                CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS));
            }

            @Override
            public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
                return CompletableFuture
                        .supplyAsync(() -> baseDataSource.fetchAllById(ids).join(),
                                CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS));
            }

            @Override
            public boolean isHealthy() {
                return baseDataSource.isHealthy();
            }

            @Override
            public void close() {
                baseDataSource.close();
            }

            @Override
            public java.util.Optional<DataSource<T>> getFallbackDataSource() {
                return baseDataSource.getFallbackDataSource();
            }

            @Override
            public void setFallbackDataSource(DataSource<T> fallbackDataSource) {
                baseDataSource.setFallbackDataSource(fallbackDataSource);
            }
        };
    }

    /**
     * Creates a chain of DataSources with fallback relationships.
     * Each DataSource in the chain will have the next one as its fallback.
     *
     * @param <T>         the entity type
     * @param dataSources the DataSources to chain (first is primary, last is final fallback)
     * @return the primary DataSource with the fallback chain configured
     */
    @SafeVarargs
    public static <T> DataSource<T> createFallbackChain(DataSource<T>... dataSources) {
        if (dataSources == null || dataSources.length == 0) {
            throw new IllegalArgumentException("At least one DataSource must be provided");
        }

        // Configure fallback chain
        for (int i = 0; i < dataSources.length - 1; i++) {
            dataSources[i].setFallbackDataSource(dataSources[i + 1]);
        }

        return dataSources[0]; // Return the primary DataSource
    }

    /**
     * Creates a DataSource that fails for specific operations only.
     *
     * @param <T>              the entity type
     * @param baseDataSource   the base DataSource to wrap
     * @param failFetchAll     whether fetchAll() should fail
     * @param failFetchAllById whether fetchAllById() should fail
     * @param exception        the exception to throw on failure
     * @return a DataSource that fails for specific operations
     */
    public static <T> DataSource<T> createSelectivelyFailingDataSource(
            DataSource<T> baseDataSource, boolean failFetchAll, boolean failFetchAllById,
            RuntimeException exception) {

        return new DataSource<T>() {
            @Override
            public String getName() {
                return baseDataSource.getName();
            }

            @Override
            public Class<T> getEntityType() {
                return baseDataSource.getEntityType();
            }

            @Override
            public CompletableFuture<List<T>> fetchAll() {
                if (failFetchAll) {
                    return CompletableFuture.failedFuture(exception);
                }
                return baseDataSource.fetchAll();
            }

            @Override
            public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
                if (failFetchAllById) {
                    return CompletableFuture.failedFuture(exception);
                }
                return baseDataSource.fetchAllById(ids);
            }

            @Override
            public boolean isHealthy() {
                return baseDataSource.isHealthy();
            }

            @Override
            public void close() {
                baseDataSource.close();
            }

            @Override
            public java.util.Optional<DataSource<T>> getFallbackDataSource() {
                return baseDataSource.getFallbackDataSource();
            }

            @Override
            public void setFallbackDataSource(DataSource<T> fallbackDataSource) {
                baseDataSource.setFallbackDataSource(fallbackDataSource);
            }
        };
    }

    /**
     * Creates a simple InMemoryDataSource for basic testing scenarios.
     *
     * @param <T>        the entity type
     * @param name       the name of the DataSource
     * @param entityType the entity type class
     * @param data       the initial data for the DataSource
     * @return a basic InMemoryDataSource
     */
    public static <T> InMemoryDataSource<T> createBasicDataSource(
            String name, Class<T> entityType, Collection<T> data) {
        return new InMemoryDataSource<>(name, entityType, data);
    }

    /**
     * Creates a simple InMemoryDataSource for basic testing scenarios.
     *
     * @param <T>        the entity type
     * @param name       the name of the DataSource
     * @param entityType the entity type class
     * @return a basic empty InMemoryDataSource
     */
    public static <T> InMemoryDataSource<T> createBasicDataSource(
            String name, Class<T> entityType) {
        return new InMemoryDataSource<>(name, entityType);
    }
}