package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


/**
 * Comprehensive unit tests for DateTimeFormatAnalyzer.
 * Tests datetime format analysis with custom and default patterns.
 * <p>
 * Requirements tested: 1.1, 1.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DateTime Format Analyzer Comprehensive Tests")
class DateTimeFormatAnalyzerComprehensiveTest {

    private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_TIME_INSTANT = "java.time.Instant";
    private static final String PATTERN_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String JAVA_LANG_STRING = "java.lang.String";

    private DateTimeFormatAnalyzerImpl analyzer;

    @Mock
    private VariableElement mockField;

    @Mock
    private TypeMirror mockTypeMirror;

    @BeforeEach
    void setUp() {
        analyzer = new DateTimeFormatAnalyzerImpl();
    }
    @ParameterizedTest
    @DisplayName("Should analyze temporal field with custom @JsonFormat pattern")
    @MethodSource("customJsonFormatPatternProvider")
    void shouldAnalyzeTemporalFieldWithCustomJsonFormatPattern(String fieldType, String customPattern) {
        // Given
        setupTemporalField(fieldType, customPattern);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(fieldType);
        assertThat(result.getPattern()).isEqualTo(customPattern);
        assertThat(result.isHasCustomFormat()).isTrue();
        assertThat(result.usesCustomFormat()).isTrue();
        assertThat(result.getEffectivePattern()).isEqualTo(customPattern);
    }

    private static Stream<Arguments> customJsonFormatPatternProvider() {
        return Stream.of(
                Arguments.of(JAVA_TIME_LOCAL_DATE_TIME, "dd/MM/yyyy HH:mm:ss"),
                Arguments.of(JAVA_TIME_LOCAL_DATE, "dd-MM-yyyy"),
                Arguments.of(JAVA_TIME_INSTANT, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        );
    }


    @Test
    @DisplayName("Should analyze LocalDateTime field with default pattern when no @JsonFormat")
    void shouldAnalyzeLocalDateTimeFieldWithDefaultPatternWhenNoJsonFormat() {
        // Given
        setupTemporalFieldWithoutAnnotation(JAVA_TIME_LOCAL_DATE_TIME);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should analyze LocalDate field with default pattern when no @JsonFormat")
    void shouldAnalyzeLocalDateFieldWithDefaultPatternWhenNoJsonFormat() {
        // Given
        setupTemporalFieldWithoutAnnotation(JAVA_TIME_LOCAL_DATE);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    @DisplayName("Should analyze Instant field with default pattern when no @JsonFormat")
    void shouldAnalyzeInstantFieldWithDefaultPatternWhenNoJsonFormat() {
        // Given
        setupTemporalFieldWithoutAnnotation(JAVA_TIME_INSTANT);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(JAVA_TIME_INSTANT);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should handle @JsonFormat with empty pattern by using default")
    void shouldHandleJsonFormatWithEmptyPatternByUsingDefault() {
        // Given
        setupTemporalField(JAVA_TIME_LOCAL_DATE_TIME, ""); // Empty pattern

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should handle @JsonFormat with whitespace-only pattern by using default")
    void shouldHandleJsonFormatWithWhitespaceOnlyPatternByUsingDefault() {
        // Given
        setupTemporalField(JAVA_TIME_LOCAL_DATE_TIME, "   "); // Whitespace-only pattern

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should handle complex datetime patterns with special characters")
    void shouldHandleComplexDatetimePatternsWithSpecialCharacters() {
        // Given
        String complexPattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        setupTemporalField(JAVA_TIME_LOCAL_DATE_TIME, complexPattern);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPattern()).isEqualTo(complexPattern);
        assertThat(result.isHasCustomFormat()).isTrue();
        assertThat(result.usesCustomFormat()).isTrue();
        assertThat(result.getEffectivePattern()).isEqualTo(complexPattern);
    }

    @Test
    @DisplayName("Should handle localized datetime patterns")
    void shouldHandleLocalizedDatetimePatterns() {
        // Given
        String localizedPattern = "dd.MM.yyyy HH:mm:ss";
        setupTemporalField(JAVA_TIME_LOCAL_DATE_TIME, localizedPattern);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPattern()).isEqualTo(localizedPattern);
        assertThat(result.isHasCustomFormat()).isTrue();
        assertThat(result.usesCustomFormat()).isTrue();
        assertThat(result.getEffectivePattern()).isEqualTo(localizedPattern);
    }

    @Test
    @DisplayName("Should validate datetime format info with valid pattern")
    void shouldValidateDatetimeFormatInfoWithValidPattern() {
        // Given
        DateTimeFormatInfo validInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        validInfo.setPattern(PATTERN_YYYY_MM_DD_HH_MM_SS);

        // When
        boolean isValid = analyzer.isValidDateTimeFormatInfo(validInfo);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate datetime format info with null pattern")
    void shouldInvalidateDatetimeFormatInfoWithNullPattern() {
        // Given
        DateTimeFormatInfo invalidInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        invalidInfo.setPattern(null);

        // When
        boolean isValid = analyzer.isValidDateTimeFormatInfo(invalidInfo);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate null datetime format info")
    void shouldInvalidateNullDatetimeFormatInfo() {
        // When
        boolean isValid = analyzer.isValidDateTimeFormatInfo(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should create custom format info correctly")
    void shouldCreateCustomFormatInfoCorrectly() {
        // Given
        String fieldType = JAVA_TIME_LOCAL_DATE;
        String customPattern = "MM/dd/yyyy";

        // When
        DateTimeFormatInfo result = analyzer.createCustomFormatInfo(fieldType, customPattern);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(fieldType);
        assertThat(result.getPattern()).isEqualTo(customPattern);
        assertThat(result.isHasCustomFormat()).isTrue();
        assertThat(result.usesCustomFormat()).isTrue();
        assertThat(result.getEffectivePattern()).isEqualTo(customPattern);
    }

    @Test
    @DisplayName("Should create default format info correctly")
    void shouldCreateDefaultFormatInfoCorrectly() {
        // Given
        String fieldType = JAVA_TIME_INSTANT;

        // When
        DateTimeFormatInfo result = analyzer.createDefaultFormatInfo(fieldType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldType()).isEqualTo(fieldType);
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(result.isHasCustomFormat()).isFalse();
        assertThat(result.usesCustomFormat()).isFalse();
        assertThat(result.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should extract custom pattern from @JsonFormat annotation")
    void shouldExtractCustomPatternFromJsonFormatAnnotation() {
        // Given
        String customPattern = "yyyy/MM/dd HH:mm:ss";
        JsonFormat jsonFormat = createJsonFormatAnnotation(customPattern);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(jsonFormat);

        // When
        String result = analyzer.extractCustomPattern(mockField);

        // Then
        assertThat(result).isEqualTo(customPattern);
    }

    @Test
    @DisplayName("Should return null when extracting pattern from field without @JsonFormat")
    void shouldReturnNullWhenExtractingPatternFromFieldWithoutJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        String result = analyzer.extractCustomPattern(mockField);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should check if field has custom datetime format correctly")
    void shouldCheckIfFieldHasCustomDatetimeFormatCorrectly() {
        // Given
        JsonFormat jsonFormat = createJsonFormatAnnotation(PATTERN_YYYY_MM_DD_HH_MM_SS);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(jsonFormat);

        // When
        boolean hasCustomFormat = analyzer.hasCustomDateTimeFormat(mockField);

        // Then
        assertThat(hasCustomFormat).isTrue();
    }

    @Test
    @DisplayName("Should return false for custom format when @JsonFormat has empty pattern")
    void shouldReturnFalseForCustomFormatWhenJsonFormatHasEmptyPattern() {
        // Given
        JsonFormat jsonFormat = createJsonFormatAnnotation("");
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(jsonFormat);

        // When
        boolean hasCustomFormat = analyzer.hasCustomDateTimeFormat(mockField);

        // Then
        assertThat(hasCustomFormat).isFalse();
    }

    @Test
    @DisplayName("Should get correct default pattern for each temporal type")
    void shouldGetCorrectDefaultPatternForEachTemporalType() {
        // When & Then
        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_LOCAL_DATE_TIME))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_LOCAL_DATE))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_INSTANT))
                .isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);

        // Unknown types should default to LocalDateTime pattern
        assertThat(analyzer.getDefaultPatternForType("java.time.ZonedDateTime"))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(JAVA_LANG_STRING))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(null))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should identify temporal fields correctly")
    void shouldIdentifyTemporalFieldsCorrectly() {
        // Given & When & Then
        setupFieldType(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(analyzer.isTemporalField(mockField)).isTrue();

        setupFieldType(JAVA_TIME_LOCAL_DATE);
        assertThat(analyzer.isTemporalField(mockField)).isTrue();

        setupFieldType(JAVA_TIME_INSTANT);
        assertThat(analyzer.isTemporalField(mockField)).isTrue();

        setupFieldType(JAVA_LANG_STRING);
        assertThat(analyzer.isTemporalField(mockField)).isFalse();

        setupFieldType("java.lang.Integer");
        assertThat(analyzer.isTemporalField(mockField)).isFalse();

        setupFieldType("java.util.Date");
        assertThat(analyzer.isTemporalField(mockField)).isFalse();

        assertThat(analyzer.isTemporalField(null)).isFalse();
    }

    @Test
    @DisplayName("Should handle @JsonFormat with timezone information")
    void shouldHandleJsonFormatWithTimezoneInformation() {
        // Given
        JsonFormat jsonFormat = createJsonFormatWithTimezone(PATTERN_YYYY_MM_DD_HH_MM_SS, "UTC");
        setupTemporalFieldWithAnnotation(JAVA_TIME_LOCAL_DATE_TIME, jsonFormat);

        // When
        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPattern()).isEqualTo(PATTERN_YYYY_MM_DD_HH_MM_SS);
        assertThat(result.isHasCustomFormat()).isTrue();
        // Note: Timezone handling might be extended in future versions
    }

    // Helper methods
    private void setupTemporalField(String fieldType, String pattern) {
        JsonFormat jsonFormat = createJsonFormatAnnotation(pattern);
        setupTemporalFieldWithAnnotation(fieldType, jsonFormat);
    }

    private void setupTemporalFieldWithoutAnnotation(String fieldType) {
        setupFieldType(fieldType);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);
    }

    private void setupTemporalFieldWithAnnotation(String fieldType, JsonFormat jsonFormat) {
        setupFieldType(fieldType);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(jsonFormat);
    }

    private void setupFieldType(String fieldType) {
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(fieldType);
    }

    private JsonFormat createJsonFormatAnnotation(String pattern) {
        return new JsonFormat() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JsonFormat.class;
            }

            @Override
            public String pattern() {
                return pattern;
            }

            @Override
            public Shape shape() {
                return Shape.ANY;
            }

            @Override
            public String locale() {
                return "";
            }

            @Override
            public String timezone() {
                return "";
            }

            @Override
            public OptBoolean lenient() {
                return OptBoolean.DEFAULT;
            }

            @Override
            public Feature[] with() {
                return new Feature[0];
            }

            @Override
            public Feature[] without() {
                return new Feature[0];
            }
        };
    }

    private JsonFormat createJsonFormatWithTimezone(String pattern, String timezone) {
        return new JsonFormat() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JsonFormat.class;
            }

            @Override
            public String pattern() {
                return pattern;
            }

            @Override
            public Shape shape() {
                return Shape.ANY;
            }

            @Override
            public String locale() {
                return "";
            }

            @Override
            public String timezone() {
                return timezone;
            }

            @Override
            public OptBoolean lenient() {
                return OptBoolean.DEFAULT;
            }

            @Override
            public Feature[] with() {
                return new Feature[0];
            }

            @Override
            public Feature[] without() {
                return new Feature[0];
            }
        };
    }
}