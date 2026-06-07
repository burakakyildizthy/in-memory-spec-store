package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.exception.DataSourceException;
import com.thy.fss.common.inmemory.testmodel.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for DataSource fallback chain functionality.
 * Tests various failure scenarios and fallback chain traversal patterns.
 */
@DisplayName("DataSource Fallback Chain Tests")
class DataSourceFallbackChainTest {

    private static final String PRIMARY = "primary";
    private static final String FALLBACK = "fallback";
    private static final String SECONDARY = "secondary";
    private static final String TERTIARY = "tertiary";
    private static final String QUATERNARY = "quaternary";
    private static final String PRIMARY_ENTITY = "Primary Entity";
    private static final String FALLBACK_ENTITY = "Fallback Entity";
    private static final String ENTITY_1 = "Entity 1";
    private static final String DS1 = "ds1";
    private static final String DS2 = "ds2";
    private static final String DS3 = "ds3";
    private static final String EXCEPTION_FALLBACK = "Exception Fallback";
    private static final String NULL_FALLBACK = "Null Fallback";
    private static final String SECONDARY_ENTITY = "Secondary Entity";
    private static final String TERTIARY_ENTITY = "Tertiary Entity";
    private static final String FINAL_WORKING_ENTITY = "Final Working Entity";
    

