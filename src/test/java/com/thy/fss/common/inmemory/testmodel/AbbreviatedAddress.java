package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Nested test model with @JsonProperty annotations for testing nested path mapping.
 */
@MetaModel
public class AbbreviatedAddress {

    @JsonProperty("st")
    private String street;

    @JsonProperty("c")
    private String city;

    @JsonProperty("z")
    private String zipCode;

    @JsonProperty("ctry")
    private String country;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
