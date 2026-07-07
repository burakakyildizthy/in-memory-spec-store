package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilterDeserializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that the annotation processor generates compilable code
 * for CollectionFilter fields and that the generated code contains all required operators.
 * 
 * Validates Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
class CollectionFilterGeneratedCodeCompilationTest {

    private static final String GENERATED_SOURCE_DIR = "build/generated/sources/annotationProcessor/java/test";
    private static final String DESERIALIZER_CLASS_NAME = "CollectionTestEntityFilterDeserializer";
    
    @Test
    void testGeneratedCodeCompiles() throws IOException {
        // Verify the generated classes are available (Requirement 6.5)
        // If this test compiles, it means the annotation processor successfully generated the classes
        assertThat(CollectionTestEntityFilter.class)
                .as("Generated filter class should be available")
                .isNotNull();
        
        assertThat(CollectionTestEntityFilterDeserializer.class)
                .as("Generated deserializer class should be available")
                .isNotNull();
        
        // Find and read the generated deserializer source file
        Path generatedFile = Paths.get(
            "build/generated/sources/annotationProcessor/java/test/com/thy/fss/common/inmemory/testmodel/" +
            DESERIALIZER_CLASS_NAME + ".java"
        );
        
        assertThat(generatedFile)
                .as("Generated deserializer source file should exist at: " + generatedFile)
                .exists();
        
        // Read the generated code
        String generatedCode = Files.readString(generatedFile);
        
        // Verify all collection operators are present (Example 6 - Requirements 6.1, 6.2)
        verifyAllCollectionOperatorsPresent(generatedCode);
        
        // Verify nested filter handling code is present (Example 7 - Requirement 6.3)
        verifyNestedFilterHandlingPresent(generatedCode);
        
        // Verify CollectionParameterHandler usage (Example 8 - Requirement 6.4)
        verifyCollectionParameterHandlerUsage(generatedCode);
        
        // Verify the generated code structure
        assertThat(generatedCode)
                .as("Generated code should be valid Java")
                .contains("public class " + DESERIALIZER_CLASS_NAME);
    }
    

    

    
    /**
     * Verifies that all collection operators are present in the generated code.
     * Example 6: Verify all collection operators present in generated code
     * Validates Requirements: 6.1, 6.2
     */
    private void verifyAllCollectionOperatorsPresent(String generatedCode) {
        // Collection-specific operators (using field.operator format)
        assertThat(generatedCode)
                .as("Generated code should handle 'cont' operator")
                .contains("case \"tags.cont\"");
        
        assertThat(generatedCode)
                .as("Generated code should handle 'empty' operator")
                .contains("case \"tags.empty\"");
        
        assertThat(generatedCode)
                .as("Generated code should handle 'nempty' operator")
                .contains("case \"tags.nempty\"");
        
        // Nested operators (using field.nested.operator format)
        assertThat(generatedCode)
                .as("Generated code should handle 'any' operator")
                .contains("case \"tags.any.");
        
        assertThat(generatedCode)
                .as("Generated code should handle 'all' operator")
                .contains("case \"tags.all.");
        
        assertThat(generatedCode)
                .as("Generated code should handle 'none' operator")
                .contains("case \"tags.none.");
        
        // Verify multiple element types are supported
        assertThat(generatedCode)
                .as("Generated code should handle String collection fields")
                .contains("CollectionFilter<java.lang.String>");
        
        assertThat(generatedCode)
                .as("Generated code should handle Integer collection fields")
                .contains("CollectionFilter<java.lang.Integer>");
        
        assertThat(generatedCode)
                .as("Generated code should handle enum collection fields")
                .contains("CollectionFilter<com.thy.fss.common.inmemory.testmodel.Priority>");
    }
    
    /**
     * Verifies that nested filter handling code is present in the generated code.
     * Example 7: Verify nested filter handling code present
     * Validates Requirement: 6.3
     */
    private void verifyNestedFilterHandlingPresent(String generatedCode) {
        // Check for nested filter creation
        assertThat(generatedCode)
                .as("Generated code should create nested filters for 'any' operator")
                .containsAnyOf(
                        "setCollectionAny",
                        "collectionAny"
                );
        
        assertThat(generatedCode)
                .as("Generated code should create nested filters for 'all' operator")
                .containsAnyOf(
                        "setCollectionAll",
                        "collectionAll"
                );
        
        assertThat(generatedCode)
                .as("Generated code should create nested filters for 'none' operator")
                .containsAnyOf(
                        "setCollectionNone",
                        "collectionNone"
                );
        
        // Check for nested filter path parsing
        assertThat(generatedCode)
                .as("Generated code should handle nested filter paths")
                .containsAnyOf(
                        "pathParts.length >= 3",
                        "pathParts.length > 2",
                        "nested"
                );
    }
    
    /**
     * Verifies that CollectionParameterHandler is used in the generated code.
     * Example 8: Verify CollectionParameterHandler usage
     * Validates Requirement: 6.4
     */
    private void verifyCollectionParameterHandlerUsage(String generatedCode) {
        assertThat(generatedCode)
                .as("Generated code should use CollectionParameterHandler")
                .containsAnyOf(
                        "CollectionParameterHandler",
                        "collectionHandler",
                        "parseCommaSeparatedValues"
                );
        
        // Check that the handler is passed as a parameter
        assertThat(generatedCode)
                .as("Generated code should accept CollectionParameterHandler as parameter")
                .containsAnyOf(
                        "CollectionParameterHandler collectionHandler",
                        "CollectionParameterHandler handler"
                );
    }
}
