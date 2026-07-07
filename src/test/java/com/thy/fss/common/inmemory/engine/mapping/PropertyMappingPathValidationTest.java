package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.TestEntity;
import com.thy.fss.common.inmemory.testmodel.TestEntity_;
import com.thy.fss.common.inmemory.testmodel.TestEntitySpecificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PropertyMapping path-based validation.
 * Tests path structure validation with MetaAttribute paths.
 * <p>
 * Requirements tested:
 * - 1.4: Path-based property mapping
 */
@DisplayName("PropertyMapping Path Validation Tests")
class PropertyMappingPathValidationTest {

    private static final String STORE1 = "store1";
    private static final String TEST = "test";
    private static final String CANNOT_BE_NULL = "cannot be null";


    // Test MetaAttributes - using real TestEntity_ attributes
    private static final MetaAttribute<TestEntity, String> STRING_ATTR = TestEntity_.name;
    private static final MetaAttribute<TestEntity, Integer> INT_ATTR = TestEntity_.age;
    private static final MetaAttribute<TestEntity, Long> ID_ATTR = TestEntity_.id;

    // ========== Target Path Validation Tests ==========

    @Test
    @DisplayName("Target path with single MetaAttribute should be valid")
    void testSingleMetaAttributePath() {
        List<MetaAttribute<?, ?>> targetPath = Collections.singletonList(STRING_ATTR);

        assertDoesNotThrow(() -> {
            PropertyMapping.<TestEntity, String>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(TestEntitySpecificationService.INSTANCE)
                    .targetService(TestEntitySpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(TEST)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();
        }, "Single MetaAttribute path should be valid");
    }

    @Test
    @DisplayName("Target path with multiple MetaAttributes should be valid")
    void testMultipleMetaAttributesPath() {
        List<MetaAttribute<?, ?>> targetPath = Arrays.asList(STRING_ATTR, INT_ATTR);

        assertDoesNotThrow(() -> {
            PropertyMapping.<TestEntity, Integer>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(TestEntitySpecificationService.INSTANCE)
                    .targetService(TestEntitySpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(TEST)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();
        }, "Multiple MetaAttributes path should be valid");
    }

    @Test
    @DisplayName("Target path cannot be null")
    void testNullTargetPath() {
        PropertyMapping.Builder<TestEntity, String> builder = PropertyMapping.<TestEntity, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(TestEntitySpecificationService.INSTANCE)
                .targetService(TestEntitySpecificationService.INSTANCE)
                .targetPath(null)
                .datasourceName(TEST)
                .mappingType(MappingType.ONE_TO_ONE);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                builder::build,
                "Null target path should throw exception");

        assertTrue(exception.getMessage().contains("Target path cannot be null"));
    }

    @Test
    @DisplayName("Target path cannot be empty")
    void testEmptyTargetPath() {
        PropertyMapping.Builder<TestEntity, String> builder = PropertyMapping.<TestEntity, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(TestEntitySpecificationService.INSTANCE)
                .targetService(TestEntitySpecificationService.INSTANCE)
                .targetPath(Collections.emptyList())
                .datasourceName(TEST)
                .mappingType(MappingType.ONE_TO_ONE);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                builder::build,
                "Empty target path should throw exception");

        assertTrue(exception.getMessage().contains("Target path cannot be null or empty"));
    }

    @Test
    @DisplayName("Target path with null element should throw exception")
    void testNullTargetPathElement() {
        List<MetaAttribute<?, ?>> targetPath = Arrays.asList(STRING_ATTR, null);

        PropertyMapping.Builder<TestEntity, String> builder = PropertyMapping.<TestEntity, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(TestEntitySpecificationService.INSTANCE)
                .targetService(TestEntitySpecificationService.INSTANCE)
                .targetPath(targetPath)
                .datasourceName(TEST)
                .mappingType(MappingType.ONE_TO_ONE);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                builder::build,
                "Null path element should throw exception");

