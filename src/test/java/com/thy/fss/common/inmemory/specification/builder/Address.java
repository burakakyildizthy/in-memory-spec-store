package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Address entity for nested object testing.
 */
@MetaModel
public class Address {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Region region;

    /**
     * Default constructor. Required for frameworks that instantiate objects via reflection.
     */
    public Address() {
        // Default constructor
    }

    // Getters and Setters
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}