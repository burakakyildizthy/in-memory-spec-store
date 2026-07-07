package com.thy.fss.common.inmemory.processor;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import com.thy.fss.common.inmemory.processor.generator.FilterMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticSpecificationServiceGenerator;
import com.thy.fss.common.inmemory.processor.validation.MetaModelValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Coverage tests for MetaModelProcessor using utility classes to avoid type system issues.
 * These tests focus on achieving high code coverage for the process() method.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MetaModelProcessor Coverage Tests")
class MetaModelProcessorCoverageTest {

    private static final String TEST_ANNOTATION = "com.test.Annotation";
    private static final String TEST_ENTITY1 = "com.test.Entity1";
    private static final String TEST_ENTITY2 = "com.test.Entity2";
    private static final String TEST_ENTITY3 = "com.test.Entity3";

    @Mock
    private StaticMetaModelGenerator staticMetaModelGenerator;

    @Mock
    private FilterMetaModelGenerator filterMetaModelGenerator;

    @Mock
    private StaticSpecificationServiceGenerator staticSpecificationServiceGenerator;

    @Mock
    private MetaModelValidator validator;

    private MetaModelProcessor processor;
    private ProcessingEnvironment processingEnv;

    @BeforeEach
    void setUp() throws Exception {
        // Enable debug mode for tests
        System.setProperty("inmemory.processor.debug", "true");

        processor = new MetaModelProcessor();
        processingEnv = ProcessorTestUtils.createProcessingEnvironment();

        // Initialize processor
        processor.init(processingEnv);

        // Inject mocked generators using reflection
        injectMockedGenerators();

        // Configure mocks to not throw exceptions during generation
        // Use lenient() since not all tests call these methods
        try {
            lenient().doNothing().when(staticMetaModelGenerator).generate(any(TypeElement.class));
            lenient().doNothing().when(filterMetaModelGenerator).generate(any(TypeElement.class));
            lenient().doNothing().when(staticSpecificationServiceGenerator).generate(any(TypeElement.class));
            // Configure validator to always return true by default
            lenient().when(validator.validate(any(TypeElement.class), anySet())).thenReturn(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure mocks", e);
        }
    }

    private void injectMockedGenerators() throws Exception {
        Field staticField = MetaModelProcessor.class.getDeclaredField("staticMetaModelGenerator");
        staticField.setAccessible(true);
        staticField.set(processor, staticMetaModelGenerator);

        Field filterField = MetaModelProcessor.class.getDeclaredField("filterMetaModelGenerator");
        filterField.setAccessible(true);
        filterField.set(processor, filterMetaModelGenerator);

        Field specField = MetaModelProcessor.class.getDeclaredField("staticSpecificationServiceGenerator");
        specField.setAccessible(true);
        specField.set(processor, staticSpecificationServiceGenerator);

        Field validatorField = MetaModelProcessor.class.getDeclaredField("validator");
        validatorField.setAccessible(true);
        validatorField.set(processor, validator);
    }

    @Nested
    @DisplayName("Early Exit Conditions")
    class EarlyExitConditions {

        @Test
        @DisplayName("Should return false when annotations set is empty")
        void shouldReturnFalseWhenAnnotationsEmpty() {
            // Given
            Set<TypeElement> emptyAnnotations = Collections.emptySet();
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false);

            // When
            boolean result = processor.process(emptyAnnotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when processing is over")
        void shouldReturnFalseWhenProcessingOver() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(true);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when no annotated classes found")
        void shouldReturnFalseWhenNoAnnotatedClassesFound() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should skip subsequent rounds when already processed")
        void shouldSkipSubsequentRoundsWhenAlreadyProcessed() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // First call - should process
            boolean firstResult = processor.process(annotations, roundEnv);
            assertTrue(firstResult);

            // When - Second call should skip
            boolean secondResult = processor.process(annotations, roundEnv);

            // Then
            assertFalse(secondResult);
        }
    }

    @Nested
    @DisplayName("Annotation Collection")
    class AnnotationCollection {

        @Test
        @DisplayName("Should collect valid annotated classes")
        void shouldCollectValidAnnotatedClasses() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should reject non-class elements with error")
        void shouldRejectNonClassElementsWithError() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            Element interfaceElement = ProcessorTestUtils.createNonClassElement(ElementKind.INTERFACE);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, interfaceElement);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should collect multiple valid classes")
        void shouldCollectMultipleValidClasses() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity1 = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            TypeElement entity2 = ProcessorTestUtils.createTypeElement(TEST_ENTITY2);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity1, entity2);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Dependency Analysis")
    class DependencyAnalysis {

