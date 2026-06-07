package com.thy.fss.common.inmemory.engine.sync;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;

/**
 * Tracks synchronization metadata for a datasource including health status,
 * sync intervals, and retry logic.
 *
 * <p>This class is used by DataSynchronizationEngine to manage datasource
 * synchronization scheduling and health monitoring.</p>
 */
public class DataSourceSyncMetadata {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceSyncMetadata.class);

    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final Duration HEALTH_CHECK_RETRY_INTERVAL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private final String dataSourceName;
    private final Duration syncInterval;
    private final Duration readTimeout;

    private LocalDateTime lastSyncTime;
    private LocalDateTime nextSyncTime;
    private LocalDateTime lastHealthCheckTime;

    private int consecutiveFailures;
    private boolean isHealthy;
    private String lastErrorMessage;

    // --- Streaming datasource fields ---
    private StreamingDataSourceState streamingState;
    private boolean isStreamingDataSource;
    private int reconnectAttempts;

    /**
     * Creates new sync metadata for a datasource with default read timeout.
     *
     * @param dataSourceName the datasource name
     * @param syncInterval   the synchronization interval
     */
    public DataSourceSyncMetadata(String dataSourceName, Duration syncInterval) {
        this(dataSourceName, syncInterval, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates new sync metadata for a datasource with custom read timeout.
     *
     * @param dataSourceName the datasource name
     * @param syncInterval   the synchronization interval
     * @param readTimeout    the timeout for datasource read operations
     */
    public DataSourceSyncMetadata(String dataSourceName, Duration syncInterval, Duration readTimeout) {
        this.dataSourceName = dataSourceName;
        this.syncInterval = syncInterval;
        this.readTimeout = readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT;
        this.isHealthy = true;
        this.consecutiveFailures = 0;
        this.lastSyncTime = null;
        this.nextSyncTime = LocalDateTime.now(); // Sync immediately on first run
        this.lastHealthCheckTime = null;

        // Streaming defaults — batch datasources get sensible defaults
        this.isStreamingDataSource = false;
        this.streamingState = null;
        this.reconnectAttempts = 0;
    }

    /**
     * Gets the datasource name.
     *
     * @return the datasource name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Gets the synchronization interval.
     *
     * @return the sync interval
     */
    public Duration getSyncInterval() {
        return syncInterval;
    }

    /**
     * Gets the read timeout for datasource operations.
     *
     * @return the read timeout
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Gets the last successful sync time.
     *
     * @return the last sync time, or null if never synced
     */
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    /**
     * Gets the next scheduled sync time.
     *
     * @return the next sync time
     */
    public LocalDateTime getNextSyncTime() {
        return nextSyncTime;
    }

    /**
     * Gets the number of consecutive failures.
     *
     * @return the consecutive failure count
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Checks if the datasource is healthy.
     *
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        return isHealthy;
    }

    /**
     * Gets the last error message.
     *
     * @return the last error message, or null if no errors
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Checks if it's time to sync this datasource.
     *
     * @return true if current time is past the next sync time
     */
    public boolean shouldSync() {
        return LocalDateTime.now().isAfter(nextSyncTime);
    }

    /**
     * Checks if a health check retry should be attempted.
     *
     * @return true if enough time has passed since last health check
     */
    public boolean shouldRetryHealthCheck() {
        if (isHealthy) {
            return false;
        }

        if (lastHealthCheckTime == null) {
            return true;
        }

        Duration timeSinceLastCheck = Duration.between(lastHealthCheckTime, LocalDateTime.now());
        return timeSinceLastCheck.compareTo(HEALTH_CHECK_RETRY_INTERVAL) >= 0;
    }

    /**
     * Records a successful synchronization.
     * Resets failure count and updates sync times.
     */
    public void recordSuccess() {
        this.lastSyncTime = LocalDateTime.now();
        this.nextSyncTime = lastSyncTime.plus(syncInterval);
        this.consecutiveFailures = 0;
        this.isHealthy = true;
        this.lastErrorMessage = null;
        this.lastHealthCheckTime = LocalDateTime.now();
    }

    /**
     * Records a failed synchronization.
     * Increments failure count and marks as unhealthy if threshold exceeded.
     *
     * @param errorMessage the error message
     */
    public void recordFailure(String errorMessage) {
        this.consecutiveFailures++;
        this.lastErrorMessage = errorMessage;
        this.lastHealthCheckTime = LocalDateTime.now();

        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            this.isHealthy = false;
        }

        // Still schedule next sync attempt (with backoff if unhealthy)
        Duration backoff = isHealthy ? syncInterval : HEALTH_CHECK_RETRY_INTERVAL;
        this.nextSyncTime = LocalDateTime.now().plus(backoff);
    }

    /**
     * Manually marks the datasource as healthy.
     * Used when health check succeeds after being unhealthy.
     */
    public void markHealthy() {
        this.isHealthy = true;
        this.consecutiveFailures = 0;
        this.lastErrorMessage = null;
        this.lastHealthCheckTime = LocalDateTime.now();
    }

    /**
     * Manually marks the datasource as unhealthy.
     *
     * @param errorMessage the error message
     */
    public void markUnhealthy(String errorMessage) {
        this.isHealthy = false;
        this.lastErrorMessage = errorMessage;
        this.lastHealthCheckTime = LocalDateTime.now();
    }

    /**
     * Updates the next sync time.
     * Used for manual scheduling adjustments.
     *
     * @param nextSyncTime the new next sync time
     */
    public void updateNextSyncTime(LocalDateTime nextSyncTime) {
        this.nextSyncTime = nextSyncTime;
    }

    // --- Streaming datasource methods ---

    /**
     * Gets the streaming lifecycle state.
     *
     * @return the streaming state, or null for batch datasources
     */
    public StreamingDataSourceState getStreamingState() {
        return streamingState;
    }

    /**
     * Sets the streaming lifecycle state.
     *
     * @param streamingState the new streaming state
     */
    public void setStreamingState(StreamingDataSourceState streamingState) {
        this.streamingState = streamingState;
    }

    /**
     * Checks if this metadata belongs to a streaming datasource.
     *
     * @return true if streaming, false if batch
     */
    public boolean isStreamingDataSource() {
        return isStreamingDataSource;
    }

    /**
     * Sets whether this metadata belongs to a streaming datasource.
     *
     * @param streamingDataSource true for streaming, false for batch
     */
    public void setStreamingDataSource(boolean streamingDataSource) {
        this.isStreamingDataSource = streamingDataSource;
    }

    /**
     * Gets the number of reconnection attempts for this streaming datasource.
     *
     * @return the reconnect attempt count
     */
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    /**
     * Sets the number of reconnection attempts.
     *
     * @param reconnectAttempts the reconnect attempt count
     */
    public void setReconnectAttempts(int reconnectAttempts) {
        this.reconnectAttempts = reconnectAttempts;
    }

    /**
     * Updates the streaming state with logging. Reports health and readiness
     * changes through the existing metadata structure.
     *
     * @param newState the new streaming state
     */
    public void updateStreamingState(StreamingDataSourceState newState) {
        StreamingDataSourceState previousState = this.streamingState;
        this.streamingState = newState;

        switch (newState) {
            case READY:
                this.isHealthy = true;
                this.lastErrorMessage = null;
                logger.info("Streaming datasource '{}' state changed: {} -> READY (healthy and ready)",
                        dataSourceName, previousState);
                break;
            case ERROR:
                this.isHealthy = false;
                logger.warn("Streaming datasource '{}' state changed: {} -> ERROR (unhealthy)",
                        dataSourceName, previousState);
                break;
            case INITIALIZING:
                // During INITIALIZING, datasource is connected but not yet ready
                logger.info("Streaming datasource '{}' state changed: {} -> INITIALIZING (not ready)",
                        dataSourceName, previousState);
                break;
            default:
                break;
        }
    }

    /**
     * Increments the reconnect attempt counter by one.
     */
    public void incrementReconnectAttempts() {
        this.reconnectAttempts++;
    }

    /**
     * Resets the reconnect attempt counter to zero.
     * Called after a successful reconnection.
     */
    public void resetReconnectAttempts() {
        this.reconnectAttempts = 0;
    }

    @Override
    public String toString() {
        if (isStreamingDataSource) {
            return String.format(
                    "DataSourceSyncMetadata[name=%s, streaming=true, streamingState=%s, healthy=%s, reconnectAttempts=%d]",
                    dataSourceName, streamingState, isHealthy, reconnectAttempts
            );
        }
        return String.format(
                "DataSourceSyncMetadata[name=%s, healthy=%s, failures=%d, lastSync=%s, nextSync=%s]",
                dataSourceName, isHealthy, consecutiveFailures, lastSyncTime, nextSyncTime
        );
    }
}
