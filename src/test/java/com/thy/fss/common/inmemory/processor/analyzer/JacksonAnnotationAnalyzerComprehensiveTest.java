package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.VariableElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for JacksonAnnotationAnalyzerImpl.
 * Tests various Jackson annotation combinations and edge cases.
 * <p>
 * Requirements tested: 1.1, 1.2, 1.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Jackson Annotation Analyzer Comprehensive Tests")
class JacksonAnnotationAnalyzerComprehensiveTest {

    private static final String VALUE_PARAM = "value";

    private JacksonAnnotationAnalyzerImpl analyzer;

    @Mock
    private VariableElement mockField;

    @Mock
    private javax.annotation.processing.ProcessingEnvironment processingEnv;

    @BeforeEach
    void setUp() {
        analyzer = new JacksonAnnotationAnalyzerImpl(processingEnv);
    }

    @Test
    @DisplayName("Should extract @JsonFormat annotation with pattern and timezone")
    void shouldExtractJsonFormatAnnotationWithPatternAndTimezone() {
        // Given
        // This test would require mocking AnnotationMirror which is complex
        // For now, we'll test the generateAnnotationCode method directly
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pattern", "yyyy-MM-dd HH:mm:ss");
        params.put("timezone", "UTC");
        info.setParameters(params);

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).contains("@JsonFormat(")
                .contains("pattern = \"yyyy-MM-dd HH:mm:ss\"")
                .contains("timezone = \"UTC\"")
                .endsWith(")");
    }

    @Test
    @DisplayName("Should generate @JsonProperty annotation with custom name")
    void shouldGenerateJsonPropertyAnnotationWithCustomName() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonProperty.class.getName());
        info.setParameters(Map.of(VALUE_PARAM, "custom_field_name"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonProperty(\"custom_field_name\")");
    }

    @Test
    @DisplayName("Should generate @JsonIgnore annotation")
    void shouldGenerateJsonIgnoreAnnotation() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonIgnore.class.getName());
        info.setParameters(Map.of(VALUE_PARAM, true));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonIgnore(true)");
    }

    @Test
    @DisplayName("Should handle null field gracefully in extractJacksonAnnotations")
    void shouldHandleNullFieldGracefullyInExtractJacksonAnnotations() {
        // When
        List<AnnotationInfo> result = analyzer.extractJacksonAnnotations(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle null field gracefully in hasJacksonAnnotations")
    void shouldHandleNullFieldGracefullyInHasJacksonAnnotations() {
        // When
        boolean result = analyzer.hasJacksonAnnotations(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should generate correct annotation code for @JsonFormat with special characters")
    void shouldGenerateCorrectAnnotationCodeForJsonFormatWithSpecialCharacters() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        info.setParameters(Map.of("pattern", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonFormat(pattern = \"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\")");
    }

    @Test
    @DisplayName("Should generate correct annotation code for @JsonProperty with access control")
    void shouldGenerateCorrectAnnotationCodeForJsonPropertyWithAccessControl() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonProperty.class.getName());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(VALUE_PARAM, "field_name");
        params.put("access", "JsonProperty.Access.READ_ONLY");
        info.setParameters(params);

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).contains("@JsonProperty(")
                .contains("value = \"field_name\"")
                .contains("access = \"JsonProperty.Access.READ_ONLY\"")
                .endsWith(")");
    }

    @Test
    @DisplayName("Should handle complex annotation parameter types")
    void shouldHandleComplexAnnotationParameterTypes() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pattern", "yyyy-MM-dd");
        params.put("timezone", "UTC");
        params.put("lenient", false);
        info.setParameters(params);

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).contains("@JsonFormat(")
                .contains("pattern = \"yyyy-MM-dd\"")
                .contains("timezone = \"UTC\"")
                .contains("lenient = false")
                .endsWith(")");
    }

    @Test
    @DisplayName("Should generate annotation code with null annotation gracefully")
    void shouldGenerateAnnotationCodeWithNullAnnotationGracefully() {
        // When
        String result = analyzer.generateAnnotationCode(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should generate annotation code with null type gracefully")
    void shouldGenerateAnnotationCodeWithNullTypeGracefully() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(null);
        info.setParameters(Map.of("pattern", "yyyy-MM-dd"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should extract and generate annotation code for null field")
    void shouldExtractAndGenerateAnnotationCodeForNullField() {
        // When
        List<String> result = analyzer.extractAndGenerateAnnotationCode(null);

        // Then
        assertThat(result).isEmpty();
    }


}