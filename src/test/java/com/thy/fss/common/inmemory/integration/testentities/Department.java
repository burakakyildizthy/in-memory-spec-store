package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test entity for complex nested hierarchy integration testing.
 * Represents a department within a company.
 */
@MetaModel
public class Department implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private Long companyId;
    private String name;
    private String type;
    private Double budget;
    private String status;

    // Level 3: Employees (simplified for testing)
    private List<String> employeeNames;

    // Level 3: Department metrics
    private Integer employeeCount;
    private Double totalSalaries;
    private Double averagePerformanceScore;

    // Level 3: Department projects
    private List<Project> projects;

    // Constructors
    public Department() {
        this.employeeNames = new ArrayList<>();
        this.projects = new ArrayList<>();
    }

    public Department(Long id, Long companyId, String name, String type, Double budget, String status) {
        this();
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.type = type;
        this.budget = budget;
        this.status = status;
    }

    // Getters and setters
    public Long getIdentity() {
        return id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getEmployeeNames() {
        return employeeNames;
    }

    public void setEmployeeNames(List<String> employeeNames) {
        this.employeeNames = employeeNames;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Double getTotalSalaries() {
        return totalSalaries;
    }

    public void setTotalSalaries(Double totalSalaries) {
        this.totalSalaries = totalSalaries;
    }

    public Double getAveragePerformanceScore() {
        return averagePerformanceScore;
    }

    public void setAveragePerformanceScore(Double averagePerformanceScore) {
        this.averagePerformanceScore = averagePerformanceScore;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Department{id=" + id + ", companyId=" + companyId + ", name='" + name +
                "', type='" + type + "', employees=" + (employeeNames != null ? employeeNames.size() : 0) + "}";
    }
}