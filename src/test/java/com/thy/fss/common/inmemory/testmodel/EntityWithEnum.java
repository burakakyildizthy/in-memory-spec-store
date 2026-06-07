package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test model with enum field for StaticMetaModel generation testing.
 */
@MetaModel  // Commented out for unit testing
public class EntityWithEnum implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private String name;
    private Status status;

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}