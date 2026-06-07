package com.thy.fss.common.inmemory.processor.analyzer;

import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;

import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * Interface for analyzing Jackson annotations on entity fields.
 * Extracts Jackson annotation information for replication in filter deserializers.
 * <p>
 * Supports the following Jackson annotations:
 * - @JsonFormat: Date/time formatting patterns
 * - @JsonProperty: Property name mapping
 * - @JsonCreator: Constructor/factory method deserialization
 * - @JsonValue: Value-based serialization/deserialization
 * - @JsonDeserialize: Custom deserializer classes
 */
public interface JacksonAnnotationAnalyzer {

    /**
     * Extracts all Jackson annotations from the given entity field.
     *
     * @param field the entity field to analyze
     * @return list of AnnotationInfo objects representing Jackson annotations found
     */
    List<AnnotationInfo> extractJacksonAnnotations(VariableElement field);

    /**
     * Checks if the given field has any Jackson annotations.
     *
     * @param field the entity field to check
     * @return true if the field has Jackson annotations, false otherwise
     */
    boolean hasJacksonAnnotations(VariableElement field);

    /**
     * Generates annotation code string from AnnotationInfo for code generation.
     *
     * @param annotation the annotation information to convert to code
     * @return Java annotation code string (e.g., "@JsonFormat(pattern = \"yyyy-MM-dd\")")
     */
    String generateAnnotationCode(AnnotationInfo annotation);

    /**
     * Extracts Jackson annotations from a field and generates the corresponding code.
     * This is a convenience method that combines extraction and code generation.
     *
     * @param field the entity field to analyze
     * @return list of annotation code strings ready for code generation
     */
    List<String> extractAndGenerateAnnotationCode(VariableElement field);
}