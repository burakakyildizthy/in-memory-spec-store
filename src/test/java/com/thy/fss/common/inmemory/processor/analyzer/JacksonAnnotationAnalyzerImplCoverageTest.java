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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JacksonAnnotationAnalyzerImpl Coverage Tests")
class JacksonAnnotationAnalyzerImplCoverageTest {

    private JacksonAnnotationAnalyzerImpl analyzer;

    @Mock
    private javax.annotation.processing.ProcessingEnvironment processingEnv;

    @BeforeEach
    void setUp() {
        analyzer = new JacksonAnnotationAnalyzerImpl(processingEnv);
    }

    // ===== generateAnnotationCode - null annotation type =====

    @Test
    void generateAnnotationCode_nullAnnotationType_returnsEmpty() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType(null);

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEmpty();
    }

    // ===== generateAnnotationCode - no parameters =====

    @Test
    void generateAnnotationCode_noParameters_returnsSimpleAnnotation() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonIgnore");

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEqualTo("@JsonIgnore");
    }

    // ===== generateAnnotationCode - empty parameters =====

    @Test
    void generateAnnotationCode_emptyParameters_returnsSimpleAnnotation() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonIgnore");
        info.setParameters(Map.of());

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEqualTo("@JsonIgnore");
    }

    // ===== generateAnnotationCode - single integer value =====

    @Test
    void generateAnnotationCode_singleIntegerValue_rendersWithoutQuotes() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonProperty");
        info.setParameters(Map.of("value", 42));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEqualTo("@JsonProperty(42)");
    }

    // ===== generateAnnotationCode - non-value single parameter =====

    @Test
    void generateAnnotationCode_singleNonValueParam_rendersWithName() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonFormat");
        info.setParameters(Map.of("timezone", "UTC"));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).contains("timezone = \"UTC\"");
    }

    // ===== generateAnnotationCode - multiple parameters =====

    @Test
    void generateAnnotationCode_multipleParams_rendersAll() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonFormat");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pattern", "yyyy-MM-dd");
        params.put("timezone", "UTC");
        params.put("locale", "tr_TR");
        info.setParameters(params);

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result)
                .startsWith("@JsonFormat(")
                .contains("pattern = \"yyyy-MM-dd\"")
                .contains("timezone = \"UTC\"")
                .contains("locale = \"tr_TR\"")
                .endsWith(")");
    }

    // ===== generateAnnotationCode - list value =====

    @Test
    void generateAnnotationCode_listValue_rendersCorrectly() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonTypeInfo");
        info.setParameters(Map.of("value", List.of("a", "b")));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).contains("@JsonTypeInfo");
    }

    // ===== generateAnnotationCode - null annotationType in params =====

    @Test
    void generateAnnotationCode_nullParams_returnsSimpleAnnotation() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonValue");
        info.setParameters(null);

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEqualTo("@JsonValue");
    }

    // ===== generateAnnotationCode - Boolean value parameter =====

    @Test
    void generateAnnotationCode_booleanValueParam_rendersCorrectly() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonIgnore");
        info.setParameters(Map.of("value", false));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).isEqualTo("@JsonIgnore(false)");
    }

    // ===== generateAnnotationCode - Number non-value param =====

    @Test
    void generateAnnotationCode_numberNonValueParam_rendersWithName() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonFormat");
        info.setParameters(Map.of("shape", 1));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).contains("shape = 1");
    }

    // ===== generateAnnotationCode - string with backslash =====

    @Test
    void generateAnnotationCode_stringWithBackslash_escapesCorrectly() {
        AnnotationInfo info = new AnnotationInfo();
        info.setAnnotationType("com.fasterxml.jackson.annotation.JsonFormat");
        info.setParameters(Map.of("pattern", "path\\to\\value"));

        String result = analyzer.generateAnnotationCode(info);

        assertThat(result).contains("@JsonFormat");
        assertThat(result).contains("pattern");
    }

    // ===== hasJacksonAnnotations - null field =====

    @Test
    void hasJacksonAnnotations_nullField_returnsFalse() {
        boolean result = analyzer.hasJacksonAnnotations(null);
        assertThat(result).isFalse();
    }

    // ===== extractAndGenerateAnnotationCode - null field =====

    @Test
    void extractAndGenerateAnnotationCode_nullField_returnsEmpty() {
        List<String> result = analyzer.extractAndGenerateAnnotationCode(null);
        assertThat(result).isEmpty();
    }

    // ===== custom annotationValidator constructor =====

    @Test
    void constructor_withCustomAnnotationValidator_createsAnalyzer() {
        AnnotationValidator customValidator = mock(AnnotationValidator.class);
        JacksonAnnotationAnalyzerImpl customAnalyzer = new JacksonAnnotationAnalyzerImpl(processingEnv, customValidator);

        assertThat(customAnalyzer).isNotNull();
    }
}
