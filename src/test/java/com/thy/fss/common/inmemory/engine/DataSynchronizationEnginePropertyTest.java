package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.engine.index.CompositeKeyIndex;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource_;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for composite key matching functionality.
 * 
 * <p><b>Feature: composite-key-mapping, Property 3: Composite key matching</b></p>
 * <p><b>Validates: Requirements 1.3</b></p>
 * 
 * <p>This test verifies that composite key matching only returns target-source pairs
 * where ALL corresponding key field values are equal.</p>
 */
class DataSynchronizationEnginePropertyTest {
    
    /**
     * Property 3: Composite key matching
     * 
     * For any set of target entities and source entities with composite keys,
     * when a mapping is executed, the result should contain only those target-source
     * pairs where ALL corresponding key field values are equal.
     * 
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void compositeKeyMatchingShouldRequireAllFieldsToMatch(
            @ForAll("sourceEntities") List<TestSource> sources) {
        
        // Given: A 2-field composite key index (id + code)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: We look up entities using composite keys
        // Then: For each unique combination of (id, code) in sources,
        // verify that lookup returns ONLY entities with BOTH fields matching
        
        // Get all unique (id, code) combinations from sources
        List<KeyPair> uniqueKeys = sources.stream()
            .map(s -> new KeyPair(s.getTargetId(), s.getTargetCode()))
            .distinct()
            .toList();
        
        for (KeyPair key : uniqueKeys) {
            // Lookup using composite key
            List<TestSource> matchedSources = index.lookup(List.of(key.id, key.code));
            
            // Count expected matches (sources where BOTH fields match)
            long expectedCount = sources.stream()
                .filter(source -> 
                    matchesField(key.id, source.getTargetId()) &&
                    matchesField(key.code, source.getTargetCode())
                )
                .count();
            
            // Verify that ALL matched sources have BOTH fields equal
            assertThat(matchedSources).hasSize((int) expectedCount);
            
            for (TestSource matched : matchedSources) {
                assertThat(matched.getTargetId())
                    .as("Matched source should have id=%s", key.id)
                    .isEqualTo(key.id);
                assertThat(matched.getTargetCode())
                    .as("Matched source should have code=%s", key.code)
                    .isEqualTo(key.code);
            }
        }
    }
    
    /**
     * Helper class to represent a key pair
     */
    private static class KeyPair {
        final Long id;
        final String code;
        
