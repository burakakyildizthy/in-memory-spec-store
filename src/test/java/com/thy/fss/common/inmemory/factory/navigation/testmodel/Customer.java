package com.thy.fss.common.inmemory.factory.navigation.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity representing a customer with nested fields
 */
@MetaModel
public class Customer {
    private Long customerId;
    private String customerName;
    private String region;
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
