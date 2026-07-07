package com.thy.fss.common.inmemory.processor.model;

import java.util.List;
import java.util.Objects;

/**
 * Configuration model for filter field deserialization.
 * Contains field information, Jackson annotations, and type-specific configuration
 * needed to generate optimized deserializers for filter fields.
 */
public class FilterFieldConfig {

    private String fieldName;
    private String fieldType;
    private String filterType;
    private List<AnnotationInfo> jacksonAnnotations;
    private boolean isEnum;
    private boolean isCollection;
    private boolean isTemporal;
    private boolean isNumeric;
    private boolean isString;
    private boolean isModel;
    private String elementType;
    private boolean isModelElementType;
    private String elementFilterType;
    private String elementFilterPackage;
    private DateTimeFormatInfo dateTimeFormatInfo;
    private InstantFormatInfo instantFormatInfo;
    private EnumDeserializationInfo enumDeserializationInfo;
    private String packageName;

    public FilterFieldConfig() {
    }

    public FilterFieldConfig(String fieldName, String fieldType, String filterType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.filterType = filterType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public List<AnnotationInfo> getJacksonAnnotations() {
        return jacksonAnnotations;
    }

    public void setJacksonAnnotations(List<AnnotationInfo> jacksonAnnotations) {
        this.jacksonAnnotations = jacksonAnnotations;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public boolean isModel() {
        return isModel;
    }

    public void setModel(boolean isModel) {
        this.isModel = isModel;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    public boolean isTemporal() {
        return isTemporal;
    }

    public void setTemporal(boolean isTemporal) {
        this.isTemporal = isTemporal;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    public void setNumeric(boolean isNumeric) {
        this.isNumeric = isNumeric;
    }

    public boolean isString() {
        return isString;
    }

    public void setString(boolean isString) {
        this.isString = isString;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public boolean isModelElementType() {
        return isModelElementType;
    }

    public void setModelElementType(boolean isModelElementType) {
        this.isModelElementType = isModelElementType;
    }

    public String getElementFilterType() {
        return elementFilterType;
    }

    public void setElementFilterType(String elementFilterType) {
        this.elementFilterType = elementFilterType;
    }

    public String getElementFilterPackage() {
        return elementFilterPackage;
    }

    public void setElementFilterPackage(String elementFilterPackage) {
        this.elementFilterPackage = elementFilterPackage;
    }

    /**
     * Gets the fully qualified filter class name for the element type.
     * Returns null if this is not a model element type.
     *
     * @return the fully qualified filter class name, or null
     */
    public String getElementFilterQualifiedName() {
        if (!isModelElementType || elementFilterType == null) {
            return null;
        }
        if (elementFilterPackage == null || elementFilterPackage.isEmpty()) {
            return elementFilterType;
        }
        return elementFilterPackage + "." + elementFilterType;
    }

    public DateTimeFormatInfo getDateTimeFormatInfo() {
        return dateTimeFormatInfo;
    }

    public void setDateTimeFormatInfo(DateTimeFormatInfo dateTimeFormatInfo) {
        this.dateTimeFormatInfo = dateTimeFormatInfo;
    }

    public InstantFormatInfo getInstantFormatInfo() {
        return instantFormatInfo;
    }

    public void setInstantFormatInfo(InstantFormatInfo instantFormatInfo) {
        this.instantFormatInfo = instantFormatInfo;
    }

    public EnumDeserializationInfo getEnumDeserializationInfo() {
        return enumDeserializationInfo;
    }

    public void setEnumDeserializationInfo(EnumDeserializationInfo enumDeserializationInfo) {
        this.enumDeserializationInfo = enumDeserializationInfo;
    }

    /**
     * Checks if this field has Jackson annotations that need to be processed.
     *
     * @return true if Jackson annotations are present
     */
    public boolean hasJacksonAnnotations() {
        return jacksonAnnotations != null && !jacksonAnnotations.isEmpty();
    }

    /**
     * Checks if this field has custom datetime format configuration.
     *
     * @return true if custom datetime format is configured
     */
    public boolean hasCustomDateTimeFormat() {
        return dateTimeFormatInfo != null && dateTimeFormatInfo.usesCustomFormat();
    }

    /**
     * Checks if this field has custom Instant format configuration.
     *
     * @return true if custom Instant format is configured
     */
    public boolean hasCustomInstantFormat() {
        return instantFormatInfo != null && instantFormatInfo.usesCustomFormat();
    }

    /**
     * Checks if this field has custom enum deserialization configuration.
     *
     * @return true if custom enum deserialization is configured
     */
    public boolean hasCustomEnumDeserialization() {
        return enumDeserializationInfo != null && enumDeserializationInfo.hasCustomDeserialization();
    }

    /**
     * Gets the effective datetime pattern for this field.
     *
     * @return the datetime pattern string, or null if not a temporal field
     */
    public String getEffectiveDateTimePattern() {
        return dateTimeFormatInfo != null ? dateTimeFormatInfo.getEffectivePattern() : null;
    }

    /**
     * Gets the effective Instant pattern for this field.
     *
     * @return the Instant pattern string, or null if not an Instant field
     */
    public String getEffectiveInstantPattern() {
        return instantFormatInfo != null ? instantFormatInfo.getEffectivePattern() : null;
    }

    /**
     * Gets the enum deserialization strategy for this field.
     *
     * @return the deserialization type, or null if not an enum field
     */
    public EnumDeserializationInfo.DeserializationType getEnumDeserializationType() {
        return enumDeserializationInfo != null ? enumDeserializationInfo.getDeserializationType() : null;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterFieldConfig that = (FilterFieldConfig) o;
        return isEnum == that.isEnum &&
                isCollection == that.isCollection &&
                isTemporal == that.isTemporal &&
                isNumeric == that.isNumeric &&
                isString == that.isString &&
                isModelElementType == that.isModelElementType &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(fieldType, that.fieldType) &&
                Objects.equals(filterType, that.filterType) &&
                Objects.equals(jacksonAnnotations, that.jacksonAnnotations) &&
                Objects.equals(elementType, that.elementType) &&
                Objects.equals(elementFilterType, that.elementFilterType) &&
                Objects.equals(elementFilterPackage, that.elementFilterPackage) &&
                Objects.equals(dateTimeFormatInfo, that.dateTimeFormatInfo) &&
                Objects.equals(instantFormatInfo, that.instantFormatInfo) &&
                Objects.equals(packageName, that.packageName) &&
                Objects.equals(enumDeserializationInfo, that.enumDeserializationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, fieldType, filterType, jacksonAnnotations,
                isEnum, isCollection, isTemporal, isNumeric, isString, isModelElementType,
                elementType, elementFilterType, elementFilterPackage,
                dateTimeFormatInfo, instantFormatInfo, enumDeserializationInfo, packageName);
    }

    @Override
    public String toString() {
        return "FilterFieldConfig{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", filterType='" + filterType + '\'' +
                ", isEnum=" + isEnum +
                ", isCollection=" + isCollection +
                ", isTemporal=" + isTemporal +
                ", isNumeric=" + isNumeric +
                ", isString=" + isString +
                ", isModelElementType=" + isModelElementType +
                ", elementType='" + elementType + '\'' +
                ", elementFilterType='" + elementFilterType + '\'' +
                ", elementFilterPackage='" + elementFilterPackage + '\'' +
                ", packageName='" + packageName + '\'' +
                ", hasJacksonAnnotations=" + hasJacksonAnnotations() +
                ", hasCustomDateTimeFormat=" + hasCustomDateTimeFormat() +
                ", hasCustomInstantFormat=" + hasCustomInstantFormat() +
                ", hasCustomEnumDeserialization=" + hasCustomEnumDeserialization() +
                '}';
    }
}