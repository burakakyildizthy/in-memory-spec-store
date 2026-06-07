package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;

/**
 * Test entity with complex Jackson annotation combinations
 * for comprehensive integration testing.
 */
@MetaModel
public class ComplexAnnotatedEntity implements com.thy.fss.common.inmemory.entity.Identifiable<String> {

    private String id;

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("user_status")
    private UserStatus status;

    @JsonProperty("priority_level")
    private Priority priority;

    // Enum without custom property name
    private Status simpleStatus;

    @JsonProperty("created_timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @JsonProperty("updated_timestamp")
    private LocalDateTime updatedAt; // No custom format

    private String description; // No annotations

    private Integer version; // No annotations

    @Override
    public String getIdentity() {
        return id;
    }

    public String getId() {
        return getIdentity();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getSimpleStatus() {
        return simpleStatus;
    }

    public void setSimpleStatus(Status simpleStatus) {
        this.simpleStatus = simpleStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}