package com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * A @MetaModel class in a different package, used to test cross-package import resolution.
 */
@MetaModel
public class Order {
    private Long id;
    private String description;
    private Double amount;

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
