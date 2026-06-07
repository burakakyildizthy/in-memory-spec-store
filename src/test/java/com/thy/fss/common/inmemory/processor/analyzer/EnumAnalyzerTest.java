package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnumAnalyzerTest {

    @Mock
    private Elements elementUtils;

    @Mock
    private Types typeUtils;

    @Mock
    private TypeElement enumElement;

    @Mock
    private Name enumName;

    private EnumAnalyzer enumAnalyzer;

    @BeforeEach
    void setUp() {
        enumAnalyzer = new EnumAnalyzer(elementUtils, typeUtils);
    }

    @Test
    void analyzeEnumWithNullElementThrowsException() {
        assertThatThrownBy(() -> enumAnalyzer.analyzeEnum(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Element must be an enum type");
    }

    @Test
    void analyzeEnumWithNonEnumElementThrowsException() {
        when(enumElement.getKind()).thenReturn(ElementKind.CLASS);

        assertThatThrownBy(() -> enumAnalyzer.analyzeEnum(enumElement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Element must be an enum type");
    }

    @Test
    void analyzeEnumWithNoJacksonAnnotationsReturnsDefaultMatching() {
        // Setup enum with no Jackson annotations
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);
        when(enumElement.getQualifiedName()).thenReturn(enumName);
        when(enumName.toString()).thenReturn("com.example.TestEnum");
        when(enumElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        EnumDeserializationInfo result = enumAnalyzer.analyzeEnum(enumElement);

        assertThat(result.getEnumClassName()).isEqualTo("com.example.TestEnum");
        assertThat(result.getDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        assertThat(result.hasCustomDeserialization()).isFalse();
        assertThat(result.getDeserializationTarget()).isNull();
    }

    @Test
    void isEnumWithEnumElementReturnsTrue() {
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);

        boolean result = enumAnalyzer.isEnum(enumElement);

        assertThat(result).isTrue();
    }

    @Test
    void isEnumWithNonEnumElementReturnsFalse() {
        when(enumElement.getKind()).thenReturn(ElementKind.CLASS);

        boolean result = enumAnalyzer.isEnum(enumElement);

        assertThat(result).isFalse();
    }

    @Test
    void isEnumWithNullElementReturnsFalse() {
        boolean result = enumAnalyzer.isEnum(null);

        assertThat(result).isFalse();
    }
}