package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.exception.DataSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for DataSource exception handling scenarios.
 * Tests various types of exceptions and error recovery mechanisms.
 */
@DisplayName("DataSource Exception Handling Tests")
class DataSourceExceptionHandlingTest {

    private static final String PRIMARY = "primary";
    private static final String FALLBACK = "fallback";
    private static final String PRIMARY_ENTITY = "Primary Entity";
    private static final String FALLBACK_ENTITY = "Fallback Entity";
    private static final String TEST_FAILURE = "Test failure";
    private static final String PRIMARY_FAILED = "Primary failed";
    private static final String FAILING = "failing";
    private static final String TERTIARY_SUCCESS = "Tertiary Success";
    private static final String CONCURRENT_FALLBACK = "Concurrent Fallback";
    private static final String CLOSE_OPERATION_FAILED = "Close operation failed";
    private static final String DATASOURCE_CONNECTION_FAILED = "DataSource connection failed";
    private static final String FAST_FALLBACK = "Fast Fallback";
    private static final String FINAL_FALLBACK = "Final Fallback";
    private static final String NULL_EXCEPTION_FALLBACK = "Null Exception Fallback";
    

    // Test data class
    public static class TestEntity {
        private Long id;
        private String name;

        public TestEntity() {
        }

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
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
    @DisplayName("Direct Method Exception Tests")
    class DirectMethodExceptionTests {

        @Test
        @DisplayName("Should propagate exception from fetchAll when no fallback")
        void shouldPropagateExceptionFromFetchAllWhenNoFallback() {
            // Given
            TestableInMemoryDataSource<TestEntity> dataSource =
                    new TestableInMemoryDataSource<>(FAILING, TestEntity.class);
            RuntimeException testException = new RuntimeException(TEST_FAILURE);
            dataSource.enableFailureSimulation(1.0);
            dataSource.setFailureExceptionSupplier(() -> testException);

            // When & Then
            assertThatThrownBy(() -> dataSource.fetchAll().get())
                    .hasCause(testException)
                    .hasMessageContaining(TEST_FAILURE);
        }

        @Test
        @DisplayName("Should propagate exception from fetchAllById when no fallback")
        void shouldPropagateExceptionFromFetchAllByIdWhenNoFallback() {
            // Given
            TestableInMemoryDataSource<TestEntity> dataSource =
                    new TestableInMemoryDataSource<>(FAILING, TestEntity.class);
            DataSourceException testException = new DataSourceException(DATASOURCE_CONNECTION_FAILED);
            dataSource.enableFailureSimulation(1.0);
            dataSource.setFailureExceptionSupplier(() -> testException);

            // When & Then
            assertThatThrownBy(() -> dataSource.fetchAllById(Arrays.asList(1L, 2L)).get())
                    .hasCause(testException)
                    .hasMessageContaining(DATASOURCE_CONNECTION_FAILED);
        }

        @Test
        @DisplayName("Should handle unhealthy DataSource exception")
        void shouldHandleUnhealthyDataSourceException() {
            // Given
            TestableInMemoryDataSource<TestEntity> dataSource =
                    new TestableInMemoryDataSource<>("unhealthy", TestEntity.class);
            dataSource.setHealthy(false);

            // When & Then
            assertThatThrownBy(() -> dataSource.fetchAll().get())
                    .hasCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DataSource 'unhealthy' is not healthy");
        }
    }

    @Nested
    @DisplayName("Fallback Exception Handling Tests")
    class FallbackExceptionHandlingTests {

        static Stream<Arguments> exceptionTypes() {
            return Stream.of(
                    Arguments.of("RuntimeException", new RuntimeException("Runtime error")),
                    Arguments.of("DataSourceException", new DataSourceException("DataSource error")),
                    Arguments.of("IllegalStateException", new IllegalStateException("Illegal state")),
                    Arguments.of("NullPointerException", new NullPointerException("Null pointer")),
                    Arguments.of("IllegalArgumentException", new IllegalArgumentException("Illegal argument"))
            );
        }

