package com.thy.fss.common.inmemory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Interface for entities that can provide their ID without reflection.
 * This replaces reflection-based ID extraction in the new meta-model system.
 *
 * @param <I> the type of the entity's ID
 */
public interface Identifiable<I> {

    /**
     * Gets the ID of this entity.
     *
     * @return the entity's ID
     */
    @JsonIgnore
    I getIdentity();
}