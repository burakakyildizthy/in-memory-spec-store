package com.thy.fss.common.inmemory.processor.validation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates @MetaModel annotated classes before code generation.
 * Provides early detection of common issues with helpful error messages.
 */
public class MetaModelValidator {

    private static final Set<String> SUPPORTED_PRIMITIVE_TYPES = Set.of(
            "java.lang.String",
            "java.lang.Integer", "int",
            "java.lang.Long", "long",
            "java.lang.Double", "double",
            "java.lang.Boolean", "boolean"
    );

    private static final Set<String> SUPPORTED_TEMPORAL_TYPES = Set.of(
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.Instant"
    );

    private static final Set<String> SUPPORTED_COLLECTION_TYPES = Set.of(
            "java.util.List",
            "java.util.Set",
            "java.util.Collection"
    );

    private final ProcessingEnvironment processingEnv;

    public MetaModelValidator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Validates a @MetaModel annotated class.
     * Returns true if valid, false if validation errors were found.
     */
    public boolean validate(TypeElement typeElement, Set<String> allMetaModelClasses) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check class modifiers
        validateClassModifiers(typeElement, errors);

        // Check fields
        validateFields(typeElement, allMetaModelClasses, errors, warnings);

        // Report errors
        if (!errors.isEmpty()) {
            reportValidationErrors(typeElement, errors, warnings);
            return false;
        }

        // Report warnings only
        if (!warnings.isEmpty()) {
            reportValidationWarnings(typeElement, warnings);
        }

