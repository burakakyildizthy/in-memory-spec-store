package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive test coverage for engine mapping components.
 * Tests property mapping, foreign key grouping, and mapping type validation.
 */
class MappingComponentsTest {

    private static final String STORE1 = "store1";
    private static final String DASHBOARD1 = "dashboard1";
    private static final String USER_DATASOURCE = "userDataSource";
    private static final String ORDER_DATASOURCE = "orderDataSource";
    private static final String MANY_TO_ONE_AGGREGATION_DATASOURCE = "MANY_TO_ONE_AGGREGATION";
    private static final String ACTIVE = "ACTIVE";

    // ========== MappingType Tests ==========

    @Test
    void testMappingTypeAllValues() {
        MappingType[] types = MappingType.values();

        assertThat(types).hasSize(7).containsExactlyInAnyOrder(
                MappingType.ONE_TO_ONE,
                MappingType.MANY_TO_ONE_COLLECTION,
                MappingType.MANY_TO_ONE_AGGREGATION,
                MappingType.MANY_TO_ONE_ALL,
                MappingType.MANY_TO_ONE_ANY,
                MappingType.MANY_TO_ONE_FIRST,
                MappingType.MANY_TO_ONE_LAST
        );
    }

    @Test
    void testMappingTypeValueOf() {
        assertThat(MappingType.valueOf("ONE_TO_ONE")).isEqualTo(MappingType.ONE_TO_ONE);
        assertThat(MappingType.valueOf("MANY_TO_ONE_COLLECTION")).isEqualTo(MappingType.MANY_TO_ONE_COLLECTION);
        assertThat(MappingType.valueOf(MANY_TO_ONE_AGGREGATION_DATASOURCE)).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
    }

    // ========== PropertyMapping Builder Tests ==========

