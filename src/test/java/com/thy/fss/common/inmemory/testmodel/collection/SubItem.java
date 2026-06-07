package com.thy.fss.common.inmemory.testmodel.collection;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class SubItem {
    private Long id;
    private Integer quantity;
    private String name;

    public SubItem() {
    }

    public SubItem(Long id, Integer quantity, String name) {
        this.id = id;
        this.quantity = quantity;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
