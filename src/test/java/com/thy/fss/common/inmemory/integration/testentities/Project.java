package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test entity for complex nested hierarchy integration testing.
 * Represents a project within a department.
 */
@MetaModel
public class Project implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private Long departmentId;
    private String name;
    private String description;
    private Double budget;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Level 4: Project team members (simplified for testing)
    private List<String> teamMemberNames;

    // Level 4: Project tags (simplified for testing)
    private List<String> tagNames;

    // Constructors
    public Project() {
        this.teamMemberNames = new ArrayList<>();
        this.tagNames = new ArrayList<>();
    }

    public Project(Long id, Long departmentId, String name, String description,
                   Double budget, String status) {
        this();
        this.id = id;
        this.departmentId = departmentId;
        this.name = name;
        this.description = description;
        this.budget = budget;
        this.status = status;
        this.startDate = LocalDateTime.now().minusMonths(id);
        this.endDate = startDate.plusMonths(6);
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public List<String> getTeamMemberNames() {
        return teamMemberNames;
    }

    public void setTeamMemberNames(List<String> teamMemberNames) {
        this.teamMemberNames = teamMemberNames;
    }

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", departmentId=" + departmentId + ", name='" + name +
                "', status='" + status + "', budget=" + budget + "}";
    }
}