package com.thy.fss.common.inmemory.integration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Test model for REST API integration tests.
 */
@MetaModel
public class ApiProduct {
    private Long id;
    private String name;
    private Double price;
    private String category;
    private boolean available;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private String brand;

    // Constructors
    public ApiProduct() {
    }

    public ApiProduct(Long id, String name, Double price, String category, boolean available) {
        this();
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.available = available;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiProduct that = (ApiProduct) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ApiProduct{id=" + id + ", name='" + name + "', price=" + price +
                ", category='" + category + "', available=" + available + "}";
    }
}