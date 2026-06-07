package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Dashboard target class for testing.
 * Used to test meta attribute-based dashboard functionality.
 */
@MetaModel
public class UserSummary {
    private Long totalUsers;
    private Double averageAge;
    private Long activeUsers;
    private Long inactiveUsers;
    private Double totalSalary;
    private Long activeCount;
    private Integer maxAge;
    private Integer minAge;
    private Long youngUsers;
    private Long seniorUsers;

    /**
     * Default constructor. Required for frameworks that instantiate objects via reflection.
     */
    public UserSummary() {
        // Default constructor
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Double getAverageAge() {
        return averageAge;
    }

    public void setAverageAge(Double averageAge) {
        this.averageAge = averageAge;
    }

    public Long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Double getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(Double totalSalary) {
        this.totalSalary = totalSalary;
    }

    public Long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Long activeCount) {
        this.activeCount = activeCount;
    }

    public Long getInactiveUsers() {
        return inactiveUsers;
    }

    public void setInactiveUsers(Long inactiveUsers) {
        this.inactiveUsers = inactiveUsers;
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

    public Long getYoungUsers() {
        return youngUsers;
    }

    public void setYoungUsers(Long youngUsers) {
        this.youngUsers = youngUsers;
    }

    public Long getSeniorUsers() {
        return seniorUsers;
    }

    public void setSeniorUsers(Long seniorUsers) {
        this.seniorUsers = seniorUsers;
    }

    @Override
    public String toString() {
        return "UserSummary{" +
                "totalUsers=" + totalUsers +
                ", averageAge=" + averageAge +
                ", activeUsers=" + activeUsers +
                ", inactiveUsers=" + inactiveUsers +
                ", totalSalary=" + totalSalary +
                ", activeCount=" + activeCount +
                ", maxAge=" + maxAge +
                ", minAge=" + minAge +
                ", youngUsers=" + youngUsers +
                ", seniorUsers=" + seniorUsers +
                '}';
    }
}