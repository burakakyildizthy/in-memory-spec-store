package com.thy.fss.common.inmemory.testmodel.collection;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

@MetaModel
public class OrderItemDetail {
    private Long id;
    private Long orderId;
    private String productName;
    private Double price;
    private List<SubItem> subItems;

    public OrderItemDetail() {
    }

    public OrderItemDetail(Long id, Long orderId, String productName, Double price) {
        this.id = id;
        this.orderId = orderId;
        this.productName = productName;
        this.price = price;
    }

    public OrderItemDetail(Long id, Long orderId, String productName, Double price, List<SubItem> subItems) {
        this.id = id;
        this.orderId = orderId;
        this.productName = productName;
        this.price = price;
        this.subItems = subItems;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<SubItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<SubItem> subItems) {
        this.subItems = subItems;
    }
}
