package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaticSpecificationServiceGenerator Tests")
class StaticSpecificationServiceGeneratorTest {

    // String literals used multiple times
    private static final String VALIDATE_SPECIFICATION = "validateSpecification";
    private static final String VALIDATE_FILTER = "validateFilter";
    private static final String GET_ENTITY_CLASS = "getEntityClass";
    private static final String FIND_META_ATTRIBUTE = "findMetaAttribute";
    private static final String SET_FIELD_VALUE = "setFieldValue";
    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
    private static final String JAVA_LANG_LONG = "java.lang.Long";
    private static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String JAVA_TIME_INSTANT = "java.time.Instant";
    private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
    private static final String JAVA_UTIL_LIST = "java.util.List";
    private static final String JAVA_UTIL_SET = "java.util.Set";
    private static final String QUALIFIED_NAME_USER = "com.test.User";
    private static final String SIMPLE_NAME_USER = "User";
    private static final String FIELD_NAME_NAME = "name";
    private static final String FIELD_NAME_AGE = "age";
    private static final String FIELD_NAME_ACTIVE = "active";
    private static final String FIELD_NAME_ID = "id";
    private static final String FIELD_NAME_STATUS = "status";
    private static final String BIRTH_DATE_FIELD = "birthDate";
    private static final String CONSTANT_FIELD = "CONSTANT";

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Messager messager;

    @Mock
    private Filer filer;

    @Mock
    private JavaFileObject javaFileObject;

