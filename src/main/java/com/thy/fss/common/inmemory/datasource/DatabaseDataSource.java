package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Database-backed DataSource implementation using JDBC.
 * Supports both fetchAll() and fetchAllById() operations with proper SQL generation.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class DatabaseDataSource<T> implements com.thy.fss.common.inmemory.datasource.DataSource<T> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDataSource.class);

    private final String name;
    private final Class<T> entityType;
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String idColumnName;
    private final RowMapper<T> rowMapper;
    private com.thy.fss.common.inmemory.datasource.DataSource<T> fallbackDataSource;

    /**
     * Creates a new DatabaseDataSource.
     *
     * @param name         the name of this DataSource for identification
     * @param entityType   the type of entities this DataSource provides
     * @param dataSource   the JDBC DataSource to use for database connections
     * @param tableName    the name of the database table to query
     * @param idColumnName the name of the ID column for filtering
     * @param rowMapper    the RowMapper to convert ResultSet rows to entities
     */
    public DatabaseDataSource(String name, Class<T> entityType, DataSource dataSource,
                              String tableName, String idColumnName, RowMapper<T> rowMapper) {
        this.name = Objects.requireNonNull(name, "DataSource name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "DataSource cannot be null"));
        this.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
        this.idColumnName = Objects.requireNonNull(idColumnName, "ID column name cannot be null");
        this.rowMapper = Objects.requireNonNull(rowMapper, "RowMapper cannot be null");

        logger.info("Created DatabaseDataSource '{}' for entity type {} with table '{}'",
                name, entityType.getSimpleName(), tableName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getEntityType() {
        return entityType;
    }

    @Override
    public CompletableFuture<List<T>> fetchAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM " + tableName;
                logger.debug("Executing fetchAll query: {}", sql);

                List<T> results = jdbcTemplate.query(sql, rowMapper);
                logger.info("Successfully fetched {} records from table '{}'", results.size(), tableName);

                return results;

            } catch (Exception e) {
                logger.error("Failed to fetch all data from table '{}': {}", tableName, e.getMessage(), e);
                throw new DataSourceConnectionException(
                        "Failed to fetch all data from database table '" + tableName + "'", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<T>> fetchAllById(Collection<Object> ids) {
        return CompletableFuture.supplyAsync(() -> {
            if (ids == null || ids.isEmpty()) {
                logger.debug("No IDs provided for fetchAllById, returning empty list");
                return new ArrayList<>();
            }

            try {
                // Create IN clause with proper parameter binding
                String placeholders = ids.stream()
                        .map(id -> "?")
                        .collect(Collectors.joining(", "));

                String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " IN (" + placeholders + ")";
                logger.debug("Executing fetchAllById query: {} with {} IDs", sql, ids.size());

                // Convert IDs to array for parameter binding
                Object[] parameters = ids.toArray();

                List<T> results = jdbcTemplate.query(sql, rowMapper, parameters);
                logger.debug("Successfully fetched {} records from table '{}' for {} IDs",
                        results.size(), tableName, ids.size());

                return results;

            } catch (Exception e) {
                logger.error("Failed to fetch data by IDs from table '{}': {}", tableName, e.getMessage(), e);
                throw new DataSourceConnectionException(
                        "Failed to fetch data by IDs from database table '" + tableName + "'", e);
            }
        });
    }

    @Override
    public boolean isHealthy() {
        try {
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource == null) {
                logger.warn("Health check failed for DatabaseDataSource '{}': DataSource is null", name);
                return false;
            }
            
            // Test connection and execute a query to verify database is accessible
            try (Connection connection = dataSource.getConnection()) {
                if (connection == null || connection.isClosed()) {
                    logger.warn("Health check failed for DatabaseDataSource '{}': Connection is null or closed", name);
                    return false;
                }
                
                // Verify connection is valid with a timeout
                if (!connection.isValid(1)) {
                    logger.warn("Health check failed for DatabaseDataSource '{}': Connection is not valid", name);
                    return false;
                }
                
                // Execute a simple query to ensure database is actually accessible
                // This will fail if the database has been shut down
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                
                logger.debug("Health check passed for DatabaseDataSource '{}'", name);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Health check failed for DatabaseDataSource '{}': {}", name, e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        logger.info("Closing DatabaseDataSource '{}'", name);
        // JdbcTemplate doesn't need explicit closing, but we can log the closure
        // The underlying DataSource should be managed by the application context
    }

    @Override
    public Optional<com.thy.fss.common.inmemory.datasource.DataSource<T>> getFallbackDataSource() {
        return Optional.ofNullable(fallbackDataSource);
    }

    @Override
    public void setFallbackDataSource(com.thy.fss.common.inmemory.datasource.DataSource<T> fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;
        logger.info("Set fallback DataSource for '{}': {}",
                name, fallbackDataSource != null ? fallbackDataSource.getName() : "null");
    }

    /**
     * Gets the underlying JdbcTemplate for advanced operations.
     *
     * @return the JdbcTemplate instance
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Gets the table name being queried.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Gets the ID column name used for filtering.
     *
     * @return the ID column name
     */
    public String getIdColumnName() {
        return idColumnName;
    }
}