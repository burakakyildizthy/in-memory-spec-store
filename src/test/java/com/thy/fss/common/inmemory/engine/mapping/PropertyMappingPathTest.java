package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PropertyMapping path-based functionality.
 * Tests path-based mapping creation, collection path support, and validation.
 * <p>
 * Requirements tested:
 * - 1.x: Nested target property support
 * - 2.x: Nested source field support
 * - 4.x: Path-based primary/foreign key support
 * - 6.x: Collection path support
 * 
 * Task 9.2: PropertyMapping tests
 * - Path-based mapping creation
 * - Collection path support
 * - Validation tests
 */
@DisplayName("PropertyMapping Path-Based Tests")
class PropertyMappingPathTest {

    private static final String STORE1 = "store1";
    private static final String DASHBOARD1 = "dashboard1";
    private static final String CUSTOMERS = "customers";
    private static final String TARGET_PATH_NULL = "Target path cannot be null or empty";
    private static final String DATASOURCE_NAME_NULL = "Datasource name cannot be null or empty";
    private static final String SIZE_MISMATCH = "Composite key field count mismatch: 1 primary key fields vs 2 foreign key fields";

    // ========== Path-Based Mapping Creation Tests ==========

    @Nested
    @DisplayName("Path-Based Mapping Creation")
    class PathBasedMappingCreationTests {

