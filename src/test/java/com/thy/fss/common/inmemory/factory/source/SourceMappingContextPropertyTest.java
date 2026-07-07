package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource_;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSourceSpecificationService;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for SourceMappingContext backward compatibility.
 * 
 * <p><b>Feature: composite-key-mapping, Property 5: Single-field as composite</b></p>
 * <p><b>Validates: Requirements 1.5</b></p>
 * 
 * <p>This test verifies that single-field mappings are treated as composite keys
 * with one field pair, ensuring backward compatibility.</p>
 */
class SourceMappingContextPropertyTest {
    
    /**
     * Property 5: Single-field as composite
     * 
     * For any single-field mapping, the behavior should be identical whether
     * it's created as a single-field or as a composite key with one field.
     * 
     * This ensures backward compatibility: existing single-field mappings
     * should work exactly the same way after composite key implementation.
     * 
     * Validates: Requirements 1.5
     */
    @Property(tries = 100)
    void singleFieldMappingShouldBehaveLikeCompositeKeyWithOneField(
            @ForAll @IntRange(min = 0, max = 4) int fieldIndex) {
        
        // Given: A mock PropertyNavigationContext
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, Long> targetContext = mock(PropertyNavigationContext.class);
        
        // And: A single PropertyNavigation for primary and foreign keys
        PropertyNavigation primaryKey = buildPrimaryKeyPath(fieldIndex);
        PropertyNavigation foreignKey = buildForeignKeyPath(fieldIndex);
        
        // When: We create a SourceMappingContext with single-field lists
        List<PropertyNavigation> primaryKeys = Collections.singletonList(primaryKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(foreignKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        SourceMappingContext<TestTarget, Long, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Then: isCompositeKey() should return false (single field is not composite)
        assertThat(context.isCompositeKey())
            .as("Single-field mapping should not be considered composite")
            .isFalse();
        
        // And: getKeyFieldCount() should return 1
        assertThat(context.getKeyFieldCount())
            .as("Single-field mapping should have exactly 1 key field")
            .isEqualTo(1);
        
        // And: getPrimaryKeyPaths() should return a list with one element
        assertThat(context.getPrimaryKeyPaths())
            .as("Primary key paths should contain exactly one element")
            .hasSize(1)
            .containsExactly(primaryKey);
        
        // And: getForeignKeyPaths() should return a list with one element
        assertThat(context.getForeignKeyPaths())
            .as("Foreign key paths should contain exactly one element")
            .hasSize(1)
            .containsExactly(foreignKey);
        
        // And: The returned lists should be unmodifiable
        assertThat(context.getPrimaryKeyPaths()).isUnmodifiable();
        assertThat(context.getForeignKeyPaths()).isUnmodifiable();
    }
    
    /**
     * Property 5 (variant): Single-field mapping behavior consistency
     * 
     * For any single-field mapping, all the composite key methods should
     * work correctly and return consistent results.
     */
    @Property(tries = 100)
    void singleFieldMappingShouldHaveConsistentBehavior(
            @ForAll @IntRange(min = 0, max = 4) int fieldIndex) {
        
        // Given: A SourceMappingContext with a single field
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, Long> targetContext = mock(PropertyNavigationContext.class);
        
        PropertyNavigation primaryKey = buildPrimaryKeyPath(fieldIndex);
        PropertyNavigation foreignKey = buildForeignKeyPath(fieldIndex);
        
        List<PropertyNavigation> primaryKeys = Collections.singletonList(primaryKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(foreignKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        SourceMappingContext<TestTarget, Long, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Then: All composite key methods should return consistent results
        assertThat(context.isCompositeKey()).isFalse();
        assertThat(context.getKeyFieldCount()).isEqualTo(1);
        assertThat(context.getPrimaryKeyPaths()).hasSize(1);
        assertThat(context.getForeignKeyPaths()).hasSize(1);
        
        // And: The key field count should match the list sizes
        assertThat(context.getKeyFieldCount())
            .isEqualTo(context.getPrimaryKeyPaths().size())
            .isEqualTo(context.getForeignKeyPaths().size());
    }
    
    /**
     * Property 5 (variant): All subclasses should support single-field mappings
     * 
     * For any SourceMappingContext subclass, single-field mappings should work
     * correctly with the composite key infrastructure.
     */
    @Property(tries = 100)
    void allSubclassesShouldSupportSingleFieldMappings(
            @ForAll @IntRange(min = 0, max = 4) int fieldIndex) {
        
        // Given: Mock target contexts for different types
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, Long> numericContext = mock(PropertyNavigationContext.class);
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, String> stringContext = mock(PropertyNavigationContext.class);
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, Boolean> booleanContext = mock(PropertyNavigationContext.class);
        
        PropertyNavigation primaryKey = buildPrimaryKeyPath(fieldIndex);
        PropertyNavigation foreignKey = buildForeignKeyPath(fieldIndex);
        
        List<PropertyNavigation> primaryKeys = Collections.singletonList(primaryKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(foreignKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // When: We create different subclass instances with single-field mappings
        NumericSourceMappingContext<TestTarget, Long, TestSource> numericCtx = 
            new NumericSourceMappingContext<>(numericContext, sourceSvc, primaryKeys, foreignKeys);
        
        StringSourceMappingContext<TestTarget, TestSource> stringCtx = 
            new StringSourceMappingContext<>(stringContext, sourceSvc, primaryKeys, foreignKeys);
        
        BooleanSourceMappingContext<TestTarget, TestSource> booleanCtx = 
            new BooleanSourceMappingContext<>(booleanContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Then: All subclasses should behave consistently with single-field mappings
        assertThat(numericCtx.isCompositeKey()).isFalse();
        assertThat(numericCtx.getKeyFieldCount()).isEqualTo(1);
        
        assertThat(stringCtx.isCompositeKey()).isFalse();
        assertThat(stringCtx.getKeyFieldCount()).isEqualTo(1);
        
        assertThat(booleanCtx.isCompositeKey()).isFalse();
        assertThat(booleanCtx.getKeyFieldCount()).isEqualTo(1);
    }
    
    /**
     * Helper method to build primary key paths based on index
     */
    @SuppressWarnings("unchecked")
    private PropertyNavigation buildPrimaryKeyPath(int index) {
        PropertyNavigation nav = mock(PropertyNavigation.class);
        
        // Mock the path based on index
        switch (index % 5) {
            case 0:
                when(nav.getPath()).thenReturn(List.of(TestTarget_.id));
                when(nav.getLeafClass()).thenReturn((Class) Long.class);
                break;
            case 1:
                when(nav.getPath()).thenReturn(List.of(TestTarget_.code));
                when(nav.getLeafClass()).thenReturn((Class) String.class);
                break;
            case 2:
                when(nav.getPath()).thenReturn(List.of(TestTarget_.version));
                when(nav.getLeafClass()).thenReturn((Class) Integer.class);
                break;
            case 3:
                when(nav.getPath()).thenReturn(List.of(TestTarget_.region));
                when(nav.getLeafClass()).thenReturn((Class) String.class);
                break;
            case 4:
                when(nav.getPath()).thenReturn(List.of(TestTarget_.timestamp));
                when(nav.getLeafClass()).thenReturn((Class) Long.class);
                break;
            default:
                throw new IllegalArgumentException("Invalid index for primary key path");
        }
        
        return nav;
    }
    
    /**
     * Helper method to build foreign key paths based on index
     */
    @SuppressWarnings("unchecked")
    private PropertyNavigation buildForeignKeyPath(int index) {
        PropertyNavigation nav = mock(PropertyNavigation.class);
        
        // Mock the path based on index
        switch (index % 5) {
            case 0:
                when(nav.getPath()).thenReturn(List.of(TestSource_.targetId));
                when(nav.getLeafClass()).thenReturn((Class) Long.class);
                break;
            case 1:
                when(nav.getPath()).thenReturn(List.of(TestSource_.targetCode));
                when(nav.getLeafClass()).thenReturn((Class) String.class);
                break;
            case 2:
                when(nav.getPath()).thenReturn(List.of(TestSource_.targetVersion));
                when(nav.getLeafClass()).thenReturn((Class) Integer.class);
                break;
            case 3:
                when(nav.getPath()).thenReturn(List.of(TestSource_.targetRegion));
                when(nav.getLeafClass()).thenReturn((Class) String.class);
                break;
            case 4:
                when(nav.getPath()).thenReturn(List.of(TestSource_.targetTimestamp));
                when(nav.getLeafClass()).thenReturn((Class) Long.class);
                break;
            default:
                throw new IllegalArgumentException("Invalid index for foreign key path");
        }
        
        return nav;
    }
    
    /**
     * Basic unit test to verify single-field mapping creation
     */
    @Test
    void singleFieldMappingShouldBeCreatable() {
        // Given
        @SuppressWarnings("unchecked")
        PropertyNavigationContext<TestTarget, Long> targetContext = mock(PropertyNavigationContext.class);
        
        PropertyNavigation primaryKey = mock(PropertyNavigation.class);
        PropertyNavigation foreignKey = mock(PropertyNavigation.class);
        
        List<PropertyNavigation> primaryKeys = Collections.singletonList(primaryKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(foreignKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // When
        SourceMappingContext<TestTarget, Long, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Then
        assertThat(context).isNotNull();
        assertThat(context.isCompositeKey()).isFalse();
        assertThat(context.getKeyFieldCount()).isEqualTo(1);
    }
}
