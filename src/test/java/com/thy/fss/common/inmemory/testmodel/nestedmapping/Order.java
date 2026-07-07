package com.thy.fss.common.inmemory.testmodel.nestedmapping;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class Order {
    private Long id;
    private Long customerId;
    private Double totalAmount;
    private String customerCityName;

    public Order() {
    }

    public Order(Long id, Long customerId, Double totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
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

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCustomerCityName() {
        return customerCityName;
    }

    public void setCustomerCityName(String customerCityName) {
        this.customerCityName = customerCityName;
    }
}
