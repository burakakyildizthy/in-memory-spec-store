package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test model with @JsonProperty annotations for testing mapFieldPath() generation.
 */
@MetaModel
public class AbbreviatedUser implements Identifiable<Long> {

    private Long id;

    @JsonProperty("n")
    private String name;

    @JsonProperty("e")
    private String email;

    @JsonProperty("a")
    private Integer age;

    @JsonProperty("stat")
    private UserStatus status;

    @JsonProperty("addr")
    private AbbreviatedAddress address;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public AbbreviatedAddress getAddress() {
        return address;
    }

    public void setAddress(AbbreviatedAddress address) {
        this.address = address;
    }
}
