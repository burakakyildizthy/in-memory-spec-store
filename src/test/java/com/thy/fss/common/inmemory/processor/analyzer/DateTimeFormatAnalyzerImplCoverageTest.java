package com.thy.fss.common.inmemory.processor.analyzer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
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
 * Additional coverage tests for DateTimeFormatAnalyzerImpl targeting missed branches.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DateTimeFormatAnalyzerImplCoverageTest {

    private static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";
    private static final String LOCAL_DATE = "java.time.LocalDate";
    private static final String INSTANT = "java.time.Instant";

    private DateTimeFormatAnalyzerImpl analyzer;

    @Mock
    private VariableElement mockField;

    @Mock
    private TypeMirror mockTypeMirror;

    @Mock
    private JsonFormat mockJsonFormat;

    @BeforeEach
    void setUp() {
        analyzer = new DateTimeFormatAnalyzerImpl();
        when(mockField.asType()).thenReturn(mockTypeMirror);
        when(mockTypeMirror.toString()).thenReturn(LOCAL_DATE_TIME);
        when(mockJsonFormat.pattern()).thenReturn("");
        when(mockJsonFormat.timezone()).thenReturn("");
        when(mockJsonFormat.locale()).thenReturn("");
    }

    // ========== analyzeDateTimeField with JsonFormat with timezone/locale ==========

    @Test
    void analyzeDateTimeField_withCustomPatternAndTimezone_setsTimezone() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd HH:mm:ss");
        when(mockJsonFormat.timezone()).thenReturn("UTC");

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getTimezone()).isEqualTo("UTC");
        assertThat(result.isHasCustomFormat()).isTrue();
    }

    @Test
    void analyzeDateTimeField_withCustomPatternAndLocale_setsLocale() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("dd/MM/yyyy");
        when(mockJsonFormat.locale()).thenReturn("tr_TR");

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isEqualTo("tr_TR");
    }

    @Test
    void analyzeDateTimeField_withCustomPatternButEmptyTimezone_noTimezone() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");
        when(mockJsonFormat.timezone()).thenReturn("");

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getTimezone()).isNull();
    }

    @Test
    void analyzeDateTimeField_withCustomPatternButEmptyLocale_noLocale() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");
        when(mockJsonFormat.locale()).thenReturn("");

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getLocale()).isNull();
    }

    @Test
    void analyzeDateTimeField_withLocalDate_usesDefaultLocalDatePattern() {
        when(mockTypeMirror.toString()).thenReturn(LOCAL_DATE);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    void analyzeDateTimeField_withInstant_usesDefaultInstantPattern() {
        when(mockTypeMirror.toString()).thenReturn(INSTANT);
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        DateTimeFormatInfo result = analyzer.analyzeDateTimeField(mockField);

        assertThat(result).isNotNull();
        assertThat(result.getPattern()).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    // ========== hasCustomDateTimeFormat ==========

    @Test
    void hasCustomDateTimeFormat_withNonEmptyAndNonBlankPattern_returnsTrue() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("yyyy-MM-dd");

        assertThat(analyzer.hasCustomDateTimeFormat(mockField)).isTrue();
    }

    @Test
    void hasCustomDateTimeFormat_withBlankPattern_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("   ");

        assertThat(analyzer.hasCustomDateTimeFormat(mockField)).isFalse();
    }

    @Test
    void hasCustomDateTimeFormat_withNullAnnotation_returnsFalse() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        assertThat(analyzer.hasCustomDateTimeFormat(mockField)).isFalse();
    }

    @Test
    void hasCustomDateTimeFormat_withNullField_returnsFalse() {
        assertThat(analyzer.hasCustomDateTimeFormat(null)).isFalse();
    }

    // ========== extractCustomPattern ==========

    @Test
    void extractCustomPattern_withNonEmptyPattern_returnsPattern() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("dd-MM-yyyy");

        assertThat(analyzer.extractCustomPattern(mockField)).isEqualTo("dd-MM-yyyy");
    }

    @Test
    void extractCustomPattern_withBlankPattern_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(mockJsonFormat);
        when(mockJsonFormat.pattern()).thenReturn("  ");

        assertThat(analyzer.extractCustomPattern(mockField)).isNull();
    }

    @Test
    void extractCustomPattern_withNullAnnotation_returnsNull() {
        when(mockField.getAnnotation(JsonFormat.class)).thenReturn(null);

        assertThat(analyzer.extractCustomPattern(mockField)).isNull();
    }

    @Test
    void extractCustomPattern_withNullField_returnsNull() {
        assertThat(analyzer.extractCustomPattern(null)).isNull();
    }

    // ========== getFieldType ==========

    @Test
    void getFieldType_withNullField_returnsNull() {
        assertThat(analyzer.getFieldType(null)).isNull();
    }

    @Test
    void getFieldType_withField_returnsTypeName() {
        assertThat(analyzer.getFieldType(mockField)).isEqualTo(LOCAL_DATE_TIME);
    }

    // ========== getDefaultPatternForType ==========

    @Test
    void getDefaultPatternForType_withNull_returnsLocalDateTimeDefault() {
        assertThat(analyzer.getDefaultPatternForType(null)).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    void getDefaultPatternForType_withLocalDateTime_returnsLocalDateTimeDefault() {
        assertThat(analyzer.getDefaultPatternForType(LOCAL_DATE_TIME)).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    @Test
    void getDefaultPatternForType_withLocalDate_returnsLocalDateDefault() {
        assertThat(analyzer.getDefaultPatternForType(LOCAL_DATE)).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_PATTERN);
    }

    @Test
    void getDefaultPatternForType_withInstant_returnsInstantDefault() {
        assertThat(analyzer.getDefaultPatternForType(INSTANT)).isEqualTo(FilterConstants.DEFAULT_INSTANT_PATTERN);
    }

    @Test
    void getDefaultPatternForType_withUnknownType_returnsLocalDateTimeDefault() {
        assertThat(analyzer.getDefaultPatternForType("java.lang.String")).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
    }

    // ========== isValidDateTimeFormatInfo ==========

    @Test
    void isValidDateTimeFormatInfo_withNull_returnsFalse() {
        assertThat(analyzer.isValidDateTimeFormatInfo(null)).isFalse();
    }

    @Test
    void isValidDateTimeFormatInfo_withNullPattern_returnsFalse() {
        DateTimeFormatInfo info = new DateTimeFormatInfo();
        info.setPattern(null);
        assertThat(analyzer.isValidDateTimeFormatInfo(info)).isFalse();
    }

    @Test
    void isValidDateTimeFormatInfo_withBlankPattern_returnsFalse() {
        DateTimeFormatInfo info = new DateTimeFormatInfo();
        info.setPattern("  ");
        assertThat(analyzer.isValidDateTimeFormatInfo(info)).isFalse();
    }

    @Test
    void isValidDateTimeFormatInfo_withValidPattern_returnsTrue() {
        DateTimeFormatInfo info = new DateTimeFormatInfo();
        info.setPattern("yyyy-MM-dd");
        assertThat(analyzer.isValidDateTimeFormatInfo(info)).isTrue();
    }

    // ========== createCustomFormatInfo and createDefaultFormatInfo ==========

    @Test
    void createCustomFormatInfo_returnsInfoWithCustomPattern() {
        DateTimeFormatInfo info = analyzer.createCustomFormatInfo(LOCAL_DATE_TIME, "dd/MM/yyyy HH:mm");
        assertThat(info.getPattern()).isEqualTo("dd/MM/yyyy HH:mm");
        assertThat(info.isHasCustomFormat()).isTrue();
    }

    @Test
    void createDefaultFormatInfo_returnsInfoWithDefaultPattern() {
        DateTimeFormatInfo info = analyzer.createDefaultFormatInfo(LOCAL_DATE_TIME);
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
    }

    // ========== extractDetailedJsonFormat ==========

    @Test
    void extractDetailedJsonFormat_withNullField_returnsNull() {
        assertThat(analyzer.extractDetailedJsonFormat(null)).isNull();
    }

    @Test
    void extractDetailedJsonFormat_withNonTemporalField_returnsNull() {
        when(mockTypeMirror.toString()).thenReturn("java.lang.String");
        assertThat(analyzer.extractDetailedJsonFormat(mockField)).isNull();
    }

    @Test
    void extractDetailedJsonFormat_withTemporalFieldNoAnnotationMirror_returnsDefault() {
        when(mockField.getAnnotationMirrors()).thenReturn(Collections.emptyList());

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
        assertThat(info.isHasCustomFormat()).isFalse();
    }

    @Test
    void extractDetailedJsonFormat_withNonMatchingAnnotationMirror_returnsDefault() {
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType mirrorType = mock(DeclaredType.class);
        when(mirrorType.toString()).thenReturn("com.other.Annotation");
        when(mirror.getAnnotationType()).thenReturn(mirrorType);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
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
        when(patternValue.getValue()).thenReturn("yyyy-MM-dd HH:mm:ss");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(patternKey, patternValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd HH:mm:ss");
        assertThat(info.isHasCustomFormat()).isTrue();
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

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

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

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

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
        when(unknownName.toString()).thenReturn("shape");
        when(unknownKey.getSimpleName()).thenReturn(unknownName);

        AnnotationValue unknownValue = mock(AnnotationValue.class);
        when(unknownValue.getValue()).thenReturn("NUMBER");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(unknownKey, unknownValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
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
        when(patternValue.getValue()).thenReturn("");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(patternKey, patternValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.isHasCustomFormat()).isFalse();
        assertThat(info.getPattern()).isEqualTo(FilterConstants.DEFAULT_LOCAL_DATE_TIME_PATTERN);
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
        when(tzValue.getValue()).thenReturn("");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(tzKey, tzValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

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
        when(localeValue.getValue()).thenReturn("");

        Map<ExecutableElement, AnnotationValue> elementValues = Map.of(localeKey, localeValue);
        when(mirror.getElementValues()).thenReturn((Map) elementValues);
        when(mockField.getAnnotationMirrors()).thenReturn((List) List.of(mirror));

        DateTimeFormatInfo info = analyzer.extractDetailedJsonFormat(mockField);

        assertThat(info).isNotNull();
        assertThat(info.getLocale()).isNull();
    }
}
