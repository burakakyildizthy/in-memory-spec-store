package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilterDeserializer;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for generated code compilation.
 * 
 * **Feature: collection-filter-web-binding, Property 9: Generated code compilation**
 * **Validates: Requirements 6.5**
 * 
 * Property: For any filter class with CollectionFilter fields, when the annotation processor completes,
 * the generated Deserializer class should compile without errors and support all collection operators.
 */
class CollectionFilterGeneratedCodeCompilationPropertyTest {

    private static final String GENERATED_SOURCE_DIR = "build/generated/sources/annotationProcessor/java/test";
    private static final List<String> REQUIRED_COLLECTION_OPERATORS = List.of(
        "cont", "empty", "nempty", "any", "all", "none"
    );

    /**
     * Property: Generated deserializer classes compile without errors.
     * 
     * For any filter class with CollectionFilter fields, the generated deserializer
     * should be a valid, compilable Java class.
     */
    @Property(tries = 10)
    void generatedDeserializerClassesCompile(
            @ForAll("collectionFilterFieldNames") String fieldName
    ) {
        // The fact that this test compiles and runs means the annotation processor
        // successfully generated the CollectionTestEntityFilter and its deserializer
        
        // Verify the generated filter class is available
        assertThat(CollectionTestEntityFilter.class)
                .as("Generated filter class should be available for field: " + fieldName)
                .isNotNull();
        
        // Verify the generated deserializer class is available
        assertThat(CollectionTestEntityFilterDeserializer.class)
                .as("Generated deserializer class should be available for field: " + fieldName)
                .isNotNull();
        
        // Verify we can instantiate the filter
        CollectionTestEntityFilter filter = new CollectionTestEntityFilter();
        assertThat(filter)
                .as("Should be able to instantiate generated filter")
                .isNotNull();
        
        // Verify we can access collection filter fields
        switch (fieldName) {
            case "tags" -> {
                CollectionFilter<String> tagsFilter = new CollectionFilter<>();
                filter.setTags(tagsFilter);
                assertThat(filter.getTags())
                        .as("Should be able to set and get tags filter")
                        .isNotNull();
            }
            case "scores" -> {
                CollectionFilter<Integer> scoresFilter = new CollectionFilter<>();
                filter.setScores(scoresFilter);
                assertThat(filter.getScores())
                        .as("Should be able to set and get scores filter")
                        .isNotNull();
            }
            case "priorities" -> {
                CollectionFilter<com.thy.fss.common.inmemory.testmodel.Priority> prioritiesFilter = new CollectionFilter<>();
                filter.setPriorities(prioritiesFilter);
                assertThat(filter.getPriorities())
                        .as("Should be able to set and get priorities filter")
                        .isNotNull();
            }
        }
    }

    /**
     * Property: Generated code contains all required collection operators.
     * 
     * For any collection filter field, the generated deserializer should contain
     * binding code for all collection-specific operators.
     */
    @Property(tries = 10)
    void generatedCodeContainsAllCollectionOperators(
            @ForAll("collectionFilterFieldNames") String fieldName,
            @ForAll("collectionOperators") String operator
    ) throws IOException {
        // Read the generated deserializer source
        Path deserializerPath = Paths.get(
            GENERATED_SOURCE_DIR + "/com/thy/fss/common/inmemory/testmodel/CollectionTestEntityFilterDeserializer.java"
        );
        
        assertThat(deserializerPath)
                .as("Generated deserializer source file should exist")
                .exists();
        
        String generatedCode = Files.readString(deserializerPath);
        
        // Verify the operator is present for the field
        String expectedCase = "case \"" + fieldName + "." + operator;
        
        assertThat(generatedCode)
                .as("Generated code should contain case for " + fieldName + "." + operator)
                .contains(expectedCase);
    }

    /**
     * Property: Generated code handles nested operators correctly.
     * 
     * For any collection filter field and nested operator (any, all, none),
     * the generated code should contain proper nested filter handling.
     * 
     * Note: Only tests fields that support nested operators (String and Integer collections).
     * Enum collections only support direct equality operators.
     */
    @Property(tries = 10)
    void generatedCodeHandlesNestedOperators(
            @ForAll("fieldsWithNestedOperators") String fieldName,
            @ForAll("nestedOperators") String nestedOp
    ) throws IOException {
        // Read the generated deserializer source
        Path deserializerPath = Paths.get(
            GENERATED_SOURCE_DIR + "/com/thy/fss/common/inmemory/testmodel/CollectionTestEntityFilterDeserializer.java"
        );
        
        String generatedCode = Files.readString(deserializerPath);
        
        // Verify nested operator handling is present
        String expectedPattern = "case \"" + fieldName + "." + nestedOp + ".";
        
        assertThat(generatedCode)
                .as("Generated code should contain nested operator handling for " + fieldName + "." + nestedOp)
                .contains(expectedPattern);
        
        // Verify the appropriate setter method is called
        String setterMethod = "setCollection" + 
            nestedOp.substring(0, 1).toUpperCase() + nestedOp.substring(1);
        
        assertThat(generatedCode)
                .as("Generated code should call " + setterMethod + " for nested operator")
                .contains(setterMethod);
    }

    @Provide
    Arbitrary<String> collectionFilterFieldNames() {
        return Arbitraries.of("tags", "scores", "priorities");
    }

    @Provide
    Arbitrary<String> collectionOperators() {
        // Only direct collection operators (not nested ones)
        return Arbitraries.of("cont", "empty", "nempty");
    }

    @Provide
    Arbitrary<String> nestedOperators() {
        return Arbitraries.of("any", "all", "none");
    }

    @Provide
    Arbitrary<String> fieldsWithNestedOperators() {
        // Only String and Integer collections support nested operators
        // Enum collections only support direct equality operators
        return Arbitraries.of("tags", "scores");
    }
}