        assertTrue(exception.getMessage().contains(CANNOT_BE_NULL));
    }

    // ========== Source Path Validation Tests ==========

    @Test
    @DisplayName("Source path with MetaAttributes should be valid")
    void testValidSourcePath() {
        List<MetaAttribute<?, ?>> targetPath = Collections.singletonList(INT_ATTR);
        List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(STRING_ATTR, INT_ATTR);

        assertDoesNotThrow(() -> {
            PropertyMapping.<TestEntity, Integer>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(TestEntitySpecificationService.INSTANCE)
                    .targetService(TestEntitySpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .sourcePath(sourcePath)
                    .datasourceName(TEST)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        }, "Valid source path should be accepted");
    }

    @Test
    @DisplayName("Source path with null element should throw exception")
    void testNullSourcePathElement() {
        List<MetaAttribute<?, ?>> targetPath = Collections.singletonList(INT_ATTR);
        List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(STRING_ATTR, null);

        PropertyMapping.Builder<TestEntity, Integer> builder = PropertyMapping.<TestEntity, Integer>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(TestEntitySpecificationService.INSTANCE)
                .targetService(TestEntitySpecificationService.INSTANCE)
                .targetPath(targetPath)
                .sourcePath(sourcePath)
                .datasourceName(TEST)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(ID_ATTR)))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                builder::build,
                "Null source path element should throw exception");

        assertTrue(exception.getMessage().contains(CANNOT_BE_NULL));
    }

    // ========== Primary/Foreign Key Path Validation Tests ==========

    @Test
    @DisplayName("Primary key path with MetaAttributes should be valid")
    void testValidPrimaryKeyPath() {
        List<MetaAttribute<?, ?>> targetPath = Collections.singletonList(INT_ATTR);
        List<MetaAttribute<?, ?>> primaryKeyPath = Arrays.asList(STRING_ATTR, ID_ATTR);
        List<MetaAttribute<?, ?>> foreignKeyPath = Collections.singletonList(ID_ATTR);

        assertDoesNotThrow(() -> {
            PropertyMapping.<TestEntity, Integer>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(TestEntitySpecificationService.INSTANCE)
                    .targetService(TestEntitySpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(TEST)
                    .primaryKeyPaths(Collections.singletonList(primaryKeyPath))
                    .foreignKeyPaths(Collections.singletonList(foreignKeyPath))
                    .sourcePath(Collections.singletonList(INT_ATTR))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        }, "Valid primary key path should be accepted");
    }

    @Test
    @DisplayName("Foreign key path with MetaAttributes should be valid")
    void testValidForeignKeyPath() {
        List<MetaAttribute<?, ?>> targetPath = Collections.singletonList(INT_ATTR);
        List<MetaAttribute<?, ?>> primaryKeyPath = Collections.singletonList(ID_ATTR);
        List<MetaAttribute<?, ?>> foreignKeyPath = Arrays.asList(STRING_ATTR, ID_ATTR);

        assertDoesNotThrow(() -> {
            PropertyMapping.<TestEntity, Integer>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(TestEntitySpecificationService.INSTANCE)
                    .targetService(TestEntitySpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(TEST)
                    .primaryKeyPaths(Collections.singletonList(primaryKeyPath))
                    .foreignKeyPaths(Collections.singletonList(foreignKeyPath))
                    .sourcePath(Collections.singletonList(INT_ATTR))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        }, "Valid foreign key path should be accepted");
    }

    // ========== Path Getter Tests ==========

    @Test
    @DisplayName("Path getters should return correct paths")
    void testPathGetters() {
        List<MetaAttribute<?, ?>> targetPath = Arrays.asList(STRING_ATTR, INT_ATTR);
        List<MetaAttribute<?, ?>> sourcePath = Collections.singletonList(INT_ATTR);
        List<MetaAttribute<?, ?>> primaryKeyPath = Collections.singletonList(ID_ATTR);
        List<MetaAttribute<?, ?>> foreignKeyPath = Collections.singletonList(ID_ATTR);

        PropertyMapping<TestEntity, Integer> mapping = PropertyMapping.<TestEntity, Integer>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(TestEntitySpecificationService.INSTANCE)
                .targetService(TestEntitySpecificationService.INSTANCE)
                .targetPath(targetPath)
                .sourcePath(sourcePath)
                .primaryKeyPaths(Collections.singletonList(primaryKeyPath))
                .foreignKeyPaths(Collections.singletonList(foreignKeyPath))
                .datasourceName(TEST)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertEquals(targetPath, mapping.getTargetPath(), "Target path should match");
        assertEquals(sourcePath, mapping.getSourcePath(), "Source path should match");
        assertEquals(primaryKeyPath, mapping.getPrimaryKeyPaths().get(0), "Primary key path should match");
        assertEquals(foreignKeyPath, mapping.getForeignKeyPaths().get(0), "Foreign key path should match");
    }
}
