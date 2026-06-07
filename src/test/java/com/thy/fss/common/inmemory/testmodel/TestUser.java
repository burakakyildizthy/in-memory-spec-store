package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test user model for DataCollectionPhaseTest.
 */
@MetaModel
public class TestUser implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private String name;
    private String email;
    private Boolean active;

    public TestUser() {
    }

    public TestUser(Long id, String name, String email, Boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.active = active;
    }

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
