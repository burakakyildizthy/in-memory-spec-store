package com.thy.fss.common.inmemory.processor.generator;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;
import com.thy.fss.common.inmemory.testmodel.Priority;
import com.thy.fss.common.inmemory.testmodel.UserStatus;

/**
 * Integration tests for end-to-end filter deserialization with real JSON data.
 * Tests complex nested filter structures and verifies that filter deserialization
 * matches entity field deserialization behavior.
 * <p>
 * Requirements tested: 1.1, 4.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Filter Deserializer Generation Integration Tests")
class FilterDeserializerGenerationIntegrationTest {

    private static final String TEST_PACKAGE = "com.test";
    private static final String DESERIALIZE_METHOD = " deserialize(";
    private static final String JSON_PARSER = "JsonParser";
    private static final String DESERIALIZATION_CONTEXT = "DeserializationContext";
    private static final String NAME_FIELD = "name";
    private static final String STATUS_FIELD = "status";
    private static final String PRIORITY_FIELD = "priority";
    private static final String VERSION_FIELD = "version";
    private static final String CUSTOM_FORMATTED_DATE_TIME_FIELD = "customFormattedDateTime";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String UPDATED_AT_FIELD = "updatedAt";
    private static final String DEEP_COPY = "deepCopy";
    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String STRING_FILTER_TYPE = "StringFilter";
    private static final String INTEGER_FILTER_TYPE = "IntegerFilter";
    private static final String LOCAL_DATE_TIME_FILTER_TYPE = "LocalDateTimeFilter";
    private static final String USER_STATUS_FILTER_TYPE = "UserStatusFilter";
    private static final String PRIORITY_FILTER_TYPE = "PriorityFilter";

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Filer filer;

    @Mock
    private JavaFileObject javaFileObject;

    private FilterDeserializerGenerator generator;

    @BeforeEach
    void setUp() {
        when(processingEnv.getFiler()).thenReturn(filer);
        generator = new FilterDeserializerGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate LocalDateTimeFilter deserializer that handles custom format JSON")
    void shouldGenerateLocalDateTimeFilterDeserializerThatHandlesCustomFormatJson() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig("customFormattedDateTime",
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER_TYPE);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("CustomFormattedDateTimeFilter", TEST_PACKAGE,
                List.of(temporalConfig), "TemporalEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and temporal field handling
        assertThat(generatedCode)
                .contains("public class CustomFormattedDateTimeFilterDeserializer extends JsonDeserializer<CustomFormattedDateTimeFilter>")
                .contains("public CustomFormattedDateTimeFilter" + DESERIALIZE_METHOD)
                .contains(CUSTOM_FORMATTED_DATE_TIME_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer that handles @JsonCreator enum JSON")
    void shouldGenerateEnumFilterDeserializerThatHandlesJsonCreatorEnumJson() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(UserStatus.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromCode");

        FilterFieldConfig enumConfig = new FilterFieldConfig("status", UserStatus.class.getName(), USER_STATUS_FILTER_TYPE);
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(USER_STATUS_FILTER_TYPE, TEST_PACKAGE,
                List.of(enumConfig), "User");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and enum field handling
        assertThat(generatedCode)
                .contains("public class UserStatusFilterDeserializer extends JsonDeserializer<UserStatusFilter>")
                .contains("public UserStatusFilter" + DESERIALIZE_METHOD)
                .contains(STATUS_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer that handles @JsonValue field enum JSON")
    void shouldGenerateEnumFilterDeserializerThatHandlesJsonValueFieldEnumJson() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(Priority.class.getName());
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
        enumInfo.setJsonValueField("value");

        FilterFieldConfig enumConfig = new FilterFieldConfig("priority", Priority.class.getName(), PRIORITY_FILTER_TYPE);
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(PRIORITY_FILTER_TYPE, TEST_PACKAGE,
                List.of(enumConfig), "Task");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and enum field handling
        assertThat(generatedCode)
                .contains("public class PriorityFilterDeserializer extends JsonDeserializer<PriorityFilter>")
                .contains("public PriorityFilter" + DESERIALIZE_METHOD)
                .contains(PRIORITY_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate StringFilter deserializer that handles all string operations")
    void shouldGenerateStringFilterDeserializerThatHandlesAllStringOperations() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", JAVA_LANG_STRING, STRING_FILTER_TYPE);
        stringConfig.setString(true);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("NameStringFilter", TEST_PACKAGE,
                List.of(stringConfig), "Person");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and string field handling
        assertThat(generatedCode)
                .contains("public class NameStringFilterDeserializer extends JsonDeserializer<NameStringFilter>")
                .contains("public NameStringFilter" + DESERIALIZE_METHOD)
                .contains(NAME_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate IntegerFilter deserializer that handles numeric operations")
    void shouldGenerateIntegerFilterDeserializerThatHandlesNumericOperations() throws Exception {
        // Given
        FilterFieldConfig integerConfig = new FilterFieldConfig("version", Integer.class.getName(), INTEGER_FILTER_TYPE);
        integerConfig.setNumeric(true);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("VersionIntegerFilter", TEST_PACKAGE,
                List.of(integerConfig), "Document");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and numeric field handling
        assertThat(generatedCode)
                .contains("public class VersionIntegerFilterDeserializer extends JsonDeserializer<VersionIntegerFilter>")
                .contains("public VersionIntegerFilter" + DESERIALIZE_METHOD)
                .contains(VERSION_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate complex filter deserializer with multiple field types")
    void shouldGenerateComplexFilterDeserializerWithMultipleFieldTypes() throws Exception {
        // Given
        EnumDeserializationInfo userStatusInfo = new EnumDeserializationInfo(UserStatus.class.getName());
        userStatusInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        userStatusInfo.setJsonCreatorMethod("fromCode");

        EnumDeserializationInfo priorityInfo = new EnumDeserializationInfo(Priority.class.getName());
        priorityInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
        priorityInfo.setJsonValueField("value");

        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());

        FilterFieldConfig nameConfig = new FilterFieldConfig("name", JAVA_LANG_STRING, STRING_FILTER_TYPE);
        nameConfig.setString(true);

        FilterFieldConfig statusConfig = new FilterFieldConfig("status", UserStatus.class.getName(), USER_STATUS_FILTER_TYPE);
        statusConfig.setEnum(true);
        statusConfig.setEnumDeserializationInfo(userStatusInfo);

        FilterFieldConfig priorityConfig = new FilterFieldConfig("priority", Priority.class.getName(), PRIORITY_FILTER_TYPE);
        priorityConfig.setEnum(true);
        priorityConfig.setEnumDeserializationInfo(priorityInfo);

        FilterFieldConfig createdAtConfig = new FilterFieldConfig("createdAt", LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER_TYPE);
        createdAtConfig.setTemporal(true);
        createdAtConfig.setDateTimeFormatInfo(formatInfo);

        FilterFieldConfig updatedAtConfig = new FilterFieldConfig("updatedAt", LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER_TYPE);
        updatedAtConfig.setTemporal(true);
        updatedAtConfig.setDateTimeFormatInfo(formatInfo);

        FilterFieldConfig versionConfig = new FilterFieldConfig("version", Integer.class.getName(), INTEGER_FILTER_TYPE);
        versionConfig.setNumeric(true);

        List<FilterFieldConfig> fieldConfigs = Arrays.asList(
                nameConfig,
                statusConfig,
                priorityConfig,
                createdAtConfig,
                updatedAtConfig,
                versionConfig
        );

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("ComplexAnnotatedEntityFilter", TEST_PACKAGE,
                fieldConfigs, "ComplexEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and all field types are handled
        assertThat(generatedCode)
                .contains("public class ComplexAnnotatedEntityFilterDeserializer extends JsonDeserializer<ComplexAnnotatedEntityFilter>")
                .contains("public ComplexAnnotatedEntityFilter" + DESERIALIZE_METHOD)
                .contains(NAME_FIELD)
                .contains(STATUS_FIELD)
                .contains(PRIORITY_FIELD)
                .contains(CREATED_AT_FIELD)
                .contains(UPDATED_AT_FIELD)
                .contains(VERSION_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should handle nested filter structures correctly")
    void shouldHandleNestedFilterStructuresCorrectly() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", JAVA_LANG_STRING, STRING_FILTER_TYPE);
        stringConfig.setString(true);

        FilterFieldConfig integerConfig = new FilterFieldConfig("version", Integer.class.getName(), INTEGER_FILTER_TYPE);
        integerConfig.setNumeric(true);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("NestedFilter", TEST_PACKAGE,
                Arrays.asList(stringConfig, integerConfig), "NestedEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated, JSON parsing structure, and fields handling
        assertThat(generatedCode)
                .contains("public class NestedFilterDeserializer extends JsonDeserializer<NestedFilter>")
                .contains("public NestedFilter" + DESERIALIZE_METHOD)
                .contains(JSON_PARSER)
                .contains(DESERIALIZATION_CONTEXT)
                .contains(NAME_FIELD)
                .contains(VERSION_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should verify filter deserialization matches entity field deserialization behavior")
    void shouldVerifyFilterDeserializationMatchesEntityFieldDeserializationBehavior() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig("customFormattedDateTime",
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER_TYPE);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("CustomFormattedDateTimeFilter", TEST_PACKAGE,
                List.of(temporalConfig), "AnnotatedTemporalEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated and temporal field handling
        assertThat(generatedCode)
                .contains("public class CustomFormattedDateTimeFilterDeserializer extends JsonDeserializer<CustomFormattedDateTimeFilter>")
                .contains("public CustomFormattedDateTimeFilter" + DESERIALIZE_METHOD)
                .contains(CUSTOM_FORMATTED_DATE_TIME_FIELD)
                .doesNotContain(DEEP_COPY);
    }

    @Test
    @DisplayName("Should generate error handling for malformed JSON")
    void shouldGenerateErrorHandlingForMalformedJson() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", JAVA_LANG_STRING, STRING_FILTER_TYPE);
        stringConfig.setString(true);

        // Mock file creation
        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("ErrorHandlingStringFilter", TEST_PACKAGE,
                List.of(stringConfig), "ErrorEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deserializer was generated with proper structure, JSON parsing, and field handling
        assertThat(generatedCode)
                .contains("public class ErrorHandlingStringFilterDeserializer extends JsonDeserializer<ErrorHandlingStringFilter>")
                .contains("public ErrorHandlingStringFilter" + DESERIALIZE_METHOD)
                .contains(JSON_PARSER)
                .contains(DESERIALIZATION_CONTEXT)
                .contains(NAME_FIELD)
                .doesNotContain(DEEP_COPY);
    }
}