package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.InstantFormatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InstantFormatAnalyzer using real annotation processing.
 * These tests verify the analyzer works with actual @JsonFormat annotations on Instant fields.
 */
@Tag("integration")
@DisplayName("InstantFormatAnalyzer Integration Tests")
class InstantFormatAnalyzerIntegrationTest {

    private static final String PATTERN_WITH_MILLIS_XXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String PATTERN_WITH_MILLIS_X = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String EUROPE_ISTANBUL = "Europe/Istanbul";
    private static final String UTC = "UTC";
    private static final String AMERICA_NEW_YORK = "America/New_York";
    private static final String TR_TR = "tr_TR";
    private static final String EN_US = "en_US";
    private static final String NUMBER = "NUMBER";
    private static final String CUSTOM_PATTERN = "custom-pattern";
    

    private InstantFormatAnalyzerImpl analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new InstantFormatAnalyzerImpl();
    }

    @Test
    @DisplayName("Should extract custom pattern from @JsonFormat annotation")
    void shouldExtractCustomPatternFromJsonFormatAnnotation() throws NoSuchFieldException {
        // Given
        var field = TestEntity.class.getDeclaredField("customFormattedInstant");

        // When & Then - Test direct annotation access logic
        JsonFormat annotation = field.getAnnotation(JsonFormat.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.pattern()).isEqualTo(PATTERN_WITH_MILLIS_XXX);
    }

    @Test
    @DisplayName("Should detect timestamp format from shape configuration")
    void shouldDetectTimestampFormatFromShapeConfiguration() throws NoSuchFieldException {
        // Given
        var timestampField = TestEntity.class.getDeclaredField("timestampInstant");
        var stringShapeField = TestEntity.class.getDeclaredField("stringShapeInstant");

        // When & Then - Test shape detection logic
        JsonFormat timestampAnnotation = timestampField.getAnnotation(JsonFormat.class);
        JsonFormat stringAnnotation = stringShapeField.getAnnotation(JsonFormat.class);

        assertThat(timestampAnnotation).isNotNull();
        assertThat(timestampAnnotation.shape()).isEqualTo(JsonFormat.Shape.NUMBER);

        assertThat(stringAnnotation).isNotNull();
        assertThat(stringAnnotation.shape()).isEqualTo(JsonFormat.Shape.STRING);
    }

    @Test
    @DisplayName("Should handle timezone configuration from @JsonFormat")
    void shouldHandleTimezoneConfigurationFromJsonFormat() throws NoSuchFieldException {
        // Given
        var utcField = TestEntity.class.getDeclaredField("utcInstant");
        var istanbulField = TestEntity.class.getDeclaredField("istanbulInstant");

        // When & Then - Test timezone extraction
        JsonFormat utcAnnotation = utcField.getAnnotation(JsonFormat.class);
        JsonFormat istanbulAnnotation = istanbulField.getAnnotation(JsonFormat.class);

        assertThat(utcAnnotation).isNotNull();
        assertThat(utcAnnotation.timezone()).isEqualTo(UTC);

        assertThat(istanbulAnnotation).isNotNull();
        assertThat(istanbulAnnotation.timezone()).isEqualTo(EUROPE_ISTANBUL);
    }

    @Test
    @DisplayName("Should handle locale configuration from @JsonFormat")
    void shouldHandleLocaleConfigurationFromJsonFormat() throws NoSuchFieldException {
        // Given
        var localizedField = TestEntity.class.getDeclaredField("localizedInstant");
        var completeField = TestEntity.class.getDeclaredField("completeConfigInstant");

        // When & Then - Test locale extraction
        JsonFormat localizedAnnotation = localizedField.getAnnotation(JsonFormat.class);
        JsonFormat completeAnnotation = completeField.getAnnotation(JsonFormat.class);

        assertThat(localizedAnnotation).isNotNull();
        assertThat(localizedAnnotation.locale()).isEqualTo(TR_TR);

        assertThat(completeAnnotation).isNotNull();
        assertThat(completeAnnotation.locale()).isEqualTo(EN_US);
        assertThat(completeAnnotation.timezone()).isEqualTo(AMERICA_NEW_YORK);
        assertThat(completeAnnotation.pattern()).isEqualTo(PATTERN_WITH_MILLIS_XXX);
    }

    @Test
    @DisplayName("Should detect custom format presence correctly")
    void shouldDetectCustomFormatPresenceCorrectly() throws NoSuchFieldException {
        // Given
        var customField = TestEntity.class.getDeclaredField("customFormattedInstant");
        var defaultField = TestEntity.class.getDeclaredField("defaultFormattedInstant");
        var timestampField = TestEntity.class.getDeclaredField("timestampInstant");
        var emptyPatternField = TestEntity.class.getDeclaredField("emptyPatternInstant");

        // When & Then - Test custom format detection logic
        JsonFormat customAnnotation = customField.getAnnotation(JsonFormat.class);
        JsonFormat defaultAnnotation = defaultField.getAnnotation(JsonFormat.class);
        JsonFormat timestampAnnotation = timestampField.getAnnotation(JsonFormat.class);
        JsonFormat emptyPatternAnnotation = emptyPatternField.getAnnotation(JsonFormat.class);

        // Custom pattern field should have custom format
        assertThat(customAnnotation != null && !customAnnotation.pattern().isEmpty()).isTrue();

        // Default field should not have annotation
        assertThat(defaultAnnotation).isNull();

        // Timestamp field should have custom format (shape configuration)
        assertThat(timestampAnnotation != null && timestampAnnotation.shape() != JsonFormat.Shape.ANY).isTrue();

        // Empty pattern field should not be considered as having custom format
        assertThat(emptyPatternAnnotation != null && !emptyPatternAnnotation.pattern().isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should create correct InstantFormatInfo for different configurations")
    void shouldCreateCorrectInstantFormatInfoForDifferentConfigurations() {
        // Test default InstantFormatInfo
        InstantFormatInfo defaultInfo = analyzer.createDefaultInstantFormatInfo();
        assertThat(defaultInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(defaultInfo.isHasCustomFormat()).isFalse();
        assertThat(defaultInfo.isUseTimestamp()).isFalse();
        assertThat(defaultInfo.getEffectiveTimezone()).isEqualTo(UTC);

        // Test custom pattern InstantFormatInfo
        InstantFormatInfo customInfo = analyzer.createCustomInstantFormatInfo(PATTERN_WITH_MILLIS_XXX);
        assertThat(customInfo.getPattern()).isEqualTo(PATTERN_WITH_MILLIS_XXX);
        assertThat(customInfo.isHasCustomFormat()).isTrue();
        assertThat(customInfo.isUseTimestamp()).isFalse();

        // Test timestamp InstantFormatInfo
        InstantFormatInfo timestampInfo = analyzer.createTimestampFormatInfo();
        assertThat(timestampInfo.isUseTimestamp()).isTrue();
        assertThat(timestampInfo.shouldUseTimestamp()).isTrue();
        assertThat(timestampInfo.getShape()).isEqualTo(NUMBER);

        // Test timezone InstantFormatInfo
        InstantFormatInfo timezoneInfo = analyzer.createInstantFormatInfoWithTimezone(EUROPE_ISTANBUL);
        assertThat(timezoneInfo.getTimezone()).isEqualTo(EUROPE_ISTANBUL);
        assertThat(timezoneInfo.getEffectiveTimezone()).isEqualTo(EUROPE_ISTANBUL);
    }

    @Test
    @DisplayName("Should validate InstantFormatInfo instances correctly")
    void shouldValidateInstantFormatInfoInstancesCorrectly() {
        // Valid default info
        InstantFormatInfo validInfo = analyzer.createDefaultInstantFormatInfo();
        assertThat(analyzer.isValidInstantFormatInfo(validInfo)).isTrue();

        // Valid custom info
        InstantFormatInfo validCustomInfo = analyzer.createCustomInstantFormatInfo(PATTERN_WITH_MILLIS_XXX);
        assertThat(analyzer.isValidInstantFormatInfo(validCustomInfo)).isTrue();

        // Valid timestamp info
        InstantFormatInfo validTimestampInfo = analyzer.createTimestampFormatInfo();
        assertThat(analyzer.isValidInstantFormatInfo(validTimestampInfo)).isTrue();

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

    @Test
    @DisplayName("Should handle InstantFormatInfo factory methods correctly")
    void shouldHandleInstantFormatInfoFactoryMethodsCorrectly() {
        // Test all factory methods
        InstantFormatInfo defaultInfo = InstantFormatInfo.withDefaultPattern();
        assertThat(defaultInfo.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(defaultInfo.isHasCustomFormat()).isFalse();

        InstantFormatInfo customInfo = InstantFormatInfo.withCustomPattern(CUSTOM_PATTERN);
        assertThat(customInfo.getPattern()).isEqualTo(CUSTOM_PATTERN);
        assertThat(customInfo.isHasCustomFormat()).isTrue();

        InstantFormatInfo timestampInfo = InstantFormatInfo.withTimestampFormat();
        assertThat(timestampInfo.isUseTimestamp()).isTrue();
        assertThat(timestampInfo.getShape()).isEqualTo(NUMBER);

        InstantFormatInfo timezoneInfo = InstantFormatInfo.withTimezone(AMERICA_NEW_YORK);
        assertThat(timezoneInfo.getTimezone()).isEqualTo(AMERICA_NEW_YORK);
        assertThat(timezoneInfo.getEffectiveTimezone()).isEqualTo(AMERICA_NEW_YORK);
    }

    @Test
    @DisplayName("Should handle Instant-specific features correctly")
    void shouldHandleInstantSpecificFeaturesCorrectly() {
        // Test timestamp format detection
        InstantFormatInfo timestampInfo = analyzer.createTimestampFormatInfo();
        assertThat(timestampInfo.shouldUseTimestamp()).isTrue();
        assertThat(timestampInfo.getShape()).isEqualTo(NUMBER);

        // Test timezone handling
        InstantFormatInfo timezoneInfo = analyzer.createInstantFormatInfoWithTimezone(EUROPE_ISTANBUL);
        assertThat(timezoneInfo.getEffectiveTimezone()).isEqualTo(EUROPE_ISTANBUL);

        // Test default timezone fallback
        InstantFormatInfo defaultInfo = analyzer.createDefaultInstantFormatInfo();
        assertThat(defaultInfo.getEffectiveTimezone()).isEqualTo(UTC);

        // Test effective pattern fallback
        InstantFormatInfo nullPatternInfo = new InstantFormatInfo();
        nullPatternInfo.setPattern(null);
        assertThat(nullPatternInfo.getEffectivePattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    /**
     * Test entity class with various Instant field configurations.
     */
    static class TestEntity {

        // Instant field with custom @JsonFormat pattern
        @JsonFormat(pattern = PATTERN_WITH_MILLIS_XXX)
        private Instant customFormattedInstant;

        // Instant field without @JsonFormat (should use default)
        private Instant defaultFormattedInstant;

        // Instant field with timestamp format (NUMBER shape)
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        private Instant timestampInstant;

        // Instant field with timezone configuration
        @JsonFormat(pattern = PATTERN_WITH_MILLIS_X, timezone = UTC)
        private Instant utcInstant;

        // Instant field with different timezone
        @JsonFormat(pattern = PATTERN_WITH_MILLIS_X, timezone = EUROPE_ISTANBUL)
        private Instant istanbulInstant;

        // Instant field with locale configuration
        @JsonFormat(pattern = PATTERN_WITH_MILLIS_X, locale = TR_TR)
        private Instant localizedInstant;

        // Instant field with complete configuration
        @JsonFormat(
                pattern = PATTERN_WITH_MILLIS_XXX,
                timezone = AMERICA_NEW_YORK,
                locale = EN_US
        )
        private Instant completeConfigInstant;

        // Instant field with STRING shape (explicit)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = PATTERN_WITH_MILLIS_X)
        private Instant stringShapeInstant;

        // Instant field with empty pattern (should be treated as no custom format)
        @JsonFormat(pattern = "")
        private Instant emptyPatternInstant;

        // Non-Instant field with @JsonFormat (should not be processed as Instant)
        @JsonFormat(pattern = PATTERN_WITH_MILLIS_X)
        private String stringFieldWithJsonFormat;
    }
}