        @ParameterizedTest
        @MethodSource("exceptionTypes")
        @DisplayName("Should handle different exception types in fallback chain")
        void shouldHandleDifferentExceptionTypesInFallbackChain(
                String testName, RuntimeException exception) throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(1L, FALLBACK_ENTITY)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);
            primary.setFailureExceptionSupplier(() -> exception);

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(1L, FALLBACK_ENTITY));
        }

        @Test
        @DisplayName("Should handle exception in middle of fallback chain")
        void shouldHandleExceptionInMiddleOfFallbackChain() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> tertiaryData = List.of(
                    new TestEntity(3L, TERTIARY_SUCCESS)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);
            primary.setFailureExceptionSupplier(() -> new RuntimeException(PRIMARY_FAILED));

            TestableInMemoryDataSource<TestEntity> secondary =
                    new TestableInMemoryDataSource<>("secondary", TestEntity.class);
            secondary.enableFailureSimulation(1.0);
            secondary.setFailureExceptionSupplier(() -> new DataSourceException("Secondary failed"));

            InMemoryDataSource<TestEntity> tertiary =
                    new InMemoryDataSource<>("tertiary", TestEntity.class, tertiaryData);

            primary.setFallbackDataSource(secondary);
            secondary.setFallbackDataSource(tertiary);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(3L, TERTIARY_SUCCESS));
        }

        @Test
        @DisplayName("Should handle intermittent failures with retry-like behavior")
        void shouldHandleIntermittentFailuresWithRetryLikeBehavior() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> primaryData = List.of(
                    new TestEntity(1L, PRIMARY_ENTITY)
            );
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(2L, FALLBACK_ENTITY)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class, primaryData);
            primary.failAfterRequests(2); // Fail after 2 successful requests

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When - First two requests should succeed
            List<TestEntity> result1 = primary.fetchAllWithFallback().get();
            List<TestEntity> result2 = primary.fetchAllWithFallback().get();

            // Third request should use fallback due to failure
            List<TestEntity> result3 = primary.fetchAllWithFallback().get();

            // Then
            assertThat(result1).hasSize(1);
            assertThat(result1.get(0)).isEqualTo(new TestEntity(1L, PRIMARY_ENTITY));

            assertThat(result2).hasSize(1);
            assertThat(result2.get(0)).isEqualTo(new TestEntity(1L, PRIMARY_ENTITY));

            assertThat(result3).hasSize(1);
            assertThat(result3.get(0)).isEqualTo(new TestEntity(2L, FALLBACK_ENTITY));
        }
    }

    @Nested
    @DisplayName("Timeout and Async Exception Tests")
    class TimeoutAndAsyncExceptionTests {

        @Test
        @DisplayName("Should handle slow DataSource with timeout")
        void shouldHandleSlowDataSourceWithTimeout() {
            // Given
            List<TestEntity> slowData = List.of(
                    new TestEntity(1L, "Slow Entity")
            );
            DataSource<TestEntity> slowDataSource = DataSourceTestUtils.createSlowDataSource(
                    "slow", TestEntity.class, slowData, 2000); // 2 second delay

            // When & Then - Should timeout when waiting for a shorter duration than the delay
            CompletableFuture<List<TestEntity>> future = slowDataSource.fetchAll();

            assertThatThrownBy(() -> future.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("Should use fallback when primary is slow")
        void shouldUseFallbackWhenPrimaryIsSlow() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fastFallbackData = List.of(
                    new TestEntity(2L, FAST_FALLBACK)
            );

            // Create a DataSource that simulates timeout by never completing
            DataSource<TestEntity> neverCompletingDataSource = new DataSource<TestEntity>() {
                private DataSource<TestEntity> fallback;

                @Override
                public String getName() {
                    return "never-completing";
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAll() {
                    // Return a future that never completes
                    return new CompletableFuture<>();
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    return new CompletableFuture<>();
                }

                @Override
                public boolean isHealthy() {
                    return false;
                } // Mark as unhealthy to trigger fallback

                @Override
                public void close() {
                    // No-op
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

            InMemoryDataSource<TestEntity> fastFallback =
                    new InMemoryDataSource<>("fast-fallback", TestEntity.class, fastFallbackData);

            neverCompletingDataSource.setFallbackDataSource(fastFallback);

            // When
            List<TestEntity> result = neverCompletingDataSource.fetchAllWithFallback().get();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(2L, FAST_FALLBACK));
        }

        @Test
        @DisplayName("Should handle concurrent exceptions gracefully")
        void shouldHandleConcurrentExceptionsGracefully() {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(1L, CONCURRENT_FALLBACK)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(0.8); // 80% failure rate

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When - Multiple concurrent requests
            int threadCount = 10;
            List<CompletableFuture<List<TestEntity>>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(primary.fetchAllWithFallback());
            }

            // Then - All should complete successfully (either primary or fallback)
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            assertThatCode(allFutures::get).doesNotThrowAnyException();

            // Verify all results are valid
            for (CompletableFuture<List<TestEntity>> future : futures) {
                assertThatCode(() -> {
                    List<TestEntity> result = future.get();
                    assertThat(result).isNotNull();
                    // Result should be either empty (primary success with no data) or fallback data
                    if (!result.isEmpty()) {
                        assertThat(result).hasSize(1);
                        assertThat(result.get(0)).isEqualTo(new TestEntity(1L, CONCURRENT_FALLBACK));
                    }
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Edge Case Exception Tests")
    class EdgeCaseExceptionTests {

        @Test
        @DisplayName("Should handle null exception from failure supplier")
        void shouldHandleNullExceptionFromFailureSupplier() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> fallbackData = List.of(
                    new TestEntity(1L, NULL_EXCEPTION_FALLBACK)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);
            primary.setFailureExceptionSupplier(() -> null); // Return null instead of exception

            InMemoryDataSource<TestEntity> fallback =
                    new InMemoryDataSource<>(FALLBACK, TestEntity.class, fallbackData);

            primary.setFallbackDataSource(fallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then - Should still use fallback
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(1L, NULL_EXCEPTION_FALLBACK));
        }

        @Test
        @DisplayName("Should handle exception during fallback DataSource retrieval")
        void shouldHandleExceptionDuringFallbackDataSourceRetrieval() throws ExecutionException, InterruptedException {
            // Given
            DataSource<TestEntity> problematicDataSource = new DataSource<TestEntity>() {
                @Override
                public String getName() {
                    return "problematic";
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAll() {
                    throw new RuntimeException(PRIMARY_FAILED);
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    throw new RuntimeException(PRIMARY_FAILED);
                }

                @Override
                public boolean isHealthy() {
                    return true;
                }

                @Override
                public void close() {
                    // No-op
                }

                @Override
                public Optional<DataSource<TestEntity>> getFallbackDataSource() {
                    throw new RuntimeException("Cannot get fallback DataSource");
                }

                @Override
                public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) {
                    // No-op
                }
            };

            // When
            List<TestEntity> result = problematicDataSource.fetchAllWithFallback().get();

            // Then - Should return empty list for graceful degradation
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle exception in fallback DataSource health check")
        void shouldHandleExceptionInFallbackDataSourceHealthCheck() throws ExecutionException, InterruptedException {
            // Given
            List<TestEntity> finalFallbackData = List.of(
                    new TestEntity(1L, FINAL_FALLBACK)
            );

            TestableInMemoryDataSource<TestEntity> primary =
                    new TestableInMemoryDataSource<>(PRIMARY, TestEntity.class);
            primary.enableFailureSimulation(1.0);

            DataSource<TestEntity> problematicFallback = new DataSource<TestEntity>() {
                private DataSource<TestEntity> fallback;

                @Override
                public String getName() {
                    return "problematic-fallback";
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAll() {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                @Override
                public boolean isHealthy() {
                    throw new RuntimeException("Health check failed");
                }

                @Override
                public void close() {
                    // No-op
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

            InMemoryDataSource<TestEntity> finalFallback =
                    new InMemoryDataSource<>("final-fallback", TestEntity.class, finalFallbackData);

            primary.setFallbackDataSource(problematicFallback);
            problematicFallback.setFallbackDataSource(finalFallback);

            // When
            List<TestEntity> result = primary.fetchAllWithFallback().get();

            // Then - Should skip problematic fallback and use final fallback
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(new TestEntity(1L, FINAL_FALLBACK));
        }
    }

    @Nested
    @DisplayName("Resource Cleanup Exception Tests")
    class ResourceCleanupExceptionTests {

        @Test
        @DisplayName("Should handle exception during close operation")
        void shouldHandleExceptionDuringCloseOperation() {
            // Given
            DataSource<TestEntity> problematicDataSource = new DataSource<TestEntity>() {
                @Override
                public String getName() {
                    return "problematic-close";
                }

                @Override
                public Class<TestEntity> getEntityType() {
                    return TestEntity.class;
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAll() {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                @Override
                public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                @Override
                public boolean isHealthy() {
                    return true;
                }

                @Override
                public void close() {
                    throw new RuntimeException(CLOSE_OPERATION_FAILED);
                }

                @Override
                public Optional<DataSource<TestEntity>> getFallbackDataSource() {
                    return Optional.empty();
                }

                @Override
                public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) {
                    // No-op
                }
            };

            // When & Then - Should not throw exception
            assertThatCode(problematicDataSource::close)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(CLOSE_OPERATION_FAILED);
        }

        @Test
        @DisplayName("Should maintain health status consistency after exceptions")
        void shouldMaintainHealthStatusConsistencyAfterExceptions() {
            // Given
            TestableInMemoryDataSource<TestEntity> dataSource =
                    new TestableInMemoryDataSource<>("consistency-test", TestEntity.class);

            // Initially healthy
            assertThat(dataSource.isHealthy()).isTrue();

            // Enable failure simulation
            dataSource.enableFailureSimulation(1.0);

            // Health should still be true (failure simulation doesn't affect health status)
            assertThat(dataSource.isHealthy()).isTrue();

            // Set unhealthy
            dataSource.setHealthy(false);
            assertThat(dataSource.isHealthy()).isFalse();

            // Close should make it unhealthy and clear data
            dataSource.close();
            assertThat(dataSource.isHealthy()).isFalse();
            assertThat(dataSource.size()).isZero();
        }
    }
}

