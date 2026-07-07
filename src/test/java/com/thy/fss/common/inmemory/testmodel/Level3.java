package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Level 3 nested entity for complex structure testing.
 */
@MetaModel  // Commented out for unit testing
public class Level3 implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String value;

    @Override
    public String getIdentity() {
        return value;
    }

    public String getId() {
        return getIdentity();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}