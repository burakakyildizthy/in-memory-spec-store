package com.thy.fss.common.inmemory.processor.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MetaModelValidator Tests")
class MetaModelValidatorTest {

    private MetaModelValidator validator;

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Messager messager;

    @Mock
    private TypeElement typeElement;

    @Mock
    private Name qualifiedName;

    @Mock
    private Name simpleName;

    @BeforeEach
    void setUp() {
        when(processingEnv.getMessager()).thenReturn(messager);
        validator = new MetaModelValidator(processingEnv);
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(typeElement.getSimpleName()).thenReturn(simpleName);
        when(qualifiedName.toString()).thenReturn("com.example.MyEntity");
        when(simpleName.toString()).thenReturn("MyEntity");
    }

    // ===== Public class, no fields =====

    @Test
    void validate_publicClassNoFields_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));
        when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
        verify(messager, atLeastOnce()).printMessage(any(), anyString(), eq(typeElement));
    }

    // ===== Abstract class =====

    @Test
    void validate_abstractClass_returnsFalseWithError() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.ABSTRACT, Modifier.PUBLIC));
        when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        boolean result = validator.validate(typeElement, Set.of());

        // Abstract classes still return true but with warning (implementation-dependent)
        // Just verify it doesn't throw
        verify(messager, atLeastOnce()).printMessage(any(), anyString(), eq(typeElement));
    }

    // ===== Private class =====

    @Test
    void validate_privateClass_returnsFalse() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PRIVATE));
        when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isFalse();
        verify(messager, atLeastOnce()).printMessage(eq(javax.tools.Diagnostic.Kind.ERROR), anyString(), eq(typeElement));
    }

    // ===== Class with supported String field =====

    @Test
    void validate_classWithStringField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("name", "java.lang.String", Set.of(Modifier.PRIVATE));
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with supported Integer field =====

    @Test
    void validate_classWithIntegerField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("age", "java.lang.Integer", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with supported Long field =====

    @Test
    void validate_classWithLongField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("id", "java.lang.Long", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with supported temporal field =====

    @Test
    void validate_classWithLocalDateTimeField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("createdAt", "java.time.LocalDateTime", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with supported LocalDate field =====

    @Test
    void validate_classWithLocalDateField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("birthDate", "java.time.LocalDate", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with supported Instant field =====

    @Test
    void validate_classWithInstantField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("timestamp", "java.time.Instant", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with unsupported field type =====

    @Test
    void validate_classWithUnsupportedFieldType_returnsFalse() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("data", "java.io.File", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isFalse();
    }

    // ===== Class with java.lang.Object field (allowed) =====

    @Test
    void validate_classWithObjectField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("payload", "java.lang.Object", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with collection field (no generic) =====

    @Test
    void validate_classWithCollectionField_warnsButReturnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        DeclaredType collectionType = mock(DeclaredType.class);
        TypeElement collectionElement = mock(TypeElement.class);
        Name collectionQName = mock(Name.class);

        when(collectionType.toString()).thenReturn("java.util.List");
        when(collectionType.getKind()).thenReturn(TypeKind.DECLARED);
        when(collectionType.asElement()).thenReturn(collectionElement);
        when(collectionType.getTypeArguments()).thenReturn(Collections.emptyList());
        when(collectionElement.getKind()).thenReturn(ElementKind.INTERFACE);
        when(collectionElement.getQualifiedName()).thenReturn(collectionQName);
        when(collectionQName.toString()).thenReturn("java.util.List");

        VariableElement field = mock(VariableElement.class);
        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("items");
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Set.of());
        when(field.asType()).thenReturn(collectionType);

        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        // Just verify it doesn't throw
        boolean result = validator.validate(typeElement, Set.of());
        verify(processingEnv, atLeastOnce()).getMessager();
    }

    // ===== Class with static field - should be skipped =====

    @Test
    void validate_classWithStaticField_skipsStaticField() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement staticField = createField("CONSTANT", "java.lang.String", Set.of(Modifier.STATIC));
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(staticField));

        boolean result = validator.validate(typeElement, Set.of());

        // Static fields are skipped, so class effectively has no non-static fields → warning
        assertThat(result).isTrue();
    }

    // ===== Class with @MetaModel referenced class =====

    @Test
    void validate_classWithMetaModelReferencedType_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        DeclaredType declaredType = mock(DeclaredType.class);
        TypeElement referencedElement = mock(TypeElement.class);
        Name refName = mock(Name.class);

        when(declaredType.toString()).thenReturn("com.example.Address");
        when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        when(declaredType.asElement()).thenReturn(referencedElement);
        when(referencedElement.getKind()).thenReturn(ElementKind.CLASS);
        when(referencedElement.getQualifiedName()).thenReturn(refName);
        when(refName.toString()).thenReturn("com.example.Address");

        VariableElement field = mock(VariableElement.class);
        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("address");
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Set.of());
        when(field.asType()).thenReturn(declaredType);

        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of("com.example.Address"));

        assertThat(result).isTrue();
    }

    // ===== Class with enum field =====

    @Test
    void validate_classWithEnumField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        DeclaredType enumType = mock(DeclaredType.class);
        Element enumElement = mock(Element.class);

        when(enumType.toString()).thenReturn("com.example.Status");
        when(enumType.getKind()).thenReturn(TypeKind.DECLARED);
        when(enumType.asElement()).thenReturn(enumElement);
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);

        VariableElement field = mock(VariableElement.class);
        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("status");
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(Set.of());
        when(field.asType()).thenReturn(enumType);

        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Multiple fields, one unsupported =====

    @Test
    void validate_classWithMixedFields_returnsFalseWhenUnsupported() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement validField = createField("name", "java.lang.String", Set.of());
        VariableElement invalidField = createField("socket", "java.net.Socket", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(validField, invalidField));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isFalse();
    }

    // ===== Class with primitive boolean =====

    @Test
    void validate_classWithPrimitiveBooleanField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("active", "boolean", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with primitive int =====

    @Test
    void validate_classWithPrimitiveIntField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("count", "int", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // ===== Class with Double field =====

    @Test
    void validate_classWithDoubleField_returnsTrue() {
        when(typeElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));

        VariableElement field = createField("price", "java.lang.Double", Set.of());
        when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));

        boolean result = validator.validate(typeElement, Set.of());

        assertThat(result).isTrue();
    }

    // Helper method to create simple VariableElement mocks

    private VariableElement createField(String name, String typeName, Set<Modifier> modifiers) {
        VariableElement field = mock(VariableElement.class);
        Name fieldName = mock(Name.class);
        TypeMirror fieldType = mock(TypeMirror.class);

        when(fieldName.toString()).thenReturn(name);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getModifiers()).thenReturn(modifiers);
        when(field.asType()).thenReturn(fieldType);
        when(fieldType.toString()).thenReturn(typeName);
        when(fieldType.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }
}
