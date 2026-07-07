package com.thy.fss.common.inmemory.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * File-based DataSource implementation supporting CSV and JSON formats.
 * Provides fallback data source capabilities with optional file watching for change detection.
 *
 * @param <T> The type of entities this DataSource provides
 */
public class FileDataSource<T> implements DataSource<T> {

    private static final Logger logger = LoggerFactory.getLogger(FileDataSource.class);
    private final String name;
    private final Class<T> entityType;
    private final Path filePath;
    private final FileFormat format;
    private final ObjectMapper objectMapper;
    private final Function<String[], T> csvMapper;
    private final String idFieldName;
    private DataSource<T> fallbackDataSource;
    private WatchService watchService;
    private volatile List<T> cachedData;
    private volatile long lastModified;

    /**
     * Creates a new FileDataSource for JSON files.
     *
     * @param name        the name of this DataSource for identification
     * @param entityType  the type of entities this DataSource provides
     * @param filePath    the path to the data file
     * @param idFieldName the name of the ID field for filtering
     */
    public FileDataSource(String name, Class<T> entityType, Path filePath, String idFieldName) {
        this(name, entityType, filePath, FileFormat.JSON, idFieldName, null, new ObjectMapper());
    }

    /**
     * Creates a new FileDataSource for CSV files.
     *
     * @param name        the name of this DataSource for identification
     * @param entityType  the type of entities this DataSource provides
     * @param filePath    the path to the data file
     * @param idFieldName the name of the ID field for filtering
     * @param csvMapper   function to convert CSV row (String array) to entity
     */
    public FileDataSource(String name, Class<T> entityType, Path filePath, String idFieldName,
                          Function<String[], T> csvMapper) {
        this(name, entityType, filePath, FileFormat.CSV, idFieldName, csvMapper, new ObjectMapper());
    }

