package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.RelatedEntityLookup;

/**
 * Unit tests for {@code FKLookupCache} (private static inner class of
 * {@link IncrementalSyncProcessor}).
 *
 * <p>Uses reflection to access the private class, following the same pattern as
 * {@link FKLookupCacheEquivalencePropertyTest}.</p>
 *
 * <p>Validates: Requirements 3.1, 3.2, 3.3, 3.4</p>
 */
class FKLookupCacheTest {

    private static final String STORE1 = "store1";
    private static final String DS1 = "ds1";

    // Reflection handles
    private static final Class<?> CACHE_CLASS;
    private static final Constructor<?> CACHE_CONSTRUCTOR;
    private static final Method GET_OR_LOOKUP;
    private static final Method CLEAR;
    private static final Method GET_HIT_RATE;
    private static final Field HIT_COUNT_FIELD;
    private static final Field MISS_COUNT_FIELD;
    private static final Field CACHE_MAP_FIELD;

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

            GET_HIT_RATE = CACHE_CLASS.getDeclaredMethod("getHitRate");
            GET_HIT_RATE.setAccessible(true);

            HIT_COUNT_FIELD = CACHE_CLASS.getDeclaredField("hitCount");
            HIT_COUNT_FIELD.setAccessible(true);

            MISS_COUNT_FIELD = CACHE_CLASS.getDeclaredField("missCount");
            MISS_COUNT_FIELD.setAccessible(true);

