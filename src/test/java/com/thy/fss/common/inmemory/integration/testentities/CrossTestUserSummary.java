package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;


@MetaModel
public class CrossTestUserSummary {
    private String userName;
    private Integer totalOrders;
    private Double totalAmount;
    private String userStatus;

    public CrossTestUserSummary() {
    }

    public CrossTestUserSummary(String userName, Integer totalOrders, Double totalAmount, String userStatus) {
        this.userName = userName;
        this.totalOrders = totalOrders;
        this.totalAmount = totalAmount;
        this.userStatus = userStatus;
    }

    // Getters and setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}