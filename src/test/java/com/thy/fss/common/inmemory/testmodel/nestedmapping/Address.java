package com.thy.fss.common.inmemory.testmodel.nestedmapping;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class Address {
    private Long id;
    private String street;
    private Long cityId;

    public Address() {
    }

    public Address(Long id, String street, Long cityId) {
        this.id = id;
        this.street = street;
        this.cityId = cityId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }
}
