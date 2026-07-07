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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PropertyMapping class.
 * Tests path-based mapping creation, collection path support, validation,
 * mapping type validation, isForDashboard() logic, requiresGrouping() logic,
 * and Store vs Dashboard mapping differences.
 * <p>
 * Requirements tested:
 * - 1.x: Nested target property support
 * - 2.x: Nested source field support
 * - 4.x: Path-based primary/foreign key support
 * - 5.1: Type-safe mapping configuration
 * - 6.x: Collection path support
 */
@DisplayName("PropertyMapping Tests")
class PropertyMappingTest {

    private static final String STORE1_CONSUMER_ID = "store1";
    private static final String DASHBOARD1_CONSUMER_ID = "dashboard1";
    private static final String ORDERS_DATASOURCE = "orders";
    private static final String CUSTOMERS_DATASOURCE = "customers";
    private static final String ADDRESSES_DATASOURCE = "addresses";
    private static final String TARGET_PATH_NULL_OR_EMPTY_MESSAGE = "Target path cannot be null or empty";
    private static final String DATASOURCE_NAME_NULL_OR_EMPTY_MESSAGE = "Datasource name cannot be null or empty";
    private static final String MUST_HAVE_AGGREGATION_TYPE_MESSAGE = "must have an aggregation type";
    private static final String MUST_HAVE_SOURCE_ATTRIBUTE_MESSAGE = "must have a source attribute";
    private static final String CANNOT_HAVE_PRIMARY_FOREIGN_KEY_MESSAGE = "cannot have primary/foreign key fields";

    // ========== Path-Based Mapping Creation Tests ==========

    @Nested
    @DisplayName("Path-Based Mapping Creation")
    class PathBasedMappingCreationTests {

        @Test
        @DisplayName("Should create mapping with single-level target path")
        void testSingleLevelTargetPath() {
            PropertyMapping<Order, Double> mapping = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .sourceService(OrderSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.totalAmount))
                    .datasourceName(ORDERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            assertNotNull(mapping.getTargetPath());
            assertEquals(1, mapping.getTargetPath().size());
            assertEquals(Order_.totalAmount, mapping.getTargetPath().get(0));
        }

        @Test
        @DisplayName("Should create mapping with multi-level target path")
        void testMultiLevelTargetPath() {
            List<MetaAttribute<?, ?>> targetPath = Arrays.asList(
                    Customer_.name,
                    Address_.street,
                    City_.name
            );

            PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName("cities")
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.addressId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Address_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            assertNotNull(mapping.getTargetPath());
            assertEquals(3, mapping.getTargetPath().size());
            assertEquals(Customer_.name, mapping.getTargetPath().get(0));
            assertEquals(Address_.street, mapping.getTargetPath().get(1));
            assertEquals(City_.name, mapping.getTargetPath().get(2));
        }