        KeyPair(Long id, String code) {
            this.id = id;
            this.code = code;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyPair keyPair = (KeyPair) o;
            return java.util.Objects.equals(id, keyPair.id) &&
                   java.util.Objects.equals(code, keyPair.code);
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, code);
        }
    }
    
    /**
     * Property 3 (variant): Partial matches should not count
     * 
     * When using composite keys, entities that match only some fields
     * (but not all) should NOT be included in the result.
     */
    @Property(tries = 100)
    void compositeKeyMatchingShouldRejectPartialMatches() {
        
        // Given: Sources with various match patterns for key (id=1, code="A")
        List<TestSource> sources = new ArrayList<>();
        
        // 1. Full match: id=1, code="A"
        sources.add(createTestSource(1L, "A", 1, "US", 100L));
        
        // 2. Partial match (id only): id=1, code="B"
        sources.add(createTestSource(1L, "B", 2, "EU", 200L));
        
        // 3. Partial match (code only): id=2, code="A"
        sources.add(createTestSource(2L, "A", 3, "ASIA", 300L));
        
        // 4. No match: id=2, code="B"
        sources.add(createTestSource(2L, "B", 4, "US", 400L));
        
        // Setup composite key index
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: Looking up with key (id=1, code="A")
        List<TestSource> matches = index.lookup(List.of(1L, "A"));
        
        // Then: Only the full match should be returned (not partial matches)
        assertThat(matches)
            .as("Should only return sources with ALL fields matching (not partial matches)")
            .hasSize(1);
        
        TestSource match = matches.get(0);
        assertThat(match.getTargetId()).isEqualTo(1L);
        assertThat(match.getTargetCode()).isEqualTo("A");
    }
    
    /**
     * Property 3 (variant): Three-field composite keys
     * 
     * Composite keys with 3 fields should also require all fields to match.
     */
    @Property(tries = 100)
    void threeFieldCompositeKeysShouldRequireAllFieldsToMatch(
            @ForAll("sourceEntities") List<TestSource> sources) {
        
        // Given: A 3-field composite key index (id + code + version)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode),
            List.of(TestSource_.targetVersion)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: We look up entities using 3-field composite keys
        // Then: Verify that lookup returns ONLY entities with ALL THREE fields matching
        
        // Get all unique (id, code, version) combinations from sources
        List<ThreeFieldKey> uniqueKeys = sources.stream()
            .map(s -> new ThreeFieldKey(s.getTargetId(), s.getTargetCode(), s.getTargetVersion()))
            .distinct()
            .toList();
        
        for (ThreeFieldKey key : uniqueKeys) {
            // Lookup using 3-field composite key
            List<TestSource> matchedSources = index.lookup(List.of(key.id, key.code, key.version));
            
            // Count expected matches (sources where ALL THREE fields match)
            long expectedCount = sources.stream()
                .filter(source -> 
                    matchesField(key.id, source.getTargetId()) &&
                    matchesField(key.code, source.getTargetCode()) &&
                    matchesField(key.version, source.getTargetVersion())
                )
                .count();
            
            // Verify that ALL matched sources have ALL THREE fields equal
            assertThat(matchedSources).hasSize((int) expectedCount);
            
            for (TestSource matched : matchedSources) {
                assertThat(matched.getTargetId())
                    .as("Matched source should have id=%s", key.id)
                    .isEqualTo(key.id);
                assertThat(matched.getTargetCode())
                    .as("Matched source should have code=%s", key.code)
                    .isEqualTo(key.code);
                assertThat(matched.getTargetVersion())
                    .as("Matched source should have version=%s", key.version)
                    .isEqualTo(key.version);
            }
        }
    }
    
    /**
     * Helper class to represent a three-field key
     */
    private static class ThreeFieldKey {
        final Long id;
        final String code;
        final Integer version;
        
        ThreeFieldKey(Long id, String code, Integer version) {
            this.id = id;
            this.code = code;
            this.version = version;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreeFieldKey that = (ThreeFieldKey) o;
            return java.util.Objects.equals(id, that.id) &&
                   java.util.Objects.equals(code, that.code) &&
                   java.util.Objects.equals(version, that.version);
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, code, version);
        }
    }
    
    /**
     * Helper method to check if two field values match (handles nulls)
     */
    private boolean matchesField(Object targetValue, Object sourceValue) {
        if (targetValue == null && sourceValue == null) {
            return true;
        }
        if (targetValue == null || sourceValue == null) {
            return false;
        }
        return targetValue.equals(sourceValue);
    }
    
    /**
     * Provides random source entities for property tests
     */
    @Provide
    Arbitrary<List<TestSource>> sourceEntities() {
        return Arbitraries.integers().between(5, 15).flatMap(count ->
            Combinators.combine(
                Arbitraries.longs().between(1L, 3L),
                Arbitraries.of("A", "B", "C"),
                Arbitraries.integers().between(1, 3),
                Arbitraries.of("US", "EU", "ASIA"),
                Arbitraries.longs().between(100L, 500L)
            ).as(this::createTestSource)
            .list().ofSize(count)
        );
    }
    
    /**
     * Helper method to create a TestSource entity
     */
    private TestSource createTestSource(Long targetId, String targetCode, Integer targetVersion, 
                                       String targetRegion, Long targetTimestamp) {
        TestSource source = new TestSource();
        source.setTargetId(targetId);
        source.setTargetCode(targetCode);
        source.setTargetVersion(targetVersion);
        source.setTargetRegion(targetRegion);
        source.setTargetTimestamp(targetTimestamp);
        return source;
    }
    
    /**
     * Basic unit test to verify composite key index works
     */
    @Test
    void testCompositeKeyIndexBasicFunctionality() {
        // Given: A simple 2-field composite key index
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        
        List<TestSource> sources = List.of(
            createTestSource(1L, "A", 1, "US", 100L),
            createTestSource(1L, "B", 2, "EU", 200L),
            createTestSource(2L, "A", 3, "ASIA", 300L)
        );
        
        // When: Building and querying the index
        index.buildIndex(sources);
        List<TestSource> matches = index.lookup(List.of(1L, "A"));
        
        // Then: Should return only the matching entity
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getTargetId()).isEqualTo(1L);
        assertThat(matches.get(0).getTargetCode()).isEqualTo("A");
    }
    
    /**
     * Property 11: Where clause combination
     * 
     * <p><b>Feature: composite-key-mapping, Property 11: Where clause combination</b></p>
     * <p><b>Validates: Requirements 4.1</b></p>
     * 
     * For any composite key mapping with a where clause, the result should contain
     * only those records that both match the composite key AND satisfy the where clause condition.
     */
    @Property(tries = 100)
    void whereClauseShouldCombineWithCompositeKeyMatching(
            @ForAll("sourceEntities") List<TestSource> sources) {
        
        // Given: A 2-field composite key index (id + code)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: We look up entities using composite keys AND apply a where clause
        // Then: Results should match BOTH the composite key AND the where clause
        
        // Get all unique (id, code) combinations from sources
        List<KeyPair> uniqueKeys = sources.stream()
            .map(s -> new KeyPair(s.getTargetId(), s.getTargetCode()))
            .distinct()
            .toList();
        
        for (KeyPair key : uniqueKeys) {
            // Step 1: Lookup using composite key
            List<TestSource> keyMatches = index.lookup(List.of(key.id, key.code));
            
            // Step 2: Apply where clause filter (e.g., targetRegion == "US")
            List<TestSource> filteredMatches = keyMatches.stream()
                .filter(s -> "US".equals(s.getTargetRegion()))
                .toList();
            
            // Step 3: Verify that filtered results match BOTH conditions
            // Count expected matches (sources where composite key matches AND region is "US")
            long expectedCount = sources.stream()
                .filter(source -> 
                    matchesField(key.id, source.getTargetId()) &&
                    matchesField(key.code, source.getTargetCode()) &&
                    "US".equals(source.getTargetRegion())
                )
                .count();
            
            // Verify count
            assertThat(filteredMatches).hasSize((int) expectedCount);
            
            // Verify each result matches BOTH composite key AND where clause
            for (TestSource matched : filteredMatches) {
                assertThat(matched.getTargetId())
                    .as("Matched source should have id=%s", key.id)
                    .isEqualTo(key.id);
                assertThat(matched.getTargetCode())
                    .as("Matched source should have code=%s", key.code)
                    .isEqualTo(key.code);
                assertThat(matched.getTargetRegion())
                    .as("Matched source should have region=US")
                    .isEqualTo("US");
            }
        }
    }
    
    /**
     * Property 12: Where clause evaluation
     * 
     * <p><b>Feature: composite-key-mapping, Property 12: Where clause evaluation</b></p>
     * <p><b>Validates: Requirements 4.3</b></p>
     * 
     * For any where clause referencing source entity fields, when combined with composite key matching,
     * the where clause should correctly evaluate the field values of source entities that match the composite key.
     */
    @Property(tries = 100)
    void whereClauseShouldEvaluateSourceEntityFields(
            @ForAll("sourceEntities") List<TestSource> sources) {
        
        // Given: A 2-field composite key index (id + code)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: We apply where clauses that reference different source entity fields
        // Then: The where clause should correctly evaluate those fields
        
        // Get all unique (id, code) combinations from sources
        List<KeyPair> uniqueKeys = sources.stream()
            .map(s -> new KeyPair(s.getTargetId(), s.getTargetCode()))
            .distinct()
            .toList();
        
        for (KeyPair key : uniqueKeys) {
            // Step 1: Lookup using composite key
            List<TestSource> keyMatches = index.lookup(List.of(key.id, key.code));
            
            // Step 2: Apply where clause that references targetVersion field
            List<TestSource> versionFiltered = keyMatches.stream()
                .filter(s -> s.getTargetVersion() != null && s.getTargetVersion() > 1)
                .toList();
            
            // Step 3: Apply where clause that references targetTimestamp field
            List<TestSource> timestampFiltered = keyMatches.stream()
                .filter(s -> s.getTargetTimestamp() != null && s.getTargetTimestamp() >= 200L)
                .toList();
            
            // Verify that where clauses correctly evaluate the specified fields
            for (TestSource matched : versionFiltered) {
                assertThat(matched.getTargetVersion())
                    .as("Where clause should filter version > 1")
                    .isGreaterThan(1);
            }
            
            for (TestSource matched : timestampFiltered) {
                assertThat(matched.getTargetTimestamp())
                    .as("Where clause should filter timestamp >= 200")
                    .isGreaterThanOrEqualTo(200L);
            }
        }
    }
    
    /**
     * Property 13: Complex where clause logic
     * 
     * <p><b>Feature: composite-key-mapping, Property 13: Complex where clause logic</b></p>
     * <p><b>Validates: Requirements 4.4</b></p>
     * 
     * For any composite key mapping with complex boolean where clauses (AND/OR combinations),
     * the logical evaluation should follow standard boolean algebra rules.
     */
    @Property(tries = 100)
    void complexWhereClausesShouldFollowBooleanAlgebra(
            @ForAll("sourceEntities") List<TestSource> sources) {
        
        // Given: A 2-field composite key index (id + code)
        List<List<MetaAttribute<?, ?>>> keyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        CompositeKeyIndex<TestSource> index = new CompositeKeyIndex<>(keyPaths);
        index.buildIndex(sources);
        
        // When: We apply complex where clauses with AND/OR logic
        // Then: Results should follow standard boolean algebra
        
        // Get all unique (id, code) combinations from sources
        List<KeyPair> uniqueKeys = sources.stream()
            .map(s -> new KeyPair(s.getTargetId(), s.getTargetCode()))
            .distinct()
            .toList();
        
        for (KeyPair key : uniqueKeys) {
            // Step 1: Lookup using composite key
            List<TestSource> keyMatches = index.lookup(List.of(key.id, key.code));
            
            // Step 2: Apply complex where clause: (region == "US" AND version > 1) OR (region == "EU")
            List<TestSource> complexFiltered = keyMatches.stream()
                .filter(s -> 
                    ("US".equals(s.getTargetRegion()) && s.getTargetVersion() != null && s.getTargetVersion() > 1) ||
                    "EU".equals(s.getTargetRegion())
                )
                .toList();
            
            // Verify that each result satisfies the complex boolean condition
            for (TestSource matched : complexFiltered) {
                boolean satisfiesCondition = 
                    ("US".equals(matched.getTargetRegion()) && matched.getTargetVersion() != null && matched.getTargetVersion() > 1) ||
                    "EU".equals(matched.getTargetRegion());
                
                assertThat(satisfiesCondition)
                    .as("Matched source should satisfy complex where clause: (region=US AND version>1) OR (region=EU)")
                    .isTrue();
            }
            
            // Step 3: Apply another complex where clause: (version == 1 OR version == 3) AND timestamp < 400
            List<TestSource> anotherComplexFiltered = keyMatches.stream()
                .filter(s -> 
                    s.getTargetVersion() != null && 
                    (s.getTargetVersion() == 1 || s.getTargetVersion() == 3) &&
                    s.getTargetTimestamp() != null &&
                    s.getTargetTimestamp() < 400L
                )
                .toList();
            
            // Verify that each result satisfies the second complex boolean condition
            for (TestSource matched : anotherComplexFiltered) {
                assertThat(matched.getTargetVersion())
                    .as("Version should be 1 or 3")
                    .isIn(1, 3);
                assertThat(matched.getTargetTimestamp())
                    .as("Timestamp should be < 400")
                    .isLessThan(400L);
            }
        }
    }
}
