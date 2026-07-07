package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;

@MetaModel
public class WorkflowCustomer {
    private String customerId;
    private String name;
    private String email;
    private String tier;
    private LocalDateTime registrationDate;
    private String status;

    public WorkflowCustomer() {
    }

    public WorkflowCustomer(String customerId, String name, String email, String tier, String status) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.status = status;
        this.registrationDate = LocalDateTime.now();
    }

    // Getters and setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

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

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}