            CACHE_MAP_FIELD = CACHE_CLASS.getDeclaredField("cache");
            CACHE_MAP_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up FKLookupCache reflection", e);
        }
    }

    private Object cache;

    @BeforeEach
    void setUp() throws Exception {
        cache = CACHE_CONSTRUCTOR.newInstance();
    }

    // ==================== Helpers ====================

    private PropertyMapping<?, ?> mockMapping(String consumerId, String dataSourceName) {
        PropertyMapping<?, ?> mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(consumerId);
        when(mapping.getDataSourceName()).thenReturn(dataSourceName);
        return mapping;
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeGetOrLookup(PropertyMapping<?, ?> mapping,
                                       List<Object> pkValues,
                                       RelatedEntityLookup fallback) throws Exception {
        return (List<?>) GET_OR_LOOKUP.invoke(cache, mapping, pkValues, fallback);
    }

    private void invokeClear() throws Exception {
        CLEAR.invoke(cache);
    }

    private double invokeGetHitRate() throws Exception {
        return (double) GET_HIT_RATE.invoke(cache);
    }

    private int getHitCount() throws Exception {
        return (int) HIT_COUNT_FIELD.get(cache);
    }

    private int getMissCount() throws Exception {
        return (int) MISS_COUNT_FIELD.get(cache);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, List<?>> getCacheMap() throws Exception {
        return (java.util.Map<String, List<?>>) CACHE_MAP_FIELD.get(cache);
    }

    private List<Object> pkValues(Object... values) {
        List<Object> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }

    // ==================== Cache Miss → Fallback Tests ====================

    /**
     * A cache miss must invoke the fallback exactly once and return its result.
     * Validates: Requirement 3.1
     */
    @Test
    void cacheMissInvokesFallbackExactlyOnce() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping(STORE1, DS1);
        List<Object> pkValues = pkValues(1L, 2L);
        List<Object> expected = List.of("entityA", "entityB");

        int[] callCount = {0};
        RelatedEntityLookup fallback = (m, pk) -> {
            callCount[0]++;
            return expected;
        };

        List<?> result = invokeGetOrLookup(mapping, pkValues, fallback);

        assertThat(callCount[0]).as("Fallback should be called exactly once on miss").isEqualTo(1);
        assertThat(result).isEqualTo(expected);
        assertThat(getMissCount()).isEqualTo(1);
        assertThat(getHitCount()).isEqualTo(0);
    }

    /**
     * A cache hit must NOT invoke the fallback — it returns the cached result.
     * Validates: Requirement 3.2
     */
    @Test
    void cacheHitDoesNotInvokeFallback() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping(STORE1, DS1);
        List<Object> pkValues = pkValues(42L);
        List<Object> expected = List.of("entity1");

        int[] callCount = {0};
        RelatedEntityLookup fallback = (m, pk) -> {
            callCount[0]++;
            return expected;
        };

        // First call — miss
        invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(callCount[0]).isEqualTo(1);

        // Second call — hit, fallback should NOT be called again
        List<?> result = invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(callCount[0]).as("Fallback must not be called on cache hit").isEqualTo(1);
        assertThat(result).isEqualTo(expected);
        assertThat(getHitCount()).isEqualTo(1);
    }

    // ==================== clear() Tests ====================

    /**
     * After clear(), the cache is empty — next call with the same key is a miss again.
     * Validates: Requirement 3.3
     */
    @Test
    void clearEmptiesCacheAndResetsCounters() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping(STORE1, DS1);
        List<Object> pkValues = pkValues(100L);
        List<Object> expected = List.of("x");

        int[] callCount = {0};
        RelatedEntityLookup fallback = (m, pk) -> {
            callCount[0]++;
            return expected;
        };

        // Populate cache
        invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(callCount[0]).isEqualTo(1);
        assertThat(getCacheMap()).isNotEmpty();

        // Clear
        invokeClear();

        assertThat(getCacheMap()).isEmpty();
        assertThat(getHitCount()).isEqualTo(0);
        assertThat(getMissCount()).isEqualTo(0);

        // Next call should be a miss again (fallback called)
        invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(callCount[0]).as("Fallback should be called again after clear").isEqualTo(2);
        assertThat(getMissCount()).isEqualTo(1);
    }

    // ==================== getHitRate() Tests ====================

    /**
     * getHitRate() returns 0.0 when no lookups have been performed.
     * Validates: Requirement 3.4
     */
    @Test
    void getHitRateReturnsZeroWhenNoLookups() throws Exception {
        assertThat(invokeGetHitRate()).isEqualTo(0.0);
    }

    /**
     * getHitRate() returns correct percentage: 1 hit / 2 total = 50.0%.
     * Validates: Requirement 3.4
     */
    @Test
    void getHitRateCalculatesCorrectPercentage() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping("s", "d");
        List<Object> pkValues = pkValues(1L);
        RelatedEntityLookup fallback = (m, pk) -> List.of("result");

        // 1 miss
        invokeGetOrLookup(mapping, pkValues, fallback);
        // 1 hit
        invokeGetOrLookup(mapping, pkValues, fallback);

        // 1 hit / 2 total = 50.0%
        assertThat(invokeGetHitRate()).isCloseTo(50.0, within(0.001));
    }

    /**
     * getHitRate() with all misses returns 0.0%.
     */
    @Test
    void getHitRateAllMissesReturnsZero() throws Exception {
        PropertyMapping<?, ?> mapping1 = mockMapping("s1", "d1");
        PropertyMapping<?, ?> mapping2 = mockMapping("s2", "d2");
        RelatedEntityLookup fallback = (m, pk) -> List.of();

        invokeGetOrLookup(mapping1, pkValues(1L), fallback);
        invokeGetOrLookup(mapping2, pkValues(2L), fallback);

        // 0 hits / 2 total = 0.0%
        assertThat(invokeGetHitRate()).isCloseTo(0.0, within(0.001));
    }

    /**
     * getHitRate() with 2 hits out of 3 total = 66.67%.
     */
    @Test
    void getHitRateTwoHitsOutOfThree() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping("s", "d");
        List<Object> pkValues = pkValues(5L);
        RelatedEntityLookup fallback = (m, pk) -> List.of("r");

        invokeGetOrLookup(mapping, pkValues, fallback); // miss
        invokeGetOrLookup(mapping, pkValues, fallback); // hit
        invokeGetOrLookup(mapping, pkValues, fallback); // hit

        // 2 hits / 3 total = 66.67%
        assertThat(invokeGetHitRate()).isCloseTo(66.667, within(0.01));
    }

    // ==================== Empty FK Values Edge Cases ====================

    /**
     * Empty primary key values list works correctly — cache key is still valid.
     * Validates: Requirement 3.4
     */
    @Test
    void emptyPrimaryKeyValuesWorksCorrectly() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping(STORE1, DS1);
        List<Object> emptyPk = new ArrayList<>();
        List<Object> expected = List.of("found");

        RelatedEntityLookup fallback = (m, pk) -> expected;

        List<?> result = invokeGetOrLookup(mapping, emptyPk, fallback);
        assertThat(result).isEqualTo(expected);

        // Second call with same empty PK should hit cache
        List<?> result2 = invokeGetOrLookup(mapping, emptyPk, fallback);
        assertThat(result2).isEqualTo(expected);
        assertThat(getHitCount()).isEqualTo(1);
    }

    /**
     * Fallback returning empty list is cached correctly.
     */
    @Test
    void fallbackReturningEmptyListIsCachedCorrectly() throws Exception {
        PropertyMapping<?, ?> mapping = mockMapping(STORE1, DS1);
        List<Object> pkValues = pkValues(99L);

        int[] callCount = {0};
        RelatedEntityLookup fallback = (m, pk) -> {
            callCount[0]++;
            return Collections.emptyList();
        };

        List<?> result1 = invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(result1).isEmpty();

        List<?> result2 = invokeGetOrLookup(mapping, pkValues, fallback);
        assertThat(result2).isEmpty();
        assertThat(callCount[0]).as("Fallback should only be called once even for empty results").isEqualTo(1);
        assertThat(getHitCount()).isEqualTo(1);
    }
}
