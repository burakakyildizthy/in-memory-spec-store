package com.thy.fss.common.inmemory.integration.testentities;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity for integration testing with nested model properties.
 * Represents a user with an address.
 */
@MetaModel
public class IntegrationTestUser implements Identifiable<Long> {
    
    private Long id;
    private String name;
    private String email;
    private Boolean active;
    private Integer age;
    private String role;
    private IntegrationTestAddress address;
    
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
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public IntegrationTestAddress getAddress() {
        return address;
    }
    
    public void setAddress(IntegrationTestAddress address) {
        this.address = address;
    }
}
