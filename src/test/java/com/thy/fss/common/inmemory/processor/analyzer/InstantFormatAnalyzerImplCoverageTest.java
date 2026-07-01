package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.InstantFormatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Coverage tests for InstantFormatAnalyzerImpl covering missed branches
 * where @JsonFormat annotation IS present with various configurations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstantFormatAnalyzerImplCoverageTest {

    private InstantFormatAnalyzerImpl analyzer;

    @Mock
    private VariableElement mockField;

    @Mock
    private TypeMirror mockTypeMirror;

    @Mock
    private JsonFormat mockJsonFormat;

    @BeforeEach
    void setUp() {
        analyzer = new InstantFormatAnalyzerImpl();
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn("java.time.Instant");
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);
        when(mockJsonFormat.pattern()).thenReturn("");
        when(mockJsonFormat.timezone()).thenReturn("");
        when(mockJsonFormat.locale()).thenReturn("");
    }

    // ========== analyzeInstantField with JsonFormat ==========

    @Test
    void analyzeInstantField_withCustomPattern_setsHasCustomFormat() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertThat(info.isHasCustomFormat()).isTrue();
    }

    @Test
    void analyzeInstantField_withNonAnyShape_setsShape() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.STRING);

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getShape()).isEqualTo("STRING");
    }

    @Test
    void analyzeInstantField_withNumberShape_setsUseTimestamp() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.NUMBER);

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isUseTimestamp()).isTrue();
        assertThat(info.getShape()).isEqualTo("NUMBER");
    }

    @Test
    void analyzeInstantField_withTimezone_setsTimezone() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.timezone()).thenReturn("UTC");

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getTimezone()).isEqualTo("UTC");
    }

    @Test
    void analyzeInstantField_withLocale_setsLocale() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.locale()).thenReturn("en_US");

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getLocale()).isEqualTo("en_US");
    }

    @Test
    void analyzeInstantField_withCustomPatternTimezoneAndLocale_setsAllFields() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd'T'HH:mm:ssXXX");
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.STRING);
        when(mockJsonFormat.timezone()).thenReturn("Europe/Istanbul");
        when(mockJsonFormat.locale()).thenReturn("tr_TR");

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ssXXX");
        assertThat(info.isHasCustomFormat()).isTrue();
        assertThat(info.getShape()).isEqualTo("STRING");
        assertThat(info.getTimezone()).isEqualTo("Europe/Istanbul");
        assertThat(info.getLocale()).isEqualTo("tr_TR");
    }

    @Test
    void analyzeInstantField_withJsonFormatButEmptyPattern_usesDefault() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        // pattern is "" (already stubbed in setUp), shape is ANY, timezone is "", locale is ""

        InstantFormatInfo info = analyzer.analyzeInstantField(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    // ========== hasCustomInstantFormat ==========

    @Test
    void hasCustomInstantFormat_withNonEmptyPattern_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);

        assertThat(analyzer.hasCustomInstantFormat(mockField)).isTrue();
    }

    @Test
    void hasCustomInstantFormat_withNonAnyShape_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("");
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.NUMBER);

        assertThat(analyzer.hasCustomInstantFormat(mockField)).isTrue();
    }

    @Test
    void hasCustomInstantFormat_withEmptyPatternAndAnyShape_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("");
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);

        assertThat(analyzer.hasCustomInstantFormat(mockField)).isFalse();
    }

    // ========== extractCustomInstantPattern ==========

    @Test
    void extractCustomInstantPattern_withNonEmptyPattern_returnsPattern() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");

        assertThat(analyzer.extractCustomInstantPattern(mockField)).isEqualTo("yyyy-MM-dd");
    }

    @Test
    void extractCustomInstantPattern_withEmptyPattern_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("");

        assertThat(analyzer.extractCustomInstantPattern(mockField)).isNull();
    }

    // ========== extractInstantShape ==========

    @Test
    void extractInstantShape_withNonAnyShape_returnsShapeName() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.NUMBER);

        assertThat(analyzer.extractInstantShape(mockField)).isEqualTo("NUMBER");
    }

    @Test
    void extractInstantShape_withAnyShape_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);

        assertThat(analyzer.extractInstantShape(mockField)).isNull();
    }

    @Test
    void extractInstantShape_withNullAnnotation_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        assertThat(analyzer.extractInstantShape(mockField)).isNull();
    }

    // ========== extractInstantTimezone ==========

    @Test
    void extractInstantTimezone_withNonEmptyTimezone_returnsTimezone() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.timezone()).thenReturn("America/New_York");

        assertThat(analyzer.extractInstantTimezone(mockField)).isEqualTo("America/New_York");
    }

    @Test
    void extractInstantTimezone_withEmptyTimezone_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.timezone()).thenReturn("");

        assertThat(analyzer.extractInstantTimezone(mockField)).isNull();
    }

    @Test
    void extractInstantTimezone_withNullAnnotation_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        assertThat(analyzer.extractInstantTimezone(mockField)).isNull();
    }

    // ========== shouldUseTimestampFormat ==========

    @Test
    void shouldUseTimestampFormat_withNumberShape_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.NUMBER);
        when(mockJsonFormat.pattern()).thenReturn("");

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isTrue();
    }

    @Test
    void shouldUseTimestampFormat_withNonNumberShape_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.STRING);
        when(mockJsonFormat.pattern()).thenReturn("");

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isFalse();
    }

    @Test
    void shouldUseTimestampFormat_withTimestampPattern_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);
        when(mockJsonFormat.pattern()).thenReturn("timestamp");

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isTrue();
    }

    @Test
    void shouldUseTimestampFormat_withEpochPattern_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);
        when(mockJsonFormat.pattern()).thenReturn("epoch-millis");

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isTrue();
    }

    @Test
    void shouldUseTimestampFormat_withRegularPattern_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.shape()).thenReturn(JsonFormat.Shape.ANY);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isFalse();
    }

    @Test
    void shouldUseTimestampFormat_withNullAnnotation_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        assertThat(analyzer.shouldUseTimestampFormat(mockField)).isFalse();
    }

    @Test
    void shouldUseTimestampFormat_withNullField_returnsFalse() {
        assertThat(analyzer.shouldUseTimestampFormat(null)).isFalse();
    }

    // ========== isValidInstantFormatInfo ==========

    @Test
    void isValidInstantFormatInfo_withNull_returnsFalse() {
        assertThat(analyzer.isValidInstantFormatInfo(null)).isFalse();
    }

    @Test
    void isValidInstantFormatInfo_withNullPattern_returnsFalse() {
        InstantFormatInfo info = new InstantFormatInfo();
        info.setPattern(null);
        assertThat(analyzer.isValidInstantFormatInfo(info)).isFalse();
    }

    @Test
    void isValidInstantFormatInfo_withEmptyPattern_returnsFalse() {
        InstantFormatInfo info = new InstantFormatInfo();
        info.setPattern("   ");
        assertThat(analyzer.isValidInstantFormatInfo(info)).isFalse();
    }

    @Test
    void isValidInstantFormatInfo_withValidPattern_returnsTrue() {
        InstantFormatInfo info = new InstantFormatInfo();
        info.setPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        assertThat(analyzer.isValidInstantFormatInfo(info)).isTrue();
    }

    // ========== createInstantFormatInfoWithTimezone ==========

    @Test
    void createInstantFormatInfoWithTimezone_returnsInfoWithTimezone() {
        InstantFormatInfo info = analyzer.createInstantFormatInfoWithTimezone("UTC");
        assertThat(info).isNotNull();
        assertThat(info.getTimezone()).isEqualTo("UTC");
    }

    // ========== extractDetailedJsonFormat ==========

    @Test
    void extractDetailedJsonFormat_withNullField_returnsNull() {
        assertThat(analyzer.extractDetailedJsonFormat(null)).isNull();
    }

    @Test
    void extractDetailedJsonFormat_withNonInstantField_returnsNull() {
        when(mockTypeMirror.toString()).thenReturn("java.time.LocalDateTime");

        assertThat(analyzer.extractDetailedJsonFormat(mockField)).isNull();
    }

    @Test
    void extractDetailedJsonFormat_withInstantFieldNoAnnotationMirror_returnsDefault() {
        when(mockField.getAnnotationMirrors()).thenReturn(Collections.emptyList());

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
    }

    @Test
    void extractDetailedJsonFormat_withNonMatchingAnnotationMirror_returnsDefault() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.other.Annotation");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withPatternInAnnotationMirror_setsPattern() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement patternKey = mock(ExecutableElement.class);
        Name patternName = mock(Name.class);
        when(patternName.toString()).thenReturn("pattern");
        when(patternKey.getSimpleName()).thenReturn(patternName);

        AnnotationValue patternValue = mock(AnnotationValue.class);
        when(patternValue.getValue()).thenReturn("yyyy-MM-dd");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(patternKey, patternValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd");
        assertThat(info.isHasCustomFormat()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withShapeInAnnotationMirror_setsShape() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement shapeKey = mock(ExecutableElement.class);
        Name shapeName = mock(Name.class);
        when(shapeName.toString()).thenReturn("shape");
        when(shapeKey.getSimpleName()).thenReturn(shapeName);

        AnnotationValue shapeValue = mock(AnnotationValue.class);
        when(shapeValue.getValue()).thenReturn("NUMBER");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(shapeKey, shapeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getShape()).isEqualTo("NUMBER");
        assertThat(info.isUseTimestamp()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withTimezoneInAnnotationMirror_setsTimezone() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement tzKey = mock(ExecutableElement.class);
        Name tzName = mock(Name.class);
        when(tzName.toString()).thenReturn("timezone");
        when(tzKey.getSimpleName()).thenReturn(tzName);

        AnnotationValue tzValue = mock(AnnotationValue.class);
        when(tzValue.getValue()).thenReturn("Europe/Istanbul");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(tzKey, tzValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getTimezone()).isEqualTo("Europe/Istanbul");
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withLocaleInAnnotationMirror_setsLocale() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement localeKey = mock(ExecutableElement.class);
        Name localeName = mock(Name.class);
        when(localeName.toString()).thenReturn("locale");
        when(localeKey.getSimpleName()).thenReturn(localeName);

        AnnotationValue localeValue = mock(AnnotationValue.class);
        when(localeValue.getValue()).thenReturn("tr_TR");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(localeKey, localeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getLocale()).isEqualTo("tr_TR");
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withUnknownParamInAnnotationMirror_isIgnored() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement unknownKey = mock(ExecutableElement.class);
        Name unknownName = mock(Name.class);
        when(unknownName.toString()).thenReturn("lenient");
        when(unknownKey.getSimpleName()).thenReturn(unknownName);

        AnnotationValue unknownValue = mock(AnnotationValue.class);
        when(unknownValue.getValue()).thenReturn("true");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(unknownKey, unknownValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withEmptyPatternInAnnotationMirror_usesDefault() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement patternKey = mock(ExecutableElement.class);
        Name patternName = mock(Name.class);
        when(patternName.toString()).thenReturn("pattern");
        when(patternKey.getSimpleName()).thenReturn(patternName);

        AnnotationValue patternValue = mock(AnnotationValue.class);
        when(patternValue.getValue()).thenReturn("");  // empty pattern

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(patternKey, patternValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withNullShapeInAnnotationMirror_noTimestamp() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement shapeKey = mock(ExecutableElement.class);
        Name shapeName = mock(Name.class);
        when(shapeName.toString()).thenReturn("shape");
        when(shapeKey.getSimpleName()).thenReturn(shapeName);

        AnnotationValue shapeValue = mock(AnnotationValue.class);
        when(shapeValue.getValue()).thenReturn(null);  // null shape

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(shapeKey, shapeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isUseTimestamp()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withNonNumberShapeInAnnotationMirror_noTimestamp() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement shapeKey = mock(ExecutableElement.class);
        Name shapeName = mock(Name.class);
        when(shapeName.toString()).thenReturn("shape");
        when(shapeKey.getSimpleName()).thenReturn(shapeName);

        AnnotationValue shapeValue = mock(AnnotationValue.class);
        when(shapeValue.getValue()).thenReturn("STRING");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(shapeKey, shapeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isUseTimestamp()).isFalse();
        assertThat(info.getShape()).isEqualTo("STRING");
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withEmptyTimezoneInAnnotationMirror_noTimezone() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement tzKey = mock(ExecutableElement.class);
        Name tzName = mock(Name.class);
        when(tzName.toString()).thenReturn("timezone");
        when(tzKey.getSimpleName()).thenReturn(tzName);

        AnnotationValue tzValue = mock(AnnotationValue.class);
        when(tzValue.getValue()).thenReturn("");  // empty timezone

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(tzKey, tzValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getTimezone()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractDetailedJsonFormat_withEmptyLocaleInAnnotationMirror_noLocale() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.fasterxml.jackson.annotation.JsonFormat");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);

        ExecutableElement localeKey = mock(ExecutableElement.class);
        Name localeName = mock(Name.class);
        when(localeName.toString()).thenReturn("locale");
        when(localeKey.getSimpleName()).thenReturn(localeName);

        AnnotationValue localeValue = mock(AnnotationValue.class);
        when(localeValue.getValue()).thenReturn("");  // empty locale

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(localeKey, localeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        InstantFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getLocale()).isNull();
    }
}
