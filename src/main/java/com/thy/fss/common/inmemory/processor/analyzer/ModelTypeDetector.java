package com.thy.fss.common.inmemory.processor.analyzer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Utility class for detecting model types and resolving their filter class names.
 * <p>
 * A model type is a complex object type that has its own generated filter class
 * (e.g., User has UserFilter, Address has AddressFilter). This detector helps
 * distinguish model types from basic types (String, Integer, etc.) during
 * annotation processing.
 * <p>
 * The detector searches for filter classes in:
 * - The same package as the model type
 * - Common filter packages (if configured)
 * <p>
 * This is used during code generation to determine whether collection elements
 * are basic types or model types, which affects how binding code is generated.
 *
 * @since 1.0
 */
public class ModelTypeDetector {

    /**
     * Checks if a type is a model type by looking for its corresponding filter class.
     * <p>
     * A type is considered a model type if a filter class with the naming convention
     * {TypeName}Filter exists in the same package as the type, AND the type is not an enum.
     * <p>
     * For example:
     * - com.example.User → looks for com.example.UserFilter
     * - com.example.Address → looks for com.example.AddressFilter
     * - com.example.Priority (enum) → NOT a model type (uses EnumFilter instead)
     *
     * @param typeName      The fully qualified type name (e.g., "com.example.User")
     * @param processingEnv The annotation processing environment
     * @return true if the type has a corresponding filter class and is not an enum, false otherwise
     */
    public static boolean isModelType(String typeName, ProcessingEnvironment processingEnv) {
        if (typeName == null || typeName.isEmpty()) {
            return false;
        }

        Elements elementUtils = processingEnv.getElementUtils();

        // First check if the type itself is an enum
        TypeElement typeElement = elementUtils.getTypeElement(typeName);
        if (typeElement != null && typeElement.getKind() == javax.lang.model.element.ElementKind.ENUM) {
            // Enums are not model types - they use EnumFilter
            return false;
        }

        // Get the filter class name for this type
        String filterClassName = getQualifiedFilterClassName(typeName, processingEnv);

        // Try to find the filter class
        TypeElement filterElement = elementUtils.getTypeElement(filterClassName);

        // If filter class exists, this is a model type
        return filterElement != null;
    }

    /**
     * Gets the simple filter class name for a model type.
     * <p>
     * Applies the naming convention: {ModelTypeName}Filter
     * <p>
     * Examples:
     * - "User" → "UserFilter"
     * - "Address" → "AddressFilter"
     * - "OrderItem" → "OrderItemFilter"
     *
     * @param modelTypeName The simple or fully qualified model type name
     * @return The simple filter class name (e.g., "UserFilter")
     */
    public static String getFilterClassName(String modelTypeName) {
        if (modelTypeName == null || modelTypeName.isEmpty()) {
            return null;
        }

        // Extract simple name if fully qualified
        String simpleName = modelTypeName;
        int lastDot = modelTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = modelTypeName.substring(lastDot + 1);
        }

        // Apply naming convention: {TypeName}Filter
        return simpleName + "Filter";
    }

    /**
     * Gets the fully qualified filter class name for a model type.
     * <p>
     * The filter class is expected to be in the same package as the model type.
     * <p>
     * Examples:
     * - "com.example.User" → "com.example.UserFilter"
     * - "com.example.model.Address" → "com.example.model.AddressFilter"
     *
     * @param modelTypeName The fully qualified model type name
     * @param processingEnv The annotation processing environment (unused but kept for consistency)
     * @return The fully qualified filter class name
     */
    public static String getQualifiedFilterClassName(String modelTypeName, ProcessingEnvironment processingEnv) {
        if (modelTypeName == null || modelTypeName.isEmpty()) {
            return null;
        }

        // Extract package name
        String packageName = "";
        int lastDot = modelTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            packageName = modelTypeName.substring(0, lastDot);
        }

        // Get simple filter class name
        String filterClassName = getFilterClassName(modelTypeName);

        // Combine package and filter class name
        if (packageName.isEmpty()) {
            return filterClassName;
        } else {
            return packageName + "." + filterClassName;
        }
    }
}
