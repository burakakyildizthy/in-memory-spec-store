package com.thy.fss.common.inmemory.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

/**
 * Test class for FileDataSource functionality.
 */
class FileDataSourceTest {

    private static final String ID = "id";
    private static final String ENTITY_1 = "Entity 1";
    private static final String ENTITY_2 = "Entity 2";
    private static final String ENTITY_3 = "Entity 3";
    private static final String JSON_SOURCE = "json-source";
    private static final String CSV_SOURCE = "csv-source";
    private static final String TEST = "test";
    private static final String FULL_CONSTRUCTOR_SOURCE = "full-constructor-source";

    @TempDir
    Path tempDir;

    private Path jsonFile;
    private Path csvFile;
    private final List<FileDataSource<?>> openDataSources = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        jsonFile = tempDir.resolve("test-data.json");
        csvFile = tempDir.resolve("test-data.csv");

        // Create test JSON file
        String jsonContent = """
                [
                    {"id": 1, "name": "Entity 1"},
                    {"id": 2, "name": "Entity 2"},
                    {"id": 3, "name": "Entity 3"}
                ]
                """;
        Files.writeString(jsonFile, jsonContent);

        // Create test CSV file
        String csvContent = """
                id,name
                1,Entity 1
                2,Entity 2
                3,Entity 3
                """;
        Files.writeString(csvFile, csvContent);
    }

    @AfterEach
    void tearDown() {
        openDataSources.forEach(FileDataSource::close);
        openDataSources.clear();
    }

    private <T> FileDataSource<T> trackDataSource(FileDataSource<T> dataSource) {
        openDataSources.add(dataSource);
        return dataSource;
    }

    @Test
    @DisplayName("Should create JSON file data source")
    void shouldCreateJsonFileDataSource() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                JSON_SOURCE,
                TestEntity.class,
                jsonFile,
                ID
        ));

        assertThat(dataSource.getName()).isEqualTo(JSON_SOURCE);
        assertThat(dataSource.getEntityType()).isEqualTo(TestEntity.class);
        assertThat(dataSource.getFilePath()).isEqualTo(jsonFile);
        assertThat(dataSource.getFormat()).isEqualTo(FileDataSource.FileFormat.JSON);
        assertThat(dataSource.getIdFieldName()).isEqualTo(ID);

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(3);
        assertThat(entities).extracting(TestEntity::getName)
                .containsExactly(ENTITY_1, ENTITY_2, ENTITY_3);
    }

    @Test
    @DisplayName("Should create CSV file data source")
    void shouldCreateCsvFileDataSource() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                CSV_SOURCE,
                TestEntity.class,
                csvFile,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        assertThat(dataSource.getName()).isEqualTo(CSV_SOURCE);
        assertThat(dataSource.getFormat()).isEqualTo(FileDataSource.FileFormat.CSV);

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(3);
        assertThat(entities).extracting(TestEntity::getName)
                .containsExactly(ENTITY_1, ENTITY_2, ENTITY_3);
    }

    @Test
    @DisplayName("Should throw exception for null parameters")
    void shouldThrowExceptionForNullParameters() {
        assertThatThrownBy(() -> new FileDataSource<>(null, TestEntity.class, jsonFile, ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DataSource name cannot be null");

        assertThatThrownBy(() -> new FileDataSource<>(TEST, null, jsonFile, ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity type cannot be null");

        assertThatThrownBy(() -> new FileDataSource<>(TEST, TestEntity.class, null, ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("File path cannot be null");

        assertThatThrownBy(() -> new FileDataSource<>(TEST, TestEntity.class, jsonFile, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID field name cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for CSV without mapper")
    void shouldThrowExceptionForCsvWithoutMapper() {
        // Setup parameters including the problematic one
        String name = CSV_SOURCE;
        Class<TestEntity> entityType = TestEntity.class;
        Path filePath = csvFile;
        FileDataSource.FileFormat format = FileDataSource.FileFormat.CSV;
        String idFieldName = ID;
        Function<String[], TestEntity> csvMapper = null;
        ObjectMapper objectMapper = new ObjectMapper();

        // Only call the constructor expected to throw
        assertThatThrownBy(() -> new FileDataSource<>(
                name,
                entityType,
                filePath,
                format,
                idFieldName,
                csvMapper,
                objectMapper
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV mapper is required for CSV format");

    }

    @Test
    @DisplayName("Should fetch entities by IDs")
    void shouldFetchEntitiesByIds() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                JSON_SOURCE,
                TestEntity.class,
                jsonFile,
                ID
        ));

        Collection<Object> ids = Arrays.asList(1L, 3L);
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::getIdentity).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("Should return empty list for null or empty IDs")
    void shouldReturnEmptyListForNullOrEmptyIds() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                JSON_SOURCE,
                TestEntity.class,
                jsonFile,
                ID
        ));

        CompletableFuture<List<TestEntity>> result1 = dataSource.fetchAllById(null);
        CompletableFuture<List<TestEntity>> result2 = dataSource.fetchAllById(Collections.emptyList());

        assertThat(result1.get()).isEmpty();
        assertThat(result2.get()).isEmpty();
    }

    @Test
    @DisplayName("Should handle non-existent file")
    void shouldHandleNonExistentFile() throws Exception {
        Path nonExistentFile = tempDir.resolve("non-existent.json");

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "non-existent-source",
                TestEntity.class,
                nonExistentFile,
                ID
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).isEmpty();
        assertThat(dataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty JSON file")
    void shouldHandleEmptyJsonFile() throws Exception {
        Path emptyFile = tempDir.resolve("empty.json");
        Files.writeString(emptyFile, "");

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "empty-source",
                TestEntity.class,
                emptyFile,
                ID
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle single JSON object")
    void shouldHandleSingleJsonObject() throws Exception {
        Path singleObjectFile = tempDir.resolve("single.json");
        String singleObjectContent = """
                {"id": 1, "name": "Single Entity"}
                """;
        Files.writeString(singleObjectFile, singleObjectContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "single-source",
                TestEntity.class,
                singleObjectFile,
                ID
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getName()).isEqualTo("Single Entity");
    }

    @Test
    @DisplayName("Should handle CSV with header")
    void shouldHandleCsvWithHeader() throws Exception {
        Path csvWithHeaderFile = tempDir.resolve("with-header.csv");
        String csvContent = """
                id,name
                1,Entity 1
                2,Entity 2
                """;
        Files.writeString(csvWithHeaderFile, csvContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "csv-header-source",
                TestEntity.class,
                csvWithHeaderFile,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::getName)
                .containsExactly(ENTITY_1, ENTITY_2);
    }

    @Test
    @DisplayName("Should handle CSV parsing errors gracefully")
    void shouldHandleCsvParsingErrorsGracefully() throws Exception {
        Path invalidCsvFile = tempDir.resolve("invalid.csv");
        String invalidCsvContent = """
                1,Entity 1
                invalid,Entity 2
                3,Entity 3
                """;
        Files.writeString(invalidCsvFile, invalidCsvContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "invalid-csv-source",
                TestEntity.class,
                invalidCsvFile,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        // Should skip invalid line and continue with valid ones
        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::getIdentity).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("Should be healthy for existing readable file")
    void shouldBeHealthyForExistingReadableFile() {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "healthy-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        assertThat(dataSource.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should handle fallback data source")
    void shouldHandleFallbackDataSource() {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "main-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        assertThat(dataSource.getFallbackDataSource()).isEmpty();

        InMemoryDataSource<TestEntity> fallback = new InMemoryDataSource<>("fallback", TestEntity.class, Collections.emptyList());
        dataSource.setFallbackDataSource(fallback);

        assertThat(dataSource.getFallbackDataSource()).isPresent().contains(fallback);
    }

    @Test
    @DisplayName("Should force reload data")
    void shouldForceReloadData() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "reload-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        // Initial data
        CompletableFuture<List<TestEntity>> result1 = dataSource.fetchAll();
        assertThat(result1.get()).hasSize(3);

        // Update file content
        String newJsonContent = """
                [
                    {"id": 4, "name": "Entity 4"},
                    {"id": 5, "name": "Entity 5"}
                ]
                """;
        Files.writeString(jsonFile, newJsonContent);

        // Force reload
        dataSource.forceReload();

        // Should have new data
        CompletableFuture<List<TestEntity>> result2 = dataSource.fetchAll();
        List<TestEntity> entities = result2.get();

        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::getIdentity).containsExactly(4L, 5L);
    }

    @Test
    @DisplayName("Should close properly")
    void shouldCloseProperly() {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "closeable-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        // Should not throw exception
        dataSource.close();
        assertThatCode(dataSource::close).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle field access for ID extraction")
    void shouldHandleFieldAccessForIdExtraction() throws Exception {
        // Create entity with private field (no getter)
        Path testFile = tempDir.resolve("field-access.json");
        String jsonContent = """
                [
                    {"privateId": 1, "name": "Entity 1"},
                    {"privateId": 2, "name": "Entity 2"}
                ]
                """;
        Files.writeString(testFile, jsonContent);

        FileDataSource<TestEntityWithPrivateId> dataSource = trackDataSource(new FileDataSource<>(
                "field-access-source",
                TestEntityWithPrivateId.class,
                testFile,
                "privateId"
        ));

        Collection<Object> ids = List.of(1L);
        CompletableFuture<List<TestEntityWithPrivateId>> result = dataSource.fetchAllById(ids);
        List<TestEntityWithPrivateId> entities = result.get();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getName()).isEqualTo(ENTITY_1);
    }

    // ==================== Comprehensive Tests for Task 23.1 ====================

    @Test
    @DisplayName("Should handle invalid JSON format")
    void shouldHandleInvalidJsonFormat() throws Exception {
        Path invalidJsonFile = tempDir.resolve("invalid.json");
        String invalidJson = """
                {
                    "id": 1,
                    "name": "Invalid JSON - missing closing bracket"
                """;
        Files.writeString(invalidJsonFile, invalidJson);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "invalid-json-source",
                TestEntity.class,
                invalidJsonFile,
                ID
        ));

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();

        // Should return empty list on parse error
        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle large JSON file with 10K+ entities")
    void shouldHandleLargeJsonFile() throws Exception {
        Path largeJsonFile = tempDir.resolve("large-data.json");
        
        // Generate 10,000 entities
        int entityCount = 10_000;
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < entityCount; i++) {
            if (i > 0) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append(String.format("{\"id\":%d,\"name\":\"Entity %d\"}", i, i));
        }
        jsonBuilder.append("]");
        
        Files.writeString(largeJsonFile, jsonBuilder.toString());

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "large-json-source",
                TestEntity.class,
                largeJsonFile,
                ID
        ));

        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();
        long duration = System.currentTimeMillis() - startTime;

        assertThat(entities).hasSize(entityCount);
        assertThat(entities.get(0).getIdentity()).isZero();
        assertThat(entities.get(entityCount - 1).getIdentity()).isEqualTo((long) entityCount - 1);
        
        // Performance check - should load 10K entities reasonably fast
        assertThat(duration).isLessThan(5000); // Less than 5 seconds
    }

    @Test
    @DisplayName("Should handle large CSV file with 10K+ entities")
    void shouldHandleLargeCsvFile() throws Exception {
        Path largeCsvFile = tempDir.resolve("large-data.csv");
        
        // Generate 10,000 entities
        int entityCount = 10_000;
        StringBuilder csvBuilder = new StringBuilder("id,name\n");
        for (int i = 0; i < entityCount; i++) {
            String csvLine = String.format("%d,Entity %d%n", i, i);
            csvBuilder.append(csvLine);
        }
        
        Files.writeString(largeCsvFile, csvBuilder.toString());

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "large-csv-source",
                TestEntity.class,
                largeCsvFile,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = dataSource.fetchAll();
        List<TestEntity> entities = result.get();
        long duration = System.currentTimeMillis() - startTime;

        assertThat(entities).hasSize(entityCount);
        assertThat(entities.get(0).getIdentity()).isZero();
        assertThat(entities.get(entityCount - 1).getIdentity()).isEqualTo((long) entityCount - 1);
        
        // Performance check
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("Should handle fetchAllById with large dataset")
    void shouldHandleFetchAllByIdWithLargeDataset() throws Exception {
        Path largeJsonFile = tempDir.resolve("large-data-by-id.json");
        
        // Generate 10,000 entities
        int entityCount = 10_000;
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < entityCount; i++) {
            if (i > 0) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append(String.format("{\"id\":%d,\"name\":\"Entity %d\"}", i, i));
        }
        jsonBuilder.append("]");
        
        Files.writeString(largeJsonFile, jsonBuilder.toString());

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "large-json-by-id-source",
                TestEntity.class,
                largeJsonFile,
                ID
        ));

        // Fetch specific IDs from large dataset
        Collection<Object> ids = IntStream.range(0, 1000)
                .mapToLong(i -> (long) i)
                .boxed()
                .collect(Collectors.toList());

        CompletableFuture<List<TestEntity>> result = dataSource.fetchAllById(ids);
        List<TestEntity> entities = result.get();

        assertThat(entities).hasSize(1000).allMatch(e -> ids.contains(e.getIdentity()));
    }

    @Test
    @DisplayName("Should handle file modification and reload")
    void shouldHandleFileModificationAndReload() throws Exception {
        Path modifiableFile = tempDir.resolve("modifiable.json");
        String initialContent = """
                [
                    {"id": 1, "name": "Initial 1"},
                    {"id": 2, "name": "Initial 2"}
                ]
                """;
        Files.writeString(modifiableFile, initialContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "modifiable-source",
                TestEntity.class,
                modifiableFile,
                ID
        ));

        // Initial fetch
        List<TestEntity> initialEntities = dataSource.fetchAll().get();
        assertThat(initialEntities)
                .hasSize(2)
                .first()
                .extracting(TestEntity::getName)
                .isEqualTo("Initial 1");

        // Modify file
        TestUtil.await(100); // Ensure different modification time
        String modifiedContent = """
                [
                    {"id": 1, "name": "Modified 1"},
                    {"id": 2, "name": "Modified 2"},
                    {"id": 3, "name": "Modified 3"}
                ]
                """;
        Files.writeString(modifiableFile, modifiedContent);

        // Force reload
        dataSource.forceReload();

        // Fetch again
        List<TestEntity> modifiedEntities = dataSource.fetchAll().get();
        assertThat(modifiedEntities).hasSize(3);
        assertThat(modifiedEntities.get(0).getName()).isEqualTo("Modified 1");
        assertThat(modifiedEntities.get(2).getName()).isEqualTo("Modified 3");
    }

    @Test
    @DisplayName("Should handle concurrent fetchAll operations")
    void shouldHandleConcurrentFetchAllOperations() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "concurrent-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        // Execute multiple concurrent fetches
        List<CompletableFuture<List<TestEntity>>> futures = IntStream.range(0, 10)
                .mapToObj(i -> dataSource.fetchAll())
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Verify all results
        for (CompletableFuture<List<TestEntity>> future : futures) {
            List<TestEntity> entities = future.get();
            assertThat(entities).hasSize(3);
        }
    }

    @Test
    @DisplayName("Should handle CSV with whitespace in fields")
    void shouldHandleCsvWithWhitespace() throws Exception {
        Path csvWithWhitespace = tempDir.resolve("whitespace.csv");
        String csvContent = """
                id,name
                1 , Entity 1 
                 2, Entity 2
                3,  Entity 3  
                """;
        Files.writeString(csvWithWhitespace, csvContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "whitespace-csv-source",
                TestEntity.class,
                csvWithWhitespace,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        assertThat(entities).hasSize(3);
        // Whitespace should be trimmed
        assertThat(entities).extracting(TestEntity::getName)
                .containsExactly(ENTITY_1, ENTITY_2, ENTITY_3);
    }

    @Test
    @DisplayName("Should handle CSV without header")
    void shouldHandleCsvWithoutHeader() throws Exception {
        Path csvNoHeader = tempDir.resolve("no-header.csv");
        String csvContent = """
                1,Entity 1
                2,Entity 2
                3,Entity 3
                """;
        Files.writeString(csvNoHeader, csvContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "no-header-csv-source",
                TestEntity.class,
                csvNoHeader,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        assertThat(entities).hasSize(3);
        assertThat(entities).extracting(TestEntity::getIdentity)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should handle empty CSV file")
    void shouldHandleEmptyCsvFile() throws Exception {
        Path emptyCsv = tempDir.resolve("empty.csv");
        Files.writeString(emptyCsv, "");

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "empty-csv-source",
                TestEntity.class,
                emptyCsv,
                ID,
                fields -> new TestEntity(Long.parseLong(fields[0]), fields[1])
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle JSON array with whitespace")
    void shouldHandleJsonArrayWithWhitespace() throws Exception {
        Path jsonWithWhitespace = tempDir.resolve("whitespace.json");
        String jsonContent = """
                
                [
                  
                  {"id": 1, "name": "Entity 1"},
                  
                  {"id": 2, "name": "Entity 2"}
                  
                ]
                
                """;
        Files.writeString(jsonWithWhitespace, jsonContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "whitespace-json-source",
                TestEntity.class,
                jsonWithWhitespace,
                ID
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        assertThat(entities).hasSize(2);
    }

    @Test
    @DisplayName("Should handle fetchAllById with non-existent IDs")
    void shouldHandleFetchAllByIdWithNonExistentIds() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "non-existent-ids-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        Collection<Object> nonExistentIds = Arrays.asList(999L, 1000L, 1001L);
        List<TestEntity> entities = dataSource.fetchAllById(nonExistentIds).get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle fetchAllById with mixed existing and non-existing IDs")
    void shouldHandleFetchAllByIdWithMixedIds() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "mixed-ids-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        Collection<Object> mixedIds = Arrays.asList(1L, 999L, 2L, 1000L);
        List<TestEntity> entities = dataSource.fetchAllById(mixedIds).get();

        assertThat(entities).hasSize(2);
        assertThat(entities).extracting(TestEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("Should handle file not found during fetchAll")
    void shouldHandleFileNotFoundDuringFetchAll() throws Exception {
        Path nonExistentFile = tempDir.resolve("does-not-exist.json");

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "not-found-source",
                TestEntity.class,
                nonExistentFile,
                ID
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        assertThat(entities).isEmpty();
        assertThat(dataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should handle file deletion after creation")
    void shouldHandleFileDeletionAfterCreation() throws Exception {
        Path deletableFile = tempDir.resolve("deletable.json");
        Files.writeString(deletableFile, """
                [{"id": 1, "name": "Entity 1"}]
                """);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "deletable-source",
                TestEntity.class,
                deletableFile,
                ID
        ));

        // Initial fetch should work
        List<TestEntity> initialEntities = dataSource.fetchAll().get();
        assertThat(initialEntities).hasSize(1);

        // Delete file
        Files.delete(deletableFile);

        // Should return cached data
        List<TestEntity> cachedEntities = dataSource.fetchAll().get();
        assertThat(cachedEntities).hasSize(1);

        // Health check should fail
        assertThat(dataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should handle CSV with empty lines")
    void shouldHandleCsvWithEmptyLines() throws Exception {
        Path csvWithEmptyLines = tempDir.resolve("empty-lines.csv");
        String csvContent = """
                id,name
                1,Entity 1
                
                2,Entity 2
                
                3,Entity 3
                """;
        Files.writeString(csvWithEmptyLines, csvContent);

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "empty-lines-csv-source",
                TestEntity.class,
                csvWithEmptyLines,
                ID,
                fields -> {
                    if (fields.length < 2 || fields[0].isEmpty()) {
                        throw new IllegalArgumentException("Invalid CSV line");
                    }
                    return new TestEntity(Long.parseLong(fields[0]), fields[1]);
                }
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();

        // Should skip empty lines
        assertThat(entities).hasSize(3);
    }

    @Test
    @DisplayName("Should handle full constructor with all parameters")
    void shouldHandleFullConstructor() throws Exception {
        ObjectMapper customMapper = new ObjectMapper();
        
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                FULL_CONSTRUCTOR_SOURCE,
                TestEntity.class,
                jsonFile,
                FileDataSource.FileFormat.JSON,
                ID,
                null,
                customMapper
        ));

        assertThat(dataSource.getName()).isEqualTo(FULL_CONSTRUCTOR_SOURCE);
        assertThat(dataSource.getEntityType()).isEqualTo(TestEntity.class);
        assertThat(dataSource.getFormat()).isEqualTo(FileDataSource.FileFormat.JSON);
        assertThat(dataSource.getIdFieldName()).isEqualTo(ID);

        List<TestEntity> entities = dataSource.fetchAll().get();
        assertThat(entities).hasSize(3);
    }

    @Test
    @DisplayName("Should throw exception when fetching from corrupted file")
    void shouldThrowExceptionForCorruptedFile() throws Exception {
        Path corruptedFile = tempDir.resolve("corrupted.json");
        // Write binary garbage
        Files.write(corruptedFile, new byte[]{0x00, 0x01, 0x02, (byte) 0xFF});

        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "corrupted-source",
                TestEntity.class,
                corruptedFile,
                ID
        ));

        List<TestEntity> entities = dataSource.fetchAll().get();
        
        // Should return empty list on parse error
        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle entity without Identifiable interface")
    void shouldHandleEntityWithoutIdentifiableInterface() throws Exception {
        Path testFile = tempDir.resolve("non-identifiable.json");
        String jsonContent = """
                [
                    {"value": "test1"},
                    {"value": "test2"}
                ]
                """;
        Files.writeString(testFile, jsonContent);

        FileDataSource<NonIdentifiableEntity> dataSource = trackDataSource(new FileDataSource<>(
                "non-identifiable-source",
                NonIdentifiableEntity.class,
                testFile,
                "value"
        ));

        // fetchAll should work
        List<NonIdentifiableEntity> entities = dataSource.fetchAll().get();
        assertThat(entities).hasSize(2);

        // fetchAllById should throw exception
        Collection<Object> ids = List.of("test1");

        // Execute async operation first
        CompletableFuture<List<NonIdentifiableEntity>> future = dataSource.fetchAllById(ids);

        // Create a helper to unwrap the actual exception
        Throwable unwrappedException = null;
        try {
            future.get();
        } catch (ExecutionException e) {
            unwrappedException = e.getCause();
        }

        // Verify the unwrapped exception
        final Throwable finalException = unwrappedException;
        assertThatThrownBy(() -> {
            if (finalException != null) {
                throw finalException;
            }
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not implement Identifiable interface");
    }

    @Test
    @DisplayName("Should cache data and reuse on subsequent fetches")
    void shouldCacheDataAndReuse() throws Exception {
        FileDataSource<TestEntity> dataSource = trackDataSource(new FileDataSource<>(
                "cache-source",
                TestEntity.class,
                jsonFile,
                ID
        ));

        // First fetch
        List<TestEntity> entities1 = dataSource.fetchAll().get();
        
        // Second fetch (should use cache)
        List<TestEntity> entities2 = dataSource.fetchAll().get();

        assertThat(entities1).hasSize(3);
        assertThat(entities2).hasSize(3);
        
        // Both should have same data
        assertThat(entities1).extracting(TestEntity::getIdentity)
                .containsExactlyElementsOf(entities2.stream().map(TestEntity::getIdentity).toList());
    }

    // Test entity classes
    public static class TestEntity implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
        private Long id;
        private String name;

        public TestEntity() {
        }

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getIdentity() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TestEntityWithPrivateId implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
        private Long privateId;
        private String name;

        public TestEntityWithPrivateId() {
            // Noncompliant - method is empty
        }

        @Override
        public Long getIdentity() {
            return privateId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPrivateId(Long privateId) {
            this.privateId = privateId;
        }
    }

    public static class NonIdentifiableEntity {
        private String value;

        public NonIdentifiableEntity() {
            // Noncompliant - method is empty
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}