        return true;
    }

    /**
     * Validates class modifiers.
     */
    private void validateClassModifiers(TypeElement typeElement, List<String> errors) {
        Set<Modifier> modifiers = typeElement.getModifiers();

        // Check if class is abstract
        if (modifiers.contains(Modifier.ABSTRACT)) {
            errors.add("Class is abstract. Abstract classes can be annotated with @MetaModel, " +
                    "but ensure they have concrete subclasses.");
        }

        // Check if class is private
        if (modifiers.contains(Modifier.PRIVATE)) {
            errors.add("Class is private. @MetaModel classes must be public or package-private.");
        }
    }

    /**
     * Validates all fields in the class.
     */
    private void validateFields(TypeElement typeElement, Set<String> allMetaModelClasses,
                                List<String> errors, List<String> warnings) {
        List<VariableElement> fields = new ArrayList<>();

        // Collect all non-static fields
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD &&
                    !element.getModifiers().contains(Modifier.STATIC)) {
                fields.add((VariableElement) element);
            }
        }

        if (fields.isEmpty()) {
            warnings.add("Class has no non-static fields. No metamodel attributes will be generated.");
            return;
        }

        // Validate each field
        for (VariableElement field : fields) {
            validateField(field, allMetaModelClasses, errors, warnings);
        }
    }

    /**
     * Validates a single field.
     */
    private void validateField(VariableElement field, Set<String> allMetaModelClasses,
                               List<String> errors, List<String> warnings) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String fieldTypeName = fieldType.toString();

        // Skip validation for private fields (they're still processed)
        if (field.getModifiers().contains(Modifier.PRIVATE)) {
            // This is fine - private fields are supported
        }

        // Check if field type is supported
        if (!isFieldTypeSupported(fieldType, allMetaModelClasses)) {
            errors.add(String.format(
                    "Field '%s' has unsupported type '%s'. " +
                            "Supported types: primitives (String, Integer, Long, Double, Boolean), " +
                            "temporal types (LocalDate, LocalDateTime, Instant), " +
                            "enums, collections, or other @MetaModel classes.",
                    fieldName, getSimpleTypeName(fieldTypeName)
            ));
        }

        // Check for collection without generic type
        if (isCollectionType(fieldType) && !hasGenericType(fieldType)) {
            warnings.add(String.format(
                    "Field '%s' is a raw collection without generic type. " +
                            "Consider using Collection<T> for better type safety.",
                    fieldName
            ));
        }
    }

    /**
     * Checks if a field type is supported.
     */
    private boolean isFieldTypeSupported(TypeMirror fieldType, Set<String> allMetaModelClasses) {
        String typeName = fieldType.toString();

        // Remove generic parameters for checking
        String baseTypeName = typeName.contains("<") ? typeName.substring(0, typeName.indexOf('<')) : typeName;

        // Check primitive types
        if (SUPPORTED_PRIMITIVE_TYPES.contains(baseTypeName)) {
            return true;
        }

        // Check temporal types
        if (SUPPORTED_TEMPORAL_TYPES.contains(baseTypeName)) {
            return true;
        }

        // Check collection types
        if (SUPPORTED_COLLECTION_TYPES.contains(baseTypeName)) {
            return true;
        }

        // Check if it's an enum
        if (fieldType instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element.getKind() == ElementKind.ENUM) {
                return true;
            }

            // Check if it's a @MetaModel class
            if (element instanceof TypeElement typeElement) {
                String qualifiedName = typeElement.getQualifiedName().toString();
                if (allMetaModelClasses.contains(qualifiedName)) {
                    return true;
                }
            }
        }

        // Check for Object type (might be intentional)
        return "java.lang.Object".equals(baseTypeName);  // Allow but will generate ModelAttribute
    }

    /**
     * Checks if a type is a collection type.
     */
    private boolean isCollectionType(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                String typeName = typeElement.getQualifiedName().toString();
                return SUPPORTED_COLLECTION_TYPES.contains(typeName);
            }
        }
        return false;
    }

    /**
     * Checks if a type has generic parameters.
     */
    private boolean hasGenericType(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            return !declaredType.getTypeArguments().isEmpty();
        }
        return false;
    }

    /**
     * Reports validation errors.
     */
    private void reportValidationErrors(TypeElement typeElement, List<String> errors, List<String> warnings) {
        StringBuilder message = new StringBuilder();
        message.append("\n");
        message.append("================================================================================\n");
        message.append("@MetaModel VALIDATION FAILED\n");
        message.append("================================================================================\n");
        message.append("\n");
        message.append("Class: ").append(typeElement.getQualifiedName()).append("\n");
        message.append("\n");
        message.append("ERRORS:\n");
        for (int i = 0; i < errors.size(); i++) {
            message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
        }

        if (!warnings.isEmpty()) {
            message.append("\n");
            message.append("WARNINGS:\n");
            for (int i = 0; i < warnings.size(); i++) {
                message.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
            }
        }

        message.append("\n");
        message.append("IMPACT:\n");
        message.append("  Metamodel classes will NOT be generated for this class:\n");
        message.append("  - ").append(typeElement.getSimpleName()).append("_ (StaticMetaModel)\n");
        message.append("  - ").append(typeElement.getSimpleName()).append("Filter (FilterMetaModel)\n");
        message.append("  - ").append(typeElement.getSimpleName()).append("SpecificationService\n");
        message.append("\n");
        message.append("NEXT STEPS:\n");
        message.append("  1. Fix the errors listed above\n");
        message.append("  2. Run: ./gradlew clean build\n");
        message.append("  3. For more help, see: ANNOTATION_PROCESSOR_TROUBLESHOOTING.md\n");
        message.append("\n");
        message.append("================================================================================\n");

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                message.toString(),
                typeElement
        );
    }

    /**
     * Reports validation warnings.
     */
    private void reportValidationWarnings(TypeElement typeElement, List<String> warnings) {
        StringBuilder message = new StringBuilder();
        message.append("\n");
        message.append("@MetaModel Validation Warnings for: ").append(typeElement.getQualifiedName()).append("\n");
        for (int i = 0; i < warnings.size(); i++) {
            message.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                message.toString(),
                typeElement
        );
    }

    /**
     * Gets simple type name for display.
     */
    private String getSimpleTypeName(String fullyQualifiedName) {
        if (fullyQualifiedName.contains("<")) {
            return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
        }
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
}
