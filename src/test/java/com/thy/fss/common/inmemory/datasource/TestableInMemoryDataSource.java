package com.thy.fss.common.inmemory.datasource;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Test-enhanced version of InMemoryDataSource with failure simulation capabilities.
 * This class extends the production InMemoryDataSource with additional testing features
 * such as failure simulation, health status manipulation, and request tracking.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class TestableInMemoryDataSource<T> extends InMemoryDataSource<T> {

    private final AtomicInteger requestCount = new AtomicInteger(0);
    // Failure simulation support
    private volatile boolean failureSimulationEnabled = false;
    private volatile double failureRate = 0.0; // 0.0 = never fail, 1.0 = always fail
    private volatile int failAfterRequests = -1; // -1 = disabled
    private final AtomicReference<Supplier<RuntimeException>> failureExceptionSupplier =
            new AtomicReference<>(() -> new RuntimeException("Simulated DataSource failure"));

    /**
     * Creates a new TestableInMemoryDataSource with the specified name and entity type.
     *
     * @param name       the name of this DataSource
     * @param entityType the Class object representing the entity type
     */
    public TestableInMemoryDataSource(String name, Class<T> entityType) {
        super(name, entityType);
    }

    /**
     * Creates a new TestableInMemoryDataSource with initial data.
     *
     * @param name        the name of this DataSource
     * @param entityType  the Class object representing the entity type
     * @param initialData the initial data to store
     */
    public TestableInMemoryDataSource(String name, Class<T> entityType, Collection<T> initialData) {
        super(name, entityType, initialData);
    }

    @Override
    public CompletableFuture<List<T>> fetchAll() {
        return CompletableFuture.supplyAsync(() -> {
            // Check for simulated failures first
            checkForSimulatedFailure();

            // Delegate to parent implementation
            return super.fetchAll().join();
        });
    }

    @Override
    public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
        return CompletableFuture.supplyAsync(() -> {
            // Check for simulated failures first
            checkForSimulatedFailure();

            // Delegate to parent implementation
            return super.fetchAllById(ids).join();
        });
    }

    // Test-specific methods

    /**
     * Sets the health status of this DataSource for testing purposes.
     *
     * @param healthy true to mark as healthy, false to mark as unhealthy
     */
    @Override
    public void setHealthy(boolean healthy) {
        super.setHealthy(healthy);
    }

    /**
     * Enables failure simulation with a specified failure rate.
     *
     * @param failureRate the probability of failure (0.0 = never fail, 1.0 = always fail)
     */
    public void enableFailureSimulation(double failureRate) {
        if (failureRate < 0.0 || failureRate > 1.0) {
            throw new IllegalArgumentException("Failure rate must be between 0.0 and 1.0");
        }
        this.failureSimulationEnabled = true;
        this.failureRate = failureRate;
    }

    /**
     * Disables failure simulation.
     */
    public void disableFailureSimulation() {
        this.failureSimulationEnabled = false;
        this.failureRate = 0.0;
        this.failAfterRequests = -1;
        this.requestCount.set(0);
    }

    /**
     * Sets whether this DataSource should fail on read operations.
     * This is a convenience method for testing failure scenarios.
     *
     * @param failOnRead true to make all read operations fail, false to succeed
     */
    public void setFailOnRead(boolean failOnRead) {
        if (failOnRead) {
            enableFailureSimulation(1.0); // Always fail
        } else {
            disableFailureSimulation();
        }
    }

    /**
     * Configures the DataSource to fail after a specific number of requests.
     *
     * @param requestCount the number of requests after which to start failing
     */
    public void failAfterRequests(int requestCount) {
        if (requestCount < 0) {
            throw new IllegalArgumentException("Request count must be non-negative");
        }
        this.failureSimulationEnabled = true;
        this.failAfterRequests = requestCount;
        this.requestCount.set(0);
    }

    /**
     * Sets a custom exception supplier for failure simulation.
     *
     * @param exceptionSupplier the supplier that provides exceptions for simulated failures
     */
    public void setFailureExceptionSupplier(Supplier<RuntimeException> exceptionSupplier) {
        this.failureExceptionSupplier.set(Objects.requireNonNull(exceptionSupplier,
                "Exception supplier cannot be null"));
    }

    /**
     * Returns the current request count for failure simulation tracking.
     *
     * @return the current request count
     */
    public int getRequestCount() {
        return requestCount.get();
    }

    /**
     * Resets the request count for failure simulation.
     */
    public void resetRequestCount() {
        requestCount.set(0);
    }

    /**
     * Checks if failure simulation is currently enabled.
     *
     * @return true if failure simulation is enabled, false otherwise
     */
    public boolean isFailureSimulationEnabled() {
        return failureSimulationEnabled;
    }

    /**
     * Returns the current failure rate.
     *
     * @return the current failure rate (0.0 to 1.0)
     */
    public double getFailureRate() {
        return failureRate;
    }

    /**
     * Checks for simulated failures and throws an exception if a failure should occur.
     * This method is called before each data access operation.
     */
    private void checkForSimulatedFailure() {
        if (!failureSimulationEnabled) {
            return;
        }

        int currentRequestCount = requestCount.incrementAndGet();

        // Check if we should fail after a specific number of requests
        if (failAfterRequests >= 0 && currentRequestCount > failAfterRequests) {
            throw failureExceptionSupplier.get().get();
        }

        // Check if we should fail based on failure rate
        if (failureRate > 0.0 && Math.random() < failureRate) {
            throw failureExceptionSupplier.get().get();
        }
    }
}