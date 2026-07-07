package com.thy.fss.common.inmemory.factory.navigation;

import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource_;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Property-based tests for KeyPairBuilder chaining functionality.
 * 
 * <p><b>Feature: composite-key-mapping, Property 6: KeyPairBuilder chaining</b></p>
 * <p><b>Validates: Requirements 2.1, 2.2</b></p>
 * 
 * <p>This test verifies that the KeyPairBuilder supports fluent chaining of multiple
 * on() calls to build composite keys with 1-5 field pairs.</p>
 */
class KeyPairBuilderPropertyTest {
    
    /**
     * Property 6: KeyPairBuilder chaining
     * 
     * For any number of field pairs (1-5), chaining multiple on() calls should:
     * 1. Return a KeyPairBuilder instance after each call (enabling chaining)
     * 2. Accumulate all field pairs in the correct order
     * 3. Produce equal-sized lists of primary and foreign key paths
     * 
     * Validates: Requirements 2.1, 2.2
     */
    @Property(tries = 100)
    void keyPairBuilderShouldSupportChainingMultipleOnCalls(
            @ForAll @IntRange(min = 1, max = 5) int numberOfPairs) {
        
        // Given: A KeyPairBuilder
        KeyPairBuilder builder = new KeyPairBuilder(TestTarget.class, TestSource.class);
        
        // When: We chain multiple on() calls
        KeyPairBuilder result = builder;
        for (int i = 0; i < numberOfPairs; i++) {
            final int index = i;
            result = result.on(
                pk -> buildPrimaryKeyPath(pk, index),
                fk -> buildForeignKeyPath(fk, index)
            );
            
            // Then: Each on() call should return a KeyPairBuilder (enabling chaining)
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(KeyPairBuilder.class);
        }
        
        // Then: The builder should have accumulated all field pairs
        List<PropertyNavigation> primaryKeyPaths = result.getPrimaryKeyPaths();
        List<PropertyNavigation> foreignKeyPaths = result.getForeignKeyPaths();
        
        assertThat(primaryKeyPaths).hasSize(numberOfPairs);
        assertThat(foreignKeyPaths).hasSize(numberOfPairs);
        
        // And: The key pair count should match
        assertThat(result.getKeyPairCount()).isEqualTo(numberOfPairs);
        
        // And: Each path should be valid (non-null and have at least one field)
        for (int i = 0; i < numberOfPairs; i++) {
            assertThat(primaryKeyPaths.get(i)).isNotNull();
            assertThat(primaryKeyPaths.get(i).getPath()).isNotEmpty();
            assertThat(foreignKeyPaths.get(i)).isNotNull();
            assertThat(foreignKeyPaths.get(i).getPath()).isNotEmpty();
        }
    }
    
    /**
     * Property 6 (variant): Chaining should preserve order
     * 
     * For any sequence of field pairs, the order in which on() is called
     * should be preserved in the resulting lists.
     */
    @Property(tries = 100)
    void keyPairBuilderShouldPreserveOrderOfOnCalls(
            @ForAll @IntRange(min = 2, max = 5) int numberOfPairs) {
        
        // Given: A KeyPairBuilder with multiple on() calls
        KeyPairBuilder builder = new KeyPairBuilder(TestTarget.class, TestSource.class);
        
        for (int i = 0; i < numberOfPairs; i++) {
            final int index = i;
            builder = builder.on(
                pk -> buildPrimaryKeyPath(pk, index),
                fk -> buildForeignKeyPath(fk, index)
            );
        }
        
        // When: We retrieve the paths
        List<PropertyNavigation> primaryKeyPaths = builder.getPrimaryKeyPaths();
        List<PropertyNavigation> foreignKeyPaths = builder.getForeignKeyPaths();
        
        // Then: The paths should be in the same order as the on() calls
        for (int i = 0; i < numberOfPairs; i++) {
            PropertyNavigation expectedPk = buildPrimaryKeyPath(
                new PropertyNavigationBuilder(TestTarget.class), i
            ).build();
            PropertyNavigation expectedFk = buildForeignKeyPath(
                new PropertyNavigationBuilder(TestSource.class), i
            ).build();
            
            assertThat(primaryKeyPaths.get(i).getPath()).isEqualTo(expectedPk.getPath());
            assertThat(foreignKeyPaths.get(i).getPath()).isEqualTo(expectedFk.getPath());
        }
    }
    
    /**
     * Property 6 (variant): Returned lists should be immutable
     * 
     * The lists returned by getPrimaryKeyPaths() and getForeignKeyPaths()
     * should be immutable to prevent external modification.
     */
    @Property(tries = 100)
    void keyPairBuilderShouldReturnImmutableLists(
            @ForAll @IntRange(min = 1, max = 3) int numberOfPairs) {
        
        // Given: A KeyPairBuilder with some field pairs
        KeyPairBuilder builder = new KeyPairBuilder(TestTarget.class, TestSource.class);
        
        for (int i = 0; i < numberOfPairs; i++) {
            final int index = i;
            builder = builder.on(
                pk -> buildPrimaryKeyPath(pk, index),
                fk -> buildForeignKeyPath(fk, index)
            );
        }
        
        // When: We retrieve the paths
        List<PropertyNavigation> primaryKeyPaths = builder.getPrimaryKeyPaths();
        List<PropertyNavigation> foreignKeyPaths = builder.getForeignKeyPaths();
        
        // Then: The lists should be immutable (attempting to modify should throw)
        assertThat(primaryKeyPaths).isUnmodifiable();
        assertThat(foreignKeyPaths).isUnmodifiable();
    }
    
    /**
     * Helper method to build primary key paths based on index
     */
    private PropertyNavigationBuilder buildPrimaryKeyPath(PropertyNavigationBuilder builder, int index) {
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
    private PropertyNavigationBuilder buildForeignKeyPath(PropertyNavigationBuilder builder, int index) {
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
     * Basic unit test to verify KeyPairBuilder can be instantiated
     */
    @Test
    void keyPairBuilderShouldBeInstantiable() {
        assertDoesNotThrow(() -> new KeyPairBuilder(TestTarget.class, TestSource.class));
    }
    
    /**
     * Basic unit test to verify single on() call works
     */
    @Test
    void keyPairBuilderShouldAcceptSingleOnCall() {
        // Given
        KeyPairBuilder builder = new KeyPairBuilder(TestTarget.class, TestSource.class);
        
        // When
        KeyPairBuilder result = builder.on(
            pk -> pk.field(TestTarget_.id),
            fk -> fk.field(TestSource_.targetId)
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeyPairCount()).isEqualTo(1);
        assertThat(result.getPrimaryKeyPaths()).hasSize(1);
        assertThat(result.getForeignKeyPaths()).hasSize(1);
    }
}
