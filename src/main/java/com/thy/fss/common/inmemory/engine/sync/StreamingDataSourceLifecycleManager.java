package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Manages the lifecycle of streaming datasources: connection state tracking,
 * error handling, reconnection logic, and exponential backoff.
 *
 * <p>Designed to be used by {@code DataSynchronizationEngine} during
 * initialization (Task 9.1). This class handles the standalone lifecycle
 * management logic for streaming datasources.</p>
 *
 * State Transitions
 * <ul>
 *   <li>Start → INITIALIZING (datasource registered)</li>
 *   <li>INITIALIZING → READY (initial data load complete)</li>
 *   <li>INITIALIZING → ERROR (connection error during initial load)</li>
 *   <li>READY → ERROR (connection dropped)</li>
 *   <li>ERROR → INITIALIZING (reconnection successful, initial load restarts)</li>
 * </ul>
 *
 * Exponential Backoff
 * <p>After 3 consecutive failed reconnection attempts, an exponential backoff
 * strategy is applied: {@code baseDelay * 2^(attempt - 3)}. The delay is
 * capped at {@link #MAX_BACKOFF_DELAY}.</p>
 */
public class StreamingDataSourceLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(StreamingDataSourceLifecycleManager.class);

    /** Number of consecutive failures before exponential backoff kicks in. */
    static final int BACKOFF_THRESHOLD = 3;

    /** Base delay for exponential backoff calculations. */
    static final Duration BASE_BACKOFF_DELAY = Duration.ofSeconds(1);

    /** Maximum backoff delay cap (5 minutes). */
    static final Duration MAX_BACKOFF_DELAY = Duration.ofMinutes(5);

    private final Map<String, DatasourceLifecycleState> states = new ConcurrentHashMap<>();

    private long initialLoadTimeoutMs = Duration.ofMinutes(5).toMillis();

    /**
     * Registers a streaming datasource for lifecycle management.
     * Initial state is set to INITIALIZING.
     *
     * @param dataSourceName the datasource name
     */
    public void register(String dataSourceName) {
        states.put(dataSourceName, new DatasourceLifecycleState(dataSourceName));
        logger.info("Registered streaming datasource '{}' for lifecycle management, state=INITIALIZING",
                dataSourceName);
    }

    /**
     * Unregisters a streaming datasource from lifecycle management.
     *
     * @param dataSourceName the datasource name
     */
    public void unregister(String dataSourceName) {
        states.remove(dataSourceName);
        logger.info("Unregistered streaming datasource '{}' from lifecycle management", dataSourceName);
    }

    /**
     * Returns the current lifecycle state for a datasource.
     *
     * @param dataSourceName the datasource name
     * @return the current state, or null if not registered
     */
    public StreamingDataSourceState getState(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        return lifecycle != null ? lifecycle.state : null;
    }

    /**
     * Returns whether the datasource is healthy (connected and not in ERROR state).
     *
     * @param dataSourceName the datasource name
     * @return true if healthy, false if unhealthy or not registered
     */
    public boolean isHealthy(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        return lifecycle != null && lifecycle.healthy;
    }

    /**
     * Returns the number of consecutive reconnection failures for a datasource.
     *
     * @param dataSourceName the datasource name
     * @return the consecutive failure count, or 0 if not registered
     */
    public int getConsecutiveFailures(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        return lifecycle != null ? lifecycle.consecutiveFailures.get() : 0;
    }

    /**
     * Handles a connection loss event. Sets state to ERROR, logs the error,
     * and marks the datasource as unhealthy.
     *
     * <p>This is called when a streaming datasource reports that its connection
     * has dropped, whether during READY state or INITIALIZING state.</p>
     *
     * @param dataSourceName the datasource name
     * @param errorMessage   a description of the connection error
     */
    public void handleConnectionLoss(String dataSourceName, String errorMessage) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null) {
            logger.warn("Connection loss reported for unregistered datasource '{}'", dataSourceName);
            return;
        }

        StreamingDataSourceState previousState = lifecycle.state;
        lifecycle.state = StreamingDataSourceState.ERROR;
        lifecycle.healthy = false;
        lifecycle.lastErrorMessage = errorMessage;

        logger.error("Streaming datasource '{}' connection lost (previous state={}): {}",
                dataSourceName, previousState, errorMessage);
    }

    /**
     * Handles a successful reconnection. Sets state to INITIALIZING so that
     * the initial data load can restart from scratch, recovering any data
     * missed during the disconnection period.
     *
     * <p>Resets the consecutive failure counter to zero.</p>
     *
     * @param dataSourceName the datasource name
     */
    public void handleReconnectionSuccess(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null) {
            logger.warn("Reconnection success reported for unregistered datasource '{}'", dataSourceName);
            return;
        }

        lifecycle.state = StreamingDataSourceState.INITIALIZING;
        lifecycle.healthy = true;
        lifecycle.consecutiveFailures.set(0);
        lifecycle.lastErrorMessage = null;

        logger.info("Streaming datasource '{}' reconnected successfully, state=INITIALIZING "
                + "(initial data load will restart)", dataSourceName);
    }

    /**
     * Records a failed reconnection attempt. Increments the consecutive failure
     * counter. After {@link #BACKOFF_THRESHOLD} consecutive failures, exponential
     * backoff is applied to subsequent reconnection delays.
     *
     * @param dataSourceName the datasource name
     */
    public void recordReconnectionFailure(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null) {
            logger.warn("Reconnection failure reported for unregistered datasource '{}'", dataSourceName);
            return;
        }

        lifecycle.consecutiveFailures.incrementAndGet();
        lifecycle.healthy = false;

        logger.warn("Streaming datasource '{}' reconnection attempt failed "
                + "(consecutive failures: {})", dataSourceName, lifecycle.consecutiveFailures.get());
    }

    /**
     * Marks the initial data load as complete. Transitions state from
     * INITIALIZING to READY.
     *
     * @param dataSourceName the datasource name
     */
    public void handleInitialLoadComplete(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null) {
            logger.warn("Initial load complete reported for unregistered datasource '{}'", dataSourceName);
            return;
        }

        lifecycle.state = StreamingDataSourceState.READY;
        lifecycle.healthy = true;

        logger.info("Streaming datasource '{}' initial load complete, state=READY", dataSourceName);
    }

    /**
     * Calculates the delay before the next reconnection attempt using
     * exponential backoff.
     *
     * <p>For the first {@link #BACKOFF_THRESHOLD} attempts, returns
     * {@link #BASE_BACKOFF_DELAY}. After that, applies exponential backoff:
     * {@code baseDelay * 2^(attempt - BACKOFF_THRESHOLD)}, capped at
     * {@link #MAX_BACKOFF_DELAY}.</p>
     *
     * @param dataSourceName the datasource name
     * @return the delay before the next reconnection attempt
     */
    public Duration calculateNextReconnectDelay(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null) {
            return BASE_BACKOFF_DELAY;
        }

        return calculateBackoffDelay(lifecycle.consecutiveFailures.get());
    }

    /**
     * Calculates the backoff delay for a given number of consecutive failures.
     * Visible for testing.
     *
     * @param consecutiveFailures the number of consecutive failures
     * @return the calculated delay
     */
    static Duration calculateBackoffDelay(int consecutiveFailures) {
        if (consecutiveFailures < BACKOFF_THRESHOLD) {
            return BASE_BACKOFF_DELAY;
        }

        int exponent = consecutiveFailures - BACKOFF_THRESHOLD;
        // Guard against overflow: cap exponent at a reasonable value
        // 2^17 seconds ≈ 36 hours, well beyond MAX_BACKOFF_DELAY
        if (exponent > 17) {
            return MAX_BACKOFF_DELAY;
        }

        long delayMillis = BASE_BACKOFF_DELAY.toMillis() * (1L << exponent);
        Duration delay = Duration.ofMillis(delayMillis);

        return delay.compareTo(MAX_BACKOFF_DELAY) > 0 ? MAX_BACKOFF_DELAY : delay;
    }

    /**
     * Checks whether the datasource is registered for lifecycle management.
     *
     * @param dataSourceName the datasource name
     * @return true if registered
     */
    public boolean isRegistered(String dataSourceName) {
        return states.containsKey(dataSourceName);
    }

    /**
     * Clears all registered datasources. Used for testing and shutdown.
     */
    public void clear() {
        states.clear();
    }

    /**
     * Checks whether a datasource in INITIALIZING state has exceeded the
     * initial load timeout. If so, transitions it to ERROR and returns true.
     *
     * @param dataSourceName the datasource name
     * @return true if timeout was exceeded and state changed to ERROR, false otherwise
     */
    public boolean checkInitialLoadTimeout(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle == null || lifecycle.state != StreamingDataSourceState.INITIALIZING) {
            return false;
        }
        long elapsed = Duration.between(lifecycle.registeredAt, Instant.now()).toMillis();
        if (elapsed > initialLoadTimeoutMs) {
            logger.warn("Streaming datasource '{}' initial load timeout after {}ms (limit: {}ms)",
                    dataSourceName, elapsed, initialLoadTimeoutMs);
            lifecycle.state = StreamingDataSourceState.ERROR;
            lifecycle.lastErrorMessage = "Initial load timeout after " + elapsed + "ms";
            return true;
        }
        return false;
    }

    /**
     * Sets the timeout duration for initial load. If a datasource stays in
     * INITIALIZING state longer than this, it will be transitioned to ERROR.
     *
     * @param timeoutMs timeout in milliseconds
     */
    public void setInitialLoadTimeout(long timeoutMs) {
        this.initialLoadTimeoutMs = timeoutMs;
    }

    /**
     * Resets the registration time for a datasource by creating a new
     * {@link DatasourceLifecycleState} instance (since registeredAt is final/immutable).
     * Sets the state to INITIALIZING so the initial load timeout restarts.
     *
     * <p>Used after reconnection to give the datasource a fresh timeout window.</p>
     *
     * @param dataSourceName the datasource name
     */
    public void resetRegistrationTime(String dataSourceName) {
        DatasourceLifecycleState lifecycle = states.get(dataSourceName);
        if (lifecycle != null) {
            DatasourceLifecycleState newState = new DatasourceLifecycleState(dataSourceName);
            newState.state = StreamingDataSourceState.INITIALIZING;
            states.put(dataSourceName, newState);
        }
    }

    /**
     * Internal mutable state holder for a single streaming datasource's lifecycle.
     */
    private static class DatasourceLifecycleState {
        final String dataSourceName;
        final Instant registeredAt;
        volatile StreamingDataSourceState state;
        volatile boolean healthy;
        AtomicInteger consecutiveFailures;
        volatile String lastErrorMessage;

        DatasourceLifecycleState(String dataSourceName) {
            this.dataSourceName = dataSourceName;
            this.registeredAt = Instant.now();
            this.state = StreamingDataSourceState.INITIALIZING;
            this.healthy = true;
            this.consecutiveFailures = new AtomicInteger(0);
        }
    }
}
