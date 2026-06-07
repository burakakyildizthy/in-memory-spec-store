package com.thy.fss.common.inmemory.common.model;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.Objects;

/**
 * Collection test entity for common test infrastructure.
 * Used for testing collection relationships and filtering.
 */
@MetaModel
public class TestTag {
    private Long id;
    private String name;
    private String category;

    // Default constructor
    public TestTag() {
    }

    // Constructor with basic fields
    public TestTag(String name) {
        this.name = name;
        this.category = "default";
    }

    // Constructor with name and category
    public TestTag(String name, String category) {
        this.name = name;
        this.category = category;
    }

    // Constructor with all fields
    public TestTag(Long id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    // Getters and setters
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "TestTag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestTag testTag = (TestTag) o;

        if (!Objects.equals(id, testTag.id)) return false;
        if (!Objects.equals(name, testTag.name)) return false;
        return Objects.equals(category, testTag.category);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        return result;
    }
}