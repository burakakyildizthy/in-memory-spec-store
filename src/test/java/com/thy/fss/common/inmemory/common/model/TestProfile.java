package com.thy.fss.common.inmemory.common.model;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Secondary test entity with relationships for common test infrastructure.
 * Demonstrates one-to-one and one-to-many relationships.
 */
@MetaModel
public class TestProfile {
    private Long id;
    private String description;

    // Default constructor
    public TestProfile() {
        // Default constructor
    }

    // Constructor with basic fields (temporarily simplified)
    public TestProfile(String description) {
        this.description = description;
    }

    // Constructor with all fields (temporarily simplified)
    public TestProfile(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return "TestProfile{" +
                "id=" + id +
                ", description='" + description + '\'' +
                '}';
    }
}