package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import com.thy.fss.common.inmemory.testmodel.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test class for CacheDataSource functionality.
 * Tests cache operations, TTL handling, eviction policies, cache hits/misses,
 * and performance with large datasets.
 */
class CacheDataSourceTest {

    private static final String DEFAULT_CACHE = "default-cache";
    private static final String TEST = "test";
    private static final String ENTITY_1 = "Entity 1";
    private static final String ENTITY_2 = "Entity 2";
    private static final String ENTITY_3 = "Entity 3";
    private static final String SINGLE_ENTITY = "Single Entity";
    private static final String FALLBACK = "fallback";
    private static final String OLD_NAME = "Old Name";
    private static final String NEW_NAME = "New Name";
    private static final String ENTITY_PREFIX = "Entity ";
    private static final String ENTITY_2_UPDATED = "Entity 2 Updated";
    private static final String CACHE_SIZE = "cacheSize";
    private static final String NEEDS_FULL_REFRESH = "needsFullRefresh";
    private static final String TTL = "ttl";
    private static final String LAST_FULL_REFRESH = "lastFullRefresh";
    private static final String HEALTHY = "healthy";

    private CacheDataSource<TestEntity> cacheDataSource;
    private LargeDatasetGenerator dataGenerator;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        cacheDataSource = new CacheDataSource<>(
                "test-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMinutes(5)
        );
        dataGenerator = LargeDatasetGenerator.create();
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        if (cacheDataSource != null) {
            cacheDataSource.close();
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("Should create cache data source with default TTL")
    void shouldCreateCacheDataSourceWithDefaultTTL() {
        CacheDataSource<TestEntity> defaultCache = new CacheDataSource<>(
                DEFAULT_CACHE,
                TestEntity.class,
                TestEntity::getId
        );

        assertThat(defaultCache.getName()).isEqualTo(DEFAULT_CACHE);
        assertThat(defaultCache.getEntityType()).isEqualTo(TestEntity.class);
        assertThat(defaultCache.getTtl()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should throw exception for null parameters")
    void shouldThrowExceptionForNullParameters() {
        assertThatThrownBy(() -> new CacheDataSource<>(null, TestEntity.class, TestEntity::getId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DataSource name cannot be null");

        assertThatThrownBy(() -> new CacheDataSource<>(TEST, null, TestEntity::getId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity type cannot be null");

        assertThatThrownBy(() -> new CacheDataSource<>(TEST, TestEntity.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID extractor cannot be null");

        assertThatThrownBy(() -> new CacheDataSource<>(TEST, TestEntity.class, TestEntity::getId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("TTL cannot be null");
    }

    @Test
    @DisplayName("Should cache and retrieve entities")
    void shouldCacheAndRetrieveEntities() throws Exception {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3)
        );

        cacheDataSource.cacheEntities(entities);

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        List<TestEntity> cachedEntities = result.get();

        assertThat(cachedEntities).hasSize(3);
        assertThat(cacheDataSource.getCacheSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should cache single entity")
    void shouldCacheSingleEntity() throws Exception {
        TestEntity entity = new TestEntity(1L, SINGLE_ENTITY);

        cacheDataSource.cacheEntity(entity);

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        List<TestEntity> cachedEntities = result.get();

        assertThat(cachedEntities).hasSize(1);
        assertThat(cachedEntities.get(0).getName()).isEqualTo(SINGLE_ENTITY);
    }

    @Test
    @DisplayName("Should fetch entities by IDs")
    void shouldFetchEntitiesByIds() throws Exception {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3)
        );

        cacheDataSource.cacheEntities(entities);

        Collection<Object> ids = Arrays.asList(1L, 3L);
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(ids);
        List<TestEntity> fetchedEntities = result.get();

        assertThat(fetchedEntities).hasSize(2);
        assertThat(fetchedEntities).extracting(TestEntity::getId).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("Should return empty list for null or empty IDs")
    void shouldReturnEmptyListForNullOrEmptyIds() throws Exception {
        CompletableFuture<List<TestEntity>> result1 = cacheDataSource.fetchAllById(null);
        CompletableFuture<List<TestEntity>> result2 = cacheDataSource.fetchAllById(Collections.emptyList());

        assertThat(result1.get()).isEmpty();
        assertThat(result2.get()).isEmpty();
    }

    @Test
    @DisplayName("Should evict entity from cache")
    void shouldEvictEntityFromCache() throws Exception {
        TestEntity entity = new TestEntity(1L, "Entity to evict");
        cacheDataSource.cacheEntity(entity);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(1);

        cacheDataSource.evictEntity(1L);

        assertThat(cacheDataSource.getCacheSize()).isZero();

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("Should clear all cache")
    void shouldClearAllCache() {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2)
        );

        cacheDataSource.cacheEntities(entities);
        assertThat(cacheDataSource.getCacheSize()).isEqualTo(2);

        cacheDataSource.clearCache();

        assertThat(cacheDataSource.getCacheSize()).isZero();
        assertThat(cacheDataSource.getLastFullRefresh()).isNull();
    }

    @Test
    @DisplayName("Should handle null entities gracefully")
    void shouldHandleNullEntitiesGracefully() {
        cacheDataSource.cacheEntity(null);
        cacheDataSource.cacheEntities(null);
        cacheDataSource.cacheEntities(Collections.emptyList());
        cacheDataSource.evictEntity(null);

        assertThat(cacheDataSource.getCacheSize()).isZero();
    }

    @Test
    @DisplayName("Should track last full refresh time")
    void shouldTrackLastFullRefreshTime() {
        LocalDateTime beforeCache = LocalDateTime.now();

        List<TestEntity> entities = List.of(
                new TestEntity(1L, ENTITY_1)
        );

        cacheDataSource.cacheEntities(entities);

        LocalDateTime afterCache = LocalDateTime.now();
        LocalDateTime lastRefresh = cacheDataSource.getLastFullRefresh();

        assertThat(lastRefresh).isNotNull().isBetween(beforeCache, afterCache);
    }

    @Test
    @DisplayName("Should determine if full refresh is needed")
    void shouldDetermineIfFullRefreshIsNeeded() {
        // Initially should need refresh
        assertThat(cacheDataSource.needsFullRefresh()).isTrue();

        // After caching, should not need refresh
        cacheDataSource.cacheEntities(List.of(new TestEntity(1L, ENTITY_1)));
        assertThat(cacheDataSource.needsFullRefresh()).isFalse();
    }

    @Test
    @DisplayName("Should provide cache statistics")
    void shouldProvideCacheStatistics() {
        cacheDataSource.cacheEntities(Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2)
        ));

        Map<String, Object> stats = cacheDataSource.getCacheStats();

        assertThat(stats).containsKey(CACHE_SIZE)
                .containsKey(TTL)
                .containsKey(LAST_FULL_REFRESH)
                .containsKey(NEEDS_FULL_REFRESH)
                .containsKey(HEALTHY)
                .containsEntry(CACHE_SIZE, 2)
                .containsEntry(HEALTHY, true);
    }

    @Test
    @DisplayName("Should be healthy by default")
    void shouldBeHealthyByDefault() {
        assertThat(cacheDataSource.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should set health status")
    void shouldSetHealthStatus() {
        cacheDataSource.setHealthy(false);
        assertThat(cacheDataSource.isHealthy()).isFalse();

        cacheDataSource.setHealthy(true);
        assertThat(cacheDataSource.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should handle fallback data source")
    void shouldHandleFallbackDataSource() {
        assertThat(cacheDataSource.getFallbackDataSource()).isEmpty();

        InMemoryDataSource<TestEntity> fallback = new InMemoryDataSource<>(FALLBACK, TestEntity.class, Collections.emptyList());
        cacheDataSource.setFallbackDataSource(fallback);

        assertThat(cacheDataSource.getFallbackDataSource()).isPresent();
        assertThat(cacheDataSource.getFallbackDataSource()).contains(fallback);
    }

    @Test
    @DisplayName("Should close properly")
    void shouldCloseProperly() {
        cacheDataSource.cacheEntities(List.of(new TestEntity(1L, ENTITY_1)));
        assertThat(cacheDataSource.getCacheSize()).isEqualTo(1);
        assertThat(cacheDataSource.isHealthy()).isTrue();

        cacheDataSource.close();

        assertThat(cacheDataSource.getCacheSize()).isZero();
        assertThat(cacheDataSource.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should handle entities with null IDs")
    void shouldHandleEntitiesWithNullIds() {
        TestEntity entityWithNullId = new TestEntity(null, "Null ID Entity");

        cacheDataSource.cacheEntity(entityWithNullId);

        assertThat(cacheDataSource.getCacheSize()).isZero(); // Should not cache entities with null IDs
    }

    // ==================== TTL and Expiration Tests ====================

    @Test
    @DisplayName("Should expire entries after TTL")
    void shouldExpireEntriesAfterTTL() throws Exception {
        // Create cache with very short TTL
        CacheDataSource<TestEntity> shortTtlCache = new CacheDataSource<>(
                "short-ttl-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMillis(100)
        );

        try {
            // Cache entities
            List<TestEntity> entities = Arrays.asList(
                    new TestEntity(1L, ENTITY_1),
                    new TestEntity(2L, ENTITY_2)
            );
            shortTtlCache.cacheEntities(entities);

            assertThat(shortTtlCache.getCacheSize()).isEqualTo(2);

            // Wait for TTL to expire
            TestUtil.await(150);

            // Fetch should trigger cleanup and return empty
            CompletableFuture<List<TestEntity>> result = shortTtlCache.fetchAll();
            assertThat(result.get()).isEmpty();
            assertThat(shortTtlCache.getCacheSize()).isZero();
        } finally {
            shortTtlCache.close();
        }
    }

    @Test
    @DisplayName("Should not expire entries with zero TTL")
    void shouldNotExpireEntriesWithZeroTTL() throws Exception {
        CacheDataSource<TestEntity> noExpirationCache = new CacheDataSource<>(
                "no-expiration-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ZERO
        );

        try {
            List<TestEntity> entities = Arrays.asList(
                    new TestEntity(1L, ENTITY_1),
                    new TestEntity(2L, ENTITY_2)
            );
            noExpirationCache.cacheEntities(entities);

            // Wait some time
            TestUtil.await(100);

            // Entities should still be cached
            CompletableFuture<List<TestEntity>> result = noExpirationCache.fetchAll();
            assertThat(result.get()).hasSize(2);
        } finally {
            noExpirationCache.close();
        }
    }

    @Test
    @DisplayName("Should clean only expired entries, keep fresh ones")
    void shouldCleanOnlyExpiredEntries() throws Exception {
        CacheDataSource<TestEntity> cache = new CacheDataSource<>(
                "mixed-ttl-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMillis(200)
        );

        try {
            // Cache first batch
            cache.cacheEntity(new TestEntity(1L, "Old Entity"));

            // Wait half TTL
            TestUtil.await(100);

            // Cache second batch (fresh)
            cache.cacheEntity(new TestEntity(2L, "Fresh Entity"));

            // Wait for first batch to expire
            TestUtil.await(150);

            // Fetch should clean expired but keep fresh
            CompletableFuture<List<TestEntity>> result = cache.fetchAll();
            List<TestEntity> cached = result.get();

            assertThat(cached).hasSize(1);
            assertThat(cached.get(0).getId()).isEqualTo(2L);
        } finally {
            cache.close();
        }
    }

    @Test
    @DisplayName("Should update timestamp when entity is re-cached")
    void shouldUpdateTimestampWhenEntityIsReCached() throws Exception {
        CacheDataSource<TestEntity> cache = new CacheDataSource<>(
                "update-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMillis(200)
        );

        try {
            // Cache entity
            cache.cacheEntity(new TestEntity(1L, ENTITY_1));

            // Wait half TTL
            TestUtil.await(100);

            // Re-cache same entity (updates timestamp)
            cache.cacheEntity(new TestEntity(1L, "Entity 1 Updated"));

            // Wait for original TTL to expire
            TestUtil.await(150);

            // Entity should still be cached (timestamp was updated)
            CompletableFuture<List<TestEntity>> result = cache.fetchAll();
            assertThat(result.get()).hasSize(1);
        } finally {
            cache.close();
        }
    }

    @Test
    @DisplayName("Should indicate full refresh needed when TTL expires")
    void shouldIndicateFullRefreshNeededWhenTTLExpires(){
        CacheDataSource<TestEntity> cache = new CacheDataSource<>(
                "refresh-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMillis(100)
        );

        try {
            // Initially needs refresh
            assertThat(cache.needsFullRefresh()).isTrue();

            // Cache entities
            cache.cacheEntities(List.of(new TestEntity(1L, ENTITY_1)));
            assertThat(cache.needsFullRefresh()).isFalse();

            // Wait for TTL to expire
            TestUtil.await(150);

            // Should need refresh again
            assertThat(cache.needsFullRefresh()).isTrue();
        } finally {
            cache.close();
        }
    }

    // ==================== Cache Hit/Miss Tests ====================

    @Test
    @DisplayName("Should handle cache hits correctly")
    void shouldHandleCacheHitsCorrectly() throws Exception {
        // Cache entities
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3)
        );
        cacheDataSource.cacheEntities(entities);

        // Fetch by IDs - all should be hits
        Collection<Object> ids = Arrays.asList(1L, 2L, 3L);
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(ids);
        List<TestEntity> fetched = result.get();

        assertThat(fetched).hasSize(3);
        assertThat(fetched).extracting(TestEntity::getId).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should handle cache misses correctly")
    void shouldHandleCacheMissesCorrectly() throws Exception {
        // Cache only some entities
        cacheDataSource.cacheEntity(new TestEntity(1L, ENTITY_1));

        // Fetch by IDs including non-existent ones
        Collection<Object> ids = Arrays.asList(1L, 2L, 3L);
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(ids);
        List<TestEntity> fetched = result.get();

        // Only cached entity should be returned
        assertThat(fetched).hasSize(1);
        assertThat(fetched.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle mixed cache hits and misses")
    void shouldHandleMixedCacheHitsAndMisses() throws Exception {
        // Cache some entities
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(3L, ENTITY_3),
                new TestEntity(5L, "Entity 5")
        );
        cacheDataSource.cacheEntities(entities);

        // Fetch mix of cached and non-cached IDs
        Collection<Object> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(ids);
        List<TestEntity> fetched = result.get();

        assertThat(fetched).hasSize(3);
        assertThat(fetched).extracting(TestEntity::getId).containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    // ==================== Eviction Policy Tests ====================

    @Test
    @DisplayName("Should evict single entity by ID")
    void shouldEvictSingleEntityById() throws Exception {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3)
        );
        cacheDataSource.cacheEntities(entities);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(3);

        // Evict one entity
        cacheDataSource.evictEntity(2L);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(2);

        // Verify evicted entity is not in cache
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(Arrays.asList(1L, 2L, 3L));
        List<TestEntity> fetched = result.get();

        assertThat(fetched).hasSize(2);
        assertThat(fetched).extracting(TestEntity::getId).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("Should evict multiple entities")
    void shouldEvictMultipleEntities() throws Exception {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3),
                new TestEntity(4L, "Entity 4"),
                new TestEntity(5L, "Entity 5")
        );
        cacheDataSource.cacheEntities(entities);

        // Evict multiple entities
        cacheDataSource.evictEntity(2L);
        cacheDataSource.evictEntity(4L);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(3);

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        List<TestEntity> remaining = result.get();

        assertThat(remaining).hasSize(3);
        assertThat(remaining).extracting(TestEntity::getId).containsExactlyInAnyOrder(1L, 3L, 5L);
    }

    @Test
    @DisplayName("Should handle eviction of non-existent entity")
    void shouldHandleEvictionOfNonExistentEntity() {
        cacheDataSource.cacheEntity(new TestEntity(1L, ENTITY_1));

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(1);

        // Evict non-existent entity - should not throw exception
        cacheDataSource.evictEntity(999L);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should clear entire cache")
    void shouldClearEntireCache() throws Exception {
        // Cache large number of entities
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entities.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }
        cacheDataSource.cacheEntities(entities);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(1000);

        // Clear cache
        cacheDataSource.clearCache();

        assertThat(cacheDataSource.getCacheSize()).isZero();
        assertThat(cacheDataSource.getLastFullRefresh()).isNull();

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        assertThat(result.get()).isEmpty();
    }

    // ==================== Cache Invalidation Tests ====================

    @Test
    @DisplayName("Should invalidate cache on clear")
    void shouldInvalidateCacheOnClear() {
        cacheDataSource.cacheEntities(Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2)
        ));

        LocalDateTime lastRefresh = cacheDataSource.getLastFullRefresh();
        assertThat(lastRefresh).isNotNull();

        cacheDataSource.clearCache();

        assertThat(cacheDataSource.getLastFullRefresh()).isNull();
        assertThat(cacheDataSource.needsFullRefresh()).isTrue();
    }

    @Test
    @DisplayName("Should invalidate cache on close")
    void shouldInvalidateCacheOnClose() throws Exception {
        cacheDataSource.cacheEntities(Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2)
        ));

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(2);
        assertThat(cacheDataSource.isHealthy()).isTrue();

        cacheDataSource.close();

        assertThat(cacheDataSource.getCacheSize()).isZero();
        assertThat(cacheDataSource.isHealthy()).isFalse();

        // Fetch after close should return empty
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("Should update cache with new data")
    void shouldUpdateCacheWithNewData() throws Exception {
        // Initial cache
        cacheDataSource.cacheEntity(new TestEntity(1L, OLD_NAME));

        CompletableFuture<List<TestEntity>> result1 = cacheDataSource.fetchAll();
        assertThat(result1.get().get(0).getName()).isEqualTo(OLD_NAME);

        // Update cache with new data
        cacheDataSource.cacheEntity(new TestEntity(1L, NEW_NAME));

        CompletableFuture<List<TestEntity>> result2 = cacheDataSource.fetchAll();
        assertThat(result2.get().get(0).getName()).isEqualTo(NEW_NAME);
    }

    // ==================== Large Dataset Tests ====================

    @Test
    @DisplayName("Should handle large dataset - 10K entities")
    void shouldHandleLargeDataset10K() throws Exception {
        // Generate 10K entities
        List<TestEntity> largeDataset = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            largeDataset.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }

        long startTime = System.currentTimeMillis();

        // Cache large dataset
        cacheDataSource.cacheEntities(largeDataset);

        long cacheTime = System.currentTimeMillis() - startTime;

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(10_000);

        // Fetch all
        startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        List<TestEntity> fetched = result.get();
        long fetchTime = System.currentTimeMillis() - startTime;

        assertThat(fetched).hasSize(10_000);

        // Performance assertions - should be fast
        assertThat(cacheTime).isLessThan(1000); // < 1 second to cache
        assertThat(fetchTime).isLessThan(500);  // < 500ms to fetch
    }

    @Test
    @DisplayName("Should handle large dataset fetch by IDs - 10K entities")
    void shouldHandleLargeDatasetFetchByIds() throws Exception {
        // Cache 10K entities
        List<TestEntity> largeDataset = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            largeDataset.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }
        cacheDataSource.cacheEntities(largeDataset);

        // Fetch subset by IDs (1000 IDs)
        List<Object> ids = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ids.add((long) (i * 10)); // Every 10th entity
        }

        long startTime = System.currentTimeMillis();
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(ids);
        List<TestEntity> fetched = result.get();
        long fetchTime = System.currentTimeMillis() - startTime;

        assertThat(fetched).hasSize(1000);
        assertThat(fetchTime).isLessThan(200); // Should be very fast
    }

    @Test
    @DisplayName("Should handle cache eviction on large dataset")
    void shouldHandleCacheEvictionOnLargeDataset() throws Exception {
        // Cache 10K entities
        List<TestEntity> largeDataset = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            largeDataset.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }
        cacheDataSource.cacheEntities(largeDataset);

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(10_000);

        // Evict 1000 entities
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            cacheDataSource.evictEntity((long) i);
        }
        long evictionTime = System.currentTimeMillis() - startTime;

        assertThat(cacheDataSource.getCacheSize()).isEqualTo(9_000);
        assertThat(evictionTime).isLessThan(500); // Should be fast

        // Verify evicted entities are not in cache
        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAllById(Arrays.asList(0L, 500L, 999L));
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("Should handle TTL expiration on large dataset")
    void shouldHandleTTLExpirationOnLargeDataset() throws Exception {
        CacheDataSource<TestEntity> cache = new CacheDataSource<>(
                "large-ttl-cache",
                TestEntity.class,
                TestEntity::getId,
                Duration.ofMillis(100)
        );

        try {
            // Cache 5K entities
            List<TestEntity> largeDataset = new ArrayList<>();
            for (int i = 0; i < 5_000; i++) {
                largeDataset.add(new TestEntity((long) i, ENTITY_PREFIX + i));
            }
            cache.cacheEntities(largeDataset);

            assertThat(cache.getCacheSize()).isEqualTo(5_000);

            // Wait for TTL to expire
            TestUtil.await(150);

            // Fetch should clean all expired entries
            long startTime = System.currentTimeMillis();
            CompletableFuture<List<TestEntity>> result = cache.fetchAll();
            long cleanupTime = System.currentTimeMillis() - startTime;

            assertThat(result.get()).isEmpty();
            assertThat(cache.getCacheSize()).isZero();
            assertThat(cleanupTime).isLessThan(500); // Cleanup should be fast
        } finally {
            cache.close();
        }
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    @DisplayName("Should handle concurrent cache operations")
    void shouldHandleConcurrentCacheOperations() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        long id = (long) threadId * operationsPerThread + j;
                        cacheDataSource.cacheEntity(new TestEntity(id, ENTITY_PREFIX + id));
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(cacheDataSource.getCacheSize()).isEqualTo(threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("Should handle concurrent reads and writes")
    void shouldHandleConcurrentReadsAndWrites() throws Exception {
        // Pre-populate cache
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entities.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }
        cacheDataSource.cacheEntities(entities);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);

        // Start reader threads
        for (int i = 0; i < threadCount / 2; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        cacheDataSource.fetchAll().get();
                    }
                    readSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        // Start writer threads
        for (int i = 0; i < threadCount / 2; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        long id = 1000 + (long) threadId * 100 + j;
                        cacheDataSource.cacheEntity(new TestEntity(id, "New Entity " + id));
                    }
                    writeSuccessCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(readSuccessCount.get()).isEqualTo(threadCount / 2);
        assertThat(writeSuccessCount.get()).isEqualTo(threadCount / 2);
        assertThat(cacheDataSource.getCacheSize()).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("Should handle concurrent evictions")
    void shouldHandleConcurrentEvictions() throws Exception {
        // Pre-populate cache
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entities.add(new TestEntity((long) i, ENTITY_PREFIX + i));
        }
        cacheDataSource.cacheEntities(entities);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        long id = (long) threadId * 100 + j;
                        cacheDataSource.evictEntity(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(cacheDataSource.getCacheSize()).isZero();
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle ID extraction failure gracefully")
    void shouldHandleIdExtractionFailureGracefully() {
        CacheDataSource<TestEntity> cache = new CacheDataSource<>(
                "error-cache",
                TestEntity.class,
                entity -> {
                    if (entity.getId() == null) {
                        throw new RuntimeException("ID extraction failed");
                    }
                    return entity.getId();
                },
                Duration.ofMinutes(5)
        );

        try {
            // Entity with null ID should not cause exception
            cache.cacheEntity(new TestEntity(null, "Null ID"));

            assertThat(cache.getCacheSize()).isZero();

            // Valid entity should still work
            cache.cacheEntity(new TestEntity(1L, "Valid Entity"));

            assertThat(cache.getCacheSize()).isEqualTo(1);
        } finally {
            cache.close();
        }
    }

    @Test
    @DisplayName("Should provide accurate cache statistics")
    void shouldProvideAccurateCacheStatistics() {
        // Empty cache stats
        Map<String, Object> emptyStats = cacheDataSource.getCacheStats();
        assertThat(emptyStats).containsEntry(CACHE_SIZE, 0).containsEntry(NEEDS_FULL_REFRESH, true).containsEntry("healthy", true);

        // Cache some entities
        cacheDataSource.cacheEntities(Arrays.asList(
                new TestEntity(1L, ENTITY_1),
                new TestEntity(2L, ENTITY_2),
                new TestEntity(3L, ENTITY_3)
        ));

        Map<String, Object> populatedStats = cacheDataSource.getCacheStats();
        assertThat(populatedStats).containsEntry(CACHE_SIZE, 3)
                .containsEntry(NEEDS_FULL_REFRESH, false).containsEntry(TTL, Duration.ofMinutes(5).toString());
        assertThat(populatedStats.get(LAST_FULL_REFRESH)).isNotNull();
    }

    @Test
    @DisplayName("Should maintain cache integrity after multiple operations")
    void shouldMaintainCacheIntegrityAfterMultipleOperations() throws Exception {
        // Perform multiple operations
        cacheDataSource.cacheEntity(new TestEntity(1L, ENTITY_1));
        cacheDataSource.cacheEntity(new TestEntity(2L, ENTITY_2));
        cacheDataSource.evictEntity(1L);
        cacheDataSource.cacheEntity(new TestEntity(3L, ENTITY_3));
        cacheDataSource.cacheEntity(new TestEntity(2L, ENTITY_2_UPDATED));

        CompletableFuture<List<TestEntity>> result = cacheDataSource.fetchAll();
        List<TestEntity> cached = result.get();

        assertThat(cached).hasSize(2);
        assertThat(cached).extracting(TestEntity::getId).containsExactlyInAnyOrder(2L, 3L);
        assertThat(cached).extracting(TestEntity::getName).contains(ENTITY_2_UPDATED, ENTITY_3);
    }

    @Test
    @DisplayName("Should handle fallback datasource integration")
    void shouldHandleFallbackDatasourceIntegration() {
        // Create fallback datasource
        InMemoryDataSource<TestEntity> fallback = new InMemoryDataSource<>(
                FALLBACK,
                TestEntity.class,
                Arrays.asList(
                        new TestEntity(1L, "Fallback Entity 1"),
                        new TestEntity(2L, "Fallback Entity 2")
                )
        );

        cacheDataSource.setFallbackDataSource(fallback);

        assertThat(cacheDataSource.getFallbackDataSource()).isPresent().contains(fallback);
        assertThat(cacheDataSource.getFallbackDataSource().get().getName()).isEqualTo(FALLBACK);

        fallback.close();
    }

    @Test
    @DisplayName("Should handle cache with User model from testmodel package")
    void shouldHandleCacheWithUserModel() throws Exception {
        List<User> users = dataGenerator.generateUsers(1000);

        CacheDataSource<User> userCache = new CacheDataSource<>(
                "user-cache",
                User.class,
                User::getName,
                Duration.ofMinutes(10)
        );

        try {
            userCache.cacheEntities(users);

            assertThat(userCache.getCacheSize()).isEqualTo(1000);

            CompletableFuture<List<User>> result = userCache.fetchAll();
            List<User> cachedUsers = result.get();

            assertThat(cachedUsers).hasSize(1000);
            assertThat(cachedUsers.get(0)).isInstanceOf(User.class);
        } finally {
            userCache.close();
        }
    }

    // Test entity class
    public static class TestEntity {
        private Long id;
        private String name;

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
    }
}