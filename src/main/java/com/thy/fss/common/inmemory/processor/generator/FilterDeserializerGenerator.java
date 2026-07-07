package com.thy.fss.common.inmemory.processor.generator;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.DoubleFilter;
import com.thy.fss.common.inmemory.filter.EnumFilter;
import com.thy.fss.common.inmemory.filter.Filter;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.filter.InstantFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.LocalDateFilter;
import com.thy.fss.common.inmemory.filter.LocalDateTimeFilter;
import com.thy.fss.common.inmemory.filter.LongFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.processor.analyzer.ModelTypeDetector;
import com.thy.fss.common.inmemory.processor.exception.DeserializerGenerationException;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;

/**
 * Unified generator for high-performance Jackson deserializers for all filter
 * types. Supports LocalDateTimeFilter, StringFilter, EnumFilter, IntegerFilter,
 * LongFilter, DoubleFilter, BooleanFilter, and nested model filters.
 * 
 * <p>Provides streaming JSON parser templates and performance optimizations
 * including switch statements, pre-sized collections, and type-specific parsing
 * logic.</p>
 * 
 * <h2>Model Type Detection and Collection Filtering</h2>
 * 
 * <p>This generator includes sophisticated model type detection logic for collection
 * filters. When processing a {@code CollectionFilter<E>} field, it automatically
 * determines whether the element type {@code E} is a basic type (String, Integer, etc.)
 * or a model type (User, Address, etc.) by checking for the existence of a corresponding
 * filter class.</p>
 * 
 * Model Type Detection Process
 * <ol>
 *   <li>Extract element type from {@code CollectionFilter<E>} generic parameter</li>
 *   <li>Use {@link ModelTypeDetector} to check if element type has a filter class</li>
 *   <li>If filter class exists, mark as model type and store filter class information</li>
 *   <li>Generate appropriate binding code based on element type classification</li>
 * </ol>
 * 
 * Delegation to Nested Model Binding
 * 
 * <p>For model type collections, this generator reuses the existing nested model binding
 * mechanism. When a collection operator (any, all, none) is used with a model type element,
 * the generated code:</p>
 * <ol>
 *   <li>Creates an instance of the element filter class (e.g., UserFilter)</li>
 *   <li>Extracts the remaining path after the collection operator</li>
 *   <li>Delegates to the existing {@code bindNestedModelFilter} method</li>
 *   <li>Uses type-safe casting with {@link com.thy.fss.common.inmemory.filter.FilterBase} 
 *       and instanceof checks</li>
 * </ol>
 * 
 * <h2>Generated Code Examples</h2>
 * 
 * Basic Type Collection
 * <pre>{@code
 * // For CollectionFilter<String> tags field
 * case "tags.any.eq" -> {
 *     if (filter.getTags() == null) {
 *         filter.setTags(new CollectionFilter<>());
 *     }
 *     if (filter.getTags().getCollectionAny() == null) {
 *         filter.getTags().setCollectionAny(new StringFilter());
 *     }
 *     ((StringFilter) filter.getTags().getCollectionAny()).setEquals(value);
 * }
 * }</pre>
 * 
 * Model Type Collection
 * <pre>{@code
 * // For CollectionFilter<User> users field
 * case "users.any" -> {
 *     if (filter.getUsers() == null) {
 *         filter.setUsers(new CollectionFilter<>());
 *     }
 *     if (filter.getUsers().getCollectionAny() == null) {
 *         filter.getUsers().setCollectionAny(new UserFilter());
 *     }
 *     // Extract remaining path after "users.any"
 *     String remainingPath = key.substring("users.any.".length());
 *     // Delegate to existing nested model binding
 *     FilterBase<User> elementFilter = filter.getUsers().getCollectionAny();
 *     if (elementFilter instanceof UserFilter) {
 *         bindNestedModelFilter((UserFilter) elementFilter, remainingPath, value, 
 *                              deserializer, registry);
 *     }
 * }
 * }</pre>
 * 
 * Type-Safe Casting with FilterBase
 * <p>The generated code uses {@link com.thy.fss.common.inmemory.filter.FilterBase} for
 * type compatibility and instanceof checks for type safety:</p>
 * <pre>{@code
 * FilterBase<User> elementFilter = filter.getUsers().getCollectionAny();
 * if (elementFilter instanceof UserFilter) {
 *     UserFilter userFilter = (UserFilter) elementFilter;
 *     // Type-safe operations on UserFilter
 * }
 * }</pre>
 * 
 * <h2>Import Generation</h2>
 * 
 * <p>For model type collections, the generator automatically adds necessary imports:</p>
 * <ul>
 *   <li>Element filter class (e.g., {@code com.example.UserFilter})</li>
 *   <li>Element type class if needed (e.g., {@code com.example.User})</li>
 *   <li>{@link com.thy.fss.common.inmemory.filter.FilterBase} for type compatibility</li>
 * </ul>
 * 
 * @see ModelTypeDetector
 * @see com.thy.fss.common.inmemory.filter.FilterBase
 * @see com.thy.fss.common.inmemory.filter.CollectionFilter
 */
public class FilterDeserializerGenerator {
    
    private static final String GET_INT_VALUE = "p.getIntValue()";
    private static final String LONG_FILTER = "LongFilter";
    private static final String DOUBLE_FILTER = "DoubleFilter";
    private static final String INTEGER_FILTER = "IntegerFilter";
    private static final String DOUBLE = "Double";
    private static final String LOWER_BOOLEAN = "boolean";
    private static final String EXP_1 = "     */";
    private static final String EXP_2 = "     * ";
    private static final String EXP_3 = "                    }";
    private static final String EXP_4 = "                        ";
    private static final String EXP_5 = "                }";
    private static final String EXP_6 = "            }";
    private static final String EXP_7 = "    /**";
    private static final String EXP_8 = "\" -> {";
    private static final String EXP_9 = "        ";
    private static final String EXP_10 = "        }";
    private static final String EXP_11 = "    }";
    private static final String EXP_12 = "                    if (p.getCurrentToken() == ";
    private static final String PARSE = "parse";
    private static final String CLASS_EXP = ".class);";
    private static final String VALUE = "value";
    private static final String FILTER_GETTER_PREFIX = "get";
    private static final String FILTER_SETTER_PREFIX = "set";
    private static final String FIELD_CASE_PREFIX = "            case \"";
    private static final String FIELD_CASE_SUFFIX = " -> {";
    private static final String DESERIALIZER_SUFFIX = "Deserializer";
    private static final String TYPICAL_IN_SIZE = ".TYPICAL_IN_SIZE);";
    

    // Debug mode flag - controlled by system property
    private static final boolean DEBUG_MODE = Boolean.parseBoolean(
            System.getProperty("inmemory.processor.debug", "false"));
    // Common imports needed for all filter deserializers
    private static final Set<String> COMMON_IMPORTS = Set.of(
            JsonParser.class.getName(),
            JsonToken.class.getName(),
            DeserializationContext.class.getName(),
            JsonDeserializer.class.getName(),
            IOException.class.getName(),
            ArrayList.class.getName(),
            List.class.getName(),
            DateTimeFormatter.class.getName(),
            FilterConstants.class.getName()
    );
    protected final ProcessingEnvironment processingEnv;

