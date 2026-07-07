package com.thy.fss.common.inmemory.processor.model;

import java.util.Map;
import java.util.Objects;

/**
 * Data model representing Jackson annotation information extracted from entity fields.
 * Contains annotation type, parameters, generated code, and application scope.
 */
public class AnnotationInfo {

    private String annotationType;
    private Map<String, Object> parameters;
    private String annotationCode;
    private boolean appliesToField;
    private boolean appliesToGetter;
    private boolean appliesToSetter;

    public AnnotationInfo() {
    }

    public AnnotationInfo(String annotationType, Map<String, Object> parameters) {
        this.annotationType = annotationType;
        this.parameters = parameters;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getAnnotationCode() {
        return annotationCode;
    }

    public void setAnnotationCode(String annotationCode) {
        this.annotationCode = annotationCode;
    }

    public boolean isAppliesToField() {
        return appliesToField;
    }

    public void setAppliesToField(boolean appliesToField) {
        this.appliesToField = appliesToField;
    }

    public boolean isAppliesToGetter() {
        return appliesToGetter;
    }

    public void setAppliesToGetter(boolean appliesToGetter) {
        this.appliesToGetter = appliesToGetter;
    }

    public boolean isAppliesToSetter() {
        return appliesToSetter;
    }

    public void setAppliesToSetter(boolean appliesToSetter) {
        this.appliesToSetter = appliesToSetter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationInfo that = (AnnotationInfo) o;
        return appliesToField == that.appliesToField &&
                appliesToGetter == that.appliesToGetter &&
                appliesToSetter == that.appliesToSetter &&
                Objects.equals(annotationType, that.annotationType) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(annotationCode, that.annotationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationType, parameters, annotationCode,
                appliesToField, appliesToGetter, appliesToSetter);
    }

    @Override
    public String toString() {
        return "AnnotationInfo{" +
                "annotationType='" + annotationType + '\'' +
                ", parameters=" + parameters +
                ", annotationCode='" + annotationCode + '\'' +
                ", appliesToField=" + appliesToField +
                ", appliesToGetter=" + appliesToGetter +
                ", appliesToSetter=" + appliesToSetter +
                '}';
    }
}