package com.thy.fss.common.inmemory.processor;

import com.thy.fss.common.inmemory.processor.dependency.DependencyGraph;
import com.thy.fss.common.inmemory.processor.dependency.TopologicalSorter;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import com.thy.fss.common.inmemory.processor.generator.FilterMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticSpecificationServiceGenerator;
import com.thy.fss.common.inmemory.processor.validation.MetaModelValidator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Annotation processor for @MetaModel annotated classes. Generates three
 * artifacts for each annotated class: 1. StaticMetaModel (ClassName_) -
 * Type-safe field references 2. FilterMetaModel (ClassNameFilter) - filter
 * definitions 3. StaticSpecificationService (ClassNameSpecificationService) -
 * Validation methods
 * <p>
 * The processor uses multi-phase generation with dependency resolution: Phase
 * 1: StaticMetaModel generation Phase 2: FilterMetaModel generation (depends on
 * StaticMetaModel) Phase 3: StaticSpecificationService generation (depends on
 * both)
 */
@SupportedAnnotationTypes("com.thy.fss.common.inmemory.processor.MetaModel")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MetaModelProcessor extends AbstractProcessor {

    // Debug mode flag - controlled by system property
    private static final boolean DEBUG_MODE = Boolean.parseBoolean(
            System.getProperty("inmemory.processor.debug", "false"));

    private final Map<String, TypeElement> annotatedClasses = new HashMap<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    private final TopologicalSorter topologicalSorter = new TopologicalSorter();
    private final Set<String> processedClasses = new HashSet<>();
    private boolean hasProcessed = false;

    private StaticMetaModelGenerator staticMetaModelGenerator;
    private FilterMetaModelGenerator filterMetaModelGenerator;
    private StaticSpecificationServiceGenerator staticSpecificationServiceGenerator;
    private MetaModelValidator validator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        // Initialize generators with processing environment
        this.staticMetaModelGenerator = new StaticMetaModelGenerator(processingEnv);
        this.filterMetaModelGenerator = new FilterMetaModelGenerator(processingEnv);
        this.staticSpecificationServiceGenerator = new StaticSpecificationServiceGenerator(processingEnv);
        this.validator = new MetaModelValidator(processingEnv);
    }

    /**
     * Prints debug message only if debug mode is enabled.
     */
    private void debugLog(String message) {
        if (DEBUG_MODE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[DEBUG] MetaModelProcessor: " + message
            );
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            debugLog("=".repeat(80));
            debugLog("Starting processing round");
            debugLog("Annotations: " + annotations);
            debugLog("Processing over: " + roundEnv.processingOver());
            debugLog("Has processed: " + hasProcessed);

            if (annotations.isEmpty() || roundEnv.processingOver()) {
                debugLog("Skipping round - annotations empty or processing over");
                return false;
            }

            // Only process once to avoid duplicate file creation
            if (hasProcessed) {
                debugLog("Skipping subsequent round to avoid duplicate generation");
                return false;
            }

            // Phase 0: Collect all @MetaModel annotated classes
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "MetaModelProcessor: Starting annotation processing..."
            );
            collectAnnotatedClasses(roundEnv);

            if (annotatedClasses.isEmpty()) {
                return false;
            }

            // Filter out already processed classes
            Map<String, TypeElement> unprocessedClasses = new HashMap<>();
            for (Map.Entry<String, TypeElement> entry : annotatedClasses.entrySet()) {
                if (!processedClasses.contains(entry.getKey())) {
                    unprocessedClasses.put(entry.getKey(), entry.getValue());
                }
            }

            if (unprocessedClasses.isEmpty()) {
                return false;
            }

            // Phase 0.5: Validate all classes before generation
            debugLog("Validating @MetaModel classes");
            boolean allValid = true;
            for (TypeElement typeElement : unprocessedClasses.values()) {
                if (!validator.validate(typeElement, annotatedClasses.keySet())) {
                    allValid = false;
                }
            }

            if (!allValid) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "\n@MetaModel validation failed. Fix the errors above and rebuild."
                );
                return false;
            }
            debugLog("All @MetaModel classes validated successfully");

            // Phase 1: Build dependency graph for unprocessed classes
            buildDependencyGraph(unprocessedClasses);

            // Phase 2: Perform topological sort to determine generation order
            List<TypeElement> sortedClasses = performTopologicalSort(unprocessedClasses);

            // Phase 3: Multi-phase generation
            generateArtifacts(sortedClasses);

            // Mark classes as processed
            for (TypeElement element : sortedClasses) {
                processedClasses.add(element.getQualifiedName().toString());
            }

            hasProcessed = true;

            // Success message with generated classes
            printSuccessMessage(sortedClasses);

            return true;

        } catch (ProcessingException e) {
            // Check if this is an "Attempt to recreate" error (in message or cause)
            boolean isRecreateError = e.getMessage().contains("Attempt to recreate") ||
                    (e.getCause() != null && e.getCause().getMessage() != null && 
                     e.getCause().getMessage().contains("Attempt to recreate"));
            
            if (isRecreateError) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "MetaModel processing: File already exists, continuing: " + e.getMessage()
                );
                hasProcessed = true;
                return true;
            }

            // Print detailed error with stack trace
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "\n" + "=".repeat(80) + "\n" +
                            "METAMODEL PROCESSING EXCEPTION\n" +
                            "=".repeat(80) + "\n" +
                            "Error: " + e.getMessage() + "\n" +
                            "Type: " + e.getClass().getName() + "\n" +
                            "Stack trace:\n" + getStackTraceString(e) + "\n" +
                            "=".repeat(80)
            );
            printDetailedError(e, annotatedClasses);
            return false;
        } catch (Exception e) {
            // Check if this is an "Attempt to recreate" error (in message or cause)
            boolean isRecreateError = (e.getMessage() != null && e.getMessage().contains("Attempt to recreate")) ||
                    (e.getCause() != null && e.getCause().getMessage() != null && 
                     e.getCause().getMessage().contains("Attempt to recreate"));
            
            if (isRecreateError) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "MetaModel processing: File already exists, continuing: " + e.getMessage()
                );
                hasProcessed = true;
                return true;
            }

            // Print detailed error with full stack trace for unexpected exceptions
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "\n" + "=".repeat(80) + "\n" +
                            "UNEXPECTED EXCEPTION IN METAMODEL PROCESSOR\n" +
                            "=".repeat(80) + "\n" +
                            "Error: " + (e.getMessage() != null ? e.getMessage() : "No message") + "\n" +
                            "Type: " + e.getClass().getName() + "\n" +
                            "Stack trace:\n" + getStackTraceString(e) + "\n" +
                            "=".repeat(80)
            );
            printDetailedError(e, annotatedClasses);
            return false;
        }
    }

    /**
     * Converts exception stack trace to string.
     */
    private String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
            // Limit to first 10 stack frames for readability
            if (sb.length() > 2000) {
                sb.append("  ... (truncated)\n");
                break;
            }
        }
        if (e.getCause() != null) {
            sb.append("Caused by: ").append(e.getCause().toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Prints a detailed error message with troubleshooting steps.
     */
    private void printDetailedError(Exception e, Map<String, TypeElement> classes) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("\n");
        errorMessage.append("================================================================================\n");
        errorMessage.append("METAMODEL GENERATION FAILED\n");
        errorMessage.append("================================================================================\n");
        errorMessage.append("\n");
        errorMessage.append("The annotation processor could not generate metamodel classes.\n");
        errorMessage.append("This will cause compilation errors for generated classes like:\n");
        errorMessage.append("  - ClassName_ (StaticMetaModel)\n");
        errorMessage.append("  - ClassNameFilter (FilterMetaModel)\n");
        errorMessage.append("  - ClassNameSpecificationService (SpecificationService)\n");
        errorMessage.append("\n");
        errorMessage.append("ERROR DETAILS:\n");
        errorMessage.append("  Type: ").append(e.getClass().getSimpleName()).append("\n");
        errorMessage.append("  Message: ").append(e.getMessage() != null ? e.getMessage() : "No message available").append("\n");

        if (e.getCause() != null) {
            errorMessage.append("  Cause: ").append(e.getCause().getMessage()).append("\n");
        }

        errorMessage.append("\n");
        errorMessage.append("AFFECTED CLASSES:\n");
        for (String className : classes.keySet()) {
            errorMessage.append("  - ").append(className).append("\n");
        }

        errorMessage.append("\n");
        errorMessage.append("TROUBLESHOOTING STEPS:\n");
        errorMessage.append("  1. Clean and rebuild the project:\n");
        errorMessage.append("     ./gradlew clean build\n");
        errorMessage.append("\n");
        errorMessage.append("  2. Check if @MetaModel annotated classes have:\n");
        errorMessage.append("     - Valid field types (supported primitives, temporal types, or other @MetaModel classes)\n");
        errorMessage.append("     - No circular dependencies between classes\n");
        errorMessage.append("     - Proper access modifiers (public or package-private)\n");
        errorMessage.append("\n");
        errorMessage.append("  3. Enable debug mode to see detailed processing logs:\n");
        errorMessage.append("     ./gradlew clean build -Dinmemory.processor.debug=true\n");
        errorMessage.append("\n");
        errorMessage.append("  4. Check for compilation errors in @MetaModel annotated classes\n");
        errorMessage.append("\n");
        errorMessage.append("  5. Verify annotation processor is properly configured in build.gradle\n");
        errorMessage.append("\n");

        if (e instanceof ProcessingException) {
            errorMessage.append("SPECIFIC ISSUE:\n");
            errorMessage.append("  This is a known processing error. Check the error message above for details.\n");
            errorMessage.append("\n");
        } else {
            errorMessage.append("UNEXPECTED ERROR:\n");
            errorMessage.append("  This is an unexpected error. Please report this issue with:\n");
            errorMessage.append("  - The full error stack trace\n");
            errorMessage.append("  - Your @MetaModel annotated class definitions\n");
            errorMessage.append("  - Java version: ").append(System.getProperty("java.version")).append("\n");
            errorMessage.append("\n");
        }

        errorMessage.append("================================================================================\n");

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                errorMessage.toString()
        );

        // Print stack trace in debug mode
        if (DEBUG_MODE && e.getStackTrace().length > 0) {
            StringBuilder stackTrace = new StringBuilder("\nSTACK TRACE:\n");
            for (int i = 0; i < Math.min(10, e.getStackTrace().length); i++) {
                stackTrace.append("  at ").append(e.getStackTrace()[i]).append("\n");
            }
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    stackTrace.toString()
            );
        }
    }

    /**
     * Prints a success message with generated classes.
     */
    private void printSuccessMessage(List<TypeElement> processedClasses) {
        if (DEBUG_MODE) {
            StringBuilder message = new StringBuilder();
            message.append("\n");
            message.append("================================================================================\n");
            message.append("METAMODEL GENERATION SUCCESSFUL\n");
            message.append("================================================================================\n");
            message.append("\n");
            message.append("Successfully generated metamodel classes for ").append(processedClasses.size()).append(" entities:\n");
            for (TypeElement element : processedClasses) {
                String className = element.getSimpleName().toString();
                message.append("  ✓ ").append(className).append("\n");
                message.append("    - ").append(className).append("_ (StaticMetaModel)\n");
                message.append("    - ").append(className).append("Filter (FilterMetaModel)\n");
                message.append("    - ").append(className).append("SpecificationService\n");
            }
            message.append("\n");
            message.append("================================================================================\n");

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    message.toString()
            );
        }
    }

    /**
     * Collects all classes annotated with @MetaModel.
     */
    private void collectAnnotatedClasses(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(MetaModel.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@MetaModel can only be applied to classes",
                        element
                );
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            String className = typeElement.getQualifiedName().toString();
            annotatedClasses.put(className, typeElement);

            debugLog("Found @MetaModel class: " + className);
        }
    }

    /**
     * Builds dependency graph by analyzing field types of given classes.
     */
    private void buildDependencyGraph(Map<String, TypeElement> classes) {
        for (TypeElement element : classes.values()) {
            String className = element.getQualifiedName().toString();
            Set<String> classDependencies = new HashSet<>();

            // Analyze fields to find dependencies
            for (Element enclosedElement : element.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    VariableElement field = (VariableElement) enclosedElement;
                    Set<String> fieldDependencies = extractFieldDependencies(field);
                    classDependencies.addAll(fieldDependencies);
                }
            }

            dependencyGraph.addNode(className, classDependencies);

            debugLog("Dependencies for " + className + ": " + classDependencies);
        }
    }

    /**
     * Extracts dependencies from a field by analyzing its type.
     */
    private Set<String> extractFieldDependencies(VariableElement field) {
        Set<String> dependencies = new HashSet<>();
        TypeMirror fieldType = field.asType();

        // Handle direct type dependencies
        String directTypeName = getTypeName(fieldType);
        if (annotatedClasses.containsKey(directTypeName)) {
            dependencies.add(directTypeName);
        }

        // Handle generic type dependencies (e.g., List<User>, Map<String, User>)
        if (fieldType instanceof DeclaredType declaredType) {
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            for (TypeMirror typeArgument : typeArguments) {
                String genericTypeName = getTypeName(typeArgument);
                if (annotatedClasses.containsKey(genericTypeName)) {
                    dependencies.add(genericTypeName);
                }
            }
        }

        return dependencies;
    }

    /**
     * Gets the qualified name of a type.
     */
    private String getTypeName(TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement.getQualifiedName().toString();
            }
        }
        return type.toString();
    }

    /**
     * Performs topological sort to determine generation order for given
     * classes.
     */
    private List<TypeElement> performTopologicalSort(Map<String, TypeElement> classes) throws ProcessingException {
        List<String> sortedClassNames = topologicalSorter.sort(dependencyGraph);

        // Convert class names back to TypeElements in sorted order
        List<TypeElement> sortedClasses = new ArrayList<>();
        for (String className : sortedClassNames) {
            TypeElement typeElement = classes.get(className);
            if (typeElement != null) {
                sortedClasses.add(typeElement);
            }
        }

        debugLog("Generation order: " + sortedClassNames);

        return sortedClasses;
    }

    /**
     * Generates all artifacts in the correct order using multi-phase
     * generation.
     */
    private void generateArtifacts(List<TypeElement> sortedClasses) throws ProcessingException {
        // Phase 1: Generate all StaticMetaModel classes first
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "Phase 1/3: Generating StaticMetaModel classes for " + sortedClasses.size() + " entities..."
        );
        debugLog("Phase 1: Generating StaticMetaModel classes");

        for (int i = 0; i < sortedClasses.size(); i++) {
            TypeElement element = sortedClasses.get(i);
            String className = element.getSimpleName().toString();
            try {
                debugLog("  [" + (i + 1) + "/" + sortedClasses.size() + "] Generating " + className + "_");
                staticMetaModelGenerator.generate(element);
                debugLog("  ✓ " + className + "_ generated successfully");
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate StaticMetaModel for " + className + ": " + e.getMessage()
                );
                throw new ProcessingException("StaticMetaModel generation failed for " + className, e);
            }
        }

        // Phase 2: Generate all FilterMetaModel classes with custom deserializers (depends on StaticMetaModel)
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "Phase 2/3: Generating FilterMetaModel classes with deserializers..."
        );
        debugLog("Phase 2: Generating FilterMetaModel classes with custom deserializers");

        for (int i = 0; i < sortedClasses.size(); i++) {
            TypeElement element = sortedClasses.get(i);
            String className = element.getSimpleName().toString();
            try {
                debugLog("  [" + (i + 1) + "/" + sortedClasses.size() + "] Generating " + className + "Filter");
                filterMetaModelGenerator.generate(element);
                debugLog("  ✓ " + className + "Filter generated successfully");
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate FilterMetaModel for " + className + ": " + e.getMessage()
                );
                throw new ProcessingException("FilterMetaModel generation failed for " + className, e);
            }
        }

        // Phase 3: Generate all StaticSpecificationService classes (depends on both)
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "Phase 3/3: Generating StaticSpecificationService classes..."
        );
        debugLog("Phase 3: Generating StaticSpecificationService classes");

        for (int i = 0; i < sortedClasses.size(); i++) {
            TypeElement element = sortedClasses.get(i);
            String className = element.getSimpleName().toString();
            try {
                debugLog("  [" + (i + 1) + "/" + sortedClasses.size() + "] Generating " + className + "SpecificationService");
                staticSpecificationServiceGenerator.generate(element);
                debugLog("  ✓ " + className + "SpecificationService generated successfully");
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate SpecificationService for " + className + ": " + e.getMessage()
                );
                throw new ProcessingException("SpecificationService generation failed for " + className, e);
            }
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "✓ All phases completed successfully! Generated " + (sortedClasses.size() * 3) + " files."
        );
    }
}
