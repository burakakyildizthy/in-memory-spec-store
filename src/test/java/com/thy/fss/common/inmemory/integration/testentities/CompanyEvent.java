package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Test entity for complex nested hierarchy integration testing.
 * Represents an event within a company.
 */
@MetaModel
public class CompanyEvent implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private Long companyId;
    private String eventType;
    private String description;
    private LocalDateTime eventDate;
    private Double impact; // Financial impact

    // Constructors
    public CompanyEvent() {
    }

    public CompanyEvent(Long id, Long companyId, String eventType, String description, Double impact) {
        this.id = id;
        this.companyId = companyId;
        this.eventType = eventType;
        this.description = description;
        this.impact = impact;
        this.eventDate = LocalDateTime.now().minusDays(id);
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Double getImpact() {
        return impact;
    }

    public void setImpact(Double impact) {
        this.impact = impact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanyEvent that = (CompanyEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CompanyEvent{id=" + id + ", companyId=" + companyId + ", eventType='" + eventType +
                "', description='" + description + "', impact=" + impact + "}";
    }
}