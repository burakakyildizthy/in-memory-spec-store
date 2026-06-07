package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Region entity for deeper nested object testing.
 */
@MetaModel
public class Region {
    private String name;
    private String continent;
    private String timezone;
    private String currency;

    // Constructors
    public Region() {
        // Default constructor. Required for some frameworks.
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}