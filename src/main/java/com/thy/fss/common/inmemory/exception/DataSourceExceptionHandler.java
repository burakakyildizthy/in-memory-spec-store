package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.datasource.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Utility class for handling DataSource exceptions and implementing fallback scenarios.
 * Provides methods for executing DataSource operations with automatic fallback chain traversal.
 */
public class DataSourceExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceExceptionHandler.class);

    /**
     * Executes a DataSource operation with fallback support.
     * If the primary operation fails, it will attempt to use the fallback DataSource chain.
     *
     * @param <T>               the type of data returned by the DataSource
     * @param primaryDataSource the primary DataSource to try first
     * @param operation         the operation to execute on the DataSource
     * @param dataSourceName    the name of the DataSource for logging purposes
     * @return CompletableFuture containing the result, or empty list if all DataSources fail
     */
    public static <T> CompletableFuture<List<T>> executeWithFallback(
            DataSource<T> primaryDataSource,
            DataSourceOperation<T> operation,
            String dataSourceName) {

        return CompletableFuture.supplyAsync(() -> {
            List<DataSource<T>> fallbackChain = buildFallbackChain(primaryDataSource);

            for (int i = 0; i < fallbackChain.size(); i++) {
                DataSource<T> currentDataSource = fallbackChain.get(i);
                String currentName = i == 0 ? dataSourceName : dataSourceName + "-fallback-" + i;

                try {
                    if (!currentDataSource.isHealthy()) {
                        logger.warn("DataSource '{}' is not healthy, skipping", currentName);
                        continue;
                    }

                    List<T> result = operation.execute(currentDataSource).get();

                    if (i > 0) {
                        logger.info("Successfully retrieved data from fallback DataSource '{}' after {} failures",
                                currentName, i);
                    }

                    return result;

                } catch (Exception e) {
                    logger.error("Failed to fetch data from DataSource '{}': {}", currentName, e.getMessage());

                    if (isConnectionException(e)) {
                        logger.warn("Connection issue detected for DataSource '{}', trying fallback", currentName);
                    }

                    // Continue to next fallback
                }
            }

            logger.error("All DataSources in fallback chain failed for '{}'", dataSourceName);
            return new ArrayList<>(); // Return empty list on complete failure
        });
    }

    /**
     * Wraps a DataSource operation to handle exceptions and convert them to appropriate DataSourceExceptions.
     *
     * @param <T>            the type of data returned by the operation
     * @param operation      the operation to wrap
     * @param dataSourceName the name of the DataSource for error context
     * @return a wrapped operation that handles exceptions
     */
    public static <T> Supplier<T> wrapOperation(Supplier<T> operation, String dataSourceName) {
        return () -> {
            try {
                return operation.get();
            } catch (Exception e) {
                if (isConnectionException(e)) {
                    throw new DataSourceConnectionException(
                            String.format("Connection failed for DataSource '%s': %s", dataSourceName, e.getMessage()), e);
                } else {
                    throw new DataSourceException(
                            String.format("Operation failed for DataSource '%s': %s", dataSourceName, e.getMessage()), e);
                }
            }
        };
    }

    /**
     * Checks if an exception indicates a connection-related issue.
     *
     * @param exception the exception to check
     * @return true if the exception indicates a connection issue
     */
    public static boolean isConnectionException(Throwable exception) {
        if (exception instanceof DataSourceConnectionException) {
            return true;
        }

        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase(java.util.Locale.ENGLISH);
            return lowerMessage.contains("connection") ||
                    lowerMessage.contains("timeout") ||
                    lowerMessage.contains("network") ||
                    lowerMessage.contains("unreachable") ||
                    lowerMessage.contains("refused") ||
                    lowerMessage.contains("authentication") ||
                    lowerMessage.contains("unauthorized");
        }

        return false;
    }

    /**
     * Builds a fallback chain from a DataSource by traversing its fallback DataSources.
     *
     * @param <T>               the type of data handled by the DataSource
     * @param primaryDataSource the primary DataSource
     * @return a list of DataSources in fallback order
     */
    private static <T> List<DataSource<T>> buildFallbackChain(DataSource<T> primaryDataSource) {
        List<DataSource<T>> chain = new ArrayList<>();
        DataSource<T> current = primaryDataSource;

        while (current != null) {
            chain.add(current);
            Optional<DataSource<T>> fallback = current.getFallbackDataSource();
            current = fallback.orElse(null);
        }

        return chain;
    }

    /**
     * Functional interface for DataSource operations that can be executed with fallback support.
     *
     * @param <T> the type of data returned by the operation
     */
    @FunctionalInterface
    public interface DataSourceOperation<T> {
        CompletableFuture<List<T>> execute(DataSource<T> dataSource);
    }
}