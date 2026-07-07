package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

/**
 * Level 1 nested entity for complex structure testing.
 */
@MetaModel  // Commented out for unit testing
public class Level1 implements com.thy.fss.common.inmemory.entity.Identifiable<String> {
    private String name;
    private List<String> items;
    private Level2 level2;

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

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public Level2 getLevel2() {
        return level2;
    }

    public void setLevel2(Level2 level2) {
        this.level2 = level2;
    }
}