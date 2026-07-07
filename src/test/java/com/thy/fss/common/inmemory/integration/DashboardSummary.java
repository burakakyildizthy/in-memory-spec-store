package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Dashboard summary class for integration tests.
 * Used to test dashboard functionality with calculated fields.
 */
@MetaModel
public class DashboardSummary {
    private Integer totalUsers;
    private Integer activeUsers;
    private Double averageAge;
    private Integer maxAge;
    private Integer minAge;
    private Integer totalAge;
    private Integer filteredCount;
    private Double avgAgeFiltered;
    private Integer userCount;
    private Integer totalCount;
    private Integer count;
    private Integer sum;

    public DashboardSummary() {
        // Default constructor
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Integer activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Double getAverageAge() {
        return averageAge;
    }

    public void setAverageAge(Double averageAge) {
        this.averageAge = averageAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getTotalAge() {
        return totalAge;
    }

    public void setTotalAge(Integer totalAge) {
        this.totalAge = totalAge;
    }

    public Integer getFilteredCount() {
        return filteredCount;
    }

    public void setFilteredCount(Integer filteredCount) {
        this.filteredCount = filteredCount;
    }

    public Double getAvgAgeFiltered() {
        return avgAgeFiltered;
    }

    public void setAvgAgeFiltered(Double avgAgeFiltered) {
        this.avgAgeFiltered = avgAgeFiltered;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getSum() {
        return sum;
    }

    public void setSum(Integer sum) {
        this.sum = sum;
    }

    @Override
    public String toString() {
        return "DashboardSummary{" +
                "totalUsers=" + totalUsers +
                ", activeUsers=" + activeUsers +
                ", averageAge=" + averageAge +
                ", maxAge=" + maxAge +
                ", minAge=" + minAge +
                ", totalAge=" + totalAge +
                ", filteredCount=" + filteredCount +
                ", avgAgeFiltered=" + avgAgeFiltered +
                ", userCount=" + userCount +
                ", totalCount=" + totalCount +
                ", count=" + count +
                ", sum=" + sum +
                '}';
    }
}