    // Test data class
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return Objects.equals(id, that.id) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", name='" + name + "'}";
        }
    }

    @Nested
    @DisplayName("Simple Fallback Chain Tests")
    class SimpleFallbackChainTests {

        @Test
        @DisplayName("Should use primary DataSource when healthy")
        void shouldUsePrimaryDataSourceWhenHealthy() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> primaryData = List.of(
                    new TestEntity(1L, PRIMARY_ENTITY)
            );
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(2L, FALLBACK_ENTITY)
            );

            InMemoryDataSource<TestEntity> primary =
                    new InMemoryDataSource<>(PRIMARY, TestEntity.class, primaryData);
            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(1L, PRIMARY_ENTITY));
        }

        @Test
        @DisplayName("Should use fallback when primary is unhealthy")
        void shouldUseFallbackWhenPrimaryIsUnhealthy() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(2L, FALLBACK_ENTITY)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.setHealthy(false);

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(2L, FALLBACK_ENTITY));
        }

        @Test
        @DisplayName("Should use fallback when primary throws DataSourceException")
        void shouldUseFallbackWhenPrimaryThrowsDataSourceException() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(3L, EXCEPTION_FALLBACK)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);
            primary.setFailureExceptionSupplier(() -> new DataSourceException("Primary failed"));

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(3L, EXCEPTION_FALLBACK));
        }

        @Test
        @DisplayName("Should use fallback when primary returns null")
        void shouldUseFallbackWhenPrimaryReturnsNull() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(4L, NULL_FALLBACK)
            );

            DataSource<TestEntity> nullReturningPrimary = new DataSource<TestEntity>() {
                private DataSource<TestEntity> fallback;

                @Override
                public String getName() {
                    return "null-primary";
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAll() {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                @Override
                public boolean isHealthy() {
                    return true;
                }

                @Override
                public void close() {
                    // Noncompliant - method is empty
                }

                @Override
                public Optional<DataSource<TestEntity>> getFallbackDataSource() {
                    return Optional.ofNullable(fallback);
                }

                @Override
                public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) {
                    this.fallback = fallbackDataSource;
                }
            };

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            nullReturningPrimary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = nullReturningPrimary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(4L, NULL_FALLBACK));
        }
    }

    @Nested
    @DisplayName("Multi-Level Fallback Chain Tests")
    class MultiLevelFallbackChainTests {

        @Test
        @DisplayName("Should traverse three-level fallback chain")
        void shouldTraverseThreeLevelFallbackChain() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> tertiaryData = List.of(
                    new TestEntity(3L, TERTIARY_ENTITY)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, TestEntity.class);
            secondary.enableFailureSimulation(1.0);

            InMemoryDataSource<TestEntity> tertiary =
                    new InMemoryDataSource<>(TERTIARY, TestEntity.class, tertiaryData);

            // Chain: primary -> secondary -> tertiary
            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(3L, TERTIARY_ENTITY));
        }

        @Test
        @DisplayName("Should stop at first working DataSource in chain")
        void shouldStopAtFirstWorkingDataSourceInChain() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> secondaryData = List.of(
                    new TestEntity(2L, SECONDARY_ENTITY)
            );
            List<TestEntity> tertiaryData = List.of(
                    new TestEntity(3L, TERTIARY_ENTITY)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            InMemoryDataSource<TestEntity> secondary =
                    new InMemoryDataSource<>(SECONDARY, TestEntity.class, secondaryData);

            InMemoryDataSource<TestEntity> tertiary =
                    new InMemoryDataSource<>(TERTIARY, TestEntity.class, tertiaryData);

            // Chain: primary -> secondary -> tertiary
            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then - Should use secondary, not tertiary
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(2L, SECONDARY_ENTITY));
        }

        @Test
        @DisplayName("Should handle mixed failure types in chain")
        void shouldHandleMixedFailureTypesInChain() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> finalData = List.of(
                    new TestEntity(4L, FINAL_WORKING_ENTITY)
            );

            // Primary: unhealthy
            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.setHealthy(false);

            // Secondary: throws exception
            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, TestEntity.class);
            secondary.enableFailureSimulation(1.0);
            secondary.setFailureExceptionSupplier(() -> new RuntimeException("Secondary failed"));

            // Tertiary: returns null
            DataSource<TestEntity> tertiary = new DataSource<TestEntity>() {
                private DataSource<TestEntity> fallback;

                @Override
                public String getName() {
                    return TERTIARY;
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAll() {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                @Override
                public java.util.concurrent.CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                @Override
                public boolean isHealthy() {
                    return true;
                }

                @Override
                public void close() {
                    // Noncompliant - method is empty
                }

                @Override
                public Optional<DataSource<TestEntity>> getFallbackDataSource() {
                    return Optional.ofNullable(fallback);
                }

                @Override
                public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) {
                    this.fallback = fallbackDataSource;
                }
            };

            // Quaternary: works
            InMemoryDataSource<TestEntity> quaternary =
                    new InMemoryDataSource<>(QUATERNARY, TestEntity.class, finalData);

            // Chain: primary -> secondary -> tertiary -> quaternary
            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);
            tertiary.setFallbackDataSource(quaternary);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(4L, FINAL_WORKING_ENTITY));
        }
    }

    @Nested
    @DisplayName("FetchAllById Fallback Tests")
    class FetchAllByIdFallbackTests {

        @Test
        @DisplayName("Should use fallback for fetchAllByIdWithFallback when primary fails")
        void shouldUseFallbackForFetchAllByIdWithFallbackWhenPrimaryFails() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = Arrays.asList(
                    new TestEntity(1L, "Fallback Entity 1"),
                    new TestEntity(2L, "Fallback Entity 2"),
                    new TestEntity(3L, "Fallback Entity 3")
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            Collection<Object> ids = Arrays.asList(1L, 3L);
            List<TestEntity> result = primary.fetchAllByIdWithFallback(ids).get();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(TestEntity::getIdentity)
                    .containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("Should traverse fallback chain for fetchAllByIdWithFallback")
        void shouldTraverseFallbackChainForFetchAllByIdWithFallback() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> tertiaryData = Arrays.asList(
                    new TestEntity(5L, "Tertiary Entity 5"),
                    new TestEntity(6L, "Tertiary Entity 6")
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.setHealthy(false);

            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, TestEntity.class);
            secondary.enableFailureSimulation(1.0);

            InMemoryDataSource<TestEntity> tertiary =
                    new InMemoryDataSource<>(TERTIARY, TestEntity.class, tertiaryData);

            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            Collection<Object> ids = Arrays.asList(5L, 6L, 7L); // 7L doesn't exist
            List<TestEntity> result = primary.fetchAllByIdWithFallback(ids).get();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(TestEntity::getIdentity)
                    .containsExactlyInAnyOrder(5L, 6L);
        }
    }

    @Nested
    @DisplayName("Complete Failure Scenarios")
    class CompleteFailureScenariosTests {

        @Test
        @DisplayName("Should return empty list when all DataSources fail")
        void shouldReturnEmptyListWhenAllDataSourcesFail() throws ExecutionException, InterruptedException {
            // Given
            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, TestEntity.class);
            secondary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<TestEntity> tertiary =
                    new TestableInMemoryDataSource<>(TERTIARY, TestEntity.class);
            tertiary.setHealthy(false);

            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for fetchAllByIdWithFallback when all fail")
        void shouldReturnEmptyListForFetchAllByIdWithFallbackWhenAllFail() throws ExecutionException, InterruptedException {
            // Given
            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<TestEntity> fallback =
                    new TestableInMemoryDataSource<>(FALLBACK, TestEntity.class);
            fallback.setHealthy(false);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllByIdWithFallback(Arrays.asList(1L, 2L)).get();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle single DataSource without fallback gracefully")
        void shouldHandleSingleDataSourceWithoutFallbackGracefully() throws ExecutionException, InterruptedException {
            // Given
            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);
            // No fallback set

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Circular Reference Prevention Tests")
    class CircularReferencePreventionTests {

        @Test
        @DisplayName("Should document circular reference behavior")
        void shouldDocumentCircularReferenceBehavior() {
            // Given
            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, TestEntity.class);

            // When - Create circular reference
            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(primary);

            // Then - Verify the circular reference is set up
            assertThat(primary.getFallbackDataSource()).isPresent()
                    .containsSame(secondary);
            assertThat(secondary.getFallbackDataSource()).isPresent()
                    .containsSame(primary);

            // Note: The current implementation does not prevent circular references.
            // In a production system, this could lead to infinite loops.
            // This test documents the current behavior for future improvement.
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should create fallback chain using DataSourceTestUtils")
        void shouldCreateFallbackChainUsingDataSourceTestUtils() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> data1 = List.of(new TestEntity(1L, ENTITY_1));
            List<TestEntity> data2 = List.of(new TestEntity(2L, "Entity 2"));
            List<TestEntity> data3 = List.of(new TestEntity(3L, "Entity 3"));

            DataSource<TestEntity> ds1 = DataSourceTestUtils.createBasicDataSource(DS1, TestEntity.class, data1);
            DataSource<TestEntity> ds2 = DataSourceTestUtils.createBasicDataSource(DS2, TestEntity.class, data2);
            DataSource<TestEntity> ds3 = DataSourceTestUtils.createBasicDataSource(DS3, TestEntity.class, data3);

            // When
            DataSource<TestEntity> chainedDataSource = DataSourceTestUtils.createFallbackChain(ds1, ds2, ds3);

            // Then
            assertThat(chainedDataSource).isSameAs(ds1);
            assertThat(ds1.getFallbackDataSource()).isPresent();
            assertThat(ds1.getFallbackDataSource()).containsSame(ds2);
            assertThat(ds2.getFallbackDataSource()).isPresent();
            assertThat(ds2.getFallbackDataSource()).containsSame(ds3);
            assertThat(ds3.getFallbackDataSource()).isEmpty();

            // Verify it works
            List<TestEntity> result = chainedDataSource.fetchAllWithFallback().get();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(1L, ENTITY_1));
        }

        @Test
        @DisplayName("Should create failing DataSource chain and use fallback")
        void shouldCreateFailingDataSourceChainAndUseFallback() throws ExecutionException, InterruptedException {
            // Given
            RuntimeException testException = new RuntimeException("Test failure");
            List<TestEntity> fallbackData = List.of(new TestEntity(99L, FALLBACK_ENTITY));

            DataSource<TestEntity> failing = DataSourceTestUtils.createAlwaysFailingDataSource(
                    "failing", TestEntity.class, testException);
            DataSource<TestEntity> working = DataSourceTestUtils.createBasicDataSource(
                    "working", TestEntity.class, fallbackData);

            // When
            DataSource<TestEntity> chainedDataSource = DataSourceTestUtils.createFallbackChain(failing, working);
            List<TestEntity> result = chainedDataSource.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(99L, FALLBACK_ENTITY));
        }
    }

    @Nested
    @DisplayName("Large Dataset Fallback Tests")
    class LargeDatasetFallbackTests {

        private final LargeDatasetGenerator generator = new LargeDatasetGenerator();

        @Test
        @DisplayName("Should handle fallback with 10K entities")
        void shouldHandleFallbackWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Generate 10K users for realistic testing
            List<User> primaryData = generator.generateUsers(10_000);
            List<User> fallbackData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class, primaryData);
            primary.enableFailureSimulation(1.0);

            InMemoryDataSource<User> fallback =
                    new InMemoryDataSource<>(FALLBACK, User.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<User> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(10_000).isEqualTo(fallbackData);
        }

        @Test
        @DisplayName("Should handle three-level fallback chain with 10K entities")
        void shouldHandleThreeLevelFallbackChainWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Create three-level fallback chain with large datasets
            List<User> tertiaryData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, User.class);
            secondary.setHealthy(false);

            InMemoryDataSource<User> tertiary =
                    new InMemoryDataSource<>(TERTIARY, User.class, tertiaryData);

            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            List<User> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(10_000).isEqualTo(tertiaryData);
        }

        @Test
        @DisplayName("Should handle fetchAllById fallback with 10K entities")
        void shouldHandleFetchAllByIdFallbackWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Generate 10K users and query subset by IDs
            List<User> fallbackData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.enableFailureSimulation(1.0);

            InMemoryDataSource<User> fallback =
                    new InMemoryDataSource<>(FALLBACK, User.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When - Query for specific IDs
            Collection<Object> ids = Arrays.asList("0", "100", "500", "1000", "5000", "9999");
            List<User> result = primary.fetchAllByIdWithFallback(ids).get();

            // Then
            assertThat(result).hasSize(6);
            assertThat(result).extracting(User::getIdentity)
                    .containsExactlyInAnyOrder("0", "100", "500", "1000", "5000", "9999");
        }

        @Test
        @DisplayName("Should handle primary recovery after failure with 10K entities")
        void shouldHandlePrimaryRecoveryAfterFailureWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Primary fails initially but recovers
            List<User> primaryData = generator.generateUsers(10_000);
            List<User> fallbackData = generator.generateUsers(5_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class, primaryData);
            primary.failAfterRequests(0); // Fail on first request

            InMemoryDataSource<User> fallback =
                    new InMemoryDataSource<>(FALLBACK, User.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When - First request should use fallback
            List<User> firstResult = primary.fetchAllWithFallback().get();

            // Then - Should get fallback data
            assertThat(firstResult).hasSize(5_000);

            // When - Disable failure simulation (recovery)
            primary.disableFailureSimulation();
            List<User> secondResult = primary.fetchAllWithFallback().get();

            // Then - Should get primary data after recovery
            assertThat(secondResult).hasSize(10_000);
        }

        @Test
        @DisplayName("Should handle mixed failure types in chain with 10K entities")
        void shouldHandleMixedFailureTypesInChainWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Complex failure scenario with large datasets
            List<User> finalData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.setHealthy(false);

            TestableInMemoryDataSource<User> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, User.class);
            secondary.enableFailureSimulation(1.0);
            secondary.setFailureExceptionSupplier(() -> new DataSourceException("Secondary failed"));

            TestableInMemoryDataSource<User> tertiary =
                    new TestableInMemoryDataSource<>(TERTIARY, User.class);
            tertiary.enableFailureSimulation(1.0);
            tertiary.setFailureExceptionSupplier(() -> new RuntimeException("Tertiary failed"));

            InMemoryDataSource<User> quaternary =
                    new InMemoryDataSource<>(QUATERNARY, User.class, finalData);

            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);
            tertiary.setFallbackDataSource(quaternary);

            // When
            List<User> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(10_000).isEqualTo(finalData);
        }

        @Test
        @DisplayName("Should handle partial failure in chain with 10K entities")
        void shouldHandlePartialFailureInChainWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - First two fail, third succeeds
            List<User> workingData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, User.class);
            secondary.enableFailureSimulation(1.0);

            InMemoryDataSource<User> tertiary =
                    new InMemoryDataSource<>(TERTIARY, User.class, workingData);

            DataSource<User> chain = DataSourceTestUtils.createFallbackChain(primary, secondary, tertiary);

            // When
            List<User> result = chain.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(10_000).isEqualTo(workingData);
        }

        @Test
        @DisplayName("Should handle all datasources failing with 10K entities")
        void shouldHandleAllDatasourcesFailingWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - All datasources fail
            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, User.class);
            secondary.setHealthy(false);

            TestableInMemoryDataSource<User> tertiary =
                    new TestableInMemoryDataSource<>(TERTIARY, User.class);
            tertiary.enableFailureSimulation(1.0);

            DataSource<User> chain = DataSourceTestUtils.createFallbackChain(primary, secondary, tertiary);

            // When
            List<User> result = chain.fetchAllWithFallback().get();

            // Then - Should return empty list for graceful degradation
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle five-level fallback chain with 10K entities")
        void shouldHandleFiveLevelFallbackChainWith10KEntities() throws ExecutionException, InterruptedException {
            // Given - Five-level fallback chain
            List<User> finalData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> ds1 =
                    new TestableInMemoryDataSource<>(DS1, User.class);
            ds1.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> ds2 =
                    new TestableInMemoryDataSource<>(DS2, User.class);
            ds2.setHealthy(false);

            TestableInMemoryDataSource<User> ds3 =
                    new TestableInMemoryDataSource<>(DS3, User.class);
            ds3.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> ds4 =
                    new TestableInMemoryDataSource<>("ds4", User.class);
            ds4.enableFailureSimulation(1.0);

            InMemoryDataSource<User> ds5 =
                    new InMemoryDataSource<>("ds5", User.class, finalData);

            DataSource<User> chain = DataSourceTestUtils.createFallbackChain(ds1, ds2, ds3, ds4, ds5);

            // When
            List<User> result = chain.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(10_000).isEqualTo(finalData);
        }

        @Test
        @DisplayName("Should handle fetchAllById with multiple fallbacks and 10K entities")
        void shouldHandleFetchAllByIdWithMultipleFallbacksAnd10KEntities() throws ExecutionException, InterruptedException {
            // Given - Multiple fallbacks for fetchAllById
            List<User> tertiaryData = generator.generateUsers(10_000);

            TestableInMemoryDataSource<User> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, User.class);
            primary.enableFailureSimulation(1.0);

            TestableInMemoryDataSource<User> secondary =
                    new TestableInMemoryDataSource<>(SECONDARY, User.class);
            secondary.enableFailureSimulation(1.0);

            InMemoryDataSource<User> tertiary =
                    new InMemoryDataSource<>(TERTIARY, User.class, tertiaryData);

            DataSource<User> chain = DataSourceTestUtils.createFallbackChain(primary, secondary, tertiary);

            // When - Query for 100 specific IDs
            List<Object> ids = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                ids.add(String.valueOf(i * 100));
            }
            List<User> result = chain.fetchAllByIdWithFallback(ids).get();

            // Then
            assertThat(result).hasSize(100).allMatch(user -> ids.contains(user.getIdentity()));
        }
    }
}