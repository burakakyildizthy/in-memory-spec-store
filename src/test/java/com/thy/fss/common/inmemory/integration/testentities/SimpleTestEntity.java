package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;

/**
 * Simple test entity for integration testing.
 */
@MetaModel
public class SimpleTestEntity implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String id;
    private String name;
    private Integer value;
    private LocalDateTime createdAt;

    public SimpleTestEntity() {
    }

    public SimpleTestEntity(String id, String name, Integer value) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getIdentity() {
        return id;
    }

    public String getId() {
        return id;
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

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}