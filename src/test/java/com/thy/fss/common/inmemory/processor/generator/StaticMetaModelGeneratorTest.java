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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for StaticMetaModelGenerator focusing on core functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaticMetaModelGenerator Tests")
class StaticMetaModelGeneratorTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Messager messager;

    @Mock
    private Filer filer;

    @Mock
    private Elements elementUtils;

    @Mock
    private Types typeUtils;

    @Mock
    private JavaFileObject javaFileObject;

    private StaticMetaModelGenerator generator;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        lenient().when(processingEnv.getTypeUtils()).thenReturn(typeUtils);

        stringWriter = new StringWriter();
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        generator = new StaticMetaModelGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate basic meta model class")
    void shouldGenerateBasicMetaModelClass() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("package com.test;")),
                () -> assertTrue(generatedCode.contains("public class User_ {")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.specification.attribute.*;")),
                () -> assertTrue(generatedCode.contains("private User_() {")),
                () -> assertTrue(generatedCode.contains("Static meta model for User"))
        );
    }

    @Test
    @DisplayName("Should generate meta model for class in default package")
    void shouldGenerateMetaModelForDefaultPackage() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("User");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertFalse(generatedCode.contains("package ")),
                () -> assertTrue(generatedCode.contains("public class User_ {"))
        );
    }

    @Test
    @DisplayName("Should generate string field attribute")
    void shouldGenerateStringFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final StringAttribute<User> name =")),
                () -> assertTrue(generatedCode.contains("new StringAttribute<>(\"name\", User.class);")),
                () -> assertTrue(generatedCode.contains("Meta attribute for field 'name' of type String"))
        );
    }

    @Test
    @DisplayName("Should throw ProcessingException on IOException")
    void shouldThrowProcessingExceptionOnIOException() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");
        when(javaFileObject.openWriter()).thenThrow(new IOException("File creation failed"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class, () -> {
            generator.generate(typeElement);
        });

        assertAll(
                () -> assertTrue(exception.getMessage().contains("Failed to generate StaticMetaModel")),
                () -> assertTrue(exception.getMessage().contains("File creation failed")),
                () -> assertInstanceOf(IOException.class, exception.getCause())
        );
    }

    @Test
    @DisplayName("Should throw ProcessingException on unexpected error")
    void shouldThrowProcessingExceptionOnUnexpectedError() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");
        when(filer.createSourceFile(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class, () -> {
            generator.generate(typeElement);
        });

        assertAll(
                () -> assertTrue(exception.getMessage().contains("Unexpected error during StaticMetaModel generation")),
                () -> assertTrue(exception.getMessage().contains("Unexpected error")),
                () -> assertInstanceOf(RuntimeException.class, exception.getCause())
        );
    }

    // Helper methods
    private TypeElement createSimpleTypeElement(String qualifiedName) {
        TypeElement typeElement = mock(TypeElement.class);
        Name name = mock(Name.class);
        Name simpleName = mock(Name.class);

        lenient().when(name.toString()).thenReturn(qualifiedName);
        lenient().when(simpleName.toString()).thenReturn(getSimpleName(qualifiedName));
        lenient().when(typeElement.getQualifiedName()).thenReturn(name);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        // Setup superclass as Object (no inheritance)
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(typeElement.getSuperclass()).thenReturn(objectType);

        return typeElement;
    }

    private TypeElement createTypeElementWithStringField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createStringField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createStringField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.String");

        return field;
    }

    private String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    @Test
    @DisplayName("Should generate integer field attribute")
    void shouldGenerateIntegerFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithIntegerField("com.test.User", "age");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final IntegerAttribute<User> age =")),
                () -> assertTrue(generatedCode.contains("new IntegerAttribute<>(\"age\", User.class);"))
        );
    }

    @Test
    @DisplayName("Should generate long field attribute")
    void shouldGenerateLongFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLongField("com.test.User", "id");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final LongAttribute<User> id =")),
                () -> assertTrue(generatedCode.contains("new LongAttribute<>(\"id\", User.class);"))
        );
    }

    @Test
    @DisplayName("Should generate boolean field attribute")
    void shouldGenerateBooleanFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithBooleanField("com.test.User", "active");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final BooleanAttribute<User> active =")),
                () -> assertTrue(generatedCode.contains("new BooleanAttribute<>(\"active\", User.class);"))
        );
    }

    @Test
    @DisplayName("Should generate double field attribute")
    void shouldGenerateDoubleFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithDoubleField("com.test.User", "salary");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final DoubleAttribute<User> salary =")),
                () -> assertTrue(generatedCode.contains("new DoubleAttribute<>(\"salary\", User.class);"))
        );
    }

    @Test
    @DisplayName("Should generate LocalDate field attribute")
    void shouldGenerateLocalDateFieldAttribute() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLocalDateField("com.test.User", "birthDate");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static final LocalDateAttribute<User> birthDate =")),
                () -> assertTrue(generatedCode.contains("new LocalDateAttribute<>(\"birthDate\", User.class);")),
                () -> assertTrue(generatedCode.contains("import java.time.LocalDate;"))
        );
    }

    @Test
    @DisplayName("Should skip static fields")
    void shouldSkipStaticFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStaticField("com.test.User", "CONSTANT");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertFalse(generatedCode.contains("CONSTANT"));
    }

    @Test
    @DisplayName("Should handle multiple field types in single class")
    void shouldHandleMultipleFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createComplexTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("StringAttribute<User> name")),
                () -> assertTrue(generatedCode.contains("IntegerAttribute<User> age")),
                () -> assertTrue(generatedCode.contains("LocalDateAttribute<User> birthDate"))
        );
    }

    // Additional helper methods
    private TypeElement createTypeElementWithIntegerField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createIntegerField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithLongField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLongField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithBooleanField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createBooleanField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithDoubleField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createDoubleField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithLocalDateField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLocalDateField(fieldName);
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
        TypeElement typeElement = createSimpleTypeElement("com.test.User");

        List<Element> fields = List.of(
                createStringField("name"),
                createIntegerField("age"),
                createLocalDateField("birthDate")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private VariableElement createIntegerField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Integer");

        return field;
    }

    private VariableElement createLongField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Long");

        return field;
    }

    private VariableElement createBooleanField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Boolean");

        return field;
    }

    private VariableElement createDoubleField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Double");

        return field;
    }

    private VariableElement createLocalDateField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.time.LocalDate");

        return field;
    }

    private VariableElement createStaticField(String fieldName) {
        VariableElement field = createStringField(fieldName);
        lenient().when(field.getModifiers()).thenReturn(Set.of(Modifier.STATIC));
        return field;
    }
}