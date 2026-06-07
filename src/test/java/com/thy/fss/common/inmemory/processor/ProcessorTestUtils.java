package com.thy.fss.common.inmemory.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Utility class for creating test mocks and scenarios for MetaModelProcessor
 * tests. Provides helper methods to create properly configured mocks that work
 * with Java's type system.
 */
public class ProcessorTestUtils {

    /**
     * Creates a basic ProcessingEnvironment mock with messager.
     */
    public static ProcessingEnvironment createProcessingEnvironment() {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        javax.annotation.processing.Messager messager = mock(javax.annotation.processing.Messager.class);

        // Configure messager to print messages to System.out for debugging
        doAnswer(invocation -> {
            javax.tools.Diagnostic.Kind kind = invocation.getArgument(0);
            CharSequence msg = invocation.getArgument(1);
            System.out.println("[" + kind + "] " + msg);
            return null;
        }).when(messager).printMessage(any(javax.tools.Diagnostic.Kind.class), any(CharSequence.class));

        doAnswer(invocation -> {
            javax.tools.Diagnostic.Kind kind = invocation.getArgument(0);
            CharSequence msg = invocation.getArgument(1);
            javax.lang.model.element.Element element = invocation.getArgument(2);
            System.out.println("[" + kind + "] " + msg + " (element: " + element + ")");
            return null;
        }).when(messager).printMessage(any(javax.tools.Diagnostic.Kind.class), any(CharSequence.class), any(javax.lang.model.element.Element.class));

        when(processingEnv.getMessager()).thenReturn(messager);
        return processingEnv;
    }

