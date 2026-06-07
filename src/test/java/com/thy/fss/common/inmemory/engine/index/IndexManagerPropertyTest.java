package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for IndexManager composite key support.
 * 
 * <p><b>Feature: composite-key-mapping, Property 14: Index creation</b></p>
 * <p><b>Validates: Requirements 5.2</b></p>
 * 
 * <p>This test verifies that the IndexManager correctly creates and caches
 * composite key indexes for various data sources and key configurations.</p>
 */
class IndexManagerPropertyTest {
    
    /**
     * Property 14: Index creation
     * 
     * For any composite key mapping, after the mapping is built, an appropriate
     * composite key index should exist in the IndexManager for the specified
     * data source and key fields.
     * 
     * This property verifies:
     * 1. Index is created with correct key paths
     * 2. Index is cached and reused on subsequent calls
     * 3. Index works correctly with the provided data
     * 4. Multiple indexes can coexist for different data sources
     * 
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    void compositeKeyIndexShouldBeCreatedAndCached(
            @ForAll("dataSourceNames") String dataSourceName,
            @ForAll @IntRange(min = 1, max = 5) int numberOfKeyFields,
            @ForAll @IntRange(min = 0, max = 20) int entityCount) {
        
        // Given: An IndexManager
        IndexManager indexManager = new IndexManager();
        
        // And: A composite key configuration with the specified number of fields
        List<List<MetaAttribute<?, ?>>> keyPaths = buildKeyPaths(numberOfKeyFields);
        
        // And: A collection of test entities
        List<TestTarget> entities = generateEntities(entityCount);
        
        // When: We request a composite index for the first time
        CompositeKeyIndex<TestTarget> firstIndex = indexManager.getOrCreateCompositeIndex(
            dataSourceName,
            keyPaths,
            entities
        );
        
        // Then: An index should be created
        assertThat(firstIndex).isNotNull();
        assertThat(firstIndex.getKeyFieldCount()).isEqualTo(numberOfKeyFields);
        assertThat(firstIndex.getKeyPaths()).hasSize(numberOfKeyFields);
        
        // When: We request the same index again
        CompositeKeyIndex<TestTarget> secondIndex = indexManager.getOrCreateCompositeIndex(
            dataSourceName,
            keyPaths,
            entities
        );
        
        // Then: The same cached instance should be returned
        assertThat(secondIndex).isSameAs(firstIndex);
        
        // And: The index should work correctly for lookups
        if (!entities.isEmpty()) {
            TestTarget firstEntity = entities.get(0);
            List<Object> keyValues = extractKeyValues(firstEntity, keyPaths);
            
            // The lookup should not throw an exception
            List<TestTarget> lookupResult = firstIndex.lookup(keyValues);
            assertThat(lookupResult).isNotNull();
        }
    }
    
    /**
     * Property: Multiple data sources should have independent indexes
     * 
     * Verifies that indexes for different data sources are independent
     * and don't interfere with each other.
     */
    @Property(tries = 100)
    void multipleDataSourcesShouldHaveIndependentIndexes(
            @ForAll("dataSourceNames") String dataSource1,
            @ForAll("dataSourceNames") String dataSource2,
            @ForAll @IntRange(min = 1, max = 3) int keyFieldCount,
            @ForAll @IntRange(min = 1, max = 10) int entityCount1,
            @ForAll @IntRange(min = 1, max = 10) int entityCount2) {
        
        // Given: An IndexManager
        IndexManager indexManager = new IndexManager();
        
        // And: The same key configuration for both data sources
        List<List<MetaAttribute<?, ?>>> keyPaths = buildKeyPaths(keyFieldCount);
        
        // And: Two collections of entities
        List<TestTarget> entities1 = generateEntities(entityCount1);
        List<TestTarget> entities2 = generateEntities(entityCount2);
        
        // When: We create indexes for two different data sources
        CompositeKeyIndex<TestTarget> index1 = indexManager.getOrCreateCompositeIndex(
            dataSource1,
            keyPaths,
            entities1
        );
        
        CompositeKeyIndex<TestTarget> index2 = indexManager.getOrCreateCompositeIndex(
            dataSource2,
            keyPaths,
            entities2
        );
        
        // Then: If data sources are different, indexes should be different instances
        if (!dataSource1.equals(dataSource2)) {
            assertThat(index1).isNotSameAs(index2);
        } else {
            // If data sources are the same, should return cached instance
            assertThat(index1).isSameAs(index2);
        }
    }
    
