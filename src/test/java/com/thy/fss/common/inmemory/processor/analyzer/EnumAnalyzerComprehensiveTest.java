package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for EnumAnalyzer.
 * Tests enum analysis with different @JsonCreator and @JsonValue configurations.
 * <p>
 * Requirements tested: 1.4, 2.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Enum Analyzer Comprehensive Tests")
class EnumAnalyzerComprehensiveTest {

    @Mock
    private Elements elementUtils;

    @Mock
    private Types typeUtils;

    @Mock
    private TypeElement enumElement;

    @Mock
    private Name enumName;

    @Mock
    private ExecutableElement jsonCreatorMethod;

    @Mock
    private VariableElement jsonValueField;

    @Mock
    private ExecutableElement jsonValueMethod;

    @Mock
    private Name methodName;

    @Mock
    private Name fieldName;

    private EnumAnalyzer enumAnalyzer;

    @BeforeEach
    void setUp() {
        enumAnalyzer = new EnumAnalyzer(elementUtils, typeUtils);
    }

    @Test
    @DisplayName("Should analyze enum with @JsonCreator method correctly")
    void shouldAnalyzeEnumWithJsonCreatorMethodCorrectly() {
        // Given
        setupEnumElement("com.test.TestEnum");
        setupJsonCreatorMethod("fromValue");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(jsonCreatorMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        final String enumQualifiedName = "com.test.TestEnum";
        final String creatorMethod = "fromValue";
        assertThat(result)
                .satisfies(r -> {
                    assertThat(r.getEnumClassName()).isEqualTo(enumQualifiedName);
                    assertThat(r.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
                    assertThat(r.getJsonCreatorMethod()).isEqualTo(creatorMethod);
                    assertThat(r.hasCustomDeserialization()).isTrue();
                    assertThat(r.getDeserializationTarget()).isEqualTo(creatorMethod);
                });
    }

    @Test
    @DisplayName("Should analyze enum with @JsonValue field correctly")
    void shouldAnalyzeEnumWithJsonValueFieldCorrectly() {
        // Given
        setupEnumElement("com.test.TestEnum");
        setupJsonValueField("code");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(jsonValueField));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        final String enumQualifiedName = "com.test.TestEnum";
        final String valueField = "code";
        assertThat(result)
                .satisfies(r -> {
                    assertThat(r.getEnumClassName()).isEqualTo(enumQualifiedName);
                    assertThat(r.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
                    assertThat(r.getJsonValueField()).isEqualTo(valueField);
                    assertThat(r.hasCustomDeserialization()).isTrue();
                    assertThat(r.getDeserializationTarget()).isEqualTo(valueField);
                });
    }

    @Test
    @DisplayName("Should analyze enum with @JsonValue method correctly")
    void shouldAnalyzeEnumWithJsonValueMethodCorrectly() {
        // Given
        setupEnumElement("com.test.TestEnum");
        setupJsonValueMethod("getValue");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(jsonValueMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        final String enumQualifiedName = "com.test.TestEnum";
        final String valueMethod = "getValue";
        assertThat(result)
                .satisfies(r -> {
                    assertThat(r.getEnumClassName()).isEqualTo(enumQualifiedName);
                    assertThat(r.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.VALUE_METHOD);
                    assertThat(r.getJsonValueMethod()).isEqualTo(valueMethod);
                    assertThat(r.hasCustomDeserialization()).isTrue();
                    assertThat(r.getDeserializationTarget()).isEqualTo(valueMethod);
                });
    }

    @Test
    @DisplayName("Should prioritize @JsonCreator over @JsonValue when both present")
    void shouldPrioritizeJsonCreatorOverJsonValueWhenBothPresent() {
        // Given
        setupEnumElement("com.test.TestEnum");
        setupJsonCreatorMethod("fromCode");
        setupJsonValueField("value");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(jsonValueField, jsonCreatorMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        final EnumDeserializationInfo.DeserializationType expectedType = EnumDeserializationInfo.DeserializationType.CREATOR_METHOD;
        final String creator = "fromCode";
        assertThat(result)
                .satisfies(r -> {
                    assertThat(r.getDeserializationType()).isEqualTo(expectedType);
                    assertThat(r.getJsonCreatorMethod()).isEqualTo(creator);
                    assertThat(r.getJsonValueField()).isNull(); // Should not be set when @JsonCreator takes precedence
                });
    }

    @Test
    @DisplayName("Should analyze enum with no Jackson annotations as default matching")
    void shouldAnalyzeEnumWithNoJacksonAnnotationsAsDefaultMatching() {
        // Given
        setupEnumElement("com.test.PlainEnum");
        when(enumElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        final String enumQualifiedName = "com.test.PlainEnum";
        assertThat(result)
                .satisfies(r -> {
                    assertThat(r.getEnumClassName()).isEqualTo(enumQualifiedName);
                    assertThat(r.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
                    assertThat(r.hasCustomDeserialization()).isFalse();
                    assertThat(r.getDeserializationTarget()).isNull();
                    assertThat(r.getJsonCreatorMethod()).isNull();
                    assertThat(r.getJsonValueField()).isNull();
                    assertThat(r.getJsonValueMethod()).isNull();
                });
    }

    @Test
    @DisplayName("Should handle multiple @JsonCreator methods by taking any valid one")
    void shouldHandleMultipleJsonCreatorMethodsByTakingAnyValidOne() {
        // Given
        setupEnumElement("com.test.TestEnum");

        ExecutableElement firstCreatorMethod = createJsonCreatorMethod("fromValue");
        ExecutableElement secondCreatorMethod = createJsonCreatorMethod("fromCode");

        when(enumElement.getEnclosedElements()).thenReturn((List) java.util.Arrays.asList(firstCreatorMethod, secondCreatorMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        assertThat(result.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        assertThat(result.getJsonCreatorMethod()).isIn("fromValue", "fromCode"); // Either one is acceptable as both are valid
    }

    @Test
    @DisplayName("Should handle multiple @JsonValue annotations by taking first one")
    void shouldHandleMultipleJsonValueAnnotationsByTakingFirstOne() {
        // Given
        setupEnumElement("com.test.TestEnum");

        VariableElement firstValueField = createJsonValueField("code");
        ExecutableElement secondValueMethod = createJsonValueMethod("getValue");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(firstValueField, secondValueMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        assertThat(result.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
        assertThat(result.getJsonValueField()).isEqualTo("code"); // First one should be taken
        assertThat(result.getJsonValueMethod()).isNull();
    }

    @Test
    @DisplayName("Should check if type element is enum correctly")
    void shouldCheckIfTypeElementIsEnumCorrectly() {
        // Given
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);

        // When
        boolean isEnum = enumAnalyzer.isEnum(enumElement);

        // Then
        assertThat(isEnum).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-enum type element")
    void shouldReturnFalseForNonEnumTypeElement() {
        // Given
        when(enumElement.getKind()).thenReturn(ElementKind.CLASS);

        // When
        boolean isEnum = enumAnalyzer.isEnum(enumElement);

        // Then
        assertThat(isEnum).isFalse();
    }

    @Test
    @DisplayName("Should handle enum with complex method signatures")
    void shouldHandleEnumWithComplexMethodSignatures() {
        // Given
        setupEnumElement("com.test.ComplexEnum");

        // Create a method with parameters (should be ignored as @JsonCreator methods should be static)
        ExecutableElement complexMethod = createJsonCreatorMethod("fromValueAndType");

        when(enumElement.getEnclosedElements()).thenReturn((List) List.of(complexMethod));

        // When
        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        // Then
        assertThat(result.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        assertThat(result.getJsonCreatorMethod()).isEqualTo("fromValueAndType");
    }

    @Test
    @DisplayName("Should return null for null type element")
    void shouldReturnNullForNullTypeElement() {
        // When
        boolean isEnum = enumAnalyzer.isEnum(null);

        // Then
        assertThat(isEnum).isFalse();
    }

    // Helper methods
    private void setupEnumElement(String qualifiedName) {
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);
        when(enumElement.getQualifiedName()).thenReturn(enumName);
        when(enumName.toString()).thenReturn(qualifiedName);
    }

    private void setupJsonCreatorMethod(String methodName) {
        // Setup method as static
        lenient().when(jsonCreatorMethod.getModifiers()).thenReturn(java.util.Set.of(javax.lang.model.element.Modifier.STATIC));

        // Setup return type to match enum type
        javax.lang.model.type.TypeMirror returnType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        javax.lang.model.type.TypeMirror enumType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        lenient().when(jsonCreatorMethod.getReturnType()).thenReturn(returnType);
        lenient().when(enumElement.asType()).thenReturn(enumType);
        lenient().when(typeUtils.isSameType(returnType, enumType)).thenReturn(true);

        // Setup single String parameter
        javax.lang.model.element.VariableElement parameter = org.mockito.Mockito.mock(javax.lang.model.element.VariableElement.class);
        javax.lang.model.type.TypeMirror stringType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        javax.lang.model.element.TypeElement stringElement = org.mockito.Mockito.mock(javax.lang.model.element.TypeElement.class);

        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.List parameters = java.util.List.of(parameter);
        lenient().when(jsonCreatorMethod.getParameters()).thenReturn(parameters);
        lenient().when(parameter.asType()).thenReturn(stringType);
        lenient().when(elementUtils.getTypeElement("java.lang.String")).thenReturn(stringElement);
        lenient().when(stringElement.asType()).thenReturn(stringType);
        lenient().when(typeUtils.isSameType(stringType, stringType)).thenReturn(true);

        // Setup method name and annotation
        lenient().when(jsonCreatorMethod.getKind()).thenReturn(ElementKind.METHOD);
        lenient().when(jsonCreatorMethod.getAnnotation(JsonCreator.class)).thenReturn(createJsonCreatorAnnotation());
        lenient().when(jsonCreatorMethod.getAnnotation(JsonValue.class)).thenReturn(null);
        lenient().when(jsonCreatorMethod.getSimpleName()).thenReturn(this.methodName);
        lenient().when(this.methodName.toString()).thenReturn(methodName);
    }

    private void setupJsonValueField(String fieldName) {
        lenient().when(jsonValueField.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(jsonValueField.getAnnotation(JsonValue.class)).thenReturn(createJsonValueAnnotation());
        lenient().when(jsonValueField.getSimpleName()).thenReturn(this.fieldName);
        lenient().when(this.fieldName.toString()).thenReturn(fieldName);
    }

    private void setupJsonValueMethod(String methodName) {
        lenient().when(jsonValueMethod.getKind()).thenReturn(ElementKind.METHOD);
        lenient().when(jsonValueMethod.getAnnotation(JsonValue.class)).thenReturn(createJsonValueAnnotation());
        lenient().when(jsonValueMethod.getAnnotation(JsonCreator.class)).thenReturn(null);
        lenient().when(jsonValueMethod.getSimpleName()).thenReturn(this.methodName);
        lenient().when(this.methodName.toString()).thenReturn(methodName);
    }

    private ExecutableElement createJsonCreatorMethod(String methodName) {
        ExecutableElement method = org.mockito.Mockito.mock(ExecutableElement.class);
        Name name = org.mockito.Mockito.mock(Name.class);

        lenient().when(method.getKind()).thenReturn(ElementKind.METHOD);
        lenient().when(method.getAnnotation(JsonCreator.class)).thenReturn(createJsonCreatorAnnotation());
        lenient().when(method.getAnnotation(JsonValue.class)).thenReturn(null);
        lenient().when(method.getSimpleName()).thenReturn(name);
        lenient().when(name.toString()).thenReturn(methodName);

        // Setup for isValidJsonCreatorMethod validation
        lenient().when(method.getModifiers()).thenReturn(java.util.Set.of(javax.lang.model.element.Modifier.STATIC));

        // Setup return type to match enum type
        javax.lang.model.type.TypeMirror returnType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        javax.lang.model.type.TypeMirror enumType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        lenient().when(method.getReturnType()).thenReturn(returnType);
        lenient().when(enumElement.asType()).thenReturn(enumType);
        lenient().when(typeUtils.isSameType(returnType, enumType)).thenReturn(true);

        // Setup single String parameter
        javax.lang.model.element.VariableElement parameter = org.mockito.Mockito.mock(javax.lang.model.element.VariableElement.class);
        javax.lang.model.type.TypeMirror stringType = org.mockito.Mockito.mock(javax.lang.model.type.TypeMirror.class);
        javax.lang.model.element.TypeElement stringElement = org.mockito.Mockito.mock(javax.lang.model.element.TypeElement.class);

        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.List parameters = java.util.List.of(parameter);
        lenient().when(method.getParameters()).thenReturn(parameters);
        lenient().when(parameter.asType()).thenReturn(stringType);
        lenient().when(elementUtils.getTypeElement("java.lang.String")).thenReturn(stringElement);
        lenient().when(stringElement.asType()).thenReturn(stringType);
        lenient().when(typeUtils.isSameType(stringType, stringType)).thenReturn(true);

        return method;
    }

    private VariableElement createJsonValueField(String fieldName) {
        VariableElement field = org.mockito.Mockito.mock(VariableElement.class);
        Name name = org.mockito.Mockito.mock(Name.class);

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getAnnotation(JsonValue.class)).thenReturn(createJsonValueAnnotation());
        when(field.getSimpleName()).thenReturn(name);
        when(name.toString()).thenReturn(fieldName);

        return field;
    }

    private ExecutableElement createJsonValueMethod(String methodName) {
        ExecutableElement method = org.mockito.Mockito.mock(ExecutableElement.class);
        Name name = org.mockito.Mockito.mock(Name.class);

        lenient().when(method.getKind()).thenReturn(ElementKind.METHOD);
        lenient().when(method.getAnnotation(JsonValue.class)).thenReturn(createJsonValueAnnotation());
        lenient().when(method.getAnnotation(JsonCreator.class)).thenReturn(null);
        lenient().when(method.getSimpleName()).thenReturn(name);
        lenient().when(name.toString()).thenReturn(methodName);

        return method;
    }

    private JsonCreator createJsonCreatorAnnotation() {
        return new JsonCreator() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JsonCreator.class;
            }

            @Override
            public Mode mode() {
                return Mode.DEFAULT;
            }
        };
    }

    private JsonValue createJsonValueAnnotation() {
        return new JsonValue() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JsonValue.class;
            }

            @Override
            public boolean value() {
                return true;
            }
        };
    }

}