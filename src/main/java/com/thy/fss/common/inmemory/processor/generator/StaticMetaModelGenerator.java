package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Generator for StaticMetaModel classes (ClassName_).
 * Creates type-safe field references for use in specifications and filters.
 * <p>
 * Supports all attribute types including:
 * - Primitive and wrapper types (String, Integer, Long, Double, Boolean)
 * - Temporal types (LocalDate, LocalDateTime, Instant)
 * - Complex types (Model, Collection, Enum)
 * - Inheritance and generic type handling
 * - Nested class support
 */
public class StaticMetaModelGenerator {

    public static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
    public static final String CLASS_SUFFIX = ".class);";
    public static final String CLASS_SUFFIX2 = ".class, ";
    // Debug mode flag - controlled by system property
    private static final boolean DEBUG_MODE = Boolean.parseBoolean(
            System.getProperty("inmemory.processor.debug", "false"));
    // Supported primitive and wrapper types
    private static final Map<String, String> PRIMITIVE_TYPE_MAPPING = Map.of(
            "java.lang.String", "StringAttribute",
            "java.lang.Integer", "IntegerAttribute",
            "int", "IntegerAttribute",
            "java.lang.Long", "LongAttribute",
            "long", "LongAttribute",
            "java.lang.Double", "DoubleAttribute",
            "double", "DoubleAttribute",
            "java.lang.Boolean", "BooleanAttribute",
            "boolean", "BooleanAttribute"
    );
    // Supported temporal types
    private static final Map<String, String> TEMPORAL_TYPE_MAPPING = Map.of(
            "java.time.LocalDate", "LocalDateAttribute",
            "java.time.LocalDateTime", "LocalDateTimeAttribute",
            "java.time.Instant", "InstantAttribute"
    );
    // Collection types
    private static final Set<String> COLLECTION_TYPES = Set.of(
            "java.util.List",
            "java.util.Set",
            JAVA_UTIL_COLLECTION,
            "java.util.ArrayList",
            "java.util.HashSet",
            "java.util.LinkedList",
            "java.util.TreeSet"
    );
    private final ProcessingEnvironment processingEnv;

