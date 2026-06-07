package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Sales Dashboard model for real-world synchronization test.
 * This is a dashboard model (not an entity), so it doesn't implement Identifiable.
 */
@MetaModel
public class SalesDashboard {

    private Integer totalCustomers;
    private Integer totalOrders;
    private Double totalRevenue;
    private Double averageOrderValue;
    private Integer totalProducts;

    public Integer getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(Integer totalCustomers) {
        this.totalCustomers = totalCustomers;
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

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public Integer getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Integer totalProducts) {
        this.totalProducts = totalProducts;
    }
}