    /**
     * Property: Different key configurations should create different indexes
     * 
     * Verifies that different key field configurations result in different
     * cached indexes, even for the same data source.
     */
    @Property(tries = 100)
    void differentKeyConfigurationsShouldCreateDifferentIndexes(
            @ForAll("dataSourceNames") String dataSourceName,
            @ForAll @IntRange(min = 1, max = 3) int keyFieldCount1,
            @ForAll @IntRange(min = 1, max = 3) int keyFieldCount2,
            @ForAll @IntRange(min = 1, max = 10) int entityCount) {
        
        // Given: An IndexManager
        IndexManager indexManager = new IndexManager();
        
        // And: Two different key configurations
        List<List<MetaAttribute<?, ?>>> keyPaths1 = buildKeyPaths(keyFieldCount1);
        List<List<MetaAttribute<?, ?>>> keyPaths2 = buildKeyPaths(keyFieldCount2);
        
        // And: A collection of entities
        List<TestTarget> entities = generateEntities(entityCount);
        
        // When: We create indexes with different key configurations
        CompositeKeyIndex<TestTarget> index1 = indexManager.getOrCreateCompositeIndex(
            dataSourceName,
            keyPaths1,
            entities
        );
        
        CompositeKeyIndex<TestTarget> index2 = indexManager.getOrCreateCompositeIndex(
            dataSourceName,
            keyPaths2,
            entities
        );
        
        // Then: If key configurations are different, indexes should be different
        if (keyFieldCount1 != keyFieldCount2) {
            assertThat(index1).isNotSameAs(index2);
            assertThat(index1.getKeyFieldCount()).isEqualTo(keyFieldCount1);
            assertThat(index2.getKeyFieldCount()).isEqualTo(keyFieldCount2);
        } else {
            // If configurations are the same, should return cached instance
            assertThat(index1).isSameAs(index2);
        }
    }
    
    /**
     * Provides arbitrary data source names for testing.
     */
    @Provide
    Arbitrary<String> dataSourceNames() {
        return Arbitraries.of(
            "users-datasource",
            "orders-datasource",
            "products-datasource",
            "customers-datasource",
            "inventory-datasource"
        );
    }
    
    /**
     * Generates a list of test entities with the specified count.
     */
    private List<TestTarget> generateEntities(int count) {
        List<TestTarget> entities = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TestTarget target = new TestTarget();
            target.setId((long) (i % 10 + 1));  // Cycle through 1-10
            target.setCode(String.valueOf((char) ('A' + (i % 5))));  // Cycle through A-E
            target.setVersion(i % 3 + 1);  // Cycle through 1-3
            target.setRegion(new String[]{"US", "EU", "AS"}[i % 3]);
            target.setTimestamp(1000L + i);
            entities.add(target);
        }
        
        return entities;
    }
    
    /**
     * Builds a list of key paths with the specified number of fields.
     * Uses different fields from TestTarget_ to create variety.
     */
    private List<List<MetaAttribute<?, ?>>> buildKeyPaths(int numberOfFields) {
        List<List<MetaAttribute<?, ?>>> keyPaths = new ArrayList<>();
        
        // Use different fields based on the number requested
        // Cycle through available fields if we need more than available
        MetaAttribute<?, ?>[] availableFields = {
            TestTarget_.id,
            TestTarget_.code,
            TestTarget_.version,
            TestTarget_.region,
            TestTarget_.timestamp
        };
        
        for (int i = 0; i < numberOfFields; i++) {
            MetaAttribute<?, ?> field = availableFields[i % availableFields.length];
            keyPaths.add(List.of(field));
        }
        
        return keyPaths;
    }
    
    /**
     * Extracts key values from an entity based on the key paths.
     */
    private List<Object> extractKeyValues(TestTarget entity, List<List<MetaAttribute<?, ?>>> keyPaths) {
        List<Object> values = new ArrayList<>();
        
        for (List<MetaAttribute<?, ?>> path : keyPaths) {
            // For single-field paths, extract the value directly
            if (path.size() == 1) {
                MetaAttribute<?, ?> field = path.get(0);
                Object value = extractFieldValue(entity, field);
                values.add(value);
            }
        }
        
        return values;
    }
    
    /**
     * Extracts a field value from an entity.
     */
    private Object extractFieldValue(TestTarget entity, MetaAttribute<?, ?> field) {
        String fieldName = field.getName();
        
        try {
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            java.lang.reflect.Method getter = entity.getClass().getMethod(getterName);
            return getter.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field: " + fieldName, e);
        }
    }
}
