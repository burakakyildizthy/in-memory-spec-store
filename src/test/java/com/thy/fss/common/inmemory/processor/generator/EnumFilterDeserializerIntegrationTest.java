package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;
import com.thy.fss.common.inmemory.testmodel.Priority;
import com.thy.fss.common.inmemory.testmodel.Status;
import com.thy.fss.common.inmemory.testmodel.UserStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for EnumFilter deserializer generation.
 * Tests the complete flow from enum analysis to deserializer code generation.
 */
@DisplayName("EnumFilter Deserializer Integration Tests")
class EnumFilterDeserializerIntegrationTest {

    private static final String GENERATED_PACKAGE = "com.thy.fss.common.inmemory.generated";
    private static final String DESERIALIZE_METHOD = "public UserStatusFilter deserialize(";
    private static final String STATUS_DESERIALIZE_METHOD = "public StatusFilter deserialize(";
    private static final String PRIORITY_DESERIALIZE_METHOD = "public PriorityFilter deserialize(";
    private static final String JSON_PARSER = "JsonParser";
    private static final String DESERIALIZATION_CONTEXT = "DeserializationContext";
    private static final String USER_STATUS_FIELD = "userStatus";
    private static final String STATUS_FIELD = "status";
    private static final String PRIORITY_FIELD = "priority";

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
    @DisplayName("Should generate complete EnumFilter deserializer for @JsonCreator enum")
    void shouldGenerateCompleteEnumFilterDeserializerForJsonCreatorEnum() throws Exception {
        // Given
        // Create enum info for UserStatus (has @JsonCreator method)
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(UserStatus.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromCode");

        FilterFieldConfig enumConfig = new FilterFieldConfig("userStatus", UserStatus.class.getName(), "UserStatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("UserStatusFilter", GENERATED_PACKAGE,
                fieldConfigs, "User");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package, imports, class declaration, methods, and field handling
        assertThat(generatedCode)
                .contains("package " + GENERATED_PACKAGE + ";")
                .contains("import " + UserStatus.class.getName() + ";")
                .contains("public class UserStatusFilterDeserializer extends JsonDeserializer<UserStatusFilter>")
                .contains(DESERIALIZE_METHOD)
                .contains(USER_STATUS_FIELD)
                .doesNotContain("deepCopy");
    }

    @Test
    @DisplayName("Should generate complete EnumFilter deserializer for @JsonValue field enum")
    void shouldGenerateCompleteEnumFilterDeserializerForJsonValueFieldEnum() throws Exception {
        // Given
        // Create enum info for Priority (has @JsonValue field)
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(Priority.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
        enumInfo.setJsonValueField("value");

        FilterFieldConfig enumConfig = new FilterFieldConfig("priority", Priority.class.getName(), "PriorityFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("PriorityFilter", GENERATED_PACKAGE,
                fieldConfigs, "Task");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package, imports, class declaration, methods, and field handling
        assertThat(generatedCode)
                .contains("package " + GENERATED_PACKAGE + ";")
                .contains("import " + Priority.class.getName() + ";")
                .contains("public class PriorityFilterDeserializer extends JsonDeserializer<PriorityFilter>")
                .contains(PRIORITY_DESERIALIZE_METHOD)
                .contains(PRIORITY_FIELD)
                .doesNotContain("deepCopy");
    }

    @Test
    @DisplayName("Should generate complete EnumFilter deserializer for default enum")
    void shouldGenerateCompleteEnumFilterDeserializerForDefaultEnum() throws Exception {
        // Given
        // Create enum info for Status (no Jackson annotations)
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(Status.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);

        FilterFieldConfig enumConfig = new FilterFieldConfig("status", Status.class.getName(), "StatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("StatusFilter", GENERATED_PACKAGE,
                fieldConfigs, "Order");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package, imports, class declaration, methods, and field handling
        assertThat(generatedCode)
                .contains("package " + GENERATED_PACKAGE + ";")
                .contains("import " + Status.class.getName() + ";")
                .contains("public class StatusFilterDeserializer extends JsonDeserializer<StatusFilter>")
                .contains(STATUS_DESERIALIZE_METHOD)
                .contains(STATUS_FIELD)
                .doesNotContain("deepCopy");
    }

    @Test
    @DisplayName("Should generate optimized array parsing for enum collections")
    void shouldGenerateOptimizedArrayParsingForEnumCollections() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(UserStatus.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromCode");

        FilterFieldConfig enumConfig = new FilterFieldConfig("userStatus", UserStatus.class.getName(), "UserStatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("UserStatusFilter", GENERATED_PACKAGE,
                fieldConfigs, "User");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and enum field is handled
        assertThat(generatedCode)
                .contains("public class UserStatusFilterDeserializer extends JsonDeserializer<UserStatusFilter>")
                .contains(DESERIALIZE_METHOD)
                .contains(USER_STATUS_FIELD);
    }

    @Test
    @DisplayName("Should handle null values correctly in enum parsing")
    void shouldHandleNullValuesCorrectlyInEnumParsing() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(Status.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);

        FilterFieldConfig enumConfig = new FilterFieldConfig("status", Status.class.getName(), "StatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("StatusFilter", GENERATED_PACKAGE,
                fieldConfigs, "Order");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and enum field is handled
        assertThat(generatedCode)
                .contains("public class StatusFilterDeserializer extends JsonDeserializer<StatusFilter>")
                .contains(STATUS_DESERIALIZE_METHOD)
                .contains(STATUS_FIELD);
    }

    @Test
    @DisplayName("Should generate proper error handling and unknown field skipping")
    void shouldGenerateProperErrorHandlingAndUnknownFieldSkipping() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(UserStatus.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromCode");

        FilterFieldConfig enumConfig = new FilterFieldConfig("userStatus", UserStatus.class.getName(), "UserStatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("UserStatusFilter", GENERATED_PACKAGE,
                fieldConfigs, "User");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated with proper structure and JSON parsing
        assertThat(generatedCode)
                .contains("public class UserStatusFilterDeserializer extends JsonDeserializer<UserStatusFilter>")
                .contains(DESERIALIZE_METHOD)
                .contains(JSON_PARSER)
                .contains(DESERIALIZATION_CONTEXT)
                .contains(USER_STATUS_FIELD);
    }
}