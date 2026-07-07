package com.thy.fss.common.inmemory.datasource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for real DataSource implementations.
 * Demonstrates usage of DatabaseDataSource, RestApiDataSource, FileDataSource, and CacheDataSource.
 */
class DataSourceIntegrationTest {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String JOHN_DOE = "John Doe";
    private static final String JOHN_EMAIL = "john@example.com";
    private static final String JANE_SMITH = "Jane Smith";
    private static final String JANE_EMAIL = "jane@example.com";
    private static final String FALLBACK_USER = "Fallback User";

    @Test
    void testDatabaseDataSource() throws Exception {
        // Create embedded H2 database
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:test-schema.sql")
                .addScript("classpath:test-data.sql")
                .build();

        try {
            // Create RowMapper
            RowMapper<TestUser> rowMapper = new RowMapper<TestUser>() {
                @Override
                public TestUser mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new TestUser(
                            rs.getLong(ID),
                            rs.getString(NAME),
                            rs.getString(EMAIL)
                    );
                }
            };

            // Create DatabaseDataSource
            DatabaseDataSource<TestUser> dataSource = new DatabaseDataSource<>(
                    "test-db", TestUser.class, database, "users", ID, rowMapper);

            // Test fetchAll
            CompletableFuture<List<TestUser>> allUsers = dataSource.fetchAll();
            List<TestUser> users = allUsers.get();

            assertThat(users).isNotEmpty();
            assertThat(dataSource.isHealthy()).isTrue();

            // Test fetchAllById
            CompletableFuture<List<TestUser>> usersByIds = dataSource.fetchAllById(Arrays.asList(1L, 2L));
            List<TestUser> filteredUsers = usersByIds.get();

            assertThat(filteredUsers).hasSize(2);

        } finally {
            database.shutdown();
        }
    }

    @Test
    void testFileDataSourceWithJson(@TempDir Path tempDir) throws Exception {
        // Create test JSON file
        Path jsonFile = tempDir.resolve("users.json");
        String jsonContent = """
                [
                    {"id": 1, "name": "John Doe", "email": "john@example.com"},
                    {"id": 2, "name": "Jane Smith", "email": "jane@example.com"}
                ]
                """;
        Files.writeString(jsonFile, jsonContent);

        // Create FileDataSource
        FileDataSource<TestUser> dataSource = new FileDataSource<>(
                "test-file", TestUser.class, jsonFile, ID);

        try {
            // Test fetchAll
            CompletableFuture<List<TestUser>> allUsers = dataSource.fetchAll();
            List<TestUser> users = allUsers.get();

            assertThat(users).hasSize(2);
            assertThat(users.get(0).getName()).isEqualTo(JOHN_DOE);
            assertThat(dataSource.isHealthy()).isTrue();

            // Test fetchAllById
            CompletableFuture<List<TestUser>> usersByIds = dataSource.fetchAllById(Collections.singletonList(1L));
            List<TestUser> filteredUsers = usersByIds.get();

            assertThat(filteredUsers).hasSize(1);
            assertThat(filteredUsers.get(0).getIdentity()).isEqualTo(1L);

        } finally {
            dataSource.close();
        }
    }

    @Test
    void testFileDataSourceWithCsv(@TempDir Path tempDir) throws Exception {
        // Create test CSV file
        Path csvFile = tempDir.resolve("users.csv");
        String csvContent = """
                id,name,email
                1,John Doe,john@example.com
                2,Jane Smith,jane@example.com
                """;
        Files.writeString(csvFile, csvContent);

        // Create CSV mapper
        java.util.function.Function<String[], TestUser> csvMapper = fields -> {
            if (fields.length >= 3) {
                return new TestUser(Long.parseLong(fields[0]), fields[1], fields[2]);
            }
            throw new IllegalArgumentException("Invalid CSV row");
        };

        // Create FileDataSource
        FileDataSource<TestUser> dataSource = new FileDataSource<>(
                "test-csv", TestUser.class, csvFile, ID, csvMapper);

        try {
            // Test fetchAll
            CompletableFuture<List<TestUser>> allUsers = dataSource.fetchAll();
            List<TestUser> users = allUsers.get();

            assertThat(users).hasSize(2);
            assertThat(users.get(0).getName()).isEqualTo(JOHN_DOE);
            assertThat(dataSource.isHealthy()).isTrue();

        } finally {
            dataSource.close();
        }
    }

    @Test
    void testCacheDataSource() throws Exception {
        // Create CacheDataSource
        CacheDataSource<TestUser> dataSource = new CacheDataSource<>(
                "test-cache", TestUser.class, TestUser::getIdentity, Duration.ofMinutes(5));

        try {
            // Initially empty
            CompletableFuture<List<TestUser>> emptyResult = dataSource.fetchAll();
            assertThat(emptyResult.get()).isEmpty();

            // Cache some entities
            List<TestUser> testUsers = Arrays.asList(
                    new TestUser(1L, JOHN_DOE, JOHN_EMAIL),
                    new TestUser(2L, JANE_SMITH, JANE_EMAIL)
            );
            dataSource.cacheEntities(testUsers);

            // Test fetchAll
            CompletableFuture<List<TestUser>> allUsers = dataSource.fetchAll();
            List<TestUser> users = allUsers.get();

            assertThat(users).hasSize(2);
            assertThat(dataSource.getCacheSize()).isEqualTo(2);
            assertThat(dataSource.isHealthy()).isTrue();

            // Test fetchAllById
            CompletableFuture<List<TestUser>> usersByIds = dataSource.fetchAllById(Collections.singletonList(1L));
            List<TestUser> filteredUsers = usersByIds.get();

            assertThat(filteredUsers).hasSize(1);
            assertThat(filteredUsers.get(0).getIdentity()).isEqualTo(1L);

            // Test cache stats
            var stats = dataSource.getCacheStats();
            assertThat(stats).containsEntry("cacheSize", 2)
                    .containsEntry("healthy", true);

        } finally {
            dataSource.close();
        }
    }

    @Test
    void testFallbackChain(@TempDir Path tempDir) throws Exception {
        // Create a failing primary DataSource (file doesn't exist)
        Path nonExistentFile = tempDir.resolve("nonexistent.json");
        FileDataSource<TestUser> primaryDataSource = new FileDataSource<>(
                "primary", TestUser.class, nonExistentFile, ID);

        // Create a working fallback DataSource
        CacheDataSource<TestUser> fallbackDataSource = new CacheDataSource<>(
                "fallback", TestUser.class, TestUser::getIdentity);

        // Cache some data in fallback
        fallbackDataSource.cacheEntities(List.of(
                new TestUser(1L, FALLBACK_USER, "fallback@example.com")
        ));

        // Set up fallback chain
        primaryDataSource.setFallbackDataSource(fallbackDataSource);

        try {
            // Test fallback functionality
            CompletableFuture<List<TestUser>> result = primaryDataSource.fetchAllWithFallback();
            List<TestUser> users = result.get();

            assertThat(users).hasSize(1);
            assertThat(users.get(0).getName()).isEqualTo(FALLBACK_USER);

        } finally {
            primaryDataSource.close();
            fallbackDataSource.close();
        }
    }

    // Test entity class
    public static class TestUser implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
        private Long id;
        private String name;
        private String email;

        public TestUser() {
        }

        public TestUser(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        // Getters and setters
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "TestUser{id=" + id + ", name='" + name + "', email='" + email + "'}";
        }
    }
}