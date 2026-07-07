package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;
import java.util.Set;

/**
 * Test model with collection fields for StaticMetaModel generation testing.
 */
@MetaModel
public class CollectionEntity implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private List<String> tags;
    private Set<Integer> numbers;

    @Override
    public Long getIdentity() {
        return id;
    }

    public Long getId() {
        return getIdentity();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Set<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(Set<Integer> numbers) {
        this.numbers = numbers;
    }
}