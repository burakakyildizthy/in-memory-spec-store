package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Executive profile for nested object testing with complex attributes.
 */
@MetaModel
public class ExecutiveProfile {
    private String name;
    private Integer age;
    private Integer yearsOfExperience;
    private EducationLevel education;

    // Constructors
    public ExecutiveProfile() {
        // Default constructor
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public EducationLevel getEducation() {
        return education;
    }

    public void setEducation(EducationLevel education) {
        this.education = education;
    }
}