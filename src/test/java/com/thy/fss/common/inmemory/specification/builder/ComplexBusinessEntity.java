package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Complex business entity for testing extremely complex specifications.
 * Contains all possible data types and nested relationships.
 */
@MetaModel
public class ComplexBusinessEntity {
    private Long id;
    private String companyName;
    private BusinessType businessType;
    private Integer employeeCount;
    private Double revenue;
    private LocalDate foundedDate;
    private LocalDateTime lastAuditDate;
    private boolean active;
    private Double complianceScore;
    private Address headquarters;
    private ExecutiveProfile ceo;
    private List<String> departments;
    private List<String> certifications;
    private List<ExecutiveProfile> boardMembers;
    private List<Address> offices;

    // Constructors
    public ComplexBusinessEntity() {
        // Default constructor. Fields can be set via setters.
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    public LocalDate getFoundedDate() {
        return foundedDate;
    }

    public void setFoundedDate(LocalDate foundedDate) {
        this.foundedDate = foundedDate;
    }

    public LocalDateTime getLastAuditDate() {
        return lastAuditDate;
    }

    public void setLastAuditDate(LocalDateTime lastAuditDate) {
        this.lastAuditDate = lastAuditDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public Address getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(Address headquarters) {
        this.headquarters = headquarters;
    }

    public ExecutiveProfile getCeo() {
        return ceo;
    }

    public void setCeo(ExecutiveProfile ceo) {
        this.ceo = ceo;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public void setDepartments(List<String> departments) {
        this.departments = departments;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public List<ExecutiveProfile> getBoardMembers() {
        return boardMembers;
    }

    public void setBoardMembers(List<ExecutiveProfile> boardMembers) {
        this.boardMembers = boardMembers;
    }

    public List<Address> getOffices() {
        return offices;
    }

    public void setOffices(List<Address> offices) {
        this.offices = offices;
    }
}