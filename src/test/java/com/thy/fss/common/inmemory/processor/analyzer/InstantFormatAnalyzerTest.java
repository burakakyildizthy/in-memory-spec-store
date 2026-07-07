package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.InstantFormatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InstantFormatAnalyzer implementation.
 * Tests Instant format analysis for various field configurations and @JsonFormat annotations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstantFormatAnalyzer Tests")
class InstantFormatAnalyzerTest {

    private InstantFormatAnalyzerImpl analyzer;

    @Mock
    private VariableElement mockField;

    @Mock
    private TypeMirror mockTypeMirror;

    @BeforeEach
    void setUp() {
        analyzer = new InstantFormatAnalyzerImpl();
    }

    @Test
    @DisplayName("Should identify Instant fields correctly")
    void shouldIdentifyInstantFieldsCorrectly() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.time.Instant");

        // When
        boolean isInstantField = analyzer.isInstantField(mockField);

        // Then
        assertThat(isInstantField).isTrue();
    }

    @Test
    @DisplayName("Should not identify LocalDateTime fields as Instant")
    void shouldNotIdentifyLocalDateTimeAsInstant() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.time.LocalDateTime");

        // When
        boolean isInstantField = analyzer.isInstantField(mockField);

        // Then
        assertThat(isInstantField).isFalse();
    }

    @Test
    @DisplayName("Should not identify String fields as Instant")
    void shouldNotIdentifyStringAsInstant() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.lang.String");

        // When
        boolean isInstantField = analyzer.isInstantField(mockField);

        // Then
        assertThat(isInstantField).isFalse();
    }

    @Test
    @DisplayName("Should return false for null field")
    void shouldReturnFalseForNullField() {
        // When
        boolean isInstantField = analyzer.isInstantField(null);

        // Then
        assertThat(isInstantField).isFalse();
    }

    @Test
    @DisplayName("Should return default Instant pattern")
    void shouldReturnDefaultInstantPattern() {
        // When
        String pattern = analyzer.getDefaultInstantPattern();

        // Then
        assertThat(pattern).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should return null when analyzing non-Instant field")
    void shouldReturnNullWhenAnalyzingNonInstantField() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.lang.String");

        // When
        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        // Then
        assertThat(info).isNull();
    }

    @Test
    @DisplayName("Should return null when analyzing null field")
    void shouldReturnNullWhenAnalyzingNullField() {
        // When
        InstantFormatInfo info = analyzer.analyzeInstantField(null);

        // Then
        assertThat(info).isNull();
    }

    @Test
    @DisplayName("Should create InstantFormatInfo with default pattern for Instant field without @JsonFormat")
    void shouldCreateInstantFormatInfoWithDefaultPattern() {
        // Given
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.time.Instant");
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isUseTimestamp()).isFalse();
        assertThat(info.shouldUseTimestamp()).isFalse();
        assertThat(info.getEffectiveTimezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("Should return false for hasCustomInstantFormat when field has no @JsonFormat")
    void shouldReturnFalseForHasCustomInstantFormatWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        boolean hasCustomFormat = analyzer.hasCustomInstantFormat(mockField);

        // Then
        assertThat(hasCustomFormat).isFalse();
    }

    @Test
    @DisplayName("Should return false for hasCustomInstantFormat when field is null")
    void shouldReturnFalseForHasCustomInstantFormatWhenFieldIsNull() {
        // When
        boolean hasCustomFormat = analyzer.hasCustomInstantFormat(null);

        // Then
        assertThat(hasCustomFormat).isFalse();
    }

    @Test
    @DisplayName("Should return null for extractCustomInstantPattern when field has no @JsonFormat")
    void shouldReturnNullForExtractCustomInstantPatternWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        String pattern = analyzer.extractCustomInstantPattern(mockField);

        // Then
        assertThat(pattern).isNull();
    }

    @Test
    @DisplayName("Should return null for extractCustomInstantPattern when field is null")
    void shouldReturnNullForExtractCustomInstantPatternWhenFieldIsNull() {
        // When
        String pattern = analyzer.extractCustomInstantPattern(null);

        // Then
        assertThat(pattern).isNull();
    }

    @Test
    @DisplayName("Should return null for extractInstantShape when field has no @JsonFormat")
    void shouldReturnNullForExtractInstantShapeWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        String shape = analyzer.extractInstantShape(mockField);

        // Then
        assertThat(shape).isNull();
    }

    @Test
    @DisplayName("Should return null for extractInstantTimezone when field has no @JsonFormat")
    void shouldReturnNullForExtractInstantTimezoneWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        String timezone = analyzer.extractInstantTimezone(mockField);

        // Then
        assertThat(timezone).isNull();
    }

    @Test
    @DisplayName("Should return false for shouldUseTimestampFormat when field has no @JsonFormat")
    void shouldReturnFalseForShouldUseTimestampFormatWhenFieldHasNoJsonFormat() {
        // Given
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        // When
        boolean useTimestamp = analyzer.shouldUseTimestampFormat(mockField);

        // Then
        assertThat(useTimestamp).isFalse();
    }

    @Test
    @DisplayName("Should return false for shouldUseTimestampFormat when field is null")
    void shouldReturnFalseForShouldUseTimestampFormatWhenFieldIsNull() {
        // When
        boolean useTimestamp = analyzer.shouldUseTimestampFormat(null);

        // Then
        assertThat(useTimestamp).isFalse();
    }

    @Test
    @DisplayName("Should create default InstantFormatInfo correctly")
    void shouldCreateDefaultInstantFormatInfoCorrectly() {
        // When
        InstantFormatInfo info = analyzer.createDefaultInstantFormatInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.usesCustomFormat()).isFalse();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isUseTimestamp()).isFalse();
        assertThat(info.shouldUseTimestamp()).isFalse();
        assertThat(info.getEffectiveTimezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("Should create custom InstantFormatInfo correctly")
    void shouldCreateCustomInstantFormatInfoCorrectly() {
        // When
        InstantFormatInfo info = analyzer.createCustomInstantFormatInfo("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        assertThat(info.isHasCustomFormat()).isTrue();
        assertThat(info.usesCustomFormat()).isTrue();
        assertThat(info.getEffectivePattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        assertThat(info.isUseTimestamp()).isFalse();
        assertThat(info.shouldUseTimestamp()).isFalse();
    }

    @Test
    @DisplayName("Should create timestamp InstantFormatInfo correctly")
    void shouldCreateTimestampInstantFormatInfoCorrectly() {
        // When
        InstantFormatInfo info = analyzer.createTimestampFormatInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.isUseTimestamp()).isTrue();
        assertThat(info.shouldUseTimestamp()).isTrue();
        assertThat(info.getShape()).isEqualTo("NUMBER");
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should create InstantFormatInfo with timezone correctly")
    void shouldCreateInstantFormatInfoWithTimezoneCorrectly() {
        // When
        InstantFormatInfo info = analyzer.createInstantFormatInfoWithTimezone("Europe/Istanbul");

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getTimezone()).isEqualTo("Europe/Istanbul");
        assertThat(info.getEffectiveTimezone()).isEqualTo("Europe/Istanbul");
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @DisplayName("Should validate InstantFormatInfo correctly")
    void shouldValidateInstantFormatInfoCorrectly() {
        // Valid info
        InstantFormatInfo validInfo = analyzer.createDefaultInstantFormatInfo();
        assertThat(analyzer.isValidInstantFormatInfo(validInfo)).isTrue();

        // Valid custom info
        InstantFormatInfo validCustomInfo = analyzer.createCustomInstantFormatInfo("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        assertThat(analyzer.isValidInstantFormatInfo(validCustomInfo)).isTrue();

        // Invalid info - null
        assertThat(analyzer.isValidInstantFormatInfo(null)).isFalse();

        // Invalid info - null pattern
        InstantFormatInfo nullPatternInfo = new InstantFormatInfo();
        nullPatternInfo.setPattern(null);
        assertThat(analyzer.isValidInstantFormatInfo(nullPatternInfo)).isFalse();

        // Invalid info - empty pattern
        InstantFormatInfo emptyPatternInfo = new InstantFormatInfo();
        emptyPatternInfo.setPattern("");
        assertThat(analyzer.isValidInstantFormatInfo(emptyPatternInfo)).isFalse();

        // Invalid info - blank pattern
        InstantFormatInfo blankPatternInfo = new InstantFormatInfo();
        blankPatternInfo.setPattern("   ");
        assertThat(analyzer.isValidInstantFormatInfo(blankPatternInfo)).isFalse();
    }

    @Test
    @DisplayName("Should handle InstantFormatInfo factory methods correctly")
    void shouldHandleInstantFormatInfoFactoryMethodsCorrectly() {
        // Test withDefaultPattern
        InstantFormatInfo defaultInfo = InstantFormatInfo.withDefaultPattern();
        assertThat(defaultInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(defaultInfo.isHasCustomFormat()).isFalse();

        // Test withCustomPattern
        InstantFormatInfo customInfo = InstantFormatInfo.withCustomPattern("custom-pattern");
        assertThat(customInfo.getPattern()).isEqualTo("custom-pattern");
        assertThat(customInfo.isHasCustomFormat()).isTrue();

        // Test withTimestampFormat
        InstantFormatInfo timestampInfo = InstantFormatInfo.withTimestampFormat();
        assertThat(timestampInfo.isUseTimestamp()).isTrue();
        assertThat(timestampInfo.getShape()).isEqualTo("NUMBER");

        // Test withTimezone
        InstantFormatInfo timezoneInfo = InstantFormatInfo.withTimezone("America/New_York");
        assertThat(timezoneInfo.getTimezone()).isEqualTo("America/New_York");
        assertThat(timezoneInfo.getEffectiveTimezone()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("Should demonstrate expected behavior with FilterConstants")
    void shouldDemonstrateExpectedBehaviorWithFilterConstants() {
        // Verify that FilterConstants contains the expected Instant pattern
        assertThat(FilterConstants.DEFAULT_INSTANT_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss.SSSX");

        // Test that analyzer uses this constant correctly
        InstantFormatInfo info = analyzer.createDefaultInstantFormatInfo();
        assertThat(info.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);

        String defaultPattern = analyzer.getDefaultInstantPattern();
        assertThat(defaultPattern).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }
}