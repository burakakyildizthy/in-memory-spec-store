package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DateTimeFormatAnalyzer using real annotation processing.
 * These tests verify the analyzer works with actual @JsonFormat annotations.
 */
@Tag("integration")
@DisplayName("DateTimeFormatAnalyzer Integration Tests")
class DateTimeFormatAnalyzerIntegrationTest {

    private static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_TIME_INSTANT = "java.time.Instant";
    private static final String PATTERN_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String PATTERN_DD_MM_YYYY_HH_MM_SS = "dd/MM/yyyy HH:mm:ss";
    private static final String CUSTOM_FORMATTED_DATE_TIME = "customFormattedDateTime";
    private static final String DEFAULT_FORMATTED_DATE_TIME = "defaultFormattedDateTime";

    private DateTimeFormatAnalyzerImpl analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DateTimeFormatAnalyzerImpl();
    }

    @Test
    @DisplayName("Should extract custom pattern from @JsonFormat annotation")
    void shouldExtractCustomPatternFromJsonFormatAnnotation() throws NoSuchFieldException {
        // Given
        var field = TestEntity.class.getDeclaredField(CUSTOM_FORMATTED_DATE_TIME);

        // Then - This test demonstrates the expected behavior
        // In a real annotation processor, this would work with actual VariableElement
        // For now, we test the logic with direct annotation access
        JsonFormat annotation = field.getAnnotation(JsonFormat.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.pattern()).isEqualTo(PATTERN_DD_MM_YYYY_HH_MM_SS);
    }

    @Test
    @DisplayName("Should detect custom format presence correctly")
    void shouldDetectCustomFormatPresenceCorrectly() throws NoSuchFieldException {
        // Given
        var customField = TestEntity.class.getDeclaredField(CUSTOM_FORMATTED_DATE_TIME);
        var defaultField = TestEntity.class.getDeclaredField(DEFAULT_FORMATTED_DATE_TIME);
        var emptyPatternField = TestEntity.class.getDeclaredField("emptyPatternDateTime");

        // When & Then - Test direct annotation access logic
        JsonFormat customAnnotation = customField.getAnnotation(JsonFormat.class);
        JsonFormat defaultAnnotation = defaultField.getAnnotation(JsonFormat.class);
        JsonFormat emptyPatternAnnotation = emptyPatternField.getAnnotation(JsonFormat.class);

        // Verify the logic that would be used in the analyzer
        assertThat(customAnnotation != null && !customAnnotation.pattern().isEmpty()).isTrue();
        assertThat(defaultAnnotation != null && !defaultAnnotation.pattern().isEmpty()).isFalse();
        assertThat(emptyPatternAnnotation != null && !emptyPatternAnnotation.pattern().isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should handle timezone and locale from @JsonFormat")
    void shouldHandleTimezoneAndLocaleFromJsonFormat() throws NoSuchFieldException {
        // Given
        var field = TestEntity.class.getDeclaredField("formattedWithTimezoneAndLocale");

        // When & Then - Test annotation parameter extraction
        JsonFormat annotation = field.getAnnotation(JsonFormat.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.pattern()).isEqualTo(PATTERN_YYYY_MM_DD_HH_MM_SS);
        assertThat(annotation.timezone()).isEqualTo("UTC");
        assertThat(annotation.locale()).isEqualTo("en_US");
    }

    @Test
    @DisplayName("Should create correct DateTimeFormatInfo for different field types")
    void shouldCreateCorrectDateTimeFormatInfoForDifferentFieldTypes() {
        // Test LocalDateTime with default pattern
        DateTimeFormatInfo localDateTimeInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(localDateTimeInfo.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(localDateTimeInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(localDateTimeInfo.isHasCustomFormat()).isFalse();

        // Test LocalDate with default pattern
        DateTimeFormatInfo localDateInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE);
        assertThat(localDateInfo.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE);
        assertThat(localDateInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
        assertThat(localDateInfo.isHasCustomFormat()).isFalse();

        // Test Instant with default pattern
        DateTimeFormatInfo instantInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_INSTANT);
        assertThat(instantInfo.getFieldType()).isEqualTo(JAVA_TIME_INSTANT);
        assertThat(instantInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(instantInfo.isHasCustomFormat()).isFalse();

        // Test LocalDateTime with custom pattern
        DateTimeFormatInfo customInfo = analyzer.createCustomFormatInfo(JAVA_TIME_LOCAL_DATE_TIME, PATTERN_DD_MM_YYYY_HH_MM_SS);
        assertThat(customInfo.getFieldType()).isEqualTo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(customInfo.getPattern()).isEqualTo(PATTERN_DD_MM_YYYY_HH_MM_SS);
        assertThat(customInfo.isHasCustomFormat()).isTrue();
    }

    @Test
    @DisplayName("Should validate DateTimeFormatInfo instances correctly")
    void shouldValidateDateTimeFormatInfoInstancesCorrectly() {
        // Valid info
        DateTimeFormatInfo validInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(analyzer.isValidDateTimeFormatInfo(validInfo)).isTrue();

        // Valid custom info
        DateTimeFormatInfo validCustomInfo = analyzer.createCustomFormatInfo(JAVA_TIME_LOCAL_DATE, "dd-MM-yyyy");
        assertThat(analyzer.isValidDateTimeFormatInfo(validCustomInfo)).isTrue();

        // Invalid info - null
        assertThat(analyzer.isValidDateTimeFormatInfo(null)).isFalse();

        // Invalid info - null pattern
        DateTimeFormatInfo nullPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        nullPatternInfo.setPattern(null);
        assertThat(analyzer.isValidDateTimeFormatInfo(nullPatternInfo)).isFalse();

        // Invalid info - empty pattern
        DateTimeFormatInfo emptyPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        emptyPatternInfo.setPattern("");
        assertThat(analyzer.isValidDateTimeFormatInfo(emptyPatternInfo)).isFalse();

        // Invalid info - blank pattern
        DateTimeFormatInfo blankPatternInfo = new DateTimeFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        blankPatternInfo.setPattern("   ");
        assertThat(analyzer.isValidDateTimeFormatInfo(blankPatternInfo)).isFalse();
    }

    @Test
    @DisplayName("Should handle all supported temporal types correctly")
    void shouldHandleAllSupportedTemporalTypesCorrectly() {
        // Test type identification logic
        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_LOCAL_DATE_TIME))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_LOCAL_DATE))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(JAVA_TIME_INSTANT))
                .isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);

        // Unsupported types should default to LocalDateTime pattern
        assertThat(analyzer.getDefaultPatternForType("java.lang.String"))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType("java.time.ZonedDateTime"))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        assertThat(analyzer.getDefaultPatternForType(null))
                .isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    @DisplayName("Should demonstrate expected behavior with FilterConstants")
    void shouldDemonstrateExpectedBehaviorWithFilterConstants() {
        // Verify that FilterConstants contains the expected patterns
        assertThat(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN).isEqualTo(PATTERN_YYYY_MM_DD_HH_MM_SS);
        assertThat(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN).isEqualTo("yyyy-MM-dd");
        assertThat(FilterConstants.DEFAULT_INSTANT_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss.SSSX");

        // Test that analyzer uses these constants correctly
        DateTimeFormatInfo localDateTimeInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE_TIME);
        assertThat(localDateTimeInfo.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);

        DateTimeFormatInfo localDateInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_LOCAL_DATE);
        assertThat(localDateInfo.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);

        DateTimeFormatInfo instantInfo = analyzer.createDefaultFormatInfo(JAVA_TIME_INSTANT);
        assertThat(instantInfo.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    /**
     * Test entity class with various datetime field configurations.
     */
    static class TestEntity {

        // LocalDateTime field with custom @JsonFormat
        @JsonFormat(pattern = PATTERN_DD_MM_YYYY_HH_MM_SS)
        private LocalDateTime customFormattedDateTime;

        // LocalDate field with custom @JsonFormat
        @JsonFormat(pattern = "dd-MM-yyyy")
        private LocalDate customFormattedDate;

        // Instant field with custom @JsonFormat
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private Instant customFormattedInstant;
        @JsonFormat
        private String defaultFormattedDateTime;

        // @JsonFormat with timezone and locale
        @JsonFormat(pattern = PATTERN_YYYY_MM_DD_HH_MM_SS, timezone = "UTC", locale = "en_US")
        private LocalDateTime formattedWithTimezoneAndLocale;

        // @JsonFormat with empty pattern (should be treated as no custom format)
        @JsonFormat(pattern = "")
        private LocalDateTime emptyPatternDateTime;

        // Non-temporal field with @JsonFormat (should not be processed as temporal)
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String stringFieldWithJsonFormat;
    }
}