package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AnnotationValidatorImpl Tests")
class AnnotationValidatorImplTest {

    private AnnotationValidatorImpl validator;

    @Mock
    private VariableElement field;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private Element enclosingElement;

    @BeforeEach
    void setUp() {
        validator = new AnnotationValidatorImpl();
        lenient().when(field.asType()).thenReturn(typeMirror);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingElement);
        lenient().when(enclosingElement.getKind()).thenReturn(ElementKind.CLASS);
    }

    // ===== getSupportedAnnotationTypes =====

    @Test
    void getSupportedAnnotationTypes_returnsAllAnnotations() {
        List<String> supported = validator.getSupportedAnnotationTypes();
        assertThat(supported).isNotEmpty();
        assertThat(supported).contains("com.fasterxml.jackson.annotation.JsonFormat");
        assertThat(supported).contains("com.fasterxml.jackson.annotation.JsonProperty");
        assertThat(supported).contains("com.fasterxml.jackson.annotation.JsonIgnore");
    }

    // ===== validateAnnotation - unsupported type =====

    @Test
    void validateAnnotation_unsupportedAnnotationType_returnsInvalid() {
        AnnotationInfo annotation = new AnnotationInfo("com.example.UnknownAnnotation", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Unsupported annotation type"));
    }

    // ===== validateAnnotation - JsonFormat on temporal type =====

    @Test
    void validateAnnotation_jsonFormatOnLocalDateTime_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("pattern", "yyyy-MM-dd HH:mm:ss"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validateAnnotation_jsonFormatOnLocalDate_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("pattern", "yyyy-MM-dd"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDate");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonFormatOnInstant_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("pattern", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        when(typeMirror.toString()).thenReturn("java.time.Instant");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonFormatOnInteger_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.Integer");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonFormatOnString_hasWarning() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        // JsonFormat on String is not temporal or numeric - should warn
        assertThat(result.getWarnings()).isNotEmpty();
    }

    @Test
    void validateAnnotation_jsonFormatEmptyPattern_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("pattern", "  "));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("@JsonFormat pattern cannot be empty"));
    }

    @Test
    void validateAnnotation_jsonFormatInvalidPattern_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("pattern", "not-a-valid-pattern-!@#$"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Invalid @JsonFormat pattern"));
    }

    @Test
    void validateAnnotation_jsonFormatValidTimezone_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("timezone", "Europe/Istanbul"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonFormatInvalidTimezone_hasWarning() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("timezone", "Invalid/Timezone/XYZ"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Timezone") && w.contains("Invalid/Timezone/XYZ"));
    }

    @Test
    void validateAnnotation_jsonFormatValidLocale_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("locale", "tr_TR"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonFormatInvalidLocale_hasWarning() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat",
                Map.of("locale", "1234"));
        when(typeMirror.toString()).thenReturn("java.time.LocalDateTime");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Locale"));
    }

    // ===== validateAnnotation - JsonProperty =====

    @Test
    void validateAnnotation_jsonPropertyWithValidName_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("value", "my_field_name"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonPropertyWithEmptyName_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("value", "  "));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("@JsonProperty value cannot be empty"));
    }

    @Test
    void validateAnnotation_jsonPropertyWithInvalidAccess_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("access", "INVALID_ACCESS"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Invalid @JsonProperty access type"));
    }

    @Test
    void validateAnnotation_jsonPropertyWithReadOnly_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("access", "READ_ONLY"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonPropertyWithWriteOnly_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("access", "WRITE_ONLY"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonPropertyWithReadWrite_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("access", "READ_WRITE"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonPropertyWithSpecialChars_hasWarning() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty",
                Map.of("value", "field\\name"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("special characters"));
    }

    // ===== validateAnnotation - JsonDeserialize =====

    @Test
    void validateAnnotation_jsonDeserializeWithValidClass_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                Map.of("using", "com.example.MyDeserializer"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonDeserializeWithInvalidClass_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                Map.of("using", "123InvalidClass"));
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Invalid deserializer class name"));
    }

    @Test
    void validateAnnotation_jsonDeserializeWithValidAs_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                Map.of("as", "java.util.ArrayList"));
        when(typeMirror.toString()).thenReturn("java.util.List");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonDeserializeWithInvalidAs_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                Map.of("as", "  "));
        when(typeMirror.toString()).thenReturn("java.util.List");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Invalid target class name"));
    }

    // ===== validateAnnotation - JsonTypeInfo =====

    @Test
    void validateAnnotation_jsonTypeInfoWithoutUse_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("@JsonTypeInfo requires 'use' parameter"));
    }

    @Test
    void validateAnnotation_jsonTypeInfoWithValidInclude_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo",
                Map.of("use", "NAME", "include", "PROPERTY"));
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonTypeInfoWithInvalidInclude_hasError() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo",
                Map.of("use", "NAME", "include", "INVALID_INCLUDE"));
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Invalid @JsonTypeInfo include type"));
    }

    @Test
    void validateAnnotation_jsonTypeInfoWrapperObject_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo",
                Map.of("use", "NAME", "include", "WRAPPER_OBJECT"));
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonTypeInfoWrapperArray_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo",
                Map.of("use", "NAME", "include", "WRAPPER_ARRAY"));
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotation_jsonTypeInfoExternalProperty_isValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo",
                Map.of("use", "NAME", "include", "EXTERNAL_PROPERTY"));
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        AnnotationValidator.ValidationResult result = validator.validateAnnotation(field, annotation);

        assertThat(result.isValid()).isTrue();
    }

    // ===== validateAnnotation - JsonIgnore =====

    @Test
    void validateAnnotation_jsonIgnore_onAnyField_isSupported() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonIgnore", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean supported = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(supported).isTrue();
    }

    // ===== validateAnnotation - JsonValue =====

    @Test
    void validateAnnotation_jsonValue_onString_isSupported() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonValue", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean supported = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(supported).isTrue();
    }

    @Test
    void validateAnnotation_jsonValue_onInteger_isSupported() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonValue", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.Integer");

        boolean supported = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(supported).isTrue();
    }

    @Test
    void validateAnnotation_jsonValue_onObject_isNotSupported() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonValue", Map.of());
        when(typeMirror.toString()).thenReturn("com.example.MyObject");

        boolean supported = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(supported).isFalse();
    }

    // ===== validateAnnotationParameters - null params =====

    @Test
    void validateAnnotationParameters_nullParams_returnsValid() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", null);

        AnnotationValidator.ValidationResult result = validator.validateAnnotationParameters(annotation);

        assertThat(result.isValid()).isTrue();
    }

    // ===== validateAnnotationCombination =====

    @Test
    void validateAnnotationCombination_emptyList_returnsValid() {
        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field, List.of());
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotationCombination_nullList_returnsValid() {
        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field, null);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateAnnotationCombination_duplicateAnnotation_hasError() {
        AnnotationInfo annotation1 = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        AnnotationInfo annotation2 = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.time.LocalDate");

        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field,
                List.of(annotation1, annotation2));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Duplicate annotation"));
    }

    @Test
    void validateAnnotationCombination_jsonIgnoreWithOthers_hasWarning() {
        AnnotationInfo ignore = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonIgnore", Map.of());
        AnnotationInfo property = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field,
                List.of(ignore, property));

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("@JsonIgnore"));
    }

    @Test
    void validateAnnotationCombination_jsonFormatAndDeserialize_hasWarning() {
        AnnotationInfo format = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        AnnotationInfo deser = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize", Map.of());
        when(typeMirror.toString()).thenReturn("java.time.LocalDate");

        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field,
                List.of(format, deser));

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("@JsonFormat and @JsonDeserialize"));
    }

    @Test
    void validateAnnotationCombination_jsonCreatorOnField_hasError() {
        AnnotationInfo creator = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonCreator", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field,
                List.of(creator));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("@JsonCreator"));
    }

    @Test
    void validateAnnotationCombination_jsonValueOnNonEnum_hasWarning() {
        AnnotationInfo value = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonValue", Map.of());
        when(typeMirror.toString()).thenReturn("com.example.SomeObject");

        AnnotationValidator.ValidationResult result = validator.validateAnnotationCombination(field,
                List.of(value));

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("@JsonValue"));
    }

    // ===== isAnnotationSupportedForFieldType =====

    @Test
    void isAnnotationSupportedForFieldType_jsonFormatOnDate_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.util.Date");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonFormatOnString_returnsFalse() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonPropertyOnAny_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonProperty", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonIgnoreOnAny_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonIgnore", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.Object");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonDeserializeOnAny_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.databind.annotation.JsonDeserialize", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonValueOnLong_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonValue", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.Long");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonTypeInfoOnObject_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo", Map.of());
        when(typeMirror.toString()).thenReturn("com.example.SomeObject");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonTypeInfoOnString_returnsFalse() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonTypeInfo", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        // String is not an object type (it's a STRING type), so JsonTypeInfo doesn't apply
        assertThat(result).isFalse();
    }

    @Test
    void isAnnotationSupportedForFieldType_unknownAnnotation_returnsFalse() {
        AnnotationInfo annotation = new AnnotationInfo("com.example.UnknownAnnotation", Map.of());
        when(typeMirror.toString()).thenReturn("java.lang.String");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonFormatOnSqlDate_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("java.sql.Date");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void isAnnotationSupportedForFieldType_jsonFormatOnDouble_returnsTrue() {
        AnnotationInfo annotation = new AnnotationInfo("com.fasterxml.jackson.annotation.JsonFormat", Map.of());
        when(typeMirror.toString()).thenReturn("double");

        boolean result = validator.isAnnotationSupportedForFieldType(field, annotation);

        assertThat(result).isTrue();
    }
}
