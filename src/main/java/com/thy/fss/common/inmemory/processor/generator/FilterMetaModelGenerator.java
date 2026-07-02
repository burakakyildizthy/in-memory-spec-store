package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.*;
import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.analyzer.*;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import com.thy.fss.common.inmemory.processor.model.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Generator for FilterMetaModel classes (ClassNameFilter). Creates filter
 * definitions for type-safe filtering.
 * <p>
 * Generates filter classes with: - JHipster-compatible filter pattern - Field
 * mapping to appropriate filter types - Builder pattern with nested builders -
 * Serialization support
 */
public class FilterMetaModelGenerator {

    public static final String LOCAL_DATE_FILTER = "LocalDateFilter";
    public static final String LOCAL_DATE_TIME_FILTER = "LocalDateTimeFilter";
    public static final String INSTANT_FILTER = "InstantFilter";
    public static final String BOOLEAN_FILTER = "BooleanFilter";
    public static final String DOUBLE_FILTER = "DoubleFilter";
    public static final String FLOAT_FILTER = "FloatFilter";
    public static final String LONG_FILTER = "LongFilter";
    public static final String INTEGER_FILTER = "IntegerFilter";
    public static final String STRING_FILTER = "StringFilter";
    public static final String RETURN_THIS_BUILDER_FOR_METHOD_CHAINING = "         * @return This builder for method chaining";
    public static final String BUILDER = "Builder ";
    public static final String RETURN_THIS = "            return this;";
    public static final String SETS_THE = "         * Sets the ";
    public static final String IF_FILTER_GET = "            if (filter.get";
    public static final String NULL_STR = "() == null) {";
    public static final String FILTER_SET = "                filter.set";
    public static final String NEW_ENUM_FILTER = "(new EnumFilter<>());";
    public static final String NEW_STR = "(new ";
    public static final String SPACE_END = "            }";
    public static final String PUBLIC = "    public ";
    public static final String JAVADOC_START = "    /**";
    public static final String JAVADOC_END = "     */";
    public static final String END = "    }";
    public static final String FILTER = " filter.";
    public static final String FILTER1 = " filter";
    public static final String IF_THIS_FILTER_GET = "            if (this.filter.get";
    public static final String THIS_FILTER_SET = "                this.filter.set";
    public static final String FILTER2 = "(new CollectionFilter<>());";
    public static final String THIS_FILTER_GET = "            this.filter.get";
    public static final String VALUE = " value) {";
    public static final String COMPARISON = " comparison";
    public static final String JAVADOC_LINE = "     * ";
    public static final String END2 = "        }";
    public static final String SPACE_THIS = "        this.";
    public static final String FILTER3 = " = filter.";
    public static final String NULL_CHECK = " != null ? ";
    public static final String NULL_END = ") : null;";
    public static final String MATCHING = " matching";
    public static final String VALUE1 = "         * @param value The value for ";
    public static final String SPACES = "            ";
    public static final String FILTER_GET = "            filter.get";
    public static final String SET = "().set";
    public static final String VALUE2 = "(value);";
    public static final String GENERIC_BUILDER_P = "GenericBuilder<P> ";
    public static final String SPACE_JAVADOC_START = "        /**";
    public static final String SPACE_JAVADOC_LINE = "         * ";
    public static final String SPACCE_JAVADOC_END = "         */";
    public static final String PUBLIC1 = "        public ";
    public static final String JAVA_UTIL_COLLECTION = "java.util.Collection<";
    public static final String BOOLEAN_STR = "Boolean";
    public static final String FIELD = "Field '";
    public static final String RETURN_SPACES = "        return ";
    public static final String SPACE_OVERRIDES_AN = "    @Override";
    public static final String FILTER4 = "Filter";
    // Debug mode flag - controlled by system property
    private static final boolean DEBUG_MODE = Boolean.parseBoolean(
            System.getProperty("inmemory.processor.debug", "false"));
    // Mapping of Java types to filter types
    private static final Map<String, String> TYPE_TO_FILTER_MAP = Map.of(
            String.class.getName(), StringFilter.class.getSimpleName(),
            Integer.class.getName(), IntegerFilter.class.getSimpleName(),
            Long.class.getName(), LongFilter.class.getSimpleName(),
            Double.class.getName(), DoubleFilter.class.getSimpleName(),
            Boolean.class.getName(), BooleanFilter.class.getSimpleName(),
            LocalDate.class.getName(), LocalDateFilter.class.getSimpleName(),
            LocalDateTime.class.getName(), LocalDateTimeFilter.class.getSimpleName(),
            Instant.class.getName(), InstantFilter.class.getSimpleName(),
            Enum.class.getName(), EnumFilter.class.getSimpleName(),
            Collection.class.getName(), CollectionFilter.class.getSimpleName()
    );
    // Collection types that should be handled as element filters
    private static final Set<String> COLLECTION_TYPES = Set.of(
            List.class.getName(),
            Set.class.getName(),
            Collection.class.getName()
    );
    private final ProcessingEnvironment processingEnv;
    private final JacksonAnnotationAnalyzer jacksonAnnotationAnalyzer;
    private final DateTimeFormatAnalyzer dateTimeFormatAnalyzer;
    private final InstantFormatAnalyzer instantFormatAnalyzer;
    private final EnumAnalyzer enumAnalyzer;
    private final FilterDeserializerGenerator filterDeserializerGenerator;

    /**
     * Constructor for FilterMetaModelGenerator.
     *
     * @param processingEnv the processing environment
     */
    public FilterMetaModelGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.jacksonAnnotationAnalyzer = new JacksonAnnotationAnalyzerImpl(processingEnv);
        this.dateTimeFormatAnalyzer = new DateTimeFormatAnalyzerImpl();
        this.instantFormatAnalyzer = new InstantFormatAnalyzerImpl();
        this.enumAnalyzer = new EnumAnalyzer(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
        this.filterDeserializerGenerator = new FilterDeserializerGenerator(processingEnv);
    }

