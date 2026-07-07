package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JacksonAnnotationAnalyzerImpl.
 * Tests Jackson annotation extraction, parameter processing, and code generation.
 */
@ExtendWith(MockitoExtension.class)
class JacksonAnnotationAnalyzerImplTest {

    private static final String PATTERN_PARAM = "pattern";

    private JacksonAnnotationAnalyzerImpl analyzer;

    @Mock
    private javax.annotation.processing.ProcessingEnvironment processingEnv;

    @BeforeEach
    void setUp() {
        analyzer = new JacksonAnnotationAnalyzerImpl(processingEnv);
    }

    @Test
    void extractJacksonAnnotationsWithNullFieldReturnsEmptyList() {
        // When
        List<AnnotationInfo> result = analyzer.extractJacksonAnnotations(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void generateAnnotationCodeWithNullAnnotationReturnsEmptyString() {
        // When
        String result = analyzer.generateAnnotationCode(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void generateAnnotationCodeWithJsonFormatAnnotationGeneratesCorrectCode() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        info.setParameters(Map.of(PATTERN_PARAM, "yyyy-MM-dd HH:mm:ss"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")");
    }

    @Test
    void generateAnnotationCodeWithJsonPropertyValueOnlyGeneratesCorrectCode() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonProperty.class.getName());
        info.setParameters(Map.of("value", "custom_name"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonProperty(\"custom_name\")");
    }

    @Test
    void generateAnnotationCodeWithMultipleParametersGeneratesCorrectCode() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        Map<String, Object> params = new LinkedHashMap<>(); // Use LinkedHashMap for predictable order
        params.put(PATTERN_PARAM, "yyyy-MM-dd");
        params.put("timezone", "UTC");
        info.setParameters(params);

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).contains("@JsonFormat(")
                .contains("pattern = \"yyyy-MM-dd\"")
                .contains("timezone = \"UTC\"")
                .endsWith(")");
    }

    @Test
    void generateAnnotationCodeWithBooleanParameterGeneratesCorrectCode() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonIgnore.class.getName());
        info.setParameters(Map.of("value", true));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonIgnore(true)");
    }

    @Test
    void generateAnnotationCodeWithStringEscapingEscapesCorrectly() {
        // Given
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(JsonFormat.class.getName());
        info.setParameters(Map.of(PATTERN_PARAM, "yyyy-MM-dd'T'HH:mm:ss"));

        // When
        String result = analyzer.generateAnnotationCode(info);

        // Then
        assertThat(result).isEqualTo("@JsonFormat(pattern = \"yyyy-MM-dd'T'HH:mm:ss\")");
    }
}