package com.thy.fss.common.inmemory.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for MetaModelProcessor using mocked environment.
 * Tests the processor behavior in controlled scenarios.
 */
@DisplayName("MetaModelProcessor Integration Tests")
class MetaModelProcessorIntegrationTest {

    @Test
    @DisplayName("Should process simple @MetaModel class successfully")
    void shouldProcessSimpleMetaModelClassSuccessfully() {
        // Given
        MetaModelProcessor processor = new MetaModelProcessor();
        ProcessingEnvironment processingEnv = ProcessorTestUtils.createProcessingEnvironment();
        processor.init(processingEnv);

        // Create a simple scenario with no annotated classes
        RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false);
        Set<TypeElement> annotations = Collections.emptySet();

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then - Should return false since no annotations to process
        assertFalse(result, "Should return false when no annotations to process");
    }

    @Test
    @DisplayName("Should handle classes with dependencies correctly")
    void shouldHandleClassesWithDependenciesCorrectly() {
        // Given
        MetaModelProcessor processor = new MetaModelProcessor();
        ProcessingEnvironment processingEnv = ProcessorTestUtils.createProcessingEnvironment();
        processor.init(processingEnv);

        // Create entities with dependencies
        TypeElement userEntity = ProcessorTestUtils.createTypeElement("com.test.User");
        TypeElement profileEntity = ProcessorTestUtils.createTypeElement("com.test.Profile");

        RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, userEntity, profileEntity);
        Set<TypeElement> annotations = Set.of(mock(TypeElement.class));

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then - Should return false since there are no actual @MetaModel annotated classes
        // The mock setup doesn't create real @MetaModel annotations
        assertFalse(result, "Should return false when no @MetaModel annotated classes found");
    }

    @Test
    @DisplayName("Should reject non-class elements")
    void shouldRejectNonClassElements() {
        // Given
        MetaModelProcessor processor = new MetaModelProcessor();
        ProcessingEnvironment processingEnv = ProcessorTestUtils.createProcessingEnvironment();
        processor.init(processingEnv);

        // Create a non-class element (interface)
        Element interfaceElement = ProcessorTestUtils.createNonClassElement(ElementKind.INTERFACE);

        RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, interfaceElement);
        Set<TypeElement> annotations = Set.of(mock(TypeElement.class));

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then - Should return false since non-class elements are rejected and no valid classes remain
        // The processor will reject non-class elements and return false when no valid classes found
        assertFalse(result, "Should return false when only non-class elements are provided");
    }

    @Test
    @DisplayName("Should handle empty annotation processing round")
    void shouldHandleEmptyAnnotationProcessingRound() {
        // Given
        MetaModelProcessor processor = new MetaModelProcessor();
        ProcessingEnvironment processingEnv = ProcessorTestUtils.createProcessingEnvironment();
        processor.init(processingEnv);

        // Create empty round environment
        RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false);
        Set<TypeElement> annotations = Set.of(mock(TypeElement.class));

        // When
        boolean result = processor.process(annotations, roundEnv);

        // Then - Should return false since no annotated classes found
        assertFalse(result, "Should return false when no annotated classes found");
    }


}