    /**
     * Constructor for StaticMetaModelGenerator.
     *
     * @param processingEnv the processing environment
     */
    public StaticMetaModelGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Prints debug message only if debug mode is enabled.
     */
    private void debugLog(String message) {
        if (DEBUG_MODE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[DEBUG] StaticMetaModelGenerator: " + message
            );
        }
    }

    /**
     * Generates a StaticMetaModel class for the given type element.
     *
     * @param typeElement the class to generate a meta model for
     * @throws ProcessingException if generation fails
     */
    public void generate(TypeElement typeElement) throws ProcessingException {
        try {
            String className = typeElement.getQualifiedName().toString();
            String simpleClassName = typeElement.getSimpleName().toString();
            String packageName = getPackageName(className);
            String metaModelClassName = simpleClassName + "_";

            // Debug log: Generation start
            debugLog("Starting generation for class " + className);
            debugLog("Package: " + packageName + ", MetaModel class: " + metaModelClassName);

            debugLog("Generating StaticMetaModel: " + metaModelClassName + " for " + className);

            // Create the Java file
            String fullyQualifiedClassName = packageName.isEmpty() ?
                    metaModelClassName :
                    packageName + "." + metaModelClassName;

            // Debug log: File creation
            debugLog("Creating source file: " + fullyQualifiedClassName);

            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(fullyQualifiedClassName);

            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                // Debug log: Generation process
                debugLog("Starting class content generation");

                generateStaticMetaModelClass(out, typeElement, packageName, metaModelClassName);

                debugLog("Class content generation completed");
            }

            // Debug log: Success
            debugLog("Successfully generated StaticMetaModel: " + metaModelClassName);

        } catch (IOException e) {
            String errorMsg = buildDetailedErrorMessage(typeElement, e, "IOException - File creation failed");
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    errorMsg,
                    typeElement
            );
            throw new ProcessingException("Failed to generate StaticMetaModel for " +
                    typeElement.getQualifiedName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            String errorMsg = buildDetailedErrorMessage(typeElement, e, "Unexpected error");
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    errorMsg,
                    typeElement
            );
            throw new ProcessingException("Unexpected error during StaticMetaModel generation for " +
                    typeElement.getQualifiedName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Builds a detailed error message for generation failures.
     */
    private String buildDetailedErrorMessage(TypeElement typeElement, Exception e, String errorType) {
        String msg = "\n" +
                "Failed to generate StaticMetaModel for: " + typeElement.getQualifiedName() + "\n" +
                "Error Type: " + errorType + "\n" +
                "Error Message: " + (e.getMessage() != null ? e.getMessage() : "No message") + "\n" +
                "\n" +
                "This will cause compilation errors like:\n" +
                "  - Cannot find symbol: " + typeElement.getSimpleName() + "_\n" +
                "  - Cannot find symbol: " + typeElement.getSimpleName() + "Filter\n" +
                "  - Cannot find symbol: " + typeElement.getSimpleName() + "SpecificationService\n" +
                "\n" +
                "Possible causes:\n" +
                "  1. Invalid field types in the class\n" +
                "  2. Missing dependencies for field types\n" +
                "  3. Circular dependencies between @MetaModel classes\n" +
                "  4. File system permissions issue\n" +
                "\n" +
                "To debug, run: ./gradlew clean build -Dinmemory.processor.debug=true\n";
        return msg;
    }

    /**
     * Generates the complete StaticMetaModel class content.
     */
    private void generateStaticMetaModelClass(PrintWriter out, TypeElement typeElement,
                                              String packageName, String metaModelClassName) {
        String simpleClassName = typeElement.getSimpleName().toString();

        // Debug log: Class generation details
        debugLog("Generating class content for " + metaModelClassName);

        // Package declaration (only if not in default package)
        if (!packageName.isEmpty()) {
            out.println("package " + packageName + ";");
            out.println();
            debugLog("Added package declaration: " + packageName);
        }

        // Imports
        debugLog("Starting imports generation");
        generateImports(out, typeElement);

        // Class declaration with JavaDoc
        debugLog("Adding class declaration and JavaDoc");
        out.println("/**");
        out.println(" * Static meta model for " + simpleClassName + ".");
        out.println(" * Provides type-safe field references for use in specifications and filters.");
        out.println(" * Generated by StaticMetaModelGenerator.");
        out.println(" */");
        out.println("public class " + metaModelClassName + " {");
        out.println();

        // Generate field attributes
        debugLog("Starting field attributes generation");
        generateFieldAttributes(out, typeElement);

        // Private constructor to prevent instantiation
        debugLog("Adding private constructor");
        out.println("    /**");
        out.println("     * Private constructor to prevent instantiation.");
        out.println("     * This class contains only static field references.");
        out.println("     */");
        out.println("    private " + metaModelClassName + "() {");
        out.println("        // Utility class - no instantiation");
        out.println("    }");

        out.println("}");

        debugLog("Class generation completed for " + metaModelClassName);
    }

    /**
     * Generates import statements for the StaticMetaModel class.
     */
    private void generateImports(PrintWriter out, TypeElement typeElement) {
        Set<String> imports = new HashSet<>();

        // Always import base attribute classes
        imports.add("com.thy.fss.common.inmemory.specification.attribute.*");

        debugLog("Added base attribute import");

        // Analyze fields to determine required imports
        List<Element> allFields = getAllFields(typeElement);
        debugLog("Found " + allFields.size() + " fields to analyze for imports");

        for (Element enclosedElement : allFields) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                addImportsForField(imports, field);
            }
        }

        // Sort and output imports
        debugLog("Generated " + imports.size() + " import statements");
        imports.stream().sorted().forEach(importStr -> out.println("import " + importStr + ";"));
        out.println();
    }

    /**
     * Gets all fields including inherited fields.
     */
    private List<Element> getAllFields(TypeElement typeElement) {
        List<Element> allFields = new ArrayList<>();

        debugLog("Analyzing fields for class " + typeElement.getQualifiedName());

        // Add fields from current class
        int currentClassFields = 0;
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD &&
                    !enclosedElement.getModifiers().contains(Modifier.STATIC)) {
                allFields.add(enclosedElement);
                currentClassFields++;
            }
        }

        debugLog("Found " + currentClassFields + " non-static fields in current class");

        // Add fields from superclass (inheritance support)
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.NONE) {
            Element superElement = processingEnv.getTypeUtils().asElement(superclass);
            if (superElement instanceof TypeElement superTypeElement) {
                debugLog("Checking superclass: " + superTypeElement.getQualifiedName());

                // Only include fields from @MetaModel annotated superclasses
                if (superTypeElement.getAnnotation(MetaModel.class) != null) {
                    debugLog("Superclass has @MetaModel annotation, including its fields");
                    List<Element> inheritedFields = getAllFields(superTypeElement);
                    allFields.addAll(inheritedFields);
                    debugLog("Added " + inheritedFields.size() + " inherited fields");
                } else {
                    debugLog("Superclass does not have @MetaModel annotation, skipping");
                }
            }
        }

        debugLog("Total fields found: " + allFields.size());
        return allFields;
    }

    /**
     * Adds necessary imports for a field type.
     */
    private void addImportsForField(Set<String> imports, VariableElement field) {
        TypeMirror fieldType = field.asType();
        String fieldName = field.getSimpleName().toString();

        debugLog("Analyzing imports for field '" + fieldName + "' of type: " + fieldType);

        // Handle temporal types
        if (fieldType.toString().contains("LocalDate")) {
            imports.add("java.time.LocalDate");
            debugLog("Added LocalDate import for field: " + fieldName);
        }
        if (fieldType.toString().contains("LocalDateTime")) {
            imports.add("java.time.LocalDateTime");
            debugLog("Added LocalDateTime import for field: " + fieldName);
        }
        if (fieldType.toString().contains("Instant")) {
            imports.add("java.time.Instant");
            debugLog("Added Instant import for field: " + fieldName);
        }

        // Handle collection types
        if (isCollectionType(fieldType)) {
            imports.add(JAVA_UTIL_COLLECTION);
            debugLog("Added Collection import for field: " + fieldName);

            // Also import collection element type if it's from a different package
            if (fieldType instanceof DeclaredType collectionDeclaredType) {
                List<? extends TypeMirror> typeArguments = collectionDeclaredType.getTypeArguments();
                if (!typeArguments.isEmpty()) {
                    TypeMirror elementType = typeArguments.get(0);
                    if (elementType instanceof DeclaredType elementDeclaredType) {
                        Element elementElement = elementDeclaredType.asElement();
                        if (elementElement instanceof TypeElement elementTypeElement) {
                            String elementQualifiedName = elementTypeElement.getQualifiedName().toString();
                            if (!PRIMITIVE_TYPE_MAPPING.containsKey(elementQualifiedName) &&
                                    !TEMPORAL_TYPE_MAPPING.containsKey(elementQualifiedName) &&
                                    !elementQualifiedName.startsWith("java.lang") &&
                                    !elementQualifiedName.startsWith("java.util") &&
                                    !elementQualifiedName.equals("java.lang.Object")) {
                                imports.add(elementQualifiedName);
                                debugLog("Added collection element type import: " + elementQualifiedName + " for field: " + fieldName);
                            }
                        }
                    }
                }
            }
        }

        // Handle complex types (ModelAttribute)
        if (fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                String qualifiedName = typeElement.getQualifiedName().toString();

                // Add import for complex types that are not primitives, wrappers, or temporal types
                if (!PRIMITIVE_TYPE_MAPPING.containsKey(qualifiedName) &&
                        !TEMPORAL_TYPE_MAPPING.containsKey(qualifiedName) &&
                        !qualifiedName.startsWith("java.lang") &&
                        !qualifiedName.startsWith("java.util") &&
                        !qualifiedName.equals("java.lang.Object")) {
                    imports.add(qualifiedName);
                    debugLog("Added complex type import: " + qualifiedName + " for field: " + fieldName);
                }
            }
        }
    }

    /**
     * Generates field attribute declarations.
     */
    private void generateFieldAttributes(PrintWriter out, TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();

        List<Element> allFields = getAllFields(typeElement);

        debugLog("Generating attributes for " + allFields.size() + " fields");

        int generatedFields = 0;
        int skippedFields = 0;

        for (Element enclosedElement : allFields) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                // Skip static fields
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    skippedFields++;
                    debugLog("Skipped static field: " + field.getSimpleName());
                    continue;
                }

                debugLog("Generating attribute for field: " + field.getSimpleName());

                generateFieldAttribute(out, field, className);
                out.println();
                generatedFields++;
            }
        }

        debugLog("Field generation summary - Generated: " + generatedFields + ", Skipped: " + skippedFields);
    }

    /**
     * Generates a single field attribute declaration.
     */
    private void generateFieldAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String fieldTypeName = fieldType.toString();

        debugLog("Processing field '" + fieldName + "' of type: " + fieldTypeName);

        // Generate JavaDoc
        out.println("    /**");
        out.println("     * Meta attribute for field '" + fieldName + "' of type " + getSimpleTypeName(fieldTypeName) + ".");
        out.println("     */");

        // Determine attribute type and generate declaration
        if (PRIMITIVE_TYPE_MAPPING.containsKey(fieldTypeName)) {
            debugLog("Generating primitive attribute for field: " + fieldName);
            generatePrimitiveAttribute(out, field, ownerClassName);
        } else if (TEMPORAL_TYPE_MAPPING.containsKey(fieldTypeName)) {
            debugLog("Generating temporal attribute for field: " + fieldName);
            generateTemporalAttribute(out, field, ownerClassName);
        } else if (isCollectionType(fieldType)) {
            debugLog("Generating collection attribute for field: " + fieldName);
            generateCollectionAttribute(out, field, ownerClassName);
        } else if (isEnumType(fieldType)) {
            debugLog("Generating enum attribute for field: " + fieldName);
            generateEnumAttribute(out, field, ownerClassName);
        } else {
            debugLog("Generating model attribute for field: " + fieldName);
            generateModelAttribute(out, field, ownerClassName);
        }
    }

    /**
     * Generates attribute for primitive/wrapper types.
     */
    private void generatePrimitiveAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        String fieldTypeName = field.asType().toString();
        String attributeType = PRIMITIVE_TYPE_MAPPING.get(fieldTypeName);
        String ownerSimpleName = getSimpleClassName(ownerClassName);

        out.println("    public static final " + attributeType + "<" + ownerSimpleName + "> " + fieldName + " =");
        out.println("        new " + attributeType + "<>(\"" + fieldName + "\", " + ownerSimpleName + CLASS_SUFFIX);
    }

    /**
     * Generates attribute for temporal types.
     */
    private void generateTemporalAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        String fieldTypeName = field.asType().toString();
        String attributeType = TEMPORAL_TYPE_MAPPING.get(fieldTypeName);
        String ownerSimpleName = getSimpleClassName(ownerClassName);

        out.println("    public static final " + attributeType + "<" + ownerSimpleName + "> " + fieldName + " =");
        out.println("        new " + attributeType + "<>(\"" + fieldName + "\", " + ownerSimpleName + CLASS_SUFFIX);
    }

    /**
     * Generates attribute for collection types.
     */
    private void generateCollectionAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        String ownerSimpleName = getSimpleClassName(ownerClassName);
        String elementType = getCollectionElementType(field.asType());
        String elementSimpleName = getSimpleClassName(elementType);

        out.println("    public static final CollectionAttribute<" + ownerSimpleName + ", " + elementSimpleName + "> " + fieldName + " =");
        out.println("        new CollectionAttribute<>(\"" + fieldName + "\", " + ownerSimpleName + CLASS_SUFFIX2 + elementSimpleName + CLASS_SUFFIX);
    }

    /**
     * Generates attribute for enum types.
     */
    private void generateEnumAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        String fieldTypeName = field.asType().toString();
        String ownerSimpleName = getSimpleClassName(ownerClassName);
        String enumSimpleName = getSimpleClassName(fieldTypeName);

        out.println("    public static final EnumAttribute<" + ownerSimpleName + ", " + enumSimpleName + "> " + fieldName + " =");
        out.println("        new EnumAttribute<>(\"" + fieldName + "\", " + ownerSimpleName + CLASS_SUFFIX2 + enumSimpleName + CLASS_SUFFIX);
    }

    /**
     * Generates attribute for model (nested object) types.
     */
    private void generateModelAttribute(PrintWriter out, VariableElement field, String ownerClassName) {
        String fieldName = field.getSimpleName().toString();
        String fieldTypeName = field.asType().toString();
        String ownerSimpleName = getSimpleClassName(ownerClassName);
        String modelSimpleName = getSimpleClassName(fieldTypeName);

        out.println("    public static final ModelAttribute<" + ownerSimpleName + ", " + modelSimpleName + "> " + fieldName + " =");
        out.println("        new ModelAttribute<>(\"" + fieldName + "\", " + ownerSimpleName + CLASS_SUFFIX2 + modelSimpleName + CLASS_SUFFIX);
    }

    /**
     * Checks if a type is a collection type.
     */
    private boolean isCollectionType(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                String typeName = typeElement.getQualifiedName().toString();
                return COLLECTION_TYPES.contains(typeName) ||
                        isAssignableToCollection(typeElement);
            }
        }
        return false;
    }

    /**
     * Checks if a type is assignable to Collection.
     */
    private boolean isAssignableToCollection(TypeElement typeElement) {
        TypeMirror collectionType = processingEnv.getElementUtils()
                .getTypeElement(JAVA_UTIL_COLLECTION).asType();
        return processingEnv.getTypeUtils().isAssignable(
                processingEnv.getTypeUtils().erasure(typeElement.asType()),
                processingEnv.getTypeUtils().erasure(collectionType)
        );
    }

    /**
     * Checks if a type is an enum type.
     */
    private boolean isEnumType(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            return element.getKind() == ElementKind.ENUM;
        }
        return false;
    }

    /**
     * Gets the element type of a collection.
     */
    private String getCollectionElementType(TypeMirror collectionType) {
        if (collectionType instanceof DeclaredType declaredType) {
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.get(0).toString();
            }
        }
        return "Object"; // Fallback for raw collections
    }

    /**
     * Extracts package name from fully qualified class name.
     */
    private String getPackageName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Gets simple class name from fully qualified name.
     */
    private String getSimpleClassName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Gets simple type name for documentation.
     */
    private String getSimpleTypeName(String typeName) {
        // Handle generic types
        if (typeName.contains("<")) {
            return typeName.substring(typeName.lastIndexOf('.') + 1);
        }
        return getSimpleClassName(typeName);
    }
}

