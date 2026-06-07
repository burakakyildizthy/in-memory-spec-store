package com.thy.fss.common.inmemory.engine.production;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity for aggregation tests.
 */
@MetaModel
public class TestEntity {
    private Long id;
    private Double value;
    private String status;

    public TestEntity() {
    }

    public TestEntity(Long id, Double value, String status) {
        this.id = id;
        this.value = value;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
