package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.RelatedEntityLookup;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test for FKLookupCache equivalence.
 *
 * <p>Verifies that cached FK lookups produce identical results to direct (uncached) lookups,
 * and that repeated lookups with the same key are served from cache (hitCount increases).</p>
 *
 * <p>Since {@code FKLookupCache} is a private static inner class of
 * {@link IncrementalSyncProcessor}, reflection is used to instantiate and invoke it.</p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.4</b></p>
 */
class FKLookupCacheEquivalencePropertyTest {

    // Reflection handles for FKLookupCache
    private static final Class<?> CACHE_CLASS;
    private static final Constructor<?> CACHE_CONSTRUCTOR;
    private static final Method GET_OR_LOOKUP;
    private static final Method CLEAR;
    private static final Field HIT_COUNT_FIELD;
    private static final Field MISS_COUNT_FIELD;

    static {
        try {
            CACHE_CLASS = Class.forName(
                    "com.thy.fss.common.inmemory.engine.sync.IncrementalSyncProcessor$FKLookupCache");
            CACHE_CONSTRUCTOR = CACHE_CLASS.getDeclaredConstructor();
            CACHE_CONSTRUCTOR.setAccessible(true);

            GET_OR_LOOKUP = CACHE_CLASS.getDeclaredMethod(
                    "getOrLookup", PropertyMapping.class, List.class, RelatedEntityLookup.class);
            GET_OR_LOOKUP.setAccessible(true);

            CLEAR = CACHE_CLASS.getDeclaredMethod("clear");
            CLEAR.setAccessible(true);

            HIT_COUNT_FIELD = CACHE_CLASS.getDeclaredField("hitCount");
            HIT_COUNT_FIELD.setAccessible(true);

            MISS_COUNT_FIELD = CACHE_CLASS.getDeclaredField("missCount");
            MISS_COUNT_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up FKLookupCache reflection", e);
        }
    }

    // ==================== Arbitraries ====================

    /**
     * Generates random consumer IDs (store identifiers).
     */
    @Provide
    Arbitrary<String> consumerIds() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    /**
     * Generates random datasource names.
     */
    @Provide
    Arbitrary<String> dataSourceNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    /**
     * Generates random primary key value lists (1–3 elements, using Long values).
     */
    @Provide
    Arbitrary<List<Object>> primaryKeyValues() {
        return Arbitraries.longs().between(1L, 10_000L)
                .list().ofMinSize(1).ofMaxSize(3)
                .map(longs -> new ArrayList<>(longs));
    }

    /**
     * Generates random lookup result lists (0–5 string elements simulating entity results).
     */
    @Provide
    Arbitrary<List<Object>> lookupResults() {
        return Arbitraries.strings().alpha().ofLength(8)
                .list().ofMinSize(0).ofMaxSize(5)
                .map(ArrayList::new);
    }

    // ==================== Helpers ====================

