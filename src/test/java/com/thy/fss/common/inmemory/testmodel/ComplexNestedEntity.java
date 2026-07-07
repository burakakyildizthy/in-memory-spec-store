package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test model for complex nested structure testing.
 * Used to verify deep nesting capabilities of the meta model system.
 */
@MetaModel  // Commented out for unit testing
public class ComplexNestedEntity implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String name;
    private Level1 level1;

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

    public Level1 getLevel1() {
        return level1;
    }

    public void setLevel1(Level1 level1) {
        this.level1 = level1;
    }
}