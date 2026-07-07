package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for CollectionFilter field detection in FilterDeserializerGenerator.
 * Validates Requirements 6.1: Collection filter detection and metadata extraction.
 */
@DisplayName("CollectionFilter Detection Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollectionFilterDetectionTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Elements elementUtils;

    private FilterDeserializerGenerator generator;

    @BeforeEach
    void setUp() {
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        generator = new FilterDeserializerGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should detect CollectionFilter fields and include import")
    void shouldDetectCollectionFilterAndIncludeImport() {
        // Given: A field configuration with CollectionFilter
        FilterFieldConfig config = new FilterFieldConfig("tags", "java.util.Collection<java.lang.String>", "CollectionFilter");
        config.setCollection(true);
        config.setElementType("String");
        
        List<FilterFieldConfig> fieldConfigs = List.of(config);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: CollectionFilter import should be included
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should be included for collection fields");
    }

    @Test
    @DisplayName("Should detect CollectionFilter with Integer element type")
    void shouldDetectCollectionFilterWithIntegerElementType() {
        // Given: A field configuration with CollectionFilter<Integer>
        FilterFieldConfig config = new FilterFieldConfig("numbers", "java.util.Collection<java.lang.Integer>", "CollectionFilter");
        config.setCollection(true);
        config.setElementType("Integer");
        
        List<FilterFieldConfig> fieldConfigs = List.of(config);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: CollectionFilter import should be included
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should be included");
        
        // And: Element type should be set correctly
        assertEquals("Integer", config.getElementType(),
                "Element type should be extracted correctly");
    }

    @Test
    @DisplayName("Should detect CollectionFilter with enum element type")
    void shouldDetectCollectionFilterWithEnumElementType() {
        // Given: A field configuration with CollectionFilter<Status>
        FilterFieldConfig config = new FilterFieldConfig("statuses", "java.util.Collection<com.test.Status>", "CollectionFilter");
        config.setCollection(true);
        config.setElementType("com.test.Status");
        config.setEnum(false); // The collection itself is not an enum, but contains enums
        
        List<FilterFieldConfig> fieldConfigs = List.of(config);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: CollectionFilter import should be included
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should be included");
        
        // And: Element type should be set correctly
        assertEquals("com.test.Status", config.getElementType(),
                "Element type should be extracted correctly for enum collections");
    }

    @Test
    @DisplayName("Should handle multiple CollectionFilter fields")
    void shouldHandleMultipleCollectionFilterFields() {
        // Given: Multiple field configurations with CollectionFilter
        FilterFieldConfig tagsConfig = new FilterFieldConfig("tags", "java.util.Collection<java.lang.String>", "CollectionFilter");
        tagsConfig.setCollection(true);
        tagsConfig.setElementType("String");
        
        FilterFieldConfig numbersConfig = new FilterFieldConfig("numbers", "java.util.Collection<java.lang.Integer>", "CollectionFilter");
        numbersConfig.setCollection(true);
        numbersConfig.setElementType("Integer");
        
        List<FilterFieldConfig> fieldConfigs = List.of(tagsConfig, numbersConfig);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: CollectionFilter import should be included once
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should be included");
        
        // And: Both configs should have correct element types
        assertEquals("String", tagsConfig.getElementType());
        assertEquals("Integer", numbersConfig.getElementType());
    }

    @Test
    @DisplayName("Should not include CollectionFilter import for non-collection fields")
    void shouldNotIncludeCollectionFilterImportForNonCollectionFields() {
        // Given: A field configuration without CollectionFilter
        FilterFieldConfig config = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        config.setString(true);
        config.setCollection(false);
        
        List<FilterFieldConfig> fieldConfigs = List.of(config);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: CollectionFilter import should NOT be included
        assertFalse(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should not be included for non-collection fields");
        
        // But: StringFilter import should be included
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.StringFilter"),
                "StringFilter import should be included");
    }

    @Test
    @DisplayName("Should handle mixed collection and non-collection fields")
    void shouldHandleMixedCollectionAndNonCollectionFields() {
        // Given: Mixed field configurations
        FilterFieldConfig nameConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        nameConfig.setString(true);
        nameConfig.setCollection(false);
        
        FilterFieldConfig tagsConfig = new FilterFieldConfig("tags", "java.util.Collection<java.lang.String>", "CollectionFilter");
        tagsConfig.setCollection(true);
        tagsConfig.setElementType("String");
        
        FilterFieldConfig ageConfig = new FilterFieldConfig("age", "java.lang.Integer", "IntegerFilter");
        ageConfig.setNumeric(true);
        ageConfig.setCollection(false);
        
        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig, tagsConfig, ageConfig);

        // When: Getting additional imports
        Set<String> imports = generator.getAdditionalImports(fieldConfigs);

        // Then: All appropriate imports should be included
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.StringFilter"),
                "StringFilter import should be included");
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.CollectionFilter"),
                "CollectionFilter import should be included");
        assertTrue(imports.contains("com.thy.fss.common.inmemory.filter.IntegerFilter"),
                "IntegerFilter import should be included");
    }

    @Test
    @DisplayName("Should correctly identify collection flag")
    void shouldCorrectlyIdentifyCollectionFlag() {
        // Given: A collection field configuration
        FilterFieldConfig config = new FilterFieldConfig("tags", "java.util.Collection<java.lang.String>", "CollectionFilter");
        config.setCollection(true);
        config.setElementType("String");

        // Then: isCollection should return true
        assertTrue(config.isCollection(),
                "isCollection() should return true for collection fields");
        
        // And: Element type should be accessible
        assertNotNull(config.getElementType(),
                "Element type should be set");
        assertEquals("String", config.getElementType(),
                "Element type should match the generic parameter");
    }
}
