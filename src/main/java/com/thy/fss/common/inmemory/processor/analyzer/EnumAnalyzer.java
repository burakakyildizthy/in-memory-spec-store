package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * Analyzer for build-time enum analysis to extract Jackson deserialization configuration.
 * <p>
 * This analyzer examines enum classes during annotation processing to determine:
 * - @JsonCreator methods for custom enum deserialization
 * - @JsonValue fields/methods for value-based deserialization
 * - Default deserialization strategy when no Jackson annotations are present
 * <p>
 * The analysis results are used to generate optimized enum parsing code in filter deserializers,
 * eliminating runtime reflection and annotation checking for maximum performance.
 */
public class EnumAnalyzer {

    private final Elements elementUtils;
    private final Types typeUtils;

    public EnumAnalyzer(Elements elementUtils, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    /**
     * Analyzes an enum class to extract Jackson deserialization configuration.
     * <p>
     * Analysis priority:
     * 1. @JsonCreator methods (highest priority)
     * 2. @JsonValue fields or methods
     * 3. Default valueOf() with case-insensitive fallback
     *
     * @param enumElement the enum TypeElement to analyze
     * @return EnumDeserializationInfo containing the deserialization strategy and configuration
     */
    public EnumDeserializationInfo analyzeEnum(TypeElement enumElement) {
        if (enumElement == null || enumElement.getKind() != ElementKind.ENUM) {
            throw new IllegalArgumentException("Element must be an enum type");
        }

        EnumDeserializationInfo info = new EnumDeserializationInfo(enumElement.getQualifiedName().toString());

        // Step 1: Check for @JsonCreator methods (highest priority)
        EnumDeserializationInfo creatorInfo = analyzeJsonCreatorMethods(enumElement);
        if (creatorInfo.hasCustomDeserialization()) {
            return creatorInfo;
        }

        // Step 2: Check for @JsonValue fields/methods
        EnumDeserializationInfo valueInfo = analyzeJsonValueElements(enumElement);
        if (valueInfo.hasCustomDeserialization()) {
            return valueInfo;
        }

        // Step 3: Default case - no Jackson annotations found
        info.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        return info;
    }

    /**
     * Analyzes enum for @JsonCreator methods.
     *
     * @param enumElement the enum to analyze
     * @return EnumDeserializationInfo with @JsonCreator method information, or default if none found
     */
    private EnumDeserializationInfo analyzeJsonCreatorMethods(TypeElement enumElement) {
        EnumDeserializationInfo info = new EnumDeserializationInfo(enumElement.getQualifiedName().toString());

        List<? extends Element> enclosedElements = enumElement.getEnclosedElements();

        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;

                // Check if method has @JsonCreator annotation
                if (method.getAnnotation(JsonCreator.class) != null && isValidJsonCreatorMethod(method, enumElement)) {
                        info.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
                        info.setJsonCreatorMethod(method.getSimpleName().toString());
                        return info;
                    }

            }
        }

        return info; // No @JsonCreator method found
    }

    /**
     * Analyzes enum for @JsonValue fields and methods.
     *
     * @param enumElement the enum to analyze
     * @return EnumDeserializationInfo with @JsonValue information, or default if none found
     */
    private EnumDeserializationInfo analyzeJsonValueElements(TypeElement enumElement) {
        EnumDeserializationInfo info = new EnumDeserializationInfo(enumElement.getQualifiedName().toString());

        List<? extends Element> enclosedElements = enumElement.getEnclosedElements();

        for (Element element : enclosedElements) {
            if (element.getAnnotation(JsonValue.class) != null) {
                if (element.getKind() == ElementKind.FIELD) {
                    VariableElement field = (VariableElement) element;
                    if (isValidJsonValueField(field)) {
                        info.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
                        info.setJsonValueField(field.getSimpleName().toString());
                        return info;
                    }
                } else if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) element;
                    if (isValidJsonValueMethod(method)) {
                        info.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_METHOD);
                        info.setJsonValueMethod(method.getSimpleName().toString());
                        return info;
                    }
                }
            }
        }

        return info; // No @JsonValue field or method found
    }

    /**
     * Validates that a @JsonCreator method has the correct signature for enum deserialization.
     * <p>
     * Valid @JsonCreator method requirements:
     * - Must be static
     * - Must return the enum type
     * - Must have exactly one String parameter
     *
     * @param method      the method to validate
     * @param enumElement the enum type
     * @return true if the method is a valid @JsonCreator method
     */
    private boolean isValidJsonCreatorMethod(ExecutableElement method, TypeElement enumElement) {
        // Check if method is static
        if (!method.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
            return false;
        }

        // Check return type matches enum type
        TypeMirror returnType = method.getReturnType();
        TypeMirror enumType = enumElement.asType();
        if (!typeUtils.isSameType(returnType, enumType)) {
            return false;
        }

        // Check parameter count and type
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != 1) {
            return false;
        }

        // Check parameter is String type
        VariableElement parameter = parameters.get(0);
        TypeMirror stringType = elementUtils.getTypeElement("java.lang.String").asType();
        return typeUtils.isSameType(parameter.asType(), stringType);
    }

    /**
     * Validates that a @JsonValue field is suitable for deserialization.
     * <p>
     * Valid @JsonValue field requirements:
     * - Must not be static (instance field)
     * - Should be accessible (public or have getter)
     *
     * @param field the field to validate
     * @return true if the field is a valid @JsonValue field
     */
    private boolean isValidJsonValueField(VariableElement field) {
        // @JsonValue fields should not be static for enum instances
        return !field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC);
    }

    /**
     * Validates that a @JsonValue method is suitable for deserialization.
     * <p>
     * Valid @JsonValue method requirements:
     * - Must not be static (instance method)
     * - Must have no parameters
     * - Must return a serializable type (String, Number, Boolean, etc.)
     *
     * @param method the method to validate
     * @return true if the method is a valid @JsonValue method
     */
    private boolean isValidJsonValueMethod(ExecutableElement method) {
        // @JsonValue methods should not be static for enum instances
        if (method.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
            return false;
        }

        // Must have no parameters
        if (!method.getParameters().isEmpty()) {
            return false;
        }

        // Return type should be serializable (we'll accept any non-void type)
        TypeMirror returnType = method.getReturnType();
        return !typeUtils.isSameType(returnType, typeUtils.getNoType(javax.lang.model.type.TypeKind.VOID));
    }

    /**
     * Checks if the given type element represents an enum class.
     *
     * @param typeElement the type element to check
     * @return true if the element is an enum
     */
    public boolean isEnum(TypeElement typeElement) {
        return typeElement != null && typeElement.getKind() == ElementKind.ENUM;
    }

    /**
     * Checks if the given type mirror represents an enum type.
     *
     * @param typeMirror the type mirror to check
     * @return true if the type is an enum
     */
    public boolean isEnumType(TypeMirror typeMirror) {
        Element element = typeUtils.asElement(typeMirror);
        return (TypeElement) element instanceof TypeElement && isEnum((TypeElement) element);
    }

    /**
     * Extracts the enum TypeElement from a type mirror if it represents an enum.
     *
     * @param typeMirror the type mirror to extract from
     * @return the enum TypeElement, or null if not an enum type
     */
    public TypeElement getEnumTypeElement(TypeMirror typeMirror) {
        Element element = typeUtils.asElement(typeMirror);
        if (element instanceof TypeElement typeElement && isEnum(typeElement)) {
            return typeElement;
        }
        return null;
    }
}