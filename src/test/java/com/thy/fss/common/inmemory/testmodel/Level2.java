package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Level 2 nested entity for complex structure testing.
 */
@MetaModel  // Commented out for unit testing
public class Level2 implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String name;
    private Level3 level3;

    @Override
    public String getIdentity() {
        return name;
    }

    public String getId() {
        return getIdentity();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level3 getLevel3() {
        return level3;
    }

    public void setLevel3(Level3 level3) {
        this.level3 = level3;
    }
}