package com.thy.fss.common.inmemory.common.model;

import com.thy.fss.common.inmemory.processor.MetaModel;


/**
 * Test dashboard model for testing dashboard builder functionality.
 */
@MetaModel
public class TestDashboard {
    private Integer totalOrders;
    private Double totalRevenue;
    private Double avgOrderValue;

    public TestDashboard() {
        // Noncompliant - method is empty
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(Double avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }
}
