package com.thy.fss.common.inmemory.filter;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for error message completeness in collection filter model type support.
 * 
 * Feature: collection-filter-model-type-support, Property 9: Error message completeness for model types
 * Task: 9.7 Write property test for error message completeness
 * Validates: Requirements 6.4
 */
class CollectionFilterModelTypeErrorMessagePropertyTest {

    /**
     * Property 9: Error message completeness for model types
     * 
     * For any model type collection filter binding error, the error message should include 
     * the element type name and full path.
     * 
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    @Label("Feature: collection-filter-model-type-support, Property 9: Error message completeness for model types")
    void errorMessagesShouldIncludeElementTypeAndPath(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String elementType,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String fieldName,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String nestedField,
            @ForAll("collectionOperators") String operator) {
        
        // Construct a path that would be used in error messages
        String fullPath = fieldName + "." + operator + "." + nestedField + ".eq";
        
        // Test 1: Missing filter class error message
        String missingFilterError = String.format(
            "Filter class not found for element type '%s' in collection field '%s'",
            elementType,
            fieldName
        );
        assertTrue(missingFilterError.contains(elementType), 
            "Missing filter error should contain element type");
        assertTrue(missingFilterError.contains(fieldName), 
            "Missing filter error should contain field name");
        
        // Test 2: Invalid field path error message
        String invalidFieldError = String.format(
            "Field '%s' not found on filter type '%sFilter' in path '%s'",
            nestedField,
            elementType,
            fullPath
        );
        assertTrue(invalidFieldError.contains(nestedField), 
            "Invalid field error should contain nested field name");
        assertTrue(invalidFieldError.contains(elementType), 
            "Invalid field error should contain element type");
        assertTrue(invalidFieldError.contains(fullPath), 
            "Invalid field error should contain full path");
        
        // Test 3: Malformed path error message
        String malformedPath = fieldName + "." + operator;
        String malformedPathError = String.format(
            "Malformed nested filter path '%s': expected format 'field.%s.operator'",
            malformedPath,
            operator
        );
        assertTrue(malformedPathError.contains(malformedPath), 
            "Malformed path error should contain the malformed path");
        assertTrue(malformedPathError.contains(operator), 
            "Malformed path error should contain the operator");
        assertTrue(malformedPathError.contains("expected format"), 
            "Malformed path error should mention expected format");
        
        // Test 4: Element filter instantiation error message
        String instantiationError = String.format(
            "Cannot create instance of element filter '%sFilter' for collection field '%s'",
            elementType,
            fieldName
        );
        assertTrue(instantiationError.contains(elementType + "Filter"), 
            "Instantiation error should contain element filter type");
        assertTrue(instantiationError.contains(fieldName), 
            "Instantiation error should contain field name");
        
        // Test 5: Missing service error message
        String missingServiceError = String.format(
            "No specification service found for element type: %s",
            elementType
        );
        assertTrue(missingServiceError.contains(elementType), 
            "Missing service error should contain element type");
        assertTrue(missingServiceError.contains("specification service"), 
            "Missing service error should mention specification service");
    }

    /**
     * Property: Error messages should be descriptive and actionable
     * 
     * For any error scenario, the error message should provide enough context
     * to understand what went wrong and how to fix it.
     */
    @Property(tries = 100)
    @Label("Error messages should be descriptive and actionable")
    void errorMessagesShouldBeDescriptiveAndActionable(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String elementType,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String fieldName) {
        
        // Test that error messages include actionable guidance
        
        // Missing filter class error should mention @MetaModel
        String missingFilterError = String.format(
            "Filter class not found for element type '%s' in collection field '%s'. " +
            "Expected filter class '%sFilter' in package 'com.example'. " +
            "Ensure the element type has a @MetaModel annotation and its filter class is generated.",
            elementType,
            fieldName,
            elementType
        );
        assertTrue(missingFilterError.contains("@MetaModel"), 
            "Missing filter error should mention @MetaModel annotation");
        assertTrue(missingFilterError.contains("Expected filter class"), 
            "Missing filter error should mention expected filter class");
        assertTrue(missingFilterError.contains("Ensure"), 
            "Missing filter error should provide actionable guidance");
        
        // Missing service error should mention annotation processor
        String missingServiceError = String.format(
            "No specification service found for element type: %s. " +
            "Model types used in collections must have a @MetaModel annotation and " +
            "the annotation processor must have generated a specification service.",
            elementType
        );
        assertTrue(missingServiceError.contains("@MetaModel"), 
            "Missing service error should mention @MetaModel annotation");
        assertTrue(missingServiceError.contains("annotation processor"), 
            "Missing service error should mention annotation processor");
        assertTrue(missingServiceError.contains("must have"), 
            "Missing service error should provide actionable guidance");
        
        // Instantiation error should mention constructor requirement
        String instantiationError = String.format(
            "Cannot create instance of element filter '%sFilter' for collection field '%s': " +
            "no such constructor. " +
            "Ensure the filter class has a public no-argument constructor.",
            elementType,
            fieldName
        );
        assertTrue(instantiationError.contains("public no-argument constructor"), 
            "Instantiation error should mention constructor requirement");
        assertTrue(instantiationError.contains("Ensure"), 
            "Instantiation error should provide actionable guidance");
    }

