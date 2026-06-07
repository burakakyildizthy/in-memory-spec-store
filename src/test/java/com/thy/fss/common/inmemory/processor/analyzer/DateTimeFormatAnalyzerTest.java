package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * Unit tests for DateTimeFormatAnalyzer implementation.
 * Tests datetime format analysis for various field types and @JsonFormat configurations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DateTimeFormatAnalyzer Tests")
class DateTimeFormatAnalyzerTest {

    private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_TIME_INSTANT = "java.time.Instant";
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
    @DisplayName("Should identify temporal field types")
    @MethodSource("temporalFieldTypeProvider")
    void shouldIdentifyTemporalFieldTypes(String fieldType) {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(fieldType);

        // When
        boolean isTemporalField = analyzer.isTemporalField(mockField);

        // Then
        assertThat(isTemporalField).isTrue();
    }

    private static Stream<Arguments> temporalFieldTypeProvider() {
        return Stream.of(
                Arguments.of(JAVA_TIME_LOCAL_DATE_TIME),
                Arguments.of(JAVA_TIME_LOCAL_DATE),
                Arguments.of(JAVA_TIME_INSTANT)
        );
    }

    @Test
    @DisplayName("Should not identify String fields as temporal")
    void shouldNotIdentifyStringAsTemporalField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(JAVA_LANG_STRING);

        // When
        boolean isTemporalField = analyzer.isTemporalField(mockField);

