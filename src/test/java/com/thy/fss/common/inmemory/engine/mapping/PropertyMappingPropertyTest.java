package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSource_;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestSourceSpecificationService;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTargetSpecificationService;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for PropertyMapping composite key validation.
 * 
 * <p>Tests three key properties:</p>
 * <ul>
 *   <li><b>Property 2: Field count validation</b> - Validates: Requirements 1.2</li>
 *   <li><b>Property 4: Type mismatch validation</b> - Validates: Requirements 1.4</li>
 *   <li><b>Property 7: Error message clarity</b> - Validates: Requirements 2.5</li>
 * </ul>
 */
class PropertyMappingPropertyTest {

    private static final String TEST_SOURCE = "test-source";
    private static final String TEST_STORE = "test-store";

    /**
     * Property 2: Field count validation
     * 
     * For any number N of primary key fields and any number M of foreign key fields 
     * where N ≠ M, attempting to build the mapping should throw an IllegalStateException 
     * with a message indicating the field count mismatch.
     * 
     * <p><b>Feature: composite-key-mapping, Property 2: Field count validation</b></p>
     * <p><b>Validates: Requirements 1.2</b></p>
     */
    @Property(tries = 100)
    void compositKeyFieldCountMismatchShouldThrowException(
            @ForAll @IntRange(min = 1, max = 5) int primaryKeyCount,
            @ForAll @IntRange(min = 1, max = 5) int foreignKeyCount) {
        
        // Only test when counts are different
        if (primaryKeyCount == foreignKeyCount) {
            return;
        }

// Given: Different numbers of primary and foreign key fields
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = createKeyPaths(primaryKeyCount, true);
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = createKeyPaths(foreignKeyCount, false);

        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping.Builder<TestTarget, Long> builder = PropertyMapping.<TestTarget, Long>builder()
                .consumerId(TEST_STORE)
                .isForDashboard(false)
                .targetPath(List.of(TestTarget_.id))
                .sourcePath(List.of(TestSource_.targetId))
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .sourceService(sourceSvc)
                .targetService(targetSvc)
                .datasourceName(TEST_SOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

// When/Then: Building the mapping should throw IllegalStateException
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Composite key field count mismatch")
                .hasMessageContaining(String.valueOf(primaryKeyCount))
                .hasMessageContaining(String.valueOf(foreignKeyCount));
        
    }
    
    /**
     * Property 4: Type mismatch validation
     * 
     * For any composite key definition where the type of the primary key field at 
     * position i does not match the type of the foreign key field at position i, 
     * building the mapping should throw an IllegalStateException with a message 
     * indicating the type mismatch and position.
     * 
     * <p><b>Feature: composite-key-mapping, Property 4: Type mismatch validation</b></p>
     * <p><b>Validates: Requirements 1.4</b></p>
     */
    @Property(tries = 100)
    void compositeKeyTypeMismatchShouldThrowException(
            @ForAll @IntRange(min = 1, max = 5) int keyFieldCount,
            @ForAll @IntRange(min = 0, max = 4) int mismatchPosition) {
        
        // Only test when mismatch position is within bounds
        if (mismatchPosition >= keyFieldCount) {
            return;
        }
        
        // Given: Composite keys with a type mismatch at a specific position
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = new ArrayList<>();
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = new ArrayList<>();
        
        for (int i = 0; i < keyFieldCount; i++) {
            if (i == mismatchPosition) {
                // Create type mismatch: Long vs String
                primaryKeyPaths.add(List.of(TestTarget_.id));  // Long
                foreignKeyPaths.add(List.of(TestSource_.targetCode));  // String
            } else {
                // Matching types
                primaryKeyPaths.add(List.of(TestTarget_.id));  // Long
                foreignKeyPaths.add(List.of(TestSource_.targetId));  // Long
            }
        }

        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping.Builder<TestTarget, Long> builder = PropertyMapping.<TestTarget, Long>builder()
                .consumerId(TEST_STORE)
                .isForDashboard(false)
                .targetPath(List.of(TestTarget_.id))
                .sourcePath(List.of(TestSource_.targetId))
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .sourceService(sourceSvc)
                .targetService(targetSvc)
                .datasourceName(TEST_SOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

// When/Then: Error message should contain position and type information
        assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Composite key field type mismatch")
        .hasMessageContaining("position " + mismatchPosition);
    }
    
    /**
     * Property 7: Error message clarity
     * 
     * For any validation error (field count mismatch, type mismatch), the exception 
     * message should include specific details about what went wrong (expected vs actual 
     * counts, mismatched types and positions).
     * 
     * <p><b>Feature: composite-key-mapping, Property 7: Error message clarity</b></p>
     * <p><b>Validates: Requirements 2.5</b></p>
     */
    @Property(tries = 100)
    void validationErrorsShouldHaveClearMessages(
            @ForAll @IntRange(min = 1, max = 5) int primaryKeyCount,
            @ForAll @IntRange(min = 1, max = 5) int foreignKeyCount) {
        
        // Only test when counts are different (to trigger validation error)
        if (primaryKeyCount == foreignKeyCount) {
            return;
        }
        
        // Given: Mismatched key field counts
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = createKeyPaths(primaryKeyCount, true);
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = createKeyPaths(foreignKeyCount, false);

        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping.Builder<TestTarget, Long> builder = PropertyMapping.<TestTarget, Long>builder()
                .consumerId(TEST_STORE)
                .isForDashboard(false)
                .targetPath(List.of(TestTarget_.id))
                .sourcePath(List.of(TestSource_.targetId))
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .sourceService(sourceSvc)
                .targetService(targetSvc)
                .datasourceName(TEST_SOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);


        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .satisfies(exception -> {
                    String message = exception.getMessage();

                    // Message should contain both counts
                    assertThat(message).contains(String.valueOf(primaryKeyCount));
                    assertThat(message).contains(String.valueOf(foreignKeyCount));

                    // Message should indicate it's about composite keys
                    assertThat(message.toLowerCase()).contains("composite key");

                    // Message should indicate it's about field count
                    assertThat(message.toLowerCase()).contains("field count");

                    // Message should indicate it's a mismatch
                    assertThat(message.toLowerCase()).contains("mismatch");
                });
    }
    
    /**
     * Property 7 (variant): Type mismatch error messages should include position and types
     * 
     * When a type mismatch occurs, the error message should clearly indicate:
     * - The position where the mismatch occurred
     * - The type of the primary key field
     * - The type of the foreign key field
     */
    @Property(tries = 100)
    void typeMismatchErrorsShouldIncludePositionAndTypes(
            @ForAll @IntRange(min = 2, max = 5) int keyFieldCount,
            @ForAll @IntRange(min = 0, max = 4) int mismatchPosition) {
        
        // Only test when mismatch position is within bounds
        if (mismatchPosition >= keyFieldCount) {
            return;
        }
        
        // Given: Composite keys with a type mismatch
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = new ArrayList<>();
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = new ArrayList<>();
        
        for (int i = 0; i < keyFieldCount; i++) {
            if (i == mismatchPosition) {
                // Create type mismatch: Long vs String
                primaryKeyPaths.add(List.of(TestTarget_.id));  // Long
                foreignKeyPaths.add(List.of(TestSource_.targetCode));  // String
            } else {
                // Matching types
                primaryKeyPaths.add(List.of(TestTarget_.id));  // Long
                foreignKeyPaths.add(List.of(TestSource_.targetId));  // Long
            }
        }

        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping.Builder<TestTarget, Long> builder = PropertyMapping.<TestTarget, Long>builder()
                .consumerId(TEST_STORE)
                .isForDashboard(false)
                .targetPath(List.of(TestTarget_.id))
                .sourcePath(List.of(TestSource_.targetId))
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .sourceService(sourceSvc)
                .targetService(targetSvc)
                .datasourceName(TEST_SOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        // When/Then: Error message should contain position and type information
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .satisfies(exception -> {
                    String message = exception.getMessage();

                    // Message should contain the position
                    assertThat(message).contains("position " + mismatchPosition);

                    // Message should contain both type names
                    assertThat(message).contains("Long");
                    assertThat(message).contains("String");

                    // Message should indicate it's about type mismatch
                    assertThat(message.toLowerCase()).contains("type mismatch");
                });
    }
    
    /**
     * Helper method to create key paths for testing.
     * Creates a list of single-attribute paths.
     */
    private List<List<MetaAttribute<?, ?>>> createKeyPaths(int count, boolean isPrimaryKey) {
        List<List<MetaAttribute<?, ?>>> paths = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            MetaAttribute<?, ?> attribute;
            if (isPrimaryKey) {
                // Cycle through target attributes
                attribute = switch (i % 5) {
                    case 0 -> TestTarget_.id;
                    case 1 -> TestTarget_.code;
                    case 2 -> TestTarget_.version;
                    case 3 -> TestTarget_.region;
                    case 4 -> TestTarget_.timestamp;
                    default -> TestTarget_.id;
                };
            } else {
                // Cycle through source attributes
                attribute = switch (i % 5) {
                    case 0 -> TestSource_.targetId;
                    case 1 -> TestSource_.targetCode;
                    case 2 -> TestSource_.targetVersion;
                    case 3 -> TestSource_.targetRegion;
                    case 4 -> TestSource_.targetTimestamp;
                    default -> TestSource_.targetId;
                };
            }
            paths.add(List.of(attribute));
        }
        
        return paths;
    }
    
    /**
     * Basic unit test: Valid composite key should not throw exception
     */
    @Test
    void validCompositeKeyShouldNotThrowException() {
        // Given: Valid composite key with matching field counts and types
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = List.of(
            List.of(TestTarget_.id),
            List.of(TestTarget_.code)
        );
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = List.of(
            List.of(TestSource_.targetId),
            List.of(TestSource_.targetCode)
        );
        
        // When/Then: Building the mapping should succeed
        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping<TestTarget, Long> mapping = PropertyMapping.<TestTarget, Long>builder()
            .consumerId(TEST_STORE)
            .isForDashboard(false)
            .targetPath(List.of(TestTarget_.id))
            .sourcePath(List.of(TestSource_.targetId))
            .primaryKeyPaths(primaryKeyPaths)
            .foreignKeyPaths(foreignKeyPaths)
            .sourceService(sourceSvc)
            .targetService(targetSvc)
            .datasourceName(TEST_SOURCE)
            .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
            .aggregationType(AggregationType.SUM)
            .build();
        
        assertThat(mapping).isNotNull();
        assertThat(mapping.isCompositeKey()).isTrue();
        assertThat(mapping.getKeyFieldCount()).isEqualTo(2);
    }
    
    /**
     * Basic unit test: Single-field key should work
     */
    @Test
    void singleFieldKeyShouldWork() {
        // Given: Single-field key (treated as composite with size 1)
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = List.of(
            List.of(TestTarget_.id)
        );
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = List.of(
            List.of(TestSource_.targetId)
        );
        
        // When/Then: Building the mapping should succeed
        SpecificationService<TestTarget> targetSvc = TestTargetSpecificationService.INSTANCE;
        SpecificationService<TestSource> sourceSvc = TestSourceSpecificationService.INSTANCE;

        PropertyMapping<TestTarget, Long> mapping = PropertyMapping.<TestTarget, Long>builder()
            .consumerId(TEST_STORE)
            .isForDashboard(false)
            .targetPath(List.of(TestTarget_.id))
            .sourcePath(List.of(TestSource_.targetId))
            .primaryKeyPaths(primaryKeyPaths)
            .foreignKeyPaths(foreignKeyPaths)
            .sourceService(sourceSvc)
            .targetService(targetSvc)
            .datasourceName(TEST_SOURCE)
            .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
            .aggregationType(AggregationType.SUM)
            .build();
        
        assertThat(mapping).isNotNull();
        assertThat(mapping.isCompositeKey()).isFalse();  // Size 1 is not composite
        assertThat(mapping.getKeyFieldCount()).isEqualTo(1);
    }
}
