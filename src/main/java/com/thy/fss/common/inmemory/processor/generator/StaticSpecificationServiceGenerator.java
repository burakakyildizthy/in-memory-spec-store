package com.thy.fss.common.inmemory.processor.generator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import com.thy.fss.common.inmemory.specification.AttributeType;
import com.thy.fss.common.inmemory.specification.Operator;

/**
 * Generator for StaticSpecificationService classes
 * (ClassNameSpecificationService). Creates validation methods for both
 * Specification and Filter structures.
 * <p>
 * This generator creates a central validation layer that eliminates reflection
 * usage by generating specific validation methods for each field-operator
 * combination.
 */
public class StaticSpecificationServiceGenerator {

    private static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    private static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private static final String JAVA_LANG_LONG = "java.lang.Long";
    private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
    private static final String ENTITY_NULL_CHECK = "        if (entity == null) {";
    private static final String RETURN_FALSE = "            return false;";
    private static final String CLOSING_BRACE = "        }";
    private static final String OVERRIDE_ANNOTATION = "    @Override";
    private static final String SUPPRESS_WARNINGS_UNCHECKED = "    @SuppressWarnings(\"unchecked\")";
    private static final String SERVICE_VARIABLE = "                    (com.thy.fss.common.inmemory.specification.SpecificationService<Object>) ";
    private static final String COLLECTION_OP_NON_COLLECTION = "                        \"Collection operation applied to non-collection field: \" + metaAttribute.getName()";
    private static final String CLOSING_PARENTHESIS_SEMICOLON = "                    );";
    private static final String EXP_1 = "            }";
    private static final String EXP_2 = "     */";
    private static final String EXP_3 = "    /**";
    private static final String EXP_4 = "                }";
    

    public static final String SPEC_REG_PACKAGE = "com.thy.fss.common.inmemory.specification";
    // Debug mode flag - controlled by system property
    private static final boolean DEBUG_MODE = Boolean.parseBoolean(
            System.getProperty("inmemory.processor.debug", "false"));
    // Supported field types and their corresponding validation method prefixes
    private static final Map<String, String> FIELD_TYPE_PREFIXES;
    // Operators supported by each field type
    private static final Map<String, Set<Operator>> FIELD_TYPE_OPERATORS;

    static {
        Map<String, String> prefixes = new HashMap<>();
        // Wrapper types
        prefixes.put("java.lang.String", "String");
        prefixes.put(JAVA_LANG_INTEGER, "Integer");
        prefixes.put(JAVA_LANG_LONG, "Long");
        prefixes.put(JAVA_LANG_DOUBLE, "Double");
        prefixes.put("java.lang.Float", "Float");
        prefixes.put(JAVA_LANG_BOOLEAN, "Boolean");
        prefixes.put("java.lang.Byte", "Byte");
        prefixes.put("java.lang.Short", "Short");
        prefixes.put("java.lang.Character", "Character");

        // Primitive types (mapped to their wrapper equivalents)
        prefixes.put("boolean", "Boolean");
        prefixes.put("int", "Integer");
        prefixes.put("long", "Long");
        prefixes.put("double", "Double");
        prefixes.put("float", "Float");
        prefixes.put("byte", "Byte");
        prefixes.put("short", "Short");
        prefixes.put("char", "Character");

        // Date/Time types
        prefixes.put("java.time.LocalDate", "LocalDate");
        prefixes.put("java.time.LocalDateTime", "LocalDateTime");
        prefixes.put("java.time.Instant", "Instant");

        FIELD_TYPE_PREFIXES = Map.copyOf(prefixes);
    }

    static {
        Map<String, Set<Operator>> operators = new HashMap<>();

        // String operators
        operators.put("String", Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.CONTAINS, Operator.STARTS_WITH,
                Operator.ENDS_WITH, Operator.IS_EMPTY, Operator.IS_NOT_EMPTY, Operator.IS_BLANK,
                Operator.IS_NOT_BLANK, Operator.IS_NULL, Operator.IS_NOT_NULL,
                Operator.IN, Operator.NOT_IN));

