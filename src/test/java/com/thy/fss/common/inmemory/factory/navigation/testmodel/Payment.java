package com.thy.fss.common.inmemory.factory.navigation.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity representing a payment with nested customer reference
 */
@MetaModel
public class Payment {
    private Long paymentId;
    private Customer customer;
    private String paymentCode;
    private Integer amount;
    
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}
