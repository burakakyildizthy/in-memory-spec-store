package com.thy.fss.common.inmemory.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic tests for MetaModelProcessor that actually call the real process method
 * to ensure code coverage.
 */
@DisplayName("MetaModelProcessor Basic Tests")
class MetaModelProcessorBasicTest {

    private MetaModelProcessor processor;
    private ProcessingEnvironment processingEnv;
    private RoundEnvironment roundEnv;

    @BeforeEach
    void setUp() {
        processor = new MetaModelProcessor();
        processingEnv = ProcessorTestUtils.createProcessingEnvironment();
        roundEnv = mock(RoundEnvironment.class);

        // Initialize processor
        processor.init(processingEnv);
    }

    @Test
    @DisplayName("Should return false when annotations set is empty")
    void shouldReturnFalseWhenAnnotationsEmpty() {
        // Given
        Set<TypeElement> emptyAnnotations = Collections.emptySet();

        // When
        boolean result = processor.process(emptyAnnotations, roundEnv);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when processing is over")
    void shouldReturnFalseWhenProcessingOver() {
        // Given
        TypeElement annotation = ProcessorTestUtils.createTypeElement("com.test.Annotation");
        Set<TypeElement> annotations = Set.of(annotation);
        when(roundEnv.processingOver()).thenReturn(true);

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when no annotated classes found")
    void shouldReturnFalseWhenNoAnnotatedClassesFound() {
        // Given
        TypeElement annotation = ProcessorTestUtils.createTypeElement("com.test.Annotation");
        Set<TypeElement> annotations = Set.of(annotation);
        when(roundEnv.processingOver()).thenReturn(false);
        when(roundEnv.getElementsAnnotatedWith(MetaModel.class)).thenReturn(Collections.emptySet());

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should initialize processor without errors")
    void shouldInitializeProcessorWithoutErrors() {
        // Given
        MetaModelProcessor newProcessor = new MetaModelProcessor();

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> newProcessor.init(processingEnv));
    }

    @Test
    @DisplayName("Should handle multiple process calls")
    void shouldHandleMultipleProcessCalls() {
        // Given
        Set<TypeElement> emptyAnnotations = Collections.emptySet();

        // When
        boolean result1 = processor.process(emptyAnnotations, roundEnv);
        boolean result2 = processor.process(emptyAnnotations, roundEnv);

        // Then
        assertFalse(result1);
        assertFalse(result2);
    }
}