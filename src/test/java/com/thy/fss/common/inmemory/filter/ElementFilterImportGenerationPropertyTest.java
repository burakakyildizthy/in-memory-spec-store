package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.testmodel.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for element filter import generation.
 * 
 * Feature: collection-filter-model-type-support
 * Property 8: Element filter import generation
 * Validates: Requirements 2.3
 * 
 * Tests that for any collection filter with model type elements,
 * the generated deserializer includes imports for the element filter class.
 */
@DisplayName("Property 8: Element Filter Import Generation")
class ElementFilterImportGenerationPropertyTest {

    private static final String GENERATED_DESERIALIZER_PATH = 
        "build/generated/sources/annotationProcessor/java/test/com/thy/fss/common/inmemory/testmodel/MixedCollectionEntityFilterDeserializer.java";

    /**
     * Property: For any collection filter with model type elements,
     * the generated deserializer should include the element filter class in the same package.
     * 
     * This test verifies that:
     * 1. Model type element filters (UserFilter, ProfileFilter) are accessible in generated code
     * 2. The filters are in the same package so no explicit import is needed
     * 3. The generated code can reference the filter classes directly
     */
    @Property(tries = 10)
    @DisplayName("Model type element filters are accessible in generated deserializer")
    void modelTypeElementFiltersShouldBeAccessible() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify the deserializer is in the correct package
        assertThat(content).contains("package com.thy.fss.common.inmemory.testmodel;");
        
        // Verify UserFilter is referenced (same package, no import needed)
        assertThat(content).contains("UserFilter");
        assertThat(content).contains("new UserFilter()");
        assertThat(content).contains("UserFilterDeserializer.bindParameter");
        
        // Verify ProfileFilter is referenced (same package, no import needed)
        assertThat(content).contains("ProfileFilter");
        assertThat(content).contains("new ProfileFilter()");
        assertThat(content).contains("ProfileFilterDeserializer.bindParameter");
    }

    /**
     * Property: For any collection filter with basic type elements,
     * the generated deserializer should import the basic filter classes.
     */
    @Property(tries = 10)
    @DisplayName("Basic type element filters are imported in generated deserializer")
    void basicTypeElementFiltersShouldBeImported() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify basic filter imports
        assertThat(content).contains("import com.thy.fss.common.inmemory.filter.StringFilter;");
        assertThat(content).contains("import com.thy.fss.common.inmemory.filter.IntegerFilter;");
        
        // Verify basic filters are used
        assertThat(content).contains("StringFilter");
        assertThat(content).contains("new StringFilter()");
        assertThat(content).contains("IntegerFilter");
        assertThat(content).contains("new IntegerFilter()");
    }

    /**
     * Property: For any collection filter with mixed types,
     * the generated deserializer should handle both basic and model type imports correctly.
     */
    @Property(tries = 10)
    @DisplayName("Mixed collection types have correct imports and references")
    void mixedCollectionTypesShouldHaveCorrectImports() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify CollectionFilter import
        assertThat(content).contains("import com.thy.fss.common.inmemory.filter.CollectionFilter;");
        
        // Verify FilterBase is used for type compatibility
        assertThat(content).contains("FilterBase<String>");
        assertThat(content).contains("FilterBase<Integer>");
        assertThat(content).contains("FilterBase<User>");
        assertThat(content).contains("FilterBase<Profile>");
        
        // Verify instanceof checks for type-safe casts
        assertThat(content).contains("instanceof StringFilter");
        assertThat(content).contains("instanceof IntegerFilter");
        assertThat(content).contains("instanceof UserFilter");
        assertThat(content).contains("instanceof ProfileFilter");
    }

    /**
     * Property: For any model type collection field,
     * the generated code should delegate to the element filter's deserializer.
     */
    @Property(tries = 10)
    @DisplayName("Model type collections delegate to element filter deserializers")
    void modelTypeCollectionsShouldDelegateToElementDeserializers() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify delegation to UserFilterDeserializer
        assertThat(content).contains("UserFilterDeserializer.bindParameter");
        assertThat(content).contains("users.any.");
        assertThat(content).contains("users.all.");
        assertThat(content).contains("users.none.");
        
        // Verify delegation to ProfileFilterDeserializer
        assertThat(content).contains("ProfileFilterDeserializer.bindParameter");
        assertThat(content).contains("profiles.any.");
        assertThat(content).contains("profiles.all.");
        assertThat(content).contains("profiles.none.");
    }

    /**
     * Property: For any collection filter field,
     * the generated code should use FilterBase for type compatibility.
     */
    @Property(tries = 10)
    @DisplayName("Generated code uses FilterBase for type compatibility")
    void generatedCodeShouldUseFilterBase() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify FilterBase is used for getting collection operators
        assertThat(content).contains("com.thy.fss.common.inmemory.filter.FilterBase");
        assertThat(content).contains("getCollectionAny()");
        assertThat(content).contains("getCollectionAll()");
        assertThat(content).contains("getCollectionNone()");
        
        // Verify type-safe casts from FilterBase to concrete types
        assertThat(content).contains("elementFilterBase instanceof");
        assertThat(content).contains("setCollectionAny(elementFilter)");
        assertThat(content).contains("setCollectionAll(elementFilter)");
        assertThat(content).contains("setCollectionNone(elementFilter)");
    }

    /**
     * Property: For any model type collection field,
     * the generated code should extract the remaining path after the collection operator.
     */
    @Property(tries = 10)
    @DisplayName("Generated code extracts remaining path for model type collections")
    void generatedCodeShouldExtractRemainingPath() throws IOException {
        // Read the generated deserializer file
        Path deserializerPath = Paths.get(GENERATED_DESERIALIZER_PATH);
        assertThat(deserializerPath).exists();
        
        String content = Files.readString(deserializerPath);
        
        // Verify path extraction for users field
        assertThat(content).contains("mappedPath.substring(\"users.any.\".length())");
        assertThat(content).contains("mappedPath.substring(\"users.all.\".length())");
        assertThat(content).contains("mappedPath.substring(\"users.none.\".length())");
        
        // Verify path extraction for profiles field
        assertThat(content).contains("mappedPath.substring(\"profiles.any.\".length())");
        assertThat(content).contains("mappedPath.substring(\"profiles.all.\".length())");
        assertThat(content).contains("mappedPath.substring(\"profiles.none.\".length())");
        
        // Verify remaining path is passed to element deserializer
        assertThat(content).contains("String remainingPath =");
    }
}
