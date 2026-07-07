package com.thy.fss.common.inmemory.engine.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the aggregation plan for a single dashboard.
 * Contains all aggregation tasks that need to be executed for this dashboard.
 *
 * <p>Each task represents a unique combination of datasource, specification, and field,
 * and may contain multiple aggregation types to be computed in a single loop.</p>
 */
public class DashboardAggregationPlan {

    private final String dashboardId;
    private final List<AggregationTask> tasks;

    /**
     * Creates a new DashboardAggregationPlan.
     *
     * @param dashboardId the dashboard ID
     */
    public DashboardAggregationPlan(String dashboardId) {
        this.dashboardId = dashboardId;
        this.tasks = new ArrayList<>();
    }

    /**
     * Adds an aggregation task to this plan.
     *
     * @param task the aggregation task
     */
    public void addTask(AggregationTask task) {
        this.tasks.add(task);
    }

    /**
     * Gets the dashboard ID.
     *
     * @return the dashboard ID
     */
    public String getDashboardId() {
        return dashboardId;
    }

    /**
     * Gets all aggregation tasks.
     *
     * @return unmodifiable list of tasks
     */
    public List<AggregationTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Checks if this plan has any tasks.
     *
     * @return true if there are tasks
     */
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    /**
     * Gets the number of tasks in this plan.
     *
     * @return the task count
     */
    public int getTaskCount() {
        return tasks.size();
    }

    @Override
    public String toString() {
        return String.format("DashboardAggregationPlan[dashboard=%s, tasks=%d]",
                dashboardId, tasks.size());
    }
}
