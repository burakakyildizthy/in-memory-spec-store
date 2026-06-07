package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

/**
 * Test entity for integration testing with model type collections.
 * Represents a company with a collection of users.
 */
@MetaModel
public class IntegrationTestCompany implements Identifiable<Long> {
    
    private Long id;
    private String name;
    private String industry;
    private List<IntegrationTestUser> users;
    
    @Override
    public Long getIdentity() {
        return id;
    }

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
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public List<IntegrationTestUser> getUsers() {
        return users;
    }
    
    public void setUsers(List<IntegrationTestUser> users) {
        this.users = users;
    }
}
