package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceConnectionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test coverage for DatabaseDataSource.
 * Tests JDBC operations, connection handling, SQL query execution, result set mapping,
 * connection pooling, transaction handling, database errors, and large dataset operations.
 */
class DatabaseDataSourceTest {


    private static final String TEST_ENTITIES = "test_entities";
    private static final String ID = "id";
    private static final String TEST_DB = "TestDB";
    private static final String NON_EXISTENT_TABLE = "non_existent_table";
    private static final String INVALID_DB = "InvalidDB";
    private static final String TABLE = "table";
    private static final String TEST_ENTITY = "TestEntity";
    private static final String NAME = "name";
    private static final String INSERT_SQL = "INSERT INTO test_entities (id, name, description, active, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String TEST_DESC = "Test Description";
    private static final String DESC = "description";

    private static EmbeddedDatabase embeddedDatabase;
    private static HikariDataSource pooledDataSource;
    private JdbcTemplate jdbcTemplate;
    private DatabaseDataSource<TestEntity> databaseDataSource;

    @BeforeAll
    static void setUpDatabase() {
        embeddedDatabase = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db/test-schema.sql")
                .build();

        HikariConfig config = new HikariConfig();
        config.setDataSource(embeddedDatabase);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        pooledDataSource = new HikariDataSource(config);
    }

    @AfterAll
    static void tearDownDatabase() {
        if (pooledDataSource != null) {
            pooledDataSource.close();
        }
        if (embeddedDatabase != null) {
            embeddedDatabase.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(embeddedDatabase);
        jdbcTemplate.execute("DELETE FROM test_entities");
        
        databaseDataSource = new DatabaseDataSource<>(
                TEST_DB,
                TestEntity.class,
                embeddedDatabase,
                TEST_ENTITIES,
                ID,
                new TestEntityRowMapper()
        );
    }

    @AfterEach
    void tearDown() {
        if (databaseDataSource != null) {
            databaseDataSource.close();
        }
    }

    // ========== Constructor and Configuration Tests ==========

    @Test
    @DisplayName("Should create DatabaseDataSource with valid parameters")
    void testConstructorValidParameters() {
        DatabaseDataSource<TestEntity> dataSource = new DatabaseDataSource<>(
                TEST_DB,
                TestEntity.class,
                embeddedDatabase,
                TEST_ENTITIES,
                ID,
                new TestEntityRowMapper()
        );

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getName()).isEqualTo(TEST_DB);
        assertThat(dataSource.getEntityType()).isEqualTo(TestEntity.class);
        assertThat(dataSource.getTableName()).isEqualTo(TEST_ENTITIES);
        assertThat(dataSource.getIdColumnName()).isEqualTo(ID);
        assertThat(dataSource.getJdbcTemplate()).isNotNull();
    }

    @Test
    @DisplayName("Should validate constructor parameters")
    void testConstructorParameterValidation() {
        RowMapper<TestEntity> rowMapper = new TestEntityRowMapper();

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                null, TestEntity.class, embeddedDatabase, TABLE, ID, rowMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NAME);

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                TEST_DB, null, embeddedDatabase, TABLE, ID, rowMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity type");

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                TEST_DB, TestEntity.class, null, TABLE, ID, rowMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DataSource");

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                TEST_DB, TestEntity.class, embeddedDatabase, null, ID, rowMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Table name");

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                TEST_DB, TestEntity.class, embeddedDatabase, TABLE, null, rowMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID column");

