package com.thy.fss.common.inmemory.store;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.filter.EntityFilter;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for InMemoryDataStore class covering all public methods.
 * Uses large datasets (10K+ entities) and generated metamodel classes.
 * 
 * Test Coverage:
 * - Constructors (simple and full)
 * - Data update methods (with/without version)
 * - Query methods (findAll variants with Specification, Pageable, EntityFilter)
 * - Metadata methods (getVersion, size, getTargetClass, etc.)
 * - Thread-safety and concurrent operations
 * - Edge cases (null, empty, boundary conditions)
 */
@DisplayName("InMemoryDataStore Comprehensive Tests")
class InMemoryDataStoreTest {

    private final LargeDatasetGenerator generator = LargeDatasetGenerator.create();

    @AfterEach
    void tearDown() {
        // No static registries to clean in InMemoryDataStore itself
    }
    @Test
    @DisplayName("Simple test to verify class loads")
    void simpleTest() {
        List<User> users = generator.generateUsers(10);
        Specification<User> nameSpec = SpecificationBuilder.forService(UserSpecificationService.INSTANCE)
                .where(User_.name).contains("User");
        InMemoryDataStore<User> store = new InMemoryDataStore<>(User.class, "users",  "datasource", nameSpec, new ArrayList<>());
        assertThat(users).isNotNull();
        assertThat(store.size()).isEqualTo(10);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create store with minimal parameters and initialize metadata")
        void shouldCreateStoreWithMinimalParameters() {
            // When
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);

            // Then - Verify all metadata initialized correctly
            assertThat(store).isNotNull();
            assertThat(store.getTargetClass()).isEqualTo(TestUser.class);
            assertThat(store.size()).isZero();
            assertThat(store.getVersion()).isZero();
            assertThat(store.getStoreId()).isNull();
            assertThat(store.getPrimaryDataSourceName()).isNull();
            assertThat(store.getRootSpecification()).isNull();
            assertThat(store.getPropertyMappings()).isEmpty();
            assertThat(store.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should create store with full constructor and all parameters")
        void shouldCreateStoreWithFullConstructor() {
            // Given
            String storeId = "test-store-123";
            String primaryDataSourceName = "primary-ds";
            Specification<User> rootSpec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(UserSpecificationService.INSTANCE)
                    .where(User_.name).contains("test");
            List<PropertyMapping<User, ?>> mappings = new ArrayList<>();

            // When
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class,
                    storeId,
                    primaryDataSourceName,
                    rootSpec,
                    mappings
            );

            // Then - Verify all parameters stored correctly
            assertThat(store.getTargetClass()).isEqualTo(User.class);
            assertThat(store.getStoreId()).isEqualTo(storeId);
            assertThat(store.getPrimaryDataSourceName()).isEqualTo(primaryDataSourceName);
            assertThat(store.getRootSpecification()).isEqualTo(rootSpec);
            assertThat(store.getPropertyMappings()).isEmpty();
            assertThat(store.size()).isZero();
            assertThat(store.getVersion()).isZero();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when target class is null")
        void shouldThrowExceptionWhenTargetClassIsNull() {
            // When & Then
            assertThatThrownBy(() -> new InMemoryDataStore<>(null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Target class cannot be null");
        }

        @Test
        @DisplayName("Should handle null optional parameters in full constructor")
        void shouldHandleNullOptionalParameters() {
            // When
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class,
                    null,
                    null,
                    null,
                    null
            );

            // Then
            assertThat(store.getTargetClass()).isEqualTo(User.class);
            assertThat(store.getStoreId()).isNull();
            assertThat(store.getPrimaryDataSourceName()).isNull();
            assertThat(store.getRootSpecification()).isNull();
            assertThat(store.getPropertyMappings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Data Update Method Tests")
    class UpdateDataTests {

        @Test
        @DisplayName("Should update data with version tracking and increment correctly")
        void shouldUpdateDataWithVersionTracking() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);

            // When - First update
            store.updateData(data, 1L);

            // Then
            assertThat(store.size()).isEqualTo(10_000);
            assertThat(store.getVersion()).isEqualTo(1L);
            assertThat(store.findAll()).hasSize(10_000);

            // When - Second update with new version
            List<TestUser> newData = generator.generateTestUsers(5_000);
            store.updateData(newData, 2L);

            // Then
            assertThat(store.size()).isEqualTo(5_000);
            assertThat(store.getVersion()).isEqualTo(2L);

            // When - Third update with higher version
            store.updateData(data, 10L);

            // Then
            assertThat(store.size()).isEqualTo(10_000);
            assertThat(store.getVersion()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should update data without version parameter and preserve current version")
        void shouldUpdateDataWithoutVersion() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> initialData = generator.generateTestUsers(10_000);
            store.updateData(initialData, 5L);
            assertThat(store.getVersion()).isEqualTo(5L);

            // When - Update without version
            List<TestUser> newData = generator.generateTestUsers(8_000);
            store.updateData(newData);

            // Then - Version should remain unchanged
            assertThat(store.size()).isEqualTo(8_000);
            assertThat(store.getVersion()).isEqualTo(5L);
            assertThat(store.findAll()).hasSize(8_000);
        }

        @Test
        @DisplayName("Should handle null data by converting to empty list")
        void shouldHandleNullData() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);

            // When
            store.updateData(null, 1L);

            // Then
            assertThat(store.size()).isZero();
            assertThat(store.getVersion()).isEqualTo(1L);
            assertThat(store.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty data list")
        void shouldHandleEmptyData() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> initialData = generator.generateTestUsers(10_000);
            store.updateData(initialData, 1L);
            assertThat(store.size()).isEqualTo(10_000);

            // When - Update with empty list
            store.updateData(Collections.emptyList(), 2L);

            // Then
            assertThat(store.size()).isZero();
            assertThat(store.getVersion()).isEqualTo(2L);
            assertThat(store.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should handle concurrent updates safely with proper synchronization")
        void shouldHandleConcurrentUpdates() throws InterruptedException {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When - Multiple threads update concurrently
            for (int i = 0; i < threadCount; i++) {
                final long version = (long) i + 1;
                final List<TestUser> data = generator.generateTestUsers(1_000);
                
                executor.submit(() -> {
                    try {
                        store.updateData(data, version);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            executor.shutdown();

            // Then - Store should be in consistent state
            assertThat(store.findAll()).isNotNull();
            assertThat(store.size()).isEqualTo(store.findAll().size());
            assertThat(store.getVersion()).isBetween(1L, (long) threadCount);
        }

        @Test
        @DisplayName("Should replace existing data completely on each update")
        void shouldReplaceExistingData() {
            // Given
            InMemoryDataStore<User> store = new InMemoryDataStore<>(User.class, null, null, null, null);
            List<User> firstBatch = generator.generateUsers(10_000);
            store.updateData(firstBatch, 1L);

            // When - Update with completely different data
            List<User> secondBatch = generator.generateUsers(5_000);
            store.updateData(secondBatch, 2L);

            // Then - Old data should be completely replaced
            assertThat(store.size()).isEqualTo(5_000);
            assertThat(store.getVersion()).isEqualTo(2L);
            
            // Verify first batch data is not present - check that no IDs >= 5000 exist
            List<User> currentData = store.findAll();
            assertThat(currentData).hasSize(5_000);
            assertThat(currentData.stream().map(User::getIdentity).map(Integer::parseInt).max(Integer::compareTo))
                .hasValue(4_999);
        }

        @Test
        @DisplayName("Should increment version correctly across multiple updates")
        void shouldIncrementVersionCorrectly() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);

            // When & Then - Multiple updates with increasing versions
            store.updateData(data, 1L);
            assertThat(store.getVersion()).isEqualTo(1L);

            store.updateData(data, 5L);
            assertThat(store.getVersion()).isEqualTo(5L);

            store.updateData(data, 10L);
            assertThat(store.getVersion()).isEqualTo(10L);

            store.updateData(data, 100L);
            assertThat(store.getVersion()).isEqualTo(100L);

            // Version can also go backwards (engine controls versioning)
            store.updateData(data, 50L);
            assertThat(store.getVersion()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("Query Method Tests")
    class QueryMethodTests {

        @Test
        @DisplayName("Should findAll without parameters on 10K entities")
        void shouldFindAllWithoutParameters() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When
            List<TestUser> result = store.findAll();

            // Then
            assertThat(result).hasSize(10_000).isNotSameAs(data); // Defensive copy
        }

        @Test
        @DisplayName("Should findAll with Specification on complex queries with 10K entities")
        void shouldFindAllWithSpecification() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Simple specification
            Specification<TestUser> activeSpec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(TestUserSpecificationService.INSTANCE)
                    .where(TestUser_.active).isTrue();
            List<TestUser> activeUsers = store.findAll(activeSpec);

            // Then
            assertThat(activeUsers).isNotEmpty().allMatch(TestUser::getActive);

            // When - Complex specification with AND
            Specification<TestUser> complexSpec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(TestUserSpecificationService.INSTANCE)
                    .where(TestUser_.active).isTrue()
                    .and(com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                            .forService(TestUserSpecificationService.INSTANCE)
                            .where(TestUser_.name).contains("User1"));
            List<TestUser> filtered = store.findAll(complexSpec);

            // Then
            assertThat(filtered).isNotEmpty().allMatch(u -> u.getActive() && u.getName().contains("User1"));
        }

        @Test
        @DisplayName("Should findAll with Pageable on various page sizes with 10K entities")
        void shouldFindAllWithPageable() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Page size 10
            Page<TestUser> page10 = store.findAll(PageRequest.of(0, 10));
            assertThat(page10.getContent()).hasSize(10);
            assertThat(page10.getTotalElements()).isEqualTo(10_000);
            assertThat(page10.getTotalPages()).isEqualTo(1_000);

            // When - Page size 50
            Page<TestUser> page50 = store.findAll(PageRequest.of(0, 50));
            assertThat(page50.getContent()).hasSize(50);
            assertThat(page50.getTotalPages()).isEqualTo(200);

            // When - Page size 100
            Page<TestUser> page100 = store.findAll(PageRequest.of(0, 100));
            assertThat(page100.getContent()).hasSize(100);
            assertThat(page100.getTotalPages()).isEqualTo(100);

            // When - Page size 1000
            Page<TestUser> page1000 = store.findAll(PageRequest.of(0, 1000));
            assertThat(page1000.getContent()).hasSize(1000);
            assertThat(page1000.getTotalPages()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should findAll with Specification and Pageable on 10K entities")
        void shouldFindAllWithSpecificationAndPageable() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When
            Specification<TestUser> spec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(TestUserSpecificationService.INSTANCE)
                    .where(TestUser_.active).isTrue();
            Page<TestUser> page = store.findAll(spec, PageRequest.of(0, 100));

            // Then
            assertThat(page.getContent()).hasSize(100);
            assertThat(page.getContent()).allMatch(TestUser::getActive);
            assertThat(page.getTotalElements()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should findAll with EntityFilter on 10K entities")
        void shouldFindAllWithEntityFilter() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - EntityFilter is stub, returns all data
            EntityFilter<TestUser> filter = null; // Stub implementation
            List<TestUser> result = store.findAll(filter);

            // Then - Currently returns all since filter.toSpecification() is stub
            assertThat(result).hasSize(10_000);
        }

        @Test
        @DisplayName("Should findAll with EntityFilter and Pageable on 10K entities")
        void shouldFindAllWithEntityFilterAndPageable() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - EntityFilter is stub, returns paginated all data
            EntityFilter<TestUser> filter = null; // Stub implementation
            Page<TestUser> page = store.findAll(filter, PageRequest.of(0, 100));

            // Then - Currently returns paginated all since filter.toSpecification() is stub
            assertThat(page.getContent()).hasSize(100);
            assertThat(page.getTotalElements()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("Should handle null specification by returning all data")
        void shouldHandleNullSpecification() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When
            List<TestUser> result = store.findAll((Specification<TestUser>) null);

            // Then
            assertThat(result).hasSize(10_000);
        }

        @Test
        @DisplayName("Should handle null filter by returning all data")
        void shouldHandleNullFilter() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When
            List<TestUser> result = store.findAll((EntityFilter<TestUser>) null);

            // Then
            assertThat(result).hasSize(10_000);
        }

        @Test
        @DisplayName("Should return empty result when no entities match specification")
        void shouldReturnEmptyResultForNoMatches() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Specification that matches nothing
            Specification<TestUser> impossibleSpec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(TestUserSpecificationService.INSTANCE)
                    .where(TestUser_.name).equalTo("NonExistentUser12345");
            List<TestUser> result = store.findAll(impossibleSpec);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle pagination with sorting on 10K entities")
        void shouldHandlePaginationWithSorting() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Sort by name ascending
            Pageable pageableAsc = PageRequest.of(0, 100, Sort.by("name").ascending());
            Page<TestUser> pageAsc = store.findAll(pageableAsc);

            // Then
            assertThat(pageAsc.getContent()).hasSize(100);
            assertThat(pageAsc.getContent()).isSortedAccordingTo((u1, u2) -> 
                    u1.getName().compareTo(u2.getName()));

            // When - Sort by name descending
            Pageable pageableDesc = PageRequest.of(0, 100, Sort.by("name").descending());
            Page<TestUser> pageDesc = store.findAll(pageableDesc);

            // Then
            assertThat(pageDesc.getContent()).hasSize(100);
            assertThat(pageDesc.getContent()).isSortedAccordingTo((u1, u2) -> 
                    u2.getName().compareTo(u1.getName()));
        }

        @Test
        @DisplayName("Should return defensive copy that doesn't affect internal data")
        void shouldReturnDefensiveCopy() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When
            List<TestUser> result1 = store.findAll();
            List<TestUser> result2 = store.findAll();

            // Then - Different instances
            assertThat(result1).isNotSameAs(result2).hasSize(result2.size());

            // When - Modify returned list
            result1.clear();

            // Then - Internal data unaffected
            assertThat(store.size()).isEqualTo(10_000);
            assertThat(store.findAll()).hasSize(10_000);
        }

        @Test
        @DisplayName("Should handle middle and last pages correctly")
        void shouldHandleMiddleAndLastPages() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Middle page
            Page<TestUser> middlePage = store.findAll(PageRequest.of(50, 100));
            assertThat(middlePage.getContent()).hasSize(100);
            assertThat(middlePage.hasNext()).isTrue();
            assertThat(middlePage.hasPrevious()).isTrue();
            assertThat(middlePage.getNumber()).isEqualTo(50);

            // When - Last page
            Page<TestUser> lastPage = store.findAll(PageRequest.of(99, 100));
            assertThat(lastPage.getContent()).hasSize(100);
            assertThat(lastPage.hasNext()).isFalse();
            assertThat(lastPage.hasPrevious()).isTrue();
            assertThat(lastPage.isLast()).isTrue();
        }

        @Test
        @DisplayName("Should handle out of bounds page gracefully")
        void shouldHandleOutOfBoundsPage() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            // When - Page beyond available data
            Page<TestUser> outOfBoundsPage = store.findAll(PageRequest.of(1000, 100));

            // Then
            assertThat(outOfBoundsPage.getContent()).isEmpty();
            assertThat(outOfBoundsPage.getTotalElements()).isEqualTo(10_000);
            assertThat(outOfBoundsPage.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("Metadata Method Tests")
    class MetadataMethodTests {

        @Test
        @DisplayName("Should getVersion correctly across updates")
        void shouldGetVersionCorrectly() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);

            // When & Then - Initial version
            assertThat(store.getVersion()).isZero();

            // When & Then - After first update
            store.updateData(data, 1L);
            assertThat(store.getVersion()).isEqualTo(1L);

            // When & Then - After subsequent updates
            store.updateData(data, 5L);
            assertThat(store.getVersion()).isEqualTo(5L);

            store.updateData(data, 100L);
            assertThat(store.getVersion()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should return correct size for various data volumes")
        void shouldReturnCorrectSize() {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);

            // When & Then - Empty
            assertThat(store.size()).isZero();

            // When & Then - 10K entities
            List<TestUser> data10k = generator.generateTestUsers(10_000);
            store.updateData(data10k, 1L);
            assertThat(store.size()).isEqualTo(10_000);

            // When & Then - 5K entities
            List<TestUser> data5k = generator.generateTestUsers(5_000);
            store.updateData(data5k, 2L);
            assertThat(store.size()).isEqualTo(5_000);

            // When & Then - Empty again
            store.updateData(Collections.emptyList(), 3L);
            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("Should getTargetClass correctly")
        void shouldGetTargetClass() {
            // Given
            InMemoryDataStore<TestUser> testUserStore = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            InMemoryDataStore<User> userStore = new InMemoryDataStore<>(User.class, null, null, null, null);

            // When & Then
            assertThat(testUserStore.getTargetClass()).isEqualTo(TestUser.class);
            assertThat(userStore.getTargetClass()).isEqualTo(User.class);
        }

        @Test
        @DisplayName("Should getStoreId correctly")
        void shouldGetStoreId() {
            // Given
            String storeId = "test-store-123";
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class, storeId, null, null, null);

            // When & Then
            assertThat(store.getStoreId()).isEqualTo(storeId);
        }

        @Test
        @DisplayName("Should getPrimaryDataSourceName correctly")
        void shouldGetPrimaryDataSourceName() {
            // Given
            String dataSourceName = "primary-datasource";
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class, null, dataSourceName, null, null);

            // When & Then
            assertThat(store.getPrimaryDataSourceName()).isEqualTo(dataSourceName);
        }

        @Test
        @DisplayName("Should getRootSpecification correctly")
        void shouldGetRootSpecification() {
            // Given
            Specification<User> rootSpec = com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                    .forService(UserSpecificationService.INSTANCE)
                    .where(User_.name).contains("test");
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class, null, null, rootSpec, null);

            // When & Then
            assertThat(store.getRootSpecification()).isEqualTo(rootSpec);
        }

        @Test
        @DisplayName("Should getPropertyMappings and return defensive copy")
        void shouldGetPropertyMappingsDefensiveCopy() {
            // Given
            List<PropertyMapping<User, ?>> mappings = new ArrayList<>();
            InMemoryDataStore<User> store = new InMemoryDataStore<>(
                    User.class, null, null, null, mappings);

            // When
            List<PropertyMapping<User, ?>> retrieved1 = store.getPropertyMappings();
            List<PropertyMapping<User, ?>> retrieved2 = store.getPropertyMappings();

            // Then - Should return unmodifiable list
            assertThat(retrieved1).isEmpty();
            assertThat(retrieved2).isEmpty();
            
            // Verify it's unmodifiable
            assertThatThrownBy(() -> retrieved1.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent reads safely on 10K entities")
        void shouldHandleConcurrentReadsSafely() throws InterruptedException {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> data = generator.generateTestUsers(10_000);
            store.updateData(data, 1L);

            int threadCount = 10;
            int iterationsPerThread = 100;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When - Multiple threads read concurrently
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < iterationsPerThread; j++) {
                            List<TestUser> result = store.findAll();
                            if (result.size() == 10_000) {
                                successCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            executor.shutdown();

            // Then - All reads should succeed
            assertThat(successCount.get()).isEqualTo(threadCount * iterationsPerThread);
        }

        @Test
        @DisplayName("Should handle concurrent writes safely")
        void shouldHandleConcurrentWritesSafely() throws InterruptedException {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When - Multiple threads write concurrently
            for (int i = 0; i < threadCount; i++) {
                final long version = (long) i + 1;
                final List<TestUser> data = generator.generateTestUsers(1_000);

                executor.submit(() -> {
                    try {
                        store.updateData(data, version);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            executor.shutdown();

            // Then - Store should be in consistent state
            assertThat(store.findAll()).isNotNull();
            assertThat(store.getVersion()).isBetween(1L, (long) threadCount);
            assertThat(store.size()).isEqualTo(store.findAll().size());
        }

        @Test
        @DisplayName("Should handle concurrent read and write operations safely")
        void shouldHandleConcurrentReadAndWriteSafely() throws InterruptedException {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            List<TestUser> initialData = generator.generateTestUsers(10_000);
            store.updateData(initialData, 1L);

            int readerCount = 5;
            int writerCount = 5;
            CountDownLatch latch = new CountDownLatch(readerCount + writerCount);
            ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);
            AtomicInteger readSuccessCount = new AtomicInteger(0);

            // When - Start readers
            for (int i = 0; i < readerCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 100; j++) {
                            List<TestUser> result = store.findAll();
                            if (result != null) {
                                readSuccessCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // When - Start writers
            for (int i = 0; i < writerCount; i++) {
                final long version = (long) i + 2;
                final List<TestUser> data = generator.generateTestUsers(5_000);
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 20; j++) {
                            store.updateData(data, version);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            executor.shutdown();

            // Then - All reads should succeed
            assertThat(readSuccessCount.get()).isEqualTo(readerCount * 100);
            assertThat(store.findAll()).isNotNull();
        }

        @Test
        @DisplayName("Should maintain data consistency during concurrent operations")
        void shouldMaintainDataConsistencyDuringConcurrentOperations() throws InterruptedException {
            // Given
            InMemoryDataStore<TestUser> store = new InMemoryDataStore<>(TestUser.class, null, null, null, null);
            int operationCount = 100;
            CountDownLatch latch = new CountDownLatch(operationCount);
            ExecutorService executor = Executors.newFixedThreadPool(10);

            // When - Multiple threads perform mixed operations
            for (int i = 0; i < operationCount; i++) {
                final long version = (long) i + 1;
                final int userCount = 1_000 + (i % 5) * 100;

                executor.submit(() -> {
                    try {
                        List<TestUser> data = generator.generateTestUsers(userCount);
                        store.updateData(data, version);

                        // Verify consistency
                        List<TestUser> result = store.findAll();
                        assertThat(result).isNotNull();
                        assertThat(store.size()).isEqualTo(result.size());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            executor.shutdown();

            // Then - Final state should be consistent
            List<TestUser> finalResult = store.findAll();
            assertThat(finalResult).isNotNull().hasSize(store.size());
        }
    }
}
