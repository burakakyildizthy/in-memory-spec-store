package com.thy.fss.common.inmemory.testmodel.dashboard;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class SimpleDashboard {
    private Integer totalCount;
    private Double totalAmount;
    private Double averageAmount;

    public SimpleDashboard() {
        // Noncompliant - method is empty
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(Double averageAmount) {
        this.averageAmount = averageAmount;
    }
}
