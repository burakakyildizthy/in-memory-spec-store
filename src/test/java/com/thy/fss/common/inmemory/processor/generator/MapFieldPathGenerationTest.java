package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for mapFieldPath() method generation in FilterDeserializerGenerator.
 * Tests the generation of field path mapping logic for query parameter support.
 */
class MapFieldPathGenerationTest {

    private static final String TEST_PACKAGE = "com.test";
    private static final String USER_FILTER = "UserFilter";
    private static final String USER_ENTITY = "User";
    private static final String NAME_FIELD = "name";
    private static final String EMAIL_FIELD = "email";
    private static final String ADDRESS_FIELD = "address";
    private static final String JSON_PROPERTY = "JsonProperty";
    private static final String VALUE_PARAM = "value";
    private static final String NAME_ABBREVIATED = "\"n\"";
    private static final String EMAIL_ABBREVIATED = "\"e\"";
    private static final String ADDRESS_ABBREVIATED = "\"addr\"";
    private static final String MAP_FIELD_PATH_METHOD = "public static String mapFieldPath(String path)";
    private static final String MAPS_ABBREVIATED_FIELD_PATHS = "Maps abbreviated field paths (using @JsonProperty names) to Java field paths";
    private static final String SWITCH_MAPPED_FIELD = "String mappedField = switch (firstSegment)";
    private static final String CASE_N_NAME = "case \"n\" -> \"name\"";
    private static final String CASE_E_EMAIL = "case \"e\" -> \"email\"";
    private static final String CASE_ADDRESS_TRUE = "case \"address\" -> true";
    private static final String DEFAULT_FIRST_SEGMENT = "default -> firstSegment";
    private static final String SEGMENTS_LENGTH_ONE = "if (segments.length == 1)";
    private static final String RETURN_MAPPED_FIELD = "return mappedField";
    private static final String STRING_BUILDER_RESULT = "StringBuilder result = new StringBuilder(mappedField)";
    private static final String NO_JSON_PROPERTY_MAPPINGS = "No @JsonProperty mappings defined for this filter";
    private static final String RETURN_PATH = "return path";
    private static final String IS_NESTED_FILTER_FIELD = "private static boolean isNestedFilterField(String fieldName)";
    private static final String MAP_NESTED_FIELD_PATH = "private static String mapNestedFieldPath(String parentField, String nestedPath)";
    private static final String ADDRESS_FILTER_DESERIALIZER = "case \"address\" -> AddressFilterDeserializer.mapFieldPath(nestedPath)";
    private static final String IF_IS_NESTED_FILTER_FIELD = "if (isNestedFilterField(mappedField))";
    private static final String MAPPED_NESTED_PATH = "String mappedNestedPath = mapNestedFieldPath(mappedField, nestedPath)";
    private static final String PATH_NULL_OR_EMPTY = "if (path == null || path.isEmpty())";
    private static final String NOT_NESTED_FILTER_OPERATORS = "Not a nested filter - remaining segments are operators";
    private static final String FOR_LOOP_SEGMENTS = "for (int i = 1; i < segments.length; i++)";
    private static final String APPEND_DOT_SEGMENTS = "result.append(\".\").append(segments[i])";
    private static final String STRING_FILTER_TYPE = "StringFilter";
    private static final String JAVA_LANG_STRING_TYPE = "java.lang.String";

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Filer filer;

    @Mock
    private Messager messager;

    @Mock
    private Elements elements;

    @Mock
    private javax.lang.model.util.Types types;

    @Mock
    private JavaFileObject javaFileObject;

    private FilterDeserializerGenerator generator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(processingEnv.getMessager()).thenReturn(messager);
        when(processingEnv.getElementUtils()).thenReturn(elements);
        when(processingEnv.getTypeUtils()).thenReturn(types);