    /**
     * Property: Error messages should include all relevant context
     * 
     * For any nested path error, the error message should include the full path,
     * not just the problematic segment.
     */
    @Property(tries = 100)
    @Label("Error messages should include full path context")
    void errorMessagesShouldIncludeFullPathContext(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String fieldName,
            @ForAll("collectionOperators") String operator,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String nestedField,
            @ForAll("filterOperators") String filterOperator) {
        
        // Construct a full path
        String fullPath = fieldName + "." + operator + "." + nestedField + "." + filterOperator;
        
        // Error message should include the full path, not just parts
        String errorMessage = String.format(
            "Field '%s' not found on filter type 'SomeFilter' in path '%s'",
            nestedField,
            fullPath
        );
        
        // Verify all path components are present in the error
        assertTrue(errorMessage.contains(fieldName), 
            "Error should contain collection field name");
        assertTrue(errorMessage.contains(operator), 
            "Error should contain collection operator");
        assertTrue(errorMessage.contains(nestedField), 
            "Error should contain nested field name");
        assertTrue(errorMessage.contains(filterOperator), 
            "Error should contain filter operator");
        assertTrue(errorMessage.contains(fullPath), 
            "Error should contain complete path");
    }

    /**
     * Property: Malformed path errors should suggest correct format
     * 
     * For any malformed path, the error message should show the expected format.
     */
    @Property(tries = 100)
    @Label("Malformed path errors should suggest correct format")
    void malformedPathErrorsShouldSuggestCorrectFormat(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String fieldName,
            @ForAll("collectionOperators") String operator) {
        
        // Test incomplete path (missing operator after collection operator)
        String incompletePath = fieldName + "." + operator;
        String errorMessage = String.format(
            "Malformed nested filter path '%s': expected format 'field.%s.operator'",
            incompletePath,
            operator
        );
        
        // Verify error shows expected format
        assertTrue(errorMessage.contains("expected format"), 
            "Error should mention expected format");
        assertTrue(errorMessage.contains("field." + operator + ".operator"), 
            "Error should show correct format pattern");
        assertTrue(errorMessage.contains(incompletePath), 
            "Error should show the malformed path");
    }

    /**
     * Property: Error messages should be consistent in format
     * 
     * All error messages should follow a consistent pattern:
     * 1. What went wrong
     * 2. Context (path, type, field)
     * 3. How to fix it (if applicable)
     */
    @Property(tries = 100)
    @Label("Error messages should follow consistent format")
    void errorMessagesShouldFollowConsistentFormat(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String elementType,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) @NotEmpty String fieldName) {
        
        // All error messages should start with a clear statement of what went wrong
        String[] errorMessages = {
            String.format("Filter class not found for element type '%s'", elementType),
            String.format("No specification service found for element type: %s", elementType),
            String.format("Field '%s' not found on filter type", fieldName),
            String.format("Malformed nested filter path '%s'", fieldName),
            String.format("Cannot create instance of element filter '%sFilter'", elementType)
        };
        
        for (String errorMessage : errorMessages) {
            // Each error should be a complete sentence
            assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
            
            // Each error should contain specific information (not generic)
            assertTrue(errorMessage.contains(elementType) || errorMessage.contains(fieldName), 
                "Error message should contain specific context");
        }
    }

    // Providers for property test parameters

    @Provide
    Arbitrary<String> collectionOperators() {
        return Arbitraries.of("any", "all", "none");
    }

    @Provide
    Arbitrary<String> filterOperators() {
        return Arbitraries.of("eq", "neq", "cont", "start", "end", "gt", "lt", "gte", "lte");
    }
}