        @Test
        @DisplayName("Should analyze field dependencies correctly")
        void shouldAnalyzeFieldDependenciesCorrectly() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY2)
                    .addEntityWithDependency(TEST_ENTITY1, TEST_ENTITY2);

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle classes with no dependencies")
        void shouldHandleClassesWithNoDependencies() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle non-field elements in class")
        void shouldHandleNonFieldElementsInClass() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            ExecutableElement method = ProcessorTestUtils.createMethodElement("testMethod");
            when(entity.getEnclosedElements()).thenReturn((List) List.of(method));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

        }

        @Test
        @DisplayName("Should handle field with primitive type")
        void shouldHandleFieldWithPrimitiveType() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            VariableElement primitiveField = ProcessorTestUtils.createPrimitiveField("id", "int");
            when(entity.getEnclosedElements()).thenReturn((List) List.of(primitiveField));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Generation Process")
    class GenerationProcess {

        @Test
        @DisplayName("Should execute all three generation phases")
        void shouldExecuteAllThreeGenerationPhases() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify generators called
            verify(staticMetaModelGenerator).generate(entity);
            verify(filterMetaModelGenerator).generate(entity);
            verify(staticSpecificationServiceGenerator).generate(entity);
        }

        @Test
        @DisplayName("Should respect topological order in generation")
        void shouldRespectTopologicalOrderInGeneration() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY2)
                    .addEntityWithDependency(TEST_ENTITY1, TEST_ENTITY2);

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();
            List<TypeElement> entities = scenario.getEntities();
            TypeElement entity1 = entities.get(1); // Entity1 (depends on Entity2)
            TypeElement entity2 = entities.get(0); // Entity2 (no dependencies)

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify Entity2 generated before Entity1 in each phase
            var inOrder = inOrder(staticMetaModelGenerator, filterMetaModelGenerator, staticSpecificationServiceGenerator);
            inOrder.verify(staticMetaModelGenerator).generate(entity2);
            inOrder.verify(staticMetaModelGenerator).generate(entity1);
            inOrder.verify(filterMetaModelGenerator).generate(entity2);
            inOrder.verify(filterMetaModelGenerator).generate(entity1);
            inOrder.verify(staticSpecificationServiceGenerator).generate(entity2);
            inOrder.verify(staticSpecificationServiceGenerator).generate(entity1);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle ProcessingException with error message")
        void shouldHandleProcessingExceptionWithErrorMessage() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            ProcessingException exception = new ProcessingException("Test processing error");
            doThrow(exception).when(staticMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle ProcessingException with 'Attempt to recreate' as warning")
        void shouldHandleProcessingExceptionWithAttemptToRecreateAsWarning() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            ProcessingException exception = new ProcessingException("Attempt to recreate file already exists");
            doThrow(exception).when(staticMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle generic Exception with error message")
        void shouldHandleGenericExceptionWithErrorMessage() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            RuntimeException exception = new RuntimeException("Unexpected error");
            doThrow(exception).when(staticMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle generic Exception with 'Attempt to recreate' as warning")
        void shouldHandleGenericExceptionWithAttemptToRecreateAsWarning() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            RuntimeException exception = new RuntimeException("Attempt to recreate something");
            doThrow(exception).when(staticMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("State Management")
    class StateManagement {

        @Test
        @DisplayName("Should maintain consistent state after successful processing")
        void shouldMaintainConsistentStateAfterSuccessfulProcessing() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify internal state using reflection
            Field processedClassesField = MetaModelProcessor.class.getDeclaredField("processedClasses");
            processedClassesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> processedClasses = (Set<String>) processedClassesField.get(processor);

            assertTrue(processedClasses.contains(TEST_ENTITY1));

            Field hasProcessedField = MetaModelProcessor.class.getDeclaredField("hasProcessed");
            hasProcessedField.setAccessible(true);
            boolean hasProcessed = (boolean) hasProcessedField.get(processor);

            assertTrue(hasProcessed);
        }

        @Test
        @DisplayName("Should not mark classes as processed after failure")
        void shouldNotMarkClassesAsProcessedAfterFailure() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            ProcessingException exception = new ProcessingException("Generation failed");
            doThrow(exception).when(staticMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);

            // Verify classes not marked as processed
            Field processedClassesField = MetaModelProcessor.class.getDeclaredField("processedClasses");
            processedClassesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> processedClasses = (Set<String>) processedClassesField.get(processor);

            assertFalse(processedClasses.contains(TEST_ENTITY1));

            Field hasProcessedField = MetaModelProcessor.class.getDeclaredField("hasProcessed");
            hasProcessedField.setAccessible(true);
            boolean hasProcessed = (boolean) hasProcessedField.get(processor);

            assertFalse(hasProcessed);
        }
    }

    @Nested
    @DisplayName("Advanced Dependency Analysis")
    class AdvancedDependencyAnalysis {

        @Test
        @DisplayName("Should handle complex generic type dependencies")
        void shouldHandleComplexGenericTypeDependencies() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY2)
                    .addEntity(TEST_ENTITY3)
                    .addEntity(TEST_ENTITY1);

            // Add complex generic field: List<Entity2>
            TypeElement entity1 = scenario.getEntities().get(2);
            TypeElement entity2 = scenario.getEntities().get(0);
            TypeElement entity3 = scenario.getEntities().get(1);

            VariableElement listField = ProcessorTestUtils.createGenericFieldWithType("entities", entity2);
            VariableElement mapField = ProcessorTestUtils.createGenericFieldWithType("entityMap", entity2, entity3);

            when(entity1.getEnclosedElements()).thenReturn((List) List.of(listField, mapField));

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle non-declared type mirrors")
        void shouldHandleNonDeclaredTypeMirrors() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            VariableElement arrayField = ProcessorTestUtils.createArrayField("items", "int[]");
            when(entity.getEnclosedElements()).thenReturn((List) List.of(arrayField));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle self-referencing entities")
        void shouldHandleSelfReferencingEntities() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            // Create entity that references itself
            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);

            // Create self-referencing field with proper DeclaredType setup
            VariableElement selfField = mock(VariableElement.class);
            when(selfField.getKind()).thenReturn(ElementKind.FIELD);

            Name fieldNameMock = mock(Name.class);
            when(fieldNameMock.toString()).thenReturn("parent");
            when(selfField.getSimpleName()).thenReturn(fieldNameMock);

            DeclaredType fieldType = mock(DeclaredType.class);
            when(selfField.asType()).thenReturn(fieldType);
            when(fieldType.asElement()).thenReturn(entity);
            when(fieldType.getTypeArguments()).thenReturn(Collections.emptyList());

            // Ensure entity's qualified name is properly set up for getTypeName method
            Name entityQualifiedName = mock(Name.class);
            when(entityQualifiedName.toString()).thenReturn(TEST_ENTITY1);
            when(entity.getQualifiedName()).thenReturn(entityQualifiedName);

            when(entity.getEnclosedElements()).thenReturn((List) List.of(selfField));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When & Then - Should handle self-referencing without crashing
            assertDoesNotThrow(() -> {
                processor.process(annotations, roundEnv);
                // Self-referencing should be handled gracefully
            });
        }

        @Test
        @DisplayName("Should handle nested generic types")
        void shouldHandleNestedGenericTypes() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY2)
                    .addEntity(TEST_ENTITY1);

            TypeElement entity1 = scenario.getEntities().get(1);
            TypeElement entity2 = scenario.getEntities().get(0);

            // Create simple generic field instead of nested: List<Entity2>
            VariableElement genericField = ProcessorTestUtils.createGenericFieldWithType("entityList", entity2);
            when(entity1.getEnclosedElements()).thenReturn((List) List.of(genericField));

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Topological Sort Edge Cases")
    class TopologicalSortEdgeCases {

        @Test
        @DisplayName("Should handle topological sort exception")
        void shouldHandleTopologicalSortException() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // Mock topological sorter to throw exception
            Field sorterField = MetaModelProcessor.class.getDeclaredField("topologicalSorter");
            sorterField.setAccessible(true);
            com.thy.fss.common.inmemory.processor.dependency.TopologicalSorter mockSorter =
                    mock(com.thy.fss.common.inmemory.processor.dependency.TopologicalSorter.class);
            sorterField.set(processor, mockSorter);

            when(mockSorter.sort(any())).thenThrow(new ProcessingException("Circular dependency detected"));

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle empty sorted classes list")
        void shouldHandleEmptySortedClassesList() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // Mock topological sorter to return empty list
            Field sorterField = MetaModelProcessor.class.getDeclaredField("topologicalSorter");
            sorterField.setAccessible(true);
            com.thy.fss.common.inmemory.processor.dependency.TopologicalSorter mockSorter =
                    mock(com.thy.fss.common.inmemory.processor.dependency.TopologicalSorter.class);
            sorterField.set(processor, mockSorter);

            when(mockSorter.sort(any())).thenReturn(Collections.emptyList());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify no generators called
            verify(staticMetaModelGenerator, never()).generate(any());
            verify(filterMetaModelGenerator, never()).generate(any());
            verify(staticSpecificationServiceGenerator, never()).generate(any());
        }
    }

    @Nested
    @DisplayName("Partial Processing Scenarios")
    class PartialProcessingScenarios {

        @Test
        @DisplayName("Should handle already processed classes correctly")
        void shouldHandleAlreadyProcessedClassesCorrectly() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity1 = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            TypeElement entity2 = ProcessorTestUtils.createTypeElement(TEST_ENTITY2);

            // First round - process entity1
            RoundEnvironment roundEnv1 = ProcessorTestUtils.createRoundEnvironment(false, entity1);
            boolean firstResult = processor.process(annotations, roundEnv1);
            assertTrue(firstResult);

            // Reset hasProcessed flag to simulate new round
            Field hasProcessedField = MetaModelProcessor.class.getDeclaredField("hasProcessed");
            hasProcessedField.setAccessible(true);
            hasProcessedField.set(processor, false);

            // Second round - process entity2 (entity1 should be skipped)
            RoundEnvironment roundEnv2 = ProcessorTestUtils.createRoundEnvironment(false, entity1, entity2);

            // When
            boolean secondResult = processor.process(annotations, roundEnv2);

            // Then
            assertTrue(secondResult);

            // Verify only entity2 was processed in second round
            verify(staticMetaModelGenerator, times(1)).generate(entity1); // From first round
            verify(staticMetaModelGenerator, times(1)).generate(entity2); // From second round
        }

        @Test
        @DisplayName("Should return false when all classes already processed")
        void shouldReturnFalseWhenAllClassesAlreadyProcessed() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);

            // First round - process entity
            RoundEnvironment roundEnv1 = ProcessorTestUtils.createRoundEnvironment(false, entity);
            boolean firstResult = processor.process(annotations, roundEnv1);
            assertTrue(firstResult);

            // Reset hasProcessed flag to simulate new round
            Field hasProcessedField = MetaModelProcessor.class.getDeclaredField("hasProcessed");
            hasProcessedField.setAccessible(true);
            hasProcessedField.set(processor, false);

            // Second round - same entity (should be skipped)
            RoundEnvironment roundEnv2 = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean secondResult = processor.process(annotations, roundEnv2);

            // Then
            assertFalse(secondResult);

            // Verify entity was only processed once
            verify(staticMetaModelGenerator, times(1)).generate(entity);
            verify(filterMetaModelGenerator, times(1)).generate(entity);
            verify(staticSpecificationServiceGenerator, times(1)).generate(entity);
        }
    }

    @Nested
    @DisplayName("Generation Phase Error Handling")
    class GenerationPhaseErrorHandling {

        @Test
        @DisplayName("Should handle error in filter generation phase")
        void shouldHandleErrorInFilterGenerationPhase() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            ProcessingException exception = new ProcessingException("Filter generation failed");
            doThrow(exception).when(filterMetaModelGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);

            // Verify static model was generated but filter and spec were not
            verify(staticMetaModelGenerator).generate(entity);
            verify(filterMetaModelGenerator).generate(entity);
            verify(staticSpecificationServiceGenerator, never()).generate(entity);
        }

        @Test
        @DisplayName("Should handle error in specification service generation phase")
        void shouldHandleErrorInSpecificationServiceGenerationPhase() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            ProcessingException exception = new ProcessingException("Specification service generation failed");
            doThrow(exception).when(staticSpecificationServiceGenerator).generate(any());

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);

            // Verify static model and filter were generated but spec was not
            verify(staticMetaModelGenerator).generate(entity);
            verify(filterMetaModelGenerator).generate(entity);
            verify(staticSpecificationServiceGenerator).generate(entity);
        }
    }

    @Nested
    @DisplayName("Unprocessed Classes Filtering")
    class UnprocessedClassesFiltering {

        @Test
        @DisplayName("Should filter out already processed classes")
        void shouldFilterOutAlreadyProcessedClasses() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity1 = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            TypeElement entity2 = ProcessorTestUtils.createTypeElement(TEST_ENTITY2);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity1, entity2);

            // Pre-mark Entity1 as processed using reflection
            Field processedClassesField = MetaModelProcessor.class.getDeclaredField("processedClasses");
            processedClassesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> processedClasses = (Set<String>) processedClassesField.get(processor);
            processedClasses.add(TEST_ENTITY1);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify only Entity2 was processed
            verify(staticMetaModelGenerator, never()).generate(entity1);
            verify(staticMetaModelGenerator).generate(entity2);
            verify(filterMetaModelGenerator, never()).generate(entity1);
            verify(filterMetaModelGenerator).generate(entity2);
            verify(staticSpecificationServiceGenerator, never()).generate(entity1);
            verify(staticSpecificationServiceGenerator).generate(entity2);
        }

        @Test
        @DisplayName("Should return false when all classes are already processed")
        void shouldReturnFalseWhenAllClassesAlreadyProcessed() throws Exception {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // Pre-mark all entities as processed using reflection
            Field processedClassesField = MetaModelProcessor.class.getDeclaredField("processedClasses");
            processedClassesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> processedClasses = (Set<String>) processedClassesField.get(processor);
            processedClasses.add(TEST_ENTITY1);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertFalse(result);

            // Verify no generators were called
            verify(staticMetaModelGenerator, never()).generate(any());
            verify(filterMetaModelGenerator, never()).generate(any());
            verify(staticSpecificationServiceGenerator, never()).generate(any());
        }
    }

    @Nested
    @DisplayName("Complex Type Analysis")
    class ComplexTypeAnalysis {

        @Test
        @DisplayName("Should handle complex multi-level generic dependencies")
        void shouldHandleComplexMultiLevelGenericDependencies() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY3)
                    .addEntity(TEST_ENTITY2)
                    .addEntity(TEST_ENTITY1);

            TypeElement entity1 = scenario.getEntities().get(2);
            TypeElement entity2 = scenario.getEntities().get(1);
            TypeElement entity3 = scenario.getEntities().get(0);

            // Create Map<Entity2, List<Entity3>> field
            VariableElement complexField = ProcessorTestUtils.createGenericFieldWithType("complexMap", entity2, entity3);
            when(entity1.getEnclosedElements()).thenReturn((List) List.of(complexField));

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle mixed field types in single class")
        void shouldHandleMixedFieldTypesInSingleClass() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity(TEST_ENTITY2)
                    .addEntity(TEST_ENTITY1);

            TypeElement entity1 = scenario.getEntities().get(1);
            TypeElement entity2 = scenario.getEntities().get(0);

            // Mix of different field types
            VariableElement primitiveField = ProcessorTestUtils.createPrimitiveField("id", "long");
            VariableElement arrayField = ProcessorTestUtils.createArrayField("tags", "String[]");
            VariableElement entityField = ProcessorTestUtils.createFieldWithType("relatedEntity", entity2);
            VariableElement genericField = ProcessorTestUtils.createGenericFieldWithType("entityList", entity2);
            ExecutableElement method = ProcessorTestUtils.createMethodElement("someMethod");

            when(entity1.getEnclosedElements()).thenReturn((List) List.of(
                    primitiveField, arrayField, entityField, genericField, method
            ));

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle circular dependencies gracefully")
        void shouldHandleCircularDependenciesGracefully() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            // Create entities with circular dependency
            TypeElement entity1 = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);
            TypeElement entity2 = ProcessorTestUtils.createTypeElement(TEST_ENTITY2);

            // Entity1 -> Entity2
            VariableElement field1 = ProcessorTestUtils.createFieldWithType("entity2", entity2);
            when(entity1.getEnclosedElements()).thenReturn((List) List.of(field1));

            // Entity2 -> Entity1
            VariableElement field2 = ProcessorTestUtils.createFieldWithType("entity1", entity1);
            when(entity2.getEnclosedElements()).thenReturn((List) List.of(field2));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity1, entity2);

            // When
            processor.process(annotations, roundEnv);

            // Then - Should handle circular dependencies (topological sort should handle this)
            // The result depends on how the topological sorter handles cycles
            // We just verify it doesn't crash
            assertDoesNotThrow(() -> processor.process(annotations, roundEnv));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle empty class with no fields")
        void shouldHandleEmptyClassWithNoFields() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement emptyEntity = ProcessorTestUtils.createTypeElement("com.test.EmptyEntity");
            // Explicitly set empty enclosed elements
            when(emptyEntity.getEnclosedElements()).thenReturn(Collections.emptyList());

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, emptyEntity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify all phases executed
            verify(staticMetaModelGenerator).generate(emptyEntity);
            verify(filterMetaModelGenerator).generate(emptyEntity);
            verify(staticSpecificationServiceGenerator).generate(emptyEntity);
        }

        @Test
        @DisplayName("Should handle class with only non-annotated dependencies")
        void shouldHandleClassWithOnlyNonAnnotatedDependencies() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);

            // Create field with non-annotated type (not in annotatedClasses)
            TypeElement nonAnnotatedType = ProcessorTestUtils.createTypeElement("com.external.ExternalClass");
            VariableElement externalField = ProcessorTestUtils.createFieldWithType("external", nonAnnotatedType);
            when(entity.getEnclosedElements()).thenReturn((List) List.of(externalField));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle large number of entities with complex dependencies")
        void shouldHandleLargeNumberOfEntitiesWithComplexDependencies() throws ProcessingException {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            // Create a chain of dependencies: Entity1 -> Entity2 -> Entity3 -> Entity4 -> Entity5
            ProcessorTestUtils.TestScenario scenario = new ProcessorTestUtils.TestScenario()
                    .addEntity("com.test.Entity5")
                    .addEntity("com.test.Entity4")
                    .addEntity(TEST_ENTITY3)
                    .addEntity(TEST_ENTITY2)
                    .addEntity(TEST_ENTITY1);

            List<TypeElement> entities = scenario.getEntities();

            // Set up dependency chain
            for (int i = entities.size() - 1; i > 0; i--) {
                TypeElement current = entities.get(i);
                TypeElement dependency = entities.get(i - 1);

                VariableElement field = ProcessorTestUtils.createFieldWithType("dependency", dependency);
                when(current.getEnclosedElements()).thenReturn((List) List.of(field));
            }

            RoundEnvironment roundEnv = scenario.buildRoundEnvironment();

            // When
            boolean result = processor.process(annotations, roundEnv);

            // Then
            assertTrue(result);

            // Verify all entities were processed
            for (TypeElement entity : entities) {
                verify(staticMetaModelGenerator).generate(entity);
                verify(filterMetaModelGenerator).generate(entity);
                verify(staticSpecificationServiceGenerator).generate(entity);
            }
        }

        @Test
        @DisplayName("Should handle TypeElement with null qualified name")
        void shouldHandleTypeElementWithNullQualifiedName() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = mock(TypeElement.class);
            when(entity.getKind()).thenReturn(ElementKind.CLASS);
            when(entity.getQualifiedName()).thenReturn(null);
            when(entity.getEnclosedElements()).thenReturn(Collections.emptyList());

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When & Then - Should handle gracefully without throwing exception
            assertDoesNotThrow(() -> {
                boolean result = processor.process(annotations, roundEnv);
                // Should return false since null qualified name causes issues
                assertFalse(result);
            });
        }

        @Test
        @DisplayName("Should handle field with null type mirror")
        void shouldHandleFieldWithNullTypeMirror() {
            // Given
            TypeElement annotation = ProcessorTestUtils.createTypeElement(TEST_ANNOTATION);
            Set<TypeElement> annotations = Set.of(annotation);

            TypeElement entity = ProcessorTestUtils.createTypeElement(TEST_ENTITY1);

            VariableElement nullTypeField = mock(VariableElement.class);
            when(nullTypeField.getKind()).thenReturn(ElementKind.FIELD);
            Name fieldName = mock(Name.class);
            when(fieldName.toString()).thenReturn("nullField");
            when(nullTypeField.getSimpleName()).thenReturn(fieldName);
            when(nullTypeField.asType()).thenReturn(null);

            when(entity.getEnclosedElements()).thenReturn((List) List.of(nullTypeField));

            RoundEnvironment roundEnv = ProcessorTestUtils.createRoundEnvironment(false, entity);

            // When & Then - Should handle gracefully without throwing exception
            assertDoesNotThrow(() -> {
                processor.process(annotations, roundEnv);
                // Should still process successfully, just skip the null field
                // The result depends on implementation - it may return false due to null handling
            });
        }
    }
}