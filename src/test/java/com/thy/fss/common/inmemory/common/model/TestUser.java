package com.thy.fss.common.inmemory.common.model;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Primary test entity for common test infrastructure.
 * Used across all test classes for consistent test data.
 */
@MetaModel
public class TestUser implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private String name;
    private Integer age;
    private String email;
    private Boolean active;
    private TestTag tag;
    private LocalDate birthDate;
    private LocalDateTime createdAt;

    // Default constructor
    public TestUser() {
    }

    // Constructor with basic fields
    public TestUser(String name, Integer age) {
        this.name = name;
        this.age = age;
        this.active = true;
    }

    // Constructor with all fields (temporarily simplified)
    public TestUser(Long id, String name, Integer age, String email, Boolean active) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
        this.active = active;
    }

    // Getters and setters
    @Override
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public TestTag getTag() {
        return tag;
    }

    public void setTag(TestTag tag) {
        this.tag = tag;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TestUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", active=" + active +
                ", tag=" + tag +
                ", birthDate=" + birthDate +
                ", createdAt=" + createdAt +
                '}';
    }
}