    /**
     * Constructor for FilterDeserializerGenerator.
     *
     * @param processingEnv the processing environment
     */
    public FilterDeserializerGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Prints debug message only if debug mode is enabled.
     */
    protected void debugLog(String message) {
        if (DEBUG_MODE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[DEBUG] FilterDeserializerGenerator: " + message
            );
        }
    }

    /**
     * Generates a custom deserializer for the specified filter type. Includes
     * comprehensive error handling, validation, and fallback mechanisms.
     *
     * @param filterClassName the name of the filter class
     * @param packageName     the package name for the deserializer
     * @param fieldConfigs    configuration for each field in the filter
     * @param entityClassName the name of the entity class
     * @throws ProcessingException if generation fails
     */
    public void generateDeserializer(String filterClassName, String packageName,
                                     List<FilterFieldConfig> fieldConfigs, String entityClassName) throws ProcessingException {

        // Input validation
        validateGenerationInputs(filterClassName, packageName, fieldConfigs);

        String deserializerClassName = filterClassName + DESERIALIZER_SUFFIX;

        debugLog("Starting deserializer generation for " + filterClassName);
        debugLog("Package: " + packageName + ", Deserializer class: " + deserializerClassName);

        JavaFileObject deserializerFile = null;
        PrintWriter out = null;

        try {
            // Validate field configurations before generation
            validateFieldConfigurations(fieldConfigs, filterClassName);

            // Create the source file
            deserializerFile = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + deserializerClassName);

            out = new PrintWriter(deserializerFile.openWriter());

            debugLog("Starting deserializer class generation");

            // Generate the deserializer class with error handling
            generateDeserializerClassWithValidation(out, filterClassName, deserializerClassName,
                    packageName, fieldConfigs, entityClassName);

            debugLog("Deserializer class generation completed");

            // Validate generated code before finalizing
            validateGeneratedCode(out, deserializerClassName);

            debugLog("Generated deserializer: " + packageName + "." + deserializerClassName);

        } catch (DeserializerGenerationException e) {
            // Re-throw deserializer-specific exceptions
            logError("Deserializer generation failed: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            String phase = (deserializerFile == null) ? "file_creation" : "code_generation";
            logError("IOException during " + phase + ": " + e.getMessage());
            throw new DeserializerGenerationException(filterClassName, deserializerClassName,
                    phase, "IO error: " + e.getMessage(), e);
        } catch (Exception e) {
            String phase = (out == null) ? "initialization" : "code_generation";
            logError("Unexpected error during " + phase + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw new DeserializerGenerationException(filterClassName, deserializerClassName,
                    phase, "Unexpected error: " + e.getMessage(), e);
        } finally {
            // Ensure resources are properly closed
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    logWarning("Failed to close PrintWriter: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Validates inputs before starting generation.
     */
    private void validateGenerationInputs(String filterClassName, String packageName,
                                          List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        if (filterClassName == null || filterClassName.trim().isEmpty()) {
            throw new DeserializerGenerationException("", "", "input_validation",
                    "Filter class name cannot be null or empty");
        }

        if (packageName == null) {
            throw new DeserializerGenerationException(filterClassName, "", "input_validation",
                    "Package name cannot be null");
        }

        if (fieldConfigs == null) {
            throw new DeserializerGenerationException(filterClassName, "", "input_validation",
                    "Field configurations cannot be null");
        }

        // Validate class name format
        if (!isValidJavaIdentifier(filterClassName)) {
            throw new DeserializerGenerationException(filterClassName, "", "input_validation",
                    "Invalid filter class name format: " + filterClassName);
        }

        // Validate package name format
        if (!packageName.isEmpty() && !isValidPackageName(packageName)) {
            throw new DeserializerGenerationException(filterClassName, "", "input_validation",
                    "Invalid package name format: " + packageName);
        }
    }

    /**
     * Validates field configurations for consistency and completeness.
     */
    private void validateFieldConfigurations(List<FilterFieldConfig> fieldConfigs,
                                             String filterClassName) throws DeserializerGenerationException {
        if (fieldConfigs.isEmpty()) {
            logWarning("No field configurations provided for " + filterClassName + " - generating empty deserializer");
            return;
        }

        for (int i = 0; i < fieldConfigs.size(); i++) {
            FilterFieldConfig config = fieldConfigs.get(i);
            if (config == null) {
                throw new DeserializerGenerationException(filterClassName, "", "field_validation",
                        "Field configuration at index " + i + " is null");
            }

            if (config.getFieldName() == null || config.getFieldName().trim().isEmpty()) {
                throw new DeserializerGenerationException(filterClassName, "", "field_validation",
                        "Field name at index " + i + " is null or empty");
            }

            if (config.getFieldType() == null || config.getFieldType().trim().isEmpty()) {
                throw new DeserializerGenerationException(filterClassName, "", "field_validation",
                        "Field type at index " + i + " is null or empty");
            }

            if (config.getFilterType() == null || config.getFilterType().trim().isEmpty()) {
                throw new DeserializerGenerationException(filterClassName, "", "field_validation",
                        "Filter type at index " + i + " is null or empty");
            }

            // Validate field name format
            if (!isValidJavaIdentifier(config.getFieldName())) {
                throw new DeserializerGenerationException(filterClassName, "", "field_validation",
                        "Invalid field name format: " + config.getFieldName());
            }
        }
    }

    /**
     * Generates the deserializer class with comprehensive error handling.
     */
    private void generateDeserializerClassWithValidation(PrintWriter out, String filterClassName,
                                                         String deserializerClassName, String packageName,
                                                         List<FilterFieldConfig> fieldConfigs, String entityClassName) throws DeserializerGenerationException {
        try {
            debugLog("Generating deserializer class " + deserializerClassName);

            // Package declaration
            generatePackageDeclaration(out, packageName);

            // Imports
            debugLog("Generating imports");
            generateImportsWithValidation(out, packageName, fieldConfigs);

            // Class declaration with JavaDoc
            debugLog("Adding class declaration and JavaDoc");
            generateClassDeclarationWithValidation(out, filterClassName, deserializerClassName, entityClassName);

            // Field-specific constants (formatters, etc.)
            debugLog("Generating field-specific constants");
            generateFieldConstantsWithValidation(out, fieldConfigs);

            // Main deserialize method
            debugLog("Generating deserialize method");
            generateDeserializeMethodWithValidation(out, filterClassName, fieldConfigs);

            // Helper methods for specific field types
            debugLog("Generating helper methods");
            generateHelperMethodsWithValidation(out, filterClassName, fieldConfigs);

            out.println("}");

            debugLog("Deserializer class generation completed for " + deserializerClassName);

        } catch (Exception e) {
            throw new DeserializerGenerationException(filterClassName, deserializerClassName,
                    "class_generation", "Failed to generate class content: " + e.getMessage(), e);
        }
    }

    /**
     * Generates package declaration with validation.
     */
    private void generatePackageDeclaration(PrintWriter out, String packageName) throws DeserializerGenerationException {
        try {
            if (!packageName.isEmpty()) {
                out.println("package " + packageName + ";");
                out.println();
                debugLog("Added package declaration: " + packageName);
            }
        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "package_declaration",
                    "Failed to generate package declaration: " + e.getMessage(), e);
        }
    }

    /**
     * Validates generated code before finalizing.
     */
    private void validateGeneratedCode(PrintWriter out, String deserializerClassName) throws DeserializerGenerationException {
        try {
            // Flush the writer to ensure all content is written
            out.flush();

            // Basic validation - check if writer is in error state
            if (out.checkError()) {
                throw new DeserializerGenerationException("", deserializerClassName, "code_validation",
                        "PrintWriter encountered errors during code generation");
            }

            debugLog("Generated code validation passed for " + deserializerClassName);

        } catch (Exception e) {
            throw new DeserializerGenerationException("", deserializerClassName, "code_validation",
                    "Code validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets additional imports needed based on field configurations.
     */
    protected Set<String> getAdditionalImports(List<FilterFieldConfig> fieldConfigs) {
        Set<String> imports = new HashSet<>();

        for (FilterFieldConfig config : fieldConfigs) {
            // Add imports based on field type
            if (config.isTemporal()) {
                if (config.getFieldType().contains(LocalDateTime.class.getSimpleName())) {
                    imports.add(LocalDateTime.class.getName());
                    imports.add(LocalDateTimeFilter.class.getName());
                } else if (config.getFieldType().contains(LocalDate.class.getSimpleName())) {
                    imports.add(LocalDate.class.getName());
                    imports.add(LocalDateFilter.class.getName());
                } else if (config.getFieldType().contains(Instant.class.getSimpleName())) {
                    imports.add(Instant.class.getName());
                    imports.add(InstantFilter.class.getName());
                }
            } else if (config.isString()) {
                imports.add(StringFilter.class.getName());
            } else if (config.isNumeric()) {
                if (config.getFilterType().equals(IntegerFilter.class.getSimpleName())) {
                    imports.add(IntegerFilter.class.getName());
                } else if (config.getFilterType().equals(LongFilter.class.getSimpleName())) {
                    imports.add(LongFilter.class.getName());
                } else if (config.getFilterType().equals(DoubleFilter.class.getSimpleName())) {
                    imports.add(DoubleFilter.class.getName());
                }
            } else if (config.getFilterType().equals("BooleanFilter")) {
                imports.add("com.thy.fss.common.inmemory.filter.BooleanFilter");
            } else if (config.isEnum()) {
                imports.add(EnumFilter.class.getName());
                // Only add enum import if it's not in the same package
                String enumType = config.getFieldType();
                if (enumType != null && !enumType.isEmpty()) {
                    imports.add(enumType);
                }
            } else if (config.isCollection()) {
                imports.add("com.thy.fss.common.inmemory.filter.CollectionFilter");
                
                // Check if this is a model element type collection
                if (config.isModelElementType()) {
                    // Add imports for model type element filter
                    String elementFilterQualifiedName = config.getElementFilterQualifiedName();
                    if (elementFilterQualifiedName != null && !elementFilterQualifiedName.isEmpty()) {
                        imports.add(elementFilterQualifiedName);
                        imports.add(elementFilterQualifiedName + DESERIALIZER_SUFFIX);
                        debugLog("Added model element filter import: " + elementFilterQualifiedName);
                        debugLog("Added model element filter deserializer import: " + elementFilterQualifiedName + DESERIALIZER_SUFFIX);
                    }
                    
                    // Add import for the element type itself if needed
                    String elementType = config.getElementType();
                    if (elementType != null && !elementType.isEmpty() && elementType.contains(".")) {
                        imports.add(elementType);
                        debugLog("Added model element type import: " + elementType);
                    }
                } else {
                    // Add imports for basic element filter types
                    String elementType = config.getElementType();
                    if (elementType != null && !elementType.isEmpty() && !isCollectionShapedType(elementType)) {
                        String elementFilterType = getFilterTypeForElementType(elementType);
                        String simpleElementType = getSimpleClassName(elementType);
                        
                        // Check if element is an enum type
                        boolean isEnumElement = false;
                        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();
                        javax.lang.model.element.TypeElement typeElement = elementUtils.getTypeElement(elementType);
                        if (typeElement != null && typeElement.getKind() == javax.lang.model.element.ElementKind.ENUM) {
                            isEnumElement = true;
                            imports.add(EnumFilter.class.getName());
                            // Add the enum type import
                            imports.add(elementType);
                            debugLog("Added EnumFilter import for enum element type: " + elementType);
                        }
                        
                        // Add the element filter import for non-enum types
                        if (!isEnumElement) {
                            if (elementFilterType.equals("StringFilter")) {
                                imports.add(StringFilter.class.getName());
                            } else if (elementFilterType.equals("IntegerFilter")) {
                                imports.add(IntegerFilter.class.getName());
                            } else if (elementFilterType.equals("LongFilter")) {
                                imports.add(LongFilter.class.getName());
                            } else if (elementFilterType.equals("DoubleFilter")) {
                                imports.add(DoubleFilter.class.getName());
                            } else if (elementFilterType.equals("BooleanFilter")) {
                                imports.add("com.thy.fss.common.inmemory.filter.BooleanFilter");
                            } else if (elementFilterType.equals("LocalDateFilter")) {
                                imports.add(LocalDateFilter.class.getName());
                                imports.add(LocalDate.class.getName());
                            } else if (elementFilterType.equals("LocalDateTimeFilter")) {
                                imports.add(LocalDateTimeFilter.class.getName());
                                imports.add(LocalDateTime.class.getName());
                            } else if (elementFilterType.equals("InstantFilter")) {
                                imports.add(InstantFilter.class.getName());
                                imports.add(Instant.class.getName());
                            } else {
                                // For custom model or enum types, add the filter import
                                // Assume it's in the same package as the current filter
                                String packageName = config.getPackageName();
                                if (packageName != null && !packageName.isEmpty()) {
                                    imports.add(packageName + "." + elementFilterType);
                                }
                            }
                        }
                    }
                }
            } else if (config.isModel()) {
                String packageName = config.getPackageName();
                if (packageName != null && !packageName.isEmpty()) {
                    imports.add(packageName + "." + config.getFilterType());
                    imports.add(packageName + "." + config.getFilterType()+DESERIALIZER_SUFFIX);
                }
            }

            // Ensure the field type used in deserializeValue calls is always imported.
            // This covers enum types from other packages, custom types, and any type
            // that the generated code references via its simple name.
            String fieldType = config.getFieldType();
            if (fieldType != null && !fieldType.isEmpty() && fieldType.contains(".")
                    && !fieldType.startsWith("java.lang.")) {
                // For collection types, the element type is what gets used in deserializeValue
                if (config.isCollection()) {
                    String elementType = config.getElementType();
                    if (elementType != null && !elementType.isEmpty() && elementType.contains(".")
                            && !elementType.startsWith("java.lang.")) {
                        imports.add(elementType);
                    }
                } else if (!config.isModel()) {
                    imports.add(fieldType);
                }
            }

            // Unified import section: mirrors generateHandleNestedFilterPathMethod logic exactly.
            // Ensures that all classes referenced in handleNestedFilterPath are imported.
            if (config.isCollection() && config.getElementType() != null
                    && !isBasicElementType(config.getElementType())) {
                // Model type collection field - import entity, filter, and deserializer classes
                String elementType = config.getElementType();
                String elementFilterType = getFilterTypeForElementType(elementType);
                // Derive the element package from the fully qualified element type
                String elementPackage;
                int lastDot = elementType.lastIndexOf('.');
                if (lastDot > 0) {
                    elementPackage = elementType.substring(0, lastDot);
                } else {
                    elementPackage = config.getPackageName();
                }
                if (elementPackage != null && !elementPackage.isEmpty()) {
                    // Entity class import (fully qualified element type)
                    imports.add(elementType);
                    // Filter class import
                    imports.add(elementPackage + "." + elementFilterType);
                    // Deserializer class import
                    imports.add(elementPackage + "." + elementFilterType + DESERIALIZER_SUFFIX);
                }
            } else if (config.isModel()) {
                // Model field - ensure filter and deserializer imports even when packageName is null
                String packageName = config.getPackageName();
                if (packageName == null || packageName.isEmpty()) {
                    // Derive package from fieldType
                    String modelFieldType = config.getFieldType();
                    if (modelFieldType != null && modelFieldType.contains(".")) {
                        int lastDot = modelFieldType.lastIndexOf('.');
                        packageName = modelFieldType.substring(0, lastDot);
                    }
                }
                if (packageName != null && !packageName.isEmpty()) {
                    String filterType = config.getFilterType();
                    if (filterType != null && !filterType.isEmpty()) {
                        imports.add(packageName + "." + filterType);
                        imports.add(packageName + "." + filterType + DESERIALIZER_SUFFIX);
                    }
                }
            }
        }

        return imports;
    }

    /**
     * Generates a constant for temporal field configuration.
     */
    protected void generateTemporalFieldConstant(PrintWriter out, FilterFieldConfig config) {
        String safeFieldName = toSafeConstantName(config.getFieldName());
        if (config.hasCustomDateTimeFormat()) {
            String pattern = config.getEffectiveDateTimePattern();
            out.println("    // Generated based on build-time analysis of " + config.getFieldName() + " field");
            out.println("    private static final " + DateTimeFormatter.class.getSimpleName() + " FORMATTER_" + safeFieldName
                    + " = " + DateTimeFormatter.class.getSimpleName() + ".ofPattern(\"" + pattern + "\");");
        } else {
            String defaultPattern = getDefaultPatternForType(config.getFieldType());
            out.println("    // Using default format from FilterConstants for " + config.getFieldName());
            out.println("    private static final " + DateTimeFormatter.class.getSimpleName() + " FORMATTER_" + safeFieldName
                    + " = " + DateTimeFormatter.class.getSimpleName() + ".ofPattern(" + defaultPattern + ");");
        }
    }

    /**
     * Gets the default pattern constant for a temporal type.
     */
    private String getDefaultPatternForType(String fieldType) {
        if (fieldType.contains(LocalDateTime.class.getSimpleName())) {
            return FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_TIME_PATTERN";
        } else if (fieldType.contains(LocalDate.class.getSimpleName())) {
            return FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_PATTERN";
        } else if (fieldType.contains(Instant.class.getSimpleName())) {
            return FilterConstants.class.getSimpleName() + ".DEFAULT_INSTANT_PATTERN";
        }
        return FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_TIME_PATTERN";
    }

    /**
     * Generates switch cases for all field deserialization. Handles both nested
     * filters (MetamodelFilter fields) and basic filter fields. All fields are
     * deserialized using Jackson's context for proper recursive handling.
     */
    protected void generateFieldDeserializationSwitchCases(PrintWriter out, List<FilterFieldConfig> fieldConfigs) {
        for (FilterFieldConfig config : fieldConfigs) {
            String filterType = config.getFilterType();
            String fieldName = config.getFieldName();
            String jsonPropertyName = getJsonPropertyName(config);

            // All filter fields are deserialized using Jackson's context
            // This allows Jackson to use the appropriate deserializer for each filter type
            debugLog("Generating switch case for field: " + fieldName + " (type: " + filterType + ")");

            out.println("                case \"" + jsonPropertyName + EXP_8);
            out.println(EXP_12 + JsonToken.class.getSimpleName() + ".START_OBJECT) {");
            out.println("                        // Deserialize filter using Jackson's context");

            // Handle CollectionFilter with generic type parameter
            if (config.isCollection() && config.getElementType() != null && !config.getElementType().isEmpty()) {
                // For CollectionFilter, we need to use TypeReference for proper generic deserialization
                String elementType = config.getElementType();
                out.println("                        // Note: CollectionFilter deserialization uses raw type due to type erasure");
                out.println("                        @SuppressWarnings(\"unchecked\")");
                out.println(EXP_4 + filterType + "<" + elementType + "> fieldFilter = (" + filterType + "<" + elementType + ">) ctxt.readValue(p, " + filterType + CLASS_EXP);
            } else {
                out.println(EXP_4 + filterType + " fieldFilter = ctxt.readValue(p, " + filterType + CLASS_EXP);
            }

            out.println("                        filter.set" + capitalize(fieldName) + "(fieldFilter);");
            out.println(EXP_3);
            out.println(EXP_5);
        }
    }

    /**
     * Checks if a filter type is a basic filter type (not a nested model
     * filter).
     */
    private boolean isBasicFilterType(String filterType) {
        return filterType.equals("StringFilter")
                || filterType.equals(INTEGER_FILTER)
                || filterType.equals(LONG_FILTER)
                || filterType.equals(DOUBLE_FILTER)
                || filterType.equals("BooleanFilter")
                || filterType.equals("LocalDateTimeFilter")
                || filterType.equals("LocalDateFilter")
                || filterType.equals("InstantFilter")
                || filterType.equals("EnumFilter")
                || filterType.equals("CollectionFilter");
    }

    /**
     * Gets the JSON property name for a field config. Returns the @JsonProperty
     * value if present, otherwise returns the field name.
     */
    private String getJsonPropertyName(FilterFieldConfig config) {
        if (config.hasJacksonAnnotations()) {
            for (AnnotationInfo annotation : config.getJacksonAnnotations()) {
                if ("JsonProperty".equals(annotation.getAnnotationType())
                        || annotation.getAnnotationType().endsWith(".JsonProperty")) {
                    java.util.Map<String, Object> params = annotation.getParameters();
                    if (params != null && params.containsKey(VALUE)) {
                        String value = String.valueOf(params.get(VALUE));
                        // Remove quotes if present
                        return value.replaceAll("^\"|\"$", "");
                    }
                }
            }
        }
        return config.getFieldName();
    }

    /**
     * Generates switch cases for field deserialization based on filter type.
     * Only generates cases for basic filter types (not nested model filters).
     */
    protected void generateSwitchCases(PrintWriter out, List<FilterFieldConfig> fieldConfigs) {
        if (fieldConfigs.isEmpty()) {
            return;
        }

        // Filter out nested filter fields - only process basic filter types
        List<FilterFieldConfig> basicFilterConfigs = fieldConfigs.stream()
                .filter(config -> {
                    String filterType = config.getFilterType();
                    return filterType == null || !filterType.endsWith("Filter") || isBasicFilterType(filterType);
                })
                .toList();

        if (basicFilterConfigs.isEmpty()) {
            return;
        }

        // Check if we have multiple field types - if so, generate all common cases
        boolean hasMultipleTypes = basicFilterConfigs.stream()
                .map(config -> {
                    if (config.isTemporal()) {
                        return "temporal";
                    }
                    if (config.isString()) {
                        return "string";
                    }
                    if (config.isNumeric()) {
                        return "numeric";
                    }
                    if (config.isEnum()) {
                        return "enum";
                    }
                    if (config.isCollection()) {
                        return "collection";
                    }
                    return "unknown";
                })
                .distinct()
                .count() > 1;

        if (hasMultipleTypes) {
            // Generate all common switch cases for mixed field types
            generateMixedFieldTypeSwitchCases(out, basicFilterConfigs);
        } else {
            // Single field type - use existing logic
            FilterFieldConfig primaryConfig = basicFilterConfigs.getFirst();

            if (primaryConfig.isTemporal()) {
                generateTemporalSwitchCases(out, primaryConfig);
            } else if (primaryConfig.isString()) {
                generateStringSwitchCases(out);
            } else if (primaryConfig.isNumeric()) {
                generateNumericSwitchCases(out, primaryConfig);
            } else if (primaryConfig.isEnum()) {
                generateEnumSwitchCases(out, primaryConfig);
            } else if (primaryConfig.isCollection()) {
                generateCollectionSwitchCases(out, primaryConfig);
            } else {
                logError("Unknown filter type for field: " + primaryConfig.getFieldName());
            }
        }
    }

    /**
     * Generates switch cases for mixed field types (complex filters).
     */
    private void generateMixedFieldTypeSwitchCases(PrintWriter out, List<FilterFieldConfig> fieldConfigs) {
        // Generate common base cases that all filters support
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", "p.getText()");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", "p.getText()");
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", "String", "p.getText()");
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", "String", "p.getText()");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");

        // Check for string fields and add string-specific cases
        if (fieldConfigs.stream().anyMatch(FilterFieldConfig::isString)) {
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_CONT", "setContains", "p.getText()");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_START", "setStartsWith", "p.getText()");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_END", "setEndsWith", "p.getText()");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_MATCH", "setMatches", "p.getText()");
            generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EMPTY", "setIsEmpty");
            generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEMPTY", "setIsNotEmpty");
            generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_BLANK", "setIsBlank");
            generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NBLANK", "setIsNotBlank");
        }

        // Check for temporal fields and add temporal-specific cases
        if (fieldConfigs.stream().anyMatch(FilterFieldConfig::isTemporal)) {
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_BEFORE", "setIsBefore", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_AFTER", "setIsAfter", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ON_OR_BEFORE", "setIsOnOrBefore", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ON_OR_AFTER", "setIsOnOrAfter", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_BEFORE", "setNotIsBefore", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_AFTER", "setNotIsAfter", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_ON_OR_BEFORE", "setNotIsOnOrBefore", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_ON_OR_AFTER", "setNotIsOnOrAfter", "parseLocalDateTime_createdAt(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LAST", "setLast", "com.thy.fss.common.inmemory.filter.TemporalPresetParser.parse(p.getText())");
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEXT", "setNext", "com.thy.fss.common.inmemory.filter.TemporalPresetParser.parse(p.getText())");
        }

        // Check for numeric fields and add numeric-specific cases
        if (fieldConfigs.stream().anyMatch(FilterFieldConfig::isNumeric)) {
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_GT", "setGreaterThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_GTE", "setGreaterOrEqualThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LT", "setLessThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LTE", "setLessOrEqualThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NGT", "setNotGreaterThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NGTE", "setNotGreaterOrEqualThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NLT", "setNotLessThan", GET_INT_VALUE);
            generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NLTE", "setNotLessOrEqualThan", GET_INT_VALUE);
        }
    }

    /**
     * Generates switch cases for temporal filters (LocalDateTime, LocalDate,
     * Instant).
     */
    private void generateTemporalSwitchCases(PrintWriter out, FilterFieldConfig config) {
        String parseMethod = getTemporalParseMethod(config);
        String elementType = getTemporalElementType(config.getFieldType());

        // Base filter fields (equals, notEquals, in, notIn, isNull, isNotNull)
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", parseMethod);
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", elementType, parseMethod);
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", elementType, parseMethod);
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");

        // Temporal-specific fields (before, after, onOrBefore, onOrAfter)
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_BEFORE", "setIsBefore", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_AFTER", "setIsAfter", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ON_OR_BEFORE", "setIsOnOrBefore", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ON_OR_AFTER", "setIsOnOrAfter", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_BEFORE", "setNotIsBefore", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_AFTER", "setNotIsAfter", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_ON_OR_BEFORE", "setNotIsOnOrBefore", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NOT_ON_OR_AFTER", "setNotIsOnOrAfter", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LAST", "setLast", "com.thy.fss.common.inmemory.filter.TemporalPresetParser.parse(p.getText())");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEXT", "setNext", "com.thy.fss.common.inmemory.filter.TemporalPresetParser.parse(p.getText())");
    }

    /**
     * Generates switch cases for string filters. Handles all StringFilter
     * operations including string-specific fields and Jackson annotations.
     */
    private void generateStringSwitchCases(PrintWriter out) {
        // Base filter fields (equals, notEquals, in, notIn, isNull, isNotNull)
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", "p.getText()");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", "p.getText()");
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", String.class.getSimpleName(), "p.getText()");
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", String.class.getSimpleName(), "p.getText()");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");

        // String-specific filter fields
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_CONT", "setContains", "p.getText()");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_START", "setStartsWith", "p.getText()");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_END", "setEndsWith", "p.getText()");
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_MATCH", "setMatches", "p.getText()");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EMPTY", "setIsEmpty");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEMPTY", "setIsNotEmpty");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_BLANK", "setIsBlank");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NBLANK", "setIsNotBlank");
    }

    /**
     * Generates switch cases for numeric filters.
     */
    private void generateNumericSwitchCases(PrintWriter out, FilterFieldConfig config) {
        String parseMethod = getNumericParseMethod(config.getFilterType());
        String elementType = getNumericElementType(config.getFilterType());

        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_GT", "setGreaterThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_GTE", "setGreaterOrEqualThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LT", "setLessThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_LTE", "setLessOrEqualThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NGT", "setNotGreaterThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NGTE", "setNotGreaterOrEqualThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NLT", "setNotLessThan", parseMethod);
        generateNumericSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NLTE", "setNotLessOrEqualThan", parseMethod);
        generateNumericArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", elementType, parseMethod);
        generateNumericArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", elementType, parseMethod);
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");
    }

    /**
     * Generates switch cases for enum filters.
     */
    private void generateEnumSwitchCases(PrintWriter out, FilterFieldConfig config) {
        String enumSimpleName = getSimpleClassName(config.getFieldType());
        String parseMethod = PARSE + enumSimpleName + "(p.getText())";

        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", parseMethod);
        generateSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", parseMethod);
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", enumSimpleName, parseMethod);
        generateArraySwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", enumSimpleName, parseMethod);
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");
    }

    /**
     * Generates switch cases for collection filter operators in Jackson deserializers.
     * Handles both collection-specific operators (cont, any, all, none, empty, nempty)
     * and inherited base operators (eq, neq, in, nin, isn, isnn).
     * 
     * <p>This method generates JSON deserialization code for CollectionFilter fields, supporting:
     * <ul>
     *   <li><b>Base operators</b> (inherited from Filter): eq, neq, in, nin, isn, isnn</li>
     *   <li><b>Collection-specific operators</b>: cont (contains), empty, nempty (not empty)</li>
     *   <li><b>Nested filter operators</b>: any, all, none (handled separately via path parsing)</li>
     * </ul>
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * // For a CollectionFilter<String> field named "tags"
     * case "cont" -> {
     *     if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
     *         filter.setCollectionContains(p.getText());
     *     }
     * }
     * case "empty" -> {
     *     if (p.getCurrentToken() == JsonToken.VALUE_TRUE || 
     *         p.getCurrentToken() == JsonToken.VALUE_FALSE) {
     *         filter.setIsEmpty(p.getBooleanValue());
     *     }
     * }
     * case "eq" -> {
     *     if (p.getCurrentToken() == JsonToken.START_ARRAY) {
     *         List<String> values = new ArrayList<>();
     *         while (p.nextToken() != JsonToken.END_ARRAY) {
     *             values.add(p.getText());
     *         }
     *         filter.setEquals(values);
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Note:</b> Nested filter operators (any, all, none) require special path parsing
     * and are not generated as simple switch cases. They are handled by 
     * {@link #generateNestedFilterBinding(PrintWriter, String, String, String, String)}.
     * 
     * @param out the PrintWriter to write generated code to
     * @param config the field configuration containing element type and field metadata
     * @see #generateNestedFilterBinding(PrintWriter, String, String, String, String)
     * @see CollectionFilter
     */
    private void generateCollectionSwitchCases(PrintWriter out, FilterFieldConfig config) {
        String elementType = config.getElementType();
        if (elementType == null || elementType.isEmpty()) {
            elementType = "Object";
        }
        
        // Base filter operators (inherited from Filter class)
        // Note: For collections, eq/neq/in/nin work on the collection itself, not elements
        generateCollectionBaseSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EQ", "setEquals", elementType);
        generateCollectionBaseSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEQ", "setNotEquals", elementType);
        generateCollectionInSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_IN", "setIn", elementType);
        generateCollectionInSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NIN", "setNotIn", elementType);
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISN", "setIsNull");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_ISNN", "setIsNotNull");
        
        // Collection-specific operators
        generateCollectionContainsSwitchCase(out, elementType);
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_EMPTY", "setIsEmpty");
        generateBooleanSwitchCase(out, FilterConstants.class.getSimpleName() + ".FIELD_NEMPTY", "setIsNotEmpty");
        
        // Nested filter operators (any, all, none) are handled separately
        // These require special path parsing and are not simple switch cases
    }
    
    /**
     * Generates binding code for nested filter operators (any, all, none) in web query parameter binding.
     * Creates nested filter instances and recursively binds their parameters for collection filtering.
     * 
     * <p>Nested filter paths follow the format: {@code field.any.operator}, {@code field.all.operator}, 
     * {@code field.none.operator}
     * 
     * <p><b>Query Parameter Examples:</b>
     * <ul>
     *   <li>{@code ?tags.any.cont=work} - Any tag contains "work"</li>
     *   <li>{@code ?numbers.all.gte=0} - All numbers >= 0</li>
     *   <li>{@code ?tags.none.eq=forbidden} - No tags equal "forbidden"</li>
     *   <li>{@code ?prices.any.gt=100} - Any price greater than 100</li>
     * </ul>
     * 
     * <p>This method generates code that:
     * <ol>
     *   <li>Parses nested filter paths (field.any.operator, field.all.operator, field.none.operator)</li>
     *   <li>Creates appropriate filter instance based on element type (StringFilter, IntegerFilter, etc.)</li>
     *   <li>Binds element filter operators to nested filter instance</li>
     *   <li>Sets nested filter on collection filter (setCollectionAny, setCollectionAll, setCollectionNone)</li>
     * </ol>
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * // For query parameter: ?tags.any.cont=work
     * case "tags.any.cont" -> {
     *     // Ensure collection filter is initialized
     *     if (filter.getTags() == null) {
     *         filter.setTags(new CollectionFilter<>());
     *     }
     *     
     *     try {
     *         // Get or create the nested element filter
     *         StringFilter elementFilter = (StringFilter) filter.getTags().getCollectionAny();
     *         if (elementFilter == null) {
     *             elementFilter = new StringFilter();
     *             filter.getTags().setCollectionAny(elementFilter);
     *         }
     *         
     *         // Bind the element operator to the element filter
     *         String value = deserializer.deserializeValue(paramValue, String.class, 
     *                                                      registry.getConfigForType(String.class));
     *         elementFilter.setContains(value);
     *     } catch (Exception e) {
     *         throw new IllegalArgumentException(
     *             "Cannot bind nested filter operator 'tags.any.cont' with value '" + 
     *             paramValue + "': " + e.getMessage(), e);
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Supported Element Operators:</b>
     * <ul>
     *   <li>String: eq, neq, cont, start, end, match</li>
     *   <li>Numeric: eq, neq, gt, gte, lt, lte</li>
     *   <li>Temporal: eq, neq, before, after, onOrBefore, onOrAfter</li>
     * </ul>
     * 
     * @param out the PrintWriter to write generated code to
     * @param fieldName the name of the collection filter field (e.g., "tags", "numbers")
     * @param elementType the type of elements in the collection (e.g., "String", "Integer", "LocalDate")
     * @param nestedOperator the nested operator ("any", "all", or "none")
     * @param elementOperator the operator to apply to elements (e.g., "cont", "eq", "gt", "before")
     * @throws IllegalArgumentException if nestedOperator is not "any", "all", or "none"
     * @see #generateElementOperatorBinding(PrintWriter, String, String)
     * @see #getFilterTypeForElementType(String)
     * @see CollectionFilter#setCollectionAny(Filter)
     * @see CollectionFilter#setCollectionAll(Filter)
     * @see CollectionFilter#setCollectionNone(Filter)
     */
    private void generateNestedFilterBinding(PrintWriter out, String fieldName, String elementType, 
                                            String nestedOperator, String elementOperator) {
        if (elementType == null || elementType.isEmpty()) {
            elementType = "Object";
        }
        
        // Check if element type is a model type
        boolean isModelElementType = !isBasicElementType(elementType);
        
        // Determine the appropriate filter type based on element type
        String elementFilterType = getFilterTypeForElementType(elementType);
        
        // Determine the appropriate setter method based on the nested operator
        String collectionSetterMethod = switch (nestedOperator) {
            case FilterConstants.FIELD_ANY -> "setCollectionAny";
            case FilterConstants.FIELD_ALL -> "setCollectionAll";
            case FilterConstants.FIELD_NONE -> "setCollectionNone";
            default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOperator);
        };
        
        // Determine the appropriate getter method based on the nested operator
        String collectionGetterMethod = switch (nestedOperator) {
            case FilterConstants.FIELD_ANY -> "getCollectionAny";
            case FilterConstants.FIELD_ALL -> "getCollectionAll";
            case FilterConstants.FIELD_NONE -> "getCollectionNone";
            default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOperator);
        };
        
        // Generate the case for this specific field.nestedOp.elementOp combination
        String capitalizedFieldName = capitalize(fieldName);
        String getterMethod = FILTER_GETTER_PREFIX + capitalizedFieldName;
        String setterFieldMethod = FILTER_SETTER_PREFIX + capitalizedFieldName;
        
        out.println(FIELD_CASE_PREFIX + fieldName + "." + nestedOperator + "." + elementOperator + EXP_8);
        out.println("                // Ensure collection filter is initialized");
        out.println("                if (filter." + getterMethod + "() == null) {");
        out.println("                    filter." + setterFieldMethod + "(new CollectionFilter<>());");
        out.println(EXP_5);
        out.println("                ");
        out.println("                try {");
        out.println("                    // Get or create the nested element filter (FilterBase<E>)");
        out.println("                    com.thy.fss.common.inmemory.filter.FilterBase<" + getSimpleClassName(elementType) + "> elementFilterBase = filter." + getterMethod + "()." + collectionGetterMethod + "();");
        out.println("                    " + elementFilterType + " elementFilter;");
        out.println("                    ");
        out.println("                    // Type-safe cast from FilterBase to concrete filter type");
        out.println("                    if (elementFilterBase instanceof " + elementFilterType + ") {");
        out.println("                        elementFilter = (" + elementFilterType + ") elementFilterBase;");
        out.println("                    } else if (elementFilterBase == null) {");
        out.println("                        try {");
        out.println("                            elementFilter = new " + elementFilterType + "();");
        out.println("                        } catch (Exception instantiationEx) {");
        out.println("                            throw new IllegalArgumentException(");
        out.println("                                \"Cannot create instance of element filter '\" + \"" + elementFilterType + "\" + ");
        out.println("                                \"' for collection field '\" + \"" + fieldName + "\" + \"': \" + instantiationEx.getMessage() + \". \" +");
        out.println("                                \"Ensure the filter class has a public no-argument constructor.\",");
        out.println("                                instantiationEx");
        out.println("                            );");
        out.println("                        }");
        out.println("                        filter." + getterMethod + "()." + collectionSetterMethod + "(elementFilter);");
        out.println("                    } else {");
        out.println("                        throw new IllegalStateException(\"Unexpected filter type: \" + elementFilterBase.getClass().getName());");
        out.println("                    }");
        out.println("                    ");
        
        if (isModelElementType) {
            // For model types, delegate to existing nested model binding
            out.println("                    // Model type element - delegate to nested filter's deserializer");
            out.println("                    // Extract remaining path after collection operator");
            out.println("                    String remainingPath = \"" + elementOperator + "\";");
            out.println("                    ");
            out.println("                    // Delegate to element filter's deserializer bindParameter method");
            out.println("                    " + elementFilterType + "Deserializer.bindParameter(");
            out.println("                        elementFilter,");
            out.println("                        remainingPath,");
            out.println("                        paramValue,");
            out.println("                        deserializer,");
            out.println("                        collectionHandler,");
            out.println("                        registry");
            out.println("                    );");
        } else {
            // For basic types, use existing element operator binding
            out.println("                    // Basic type element - bind operator directly");
            generateElementOperatorBinding(out, elementType, elementOperator);
        }
        
        out.println("                } catch (Exception e) {");
        out.println("                    throw new IllegalArgumentException(");
        out.println("                        \"Cannot bind nested filter operator '\" + \"" + fieldName + "." + nestedOperator + "." + elementOperator + "\" + ");
        out.println("                        \"' with value '\" + paramValue + \"': \" + e.getMessage(),");
        out.println("                        e");
        out.println("                    );");
        out.println(EXP_5);
        out.println(EXP_6);
    }
    
    /**
     * Generates binding code for nested filter operators (any, all, none) with model type elements.
     * Delegates to the element filter's deserializer bindParameter method for multi-level path handling.
     * 
     * <p>This method generates code that handles model type collection filtering by:
     * <ol>
     *   <li>Creating or retrieving the element filter instance (FilterBase<E>)</li>
     *   <li>Extracting the remaining path after the collection operator</li>
     *   <li>Delegating to the element filter's deserializer bindParameter method</li>
     * </ol>
     * 
     * <p><b>Query Parameter Examples:</b>
     * <ul>
     *   <li>{@code ?users.any.name.eq=John} - Any user has name "John"</li>
     *   <li>{@code ?users.all.active.eq=true} - All users are active</li>
     *   <li>{@code ?orders.any.customer.status.eq=ACTIVE} - Any order has active customer</li>
     *   <li>{@code ?users.any.address.city.eq=Istanbul} - Any user's address city is Istanbul</li>
     * </ul>
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * // For query parameter: ?users.any.name.eq=John
     * // This generates a default case that handles any path starting with "users.any."
     * default -> {
     *     if (mappedPath.startsWith("users.any.")) {
     *         // Ensure collection filter is initialized
     *         if (filter.getUsers() == null) {
     *             filter.setUsers(new CollectionFilter<>());
     *         }
     *         
     *         try {
     *             // Get or create the nested element filter (FilterBase<User>)
     *             com.thy.fss.common.inmemory.filter.FilterBase<User> elementFilterBase = 
     *                 filter.getUsers().getCollectionAny();
     *             UserFilter elementFilter;
     *             
     *             // Type-safe cast from FilterBase to concrete filter type
     *             if (elementFilterBase instanceof UserFilter) {
     *                 elementFilter = (UserFilter) elementFilterBase;
     *             } else if (elementFilterBase == null) {
     *                 elementFilter = new UserFilter();
     *                 filter.getUsers().setCollectionAny(elementFilter);
     *             } else {
     *                 throw new IllegalStateException("Unexpected filter type: " + 
     *                     elementFilterBase.getClass().getName());
     *             }
     *             
     *             // Extract remaining path after collection operator
     *             String remainingPath = mappedPath.substring("users.any.".length());
     *             
     *             // Delegate to element filter's deserializer bindParameter method
     *             UserFilterDeserializer.bindParameter(
     *                 elementFilter,
     *                 remainingPath,
     *                 paramValue,
     *                 deserializer,
     *                 collectionHandler,
     *                 registry
     *             );
     *         } catch (Exception e) {
     *             throw new IllegalArgumentException(
     *                 "Cannot bind nested filter path '" + mappedPath + 
     *                 "' with value '" + paramValue + "': " + e.getMessage(), e);
     *         }
     *     }
     * }
     * }</pre>
     * 
     * @param out the PrintWriter to write generated code to
     * @param fieldName the name of the collection filter field (e.g., "users", "orders")
     * @param elementType the type of elements in the collection (e.g., "User", "Order")
     * @param nestedOperator the nested operator ("any", "all", or "none")
     * @throws IllegalArgumentException if nestedOperator is not "any", "all", or "none"
     * @see #generateNestedFilterBinding(PrintWriter, String, String, String, String)
     * @see #generateHandleNestedFilterPathMethod(PrintWriter, String, List)
     */
    private void generateModelTypeNestedFilterBinding(PrintWriter out, String fieldName, 
                                                      String elementType, String nestedOperator) {
        // Determine the appropriate filter type based on element type
        String elementFilterType = getFilterTypeForElementType(elementType);
        
        // Determine the appropriate setter method based on the nested operator
        String collectionSetterMethod = switch (nestedOperator) {
            case FilterConstants.FIELD_ANY -> "setCollectionAny";
            case FilterConstants.FIELD_ALL -> "setCollectionAll";
            case FilterConstants.FIELD_NONE -> "setCollectionNone";
            default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOperator);
        };
        
        // Determine the appropriate getter method based on the nested operator
        String collectionGetterMethod = switch (nestedOperator) {
            case FilterConstants.FIELD_ANY -> "getCollectionAny";
            case FilterConstants.FIELD_ALL -> "getCollectionAll";
            case FilterConstants.FIELD_NONE -> "getCollectionNone";
            default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOperator);
        };
        
        String capitalizedFieldName = capitalize(fieldName);
        String getterMethod = FILTER_GETTER_PREFIX + capitalizedFieldName;
        String setterFieldMethod = FILTER_SETTER_PREFIX + capitalizedFieldName;
        String pathPrefix = fieldName + "." + nestedOperator + ".";
        
        // Generate a default case that handles any path starting with this prefix
        out.println("            default -> {");
        out.println("                // Handle model type collection nested filter paths");
        out.println("                if (mappedPath.startsWith(\"" + pathPrefix + "\")) {");
        out.println("                    // Ensure collection filter is initialized");
        out.println("                    if (filter." + getterMethod + "() == null) {");
        out.println("                        filter." + setterFieldMethod + "(new CollectionFilter<>());");
        out.println("                    }");
        out.println("                    ");
        out.println("                    try {");
        out.println("                        // Get or create the nested element filter (FilterBase<E>)");
        out.println("                        com.thy.fss.common.inmemory.filter.FilterBase<" + getSimpleClassName(elementType) + "> elementFilterBase = filter." + getterMethod + "()." + collectionGetterMethod + "();");
        out.println("                        " + elementFilterType + " elementFilter;");
        out.println("                        ");
        out.println("                        // Type-safe cast from FilterBase to concrete filter type");
        out.println("                        if (elementFilterBase instanceof " + elementFilterType + ") {");
        out.println("                            elementFilter = (" + elementFilterType + ") elementFilterBase;");
        out.println("                        } else if (elementFilterBase == null) {");
        out.println("                            try {");
        out.println("                                elementFilter = new " + elementFilterType + "();");
        out.println("                            } catch (Exception instantiationEx) {");
        out.println("                                throw new IllegalArgumentException(");
        out.println("                                    \"Cannot create instance of element filter '\" + \"" + elementFilterType + "\" + ");
        out.println("                                    \"' for collection field '\" + \"" + fieldName + "\" + \"': \" + instantiationEx.getMessage() + \". \" +");
        out.println("                                    \"Ensure the filter class has a public no-argument constructor.\",");
        out.println("                                    instantiationEx");
        out.println("                                );");
        out.println("                            }");
        out.println("                            filter." + getterMethod + "()." + collectionSetterMethod + "(elementFilter);");
        out.println("                        } else {");
        out.println("                            throw new IllegalStateException(\"Unexpected filter type: \" + elementFilterBase.getClass().getName());");
        out.println("                        }");
        out.println("                        ");
        out.println("                        // Extract remaining path after collection operator");
        out.println("                        String remainingPath = mappedPath.substring(\"" + pathPrefix + "\".length());");
        out.println("                        ");
        out.println("                        // Delegate to element filter's deserializer bindParameter method");
        out.println("                        " + elementFilterType + "Deserializer.bindParameter(");
        out.println("                            elementFilter,");
        out.println("                            remainingPath,");
        out.println("                            paramValue,");
        out.println("                            deserializer,");
        out.println("                            collectionHandler,");
        out.println("                            registry");
        out.println("                        );");
        out.println("                    } catch (Exception e) {");
        out.println("                        throw new IllegalArgumentException(");
        out.println("                            \"Cannot bind nested filter path '\" + mappedPath + ");
        out.println("                            \"' with value '\" + paramValue + \"': \" + e.getMessage(),");
        out.println("                            e");
        out.println("                        );");
        out.println("                    }");
        out.println("                }");
        out.println(EXP_6);
    }
    
    /**
     * Generates the code to bind an element operator to an element filter within nested collection filters.
     * Writes type-safe deserialization and binding code directly to the PrintWriter.
     * 
     * <p>This method generates code that deserializes a query parameter value to the appropriate
     * element type and binds it to the element filter using the correct setter method.
     * 
     * <p><b>Generated Code Examples:</b>
     * <pre>{@code
     * // For String element with "cont" operator:
     * String value = deserializer.deserializeValue(paramValue, String.class, 
     *                                              registry.getConfigForType(String.class));
     * elementFilter.setContains(value);
     * 
     * // For Integer element with "gt" operator:
     * Integer value = deserializer.deserializeValue(paramValue, Integer.class, 
     *                                               registry.getConfigForType(Integer.class));
     * elementFilter.setGreaterThan(value);
     * 
     * // For LocalDate element with "before" operator:
     * LocalDate value = deserializer.deserializeValue(paramValue, LocalDate.class, 
     *                                                 registry.getConfigForType(LocalDate.class));
     * elementFilter.setIsBefore(value);
     * 
     * // For String element with "in" operator (collection):
     * java.util.List<String> values = collectionHandler.parseCommaSeparatedValues(
     *     paramValue, String.class, registry.getConfigForType(String.class));
     * elementFilter.setIn(values);
     * }</pre>
     * 
     * <p><b>Operator to Setter Method Mapping:</b>
     * <ul>
     *   <li>eq → setEquals</li>
     *   <li>neq → setNotEquals</li>
     *   <li>in → setIn (collection)</li>
     *   <li>nin → setNotIn (collection)</li>
     *   <li>cont → setContains</li>
     *   <li>start → setStartsWith</li>
     *   <li>end → setEndsWith</li>
     *   <li>match → setMatches</li>
     *   <li>gt → setGreaterThan</li>
     *   <li>gte → setGreaterOrEqualThan</li>
     *   <li>lt → setLessThan</li>
     *   <li>lte → setLessOrEqualThan</li>
     *   <li>before → setIsBefore</li>
     *   <li>after → setIsAfter</li>
     *   <li>onOrBefore → setIsOnOrBefore</li>
     *   <li>onOrAfter → setIsOnOrAfter</li>
    *   <li>last → setLast</li>
    *   <li>next → setNext</li>
     * </ul>
     * 
     * @param out the PrintWriter to write generated code to
     * @param elementType the type of elements in the collection (e.g., "String", "Integer", "LocalDate")
     * @param elementOperator the operator to apply (e.g., "cont", "eq", "gt", "before", "in", "nin")
     * @throws IllegalArgumentException if elementOperator is not recognized
     * @see #getSimpleClassName(String)
     * @see #isTemporalType(String)
     */
    private void generateElementOperatorBinding(PrintWriter out, String elementType, String elementOperator) {
        String simpleType = getSimpleClassName(elementType);
        boolean isTemporal = isTemporalType(elementType);
        boolean isPresetOperator = elementOperator.equals(FilterConstants.FIELD_LAST)
            || elementOperator.equals(FilterConstants.FIELD_NEXT);
        
        // Check if this is a collection operator (in, nin)
        boolean isCollectionOperator = elementOperator.equals(FilterConstants.FIELD_IN) || 
                                       elementOperator.equals(FilterConstants.FIELD_NIN);
        
        // Determine the setter method based on the operator
        String setterMethod = switch (elementOperator) {
            case FilterConstants.FIELD_EQ -> "setEquals";
            case FilterConstants.FIELD_NEQ -> "setNotEquals";
            case FilterConstants.FIELD_IN -> "setIn";
            case FilterConstants.FIELD_NIN -> "setNotIn";
            case FilterConstants.FIELD_CONT -> "setContains";
            case FilterConstants.FIELD_START -> "setStartsWith";
            case FilterConstants.FIELD_END -> "setEndsWith";
            case FilterConstants.FIELD_MATCH -> "setMatches";
            case FilterConstants.FIELD_GT -> "setGreaterThan";
            case FilterConstants.FIELD_GTE -> "setGreaterOrEqualThan";
            case FilterConstants.FIELD_LT -> "setLessThan";
            case FilterConstants.FIELD_LTE -> "setLessOrEqualThan";
            case FilterConstants.FIELD_NGT -> "setNotGreaterThan";
            case FilterConstants.FIELD_NLT -> "setNotLessThan";
            case FilterConstants.FIELD_NGTE -> "setNotGreaterOrEqualThan";
            case FilterConstants.FIELD_NLTE -> "setNotLessOrEqualThan";
            case FilterConstants.FIELD_BEFORE -> "setIsBefore";
            case FilterConstants.FIELD_AFTER -> "setIsAfter";
            case FilterConstants.FIELD_ON_OR_BEFORE -> "setIsOnOrBefore";
            case FilterConstants.FIELD_ON_OR_AFTER -> "setIsOnOrAfter";
            case FilterConstants.FIELD_NOT_BEFORE -> "setNotIsBefore";
            case FilterConstants.FIELD_NOT_AFTER -> "setNotIsAfter";
            case FilterConstants.FIELD_NOT_ON_OR_BEFORE -> "setNotIsOnOrBefore";
            case FilterConstants.FIELD_NOT_ON_OR_AFTER -> "setNotIsOnOrAfter";
            case FilterConstants.FIELD_LAST -> "setLast";
            case FilterConstants.FIELD_NEXT -> "setNext";
            default -> throw new IllegalArgumentException("Unknown element operator: " + elementOperator);
        };
        
        // Generate the deserialization code based on element type and operator
        if (isCollectionOperator) {
            // For collection operators (in, nin), parse comma-separated values
            if (isTemporal) {
                // For temporal types, we would need a custom parse method for each element
                // For now, use the collectionHandler which will delegate to deserializer
                out.println("                    java.util.List<" + simpleType + "> values = collectionHandler.parseCommaSeparatedValues(");
                out.println("                        paramValue, " + simpleType + ".class,");
                out.println("                        registry.getConfigForType(" + simpleType + ".class)");
                out.println("                    );");
            } else {
                out.println("                    java.util.List<" + simpleType + "> values = collectionHandler.parseCommaSeparatedValues(");
                out.println("                        paramValue, " + simpleType + ".class,");
                out.println("                        registry.getConfigForType(" + simpleType + ".class)");
                out.println("                    );");
            }
            out.println("                    elementFilter." + setterMethod + "(values);");
        } else if (isPresetOperator) {
            out.println("                    com.thy.fss.common.inmemory.filter.TemporalPreset value = ");
            out.println("                        deserializer.deserializeTemporalPreset(paramValue);");
            out.println("                    elementFilter." + setterMethod + "(value);");
        } else {
            // For single value operators
            String deserializationCode;
            if (isTemporal) {
                // For temporal types, we would need a custom parse method
                // For now, use the deserializer
                deserializationCode = "deserializer.deserializeValue(paramValue, " + simpleType + ".class, registry.getConfigForType(" + simpleType + ".class))";
            } else {
                deserializationCode = "deserializer.deserializeValue(paramValue, " + simpleType + ".class, registry.getConfigForType(" + simpleType + ".class))";
            }
            
            out.println("                    " + simpleType + " value = " + deserializationCode + ";");
            out.println("                    elementFilter." + setterMethod + "(value);");
        }
    }
    
    /**
     * Determines the appropriate filter type for a given element type in collection filters.
     * Maps element types to their corresponding filter classes for nested filter generation.
     * 
     * <p>This method is used when generating nested filter binding code for collection operators
     * (any, all, none). It determines which filter class should be instantiated based on the
     * collection's element type.
     * 
     * <p><b>Type Mapping Examples:</b>
     * <ul>
     *   <li>String → StringFilter</li>
     *   <li>Integer, int → IntegerFilter</li>
     *   <li>Long, long → LongFilter</li>
     *   <li>Double, double → DoubleFilter</li>
     *   <li>Boolean, boolean → BooleanFilter</li>
     *   <li>LocalDate → LocalDateFilter</li>
     *   <li>LocalDateTime → LocalDateTimeFilter</li>
     *   <li>Instant → InstantFilter</li>
     *   <li>OrderItemDetail → OrderItemDetailFilter (custom model)</li>
     *   <li>Status → StatusFilter (enum)</li>
     * </ul>
     * 
     * <p><b>Custom Types:</b> For custom model types or enum types not in the standard mapping,
     * this method assumes that the annotation processor has generated a filter for that type
     * by appending "Filter" to the type name. For example, a custom {@code OrderItemDetail}
     * class would map to {@code OrderItemDetailFilter}.
     * 
     * @param elementType the element type, either simple name (e.g., "String") or fully qualified 
     *                    (e.g., "java.lang.String", "com.example.OrderItemDetail")
     * @return the filter type name (e.g., "StringFilter", "IntegerFilter", "OrderItemDetailFilter")
     * @see #getSimpleClassName(String)
     */
    private String getFilterTypeForElementType(String elementType) {
        // Handle simple type names and fully qualified names
        String simpleType = getSimpleClassName(elementType);
        
        // Check if it's a basic type first
        String basicFilterType = switch (simpleType) {
            case "String" -> "StringFilter";
            case "Integer", "int" -> "IntegerFilter";
            case "Long", "long" -> "LongFilter";
            case "Double", "double" -> "DoubleFilter";
            case "Boolean", "boolean" -> "BooleanFilter";
            case "LocalDate" -> "LocalDateFilter";
            case "LocalDateTime" -> "LocalDateTimeFilter";
            case "Instant" -> "InstantFilter";
            default -> null;
        };
        
        if (basicFilterType != null) {
            return basicFilterType;
        }
        
        // Check if it's an enum type
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();
        javax.lang.model.element.TypeElement typeElement = elementUtils.getTypeElement(elementType);
        if (typeElement != null && typeElement.getKind() == javax.lang.model.element.ElementKind.ENUM) {
            // Enums use EnumFilter<E> instead of a custom filter class
            return "EnumFilter<" + simpleType + ">";
        }
        
        // For custom model types, use the type name + "Filter"
        // This assumes that the annotation processor has generated a filter for this type
        // For example: OrderItemDetail -> OrderItemDetailFilter
        return simpleType + "Filter";
    }
    
    /**
     * Generates a switch case for the collection contains operator in Jackson deserializers.
     * Produces code that deserializes a single element value and sets it on the CollectionFilter.
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * case "cont" -> {
     *     if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
     *         // Deserialize element using appropriate type deserializer
     *         // Element type: String
     *         filter.setCollectionContains(p.getText());
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Usage in JSON:</b>
     * <pre>{@code
     * {
     *   "tags": {
     *     "cont": "important"
     *   }
     * }
     * }</pre>
     * 
     * @param out the PrintWriter to write generated code to
     * @param elementType the type of elements in the collection (e.g., "String", "Integer")
     * @see CollectionFilter#setCollectionContains(Object)
     */
    private void generateCollectionContainsSwitchCase(PrintWriter out, String elementType) {
        out.println("                case " + FilterConstants.class.getSimpleName() + ".FIELD_CONT" + FIELD_CASE_SUFFIX);
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                        // Deserialize element using appropriate type deserializer");
        out.println("                        // Element type: " + elementType);
        out.println("                        filter.setCollectionContains(p.getText());");
        out.println(EXP_3);
        out.println(EXP_5);
    }
    
    /**
     * Generates a switch case for collection base operators (eq, neq) in Jackson deserializers.
     * These operators work on the entire collection as a value, not on individual elements.
     * 
     * <p>The generated code deserializes a JSON array into a List and sets it on the filter
     * using the specified setter method (setEquals or setNotEquals).
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * case "eq" -> {
     *     if (p.getCurrentToken() == JsonToken.START_ARRAY) {
     *         // Parse collection value for setEquals
     *         List<String> values = new ArrayList<>(FilterConstants.TYPICAL_IN_SIZE);
     *         while (p.nextToken() != JsonToken.END_ARRAY) {
     *             if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
     *                 values.add((String) p.getText());
     *             }
     *         }
     *         filter.setEquals(values);
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Usage in JSON:</b>
     * <pre>{@code
     * {
     *   "tags": {
     *     "eq": ["tag1", "tag2", "tag3"]
     *   }
     * }
     * }</pre>
     * 
     * @param out the PrintWriter to write generated code to
     * @param fieldConstant the FilterConstants field name (e.g., "FilterConstants.FIELD_EQ")
     * @param setterMethod the setter method name (e.g., "setEquals", "setNotEquals")
     * @param elementType the type of elements in the collection (e.g., "String", "Integer")
     * @see CollectionFilter#setEquals(java.util.Collection)
     * @see CollectionFilter#setNotEquals(java.util.Collection)
     */
    private void generateCollectionBaseSwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String elementType) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".START_ARRAY) {");
        out.println("                        // Parse collection value for " + setterMethod);
        out.println(EXP_4 + List.class.getSimpleName() + "<" + elementType + "> values = new " + ArrayList.class.getSimpleName() + "<>(" + FilterConstants.class.getSimpleName() + TYPICAL_IN_SIZE);
        out.println("                        while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_ARRAY) {");
        out.println("                            if (p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                                values.add((" + elementType + ") p.getText());");
        out.println("                            }");
        out.println("                        }");
        out.println("                        filter." + setterMethod + "(values);");
        out.println(EXP_3);
        out.println(EXP_5);
    }
    
    /**
     * Generates a switch case for collection in/nin operators in Jackson deserializers.
     * These operators accept a collection of collections (array of arrays in JSON).
     * 
     * <p>The generated code deserializes a JSON array of arrays into a List of Lists
     * and sets it on the filter using the specified setter method (setIn or setNotIn).
     * 
     * <p><b>Generated Code Example:</b>
     * <pre>{@code
     * case "in" -> {
     *     if (p.getCurrentToken() == JsonToken.START_ARRAY) {
     *         // Parse collection of collections for setIn
     *         List<List<String>> collections = new ArrayList<>(FilterConstants.TYPICAL_IN_SIZE);
     *         while (p.nextToken() != JsonToken.END_ARRAY) {
     *             if (p.getCurrentToken() == JsonToken.START_ARRAY) {
     *                 List<String> innerCollection = new ArrayList<>(FilterConstants.TYPICAL_IN_SIZE);
     *                 while (p.nextToken() != JsonToken.END_ARRAY) {
     *                     if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
     *                         innerCollection.add((String) p.getText());
     *                     }
     *                 }
     *                 collections.add(innerCollection);
     *             }
     *         }
     *         filter.setIn(collections);
     *     }
     * }
     * }</pre>
     * 
     * <p><b>Usage in JSON:</b>
     * <pre>{@code
     * {
     *   "tags": {
     *     "in": [
     *       ["tag1", "tag2"],
     *       ["tag3", "tag4"],
     *       ["tag5"]
     *     ]
     *   }
     * }
     * }</pre>
     * 
     * @param out the PrintWriter to write generated code to
     * @param fieldConstant the FilterConstants field name (e.g., "FilterConstants.FIELD_IN")
     * @param setterMethod the setter method name (e.g., "setIn", "setNotIn")
     * @param elementType the type of elements in the collection (e.g., "String", "Integer")
     * @see CollectionFilter#setIn(java.util.Collection)
     * @see CollectionFilter#setNotIn(java.util.Collection)
     */
    private void generateCollectionInSwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String elementType) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".START_ARRAY) {");
        out.println("                        // Parse collection of collections for " + setterMethod);
        out.println(EXP_4 + List.class.getSimpleName() + "<" + List.class.getSimpleName() + "<" + elementType + ">> collections = new " + ArrayList.class.getSimpleName() + "<>(" + FilterConstants.class.getSimpleName() + TYPICAL_IN_SIZE + ");");
        out.println("                        while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_ARRAY) {");
        out.println("                            if (p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".START_ARRAY) {");
        out.println("                                " + List.class.getSimpleName() + "<" + elementType + "> innerCollection = new " + ArrayList.class.getSimpleName() + "<>(" + FilterConstants.class.getSimpleName() + TYPICAL_IN_SIZE + ");");
        out.println("                                while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_ARRAY) {");
        out.println("                                    if (p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                                        innerCollection.add((" + elementType + ") p.getText());");
        out.println("                                    }");
        out.println("                                }");
        out.println("                                collections.add(innerCollection);");
        out.println("                            }");
        out.println("                        }");
        out.println("                        filter." + setterMethod + "(collections);");
        out.println(EXP_3);
        out.println(EXP_5);
    }

    /**
     * Generates helper method for temporal parsing.
     */
    private void generateTemporalHelperMethod(PrintWriter out, FilterFieldConfig config) {
        String methodName = getTemporalParseMethodName(config);
        String returnType = getTemporalElementType(config.getFieldType());
        String formatterName = "FORMATTER_" + toSafeConstantName(config.getFieldName());

        out.println(EXP_7);
        out.println("     * Parses " + returnType + " using the pre-determined format (custom or default).");
        out.println("     * Generated based on build-time analysis of entity field annotations.");
        out.println(EXP_1);
        out.println("    private static " + returnType + " " + methodName + "(String value) {");

        if (config.getFieldType().contains(Instant.class.getSimpleName())) {
            out.println("        return " + Instant.class.getSimpleName() + ".from(" + formatterName + ".parse(value));");
        } else {
            out.println("        return " + returnType + ".parse(value, " + formatterName + ");");
        }

        out.println(EXP_11);
        out.println();
    }

    /**
     * Generates helper method for enum parsing.
     */
    private void generateEnumHelperMethod(PrintWriter out, FilterFieldConfig config) {
        String enumSimpleName = getSimpleClassName(config.getFieldType());
        EnumDeserializationInfo enumInfo = config.getEnumDeserializationInfo();

        out.println(EXP_7);
        out.println("     * Generated enum parsing method - logic determined during code generation.");
        out.println("     * Based on build-time analysis of " + config.getFieldType() + " enum.");
        out.println(EXP_1);
        out.println("    private static " + enumSimpleName + " parse" + enumSimpleName + "(String value) {");

        if (enumInfo != null) {
            switch (enumInfo.getDeserializationType()) {
                case CREATOR_METHOD -> generateCreatorMethodCode(out, enumSimpleName, enumInfo);
                case VALUE_FIELD -> generateValueFieldCode(out, enumSimpleName, enumInfo);
                case VALUE_METHOD -> generateValueMethodCode(out, enumSimpleName, enumInfo);
                case DEFAULT_MATCHING -> generateDefaultMatchingCode(out, enumSimpleName);
            }
        } else {
            generateDefaultMatchingCode(out, enumSimpleName);
        }

        out.println(EXP_11);
        out.println();
    }

    /**
     * Gets the numeric parse method for the specified filter type.
     */
    private String getNumericParseMethod(String filterType) {
        return switch (filterType) {
            case INTEGER_FILTER -> GET_INT_VALUE;
            case LONG_FILTER -> "p.getLongValue()";
            case DOUBLE_FILTER -> "p.getDoubleValue()";
            default -> throw new IllegalArgumentException("Unsupported numeric filter type: " + filterType);
        };
    }

    /**
     * Gets the numeric element type for the specified filter type.
     */
    private String getNumericElementType(String filterType) {
        return switch (filterType) {
            case INTEGER_FILTER -> "Integer";
            case LONG_FILTER -> "Long";
            case DOUBLE_FILTER -> DOUBLE;
            default -> throw new IllegalArgumentException("Unsupported numeric filter type: " + filterType);
        };
    }

    /**
     * Generates a switch case for numeric field deserialization.
     */
    private void generateNumericSwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String parseMethod) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".VALUE_NUMBER_INT || ");
        out.println("                        p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_NUMBER_FLOAT ||");
        out.println("                        p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                        // Performance: Direct parsing, no intermediate objects");
        out.println("                        filter." + setterMethod + "(" + parseMethod + ");");
        out.println(EXP_3);
        out.println(EXP_5);
    }

    /**
     * Generates a switch case for numeric array field deserialization.
     */
    private void generateNumericArraySwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String elementType, String parseMethod) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".START_ARRAY) {");
        out.println("                        // Performance: Pre-sized ArrayList using shared constants");
        out.println(EXP_4 + List.class.getSimpleName() + "<" + elementType + "> values = new " + ArrayList.class.getSimpleName() + "<>(" + FilterConstants.class.getSimpleName() + TYPICAL_IN_SIZE);
        out.println("                        while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_ARRAY) {");
        out.println("                            if (p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_NUMBER_INT || ");
        out.println("                                p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_NUMBER_FLOAT ||");
        out.println("                                p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                                values.add(" + parseMethod + ");");
        out.println("                            }");
        out.println("                        }");
        out.println("                        filter." + setterMethod + "(values);");
        out.println(EXP_3);
        out.println(EXP_5);
    }

    /**
     * Generates a switch case for boolean field deserialization.
     */
    private void generateBooleanSwitchCase(PrintWriter out, String fieldConstant, String setterMethod) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        generateBooleanParsingTemplate(out, setterMethod);
        out.println(EXP_5);
    }

    /**
     * Generates a switch case for single value field deserialization.
     */
    private void generateSwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String parseMethod) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        generateSingleValueParsingTemplate(out, parseMethod, setterMethod);
        out.println(EXP_5);
    }

    /**
     * Generates a switch case for array field deserialization.
     */
    private void generateArraySwitchCase(PrintWriter out, String fieldConstant, String setterMethod, String elementType, String parseMethod) {
        out.println("                case " + fieldConstant + FIELD_CASE_SUFFIX);
        generateArrayParsingTemplate(out, elementType, parseMethod, setterMethod);
        out.println(EXP_5);
    }

    /**
     * Capitalizes the first letter of a string for getter/setter method names using English locale.
     * This ensures 'i' becomes 'I' not 'İ' (Turkish).
     * For boolean fields starting with "is", removes the "is" prefix since Lombok
     * generates getters without the duplicate "is" (e.g., isActive -> isActive(), not isIsActive()).
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // Handle boolean fields with "is" prefix (Lombok behavior)
        // e.g., "isActive" -> "Active" so that "get" + "Active" = "getActive"
        // and the actual getter from Lombok is "isActive()"
        if (str.startsWith("is") && str.length() > 2 && Character.isUpperCase(str.charAt(2))) {
            return str.substring(2); // Remove "is" prefix
        }
        return str.substring(0, 1).toUpperCase(java.util.Locale.ENGLISH) + str.substring(1);
    }

    /**
     * Gets the simple type name from a fully qualified type name. Handles
     * generic types like "java.util.List<java.lang.String>" -> "String" and
     * simple types like "java.lang.String" -> "String" Also converts primitive
     * types to their boxed equivalents (boolean -> Boolean, int -> Integer,
     * etc.)
     */
    private String getSimpleTypeName(String fullyQualifiedType) {
        if (fullyQualifiedType == null || fullyQualifiedType.isEmpty()) {
            return fullyQualifiedType;
        }

        // Handle generic types: extract the element type from Collection<ElementType>
        if (fullyQualifiedType.contains("<") && fullyQualifiedType.contains(">")) {
            int startIndex = fullyQualifiedType.indexOf('<') + 1;
            int endIndex = fullyQualifiedType.lastIndexOf('>');
            if (startIndex > 0 && endIndex > startIndex) {
                fullyQualifiedType = fullyQualifiedType.substring(startIndex, endIndex).trim();
            }
        }

        // Get simple name from fully qualified name
        int lastDot = fullyQualifiedType.lastIndexOf('.');
        String simpleName;
        if (lastDot > 0 && lastDot < fullyQualifiedType.length() - 1) {
            simpleName = fullyQualifiedType.substring(lastDot + 1);
        } else {
            simpleName = fullyQualifiedType;
        }

        // Convert primitive types to boxed types for use in generics and reflection
        return convertPrimitiveToBoxed(simpleName);
    }

    /**
     * Converts primitive type names to their boxed equivalents. This is
     * necessary because primitives cannot be used in generics (List<int> is
     * invalid) and Class.forName() doesn't work with primitives.
     */
    private String convertPrimitiveToBoxed(String typeName) {
        return switch (typeName) {
            case LOWER_BOOLEAN -> "Boolean";
            case "byte" -> "Byte";
            case "char" -> "Character";
            case "short" -> "Short";
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "double" -> DOUBLE;
            default -> typeName;
        };
    }

    /**
     * Generates a streaming JSON array parser template for collection fields.
     * This is a common pattern used across different filter types.
     */
    protected void generateArrayParsingTemplate(PrintWriter out, String elementType,
                                                String parseMethodCall, String setterMethod) {
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".START_ARRAY) {");
        out.println("                        // Performance: Pre-sized ArrayList using shared constants");
        out.println(EXP_4 + List.class.getSimpleName() + "<" + elementType + "> values = new " + ArrayList.class.getSimpleName() + "<>(" + FilterConstants.class.getSimpleName() + TYPICAL_IN_SIZE);
        out.println("                        while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_ARRAY) {");
        out.println("                            if (p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                                values.add(" + parseMethodCall + ");");
        out.println("                            }");
        out.println("                        }");
        out.println("                        filter." + setterMethod + "(values);");
        out.println(EXP_3);
    }

    /**
     * Generates a single value parsing template for non-collection fields. This
     * is a common pattern used across different filter types.
     */
    protected void generateSingleValueParsingTemplate(PrintWriter out, String parseMethodCall,
                                                      String setterMethod) {
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".VALUE_STRING) {");
        out.println("                        // Performance: Direct parsing, no intermediate objects");
        out.println("                        filter." + setterMethod + "(" + parseMethodCall + ");");
        out.println(EXP_3);
    }

    /**
     * Generates a boolean value parsing template for null check fields.
     */
    protected void generateBooleanParsingTemplate(PrintWriter out, String setterMethod) {
        out.println(EXP_12 + JsonToken.class.getSimpleName() + ".VALUE_TRUE || p.getCurrentToken() == " + JsonToken.class.getSimpleName() + ".VALUE_FALSE) {");
        out.println("                        filter." + setterMethod + "(p.getBooleanValue());");
        out.println(EXP_3);
    }

    // Helper methods for getting type-specific information
    private String getTemporalParseMethod(FilterFieldConfig config) {
        return getTemporalParseMethodName(config) + "(p.getText())";
    }

    private String getTemporalParseMethodName(FilterFieldConfig config) {
        String fieldType = config.getFieldType();
        if (fieldType.contains(LocalDateTime.class.getSimpleName())) {
            return PARSE + LocalDateTime.class.getSimpleName() + "_" + config.getFieldName();
        } else if (fieldType.contains(LocalDate.class.getSimpleName())) {
            return PARSE + LocalDate.class.getSimpleName() + "_" + config.getFieldName();
        } else if (fieldType.contains(Instant.class.getSimpleName())) {
            return PARSE + Instant.class.getSimpleName() + "_" + config.getFieldName();
        }
        return PARSE + LocalDateTime.class.getSimpleName() + "_" + config.getFieldName();
    }

    private String getTemporalElementType(String fieldType) {
        if (fieldType.contains(LocalDateTime.class.getSimpleName())) {
            return LocalDateTime.class.getSimpleName();
        } else if (fieldType.contains(LocalDate.class.getSimpleName())) {
            return LocalDate.class.getSimpleName();
        } else if (fieldType.contains(Instant.class.getSimpleName())) {
            return Instant.class.getSimpleName();
        }
        return LocalDateTime.class.getSimpleName();
    }

    private String getSimpleClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Processes a collection field to detect if its element type is a model type.
     * <p>
     * This method extracts the element type from the CollectionFilter generic parameter,
     * uses ModelTypeDetector to check if the element type is a model (has a corresponding
     * filter class), and sets the appropriate flags and metadata on the FilterFieldConfig.
     * <p>
     * For model element types, this method sets:
     * - isModelElementType flag to true
     * - elementFilterType to the filter class name (e.g., "UserFilter")
     * - elementFilterPackage to the package name of the filter class
     * <p>
     * For basic element types (String, Integer, etc.), no additional metadata is set.
     *
     * @param config the FilterFieldConfig for the collection field
     */
    protected void processCollectionField(FilterFieldConfig config) {
        if (config == null || !config.isCollection()) {
            return;
        }

        String elementType = config.getElementType();
        if (elementType == null || elementType.isEmpty()) {
            debugLog("Collection field " + config.getFieldName() + " has no element type, skipping model type detection");
            return;
        }

        debugLog("Processing collection field: " + config.getFieldName() + " with element type: " + elementType);

        // Check if element type is a model type using ModelTypeDetector
        boolean isModelElement = ModelTypeDetector.isModelType(elementType, processingEnv);

        if (isModelElement) {
            debugLog("Element type " + elementType + " is a model type");

            // Set model element type flag
            config.setModelElementType(true);

            // Get the filter class name for the element type
            String elementFilterType = ModelTypeDetector.getFilterClassName(elementType);
            config.setElementFilterType(elementFilterType);

            // Extract package name from element type
            String elementFilterPackage = "";
            int lastDot = elementType.lastIndexOf('.');
            if (lastDot >= 0) {
                elementFilterPackage = elementType.substring(0, lastDot);
            }
            config.setElementFilterPackage(elementFilterPackage);

            debugLog("Set element filter type: " + elementFilterType + " in package: " + elementFilterPackage);
            
            // Verify that the filter class actually exists
            String qualifiedFilterClassName = ModelTypeDetector.getQualifiedFilterClassName(elementType, processingEnv);
            if (qualifiedFilterClassName == null) {
                // This should not happen if isModelType returned true, but check anyway
                String simpleElementType = elementType.substring(elementType.lastIndexOf('.') + 1);
                String expectedFilterClass = simpleElementType + "Filter";
                String errorMessage = String.format(
                    "Filter class not found for element type '%s' in collection field '%s'. " +
                    "Expected filter class '%s' in package '%s'. " +
                    "Ensure the element type has a @MetaModel annotation and its filter class is generated.",
                    simpleElementType,
                    config.getFieldName(),
                    expectedFilterClass,
                    elementFilterPackage.isEmpty() ? "(default package)" : elementFilterPackage
                );
                processingEnv.getMessager().printMessage(
                    javax.tools.Diagnostic.Kind.ERROR,
                    errorMessage
                );
                // Don't throw exception - let annotation processor continue and report all errors
            }
        } else {
            debugLog("Element type " + elementType + " is a basic type");
            // For basic types, no additional metadata is needed
            config.setModelElementType(false);
        }
    }

    // Enum parsing code generation methods
    private void generateCreatorMethodCode(PrintWriter out, String enumSimpleName, EnumDeserializationInfo enumInfo) {
        out.println("        // CASE: " + enumSimpleName + " has @JsonCreator method \"" + enumInfo.getJsonCreatorMethod() + "\"");
        out.println("        return " + enumSimpleName + "." + enumInfo.getJsonCreatorMethod() + "(value);");
    }

    private void generateValueFieldCode(PrintWriter out, String enumSimpleName, EnumDeserializationInfo enumInfo) {
        out.println("        // CASE: " + enumSimpleName + " has @JsonValue on \"" + enumInfo.getJsonValueField() + "\" field");
        out.println("        for (" + enumSimpleName + " enumValue : " + enumSimpleName + ".values()) {");
        out.println("            if (java.util.Objects.equals(enumValue." + enumInfo.getJsonValueField() + ", value)) {");
        out.println("                return enumValue;");
        out.println(EXP_6);
        out.println(EXP_10);
        out.println("        throw new IllegalArgumentException(\"Unknown " + enumSimpleName + " " + enumInfo.getJsonValueField() + ": \" + value);");
    }

    private void generateValueMethodCode(PrintWriter out, String enumSimpleName, EnumDeserializationInfo enumInfo) {
        out.println("        // CASE: " + enumSimpleName + " has @JsonValue on \"" + enumInfo.getJsonValueMethod() + "\" method");
        out.println("        for (" + enumSimpleName + " enumValue : " + enumSimpleName + ".values()) {");
        out.println("            if (java.util.Objects.equals(enumValue." + enumInfo.getJsonValueMethod() + "(), value)) {");
        out.println("                return enumValue;");
        out.println(EXP_6);
        out.println(EXP_10);
        out.println("        throw new IllegalArgumentException(\"Unknown " + enumSimpleName + " value: \" + value);");
    }

    private void generateDefaultMatchingCode(PrintWriter out, String enumSimpleName) {
        out.println("        // CASE: No Jackson annotations - using valueOf() with case-insensitive fallback");
        out.println("        try {");
        out.println("            return " + enumSimpleName + ".valueOf(value);");
        out.println("        } catch (IllegalArgumentException e) {");
        out.println("            for (" + enumSimpleName + " enumValue : " + enumSimpleName + ".values()) {");
        out.println("                if (enumValue.name().equalsIgnoreCase(value)) {");
        out.println("                    return enumValue;");
        out.println(EXP_5);
        out.println(EXP_6);
        out.println("            throw new IllegalArgumentException(\"Unknown " + enumSimpleName + ": \" + value);");
        out.println(EXP_10);
    }

    // Enhanced generation methods with validation

    /**
     * Generates imports with validation and error handling. Filters out imports
     * for classes in the same package.
     */
    private void generateImportsWithValidation(PrintWriter out, String packageName, List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        try {
            // Add common imports
            COMMON_IMPORTS.forEach(imp -> out.println("import " + imp + ";"));

            // Add filter-specific imports based on field configurations
            Set<String> additionalImports = getAdditionalImports(fieldConfigs);

            // Filter out imports for classes in the same package
            additionalImports.stream()
                    .filter(imp -> !isSamePackage(imp, packageName))
                    .forEach(imp -> out.println("import " + imp + ";"));

            out.println();
        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "import_generation",
                    "Failed to generate imports: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a fully qualified class name is in the same package as the
     * given package.
     */
    private boolean isSamePackage(String fullyQualifiedClassName, String packageName) {
        if (fullyQualifiedClassName == null || packageName == null) {
            return false;
        }

        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        if (lastDot == -1) {
            // No package (default package)
            return packageName.isEmpty();
        }

        String classPackage = fullyQualifiedClassName.substring(0, lastDot);
        return classPackage.equals(packageName);
    }

    /**
     * Generates class declaration with validation.
     */
    private void generateClassDeclarationWithValidation(PrintWriter out, String filterClassName,
                                                        String deserializerClassName, String entityClassName) throws DeserializerGenerationException {
        try {
            out.println("/**");
            out.println(" * High-performance custom Jackson deserializer for " + filterClassName + ".");
            out.println(" * Generated by FilterDeserializerGenerator to replicate entity field");
            out.println(" * Jackson annotation behavior for optimal filter deserialization performance.");
            out.println(" * ");
            out.println(" * Performance optimizations:");
            out.println(" * - Streaming JSON parser (no intermediate tree model)");
            out.println(" * - Switch statements for field name matching");
            out.println(" * - Pre-sized collections using FilterConstants");
            out.println(" * - Type-specific parsing without reflection");
            out.println(" * - Shared constants for field names");
            out.println(" */");
            out.println("public class " + deserializerClassName + " extends " + JsonDeserializer.class.getSimpleName() + "<" + filterClassName + "> {");
            out.println();
            out.println(EXP_7);
            out.println("     * Singleton instance of this deserializer.");
            out.println(EXP_1);
            out.println("    public static final " + deserializerClassName + " INSTANCE = new " + deserializerClassName + "();");
            out.println();
        } catch (Exception e) {
            throw new DeserializerGenerationException(filterClassName, deserializerClassName, "class_declaration",
                    "Failed to generate class declaration: " + e.getMessage(), e);
        }
    }

    /**
     * Generates field constants with validation.
     */
    private void generateFieldConstantsWithValidation(PrintWriter out, List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        try {
            boolean hasConstants = false;

            for (FilterFieldConfig config : fieldConfigs) {
                if (config.isTemporal()) {
                    generateTemporalFieldConstant(out, config);
                    hasConstants = true;
                }
            }

            if (hasConstants) {
                out.println();
            }
        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "field_constants",
                    "Failed to generate field constants: " + e.getMessage(), e);
        }
    }

    /**
     * Generates deserialize method with validation. This method deserializes
     * the entire filter from JSON, handling nested filters recursively.
     */
    private void generateDeserializeMethodWithValidation(PrintWriter out, String filterClassName,
                                                         List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        try {
            out.println("    @Override");
            out.println("    public " + filterClassName + " deserialize(" + JsonParser.class.getSimpleName() + " p, " + DeserializationContext.class.getSimpleName() + " ctxt) throws " + IOException.class.getSimpleName() + " {");
            out.println("        // Performance: Direct object creation, no reflection");
            out.println(EXP_9 + filterClassName + " filter = new " + filterClassName + "();");
            out.println();
            out.println("        if (p.getCurrentToken() != " + JsonToken.class.getSimpleName() + ".START_OBJECT) {");
            out.println("            throw ctxt.wrongTokenException(p, " + filterClassName + ".class, " + JsonToken.class.getSimpleName() + ".START_OBJECT, \"Expected start object\");");
            out.println(EXP_10);
            out.println();
            out.println("        // Performance: Streaming parser, no intermediate tree");
            out.println("        while (p.nextToken() != " + JsonToken.class.getSimpleName() + ".END_OBJECT) {");
            out.println("            String fieldName = p.getCurrentName();");
            out.println("            p.nextToken();");
            out.println();
            out.println("            // Performance: Switch statement with interned strings");
            out.println("            switch (fieldName) {");

            // Generate switch cases for all fields (both nested filters and their operations)
            generateFieldDeserializationSwitchCases(out, fieldConfigs);

            out.println("                default -> p.skipChildren(); // Skip unknown fields efficiently");
            out.println(EXP_6);
            out.println(EXP_10);
            out.println();
            out.println("        return filter;");
            out.println(EXP_11);
            out.println();
        } catch (Exception e) {
            throw new DeserializerGenerationException(filterClassName, "", "deserialize_method",
                    "Failed to generate deserialize method: " + e.getMessage(), e);
        }
    }

    /**
     * Generates helper methods with validation.
     */
    private void generateHelperMethodsWithValidation(PrintWriter out, String filterClassName, List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        try {
            // Generate mapFieldPath method for query parameter support
            generateMapFieldPathMethod(out, fieldConfigs);

            // Generate bindQueryParameters method for query parameter support
            generateBindQueryParametersMethod(out, filterClassName, fieldConfigs);

            for (FilterFieldConfig config : fieldConfigs) {
                if (config.isTemporal()) {
                    generateTemporalHelperMethod(out, config);
                } else if (config.isEnum()) {
                    generateEnumHelperMethod(out, config);
                }
            }
        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "helper_methods",
                    "Failed to generate helper methods: " + e.getMessage(), e);
        }
    }

    /**
     * Generates the mapFieldPath() static method that maps abbreviated field
     * paths (using @JsonProperty names) to Java field paths for query parameter
     * binding.
     * <p>
     * This method: - Maps abbreviated field names to Java field names based on
     *
     * @param out          the PrintWriter to write generated code to
     * @param fieldConfigs list of field configurations for the filter
     * @throws DeserializerGenerationException if generation fails
     * @JsonProperty annotations - Handles nested filter paths by delegating to
     * nested filter's mapFieldPath() method - Preserves operator segments in
     * paths (e.g., "n.eq" → "name.eq") - Uses switch statements for O(1) field
     * name lookup
     */
    private void generateMapFieldPathMethod(PrintWriter out, List<FilterFieldConfig> fieldConfigs) throws DeserializerGenerationException {
        try {
            debugLog("Generating mapFieldPath() method");

            // Build mapping of abbreviated names to Java field names
            // Only include fields that have @JsonProperty annotations
            java.util.Map<String, String> fieldMappings = new java.util.HashMap<>();
            java.util.Set<String> nestedFilterFields = new java.util.HashSet<>();

            for (FilterFieldConfig config : fieldConfigs) {
                String fieldName = config.getFieldName();
                String filterType = config.getFilterType();

                // Check if this is a nested filter (field type ends with "Filter")
                boolean isNestedFilter = filterType != null && filterType.endsWith("Filter")
                        && !isBasicFilterType(filterType);

                if (isNestedFilter) {
                    nestedFilterFields.add(fieldName);
                }

                // Check for @JsonProperty annotation
                if (config.hasJacksonAnnotations()) {
                    for (AnnotationInfo annotation : config.getJacksonAnnotations()) {
                        if ("JsonProperty".equals(annotation.getAnnotationType())
                                || annotation.getAnnotationType().endsWith(".JsonProperty")) {

                            // Get the value parameter from the annotation
                            java.util.Map<String, Object> params = annotation.getParameters();
                            if (params != null && params.containsKey(VALUE)) {
                                String abbreviatedName = String.valueOf(params.get(VALUE));
                                // Remove quotes if present
                                abbreviatedName = abbreviatedName.replaceAll("^\"|\"$", "");
                                fieldMappings.put(abbreviatedName, fieldName);
                                debugLog("Found @JsonProperty mapping: " + abbreviatedName + " -> " + fieldName);
                            }
                        }
                    }
                }
            }

            // Generate the method
            out.println(EXP_7);
            out.println("     * Maps abbreviated field paths (using @JsonProperty names) to Java field paths.");
            out.println("     * This method supports query parameter binding with abbreviated field names.");
            out.println(EXP_2);
            out.println("     * Examples:");
            out.println("     * - \"n.eq\" -> \"name.eq\" (if @JsonProperty(\"n\") is on name field)");
            out.println("     * - \"addr.city.eq\" -> \"address.city.eq\" (nested path mapping)");
            out.println("     * - \"unknownField.eq\" -> \"unknownField.eq\" (pass-through for unmapped fields)");
            out.println(EXP_2);
            out.println("     * @param path the abbreviated field path from query parameters");
            out.println("     * @return the mapped Java field path");
            out.println(EXP_1);
            out.println("    public static String mapFieldPath(String path) {");
            out.println("        if (path == null || path.isEmpty()) {");
            out.println("            return path;");
            out.println(EXP_10);
            out.println();
            out.println("        // Split path into segments");
            out.println("        String[] segments = path.split(\"\\\\.\");");
            out.println("        if (segments.length == 0) {");
            out.println("            return path;");
            out.println(EXP_10);
            out.println();
            out.println("        // Map first segment (field name)");
            out.println("        String firstSegment = segments[0];");

            if (fieldMappings.isEmpty()) {
                // No mappings - just return the path as-is
                out.println("        // No @JsonProperty mappings defined for this filter");
                out.println("        return path;");
            } else {
                // Generate switch statement for field mapping
                out.println("        String mappedField = switch (firstSegment) {");
                for (java.util.Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                    out.println(FIELD_CASE_PREFIX + entry.getKey() + "\" -> \"" + entry.getValue() + "\";");
                }
                out.println("            default -> firstSegment; // No mapping, use as-is");
                out.println("        };");
                out.println();
                out.println("        // If only one segment, return mapped field");
                out.println("        if (segments.length == 1) {");
                out.println("            return mappedField;");
                out.println(EXP_10);
                out.println();

                if (!nestedFilterFields.isEmpty()) {
                    // Generate nested path handling
                    out.println("        // Handle nested paths");
                    out.println("        if (segments.length >= 2) {");
                    out.println("            // Check if mapped field is a nested filter");
                    out.println("            if (isNestedFilterField(mappedField)) {");
                    out.println("                // Recursively map nested path");
                    out.println("                String nestedPath = String.join(\".\", ");
                    out.println("                    java.util.Arrays.copyOfRange(segments, 1, segments.length));");
                    out.println("                String mappedNestedPath = mapNestedFieldPath(mappedField, nestedPath);");
                    out.println("                return mappedField + \".\" + mappedNestedPath;");
                    out.println(EXP_6);
                    out.println(EXP_10);
                    out.println();
                }

                out.println("        // Not a nested filter - remaining segments are operators");
                out.println("        // Reconstruct path with mapped field name");
                out.println("        StringBuilder result = new StringBuilder(mappedField);");
                out.println("        for (int i = 1; i < segments.length; i++) {");
                out.println("            result.append(\".\").append(segments[i]);");
                out.println(EXP_10);
                out.println("        return result.toString();");
            }

            out.println(EXP_11);
            out.println();

            // Generate helper method for nested filter detection
            if (!nestedFilterFields.isEmpty()) {
                generateIsNestedFilterFieldMethod(out, nestedFilterFields);
                generateMapNestedFieldPathMethod(out, nestedFilterFields, fieldConfigs);
            }

            debugLog("mapFieldPath() method generation completed");

        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "mapFieldPath_generation",
                    "Failed to generate mapFieldPath method: " + e.getMessage(), e);
        }
    }

    /**
     * Generates the isNestedFilterField() helper method.
     */
    private void generateIsNestedFilterFieldMethod(PrintWriter out, java.util.Set<String> nestedFilterFields) {
        out.println(EXP_7);
        out.println("     * Checks if a field is a nested filter object.");
        out.println(EXP_2);
        out.println("     * @param fieldName the Java field name");
        out.println("     * @return true if the field is a nested filter");
        out.println(EXP_1);
        out.println("    private static boolean isNestedFilterField(String fieldName) {");
        out.println("        return switch (fieldName) {");
        for (String nestedField : nestedFilterFields) {
            out.println(FIELD_CASE_PREFIX + nestedField + "\" -> true;");
        }
        out.println("            default -> false;");
        out.println("        };");
        out.println(EXP_11);
        out.println();
    }

    /**
     * Generates the mapNestedFieldPath() helper method that delegates to the
     * appropriate nested filter's mapFieldPath() method.
     */
    private void generateMapNestedFieldPathMethod(PrintWriter out, java.util.Set<String> nestedFilterFields,
                                                  List<FilterFieldConfig> fieldConfigs) {
        out.println(EXP_7);
        out.println("     * Maps a nested field path by delegating to the appropriate nested filter's mapFieldPath() method.");
        out.println(EXP_2);
        out.println("     * @param parentField the parent field name");
        out.println("     * @param nestedPath the nested path to map");
        out.println("     * @return the mapped nested path");
        out.println(EXP_1);
        out.println("    private static String mapNestedFieldPath(String parentField, String nestedPath) {");
        out.println("        return switch (parentField) {");

        for (String nestedField : nestedFilterFields) {
            // Find the config for this nested field to get its filter type
            FilterFieldConfig nestedConfig = fieldConfigs.stream()
                    .filter(c -> c.getFieldName().equals(nestedField))
                    .findFirst()
                    .orElse(null);

            if (nestedConfig != null && nestedConfig.getFilterType() != null) {
                String nestedFilterType = nestedConfig.getFilterType();

                // Skip CollectionFilter - it doesn't have nested fields to map
                if (nestedConfig.isCollection()) {
                    out.println(FIELD_CASE_PREFIX + nestedField + "\" -> nestedPath; // CollectionFilter has no nested fields");
                } else if (isBasicFilterType(nestedFilterType)) {
                    // For basic filter types, return path as-is
                    out.println(FIELD_CASE_PREFIX + nestedField + "\" -> nestedPath; // Basic filter type");
                } else {
                    // Generate delegation to nested filter's deserializer for model/custom filter types
                    out.println(FIELD_CASE_PREFIX + nestedField + "\" -> " + nestedFilterType + "Deserializer.mapFieldPath(nestedPath);");
                }
            }
        }

        out.println("            default -> nestedPath; // Unknown nested field, return as-is");
        out.println("        };");
        out.println(EXP_11);
        out.println();
    }

    // Utility methods for validation

    /**
     * Validates if a string is a valid Java identifier.
     */
    private boolean isValidJavaIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates if a string is a valid Java package name.
     */
    private boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return true; // Empty package is valid (default package)
        }

        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (!isValidJavaIdentifier(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Logs an error message using the processing environment.
     */
    private void logError(String message) {
        if (processingEnv != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "FilterDeserializerGenerator: " + message);
        } else {
            System.err.println("FilterDeserializerGenerator ERROR: " + message);
        }
    }

    /**
     * Logs a warning message using the processing environment.
     */
    private void logWarning(String message) {
        if (processingEnv != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "FilterDeserializerGenerator: " + message);
        } else {
            System.out.println("FilterDeserializerGenerator WARNING: " + message);
        }
    }

    /**
     * Converts a field name to a safe constant name (uppercase, alphanumeric
     * only).
     */
    private String toSafeConstantName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "FIELD";
        }

        // Convert to uppercase using ROOT locale (avoids Turkish İ/i issue) and replace non-alphanumeric characters with underscores
        return fieldName.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_") // Replace multiple underscores with single
                .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
    }

    /**
     * Generates the bindQueryParameters() static method that binds query
     * parameters to filter objects. This method provides zero-reflection
     * parameter binding with full type safety.
     *
     * @param out             the PrintWriter to write to
     * @param filterClassName the name of the filter class
     * @param fieldConfigs    the field configurations
     * @throws DeserializerGenerationException if generation fails
     */
    private void generateBindQueryParametersMethod(PrintWriter out, String filterClassName, List<FilterFieldConfig> fieldConfigs)
            throws DeserializerGenerationException {

        try {
            debugLog("Generating bindQueryParameters() method for " + filterClassName);

            if (fieldConfigs.isEmpty()) {
                debugLog("No field configs provided, skipping bindQueryParameters generation");
                return;
            }

            if (filterClassName == null || filterClassName.isEmpty()) {
                debugLog("No filter class name provided, skipping bindQueryParameters generation");
                return;
            }

            // Method signature with proper types - fully typed, no generics needed
            out.println(EXP_7);
            out.println("     * Binds query parameters to a " + filterClassName + " object without reflection.");
            out.println("     * Generated at compile-time for optimal performance.");
            out.println("     * <p>");
            out.println("     * This method automatically excludes Spring pagination and sorting parameters");
            out.println("     * (page, size, sort) which are handled by Spring's PageableHandlerMethodArgumentResolver.");
            out.println(EXP_2);
            out.println("     * @param parameterMap the query parameter map");
            out.println("     * @param deserializer the value deserializer");
            out.println("     * @param collectionHandler the collection parameter handler");
            out.println("     * @param registry the deserializer registry");
            out.println("     * @return the populated filter object");
            out.println(EXP_1);
            out.println("    public static " + filterClassName + " bindQueryParameters(");
            out.println("            java.util.Map<String, String[]> parameterMap,");
            out.println("            com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer,");
            out.println("            com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler,");
            out.println("            com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry) {");
            out.println();
            out.println("        // Create filter instance - no reflection needed");
            out.println(EXP_9 + filterClassName + " filter = new " + filterClassName + "();");
            out.println();
            out.println("        // Process each query parameter");
            out.println("        for (java.util.Map.Entry<String, String[]> entry : parameterMap.entrySet()) {");
            out.println("            String paramName = entry.getKey();");
            out.println("            String[] paramValues = entry.getValue();");
            out.println("            ");
            out.println("            // Skip Spring pagination and sorting parameters");
            out.println("            // These are handled by PageableHandlerMethodArgumentResolver");
            out.println("            if (\"page\".equals(paramName) || \"size\".equals(paramName) || \"sort\".equals(paramName)) {");
            out.println("                continue;");
            out.println(EXP_6);
            out.println("            ");
            out.println("            if (paramValues == null || paramValues.length == 0) {");
            out.println("                continue;");
            out.println(EXP_6);
            out.println("            ");
            out.println("            String paramValue = paramValues[0];");
            out.println("            ");
            out.println("            // Map abbreviated path to Java field path");
            out.println("            String mappedPath = mapFieldPath(paramName);");
            out.println("            ");
            out.println("            // Route to appropriate setter based on field.operator combination");
            out.println("            // Generated switch statement for all field.operator combinations");
            out.println("            try {");
            out.println("                bindParameter(filter, mappedPath, paramValue, deserializer, collectionHandler, registry);");
            out.println("            } catch (Exception e) {");
            out.println("                throw new RuntimeException(\"Failed to bind parameter '\" + paramName + \"': \" + e.getMessage(), e);");
            out.println(EXP_6);
            out.println(EXP_10);
            out.println();
            out.println("        return filter;");
            out.println(EXP_11);
            out.println();

            // Generate the bindParameter helper method
            generateBindParameterMethod(out, filterClassName, fieldConfigs);

            debugLog("bindQueryParameters() method generation completed");

        } catch (Exception e) {
            throw new DeserializerGenerationException("", "", "bindQueryParameters_generation",
                    "Failed to generate bindQueryParameters method: " + e.getMessage(), e);
        }
    }

    /**
     * Generates the bindParameter helper method that contains the switch
     * statement.
     */
    private void generateBindParameterMethod(PrintWriter out, String filterClassName, List<FilterFieldConfig> fieldConfigs) {
        out.println(EXP_7);
        out.println("     * Binds a single parameter to the filter object.");
        out.println("     * Generated switch statement for optimal performance - zero reflection.");
        out.println("     * Handles nested filter paths by delegating to nested filter's bindQueryParameters.");
        out.println(EXP_1);
        out.println("    public static void bindParameter(");
        out.println("            " + filterClassName + " filter,");
        out.println("            String mappedPath,");
        out.println("            String paramValue,");
        out.println("            com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer,");
        out.println("            com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler,");
        out.println("            com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry) throws Exception {");
        out.println();
        out.println("        // Validate path structure");
        out.println("        if (mappedPath == null || mappedPath.isEmpty()) {");
        out.println("            throw new IllegalArgumentException(\"Parameter path cannot be null or empty\");");
        out.println(EXP_10);
        out.println();
        out.println("        String[] pathSegments = mappedPath.split(\"\\\\.\");");
        out.println("        if (pathSegments.length < 2) {");
        out.println("            throw new IllegalArgumentException(");
        out.println("                \"Malformed filter path '\" + mappedPath + \"': expected format 'field.operator' or 'field.nested.operator'\"");
        out.println("            );");
        out.println(EXP_10);
        out.println();
        out.println("        // Validate incomplete nested filter paths (any, all, none without operator)");
        out.println("        if (pathSegments.length == 2) {");
        out.println("            String nestedOp = pathSegments[1];");
        out.println("            if (nestedOp.equals(\"any\") || nestedOp.equals(\"all\") || nestedOp.equals(\"none\")) {");
        out.println("                throw new IllegalArgumentException(");
        out.println("                    \"Malformed nested filter path '\" + mappedPath + \"': expected format 'field.\" + nestedOp + \".operator'\"");
        out.println("                );");
        out.println("            }");
        out.println(EXP_10);
        out.println();
        out.println("        switch (mappedPath) {");

        // Generate switch cases for all field.operator combinations
        generateBindQueryParametersSwitchCases(out, fieldConfigs);

        // Check if there are nested filter fields or model type collection fields
        boolean hasNestedFilters = fieldConfigs.stream().anyMatch(FilterFieldConfig::isModel);
        boolean hasModelTypeCollections = fieldConfigs.stream()
            .anyMatch(config -> config.isCollection() && config.getElementType() != null && 
                      !isBasicElementType(config.getElementType()));

        if (hasNestedFilters || hasModelTypeCollections) {
            out.println("            default -> {");
            out.println("                // Handle nested filter paths and model type collection paths");
            out.println("                handleNestedFilterPath(filter, mappedPath, paramValue, deserializer, collectionHandler, registry);");
            out.println(EXP_6);
        } else {
            out.println("            default -> {");
            out.println("                // Unknown operator - silently ignore");
            out.println("                // This handles paths with extra segments or unknown operators gracefully");
            out.println(EXP_6);
        }

        out.println(EXP_10);
        out.println(EXP_11);
        out.println();

        // Generate the handleNestedFilterPath method if there are nested filters or model type collections
        if (hasNestedFilters || hasModelTypeCollections) {
            generateHandleNestedFilterPathMethod(out, filterClassName, fieldConfigs);
        }
    }

    /**
     * Generates switch cases for all field.operator combinations in
     * bindQueryParameters method.
     */
    private void generateBindQueryParametersSwitchCases(PrintWriter out, List<FilterFieldConfig> fieldConfigs) {
        for (FilterFieldConfig config : fieldConfigs) {
            String fieldName = config.getFieldName();
            String filterType = config.getFilterType();
            String fieldType = config.getFieldType();

            if (config.isModel()) {
                continue;
            }

            // Handle CollectionFilter fields with nested operators
            if (config.isCollection()) {
                String elementType = config.getElementType();
                if (elementType == null || elementType.isEmpty()) {
                    elementType = "Object";
                }
                
                // Check if element type is a model type (not basic type - isBasicElementType already checks for enums)
                boolean isModelElementType = !isBasicElementType(elementType);
                boolean isCollectionShapedElementType = isCollectionShapedType(elementType);
                
                if (!isModelElementType && !isCollectionShapedElementType) {
                    // For basic types and enums, generate cases for nested filter operators (any, all, none)
                    // These handle paths like: tags.any.cont, numbers.all.gte, tags.none.eq
                    String[] nestedOperators = {FilterConstants.FIELD_ANY, FilterConstants.FIELD_ALL, FilterConstants.FIELD_NONE};
                    String[] elementOperators = getElementOperatorsForType(elementType);
                    
                    for (String nestedOp : nestedOperators) {
                        for (String elementOp : elementOperators) {
                            generateNestedFilterBinding(out, fieldName, elementType, nestedOp, elementOp);
                        }
                    }
                }
                // Collection-shaped element types (e.g., List<List<String>>) are not
                // supported for per-element any/all/none filtering in this scope.
                // For model types, we'll handle them in the default case since we don't know all possible nested paths
                
                // Also generate cases for direct collection operators
                // These handle paths like: tags.cont, tags.empty, tags.nempty
                generateCollectionDirectOperatorBindCase(out, fieldName, elementType, FilterConstants.FIELD_CONT, "setCollectionContains");
                generateBindCaseBooleanOperator(out, fieldName, "CollectionFilter<" + elementType + ">", FilterConstants.FIELD_EMPTY, "setIsEmpty");
                generateBindCaseBooleanOperator(out, fieldName, "CollectionFilter<" + elementType + ">", FilterConstants.FIELD_NEMPTY, "setIsNotEmpty");
                
                // Skip base operators for collections as they have different semantics
                continue;
            }

            // Generate cases for common operators (all filter types support these)
            generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_EQ, "setEquals", false);
            generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NEQ, "setNotEquals", false);
            generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_IN, "setIn", true);
            generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NIN, "setNotIn", true);
            generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_ISN, "setIsNull");
            generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_ISNN, "setIsNotNull");

            // Generate cases for filter-type-specific operators
            if (config.isString()) {
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_CONT, "setContains", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_START, "setStartsWith", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_END, "setEndsWith", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_MATCH, "setMatches", false);
                generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_EMPTY, "setIsEmpty");
                generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_NEMPTY, "setIsNotEmpty");
                generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_BLANK, "setIsBlank");
                generateBindCaseBooleanOperator(out, fieldName, filterType, FilterConstants.FIELD_NBLANK, "setIsNotBlank");
            } else if (config.isNumeric()) {
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_GT, "setGreaterThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_GTE, "setGreaterOrEqualThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_LT, "setLessThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_LTE, "setLessOrEqualThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NGT, "setNotGreaterThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NLT, "setNotLessThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NGTE, "setNotGreaterOrEqualThan", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NLTE, "setNotLessOrEqualThan", false);
            } else if (config.isTemporal()) {
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_BEFORE, "setIsBefore", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_AFTER, "setIsAfter", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_ON_OR_BEFORE, "setIsOnOrBefore", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_ON_OR_AFTER, "setIsOnOrAfter", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NOT_BEFORE, "setNotIsBefore", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NOT_AFTER, "setNotIsAfter", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NOT_ON_OR_BEFORE, "setNotIsOnOrBefore", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NOT_ON_OR_AFTER, "setNotIsOnOrAfter", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_LAST, "setLast", false);
                generateBindCase(out, fieldName, filterType, fieldType, FilterConstants.FIELD_NEXT, "setNext", false);
            }
        }
    }
    
    /**
     * Checks if an element type is a basic type (not a model type).
     * Basic types include: String, Integer, Long, Double, Boolean, LocalDate, LocalDateTime, Instant.
     * Note: Enums are checked separately using FilterFieldConfig.isEnum().
     * 
     * @param elementType the element type to check
     * @return true if the element type is a basic type, false otherwise
     */
    /**
     * Checks if an element type is itself a collection-shaped type (List, Set,
     * Collection, or common implementations). Used to detect "legitimate nesting"
     * cases like List&lt;List&lt;String&gt;&gt; where the collection's element type
     * is itself a collection. Such element types have no corresponding *Filter class
     * and per-element (any/all/none) filtering is not supported for them.
     *
     * @param elementType the (erased) element type to check
     * @return true if the element type is a collection-shaped type
     */
    private boolean isCollectionShapedType(String elementType) {
        if (elementType == null) {
            return false;
        }
        String simpleType = getSimpleClassName(elementType);
        return simpleType.equals("List")
                || simpleType.equals("Set")
                || simpleType.equals("Collection")
                || simpleType.equals("ArrayList")
                || simpleType.equals("HashSet")
                || simpleType.equals("LinkedList")
                || simpleType.equals("TreeSet");
    }

    private boolean isBasicElementType(String elementType) {
        String simpleType = getSimpleClassName(elementType);

        // Collection-shaped element types (e.g., List<List<String>>) have no
        // corresponding *Filter class and are treated as "basic" here to avoid
        // attempting model-type filter class resolution for them.
        if (isCollectionShapedType(elementType)) {
            return true;
        }

        // Check if it's a basic type
        boolean isBasic = simpleType.equals("String") ||
               simpleType.equals("Integer") || simpleType.equals("int") ||
               simpleType.equals("Long") || simpleType.equals("long") ||
               simpleType.equals("Double") || simpleType.equals("double") ||
               simpleType.equals("Boolean") || simpleType.equals("boolean") ||
               simpleType.equals("LocalDate") ||
               simpleType.equals("LocalDateTime") ||
               simpleType.equals("Instant");
        
        if (isBasic) {
            return true;
        }
        
        // Check if it's an enum type (enums are also considered basic for collection purposes)
        // They use EnumFilter<E> instead of a custom filter class
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();
        javax.lang.model.element.TypeElement typeElement = elementUtils.getTypeElement(elementType);
        if (typeElement != null && typeElement.getKind() == javax.lang.model.element.ElementKind.ENUM) {
            return true; // Enums are treated as basic types for collections
        }
        
        return false;
    }
    
    /**
     * Gets the list of element operators that are applicable for a given element type.
     * 
     * @param elementType the type of elements in the collection
     * @return array of operator constants
     */
    private String[] getElementOperatorsForType(String elementType) {
        String simpleType = getSimpleClassName(elementType);
        
        // Determine which operators are applicable based on element type
        if (simpleType.equals("String")) {
            return new String[]{
                FilterConstants.FIELD_EQ, FilterConstants.FIELD_NEQ,
                FilterConstants.FIELD_IN, FilterConstants.FIELD_NIN,
                FilterConstants.FIELD_CONT, FilterConstants.FIELD_START, 
                FilterConstants.FIELD_END, FilterConstants.FIELD_MATCH
            };
        } else if (simpleType.equals("Integer") || simpleType.equals("Long") || 
                   simpleType.equals("Double") || simpleType.equals("int") || 
                   simpleType.equals("long") || simpleType.equals("double")) {
            return new String[]{
                FilterConstants.FIELD_EQ, FilterConstants.FIELD_NEQ,
                FilterConstants.FIELD_IN, FilterConstants.FIELD_NIN,
                FilterConstants.FIELD_GT, FilterConstants.FIELD_GTE,
                FilterConstants.FIELD_LT, FilterConstants.FIELD_LTE
            };
        } else if (simpleType.equals("LocalDate") || simpleType.equals("LocalDateTime") || 
                   simpleType.equals("Instant")) {
            return new String[]{
                FilterConstants.FIELD_EQ, FilterConstants.FIELD_NEQ,
                FilterConstants.FIELD_IN, FilterConstants.FIELD_NIN,
                FilterConstants.FIELD_BEFORE, FilterConstants.FIELD_AFTER,
                FilterConstants.FIELD_ON_OR_BEFORE, FilterConstants.FIELD_ON_OR_AFTER,
                FilterConstants.FIELD_LAST, FilterConstants.FIELD_NEXT
            };
        } else if (simpleType.equals("Boolean") || simpleType.equals("boolean")) {
            return new String[]{
                FilterConstants.FIELD_EQ, FilterConstants.FIELD_NEQ,
                FilterConstants.FIELD_IN, FilterConstants.FIELD_NIN
            };
        } else {
            // For enum or unknown types, support basic equality operators
            return new String[]{
                FilterConstants.FIELD_EQ, FilterConstants.FIELD_NEQ,
                FilterConstants.FIELD_IN, FilterConstants.FIELD_NIN
            };
        }
    }
    
    /**
     * Generates a bind case for direct collection operators (cont).
     * These operators work on the collection elements directly, not on the collection itself.
     */
    private void generateCollectionDirectOperatorBindCase(PrintWriter out, String fieldName, 
                                                          String elementType, String operator, 
                                                          String setterMethod) {
        String capitalizedFieldName = capitalize(fieldName);
        String getterMethod = FILTER_GETTER_PREFIX + capitalizedFieldName;
        String setterFieldMethod = FILTER_SETTER_PREFIX + capitalizedFieldName;
        String simpleType = getSimpleClassName(elementType);
        
        out.println(FIELD_CASE_PREFIX + fieldName + "." + operator + EXP_8);
        out.println("                if (filter." + getterMethod + "() == null) {");
        out.println("                    filter." + setterFieldMethod + "(new CollectionFilter<>());");
        out.println(EXP_5);
        out.println("                ");
        out.println("                try {");
        out.println("                    " + simpleType + " value = deserializer.deserializeValue(");
        out.println("                        paramValue, " + simpleType + ".class,");
        out.println("                        registry.getConfigForType(" + simpleType + ".class)");
        out.println("                    );");
        out.println("                    ");
        out.println("                    filter." + getterMethod + "()." + setterMethod + "(value);");
        out.println("                } catch (Exception e) {");
        out.println("                    throw new IllegalArgumentException(");
        out.println("                        \"Cannot parse collection element '\" + paramValue + \"' as \" + ");
        out.println("                        \"" + simpleType + "\" + \" for field '\" + \"" + fieldName + "." + operator + "\" + \"': \" + e.getMessage(),");
        out.println("                        e");
        out.println("                    );");
        out.println(EXP_5);
        out.println(EXP_6);
    }

    /**
     * Generates a single bind case for a field.operator combination. Generates
     * fully typed code with zero reflection.
     */
    private void generateBindCase(PrintWriter out, String fieldName, String filterType, String fieldType,
                                  String operator, String setterMethod, boolean isCollection) {
        String getterMethod = FILTER_GETTER_PREFIX + capitalize(fieldName);
        String setterFieldMethod = FILTER_SETTER_PREFIX + capitalize(fieldName);
        String simpleFieldType = getSimpleTypeName(fieldType);

        // Check if this is a temporal type that needs special parsing
        boolean isTemporal = isTemporalType(fieldType);
        boolean isPresetOperator = operator.equals(FilterConstants.FIELD_LAST)
            || operator.equals(FilterConstants.FIELD_NEXT);

        out.println(FIELD_CASE_PREFIX + fieldName + "." + operator + EXP_8);
        out.println("                if (filter." + getterMethod + "() == null) {");
        out.println("                    filter." + setterFieldMethod + "(new " + filterType + "());");
        out.println(EXP_5);
        out.println("                ");

        if (isCollection) {
            // Collection operators (in, nin)
            if (isTemporal) {
                // For temporal types, use custom parse method for each element
                String parseMethodName = PARSE + simpleFieldType + "_" + fieldName;
                out.println("                java.util.List<" + simpleFieldType + "> values = new java.util.ArrayList<>();");
                out.println("                String[] parts = paramValue.split(\",\");");
                out.println("                for (String part : parts) {");
                out.println("                    values.add(" + parseMethodName + "(part.trim()));");
                out.println(EXP_5);
            } else {
                out.println("                java.util.List<" + simpleFieldType + "> values = collectionHandler.parseCommaSeparatedValues(");
                out.println("                    paramValue, " + simpleFieldType + ".class, ");
                out.println("                    registry.getConfigForType(" + simpleFieldType + ".class)");
                out.println("                );");
            }
            out.println("                ");
            out.println("                filter." + getterMethod + "()." + setterMethod + "(values);");
        } else {
            // Single value operators
            if (isPresetOperator) {
                out.println("                com.thy.fss.common.inmemory.filter.TemporalPreset value = deserializer.deserializeTemporalPreset(paramValue);");
            } else if (isTemporal) {
                // For temporal types, use custom parse method
                String parseMethodName = PARSE + simpleFieldType + "_" + fieldName;
                out.println("                " + simpleFieldType + " value = " + parseMethodName + "(paramValue);");
            } else {
                out.println("                " + simpleFieldType + " value = deserializer.deserializeValue(");
                out.println("                    paramValue, " + simpleFieldType + ".class,");
                out.println("                    registry.getConfigForType(" + simpleFieldType + ".class)");
                out.println("                );");
            }
            out.println("                ");
            out.println("                filter." + getterMethod + "()." + setterMethod + "(value);");
        }

        out.println(EXP_6);
    }

    /**
     * Checks if a field type is a temporal type (LocalDate, LocalDateTime,
     * Instant).
     */
    private boolean isTemporalType(String fieldType) {
        return fieldType != null && (fieldType.contains("LocalDate")
                || fieldType.contains("LocalDateTime")
                || fieldType.contains("Instant"));
    }

    /**
     * Generates a bind case for boolean operators (isn, isnn, empty, etc.).
     * Generates fully typed code with zero reflection.
     */
    private void generateBindCaseBooleanOperator(PrintWriter out, String fieldName, String filterType,
                                                 String operator, String setterMethod) {
        String getterMethod = FILTER_GETTER_PREFIX + capitalize(fieldName);
        String setterFieldMethod = FILTER_SETTER_PREFIX + capitalize(fieldName);

        out.println(FIELD_CASE_PREFIX + fieldName + "." + operator + EXP_8);
        out.println("                if (filter." + getterMethod + "() == null) {");
        out.println("                    filter." + setterFieldMethod + "(new " + filterType + "());");
        out.println(EXP_5);
        out.println("                ");
        out.println("                try {");
        out.println("                    Boolean value = Boolean.parseBoolean(paramValue);");
        out.println("                    ");
        out.println("                    filter." + getterMethod + "()." + setterMethod + "(value);");
        out.println("                } catch (Exception e) {");
        out.println("                    throw new IllegalArgumentException(");
        out.println("                        \"Cannot parse boolean value '\" + paramValue + \"' for operator '\" + ");
        out.println("                        \"" + operator + "\" + \"' on field '\" + \"" + fieldName + "\" + \"': \" + e.getMessage(),");
        out.println("                        e");
        out.println("                    );");
        out.println(EXP_5);
        out.println(EXP_6);
    }

    /**
     * Generates the handleNestedFilterPath method that handles nested filter
     * paths by delegating to the nested filter's deserializer bindParameter
     * method.
     */
    private void generateHandleNestedFilterPathMethod(PrintWriter out, String filterClassName,
                                                      List<FilterFieldConfig> fieldConfigs) {
        // Find all nested filter fields and model type collection fields
        java.util.List<FilterFieldConfig> nestedFilterFields = new java.util.ArrayList<>();
        java.util.List<FilterFieldConfig> modelTypeCollectionFields = new java.util.ArrayList<>();
        
        for (FilterFieldConfig config : fieldConfigs) {
            if (config.isModel()) {
                nestedFilterFields.add(config);
            } else if (config.isCollection() && config.getElementType() != null && 
                       !isBasicElementType(config.getElementType())) {
                // Model type collection field (element type is not basic - which includes enums)
                modelTypeCollectionFields.add(config);
            }
        }

        if (nestedFilterFields.isEmpty() && modelTypeCollectionFields.isEmpty()) {
            // No nested filters or model type collections, generate a simple no-op method
            debugLog("No nested filter fields or model type collections found, skipping handleNestedFilterPath generation for filter " + filterClassName);
            return;
        }

        // Generate method with nested filter handling
        out.println(EXP_7);
        out.println("     * Handles nested filter paths by delegating to nested filter's deserializer.");
        out.println("     * Supports multi-level nested paths (e.g., \"address.city.eq\").");
        out.println("     * Supports model type collection paths (e.g., \"users.any.name.eq\").");
        out.println("     * Uses the nested filter's deserializer to properly handle nested field operations.");
        out.println(EXP_1);
        out.println("    private static void handleNestedFilterPath(");
        out.println("            " + filterClassName + " filter,");
        out.println("            String mappedPath,");
        out.println("            String paramValue,");
        out.println("            com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer,");
        out.println("            com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler,");
        out.println("            com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry) throws Exception {");
        out.println();
        
        // Handle model type collection paths first (e.g., users.any.name.eq)
        if (!modelTypeCollectionFields.isEmpty()) {
            out.println("        // Handle model type collection nested filter paths");
            for (FilterFieldConfig config : modelTypeCollectionFields) {
                String fieldName = config.getFieldName();
                String elementType = config.getElementType();
                String elementFilterType = getFilterTypeForElementType(elementType);
                String capitalizedFieldName = capitalize(fieldName);
                String getterMethod = FILTER_GETTER_PREFIX + capitalizedFieldName;
                String setterFieldMethod = FILTER_SETTER_PREFIX + capitalizedFieldName;
                
                // Generate handling for any, all, none operators
                String[] nestedOperators = {FilterConstants.FIELD_ANY, FilterConstants.FIELD_ALL, FilterConstants.FIELD_NONE};
                for (String nestedOp : nestedOperators) {
                    String pathPrefix = fieldName + "." + nestedOp + ".";
                    String collectionSetterMethod = switch (nestedOp) {
                        case FilterConstants.FIELD_ANY -> "setCollectionAny";
                        case FilterConstants.FIELD_ALL -> "setCollectionAll";
                        case FilterConstants.FIELD_NONE -> "setCollectionNone";
                        default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOp);
                    };
                    String collectionGetterMethod = switch (nestedOp) {
                        case FilterConstants.FIELD_ANY -> "getCollectionAny";
                        case FilterConstants.FIELD_ALL -> "getCollectionAll";
                        case FilterConstants.FIELD_NONE -> "getCollectionNone";
                        default -> throw new IllegalArgumentException("Unknown nested operator: " + nestedOp);
                    };
                    
                    out.println("        if (mappedPath.startsWith(\"" + pathPrefix + "\")) {");
                    out.println("            // Ensure collection filter is initialized");
                    out.println("            if (filter." + getterMethod + "() == null) {");
                    out.println("                filter." + setterFieldMethod + "(new com.thy.fss.common.inmemory.filter.CollectionFilter<>());");
                    out.println(EXP_6);
                    out.println("            ");
                    out.println("            // Get or create the nested element filter (FilterBase<E>)");
                    out.println("            com.thy.fss.common.inmemory.filter.FilterBase<" + getSimpleClassName(elementType) + "> elementFilterBase = filter." + getterMethod + "()." + collectionGetterMethod + "();");
                    out.println("            " + elementFilterType + " elementFilter;");
                    out.println("            ");
                    out.println("            // Type-safe cast from FilterBase to concrete filter type");
                    out.println("            if (elementFilterBase instanceof " + elementFilterType + ") {");
                    out.println("                elementFilter = (" + elementFilterType + ") elementFilterBase;");
                    out.println("            } else if (elementFilterBase == null) {");
                    out.println("                try {");
                    out.println("                    elementFilter = new " + elementFilterType + "();");
                    out.println("                } catch (Exception instantiationEx) {");
                    out.println("                    throw new IllegalArgumentException(");
                    out.println("                        \"Cannot create instance of element filter '\" + \"" + elementFilterType + "\" + ");
                    out.println("                        \"' for collection field '\" + \"" + fieldName + "\" + \"': \" + instantiationEx.getMessage() + \". \" +");
                    out.println("                        \"Ensure the filter class has a public no-argument constructor.\",");
                    out.println("                        instantiationEx");
                    out.println("                    );");
                    out.println("                }");
                    out.println("                filter." + getterMethod + "()." + collectionSetterMethod + "(elementFilter);");
                    out.println("            } else {");
                    out.println("                throw new IllegalStateException(\"Unexpected filter type: \" + elementFilterBase.getClass().getName());");
                    out.println(EXP_6);
                    out.println("            ");
                    out.println("            // Extract remaining path after collection operator");
                    out.println("            String remainingPath = mappedPath.substring(\"" + pathPrefix + "\".length());");
                    out.println("            ");
                    out.println("            // Delegate to element filter's deserializer bindParameter method");
                    out.println("            " + elementFilterType + "Deserializer.bindParameter(");
                    out.println("                elementFilter,");
                    out.println("                remainingPath,");
                    out.println("                paramValue,");
                    out.println("                deserializer,");
                    out.println("                collectionHandler,");
                    out.println("                registry");
                    out.println("            );");
                    out.println("            return;");
                    out.println(EXP_10);
                }
            }
            out.println();
        }
        
        out.println("        int firstDotIndex = mappedPath.indexOf('.');");
        out.println("        if (firstDotIndex == -1) {");
        out.println("            return;");
        out.println(EXP_10);
        out.println();
        out.println("        String firstSegment = mappedPath.substring(0, firstDotIndex);");
        out.println("        String remainingPath = mappedPath.substring(firstDotIndex + 1);");
        out.println();
        out.println("        // Check if the first segment is a nested filter field");
        out.println("        switch (firstSegment) {");

        // Generate cases for each nested filter field
        for (FilterFieldConfig config : nestedFilterFields) {
            String fieldName = config.getFieldName();
            String filterType = config.getFilterType();
            String getterMethod = FILTER_GETTER_PREFIX + capitalize(fieldName);
            String setterMethod = FILTER_SETTER_PREFIX + capitalize(fieldName);

            out.println(FIELD_CASE_PREFIX + fieldName + EXP_8);
            out.println("                // Ensure nested filter is initialized");
            out.println("                if (filter." + getterMethod + "() == null) {");
            out.println("                    filter." + setterMethod + "(new " + filterType + "());");
            out.println(EXP_5);
            out.println();
            out.println("                " + filterType + "Deserializer.bindParameter(");
            out.println("                    filter." + getterMethod + "(),");
            out.println("                    remainingPath,");
            out.println("                    paramValue,");
            out.println("                    deserializer,");
            out.println("                    collectionHandler,");
            out.println("                    registry");
            out.println("                );");
            out.println(EXP_6);
        }

        out.println("            default -> {");
        out.println("                // Unknown nested field - throw error with helpful message");
        out.println("                throw new IllegalArgumentException(");
        out.println("                    \"Field '\" + firstSegment + \"' not found on filter type '" + filterClassName + "' \" +");
        out.println("                    \"in path '\" + mappedPath + \"'. \" +");
        out.println("                    \"Valid nested fields are: " + nestedFilterFields.stream().map(FilterFieldConfig::getFieldName).collect(java.util.stream.Collectors.joining(", ")) + "\"");
        out.println("                );");
        out.println(EXP_6);
        out.println(EXP_10);
        out.println(EXP_11);
        out.println();
    }

}