        // Numeric operators (Integer, Long, Double, Float, Byte, Short)
        Set<Operator> numericOps = Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.GREATER_THAN, Operator.LESS_THAN,
                Operator.GREATER_OR_EQUAL_THAN, Operator.LESS_OR_EQUAL_THAN,
                Operator.NOT_GREATER_THAN, Operator.NOT_LESS_THAN,
                Operator.NOT_GREATER_OR_EQUAL_THAN, Operator.NOT_LESS_OR_EQUAL_THAN,
                Operator.IS_NULL, Operator.IS_NOT_NULL, Operator.IN, Operator.NOT_IN);
        operators.put("Integer", numericOps);
        operators.put("Long", numericOps);
        operators.put("Double", numericOps);
        operators.put("Float", numericOps);
        operators.put("Byte", numericOps);
        operators.put("Short", numericOps);

        // Boolean operators
        operators.put("Boolean", Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL,
                Operator.IN, Operator.NOT_IN));

        // Character operators
        operators.put("Character", Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.GREATER_THAN, Operator.LESS_THAN,
                Operator.GREATER_OR_EQUAL_THAN, Operator.LESS_OR_EQUAL_THAN,
                Operator.IS_NULL, Operator.IS_NOT_NULL, Operator.IN, Operator.NOT_IN));

        // Enum operators
        operators.put("Enum", Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL,
                Operator.IN, Operator.NOT_IN));

        // Date/Time operators
        Set<Operator> dateTimeOps = Set.of(Operator.EQUALS, Operator.NOT_EQUALS, Operator.IS_BEFORE, Operator.IS_AFTER,
                Operator.IS_ON_OR_BEFORE, Operator.IS_ON_OR_AFTER,
                Operator.NOT_IS_BEFORE, Operator.NOT_IS_AFTER,
                Operator.NOT_IS_ON_OR_BEFORE, Operator.NOT_IS_ON_OR_AFTER,
            Operator.LAST, Operator.NEXT,
                Operator.IS_NULL, Operator.IS_NOT_NULL, Operator.IN, Operator.NOT_IN);
        operators.put("LocalDate", dateTimeOps);
        operators.put("LocalDateTime", dateTimeOps);
        operators.put("Instant", dateTimeOps);

        // Collection operators
        operators.put("Collection", Set.of(Operator.EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL,
                Operator.CONTAINS, Operator.IN, Operator.NOT_IN,
                Operator.COLLECTION_CONTAINS, Operator.COLLECTION_ANY,
                Operator.COLLECTION_ALL, Operator.COLLECTION_NONE,
                Operator.IS_EMPTY, Operator.IS_NOT_EMPTY));

        FIELD_TYPE_OPERATORS = Map.copyOf(operators);
    }

    private final ProcessingEnvironment processingEnv;
    private final List<TypeElement> processedEntities = new ArrayList<>();

    /**
     * Constructor for StaticSpecificationServiceGenerator.
     *
     * @param processingEnv the processing environment
     */
    public StaticSpecificationServiceGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Prints debug message only if debug mode is enabled.
     */
    private void debugLog(String message) {
        if (DEBUG_MODE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[DEBUG] StaticSpecificationServiceGenerator: " + message
            );
        }
    }

    /**
     * Generates a StaticSpecificationService class for the given type element.
     *
     * @param typeElement the class to generate a specification service for
     * @throws ProcessingException if generation fails
     */
    public void generate(TypeElement typeElement) throws ProcessingException {
        // Track processed entities for registry generation
        processedEntities.add(typeElement);
        String className = typeElement.getQualifiedName().toString();
        String simpleClassName = typeElement.getSimpleName().toString();
        String packageName = getPackageName(className);
        String serviceClassName = simpleClassName + "SpecificationService";

        // Debug log: Generation start
        debugLog("Starting generation for class " + className);
        debugLog("Package: " + packageName + ", Service class: " + serviceClassName);

        try {
            // Debug log: File creation
            debugLog("Creating source file: " + packageName + "." + serviceClassName);

            // Create the Java file
            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + serviceClassName);

            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                debugLog("Starting service class generation");

                generateServiceClass(out, typeElement, packageName, serviceClassName, simpleClassName);

                debugLog("Service class generation completed");
            }

            // Debug log: Success
            debugLog("File written successfully");

            debugLog("Generated StaticSpecificationService: " + packageName + "." + serviceClassName);

        } catch (IOException e) {
            // Keep error messages as they are important
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "StaticSpecificationServiceGenerator: IOException occurred: " + e.getMessage()
            );
            throw new ProcessingException("Failed to generate StaticSpecificationService for " + className, e);
        } catch (Exception e) {
            // Keep error messages as they are important
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "StaticSpecificationServiceGenerator: Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            );
            throw new ProcessingException("Unexpected error during StaticSpecificationService generation for " + className, e);
        }
    }

    /**
     * Generates the complete service class content.
     */
    private void generateServiceClass(PrintWriter out, TypeElement typeElement, String packageName,
                                      String serviceClassName, String simpleClassName) {

        // Debug log: Service class generation start
        debugLog("Generating service class " + serviceClassName);

        // Analyze fields to determine what validation methods to generate
        debugLog("Starting field analysis");
        List<FieldInfo> fields = analyzeFields(typeElement);

        // Analyze dependencies for nested model services
        List<String> dependentModelTypes = analyzeDependentModelTypes(fields);
        debugLog("Found " + dependentModelTypes.size() + " dependent model types");

        debugLog("Field analysis completed, found " + fields.size() + " fields");

        // Generate package and imports
        debugLog("Generating package and imports");
        generatePackageAndImports(out, packageName, simpleClassName, dependentModelTypes);

        // Generate class declaration
        debugLog("Generating class declaration");
        generateClassDeclaration(out, serviceClassName, simpleClassName);

        // Generate dependent service instance variables
        debugLog("Generating dependent service instance variables");
        generateDependentServiceFields(out, serviceClassName, dependentModelTypes);

        // Generate field validation methods for each field-operator combination
        debugLog("Generating field validation methods");
        generateFieldValidationMethods(out, fields, simpleClassName);

        // Generate the main validateSpecification method
        debugLog("Generating validateSpecification method");
        generateValidateSpecificationMethod(out, fields, simpleClassName);

        // Generate the validateFilter method
        debugLog("Generating validateFilter method");
        generateValidateFilterMethod(out, fields, simpleClassName);

        // Generate getEntityClass method
        debugLog("Generating getEntityClass method");
        generateGetEntityClassMethod(out, fields, simpleClassName);

        // Generate field value extraction methods
        debugLog("Generating field value extraction methods");
        generateFieldValueExtractionMethods(out, fields, simpleClassName);

        // Generate setFieldValue method
        generateSetFieldValueMethod(out, fields, simpleClassName);

        // Generate sorting support methods
        debugLog("Generating sorting methods");
        generateSortingMethods(out, fields, simpleClassName);

        // Generate collection operation methods with specifications
        debugLog("Generating collection operation methods");
        generateCollectionOperationMethods(out, fields, simpleClassName);

        // Generate delegation methods for path navigation (eliminates runtime lookups)
        debugLog("Generating delegation methods");
        generateDelegationMethods(out, fields, simpleClassName);

        // Note: Static initializer removed - services are now accessed via SpecificationServiceRegistry
        debugLog("Skipping static initializer - using registry-based approach");

        // Close class
        out.println("}");

        // Log service generation
        debugLog("Service class generation completed");
        logServiceGeneration(packageName, serviceClassName, simpleClassName);
    }

    /**
     * Analyzes the fields of the type element to extract field information.
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
     * Analyzes a single field to extract its information.
     */
    private FieldInfo analyzeField(VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String fieldTypeName = getFieldTypeName(fieldType);

        debugLog("Field '" + fieldName + "' has type: " + fieldTypeName);

        // Determine attribute type and validation prefix
        AttributeType attributeType = determineAttributeType(fieldType);
        debugLog("Field '" + fieldName + "' determined attribute type: " + attributeType);

        String validationPrefix = getValidationPrefix(fieldTypeName, attributeType);

        if (validationPrefix == null) {
            // Skip unsupported field types
            debugLog("Skipping unsupported field type: " + fieldTypeName + " for field: " + fieldName);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Skipping unsupported field type: " + fieldTypeName + " for field: " + fieldName
            );
            return null;
        }

        debugLog("Field '" + fieldName + "' validation prefix: " + validationPrefix);

        boolean isCollection = isCollection(fieldType);
        String collectionElementType = isCollection ? getCollectionElementType(fieldType) : null;

        debugLog("Field '" + fieldName + "' - IsCollection: " + isCollection
                + ", ElementType: " + collectionElementType);

        boolean isEnumField = (attributeType == AttributeType.ENUM);

        String getterMethodName = getGetterMethodName(fieldName, (TypeElement) field.getEnclosingElement(), fieldTypeName);
        String setterMethodName = getSetterMethodName(fieldName);

        return new FieldInfo(fieldName, fieldTypeName, attributeType, validationPrefix,
                getterMethodName, setterMethodName, isCollection, isEnumField, collectionElementType, fieldType);
    }

    /**
     * Determines the attribute type of a field.
     */
    private AttributeType determineAttributeType(TypeMirror fieldType) {
        if (isCollection(fieldType)) {
            debugLog("Field type determined as COLLECTION");
            return AttributeType.COLLECTION;
        } else if (isEnum(fieldType)) {
            debugLog("Field type determined as ENUM");
            return AttributeType.ENUM;
        } else if (isModelType(fieldType)) {
            debugLog("Field type determined as MODEL");
            return AttributeType.MODEL;
        } else {
            debugLog("Field type determined as SINGLE");
            return AttributeType.SINGLE;
        }
    }

    /**
     * Checks if a type is a collection type.
     */
    private boolean isCollection(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }

        DeclaredType declaredType = (DeclaredType) type;
        String typeName = declaredType.asElement().toString();

        return typeName.equals("java.util.List")
                || typeName.equals("java.util.Set")
                || typeName.equals("java.util.Collection");
    }

    /**
     * Checks if a type is a model type (custom class that could have
     *
     * @MetaModel).
     */
    private boolean isModelType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }

        String typeName = getFieldTypeName(type);

        // Consider it a model type if it's not a standard Java type
        return !FIELD_TYPE_PREFIXES.containsKey(typeName)
                && !typeName.startsWith("java.")
                && !typeName.startsWith("javax.")
                && !isEnum(type);
    }

    /**
     * Checks if a type is an enum.
     */
    private boolean isEnum(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }

        DeclaredType declaredType = (DeclaredType) type;
        Element element = declaredType.asElement();

        return element.getKind() == ElementKind.ENUM;
    }

    /**
     * Gets the collection element type if the field is a collection.
     */
    private String getCollectionElementType(TypeMirror type) {
        if (!isCollection(type)) {
            return null;
        }

        if (type instanceof DeclaredType declaredType) {
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return getFieldTypeName(typeArguments.get(0));
            }
        }

        return "java.lang.Object"; // Default if no generic type found
    }

    /**
     * Gets the field type name from a TypeMirror.
     */
    private String getFieldTypeName(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement.getQualifiedName().toString();
            }
        }
        return type.toString();
    }

    /**
     * Gets the validation prefix for a field type.
     */
    private String getValidationPrefix(String fieldTypeName, AttributeType attributeType) {
        if (attributeType == AttributeType.COLLECTION) {
            return "Collection";
        } else if (attributeType == AttributeType.ENUM) {
            return "Enum";
        } else if (attributeType == AttributeType.MODEL) {
            return "Model";
        } else {
            return FIELD_TYPE_PREFIXES.get(fieldTypeName);
        }
    }

    /**
     * Gets the getter method name for a field. Special handling for 'id' field
     * which uses getId() method from Identifiable interface. Special handling
     * for boolean fields which use is* prefix.
     */
    private String getGetterMethodName(String fieldName, TypeElement typeElement, String fieldTypeName) {
//        // Special case for 'id' field - check if class implements Identifiable interface
//        if ("id".equals(fieldName)) {
//            if (implementsIdentifiable(typeElement)) {
//                return "id";
//            }
//        }

        // Special case for boolean fields - use 'is' prefix
        if ("boolean".equals(fieldTypeName)) {
            // Handle Lombok behavior: if field already starts with "is", don't add another "is"
            // e.g., "isVerified" -> "isVerified()", not "isIsVerified()"
            if (fieldName.startsWith("is") && fieldName.length() > 2 && Character.isUpperCase(fieldName.charAt(2))) {
                return fieldName; // Field already has "is" prefix
            }
            return "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }

        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * Checks if a type element implements the Identifiable interface.
     */
    private boolean implementsIdentifiable(TypeElement typeElement) {
        return typeElement.getInterfaces().stream()
                .anyMatch(interfaceType -> {
                    if (interfaceType instanceof DeclaredType declaredType) {
                        Element element = declaredType.asElement();
                        if (element instanceof TypeElement) {
                            String interfaceName = ((TypeElement) element).getQualifiedName().toString();
                            return "com.thy.fss.common.inmemory.entity.Identifiable".equals(interfaceName);
                        }
                    }
                    return false;
                });
    }

    /**
     * Gets the setter method name for a field. Handles Lombok behavior for
     * boolean fields starting with "is".
     */
    private String getSetterMethodName(String fieldName) {
        // Handle Lombok behavior: if field starts with "is", remove it for setter
        // e.g., "isVerified" -> "setVerified()", not "setIsVerified()"
        if (fieldName.startsWith("is") && fieldName.length() > 2 && Character.isUpperCase(fieldName.charAt(2))) {
            return "set" + fieldName.substring(2); // Remove "is" prefix
        }
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * Capitalizes the first letter of a string for getter/setter method names.
     * For boolean fields starting with "is", removes the "is" prefix since
     * Lombok generates getters without the duplicate "is" (e.g., isActive ->
     * isActive(), not isIsActive()).
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
     * Checks if a field type is a primitive type.
     */
    private boolean isPrimitiveType(String fieldTypeName) {
        return "boolean".equals(fieldTypeName)
                || "int".equals(fieldTypeName)
                || "long".equals(fieldTypeName)
                || "double".equals(fieldTypeName)
                || "float".equals(fieldTypeName)
                || "byte".equals(fieldTypeName)
                || "short".equals(fieldTypeName)
                || "char".equals(fieldTypeName);
    }

    /**
     * Gets the wrapper type for a primitive type, or returns the type as-is if
     * it's not primitive.
     */
    private String getWrapperType(String fieldTypeName) {
        return switch (fieldTypeName) {
            case "boolean" -> JAVA_LANG_BOOLEAN;
            case "int" -> JAVA_LANG_INTEGER;
            case "long" -> JAVA_LANG_LONG;
            case "double" -> JAVA_LANG_DOUBLE;
            case "float" -> "java.lang.Float";
            case "byte" -> "java.lang.Byte";
            case "short" -> "java.lang.Short";
            case "char" -> "java.lang.Character";
            default -> fieldTypeName; // Already a wrapper or other type
        };
    }

    /**
     * Generates package declaration and imports.
     */
    private void generatePackageAndImports(PrintWriter out, String packageName, String simpleClassName, List<String> dependentModelTypes) {
        out.println("package " + packageName + ";");
        out.println();
        out.println("import com.thy.fss.common.inmemory.specification.SpecificationService;");
        out.println("import com.thy.fss.common.inmemory.specification.Operator;");
        out.println("import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;");
        out.println("import com.thy.fss.common.inmemory.filter.*;");
        out.println("import java.util.*;");
        out.println("import java.time.*;");

        if (dependentModelTypes != null) {
            for (String modelType : dependentModelTypes) {
                String modelPackage = getPackageName(modelType);
                // Only add cross-package imports when the dependent model is in a different package
                if (!modelPackage.isEmpty() && !modelPackage.equals(packageName)) {
                    // Import the entity class itself (used in collection type declarations and casts)
                    out.println("import " + modelType + ";");
                    // Import the Filter class (used in model type collection validation)
                    out.println("import " + modelType + "Filter;");
                    // Import the static metamodel class (used in navigateNested for attr comparison)
                    out.println("import " + modelType + "_;");
                }
                // Import the SpecificationService class (always needed for .INSTANCE references)
                out.println("import " + modelType + "SpecificationService;");
            }
        }

        out.println();
        out.println("/**");
        out.println(" * Generated StaticSpecificationService for " + simpleClassName + ".");
        out.println(" * Provides validation methods for both Specification and Filter structures.");
        out.println(" * This class eliminates reflection usage by providing direct field access.");
        out.println(" * ");
        out.println(" * Generated at build time by MetaModelProcessor.");
        out.println(" */");
    }

    /**
     * Generates the class declaration. Generated services extend
     * BaseSpecificationService to inherit collection operation implementations.
     */
    private void generateClassDeclaration(PrintWriter out, String serviceClassName, String simpleClassName) {
        out.println("public class " + serviceClassName + " extends com.thy.fss.common.inmemory.specification.BaseSpecificationService<" + simpleClassName + "> {");
        out.println();
    }

    /**
     * Generates dependent service instance variables.
     */
    private void generateDependentServiceFields(PrintWriter out, String serviceClassName, List<String> dependentModelTypes) {
        // Generate static INSTANCE variable for this service
        out.println(EXP_3);
        out.println("     * Static instance of this service for dependency injection.");
        out.println(EXP_2);
        out.println("    public static final " + serviceClassName + " INSTANCE = new " + serviceClassName + "();");
        out.println();

        // No need for instance variables since we'll use static INSTANCE references
        out.println();
    }

    /**
     * Generates field validation methods for each field-operator combination.
     */
    private void generateFieldValidationMethods(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ========== Field Validation Methods ==========");
        out.println();

        for (FieldInfo field : fields) {
            generateFieldValidationMethodsForField(out, field, simpleClassName);
        }

        // Generate element validation helper methods
        generateElementValidationHelperMethods(out);
    }

    /**
     * Generates validation methods for a specific field.
     */
    private void generateFieldValidationMethodsForField(PrintWriter out, FieldInfo field, String simpleClassName) {
        Set<Operator> supportedOperators = FIELD_TYPE_OPERATORS.get(field.validationPrefix);
        if (supportedOperators == null) {
            // For model and collection types, generate basic operators
            supportedOperators = Set.of(Operator.EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL);
        }

        for (Operator operator : supportedOperators) {
            generateValidationMethod(out, field, operator, simpleClassName);
        }
        
        // Generate additional validation methods for model type collections
        if (field.isCollection && field.collectionElementType != null && isModelElementType(field.collectionElementType)) {
            generateModelTypeCollectionValidationMethods(out, field, simpleClassName);
        }

        out.println();
    }
    
    /**
     * Generates validation methods for model type collection fields with element type service delegation.
     */
    private void generateModelTypeCollectionValidationMethods(PrintWriter out, FieldInfo field, String simpleClassName) {
        String fieldPrefix = capitalize(field.fieldName);
        String elementTypeName = field.collectionElementType;
        String simpleElementName = elementTypeName.substring(elementTypeName.lastIndexOf('.') + 1);
        String elementServiceClass = simpleElementName + "SpecificationService";
        
        // Generate ANY operator validation with model filter
        out.println(EXP_3);
        out.println("     * Validates " + field.fieldName + " collection with ANY operator using model filter and element type service.");
        out.println(EXP_2);
        out.println("    public boolean validate" + fieldPrefix + "CollectionAnyWithModelFilter(" + simpleClassName + " entity, ");
        out.println("            " + simpleElementName + "Filter elementFilter, ");
        out.println("            " + elementServiceClass + " elementService) {");
        out.println(ENTITY_NULL_CHECK);
        out.println(RETURN_FALSE);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        java.util.Collection<" + simpleElementName + "> collection = entity." + field.getterMethodName + "();");
        out.println("        if (collection == null || collection.isEmpty()) {");
        out.println("            return false;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // At least one element must match the filter");
        out.println("        return collection.stream().anyMatch(element -> ");
        out.println("            validateCollectionElement(element, elementFilter, elementService));");
        out.println("    }");
        out.println();
        
        // Generate ALL operator validation with model filter
        out.println(EXP_3);
        out.println("     * Validates " + field.fieldName + " collection with ALL operator using model filter and element type service.");
        out.println(EXP_2);
        out.println("    public boolean validate" + fieldPrefix + "CollectionAllWithModelFilter(" + simpleClassName + " entity, ");
        out.println("            " + simpleElementName + "Filter elementFilter, ");
        out.println("            " + elementServiceClass + " elementService) {");
        out.println(ENTITY_NULL_CHECK);
        out.println(RETURN_FALSE);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        java.util.Collection<" + simpleElementName + "> collection = entity." + field.getterMethodName + "();");
        out.println("        if (collection == null || collection.isEmpty()) {");
        out.println("            return false;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // All elements must match the filter");
        out.println("        return collection.stream().allMatch(element -> ");
        out.println("            validateCollectionElement(element, elementFilter, elementService));");
        out.println("    }");
        out.println();
        
        // Generate NONE operator validation with model filter
        out.println(EXP_3);
        out.println("     * Validates " + field.fieldName + " collection with NONE operator using model filter and element type service.");
        out.println(EXP_2);
        out.println("    public boolean validate" + fieldPrefix + "CollectionNoneWithModelFilter(" + simpleClassName + " entity, ");
        out.println("            " + simpleElementName + "Filter elementFilter, ");
        out.println("            " + elementServiceClass + " elementService) {");
        out.println(ENTITY_NULL_CHECK);
        out.println(RETURN_FALSE);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        java.util.Collection<" + simpleElementName + "> collection = entity." + field.getterMethodName + "();");
        out.println("        if (collection == null) {");
        out.println("            return true; // Null collection means no elements match");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // No elements should match the filter");
        out.println("        return collection.stream().noneMatch(element -> ");
        out.println("            validateCollectionElement(element, elementFilter, elementService));");
        out.println("    }");
        out.println();
    }

    /**
     * Generates a single validation method for a field-operator combination.
     */
    private void generateValidationMethod(PrintWriter out, FieldInfo field, Operator operator, String simpleClassName) {
        String methodName = "validate" + capitalize(field.fieldName) + getOperatorMethodSuffix(operator);
        String parameterType = getParameterType(field, operator);

        out.println(EXP_3);
        out.println("     * Validates " + field.fieldName + " field with " + operator + " operator.");
        out.println(EXP_2);
        out.println("    public boolean " + methodName + "(" + simpleClassName + " entity, " + parameterType + " value) {");

        generateValidationMethodBody(out, field, operator);

        out.println("    }");
        out.println();
    }

    /**
     * Generates the body of a validation method.
     */
    private void generateValidationMethodBody(PrintWriter out, FieldInfo field, Operator operator) {
        // Add null check for entity parameter with correct semantics
        // When entity is null, all fields are effectively null
        if (operator == Operator.IS_NULL) {
            // When entity is null, the field is null, so isNull check should pass if value is true
            out.println(ENTITY_NULL_CHECK);
            out.println("            return (value == null || value.booleanValue());");
            out.println(CLOSING_BRACE);
        } else if (operator == Operator.IS_NOT_NULL) {
            // When entity is null, the field is null, so isNotNull check should fail if value is true
            out.println(ENTITY_NULL_CHECK);
            out.println("            return !(value == null || value.booleanValue());");
            out.println(CLOSING_BRACE);
        } else {
            out.println(ENTITY_NULL_CHECK);
            out.println(RETURN_FALSE);
            out.println(CLOSING_BRACE);
        }

        String fieldAccess = "entity." + field.getterMethodName + "()";

        switch (operator) {
            case EQUALS -> {
                if (field.validationPrefix.equals("Collection")) {
                    // For collections, compare the entire collection using Collection interface
                    out.println("        if (" + fieldAccess + " == null && value == null) return true;");
                    out.println("        if (" + fieldAccess + " == null || value == null) return false;");
                    out.println("        return " + fieldAccess + ".size() == value.size() && " + fieldAccess + ".containsAll(value);");
                } else {
                    out.println("        return Objects.equals(" + fieldAccess + ", value);");
                }
            }
            case NOT_EQUALS -> {
                out.println("        return !Objects.equals(" + fieldAccess + ", value);");
            }
            case IS_NULL -> {
                // Primitive types can never be null, always return false for IS_NULL
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return false; // Primitive types cannot be null");
                } else {
                    // Null-safe check: if value is null or true, check if field is null
                    // if value is false, check if field is not null
                    out.println("        return (value == null || value.booleanValue()) ? " + fieldAccess + " == null : " + fieldAccess + " != null;");
                }
            }
            case IS_NOT_NULL -> {
                // Primitive types are never null, always return true for IS_NOT_NULL
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return true; // Primitive types are never null");
                } else {
                    // Null-safe check: if value is null or true, check if field is not null
                    // if value is false, check if field is null
                    out.println("        return (value == null || value.booleanValue()) ? " + fieldAccess + " != null : " + fieldAccess + " == null;");
                }
            }
            case CONTAINS -> {
                if (field.validationPrefix.equals("String")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".contains(value);");
                } else if (field.validationPrefix.equals("Collection")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".contains(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".contains(value);");
                }
            }
            case STARTS_WITH -> {
                out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".startsWith(value);");
            }
            case ENDS_WITH -> {
                out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".endsWith(value);");
            }
            case IS_EMPTY -> {
                if (field.validationPrefix.equals("String")) {
                    out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && " + fieldAccess + ".isEmpty()) : (" + fieldAccess + " == null || !" + fieldAccess + ".isEmpty());");
                } else {
                    out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && " + fieldAccess + ".isEmpty()) : (" + fieldAccess + " == null || !" + fieldAccess + ".isEmpty());");
                }
            }
            case IS_BLANK -> {
                out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && " + fieldAccess + ".isBlank()) : (" + fieldAccess + " == null || !" + fieldAccess + ".isBlank());");
            }
            case IS_NOT_EMPTY -> {
                if (field.validationPrefix.equals("String")) {
                    out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && !" + fieldAccess + ".isEmpty()) : (" + fieldAccess + " == null || " + fieldAccess + ".isEmpty());");
                } else {
                    out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && !" + fieldAccess + ".isEmpty()) : (" + fieldAccess + " == null || " + fieldAccess + ".isEmpty());");
                }
            }
            case IS_NOT_BLANK -> {
                out.println("        return (value == null || value.booleanValue()) ? (" + fieldAccess + " != null && !" + fieldAccess + ".isBlank()) : (" + fieldAccess + " == null || " + fieldAccess + ".isBlank());");
            }
            case GREATER_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " > value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) > 0;");
                }
            }
            case LESS_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " < value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) < 0;");
                }
            }
            case GREATER_OR_EQUAL_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " >= value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) >= 0;");
                }
            }
            case LESS_OR_EQUAL_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " <= value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) <= 0;");
                }
            }
            case IS_BEFORE -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) < 0;");
                }
            }
            case IS_AFTER -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) > 0;");
                }
            }
            case IS_ON_OR_BEFORE -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) <= 0;");
                }
            }
            case IS_ON_OR_AFTER -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) >= 0;");
                }
            }
            case NOT_GREATER_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " <= value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) <= 0;");
                }
            }
            case NOT_LESS_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " >= value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) >= 0;");
                }
            }
            case NOT_GREATER_OR_EQUAL_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " < value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) < 0;");
                }
            }
            case NOT_LESS_OR_EQUAL_THAN -> {
                if (isPrimitiveType(field.fieldTypeName)) {
                    out.println("        return " + fieldAccess + " > value;");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) > 0;");
                }
            }
            case NOT_IS_BEFORE -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isBefore(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) >= 0;");
                }
            }
            case NOT_IS_AFTER -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && !" + fieldAccess + ".isAfter(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) <= 0;");
                }
            }
            case NOT_IS_ON_OR_BEFORE -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isAfter(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) > 0;");
                }
            }
            case NOT_IS_ON_OR_AFTER -> {
                if (field.validationPrefix.equals("LocalDate")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("LocalDateTime")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else if (field.validationPrefix.equals("Instant")) {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".isBefore(value);");
                } else {
                    out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".compareTo(value) < 0;");
                }
            }
            case LAST -> {
                out.println("        return com.thy.fss.common.inmemory.filter.TemporalPresetEvaluator.matchesLast(" + fieldAccess + ", value);");
            }
            case NEXT -> {
                out.println("        return com.thy.fss.common.inmemory.filter.TemporalPresetEvaluator.matchesNext(" + fieldAccess + ", value);");
            }
            case IN -> {
                out.println("        return value != null && value.contains(" + fieldAccess + ");");
            }
            case NOT_IN -> {
                out.println("        return value == null || !value.contains(" + fieldAccess + ");");
            }
            case COLLECTION_CONTAINS -> {
                out.println("        return " + fieldAccess + " != null && " + fieldAccess + ".contains(value);");
            }
            case COLLECTION_ANY -> {
                out.println("        if (" + fieldAccess + " == null) return false;");
                out.println("        return " + fieldAccess + ".stream().anyMatch(element -> ");
                out.println("            validateElementAgainstFilter(element, value));");
            }
            case COLLECTION_ALL -> {
                out.println("        if (" + fieldAccess + " == null) return false;");
                out.println("        return " + fieldAccess + ".stream().allMatch(element -> ");
                out.println("            validateElementAgainstFilter(element, value));");
            }
            case COLLECTION_NONE -> {
                out.println("        if (" + fieldAccess + " == null) return true;");
                out.println("        return " + fieldAccess + ".stream().noneMatch(element -> ");
                out.println("            validateElementAgainstFilter(element, value));");
            }
            default -> {
                out.println("        throw new UnsupportedOperationException(\"Operator " + operator + " not implemented for " + field.validationPrefix + "\");");
            }
        }
    }

    /**
     * Gets the method suffix for an operator.
     */
    private String getOperatorMethodSuffix(Operator operator) {
        return switch (operator) {
            case EQUALS -> "Equals";
            case NOT_EQUALS -> "NotEquals";
            case GREATER_THAN -> "GreaterThan";
            case LESS_THAN -> "LessThan";
            case GREATER_OR_EQUAL_THAN -> "GreaterOrEqualThan";
            case LESS_OR_EQUAL_THAN -> "LessOrEqualThan";
            case CONTAINS -> "Contains";
            case STARTS_WITH -> "StartsWith";
            case ENDS_WITH -> "EndsWith";
            case IS_EMPTY -> "IsEmpty";
            case IS_NOT_EMPTY -> "IsNotEmpty";
            case IS_BLANK -> "IsBlank";
            case IS_NOT_BLANK -> "IsNotBlank";
            case IS_BEFORE -> "IsBefore";
            case IS_AFTER -> "IsAfter";
            case IS_ON_OR_BEFORE -> "IsOnOrBefore";
            case IS_ON_OR_AFTER -> "IsOnOrAfter";
            case LAST -> "Last";
            case NEXT -> "Next";
            case IS_NULL -> "IsNull";
            case IS_NOT_NULL -> "IsNotNull";
            case IN -> "In";
            case NOT_IN -> "NotIn";
            case NOT_GREATER_THAN -> "NotGreaterThan";
            case NOT_LESS_THAN -> "NotLessThan";
            case NOT_GREATER_OR_EQUAL_THAN -> "NotGreaterOrEqualThan";
            case NOT_LESS_OR_EQUAL_THAN -> "NotLessOrEqualThan";
            case NOT_IS_BEFORE -> "NotIsBefore";
            case NOT_IS_AFTER -> "NotIsAfter";
            case NOT_IS_ON_OR_BEFORE -> "NotIsOnOrBefore";
            case NOT_IS_ON_OR_AFTER -> "NotIsOnOrAfter";
            case COLLECTION_CONTAINS -> "CollectionContains";
            case COLLECTION_ANY -> "CollectionAny";
            case COLLECTION_ALL -> "CollectionAll";
            case COLLECTION_NONE -> "CollectionNone";
            default -> operator.name();
        };
    }

    /**
     * Generates element validation helper methods for collection operations.
     */
    private void generateElementValidationHelperMethods(PrintWriter out) {
        out.println("    // ========== Element Validation Helper Methods ==========");
        out.println();

        out.println(EXP_3);
        out.println("     * Helper method to validate collection elements against filter criteria.");
        out.println("     * Used by COLLECTION_ANY, COLLECTION_ALL, and COLLECTION_NONE operations.");
        out.println(EXP_2);
        out.println("    private boolean validateElementAgainstFilter(Object element, Object filterValue) {");
        out.println("        if (filterValue == null) {");
        out.println("            return element == null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Handle Filter objects for complex element validation");
        out.println("        if (filterValue instanceof com.thy.fss.common.inmemory.filter.Filter<?> filter) {");
        out.println("            return validateElementWithFilter(element, filter);");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Simple equality check for primitive values");
        out.println("        return java.util.Objects.equals(element, filterValue);");
        out.println("    }");
        out.println();

        out.println(EXP_3);
        out.println("     * Validates an element against a specific filter type.");
        out.println(EXP_2);
        out.println("    private boolean validateElementWithFilter(Object element, com.thy.fss.common.inmemory.filter.Filter<?> filter) {");
        out.println("        if (element == null) {");
        out.println("            return filter.getIsNull() != null && filter.getIsNull();");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Handle different filter types");
        out.println("        if (filter instanceof com.thy.fss.common.inmemory.filter.StringFilter && element instanceof String) {");
        out.println("            return validateStringElement((String) element, (com.thy.fss.common.inmemory.filter.StringFilter) filter);");
        out.println(CLOSING_BRACE);
        out.println("        if (filter instanceof com.thy.fss.common.inmemory.filter.NumberFilter && element instanceof Number) {");
        out.println("            return validateNumberElement((Number) element, (com.thy.fss.common.inmemory.filter.NumberFilter<?>) filter);");
        out.println(CLOSING_BRACE);
        out.println("        if (filter instanceof com.thy.fss.common.inmemory.filter.BooleanFilter && element instanceof Boolean) {");
        out.println("            return validateBooleanElement((Boolean) element, (com.thy.fss.common.inmemory.filter.BooleanFilter) filter);");
        out.println(CLOSING_BRACE);
        out.println("        if (filter instanceof com.thy.fss.common.inmemory.filter.TemporalFilter && ");
        out.println("                (element instanceof java.time.LocalDate || element instanceof java.time.LocalDateTime || element instanceof java.time.Instant)) {");
        out.println("            return validateTemporalElement(element, (com.thy.fss.common.inmemory.filter.TemporalFilter<?>) filter);");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // For other types, check basic equality");
        out.println("        return java.util.Objects.equals(element, filter.getEquals());");
        out.println("    }");
        out.println();

        generateElementTypeValidationMethods(out);
    }

    /**
     * Generates element-specific validation methods for different filter types.
     */
    private void generateElementTypeValidationMethods(PrintWriter out) {
        // String element validation
        out.println(EXP_3);
        out.println("     * Validates a string element against a StringFilter.");
        out.println(EXP_2);
        out.println("    private boolean validateStringElement(String element, com.thy.fss.common.inmemory.filter.StringFilter filter) {");
        out.println("        if (filter.getEquals() != null && !java.util.Objects.equals(element, filter.getEquals())) return false;");
        out.println("        if (filter.getNotEquals() != null && java.util.Objects.equals(element, filter.getNotEquals())) return false;");
        out.println("        if (filter.getIn() != null && !filter.getIn().contains(element)) return false;");
        out.println("        if (filter.getNotIn() != null && filter.getNotIn().contains(element)) return false;");
        out.println("        if (filter.getContains() != null && (element == null || !element.contains(filter.getContains()))) return false;");
        out.println("        if (filter.getStartsWith() != null && (element == null || !element.startsWith(filter.getStartsWith()))) return false;");
        out.println("        if (filter.getEndsWith() != null && (element == null || !element.endsWith(filter.getEndsWith()))) return false;");
        out.println("        if (filter.getIsEmpty() != null && filter.getIsEmpty() && (element == null || !element.isEmpty())) return false;");
        out.println("        if (filter.getIsNotEmpty() != null && filter.getIsNotEmpty() && (element == null || element.isEmpty())) return false;");
        out.println("        if (filter.getIsNull() != null && filter.getIsNull() && element != null) return false;");
        out.println("        if (filter.getIsNotNull() != null && filter.getIsNotNull() && element == null) return false;");
        out.println("        return true;");
        out.println("    }");
        out.println();

        // Number element validation
        out.println(EXP_3);
        out.println("     * Validates a number element against a NumberFilter.");
        out.println(EXP_2);
        out.println("    private boolean validateNumberElement(Number element, com.thy.fss.common.inmemory.filter.NumberFilter<?> filter) {");
        out.println("        if (filter.getEquals() != null && !java.util.Objects.equals(element, filter.getEquals())) return false;");
        out.println("        if (filter.getNotEquals() != null && java.util.Objects.equals(element, filter.getNotEquals())) return false;");
        out.println("        if (filter.getIn() != null && !filter.getIn().contains(element)) return false;");
        out.println("        if (filter.getNotIn() != null && filter.getNotIn().contains(element)) return false;");
        out.println("        if (filter.getIsNull() != null && filter.getIsNull() && element != null) return false;");
        out.println("        if (filter.getIsNotNull() != null && filter.getIsNotNull() && element == null) return false;");
        out.println("        // Add numeric comparisons if needed");
        out.println("        return true;");
        out.println("    }");
        out.println();

        // Boolean element validation
        out.println(EXP_3);
        out.println("     * Validates a boolean element against a BooleanFilter.");
        out.println(EXP_2);
        out.println("    private boolean validateBooleanElement(Boolean element, com.thy.fss.common.inmemory.filter.BooleanFilter filter) {");
        out.println("        if (filter.getEquals() != null && !java.util.Objects.equals(element, filter.getEquals())) return false;");
        out.println("        if (filter.getNotEquals() != null && java.util.Objects.equals(element, filter.getNotEquals())) return false;");
        out.println("        if (filter.getIn() != null && !filter.getIn().contains(element)) return false;");
        out.println("        if (filter.getNotIn() != null && filter.getNotIn().contains(element)) return false;");
        out.println("        if (filter.getIsNull() != null && filter.getIsNull() && element != null) return false;");
        out.println("        if (filter.getIsNotNull() != null && filter.getIsNotNull() && element == null) return false;");
        out.println("        return true;");
        out.println("    }");
        out.println();

        // Temporal element validation
        out.println(EXP_3);
        out.println("     * Validates a temporal element against a TemporalFilter.");
        out.println(EXP_2);
        out.println("    private boolean validateTemporalElement(Object element, com.thy.fss.common.inmemory.filter.TemporalFilter<?> filter) {");
        out.println("        if (filter.getEquals() != null && !java.util.Objects.equals(element, filter.getEquals())) return false;");
        out.println("        if (filter.getNotEquals() != null && java.util.Objects.equals(element, filter.getNotEquals())) return false;");
        out.println("        if (filter.getIn() != null && !filter.getIn().contains(element)) return false;");
        out.println("        if (filter.getNotIn() != null && filter.getNotIn().contains(element)) return false;");
        out.println("        if (filter.getIsNull() != null && filter.getIsNull() && element != null) return false;");
        out.println("        if (filter.getIsNotNull() != null && filter.getIsNotNull() && element == null) return false;");
        out.println("        if (element == null) return true;");
        out.println();
        out.println("        // Resolve comparison operators by runtime temporal type");
        out.println("        if (element instanceof java.time.LocalDate value) {");
        out.println("            if (filter.getIsBefore() != null && !value.isBefore((java.time.LocalDate) filter.getIsBefore())) return false;");
        out.println("            if (filter.getIsAfter() != null && !value.isAfter((java.time.LocalDate) filter.getIsAfter())) return false;");
        out.println("            if (filter.getIsOnOrBefore() != null && value.isAfter((java.time.LocalDate) filter.getIsOnOrBefore())) return false;");
        out.println("            if (filter.getIsOnOrAfter() != null && value.isBefore((java.time.LocalDate) filter.getIsOnOrAfter())) return false;");
        out.println("        } else if (element instanceof java.time.LocalDateTime value) {");
        out.println("            if (filter.getIsBefore() != null && !value.isBefore((java.time.LocalDateTime) filter.getIsBefore())) return false;");
        out.println("            if (filter.getIsAfter() != null && !value.isAfter((java.time.LocalDateTime) filter.getIsAfter())) return false;");
        out.println("            if (filter.getIsOnOrBefore() != null && value.isAfter((java.time.LocalDateTime) filter.getIsOnOrBefore())) return false;");
        out.println("            if (filter.getIsOnOrAfter() != null && value.isBefore((java.time.LocalDateTime) filter.getIsOnOrAfter())) return false;");
        out.println("        } else if (element instanceof java.time.Instant value) {");
        out.println("            if (filter.getIsBefore() != null && !value.isBefore((java.time.Instant) filter.getIsBefore())) return false;");
        out.println("            if (filter.getIsAfter() != null && !value.isAfter((java.time.Instant) filter.getIsAfter())) return false;");
        out.println("            if (filter.getIsOnOrBefore() != null && value.isAfter((java.time.Instant) filter.getIsOnOrBefore())) return false;");
        out.println("            if (filter.getIsOnOrAfter() != null && value.isBefore((java.time.Instant) filter.getIsOnOrAfter())) return false;");
        out.println("        }");
        out.println();
        out.println("        if (filter.getLast() != null && !com.thy.fss.common.inmemory.filter.TemporalPresetEvaluator.matchesLast(element, filter.getLast())) return false;");
        out.println("        if (filter.getNext() != null && !com.thy.fss.common.inmemory.filter.TemporalPresetEvaluator.matchesNext(element, filter.getNext())) return false;");
        out.println("        return true;");
        out.println("    }");
        out.println();
        
        // Generate model type collection validation helper methods
        generateModelTypeCollectionHelperMethods(out);
    }
    
    /**
     * Generates helper methods for model type collection validation with element type service delegation.
     */
    private void generateModelTypeCollectionHelperMethods(PrintWriter out) {
        out.println(EXP_3);
        out.println("     * Gets the element type specification service for a collection field.");
        out.println("     * Used for model type collection validation.");
        out.println("     * @param elementType the element type class");
        out.println("     * @return the specification service for the element type");
        out.println("     * @throws IllegalStateException if service not found");
        out.println(EXP_2);
        out.println("    @SuppressWarnings(\"unchecked\")");
        out.println("    protected <E> com.thy.fss.common.inmemory.specification.SpecificationService<E> getElementTypeService(Class<E> elementType) {");
        out.println("        if (elementType == null) {");
        out.println("            throw new IllegalArgumentException(\"Element type cannot be null\");");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        try {");
        out.println("            return com.thy.fss.common.inmemory.specification.SpecificationServices.getService(elementType);");
        out.println("        } catch (IllegalArgumentException e) {");
        out.println("            throw new IllegalStateException(");
        out.println("                \"No SpecificationService found for element type: \" + elementType.getName() + \". \" +");
        out.println("                \"Make sure the element type is annotated with @MetaModel and the annotation processor has run.\",");
        out.println("                e");
        out.println("            );");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();
        
        out.println(EXP_3);
        out.println("     * Validates a collection element against a model filter using element type service.");
        out.println("     * @param element the collection element to validate");
        out.println("     * @param elementFilter the filter for the element");
        out.println("     * @param elementService the specification service for the element type");
        out.println("     * @return true if element matches the filter");
        out.println(EXP_2);
        out.println("    @SuppressWarnings(\"unchecked\")");
        out.println("    protected <E> boolean validateCollectionElement(E element, Object elementFilter, ");
        out.println("            com.thy.fss.common.inmemory.specification.SpecificationService<E> elementService) {");
        out.println("        if (elementFilter == null) {");
        out.println("            return true;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Delegate to element type service for validation (handles null element correctly)");
        out.println("        return elementService.validateFilter(element, elementFilter);");
        out.println("    }");
        out.println();
    }

    /**
     * Gets the parameter type for a validation method.
     */
    private String getParameterType(FieldInfo field, Operator operator) {
        return switch (operator) {
            case IN, NOT_IN -> {
                if (field.validationPrefix.equals("Collection")) {
                    yield "java.util.Collection<" + field.collectionElementType + ">";
                } else {
                    // For primitive types, use wrapper types in collections
                    String collectionType = getWrapperType(field.fieldTypeName);
                    yield "java.util.Collection<" + collectionType + ">";
                }
            }
            case EQUALS -> {
                if (field.validationPrefix.equals("Collection")) {
                    yield "java.util.Collection<" + field.collectionElementType + ">"; // Use Collection<ElementType> for compatibility
                } else {
                    yield field.fieldTypeName;
                }
            }
            case CONTAINS -> {
                if (field.validationPrefix.equals("Collection")) {
                    yield field.collectionElementType; // For collection contains, use element type
                } else {
                    yield field.fieldTypeName;
                }
            }
            case COLLECTION_CONTAINS -> {
                // COLLECTION_CONTAINS uses element type
                yield field.collectionElementType != null ? field.collectionElementType : "Object";
            }
            case COLLECTION_ANY, COLLECTION_ALL, COLLECTION_NONE -> {
                // These operators use Filter<ElementType>
                String elementType = field.collectionElementType != null ? field.collectionElementType : "Object";
                yield "com.thy.fss.common.inmemory.filter.Filter<" + elementType + ">";
            }
            case IS_EMPTY, IS_NOT_EMPTY, IS_NULL, IS_NOT_NULL, IS_BLANK, IS_NOT_BLANK -> "Boolean";
            case LAST, NEXT -> "com.thy.fss.common.inmemory.filter.TemporalPreset";
            default -> field.fieldTypeName;
        };
    }

    /**
     * Generates the main validateSpecification method.
     */
    private void generateValidateSpecificationMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ========== Main Validation Methods ==========");
        out.println();
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public boolean validateSpecification(" + simpleClassName + " entity, MetaAttribute<" + simpleClassName + ", ?> attribute, Operator operator, Object value) {");
        out.println(ENTITY_NULL_CHECK);
        out.println(RETURN_FALSE);
        out.println(CLOSING_BRACE);
        out.println();

        // Generate switch statement for each field using attribute reference comparison
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo field = fields.get(i);
            String condition = i == 0 ? "if" : "} else if";

            out.println("        " + condition + " (attribute == " + simpleClassName + "_." + field.fieldName + ") {");
            generateFieldOperatorSwitch(out, field, simpleClassName);
        }

        if (!fields.isEmpty()) {
            out.println(CLOSING_BRACE);
        }

        out.println();
        out.println("        throw new UnsupportedOperationException(\"Unsupported attribute: \" + attribute);");
        out.println("    }");
        out.println();
    }

    /**
     * Generates the operator switch for a specific field in
     * validateSpecification.
     */
    private void generateFieldOperatorSwitch(PrintWriter out, FieldInfo field, String simpleClassName) {
        Set<Operator> supportedOperators = FIELD_TYPE_OPERATORS.get(field.validationPrefix);
        if (supportedOperators == null) {
            supportedOperators = Set.of(Operator.EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL);
        }

        out.println("            return switch (operator) {");

        for (Operator operator : supportedOperators) {
            String methodName = "validate" + capitalize(field.fieldName) + getOperatorMethodSuffix(operator);
            String castType = getParameterType(field, operator);

            out.println("                case " + operator + " -> " + methodName + "(entity, (" + castType + ") value);");
        }

        out.println("                default -> throw new UnsupportedOperationException(\"Unsupported operator for " + field.fieldName + ": \" + operator);");
        out.println("            };");
    }

    /**
     * Generates the validateFilter method.
     */
    private void generateValidateFilterMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        String filterClassName = simpleClassName + "Filter";

        out.println(OVERRIDE_ANNOTATION);
        out.println("    public boolean validateFilter(" + simpleClassName + " entity, Object filter) {");
        out.println("        if (!(filter instanceof " + filterClassName + " " + simpleClassName.toLowerCase() + "Filter)) {");
        out.println("            throw new IllegalArgumentException(\"Filter must be of type " + filterClassName + "\");");
        out.println(CLOSING_BRACE);
        out.println("        if (entity == null) {");
        out.println("            return validateFilterForNullEntity(" + simpleClassName.toLowerCase() + "Filter);");
        out.println(CLOSING_BRACE);
        out.println();

        // Generate validation for each field's filter
        for (FieldInfo field : fields) {
            generateFieldFilterValidation(out, field, simpleClassName.toLowerCase() + "Filter");
        }

        out.println();
        out.println("        return true; // All filters passed");
        out.println("    }");
        out.println();

        // Generate the validateFilterForNullEntity helper method
        generateValidateFilterForNullEntityMethod(out, fields, simpleClassName);
    }

    /**
     * Generates the validateFilterForNullEntity helper method.
     * When entity is null, all fields are effectively null.
     * Only isNull=true filters pass; isNotNull=true and value-based filters fail.
     * Nested model filters are delegated to their service with null entity.
     */
    private void generateValidateFilterForNullEntityMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        String filterClassName = simpleClassName + "Filter";
        String filterVariableName = simpleClassName.toLowerCase() + "Filter";

        out.println("    /**");
        out.println("     * Validates filter criteria when entity is null.");
        out.println("     * When entity is null, all fields are effectively null.");
        out.println("     */");
        out.println("    private boolean validateFilterForNullEntity(" + filterClassName + " " + filterVariableName + ") {");

        for (FieldInfo field : fields) {
            generateNullEntityFieldFilterValidation(out, field, filterVariableName);
        }

        out.println("        return true;");
        out.println("    }");
        out.println();
    }

    /**
     * Generates filter validation for a specific field when the entity is null.
     */
    private void generateNullEntityFieldFilterValidation(PrintWriter out, FieldInfo field, String filterVariableName) {
        String filterGetterName = "get" + capitalize(field.fieldName);
        String filterFieldAccess = filterVariableName + "." + filterGetterName + "()";

        if (isModelType(field.fieldType)) {
            // For model type fields: delegate to nested service with null entity
            String nestedFieldTypeName = getFieldTypeName(field.fieldType);
            String simpleModelName = nestedFieldTypeName.substring(nestedFieldTypeName.lastIndexOf('.') + 1);
            String nestedServiceStaticRef = simpleModelName + "SpecificationService.INSTANCE";

            out.println("        if (" + filterFieldAccess + " != null) {");
            out.println("            if (!" + nestedServiceStaticRef + ".validateFilter(null, " + filterFieldAccess + ")) return false;");
            out.println(CLOSING_BRACE);
            out.println();
            return;
        }

        out.println("        if (" + filterFieldAccess + " != null) {");

        if (field.isCollection) {
            // For collection fields when entity is null: collection is null
            // isNull=true passes, isNotNull=true fails, isEmpty/isNotEmpty/collectionOps fail
            generateNullEntityCollectionFilterValidation(out, field, filterFieldAccess);
        } else {
            // For simple fields when entity is null: field is null
            // Only isNull/isNotNull need evaluation; all other filters fail
            generateNullEntitySimpleFieldFilterValidation(out, field, filterFieldAccess);
        }

        out.println(CLOSING_BRACE);
        out.println();
    }

    /**
     * Generates validation for simple field filters when entity is null (field is effectively null).
     */
    private void generateNullEntitySimpleFieldFilterValidation(PrintWriter out, FieldInfo field, String filterAccess) {
        Set<Operator> supportedOperators = FIELD_TYPE_OPERATORS.get(field.validationPrefix);
        if (supportedOperators == null) {
            supportedOperators = Set.of(Operator.EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL);
        }

        for (Operator operator : supportedOperators) {
            String validationMethodSuffix = getOperatorMethodSuffix(operator);
            String filterGetterMethod = "get" + validationMethodSuffix;

            if (operator == Operator.IS_NULL) {
                // isNull: when field is null, isNull=true should pass, isNull=false should fail
                out.println("            if (" + filterAccess + "." + filterGetterMethod + "() != null) {");
                out.println("                Boolean isNullValue = " + filterAccess + "." + filterGetterMethod + "();");
                out.println("                if (!(isNullValue == null || isNullValue.booleanValue())) return false;");
                out.println("            }");
            } else if (operator == Operator.IS_NOT_NULL) {
                // isNotNull: when field is null, isNotNull=true should fail, isNotNull=false should pass
                out.println("            if (" + filterAccess + "." + filterGetterMethod + "() != null) {");
                out.println("                Boolean isNotNullValue = " + filterAccess + "." + filterGetterMethod + "();");
                out.println("                if (isNotNullValue == null || isNotNullValue.booleanValue()) return false;");
                out.println("            }");
            } else {
                // All other operators: value-based operations fail when field is null
                out.println("            if (" + filterAccess + "." + filterGetterMethod + "() != null) return false;");
            }
        }
    }

    /**
     * Generates validation for collection field filters when entity is null (collection is effectively null).
     */
    private void generateNullEntityCollectionFilterValidation(PrintWriter out, FieldInfo field, String filterAccess) {
        // When entity is null, the collection field is null
        // isNull=true passes, isNotNull=true fails
        // isEmpty, isNotEmpty, collectionAny, collectionAll, collectionNone: fail (no collection exists)
        out.println("            // Collection is null when entity is null");
        out.println("            if (" + filterAccess + ".getIsNull() != null) {");
        out.println("                Boolean isNullValue = " + filterAccess + ".getIsNull();");
        out.println("                if (!(isNullValue == null || isNullValue.booleanValue())) return false;");
        out.println("            }");
        out.println("            if (" + filterAccess + ".getIsNotNull() != null) {");
        out.println("                Boolean isNotNullValue = " + filterAccess + ".getIsNotNull();");
        out.println("                if (isNotNullValue == null || isNotNullValue.booleanValue()) return false;");
        out.println("            }");
        out.println("            if (" + filterAccess + ".getIsEmpty() != null) {");
        out.println("                Boolean isEmptyValue = " + filterAccess + ".getIsEmpty();");
        out.println("                // null collection: isEmpty=true should fail (null != empty), isEmpty=false should pass");
        out.println("                if (isEmptyValue == null || isEmptyValue.booleanValue()) return false;");
        out.println("            }");
        out.println("            if (" + filterAccess + ".getIsNotEmpty() != null) {");
        out.println("                Boolean isNotEmptyValue = " + filterAccess + ".getIsNotEmpty();");
        out.println("                // null collection: isNotEmpty=true should fail, isNotEmpty=false should pass");
        out.println("                if (isNotEmptyValue == null || isNotEmptyValue.booleanValue()) return false;");
        out.println("            }");

        // Collection operations (any/all/none) with null collection
        boolean isModelElementType = field.collectionElementType != null && isModelElementType(field.collectionElementType);
        if (isModelElementType) {
            out.println("            if (" + filterAccess + ".getCollectionAny() != null) return false;");
            out.println("            if (" + filterAccess + ".getCollectionAll() != null) return false;");
            out.println("            // collectionNone with null collection: no elements violate -> true (do not fail)");
        } else {
            out.println("            if (" + filterAccess + ".getCollectionAny() != null) return false;");
            out.println("            if (" + filterAccess + ".getCollectionAll() != null) return false;");
            // collectionNone is correct: null collection has no elements, so none match
        }
    }

    /**
     * Generates the getEntityClass method.
     */
    private void generateGetEntityClassMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public Class<" + simpleClassName + "> getEntityClass() {");
        out.println("        return " + simpleClassName + ".class;");
        out.println("    }");
        out.println();

        // Generate createInstance method
        generateCreateInstanceMethod(out, simpleClassName);
    }

    /**
     * Generates the setFieldValue method for efficient field setting using
     * MetaAttribute.
     */
    private void generateSetFieldValueMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        // Generate individual setter methods for each field
        for (FieldInfo field : fields) {
            generateFieldSetterMethod(out, field, simpleClassName);
        }
        // Note: The main setFieldValue method is generated in generateFieldValueExtractionMethods
    }

    /**
     * Generates individual field setter method with type conversion.
     */
    private void generateFieldSetterMethod(PrintWriter out, FieldInfo field, String simpleClassName) {
        String methodName = "set" + capitalize(field.fieldName) + "Value";
        String setterName = "set" + capitalize(field.fieldName);

        out.println(EXP_3);
        out.println("     * Sets " + field.fieldName + " field value with type conversion support.");
        out.println(EXP_2);
        out.println("    private void " + methodName + "(" + simpleClassName + " entity, Object value) {");

        out.println("        if (value == null) {");
        // For primitive types, we cannot set null, so return false or use default value
        if (isPrimitiveType(field.fieldTypeName)) {
            //set 0 for primitive values
            if (field.fieldTypeName.equals("int") || field.fieldTypeName.equals("long") || field.fieldTypeName.equals("double") || field.fieldTypeName.equals("float")) {
                out.println("            entity." + setterName + "(0);");
            } else if (field.fieldTypeName.equals("boolean")) {
                out.println("            entity." + setterName + "(false);");
            }
        } else {
            out.println("            entity." + setterName + "(null);");
        }
        out.println("            return;");
        out.println(CLOSING_BRACE);
        out.println();

        // Generate type conversion logic based on field type
        generateTypeConversionLogic(out, field, setterName);

        out.println("    }");
        out.println();
    }

    /**
     * Generates type conversion logic for field setting.
     */
    private void generateTypeConversionLogic(PrintWriter out, FieldInfo field, String setterName) {
        String fieldType = field.fieldTypeName;

        if (fieldType.equals("java.lang.String")) {
            out.println("        entity." + setterName + "(value.toString());");
            out.println("        return;");
        } else if (fieldType.equals(JAVA_LANG_LONG) || fieldType.equals("long")) {
            out.println("        if (value instanceof Long) {");
            out.println("            entity." + setterName + "((Long) value);");
            out.println("        } else if (value instanceof Integer) {");
            out.println("            entity." + setterName + "(((Integer) value).longValue());");
            out.println("        } else if (value instanceof Number) {");
            out.println("            entity." + setterName + "(((Number) value).longValue());");
            out.println("        } else {");
            out.println("            entity." + setterName + "(Long.valueOf(value.toString()));");
            out.println(CLOSING_BRACE);
            out.println("        return;");
        } else if (fieldType.equals(JAVA_LANG_INTEGER) || fieldType.equals("int")) {
            out.println("        if (value instanceof Integer) {");
            out.println("            entity." + setterName + "((Integer) value);");
            out.println("        } else if (value instanceof Number) {");
            out.println("            entity." + setterName + "(((Number) value).intValue());");
            out.println("        } else {");
            out.println("            entity." + setterName + "(Integer.valueOf(value.toString()));");
            out.println(CLOSING_BRACE);
            out.println("        return;");
        } else if (fieldType.equals(JAVA_LANG_DOUBLE) || fieldType.equals("double")) {
            out.println("        if (value instanceof Double) {");
            out.println("            entity." + setterName + "((Double) value);");
            out.println("        } else if (value instanceof Float) {");
            out.println("            entity." + setterName + "(((Float) value).doubleValue());");
            out.println("        } else if (value instanceof Number) {");
            out.println("            entity." + setterName + "(((Number) value).doubleValue());");
            out.println("        } else {");
            out.println("            entity." + setterName + "(Double.valueOf(value.toString()));");
            out.println(CLOSING_BRACE);
            out.println("        return;");
        } else if (fieldType.equals(JAVA_LANG_BOOLEAN) || fieldType.equals("boolean")) {
            out.println("        if (value instanceof Boolean) {");
            out.println("            entity." + setterName + "((Boolean) value);");
            out.println("        } else {");
            out.println("            entity." + setterName + "(Boolean.valueOf(value.toString()));");
            out.println(CLOSING_BRACE);
            out.println("        return;");
        } else {
            // For other types, try direct casting
            out.println("        entity." + setterName + "((" + fieldType + ") value);");
            out.println("        return;");
        }
    }

    /**
     * Generates the createInstance method for entity instantiation.
     */
    private void generateCreateInstanceMethod(PrintWriter out, String simpleClassName) {
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public " + simpleClassName + " createInstance() throws Exception {");
        out.println("        try {");
        out.println("            return new " + simpleClassName + "();");
        out.println("        } catch (Exception e) {");
        out.println("            throw new Exception(\"Failed to create instance of " + simpleClassName + ": \" + e.getMessage(), e);");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();
    }

    /**
     * Generates filter validation for a specific field.
     */
    private void generateFieldFilterValidation(PrintWriter out, FieldInfo field, String filterVariableName) {

        if (isModelType(field.fieldType)) {
            // Use static INSTANCE variable instead of creating new instance
            String nestedFieldTypeName = getFieldTypeName(field.fieldType);
            String simpleModelName = nestedFieldTypeName.substring(nestedFieldTypeName.lastIndexOf('.') + 1);
            String nestedServiceStaticRef = simpleModelName + "SpecificationService.INSTANCE";
            String filterGetterName = "get" + capitalize(field.fieldName);
            String filterFieldAccess = filterVariableName + "." + filterGetterName + "()";

            out.println("        if (" + filterFieldAccess + " != null) {");
            out.println("            if (!" + nestedServiceStaticRef + ".validateFilter(entity." + filterGetterName + "(), " + filterFieldAccess + ")) return false;");
            out.println(CLOSING_BRACE);
            out.println();
            return;
        }

        String filterGetterName = "get" + capitalize(field.fieldName);
        String filterFieldAccess = filterVariableName + "." + filterGetterName + "()";

        out.println("        // Validate " + field.fieldName + " filter");
        out.println("        if (" + filterFieldAccess + " != null) {");

        // Generate validation for each filter property based on field type
        generateFilterPropertyValidations(out, field, filterFieldAccess);

        out.println(CLOSING_BRACE);
        out.println();
    }

    /**
     * Generates validation for filter properties.
     */
    private void generateFilterPropertyValidations(PrintWriter out, FieldInfo field, String filterAccess) {
        String fieldPrefix = capitalize(field.fieldName);

        // Special handling for collection fields with CollectionFilter
        if (field.isCollection) {
            generateCollectionFilterValidation(out, field, filterAccess, fieldPrefix);
            return;
        }

        // Get supported operators for this field type
        Set<Operator> supportedOperators = FIELD_TYPE_OPERATORS.get(field.validationPrefix);
        if (supportedOperators == null) {
            // For model and collection types, generate basic operators
            supportedOperators = Set.of(Operator.EQUALS, Operator.IS_NULL, Operator.IS_NOT_NULL);
        }

        // Generate validation for each supported operator
        for (Operator operator : supportedOperators) {
            String validationMethodSuffix = getOperatorMethodSuffix(operator);
            String filterGetterMethod = "get" + validationMethodSuffix;

            out.println("            if (" + filterAccess + "." + filterGetterMethod + "() != null && !validate"
                    + fieldPrefix + validationMethodSuffix + "(entity, " + filterAccess + "." + filterGetterMethod + "())) return false;");
        }
    }

    /**
     * Generates validation for CollectionFilter properties.
     */
    private void generateCollectionFilterValidation(PrintWriter out, FieldInfo field, String filterAccess, String fieldPrefix) {
        out.println("            // Validate CollectionFilter properties");

        // Check if this is a model type collection (needs element type service delegation)
        boolean isModelElementType = field.collectionElementType != null && isModelElementType(field.collectionElementType);
        
        if (isModelElementType) {
            // Generate model type collection validation with element type service delegation
            generateModelTypeCollectionValidation(out, field, filterAccess, fieldPrefix);
        } else {
            // Generate basic type collection validation (existing logic)
            generateBasicTypeCollectionValidation(out, field, filterAccess, fieldPrefix);
        }
    }
    
    /**
     * Checks if an element type (String) is a model type.
     * Model types are custom classes that are not basic types, enums, or standard Java types.
     */
    private boolean isModelElementType(String elementTypeName) {
        if (elementTypeName == null || elementTypeName.isEmpty()) {
            return false;
        }
        
        // Check if it's a basic type
        if (FIELD_TYPE_PREFIXES.containsKey(elementTypeName)) {
            return false;
        }
        
        // Check if it's a standard Java type
        if (elementTypeName.startsWith("java.") || elementTypeName.startsWith("javax.")) {
            return false;
        }
        
        // Check if it's an enum
        javax.lang.model.util.Elements elementUtils = processingEnv.getElementUtils();
        javax.lang.model.element.TypeElement typeElement = elementUtils.getTypeElement(elementTypeName);
        if (typeElement != null && typeElement.getKind() == javax.lang.model.element.ElementKind.ENUM) {
            return false;
        }
        
        // If it's not a basic type, standard Java type, or enum, it's a model type
        return true;
    }
    
    /**
     * Generates validation for model type collection filters with element type service delegation.
     */
    private void generateModelTypeCollectionValidation(PrintWriter out, FieldInfo field, String filterAccess, String fieldPrefix) {
        String elementTypeName = field.collectionElementType;
        String simpleElementName = elementTypeName.substring(elementTypeName.lastIndexOf('.') + 1);
        String elementServiceClass = simpleElementName + "SpecificationService";
        
        out.println("            // Model type collection - delegate to element type service");
        
        // Validate any operator
        out.println("            if (" + filterAccess + ".getCollectionAny() != null) {");
        out.println("                com.thy.fss.common.inmemory.filter.FilterBase<" + simpleElementName + "> elementFilter = " + filterAccess + ".getCollectionAny();");
        out.println("                if (elementFilter instanceof " + simpleElementName + "Filter) {");
        out.println("                    " + simpleElementName + "Filter modelFilter = (" + simpleElementName + "Filter) elementFilter;");
        out.println("                    if (!validate" + fieldPrefix + "CollectionAnyWithModelFilter(entity, modelFilter, " + elementServiceClass + ".INSTANCE)) return false;");
        out.println("                } else {");
        out.println("                    if (!validate" + fieldPrefix + "CollectionAny(entity, (com.thy.fss.common.inmemory.filter.Filter<" + simpleElementName + ">) elementFilter)) return false;");
        out.println("                }");
        out.println(EXP_1);
        
        // Validate all operator
        out.println("            if (" + filterAccess + ".getCollectionAll() != null) {");
        out.println("                com.thy.fss.common.inmemory.filter.FilterBase<" + simpleElementName + "> elementFilter = " + filterAccess + ".getCollectionAll();");
        out.println("                if (elementFilter instanceof " + simpleElementName + "Filter) {");
        out.println("                    " + simpleElementName + "Filter modelFilter = (" + simpleElementName + "Filter) elementFilter;");
        out.println("                    if (!validate" + fieldPrefix + "CollectionAllWithModelFilter(entity, modelFilter, " + elementServiceClass + ".INSTANCE)) return false;");
        out.println("                } else {");
        out.println("                    if (!validate" + fieldPrefix + "CollectionAll(entity, (com.thy.fss.common.inmemory.filter.Filter<" + simpleElementName + ">) elementFilter)) return false;");
        out.println("                }");
        out.println(EXP_1);
        
        // Validate none operator
        out.println("            if (" + filterAccess + ".getCollectionNone() != null) {");
        out.println("                com.thy.fss.common.inmemory.filter.FilterBase<" + simpleElementName + "> elementFilter = " + filterAccess + ".getCollectionNone();");
        out.println("                if (elementFilter instanceof " + simpleElementName + "Filter) {");
        out.println("                    " + simpleElementName + "Filter modelFilter = (" + simpleElementName + "Filter) elementFilter;");
        out.println("                    if (!validate" + fieldPrefix + "CollectionNoneWithModelFilter(entity, modelFilter, " + elementServiceClass + ".INSTANCE)) return false;");
        out.println("                } else {");
        out.println("                    if (!validate" + fieldPrefix + "CollectionNone(entity, (com.thy.fss.common.inmemory.filter.Filter<" + simpleElementName + ">) elementFilter)) return false;");
        out.println("                }");
        out.println(EXP_1);
        
        // Also check inherited Filter properties
        out.println("            if (" + filterAccess + ".getIsEmpty() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsEmpty(entity, " + filterAccess + ".getIsEmpty())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNotEmpty() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNotEmpty(entity, " + filterAccess + ".getIsNotEmpty())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNull() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNull(entity, " + filterAccess + ".getIsNull())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNotNull() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNotNull(entity, " + filterAccess + ".getIsNotNull())) return false;");
        out.println(EXP_1);
    }
    
    /**
     * Generates validation for basic type collection filters (existing logic).
     */
    private void generateBasicTypeCollectionValidation(PrintWriter out, FieldInfo field, String filterAccess, String fieldPrefix) {
        // Check each collection operation
        out.println("            if (" + filterAccess + ".getCollectionContains() != null) {");
        out.println("                if (!validate" + fieldPrefix + "CollectionContains(entity, " + filterAccess + ".getCollectionContains())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getCollectionAny() != null) {");
        out.println("                if (!validate" + fieldPrefix + "CollectionAny(entity, (com.thy.fss.common.inmemory.filter.Filter<" + getSimpleElementType(field) + ">) " + filterAccess + ".getCollectionAny())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getCollectionAll() != null) {");
        out.println("                if (!validate" + fieldPrefix + "CollectionAll(entity, (com.thy.fss.common.inmemory.filter.Filter<" + getSimpleElementType(field) + ">) " + filterAccess + ".getCollectionAll())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getCollectionNone() != null) {");
        out.println("                if (!validate" + fieldPrefix + "CollectionNone(entity, (com.thy.fss.common.inmemory.filter.Filter<" + getSimpleElementType(field) + ">) " + filterAccess + ".getCollectionNone())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsEmpty() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsEmpty(entity, " + filterAccess + ".getIsEmpty())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNotEmpty() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNotEmpty(entity, " + filterAccess + ".getIsNotEmpty())) return false;");
        out.println(EXP_1);

        // Also check inherited Filter properties
        out.println("            if (" + filterAccess + ".getEquals() != null) {");
        out.println("                if (!validate" + fieldPrefix + "Equals(entity, " + filterAccess + ".getEquals())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNull() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNull(entity, " + filterAccess + ".getIsNull())) return false;");
        out.println(EXP_1);

        out.println("            if (" + filterAccess + ".getIsNotNull() != null) {");
        out.println("                if (!validate" + fieldPrefix + "IsNotNull(entity, " + filterAccess + ".getIsNotNull())) return false;");
        out.println(EXP_1);
    }
    
    /**
     * Gets the simple element type name for casting.
     */
    private String getSimpleElementType(FieldInfo field) {
        if (field.collectionElementType == null) {
            return "Object";
        }
        String elementType = field.collectionElementType;
        int lastDot = elementType.lastIndexOf('.');
        return lastDot > 0 ? elementType.substring(lastDot + 1) : elementType;
    }

    /**
     * Generates field value extraction methods.
     */
    private void generateFieldValueExtractionMethods(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ==================== GENERATED FIELD VALUE EXTRACTION ====================");
        out.println();

        // Generate string-based getFieldValue method
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public Object getFieldValue(" + simpleClassName + " entity, String fieldName) {");
        out.println("        if (entity == null || fieldName == null) {");
        out.println("            return null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Handle nested field paths (e.g., \"irropsFlight.flightDate\")");
        out.println("        if (fieldName.contains(\".\")) {");
        out.println("            String[] parts = fieldName.split(\"\\\\.\", 2);");
        out.println("            String firstField = parts[0];");
        out.println("            String remainingPath = parts[1];");
        out.println("            ");
        out.println("            // Delegate to nested field's specification service based on first field");
        out.println("            switch (firstField) {");

        // Generate switch cases for nested model fields
        for (FieldInfo field : fields) {
            if (field.attributeType == AttributeType.MODEL) {
                String nestedServiceClass = field.fieldTypeName + "SpecificationService";
                out.println("                case \"" + field.fieldName + "\" -> {");
                out.println("                    " + field.fieldTypeName + " nestedObject = entity." + field.getterMethodName + "();");
                out.println("                    if (nestedObject == null) {");
                out.println("                        return null;");
                out.println("                    }");
                out.println("                    return " + nestedServiceClass + ".INSTANCE.getFieldValue(nestedObject, remainingPath);");
                out.println(EXP_4);
            }
        }

        out.println("                default -> {");
        out.println("                    throw new UnsupportedOperationException(");
        out.println("                        \"Field '\" + firstField + \"' is not a nested model or does not exist\");");
        out.println(EXP_4);
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Efficient field access using switch statement");
        out.println("        switch (fieldName) {");

        for (FieldInfo field : fields) {
            out.println("            case \"" + field.fieldName + "\" -> {");
            out.println("                return entity." + field.getterMethodName + "();");
            out.println(EXP_1);
        }

        out.println("            default -> {");
        out.println("                throw new UnsupportedOperationException(\"Unsupported field name: \" + fieldName);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();

        // Generate meta attribute-based getFieldValue method
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public Object getFieldValue(" + simpleClassName + " entity, MetaAttribute<?, ?> metaAttribute) {");
        out.println("        if (entity == null || metaAttribute == null) {");
        out.println("            return null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Type-safe field access using meta attributes");

        boolean first = true;
        for (FieldInfo field : fields) {
            String condition = first ? "if" : "} else if";
            out.println("        " + condition + " (metaAttribute == " + simpleClassName + "_." + field.fieldName + ") {");
            out.println("            return entity." + field.getterMethodName + "();");
            first = false;
        }

        out.println("        } else {");
        out.println("            throw new UnsupportedOperationException(\"Unsupported meta attribute: \" + metaAttribute);");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();

        // Generate string-based setFieldValue method
        // Generate meta attribute-based setFieldValue method
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public void setFieldValue(" + simpleClassName + " entity, MetaAttribute<?, ?> metaAttribute, Object value) {");
        out.println("        if (entity == null || metaAttribute == null) {");
        out.println("            return;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Type-safe field access using meta attributes");

        boolean firstSet = true;
        for (FieldInfo field : fields) {
            String condition = firstSet ? "if" : "} else if";
            out.println("        " + condition + " (metaAttribute == " + simpleClassName + "_." + field.fieldName + ") {");

            out.println("            set" + capitalize(field.fieldName) + "Value(entity, value);");

            firstSet = false;
        }

        out.println("        } else {");
        out.println("            throw new UnsupportedOperationException(\"Unsupported meta attribute: \" + metaAttribute);");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();

        // Generate findMetaAttribute method
        out.println(EXP_3);
        out.println("     * Finds a MetaAttribute by field name.");
        out.println("     * This method provides a way to look up meta attributes dynamically.");
        out.println(EXP_2);
        out.println("    public MetaAttribute<" + simpleClassName + ", ?> findMetaAttribute(String fieldName) {");
        out.println("        if (fieldName == null) {");
        out.println("            return null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Find meta attribute by field name");
        out.println("        switch (fieldName) {");

        for (FieldInfo field : fields) {
            out.println("            case \"" + field.fieldName + "\":");
            out.println("                return " + simpleClassName + "_." + field.fieldName + ";");
        }

        out.println("            default:");
        out.println("                return null;");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();

    }

    /**
     * Generates sorting support methods.
     */
    private void generateSortingMethods(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ==================== SORTING SUPPORT METHODS ====================");
        out.println();

        // Generate field-specific comparators
        out.println(EXP_3);
        out.println("     * Creates a comparator for the specified field path.");
        out.println("     * Supports nested field paths using dot notation.");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public java.util.Comparator<" + simpleClassName + "> createComparator(String fieldPath, boolean ascending) {");
        out.println("        java.util.Comparator<" + simpleClassName + "> comparator = (entity1, entity2) -> {");
        out.println("            Object value1 = getFieldValue(entity1, fieldPath);");
        out.println("            Object value2 = getFieldValue(entity2, fieldPath);");
        out.println();
        out.println("            return compareValues(value1, value2);");
        out.println("        };");
        out.println();
        out.println("        return ascending ? comparator : comparator.reversed();");
        out.println("    }");
        out.println();

        // Generate meta attribute-based comparator
        out.println(EXP_3);
        out.println("     * Creates a comparator for the specified meta attribute.");
        out.println("     * Type-safe alternative to string-based field path.");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public java.util.Comparator<" + simpleClassName + "> createComparator(MetaAttribute<?, ?> metaAttribute, boolean ascending) {");
        out.println("        java.util.Comparator<" + simpleClassName + "> comparator = (entity1, entity2) -> {");
        out.println("            Object value1 = getFieldValue(entity1, metaAttribute);");
        out.println("            Object value2 = getFieldValue(entity2, metaAttribute);");
        out.println();
        out.println("            return compareValues(value1, value2);");
        out.println("        };");
        out.println();
        out.println("        return ascending ? comparator : comparator.reversed();");
        out.println("    }");
        out.println();

        // Generate value comparison method
        out.println(EXP_3);
        out.println("     * Compares two values with null-safe handling.");
        out.println(EXP_2);
        out.println(SUPPRESS_WARNINGS_UNCHECKED);
        out.println("    public int compareValues(Object value1, Object value2) {");
        out.println("        if (value1 == null && value2 == null) return 0;");
        out.println("        if (value1 == null) return -1;");
        out.println("        if (value2 == null) return 1;");
        out.println();
        out.println("        if (value1 instanceof Comparable && value2 instanceof Comparable) {");
        out.println("            try {");
        out.println("                return ((Comparable<Object>) value1).compareTo(value2);");
        out.println("            } catch (ClassCastException e) {");
        out.println("                // Fall back to string comparison if types are incompatible");
        out.println("                return value1.toString().compareTo(value2.toString());");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Fall back to string comparison for non-comparable types");
        out.println("        return value1.toString().compareTo(value2.toString());");
        out.println("    }");
        out.println();

        // Generate multi-field sorting support
        out.println(EXP_3);
        out.println("     * Creates a multi-field comparator that sorts by multiple fields in order.");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public java.util.Comparator<" + simpleClassName + "> createMultiFieldComparator(");
        out.println("            java.util.List<String> fieldPaths, java.util.List<Boolean> ascendingFlags) {");
        out.println("        if (fieldPaths.size() != ascendingFlags.size()) {");
        out.println("            throw new IllegalArgumentException(\"Field paths and ascending flags must have the same size\");");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        java.util.Comparator<" + simpleClassName + "> comparator = null;");
        out.println();
        out.println("        for (int i = 0; i < fieldPaths.size(); i++) {");
        out.println("            java.util.Comparator<" + simpleClassName + "> fieldComparator = ");
        out.println("                createComparator(fieldPaths.get(i), ascendingFlags.get(i));");
        out.println();
        out.println("            if (comparator == null) {");
        out.println("                comparator = fieldComparator;");
        out.println("            } else {");
        out.println("                comparator = comparator.thenComparing(fieldComparator);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        return comparator != null ? comparator : (e1, e2) -> 0;");
        out.println("    }");
        out.println();

        // Generate meta attribute-based multi-field sorting
        out.println(EXP_3);
        out.println("     * Creates a multi-field comparator using meta attributes for type safety.");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println("    public java.util.Comparator<" + simpleClassName + "> createMultiFieldComparatorWithMetaAttributes(");
        out.println("            java.util.List<MetaAttribute<?, ?>> metaAttributes, java.util.List<Boolean> ascendingFlags) {");
        out.println("        if (metaAttributes.size() != ascendingFlags.size()) {");
        out.println("            throw new IllegalArgumentException(\"Meta attributes and ascending flags must have the same size\");");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        java.util.Comparator<" + simpleClassName + "> comparator = null;");
        out.println();
        out.println("        for (int i = 0; i < metaAttributes.size(); i++) {");
        out.println("            java.util.Comparator<" + simpleClassName + "> fieldComparator = ");
        out.println("                createComparator(metaAttributes.get(i), ascendingFlags.get(i));");
        out.println();
        out.println("            if (comparator == null) {");
        out.println("                comparator = fieldComparator;");
        out.println("            } else {");
        out.println("                comparator = comparator.thenComparing(fieldComparator);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        return comparator != null ? comparator : (e1, e2) -> 0;");
        out.println("    }");
        out.println();
    }

    /**
     * Generates collection operation methods with specification support.
     * Implements the three new methods required for collection operations: 1.
     * extractFromCollection with specification 2. getValueByPathWithCollections
     * 3. setValueByPathWithCollections
     */
    private void generateCollectionOperationMethods(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ==================== COLLECTION OPERATIONS WITH SPECIFICATIONS ====================");
        out.println();

        // Generate extractFromCollection with specification
        generateExtractFromCollectionWithSpecification(out, simpleClassName);

        // Generate getValueByPathWithCollections
        generateGetValueByPathWithCollections(out, simpleClassName);

        // Generate setValueByPathWithCollections
        generateSetValueByPathWithCollections(out, simpleClassName);
    }

    /**
     * Generates the extractFromCollection method with specification filtering.
     * This method filters the collection using validateSpecification() for each
     * element, then applies the selector using the existing
     * extractFromCollection() method.
     */
    private void generateExtractFromCollectionWithSpecification(PrintWriter out, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Extracts value(s) from a collection with specification filtering.");
        out.println("     * Filters the collection using the specification, then applies the selector.");
        out.println("     * Generated at build time for type-safe collection operations.");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println(SUPPRESS_WARNINGS_UNCHECKED);
        out.println("    public Object extractFromCollection(");
        out.println("            java.util.Collection<" + simpleClassName + "> collection,");
        out.println("            com.thy.fss.common.inmemory.engine.mapping.CollectionSelector selector,");
        out.println("            com.thy.fss.common.inmemory.specification.Specification<" + simpleClassName + "> specification");
        out.println("    ) {");
        out.println("        if (collection == null || collection.isEmpty()) {");
        out.println("            return selector == com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.ALL ? new java.util.ArrayList<>() : null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // If no specification, use the base extractFromCollection method");
        out.println("        if (specification == null) {");
        out.println("            return extractFromCollection(collection, selector);");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Filter collection using specification");
        out.println("        java.util.List<" + simpleClassName + "> filteredCollection = new java.util.ArrayList<>();");
        out.println("        for (" + simpleClassName + " element : collection) {");
        out.println("            // Handle null elements safely");
        out.println("            if (element != null && specification.test(element)) {");
        out.println("                filteredCollection.add(element);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Apply selector to filtered collection");
        out.println("        return extractFromCollection(filteredCollection, selector);");
        out.println("    }");
        out.println();
    }

    /**
     * Generates the getValueByPathWithCollections method. This method navigates
     * through the path, handling both regular fields and collection operations.
     */
    private void generateGetValueByPathWithCollections(PrintWriter out, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Gets a value from an entity using a path that may contain collection operations.");
        out.println("     * Navigates through the path, handling both regular fields and collection operations.");
        out.println("     * Generated at build time with direct method calls (no reflection).");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println(SUPPRESS_WARNINGS_UNCHECKED);
        out.println("    public Object getValueByPathWithCollections(");
        out.println("            " + simpleClassName + " entity,");
        out.println("            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> path,");
        out.println("            java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations");
        out.println("    ) {");
        out.println("        if (entity == null || path == null || path.isEmpty()) {");
        out.println("            return null;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        Object currentValue = entity;");
        out.println();
        out.println("        // Iterate through path elements");
        out.println("        for (int i = 0; i < path.size(); i++) {");
        out.println("            if (currentValue == null) {");
        out.println("                return null; // Null-safe navigation");
        out.println(EXP_1);
        out.println();
        out.println("            com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> metaAttribute = path.get(i);");
        out.println();
        out.println("            // Check if there's a collection operation at this index");
        out.println("            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> collectionOp = null;");
        out.println("            if (collectionOperations != null) {");
        out.println("                for (com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> op : collectionOperations) {");
        out.println("                    if (op.getPathIndex() == i) {");
        out.println("                        collectionOp = op;");
        out.println("                        break;");
        out.println("                    }");
        out.println(EXP_4);
        out.println(EXP_1);
        out.println();
        out.println("            if (collectionOp != null) {");
        out.println("                // Apply collection operation");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> service = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(currentValue.getClass());");
        out.println();
        out.println("                // Get the collection field value");
        out.println("                Object collectionValue = service.getFieldValue(currentValue, metaAttribute);");
        out.println();
        out.println("                if (collectionValue == null) {");
        out.println("                    return null;");
        out.println(EXP_4);
        out.println();
        out.println("                if (!(collectionValue instanceof java.util.Collection)) {");
        out.println("                    throw new IllegalStateException(");
        out.println(COLLECTION_OP_NON_COLLECTION);
        out.println(CLOSING_PARENTHESIS_SEMICOLON);
        out.println(EXP_4);
        out.println();
        out.println("                java.util.Collection<?> collection = (java.util.Collection<?>) collectionValue;");
        out.println();
        out.println("                // Get the service for the collection element type");
        out.println("                Class<?> elementType = collectionOp.getElementType();");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> elementService = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(elementType);");
        out.println();
        out.println("                // Apply the collection operation with specification");
        out.println("                com.thy.fss.common.inmemory.specification.Specification<Object> spec = ");
        out.println("                    (com.thy.fss.common.inmemory.specification.Specification<Object>) collectionOp.getSpecification();");
        out.println();
        out.println("                currentValue = elementService.extractFromCollection(");
        out.println("                    (java.util.Collection<Object>) collection,");
        out.println("                    collectionOp.getSelector(),");
        out.println("                    spec");
        out.println("                );");
        out.println("            } else {");
        out.println("                // Regular field access");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> service = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(currentValue.getClass());");
        out.println();
        out.println("                currentValue = service.getFieldValue(currentValue, metaAttribute);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        return currentValue;");
        out.println("    }");
        out.println();
    }

    /**
     * Generates the setValueByPathWithCollections method. This method navigates
     * through the path and sets the value at the target location.
     */
    private void generateSetValueByPathWithCollections(PrintWriter out, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Sets a value using a path that may contain collection operations.");
        out.println("     * Navigates through the path and sets the value at the target location.");
        out.println("     * Generated at build time with direct method calls (no reflection).");
        out.println(EXP_2);
        out.println(OVERRIDE_ANNOTATION);
        out.println(SUPPRESS_WARNINGS_UNCHECKED);
        out.println("    public void setValueByPathWithCollections(");
        out.println("            " + simpleClassName + " entity,");
        out.println("            java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> path,");
        out.println("            java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations,");
        out.println("            Object value");
        out.println("    ) {");
        out.println("        if (entity == null || path == null || path.isEmpty()) {");
        out.println("            return;");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Navigate to the parent of the final element");
        out.println("        Object currentValue = entity;");
        out.println();
        out.println("        for (int i = 0; i < path.size() - 1; i++) {");
        out.println("            if (currentValue == null) {");
        out.println("                throw new IllegalStateException(");
        out.println("                    \"Cannot set value: intermediate value is null at path index \" + i");
        out.println("                );");
        out.println(EXP_1);
        out.println();
        out.println("            com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> metaAttribute = path.get(i);");
        out.println();
        out.println("            // Check if there's a collection operation at this index");
        out.println("            com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> collectionOp = null;");
        out.println("            if (collectionOperations != null) {");
        out.println("                for (com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> op : collectionOperations) {");
        out.println("                    if (op.getPathIndex() == i) {");
        out.println("                        collectionOp = op;");
        out.println("                        break;");
        out.println("                    }");
        out.println(EXP_4);
        out.println(EXP_1);
        out.println();
        out.println("            if (collectionOp != null) {");
        out.println("                // Apply collection operation");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> service = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(currentValue.getClass());");
        out.println();
        out.println("                Object collectionValue = service.getFieldValue(currentValue, metaAttribute);");
        out.println();
        out.println("                if (collectionValue == null) {");
        out.println("                    throw new IllegalStateException(");
        out.println("                        \"Cannot set value: collection is null at path index \" + i");
        out.println(CLOSING_PARENTHESIS_SEMICOLON);
        out.println(EXP_4);
        out.println();
        out.println("                if (!(collectionValue instanceof java.util.Collection)) {");
        out.println("                    throw new IllegalStateException(");
        out.println(COLLECTION_OP_NON_COLLECTION);
        out.println(CLOSING_PARENTHESIS_SEMICOLON);
        out.println(EXP_4);
        out.println();
        out.println("                java.util.Collection<?> collection = (java.util.Collection<?>) collectionValue;");
        out.println();
        out.println("                // Get the service for the collection element type");
        out.println("                Class<?> elementType = collectionOp.getElementType();");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> elementService = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(elementType);");
        out.println();
        out.println("                // Apply the collection operation with specification");
        out.println("                com.thy.fss.common.inmemory.specification.Specification<Object> spec = ");
        out.println("                    (com.thy.fss.common.inmemory.specification.Specification<Object>) collectionOp.getSpecification();");
        out.println();
        out.println("                currentValue = elementService.extractFromCollection(");
        out.println("                    (java.util.Collection<Object>) collection,");
        out.println("                    collectionOp.getSelector(),");
        out.println("                    spec");
        out.println("                );");
        out.println("            } else {");
        out.println("                // Regular field access");
        out.println("                com.thy.fss.common.inmemory.specification.SpecificationService<Object> service = ");
        out.println(SERVICE_VARIABLE);
        out.println("                    com.thy.fss.common.inmemory.specification.SpecificationServices.getService(currentValue.getClass());");
        out.println();
        out.println("                currentValue = service.getFieldValue(currentValue, metaAttribute);");
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        if (currentValue == null) {");
        out.println("            throw new IllegalStateException(");
        out.println("                \"Cannot set value: parent object is null\"");
        out.println("            );");
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        // Handle the final element");
        out.println("        com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?> lastAttribute = path.get(path.size() - 1);");
        out.println();
        out.println("        // Check if there's a collection operation on the final element");
        out.println("        com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> finalCollectionOp = null;");
        out.println("        if (collectionOperations != null) {");
        out.println("            for (com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?> op : collectionOperations) {");
        out.println("                if (op.getPathIndex() == path.size() - 1) {");
        out.println("                    finalCollectionOp = op;");
        out.println("                    break;");
        out.println(EXP_4);
        out.println(EXP_1);
        out.println(CLOSING_BRACE);
        out.println();
        out.println("        com.thy.fss.common.inmemory.specification.SpecificationService<Object> service = ");
        out.println("            (com.thy.fss.common.inmemory.specification.SpecificationService<Object>) ");
        out.println("            com.thy.fss.common.inmemory.specification.SpecificationServices.getService(currentValue.getClass());");
        out.println();
        out.println("        if (finalCollectionOp != null) {");
        out.println("            // Collection operation on final element");
        out.println("            Object collectionValue = service.getFieldValue(currentValue, lastAttribute);");
        out.println();
        out.println("            if (collectionValue == null) {");
        out.println("                // Initialize collection if null");
        out.println("                collectionValue = new java.util.ArrayList<>();");
        out.println("                service.setFieldValue(currentValue, lastAttribute, collectionValue);");
        out.println(EXP_1);
        out.println();
        out.println("            if (!(collectionValue instanceof java.util.Collection)) {");
        out.println("                throw new IllegalStateException(");
        out.println("                    \"Collection operation applied to non-collection field: \" + lastAttribute.getName()");
        out.println("                );");
        out.println(EXP_1);
        out.println();
        out.println("            java.util.Collection<Object> collection = (java.util.Collection<Object>) collectionValue;");
        out.println();
        out.println("            if (finalCollectionOp.getSelector() == com.thy.fss.common.inmemory.engine.mapping.CollectionSelector.ALL) {");
        out.println("                // ALL selector: add value to collection");
        out.println("                collection.add(value);");
        out.println("            } else {");
        out.println("                // FIRST/LAST/ANY selector: find element and set nested field");
        out.println("                // This case is not fully supported in this implementation");
        out.println("                // For now, we'll throw an exception");
        out.println("                throw new UnsupportedOperationException(");
        out.println("                    \"Setting values on specific collection elements (FIRST/LAST/ANY) is not yet supported. \" +");
        out.println("                    \"Use ALL selector to add elements to collections.\"");
        out.println("                );");
        out.println(EXP_1);
        out.println("        } else {");
        out.println("            // Regular field assignment");
        out.println("            service.setFieldValue(currentValue, lastAttribute, value);");
        out.println(CLOSING_BRACE);
        out.println("    }");
        out.println();
    }

    /**
     * Note: Static initializer method removed as services are now accessed via
     * SpecificationServiceRegistry. Services no longer register themselves -
     * they are accessed through build-time generated registry.
     */
    /**
     * Logs service generation information.
     */
    private void logServiceGeneration(String packageName, String serviceClassName, String simpleClassName) {
        debugLog("Generated StaticSpecificationService: " + packageName + "." + serviceClassName
                + " for entity: " + packageName + "." + simpleClassName
                + ". Service is available as singleton via " + serviceClassName + ".INSTANCE and accessible through "
                + "SpecificationServices.getService(" + simpleClassName + ".class) via registry lookup");
    }

    /**
     * Generates delegation methods for path navigation without runtime lookups.
     * These methods implement the abstract methods from BaseSpecificationService.
     */
    private void generateDelegationMethods(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println("    // ==================== DELEGATION METHODS (NO RUNTIME LOOKUPS) ====================");
        out.println();
        
        // Generate navigateNested method
        generateNavigateNestedMethod(out, fields, simpleClassName);
        
        // Generate navigateNestedForSet method
        generateNavigateNestedForSetMethod(out, fields, simpleClassName);
        
        // Generate createIntermediateInstanceForField method
        generateCreateIntermediateInstanceForFieldMethod(out, fields, simpleClassName);
    }
    
    /**
     * Generates the navigateNested method with direct INSTANCE references.
     * For each model-type field, generates if-else branch with direct service reference.
     */
    private void generateNavigateNestedMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Navigates to nested model fields using direct service references.");
        out.println("     * Eliminates runtime type lookups by using compile-time known types.");
        out.println("     * @param fieldValue The current field value to navigate from");
        out.println("     * @param attr The meta attribute being navigated");
        out.println("     * @param path The complete path being navigated");
        out.println("     * @param nextIndex The next index in the path to process");
        out.println("     * @return The value at the end of the path navigation");
        out.println(EXP_2);
        out.println("    @Override");
        out.println("    protected Object navigateNested(Object fieldValue,");
        out.println("                                    MetaAttribute<?, ?> attr,");
        out.println("                                    List<MetaAttribute<?, ?>> path,");
        out.println("                                    int nextIndex) {");
        out.println("        if (fieldValue == null) {");
        out.println("            return null;");
        out.println(CLOSING_BRACE);
        out.println();
        
        // Generate if-else branches for each model-type field
        boolean hasModelFields = false;
        boolean first = true;
        for (FieldInfo field : fields) {
            if (field.attributeType == AttributeType.MODEL) {
                hasModelFields = true;
                String nestedTypeName = field.fieldTypeName;
                String simpleNestedName = nestedTypeName.substring(nestedTypeName.lastIndexOf('.') + 1);
                String nestedServiceClass = simpleNestedName + "SpecificationService";
                
                String condition = first ? "if" : "} else if";
                out.println("        " + condition + " (attr == " + simpleClassName + "_." + field.fieldName + ") {");
                out.println("            // Direct INSTANCE reference - NO RUNTIME LOOKUP!");
                out.println("            return " + nestedServiceClass + ".INSTANCE.getValueByPathImpl(");
                out.println("                (" + nestedTypeName + ") fieldValue, path, nextIndex);");
                first = false;
            }
        }
        
        if (hasModelFields) {
            out.println("        } else {");
            out.println("            throw new IllegalArgumentException(\"Unknown nested field: \" + attr.getName());");
            out.println(CLOSING_BRACE);
        } else {
            out.println("        // No model-type fields in this entity");
            out.println("        throw new IllegalArgumentException(\"No nested model fields: \" + attr.getName());");
        }
        
        out.println("    }");
        out.println();
    }
    
    /**
     * Generates the navigateNestedForSet method with direct INSTANCE references.
     * Similar to navigateNested but for setting values.
     */
    private void generateNavigateNestedForSetMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Navigates to nested model fields for setting values using direct service references.");
        out.println("     * Eliminates runtime type lookups by using compile-time known types.");
        out.println("     * @param fieldValue The current field value to navigate from");
        out.println("     * @param attr The meta attribute being navigated");
        out.println("     * @param path The complete path being navigated");
        out.println("     * @param nextIndex The next index in the path to process");
        out.println("     * @param value The value to set at the end of the path");
        out.println(EXP_2);
        out.println("    @Override");
        out.println("    protected void navigateNestedForSet(Object fieldValue,");
        out.println("                                        MetaAttribute<?, ?> attr,");
        out.println("                                        List<MetaAttribute<?, ?>> path,");
        out.println("                                        int nextIndex,");
        out.println("                                        Object value) {");
        out.println("        if (fieldValue == null) {");
        out.println("            throw new IllegalStateException(\"Cannot navigate through null field: \" + attr.getName());");
        out.println(CLOSING_BRACE);
        out.println();
        
        // Generate if-else branches for each model-type field
        boolean hasModelFields = false;
        boolean first = true;
        for (FieldInfo field : fields) {
            if (field.attributeType == AttributeType.MODEL) {
                hasModelFields = true;
                String nestedTypeName = field.fieldTypeName;
                String simpleNestedName = nestedTypeName.substring(nestedTypeName.lastIndexOf('.') + 1);
                String nestedServiceClass = simpleNestedName + "SpecificationService";
                
                String condition = first ? "if" : "} else if";
                out.println("        " + condition + " (attr == " + simpleClassName + "_." + field.fieldName + ") {");
                out.println("            // Direct INSTANCE reference - NO RUNTIME LOOKUP!");
                out.println("            " + nestedServiceClass + ".INSTANCE.setValueByPathImpl(");
                out.println("                (" + nestedTypeName + ") fieldValue, path, nextIndex, value);");
                first = false;
            }
        }
        
        if (hasModelFields) {
            out.println("        } else {");
            out.println("            throw new IllegalArgumentException(\"Unknown nested field: \" + attr.getName());");
            out.println(CLOSING_BRACE);
        } else {
            out.println("        // No model-type fields in this entity");
            out.println("        throw new IllegalArgumentException(\"No nested model fields: \" + attr.getName());");
        }
        
        out.println("    }");
        out.println();
    }
    
    /**
     * Generates the createIntermediateInstanceForField method with direct INSTANCE references.
     * For each model-type field, generates if-else branch to create instances.
     */
    private void generateCreateIntermediateInstanceForFieldMethod(PrintWriter out, List<FieldInfo> fields, String simpleClassName) {
        out.println(EXP_3);
        out.println("     * Creates intermediate instances for null fields during path navigation.");
        out.println("     * Uses direct service references to create instances without runtime lookups.");
        out.println("     * @param attr The meta attribute for which to create an instance");
        out.println("     * @return A new instance of the field type");
        out.println(EXP_2);
        out.println("    @Override");
        out.println("    protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {");
        
        // Generate if-else branches for each model-type field
        boolean hasModelFields = false;
        boolean first = true;
        for (FieldInfo field : fields) {
            if (field.attributeType == AttributeType.MODEL) {
                hasModelFields = true;
                String nestedTypeName = field.fieldTypeName;
                String simpleNestedName = nestedTypeName.substring(nestedTypeName.lastIndexOf('.') + 1);
                String nestedServiceClass = simpleNestedName + "SpecificationService";
                
                String condition = first ? "if" : "} else if";
                out.println("        " + condition + " (attr == " + simpleClassName + "_." + field.fieldName + ") {");
                out.println("            try {");
                out.println("                // Direct INSTANCE reference - NO RUNTIME LOOKUP!");
                out.println("                return " + nestedServiceClass + ".INSTANCE.createInstance();");
                out.println("            } catch (Exception e) {");
                out.println("                throw new RuntimeException(\"Failed to create instance for field: \" + attr.getName(), e);");
                out.println(EXP_1);
                first = false;
            }
        }
        
        if (hasModelFields) {
            out.println("        } else {");
            out.println("            throw new IllegalArgumentException(\"Cannot create instance for non-model field: \" + attr.getName());");
            out.println(CLOSING_BRACE);
        } else {
            out.println("        // No model-type fields in this entity");
            out.println("        throw new IllegalArgumentException(\"No model fields to create instances for: \" + attr.getName());");
        }
        
        out.println("    }");
        out.println();
    }

    /**
     * Analyzes fields to find dependent model types that need
     * StaticSpecificationService instances.
     */
    private List<String> analyzeDependentModelTypes(List<FieldInfo> fields) {
        List<String> dependentTypes = new ArrayList<>();

        for (FieldInfo field : fields) {
            if (isModelType(field.fieldType)) {
                String modelTypeName = getFieldTypeName(field.fieldType);
                if (!dependentTypes.contains(modelTypeName)) {
                    dependentTypes.add(modelTypeName);
                    debugLog("[DEBUG] StaticSpecificationServiceGenerator: Found dependent model type: " + modelTypeName);
                }
            }
            // Also include collection element types that are model types
            if (field.isCollection && field.collectionElementType != null && isModelElementType(field.collectionElementType)) {
                String elementTypeName = field.collectionElementType;
                if (!dependentTypes.contains(elementTypeName)) {
                    dependentTypes.add(elementTypeName);
                    debugLog("[DEBUG] StaticSpecificationServiceGenerator: Found dependent collection element model type: " + elementTypeName);
                }
            }
        }

        return dependentTypes;
    }

    /**
     * Extracts the package name from a fully qualified class name.
     */
    private String getPackageName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Inner class to hold field information.
     */
    private record FieldInfo(String fieldName, String fieldTypeName, AttributeType attributeType,
                             String validationPrefix, String getterMethodName, String setterMethodName,
                             boolean isCollection, boolean isEnum, String collectionElementType,
                             TypeMirror fieldType) {

    }
}