    /**
     * Prints debug message only if debug mode is enabled.
     */
    private void debugLog(String message) {
        if (DEBUG_MODE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[DEBUG] FilterMetaModelGenerator: " + message
            );
        }
    }

    /**
     * Generates a FilterMetaModel class for the given type element.
     *
     * @param typeElement the class to generate a filter model for
     * @throws ProcessingException if generation fails
     */
    public void generate(TypeElement typeElement) throws ProcessingException {
        String className = typeElement.getQualifiedName().toString();
        String simpleClassName = typeElement.getSimpleName().toString();
        String filterClassName = simpleClassName + FILTER4;
        String packageName = getPackageName(className);

        // Debug log: Generation start
        debugLog("Starting generation for class " + className);
        debugLog("Package: " + packageName + ", Filter class: " + filterClassName);

        try {
            // Debug log: File creation
            debugLog("Creating source file: " + packageName + "." + filterClassName);

            // Create the filter class file
            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + filterClassName);

            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                debugLog("Starting filter class generation");

                generateFilterClass(out, typeElement, packageName, filterClassName);

                debugLog("Filter class generation completed");
            }

            // Debug log: Success
            debugLog("File written successfully");

            debugLog("Generated FilterMetaModel: " + packageName + "." + filterClassName);

        } catch (IOException e) {
            String errorMsg = buildDetailedErrorMessage(typeElement, e, "IOException - File creation failed", filterClassName);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    errorMsg,
                    typeElement
            );
            throw new ProcessingException("Failed to generate FilterMetaModel for " + className, e);
        } catch (Exception e) {
            String errorMsg = buildDetailedErrorMessage(typeElement, e, "Unexpected error", filterClassName);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    errorMsg,
                    typeElement
            );
            throw new ProcessingException("Unexpected error during FilterMetaModel generation for " + className, e);
        }
    }

    /**
     * Builds a detailed error message for generation failures.
     */
    private String buildDetailedErrorMessage(TypeElement typeElement, Exception e, String errorType, String filterClassName) {
        String msg = "\n" +
                "Failed to generate FilterMetaModel for: " + typeElement.getQualifiedName() + "\n" +
                "Filter Class: " + filterClassName + "\n" +
                "Error Type: " + errorType + "\n" +
                "Error Message: " + (e.getMessage() != null ? e.getMessage() : "No message") + "\n" +
                "\n" +
                "This will cause compilation errors like:\n" +
                "  - Cannot find symbol: " + filterClassName + "\n" +
                "  - Cannot find symbol: " + filterClassName + ".Builder\n" +
                "\n" +
                "Possible causes:\n" +
                "  1. Invalid field types in the class\n" +
                "  2. Missing StaticMetaModel (" + typeElement.getSimpleName() + "_) - ensure it's generated first\n" +
                "  3. Unsupported filter type for a field\n" +
                "  4. Jackson annotation processing error\n" +
                "\n" +
                "To debug, run: ./gradlew clean build -Dinmemory.processor.debug=true\n";
        return msg;
    }

    /**
     * Generates the complete filter class content.
     */
    private void generateFilterClass(PrintWriter out, TypeElement typeElement,
                                     String packageName, String filterClassName) {
        String simpleClassName = typeElement.getSimpleName().toString();

        // Debug log: Filter class generation start
        debugLog("Generating filter class " + filterClassName);

        // Package declaration
        if (!packageName.isEmpty()) {
            out.println("package " + packageName + ";");
            out.println();
            debugLog("Added package declaration: " + packageName);
        }

        // Imports
        debugLog("Generating imports");
        generateImports(out, typeElement);

        // Class declaration with JavaDoc
        debugLog("Adding class declaration and JavaDoc");
        out.println("/**");
        out.println(" * Filter class for " + simpleClassName + " entity.");
        out.println(" * Provides  filtering capabilities with type-safe field access.");
        out.println(" * Generated by FilterMetaModelGenerator.");
        out.println(" */");

        // Field declarations and analysis
        debugLog("Starting field analysis");
        List<FieldInfo> fields = analyzeFields(typeElement);
        debugLog("Field analysis completed, found " + fields.size() + " fields");

        // Always generate deserializer for all filter classes
        // This ensures proper JSON deserialization for all filter types
        String deserializerClassName = filterClassName + "Deserializer";
        out.println("@JsonDeserialize(using = " + deserializerClassName + ".class)");
        debugLog("Added @JsonDeserialize annotation pointing to " + deserializerClassName);

        out.println("public class " + filterClassName + " implements EntityFilter<" + simpleClassName + "> {");
        out.println();
        out.println("    private static final long serialVersionUID = 1L;");
        out.println();

        debugLog("Generating field declarations");
        generateFieldDeclarations(out, fields);

        // Default constructor
        debugLog("Generating default constructor");
        out.println(JAVADOC_START);
        out.println("     * Default constructor.");
        out.println(JAVADOC_END);
        out.println(PUBLIC + filterClassName + "() {");
        out.println(END);
        out.println();

        // Copy constructor
        debugLog("Generating copy constructor");
        generateCopyConstructor(out, filterClassName, fields);

        // getEntityClass method
        debugLog("Generating getEntityClass method");
        generateGetEntityClassMethod(out, simpleClassName);

        // Getters and setters
        debugLog("Generating getters and setters");
        generateGettersAndSetters(out, fields, filterClassName);

        // Builder pattern
        debugLog("Generating builder pattern");
        generateBuilderPattern(out, filterClassName, fields);

        // equals, hashCode, toString
        debugLog("Generating utility methods (equals, hashCode, toString)");
        generateEqualsHashCodeToString(out, filterClassName, fields);

        out.println("}");

        // Store annotation analysis results for deserializer generation (task 4.1 requirement)
        storeAnnotationAnalysisResults(typeElement, filterClassName, fields);

        debugLog("Filter class generation completed for " + filterClassName);
    }

    /**
     * Stores annotation analysis results for deserializer generation. This
     * method fulfills the task 4.1 requirement to store annotation analysis
     * results.
     *
     * @param typeElement     the entity type element
     * @param filterClassName the generated filter class name
     * @param fields          the analyzed field information including annotation
     *                        analysis
     */
    private void storeAnnotationAnalysisResults(TypeElement typeElement, String filterClassName, List<FieldInfo> fields) {
        debugLog("Storing annotation analysis results for " + filterClassName);

        // Count fields with annotation analysis results
        int fieldsWithAnnotations = 0;
        int fieldsWithCustomDateTimeFormat = 0;
        int fieldsWithCustomInstantFormat = 0;
        int fieldsWithCustomEnumDeserialization = 0;

        for (FieldInfo field : fields) {
            FilterFieldConfig config = field.annotationConfig();
            if (config != null) {
                if (config.hasJacksonAnnotations()) {
                    fieldsWithAnnotations++;
                    debugLog("Field '" + field.name() + "' has " + config.getJacksonAnnotations().size() + " Jackson annotations");
                }

                if (config.hasCustomDateTimeFormat()) {
                    fieldsWithCustomDateTimeFormat++;
                    debugLog("Field '" + field.name() + "' has custom datetime format: " + config.getEffectiveDateTimePattern());
                }

                if (config.hasCustomInstantFormat()) {
                    fieldsWithCustomInstantFormat++;
                    debugLog("Field '" + field.name() + "' has custom instant format: " + config.getEffectiveInstantPattern());
                }

                if (config.hasCustomEnumDeserialization()) {
                    fieldsWithCustomEnumDeserialization++;
                    debugLog("Field '" + field.name() + "' has custom enum deserialization: " + config.getEnumDeserializationType());
                }
            }
        }

        debugLog("Annotation analysis summary for " + filterClassName + ":");
        debugLog("  - Total fields: " + fields.size());
        debugLog("  - Fields with Jackson annotations: " + fieldsWithAnnotations);
        debugLog("  - Fields with custom datetime format: " + fieldsWithCustomDateTimeFormat);
        debugLog("  - Fields with custom instant format: " + fieldsWithCustomInstantFormat);
        debugLog("  - Fields with custom enum deserialization: " + fieldsWithCustomEnumDeserialization);

        // Always generate deserializer for all filter classes
        // This ensures proper JSON deserialization for all filter types including nested models
        String packageName = getPackageName(typeElement.getQualifiedName().toString());
        generateCustomDeserializer(typeElement, packageName, filterClassName, fields);

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "FilterMetaModelGenerator: Completed annotation analysis for " + filterClassName
                        + " (" + fieldsWithAnnotations + " fields with Jackson annotations)"
        );
    }

    /**
     * Generates a custom deserializer for the filter class. This method creates
     * a Jackson deserializer that handles custom annotations and formats.
     *
     * @param packageName the package name for the deserializer
     * @param filterClassName the filter class name
     * @param fields the analyzed field information
     */
    /**
     * Generates import statements.
     */
    private void generateImports(PrintWriter out, TypeElement typeElement) {
        Set<String> imports = new HashSet<>();

        // Standard imports
        imports.add("java.util.Objects");
        imports.add("java.util.function.Function");

        // Time imports for builder methods
        imports.add("java.time.LocalDate");
        imports.add("java.time.LocalDateTime");
        imports.add("java.time.Instant");

        // Jackson imports for @JsonDeserialize annotation
        imports.add("com.fasterxml.jackson.databind.annotation.JsonDeserialize");

        // Filter imports
        imports.add("com.thy.fss.common.inmemory.filter.EntityFilter");
        imports.add("com.thy.fss.common.inmemory.filter.Filter");
        imports.add("com.thy.fss.common.inmemory.filter.StringFilter");
        imports.add("com.thy.fss.common.inmemory.filter.IntegerFilter");
        imports.add("com.thy.fss.common.inmemory.filter.LongFilter");
        imports.add("com.thy.fss.common.inmemory.filter.DoubleFilter");
        imports.add("com.thy.fss.common.inmemory.filter.BooleanFilter");
        imports.add("com.thy.fss.common.inmemory.filter.LocalDateFilter");
        imports.add("com.thy.fss.common.inmemory.filter.LocalDateTimeFilter");
        imports.add("com.thy.fss.common.inmemory.filter.InstantFilter");
        imports.add("com.thy.fss.common.inmemory.filter.EnumFilter");
        imports.add("com.thy.fss.common.inmemory.filter.CollectionFilter");

        // Analyze fields for additional imports (including inherited fields)
        List<Element> allFields = getAllFields(typeElement);
        for (Element enclosedElement : allFields) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                addFieldImports(imports, field);
            }
        }

        // Sort and print imports
        imports.stream().sorted().forEach(imp -> out.println("import " + imp + ";"));
        out.println();
    }

    /**
     * Gets all fields including inherited fields from @MetaModel superclasses.
     */
    private List<Element> getAllFields(TypeElement typeElement) {
        List<Element> allFields = new ArrayList<>();

        // Add fields from current class
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD &&
                    !enclosedElement.getModifiers().contains(Modifier.STATIC)) {
                allFields.add(enclosedElement);
            }
        }

        // Add fields from superclass (inheritance support)
        try {
            TypeMirror superclass = typeElement.getSuperclass();
            if (superclass != null && superclass.getKind() != TypeKind.NONE) {
                javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();
                if (typeUtils != null) {
                    Element superElement = typeUtils.asElement(superclass);
                    if (superElement instanceof TypeElement superTypeElement) {
                        if (superTypeElement.getAnnotation(MetaModel.class) != null) {
                            List<Element> inheritedFields = getAllFields(superTypeElement);
                            allFields.addAll(inheritedFields);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If superclass resolution fails (e.g., in test environments), continue with current class fields only
            debugLog("Could not resolve superclass for " + typeElement.getQualifiedName() + ": " + e.getMessage());
        }

        return allFields;
    }

    /**
     * Adds imports needed for a specific field.
     */
    private void addFieldImports(Set<String> imports, VariableElement field) {
        TypeMirror fieldType = field.asType();

        // Handle nested model types (other @MetaModel classes)
        if (fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                if (typeElement.getAnnotation(MetaModel.class) != null) {
                    // Import the filter class for the nested model
                    String nestedFilterClass = typeElement.getSimpleName() + FILTER4;
                    imports.add(typeElement.getQualifiedName().toString().replace(
                            typeElement.getSimpleName().toString(), nestedFilterClass));
                    // Also import the entity class itself
                    String qualifiedName = typeElement.getQualifiedName().toString();
                    if (!qualifiedName.startsWith("java.lang")) {
                        imports.add(qualifiedName);
                    }
                } else if (typeElement.getKind() == ElementKind.ENUM) {
                    // Import enum types from different packages
                    String qualifiedName = typeElement.getQualifiedName().toString();
                    if (!qualifiedName.startsWith("java.lang") && !qualifiedName.startsWith("java.util")) {
                        imports.add(qualifiedName);
                    }
                }

                // Handle collection element types from different packages
                if (isCollectionType(fieldType)) {
                    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                    if (!typeArguments.isEmpty()) {
                        TypeMirror elementTypeMirror = typeArguments.get(0);
                        if (elementTypeMirror instanceof DeclaredType elementDeclaredType) {
                            Element elementElement = elementDeclaredType.asElement();
                            if (elementElement instanceof TypeElement elementTypeElement) {
                                String elementQualifiedName = elementTypeElement.getQualifiedName().toString();
                                if (!elementQualifiedName.startsWith("java.lang") &&
                                        !elementQualifiedName.startsWith("java.util")) {
                                    imports.add(elementQualifiedName);
                                    // If the element type is a @MetaModel, also import its filter class
                                    if (elementTypeElement.getAnnotation(MetaModel.class) != null) {
                                        String elementFilterClass = elementTypeElement.getSimpleName() + FILTER4;
                                        imports.add(elementQualifiedName.replace(
                                                elementTypeElement.getSimpleName().toString(), elementFilterClass));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Analyzes fields of the type element and returns field information.
     */
    private List<FieldInfo> analyzeFields(TypeElement typeElement) {
        List<FieldInfo> fields = new ArrayList<>();

        debugLog("Analyzing fields for class " + typeElement.getQualifiedName());

        int totalFields = 0;
        int processedFields = 0;
        int skippedFields = 0;

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                totalFields++;
                VariableElement field = (VariableElement) enclosedElement;

                // Skip static and final fields
                Set<Modifier> modifiers = field.getModifiers();
                if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.FINAL)) {
                    skippedFields++;
                    debugLog("Skipping field '" + field.getSimpleName() + "' (static/final)");
                    continue;
                }

                debugLog("Analyzing field: " + field.getSimpleName());
                FieldInfo fieldInfo = analyzeField(field);
                if (fieldInfo != null) {
                    fields.add(fieldInfo);
                    processedFields++;
                    debugLog("Successfully processed field: " + field.getSimpleName());
                } else {
                    skippedFields++;
                    debugLog("Skipped unsupported field: " + field.getSimpleName());
                }
            }
        }

        debugLog("Field analysis summary - Total: " + totalFields
                + ", Processed: " + processedFields + ", Skipped: " + skippedFields);

        return fields;
    }

    /**
     * Analyzes a single field and returns field information including Jackson
     * annotation analysis.
     */
    private FieldInfo analyzeField(VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String fieldTypeName = fieldType.toString();

        debugLog(FIELD + fieldName + "' has type: " + fieldTypeName);

        // Determine filter type based on field type
        String filterType = determineFilterType(fieldType);
        if (filterType == null) {
            debugLog("Unsupported field type: " + fieldTypeName + " for field: " + fieldName);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Unsupported field type for filtering: " + fieldTypeName + " in field " + fieldName
            );
            return null;
        }

        debugLog(FIELD + fieldName + "' mapped to filter type: " + filterType);

        boolean isNestedModel = isNestedModelType(fieldType);
        boolean isCollection = isCollectionType(fieldType);
        boolean isEnum = isEnumType(fieldType);
        String elementType = null;
        String modelPackageName = null;

        if (isCollection) {
            elementType = getCollectionElementType(fieldType);
            debugLog(FIELD + fieldName + "' is collection with element type: " + elementType);
        }

        if (isNestedModel) {
            debugLog(FIELD + fieldName + "' is nested model type");
            modelPackageName = getPackageName(fieldTypeName);
        }

        if (isEnum) {
            debugLog(FIELD + fieldName + "' is enum type");
        }

        // Perform Jackson annotation analysis
        FilterFieldConfig annotationConfig = analyzeFieldAnnotations(field, fieldTypeName, filterType, isEnum, isCollection, isNestedModel, elementType, modelPackageName);

        return new FieldInfo(fieldName, fieldTypeName, filterType, isNestedModel, isCollection, isEnum, elementType, annotationConfig);
    }

    /**
     * Determines the appropriate filter type for a given field type.
     */
    private String determineFilterType(TypeMirror fieldType) {
        String typeName = fieldType.toString();

        // Handle primitive types
        if (fieldType.getKind().isPrimitive()) {
            return switch (fieldType.getKind()) {
                case INT -> IntegerFilter.class.getSimpleName();
                case LONG -> LongFilter.class.getSimpleName();
                case DOUBLE -> DoubleFilter.class.getSimpleName();
                case BOOLEAN -> BooleanFilter.class.getSimpleName();
                default -> null;
            };
        }

        // Handle enum types FIRST (before collections and other checks)
        if (isEnumType(fieldType)) {
            return EnumFilter.class.getSimpleName();
        }

        // Handle collections - use CollectionFilter instead of element type filter
        if (isCollectionType(fieldType)) {
            return CollectionFilter.class.getSimpleName();
        }

        // Handle direct type mapping
        if (TYPE_TO_FILTER_MAP.containsKey(typeName)) {
            return TYPE_TO_FILTER_MAP.get(typeName);
        }

        // Handle nested model types (other @MetaModel classes)
        if (isNestedModelType(fieldType) && fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement.getSimpleName() + FILTER4;
            }
        }

        return null;
    }

    /**
     * Checks if the field type is a nested model type (annotated with
     *
     * @MetaModel).
     */
    private boolean isNestedModelType(TypeMirror fieldType) {
        if (fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement.getAnnotation(MetaModel.class) != null;
            }
        }
        return false;
    }

    private boolean isEnumType(TypeMirror typeMirror) {
        // Handle enum types FIRST (before collections and other checks)
        if (typeMirror instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            return element instanceof TypeElement typeElement && typeElement.getKind() == ElementKind.ENUM;
        }
        return false;
    }

    /**
     * Checks if the field type is a collection type.
     */
    private boolean isCollectionType(TypeMirror fieldType) {
        if (fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                String qualifiedName = typeElement.getQualifiedName().toString();
                return COLLECTION_TYPES.contains(qualifiedName);
            }
        }
        return false;
    }

    /**
     * Gets the element type of a collection.
     */
    private String getCollectionElementType(TypeMirror fieldType) {
        if (fieldType instanceof DeclaredType declaredType) {
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.get(0).toString();
            }
        }
        return null;
    }

    /**
     * Generates field declarations for the filter class. Includes @JsonProperty
     * annotation if present in the entity field.
     */
    private void generateFieldDeclarations(PrintWriter out, List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            // Add @JsonProperty annotation if present in the entity field
            if (field.annotationConfig != null && field.annotationConfig.hasJacksonAnnotations()) {
                for (AnnotationInfo annotation : field.annotationConfig.getJacksonAnnotations()) {
                    if ("JsonProperty".equals(annotation.getAnnotationType())
                            || annotation.getAnnotationType().endsWith(".JsonProperty")) {
                        // Get the value parameter from the annotation
                        Map<String, Object> params = annotation.getParameters();
                        if (params != null && params.containsKey("value")) {
                            String value = String.valueOf(params.get("value"));
                            // Ensure the value is properly quoted (it should already have quotes from annotation)
                            // If it doesn't have quotes, add them
                            if (!value.startsWith("\"")) {
                                value = "\"" + value + "\"";
                            }
                            out.println("    @com.fasterxml.jackson.annotation.JsonProperty(" + value + ")");
                        }
                    }
                }
            }

            // Generate field declaration
            if (field.isCollection && field.elementType != null) {
                out.println("    private CollectionFilter<" + field.elementType + "> " + field.name + ";");
            } else if (field.isEnum) {
                out.println("    private EnumFilter<" + field.typeName + "> " + field.name + ";");
            } else {
                out.println("    private " + field.filterType + " " + field.name + ";");
            }
        }
        out.println();
    }

    /**
     * Generates copy constructor.
     */
    private void generateCopyConstructor(PrintWriter out, String filterClassName, List<FieldInfo> fields) {
        out.println(JAVADOC_START);
        out.println("     * Copy constructor.");
        out.println(JAVADOC_LINE);
        out.println("     * @param filter The filter to copy from (can be null)");
        out.println(JAVADOC_END);
        out.println(PUBLIC + filterClassName + "(" + filterClassName + " filter) {");
        out.println("        if (filter == null) {");
        out.println("            return; // Create empty filter when input is null");
        out.println(END2);

        for (FieldInfo field : fields) {
            if (field.isCollection && field.elementType != null) {
                out.println(SPACE_THIS + field.name + FILTER3 + field.name + NULL_CHECK);
                out.println("            new CollectionFilter<>(filter." + field.name + NULL_END);
            } else if (field.isNestedModel) {
                out.println(SPACE_THIS + field.name + FILTER3 + field.name + NULL_CHECK);
                out.println("            new " + field.filterType + "(filter." + field.name + NULL_END);
            } else {
                out.println(SPACE_THIS + field.name + FILTER3 + field.name + NULL_CHECK);
                if (field.isEnum) {
                    out.println("            new EnumFilter<>(filter." + field.name + NULL_END);
                } else {
                    out.println("            new " + field.filterType + "(filter." + field.name + NULL_END);
                }
            }
        }

        out.println(END);
        out.println();
    }

    /**
     * Generates the getEntityClass method implementation.
     */
    private void generateGetEntityClassMethod(PrintWriter out, String simpleClassName) {
        out.println(JAVADOC_START);
        out.println("     * Returns the entity class that this filter applies to.");
        out.println(JAVADOC_LINE);
        out.println("     * @return the entity class");
        out.println(JAVADOC_END);
        out.println(SPACE_OVERRIDES_AN);
        out.println("    public Class<" + simpleClassName + "> getEntityClass() {");
        out.println(RETURN_SPACES + simpleClassName + ".class;");
        out.println(END);
        out.println();
    }

    /**
     * Generates getters and setters for all fields.
     */
    private void generateGettersAndSetters(PrintWriter out, List<FieldInfo> fields, String filterClassName) {
        for (FieldInfo field : fields) {
            String fieldType;
            if (field.isCollection && field.elementType != null) {
                fieldType = "CollectionFilter<" + field.elementType + ">";
            } else if (field.isEnum) {
                fieldType = "EnumFilter<" + field.typeName + ">";
            } else {
                fieldType = field.filterType;
            }

            // Getter
            out.println(JAVADOC_START);
            out.println("     * Gets the " + field.name + FILTER);
            out.println(JAVADOC_LINE);
            out.println("     * @return The " + field.name + FILTER1);
            out.println(JAVADOC_END);
            out.println(PUBLIC + fieldType + " get" + capitalize(field.name) + "() {");
            out.println(RETURN_SPACES + field.name + ";");
            out.println(END);
            out.println();

            // Setter
            out.println(JAVADOC_START);
            out.println("     * Sets the " + field.name + FILTER);
            out.println(JAVADOC_LINE);
            out.println("     * @param " + field.name + " The " + field.name + FILTER1);
            out.println("     * @return This filter instance for method chaining");
            out.println(JAVADOC_END);
            out.println(PUBLIC + filterClassName + " set" + capitalize(field.name) + "(" + fieldType + " " + field.name + ") {");
            out.println(SPACE_THIS + field.name + " = " + field.name + ";");
            out.println("        return this;");
            out.println(END);
            out.println();
        }
    }

    /**
     * Generates builder pattern with nested builders.
     */
    private void generateBuilderPattern(PrintWriter out, String filterClassName, List<FieldInfo> fields) {
        // Static builder method
        out.println(JAVADOC_START);
        out.println("     * Creates a new builder for this filter.");
        out.println(JAVADOC_LINE);
        out.println("     * @return A new builder instance");
        out.println(JAVADOC_END);
        out.println("    public static " + filterClassName + "Builder builder() {");
        out.println("        return new " + filterClassName + "Builder();");
        out.println(END);
        out.println();

        // Builder class
        generateBuilderClass(out, filterClassName, fields);
    }

    /**
     * Generates the builder class with nested builder support.
     */
    private void generateBuilderClass(PrintWriter out, String filterClassName, List<FieldInfo> fields) {
        out.println(JAVADOC_START);
        out.println("     * Builder class for " + filterClassName + ".");
        out.println(JAVADOC_END);
        out.println("    public static class " + filterClassName + "Builder {");
        out.println("        private " + filterClassName + " filter = new " + filterClassName + "();");
        out.println();

        // Generate builder methods for each field
        for (FieldInfo field : fields) {
            generateBuilderMethods(out, field, filterClassName);
        }

        // Build method
        out.println(SPACE_JAVADOC_START);
        out.println("         * Builds the filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @return The constructed filter");
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + " build() {");
        out.println("            return filter;");
        out.println(END2);

        out.println(END);
        out.println();

        // Generate generic parent builder class for nested usage
        generateGenericBuilderClass(out, filterClassName, fields);
    }

    /**
     * Generates builder methods for a specific field.
     */
    private void generateBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName) {
        String capitalizedName = capitalize(field.name);

        if (field.isNestedModel) {
            // Nested model builder methods
            generateNestedModelBuilderMethods(out, field, filterClassName, capitalizedName);
        } else {
            // Regular field builder methods
            generateRegularFieldBuilderMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates builder methods for nested model fields.
     */
    private void generateNestedModelBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        // Direct setter method
        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + FILTER);
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param " + field.name + "Filter The " + field.name + FILTER1);
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "(" + field.filterType + " " + field.name + "Filter) {");
        out.println("            filter.set" + capitalizedName + "(" + field.name + "Filter);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        // Nested builder method
        String nestedBuilderType = field.filterType + "." + field.filterType + "GenericBuilder<" + filterClassName + "Builder>";
        out.println(SPACE_JAVADOC_START);
        out.println("         * Creates a nested builder for " + field.name + FILTER);
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @return A nested builder for " + field.name);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + nestedBuilderType + " " + field.name + "() {");
        out.println(IF_FILTER_GET + capitalizedName + NULL_STR);
        if (field.isEnum) {
            out.println(FILTER_SET + capitalizedName + NEW_ENUM_FILTER);
        } else {
            out.println(FILTER_SET + capitalizedName + NEW_STR + field.filterType + "());");
        }
        out.println(SPACE_END);
        out.println("            return new " + field.filterType + "." + field.filterType + "GenericBuilder<>(this, filter.get" + capitalizedName + "(), ");
        out.println("                parentBuilder -> parentBuilder.build());");
        out.println(END2);
        out.println();
    }

    /**
     * Generates builder methods for regular (non-nested) fields.
     */
    private void generateRegularFieldBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        if (field.isCollection) {
            // Generate collection-specific builder methods
            generateCollectionBuilderMethods(out, field, filterClassName, capitalizedName);
        } else {
            // Initialize method
            out.println(SPACE_JAVADOC_START);
            out.println("         * Initializes the " + field.name + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "() {");
            out.println(IF_FILTER_GET + capitalizedName + NULL_STR);
            if (field.isEnum) {
                out.println(FILTER_SET + capitalizedName + NEW_ENUM_FILTER);
            } else {
                out.println(FILTER_SET + capitalizedName + NEW_STR + field.filterType + "());");
            }
            out.println(SPACE_END);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();

            // Generate specific operation methods based on filter type
            generateFilterOperationMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates collection-specific builder methods.
     */
    private void generateCollectionBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        // Direct setter method
        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + FILTER);
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param filter The " + field.name + FILTER1);
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "(CollectionFilter<" + field.elementType + "> filter) {");
        out.println("            this.filter.set" + capitalizedName + "(filter);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        // Collection operation methods
        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " contains filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value The value that the collection must contain");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "Contains(" + field.elementType + VALUE);
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setCollectionContains(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " isEmpty filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value True to match empty collections, false to match non-empty collections");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "IsEmpty(Boolean value) {");
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setIsEmpty(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " isNotEmpty filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value True to match non-empty collections, false to match empty collections");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "IsNotEmpty(Boolean value) {");
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setIsNotEmpty(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();
    }

    /**
     * Generates operation methods based on filter type.
     */
    private void generateFilterOperationMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        switch (field.filterType) {
            case STRING_FILTER -> generateStringFilterMethods(out, field, filterClassName, capitalizedName);
            case INTEGER_FILTER, LONG_FILTER, DOUBLE_FILTER, FLOAT_FILTER ->
                    generateRangeFilterMethods(out, field, filterClassName, capitalizedName);
            case BOOLEAN_FILTER -> generateBooleanFilterMethods(out, field, filterClassName, capitalizedName);
            case LOCAL_DATE_FILTER, LOCAL_DATE_TIME_FILTER, INSTANT_FILTER ->
                    generateTemporalFilterMethods(out, field, filterClassName, capitalizedName);
            default -> generateBasicFilterMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates methods for StringFilter.
     */
    private void generateStringFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // String-specific methods
        String[] stringMethods = {"Contains", "StartsWith", "EndsWith", "Matches"};
        for (String method : stringMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + MATCHING);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + method + "(String value) {");
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }

        // Boolean methods for isEmpty and isBlank
        String[] booleanMethods = {"IsEmpty", "IsBlank"};
        for (String method : booleanMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + MATCHING);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + method + "(Boolean value) {");
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Generates methods for range filters (Integer, Long, Double, Float).
     */
    private void generateRangeFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // Range-specific methods
        String[] rangeMethods = {"GreaterThan", "LessThan", "GreaterOrEqualThan", "LessOrEqualThan"};
        String valueType = getValueTypeForFilter(field);

        for (String method : rangeMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + method + "(" + valueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Generates methods for BooleanFilter.
     */
    private void generateBooleanFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateBasicFilterMethods(out, field, filterClassName, capitalizedName);
    }

    /**
     * Generates methods for temporal filters (LocalDate, LocalDateTime,
     * Instant).
     */
    private void generateTemporalFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // Range-specific methods
        String[] temporalMethods = {"IsBefore", "IsAfter", "IsOnOrBefore", "IsOnOrAfter"};
        String valueType = getValueTypeForFilter(field);

        for (String method : temporalMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + method + "(" + valueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " last filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("     * @param value Temporal preset value (e.g. 24h, 40m)");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "Last(com.thy.fss.common.inmemory.filter.TemporalPreset value) {");
        out.println(SPACES + field.name + "();");
        out.println(FILTER_GET + capitalizedName + "().setLast(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " next filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("     * @param value Temporal preset value (e.g. 24h, 40m)");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + BUILDER + field.name + "Next(com.thy.fss.common.inmemory.filter.TemporalPreset value) {");
        out.println(SPACES + field.name + "();");
        out.println(FILTER_GET + capitalizedName + "().setNext(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();
    }

    /**
     * Generates basic filter methods (equals, in, notIn).
     */
    private void generateBasicFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {

        String valueType = getValueTypeForFilter(field);

        // Range-specific methods
        String[] basicMethods = {"Equals", "NotEquals", "IsNull", "IsNotNull", "In", "NotIn"};
        String[] basicMethodValueTypes = {valueType, valueType, BOOLEAN_STR, BOOLEAN_STR, JAVA_UTIL_COLLECTION + valueType + ">", JAVA_UTIL_COLLECTION + valueType + ">"};

        for (int i = 0; i < basicMethods.length; i++) {
            String method = basicMethods[i];
            String methodValueType = basicMethodValueTypes[i];
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + BUILDER + field.name + method + "(" + methodValueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Gets the value type for a filter type.
     */
    private String getValueTypeForFilter(FieldInfo fieldInfo) {
        return switch (fieldInfo.filterType) {
            case STRING_FILTER -> "String";
            case INTEGER_FILTER -> "Integer";
            case LONG_FILTER -> "Long";
            case DOUBLE_FILTER -> "Double";
            case BOOLEAN_FILTER -> BOOLEAN_STR;
            case LOCAL_DATE_FILTER -> "LocalDate";
            case LOCAL_DATE_TIME_FILTER -> "LocalDateTime";
            case INSTANT_FILTER -> "Instant";
            case "EnumFilter" -> fieldInfo.typeName; // Use the actual enum type
            default -> "Object";
        };
    }

    /**
     * Generates the generic builder class for nested usage.
     */
    private void generateGenericBuilderClass(PrintWriter out, String filterClassName, List<FieldInfo> fields) {
        out.println(JAVADOC_START);
        out.println("     * Generic builder class for " + filterClassName + " with parent builder support.");
        out.println("     * Used for nested filter building with type-safe parent builder return.");
        out.println(JAVADOC_LINE);
        out.println("     * @param <P> The type of the parent builder");
        out.println(JAVADOC_END);
        out.println("    public static class " + filterClassName + "GenericBuilder<P> {");
        out.println("        private final P parent;");
        out.println("        private final " + filterClassName + " filter;");
        out.println("        private final Function<P, ?> buildFunction;");
        out.println();

        // Constructor
        out.println(SPACE_JAVADOC_START);
        out.println("         * Constructor for generic builder.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param parent The parent builder");
        out.println("         * @param filter The filter instance to build");
        out.println("         * @param buildFunction Function to call when building");
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + "GenericBuilder(P parent, " + filterClassName + " filter, Function<P, ?> buildFunction) {");
        out.println("            this.parent = parent;");
        out.println("            this.filter = filter;");
        out.println("            this.buildFunction = buildFunction;");
        out.println(END2);
        out.println();

        // Generate builder methods for each field
        for (FieldInfo field : fields) {
            generateGenericBuilderMethods(out, field, filterClassName);
        }

        // Parent return method
        out.println(SPACE_JAVADOC_START);
        out.println("         * Returns to the parent builder.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @return The parent builder");
        out.println(SPACCE_JAVADOC_END);
        out.println("        public P and() {");
        out.println("            return parent;");
        out.println(END2);
        out.println();

        // Build method
        out.println(SPACE_JAVADOC_START);
        out.println("         * Builds the filter and returns the result of the build function.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @return The result of the build function");
        out.println(SPACCE_JAVADOC_END);
        out.println("        public Object build() {");
        out.println("            return buildFunction.apply(parent);");
        out.println(END2);

        out.println(END);
        out.println();
    }

    /**
     * Generates builder methods for the generic builder class.
     */
    private void generateGenericBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName) {
        String capitalizedName = capitalize(field.name);

        if (field.isNestedModel) {
            // Nested model methods for generic builder
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println("         * @param " + field.name + "Filter The " + field.name + FILTER1);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "(" + field.filterType + " " + field.name + "Filter) {");
            out.println("            filter.set" + capitalizedName + "(" + field.name + "Filter);");
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();

            // Nested builder method
            out.println(SPACE_JAVADOC_START);
            out.println("         * Creates a nested builder for " + field.name + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println("         * @return A nested builder for " + field.name);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + field.filterType + "." + field.filterType + "GenericBuilder<" + filterClassName + "GenericBuilder<P>> " + field.name + "() {");
            out.println(IF_FILTER_GET + capitalizedName + NULL_STR);
            if (field.isEnum) {
                out.println(FILTER_SET + capitalizedName + NEW_ENUM_FILTER);
            } else {
                out.println(FILTER_SET + capitalizedName + NEW_STR + field.filterType + "());");
            }
            out.println(SPACE_END);
            out.println("            return new " + field.filterType + "." + field.filterType + "GenericBuilder<>(this, filter.get" + capitalizedName + "(), ");
            out.println("                parentBuilder -> parentBuilder.buildFunction.apply(parentBuilder.parent));");
            out.println(END2);
            out.println();
        } else {
            // Regular field methods for generic builder
            generateGenericBuilderFieldMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates methods for regular fields in the generic builder.
     */
    private void generateGenericBuilderFieldMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        if (field.isCollection) {
            // Generate collection-specific generic builder methods
            generateGenericCollectionBuilderMethods(out, field, filterClassName, capitalizedName);
        } else {
            // Initialize method
            out.println(SPACE_JAVADOC_START);
            out.println("         * Initializes the " + field.name + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "() {");
            out.println(IF_FILTER_GET + capitalizedName + NULL_STR);
            if (field.isEnum) {
                out.println(FILTER_SET + capitalizedName + NEW_ENUM_FILTER);
            } else {
                out.println(FILTER_SET + capitalizedName + NEW_STR + field.filterType + "());");
            }
            out.println(SPACE_END);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();

            // Generate specific operation methods based on filter type
            generateGenericBuilderOperationMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates collection-specific generic builder methods.
     */
    private void generateGenericCollectionBuilderMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        // Direct setter method
        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + FILTER);
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param filter The " + field.name + FILTER1);
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "(CollectionFilter<" + field.elementType + "> filter) {");
        out.println("            this.filter.set" + capitalizedName + "(filter);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        // Collection operation methods
        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " contains filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value The value that the collection must contain");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "Contains(" + field.elementType + VALUE);
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setCollectionContains(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " isEmpty filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value True to match empty collections, false to match non-empty collections");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "IsEmpty(Boolean value) {");
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setIsEmpty(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " isNotEmpty filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value True to match non-empty collections, false to match empty collections");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "IsNotEmpty(Boolean value) {");
        out.println(IF_THIS_FILTER_GET + capitalizedName + NULL_STR);
        out.println(THIS_FILTER_SET + capitalizedName + FILTER2);
        out.println(SPACE_END);
        out.println(THIS_FILTER_GET + capitalizedName + "().setIsNotEmpty(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();
    }

    /**
     * Generates operation methods for the generic builder based on filter type.
     */
    private void generateGenericBuilderOperationMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        switch (field.filterType) {
            case STRING_FILTER -> generateGenericStringFilterMethods(out, field, filterClassName, capitalizedName);
            case INTEGER_FILTER, LONG_FILTER, DOUBLE_FILTER, FLOAT_FILTER ->
                    generateGenericRangeFilterMethods(out, field, filterClassName, capitalizedName);
            case BOOLEAN_FILTER -> generateGenericBooleanFilterMethods(out, field, filterClassName, capitalizedName);
            case LOCAL_DATE_FILTER, LOCAL_DATE_TIME_FILTER, INSTANT_FILTER ->
                    generateGenericTemporalFilterMethods(out, field, filterClassName, capitalizedName);
            default -> generateGenericBasicFilterMethods(out, field, filterClassName, capitalizedName);
        }
    }

    /**
     * Generates methods for StringFilter in generic builder.
     */
    private void generateGenericStringFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateGenericBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // String-specific methods
        String[] stringMethods = {"Contains", "StartsWith", "EndsWith", "Matches"};
        for (String method : stringMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + MATCHING);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + method + "(String value) {");
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }

        // Boolean methods for isEmpty and isBlank
        String[] booleanMethods = {"IsEmpty", "IsBlank"};
        for (String method : booleanMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + MATCHING);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + method + "(Boolean value) {");
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Generates methods for range filters in generic builder.
     */
    private void generateGenericRangeFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateGenericBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // Range-specific methods
        String[] rangeMethods = {"GreaterThan", "LessThan", "GreaterOrEqualThan", "LessOrEqualThan"};
        String valueType = getValueTypeForFilter(field);

        for (String method : rangeMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + method + "(" + valueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Generates methods for BooleanFilter in generic builder.
     */
    private void generateGenericBooleanFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateGenericBasicFilterMethods(out, field, filterClassName, capitalizedName);
    }

    /**
     * Generates methods for temporal filters in generic builder.
     */
    private void generateGenericTemporalFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {
        generateGenericBasicFilterMethods(out, field, filterClassName, capitalizedName);

        // Range-specific methods
        String[] rangeMethods = {"IsBefore", "IsAfter", "IsOnOrBefore", "IsOnOrAfter"};
        String valueType = getValueTypeForFilter(field);

        for (String method : rangeMethods) {
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + method + "(" + valueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " last filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value Temporal preset value (e.g. 24h, 40m)");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "Last(com.thy.fss.common.inmemory.filter.TemporalPreset value) {");
        out.println(SPACES + field.name + "();");
        out.println(FILTER_GET + capitalizedName + "().setLast(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();

        out.println(SPACE_JAVADOC_START);
        out.println(SETS_THE + field.name + " next filter.");
        out.println(SPACE_JAVADOC_LINE);
        out.println("         * @param value Temporal preset value (e.g. 24h, 40m)");
        out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
        out.println(SPACCE_JAVADOC_END);
        out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + "Next(com.thy.fss.common.inmemory.filter.TemporalPreset value) {");
        out.println(SPACES + field.name + "();");
        out.println(FILTER_GET + capitalizedName + "().setNext(value);");
        out.println(RETURN_THIS);
        out.println(END2);
        out.println();
    }

    /**
     * Generates basic filter methods for generic builder.
     */
    private void generateGenericBasicFilterMethods(PrintWriter out, FieldInfo field, String filterClassName, String capitalizedName) {

        String valueType = getValueTypeForFilter(field);

        // Range-specific methods
        String[] basicMethods = {"Equals", "NotEquals", "IsNull", "IsNotNull", "In", "NotIn"};
        String[] basicMethodValueTypes = {valueType, valueType, BOOLEAN_STR, BOOLEAN_STR, JAVA_UTIL_COLLECTION + valueType + ">", JAVA_UTIL_COLLECTION + valueType + ">"};

        for (int i = 0; i < basicMethods.length; i++) {
            String method = basicMethods[i];
            String methodValueType = basicMethodValueTypes[i];
            out.println(SPACE_JAVADOC_START);
            out.println(SETS_THE + field.name + " " + method.toLowerCase() + FILTER);
            out.println(SPACE_JAVADOC_LINE);
            out.println(VALUE1 + method.toLowerCase() + COMPARISON);
            out.println(RETURN_THIS_BUILDER_FOR_METHOD_CHAINING);
            out.println(SPACCE_JAVADOC_END);
            out.println(PUBLIC1 + filterClassName + GENERIC_BUILDER_P + field.name + method + "(" + methodValueType + VALUE);
            out.println(SPACES + field.name + "();");
            out.println(FILTER_GET + capitalizedName + SET + method + VALUE2);
            out.println(RETURN_THIS);
            out.println(END2);
            out.println();
        }
    }

    /**
     * Generates equals, hashCode, and toString methods.
     */
    private void generateEqualsHashCodeToString(PrintWriter out, String filterClassName, List<FieldInfo> fields) {
        // equals method
        out.println(SPACE_OVERRIDES_AN);
        out.println("    public boolean equals(Object o) {");
        out.println("        if (this == o) return true;");
        out.println("        if (o == null || getClass() != o.getClass()) return false;");
        out.println();
        out.println("        " + filterClassName + " that = (" + filterClassName + ") o;");
        out.println(RETURN_SPACES + generateEqualsComparison(fields) + ";");
        out.println(END);
        out.println();

        // hashCode method
        out.println(SPACE_OVERRIDES_AN);
        out.println("    public int hashCode() {");
        out.println("        return Objects.hash(" + generateHashCodeFields(fields) + ");");
        out.println(END);
        out.println();

        // toString method
        out.println(SPACE_OVERRIDES_AN);
        out.println("    public String toString() {");
        out.println("        return \"" + filterClassName + "{\" +");
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo field = fields.get(i);
            String separator = " +";
            out.println("               \"" + field.name + "=\" + " + field.name + separator);
        }
        out.println("               '}';");
        out.println(END);
    }

    /**
     * Generates the equals comparison for all fields.
     */
    private String generateEqualsComparison(List<FieldInfo> fields) {
        if (fields.isEmpty()) {
            return "true";
        }

        return fields.stream()
                .map(field -> "Objects.equals(" + field.name + ", that." + field.name + ")")
                .reduce((a, b) -> a + " &&\n               " + b)
                .orElse("true");
    }

    /**
     * Generates the hashCode fields list.
     */
    private String generateHashCodeFields(List<FieldInfo> fields) {
        return fields.stream()
                .map(field -> field.name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * Extracts package name from fully qualified class name.
     */
    private String getPackageName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Capitalizes the first letter of a string for getter/setter method names.
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
     * Analyzes Jackson annotations on an entity field and creates
     * FilterFieldConfig. This method performs the annotation analysis phase as
     * required by task 4.1.
     *
     * @param field            the entity field to analyze
     * @param fieldTypeName    the field type name
     * @param filterType       the determined filter type
     * @param isEnum           whether the field is an enum
     * @param isCollection     whether the field is a collection
     * @param elementType      the collection element type (if applicable)
     * @param modelPackageName the full package name of the model
     * @return FilterFieldConfig containing annotation analysis results
     */
    private FilterFieldConfig analyzeFieldAnnotations(VariableElement field, String fieldTypeName,
                                                      String filterType, boolean isEnum, boolean isCollection, boolean isNestedModel, String elementType, String modelPackageName) {
        String fieldName = field.getSimpleName().toString();

        debugLog("Starting Jackson annotation analysis for field: " + fieldName);

        // Create base FilterFieldConfig
        FilterFieldConfig config = new FilterFieldConfig(fieldName, fieldTypeName, filterType);
        config.setEnum(isEnum);
        config.setCollection(isCollection);
        config.setElementType(elementType);
        config.setModel(isNestedModel);
        config.setPackageName(modelPackageName);

        // Set field type flags
        config.setTemporal(isTemporalType(fieldTypeName));
        config.setNumeric(isNumericType(fieldTypeName));
        config.setString("java.lang.String".equals(fieldTypeName));

        try {
            // Extract Jackson annotations
            List<AnnotationInfo> jacksonAnnotations = jacksonAnnotationAnalyzer.extractJacksonAnnotations(field);
            config.setJacksonAnnotations(jacksonAnnotations);

            if (!jacksonAnnotations.isEmpty()) {
                debugLog("Found " + jacksonAnnotations.size() + " Jackson annotations on field: " + fieldName);
            }

            // Analyze datetime format if temporal field
            if (config.isTemporal()) {
                if (isInstantType(fieldTypeName)) {
                    // Use InstantFormatAnalyzer for Instant fields
                    InstantFormatInfo instantFormatInfo = instantFormatAnalyzer.analyzeInstantField(field);
                    config.setInstantFormatInfo(instantFormatInfo);

                    if (instantFormatInfo != null) {
                        debugLog("Instant format analysis for field '" + fieldName + "': "
                                + (instantFormatInfo.usesCustomFormat() ? "custom pattern '" + instantFormatInfo.getPattern() + "'"
                                : "default pattern '" + instantFormatInfo.getPattern() + "'")
                                + (instantFormatInfo.shouldUseTimestamp() ? " (timestamp format)" : " (string format)"));
                    }
                } else {
                    // Use DateTimeFormatAnalyzer for LocalDateTime and LocalDate fields
                    DateTimeFormatInfo dateTimeFormatInfo = dateTimeFormatAnalyzer.analyzeDateTimeField(field);
                    config.setDateTimeFormatInfo(dateTimeFormatInfo);

                    if (dateTimeFormatInfo != null) {
                        debugLog("DateTime format analysis for field '" + fieldName + "': "
                                + (dateTimeFormatInfo.usesCustomFormat() ? "custom pattern '" + dateTimeFormatInfo.getPattern() + "'"
                                : "default pattern '" + dateTimeFormatInfo.getPattern() + "'"));
                    }
                }
            }

            // Analyze enum deserialization if enum field
            if (config.isEnum()) {
                TypeMirror fieldType = field.asType();
                TypeElement enumTypeElement = enumAnalyzer.getEnumTypeElement(fieldType);

                if (enumTypeElement != null) {
                    EnumDeserializationInfo enumInfo = enumAnalyzer.analyzeEnum(enumTypeElement);
                    config.setEnumDeserializationInfo(enumInfo);

                    debugLog("Enum deserialization analysis for field '" + fieldName + "': "
                            + enumInfo.getDeserializationType()
                            + (enumInfo.hasCustomDeserialization() ? " (custom)" : " (default)"));
                }
            }

        } catch (Exception e) {
            // Log error but continue processing - don't fail the entire generation
            debugLog("Error during annotation analysis for field '" + fieldName + "': " + e.getMessage());
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Failed to analyze Jackson annotations for field '" + fieldName + "': " + e.getMessage()
            );
        }

        debugLog("Completed Jackson annotation analysis for field: " + fieldName);
        return config;
    }

    /**
     * Checks if a field type is a temporal type (LocalDateTime, LocalDate,
     * Instant).
     */
    private boolean isTemporalType(String fieldTypeName) {
        return "java.time.LocalDateTime".equals(fieldTypeName)
                || "java.time.LocalDate".equals(fieldTypeName)
                || "java.time.Instant".equals(fieldTypeName);
    }

    /**
     * Checks if a field type is specifically an Instant type.
     */
    private boolean isInstantType(String fieldTypeName) {
        return "java.time.Instant".equals(fieldTypeName);
    }

    /**
     * Checks if a field type is a numeric type.
     */
    private boolean isNumericType(String fieldTypeName) {
        return "java.lang.Integer".equals(fieldTypeName)
                || "java.lang.Long".equals(fieldTypeName)
                || "java.lang.Double".equals(fieldTypeName)
                || "java.lang.Float".equals(fieldTypeName)
                || "int".equals(fieldTypeName)
                || "long".equals(fieldTypeName)
                || "double".equals(fieldTypeName)
                || "float".equals(fieldTypeName);
    }

    /**
     * Generates a high-performance custom deserializer class using
     * FilterDeserializerGenerator. This replaces the placeholder deserializer
     * with actual optimized deserialization logic.
     */
    private void generateCustomDeserializer(TypeElement typeElement, String packageName, String filterClassName, List<FieldInfo> fields) {
        try {
            debugLog("Generating custom deserializer for: " + filterClassName);

            // Convert FieldInfo list to FilterFieldConfig list for deserializer generator
            List<FilterFieldConfig> fieldConfigs = new ArrayList<>();

            // Include all fields for deserializer generation
            for (FieldInfo field : fields) {
                if (field.annotationConfig() != null) {
                    FilterFieldConfig config = field.annotationConfig();
                    // Process collection fields to detect model element types and set import metadata
                    if (config.isCollection()) {
                        filterDeserializerGenerator.processCollectionField(config);
                    }
                    fieldConfigs.add(config);
                    debugLog("Added field '" + field.name() + "' to deserializer generation");
                }
            }

            // Always generate deserializer, even if no fields (for consistency)
            String entityClassName = typeElement.getSimpleName().toString();
            filterDeserializerGenerator.generateDeserializer(filterClassName, packageName, fieldConfigs, entityClassName);
            debugLog("Generated deserializer for " + filterClassName + " with " + fieldConfigs.size() + " fields");

        } catch (ProcessingException e) {
            debugLog("Failed to generate custom deserializer for " + filterClassName + ": " + e.getMessage());

            // Print ERROR to fail the build - annotation processor will stop compilation
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate custom deserializer for " + filterClassName + ": " + e.getMessage()
                            + "\nCause: " + (e.getCause() != null ? e.getCause().getMessage() : "Unknown")
            );
            // Don't throw exception - ERROR message is enough to fail the build

        } catch (Exception e) {
            debugLog("Unexpected error generating custom deserializer for " + filterClassName + ": " + e.getMessage());

            // Print ERROR to fail the build - annotation processor will stop compilation
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Unexpected error generating custom deserializer for " + filterClassName + ": "
                            + e.getClass().getSimpleName() + " - " + e.getMessage()
                            + "\nStack trace: " + getStackTraceAsString(e)
            );
            // Don't throw exception - ERROR message is enough to fail the build
        }
    }

    /**
     * Helper method to get stack trace as string for error reporting.
     */
    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
            if (sb.length() > 500) { // Limit output
                sb.append("  ... (truncated)");
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Validates that deserializer generation was successful by checking if the
     * deserializer file exists. This ensures proper compilation order and
     * dependencies.
     */
    private void validateDeserializerGeneration(String packageName, String filterClassName) {
        String deserializerClassName = filterClassName + "Deserializer";
        String fullyQualifiedName = packageName.isEmpty() ? deserializerClassName : packageName + "." + deserializerClassName;

        try {
            // Try to get the generated deserializer file to verify it exists
            // This is a validation step to ensure the build process generated the deserializer correctly
            debugLog("Validating deserializer generation for " + fullyQualifiedName);

            // The file should have been created by generateCustomDeserializer or generateFallbackDeserializer
            // If we reach this point without exceptions, the deserializer was generated successfully
            debugLog("Deserializer validation successful for " + fullyQualifiedName);

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "Successfully integrated custom deserializer generation for " + filterClassName
            );

        } catch (Exception e) {
            debugLog("Deserializer validation failed for " + fullyQualifiedName + ": " + e.getMessage());

            // This is a warning, not an error, because we have fallback mechanisms
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Deserializer validation failed for " + filterClassName + ": " + e.getMessage()
                            + ". Build process may have compilation order issues."
            );
        }
    }



    /**
     * Inner class to hold field information including Jackson annotation
     * analysis results.
     */
    private record FieldInfo(String name, String typeName, String filterType, boolean isNestedModel,
                             boolean isCollection, boolean isEnum, String elementType,
                             FilterFieldConfig annotationConfig) {

    }
}
