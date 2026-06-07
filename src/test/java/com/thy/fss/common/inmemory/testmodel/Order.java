package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order entity for real-world synchronization test.
 */
@MetaModel
public class Order implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {

    private Long id;
    private Long customerId;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private String status;
    private LocalDateTime lastUpdated;

    // Collection mapping
    private List<OrderItem> items;

    @Override
    public Long getIdentity() {
        return id;
    }

    public Long getId() {
        return getIdentity();
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

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
