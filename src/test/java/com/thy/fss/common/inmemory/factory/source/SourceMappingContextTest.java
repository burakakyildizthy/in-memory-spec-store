package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSourceSpecificationService;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for SourceMappingContext to verify composite key functionality.
 */
class SourceMappingContextTest {
    
    @Test
    void testIsCompositeKeyWithSingleKeyReturnsFalse() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation singleKey = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Collections.singletonList(singleKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(singleKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Assert
        assertFalse(context.isCompositeKey(), "Single key should not be composite");
    }
    
    @Test
    void testIsCompositeKeyWithMultipleKeysReturnsTrue() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation key1 = mock(PropertyNavigation.class);
        PropertyNavigation key2 = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Arrays.asList(key1, key2);
        List<PropertyNavigation> foreignKeys = Arrays.asList(key1, key2);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Assert
        assertTrue(context.isCompositeKey(), "Multiple keys should be composite");
    }
    
    @Test
    void testIsCompositeKeyWithNullKeysReturnsFalse() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, null, null);
        
        // Assert
        assertFalse(context.isCompositeKey(), "Null keys should not be composite");
    }
    
    @Test
    void testGetKeyFieldCountWithSingleKeyReturnsOne() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation singleKey = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Collections.singletonList(singleKey);
        List<PropertyNavigation> foreignKeys = Collections.singletonList(singleKey);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Assert
        assertEquals(1, context.getKeyFieldCount(), "Should return 1 for single key");
    }
    
    @Test
    void testGetKeyFieldCountWithMultipleKeysReturnsCorrectCount() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation key1 = mock(PropertyNavigation.class);
        PropertyNavigation key2 = mock(PropertyNavigation.class);
        PropertyNavigation key3 = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Arrays.asList(key1, key2, key3);
        List<PropertyNavigation> foreignKeys = Arrays.asList(key1, key2, key3);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        
        // Assert
        assertEquals(3, context.getKeyFieldCount(), "Should return 3 for three keys");
    }
    
    @Test
    void testGetKeyFieldCountWithNullKeysReturnsZero() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, null, null);
        
        // Assert
        assertEquals(0, context.getKeyFieldCount(), "Should return 0 for null keys");
    }
    
    @Test
    void testGetPrimaryKeyPathsReturnsUnmodifiableList() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation key1 = mock(PropertyNavigation.class);
        PropertyNavigation key2 = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Arrays.asList(key1, key2);
        List<PropertyNavigation> foreignKeys = Arrays.asList(key1, key2);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        List<PropertyNavigation> result = context.getPrimaryKeyPaths();
        
        // Assert
        assertNotNull(result, "Should return non-null list");
        assertEquals(2, result.size(), "Should return list with 2 elements");
        assertThrows(UnsupportedOperationException.class, () -> result.add(mock(PropertyNavigation.class)),
            "Returned list should be unmodifiable");
    }
    
    @Test
    void testGetForeignKeyPathsReturnsUnmodifiableList() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        PropertyNavigation key1 = mock(PropertyNavigation.class);
        PropertyNavigation key2 = mock(PropertyNavigation.class);
        List<PropertyNavigation> primaryKeys = Arrays.asList(key1, key2);
        List<PropertyNavigation> foreignKeys = Arrays.asList(key1, key2);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, primaryKeys, foreignKeys);
        List<PropertyNavigation> result = context.getForeignKeyPaths();
        
        // Assert
        assertNotNull(result, "Should return non-null list");
        assertEquals(2, result.size(), "Should return list with 2 elements");
        assertThrows(UnsupportedOperationException.class, () -> result.add(mock(PropertyNavigation.class)),
            "Returned list should be unmodifiable");
    }
    
    @Test
    void testGetPrimaryKeyPathsWithNullKeysReturnsNull() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, null, null);
        
        // Assert
        assertNull(context.getPrimaryKeyPaths(), "Should return null for null keys");
    }
    
    @Test
    void testGetForeignKeyPathsWithNullKeysReturnsNull() {
        // Arrange
        PropertyNavigationContext<Object, Object> targetContext = mock(PropertyNavigationContext.class);
        
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;
        
        // Act
        SourceMappingContext<Object, Object, TestSource> context = 
            new SourceMappingContext<>(targetContext, sourceSvc, null, null);
        
        // Assert
        assertNull(context.getForeignKeyPaths(), "Should return null for null keys");
    }
}