        generator = new FilterDeserializerGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate mapFieldPath method with JsonProperty mappings")
    void shouldGenerateMapFieldPathWithJsonPropertyMappings() throws Exception {
        // Given
        // Create field config with @JsonProperty annotation
        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        nameConfig.setString(true);
        Map<String, Object> jsonPropertyParams = new HashMap<>();
        jsonPropertyParams.put(VALUE_PARAM, NAME_ABBREVIATED);
        AnnotationInfo jsonPropertyAnnotation = new AnnotationInfo(JSON_PROPERTY, jsonPropertyParams);
        nameConfig.setJacksonAnnotations(List.of(jsonPropertyAnnotation));

        FilterFieldConfig emailConfig = new FilterFieldConfig(EMAIL_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        emailConfig.setString(true);
        Map<String, Object> emailJsonPropertyParams = new HashMap<>();
        emailJsonPropertyParams.put(VALUE_PARAM, EMAIL_ABBREVIATED);
        AnnotationInfo emailJsonPropertyAnnotation = new AnnotationInfo(JSON_PROPERTY, emailJsonPropertyParams);
        emailConfig.setJacksonAnnotations(List.of(emailJsonPropertyAnnotation));

        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig, emailConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_FILTER, TEST_PACKAGE, fieldConfigs, USER_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify mapFieldPath method, switch statement, and path reconstruction logic
        assertThat(generatedCode)
                .contains(MAP_FIELD_PATH_METHOD)
                .contains(MAPS_ABBREVIATED_FIELD_PATHS)
                .contains(SWITCH_MAPPED_FIELD)
                .contains(CASE_N_NAME)
                .contains(CASE_E_EMAIL)
                .contains(DEFAULT_FIRST_SEGMENT)
                .contains(SEGMENTS_LENGTH_ONE)
                .contains(RETURN_MAPPED_FIELD)
                .contains(STRING_BUILDER_RESULT);
    }

    @Test
    @DisplayName("Should generate mapFieldPath method without mappings when no JsonProperty annotations")
    void shouldGenerateMapFieldPathWithoutMappings() throws Exception {
        // Given
        // Create field configs without @JsonProperty annotations
        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        nameConfig.setString(true);
        FilterFieldConfig emailConfig = new FilterFieldConfig(EMAIL_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        emailConfig.setString(true);

        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig, emailConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_FILTER, TEST_PACKAGE, fieldConfigs, USER_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify mapFieldPath method and path handling when no mappings
        assertThat(generatedCode)
                .contains(MAP_FIELD_PATH_METHOD)
                .contains(NO_JSON_PROPERTY_MAPPINGS)
                .contains(RETURN_PATH);
    }

    @Test
    @DisplayName("Should generate nested filter support in mapFieldPath")
    void shouldGenerateNestedFilterSupport() throws Exception {
        // Given
        // Create nested filter field config
        FilterFieldConfig addressConfig = new FilterFieldConfig(ADDRESS_FIELD, "AddressFilter", "AddressFilter");
        Map<String, Object> jsonPropertyParams = new HashMap<>();
        jsonPropertyParams.put(VALUE_PARAM, ADDRESS_ABBREVIATED);
        AnnotationInfo jsonPropertyAnnotation = new AnnotationInfo(JSON_PROPERTY, jsonPropertyParams);
        addressConfig.setJacksonAnnotations(List.of(jsonPropertyAnnotation));

        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        nameConfig.setString(true);
        Map<String, Object> nameJsonPropertyParams = new HashMap<>();
        nameJsonPropertyParams.put(VALUE_PARAM, NAME_ABBREVIATED);
        AnnotationInfo nameJsonPropertyAnnotation = new AnnotationInfo(JSON_PROPERTY, nameJsonPropertyParams);
        nameConfig.setJacksonAnnotations(List.of(nameJsonPropertyAnnotation));

        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig, addressConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_FILTER, TEST_PACKAGE, fieldConfigs, USER_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify nested filter helper methods and path handling
        assertThat(generatedCode)
                .contains(IS_NESTED_FILTER_FIELD)
                .contains(CASE_ADDRESS_TRUE)
                .contains(MAP_NESTED_FIELD_PATH)
                .contains(ADDRESS_FILTER_DESERIALIZER)
                .contains(IF_IS_NESTED_FILTER_FIELD)
                .contains(MAPPED_NESTED_PATH);
    }

    @Test
    @DisplayName("Should handle null and empty paths in mapFieldPath")
    void shouldHandleNullAndEmptyPaths() throws Exception {
        // Given
        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        nameConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_FILTER, TEST_PACKAGE, fieldConfigs, USER_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify null/empty handling
        assertThat(generatedCode)
                .contains(PATH_NULL_OR_EMPTY)
                .contains(RETURN_PATH);
    }

    @Test
    @DisplayName("Should preserve operator segments in mapped paths")
    void shouldPreserveOperatorSegments() throws Exception {
        // Given
        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING_TYPE, STRING_FILTER_TYPE);
        nameConfig.setString(true);
        Map<String, Object> jsonPropertyParams = new HashMap<>();
        jsonPropertyParams.put(VALUE_PARAM, NAME_ABBREVIATED);
        AnnotationInfo jsonPropertyAnnotation = new AnnotationInfo(JSON_PROPERTY, jsonPropertyParams);
        nameConfig.setJacksonAnnotations(List.of(jsonPropertyAnnotation));

        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_FILTER, TEST_PACKAGE, fieldConfigs, USER_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify operator preservation logic
        assertThat(generatedCode)
                .contains(NOT_NESTED_FILTER_OPERATORS)
                .contains(FOR_LOOP_SEGMENTS)
                .contains(APPEND_DOT_SEGMENTS);
    }
}
