package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test entity for complex nested hierarchy integration testing.
 * Represents a company with departments and events.
 */
@MetaModel
public class Company implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private String name;
    private String industry;
    private LocalDateTime foundedDate;
    private String status;

    // Level 2: Departments
    private List<Department> departments;

    // Level 2: Company metrics (aggregated)
    private Integer totalEmployees;
    private Double totalRevenue;
    private Double averageSalary;

    // Level 2: Company events
    private List<CompanyEvent> events;

    // Constructors
    public Company() {
        this.departments = new ArrayList<>();
        this.events = new ArrayList<>();
    }

    public Company(Long id, String name, String industry, String status) {
        this();
        this.id = id;
        this.name = name;
        this.industry = industry;
        this.status = status;
        this.foundedDate = LocalDateTime.now().minusYears(id);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public LocalDateTime getFoundedDate() {
        return foundedDate;
    }

    public void setFoundedDate(LocalDateTime foundedDate) {
        this.foundedDate = foundedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public Integer getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(Integer totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getAverageSalary() {
        return averageSalary;
    }

    public void setAverageSalary(Double averageSalary) {
        this.averageSalary = averageSalary;
    }

    public List<CompanyEvent> getEvents() {
        return events;
    }

    public void setEvents(List<CompanyEvent> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(id, company.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Company{id=" + id + ", name='" + name + "', industry='" + industry +
                "', status='" + status + "', departments=" + (departments != null ? departments.size() : 0) + "}";
    }
}