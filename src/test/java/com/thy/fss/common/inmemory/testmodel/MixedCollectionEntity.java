package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.util.List;

/**
 * Test entity with mixed collection types for testing collection filter support.
 * Contains both basic type collections (String, Integer) and model type collections (User, Profile).
 */
@MetaModel
public class MixedCollectionEntity implements Identifiable<String> {
    
    private String id;
    private String name;
    
    // Basic type collections
    private List<String> tags;
    private List<Integer> scores;
    
    // Model type collections
    private List<User> users;
    private List<Profile> profiles;
    
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
    
    public List<User> getUsers() {
        return users;
    }
    
    public void setUsers(List<User> users) {
        this.users = users;
    }
    
    public List<Profile> getProfiles() {
        return profiles;
    }
    
    public void setProfiles(List<Profile> profiles) {
        this.profiles = profiles;
    }
}
