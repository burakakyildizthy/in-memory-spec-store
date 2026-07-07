package com.thy.fss.common.inmemory.factory.testmodels;

import com.thy.fss.common.inmemory.processor.MetaModel;


/**
 * Test dashboard model for testing dashboard builder.
 */
@MetaModel
public class TestDashboardModel {
    private Integer totalOrders;
    private Double totalRevenue;
    private Double avgOrderValue;

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