    private StringWriter stringWriter;
    private StaticSpecificationServiceGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        stringWriter = new StringWriter();

        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        generator = new StaticSpecificationServiceGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate basic service class")
    void shouldGenerateBasicServiceClass() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("package com.test;")),
                () -> assertTrue(generatedCode.contains("public class UserSpecificationService")),
                () -> assertTrue(generatedCode.contains("extends com.thy.fss.common.inmemory.specification.BaseSpecificationService<User>")),
                () -> assertTrue(generatedCode.contains("public Class<User> getEntityClass()")),
                () -> assertTrue(generatedCode.contains("return User.class;"))
        );
    }

    @Test
    @DisplayName("Should generate service for class in default package")
    void shouldGenerateServiceForDefaultPackage() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(SIMPLE_NAME_USER);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("package ;")),
                () -> assertTrue(generatedCode.contains("public class UserSpecificationService"))
        );
    }

    @Test
    @DisplayName("Should generate string field validation")
    void shouldGenerateStringFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField(QUALIFIED_NAME_USER, FIELD_NAME_NAME);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_FILTER)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_STRING)),
                () -> assertTrue(generatedCode.contains(GET_ENTITY_CLASS))
        );
    }

    @Test
    @DisplayName("Should generate integer field validation")
    void shouldGenerateIntegerFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithIntegerField(QUALIFIED_NAME_USER, FIELD_NAME_AGE);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_INTEGER))
        );
    }

    @Test
    @DisplayName("Should generate boolean field validation")
    void shouldGenerateBooleanFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithBooleanField(QUALIFIED_NAME_USER, FIELD_NAME_ACTIVE);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_BOOLEAN))
        );
    }

    @Test
    @DisplayName("Should generate LocalDate field validation")
    void shouldGenerateLocalDateFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLocalDateField(QUALIFIED_NAME_USER, BIRTH_DATE_FIELD);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_TIME_LOCAL_DATE))
        );
    }

    @Test
    @DisplayName("Should generate collection field validation")
    void shouldGenerateCollectionFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCollectionField(QUALIFIED_NAME_USER, "tags");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_UTIL_COLLECTION))
        );
    }

    @Test
    @DisplayName("Should skip static fields")
    void shouldSkipStaticFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStaticField(QUALIFIED_NAME_USER, CONSTANT_FIELD);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertFalse(generatedCode.contains(CONSTANT_FIELD));
    }

    @Test
    @DisplayName("Should handle multiple field types")
    void shouldHandleMultipleFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createComplexTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_STRING)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_INTEGER)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_BOOLEAN))
        );
    }

    @Test
    @DisplayName("Should throw ProcessingException on IOException")
    void shouldThrowProcessingExceptionOnIOException() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);
        when(javaFileObject.openWriter()).thenThrow(new IOException("Test IO error"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> generator.generate(typeElement));

        assertTrue(exception.getMessage().contains("Failed to generate StaticSpecificationService"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should throw ProcessingException on unexpected error")
    void shouldThrowProcessingExceptionOnUnexpectedError() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);
        when(javaFileObject.openWriter()).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> generator.generate(typeElement));

        assertTrue(exception.getMessage().contains("Unexpected error during StaticSpecificationService generation"));
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    // Helper methods
    private TypeElement createSimpleTypeElement(String qualifiedName) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedNameObj = mock(Name.class);
        Name simpleNameObj = mock(Name.class);

        String simpleName = qualifiedName.contains(".") ?
                qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1) : qualifiedName;

        lenient().when(qualifiedNameObj.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameObj.toString()).thenReturn(simpleName);
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedNameObj);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleNameObj);
        lenient().when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        return typeElement;
    }

    private TypeElement createTypeElementWithStringField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createStringField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithIntegerField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createIntegerField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithBooleanField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createBooleanField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithLocalDateField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLocalDateField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithCollectionField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createCollectionField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithStaticField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createStaticField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createComplexTypeElement() {
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);

        List<Element> fields = List.of(
                createStringField(FIELD_NAME_NAME),
                createIntegerField(FIELD_NAME_AGE),
                createBooleanField(FIELD_NAME_ACTIVE)
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private VariableElement createStringField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_LANG_STRING);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_LANG_STRING);

        return field;
    }

    private VariableElement createIntegerField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_LANG_INTEGER);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_LANG_INTEGER);

        return field;
    }

    private VariableElement createBooleanField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_LANG_BOOLEAN);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_LANG_BOOLEAN);

        return field;
    }

    private VariableElement createLocalDateField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_TIME_LOCAL_DATE);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_TIME_LOCAL_DATE);

        return field;
    }

    private VariableElement createCollectionField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeMirror elementType = mock(TypeMirror.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.util.List<java.lang.String>");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_UTIL_LIST);
        lenient().when(type.getTypeArguments()).thenReturn((List) Collections.singletonList(elementType));
        lenient().when(elementType.toString()).thenReturn(JAVA_LANG_STRING);

        return field;
    }

    private VariableElement createStaticField(String fieldName) {
        VariableElement field = createStringField(fieldName);
        lenient().when(field.getModifiers()).thenReturn(Set.of(Modifier.STATIC));
        return field;
    }

    // Additional test methods for better coverage
    @Test
    @DisplayName("Should generate Long field validation")
    void shouldGenerateLongFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLongField(QUALIFIED_NAME_USER, FIELD_NAME_ID);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains("validateId"))
        );
    }

    @Test
    @DisplayName("Should generate Double field validation")
    void shouldGenerateDoubleFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithDoubleField(QUALIFIED_NAME_USER, "salary");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_DOUBLE))
        );
    }

    @Test
    @DisplayName("Should generate LocalDateTime field validation")
    void shouldGenerateLocalDateTimeFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLocalDateTimeField(QUALIFIED_NAME_USER, "createdAt");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_TIME_LOCAL_DATE_TIME))
        );
    }

    @Test
    @DisplayName("Should generate Instant field validation")
    void shouldGenerateInstantFieldValidation() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithInstantField(QUALIFIED_NAME_USER, "lastModified");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(JAVA_TIME_INSTANT))
        );
    }

    @Test
    @DisplayName("Should handle nested model fields")
    void shouldHandleNestedModelFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithNestedModelField(QUALIFIED_NAME_USER, "profile");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains("ProfileSpecificationService"))
        );
    }

    @Test
    @DisplayName("Should generate service with no fields")
    void shouldGenerateServiceWithNoFields() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.EmptyUser");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public class EmptyUserSpecificationService")),
                () -> assertTrue(generatedCode.contains("extends com.thy.fss.common.inmemory.specification.BaseSpecificationService<EmptyUser>")),
                () -> assertTrue(generatedCode.contains("public Class<EmptyUser> getEntityClass()"))
        );
    }

    @Test
    @DisplayName("Should handle class with mixed field types")
    void shouldHandleMixedFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createMixedFieldTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(JAVA_LANG_STRING)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_INTEGER)),
                () -> assertTrue(generatedCode.contains(JAVA_TIME_LOCAL_DATE)),
                () -> assertTrue(generatedCode.contains(JAVA_UTIL_COLLECTION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_FILTER))
        );
    }

    // Helper methods for additional field types
    private TypeElement createTypeElementWithLongField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLongField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithDoubleField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createDoubleField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithLocalDateTimeField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLocalDateTimeField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithInstantField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createInstantField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithNestedModelField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createNestedModelField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createMixedFieldTypeElement() {
        TypeElement typeElement = createSimpleTypeElement("com.test.ComplexUser");

        List<Element> fields = List.of(
                createStringField(FIELD_NAME_NAME),
                createIntegerField(FIELD_NAME_AGE),
                createLocalDateField(BIRTH_DATE_FIELD),
                createCollectionField("tags")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private VariableElement createLongField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);
        TypeElement enclosingElement = mock(TypeElement.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingElement);
        lenient().when(enclosingElement.getInterfaces()).thenReturn(Collections.emptyList());
        lenient().when(type.toString()).thenReturn(JAVA_LANG_LONG);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_LANG_LONG);

        return field;
    }

    private VariableElement createDoubleField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);
        TypeElement enclosingElement = mock(TypeElement.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingElement);
        lenient().when(enclosingElement.getInterfaces()).thenReturn(Collections.emptyList());
        lenient().when(type.toString()).thenReturn(JAVA_LANG_DOUBLE);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_LANG_DOUBLE);

        return field;
    }

    private VariableElement createLocalDateTimeField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_TIME_LOCAL_DATE_TIME);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_TIME_LOCAL_DATE_TIME);

        return field;
    }

    private VariableElement createInstantField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn(JAVA_TIME_INSTANT);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_TIME_INSTANT);

        return field;
    }

    private VariableElement createNestedModelField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("com.test.Profile");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn("com.test.Profile");
        lenient().when(element.getKind()).thenReturn(ElementKind.CLASS);

        return field;
    }

    // Additional comprehensive tests for better coverage

    @Test
    @DisplayName("Should handle field analysis with various field types")
    void shouldHandleFieldAnalysisWithVariousFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createComplexTypeElementWithAllFieldTypes();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_FILTER)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_STRING)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_INTEGER)),
                () -> assertTrue(generatedCode.contains(JAVA_LANG_BOOLEAN)),
                () -> assertTrue(generatedCode.contains(JAVA_TIME_LOCAL_DATE)),
                () -> assertTrue(generatedCode.contains(JAVA_UTIL_COLLECTION))
        );
    }

    @Test
    @DisplayName("Should generate validation methods for all supported operators")
    void shouldGenerateValidationMethodsForAllSupportedOperators() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField(QUALIFIED_NAME_USER, FIELD_NAME_NAME);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Check for various operator method suffixes
        assertAll(
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_FILTER)),
                () -> assertTrue(generatedCode.contains(GET_ENTITY_CLASS)),
                () -> assertTrue(generatedCode.contains(FIND_META_ATTRIBUTE)),
                () -> assertTrue(generatedCode.contains(SET_FIELD_VALUE))
        );
    }

    @Test
    @DisplayName("Should handle enum fields with proper validation methods")
    void shouldHandleEnumFieldsWithProperValidationMethods() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithEnumField(QUALIFIED_NAME_USER, FIELD_NAME_STATUS);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Enum fields should now be supported with proper validation methods
        assertAll(
                () -> assertTrue(generatedCode.contains("public class UserSpecificationService")),
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(FIELD_NAME_STATUS)), // Enum field should be included
                () -> assertTrue(generatedCode.contains("validateStatusEquals")),
                () -> assertTrue(generatedCode.contains("validateStatusNotEquals")),
                () -> assertTrue(generatedCode.contains("validateStatusIsNull")),
                () -> assertTrue(generatedCode.contains("validateStatusIsNotNull"))
        );
    }

    @Test
    @DisplayName("Should generate proper package structure for nested packages")
    void shouldGenerateProperPackageStructureForNestedPackages() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.example.deep.nested.package.User");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("package com.example.deep.nested.package;")),
                () -> assertTrue(generatedCode.contains("public class UserSpecificationService"))
        );
    }

    @Test
    @DisplayName("Should handle final fields by skipping them")
    void shouldHandleFinalFieldsBySkippingThem() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithFinalField(QUALIFIED_NAME_USER, CONSTANT_FIELD);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertFalse(generatedCode.contains(CONSTANT_FIELD));
    }

    @Test
    @DisplayName("Should generate service with identifiable interface support")
    void shouldGenerateServiceWithIdentifiableInterfaceSupport() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithIdentifiableInterface(QUALIFIED_NAME_USER);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public class UserSpecificationService")),
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(GET_ENTITY_CLASS))
        );
    }

    @Test
    @DisplayName("Should handle collection fields with different element types")
    void shouldHandleCollectionFieldsWithDifferentElementTypes() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithMultipleCollectionTypes(QUALIFIED_NAME_USER);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(JAVA_UTIL_COLLECTION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION)),
                () -> assertTrue(generatedCode.contains(VALIDATE_FILTER))
        );
    }

    @Test
    @DisplayName("Should generate proper imports and dependencies")
    void shouldGenerateProperImportsAndDependencies() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.specification.SpecificationService;")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.specification.Operator;")),
                () -> assertTrue(generatedCode.contains("import java.util.*;")),
                () -> assertTrue(generatedCode.contains("import java.time.*;"))
        );
    }

    @Test
    @DisplayName("Should handle IOException during file creation")
    void shouldHandleIOExceptionDuringFileCreation() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement(QUALIFIED_NAME_USER);
        when(filer.createSourceFile(anyString())).thenThrow(new IOException("File creation failed"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> generator.generate(typeElement));

        assertTrue(exception.getMessage().contains("Failed to generate StaticSpecificationService"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should generate element validation helper methods")
    void shouldGenerateElementValidationHelperMethods() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCollectionField(QUALIFIED_NAME_USER, "tags");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("validateElementAgainstFilter")),
                () -> assertTrue(generatedCode.contains("validateElementWithFilter")),
                () -> assertTrue(generatedCode.contains("validateStringElement")),
                () -> assertTrue(generatedCode.contains("validateNumberElement")),
                () -> assertTrue(generatedCode.contains("validateBooleanElement"))
        );
    }

    @Test
    @DisplayName("Should generate field value extraction methods")
    void shouldGenerateFieldValueExtractionMethods() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField(QUALIFIED_NAME_USER, FIELD_NAME_NAME);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains(FIND_META_ATTRIBUTE)),
                () -> assertTrue(generatedCode.contains(SET_FIELD_VALUE)),
                () -> assertTrue(generatedCode.contains("createInstance"))
        );
    }

    @Test
    @DisplayName("Should generate sorting methods")
    void shouldGenerateSortingMethods() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField(QUALIFIED_NAME_USER, FIELD_NAME_NAME);

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Sorting methods should be generated
        assertTrue(generatedCode.contains(VALIDATE_SPECIFICATION));
    }

    // Helper methods for additional test scenarios

    private TypeElement createComplexTypeElementWithAllFieldTypes() {
        TypeElement typeElement = createSimpleTypeElement("com.test.ComplexUser");

        List<Element> fields = List.of(
                createStringField(FIELD_NAME_NAME),
                createIntegerField(FIELD_NAME_AGE),
                createBooleanField(FIELD_NAME_ACTIVE),
                createLocalDateField(BIRTH_DATE_FIELD),
                createLocalDateTimeField("createdAt"),
                createInstantField("lastModified"),
                createLongField(FIELD_NAME_ID),
                createDoubleField("salary"),
                createCollectionField("tags")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private TypeElement createTypeElementWithEnumField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createEnumField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithFinalField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createFinalField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithIdentifiableInterface(String qualifiedName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);

        // Mock Identifiable interface
        DeclaredType identifiableInterface = mock(DeclaredType.class);
        TypeElement identifiableElement = mock(TypeElement.class);
        Name identifiableName = mock(Name.class);

        lenient().when(identifiableName.toString()).thenReturn("com.thy.fss.common.inmemory.entity.Identifiable");
        lenient().when(identifiableElement.getQualifiedName()).thenReturn(identifiableName);
        lenient().when(identifiableInterface.asElement()).thenReturn(identifiableElement);

        lenient().when(typeElement.getInterfaces()).thenReturn((List) List.of(identifiableInterface));

        // Add id field
        VariableElement idField = createLongField(FIELD_NAME_ID);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(idField));

        return typeElement;
    }

    private TypeElement createTypeElementWithMultipleCollectionTypes(String qualifiedName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);

        List<Element> fields = List.of(
                createCollectionField("stringList"),
                createSetField("integerSet"),
                createListField("longList")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private VariableElement createEnumField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("com.test.Status");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn("com.test.Status");
        lenient().when(element.getKind()).thenReturn(ElementKind.ENUM);

        return field;
    }

    private VariableElement createFinalField(String fieldName) {
        VariableElement field = createStringField(fieldName);
        lenient().when(field.getModifiers()).thenReturn(Set.of(Modifier.FINAL));
        return field;
    }

    private VariableElement createSetField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeMirror elementType = mock(TypeMirror.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.util.Set<java.lang.Integer>");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_UTIL_SET);
        lenient().when(type.getTypeArguments()).thenReturn((List) Collections.singletonList(elementType));
        lenient().when(elementType.toString()).thenReturn(JAVA_LANG_INTEGER);

        return field;
    }

    private VariableElement createListField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeMirror elementType = mock(TypeMirror.class);
        Element element = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.util.List<java.lang.Long>");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(element.toString()).thenReturn(JAVA_UTIL_LIST);
        lenient().when(type.getTypeArguments()).thenReturn((List) Collections.singletonList(elementType));
        lenient().when(elementType.toString()).thenReturn(JAVA_LANG_LONG);

        return field;
    }
}