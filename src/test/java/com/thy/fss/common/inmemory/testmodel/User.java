package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test model with nested object for StaticMetaModel generation testing.
 */
@MetaModel
public class User implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String id;
    private String name;
    private Profile profile;

    @Override
    public String getIdentity() {
        return id;
    }

    public String getId() {
        return getIdentity();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}