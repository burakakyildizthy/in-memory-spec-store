package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;

@MetaModel
public class CrossTestUser {
    private String name;
    private String email;
    private Integer age;
    private String status;
    private LocalDateTime createdAt;

    public CrossTestUser() {
    }

    public CrossTestUser(String name, String email, Integer age, String status) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}