    @SuppressWarnings("unchecked")
    private PropertyMapping<?, ?> mockMapping(String consumerId, String dataSourceName) {
        PropertyMapping<?, ?> mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.getDataSourceName()).thenReturn(dataSourceName);
        return mapping;
    }

    private Object newCache() throws Exception {
        return CACHE_CONSTRUCTOR.newInstance();
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeGetOrLookup(Object cache, PropertyMapping<?, ?> mapping,
                                       List<Object> pkValues, RelatedEntityLookup fallback) throws Exception {
        return (List<?>) GET_OR_LOOKUP.invoke(cache, mapping, pkValues, fallback);
    }

    private int getHitCount(Object cache) throws Exception {
        return (int) HIT_COUNT_FIELD.get(cache);
    }

    private int getMissCount(Object cache) throws Exception {
        return (int) MISS_COUNT_FIELD.get(cache);
    }

    private void clearCache(Object cache) throws Exception {
        CLEAR.invoke(cache);
    }

    // ==================== Property Tests ====================

    /**
     * For any PropertyMapping and FK value combination, the cached lookup result
     * must be identical to the direct (uncached) fallback result.
     *
     * <p>This verifies that the cache does not alter, corrupt, or lose data.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.4</b></p>
     */
    @Property(tries = 100)
    void cachedLookupReturnsSameResultAsDirectLookup(
            @ForAll("consumerIds") String consumerId,
            @ForAll("dataSourceNames") String dataSourceName,
            @ForAll("primaryKeyValues") List<Object> pkValues,
            @ForAll("lookupResults") List<Object> expectedResult) throws Exception {

        PropertyMapping<?, ?> mapping = mockMapping(consumerId, dataSourceName);

        // Direct (uncached) lookup — the fallback always returns expectedResult
        RelatedEntityLookup fallback = (m, pk) -> expectedResult;
        List<?> directResult = fallback.lookupRelatedEntities(mapping, pkValues);

        // Cached lookup — first call should delegate to fallback
        Object cache = newCache();
        List<?> cachedResult = invokeGetOrLookup(cache, mapping, pkValues, fallback);

        assertThat(cachedResult)
                .as("Cached lookup must return the same result as direct lookup "
                        + "(consumerId=%s, ds=%s, pk=%s)", consumerId, dataSourceName, pkValues)
                .isEqualTo(directResult);
    }

    /**
     * For any PropertyMapping and FK value combination, the second call with the same key
     * must return from cache (hitCount increases by 1) and produce the same result.
     *
     * <p>This verifies cache hit behavior and result stability.</p>
     *
     * <p><b>Validates: Requirements 3.2, 3.4</b></p>
     */
    @Property(tries = 100)
    void secondCallWithSameKeyReturnsCachedResult(
            @ForAll("consumerIds") String consumerId,
            @ForAll("dataSourceNames") String dataSourceName,
            @ForAll("primaryKeyValues") List<Object> pkValues,
            @ForAll("lookupResults") List<Object> expectedResult) throws Exception {

        PropertyMapping<?, ?> mapping = mockMapping(consumerId, dataSourceName);
        Object cache = newCache();

        // Track fallback invocation count
        int[] fallbackCallCount = {0};
        RelatedEntityLookup fallback = (m, pk) -> {
            fallbackCallCount[0]++;
            return expectedResult;
        };

        // First call — cache miss
        List<?> firstResult = invokeGetOrLookup(cache, mapping, pkValues, fallback);
        int hitCountAfterFirst = getHitCount(cache);
        int missCountAfterFirst = getMissCount(cache);

        assertThat(missCountAfterFirst).as("First call should be a cache miss").isEqualTo(1);
        assertThat(hitCountAfterFirst).as("No hits yet after first call").isEqualTo(0);
        assertThat(fallbackCallCount[0]).as("Fallback should be called once").isEqualTo(1);

        // Second call — same key, should be cache hit
        List<?> secondResult = invokeGetOrLookup(cache, mapping, pkValues, fallback);
        int hitCountAfterSecond = getHitCount(cache);

        assertThat(hitCountAfterSecond)
                .as("hitCount must increase after second call with same key")
                .isEqualTo(1);
        assertThat(fallbackCallCount[0])
                .as("Fallback should NOT be called again on cache hit")
                .isEqualTo(1);
        assertThat(secondResult)
                .as("Second call must return the same result as first call")
                .isEqualTo(firstResult);
    }

    /**
     * For any sequence of distinct mapping/FK combinations, each unique key results in
     * exactly one cache miss, and repeated lookups with the same key always hit the cache.
     *
     * <p>This verifies the cache key strategy correctly distinguishes different combinations
     * and that the cache is consistent across multiple entries.</p>
     *
     * <p><b>Validates: Requirements 3.1, 3.2, 3.4</b></p>
     */
    @Property(tries = 100)
    void cacheDistinguishesDifferentKeysCorrectly(
            @ForAll("consumerIds") String consumerId1,
            @ForAll("consumerIds") String consumerId2,
            @ForAll("dataSourceNames") String dataSourceName,
            @ForAll("primaryKeyValues") List<Object> pkValues,
            @ForAll("lookupResults") List<Object> result1,
            @ForAll("lookupResults") List<Object> result2) throws Exception {

        // Skip if consumer IDs happen to be equal — we need distinct keys
        if (consumerId1.equals(consumerId2)) {
            return;
        }

        PropertyMapping<?, ?> mapping1 = mockMapping(consumerId1, dataSourceName);
        PropertyMapping<?, ?> mapping2 = mockMapping(consumerId2, dataSourceName);
        Object cache = newCache();

        // Lookup for mapping1
        RelatedEntityLookup fallback1 = (m, pk) -> result1;
        List<?> cached1 = invokeGetOrLookup(cache, mapping1, pkValues, fallback1);

        // Lookup for mapping2 — different key, should miss
        RelatedEntityLookup fallback2 = (m, pk) -> result2;
        List<?> cached2 = invokeGetOrLookup(cache, mapping2, pkValues, fallback2);

        assertThat(cached1)
                .as("Cache result for mapping1 must match its fallback result")
                .isEqualTo(result1);
        assertThat(cached2)
                .as("Cache result for mapping2 must match its fallback result")
                .isEqualTo(result2);

        // Repeat mapping1 — should hit cache
        int hitsBefore = getHitCount(cache);
        List<?> cached1Again = invokeGetOrLookup(cache, mapping1, pkValues, fallback1);
        int hitsAfter = getHitCount(cache);

        assertThat(hitsAfter).as("hitCount must increase for repeated key").isEqualTo(hitsBefore + 1);
        assertThat(cached1Again).as("Repeated lookup must return same result").isEqualTo(result1);
    }
}
