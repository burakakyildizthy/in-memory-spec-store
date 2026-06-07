package com.thy.fss.common.inmemory.testmodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.processor.MetaModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Test entity with various Jackson annotations on temporal fields
 * for comprehensive integration testing.
 */
@MetaModel
public class AnnotatedTemporalEntity implements com.thy.fss.common.inmemory.entity.Identifiable<Long> {

    private Long id;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime customFormattedDateTime;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate customFormattedDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private Instant customFormattedInstant;

    // No annotation - should use default format
    private LocalDateTime defaultDateTime;

    // No annotation - should use default format
    private LocalDate defaultDate;

    // No annotation - should use default format
    private Instant defaultInstant;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("birth_date")
    private LocalDate birthDate;

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

    public LocalDateTime getCustomFormattedDateTime() {
        return customFormattedDateTime;
    }

    public void setCustomFormattedDateTime(LocalDateTime customFormattedDateTime) {
        this.customFormattedDateTime = customFormattedDateTime;
    }

    public LocalDate getCustomFormattedDate() {
        return customFormattedDate;
    }

    public void setCustomFormattedDate(LocalDate customFormattedDate) {
        this.customFormattedDate = customFormattedDate;
    }

    public Instant getCustomFormattedInstant() {
        return customFormattedInstant;
    }

    public void setCustomFormattedInstant(Instant customFormattedInstant) {
        this.customFormattedInstant = customFormattedInstant;
    }

    public LocalDateTime getDefaultDateTime() {
        return defaultDateTime;
    }

    public void setDefaultDateTime(LocalDateTime defaultDateTime) {
        this.defaultDateTime = defaultDateTime;
    }

    public LocalDate getDefaultDate() {
        return defaultDate;
    }

    public void setDefaultDate(LocalDate defaultDate) {
        this.defaultDate = defaultDate;
    }

    public Instant getDefaultInstant() {
        return defaultInstant;
    }

    public void setDefaultInstant(Instant defaultInstant) {
        this.defaultInstant = defaultInstant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}