        // Then
        assertThat(isTemporalField).isFalse();
    }

    @Test
    @DisplayName("Should not identify Integer fields as temporal")
    void shouldNotIdentifyIntegerAsTemporalField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.lang.Integer");

        // When
        boolean isTemporalField = analyzer.isTemporalField(mockField);

        // Then
        assertThat(isTemporalField).isFalse();
    }

    @Test
    @DisplayName("Should return null for null field")
    void shouldReturnNullForNullField() {
        // When
        boolean isTemporalField = analyzer.isTemporalField(null);

        // Then
        assertThat(isTemporalField).isFalse();
    }
    
    @ParameterizedTest
    @DisplayName("Should return correct field type for temporal types")
    @MethodSource("fieldTypeProvider")
    void shouldReturnCorrectFieldTypeForTemporalTypes(String fieldType) {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(fieldType);

        // When
        String result = analyzer.getFieldType(mockField);

        // Then
        assertThat(result).isEqualTo(fieldType);
    }

    private static Stream<Arguments> fieldTypeProvider() {
        return Stream.of(
                Arguments.of(JAVA_TIME_LOCAL_DATE_TIME),
                Arguments.of(JAVA_TIME_LOCAL_DATE),
                Arguments.of(JAVA_TIME_INSTANT)
        );
    }

    @Test
    @DisplayName("Should return null for null field when getting field type")
    void shouldReturnNullForNullFieldWhenGettingFieldType() {
        // When
        String fieldType = analyzer.getFieldType(null);

        // Then
        assertThat(fieldType).isNull();
    }

    @Test
    @DisplayName("Should return default LocalDate pattern")
    void shouldReturnDefaultLocalDatePattern() {
        // When
        String pattern = analyzer.getDefaultPatternForType(JAVA_TIME_LOCAL_DATE);

        // Then
        assertThat(pattern).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    @DisplayName("Should return default Instant pattern")
    void shouldReturnDefaultInstantPattern() {
        // When
        String pattern = analyzer.getDefaultPatternForType(JAVA_TIME_INSTANT);

        // Then
        assertThat(pattern).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @ParameterizedTest
    @DisplayName("Should return default LocalDateTime pattern for various input types")
    @MethodSource("localDateTimePatternProvider")
    void shouldReturnDefaultLocalDateTimePatternForVariousInputTypes(String inputType) {
        // When
        String pattern = analyzer.getDefaultPatternForType(inputType);

        // Then
        assertThat(pattern).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    private static Stream<Arguments> localDateTimePatternProvider() {
        return Stream.of(
                Arguments.of(JAVA_TIME_LOCAL_DATE_TIME),
                Arguments.of(JAVA_LANG_STRING),
                Arguments.of((String) null)
        );
    }
    

    @Test
    @DisplayName("Should return null when analyzing non-temporal field")
    void shouldReturnNullWhenAnalyzingNonTemporalField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(JAVA_LANG_STRING);

        // When
        DateTimeFormatInfo info = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(info).isNull();
    }

    @Test
    @DisplayName("Should return null when analyzing null field")
    void shouldReturnNullWhenAnalyzingNullField() {
        // When
        DateTimeFormatInfo info = analyzer.analyzeDateTimeField(null);

        // Then
        assertThat(info).isNull();
    }

    @Test
    @DisplayName("Should create DateTimeFormatInfo with default pattern for LocalDateTime field without @JsonFormat")
    void shouldCreateDateTimeFormatInfoWithDefaultPatternForLocalDateTimeField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(JAVA_TIME_LOCAL_DATE_TIME);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        DateTimeFormatInfo info = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should create DateTimeFormatInfo with default pattern for LocalDate field without @JsonFormat")
    void shouldCreateDateTimeFormatInfoWithDefaultPatternForLocalDateField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(JAVA_TIME_LOCAL_DATE);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        DateTimeFormatInfo info = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE);
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    @DisplayName("Should create DateTimeFormatInfo with default pattern for Instant field without @JsonFormat")
    void shouldCreateDateTimeFormatInfoWithDefaultPatternForInstantField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(JAVA_TIME_INSTANT);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        DateTimeFormatInfo info = analyzer.analyzeDateTimeField(mockField);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getFieldType()).isEqualTo(JAVA_TIME_INSTANT);
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should validate DateTimeFormatInfo correctly")
    void shouldValidateDateTimeFormatInfoCorrectly() {
        // Given
        DateTimeFormatInfo validInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        validInfo.setPattern("yyyy-MM-dd HH:mm:ss");

        DateTimeFormatInfo nullInfo = null;

        DateTimeFormatInfo nullPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        nullPatternInfo.setPattern(null);

        DateTimeFormatInfo emptyPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        emptyPatternInfo.setPattern("");

        DateTimeFormatInfo blankPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        blankPatternInfo.setPattern("   ");

        // When & Then
        assertThat(analyzer.isValidDateTimeFormatInfo(validInfo)).isTrue();
        assertThat(analyzer.isValidDateTimeFormatInfo(nullInfo)).isFalse();
        assertThat(analyzer.isValidDateTimeFormatInfo(nullPatternInfo)).isFalse();
        assertThat(analyzer.isValidDateTimeFormatInfo(emptyPatternInfo)).isFalse();
        assertThat(analyzer.isValidDateTimeFormatInfo(blankPatternInfo)).isFalse();
    }

    @Test
    @DisplayName("Should create custom format info correctly")
    void shouldCreateCustomFormatInfoCorrectly() {
        // When
        DateTimeFormatInfo info = analyzer.createCustomFormatInfo(JAVA_TIME_LOCAL_DATE_TIME, "dd/MM/yyyy HH:mm:ss");

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(info.getPattern()).isEqualTo("dd/MM/yyyy HH:mm:ss");
        assertThat(info.isHasCustomFormat()).isTrue();
        assertThat(info.usesCustomFormat()).isTrue();
        assertThat(info.getEffectivePattern()).isEqualTo("dd/MM/yyyy HH:mm:ss");
    }

    @Test
    @DisplayName("Should create default format info correctly")
    void shouldCreateDefaultFormatInfoCorrectly() {
        // When
        DateTimeFormatInfo info = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE);
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    @DisplayName("Should return false for hasCustomDateTimeFormat when field has no @JsonFormat")
    void shouldReturnFalseForHasCustomDateTimeFormatWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        boolean hasCustomFormat = analyzer.hasCustomDateTimeFormat(mockField);

        // Then
        assertThat(hasCustomFormat).isFalse();
    }

    @Test
    @DisplayName("Should return false for hasCustomDateTimeFormat when field is null")
    void shouldReturnFalseForHasCustomDateTimeFormatWhenFieldIsNull() {
        // When
        boolean hasCustomFormat = analyzer.hasCustomDateTimeFormat(null);

        // Then
        assertThat(hasCustomFormat).isFalse();
    }

    @Test
    @DisplayName("Should return null for extractCustomPattern when field has no @JsonFormat")
    void shouldReturnNullForExtractCustomPatternWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        String pattern = analyzer.extractCustomPattern(mockField);

        // Then
        assertThat(pattern).isNull();
    }

    @Test
    @DisplayName("Should return null for extractCustomPattern when field is null")
    void shouldReturnNullForExtractCustomPatternWhenFieldIsNull() {
        // When
        String pattern = analyzer.extractCustomPattern(null);

        // Then
        assertThat(pattern).isNull();
    }
}