    @Test
    void testPropertyMappingBuilderStoreMappingOneToOne() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(USER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.status))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.ONE_TO_ONE)
                .build();

        assertThat(mapping.getConsumerId()).isEqualTo(STORE1);
        assertThat(mapping.isForDashboard()).isFalse();
        assertThat(mapping.getTargetPath().get(mapping.getTargetPath().size() - 1)).isEqualTo(User_.name);
        assertThat(mapping.getDataSourceName()).isEqualTo(USER_DATASOURCE);
        assertThat(mapping.getSourcePath().get(mapping.getSourcePath().size() - 1)).isEqualTo(Order_.status);
        assertThat(mapping.getPrimaryKeyPaths().get(0).get(mapping.getPrimaryKeyPaths().get(0).size() - 1)).isEqualTo(User_.id);
        assertThat(mapping.getForeignKeyPaths().get(0).get(mapping.getForeignKeyPaths().get(0).size() - 1)).isEqualTo(Order_.status);
        assertThat(mapping.getMappingType()).isEqualTo(MappingType.ONE_TO_ONE);
        assertThat(mapping.getAggregationType()).isNull();
    }

    @Test
    void testPropertyMappingBuilderStoreMappingManyToOneCollection() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                .build();

        assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_COLLECTION);
        assertThat(mapping.requiresGrouping()).isTrue();
    }

    @Test
    void testPropertyMappingBuilderStoreMappingManyToOneAggregation() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
        assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.SUM);
        assertThat(mapping.requiresGrouping()).isTrue();
    }

    @Test
    void testPropertyMappingBuilderDashboardMapping() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertThat(mapping.isForDashboard()).isTrue();
        assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
        assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.SUM);
        assertThat(mapping.requiresGrouping()).isFalse();
    }

    @Test
    void testPropertyMappingBuilderDashboardMappingCountAggregation() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        assertThat(mapping.getSourcePath()).isNull();
        assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.COUNT);
    }

    @Test
    void testPropertyMappingBuilderWithSpecification() {
        Specification<Order> spec = SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.status).contains(ACTIVE);

        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .specification(spec)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertThat(mapping.getSpecification()).isEqualTo(spec);
    }

    // ========== PropertyMapping Validation Tests ==========

    @Test
    void testPropertyMappingValidationNullConsumerId() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .isForDashboard(true)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Consumer ID cannot be null");
    }

    @Test
    void testPropertyMappingValidationNullTargetPath() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Target path cannot be null or empty");
    }

    @Test
    void testPropertyMappingValidationNullDataSourceName() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Datasource name cannot be null or empty");
    }

    @Test
    void testPropertyMappingValidationNullMappingType() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .aggregationType(AggregationType.COUNT);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Mapping type cannot be null");
    }

    @Test
    void testPropertyMappingValidationDashboardMustBeAggregation() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.ONE_TO_ONE);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dashboard mappings must be MANY_TO_ONE_AGGREGATION type");
    }

    @Test
    void testPropertyMappingValidationDashboardMustHaveAggregationType() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dashboard mappings must have an aggregation type");
    }

    @Test
    void testPropertyMappingValidationDashboardNonCountMustHaveSourceAttribute() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dashboard mappings must have a source attribute for SUM aggregation");
    }

    @Test
    void testPropertyMappingValidationDashboardCannotHavePrimaryForeignKeys() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dashboard mappings cannot have primary/foreign key fields");
    }

    @Test
    void testPropertyMappingValidationStoreMustHavePrimaryForeignKeys() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Store mappings with relationships must have primary and foreign key fields");
    }

    @Test
    void testPropertyMappingValidationAggregationMustHaveAggregationType() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MANY_TO_ONE_AGGREGATION mappings must have an aggregation type");
    }

    @Test
    void testPropertyMappingValidationAggregationNonCountMustHaveSourceAttribute() {
        PropertyMapping.Builder<User, String> builder = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.AVG);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MANY_TO_ONE_AGGREGATION mappings (except COUNT) must have a source attribute");
    }

    // ========== PropertyMapping Grouping Tests ==========

    @Test
    void testPropertyMappingRequiresGroupingStore() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                .build();

        assertThat(mapping.requiresGrouping()).isTrue();
    }

    @Test
    void testPropertyMappingRequiresGroupingDashboard() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        assertThat(mapping.requiresGrouping()).isFalse();
    }

    // ========== PropertyMapping Equality Tests ==========

    @Test
    void testPropertyMappingEquality() {
        PropertyMapping<User, String> mapping1 = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        PropertyMapping<User, String> mapping2 = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        PropertyMapping<User, String> mapping3 = PropertyMapping.<User, String>builder()
                .consumerId("dashboard2")
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        assertAll(
                () -> assertThat(mapping1).isEqualTo(mapping2),
                () -> assertThat(mapping1).hasSameHashCodeAs(mapping2),
                () -> assertThat(mapping1).isNotEqualTo(mapping3)
        );
    }

    // ========== PropertyMapping ToString Tests ==========

    @Test
    void testPropertyMappingToString() {
        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(DASHBOARD1)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        String toString = mapping.toString();

        assertThat(toString).contains("PropertyMapping")
                .contains(DASHBOARD1)
                .contains(ORDER_DATASOURCE)
                .contains(MANY_TO_ONE_AGGREGATION_DATASOURCE)
                .contains("SUM");
    }

    // ========== PropertyMapping Getters Tests ==========

    @Test
    void testPropertyMappingAllGetters() {
        Specification<Order> spec = SpecificationBuilder.forService(OrderSpecificationService.INSTANCE)
                .where(Order_.status).contains(ACTIVE);

        PropertyMapping<User, String> mapping = PropertyMapping.<User, String>builder()
                .consumerId(STORE1)
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.status))
                // Fix: Ensure both primary and foreign keys have the same type
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status))) // Change to userId if it matches User_.id type
                .specification(spec)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        assertThat(mapping.getConsumerId()).isEqualTo(STORE1);
        assertThat(mapping.isForDashboard()).isFalse();
        assertThat(mapping.getTargetPath().get(mapping.getTargetPath().size() - 1)).isEqualTo(User_.name);
        assertThat(mapping.getDataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(mapping.getSourcePath().get(mapping.getSourcePath().size() - 1)).isEqualTo(Order_.status);
        assertThat(mapping.getPrimaryKeyPaths().get(0).get(mapping.getPrimaryKeyPaths().get(0).size() - 1)).isEqualTo(User_.id);
        assertThat(mapping.getForeignKeyPaths().get(0).get(mapping.getForeignKeyPaths().get(0).size() - 1)).isEqualTo(Order_.status); // Update assertion
        assertThat(mapping.getSpecification()).isEqualTo(spec);
        assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
        assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.COUNT);
    }
}

