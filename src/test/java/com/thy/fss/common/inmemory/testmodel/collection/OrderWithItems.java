package com.thy.fss.common.inmemory.testmodel.collection;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

@MetaModel
public class OrderWithItems {
    private Long id;
    private Long customerId;
    private List<OrderItemDetail> orderItems;
    private Double totalItemPrice;
    private String firstProductName;
    private Double lastItemPrice;

    public OrderWithItems() {
    }

    public OrderWithItems(Long id, Long customerId, List<OrderItemDetail> orderItems) {
        this.id = id;
        this.customerId = customerId;
        this.orderItems = orderItems;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemDetail> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDetail> orderItems) {
        this.orderItems = orderItems;
    }

    public Double getTotalItemPrice() {
        return totalItemPrice;
    }

    public void setTotalItemPrice(Double totalItemPrice) {
        this.totalItemPrice = totalItemPrice;
    }

    public String getFirstProductName() {
        return firstProductName;
    }

    public void setFirstProductName(String firstProductName) {
        this.firstProductName = firstProductName;
    }

    public Double getLastItemPrice() {
        return lastItemPrice;
    }

    public void setLastItemPrice(Double lastItemPrice) {
        this.lastItemPrice = lastItemPrice;
    }
}
