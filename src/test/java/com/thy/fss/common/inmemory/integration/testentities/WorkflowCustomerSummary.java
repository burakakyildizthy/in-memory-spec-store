package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;

@MetaModel
public class WorkflowCustomerSummary {
    private String customerName;
    private String tier;
    private Integer totalOrders;
    private Double totalSpent;
    private String status;
    private LocalDateTime lastOrderDate;

    public WorkflowCustomerSummary() {
    }

    public WorkflowCustomerSummary(String customerName, String tier, Integer totalOrders,
                                   Double totalSpent, String status, LocalDateTime lastOrderDate) {
        this.customerName = customerName;
        this.tier = tier;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.status = status;
        this.lastOrderDate = lastOrderDate;
    }

    // Getters and setters
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }
}