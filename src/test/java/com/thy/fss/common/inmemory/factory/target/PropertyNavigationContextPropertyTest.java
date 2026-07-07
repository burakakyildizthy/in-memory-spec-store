package com.thy.fss.common.inmemory.factory.target;

import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.*;
import com.thy.fss.common.inmemory.factory.source.SourceMappingContext;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Property-based tests for PropertyNavigationContext.from() method with composite keys.
 * 
 * <p><b>Feature: composite-key-mapping, Property 1: Multiple field acceptance</b></p>
 * <p><b>Validates: Requirements 1.1</b></p>
 * 
 * <p>This test verifies that the from() method accepts multiple field pairs (2-5 fields)
 * for composite key mappings without throwing exceptions.</p>
 */
class PropertyNavigationContextPropertyTest {
    
    /**
     * Property 1: Multiple field acceptance
     * 
     * For any valid set of 2-5 PropertyNavigation objects representing primary key fields
     * and an equal number representing foreign key fields, the from() method should accept
     * them without throwing an exception.
     * 
     * Validates: Requirements 1.1
     */
    @Property(tries = 100)
    void fromMethodShouldAcceptMultipleFieldPairs(
            @ForAll @IntRange(min = 2, max = 5) int numberOfPairs) {
        
        // Given: A factory and store builder
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryStoreBuilder<TestTarget> storeBuilder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE);
        
        // When: We call from() with multiple field pairs
        assertDoesNotThrow(() -> {
            SourceMappingContext<TestTarget, Long, TestSource> context = storeBuilder
                .target(TestTarget_.id)
                .from(TestSourceSpecificationService.INSTANCE, keys -> {
                    // Build the composite key with the specified number of pairs
                    for (int i = 0; i < numberOfPairs; i++) {
                        final int index = i;
                        keys = keys.on(
                            pk -> buildPrimaryKeyPath(pk, index),
                            fk -> buildForeignKeyPath(fk, index)
                        );
                    }
                    return keys;
                });
            
            // Then: The context should be created successfully
            assertThat(context).isNotNull();
        });
    }
    
    /**
     * Property 1 (variant): Single field should also work
     * 
     * For backward compatibility, a single field pair should also be accepted.
     */
    @Property(tries = 100)
    void fromMethodShouldAcceptSingleFieldPair() {
        
        // Given: A factory and store builder
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryStoreBuilder<TestTarget> storeBuilder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE);
        
        // When: We call from() with a single field pair
        assertDoesNotThrow(() -> {
            SourceMappingContext<TestTarget, Long, TestSource> context = storeBuilder
                .target(TestTarget_.id)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                );
            
            // Then: The context should be created successfully
            assertThat(context).isNotNull();
        });
    }
    
    /**
     * Property 1 (variant): Old API should still work
     * 
     * The old from() method with two Function parameters should still work
     * for backward compatibility.
     */
    @Property(tries = 100)
    void oldFromMethodShouldStillWork() {
        
        // Given: A factory and store builder
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryStoreBuilder<TestTarget> storeBuilder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE);
        
        // When: We call the old from() method
        assertDoesNotThrow(() -> {
            SourceMappingContext<TestTarget, Long, TestSource> context = storeBuilder
                .target(TestTarget_.id)
                .from(TestSourceSpecificationService.INSTANCE,
                    pk -> pk.field(TestTarget_.id),
                    fk -> fk.field(TestSource_.targetId)
                );
            
            // Then: The context should be created successfully
            assertThat(context).isNotNull();
        });
    }
    
    /**
     * Helper method to build primary key paths based on index
     */
    private com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder buildPrimaryKeyPath(
            com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder builder, int index) {
        switch (index % 5) {
            case 0: return builder.field(TestTarget_.id);
            case 1: return builder.field(TestTarget_.code);
            case 2: return builder.field(TestTarget_.version);
            case 3: return builder.field(TestTarget_.region);
            case 4: return builder.field(TestTarget_.timestamp);
            default: return builder.field(TestTarget_.id);
        }
    }
    
    /**
     * Helper method to build foreign key paths based on index
     */
    private com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder buildForeignKeyPath(
            com.thy.fss.common.inmemory.factory.navigation.PropertyNavigationBuilder builder, int index) {
        switch (index % 5) {
            case 0: return builder.field(TestSource_.targetId);
            case 1: return builder.field(TestSource_.targetCode);
            case 2: return builder.field(TestSource_.targetVersion);
            case 3: return builder.field(TestSource_.targetRegion);
            case 4: return builder.field(TestSource_.targetTimestamp);
            default: return builder.field(TestSource_.targetId);
        }
    }
    
    /**
     * Basic unit test to verify from() with composite keys works
     */
    @Test
    void fromMethodShouldAcceptCompositeKeys() {
        // Given
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        InMemoryStoreBuilder<TestTarget> storeBuilder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE);
        
        // When
        SourceMappingContext<TestTarget, Long, TestSource> context = storeBuilder
            .target(TestTarget_.id)
            .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
            );
        
        // Then
        assertThat(context).isNotNull();
    }
}
