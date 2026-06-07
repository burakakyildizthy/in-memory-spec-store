package com.thy.fss.common.inmemory.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Core Dashboard class that manages dashboard data.
 * This class is now a passive data receiver that gets its data from DataSynchronizationEngine.
 * All synchronization logic has been moved to the centralized DataSynchronizationEngine.
 *
 * @param <T> The target class type for the dashboard data
 */
public class Dashboard<T> {

    private static final Logger log = LoggerFactory.getLogger(Dashboard.class);

    private final String id;
    private final String name;
    // AtomicReference provides thread-safe reads and writes
    // Write operations are infrequent (only during sync), reads are frequent
    private final AtomicReference<T> currentData;
    private final Class<T> targetClass;

    /**
     * Creates a new Dashboard with the specified configuration.
     * Initializes the dashboard with null data. Data will be populated by DataSynchronizationEngine.
     *
     * @throws IllegalArgumentException if configuration is null
     */
    public Dashboard(String id, String name, Class<T> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.id = id;
        this.targetClass = targetClass;
        this.name = name;
        this.currentData = new AtomicReference<>(null);
    }

    /**
     * Returns the unique dashboard ID.
     *
     * @return the dashboard ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the dashboard name from configuration.
     *
     * @return the dashboard name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the target class for dashboard data.
     *
     * @return the target class
     */
    public Class<T> getTargetClass() {
        return targetClass;
    }


    /**
     * Returns the current dashboard data.
     * Lock-free read with minimal memory barrier overhead.
     *
     * @return the current dashboard data, or null if not yet populated
     */
    public T getData() {
        return currentData.get();
    }

    /**
     * Updates the dashboard data.
     * This method is called by DataSynchronizationEngine to push new calculated data.
     * Volatile write ensures visibility to all threads.
     *
     * @param newData the new dashboard data to set
     */
    public void updateData(T newData) {
        this.currentData.set(newData);
        log.debug("Dashboard {} data updated", id);
    }
}
