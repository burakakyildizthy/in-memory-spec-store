package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;

import java.util.*;

/**
 * Contains the results of the analysis phase.
 * This includes common groupings for Stores, dashboard aggregation plans,
 * and the set of all source datasources needed.
 *
 * <p>This result is used by subsequent phases to efficiently process data.</p>
 *
 * @param commonGroupings           Store common groupings: GroupingKey → List of PropertyMappings
 * @param dashboardAggregationPlans Dashboard aggregation plans: dashboardId → DashboardAggregationPlan
 * @param sourceDatasources         All source datasources needed (for reading)
 */
public record AnalysisResult(Map<GroupingKey, List<PropertyMapping<?, ?>>> commonGroupings,
                             Map<String, DashboardAggregationPlan> dashboardAggregationPlans,
                             Set<String> sourceDatasources) {

    /**
     * Creates a new AnalysisResult.
     *
     * @param commonGroupings           the common groupings for Stores
     * @param dashboardAggregationPlans the dashboard aggregation plans
     * @param sourceDatasources         the source datasources
     */
    public AnalysisResult(
            Map<GroupingKey, List<PropertyMapping<?, ?>>> commonGroupings,
            Map<String, DashboardAggregationPlan> dashboardAggregationPlans,
            Set<String> sourceDatasources) {
        this.commonGroupings = commonGroupings != null ? commonGroupings : new HashMap<>();
        this.dashboardAggregationPlans = dashboardAggregationPlans != null ? dashboardAggregationPlans : new HashMap<>();
        this.sourceDatasources = sourceDatasources != null ? sourceDatasources : new HashSet<>();
    }

    /**
     * Gets the common groupings for Stores.
     *
     * @return map of grouping key to property mappings
     */
    @Override
    public Map<GroupingKey, List<PropertyMapping<?, ?>>> commonGroupings() {
        return Collections.unmodifiableMap(commonGroupings);
    }

    /**
     * Gets the dashboard aggregation plans.
     *
     * @return map of dashboard ID to aggregation plan
     */
    @Override
    public Map<String, DashboardAggregationPlan> dashboardAggregationPlans() {
        return Collections.unmodifiableMap(dashboardAggregationPlans);
    }

    /**
     * Gets all source datasources.
     *
     * @return set of datasource names
     */
    @Override
    public Set<String> sourceDatasources() {
        return Collections.unmodifiableSet(sourceDatasources);
    }

    /**
     * Gets the aggregation plan for a specific dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return the aggregation plan, or null if not found
     */
    public DashboardAggregationPlan getDashboardPlan(String dashboardId) {
        return dashboardAggregationPlans.get(dashboardId);
    }

    /**
     * Checks if there are any common groupings.
     *
     * @return true if there are common groupings
     */
    public boolean hasCommonGroupings() {
        return !commonGroupings.isEmpty();
    }

    /**
     * Checks if there are any dashboard aggregation plans.
     *
     * @return true if there are dashboard plans
     */
    public boolean hasDashboardPlans() {
        return !dashboardAggregationPlans.isEmpty();
    }

    /**
     * Checks if there are any source datasources.
     *
     * @return true if there are source datasources
     */
    public boolean hasSourceDatasources() {
        return !sourceDatasources.isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
                "AnalysisResult[groupings=%d, dashboardPlans=%d, datasources=%d]",
                commonGroupings.size(),
                dashboardAggregationPlans.size(),
                sourceDatasources.size()
        );
    }
}
