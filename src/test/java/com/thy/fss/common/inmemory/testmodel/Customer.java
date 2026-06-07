package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer entity for real-world synchronization test.
 */
@MetaModel
public class Customer implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime registrationDate;
    private Boolean active;
    private LocalDateTime lastUpdated;

    // Aggregated fields from orders
    private Integer totalOrders;
    private Double totalSpent;
    private Double averageOrderValue;

    // Collection mapping
    private List<Order> orders;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
}
