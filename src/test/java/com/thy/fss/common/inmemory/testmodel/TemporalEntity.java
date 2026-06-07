package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Test model with temporal fields for StaticMetaModel generation testing.
 */
@MetaModel  // Commented out for unit testing
public class TemporalEntity implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {
    private Long id;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private Instant lastModified;

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }
}