        @Test
        @DisplayName("Should create mapping with nested source path")
        void testNestedSourcePath() {
            List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(
                    Customer_.addressId,
                    Address_.cityId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
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
        @DisplayName("Should create mapping with nested primary key path")
        void testNestedPrimaryKeyPath() {
            List<MetaAttribute<?, ?>> primaryKeyPath = Arrays.asList(
                    Customer_.addressId,
                    Address_.cityId
            );

            PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
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
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
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
                    Customer_.name,
                    Address_.street
            );

            PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(ADDRESSES_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.addressId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Address_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();

            // Verify reference equality (not just equals)
            assertSame(Customer_.name, mapping.getTargetPath().get(0));
            assertSame(Address_.street, mapping.getTargetPath().get(1));
        }
    }

    // ========== Collection Path Support Tests ==========

    @Nested
    @DisplayName("Collection Path Support")
    class CollectionPathSupportTests {

        @Test
        @DisplayName("Should validate path elements are not null")
        void testPathElementsNotNull() {
            List<MetaAttribute<?, ?>> targetPath = Arrays.asList(
                    Customer_.name,
                    null,
                    Address_.street
            );

            PropertyMapping.Builder<Customer, String> builder = PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(targetPath)
                    .datasourceName(ADDRESSES_DATASOURCE)
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("path element at index 1 cannot be null"));
        }

        @Test
        @DisplayName("Should validate source path elements are not null")
        void testSourcePathElementsNotNull() {
            List<MetaAttribute<?, ?>> sourcePath = Arrays.asList(
                    Customer_.addressId,
                    null
            );

            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
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
        void testForeignKeyPathElementsNotNull() {
            List<MetaAttribute<?, ?>> foreignKeyPath = Arrays.asList(
                    Customer_.addressId
            );
            List<List<MetaAttribute<?, ?>>> foreignKeyPaths = Arrays.asList(foreignKeyPath, null);

            List<MetaAttribute<?, ?>> primaryKeyPath = Arrays.asList(
                    Order_.id
            );
            List<List<MetaAttribute<?, ?>>> primaryKeyPaths = Arrays.asList(primaryKeyPath, null);
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .foreignKeyPaths(foreignKeyPaths)
                    .primaryKeyPaths(primaryKeyPaths)
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("Primary key path at position 1 cannot be null or empty"));
        }
    }

