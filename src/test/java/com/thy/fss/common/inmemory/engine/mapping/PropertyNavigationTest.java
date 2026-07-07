package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PropertyNavigation class.
 * Verifies path storage, immutability, getters, and validation.
 * 
 * Requirements tested:
 * - 1.1: PropertyNavigation stores field paths correctly
 * - Immutability: PropertyNavigation is immutable
 * - Validation: Path cannot be null or empty
 */
@DisplayName("PropertyNavigation Tests")
class PropertyNavigationTest {

    private static final String PATH_CANNOT_BE_NULL_OR_EMPTY = "Path cannot be null or empty";

    @Nested
    @DisplayName("Constructor and Validation")
    class ConstructorAndValidationTests {

        @Test
        @DisplayName("Should create PropertyNavigation with valid single-level path")
        void testCreateWithSingleLevelPath() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertNotNull(navigation);
            assertNotNull(navigation.getPath());
            assertEquals(1, navigation.getPath().size());
            assertEquals(User_.id, navigation.getPath().get(0));
        }

        @Test
        @DisplayName("Should create PropertyNavigation with multi-level path")
        void testCreateWithMultiLevelPath() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(User_.profile, Profile_.bio);
            
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertNotNull(navigation);
            assertEquals(2, navigation.getPath().size());
            assertEquals(User_.profile, navigation.getPath().get(0));
            assertEquals(Profile_.bio, navigation.getPath().get(1));
        }

        @Test
        @DisplayName("Should reject null path")
        void testRejectNullPath() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PropertyNavigation(null, null)
            );
            
            assertTrue(exception.getMessage().contains(PATH_CANNOT_BE_NULL_OR_EMPTY));
        }

        @Test
        @DisplayName("Should reject empty path")
        void testRejectEmptyPath() {
            List<MetaAttribute<?, ?>> emptyPath = Collections.emptyList();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PropertyNavigation(emptyPath, null)
            );
            
            assertTrue(exception.getMessage().contains(PATH_CANNOT_BE_NULL_OR_EMPTY));
        }

        @Test
        @DisplayName("Should create PropertyNavigation with empty collection operations")
        void testCreateWithEmptyCollectionOperations() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            
            PropertyNavigation navigation = new PropertyNavigation(path, Collections.emptyList());
            
            assertNotNull(navigation.getCollectionOperations());
            assertTrue(navigation.getCollectionOperations().isEmpty());
            assertFalse(navigation.hasCollectionOperations());
        }

        @Test
        @DisplayName("Should create PropertyNavigation with null collection operations")
        void testCreateWithNullCollectionOperations() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertNotNull(navigation.getCollectionOperations());
            assertTrue(navigation.getCollectionOperations().isEmpty());
            assertFalse(navigation.hasCollectionOperations());
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should return immutable path list")
        void testPathIsImmutable() {
            List<MetaAttribute<?, ?>> path = new ArrayList<>(Arrays.asList(User_.profile, Profile_.bio));
            PropertyNavigation navigation = new PropertyNavigation(path, null);

            List<MetaAttribute<?, ?>> pathList = navigation.getPath();

            assertThrows(UnsupportedOperationException.class, () -> pathList.add(User_.id));
        }

        @Test
        @DisplayName("Should not be affected by changes to original path list")
        void testOriginalPathModificationDoesNotAffect() {
            List<MetaAttribute<?, ?>> path = new ArrayList<>(Arrays.asList(User_.profile, Profile_.bio));
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            // Modify original list
            path.add(User_.id);
            
            // Navigation should still have 2 elements
            assertEquals(2, navigation.getPath().size());
            assertEquals(User_.profile, navigation.getPath().get(0));
            assertEquals(Profile_.bio, navigation.getPath().get(1));
        }

        @Test
        @DisplayName("Should return immutable collection operations list")
        void testCollectionOperationsIsImmutable() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            List<CollectionOperationMetadata<?, ?>> collectionOps = new ArrayList<>();
            
            PropertyNavigation navigation = new PropertyNavigation(path, collectionOps);
            
            // Try to modify returned collection operations
            List<CollectionOperationMetadata<?, ?>> collectionOperations = navigation.getCollectionOperations();

            assertThrows(UnsupportedOperationException.class, () -> collectionOperations.add(null));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("getPath() should return correct path")
        void testGetPath() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(User_.profile, Profile_.bio);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            List<MetaAttribute<?, ?>> returnedPath = navigation.getPath();
            
            assertNotNull(returnedPath);
            assertEquals(2, returnedPath.size());
            assertEquals(User_.profile, returnedPath.get(0));
            assertEquals(Profile_.bio, returnedPath.get(1));
        }

        @Test
        @DisplayName("getCollectionOperations() should return empty list when null provided")
        void testGetCollectionOperationsWhenNull() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            List<CollectionOperationMetadata<?, ?>> collectionOps = navigation.getCollectionOperations();
            
            assertNotNull(collectionOps);
            assertTrue(collectionOps.isEmpty());
        }

        @Test
        @DisplayName("getLeafClass() should return correct type for single-level path")
        void testGetLeafClassSingleLevel() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            Class<?> leafClass = navigation.getLeafClass();
            
            assertEquals(String.class, leafClass);
        }

        @Test
        @DisplayName("getLeafClass() should return correct type for multi-level path")
        void testGetLeafClassMultiLevel() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(User_.profile, Profile_.bio);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            Class<?> leafClass = navigation.getLeafClass();
            
            assertEquals(String.class, leafClass);
        }

        @Test
        @DisplayName("getRootClass() should return correct type")
        void testGetRootClass() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(User_.profile, Profile_.bio);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            Class<?> rootClass = navigation.getRootClass();
            
            assertEquals(User.class, rootClass);
        }

        @Test
        @DisplayName("getDepth() should return correct path depth")
        void testGetDepth() {
            List<MetaAttribute<?, ?>> singlePath = Collections.singletonList(User_.id);
            PropertyNavigation singleNav = new PropertyNavigation(singlePath, null);
            assertEquals(1, singleNav.getDepth());
            
            List<MetaAttribute<?, ?>> multiPath = Arrays.asList(User_.profile, Profile_.bio);
            PropertyNavigation multiNav = new PropertyNavigation(multiPath, null);
            assertEquals(2, multiNav.getDepth());
        }

        @Test
        @DisplayName("hasCollectionOperations() should return false when no operations")
        void testHasCollectionOperationsFalse() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertFalse(navigation.hasCollectionOperations());
        }

        @Test
        @DisplayName("hasCollectionOperations() should return false when empty list")
        void testHasCollectionOperationsFalseEmptyList() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, Collections.emptyList());
            
            assertFalse(navigation.hasCollectionOperations());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void testEqualsSelf() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertEquals(navigation, navigation);
        }

        @Test
        @DisplayName("Should be equal to another with same path")
        void testEqualsSamePath() {
            List<MetaAttribute<?, ?>> path1 = Arrays.asList(User_.profile, Profile_.bio);
            List<MetaAttribute<?, ?>> path2 = Arrays.asList(User_.profile, Profile_.bio);
            
            PropertyNavigation nav1 = new PropertyNavigation(path1, null);
            PropertyNavigation nav2 = new PropertyNavigation(path2, null);
            
            assertEquals(nav1, nav2);
            assertEquals(nav1.hashCode(), nav2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to another with different path")
        void testNotEqualsDifferentPath() {
            List<MetaAttribute<?, ?>> path1 = Collections.singletonList(User_.id);
            List<MetaAttribute<?, ?>> path2 = Collections.singletonList(User_.name);
            
            PropertyNavigation nav1 = new PropertyNavigation(path1, null);
            PropertyNavigation nav2 = new PropertyNavigation(path2, null);
            
            assertNotEquals(nav1, nav2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void testNotEqualsNull() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertNotEquals(null, navigation);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void testNotEqualsDifferentType() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertNotEquals("string", navigation);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString() should contain path information")
        void testToStringContainsPath() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(User_.profile, Profile_.bio);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            String toString = navigation.toString();
            
            assertNotNull(toString);
            assertTrue(toString.contains("PropertyNavigation"));
            assertTrue(toString.contains("User"));
            assertTrue(toString.contains("Profile"));
        }

        @Test
        @DisplayName("toString() should be non-null for single-level path")
        void testToStringSingleLevel() {
            List<MetaAttribute<?, ?>> path = Collections.singletonList(User_.id);
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            String toString = navigation.toString();
            
            assertNotNull(toString);
            assertFalse(toString.isEmpty());
        }
    }

    @Nested
    @DisplayName("Complex Path Tests")
    class ComplexPathTests {

        @Test
        @DisplayName("Should handle deep nested paths")
        void testDeepNestedPath() {
            List<MetaAttribute<?, ?>> path = Arrays.asList(
                User_.profile,
                Profile_.bio
            );
            
            PropertyNavigation navigation = new PropertyNavigation(path, null);
            
            assertEquals(2, navigation.getDepth());
            assertEquals(User.class, navigation.getRootClass());
            assertEquals(String.class, navigation.getLeafClass());
        }

        @Test
        @DisplayName("Should handle paths from different entity types")
        void testDifferentEntityTypes() {
            List<MetaAttribute<?, ?>> userPath = Collections.singletonList(User_.id);
            List<MetaAttribute<?, ?>> orderPath = Collections.singletonList(Order_.id);
            
            PropertyNavigation userNav = new PropertyNavigation(userPath, null);
            PropertyNavigation orderNav = new PropertyNavigation(orderPath, null);
            
            assertEquals(User.class, userNav.getRootClass());
            assertEquals(Order.class, orderNav.getRootClass());
            assertNotEquals(userNav, orderNav);
        }
    }
}
