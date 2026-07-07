package com.thy.fss.common.inmemory.factory.navigation.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity representing an order with nested customer
 */
@MetaModel
public class Order {
    private Long orderId;
    private Customer customer;
    private String orderCode;
    private Integer totalAmount;
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
}
