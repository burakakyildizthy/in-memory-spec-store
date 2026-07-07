package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.processor.MetaModel;


@MetaModel
public class WorkflowProduct {
    private String productId;
    private String name;
    private String category;
    private Double price;
    private Integer stockLevel;
    private String status;

    public WorkflowProduct() {
    }

    public WorkflowProduct(String productId, String name, String category, Double price, Integer stockLevel, String status) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockLevel = stockLevel;
        this.status = status;
    }

    // Getters and setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}