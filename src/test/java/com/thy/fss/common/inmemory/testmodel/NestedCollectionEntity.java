package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

/**
 * Test model with nested collection type (List<List<String>>) for testing
 * legitimate nesting scenarios where the element type itself is a collection.
 */
@MetaModel
public class NestedCollectionEntity implements com.thy.fss.common.inmemory.entity.Identifiable<String> {

    private String id;
    private String name;
    private List<List<String>> nestedTags;

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

    public List<List<String>> getNestedTags() {
        return nestedTags;
    }

    public void setNestedTags(List<List<String>> nestedTags) {
        this.nestedTags = nestedTags;
    }
}
