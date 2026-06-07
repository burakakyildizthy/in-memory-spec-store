package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

/**
 * Test entity with collection fields for testing CollectionFilter web binding.
 * This entity is used to verify that the annotation processor generates correct
 * deserializer code for CollectionFilter fields.
 */
@MetaModel
public class CollectionTestEntity implements Identifiable<Long> {
    
    private Long id;
    private String name;
    private List<String> tags;
    private List<Integer> scores;
    private List<Priority> priorities;
    
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public List<Integer> getScores() {
        return scores;
    }
    
    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }
    
    public List<Priority> getPriorities() {
        return priorities;
    }
    
    public void setPriorities(List<Priority> priorities) {
        this.priorities = priorities;
    }
}