        @Test
        @DisplayName("Should create mapping with single-level target path")
        void testSingleLevelTargetPath() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.id))
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            assertNotNull(mapping.getTargetPath());
            assertEquals(1, mapping.getTargetPath().size());
            assertEquals(Order_.customerId, mapping.getTargetPath().get(0));
        }

        @Test
        @DisplayName("Should create mapping with nested source path")
        void testNestedSourcePath() {
            List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(
                    Customer_.addressId,
                    Address_.cityId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(sourcePath)
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();

            assertNotNull(mapping.getSourcePath());
            assertEquals(2, mapping.getSourcePath().size());
            assertEquals(Customer_.addressId, mapping.getSourcePath().get(0));
            assertEquals(Address_.cityId, mapping.getSourcePath().get(1));
        }

        @Test
        @DisplayName("Should create mapping with nested target path")
        void testNestedTargetPath() {
            List<MetaAttribute<?, ?>> targetPath = Arrays.asList(
                    Order_.customerId,
                    Customer_.addressId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            assertNotNull(mapping.getTargetPath());
            assertEquals(2, mapping.getTargetPath().size());
            assertEquals(Order_.customerId, mapping.getTargetPath().get(0));
            assertEquals(Customer_.addressId, mapping.getTargetPath().get(1));
        }

        @Test
        @DisplayName("Should create mapping with nested primary key path")
        void testNestedPrimaryKeyPath() {
            List<MetaAttribute<?, ?>> primaryKeyPath = Arrays.asList(
                    Customer_.addressId,
                    Address_.cityId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(primaryKeyPath))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(City_.id)))
                    .sourcePath(Collections.singletonList(City_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT)
                    .build();

            assertNotNull(mapping.getPrimaryKeyPaths().get(0));
            assertEquals(2, mapping.getPrimaryKeyPaths().get(0).size());
            assertEquals(Customer_.addressId, mapping.getPrimaryKeyPaths().get(0).get(0));
            assertEquals(Address_.cityId, mapping.getPrimaryKeyPaths().get(0).get(1));
        }

        @Test
        @DisplayName("Should create mapping with nested foreign key path")
        void testNestedForeignKeyPath() {
            List<MetaAttribute<?, ?>> foreignKeyPath = Arrays.asList(
                    Customer_.addressId,
                    Address_.id
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                    .foreignKeyPaths(Collections.singletonList(foreignKeyPath))
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT)
                    .build();

            assertNotNull(mapping.getForeignKeyPaths().get(0));
            assertEquals(2, mapping.getForeignKeyPaths().get(0).size());
            assertEquals(Customer_.addressId, mapping.getForeignKeyPaths().get(0).get(0));
            assertEquals(Address_.id, mapping.getForeignKeyPaths().get(0).get(1));
        }

        @Test
        @DisplayName("Should preserve MetaAttribute reference equality in paths")
        void testMetaAttributeReferenceEquality() {
            List<MetaAttribute<?, ?>> targetPath = Arrays.asList(
                    Order_.customerId,
                    Customer_.addressId
            );

            List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(
                    Customer_.addressId,
                    Address_.cityId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(CUSTOMERS)
                    .sourcePath(sourcePath)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            // Test reference equality
            assertSame(Order_.customerId, mapping.getTargetPath().get(0));
            assertSame(Customer_.addressId, mapping.getTargetPath().get(1));
            assertSame(Customer_.addressId, mapping.getSourcePath().get(0));
            assertSame(Address_.cityId, mapping.getSourcePath().get(1));
        }
    }

    // ========== Validation Tests ==========

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate target path elements are not null")
        void testTargetPathElementsNotNull() {
            List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(
                    Customer_.addressId,
                    null
            );

            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(sourcePath)
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Source path element at index 1 cannot be null"));
        }

        @Test
        @DisplayName("Should validate primary key path elements are not null")
        void testPrimaryKeyPathElementsNotNull() {
            List<MetaAttribute<?, ?>> primaryKeyPath = Arrays.asList(
                    Customer_.addressId
            );
            List<List<MetaAttribute<?, ?>>> primaryKeyPaths = Arrays.asList(primaryKeyPath, null);


            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(primaryKeyPaths)
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Composite key field count mismatch: 2 primary key fields vs 1 foreign key fields"));
        }

        @Test
        @DisplayName("Should validate foreign key path elements are not null")
        void testForeignKeyPathElementsNotNull() {
            List<MetaAttribute<?, ?>> foreignKeyPath = Arrays.asList(
                    Customer_.addressId
            );
            List<List<MetaAttribute<?, ?>>> foreignKeyPaths = Arrays.asList(foreignKeyPath, null);

            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                    .foreignKeyPaths(foreignKeyPaths)
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(SIZE_MISMATCH));
        }
    }

    // ========== Basic Validation Tests ==========

    @Nested
    @DisplayName("Basic Validation Tests")
    class BasicValidationTests {

        @Test
        @DisplayName("Consumer ID cannot be null")
        void testConsumerIdCannotBeNull() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(null)  // This should fail
                    .isForDashboard(false)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .mappingType(MappingType.ONE_TO_ONE);

            NullPointerException exception = assertThrows(NullPointerException.class, builder::build);

            assertTrue(exception.getMessage().contains("Consumer ID cannot be null"));
        }

        @Test
        @DisplayName("Target path cannot be null or empty")
        void testTargetPathCannotBeNull() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(null)  // This should fail
                    .datasourceName(CUSTOMERS)
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(TARGET_PATH_NULL));
        }

        @Test
        @DisplayName("Datasource name cannot be null or empty")
        void testDatasourceNameCannotBeNull() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(null)  // This should fail
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(DATASOURCE_NAME_NULL));
        }

        @Test
        @DisplayName("Mapping type cannot be null")
        void testMappingTypeCannotBeNull() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .mappingType(null);  // This should fail

            NullPointerException exception = assertThrows(NullPointerException.class, builder::build);

            assertTrue(exception.getMessage().contains("Mapping type cannot be null"));
        }

        @Test
        @DisplayName("Dashboard mappings must be MANY_TO_ONE_AGGREGATION type")
        void testDashboardMappingMustBeAggregation() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(DASHBOARD1)
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.ONE_TO_ONE);  // Should fail for dashboard

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Dashboard mappings must be MANY_TO_ONE_AGGREGATION type"));
        }

        @Test
        @DisplayName("Dashboard mappings must have aggregation type")
        void testDashboardMappingMustHaveAggregationType() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(DASHBOARD1)
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);
            // Missing aggregationType

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Dashboard mappings must have an aggregation type"));
        }

        @Test
        @DisplayName("Dashboard mappings cannot have primary/foreign key fields")
        void testDashboardMappingCannotHaveKeys() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(DASHBOARD1)
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))  // Should not be allowed
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))  // Should not be allowed
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("cannot have primary/foreign key fields"));
        }

        @Test
        @DisplayName("Store mappings with relationships must have primary and foreign key fields")
        void testStoreMappingMustHaveKeys() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM);
                    // Missing primaryKeyPath and foreignKeyPath

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Store mappings with relationships must have primary and foreign key fields"));
        }

        @Test
        @DisplayName("Store COUNT aggregation can work without source attribute")
        void testStoreCountWithoutSourceAttribute() {
            // This should NOT throw an exception
            assertDoesNotThrow(() -> {
                PropertyMapping.<Order, Long>builder()
                        .consumerId(STORE1)
                        .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                        .targetPath(Collections.singletonList(Order_.customerId))
                        .datasourceName(CUSTOMERS)
                        .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                        .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                        .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                        .aggregationType(AggregationType.COUNT)
                        // No sourceAttribute needed for COUNT
                        .build();
            }, "COUNT aggregation should work without source attribute");
        }

        @Test
        @DisplayName("Dashboard COUNT aggregation can work without source attribute")
        void testDashboardCountWithoutSourceAttribute() {
            // This should NOT throw an exception
            assertDoesNotThrow(() -> {
                PropertyMapping.<Order, Long>builder()
                        .consumerId(DASHBOARD1)
                        .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                        .targetPath(Collections.singletonList(Order_.customerId))
                        .datasourceName(CUSTOMERS)
                        .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                        .aggregationType(AggregationType.COUNT)
                        // No sourceAttribute needed for COUNT
                        .build();
            }, "COUNT aggregation should work without source attribute");
        }
    }

    // ========== Getters and Utility Method Tests ==========

    @Nested
    @DisplayName("Getters and Utility Methods Tests")
    class GettersAndUtilityMethodsTests {

        @Test
        @DisplayName("All getters should return expected values")
        void testAllGetters() {
            Specification<Customer> spec = SpecificationBuilder.forService(CustomerSpecificationService.INSTANCE)
                    .where(Customer_.addressId).greaterThan(0L);

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .specification(spec)
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();

            assertEquals(STORE1, mapping.getConsumerId());
            assertFalse(mapping.isForDashboard());
            assertEquals(Order_.customerId, mapping.getTargetPath().get(mapping.getTargetPath().size() - 1));
            assertEquals(CUSTOMERS, mapping.getDataSourceName());
            assertEquals(Order_.customerId, mapping.getPrimaryKeyPaths().get(0).get(mapping.getPrimaryKeyPaths().get(0).size() - 1));
            assertEquals(Customer_.id, mapping.getForeignKeyPaths().get(0).get(mapping.getForeignKeyPaths().get(0).size() - 1));
            assertEquals(Customer_.addressId, mapping.getSourcePath().get(mapping.getSourcePath().size() - 1));
            assertEquals(spec, mapping.getSpecification());
            assertEquals(MappingType.MANY_TO_ONE_AGGREGATION, mapping.getMappingType());
            assertEquals(AggregationType.SUM, mapping.getAggregationType());
        }

        @Test
        @DisplayName("toString should include key information")
        void testToString() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();

            String toStringResult = mapping.toString();
            assertTrue(toStringResult.contains(STORE1));
            assertTrue(toStringResult.contains(CUSTOMERS));
            assertTrue(toStringResult.contains("MANY_TO_ONE_AGGREGATION"));
        }

        @Test
        @DisplayName("Dashboard mapping should not require grouping")
        void testDashboardMappingRequiresGrouping() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(DASHBOARD1)
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();

            assertFalse(mapping.requiresGrouping());
        }

        @Test
        @DisplayName("Store mapping with aggregation should require grouping")
        void testStoreMappingWithAggregationRequiresGrouping() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();

            assertTrue(mapping.requiresGrouping());
        }

        @Test
        @DisplayName("Store mapping with collection should require grouping")
        void testStoreMappingWithCollectionRequiresGrouping() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                    .build();

            assertTrue(mapping.requiresGrouping());
        }

        @Test
        @DisplayName("Dashboard mapping builder should validate dashboard requirements")
        void testDashboardMappingBuilder() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId("dashboard2")
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT)
                    .build();

            assertTrue(mapping.isForDashboard());
            assertEquals("dashboard2", mapping.getConsumerId());
            assertEquals(MappingType.MANY_TO_ONE_AGGREGATION, mapping.getMappingType());
            assertEquals(AggregationType.COUNT, mapping.getAggregationType());
        }

        @Test
        @DisplayName("Store mapping without source path should work for certain types")
        void testStoreMappingWithoutSourcePath() {
            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                    .build();

            assertNotNull(mapping.getTargetPath());
            assertNull(mapping.getSourcePath());
        }
    }

    // Test entity placeholder
    private static class TestEntity {
    }
}