    /**
     * Creates a TypeElement mock with the given qualified name.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TypeElement createTypeElement(String qualifiedName) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedNameMock = mock(Name.class);
        when(qualifiedNameMock.toString()).thenReturn(qualifiedName);
        when(typeElement.getQualifiedName()).thenReturn(qualifiedNameMock);

        // Extract simple name from qualified name (e.g., "com.test.Entity1" -> "Entity1")
        String simpleName = qualifiedName.contains(".")
                ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
                : qualifiedName;
        Name simpleNameMock = mock(Name.class);
        when(simpleNameMock.toString()).thenReturn(simpleName);
        when(typeElement.getSimpleName()).thenReturn(simpleNameMock);

        when(typeElement.getKind()).thenReturn(ElementKind.CLASS);
        when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());
        return typeElement;
    }

    /**
     * Creates a TypeElement mock with fields that depend on other entities.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TypeElement createTypeElementWithDependencies(String qualifiedName, TypeElement... dependencies) {
        TypeElement typeElement = createTypeElement(qualifiedName);

        List<Element> fields = new ArrayList<>();
        for (int i = 0; i < dependencies.length; i++) {
            VariableElement field = createFieldWithType("field" + i, dependencies[i]);
            fields.add(field);
        }

        when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    /**
     * Creates a VariableElement (field) mock that has a dependency on the given
     * TypeElement.
     */
    public static VariableElement createFieldWithType(String fieldName, TypeElement dependencyType) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);

        Name fieldNameMock = mock(Name.class);
        when(fieldNameMock.toString()).thenReturn(fieldName);
        when(field.getSimpleName()).thenReturn(fieldNameMock);

        DeclaredType fieldType = mock(DeclaredType.class);
        when(field.asType()).thenReturn(fieldType);
        when(fieldType.asElement()).thenReturn(dependencyType);
        when(fieldType.getTypeArguments()).thenReturn(Collections.emptyList());

        return field;
    }

    /**
     * Creates a VariableElement (field) mock with generic type dependencies.
     */
    public static VariableElement createGenericFieldWithType(String fieldName, TypeElement... genericTypes) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);

        Name fieldNameMock = mock(Name.class);
        when(fieldNameMock.toString()).thenReturn(fieldName);
        when(field.getSimpleName()).thenReturn(fieldNameMock);

        DeclaredType fieldType = mock(DeclaredType.class);
        when(field.asType()).thenReturn(fieldType);

        List<TypeMirror> typeArguments = new ArrayList<>();
        for (TypeElement genericType : genericTypes) {
            DeclaredType genericArgType = mock(DeclaredType.class);
            when(genericArgType.asElement()).thenReturn(genericType);
            typeArguments.add(genericArgType);
        }

        when(fieldType.getTypeArguments()).thenReturn((List) typeArguments);
        return field;
    }

    /**
     * Creates a VariableElement (field) mock with primitive type (no
     * dependencies).
     */
    public static VariableElement createPrimitiveField(String fieldName, String primitiveTypeName) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);

        Name fieldNameMock = mock(Name.class);
        when(fieldNameMock.toString()).thenReturn(fieldName);
        when(field.getSimpleName()).thenReturn(fieldNameMock);

        TypeMirror primitiveType = mock(TypeMirror.class);
        when(field.asType()).thenReturn(primitiveType);
        when(primitiveType.toString()).thenReturn(primitiveTypeName);

        return field;
    }

    /**
     * Creates a RoundEnvironment mock with the given annotated elements.
     */
    public static RoundEnvironment createRoundEnvironment(boolean processingOver, Element... annotatedElements) {
        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        when(roundEnv.processingOver()).thenReturn(processingOver);

        Set<Element> elementSet = new HashSet<>(Arrays.asList(annotatedElements));
        when(roundEnv.getElementsAnnotatedWith(MetaModel.class)).thenReturn((Set) elementSet);

        return roundEnv;
    }

    /**
     * Creates a non-class Element mock (interface, enum, etc.) for testing
     * error conditions.
     */
    public static Element createNonClassElement(ElementKind kind) {
        Element element = mock(Element.class);
        when(element.getKind()).thenReturn(kind);
        return element;
    }

    /**
     * Creates a method element mock for testing non-field elements.
     */
    public static ExecutableElement createMethodElement(String methodName) {
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);

        Name methodNameMock = mock(Name.class);
        when(methodNameMock.toString()).thenReturn(methodName);
        when(method.getSimpleName()).thenReturn(methodNameMock);

        return method;
    }

    /**
     * Creates a VariableElement (field) mock with array type (no dependencies).
     */
    public static VariableElement createArrayField(String fieldName, String arrayTypeName) {
        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);

        Name fieldNameMock = mock(Name.class);
        when(fieldNameMock.toString()).thenReturn(fieldName);
        when(field.getSimpleName()).thenReturn(fieldNameMock);

        TypeMirror arrayType = mock(TypeMirror.class);
        when(field.asType()).thenReturn(arrayType);
        when(arrayType.toString()).thenReturn(arrayTypeName);

        return field;
    }

    /**
     * Test scenario builder for common test cases.
     */
    public static class TestScenario {

        private final List<TypeElement> entities = new ArrayList<>();
        private final Map<TypeElement, List<Element>> entityFields = new HashMap<>();
        private boolean processingOver = false;

        public TestScenario addEntity(String qualifiedName) {
            TypeElement entity = createTypeElement(qualifiedName);
            entities.add(entity);
            entityFields.put(entity, new ArrayList<>());
            return this;
        }

        public TestScenario addEntityWithDependency(String qualifiedName, String dependencyName) {
            TypeElement entity = createTypeElement(qualifiedName);
            TypeElement dependency = entities.stream()
                    .filter(e -> e.getQualifiedName().toString().equals(dependencyName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Dependency not found: " + dependencyName));

            VariableElement field = createFieldWithType("field", dependency);
            List<Element> fields = new ArrayList<>();
            fields.add(field);

            when(entity.getEnclosedElements()).thenReturn((List) fields);
            entities.add(entity);
            entityFields.put(entity, fields);
            return this;
        }

        public TestScenario addFieldToEntity(String entityName, String fieldName, String fieldTypeName) {
            TypeElement entity = entities.stream()
                    .filter(e -> e.getQualifiedName().toString().equals(entityName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityName));

            TypeElement fieldType = entities.stream()
                    .filter(e -> e.getQualifiedName().toString().equals(fieldTypeName))
                    .findFirst()
                    .orElse(null);

            Element field;
            if (fieldType != null) {
                field = createFieldWithType(fieldName, fieldType);
            } else {
                field = createPrimitiveField(fieldName, fieldTypeName);
            }

            List<Element> fields = entityFields.get(entity);
            fields.add(field);
            when(entity.getEnclosedElements()).thenReturn((List) fields);
            return this;
        }

        public TestScenario setProcessingOver(boolean processingOver) {
            this.processingOver = processingOver;
            return this;
        }

        public RoundEnvironment buildRoundEnvironment() {
            return createRoundEnvironment(processingOver, entities.toArray(new Element[0]));
        }

        public List<TypeElement> getEntities() {
            return new ArrayList<>(entities);
        }
    }
}
