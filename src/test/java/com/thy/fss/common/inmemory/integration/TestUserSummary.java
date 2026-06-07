package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Summary class for TestUser dashboard aggregations.
 * Used in ComplexWorkflowIntegrationTest.
 */
@MetaModel
public class TestUserSummary {
    private Integer id;  // COUNT aggregation
    private Integer age; // Various aggregations (AVG, MAX, MIN, SUM)
    private Integer name; // COUNT aggregation
    private Integer email; // COUNT aggregation
    private Integer active; // COUNT aggregation

    /**
     * Default constructor. Required for reflection-based instantiation.
     */
    public TestUserSummary() {
        // Default constructor
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getName() {
        return name;
    }

    public void setName(Integer name) {
        this.name = name;
    }

    public Integer getEmail() {
        return email;
    }

    public void setEmail(Integer email) {
        this.email = email;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "TestUserSummary{" +
                "id=" + id +
                ", age=" + age +
                ", name=" + name +
                ", email=" + email +
                ", active=" + active +
                '}';
    }
}