    /**
     * Creates a new FileDataSource with full configuration.
     *
     * @param name         the name of this DataSource for identification
     * @param entityType   the type of entities this DataSource provides
     * @param filePath     the path to the data file
     * @param format       the file format (JSON or CSV)
     * @param idFieldName  the name of the ID field for filtering
     * @param csvMapper    function to convert CSV row to entity (required for CSV format)
     * @param objectMapper the ObjectMapper for JSON parsing
     */
    public FileDataSource(String name, Class<T> entityType, Path filePath, FileFormat format,
                          String idFieldName, Function<String[], T> csvMapper, ObjectMapper objectMapper) {
        this.name = Objects.requireNonNull(name, "DataSource name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.format = Objects.requireNonNull(format, "File format cannot be null");
        this.idFieldName = Objects.requireNonNull(idFieldName, "ID field name cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");

        if (format == FileFormat.CSV && csvMapper == null) {
            throw new IllegalArgumentException("CSV mapper is required for CSV format");
        }
        this.csvMapper = csvMapper;

        // Initialize file watching
        initializeFileWatching();

        // Load initial data
        loadDataFromFile();

        logger.info("Created FileDataSource '{}' for entity type {} with file '{}' (format: {})",
                name, entityType.getSimpleName(), filePath, format);
    }

    private void initializeFileWatching() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path parentDir = filePath.getParent();
            if (parentDir != null && Files.exists(parentDir)) {
                parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                logger.debug("Initialized file watching for directory: {}", parentDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to initialize file watching for '{}': {}", filePath, e.getMessage());
            // Continue without file watching
        }
    }

    private void loadDataFromFile() {
        try {
            if (!Files.exists(filePath)) {
                logger.warn("File does not exist: {}", filePath);
                this.cachedData = new ArrayList<>();
                this.lastModified = 0;
                return;
            }

            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
            if (cachedData != null && currentModified == lastModified) {
                logger.debug("File has not been modified, using cached data");
                return;
            }

            List<T> data = switch (format) {
                case JSON -> loadJsonData();
                case CSV -> loadCsvData();
            };

            this.cachedData = data;
            this.lastModified = currentModified;

            logger.info("Loaded {} records from file '{}' (format: {})", data.size(), filePath, format);

        } catch (IOException e) {
            logger.error("Failed to load data from file '{}': {}", filePath, e.getMessage(), e);
            if (cachedData == null) {
                this.cachedData = new ArrayList<>();
            }
        }
    }

    private List<T> loadJsonData() throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            String content = new String(inputStream.readAllBytes());

            if (content.trim().isEmpty()) {
                return new ArrayList<>();
            }

            if (content.trim().startsWith("[")) {
                // Array format
                return objectMapper.readValue(content,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, entityType));
            } else {
                // Single object format
                T singleObject = objectMapper.readValue(content, entityType);
                return Collections.singletonList(singleObject);
            }
        }
    }

    private List<T> loadCsvData() throws IOException {
        List<T> data = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    // Skip header line if it doesn't look like data
                    if (line.toLowerCase().contains("id") || line.toLowerCase().contains("name")) {
                        continue;
                    }
                }

                String[] fields = line.split(",");
                // Trim whitespace from fields
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }

                try {
                    T entity = csvMapper.apply(fields);
                    data.add(entity);
                } catch (Exception e) {
                    logger.warn("Failed to parse CSV line '{}': {}", line, e.getMessage());
                }
            }
        }

        return data;
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
                // Check for file changes
                checkForFileChanges();

                // Return cached data
                List<T> result = new ArrayList<>(cachedData);
                logger.debug("Returning {} records from FileDataSource '{}'", result.size(), name);

                return result;

            } catch (Exception e) {
                logger.error("Failed to fetch all data from file '{}': {}", filePath, e.getMessage(), e);
                throw new DataSourceConnectionException(
                        "Failed to fetch all data from file '" + filePath + "'", e);
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
                // Check for file changes
                checkForFileChanges();

                // Filter cached data by IDs using reflection
                List<T> results = cachedData.stream()
                        .filter(entity -> {
                            Object entityId = extractEntityId(entity);
                            return entityId != null && ids.contains(entityId);
                        })
                        .toList();

                logger.debug("Returning {} records from FileDataSource '{}' for {} IDs",
                        results.size(), name, ids.size());

                return results;

            } catch (IllegalStateException e) {
                // Re-throw configuration errors without wrapping
                throw e;
            } catch (Exception e) {
                logger.error("Failed to fetch data by IDs from file '{}': {}", filePath, e.getMessage(), e);
                throw new DataSourceConnectionException(
                        "Failed to fetch data by IDs from file '" + filePath + "'", e);
            }
        });
    }

    private void checkForFileChanges() {
        if (watchService != null) {
            WatchKey key = watchService.poll();
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context().toString().equals(filePath.getFileName().toString())) {
                        logger.debug("File change detected for '{}', reloading data", filePath);
                        loadDataFromFile();
                        break;
                    }
                }
                key.reset();
            }
        } else {
            // Fallback: check file modification time
            try {
                long currentModified = Files.getLastModifiedTime(filePath).toMillis();
                if (currentModified > lastModified) {
                    logger.debug("File modification detected for '{}', reloading data", filePath);
                    loadDataFromFile();
                }
            } catch (IOException e) {
                logger.debug("Failed to check file modification time: {}", e.getMessage());
            }
        }
    }

    /**
     * Extracts the ID from an entity using the Identifiable interface.
     * This replaces reflection-based ID extraction for type safety and performance.
     *
     * @param entity the entity to extract ID from
     * @return the entity's ID, or the entity itself if not Identifiable
     */
    private Object extractEntityId(T entity) {
        if (entity == null) {
            return null;
        }

        // Use Identifiable interface if available (preferred approach)
        if (entity instanceof com.thy.fss.common.inmemory.entity.Identifiable) {
            return ((com.thy.fss.common.inmemory.entity.Identifiable<?>) entity).getIdentity();
        }

        throw new IllegalStateException("Entity of type " + entity.getClass().getName() +
                " does not implement Identifiable interface. Cannot extract ID. Implement Identifiable interface !");
    }

    @Override
    public boolean isHealthy() {
        try {
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (Exception e) {
            logger.warn("Health check failed for FileDataSource '{}': {}", name, e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        logger.info("Closing FileDataSource '{}'", name);

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Failed to close watch service: {}", e.getMessage());
            }
        }
    }

    @Override
    public Optional<DataSource<T>> getFallbackDataSource() {
        return Optional.ofNullable(fallbackDataSource);
    }

    @Override
    public void setFallbackDataSource(DataSource<T> fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;
        logger.info("Set fallback DataSource for '{}': {}",
                name, fallbackDataSource != null ? fallbackDataSource.getName() : "null");
    }

    /**
     * Gets the file path being monitored.
     *
     * @return the file path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Gets the file format.
     *
     * @return the file format
     */
    public FileFormat getFormat() {
        return format;
    }

    /**
     * Gets the ID field name used for filtering.
     *
     * @return the ID field name
     */
    public String getIdFieldName() {
        return idFieldName;
    }

    /**
     * Forces a reload of data from the file.
     */
    public void forceReload() {
        logger.info("Forcing reload of data from file '{}'", filePath);
        loadDataFromFile();
    }

    public enum FileFormat {
        JSON, CSV
    }
}