    // ========== Validation Tests ==========

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null consumer ID")
        void testNullConsumerId() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(null)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.totalAmount))
                    .datasourceName(ORDERS_DATASOURCE)
                    .mappingType(MappingType.ONE_TO_ONE);

            NullPointerException exception = assertThrows(NullPointerException.class, builder::build);

            assertTrue(exception.getMessage().contains("Consumer ID cannot be null"));
        }

        @Test
        @DisplayName("Should reject null mapping type")
        void testNullMappingType() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.totalAmount))
                    .datasourceName(ORDERS_DATASOURCE)
                    .mappingType(null);

            NullPointerException exception = assertThrows(NullPointerException.class, builder::build);

            assertTrue(exception.getMessage().contains("Mapping type cannot be null"));
        }

        @Test
        @DisplayName("Should reject null target path")
        void testNullTargetPath() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(null)
                    .datasourceName(ORDERS_DATASOURCE)
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(TARGET_PATH_NULL_OR_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("Should reject empty target path")
        void testEmptyTargetPath() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.emptyList())
                    .datasourceName(ORDERS_DATASOURCE)
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(TARGET_PATH_NULL_OR_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("Should reject null datasource name")
        void testNullDatasourceName() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.totalAmount))
                    .datasourceName(null)
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(DATASOURCE_NAME_NULL_OR_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("Should reject empty datasource name")
        void testEmptyDatasourceName() {
            PropertyMapping.Builder<Order, Double> builder = PropertyMapping.<Order, Double>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.totalAmount))
                    .datasourceName("")
                    .mappingType(MappingType.ONE_TO_ONE);

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains(DATASOURCE_NAME_NULL_OR_EMPTY_MESSAGE));
        }

        @Test
        @DisplayName("Should require relationship keys for Store mappings with relationships")
        void testStoreRequiresRelationshipKeys() {
            PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .sourcePath(Collections.singletonList(Customer_.name))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM);
            // Missing primaryKeyPath and foreignKeyPath

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("must have primary and foreign key fields"));
        }

        @Test
        @DisplayName("Should accept ONE_TO_ONE with null sourcePath when PK/FK are defined (direct model reference)")
        void testOneToOneNullSourcePathWithPkFkDefined() {
            // This is the valid direct model reference case:
            // ONE_TO_ONE + sourcePath=null + PK/FK defined + store mapping
            PropertyMapping<Order, Customer> mapping = assertDoesNotThrow(() ->
                    PropertyMapping.<Order, Customer>builder()
                            .consumerId(STORE1_CONSUMER_ID)
                            .isForDashboard(false)
                            .sourceService(CustomerSpecificationService.INSTANCE)
                            .targetService(OrderSpecificationService.INSTANCE)
                            .targetPath(Collections.singletonList(Order_.customerId))
                            .datasourceName(CUSTOMERS_DATASOURCE)
                            .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                            .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                            .sourcePath(null)
                            .mappingType(MappingType.ONE_TO_ONE)
                            .build()
            );

            assertNotNull(mapping);
            assertEquals(MappingType.ONE_TO_ONE, mapping.getMappingType());
            assertNull(mapping.getSourcePath());
            assertFalse(mapping.isForDashboard());
            assertNotNull(mapping.getPrimaryKeyPaths());
            assertFalse(mapping.getPrimaryKeyPaths().isEmpty());
            assertNotNull(mapping.getForeignKeyPaths());
            assertFalse(mapping.getForeignKeyPaths().isEmpty());
        }

        @Test
        @DisplayName("Should reject ONE_TO_ONE with null sourcePath when PK/FK are NOT defined")
        void testOneToOneNullSourcePathWithoutPkFkRejects() {
            // This is the invalid case: ONE_TO_ONE + sourcePath=null + NO PK/FK + store mapping
            // Without PK/FK, the system cannot locate the related entity, so this must be rejected.
            PropertyMapping.Builder<Order, Customer> builder = PropertyMapping.<Order, Customer>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .sourcePath(null)
                    .mappingType(MappingType.ONE_TO_ONE);
            // No primaryKeyPaths and no foreignKeyPaths

            IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

            assertTrue(exception.getMessage().contains("must have primary and foreign key fields"));
        }
    }

    // ========== Mapping Type Validation Tests ==========

    @Test
    @DisplayName("ONE_TO_ONE mapping should be validated for Store")
    void testOneToOneMappingForStore() {
        assertDoesNotThrow(() -> {
            PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Customer_.name))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.addressId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Address_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();
        }, "ONE_TO_ONE mapping should be valid for Store");
    }


    @DisplayName("MANY_TO_ONE_COLLECTION mapping should be validated for Store")
    void testManyToOneCollectionMappingForStore() {
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, java.util.Collection<Customer>>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                    .build();
        }, "MANY_TO_ONE_COLLECTION mapping should be valid for Store");
    }

    @Test
    @DisplayName("MANY_TO_ONE_AGGREGATION mapping should be validated for Store")
    void testManyToOneAggregationMappingForStore() {
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        }, "MANY_TO_ONE_AGGREGATION mapping should be valid for Store");
    }

    @Test
    @DisplayName("Dashboard mapping must be MANY_TO_ONE_AGGREGATION type")
    void testDashboardMappingMustBeAggregation() {
        PropertyMapping.Builder<Customer, String> builder = PropertyMapping.<Customer, String>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(AddressSpecificationService.INSTANCE)
                .targetService(CustomerSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Customer_.name))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .mappingType(MappingType.ONE_TO_ONE);

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains("Dashboard mappings must be MANY_TO_ONE_AGGREGATION"));
    }

    @Test
    @DisplayName("Dashboard mapping must have aggregation type")
    void testDashboardMappingMustHaveAggregationType() {
        PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);
        // Missing aggregationType

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains(MUST_HAVE_AGGREGATION_TYPE_MESSAGE));
    }

    @Test
    @DisplayName("Dashboard mapping must have source attribute for non-COUNT aggregations")
    void testDashboardMappingMustHaveSourceAttribute() {
        PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                // Missing sourceAttribute
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains(MUST_HAVE_SOURCE_ATTRIBUTE_MESSAGE));
    }

    @Test
    @DisplayName("Dashboard mapping cannot have primary/foreign key fields")
    void testDashboardMappingCannotHaveKeys() {
        PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))  // Should not be allowed
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))  // Should not be allowed
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains(CANNOT_HAVE_PRIMARY_FOREIGN_KEY_MESSAGE));
    }

    @Test
    @DisplayName("Aggregation mapping must have aggregation type")
    void testAggregationMappingMustHaveAggregationType() {
        PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);
        // Missing aggregationType

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains(MUST_HAVE_AGGREGATION_TYPE_MESSAGE));
    }

    @Test
    @DisplayName("Aggregation mapping (except COUNT) must have source attribute")
    void testAggregationMappingMustHaveSourceAttribute() {
        PropertyMapping.Builder<Order, Long> builder = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                // Missing sourceAttribute
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(exception.getMessage().contains(MUST_HAVE_SOURCE_ATTRIBUTE_MESSAGE));
    }

    @Test
    @DisplayName("COUNT aggregation can work without source attribute")
    void testCountAggregationWithoutSourceAttribute() {
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.COUNT)
                    // No sourceAttribute needed for COUNT
                    .build();
        }, "COUNT aggregation should work without source attribute");
    }

    // ========== isForDashboard() Logic Tests ==========

    @Test
    @DisplayName("isForDashboard should return true for Dashboard mappings")
    void testIsForDashboardTrue() {
        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertTrue(mapping.isForDashboard(), "isForDashboard should return true");
    }

    @Test
    @DisplayName("isForDashboard should return false for Store mappings")
    void testIsForDashboardFalse() {
        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertFalse(mapping.isForDashboard(), "isForDashboard should return false");
    }

    // ========== requiresGrouping() Logic Tests ==========

    @Test
    @DisplayName("requiresGrouping should return false for Dashboard mappings")
    void testRequiresGroupingFalseForDashboard() {
        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertFalse(mapping.requiresGrouping(), "Dashboard mappings should not require grouping");
    }

    @Test
    @DisplayName("requiresGrouping should return true for Store mappings with keys")
    void testRequiresGroupingTrueForStoreWithKeys() {
        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertTrue(mapping.requiresGrouping(), "Store mappings with keys should require grouping");
    }

    @Test
    @DisplayName("Building a store mapping without keys should throw IllegalStateException")
    void testRequiresGroupingFalseForStoreWithoutKeys() {
        PropertyMapping.Builder<Customer, String> builder = PropertyMapping.<Customer, String>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(AddressSpecificationService.INSTANCE)
                .targetService(CustomerSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Customer_.name))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .mappingType(MappingType.ONE_TO_ONE);

        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
                "Store mappings without keys should throw IllegalStateException");
        assertTrue(exception.getMessage().contains("must have primary and foreign key fields"));
    }

    // ========== Store vs Dashboard Mapping Differences Tests ==========

    @Test
    @DisplayName("Store mapping should allow all mapping types")
    void testStoreMappingAllowsAllTypes() {
        // ONE_TO_ONE
        assertDoesNotThrow(() -> {
            PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Customer_.name))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.addressId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Address_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    .build();
        });

        // MANY_TO_ONE_COLLECTION
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, java.util.Collection<Customer>>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                    .build();
        });

        // MANY_TO_ONE_AGGREGATION
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, Long>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        });
    }

    @Test
    @DisplayName("Dashboard mapping should only allow MANY_TO_ONE_AGGREGATION")
    void testDashboardMappingOnlyAllowsAggregation() {
        // MANY_TO_ONE_AGGREGATION - should work
        assertDoesNotThrow(() -> {
            PropertyMapping.<Order, Long>builder()
                    .consumerId(DASHBOARD1_CONSUMER_ID)
                    .isForDashboard(true)
                    .sourceService(CustomerSpecificationService.INSTANCE)
                    .targetService(OrderSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Order_.customerId))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .sourcePath(Collections.singletonList(Customer_.addressId))
                    .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                    .aggregationType(AggregationType.SUM)
                    .build();
        });

        // ONE_TO_ONE - should fail
        PropertyMapping.Builder<Customer, String> oneToOneBuilder = PropertyMapping.<Customer, String>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(AddressSpecificationService.INSTANCE)
                .targetService(CustomerSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Customer_.name))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .mappingType(MappingType.ONE_TO_ONE);

        assertThrows(IllegalStateException.class, oneToOneBuilder::build);

        // MANY_TO_ONE_COLLECTION - should fail
        PropertyMapping.Builder<Order, java.util.Collection<Customer>> collectionBuilder = PropertyMapping.<Order, java.util.Collection<Customer>>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION);

        assertThrows(IllegalStateException.class, collectionBuilder::build);
    }

    @Test
    @DisplayName("Store and Dashboard mappings should have different validation rules")
    void testStoreVsDashboardValidationDifferences() {
        // Store: Can have mapping without aggregation type
        assertDoesNotThrow(() -> PropertyMapping.<Customer, String>builder()
                    .consumerId(STORE1_CONSUMER_ID)
                    .isForDashboard(false)
                    .sourceService(AddressSpecificationService.INSTANCE)
                    .targetService(CustomerSpecificationService.INSTANCE)
                    .targetPath(Collections.singletonList(Customer_.name))
                    .datasourceName(CUSTOMERS_DATASOURCE)
                    .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.addressId)))
                    .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Address_.id)))
                    .mappingType(MappingType.ONE_TO_ONE)
                    // No aggregationType
                    .build());

        // Dashboard: Must have aggregation type
        PropertyMapping.Builder<Order, Long> dashboardBuilder = PropertyMapping.<Order, Long>builder()
                .consumerId(DASHBOARD1_CONSUMER_ID)
                .isForDashboard(true)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);
        // Missing aggregationType

        assertThrows(IllegalStateException.class, dashboardBuilder::build);
    }

    // ========== Getters and Utility Methods Tests ==========

    @Test
    @DisplayName("All getters should return correct values")
    void testGetters() {
        Specification<Customer> spec = SpecificationBuilder.forService(CustomerSpecificationService.INSTANCE)
                .where(Customer_.addressId).greaterThan(0L);

        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .specification(spec)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertEquals(STORE1_CONSUMER_ID, mapping.getConsumerId());
        assertFalse(mapping.isForDashboard());
        assertEquals(Order_.customerId, mapping.getTargetPath().get(mapping.getTargetPath().size() - 1));
        assertEquals(CUSTOMERS_DATASOURCE, mapping.getDataSourceName());
        assertEquals(Customer_.addressId, mapping.getSourcePath().get(mapping.getSourcePath().size() - 1));
        assertEquals(spec, mapping.getSpecification());
        assertEquals(MappingType.MANY_TO_ONE_AGGREGATION, mapping.getMappingType());
        assertEquals(AggregationType.SUM, mapping.getAggregationType());
    }

    @Test
    @DisplayName("toString should include key information")
    void testToString() {
        PropertyMapping<Order, Long> mapping = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        String str = mapping.toString();
        assertThat(str)
                .contains(STORE1_CONSUMER_ID)
                .contains("customerId")
                .contains(CUSTOMERS_DATASOURCE)
                .contains("MANY_TO_ONE_AGGREGATION")
                .contains("SUM");
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void testEqualsAndHashCode() {
        PropertyMapping<Order, Long> mapping1 = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        PropertyMapping<Order, Long> mapping2 = PropertyMapping.<Order, Long>builder()
                .consumerId(STORE1_CONSUMER_ID)
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        PropertyMapping<Order, Long> mapping3 = PropertyMapping.<Order, Long>builder()
                .consumerId("store2")  // Different consumer ID
                .isForDashboard(false)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(OrderSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(Order_.customerId))
                .datasourceName(CUSTOMERS_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourcePath(Collections.singletonList(Customer_.addressId))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        // Test equals
        assertEquals(mapping1, mapping2, "Identical mappings should be equal");
        assertNotEquals(mapping1, mapping3, "Different mappings should not be equal");

        // Test hashCode
        assertEquals(mapping1.hashCode(), mapping2.hashCode(), "Equal mappings should have same hashCode");
    }
}