        assertThatThrownBy(() -> new DatabaseDataSource<>(
                TEST_DB, TestEntity.class, embeddedDatabase, TABLE, ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("RowMapper");
    }

    // ========== FetchAll Tests ==========

    @Test
    @DisplayName("Should fetch all entities successfully")
    void testFetchAllSuccess() throws Exception {
        insertTestEntities(100);

        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAll();
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(100).allMatch(e -> e.getId() != null && e.getName() != null);
    }

    @Test
    @DisplayName("Should fetch all with empty table")
    void testFetchAllEmptyTable() throws Exception {
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAll();
        List<TestEntity> entities = future.get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should fetch all with large dataset (10K entities)")
    void testFetchAllLargeDataset() throws Exception {
        insertTestEntities(10_000);

        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAll();
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(10_000);
        assertThat(entities.get(0).getId()).isZero();
        assertThat(entities.get(9999).getId()).isEqualTo(9999L);
    }

    @Test
    @DisplayName("Should handle SQL exception in fetchAll")
    void testFetchAllSQLException() {
        DatabaseDataSource<TestEntity> invalidDataSource = new DatabaseDataSource<>(
                INVALID_DB,
                TestEntity.class,
                embeddedDatabase,
                NON_EXISTENT_TABLE,
                ID,
                new TestEntityRowMapper()
        );

        CompletableFuture<List<TestEntity>> future = invalidDataSource.fetchAll();

        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(DataSourceConnectionException.class)
                .hasMessageContaining(NON_EXISTENT_TABLE);
    }

    // ========== FetchAllById Tests ==========

    @Test
    @DisplayName("Should fetch entities by IDs successfully")
    void testFetchAllByIdSuccess() throws Exception {
        insertTestEntities(1000);

        List<Object> ids = Arrays.asList(10L, 20L, 30L, 40L, 50L);
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(ids);
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(5);
        assertThat(entities).extracting(TestEntity::getId)
                .containsExactlyInAnyOrder(10L, 20L, 30L, 40L, 50L);
    }

    @Test
    @DisplayName("Should fetch single entity by ID")
    void testFetchAllByIdSingleId() throws Exception {
        insertTestEntities(100);

        List<Object> ids = Collections.singletonList(42L);
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(ids);
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getId()).isEqualTo(42L);
        assertThat(entities.get(0).getName()).isEqualTo("Entity42");
    }

    @Test
    @DisplayName("Should handle null IDs collection")
    void testFetchAllByIdNullIds() throws Exception {
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(null);
        List<TestEntity> entities = future.get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty IDs collection")
    void testFetchAllByIdEmptyIds() throws Exception {
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(Collections.emptyList());
        List<TestEntity> entities = future.get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should fetch large number of entities by IDs (1000 IDs)")
    void testFetchAllByIdLargeIdSet() throws Exception {
        insertTestEntities(10_000);

        List<Object> ids = new ArrayList<>();
        for (long i = 0; i < 1000; i++) {
            ids.add(i * 10);
        }

        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(ids);
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(1000);
        List<Long> entityIds = entities.stream().map(TestEntity::getId).toList();
        List<Long> expectedIds = ids.stream().map(id -> (Long) id).toList();
        assertThat(entityIds).containsAll(expectedIds);
    }

    @Test
    @DisplayName("Should handle non-existent IDs")
    void testFetchAllByIdNonExistentIds() throws Exception {
        insertTestEntities(100);

        List<Object> ids = Arrays.asList(999L, 1000L, 1001L);
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(ids);
        List<TestEntity> entities = future.get();

        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should handle mixed existent and non-existent IDs")
    void testFetchAllByIdMixedIds() throws Exception {
        insertTestEntities(100);

        List<Object> ids = Arrays.asList(10L, 999L, 20L, 1000L, 30L);
        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAllById(ids);
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(3);
        assertThat(entities).extracting(TestEntity::getId)
                .containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    @DisplayName("Should handle SQL exception in fetchAllById")
    void testFetchAllByIdSQLException() {
        DatabaseDataSource<TestEntity> invalidDataSource = new DatabaseDataSource<>(
                INVALID_DB,
                TestEntity.class,
                embeddedDatabase,
                NON_EXISTENT_TABLE,
                ID,
                new TestEntityRowMapper()
        );

        List<Object> ids = Arrays.asList(1L, 2L, 3L);
        CompletableFuture<List<TestEntity>> future = invalidDataSource.fetchAllById(ids);

        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(DataSourceConnectionException.class)
                .hasMessageContaining(NON_EXISTENT_TABLE);
    }

    // ========== Health Check Tests ==========

    @Test
    @DisplayName("Should return healthy status for valid connection")
    void testIsHealthySuccess() {
        boolean healthy = databaseDataSource.isHealthy();

        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("Should return unhealthy status for invalid connection")
    void testIsHealthyInvalidConnection() {
        // Create a datasource that will fail on connection attempts
        DataSource failingDataSource = new DataSource() {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                throw new java.sql.SQLException("Database connection failed");
            }

            @Override
            public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
                throw new java.sql.SQLException("Database connection failed");
            }

            @Override
            public java.io.PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {
                // Noncompliant - method is empty
            }

            @Override
            public void setLoginTimeout(int seconds) {
                // Noncompliant - method is empty
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getLogger("test");
            }

            @Override
            public <T> T unwrap(Class<T> iface) {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }
        };

        DatabaseDataSource<TestEntity> invalidDataSource = new DatabaseDataSource<>(
                INVALID_DB,
                TestEntity.class,
                failingDataSource,
                TEST_ENTITIES,
                ID,
                new TestEntityRowMapper()
        );

        boolean healthy = invalidDataSource.isHealthy();

        assertThat(healthy).isFalse();
    }

    // ========== Connection Pooling Tests ==========

    @Test
    @DisplayName("Should work with connection pooling")
    void testConnectionPoolingSuccess() throws Exception {
        DatabaseDataSource<TestEntity> pooledDataSourceInstance = new DatabaseDataSource<>(
                "PooledDB",
                TestEntity.class,
                pooledDataSource,
                TEST_ENTITIES,
                ID,
                new TestEntityRowMapper()
        );

        insertTestEntities(100);

        List<CompletableFuture<List<TestEntity>>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(pooledDataSourceInstance.fetchAll());
        }

        for (CompletableFuture<List<TestEntity>> future : futures) {
            List<TestEntity> entities = future.get();
            assertThat(entities).hasSize(100);
        }

        pooledDataSourceInstance.close();
    }

    // ========== Fallback DataSource Tests ==========

    @Test
    @DisplayName("Should handle fallback datasource operations")
    void testFallbackDataSourceOperations() {
        DatabaseDataSource<TestEntity> fallbackDataSource = new DatabaseDataSource<>(
                "FallbackDB",
                TestEntity.class,
                embeddedDatabase,
                TEST_ENTITIES,
                ID,
                new TestEntityRowMapper()
        );

        assertThat(databaseDataSource.getFallbackDataSource()).isEmpty();

        databaseDataSource.setFallbackDataSource(fallbackDataSource);
        assertThat(databaseDataSource.getFallbackDataSource()).isPresent();
        assertThat(databaseDataSource.getFallbackDataSource()).contains(fallbackDataSource);

        databaseDataSource.setFallbackDataSource(null);
        assertThat(databaseDataSource.getFallbackDataSource()).isEmpty();
    }

    // ========== Getter Methods Tests ==========

    @Test
    @DisplayName("Should return correct values from getter methods")
    void testGettersAllMethods() {
        assertThat(databaseDataSource.getName()).isEqualTo(TEST_DB);
        assertThat(databaseDataSource.getEntityType()).isEqualTo(TestEntity.class);
        assertThat(databaseDataSource.getTableName()).isEqualTo(TEST_ENTITIES);
        assertThat(databaseDataSource.getIdColumnName()).isEqualTo(ID);
        assertThat(databaseDataSource.getJdbcTemplate()).isNotNull();
        assertThat(databaseDataSource.getJdbcTemplate().getDataSource()).isEqualTo(embeddedDatabase);
    }

    // ========== Close Method Tests ==========

    @Test
    @DisplayName("Should handle close method without errors")
    void testCloseSuccess() {
        assertThatCode(() -> databaseDataSource.close()).doesNotThrowAnyException();
    }

    // ========== Result Set Mapping Tests ==========

    @Test
    @DisplayName("Should map result set correctly with all field types")
    void testResultSetMappingAllFieldTypes() throws Exception {
        jdbcTemplate.update(
                INSERT_SQL,
                1L, TEST_ENTITY, TEST_DESC, true, new java.sql.Timestamp(System.currentTimeMillis())
        );

        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAll();
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(1);
        TestEntity entity = entities.get(0);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo(TEST_ENTITY);
        assertThat(entity.getDescription()).isEqualTo(TEST_DESC);
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null values in result set")
    void testResultSetMappingNullValues() throws Exception {
        jdbcTemplate.update(
                INSERT_SQL,
                1L, TEST_ENTITY, null, null, null
        );

        CompletableFuture<List<TestEntity>> future = databaseDataSource.fetchAll();
        List<TestEntity> entities = future.get();

        assertThat(entities).hasSize(1);
        TestEntity entity = entities.get(0);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo(TEST_ENTITY);
        assertThat(entity.getDescription()).isNull();
    }

    // ========== Helper Methods ==========

    private void insertTestEntities(int count) {
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            batchArgs.add(new Object[]{
                    (long) i,
                    "Entity" + i,
                    DESC + i,
                    i % 2 == 0,
                    new java.sql.Timestamp(System.currentTimeMillis())
            });
        }

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                batchArgs
        );
    }

    // ========== Test Entity and RowMapper ==========

    static class TestEntity {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private java.sql.Timestamp createdAt;

        public TestEntity() {
        }

        public TestEntity(Long id, String name, String description, boolean active, java.sql.Timestamp createdAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.active = active;
            this.createdAt = createdAt;
        }

        public Long getId() {
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public java.sql.Timestamp getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.sql.Timestamp createdAt) {
            this.createdAt = createdAt;
        }
    }

    static class TestEntityRowMapper implements RowMapper<TestEntity> {
        @Override
        public TestEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            TestEntity entity = new TestEntity();
            entity.setId(rs.getLong(ID));
            entity.setName(rs.getString(NAME));
            entity.setDescription(rs.getString(DESC));
            
            Boolean active = (Boolean) rs.getObject("active");
            entity.setActive(active != null && active);
            
            entity.setCreatedAt(rs.getTimestamp("created_at"));
            return entity;
        }
    }
}