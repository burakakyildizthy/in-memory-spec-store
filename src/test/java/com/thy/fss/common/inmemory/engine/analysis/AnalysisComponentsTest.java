package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test coverage for engine analysis components.
 * Tests dependency analysis, aggregation planning, and grouping key structures.
 */
class AnalysisComponentsTest {
    

    private static final String ORDER_DATASOURCE = "orderDataSource";
    private static final String DASHBOARD_1 = "dashboard1";
    private static final String DASHBOARD_2 = "dashboard2";
    private static final String DATASOURCE_NAME_NULL_MESSAGE = "Datasource name cannot be null";
    private static final String DS_1= "ds1";
    private static final String DS_2= "ds2";
    private static final String DS_3= "ds3";

    // ========== GroupingKey Tests ==========

    @Test
    void testGroupingKeyCreation() {
        GroupingKey key = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));

        assertThat(key.dataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(key.primaryKeyPath().get(key.primaryKeyPath().size() - 1)).isEqualTo(User_.id);
        assertThat(key.foreignKeyPath().get(key.foreignKeyPath().size() - 1)).isEqualTo(Order_.customerId);
    }

    @Test
    void testGroupingKeyToStorageKey() {
        GroupingKey key = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));

        String storageKey = key.toString();

        assertThat(storageKey).isEqualTo("GroupingKey[orderDataSource:id:customerId]");
    }

    @Test
    void testGroupingKeyNullValidation() {
        List<MetaAttribute<?, ?>> userIdPath = Collections.singletonList(User_.id);
        List<MetaAttribute<?, ?>> customerIdPath = Collections.singletonList(Order_.customerId);

        assertThatThrownBy(() -> new GroupingKey(null, userIdPath, customerIdPath))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(DATASOURCE_NAME_NULL_MESSAGE);

        assertThatThrownBy(() -> new GroupingKey(ORDER_DATASOURCE, null, customerIdPath))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Primary key path cannot be null");

        assertThatThrownBy(() -> new GroupingKey(ORDER_DATASOURCE, userIdPath, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Foreign key path cannot be null");

    }

    @Test
    void testGroupingKeyEquality() {
        GroupingKey key1 = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));
        GroupingKey key2 = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));
        GroupingKey key3 = new GroupingKey("differentDataSource", Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));

        assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2).isNotEqualTo(key3);
    }

    @Test
    void testGroupingKeyToString() {
        GroupingKey key = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));

        String toString = key.toString();

        assertThat(toString).contains("GroupingKey").contains("orderDataSource:id:customerId");
    }

    // ========== AggregationGroupKey Tests ==========

    @Test
    void testAggregationGroupKeyCreation() {
        Specification<Order> spec = createOrderSpecification();
        AggregationGroupKey key = new AggregationGroupKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount));

        assertThat(key.dataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(key.specification()).isEqualTo(spec);
        assertThat(key.fieldPath().get(key.fieldPath().size() - 1)).isEqualTo(Order_.totalAmount);
    }

    @Test
    void testAggregationGroupKeyWithNullSpecification() {
        AggregationGroupKey key = new AggregationGroupKey(ORDER_DATASOURCE, null, Collections.singletonList(Order_.totalAmount));

        assertThat(key.dataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(key.specification()).isNull();
        assertThat(key.fieldPath().get(key.fieldPath().size() - 1)).isEqualTo(Order_.totalAmount);
    }

    @Test
    void testAggregationGroupKeyEquality() {
        Specification<Order> spec = createOrderSpecification();
        AggregationGroupKey key1 = new AggregationGroupKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount));
        AggregationGroupKey key2 = new AggregationGroupKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount));
        AggregationGroupKey key3 = new AggregationGroupKey(ORDER_DATASOURCE, null, Collections.singletonList(Order_.totalAmount));

        assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2).isNotEqualTo(key3);
    }

    // ========== CommonAggregationKey Tests ==========

    @Test
    void testCommonAggregationKeyCreation() {
        Specification<Order> spec = createOrderSpecification();
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount), AggregationType.SUM);

        assertThat(key.dataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(key.specification()).isEqualTo(spec);
        assertThat(key.fieldPath().get(key.fieldPath().size() - 1)).isEqualTo(Order_.totalAmount);
        assertThat(key.aggregationType()).isEqualTo(AggregationType.SUM);
    }

    @Test
    void testCommonAggregationKeyWithNullSpecification() {
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, null, Collections.singletonList(Order_.totalAmount), AggregationType.AVG);

        assertThat(key.specification()).isNull();
        assertThat(key.fieldPath().get(key.fieldPath().size() - 1)).isEqualTo(Order_.totalAmount);
    }

    @Test
    void testCommonAggregationKeyWithNullFieldForCountAggregation() {
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, null, null, AggregationType.COUNT);

        assertThat(key.fieldPath()).isEmpty();
        assertThat(key.aggregationType()).isEqualTo(AggregationType.COUNT);
    }

    @Test
    void testCommonAggregationKeyToStorageKey() {
        Specification<Order> spec = createOrderSpecification();
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount), AggregationType.SUM);

        String storageKey = key.toStorageKey();

        assertThat(storageKey).startsWith("orderDataSource:").endsWith("SUM");
    }

    @Test
    void testCommonAggregationKeyToStorageKeyWithNullSpecAndField() {
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, null, null, AggregationType.COUNT);

        String storageKey = key.toStorageKey();

        assertThat(storageKey).isEqualTo("orderDataSource:null::COUNT");
    }

    @Test
    void testCommonAggregationKeyNullValidation() {
        List<MetaAttribute<?, ?>> aggregationFields = Collections.singletonList(Order_.totalAmount);

        assertThatThrownBy(() -> new CommonAggregationKey(null, null, aggregationFields, AggregationType.SUM))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(DATASOURCE_NAME_NULL_MESSAGE);

        assertThatThrownBy(() -> new CommonAggregationKey(ORDER_DATASOURCE, null, aggregationFields, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Aggregation type cannot be null");

    }

    @Test
    void testCommonAggregationKeyEquality() {
        Specification<Order> spec = createOrderSpecification();
        CommonAggregationKey key1 = new CommonAggregationKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount), AggregationType.SUM);
        CommonAggregationKey key2 = new CommonAggregationKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount), AggregationType.SUM);
        CommonAggregationKey key3 = new CommonAggregationKey(ORDER_DATASOURCE, spec, Collections.singletonList(Order_.totalAmount), AggregationType.AVG);

        assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2).isNotEqualTo(key3);
    }

    @Test
    void testCommonAggregationKeyToString() {
        CommonAggregationKey key = new CommonAggregationKey(
                ORDER_DATASOURCE, null, Collections.singletonList(Order_.totalAmount), AggregationType.SUM);

        String toString = key.toString();

        assertThat(toString).contains("CommonAggregationKey").contains(ORDER_DATASOURCE).contains("SUM");
    }

    // ========== AggregationTask Tests ==========

    @Test
    void testAggregationTaskCreation() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE, Collections.singletonList(Order_.totalAmount));

        assertThat(task.getDataSourceName()).isEqualTo(ORDER_DATASOURCE);
        assertThat(task.getFieldPath().get(task.getFieldPath().size() - 1)).isEqualTo(Order_.totalAmount);
        assertThat(task.hasMappings()).isFalse();
    }


    @Test
    void testAggregationTaskWithNullFieldForCountAggregation() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  null);

        assertThat(task.getFieldPath()).isEmpty();
    }

    @Test
    void testAggregationTaskAddMapping() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        PropertyMapping<User, String> mapping = createTestMapping(DASHBOARD_1, AggregationType.SUM);

        task.addMapping(AggregationType.SUM, mapping);

        assertThat(task.hasMappings()).isTrue();
        assertThat(task.getAggregationTypes()).containsExactly(AggregationType.SUM);
        assertThat(task.getMappings(AggregationType.SUM)).containsExactly(mapping);
    }

    @Test
    void testAggregationTaskAddMultipleMappingsSameAggregationType() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        PropertyMapping<User, String> mapping1 = createTestMapping(DASHBOARD_1, AggregationType.SUM);
        PropertyMapping<User, String> mapping2 = createTestMapping(DASHBOARD_2, AggregationType.SUM);

        task.addMapping(AggregationType.SUM, mapping1);
        task.addMapping(AggregationType.SUM, mapping2);

        assertThat(task.getMappings(AggregationType.SUM)).containsExactly(mapping1, mapping2);
        assertThat(task.getAggregationTypeCount()).isEqualTo(1);
    }

    @Test
    void testAggregationTaskAddMultipleMappingsDifferentAggregationTypes() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE, Collections.singletonList(Order_.totalAmount));
        PropertyMapping<User, String> sumMapping = createTestMapping(DASHBOARD_1, AggregationType.SUM);
        PropertyMapping<User, String> avgMapping = createTestMapping(DASHBOARD_2, AggregationType.AVG);
        PropertyMapping<User, String> maxMapping = createTestMapping("dashboard3", AggregationType.MAX);

        task.addMapping(AggregationType.SUM, sumMapping);
        task.addMapping(AggregationType.AVG, avgMapping);
        task.addMapping(AggregationType.MAX, maxMapping);

        assertThat(task.getAggregationTypes()).containsExactlyInAnyOrder(
                AggregationType.SUM, AggregationType.AVG, AggregationType.MAX);
        assertThat(task.getAggregationTypeCount()).isEqualTo(3);
        assertThat(task.getMappings(AggregationType.SUM)).containsExactly(sumMapping);
        assertThat(task.getMappings(AggregationType.AVG)).containsExactly(avgMapping);
        assertThat(task.getMappings(AggregationType.MAX)).containsExactly(maxMapping);
    }

    @Test
    void testAggregationTaskGetAllMappings() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        PropertyMapping<User, String> sumMapping = createTestMapping(DASHBOARD_1, AggregationType.SUM);
        PropertyMapping<User, String> avgMapping = createTestMapping(DASHBOARD_2, AggregationType.AVG);

        task.addMapping(AggregationType.SUM, sumMapping);
        task.addMapping(AggregationType.AVG, avgMapping);

        List<PropertyMapping<?, ?>> allMappings = task.getAllMappings();

        assertThat(allMappings).hasSize(2).containsExactlyInAnyOrder(sumMapping, avgMapping);
    }

    @Test
    void testAggregationTaskGetMappingsByAggregationType() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        PropertyMapping<User, String> sumMapping = createTestMapping(DASHBOARD_1, AggregationType.SUM);

        task.addMapping(AggregationType.SUM, sumMapping);

        Map<AggregationType, List<PropertyMapping<?, ?>>> mappingsByType = task.getMappingsByAggregationType();

        assertThat(mappingsByType).containsOnlyKeys(AggregationType.SUM);
        assertThat(mappingsByType.get(AggregationType.SUM)).containsExactly(sumMapping);
    }

    @Test
    void testAggregationTaskGetMappingsNonExistentType() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE, Collections.singletonList(Order_.totalAmount));

        List<PropertyMapping<?, ?>> mappings = task.getMappings(AggregationType.COUNT);

        assertThat(mappings).isEmpty();
    }

    @Test
    void testAggregationTaskNullValidation() {
        List<MetaAttribute<?, ?>> aggregationFields = Collections.singletonList(Order_.totalAmount);
        assertThatThrownBy(() -> {
            new AggregationTask(null, aggregationFields);
        })
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(DATASOURCE_NAME_NULL_MESSAGE);
    }

    @Test
    void testAggregationTaskToString() {
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        task.addMapping(AggregationType.SUM, createTestMapping(DASHBOARD_1, AggregationType.SUM));

        String toString = task.toString();

        assertThat(toString).contains("AggregationTask").contains(ORDER_DATASOURCE).contains("types=[SUM]");
    }

    // ========== DashboardAggregationPlan Tests ==========

    @Test
    void testDashboardAggregationPlanCreation() {
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);

        assertThat(plan.getDashboardId()).isEqualTo(DASHBOARD_1);
        assertThat(plan.hasTasks()).isFalse();
        assertThat(plan.getTaskCount()).isZero();
    }

    @Test
    void testDashboardAggregationPlanAddTask() {
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));

        plan.addTask(task);

        assertThat(plan.hasTasks()).isTrue();
        assertThat(plan.getTaskCount()).isEqualTo(1);
        assertThat(plan.getTasks()).containsExactly(task);
    }

    @Test
    void testDashboardAggregationPlanAddMultipleTasks() {
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        AggregationTask task1 = new AggregationTask(ORDER_DATASOURCE, Collections.singletonList(Order_.totalAmount));
        AggregationTask task2 = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.customerId));

        plan.addTask(task1);
        plan.addTask(task2);

        assertThat(plan.getTaskCount()).isEqualTo(2);
        assertThat(plan.getTasks()).containsExactly(task1, task2);
    }

    @Test
    void testDashboardAggregationPlanGetTasksUnmodifiable() {
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        AggregationTask task = new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount));
        plan.addTask(task);

        List<AggregationTask> tasks = plan.getTasks();

        AggregationTask newTask = new AggregationTask("other", Collections.singletonList(Order_.customerId));
        assertThatThrownBy(() -> tasks.add(newTask))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testDashboardAggregationPlanToString() {
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        plan.addTask(new AggregationTask(ORDER_DATASOURCE,  Collections.singletonList(Order_.totalAmount)));

        String toString = plan.toString();

        assertThat(toString).contains("DashboardAggregationPlan").contains(DASHBOARD_1).contains("tasks=1");
    }

    // ========== AnalysisResult Tests ==========

    @Test
    void testAnalysisResultCreation() {
        Map<GroupingKey, List<PropertyMapping<?, ?>>> groupings = new HashMap<>();
        Map<String, DashboardAggregationPlan> plans = new HashMap<>();
        Set<String> datasources = new HashSet<>(Arrays.asList(DS_1, DS_2));

        AnalysisResult result = new AnalysisResult(groupings, plans, datasources);

        assertThat(result.commonGroupings()).isEqualTo(groupings);
        assertThat(result.dashboardAggregationPlans()).isEqualTo(plans);
        assertThat(result.sourceDatasources()).isEqualTo(datasources);
    }

    @Test
    void testAnalysisResultWithNullParameters() {
        AnalysisResult result = new AnalysisResult(null, null, null);

        assertThat(result.commonGroupings()).isEmpty();
        assertThat(result.dashboardAggregationPlans()).isEmpty();
        assertThat(result.sourceDatasources()).isEmpty();
    }

    @Test
    void testAnalysisResultWithCommonGroupings() {
        Map<GroupingKey, List<PropertyMapping<?, ?>>> groupings = new HashMap<>();
        GroupingKey key = new GroupingKey(ORDER_DATASOURCE, Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));
        groupings.put(key, List.of(createTestMapping("store1", AggregationType.SUM)));

        AnalysisResult result = new AnalysisResult(groupings, new HashMap<>(), new HashSet<>());

        assertThat(result.hasCommonGroupings()).isTrue();
        assertThat(result.commonGroupings()).containsKey(key);
    }

    @Test
    void testAnalysisResultWithDashboardPlans() {
        Map<String, DashboardAggregationPlan> plans = new HashMap<>();
        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        plans.put(DASHBOARD_1, plan);

        AnalysisResult result = new AnalysisResult(new HashMap<>(), plans, new HashSet<>());

        assertThat(result.hasDashboardPlans()).isTrue();
        assertThat(result.getDashboardPlan(DASHBOARD_1)).isEqualTo(plan);
    }

    @Test
    void testAnalysisResultWithSourceDatasources() {
        Set<String> datasources = new HashSet<>(Arrays.asList(DS_1, DS_2, DS_3));

        AnalysisResult result = new AnalysisResult(new HashMap<>(), new HashMap<>(), datasources);

        assertThat(result.hasSourceDatasources()).isTrue();
        assertThat(result.sourceDatasources()).containsExactlyInAnyOrder(DS_1, DS_2, DS_3);
    }

    @Test
    void testAnalysisResultGetDashboardPlanNonExistent() {
        AnalysisResult result = new AnalysisResult(new HashMap<>(), new HashMap<>(), new HashSet<>());

        DashboardAggregationPlan plan = result.getDashboardPlan("nonExistent");

        assertThat(plan).isNull();
    }

    @Test
    void testAnalysisResultUnmodifiableCollections() {
        Map<GroupingKey, List<PropertyMapping<?, ?>>> groupings = new HashMap<>();
        Map<String, DashboardAggregationPlan> plans = new HashMap<>();
        Set<String> datasources = new HashSet<>();

        AnalysisResult result = new AnalysisResult(groupings, plans, datasources);

        GroupingKey groupingKey = new GroupingKey("ds", Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId));
        ArrayList<PropertyMapping<?, ?>> emptyList = new ArrayList<>();
        Map<GroupingKey, List<PropertyMapping<?, ?>>> commonGroupings = result.commonGroupings();
        assertThatThrownBy(() -> commonGroupings.put(groupingKey, emptyList))
                .isInstanceOf(UnsupportedOperationException.class);

        DashboardAggregationPlan plan = new DashboardAggregationPlan(DASHBOARD_1);
        Map<String, DashboardAggregationPlan> dashboardPlans = result.dashboardAggregationPlans();
        assertThatThrownBy(() -> dashboardPlans.put(DASHBOARD_1, plan))
                .isInstanceOf(UnsupportedOperationException.class);

        Set<String> sourceDatasources = result.sourceDatasources();
        assertThatThrownBy(() -> sourceDatasources.add("newDs"))
                .isInstanceOf(UnsupportedOperationException.class);


    }

    @Test
    void testAnalysisResultToString() {
        Map<GroupingKey, List<PropertyMapping<?, ?>>> groupings = new HashMap<>();
        groupings.put(new GroupingKey("ds", Collections.singletonList(User_.id), Collections.singletonList(Order_.customerId)), new ArrayList<>());
        Map<String, DashboardAggregationPlan> plans = new HashMap<>();
        plans.put(DASHBOARD_1, new DashboardAggregationPlan(DASHBOARD_1));
        Set<String> datasources = new HashSet<>(Arrays.asList(DS_1, DS_2));

        AnalysisResult result = new AnalysisResult(groupings, plans, datasources);

        String toString = result.toString();

        assertThat(toString).contains("AnalysisResult").contains("groupings=1").contains("dashboardPlans=1").contains("datasources=2");
    }

    // ========== Helper Methods ==========

    private Specification<Order> createOrderSpecification() {
        return com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder
                .forService(OrderSpecificationService.INSTANCE)
                .where(Order_.status).contains("ACTIVE");
    }

    private PropertyMapping<User, String> createTestMapping(String consumerId, AggregationType aggregationType) {
        return PropertyMapping.<User, String>builder()
                .consumerId(consumerId)
                .isForDashboard(true)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.id))
                .datasourceName(ORDER_DATASOURCE)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(aggregationType)
                .build();
    }
}
